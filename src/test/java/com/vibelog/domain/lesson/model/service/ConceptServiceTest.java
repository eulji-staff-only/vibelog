package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ConceptBookmarkRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dto.BookmarkedConceptSummary;
import com.vibelog.domain.lesson.model.dto.ConceptSearchItem;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.LessonPageData;
import com.vibelog.domain.lesson.model.dto.RelatedConcept;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.ConceptBookmark;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 연관 개념(API-LSN-07)·개념 북마크(API-LSN-08)·개념 태그 검색(API-LSN-09)·북마크 목록(LessonReader)을 실제 DB 로 확인한다.
 * 회원·코스·수업·활동은 모두 테스트 안에서만 넣는다(로컬 DB 에 직접 눌러 본 북마크·수강이 있어도 영향이 없게). 끝나면 모두 롤백된다.
 *
 * 테스트 데이터
 *  - 코스 9201(공개): 수업 9201(공개), 수업 9202(비공개), 수업 9204(공개)
 *  - 코스 9202(비공개): 수업 9203(공개 - 수업만 공개이고 코스가 비공개인 경우)
 *  - 개념: 9201(수업 9201, 순서 1) · 9202(수업 9201, 순서 3) · 9203(비공개 수업) · 9204(비공개 코스) · 9206(수업 9204)
 *          · 9207(수업 9201, 사용 중지) / 퀴즈: 9205(수업 9201)
 *  - 태그 9201 은 위 활동 전부(9201~9207)에 붙어 있다
 *  - 연관 개념: 9201 → 9202(순서 2), 9203(순서 3), 9204(순서 4), 9206(순서 1)
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, password_hash, nickname, role, status, is_test) VALUES "
                + "(9201, 'concept9201@vibelog.test', 'x', '개념테스트1', 'LEARNER', 'ACTIVE', TRUE), "
                + "(9202, 'concept9202@vibelog.test', 'x', '개념테스트2', 'LEARNER', 'ACTIVE', TRUE)",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9201, '테스트 트랙', '대상', '설명', 9201)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9201, '테스트 카테고리', 9201)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) VALUES "
                + "(9201, 9201, 9201, '공개 코스 9201', '요약', '설명', '대상', '목표', 7, TRUE), "
                + "(9202, 9201, 9201, '비공개 코스 9202', '요약', '설명', '대상', '목표', 7, FALSE)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9201, 9201, '수업 9201', '소개', '목표', '본문', 30, 1, TRUE), "
                + "(9202, 9201, '비공개 수업 9202', '소개', '목표', '본문', 30, 2, FALSE), "
                + "(9204, 9201, '수업 9204', '소개', '목표', '본문', 30, 3, TRUE), "
                + "(9203, 9202, '수업 9203', '소개', '목표', '본문', 30, 1, TRUE)",
        "INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order, is_active) VALUES "
                + "(9201, 9201, 'CONCEPT', '개념 9201', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(9205, 9201, 'QUIZ', '퀴즈 9205', NULL, NULL, 10, 50, 2, TRUE), "
                + "(9202, 9201, 'CONCEPT', '개념 9202', '본문', 'MARKDOWN', 10, NULL, 3, TRUE), "
                + "(9207, 9201, 'CONCEPT', '개념 9207', '본문', 'MARKDOWN', 10, NULL, 4, FALSE), "
                + "(9203, 9202, 'CONCEPT', '개념 9203', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(9204, 9203, 'CONCEPT', '개념 9204', '본문', 'MARKDOWN', 10, NULL, 1, TRUE), "
                + "(9206, 9204, 'CONCEPT', '개념 9206', '본문', 'MARKDOWN', 10, NULL, 1, TRUE)",
        "INSERT INTO concept_tag (concept_tag_id, name) VALUES (9201, '테스트태그'), (9202, '빈태그')",
        "INSERT INTO activity_tag (activity_id, concept_tag_id) VALUES "
                + "(9201, 9201), (9202, 9201), (9203, 9201), (9204, 9201), (9205, 9201), (9206, 9201), (9207, 9201)",
        "INSERT INTO concept_relation (source_activity_id, related_activity_id, sort_order) VALUES "
                + "(9201, 9202, 2), (9201, 9203, 3), (9201, 9204, 4), (9201, 9206, 1)"
})
class ConceptServiceTest {

    static final long LEARNER = 9201L;
    static final long ADMIN = 9202L; // 다른 회원

    @Autowired ConceptService conceptService;
    @Autowired LessonReader lessonReader;
    @Autowired LessonPageService lessonPageService;
    @Autowired ConceptBookmarkRepository conceptBookmarkRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressRepository lessonProgressRepository;
    @Autowired ActivityProgressRepository activityProgressRepository;
    @Autowired EntityManager em;

    // ---- API-LSN-08 북마크 토글 ----

    @Test
    void 북마크를_누르면_켜지고_다시_누르면_꺼진다() {
        assertThat(conceptService.toggleBookmark(LEARNER, 9201L).bookmarked()).isTrue();
        assertThat(conceptBookmarkRepository.existsByMemberIdAndActivityId(LEARNER, 9201L)).isTrue();

        assertThat(conceptService.toggleBookmark(LEARNER, 9201L).bookmarked()).isFalse();
        assertThat(conceptBookmarkRepository.existsByMemberIdAndActivityId(LEARNER, 9201L)).isFalse();
    }

    @Test
    void 북마크는_회원마다_따로_저장된다() {
        conceptService.toggleBookmark(LEARNER, 9201L);

        assertThat(conceptService.toggleBookmark(ADMIN, 9201L).bookmarked()).isTrue();
        assertThat(conceptService.isBookmarked(LEARNER, 9201L)).isTrue();
    }

    @Test
    void 퀴즈_활동은_북마크할_수_없다() {
        assertThatThrownBy(() -> conceptService.toggleBookmark(LEARNER, 9205L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(LessonErrorCode.ACTIVITY_NOT_FOUND));
        assertThat(conceptBookmarkRepository.existsByMemberIdAndActivityId(LEARNER, 9205L)).isFalse();
    }

    @Test
    void 비공개_수업_비공개_코스_사용_중지_개념은_북마크할_수_없다() {
        for (long hidden : new long[]{9203L, 9204L, 9207L}) { // 비공개 수업 · 수업만 공개이고 코스는 비공개 · 사용 중지
            assertThatThrownBy(() -> conceptService.toggleBookmark(LEARNER, hidden))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(LessonErrorCode.ACTIVITY_NOT_FOUND));
            assertThat(conceptBookmarkRepository.existsByMemberIdAndActivityId(LEARNER, hidden)).isFalse();
        }
    }

    @Test
    void 이미_있는_북마크는_나중에_비공개가_되어도_해제할_수_있다() {
        conceptBookmarkRepository.saveAndFlush(ConceptBookmark.create(LEARNER, 9204L)); // 공개였을 때 북마크한 것으로 본다

        assertThat(conceptService.toggleBookmark(LEARNER, 9204L).bookmarked()).isFalse();
        assertThat(conceptBookmarkRepository.existsByMemberIdAndActivityId(LEARNER, 9204L)).isFalse();
    }

    @Test
    void 없는_활동은_북마크할_수_없다() {
        assertThatThrownBy(() -> conceptService.toggleBookmark(LEARNER, 9999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(LessonErrorCode.ACTIVITY_NOT_FOUND));
    }

    // ---- API-LSN-09 개념 태그 검색 ----

    @Test
    void 태그로_검색하면_공개_수업과_공개_코스의_개념만_순서대로_나온다() {
        assertThat(conceptService.searchByTag(9201L))
                .extracting(ConceptSearchItem::activityId)
                .containsExactly(9201L, 9202L, 9206L);
    }

    @Test
    void 검색_결과에는_코스와_수업_제목이_함께_담긴다() {
        ConceptSearchItem first = conceptService.searchByTag(9201L).get(0);

        assertThat(first.title()).isEqualTo("개념 9201");
        assertThat(first.courseId()).isEqualTo(9201L);
        assertThat(first.lessonId()).isEqualTo(9201L);
        assertThat(first.courseTitle()).isEqualTo("공개 코스 9201");
        assertThat(first.lessonTitle()).isEqualTo("수업 9201");
    }

    @Test
    void 비공개_수업의_개념은_검색_결과에서_빠진다() {
        assertThat(conceptService.searchByTag(9201L))
                .extracting(ConceptSearchItem::activityId).doesNotContain(9203L);
    }

    @Test
    void 수업은_공개여도_코스가_비공개이면_검색_결과에서_빠진다() {
        assertThat(conceptService.searchByTag(9201L))
                .extracting(ConceptSearchItem::activityId).doesNotContain(9204L);
        assertThat(conceptService.searchByTag(9201L))
                .extracting(ConceptSearchItem::courseTitle).doesNotContain("비공개 코스 9202");
    }

    @Test
    void 퀴즈와_사용_중지된_활동은_검색_결과에서_빠진다() {
        assertThat(conceptService.searchByTag(9201L))
                .extracting(ConceptSearchItem::activityId).doesNotContain(9205L, 9207L);
    }

    @Test
    void conceptTagId가_없으면_빈_목록이다() {
        assertThat(conceptService.searchByTag(null)).isEmpty();
    }

    @Test
    void 달린_개념이_없는_태그는_빈_목록이다() {
        assertThat(conceptService.searchByTag(9202L)).isEmpty();
    }

    // ---- API-LSN-07 연관 개념 ----

    @Test
    void 연관_개념은_sort_order_순이고_보여줄_수_없는_개념은_빠진다() {
        assertThat(conceptService.findRelatedConcepts(9201L))
                .extracting(RelatedConcept::relatedActivityId)
                .containsExactly(9206L, 9202L);
    }

    @Test
    void 연관_개념에는_이동에_필요한_코스와_수업_ID가_담긴다() {
        RelatedConcept first = conceptService.findRelatedConcepts(9201L).get(0);

        assertThat(first.title()).isEqualTo("개념 9206");
        assertThat(first.courseId()).isEqualTo(9201L);
        assertThat(first.lessonId()).isEqualTo(9204L);
    }

    @Test
    void 연관_개념이_없으면_빈_목록이다() {
        assertThat(conceptService.findRelatedConcepts(9202L)).isEmpty();
    }

    // ---- API-LSN-01 응답에 반영 ----

    @Test
    void 수업_화면의_개념_카드에_연관_개념_태그_ID_북마크_여부가_담긴다() {
        openLessonFor(LEARNER);
        conceptService.toggleBookmark(LEARNER, 9201L);

        LessonActivityCard card = cardOf(
                lessonPageService.getPage(LEARNER, 9201L, 9201L, "9201", false).data(), 9201L);

        assertThat(card.relatedConcepts()).extracting(RelatedConcept::relatedActivityId).containsExactly(9206L, 9202L);
        assertThat(card.conceptTagIds()).containsExactly(9201L);
        assertThat(card.conceptTags()).containsExactly("테스트태그");
        assertThat(card.bookmarked()).isTrue();
    }

    @Test
    void 북마크하지_않은_개념_카드는_북마크_꺼짐이다() {
        openLessonFor(LEARNER);
        conceptService.toggleBookmark(ADMIN, 9201L); // 다른 회원의 북마크는 영향이 없다

        LessonActivityCard card = cardOf(
                lessonPageService.getPage(LEARNER, 9201L, 9201L, "9201", false).data(), 9201L);

        assertThat(card.bookmarked()).isFalse();
    }

    // ---- LessonReader.findBookmarkedConcepts ----

    @Test
    void 북마크_목록은_북마크한_시각_내림차순이다() {
        bookmarkAt(9201L, "2026-01-01 10:00:00");
        bookmarkAt(9206L, "2026-03-01 10:00:00");
        bookmarkAt(9202L, "2026-02-01 10:00:00");

        Page<BookmarkedConceptSummary> page = lessonReader.findBookmarkedConcepts(LEARNER, 1);

        assertThat(page.getContent()).extracting(BookmarkedConceptSummary::activityId)
                .containsExactly(9206L, 9202L, 9201L);
        BookmarkedConceptSummary first = page.getContent().get(0);
        assertThat(first.title()).isEqualTo("개념 9206");
        assertThat(first.courseId()).isEqualTo(9201L);
        assertThat(first.lessonId()).isEqualTo(9204L);
        assertThat(first.courseTitle()).isEqualTo("공개 코스 9201");
        assertThat(first.lessonTitle()).isEqualTo("수업 9204");
        assertThat(first.bookmarkedAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 0));
    }

    @Test
    void 북마크_목록은_내_북마크만_담는다() {
        bookmarkAt(9201L, "2026-01-01 10:00:00");
        conceptService.toggleBookmark(ADMIN, 9202L);

        assertThat(lessonReader.findBookmarkedConcepts(LEARNER, 1).getContent())
                .extracting(BookmarkedConceptSummary::activityId).containsExactly(9201L);
    }

    @Test
    void 북마크_목록은_20개씩_나눈다() {
        addConceptsAndBookmark(21);

        Page<BookmarkedConceptSummary> first = lessonReader.findBookmarkedConcepts(LEARNER, 1);
        Page<BookmarkedConceptSummary> second = lessonReader.findBookmarkedConcepts(LEARNER, 2);

        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getTotalElements()).isEqualTo(21);
        assertThat(first.getContent().get(0).activityId()).isEqualTo(9321L); // 가장 최근
        assertThat(second.getContent()).extracting(BookmarkedConceptSummary::activityId).containsExactly(9301L);
    }

    // ---- 도우미 ----

    /* 수업 9201 에 개념 활동(9301~)을 count 개 만들고 북마크한다. 번호가 클수록 최근 */
    private void addConceptsAndBookmark(int count) {
        for (int i = 1; i <= count; i++) {
            long activityId = 9300L + i;
            em.createNativeQuery("INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, "
                            + "estimated_minutes, pass_rate, sort_order) VALUES (" + activityId + ", 9201, 'CONCEPT', '북마크용 "
                            + i + "', '본문', 'MARKDOWN', 10, NULL, " + (100 + i) + ")").executeUpdate();
            bookmarkAt(activityId, "2026-04-01 10:00:" + String.format("%02d", i));
        }
    }

    private void bookmarkAt(long activityId, String createdAt) {
        em.createNativeQuery("INSERT INTO concept_bookmark (member_id, activity_id, created_at) VALUES ("
                + LEARNER + ", " + activityId + ", '" + createdAt + "')").executeUpdate();
    }

    private void openLessonFor(long memberId) {
        Enrollment enrollment = enrollmentRepository.saveAndFlush(Enrollment.create(memberId, 9201L, LocalDateTime.now()));
        lessonProgressRepository.saveAndFlush(LessonProgress.create(enrollment.getEnrollmentId(), 9201L, LocalDateTime.now()));
        activityProgressRepository.saveAndFlush(ActivityProgress.create(enrollment.getEnrollmentId(), 9201L, LocalDateTime.now()));
    }

    private LessonActivityCard cardOf(LessonPageData data, long activityId) {
        return data.cards().stream()
                .filter(card -> Long.valueOf(activityId).equals(card.activityId()))
                .findFirst()
                .orElseThrow();
    }
}
