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
| API-EXM-05 | 오답 분석 리포트 | GET | `/lessons/{lessonId}/exam/report` | 조건(본인) | query `attempt`(= `attempt_no`, 선택, 없으면 최근 제출) | 처음 열면 `report_viewed_at` 기록 + 수업 완료 + 다음 수업 열림(마지막 수업이면 포트폴리오 열림) → U-16 + 점수(정답 수 / 문항 수), 개념별 정답률·약한 부분, 틀린 문항(문항, 내 답, 정답, 해설, 연관 개념 링크), 다음 행동(다음 수업 / 포트폴리오) | `EXAM_NOT_SUBMITTED` → `/lessons/{lessonId}/exam` | FR-EXM-003, 004, PL-EXM-007 |

- 제한 시간 없음(PL-EXM-005), 통과 점수 없음(PL-EXM-001). 다시 응시는 API-EXM-01을 다시 부른다.
- API-EXM-05는 GET이지만 기록을 남긴다. 명세가 "리포트 화면을 열면 확인 처리"로 정했다(PQ22). 같은 응시를 다시 열면 기록하지 않는다.
