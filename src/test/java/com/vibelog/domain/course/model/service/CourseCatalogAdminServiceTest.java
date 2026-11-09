package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.AdminCategoryItem;
import com.vibelog.domain.course.model.dto.AdminTrackItem;
import com.vibelog.domain.course.model.dto.CategoryForm;
import com.vibelog.domain.course.model.dto.TrackForm;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 트랙·카테고리 관리 (API-ADM-02~10, #95)
 * - 이름이 겹치면 DUPLICATE_NAME, 트랙 순서가 겹치면 SORT_ORDER_TAKEN (수정할 때 자기 자신은 괜찮다)
 * - 코스가 있는 트랙·카테고리는 지워지지 않는다 (TRACK_HAS_COURSES·CATEGORY_HAS_COURSES)
 * - 카테고리 순서는 모든 ID 를 새 순서대로 받아야 한다
 * DB 에 샘플 트랙·카테고리가 있을 수 있어 이 테스트가 넣은 값(990x, 이름 앞 "관리")만 본다.
 */
@SpringBootTest
@Transactional
class CourseCatalogAdminServiceTest {

    @Autowired
    CourseCatalogAdminService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9951, '관리 트랙A', '대상', '설명', 9951), (9952, '관리 트랙B', '대상', '설명', 9952)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES "
                + "(9951, '관리 카테고리A', 9951), (9952, '관리 카테고리B', 9952)");
        // 트랙A·카테고리A 에만 코스가 있다
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9951, 9951, 9951, '관리 코스', '한 줄', '소개', '대상', '목표', 7)");
    }

    @Test
    void 트랙_목록은_순서대로_코스_수와_함께() {
        List<AdminTrackItem> tracks = service.getTracks().stream().filter(t -> t.name().startsWith("관리")).toList();

        assertThat(tracks).extracting(AdminTrackItem::name).containsExactly("관리 트랙A", "관리 트랙B");
        assertThat(tracks).extracting(AdminTrackItem::courseCount).containsExactly(1L, 0L);
    }

    @Test
    void 트랙_이름이나_순서가_겹치면_만들_수_없다() {
        assertError(() -> service.createTrack(TrackForm.of("관리 트랙A", "대상", "설명", 9953)), CourseErrorCode.DUPLICATE_NAME);
        assertError(() -> service.createTrack(TrackForm.of("관리 트랙C", "대상", "설명", 9951)), CourseErrorCode.SORT_ORDER_TAKEN);

        service.createTrack(TrackForm.of("  관리 트랙C  ", "대상", "설명", 9953));   // 앞뒤 공백은 지운다
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM track WHERE name = '관리 트랙C' AND sort_order = 9953", Integer.class)).isOne();
    }

    @Test
    void 트랙을_고칠_때_자기_이름과_순서는_그대로_둬도_된다() {
        service.updateTrack(9952L, TrackForm.of("관리 트랙B", "새 대상", "새 설명", 9952));
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT target FROM track WHERE track_id = 9952", String.class)).isEqualTo("새 대상");

        assertError(() -> service.updateTrack(9952L, TrackForm.of("관리 트랙A", "대상", "설명", 9952)), CourseErrorCode.DUPLICATE_NAME);
        assertError(() -> service.updateTrack(9952L, TrackForm.of("관리 트랙B", "대상", "설명", 9951)), CourseErrorCode.SORT_ORDER_TAKEN);
        assertError(() -> service.updateTrack(99999L, TrackForm.of("없음", "대상", "설명", 1)), CourseErrorCode.TRACK_NOT_FOUND);
    }

    @Test
    void 코스가_있는_트랙은_지워지지_않고_없는_트랙은_지워진다() {
        assertError(() -> service.deleteTrack(9951L), CourseErrorCode.TRACK_HAS_COURSES);

        service.deleteTrack(9952L);
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM track WHERE track_id IN (9951, 9952)", Integer.class)).isOne();
    }

    @Test
    void 진단_보기가_쓰는_트랙은_코스가_없어도_지워지지_않는다() {
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order) VALUES (9951, '관리 문항', 9951)");
        jdbcTemplate.update("INSERT INTO diagnosis_option (diagnosis_option_id, diagnosis_question_id, track_id, content, sort_order) "
                + "VALUES (9951, 9951, 9952, '관리 보기', 1)");

        assertError(() -> service.deleteTrack(9952L), CourseErrorCode.TRACK_IN_USE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM track WHERE track_id = 9952", Integer.class)).isOne();
    }

    @Test
    void 카테고리는_이름이_겹치면_안_되고_새로_만들면_맨_뒤_순서다() {
        assertError(() -> service.createCategory(CategoryForm.of("관리 카테고리A")), CourseErrorCode.DUPLICATE_NAME);

        service.createCategory(CategoryForm.of("관리 카테고리C"));
        Integer maxOrder = jdbcTemplate.queryForObject("SELECT MAX(sort_order) FROM course_category", Integer.class);
        assertThat(jdbcTemplate.queryForObject("SELECT sort_order FROM course_category WHERE name = '관리 카테고리C'", Integer.class))
                .isEqualTo(maxOrder);
    }

    @Test
    void 코스가_있는_카테고리는_지워지지_않는다() {
        assertError(() -> service.deleteCategory(9951L), CourseErrorCode.CATEGORY_HAS_COURSES);
        assertError(() -> service.deleteCategory(99999L), CourseErrorCode.CATEGORY_NOT_FOUND);

        service.deleteCategory(9952L);
        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_category WHERE course_category_id = 9952", Integer.class)).isZero();
    }

    @Test
    void 카테고리_순서는_모든_ID_를_받아_1부터_다시_매긴다() {
        List<Long> all = service.getCategories().stream().map(AdminCategoryItem::courseCategoryId).toList();
        List<Long> reversed = new java.util.ArrayList<>(all);
        java.util.Collections.reverse(reversed);

        service.reorderCategories(reversed);
        entityManager.flush();

        assertThat(service.getCategories()).extracting(AdminCategoryItem::courseCategoryId).containsExactlyElementsOf(reversed);
        assertThat(jdbcTemplate.queryForObject("SELECT sort_order FROM course_category WHERE course_category_id = ?", Integer.class, reversed.get(0)))
                .isOne();
    }

    @Test
    void 카테고리_순서에_빠지거나_없거나_겹치는_ID_가_있으면_INVALID_INPUT() {
        List<Long> all = service.getCategories().stream().map(AdminCategoryItem::courseCategoryId).toList();

        assertError(() -> service.reorderCategories(all.subList(0, all.size() - 1)), CourseErrorCode.INVALID_INPUT);   // 빠짐
        List<Long> withUnknown = new java.util.ArrayList<>(all.subList(0, all.size() - 1));
        withUnknown.add(99999L);
        assertError(() -> service.reorderCategories(withUnknown), CourseErrorCode.INVALID_INPUT);                      // 없는 ID
        List<Long> withDuplicate = new java.util.ArrayList<>(all.subList(0, all.size() - 1));
        withDuplicate.add(all.get(0));
        assertError(() -> service.reorderCategories(withDuplicate), CourseErrorCode.INVALID_INPUT);                    // 겹침
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(expected);
    }
}
