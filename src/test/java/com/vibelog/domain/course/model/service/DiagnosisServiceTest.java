package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.DiagnosisResultView;
import com.vibelog.domain.course.model.dto.DiagnosisSurveyView;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 진단 (API-DGN-01~04, #99)
 * - 응답(고른 보기)은 저장하지 않고 결과(완료·건너뜀, 추천 트랙)만 저장한다 (PL-DGN-001)
 * - 문항마다 정확히 하나씩 답하지 않으면 INVALID_INPUT
 * - 결과가 없거나 건너뛰었으면 결과 화면 데이터가 없다 (→ /diagnosis)
 * DB 에 다른 진단 문항이 있으면 설문이 섞이므로, 이 테스트는 사용 중인 문항을 잠시 끄고 자기 문항(98xx)만 켠다 (트랜잭션이라 되돌려진다).
 */
@SpringBootTest
@Transactional
class DiagnosisServiceTest {

    @Autowired
    DiagnosisService diagnosisService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long memberId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE diagnosis_question SET is_active = FALSE");
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9811, '진단 Starter', '입문 대상', '입문 설명', 9811), (9812, '진단 Controller', '통제 대상', '통제 설명', 9812)");
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order, is_active) VALUES "
                + "(9811, '문항1', 1, TRUE), (9812, '문항2', 2, TRUE), (9813, '꺼진 문항', 3, FALSE)");
        jdbcTemplate.update("INSERT INTO diagnosis_option (diagnosis_option_id, diagnosis_question_id, track_id, content, score, sort_order) VALUES "
                + "(98111, 9811, 9811, '처음이에요', 1, 1), (98112, 9811, 9812, '범위가 자꾸 넓어져요', 1, 2), "
                + "(98121, 9812, 9811, '요청부터 배우고 싶어요', 1, 1), (98122, 9812, 9812, '바뀐 곳만 확인하고 싶어요', 1, 2), "
                + "(98131, 9813, 9811, '꺼진 문항 보기', 1, 1)");
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES ('diag-svc@vibelog.test', 'x', '진단서비스')");
        memberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void 설문은_사용_중인_문항만_순서대로_보기와_함께_보여준다() {
        DiagnosisSurveyView survey = diagnosisService.getSurvey();

        assertThat(survey.questions()).extracting(DiagnosisSurveyView.Question::content).containsExactly("문항1", "문항2");
        assertThat(survey.questions().get(0).options()).extracting(DiagnosisSurveyView.Option::content)
                .containsExactly("처음이에요", "범위가 자꾸 넓어져요");
    }

    @Test
    void 제출하면_추천_트랙만_저장하고_고른_보기는_저장하지_않는다() {
        DiagnosisService.SubmitResult result = diagnosisService.submit(memberId, List.of(98112L, 98122L));

        assertThat(result.recommendedTrackId()).isEqualTo(9812L);
        assertThat(result.reasons()).containsExactly("범위가 자꾸 넓어져요", "바뀐 곳만 확인하고 싶어요");

        Map<String, Object> saved = jdbcTemplate.queryForMap(
                "SELECT status, recommended_track_id FROM diagnosis_result WHERE member_id = ?", memberId);
        assertThat(saved.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) saved.get("recommended_track_id")).longValue()).isEqualTo(9812L);
        // diagnosis_result 에는 고른 보기를 담을 칸이 없다. 진단 결과 1행만 생긴다
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_result WHERE member_id = ?", Integer.class, memberId))
                .isEqualTo(1);
    }

    @Test
    void 동점이면_트랙_순서가_앞인_트랙을_추천한다() {
        // Starter 1점, Controller 1점 → Starter(순서 앞)
        DiagnosisService.SubmitResult result = diagnosisService.submit(memberId, List.of(98112L, 98121L));

        assertThat(result.recommendedTrackId()).isEqualTo(9811L);
    }

    @Test
    void 문항마다_하나씩_답하지_않으면_INVALID_INPUT() {
        assertInvalid(List.of(98111L));                         // 답 수가 문항 수보다 적다
        assertInvalid(List.of(98111L, 98112L));                 // 같은 문항에 두 번
        assertInvalid(List.of(98111L, 98131L));                 // 꺼진 문항의 보기
        assertInvalid(List.of(98111L, 99999L));                 // 없는 보기
        assertInvalid(Arrays.asList(98111L, null));             // 빈 값
        assertInvalid(null);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_result WHERE member_id = ?", Integer.class, memberId))
                .isZero();
    }

    @Test
    void 건너뛰면_건너뜀으로_저장하고_결과_화면_데이터는_없다() {
        diagnosisService.skip(memberId);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM diagnosis_result WHERE member_id = ?", String.class, memberId))
                .isEqualTo("SKIPPED");
        assertThat(diagnosisService.getResult(memberId, null)).isEmpty();
    }

    @Test
    void 결과가_없으면_결과_화면_데이터가_없다() {
        assertThat(diagnosisService.getResult(memberId, null)).isEmpty();
    }

    @Test
    void 결과_화면은_추천_트랙과_다른_트랙을_보여주고_이유는_넘겨받은_것만_쓴다() {
        diagnosisService.submit(memberId, List.of(98112L, 98122L));

        Optional<DiagnosisResultView> withReasons = diagnosisService.getResult(memberId, List.of("범위가 자꾸 넓어져요"));
        assertThat(withReasons).isPresent();
        assertThat(withReasons.get().recommended().name()).isEqualTo("진단 Controller");
        assertThat(withReasons.get().reasons()).containsExactly("범위가 자꾸 넓어져요");
        assertThat(withReasons.get().otherTracks()).extracting(DiagnosisResultView.OtherTrack::trackId)
                .contains(9811L).doesNotContain(9812L);

        // 나중에 다시 열면 (flash 없음) 이유 없이 트랙 설명만
        assertThat(diagnosisService.getResult(memberId, null).get().reasons()).isEmpty();
    }

    private void assertInvalid(List<Long> optionIds) {
        assertThatThrownBy(() -> diagnosisService.submit(memberId, optionIds))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CourseErrorCode.INVALID_INPUT);
    }
}
