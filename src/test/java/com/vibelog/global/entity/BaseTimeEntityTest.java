package com.vibelog.global.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * JPA Auditing 이 켜져 있어서 저장할 때 created_at, updated_at 이 자동으로 채워지는지 확인한다.
 * 실제 테이블을 건드리지 않도록 이 테스트 전용 테이블(test_base_time)을 만들고, 끝나면 지운다.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "DROP TABLE IF EXISTS test_base_time",
        "CREATE TABLE test_base_time (id BIGINT AUTO_INCREMENT PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL)"
})
@Sql(statements = "DROP TABLE IF EXISTS test_base_time", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class BaseTimeEntityTest {

    @Autowired
    EntityManager em;

    @Test
    void 저장하면_생성_수정_시각이_자동으로_채워진다() {
        TestBaseTime entity = new TestBaseTime();

        em.persist(entity);
        em.flush();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Entity
    @Table(name = "test_base_time")
    static class TestBaseTime extends BaseTimeEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
    }
}
