package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.model.dao.QuizAttemptRepository;
import com.vibelog.domain.quiz.model.dao.QuizQuestionRepository;
import com.vibelog.domain.quiz.model.dto.MemberQuizResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /* 회원이 제출한 퀴즈 결과, 최근 제출 순. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다. 제출 전 풀이는 뺀다 */
    public List<MemberQuizResult> getSubmittedResultsOf(Long memberId) {
        return quizAttemptRepository.findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(memberId).stream()
                .map(attempt -> new MemberQuizResult(attempt.getActivityId(), attempt.getCorrectRate(),
                        Boolean.TRUE.equals(attempt.getIsPassed()), attempt.getSubmittedAt()))
                .toList();
    }

    /*
     * 퀴즈 활동별 사용 중인 문항 수. 수업 공개 가능 여부 판단(API-ADM-33, FR-LSN-014, PL-LSN-003, #118)이 쓴다.
     * quiz_question Entity는 아직 등록·수정 기능(#102) 전이라 최소 컬럼만 매핑돼 있다.
     */
    public int countActiveQuestionsOf(Long activityId) {
        return quizQuestionRepository.countByActivityIdAndIsActiveTrue(activityId);
    }
}
