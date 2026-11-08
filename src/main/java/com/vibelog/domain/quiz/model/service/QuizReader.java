package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.model.dao.QuizAttemptRepository;
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

    /* 회원이 제출한 퀴즈 결과, 최근 제출 순. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다. 제출 전 풀이는 뺀다 */
    public List<MemberQuizResult> getSubmittedResultsOf(Long memberId) {
        return quizAttemptRepository.findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(memberId).stream()
                .map(attempt -> new MemberQuizResult(attempt.getActivityId(), attempt.getCorrectRate(),
                        Boolean.TRUE.equals(attempt.getIsPassed()), attempt.getSubmittedAt()))
                .toList();
    }
}
