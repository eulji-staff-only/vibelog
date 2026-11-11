package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.entity.BodyFormat;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/*
 * [ConceptBodyRenderer]
 * 역할 : 개념 본문(activity.body)을 학습자 화면에 그릴 안전한 HTML 로 바꾼다 (FR-LSN-002).
 * 책임 : - RICH_TEXT 는 이미 HTML 이라고 보고 그대로 쓴다.
 *        - MARKDOWN 은 commonmark 로 제목·굵게·목록·코드를 그린다 (#473, #384 의 "그림·줄바꿈만" 을 넓힘).
 *          화면이 th:utext 로 그대로 넣으므로 아래 안전 규칙을 지킨다.
 *          · 본문에 쓴 HTML 태그는 그리지 않고 글자로 보인다 (escapeHtml)
 *          · javascript: 같은 위험한 링크 주소는 지운다 (sanitizeUrls)
 *          · 그림은 운영자가 이미지 올리기로 넣은 /uploads/{UUID}.{jpg|jpeg|png|webp} 만 <img> 로 그린다.
 *            그 밖의 주소는 그림이 되지 않고 ![설명](주소) 글자 그대로 보인다 (FileStorage 파일 이름 형식)
 *          · 줄바꿈 하나도 <br> 로 바꾼다 (운영자가 엔터 한 번으로 줄을 나눈 글이 지금처럼 보이게)
 */
public final class ConceptBodyRenderer {

    private static final Pattern UPLOAD_IMAGE = Pattern.compile("/uploads/[A-Za-z0-9-]+\\.(?:jpe?g|png|webp)");

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .softbreak("<br>\n")
            .nodeRendererFactory(UploadImageRenderer::new)
            .build();

    private ConceptBodyRenderer() {
    }

    public static String render(String body, BodyFormat bodyFormat) {
        if (body == null) {
            return null;
        }
        if (bodyFormat == BodyFormat.RICH_TEXT) {
            return body;
        }
        return RENDERER.render(PARSER.parse(body));
    }

    /* 그림 노드만 맡는다. 업로드 파일이면 <img>, 아니면 원래 글자 그대로 (속성·글자는 HtmlWriter 가 이스케이프한다) */
    private static final class UploadImageRenderer implements NodeRenderer {

        private final HtmlWriter html;

        private UploadImageRenderer(HtmlNodeRendererContext context) {
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Image.class);
        }

        @Override
        public void render(Node node) {
            Image image = (Image) node;
            String alt = altText(image);
            String src = image.getDestination();
            if (!UPLOAD_IMAGE.matcher(src).matches()) {
                html.text("![" + alt + "](" + src + ")");
                return;
            }
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("class", "ls-body-img");
            attrs.put("src", src);
            attrs.put("alt", alt);
            html.tag("img", attrs, true);
        }

        private static String altText(Image image) {
            StringBuilder alt = new StringBuilder();
            image.accept(new AbstractVisitor() {
                @Override
                public void visit(Text text) {
                    alt.append(text.getLiteral());
                }

                @Override
                public void visit(Code code) {
                    alt.append(code.getLiteral());
                }

                @Override
                public void visit(SoftLineBreak softLineBreak) {
                    alt.append(' ');
                }

                @Override
                public void visit(HardLineBreak hardLineBreak) {
                    alt.append(' ');
                }
            });
            return alt.toString();
        }
    }
}
