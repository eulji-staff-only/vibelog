package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.CourseLessonCount;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.domain.lesson.model.policy.CourseLessonInfo;
import com.vibelog.domain.lesson.model.policy.ExamLessonProgress;
import com.vibelog.domain.lesson.model.policy.LessonExamOpenPolicy;
import com.vibelog.domain.lesson.model.policy.LessonSummary;
import com.vibelog.domain.lesson.model.policy.LoginEnrollmentChecker;
import com.vibelog.domain.lesson.model.policy.NextStep;
import com.vibelog.domain.lesson.model.policy.PortfolioAccess;
import com.vibelog.domain.lesson.model.policy.PortfolioEnrollmentProgress;
import com.vibelog.domain.lesson.model.policy.QuizActivityInfo;
import com.vibelog.domain.lesson.model.policy.QuizActivityProgress;
import com.vibelog.domain.lesson.model.policy.ReportNextStep;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * [LessonContractService]
 * 역할 : lesson 영역이 다른 영역에 주는 계약 ①②③⑥⑦⑨⑪⑫의 진짜 구현.
 * 책임 : - 다른 영역은 이 클래스가 아니라 model/policy 의 계약 인터페이스를 주입해 쓴다.
 *        - 조회 계약은 읽기 전용, 기록 계약(①③⑥⑦)은 부르는 쪽 트랜잭션에 함께 묶인다 (08 문서 규칙 1-6).
 *        - 열림·완료 기록(①③⑦)은 처음 한 번만 하고, 다시 불러도 다음 행동만 돌려준다.
 *        - 포기한 수강(dropped_at)은 ①②③⑥⑦의 열림·완료·제출 판단에서 "수강 안 함"으로 본다 (#306).
 *          예외: 포트폴리오 삭제의 완료 기록 비우기(⑦ recordDeleted)는 이미 있는 기록을 정리하므로 포기한 수강도 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonContractService implements QuizActivityProgress, ExamLessonProgress, PortfolioEnrollmentProgress,
        CourseLessonInfo, LoginEnrollmentChecker {

    private final LessonRepository lessonRepository;
    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ActivityProgressRepository activityProgressRepository;
    private final LessonExamOpenPolicy lessonExamOpenPolicy;

    /* ① unlocked = 수강 중이고 그 활동이 열려 있음 */
    @Override
    public QuizActivityInfo getQuizActivity(Long memberId, Long activityId) {
        Activity activity = getActivity(activityId);
        Lesson lesson = getLesson(activity.getLessonId());
        boolean unlocked = enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, lesson.getCourseId())
                .flatMap(enrollment -> activityProgressRepository
                        .findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), activityId))
                .map(ActivityProgress::isUnlocked)
                .orElse(false);
        return new QuizActivityInfo(lesson.getCourseId(), lesson.getLessonId(), unlocked, activity.getPassRate());
    }

    /*
     * ① 퀴즈 활동 완료는 처음 한 번만 기록한다. 다음 활동이 있으면 그 활동을 열고 CONCEPT,
     * 공개 수업의 마지막 활동이면 EXAM, 비공개 수업의 마지막 활동이면 NONE (API-ADM-33: 공개 수업은 모두 모의고사가 있다)
     */
    @Override
    @Transactional
    public NextStep recordQuizPassed(Long memberId, Long activityId) {
        Activity quiz = getActivity(activityId);
        Lesson lesson = getLesson(quiz.getLessonId());
        Enrollment enrollment = getEnrollment(memberId, lesson.getCourseId());

        activityProgressRepository.findByEnrollmentIdAndActivityId(enrollment.getEnrollmentId(), activityId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND))
                .complete();

        Optional<Activity> next = activityRepository
                .findFirstByLessonIdAndIsActiveTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
                        lesson.getLessonId(), quiz.getSortOrder());
        if (next.isPresent()) {
            unlockActivity(enrollment.getEnrollmentId(), next.get());
            return new NextStep(NextStep.CONCEPT, lesson.getCourseId(), lesson.getLessonId(), next.get().getActivityId());
        }
        if (lesson.isPublic()) {
            return new NextStep(NextStep.EXAM, lesson.getCourseId(), lesson.getLessonId(), null);
        }
        return new NextStep(NextStep.NONE, lesson.getCourseId(), lesson.getLessonId(), null);
    }

    /* ② 그 수업 마지막 퀴즈 활동의 completed_at 이 있음. 한 번 열린 모의고사는 퀴즈가 추가돼도 다시 잠그지 않는다 (LessonExamOpenPolicy, PL-ENR-006) */
    @Override
    public boolean isExamOpen(Long memberId, Long lessonId) {
        Lesson lesson = getLesson(lessonId);
        return enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, lesson.getCourseId())
                .map(enrollment -> lessonExamOpenPolicy.isOpen(enrollment.getEnrollmentId(), lessonId))
                .orElse(false);
    }

    /*
     * ③ 수업 완료는 처음 한 번만 기록한다. 다음 공개 수업이 있으면 그 수업과 첫 개념을 열고 NEXT_LESSON,
     * 마지막 공개 수업이면 포트폴리오를 열고 PORTFOLIO (API-ENR-01과 같은 방식으로 다음 수업을 연다)
     */
    @Override
    @Transactional
    public ReportNextStep recordReportViewed(Long memberId, Long lessonId) {
        Lesson lesson = getLesson(lessonId);
        Enrollment enrollment = getEnrollment(memberId, lesson.getCourseId());
        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), lessonId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_PROGRESS_NOT_FOUND));

        boolean alreadyCompleted = progress.isCompleted();
        if (!alreadyCompleted) {
            progress.complete();
        }

        Optional<Lesson> nextLesson = lessonRepository
                .findFirstByCourseIdAndIsPublicTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
                        lesson.getCourseId(), lesson.getSortOrder());
        if (nextLesson.isPresent()) {
            if (!alreadyCompleted) {
                unlockLessonAndFirstConcept(enrollment.getEnrollmentId(), nextLesson.get());
            }
            return new ReportNextStep(ReportNextStep.NEXT_LESSON, lesson.getCourseId(), nextLesson.get().getLessonId());
        }
        if (!alreadyCompleted) {
            enrollment.unlockPortfolio();
        }
        return new ReportNextStep(ReportNextStep.PORTFOLIO, lesson.getCourseId(), null);
    }

    /* ⑥ unlocked = enrollment.portfolio_unlocked_at 이 있음 */
    @Override
    public PortfolioAccess getAccess(Long memberId, Long courseId) {
        return enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, courseId)
                .map(enrollment -> new PortfolioAccess(true, enrollment.isPortfolioUnlocked()))
                .orElse(new PortfolioAccess(false, false));
    }

    /* ⑦ 모든 공개 수업의 lesson_progress.completed_at 이 있으면 코스 완료를 처음 한 번만 기록한다 */
    @Override
    @Transactional
    public void recordSubmitted(Long memberId, Long courseId) {
        Enrollment enrollment = getEnrollment(memberId, courseId);
        List<Lesson> publicLessons = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId);
        if (publicLessons.isEmpty()) {
            return;
        }
        List<Long> lessonIds = publicLessons.stream().map(Lesson::getLessonId).toList();
        long completedCount = lessonProgressRepository
                .countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(enrollment.getEnrollmentId(), lessonIds);
        if (completedCount == publicLessons.size()) {
            enrollment.complete();
        }
    }

    /* ⑦ 포트폴리오를 삭제하면 코스 완료 기록을 비운다 (PL-ENR-003) */
    @Override
    @Transactional
    public void recordDeleted(Long memberId, Long courseId) {
        // 이미 있는 완료 기록을 정리하는 쪽이라 포기한 수강도 막지 않는다 (안 비우면 다시 시작했을 때 작품 없이 완료로 남는다, #306)
        enrollmentRepository.findByMemberIdAndCourseId(memberId, courseId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ENROLLMENT_NOT_FOUND))
                .cancelCompletion();
    }

    /* ⑨ 공개 수업이 하나 이상 있음 */
    @Override
    public boolean hasPublicLesson(Long courseId) {
        return lessonRepository.existsByCourseIdAndIsPublicTrue(courseId);
    }

    /*
     * ⑪ NONE·ENROLLED·COMPLETED. 수강 포기(dropped_at)는 다시 시작하면 이어서 하므로(PL-ENR-004)
     * "시작 전"과 같은 NONE 으로 본다. 포기한 동안 완료 시각이 기록돼도(포트폴리오 제출) 포기가 우선이다
     * (다시 시작하면 그 완료 기록이 반영된다)
     */
    @Override
    public String getEnrollmentStatus(Long memberId, Long courseId) {
        return enrollmentRepository.findByMemberIdAndCourseId(memberId, courseId)
                .map(this::toEnrollmentStatus)
                .orElse("NONE");
    }

    private String toEnrollmentStatus(Enrollment enrollment) {
        if (enrollment.isDropped()) {
            return "NONE";
        }
        if (enrollment.isCompleted()) {
            return "COMPLETED";
        }
        return "ENROLLED";
    }

    /* ⑪ sort_order 순 공개 수업 목록 */
    @Override
    public List<LessonSummary> getPublicLessons(Long courseId) {
        return lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId).stream()
                .map(this::toSummary)
                .toList();
    }

    private LessonSummary toSummary(Lesson lesson) {
        int conceptCount = (int) activityRepository
                .countByLessonIdAndActivityTypeAndIsActiveTrue(lesson.getLessonId(), ActivityType.CONCEPT);
        int quizCount = (int) activityRepository
                .countByLessonIdAndActivityTypeAndIsActiveTrue(lesson.getLessonId(), ActivityType.QUIZ);
        return new LessonSummary(lesson.getLessonId(), lesson.getTitle(), lesson.getGoal(),
                conceptCount, quizCount, lesson.getEstimatedMinutes());
    }

    /* ⑪ 코스 카드의 수업 수 (목록 화면용. ID 묶음을 받아 쿼리 한 번으로 읽는다) */
    @Override
    public Map<Long, Integer> countPublicLessons(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return lessonRepository.countPublicLessonsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(CourseLessonCount::courseId, count -> (int) count.count()));
    }

    /* ⑫ completed_at·dropped_at 이 모두 비어 있는 수강이 있음 */
    @Override
    public boolean hasActiveEnrollment(Long memberId) {
        return enrollmentRepository.existsByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(memberId);
    }

    private Lesson getLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_NOT_FOUND));
    }

    private Activity getActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND));
    }

    /* 진행 기록을 바꾸는 계약용. 포기한 수강은 수강 안 함과 같게 ENROLLMENT_NOT_FOUND (#306) */
    private Enrollment getEnrollment(Long memberId, Long courseId) {
        return enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, courseId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ENROLLMENT_NOT_FOUND));
    }

    /* 활동 진도가 없으면 만들면서 열고, 있으면 그대로 연다 (unlock 은 처음 한 번만 기록). */
    private void unlockActivity(Long enrollmentId, Activity activity) {
        activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity.getActivityId())
                .orElseGet(() -> activityProgressRepository.save(
                        ActivityProgress.create(enrollmentId, activity.getActivityId(), null)))
                .unlock();
    }

    /* 다음 수업과 그 첫 개념을 연다 (API-ENR-01과 같은 방식) */
    private void unlockLessonAndFirstConcept(Long enrollmentId, Lesson lesson) {
        lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lesson.getLessonId())
                .orElseGet(() -> lessonProgressRepository.save(
                        LessonProgress.create(enrollmentId, lesson.getLessonId(), null)))
                .unlock();
        activityRepository.findFirstByLessonIdAndIsActiveTrueOrderBySortOrderAsc(lesson.getLessonId())
                .ifPresent(firstActivity -> unlockActivity(enrollmentId, firstActivity));
    }
}
