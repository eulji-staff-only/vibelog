package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.dao.MemberSuspensionRepository;
import com.vibelog.domain.auth.model.entity.MemberSuspension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/*
 * [SuspensionPolicy]
 * 역할 : 이 회원이 지금 이용 정지 중인지 판단한다 (FR-USR-010, PL-RPT-005).
 * 책임 : 해제되지 않았고, 경고가 아니고, 끝나는 시각이 지나지 않은 제재가 있으면 정지 중이다.
 *        로그인(폼·소셜·로그인 유지)과 로그인 중 POST 요청, 탈퇴가 이 판단을 같이 쓴다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuspensionPolicy {

    private final MemberSuspensionRepository memberSuspensionRepository;

    public Optional<MemberSuspension> findActive(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        return memberSuspensionRepository.findByMemberIdAndReleasedAtIsNull(memberId).stream()
                .filter(suspension -> suspension.isActiveAt(now))
                .findFirst();
    }

    public boolean isSuspended(Long memberId) {
        return findActive(memberId).isPresent();
    }
}
