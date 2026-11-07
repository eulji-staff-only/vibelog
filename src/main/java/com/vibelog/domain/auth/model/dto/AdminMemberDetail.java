package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberRole;
import com.vibelog.domain.auth.model.entity.MemberStatus;

import java.time.LocalDateTime;
import java.util.List;

/*
 * 운영자 회원 상세 (API-ADM-38). 수강·모의고사·포트폴리오는 각 영역 Reader 에서 받은 값이다.
 * 코스 이름과 퀴즈 결과는 course·quiz 영역이 아직 없어 담지 않는다 (#119 후속).
 */
public record AdminMemberDetail(Long memberId, String nickname, String email, String roleLabel, boolean withdrawn,
                                boolean emailVerified, LocalDateTime joinedAt, List<Enrollment> enrollments,
                                List<ExamResult> examResults, long portfolioCount) {

    /* 수강 코스 하나. status 는 "수강 중"·"완료"·"포기" */
    public record Enrollment(Long courseId, LocalDateTime startedAt, int progressPercent, String status) {
    }

    /* 제출한 모의고사 한 번 */
    public record ExamResult(String lessonTitle, int attemptNo, int score, LocalDateTime submittedAt) {
    }

    static String roleLabel(Member member) {
        return member.getRole() == MemberRole.ADMIN ? "운영자" : "학습자";
    }

    public static AdminMemberDetail of(Member member, List<Enrollment> enrollments, List<ExamResult> examResults, long portfolioCount) {
        return new AdminMemberDetail(member.getMemberId(), member.getNickname(), member.getEmail(), roleLabel(member),
                member.getStatus() == MemberStatus.WITHDRAWN, member.getEmailVerifiedAt() != null, member.getCreatedAt(),
                enrollments, examResults, portfolioCount);
    }
}
