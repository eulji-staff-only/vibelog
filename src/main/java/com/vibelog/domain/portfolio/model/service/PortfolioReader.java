package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [PortfolioReader]
 * 역할 : 다른 영역이 포트폴리오 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용. 값은 Entity 가 아니라 기본형·DTO 로 돌려준다.
 *        - 필요한 메서드는 부르는 쪽이 추가만 한다. 목록용은 ID 묶음을 받는다 (규칙 1-5).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioReader {

    private final PortfolioRepository portfolioRepository;

    /* 회원의 포트폴리오 수 (삭제 표시 제외). 운영자 회원 상세(API-ADM-38, FR-USR-009) */
    public long countOf(Long memberId) {
        return portfolioRepository.countByMemberIdAndDeletedAtIsNull(memberId);
    }
}
