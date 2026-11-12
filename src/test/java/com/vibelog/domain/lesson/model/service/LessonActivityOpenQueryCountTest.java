package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dto.ActivityFormRequest;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 활동 추가 때 수업을 끝낸 수강자에게 새 활동을 여는 흐름(PL-ENR-005, #379)이 수강자 수만큼 조회하지 않는지 확인한다 (#476, 점검표 P-1·P-4).
 * 실제 DB(01·02 실행된 로컬 MySQL)를 쓰고, 끝나면 모두 롤백된다.
 *
 * 테스트 코스 (8701), 수업 8701: 개념 8711 → 퀴즈 8712.
 * 회원 8701~8720(20명)이 모두 수강하고, 수업 8701과 퀴즈 8712를 끝냈다 → 새 활동이 20명 모두에게 열려야 한다.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class LessonActivityOpenQueryCountTest {

    static final int LEARNERS = 20;
    static final long LESSON = 8701L;

    @Autowired LessonActivityAdminService lessonActivityAdminService;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (8701, '쿼리 트랙', '대상', '설명', 8701)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (8701, '쿼리 분류', 8701)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (8701, 8701, 8701, '코스 8701', '요약', '설명', '대상', '목표', 7, TRUE)");
        jdbcTemplate.update("INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                + "VALUES (8701, 8701, '수업 1', '소개', '목표', '본문', 30, 1, TRUE)");
        jdbcTemplate.update("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order, is_active) VALUES "
                + "(8711, 8701, 'CONCEPT', '개념 1', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(8712, 8701, 'QUIZ', '퀴즈 1', NULL, NULL, 10, 50, 2, TRUE)");
        for (int i = 1; i <= LEARNERS; i++) {
            long id = 8700L + i;
            jdbcTemplate.update("INSERT INTO member (member_id, email, nickname) VALUES (?, ?, ?)", id, "open-" + i + "@vibelog.test", "열림" + i);
            jdbcTemplate.update("INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES (?, ?, 8701, NOW())", id, id);
            jdbcTemplate.update("INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at, completed_at) VALUES (?, 8701, NOW(), NOW())", id);
            jdbcTemplate.update("INSERT INTO activity_progress (enrollment_id, activity_id, unlocked_at, completed_at) VALUES "
                    + "(?, 8711, NOW(), NOW()), (?, 8712, NOW(), NOW())", id, id);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 수강자가_20명이어도_진도_조회는_수강자마다_나가지_않는다() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        ActivityFormRequest request = new ActivityFormRequest();
        request.setActivityType(ActivityType.CONCEPT);
        request.setTitle("새 개념");
        request.setBody("본문");
        long activityId = lessonActivityAdminService.addActivity(LESSON, request);
        entityManager.flush();

        // 동작은 그대로: 20명 모두에게 새 활동이 열린다
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_progress WHERE activity_id = ? AND unlocked_at IS NOT NULL", Integer.class, activityId))
                .isEqualTo(LEARNERS);
        // 마지막 순서 + 태그 정리 + 앞 활동 + 수강 목록 + 수업 진도 묶음 + 활동 진도 묶음. 수강자 수와 상관없이 늘지 않는다
        // (전: 수강자마다 수업 진도·활동 진도를 1번씩 → 20명이면 조회 쿼리 44번, SQL 66번)
        assertThat(statistics.getQueryExecutionCount()).as("SQL %d번, 조회 쿼리 %d번", statistics.getPrepareStatementCount(), statistics.getQueryExecutionCount()).isLessThanOrEqualTo(6);
    }
}
