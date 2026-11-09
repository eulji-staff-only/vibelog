package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /*
     * 회원이 북마크한 개념, 북마크한 순서 최신순 (concept_bookmark, FR-LSN-004). 마이페이지 목록은 읽기만 해서 행 값만 읽는다.
     * 북마크 Entity·토글(API-LSN-08)은 #192 에서 만든다.
     */
    @Query(value = "SELECT activity_id AS activityId, created_at AS bookmarkedAt FROM concept_bookmark WHERE member_id = :memberId"
            + " ORDER BY created_at DESC, concept_bookmark_id DESC",
            countQuery = "SELECT COUNT(*) FROM concept_bookmark WHERE member_id = :memberId", nativeQuery = true)
    Page<BookmarkRow> findBookmarksOf(@Param("memberId") Long memberId, Pageable pageable);

    interface BookmarkRow {
        Long getActivityId();

        LocalDateTime getBookmarkedAt();
    }

    List<Activity> findByLessonIdAndIsActiveTrueOrderBySortOrderAsc(Long lessonId);

    /* 운영자 수업 편집 화면(API-ADM-19, #100): 사용 중지된 활동도 함께 보여준다 */
    List<Activity> findByLessonIdOrderBySortOrderAsc(Long lessonId);

    Optional<Activity> findTopByLessonIdAndIsActiveTrueOrderBySortOrderDesc(Long lessonId);

    long countByLessonIdAndActivityTypeAndIsActiveTrue(Long lessonId, ActivityType activityType);

    Optional<Activity> findFirstByLessonIdAndIsActiveTrueOrderBySortOrderAsc(Long lessonId);

    Optional<Activity> findFirstByLessonIdAndIsActiveTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
            Long lessonId, int sortOrder);

    /* 잠긴 활동 선택 시 "앞 활동 바로가기"(FR-LSN-009)에 쓴다 */
    Optional<Activity> findFirstByLessonIdAndIsActiveTrueAndSortOrderLessThanOrderBySortOrderDesc(
            Long lessonId, int sortOrder);

    Optional<Activity> findTopByLessonIdAndActivityTypeAndIsActiveTrueOrderBySortOrderDesc(
            Long lessonId, ActivityType activityType);

    /* 새 활동의 다음 순서 계산(API-ADM-21, #101). uk_activity_lesson_sort 가 사용 중지 활동도 포함해 걸려 있어 전체에서 구한다 */
    Optional<Activity> findTopByLessonIdOrderBySortOrderDesc(Long lessonId);
}
