-- pgdev DB에서 실행하세요. (pgAdmin에서 pgdev 선택 후 Query Tool에서 실행)
-- tb_user 테이블 생성 + admin 계정 추가 (Spring Boot 앱 없이 DB만으로 준비)

-- 1) pgcrypto 확장 (BCrypt 비밀번호용)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2) tb_user 테이블 (AppUser 엔티티와 동일 구조)
CREATE TABLE IF NOT EXISTS tb_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password        VARCHAR(100) NOT NULL,
    name            VARCHAR(100),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP
);

-- 3) admin 계정 추가 (비밀번호: admin1!)
-- pgcrypto crypt('admin1!', gen_salt('bf')) = Spring BCrypt와 호환
INSERT INTO tb_user (username, password, name, role, enabled, created_at)
SELECT 'admin', crypt('admin1!', gen_salt('bf')), '관리자', 'ADMIN', true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_user WHERE username = 'admin');
