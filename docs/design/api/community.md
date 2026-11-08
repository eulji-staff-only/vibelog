# community API 명세 (커뮤니티)

> 영역: community / API ID: API-CMT-01~15 (11~15는 P2), 동료 리뷰 API-REV-01~07 (P2)
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CMT-01 | 게시판 목록 | GET | `/community` | 전체 | query `category`(FREE, QNA, SHOWCASE, 없으면 전체), `q`(제목 검색), `page` | U-21 + 글 목록(카테고리, 제목, 작성자, 작성일, 댓글 수) 최신순, 페이지 정보 | 결과 없음은 빈 목록 | FR-CMT-001, PL-CMT-005 |
| API-CMT-02 | 게시글 상세 | GET | `/community/{postId}` | 전체 | path `postId` | U-22 + 카테고리, 제목, 작성자, 작성일, 본문, 사진, (작품 공유) 작품 링크·사용한 도구, 댓글·답글(삭제된 댓글은 "삭제된 댓글입니다", 수정됨 표시), 내 글 여부 | `POST_NOT_FOUND`(없음·삭제) | FR-CMT-002 |
| API-CMT-03 | 글쓰기 화면 | GET | `/community/write` | 회원 | - | U-23. 미인증이면 안내 상자와 등록 버튼 막음 | - | FR-CMT-003 |
| API-CMT-04 | 글 등록 | POST | `/community` | 인증 | form `category`, `title`, `body`, `workUrl`, `tools`, file `images`(최대 3장) | → `/community/{새 postId}` | `INVALID_INPUT`(제목 1~100자, 본문 1~5,000자, PL-CMT-010), `SHOWCASE_LINK_REQUIRED`(PL-CMT-008), `INVALID_FILE`(3장·5MB·JPG·PNG·WebP, PL-CMT-009, PL-DAT-004), `BANNED_WORD`, `EMAIL_NOT_VERIFIED` → 같은 폼 다시 | FR-CMT-003 |
| API-CMT-05 | 글 수정 화면 | GET | `/community/{postId}/edit` | 회원(작성자) | path `postId` | U-23 + 기존 내용 | `NOT_OWNER`, `POST_NOT_FOUND` | FR-CMT-003 |
| API-CMT-06 | 글 수정 | POST | `/community/{postId}/edit` | 인증(작성자) | path `postId`, form API-CMT-04와 같음 + `keepPostImageIds` | → `/community/{postId}` | API-CMT-04와 같음, `NOT_OWNER` | FR-CMT-003 |
| API-CMT-07 | 글 삭제 | POST | `/community/{postId}/delete` | 회원(작성자) | path `postId` (M-05 확인 후) | 삭제 표시 → `/community` + toast | `NOT_OWNER` | FR-CMT-003, PL-DAT-002 |
| API-CMT-08 | 댓글·답글 쓰기 | POST | `/community/{postId}/comments` | 인증 | path `postId`, form `content`, `parentCommentId`(답글이면) | → `/community/{postId}#comment-{commentId}` | `INVALID_INPUT`(500자, PL-CMT-006), `REPLY_DEPTH_EXCEEDED`(답글에 답글), `BANNED_WORD`, `EMAIL_NOT_VERIFIED` | FR-CMT-004 |
| API-CMT-09 | 댓글 수정 | POST | `/comments/{commentId}/edit` | 인증(작성자) | path `commentId`, form `content` | → `/community/{postId}#comment-{commentId}` | `NOT_OWNER`, `INVALID_INPUT`, `BANNED_WORD` | PL-CMT-006 |
| API-CMT-10 | 댓글 삭제 | POST | `/comments/{commentId}/delete` | 회원(작성자) | path `commentId` | 삭제 표시 → `/community/{postId}` | `NOT_OWNER` | PL-CMT-006 |

- 비회원도 목록과 상세를 본다(PL-USR-008). 작성·댓글은 회원, 그중 이메일 인증을 마친 회원만 한다(PL-USR-003).
- 댓글 알림(FR-CMT-004 "글 작성자에게 알린다")은 알림(FR-NTF-001)이 P2라 P2에서 붙인다.

## P2 — 추천 · 랭킹 · 즐겨찾기 · 공개 프로필 · 팔로우 (#170)

> P2 · 마일스톤 `4. P2 구현`. 테이블 `recommendation`, `favorite`, `follow`(DB 설계서 9장).

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CMT-02 (P2) | 게시글 상세 | GET | `/community/{postId}` | 전체 | (그대로) | U-22에 추천 수(작품 공유 글), 내가 추천했나·즐겨찾기했나(회원), 작성자 → `/users/{memberId}` 링크 추가 | - | FR-CMT-005, FR-CMT-007, FR-CMT-009 |
| API-CMT-11 | 추천·추천 취소 | POST | `/api/posts/{postId}/recommend` | 회원 | path `postId` | 내 추천이 없으면 추가, 있으면 취소. JSON `{"recommended", "recommendCount"}`. 추가할 때만 작성자에게 RECOMMEND 알림 (message 계약 ⑬) | `POST_NOT_FOUND`, `NOT_SHOWCASE`(작품 공유 글만, PL-CMT-001), `SELF_RECOMMEND`(PL-CMT-002) | FR-CMT-005, PL-CMT-001·002 |
| API-CMT-12 | 추천 랭킹 | GET | `/community/ranking` | 전체 | - | U-30 + 이번 주(월요일 0시부터) 받은 추천 수 상위 10개 작품 공유 글(순위, 제목, 작성자, 추천 수). 동점이면 이번 주 첫 추천을 먼저 받은 글이 위. 삭제·숨김 글은 뺀다 | - | FR-CMT-006, PL-CMT-003·004 |
| API-CMT-13 | 즐겨찾기·취소 | POST | `/api/posts/{postId}/favorite` | 회원 | path `postId` | 토글. JSON `{"favorited"}`. 모든 카테고리 글 가능 | `POST_NOT_FOUND` | FR-CMT-007 |
| API-CMT-14 | 공개 프로필 | GET | `/users/{memberId}` | 회원 | path `memberId`, query `page`(게시글) | U-29 + 닉네임, 배지(`ContestReader`, #202 전에는 빈 목록), 게시글 최신순 20개씩, 공개 포트폴리오 카드(`PortfolioReader`), 팔로워 수·팔로잉 수, 내가 팔로우하나, 나인가(나면 팔로우·쪽지 버튼 숨김). flash `messageContent`·`messageError`가 있으면 M-13을 연 채로 보여준다 (message API-MSG-04) | `USER_NOT_FOUND`(없음·탈퇴·영구 정지) | FR-CMT-009, FR-CMT-008, FR-RWD-002, FR-MSG-001 |
| API-CMT-15 | 팔로우·취소 | POST | `/api/users/{memberId}/follow` | 회원 | path `memberId` | 토글. JSON `{"following", "followerCount"}` | `USER_NOT_FOUND`, `SELF_FOLLOW` | FR-CMT-008 |

- 추천·즐겨찾기·팔로우는 "작성"이 아니라 이메일 인증 없이 된다 (PL-USR-003의 작성 목록에 없음, FR-USR-006 "추천은 회원만").
- 팔로우 알림은 요구사항(FR-NTF-001)에 없어 보내지 않는다.

## P2 — 동료 리뷰 (#170)

> 테이블 `review_criterion`, `review_request`, `peer_review`, `review_comment`(DB 설계서 11장). ID는 업무 코드 REV (D-04).

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-REV-01 | 리뷰 요청 | POST | `/portfolios/{portfolioId}/review-requests` | 회원(작품 주인) | path `portfolioId` (M-08 확인 후, U-19·U-20) | `review_request` 저장 → `/portfolios/{portfolioId}` + toast "리뷰를 요청했습니다" | `NOT_OWNER`, `PRIVATE_PORTFOLIO`(공개만, FR-REV-001), `REVIEW_REQUEST_OPEN`(이 작품에 열린 요청이 있음), `REVIEW_REQUEST_LIMIT`(내 열린 요청 3개, PL-REV-002) → ③ 같은 화면 + toast | FR-REV-001, PL-REV-002 |
| API-REV-02 | 리뷰 요청 목록 | GET | `/reviews` | 회원 | query `page`, `reviewRequestId`(선택: 리뷰 쓰기 폼을 연다) | U-31 + 열린 요청 카드(작품 제목, 한 줄 요약, 코스, 작성자, 요청일, 받은 리뷰 수) 최근 요청순 20개씩. 없으면 "지금 리뷰를 기다리는 작품이 없습니다". `reviewRequestId`가 있으면 그 작품의 평가 기준과 기준별 코멘트 폼 (이미 쓴 리뷰가 있으면 수정 폼) | `REVIEW_REQUEST_NOT_FOUND`, `REVIEW_REQUEST_CLOSED`, `SELF_REVIEW` → ③ `/reviews` + toast | FR-REV-002, FR-REV-003, PL-REV-001 |
| API-REV-03 | 리뷰 쓰기 | POST | `/review-requests/{reviewRequestId}/reviews` | 인증 | path `reviewRequestId`, form `comments[i].reviewCriterionId`, `comments[i].content` (기준이 없으면 `reviewCriterionId` 없이 1개) | `peer_review`·`review_comment` 저장 → 요청자에게 REVIEW 알림 (⑬) → `/portfolios/{portfolioId}` + toast "리뷰를 남겼습니다" | `INVALID_INPUT`(코멘트마다 1~1,000자. 보낸 `reviewCriterionId` 묶음이 그 과제의 활성 기준 묶음과 정확히 같아야 함 — 빠지거나 다른 과제 기준이면 거절. 활성 기준이 없으면 기준 없는 코멘트 1개만), `BANNED_WORD`(NFR-SEC-009), `EMAIL_NOT_VERIFIED`, `SELF_REVIEW`, `ALREADY_REVIEWED`(1인 1리뷰, PL-REV-003), `REVIEW_REQUEST_CLOSED` → U-31 폼 다시 | FR-REV-003, PL-REV-003 |
| API-REV-04 | 리뷰 수정 | POST | `/peer-reviews/{peerReviewId}/edit` | 인증(리뷰어) | path `peerReviewId`, form API-REV-03과 같음 | 코멘트 바꿈 → `/portfolios/{portfolioId}` + toast "리뷰를 수정했습니다" | `NOT_OWNER`, API-REV-03의 입력 검사 (요청이 마감돼도 수정 가능) | PL-REV-003 |
| API-REV-05 | 리뷰 삭제 | POST | `/peer-reviews/{peerReviewId}/delete` | 회원(리뷰어) | path `peerReviewId` | `deleted_at` 기록 → `/portfolios/{portfolioId}` + toast "리뷰를 삭제했습니다" | `NOT_OWNER` | PL-REV-003, PL-DAT-002 |
| API-REV-06 | 도움됨 표시·취소 | POST | `/api/reviews/{peerReviewId}/helpful` | 회원(요청자) | path `peerReviewId` | `helpful_at` 토글. JSON `{"helpful"}` | `NOT_OWNER`(요청자만, PL-REV-003) | FR-REV-004 |
| API-REV-07 | 리뷰 요청 마감 | POST | `/review-requests/{reviewRequestId}/close` | 회원(요청자) | path `reviewRequestId` (U-20 내 작품) | `closed_at` 기록 → `/portfolios/{portfolioId}` + toast "리뷰 요청을 마감했습니다" | `NOT_OWNER`, `REVIEW_REQUEST_CLOSED` | FR-REV-005 |

- **열린 요청:** `closed_at`이 없고 요청 후 14일이 지나지 않음 (PL-REV-002). 14일은 조회할 때 계산하고 배치로 닫지 않는다. 포트폴리오를 비공개·삭제하면 portfolio 영역이 계약 ⑭로 `closed_at`을 남긴다.
- **평가 기준:** 작품의 포트폴리오 과제(`PortfolioReader`로 과제 ID)에 딸린 `is_active` 기준을 `sort_order` 순으로 보여준다. 기준을 넣는 운영자 화면은 화면 설계에 없어 **`sql/02-seed.sql` 샘플 데이터**로 넣는다 (#209, DB 팀 `Choyeonik`). 운영자 화면이 생기기 전까지 새 과제의 기준도 DB 팀이 SQL로 넣는다. 기준이 없는 과제와 개인 작품은 기준 없는 코멘트 1개로 쓴다 (`review_comment.review_criterion_id` NULL).
- **지운 리뷰를 다시 쓰면** 지운 행을 되살려 내용을 바꾼다 (`uk_peer_review_request_reviewer`).
- **U-19·U-20은 portfolio 화면**이다. 리뷰 요청·마감 버튼, 받은 리뷰, 도움됨 버튼은 portfolio 화면에 `CommunityReader` 값으로 붙인다 (그 PR은 portfolio 담당 영역 리뷰).

## 영역 간 계약 (community 영역이 받는 것, P2)

인터페이스는 `domain/community/model/policy/`에 두고 community 영역이 구현합니다. 08 계약 표 번호는 message 명세(#207)의 ⑬ 다음인 ⑭입니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑭ | `ReviewRequestCloser` (portfolio) | `void closeOpenRequests(Long portfolioId)` | 포트폴리오를 비공개하거나 삭제할 때 그 작품의 열린 리뷰 요청에 `closed_at` 기록 (FR-REV-005, PL-REV-002) |

## `CommunityReader` P2 메서드

| 메서드 | 쓰는 곳 |
| --- | --- |
| `Long getPostIdOfComment(Long commentId)` — 삭제 표시된 댓글도 | 알림 도착지 (message API-NTF-02) |
| `Long getPortfolioIdOfReview(Long peerReviewId)` — 삭제 표시된 리뷰도 | 알림 도착지 (message API-NTF-02) |
| `List<RankingItem> findWeeklyTop(int limit)` | 메인 이번 주 추천 작품 상위 3개 (API-USR-01, #203) |
| `Page<PostSummary> findFavoritePosts(Long memberId, int page)`, `Page<MemberSummary> findFollowing(Long memberId, int page)` | 마이페이지 즐겨찾기·팔로우 탭 (API-USR-15 P2) |
| `List<ReviewView> findReviewsOfPortfolio(Long portfolioId, Long viewerMemberId)` | U-20 받은 리뷰·도움됨·내 리뷰 수정 |
| `Optional<Long> findOpenReviewRequestId(Long portfolioId)`, `Set<Long> findPortfolioIdsWithOpenRequest(Collection<Long> portfolioIds)` | U-20·U-19 리뷰 요청·마감 버튼 |

## 이 부분에서 정한 것 (리뷰에서 확인)

| # | 항목 | 결정 | 이유 |
| --- | --- | --- | --- |
| 1 | 추천·즐겨찾기·팔로우 | 같은 주소로 토글 (JSON) | "페이지 이동 없이 숫자를 갱신" (FR-CMT-005) |
| 2 | 랭킹 동점 | 이번 주 첫 추천을 먼저 받은 글이 위 | PL-CMT-004 "먼저 추천받은 글" |
| 3 | 공개 프로필 게시글 | 20개씩 (게시판과 같음, D-06) | 새 숫자를 만들지 않음 |
| 4 | 리뷰 쓰는 곳 | U-31 안의 폼 (`/reviews?reviewRequestId=`) → 저장 후 U-20 | 화면 흐름 "리뷰 쓰기 → 평가 기준별 코멘트 입력 → U-20", 새 화면을 만들지 않음 |
| 5 | 코멘트 길이 | 기준마다 1~1,000자 | 정책에 없음. 쪽지와 같은 길이 |
| 6 | 리뷰 요청 마감 버튼 | U-20 내 작품에 둠 | FR-REV-005 "요청을 닫고"의 위치가 화면에 없음 |
| 7 | 14일 자동 마감 | 조회할 때 계산 (배치 없음) | 기록 없이 계산할 수 있다 |
| 8 | 비공개·삭제 시 마감 | 계약 ⑭로 portfolio가 기록 | 다시 공개해도 요청이 되살아나지 않게 |
| 9 | 평가 기준 입력 | 샘플 SQL(#209), 새 과제도 DB 팀이 SQL로. 없으면 기준 없는 코멘트 1개 | 운영자 화면이 설계에 없음 |
| 10 | 지운 리뷰 다시 쓰기 | 지운 행을 되살림 | UNIQUE 제약 |
