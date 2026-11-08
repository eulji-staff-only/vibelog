package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    boolean existsByEmail(String email);

    boolean existsByNicknameAndStatus(String nickname, MemberStatus status);

    /* 운영자 회원 목록 (API-ADM-37). q 가 null 이면 전체, 있으면 닉네임·이메일 일부로 찾는다. 이용 중인 회원만, 최근 가입 순 */
    @Query("select m from Member m where m.status = com.vibelog.domain.auth.model.entity.MemberStatus.ACTIVE"
            + " and (:q is null or m.nickname like concat('%', :q, '%') or m.email like concat('%', :q, '%'))"
            + " order by m.memberId desc")
    Page<Member> searchActive(@Param("q") String q, Pageable pageable);
}
