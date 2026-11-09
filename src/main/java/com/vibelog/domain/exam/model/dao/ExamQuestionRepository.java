package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
 * [ExamQuestionRepository]
 * 역할 : 모의고사 문항(ExamQuestion) 저장·조회.
 */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    /* 응시 화면·채점에 쓰는 사용 중인 문항, 순서대로 */
    List<ExamQuestion> findByLessonIdAndActiveTrueOrderBySortOrderAsc(Long lessonId);

    /* 운영자 수업 편집 화면(API-ADM-19, #100)의 모의고사 문항 목록 */
    List<ExamQuestion> findByLessonIdOrderBySortOrderAsc(Long lessonId);

    /* 새 문항의 순서 = 그 수업 마지막 문항 + 1 (API-ADM-29) */
    Optional<ExamQuestion> findTopByLessonIdOrderBySortOrderDesc(Long lessonId);
}
