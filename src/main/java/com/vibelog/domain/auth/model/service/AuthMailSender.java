package com.vibelog.domain.auth.model.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/*
 * [AuthMailSender]
 * 역할 : 회원 영역의 메일(이메일 인증 #89, 비밀번호 재설정 #90)을 보낸다.
 * 책임 : - 메일 서버는 환경 변수(SPRING_MAIL_HOST 등)로만 켠다. 설정이 없으면 Spring 이 JavaMailSender 를 만들지 않으므로,
 *          메일 대신 내용을 로그에 남긴다. 로컬 개발에서 링크를 로그에서 복사해 확인한다.
 *        - 보내기에 실패해도 요청은 실패시키지 않고 로그만 남긴다. 사용자는 다시 받기로 새 링크를 받는다.
 *        - 링크 앞부분은 설정(vibelog.base-url)으로 만든다. 요청의 Host 헤더를 믿지 않는다.
 */
// ponytail: 요청 안에서 바로 보낸다 (메일 서버가 느리면 응답도 느림, 연결 시간 5초 제한). 느려지면 @Async 로 바꾼다
@Slf4j
@Component
public class AuthMailSender {

    private final ObjectProvider<JavaMailSender> javaMailSender;
    private final String from;
    private final String baseUrl;

    public AuthMailSender(ObjectProvider<JavaMailSender> javaMailSender,
                          @Value("${vibelog.mail.from}") String from,
                          @Value("${vibelog.base-url}") String baseUrl) {
        this.javaMailSender = javaMailSender;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    /* "/auth/verify?token=..." → "https://사이트주소/auth/verify?token=..." */
    public String link(String pathAndQuery) {
        return baseUrl + pathAndQuery;
    }

    public void send(String to, String subject, String text) {
        JavaMailSender sender = javaMailSender.getIfAvailable();
        if (sender == null) {
            log.warn("메일 서버 설정(SPRING_MAIL_HOST)이 없어 메일 대신 로그로 남깁니다 (개발용). to={}, subject={}\n{}", to, subject, text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (MailException e) {
            log.error("메일을 보내지 못했습니다. subject={}", subject, e);
        }
    }
}
