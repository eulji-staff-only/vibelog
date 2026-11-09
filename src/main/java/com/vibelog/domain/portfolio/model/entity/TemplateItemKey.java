package com.vibelog.domain.portfolio.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/*
 * [TemplateItemKey]
 * 역할 : 개인 작품 템플릿 항목 종류 (portfolio_template_item.item_key, ck_portfolio_template_item_item_key).
 * 책임 : 항목은 이 5개로 고정이다 (#168 결정 A). 값은 항목마다 정해진 portfolio 칸에 저장한다
 *        (TITLE → title, LINK → result_url, DESCRIPTION → summary, AI_TOOLS → ai_tools, RETROSPECTIVE → retrospective).
 *        defaultLabel·defaultGuide 는 테이블에 행이 없을 때 U-34 가 쓰는 P2 문구다 (portfolio.md P3).
 */
@Getter
@RequiredArgsConstructor
public enum TemplateItemKey {
    TITLE("제목", "작품 제목"),
    LINK("링크", "https://github.com/…"),
    DESCRIPTION("설명", "이 작품을 한 줄로 소개해 주세요"),
    AI_TOOLS("사용한 AI 도구", "예: Claude Code, Cursor"),
    RETROSPECTIVE("회고", "이 작품을 만들며 배운 점과 아쉬운 점");

    private final String defaultLabel;
    private final String defaultGuide;

    /* A-12 "기본 항목" 표시용. 폼이 모르는 키를 보내 다시 그릴 때도 화면이 깨지지 않게 그 값을 그대로 돌려준다 */
    public static String defaultLabelOf(String key) {
        return Arrays.stream(values()).filter(k -> k.name().equals(key)).findFirst()
                .map(TemplateItemKey::getDefaultLabel).orElse(key);
    }
}
