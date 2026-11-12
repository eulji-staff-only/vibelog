package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.entity.BodyFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* 개념 본문 그리기 (FR-LSN-002, #473): 마크다운 서식은 HTML 로 보이고, 운영자가 올린 그림만 <img>, 그 밖의 주소·태그는 글자로만 보인다 (DB 없이 도는 단위 테스트) */
class ConceptBodyRendererTest {

    private static final String FILE = "123e4567-e89b-12d3-a456-426614174000.png";

    private String markdown(String body) {
        return ConceptBodyRenderer.render(body, BodyFormat.MARKDOWN);
    }

    @Test
    void 이미지_올리기로_넣은_그림_문법은_img_태그가_된다() {
        String html = markdown("설명입니다.\n![예시 화면](/uploads/" + FILE + ")\n끝");

        assertThat(html)
                .contains("<img class=\"ls-body-img\" src=\"/uploads/" + FILE + "\" alt=\"예시 화면\"")
                .doesNotContain("![")
                .contains("설명입니다.<br>");
    }

    @Test
    void 설명이_비어_있어도_그림이_된다() {
        assertThat(markdown("![](/uploads/abc-123.jpg)")).contains("src=\"/uploads/abc-123.jpg\" alt=\"\"");
    }

    @Test
    void 설명에_달러_기호나_역슬래시가_있어도_그대로_나온다() {
        assertThat(markdown("![가격 $1 \\ 끝](/uploads/" + FILE + ")")).contains("alt=\"가격 $1 \\ 끝\"");
    }

    @Test
    void 업로드_폴더_밖의_주소는_그림이_되지_않고_글자로_보인다() {
        // 외부 주소·javascript: 주소·폴더 이탈·하위 폴더·허용하지 않는 확장자는 모두 그림으로 바꾸지 않는다
        for (String body : new String[]{
                "![x](https://evil.example/a.png)",
                "![x](javascript:alert(1))",
                "![x](/uploads/../secret.png)",
                "![x](/uploads/sub/a.png)",
                "![x](/uploads/a.svg)",
                "![x](/images/a.png)"}) {
            assertThat(markdown(body)).as(body).doesNotContain("<img");
        }
    }

    @Test
    void 본문의_태그는_이스케이프되고_설명의_따옴표로_속성을_빠져나갈_수_없다() {
        String html = markdown("<script>alert(1)</script>\n![a\"onerror=\"x](/uploads/" + FILE + ")");

        assertThat(html)
                .contains("&lt;script&gt;")
                .doesNotContain("<script>")
                .contains("<img class=\"ls-body-img\"")
                .doesNotContain("onerror=\"");
    }

    @Test
    void 리치_텍스트는_그대로_쓰고_본문이_없으면_null이다() {
        assertThat(ConceptBodyRenderer.render("<p>글</p>", BodyFormat.RICH_TEXT)).isEqualTo("<p>글</p>");
        assertThat(ConceptBodyRenderer.render(null, BodyFormat.MARKDOWN)).isNull();
    }

    @Test
    void 엔터_한_번으로_나눈_줄도_줄바꿈으로_보인다() {
        assertThat(markdown("첫 줄\r\n둘째 줄")).isEqualTo("<p>첫 줄<br>\n둘째 줄</p>\n");
    }

    @Test
    void 제목_굵게_목록_코드는_서식으로_보이고_기호는_남지_않는다() {
        String html = markdown("## 범위 정하기\n\n**고칠 곳**을 적는다.\n\n- 첫째\n- 둘째\n\n1. 하나\n2. 둘\n\n`git status` 로 확인");

        assertThat(html)
                .contains("<h2>범위 정하기</h2>")
                .contains("<strong>고칠 곳</strong>")
                .contains("<ul>\n<li>첫째</li>")
                .contains("<ol>\n<li>하나</li>")
                .contains("<code>git status</code>")
                .doesNotContain("##")
                .doesNotContain("**");
    }

    @Test
    void HTML_태그로_시작하는_여러_줄도_글자로_보이고_줄바꿈이_남는다() {
        String html = markdown("<div>\n첫 줄\n둘째 줄\n</div>");

        assertThat(html)
                .isEqualTo("<p>&lt;div&gt;<br>\n첫 줄<br>\n둘째 줄<br>\n&lt;/div&gt;</p>\n")
                .doesNotContain("<div>");
    }

    @Test
    void 링크의_javascript_주소는_지워진다() {
        assertThat(markdown("[눌러 보기](javascript:alert(1))"))
                .contains("눌러 보기")
                .doesNotContain("javascript:");
    }
}
