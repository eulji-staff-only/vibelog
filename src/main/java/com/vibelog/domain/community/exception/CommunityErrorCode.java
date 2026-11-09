package com.vibelog.domain.community.exception;

import com.vibelog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/*
 * [CommunityErrorCode]
 * 역할 : 커뮤니티(community) 영역의 비즈니스 예외 종류.
 * 책임 : 코드·상태·메시지는 API 명세 _template.md 7-3 그대로 둔다. 쓰는 곳이 생길 때 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    SHOWCASE_LINK_REQUIRED(HttpStatus.BAD_REQUEST, "작품 공유 글은 작품 링크가 필요합니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "답글에는 답글을 달 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    SELF_FOLLOW(HttpStatus.BAD_REQUEST, "나는 팔로우할 수 없습니다."),
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    PRIVATE_PORTFOLIO(HttpStatus.FORBIDDEN, "비공개된 작품입니다."),
    REVIEW_REQUEST_OPEN(HttpStatus.CONFLICT, "이 작품은 이미 리뷰를 요청했습니다."),
    REVIEW_REQUEST_LIMIT(HttpStatus.CONFLICT, "리뷰 요청은 동시에 3개까지 할 수 있습니다. 지난 요청을 마감한 뒤 다시 요청해 주세요."),
    REVIEW_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰 요청을 찾을 수 없습니다."),
    REVIEW_REQUEST_CLOSED(HttpStatus.CONFLICT, "마감된 리뷰 요청입니다."),
    SELF_REVIEW(HttpStatus.BAD_REQUEST, "내 작품에는 리뷰를 쓸 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 리뷰를 남겼습니다. 내 리뷰를 수정해 주세요."),
    HIDDEN_CONTENT(HttpStatus.FORBIDDEN, "운영 정책에 따라 숨겨진 콘텐츠입니다.");

    private final HttpStatus status;
    private final String message;
}
