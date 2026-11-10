package com.vibelog.domain.portfolio.model.policy;

import java.util.regex.Pattern;

/*
 * [ApiKeyMasker]
 * 역할 : 포트폴리오 글에 들어간 API 키 형식 문자열을 가린다 (PL-PRT-008, NFR-SEC-007).
 * 책임 : - 대상은 정책 그대로: sk-(OpenAI·Anthropic 등), sk_(Stripe 등 결제 서비스 sk_test_·sk_live_), ghp_·github_pat_(GitHub), AKIA(AWS), AIza(Google)로 시작하는 문자열.
 *        - 앞부분(접두어)은 남기고 뒤를 ****로 바꾼다 (예: sk-abc123 → sk-****, sk_test_abc → sk_****). 단어 중간(예: task-1)은 건드리지 않는다.
 *        - 화면(portfolio.js)도 같은 형식으로 저장 전에 경고한다.
 */
public final class ApiKeyMasker {

    private static final Pattern API_KEY =
            Pattern.compile("(?<![A-Za-z0-9])(sk-|sk_|ghp_|github_pat_|AKIA|AIza)[A-Za-z0-9_\\-]+");

    private ApiKeyMasker() {
    }

    /* null 은 그대로 null (선택 항목) */
    public static String mask(String text) {
        return text == null ? null : API_KEY.matcher(text).replaceAll("$1****");
    }
}
