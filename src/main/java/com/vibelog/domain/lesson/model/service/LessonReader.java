package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ConceptTagRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.CourseEnrollmentStat;
import com.vibelog.domain.lesson.model.dto.CourseLessonRow;
import com.vibelog.domain.lesson.model.dto.MemberEnrollmentSummary;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ConceptTagRepository conceptTagRepository;

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

    /* 코스의 마지막 공개 수업 ID, 공개 수업이 없으면 null. 포트폴리오 잠김 안내의 "리포트 보러 가기"(portfolio #112)가 쓴다 */
    public Long getLastLessonId(Long courseId) {
        List<Lesson> lessons = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId);
        return lessons.isEmpty() ? null : lessons.get(lessons.size() - 1).getLessonId();
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
