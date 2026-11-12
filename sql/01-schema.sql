-- 바이브로그 테이블 생성 (MySQL 8.0, InnoDB, utf8mb4)
-- 기준: docs/design/03-table-definition.md (컬럼·제약), docs/design/02-database.md 6-2 (조회용 인덱스 13개)
-- 몇 번을 실행해도 같은 결과가 나온다. 기존 테이블과 데이터는 모두 지워진다.
-- 실행 순서: 00-create-database.sql(최초 1회) → 01-schema.sql → 02-seed.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS
    member, social_account, email_verification,
    diagnosis_question, diagnosis_option, diagnosis_result,
    track, course_category, course, lesson, activity, concept_tag, activity_tag,
    quiz_question, quiz_choice, exam_question, exam_choice,
    enrollment, lesson_progress, activity_progress, quiz_attempt, quiz_answer, exam_attempt, exam_answer,
    portfolio_assignment, guide_step, guide_step_check, portfolio,
    post, post_image, comment,
    member_suspension, concept_relation, concept_bookmark, portfolio_faq,
    recommendation, favorite, follow,
    contest, contest_entry, contest_vote, contest_award,
    review_criterion, review_request, peer_review, review_comment,
    message, notification, notice,
    report, banned_word, portfolio_template_item;

-- =====================================================================
-- 1. 회원 (P1)
-- =====================================================================

CREATE TABLE member (
    member_id         BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255) NULL,
    password_hash     VARCHAR(255) NULL,
    nickname          VARCHAR(30)  NOT NULL,
    role              VARCHAR(20)  NOT NULL DEFAULT 'LEARNER',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_test           BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verified_at DATETIME     NULL,
    withdrawn_at      DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_member PRIMARY KEY (member_id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT ck_member_role CHECK (role IN ('LEARNER', 'ADMIN')),
    CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'WITHDRAWN')),
    CONSTRAINT ck_member_email_required CHECK (status = 'WITHDRAWN' OR email IS NOT NULL),
    INDEX idx_member_nickname (nickname)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '회원';

CREATE TABLE social_account (
    social_account_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id         BIGINT       NOT NULL,
    provider          VARCHAR(20)  NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_social_account PRIMARY KEY (social_account_id),
    CONSTRAINT fk_social_account_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT uk_social_account_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_social_account_provider CHECK (provider IN ('GOOGLE', 'GITHUB'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '소셜 연결 계정';

CREATE TABLE email_verification (
    email_verification_id BIGINT       NOT NULL AUTO_INCREMENT,
    email                 VARCHAR(255) NOT NULL,
    token_hash            VARCHAR(255) NOT NULL,
    purpose               VARCHAR(20)  NOT NULL,
    expires_at            DATETIME     NOT NULL,
    used_at               DATETIME     NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_email_verification PRIMARY KEY (email_verification_id),
    CONSTRAINT uk_email_verification_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_email_verification_purpose CHECK (purpose IN ('SIGNUP', 'PASSWORD_RESET')),
    INDEX idx_email_verification_email_purpose (email, purpose)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '이메일 인증·비밀번호 재설정';

-- =====================================================================
-- 2. 진단 (P1)
-- =====================================================================

CREATE TABLE diagnosis_question (
    diagnosis_question_id BIGINT       NOT NULL AUTO_INCREMENT,
    content               VARCHAR(500) NOT NULL,
    sort_order            INT          NOT NULL,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_diagnosis_question PRIMARY KEY (diagnosis_question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '진단 문항';

CREATE TABLE diagnosis_option (
    diagnosis_option_id   BIGINT       NOT NULL AUTO_INCREMENT,
    diagnosis_question_id BIGINT       NOT NULL,
    track_id              BIGINT       NOT NULL,
    content               VARCHAR(255) NOT NULL,
    score                 INT          NOT NULL DEFAULT 1,
    sort_order            INT          NOT NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_diagnosis_option PRIMARY KEY (diagnosis_option_id),
    CONSTRAINT fk_diagnosis_option_diagnosis_question_id FOREIGN KEY (diagnosis_question_id) REFERENCES diagnosis_question (diagnosis_question_id) ON DELETE RESTRICT,
    CONSTRAINT fk_diagnosis_option_track_id FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '진단 보기';

CREATE TABLE diagnosis_result (
    diagnosis_result_id  BIGINT      NOT NULL AUTO_INCREMENT,
    member_id            BIGINT      NOT NULL,
    recommended_track_id BIGINT      NULL,
    status               VARCHAR(20) NOT NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_diagnosis_result PRIMARY KEY (diagnosis_result_id),
    CONSTRAINT fk_diagnosis_result_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_diagnosis_result_recommended_track_id FOREIGN KEY (recommended_track_id) REFERENCES track (track_id) ON DELETE RESTRICT,
    CONSTRAINT ck_diagnosis_result_status CHECK (status IN ('COMPLETED', 'SKIPPED')),
    CONSTRAINT ck_diagnosis_result_track CHECK ((status = 'COMPLETED' AND recommended_track_id IS NOT NULL) OR (status = 'SKIPPED' AND recommended_track_id IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '진단 결과';

-- =====================================================================
-- 3. 콘텐츠 (P1)
-- =====================================================================

CREATE TABLE track (
    track_id    BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(30)  NOT NULL,
    target      VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,
    sort_order  INT          NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_track PRIMARY KEY (track_id),
    CONSTRAINT uk_track_name UNIQUE (name),
    CONSTRAINT uk_track_sort_order UNIQUE (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '트랙';

CREATE TABLE course_category (
    course_category_id BIGINT      NOT NULL AUTO_INCREMENT,
    name               VARCHAR(30) NOT NULL,
    sort_order         INT         NOT NULL,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_course_category PRIMARY KEY (course_category_id),
    CONSTRAINT uk_course_category_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '코스 카테고리';

CREATE TABLE course (
    course_id          BIGINT       NOT NULL AUTO_INCREMENT,
    track_id           BIGINT       NOT NULL,
    course_category_id BIGINT       NOT NULL,
    title              VARCHAR(100) NOT NULL,
    summary            VARCHAR(200) NOT NULL,
    description        TEXT         NOT NULL,
    target             VARCHAR(200) NOT NULL,
    goal               TEXT         NOT NULL,
    plan_days          INT          NOT NULL,
    is_public          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_course PRIMARY KEY (course_id),
    CONSTRAINT fk_course_track_id FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_course_category_id FOREIGN KEY (course_category_id) REFERENCES course_category (course_category_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '코스';

CREATE TABLE lesson (
    lesson_id         BIGINT       NOT NULL AUTO_INCREMENT,
    course_id         BIGINT       NOT NULL,
    title             VARCHAR(100) NOT NULL,
    intro             TEXT         NOT NULL,
    goal              TEXT         NOT NULL,
    contents          TEXT         NOT NULL,
    estimated_minutes INT          NOT NULL,
    sort_order        INT          NOT NULL,
    is_public         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_lesson PRIMARY KEY (lesson_id),
    CONSTRAINT fk_lesson_course_id FOREIGN KEY (course_id) REFERENCES course (course_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '수업';

CREATE TABLE activity (
    activity_id       BIGINT       NOT NULL AUTO_INCREMENT,
    lesson_id         BIGINT       NOT NULL,
    activity_type     VARCHAR(20)  NOT NULL,
    title             VARCHAR(100) NOT NULL,
    body              TEXT         NULL,
    body_format       VARCHAR(20)  NULL,
    tool_note_url     VARCHAR(500) NULL,
    estimated_minutes INT          NULL,
    pass_rate         INT          NULL DEFAULT 50,
    sort_order        INT          NOT NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_activity PRIMARY KEY (activity_id),
    CONSTRAINT fk_activity_lesson_id FOREIGN KEY (lesson_id) REFERENCES lesson (lesson_id) ON DELETE RESTRICT,
    CONSTRAINT uk_activity_lesson_sort UNIQUE (lesson_id, sort_order),
    CONSTRAINT ck_activity_type CHECK (activity_type IN ('CONCEPT', 'QUIZ')),
    CONSTRAINT ck_activity_body_format CHECK (body_format IS NULL OR body_format IN ('MARKDOWN', 'RICH_TEXT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '활동 (개념·퀴즈)';

CREATE TABLE concept_tag (
    concept_tag_id BIGINT      NOT NULL AUTO_INCREMENT,
    name           VARCHAR(50) NOT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_concept_tag PRIMARY KEY (concept_tag_id),
    CONSTRAINT uk_concept_tag_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '개념 태그';

CREATE TABLE activity_tag (
    activity_tag_id BIGINT   NOT NULL AUTO_INCREMENT,
    activity_id     BIGINT   NOT NULL,
    concept_tag_id  BIGINT   NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_activity_tag PRIMARY KEY (activity_tag_id),
    CONSTRAINT fk_activity_tag_activity_id FOREIGN KEY (activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_tag_concept_tag_id FOREIGN KEY (concept_tag_id) REFERENCES concept_tag (concept_tag_id) ON DELETE RESTRICT,
    CONSTRAINT uk_activity_tag_activity_tag UNIQUE (activity_id, concept_tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '개념 활동의 태그';

-- =====================================================================
-- 4. 문항 (P1)
-- =====================================================================

CREATE TABLE quiz_question (
    quiz_question_id BIGINT      NOT NULL AUTO_INCREMENT,
    activity_id      BIGINT      NOT NULL,
    concept_tag_id   BIGINT      NOT NULL,
    display_type     VARCHAR(20) NOT NULL,
    content          TEXT        NOT NULL,
    code_snippet     TEXT        NULL,
    explanation      TEXT        NOT NULL,
    sort_order       INT         NOT NULL,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_quiz_question PRIMARY KEY (quiz_question_id),
    CONSTRAINT fk_quiz_question_activity_id FOREIGN KEY (activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT fk_quiz_question_concept_tag_id FOREIGN KEY (concept_tag_id) REFERENCES concept_tag (concept_tag_id) ON DELETE RESTRICT,
    CONSTRAINT ck_quiz_question_display_type CHECK (display_type IN ('CODE_BLANK', 'CONCEPT', 'OUTPUT_PREDICT', 'TRICK')),
    CONSTRAINT ck_quiz_question_code_snippet CHECK (display_type NOT IN ('CODE_BLANK', 'OUTPUT_PREDICT') OR code_snippet IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '퀴즈 문항';

CREATE TABLE quiz_choice (
    quiz_choice_id   BIGINT       NOT NULL AUTO_INCREMENT,
    quiz_question_id BIGINT       NOT NULL,
    content          VARCHAR(500) NOT NULL,
    is_correct       BOOLEAN      NOT NULL,
    sort_order       INT          NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_quiz_choice PRIMARY KEY (quiz_choice_id),
    CONSTRAINT fk_quiz_choice_quiz_question_id FOREIGN KEY (quiz_question_id) REFERENCES quiz_question (quiz_question_id) ON DELETE RESTRICT,
    CONSTRAINT uk_quiz_choice_question_sort UNIQUE (quiz_question_id, sort_order),
    CONSTRAINT ck_quiz_choice_sort CHECK (sort_order BETWEEN 1 AND 4)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '퀴즈 보기';

CREATE TABLE exam_question (
    exam_question_id    BIGINT   NOT NULL AUTO_INCREMENT,
    lesson_id           BIGINT   NOT NULL,
    concept_activity_id BIGINT   NOT NULL,
    content             TEXT     NOT NULL,
    explanation         TEXT     NOT NULL,
    sort_order          INT      NOT NULL,
    is_active           BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_exam_question PRIMARY KEY (exam_question_id),
    CONSTRAINT fk_exam_question_lesson_id FOREIGN KEY (lesson_id) REFERENCES lesson (lesson_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_question_concept_activity_id FOREIGN KEY (concept_activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '모의고사 문항';

CREATE TABLE exam_choice (
    exam_choice_id   BIGINT       NOT NULL AUTO_INCREMENT,
    exam_question_id BIGINT       NOT NULL,
    content          VARCHAR(500) NOT NULL,
    is_correct       BOOLEAN      NOT NULL,
    sort_order       INT          NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_exam_choice PRIMARY KEY (exam_choice_id),
    CONSTRAINT fk_exam_choice_exam_question_id FOREIGN KEY (exam_question_id) REFERENCES exam_question (exam_question_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '모의고사 보기';

-- =====================================================================
-- 5. 수강과 학습 기록 (P1)
-- =====================================================================

CREATE TABLE enrollment (
    enrollment_id         BIGINT   NOT NULL AUTO_INCREMENT,
    member_id             BIGINT   NOT NULL,
    course_id             BIGINT   NOT NULL,
    started_at            DATETIME NOT NULL,
    portfolio_unlocked_at DATETIME NULL,
    completed_at          DATETIME NULL,
    dropped_at            DATETIME NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_enrollment PRIMARY KEY (enrollment_id),
    CONSTRAINT fk_enrollment_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_enrollment_course_id FOREIGN KEY (course_id) REFERENCES course (course_id) ON DELETE RESTRICT,
    CONSTRAINT uk_enrollment_member_course UNIQUE (member_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '수강';

CREATE TABLE lesson_progress (
    lesson_progress_id BIGINT   NOT NULL AUTO_INCREMENT,
    enrollment_id      BIGINT   NOT NULL,
    lesson_id          BIGINT   NOT NULL,
    unlocked_at        DATETIME NULL,
    completed_at       DATETIME NULL,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_lesson_progress PRIMARY KEY (lesson_progress_id),
    CONSTRAINT fk_lesson_progress_enrollment_id FOREIGN KEY (enrollment_id) REFERENCES enrollment (enrollment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_progress_lesson_id FOREIGN KEY (lesson_id) REFERENCES lesson (lesson_id) ON DELETE RESTRICT,
    CONSTRAINT uk_lesson_progress_enrollment_lesson UNIQUE (enrollment_id, lesson_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '수업 진도';

CREATE TABLE activity_progress (
    activity_progress_id BIGINT   NOT NULL AUTO_INCREMENT,
    enrollment_id        BIGINT   NOT NULL,
    activity_id          BIGINT   NOT NULL,
    unlocked_at          DATETIME NULL,
    completed_at         DATETIME NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_activity_progress PRIMARY KEY (activity_progress_id),
    CONSTRAINT fk_activity_progress_enrollment_id FOREIGN KEY (enrollment_id) REFERENCES enrollment (enrollment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_progress_activity_id FOREIGN KEY (activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT uk_activity_progress_enrollment_activity UNIQUE (enrollment_id, activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '활동 진도';

CREATE TABLE quiz_attempt (
    quiz_attempt_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id       BIGINT       NOT NULL,
    activity_id     BIGINT       NOT NULL,
    started_at      DATETIME     NOT NULL,
    correct_rate    DECIMAL(5,2) NULL,
    is_passed       BOOLEAN      NULL,
    submitted_at    DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_quiz_attempt PRIMARY KEY (quiz_attempt_id),
    CONSTRAINT fk_quiz_attempt_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_quiz_attempt_activity_id FOREIGN KEY (activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT ck_quiz_attempt_submitted CHECK (submitted_at IS NULL OR (correct_rate IS NOT NULL AND is_passed IS NOT NULL)),
    INDEX idx_quiz_attempt_member_id_activity_id (member_id, activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '퀴즈 풀이';

CREATE TABLE quiz_answer (
    quiz_answer_id   BIGINT   NOT NULL AUTO_INCREMENT,
    quiz_attempt_id  BIGINT   NOT NULL,
    quiz_question_id BIGINT   NOT NULL,
    quiz_choice_id   BIGINT   NOT NULL,
    is_correct       BOOLEAN  NOT NULL,
    checked_at       DATETIME NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_quiz_answer PRIMARY KEY (quiz_answer_id),
    CONSTRAINT fk_quiz_answer_quiz_attempt_id FOREIGN KEY (quiz_attempt_id) REFERENCES quiz_attempt (quiz_attempt_id) ON DELETE RESTRICT,
    CONSTRAINT fk_quiz_answer_quiz_question_id FOREIGN KEY (quiz_question_id) REFERENCES quiz_question (quiz_question_id) ON DELETE RESTRICT,
    CONSTRAINT fk_quiz_answer_quiz_choice_id FOREIGN KEY (quiz_choice_id) REFERENCES quiz_choice (quiz_choice_id) ON DELETE RESTRICT,
    CONSTRAINT uk_quiz_answer_attempt_question UNIQUE (quiz_attempt_id, quiz_question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '퀴즈 답';

CREATE TABLE exam_attempt (
    exam_attempt_id  BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    lesson_id        BIGINT       NOT NULL,
    attempt_no       INT          NOT NULL,
    started_at       DATETIME     NOT NULL,
    score            INT          NULL,
    correct_rate     DECIMAL(5,2) NULL,
    submitted_at     DATETIME     NULL,
    report_viewed_at DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_exam_attempt PRIMARY KEY (exam_attempt_id),
    CONSTRAINT fk_exam_attempt_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_attempt_lesson_id FOREIGN KEY (lesson_id) REFERENCES lesson (lesson_id) ON DELETE RESTRICT,
    CONSTRAINT uk_exam_attempt_member_lesson_no UNIQUE (member_id, lesson_id, attempt_no),
    CONSTRAINT ck_exam_attempt_submitted CHECK (submitted_at IS NULL OR (score IS NOT NULL AND correct_rate IS NOT NULL)),
    CONSTRAINT ck_exam_attempt_report CHECK (report_viewed_at IS NULL OR submitted_at IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '모의고사 응시';

CREATE TABLE exam_answer (
    exam_answer_id   BIGINT   NOT NULL AUTO_INCREMENT,
    exam_attempt_id  BIGINT   NOT NULL,
    exam_question_id BIGINT   NOT NULL,
    exam_choice_id   BIGINT   NULL,
    is_correct       BOOLEAN  NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_exam_answer PRIMARY KEY (exam_answer_id),
    CONSTRAINT fk_exam_answer_exam_attempt_id FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempt (exam_attempt_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_answer_exam_question_id FOREIGN KEY (exam_question_id) REFERENCES exam_question (exam_question_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_answer_exam_choice_id FOREIGN KEY (exam_choice_id) REFERENCES exam_choice (exam_choice_id) ON DELETE RESTRICT,
    CONSTRAINT uk_exam_answer_attempt_question UNIQUE (exam_attempt_id, exam_question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '모의고사 답';

-- =====================================================================
-- 6. 포트폴리오 (P1)
-- =====================================================================

CREATE TABLE portfolio_assignment (
    portfolio_assignment_id BIGINT       NOT NULL AUTO_INCREMENT,
    course_id               BIGINT       NOT NULL,
    topic                   VARCHAR(200) NOT NULL,
    goal                    TEXT         NOT NULL,
    agent_guide             TEXT         NOT NULL,
    agent_file_url          VARCHAR(500) NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_portfolio_assignment PRIMARY KEY (portfolio_assignment_id),
    CONSTRAINT fk_portfolio_assignment_course_id FOREIGN KEY (course_id) REFERENCES course (course_id) ON DELETE RESTRICT,
    CONSTRAINT uk_portfolio_assignment_course_id UNIQUE (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '포트폴리오 과제';

CREATE TABLE guide_step (
    guide_step_id           BIGINT   NOT NULL AUTO_INCREMENT,
    portfolio_assignment_id BIGINT   NOT NULL,
    content                 TEXT     NOT NULL,
    sort_order              INT      NOT NULL,
    is_active               BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_guide_step PRIMARY KEY (guide_step_id),
    CONSTRAINT fk_guide_step_portfolio_assignment_id FOREIGN KEY (portfolio_assignment_id) REFERENCES portfolio_assignment (portfolio_assignment_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '과제 스텝';

CREATE TABLE guide_step_check (
    guide_step_check_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id           BIGINT   NOT NULL,
    guide_step_id       BIGINT   NOT NULL,
    checked_at          DATETIME NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_guide_step_check PRIMARY KEY (guide_step_check_id),
    CONSTRAINT fk_guide_step_check_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_guide_step_check_guide_step_id FOREIGN KEY (guide_step_id) REFERENCES guide_step (guide_step_id) ON DELETE RESTRICT,
    CONSTRAINT uk_guide_step_check_member_step UNIQUE (member_id, guide_step_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '스텝 완료 체크';

CREATE TABLE portfolio (
    portfolio_id   BIGINT       NOT NULL AUTO_INCREMENT,
    member_id      BIGINT       NOT NULL,
    course_id      BIGINT       NULL,
    portfolio_type VARCHAR(20)  NOT NULL DEFAULT 'COURSE',
    title          VARCHAR(100) NOT NULL,
    result_url     VARCHAR(500) NOT NULL,
    summary        VARCHAR(200) NOT NULL,
    retrospective  TEXT         NOT NULL,
    ai_tools       VARCHAR(200) NOT NULL,
    is_public      BOOLEAN      NOT NULL,
    agent_result   TEXT         NULL,
    screenshot_url VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     DATETIME     NULL,
    CONSTRAINT pk_portfolio PRIMARY KEY (portfolio_id),
    CONSTRAINT fk_portfolio_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_portfolio_course_id FOREIGN KEY (course_id) REFERENCES course (course_id) ON DELETE RESTRICT,
    CONSTRAINT uk_portfolio_member_course UNIQUE (member_id, course_id),
    CONSTRAINT ck_portfolio_type CHECK (portfolio_type IN ('COURSE', 'PERSONAL')),
    CONSTRAINT ck_portfolio_type_course CHECK ((portfolio_type = 'COURSE' AND course_id IS NOT NULL) OR (portfolio_type = 'PERSONAL' AND course_id IS NULL)),
    INDEX idx_portfolio_screenshot_url (screenshot_url)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '포트폴리오';

-- =====================================================================
-- 7. 커뮤니티 (P1)
-- =====================================================================

CREATE TABLE post (
    post_id    BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    category   VARCHAR(20)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    body       TEXT         NOT NULL,
    work_url   VARCHAR(500) NULL,
    tools      VARCHAR(200) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME     NULL,
    CONSTRAINT pk_post PRIMARY KEY (post_id),
    CONSTRAINT fk_post_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT ck_post_category CHECK (category IN ('FREE', 'QNA', 'SHOWCASE')),
    CONSTRAINT ck_post_showcase_work_url CHECK (category <> 'SHOWCASE' OR work_url IS NOT NULL),
    INDEX idx_post_category_created_at (category, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '게시글';

CREATE TABLE post_image (
    post_image_id BIGINT       NOT NULL AUTO_INCREMENT,
    post_id       BIGINT       NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    sort_order    INT          NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_post_image PRIMARY KEY (post_image_id),
    CONSTRAINT fk_post_image_post_id FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE RESTRICT,
    CONSTRAINT uk_post_image_post_sort UNIQUE (post_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '게시글 사진';

CREATE TABLE comment (
    comment_id        BIGINT       NOT NULL AUTO_INCREMENT,
    post_id           BIGINT       NOT NULL,
    member_id         BIGINT       NOT NULL,
    parent_comment_id BIGINT       NULL,
    content           VARCHAR(500) NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME     NULL,
    CONSTRAINT pk_comment PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_post_id FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE RESTRICT,
    CONSTRAINT fk_comment_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_comment_parent_comment_id FOREIGN KEY (parent_comment_id) REFERENCES comment (comment_id) ON DELETE RESTRICT,
    INDEX idx_comment_post_id_created_at (post_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '댓글·답글';

-- =====================================================================
-- 8. 회원 관리 · 개념 확장 · 포트폴리오 확장 (P2)
-- =====================================================================

CREATE TABLE member_suspension (
    member_suspension_id BIGINT      NOT NULL AUTO_INCREMENT,
    member_id            BIGINT      NOT NULL,
    admin_member_id      BIGINT      NOT NULL,
    level                VARCHAR(20) NOT NULL,
    reason               TEXT        NOT NULL,
    starts_at            DATETIME    NOT NULL,
    ends_at              DATETIME    NULL,
    released_at          DATETIME    NULL,
    releaser_member_id   BIGINT      NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_member_suspension PRIMARY KEY (member_suspension_id),
    CONSTRAINT fk_member_suspension_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_member_suspension_admin_member_id FOREIGN KEY (admin_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_member_suspension_releaser_member_id FOREIGN KEY (releaser_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT ck_member_suspension_level CHECK (level IN ('WARNING', 'DAYS_7', 'DAYS_30', 'PERMANENT')),
    CONSTRAINT ck_member_suspension_period CHECK ((level IN ('DAYS_7', 'DAYS_30') AND ends_at > starts_at) OR (level IN ('WARNING', 'PERMANENT') AND ends_at IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '경고·이용 정지';

CREATE TABLE concept_relation (
    concept_relation_id BIGINT   NOT NULL AUTO_INCREMENT,
    source_activity_id  BIGINT   NOT NULL,
    related_activity_id BIGINT   NOT NULL,
    sort_order          INT      NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_concept_relation PRIMARY KEY (concept_relation_id),
    CONSTRAINT fk_concept_relation_source_activity_id FOREIGN KEY (source_activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT fk_concept_relation_related_activity_id FOREIGN KEY (related_activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT uk_concept_relation_source_related UNIQUE (source_activity_id, related_activity_id),
    CONSTRAINT ck_concept_relation_self CHECK (source_activity_id <> related_activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '연관 개념';

CREATE TABLE concept_bookmark (
    concept_bookmark_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id           BIGINT   NOT NULL,
    activity_id         BIGINT   NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_concept_bookmark PRIMARY KEY (concept_bookmark_id),
    CONSTRAINT fk_concept_bookmark_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_concept_bookmark_activity_id FOREIGN KEY (activity_id) REFERENCES activity (activity_id) ON DELETE RESTRICT,
    CONSTRAINT uk_concept_bookmark_member_activity UNIQUE (member_id, activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '개념 북마크';

CREATE TABLE portfolio_faq (
    portfolio_faq_id        BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_assignment_id BIGINT       NOT NULL,
    question                VARCHAR(300) NOT NULL,
    answer                  TEXT         NOT NULL,
    sort_order              INT          NOT NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_portfolio_faq PRIMARY KEY (portfolio_faq_id),
    CONSTRAINT fk_portfolio_faq_portfolio_assignment_id FOREIGN KEY (portfolio_assignment_id) REFERENCES portfolio_assignment (portfolio_assignment_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '포트폴리오 과제 FAQ';

-- =====================================================================
-- 9. 커뮤니티 확장 (P2)
-- =====================================================================

CREATE TABLE recommendation (
    recommendation_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id         BIGINT   NOT NULL,
    post_id           BIGINT   NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_recommendation PRIMARY KEY (recommendation_id),
    CONSTRAINT fk_recommendation_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recommendation_post_id FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE RESTRICT,
    CONSTRAINT uk_recommendation_member_post UNIQUE (member_id, post_id),
    INDEX idx_recommendation_created_at_post_id (created_at, post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '추천';

CREATE TABLE favorite (
    favorite_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id   BIGINT   NOT NULL,
    post_id     BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_favorite PRIMARY KEY (favorite_id),
    CONSTRAINT fk_favorite_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_favorite_post_id FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE RESTRICT,
    CONSTRAINT uk_favorite_member_post UNIQUE (member_id, post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '즐겨찾기';

CREATE TABLE follow (
    follow_id           BIGINT   NOT NULL AUTO_INCREMENT,
    follower_member_id  BIGINT   NOT NULL,
    following_member_id BIGINT   NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_follow PRIMARY KEY (follow_id),
    CONSTRAINT fk_follow_follower_member_id FOREIGN KEY (follower_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_follow_following_member_id FOREIGN KEY (following_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT uk_follow_follower_following UNIQUE (follower_member_id, following_member_id),
    CONSTRAINT ck_follow_self CHECK (follower_member_id <> following_member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '팔로우';

-- =====================================================================
-- 10. 콘테스트 (P2)
-- =====================================================================

CREATE TABLE contest (
    contest_id      BIGINT       NOT NULL AUTO_INCREMENT,
    contest_month   CHAR(7)      NOT NULL,
    topic           VARCHAR(200) NOT NULL,
    description     TEXT         NOT NULL,
    entry_starts_at DATETIME     NOT NULL,
    entry_ends_at   DATETIME     NOT NULL,
    vote_starts_at  DATETIME     NOT NULL,
    vote_ends_at    DATETIME     NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_contest PRIMARY KEY (contest_id),
    CONSTRAINT uk_contest_contest_month UNIQUE (contest_month),
    CONSTRAINT ck_contest_period CHECK (entry_starts_at < entry_ends_at AND entry_ends_at <= vote_starts_at AND vote_starts_at < vote_ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '월간 콘테스트';

CREATE TABLE contest_entry (
    contest_entry_id BIGINT   NOT NULL AUTO_INCREMENT,
    contest_id       BIGINT   NOT NULL,
    member_id        BIGINT   NOT NULL,
    portfolio_id     BIGINT   NOT NULL,
    canceled_at      DATETIME NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_contest_entry PRIMARY KEY (contest_entry_id),
    CONSTRAINT fk_contest_entry_contest_id FOREIGN KEY (contest_id) REFERENCES contest (contest_id) ON DELETE RESTRICT,
    CONSTRAINT fk_contest_entry_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_contest_entry_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES portfolio (portfolio_id) ON DELETE RESTRICT,
    CONSTRAINT uk_contest_entry_contest_member UNIQUE (contest_id, member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '출품';

CREATE TABLE contest_vote (
    contest_vote_id  BIGINT   NOT NULL AUTO_INCREMENT,
    contest_id       BIGINT   NOT NULL,
    contest_entry_id BIGINT   NOT NULL,
    voter_member_id  BIGINT   NOT NULL,
    vote_date        DATE     NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_contest_vote PRIMARY KEY (contest_vote_id),
    CONSTRAINT fk_contest_vote_contest_id FOREIGN KEY (contest_id) REFERENCES contest (contest_id) ON DELETE RESTRICT,
    CONSTRAINT fk_contest_vote_contest_entry_id FOREIGN KEY (contest_entry_id) REFERENCES contest_entry (contest_entry_id) ON DELETE RESTRICT,
    CONSTRAINT fk_contest_vote_voter_member_id FOREIGN KEY (voter_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT uk_contest_vote_daily UNIQUE (contest_id, voter_member_id, vote_date),
    CONSTRAINT uk_contest_vote_entry_voter UNIQUE (contest_entry_id, voter_member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '투표';

CREATE TABLE contest_award (
    contest_award_id BIGINT   NOT NULL AUTO_INCREMENT,
    contest_id       BIGINT   NOT NULL,
    contest_entry_id BIGINT   NOT NULL,
    award_rank       INT      NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_contest_award PRIMARY KEY (contest_award_id),
    CONSTRAINT fk_contest_award_contest_id FOREIGN KEY (contest_id) REFERENCES contest (contest_id) ON DELETE RESTRICT,
    CONSTRAINT fk_contest_award_contest_entry_id FOREIGN KEY (contest_entry_id) REFERENCES contest_entry (contest_entry_id) ON DELETE RESTRICT,
    CONSTRAINT uk_contest_award_contest_entry UNIQUE (contest_id, contest_entry_id),
    CONSTRAINT ck_contest_award_rank CHECK (award_rank BETWEEN 1 AND 3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '수상';

-- =====================================================================
-- 11. 동료 리뷰 (P2)
-- =====================================================================

CREATE TABLE review_criterion (
    review_criterion_id     BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_assignment_id BIGINT       NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    description             TEXT         NULL,
    sort_order              INT          NOT NULL,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_review_criterion PRIMARY KEY (review_criterion_id),
    CONSTRAINT fk_review_criterion_portfolio_assignment_id FOREIGN KEY (portfolio_assignment_id) REFERENCES portfolio_assignment (portfolio_assignment_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '평가 기준';

CREATE TABLE review_request (
    review_request_id BIGINT   NOT NULL AUTO_INCREMENT,
    portfolio_id      BIGINT   NOT NULL,
    requested_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at         DATETIME NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_review_request PRIMARY KEY (review_request_id),
    CONSTRAINT fk_review_request_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES portfolio (portfolio_id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '리뷰 요청';

CREATE TABLE peer_review (
    peer_review_id     BIGINT   NOT NULL AUTO_INCREMENT,
    review_request_id  BIGINT   NOT NULL,
    reviewer_member_id BIGINT   NOT NULL,
    helpful_at         DATETIME NULL,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         DATETIME NULL,
    CONSTRAINT pk_peer_review PRIMARY KEY (peer_review_id),
    CONSTRAINT fk_peer_review_review_request_id FOREIGN KEY (review_request_id) REFERENCES review_request (review_request_id) ON DELETE RESTRICT,
    CONSTRAINT fk_peer_review_reviewer_member_id FOREIGN KEY (reviewer_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT uk_peer_review_request_reviewer UNIQUE (review_request_id, reviewer_member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '리뷰';

CREATE TABLE review_comment (
    review_comment_id   BIGINT   NOT NULL AUTO_INCREMENT,
    peer_review_id      BIGINT   NOT NULL,
    review_criterion_id BIGINT   NULL,
    content             TEXT     NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_review_comment PRIMARY KEY (review_comment_id),
    CONSTRAINT fk_review_comment_peer_review_id FOREIGN KEY (peer_review_id) REFERENCES peer_review (peer_review_id) ON DELETE RESTRICT,
    CONSTRAINT fk_review_comment_review_criterion_id FOREIGN KEY (review_criterion_id) REFERENCES review_criterion (review_criterion_id) ON DELETE RESTRICT,
    CONSTRAINT uk_review_comment_review_criterion UNIQUE (peer_review_id, review_criterion_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '평가 기준별 코멘트';

-- =====================================================================
-- 12. 소통 · 운영 (P2)
-- =====================================================================

CREATE TABLE message (
    message_id          BIGINT        NOT NULL AUTO_INCREMENT,
    sender_member_id    BIGINT        NOT NULL,
    receiver_member_id  BIGINT        NOT NULL,
    parent_message_id   BIGINT        NULL,
    content             VARCHAR(1000) NOT NULL,
    read_at             DATETIME      NULL,
    sender_deleted_at   DATETIME      NULL,
    receiver_deleted_at DATETIME      NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_message PRIMARY KEY (message_id),
    CONSTRAINT fk_message_sender_member_id FOREIGN KEY (sender_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_message_receiver_member_id FOREIGN KEY (receiver_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_message_parent_message_id FOREIGN KEY (parent_message_id) REFERENCES message (message_id) ON DELETE RESTRICT,
    CONSTRAINT ck_message_self CHECK (sender_member_id <> receiver_member_id),
    INDEX idx_message_receiver_member_id_created_at (receiver_member_id, created_at),
    INDEX idx_message_sender_member_id_created_at (sender_member_id, created_at),
    INDEX idx_message_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '쪽지';

CREATE TABLE notification (
    notification_id   BIGINT       NOT NULL AUTO_INCREMENT,
    member_id         BIGINT       NOT NULL,
    notification_type VARCHAR(20)  NOT NULL,
    target_type       VARCHAR(20)  NOT NULL,
    target_id         BIGINT       NOT NULL,
    content           VARCHAR(255) NOT NULL,
    read_at           DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_member_id FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT ck_notification_type CHECK (notification_type IN ('RECOMMEND', 'COMMENT', 'REPLY', 'REVIEW', 'MESSAGE', 'BADGE', 'NOTICE', 'HIDDEN')),
    CONSTRAINT ck_notification_target_type CHECK (target_type IN ('POST', 'COMMENT', 'PORTFOLIO', 'REVIEW', 'MESSAGE', 'CONTEST_AWARD', 'NOTICE')),
    INDEX idx_notification_member_id_read_at (member_id, read_at, created_at),
    INDEX idx_notification_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '알림';

CREATE TABLE notice (
    notice_id        BIGINT       NOT NULL AUTO_INCREMENT,
    admin_member_id  BIGINT       NOT NULL,
    title            VARCHAR(100) NOT NULL,
    body             TEXT         NOT NULL,
    is_banner_shown  BOOLEAN      NOT NULL DEFAULT FALSE,
    banner_starts_at DATETIME     NULL,
    banner_ends_at   DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_notice PRIMARY KEY (notice_id),
    CONSTRAINT fk_notice_admin_member_id FOREIGN KEY (admin_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT ck_notice_banner CHECK (is_banner_shown = FALSE OR (banner_starts_at IS NOT NULL AND banner_ends_at > banner_starts_at))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공지';

-- =====================================================================
-- 13. P3 · 비기능
-- =====================================================================

CREATE TABLE report (
    report_id          BIGINT      NOT NULL AUTO_INCREMENT,
    reporter_member_id BIGINT      NOT NULL,
    target_type        VARCHAR(20) NOT NULL,
    target_id          BIGINT      NOT NULL,
    target_member_id   BIGINT      NOT NULL,
    reason             VARCHAR(20) NOT NULL,
    reason_detail      TEXT        NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    handler_member_id  BIGINT      NULL,
    handled_at         DATETIME    NULL,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_report PRIMARY KEY (report_id),
    CONSTRAINT fk_report_reporter_member_id FOREIGN KEY (reporter_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_report_target_member_id FOREIGN KEY (target_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_report_handler_member_id FOREIGN KEY (handler_member_id) REFERENCES member (member_id) ON DELETE RESTRICT,
    CONSTRAINT uk_report_reporter_target UNIQUE (reporter_member_id, target_type, target_id),
    CONSTRAINT ck_report_target_type CHECK (target_type IN ('POST', 'PORTFOLIO', 'COMMENT', 'REVIEW', 'MESSAGE')),
    CONSTRAINT ck_report_reason CHECK (reason IN ('SPAM', 'ABUSE', 'PLAGIARISM', 'PRIVACY', 'ETC')),
    CONSTRAINT ck_report_status CHECK (status IN ('PENDING', 'HIDDEN', 'REJECTED')),
    CONSTRAINT ck_report_reason_detail CHECK (reason <> 'ETC' OR reason_detail IS NOT NULL),
    INDEX idx_report_target_type_target_id (target_type, target_id),
    INDEX idx_report_status_created_at (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '신고';

CREATE TABLE banned_word (
    banned_word_id BIGINT      NOT NULL AUTO_INCREMENT,
    word           VARCHAR(50) NOT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_banned_word PRIMARY KEY (banned_word_id),
    CONSTRAINT uk_banned_word_word UNIQUE (word)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '금칙어';

-- 개인 작품 템플릿 항목 (P3 FR-PRT-015, #253). 항목은 5개로 고정이고(#168 결정 A) 운영자는 이름·안내 문구·순서만 바꾼다.
-- 값은 항목 키에 맞는 portfolio 칸에 저장한다: TITLE → title, LINK → result_url, DESCRIPTION → summary, AI_TOOLS → ai_tools, RETROSPECTIVE → retrospective
CREATE TABLE portfolio_template_item (
    portfolio_template_item_id BIGINT       NOT NULL AUTO_INCREMENT,
    item_key                   VARCHAR(20)  NOT NULL,
    label                      VARCHAR(50)  NOT NULL,
    guide                      VARCHAR(200) NULL,
    sort_order                 INT          NOT NULL,
    created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_portfolio_template_item PRIMARY KEY (portfolio_template_item_id),
    CONSTRAINT uk_portfolio_template_item_item_key UNIQUE (item_key),
    CONSTRAINT ck_portfolio_template_item_item_key CHECK (item_key IN ('TITLE', 'LINK', 'DESCRIPTION', 'AI_TOOLS', 'RETROSPECTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '개인 작품 템플릿 항목';

SET FOREIGN_KEY_CHECKS = 1;
