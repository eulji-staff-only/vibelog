package com.vibelog.domain.message.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dao.MessageRepository;
import com.vibelog.domain.message.model.dao.ReportRepository;
import com.vibelog.domain.message.model.dto.ReportRequest;
import com.vibelog.domain.message.model.entity.Message;
import com.vibelog.domain.message.model.entity.Report;
import com.vibelog.domain.message.model.entity.ReportStatus;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
 * [ReportService]
 * 역할 : 신고하기 (API-RPT-01, FR-RPT-001, PL-RPT-001·003, #255).
 * 책임 : - 검사 순서: 이메일 인증 → 대상(지금 볼 수 있는 것) → 내 콘텐츠 → 1인 1회.
 *        - 볼 수 있는 대상: 삭제되지 않은 게시글·댓글·리뷰(CommunityReader, 리뷰는 달린 작품을 볼 수 있을 때), 공개 포트폴리오(PortfolioReader),
 *          내가 받은 쪽지. 영구 정지 회원의 공개 콘텐츠는 숨겨져 있어 받지 않는다 (PL-RPT-006).
 *          운영자가 이미 숨긴 대상도 받지 않는다. 아니면 모두 REPORT_TARGET_NOT_FOUND (있는지 알리지 않는다, 명세 결정 15).
 *        - 대상 작성자를 target_member_id 로 함께 저장한다 (#270).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final MessageRepository messageRepository;
    private final CommunityReader communityReader;
    private final PortfolioReader portfolioReader;
    private final MemberReader memberReader;

    @Transactional
    public void report(Long memberId, ReportRequest request) {
        if (!memberReader.isEmailVerified(memberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
        ReportTargetType type = request.targetType();
        Long targetId = request.targetId();
        Long authorId = findAuthor(memberId, type, targetId)
                // 영구 정지 회원의 공개 콘텐츠는 다른 회원에게 숨겨져 있다 (PL-RPT-006). 쪽지는 공개 콘텐츠가 아니라 그대로 받는다
                .filter(author -> type == ReportTargetType.MESSAGE || memberReader.findHiddenMemberIds(Set.of(author)).isEmpty())
                .filter(author -> !reportRepository.existsByTargetTypeAndTargetIdAndStatus(type, targetId, ReportStatus.HIDDEN))
                .orElseThrow(() -> new BusinessException(MessageErrorCode.REPORT_TARGET_NOT_FOUND));
        if (authorId.equals(memberId)) {
            throw new BusinessException(MessageErrorCode.SELF_REPORT);
        }
        if (reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(memberId, type, targetId)) {
            throw new BusinessException(MessageErrorCode.ALREADY_REPORTED);
        }
        try {
            reportRepository.saveAndFlush(Report.create(memberId, type, targetId, authorId, request.reason(), request.detailToSave()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MessageErrorCode.ALREADY_REPORTED); // 동시에 두 번: uk_report_reporter_target
        }
    }

    private boolean canSeePortfolioOf(Long memberId, Long peerReviewId) {
        return communityReader.findPortfolioIdOfReview(peerReviewId).flatMap(portfolioReader::findBrief)
                .filter(p -> !p.deleted() && (p.isPublic() || p.memberId().equals(memberId)))
                .isPresent();
    }

    /* 지금 볼 수 있는 대상의 작성자. 쪽지는 내가 받은 것만 (보낸 사람이 작성자) */
    private Optional<Long> findAuthor(Long memberId, ReportTargetType type, Long targetId) {
        return switch (type) {
            case POST, COMMENT -> Optional.ofNullable(communityReader.findReportTargets(type.name(), List.of(targetId)).get(targetId))
                    .map(com.vibelog.domain.community.model.dto.ReportTarget::authorMemberId);
            // 리뷰는 달린 작품을 볼 수 있을 때만: 공개 작품이거나 내 작품 (비공개 작품 주인은 받은 리뷰를 신고할 수 있다)
            case REVIEW -> Optional.ofNullable(communityReader.findReportTargets(type.name(), List.of(targetId)).get(targetId))
                    .filter(review -> canSeePortfolioOf(memberId, targetId))
                    .map(com.vibelog.domain.community.model.dto.ReportTarget::authorMemberId);
            case PORTFOLIO -> Optional.ofNullable(portfolioReader.findReportTargets(List.of(targetId)).get(targetId))
                    .map(com.vibelog.domain.portfolio.model.dto.ReportTarget::authorMemberId);
            case MESSAGE -> messageRepository.findById(targetId).filter(message -> message.isReceivedBy(memberId))
                    .map(Message::getSenderMemberId);
        };
    }
}
