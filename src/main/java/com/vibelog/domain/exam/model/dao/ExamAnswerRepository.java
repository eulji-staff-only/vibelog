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
}
