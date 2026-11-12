# exam API 명세 (모의고사)

> 영역: exam / API ID: API-EXM-01~05
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

## 모의고사

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-EXM-01 | 응시 시작·이어 풀기 | POST | `/lessons/{lessonId}/exam/attempts` | 조건(모의고사 열림) | path `lessonId` | 제출 안 한 응시가 있으면 그것을, 없으면 새 회차를 만든다 → `/lessons/{lessonId}/exam` | `EXAM_LOCKED` → `/my/learning` + toast "이 수업의 마지막 퀴즈를 통과하면 열립니다", `REPORT_NOT_VIEWED`(직전 리포트를 보기 전 재응시, PL-EXM-003) | FR-EXM-001, 002 |
| API-EXM-02 | 모의고사 화면 | GET | `/lessons/{lessonId}/exam` | 조건 | path `lessonId` | U-15 + 수업 제목, 회차, 문항 전체(`examQuestionId`, `content`, 보기), 저장된 내 답(이어 풀기) | 진행 중 응시 없음 → 수업 화면의 모의고사 항목 | FR-EXM-002, PL-EXM-006 |
| API-EXM-03 | 답 저장 | POST | `/api/exam-attempts/{examAttemptId}/answers` | 조건(본인 응시) | path `examAttemptId`, json `examQuestionId`, `examChoiceId`(비우면 선택 취소) | `exam_answer` 저장 → JSON | `ATTEMPT_SUBMITTED`, `NOT_OWNER` | PL-EXM-006, D-10 |
| API-EXM-04 | 제출·채점 | POST | `/lessons/{lessonId}/exam/submit` | 조건(본인 응시) | form `examAttemptId`, `answers`(마지막 상태) | 남은 답 저장 → 채점(`exam_attempt.score`·`correct_rate`·`submitted_at` 기록) → `/lessons/{lessonId}/exam/report?attempt={회차}` | `ATTEMPT_SUBMITTED` | FR-EXM-002 |
| API-EXM-05 | 오답 분석 리포트 | GET | `/lessons/{lessonId}/exam/report` | 조건(본인) | query `attempt`(= `attempt_no`, 선택, 없으면 최근 제출) | 처음 열면 `report_viewed_at` 기록 + 수업 완료 + 다음 수업 열림(마지막 수업이면 포트폴리오 열림) → U-16 + 점수(정답 수 / 문항 수), 개념별 정답률·약한 부분, 틀린 문항(문항, 내 답, 정답, 해설, 연관 개념 링크), 다음 행동(다음 수업 / 포트폴리오) | `EXAM_NOT_SUBMITTED` → `/lessons/{lessonId}/exam`, `COURSE_NOT_ENROLLED`(수강을 포기했거나 수강 중이 아님, 리포트 기록·수업 완료는 하지 않음) → `/courses/{courseId}` + toast "수강 중인 코스가 아닙니다. 코스 상세에서 다시 시작해 주세요." (#439) | FR-EXM-003, 004, PL-EXM-007, PL-ENR-004 |

- 제한 시간 없음(PL-EXM-005), 통과 점수 없음(PL-EXM-001). 다시 응시는 API-EXM-01을 다시 부른다.
- API-EXM-05는 GET이지만 기록을 남긴다. 명세가 "리포트 화면을 열면 확인 처리"로 정했다(PQ22). 같은 응시를 다시 열면 기록하지 않는다.

## P2 — 평균 점수 비교 · 리포트 다시 보기 (#173)

> P2 · 마일스톤 `4. P2 구현`. 새 테이블·새 API 없이 API-EXM-05 응답에 더한다 (_template.md 8장).

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-EXM-05 (P2) | 오답 분석 리포트 | GET | `/lessons/{lessonId}/exam/report` | 조건(본인) | (그대로) | U-16에 **같은 수업 응시자 평균 점수** 추가: 학습자마다 그 수업 **1회차**(`attempt_no` = 1)를 제출한 점수(`score`)만 모아 평균, 소수 첫째 자리까지. 운영자(`role` = ADMIN)·테스트 계정(`is_test`)은 뺀다(`MemberReader`). 넣은 응시자가 5명 미만이면 평균을 보내지 않는다(화면에서 숨김). 내 점수는 P1처럼 이 회차 점수 그대로 | (그대로) | FR-EXM-005, PL-EXM-004, PL-EXM-008 |

- **평균 점수 (FR-EXM-005):** 다른 학습자의 개별 점수·순위는 보내지 않고 평균 하나만 보낸다 (PL-EXM-008, PL-DAT-001). 내 1회차 점수도 평균에 들어간다. 저장하지 않고 리포트를 열 때 계산한다.
- 운영자·테스트 계정을 빼려면 `MemberReader`에 읽기 전용 메서드를 더한다 (회원 ID 묶음 → 통계에서 뺄 회원 ID, 08 문서 규칙 1).
- **리포트 다시 보기 (FR-EXM-006):** 새 API 없이 API-EXM-05에 `attempt`(회차)를 넣어 연다. 이미 연 리포트는 다시 기록하지 않는다 (P1 규칙 그대로). 들어오는 링크는 lesson 영역이 계약 ④로 회차를 받아 만든다: U-12 내 학습·U-18 코스 완료는 P1(API-ENR-02·03), U-13 수업 화면 "지난 리포트 다시 보기"는 lesson P2 명세(#172).

## 영역 간 계약 (exam 영역이 받는 것)

[08. 작업 순서](../../collaboration/08-task-flow.md)의 계약 표 번호입니다. 인터페이스는 `domain/exam/model/policy/`에 두고 exam 영역이 구현합니다. DTO는 인터페이스와 같은 패키지의 `record`입니다. 없는 ID가 오면 exam 영역이 자기 ErrorCode로 예외를 던집니다.

| # | 인터페이스 (부르는 쪽) | 메서드 | 하는 일 |
| --- | --- | --- | --- |
| ④ | `LessonExamStatusChecker` (lesson) | `Map<Long, Integer> getLatestSubmittedAttemptNos(Long memberId, Collection<Long> lessonIds)` | 수업별로 마지막에 제출한 응시의 `attempt_no`. 제출한 응시가 없는 수업은 빠진다 (API-ENR-02 "응시함", API-ENR-03 리포트 링크) |
