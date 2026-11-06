package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.CompleteActivityResult;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/*
 * [LessonActivityService]
 * 역할 : 개념 읽기 완료 POST(API-LSN-02)를 처리한다.
 * 책임 : - 활동이 열려 있지 않으면(진도 없음·unlocked_at 없음) 에러 화면이 아니라 "수업 화면 + 토스트"(③, ACTIVITY_LOCKED)로
 *          돌려줄 수 있게 locked 결과를 돌려준다 (BusinessException 을 쓰지 않는다).
 *        - 완료 기록은 처음 한 번만 하고(ActivityProgress.complete), 바로 뒤 활동(항상 퀴즈, PL-LSN-001)을 연다.
 *          "다음 활동 열기"는 LessonContractService(계약 ①)의 것을 그대로 재사용한다 (복사하지 않음, #107).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonActivityService {

    private final LessonRepository lessonRepository;
    private final ActivityRepository activityRepository;
    private final ActivityProgressRepository activityProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonContractService lessonContractService;

    @Transactional
    public CompleteActivityResult completeConcept(Long memberId, Long courseId, Long lessonId, Long activityId) {
        getLesson(lessonId, courseId);
        Activity activity = getActivity(activityId, lessonId);

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByMemberIdAndCourseId(memberId, courseId);
        Optional<ActivityProgress> progressOpt = enrollmentOpt.flatMap(enrollment ->
                activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), activityId));
        boolean unlocked = progressOpt.map(ActivityProgress::isUnlocked).orElse(false);
        if (!unlocked) {
            return CompleteActivityResult.ofLocked();
        }

        progressOpt.get().complete();
        Enrollment enrollment = enrollmentOpt.get();

        Optional<Activity> next = activityRepository
                .findFirstByLessonIdAndIsActiveTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
                        lessonId, activity.getSortOrder());
        if (next.isPresent()) {
            lessonContractService.unlockActivity(enrollment.getEnrollmentId(), next.get());
            return CompleteActivityResult.redirectTo(
                    "/courses/" + courseId + "/lessons/" + lessonId + "/quizzes/" + next.get().getActivityId());
        }
        return CompleteActivityResult.redirectTo("/courses/" + courseId + "/lessons/" + lessonId);
    }

    private Lesson getLesson(Long lessonId, Long courseId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_NOT_FOUND));
        if (!lesson.getCourseId().equals(courseId)) {
            throw new BusinessException(LessonErrorCode.LESSON_NOT_FOUND);
        }
        return lesson;
    }

    private Activity getActivity(Long activityId, Long lessonId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND));
        if (!activity.getLessonId().equals(lessonId)) {
            throw new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }
}
