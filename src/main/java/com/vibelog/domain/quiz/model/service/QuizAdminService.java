package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.quiz.exception.QuizErrorCode;
import com.vibelog.domain.quiz.model.dao.QuizAnswerRepository;
import com.vibelog.domain.quiz.model.dao.QuizChoiceRepository;
import com.vibelog.domain.quiz.model.dao.QuizQuestionRepository;
import com.vibelog.domain.quiz.model.dto.QuizQuestionForm;
import com.vibelog.domain.quiz.model.entity.QuizChoice;
import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * [QuizAdminService]
 * 역할 : 운영자의 퀴즈 문항 추가·수정·사용 여부 (A-06 활동 탭, API-ADM-26~28, FR-LSN-012, #102).
 * 책임 : - 보기 4개·정답 1개, 코드 빈칸 채우기·결과 유추하기는 코드 필수. 어기면 INVALID_INPUT (화면 칸 메시지는 컨트롤러가 먼저 막는다).
 *        - 풀이 기록(확인한 답)이 있는 문항을 고치면 기존 문항은 중지하고 새 문항으로 저장한다 (DB 설계서 5-4).
 *          기존 학습자의 답은 예전 문항·보기를 그대로 가리킨다.
 *        - 퀴즈 활동인지·개념 태그가 있는지는 lesson 의 LessonReader 로 묻는다 (다른 영역 Entity 를 보지 않는다).
 *        - 돌려주는 값은 redirect 할 수업 ID (요청에는 활동·문항 ID 만 있다). 모의고사 문항(ExamAdminService)과 같은 방식.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizAdminService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final LessonReader lessonReader;

    /* API-ADM-26. 새 문항은 그 활동 마지막 문항 뒤에 붙고 사용 중으로 시작한다 */
    @Transactional
    public Long create(Long activityId, QuizQuestionForm form) {
        Long lessonId = getLessonIdOfActivity(activityId);
        requireValid(form);
        int sortOrder = quizQuestionRepository.findTopByActivityIdOrderBySortOrderDesc(activityId)
                .map(last -> last.getSortOrder() + 1)
                .orElse(1);
        saveNew(activityId, sortOrder, form);
        return lessonId;
    }

    /* API-ADM-27 */
    @Transactional
    public Long edit(Long quizQuestionId, QuizQuestionForm form) {
        QuizQuestion question = getQuestion(quizQuestionId);
        Long lessonId = getLessonIdOfActivity(question.getActivityId());
        requireValid(form);
        if (quizAnswerRepository.existsByQuizQuestionId(quizQuestionId)) {
            boolean active = question.isActive();   // 사용 중지해 둔 문항을 고쳐도 새 문항이 다시 살아나지 않게
            question.changeActive(false);
            saveNew(question.getActivityId(), question.getSortOrder(), form).changeActive(active);
            return lessonId;
        }
        question.edit(form.getConceptTagId(), form.getDisplayType(), form.getContent(), form.codeToSave(), form.getExplanation());

        // 풀이 기록이 없으니 보기를 가리키는 답도 없다. 보기 4개를 그 자리에서 고친다
        List<QuizChoice> choices = quizChoiceRepository.findByQuizQuestionIdOrderBySortOrderAsc(quizQuestionId);
        for (int i = 0; i < QuizQuestionForm.CHOICE_COUNT; i++) {
            String content = form.getChoices().get(i);
            boolean correct = i == form.getCorrectIndex();
            if (i < choices.size()) {
                choices.get(i).edit(content, correct);
            } else {
                quizChoiceRepository.save(QuizChoice.create(quizQuestionId, content, correct, i + 1));
            }
        }
        return lessonId;
    }

    /* API-ADM-28 */
    @Transactional
    public Long changeActive(Long quizQuestionId, boolean active) {
        QuizQuestion question = getQuestion(quizQuestionId);
        question.changeActive(active);
        return getLessonIdOfActivity(question.getActivityId());
    }

    /* 입력 오류로 A-06 에 돌려보낼 때 쓰는 수업 ID */
    public Long getLessonIdOfActivity(Long activityId) {
        return lessonReader.findLessonIdOfQuizActivity(activityId)
                .orElseThrow(() -> new BusinessException(QuizErrorCode.INVALID_INPUT));
    }

    public Long getLessonIdOfQuestion(Long quizQuestionId) {
        return getLessonIdOfActivity(getQuestion(quizQuestionId).getActivityId());
    }

    /* 새 문항과 보기 4개 */
    private QuizQuestion saveNew(Long activityId, int sortOrder, QuizQuestionForm form) {
        QuizQuestion question = quizQuestionRepository.save(QuizQuestion.create(activityId, form.getConceptTagId(),
                form.getDisplayType(), form.getContent(), form.codeToSave(), form.getExplanation(), sortOrder));
        for (int i = 0; i < QuizQuestionForm.CHOICE_COUNT; i++) {
            quizChoiceRepository.save(QuizChoice.create(question.getQuizQuestionId(), form.getChoices().get(i),
                    i == form.getCorrectIndex(), i + 1));
        }
        return question;
    }

    /* 보기 4개·정답 1개·코드 필수 유형·있는 개념 태그. 컨트롤러 검사를 거치지 않고 불러도 규칙이 깨지지 않게 다시 본다 */
    private void requireValid(QuizQuestionForm form) {
        boolean choicesOk = form.getChoices() != null && form.getChoices().size() == QuizQuestionForm.CHOICE_COUNT
                && form.getChoices().stream().noneMatch(choice -> choice == null || choice.isBlank());
        boolean correctOk = form.getCorrectIndex() != null
                && form.getCorrectIndex() >= 0 && form.getCorrectIndex() < QuizQuestionForm.CHOICE_COUNT;
        if (!choicesOk || !correctOk || form.getDisplayType() == null || form.needsCode()
                || form.getConceptTagId() == null
                || lessonReader.getConceptTagNames(List.of(form.getConceptTagId())).isEmpty()) {
            throw new BusinessException(QuizErrorCode.INVALID_INPUT);
        }
    }

    /* 명세의 실패는 INVALID_INPUT 뿐이라 없는 문항도 INVALID_INPUT 으로 본다 (모의고사 문항과 같은 방식) */
    private QuizQuestion getQuestion(Long quizQuestionId) {
        return quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(() -> new BusinessException(QuizErrorCode.INVALID_INPUT));
    }
}
