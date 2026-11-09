package com.vibelog.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * [SchedulingConfig]
 * 역할 : @Scheduled 배치를 켠다 (message 명세: 알림 90일·쪽지 1년 삭제, PL-NTF-002·PL-MSG-002).
 * 책임 : 켜기만 한다. 배치 메서드는 각 영역 Service 에 둔다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
