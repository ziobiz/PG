# ox (OxPay Financial) — ICOPAY ↔ NOTI 연동 계약

병칭 **ox**. 가맹·구매자 노출은 항상 `ICOPAY`.

## NOTI (저장소 `D:\Delopment\NOTI`)

| 항목 | 값 |
|---|---|
| Webhook | `POST https://noti.icopay.net/noti/ox` |
| Result | `GET\|POST https://noti.icopay.net/noti/result/ox` |
| Provision | `pgKind: "ox"` (슬롯 없음) |
| ICOPAY ingress | `…/pg-notify/{token}/OX` (`config/ox-ingress.json`) |
| 가맹 통보 스키마 | `lib/oxNoti.js` → JPAY/EP와 동일 (`returncode`/`orderid`/…) |

상세: NOTI `docs/ICOPAY_Provision_API_ox.md`

## ICOPAY (본 저장소)

| 항목 | 상태 |
|---|---|
| `PgVendor.OX` | 완료 |
| 노티생성 UI `pgKind=ox` | 완료 |
| 통합 인라인 분기 | 완료 (골격) |
| Merchant Hosted Create Payment | **OxPay API 문서(엔드포인트·서명·3DS) 확정 후 완성** |
| HPP 카드 UI | 사용 금지 (인라인 1회만) |

## 채널 (승인 API 제외 · 2026-09-29)

웹 결제, API 인라인, API 리다이렉트, 챗봇(URL), 분할, WooCommerce(통합 prepare)는 운영 PG가 OX이면 `/checkout` → 내부 `ox-pay.html` 로 연다. 가맹 응답 `pgVendor`는 ICOPAY. 결제 버튼은 승인 스펙 전까지 «카드 승인을 준비 중»이다.

ICOPAY ingress `…/OX` 는 `returncode`/`orderid`/`pgKind=ox` 로 기존 거래를 성공(10)·실패(99) 반영 후 가맹 Background/Result로 넘긴다.

구독·Create Payment·3DS·조회·환불·OxPay 원문 웹훅 매핑은 상세 문서 이후.

1. OxPay Merchant Hosted Create Payment API 스펙 (요청/응답/3DS redirect)
2. Sandbox·Live base URL · API Key / Sign Key 샘플
3. Webhook 서명 방식 (있을 경우)
4. 보드된 MID/TID
