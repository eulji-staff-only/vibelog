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
