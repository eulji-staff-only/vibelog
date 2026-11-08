package com.vibelog.domain.auth.model.dto;

/* 마이페이지 "내 정보" (API-USR-15). joinMethod 는 화면에 보이는 가입 방식(이메일·구글·깃허브) */
public record MypageInfo(String email, String nickname, String joinMethod, boolean hasPassword) {
}
