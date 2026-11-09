package com.vibelog.domain.message.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dao.MessageRepository;
import com.vibelog.domain.message.model.dao.NotificationRepository;
import com.vibelog.domain.message.model.dao.ReportRepository;
import com.vibelog.domain.message.model.dto.ReportAdminDetail;
import com.vibelog.domain.message.model.dto.ReportAdminItem;
import com.vibelog.domain.message.model.entity.Message;
import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationTargetType;
import com.vibelog.domain.message.model.entity.NotificationType;
import com.vibelog.domain.message.model.entity.Report;
import com.vibelog.domain.message.model.entity.ReportReason;
import com.vibelog.domain.message.model.entity.ReportStatus;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * [ReportAdminService]
 * 역할 : 운영자 신고 처리 A-13 (API-RPT-02~04, FR-RPT-004, PL-RPT-004, #257).
 * 책임 : - 목록은 처리 대기(PENDING) 신고를 대상별로 묶어 마지막 신고 순 20개씩.
 *        - 처리는 대상 단위: 그 대상의 PENDING 신고 전부를 숨김·기각한다. 고른 신고가 이미 처리됐으면 REPORT_ALREADY_HANDLED.
 *        - 원문·미리보기는 심사용 Reader(삭제·비공개 포함)로 읽는다. 쪽지는 신고된 그 쪽지만 읽는다.
 *        - 숨기면 작성자에게 HIDDEN 알림. 사유는 이번에 숨긴 신고 중 가장 많은 사유, 같으면 먼저 받은 사유 (명세 결정 22).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportAdminService {

    static final int PAGE_SIZE = 20;
    private static final int PREVIEW_LENGTH = 50;
    /* 종류·사유는 먼저 채우고, 미리보기(%s)는 255자에 맞춰 fit 이 채운다 */
    private static final String HIDDEN_FORMAT = "운영 정책에 따라 내 %s 「%%s」이(가) 숨겨졌습니다. 사유: %s";

    private final ReportRepository reportRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final CommunityReader communityReader;
    private final PortfolioReader portfolioReader;
    private final MemberReader memberReader;
    private final MessageReader messageReader;

    /* 대상 하나를 가리키는 키 */
    private record TargetKey(ReportTargetType type, Long id) {
        static TargetKey of(Report report) {
            return new TargetKey(report.getTargetType(), report.getTargetId());
        }
    }

    /* 미리보기·원문. 행이 아예 없으면 빈 값 */
    private record Text(String preview, String content) {
        static final Text EMPTY = new Text("", "");
    }

    /* API-RPT-02 목록. page 는 1부터 */
    public Page<ReportAdminItem> getPendingTargets(int page) {
        // ponytail: 처리 대기 신고를 모두 읽어 메모리에서 묶는다. 대기가 수천 건이 되면 대상별 집계 쿼리로 바꾼다
        Map<TargetKey, List<Report>> groups = reportRepository.findByStatusOrderByCreatedAtAscReportIdAsc(ReportStatus.PENDING).stream()
                .collect(Collectors.groupingBy(TargetKey::of, LinkedHashMap::new, Collectors.toList()));
        List<List<Report>> sorted = groups.values().stream()
                .sorted(Comparator.comparing((List<Report> reports) -> last(reports).getCreatedAt())
                        .thenComparing(reports -> last(reports).getReportId()).reversed())
                .toList();
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE);
        List<List<Report>> slice = sorted.stream().skip(pageable.getOffset()).limit(PAGE_SIZE).toList();

        Map<TargetKey, Text> texts = texts(slice.stream().map(reports -> TargetKey.of(reports.get(0))).toList());
        Map<Long, String> nicknames = memberReader.getNicknames(slice.stream().flatMap(List::stream)
                .flatMap(r -> Stream.of(r.getTargetMemberId(), r.getReporterMemberId())).collect(Collectors.toSet()));
        List<ReportAdminItem> items = slice.stream().map(reports -> {
            Report latest = last(reports);
            TargetKey key = TargetKey.of(latest);
            return new ReportAdminItem(latest.getReportId(), key.type().getLabel(), texts.getOrDefault(key, Text.EMPTY).preview(),
                    nicknames.getOrDefault(latest.getTargetMemberId(), ""), reasonCounts(reports),
                    reports.stream().map(r -> nicknames.getOrDefault(r.getReporterMemberId(), "")).toList(), reports.size(),
                    key.type() != ReportTargetType.MESSAGE && reports.size() >= MessageReader.AUTO_HIDE_COUNT, latest.getCreatedAt());
        }).toList();
        return new PageImpl<>(items, pageable, sorted.size());
    }

    /* API-RPT-02 reportId: 그 대상의 원문, 작성자 이력, 처리 대기 신고 */
    public ReportAdminDetail getDetail(Long reportId) {
        Report report = find(reportId);
        TargetKey key = TargetKey.of(report);
        List<Report> pending = pendingOf(key);
        Map<Long, String> nicknames = memberReader.getNicknames(Stream.concat(Stream.of(report.getTargetMemberId()),
                pending.stream().map(Report::getReporterMemberId)).collect(Collectors.toSet()));
        List<ReportAdminDetail.Entry> entries = pending.stream()
                .map(r -> new ReportAdminDetail.Entry(nicknames.getOrDefault(r.getReporterMemberId(), ""), r.getReason().getLabel(),
                        r.getReasonDetail(), r.getCreatedAt()))
                .toList();
        return new ReportAdminDetail(reportId, key.type().getLabel(), texts(List.of(key)).getOrDefault(key, Text.EMPTY).content(),
                report.getTargetMemberId(), nicknames.getOrDefault(report.getTargetMemberId(), ""),
                messageReader.getReportHistory(report.getTargetMemberId()), entries, report.isPending());
    }

    /* API-RPT-03 숨김 → 작성자에게 HIDDEN 알림 */
    @Transactional
    public void hide(Long adminMemberId, Long reportId) {
        Report report = find(reportId);
        List<Report> pending = handleable(report);
        pending.forEach(r -> r.hide(adminMemberId));
        TargetKey key = TargetKey.of(report);
        String format = HIDDEN_FORMAT.formatted(key.type().getLabel(), mostReported(pending).getLabel());
        String preview = texts(List.of(key)).getOrDefault(key, Text.EMPTY).preview();
        notificationRepository.save(Notification.create(report.getTargetMemberId(), NotificationType.HIDDEN,
                NotificationTargetType.valueOf(key.type().name()), key.id(), NotificationContractService.fit(format, preview)));
    }

    /* API-RPT-04 기각 (알림 없음) */
    @Transactional
    public void dismiss(Long adminMemberId, Long reportId) {
        handleable(find(reportId)).forEach(r -> r.reject(adminMemberId));
    }

    private Report find(Long reportId) {
        return reportRepository.findById(reportId).orElseThrow(() -> new BusinessException(MessageErrorCode.REPORT_NOT_FOUND));
    }

    /* 고른 신고가 아직 대기 중이면 그 대상의 대기 신고 전부 (PL-RPT-004 "이미 처리된 신고는 다시 처리하지 않는다") */
    private List<Report> handleable(Report report) {
        if (!report.isPending()) {
            throw new BusinessException(MessageErrorCode.REPORT_ALREADY_HANDLED);
        }
        return pendingOf(TargetKey.of(report));
    }

    private List<Report> pendingOf(TargetKey key) {
        return reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAscReportIdAsc(key.type(), key.id(), ReportStatus.PENDING);
    }

    /* "스팸 2, 욕설/비방 1": 사유 순서대로, 없는 사유는 뺀다 */
    private static String reasonCounts(List<Report> reports) {
        return Arrays.stream(ReportReason.values())
                .map(reason -> Map.entry(reason.getLabel(), reports.stream().filter(r -> r.getReason() == reason).count()))
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /* 가장 많은 사유, 같으면 먼저 받은 사유 (reports 는 오래된 것부터) */
    static ReportReason mostReported(List<Report> reports) {
        Map<ReportReason, Long> counts = reports.stream()
                .collect(Collectors.groupingBy(Report::getReason, LinkedHashMap::new, Collectors.counting()));
        ReportReason best = null;
        for (Map.Entry<ReportReason, Long> entry : counts.entrySet()) {
            if (best == null || entry.getValue() > counts.get(best)) {
                best = entry.getKey();
            }
        }
        return best;
    }

    private static Report last(List<Report> reports) {
        return reports.get(reports.size() - 1);
    }

    /* 심사용 원문 (삭제·비공개 포함). 종류별로 한 번씩 읽는다 */
    private Map<TargetKey, Text> texts(Collection<TargetKey> keys) {
        Map<TargetKey, Text> texts = new LinkedHashMap<>();
        Map<ReportTargetType, List<Long>> idsByType = keys.stream()
                .collect(Collectors.groupingBy(TargetKey::type, Collectors.mapping(TargetKey::id, Collectors.toList())));
        idsByType.forEach((type, ids) -> {
            switch (type) {
                case POST, COMMENT, REVIEW -> communityReader.findReportReviewTargets(type.name(), ids)
                        .forEach((id, t) -> texts.put(new TargetKey(type, id), new Text(t.preview(), t.content())));
                case PORTFOLIO -> portfolioReader.findReportReviewTargets(ids)
                        .forEach((id, t) -> texts.put(new TargetKey(type, id), new Text(t.preview(), t.content())));
                case MESSAGE -> messageRepository.findAllById(ids)
                        .forEach(m -> texts.put(new TargetKey(type, m.getMessageId()), messageText(m)));
            }
        });
        return texts;
    }

    private static Text messageText(Message message) {
        String content = message.getContent();
        return new Text(content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) + "…" : content, content);
    }
}
