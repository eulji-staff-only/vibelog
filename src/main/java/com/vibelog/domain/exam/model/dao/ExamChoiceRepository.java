package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * [ExamChoiceRepository]
 * 역할 : 모의고사 보기(ExamChoice) 저장·조회.
 */
public interface ExamChoiceRepository extends JpaRepository<ExamChoice, Long> {

    /* 운영자 수업 편집 화면(API-ADM-19, #100)의 문항별 보기 목록 */
    List<ExamChoice> findByExamQuestion_ExamQuestionIdOrderBySortOrderAsc(Long examQuestionId);
}
