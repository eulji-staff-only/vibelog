package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.model.dao.DiagnosisOptionRepository;
import com.vibelog.domain.course.model.dao.DiagnosisQuestionRepository;
import com.vibelog.domain.course.model.entity.DiagnosisOption;
import com.vibelog.domain.course.model.entity.DiagnosisQuestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/* course 기반(#83): Entity 가 스키마와 맞게 읽히는지, CourseReader 가 이름을 돌려주는지 */
@SpringBootTest
@Transactional
class CourseReaderTest {

    @Autowired
    CourseReader courseReader;

    @Autowired
    DiagnosisQuestionRepository diagnosisQuestionRepository;

    @Autowired
    DiagnosisOptionRepository diagnosisOptionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 트랙_카테고리_코스_이름을_ID로_읽고_없는_ID는_빠진다() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9501, '리더 트랙', '대상', '설명', 9501)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9501, '웹 기초', 9501)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) "
                + "VALUES (9501, 9501, 9501, '첫 코스', '한 줄', '소개', '대상', '목표', 7, TRUE)");

        assertThat(courseReader.getTrackTitles(List.of(9501L, 9999L))).isEqualTo(Map.of(9501L, "리더 트랙"));
        assertThat(courseReader.getCategoryNames(List.of(9501L, 9999L))).isEqualTo(Map.of(9501L, "웹 기초"));
        assertThat(courseReader.getCourseTitles(List.of(9501L, 9999L))).isEqualTo(Map.of(9501L, "첫 코스"));
    }

    @Test
    void 진단_설문에는_사용_중인_문항만_순서대로_나오고_보기가_문항에_묶인다() {
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9502, '진단 트랙', '대상', '설명', 9502)");
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order, is_active) VALUES (9502, '둘째 문항', 9502, TRUE)");
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order, is_active) VALUES (9501, '첫 문항', 9501, TRUE)");
        jdbcTemplate.update("INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order, is_active) VALUES (9503, '꺼진 문항', 9503, FALSE)");
        jdbcTemplate.update("INSERT INTO diagnosis_option (diagnosis_question_id, track_id, content, score, sort_order) VALUES (9501, 9502, '보기', 2, 1)");

        List<Long> activeIds = diagnosisQuestionRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(DiagnosisQuestion::getDiagnosisQuestionId)
                .filter(id -> id >= 9501)
                .toList();
        List<DiagnosisOption> options = diagnosisOptionRepository.findByDiagnosisQuestionIdInOrderBySortOrderAsc(List.of(9501L));

        assertThat(activeIds).containsExactly(9501L, 9502L);
        assertThat(options).hasSize(1);
        assertThat(options.get(0).getTrackId()).isEqualTo(9502L);
        assertThat(options.get(0).getScore()).isEqualTo(2);
    }
}
