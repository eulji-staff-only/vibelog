package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * [ExamQuestionRepository]
 * 역할 : 모의고사 문항(ExamQuestion) 저장·조회.
 */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
}
