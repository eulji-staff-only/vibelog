package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * [ExamAnswerRepository]
 * 역할 : 모의고사 답(ExamAnswer) 저장·조회.
 */
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
}
