# contest API 명세 (콘테스트 · 배지) — P2

> 영역: contest / API ID: API-CTS-01~08
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md) U-32·U-33, M-09·M-10, A-11, U-24(배지 탭)·U-29(배지)이고, 테이블은 `contest`, `contest_entry`, `contest_vote`, `contest_award`입니다 ([DB 설계서](../02-database.md) 10장).
> P2 · 마일스톤 `4. P2 구현` · #174

## 콘테스트 (FR-CTS-001 ~ 005)

일정은 PL-CTS-001(매달 1~20일 출품, 21일~말일 투표, 다음 달 1일 발표)을 운영자가 `contest`에 기간으로 등록한 값을 따른다. 아래 "단계"는 지금 시각과 그 기간으로 정한다.

| 단계 | 조건 |
| --- | --- |
| 출품 | `entry_starts_at` ≤ 지금 < `entry_ends_at` |
| 투표 | `vote_starts_at` ≤ 지금 < `vote_ends_at` |
| 발표 | `vote_ends_at` ≤ 지금. 수상작은 집계 배치가 남긴 `contest_award` (아래 "결과 발표"), 없으면 "수상작이 없습니다" |

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CTS-01 | 이달의 콘테스트 | GET | `/contests/current` | 회원 | - | U-32 + 이번 달(`contest_month` = 지금 `yyyy-MM`) 주제, 설명, 일정, 단계. **출품 단계**: 내 출품(있으면 작품 제목), 없으면 출품할 내 공개 포트폴리오 카드(`PortfolioReader`, 개인 작품 포함 PL-CTS-002). 공개 포트폴리오가 없으면 "내 포트폴리오 보기" → U-19. **투표 단계**: 출품작 카드(작품 제목, 한 줄 요약, 작성자. 표 수는 보여주지 않음) 출품순, 오늘 투표했나, 내가 투표한 출품작, 미인증이면 "인증 메일 받기" → `/auth/verify`. **결과**: 지난 달 콘테스트 수상작(아래 API-CTS-05와 같은 값). 이번 달 콘테스트가 없으면 "이번 달 콘테스트를 준비하고 있습니다"와 결과만 | - | FR-CTS-001~004, PL-CTS-001 |
| API-CTS-02 | 출품 | POST | `/contests/{contestId}/entries` | 회원 | path `contestId`, form `portfolioId` (M-09 확인 후) | `contest_entry` 저장 → `/contests/current` + toast "출품했습니다" | `CONTEST_NOT_FOUND`, `ENTRY_CLOSED`(출품 단계 아님), `PORTFOLIO_NOT_FOUND`(없음·삭제), `NOT_OWNER`, `PRIVATE_PORTFOLIO`(공개만, PL-CTS-002), `ALREADY_ENTERED`(1인 1작품, `uk_contest_entry_contest_member`) → ③ `/contests/current` + toast | FR-CTS-002, PL-CTS-002 |
| API-CTS-03 | 투표 | POST | `/api/contests/{contestId}/votes` | 회원 | path `contestId`, json `contestEntryId` | `contest_vote` 저장(`vote_date` = 오늘) → JSON `{"contestEntryId", "votedToday": true}` | `CONTEST_NOT_FOUND`, `VOTE_CLOSED`(투표 단계 아님), `EMAIL_NOT_VERIFIED`(PL-CTS-004), `ENTRY_NOT_FOUND`(없음·다른 콘테스트·출품 취소), `SELF_VOTE`, `DAILY_VOTE_LIMIT`(`uk_contest_vote_daily`), `ALREADY_VOTED`(같은 출품작, `uk_contest_vote_entry_voter`) | FR-CTS-003, PL-CTS-003·004 |
| API-CTS-04 | 지난 콘테스트 목록 | GET | `/contests` | 회원 | query `page` | U-33 + 투표가 끝난 콘테스트(월, 주제, 1등 작품 제목. 없으면 "수상작 없음") 최근 월순 10개씩, 페이지 정보. 없으면 "지난 콘테스트가 없습니다" | - | FR-CTS-005 |
| API-CTS-05 | 지난 콘테스트 상세 | GET | `/contests/{contestId}` | 회원 | path `contestId` | U-33 + 월, 주제, 설명, 수상작(순위, 작품 제목 → U-20, 작성자, 받은 표 수) 순위순. 없으면 "수상작이 없습니다" | `CONTEST_NOT_FOUND`(없음·투표 끝나기 전) | FR-CTS-005 |

- **출품 순서 검사:** 콘테스트 → 출품 단계 → 포트폴리오(없음·삭제) → 내 것 → 공개 → 1인 1작품.
- **투표 순서 검사:** 콘테스트 → 투표 단계 → 이메일 인증 → 출품작 → 내 작품 → 오늘 1표 → 같은 작품 1번.
- 출품 취소(`canceled_at`)된 출품작은 투표 목록·집계에서 빼고, 그 출품작이 받은 표는 무효다 (PL-CTS-006). 표 행은 지우지 않는다.
- **비공개·삭제된 수상작**(발표 뒤 작성자가 비공개·삭제하거나 탈퇴): 순위·작성자는 그대로 두고, 작품 제목 대신 "비공개된 작품"을 보여 주며 U-20 링크는 걸지 않는다 (API-CTS-01 결과·04·05 모두).
- 정지 회원은 #214 정지 필터가 POST 를 막아 출품·투표를 할 수 없다.

## 결과 발표 · 배지 (FR-CTS-004, FR-RWD-001 · 002)

- **결과는 투표가 끝난 순간으로 굳는다:** 투표가 끝나면 표를 받지 않고(`VOTE_CLOSED`), 출품도 취소하지 않는다(계약 ⑯은 투표가 끝나기 전 출품만 취소). 회원 상태(정지·탈퇴)도 투표가 끝난 시각으로 본다. 그래서 집계가 언제 돌아도 같은 결과가 나온다.
- **언제:** 10분마다 배치(`@Scheduled`)가 투표가 끝났고 `contest_award`가 없는 콘테스트를 집계한다. PL-CTS-001대로 투표가 말일 자정에 끝나면 다음 달 1일 0시 10분 안에 발표된다. 서버가 멈춰 배치가 밀려도 다시 돌면 그때 집계한다. 수상자가 없는 콘테스트(모두 0표 등)는 10분마다 다시 보지만 결과는 같다 (발표 완료를 따로 저장하지 않는다).
- **누가:** 출품 취소되지 않았고, **투표가 끝난 시각에** 정지 중이 아니었고 탈퇴하지 않은 회원의 출품작 (PL-CTS-006 "정지된 회원의 출품은 무효"). `MemberReader.findSuspendedMemberIds(작성자 ID 묶음, 시각)`·`findWithdrawnMemberIds(작성자 ID 묶음, 시각)`(auth 에 추가할 읽기 메서드)로 본다. **표가 1표 이상**인 출품작만 시상한다.
- **순위:** 받은 표 수 많은 순으로 1·2·3등을 매긴다 (같은 표 수는 같은 순위, 다음 순위는 그다음 표 수 — 1·1·2처럼 건너뛰지 않음). **한 순위는 2명까지** 공동 수상하고, 2명을 넘으면 먼저 출품한 2명만 받는다 (PL-CTS-006). "먼저 출품"은 `contest_entry_id`가 작은 것이다 (출품 순서대로 늘어나서 같은 초에 출품해도 순서가 정해진다). 3등까지만 준다.
- **배지 = `contest_award` 한 행.** 배지 이름은 "{yyyy}년 {M}월 {순위}등" (예: "2026년 11월 1등"). 수상자에게 BADGE 알림 (message 계약 ⑬ `badgeAwarded`).
- **M-10 배지 획득 축하:** 로그인한 회원에게 안 읽은 BADGE 알림이 있으면 화면에 축하 모달을 띄우고, "확인"은 그 알림 열기(`/notifications/{id}`, 읽음 + 마이페이지 배지 탭)로 보낸다. 모달 표시는 message 영역 헤더(`MessageHeaderAdvice`)가 맡는다.
- **배지 표시:** 마이페이지 배지 탭(API-USR-15 `tab=badges`)과 공개 프로필(API-CMT-14)이 아래 `ContestReader.findBadgesOf`로 보여준다 (월·순위, 최근 것부터).

## 콘테스트 관리 (FR-CTS-006, A-11)

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CTS-06 | 콘테스트 관리 | GET | `/admin/contests` | 운영자 | query `page`, `contestId`(선택: 수정할 콘테스트를 폼에 채움) | A-11 + 콘테스트 목록(월, 주제, 출품·투표 기간, 출품 수, 발표 여부) 최근 월순 10개씩, 작성·수정 폼. 새 폼은 다음 달과 PL-CTS-001 기본 기간(1일 0시 ~ 21일 0시 출품, 21일 0시 ~ 다음 달 1일 0시 투표)으로 채운다 | `CONTEST_NOT_FOUND` | FR-CTS-006 |
| API-CTS-07 | 콘테스트 등록 | POST | `/admin/contests` | 운영자 | form `contestMonth`(`yyyy-MM`), `topic`, `description`, `entryStartsAt`, `entryEndsAt`, `voteStartsAt`, `voteEndsAt` | 저장 → `/admin/contests` + toast "콘테스트를 등록했습니다" | `INVALID_INPUT`(주제 1~200자, 설명 필수, 기간 순서 — `ck_contest_period`, **모든 기간이 그 달 1일 0시 ~ 다음 달 1일 0시 안** — 이달의 콘테스트는 월로 찾으므로), `CONTEST_MONTH_EXISTS`(`uk_contest_contest_month`) → A-11 폼 다시 | FR-CTS-006, PL-CTS-001 |
| API-CTS-08 | 콘테스트 수정 | POST | `/admin/contests/{contestId}/edit` | 운영자 | path `contestId`, form API-CTS-07과 같음 | 저장 → `/admin/contests` + toast "콘테스트를 수정했습니다" | API-CTS-07과 같음, `CONTEST_NOT_FOUND`, `CONTEST_STARTED`(출품이 시작된 뒤에는 월·기간을 바꿀 수 없음. 주제·설명은 바꿀 수 있음) | FR-CTS-006 |

- 콘테스트 삭제는 요구사항·화면에 없어 만들지 않는다.
- 백오피스 왼쪽 메뉴 "설정 › 콘테스트"(1-5)에 둔다.

## 영역 간 계약 (contest 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/contest/model/policy/`에 두고 contest 영역이 구현합니다. community 명세(#208)의 ⑭ 다음입니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑮ | `ContestEntryChecker` (portfolio) | `boolean isEntered(Long portfolioId)` | 출품 중인가: 취소되지 않은 `contest_entry`가 있고 그 콘테스트의 `vote_ends_at`이 아직 안 지남. portfolio 의 수정(API-PRT-06)이 `CONTEST_ENTERED`로 막는다 (PL-PRT-007) |
| ⑯ | `ContestEntryCanceler` (portfolio) | `void cancelEntryOf(Long portfolioId)` | 출품 중인 그 작품의 `contest_entry.canceled_at` 기록. portfolio 가 삭제(API-PRT-07)·비공개 전환(API-PRT-08)·회원 탈퇴로 전부 지울 때(계약 ⑩ `MemberPortfolioCleaner.deleteAllOf` 안에서 작품마다) 부른다. 받은 표는 무효 (PL-CTS-006) |

- 출품작의 삭제·비공개는 막지 않는다. PL-CTS-006이 "투표 기간 중 출품작을 비공개·삭제하면 출품을 취소"로 정했기 때문이다.
- 부르는 쪽이 이 계약보다 먼저 구현되면 08 "안 멈추는 규칙"대로 받는 쪽에 `Fake…`를 두고 진행한다.

## 다른 영역에 주는 읽기 값 (`ContestReader`)

| 메서드 | 쓰는 곳 |
| --- | --- |
| `List<BadgeView> findBadgesOf(Long memberId)` — `contestAwardId`, 배지 이름, 월, 순위. 최근 것부터 | 마이페이지 배지 탭(API-USR-15), 공개 프로필(API-CMT-14) |
| `Optional<ContestSummary> findCurrent()` — 이번 달 `contestId`, 주제, 출품·투표 기간 | 메인 이달의 콘테스트(API-USR-01, #203) |

## 다른 영역에서 받는 읽기 값

| Reader | 메서드 | 쓰는 곳 |
| --- | --- | --- |
| `PortfolioReader` | 공개 포트폴리오 카드(제목, 한 줄 요약, 작성자)·요약(주인, 공개 여부, 삭제 여부). 없으면 portfolio Reader 에 읽기 메서드 추가 | 출품할 작품 고르기, 출품작·수상작 카드 |
| `MemberReader` | `getNicknames`, `isEmailVerified`, `findSuspendedMemberIds(Collection<Long>, LocalDateTime)`·`findWithdrawnMemberIds(Collection<Long>, LocalDateTime)`(추가: 그 시각에 정지 중이던·이미 탈퇴한 회원) | 작성자, 투표 자격, 집계 |

## 이 명세에서 정한 것 (리뷰에서 확인)

요구사항·정책·화면에 없어 이 명세에서 정했다.

| # | 항목 | 결정 | 이유 |
| --- | --- | --- | --- |
| 1 | "출품 중" 기간 | 취소 안 됨 + 투표가 끝나기 전 | PL-PRT-007 "출품 중에는 수정 불가". 발표 뒤에는 수정할 수 있다 |
| 2 | 출품작 삭제·비공개 | 막지 않고 출품 취소 (계약 ⑯) | PL-CTS-006 |
| 3 | 투표 중 표 수 | 보여주지 않고 결과에서만 | 먼저 표를 많이 받은 작품에 표가 몰리지 않게 |
| 4 | 투표 응답 | JSON (`/api/…`) | U-32 "투표 → 갱신" (페이지 이동 없이) |
| 5 | 순위 매기기 | 같은 표 수 = 같은 순위, 다음은 바로 다음 순위, 순위당 2명, 3등까지 | PL-CTS-005·006 |
| 6 | 0표 출품작 | 시상하지 않음 | 표가 없으면 "득표순"이 성립하지 않는다 |
| 7 | 발표 시각 | 10분마다 배치가 투표가 끝났고 수상 기록이 없는 콘테스트를 집계 (결과가 굳어 있어 늦게·다시 돌아도 같음) | PL-CTS-001 "다음 달 1일 발표". 발표 완료 컬럼 없이 처리, 배치가 밀려도 놓치지 않음 |
| 8 | 정지·탈퇴 회원 출품 | 투표가 끝난 시각에 정지 중이었거나 탈퇴했으면 뺌 | PL-CTS-006. 집계 시각에 따라 수상자가 바뀌지 않게 |
| 9 | 배지 이름 | "{yyyy}년 {M}월 {순위}등" | FR-RWD-001 "해당 월·순위가 적힌 배지" |
| 10 | M-10 | 안 읽은 BADGE 알림이 있으면 띄우고, 확인 = 알림 열기 | 따로 "봤음" 컬럼이 없다 |
| 11 | 콘테스트 수정 | 출품 시작 뒤에는 월·기간을 못 바꿈 | 진행 중 일정이 바뀌면 출품·투표가 어긋난다 |
| 12 | U-32 결과 보드 | 지난 달 콘테스트 수상작 | 이번 달 결과는 다음 달 1일에 나온다 |
| 13 | 결과 굳히기 | 투표가 끝나면 표도 출품 취소도 받지 않음 | 집계 시각과 상관없이 같은 결과 |
| 14 | 비공개·삭제된 수상작 | 순위·작성자는 두고 제목 대신 "비공개된 작품", 링크 없음 | 다른 회원은 U-20을 열 수 없다 |
| 15 | 등록 기간 | 모든 기간이 그 달 안 | 이달의 콘테스트를 `contest_month`로 찾는다 |

> 배치(결과 발표)는 `@Scheduled`를 쓴다. `@EnableScheduling`은 `global/config/SchedulingConfig`(#218)에 있다.
