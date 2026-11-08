package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.model.policy.LoginDiagnosisChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/* 계약 ⑫ LoginDiagnosisChecker (#12): 진단을 마쳤거나 건너뛴 기록이 있으면 true */
@SpringBootTest
@Transactional
class CourseContractServiceTest {

    @Autowired
    LoginDiagnosisChecker loginDiagnosisChecker;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 진단을_마쳤거나_건너뛴_기록이_있으면_true_없으면_false() {
        long none = member("diag-none@vibelog.test");
        long skipped = member("diag-skip@vibelog.test");
        long completed = member("diag-done@vibelog.test");
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status) VALUES (?, NULL, 'SKIPPED')", skipped);
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9401, '트랙', '대상', '설명', 9401)");
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status) VALUES (?, 9401, 'COMPLETED')", completed);

        assertThat(loginDiagnosisChecker.hasDiagnosisRecord(none)).isFalse();
        assertThat(loginDiagnosisChecker.hasDiagnosisRecord(skipped)).isTrue();
        assertThat(loginDiagnosisChecker.hasDiagnosisRecord(completed)).isTrue();
    }

    private long member(String email) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', '진단회원')", email);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
