# 바이브로그 테이블 정의서 (51개)

2026-11-05 · P1 조연익(연익) · P2·P3 서연

## 작성 기준

| 항목 | 내용 |
| --- | --- |
| 범위 | 엔티티 확정안(2026-11-04)의 51개 테이블 전부. P1 31개 + P2 18개 + P3 1개 + 비기능 1개 |
| 컬럼 내용 | 「엔티티 확정안(2026-11-04)」 4장 + 「최신 기획 반영 DBA 공유용(2026-11-04)」 4·5장 |
| 숫자 제한·상태 조건 | 「정책 정의서(2026-11-04)」 |
| 우선순위·화면 데이터 | 「요구사항 명세서(2026-11-04)」 |
| 이름 | 「DB 설계 규칙 v2」. snake_case 단수, User → member, PK는 `테이블_id`, FK는 참조 PK 이름. 확정안 이름과의 대응은 규칙 v2 4장 변환표 |
| 양식 | 「DB 설계 오늘 끝내기 안내서」 (컬럼명 · 타입 · NULL · 키 · 기본값 · 설명) |
| DBMS | MySQL 8.0 |
| 합친 문서 | P1 테이블 정의서 v2(조연익) + P2·P3 테이블 정의서(서연) |

문서끼리 다르면 **명세서 → 정책 정의서 → 확정안** 순서로 따랐어요.

### 읽는 법

- **NULL**: N은 꼭 있어야 하는 값, Y는 비어 있어도 되는 값이에요.
- **키**: PK, FK(→ 가리키는 테이블), UK(단일 컬럼 유일)로 적었어요. 여러 컬럼에 걸친 UNIQUE와 CHECK는 표 아래 **제약**에 이름과 함께 적었어요.
- **컬럼 순서**: PK → FK → 일반 컬럼 → `created_at` → `updated_at` → `deleted_at`.
- **공통 컬럼**: 모든 테이블 끝에 `created_at`, `updated_at`이 같은 정의로 들어가요. 확정안은 "수정이 있는 테이블에만 `updated_at`"이라고 했지만, 공통 컬럼은 DBA가 정하는 항목(DBA 공유용 3장 3번)이라 P2·P3 정의서와 맞춰 전부 넣었어요.
- **상태 컬럼**: 전부 `VARCHAR(20)` + `CHECK`로 통일했어요. 제약 이름은 `ck_테이블_컬럼`이에요.
- **FK 삭제 옵션**: 모두 `ON DELETE RESTRICT`예요.

## 목차

| 장 | 묶음 | 테이블 | 우선순위 |
| --- | --- | --- | --- |
| 1 | 회원 | member, social_account, email_verification | P1 |
| 2 | 진단 | diagnosis_question, diagnosis_option, diagnosis_result | P1 |
| 3 | 콘텐츠 | track, course_category, course, lesson, activity, concept_tag, activity_tag | P1 |
| 4 | 문항 | quiz_question, quiz_choice, exam_question, exam_choice | P1 |
| 5 | 수강과 학습 기록 | enrollment, lesson_progress, activity_progress, quiz_attempt, quiz_answer, exam_attempt, exam_answer | P1 |
| 6 | 포트폴리오 | portfolio_assignment, guide_step, guide_step_check, portfolio | P1 |
| 7 | 커뮤니티 | post, post_image, comment | P1 |
| 8 | 회원 관리 · 개념 확장 · 포트폴리오 확장 | member_suspension, concept_relation, concept_bookmark, portfolio_faq | P2 |
| 9 | 커뮤니티 확장 | recommendation, favorite, follow | P2 |
| 10 | 콘테스트 | contest, contest_entry, contest_vote, contest_award | P2 |
| 11 | 동료 리뷰 | review_criterion, review_request, peer_review, review_comment | P2 |
| 12 | 소통 · 운영 | message, notification, notice | P2 |
| 13 | P3 · 비기능 | report, banned_word | P3 · 비기능 |
| 14 | 상태값 정의 | | |
| 15 | 확정안과의 관계 | | |
| 16 | 남은 확인 사항 | | |
| 17 | ERD와 대조할 숫자 | | |

## 1. 회원

### member (회원)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| member_id | BIGINT | N | PK | 자동 증가 | 회원 번호 |
| email | VARCHAR(255) | Y | UK |  | 로그인 이메일. 활성 회원은 필수, 탈퇴 시 비움 |
| password_hash | VARCHAR(255) | Y |  |  | 암호화한 비밀번호. 소셜 가입자는 비움 |
| nickname | VARCHAR(30) | N |  |  | 닉네임. 2~12자(PL-USR-002). 탈퇴 시 "탈퇴한 사용자" |
| role | VARCHAR(20) | N |  | 'LEARNER' | 권한 (상태값 정의 참고) |
| status | VARCHAR(20) | N |  | 'ACTIVE' | 계정 상태 (상태값 정의 참고) |
| is_test | BOOLEAN | N |  | FALSE | 테스트 계정. 평균 점수에서 뺌(PL-EXM-008). DB에서 직접 표시 |
| email_verified_at | DATETIME | Y |  |  | 이메일 인증 완료 일시. 소셜 가입은 가입 시 채움 |
| withdrawn_at | DATETIME | Y |  |  | 탈퇴 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_member_role` CHECK(role IN ('LEARNER', 'ADMIN')), `ck_member_status` CHECK(status IN ('ACTIVE', 'WITHDRAWN')), `ck_member_email_required` CHECK(status = 'WITHDRAWN' OR email IS NOT NULL)

메모: 닉네임 중복 금지는 **활성 회원끼리만** 적용해요. 탈퇴 회원은 모두 "탈퇴한 사용자"라서 `nickname` 전체에 UNIQUE를 걸면 두 번째 탈퇴부터 실패해요(DBA 공유용 5-3). 앱에서 검사하거나 `status = 'ACTIVE'` 조건의 부분 유니크 인덱스를 써요. 정지 여부는 저장하지 않고 `member_suspension`으로 계산해요(P2, DBA 공유용 5-2). 연속 로그인 실패 횟수는 캐시로 세요(PL-USR-007).

### social_account (소셜 연결 계정)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| social_account_id | BIGINT | N | PK | 자동 증가 | 소셜 계정 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| provider | VARCHAR(20) | N |  |  | 소셜 제공자 (상태값 정의 참고) |
| provider_user_id | VARCHAR(255) | N |  |  | 제공자 쪽 회원 식별값 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_social_account_provider_user` UNIQUE(provider, provider_user_id), `ck_social_account_provider` CHECK(provider IN ('GOOGLE', 'GITHUB'))

메모: 탈퇴 시 행을 삭제해요(DBA 공유용 5-1).

### email_verification (이메일 인증·비밀번호 재설정)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| email_verification_id | BIGINT | N | PK | 자동 증가 | 인증 번호 |
| email | VARCHAR(255) | N |  |  | 인증 메일을 보낸 주소. 회원 FK가 아니에요 |
| token_hash | VARCHAR(255) | N | UK |  | 메일 링크 토큰의 해시. 원문은 저장하지 않아요 |
| purpose | VARCHAR(20) | N |  |  | 용도 (상태값 정의 참고) |
| expires_at | DATETIME | N |  |  | 만료 일시. SIGNUP 24시간, PASSWORD_RESET 30분(PL-USR-004) |
| used_at | DATETIME | Y |  |  | 사용 일시. 재설정 링크는 1회만(PL-USR-004) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_email_verification_purpose` CHECK(purpose IN ('SIGNUP', 'PASSWORD_RESET'))

메모: 가입 전에도 메일을 보내므로 `member`를 가리키지 않고 **이메일 주소를 그대로 저장**해요. 인증 메일을 다시 보내면 이전 미사용 토큰은 무효로 처리해요.

## 2. 진단

### diagnosis_question (진단 문항)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| diagnosis_question_id | BIGINT | N | PK | 자동 증가 | 진단 문항 번호 |
| content | VARCHAR(500) | N |  |  | 문항 내용 |
| sort_order | INT | N |  |  | 표시 순서 |
| is_active | BOOLEAN | N |  | TRUE | 고칠 때는 비활성화하고 새로 추가해요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 한 문항에 보기 하나를 골라요. 진단 설정 화면(FR-DGN-004)은 P2라서 초기 데이터는 DB에 직접 넣어요.

### diagnosis_option (진단 보기)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| diagnosis_option_id | BIGINT | N | PK | 자동 증가 | 진단 보기 번호 |
| diagnosis_question_id | BIGINT | N | FK → diagnosis_question |  | 진단 문항 |
| track_id | BIGINT | N | FK → track |  | 이 보기를 고르면 점수를 받는 트랙 |
| content | VARCHAR(255) | N |  |  | 보기 내용 |
| score | INT | N |  | 1 | 트랙 점수. 합계가 가장 높은 트랙을 추천(PL-DGN-002) |
| sort_order | INT | N |  |  | 표시 순서 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 동점이면 `track.sort_order`가 앞선 트랙을 추천해요(PL-DGN-002).

### diagnosis_result (진단 결과)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| diagnosis_result_id | BIGINT | N | PK | 자동 증가 | 진단 결과 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| recommended_track_id | BIGINT | Y | FK → track |  | 추천된 트랙. 건너뛰면 비움 |
| status | VARCHAR(20) | N |  |  | 진단 상태 (상태값 정의 참고) |
| created_at | DATETIME | N |  | 현재 시각 | 진단한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_diagnosis_result_status` CHECK(status IN ('COMPLETED', 'SKIPPED')), `ck_diagnosis_result_track` CHECK((status = 'COMPLETED' AND recommended_track_id IS NOT NULL) OR (status = 'SKIPPED' AND recommended_track_id IS NULL))

메모: 진행 중 응답은 저장하지 않아요(PL-DGN-001). 여러 번 진단하면 기록이 쌓이고 가장 최근 기록을 써요.

## 3. 콘텐츠

트랙 → 코스 → 수업 → 활동 순서로 중첩돼요.

### track (트랙)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| track_id | BIGINT | N | PK | 자동 증가 | 트랙 번호 |
| name | VARCHAR(30) | N | UK |  | Starter / Controller / Builder / Explorer |
| target | VARCHAR(200) | N |  |  | 대상 학습자 유형 |
| description | TEXT | N |  |  | 트랙 설명 |
| sort_order | INT | N | UK |  | Starter 1, Controller 2, Builder 3, Explorer 4. 진단 동점 시 우선순위 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

### course_category (코스 카테고리)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| course_category_id | BIGINT | N | PK | 자동 증가 | 카테고리 번호 |
| name | VARCHAR(30) | N | UK |  | 카테고리 이름 |
| sort_order | INT | N |  |  | 표시 순서 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 트랙과 따로 둬요(D4). 카테고리별 탐색(FR-CRS-009)은 P2예요.

### course (코스)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| course_id | BIGINT | N | PK | 자동 증가 | 코스 번호 |
| track_id | BIGINT | N | FK → track |  | 코스는 트랙 하나에 속해요 |
| course_category_id | BIGINT | N | FK → course_category |  | 코스는 카테고리 하나에 속해요(FR-CRS-005) |
| title | VARCHAR(100) | N |  |  | 코스명 |
| summary | VARCHAR(200) | N |  |  | 한 줄 소개 |
| description | TEXT | N |  |  | 코스 소개(FR-CRS-005) |
| target | VARCHAR(200) | N |  |  | 학습 대상 |
| goal | TEXT | N |  |  | 학습 목표(FR-CRS-002) |
| plan_days | INT | N |  |  | 며칠 플랜인지 |
| is_public | BOOLEAN | N |  | FALSE | 새 코스는 비공개로 저장(FR-CRS-006) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 공개된 수업이 1개 이상이고 포트폴리오 과제가 있어야 공개할 수 있어요(FR-CRS-006).

### lesson (수업)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| lesson_id | BIGINT | N | PK | 자동 증가 | 수업 번호 |
| course_id | BIGINT | N | FK → course |  | 코스 |
| title | VARCHAR(100) | N |  |  | 수업명 |
| intro | TEXT | N |  |  | 소개 |
| goal | TEXT | N |  |  | 학습 목표 |
| contents | TEXT | N |  |  | 배울 내용 |
| estimated_minutes | INT | N |  |  | 예상 소요 시간(FR-CRS-002) |
| sort_order | INT | N |  |  | 표시 순서 |
| is_public | BOOLEAN | N |  | FALSE | 공개 여부 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 개념 뒤마다 퀴즈가 있고 퀴즈·모의고사 문항이 모두 있어야 공개할 수 있어요(FR-LSN-014).

### activity (활동 · 개념·퀴즈)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| activity_id | BIGINT | N | PK | 자동 증가 | 활동 번호 |
| lesson_id | BIGINT | N | FK → lesson |  | 수업 |
| activity_type | VARCHAR(20) | N |  |  | 활동 종류 (상태값 정의 참고) |
| title | VARCHAR(100) | N |  |  | 활동명 |
| body | TEXT | Y |  |  | 개념 본문. 퀴즈는 비움 |
| body_format | VARCHAR(20) | Y |  |  | 본문 형식. 개념만(FR-LSN-011) (상태값 정의 참고) |
| tool_note_url | VARCHAR(500) | Y |  |  | 도구 노트 링크 |
| estimated_minutes | INT | Y |  |  | 예상 소요 시간(FR-LSN-011) |
| pass_rate | INT | Y |  | 50 | 퀴즈 통과 정답률(%). 개념은 비움(PL-LSN-002) |
| sort_order | INT | N |  |  | 개념과 퀴즈가 번갈아 놓이는 순서 |
| is_active | BOOLEAN | N |  | TRUE | 삭제 대신 비활성화해 진도 기록을 보존해요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_activity_lesson_sort` UNIQUE(lesson_id, sort_order), `ck_activity_type` CHECK(activity_type IN ('CONCEPT', 'QUIZ')), `ck_activity_body_format` CHECK(body_format IS NULL OR body_format IN ('MARKDOWN', 'RICH_TEXT'))

메모: 순서를 바꿀 때 `uk_activity_lesson_sort`가 중간에 충돌할 수 있어요. 임시 값(예: 음수)으로 옮긴 뒤 다시 매기거나 제약 검사를 트랜잭션 끝으로 미뤄요(DBA 공유용 5-3). 게임형 퀴즈는 만들지 않으므로 `activity_type`에 값을 더하지 않아요.

### concept_tag (개념 태그)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| concept_tag_id | BIGINT | N | PK | 자동 증가 | 개념 태그 번호 |
| name | VARCHAR(50) | N | UK |  | 태그 이름 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

### activity_tag (개념 활동의 태그)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| activity_tag_id | BIGINT | N | PK | 자동 증가 | 연결 번호 |
| activity_id | BIGINT | N | FK → activity |  | 개념 활동 (CONCEPT만, 앱에서 검사) |
| concept_tag_id | BIGINT | N | FK → concept_tag |  | 개념 태그 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_activity_tag_activity_tag` UNIQUE(activity_id, concept_tag_id)

메모: 확정안의 컬럼명은 `tag_id`지만, 이름 규칙("FK는 참조 PK 이름")에 따라 `concept_tag_id`로 썼어요.

## 4. 문항

### quiz_question (퀴즈 문항)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| quiz_question_id | BIGINT | N | PK | 자동 증가 | 퀴즈 문항 번호 |
| activity_id | BIGINT | N | FK → activity |  | 퀴즈 활동 (activity_type = QUIZ) |
| concept_tag_id | BIGINT | N | FK → concept_tag |  | 태그별 오답 비율 계산용(FR-LSN-007, 012) |
| display_type | VARCHAR(20) | N |  |  | 표시 유형 (상태값 정의 참고) |
| content | TEXT | N |  |  | 문항 내용 |
| code_snippet | TEXT | Y |  |  | 코드 빈칸 채우기·결과 유추하기 문항의 코드 |
| explanation | TEXT | N |  |  | 해설. 확인을 누르면 보여줘요(PL-LSN-006) |
| sort_order | INT | N |  |  | 표시 순서 |
| is_active | BOOLEAN | N |  | TRUE | 풀이 기록이 생긴 문항은 고치지 않고 비활성화해요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_quiz_question_display_type` CHECK(display_type IN ('CODE_BLANK', 'CONCEPT', 'OUTPUT_PREDICT', 'TRICK')), `ck_quiz_question_code_snippet` CHECK(display_type NOT IN ('CODE_BLANK', 'OUTPUT_PREDICT') OR code_snippet IS NOT NULL)

메모: 퀴즈 하나는 활성 문항 3~5개예요(PL-LSN-003, 앱에서 검사).

### quiz_choice (퀴즈 보기)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| quiz_choice_id | BIGINT | N | PK | 자동 증가 | 퀴즈 보기 번호 |
| quiz_question_id | BIGINT | N | FK → quiz_question |  | 퀴즈 문항 |
| content | VARCHAR(500) | N |  |  | 보기 내용 |
| is_correct | BOOLEAN | N |  |  | 정답 여부 |
| sort_order | INT | N |  |  | 표시 순서 1~4 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_quiz_choice_question_sort` UNIQUE(quiz_question_id, sort_order), `ck_quiz_choice_sort` CHECK(sort_order BETWEEN 1 AND 4)

메모: 4지선다예요. 문항마다 보기 4개, 정답 1개를 앱에서 검사해요. 다시 풀 때 보기 순서를 섞어 보여주고(PL-LSN-005), 저장 순서는 그대로 둬요.

### exam_question (모의고사 문항)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| exam_question_id | BIGINT | N | PK | 자동 증가 | 모의고사 문항 번호 |
| lesson_id | BIGINT | N | FK → lesson |  | 수업 |
| concept_activity_id | BIGINT | N | FK → activity |  | 연관 개념 (CONCEPT만). 없으면 저장 불가(FR-EXM-007) |
| content | TEXT | N |  |  | 문항 내용 |
| explanation | TEXT | N |  |  | 해설. 제출 후 공개해요 |
| sort_order | INT | N |  |  | 표시 순서 |
| is_active | BOOLEAN | N |  | TRUE | 응시 기록이 생긴 문항은 비활성화해요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 연관 개념은 같은 수업의 개념이어야 해요(앱에서 검사).

### exam_choice (모의고사 보기)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| exam_choice_id | BIGINT | N | PK | 자동 증가 | 모의고사 보기 번호 |
| exam_question_id | BIGINT | N | FK → exam_question |  | 모의고사 문항 |
| content | VARCHAR(500) | N |  |  | 보기 내용 |
| is_correct | BOOLEAN | N |  |  | 정답 여부 |
| sort_order | INT | N |  |  | 표시 순서 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

## 5. 수강과 학습 기록

### enrollment (수강)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| enrollment_id | BIGINT | N | PK | 자동 증가 | 수강 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| course_id | BIGINT | N | FK → course |  | 코스 |
| started_at | DATETIME | N |  |  | 수강 시작 일시 |
| portfolio_unlocked_at | DATETIME | Y |  |  | 마지막 수업 리포트 확인으로 포트폴리오가 열린 일시 |
| completed_at | DATETIME | Y |  |  | 코스 완료 일시. 포트폴리오를 지우면 비움(R6, PL-ENR-003) |
| dropped_at | DATETIME | Y |  |  | 수강 포기 일시(FR-ENR-005, P2). 다시 시작하면 비우고 이어서 해요(PL-ENR-004) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_enrollment_member_course` UNIQUE(member_id, course_id)

메모: **수강 상태 컬럼은 두지 않아요.** 수강 중·완료·포기는 `completed_at`과 `dropped_at`으로 판단해요. 이전 정의서의 `status`(IN_PROGRESS / COMPLETED / DROPPED)는 재수강(PL-ENR-004)과 완료 해제(R6)에서 컬럼과 시각이 어긋날 수 있어 뺐어요. 판단 방법은 아래와 같아요.

| 보이는 상태 | 판단 |
| --- | --- |
| 수강 중 | `completed_at IS NULL AND dropped_at IS NULL` |
| 완료 | `completed_at IS NOT NULL` |
| 포기 | `dropped_at IS NOT NULL` |

### lesson_progress (수업 진도)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| lesson_progress_id | BIGINT | N | PK | 자동 증가 | 수업 진도 번호 |
| enrollment_id | BIGINT | N | FK → enrollment |  | 수강 |
| lesson_id | BIGINT | N | FK → lesson |  | 수업. 수강 코스의 수업이어야 해요(앱에서 검사) |
| unlocked_at | DATETIME | Y |  |  | 수업이 열린 일시. 한 번 채우면 유지해요 |
| completed_at | DATETIME | Y |  |  | 모의고사 리포트를 확인한 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_lesson_progress_enrollment_lesson` UNIQUE(enrollment_id, lesson_id)

메모: 수강 시작 시 공개 수업마다 행을 만들고 첫 수업만 열어요. 새 수업이 공개되면 기존 수강에도 행을 추가해요. 완료한 수강은 바로 열고, 진행 중 수강은 바로 앞 수업이 완료됐으면 열어요(PL-ENR-005).

### activity_progress (활동 진도)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| activity_progress_id | BIGINT | N | PK | 자동 증가 | 활동 진도 번호 |
| enrollment_id | BIGINT | N | FK → enrollment |  | 수강 |
| activity_id | BIGINT | N | FK → activity |  | 활동 (개념·퀴즈 모두) |
| unlocked_at | DATETIME | Y |  |  | 활동이 열린 일시. 한 번 채우면 유지해요 |
| completed_at | DATETIME | Y |  |  | 개념은 "읽기 완료"를 누른 일시, 퀴즈는 처음 통과한 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_activity_progress_enrollment_activity` UNIQUE(enrollment_id, activity_id)

메모: 부모가 `lesson_progress`에서 **`enrollment`로 바뀌었어요**(확정안 4-5). 개념을 완료하면 바로 뒤 퀴즈를 열고, 퀴즈를 통과하면 다음 개념을 열어요. 마지막 퀴즈를 통과하면 모의고사가 열린 것으로 봐요. 재응시 점수가 낮아도 다시 잠그지 않아요.

### quiz_attempt (퀴즈 풀이)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| quiz_attempt_id | BIGINT | N | PK | 자동 증가 | 퀴즈 풀이 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| activity_id | BIGINT | N | FK → activity |  | 퀴즈 활동 (activity_type = QUIZ) |
| started_at | DATETIME | N |  |  | 퀴즈 화면에서 풀이를 시작한 일시 |
| correct_rate | DECIMAL(5,2) | Y |  |  | 정답률(%). 제출 시 채워요 |
| is_passed | BOOLEAN | Y |  |  | 풀이 당시 기준 통과 여부. 제출 시 채워요 |
| submitted_at | DATETIME | Y |  |  | 제출 일시. 제출 전이면 비움 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_quiz_attempt_submitted` CHECK(submitted_at IS NULL OR (correct_rate IS NOT NULL AND is_passed IS NOT NULL))

메모: **풀이를 시작할 때** 행을 만들고, 문항에서 "확인"을 누를 때마다 `quiz_answer`를 저장해요(R11). 제출하지 않은 풀이는 점수·통과에 쓰지 않아요. 다시 풀면 새 행을 만들어요. 모든 점수를 보관하고 화면에는 최고 점수를 보여줘요(PL-LSN-007). 확정안의 컬럼명은 `passed`지만 `is_correct`·`is_public`과 맞춰 `is_passed`로 썼어요(아래 확인 항목).

### quiz_answer (퀴즈 답)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| quiz_answer_id | BIGINT | N | PK | 자동 증가 | 퀴즈 답안 번호 |
| quiz_attempt_id | BIGINT | N | FK → quiz_attempt |  | 퀴즈 풀이 |
| quiz_question_id | BIGINT | N | FK → quiz_question |  | 퀴즈 문항 |
| quiz_choice_id | BIGINT | N | FK → quiz_choice |  | 고른 보기. 확인을 누르려면 답을 골라야 해요 |
| is_correct | BOOLEAN | N |  |  | 정답 여부 |
| checked_at | DATETIME | N |  |  | "확인"을 누른 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_quiz_answer_attempt_question` UNIQUE(quiz_attempt_id, quiz_question_id)

메모: 확인한 문항은 답을 바꿀 수 없으므로 행을 수정하지 않아요(PL-LSN-006). 모든 문항에 행이 있어야 제출할 수 있어요(앱에서 검사).

### exam_attempt (모의고사 응시)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| exam_attempt_id | BIGINT | N | PK | 자동 증가 | 모의고사 응시 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| lesson_id | BIGINT | N | FK → lesson |  | 수업 |
| attempt_no | INT | N |  |  | 같은 수업에서 몇 번째 응시인지 |
| started_at | DATETIME | N |  |  | 응시 시작 일시 |
| score | INT | Y |  |  | 점수. 제출 시 채워요 |
| correct_rate | DECIMAL(5,2) | Y |  |  | 정답률(%). 제출 시 채워요 |
| submitted_at | DATETIME | Y |  |  | 제출 일시. 제출 전이면 비움 |
| report_viewed_at | DATETIME | Y |  |  | 리포트 화면을 처음 연 일시(FR-EXM-004, PQ22) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_exam_attempt_member_lesson_no` UNIQUE(member_id, lesson_id, attempt_no), `ck_exam_attempt_submitted` CHECK(submitted_at IS NULL OR (score IS NOT NULL AND correct_rate IS NOT NULL)), `ck_exam_attempt_report` CHECK(report_viewed_at IS NULL OR submitted_at IS NOT NULL)

메모: 제출 전에 나가면 답안이 남아 있고, 돌아오면 같은 응시를 이어서 풀어요(PL-EXM-006, R12). 같은 수업에 제출하지 않은 응시는 하나만 둬요(앱에서 검사). 리포트를 확인한 뒤에는 횟수 제한 없이 다시 응시해요(PL-EXM-003). 리포트를 열면 같은 트랜잭션에서 `lesson_progress.completed_at`을 채우고 다음 수업 또는 포트폴리오를 열어요.

### exam_answer (모의고사 답)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| exam_answer_id | BIGINT | N | PK | 자동 증가 | 모의고사 답안 번호 |
| exam_attempt_id | BIGINT | N | FK → exam_attempt |  | 모의고사 응시 |
| exam_question_id | BIGINT | N | FK → exam_question |  | 모의고사 문항 |
| exam_choice_id | BIGINT | Y | FK → exam_choice |  | 고른 보기. 미응답이면 비움. 제출 전까지 바꿀 수 있어요 |
| is_correct | BOOLEAN | Y |  |  | 정답 여부. 제출 시 채점해 채워요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 답을 고른 마지막 일시 |

제약: `uk_exam_answer_attempt_question` UNIQUE(exam_attempt_id, exam_question_id)

## 6. 포트폴리오

### portfolio_assignment (포트폴리오 과제)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| portfolio_assignment_id | BIGINT | N | PK | 자동 증가 | 과제 번호 |
| course_id | BIGINT | N | FK → course, UK |  | 코스당 과제 1개 |
| topic | VARCHAR(200) | N |  |  | 과제 주제 |
| goal | TEXT | N |  |  | 학습 목표 |
| agent_guide | TEXT | N |  |  | 평가 에이전트 세팅·평가 기준·실행 방법·"실행이 안 될 때" 체크리스트 |
| agent_file_url | VARCHAR(500) | Y |  |  | 평가 에이전트 설정 파일(md·json·yaml·txt, 5MB, PL-PRT-010) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 과제 → 평가 에이전트 자가 점검 → 제출을 한 페이지에서 진행해요(화면 설계 반영 결정 4). 테이블 구조는 같아요.

### guide_step (과제 스텝)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| guide_step_id | BIGINT | N | PK | 자동 증가 | 스텝 번호 |
| portfolio_assignment_id | BIGINT | N | FK → portfolio_assignment |  | 포트폴리오 과제 |
| content | TEXT | N |  |  | 스텝 안내 |
| sort_order | INT | N |  |  | 표시 순서 |
| is_active | BOOLEAN | N |  | TRUE | 체크 기록이 있는 스텝은 삭제 대신 비활성화해요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

### guide_step_check (스텝 완료 체크)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| guide_step_check_id | BIGINT | N | PK | 자동 증가 | 체크 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| guide_step_id | BIGINT | N | FK → guide_step |  | 과제 스텝 |
| checked_at | DATETIME | N |  |  | 체크한 일시. 체크를 풀면 행을 삭제해요 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_guide_step_check_member_step` UNIQUE(member_id, guide_step_id)

### portfolio (포트폴리오)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| portfolio_id | BIGINT | N | PK | 자동 증가 | 포트폴리오 번호 |
| member_id | BIGINT | N | FK → member |  | 회원 |
| course_id | BIGINT | Y | FK → course |  | 코스. 개인 작품은 비움 |
| portfolio_type | VARCHAR(20) | N |  | 'COURSE' | 포트폴리오 종류 (상태값 정의 참고) |
| title | VARCHAR(100) | N |  |  | 제목. 화면 입력은 50자까지(PL-PRT-003) |
| result_url | VARCHAR(500) | N |  |  | 결과물 링크. URL 형식만 검사하고 접속은 확인하지 않아요(PL-PRT-004) |
| summary | VARCHAR(200) | N |  |  | 한 줄 요약(개인 작품은 설명). 화면 입력은 100자까지(PL-PRT-003) |
| retrospective | TEXT | N |  |  | 회고. 100자 이상 2,000자 이하(PL-PRT-003, 앱에서 검사) |
| ai_tools | VARCHAR(200) | N |  |  | 사용한 AI 도구·모델 |
| is_public | BOOLEAN | N |  |  | 공개 여부. 기본값 없이 제출 시 골라요(PL-PRT-006) |
| agent_result | TEXT | Y |  |  | 에이전트 점검 결과. 선택 입력(PL-PRT-002) |
| screenshot_url | VARCHAR(500) | Y |  |  | 스크린샷. 선택 입력(PL-PRT-005) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일. 수정하면 최신본만 남겨요 |
| deleted_at | DATETIME | Y |  |  | 삭제 표시 일시(PL-DAT-002) |

제약: `uk_portfolio_member_course` UNIQUE(member_id, course_id), `ck_portfolio_type` CHECK(portfolio_type IN ('COURSE', 'PERSONAL')), `ck_portfolio_type_course` CHECK((portfolio_type = 'COURSE' AND course_id IS NOT NULL) OR (portfolio_type = 'PERSONAL' AND course_id IS NULL))

메모: 저장 전에 API 키 형식 문자열을 경고하고 가려서 저장해요(NFR-SEC-007, PL-PRT-008). 제출 전 작성 중 내용은 서버에 저장하지 않아요(브라우저 임시 저장, FR-PRT-010). 코스 작품을 삭제하면 해당 수강의 `completed_at`을 비워요(R6). 같은 코스에 다시 제출하면 삭제 표시된 행을 되살려 새 내용으로 덮어써요(UNIQUE 유지). 탈퇴 시 삭제 표시해요(PL-USR-009). 개인 작품은 `course_id`가 비어 있어 UNIQUE에 걸리지 않으므로 여러 개 등록할 수 있어요.

## 7. 커뮤니티

### post (게시글)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| post_id | BIGINT | N | PK | 자동 증가 | 게시글 번호 |
| member_id | BIGINT | N | FK → member |  | 작성자. 탈퇴해도 글은 남고 "탈퇴한 사용자"로 보여요(PL-USR-009) |
| category | VARCHAR(20) | N |  |  | 게시판 구분 (상태값 정의 참고) |
| title | VARCHAR(100) | N |  |  | 제목. 1~100자(PL-CMT-010) |
| body | TEXT | N |  |  | 본문. 1~5,000자(PL-CMT-010, 앱에서 검사) |
| work_url | VARCHAR(500) | Y |  |  | 작품 링크. 작품 공유는 필수(PL-CMT-008) |
| tools | VARCHAR(200) | Y |  |  | 사용한 도구(작품 공유) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |
| deleted_at | DATETIME | Y |  |  | 삭제 표시 일시 |

제약: `ck_post_category` CHECK(category IN ('FREE', 'QNA', 'SHOWCASE')), `ck_post_showcase_work_url` CHECK(category <> 'SHOWCASE' OR work_url IS NOT NULL)

메모: 기본 정렬은 최신순(PL-CMT-005). 비회원은 목록·상세를 볼 수 있고, 작성은 이메일 인증을 마친 회원만 해요(PL-USR-003, PL-USR-008). 확정안의 본문 컬럼명은 `body`라서 이전 정의서의 `content`에서 바꿨어요.

### post_image (게시글 사진)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| post_image_id | BIGINT | N | PK | 자동 증가 | 사진 번호 |
| post_id | BIGINT | N | FK → post |  | 게시글 |
| image_url | VARCHAR(500) | N |  |  | 이미지 주소 |
| sort_order | INT | N |  |  | 표시 순서 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_post_image_post_sort` UNIQUE(post_id, sort_order)

메모: 글당 최대 3장, 장당 5MB, JPG·PNG·WebP(PL-CMT-009, PL-DAT-004, 앱에서 검사).

### comment (댓글·답글)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| comment_id | BIGINT | N | PK | 자동 증가 | 댓글 번호 |
| post_id | BIGINT | N | FK → post |  | 게시글 |
| member_id | BIGINT | N | FK → member |  | 작성자 |
| parent_comment_id | BIGINT | Y | FK → comment |  | 답글이면 원 댓글. 한 단계까지만(앱에서 검사: 부모의 `parent_comment_id`가 비어 있어야 함) |
| content | VARCHAR(500) | N |  |  | 내용. 500자까지(PL-CMT-006) |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일. `created_at`보다 늦으면 "수정됨" 표시 |
| deleted_at | DATETIME | Y |  |  | 삭제 표시 일시. 삭제하면 "삭제된 댓글입니다"로 표시 |

메모: 확정안의 컬럼명은 `parent_id`지만 이름 규칙("FK는 참조 PK 이름")에 따라 `parent_comment_id`로 썼어요.

## 8. 회원 관리 · 개념 확장 · 포트폴리오 확장 (P2)

### member_suspension (경고·이용 정지)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| member_suspension_id | BIGINT | N | PK | 자동 증가 | 제재 번호 |
| member_id | BIGINT | N | FK → member |  | 제재 받은 회원 |
| admin_member_id | BIGINT | N | FK → member |  | 제재한 운영자 |
| level | VARCHAR(20) | N |  |  | 제재 단계 (상태값 정의 참고) |
| reason | TEXT | N |  |  | 제재 사유 |
| starts_at | DATETIME | N |  |  | 제재 시작 일시 |
| ends_at | DATETIME | Y |  |  | 제재 종료 일시. 경고·영구 정지는 비움 |
| released_at | DATETIME | Y |  |  | 운영자가 해제한 일시 |
| releaser_member_id | BIGINT | Y | FK → member |  | 해제한 운영자 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_member_suspension_level` CHECK(level IN ('WARNING', 'DAYS_7', 'DAYS_30', 'PERMANENT')), `ck_member_suspension_period` CHECK((level IN ('DAYS_7', 'DAYS_30') AND ends_at > starts_at) OR (level IN ('WARNING', 'PERMANENT') AND ends_at IS NULL))

메모: 지금 정지 중인지는 저장하지 않고 계산해요(starts_at ≤ 현재 < ends_at 이고 released_at이 비어 있음).

### concept_relation (연관 개념)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| concept_relation_id | BIGINT | N | PK | 자동 증가 | 연관 번호 |
| source_activity_id | BIGINT | N | FK → activity |  | 기준 개념 활동 (CONCEPT만, 앱에서 검사) |
| related_activity_id | BIGINT | N | FK → activity |  | 연관된 개념 활동 (CONCEPT만, 앱에서 검사) |
| sort_order | INT | N |  |  | 표시 순서 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_concept_relation_source_related` UNIQUE(source_activity_id, related_activity_id), `ck_concept_relation_self` CHECK(source_activity_id <> related_activity_id)

### concept_bookmark (개념 북마크)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| concept_bookmark_id | BIGINT | N | PK | 자동 증가 | 북마크 번호 |
| member_id | BIGINT | N | FK → member |  | 북마크한 회원 |
| activity_id | BIGINT | N | FK → activity |  | 북마크한 개념 활동 (CONCEPT만, 앱에서 검사) |
| created_at | DATETIME | N |  | 현재 시각 | 북마크한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_concept_bookmark_member_activity` UNIQUE(member_id, activity_id)

메모: 북마크를 해제하면 행을 지워요(규칙 v2 5장).

### portfolio_faq (포트폴리오 과제 FAQ)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| portfolio_faq_id | BIGINT | N | PK | 자동 증가 | FAQ 번호 |
| portfolio_assignment_id | BIGINT | N | FK → portfolio_assignment |  | 어느 과제의 FAQ인지 |
| question | VARCHAR(300) | N |  |  | 질문 |
| answer | TEXT | N |  |  | 답변 |
| sort_order | INT | N |  |  | 표시 순서 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

## 9. 커뮤니티 확장 (P2)

세 테이블 모두 켜고 끄는 토글이라, 취소하면 행을 지워요(실제 삭제). 추천 수·랭킹은 저장하지 않고 행을 세서 계산해요.

### recommendation (추천)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| recommendation_id | BIGINT | N | PK | 자동 증가 | 추천 번호 |
| member_id | BIGINT | N | FK → member |  | 추천한 회원 |
| post_id | BIGINT | N | FK → post |  | 추천받은 게시글 (작품 공유 SHOWCASE만, 앱에서 검사) |
| created_at | DATETIME | N |  | 현재 시각 | 추천한 일시. 주간 랭킹 집계에 사용 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_recommendation_member_post` UNIQUE(member_id, post_id)

### favorite (즐겨찾기)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| favorite_id | BIGINT | N | PK | 자동 증가 | 즐겨찾기 번호 |
| member_id | BIGINT | N | FK → member |  | 저장한 회원 |
| post_id | BIGINT | N | FK → post |  | 저장한 게시글 |
| created_at | DATETIME | N |  | 현재 시각 | 저장한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_favorite_member_post` UNIQUE(member_id, post_id)

### follow (팔로우)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| follow_id | BIGINT | N | PK | 자동 증가 | 팔로우 번호 |
| follower_member_id | BIGINT | N | FK → member |  | 팔로우하는 회원 |
| following_member_id | BIGINT | N | FK → member |  | 팔로우받는 회원 |
| created_at | DATETIME | N |  | 현재 시각 | 팔로우한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_follow_follower_following` UNIQUE(follower_member_id, following_member_id), `ck_follow_self` CHECK(follower_member_id <> following_member_id)

## 10. 콘테스트 (P2)

진행 상태(출품 중·투표 중·종료)와 득표 수는 저장하지 않고 기간과 투표 행으로 계산해요.

### contest (월간 콘테스트)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| contest_id | BIGINT | N | PK | 자동 증가 | 콘테스트 번호 |
| contest_month | CHAR(7) | N | UK |  | 개최 월 (예: '2026-12'). 확정안의 year_month는 MySQL 예약어라 바꿈 |
| topic | VARCHAR(200) | N |  |  | 이달의 주제 |
| description | TEXT | N |  |  | 주제 설명 |
| entry_starts_at | DATETIME | N |  |  | 출품 시작 |
| entry_ends_at | DATETIME | N |  |  | 출품 마감 |
| vote_starts_at | DATETIME | N |  |  | 투표 시작 |
| vote_ends_at | DATETIME | N |  |  | 투표 마감 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_contest_period` CHECK(entry_starts_at < entry_ends_at AND entry_ends_at <= vote_starts_at AND vote_starts_at < vote_ends_at)

### contest_entry (출품)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| contest_entry_id | BIGINT | N | PK | 자동 증가 | 출품 번호 |
| contest_id | BIGINT | N | FK → contest |  | 출품한 콘테스트 |
| member_id | BIGINT | N | FK → member |  | 출품한 회원 |
| portfolio_id | BIGINT | N | FK → portfolio |  | 출품작 (공개 포트폴리오만, 앱에서 검사) |
| canceled_at | DATETIME | Y |  |  | 출품 취소 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 출품한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_contest_entry_contest_member` UNIQUE(contest_id, member_id) — 1인 1작품(PL-CTS-002)

### contest_vote (투표)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| contest_vote_id | BIGINT | N | PK | 자동 증가 | 투표 번호 |
| contest_id | BIGINT | N | FK → contest |  | 투표한 콘테스트 (하루 1표 검사용) |
| contest_entry_id | BIGINT | N | FK → contest_entry |  | 투표받은 출품작 |
| voter_member_id | BIGINT | N | FK → member |  | 투표한 회원. 자기 작품은 안 됨(앱에서 검사) |
| vote_date | DATE | N |  |  | 투표한 날짜. 확정안의 voted_on을 바꿈 |
| created_at | DATETIME | N |  | 현재 시각 | 투표한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_contest_vote_daily` UNIQUE(contest_id, voter_member_id, vote_date) — 계정당 하루 1표, `uk_contest_vote_entry_voter` UNIQUE(contest_entry_id, voter_member_id) — 같은 작품에 1번

### contest_award (수상)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| contest_award_id | BIGINT | N | PK | 자동 증가 | 수상 번호 |
| contest_id | BIGINT | N | FK → contest |  | 콘테스트 |
| contest_entry_id | BIGINT | N | FK → contest_entry |  | 수상한 출품작 |
| award_rank | INT | N |  |  | 등수 1~3. 동점은 같은 등수로 2명까지 |
| created_at | DATETIME | N |  | 현재 시각 | 수상 확정 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_contest_award_contest_entry` UNIQUE(contest_id, contest_entry_id), `ck_contest_award_rank` CHECK(award_rank BETWEEN 1 AND 3)

메모: 배지 테이블은 따로 없고 이 행으로 배지를 보여줘요.

## 11. 동료 리뷰 (P2)

### review_criterion (평가 기준)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| review_criterion_id | BIGINT | N | PK | 자동 증가 | 평가 기준 번호 |
| portfolio_assignment_id | BIGINT | N | FK → portfolio_assignment |  | 어느 과제의 평가 기준인지 |
| name | VARCHAR(100) | N |  |  | 기준 이름 |
| description | TEXT | Y |  |  | 기준 설명 |
| sort_order | INT | N |  |  | 표시 순서 |
| is_active | BOOLEAN | N |  | TRUE | 사용 여부. 코멘트가 달린 기준은 지우지 않고 끔 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

### review_request (리뷰 요청)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| review_request_id | BIGINT | N | PK | 자동 증가 | 요청 번호 |
| portfolio_id | BIGINT | N | FK → portfolio |  | 리뷰를 요청한 포트폴리오 (공개만, 앱에서 검사) |
| requested_at | DATETIME | N |  | 현재 시각 | 요청 일시 (확정안대로 유지, 규칙 v2 3-3) |
| closed_at | DATETIME | Y |  |  | 직접 마감하거나 비공개로 바꾼 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

메모: 14일 자동 마감은 저장하지 않고 요청 일시로 계산해요. 동시에 3개까지(PL-REV-002)는 앱에서 검사해요.

### peer_review (리뷰)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| peer_review_id | BIGINT | N | PK | 자동 증가 | 리뷰 번호 |
| review_request_id | BIGINT | N | FK → review_request |  | 어느 요청에 단 리뷰인지 |
| reviewer_member_id | BIGINT | N | FK → member |  | 리뷰한 회원. 자기 작품은 안 됨(앱에서 검사) |
| helpful_at | DATETIME | Y |  |  | 요청자가 '도움됨'을 누른 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |
| deleted_at | DATETIME | Y |  |  | 삭제 표시 일시 |

제약: `uk_peer_review_request_reviewer` UNIQUE(review_request_id, reviewer_member_id) — 한 요청에 1인 1리뷰

메모: 리뷰 내용은 기준별로 review_comment에 저장해요.

### review_comment (평가 기준별 코멘트)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| review_comment_id | BIGINT | N | PK | 자동 증가 | 코멘트 번호 |
| peer_review_id | BIGINT | N | FK → peer_review |  | 어느 리뷰의 코멘트인지 |
| review_criterion_id | BIGINT | Y | FK → review_criterion |  | 평가 기준. 개인 작품 리뷰는 기준이 없어 비움 |
| content | TEXT | N |  |  | 코멘트 내용 |
| created_at | DATETIME | N |  | 현재 시각 | 생성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_review_comment_review_criterion` UNIQUE(peer_review_id, review_criterion_id)

메모: review_criterion_id가 비어 있으면 UNIQUE에 걸리지 않아요. 개인 작품 리뷰의 코멘트를 리뷰당 1개로 막는 건 앱에서 해요.

## 12. 소통 · 운영 (P2)

### message (쪽지)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| message_id | BIGINT | N | PK | 자동 증가 | 쪽지 번호 |
| sender_member_id | BIGINT | N | FK → member |  | 보낸 회원 |
| receiver_member_id | BIGINT | N | FK → member |  | 받은 회원 |
| parent_message_id | BIGINT | Y | FK → message |  | 답장이면 원래 쪽지 |
| content | VARCHAR(1000) | N |  |  | 쪽지 내용 (1,000자) |
| read_at | DATETIME | Y |  |  | 받은 사람이 읽은 일시 |
| sender_deleted_at | DATETIME | Y |  |  | 보낸 사람이 지운 일시 |
| receiver_deleted_at | DATETIME | Y |  |  | 받은 사람이 지운 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 보낸 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_message_self` CHECK(sender_member_id <> receiver_member_id)

메모: 보낸 사람과 받은 사람이 따로 지워요. 1년이 지나면 배치로 행을 지워요. 하루 20통, 답장 없는 상대에게 연속 3통 제한(PL-MSG-003)은 앱에서 검사해요.

### notification (알림)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| notification_id | BIGINT | N | PK | 자동 증가 | 알림 번호 |
| member_id | BIGINT | N | FK → member |  | 알림 받는 회원 |
| notification_type | VARCHAR(20) | N |  |  | 알림 종류 (상태값 정의 참고) |
| target_type | VARCHAR(20) | N |  |  | 눌렀을 때 이동할 화면 종류 (상태값 정의 참고) |
| target_id | BIGINT | N |  |  | 이동할 대상 번호. FK 아님 |
| content | VARCHAR(255) | N |  |  | 알림 문구 |
| read_at | DATETIME | Y |  |  | 읽은 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 알림 생성 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_notification_type` CHECK(notification_type IN ('RECOMMEND', 'COMMENT', 'REPLY', 'REVIEW', 'MESSAGE', 'BADGE', 'NOTICE', 'HIDDEN')), `ck_notification_target_type` CHECK(target_type IN ('POST', 'COMMENT', 'PORTFOLIO', 'REVIEW', 'MESSAGE', 'CONTEST_AWARD', 'NOTICE'))

메모: 90일이 지나면 배치로 행을 지워요. 대상이 있는지는 FK가 아니라 앱에서 확인해요.

### notice (공지)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| notice_id | BIGINT | N | PK | 자동 증가 | 공지 번호 |
| admin_member_id | BIGINT | N | FK → member |  | 작성한 운영자 |
| title | VARCHAR(100) | N |  |  | 제목 |
| body | TEXT | N |  |  | 본문 |
| is_banner_shown | BOOLEAN | N |  | FALSE | 상단 배너로 보여줄지 |
| banner_starts_at | DATETIME | Y |  |  | 배너 시작 |
| banner_ends_at | DATETIME | Y |  |  | 배너 종료 |
| created_at | DATETIME | N |  | 현재 시각 | 작성일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `ck_notice_banner` CHECK(is_banner_shown = FALSE OR (banner_starts_at IS NOT NULL AND banner_ends_at > banner_starts_at))

## 13. P3 · 비기능

### report (신고, P3)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| report_id | BIGINT | N | PK | 자동 증가 | 신고 번호 |
| reporter_member_id | BIGINT | N | FK → member |  | 신고한 회원 |
| target_type | VARCHAR(20) | N |  |  | 신고 대상 종류 (상태값 정의 참고) |
| target_id | BIGINT | N |  |  | 신고 대상 번호. FK 아님 |
| reason | VARCHAR(20) | N |  |  | 신고 사유 (상태값 정의 참고) |
| reason_detail | TEXT | Y |  |  | 사유 내용. 기타(ETC)면 필수 |
| status | VARCHAR(20) | N |  | 'PENDING' | 처리 상태 (상태값 정의 참고) |
| handler_member_id | BIGINT | Y | FK → member |  | 처리한 운영자 |
| handled_at | DATETIME | Y |  |  | 처리 일시 |
| created_at | DATETIME | N |  | 현재 시각 | 신고한 일시 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

제약: `uk_report_reporter_target` UNIQUE(reporter_member_id, target_type, target_id) — 같은 대상 1인 1회, `ck_report_target_type` CHECK(target_type IN ('POST', 'PORTFOLIO', 'COMMENT', 'REVIEW', 'MESSAGE')), `ck_report_reason` CHECK(reason IN ('SPAM', 'ABUSE', 'PLAGIARISM', 'PRIVACY', 'ETC')), `ck_report_status` CHECK(status IN ('PENDING', 'HIDDEN', 'REJECTED')), `ck_report_reason_detail` CHECK(reason <> 'ETC' OR reason_detail IS NOT NULL)

메모: 숨김 여부는 저장하지 않고 계산해요(HIDDEN 처리된 신고가 있거나, 서로 다른 3명이 신고함).

### banned_word (금칙어, 비기능)

| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
| --- | --- | --- | --- | --- | --- |
| banned_word_id | BIGINT | N | PK | 자동 증가 | 금칙어 번호 |
| word | VARCHAR(50) | N | UK |  | 금칙어. 닉네임·게시글·댓글·리뷰·쪽지 저장 전에 검사 |
| created_at | DATETIME | N |  | 현재 시각 | 등록일 |
| updated_at | DATETIME | N |  | 현재 시각, 수정 시 갱신 | 수정일 |

## 14. 상태값 정의

51개 테이블의 상태·구분 컬럼이에요. 모두 `VARCHAR(20)` + `CHECK`로 통일했고, 제약 이름은 `ck_테이블_컬럼`이에요. 값은 DBA 공유용 6장의 목록과 같아요.

| 테이블.컬럼 | 기본값 | 값 | 뜻 | 근거 |
| --- | --- | --- | --- | --- |
| member.role | 'LEARNER' | LEARNER | 학습자 | PL-USR-008 |
|  |  | ADMIN | 운영자. DB에 직접 등록 |  |
| member.status | 'ACTIVE' | ACTIVE | 활성 | PL-USR-010 |
|  |  | WITHDRAWN | 탈퇴. email·password_hash를 비우고 nickname을 "탈퇴한 사용자"로 |  |
| social_account.provider | 없음 | GOOGLE | 구글 로그인 | PL-USR-005 |
|  |  | GITHUB | 깃허브 로그인 |  |
| email_verification.purpose | 없음 | SIGNUP | 가입 인증. 링크 24시간 | PL-USR-004 |
|  |  | PASSWORD_RESET | 비밀번호 재설정. 링크 30분, 1회만 |  |
| diagnosis_result.status | 없음 | COMPLETED | 진단 완료. 추천 트랙이 있어야 해요 | PL-DGN-001 |
|  |  | SKIPPED | 건너뜀. 추천 트랙을 비워요 |  |
| activity.activity_type | 없음 | CONCEPT | 개념 학습 활동 | PL-LSN-001 |
|  |  | QUIZ | 퀴즈 활동 |  |
| activity.body_format | 없음 (NULL 허용) | MARKDOWN | 마크다운 본문 | FR-LSN-011 |
|  |  | RICH_TEXT | 리치 텍스트 본문 |  |
| quiz_question.display_type | 없음 | CODE_BLANK | 코드 빈칸 채우기. `code_snippet` 필수 | PL-LSN-003 |
|  |  | CONCEPT | 개념 문제 |  |
|  |  | OUTPUT_PREDICT | 결과 유추하기. `code_snippet` 필수 |  |
|  |  | TRICK | 낚시 문제 |  |
| portfolio.portfolio_type | 'COURSE' | COURSE | 코스 작품. `course_id` 필수 | PL-PRT-001 |
|  |  | PERSONAL | 개인 작품(P2). `course_id` 비움 | PL-CTS-002, FR-PRT-011 |
| post.category | 없음 | FREE | 자유게시판 | PL-CMT-008 |
|  |  | QNA | 질문·답변 |  |
|  |  | SHOWCASE | 작품 공유. `work_url` 필수 |  |
| member_suspension.level | 없음 | WARNING | 경고. 종료 일시 없음 | DBA 공유용 6장 |
|  |  | DAYS_7 | 7일 정지 |  |
|  |  | DAYS_30 | 30일 정지 |  |
|  |  | PERMANENT | 영구 정지. 종료 일시 없음 |  |
| notification.notification_type | 없음 | RECOMMEND | 추천받음 | DBA 공유용 6장 |
|  |  | COMMENT | 댓글 |  |
|  |  | REPLY | 답글 |  |
|  |  | REVIEW | 동료 리뷰 |  |
|  |  | MESSAGE | 쪽지 |  |
|  |  | BADGE | 콘테스트 수상 |  |
|  |  | NOTICE | 공지 |  |
|  |  | HIDDEN | 내 글이 신고로 숨겨짐 |  |
| notification.target_type | 없음 | POST | 게시글 | API 설계 10장 |
|  |  | COMMENT | 댓글 |  |
|  |  | PORTFOLIO | 포트폴리오 |  |
|  |  | REVIEW | 동료 리뷰 (`peer_review`) |  |
|  |  | MESSAGE | 쪽지 |  |
|  |  | CONTEST_AWARD | 콘테스트 수상·배지 (`contest_award_id`) |  |
|  |  | NOTICE | 공지 |  |
| report.target_type | 없음 | POST | 게시글 | DBA 공유용 6장 |
|  |  | PORTFOLIO | 포트폴리오 |  |
|  |  | COMMENT | 댓글 |  |
|  |  | REVIEW | 동료 리뷰 |  |
|  |  | MESSAGE | 쪽지 |  |
| report.reason | 없음 | SPAM | 스팸 | DBA 공유용 6장 |
|  |  | ABUSE | 욕설·비방 |  |
|  |  | PLAGIARISM | 도용 |  |
|  |  | PRIVACY | 개인정보 노출 |  |
|  |  | ETC | 기타. reason_detail 필수 |  |
| report.status | 'PENDING' | PENDING | 처리 대기 | DBA 공유용 6장 |
|  |  | HIDDEN | 숨김 처리 |  |
|  |  | REJECTED | 기각 |  |

상태가 바뀌는 규칙

- `member.status`: 탈퇴하면 ACTIVE → WITHDRAWN이 되고 `withdrawn_at`을 채워요(PL-USR-010). 정지는 상태 컬럼이 아니라 `member_suspension`(P2)으로 계산해요. 정지 중에는 탈퇴할 수 없어요(PL-USR-010).
- `diagnosis_result.status`: 진단을 마치면 COMPLETED + 추천 트랙, 건너뛰면 SKIPPED + 추천 트랙 비움이에요. 중간에 나가면 아무것도 저장하지 않아요(PL-DGN-001).
- `portfolio.portfolio_type`: COURSE는 코스 과제 제출, PERSONAL은 코스와 상관없는 개인 작품이에요. 개인 작품 등록은 P2(FR-PRT-011)라 P1에서는 COURSE만 들어가요.
- 수강 상태는 컬럼이 아니라 `enrollment.completed_at`·`dropped_at`으로 판단해요(5장 표 참고).

## 15. 확정안과의 관계

이 정의서는 **엔티티 확정안(2026-11-04)에 있는 컬럼만** 담았어요. 확정안에 없는 컬럼은 넣지 않았어요.

이전 P1 정의서에 있던 아래 컬럼은 확정안에 없어서 뺐어요.

| 테이블.컬럼 | 뺀 이유 |
| --- | --- |
| member.profile_image_url, member.bio | 확정안에 없음 |
| course.thumbnail_url, course.sort_order | 확정안에 없음 |
| quiz_attempt.correct_count | 확정안에 없음. 필요하면 `correct_rate`로 계산 |
| post.view_count | 확정안에 없음 |
| enrollment.status | 확정안에 없음. `completed_at`·`dropped_at`으로 판단 |

확정안과 다른 점은 **이름**(규칙 v2 4장 변환표)과 **모든 테이블에 `updated_at`을 둔 것**(규칙 v2 3-1, DBA가 정하는 공통 컬럼)뿐이에요.

## 16. 남은 확인 사항

| 항목 | 지금 정의서 | 확인할 것 | 누가 |
| --- | --- | --- | --- |
| 인덱스 | PK·FK·UNIQUE만 적음 | 게시판 최신순, 내 학습, 주간 추천 랭킹 조회에 맞춰 추가 (DBA 공유용 3장 6번) | DBA |
| concept_bookmark 삭제 방식 | 해제 시 행 삭제 | 확정안·DBA 공유용 5-1에 없어 DBA가 정함. 기획 담당에게 공유 | DBA → 기획 |

이미 정해진 것(다시 묻지 않아도 돼요): `is_passed` 이름(규칙 v2 1-1), 모든 테이블의 `updated_at`(규칙 v2 3-1), 시작·확인 시각 컬럼 6개 유지(규칙 v2 3-3), 닉네임 중복은 앱에서 검사(규칙 v2 5-3), 포트폴리오 종류 컬럼은 `portfolio_type`(규칙 v2 4장).

## 17. ERD와 대조할 숫자

ERDCloud에서 Export → SQL Preview로 아래 숫자가 나오면 ERD와 이 정의서가 맞는 거예요.

| 항목 | 개수 |
| --- | --- |
| 테이블 | 51 (P1 31 + P2·P3·비기능 20) |
| FK | 79 (P1 안 44 + P2·P3에서 나가는 35) |
| member를 가리키는 FK | 26 (P1 9 + P2·P3 17) |
| `deleted_at`이 있는 테이블 | 4 (post, comment, portfolio, peer_review) |

## 18. 변경 이력

| 날짜 | 내용 |
| --- | --- |
| 2026-11-05 | 확정안에 없는 컬럼 6개(profile_image_url, bio, thumbnail_url, course.sort_order, correct_count, view_count)를 뺌. 확정안 컬럼만 남김 |
| 2026-11-05 | P1 정의서 v2와 P2·P3 정의서를 51개 한 문서로 합침. P1의 `portfolio.kind`를 규칙 v2대로 `portfolio_type`으로 바꾸고 CHECK 이름을 `ck_portfolio_type`, `ck_portfolio_type_course`로 바꿈. 상태값 정의에 P2·P3 컬럼 5개 추가 |
