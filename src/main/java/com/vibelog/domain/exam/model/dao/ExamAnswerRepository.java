package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamAnswer;
import com.vibelog.domain.exam.model.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * [ExamAnswerRepository]
 * 역할 : 모의고사 답(ExamAnswer) 저장·조회.
 */
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
    /* 응시 1회의 답 전체 (이어 풀기·채점) */
    List<ExamAnswer> findByExamAttempt(ExamAttempt examAttempt);

    /* 이 문항에 응시 기록이 있나. 있으면 문항을 고치지 않고 사용 중지한 뒤 새 문항을 만든다 (DB 설계서 5-4, API-ADM-30) */
    boolean existsByExamQuestion_ExamQuestionId(Long examQuestionId);
}
