package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.course.model.policy.CourseReader;
import com.vibelog.domain.course.model.policy.CourseSummary;
import com.vibelog.domain.exam.model.policy.LessonExamStatusChecker;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.CourseCompleteCondition;
import com.vibelog.domain.lesson.model.dto.CourseCompleteLessonReport;
import com.vibelog.domain.lesson.model.dto.CourseCompleteView;
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
import java.util.Optional;

/*
 * [CourseCompleteService]
 * 역할 : 코스 완료 화면(U-18, API-ENR-03) 데이터를 만든다.
 * 책임 : - 완료 조건 3개(PL-ENR-003: 모의고사 응시 · 오답 분석 리포트 확인 · 포트폴리오 제출)의 완료·남음,
 *          완료 일시, 수업별 리포트 목록을 계산한다. 어떤 값도 저장하지 않고 조회할 때마다 계산한다.
 *        - 완료 여부·완료 일시는 계약 ⑦(PortfolioEnrollmentProgress.recordSubmitted/recordDeleted)이
 *          이미 기록한 enrollment.completed_at 을 읽기만 한다 (완료 기록 로직은 다시 만들지 않는다).
 *        - 수업별 모의고사 응시 여부는 계약 ④(LessonExamStatusChecker), 포트폴리오 제출 여부는
 *          계약 ⑧(LessonPortfolioChecker)로 받는다 (MyLearningService 와 같은 패턴).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCompleteService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseReader courseReader;
    private final LessonExamStatusChecker lessonExamStatusChecker;
    private final LessonPortfolioChecker lessonPortfolioChecker;

    /* 수강하지 않았거나 포기한 수강이면(수강 안 함으로 본다, #306) 빈 값을 돌려준다 (Controller 가 /courses/{courseId}로 돌려보낸다, API-ENR-03 실패) */
    public Optional<CourseCompleteView> getCourseComplete(Long memberId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByMemberIdAndCourseIdAndDroppedAtIsNull(memberId, courseId).orElse(null);
        if (enrollment == null) {
            return Optional.empty();
        }

        Long enrollmentId = enrollment.getEnrollmentId();
        List<Lesson> publicLessons = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(courseId);
        List<Long> lessonIds = publicLessons.stream().map(Lesson::getLessonId).toList();

        Map<Long, LessonProgress> progressByLessonId = new HashMap<>();
        for (LessonProgress progress : lessonProgressRepository.findByEnrollmentIdAndLessonIdIn(enrollmentId, lessonIds)) {
            progressByLessonId.put(progress.getLessonId(), progress);
        }
        Map<Long, Integer> latestSubmittedAttemptNos =
                lessonExamStatusChecker.getLatestSubmittedAttemptNos(memberId, lessonIds);

        long reportConfirmedCount = lessonProgressRepository
                .countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(enrollmentId, lessonIds);
        boolean portfolioCondition = lessonPortfolioChecker.hasSubmitted(memberId, courseId);

        List<CourseCompleteCondition> conditions = buildConditions(
                lessonIds.size(), latestSubmittedAttemptNos.size(), reportConfirmedCount, portfolioCondition);
        List<CourseCompleteLessonReport> lessonReports = buildLessonReports(publicLessons, latestSubmittedAttemptNos);

        CourseSummary summary = courseReader.getSummary(courseId);

        return Optional.of(new CourseCompleteView(
                courseId,
                summary.trackName(),
                summary.courseName(),
                enrollment.isCompleted(),
                enrollment.getCompletedAt(),
                conditions,
                lessonReports
        ));
    }

    /* PL-ENR-003 완료 조건 3개. 공개 수업이 하나도 없으면(FR-CRS-006로 막힘) 모의고사·리포트 조건은 미충족으로 둔다 */
    private List<CourseCompleteCondition> buildConditions(int publicLessonCount, int submittedLessonCount,
                                                            long reportConfirmedCount, boolean portfolioCondition) {
        boolean examCondition = publicLessonCount > 0 && submittedLessonCount == publicLessonCount;
        boolean reportCondition = publicLessonCount > 0 && reportConfirmedCount == publicLessonCount;
        return List.of(
                new CourseCompleteCondition("모든 공개 수업 모의고사 응시", examCondition),
                new CourseCompleteCondition("모든 공개 수업 오답 분석 리포트 확인", reportCondition),
                new CourseCompleteCondition("포트폴리오 제출", portfolioCondition)
        );
    }

    /* 수업별 리포트 링크. 제출한 응시가 있어야 회차(attempt)를 알 수 있어 링크를 만든다 (계약 ④) */
    private List<CourseCompleteLessonReport> buildLessonReports(List<Lesson> publicLessons,
                                                                 Map<Long, Integer> latestSubmittedAttemptNos) {
        List<CourseCompleteLessonReport> lessonReports = new ArrayList<>();
        for (Lesson lesson : publicLessons) {
            Integer attemptNo = latestSubmittedAttemptNos.get(lesson.getLessonId());
            String reportUrl = attemptNo == null
                    ? null
                    : "/lessons/" + lesson.getLessonId() + "/exam/report?attempt=" + attemptNo;
            lessonReports.add(new CourseCompleteLessonReport(lesson.getLessonId(), lesson.getTitle(), reportUrl));
        }
        return lessonReports;
    }
}
