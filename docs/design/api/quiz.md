# quiz API 명세 (퀴즈)

> 영역: quiz / API ID: API-LSN-03~06, API-LSN-10(P2)
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.
> P2(틀린 문제 모아보기)는 마일스톤 `4. P2 구현` · #171. `API-LSN-07~09`는 lesson 영역(lesson.md)이 쓴다.

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

## 틀린 문제 모아보기 (P2)

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-LSN-10 | 틀린 문제 모아보기 | GET | `/my/wrong-answers` | 회원 | query `course`(`courseId`, 선택), `lesson`(`lessonId`, 선택), `page`(1부터, _template.md 0장) | U-28 + 필터 칸(내가 틀린 문항이 있는 코스·수업 목록) + 틀린 문항 목록(최근에 틀린 순, 20개씩): 코스·수업 제목, 표시 유형, 문항, 코드, 내 답(보기 내용), 정답(보기 내용), 해설, 개념 태그 이름, 개념으로 가기 링크(`/courses/{courseId}/lessons/{lessonId}?activity={그 문항의 개념 태그가 붙은 같은 수업의 개념 활동}`, U-13. 그런 개념이 없으면 `?activity=` 없이 수업 화면) | 없는 `course`·`lesson`, 틀린 문항 없음은 빈 목록(화면이 안내 상자, 화면 흐름 5-4) | FR-LSN-008 |

- **무엇을 모으나:** 로그인 회원의 `quiz_answer` 중 `is_correct = false`인 답. 같은 문항을 여러 번 틀렸으면 **가장 최근 답 1개**만 보여준다. 나중에 다시 풀어 맞혔어도 목록에서 빼지 않는다 (아래 "이 명세에서 정한 것" 1).
- 답은 "확인"하면 고정되므로(PL-LSN-006) 제출하지 않은 풀이의 답도 모은다.
- 운영자가 나중에 고치거나 사용 중지한 문항(API-ADM-27·28)도 그대로 보여준다. 내 답은 예전 문항·보기를 가리키므로(DB 설계서 5-4) 그때 틀린 문항 그대로다.
- `lesson`만 오고 `course`가 없으면 그 수업으로만 거른다. 둘 다 오면 둘 다 만족하는 것만.
- 개념으로 가기가 가리키는 개념이 아직 잠겨 있으면(태그가 뒤쪽 개념에만 붙은 경우) U-13이 지금처럼 잠김 안내를 보여준다 (API-LSN-01 그대로, 이 API는 잠김을 따로 검사하지 않는다).
- 들어오는 길: U-13 "틀린 문제 모아보기"(`?course=&lesson=`를 붙여 그 수업으로 걸러 연다, 화면 흐름 U-13 "누르면").

## 영역 간 계약 (quiz 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/quiz/model/policy/`에 두고 quiz 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 quiz 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ⑤ | `LessonQuizScoreChecker` (lesson) | `BigDecimal getBestCorrectRate(Long memberId, Long activityId)` | 제출한 풀이 중 가장 높은 `correct_rate`. 없으면 `null` (API-LSN-01, PL-LSN-007) |

## 다른 영역에서 받는 읽기 값 (P2)

계약 표(①~⑬)에 넣지 않는 읽기 전용 값이라 받는 쪽 `<영역>Reader`에 메서드를 추가한다 ([08. 작업 순서](../../collaboration/08-task-flow.md) 규칙 1). 아래는 **제안**이고 구현(#190) 때 부르는 쪽(quiz)이 추가한다.

| Reader·메서드 | 쓰는 곳 |
| --- | --- |
| `LessonReader.getQuizActivityPlaces(Collection<Long> quizActivityIds)` → `Map<Long activityId, QuizActivityPlace>`, `QuizActivityPlace(Long courseId, Long lessonId)` (제안, 새로 추가) | 코스·수업 필터 (API-LSN-10) |
| `LessonReader.getConceptActivityIdsByTag(Collection<Long> lessonIds)` → `Map<Long lessonId, Map<Long conceptTagId, Long conceptActivityId>>` — 수업마다 **사용 중인 개념 활동**(`activity_type = CONCEPT`, `is_active = true`)의 태그(`activity_tag`)를 모아, 같은 태그가 여러 개념에 붙어 있으면 `sort_order`가 가장 앞인 개념. 사용 중지한 개념은 U-13에 보이지 않아(API-ADM-23) 고르지 않는다 (제안, 새로 추가. #284 "이 개념 다시 보기"와 같은 규칙) | 개념으로 가기 링크: 문항의 `concept_tag_id`로 찾는다 (API-LSN-10) |
| `LessonReader.getLessonTitles(Collection<Long> lessonIds)` (이미 있음) | 수업 제목 |
| `LessonReader.getConceptTagNames(Collection<Long> conceptTagIds)` (이미 있음) | 개념 태그 이름 |
| `CourseReader.getCourseTitles(Collection<Long> courseIds)` (course 영역, 이미 있음) | 코스 제목 |

## 이 명세에서 정한 것 (P2, 리뷰에서 확인)

요구사항·정책·화면에 없어 이 명세에서 정했다.

| # | 항목 | 결정 | 이유 |
| --- | --- | --- | --- |
| 1 | 다시 맞힌 문항 | 목록에서 빼지 않는다 | FR-LSN-008 "틀렸던 퀴즈 문제를 모아 다시 본다". 한 번 틀린 문항을 복습하는 목적 |
| 2 | 같은 문항을 여러 번 틀림 | 가장 최근 답 1줄만 | 같은 문항이 여러 번 나오면 목록이 길어지기만 한다 |
| 3 | 한 쪽 개수 | 20개씩 (`page`) | 게시판 목록(U-21)과 같은 수 |
| 4 | 개념으로 가기 | 문항의 개념 태그(`quiz_question.concept_tag_id`)가 붙은 같은 수업의 **사용 중인** 개념 활동(`activity_tag`)으로 간다. 여러 개면 순서가 가장 앞인 것, 없으면 수업 화면 | 화면 흐름 U-28 "문항 → U-13 해당 개념". 퀴즈의 위치(바로 앞 개념)로 정하면 운영자가 활동 순서를 바꿀 때(API-ADM-24) 다른 개념을 가리키게 된다. 태그로 찾으면 순서와 상관없고 테이블을 바꾸지 않아도 된다. #284(채점 결과 "이 개념 다시 보기")가 제안한 규칙과 같다 |
