package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /* 회원이 제출한 응시, 최근 제출 순 (ExamReader.getSubmittedResultsOf) */
    List<ExamAttempt> findByMemberIdAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(Long memberId);

    /* 그 수업의 가장 최근 회차. 제출 안 한 응시가 있다면 늘 이것이다 (앞 회차를 제출해야 새 회차를 만든다) */
    Optional<ExamAttempt> findFirstByMemberIdAndLessonIdOrderByAttemptNoDesc(Long memberId, Long lessonId);

    /* 리포트 평균 점수(API-EXM-05 P2): 그 수업의 회차별 제출 응시. 평균은 1회차만 쓴다 (PL-EXM-004) */
    List<ExamAttempt> findByLessonIdAndAttemptNoAndSubmittedAtIsNotNull(Long lessonId, int attemptNo);

    /* 리포트(API-EXM-05): 회차를 고른 경우. 같은 응시를 동시에 처음 열어도 연 일시·계약 ③을 한 번만 처리하려고 행을 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExamAttempt> findByMemberIdAndLessonIdAndAttemptNo(Long memberId, Long lessonId, int attemptNo);

    /* 리포트(API-EXM-05): 회차가 없으면 최근 제출한 응시. 잠금 이유는 위와 같다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExamAttempt> findFirstByMemberIdAndLessonIdAndSubmittedAtIsNotNullOrderByAttemptNoDesc(Long memberId, Long lessonId);
}
