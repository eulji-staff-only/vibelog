package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dao.CourseCategoryRepository;
import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.dto.CourseAdminDetail;
import com.vibelog.domain.course.model.dto.CourseAdminRow;
import com.vibelog.domain.course.model.dto.CourseForm;
import com.vibelog.domain.course.model.dto.CourseOption;
import com.vibelog.domain.course.model.entity.Course;
import com.vibelog.domain.course.model.entity.CourseCategory;
import com.vibelog.domain.course.model.entity.Track;
import com.vibelog.domain.lesson.model.dto.CourseEnrollmentStat;
import com.vibelog.domain.lesson.model.policy.CourseLessonInfo;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.portfolio.model.policy.CourseAssignmentChecker;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * [CourseAdminService]
 * 역할 : 운영자의 코스 관리 (A-04 목록, A-05 만들기·상세·수정, API-ADM-11~15, FR-CRS-005, #96 / 공개·비공개 API-ADM-16, FR-CRS-006, #117).
 * 책임 : - 새 코스는 비공개로 저장한다 (Course.create).
 *        - 공개는 공개된 수업(계약 ⑨ lesson)과 포트폴리오 과제(계약 ⑨ portfolio)가 모두 있어야 한다 (FR-CRS-006). 비공개는 조건 없이 된다.
 *        - 트랙·카테고리는 있는 것만 고를 수 있다. 없는 ID 면 TRACK_NOT_FOUND·CATEGORY_NOT_FOUND (컨트롤러가 칸 메시지로 바꾼다).
 *        - 수강자 수·수업 목록은 lesson 의 LessonReader, 포트폴리오 과제가 있는지는 계약 ⑨로 받는다 (다른 영역 Entity 를 보지 않는다).
 *        - 운영자인지는 SecurityConfig(/admin/**) 가 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final TrackRepository trackRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final LessonReader lessonReader;
    private final CourseAssignmentChecker courseAssignmentChecker;
    private final CourseLessonInfo courseLessonInfo;

    /* API-ADM-11. 트랙·카테고리로 거를 수 있고(null 이면 전체), 만든 순서대로. 수강자 수는 지금까지 수강을 시작한 수 */
    public List<CourseAdminRow> getCourses(Long trackId, Long courseCategoryId) {
        Map<Long, String> trackNames = trackRepository.findAll().stream()
                .collect(Collectors.toMap(Track::getTrackId, Track::getName));
        Map<Long, String> categoryNames = courseCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(CourseCategory::getCourseCategoryId, CourseCategory::getName));
        Map<Long, Long> enrollmentCounts = lessonReader.getEnrollmentStatsByCourse(null, null).stream()
                .collect(Collectors.toMap(CourseEnrollmentStat::courseId, CourseEnrollmentStat::startedCount));
        return courseRepository.findAll(Sort.by("courseId")).stream()
                .filter(course -> trackId == null || course.getTrackId().equals(trackId))
                .filter(course -> courseCategoryId == null || course.getCourseCategoryId().equals(courseCategoryId))
                .map(course -> new CourseAdminRow(course.getCourseId(), course.getTitle(),
                        trackNames.get(course.getTrackId()), categoryNames.get(course.getCourseCategoryId()),
                        course.isPublic(), enrollmentCounts.getOrDefault(course.getCourseId(), 0L)))
                .toList();
    }

    /* 폼·필터의 트랙 고르기 칸, 트랙 순서대로 */
    public List<CourseOption> getTrackOptions() {
        return trackRepository.findAllByOrderBySortOrderAsc().stream()
                .map(track -> new CourseOption(track.getTrackId(), track.getName()))
                .toList();
    }

    /* 폼·필터의 카테고리 고르기 칸, 카테고리 순서대로 */
    public List<CourseOption> getCategoryOptions() {
        return courseCategoryRepository.findAllByOrderBySortOrderAscCourseCategoryIdAsc().stream()
                .map(category -> new CourseOption(category.getCourseCategoryId(), category.getName()))
                .toList();
    }

    /* API-ADM-13. 비공개로 저장하고 새 코스 ID 를 돌려준다 */
    @Transactional
    public Long createCourse(CourseForm form) {
        checkTrackAndCategory(form);
        Course course = Course.create(form.getTrackId(), form.getCourseCategoryId(), form.getTitle(), form.getSummary(),
                form.getDescription(), form.getTarget(), form.getGoal(), form.getPlanDays());
        return courseRepository.save(course).getCourseId();
    }

    /* API-ADM-14 */
    public CourseAdminDetail getCourse(Long courseId) {
        Course course = findCourse(courseId);
        String trackName = trackRepository.findById(course.getTrackId()).map(Track::getName).orElse(null);
        String categoryName = courseCategoryRepository.findById(course.getCourseCategoryId())
                .map(CourseCategory::getName).orElse(null);
        return new CourseAdminDetail(course.getCourseId(), course.getTitle(), course.isPublic(), trackName, categoryName,
                lessonReader.getLessonsOfCourse(courseId), courseAssignmentChecker.hasAssignment(courseId),
                courseLessonInfo.hasPublicLesson(courseId));
    }

    /* 상세 화면의 수정 폼에 채울 값 */
    public CourseForm getCourseForm(Long courseId) {
        return CourseForm.from(findCourse(courseId));
    }

    /* API-ADM-15. 공개 상태는 그대로 둔다 */
    @Transactional
    public void updateCourse(Long courseId, CourseForm form) {
        Course course = findCourse(courseId);
        checkTrackAndCategory(form);
        course.change(form.getTrackId(), form.getCourseCategoryId(), form.getTitle(), form.getSummary(),
                form.getDescription(), form.getTarget(), form.getGoal(), form.getPlanDays());
    }

    /*
     * API-ADM-16. 바꿨으면 true. 공개 조건이 모자라 공개하지 못하면 false (COURSE_NOT_PUBLISHABLE).
     * 실패를 예외로 던지지 않는 이유: 명세 방식이 "상세 화면 + 토스트"(③)라 오류 화면으로 보내지 않는다 (수업 공개 API-ADM-33 과 같은 방식)
     */
    @Transactional
    public boolean changeVisibility(Long courseId, boolean isPublic) {
        Course course = findCourse(courseId);
        if (isPublic && !(courseLessonInfo.hasPublicLesson(courseId) && courseAssignmentChecker.hasAssignment(courseId))) {
            return false;
        }
        course.changeVisibility(isPublic);
        return true;
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
    }

    private void checkTrackAndCategory(CourseForm form) {
        if (!trackRepository.existsById(form.getTrackId())) {
            throw new BusinessException(CourseErrorCode.TRACK_NOT_FOUND);
        }
        if (!courseCategoryRepository.existsById(form.getCourseCategoryId())) {
            throw new BusinessException(CourseErrorCode.CATEGORY_NOT_FOUND);
        }
    }
}
