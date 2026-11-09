package com.vibelog.domain.admin.model.service;

import com.vibelog.domain.admin.exception.AdminErrorCode;
import com.vibelog.domain.admin.model.dto.AdminActivityCounts;
import com.vibelog.domain.admin.model.dto.AdminCourseDashboardItem;
import com.vibelog.domain.admin.model.dto.AdminDashboardResponse;
import com.vibelog.domain.admin.model.dto.AdminDiagnosisStat;
import com.vibelog.domain.admin.model.dto.AdminTrackDashboardItem;
import com.vibelog.domain.admin.model.policy.CompletionRatePolicy;
import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.model.dto.CourseEnrollmentStat;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.message.model.service.MessageReader;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * [AdminDashboardService]
 * 역할 : 대시보드(A-01, API-ADM-01·40, FR-ADM-002·003)의 가입자 수, 트랙·코스별 수강 수·완료율, 진단 완료·건너뛰기 비율,
 *        활동 건수(포트폴리오·게시글·리뷰·쪽지·신고·콘테스트 참여)를 모은다.
 * 책임 : - 가입자 수는 MemberReader, 수강 집계는 LessonReader, 트랙·코스 이름과 진단 결과는 CourseReader,
 *          활동 건수는 Portfolio·Community·Message·ContestReader 에서 읽기만 한다
 *          (다른 영역의 Entity·Repository 는 import 하지 않는다. 직접 계산하지 않고 기간만 넘긴다).
 *        - 시작일이 종료일보다 늦으면 AdminErrorCode.INVALID_INPUT 을 던진다.
 *        - 기간에 시작한 수강이 없는 트랙·코스는 결과에 포함하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final MemberReader memberReader;
    private final LessonReader lessonReader;
    private final CourseReader courseReader;
    private final PortfolioReader portfolioReader;
    private final CommunityReader communityReader;
    private final MessageReader messageReader;
    private final ContestReader contestReader;

    public AdminDashboardResponse getDashboard(LocalDate from, LocalDate to) {
        validatePeriod(from, to);

        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);

        long memberCount = memberReader.countJoinedBetween(fromDateTime, toDateTime);
        List<CourseEnrollmentStat> courseStats = lessonReader.getEnrollmentStatsByCourse(fromDateTime, toDateTime);

        return new AdminDashboardResponse(memberCount, toTrackItems(courseStats),
                toDiagnosisStat(fromDateTime, toDateTime), toActivityCounts(fromDateTime, toDateTime));
    }

    private AdminDiagnosisStat toDiagnosisStat(LocalDateTime from, LocalDateTime to) {
        Map<String, Integer> counts = courseReader.countDiagnosisResultsByStatus(from, to);
        return AdminDiagnosisStat.of(counts.getOrDefault("COMPLETED", 0), counts.getOrDefault("SKIPPED", 0));
    }

    private AdminActivityCounts toActivityCounts(LocalDateTime from, LocalDateTime to) {
        return new AdminActivityCounts(portfolioReader.countSubmitted(from, to), communityReader.countPosts(from, to),
                communityReader.countPeerReviews(from, to), messageReader.countMessages(from, to),
                messageReader.countReports(from, to), contestReader.countEntries(from, to));
    }

    private List<AdminTrackDashboardItem> toTrackItems(List<CourseEnrollmentStat> courseStats) {
        if (courseStats.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courseStats.stream().map(CourseEnrollmentStat::courseId).toList();
        Map<Long, String> courseTitles = courseReader.getCourseTitles(courseIds);
        Map<Long, Long> trackIdsByCourse = courseReader.getTrackIdsByCourse(courseIds);
        Map<Long, String> trackTitles = courseReader.getTrackTitles(trackIdsByCourse.values());

        Map<Long, List<CourseEnrollmentStat>> statsByTrack = courseStats.stream()
                .filter(stat -> trackIdsByCourse.get(stat.courseId()) != null)
                .collect(Collectors.groupingBy(stat -> trackIdsByCourse.get(stat.courseId())));

        return statsByTrack.entrySet().stream()
                .map(entry -> toTrackItem(entry.getKey(), entry.getValue(), trackTitles, courseTitles))
                .sorted(Comparator.comparing(AdminTrackDashboardItem::trackName))
                .toList();
    }

    private AdminTrackDashboardItem toTrackItem(Long trackId, List<CourseEnrollmentStat> stats,
                                                 Map<Long, String> trackTitles, Map<Long, String> courseTitles) {
        List<AdminCourseDashboardItem> courses = stats.stream()
                .map(stat -> new AdminCourseDashboardItem(stat.courseId(), courseTitles.getOrDefault(stat.courseId(), ""),
                        stat.startedCount(), CompletionRatePolicy.calculate(stat.startedCount(), stat.completedCount())))
                .sorted(Comparator.comparing(AdminCourseDashboardItem::courseTitle))
                .toList();

        long trackStarted = stats.stream().mapToLong(CourseEnrollmentStat::startedCount).sum();
        long trackCompleted = stats.stream().mapToLong(CourseEnrollmentStat::completedCount).sum();

        return new AdminTrackDashboardItem(trackId, trackTitles.getOrDefault(trackId, ""), trackStarted,
                CompletionRatePolicy.calculate(trackStarted, trackCompleted), courses);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(AdminErrorCode.INVALID_INPUT);
        }
    }
}
