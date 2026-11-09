package com.vibelog.domain.admin.model.service;

import com.vibelog.domain.admin.exception.AdminErrorCode;
import com.vibelog.domain.admin.model.dto.AdminDashboardResponse;
import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.message.model.service.MessageReader;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/*
 * 확장 대시보드(API-ADM-40, FR-ADM-003, #194 1차): admin 은 직접 세지 않고 각 영역 Reader 값을 기간과 함께 받아 합친다.
 * 실제 건수 계산(삭제·취소 제외, 기간 필터)은 DashboardReaderCountTest 가 DB 로 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock MemberReader memberReader;
    @Mock LessonReader lessonReader;
    @Mock CourseReader courseReader;
    @Mock PortfolioReader portfolioReader;
    @Mock CommunityReader communityReader;
    @Mock MessageReader messageReader;
    @Mock ContestReader contestReader;
    @InjectMocks AdminDashboardService service;

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);
    private static final LocalDateTime FROM_TIME = FROM.atStartOfDay();
    private static final LocalDateTime TO_TIME = TO.atTime(LocalTime.MAX);

    @Test
    void 각_영역_Reader에_같은_기간을_넘겨_지표를_합친다() {
        given(memberReader.countJoinedBetween(FROM_TIME, TO_TIME)).willReturn(7L);
        given(lessonReader.getEnrollmentStatsByCourse(FROM_TIME, TO_TIME)).willReturn(List.of());
        given(courseReader.countDiagnosisResultsByStatus(FROM_TIME, TO_TIME)).willReturn(Map.of("COMPLETED", 3, "SKIPPED", 1));
        given(portfolioReader.countSubmitted(FROM_TIME, TO_TIME)).willReturn(4L);
        given(communityReader.countPosts(FROM_TIME, TO_TIME)).willReturn(5L);
        given(communityReader.countPeerReviews(FROM_TIME, TO_TIME)).willReturn(6L);
        given(messageReader.countMessages(FROM_TIME, TO_TIME)).willReturn(8L);
        given(messageReader.countReports(FROM_TIME, TO_TIME)).willReturn(9L);
        given(contestReader.countEntries(FROM_TIME, TO_TIME)).willReturn(10L);

        AdminDashboardResponse response = service.getDashboard(FROM, TO);

        assertThat(response.memberCount()).isEqualTo(7);
        assertThat(response.diagnosis().completedCount()).isEqualTo(3);
        assertThat(response.diagnosis().skippedCount()).isEqualTo(1);
        assertThat(response.diagnosis().completedRate()).isEqualTo(75);
        assertThat(response.diagnosis().skippedRate()).isEqualTo(25);
        assertThat(response.activity().portfolioCount()).isEqualTo(4);
        assertThat(response.activity().postCount()).isEqualTo(5);
        assertThat(response.activity().peerReviewCount()).isEqualTo(6);
        assertThat(response.activity().messageCount()).isEqualTo(8);
        assertThat(response.activity().reportCount()).isEqualTo(9);
        assertThat(response.activity().contestEntryCount()).isEqualTo(10);
    }

    @Test
    void 기간을_비우면_null_그대로_넘기고_진단_결과가_없으면_비율은_0이다() {
        given(courseReader.countDiagnosisResultsByStatus(null, null)).willReturn(Map.of("COMPLETED", 0, "SKIPPED", 0));
        given(lessonReader.getEnrollmentStatsByCourse(null, null)).willReturn(List.of());

        AdminDashboardResponse response = service.getDashboard(null, null);

        assertThat(response.diagnosis().completedRate()).isZero();
        assertThat(response.diagnosis().skippedRate()).isZero();
        verify(contestReader).countEntries(null, null);
        verify(messageReader).countReports(null, null);
    }

    @Test
    void 시작일이_종료일보다_늦으면_Reader를_부르지_않고_잘못된_요청이다() {
        assertThatThrownBy(() -> service.getDashboard(TO, FROM))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AdminErrorCode.INVALID_INPUT);
        verifyNoInteractions(courseReader, portfolioReader, communityReader, messageReader, contestReader);
    }
}
