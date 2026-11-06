package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/*
 * [ExamAttemptRepository]
 * 역할 : 모의고사 응시(ExamAttempt) 저장·조회.
 */
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /* 수업별 마지막 제출 회차. 회차는 1부터 늘어나므로 제출한 응시 중 가장 큰 attempt_no 가 마지막이다. [lessonId, attemptNo] */
    @Query("""
            select a.lessonId, max(a.attemptNo)
            from ExamAttempt a
            where a.memberId = :memberId and a.lessonId in :lessonIds and a.submittedAt is not null
            group by a.lessonId
            """)
    List<Object[]> findLatestSubmittedAttemptNos(@Param("memberId") Long memberId,
                                                 @Param("lessonIds") Collection<Long> lessonIds);
}
