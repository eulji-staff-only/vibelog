package com.vibelog.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/*
 * [JpaAuditingConfig]
 * 역할 : BaseTimeEntity 의 created_at, updated_at 자동 기록을 켠다.
 * 책임 : 실행 클래스(VibelogApplication)에 붙이지 않고 따로 둔다.
 *        실행 클래스에 붙이면 @WebMvcTest 처럼 JPA 없이 띄우는 테스트가 실패하기 때문이다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
