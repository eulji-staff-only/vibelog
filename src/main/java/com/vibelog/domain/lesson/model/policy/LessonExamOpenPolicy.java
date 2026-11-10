package com.vibelog.domain.lesson.model.policy;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [LessonExamOpenPolicy]
 * 역할 : 수강(enrollment) 하나에서 어떤 수업의 모의고사가 열렸는지 판단한다 (PL-LSN-002, PL-ENR-006).
 * 책임 : - 기본 규칙: 그 수업 마지막 퀴즈를 통과(activity_progress.completed_at)하면 모의고사가 열린다.
 *        - 한 번 열린 것은 다시 잠그지 않는다 (PL-ENR-006). 열림 상태를 따로 저장하지 않으므로, 학습자가 마지막으로 퀴즈를
 *          통과한 시각보다 나중에 추가된 퀴즈(activity.created_at)는 "마지막 퀴즈" 판단에서 뺀다.
 *          그래서 모의고사가 열린 뒤 운영자가 수업 끝에 퀴즈를 추가해도 다시 잠기지 않는다.
 *        - 열리기 전에 이미 있던 퀴즈는 그대로 통과해야 한다 (마지막 퀴즈를 통과해야 열린다).
 *        - 수업 화면(LessonPageService)·내 학습(MyLearningService)·계약 ②(LessonContractService)가 같은 규칙을 쓰도록 여기 한 곳에 둔다.
 */
@Component
@RequiredArgsConstructor
public class LessonExamOpenPolicy {

    private final ActivityRepository activityRepository;
    private final ActivityProgressRepository activityProgressRepository;

    public boolean isOpen(Long enrollmentId, Long lessonId) {
        List<Activity> quizzes = activityRepository.findByLessonIdAndIsActiveTrueOrderBySortOrderAsc(lessonId).stream()
                .filter(Activity::isQuiz)
                .toList();
        if (quizzes.isEmpty()) {
            return false;
        }

        Map<Long, ActivityProgress> progressByActivityId = activityProgressRepository
                .findByEnrollmentIdAndActivityIdIn(enrollmentId, quizzes.stream().map(Activity::getActivityId).toList())
                .stream()
                .collect(Collectors.toMap(ActivityProgress::getActivityId, Function.identity()));

        LocalDateTime lastPassedAt = progressByActivityId.values().stream()
                .filter(ActivityProgress::isCompleted)
                .map(ActivityProgress::getCompletedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (lastPassedAt == null) {
            return false;
        }

        // 통과한 퀴즈, 또는 마지막 통과 시각까지 이미 있던 퀴즈만 "마지막 퀴즈" 후보다. 그 뒤에 추가된 퀴즈는 열림 조건이 아니다
        Activity lastRequiredQuiz = quizzes.stream()
                .filter(quiz -> isPassed(progressByActivityId.get(quiz.getActivityId()))
                        || !quiz.getCreatedAt().isAfter(lastPassedAt))
                .reduce((first, second) -> second)
                .orElse(null);
        return lastRequiredQuiz != null && isPassed(progressByActivityId.get(lastRequiredQuiz.getActivityId()));
    }

    private boolean isPassed(ActivityProgress progress) {
        return progress != null && progress.isCompleted();
    }
}
