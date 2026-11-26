# INICIS 일본결제 — PayPay · 라쿠텐페이 연동 분석 (개발 전)

> 상태: **분석·합의 문서** (구현 착수 전)  
> 일자: 2026-11-26  
> 범위: PayPay · 라쿠텐페이만 (편의점·기타 APM · 일본카드는 1차 제외)

---

## 1. 목적

ICOPAY에 **KG이니시스 일본결제(CBT)** 를 결제대행사로 추가하여,  
가맹에게 **PayPay / 라쿠텐페이** 를 직접 서비스한다.

기존 ICOPAY 원칙을 그대로 적용한다.

| 원칙 | 적용 |
|---|---|
| ICOPAY ↔ 이니시스 | **1:1** (집계 MID 하나) |
| ICOPAY ↔ 가맹 | **1:N** |
| 이니시스가 하위 가맹을 알면 안 됨 | 영수증·결제창 상호 = **계약사(온더라인) 고정** |
| 내부 분리 | `compId` / `merchant_id`(업체코드) |
| 가맹·구매자 노출 | 항상 `pgVendor=ICOPAY` |

---

## 2. 문서·제품 구분 (혼동 금지)

| URL / 제품 | 용도 | 이번 범위 |
|---|---|---|
| [INIpay PRO](https://manual.inicis.com/pay/pro.html) | **국내** 일반결제 (CARD/BANK/VBANK 등) | **사용 안 함** |
| [일본결제 소개](https://www.inicis.com/japanpayments/) | 마케팅·상품 소개 | 참고 |
| [CBT결제 매뉴얼](https://manual.inicis.com/jppay/cbtauth.html) | 일본 CBT (`cbtauth` / `cbtapprove`) | **연동 기준** |
| [승인대사](https://manual.inicis.com/jppay/urlsvc.html) | 일별 대사 | 운영 시 검토 |

일본 PayPay·라쿠텐은 **CBT(JPPG/SBPS)** 이다. PRO로 가면 안 된다.

---

## 3. 1차 결제수단

| 수단 | 1차 | 비고 |
|---|---|---|
| PayPay | ✅ | CBT `paymethod`로 제한 |
| 라쿠텐페이 | ✅ | 동일 |
| 편의점(CVS) | ❌ | 입금 NOTI·영수증 표기 부담 — 제외 |
| 일본 신용카드 | ❌ | 1차 제외 |
| 기타 간편결제 | ❌ | 추후 옵션 |

통화 전제: **JPY**.

---

## 4. 합의 UX

### 4.1 앞단 — 통합 결제창 (수단 선택)

기존 **`/checkout-all`** 계열을 확장한다.

```
통합 결제창
  ├─ 카드      → 기존 카드 PG (KSNET·JPAY·…)
  ├─ 크립토    → 기존 CRYPTO
  ├─ PayPay    → 이니시스 CBT → PayPay
  └─ 라쿠텐페이 → 이니시스 CBT → 라쿠텐
```

- 서버가 허용한 수단만 표시 (가맹 바인딩·운영 ON).
- QBANG PayPay와 이니시스 PayPay가 **동시에 안 겹치게** 운영 PG/바인딩으로 하나만 사용.

### 4.2 URL결제와 동일 층

1. 가맹/구매자 → ICOPAY URL결제(중립 `/checkout` 또는 `/checkout-all`)
2. PayPay / 라쿠텐 선택
3. ICOPAY → 이니시스 `cbtauth` → **이니시스 호스티드 창**
4. 이니시스가 PayPay·라쿠텐과 연동해 결제
5. return + `cbtapprove` → ICOPAY `pg_trnsctn` → 가맹 통보 · NOTI

카드 인라인(PAN 입력)이 아니라 **호스티드/리다이렉트 APM**이다.

### 4.3 영수증·창 표기

- 계약자 = **온더라인** → 이니시스/원천 표기도 온더라인(또는 ICOPAY 집계 상호).
- 하위 가맹 상호는 이니시스에 **넣지 않음**.
- 내부 정산·조회만 업체코드로 분리.

---

## 5. ICOPAY 표준 운영 로직 (그대로 따름)

신규 PG와 동일한 등록·운영 절차:

```
① 결제대행사 등록 (본사 tb_pg_agency, pg_cd 예: INICIS / INICIS_JP)
② NOTI 입구·노티거래내역 탭 (POST /noti/{slug}, pgProvider, Comp-Id 릴레이)
③ 가맹 등록 · 결제대행사 바인딩 (tb_merchant_pg_binding, operational)
④ 통합 prepare / 중립 checkout → 운영 PG 디스패치
⑤ PG 호출 → pg_trnsctn(van=INICIS*) → 가맹 Callback + NOTI
```

체크리스트 앵커: `pg-unified-payment-integration.mdc`, `pg-new-vendor-noti-tx-mandatory.mdc`.

---

## 6. API ↔ ICOPAY 단계 매핑

| ICOPAY | 이니시스 CBT |
|---|---|
| agency 집계 `mid` + INIAPIKey | 가맹 MID 미사용 |
| prepare / session · orderId | `orderId` = ICOPAY 불투명 ref |
| 수단 선택 후 진행 | `POST …/cbtauth` |
| 서버 승인 확정 | `POST …/cbtapprove` |
| `extraData` 상호·연락처 | **온더라인 고정** (`merchantName*` 등) |
| `paymethod` | PayPay · 라쿠텐만 |
| 결과·취소·대사 | returnUrl + (필요 시) NOTI / INIAPI / 승인대사 |
| 내부 분리 | `merchant_id` = 업체코드, `van` = INICIS 계열 |

`cbtType` 은 **JPPG 또는 SBPS 중 하나**(MID당 양립 불가) — 영업 확인.

---

## 7. QBANG PayPay 경험과의 관계

| | QBANG (기존) | 이니시스 CBT (이번) |
|---|---|---|
| 인식 | PayPay (Alipay+ 중계) | 일본 PayPay·라쿠텐 |
| UI | ICOPAY QR + PL 폴링 | **이니시스 호스티드** |
| API | TK→QR→PL→OKAY | cbtauth → cbtapprove |
| 운영 등록 | agency → NOTI → 가맹 바인딩 | **동일** |
| 가맹 미식별 | 집계 posId | 집계 mid + 온더라인 표기 |

- 재사용: **PG 추가 운영 로직**, 집계 MID, 가맹 분리, NOTI.
- 결제 엔진 템플릿: QBANG QR보다 **Eximbay / OMSPAY·PAYSO형 리다이렉트** 에 가깝다.
- QBANG Alipay+ 프로토콜을 이니시스에 재사용하지 않는다.

---

## 8. 구조도

```
[가맹 N] ──(업체코드)──► ICOPAY
                           │
                           ├─ 결제대행사: INICIS (1)
                           ├─ 노티: /noti/inicis (가칭) + 본사 수신통보
                           └─ 바인딩: 가맹별 operational PG
                                      │
                                      ▼
                         통합 결제창 (/checkout-all)
                         카드 | 크립토 | PayPay | 라쿠텐
                                      │
                         PayPay/라쿠텐 선택 시
                                      ▼
                         이니시스 CBT (mid=온더라인)
                         표기=온더라인 · JPY
                                      │
                                      ▼
                         pg_trnsctn (merchant_id 분리)
                         → 가맹 Callback + NOTI 거래내역
```

---

## 9. 개발 착수 전 — 이니시스 영업/계약 확인

1. 일본 CBT MID로 **PayPay + 라쿠텐만** 계약 가능한지 (CARD/CVS 제외)
2. **JPPG vs SBPS**
3. 결제창·영수증 상호를 **항상 온더라인(집계)만** 써도 되는지 (플랫폼/집계형)
4. 통화 **JPY**, 정산 KRW/JPY, 취소·부분취소
5. returnUrl / NOTI IP / 방화벽 / 승인대사 신청

여기 OK 후 구현 착수.

---

## 10. 구현 시 예상 작업 (착수 시 — 현재는 문서만)

- `PgVendor` · `tb_pg_agency` · 가맹 바인딩
- Unified / URL `/checkout-all` 수단 + 이니시스 디스패치
- 중립 checkout forward · 호스티드 return
- NOTI `POST /noti/inicis` · 노티거래내역 탭 · `pgProvider`
- 다국어 5개 · 플랫폼 버전 마이너 +0.1
- QBANG PayPay와 수단 라벨/바인딩 충돌 방지

---

## 11. 결론

- **연동 가능.** 1차는 PayPay + 라쿠텐, 제품은 일본 **CBT** (PRO 아님).
- **영수증 = 온더라인**, 내부 = 업체코드 → 가맹 미식별·1:1과 맞음.
- **운영 로직**은 기존 결제대행사 추가와 동일.
- **UX**는 통합 결제창에서 수단 선택 후, PayPay/라쿠텐만 이니시스로 넘김.
- **지금은 분석·합의만.** 코드 구현은 계약 확인 후.

---

## 참고 링크

- https://manual.inicis.com/jppay/cbtauth.html  
- https://manual.inicis.com/jppay/urlsvc.html  
- https://www.inicis.com/japanpayments/  
- (비대상) https://manual.inicis.com/pay/pro.html  
- 관련 규칙: `.cursor/rules/pg-unified-payment-integration.mdc`, `pg-new-vendor-noti-tx-mandatory.mdc`, `pg-qbang-qr-alipay-only.mdc`
