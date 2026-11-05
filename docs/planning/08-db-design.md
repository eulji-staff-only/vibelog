# 바이브로그 DB 설계서

2026-11-05 · 조연익

## 1. 개요

바이브로그의 데이터 구조는 MySQL 8.0 기준 **51개 테이블**로 확정했다. 엔티티 확정안(2026-11-04)을 DB 설계 규칙 v2의 물리 이름 규칙으로 옮긴 결과이며, P1 31개는 조연익, P2·P3·비기능 20개는 서연이 맡는다.

| 항목 | 내용 |
| --- | --- |
| 대상 | 51개 테이블 (P1 31 · P2 18 · P3 1 · 비기능 1) |
| DBMS | MySQL 8.0, InnoDB, utf8mb4 |
| 이름 규칙 | snake_case 단수, `User` → `member`, PK는 `테이블명_id`, FK는 참조 PK 이름 그대로 |
| 담당 | P1 31개 조연익 · P2·P3·비기능 20개 서연 |
| FK 방향 | P2·P3 → P1 한 방향. P1 테이블은 P2·P3를 가리키지 않는다 |

### 근거 문서

| 문서 | 이 설계서에서 쓰는 부분 |
| --- | --- |
| 엔티티 확정안 (2026-11-04, 51개) | 엔티티·컬럼·관계의 논리 정의 |
| 바이브로그 DB 설계 규칙 v2 | 물리 이름 규칙, PK·FK, 공통 컬럼, 변환표 (2장) |
| 바이브로그 테이블 정의서 (51개) | 컬럼 타입·NULL·기본값·제약 (4장) |
| 정책 정의서 (PL-DAT-002·003, PL-USR-009·010) | 삭제 방식과 탈퇴 처리 (5장) |
| 요구사항 명세서 (2026-11-04) | P1 기능 요구사항 50개 대조 (7장) |

### 문서 구성

이 설계서는 템플릿의 세 항목(ERD, 테이블 정의서, 데이터 이력 관리 정책)에 인덱스 설계와 요구사항 대조를 더해 일곱 장으로 나눴다.

## 2. 설계 규칙

확정안은 논리 이름(PascalCase, `id`, `user_id`)으로 쓰여 있고, 실제 테이블과 컬럼은 아래 규칙으로 바꿔 쓴다. 확정안과 이름이 다르면 이 장을 따른다.

### 2-1. 이름 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 테이블 | 소문자 + 밑줄, 단수, 접두어 없음 | `course`, `quiz_attempt` |
| 컬럼 | 소문자 + 밑줄 | `estimated_minutes`, `correct_rate` |
| 예/아니오 | `is_`로 시작, BOOLEAN, NOT NULL + 기본값 | `is_public`, `is_active`, `is_passed` |
| 시각 | `_at`으로 끝남, DATETIME | `completed_at`, `report_viewed_at` |
| 날짜만 | `_date`로 끝남, DATE | `vote_date` |
| 기간 | `~_starts_at` / `~_ends_at` | `entry_starts_at` / `entry_ends_at` |
| 상태·종류 | `status`, `~_type` 또는 허용 목록. VARCHAR(20) + CHECK, 값은 대문자 | `activity_type = 'QUIZ'` |
| 순서 | `sort_order` INT |  |
| 줄임말 | 쓰지 않음. `id`, `url`, `faq`만 허용 | `description` (O), `desc` (X) |

테이블명을 소문자로 통일한 이유는 MySQL의 대소문자 구분이 OS마다 다르기 때문이다. ENUM 대신 CHECK를 쓰는 이유는 값을 더할 때마다 `ALTER TABLE`이 필요하고 정의 순서가 바뀌면 기존 값의 의미가 바뀔 수 있어서다.

### 2-2. 예약어·헷갈리는 이름 바꾸기

| 확정안 이름 | 물리 이름 | 이유 |
| --- | --- | --- |
| User (테이블) | `member` | `user`는 PostgreSQL·H2 예약어, MySQL `USER()` 함수와 헷갈림 |
| UserSuspension | `member_suspension` | User를 member로 바꾼 것에 맞춤 |
| Contest.year_month | `contest_month` | `YEAR_MONTH`는 MySQL 예약어 |
| ContestVote.voted_on | `vote_date` | 날짜만 있는 컬럼은 `_date` |
| QuizAttempt.passed | `is_passed` | 예/아니오는 `is_` |
| Notice.show_banner | `is_banner_shown` | 예/아니오는 `is_` |
| Portfolio.kind | `portfolio_type` | 종류 컬럼은 `~_type` |
| Notification.type | `notification_type` | `type`은 MySQL 키워드 |
| Notification.message | `content` | `message` 테이블과 이름이 같아 헷갈림 |

새 이름을 지을 때 피할 단어: `order`, `group`, `key`, `rank`, `row`, `range`, `condition`, `interval`, `window`, `usage`, `year_month`, `type`.

### 2-3. PK·FK

| 항목 | 규칙 |
| --- | --- |
| PK 이름 | `테이블명_id` (`member_id`, `quiz_attempt_id`) |
| PK 타입 | BIGINT NOT NULL AUTO_INCREMENT |
| 복합 PK | 쓰지 않음. 연결 테이블도 자기 PK를 두고 중복은 UNIQUE로 막음 |
| FK 이름 | 참조하는 PK 이름 그대로 |
| FK 예외 | 같은 테이블을 두 번 가리키거나, 자기 자신을 가리키거나, 역할이 있으면 `역할_테이블명_id` |
| FK 삭제 옵션 | 전부 `ON DELETE RESTRICT`. CASCADE·SET NULL은 쓰지 않음 |

역할이 붙는 FK는 12곳이다: `diagnosis_result.recommended_track_id`, `exam_question.concept_activity_id`, `comment.parent_comment_id`, `message.sender_member_id`·`receiver_member_id`·`parent_message_id`, `follow.follower_member_id`·`following_member_id`, `concept_relation.source_activity_id`·`related_activity_id`, `contest_vote.voter_member_id`, `peer_review.reviewer_member_id`, `member_suspension.admin_member_id`·`releaser_member_id`, `notice.admin_member_id`, `report.reporter_member_id`·`handler_member_id`.

`quiz_question.activity_id`처럼 특정 종류(CONCEPT·QUIZ)의 활동만 가리켜도 한 번만 가리키면 역할을 붙이지 않는다. 종류는 앱에서 검사한다.

### 2-4. 제약 이름

| 종류 | 형식 | 예시 |
| --- | --- | --- |
| PK | `pk_테이블` | `pk_course` |
| FK | `fk_테이블_FK컬럼` | `fk_message_sender_member_id` |
| 유일 | `uk_테이블_컬럼` | `uk_member_email` |
| CHECK | `ck_테이블_내용` | `ck_portfolio_type_course` |
| 인덱스 | `idx_테이블_컬럼` | `idx_post_category_created_at` |

64자를 넘으면 컬럼 부분을 줄인다.

### 2-5. 공통 컬럼

51개 테이블 모두 맨 끝에 같은 정의로 넣는다.

```sql
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

`post`, `comment`, `portfolio`, `peer_review` 4개는 공통 컬럼 바로 뒤에 `deleted_at` DATETIME NULL을 둔다. `message`는 보낸 사람과 받은 사람이 따로 지우므로 `sender_deleted_at`, `receiver_deleted_at` 두 개를 일반 컬럼 자리에 둔다. `member`에는 `deleted_at`이 없다(5-3 탈퇴 참고).

컬럼 순서는 **PK → FK → 일반 컬럼 → `created_at` → `updated_at` → `deleted_at`** 이다.

## 3. ERD

ERD는 세 개로 나눴다. 전체 ERD는 51개의 관계를 한눈에 보는 용도고, PK·FK·인덱스 위치는 상세 ERD 두 개에서 본다.

| ERD | 범위 | 담당 | 링크 |
| --- | --- | --- | --- |
| 전체 ERD | P1·P2·P3 51개, PK·FK만 | 서연 | [vivelog 전체](https://www.erdcloud.com/d/Y3hPWpXaKfMat3RrQ) |
| P1 상세 ERD | P1 31개, 전체 컬럼 | 조연익 | [vivelog P1 (11.05)](https://www.erdcloud.com/d/sFf9RygGxhS5ovQFN) |
| P2·P3 상세 ERD | P2·P3 20개, 전체 컬럼 | 서연 | [vivelog P2·P3](https://www.erdcloud.com/d/mbjMFniEXxigmSMH9) |

전체 ERD는 파란색 P1(31개), 주황색 P2·P3(20개)로 칠해 구현 순서가 보이게 했다.

### 3-1. 논리 ERD와 물리 ERD

ERD마다 논리 이름(한글)과 물리 이름(영문)을 함께 적었다. ERDCloud 화면 위 보기 전환으로 둘을 각각 캡처해 붙인다.

| 구분 | 무엇을 보여주나 | 캡처 방법 |
| --- | --- | --- |
| 논리 ERD | 엔티티(한글 이름)과 관계. 무엇을 저장하는지 | ERDCloud에서 **논리** 보기로 전환 후 캡처 |
| 물리 ERD | 실제 테이블·컬럼 이름, 타입, PK·FK | ERDCloud에서 **물리** 보기로 전환 후 캡처 |

### 3-2. 관계 표기

| 항목 | 내용 |
| --- | --- |
| PK | 모든 테이블이 자기 PK(`테이블명_id`) 하나. 복합 PK 없음 |
| 관계선 | FK가 PK에 들어가지 않아 **전부 비식별 관계(점선)** |
| 1:1 | `portfolio_assignment.course_id`(UNIQUE) 하나뿐 |
| 1:N | 나머지 FK 전부. NULL 허용 FK는 선택(0 또는 1)로 그린다 |
| N:M | 중간 테이블로 풀었다: activity_tag, enrollment, lesson_progress, activity_progress, quiz_answer, exam_answer, guide_step_check, portfolio, recommendation, favorite, follow, contest_vote |
| 자기 참조 | `comment.parent_comment_id`(답글), `message.parent_message_id`(답장) |
| FK 없는 연결 | `notification`·`report`의 `target_type` + `target_id`, `email_verification.email` |

### 3-3. ERD 확인 숫자

ERDCloud에서 **Export → SQL Preview**로 확인한다.

| 항목 | 전체 | P1 상세 | P2·P3 상세 |
| --- | --- | --- | --- |
| 테이블 | 51 | 31 | 20 |
| FK | 79 | 44 | 35 |
| member를 가리키는 FK | 26 | 9 | 17 |

**주의**: SQL Preview의 **"Add Non Identifying Relationship Constraint"** 체크박스를 켜야 한다. 이 설계의 관계선은 전부 비식별이라, 꺼 두면 FK가 SQL에서 통째로 빠져 0개로 보인다.

### 3-4. 인덱스 표시

인덱스는 6장 표에 모았다. ERD에는 각 테이블의 인덱스 컬럼에 표시만 한다. P1 5개(member, email_verification, post, comment, quiz_attempt)는 P1 상세 ERD에, 나머지 8개는 P2·P3 상세 ERD에 표시한다.

노션에는 전체 ERD 캡처 1장과 상세 ERD 영역별 캡처를 붙인다. ERDCloud에서 크게 내보내야 글자가 읽힌다.

## 4. 테이블 정의서

51개 테이블의 컬럼명·데이터 타입·NULL 여부·키·기본값·설명·제약은 **[「바이브로그 테이블 정의서 (51개)」](09-table-definition.md)**에 있다. 이 장은 그 문서의 구성과 공통 규칙만 정리한다.

### 4-1. 구성

| 장 | 묶음 | 테이블 | 우선순위 |
| --- | --- | --- | --- |
| 1 | 회원 | member, social_account, email_verification | P1 |
| 2 | 진단 | diagnosis_question, diagnosis_option, diagnosis_result | P1 |
| 3 | 콘텐츠 | track, course_category, course, lesson, activity, concept_tag, activity_tag | P1 |
| 4 | 문항 | quiz_question, quiz_choice, exam_question, exam_choice | P1 |
| 5 | 수강과 학습 기록 | enrollment, lesson_progress, activity_progress, quiz_attempt, quiz_answer, exam_attempt, exam_answer | P1 |
| 6 | 포트폴리오 | portfolio_assignment, guide_step, guide_step_check, portfolio | P1 |
| 7 | 커뮤니티 | post, post_image, comment | P1 |
| 8 | 회원 관리 · 개념·포트폴리오 확장 | member_suspension, concept_relation, concept_bookmark, portfolio_faq | P2 |
| 9 | 커뮤니티 확장 | recommendation, favorite, follow | P2 |
| 10 | 콘테스트 | contest, contest_entry, contest_vote, contest_award | P2 |
| 11 | 동료 리뷰 | review_criterion, review_request, peer_review, review_comment | P2 |
| 12 | 소통 · 운영 | message, notification, notice | P2 |
| 13 | P3 · 비기능 | report, banned_word | P3 · 비기능 |
| 14~18 | 상태값 정의, 확정안과의 관계, 남은 확인 사항, 대조 숫자, 변경 이력 |  |  |

문서끼리 내용이 다르면 **명세서 → 정책 정의서 → 확정안** 순서로 따랐다.

### 4-2. 공통 규칙

- 모든 테이블 끝에 `created_at`, `updated_at` (DATETIME, 자동 입력·자동 갱신)
- 상태 컬럼은 전부 `VARCHAR(20)` + `CHECK`, 값은 대문자 + 밑줄. 제약 이름은 `ck_테이블_컬럼`
- FK 삭제 옵션은 모두 `ON DELETE RESTRICT`. 삭제는 5장 정책대로 앱에서 처리
- 키 표기는 PK, FK(→ 가리키는 테이블), UK(단일 컬럼 유일). 여러 컬럼 UNIQUE와 CHECK는 표 아래 제약에 이름과 함께 적음

### 4-3. 상태·종류 컬럼 (14개)

| 테이블.컬럼 | 기본값 | 값 |
| --- | --- | --- |
| member.role | 'LEARNER' | LEARNER / ADMIN |
| member.status | 'ACTIVE' | ACTIVE / WITHDRAWN |
| social_account.provider | 없음 | GOOGLE / GITHUB |
| email_verification.purpose | 없음 | SIGNUP / PASSWORD_RESET |
| diagnosis_result.status | 없음 | COMPLETED / SKIPPED |
| activity.activity_type | 없음 | CONCEPT / QUIZ |
| activity.body_format | 없음 (NULL 허용) | MARKDOWN / RICH_TEXT |
| quiz_question.display_type | 없음 | CODE_BLANK / CONCEPT / OUTPUT_PREDICT / TRICK |
| portfolio.portfolio_type | 'COURSE' | COURSE / PERSONAL |
| post.category | 없음 | FREE / QNA / SHOWCASE |
| member_suspension.level | 없음 | WARNING / DAYS_7 / DAYS_30 / PERMANENT |
| notification.notification_type | 없음 | RECOMMEND / COMMENT / REPLY / REVIEW / MESSAGE / BADGE / NOTICE / HIDDEN |
| report.target_type | 없음 | POST / PORTFOLIO / COMMENT / REVIEW / MESSAGE |
| report.reason | 없음 | SPAM / ABUSE / PLAGIARISM / PRIVACY / ETC |
| report.status | 'PENDING' | PENDING / HIDDEN / REJECTED |

`CODE_BLANK`와 `OUTPUT_PREDICT`는 `quiz_question.code_snippet`이 필수고, `SHOWCASE`는 `post.work_url`이 필수이며, `COURSE`는 `portfolio.course_id`가 필수다. 세 가지 모두 앱에서 검사한다. `report.reason`이 `ETC`면 `reason_detail`이 필요하다.

수강 상태는 컬럼이 아니라 `enrollment.completed_at`·`dropped_at`으로 판단하고, 정지 여부는 `member_suspension`으로 계산한다.

### 4-4. 확정안과 다른 점

정의서는 확정안에 있는 컬럼만 담았다. 다른 점은 **이름**(규칙 v2 변환표)과 **모든 테이블에 `updated_at`을 둔 것** 둘뿐이다.

이전 P1 정의서에 있던 아래 컬럼은 확정안에 없어서 뺐다: `member.profile_image_url`, `member.bio`, `course.thumbnail_url`, `course.sort_order`, `quiz_attempt.correct_count`, `post.view_count`, `enrollment.status`.

## 5. 데이터 이력 관리 정책

51개 테이블이 모두 여섯 가지 삭제 방식 중 하나에 들어간다. 근거는 정책 정의서 PL-DAT-002·003, PL-USR-009·010이다.

### 5-1. 삭제 방식

| 방식 | 뜻 | 테이블 | 개수 |
| --- | --- | --- | --- |
| **Soft Delete** (삭제 표시) | 행을 남기고 `deleted_at`에 삭제 일시를 적는다. 조회할 때 `deleted_at IS NULL`로 거른다 | post, comment, portfolio, peer_review | 4 |
| **Soft Delete (양쪽 따로)** | 보낸 사람과 받은 사람이 각자 지운다 | message (`sender_deleted_at`, `receiver_deleted_at`) | 1 |
| **Hard Delete** (실제 삭제) | 행을 지운다. 켜고 끄는 기록이거나 탈퇴로 필요 없어진 것 | social_account(탈퇴 시), guide_step_check(체크 해제), recommendation·favorite·follow(취소), concept_bookmark(북마크 해제) | 6 |
| **비활성화** | 지우지 않고 `is_active = FALSE`로 끄는다. 풀이·응시 기록이 이 콘텐츠를 가리키기 때문 | diagnosis_question, activity, quiz_question, exam_question, guide_step, review_criterion | 6 |
| **기간 만료 삭제** | 보관 기간이 지나면 배치로 행을 지운다 | notification(90일), message(1년) | 1 |
| **삭제하지 않음** | 학습·평가·처리 기록이거나 기획 문서에 삭제 기능이 없는 테이블 | 5-2 참고 | 33 |

### 5-2. 삭제하지 않는 테이블 (33개)

| 묶음 | 테이블 |
| --- | --- |
| 회원 | member(탈퇴는 5-3대로 정보만 비움), email_verification, member_suspension |
| 진단 | diagnosis_option, diagnosis_result |
| 콘텐츠 | track, course_category, course, lesson, concept_tag, activity_tag, concept_relation |
| 문항 | quiz_choice, exam_choice |
| 학습 기록 | enrollment, lesson_progress, activity_progress, quiz_attempt, quiz_answer, exam_attempt, exam_answer |
| 포트폴리오 | portfolio_assignment, portfolio_faq |
| 커뮤니티 | post_image |
| 콘테스트 | contest, contest_entry, contest_vote, contest_award |
| 리뷰 | review_request, review_comment |
| 운영 | notice, report, banned_word |

코스와 수업은 지우는 대신 `is_public`으로 숨긴다. 콘테스트 출품 취소는 `contest_entry.canceled_at`, 리뷰 요청 마감은 `review_request.closed_at`으로 남긴다.

`is_public`과 `is_active`는 뜻이 다르다. `is_public`은 다른 사람에게 보여 줄지(course, lesson, portfolio), `is_active`는 아직 쓰는 콘텐츠인지(diagnosis_question, activity, quiz_question, exam_question, guide_step, review_criterion)를 뜻한다.

### 5-3. 회원 탈퇴

`member` 행은 지우지 않고 계정 정보만 비운다(PL-USR-009·010). 그래서 `member`에는 `deleted_at`이 없다.

| 대상 | 처리 |
| --- | --- |
| member | status = WITHDRAWN, withdrawn_at 기록, email·password_hash는 NULL, nickname은 "탈퇴한 사용자" |
| social_account | Hard Delete |
| portfolio | Soft Delete |
| post, comment, peer_review | 그대로 둔다. 작성자는 "탈퇴한 사용자"로 보인다 |

email이 비워지므로 같은 이메일로 다시 가입할 수 있다(MySQL의 UNIQUE는 NULL을 여러 개 허용한다). 정지 중에는 탈퇴할 수 없다.

닉네임은 활성 회원끼리만 겹치지 않으면 되는데 MySQL에는 조건부 유니크 인덱스가 없어서, 가입·변경 시 앱에서 `status = 'ACTIVE'`인 회원만 비교한다.

### 5-4. 수정 이력

별도의 이력 테이블은 두지 않는다.

| 대상 | 수정 방식 |
| --- | --- |
| 포트폴리오 | 수정하면 최신본만 남긴다. 삭제 표시한 코스 작품을 다시 제출하면 그 행을 되살려 덮어쓴다(UNIQUE 유지) |
| 퀴즈·모의고사 문항 | 풀이·응시 기록이 있으면 고치지 않고 비활성화한 뒤 새 문항을 만든다 |
| 퀴즈 답 | "확인"을 누른 답은 수정하지 않는다(PL-LSN-006) |
| 모의고사 답 | 제출 전까지 바꿀 수 있다(PL-EXM-006). 제출 후에는 고치지 않는다 |
| 학습 진도 | 열린 시각·완료 시각은 한 번 기록하면 순서가 바뀌어도 다시 잠금에 걸리지 않는다(PL-ENR-006) |

### 5-5. 저장하지 않고 계산하는 값

같은 정보를 두 번 저장하지 않으려고 컬럼을 만들지 않았다: 진행률, 모의고사 열림, 약한 개념, 오답 비율, 추천 수·랭킹, 득표 수, 콘테스트 진행 상태, 리뷰 요청 마감, 정지 여부, 신고 숨김 여부.

### 5-6. 백업

매일 1회 백업하고 7일 보관한다. 장애 시 4시간 안에 복구한다(PL-DAT-003).

## 6. 인덱스 설계

자주 쓰는 화면 조회에 맞춰 **13개**를 더했다. 왼쪽 컬럼부터 조건에 쓰여야 효과가 있다.

### 6-1. 자동으로 생기는 인덱스

| 종류 | 설명 |
| --- | --- |
| PK | 51개 테이블의 `테이블명_id` |
| UNIQUE | 테이블 정의서에 적은 UNIQUE 제약 전부 |
| FK | MySQL InnoDB는 FK 컬럼에 인덱스가 없으면 자동으로 만든다 |

### 6-2. 조회용으로 더하는 인덱스 (13개)

| 인덱스 이름 | 테이블 | 컬럼 | 쓰는 곳 |
| --- | --- | --- | --- |
| idx_member_nickname | member | (nickname) | 닉네임 중복 검사(앱에서 활성 회원만 비교) |
| idx_email_verification_email_purpose | email_verification | (email, purpose) | 인증·재설정 링크 확인 |
| idx_post_category_created_at | post | (category, created_at) | 게시판 카테고리별 최신순 |
| idx_comment_post_id_created_at | comment | (post_id, created_at) | 게시글 댓글 순서대로 보기 |
| idx_quiz_attempt_member_id_activity_id | quiz_attempt | (member_id, activity_id) | 내 퀴즈 풀이 기록, 이전 점수 |
| idx_recommendation_created_at_post_id | recommendation | (created_at, post_id) | 주간 추천 랭킹 |
| idx_notification_member_id_read_at | notification | (member_id, read_at, created_at) | 내 알림, 안 읽은 알림 |
| idx_notification_created_at | notification | (created_at) | 90일 지난 알림 배치 삭제 |
| idx_message_receiver_member_id_created_at | message | (receiver_member_id, created_at) | 받은 쪽지함 |
| idx_message_sender_member_id_created_at | message | (sender_member_id, created_at) | 보낸 쪽지함, 하루 20통 검사 |
| idx_message_created_at | message | (created_at) | 1년 지난 쪽지 배치 삭제 |
| idx_report_target_type_target_id | report | (target_type, target_id) | 대상별 신고 수 (3명이면 숨김) |
| idx_report_status_created_at | report | (status, created_at) | 운영자 신고 처리 목록 |

P1 테이블 5개(member, email_verification, post, comment, quiz_attempt)는 P1 상세 ERD에, 나머지 8개(recommendation, notification, message, report)는 P2·P3 상세 ERD에 표시한다.

### 6-3. 따로 만들지 않은 것

이미 UNIQUE가 같은 역할을 하는 조회는 인덱스를 더하지 않았다.

| 조회 | 대신하는 UNIQUE |
| --- | --- |
| 내 학습 목록 | enrollment (member_id, course_id) |
| 모의고사 응시 기록 | exam_attempt (member_id, lesson_id, attempt_no) |
| 출품작 득표 수 | contest_vote (contest_entry_id, voter_member_id) |

## 7. 요구사항 대조

**P1 기능 요구사항 50개 모두 정의서 컬럼으로 구현 가능하고, 빠진 컬럼은 없다.** 계산으로 보여주는 값(진행률, 오답 비율, 모의고사 열림 등)은 컬럼 없이 기록에서 계산한다. 3개 요구사항의 일부 표시(신고 이력, 추천 수, 댓글 알림)는 P2·P3 테이블이 생긴 뒤에 붙는다.

| ID | 요구사항 | 테이블 | 확인한 컬럼 |
| --- | --- | --- | --- |
| FR-USR-001 | 이메일 회원 가입 | member, email_verification | member.email(UK), password_hash, nickname |
| FR-USR-002 | 이메일 인증 | email_verification, member | token_hash, purpose = SIGNUP, expires_at, used_at / member.email_verified_at |
| FR-USR-003 | 소셜 로그인 | member, social_account | provider, provider_user_id |
| FR-USR-004 | 로그인 · 로그아웃 | member, enrollment, diagnosis_result | password_hash / enrollment.completed_at·dropped_at |
| FR-USR-005 | 비밀번호 찾기 | email_verification, member | purpose = PASSWORD_RESET, expires_at, used_at |
| FR-USR-006 | 역할 구분 | member | role |
| FR-USR-008 | 회원 탈퇴 | member, social_account, portfolio | status, withdrawn_at, nickname / social_account 삭제 / portfolio.deleted_at |
| FR-USR-009 | 회원 조회 | member, enrollment, lesson_progress, quiz_attempt, exam_attempt, portfolio | 진행률·포트폴리오 수는 계산. 신고 이력은 report(P3) |
| FR-DGN-001 | 진단 설문 | diagnosis_question, diagnosis_option | content, sort_order, is_active. 응답은 저장 안 함 |
| FR-DGN-002 | 트랙 추천 | diagnosis_option, diagnosis_result, track, course | track_id·score / track.sort_order(UK) / recommended_track_id |
| FR-DGN-003 | 진단 건너뛰기 | diagnosis_result | status = SKIPPED, recommended_track_id 비움 |
| FR-CRS-001 | 트랙 · 코스 목록 | track, course, course_category | course.is_public, track_id |
| FR-CRS-002 | 코스 상세 | course, track, lesson, activity, portfolio_assignment | target, goal, plan_days / lesson.estimated_minutes / activity_type |
| FR-CRS-004 | 트랙 소개 | track, course | track.description |
| FR-CRS-005 | 코스 관리 | course, course_category | title, track_id, course_category_id, goal, description, plan_days, is_public |
| FR-CRS-006 | 공개 · 비공개 | course, lesson, portfolio_assignment | course.is_public / lesson.is_public / 과제 존재 |
| FR-CRS-007 | 트랙 관리 | track, course | name, target, description, sort_order / course.track_id |
| FR-CRS-008 | 카테고리 관리 | course_category, course | name, sort_order / course.course_category_id |
| FR-ENR-001 | 수강 시작 | enrollment, lesson_progress, activity_progress | started_at / unlocked_at |
| FR-ENR-002 | 내 학습 | enrollment, lesson_progress, activity_progress, portfolio | unlocked_at, completed_at, portfolio_unlocked_at. 진행률은 계산 |
| FR-ENR-003 | 수업 잠금 | lesson_progress | unlocked_at |
| FR-ENR-004 | 코스 완료 처리 | enrollment, lesson_progress, portfolio | enrollment.completed_at |
| FR-LSN-001 | 수업 화면 | lesson, activity, activity_progress | activity.sort_order, activity_type / unlocked_at |
| FR-LSN-002 | 개념 활동 | activity, activity_tag, concept_tag, activity_progress | body, body_format, tool_note_url / completed_at |
| FR-LSN-006 | 퀴즈 풀이 · 채점 | quiz_question, quiz_choice, quiz_attempt, quiz_answer, activity_progress | display_type, code_snippet / started_at, correct_rate, is_passed, submitted_at / checked_at / pass_rate |
| FR-LSN-007 | 퀴즈 오답 비율 | quiz_answer, quiz_question, concept_tag | is_correct / concept_tag_id. 비율은 계산 |
| FR-LSN-009 | 활동 잠금 | activity_progress | unlocked_at |
| FR-LSN-011 | 수업 · 활동 관리 | lesson, activity, activity_tag | sort_order, body, body_format, tool_note_url, estimated_minutes / concept_tag_id |
| FR-LSN-012 | 퀴즈 문항 등록 | quiz_question, quiz_choice, concept_tag | content, explanation, concept_tag_id, display_type, code_snippet, is_active / 보기 4개, is_correct |
| FR-LSN-014 | 수업 공개 · 비공개 | lesson, activity, quiz_question, exam_question, lesson_progress | is_public / is_active. 영향 학습자 수는 계산 |
| FR-EXM-001 | 모의고사 열림 | activity_progress | 마지막 퀴즈의 completed_at으로 계산 |
| FR-EXM-002 | 응시 · 자동 채점 | exam_question, exam_choice, exam_attempt, exam_answer | attempt_no, started_at, score, correct_rate, submitted_at / exam_choice_id, is_correct |
| FR-EXM-003 | 오답 분석 리포트 | exam_answer, exam_question, activity | concept_activity_id. 개념별 정답률은 계산 |
| FR-EXM-004 | 리포트 확인 | exam_attempt, lesson_progress, enrollment | report_viewed_at / unlocked_at / portfolio_unlocked_at |
| FR-EXM-007 | 모의고사 문항 관리 | exam_question, exam_choice | content, explanation, concept_activity_id(NOT NULL), is_active / is_correct |
| FR-PRT-001 | 포트폴리오 과제 화면 | portfolio_assignment, guide_step, guide_step_check, enrollment | topic, goal / content, sort_order / checked_at / portfolio_unlocked_at |
| FR-PRT-002 | 평가 에이전트 설명 | portfolio_assignment | agent_guide, agent_file_url |
| FR-PRT-003 | 포트폴리오 제출 | portfolio, enrollment | title, result_url, summary, retrospective, ai_tools, is_public, agent_result, screenshot_url, course_id |
| FR-PRT-004 | 링크 형식 검증 | portfolio | result_url. 형식은 앱에서 검사 |
| FR-PRT-005 | 공개 설정 | portfolio | is_public |
| FR-PRT-006 | 포트폴리오 수정 · 삭제 | portfolio, enrollment | updated_at, deleted_at / completed_at 비움 |
| FR-PRT-007 | 내 포트폴리오 목록 | portfolio | member_id, deleted_at |
| FR-PRT-012 | 포트폴리오 과제 작성 | portfolio_assignment, guide_step | topic, goal / content, sort_order |
| FR-PRT-013 | 평가 에이전트 설명 작성 | portfolio_assignment | agent_guide, agent_file_url |
| FR-CMT-001 | 게시판 목록 | post | category, title, created_at |
| FR-CMT-002 | 게시글 상세 | post, post_image, comment, member | title, body, work_url, tools, created_at / image_url. 추천 수는 recommendation(P2) |
| FR-CMT-003 | 게시글 작성 · 수정 · 삭제 | post, post_image | category, title, body, work_url, tools, deleted_at / sort_order(최대 3장) |
| FR-CMT-004 | 댓글 · 답글 | comment | content, parent_comment_id, deleted_at. 알림은 notification(P2) |
| FR-ADM-001 | 백오피스 접근 | member | role = ADMIN |
| FR-ADM-002 | 기본 대시보드 | member, track, course, enrollment | member.created_at / enrollment.completed_at. 수치는 계산 |

## 8. 남은 확인 사항

세 가지가 아직 정해지지 않았다.

| 항목 | 지금 설계 | 확인할 것 | 누가 |
| --- | --- | --- | --- |
| `notification.target_type` 값 | 값 목록 비움 | API 설계와 함께 정하고 CHECK 추가 | DBA · 개발 |
| `concept_bookmark` 삭제 방식 | 해제 시 행 삭제 | 확정안·DBA 공유용 5-1에 없어 DBA가 정함. 기획 담당에게 공유 | DBA → 기획 |
| 인덱스 추가 | 6-2의 13개 | 운영 뒤 실제 조회 패턴으로 재검토 | DBA |

이미 정해져 다시 묻지 않아도 되는 것: `is_passed` 이름, 모든 테이블의 `updated_at`, 시작·확인 시각 컬럼 6개 유지, 닉네임 중복은 앱에서 검사, 포트폴리오 종류 컬럼은 `portfolio_type`.

### 8-1. 넘기기 전 체크리스트

- [ ] 테이블명이 규칙 v2 변환표와 같은가? (소문자 + 밑줄 + 단수, `member`)
- [ ] PK가 `테이블명_id BIGINT NOT NULL AUTO_INCREMENT`인가? (`id` 아님)
- [ ] FK 이름이 변환표와 같은가? (`user_id`, `question_id` 등이 남아 있지 않은가)
- [ ] 예/아니오는 `is_`, 시각은 `_at`, 날짜는 `_date`인가?
- [ ] 상태·종류 컬럼이 VARCHAR(20) + CHECK이고 값이 대문자인가?
- [ ] 51개 모두 맨 끝에 `created_at`, `updated_at`이 있는가?
- [ ] post, comment, portfolio, peer_review에만 `deleted_at`이 있는가? (`member`에는 없음)
- [ ] `is_public`과 `is_active`를 5-2 표대로 썼는가?
- [ ] FK가 모두 `ON DELETE RESTRICT`이고, 제약에 모두 이름이 붙어 있는가?
- [ ] SQL Preview에서 51 / 79 / 26이 나오는가? (비식별 관계 체크박스 켜고)

### 8-2. 변경 이력

| 날짜 | 내용 |
| --- | --- |
| 2026-11-04 | 엔티티 확정안 51개 확정. DB 설계 규칙 v2 작성(변환표 추가, 분담 P1 31 / P2·P3 20) |
| 2026-11-05 | 시작·확인 시각 컬럼 6개 유지로 확정. 포트폴리오 종류 컬럼을 `portfolio_type`으로 확정 |
| 2026-11-05 | 확정안에 없는 컬럼 7개 제거. P1·P2·P3 정의서를 51개 한 문서로 합침 |
| 2026-11-05 | 이 설계서로 ERD·정의서·이력 정책·인덱스·요구사항 대조를 한데 모음 |
