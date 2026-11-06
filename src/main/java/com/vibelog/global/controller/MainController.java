package com.vibelog.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * [MainController]
 * 역할 : 메인(랜딩) 화면 `/` (U-01) 을 보여준다. 어느 도메인 영역에도 속하지 않는 화면이라 global 에 둔다 (#13).
 * 책임 : 화면 이름만 돌려준다. 로그인 상태별 메뉴·추천 데이터 연결은 로그인·권한 이슈(#12)와
 *        각 영역 기능이 준비된 뒤 추가한다 (지금은 화면 흐름 설계서 기준 샘플 문구로 채워진 화면).
 */
@Controller
public class MainController {

    @GetMapping("/")
    public String main() {
        return "common/main";
    }
}
