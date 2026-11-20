-- V321: 총판 0000000002(OTL JP USD) 아래 가맹 TEST USD 생성
-- 원본: 6000000067 DEALMAI DEMO 계정·프로필 복사
-- 제외: 결제대행사 바인딩(KSNET 수기 연동 예정), THB 기준 통화·정산 최소액, OMSPAY 노티 URL, 크립토설정
-- 로그인: testusd (비밀번호는 demoth 와 동일 해시)
SET client_min_messages TO WARNING;

DO $$
DECLARE
  v_parent_id BIGINT;
  v_src_ou_id BIGINT;
  v_new_ou_id BIGINT;
  v_new_code  TEXT := '6000000069';
  v_login     TEXT := 'testusd';
  v_rs        TEXT;
BEGIN
  SELECT id INTO v_parent_id FROM tb_org_unit WHERE code = '0000000002';
  IF v_parent_id IS NULL THEN
    RAISE EXCEPTION 'parent 0000000002 (OTL JP USD) not found';
  END IF;

  SELECT id INTO v_src_ou_id FROM tb_org_unit WHERE code = '6000000067';
  IF v_src_ou_id IS NULL THEN
    RAISE EXCEPTION 'source merchant 6000000067 not found';
  END IF;

  IF EXISTS (SELECT 1 FROM tb_org_unit WHERE code = v_new_code) THEN
    RAISE NOTICE 'merchant % already exists — skip create', v_new_code;
    RETURN;
  END IF;

  IF EXISTS (SELECT 1 FROM tb_merchant_profile WHERE login_id = v_login) THEN
    RAISE EXCEPTION 'login_id % already exists', v_login;
  END IF;

  IF EXISTS (SELECT 1 FROM tb_user WHERE username = v_login) THEN
    RAISE EXCEPTION 'username % already exists', v_login;
  END IF;

  INSERT INTO tb_org_unit (
    org_level, parent_id, code, name, status, created_at,
    page_permission_mode, tablet_feature_use_yn
  ) VALUES (
    'MERCHANT', v_parent_id, v_new_code, 'TEST USD', 'ACTIVE', NOW(),
    'LEVEL_DEFAULT', 'Y'
  )
  RETURNING id INTO v_new_ou_id;

  /* 프로필 전체 복사 후 USD·로그인·수수료 JSON 조정 */
  CREATE TEMP TABLE tmp_mp_test_usd ON COMMIT DROP AS
  SELECT * FROM tb_merchant_profile WHERE org_unit_id = v_src_ou_id;

  UPDATE tmp_mp_test_usd SET
    id = nextval('tb_merchant_profile_id_seq'),
    org_unit_id = v_new_ou_id,
    comp_div = 'MERCHANT',
    login_id = v_login,
    base_currency = 'USD',
    trade_nm = COALESCE(NULLIF(TRIM(trade_nm), ''), 'TEST USD'),
    /* THB 정산·QR 통화 흔적 제거 → USD 테스트용 */
    regional_settings = COALESCE(
      (
        SELECT jsonb_set(
                 jsonb_set(
                   COALESCE(NULLIF(BTRIM(regional_settings), ''), '{}')::jsonb,
                   '{qrCurrencyCode}',
                   '"USD"'::jsonb,
                   true
                 ),
                 '{fiatCommissionSlotYn}',
                 '"Y"'::jsonb,
                 true
               )::text
      ),
      '{"commissionFollowHq":"Y","hqPolicyScope":"","commissionCryptoFollowHq":"Y","hqCryptoPolicyScope":"","fiatCommissionSlotYn":"Y"}'
    ),
    created_at = NOW();

  INSERT INTO tb_merchant_profile SELECT * FROM tmp_mp_test_usd;

  /* 정산: 사이클 구조는 유지, THB 규모 최소이체/최소정산액은 비움(USD에서 재설정) */
  CREATE TEMP TABLE tmp_ss_test_usd ON COMMIT DROP AS
  SELECT * FROM tb_settlement_setting WHERE org_unit_id = v_src_ou_id;

  IF EXISTS (SELECT 1 FROM tmp_ss_test_usd) THEN
    UPDATE tmp_ss_test_usd SET
      id = nextval('tb_settlement_setting_id_seq'),
      org_unit_id = v_new_ou_id,
      auto_transfer_min = NULL,
      calc_min_amt = NULL,
      pay_limit_default = NULL,
      pay_limit_extra = NULL,
      pay_limit_month = NULL,
      pay_limit_year = NULL,
      pay_limit_daily = NULL,
      same_card_limit_amt_web = NULL,
      same_card_limit_amt_terminal = NULL,
      pay_lmt_tx_max = NULL,
      pay_lmt_tx_min = NULL,
      pay_lmt_day = NULL,
      pay_lmt_month = NULL,
      pay_lmt_year_corp = NULL,
      pay_lmt_year_ind = NULL,
      pay_lmt_tx_max_mode = COALESCE(pay_lmt_tx_max_mode, 'FOLLOW'),
      pay_lmt_tx_min_mode = COALESCE(pay_lmt_tx_min_mode, 'FOLLOW'),
      pay_lmt_day_mode = COALESCE(pay_lmt_day_mode, 'FOLLOW'),
      pay_lmt_month_mode = COALESCE(pay_lmt_month_mode, 'FOLLOW'),
      pay_lmt_year_corp_mode = COALESCE(pay_lmt_year_corp_mode, 'FOLLOW'),
      pay_lmt_year_ind_mode = COALESCE(pay_lmt_year_ind_mode, 'FOLLOW'),
      pay_lmt_ui_mode = COALESCE(pay_lmt_ui_mode, 'FOLLOW');
    INSERT INTO tb_settlement_setting SELECT * FROM tmp_ss_test_usd;
  END IF;

  /* 로그인 계정 — demoth 비밀번호 해시 재사용, 업체코드만 신규 */
  IF EXISTS (SELECT 1 FROM tb_user WHERE username = 'demoth') THEN
    INSERT INTO tb_user (
      username, password, name, role, enabled, created_at, org_unit_code,
      permission_group_nm, otp_registered_yn, user_type, assistant_role_type,
      parent_username, menu_policy_code, password_must_change_yn, mobile,
      user_status, inactive_reason
    )
    SELECT
      v_login,
      u.password,
      'TEST USD',
      u.role,
      TRUE,
      NOW(),
      v_new_code,
      u.permission_group_nm,
      'N',
      u.user_type,
      u.assistant_role_type,
      u.parent_username,
      u.menu_policy_code,
      'N',
      u.mobile,
      'ACTIVE',
      NULL
    FROM tb_user u
    WHERE u.username = 'demoth'
    LIMIT 1;
  ELSE
    RAISE NOTICE 'demoth user not found — merchant created without login user';
  END IF;

  RAISE NOTICE 'created merchant % id=% login=% under parent 0000000002 (no PG binding)',
    v_new_code, v_new_ou_id, v_login;
END $$;

-- 검증
SELECT ou.code, ou.name, p.code AS parent_code, p.name AS parent_name,
       mp.login_id, mp.base_currency, mp.use_yn, mp.web_payment_use_yn
FROM tb_org_unit ou
JOIN tb_org_unit p ON p.id = ou.parent_id
JOIN tb_merchant_profile mp ON mp.org_unit_id = ou.id
WHERE ou.code = '6000000069';

SELECT COUNT(*) AS binding_cnt
FROM tb_merchant_pg_binding b
JOIN tb_org_unit ou ON ou.id = b.org_unit_id
WHERE ou.code = '6000000069';
