package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dto.DiagnosisAdminView;
import com.vibelog.domain.course.model.dto.DiagnosisOptionForm;
import com.vibelog.domain.course.model.dto.DiagnosisPreview;
import com.vibelog.domain.course.model.dto.DiagnosisQuestionForm;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 진단 설정 (API-ADM-41~45, FR-DGN-004, #187)
 * - 보기 2개 이상·점수 1 이상·있는 트랙만, 빈 줄은 보기로 치지 않는다
 * - 수정: 보기 내용이 그대로면 같은 ID 로 트랙·점수만, 내용이 바뀌면 새 ID 로 (예전 ID 로는 진단 제출이 안 된다), 폼에 없는 보기는 지운다
 * - 마지막 사용 중 문항은 중지할 수 없다
 * - 테스트 응답은 실제 진단과 같은 계산이고 저장하지 않는다
 * DB 의 샘플 문항이 섞이지 않게 사용 중 문항을 잠시 끄고(트랜잭션이라 되돌려진다) 자기 문항(9541~)만 쓴다.
 */
@SpringBootTest
@Transactional
class DiagnosisAdminServiceTest {

    @Autowired
    DiagnosisAdminService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE diagnosis_question SET is_active = FALSE");
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9541, '설정 Starter', '대상', '설명', 9541), (9542, '설정 Controller', '대상', '설명', 9542)");
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order, is_active) VALUES "
                + "(9541, '설정 문항1', 1, TRUE), (9542, '설정 문항2', 2, TRUE)");
        jdbcTemplate.update("INSERT INTO diagnosis_option (diagnosis_option_id, diagnosis_question_id, track_id, content, score, sort_order) VALUES "
                + "(95411, 9541, 9541, '처음이에요', 1, 1), (95412, 9541, 9542, '범위가 넓어져요', 1, 2), "
                + "(95421, 9542, 9541, '요청을 배우고 싶어요', 1, 1), (95422, 9542, 9542, '바뀐 곳만 볼래요', 3, 2)");
    }

    @Test
    void 문항을_추가하면_사용_중으로_저장되고_빈_줄은_빼고_순서대로_저장된다() {
        service.createQuestion(form("새 문항", 3, row(null, "보기A", 9541L, 2), new DiagnosisOptionForm(), row(null, "보기B", 9542L, 1)));
        flush();

        DiagnosisAdminView.Question added = mine(service.getView()).stream()
                .filter(q -> q.content().equals("새 문항")).findFirst().orElseThrow();
        assertThat(added.active()).isTrue();
        assertThat(added.options()).extracting(DiagnosisAdminView.Option::content).containsExactly("보기A", "보기B");
        assertThat(added.options()).extracting(DiagnosisAdminView.Option::score).containsExactly(2, 1);
    }

    @Test
    void 보기가_2개_미만이거나_점수가_1_미만이거나_없는_트랙이면_저장하지_않는다() {
        assertError(() -> service.createQuestion(form("문항", 1, row(null, "보기A", 9541L, 1))));
        assertError(() -> service.createQuestion(form("문항", 1, row(null, "보기A", 9541L, 0), row(null, "보기B", 9542L, 1))));
        assertError(() -> service.createQuestion(form("문항", 1, row(null, "보기A", 9549L, 1), row(null, "보기B", 9542L, 1))));
        assertError(() -> service.createQuestion(form(" ", 1, row(null, "보기A", 9541L, 1), row(null, "보기B", 9542L, 1))));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_question WHERE content = '문항'", Integer.class)).isZero();
    }

    @Test
    void 보기_내용이_그대로면_같은_ID로_고치고_바뀌면_새_ID로_저장하며_빠진_보기는_지운다() {
        // 95411: 내용 그대로, 점수만 5 / 95412: 내용 바뀜 / 새 보기 하나 추가
        service.updateQuestion(9541L, form("고친 문항", 1,
                row(95411L, "처음이에요", 9541L, 5), row(95412L, "범위가 계속 넓어져요", 9542L, 1), row(null, "새 보기", 9541L, 1)));
        flush();

        assertThat(jdbcTemplate.queryForObject("SELECT score FROM diagnosis_option WHERE diagnosis_option_id = 95411", Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_option WHERE diagnosis_option_id = 95412", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT content FROM diagnosis_option WHERE diagnosis_question_id = 9541 ORDER BY sort_order", String.class))
                .containsExactly("처음이에요", "범위가 계속 넓어져요", "새 보기");
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM diagnosis_question WHERE diagnosis_question_id = 9541", String.class))
                .isEqualTo("고친 문항");

        // 예전 보기 ID 로는 테스트 응답(실제 진단과 같은 검사)이 안 된다
        assertThat(service.preview(List.of(95412L, 95421L))).isEmpty();

        // 폼에서 뺀 보기는 지운다
        service.updateQuestion(9542L, form("설정 문항2", 2, row(95421L, "요청을 배우고 싶어요", 9541L, 1), row(null, "다른 보기", 9542L, 1)));
        flush();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_option WHERE diagnosis_option_id = 95422", Integer.class)).isZero();
    }

    @Test
    void 마지막_사용_중_문항은_중지할_수_없다() {
        assertThat(service.changeActive(9541L, false)).isTrue();
        flush();
        assertThat(service.changeActive(9542L, false)).isFalse();
        flush();
        assertThat(jdbcTemplate.queryForObject("SELECT is_active FROM diagnosis_question WHERE diagnosis_question_id = 9542", Boolean.class)).isTrue();

        assertThat(service.changeActive(9541L, true)).isTrue();
        assertError(() -> service.changeActive(9549L, true));
    }

    @Test
    void 테스트_응답은_추천_트랙과_트랙별_합계를_보여주고_저장하지_않는다() {
        int before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_result", Integer.class);

        // Starter 1 + Controller 3 → Controller
        DiagnosisPreview preview = service.preview(List.of(95411L, 95422L)).orElseThrow();
        assertThat(preview.recommendedTrackId()).isEqualTo(9542L);
        assertThat(preview.recommendedTrackName()).isEqualTo("설정 Controller");
        assertThat(preview.scores()).filteredOn(score -> score.trackId() >= 9541L && score.trackId() <= 9542L)
                .extracting(DiagnosisPreview.TrackScore::score).containsExactly(1L, 3L);

        assertThat(service.preview(List.of(95411L))).isEmpty();               // 문항 수와 답 수가 다름
        assertThat(service.preview(List.of(95411L, 95412L))).isEmpty();       // 같은 문항에 두 개
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diagnosis_result", Integer.class)).isEqualTo(before);
    }

    private List<DiagnosisAdminView.Question> mine(DiagnosisAdminView view) {
        return view.questions().stream().filter(q -> q.content().startsWith("설정") || q.content().equals("새 문항")).toList();
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    private static DiagnosisQuestionForm form(String content, int sortOrder, DiagnosisOptionForm... options) {
        return DiagnosisQuestionForm.of(content, sortOrder, List.of(options));
    }

    private static DiagnosisOptionForm row(Long id, String content, Long trackId, Integer score) {
        return DiagnosisOptionForm.of(id, content, trackId, score);
    }

    private void assertError(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo((ErrorCode) CourseErrorCode.INVALID_INPUT);
    }
}
