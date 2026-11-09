# course API 명세 (트랙 · 코스 · 진단)

> 영역: course / API ID: API-CRS-01~03, API-DGN-01~04
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.
> P2(검색·카테고리별 탐색·진단 다시 하기)는 마일스톤 `4. P2 구현` · #171. 진단 설정(운영자, FR-DGN-004)은 admin.md API-ADM-41~45.

## 트랙 · 코스

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CRS-01 | 트랙 소개 | GET | `/tracks/{trackId}` | 전체 | path `trackId` | U-09 + 트랙(이름, 단계, 대상, 설명), 소속 공개 코스 카드(코스명, 한 줄 소개, 카테고리, 플랜 일수, 수업 수) | `TRACK_NOT_FOUND` | FR-CRS-004 |
| API-CRS-02 | 코스 목록 | GET | `/courses` | 전체 | query `track`, `category`(P2), `q`(P2) | U-10 + (진단 완료 회원) 추천 트랙과 그 코스 / 트랙별로 묶은 공개 코스 카드 / 결과 수 | 결과 없음은 빈 목록(화면이 안내 상자) | FR-CRS-001 |
| API-CRS-03 | 코스 상세 | GET | `/courses/{courseId}` | 전체 | path `courseId` | U-11 + 코스(트랙, 카테고리, 한 줄 소개, 대상, 목표, 플랜 일수, 소개), 공개 수업 목록(제목, 목표, 개념·퀴즈 수, 예상 소요 시간, 모의고사 있음), 포트폴리오 안내, 수강 상태(NONE / ENROLLED / COMPLETED) | `COURSE_NOT_FOUND`(없음·비공개) | FR-CRS-002 |
| API-CRS-02 (P2) | 코스 목록 — 카테고리·검색 | GET | (그대로) | 전체 | query `track`, `category`(`course_category_id`), `q`(검색어). 셋은 함께 쓸 수 있다(모두 만족하는 코스만) | API-CRS-02 응답 + 카테고리 칩(전체 + 모든 카테고리, `course_category.sort_order` 순) + 검색어. **`q`**: 앞뒤 공백을 지우고, 빈 값이면 검색하지 않는다. 공개 코스 중 코스 제목이나 그 코스의 공개 수업 제목에 `q`가 들어 있으면(부분 일치) 그 코스 카드를 보여주고, 수업 제목으로 걸린 코스 카드에는 걸린 수업 제목(`matchedLessonTitles`)을 함께 보여준다. `q`가 있으면 추천 트랙 블록은 보여주지 않는다 | 없는 `category`·결과 없음은 빈 목록(화면이 안내 상자 + 조건 초기화, 화면 흐름 5-4) | FR-CRS-003, 009 |


## 진단

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-DGN-01 | 진단 설문 | GET | `/diagnosis` | 회원 | - | U-07 + 활성 문항 전체(`diagnosisQuestionId`, `content`, 보기 `diagnosisOptionId`, `content`). 문항 전환은 화면 안에서 한다 | - | FR-DGN-001 |
| API-DGN-02 | 진단 제출 | POST | `/diagnosis` | 회원 | form `diagnosisOptionIds`(문항마다 1개) | 트랙 점수 합계 → 가장 높은 트랙(동점이면 트랙 순서 앞) → `diagnosis_result`(status = COMPLETED, `recommended_track_id`) 저장 → `/diagnosis/result` | `INVALID_INPUT`(문항 수와 답 수가 다름) | FR-DGN-002, PL-DGN-001·002 |
| API-DGN-03 | 진단 건너뛰기 | POST | `/diagnosis/skip` | 회원 | - (M-01 확인 후) | `diagnosis_result`(status = SKIPPED) 저장 → `/courses` | - | FR-DGN-003 |
| API-DGN-04 | 진단 결과 | GET | `/diagnosis/result` | 회원 | - | U-08 + 추천 트랙(이름, 단계, 설명), 추천 이유, 그 트랙의 공개 코스 카드, 다른 트랙 3개 요약 | 결과 없음·건너뜀 → `/diagnosis` | FR-DGN-002 |
| API-DGN-01 (P2) | 진단 다시 하기 — 설문 | GET | `/diagnosis` (그대로) | 회원 | - | U-24 "진단 다시 하기"(auth 영역 화면, 링크만)로 들어온다. 진단 기록이 있어도 막지 않고 같은 설문을 보여준다 | - | FR-DGN-005, PL-DGN-003 |
| API-DGN-02 (P2) | 진단 다시 하기 — 제출 | POST | `/diagnosis` (그대로) | 회원 | (그대로) | 지난 결과를 고치지 않고 `diagnosis_result`에 **새 행**을 저장한다. 코스 목록 추천·진단 결과는 가장 최근 행을 쓴다. 진행 중인 수강(`enrollment`)은 바꾸지 않는다 → `/diagnosis/result` | (그대로) | FR-DGN-005, PL-DGN-003 |
| API-DGN-03 (P2) | 진단 다시 하기 — 건너뛰기 | POST | `/diagnosis/skip` (그대로) | 회원 | - | 이미 진단 기록(`COMPLETED`·`SKIPPED`)이 있는 회원이 건너뛰면 **아무것도 저장하지 않는다**(지난 추천을 SKIPPED로 덮지 않게) → `/courses` | - | FR-DGN-005, PL-DGN-001 |

- 응답은 저장하지 않으므로(PL-DGN-001) 추천 이유(고른 보기 설명)는 API-DGN-02가 redirect할 때 flash로 넘긴 보기 목록으로 만든다. 나중에 다시 열면 트랙 설명만 보여준다.

## 영역 간 계약 (course 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/course/model/policy/`에 두고 course 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 course 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑫ | `LoginDiagnosisChecker` (auth) | `boolean hasDiagnosisRecord(Long memberId)` | `diagnosis_result`(`COMPLETED`·`SKIPPED`)가 있음 (API-USR-05) |

## 다른 영역에서 받는 읽기 값 (P2)

계약 표(①~⑬)에 넣지 않는 읽기 전용 값이라 받는 쪽 `<영역>Reader`에 메서드를 추가한다 ([08. 작업 순서](../../collaboration/08-task-flow.md) 규칙 1). 아래는 **제안**이고 구현(#189) 때 부르는 쪽(course)이 추가한다.

| Reader·메서드(제안) | 쓰는 곳 |
| --- | --- |
| `LessonReader.searchPublicLessonTitles(String q)` → `Map<Long courseId, List<String> lessonTitle>` — 공개 수업(`lesson.is_public = true`) 중 제목에 `q`가 들어 있는 것, 코스별로 `sort_order` 순. 코스가 공개인지는 course가 거른다 | 코스 목록 검색 (API-CRS-02 P2, FR-CRS-003) |

## 이 명세에서 정한 것 (P2, 리뷰에서 확인)

요구사항·정책·화면에 없어 이 명세에서 정했다.

| # | 항목 | 결정 | 이유 |
| --- | --- | --- | --- |
| 1 | 검색 결과 모양 | 새 화면 없이 U-10 코스 카드로 보여준다. 수업 제목으로 걸린 코스는 카드에 걸린 수업 제목을 함께 적는다 | 화면 흐름 설계서 "헤더 검색창은 별도 화면 없이 U-10의 `q`로 보낸다" |
| 2 | 검색 방식 | 앞뒤 공백 제거 후 부분 일치. 대소문자는 DB 정렬 규칙(utf8mb4_0900_ai_ci)대로 구분하지 않는다. 빈 검색어는 검색하지 않음 | 정책에 방식이 없음. 가장 단순한 방식 |
| 3 | 검색 중 추천 트랙 | `q`가 있으면 추천 트랙 블록을 숨긴다 (트랙·카테고리 필터만 있을 때는 그대로) | 검색 결과 위에 추천 코스가 섞이면 결과로 오해할 수 있음 |
| 4 | 카테고리 칩 | 공개 코스가 없는 카테고리도 칩으로 보여준다(누르면 빈 목록 안내) | 칩이 데이터에 따라 생겼다 사라지지 않게 |
| 5 | 다시 하기 중 건너뛰기 | 진단 기록이 이미 있으면 건너뛰기는 아무것도 저장하지 않는다 | PL-DGN-003 "언제든 다시 할 수 있다"가 지난 추천을 지우는 뜻은 아님. SKIPPED 새 행이 최근 행이 되면 추천이 사라진다 |
| 6 | 다시 하기 결과 저장 | 지난 행을 고치지 않고 새 행을 쌓는다(최근 행 기준으로 추천) | `diagnosis_result`는 회원당 여러 행이 가능(UNIQUE 없음). 확장 대시보드(API-ADM-40)의 진단 비율 집계와도 맞음 |
