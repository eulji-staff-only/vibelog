# message API 명세 (쪽지 · 알림 · 공지) — P2

> 영역: message / API ID: API-MSG-01~05, API-NTF-01~08
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md) U-25~U-27, M-13, A-10, 1-5(헤더 알림·공지 배너)이고, 테이블은 `message`, `notification`, `notice`입니다 ([DB 설계서](../02-database.md) 12장).
> P2 · 마일스톤 `4. P2 구현` · #169

## 쪽지 (FR-MSG-001 · 002)

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-MSG-01 | 쪽지함 | GET | `/messages` | 회원 | query `box`(received, sent. 없으면 received), `messageId`(선택: 오른쪽에 열 쪽지), `page` | U-27 + 탭별 쪽지 목록(상대 닉네임, 내용 앞부분, 보낸 시각, 받은 쪽지는 읽음 여부) 최신순 20개씩, 페이지 정보 / `messageId`가 있으면 그 쪽지 내용과 (받은 쪽지면) 답장 폼. **받은 쪽지를 열면 `read_at`을 기록한다** | `MESSAGE_NOT_FOUND`(없음), `NOT_OWNER`(주고받은 두 사람이 아님, PL-MSG-001) | FR-MSG-002, PL-MSG-001 |
| API-MSG-02 | 받는 사람 찾기 | GET | `/api/users` | 회원 | query `nickname` | JSON `[{"memberId", "nickname"}]` — 닉네임이 `nickname`으로 시작하는 회원 최대 10명. 나와 탈퇴 회원은 뺀다. `nickname`이 비면 빈 목록 | - | FR-MSG-001 (U-27 새 쪽지) |
| API-MSG-03 | 쪽지 보내기 (쪽지함) | POST | `/messages` | 인증 | form `receiverMemberId`, `content` | 저장 → 받는 사람에게 MESSAGE 알림 → `/messages?box=sent&messageId={새 messageId}` + toast "쪽지를 보냈습니다" | `INVALID_INPUT`(내용 1~1,000자, PL-MSG-003), `BANNED_WORD`(NFR-SEC-009), `EMAIL_NOT_VERIFIED`, `MESSAGE_SELF`, `MESSAGE_RECEIVER_NOT_FOUND`(없음·탈퇴), `MESSAGE_DAILY_LIMIT`, `MESSAGE_UNANSWERED_LIMIT` → U-27 새 쪽지 폼 다시 | FR-MSG-001, PL-MSG-003 |
| API-MSG-04 | 쪽지 보내기 (공개 프로필) | POST | `/users/{memberId}/messages` | 인증 | path `memberId`(받는 사람), form `content` (M-13) | API-MSG-03과 같게 저장·알림 → `/users/{memberId}` + toast "쪽지를 보냈습니다" | API-MSG-03과 같음 → `/users/{memberId}` + toast(에러 메시지) (③, 모달 폼이라 다시 그리지 않음) | FR-MSG-001 |
| API-MSG-05 | 답장 | POST | `/messages/{messageId}/reply` | 인증 | path `messageId`(내가 받은 쪽지), form `content` | 새 쪽지(`parent_message_id` = `messageId`, 받는 사람 = 원래 보낸 사람) 저장 → MESSAGE 알림 → `/messages?box=received&messageId={messageId}` + toast "답장을 보냈습니다" | API-MSG-03과 같음, `MESSAGE_NOT_FOUND`, `NOT_OWNER`(내가 받은 쪽지가 아님) | FR-MSG-002 |

- **보내기 검사 순서:** 입력 → 금칙어 → 이메일 인증 → 나에게 보냄 → 받는 사람 → 하루 20통 → 연속 3통. 세 POST가 같은 Service 메서드를 쓴다.
- **하루 20통:** 오늘 0시부터 내가 보낸 쪽지가 20통이면 막는다 (`idx_message_sender_member_id_created_at`).
- **연속 3통:** 상대가 나에게 보낸 마지막 쪽지 이후(없으면 처음부터) 내가 그 상대에게 보낸 쪽지가 3통이면 막는다. 상대가 한 번 답하면 다시 3통까지 보낼 수 있다.
- **1년 지난 쪽지 삭제 (PL-MSG-003):** 하루 한 번 배치로 `created_at`이 1년 지난 행을 지운다. 지울 쪽지를 가리키는 답장이 있으면 답장의 `parent_message_id`를 먼저 비운다 (FK가 RESTRICT).
- `sender_deleted_at`·`receiver_deleted_at`(각자 지우기)는 화면 설계(U-27)에 삭제 버튼이 없어 이번에는 쓰지 않는다.
- 사용자 차단(FR-MSG-003, PL-MSG-002)은 이번 범위 제외다.

## 알림 (FR-NTF-001)

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-NTF-01 | 알림 목록 | GET | `/api/notifications` | 회원 | - | JSON `{"unreadCount", "items": [{"notificationId", "notificationType", "content", "read", "createdAt"}]}` — 내 알림 최신순 20개 (헤더 드롭다운을 열 때 부름) | - | FR-NTF-001 |
| API-NTF-02 | 알림 열기 | GET | `/notifications/{notificationId}` | 회원(본인) | path `notificationId` | `read_at` 기록 → 아래 "알림 도착지" 주소로 redirect | `NOTIFICATION_NOT_FOUND`, `NOT_OWNER` | FR-NTF-001 |
| API-NTF-03 | 모두 읽음 | POST | `/api/notifications/read-all` | 회원 | - | 내 안 읽은 알림 전부 `read_at` 기록 → 204 | - | PL-NTF-002 |

**알림 도착지** (target 값은 [_template.md](_template.md) 10장 2번 표):

| notification_type | target_type | redirect 주소 | 주소를 만들 때 필요한 값 |
| --- | --- | --- | --- |
| RECOMMEND | POST | `/community/{postId}` | - |
| COMMENT · REPLY | COMMENT | `/community/{postId}#comment-{commentId}` | 댓글의 `postId` → `CommunityReader` |
| REVIEW | REVIEW | `/portfolios/{portfolioId}` | 리뷰의 `portfolioId` → `CommunityReader` (동료 리뷰는 community 영역, #170) |
| MESSAGE | MESSAGE | `/messages?box=received&messageId={messageId}` | - |
| BADGE | CONTEST_AWARD | `/mypage?tab=badges` | - (`badges` 탭 이름은 #170 auth 명세와 맞춘다) |
| NOTICE | NOTICE | `/notices/{noticeId}` | - |
| HIDDEN (P3) | (신고 대상) | P3에서 정한다 | - |

- 대상이 지워졌으면 redirect한 화면이 자기 "없음" 처리를 한다 (예: `POST_NOT_FOUND` → E-01). 알림 쪽에서 미리 확인하지 않는다.
- **알림 문구** (`content`, 255자): 아래 "영역 간 계약"의 메서드가 만든다. 부르는 쪽은 문구를 만들지 않는다.
- **90일 지난 알림 삭제 (PL-NTF-002):** 하루 한 번 배치로 지운다 (`idx_notification_created_at`).
- 이메일 알림은 두지 않는다 (PL-NTF-001).

## 공지 (FR-NTF-003 · 004 · 005)

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-NTF-04 | 공지 목록 | GET | `/notices` | 회원 | query `page` | U-25 + 공지(제목, 게시일) 최신순 10개씩(0-1), 페이지 정보. 없으면 "등록된 공지사항이 없습니다" | - | FR-NTF-004 |
| API-NTF-05 | 공지 상세 | GET | `/notices/{noticeId}` | 회원 | path `noticeId` | U-26 + 제목, 게시일, 본문 | `NOTICE_NOT_FOUND` | FR-NTF-003, FR-NTF-004 |
| API-NTF-06 | 공지 관리 | GET | `/admin/notices` | 운영자 | query `page`, `noticeId`(선택: 수정할 공지를 폼에 채움) | A-10 + 공지 목록(제목, 게시일, 배너 노출 여부·기간) 최신순 10개씩, 작성·수정 폼 | `NOTICE_NOT_FOUND` | FR-NTF-005 |
| API-NTF-07 | 공지 작성 | POST | `/admin/notices` | 운영자 | form `title`, `body`, `isBannerShown`, `bannerStartsAt`, `bannerEndsAt` | 저장(`admin_member_id` = 로그인 운영자) → 탈퇴하지 않은 회원 전체에 NOTICE 알림 → `/admin/notices` + toast "공지를 등록했습니다" | `INVALID_INPUT`(제목 1~100자, 본문 필수, 배너면 시작·끝 필수이고 끝이 시작보다 뒤 — `ck_notice_banner`), `BANNER_PERIOD_OVERLAP` → A-10 폼 다시 | FR-NTF-005, PL-NTF-003 |
| API-NTF-08 | 공지 수정 | POST | `/admin/notices/{noticeId}/edit` | 운영자 | path `noticeId`, form API-NTF-07과 같음 | 저장 → `/admin/notices` + toast "공지를 수정했습니다" (알림은 다시 보내지 않음) | API-NTF-07과 같음, `NOTICE_NOT_FOUND` | FR-NTF-005, NFR-SCL-001 |

- **배너는 동시에 1개 (PL-NTF-003):** 배너 기간이 다른 배너 공지와 겹치면 저장을 막는다 (`BANNER_PERIOD_OVERLAP`). 수정할 때는 자기 자신을 빼고 본다.
- 공지 삭제는 요구사항·화면에 없어 만들지 않는다 (`notice`에 `deleted_at`도 없다).
- 공지 관리는 운영자 화면이지만 message 영역이 구현하므로 이 파일에 둔다 (ID는 업무 코드 NTF, D-04).

## 헤더에 보여줄 값 (공통 모델)

모든 화면의 헤더(`templates/common/layout.html`)가 쓰는 값이다. message 영역의 `@ControllerAdvice`가 화면 응답에 넣는다.

| 모델 이름 | 누구에게 | 값 | 화면 |
| --- | --- | --- | --- |
| `unreadNotificationCount` | 회원 | 안 읽은 알림 수 (`idx_notification_member_id_read_at`) | 알림 아이콘 표시. 아이콘을 누르면 API-NTF-01 |
| `noticeBanner` | 전체 | 지금 배너 기간인 공지 1개 `{noticeId, title}`, 없으면 `null` | 상단 바 위 배너, 누르면 U-26 (비회원은 로그인으로, 0-2) |

## 영역 간 계약 (message 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/message/model/policy/`에 두고 message 영역이 구현합니다. 저장만 하고 돌려주는 값은 없습니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑬ | `NotificationSender` (community, contest) | `void recommended(Long actorMemberId, Long receiverMemberId, Long postId, String postTitle)` | RECOMMEND · POST. 문구 "내 글 「{postTitle}」이 추천을 받았습니다" (FR-CMT-005, #179) |
| | | `void commented(Long actorMemberId, Long receiverMemberId, Long commentId, String postTitle)` | COMMENT · COMMENT. "내 글 「{postTitle}」에 댓글이 달렸습니다" (FR-CMT-004, #185) |
| | | `void replied(Long actorMemberId, Long receiverMemberId, Long commentId, String postTitle)` | REPLY · COMMENT. "내 댓글에 답글이 달렸습니다: 「{postTitle}」" (FR-CMT-004, #185) |
| | | `void reviewed(Long actorMemberId, Long receiverMemberId, Long peerReviewId, String portfolioTitle)` | REVIEW · REVIEW. "내 작품 「{portfolioTitle}」에 리뷰가 달렸습니다" (FR-REV-003, #183) |
| | | `void badgeAwarded(Long receiverMemberId, Long contestAwardId, String badgeName)` | BADGE · CONTEST_AWARD. "배지를 받았습니다: {badgeName}" (FR-RWD-001, #202) |

- `actorMemberId`와 `receiverMemberId`가 같으면(내 글에 내가 댓글 등) 저장하지 않는다.
- 제목이 길어 문구가 255자를 넘으면 제목을 줄여(…) 맞춘다.
- 쪽지(MESSAGE)·공지(NOTICE) 알림은 message 영역 안에서 남기므로 계약에 넣지 않는다.
- 부르는 쪽이 이 계약보다 먼저 구현되면 08 "안 멈추는 규칙"대로 받는 쪽에 `FakeNotificationSender`를 두고 진행한다.

## 다른 영역에 주는 읽기 값 (`MessageReader`)

| 메서드 | 쓰는 곳 |
| --- | --- |
| `List<NoticeSummary> findRecentNotices(int limit)` — 제목, 게시일, `noticeId` 최신순 | 메인 화면 최근 공지 (API-USR-01, #203) |

## 이 명세에서 정한 것 (리뷰에서 확인)

요구사항·정책·화면에 없어 이 명세에서 정했다.

| # | 항목 | 결정 | 이유 |
| --- | --- | --- | --- |
| 1 | 쪽지함 `box` 값 | `received`, `sent` (기본 `received`) | 마이페이지 `tab` 값처럼 소문자 |
| 2 | 받은 쪽지 읽음 | API-MSG-01에서 쪽지를 열면 기록 (GET 예외, API-EXM-05와 같음) | 같은 쪽지를 다시 열어도 결과가 같다 |
| 3 | 받는 사람 찾기 | 닉네임 앞부분 일치, 최대 10명 | U-27 "닉네임으로 찾아 골라" |
| 4 | 하루 20통 기준 | 오늘 0시부터 | PL-MSG-003 "하루" |
| 5 | 연속 3통 기준 | 상대의 마지막 쪽지 이후 내가 보낸 수 | PL-MSG-003 "답장이 없는 상대에게 연속 3통" |
| 6 | 알림 목록 개수 | 최신 20개 (드롭다운) | 90일 보관이라 전체는 많다 |
| 7 | 알림 열기 | GET redirect에서 읽음 기록 (GET 예외) | 드롭다운 항목이 링크 하나로 끝난다 |
| 8 | 알림 문구 | 위 ⑬ 표 | 부르는 쪽마다 문구가 달라지지 않게 한 곳에서 만든다 |
| 9 | 공지 알림 | 작성할 때 탈퇴하지 않은 회원 전체에 1건씩 (수정 때는 안 보냄) | FR-NTF-001 "공지"를 알림, 1-5 "공지 → U-26" |
| 10 | 배너 1개 | 기간이 겹치면 저장을 막음 | PL-NTF-003 "동시에 1개" |
| 11 | 공지 수정 | 만든다 (삭제는 안 만듦) | NFR-SCL-001 "백오피스에서 추가·수정" |
| 12 | 헤더 값 | `@ControllerAdvice`로 모든 화면에 넣음 | 헤더가 모든 화면에 있다 |

> 배치 삭제(쪽지 1년, 알림 90일)는 `@Scheduled`를 쓴다. `@EnableScheduling`은 `global/config`(형상관리)에 둔다.
