# quiz API 명세 (퀴즈)

> 영역: quiz / API ID: API-LSN-03~06
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

## 퀴즈

문항마다 "확인"하면 정답·해설이 나오고 답이 고정된다. 마지막에 한 번 제출해 채점한다 (PL-LSN-006). 그래서 퀴즈 화면 안의 동작은 JSON이다.

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-LSN-03 | 퀴즈 화면 | GET | `/courses/{courseId}/lessons/{lessonId}/quizzes/{activityId}` | 조건(활동 열림) | path 3개 | U-14 틀 + 퀴즈 제목, 문항 수, 최고 점수(PL-LSN-007) | `ACTIVITY_LOCKED` → 수업 화면 + 잠김 안내 | FR-LSN-006 |
| API-LSN-04 | 풀이 시작 | POST | `/api/quizzes/{activityId}/attempts` | 조건 | path `activityId` | JSON `quizAttemptId`, `questions[]`(`quizQuestionId`, `displayType`, `content`, `codeSnippet`, `choices[]`(`quizChoiceId`, `content`)). 정답은 넣지 않는다. 다시 풀면 문항·보기 순서를 섞는다 | `ACTIVITY_LOCKED` | FR-LSN-006, PL-LSN-004·005 |
| API-LSN-05 | 문항 확인 | POST | `/api/quiz-attempts/{quizAttemptId}/answers` | 조건(본인 풀이) | path `quizAttemptId`, json `quizQuestionId`, `quizChoiceId` | `quiz_answer` 저장 → JSON `correct`, `correctQuizChoiceId`, `explanation` | `ALREADY_CHECKED`(답 고정), `ATTEMPT_SUBMITTED`, `NOT_OWNER` | FR-LSN-006, PL-LSN-006 |
| API-LSN-06 | 제출·채점 | POST | `/api/quiz-attempts/{quizAttemptId}/submit` | 조건(본인 풀이) | path `quizAttemptId` | 채점(정답률 ≥ `activity.pass_rate`(기본 50)면 통과, `quiz_attempt.correct_rate`·`is_passed`·`submitted_at` 기록) → 통과면 퀴즈 완료·다음 개념 열림(마지막 퀴즈면 모의고사 열림) → JSON `correctRate`, `isPassed`, `tagStats[]`(`conceptTagName`, `wrongRate`, `weak`), `next`(`type`: CONCEPT·EXAM·NONE, `url`), `message`("다음 개념이 열렸습니다" 등) | `QUIZ_NOT_FINISHED`(확인 안 한 문항 있음), `ATTEMPT_SUBMITTED` | FR-LSN-006, 007, PL-LSN-002·003·008 |

- 약한 개념 = 개념 태그별 정답률 60% 미만 (PL-LSN-008).
- 문항 표시 유형 값은 DB 설계서 4-3을 따른다 (`quiz_question.display_type`): `CODE_BLANK`, `CONCEPT`, `OUTPUT_PREDICT`, `TRICK`.

## 영역 간 계약 (quiz 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/quiz/model/policy/`에 두고 quiz 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 quiz 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑤ | `LessonQuizScoreChecker` (lesson) | `BigDecimal getBestCorrectRate(Long memberId, Long activityId)` | 제출한 풀이 중 가장 높은 `correct_rate`. 없으면 `null` (API-LSN-01, PL-LSN-007) |
