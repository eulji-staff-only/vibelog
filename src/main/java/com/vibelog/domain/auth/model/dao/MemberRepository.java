package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    boolean existsByEmail(String email);

    boolean existsByNicknameAndStatus(String nickname, MemberStatus status);
}
