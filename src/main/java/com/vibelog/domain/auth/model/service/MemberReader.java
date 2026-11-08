package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dao.MemberSuspensionRepository;
import com.vibelog.domain.auth.model.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [MemberReader]
 * 역할 : 다른 영역이 회원 값(닉네임 등)을 읽어 가는 곳. 읽기 전용이다.
 * 책임 : - 다른 영역은 Member·MemberRepository 를 import 하지 않고 이 클래스만 주입해 쓴다 (08 문서 규칙 1).
 *        - 목록 화면용 메서드는 ID 묶음을 받아 쿼리 한 번으로 읽는다 (08 문서 규칙 1-5).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가만 한다. 기존 메서드는 고치지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReader {

    private final MemberRepository memberRepository;
    private final MemberSuspensionRepository memberSuspensionRepository;

    /* 회원 ID → 닉네임. 없는 ID는 결과에 빠진다. 탈퇴 회원은 "탈퇴한 사용자"로 저장되어 있어 그대로 나온다 (PL-USR-009) */
    public Map<Long, String> getNicknames(Collection<Long> memberIds) {
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getMemberId, Member::getNickname));
    }

    /* 영구 정지 중인 회원 ID (PL-RPT-006). 공개 콘텐츠를 숨길 때 쓴다 (명세 auth.md P2). 빈 묶음이면 빈 결과 */
    public Set<Long> findHiddenMemberIds(Collection<Long> memberIds) {
        return memberIds.isEmpty() ? Set.of() : memberSuspensionRepository.findPermanentlySuspendedIn(memberIds);
    }

    /* 이메일 인증을 마쳤는지 (PL-USR-003). 글·댓글 등 작성 기능이 쓴다. 없는 ID면 false */
    public boolean isEmailVerified(Long memberId) {
        return memberRepository.findById(memberId)
                .map(member -> member.getEmailVerifiedAt() != null)
                .orElse(false);
    }

    /* 회원 ID 1건 → 닉네임. 공통 헤더(#132)처럼 로그인 회원 1명의 닉네임만 필요할 때 쓴다. 없는 ID면 빈 문자열 */
    public String getNickname(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .orElse("");
    }

    /* 기간 내 가입자 수 (member.created_at 기준). from·to 가 null 이면 그 쪽 제한 없음 (운영자 대시보드 API-ADM-01, FR-ADM-002) */
    public long countJoinedBetween(LocalDateTime from, LocalDateTime to) {
        return memberRepository.countByCreatedAtBetween(from, to);
    }
}
