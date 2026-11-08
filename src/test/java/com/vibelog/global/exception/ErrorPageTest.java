package com.vibelog.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.global.controller.MainController;
import java.util.Locale;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

/*
 * [ErrorPageTest]
 * 역할 : 오류 화면(E-01~E-03, #46)이 문법 오류 없이 그려지는지 확인한다. 템플릿 문법 오류(예: 이번에 실제로
 *        발견한 "th:attr 중복", "fragment 이름이 HTML 태그 이름과 겹침")는 Java 컴파일로는 잡히지 않아
 *        화면 테스트로 직접 그려서 확인한다 (CLAUDE.md "완료 전 확인", ExamPageControllerTest 와 같은 방식).
 * 책임 : - GlobalExceptionHandler 가 Controller 흐름에서 생긴 오류를 바로 그리는 error/error.html
 *        - Spring Boot 가 Controller 를 거치지 않은 오류를 "/error" 로 보낼 때 상태 코드로 찾는
 *          error/404·403·500.html (SecurityConfig 의 403이 실제로 타는 경로). 이 셋은 BasicErrorController 의
 *          콘텐츠 협상·DefaultErrorViewResolver 가 슬라이스 테스트에서는 실제 앱과 다르게 움직여서, 뷰 이름을
 *          직접 렌더링해 "문법 오류 없이 그려지는지"만 확인한다 (뷰 이름 라우팅 자체는 Spring Boot 기본 동작).
 */
@WebMvcTest(MainController.class)
@WithMockUser   // @WebMvcTest 는 SecurityConfig 를 읽지 않고 기본 보안으로 모든 주소를 막는다 (CLAUDE.md 화면 선행 작업)
class ErrorPageTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ThymeleafViewResolver viewResolver;

    @MockitoBean
    MemberReader memberReader;

    @Test
    void 컨트롤러_흐름에서_생긴_404는_error_error_로_그려진다() throws Exception {
        mockMvc.perform(get("/no-such-url-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/error"))
                .andExpect(content().string(Matchers.containsString("페이지를 찾을 수 없습니다")));
    }

    @Test
    void error_404_템플릿이_문법_오류_없이_그려진다() throws Exception {
        assertThat(render("error/404")).contains("페이지를 찾을 수 없습니다");
    }

    @Test
    void error_403_템플릿이_문법_오류_없이_그려진다() throws Exception {
        assertThat(render("error/403")).contains("접근할 수 없습니다");
    }

    @Test
    void error_500_템플릿이_문법_오류_없이_그려진다() throws Exception {
        assertThat(render("error/500")).contains("오류 번호");
    }

    private String render(String viewName) throws Exception {
        View view = viewResolver.resolveViewName(viewName, Locale.KOREAN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(Map.of(), request, response);
        return response.getContentAsString();
    }
}
