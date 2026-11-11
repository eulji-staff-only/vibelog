package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dao.ConceptBookmarkRepository;
import com.vibelog.domain.lesson.model.dao.ConceptTagRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.model.dto.BookmarkedConceptSummary;
import com.vibelog.domain.lesson.model.dto.CourseEnrollmentStat;
import com.vibelog.domain.lesson.model.dto.CourseLessonRow;
import com.vibelog.domain.lesson.model.dto.DropOffRate;
import com.vibelog.domain.lesson.model.dto.MemberEnrollmentSummary;
import com.vibelog.domain.lesson.model.dto.QuizActivityPlace;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityTag;
import com.vibelog.domain.lesson.model.entity.ConceptBookmark;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [LessonReader]
 * 역할 : 다른 영역이 lesson 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonReader {

    private final EnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ActivityProgressRepository activityProgressRepository;
    private final ConceptTagRepository conceptTagRepository;
    private final ActivityTagRepository activityTagRepository;
    private final CourseReader courseReader;
    private final ConceptBookmarkRepository conceptBookmarkRepository;

    /* 마이페이지 북마크 탭 한 쪽 개수 (auth API-USR-15 P2) */
    static final int BOOKMARK_PAGE_SIZE = 20;

    /*
     * 회원이 북마크한 개념, 북마크한 시각 최신순 20개씩. page 는 1부터, 0 이하는 1쪽 (lesson 명세 "다른 영역에 주는 읽기 값", #315).
     * 개념 이름·수업·코스 이름을 붙여 수업 화면 링크(/courses/{courseId}/lessons/{lessonId}?activity={activityId})를 만들 수 있게 한다.
     */
    public Page<BookmarkedConceptSummary> findBookmarkedConcepts(Long memberId, int page) {
        Page<ConceptBookmark> rows = conceptBookmarkRepository.findByMemberIdOrderByCreatedAtDescConceptBookmarkIdDesc(memberId,
                PageRequest.of(Math.max(page, 1) - 1, BOOKMARK_PAGE_SIZE));
        Map<Long, Activity> activities = activityRepository.findAllById(rows.map(ConceptBookmark::getActivityId).toList())
                .stream().collect(Collectors.toMap(Activity::getActivityId, a -> a));
        Map<Long, Lesson> lessons = lessonRepository.findAllById(activities.values().stream().map(Activity::getLessonId).toList())
                .stream().collect(Collectors.toMap(Lesson::getLessonId, l -> l));
        Map<Long, String> courseTitles = courseReader.getCourseTitles(lessons.values().stream().map(Lesson::getCourseId).toList());
        return rows.map(row -> {
            Activity activity = activities.get(row.getActivityId());
            Lesson lesson = lessons.get(activity.getLessonId());
            return new BookmarkedConceptSummary(lesson.getCourseId(), lesson.getLessonId(), activity.getActivityId(), activity.getTitle(),
                    courseTitles.get(lesson.getCourseId()), lesson.getTitle(), row.getCreatedAt());
        });
    }

    /*
     * 회원의 수강 목록과 진행률. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다.
     * 진행률 = 리포트까지 확인한 수업 수 ÷ 공개 수업 수, 저장하지 않고 계산한다 (PL-ENR-006). 시작한 순서대로.
     */
    // ponytail: 수강마다 쿼리 2번. 운영자 상세 화면 한 명분이라 그대로 둔다
    public List<MemberEnrollmentSummary> getEnrollmentsOf(Long memberId) {
        return enrollmentRepository.findByMemberIdOrderByStartedAtAsc(memberId).stream()
                .map(this::summaryOf)
                .toList();
    }

    /*
     * 지금 수강 중인 회원 ID (완료·포기하지 않은 수강이 1개 이상). 운영자 회원 목록의 "수강 중" 필터(API-ADM-37, auth #446)가 쓴다.
     * "수강 중"은 상세 화면의 수강 상태(수강 중·완료·포기)와 같은 기준이다
     */
    public List<Long> getLearningMemberIds() {
        return enrollmentRepository.findLearningMemberIds();
    }

    /* 코스의 마지막 공개 수업 ID, 공개 수업이 없으면 null. 포트폴리오 잠김 안내의 "리포트 보러 가기"(portfolio #112)가 쓴다 */
    public Long getLastLessonId(Long courseId) {
        List<Lesson> lessons = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId);
        return lessons.isEmpty() ? null : lessons.get(lessons.size() - 1).getLessonId();
    }

    /*
     * 이 코스를 지금 수강 중인지 (포기하지 않은 수강이 있음. 완료한 코스는 복습이라 true). 수강 기록이 없거나 포기했으면 false.
     * 포기한 회원이 리포트를 열 때 코스 상세로 보내려고 묻는다 (exam #439, API-EXM-05)
     */
    public boolean hasActiveEnrollment(Long memberId, Long courseId) {
        return enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, courseId).isPresent();
    }

    /* 수업 ID → 수업 제목. 없는 ID 는 결과에 빠진다. 운영자 회원 상세의 모의고사 결과(auth #119)가 쓴다 */
    public Map<Long, String> getLessonTitles(Collection<Long> lessonIds) {
        return lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getLessonId, Lesson::getTitle));
    }

    /* 그 수업의 개념 활동인가 (사용 중지 여부는 보지 않는다). 모의고사 문항의 연관 개념 검사(exam #103, PL-EXM-002)가 쓴다 */
    public boolean isConceptOf(Long lessonId, Long activityId) {
        return activityRepository.findById(activityId)
                .filter(activity -> activity.getLessonId().equals(lessonId) && activity.isConcept())
                .isPresent();
    }

    /* 활동 ID → 활동 제목. 없는 ID 는 결과에 빠진다. 운영자 회원 상세의 퀴즈 결과(auth #119)가 쓴다 */
    public Map<Long, String> getActivityTitles(Collection<Long> activityIds) {
        return activityRepository.findAllById(activityIds).stream()
                .collect(Collectors.toMap(Activity::getActivityId, Activity::getTitle));
    }

    /* 수업이 속한 코스 ID. 없는 수업이면 LESSON_NOT_FOUND. 모의고사 화면의 수업 화면 링크(exam #110)가 쓴다 */
    public Long getCourseId(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .map(Lesson::getCourseId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_NOT_FOUND));
    }

    /* 개념 태그 ID → 태그 이름. 없는 ID 는 결과에 빠진다. 퀴즈 채점의 개념 태그별 오답 비율(quiz #109, FR-LSN-007)이 쓴다 */
    public Map<Long, String> getConceptTagNames(Collection<Long> conceptTagIds) {
        return conceptTagRepository.findAllById(conceptTagIds).stream()
                .collect(Collectors.toMap(ConceptTag::getConceptTagId, ConceptTag::getName));
    }

    /* 퀴즈 활동 ID → 속한 코스·수업. 없는 활동은 결과에 빠진다. 틀린 문제 모아보기(API-LSN-10, quiz #190)의 필터·링크가 쓴다 */
    public Map<Long, QuizActivityPlace> getQuizActivityPlaces(Collection<Long> quizActivityIds) {
        List<Activity> activities = activityRepository.findAllById(quizActivityIds);
        Map<Long, Long> courseIdByLesson = lessonRepository.findAllById(activities.stream().map(Activity::getLessonId).distinct().toList())
                .stream().collect(Collectors.toMap(Lesson::getLessonId, Lesson::getCourseId));
        return activities.stream()
                .filter(activity -> courseIdByLesson.containsKey(activity.getLessonId()))
                .collect(Collectors.toMap(Activity::getActivityId,
                        activity -> new QuizActivityPlace(courseIdByLesson.get(activity.getLessonId()), activity.getLessonId())));
    }

    /*
     * 수업마다 개념 태그 ID → 그 태그가 붙은 사용 중인 개념 활동 ID (같은 태그가 여러 개념에 붙어 있으면 순서가 가장 앞인 것).
     * 사용 중지한 개념은 수업 화면에 보이지 않아 고르지 않는다. 틀린 문제의 "개념으로 가기"(API-LSN-10, quiz #190)가 쓴다
     */
    public Map<Long, Map<Long, Long>> getConceptActivityIdsByTag(Collection<Long> lessonIds) {
        List<Activity> concepts = activityRepository.findByLessonIdInAndIsActiveTrueOrderBySortOrderAsc(lessonIds).stream()
                .filter(Activity::isConcept)
                .toList();
        Map<Long, List<ActivityTag>> tagsByActivity = activityTagRepository.findByActivityIdIn(
                        concepts.stream().map(Activity::getActivityId).toList()).stream()
                .collect(Collectors.groupingBy(ActivityTag::getActivityId));
        Map<Long, Map<Long, Long>> result = new HashMap<>();
        for (Activity concept : concepts) {   // 순서대로 돌며 먼저 나온 개념을 남긴다
            Map<Long, Long> byTag = result.computeIfAbsent(concept.getLessonId(), lessonId -> new HashMap<>());
            for (ActivityTag tag : tagsByActivity.getOrDefault(concept.getActivityId(), List.of())) {
                byTag.putIfAbsent(tag.getConceptTagId(), concept.getActivityId());
            }
        }
        return result;
    }

    /*
     * 기간 내 수강 시작한 수강을 코스별로 묶은 집계 (운영자 대시보드 API-ADM-01, FR-ADM-002).
     * from·to 가 null 이면 그 쪽 기간 제한이 없다 (쿼리 파라미터 없으면 전체 기간).
     */
    public List<CourseEnrollmentStat> getEnrollmentStatsByCourse(LocalDateTime from, LocalDateTime to) {
        Map<Long, List<Enrollment>> byCourse = enrollmentRepository.findStartedBetween(from, to).stream()
                .collect(Collectors.groupingBy(Enrollment::getCourseId));
        return byCourse.entrySet().stream()
                .map(entry -> new CourseEnrollmentStat(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().filter(Enrollment::isCompleted).count()))
                .toList();
    }

    /*
     * 회원 ID → 그 회원이 수강을 시작한 코스 ID들 (완료·포기한 수강도 포함, 기간 제한 없음). 수강이 없는 회원은 결과에 빠진다.
     * 운영자 확장 대시보드(admin #194, FR-ADM-003)의 "추천 트랙 vs 실제 수강"이 쓴다
     */
    public Map<Long, Set<Long>> findEnrolledCourseIds(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentRepository.findByMemberIdIn(memberIds).stream()
                .collect(Collectors.groupingBy(Enrollment::getMemberId,
                        Collectors.mapping(Enrollment::getCourseId, Collectors.toSet())));
    }

    /*
     * 수업 ID → 그 수업이 기간 안에 열린 수와 그중 완료하지 못한 수 (lesson_progress.unlocked_at 이 기간 안인 것만, 완료 못 함 = completed_at 없음).
     * 진행 중인 학습자도 포함하므로 "아직 못 끝낸 비율"이다. from·to 가 null 이면 그 쪽 제한 없음. 열린 진도가 없는 수업은 결과에 빠진다.
     * 운영자 확장 대시보드(admin #194, FR-ADM-003)의 수업별 이탈률이 쓴다
     */
    public Map<Long, DropOffRate> getDropOffRateByLesson(LocalDateTime from, LocalDateTime to) {
        Map<Long, DropOffRate> rates = new HashMap<>();
        for (Object[] row : lessonProgressRepository.countDropOffByLessonBetween(from, to)) {
            rates.put((Long) row[0], new DropOffRate(((Long) row[1]).intValue(), ((Number) row[2]).intValue()));
        }
        return rates;
    }

    /*
     * 활동 ID → 그 활동이 기간 안에 열린 수와 그중 완료하지 못한 수 (activity_progress.unlocked_at 이 기간 안인 것만, 완료 못 함 = completed_at 없음).
     * 수업별(getDropOffRateByLesson)과 같은 기준이다. 진행 중인 학습자도 포함하므로 "아직 못 끝낸 비율"이다.
     * from·to 가 null 이면 그 쪽 제한 없음. 열린 진도가 없는 활동은 결과에 빠진다. 운영자 확장 대시보드(admin #364, FR-ADM-003)의 활동별 이탈률이 쓴다
     */
    public Map<Long, DropOffRate> getDropOffRateByActivity(LocalDateTime from, LocalDateTime to) {
        Map<Long, DropOffRate> rates = new HashMap<>();
        for (Object[] row : activityProgressRepository.countDropOffByActivityBetween(from, to)) {
            rates.put((Long) row[0], new DropOffRate(((Long) row[1]).intValue(), ((Number) row[2]).intValue()));
        }
        return rates;
    }

    /* 활동 ID → 속한 수업 ID. 없는 ID 는 결과에 빠진다. 운영자 확장 대시보드(admin #364)가 활동 이름에 수업·코스 이름을 붙일 때 쓴다 */
    public Map<Long, Long> getLessonIdsByActivity(Collection<Long> activityIds) {
        return activityRepository.findAllById(activityIds).stream()
                .collect(Collectors.toMap(Activity::getActivityId, Activity::getLessonId));
    }

    /* 수업 ID → 속한 코스 ID. 없는 ID 는 결과에 빠진다. 운영자 확장 대시보드(admin #194)가 수업 제목에 코스 이름을 붙일 때 쓴다 */
    public Map<Long, Long> getCourseIdsByLesson(Collection<Long> lessonIds) {
        return lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getLessonId, Lesson::getCourseId));
    }

    /* 코스의 모든 수업(비공개 포함), 순서대로. 운영자 코스 상세(A-05, API-ADM-14, course #96)의 수업 목록이 쓴다 */
    public List<CourseLessonRow> getLessonsOfCourse(Long courseId) {
        return lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId).stream()
                .map(lesson -> new CourseLessonRow(lesson.getLessonId(), lesson.getTitle(), lesson.getSortOrder(), lesson.isPublic()))
                .toList();
    }

    /* 퀴즈 활동이 속한 수업 ID. 퀴즈 활동이 아니거나 없는 활동이면 빈 값. 운영자 퀴즈 문항 등록(quiz #102, API-ADM-26)이 쓴다 */
    public Optional<Long> findLessonIdOfQuizActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .filter(Activity::isQuiz)
                .map(Activity::getLessonId);
    }

    /*
     * 제목에 q 가 들어 있는 공개 수업의 제목을 코스별로, 수업 순서대로. 코스가 공개인지는 부르는 쪽(course)이 거른다.
     * 코스 목록 검색(API-CRS-02 P2, FR-CRS-003, course #189)이 쓴다
     */
    public Map<Long, List<String>> searchPublicLessonTitles(String q) {
        return lessonRepository.findByIsPublicTrueAndTitleContainingOrderByCourseIdAscSortOrderAsc(q).stream()
                .collect(Collectors.groupingBy(Lesson::getCourseId, Collectors.mapping(Lesson::getTitle, Collectors.toList())));
    }

    private MemberEnrollmentSummary summaryOf(Enrollment enrollment) {
        List<Long> lessonIds = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(enrollment.getCourseId()).stream()
                .map(Lesson::getLessonId)
                .toList();
        long done = lessonIds.isEmpty() ? 0
                : lessonProgressRepository.countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(enrollment.getEnrollmentId(), lessonIds);
        int percent = lessonIds.isEmpty() ? 0 : (int) Math.round(done * 100.0 / lessonIds.size());
        return new MemberEnrollmentSummary(enrollment.getCourseId(), enrollment.getStartedAt(), percent,
                enrollment.isCompleted(), enrollment.isDropped());
    }
}
