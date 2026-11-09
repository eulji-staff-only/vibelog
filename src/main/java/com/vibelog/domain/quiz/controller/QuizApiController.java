package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.quiz.model.dto.QuizCheckRequest;
import com.vibelog.domain.quiz.model.dto.QuizCheckResponse;
import com.vibelog.domain.quiz.model.dto.QuizStartResponse;
import com.vibelog.domain.quiz.model.dto.QuizSubmitResponse;
import com.vibelog.domain.quiz.model.service.QuizSolveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * [QuizApiController]
 * 역할 : 퀴즈 화면 안의 동작 (API-LSN-04~06). 화면이 바뀌지 않으므로 JSON 으로 답한다 (quiz.md "퀴즈").
 * 책임 : 회원만 부른다 (SecurityConfig). 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다.
 *        실패는 GlobalExceptionHandler 가 {code, message} JSON 으로 보낸다 (_template.md ⑤).
 */
@RestController
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizSolveService quizSolveService;

    /* API-LSN-04 풀이 시작 */
    @PostMapping("/api/quizzes/{activityId}/attempts")
    public QuizStartResponse start(@PathVariable Long activityId, @AuthenticationPrincipal LoginMember loginMember) {
        return quizSolveService.start(loginMember.getMemberId(), activityId);
    }

    /* API-LSN-05 문항 확인 */
    @PostMapping("/api/quiz-attempts/{quizAttemptId}/answers")
    public QuizCheckResponse check(@PathVariable Long quizAttemptId, @Valid @RequestBody QuizCheckRequest request,
                                   @AuthenticationPrincipal LoginMember loginMember) {
        return quizSolveService.check(loginMember.getMemberId(), quizAttemptId, request.quizQuestionId(), request.quizChoiceId());
    }

    /* API-LSN-06 제출·채점 */
    @PostMapping("/api/quiz-attempts/{quizAttemptId}/submit")
    public QuizSubmitResponse submit(@PathVariable Long quizAttemptId, @AuthenticationPrincipal LoginMember loginMember) {
        return quizSolveService.submit(loginMember.getMemberId(), quizAttemptId);
    }
}
