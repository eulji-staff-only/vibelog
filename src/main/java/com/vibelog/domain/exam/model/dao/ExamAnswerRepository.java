package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamAnswer;
import com.vibelog.domain.exam.model.entity.ExamAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/*
 * [ExamAnswerRepository]
 * 역할 : 모의고사 답(ExamAnswer) 저장·조회.
 * 책임 : 답 목록은 문항·고른 보기(LAZY)를 함께 읽는다(@EntityGraph). 리포트·대시보드가 답마다 문항·보기를 꺼내므로,
 *        따로 읽으면 문항 수만큼 쿼리가 더 나간다 (N+1 방지, #476). 보기는 고르지 않았으면 비어 있어 left join 으로 읽힌다.
 */
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
    /* 응시 1회의 답 전체 (이어 풀기·채점·오답 리포트) */
    @EntityGraph(attributePaths = {"examQuestion", "examChoice"})
    List<ExamAnswer> findByExamAttempt(ExamAttempt examAttempt);

    /* 여러 응시의 답 전체 (admin 확장 대시보드의 수업별 약한 개념, FR-ADM-003) */
    @EntityGraph(attributePaths = {"examQuestion", "examChoice"})
    List<ExamAnswer> findByExamAttemptIn(Collection<ExamAttempt> examAttempts);

    /* 이 문항에 응시 기록이 있나. 있으면 문항을 고치지 않고 사용 중지한 뒤 새 문항을 만든다 (DB 설계서 5-4, API-ADM-30) */
    boolean existsByExamQuestion_ExamQuestionId(Long examQuestionId);
}
