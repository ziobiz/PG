-- V168 이 CHAR(1)로 추가된 컬럼을 기존 show_on_login/show_as_popup 과 동일하게 VARCHAR(1) 로 맞춤 (Hibernate schema-validation)
ALTER TABLE pg_notice
    ALTER COLUMN show_post_login_popup TYPE VARCHAR(1) USING show_post_login_popup::varchar(1);

ALTER TABLE pg_notice
    ALTER COLUMN show_on_main TYPE VARCHAR(1) USING show_on_main::varchar(1);
