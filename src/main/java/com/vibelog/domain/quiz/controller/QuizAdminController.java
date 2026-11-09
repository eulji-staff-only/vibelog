package com.vibelog.domain.quiz.controller;

import com.vibelog.domain.quiz.model.dto.QuizQuestionForm;
import com.vibelog.domain.quiz.model.service.QuizAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * [QuizAdminController]
 * 역할 : 운영자의 퀴즈 문항 추가·수정·사용 여부 요청 (A-06 활동 탭, API-ADM-26~28, FR-LSN-012, #102).
 * 책임 : - 운영자만 들어온다 (SecurityConfig /admin/**). 처리 뒤 A-06(/admin/lessons/{lessonId})으로 돌려보낸다.
 *        - 입력이 틀리면 보낸 값·칸별 메시지·연 모달을 flash 에 담아 A-06 이 그 모달을 열린 채로 다시 그린다
 *          (같은 폼 다시 + 칸별 메시지, _template.md ④). 모의고사 문항(ExamAdminController)과 같은 방식.
 *        - A-06 화면(lesson)은 quiz/admin-quiz-questions 조각을 끼워 쓴다.
 */
@Controller
@RequiredArgsConstructor
public class QuizAdminController {

    static final String FORM = "quizQuestionForm";
    static final String ERRORS = "quizQuestionErrors";
    static final String TARGET = "quizQuestionTarget";   // "new-{activityId}" 또는 수정 중인 문항 ID

    private final QuizAdminService quizAdminService;

    /* API-ADM-26 */
    @PostMapping("/admin/activities/{activityId}/questions")
    public String create(@PathVariable Long activityId, @Valid @ModelAttribute QuizQuestionForm form,
                         BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (keepErrors(form, bindingResult, "new-" + activityId, redirectAttributes)) {
            return lessonPage(quizAdminService.getLessonIdOfActivity(activityId));
        }
        return lessonPage(quizAdminService.create(activityId, form));
    }

    /* API-ADM-27 */
    @PostMapping("/admin/quiz-questions/{quizQuestionId}/edit")
    public String edit(@PathVariable Long quizQuestionId, @Valid @ModelAttribute QuizQuestionForm form,
                       BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (keepErrors(form, bindingResult, String.valueOf(quizQuestionId), redirectAttributes)) {
            return lessonPage(quizAdminService.getLessonIdOfQuestion(quizQuestionId));
        }
        return lessonPage(quizAdminService.edit(quizQuestionId, form));
    }

    /* API-ADM-28 */
    @PostMapping("/admin/quiz-questions/{quizQuestionId}/active")
    public String active(@PathVariable Long quizQuestionId, @RequestParam boolean active) {
        return lessonPage(quizAdminService.changeActive(quizQuestionId, active));
    }

    /* 칸별 메시지가 있으면 flash 에 담고 true. 보기 4칸의 오류는 "choices" 한 칸으로 모은다 */
    private boolean keepErrors(QuizQuestionForm form, BindingResult bindingResult, String target,
                               RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            String field = error.getField().startsWith("choices") ? "choices" : error.getField();
            errors.putIfAbsent(field, error.isBindingFailure() ? "입력 값을 확인해주세요." : error.getDefaultMessage());
        }
        if (form.needsCode()) {
            errors.putIfAbsent("codeSnippet", "코드 빈칸 채우기·결과 유추하기 문항은 코드를 입력해 주세요.");
        }
        if (errors.isEmpty()) {
            return false;
        }
        redirectAttributes.addFlashAttribute(FORM, form);
        redirectAttributes.addFlashAttribute(ERRORS, errors);
        redirectAttributes.addFlashAttribute(TARGET, target);
        return true;
    }

    private String lessonPage(Long lessonId) {
        return "redirect:/admin/lessons/" + lessonId;
    }
}
