# portfolio API 명세 (포트폴리오)

> 영역: portfolio / API ID: API-PRT-01~13 (10~13은 P2)
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-PRT-01 | 포트폴리오 과제 | GET | `/courses/{courseId}/portfolio` | 회원(수강 중) | path `courseId`, query `edit`(선택) | U-17 + 과제(주제, 학습 목표), 스텝 목록과 내 체크, 평가 에이전트 안내, 설정 파일 링크, 제출한 내 포트폴리오(있으면). `edit=true`면 제출 영역을 내 포트폴리오 값으로 채운 수정 상태. 열리지 않았으면 잠김 상태만 | 수강 안 함 → `/courses/{courseId}` | FR-PRT-001, 002, 006 |
| API-PRT-02 | 스텝 체크 | POST | `/api/guide-steps/{guideStepId}/check` | 조건(과제 열림) | path `guideStepId`, json `checked`(true·false) | 체크 저장·삭제 → JSON `checkedCount`, `totalCount` | `PORTFOLIO_LOCKED` | FR-PRT-001 |
| API-PRT-03 | 설정 파일 내려받기 | GET | `/courses/{courseId}/portfolio/agent-file` | 조건(과제 열림) | path `courseId` | 파일 응답(내려받기) | `PORTFOLIO_LOCKED`, `FILE_NOT_FOUND` | FR-PRT-002 |
| API-PRT-04 | 포트폴리오 제출 | POST | `/courses/{courseId}/portfolio` | 인증 + 조건(과제 열림) | form `title`, `resultUrl`, `summary`, `retrospective`, `aiTools`, `isPublic`, `agentResult`(선택), file `screenshot`(선택, 1장, [_template.md](_template.md) 10장 1번) | API 키 형식 문자열을 가려 저장(PL-PRT-008) → 완료 조건 확인 → `/courses/{courseId}/complete` | `INVALID_INPUT`(PL-PRT-002~004: 제목 50자, 요약 100자, 회고 100~2,000자, URL 형식) → 같은 폼 다시, `PORTFOLIO_LOCKED`, `ALREADY_SUBMITTED`(수정으로), `EMAIL_NOT_VERIFIED`, `BANNED_WORD`, `INVALID_FILE`(PL-PRT-005) | FR-PRT-003, 004 |
| API-PRT-05 | 내 포트폴리오 | GET | `/my/portfolios` | 회원 | - | U-19 + 카드(제목, 코스, 한 줄 요약, 공개 여부, 제출일). "수정"은 → `/courses/{courseId}/portfolio?edit=true` | - | FR-PRT-007 |
| API-PRT-06 | 포트폴리오 수정 | POST | `/portfolios/{portfolioId}/edit` | 인증(본인) | path `portfolioId`, form API-PRT-04와 같음 | 최신본으로 덮고 수정 일시 기록 → `/my/portfolios` + toast | `NOT_OWNER`, `INVALID_INPUT` → U-17 수정 상태 폼 다시, `BANNED_WORD`, `CONTEST_ENTERED`(콘테스트 출품 중 수정 불가, PL-PRT-007, P2) | FR-PRT-006, PL-PRT-007 |
| API-PRT-07 | 포트폴리오 삭제 | POST | `/portfolios/{portfolioId}/delete` | 회원(본인) | path `portfolioId` (M-04 확인 후) | 삭제 표시, 그 수강의 완료 일시 비움 → `/my/portfolios` + toast | `NOT_OWNER` | FR-PRT-006, PL-ENR-003 |
| API-PRT-08 | 공개 전환 | POST | `/api/portfolios/{portfolioId}/visibility` | 회원(본인) | path `portfolioId`, json `isPublic` | JSON `isPublic` | `NOT_OWNER` | FR-PRT-005 |
| API-PRT-09 | 포트폴리오 상세 | GET | `/portfolios/{portfolioId}` | 회원(공개만, 본인은 비공개도) | path `portfolioId` | U-20 + 제목, 결과물 링크, 한 줄 요약, 회고, AI 도구, 코스, 작성자, 제출일, 내 작품 여부 | 비회원 → `/login?redirect=` (FR-USR-006), `PRIVATE_PORTFOLIO`(본인 아님, "비공개된 작품입니다"), `PORTFOLIO_NOT_FOUND`(삭제) | FR-PRT-005 |

- 작성 중 내용 임시 저장(FR-PRT-010, P2)은 브라우저에 한다. 서버 API가 없다.
- 포트폴리오 제출은 "작성"이라 이메일 인증이 필요하다(PL-USR-003). 미인증 학습자는 제출 영역에 인증 안내를 보여준다.

## P2 — FAQ · 같은 코스 작품 보기 · 작성 중 내용 보존 · 콘테스트 출품 중 수정 막기 (#173) · 개인 작품 템플릿 (#168·#199)

> P2 · 마일스톤 `4. P2 구현`. 테이블 `portfolio_faq`(과제별 FAQ), `portfolio.portfolio_type`(COURSE·PERSONAL).

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-PRT-01 (P2) | 포트폴리오 과제 | GET | `/courses/{courseId}/portfolio` | 회원(수강 중) | (그대로) | U-17 4번 영역에 그 코스 과제의 FAQ(질문, 답) `sort_order` 순 추가. 과제가 열렸을 때만 보낸다(잠김이면 P1처럼 잠김 상태만). FAQ가 없으면 4번 영역을 그리지 않는다 | (그대로) | FR-PRT-008 |
| API-PRT-06 (P2) | 포트폴리오 수정 | POST | `/portfolios/{portfolioId}/edit` | 인증(본인) | (그대로) | (그대로) | P1 실패에 `CONTEST_ENTERED`(④) 추가: 콘테스트에 출품한 작품이면 수정하지 않는다. 출품 여부는 contest 영역 계약으로 묻는다 (아래 메모) | PL-PRT-007, PL-CTS-002 |
| API-PRT-10 | 같은 코스 작품 보기 | GET | `/courses/{courseId}/portfolios` | 회원(그 코스에 포트폴리오를 낸 사람) | path `courseId`, query `page`(1부터) | U-35 + 그 코스의 공개 포트폴리오 카드(제목, 한 줄 요약, 작성자, 제출일, 상세 링크 → `/portfolios/{portfolioId}`) 최근 제출순 20개씩. 삭제 표시한 것, 영구 정지 회원의 것(`MemberReader.findHiddenMemberIds`, PL-RPT-006)은 뺀다. 작성자 닉네임은 `MemberReader` | `PORTFOLIO_NOT_SUBMITTED`(삭제 표시 없는 내 포트폴리오가 그 코스에 없음) → ③ `/courses/{courseId}/portfolio` + toast, `COURSE_NOT_FOUND` | FR-PRT-009, PL-PRT-011 |
| API-PRT-11 | 과제 FAQ 저장 (운영자) | POST | `/admin/courses/{courseId}/portfolio/faq` | 운영자 | path `courseId`, form `faqs[i].portfolioFaqId`(새 항목은 비움), `faqs[i].question`, `faqs[i].answer` (보낸 순서 = `sort_order`) | 그 과제의 FAQ를 보낸 목록으로 맞춘다: 있는 ID는 고치고, 새 항목은 추가하고, 목록에 없는 기존 FAQ는 지운다 → `/admin/courses/{courseId}/portfolio?tab=faq` | `COURSE_NOT_FOUND`(`CourseReader`), `INVALID_INPUT`(질문 1~300자·답 필수, 다른 과제의 `portfolioFaqId`, 과제를 아직 저장하지 않음) → A-07 FAQ 탭 다시 | FR-PRT-014 |
| API-PRT-12 | 개인 작품 추가 화면 | GET | `/my/portfolios/new` | 인증 | query `template`(선택. 항목이 고정이라 템플릿이 하나뿐이어서 지금은 쓰지 않는다. 템플릿 관리 P3 #253에서 정한다) | U-34 + 고정 항목 5개(제목, 링크, 설명, 사용한 AI 도구, 회고)와 공개 여부 입력 양식. 이메일 미인증이면 양식 대신 인증 안내(PL-USR-003) | - | FR-PRT-011 |
| API-PRT-13 | 개인 작품 등록 | POST | `/my/portfolios` | 인증 | form `title`, `resultUrl`, `summary`(설명), `retrospective`, `aiTools`, `isPublic` | `portfolio` 저장(`portfolio_type` = PERSONAL, `course_id` 비움). 글 칸의 API 키 형식은 가린다(PL-PRT-008) → `/my/portfolios` + toast "개인 작품을 등록했습니다" | `INVALID_INPUT`(코스 포트폴리오와 같은 규칙: 제목 50자, 설명 100자, 회고 100~2,000자, 링크 URL 형식, 공개 여부 필수, PL-PRT-002~004·006) → U-34 같은 폼 다시, `EMAIL_NOT_VERIFIED`, `BANNED_WORD` | FR-PRT-011, PL-PRT-002~004·006·008 |

- **A-07 FAQ 탭 (FR-PRT-014):** API-ADM-34(admin.md, 코드는 portfolio 영역 #104)의 `tab`에 `faq`를 더한다. `tab=faq`면 A-07 FAQ 탭 + 그 과제의 FAQ 목록(`portfolioFaqId`, 질문, 답) `sort_order` 순. `portfolioFaqId`는 API-PRT-11로 기존 FAQ를 고칠 때 다시 보낸다. 과제(API-ADM-35)를 저장하기 전에는 FAQ를 저장할 수 없다 (`portfolio_faq`가 과제 ID를 가짐).
- FAQ는 운영자가 쓰는 안내 문구라 삭제 표시 없이 행을 지운다. 학습자 콘텐츠의 삭제 표시 규칙(PL-DAT-002)의 대상이 아니고, 테이블에도 `deleted_at`이 없다.
- **같은 코스 작품 보기 (FR-PRT-009):** 들어오는 링크는 U-18 코스 완료의 "같은 코스 작품 보기"다 (lesson 화면). 내 포트폴리오도 공개면 목록에 나온다. 개인 작품(`portfolio_type` = PERSONAL)은 코스가 없어 나오지 않는다.
- **작성 중 내용 보존 (FR-PRT-010):** 서버 API가 없다. U-17 제출 영역(새 제출·수정 상태)의 입력 값을 제출 버튼을 누를 때 브라우저 저장소에 회원·코스별로 7일 보관하고(PL-ERR-003·004), 제출 성공을 확인하면 지운다. 그래서 제출이 실패해도(서버 오류·네트워크 끊김) 작성 중 세션이 만료되어 로그인 화면으로 가도(PL-ERR-005) 내용이 남고, 다시 로그인해 U-17을 열면 채워 넣는다. 회원별로 나눠 같은 브라우저를 쓰는 다른 회원에게 채워지지 않게 하고, 보관하기 전에도 API 키 형식 문자열은 가린다(PL-PRT-008, 서버 저장과 같은 규칙). 스크린샷 파일은 보관하지 않는다.
- **콘테스트 출품 중 수정 막기 (PL-PRT-007, PL-CTS-002):** portfolio가 contest에 "이 포트폴리오가 출품 중인가"를 묻는 계약이 필요하다. 계약 이름·메서드·"출품 중"의 기간은 contest 명세(#174)에서 정하고, 그 전에는 `CONTEST_ENTERED` 검사를 붙이지 않는다. 출품한 작품의 삭제(API-PRT-07)·비공개 전환(API-PRT-08)을 막을지도 #174에서 정한다.
- **개인 작품 템플릿 (FR-PRT-011, U-34, #168 결정 A):** 템플릿 항목은 5개로 고정하고, 운영자가 항목을 바꾸는 템플릿 관리(FR-PRT-015, A-12)는 P3에서 정한다.
  - 항목은 코스 포트폴리오와 같은 칸에 저장한다: 제목 → `title`, 링크 → `result_url`, 설명 → `summary`(한 줄 요약), 사용한 AI 도구 → `ai_tools`, 회고 → `retrospective`. 그래서 U-19·U-20·공개 프로필이 그대로 보여 준다. 길이·형식·API 키 가리기·금칙어도 코스 포트폴리오와 같다.
  - 공개 여부는 기본값 없이 고른다 (PL-PRT-006). 에이전트 점검 결과·스크린샷은 코스 과제(평가 에이전트) 전용이라 받지 않는다. 개수 제한은 없다.
  - 등록 뒤 목록(U-19, API-PRT-05)·상세(U-20, API-PRT-09)·공개 전환(API-PRT-08)·삭제(API-PRT-07)는 기존 API를 그대로 쓴다. 코스가 없어 삭제해도 완료 일시 계약 ⑦은 부르지 않는다. 같은 코스 작품 보기(U-35)에는 나오지 않는다.
  - **수정은 이번 P2에서 하지 않는다.** 수정 화면은 코스 과제 화면(U-17)뿐이라, U-19·U-20의 "수정"은 개인 작품에 그리지 않는다.
  - 공개한 개인 작품은 콘테스트에 출품할 수 있다 (PL-CTS-002).

## 영역 간 계약 (portfolio 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/portfolio/model/policy/`에 두고 portfolio 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 portfolio 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑧ | `LessonPortfolioChecker` (lesson) | `boolean hasSubmitted(Long memberId, Long courseId)` | 삭제 표시(`deleted_at`) 없는 `portfolio`가 있음 (FR-ENR-002·004) |
| ⑨ | `CourseAssignmentChecker` (course) | `boolean hasAssignment(Long courseId)` | `portfolio_assignment`가 있음 (FR-CRS-006) |
| ⑩ | `MemberPortfolioCleaner` (auth) | `void deleteAllOf(Long memberId)` | 그 회원의 포트폴리오 전부 `deleted_at` 기록 (API-USR-16, DB 설계서 5-3) |
