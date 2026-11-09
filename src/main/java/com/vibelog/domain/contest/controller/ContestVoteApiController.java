package com.vibelog.domain.contest.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.contest.model.dto.VoteRequest;
import com.vibelog.domain.contest.model.dto.VoteResponse;
import com.vibelog.domain.contest.model.service.ContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * [ContestVoteApiController]
 * 역할 : 출품작 투표 (API-CTS-03, FR-CTS-003, #201). U-32 투표 보드가 페이지 이동 없이 부른다.
 * 책임 : 성공은 {"contestEntryId", "votedToday": true}, 실패는 GlobalExceptionHandler 가 {code, message} 로 답한다.
 */
@RestController
@RequiredArgsConstructor
public class ContestVoteApiController {

    private final ContestService contestService;

    @PostMapping("/api/contests/{contestId}/votes")
    public VoteResponse vote(@PathVariable Long contestId, @Valid @RequestBody VoteRequest request,
                             @AuthenticationPrincipal LoginMember loginMember) {
        contestService.vote(contestId, loginMember.getMemberId(), request.contestEntryId());
        return new VoteResponse(request.contestEntryId(), true);
    }
}
