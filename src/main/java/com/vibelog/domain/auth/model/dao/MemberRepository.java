package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    boolean existsByEmail(String email);

    boolean existsByNicknameAndStatus(String nickname, MemberStatus status);

    boolean existsByNicknameAndStatusAndMemberIdNot(String nickname, MemberStatus status, Long memberId);

    /*
     * 운영자 회원 목록 (API-ADM-37). q 가 null 이면 전체, 있으면 닉네임·이메일 일부로 찾는다. 이용 중인 회원만, 최근 가입 순.
     * q 의 % _ ! 는 부르는 쪽이 '!'로 이스케이프해서 넘긴다 (글자 그대로 찾기).
     */
    @Query("select m from Member m where m.status = com.vibelog.domain.auth.model.entity.MemberStatus.ACTIVE"
            + " and (:q is null or m.nickname like concat('%', :q, '%') escape '!' or m.email like concat('%', :q, '%') escape '!')"
            + " order by m.createdAt desc, m.memberId desc")
    Page<Member> searchActive(@Param("q") String q, Pageable pageable);

    /* 공지 알림을 받을 탈퇴하지 않은 회원 (message API-NTF-07) */
    @Query("select m.memberId from Member m where m.status = com.vibelog.domain.auth.model.entity.MemberStatus.ACTIVE")
    List<Long> findActiveMemberIds();

    /* 쪽지 받는 사람 찾기 (message API-MSG-02). prefix 의 ! % _ 는 부르는 쪽이 '!'로 이스케이프해서 넘긴다 */
    @Query("select m from Member m where m.status = com.vibelog.domain.auth.model.entity.MemberStatus.ACTIVE"
            + " and m.memberId <> :excludeId and m.nickname like concat(:prefix, '%') escape '!'"
            + " order by m.nickname asc, m.memberId asc")
    List<Member> findActiveByNicknamePrefix(@Param("prefix") String prefix, @Param("excludeId") Long excludeId, Pageable pageable);

    boolean existsByMemberIdAndStatus(Long memberId, MemberStatus status);

    /* 기간 내 가입자 수 (member.created_at 기준). from·to 가 null 이면 그 쪽 제한 없음 (운영자 대시보드 API-ADM-01, FR-ADM-002) */
    @Query("select count(m) from Member m where (:from is null or m.createdAt >= :from) and (:to is null or m.createdAt <= :to)")
    long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /* 그 시각까지 탈퇴한 회원 (콘테스트 집계, contest 명세 결정 8) */
    @Query("select m.memberId from Member m where m.memberId in :memberIds and m.withdrawnAt <= :at")
    Set<Long> findWithdrawnIn(@Param("memberIds") Collection<Long> memberIds, @Param("at") LocalDateTime at);
}
