package com.vibelog.domain.admin.model.service;

import com.vibelog.domain.admin.model.dto.AdminLearningAnalytics;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.exam.model.dto.ExamWeakConcept;
import com.vibelog.domain.exam.model.service.ExamReader;
import com.vibelog.domain.lesson.model.dto.DropOffRate;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.quiz.model.dto.ConceptWrongStat;
import com.vibelog.domain.quiz.model.dto.QuizPassRate;
import com.vibelog.domain.quiz.model.service.QuizReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/*
 * 확장 대시보드(API-ADM-40, FR-ADM-003, #194 2차): admin 은 직접 세지 않고 각 영역 Reader 값을 기간과 함께 받아
 * 이름을 붙이고 비율을 계산해 합친다. Reader 안의 집계 규칙은 DashboardAnalyticsReaderTest 가 DB 로 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminLearningAnalyticsServiceTest {

    @Mock CourseReader courseReader;
    @Mock LessonReader lessonReader;
    @Mock QuizReader quizReader;
    @Mock ExamReader examReader;
    @InjectMocks AdminLearningAnalyticsService service;

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

    private void givenEmptyReaders() {
        given(quizReader.getPassRate(FROM, TO)).willReturn(new QuizPassRate(0, 0));
    }

    @Test
    void 추천받은_트랙마다_그_트랙_코스를_수강_시작한_회원_수와_비율을_계산한다() {
        givenEmptyReaders();
        // 회원 1·2·3 은 트랙 10 추천, 회원 4 는 트랙 11 추천
        given(courseReader.findDiagnosisRecommendedTracks(FROM, TO)).willReturn(Map.of(1L, 10L, 2L, 10L, 3L, 10L, 4L, 11L));
        // 회원 1: 트랙10 코스 수강, 회원 2: 다른 트랙(11) 코스만 수강, 회원 3: 수강 없음, 회원 4: 트랙11 코스 수강
        given(lessonReader.findEnrolledCourseIds(any())).willReturn(Map.of(1L, Set.of(100L), 2L, Set.of(200L), 4L, Set.of(201L)));
        given(courseReader.getTrackIdsByCourse(any())).willReturn(Map.of(100L, 10L, 200L, 11L, 201L, 11L));
        given(courseReader.getTrackTitles(any())).willReturn(Map.of(10L, "트랙열", 11L, "트랙열하나"));

        List<AdminLearningAnalytics.TrackRecommendation> result = service.getAnalytics(FROM, TO).trackRecommendations();

        assertThat(result).containsExactly(
                new AdminLearningAnalytics.TrackRecommendation(10L, "트랙열", 3, 1, 33),
                new AdminLearningAnalytics.TrackRecommendation(11L, "트랙열하나", 1, 1, 100));
    }

    @Test
    void 추천받은_회원이_없으면_빈_목록이고_수강을_조회하지_않는다() {
        givenEmptyReaders();

        AdminLearningAnalytics result = service.getAnalytics(FROM, TO);

        assertThat(result.trackRecommendations()).isEmpty();
        assertThat(result.lessonDropOffs()).isEmpty();
        assertThat(result.conceptWrongRates()).isEmpty();
        assertThat(result.examLessons()).isEmpty();
        assertThat(result.quizPass()).isEqualTo(new AdminLearningAnalytics.QuizPassStat(0, 0, 0));
    }

    @Test
    void 수업별_이탈률은_수업과_코스_이름을_붙이고_못_끝낸_비율이_높은_순으로_정렬한다() {
        givenEmptyReaders();
        given(lessonReader.getDropOffRateByLesson(FROM, TO)).willReturn(
                Map.of(1L, new DropOffRate(10, 2), 2L, new DropOffRate(3, 2), 3L, new DropOffRate(4, 0)));
        given(lessonReader.getLessonTitles(any())).willReturn(Map.of(1L, "수업1", 2L, "수업2", 3L, "수업3"));
        given(lessonReader.getCourseIdsByLesson(any())).willReturn(Map.of(1L, 100L, 2L, 100L, 3L, 200L));
        given(courseReader.getCourseTitles(any())).willReturn(Map.of(100L, "코스100", 200L, "코스200"));

        List<AdminLearningAnalytics.LessonDropOff> result = service.getAnalytics(FROM, TO).lessonDropOffs();

        assertThat(result).containsExactly(
                new AdminLearningAnalytics.LessonDropOff(2L, "코스100", "수업2", 3, 2, 67),
                new AdminLearningAnalytics.LessonDropOff(1L, "코스100", "수업1", 10, 2, 20),
                new AdminLearningAnalytics.LessonDropOff(3L, "코스200", "수업3", 4, 0, 0));
    }

    @Test
    void 퀴즈_통과율은_응시_대비_통과_비율이다() {
        given(quizReader.getPassRate(FROM, TO)).willReturn(new QuizPassRate(8, 3));

        assertThat(service.getAnalytics(FROM, TO).quizPass()).isEqualTo(new AdminLearningAnalytics.QuizPassStat(8, 3, 38));
    }

    @Test
    void 개념별_오답률은_정답률_60퍼센트_미만만_약한_개념으로_표시하고_오답률_높은_순으로_정렬한다() {
        givenEmptyReaders();
        given(quizReader.getWrongRateByConceptTag(FROM, TO)).willReturn(Map.of(
                1L, new ConceptWrongStat(10, 4),    // 정답률 60% → 약하지 않음
                2L, new ConceptWrongStat(12, 7),    // 정답률 41.7% → 약함
                3L, new ConceptWrongStat(42, 17))); // 정답률 59.5% → 약함 (반올림하면 60% 로 보여도)
        given(lessonReader.getConceptTagNames(any())).willReturn(Map.of(1L, "변수", 2L, "반복문", 3L, "함수"));

        List<AdminLearningAnalytics.ConceptWrongRate> result = service.getAnalytics(FROM, TO).conceptWrongRates();

        assertThat(result).containsExactly(
                new AdminLearningAnalytics.ConceptWrongRate(2L, "반복문", 12, 7, 58, true),
                new AdminLearningAnalytics.ConceptWrongRate(1L, "변수", 10, 4, 40, false),
                new AdminLearningAnalytics.ConceptWrongRate(3L, "함수", 42, 17, 40, true));
    }

    @Test
    void 모의고사_평균과_약한_개념은_수업_이름과_개념_이름을_붙여_평균이_낮은_수업부터_보여준다() {
        givenEmptyReaders();
        given(examReader.getAverageScoreByLesson(FROM, TO)).willReturn(Map.of(1L, 80.5, 2L, 60.2));
        given(examReader.getWeakConceptsByLesson(FROM, TO)).willReturn(
                Map.of(1L, List.of(), 2L, List.of(new ExamWeakConcept(500L, 2, 5), new ExamWeakConcept(501L, 1, 4))));
        given(lessonReader.getLessonTitles(any())).willReturn(Map.of(1L, "수업1", 2L, "수업2"));
        given(lessonReader.getCourseIdsByLesson(any())).willReturn(Map.of(1L, 100L, 2L, 100L));
        given(courseReader.getCourseTitles(any())).willReturn(Map.of(100L, "코스100"));
        given(lessonReader.getActivityTitles(any())).willReturn(Map.of(500L, "개념500", 501L, "개념501"));

        List<AdminLearningAnalytics.ExamLessonStat> result = service.getAnalytics(FROM, TO).examLessons();

        assertThat(result).containsExactly(
                new AdminLearningAnalytics.ExamLessonStat(2L, "코스100", "수업2", 60.2, List.of("개념500", "개념501")),
                new AdminLearningAnalytics.ExamLessonStat(1L, "코스100", "수업1", 80.5, List.of()));
    }

    @Test
    void 모든_지표에_같은_기간을_넘긴다() {
        givenEmptyReaders();

        service.getAnalytics(FROM, TO);

        verify(courseReader).findDiagnosisRecommendedTracks(FROM, TO);
        verify(lessonReader).getDropOffRateByLesson(FROM, TO);
        verify(quizReader).getWrongRateByConceptTag(FROM, TO);
        verify(examReader).getAverageScoreByLesson(FROM, TO);
    }
}
