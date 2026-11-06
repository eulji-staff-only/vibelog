package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.course.model.policy.CourseReader;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/*
 * CourseReader(계약)·EnrollmentRepository 를 모킹해 수강 시작의 분기 규칙만 빠르게 확인한다 (DB 없이 도는 단위 테스트).
 * 공개 수업별 진도 생성 흐름은 EnrollmentServiceTest(실제 DB)에서 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceUnitTest {

    @Mock CourseReader courseReader;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock LessonRepository lessonRepository;
    @Mock LessonProgressRepository lessonProgressRepository;
    @Mock ActivityRepository activityRepository;
    @Mock ActivityProgressRepository activityProgressRepository;

    private EnrollmentService enrollmentService() {
        return new EnrollmentService(courseReader, enrollmentRepository, lessonRepository,
                lessonProgressRepository, activityRepository, activityProgressRepository);
    }

    @Test
    void 코스가_없거나_비공개면_COURSE_NOT_FOUND를_던지고_수강을_만들지_않는다() {
        given(courseReader.isPublicCourse(1L)).willReturn(false);

        assertThatThrownBy(() -> enrollmentService().enroll(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(LessonErrorCode.COURSE_NOT_FOUND);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void 이미_수강_중이면_그대로_두고_새로_만들지_않는다() {
        given(courseReader.isPublicCourse(1L)).willReturn(true);
        given(enrollmentRepository.findByMemberIdAndCourseId(1L, 1L))
                .willReturn(Optional.of(mock(Enrollment.class)));

        enrollmentService().enroll(1L, 1L);

        verify(enrollmentRepository, never()).save(any());
        verify(lessonRepository, never()).findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(any());
    }
}
