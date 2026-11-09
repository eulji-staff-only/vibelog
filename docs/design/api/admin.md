# admin API 명세 (운영자 · 백오피스)

> 영역: admin / API ID: API-ADM-01~45
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.
> P2(미리보기 · 확장 대시보드)는 마일스톤 `4. P2 구현` · #172. P2 진단 설정(API-ADM-41~45)은 #171 (코드는 course 영역)

모든 `/admin/**`, `/api/admin/**`은 운영자만 들어온다. 운영자가 아니면 → `/` (FR-ADM-001).

## 대시보드 · 트랙 · 카테고리

| ID | 기능 | Method | URL | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-01 | 대시보드 | GET | `/admin` | query `from`, `to` | A-01 + 가입자 수, 트랙·코스별 수강 수·완료율 | `INVALID_INPUT`(기간) | FR-ADM-002 |
| API-ADM-40 | 확장 대시보드 | GET | `/admin` (그대로, API-ADM-01 응답에 추가) | query `from`, `to`(API-ADM-01과 같음) | A-01 + 진단 완료·건너뛰기 비율, 추천 트랙 vs 실제 선택 트랙, 수업·활동별 이탈률, 퀴즈 통과율·개념별 오답률, 수업별 모의고사 평균·약한 개념, 포트폴리오 제출 수, 게시글 수, 리뷰 수, 쪽지 수, 신고 수, 콘테스트 참여 수 (아래 "영역 간 계약" 표) | `INVALID_INPUT`(기간) | FR-ADM-003 |
| API-ADM-02 | 트랙 관리 | GET | `/admin/tracks` | - | A-02 + 트랙(이름, 대상, 설명, 순서, 코스 수) | - | FR-CRS-007 |
| API-ADM-03 | 트랙 만들기 | POST | `/admin/tracks` | form `name`, `target`, `description`, `sortOrder` | → `/admin/tracks` | `INVALID_INPUT`, `DUPLICATE_NAME` | FR-CRS-007 |
| API-ADM-04 | 트랙 수정 | POST | `/admin/tracks/{trackId}/edit` | form 위와 같음 | → `/admin/tracks` | `TRACK_NOT_FOUND`, `DUPLICATE_NAME` | FR-CRS-007 |
| API-ADM-05 | 트랙 삭제 | POST | `/admin/tracks/{trackId}/delete` | path `trackId` | → `/admin/tracks` | `TRACK_HAS_COURSES`(코스 있는 트랙) | 화면 흐름 4장 |
| API-ADM-06 | 카테고리 관리 | GET | `/admin/categories` | - | A-03 + 카테고리(이름, 순서, 코스 수) | - | FR-CRS-008 |
| API-ADM-07 | 카테고리 만들기 | POST | `/admin/categories` | form `name` | → `/admin/categories` | `DUPLICATE_NAME` | FR-CRS-008 |
| API-ADM-08 | 카테고리 수정 | POST | `/admin/categories/{courseCategoryId}/edit` | form `name` | → `/admin/categories` | `DUPLICATE_NAME` | FR-CRS-008 |
| API-ADM-09 | 카테고리 삭제 | POST | `/admin/categories/{courseCategoryId}/delete` | path `courseCategoryId` | → `/admin/categories` | `CATEGORY_HAS_COURSES`(코스가 카테고리를 꼭 가져야 함, DB `course.course_category_id` NOT NULL) | FR-CRS-008 |
| API-ADM-10 | 카테고리 순서 | POST | `/api/admin/categories/order` | json `courseCategoryIds[]`(새 순서) | JSON | `INVALID_INPUT` | 화면 흐름 4장 |

## 코스 · 수업 · 문항

| ID | 기능 | Method | URL | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-11 | 코스 목록 | GET | `/admin/courses` | query `track`, `category` | A-04 + 코스(이름, 트랙, 카테고리, 공개 상태, 수강자 수) | - | FR-CRS-005 |
| API-ADM-12 | 코스 생성 화면 | GET | `/admin/courses/new` | - | A-05 생성 폼 + 트랙·카테고리 목록 | - | FR-CRS-005 |
| API-ADM-13 | 코스 만들기 | POST | `/admin/courses` | form `trackId`, `courseCategoryId`, `title`, `summary`, `description`, `target`, `goal`, `planDays` | 비공개로 저장 → `/admin/courses/{courseId}` | `INVALID_INPUT` → 같은 폼 다시 | FR-CRS-005 |
| API-ADM-14 | 코스 상세 | GET | `/admin/courses/{courseId}` | path `courseId` | A-05 + 코스 정보, 수업 목록(순서, 공개 상태), 포트폴리오 과제 상태 | `COURSE_NOT_FOUND` | FR-CRS-005, 006 |
| API-ADM-15 | 코스 수정 | POST | `/admin/courses/{courseId}/edit` | form API-ADM-13과 같음 | → `/admin/courses/{courseId}` | `INVALID_INPUT` | FR-CRS-005 |
| API-ADM-16 | 코스 공개·비공개 | POST | `/admin/courses/{courseId}/visibility` | form `isPublic` (확인 모달 후) | → `/admin/courses/{courseId}` + toast | `COURSE_NOT_PUBLISHABLE`(공개 수업 없음·과제 없음) | FR-CRS-006 |
| API-ADM-17 | 수업 만들기 | POST | `/admin/courses/{courseId}/lessons` | form `title`, `intro`, `goal`, `contents`, `estimatedMinutes` | 비공개로 저장 → `/admin/lessons/{lessonId}` | `INVALID_INPUT` | FR-LSN-011 |
| API-ADM-18 | 수업 순서 | POST | `/admin/courses/{courseId}/lessons/order` | form `lessonIds[]` (저장 버튼) | → `/admin/courses/{courseId}` | `INVALID_INPUT` | FR-LSN-011 |
| API-ADM-19 | 수업 편집 화면 | GET | `/admin/lessons/{lessonId}` | query `tab`(activities, exam) | A-06 + 수업 정보, 활동 목록(개념·퀴즈, 순서, 사용 여부), 퀴즈 문항, 모의고사 문항, 개념 태그 목록 | `LESSON_NOT_FOUND` | FR-LSN-011, 012, FR-EXM-007 |
| API-ADM-20 | 수업 정보 수정 | POST | `/admin/lessons/{lessonId}/edit` | form API-ADM-17과 같음 | → `/admin/lessons/{lessonId}` | `INVALID_INPUT` | FR-LSN-011 |
| API-ADM-21 | 활동 추가 | POST | `/admin/lessons/{lessonId}/activities` | form `activityType`(CONCEPT, QUIZ), `title`, `body`, `bodyFormat`(MARKDOWN, RICH_TEXT), `toolNoteUrl`, `estimatedMinutes`, `tagNames` | → `/admin/lessons/{lessonId}` | `INVALID_INPUT` | FR-LSN-011 |
| API-ADM-22 | 활동 수정 | POST | `/admin/activities/{activityId}/edit` | form 위와 같음 | → `/admin/lessons/{lessonId}` | `INVALID_INPUT` | FR-LSN-011 |
| API-ADM-23 | 활동 사용 중지 | POST | `/admin/activities/{activityId}/deactivate` | path `activityId` | 삭제 대신 비활성(진도 보존) → `/admin/lessons/{lessonId}` | - | DB `activity.is_active` |
| API-ADM-24 | 활동 순서 | POST | `/admin/lessons/{lessonId}/activities/order` | form `activityIds[]` | → `/admin/lessons/{lessonId}` | `INVALID_ORDER`(개념 뒤에 퀴즈가 와야 함, PL-LSN-001) | FR-LSN-011 |
| API-ADM-25 | 개념 이미지 올리기 | POST | `/api/admin/uploads/images` | file `image` | JSON `url` (편집기에 넣음) | `INVALID_FILE`(PL-DAT-004) | FR-LSN-011 |
| API-ADM-26 | 퀴즈 문항 추가 | POST | `/admin/activities/{activityId}/questions` | form `content`, `displayType`, `codeSnippet`, `explanation`, `conceptTagId`, `choices[4]`, `correctIndex` | → `/admin/lessons/{lessonId}` | `INVALID_INPUT`(보기 4개·정답 1개, CODE_BLANK·OUTPUT_PREDICT는 코드 필수) | FR-LSN-012 |
| API-ADM-27 | 퀴즈 문항 수정 | POST | `/admin/quiz-questions/{quizQuestionId}/edit` | form 위와 같음 | 풀이 기록이 있으면 기존 문항 비활성 + 새 문항 저장 → `/admin/lessons/{lessonId}` | `INVALID_INPUT` | FR-LSN-012, DB 5-4 |
| API-ADM-28 | 퀴즈 문항 사용·중지 | POST | `/admin/quiz-questions/{quizQuestionId}/active` | form `active` | → `/admin/lessons/{lessonId}` | - | FR-LSN-012 |
| API-ADM-29 | 모의고사 문항 추가 | POST | `/admin/lessons/{lessonId}/exam-questions` | form `content`, `explanation`, `conceptActivityId`, `choices[]`, `correctIndex` | → `/admin/lessons/{lessonId}?tab=exam` | `CONCEPT_REQUIRED`(연관 개념 없음, FR-EXM-007), `INVALID_INPUT` | FR-EXM-007 |
| API-ADM-30 | 모의고사 문항 수정 | POST | `/admin/exam-questions/{examQuestionId}/edit` | form 위와 같음 | → `/admin/lessons/{lessonId}?tab=exam` | API-ADM-29와 같음 | FR-EXM-007 |
| API-ADM-31 | 모의고사 문항 사용·중지 | POST | `/admin/exam-questions/{examQuestionId}/active` | form `active` | → `/admin/lessons/{lessonId}?tab=exam` | - | FR-EXM-007 |
| API-ADM-32 | 비공개 영향 확인 | GET | `/api/admin/lessons/{lessonId}/impact` | path `lessonId` | JSON `activeLearnerCount` (경고 모달) | - | FR-LSN-014 |
| API-ADM-33 | 수업 공개·비공개 | POST | `/admin/lessons/{lessonId}/visibility` | form `isPublic` (확인 모달 후) | 공개하면 기존 수강의 `lesson_progress` 추가(PL-ENR-005) → `/admin/courses/{courseId}` + toast | `LESSON_NOT_PUBLISHABLE`(빠진 항목 목록: 개념 뒤 퀴즈, 퀴즈 3~5문항, 정답, 모의고사 문항) | FR-LSN-014, PL-LSN-003 |
| API-ADM-39 | 미리보기 | GET | `/courses/{courseId}/lessons/{lessonId}` (학습자 U-13과 같은 주소, A-06 "미리보기" 버튼이 `activity` 없이 새 탭으로 연다) | path `courseId`, `lessonId` | U-13을 운영자 권한으로 그대로 보여준다. 비공개 수업·수강하지 않은 코스여도 잠김·수강 여부를 검사하지 않는다(공개 전에 확인해야 하므로). 운영자는 진행 기록(`activity_progress`)이 없으므로 `activity` 쿼리가 없으면 API-LSN-01의 "진행 중 활동" 대신 **첫 번째 사용 중인 활동**(`sort_order` 1번)을 보여준다. 조회만 하고 진행 기록은 남기지 않는다 | `LESSON_NOT_FOUND` | FR-LSN-015 |

- API-ADM-39는 `admin` 화면(A-06)에 버튼만 있고, 실제 요청·검사 로직은 lesson 영역의 API-LSN-01에 운영자 예외로 들어간다 (lesson.md 참고, 같은 묶음 C 담당이라 요청서 없이 함께 정함). 운영자이면서 진행 기록이 없을 때 첫 번째 활동을 고르는 분기도 API-LSN-01에 둔다.

## 포트폴리오 과제 · 회원

| ID | 기능 | Method | URL | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-34 | 과제 편집 화면 | GET | `/admin/courses/{courseId}/portfolio` | query `tab`(assignment, agent) | A-07 + 과제(주제, 목표), 스텝 목록, 평가 에이전트 안내, 설정 파일 | `COURSE_NOT_FOUND` | FR-PRT-012, 013 |
| API-ADM-35 | 과제·스텝 저장 | POST | `/admin/courses/{courseId}/portfolio` | form `topic`, `goal`, `steps[]`(`guideStepId`, `content`, 순서) | 체크 기록이 있는 스텝은 지우지 않고 비활성 → `/admin/courses/{courseId}` | `INVALID_INPUT` | FR-PRT-012 |
| API-ADM-36 | 평가 에이전트 저장 | POST | `/admin/courses/{courseId}/portfolio/agent` | form `agentGuide`, file `agentFile`(선택, 저장 컬럼: `agent_file_url`) | → `/admin/courses/{courseId}/portfolio?tab=agent` | `INVALID_FILE`(md·json·yaml·txt, 5MB, 실행 파일 불가, PL-PRT-010) | FR-PRT-013 |
| API-ADM-37 | 회원 목록 | GET | `/admin/members` | query `q`(닉네임·이메일), `page` | A-08 목록 | - | FR-USR-009 |
| API-ADM-38 | 회원 상세 | GET | `/admin/members/{memberId}` | path `memberId` | A-08 상세 + 수강 코스, 진행률, 퀴즈·모의고사 결과, 포트폴리오 수 | `USER_NOT_FOUND` | FR-USR-009 |

## 진단 설정 (P2, FR-DGN-004)

A-09 `/admin/diagnosis`. 코드는 course 영역에 둔다(진단 테이블이 course 영역). 응답은 저장하지 않으므로(PL-DGN-001) 문항·보기를 고쳐도 지난 진단 결과(`diagnosis_result`)에는 영향이 없다.

| ID | 기능 | Method | URL | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-41 | 진단 설정 화면 | GET | `/admin/diagnosis` | - | A-09 + 문항 전체(사용 중지 포함, `sort_order` 순: `diagnosisQuestionId`, 내용, 순서, 사용 여부) + 보기(`sort_order` 순: `diagnosisOptionId`, 내용, `trackId`·트랙 이름, 점수) + 트랙 목록(`trackId`, 이름) + (테스트 응답 직후) 미리 보기 결과 | - | FR-DGN-004 |
| API-ADM-42 | 문항 추가 | POST | `/admin/diagnosis/questions` | form `content`, `sortOrder`, `options[]`(`content`, `trackId`, `score`) | 사용 중으로 저장 → `/admin/diagnosis` + toast | `INVALID_INPUT`(빈 칸, 문항 500자·보기 255자 초과(`sql/01-schema.sql` 컬럼 길이), 보기 2개 미만, 없는 트랙, 점수가 1 미만이거나 `INT` 범위 초과) → 같은 폼 다시 | FR-DGN-004, PL-DGN-002 |
| API-ADM-43 | 문항 수정 | POST | `/admin/diagnosis/questions/{diagnosisQuestionId}/edit` | form API-ADM-42와 같음 + `options[].diagnosisOptionId`(있는 보기) | 문항·보기를 그 자리에서 고친다. 폼에 없는 기존 보기는 지운다(응답을 저장하지 않아 가리키는 기록이 없다) → `/admin/diagnosis` + toast | API-ADM-42와 같음 | FR-DGN-004 |
| API-ADM-44 | 문항 사용·중지 | POST | `/admin/diagnosis/questions/{diagnosisQuestionId}/active` | form `active` | → `/admin/diagnosis` + toast. 중지한 문항은 진단 설문(API-DGN-01)에 나오지 않는다 | `INVALID_INPUT`(마지막 사용 중 문항은 중지할 수 없음) → toast | FR-DGN-004 |
| API-ADM-45 | 테스트 응답 | POST | `/admin/diagnosis/test` | form `diagnosisOptionIds`(사용 중 문항마다 1개, API-DGN-02와 같음) | 진단 제출과 같은 규칙(PL-DGN-002: 점수 합계, 동점이면 트랙 순서 앞)으로 추천 트랙을 계산만 하고 **저장하지 않는다** → `/admin/diagnosis` + 미리 보기(추천 트랙, 트랙별 점수 합계) | `INVALID_INPUT`(문항 수와 답 수가 다름) | FR-DGN-004, PL-DGN-002 |

- 보기 순서(`diagnosis_option.sort_order`)는 폼에 적힌 순서대로 1부터 다시 매긴다.
- 테스트 응답은 API-DGN-02와 같은 계산(course 영역 `TrackRecommendPolicy`)을 그대로 써서, 실제 진단과 미리 보기 결과가 달라지지 않게 한다.
- 이 명세에서 정한 것 (P2, 리뷰에서 확인):
  1. 보기는 문항마다 **2개 이상** (고를 것이 하나뿐이면 문항이 아니다). 점수는 **1 이상, DB `INT` 범위(2,147,483,647) 이하** (`diagnosis_option.score` 기본값 1, 0점 보기는 추천에 영향이 없어 뜻이 없다). 그 밖의 보기 수·점수 상한은 두지 않는다. 큰 점수를 여러 개 골라도 넘치지 않게 **트랙별 점수 합계는 `long`으로 계산**한다 (실제 진단 API-DGN-02와 테스트 응답 API-ADM-45 모두. 지금 `TrackRecommendPolicy`의 `int` 합계는 진단 설정 구현 #187에서 `long`으로 바꾼다).
  2. 문항 순서는 폼의 `sortOrder` 숫자로 정한다 (드래그 정렬은 두지 않는다 — 문항 수가 적다).
  3. 사용 중인 문항이 하나도 없으면 진단을 할 수 없으므로 **마지막 사용 중 문항은 중지할 수 없다.**
  4. 문항은 지우지 않고 사용·중지만 한다 (화면 흐름 4장 A-09 "문항, 보기, 보기별 트랙 점수 … 저장").

## 확장 대시보드 — 영역 간 계약 (P2, FR-ADM-003)

admin은 직접 계산하지 않고, 각 지표를 담당 영역의 `<영역>Reader`에서 읽어 와 기간(`from`~`to`)으로 걸러 합친다. 영역 담당은 [README 영역 담당](../../../README.md#영역-담당) 표를 따른다. 아래 메서드는 **제안**이고, 받는 쪽이 자기 Reader에 없으면 구현 때(#194) 추가한다 ([08. 작업 순서](../../collaboration/08-task-flow.md) 규칙 1·4 "Reader는 새 메서드 추가만").

| 지표 | 담당 영역 | Reader·메서드(제안) | 비고 |
| --- | --- | --- | --- |
| 진단 완료·건너뛰기 비율 | course | `CourseReader.countDiagnosisResultsByStatus(from, to)` → `Map<String, Integer>`(`COMPLETED`, `SKIPPED`) | `diagnosis_result.status` |
| 추천 트랙 vs 실제 선택 트랙 | course, lesson | `CourseReader.findDiagnosisRecommendedTracks(from, to)` → `Map<Long memberId, Long trackId>` + `LessonReader.findEnrolledCourseIds(memberIds)` → `Map<Long memberId, Set<Long> courseId>` (courseId→trackId는 `CourseReader.getTrackIdsByCourseIds`로 변환해 admin이 비교) | 두 영역 값을 admin이 합친다 |
| 수업·활동별 이탈률 | lesson | `LessonReader.getDropOffRateByLesson(from, to)` → `Map<Long lessonId, DropOffRate>`, `DropOffRate(int eligibleCount, int dropOffCount)` — `eligibleCount`는 그 수업·활동이 기간 안에 열린(`unlocked_at` 있음) 전체 건수, `dropOffCount`는 그중 기간이 끝난 시점까지도 `completed_at`이 없는 건수(진행 중인 회원도 포함되므로 정확한 "이탈"이 아니라 "아직 못 끝낸 비율"로 본다) | PL-ENR-006 계산 값과 같은 테이블 |
| 퀴즈 통과율 · 개념별 오답률 | quiz | `QuizReader.getPassRate(from, to)`, `QuizReader.getWrongRateByConceptTag(from, to)` | PL-LSN-008 약한 개념 기준(60%) 재사용 |
| 수업별 모의고사 평균 · 약한 개념 | exam | `ExamReader.getAverageScoreByLesson(from, to)`, `ExamReader.getWeakConceptsByLesson(from, to)` | PL-LSN-008과 같은 기준, 5명 미만 수업은 숨김(U-16과 같음) |
| 포트폴리오 제출 수 | portfolio | `PortfolioReader.countSubmitted(from, to)` | - |
| 게시글 수 | community | `CommunityReader.countPosts(from, to)` | - |
| 리뷰 수 | community | `CommunityReader.countPeerReviews(from, to)` | 동료 리뷰(FR-REV) |
| 쪽지 수 | message | `MessageReader.countMessages(from, to)` | - |
| 신고 수 | message | `MessageReader.countReports(from, to)` | README 영역 담당 표에서 "신고"는 `message` 묶음(쪽지·신고·알림)이 담당. `report`는 P3 기능이라 신고 처리 자체는 아직 없고, 지금은 0으로 보여도 된다 |
| 콘테스트 참여 수 | contest | `ContestReader.countEntries(from, to)` | FR-CTS |

- 계약 표 ①~⑬(08 문서)에는 넣지 않는다. 단순 집계 조회라 "그 밖에 읽기만 하는 값"에 해당해 각 영역 Reader에 메서드만 추가하면 된다 (08 문서 "안 멈추는 규칙" 1번).
- 기간(`from`~`to`) 없이 걸러지는 값(예: 지금 시점 기준 "약한 개념")은 영역 담당이 Reader 설계 때 정한다.
