package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * [ExamQuestionRepository]
 * 역할 : 모의고사 문항(ExamQuestion) 저장·조회.
 */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    /* 운영자 수업 편집 화면(API-ADM-19, #100)의 모의고사 문항 목록 */
    List<ExamQuestion> findByLessonIdOrderBySortOrderAsc(Long lessonId);
}
