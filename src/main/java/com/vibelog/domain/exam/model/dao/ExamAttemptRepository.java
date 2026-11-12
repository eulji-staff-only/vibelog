package com.vibelog.domain.exam.model.dao;

import com.vibelog.domain.exam.model.entity.ExamAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /*
     * 리포트 평균 점수(API-EXM-05 P2, #481): 그 수업 1회차 제출 응시의 [인원수, 점수 합계] 한 행. excludedIds(운영자·테스트 계정)는 뺀다 (PL-EXM-004·008).
     * 응시 엔티티를 모두 불러오지 않고 DB 에서 센다. excludedIds 는 비어 있으면 안 된다 (IN 이 빈 목록이면 오류) → 부르는 쪽이 비면 -1 하나를 넣는다
     */
    @Query("""
            select count(a), coalesce(sum(a.score), 0)
            from ExamAttempt a
            where a.lessonId = :lessonId and a.attemptNo = 1 and a.submittedAt is not null
              and a.memberId not in :excludedIds
            """)
    List<Object[]> countAndSumFirstScores(@Param("lessonId") Long lessonId, @Param("excludedIds") Collection<Long> excludedIds);

    /* 기간 내 제출한 1회차 응시 전체 (submitted_at 기준). from·to 가 null 이면 그 쪽 제한 없음 (admin 확장 대시보드, FR-ADM-003) */
    @Query("select a from ExamAttempt a where a.attemptNo = 1 and a.submittedAt is not null "
            + "and (:from is null or a.submittedAt >= :from) and (:to is null or a.submittedAt <= :to)")
    List<ExamAttempt> findFirstAttemptsSubmittedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /* 리포트(API-EXM-05): 회차를 고른 경우. 같은 응시를 동시에 처음 열어도 연 일시·계약 ③을 한 번만 처리하려고 행을 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExamAttempt> findByMemberIdAndLessonIdAndAttemptNo(Long memberId, Long lessonId, int attemptNo);

    /* 리포트(API-EXM-05): 회차가 없으면 최근 제출한 응시. 잠금 이유는 위와 같다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExamAttempt> findFirstByMemberIdAndLessonIdAndSubmittedAtIsNotNullOrderByAttemptNoDesc(Long memberId, Long lessonId);
}
