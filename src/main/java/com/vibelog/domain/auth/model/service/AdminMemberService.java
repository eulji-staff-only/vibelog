package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dao.MemberSuspensionRepository;
import com.vibelog.domain.auth.model.dto.AdminMemberDetail;
import com.vibelog.domain.auth.model.dto.AdminMemberItem;
import com.vibelog.domain.auth.model.dto.AdminSanction;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberRole;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import com.vibelog.domain.auth.model.entity.MemberSuspension;
import com.vibelog.domain.auth.model.entity.SuspensionLevel;
import com.vibelog.domain.auth.model.policy.SuspensionPolicy;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.exam.model.dto.MemberExamResult;
import com.vibelog.domain.exam.model.service.ExamReader;
import com.vibelog.domain.lesson.model.dto.MemberEnrollmentSummary;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.domain.quiz.model.dto.MemberQuizResult;
import com.vibelog.domain.quiz.model.service.QuizReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * [AdminMemberService]
 * 역할 : 운영자의 회원 조회 (FR-USR-009, API-ADM-37·38). 운영자만 부른다 (SecurityConfig /admin/**).
 * 책임 : - 목록은 닉네임·이메일 일부로 찾고 20명씩 보여 준다. 탈퇴 회원은 목록에서 뺀다 (상세는 ID 로 열 수 있다).
 *        - 상세의 수강·진행률은 LessonReader, 코스 이름은 CourseReader, 퀴즈 결과는 QuizReader, 모의고사 결과는 ExamReader,
 *          포트폴리오 수는 PortfolioReader 로 받는다 (08 문서 규칙 1). 퀴즈·수업 제목은 LessonReader 로 받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    public static final int PAGE_SIZE = 20;

    private final MemberRepository memberRepository;
    private final MemberSuspensionRepository memberSuspensionRepository;
    private final SuspensionPolicy suspensionPolicy;
    private final LessonReader lessonReader;
    private final CourseReader courseReader;
    private final QuizReader quizReader;
    private final ExamReader examReader;
    private final PortfolioReader portfolioReader;

    /* page 는 화면 그대로 1부터 센다 */
    public Page<AdminMemberItem> search(String q, int page) {
        String keyword = q == null || q.isBlank() ? null : escapeLike(q.strip());
        return memberRepository.searchActive(keyword, PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE))
                .map(AdminMemberItem::from);
    }

    public AdminMemberDetail getDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        List<MemberEnrollmentSummary> summaries = lessonReader.getEnrollmentsOf(memberId);
        Map<Long, String> courseTitles = courseReader.getCourseTitles(summaries.stream().map(MemberEnrollmentSummary::courseId).toList());
        List<AdminMemberDetail.Enrollment> enrollments = summaries.stream()
                .map(e -> new AdminMemberDetail.Enrollment(courseTitles.getOrDefault(e.courseId(), "코스 " + e.courseId()),
                        e.startedAt(), e.progressPercent(), e.dropped() ? "포기" : e.completed() ? "완료" : "수강 중"))
                .toList();
        List<MemberQuizResult> quizzes = quizReader.getSubmittedResultsOf(memberId);
        Map<Long, String> activityTitles = lessonReader.getActivityTitles(quizzes.stream().map(MemberQuizResult::activityId).toList());
        List<AdminMemberDetail.QuizResult> quizResults = quizzes.stream()
                .map(q -> new AdminMemberDetail.QuizResult(activityTitles.getOrDefault(q.activityId(), "퀴즈 " + q.activityId()),
                        q.correctRate(), q.passed(), q.submittedAt()))
                .toList();
        List<MemberExamResult> exams = examReader.getSubmittedResultsOf(memberId);
        Map<Long, String> lessonTitles = lessonReader.getLessonTitles(exams.stream().map(MemberExamResult::lessonId).toList());
        List<AdminMemberDetail.ExamResult> examResults = exams.stream()
                .map(r -> new AdminMemberDetail.ExamResult(lessonTitles.getOrDefault(r.lessonId(), "수업 " + r.lessonId()),
                        r.attemptNo(), r.score(), r.submittedAt()))
                .toList();
        MemberSuspension current = suspensionPolicy.findActive(memberId).orElse(null);
        boolean suspendable = member.getStatus() == MemberStatus.ACTIVE && member.getRole() != MemberRole.ADMIN && current == null;
        AdminSanction sanction = AdminSanction.of(current, suspendable,
                memberSuspensionRepository.findByMemberIdOrderByStartsAtDescMemberSuspensionIdDesc(memberId));
        return AdminMemberDetail.of(member, enrollments, quizResults, examResults, portfolioReader.countOf(memberId), sanction);
    }

    /* API-USR-19. 이용 중인 학습자만. 이미 정지 중이면 막는다 (해제한 뒤 다시) */
    @Transactional
    public void suspend(Long adminMemberId, Long memberId, SuspensionLevel level, String reason) {
        Member member = memberRepository.findById(memberId)
                .filter(found -> found.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        if (member.getRole() == MemberRole.ADMIN) {
            throw new BusinessException(AuthErrorCode.CANNOT_SUSPEND_ADMIN);
        }
        if (suspensionPolicy.isSuspended(memberId)) {
            throw new BusinessException(AuthErrorCode.ALREADY_SUSPENDED);
        }
        memberSuspensionRepository.save(MemberSuspension.create(memberId, adminMemberId, level, reason, LocalDateTime.now()));
    }

    /* API-USR-20. 지금 정지 중인 제재를 푼다. 변경 감지로 저장된다 */
    @Transactional
    public void release(Long adminMemberId, Long memberId) {
        suspensionPolicy.findActive(memberId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.NOT_SUSPENDED))
                .release(adminMemberId, LocalDateTime.now());
    }

    /* LIKE 검색어의 ! % _ 를 글자 그대로 찾도록 이스케이프한다 (MemberRepository 의 escape '!'). MemberReader 쪽지 받는 사람 찾기도 쓴다 */
    static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
