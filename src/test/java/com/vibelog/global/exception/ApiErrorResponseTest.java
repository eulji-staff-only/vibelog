package com.vibelog.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * /api/** 요청의 응답 모양 (D-08, #152): 성공은 DTO 그대로, 실패는 상태 코드 + {code, message}.
 * 화면 요청의 실패는 지금처럼 오류 화면(HTML)이다. 테스트 전용 /api/test 컨트롤러로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiErrorResponseTest.TestApi.class)
class ApiErrorResponseTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser
    void 성공은_감싸지_않고_값을_그대로_보낸다() throws Exception {
        mockMvc.perform(get("/api/test/ok"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"checkedCount\": 2, \"totalCount\": 5}", true));
    }

    @Test
    @WithMockUser
    void 업무_규칙_위반은_에러_코드의_상태와_code_message_로_보낸다() throws Exception {
        mockMvc.perform(get("/api/test/business"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\": \"TEST_DENIED\", \"message\": \"테스트로 막았습니다.\"}", true));
    }

    @Test
    @WithMockUser
    void 입력_검증_실패와_읽을_수_없는_본문은_INVALID_INPUT() throws Exception {
        mockMvc.perform(post("/api/test/valid").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("이름을 입력해 주세요"));
        mockMvc.perform(post("/api/test/valid").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{깨진 JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        // 잘못된 메서드·Content-Type 도 서버 오류가 아니라 입력 오류다
        mockMvc.perform(get("/api/test/valid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/test/valid").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUser
    void 없는_주소와_예상하지_못한_오류도_JSON_이고_내부_정보는_보내지_않는다() throws Exception {
        mockMvc.perform(get("/api/test/no-such"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(content().string(not(containsString("내부 비밀"))));
    }

    @Test
    void 비로그인은_401_LOGIN_REQUIRED() throws Exception {
        mockMvc.perform(get("/api/test/ok"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"code\": \"LOGIN_REQUIRED\", \"message\": \"로그인이 필요합니다.\"}", true));
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자의_운영자_API_와_CSRF_없는_요청은_403_JSON() throws Exception {
        mockMvc.perform(get("/api/admin/anything"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
        mockMvc.perform(post("/api/test/valid").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"a\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 운영자의_CSRF_없는_요청은_ADMIN_ONLY_가_아니라_FORBIDDEN() throws Exception {
        mockMvc.perform(post("/api/admin/anything"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser
    void DB_제약_위반은_409_CONFLICT() throws Exception {
        mockMvc.perform(get("/api/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser
    void 화면_요청의_오류는_그대로_오류_화면이다() throws Exception {
        mockMvc.perform(get("/no-such-page"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @RestController
    static class TestApi {

        enum TestErrorCode implements ErrorCode {
            TEST_DENIED;

            @Override
            public HttpStatus getStatus() {
                return HttpStatus.FORBIDDEN;
            }

            @Override
            public String getMessage() {
                return "테스트로 막았습니다.";
            }
        }

        record NameRequest(@NotBlank(message = "이름을 입력해 주세요") String name) {
        }

        @GetMapping("/api/test/ok")
        Map<String, Integer> ok() {
            return Map.of("checkedCount", 2, "totalCount", 5);
        }

        @GetMapping("/api/test/business")
        void business() {
            throw new BusinessException(TestErrorCode.TEST_DENIED);
        }

        @PostMapping("/api/test/valid")
        void valid(@Valid @RequestBody NameRequest request) {
        }

        @GetMapping("/api/test/conflict")
        void conflict() {
            throw new DataIntegrityViolationException("중복");
        }

        @GetMapping("/api/test/boom")
        void boom() {
            throw new IllegalStateException("내부 비밀");
        }
    }
}
