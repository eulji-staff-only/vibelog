package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.MemberSuspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface MemberSuspensionRepository extends JpaRepository<MemberSuspension, Long> {

    List<MemberSuspension> findByMemberIdAndReleasedAtIsNull(Long memberId);

    /* 해제되지 않은 제재 (MemberReader.findSuspendedMemberIds, 콘테스트 집계) */
    List<MemberSuspension> findByMemberIdInAndReleasedAtIsNull(Collection<Long> memberIds);

    /* 운영자 회원 상세의 제재 이력. 최근 것부터 */
    List<MemberSuspension> findByMemberIdOrderByStartsAtDescMemberSuspensionIdDesc(Long memberId);

    /* 영구 정지 중인 회원 (PL-RPT-006) */
    @Query("select distinct s.memberId from MemberSuspension s where s.memberId in :memberIds"
            + " and s.level = com.vibelog.domain.auth.model.entity.SuspensionLevel.PERMANENT and s.releasedAt is null")
    Set<Long> findPermanentlySuspendedIn(@Param("memberIds") Collection<Long> memberIds);
}
