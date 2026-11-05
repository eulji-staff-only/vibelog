package com.vibelog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/* 애플리케이션이 뜨는지(설정 · DB 연결 · Bean 등록) 확인한다. 로컬 MySQL 이 켜져 있어야 통과한다. */
@SpringBootTest
class VibelogApplicationTests {

    @Test
    void contextLoads() {
    }
}
