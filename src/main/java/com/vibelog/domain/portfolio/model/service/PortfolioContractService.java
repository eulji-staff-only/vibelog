package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.model.dao.PortfolioAssignmentRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.domain.portfolio.model.policy.CourseAssignmentChecker;
import com.vibelog.domain.portfolio.model.policy.LessonPortfolioChecker;
import com.vibelog.domain.portfolio.model.policy.MemberPortfolioCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [PortfolioContractService]
 * 역할 : portfolio 영역이 다른 영역에 주는 계약 ⑧⑨⑩의 진짜 구현.
 * 책임 : - 다른 영역은 이 클래스가 아니라 model/policy 의 계약 인터페이스를 주입해 쓴다.
 *        - 조회 계약은 읽기 전용, 기록 계약(⑩)은 부르는 쪽 트랜잭션에 함께 묶인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioContractService implements LessonPortfolioChecker, CourseAssignmentChecker, MemberPortfolioCleaner {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssignmentRepository portfolioAssignmentRepository;

    /* ⑧ 삭제 표시된 포트폴리오만 있으면 "제출 안 함" */
    @Override
    public boolean hasSubmitted(Long memberId, Long courseId) {
        return portfolioRepository.existsByMemberIdAndCourseIdAndDeletedAtIsNull(memberId, courseId);
    }

    /* ⑨ */
    @Override
    public boolean hasAssignment(Long courseId) {
        return portfolioAssignmentRepository.existsByCourseId(courseId);
    }

    /* ⑩ 변경 감지로 저장되므로 save 를 다시 부르지 않는다 */
    @Override
    @Transactional
    public void deleteAllOf(Long memberId) {
        portfolioRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)
                .forEach(Portfolio::markDeleted);
    }
}
