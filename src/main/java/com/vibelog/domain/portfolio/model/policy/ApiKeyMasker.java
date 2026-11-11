package com.vibelog.domain.portfolio.model.policy;

import java.util.regex.Pattern;

/*
 * [ApiKeyMasker]
 * 역할 : 포트폴리오 글에 들어간 API 키 형식 문자열을 가린다 (PL-PRT-008, NFR-SEC-007).
 * 책임 : - 대상은 정책 그대로: sk-(OpenAI·Anthropic 등), sk_test_·sk_live_(Stripe 등 결제 서비스), ghp_·github_pat_(GitHub), AKIA(AWS), AIza(Google)로 시작하는 문자열.
 *        - 앞부분(접두어)은 남기고 뒤를 ****로 바꾼다 (예: sk-abc123 → sk-****, sk_test_abc → sk_test_****). 단어 중간(예: task-1)은 건드리지 않는다.
 *        - 이미 가린 값(sk_test_****)은 다시 가려도 그대로다. 수정·초안 복원 뒤 다시 가려도 접두어가 줄지 않는다.
 *        - sk_ 로 시작하는 평범한 이름(task_sk_status)은 키가 아니라 건드리지 않는다. 키 형식인 sk_test_·sk_live_ 만 본다.
 *        - 화면(portfolio.js)도 같은 형식으로 저장 전에 경고한다.
 */
public final class ApiKeyMasker {

    private static final Pattern API_KEY =
            Pattern.compile("(?<![A-Za-z0-9])(sk-|sk_test_|sk_live_|ghp_|github_pat_|AKIA|AIza)[A-Za-z0-9_\\-]+");

    private ApiKeyMasker() {
    }

    /* null 은 그대로 null (선택 항목) */
    public static String mask(String text) {
        return text == null ? null : API_KEY.matcher(text).replaceAll("$1****");
    }
}
