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
 *        - 포기한 수강은 수강 안 함과 같게 locked 로 돌려주고 기록하지 않는다 (#306, 수업 화면이 코스 상세로 돌려보낸다).
 *        - 완료 기록은 처음 한 번만 하고(ActivityProgress.complete), 바로 뒤 활동(항상 퀴즈, PL-LSN-001)을 연다.
 *          "다음 활동 열기"는 LessonContractService.unlockActivity(계약 ①)와 같은 계산이지만, 그 구현 클래스를
 *          직접 주입하면 그 클래스가 구현하는 다른 계약 인터페이스를 모킹하는 테스트(SecurityConfigTest 등)가
 *          lessonContractService 빈을 다른 타입으로 바꿔치기할 때 이 조회까지 같이 깨지므로 여기서 직접 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonActivityService {

    private final LessonRepository lessonRepository;
    private final ActivityRepository activityRepository;
    private final ActivityProgressRepository activityProgressRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public CompleteActivityResult completeConcept(Long memberId, Long courseId, Long lessonId, Long activityId) {
        getLesson(lessonId, courseId);
        Activity activity = getActivity(activityId, lessonId);
        if (!activity.isConcept()) {
            // 퀴즈는 정답률을 통과해야만 완료된다 (퀴즈 영역 몫). 이 요청으로 완료 처리하지 않는다
            throw new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND);
        }

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, courseId);
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
            unlockActivity(enrollment.getEnrollmentId(), next.get());
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

    /* LessonContractService.unlockActivity(계약 ①)와 같은 계산 (이미 기록돼 있으면 다시 만들지 않고 연다) */
    private void unlockActivity(Long enrollmentId, Activity activity) {
        activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity.getActivityId())
                .orElseGet(() -> activityProgressRepository.save(
                        ActivityProgress.create(enrollmentId, activity.getActivityId(), null)))
                .unlock();
    }
}
