package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
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

    /* 회원 ID → 닉네임. 없는 ID는 결과에 빠진다. 탈퇴 회원은 "탈퇴한 사용자"로 저장되어 있어 그대로 나온다 (PL-USR-009) */
    public Map<Long, String> getNicknames(Collection<Long> memberIds) {
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getMemberId, Member::getNickname));
    }
}
