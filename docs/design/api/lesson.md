# lesson API 명세 (수강 · 수업 · 개념)

> 영역: lesson / API ID: API-ENR-01~04, API-LSN-01~02, 07~09
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.
> P2(수강 포기·연관 개념·북마크·태그 검색·운영자 미리보기)는 마일스톤 `4. P2 구현` · #172. `API-LSN-03~06`은 quiz 영역이 씁니다(quiz.md).

## 수강 · 내 학습 · 코스 완료

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-ENR-01 | 수강 시작 | POST | `/courses/{courseId}/enroll` | 회원 | path `courseId` | `enrollment` 생성(`started_at`), 공개 수업마다 `lesson_progress`, 첫 수업·첫 개념 `unlocked_at` 기록 → `/my/learning`. 이미 수강 중이면 그대로 → `/my/learning` | 비회원 → `/login?redirect=/courses/{courseId}`, `COURSE_NOT_FOUND` | FR-ENR-001 |
| API-ENR-02 | 내 학습 | GET | `/my/learning` | 회원 | query `tab`(active, completed) | U-12 + 코스 카드(트랙, 코스명, 진행률, 다음에 할 일), 수업별 상태(잠김·열림·완료), 수업별 모의고사 상태(잠김·열림·응시함·리포트 확인), 포트폴리오 상태, 남은 완료 조건 | - | FR-ENR-002, 003 |
| API-ENR-03 | 코스 완료 | GET | `/courses/{courseId}/complete` | 조건(수강 중) | path `courseId` | U-18 + 완료 조건 3개(완료·남음), 완료 일시, 수업별 리포트 목록. 조건이 남아도 화면은 보여준다("한 걸음 남았어요") | 수강 안 함 → `/courses/{courseId}` | FR-ENR-004 |
| API-ENR-01 (P2) | 수강 시작 — 재시작 | POST | (그대로) | 회원 | path `courseId` | 포기한 적 있는 코스면(`dropped_at`이 있음) `dropped_at`을 비운다. 기존 `lesson_progress`·`activity_progress`(열림·완료 기록)는 그대로 둔다(PL-ENR-004 "이전 진행 상태에서 이어서") → `/my/learning` + toast "이어서 학습합니다" | (API-ENR-01과 같음) | FR-ENR-005, PL-ENR-004 |
| API-ENR-04 | 수강 포기 | POST | `/courses/{courseId}/drop` | 조건(수강 중, M-12 확인 후) | path `courseId` | `enrollment.dropped_at` 기록(처음) → `/my/learning` + toast "수강을 포기했습니다" (내 학습 목록에서 내려감) | `ENROLLMENT_NOT_FOUND`(수강 안 함·이미 포기·이미 완료) | FR-ENR-005, PL-ENR-004 |

- 진행률 = 리포트까지 확인한 수업 수 ÷ 공개 수업 수. 저장하지 않고 응답할 때 계산한다 (PL-ENR-006).
- **수강 포기 (P2):** `/my/learning`의 "수강 중" 탭에만 포기 버튼을 둔다(완료한 코스는 포기 대상이 아니다, `completed_at`이 없고 `dropped_at`도 없을 때만 포기할 수 있다). 포기해도 `lesson_progress`·`activity_progress`·`quiz_attempt`·`exam_attempt` 기록은 지우지 않는다(PL-ENR-004 "포기하면 내 학습에서 내린다"). 다시 수강 시작(API-ENR-01)을 누르면 이어서 한다. 포기한 수강은 수강 안 함으로 보므로 수업 화면(API-LSN-01)은 `/courses/{courseId}`로 돌려보낸다(계약 ⑪ `NONE`과 같다). 퀴즈·모의고사 화면의 처리는 정하지 않았다.

## 수업 · 개념

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-LSN-01 | 수업 화면 | GET | `/courses/{courseId}/lessons/{lessonId}` | 조건(수업 열림) | path `courseId`, `lessonId`, query `activity`(선택, 없으면 진행 중 활동) | U-13 + 활동 목록(개념·퀴즈·모의고사, 각 완료·진행 중·잠김), 선택한 활동: 개념이면 제목·본문·본문 형식·개념 태그·도구 노트 링크 / 퀴즈면 안내와 최고 점수 / 모의고사면 안내. 잠긴 활동을 고르면 잠김 안내와 앞 활동 링크 | `LESSON_LOCKED` → `/my/learning` + toast "이전 수업의 모의고사를 보고 리포트를 확인하면 열립니다", 수강 안 함(포기한 수강 포함) → `/courses/{courseId}` | FR-LSN-001, 002, 009 |
| API-LSN-02 | 개념 읽기 완료 | POST | `/courses/{courseId}/lessons/{lessonId}/activities/{activityId}/complete` | 조건(활동 열림) | path 3개 | 개념 완료 일시 기록(처음 한 번), 바로 뒤 퀴즈 열림 → `/courses/{courseId}/lessons/{lessonId}/quizzes/{뒤 퀴즈 activityId}` | `ACTIVITY_LOCKED` | FR-LSN-002 |
| API-LSN-07 | 연관 개념 보기 | - | (API-LSN-01 응답에 추가) | 조건(수업 열림) | path 3개(API-LSN-01과 같음) | 선택한 활동이 개념이면 응답에 연관 개념 목록(`concept_relation.sort_order` 순, 각각 `relatedActivityId`, 제목, `courseId`, `lessonId`) 추가. U-13 개념 본문 끝에 표시, 누르면 그 개념의 `/courses/{courseId}/lessons/{lessonId}?activity={relatedActivityId}` (다른 코스 개념이면 그 코스 URL, 잠겨 있으면 API-LSN-01의 잠김 안내를 그대로 본다) | (API-LSN-01과 같음) | FR-LSN-003 |
| API-LSN-08 | 개념 북마크 토글 | POST | `/api/concepts/{activityId}/bookmark` | 회원 | path `activityId` | `concept_bookmark` 있으면 지우고 없으면 만듦(`uk_concept_bookmark_member_activity`) → JSON `{"bookmarked": true\|false}` | `ACTIVITY_NOT_FOUND` | FR-LSN-004 |
| API-LSN-09 | 개념 태그 검색 | GET | `/api/concepts?conceptTagId={conceptTagId}` | 회원 | query `conceptTagId` | JSON `[{"activityId", "title", "courseId", "lessonId", "courseTitle", "lessonTitle"}]` — 그 태그가 달린 개념 활동 전체(수업이 공개이고 그 수업이 속한 코스도 공개인 것만, `sort_order` 순) | `conceptTagId`가 없으면 빈 목록 | FR-LSN-005 |

- **연관 개념 (P2, FR-LSN-003):** `concept_relation`은 운영자 입력 화면을 만들지 않고 `sql/02-seed.sql` 샘플 데이터로 채운다 (A-06에 입력 UI를 추가하면 #100·#101 범위가 커져서, 이번 명세에서는 샘플 데이터로 정한다 — 운영자 등록이 필요해지면 별도 이슈). **DB 팀에 `concept_relation` 샘플 데이터 추가를 요청해야 한다** (이 PR은 `sql/`을 건드리지 않는다).
- **연관 개념의 공개 기준 (P2, #192):** 연관 개념 목록(API-LSN-07)도 개념 태그 검색(API-LSN-09)과 같은 기준으로 거른다 - 사용 중인 개념이고, 그 수업이 공개이며 그 수업이 속한 코스도 공개인 것만 보여준다.
- **개념 북마크 (P2, FR-LSN-004):** U-13의 북마크 버튼은 이 토글 하나로 켜고 끈다(멱등 아님, 누를 때마다 반대로 바뀜). 목록은 U-24 "북마크" 탭(`tab=bookmarks`)에서 auth 영역이 `LessonReader`로 가져간다(아래 "다른 영역에 주는 읽기 값").
- **개념 태그 검색 (P2, FR-LSN-005):** 화면 흐름 설계서에 태그 검색의 화면 위치가 없어 이 명세에서 정한다. U-13이 이미 보여주는 "개념 태그" 칩(API-LSN-01 응답에 `conceptTagId`가 있어야 클릭할 수 있다 — 기존 응답에 태그 이름만 있었다면 `conceptTagId`를 추가)을 누르면 같은 화면에서 모달로 검색 결과(API-LSN-09)를 보여주고, 결과를 고르면 그 개념의 수업 화면으로 이동한다. 새 화면을 만들지 않고 기존 U-13에 끼워 넣는 쪽을 골랐다(완전히 새 화면을 만들 만큼 독립적인 기능은 아니라고 판단). 자유 텍스트 검색창은 두지 않는다(태그 칩 클릭만). **검색 조건은 `activity.lesson_id`가 가리키는 수업의 `lesson.is_public = true`뿐 아니라 그 수업이 속한 `course.is_public = true`까지 함께 걸러야 한다** (수업만 공개이고 코스가 비공개인 경우까지 제외해, 비공개 코스 제목이 검색 결과로 새지 않게 한다).
- **운영자 미리보기 (P2, FR-LSN-015):** 운영자(`role = ADMIN`)가 `/courses/{courseId}/lessons/{lessonId}`(API-LSN-01)를 열면 수강 여부·잠김 여부·수업 공개 여부를 검사하지 않고 그대로 보여준다(비공개 수업도 볼 수 있어야 공개 전에 확인할 수 있다). 조회만 하고 `activity_progress`·`lesson_progress`를 기록하지 않는다(API-LSN-02를 호출하지 않는 한 그대로). admin.md API-ADM-39가 이 주소를 새 탭으로 여는 버튼을 설명한다.

## 다른 영역에 주는 읽기 값 (`LessonReader`, P2)

| 메서드 | 쓰는 곳 |
| --- | --- |
| `Page<BookmarkedConceptSummary> findBookmarkedConcepts(Long memberId, int page)` — `BookmarkedConceptSummary(Long courseId, Long lessonId, Long activityId, String title, String courseTitle, String lessonTitle, LocalDateTime bookmarkedAt)`, 북마크한 시각 내림차순 20개씩 | 마이페이지 북마크 탭 (auth API-USR-15 P2, #192) |

## 영역 간 계약 (lesson 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/lesson/model/policy/`에 두고 lesson 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 lesson 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ① | `QuizActivityProgress` (quiz) | `QuizActivityInfo getQuizActivity(Long memberId, Long activityId)` | `QuizActivityInfo(Long courseId, Long lessonId, boolean unlocked, int passRate)`. `unlocked` = 수강 중이고 그 활동의 `activity_progress.unlocked_at`이 있음, `passRate` = `activity.pass_rate` (API-LSN-03~06) |
| ① | 〃 | `NextStep recordQuizPassed(Long memberId, Long activityId)` | 퀴즈 활동 `completed_at`(처음 한 번), 다음 개념 `unlocked_at` 기록. `NextStep(String type, Long courseId, Long lessonId, Long activityId)`, `type` = `CONCEPT`·`EXAM`·`NONE` (API-LSN-06 `next`). `EXAM`·`NONE`이면 `activityId`는 `null` |
| ② | `ExamLessonProgress` (exam) | `boolean isExamOpen(Long memberId, Long lessonId)` | 그 수업 마지막 퀴즈 활동의 `completed_at`이 있음 (FR-EXM-001) |
| ③ | 〃 | `ReportNextStep recordReportViewed(Long memberId, Long lessonId)` | `lesson_progress.completed_at`, 다음 수업과 그 첫 개념 `unlocked_at`(API-ENR-01과 같음), 마지막 수업이면 `enrollment.portfolio_unlocked_at` 기록. 이미 기록돼 있으면 기록하지 않고 다음 행동만 돌려준다. `ReportNextStep(String type, Long courseId, Long nextLessonId)`, `type` = `NEXT_LESSON`·`PORTFOLIO` (API-EXM-05) |
| ⑥ | `PortfolioEnrollmentProgress` (portfolio) | `PortfolioAccess getAccess(Long memberId, Long courseId)` | `PortfolioAccess(boolean enrolled, boolean unlocked)`. `unlocked` = `enrollment.portfolio_unlocked_at`이 있음 (API-PRT-01~04) |
| ⑦ | 〃 | `void recordSubmitted(Long memberId, Long courseId)` | 모든 공개 수업의 `lesson_progress.completed_at`이 있으면 `enrollment.completed_at` 기록 (처음 한 번, FR-ENR-004) |
| ⑦ | 〃 | `void recordDeleted(Long memberId, Long courseId)` | `enrollment.completed_at` 비움 (API-PRT-07, PL-ENR-003) |
| ⑨ | `CourseLessonInfo` (course) | `boolean hasPublicLesson(Long courseId)` | 공개 수업이 하나 이상 있음 (FR-CRS-006) |
| ⑪ | 〃 | `String getEnrollmentStatus(Long memberId, Long courseId)` | `NONE`·`ENROLLED`·`COMPLETED` (`enrollment`·`completed_at`으로 계산, `dropped_at`이 있으면 `NONE`을 돌려준다(P2) — 포기한 코스는 코스 상세에서 "시작하기"로 보이고, API-ENR-01을 다시 부르면 이어서 한다, API-CRS-03) |
| ⑪ | 〃 | `List<LessonSummary> getPublicLessons(Long courseId)` | `LessonSummary(Long lessonId, String title, String goal, int conceptCount, int quizCount, int estimatedMinutes)`, `sort_order` 순 (API-CRS-03). 공개 수업은 모두 모의고사가 있다 (API-ADM-33) |
| ⑪ | 〃 | `Map<Long, Integer> countPublicLessons(Collection<Long> courseIds)` | 코스 카드의 수업 수 (API-CRS-01·02) |
| ⑫ | `LoginEnrollmentChecker` (auth) | `boolean hasActiveEnrollment(Long memberId)` | `completed_at`·`dropped_at`이 모두 비어 있는 수강이 있음 (API-USR-05·07) |

## 이 명세에서 정한 것 (P2, 리뷰에서 확인)

요구사항·정책·화면에 없어 이 명세에서 정했다.

| # | 항목 | 결정 | 이유 |
| --- | --- | --- | --- |
| 1 | 수강 포기 대상 | 완료하지 않은 수강(`completed_at` 없음)만 포기할 수 있다 | PL-ENR-004는 "완료한 코스는 다시 열어 복습"이라 포기와 별개 |
| 2 | 재시작 때 `started_at` | 그대로 둔다(새로고침하지 않음) | 정책에 언급 없음, 진행 기록을 그대로 유지하는 쪽과 맞춤 |
| 3 | 연관 개념 데이터 입력 | 운영자 입력 화면을 만들지 않고 `sql/02-seed.sql` 샘플 데이터로 채운다 | A-06 입력 UI는 #100·#101 범위를 늘림. 필요해지면 별도 이슈 |
| 4 | 개념 태그 검색 위치 | 새 화면 대신 U-13의 개념 태그 칩 클릭 → 모달 | 화면 흐름 설계서에 위치가 없음. 기존 화면에 끼워 넣는 쪽이 더 가볍다고 판단 |
| 5 | 북마크 토글 모양 | POST 토글 하나(켜기·끄기 API 분리 안 함) | `_template.md` 8장이 이미 이 모양을 제안함 |
| 6 | 운영자 미리보기 | 학습자와 같은 URL(API-LSN-01)을 운영자 권한으로 검사 없이 연다 | 화면 흐름 설계서 "학습자 수업 화면을 새 탭으로 연다"가 같은 URL을 가리킴 |

> `concept_relation` 샘플 데이터는 이 PR에서 `sql/`을 고치지 않는다 (DB 팀 `Choyeonik` 담당). 별도로 "다른 팀 요청서"를 보낸다.
