package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.model.dao.QuizAttemptRepository;
import com.vibelog.domain.quiz.model.dao.QuizChoiceRepository;
import com.vibelog.domain.quiz.model.dao.QuizQuestionRepository;
import com.vibelog.domain.quiz.model.dto.AdminQuizQuestionItem;
import com.vibelog.domain.quiz.model.dto.MemberQuizResult;
import com.vibelog.domain.quiz.model.entity.QuizChoice;
import com.vibelog.domain.quiz.model.entity.QuizQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * [QuizReader]
 * 역할 : 다른 영역이 quiz 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 *        - quiz 기반(#84)보다 먼저 운영자 회원 상세(#119)가 만들었다. #84 는 이 클래스에 이어서 추가한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizReader {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;

    /* 회원이 제출한 퀴즈 결과, 최근 제출 순. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다. 제출 전 풀이는 뺀다 */
    public List<MemberQuizResult> getSubmittedResultsOf(Long memberId) {
        return quizAttemptRepository.findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(memberId).stream()
                .map(attempt -> new MemberQuizResult(attempt.getActivityId(), attempt.getCorrectRate(),
                        Boolean.TRUE.equals(attempt.getIsPassed()), attempt.getSubmittedAt()))
                .toList();
    }

    /*
     * 퀴즈 활동별 사용 중인 문항 수. 수업 공개 가능 여부 판단(API-ADM-33, FR-LSN-014, PL-LSN-003, #118)이 쓴다.
     */
    public int countActiveQuestionsOf(Long activityId) {
        return quizQuestionRepository.countByActivityIdAndIsActiveTrue(activityId);
    }

    /*
     * 여러 퀴즈 활동의 문항 전부(사용 중지 포함)와 보기, 활동·순서대로. 운영자 수업 편집(A-06, API-ADM-19)의
     * 퀴즈 문항 목록(quiz #102)이 쓴다. 빈 묶음이면 빈 목록
     */
    public List<AdminQuizQuestionItem> getAdminQuestionsOf(Collection<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return List.of();
        }
        List<QuizQuestion> questions = quizQuestionRepository.findByActivityIdInOrderBySortOrderAscQuizQuestionIdAsc(activityIds);
        Map<Long, List<QuizChoice>> choicesByQuestion = quizChoiceRepository.findByQuizQuestionIdInOrderBySortOrderAsc(
                        questions.stream().map(QuizQuestion::getQuizQuestionId).toList()).stream()
                .collect(Collectors.groupingBy(QuizChoice::getQuizQuestionId));
        return questions.stream()
                .map(question -> new AdminQuizQuestionItem(question.getQuizQuestionId(), question.getActivityId(),
                        question.getDisplayType(), question.getContent(), question.getCodeSnippet(), question.getExplanation(),
                        question.getConceptTagId(), question.getSortOrder(), question.isActive(),
                        choicesByQuestion.getOrDefault(question.getQuizQuestionId(), List.of()).stream()
                                .map(choice -> new AdminQuizQuestionItem.Choice(choice.getContent(), choice.isCorrect(), choice.getSortOrder()))
                                .toList()))
                .toList();
    }
}
