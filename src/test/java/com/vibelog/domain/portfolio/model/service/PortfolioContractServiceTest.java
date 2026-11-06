package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.model.dao.PortfolioAssignmentRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import com.vibelog.domain.portfolio.model.policy.CourseAssignmentChecker;
import com.vibelog.domain.portfolio.model.policy.LessonPortfolioChecker;
import com.vibelog.domain.portfolio.model.policy.MemberPortfolioCleaner;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * portfolio 계약 ⑧⑨⑩과 PortfolioReader 를 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자, 2 운영자)을 쓰고, 코스는 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9001, '테스트 트랙', '대상', '설명', 9001)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9001, '테스트 카테고리', 9001)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9001, 9001, 9001, '코스 A', '요약', '설명', '대상', '목표', 7), "
                + "(9002, 9001, 9001, '코스 B', '요약', '설명', '대상', '목표', 7)"
})
class PortfolioContractServiceTest {

    static final long LEARNER = 1L;
    static final long OTHER = 2L;
    static final long COURSE_A = 9001L;
    static final long COURSE_B = 9002L;

    @Autowired LessonPortfolioChecker lessonPortfolioChecker;
    @Autowired CourseAssignmentChecker courseAssignmentChecker;
    @Autowired MemberPortfolioCleaner memberPortfolioCleaner;
    @Autowired PortfolioReader portfolioReader;
    @Autowired PortfolioRepository portfolioRepository;
    @Autowired PortfolioAssignmentRepository portfolioAssignmentRepository;
    @Autowired EntityManager em;

    @Test
    void 계약8_제출한_포트폴리오가_있으면_true() {
        submit(LEARNER, COURSE_A);

        assertThat(lessonPortfolioChecker.hasSubmitted(LEARNER, COURSE_A)).isTrue();
        assertThat(lessonPortfolioChecker.hasSubmitted(LEARNER, COURSE_B)).isFalse();
    }

    @Test
    void 계약8_삭제_표시된_포트폴리오만_있으면_false() {
        submit(LEARNER, COURSE_A).markDeleted();
        em.flush();

        assertThat(lessonPortfolioChecker.hasSubmitted(LEARNER, COURSE_A)).isFalse();
    }

    @Test
    void 계약9_과제가_있는_코스만_true() {
        portfolioAssignmentRepository.saveAndFlush(PortfolioAssignment.create(COURSE_A, "주제", "목표", "안내", null));

        assertThat(courseAssignmentChecker.hasAssignment(COURSE_A)).isTrue();
        assertThat(courseAssignmentChecker.hasAssignment(COURSE_B)).isFalse();
    }

    @Test
    void 계약10_그_회원의_포트폴리오만_모두_삭제_표시된다() {
        Portfolio a = submit(LEARNER, COURSE_A);
        Portfolio b = submit(LEARNER, COURSE_B);
        Portfolio others = submit(OTHER, COURSE_A);

        memberPortfolioCleaner.deleteAllOf(LEARNER);
        em.flush();
        em.clear();

        assertThat(portfolioRepository.findById(a.getPortfolioId()).orElseThrow().isDeleted()).isTrue();
        assertThat(portfolioRepository.findById(b.getPortfolioId()).orElseThrow().isDeleted()).isTrue();
        assertThat(portfolioRepository.findById(others.getPortfolioId()).orElseThrow().isDeleted()).isFalse();
        assertThat(lessonPortfolioChecker.hasSubmitted(LEARNER, COURSE_A)).isFalse();
    }

    @Test
    void Reader_포트폴리오_수는_삭제_표시를_빼고_센다() {
        submit(LEARNER, COURSE_A);
        submit(LEARNER, COURSE_B).markDeleted();
        em.flush();

        assertThat(portfolioReader.countOf(LEARNER)).isEqualTo(1);
    }

    private Portfolio submit(long memberId, long courseId) {
        return portfolioRepository.saveAndFlush(Portfolio.create(memberId, courseId, "제목", "https://github.com/x/y",
                "요약", "회고", "Claude", true, null, null));
    }
}
