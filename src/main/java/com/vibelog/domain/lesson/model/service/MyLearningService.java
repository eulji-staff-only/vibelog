package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.course.model.policy.CourseReader;
import com.vibelog.domain.course.model.policy.CourseSummary;
import com.vibelog.domain.exam.model.policy.LessonExamStatusChecker;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.LessonExamStatus;
import com.vibelog.domain.lesson.model.dto.LessonStatus;
import com.vibelog.domain.lesson.model.dto.MyLearningCourseCard;
import com.vibelog.domain.lesson.model.dto.MyLearningLessonCard;
import com.vibelog.domain.lesson.model.dto.PortfolioStatus;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.domain.portfolio.model.policy.LessonPortfolioChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * [MyLearningService]
 * 역할 : 내 학습 화면(U-12, API-ENR-02) 데이터를 만든다.
 * 책임 : - 탭(수강 중 · 완료)별 코스 카드, 수업·모의고사·포트폴리오 상태, 진행률(PL-ENR-006), 다음에 할 일 ·
 *          남은 완료 조건을 계산한다. 어떤 값도 저장하지 않고 조회할 때마다 계산한다.
 *        - 수업별 모의고사 상태는 계약 ④(LessonExamStatusChecker), 포트폴리오 제출 여부는 계약 ⑧(LessonPortfolioChecker)로 받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyLearningService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseReader courseReader;
    private final LessonExamStatusChecker lessonExamStatusChecker;
    private final LessonPortfolioChecker lessonPortfolioChecker;

    /* "수강 중" 탭 (완료·포기하지 않은 수강) */
    public List<MyLearningCourseCard> getActiveCourses(Long memberId) {
        return enrollmentRepository.findByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(memberId).stream()
                .map(this::toCard)
                .toList();
    }

    /* "완료" 탭 */
    public List<MyLearningCourseCard> getCompletedCourses(Long memberId) {
        return enrollmentRepository.findByMemberIdAndCompletedAtIsNotNull(memberId).stream()
                .map(this::toCard)
                .toList();
    }

    private MyLearningCourseCard toCard(Enrollment enrollment) {
        Long memberId = enrollment.getMemberId();
        Long courseId = enrollment.getCourseId();
        Long enrollmentId = enrollment.getEnrollmentId();

        List<Lesson> publicLessons = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId);
        List<Long> lessonIds = publicLessons.stream().map(Lesson::getLessonId).toList();

        Map<Long, LessonProgress> progressByLessonId = new HashMap<>();
        for (LessonProgress progress : lessonProgressRepository.findByEnrollmentIdAndLessonIdIn(enrollmentId, lessonIds)) {
            progressByLessonId.put(progress.getLessonId(), progress);
        }

        Map<Long, Integer> latestSubmittedAttemptNos =
                lessonExamStatusChecker.getLatestSubmittedAttemptNos(memberId, lessonIds);

        List<MyLearningLessonCard> lessonCards = new ArrayList<>();
        List<String> remainingConditions = new ArrayList<>();
        int order = 1;
        for (Lesson lesson : publicLessons) {
            LessonProgress progress = progressByLessonId.get(lesson.getLessonId());
            LessonStatus status = toLessonStatus(progress);
            LessonExamStatus examStatus = toExamStatus(status, lesson.getLessonId(), latestSubmittedAttemptNos);

            lessonCards.add(new MyLearningLessonCard(lesson.getLessonId(), order++, lesson.getTitle(), status, examStatus));
            addLessonRemainingCondition(remainingConditions, lesson, status, examStatus);
        }

        long completedLessonCount = lessonProgressRepository
                .countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(enrollmentId, lessonIds);
        int progressPercent = calculateProgressPercent(completedLessonCount, lessonIds.size());

        PortfolioStatus portfolioStatus = toPortfolioStatus(enrollment, lessonPortfolioChecker.hasSubmitted(memberId, courseId));
        addPortfolioRemainingCondition(remainingConditions, portfolioStatus);

        String nextStepText = remainingConditions.isEmpty() ? "코스를 완료했습니다" : remainingConditions.get(0);
        CourseSummary summary = courseReader.getSummary(courseId);

        return new MyLearningCourseCard(
                courseId,
                summary.trackName(),
                summary.courseName(),
                progressPercent,
                nextStepText,
                lessonCards,
                portfolioStatus,
                remainingConditions,
                enrollment.isCompleted(),
                enrollment.getCompletedAt()
        );
    }

    /* lesson_progress 가 없거나 unlocked_at 이 없으면 잠김, completed_at 이 있으면 완료, 그 외는 진행 중 */
    private LessonStatus toLessonStatus(LessonProgress progress) {
        if (progress == null || !progress.isUnlocked()) {
            return LessonStatus.LOCKED;
        }
        if (progress.isCompleted()) {
            return LessonStatus.COMPLETED;
        }
        return LessonStatus.IN_PROGRESS;
    }

    /* 수업이 잠기면 모의고사도 잠김, 수업이 완료(리포트 확인)면 리포트 확인, 그 외 열린 수업은 응시 여부로 판단 */
    private LessonExamStatus toExamStatus(LessonStatus status, Long lessonId, Map<Long, Integer> latestSubmittedAttemptNos) {
        if (status == LessonStatus.LOCKED) {
            return LessonExamStatus.LOCKED;
        }
        if (status == LessonStatus.COMPLETED) {
            return LessonExamStatus.REPORT_CONFIRMED;
        }
        return latestSubmittedAttemptNos.containsKey(lessonId) ? LessonExamStatus.ATTEMPTED : LessonExamStatus.OPEN;
    }

    private PortfolioStatus toPortfolioStatus(Enrollment enrollment, boolean submitted) {
        if (submitted) {
            return PortfolioStatus.SUBMITTED;
        }
        return enrollment.isPortfolioUnlocked() ? PortfolioStatus.OPEN : PortfolioStatus.LOCKED;
    }

    private void addLessonRemainingCondition(List<String> remainingConditions, Lesson lesson,
                                              LessonStatus status, LessonExamStatus examStatus) {
        if (status == LessonStatus.LOCKED) {
            remainingConditions.add("[" + lesson.getTitle() + "] 잠김 — 이전 수업을 완료하면 열립니다");
        } else if (status == LessonStatus.IN_PROGRESS) {
            if (examStatus == LessonExamStatus.OPEN) {
                remainingConditions.add("[" + lesson.getTitle() + "] 모의고사 응시하기");
            } else if (examStatus == LessonExamStatus.ATTEMPTED) {
                remainingConditions.add("[" + lesson.getTitle() + "] 오답 분석 리포트 확인하기");
            }
        }
    }

    private void addPortfolioRemainingCondition(List<String> remainingConditions, PortfolioStatus portfolioStatus) {
        if (portfolioStatus == PortfolioStatus.LOCKED) {
            remainingConditions.add("포트폴리오 — 마지막 수업 리포트를 확인하면 열립니다");
        } else if (portfolioStatus == PortfolioStatus.OPEN) {
            remainingConditions.add("포트폴리오 제출하기");
        }
    }

    /* PL-ENR-006: 진행률 = 리포트까지 확인한 수업 수 ÷ 공개 수업 수. 저장하지 않고 호출마다 계산한다 */
    private int calculateProgressPercent(long completedLessonCount, long publicLessonCount) {
        if (publicLessonCount <= 0) {
            return 0;
        }
        return (int) Math.round(completedLessonCount * 100.0 / publicLessonCount);
    }
}
