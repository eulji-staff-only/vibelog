package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dto.AdminMemberDetail;
import com.vibelog.domain.auth.model.dto.AdminMemberItem;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.exam.model.dto.MemberExamResult;
import com.vibelog.domain.exam.model.service.ExamReader;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/*
 * [AdminMemberService]
 * 역할 : 운영자의 회원 조회 (FR-USR-009, API-ADM-37·38). 운영자만 부른다 (SecurityConfig /admin/**).
 * 책임 : - 목록은 닉네임·이메일 일부로 찾고 20명씩 보여 준다. 탈퇴 회원은 목록에서 뺀다 (상세는 ID 로 열 수 있다).
 *        - 상세의 수강·진행률은 LessonReader, 모의고사 결과는 ExamReader, 포트폴리오 수는 PortfolioReader 로 받는다 (08 문서 규칙 1).
 *        - 코스 이름과 퀴즈 결과는 course·quiz 영역 코드가 아직 없어 이번에는 보여 주지 않는다 (#119 남은 일).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    public static final int PAGE_SIZE = 20;

    private final MemberRepository memberRepository;
    private final LessonReader lessonReader;
    private final ExamReader examReader;
    private final PortfolioReader portfolioReader;

    /* page 는 화면 그대로 1부터 센다 */
    public Page<AdminMemberItem> search(String q, int page) {
        String keyword = q == null || q.isBlank() ? null : q.strip();
        return memberRepository.searchActive(keyword, PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE))
                .map(AdminMemberItem::from);
    }

    public AdminMemberDetail getDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        List<AdminMemberDetail.Enrollment> enrollments = lessonReader.getEnrollmentsOf(memberId).stream()
                .map(e -> new AdminMemberDetail.Enrollment(e.courseId(), e.startedAt(), e.progressPercent(),
                        e.dropped() ? "포기" : e.completed() ? "완료" : "수강 중"))
                .toList();
        List<MemberExamResult> exams = examReader.getSubmittedResultsOf(memberId);
        Map<Long, String> lessonTitles = lessonReader.getLessonTitles(exams.stream().map(MemberExamResult::lessonId).toList());
        List<AdminMemberDetail.ExamResult> examResults = exams.stream()
                .map(r -> new AdminMemberDetail.ExamResult(lessonTitles.getOrDefault(r.lessonId(), "수업 " + r.lessonId()),
                        r.attemptNo(), r.score(), r.submittedAt()))
                .toList();
        return AdminMemberDetail.of(member, enrollments, examResults, portfolioReader.countOf(memberId));
    }
}
