package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * [ExamQuestionRepository]
 * 역할 : 모의고사 문항(ExamQuestion) 저장·조회.
 */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    /* 응시 화면·채점에 쓰는 사용 중인 문항, 순서대로 */
    List<ExamQuestion> findByLessonIdAndActiveTrueOrderBySortOrderAsc(Long lessonId);
}
