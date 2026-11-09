package com.vibelog.domain.exam.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.exam.model.dto.ExamAnswerRequest;
import com.vibelog.domain.exam.model.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * [ExamAnswerApiController]
 * 역할 : 응시 중 답 저장(API-EXM-03, JSON)을 받는다.
 * 책임 : 화면이 바뀌지 않는 동작이라 JSON 으로 답한다. 돌려줄 값이 없어 성공은 204 (_template.md 7-1 ⑤).
 *        실패(ATTEMPT_SUBMITTED·NOT_OWNER·INVALID_INPUT)는 GlobalExceptionHandler 가 {code, message} 로 보낸다.
 */
@RestController
@RequiredArgsConstructor
public class ExamAnswerApiController {

    private final ExamService examService;

    @PostMapping("/api/exam-attempts/{examAttemptId}/answers")
    public ResponseEntity<Void> saveAnswer(@PathVariable Long examAttemptId,
                                           @Valid @RequestBody ExamAnswerRequest request,
                                           @AuthenticationPrincipal LoginMember loginMember) {
        examService.saveAnswer(loginMember.getMemberId(), examAttemptId,
                request.examQuestionId(), request.examChoiceId());
        return ResponseEntity.noContent().build();
    }
}
