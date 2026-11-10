package com.vibelog.domain.admin.model.service;

import com.vibelog.domain.admin.model.dto.AdminLearningAnalytics;
import com.vibelog.domain.admin.model.policy.AnalyticsRatePolicy;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.exam.model.dto.ExamWeakConcept;
import com.vibelog.domain.exam.model.service.ExamReader;
import com.vibelog.domain.lesson.model.dto.DropOffRate;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.quiz.model.dto.ConceptWrongStat;
import com.vibelog.domain.quiz.model.dto.QuizPassRate;
import com.vibelog.domain.quiz.model.service.QuizReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * [AdminLearningAnalyticsService]
 * 역할 : 확장 대시보드(A-01, API-ADM-40, FR-ADM-003) 2차 지표를 모은다.
 *        추천 트랙 vs 실제 수강, 수업별·활동별 이탈률, 퀴즈 통과율·개념별 오답률, 수업별 모의고사 평균·약한 개념.
 * 책임 : - 직접 계산하지 않고 Course·Lesson·Quiz·ExamReader 가 준 값에 기간(from~to)만 넘겨 합친다
 *          (다른 영역의 Entity·Repository 는 import 하지 않는다). admin 이 하는 일은 이름 붙이기·비율 계산·정렬이다.
 *        - 추천 트랙의 실제 수강은 회원이 수강한 코스의 트랙(CourseReader)이 추천 트랙과 같은지로 본다.
 *        - 값이 없는 지표는 빈 목록·0 으로 둔다 (화면이 빈 상태 문구를 보여 준다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLearningAnalyticsService {

    private final CourseReader courseReader;
    private final LessonReader lessonReader;
    private final QuizReader quizReader;
    private final ExamReader examReader;

    public AdminLearningAnalytics getAnalytics(LocalDateTime from, LocalDateTime to) {
        return new AdminLearningAnalytics(toTrackRecommendations(from, to), toLessonDropOffs(from, to),
                toActivityDropOffs(from, to), toQuizPass(from, to), toConceptWrongRates(from, to), toExamLessons(from, to));
    }

    /* 추천받은 트랙마다 추천받은 회원 수, 그중 그 트랙의 코스를 수강 시작한 회원 수. 트랙 이름 순 */
    private List<AdminLearningAnalytics.TrackRecommendation> toTrackRecommendations(LocalDateTime from, LocalDateTime to) {
        Map<Long, Long> recommendedByMember = courseReader.findDiagnosisRecommendedTracks(from, to);
        if (recommendedByMember.isEmpty()) {
            return List.of();
        }
        Map<Long, Set<Long>> courseIdsByMember = lessonReader.findEnrolledCourseIds(recommendedByMember.keySet());
        Set<Long> allCourseIds = new HashSet<>();
        courseIdsByMember.values().forEach(allCourseIds::addAll);
        Map<Long, Long> trackIdsByCourse = allCourseIds.isEmpty() ? Map.of() : courseReader.getTrackIdsByCourse(allCourseIds);

        Map<Long, Integer> recommendedCounts = new HashMap<>();
        Map<Long, Integer> enrolledCounts = new HashMap<>();
        recommendedByMember.forEach((memberId, trackId) -> {
            recommendedCounts.merge(trackId, 1, Integer::sum);
            boolean enrolledInTrack = courseIdsByMember.getOrDefault(memberId, Set.of()).stream()
                    .anyMatch(courseId -> trackId.equals(trackIdsByCourse.get(courseId)));
            if (enrolledInTrack) {
                enrolledCounts.merge(trackId, 1, Integer::sum);
            }
        });

        Map<Long, String> trackNames = courseReader.getTrackTitles(recommendedCounts.keySet());
        return recommendedCounts.entrySet().stream()
                .map(entry -> new AdminLearningAnalytics.TrackRecommendation(entry.getKey(),
                        trackNames.getOrDefault(entry.getKey(), ""), entry.getValue(),
                        enrolledCounts.getOrDefault(entry.getKey(), 0),
                        AnalyticsRatePolicy.percent(enrolledCounts.getOrDefault(entry.getKey(), 0), entry.getValue())))
                .sorted(Comparator.comparing(AdminLearningAnalytics.TrackRecommendation::trackName))
                .toList();
    }

    /* 수업별 이탈률(못 끝낸 비율)이 높은 것부터, 같으면 열린 수가 많은 것부터 */
    private List<AdminLearningAnalytics.LessonDropOff> toLessonDropOffs(LocalDateTime from, LocalDateTime to) {
        Map<Long, DropOffRate> rates = lessonReader.getDropOffRateByLesson(from, to);
        if (rates.isEmpty()) {
            return List.of();
        }
        Map<Long, String> lessonTitles = lessonReader.getLessonTitles(rates.keySet());
        Map<Long, String> courseTitles = courseTitlesOfLessons(rates.keySet());
        return rates.entrySet().stream()
                .map(entry -> new AdminLearningAnalytics.LessonDropOff(entry.getKey(),
                        courseTitles.getOrDefault(entry.getKey(), ""), lessonTitles.getOrDefault(entry.getKey(), ""),
                        entry.getValue().eligibleCount(), entry.getValue().dropOffCount(),
                        AnalyticsRatePolicy.percent(entry.getValue().dropOffCount(), entry.getValue().eligibleCount())))
                .sorted(Comparator.comparingInt(AdminLearningAnalytics.LessonDropOff::dropOffRate).reversed()
                        .thenComparing(Comparator.comparingInt(AdminLearningAnalytics.LessonDropOff::eligibleCount).reversed())
                        .thenComparing(AdminLearningAnalytics.LessonDropOff::lessonId))
                .toList();
    }

    /* 활동별 이탈률(못 끝낸 비율)이 높은 것부터, 같으면 열린 수가 많은 것부터. 수업별과 같은 기준이고, 활동 이름에 수업·코스 이름을 붙인다 */
    private List<AdminLearningAnalytics.ActivityDropOff> toActivityDropOffs(LocalDateTime from, LocalDateTime to) {
        Map<Long, DropOffRate> rates = lessonReader.getDropOffRateByActivity(from, to);
        if (rates.isEmpty()) {
            return List.of();
        }
        Map<Long, String> activityTitles = lessonReader.getActivityTitles(rates.keySet());
        Map<Long, Long> lessonIdsByActivity = lessonReader.getLessonIdsByActivity(rates.keySet());
        Set<Long> lessonIds = new HashSet<>(lessonIdsByActivity.values());
        Map<Long, String> lessonTitles = lessonReader.getLessonTitles(lessonIds);
        Map<Long, String> courseTitles = courseTitlesOfLessons(lessonIds);
        return rates.entrySet().stream()
                .map(entry -> {
                    Long lessonId = lessonIdsByActivity.get(entry.getKey());
                    DropOffRate rate = entry.getValue();
                    return new AdminLearningAnalytics.ActivityDropOff(entry.getKey(),
                            courseTitles.getOrDefault(lessonId, ""), lessonTitles.getOrDefault(lessonId, ""),
                            activityTitles.getOrDefault(entry.getKey(), ""), rate.eligibleCount(), rate.dropOffCount(),
                            AnalyticsRatePolicy.percent(rate.dropOffCount(), rate.eligibleCount()));
                })
                .sorted(Comparator.comparingInt(AdminLearningAnalytics.ActivityDropOff::dropOffRate).reversed()
                        .thenComparing(Comparator.comparingInt(AdminLearningAnalytics.ActivityDropOff::eligibleCount).reversed())
                        .thenComparing(AdminLearningAnalytics.ActivityDropOff::activityId))
                .toList();
    }

    private AdminLearningAnalytics.QuizPassStat toQuizPass(LocalDateTime from, LocalDateTime to) {
        QuizPassRate passRate = quizReader.getPassRate(from, to);
        return new AdminLearningAnalytics.QuizPassStat(passRate.attemptedCount(), passRate.passedCount(),
                AnalyticsRatePolicy.percent(passRate.passedCount(), passRate.attemptedCount()));
    }

    /* 오답률이 높은 개념부터, 같으면 개념 이름 순. 약한 개념(정답률 60% 미만) 표시 */
    private List<AdminLearningAnalytics.ConceptWrongRate> toConceptWrongRates(LocalDateTime from, LocalDateTime to) {
        Map<Long, ConceptWrongStat> stats = quizReader.getWrongRateByConceptTag(from, to);
        if (stats.isEmpty()) {
            return List.of();
        }
        Map<Long, String> tagNames = lessonReader.getConceptTagNames(stats.keySet());
        return stats.entrySet().stream()
                .map(entry -> new AdminLearningAnalytics.ConceptWrongRate(entry.getKey(), tagNames.getOrDefault(entry.getKey(), ""),
                        entry.getValue().questionCount(), entry.getValue().wrongCount(),
                        AnalyticsRatePolicy.percent(entry.getValue().wrongCount(), entry.getValue().questionCount()),
                        AnalyticsRatePolicy.isWeakConcept(entry.getValue().wrongCount(), entry.getValue().questionCount())))
                .sorted(Comparator.comparingInt(AdminLearningAnalytics.ConceptWrongRate::wrongRate).reversed()
                        .thenComparing(AdminLearningAnalytics.ConceptWrongRate::conceptName))
                .toList();
    }

    /* 평균 점수가 낮은 수업부터. 평균이 없는(응시자 5명 미만) 수업은 ExamReader 가 이미 뺐다 */
    private List<AdminLearningAnalytics.ExamLessonStat> toExamLessons(LocalDateTime from, LocalDateTime to) {
        Map<Long, Double> averages = examReader.getAverageScoreByLesson(from, to);
        if (averages.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ExamWeakConcept>> weakByLesson = examReader.getWeakConceptsByLesson(from, to);
        Map<Long, String> lessonTitles = lessonReader.getLessonTitles(averages.keySet());
        Map<Long, String> courseTitles = courseTitlesOfLessons(averages.keySet());
        Set<Long> conceptIds = new HashSet<>();
        averages.keySet().forEach(lessonId -> weakByLesson.getOrDefault(lessonId, List.of())
                .forEach(weak -> conceptIds.add(weak.conceptActivityId())));
        Map<Long, String> conceptTitles = conceptIds.isEmpty() ? Map.of() : lessonReader.getActivityTitles(conceptIds);

        return averages.entrySet().stream()
                .map(entry -> new AdminLearningAnalytics.ExamLessonStat(entry.getKey(),
                        courseTitles.getOrDefault(entry.getKey(), ""), lessonTitles.getOrDefault(entry.getKey(), ""),
                        entry.getValue(),
                        weakByLesson.getOrDefault(entry.getKey(), List.of()).stream()
                                .map(weak -> conceptTitles.getOrDefault(weak.conceptActivityId(), ""))
                                .toList()))
                .sorted(Comparator.comparingDouble(AdminLearningAnalytics.ExamLessonStat::averageScore)
                        .thenComparing(AdminLearningAnalytics.ExamLessonStat::lessonId))
                .toList();
    }

    /* 수업 ID → 그 수업이 속한 코스 이름 */
    private Map<Long, String> courseTitlesOfLessons(java.util.Collection<Long> lessonIds) {
        Map<Long, Long> courseIdsByLesson = lessonReader.getCourseIdsByLesson(lessonIds);
        Map<Long, String> courseTitles = courseReader.getCourseTitles(new HashSet<>(courseIdsByLesson.values()));
        Map<Long, String> result = new HashMap<>();
        courseIdsByLesson.forEach((lessonId, courseId) -> result.put(lessonId, courseTitles.getOrDefault(courseId, "")));
        return result;
    }
}
