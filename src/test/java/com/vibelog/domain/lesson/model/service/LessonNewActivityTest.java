package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.ActivityFormRequest;
import com.vibelog.domain.lesson.model.dto.ActivityStatus;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.domain.lesson.model.policy.LessonExamOpenPolicy;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 완료한 수업에 활동이 추가될 때(PL-ENR-005 "새 활동으로 표시하고 선택 학습으로 둔다", #379)를 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자)과 직접 넣은 회원(9801)을 쓰고, 코스·수업·활동은 테스트 안에서만 넣는다. 끝나면 모두 롤백된다.
 *
 * 테스트 코스 (9801), 수업 9801·9802(공개), 활동은 모두 2026-01-01에 만들어짐
 *  - 수업 9801: 개념 9811 → 퀴즈 9812. 수강 E1(회원 1)은 이 수업을 02-01에 끝냄(두 활동 모두 완료). 수강 E2(회원 9801)는 아직 못 끝냄(퀴즈 9812 진행 중)
 *  - 수업 9802: 개념 9821 → 퀴즈 9822. E1은 이 수업을 열었지만 아직 못 끝냄(개념 9821 진행 중)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (9801, 'new-activity@vibelog.test', '새활동')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9801, '테스트 트랙', '대상', '설명', 9801)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9801, '테스트 카테고리', 9801)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9801, 9801, 9801, '코스 9801', '요약', '설명', '대상', '목표', 7, TRUE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9801, 9801, '수업 1', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9802, 9801, '수업 2', '소개', '목표', '본문', 20, 2, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, sort_order, is_active, created_at) VALUES "
                + "(9811, 9801, 'CONCEPT', '개념 1-1', '본문', 'MARKDOWN', 10, 1, TRUE, '2026-01-01 00:00:00'), "
                + "(9812, 9801, 'QUIZ', '퀴즈 1-1', NULL, NULL, 10, 2, TRUE, '2026-01-01 00:00:00'), "
                + "(9821, 9802, 'CONCEPT', '개념 2-1', '본문', 'MARKDOWN', 10, 1, TRUE, '2026-01-01 00:00:00'), "
                + "(9822, 9802, 'QUIZ', '퀴즈 2-1', NULL, NULL, 10, 2, TRUE, '2026-01-01 00:00:00')",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at) VALUES "
                + "(9801, 1, 9801, '2026-01-02 00:00:00'), (9802, 9801, 9801, '2026-01-02 00:00:00')",
        "INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at, completed_at) VALUES "
                + "(9801, 9801, '2026-01-02 00:00:00', '2026-02-01 00:00:00'), "
                + "(9801, 9802, '2026-02-01 00:00:00', NULL), "
                + "(9802, 9801, '2026-01-02 00:00:00', NULL)",
        "INSERT INTO activity_progress (enrollment_id, activity_id, unlocked_at, completed_at) VALUES "
                + "(9801, 9811, '2026-01-02 00:00:00', '2026-01-10 00:00:00'), "
                + "(9801, 9812, '2026-01-10 00:00:00', '2026-01-11 00:00:00'), "
                + "(9801, 9821, '2026-02-01 00:00:00', NULL), "
                + "(9802, 9811, '2026-01-02 00:00:00', '2026-01-10 00:00:00'), "
                + "(9802, 9812, '2026-01-10 00:00:00', NULL)"
})
class LessonNewActivityTest {

    static final long E1 = 9801L;   // 수업 9801을 끝낸 수강 (회원 1)
    static final long E2 = 9802L;   // 수업 9801을 아직 못 끝낸 수강 (회원 9801)
    static final long LESSON_1 = 9801L;
    static final long LESSON_2 = 9802L;

    @Autowired LessonActivityAdminService lessonActivityAdminService;
    @Autowired LessonPageService lessonPageService;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired LessonExamOpenPolicy lessonExamOpenPolicy;
    @Autowired EntityManager em;

    private long add(long lessonId, ActivityType type, String title) {
        ActivityFormRequest request = new ActivityFormRequest();
        request.setActivityType(type);
        request.setTitle(title);
        request.setBody(type == ActivityType.CONCEPT ? "본문" : null);
        return lessonActivityAdminService.addActivity(lessonId, request);
    }

    private List<LessonActivityCard> cardsOf(long memberId, long lessonId) {
        return lessonPageService.getPage(memberId, 9801L, lessonId, null, false).data().cards();
    }

    private LessonActivityCard cardOf(List<LessonActivityCard> cards, long activityId) {
        return cards.stream().filter(card -> Long.valueOf(activityId).equals(card.activityId())).findFirst().orElseThrow();
    }

    @Test
    void 수업을_끝낸_수강자에게는_새_개념이_열리고_아직_못_끝낸_수강자에게는_만들지_않는다() {
        long concept = add(LESSON_1, ActivityType.CONCEPT, "새 개념");

        ActivityProgress opened = activityProgressRepository.findByEnrollmentIdAndActivityId(E1, concept).orElseThrow();
        assertThat(opened.isUnlocked()).isTrue();
        assertThat(opened.isCompleted()).isFalse();
        // E2 는 수업 9801을 아직 못 끝냈다 → 순서대로 진행하면 닿으므로 손대지 않는다
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(E2, concept)).isEmpty();
    }

    @Test
    void 새_개념을_아직_안_끝냈으면_그_뒤에_추가한_퀴즈는_열지_않는다() {
        add(LESSON_1, ActivityType.CONCEPT, "새 개념");
        long quiz = add(LESSON_1, ActivityType.QUIZ, "새 퀴즈");

        // 앞 활동(새 개념)을 끝내야 열린다. 그 개념을 끝낼 때 기존 흐름이 다음 활동을 연다
        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(E1, quiz)).isEmpty();
    }

    @Test
    void 아직_끝내지_못한_수업에_활동을_추가해도_열어_주지_않는다() {
        long concept = add(LESSON_2, ActivityType.CONCEPT, "새 개념");

        assertThat(activityProgressRepository.findByEnrollmentIdAndActivityId(E1, concept)).isEmpty();
    }

    @Test
    void 수업_화면은_끝낸_뒤_추가된_활동에만_새_활동을_표시한다() {
        long concept = add(LESSON_1, ActivityType.CONCEPT, "새 개념");
        long quiz = add(LESSON_1, ActivityType.QUIZ, "새 퀴즈");

        List<LessonActivityCard> cards = cardsOf(1L, LESSON_1);

        assertThat(cardOf(cards, 9811L).newActivity()).isFalse();
        assertThat(cardOf(cards, 9812L).newActivity()).isFalse();
        assertThat(cardOf(cards, concept).newActivity()).isTrue();
        assertThat(cardOf(cards, concept).status()).isEqualTo(ActivityStatus.IN_PROGRESS);   // 선택 학습: 열어서 볼 수 있다
        assertThat(cardOf(cards, quiz).newActivity()).isTrue();
        assertThat(cardOf(cards, quiz).status()).isEqualTo(ActivityStatus.LOCKED);           // 앞 활동을 끝내야 열린다
        assertThat(cards.stream().filter(LessonActivityCard::isExam).findFirst().orElseThrow().newActivity()).isFalse();
    }

    @Test
    void 새_활동을_끝내면_더_이상_새_활동으로_표시하지_않는다() {
        long concept = add(LESSON_1, ActivityType.CONCEPT, "새 개념");
        activityProgressRepository.findByEnrollmentIdAndActivityId(E1, concept).orElseThrow().complete();
        em.flush();

        assertThat(cardOf(cardsOf(1L, LESSON_1), concept).newActivity()).isFalse();
    }

    @Test
    void 수업을_아직_못_끝낸_학습자에게는_나중에_추가된_활동도_새_활동이_아니다() {
        long concept = add(LESSON_1, ActivityType.CONCEPT, "새 개념");

        // E2(회원 9801)는 수업 9801을 끝내지 못했다. 순서대로 진행하면 닿는 다음 활동일 뿐이다
        assertThat(cardOf(cardsOf(9801L, LESSON_1), concept).newActivity()).isFalse();
    }

    @Test
    void 새_활동은_수업_완료와_모의고사_열림을_바꾸지_않는다() {
        // PL-ENR-005·006: 선택 학습이라 수업 완료·모의고사 열림에 영향이 없다 (열린 모의고사는 다시 잠그지 않는다)
        add(LESSON_1, ActivityType.CONCEPT, "새 개념");
        add(LESSON_1, ActivityType.QUIZ, "새 퀴즈");

        LessonProgress lessonProgress = lessonProgressRepository.findByEnrollmentIdAndLessonId(E1, LESSON_1).orElseThrow();
        assertThat(lessonProgress.isCompleted()).isTrue();
        assertThat(lessonExamOpenPolicy.isOpen(E1, LESSON_1)).isTrue();
    }
}
