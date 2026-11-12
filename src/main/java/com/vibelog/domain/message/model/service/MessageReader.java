package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.dao.MessageRepository;
import com.vibelog.domain.message.model.dao.NoticeRepository;
import com.vibelog.domain.message.model.dao.ReportRepository;
import com.vibelog.domain.message.model.dto.NoticeSummary;
import com.vibelog.domain.message.model.dto.ReportHistory;
import com.vibelog.domain.message.model.entity.Report;
import com.vibelog.domain.message.model.entity.ReportStatus;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
 * [MessageReader]
 * 역할 : 다른 영역이 message 영역 값(최근 공지 등)을 읽어 가는 곳. 읽기 전용이다.
 * 책임 : 다른 영역은 Notice·Repository 를 import 하지 않고 이 클래스만 주입해 쓴다 (08 문서 규칙 1).
 *        메서드는 쓰는 쪽이 필요할 때 추가한다. 신고 숨김 여부는 컬럼 없이 report 에서 계산한다 (P3).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageReader {

    /* 서로 다른 회원의 처리 대기 신고가 이만큼 쌓이면 자동 숨김 (PL-RPT-002) */
    static final int AUTO_HIDE_COUNT = 3;

    /* 신고로 숨겨진 콘텐츠 자리에 보여 주는 문구 (FR-RPT-003, PL-ERR-006). 각 영역 화면·DTO 가 같이 쓴다 */
    public static final String HIDDEN_TEXT = "운영 정책에 따라 숨겨진 콘텐츠입니다";

    /*
     * 신고 대상 종류 (report.target_type). 다른 영역은 ReportTargetType(entity)을 import 할 수 없으므로 이 상수로 넘긴다 (#484).
     * switch case 에도 쓰도록 문자열로 적고, ReportTargetType 이름과 같은지는 ReportTargetConstantsTest 가 확인한다.
     */
    public static final String TARGET_POST = "POST";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String TARGET_REVIEW = "REVIEW";
    public static final String TARGET_PORTFOLIO = "PORTFOLIO";
    public static final String TARGET_MESSAGE = "MESSAGE";

    private static final Predicate<List<Report>> HIDDEN_RULE = reports -> hasHidden(reports) || pendingCount(reports) >= AUTO_HIDE_COUNT;

    private final MessageRepository messageRepository;
    private final NoticeRepository noticeRepository;
    private final ReportRepository reportRepository;

    /* 최신순 limit 개 (메인 화면 최근 공지, #203) */
    public List<NoticeSummary> findRecentNotices(int limit) {
        return noticeRepository.findAllByOrderByCreatedAtDescNoticeIdDesc(PageRequest.of(0, limit)).stream()
                .map(NoticeSummary::from)
                .toList();
    }

    /*
     * 신고로 숨겨진 대상 ID (FR-RPT-002·003, P3). HIDDEN 신고가 있거나 PENDING 신고가 3건 이상이면 숨김.
     * targetType 은 report.target_type 값(POST·PORTFOLIO·COMMENT·REVIEW·MESSAGE). 공개 목록·상세에서 거를 때 쓴다 (#256)
     */
    public Set<Long> findHiddenTargetIds(String targetType, Collection<Long> targetIds) {
        return targetIdsWhere(targetType, targetIds, HIDDEN_RULE);
    }

    /* 신고로 숨겨진 그 종류의 대상 전체. 게시판 목록처럼 페이지를 나누는 조회에서 미리 빼려고 쓴다 (#256) */
    // ponytail: 처리 대기·숨김 신고를 모두 읽어 Java 로 센다. 신고가 수만 건 규모가 되면 group by 쿼리로 옮긴다
    public Set<Long> findAllHiddenTargetIds(String targetType) {
        return idsWhere(reportRepository.findByTargetTypeAndStatusIn(ReportTargetType.valueOf(targetType),
                List.of(ReportStatus.PENDING, ReportStatus.HIDDEN)), HIDDEN_RULE);
    }

    /* at 까지 운영자가 숨김으로 확정한 대상 ID (콘테스트 시상 제외, PL-CTS-006, #295) */
    public Set<Long> findConfirmedHiddenTargetIds(String targetType, Collection<Long> targetIds, LocalDateTime at) {
        return targetIdsWhere(targetType, targetIds, reports -> reports.stream().anyMatch(report -> report.isHiddenBy(at)));
    }

    /* 숨김 확정 없이 PENDING 3건 이상으로 자동 숨김인 대상 ID (콘테스트 시상 보류, PL-CTS-006, #295) */
    public Set<Long> findAutoHiddenTargetIds(String targetType, Collection<Long> targetIds) {
        return targetIdsWhere(targetType, targetIds, reports -> !hasHidden(reports) && pendingCount(reports) >= AUTO_HIDE_COUNT);
    }

    /* 이 회원의 콘텐츠가 받은 신고 건수 (A-08 회원 상세 신고 이력, FR-USR-009, #257) */
    public ReportHistory getReportHistory(Long memberId) {
        return new ReportHistory(reportRepository.countByTargetMemberIdAndStatus(memberId, ReportStatus.PENDING),
                reportRepository.countByTargetMemberIdAndStatus(memberId, ReportStatus.HIDDEN),
                reportRepository.countByTargetMemberIdAndStatus(memberId, ReportStatus.REJECTED));
    }

    /* 대상별 신고 묶음이 rule 에 맞는 대상 ID. 신고가 없는 대상은 빠진다. 빈 묶음이면 빈 결과 */
    private Set<Long> targetIdsWhere(String targetType, Collection<Long> targetIds, Predicate<List<Report>> rule) {
        if (targetIds.isEmpty()) {
            return Set.of();
        }
        return idsWhere(reportRepository.findByTargetTypeAndTargetIdIn(ReportTargetType.valueOf(targetType), targetIds), rule);
    }

    private static Set<Long> idsWhere(List<Report> reports, Predicate<List<Report>> rule) {
        return reports.stream().collect(Collectors.groupingBy(Report::getTargetId)).entrySet().stream()
                .filter(entry -> rule.test(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static boolean hasHidden(List<Report> reports) {
        return reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.HIDDEN);
    }

    private static long pendingCount(List<Report> reports) {
        return reports.stream().filter(report -> report.getStatus() == ReportStatus.PENDING).count();
    }

    /* 기간 내 쪽지 수 (message.created_at 기준, 보낸 사람·받은 사람이 둘 다 지운 쪽지 제외). from·to 가 null 이면 그 쪽 제한 없음. admin 확장 대시보드(#194, FR-ADM-003) */
    public long countMessages(LocalDateTime from, LocalDateTime to) {
        return messageRepository.countSentBetween(from, to);
    }

    /* 기간 내 접수된 신고 수 (report.created_at 기준, 처리 상태 무관). from·to 가 null 이면 그 쪽 제한 없음. admin 확장 대시보드(#194, FR-ADM-003) */
    public long countReports(LocalDateTime from, LocalDateTime to) {
        return reportRepository.countCreatedBetween(from, to);
    }
}
