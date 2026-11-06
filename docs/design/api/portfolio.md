# portfolio API 명세 (포트폴리오)

> 영역: portfolio / API ID: API-PRT-01~09
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

## 영역 간 계약 (portfolio 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/portfolio/model/policy/`에 두고 portfolio 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 portfolio 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑧ | `LessonPortfolioChecker` (lesson) | `boolean hasSubmitted(Long memberId, Long courseId)` | 삭제 표시(`deleted_at`) 없는 `portfolio`가 있음 (FR-ENR-002·004) |
| ⑨ | `CourseAssignmentChecker` (course) | `boolean hasAssignment(Long courseId)` | `portfolio_assignment`가 있음 (FR-CRS-006) |
| ⑩ | `MemberPortfolioCleaner` (auth) | `void deleteAllOf(Long memberId)` | 그 회원의 포트폴리오 전부 `deleted_at` 기록 (API-USR-16, DB 설계서 5-3) |
