-- 로컬 개발용 DB와 계정을 만든다. (최초 1회, MySQL root 계정으로 실행)
-- 테이블 생성 스크립트는 DB 설계가 확정되면 01-schema.sql 로 추가한다.
CREATE DATABASE IF NOT EXISTS vibelog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'vibelog'@'%' IDENTIFIED BY 'vibelog';
GRANT ALL PRIVILEGES ON vibelog.* TO 'vibelog'@'%';
FLUSH PRIVILEGES;
