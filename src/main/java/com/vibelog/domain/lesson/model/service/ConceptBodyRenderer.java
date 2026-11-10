package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.entity.BodyFormat;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * [ConceptBodyRenderer]
 * 역할 : 개념 본문(activity.body)을 학습자 화면에 그릴 안전한 HTML 로 바꾼다 (FR-LSN-002).
 * 책임 : - 마크다운 파서 라이브러리는 추가하지 않는다 (CLAUDE.md "의존성을 마음대로 추가하지 않는다").
 *        - RICH_TEXT 는 이미 HTML 이라고 보고 그대로 쓴다.
 *        - MARKDOWN 은 먼저 모든 태그를 이스케이프한다. 그 뒤에 운영자가 이미지 올리기로 넣은 그림 문법
 *          ![설명](/uploads/{UUID}.{jpg|jpeg|png|webp}) 만 <img> 로 바꾸고, 줄바꿈은 <br> 로 바꾼다.
 *          주소는 서버가 만든 업로드 파일 이름 형식(FileStorage)만 허용한다. 그래서 외부 주소·javascript: 같은 주소는 그림이 되지 않고
 *          글자 그대로 보인다. 굵게·목록 같은 나머지 마크다운은 이번에 그리지 않는다.
 */
public final class ConceptBodyRenderer {

    /* 이스케이프가 끝난 글에서 찾는다. 설명에는 닫는 대괄호·줄바꿈이 없고, 주소는 FileStorage 가 만드는 파일 이름만 받는다 */
    private static final Pattern IMAGE = Pattern.compile(
            "!\\[([^\\]\\r\\n]*)]\\((/uploads/[A-Za-z0-9-]+\\.(?:jpe?g|png|webp))\\)");

    private ConceptBodyRenderer() {
    }

    public static String render(String body, BodyFormat bodyFormat) {
        if (body == null) {
            return null;
        }
        if (bodyFormat == BodyFormat.RICH_TEXT) {
            return body;
        }
        String escaped = HtmlUtils.htmlEscape(body);
        String withImages = IMAGE.matcher(escaped).replaceAll(match -> Matcher.quoteReplacement(toImageTag(match)));
        return withImages.replace("\r\n", "\n").replace("\n", "<br>\n");
    }

    /* alt 는 이미 이스케이프된 글이라 속성값에 그대로 넣어도 안전하다 (따옴표는 &quot; 로 바뀌어 있다) */
    private static String toImageTag(java.util.regex.MatchResult match) {
        return "<img class=\"ls-body-img\" src=\"" + match.group(2) + "\" alt=\"" + match.group(1) + "\">";
    }
}
