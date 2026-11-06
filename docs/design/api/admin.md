# admin API 명세 (운영자 · 백오피스)

> 영역: admin / API ID: API-ADM-01~38
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

모든 `/admin/**`, `/api/admin/**`은 운영자만 들어온다. 운영자가 아니면 → `/` (FR-ADM-001).

## 대시보드 · 트랙 · 카테고리

| ID | 기능 | Method | URL | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-01 | 대시보드 | GET | `/admin` | query `from`, `to` | A-01 + 가입자 수, 트랙·코스별 수강 수·완료율 | `INVALID_INPUT`(기간) | FR-ADM-002 |
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

## 포트폴리오 과제 · 회원

| ID | 기능 | Method | URL | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-34 | 과제 편집 화면 | GET | `/admin/courses/{courseId}/portfolio` | query `tab`(assignment, agent) | A-07 + 과제(주제, 목표), 스텝 목록, 평가 에이전트 안내, 설정 파일 | `COURSE_NOT_FOUND` | FR-PRT-012, 013 |
| API-ADM-35 | 과제·스텝 저장 | POST | `/admin/courses/{courseId}/portfolio` | form `topic`, `goal`, `steps[]`(`guideStepId`, `content`, 순서) | 체크 기록이 있는 스텝은 지우지 않고 비활성 → `/admin/courses/{courseId}` | `INVALID_INPUT` | FR-PRT-012 |
| API-ADM-36 | 평가 에이전트 저장 | POST | `/admin/courses/{courseId}/portfolio/agent` | form `agentGuide`, file `agentFile`(선택, 저장 컬럼: `agent_file_url`) | → `/admin/courses/{courseId}/portfolio?tab=agent` | `INVALID_FILE`(md·json·yaml·txt, 5MB, 실행 파일 불가, PL-PRT-010) | FR-PRT-013 |
| API-ADM-37 | 회원 목록 | GET | `/admin/members` | query `q`(닉네임·이메일), `page` | A-08 목록 | - | FR-USR-009 |
| API-ADM-38 | 회원 상세 | GET | `/admin/members/{memberId}` | path `memberId` | A-08 상세 + 수강 코스, 진행률, 퀴즈·모의고사 결과, 포트폴리오 수 | `USER_NOT_FOUND` | FR-USR-009 |
