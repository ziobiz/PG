-- V266: 가맹 6000000064 샌드박스 사용 활성
SET client_min_messages TO WARNING;

UPDATE tb_merchant_profile mp
SET sandbox_use_yn = 'Y'
FROM tb_org_unit ou
WHERE mp.org_unit_id = ou.id
  AND ou.code = '6000000064';
