package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Report;
import com.vibelog.domain.message.model.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /* 대상들이 받은 신고 전부 (숨김 여부 계산, idx_report_target_type_target_id) */
    List<Report> findByTargetTypeAndTargetIdIn(ReportTargetType targetType, Collection<Long> targetIds);
}
