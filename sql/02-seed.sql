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
-- 개발용 샘플: 테스트 학습자(회원 1)가 코스 1을 완료한 학습 기록 (#429)
-- 직접 적는 ID 는 1~99 만 쓴다 (#428 회원 3~32 · #430 101~199 · #431 201~999 와 나눔)
-- 완료 여부·진행률은 저장하지 않고 학습 기록에서 계산하므로(PL-ENR-006) 앱이 남기는 순서 그대로 넣는다:
--   수강 시작 → 개념 읽기·퀴즈 통과로 활동이 차례로 열림 → 마지막 퀴즈 통과로 모의고사 열림 → 모의고사 제출
--   → 리포트 확인 시각에 수업 완료·다음 수업 열림(PL-ENR-002) → 마지막 수업 리포트 확인으로 포트폴리오 열림
--   → 포트폴리오 제출 시각에 코스 완료 (PL-ENR-003)
-- 시각은 모두 과거(2026-11-02 ~ 11-06)이고 앞 단계보다 뒤다.
-- 퀴즈 1(활동 2)·모의고사 1(수업 1)은 한 문항씩 틀려 틀린 문제 모아보기(U-28)·약한 개념 리포트(U-16)에도 보이게 했다.
-- 이 뒤로 learner@vibelog.test 는 "수강 시작·진행 중"을 볼 수 없다 → #428 의 새 계정으로 확인한다
-- =====================================================================

INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at, portfolio_unlocked_at, completed_at, created_at, updated_at) VALUES
    (1, 1, 1, '2026-11-02 10:00:00', '2026-11-05 11:00:00', '2026-11-06 15:00:00', '2026-11-02 10:00:00', '2026-11-06 15:00:00');

-- 수업 진도: completed_at = 그 수업 모의고사 리포트를 확인한 시각, 다음 수업은 그때 열린다
INSERT INTO lesson_progress (lesson_progress_id, enrollment_id, lesson_id, unlocked_at, completed_at, created_at, updated_at) VALUES
    (1, 1, 1, '2026-11-02 10:00:00', '2026-11-03 10:40:00', '2026-11-02 10:00:00', '2026-11-03 10:40:00'),
    (2, 1, 2, '2026-11-03 10:40:00', '2026-11-05 11:00:00', '2026-11-03 10:40:00', '2026-11-05 11:00:00');

-- 활동 진도: 개념은 읽기 완료, 퀴즈는 통과한 시각에 완료. 다음 활동은 앞 활동이 끝난 시각에 열린다
INSERT INTO activity_progress (activity_progress_id, enrollment_id, activity_id, unlocked_at, completed_at, created_at, updated_at) VALUES
    (1, 1, 1, '2026-11-02 10:00:00', '2026-11-02 10:10:00', '2026-11-02 10:00:00', '2026-11-02 10:10:00'),
    (2, 1, 2, '2026-11-02 10:10:00', '2026-11-02 10:20:00', '2026-11-02 10:10:00', '2026-11-02 10:20:00'),
    (3, 1, 3, '2026-11-02 10:20:00', '2026-11-02 10:35:00', '2026-11-02 10:20:00', '2026-11-02 10:35:00'),
    (4, 1, 4, '2026-11-02 10:35:00', '2026-11-02 10:45:00', '2026-11-02 10:35:00', '2026-11-02 10:45:00'),
    (5, 1, 5, '2026-11-03 10:40:00', '2026-11-04 20:10:00', '2026-11-03 10:40:00', '2026-11-04 20:10:00'),
    (6, 1, 6, '2026-11-04 20:10:00', '2026-11-04 20:20:00', '2026-11-04 20:10:00', '2026-11-04 20:20:00');

-- 퀴즈 응시: 3문항 중 2개 이상 맞히면 통과(통과 기준 50%, PL-LSN-002). correct_rate 는 %(소수 둘째 자리)
INSERT INTO quiz_attempt (quiz_attempt_id, member_id, activity_id, started_at, correct_rate, is_passed, submitted_at, created_at, updated_at) VALUES
    (1, 1, 2, '2026-11-02 10:12:00',  66.67, TRUE, '2026-11-02 10:20:00', '2026-11-02 10:12:00', '2026-11-02 10:20:00'),
    (2, 1, 4, '2026-11-02 10:37:00', 100.00, TRUE, '2026-11-02 10:45:00', '2026-11-02 10:37:00', '2026-11-02 10:45:00'),
    (3, 1, 6, '2026-11-04 20:12:00', 100.00, TRUE, '2026-11-04 20:20:00', '2026-11-04 20:12:00', '2026-11-04 20:20:00');

-- 퀴즈 답: 보기는 위 quiz_choice 의 (문항, sort_order) 로 찾는다. 문항 3만 오답(보기 1)
INSERT INTO quiz_answer (quiz_answer_id, quiz_attempt_id, quiz_question_id, quiz_choice_id, is_correct, checked_at, created_at, updated_at)
SELECT a.id, a.attempt_id, a.question_id, c.quiz_choice_id, c.is_correct, a.checked_at, a.checked_at, a.checked_at
FROM (
    SELECT 1 AS id, 1 AS attempt_id, 1 AS question_id, 1 AS choice_order, CAST('2026-11-02 10:14:00' AS DATETIME) AS checked_at
    UNION ALL SELECT 2, 1, 2, 2, '2026-11-02 10:16:00'
    UNION ALL SELECT 3, 1, 3, 1, '2026-11-02 10:18:00'
    UNION ALL SELECT 4, 2, 4, 1, '2026-11-02 10:39:00'
    UNION ALL SELECT 5, 2, 5, 3, '2026-11-02 10:41:00'
    UNION ALL SELECT 6, 2, 6, 3, '2026-11-02 10:43:00'
    UNION ALL SELECT 7, 3, 7, 2, '2026-11-04 20:14:00'
    UNION ALL SELECT 8, 3, 8, 3, '2026-11-04 20:16:00'
    UNION ALL SELECT 9, 3, 9, 2, '2026-11-04 20:18:00'
) a
JOIN quiz_choice c ON c.quiz_question_id = a.question_id AND c.sort_order = a.choice_order;

-- 모의고사 응시: score = 맞힌 문항 수, correct_rate = 정답률(%). report_viewed_at = 리포트 확인(수업 완료) 시각
INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at, report_viewed_at, created_at, updated_at) VALUES
    (1, 1, 1, 1, '2026-11-02 21:00:00', 2,  66.67, '2026-11-02 21:10:00', '2026-11-03 10:40:00', '2026-11-02 21:00:00', '2026-11-03 10:40:00'),
    (2, 1, 2, 1, '2026-11-05 10:30:00', 2, 100.00, '2026-11-05 10:40:00', '2026-11-05 11:00:00', '2026-11-05 10:30:00', '2026-11-05 11:00:00');

-- 모의고사 답: 보기는 위 exam_choice 의 (문항, sort_order) 로 찾는다. 수업 1의 문항 3만 오답(보기 1)
INSERT INTO exam_answer (exam_answer_id, exam_attempt_id, exam_question_id, exam_choice_id, is_correct, created_at, updated_at)
SELECT a.id, a.attempt_id, a.question_id, c.exam_choice_id, c.is_correct, a.answered_at, a.answered_at
FROM (
    SELECT 1 AS id, 1 AS attempt_id, 1 AS question_id, 1 AS choice_order, CAST('2026-11-02 21:10:00' AS DATETIME) AS answered_at
    UNION ALL SELECT 2, 1, 2, 2, '2026-11-02 21:10:00'
    UNION ALL SELECT 3, 1, 3, 1, '2026-11-02 21:10:00'
    UNION ALL SELECT 4, 2, 4, 1, '2026-11-05 10:40:00'
    UNION ALL SELECT 5, 2, 5, 2, '2026-11-05 10:40:00'
) a
JOIN exam_choice c ON c.exam_question_id = a.question_id AND c.sort_order = a.choice_order;

-- 코스 포트폴리오 제출 (PL-PRT-002 필수 항목 모두, 공개). 제출 시각 = 코스 완료 시각
INSERT INTO portfolio (portfolio_id, member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public, created_at, updated_at) VALUES
    (1, 1, 1, 'COURSE', '할 일 앱 완료 버튼 범위 지정 수정',
     'https://example.com/todo-app',
     '할 일 앱의 완료 버튼만 고치도록 범위를 정해 요청하고, diff로 바뀐 부분을 확인했습니다.',
     '처음에는 "완료 버튼 고쳐 줘"라고만 했더니 목록 스타일까지 바뀌었습니다. TodoItem.jsx의 handleToggle만 고치고 다른 파일은 바꾸지 말라고 범위를 적어 다시 요청했고, diff에서 요청하지 않은 CSS 변경을 git restore로 되돌렸습니다. 요청 문장에 건드리지 않을 곳을 함께 적는 습관이 생겼습니다.',
     'Claude Code', TRUE, '2026-11-06 15:00:00', '2026-11-06 15:00:00');

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

-- =====================================================================
-- 실제 같은 대량 데이터: 일반 학습자 1만 명 규모 (#431)
-- 목적: 목록·페이지 나눔·정렬·평균 점수를 실제 서비스 규모에서 확인한다 (로컬 QA 용)
-- 규모: 일반 학습자 10,000명 = 3~32 (#428) + 새 회원 9,970명 (member_id 100001~109970, 비밀번호는 #428 새 계정과 같다)
--       수강 약 4,000 · 게시글 3,000 · 댓글 약 9,000 · 추천 · 즐겨찾기 · 팔로우 · 공개 코스 포트폴리오 약 700
-- 직접 적는 ID: 이 블록의 모든 표에서 100000 이상. 1만 명 규모라 이슈의 201~999 에 들어가지 않아 넓혔다 (다른 블록 범위와 안 겹친다)
-- 값은 모두 순번으로 계산한다 (RAND 없음) → 누가 실행해도 같은 데이터가 나온다
--   k = 회원 순번 1~10000. k 1~30 → member_id 3~32, k 31~10000 → member_id 100001~109970
-- 시각은 NOW() 기준 최근 3개월. 수강·글은 그 회원 가입 뒤, 댓글·추천은 글과 그 회원 가입 뒤에 온다
-- 실행 순서: #428 블록(회원 3~32) 다음에 있어야 한다
-- =====================================================================

SET SESSION cte_max_recursion_depth = 10000;   -- 아래 WITH RECURSIVE 로 1~10000 번호를 만든다 (기본값 1000)

-- 회원 9,970명: 가입 시각을 약 90일 전 ~ 27시간 전에 고르게, 이메일 인증 완료. 닉네임은 PL-USR-002 (2~12자, 한글·숫자, 중복 없음)
INSERT INTO member (member_id, email, password_hash, nickname, role, status, is_test, email_verified_at, created_at, updated_at)
WITH RECURSIVE n (k) AS (SELECT 31 UNION ALL SELECT k + 1 FROM n WHERE k < 10000)
SELECT k + 99970,
       CONCAT('user', k + 99970, '@vibelog.test'),
       '$2a$10$kOoYKTU6AHDxSffQUjSIROIwyvbaprZfCanxWBYbVU.LkTC2Cuofe',
       CONCAT(ELT(1 + k % 12, '성실한', '졸린', '배고픈', '신난', '꼼꼼한', '느긋한', '용감한', '조용한', '새벽의', '주말의', '열정', '초보'),
              ELT(1 + k DIV 12 % 12, '코더', '개발자', '학습자', '러너', '메이커', '빌더', '고양이', '다람쥐', '펭귄', '감자', '토끼', '부엉이'),
              k),
       'LEARNER', 'ACTIVE', FALSE, t + INTERVAL 5 MINUTE, t, t
FROM (SELECT k, NOW() - INTERVAL (129600 - (k - 31) * 128000 DIV 9969) MINUTE AS t FROM n) x;

-- 수강 (코스 1): 3~32 는 모두, 나머지는 약 40%. 상태(st)를 enrollment_id 끝자리에 적어 두고, 아래 진도·응시 INSERT 가 그대로 읽는다
--   st 1 수업 1 시작 (활동 1만 열림)                       30%
--   st 2 수업 1 중간 포기 (퀴즈 1 앞, 그중 절반은 퀴즈 1 불합격)  15%
--   st 3 수업 2 진행 중 (수업 1 모의고사·리포트 끝)            25%
--   st 4 수업 1 끝내고 포기                                 8%
--   st 5 코스 완료 + 코스 포트폴리오 제출                     22%
--   가입한 지 얼마 안 돼 그 상태까지 갈 시간이 없으면 st 1 로 둔다
-- 시간표 (수강 시작 뒤 분): 활동1 0→10, 퀴즈1(활동2) 10→17, 개념(활동3) 17→30, 퀴즈2(활동4) 30→37,
--   수업 1 모의고사 40→50, 리포트 확인 52 (= 수업 1 완료·수업 2 열림, PL-ENR-002), 재응시 55→67,
--   g1(1~72시간) 쉰 뒤 활동5 →62+g1, 퀴즈3(활동6) →69+g1, 수업 2 모의고사 72+g1→82+g1, 리포트 84+g1 (= 포트폴리오 열림),
--   g2(1~72시간) 뒤 포트폴리오 제출 = 코스 완료. g1 = 60 * (1 + k * 37 % 72), g2 = 60 * (1 + k * 53 % 72)
-- 수업 1 은 퀴즈를 하나도 통과하지 않았거나 모두 통과한 상태만 넣는다 (#428 블록 주의 문단과 같은 이유)
INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at, portfolio_unlocked_at, completed_at, dropped_at)
WITH RECURSIVE n (k) AS (SELECT 1 UNION ALL SELECT k + 1 FROM n WHERE k < 10000)
SELECT 100000 + k * 10 + st, member_id, 1,
       NOW() - INTERVAL s MINUTE,
       IF(st = 5, NOW() - INTERVAL (s - 84 - g1) MINUTE, NULL),
       IF(st = 5, NOW() - INTERVAL (s - 84 - g1 - g2) MINUTE, NULL),
       CASE st WHEN 2 THEN NOW() - INTERVAL (s - 17 - g1) MINUTE
               WHEN 4 THEN NOW() - INTERVAL (s - 52 - g1) MINUTE END
FROM (
    SELECT k, member_id, st, g1, g2,
           need + FLOOR((age - 10 - need) * (CRC32(CONCAT('s', k)) % 1000) / 1000) AS s
    FROM (
        SELECT k, member_id, g1, g2,
               age,
               IF(age - 10 >= want_need, want, 1) AS st,
               IF(age - 10 >= want_need, want_need, 10) AS need
        FROM (
            SELECT y.k, m.member_id, y.g1, y.g2, y.want,
                   TIMESTAMPDIFF(MINUTE, m.created_at, NOW()) AS age,
                   CASE y.want WHEN 1 THEN 10 WHEN 2 THEN 17 + y.g1 WHEN 3 THEN 67 WHEN 4 THEN 52 + y.g1
                               ELSE 84 + y.g1 + y.g2 END AS want_need
            FROM (
                SELECT k,
                       60 * (1 + k * 37 % 72) AS g1,
                       60 * (1 + k * 53 % 72) AS g2,
                       CASE WHEN h < 30 THEN 1 WHEN h < 45 THEN 2 WHEN h < 70 THEN 3 WHEN h < 78 THEN 4 ELSE 5 END AS want
                FROM (SELECT k, CRC32(CONCAT('st', k)) % 100 AS h FROM n
                      WHERE k <= 30 OR CRC32(CONCAT('enr', k)) % 100 < 40) h
            ) y
            JOIN member m ON m.member_id = IF(y.k <= 30, y.k + 2, y.k + 99970)
        ) z
    ) w
) v;

-- 수업 진도: 수업 2 행은 잠겨 있어도 만든다 (#428 시연 계정과 같은 모양)
INSERT INTO lesson_progress (lesson_progress_id, enrollment_id, lesson_id, unlocked_at, completed_at)
SELECT 100000 + e.k * 10 + l.lesson_id, e.enrollment_id, l.lesson_id,
       CASE WHEN l.lesson_id = 1 THEN e.started_at
            WHEN e.st >= 3 THEN e.started_at + INTERVAL 52 MINUTE END,
       CASE WHEN l.lesson_id = 1 AND e.st >= 3 THEN e.started_at + INTERVAL 52 MINUTE
            WHEN l.lesson_id = 2 AND e.st = 5 THEN e.started_at + INTERVAL (84 + e.g1) MINUTE END
FROM (SELECT enrollment_id, started_at, enrollment_id % 10 AS st, (enrollment_id - 100000) DIV 10 AS k,
             60 * (1 + (enrollment_id - 100000) DIV 10 * 37 % 72) AS g1
      FROM enrollment WHERE enrollment_id > 100000) e
CROSS JOIN (SELECT 1 AS lesson_id UNION ALL SELECT 2) l;

-- 활동 진도: 열린 활동만 행이 있다 (활동 1~6 = 코스 1 샘플 활동)
INSERT INTO activity_progress (activity_progress_id, enrollment_id, activity_id, unlocked_at, completed_at)
SELECT 100000 + e.k * 10 + a.activity_id, e.enrollment_id, a.activity_id,
       e.started_at + INTERVAL (a.unlock_off + IF(a.activity_id = 6, e.g1, 0)) MINUTE,
       IF(e.st >= a.done_st, e.started_at + INTERVAL (a.done_off + IF(a.activity_id >= 5, e.g1, 0)) MINUTE, NULL)
FROM (SELECT enrollment_id, started_at, enrollment_id % 10 AS st, (enrollment_id - 100000) DIV 10 AS k,
             60 * (1 + (enrollment_id - 100000) DIV 10 * 37 % 72) AS g1
      FROM enrollment WHERE enrollment_id > 100000) e
JOIN (SELECT 1 AS activity_id, 1 AS open_st, 0 AS unlock_off, 2 AS done_st, 10 AS done_off UNION ALL
      SELECT 2, 2, 10, 3, 17 UNION ALL
      SELECT 3, 3, 17, 3, 30 UNION ALL
      SELECT 4, 3, 30, 3, 37 UNION ALL
      SELECT 5, 3, 52, 5, 62 UNION ALL
      SELECT 6, 5, 62, 5, 69) a ON e.st >= a.open_st;

-- 퀴즈 풀이: 통과한 퀴즈마다 1번 (2/3 또는 3/3). st 2 의 절반은 퀴즈 1 을 1/3 로 떨어진 뒤 포기했다
INSERT INTO quiz_attempt (quiz_attempt_id, member_id, activity_id, started_at, correct_rate, is_passed, submitted_at)
SELECT 100000 + e.k * 10 + q.activity_id, e.member_id, q.activity_id,
       e.started_at + INTERVAL (q.off + q.g * e.g1) MINUTE,
       IF(e.st = 2, 33.33, IF((e.k + q.activity_id) % 3 = 0, 100.00, 66.67)),
       e.st <> 2,
       e.started_at + INTERVAL (q.off + 5 + q.g * e.g1) MINUTE
FROM (SELECT enrollment_id, member_id, started_at, enrollment_id % 10 AS st, (enrollment_id - 100000) DIV 10 AS k,
             60 * (1 + (enrollment_id - 100000) DIV 10 * 37 % 72) AS g1
      FROM enrollment WHERE enrollment_id > 100000) e
JOIN (SELECT 2 AS activity_id, 12 AS off, 0 AS g, 3 AS min_st UNION ALL
      SELECT 4, 32, 0, 3 UNION ALL
      SELECT 6, 64, 1, 5) q
  ON e.st >= q.min_st OR (e.st = 2 AND q.activity_id = 2 AND e.k % 2 = 0);

-- 퀴즈 답: 정답률만큼 앞 문항부터 맞힌다 (66.67% 면 세 번째 문항만 틀림). 보기는 문항의 정답·첫 오답
INSERT INTO quiz_answer (quiz_answer_id, quiz_attempt_id, quiz_question_id, quiz_choice_id, is_correct, checked_at)
SELECT a.quiz_attempt_id * 10 + qq.sort_order, a.quiz_attempt_id, qq.quiz_question_id,
       IF(qq.sort_order <= ROUND(a.correct_rate * 3 / 100), c.right_id, c.wrong_id),
       qq.sort_order <= ROUND(a.correct_rate * 3 / 100),
       a.submitted_at - INTERVAL 1 MINUTE
FROM quiz_attempt a
JOIN quiz_question qq ON qq.activity_id = a.activity_id
JOIN (SELECT quiz_question_id,
             MIN(IF(is_correct, quiz_choice_id, NULL)) AS right_id,
             MIN(IF(is_correct, NULL, quiz_choice_id)) AS wrong_id
      FROM quiz_choice GROUP BY quiz_question_id) c ON c.quiz_question_id = qq.quiz_question_id
WHERE a.quiz_attempt_id > 100000;

-- 모의고사: 1회차 점수가 평균에 들어간다 (PL-EXM-008). 수업 1 은 0~3점, 수업 2 는 0~2점이 고르게
--   응시 번호 끝자리 1 = 수업 1 1회차, 2 = 수업 2 1회차, 3 = 수업 1 재응시 (1회차 0~1점인 사람 절반, 평균에 안 들어가야 한다)
INSERT INTO exam_attempt (exam_attempt_id, member_id, lesson_id, attempt_no, started_at, score, correct_rate, submitted_at, report_viewed_at)
SELECT 100000 + k * 10 + slot, member_id, lesson_id, attempt_no, t, score, ROUND(score * 100 / nq, 2),
       t + INTERVAL 10 MINUTE, t + INTERVAL 12 MINUTE
FROM (
    SELECT e.k, e.member_id, x.slot, x.lesson_id, x.attempt_no, x.nq,
           e.started_at + INTERVAL (x.off + x.g * e.g1) MINUTE AS t,
           CASE x.slot WHEN 1 THEN CRC32(CONCAT('x1', e.k)) % 4
                       WHEN 2 THEN CRC32(CONCAT('x2', e.k)) % 3
                       ELSE 2 + e.k DIV 2 % 2 END AS score
    FROM (SELECT enrollment_id, member_id, started_at, enrollment_id % 10 AS st, (enrollment_id - 100000) DIV 10 AS k,
                 60 * (1 + (enrollment_id - 100000) DIV 10 * 37 % 72) AS g1
          FROM enrollment WHERE enrollment_id > 100000) e
    JOIN (SELECT 1 AS slot, 1 AS lesson_id, 1 AS attempt_no, 3 AS nq, 40 AS off, 0 AS g, 3 AS min_st UNION ALL
          SELECT 2, 2, 1, 2, 72, 1, 5 UNION ALL
          SELECT 3, 1, 2, 3, 55, 0, 3) x ON e.st >= x.min_st
    WHERE x.slot <> 3 OR (e.k % 2 = 0 AND CRC32(CONCAT('x1', e.k)) % 4 <= 1)
) a;

-- 모의고사 답: 점수만큼 맞힌다. 맞힌 문항은 사람마다 돌아가며 달라 개념별 오답률이 한쪽에 몰리지 않는다
INSERT INTO exam_answer (exam_answer_id, exam_attempt_id, exam_question_id, exam_choice_id, is_correct)
SELECT x.exam_attempt_id * 10 + x.sort_order, x.exam_attempt_id, x.exam_question_id,
       IF(x.ok, c.right_id, c.wrong_id), x.ok
FROM (SELECT a.exam_attempt_id, q.exam_question_id, q.sort_order,
             (q.sort_order - 1 + (a.exam_attempt_id - 100000) DIV 10) % cnt.nq < a.score AS ok
      FROM exam_attempt a
      JOIN exam_question q ON q.lesson_id = a.lesson_id
      JOIN (SELECT lesson_id, COUNT(*) AS nq FROM exam_question GROUP BY lesson_id) cnt ON cnt.lesson_id = a.lesson_id
      WHERE a.exam_attempt_id > 100000) x
JOIN (SELECT exam_question_id,
             MIN(IF(is_correct, exam_choice_id, NULL)) AS right_id,
             MIN(IF(is_correct, NULL, exam_choice_id)) AS wrong_id
      FROM exam_choice GROUP BY exam_question_id) c ON c.exam_question_id = x.exam_question_id;

-- 코스 포트폴리오: 코스 완료(st 5)마다 1개, 제출 시각 = 코스 완료 시각. 80% 공개 → U-35 같은 코스 작품이 여러 페이지 (20개씩)
INSERT INTO portfolio (portfolio_id, member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public, created_at, updated_at)
SELECT 100000 + k * 10 + 1, member_id, 1, 'COURSE',
       CONCAT(ELT(1 + a % 15, '할 일 앱', '가계부', '독서 기록장', '습관 트래커', '레시피 메모', '날씨 위젯', '포모도로 타이머', '일기장',
                  '영단어장', '지출 차트', '북마크 정리기', '회의록 정리기', '동아리 출석부', '주간 식단표', '운동 기록'),
              ' ', ELT(1 + b % 6, '완료 버튼 버그 고치기', '정렬 기능 추가하기', '입력 검증 추가하기', '삭제 확인 창 붙이기',
                       '날짜 표시 형식 고치기', '검색 기능 리팩터링')),
       CONCAT('https://github.com/vibelog-demo/',
              ELT(1 + a % 15, 'todo-app', 'budget-book', 'reading-log', 'habit-tracker', 'recipe-note', 'weather-widget', 'pomodoro-timer',
                  'diary', 'word-cards', 'spending-chart', 'bookmark-box', 'meeting-notes', 'club-attendance', 'meal-planner', 'workout-log'),
              '/pull/', 1 + k % 40),
       ELT(1 + b % 6,
           '완료 버튼이 눌리지 않는 버그를 수정 범위를 한 파일로 정해 고쳤다',
           '목록 정렬 기능을 추가하면서 기존 화면 코드는 건드리지 않게 요청했다',
           '빈 값 입력을 막는 검증을 추가하고 diff로 요청하지 않은 변경이 없는지 확인했다',
           '삭제 전에 확인 창을 띄우는 기능을 고칠 함수만 정해 추가했다',
           '날짜가 영어로 나오던 문제를 표시 함수 하나만 고쳐 해결했다',
           '검색 함수를 정리하면서 바뀐 부분을 diff로 확인하고 필요 없는 변경을 되돌렸다'),
       CONCAT(ELT(1 + c % 6,
                  '처음에는 범위 없이 요청해서 스타일 파일까지 바뀌었다.',
                  '첫 요청에서 AI가 관련 없는 컴포넌트 이름을 바꿨다.',
                  '요청을 한 번에 크게 했더니 바뀐 파일이 너무 많아 확인하기 어려웠다.',
                  '테스트는 통과했지만 diff를 보니 요청하지 않은 함수가 바뀌어 있었다.',
                  '고칠 파일은 적었지만 건드리지 않을 곳을 빼먹어 import 순서가 바뀌었다.',
                  '처음부터 고칠 파일과 함수를 적어서 큰 문제 없이 진행했다.'),
              ' ',
              ELT(1 + d % 6,
                  '고칠 파일과 건드리지 않을 곳을 함께 적어 다시 요청하니 원하는 부분만 바뀌었다.',
                  '요청 전에 커밋해 두고 의도와 다른 변경은 git restore로 그 부분만 되돌렸다.',
                  '요청을 작게 나누니 diff가 짧아져 확인이 쉬워졌다. 다음에는 처음부터 나눠서 요청하겠다.',
                  'diff를 읽는 습관이 생겨서 다음 프로젝트에서도 바로 쓸 수 있을 것 같다.',
                  '평가 기준 세 가지를 체크리스트로 만들어 하나씩 확인했다.',
                  '아쉬운 점은 테스트를 늦게 붙인 것이다. 다음에는 테스트부터 요청해 보겠다.')),
       ELT(1 + c % 5, 'Claude Code', 'Cursor', 'Claude Code, Git', 'GitHub Copilot', 'Cursor, Claude Code'),
       CRC32(CONCAT('pub', k)) % 10 < 8,
       completed_at, completed_at
FROM (SELECT member_id, completed_at, (enrollment_id - 100000) DIV 10 AS k,
             CRC32(CONCAT('pfa', enrollment_id)) AS a, CRC32(CONCAT('pfb', enrollment_id)) AS b,
             CRC32(CONCAT('pfc', enrollment_id)) AS c, CRC32(CONCAT('pfd', enrollment_id)) AS d
      FROM enrollment WHERE enrollment_id > 100000 AND enrollment_id % 10 = 5) e;

-- 게시글 3,000개: 자유 45% · 질문 35% · 작품 공유 20% (PL-CMT-008). 목록 20개씩이라 카테고리마다 여러 페이지
--   글 10개 중 1개는 3~32 가 고르게 쓰고(1인 10개), 나머지는 먼저 가입한 회원일수록 많이 쓴다
--   updated_at = created_at (수정 안 한 글). 삭제된 글 2개 (post_id 100100 · 102001) → 목록에서 빠지는지 확인용
INSERT INTO post (post_id, member_id, category, title, body, work_url, tools, created_at, updated_at, deleted_at)
WITH RECURSIVE n (p) AS (SELECT 1 UNION ALL SELECT p + 1 FROM n WHERE p < 3000)
SELECT 100000 + p, member_id, cat,
       CASE cat
         WHEN 'FREE' THEN CONCAT(
           ELT(1 + a % 6, '', '[후기] ', '[잡담] ', '[모각코] ', '[공유] ', '[수다] '),
           ELT(1 + b % 15, '바이브코딩 시작한 지 2주 됐어요', '오늘 모각코 3시간 채웠습니다', '수업 1 끝내고 느낀 점',
               'AI한테 "고쳐 줘"만 하던 시절이 있었죠', 'diff 보는 습관 생기니까 마음이 편하네요', '주말에 같이 공부하실 분 있나요',
               '퇴근하고 30분씩 듣고 있어요', '첫 포트폴리오 제출했어요', '모의고사 첫 점수 공개합니다',
               '커밋 자주 하라는 말이 이제야 이해돼요', '요청 문장 길게 쓰는 게 오히려 빠르네요', 'Claude Code랑 Cursor 둘 다 써 보신 분?',
               '이번 주 목표: 수업 2 끝내기', 'git restore 덕분에 살았습니다', '다들 하루에 얼마나 공부하세요?'))
         WHEN 'QNA' THEN CONCAT(
           ELT(1 + a % 6, '', '[수업 1] ', '[수업 2] ', '[Git] ', '[Claude Code] ', '[초보] '),
           ELT(1 + b % 15, '다른 파일까지 바뀌었는데 어떻게 되돌리나요?', '범위를 정해 줬는데도 AI가 스타일 파일을 고쳐요',
               'git diff에서 + 줄과 - 줄이 헷갈려요', '커밋 전에 꼭 확인해야 할 게 뭘까요?', '요청 문장에 "건드리지 마"를 꼭 넣어야 하나요?',
               '모의고사 다시 보면 평균 점수도 바뀌나요?', '퀴즈 통과 기준이 몇 문제인가요?', 'git stash는 언제 쓰는 건가요?',
               'AI가 테스트 코드까지 지워 버렸어요', '파일 이름을 정확히 몰라도 범위를 정할 수 있나요?', '포트폴리오 결과 링크는 어디 걸 넣어야 하나요?',
               '수업 2가 안 열려요', 'diff가 너무 길 때는 어떻게 읽으세요?', '되돌린 다음 다시 요청할 때 팁 있나요?',
               '함수 하나만 고쳐 달라고 해도 import가 바뀌어요'))
         ELSE CONCAT(
           ELT(1 + a % 15, '할 일 앱', '가계부', '독서 기록장', '습관 트래커', '레시피 메모', '날씨 위젯', '포모도로 타이머', '일기장',
               '영단어장', '지출 차트', '북마크 정리기', '회의록 정리기', '동아리 출석부', '주간 식단표', '운동 기록'),
           ' ',
           ELT(1 + b % 8, '만들었어요', '완성했습니다', '1차 버전 공유해요', '리팩터링 전후 비교', '범위 정해서 기능 추가해 봤어요',
               '배포까지 해 봤어요', '피드백 부탁드려요', 'Claude Code로 이틀 만에 만들었어요'))
       END,
       CASE cat
         WHEN 'FREE' THEN CONCAT(
           ELT(1 + c % 8,
               '바이브코딩 공부를 시작한 지 2주 정도 됐습니다. 처음에는 AI가 다 해 줄 줄 알았는데, 요청을 어떻게 쓰느냐에 따라 결과가 너무 달라서 놀랐어요.',
               '오늘은 퇴근하고 수업 하나를 끝냈어요. 개념 설명이 짧아서 부담 없이 볼 수 있는 게 좋네요.',
               '예전에는 AI가 고친 코드를 그대로 커밋했는데, 수업에서 diff 보는 법을 배우고 나서 습관이 바뀌었습니다.',
               '주말에 할 일 앱을 다시 만들어 보면서 범위를 정해 요청하는 연습을 했어요.',
               '모의고사를 처음 봤는데 생각보다 헷갈리는 문제가 많았어요. 리포트에서 틀린 개념을 다시 보니 이해가 됐습니다.',
               '같이 공부하는 사람이 있으면 덜 지칠 것 같아서 글 남겨 봐요.',
               'AI한테 "알아서 고쳐 줘"라고만 하던 시절의 프로젝트를 다시 열어 봤는데, 어디가 바뀌었는지 저도 모르겠더라고요.',
               '요청 문장을 길게 쓰는 게 귀찮았는데, 결과적으로는 다시 고치는 시간이 줄어서 더 빨랐어요.'),
           '\n\n',
           ELT(1 + d % 6,
               '다들 하루에 얼마나 공부하시는지 궁금해요.',
               '이번 주에는 수업 2까지 끝내는 게 목표입니다. 다 하면 후기 다시 올릴게요.',
               '비슷한 경험 있으신 분들 이야기 들려주세요.',
               '혹시 같이 모각코 하실 분 있으면 댓글 주세요.',
               '작은 습관 하나가 꽤 크게 느껴지는 하루였습니다.',
               '오늘도 커밋 하나 남기고 잡니다. 다들 화이팅!'))
         WHEN 'QNA' THEN CONCAT(
           ELT(1 + c % 8,
               'LoginForm.jsx의 handleSubmit만 고쳐 달라고 요청했는데, AI가 CSS 파일과 다른 컴포넌트까지 같이 바꿨어요.',
               '수업에서 배운 대로 고칠 파일과 건드리지 않을 곳을 적어서 요청했는데도 import 순서가 계속 바뀝니다.',
               'git diff를 열어 보면 바뀐 줄이 너무 많아서 어디부터 봐야 할지 모르겠어요.',
               'AI가 고친 뒤 테스트는 통과했는데, 요청하지 않은 함수 이름이 바뀌어 있었습니다.',
               '커밋하기 전에 git status를 보니 제가 모르는 파일이 세 개나 바뀌어 있었어요.',
               '모의고사 리포트에서 "변경 확인" 개념이 약하다고 나왔는데, 어떤 식으로 복습하면 좋을지 모르겠어요.',
               '되돌리려고 git restore를 썼는데, 제가 직접 고친 부분까지 같이 사라졌어요.',
               '작은 프로젝트라 파일이 몇 개 없는데도 범위를 매번 적어야 하는지 궁금합니다.'),
           '\n\n',
           ELT(1 + d % 6,
               '이럴 때는 요청을 어떻게 고쳐야 할까요?',
               '비슷한 경험 있으신 분 계시면 어떻게 해결하셨는지 알려 주세요.',
               '제가 놓친 부분이 있는지 봐 주시면 감사하겠습니다.',
               '초보라 질문이 이상할 수도 있는데 너그럽게 봐 주세요.',
               '수업에서 다룬 부분이면 몇 번째 활동인지 알려 주셔도 좋아요.',
               '되돌리는 방법이랑 다시 요청하는 방법 둘 다 궁금합니다.'))
         ELSE CONCAT(
           ELT(1 + c % 6,
               '코스에서 배운 수정 범위 지정을 연습하려고 작은 앱을 만들어 봤어요.',
               '주말 이틀 동안 Claude Code로 만든 사이드 프로젝트입니다.',
               '처음 만든 버전을 AI와 함께 리팩터링해 봤습니다.',
               '평소에 필요했던 도구를 직접 만들어 봤어요.',
               '포트폴리오 과제를 하면서 만든 결과물을 조금 더 다듬었습니다.',
               '기능은 단순하지만 처음으로 끝까지 완성한 프로젝트예요.'),
           '\n\n',
           ELT(1 + d % 6,
               '기능을 하나씩 나눠서 요청했고, 매번 고칠 파일과 건드리지 않을 곳을 같이 적었어요.',
               '요청할 때마다 커밋을 먼저 해 두고, diff를 보면서 요청하지 않은 변경은 되돌렸습니다.',
               '처음에는 한 번에 다 만들어 달라고 했다가 코드가 엉켜서, 화면 하나씩 다시 만들었어요.',
               '스타일은 건드리지 말라고 적어 두니 디자인이 깨지는 일이 확실히 줄었습니다.',
               '버그가 생기면 원인 파일을 먼저 찾고, 그 파일만 고쳐 달라고 요청했어요.',
               '테스트를 먼저 만들어 달라고 한 다음 기능을 붙이니까 안심이 됐어요.'),
           '\n\n',
           ELT(1 + a % 4,
               '써 보시고 불편한 점 있으면 편하게 알려 주세요.',
               '코드 구조나 요청 방식에 대한 피드백도 환영합니다.',
               '다음에는 로그인 기능을 붙여 볼 생각이에요.',
               '추천 눌러 주시면 힘이 됩니다!'))
       END,
       IF(cat = 'SHOWCASE',
          CONCAT('https://github.com/vibelog-demo/',
                 ELT(1 + a % 15, 'todo-app', 'budget-book', 'reading-log', 'habit-tracker', 'recipe-note', 'weather-widget', 'pomodoro-timer',
                     'diary', 'word-cards', 'spending-chart', 'bookmark-box', 'meeting-notes', 'club-attendance', 'meal-planner', 'workout-log'),
                 '-', p),
          NULL),
       IF(cat = 'SHOWCASE',
          ELT(1 + b % 6, 'Claude Code', 'Cursor', 'Claude Code, Git', 'GitHub Copilot', 'Cursor, Claude', 'Claude Code, Vercel'),
          NULL),
       t, t,
       IF(p IN (100, 2001), LEAST(t + INTERVAL 1 DAY, NOW()), NULL)
FROM (
    SELECT x.p, x.cat, x.a, x.b, x.c, x.d, m.member_id,
           m.created_at + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, m.created_at, NOW()) * (CRC32(CONCAT('pt', x.p)) % 1000) / 1000) MINUTE AS t
    FROM (SELECT p,
                 CASE WHEN h < 45 THEN 'FREE' WHEN h < 80 THEN 'QNA' ELSE 'SHOWCASE' END AS cat,
                 IF(p % 10 = 0, 1 + p DIV 10 % 30, 1 + FLOOR(POW(CRC32(CONCAT('pa', p)) % 10000 / 10000, 2) * 10000)) AS ak,
                 CRC32(CONCAT('ta', p)) AS a, CRC32(CONCAT('tb', p)) AS b,
                 CRC32(CONCAT('tc', p)) AS c, CRC32(CONCAT('td', p)) AS d
          FROM (SELECT p, CRC32(CONCAT('pc', p)) % 100 AS h FROM n) h) x
    JOIN member m ON m.member_id = IF(x.ak <= 30, x.ak + 2, x.ak + 99970)
) y;

-- 댓글: 글마다 0~6개 (평균 3). 1~3번째는 댓글, 4~6번째는 1~3번째 댓글에 단 답글 (PL-CMT-006 한 단계)
--   글 종류에 맞는 문장. 댓글은 글이 올라온 직후에 많이 달린다. 일부는 수정됨(updated_at 이 더 늦음)·삭제된 댓글
INSERT INTO comment (comment_id, post_id, member_id, parent_comment_id, content, created_at, updated_at, deleted_at)
SELECT 100000 + (post_id - 100000) * 10 + j, post_id, member_id, NULL, content, t,
       IF((post_id + j) % 97 = 0, LEAST(t + INTERVAL 10 MINUTE, NOW()), t),
       IF((post_id + j) % 499 = 0, LEAST(t + INTERVAL 1 HOUR, NOW()), NULL)
FROM (
    SELECT p.post_id, j.j, m.member_id,
           CASE p.category
             WHEN 'QNA' THEN ELT(1 + CRC32(CONCAT('cc', p.post_id, '-', j.j)) % 10,
               '요청 마지막 줄에 "다른 파일은 바꾸지 마"를 따로 적어 보세요. 저는 그렇게 하고 나서 많이 줄었어요.',
               '먼저 git status로 바뀐 파일 목록을 보고, 요청하지 않은 파일만 git restore로 되돌리면 됩니다.',
               'diff가 길면 파일 단위로 나눠서 보세요. git diff 뒤에 파일 이름을 붙여 하나씩 보면 훨씬 읽기 쉬워요.',
               '요청 전에 커밋해 두면 되돌릴 때 직접 고친 부분이 같이 사라지는 일을 막을 수 있어요.',
               '수업 2 첫 번째 개념에 diff 읽는 방법이 나와요. 다시 보시면 도움 될 거예요.',
               '고칠 함수 이름까지 적어 주면 import 같은 주변 코드는 덜 건드리더라고요.',
               '파일이 적어도 범위는 적는 게 좋아요. 습관이 되면 큰 프로젝트에서 차이가 납니다.',
               '테스트가 통과해도 diff는 꼭 보세요. 저도 이름 바뀐 걸 나중에 발견한 적 있어요.',
               '저도 같은 문제 있었는데, 요청을 두 번으로 나눠서 하니까 해결됐어요.',
               '틀린 문제 해설을 먼저 읽고 연관 개념을 다시 보는 걸 추천해요.')
             WHEN 'FREE' THEN ELT(1 + CRC32(CONCAT('cc', p.post_id, '-', j.j)) % 10,
               '저도 딱 그 단계에서 많이 배웠어요. 화이팅입니다!',
               'diff 보는 습관 생기면 진짜 편해지더라고요.',
               '저도 퇴근하고 조금씩 하고 있어요. 같이 힘내요.',
               '후기 잘 읽었습니다. 저도 이번 주에 수업 2 들어가요.',
               '모각코 관심 있어요! 시간 맞으면 같이 해요.',
               '공감합니다. 요청 문장 길게 쓰는 게 결국 빠르더라고요.',
               '꾸준히 하시는 거 멋져요.',
               '저는 하루 30분 정도 하고 있어요.',
               '처음엔 다들 비슷한 것 같아요. 저도 그랬어요.',
               '좋은 글 감사합니다. 저도 커밋 자주 해 봐야겠어요.')
             ELSE ELT(1 + CRC32(CONCAT('cc', p.post_id, '-', j.j)) % 10,
               '깔끔하네요! 범위 정해서 요청하신 과정이 잘 보여요.',
               '저도 비슷한 앱 만들고 있는데 참고가 많이 됐어요.',
               '화면이 단순해서 쓰기 편해 보여요. 추천 누르고 갑니다.',
               '커밋을 먼저 해 두는 습관 좋네요. 배워 갑니다.',
               '다음 버전도 기대할게요!',
               '모바일에서도 잘 보이는지 궁금해요.',
               '테스트 먼저 만든 방식 좋네요. 저도 해 봐야겠어요.',
               '코드 구조가 깔끔해서 읽기 좋았어요.',
               '이틀 만에 이 정도라니 대단해요.',
               '작은 기능부터 하나씩 나눈 게 보여서 좋아요.')
           END AS content,
           GREATEST(p.created_at, m.created_at)
             + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, GREATEST(p.created_at, m.created_at), NOW())
                              * POW(CRC32(CONCAT('ct', p.post_id, '-', j.j)) % 1000 / 1000, 3)) MINUTE AS t
    FROM post p
    JOIN (SELECT 1 AS j UNION ALL SELECT 2 UNION ALL SELECT 3) j ON j.j <= CRC32(CONCAT('nc', p.post_id)) % 7
    CROSS JOIN LATERAL (SELECT 1 + FLOOR(POW(CRC32(CONCAT('ca', p.post_id, '-', j.j)) % 10000 / 10000, 2) * 10000) AS ak) r
    JOIN member m ON m.member_id = IF(r.ak <= 30, r.ak + 2, r.ak + 99970)
    WHERE p.post_id > 100000
) x;

-- 답글: 댓글 j(1~3)에 단 답글 = comment_id + 3. 절반은 글쓴이가 단다
INSERT INTO comment (comment_id, post_id, member_id, parent_comment_id, content, created_at, updated_at, deleted_at)
SELECT comment_id + 3, post_id, member_id, comment_id, content, t, t, NULL
FROM (
    SELECT c.comment_id, c.post_id, m.member_id,
           ELT(1 + CRC32(CONCAT('rt', c.comment_id)) % 8,
               '감사합니다! 말씀하신 방법으로 해 볼게요.',
               '오 그 방법은 생각 못 했네요. 고마워요.',
               '저도 같은 생각이에요.',
               '해 보니까 진짜 되네요. 감사합니다!',
               '좋은 팁이에요. 저장해 둘게요.',
               '저도 궁금했던 부분인데 덕분에 알았어요.',
               '맞아요, 저도 그렇게 하고 있어요.',
               '다음에 또 질문 올릴게요. 감사합니다!') AS content,
           GREATEST(c.created_at, m.created_at)
             + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, GREATEST(c.created_at, m.created_at), NOW())
                              * POW(CRC32(CONCAT('rtt', c.comment_id)) % 1000 / 1000, 3)) MINUTE AS t
    FROM comment c
    JOIN post p ON p.post_id = c.post_id
    CROSS JOIN LATERAL (SELECT 1 + FLOOR(POW(CRC32(CONCAT('ra', c.comment_id)) % 10000 / 10000, 2) * 10000) AS ak) r
    JOIN member m ON m.member_id = IF(CRC32(CONCAT('rw', c.comment_id)) % 2 = 0, p.member_id, IF(r.ak <= 30, r.ak + 2, r.ak + 99970))
    WHERE c.comment_id > 100000 AND c.parent_comment_id IS NULL
      AND (c.comment_id - 100000) % 10 + 3 <= CRC32(CONCAT('nc', c.post_id)) % 7
) x;

-- 추천: 작품 공유 글에만, 자기 글 제외 (PL-CMT-001·002). 글마다 0~40개, 대부분 적고 몇 개만 많다
--   추천한 사람 = 글쓴이 순번에서 7919 씩 건너뛴 회원 (같은 글에 중복 없음, 글쓴이 자신 없음)
--   주의: 대량 추천은 모두 이번 주 월요일 0시 전에 둔다. 주간 랭킹 테스트(RecommendTest·HiddenContentTest·ReportHiddenContentTest)는
--   테스트가 만든 추천 1~2개짜리 글이 상위 10개 안에 드는 DB 를 가정하고, CI 도 이 파일을 실행하기 때문이다
INSERT INTO recommendation (recommendation_id, member_id, post_id, created_at, updated_at)
WITH RECURSIVE n (j) AS (SELECT 1 UNION ALL SELECT j + 1 FROM n WHERE j < 40)
SELECT recommendation_id, member_id, post_id, t, t
FROM (
    SELECT 100000 + (p.post_id - 100000) * 100 + n.j AS recommendation_id, m.member_id, p.post_id,
           b.base + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, b.base, b.monday)
                                   * POW(CRC32(CONCAT('rct', p.post_id, '-', n.j)) % 1000 / 1000, 2)) MINUTE AS t
    FROM post p
    JOIN n ON n.j <= FLOOR(POW(CRC32(CONCAT('rc', p.post_id)) % 1000 / 1000, 3) * 41)
    CROSS JOIN LATERAL (SELECT 1 + (IF(p.member_id <= 32, p.member_id - 2, p.member_id - 99970) - 1 + n.j * 7919) % 10000 AS rk) r
    JOIN member m ON m.member_id = IF(r.rk <= 30, r.rk + 2, r.rk + 99970)
    CROSS JOIN LATERAL (SELECT GREATEST(p.created_at, m.created_at) AS base,
                               CAST(CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY AS DATETIME) AS monday) b
    WHERE p.post_id > 100000 AND p.category = 'SHOWCASE' AND b.base < b.monday
) x;

-- 이번 주 추천: 하루 이상 지난 작품 공유 글 중 최근 5개에만 12·9·7·5·3개 → 주간 랭킹(PL-CMT-003·004) 1~5위가 보인다
--   5개로 줄인 이유는 위 주의와 같다 (테스트 글이 6위 이하로 들어갈 자리를 남긴다). 추천한 사람은 위와 겹치지 않게 j 41 부터
INSERT INTO recommendation (recommendation_id, member_id, post_id, created_at, updated_at)
WITH RECURSIVE n (j) AS (SELECT 41 UNION ALL SELECT j + 1 FROM n WHERE j < 52)
SELECT 100000 + (h.post_id - 100000) * 100 + n.j, m.member_id, h.post_id, b.t, b.t
FROM (SELECT post_id, member_id, created_at, ROW_NUMBER() OVER (ORDER BY created_at DESC, post_id DESC) AS rn
      FROM post
      WHERE post_id > 100000 AND category = 'SHOWCASE' AND deleted_at IS NULL AND created_at < NOW() - INTERVAL 1 DAY) h
JOIN n ON h.rn <= 5 AND n.j <= 40 + ELT(h.rn, 12, 9, 7, 5, 3)
CROSS JOIN LATERAL (SELECT 1 + (IF(h.member_id <= 32, h.member_id - 2, h.member_id - 99970) - 1 + n.j * 7919) % 10000 AS rk) r
JOIN member m ON m.member_id = IF(r.rk <= 30, r.rk + 2, r.rk + 99970)
CROSS JOIN LATERAL (
    SELECT GREATEST(h.created_at, m.created_at, CAST(CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY AS DATETIME)) AS base
) a
CROSS JOIN LATERAL (
    SELECT a.base + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, a.base, NOW()) * (CRC32(CONCAT('rht', h.post_id, '-', n.j)) % 1000) / 1000) MINUTE AS t
) b;

-- 즐겨찾기: 모든 글에 0~4개, 자기 글 제외 (6007 씩 건너뛰어 중복 없음)
INSERT INTO favorite (favorite_id, member_id, post_id, created_at, updated_at)
SELECT favorite_id, member_id, post_id, t, t
FROM (
    SELECT 100000 + (p.post_id - 100000) * 10 + j.j AS favorite_id, m.member_id, p.post_id,
           GREATEST(p.created_at, m.created_at)
             + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, GREATEST(p.created_at, m.created_at), NOW())
                              * (CRC32(CONCAT('fvt', p.post_id, '-', j.j)) % 1000) / 1000) MINUTE AS t
    FROM post p
    JOIN (SELECT 1 AS j UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) j ON j.j <= CRC32(CONCAT('fv', p.post_id)) % 5
    CROSS JOIN LATERAL (SELECT 1 + (IF(p.member_id <= 32, p.member_id - 2, p.member_id - 99970) - 1 + j.j * 6007) % 10000 AS fk) r
    JOIN member m ON m.member_id = IF(r.fk <= 30, r.fk + 2, r.fk + 99970)
    WHERE p.post_id > 100000
) x;

-- 팔로우: 회원마다 0~4명, 먼저 가입한 회원(3~32 포함)일수록 팔로워가 많다. 같은 사람 두 번·자기 자신 제외
INSERT INTO follow (follow_id, follower_member_id, following_member_id, created_at, updated_at)
WITH RECURSIVE n (k) AS (SELECT 1 UNION ALL SELECT k + 1 FROM n WHERE k < 10000)
SELECT 100000 + x.k * 10 + x.j, a.member_id, b.member_id, t, t
FROM (
    SELECT k, tk, MIN(j) AS j
    FROM (SELECT n.k, j.j, 1 + FLOOR(POW(CRC32(CONCAT('fw', n.k, '-', j.j)) % 10000 / 10000, 3) * 10000) AS tk
          FROM n JOIN (SELECT 1 AS j UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) j
            ON j.j <= CRC32(CONCAT('nf', n.k)) % 5) f
    WHERE tk <> k
    GROUP BY k, tk
) x
JOIN member a ON a.member_id = IF(x.k <= 30, x.k + 2, x.k + 99970)
JOIN member b ON b.member_id = IF(x.tk <= 30, x.tk + 2, x.tk + 99970)
CROSS JOIN LATERAL (
    SELECT GREATEST(a.created_at, b.created_at)
             + INTERVAL FLOOR(TIMESTAMPDIFF(MINUTE, GREATEST(a.created_at, b.created_at), NOW())
                              * (CRC32(CONCAT('fwt', x.k, '-', x.j)) % 1000) / 1000) MINUTE AS t
) tt;

-- =====================================================================
-- 대량 회원의 진단 결과 (#438)
-- 목적: 대시보드 "진단 완료·건너뜀", "추천 트랙과 실제 수강"을 1만 명 규모로 보고, 대량 회원이 로그인할 때 진단 화면을 거치지 않게 한다
-- 대상: #431 블록의 회원 순번 k (k 1~30 → member_id 3~32, k 31~10000 → 100001~109970). 시연 계정 33~45 는 #428 그대로 둔다
-- 직접 적는 ID: diagnosis_result_id = 100000 + k
-- 상태 (h = 순번으로 계산한 0~99):
--   수강하지 않은 회원 h 0~7  → 진단 전 (행 없음, 로그인하면 진단 화면으로 간다)
--   수강한 회원 h 0~7 · 모든 회원 h 8~17 → 건너뜀 SKIPPED (추천 트랙 NULL, ck_diagnosis_result_track)
--   나머지 → 완료 COMPLETED. 추천 트랙은 수강한 회원이면 Controller(코스 1의 트랙) 70% · Starter 10% · Builder 15% · Explorer 5%,
--            수강하지 않은 회원이면 Starter 35% · Controller 30% · Builder 25% · Explorer 10% (PL-DGN-002 결과만 저장, PL-DGN-001)
-- 시각: 가입 2~6분 뒤. #431 수강 시작은 가입 10분 뒤 이후라 항상 진단이 먼저다
-- =====================================================================

INSERT INTO diagnosis_result (diagnosis_result_id, member_id, recommended_track_id, status, created_at, updated_at)
WITH RECURSIVE n (k) AS (SELECT 1 UNION ALL SELECT k + 1 FROM n WHERE k < 10000)
SELECT 100000 + x.k, m.member_id,
       CASE WHEN x.h < 18 THEN NULL
            WHEN e.enrollment_id IS NOT NULL THEN CASE WHEN x.t < 70 THEN 2 WHEN x.t < 80 THEN 1 WHEN x.t < 95 THEN 3 ELSE 4 END
            ELSE CASE WHEN x.t < 35 THEN 1 WHEN x.t < 65 THEN 2 WHEN x.t < 90 THEN 3 ELSE 4 END
       END,
       IF(x.h < 18, 'SKIPPED', 'COMPLETED'),
       m.created_at + INTERVAL (2 + x.k % 5) MINUTE,
       m.created_at + INTERVAL (2 + x.k % 5) MINUTE
FROM (SELECT k, CRC32(CONCAT('dg', k)) % 100 AS h, CRC32(CONCAT('dt', k)) % 100 AS t FROM n) x
JOIN member m ON m.member_id = IF(x.k <= 30, x.k + 2, x.k + 99970)
LEFT JOIN enrollment e ON e.member_id = m.member_id AND e.course_id = 1
WHERE e.enrollment_id IS NOT NULL OR x.h >= 8;

-- =====================================================================
-- 공지사항 + 상단 배너 (#440, FR-NTF-003·004, PL-NTF-003)
-- 직접 적는 ID: notice 2001~2099 (#429 1~99 · #430 101~199 · #428 1001~1999 · #431 100000 이상과 겹치지 않게)
-- 작성자는 운영자(회원 2). 목록이 10개씩이라(NoticeService.PAGE_SIZE) 12개를 넣어 2페이지까지 보이게 한다
-- 시각은 NOW() 기준이라 언제 01 → 02 를 실행해도 "며칠 전 공지"로 보이고, 배너는 실행한 날부터 2주 동안 뜬다
-- 배너는 동시에 1개만 뜬다(PL-NTF-003): 지금 뜨는 배너는 2012 하나, 2001 은 기간이 지난 배너(목록에서만 보임)
-- 배너가 아니면 기간을 비운다 (ck_notice_banner)
-- 본문의 규칙은 정책 문서에 있는 값만 쓴다 (PL-CTS-001·003, PL-USR-003, PL-REV-002, PL-RPT-002, PL-MSG-001)
-- =====================================================================

INSERT INTO notice (notice_id, admin_member_id, title, body, is_banner_shown, banner_starts_at, banner_ends_at, created_at, updated_at) VALUES
    (2001, 2, '바이브로그를 시작합니다',
     '바이브코딩 학습 플랫폼 바이브로그를 열었습니다.\n\n진단으로 내 트랙을 찾고, 수업과 퀴즈로 실력을 쌓고, 포트폴리오로 결과물을 남겨 보세요.\n이용하다가 불편한 점은 화면 아래 문의 이메일로 알려 주세요.',
     TRUE, NOW() - INTERVAL 42 DAY, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 42 DAY, NOW() - INTERVAL 42 DAY),
    (2002, 2, '처음 오셨다면 진단부터 해 보세요',
     '처음 로그인하면 5문항 진단이 열립니다. 고른 답을 바탕으로 Starter·Controller·Builder·Explorer 중 나에게 맞는 트랙을 추천해 드려요.\n\n진단은 건너뛸 수 있고, 마이페이지에서 언제든 다시 할 수 있습니다.',
     FALSE, NULL, NULL, NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY),
    (2003, 2, '이메일 인증 안내',
     '이메일 인증 전에도 로그인과 학습은 할 수 있습니다.\n게시글·댓글·포트폴리오·쪽지·리뷰를 쓰려면 가입할 때 받은 메일의 인증 링크를 눌러 주세요.\n\n메일이 오지 않았다면 스팸함을 먼저 확인해 주세요.',
     FALSE, NULL, NULL, NOW() - INTERVAL 37 DAY, NOW() - INTERVAL 37 DAY),
    (2004, 2, '커뮤니티 이용 규칙',
     '서로 배우는 공간이 되도록 아래 규칙을 지켜 주세요.\n\n- 다른 사람을 비난하거나 광고하는 글은 쓰지 않습니다\n- 코드와 함께 API 키·비밀번호를 올리지 않습니다\n\n규칙에 맞지 않는 글은 신고해 주세요. 서로 다른 3명이 신고하면 자동으로 숨겨지고 운영자가 확인합니다.',
     FALSE, NULL, NULL, NOW() - INTERVAL 33 DAY, NOW() - INTERVAL 33 DAY),
    (2005, 2, '틀린 문제 모아보기를 추가했습니다',
     '퀴즈에서 틀렸던 문항을 한곳에서 다시 볼 수 있습니다.\n내 학습 또는 수업 화면의 "틀린 문제 모아보기"에서 코스·수업별로 거르고, 문항마다 내 답·정답·해설과 관련 개념을 확인해 보세요.',
     FALSE, NULL, NULL, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 29 DAY),
    (2006, 2, '포트폴리오 동료 리뷰를 열었습니다',
     '제출한 포트폴리오에 다른 학습자의 리뷰를 받을 수 있습니다.\n리뷰 요청은 동시에 3개까지 할 수 있고, 요청 후 14일이 지나면 자동으로 마감됩니다.\n\n내 포트폴리오에서 "리뷰 요청"을 눌러 시작해 보세요.',
     FALSE, NULL, NULL, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
    (2007, 2, '쪽지 기능 안내',
     '공개 프로필에서 다른 학습자에게 쪽지를 보낼 수 있습니다.\n쪽지는 주고받은 두 사람만 볼 수 있고, 운영자는 신고된 쪽지만 확인합니다.\n불쾌한 쪽지를 받았다면 쪽지 화면에서 신고해 주세요.',
     FALSE, NULL, NULL, NOW() - INTERVAL 21 DAY, NOW() - INTERVAL 21 DAY),
    (2008, 2, '월간 콘테스트 일정 안내',
     '매달 한 가지 주제로 작품을 겨루는 월간 콘테스트를 엽니다.\n\n- 1일 ~ 20일: 출품 (1인 1작품, 공개 포트폴리오만)\n- 21일 ~ 말일: 투표 (계정당 하루 1표, 자기 작품에는 투표할 수 없음)\n- 다음 달 1일: 결과 발표, 1·2·3등에게 배지 지급',
     FALSE, NULL, NULL, NOW() - INTERVAL 17 DAY, NOW() - INTERVAL 17 DAY),
    (2009, 2, '모바일 화면을 개선했습니다',
     '휴대폰에서 코스 상세, 진단 결과, 퀴즈 화면이 화면 폭에 맞게 보이도록 고쳤습니다.\n여전히 화면이 잘리거나 겹치는 곳이 있다면 화면 이름과 함께 알려 주세요.',
     FALSE, NULL, NULL, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY),
    (2010, 2, '수강 포기와 다시 시작 안내',
     '내 학습에서 수강 중인 코스를 포기할 수 있습니다. 포기한 코스는 목록에서 내려갑니다.\n같은 코스를 다시 시작하면 이전에 하던 곳부터 이어서 학습합니다.',
     FALSE, NULL, NULL, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY),
    (2011, 2, '정기 점검 안내',
     '더 안정적인 서비스를 위해 정기 점검을 합니다.\n점검 중에는 모든 화면 대신 점검 안내가 보이며, 점검이 끝나면 바로 다시 이용할 수 있습니다.\n학습 기록은 그대로 유지됩니다.',
     FALSE, NULL, NULL, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),
    (2012, 2, CONCAT(MONTH(NOW()), '월 콘테스트가 진행 중입니다'),
     CONCAT('이번 달 콘테스트가 열렸습니다. 콘테스트 탭에서 이달의 주제를 확인해 보세요.\n\n',
            '- 1일 ~ 20일: 출품 (1인 1작품, 공개 포트폴리오만)\n',
            '- 21일 ~ 말일: 투표 (계정당 하루 1표)\n\n',
            '코스 포트폴리오뿐 아니라 개인 작품도 출품할 수 있습니다.'),
     TRUE, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 14 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);

-- =====================================================================
-- 쪽지 · 알림 · 신고 시연 데이터 (#450, FR-MSG-001·002, FR-NTF-001, FR-RPT-001~004, PL-RPT-002·003)
-- 직접 적는 ID: 이 블록의 모든 표에서 2101~2199 (#440 공지 2001~2099 · #451 2201~2299 와 겹치지 않게)
-- 시각은 NOW() 기준. 알림·쪽지는 시연 계정 42 코스완주 중심 (README "발표 시연 계정")
-- 알림이 가리키는 글·댓글이 실제로 그 회원의 것이 되도록, 42의 작품 공유 글 1개와 댓글·추천을 이 블록에서 함께 넣는다
-- 알림 문구는 앱이 만드는 문구 그대로 쓴다 (NotificationContractService · MessageService · NoticeService · ReportAdminService)
-- 신고자는 대량 데이터(#431) 작성자가 아닌 시연 계정으로 골라 자기 글을 신고하는 일이 없게 했다
-- =====================================================================

-- 42 코스완주의 작품 공유 글 + 추천 3개 + 댓글 1개 (RECOMMEND · COMMENT 알림 대상)
INSERT INTO post (post_id, member_id, category, title, body, work_url, tools, created_at, updated_at) VALUES
    (2101, 42, 'SHOWCASE', '범위를 정해 요청해서 할 일 앱 버그 고친 후기',
     '코스에서 배운 대로 "TodoItem.jsx의 handleToggle만 고치고 다른 파일은 바꾸지 마"라고 요청했더니 diff가 한 파일로 끝났어요.\n요청하지 않은 CSS 변경은 git restore로 되돌렸습니다. 처음 해 보시는 분들께 추천해요!',
     'https://example.com/todo-app', 'Claude Code', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY);

INSERT INTO recommendation (recommendation_id, member_id, post_id, created_at, updated_at) VALUES
    (2101, 3, 2101, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (2102, 4, 2101, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 30 HOUR),
    (2103, 5, 2101, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR);

INSERT INTO comment (comment_id, post_id, member_id, parent_comment_id, content, created_at, updated_at) VALUES
    (2101, 2101, 3, NULL, '건드리지 않을 곳까지 적는 게 핵심이네요. 저도 다음 요청부터 따라 해 볼게요!', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR);

-- 42가 다른 사람 글(#431 게시글 100010)에 단 댓글 + 그 댓글에 달린 답글 (REPLY 알림 대상)
INSERT INTO comment (comment_id, post_id, member_id, parent_comment_id, content, created_at, updated_at) VALUES
    (2102, 100010, 42, NULL, '저도 같은 고민이었는데 수정 범위를 먼저 적으니 훨씬 나아졌어요.', NOW() - INTERVAL 26 HOUR, NOW() - INTERVAL 26 HOUR),
    (2103, 100010, 6, 2102, '오 좋은 방법이네요. 혹시 요청 문장 예시도 공유해 주실 수 있나요?', NOW() - INTERVAL 90 MINUTE, NOW() - INTERVAL 90 MINUTE);

-- 쪽지: 42가 받은 쪽지(안 읽음·읽음), 42 ↔ 4 답장으로 이어진 대화, 신고할 광고 쪽지, 35 트랙고민중이 받은 환영 쪽지
INSERT INTO message (message_id, sender_member_id, receiver_member_id, parent_message_id, content, read_at, created_at, updated_at) VALUES
    (2101, 3, 42, NULL, '안녕하세요! 포트폴리오 잘 봤어요. diff 확인하는 부분이 특히 도움이 됐습니다. 혹시 평가 기준 체크리스트도 공유해 주실 수 있을까요?',
     NULL, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR),
    (2102, 7, 42, NULL, '코스 완주 축하드려요! 저도 이번 주 안에 포트폴리오 제출하려고 합니다.',
     NOW() - INTERVAL 47 HOUR, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 47 HOUR),
    (2103, 42, 4, NULL, '모의고사 2회차에서 만점 받으셨던데, 약한 개념은 어떻게 다시 보셨어요?',
     NOW() - INTERVAL 70 HOUR, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 70 HOUR),
    (2104, 4, 42, 2103, '리포트에서 약한 부분으로 표시된 개념을 다시 읽고 틀린 문제 모아보기로 한 번 더 풀었어요!',
     NULL, NOW() - INTERVAL 66 HOUR, NOW() - INTERVAL 66 HOUR),
    (2105, 9, 42, NULL, '[광고] 단 하루! AI 코딩 강의 전 과정 90% 할인. 지금 바로 링크로 신청하세요',
     NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 22 HOUR, NOW() - INTERVAL 20 HOUR),
    (2106, 8, 35, NULL, '바이브로그에 오신 걸 환영해요! Controller 트랙 첫 코스 같이 들어요 :)',
     NULL, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR);

-- 신고 (A-13 은 처리 대기 PENDING 을 대상별로 묶어 보여 준다)
--   게시글 100020: 서로 다른 3명 신고 → 자동 숨김 상태로 운영자 확인 대기 (PL-RPT-002)
--   게시글 100035 기타(내용 필수) · 댓글 1개(#431 첫 댓글) · 포트폴리오 1개(#431 첫 공개 포트폴리오) · 쪽지 2105(받은 사람 42만 신고 가능) → 각각 1건 대기
--   게시글 100060: 2명 신고 → 운영자 숨김 처리 HIDDEN (작성자에게 숨김 알림) / 게시글 100070: 기각 REJECTED
--   신고당한 회원(target_member_id)은 대상의 작성자를 찾아 넣는다
INSERT INTO report (report_id, reporter_member_id, target_type, target_id, target_member_id, reason, reason_detail, status, handler_member_id, handled_at, created_at, updated_at)
SELECT r.id, r.reporter, r.target_type, r.target_id,
       CASE r.target_type
         WHEN 'POST' THEN (SELECT p.member_id FROM post p WHERE p.post_id = r.target_id)
         WHEN 'COMMENT' THEN (SELECT c.member_id FROM comment c WHERE c.comment_id = r.target_id)
         WHEN 'PORTFOLIO' THEN (SELECT f.member_id FROM portfolio f WHERE f.portfolio_id = r.target_id)
         WHEN 'MESSAGE' THEN (SELECT m.sender_member_id FROM message m WHERE m.message_id = r.target_id)
       END,
       r.reason, r.detail, r.status, IF(r.status = 'PENDING', NULL, 2), r.handled_at, r.created_at, COALESCE(r.handled_at, r.created_at)
FROM (
    SELECT 2101 AS id, 35 AS reporter, 'POST' AS target_type, 100020 AS target_id, 'SPAM' AS reason, CAST(NULL AS CHAR(200)) AS detail,
           'PENDING' AS status, CAST(NULL AS DATETIME) AS handled_at, NOW() - INTERVAL 30 HOUR AS created_at
    UNION ALL SELECT 2102, 36, 'POST', 100020, 'SPAM', NULL, 'PENDING', NULL, NOW() - INTERVAL 20 HOUR
    UNION ALL SELECT 2103, 37, 'POST', 100020, 'ABUSE', NULL, 'PENDING', NULL, NOW() - INTERVAL 4 HOUR
    UNION ALL SELECT 2104, 38, 'POST', 100035, 'ETC', '본문과 상관없는 외부 링크를 여러 글에 반복해서 올려요', 'PENDING', NULL, NOW() - INTERVAL 10 HOUR
    UNION ALL SELECT 2105, 39, 'COMMENT', (SELECT MIN(c.comment_id) FROM comment c WHERE c.comment_id > 100000 AND c.deleted_at IS NULL),
                     'ABUSE', NULL, 'PENDING', NULL, NOW() - INTERVAL 7 HOUR
    UNION ALL SELECT 2106, 40, 'PORTFOLIO', (SELECT MIN(f.portfolio_id) FROM portfolio f WHERE f.portfolio_id > 100000 AND f.is_public = TRUE AND f.deleted_at IS NULL),
                     'PLAGIARISM', NULL, 'PENDING', NULL, NOW() - INTERVAL 15 HOUR
    UNION ALL SELECT 2107, 42, 'MESSAGE', 2105, 'SPAM', NULL, 'PENDING', NULL, NOW() - INTERVAL 19 HOUR
    UNION ALL SELECT 2108, 41, 'POST', 100060, 'ABUSE', NULL, 'HIDDEN', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 DAY
    UNION ALL SELECT 2109, 43, 'POST', 100060, 'ABUSE', NULL, 'HIDDEN', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY
    UNION ALL SELECT 2110, 33, 'POST', 100070, 'PRIVACY', NULL, 'REJECTED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 4 DAY
) r;

-- 알림: 42 코스완주 (안 읽음 6 · 읽음 3, 추천·댓글·답글·쪽지·공지 종류 골고루), 35 트랙고민중 쪽지 1, 숨김 처리된 글의 작성자 1
-- 리뷰 알림은 리뷰 데이터가 생기는 #451 에서, 배지 알림은 수상 데이터가 생기는 #430 에서 넣는다
INSERT INTO notification (notification_id, member_id, notification_type, target_type, target_id, content, read_at, created_at, updated_at)
SELECT n.id, n.member_id, n.type, n.target_type, n.target_id, n.content, n.read_at, n.created_at, COALESCE(n.read_at, n.created_at)
FROM (
    SELECT 2101 AS id, 42 AS member_id, 'RECOMMEND' AS type, 'POST' AS target_type, 2101 AS target_id,
           CAST('내 글 「범위를 정해 요청해서 할 일 앱 버그 고친 후기」이 추천을 받았습니다' AS CHAR(255)) AS content,
           NOW() - INTERVAL 40 HOUR AS read_at, NOW() - INTERVAL 2 DAY AS created_at
    UNION ALL SELECT 2102, 42, 'RECOMMEND', 'POST', 2101, '내 글 「범위를 정해 요청해서 할 일 앱 버그 고친 후기」이 추천을 받았습니다',
                     NULL, NOW() - INTERVAL 3 HOUR
    UNION ALL SELECT 2103, 42, 'COMMENT', 'COMMENT', 2101, '내 글 「범위를 정해 요청해서 할 일 앱 버그 고친 후기」에 댓글이 달렸습니다',
                     NULL, NOW() - INTERVAL 2 HOUR
    UNION ALL SELECT 2104, 42, 'REPLY', 'COMMENT', 2103,
                     CONCAT('내 댓글에 답글이 달렸습니다: 「', (SELECT p.title FROM post p WHERE p.post_id = 100010), '」'),
                     NULL, NOW() - INTERVAL 90 MINUTE
    UNION ALL SELECT 2105, 42, 'MESSAGE', 'MESSAGE', 2101, CONCAT((SELECT m.nickname FROM member m WHERE m.member_id = 3), '님이 쪽지를 보냈습니다'),
                     NULL, NOW() - INTERVAL 5 HOUR
    UNION ALL SELECT 2106, 42, 'MESSAGE', 'MESSAGE', 2104, CONCAT((SELECT m.nickname FROM member m WHERE m.member_id = 4), '님이 쪽지를 보냈습니다'),
                     NULL, NOW() - INTERVAL 66 HOUR
    UNION ALL SELECT 2107, 42, 'MESSAGE', 'MESSAGE', 2102, CONCAT((SELECT m.nickname FROM member m WHERE m.member_id = 7), '님이 쪽지를 보냈습니다'),
                     NOW() - INTERVAL 47 HOUR, NOW() - INTERVAL 2 DAY
    UNION ALL SELECT 2108, 42, 'NOTICE', 'NOTICE', 2012, CONCAT('새 공지사항: ', (SELECT t.title FROM notice t WHERE t.notice_id = 2012)),
                     NULL, NOW() - INTERVAL 1 DAY
    UNION ALL SELECT 2109, 42, 'NOTICE', 'NOTICE', 2011, '새 공지사항: 정기 점검 안내',
                     NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 4 DAY
    UNION ALL SELECT 2110, 35, 'MESSAGE', 'MESSAGE', 2106, CONCAT((SELECT m.nickname FROM member m WHERE m.member_id = 8), '님이 쪽지를 보냈습니다'),
                     NULL, NOW() - INTERVAL 1 HOUR
    UNION ALL SELECT 2111, (SELECT p.member_id FROM post p WHERE p.post_id = 100060), 'HIDDEN', 'POST', 100060,
                     CONCAT('운영 정책에 따라 내 게시글 「', (SELECT p.title FROM post p WHERE p.post_id = 100060), '」이(가) 숨겨졌습니다. 사유: 욕설/비방'),
                     NULL, NOW() - INTERVAL 1 DAY
) n;

-- =====================================================================
-- 월간 콘테스트: 지난달(결과 발표) + 이번 달(진행 중) (#430, FR-CTS-001~005, PL-CTS-001~006)
-- 직접 적는 ID: 101~199 (contest 101·102, portfolio 101~109, contest_entry 101~109, contest_award 101~104, notification 101~104)
--   투표(contest_vote)는 수가 많아 SELECT 로 만들고 ID 는 DB 가 채운다
-- 날짜는 NOW() 기준 → 언제 01 → 02 를 실행해도 "지난달 결과 + 이번 달 진행 중"이 보인다
-- 기간은 운영자 화면 기본값과 같다 (ContestForm): 1일 0시 ~ 21일 0시 출품, 21일 0시 ~ 다음 달 1일 0시 투표 (PL-CTS-001)
--
-- 지난달 "스크롤 액션 애니메이션 페이지": 출품 5개, 득표 4,820 · 3,150 · 3,150 · 2,040 · 980
--   수상은 시상 배치(ContestAwardService)와 같은 규칙: 표 많은 순, 같은 표면 같은 순위(1·1·2), 순위마다 2명까지, 3등까지
--   → 1등 1명, 2등 공동 2명 (PL-CTS-006), 3등 1명. 수상 기록이 있으니 배치가 다시 집계하지 않는다
-- 이번 달 "AI로 만든 미니게임": 출품 4개, 표 없음 (21일 이후에 실행하면 투표 중으로 보이고 0표에서 시작한다)
--
-- 출품작은 모두 공개 개인 작품 (PL-CTS-002). 출품자: 지난달 회원 3~7, 이번 달 회원 8~10·12 (1인 1작품)
--   두 달 모두 이 회원들이 가입한 뒤라(가입 63~90일 전) 언제 실행해도 시간 순서가 맞는다
-- 투표자: #431 대량 회원(100001~109970, 이메일 인증, 테스트 계정 아님 PL-CTS-004) 중 먼저 가입한 순서대로
--   작품마다 투표 날짜를 하루씩 다르게 둬서(21·22·23·24·25일) 계정당 하루 1표·같은 작품 1번을 지킨다 (PL-CTS-003,
--   uk_contest_vote_daily·uk_contest_vote_entry_voter). 출품자(3~7)는 투표자에 없어 자기 작품 투표도 없다
-- 실행 순서: #431 블록(대량 회원) 다음에 있어야 한다
-- =====================================================================

INSERT INTO contest (contest_id, contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at, created_at, updated_at)
SELECT 101, DATE_FORMAT(lm, '%Y-%m'), '스크롤 액션 애니메이션 페이지',
       '스크롤을 내리면 요소가 나타나고 움직이는 한 페이지를 만들어 보세요. AI에게 애니메이션을 요청할 때 고칠 곳과 건드리지 않을 곳을 정해 두면 다른 섹션이 깨지지 않습니다.',
       lm, lm + INTERVAL 20 DAY, lm + INTERVAL 20 DAY, cm, lm - INTERVAL 5 DAY, lm - INTERVAL 5 DAY
FROM (SELECT CAST(DATE_FORMAT(NOW() - INTERVAL 1 MONTH, '%Y-%m-01') AS DATETIME) AS lm,
             CAST(DATE_FORMAT(NOW(), '%Y-%m-01') AS DATETIME) AS cm) d
UNION ALL
SELECT 102, DATE_FORMAT(cm, '%Y-%m'), 'AI로 만든 미니게임',
       'AI와 함께 브라우저에서 바로 해 볼 수 있는 작은 게임을 만들어 보세요. 규칙 하나, 화면 하나여도 좋습니다. 기능을 작게 나눠 요청하고 바뀐 부분을 확인하는 과정도 함께 남겨 주세요.',
       cm, cm + INTERVAL 20 DAY, cm + INTERVAL 20 DAY, cm + INTERVAL 1 MONTH, cm - INTERVAL 5 DAY, cm - INTERVAL 5 DAY
FROM (SELECT CAST(DATE_FORMAT(NOW() - INTERVAL 1 MONTH, '%Y-%m-01') AS DATETIME) AS lm,
             CAST(DATE_FORMAT(NOW(), '%Y-%m-01') AS DATETIME) AS cm) d;

-- 출품용 공개 개인 작품 9개 (PERSONAL 은 course_id NULL, ck_portfolio_type_course). 회고는 100자 이상 (PL-PRT-003)
--   지난달 작품은 지난달 출품 기간 안에, 이번 달 작품은 이번 달이 되기 직전에 만든 것으로 한다
INSERT INTO portfolio (portfolio_id, member_id, course_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public, created_at, updated_at)
SELECT p.id, p.member_id, NULL, 'PERSONAL', p.title, p.url, p.summary, p.retro, p.tools, TRUE,
       IF(p.last_month, d.lm, d.cm) + INTERVAL p.off_hour HOUR,
       IF(p.last_month, d.lm, d.cm) + INTERVAL p.off_hour HOUR
FROM (
    SELECT 101 AS id, 3 AS member_id, TRUE AS last_month, 58 AS off_hour,
           '스크롤하면 피어나는 꽃 랜딩 페이지' AS title,
           'https://github.com/vibelog-sample/scroll-bloom' AS url,
           '스크롤 위치에 따라 꽃잎이 하나씩 피어나는 한 페이지 랜딩이다' AS summary,
           '처음에는 "스크롤 애니메이션 넣어 줘"라고만 했더니 헤더와 버튼 스타일까지 바뀌었다. 꽃 섹션의 CSS 파일 하나만 고치고 나머지는 그대로 두라고 범위를 적어 다시 요청했고, diff에서 요청하지 않은 폰트 변경 한 줄을 찾아 되돌렸다.' AS retro,
           'Claude Code' AS tools
    UNION ALL SELECT 102, 4, TRUE, 117, '패럴랙스 우주 여행 페이지', 'https://github.com/vibelog-sample/parallax-space',
           '배경 별과 행성이 서로 다른 속도로 움직이는 패럴랙스 스크롤 페이지',
           '레이어마다 속도를 다르게 주는 부분을 한 번에 요청하니 코드가 길어져 확인이 어려웠다. 별 레이어, 행성 레이어, 글자 레이어 순서로 나눠 요청하고 매번 커밋해 두었더니 어느 단계에서 깨졌는지 바로 찾을 수 있었다.',
           'Cursor'
    UNION ALL SELECT 103, 5, TRUE, 158, '스크롤로 넘기는 그림책', 'https://github.com/vibelog-sample/scroll-picture-book',
           '스크롤할 때마다 그림책 한 장면씩 넘어가고 글자가 타이핑되듯 나온다',
           '장면 전환은 잘 됐지만 모바일에서 글자가 겹치는 문제가 있었다. 장면 컴포넌트는 건드리지 말고 글자 스타일 파일만 고치라고 요청해 해결했다. 테스트는 없었지만 화면 폭을 바꿔 가며 직접 확인하는 체크리스트를 만들었다.',
           'Claude Code, Git'
    UNION ALL SELECT 104, 6, TRUE, 225, '숫자가 올라가는 회사 소개 페이지', 'https://github.com/vibelog-sample/count-up-intro',
           '보이는 순간 통계 숫자가 0부터 올라가는 스크롤 연출을 넣은 소개 페이지',
           '숫자가 보이기 전에 미리 올라가 버리는 버그가 있었다. 감지 함수 하나만 고쳐 달라고 범위를 정해 요청했더니 다른 섹션은 그대로 두고 그 부분만 바뀌었다. diff를 읽는 습관 덕분에 요청하지 않은 변경이 없는지 빠르게 확인했다.',
           'GitHub Copilot'
    UNION ALL SELECT 105, 7, TRUE, 306, '스크롤 따라 달리는 고양이', 'https://github.com/vibelog-sample/running-cat',
           '스크롤 진행률에 맞춰 고양이가 화면 아래를 달리고 도착하면 인사한다',
           '고양이 이미지가 스크롤 속도보다 늦게 따라오는 문제가 있었다. 처음에는 AI가 전체 구조를 바꾸려 해서 되돌렸고, 위치 계산 함수만 고치라고 다시 요청했다. 작은 범위로 나눠 요청하니 원하는 부분만 바뀌었다.',
           'Cursor, Claude Code'
    UNION ALL SELECT 106, 8, FALSE, -72, '단어 맞히기 미니게임', 'https://github.com/vibelog-sample/word-guess',
           '다섯 글자 영어 단어를 여섯 번 안에 맞히는 브라우저 게임',
           '게임 규칙과 화면을 한 번에 만들어 달라고 했다가 코드가 엉켰다. 규칙 검사 함수, 키보드 입력, 결과 표시 순서로 나눠 요청하고 단계마다 커밋했다. 규칙 함수에 테스트를 먼저 만들어 달라고 한 것이 가장 도움이 됐다.',
           'Claude Code'
    UNION ALL SELECT 107, 9, FALSE, -48, '두더지 잡기 타이머 게임', 'https://github.com/vibelog-sample/whack-a-mole',
           '30초 동안 나타나는 두더지를 눌러 점수를 모으는 게임',
           '두더지가 나타나는 간격을 바꿔 달라고 했더니 점수 계산 코드까지 바뀌었다. 간격을 정하는 상수만 바꾸고 점수 함수는 건드리지 말라고 적어 다시 요청했다. 바뀐 부분이 짧아져 확인이 훨씬 쉬웠다.',
           'Cursor'
    UNION ALL SELECT 108, 10, FALSE, -24, '숫자 합치기 퍼즐 게임', 'https://github.com/vibelog-sample/merge-numbers',
           '같은 숫자 타일을 합쳐 큰 수를 만드는 퍼즐을 방향키로 즐긴다',
           '타일이 합쳐지는 규칙이 가끔 두 번 적용되는 버그가 있었다. 합치기 함수 하나만 보라고 요청하고, 버그가 나는 상황을 그대로 적어 줬더니 원인을 바로 찾았다. 다음에는 처음부터 테스트할 상황을 함께 적어 요청하겠다.',
           'Claude Code, Git'
    UNION ALL SELECT 109, 12, FALSE, -18, '리듬 맞춰 누르기 게임', 'https://github.com/vibelog-sample/rhythm-tap',
           '내려오는 노트에 맞춰 키를 누르면 판정이 뜨는 간단한 리듬 게임',
           '판정 범위를 넓혀 달라고 했을 뿐인데 노트 속도까지 바뀌었다. 판정 함수만 고치고 속도 설정은 그대로 두라고 범위를 적어 다시 요청했고, diff로 요청하지 않은 변경이 없는지 확인했다. 작은 요청이 결국 더 빨랐다.',
           'GitHub Copilot'
) p
CROSS JOIN (SELECT CAST(DATE_FORMAT(NOW() - INTERVAL 1 MONTH, '%Y-%m-01') AS DATETIME) AS lm,
                   CAST(DATE_FORMAT(NOW(), '%Y-%m-01') AS DATETIME) AS cm) d;

-- 출품: 지난달은 작품을 만든 지 1시간 뒤, 이번 달은 출품 기간 첫날 9시부터 하루씩 (단 지금보다 미래가 되지 않게)
INSERT INTO contest_entry (contest_entry_id, contest_id, member_id, portfolio_id, canceled_at, created_at, updated_at)
SELECT x.id, x.contest_id, p.member_id, p.portfolio_id, NULL, t.t, t.t
FROM (SELECT 101 AS id, 101 AS contest_id, 101 AS portfolio_id UNION ALL
      SELECT 102, 101, 102 UNION ALL
      SELECT 103, 101, 103 UNION ALL
      SELECT 104, 101, 104 UNION ALL
      SELECT 105, 101, 105 UNION ALL
      SELECT 106, 102, 106 UNION ALL
      SELECT 107, 102, 107 UNION ALL
      SELECT 108, 102, 108 UNION ALL
      SELECT 109, 102, 109) x
JOIN portfolio p ON p.portfolio_id = x.portfolio_id
CROSS JOIN LATERAL (
    SELECT IF(x.contest_id = 101, p.created_at + INTERVAL 1 HOUR,
              GREATEST(d.cm, LEAST(d.cm + INTERVAL (x.id - 106) DAY + INTERVAL 9 HOUR,
                                   NOW() - INTERVAL ((110 - x.id) * 7) MINUTE))) AS t
    FROM (SELECT CAST(DATE_FORMAT(NOW(), '%Y-%m-01') AS DATETIME) AS cm) d
) t;

-- 투표 (지난달): 작품마다 정한 표 수만큼, 먼저 가입한 대량 회원부터. 작품 순서 d 에 따라 21+d 일에 투표한다
INSERT INTO contest_vote (contest_id, contest_entry_id, voter_member_id, vote_date, created_at, updated_at)
SELECT 101, e.entry_id, v.member_id, DATE(e.day),
       e.day + INTERVAL (v.member_id % 1380) MINUTE, e.day + INTERVAL (v.member_id % 1380) MINUTE
FROM (
    SELECT entry_id, votes, CAST(DATE_FORMAT(NOW() - INTERVAL 1 MONTH, '%Y-%m-01') AS DATETIME) + INTERVAL (20 + d) DAY AS day
    FROM (SELECT 101 AS entry_id, 4820 AS votes, 0 AS d UNION ALL
          SELECT 102, 3150, 1 UNION ALL
          SELECT 103, 3150, 2 UNION ALL
          SELECT 104, 2040, 3 UNION ALL
          SELECT 105, 980, 4) t
) e
JOIN (SELECT member_id, created_at, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE member_id BETWEEN 100001 AND 109970) v
  ON v.rn <= e.votes AND v.created_at < e.day;

-- 수상 (지난달): 투표가 끝난 이번 달 1일 0시 직후 시상 배치가 남긴 것으로 한다. 수상 1건 = 배지 1개 (FR-RWD-001)
INSERT INTO contest_award (contest_award_id, contest_id, contest_entry_id, award_rank, created_at, updated_at)
SELECT a.id, 101, a.entry_id, a.award_rank, d.t, d.t
FROM (SELECT 101 AS id, 101 AS entry_id, 1 AS award_rank UNION ALL
      SELECT 102, 102, 2 UNION ALL
      SELECT 103, 103, 2 UNION ALL
      SELECT 104, 104, 3) a
CROSS JOIN (SELECT CAST(DATE_FORMAT(NOW(), '%Y-%m-01') AS DATETIME) + INTERVAL 10 MINUTE AS t) d;

-- 배지 알림 (지난달 수상자 4명): 시상 배치가 수상과 함께 보내는 알림과 같은 모양 (NotificationContractService.badgeAwarded,
--   내용 "배지를 받았습니다: {yyyy}년 {M}월 {순위}등" = ContestAward.badgeName). 안 읽은 상태라 수상자로 로그인하면
--   배지 획득 축하(M-10)가 뜬다. 직접 적는 ID 101~104 (#459 블록 주석: 배지 알림은 수상 데이터가 생기는 #430 에서 넣는다)
INSERT INTO notification (notification_id, member_id, notification_type, target_type, target_id, content, read_at, created_at, updated_at)
SELECT a.contest_award_id, e.member_id, 'BADGE', 'CONTEST_AWARD', a.contest_award_id,
       CONCAT('배지를 받았습니다: ', LEFT(c.contest_month, 4), '년 ', CAST(SUBSTRING(c.contest_month, 6, 2) AS UNSIGNED), '월 ', a.award_rank, '등'),
       NULL, a.created_at, a.created_at
FROM contest_award a
JOIN contest_entry e ON e.contest_entry_id = a.contest_entry_id
JOIN contest c ON c.contest_id = a.contest_id
WHERE a.contest_award_id BETWEEN 101 AND 104;
