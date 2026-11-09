package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Report;
import com.vibelog.domain.message.model.entity.ReportStatus;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /* 대상들이 받은 신고 전부 (숨김 여부 계산, idx_report_target_type_target_id) */
    List<Report> findByTargetTypeAndTargetIdIn(ReportTargetType targetType, Collection<Long> targetIds);

    /* 한 종류의 처리 대기·숨김 신고 전부 (목록 조회에서 숨긴 대상을 미리 뺄 때, #256) */
    List<Report> findByTargetTypeAndStatusIn(ReportTargetType targetType, Collection<ReportStatus> statuses);

    /* A-13 처리 대기 신고 전부, 오래된 것부터 (대상별로 묶는 것은 Service) */
    List<Report> findByStatusOrderByCreatedAtAscReportIdAsc(ReportStatus status);

    /* 한 대상의 처리 대기 신고, 오래된 것부터 (숨김·기각은 이 전부에 적용) */
    List<Report> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAscReportIdAsc(ReportTargetType targetType, Long targetId,
                                                                                     ReportStatus status);

    /* 이 회원의 콘텐츠가 받은 신고 수 (작성자 이력·A-08 신고 이력, target_member_id) */
    long countByTargetMemberIdAndStatus(Long targetMemberId, ReportStatus status);

    /* 1인 1회 (uk_report_reporter_target, API-RPT-01 ALREADY_REPORTED) */
    boolean existsByReporterMemberIdAndTargetTypeAndTargetId(Long reporterMemberId, ReportTargetType targetType, Long targetId);

    /* 운영자가 이미 숨긴 대상인가 (그 대상은 신고를 받지 않는다) */
    boolean existsByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);
}
