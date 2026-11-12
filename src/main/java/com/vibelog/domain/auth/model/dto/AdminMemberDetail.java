package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.message.model.dto.ReportHistory;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberRole;
import com.vibelog.domain.auth.model.entity.MemberStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/*
 * 운영자 회원 상세 (API-ADM-38). 수강·퀴즈·모의고사·포트폴리오·신고 이력은 각 영역 Reader 에서 받은 값이다.
 */
public record AdminMemberDetail(Long memberId, String nickname, String email, String roleLabel, boolean withdrawn,
                                boolean emailVerified, LocalDateTime joinedAt, List<Enrollment> enrollments,
                                List<QuizResult> quizResults, List<ExamResult> examResults, long portfolioCount,
                                AdminSanction sanction, ReportHistory reportHistory) {

    /* 수강 코스 하나. status 는 "수강 중"·"완료"·"포기" */
    public record Enrollment(String courseTitle, LocalDateTime startedAt, int progressPercent, String status) {
    }

    /* 제출한 퀴즈 풀이 한 번. correctRate 는 정답률(%) */
    public record QuizResult(String activityTitle, BigDecimal correctRate, boolean passed, LocalDateTime submittedAt) {
    }

    /* 제출한 모의고사 한 번 */
    public record ExamResult(String lessonTitle, int attemptNo, int score, LocalDateTime submittedAt) {
    }

    static String roleLabel(Member member) {
        return member.getRole() == MemberRole.ADMIN ? "운영자" : "학습자";
    }

    public static AdminMemberDetail of(Member member, List<Enrollment> enrollments, List<QuizResult> quizResults,
                                       List<ExamResult> examResults, long portfolioCount, AdminSanction sanction,
                                       ReportHistory reportHistory) {
        return new AdminMemberDetail(member.getMemberId(), member.getNickname(), member.getEmail(), roleLabel(member),
                member.getStatus() == MemberStatus.WITHDRAWN, member.getEmailVerifiedAt() != null, member.getCreatedAt(),
                enrollments, quizResults, examResults, portfolioCount, sanction, reportHistory);
    }
}
