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

    /* 1인 1회 (uk_report_reporter_target, API-RPT-01 ALREADY_REPORTED) */
    boolean existsByReporterMemberIdAndTargetTypeAndTargetId(Long reporterMemberId, ReportTargetType targetType, Long targetId);

    /* 운영자가 이미 숨긴 대상인가 (그 대상은 신고를 받지 않는다) */
    boolean existsByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);
}
