package com.vibelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * [VibelogApplication]
 * 역할 : 실행 진입점. 이 클래스의 main 을 실행해 서버를 켠다.
 * 책임 : 루트 패키지(com.vibelog)에 있으므로 하위의 모든 Bean / Entity / Repository 를 자동으로 스캔한다.
 *        이 클래스를 다른 패키지로 옮기지 않는다.
 */
@SpringBootApplication
public class VibelogApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibelogApplication.class, args);
    }
}
