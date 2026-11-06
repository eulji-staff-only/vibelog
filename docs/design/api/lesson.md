# lesson API 명세 (수강 · 수업 · 개념)

> 영역: lesson / API ID: API-ENR-01~03, API-LSN-01~02
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

## 수강 · 내 학습 · 코스 완료

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-ENR-01 | 수강 시작 | POST | `/courses/{courseId}/enroll` | 회원 | path `courseId` | `enrollment` 생성(`started_at`), 공개 수업마다 `lesson_progress`, 첫 수업·첫 개념 `unlocked_at` 기록 → `/my/learning`. 이미 수강 중이면 그대로 → `/my/learning` | 비회원 → `/login?redirect=/courses/{courseId}`, `COURSE_NOT_FOUND` | FR-ENR-001 |
| API-ENR-02 | 내 학습 | GET | `/my/learning` | 회원 | query `tab`(active, completed) | U-12 + 코스 카드(트랙, 코스명, 진행률, 다음에 할 일), 수업별 상태(잠김·열림·완료), 수업별 모의고사 상태(잠김·열림·응시함·리포트 확인), 포트폴리오 상태, 남은 완료 조건 | - | FR-ENR-002, 003 |
| API-ENR-03 | 코스 완료 | GET | `/courses/{courseId}/complete` | 조건(수강 중) | path `courseId` | U-18 + 완료 조건 3개(완료·남음), 완료 일시, 수업별 리포트 목록. 조건이 남아도 화면은 보여준다("한 걸음 남았어요") | 수강 안 함 → `/courses/{courseId}` | FR-ENR-004 |

- 진행률 = 리포트까지 확인한 수업 수 ÷ 공개 수업 수. 저장하지 않고 응답할 때 계산한다 (PL-ENR-006).

## 수업 · 개념

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-LSN-01 | 수업 화면 | GET | `/courses/{courseId}/lessons/{lessonId}` | 조건(수업 열림) | path `courseId`, `lessonId`, query `activity`(선택, 없으면 진행 중 활동) | U-13 + 활동 목록(개념·퀴즈·모의고사, 각 완료·진행 중·잠김), 선택한 활동: 개념이면 제목·본문·본문 형식·개념 태그·도구 노트 링크 / 퀴즈면 안내와 최고 점수 / 모의고사면 안내. 잠긴 활동을 고르면 잠김 안내와 앞 활동 링크 | `LESSON_LOCKED` → `/my/learning` + toast "이전 수업의 모의고사를 보고 리포트를 확인하면 열립니다", 수강 안 함 → `/courses/{courseId}` | FR-LSN-001, 002, 009 |
| API-LSN-02 | 개념 읽기 완료 | POST | `/courses/{courseId}/lessons/{lessonId}/activities/{activityId}/complete` | 조건(활동 열림) | path 3개 | 개념 완료 일시 기록(처음 한 번), 바로 뒤 퀴즈 열림 → `/courses/{courseId}/lessons/{lessonId}/quizzes/{뒤 퀴즈 activityId}` | `ACTIVITY_LOCKED` | FR-LSN-002 |

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
| ⑪ | 〃 | `String getEnrollmentStatus(Long memberId, Long courseId)` | `NONE`·`ENROLLED`·`COMPLETED` (`enrollment`·`completed_at`으로 계산, API-CRS-03) |
| ⑪ | 〃 | `List<LessonSummary> getPublicLessons(Long courseId)` | `LessonSummary(Long lessonId, String title, String goal, int conceptCount, int quizCount, int estimatedMinutes)`, `sort_order` 순 (API-CRS-03). 공개 수업은 모두 모의고사가 있다 (API-ADM-33) |
| ⑪ | 〃 | `Map<Long, Integer> countPublicLessons(Collection<Long> courseIds)` | 코스 카드의 수업 수 (API-CRS-01·02) |
| ⑫ | `LoginEnrollmentChecker` (auth) | `boolean hasActiveEnrollment(Long memberId)` | `completed_at`·`dropped_at`이 모두 비어 있는 수강이 있음 (API-USR-05·07) |
