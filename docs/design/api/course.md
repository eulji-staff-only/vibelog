# course API 명세 (트랙 · 코스 · 진단)

> 영역: course / API ID: API-CRS-01~03, API-DGN-01~04
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

## 트랙 · 코스

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CRS-01 | 트랙 소개 | GET | `/tracks/{trackId}` | 전체 | path `trackId` | U-09 + 트랙(이름, 단계, 대상, 설명), 소속 공개 코스 카드(코스명, 한 줄 소개, 카테고리, 플랜 일수, 수업 수) | `TRACK_NOT_FOUND` | FR-CRS-004 |
| API-CRS-02 | 코스 목록 | GET | `/courses` | 전체 | query `track`, `category`(P2), `q`(P2) | U-10 + (진단 완료 회원) 추천 트랙과 그 코스 / 트랙별로 묶은 공개 코스 카드 / 결과 수 | 결과 없음은 빈 목록(화면이 안내 상자) | FR-CRS-001 |
| API-CRS-03 | 코스 상세 | GET | `/courses/{courseId}` | 전체 | path `courseId` | U-11 + 코스(트랙, 카테고리, 한 줄 소개, 대상, 목표, 플랜 일수, 소개), 공개 수업 목록(제목, 목표, 개념·퀴즈 수, 예상 소요 시간, 모의고사 있음), 포트폴리오 안내, 수강 상태(NONE / ENROLLED / COMPLETED) | `COURSE_NOT_FOUND`(없음·비공개) | FR-CRS-002 |


## 진단

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-DGN-01 | 진단 설문 | GET | `/diagnosis` | 회원 | - | U-07 + 활성 문항 전체(`diagnosisQuestionId`, `content`, 보기 `diagnosisOptionId`, `content`). 문항 전환은 화면 안에서 한다 | - | FR-DGN-001 |
| API-DGN-02 | 진단 제출 | POST | `/diagnosis` | 회원 | form `diagnosisOptionIds`(문항마다 1개) | 트랙 점수 합계 → 가장 높은 트랙(동점이면 트랙 순서 앞) → `diagnosis_result`(status = COMPLETED, `recommended_track_id`) 저장 → `/diagnosis/result` | `INVALID_INPUT`(문항 수와 답 수가 다름) | FR-DGN-002, PL-DGN-001·002 |
| API-DGN-03 | 진단 건너뛰기 | POST | `/diagnosis/skip` | 회원 | - (M-01 확인 후) | `diagnosis_result`(status = SKIPPED) 저장 → `/courses` | - | FR-DGN-003 |
| API-DGN-04 | 진단 결과 | GET | `/diagnosis/result` | 회원 | - | U-08 + 추천 트랙(이름, 단계, 설명), 추천 이유, 그 트랙의 공개 코스 카드, 다른 트랙 3개 요약 | 결과 없음·건너뜀 → `/diagnosis` | FR-DGN-002 |

- 응답은 저장하지 않으므로(PL-DGN-001) 추천 이유(고른 보기 설명)는 API-DGN-02가 redirect할 때 flash로 넘긴 보기 목록으로 만든다. 나중에 다시 열면 트랙 설명만 보여준다.

## 영역 간 계약 (course 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/course/model/policy/`에 두고 course 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 course 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑫ | `LoginDiagnosisChecker` (auth) | `boolean hasDiagnosisRecord(Long memberId)` | `diagnosis_result`(`COMPLETED`·`SKIPPED`)가 있음 (API-USR-05) |
