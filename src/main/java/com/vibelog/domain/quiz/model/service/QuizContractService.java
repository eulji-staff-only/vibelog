package com.vibelog.domain.quiz.model.service;

import com.vibelog.domain.quiz.model.dao.QuizAttemptRepository;
import com.vibelog.domain.quiz.model.entity.QuizAttempt;
import com.vibelog.domain.quiz.model.policy.LessonQuizScoreChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/*
 * [QuizContractService]
 * 역할 : quiz 영역이 다른 영역에 주는 계약의 진짜 구현. 지금은 ⑤(LessonQuizScoreChecker)만 있다.
 * 책임 : - 다른 영역은 이 클래스가 아니라 model/policy 의 계약 인터페이스를 주입해 쓴다.
 *        - docs/design/api/quiz.md "영역 간 계약" 표 그대로 구현한다. 이 구현이 생겨 FakeLessonQuizScoreChecker 는 지웠다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizContractService implements LessonQuizScoreChecker {

    private final QuizAttemptRepository quizAttemptRepository;

    /* ⑤ 제출한 풀이 중 가장 높은 correct_rate. 제출 전 풀이는 빼고, 제출한 풀이가 없으면 null (API-LSN-01, PL-LSN-007) */
    @Override
    public BigDecimal getBestCorrectRate(Long memberId, Long activityId) {
        return quizAttemptRepository
                .findTopByMemberIdAndActivityIdAndSubmittedAtIsNotNullOrderByCorrectRateDesc(memberId, activityId)
                .map(QuizAttempt::getCorrectRate)
                .orElse(null);
    }
}
