package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.MemberEnrollmentSummary;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * [LessonReader]
 * 역할 : 다른 영역이 lesson 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonReader {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    /*
     * 회원의 수강 목록과 진행률. 운영자 회원 상세(API-ADM-38, auth #119)가 쓴다.
     * 진행률 = 리포트까지 확인한 수업 수 ÷ 공개 수업 수, 저장하지 않고 계산한다 (PL-ENR-006). 시작한 순서대로.
     */
    // ponytail: 수강마다 쿼리 2번. 운영자 상세 화면 한 명분이라 그대로 둔다
    public List<MemberEnrollmentSummary> getEnrollmentsOf(Long memberId) {
        return enrollmentRepository.findByMemberIdOrderByStartedAtAsc(memberId).stream()
                .map(this::summaryOf)
                .toList();
    }

    /* 수업 ID → 수업 제목. 없는 ID 는 결과에 빠진다. 운영자 회원 상세의 모의고사 결과(auth #119)가 쓴다 */
    public Map<Long, String> getLessonTitles(Collection<Long> lessonIds) {
        return lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getLessonId, Lesson::getTitle));
    }

    private MemberEnrollmentSummary summaryOf(Enrollment enrollment) {
        List<Long> lessonIds = lessonRepository.findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(enrollment.getCourseId()).stream()
                .map(Lesson::getLessonId)
                .toList();
        long done = lessonIds.isEmpty() ? 0
                : lessonProgressRepository.countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(enrollment.getEnrollmentId(), lessonIds);
        int percent = lessonIds.isEmpty() ? 0 : (int) Math.round(done * 100.0 / lessonIds.size());
        return new MemberEnrollmentSummary(enrollment.getCourseId(), enrollment.getStartedAt(), percent,
                enrollment.isCompleted(), enrollment.isDropped());
    }
}
