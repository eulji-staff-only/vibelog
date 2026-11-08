package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dao.MemberSuspensionRepository;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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

    /* 탈퇴하지 않은 회원 전체 ID. 공지 등록 때 모든 회원에게 알림을 남긴다 (message API-NTF-07, #186) */
    public List<Long> findActiveMemberIds() {
        return memberRepository.findActiveMemberIds();
    }

    /* 쪽지 받는 사람 찾기 (message API-MSG-02). 닉네임 앞부분 일치, 이용 중인 회원만, 나는 뺀다. 닉네임 순 limit 명 (ID → 닉네임, 순서 유지) */
    public Map<Long, String> findActiveNicknamesStartingWith(String prefix, Long excludeMemberId, int limit) {
        return memberRepository.findActiveByNicknamePrefix(AdminMemberService.escapeLike(prefix), excludeMemberId,
                        PageRequest.of(0, limit)).stream()
                .collect(Collectors.toMap(Member::getMemberId, Member::getNickname, (a, b) -> a, LinkedHashMap::new));
    }

    /* 탈퇴하지 않은 회원인가 (쪽지 받는 사람 검사, MESSAGE_RECEIVER_NOT_FOUND). 없는 ID 면 false */
    public boolean isActive(Long memberId) {
        return memberRepository.existsByMemberIdAndStatus(memberId, MemberStatus.ACTIVE);
    }
}
