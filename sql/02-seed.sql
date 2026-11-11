-- 바이브로그 기초·샘플 데이터
-- 실행 순서: 01-schema.sql 바로 다음 (01 이 테이블을 비우므로 이 파일은 빈 테이블에 넣는 것만 생각한다)
-- 기존 행의 수정·삭제는 팀 상의가 먼저다. 새 행은 맨 아래에 추가만 한다 (08-task-flow 규칙 4)

SET NAMES utf8mb4;

-- =====================================================================
-- 테스트 계정 (로컬 개발용)
-- 비밀번호는 BCrypt(cost 10) 해시만 저장한다 (NFR-SEC-001). 평문은 팀 채팅으로 공유한다
-- is_test = TRUE: 평균 점수 계산에서 뺀다 (PL-EXM-008)
-- email_verified_at: 인증을 마친 상태로 둔다 (PL-USR-003)
-- =====================================================================

INSERT INTO member (member_id, email, password_hash, nickname, role, status, is_test, email_verified_at) VALUES
    (1, 'learner@vibelog.test', '$2a$10$V4C2gpFdfLOGnquFYp/wO.wTDCfAqpm8TOLUD57Bwj8kW4Vg/fRrG', '테스트학습자', 'LEARNER', 'ACTIVE', TRUE, NOW()),
    (2, 'admin@vibelog.test',   '$2a$10$XVdbbf4OLMH2z4XPeqQ5juOf2IEMt15JZJD3lb.UBnSjWjGVUyVt6', '테스트운영자', 'ADMIN',   'ACTIVE', TRUE, NOW());

-- =====================================================================
-- 기초 데이터: 트랙 4개 (#70, 팀 확정 2026-11-09)
-- 대상·설명은 기획서(01-project-overview.md) 2-2 대상·5 기대효과의 학습자 유형 4개를 순서대로 옮겼다
-- sort_order 는 진단 동점일 때 우선순위다 (PL-DGN-002: Starter → Controller → Builder → Explorer)
-- =====================================================================

INSERT INTO track (track_id, name, target, description, sort_order) VALUES
    (1, 'Starter',    '바이브코딩을 거의 해 본 적이 없고, AI에게 무엇을 어떻게 지시해야 할지 모르는 입문 학습자',
        '막연하게 AI에게 코드를 요청하는 것이 아니라 요구사항을 작은 단위로 전달하고 결과를 확인하는 기본 습관을 익힙니다.', 1),
    (2, 'Controller', '결과물은 만들어 봤지만 AI가 수정 범위를 벗어나거나 프로젝트가 쉽게 꼬이는 학습자',
        '수정 계획, 작업 범위, 컨텍스트와 검증 기준을 명확하게 주는 방법을 익혀 AI가 프로젝트를 예상 밖으로 바꾸는 일을 줄입니다.', 2),
    (3, 'Builder',    '원하는 기능은 구현하지만 큰 프로젝트의 작업 분할, 맥락 관리, 테스트, 코드 품질 관리가 필요한 학습자',
        '큰 기능을 작업 단위로 나누고 테스트·보안·리팩터링까지 포함해 AI와 안정적으로 프로젝트를 개발하는 방법을 익힙니다.', 3),
    (4, 'Explorer',   'AI 코딩 경험이 충분하고 최신 모델·에이전트·컨텍스트 관리·개발 워크플로를 빠르게 실험하고 싶은 숙련 학습자',
        '빠르게 변하는 AI 개발 도구와 워크플로를 같은 과제로 비교·검증하고 자신의 개발 방식에 적용할 근거를 얻습니다.', 4);

-- =====================================================================
-- 기초 데이터: 코스 카테고리 5개 (#70, 팀 확정). 트랙(숙련도)과 따로 주제로 나눈다 (D4)
-- =====================================================================

INSERT INTO course_category (course_category_id, name, sort_order) VALUES
    (1, '요구사항 전달',      1),
    (2, '작업 범위·컨텍스트', 2),
    (3, '테스트·품질',        3),
    (4, '보안',               4),
    (5, '도구·워크플로',      5);

-- =====================================================================
-- 기초 데이터: 진단 문항 5개 × 보기 4개 (#70, 팀 확정)
-- 보기 하나가 트랙 하나에 1점. 고른 보기 점수 합계가 가장 높은 트랙을 추천한다 (PL-DGN-002)
-- 보기 sort_order 1~4 = Starter·Controller·Builder·Explorer 순
-- =====================================================================

INSERT INTO diagnosis_question (diagnosis_question_id, content, sort_order) VALUES
    (1, 'AI로 코딩해 본 경험은 어느 정도인가요?', 1),
    (2, 'AI에게 일을 시킬 때 가장 어려운 점은 무엇인가요?', 2),
    (3, 'AI가 만든 코드를 받으면 보통 어떻게 하나요?', 3),
    (4, '요청할 때 어떤 정보를 함께 주나요?', 4),
    (5, '바이브로그에서 가장 얻고 싶은 것은 무엇인가요?', 5);

INSERT INTO diagnosis_option (diagnosis_option_id, diagnosis_question_id, track_id, content, score, sort_order) VALUES
    (1,  1, 1, '거의 없어요', 1, 1),
    (2,  1, 2, '작은 결과물은 만들어 봤어요', 1, 2),
    (3,  1, 3, '기능이 여러 개인 프로젝트를 만들어 봤어요', 1, 3),
    (4,  1, 4, '여러 도구로 꾸준히 개발하고 있어요', 1, 4),
    (5,  2, 1, '무엇을 어떻게 요청해야 할지 모르겠어요', 1, 1),
    (6,  2, 2, 'AI가 시키지 않은 곳까지 고쳐요', 1, 2),
    (7,  2, 3, '기능이 커지면 코드가 엉켜요', 1, 3),
    (8,  2, 4, '새 도구가 너무 빨리 나와 고르기 어려워요', 1, 4),
    (9,  3, 1, '그대로 실행해 봐요', 1, 1),
    (10, 3, 2, '바뀐 부분을 확인하고 되돌리기도 해요', 1, 2),
    (11, 3, 3, '테스트를 돌리고 리뷰해요', 1, 3),
    (12, 3, 4, '다른 모델·도구 결과와 비교해요', 1, 4),
    (13, 4, 1, '만들고 싶은 것만 말해요', 1, 1),
    (14, 4, 2, '고칠 파일과 범위를 정해 줘요', 1, 2),
    (15, 4, 3, '작업을 나누고 완료 기준을 정해 줘요', 1, 3),
    (16, 4, 4, '규칙 파일·에이전트 설정까지 맞춰 둬요', 1, 4),
    (17, 5, 1, 'AI에게 요청하는 기본 습관', 1, 1),
    (18, 5, 2, 'AI가 프로젝트를 망치지 않게 통제하는 법', 1, 2),
    (19, 5, 3, '큰 프로젝트를 안정적으로 만드는 법', 1, 3),
    (20, 5, 4, '최신 도구·워크플로를 검증할 근거', 1, 4);

-- =====================================================================
-- 개발용 샘플: 공개 코스 1개 (#70, 08-task-flow 규칙 2: 학습자 화면은 운영자 화면 대신 이 데이터로 만든다)
-- Controller 트랙 · 작업 범위·컨텍스트 카테고리 · 기획서 4-1 학습 구조 예시 그대로
-- 수업 1: 개념 → 퀴즈 → 개념 → 퀴즈 → 모의고사 / 수업 2: 개념 → 퀴즈 → 모의고사 → 포트폴리오
-- 퀴즈는 3문항·4지선다·정답률 50% 통과 (PL-LSN-002·003), 모의고사 문항은 보기 4개·개념마다 연관 개념 (PL-EXM-002)
-- =====================================================================

INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days, is_public) VALUES
    (1, 2, 2, 'AI 작업 범위 통제하기',
     'AI가 시키지 않은 곳까지 고치지 않게, 수정 범위를 정해 요청하고 바뀐 부분을 확인하는 방법을 배웁니다.',
     'AI에게 기능을 고쳐 달라고 하면 관련 없는 파일까지 바뀌어 프로젝트가 꼬이는 일이 많습니다. 이 코스에서는 고칠 파일과 범위를 정해 요청하는 법, 바뀐 부분을 확인하고 되돌리는 법을 익히고, 마지막에 작은 프로젝트로 직접 적용해 봅니다.',
     '결과물은 만들어 봤지만 AI가 수정 범위를 벗어나 프로젝트가 꼬인 경험이 있는 학습자',
     '수정 범위를 지정해 요청하고, 바뀐 부분을 확인해 필요하면 되돌릴 수 있다.',
     7, TRUE);

INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES
    (1, 1, '수정 범위를 지정해서 요청하기',
     'AI에게 "이거 고쳐 줘"라고만 하면 어디까지 고칠지는 AI가 정합니다. 범위를 먼저 정해 주는 연습을 합니다.',
     '고칠 파일·함수와 건드리면 안 되는 곳을 요청에 함께 적을 수 있다.',
     '1. 범위 없이 요청하면 생기는 일 2. 고칠 곳과 건드리지 않을 곳을 함께 적기',
     30, 1, TRUE),
    (2, 1, '바뀐 부분 확인하고 되돌리기',
     'AI가 고친 결과를 그대로 받아들이지 않고, 무엇이 바뀌었는지 확인하는 습관을 만듭니다.',
     '변경 내용(diff)을 읽고, 의도와 다른 변경을 되돌릴 수 있다.',
     '1. 변경 내용(diff) 읽기',
     20, 2, TRUE);

INSERT INTO activity (activity_id, lesson_id, activity_type, title, body, body_format, estimated_minutes, pass_rate, sort_order) VALUES
    (1, 1, 'CONCEPT', '범위 없이 요청하면 생기는 일',
     '## 범위 없이 요청하면 생기는 일\n\nAI에게 "로그인 버튼이 안 눌려요, 고쳐 줘"라고만 하면, AI는 버튼뿐 아니라 로그인 화면 전체나 관련 없는 파일까지 고칠 수 있습니다.\n\n- 고칠 곳을 AI가 스스로 정한다\n- 그래서 확인해야 할 변경이 많아지고, 다른 기능이 망가지기 쉽다\n\n요청 전에 **어디까지 고칠지** 먼저 정하는 것이 시작입니다.',
     'MARKDOWN', 10, NULL, 1),
    (2, 1, 'QUIZ', '확인 퀴즈: 범위 없는 요청', NULL, NULL, 5, 50, 2),
    (3, 1, 'CONCEPT', '고칠 곳과 건드리지 않을 곳을 함께 적기',
     '## 고칠 곳과 건드리지 않을 곳을 함께 적기\n\n좋은 요청에는 두 가지가 함께 들어갑니다.\n\n1. **고칠 곳:** 파일·함수 이름 (예: `LoginForm.jsx`의 `handleSubmit`)\n2. **건드리지 않을 곳:** 바꾸면 안 되는 부분 (예: "스타일과 다른 컴포넌트는 그대로 둬 줘")\n\n예) "`LoginForm.jsx`의 `handleSubmit`만 고쳐서 버튼이 눌리게 해 줘. 다른 파일과 스타일은 바꾸지 마."',
     'MARKDOWN', 10, NULL, 3),
    (4, 1, 'QUIZ', '확인 퀴즈: 범위를 정한 요청', NULL, NULL, 5, 50, 4),
    (5, 2, 'CONCEPT', '변경 내용(diff) 읽기',
     '## 변경 내용(diff) 읽기\n\nAI가 코드를 고치면 **무엇이 바뀌었는지** 먼저 봅니다. Git의 diff에서 `-` 줄은 지워진 코드, `+` 줄은 새로 들어간 코드입니다.\n\n- 요청하지 않은 파일이 바뀌었는지 본다\n- 의도와 다른 변경은 그 부분만 되돌린다 (`git restore <파일>`)\n\n확인하지 않고 받아들인 변경은 나중에 원인을 찾기 어렵습니다.',
     'MARKDOWN', 10, NULL, 1),
    (6, 2, 'QUIZ', '확인 퀴즈: 변경 내용 확인', NULL, NULL, 5, 50, 2);

INSERT INTO concept_tag (concept_tag_id, name) VALUES
    (1, '수정 범위'),
    (2, '요청 작성'),
    (3, '변경 확인');

INSERT INTO activity_tag (activity_id, concept_tag_id) VALUES
    (1, 1),
    (3, 2),
    (5, 3);

-- 연관 개념 (FR-LSN-003, API-LSN-07, #338). 운영자 입력 화면 없이 샘플 데이터로 채운다 (lesson.md 연관 개념).
-- 개념 활동 1·3(수업 1)과 5(수업 2)를 잇는다. 개념 5는 잠긴 수업에 있어 누르면 잠김 안내가 보인다
INSERT INTO concept_relation (source_activity_id, related_activity_id, sort_order) VALUES
    (1, 3, 1),
    (1, 5, 2),
    (3, 1, 1),
    (5, 1, 1);

-- 퀴즈 문항 3개씩 (PL-LSN-003). 3문항이면 2개 이상 맞혀야 통과
INSERT INTO quiz_question (quiz_question_id, activity_id, concept_tag_id, display_type, content, code_snippet, explanation, sort_order) VALUES
    (1, 2, 1, 'CONCEPT', '"로그인 버튼이 안 눌려요, 고쳐 줘"처럼 범위 없이 요청하면 생기기 쉬운 일은?', NULL,
     '고칠 범위를 AI가 정하므로 관련 없는 파일까지 바뀌기 쉽습니다.', 1),
    (2, 2, 1, 'CONCEPT', '요청하기 전에 가장 먼저 정해야 하는 것은?', NULL,
     '어디까지 고칠지(범위)를 먼저 정해야 변경을 확인하기 쉽습니다.', 2),
    (3, 2, 1, 'TRICK', '범위를 정하지 않아도 AI가 알아서 필요한 곳만 고치므로 괜찮다. 맞을까요?', NULL,
     'AI는 필요하다고 판단한 곳을 넓게 고칠 수 있습니다. 범위는 사람이 정해 줘야 합니다.', 3),
    (4, 4, 2, 'CONCEPT', '좋은 요청에 함께 들어가야 하는 두 가지는?', NULL,
     '고칠 곳과 건드리지 않을 곳을 함께 적어야 AI가 범위를 지킵니다.', 1),
    (5, 4, 2, 'CONCEPT', '다음 중 범위를 가장 잘 정한 요청은?', NULL,
     '파일·함수 이름과 바꾸면 안 되는 곳을 함께 적은 요청이 가장 좋습니다.', 2),
    (6, 4, 2, 'TRICK', '"다른 파일은 바꾸지 마"라고 적으면 요청이 길어지니 빼는 게 낫다. 맞을까요?', NULL,
     '건드리지 않을 곳을 적는 문장이 범위를 지키게 하는 핵심입니다.', 3),
    (7, 6, 3, 'CONCEPT', 'Git diff에서 `+`로 시작하는 줄은 무엇인가요?', NULL,
     '`+` 줄은 새로 들어간 코드, `-` 줄은 지워진 코드입니다.', 1),
    (8, 6, 3, 'CONCEPT', 'AI가 요청하지 않은 파일까지 바꿨을 때 가장 알맞은 행동은?', NULL,
     '의도와 다른 변경은 그 부분만 되돌리고 다시 범위를 정해 요청합니다.', 2),
    (9, 6, 3, 'TRICK', 'AI가 고친 코드는 테스트가 통과하면 diff를 볼 필요가 없다. 맞을까요?', NULL,
     '테스트가 통과해도 요청하지 않은 변경이 섞일 수 있어 diff를 확인해야 합니다.', 3);

INSERT INTO quiz_choice (quiz_question_id, content, is_correct, sort_order) VALUES
    (1, '관련 없는 파일까지 바뀔 수 있다', TRUE, 1),
    (1, 'AI가 요청을 거절한다', FALSE, 2),
    (1, '항상 버튼만 정확히 고친다', FALSE, 3),
    (1, '코드가 자동으로 테스트된다', FALSE, 4),
    (2, '사용할 AI 모델', FALSE, 1),
    (2, '어디까지 고칠지(범위)', TRUE, 2),
    (2, '요청 문장의 길이', FALSE, 3),
    (2, '코드의 줄 수', FALSE, 4),
    (3, '맞다, AI가 알아서 판단한다', FALSE, 1),
    (3, '틀리다, 범위는 사람이 정해 줘야 한다', TRUE, 2),
    (3, '파일이 하나일 때만 맞다', FALSE, 3),
    (3, '테스트가 있으면 맞다', FALSE, 4),
    (4, '고칠 곳과 건드리지 않을 곳', TRUE, 1),
    (4, '모델 이름과 버전', FALSE, 2),
    (4, '완성 날짜와 담당자', FALSE, 3),
    (4, '코드 줄 수와 파일 크기', FALSE, 4),
    (5, '"로그인 좀 고쳐 줘"', FALSE, 1),
    (5, '"전체적으로 깔끔하게 정리해 줘"', FALSE, 2),
    (5, '"LoginForm.jsx의 handleSubmit만 고쳐 줘. 다른 파일은 바꾸지 마"', TRUE, 3),
    (5, '"버튼이 이상해"', FALSE, 4),
    (6, '맞다, 짧을수록 좋다', FALSE, 1),
    (6, '맞다, AI가 알아서 지킨다', FALSE, 2),
    (6, '틀리다, 건드리지 않을 곳을 적어야 범위가 지켜진다', TRUE, 3),
    (6, '파일이 많을 때만 적는다', FALSE, 4),
    (7, '지워진 코드', FALSE, 1),
    (7, '새로 들어간 코드', TRUE, 2),
    (7, '주석 처리된 코드', FALSE, 3),
    (7, '오류가 난 코드', FALSE, 4),
    (8, '그대로 둔다', FALSE, 1),
    (8, '프로젝트를 처음부터 다시 만든다', FALSE, 2),
    (8, '그 부분만 되돌리고 범위를 정해 다시 요청한다', TRUE, 3),
    (8, '다른 AI 도구로 바꾼다', FALSE, 4),
    (9, '맞다, 테스트가 통과하면 충분하다', FALSE, 1),
    (9, '틀리다, 요청하지 않은 변경이 섞일 수 있다', TRUE, 2),
    (9, '테스트가 10개 이상이면 맞다', FALSE, 3),
    (9, '파일이 하나면 맞다', FALSE, 4);

-- 모의고사 문항: 보기 4개(팀 결정), 연관 개념 = 그 수업의 개념 활동 (PL-EXM-002)
INSERT INTO exam_question (exam_question_id, lesson_id, concept_activity_id, content, explanation, sort_order) VALUES
    (1, 1, 1, '범위 없이 "고쳐 줘"라고만 요청했을 때의 문제로 가장 알맞은 것은?',
     '고칠 범위를 AI가 정해 관련 없는 곳까지 바뀌기 쉽고, 확인할 변경이 많아집니다.', 1),
    (2, 1, 3, '"건드리지 않을 곳"을 요청에 적는 이유는?',
     'AI가 넓게 고치지 않도록 바꾸면 안 되는 부분을 분명히 알려 주기 위해서입니다.', 2),
    (3, 1, 3, '다음 요청에서 빠진 것은? "LoginForm.jsx의 버튼이 눌리게 해 줘"',
     '고칠 곳은 있지만 건드리지 않을 곳(다른 파일·스타일)이 빠져 있습니다.', 3),
    (4, 2, 5, 'AI가 고친 뒤 가장 먼저 확인할 것은?',
     '무엇이 바뀌었는지(diff)를 보고 요청하지 않은 변경이 있는지 확인합니다.', 1),
    (5, 2, 5, 'diff에서 요청하지 않은 파일이 바뀐 것을 발견했다. 알맞은 행동은?',
     '그 파일만 되돌리고(git restore) 범위를 정해 다시 요청합니다.', 2);

INSERT INTO exam_choice (exam_question_id, content, is_correct, sort_order) VALUES
    (1, '관련 없는 곳까지 바뀌어 확인할 변경이 많아진다', TRUE, 1),
    (1, 'AI가 답을 하지 않는다', FALSE, 2),
    (1, '코드가 짧아진다', FALSE, 3),
    (1, '테스트가 자동으로 생긴다', FALSE, 4),
    (2, '요청을 길게 보이게 하려고', FALSE, 1),
    (2, 'AI가 넓게 고치지 않게 하려고', TRUE, 2),
    (2, 'AI 사용 요금을 줄이려고', FALSE, 3),
    (2, '정해진 형식이라서', FALSE, 4),
    (3, '고칠 파일 이름', FALSE, 1),
    (3, '고칠 기능 설명', FALSE, 2),
    (3, '건드리지 않을 곳', TRUE, 3),
    (3, '빠진 것이 없다', FALSE, 4),
    (4, '바뀐 부분(diff)', TRUE, 1),
    (4, '파일 개수', FALSE, 2),
    (4, 'AI의 답변 속도', FALSE, 3),
    (4, '코드 줄 수', FALSE, 4),
    (5, '그대로 커밋한다', FALSE, 1),
    (5, '그 파일만 되돌리고 범위를 정해 다시 요청한다', TRUE, 2),
    (5, '프로젝트를 새로 만든다', FALSE, 3),
    (5, '모든 변경을 되돌린다', FALSE, 4);

-- 포트폴리오 과제 1개 + 스텝 3개 (FR-PRT-001·002). 설정 파일은 운영자가 올리므로 비워 둔다 (agent_file_url NULL)
INSERT INTO portfolio_assignment (portfolio_assignment_id, course_id, topic, goal, agent_guide, agent_file_url) VALUES
    (1, 1, '범위를 정해 AI로 할 일 앱의 기능 하나 고치기',
     '내 할 일 앱(또는 작은 프로젝트)에서 기능 하나를 골라, 수정 범위를 정해 AI에게 요청하고 바뀐 부분을 확인해 결과를 정리한다.',
     '세팅 방법: 프로젝트를 Git으로 관리하고, 고치기 전에 커밋해 둡니다.\n평가 기준: 요청에 고칠 곳과 건드리지 않을 곳이 모두 있는가, 요청하지 않은 파일이 바뀌지 않았는가, 의도와 다른 변경을 되돌렸는가.\n실행 방법: 요청 문장과 diff를 나란히 놓고 평가 기준을 하나씩 체크합니다.\n실행이 안 될 때: git status 로 바뀐 파일을 먼저 확인하고, 커밋하지 않은 변경은 git stash 로 잠시 치워 둡니다.',
     NULL);

INSERT INTO guide_step (portfolio_assignment_id, content, sort_order) VALUES
    (1, '고칠 기능 하나를 고르고, 고칠 파일·함수와 건드리지 않을 곳을 적는다', 1),
    (1, '적은 범위를 넣어 AI에게 요청하고 결과를 받는다', 2),
    (1, 'diff를 확인해 요청하지 않은 변경은 되돌리고, 과정을 회고로 정리한다', 3);

-- 동료 리뷰 평가 기준 3개 (FR-REV-003, PL-REV-003, #209). 과제 1의 평가 에이전트 안내(agent_guide) "평가 기준" 세 가지와 같다.
-- 기준을 넣는 운영자 화면이 없어 새 과제의 기준도 DB 팀이 SQL 로 넣는다 (community.md 동료 리뷰 "평가 기준")
INSERT INTO review_criterion (review_criterion_id, portfolio_assignment_id, name, description, sort_order, is_active) VALUES
    (1, 1, '요청 범위', '요청에 고칠 곳(파일·함수)과 건드리지 않을 곳이 모두 적혀 있는가', 1, TRUE),
    (2, 1, '요청하지 않은 변경', '요청하지 않은 파일·코드가 바뀌지 않았는가 (diff로 확인)', 2, TRUE),
    (3, 1, '되돌리기', '의도와 다른 변경을 찾아 되돌렸는가', 3, TRUE);

-- 개인 작품 템플릿 항목 5개 (FR-PRT-011·015, #253). 이름·안내 문구는 U-34 화면(#199)과 같다. 운영자가 A-12에서 바꾼다
INSERT INTO portfolio_template_item (portfolio_template_item_id, item_key, label, guide, sort_order) VALUES
    (1, 'TITLE', '제목', '작품 제목', 1),
    (2, 'LINK', '링크', 'https://github.com/…', 2),
    (3, 'DESCRIPTION', '설명', '이 작품을 한 줄로 소개해 주세요', 3),
    (4, 'AI_TOOLS', '사용한 AI 도구', '예: Claude Code, Cursor', 4),
    (5, 'RETROSPECTIVE', '회고', '이 작품을 만들며 배운 점과 아쉬운 점', 5);

-- =====================================================================
-- 회원 늘리기 + 시연 계정 (#428)
-- 새 계정 43개(3~45)는 모두 같은 비밀번호다. 평문은 팀 채팅으로 공유한다 (해시만 저장, NFR-SEC-001)
-- 시각은 NOW() 기준이라 언제 실행해도 "며칠 전" 기록으로 보인다. 발표 직전에 01 → 02 를 다시 실행하면 처음 상태로 돌아간다
-- 이 블록이 직접 적는 ID: member 3~45, 그 밖의 표 1001~1999
--
-- 시연 계정 (이메일 @vibelog.test / 닉네임 / 상태)
--   33 demo.new         처음왔어요      가입 직후, 진단 전
--   34 demo.unverified  메일확인전      이메일 인증 전: 학습은 되고 글쓰기는 막힘 (PL-USR-003)
--   35 demo.diagnosed   트랙고민중      진단 완료(Controller 추천), 수강 전 → 수강 시작 시연
--   36 demo.started     첫수업시작      수강 시작 직후, 수업 1 첫 개념만 열림
--   37 demo.quizfail    퀴즈재도전      첫 퀴즈 1/3 불합격 → 다시 풀기
--   38 demo.examready   모의고사직전    수업 1 퀴즈 모두 통과, 모의고사 열림·응시 전
--   39 demo.report      리포트대기      수업 1 모의고사 1/3 제출, 리포트 확인 전 (확인하면 수업 2 열림)
--   40 demo.lesson2     두번째수업      수업 1 완료(모의고사 1/3 → 재응시 3/3), 수업 2 첫 개념 열림
--   41 demo.portfolio   포트폴리오직전  모든 수업 완료, 포트폴리오 열림·제출 전 (제출하면 코스 완료)
--   42 demo.completed   코스완주        코스 완료, 공개 포트폴리오 제출
--   43 demo.dropped     잠시쉬는중      수업 1 중간에 수강 포기 → 다시 시작하면 이어서
--   44 demo.suspended   정지회원        30일 이용 정지: 로그인하면 정지 안내
--   45 demo.admin       시연운영자      운영자 (백오피스)
--
-- 주의: 수업 안에서 퀴즈 일부만 통과한 상태를 과거 시각으로 넣으면 모의고사가 열린 것으로 보인다.
-- 샘플 활동의 created_at 이 시드 실행 시각이라 LessonExamOpenPolicy 가 "그때는 그 퀴즈가 마지막이었다"고 보기 때문이다.
-- 그래서 수업 1 은 퀴즈를 하나도 통과하지 않았거나(36·37·43) 모두 통과한(38~42) 상태만 넣었다
-- =====================================================================

-- 회원: 3~32 일반 학습자 30명(is_test = FALSE) / 33~45 시연 계정 13개(is_test = TRUE, 평균 점수에서 뺀다)
INSERT INTO member (member_id, email, password_hash, nickname, role, status, is_test, email_verified_at, created_at) VALUES
    (3, 'user03@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '코딩하는곰', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 129595 MINUTE, NOW() - INTERVAL 129600 MINUTE),
    (4, 'user04@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '바이브러너', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 125312 MINUTE, NOW() - INTERVAL 125317 MINUTE),
    (5, 'user05@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '새벽커밋', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 121029 MINUTE, NOW() - INTERVAL 121034 MINUTE),
    (6, 'user06@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '프롬프트장인', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 116746 MINUTE, NOW() - INTERVAL 116751 MINUTE),
    (7, 'user07@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '버그헌터', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 112463 MINUTE, NOW() - INTERVAL 112468 MINUTE),
    (8, 'user08@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '리팩터링중', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 108180 MINUTE, NOW() - INTERVAL 108185 MINUTE),
    (9, 'user09@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '커피와코드', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 103897 MINUTE, NOW() - INTERVAL 103902 MINUTE),
    (10, 'user10@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', 'diffreader', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 99614 MINUTE, NOW() - INTERVAL 99619 MINUTE),
    (11, 'user11@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '깃초보', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 95331 MINUTE, NOW() - INTERVAL 95336 MINUTE),
    (12, 'user12@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '주말개발자', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 91048 MINUTE, NOW() - INTERVAL 91053 MINUTE),
    (13, 'user13@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '부끄뿌끄끄', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 86765 MINUTE, NOW() - INTERVAL 86770 MINUTE),
    (14, 'user14@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '뿌끄부끄', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 82482 MINUTE, NOW() - INTERVAL 82487 MINUTE),
    (15, 'user15@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '사이드프로젝트', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 78199 MINUTE, NOW() - INTERVAL 78204 MINUTE),
    (16, 'user16@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '무한루프탈출', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 73916 MINUTE, NOW() - INTERVAL 73921 MINUTE),
    (17, 'user17@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '버그안걸림', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 69633 MINUTE, NOW() - INTERVAL 69638 MINUTE),
    (18, 'user18@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '버그가날걸었다', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 65350 MINUTE, NOW() - INTERVAL 65355 MINUTE),
    (19, 'user19@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', 'vibecoder', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 61067 MINUTE, NOW() - INTERVAL 61072 MINUTE),
    (20, 'user20@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '삼점오도류', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 56184 MINUTE, NOW() - INTERVAL 56189 MINUTE),
    (21, 'user21@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '스프링새싹', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 51901 MINUTE, NOW() - INTERVAL 51906 MINUTE),
    (22, 'user22@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '이도류였던것', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 47618 MINUTE, NOW() - INTERVAL 47623 MINUTE),
    (23, 'user23@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '포카칩칩칩', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 43335 MINUTE, NOW() - INTERVAL 43340 MINUTE),
    (24, 'user24@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '퇴근후코딩', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 39052 MINUTE, NOW() - INTERVAL 39057 MINUTE),
    (25, 'user25@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '눅눅한포카칩', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 34769 MINUTE, NOW() - INTERVAL 34774 MINUTE),
    (26, 'user26@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '할일앱장인', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 30486 MINUTE, NOW() - INTERVAL 30491 MINUTE),
    (27, 'user27@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '관계자만출입금지', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 26203 MINUTE, NOW() - INTERVAL 26208 MINUTE),
    (28, 'user28@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '코드리뷰어', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 21920 MINUTE, NOW() - INTERVAL 21925 MINUTE),
    (29, 'user29@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '오늘도배포', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 17637 MINUTE, NOW() - INTERVAL 17642 MINUTE),
    (30, 'user30@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '모각코러', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 13354 MINUTE, NOW() - INTERVAL 13359 MINUTE),
    (31, 'user31@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '관계자외출입환영', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 9071 MINUTE, NOW() - INTERVAL 9076 MINUTE),
    (32, 'user32@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '깃허브잔디', 'LEARNER', 'ACTIVE', FALSE, NOW() - INTERVAL 4788 MINUTE, NOW() - INTERVAL 4793 MINUTE),
    (33, 'demo.new@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '처음왔어요', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 5 MINUTE, NOW() - INTERVAL 10 MINUTE),
    (34, 'demo.unverified@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '메일확인전', 'LEARNER', 'ACTIVE', TRUE, NULL, NOW() - INTERVAL 60 MINUTE),
    (35, 'demo.diagnosed@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '트랙고민중', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 1435 MINUTE, NOW() - INTERVAL 1440 MINUTE),
    (36, 'demo.started@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '첫수업시작', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 2875 MINUTE, NOW() - INTERVAL 2880 MINUTE),
    (37, 'demo.quizfail@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '퀴즈재도전', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 4315 MINUTE, NOW() - INTERVAL 4320 MINUTE),
    (38, 'demo.examready@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '모의고사직전', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 5755 MINUTE, NOW() - INTERVAL 5760 MINUTE),
    (39, 'demo.report@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '리포트대기', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 7195 MINUTE, NOW() - INTERVAL 7200 MINUTE),
    (40, 'demo.lesson2@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '두번째수업', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 11515 MINUTE, NOW() - INTERVAL 11520 MINUTE),
    (41, 'demo.portfolio@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '포트폴리오직전', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 14395 MINUTE, NOW() - INTERVAL 14400 MINUTE),
    (42, 'demo.completed@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '코스완주', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 20155 MINUTE, NOW() - INTERVAL 20160 MINUTE),
    (43, 'demo.dropped@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '잠시쉬는중', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 10075 MINUTE, NOW() - INTERVAL 10080 MINUTE),
    (44, 'demo.suspended@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '정지회원', 'LEARNER', 'ACTIVE', TRUE, NOW() - INTERVAL 28795 MINUTE, NOW() - INTERVAL 28800 MINUTE),
    (45, 'demo.admin@vibelog.test', '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe', '시연운영자', 'ADMIN', 'ACTIVE', TRUE, NOW() - INTERVAL 43195 MINUTE, NOW() - INTERVAL 43200 MINUTE);

-- 진단 결과: 35~43 은 Controller 트랙 추천 (33 처음왔어요·34 메일확인전은 진단 전)
INSERT INTO diagnosis_result (diagnosis_result_id, member_id, recommended_track_id, status, created_at) VALUES
    (1035, 35, 2, 'COMPLETED', NOW() - INTERVAL 1410 MINUTE),
    (1036, 36, 2, 'COMPLETED', NOW() - INTERVAL 2850 MINUTE),
    (1037, 37, 2, 'COMPLETED', NOW() - INTERVAL 4290 MINUTE),
    (1038, 38, 2, 'COMPLETED', NOW() - INTERVAL 5730 MINUTE),
    (1039, 39, 2, 'COMPLETED', NOW() - INTERVAL 7170 MINUTE),
    (1040, 40, 2, 'COMPLETED', NOW() - INTERVAL 11490 MINUTE),
    (1041, 41, 2, 'COMPLETED', NOW() - INTERVAL 14370 MINUTE),
    (1042, 42, 2, 'COMPLETED', NOW() - INTERVAL 20130 MINUTE),
    (1043, 43, 2, 'COMPLETED', NOW() - INTERVAL 10050 MINUTE);

-- 수강: 36~43 은 샘플 코스 1 (수업 1 = 활동 1~4, 수업 2 = 활동 5~6)
INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at, portfolio_unlocked_at, completed_at, dropped_at) VALUES
    (1001, 36, 1, NOW() - INTERVAL 60 MINUTE, NULL, NULL, NULL),
    (1002, 37, 1, NOW() - INTERVAL 2880 MINUTE, NULL, NULL, NULL),
    (1003, 38, 1, NOW() - INTERVAL 4320 MINUTE, NULL, NULL, NULL),
    (1004, 39, 1, NOW() - INTERVAL 5760 MINUTE, NULL, NULL, NULL),
    (1005, 40, 1, NOW() - INTERVAL 10080 MINUTE, NULL, NULL, NULL),
    (1006, 41, 1, NOW() - INTERVAL 12960 MINUTE, NOW() - INTERVAL 8640 MINUTE, NULL, NULL),
    (1007, 42, 1, NOW() - INTERVAL 18720 MINUTE, NOW() - INTERVAL 12960 MINUTE, NOW() - INTERVAL 10080 MINUTE, NULL),
    (1008, 43, 1, NOW() - INTERVAL 8640 MINUTE, NULL, NULL, NOW() - INTERVAL 7200 MINUTE);

-- 수업 진도: 수업 2 는 수업 1 리포트를 확인한 시각에 열린다 (PL-ENR-002)
INSERT INTO lesson_progress (lesson_progress_id, enrollment_id, lesson_id, unlocked_at, completed_at) VALUES
    (1001, 1001, 1, NOW() - INTERVAL 60 MINUTE, NULL),
    (1002, 1001, 2, NULL, NULL),
    (1003, 1002, 1, NOW() - INTERVAL 2880 MINUTE, NULL),
    (1004, 1002, 2, NULL, NULL),
    (1005, 1003, 1, NOW() - INTERVAL 4320 MINUTE, NULL),
    (1006, 1003, 2, NULL, NULL),
    (1007, 1004, 1, NOW() - INTERVAL 5760 MINUTE, NULL),
    (1008, 1004, 2, NULL, NULL),
    (1009, 1005, 1, NOW() - INTERVAL 10080 MINUTE, NOW() - INTERVAL 7200 MINUTE),
    (1010, 1005, 2, NOW() - INTERVAL 7200 MINUTE, NULL),
    (1011, 1006, 1, NOW() - INTERVAL 12960 MINUTE, NOW() - INTERVAL 11520 MINUTE),
    (1012, 1006, 2, NOW() - INTERVAL 11520 MINUTE, NOW() - INTERVAL 8640 MINUTE),
    (1013, 1007, 1, NOW() - INTERVAL 18720 MINUTE, NOW() - INTERVAL 15840 MINUTE),
    (1014, 1007, 2, NOW() - INTERVAL 15840 MINUTE, NOW() - INTERVAL 12960 MINUTE),
    (1015, 1008, 1, NOW() - INTERVAL 8640 MINUTE, NULL),
    (1016, 1008, 2, NULL, NULL);

-- 활동 진도: 열린 활동만 행이 있다
INSERT INTO activity_progress (activity_progress_id, enrollment_id, activity_id, unlocked_at, completed_at) VALUES
    (1001, 1001, 1, NOW() - INTERVAL 60 MINUTE, NULL),
    (1002, 1002, 1, NOW() - INTERVAL 2880 MINUTE, NOW() - INTERVAL 2870 MINUTE),
    (1003, 1002, 2, NOW() - INTERVAL 2870 MINUTE, NULL),
    (1004, 1003, 1, NOW() - INTERVAL 4320 MINUTE, NOW() - INTERVAL 4310 MINUTE),
    (1005, 1003, 2, NOW() - INTERVAL 4310 MINUTE, NOW() - INTERVAL 4295 MINUTE),
    (1006, 1003, 3, NOW() - INTERVAL 4295 MINUTE, NOW() - INTERVAL 4280 MINUTE),
    (1007, 1003, 4, NOW() - INTERVAL 4280 MINUTE, NOW() - INTERVAL 4265 MINUTE),
    (1008, 1004, 1, NOW() - INTERVAL 5760 MINUTE, NOW() - INTERVAL 5750 MINUTE),
    (1009, 1004, 2, NOW() - INTERVAL 5750 MINUTE, NOW() - INTERVAL 5735 MINUTE),
    (1010, 1004, 3, NOW() - INTERVAL 5735 MINUTE, NOW() - INTERVAL 5720 MINUTE),
    (1011, 1004, 4, NOW() - INTERVAL 5720 MINUTE, NOW() - INTERVAL 5705 MINUTE),
    (1012, 1005, 1, NOW() - INTERVAL 10080 MINUTE, NOW() - INTERVAL 10070 MINUTE),
    (1013, 1005, 2, NOW() - INTERVAL 10070 MINUTE, NOW() - INTERVAL 10055 MINUTE),
    (1014, 1005, 3, NOW() - INTERVAL 10055 MINUTE, NOW() - INTERVAL 10040 MINUTE),
    (1015, 1005, 4, NOW() - INTERVAL 10040 MINUTE, NOW() - INTERVAL 10025 MINUTE),
    (1016, 1005, 5, NOW() - INTERVAL 7200 MINUTE, NULL),
    (1017, 1006, 1, NOW() - INTERVAL 12960 MINUTE, NOW() - INTERVAL 12950 MINUTE),
    (1018, 1006, 2, NOW() - INTERVAL 12950 MINUTE, NOW() - INTERVAL 12935 MINUTE),
    (1019, 1006, 3, NOW() - INTERVAL 12935 MINUTE, NOW() - INTERVAL 12920 MINUTE),
    (1020, 1006, 4, NOW() - INTERVAL 12920 MINUTE, NOW() - INTERVAL 12905 MINUTE),
    (1021, 1006, 5, NOW() - INTERVAL 11520 MINUTE, NOW() - INTERVAL 11505 MINUTE),
    (1022, 1006, 6, NOW() - INTERVAL 11505 MINUTE, NOW() - INTERVAL 11490 MINUTE),
    (1023, 1007, 1, NOW() - INTERVAL 18720 MINUTE, NOW() - INTERVAL 18710 MINUTE),
    (1024, 1007, 2, NOW() - INTERVAL 18710 MINUTE, NOW() - INTERVAL 18695 MINUTE),
    (1025, 1007, 3, NOW() - INTERVAL 18695 MINUTE, NOW() - INTERVAL 18680 MINUTE),
    (1026, 1007, 4, NOW() - INTERVAL 18680 MINUTE, NOW() - INTERVAL 18665 MINUTE),
    (1027, 1007, 5, NOW() - INTERVAL 15840 MINUTE, NOW() - INTERVAL 15825 MINUTE),
    (1028, 1007, 6, NOW() - INTERVAL 15825 MINUTE, NOW() - INTERVAL 15810 MINUTE),
    (1029, 1008, 1, NOW() - INTERVAL 8640 MINUTE, NOW() - INTERVAL 8630 MINUTE),
    (1030, 1008, 2, NOW() - INTERVAL 8630 MINUTE, NULL);

-- 퀴즈 풀이: 3문항, 통과 기준 50% (2/3 이상 통과)
INSERT INTO quiz_attempt (quiz_attempt_id, member_id, activity_id, started_at, correct_rate, is_passed, submitted_at) VALUES
    (1001, 37, 2, NOW() - INTERVAL 2865 MINUTE, 33.33, FALSE, NOW() - INTERVAL 2860 MINUTE),
    (1002, 38, 2, NOW() - INTERVAL 4300 MINUTE, 66.67, TRUE, NOW() - INTERVAL 4295 MINUTE),
    (1003, 38, 4, NOW() - INTERVAL 4270 MINUTE, 100.00, TRUE, NOW() - INTERVAL 4265 MINUTE),
    (1004, 39, 2, NOW() - INTERVAL 5740 MINUTE, 66.67, TRUE, NOW() - INTERVAL 5735 MINUTE),
    (1005, 39, 4, NOW() - INTERVAL 5710 MINUTE, 100.00, TRUE, NOW() - INTERVAL 5705 MINUTE),
    (1006, 40, 2, NOW() - INTERVAL 10060 MINUTE, 66.67, TRUE, NOW() - INTERVAL 10055 MINUTE),
    (1007, 40, 4, NOW() - INTERVAL 10030 MINUTE, 100.00, TRUE, NOW() - INTERVAL 10025 MINUTE),
    (1008, 41, 2, NOW() - INTERVAL 12940 MINUTE, 66.67, TRUE, NOW() - INTERVAL 12935 MINUTE),
    (1009, 41, 4, NOW() - INTERVAL 12910 MINUTE, 100.00, TRUE, NOW() - INTERVAL 12905 MINUTE),
    (1010, 41, 6, NOW() - INTERVAL 11495 MINUTE, 66.67, TRUE, NOW() - INTERVAL 11490 MINUTE),
    (1011, 42, 2, NOW() - INTERVAL 18700 MINUTE, 66.67, TRUE, NOW() - INTERVAL 18695 MINUTE),
    (1012, 42, 4, NOW() - INTERVAL 18670 MINUTE, 100.00, TRUE, NOW() - INTERVAL 18665 MINUTE),
    (1013, 42, 6, NOW() - INTERVAL 15815 MINUTE, 100.00, TRUE, NOW() - INTERVAL 15810 MINUTE);

-- 퀴즈 답
INSERT INTO quiz_answer (quiz_answer_id, quiz_attempt_id, quiz_question_id, quiz_choice_id, is_correct, checked_at) VALUES
    (1001, 1001, 1, 1, TRUE, NOW() - INTERVAL 2861 MINUTE),
    (1002, 1001, 2, 5, FALSE, NOW() - INTERVAL 2861 MINUTE),
    (1003, 1001, 3, 9, FALSE, NOW() - INTERVAL 2861 MINUTE),
    (1004, 1002, 1, 1, TRUE, NOW() - INTERVAL 4296 MINUTE),
    (1005, 1002, 2, 6, TRUE, NOW() - INTERVAL 4296 MINUTE),
    (1006, 1002, 3, 9, FALSE, NOW() - INTERVAL 4296 MINUTE),
    (1007, 1003, 4, 13, TRUE, NOW() - INTERVAL 4266 MINUTE),
    (1008, 1003, 5, 19, TRUE, NOW() - INTERVAL 4266 MINUTE),
    (1009, 1003, 6, 23, TRUE, NOW() - INTERVAL 4266 MINUTE),
    (1010, 1004, 1, 1, TRUE, NOW() - INTERVAL 5736 MINUTE),
    (1011, 1004, 2, 6, TRUE, NOW() - INTERVAL 5736 MINUTE),
    (1012, 1004, 3, 9, FALSE, NOW() - INTERVAL 5736 MINUTE),
    (1013, 1005, 4, 13, TRUE, NOW() - INTERVAL 5706 MINUTE),
    (1014, 1005, 5, 19, TRUE, NOW() - INTERVAL 5706 MINUTE),
    (1015, 1005, 6, 23, TRUE, NOW() - INTERVAL 5706 MINUTE),
    (1016, 1006, 1, 1, TRUE, NOW() - INTERVAL 10056 MINUTE),
    (1017, 1006, 2, 6, TRUE, NOW() - INTERVAL 10056 MINUTE),
    (1018, 1006, 3, 9, FALSE, NOW() - INTERVAL 10056 MINUTE),
    (1019, 1007, 4, 13, TRUE, NOW() - INTERVAL 10026 MINUTE),
    (1020, 1007, 5, 19, TRUE, NOW() - INTERVAL 10026 MINUTE),
    (1021, 1007, 6, 23, TRUE, NOW() - INTERVAL 10026 MINUTE),
    (1022, 1008, 1, 1, TRUE, NOW() - INTERVAL 12936 MINUTE),
    (1023, 1008, 2, 6, TRUE, NOW() - INTERVAL 12936 MINUTE),
    (1024, 1008, 3, 9, FALSE, NOW() - INTERVAL 12936 MINUTE),
    (1025, 1009, 4, 13, TRUE, NOW() - INTERVAL 12906 MINUTE),
    (1026, 1009, 5, 19, TRUE, NOW() - INTERVAL 12906 MINUTE),
    (1027, 1009, 6, 23, TRUE, NOW() - INTERVAL 12906 MINUTE),
    (1028, 1010, 7, 26, TRUE, NOW() - INTERVAL 11491 MINUTE),
    (1029, 1010, 8, 31, TRUE, NOW() - INTERVAL 11491 MINUTE),
    (1030, 1010, 9, 33, FALSE, NOW() - INTERVAL 11491 MINUTE),
    (1031, 1011, 1, 1, TRUE, NOW() - INTERVAL 18696 MINUTE),
    (1032, 1011, 2, 6, TRUE, NOW() - INTERVAL 18696 MINUTE),
    (1033, 1011, 3, 9, FALSE, NOW() - INTERVAL 18696 MINUTE),
    (1034, 1012, 4, 13, TRUE, NOW() - INTERVAL 18666 MINUTE),
    (1035, 1012, 5, 19, TRUE, NOW() - INTERVAL 18666 MINUTE),
    (1036, 1012, 6, 23, TRUE, NOW() - INTERVAL 18666 MINUTE),
    (1037, 1013, 7, 26, TRUE, NOW() - INTERVAL 15811 MINUTE),
    (1038, 1013, 8, 31, TRUE, NOW() - INTERVAL 15811 MINUTE),
    (1039, 1013, 9, 34, TRUE, NOW() - INTERVAL 15811 MINUTE);

-- 모의고사: score = 맞힌 문항 수, report_viewed_at = 오답 분석 리포트 확인
INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at, report_viewed_at) VALUES
    (1001, 39, 1, 1, NOW() - INTERVAL 5680 MINUTE, 1, 33.33, NOW() - INTERVAL 5670 MINUTE, NULL),
    (1002, 40, 1, 1, NOW() - INTERVAL 7212 MINUTE, 1, 33.33, NOW() - INTERVAL 7202 MINUTE, NOW() - INTERVAL 7200 MINUTE),
    (1003, 40, 1, 2, NOW() - INTERVAL 7150 MINUTE, 3, 100.00, NOW() - INTERVAL 7140 MINUTE, NOW() - INTERVAL 7138 MINUTE),
    (1004, 41, 1, 1, NOW() - INTERVAL 11532 MINUTE, 2, 66.67, NOW() - INTERVAL 11522 MINUTE, NOW() - INTERVAL 11520 MINUTE),
    (1005, 41, 2, 1, NOW() - INTERVAL 8652 MINUTE, 2, 100.00, NOW() - INTERVAL 8642 MINUTE, NOW() - INTERVAL 8640 MINUTE),
    (1006, 42, 1, 1, NOW() - INTERVAL 15852 MINUTE, 3, 100.00, NOW() - INTERVAL 15842 MINUTE, NOW() - INTERVAL 15840 MINUTE),
    (1007, 42, 2, 1, NOW() - INTERVAL 12972 MINUTE, 1, 50.00, NOW() - INTERVAL 12962 MINUTE, NOW() - INTERVAL 12960 MINUTE);

-- 모의고사 답
INSERT INTO exam_answer (exam_answer_id, exam_attempt_id, exam_question_id, exam_choice_id, is_correct) VALUES
    (1001, 1001, 1, 1, TRUE),
    (1002, 1001, 2, 5, FALSE),
    (1003, 1001, 3, 9, FALSE),
    (1004, 1002, 1, 1, TRUE),
    (1005, 1002, 2, 5, FALSE),
    (1006, 1002, 3, 9, FALSE),
    (1007, 1003, 1, 1, TRUE),
    (1008, 1003, 2, 6, TRUE),
    (1009, 1003, 3, 11, TRUE),
    (1010, 1004, 1, 1, TRUE),
    (1011, 1004, 2, 6, TRUE),
    (1012, 1004, 3, 9, FALSE),
    (1013, 1005, 4, 13, TRUE),
    (1014, 1005, 5, 18, TRUE),
    (1015, 1006, 1, 1, TRUE),
    (1016, 1006, 2, 6, TRUE),
    (1017, 1006, 3, 11, TRUE),
    (1018, 1007, 4, 13, TRUE),
    (1019, 1007, 5, 17, FALSE);

-- 포트폴리오: 42 코스완주의 코스 포트폴리오 (공개, 제출 시각 = 코스 완료 시각)
INSERT INTO portfolio (portfolio_id, member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public, created_at) VALUES
    (1001, 42, 1, 'COURSE', '할 일 앱 완료 버튼 고치기', 'https://github.com/vibelog-demo/todo-app/pull/3',
     '수정 범위를 TodoItem.jsx 한 파일로 정해 AI에게 완료 버튼 버그만 고치게 했다',
     '처음에는 "버튼 고쳐 줘"라고만 해서 스타일 파일까지 바뀌었다. 고칠 파일과 건드리지 않을 곳을 적어 다시 요청했고, diff에서 요청하지 않은 변경 한 줄을 찾아 되돌렸다.',
     'Claude Code', TRUE, NOW() - INTERVAL 10080 MINUTE);

-- 이용 정지: 44 정지회원 30일 정지 (처리 운영자 = 2 테스트운영자). 시드를 실행한 날부터 29일 동안 로그인이 막힌다
INSERT INTO member_suspension (member_suspension_id, member_id, admin_member_id, level, reason, starts_at, ends_at) VALUES
    (1001, 44, 2, 'DAYS_30', '게시판 도배 (시연용 샘플)', NOW() - INTERVAL 1440 MINUTE, NOW() + INTERVAL 29 DAY);
