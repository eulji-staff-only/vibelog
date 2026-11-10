package com.vibelog.domain.lesson.model.policy;

import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [LessonExamOpenPolicy]
 * 역할 : 수강(enrollment) 하나에서 어떤 수업의 모의고사가 열렸는지 판단한다 (PL-LSN-002, PL-ENR-006).
 * 책임 : - 기본 규칙: 그 수업 마지막 퀴즈를 통과(activity_progress.completed_at)하면 모의고사가 열린다.
 *        - 한 번 열린 것은 다시 잠그지 않는다 (PL-ENR-006). 열림 상태를 따로 저장하지 않으므로 "한 번이라도 열린 적이 있나"로 본다.
 *          퀴즈를 통과한 시각마다, 그 시각에 있던 퀴즈(activity.created_at 이 그 시각 이전) 중 마지막 퀴즈가 그 시각까지 통과돼 있었으면
 *          그때 열린 것이다. completed_at 은 한 번만 기록되어 한 번 참이면 계속 참이므로, 열린 뒤 운영자가 퀴즈를 추가하거나
 *          학습자가 그 새 퀴즈를 통과해도 다시 잠기지 않는다.
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

        // "한 번이라도 열린 적이 있나"로 본다. 퀴즈를 통과한 각 시각에, 그때 있던 퀴즈 중 마지막 퀴즈가 그 시각까지 통과돼 있었으면
        // 그때 열린 것이다. completed_at 은 한 번만 기록되므로 한 번 참이면 이후 퀴즈가 추가되거나 더 통과해도 계속 참이다 (PL-ENR-006)
        return progressByActivityId.values().stream()
                .filter(ActivityProgress::isCompleted)
                .map(ActivityProgress::getCompletedAt)
                .anyMatch(at -> wasOpenAt(quizzes, progressByActivityId, at));
    }

    /* 시각 at 에 있던 퀴즈(그때까지 만들어졌거나 그때까지 통과한 퀴즈) 중 마지막 퀴즈를 그 시각까지 통과했나 */
    private boolean wasOpenAt(List<Activity> quizzes, Map<Long, ActivityProgress> progressByActivityId, LocalDateTime at) {
        return quizzes.stream()
                .filter(quiz -> !quiz.getCreatedAt().isAfter(at) || isPassedBy(progressByActivityId.get(quiz.getActivityId()), at))
                .reduce((first, second) -> second)
                .map(last -> isPassedBy(progressByActivityId.get(last.getActivityId()), at))
                .orElse(false);
    }

    /* 통과 시각이 at 이전(같은 시각 포함)이면 true. created_at 이 DB 에서 초 단위로 반올림돼 통과한 퀴즈가 빠지는 일도 이것으로 막는다 */
    private boolean isPassedBy(ActivityProgress progress, LocalDateTime at) {
        return progress != null && progress.isCompleted() && !progress.getCompletedAt().isAfter(at);
    }
}
