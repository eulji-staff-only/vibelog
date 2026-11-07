package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.course.model.policy.CourseReader;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [EnrollmentService]
 * 역할 : 수강 시작 화면 요청(API-ENR-01)을 처리한다.
 * 책임 : - 코스 존재·공개 확인은 CourseReader(계약)에 맡긴다.
 *        - 이미 수강 중이면 그대로 두고(두 번 눌러도 기록은 하나), 처음이면 수강·공개 수업별 진도를 만들고
 *          첫 수업·첫 개념을 연다 (enrollment.member_id·course_id 유니크 제약이 동시 요청도 막아 준다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final CourseReader courseReader;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ActivityRepository activityRepository;
    private final ActivityProgressRepository activityProgressRepository;

    @Transactional
    public void enroll(Long memberId, Long courseId) {
        if (!courseReader.isPublicCourse(courseId)) {
            throw new BusinessException(LessonErrorCode.COURSE_NOT_FOUND);
        }
        if (enrollmentRepository.findByMemberIdAndCourseId(memberId, courseId).isPresent()) {
            return;
        }

        Enrollment enrollment = enrollmentRepository.save(Enrollment.create(memberId, courseId, LocalDateTime.now()));
        List<Lesson> publicLessons = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId);
        for (int i = 0; i < publicLessons.size(); i++) {
            unlockLessonProgress(enrollment.getEnrollmentId(), publicLessons.get(i), i == 0);
        }
    }

    /* 공개 수업마다 진도를 만들고, 첫 수업이면 그 수업과 첫 개념을 함께 연다 */
    private void unlockLessonProgress(Long enrollmentId, Lesson lesson, boolean isFirstLesson) {
        LocalDateTime unlockedAt = isFirstLesson ? LocalDateTime.now() : null;
        lessonProgressRepository.save(LessonProgress.create(enrollmentId, lesson.getLessonId(), unlockedAt));

        if (isFirstLesson) {
            activityRepository.findFirstByLessonIdAndIsActiveTrueOrderBySortOrderAsc(lesson.getLessonId())
                    .ifPresent(firstActivity -> activityProgressRepository.save(
                            ActivityProgress.create(enrollmentId, firstActivity.getActivityId(), LocalDateTime.now())));
        }
    }
}
