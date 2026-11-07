# API 명세 공통 규칙 (양식)

> 영역별 명세는 이 폴더의 `auth.md`, `course.md`, `lesson.md`, `quiz.md`, `exam.md`, `portfolio.md`, `community.md`, `admin.md`입니다.
> 이 파일은 모든 영역이 같이 쓰는 규칙(0장), 에러 코드(7장), P2·P3 목록(8장), 확정 근거(9·10장)를 담습니다.
> 장 번호는 원본 `API 설계(2026-11-05)`의 번호를 그대로 둡니다 (영역별 표가 "7장", "10장"으로 가리키기 때문입니다).

> **팀 결정판:** [API 설계 결정판](https://claude.ai/artifact/5LEMLuehcBYMeELRLeK3xM)
> 0장 방식은 결정판 D-01~D-12의 **추천안**으로 확정했다 (10장).

| 항목 | 내용 |
| --- | --- |
| 문서 목적 | 화면의 이동과 버튼을 서버 요청으로 바꿔 적는다. 개발자가 이 표만 보고 Controller 주소, 받을 값, 돌려줄 화면을 만들 수 있게 한다 |
| 기준 문서 | [화면 흐름 설계서](../01-screen-flow.md), [DB 설계서](../02-database.md) (둘 다 확정), 요구사항 명세서·정책 정의서(2026-11-04, 팀(확정)), API 설계 가이드 |
| 범위 | **P1 전체를 상세히** 적는다(학습자 + 운영자). P2·P3는 8장에 목록만 둔다 |
| 상태 | **확정.** 화면 흐름 설계서와 DB 설계서에 맞췄다 (9장). 두 문서가 열어 둔 값은 10장에서 정했다 |

---
## 0. 규칙 (결정판 추천안으로 확정)

### 0-1. 방식

| 규칙 | 내용 | 결정판 |
| --- | --- | --- |
| 응답 방식 | 서버가 화면(Thymeleaf)을 만들어 보낸다. 화면 URL이 곧 그 화면의 GET API다. 화면이 바뀌지 않는 동작만 JSON으로 보낸다 | D-01 |
| Method | GET·POST만 쓴다. 수정은 `POST .../edit`, 삭제는 `POST .../delete`, 상태 변경은 `POST .../{동작}` | D-02 |
| JSON 주소 | `/api/`로 시작한다. 운영자 JSON은 `/api/admin/` | D-03 |
| 저장 후 응답 | 화면 폼으로 보낸 POST(저장·수정·삭제)가 성공하면 **redirect**한다(PRG). 새로고침해도 같은 요청이 다시 가지 않는다. 입력 오류면 redirect하지 않고 같은 폼을 다시 보여준다(입력값 유지 + 칸별 메시지). `/api/**`는 DB에 써도 JSON으로 답한다. 예외: 오답 분석 리포트 GET은 연 일시를 기록한다(API-EXM-05) | 참고 프로젝트 |
| 토스트 | redirect할 때 보여줄 문구는 flash 값 `toast`로 넘긴다 (예: "다음 개념이 열렸습니다") | 화면 흐름 5-1 |
| 로그인 | Spring Security. 폼 로그인 `POST /login`, 소셜 로그인 `/oauth2/authorization/{provider}`, 로그아웃 `POST /logout`. 모든 POST 폼에 CSRF 토큰 | D-07 |
| 페이지 | `page`는 1부터. 게시판 20개, 공지 10개씩 서버가 정한다 | D-06 |

### 0-2. URL

- 화면 URL은 화면 흐름 설계서 1-1·1-3의 주소를 그대로 쓴다(명사 복수형, 하위 자원은 `/{상위}/{id}/{하위}`, 운영자는 `/admin`).
- 목록의 필터·검색·탭은 쿼리 스트링으로 받는다.
- 로그인이 필요한 화면에 비회원이 오면 `/login?redirect={원래 URL}`로 보낸다.

### 0-3. 표 읽는 법

| 칸 | 내용 |
| --- | --- |
| ID | `API-{업무}-{번호}`. 업무 코드는 요구사항 명세서와 같다 (D-04) |
| 접근 | **전체**(비회원 포함) · **회원**(로그인) · **인증**(회원 + 이메일 인증, PL-USR-003) · **조건**(회원 + 학습 조건, 화면 흐름 0-3) · **운영자** |
| 요청 값 | `path` 주소 안 값, `query` 주소 뒤 값, `form` 폼 값, `json` JSON 본문, `file` 파일. 이름은 **DB 컬럼 이름을 camelCase로** 바꾼 것 (D-05). 예: `result_url` → `resultUrl`, `course_category_id` → `courseCategoryId`, `is_public` → `isPublic`. ID는 PK 이름 그대로(`memberId`, `quizAttemptId`). 화면 URL의 목록 필터(`track`, `category`, `q`, `tab`, `page`)는 화면 흐름 설계서 그대로 둔다 |
| 상태 값 | DB 설계서 4-3의 값을 그대로 쓴다 (대문자 + 밑줄, 예: `SHOWCASE`, `CODE_BLANK`) |
| 성공 응답 | 화면 ID(그 화면을 보여줌) · `→ 주소`(redirect) · `JSON`(본문 모양은 형상관리자 공통 포맷, D-08) |
| 실패 | 7장 에러 코드. 괄호 안은 근거 정책 |

## 7. 에러 코드 (형상관리자 공통 포맷 초안)

### 7-1. 처리 방식

| 기호 | 방식 | 언제 |
| --- | --- | --- |
| ① | 에러 화면 E-01(404)·E-02(403)·E-03(500)·E-04(점검 중) | 화면을 그릴 수 없을 때. 모두 문의 이메일 표시 (PL-ERR-006) |
| ② | 로그인으로 이동(`?redirect=`) | 비회원·세션 만료 |
| ③ | 다른 화면으로 이동 + 토스트 | 잠긴 수업·모의고사, 권한 없는 백오피스 |
| ④ | 같은 폼 다시 + 칸별 메시지 | 폼 입력 오류 (D-09) |
| ⑤ | JSON 실패 응답 | `/api/**` 요청 전부 |

`/api/**` 요청은 어떤 실패든 ⑤로 보낸다. 아래 "방식"은 화면 요청일 때다.

⑤ JSON 응답 모양 (D-08, #152): 성공은 명세에 적힌 값을 그대로 담은 JSON(감싸지 않음), 실패는 에러 코드의 HTTP 상태 + `{"code": "에러 코드", "message": "보여 줄 문구"}`. 아래 표에 이름이 없는 실패는 `NOT_FOUND`(404, 없는 주소), `FORBIDDEN`(403, CSRF 토큰 없음·만료), `CONFLICT`(409, 정책으로 걸러지지 않은 DB 제약 위반), `SERVER_ERROR`(500)로 보낸다. 자세한 표는 README "JSON API 약속".

### 7-2. 공통

| 코드 | 상태 | 메시지 | 방식 |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 칸별 검증 메시지 | ④ |
| `LOGIN_REQUIRED` | 401 | 로그인이 필요합니다. | ② |
| `ADMIN_ONLY` | 403 | (표시 없이 메인으로) | → `/` |
| `NOT_OWNER` | 403 | 본인만 할 수 있습니다. | ① E-02 |
| `EMAIL_NOT_VERIFIED` | 403 | 이메일 인증을 마치면 글을 쓸 수 있습니다. | ④ |
| `BANNED_WORD` | 400 | 사용할 수 없는 단어가 들어 있습니다. | ④ |
| `INVALID_FILE` | 400 | JPG·PNG·WebP 파일만, 장당 5MB까지 올릴 수 있습니다. (설정 파일은 md·json·yaml·txt) | ④ |
| `NOT_FOUND`(없는 주소) | 404 | 요청하신 페이지를 찾을 수 없습니다. | ① E-01 |
| 서버 오류 | 500 | 잠시 후 다시 시도해 주세요. (문의용 오류 번호, PL-ERR-003) | ① E-03 |
| 점검 중 | 503 | 서비스 점검 중입니다. | ① E-04 |

### 7-3. 업무별

| 코드 | 상태 | 메시지 | 방식 | 근거 |
| --- | --- | --- | --- | --- |
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일입니다. 로그인해 주세요. | ④ | FR-USR-001 |
| `DUPLICATE_NICKNAME` | 409 | 이미 사용 중인 닉네임입니다. | ④ | PL-USR-002 |
| `LOGIN_FAILED` | 401 | 이메일 또는 비밀번호가 맞지 않습니다. | ④ | PL-USR-007 |
| `LOGIN_LOCKED` | 429 | 로그인에 5번 실패해 5분 동안 로그인할 수 없습니다. | ④ | PL-USR-007 |
| `SOCIAL_ACCOUNT_EMAIL` / `SOCIAL_EMAIL_CONFLICT` | 409 | 처음 가입한 방식(이메일·GitHub·구글)으로 로그인해 주세요. | ④ / → `/login` | PL-USR-005 |
| `SOCIAL_ACCOUNT` | 409 | 구글·깃허브로 가입한 계정은 비밀번호가 없습니다. | ④ | U-24 |
| `PASSWORD_MISMATCH` | 400 | 현재 비밀번호가 맞지 않습니다. | ④ | API-USR-17 (#116 리뷰) |
| `SUSPENDED_CANNOT_WITHDRAW` | 409 | 이용 정지 중에는 탈퇴할 수 없습니다. (P2) | ③ | PL-USR-010 |
| `CONTEST_ENTERED` | 409 | 콘테스트에 출품한 작품은 수정할 수 없습니다. (P2) | ④ | PL-PRT-007 |
| `TOKEN_EXPIRED` / `TOKEN_USED` | 400 | 링크가 만료되었습니다. | 화면 안 상태 | PL-USR-004 |
| `RESEND_TOO_SOON` | 429 | 1분 뒤에 다시 받을 수 있습니다. | ⑤ | PL-CST-002 |
| `ALREADY_VERIFIED` | 409 | 이미 인증된 이메일입니다. | ⑤ | FR-USR-002 |
| `TRACK_NOT_FOUND` / `COURSE_NOT_FOUND` / `LESSON_NOT_FOUND` / `POST_NOT_FOUND` / `PORTFOLIO_NOT_FOUND` / `USER_NOT_FOUND` / `FILE_NOT_FOUND` | 404 | 찾을 수 없습니다. | ① E-01 | PL-ERR-006 |
| `LESSON_LOCKED` | 403 | 이전 수업의 모의고사를 보고 리포트를 확인하면 열립니다. | ③ → `/my/learning` | FR-ENR-003 |
| `ACTIVITY_LOCKED` | 403 | 앞 퀴즈를 정답률 50% 이상으로 통과하면 열립니다. | ③ → 수업 화면 | FR-LSN-009 |
| `ALREADY_CHECKED` | 409 | 이미 확인한 문항입니다. | ⑤ | PL-LSN-006 |
| `ATTEMPT_SUBMITTED` | 409 | 이미 제출했습니다. | ⑤ / ③ | DB `quiz_attempt`·`exam_attempt` |
| `QUIZ_NOT_FINISHED` | 400 | 모든 문항을 확인한 뒤 제출할 수 있습니다. | ⑤ | PL-LSN-006 |
| `EXAM_LOCKED` | 403 | 이 수업의 마지막 퀴즈를 통과하면 열립니다. | ③ → `/my/learning` | FR-EXM-001 |
| `REPORT_NOT_VIEWED` | 409 | 지난 응시의 리포트를 먼저 확인해 주세요. | ③ → 리포트 | PL-EXM-003 |
| `EXAM_NOT_SUBMITTED` | 409 | 제출한 뒤에 리포트를 볼 수 있습니다. | ③ → 모의고사 | FR-EXM-003 |
| `PORTFOLIO_LOCKED` | 403 | 마지막 수업의 오답 분석 리포트를 확인하면 열립니다. | 화면 안 안내 상자 / ⑤ | PL-PRT-001 |
| `ALREADY_SUBMITTED` | 409 | 이미 제출한 포트폴리오가 있습니다. 내 포트폴리오에서 수정해 주세요. | ④ | DB `portfolio` UNIQUE |
| `PRIVATE_PORTFOLIO` | 403 | 비공개된 작품입니다. | ① E-02 | PL-ERR-006 |
| `SHOWCASE_LINK_REQUIRED` | 400 | 작품 공유 글은 작품 링크가 필요합니다. | ④ | PL-CMT-008 |
| `REPLY_DEPTH_EXCEEDED` | 400 | 답글에는 답글을 달 수 없습니다. | ③ → 게시글 | PL-CMT-006 |
| `DUPLICATE_NAME` | 409 | 같은 이름이 이미 있습니다. | ④ | DB `track`·`course_category` UNIQUE |
| `TRACK_HAS_COURSES` / `CATEGORY_HAS_COURSES` | 409 | 코스가 있어 삭제할 수 없습니다. 코스를 먼저 옮겨 주세요. | ③ | 화면 흐름 4장 |
| `COURSE_NOT_PUBLISHABLE` | 409 | 공개된 수업과 포트폴리오 과제가 있어야 공개할 수 있습니다. | ③ | FR-CRS-006 |
| `LESSON_NOT_PUBLISHABLE` | 409 | 빠진 항목: (목록) | ③ | FR-LSN-014 |
| `INVALID_ORDER` | 400 | 개념 뒤에는 퀴즈가 하나씩 와야 합니다. | ③ | PL-LSN-001 |
| `CONCEPT_REQUIRED` | 400 | 연관 개념을 골라야 저장할 수 있습니다. | ④ | FR-EXM-007 |

## 8. P2 · P3 (목록만)

P1 구현 뒤 같은 규칙으로 표를 채운다. 테이블은 이미 있다(DB 설계서 4-1의 8~13장).

| 우선 | 기능 | 화면 URL | 하기(동작) API 후보 |
| --- | --- | --- | --- |
| P2 | 프로필 수정 | `/mypage?tab=info` | POST `/mypage/profile` |
| P2 | 진단 다시 하기 | `/diagnosis` | API-DGN-02 재사용 |
| P2 | 카테고리 탐색·코스 검색 (헤더 검색 포함) | `/courses?category=&q=` | API-CRS-02 query 사용 |
| P2 | 수강 포기 | `/my/learning` | POST `/courses/{courseId}/drop` (M-12 확인 후) |
| P2 | 연관 개념·개념 북마크 | U-13, U-24 북마크 탭 | POST `/api/concepts/{activityId}/bookmark` |
| P2 | 틀린 문제 모아보기 | `/my/wrong-answers?course=&lesson=` | GET만 |
| P2 | 평균 점수·리포트 다시 보기 | U-16 | API-EXM-05 응답에 추가 |
| P2 | FAQ | U-17 | API-PRT-01 응답에 추가 |
| P2 | 같은 코스 작품 보기 | `/courses/{courseId}/portfolios` | GET만 (그 코스 포트폴리오를 낸 사람만) |
| P2 | 개인 작품 추가 | `/my/portfolios/new?template=` | POST `/my/portfolios` |
| P2 | 프로필 수정·닉네임 저장 | `/mypage?tab=info` | POST `/mypage/profile` |
| P2 | 추천·랭킹·즐겨찾기·팔로우·공개 프로필 | `/community/ranking`, `/users/{memberId}` (회원) | POST `/api/posts/{postId}/recommend`, `/api/posts/{postId}/favorite`, `/api/users/{memberId}/follow` |
| P2 | 콘테스트 | `/contests/current`, `/contests/{contestId}` (회원) | POST `/contests/{contestId}/entries`, `/api/contests/{contestId}/votes`. 미인증 "인증 메일 받기" → `/auth/verify` (API-USR-09, token 없음) |
| P2 | 동료 리뷰 | `/reviews` | POST `/portfolios/{portfolioId}/review-requests`, `/review-requests/{reviewRequestId}/reviews`, `/api/reviews/{peerReviewId}/helpful` |
| P2 | 쪽지 | `/messages?box=` | POST `/messages`(쪽지함 새 쪽지), `/users/{memberId}/messages`(공개 프로필 M-13), `/messages/{messageId}/reply`, GET `/api/users?nickname=`(받는 사람 검색) |
| P2 | 알림 | 상단 드롭다운 | GET `/api/notifications`, POST `/api/notifications/read-all` |
| P2 | 공지 | `/notices`, `/notices/{noticeId}` (회원) | GET만 (운영자 A-10 별도) |
| P2 | 운영자: 이용 정지, 진단 설정, 공지·콘테스트 관리, 확장 대시보드 | A-08~A-11 | `/admin/**` 같은 규칙 |
| P3 | 신고·신고 처리, 템플릿 관리 | M-11, A-12, A-13 | POST `/api/reports`, `/admin/reports/{reportId}/hide`·`/dismiss` |

## 9. 확정 문서와 맞춘 것

### 9-1. 화면 흐름 설계서(2026-11-05)

화면 흐름 설계서 9장의 Q1~Q9가 모두 닫혀서, 11-04 임시본과 설계서가 다르던 곳을 설계서대로 맞췄다. 설계서의 P1 화면(U-01~U-24, A-01~A-08)에 있는 버튼·이동마다 API가 있는지 확인했다 (API 설계 가이드 4단계). 화면 안에서만 바뀌는 동작(탭, 문항 이동, 모달 열기)과 외부 링크(도구 노트, 결과물 링크)는 API가 없다.

| 바뀐 곳 | 11-04 임시본 | 이 문서 | 근거 |
| --- | --- | --- | --- |
| API-USR-05·07 로그인 도착지 | ④ 그 외 `/` | ② 운영자 `/admin`, ⑤ 그 외 `/courses` | FR-USR-004, 설계서 U-03 |
| API-USR-02·03 회원가입 | 가입 후 바로 `/diagnosis` | `/signup?done=true` 가입 완료 상태 → 첫 진단 시작하기 | 설계서 U-02 (프로토타입) |
| API-USR-09 이메일 인증 | token 필수 | token 없이 열면 다시 받기 상태 (콘테스트 "인증 메일 받기") | 설계서 U-04 (프로토타입) |
| API-USR-17 비밀번호 변경 | 만들지 않음 (명세 밖) | **추가** | 설계서 Q6 (프로토타입, 명세를 어기지 않는 추가) |
| P2 쪽지 | 공개 프로필 모달로만 | 쪽지함 새 쪽지 + 받는 사람 검색 **추가** | 설계서 Q7 |
| API-PRT-05·06 포트폴리오 수정 | 내 포트폴리오의 수정 모달(M-03) | U-17 수정 상태 (`?edit=true`), M-03 삭제 | 설계서 U-19 (프로토타입) |
| API-PRT-09 포트폴리오 상세 | 전체(공개만) | 회원 (비회원은 로그인) | FR-USR-006 |
| P2 공지·콘테스트·공개 프로필 | 전체 | 회원 | FR-USR-006, 2026-11-05 결정 |
| API-USR-15 마이페이지 탭 | info, posts, comments, withdraw | info, posts, comments (탈퇴는 info 안) | 설계서 U-24 (프로토타입) |
| 에러 | - | E-04 점검 중, `SOCIAL_ACCOUNT`, `SUSPENDED_CANNOT_WITHDRAW`, `CONTEST_ENTERED` 추가 | NFR-UX-006, PL-USR-010, PL-PRT-007 |

그대로 둔 것: 퀴즈는 확인할 때마다 답 저장(API-LSN-05), 모의고사 이어 풀기(API-EXM-01·03), 비회원 게시판 목록·상세 열람(API-CMT-01·02). 11-04 임시본이 이미 명세대로 되어 있었고 설계서도 같게 고쳤다.

명세를 그대로 적용하면서 생기는 점:

- 미인증 학습자는 포트폴리오를 제출할 수 없어서(PL-USR-003) 코스 완료(PL-ENR-003)도 인증 뒤에 된다. API-PRT-01 화면에 인증 안내를 넣는다.
- 진단 추천 이유는 응답을 저장하지 않기 때문에(PL-DGN-001) 제출 직후 한 번만 자세히 보인다 (course.md의 진단 메모).

### 9-2. DB 설계서(2026-11-05)

요청·응답 값과 처리 내용을 DB 설계서의 물리 이름(2장)·상태 값(4-3)·삭제 정책(5장)에 맞췄다 (API 설계 가이드 3단계). 테이블 정의서(51개) 원본은 받지 않아서, 컬럼은 DB 설계서 7장 대조표와 2장 변환표로 확인했다.

| 바뀐 곳 | 전 | 지금 | DB 설계서 |
| --- | --- | --- | --- |
| ID 이름 | `userId`, `questionId`, `choiceId`, `attemptId`, `optionIds`, `stepId`, `parentId`, `categoryId`, `keepImageIds`, 리뷰 `{id}` | `memberId`, `quizQuestionId`·`examQuestionId`, `quizChoiceId`·`examChoiceId`, `quizAttemptId`·`examAttemptId`, `diagnosisOptionIds`, `guideStepId`, `parentCommentId`, `courseCategoryId`, `keepPostImageIds`, `reviewRequestId`·`peerReviewId` | 2-3 PK·FK (`User` → `member`, PK는 `테이블명_id`) |
| 퀴즈 결과 | `passed` | `isPassed` | 2-2 (`quiz_attempt.is_passed`) |
| 퀴즈 통과 기준 | 맞힌 수 ÷ 문항 수 ≥ 0.5 | 정답률 ≥ `activity.pass_rate`(기본 50) | 7장 FR-LSN-006 |
| 처리 내용의 테이블 이름 | DiagnosisResult, Enrollment, LessonProgress, QuizAnswer, ExamAnswer | `diagnosis_result`, `enrollment`, `lesson_progress`, `quiz_answer`, `exam_answer` | 2-1 |
| 리포트 회차 | `attempt` | `attempt`(= `exam_attempt.attempt_no`) | 6-3 |

맞는지 확인만 한 것:

- 상태 값: `FREE`·`QNA`·`SHOWCASE`, `CODE_BLANK`·`CONCEPT`·`OUTPUT_PREDICT`·`TRICK`, `COMPLETED`·`SKIPPED`, `CONCEPT`·`QUIZ`, `MARKDOWN`·`RICH_TEXT`, `GOOGLE`·`GITHUB`, `LEARNER`·`ADMIN` 모두 4-3과 같다.
- 수강 상태(코스 상세의 NONE·ENROLLED·COMPLETED)는 DB 컬럼이 아니라 `enrollment.completed_at`·`dropped_at`으로 계산한 응답 값이다 (4-3). 로그인 도착지의 "진행 중 코스"도 둘 다 비어 있는 수강이다.
- 운영자 확인은 `member.role = ADMIN` (FR-ADM-001).
- 회원 탈퇴(API-USR-16)는 5-3과 같다: `status = WITHDRAWN`, `withdrawn_at`, `email`·`password_hash` 비움, 닉네임 "탈퇴한 사용자", `social_account` 삭제, `portfolio` 삭제 표시. 같은 이메일로 다시 가입할 수 있다.
- 닉네임 중복(`DUPLICATE_NICKNAME`)은 `status = ACTIVE`인 회원끼리만 비교한다 (5-3).
- 삭제 API는 5-1과 같다: 글·댓글·포트폴리오는 `deleted_at`(삭제 표시), 스텝 체크 해제·추천·즐겨찾기·팔로우·북마크 취소는 행 삭제, 활동·문항·스텝은 `is_active = FALSE`.
- 포트폴리오를 지운 뒤 같은 코스로 다시 제출하면 삭제 표시한 행을 되살려 덮어쓴다(5-4). 그래서 API-PRT-04의 `ALREADY_SUBMITTED`는 삭제 표시가 없는 포트폴리오가 있을 때만 낸다.
- 8장 P2·P3 기능의 테이블(리뷰, 팔로우, 즐겨찾기, 북마크, 알림, 쪽지, 콘테스트, 이용 정지, 신고)은 모두 있다 (4-1).

---

## 10. 이 문서에서 정한 것

DB 설계서와 정책이 서로 열어 둔 곳을 아래처럼 정했다. 남은 미정은 없다.

| # | 항목 | 결정 | 근거 |
| --- | --- | --- | --- |
| 1 | 포트폴리오 스크린샷 개수 | **1장**. DB `portfolio.screenshot_url` 하나에 저장한다. 정책 PL-PRT-005는 "최대 3장"이라 1장만 받아도 정책을 어기지 않는다. JPG·PNG·WebP, 5MB | DB 설계서 4장, PL-PRT-005, PL-DAT-004 |
| 2 | `notification.target_type` 값 (DB 설계서 8장 "API 설계와 함께 정한다") | 아래 표의 7개. DB에 `ck_notification_target_type`으로 넣는다 | 화면 흐름 설계서 1-5 알림 도착지 |
| 3 | 0장 방식 (결정판 D-01~D-12) | 추천안으로 확정 | API 설계 결정판 |

`notification.target_type` (P2):

| notification_type | target_type | target_id | 누르면 |
| --- | --- | --- | --- |
| RECOMMEND | POST | `post_id` | 게시글 상세 |
| COMMENT · REPLY | COMMENT | `comment_id` | 그 댓글이 있는 게시글 상세 |
| REVIEW | REVIEW | `peer_review_id` | 리뷰가 달린 포트폴리오 상세 |
| MESSAGE | MESSAGE | `message_id` | 쪽지함 |
| BADGE | CONTEST_AWARD | `contest_award_id` | 마이페이지 배지 |
| NOTICE | NOTICE | `notice_id` | 공지 상세 |
| HIDDEN (P3) | `report.target_type`과 같은 값 (POST · PORTFOLIO · COMMENT · REVIEW · MESSAGE) | 숨겨진 대상 ID | 숨겨진 콘텐츠 화면 |

→ 값: `POST`, `COMMENT`, `PORTFOLIO`, `REVIEW`, `MESSAGE`, `CONTEST_AWARD`, `NOTICE`. `REVIEW`는 `report.target_type`과 같은 이름으로 `peer_review`를 가리킨다.

이 문서를 쓰는 곳:

- 형상관리자: 0장(방식·로그인), 7장(에러 코드) → 공통 응답·에러 포맷
- 코드 컨벤션(BE): 0-1 "저장 후 응답" 규칙 (폼 POST 성공은 redirect, 입력 오류는 같은 폼, `/api/**`는 JSON)
- DBA: 10장 2번 CHECK 값

## 변경 이력

- 2026-11-04 임시본 작성. 결정판 추천안 기준, P1 학습자 56개 + 운영자 38개 API, 에러 코드 초안, P2·P3 목록
- 2026-11-05 초안. 확정된 화면 흐름 설계서(2026-11-05)에 맞춤: 로그인 도착지, 회원가입 완료 상태, 토큰 없는 인증 화면, 비밀번호 변경 API 추가, 쪽지함 새 쪽지 추가, 포트폴리오 수정 위치, 비회원 접근 범위, 에러 코드 4개 추가(9-1)
- 2026-11-05 완성. DB 설계서(2026-11-05)와 대조: 요청·응답 값 이름을 물리 이름(camelCase)으로, 퀴즈 통과 기준을 `activity.pass_rate`로, 처리 내용을 테이블 이름으로 바꿈, 상태 값·삭제 정책·탈퇴 처리 확인(9-2). 0-1 저장 후 응답 규칙을 폼 POST·입력 오류·JSON으로 나눠 적음. 파일 이름을 `API 설계(2026-11-05).md`로 바꿔 확정문서로 옮겼다
- 2026-11-05 확정. 남은 미정을 닫음(10장): 스크린샷 1장(DB 기준, 정책 최대 3장 안), `notification.target_type` 값 7개, 0장은 결정판 추천안으로 확정
