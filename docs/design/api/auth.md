# auth API 명세 (회원)

> 영역: auth / API ID: API-USR-01~17
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

## 메인 · 가입 · 로그인

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-USR-01 | 메인 | GET | `/` | 전체 | - | U-01 + 로그인 여부(헤더), 트랙 4개, 대표 코스 카드, 최근 공지 N개·이번 주 추천 작품 상위 3개·이달의 콘테스트(P2 전엔 빈 값) | - | FR-USR-006 |
| API-USR-02 | 회원가입 화면 | GET | `/signup` | 전체 | query `done`(선택) | U-02. `done=true`면 가입 완료 상태(인증 메일 안내 + 첫 진단 시작하기 → `/diagnosis`) | 로그인 상태이고 `done` 없으면 → `/` | FR-USR-001 |
| API-USR-03 | 회원가입 | POST | `/signup` | 전체 | form `nickname`, `email`, `password` | 계정 생성 → 인증 메일 발송 → 로그인 상태로 → `/signup?done=true` | `INVALID_INPUT`(PL-USR-001·002), `DUPLICATE_EMAIL`(로그인으로 이동 링크), `DUPLICATE_NICKNAME`, `BANNED_WORD`(PL-SEC-003) → 같은 폼 다시 | FR-USR-001, 002 |
| API-USR-04 | 로그인 화면 | GET | `/login` | 전체 | query `redirect`(선택) | U-03 + 진입 안내 문구(redirect가 있을 때) | - | FR-USR-004 |
| API-USR-05 | 로그인 | POST | `/login` | 전체 | form `email`, `password`, `rememberMe`, `redirect` | 세션 ID 새로 발급 → 도착지 규칙: ① redirect ② 운영자면 `/admin` ③ 진단 기록 없으면 `/diagnosis` ④ 진행 중 코스 있으면 `/my/learning` ⑤ 그 외 `/courses` | `LOGIN_FAILED`(PL-USR-007, 어느 값이 틀렸는지 알리지 않음), `LOGIN_LOCKED`(5회 연속 실패 시 5분), `SOCIAL_ACCOUNT_EMAIL`(PL-USR-005) → 같은 폼 다시 | FR-USR-004, PL-USR-006 |
| API-USR-06 | 소셜 로그인 시작 | GET | `/oauth2/authorization/{provider}` | 전체 | path `provider`(google, github) | 구글·깃허브 로그인 화면으로 이동 | - | FR-USR-003 |
| API-USR-07 | 소셜 로그인 돌아옴 | GET | `/login/oauth2/code/{provider}` | 전체 | (Security가 처리) | 처음이면 계정 생성(이메일 인증된 것으로 봄) → `/diagnosis`. 아니면 API-USR-05 도착지 규칙 | `SOCIAL_EMAIL_CONFLICT`(같은 이메일이 다른 방식으로 가입됨, PL-USR-005) → `/login` + 안내 | FR-USR-003 |
| API-USR-08 | 로그아웃 | POST | `/logout` | 회원 | - | → `/` | - | FR-USR-004 |

- 로그인 유지(`rememberMe`)는 14일, 체크하지 않으면 2시간 미사용 시 만료. 운영자 계정은 `rememberMe`를 받아도 적용하지 않는다 (PL-USR-006).
- 이용 정지 회원의 로그인 안내(M-07, `ACCOUNT_SUSPENDED`)는 P2에서 붙인다.
- 세션이 만료된 뒤 회원 화면에 오면 → `/login?redirect=`. JSON 요청이면 401 `LOGIN_REQUIRED`.

## 이메일 인증 · 비밀번호 찾기

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-USR-09 | 이메일 인증 | GET | `/auth/verify` | 전체 | query `token`(선택) | 인증 처리(`email_verified_at`) → U-04 성공 상태. `token` 없이 열면(콘테스트 "인증 메일 받기") U-04 다시 받기 상태 | `TOKEN_EXPIRED`·`TOKEN_USED`(24시간, PL-USR-004) → U-04 만료 상태 + 다시 받기 | FR-USR-002 |
| API-USR-10 | 인증 메일 다시 받기 | POST | `/api/auth/verification-mail` | 전체 | json `email` (로그인 상태면 생략) | 204(본문 없음). 새 링크를 보내면 이전 링크는 무효 처리. 화면은 안내 상자 "인증 메일을 다시 보냈습니다"(로그인 상태) | `RESEND_TOO_SOON`(1분 간격, PL-CST-002)·`ALREADY_VERIFIED`는 로그인한 본인에게만 (비회원은 204, 아래 설명), `INVALID_INPUT`(이메일 형식) | FR-USR-002 |
| API-USR-11 | 비밀번호 찾기 화면 | GET | `/password/find` | 전체 | query `sent`(선택) | U-05. `sent=true`면 "메일을 보냈습니다" 단계 | - | FR-USR-005 |
| API-USR-12 | 재설정 메일 보내기 | POST | `/password/find` | 전체 | form `email` | 가입 여부와 관계없이 → `/password/find?sent=true` (소셜 가입 계정이면 소셜 로그인 안내 메일) | `INVALID_INPUT` → 같은 폼 다시 | FR-USR-005 |
| API-USR-13 | 비밀번호 재설정 화면 | GET | `/password/reset` | 전체 | query `token` | U-06 | `TOKEN_EXPIRED`·`TOKEN_USED`(30분, 1회, PL-USR-004) → U-06 만료 상태 + U-05 바로가기 | FR-USR-005 |
| API-USR-14 | 비밀번호 재설정 | POST | `/password/reset` | 전체 | form `token`, `newPassword`, `newPasswordConfirm` | → `/login` + toast "비밀번호가 변경되었습니다" | `INVALID_INPUT`(PL-USR-001, 확인 불일치) → 같은 폼 다시, `TOKEN_EXPIRED`·`TOKEN_USED` → U-06 만료 상태 | FR-USR-005 |

- API-USR-10 의 `RESEND_TOO_SOON`·`ALREADY_VERIFIED`는 로그인한 본인에게만 돌려준다. 올바른 형식의 비회원 요청은 가입 여부·인증 여부·간격과 관계없이 항상 성공(204, 본문 없음)이다. 이메일이 비어 있으면 아무것도 하지 않고 204, 비어 있지 않은데 형식이 틀리면 400 `INVALID_INPUT`이다. 메일은 가입했고 아직 인증하지 않았으며 마지막 발송에서 1분이 지난 이메일에만 보낸다 (응답 상태로 가입 여부를 알 수 없게, #89 리뷰). 그래서 비회원 화면은 보냈는지 단정하지 않고 "가입했고 아직 인증 전인 이메일이면 새 인증 메일이 도착합니다"로 안내한다.

## 마이페이지 · 탈퇴

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-USR-15 | 마이페이지 | GET | `/mypage` | 회원 | query `tab`(info, posts, comments), `page` | U-24 + 닉네임, 이메일, 가입 방식 / 작성한 글 목록 / 작성한 댓글 목록. 회원 탈퇴는 info 탭 안 | - | FR-USR-008 |
| API-USR-16 | 회원 탈퇴 | POST | `/mypage/withdraw` | 회원 | - (M-06 확인 후) | 닉네임 "탈퇴한 사용자", 계정 정보 파기, 포트폴리오 삭제 표시, 소셜 연결 삭제 → 로그아웃 → `/` + toast | `SUSPENDED_CANNOT_WITHDRAW`(정지 중, PL-USR-010, P2) | FR-USR-008, PL-USR-009·010 |
| API-USR-17 | 비밀번호 변경 | POST | `/mypage/password` | 회원(이메일 가입) | form `currentPassword`, `newPassword` | 해시로 저장 → `/mypage?tab=info` + toast "비밀번호를 변경했습니다" | `INVALID_INPUT`(PL-USR-001)·`PASSWORD_MISMATCH`(현재 비밀번호 틀림) → 같은 폼 다시, `SOCIAL_ACCOUNT`(소셜 가입 계정은 비밀번호 없음) | 프로토타입(U-24), PL-USR-001 |
