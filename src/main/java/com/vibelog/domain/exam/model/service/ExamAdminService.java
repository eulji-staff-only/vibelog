package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.exception.ExamErrorCode;
import com.vibelog.domain.exam.model.dao.ExamAnswerRepository;
import com.vibelog.domain.exam.model.dao.ExamChoiceRepository;
import com.vibelog.domain.exam.model.dao.ExamQuestionRepository;
import com.vibelog.domain.exam.model.dto.ExamQuestionForm;
import com.vibelog.domain.exam.model.entity.ExamChoice;
import com.vibelog.domain.exam.model.entity.ExamQuestion;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * [ExamAdminService]
 * 역할 : 운영자의 모의고사 문항 추가·수정·사용 여부 변경 (A-06 모의고사 탭, API-ADM-29~31, FR-EXM-007).
 * 책임 : - 연관 개념은 그 수업의 개념 활동이어야 한다 (PL-EXM-002). 비었으면 CONCEPT_REQUIRED, 다른 수업·퀴즈 활동이면
 *          INVALID_INPUT. 개념인지는 lesson 영역 LessonReader 로 묻는다.
 *        - 보기는 4개 고정이다 (팀 결정).
 *        - 수정(DB 설계서 5-4): 응시 기록이 있는 문항은 고치지 않고 사용 중지한 뒤, 같은 순서 자리에 새 문항을 만든다.
 *          지난 응시의 리포트가 학습자가 실제로 푼 문항·보기 그대로 남게 하려는 것이다. 응시 기록이 없으면 그 자리에서 고친다.
 *        - 문항은 지우지 않고 사용 중지만 한다. 처리 뒤 돌아갈 수업 ID 를 돌려준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamAdminService {

    private final ExamQuestionRepository examQuestionRepository;
    private final ExamChoiceRepository examChoiceRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final LessonReader lessonReader;

    /* API-ADM-29. 새 문항은 그 수업 마지막 문항 뒤에 붙고 사용 중으로 시작한다 */
    @Transactional
    public Long create(Long lessonId, ExamQuestionForm form) {
        requireConceptOf(lessonId, form.getConceptActivityId());
        int sortOrder = examQuestionRepository.findTopByLessonIdOrderBySortOrderDesc(lessonId)
                .map(last -> last.getSortOrder() + 1)
                .orElse(1);
        return saveNew(lessonId, sortOrder, form).getExamQuestionId();
    }

    /* API-ADM-30. 돌려주는 값은 redirect 할 수업 ID (요청에는 문항 ID 만 있다) */
    @Transactional
    public Long edit(Long examQuestionId, ExamQuestionForm form) {
        ExamQuestion question = getQuestion(examQuestionId);
        requireConceptOf(question.getLessonId(), form.getConceptActivityId());
        if (examAnswerRepository.existsByExamQuestion_ExamQuestionId(examQuestionId)) {
            boolean active = question.isActive();   // 사용 중지해 둔 문항을 고쳐도 새 문항이 다시 살아나지 않게
            question.changeActive(false);
            saveNew(question.getLessonId(), question.getSortOrder(), form).changeActive(active);
            return question.getLessonId();
        }
        question.edit(form.getConceptActivityId(), form.getContent(), form.getExplanation());

        // 응시 기록이 없으니 보기를 가리키는 답도 없다. 4개를 넘는 예전 보기는 지운다
        List<ExamChoice> choices = examChoiceRepository.findByExamQuestion_ExamQuestionIdOrderBySortOrderAsc(examQuestionId);
        if (choices.size() > ExamQuestionForm.CHOICE_COUNT) {
            examChoiceRepository.deleteAll(choices.subList(ExamQuestionForm.CHOICE_COUNT, choices.size()));
        }
        for (int i = 0; i < ExamQuestionForm.CHOICE_COUNT; i++) {
            String content = form.getChoices().get(i);
            boolean correct = i == form.getCorrectIndex();
            if (i < choices.size()) {
                choices.get(i).edit(content, correct);
            } else {
                examChoiceRepository.save(ExamChoice.create(question, content, correct, i + 1));
            }
        }
        return question.getLessonId();
    }

    /* API-ADM-31 */
    @Transactional
    public Long changeActive(Long examQuestionId, boolean active) {
        ExamQuestion question = getQuestion(examQuestionId);
        question.changeActive(active);
        return question.getLessonId();
    }

    /* 새 문항과 보기 4개를 사용 중으로 만든다 */
    private ExamQuestion saveNew(Long lessonId, int sortOrder, ExamQuestionForm form) {
        ExamQuestion question = examQuestionRepository.save(ExamQuestion.create(lessonId, form.getConceptActivityId(),
                form.getContent(), form.getExplanation(), sortOrder));
        for (int i = 0; i < ExamQuestionForm.CHOICE_COUNT; i++) {
            examChoiceRepository.save(ExamChoice.create(question, form.getChoices().get(i), i == form.getCorrectIndex(), i + 1));
        }
        return question;
    }

    /* 입력 오류로 A-06 에 돌려보낼 때 쓰는 문항의 수업 ID */
    public Long getLessonId(Long examQuestionId) {
        return getQuestion(examQuestionId).getLessonId();
    }

    private void requireConceptOf(Long lessonId, Long conceptActivityId) {
        if (conceptActivityId == null) {
            throw new BusinessException(ExamErrorCode.CONCEPT_REQUIRED);
        }
        if (!lessonReader.isConceptOf(lessonId, conceptActivityId)) {
            throw new BusinessException(ExamErrorCode.INVALID_INPUT);
        }
    }

    /* 명세의 실패는 CONCEPT_REQUIRED·INVALID_INPUT 뿐이라 없는 문항도 INVALID_INPUT 으로 본다 */
    private ExamQuestion getQuestion(Long examQuestionId) {
        return examQuestionRepository.findById(examQuestionId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.INVALID_INPUT));
    }
}
