package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamChoice;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * [ExamChoiceRepository]
 * 역할 : 모의고사 보기(ExamChoice) 저장·조회.
 */
public interface ExamChoiceRepository extends JpaRepository<ExamChoice, Long> {
}
