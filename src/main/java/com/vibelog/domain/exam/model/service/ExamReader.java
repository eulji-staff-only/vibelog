package com.vibelog.domain.exam.model.service;

import com.vibelog.domain.exam.model.dao.ExamAttemptRepository;
import com.vibelog.domain.exam.model.dao.ExamChoiceRepository;
import com.vibelog.domain.exam.model.dao.ExamQuestionRepository;
import com.vibelog.domain.exam.model.dto.AdminExamQuestionItem;
import com.vibelog.domain.exam.model.dto.MemberExamResult;
import com.vibelog.domain.exam.model.entity.ExamQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * [ExamReader]
 * 역할 : 다른 영역이 exam 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamReader {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamChoiceRepository examChoiceRepository;

    /* 회원이 제출한 모의고사 결과, 최근 제출 순. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다. 제출 전 응시는 뺀다 */
    public List<MemberExamResult> getSubmittedResultsOf(Long memberId) {
        return examAttemptRepository.findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(memberId).stream()
                .map(attempt -> new MemberExamResult(attempt.getLessonId(), attempt.getAttemptNo(), attempt.getScore(),
                        attempt.getSubmittedAt()))
                .toList();
    }

    /*
     * 수업의 모의고사 문항 목록, 순서대로. 운영자 수업 편집 화면(API-ADM-19, #100)이 쓴다.
     * 문항 추가·수정·사용 중지(API-ADM-29~31)는 이 메서드가 아니라 모의고사 문항 관리 이슈(FR-EXM-007)에서 만든다.
     */
    public List<AdminExamQuestionItem> getQuestionsOf(Long lessonId) {
        return examQuestionRepository.findByLessonIdOrderBySortOrderAsc(lessonId).stream()
                .map(question -> new AdminExamQuestionItem(question.getExamQuestionId(), question.getContent(),
                        question.getExplanation(), question.getConceptActivityId(), question.getSortOrder(),
                        question.isActive(), choicesOf(question)))
                .toList();
    }

    private List<AdminExamQuestionItem.Choice> choicesOf(ExamQuestion question) {
        return examChoiceRepository.findByExamQuestion_ExamQuestionIdOrderBySortOrderAsc(question.getExamQuestionId()).stream()
                .map(choice -> new AdminExamQuestionItem.Choice(choice.getContent(), choice.isCorrect(), choice.getSortOrder()))
                .toList();
    }
}
