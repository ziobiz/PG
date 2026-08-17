/**
 * ICOPAY 라이브 버전 · 릴리스 노트 (본사정책 > 플랫폼 > 업데이트 내용).
 * 큰 변화: 메이저(2.0, 3.0) · 소소한 변경: 마이너(2.1, 2.2…).
 * 결제대행사(PG) 신규 추가·연동 시: 반드시 마이너 +0.1 (예: 2.2 → 2.3).
 */
(function (global) {
  'use strict';

  var CURRENT_LIVE = '3.37';

  /**
   * howTo: { KO|EN|JP|CH|TH: Array<{ title:string, steps:string[] }> }
   * @type {Array<{version:string,kind:string,date:string,items:object,howTo?:object}>}
   */
  var RELEASES = [
    {
      version: '3.37',
      kind: 'minor',
      date: '2026-08-18',
      items: {
        KO: [
          'ElementPay 라이브: 거절·환불·차지백·환불요청 웹훅을 결제내역 상태에 반영. Merchant API 응답 서명 확인. 결제창 결과 메시지 5개국어'
        ],
        EN: [
          'ElementPay go-live: reject/refund/chargeback/refund-request webhooks update payment history. Merchant API response signature check. Checkout result messages in 5 languages'
        ],
        JP: [
          'ElementPay本番: 拒否・返金・チャージバック・返金依頼Webhookを取引状態へ反映。Merchant API応答署名確認。決済画面結果を5言語'
        ],
        CH: [
          'ElementPay 上线：拒绝/退款/拒付/退款申请 Webhook 写入交易状态。校验 Merchant API 响应签名。结账结果支持 5 语'
        ],
        TH: [
          'ElementPay ไลฟ์: webhook ปฏิเสธ/คืนเงิน/ชาร์จแบ็ก/คำขอคืน อัปเดตประวัติชำระ ตรวจลายเซ็นคำตอบ Merchant API ข้อความผลลัพธ์ 5 ภาษา'
        ]
      }
    },
    {
      version: '3.36',
      kind: 'minor',
      date: '2026-08-18',
      items: {
        KO: [
          '업체관리: 채널과 정산주기 사이에 카드 열 추가. 가맹 결제대행사 카드브랜드를 괄호 설명 없이 표시(예: VM)'
        ],
        EN: [
          'Company management: Card column between Channel and settlement cycle. Shows merchant processor card-brand codes without parenthetical notes (e.g. VM)'
        ],
        JP: [
          '加盟店管理: チャネルと精算周期の間にカード列。決済代行のカードブランドを括弧説明なしで表示(例: VM)'
        ],
        CH: [
          '商户管理：在渠道与结算周期之间增加卡列。显示支付服务机构卡品牌代码、不含括号说明（如 VM）'
        ],
        TH: [
          'จัดการร้าน: คอลัมน์บัตรระหว่างช่องทางกับรอบชำระ แสดงรหัสแบรนด์บัตรโดยไม่มีข้อความในวงเล็บ (เช่น VM)'
        ]
      }
    },
    {
      version: '3.35',
      kind: 'minor',
      date: '2026-08-18',
      items: {
        KO: [
          '가맹 결제대행사 카드브랜드(예: V+M)만 결제창 자동인식에 표시. 그 외 카드 입력 시 사용 가능 브랜드 안내 후 결제 차단'
        ],
        EN: [
          'Checkout auto-detect lists only merchant-allowed card brands (e.g. V+M). Other cards show an allowed-brand notice and payment is blocked'
        ],
        JP: [
          '加盟店決済代行のカードブランド(例: V+M)のみ自動認識に表示。それ以外は利用可能ブランドを案内して決済を遮断'
        ],
        CH: [
          '结账自动识别仅显示商户允许的卡品牌（如 V+M）。其他卡会提示可用品牌并阻止支付'
        ],
        TH: [
          'ตรวจจับบัตรอัตโนมัติแสดงเฉพาะแบรนด์ที่ร้านอนุญาต (เช่น V+M) บัตรอื่นแจ้งแบรนด์ที่ใช้ได้และบล็อกการชำระ'
        ]
      }
    },
    {
      version: '3.34',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          'ElementPay: 별도 인증 팝업을 없애고 같은 탭에서만 처리(대기 창이 두 개 열리던 문제). 샌드박스는 3DS 창이 필수가 아님'
        ],
        EN: [
          'ElementPay: no extra auth popup — same tab only (fixes two waiting windows). Sandbox does not always need a 3DS screen'
        ],
        JP: [
          'ElementPay: 別ウィンドウ認証をやめ同一タブのみ（待機画面が二重になる問題）。サンドボックスでは3DS画面は必須ではない'
        ],
        CH: [
          'ElementPay：取消单独认证弹窗，仅同标签处理（避免两个等待窗）。沙箱不一定需要 3DS 画面'
        ],
        TH: [
          'ElementPay: ไม่เปิดป๊อปอัปยืนยัน ใช้แท็บเดียว (แก้หน้าต่างรอสองอัน) แซนด์บ็อกซ์ไม่จำเป็นต้องมีหน้า 3DS เสมอ'
        ]
      }
    },
    {
      version: '3.33',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          'ElementPay 3DS: 결제 클릭 즉시 인증 창을 열고 은행 승인 폼을 넣음(비동기 후 창이 안 뜨던 문제)'
        ],
        EN: [
          'ElementPay 3DS: open the auth window on Pay click, then post the bank form (fixes blank wait after async)'
        ],
        JP: [
          'ElementPay 3DS: 支払いクリック直後に認証窓を開き銀行フォームを送信（非同期後に画面が出ない問題）'
        ],
        CH: [
          'ElementPay 3DS：点击支付即打开认证窗再提交银行表单（修复异步后不弹出）'
        ],
        TH: [
          'ElementPay 3DS: เปิดหน้าต่างยืนยันทันทีที่กด Pay แล้วส่งฟอร์มธนาคาร (แก้รอแล้วไม่ขึ้นหน้าต่าง)'
        ]
      }
    },
    {
      version: '3.32',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          'ElementPay 샌드박스: 운영 API URL이 있어도 api-sbox 고정, 가이드 테스트카드 안내(REJECT BY BANK 방지)'
        ],
        EN: [
          'ElementPay sandbox: keep api-sbox even if live endpoint is stored; show guide test-card hint (avoid REJECT BY BANK)'
        ],
        JP: [
          'ElementPayサンドボックス: 本番API URLがあってもapi-sbox固定。ガイドのテストカード案内'
        ],
        CH: [
          'ElementPay 沙箱：即使登记了生产 API 也固定 api-sbox，并提示指南测试卡'
        ],
        TH: [
          'ElementPay sandbox: บังคับ api-sbox แม้มี URL จริง และแสดงบัตรทดสอบตามคู่มือ'
        ]
      }
    },
    {
      version: '3.31',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '신용카드·PayPay·UnionPay·은행페이 공통: 결제 클릭 즉시 보안창을 열고 결제 주소를 넣음(로그인 화면 방지)'
        ],
        EN: [
          'Card, PayPay, UnionPay, bank pay: open the secure window on click, then load the payment URL (avoids login page)'
        ],
        JP: [
          'カード・PayPay・UnionPay・銀行払い: クリック直後に安全画面を開き決済URLを載せる（ログイン画面防止）'
        ],
        CH: [
          '信用卡、PayPay、银联、银行支付：点击即开安全窗再载入支付地址（避免登录页）'
        ],
        TH: [
          'บัตร, PayPay, UnionPay, ธนาคาร: เปิดหน้าต่างปลอดภัยทันทีที่กด แล้วใส่ URL ชำระ (กันหน้าล็อกอิน)'
        ]
      }
    },
    {
      version: '3.30',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          'PayPay 등 호스티드 결제창이 로그인 화면으로 열리던 문제 수정 — 결제 URL을 서버에서 받아 보안창으로 연다'
        ],
        EN: [
          'Fix hosted checkout opening the login page; payment URL is resolved on the server and opened in a secure window'
        ],
        JP: [
          'ホスト決済画面がログインになる不具合を修正。決済URLをサーバーで取得して開く'
        ],
        CH: [
          '修复托管支付窗打开登录页的问题：由服务器取得支付地址后再打开'
        ],
        TH: [
          'แก้หน้าต่างชำระเงินเปิดเป็นหน้าเข้าสู่ระบบ — รับ URL จากเซิร์ฟเวอร์แล้วเปิดหน้าต่างปลอดภัย'
        ]
      }
    },
    {
      version: '3.29',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '샌드박스 신용카드·PayPay·UnionPay·편의점/은행페이 호스티드 연동, 수단별 다국어(5개) 안내, PayPay·은행은 샌드박스에서 JPY 전송'
        ],
        EN: [
          'Sandbox hosted checkout for card, PayPay, UnionPay, convenience store/bank pay; 5-locale hints; PayPay/bank sent as JPY in sandbox'
        ],
        JP: [
          'サンドボックスでカード・PayPay・UnionPay・コンビニ/銀行払いのホスト決済。5言語案内。PayPay/銀行はJPY送信'
        ],
        CH: [
          '沙箱托管支付：信用卡、PayPay、银联、便利店/银行；五语提示；PayPay/银行按日元发送'
        ],
        TH: [
          'sandbox โฮสต์: บัตร, PayPay, UnionPay, ร้านสะดวกซื้อ/ธนาคาร — คำใบ้ 5 ภาษา, PayPay/ธนาคารส่งเป็น JPY'
        ]
      }
    },
    {
      version: '3.28',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '샌드박스 신용카드 결제: 호스티드 카드창·테스트 카드 안내, 샌드박스 API(api-test) 고정, 사전 리스크 필터 생략'
        ],
        EN: [
          'Sandbox card checkout: hosted card window, test-card hint, api-test endpoint, skip presale risk filter'
        ],
        JP: [
          'サンドボックスカード決済: ホスト型カード画面、テストカード案内、api-test固定、事前リスク省略'
        ],
        CH: [
          '沙箱信用卡：托管填卡窗口、测试卡提示、固定 api-test、跳过预售风控'
        ],
        TH: [
          'บัตรบน sandbox: หน้าต่างกรอกบัตรของโฮสต์, คำใบ้บัตรทดสอบ, api-test, ข้ามตัวกรองความเสี่ยงก่อนขาย'
        ]
      }
    },
    {
      version: '3.27',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '엑심베이 샌드박스 PayPay 결제 테스트: 수단코드 P201, JPY 정수, 상품정보·팝업(ostype) 보강'
        ],
        EN: [
          'Eximbay sandbox PayPay checkout: method P201, whole JPY, product line and popup ostype'
        ],
        JP: [
          'EximbayサンドボックスPayPay決済: 手段P201、JPY整数、商品行・ポップアップ(ostype)'
        ],
        CH: [
          'Eximbay 沙箱 PayPay：手段 P201、日元整数、商品行与弹窗 ostype'
        ],
        TH: [
          'ทดสอบ PayPay บน sandbox Eximbay: รหัส P201, JPY จำนวนเต็ม, สินค้าและป๊อปอัป ostype'
        ]
      }
    },
    {
      version: '3.26',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '본사 결제 라우팅에 엑심베이 결제방식(신용카드·PayPay·편의점/Pay-easy·UnionPay) 추가 — 가맹은 본사설정 따름',
          '신용카드만 켜면 다른 PG와 동일한 카드번호 입력 UI, 신용카드는 등록업체 결제대행사 카드 등록 필요'
        ],
        EN: [
          'HQ payment routing: Eximbay methods (card, PayPay, convenience store/Pay-easy, UnionPay); merchants follow HQ',
          'Card-only uses the same card-entry UI as other PGs; card requires merchant PG binding'
        ],
        JP: [
          '本社決済ルーティングにEximbay決済手段を追加。加盟店は本社設定に従う',
          'クレジットカードのみの場合は他PGと同じカード入力UI。カードは加盟店の決済代行カード登録が必要'
        ],
        CH: [
          '总部支付路由增加 Eximbay 支付方式；商户跟随总部设置',
          '仅信用卡时使用与其他 PG 相同的填卡界面；需在商户绑定支付机构卡片'
        ],
        TH: [
          'เส้นทางชำระ HQ: วิธีชำระ Eximbay — ร้านค้าตาม HQ',
          'ถ้าเปิดเฉพาะบัตร ใช้ UI กรอกบัตรแบบ PG อื่น ต้องผูกบัตร PG ที่ร้าน'
        ]
      }
    },
    {
      version: '3.25',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '결제창 결제수단을 로고 타일로 표시(신용카드 브랜드·PayPay·편의점/Pay-easy·UnionPay), 5개국어 캡션'
        ],
        EN: [
          'Checkout payment methods as logo tiles (card brands, PayPay, convenience store/Pay-easy, UnionPay) with 5-locale captions'
        ],
        JP: [
          '決済手段をロゴタイル表示（カードブランド・PayPay・コンビニ/Pay-easy・UnionPay）、5言語キャプション'
        ],
        CH: [
          '结账支付方式改为 Logo 磁贴（卡品牌、PayPay、便利店/Pay-easy、银联），5语说明'
        ],
        TH: [
          'แสดงวิธีชำระเป็นไทล์โลโก้ (บัตร, PayPay, ร้านสะดวกซื้อ/Pay-easy, UnionPay) คำบรรยาย 5 ภาษา'
        ]
      }
    },
    {
      version: '3.24',
      kind: 'minor',
      date: '2026-08-14',
      items: {
        KO: [
          '해외 결제창: 신용카드·PayPay·일본 편의점·은행·UnionPay 수단 노출 및 5개국어',
          '결제내역 자동환불·강제환불에 해외 PG 전액 취소(cancel) 연동'
        ],
        EN: [
          'Checkout methods: credit card, PayPay, Japan convenience store/bank, UnionPay (5 locales)',
          'Pay-list auto/force refund wired to full cancel API'
        ],
        JP: [
          '決済画面: クレジットカード・PayPay・コンビニ/銀行・UnionPay（5言語）',
          '決済履歴の自動/強制返金を全額取消APIに連携'
        ],
        CH: [
          '结账方式：银行卡、PayPay、日本便利店/银行、银联（5语）',
          '支付明细自动/强制退款对接全额取消 API'
        ],
        TH: [
          'วิธีชำระ: บัตร, PayPay, ร้านสะดวกซื้อ/ธนาคารญี่ปุ่น, UnionPay (5 ภาษา)',
          'เชื่อม AUTO/FORCE_REFUND กับ API ยกเลิกเต็มจำนวน'
        ]
      }
    },
    {
      version: '3.23',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay: 결제내역 자동환불·강제환불에 initRefund 연동(전액)',
          'ElementPay 자동무효(voidPayment)는 2단계 결제 전용이라 미지원 — 승인 건은 환불 사용',
          'getStatus 207(refunded) 시 로컬 환불 상태(42) 동기화'
        ],
        EN: [
          'ElementPay: wire pay-list auto/force refund to initRefund (full amount)',
          'ElementPay auto-void unsupported (voidPayment is two-step only) — use refund after capture',
          'Sync local refund status (42) when getStatus returns 207'
        ],
        JP: [
          'ElementPay: 決済履歴の自動/強制返金を initRefund に連携（全額）',
          'ElementPay 自動無効は非対応（voidPayment は2段階専用）— 承認後は返金を使用',
          'getStatus 207 時にローカル返金状態(42)を同期'
        ],
        CH: [
          'ElementPay：支付明细自动/强制退款对接 initRefund（全额）',
          'ElementPay 不支持自动作废（voidPayment 仅两阶段）— 已批准请用退款',
          'getStatus 207 时同步本地退款状态(42)'
        ],
        TH: [
          'ElementPay: เชื่อม AUTO/FORCE_REFUND กับ initRefund (เต็มจำนวน)',
          'ElementPay ไม่รองรับ AUTO_VOID (voidPayment สำหรับ 2 ขั้น) — ใช้คืนเงินหลังอนุมัติ',
          'ซิงก์สถานะคืนเงินท้องถิ่น (42) เมื่อ getStatus เป็น 207'
        ]
      }
    },
    {
      version: '3.22',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'URL 결제 결과 화면(성공/거절)에 「다시 결제하기」「닫기」 추가 — API(merchant_api) 진입에는 미표시',
          'ElementPay·JPAY 공통: 가맹 API는 returnUrl 복귀만, URL 결제는 결제 초기 화면으로 복귀 가능'
        ],
        EN: [
          'Add Pay again / Close on URL checkout result (success/fail) — hidden for merchant_api entry',
          'ElementPay & JPAY: API uses returnUrl only; URL pay can return to fresh checkout'
        ],
        JP: [
          'URL決済の結果画面に「もう一度支払う」「閉じる」を追加 — merchant_api では非表示',
          'ElementPay・JPAY共通: APIはreturnUrlのみ、URL決済は初期画面へ復帰可能'
        ],
        CH: [
          'URL 结账结果页增加「再次支付」「关闭」— merchant_api 入口不显示',
          'ElementPay / JPAY：API 仅 returnUrl；URL 支付可回到结账首页'
        ],
        TH: [
          'เพิ่มปุ่มชำระอีกครั้ง/ปิดในหน้าผล URL — ไม่แสดงเมื่อเข้า merchant_api',
          'ElementPay และ JPAY: API ใช้ returnUrl เท่านั้น URL กลับหน้าชำระใหม่ได้'
        ]
      }
    },
    {
      version: '3.21',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay 결제 결과/대기 복귀 시에도 총판 상단 로고·부제 적용(ICOPAY 텍스트 폴백만 보이던 문제 수정)'
        ],
        EN: [
          'Apply distributor header logo/subtitle on ElementPay result/waiting return (fix ICOPAY text-only fallback)'
        ],
        JP: [
          'ElementPay結果/待機復帰時も総代理店ヘッダーロゴ・字幕を適用（ICOPAYテキストのみ表示を修正）'
        ],
        CH: [
          'ElementPay 结果/等待返回时也应用总代顶部 Logo/副标题（修复仅显示 ICOPAY 文本）'
        ],
        TH: [
          'ใช้โลโก้/คำบรรยายส่วนหัวเมื่อกลับหน้าผล/รอ ElementPay (แก้กรณีเหลือข้อความ ICOPAY)'
        ]
      }
    },
    {
      version: '3.20',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay INLINE: /k/cards/form·check 를 동일 쿠키 세션으로 처리하고 threeDSCustomerIP 에 구매자 IP 우선 적용',
          '샌드박스 3DS 챌린지 생략·EP 공식 테스트카드 안내 및 은행 거절 메시지 5개국어 보강'
        ],
        EN: [
          'ElementPay INLINE: run /k/cards/form and check in one cookie session; prefer buyer IP for threeDSCustomerIP',
          'Clarify sandbox 3DS skip + EP official test-card guidance; localize bank-reject messages (5 locales)'
        ],
        JP: [
          'ElementPay INLINE: /k/cards/form・check を同一Cookieセッションで実行し、threeDSCustomerIP は購入者IP優先',
          'サンドボックス3DS省略とEP公式テストカード案内、銀行拒否メッセージを5言語で補強'
        ],
        CH: [
          'ElementPay INLINE：/k/cards/form 与 check 共用 Cookie 会话，threeDSCustomerIP 优先使用买家 IP',
          '明确沙盒可跳过 3DS，并补充 EP 官方测试卡说明与银行拒付文案（5 语）'
        ],
        TH: [
          'ElementPay INLINE: เรียก /k/cards/form และ check ในคุกกี้เซสชันเดียวกัน และใช้ IP ผู้ซื้อกับ threeDSCustomerIP เป็นหลัก',
          'ชี้แจงการข้าม 3DS ในแซนด์บ็อกซ์ + บัตรทดสอบ EP อย่างเป็นทางการ และแปลข้อความธนาคารปฏิเสธ 5 ภาษา'
        ]
      }
    },
    {
      version: '3.19',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay 실패 시 EP status_message 표시(예: rejected by bank) — 웹훅 오류와 은행 거절 구분',
          'getStatus 최종 거절/승인 시 로컬 대기 거래를 동기화'
        ],
        EN: [
          'Show ElementPay status_message on failure (e.g. rejected by bank) to separate bank decline from webhook errors',
          'Sync local pending txn when getStatus is final reject/approve'
        ],
        JP: [
          'ElementPay失敗時にstatus_message表示（例: rejected by bank）— 銀行拒否とwebhook誤りの切り分け',
          'getStatus最終結果でローカル保留取引を同期'
        ],
        CH: [
          'ElementPay 失败时显示 status_message（如 rejected by bank），区分银行拒付与 webhook 错误',
          'getStatus 最终结果时同步本地待处理交易'
        ],
        TH: [
          'แสดง status_message ของ ElementPay เมื่อล้มเหลว (เช่น rejected by bank) แยกปฏิเสธธนาคารกับ webhook',
          'ซิงก์รายการค้างเมื่อ getStatus เป็นผลสุดท้าย'
        ]
      }
    },
    {
      version: '3.18',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay 웹훅 check 수정: Merchant Key 없이도 agency·서명 검증 → 474 거절(결제 실패) 해소',
          'check 270 승인 후 샌드박스 승인·ICOPAY 결과 페이지 복귀 가능'
        ],
        EN: [
          'ElementPay webhook check fix: resolve agency/signature without Merchant Key — stop 474 reject failures',
          'Allow check 270 so sandbox can approve and return to ICOPAY result page'
        ],
        JP: [
          'ElementPay webhook check修正: Merchant Keyなしでagency・署名検証 → 474拒否(決済失敗)を解消',
          'check 270承認でサンドボックス承認・ICOPAY結果画面へ復帰可能'
        ],
        CH: [
          'ElementPay webhook check 修复：无 Merchant Key 也可解析 agency/签名 — 消除 474 拒绝导致失败',
          'check 返回 270 后沙盒可批准并回到 ICOPAY 结果页'
        ],
        TH: [
          'แก้ ElementPay webhook check: ตรวจ agency/ลายเซ็นโดยไม่ต้องมี Merchant Key — เลิก 474 ที่ทำให้จ่ายล้ม',
          'check ตอบ 270 ให้ sandbox อนุมัติแล้วกลับหน้าผล ICOPAY ได้'
        ]
      }
    },
    {
      version: '3.17',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay INLINE: KTC 제출 전 /k/cards/check 호출 — check 누락으로 fail→waiting 되던 버그 수정',
          '샌드박스 테스트카드 승인 경로 복구 · 다국어 유지'
        ],
        EN: [
          'ElementPay INLINE: call /k/cards/check before KTC submit — fix fail→waiting when check was skipped',
          'Restore sandbox test-card auth path · keep i18n'
        ],
        JP: [
          'ElementPay INLINE: KTC送信前に /k/cards/check を呼出 — check省略によるfail→waitingを修正',
          'サンドボックステストカード承認経路を復旧 · 多言語維持'
        ],
        CH: [
          'ElementPay INLINE：KTC 提交前调用 /k/cards/check — 修复跳过 check 导致 fail→waiting',
          '恢复沙盒测试卡授权路径 · 保留多语言'
        ],
        TH: [
          'ElementPay INLINE: เรียก /k/cards/check ก่อนส่ง KTC — แก้ fail→waiting เมื่อข้าม check',
          'กู้เส้นทางอนุมัติบัตรทดสอบ sandbox · คงหลายภาษา'
        ]
      }
    },
    {
      version: '3.16',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay INLINE 수정: /k/cards/form(KTC) 자동 POST — waiting URL을 ACS로 오인하던 버그 제거',
          '샌드박스 테스트카드 승인 경로 복구 · 다국어 유지'
        ],
        EN: [
          'ElementPay INLINE fix: auto-POST /k/cards/form (KTC) — stop treating waiting URL as ACS',
          'Restore sandbox test-card auth path · keep i18n'
        ],
        JP: [
          'ElementPay INLINE修正: /k/cards/form(KTC)自動POST — waiting URLをACSと誤認する不具合を解消',
          'サンドボックステストカード承認経路を復旧 · 多言語維持'
        ],
        CH: [
          'ElementPay INLINE 修复：自动 POST /k/cards/form(KTC) — 不再把 waiting URL 当成 ACS',
          '恢复沙盒测试卡授权路径 · 保留多语言'
        ],
        TH: [
          'แก้ ElementPay INLINE: POST อัตโนมัติ /k/cards/form(KTC) — ไม่ใช้ waiting เป็น ACS',
          'กู้เส้นทางอนุมัติบัตรทดสอบ sandbox · คงหลายภาษา'
        ]
      }
    },
    {
      version: '3.15',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay waiting 복귀: URL의 pid·orderNo로 상태 폴링 · elementpayReturn 파라미터 깨짐 보정',
          '다국어(KOR/ENG/JPN/CHN/THA) 대기/미완료 안내 유지'
        ],
        EN: [
          'ElementPay waiting return: poll status with pid/orderNo from URL · tolerate broken elementpayReturn param',
          'Keep i18n (KOR/ENG/JPN/CHN/THA) waiting/incomplete copy'
        ],
        JP: [
          'ElementPay waiting復帰: URLのpid・orderNoでポーリング · elementpayReturn破損を補正',
          '多言語の待機/未完了案内を維持'
        ],
        CH: [
          'ElementPay waiting 回跳：用 URL 的 pid/orderNo 轮询 · 兼容损坏的 elementpayReturn',
          '保留多语言等待/未完成提示'
        ],
        TH: [
          'ElementPay waiting: โพลด้วย pid/orderNo จาก URL · รองรับ elementpayReturn ที่พัง',
          'คงข้อความรอ/ไม่สำเร็จหลายภาษา'
        ]
      }
    },
    {
      version: '3.14',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay INLINE: waiting 멈춤 해소 — 폴링 강화·미완료 시 명확한 실패 안내(테스트 카드)',
          '다국어(KOR/ENG/JPN/CHN/THA) · 로컬 거래상태 우선 반영'
        ],
        EN: [
          'ElementPay INLINE: fix stuck waiting — stronger poll and clear incomplete message (test cards)',
          'i18n (KOR/ENG/JPN/CHN/THA) · prefer local txn status'
        ],
        JP: [
          'ElementPay INLINE: waiting滞留解消 — ポーリング強化・未完了時の明確案内(テストカード)',
          '多言語 · ローカル取引状態を優先'
        ],
        CH: [
          'ElementPay INLINE：修复 waiting 卡住 — 加强轮询与未完成提示（测试卡）',
          '多语言 · 优先本地交易状态'
        ],
        TH: [
          'ElementPay INLINE: แก้ค้าง waiting — โพลเข้มขึ้นและข้อความไม่สำเร็จชัดเจน (บัตรทดสอบ)',
          'หลายภาษา · ใช้สถานะธุรกรรมในเครื่องก่อน'
        ]
      }
    },
    {
      version: '3.13',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay: JPAY/ChillPay형 INLINE — 카드 1회 입력·BKB 자동제출·Light 이중입력 제거',
          '결제 결과는 ICOPAY checkout 결과 화면(elementpayReturn) · 다국어(KOR/ENG/JPN/CHN/THA)'
        ],
        EN: [
          'ElementPay: JPAY/ChillPay-style INLINE — enter card once, auto BKB submit, no Light double entry',
          'Results on ICOPAY checkout page (elementpayReturn) · i18n (KOR/ENG/JPN/CHN/THA)'
        ],
        JP: [
          'ElementPay: JPAY/ChillPay型INLINE — カード1回入力・BKB自動送信・Light二重入力なし',
          '結果はICOPAY checkout画面(elementpayReturn) · 多言語'
        ],
        CH: [
          'ElementPay：JPAY/ChillPay 式 INLINE — 卡信息只填一次、BKB 自动提交、无 Light 二次输入',
          '结果回到 ICOPAY checkout（elementpayReturn）· 多语言'
        ],
        TH: [
          'ElementPay: แบบ INLINE เหมือน JPAY/ChillPay — กรอกบัตรครั้งเดียว ส่ง BKB อัตโนมัติ ไม่กรอกซ้ำ Light',
          'ผลกลับหน้า ICOPAY checkout (elementpayReturn) · หลายภาษา'
        ]
      }
    },
    {
      version: '3.12',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay URL결제: Light 카테고리/팝업 제거 → INLINE(카드 입력 후 Bangkok Bank 폼·iframe)',
          '결제 결과 폴링 API · 다국어(KOR/ENG/JPN/CHN/THA) 처리 중 안내'
        ],
        EN: [
          'ElementPay URL pay: remove Light category/popup → INLINE (card then Bangkok Bank form/iframe)',
          'Status poll API · i18n (KOR/ENG/JPN/CHN/THA) processing copy'
        ],
        JP: [
          'ElementPay URL決済: Lightカテゴリ/ポップアップ廃止 → INLINE(カード入力後Bangkok Bankフォーム/iframe)',
          '結果ポーリングAPI · 多言語の処理中案内'
        ],
        CH: [
          'ElementPay URL 支付：去掉 Light 分类/弹窗 → INLINE（填卡后 Bangkok Bank 表单/iframe）',
          '结果轮询 API · 多语言处理中提示'
        ],
        TH: [
          'ElementPay URL: เลิก Light หมวด/ป๊อปอัป → INLINE (กรอกบัตรแล้ว Bangkok Bank ฟอร์ม/iframe)',
          'API โพลสถานะ · ข้อความกำลังประมวลผลหลายภาษา'
        ]
      }
    },
    {
      version: '3.11',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay URL결제: initPayment redirectUrl(카드 전용 폼)을 iframe으로 표시 — 은행·지갑 카테고리 미노출',
          '다국어(KOR/ENG/JPN/CHN/THA) 호스티드 URL 누락 안내'
        ],
        EN: [
          'ElementPay URL pay: embed initPayment redirectUrl (card-only form) in iframe — hide bank/wallet categories',
          'i18n (KOR/ENG/JPN/CHN/THA) when hosted URL is missing'
        ],
        JP: [
          'ElementPay URL決済: initPayment redirectUrl(カード専用)をiframe表示 — 銀行・ウォレットカテゴリ非表示',
          'ホストURL欠如時の多言語案内'
        ],
        CH: [
          'ElementPay URL 支付：将 initPayment redirectUrl（仅卡表单）嵌入 iframe — 不显示银行/钱包分类',
          '缺少托管 URL 时的多语言提示'
        ],
        TH: [
          'ElementPay URL: ฝัง redirectUrl จาก initPayment (ฟอร์มบัตรอย่างเดียว) ใน iframe — ไม่โชว์หมวดธนาคาร/วอลเล็ต',
          'ข้อความหลายภาษาเมื่อไม่มี URL โฮสต์'
        ]
      }
    },
    {
      version: '3.10',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay: 기본 카드 alias kCards · disabled 시 getMethods 추천값으로 1회 자동 재시도',
          'Light URL service 경로를 실제 alias에 맞춤 · 다국어 오류 안내'
        ],
        EN: [
          'ElementPay: default card alias kCards; one auto-retry with getMethods suggestion on disabled',
          'Light URL uses actual service alias · i18n error copy'
        ],
        JP: [
          'ElementPay: 既定カードaliasをkCards・disabled時はgetMethods推奨で1回自動再試行',
          'Light URLを実aliasに合わせる · 多言語エラー案内'
        ],
        CH: [
          'ElementPay：默认卡 alias 为 kCards；disabled 时用 getMethods 推荐值自动重试一次',
          'Light URL 使用实际 alias · 多语言错误提示'
        ],
        TH: [
          'ElementPay: alias บัตรเริ่มต้น kCards และ retry อัตโนมัติ 1 ครั้งจาก getMethods เมื่อ disabled',
          'ปรับ Light URL ตาม alias จริง · ข้อความผิดพลาดหลายภาษา'
        ]
      }
    },
    {
      version: '3.9',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'PG사 연동: Route No는 ChillPay만 입력 가능(타 PG 비활성·저장 시 비움)',
          'ElementPay 카드 alias 안내·다국어(KOR/ENG/JPN/CHN/THA) — 조회 추천값 예: kCards'
        ],
        EN: [
          'PG integration: Route No enabled only for ChillPay (disabled/cleared for others)',
          'ElementPay card alias help + i18n (KOR/ENG/JPN/CHN/THA) — suggested e.g. kCards'
        ],
        JP: [
          'PG連携: Route NoはChillPayのみ入力可（他PGは無効・保存時クリア）',
          'ElementPayカードalias案内・多言語 — 推奨例 kCards'
        ],
        CH: [
          'PG 对接：Route No 仅 ChillPay 可填（其他 PG 禁用并在保存时清空）',
          'ElementPay 卡 alias 说明与多语言 — 推荐例 kCards'
        ],
        TH: [
          'เชื่อม PG: Route No ใช้ได้เฉพาะ ChillPay (PG อื่นปิดและล้างตอนบันทึก)',
          'คำอธิบาย alias บัตร ElementPay + หลายภาษา — แนะนำเช่น kCards'
        ]
      }
    },
    {
      version: '3.8',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          '본사 API연동설정: ElementPay 결제수단 조회(getMethods)·cardServiceAlias 입력 UI',
          '「Payment method is disabled」안내 다국어 — Cabinet 카드 활성화·alias 맞춤'
        ],
        EN: [
          'HQ API integration: ElementPay getMethods probe and cardServiceAlias field',
          'i18n for “Payment method is disabled” — enable card in Cabinet and align alias'
        ],
        JP: [
          '本社API連携: ElementPay getMethods照会・cardServiceAlias入力UI',
          '「Payment method is disabled」多言語 — Cabinetでカード有効化・alias合わせ'
        ],
        CH: [
          '总部 API 联动：ElementPay getMethods 查询与 cardServiceAlias 输入',
          '“Payment method is disabled” 多语言 — Cabinet 启用卡并核对 alias'
        ],
        TH: [
          'HQ API: ตรวจ getMethods ของ ElementPay และช่อง cardServiceAlias',
          'หลายภาษาสำหรับ Payment method is disabled — เปิดบัตรใน Cabinet และจับคู่ alias'
        ]
      }
    },
    {
      version: '3.7',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay 서명: Postman/PHP와 동일 — 파라미터 삽입 순서(정렬 제거) + RFC3986 body 일치',
          'Wrong signature 재발 수정 · 샌드박스 initPayment'
        ],
        EN: [
          'ElementPay signature: match Postman/PHP — insertion order (no sort) + RFC3986 body alignment',
          'Fix recurring Wrong signature · sandbox initPayment'
        ],
        JP: [
          'ElementPay署名: Postman/PHPと同様 — 挿入順(ソートなし) + RFC3986 body一致',
          'Wrong signature再発を修正 · サンドボックス initPayment'
        ],
        CH: [
          'ElementPay 签名：与 Postman/PHP 一致 — 插入顺序（不排序）+ RFC3986 body',
          '修复反复 Wrong signature · 沙箱 initPayment'
        ],
        TH: [
          'ลายเซ็น ElementPay: ตาม Postman/PHP — ลำดับใส่ค่า (ไม่เรียง) + body RFC3986',
          'แก้ Wrong signature ซ้ำ · sandbox initPayment'
        ]
      }
    },
    {
      version: '3.6',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay initPayment 서명: Result URL 등 특수문자를 PHP RFC3986과 동일하게 인코딩 — Wrong signature 수정',
          '서명 오류 안내 다국어(KOR/ENG/JPN/CHN/THA)'
        ],
        EN: [
          'ElementPay initPayment signature: encode Result URL params like PHP RFC3986 — fixes Wrong signature',
          'Signature-error messages i18n (KOR/ENG/JPN/CHN/THA)'
        ],
        JP: [
          'ElementPay initPayment署名: Result URL等をPHP RFC3986と同様にエンコード — Wrong signature修正',
          '署名エラー案内の多言語(KOR/ENG/JPN/CHN/THA)'
        ],
        CH: [
          'ElementPay initPayment 签名：Result URL 等按 PHP RFC3986 编码 — 修复 Wrong signature',
          '签名错误提示多语言(KOR/ENG/JPN/CHN/THA)'
        ],
        TH: [
          'ลายเซ็น ElementPay initPayment: เข้ารหัส Result URL แบบ PHP RFC3986 — แก้ Wrong signature',
          'ข้อความผิดพลาดลายเซ็นหลายภาษา (KOR/ENG/JPN/CHN/THA)'
        ]
      }
    },
    {
      version: '3.5',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay URL결제: PromptPay/수단 선택 제거 — 신용카드 INLINE(카드번호·유효기간·CVV) 폼으로 JPAY와 동일 구성',
          '다국어(KOR/ENG/JPN/CHN/THA) · 결제는 CARD 전용'
        ],
        EN: [
          'ElementPay URL checkout: removed PromptPay/method picker — credit-card INLINE form (PAN/expiry/CVV) aligned with JPAY',
          'i18n (KOR/ENG/JPN/CHN/THA) · CARD only'
        ],
        JP: [
          'ElementPay URL決済: PromptPay/手段選択を削除 — クレジットカードINLINE(番号・有効期限・CVV)をJPAYと同構成',
          '多言語(KOR/ENG/JPN/CHN/THA) · CARD専用'
        ],
        CH: [
          'ElementPay URL 支付：移除 PromptPay/方式选择 — 信用卡 INLINE（卡号/有效期/CVV）与 JPAY 同布局',
          '多语言(KOR/ENG/JPN/CHN/THA) · 仅 CARD'
        ],
        TH: [
          'ElementPay URL checkout: เอา PromptPay/ตัวเลือกวิธีชำระออก — ฟอร์มบัตร INLINE (เลขบัตร/หมดอายุ/CVV) แบบ JPAY',
          'หลายภาษา (KOR/ENG/JPN/CHN/THA) · เฉพาะ CARD'
        ]
      }
    },
    {
      version: '3.4',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay URL결제(/checkout): 본사 로고·부제·가맹점명·표시옵션을 JPAY/Eximbay와 동일 셸로 적용',
          '브라우저 언어 초기값·다국어(KOR/ENG/JPN/CHN/THA) 헤더·안내문 정비'
        ],
        EN: [
          'ElementPay URL checkout (/checkout): apply HQ logo, subtitle, merchant name, display options via shared shell (same as JPAY/Eximbay)',
          'Browser language init and i18n (KOR/ENG/JPN/CHN/THA) header/help text'
        ],
        JP: [
          'ElementPay URL決済(/checkout): 本社ロゴ・副題・加盟店名・表示オプションをJPAY/Eximbayと同じシェルで適用',
          'ブラウザ言語初期値・多言語(KOR/ENG/JPN/CHN/THA)ヘッダ・案内を整備'
        ],
        CH: [
          'ElementPay URL 支付(/checkout)：通过与 JPAY/Eximbay 相同的壳层应用总部 Logo、副标题、商户名与显示选项',
          '浏览器语言初始值与多语言(KOR/ENG/JPN/CHN/THA)页头/说明整理'
        ],
        TH: [
          'ElementPay URL checkout (/checkout): ใช้โลโก้/คำบรรยาย/ชื่อร้าน/ตัวเลือกแสดงผลผ่าน shell เดียวกับ JPAY/Eximbay',
          'ภาษาเริ่มต้นเบราว์เซอร์และ i18n (KOR/ENG/JPN/CHN/THA)'
        ]
      }
    },
    {
      version: '3.3',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          '본사 노티 구성: 전산 대상 목록 조회 실패 사유 표시(미설정·404·인증) · 목록은 참고용 안내',
          'Provision API 설정 위치·구동 안내 및 [목록 다시 불러오기] · 다국어'
        ],
        EN: [
          'HQ notify config: show why internal-target list failed (not configured / 404 / auth) · list is optional',
          'Provision API location/how-to and [Reload list] · i18n'
        ],
        JP: [
          '本社ノティ構成: 全算対象一覧失敗理由の表示（未設定・404・認証）・一覧は参考用',
          'Provision API設定場所・動線案内と［一覧再読込］·多言語'
        ],
        CH: [
          '总部通知配置：显示账务目标列表失败原因（未配置/404/认证）· 列表为参考',
          'Provision API 位置与用法说明及［重新加载］· 多语言'
        ],
        TH: [
          'ตั้งค่า NOTI HQ: แสดงเหตุผลโหลดรายการเป้าหมายไม่สำเร็จ (ยังไม่ตั้ง/404/auth) · รายการเป็นอ้างอิง',
          'ตำแหน่ง/วิธีใช้ Provision API และ [โหลดอีกครั้ง] · หลายภาษา'
        ]
      }
    },
    {
      version: '3.2',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          '노티생성 전산 대상: THB(ElementPay) 매핑·자동 제안 추가 (본사설정 JPY/USD/THB)',
          '기준화폐 THB 가맹에 JPY 대상이 잘못 제안되던 문제 수정 · 다국어'
        ],
        EN: [
          'NOTI provision ledger targets: added THB (ElementPay) mapping and auto-suggest (HQ JPY/USD/THB)',
          'Fixed wrong JPY target suggestion for THB merchants · i18n'
        ],
        JP: [
          'ノティ作成の全算対象: THB(ElementPay)マッピング・自動提案を追加（本社設定 JPY/USD/THB）',
          '基準通貨THB加盟にJPY対象が誤提案される問題を修正 · 多言語'
        ],
        CH: [
          '通知创建账务目标：新增 THB（ElementPay）映射与自动建议（总部 JPY/USD/THB）',
          '修复 THB 商户被误建议 JPY 目标 · 多语言'
        ],
        TH: [
          'เป้าหมายบัญชีสร้าง NOTI: เพิ่มแมป THB (ElementPay) และแนะนำอัตโนมัติ (HQ JPY/USD/THB)',
          'แก้การแนะนำเป้าหมาย JPY ผิดสำหรับร้าน THB · หลายภาษา'
        ]
      }
    },
    {
      version: '3.1',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          '운영관리 노티생성: 「JPAY PG 노티 슬롯」→「PG 노티 슬롯」등 PG 중립 명칭(다국어)',
          '노티생성 버튼·Provision API 안내를 JPAY 전용 문구에서 공통 노티생성으로 정리'
        ],
        EN: [
          'Ops NOTI provision: renamed 「JPAY PG NOTI slot」 to 「PG NOTI slot」 and other PG-neutral labels (i18n)',
          'Provision button and API help text no longer JPAY-only'
        ],
        JP: [
          '運用管理ノティ作成: 「JPAY PGノティスロット」→「PGノティスロット」などPG中立表記(多言語)',
          'ノティ作成ボタン・Provision API案内をJPAY専用から共通表記へ'
        ],
        CH: [
          '运营管理通知创建：「JPAY PG NOTI 槽位」改为「PG NOTI 槽位」等中性名称（多语言）',
          '通知创建按钮与 Provision API 说明不再仅限 JPAY'
        ],
        TH: [
          'Ops สร้าง NOTI: เปลี่ยนชื่อ 「JPAY PG NOTI slot」เป็น 「PG NOTI slot」 และข้อความกลาง PG (หลายภาษา)',
          'ปุ่มสร้าง NOTI และคำอธิบาย API ไม่จำกัดแค่ JPAY'
        ]
      }
    },
    {
      version: '3.0',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'URL결제 공개 경로 통일: /checkout/{업체코드} — ElementPay가 ChillPay pay.html에 잘못 열리던 문제 수정',
          '레거시 pay.html?m= · /pay/{코드} → /checkout 리다이렉트 · 웹결제 미사용 시 결제 폼 숨김'
        ],
        EN: [
          'Unified public URL pay path: /checkout/{merchantCode} — ElementPay no longer opens ChillPay pay.html by mistake',
          'Legacy pay.html?m= and /pay/{code} redirect to /checkout; hide form when WEB payment is off'
        ],
        JP: [
          'URL決済公開パス統一: /checkout/{加盟店コード} — ElementPayがChillPay pay.htmlを誤表示する問題を修正',
          'レガシー pay.html?m=・/pay/{コード}は/checkoutへリダイレクト・WEB決済未使用時はフォーム非表示'
        ],
        CH: [
          '统一公开 URL 支付路径：/checkout/{商户代码} — 修复 ElementPay 误开 ChillPay pay.html',
          '旧版 pay.html?m= 与 /pay/{代码} 重定向至 /checkout；WEB 支付关闭时隐藏表单'
        ],
        TH: [
          'รวมพาธ URL pay สาธารณะ: /checkout/{รหัสร้าน} — แก้ ElementPay เปิด pay.html ของ ChillPay ผิด',
          'pay.html?m= และ /pay/{รหัส} เดิม redirect ไป /checkout และซ่อนฟอร์มเมื่อปิด WEB'
        ]
      }
    },
    {
      version: '2.99',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'API연동설정 PG사 연동: 연동용도 복수 선택(노티+URL 등) 지원 — 용도별 엔드포인트 입력',
          '기존 복합(MULTI) 행 편집·저장 시 용도가 1개로 줄어들던 문제 해소'
        ],
        EN: [
          'API integration PG form: multi-select integration scopes (e.g. notify+URL) with per-scope endpoints',
          'Editing legacy MULTI rows no longer collapses scopes to a single purpose'
        ],
        JP: [
          'API連携設定のPG連携: 連携用途の複数選択(ノティ+URL等)と用途別エンドポイント対応',
          '従来の複合(MULTI)行の編集保存で用途が1つに縮む問題を解消'
        ],
        CH: [
          'API 联动 PG 表单：支持多选对接用途（如通知+URL），并按用途填写端点',
          '修复编辑旧版复合(MULTI)行保存后用途被收成单一项的问题'
        ],
        TH: [
          'ฟอร์มเชื่อม PG: เลือกขอบเขตหลายอย่างได้ (เช่น แจ้ง+URL) พร้อม endpoint ต่อขอบเขต',
          'แก้แถว MULTI เดิมที่บันทึกแล้วเหลือขอบเขตเดียว'
        ]
      },
      howTo: {
        KO: [{ title: '연동용도 복수 선택', steps: [
          '배포설정 → API연동설정 → PG사 연동 추가/수정',
          '연동 용도에서 필요한 항목을 체크(예: 노티+URL)',
          '용도별 엔드포인트(선택) 입력 후 저장'
        ]}],
        EN: [{ title: 'Multi-select scopes', steps: [
          'Deployment → API integration → Add/Edit PG linkage',
          'Check needed scopes (e.g. Notify + URL)',
          'Optionally fill per-scope endpoints, then Save'
        ]}],
        JP: [{ title: '連携用途の複数選択', steps: [
          'デプロイ設定→API連携設定→PG連携の追加/修正',
          '必要な用途をチェック(例: ノティ+URL)',
          '用途別エンドポイント(任意)を入力して保存'
        ]}],
        CH: [{ title: '多选对接用途', steps: [
          '部署设置 → API 联动 → 添加/修改 PG 对接',
          '勾选所需用途（如通知+URL）',
          '按需填写各用途端点后保存'
        ]}],
        TH: [{ title: 'เลือกขอบเขตหลายอย่าง', steps: [
          'ตั้งค่า deploy → เชื่อม API → เพิ่ม/แก้ PG',
          'ติ๊กขอบเขตที่ต้องการ (เช่น แจ้ง+URL)',
          'ใส่ endpoint ต่อขอบเขต(ถ้ามี) แล้วบันทึก'
        ]}]
      }
    },
    {
      version: '2.98',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay Result: NOTI /noti/result/elementpay 에 order·compId·merchantId 쿼리 부여 — NOTI 가맹 매칭 안정화',
          'NOTI 매칭 순서(compId → 선택 order조회 → webhook 로그)에 맞춘 ICOPAY 연동'
        ],
        EN: [
          'ElementPay Result: append order·compId·merchantId on NOTI /noti/result/elementpay for stable merchant match',
          'Aligned with NOTI match order (compId → optional order lookup → webhook logs)'
        ],
        JP: [
          'ElementPay Result: NOTI /noti/result/elementpay に order・compId・merchantId を付与 — 加盟マッチ安定化',
          'NOTI照合順(compId→任意order照会→webhookログ)に合わせたICOPAY連携'
        ],
        CH: [
          'ElementPay Result：在 NOTI /noti/result/elementpay 附加 order·compId·merchantId — 稳定匹配商户',
          '对齐 NOTI 匹配顺序（compId → 可选 order 查询 → webhook 日志）'
        ],
        TH: [
          'ElementPay Result: ใส่ order·compId·merchantId ที่ /noti/result/elementpay เพื่อจับคู่ร้านให้เสถียร',
          'สอดคล้องลำดับจับคู่ NOTI (compId → ค้น order ทางเลือก → ล็อก webhook)'
        ]
      },
      howTo: {
        KO: [{ title: 'ElementPay Result 쿼리', steps: [
          'EP 결제 시 _successUrl 예: https://noti.icopay.net/noti/result/elementpay?order=…&compId=…&merchantId=…',
          'Cabinet Webhooks는 /noti/elementpay 고정',
          'NOTI가 compId로 가맹을 찾아 resultUrl로 브라우저 전달'
        ]}],
        EN: [{ title: 'ElementPay Result query', steps: [
          'On pay, _successUrl e.g. https://noti.icopay.net/noti/result/elementpay?order=…&compId=…&merchantId=…',
          'Cabinet Webhooks stay /noti/elementpay',
          'NOTI matches merchant by compId and forwards browser to resultUrl'
        ]}],
        JP: [{ title: 'ElementPay Resultクエリ', steps: [
          '決済時_successUrl例: https://noti.icopay.net/noti/result/elementpay?order=…&compId=…&merchantId=…',
          'Cabinet Webhooksは /noti/elementpay 固定',
          'NOTIがcompIdで加盟を特定しresultUrlへブラウザ転送'
        ]}],
        CH: [{ title: 'ElementPay Result 参数', steps: [
          '支付时 _successUrl 例：https://noti.icopay.net/noti/result/elementpay?order=…&compId=…&merchantId=…',
          'Cabinet Webhooks 仍为 /noti/elementpay',
          'NOTI 按 compId 匹配商户并浏览器转到 resultUrl'
        ]}],
        TH: [{ title: 'พารามิเตอร์ ElementPay Result', steps: [
          'ตอนจ่าย _successUrl เช่น https://noti.icopay.net/noti/result/elementpay?order=…&compId=…&merchantId=…',
          'Cabinet Webhooks คงที่ /noti/elementpay',
          'NOTI จับคู่ร้านด้วย compId แล้วส่งเบราว์เซอร์ไป resultUrl'
        ]}]
      }
    },
    {
      version: '2.97',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          'ElementPay: 결제 시 _successUrl·_rejectUrl·_waitingUrl에 NOTI Result(/noti/result/elementpay) 사용 — 가맹 무수정·본사 PG 전환',
          'ElementPay 노티생성 시 Webhook·Result 고정 URL을 수신통보에 자동 반영',
          '운영 안내·다국어(수신통보·노티생성) 보강'
        ],
        EN: [
          'ElementPay: use NOTI Result(/noti/result/elementpay) for _successUrl·_rejectUrl·_waitingUrl — merchants unchanged when HQ switches PG',
          'ElementPay NOTI provision auto-saves fixed Webhook·Result URLs to inbound notify',
          'Ops copy & 5-locale strings updated'
        ],
        JP: [
          'ElementPay: 決済時_successUrl等にNOTI Result(/noti/result/elementpay)を使用 — 加盟無修正で本社PG切替',
          'ElementPayノティ作成でWebhook・Result固定URLを受信通知へ自動反映',
          '運用案内・5言語を補強'
        ],
        CH: [
          'ElementPay：支付时 _successUrl 等使用 NOTI Result(/noti/result/elementpay) — 商户无需改动即可切换 PG',
          'ElementPay 通知创建自动将固定 Webhook·Result 写入接收通知',
          '运营说明与五语种文案补强'
        ],
        TH: [
          'ElementPay: ใช้ NOTI Result(/noti/result/elementpay) กับ _successUrl ฯลฯ — ร้านไม่ต้องแก้เมื่อ HQ เปลี่ยน PG',
          'สร้าง NOTI ElementPay บันทึก Webhook·Result คงที่ลงรับแจ้งอัตโนมัติ',
          'อัปเดตข้อความ ops และ 5 ภาษา'
        ]
      },
      howTo: {
        KO: [
          {
            title: 'ElementPay 노티·Result',
            steps: [
              'EP Cabinet Webhooks = https://noti.icopay.net/noti/elementpay',
              '운영관리 노티생성에서 PG=ElementPay로 가맹 등록(가맹 callback/result URL 입력)',
              '수신통보에 Webhook·Result(/noti/result/elementpay)가 반영되는지 확인',
              '결제 후 브라우저가 NOTI Result → 가맹 resultUrl 로 이동하는지 확인'
            ]
          }
        ],
        EN: [
          {
            title: 'ElementPay NOTI & Result',
            steps: [
              'EP Cabinet Webhooks = https://noti.icopay.net/noti/elementpay',
              'Ops → NOTI provision with PG=ElementPay (merchant callback/result URLs)',
              'Confirm inbound notify shows Webhook·Result(/noti/result/elementpay)',
              'After pay, browser should hit NOTI Result then merchant resultUrl'
            ]
          }
        ],
        JP: [
          {
            title: 'ElementPay ノティ・Result',
            steps: [
              'EP Cabinet Webhooks = https://noti.icopay.net/noti/elementpay',
              '運用管理ノティ作成で PG=ElementPay（加盟 callback/result）',
              '受信通知に Webhook・Result(/noti/result/elementpay) を確認',
              '決済後ブラウザが NOTI Result → 加盟 resultUrl へ進むことを確認'
            ]
          }
        ],
        CH: [
          {
            title: 'ElementPay 通知与 Result',
            steps: [
              'EP Cabinet Webhooks = https://noti.icopay.net/noti/elementpay',
              '运营管理通知创建选 PG=ElementPay（填写商户 callback/result）',
              '确认接收通知含 Webhook·Result(/noti/result/elementpay)',
              '支付后浏览器经 NOTI Result 再到商户 resultUrl'
            ]
          }
        ],
        TH: [
          {
            title: 'ElementPay NOTI และ Result',
            steps: [
              'EP Cabinet Webhooks = https://noti.icopay.net/noti/elementpay',
              'สร้าง NOTI เลือก PG=ElementPay (กรอก callback/result ของร้าน)',
              'ตรวจรับแจ้งว่ามี Webhook·Result(/noti/result/elementpay)',
              'หลังจ่าย เบราว์เซอร์ไป NOTI Result แล้วไป resultUrl ของร้าน'
            ]
          }
        ]
      }
    },
    {
      version: '2.96',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          '운영관리 노티생성: PG 선택(JPAY / ElementPay) — ElementPay는 pgKind=elementpay로 NOTI EP 목록에 등록·슬롯 미사용',
          '노티생성 이력에 PG 구분 표시(DB pg_kind)'
        ],
        EN: [
          'Ops NOTI provision: PG select (JPAY / ElementPay) — ElementPay registers with pgKind=elementpay (no slot)',
          'Provision log shows PG kind (DB pg_kind)'
        ],
        JP: [
          '運用管理ノティ作成: PG選択(JPAY/ElementPay) — ElementPayはpgKind=elementpayで登録・スロットなし',
          'ノティ作成履歴にPG区分を表示(DB pg_kind)'
        ],
        CH: [
          '运营管理 NOTI 创建：可选 PG（JPAY / ElementPay）— ElementPay 以 pgKind=elementpay 登记且无槽位',
          '创建历史显示 PG 区分（DB pg_kind）'
        ],
        TH: [
          'สร้าง NOTI ฝ่าย ops: เลือก PG (JPAY / ElementPay) — ElementPay ลงทะเบียนด้วย pgKind=elementpay ไม่ใช้สล็อต',
          'ประวัติการสร้างแสดงชนิด PG (DB pg_kind)'
        ]
      }
    },
    {
      version: '2.95',
      kind: 'minor',
      date: '2026-08-13',
      items: {
        KO: [
          '업체정보 「수신통보 URL」로 명칭 통일(구 JPAY 수신통보) — J-Pay·ElementPay 공통 안내',
          'ElementPay: 가맹 NOTI Result URL을 _successUrl·_rejectUrl·_waitingUrl에 사용(가맹 도메인 미노출)'
        ],
        EN: [
          'Merchant “Inbound notify URLs” rename (was JPAY inbound) — shared J-Pay·ElementPay help text',
          'ElementPay: use merchant NOTI Result URL for _successUrl·_rejectUrl·_waitingUrl (no merchant domain to EP)'
        ],
        JP: [
          '加盟店「受信通知URL」に名称統一（旧JPAY受信通知）— J-Pay・ElementPay共通案内',
          'ElementPay: 加盟NOTI Result URLを_successUrl・_rejectUrl・_waitingUrlに使用（加盟ドメインはEPへ非公開）'
        ],
        CH: [
          '商户「接收通知 URL」更名（原 JPAY 接收通知）— J-Pay·ElementPay 共用说明',
          'ElementPay：将商户 NOTI Result URL 用于 _successUrl·_rejectUrl·_waitingUrl（不向 EP 暴露商户域名）'
        ],
        TH: [
          'เปลี่ยนชื่อเป็น「URL รับแจ้ง」(เดิม JPAY) — คำอธิบายร่วม J-Pay·ElementPay',
          'ElementPay: ใช้ NOTI Result URL ของร้านกับ _successUrl·_rejectUrl·_waitingUrl (ไม่เปิดโดเมนร้านให้ EP)'
        ]
      }
    },
    {
      version: '2.94',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '가맹점 리스크 현황·필터링: 방식 선택 화살표 이중 표시 수정(1개만)',
          '가맹점 리스크 현황 본사설정 색톤을 파스텔보라로 복원'
        ],
        EN: [
          'Merchant risk status & filtering: fixed double chevron on Method select (one arrow only)',
          'Restored pastel purple for Follow HQ on merchant risk status'
        ],
        JP: [
          '加盟店リスク現状・フィルタ: 方式セレクトの二重矢印を1つに修正',
          '加盟店リスク現状の本社設定色をパステル紫に復元'
        ],
        CH: [
          '商户风险现状·过滤: 修正方式下拉双重箭头为单个',
          '商户风险现状「总部设置」恢复为淡紫'
        ],
        TH: [
          'สถานะ/กรองความเสี่ยงร้าน: แก้ลูกศรซ้ำในช่องรูปแบบให้เหลืออันเดียว',
          'คืนสีม่วงพาสเทลของโหมดตาม HQ ในสถานะความเสี่ยงร้าน'
        ]
      }
    },
    {
      version: '2.93',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '가맹점 리스크(트리거·사전필터): 업체정보 또는 본사 현황/필터링 표에서 별도설정 시 본사 기본값보다 우선 — 안내·다국어·매뉴얼 보강',
          '우선 적용 로직 단위 테스트 추가(별도설정·미사용이 본사 설정을 덮어씀)'
        ],
        EN: [
          'Merchant risk (trigger & presale): Custom from merchant info or HQ status/filter tables overrides HQ defaults — notices, i18n, and manual clarified',
          'Added unit tests that Custom/Disabled merchant settings override HQ'
        ],
        JP: [
          '加盟店リスク(トリガー・事前フィルタ): 加盟店情報または本社表で別途設定すると本社既定より優先 — 案内・多言語・マニュアル補強',
          '優先適用ロジックの単体テスト追加(別途設定・未使用が本社設定を上書き)'
        ],
        CH: [
          '商户风险(触发·预过滤): 商户信息或总部表单独设置优先于总部默认 — 强化说明、多语言与手册',
          '新增优先应用单元测试(单独设置/未使用覆盖总部)'
        ],
        TH: [
          'ความเสี่ยงร้าน (ทริกเกอร์·พรีฟิลเตอร์): ตั้งค่าแยกจากข้อมูลร้านหรือตาราง HQ มีลำดับเหนือค่าเริ่ม HQ — เสริมคำอธิบาย/i18n/คู่มือ',
          'เพิ่มหน่วยทดสอบลำดับความสำคัญ (โหมดแยก/ไม่ใช้ทับค่า HQ)'
        ]
      }
    },
    {
      version: '2.92',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '본사 리스크: 가맹점 리스크 현황·필터링 열 순서 통일(방식·저장을 앞쪽 배치)',
          '방식 색톤 통일 — 본사설정 파스텔그린·미사용 회색·별도설정 파스텔빨강',
          '가맹점 리스크 필터링에서 적용상태 열 제거(방식 색톤으로 대체)'
        ],
        EN: [
          'HQ Risk: aligned merchant risk status & filtering column order (Mode + Save near the front)',
          'Unified mode colors — Follow HQ pastel green, Disabled grey, Custom pastel red',
          'Removed Applied-status column from merchant risk filtering (mode color conveys it)'
        ],
        JP: [
          '本社リスク: 加盟店リスク現状・フィルタ表の列順を統一(方式・保存を前方へ)',
          '方式の色調統一 — 本社設定パステル緑・未使用グレー・個別設定パステル赤',
          '加盟店リスクフィルタから適用状態列を削除(方式色調で代替)'
        ],
        CH: [
          '总部风险: 统一商户风险现状与过滤表列序(方式·保存前置)',
          '方式色调统一 — 总部设置浅绿、未使用灰、单独设置浅红',
          '商户风险过滤表移除适用状态列(由方式色调表达)'
        ],
        TH: [
          'ความเสี่ยง HQ: จัดลำดับคอลัมน์สถานะ/กรองร้านให้ตรงกัน (โหมด+บันทึกไว้ด้านหน้า)',
          'สีโหมดเดียวกัน — ตาม HQ เขียวพาสเทล / ไม่ใช้ เทา / แยก แดงพาสเทล',
          'ลบคอลัมน์สถานะที่ใช้ในตารางกรอง (ใช้สีโหมดแทน)'
        ]
      }
    },
    {
      version: '2.91',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '업체등록·업체정보: 「리스크관리 트리거」→「리스크 위험관리트리거」명칭 변경',
          '「리스크 사전필터트리거」카드 추가(미사용·본사정책 따름·별도정책, 본사 리스크 필터링과 동일 항목)',
          '본사 수수료·리스크 → 리스크: 가맹점 리스크 필터링 표와 업체정보 사전필터 연동 안내 보강'
        ],
        EN: [
          'Merchant register/info: renamed Risk trigger to Risk danger-management trigger',
          'Added Risk presale-filter trigger card (Disabled / Follow HQ / Custom; same fields as HQ risk filtering)',
          'HQ Fees & risk → Risk: clarified merchant risk-filtering table vs merchant-info prefilter'
        ],
        JP: [
          '加盟店登録・情報: 「リスク管理トリガー」→「リスク危険管理トリガー」に名称変更',
          '「リスク事前フィルタトリガー」カード追加(未使用・本社ポリシーに従う・個別ポリシー)',
          '本社手数料・リスク → リスク: 加盟店フィルタ表と事前フィルタ連携説明を補強'
        ],
        CH: [
          '商户注册/信息: 「风险管理触发」更名为「风险危险管理触发」',
          '新增「风险预过滤触发」卡片(未使用/遵循总部/单独政策)',
          '总部手续费·风险 → 风险: 强化商户过滤表与信息页预过滤联动说明'
        ],
        TH: [
          'ลงทะเบียน/ข้อมูลร้าน: เปลี่ยนชื่อทริกเกอร์ความเสี่ยงเป็นทริกเกอร์บริหารความเสี่ยงอันตราย',
          'เพิ่มการ์ดทริกเกอร์ตัวกรองล่วงหน้า (ไม่ใช้/ตาม HQ/แยก)',
          'HQ ค่าธรรมเนียม·ความเสี่ยง → ความเสี่ยง: เสริมคำอธิบายตารางกรองกับพรีฟิลเตอร์'
        ]
      }
    },
    {
      version: '2.90',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '본사정책 허브(수수료·리스크 등): 상단 탭으로 다른 메뉴에 갔다가 돌아와도 마지막으로 보던 서브탭(예: 리스크)을 유지'
        ],
        EN: [
          'HQ policy hubs (Fees & risk, etc.): returning via the top tab restores the last hub sub-tab (e.g. Risk) instead of resetting to the first'
        ],
        JP: [
          '本社政策ハブ(手数料・リスク等): 上部タブで他メニューへ移動後に戻っても、最後に見ていたサブタブ(例:リスク)を維持'
        ],
        CH: [
          '总部政策枢纽(手续费·风险等): 通过顶部标签切到其他菜单再返回时，保留上次查看的子页签(如风险)，不再回到第一个'
        ],
        TH: [
          'ฮับนโยบาย HQ (ค่าธรรมเนียม·ความเสี่ยง ฯลฯ): กลับจากแท็บบนแล้วยังอยู่ที่ซับแท็บล่าสุด (เช่น ความเสี่ยง) ไม่รีเซ็ตไปแท็บแรก'
        ]
      }
    },
    {
      version: '2.89',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '본사 리스크설정: 가맹점 리스크 필터링 현황 카드 추가(본사설정·미사용·별도설정)',
          '가맹점 리스크 현황·필터링 테이블에서 방식·조건 인라인 저장 지원',
          '가맹점별 사전필터 오버라이드(미사용 시 해당 가맹만 사전필터 OFF) — 트리거 미사용과 분리'
        ],
        EN: [
          'HQ Risk settings: added Merchant risk filtering status card (Follow HQ / Disabled / Custom)',
          'Inline save of mode and conditions on merchant risk trigger and filtering tables',
          'Per-merchant presale filter override (Disabled turns off filters for that merchant only) — separate from trigger Disabled'
        ],
        JP: [
          '本社リスク設定: 加盟店リスクフィルタリング状況カード追加(本社設定・未使用・別途設定)',
          '加盟店リスク状況・フィルタリング表で方式・条件のインライン保存',
          '加盟店別事前フィルタ上書き(未使用で当該加盟のみOFF) — トリガー未使用と分離'
        ],
        CH: [
          '总部风险设置: 新增商户风险过滤现状卡片(总部设置/未使用/单独设置)',
          '商户风险现状与过滤表支持方式与条件行内保存',
          '按商户预过滤覆盖(未使用仅关闭该商户) — 与触发未使用分离'
        ],
        TH: [
          'ตั้งค่าความเสี่ยง HQ: เพิ่มการ์ดสถานะการกรองความเสี่ยงร้านค้า (ตาม HQ / ไม่ใช้ / แยก)',
          'บันทึกรูปแบบและเงื่อนไขแบบอินไลน์ในตารางทริกเกอร์และการกรอง',
          'ทับตัวกรองล่วงหน้าต่อร้าน (ปิดใช้ปิดเฉพาะร้าน) — แยกจากปิดทริกเกอร์'
        ]
      }
    },
    {
      version: '2.88',
      kind: 'minor',
      date: '2026-08-11',
      items: {
        KO: [
          '긴급: JPAY 사후 고위험·PY0124 FAIL 이 「사후 쿨다운 미사용」이면 위험관리(실패 쿨다운·자동 비활성) 집계에서 빠지던 오류 수정 — 위험관리 사용 시 항상 집계',
          '사후 옵션은 운영관리 리스크 현황 기록만 제어하도록 문구·다국어 정리'
        ],
        EN: [
          'Hotfix: JPAY post-sale high-risk/PY0124 FAILs were skipped from risk management (cooldown/auto inactive) when postsale toggles were off — they now always count when risk management is on',
          'Postsale toggles now only control Ops Risk status logging; UI/i18n clarified'
        ],
        JP: [
          '緊急: JPAY事後ハイリスク・PY0124 FAILが「事後クールダウン未使用」だと危険管理(失敗クールダウン・自動非活性)集計から外れていた不具合を修正 — 危険管理ON時は常に集計',
          '事後オプションは運用リスク状況の記録のみ制御するよう文言・多言語を整理'
        ],
        CH: [
          '紧急：修复「事后冷却关闭」时 JPAY 事后高风险/PY0124 失败不计入风险管理（冷却/自动停用）的问题 — 开启风险管理时始终计入',
          '事后选项仅控制运营风险现状记录；文案与多语言已整理'
        ],
        TH: [
          'ด่วน: แก้กรณี JPAY high-risk/PY0124 FAIL ไม่ถูกนับในบริหารความเสี่ยงเมื่อปิดโพสต์เซลล์ — ตอนเปิดบริหารความเสี่ยงจะนับเสมอ',
          'ตัวเลือกโพสต์เซลล์ควบคุมแค่บันทึกสถานะความเสี่ยง ปรับข้อความ/i18n'
        ]
      }
    },
    {
      version: '2.87',
      kind: 'minor',
      date: '2026-08-06',
      items: {
        KO: [
          '긴급: 가맹 API 결제창(/checkout/{업체코드})에서 CSS·JS 상대경로가 깨져 가맹점명·상품명이 URL결제와 달리 노출되던 문제 수정(루트 절대경로로 통일)'
        ],
        EN: [
          'Hotfix: merchant API checkout (/checkout/{merchantCode}) broke relative CSS/JS paths so merchant/product name showed unlike URL pay; assets now use root-absolute paths'
        ],
        JP: [
          '緊急: 加盟店API決済画面(/checkout/{加盟店コード})で相対CSS・JSが壊れ、URL決済と異なり加盟店名・商品名が表示されていた問題を修正（ルート絶対パスに統一）'
        ],
        CH: [
          '紧急：修复商户 API 支付页(/checkout/{商户代码})相对 CSS/JS 路径失效导致商户名/商品名与 URL 支付不一致的问题（统一为站点根绝对路径）'
        ],
        TH: [
          'ด่วน: แก้หน้าชำระ API (/checkout/{รหัสร้าน}) ที่โหลด CSS/JS ผิดพาธ ทำให้ชื่อร้าน/สินค้าโผล่ต่างจาก URL pay — ใช้พาธ absolute จาก root'
        ]
      }
    },
    {
      version: '2.86',
      kind: 'minor',
      date: '2026-08-06',
      items: {
        KO: [
          '긴급: 수수료 OTP 수정 중 발생한 app.js 문법 오류로 메뉴 클릭이 안 되던 문제 수정'
        ],
        EN: [
          'Hotfix: restored menu navigation broken by an app.js syntax error in the commission OTP change'
        ],
        JP: [
          '緊急: 手数料OTP修正時のapp.js構文エラーでメニュークリック不可だった問題を修正'
        ],
        CH: [
          '紧急：修复手续费 OTP 改动中 app.js 语法错误导致菜单无法点击的问题'
        ],
        TH: [
          'ด่วน: แก้เมนูคลิกไม่ได้จาก syntax error ใน app.js ช่วงแก้ OTP ค่าธรรมเนียม'
        ]
      }
    },
    {
      version: '2.85',
      kind: 'minor',
      date: '2026-08-06',
      items: {
        KO: [
          '수수료 저장 Google OTP: 6자리 입력 즉시 적용(확인 버튼 불필요)',
          'OTP 입력 UI 단순화 및 다국어 안내'
        ],
        EN: [
          'Commission save Google OTP: applies as soon as 6 digits are entered (no OK click)',
          'Simplified OTP UI with multilingual hints'
        ],
        JP: [
          '手数料保存Google OTP: 6桁入力ですぐ適用(OK不要)',
          'OTP入力UIを簡素化し多言語案内を追加'
        ],
        CH: [
          '手续费保存 Google OTP：输入满 6 位即应用（无需点确定）',
          '简化 OTP 界面并补充多语言提示'
        ],
        TH: [
          'บันทึกค่าธรรมเนียม Google OTP: กรอกครบ 6 หลักแล้วใช้ทันที (ไม่ต้องกดตกลง)',
          'ปรับ UI OTP ให้เรียบง่ายและเพิ่มคำแนะนำหลายภาษา'
        ]
      }
    },
    {
      version: '2.84',
      kind: 'minor',
      date: '2026-08-06',
      items: {
        KO: [
          '중요: 업체정보 저장 시 본사정책 따름 템플릿이 수수료관리 배분(본사 요율% 등)을 덮어쓰던 문제 수정',
          '가맹 6000000044 본사 요율 1.7% 복구(V240). 수수료 그리드 빈칸→0 강제 저장 방지'
        ],
        EN: [
          'Critical: saving merchant profile no longer overwrites commission-grid distribution with HQ template rates',
          'Restored merchant 6000000044 regional rate 1.7% (V240). Empty commission grid cells no longer force 0 on save'
        ],
        JP: [
          '重要: 加盟店情報保存時に本社ポリシーテンプレートが手数料配分(本社料率%等)を上書きしていた問題を修正',
          '加盟店6000000044の本社料率1.7%を復元(V240)。手数料グリッドの空欄→0強制保存を防止'
        ],
        CH: [
          '重要：保存商户资料时不再用总部模板覆盖手续费管理中的分成（如本部费率%）',
          '已恢复商户 6000000044 本部费率 1.7%（V240）。手续费网格空单元格保存时不再强制为 0'
        ],
        TH: [
          'สำคัญ: บันทึกข้อมูลร้านจะไม่ทับอัตราค่าธรรมเนียมในตารางด้วยเทมเพลต HQ อีกต่อไป',
          'กู้คืนอัตราภูมิภาค 1.7% ของร้าน 6000000044 (V240) และป้องกันการบังคับเป็น 0 เมื่อช่องว่าง'
        ]
      }
    },
    {
      version: '2.83',
      kind: 'minor',
      date: '2026-08-01',
      items: {
        KO: [
          '가맹점 업체정보 JPAY 수신통보 URL: 노티관리에서 생성한 Notify/Callback URL이 비어 보이던 문제 수정',
          '총본사·본사·총판(및 ADMIN) 조회 시 노티생성 이력·DB 값을 상세에 안정적으로 표시'
        ],
        EN: [
          'Merchant profile JPAY notify URLs: fixed empty Notify/Callback after NOTI provision',
          'Root HQ / HQ / master distributor (and ADMIN) now see provisioned URLs reliably on detail'
        ],
        JP: [
          '加盟店のJPAY受信通知URL: ノティ作成後に Notify/Callback が空表示になる問題を修正',
          '総本社・本社・総販(およびADMIN)で作成履歴・DB値を詳細に安定表示'
        ],
        CH: [
          '商户资料 JPAY 接收通知 URL：修复在通知管理创建后 Notify/Callback 显示为空的问题',
          '总总部/总部/总代（及 ADMIN）在详情中稳定显示已生成的 URL'
        ],
        TH: [
          'URL แจ้งเตือน JPAY ในข้อมูลร้าน: แก้กรณี Notify/Callback ว่างหลังสร้าง NOTI',
          'HQ / ตัวแทนหลัก (และ ADMIN) เห็น URL ที่สร้างแล้วในหน้ารายละเอียดอย่างเสถียร'
        ]
      }
    },
    {
      version: '2.82',
      kind: 'minor',
      date: '2026-08-01',
      items: {
        KO: [
          '가맹점 업체정보: 총본사·본사·총판 전용 「운영기록」 카드 추가(첨부파일 아래)',
          '운영기록 저장 시 작성자(로그인ID)가 업체변경이력에 기록'
        ],
        EN: [
          'Merchant profile: Operation record card for root HQ / HQ / master distributor (below attachments)',
          'Saving the record logs the author (login ID) in company change history'
        ],
        JP: [
          '加盟店業者情報: 総本社・本社・総販専用「運営記録」カードを追加(添付ファイル下)',
          '運営記録保存時に作成者(ログインID)が業者変更履歴に記録'
        ],
        CH: [
          '商户资料：总总部/总部/总代专用「运营记录」卡片（附件下方）',
          '保存运营记录时将作者(登录ID)记入商户变更历史'
        ],
        TH: [
          'ข้อมูลร้าน: บัตรบันทึกการดำเนินงานสำหรับ HQ / ตัวแทนหลัก (ใต้ไฟล์แนบ)',
          'เมื่อบันทึก บันทึกผู้เขียน (login ID) ในประวัติการเปลี่ยนบริษัท'
        ]
      }
    },
    {
      version: '2.81',
      kind: 'minor',
      date: '2026-07-27',
      items: {
        KO: [
          '결제내역: 당월 등 소량 조회 시 COUNT(*)·상태바/금액요약 이중 스캔으로 HTTP 504 나던 문제 추가 완화',
          '한 페이지에 다 들어오면 COUNT 생략, 메타는 최대 1회만 읽어 목록이 먼저 뜨도록 개선'
        ],
        EN: [
          'Payment list: further mitigated HTTP 504 on small month searches (COUNT + double meta scan)',
          'Skip COUNT when all rows fit one page; load status/financial meta at most once'
        ],
        JP: [
          '決済一覧: 当月など少数件での COUNT(*)・状態バー/金額要約の二重スキャンによる HTTP 504 を追加緩和',
          '1ページに収まる場合は COUNT 省略、メタは最大1回のみ読み取り'
        ],
        CH: [
          '支付列表：进一步缓解当月等少量查询因 COUNT(*) 与状态栏/金额汇总双重扫描导致的 HTTP 504',
          '一页装下时跳过 COUNT；状态/金额元数据最多只读一次'
        ],
        TH: [
          'รายการชำระ: ลด HTTP 504 เพิ่มเมื่อค้นหาเดือนปัจจุบันจำนวนน้อย (COUNT + สแกนเมตาซ้ำ)',
          'ข้าม COUNT เมื่อข้อมูลพอในหนึ่งหน้า และโหลดเมตาสถานะ/ยอดเงินได้ไม่เกินครั้งเดียว'
        ]
      }
    },
    {
      version: '2.80',
      kind: 'minor',
      date: '2026-07-27',
      items: {
        KO: [
          '운영관리 리스크 현황: 「내용」·필터구분 라벨을 UI 언어(KO/EN/JP/CH/TH)로 표시',
          '비정상 전화·이메일, 구매자 불일치, 속도제한, JPAY 사후 위험 등 모든 필터 코드 다국어',
          '언어 전환·엑셀 다운로드에도 동일 번역 적용'
        ],
        EN: [
          'Ops Risk dashboard: Description and filter labels follow UI language (KO/EN/JP/CH/TH)',
          'All filter codes multilingual (invalid phone/email, buyer mismatch, velocity, JPAY post-sale, …)',
          'Same translations on language switch and Excel export'
        ],
        JP: [
          '運用管理リスク状況: 「内容」・フィルター区分ラベルをUI言語(KO/EN/JP/CH/TH)で表示',
          '異常電話・メール、購入者不一致、速度制限、JPAY事後リスクなど全フィルターコード多言語',
          '言語切替・Excel出力にも同一翻訳を適用'
        ],
        CH: [
          '运营管理风险看板：「内容」与筛选分类按界面语言(KO/EN/JP/CH/TH)显示',
          '异常电话/邮箱、买家不一致、速度限制、JPAY事后风险等全部筛选码多语言',
          '切换语言与Excel导出使用相同译文'
        ],
        TH: [
          'ภาพรวมความเสี่ยง: คอลัมน์เนื้อหาและประเภทตัวกรองตามภาษา UI (KO/EN/JP/CH/TH)',
          'แปลรหัสตัวกรองทั้งหมด (โทร/อีเมลผิดปกติ, ผู้ซื้อไม่ตรง, ความถี่, JPAY หลังขาย ฯลฯ)',
          'ใช้คำแปลเดียวกันเมื่อเปลี่ยนภาษาและส่งออก Excel'
        ]
      }
    },
    {
      version: '2.79',
      kind: 'minor',
      date: '2026-07-27',
      items: {
        KO: [
          '결제내역 당월 등 소량 조회 시 HTTP 504(게이트웨이 시간 초과) 완화',
          '기간 조건을 COALESCE→paid_at/created_at OR 로 바꿔 인덱스 사용, 소량 건은 상태바·금액요약 재스캔 생략',
          'paid_at·created_at 목록용 인덱스(V238) 추가'
        ],
        EN: [
          'Payment list: mitigated HTTP 504 on small month searches',
          'Date filter uses paid_at/created_at OR (index-friendly); reuse loaded rows for status/summary when few hits',
          'Added list indexes on paid_at/created_at (V238)'
        ],
        JP: [
          '決済一覧: 当月など少数件検索での HTTP 504(ゲートウェイタイムアウト)を緩和',
          '期間条件を COALESCE→paid_at/created_at OR に変更してインデックス利用、少数件は状態バー・金額要約の再スキャン省略',
          'paid_at・created_at 一覧用インデックス(V238)追加'
        ],
        CH: [
          '支付列表：缓解当月等少量查询的 HTTP 504（网关超时）',
          '日期条件改为 paid_at/created_at OR 以利用索引；少量命中时复用已加载行做状态栏/金额汇总',
          '新增 paid_at/created_at 列表索引(V238)'
        ],
        TH: [
          'รายการชำระ: ลด HTTP 504 เมื่อค้นหาเดือนปัจจุบันที่จำนวนน้อย',
          'เงื่อนไขวันที่ใช้ paid_at/created_at OR ให้ใช้ดัชนีได้ และข้ามสแกนซ้ำเมื่อจำนวนน้อย',
          'เพิ่มดัชนีรายการ paid_at/created_at (V238)'
        ]
      }
    },
    {
      version: '2.78',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '분할 진행관리: 이벤트일시에서 취소일시/납부일시 라벨 제거(날짜·시각 2줄만)',
          '회차상태=취소, 계약상태=파기 로 구분 표시 · 상태값 다국어(data-pg-ui-t)',
          '회차·총회차·진행률·통화 컬럼 축소, 취소사유 컬럼 확대'
        ],
        EN: [
          'Split Progress: removed Paid at/Cancelled at labels from Event time (date/time two lines only)',
          'Installment status=Cancelled, contract status=Voided; status labels multilingual (data-pg-ui-t)',
          'Narrowed installment/total/progress/currency columns; widened cancel reason'
        ],
        JP: [
          '分割進行管理: イベント日時から取消日時/納付日時ラベルを削除(日付・時刻の2行のみ)',
          '回次状態=取消、契約状態=破棄で区分表示 · 状態の多言語(data-pg-ui-t)',
          '回次・総回次・進捗率・通貨列を縮小、取消理由列を拡大'
        ],
        CH: [
          '分期进度：事件时间去掉已付/取消时间标签（仅日期·时间两行）',
          '期次状态=取消、合同状态=作废 · 状态多语言(data-pg-ui-t)',
          '缩小期次/总期/进度/币种列，加宽取消原因列'
        ],
        TH: [
          'ความคืบหน้าแบ่งจ่าย: เอาป้ายเวลาชำระ/ยกเลิกออกจากเวลากิจกรรม (เหลือวันที่/เวลา 2 บรรทัด)',
          'สถานะงวด=ยกเลิก สถานะสัญญา=ยกเลิกแล้ว · แปลสถานะ (data-pg-ui-t)',
          'ย่อคอลัมน์งวด/ทั้งหมด/ความคืบหน้า/สกุลเงิน กว้างเหตุผลยกเลิก'
        ]
      }
    },
    {
      version: '2.77',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '분할 진행관리: 이벤트일시·취소사유·이메일 컬럼 겹침/깨짐 수정',
          '이벤트일시를 구분(납부/취소)+날짜/시각 2줄로 표시, 시각은 초 단위(소수초 제거)',
          '계약취소는 기존 계약·진행 관리 화면에서 계속 처리(별도 취소관리 메뉴 없음)'
        ],
        EN: [
          'Split Progress: fixed overlapping Event time / Cancel reason / Email columns',
          'Event time shows kind (paid/cancel) + date/time on two lines; seconds only (no fractions)',
          'Contract cancel stays on Contract/Progress screens (no separate cancel menu)'
        ],
        JP: [
          '分割進行管理: イベント日時・取消理由・メール列の重なり/崩れを修正',
          'イベント日時は区分(納付/取消)+日付/時刻の2行表示、秒まで(小数秒なし)',
          '契約取消は従来どおり契約・進行管理画面で処理(別メニューなし)'
        ],
        CH: [
          '分期进度：修复事件时间/取消原因/邮箱列重叠错位',
          '事件时间按类型(已付/取消)+日期/时间两行显示，精确到秒(无小数秒)',
          '合同取消仍在合同/进度管理中处理(无单独取消菜单)'
        ],
        TH: [
          'ความคืบหน้าแบ่งจ่าย: แก้คอลัมน์เวลากิจกรรม/เหตุผลยกเลิก/อีเมลทับกัน',
          'เวลากิจกรรมแสดงประเภท(ชำระ/ยกเลิก)+วันที่/เวลา 2 บรรทัด (ถึงวินาที ไม่มีเศษวินาที)',
          'ยกเลิกสัญญายังทำในหน้าสัญญา/ความคืบหน้า (ไม่มีเมนูยกเลิกแยก)'
        ]
      }
    },
    {
      version: '2.76',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '총본사·본사·총판 운영 메뉴얼 PDF 표지에 문서 버전(V2.76) 표기',
          '분할 진행관리: 이벤트일시(납부/취소)·취소사유 컬럼 추가, VIEW SETTING 기본 표시',
          '이벤트일시는 납부완료 시 납부일시·회차 취소 시 계약취소일시를 표시'
        ],
        EN: [
          'Stamp document version (V2.76) on Super HQ / HQ / Distributor ops manual PDF covers',
          'Split Progress: Event time (paid/cancel) + cancel reason columns; default on in VIEW SETTING',
          'Event time shows payment time when paid, contract cancel time when cancelled'
        ],
        JP: [
          '総本部・本社・総代理 運営マニュアルPDF表紙に文書版(V2.76)を表示',
          '分割進行管理: イベント日時(納付/取消)・取消理由列を追加、VIEW SETTING既定表示',
          'イベント日時は納付時は納付日時、取消時は契約取消日時'
        ],
        CH: [
          '总本部/总部/总代理运营手册 PDF 封面标注文档版本(V2.76)',
          '分期进度：事件时间(已付/取消)+取消原因列，VIEW SETTING 默认显示',
          '事件时间：已付为付款时间，取消为合同取消时间'
        ],
        TH: [
          'ประทับเวอร์ชันเอกสาร (V2.76) บนปก PDF คู่มือ HQ สูงสุด/HQ/ตัวแทน',
          'ความคืบหน้าแบ่งจ่าย: คอลัมน์เวลากิจกรรม(ชำระ/ยกเลิก)+เหตุผลยกเลิก แสดงใน VIEW SETTING โดยค่าเริ่มต้น',
          'เวลากิจกรรม: ชำระแล้ว=เวลาชำระ ยกเลิก=เวลายกเลิกสัญญา'
        ]
      }
    },
    {
      version: '2.75',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '분할관리 계약취소: 계약관리·진행관리 행에 [계약취소] 버튼이 나오지 않던 오류 수정',
          '취소 시 사유(선택) 입력·이중 확인 후 미납 회차 중단·기납부 인정',
          '가맹점 계약취소 기본 부여를 사용(Y)으로 맞춤 — 총본사는 항상 취소 가능'
        ],
        EN: [
          'Split-pay contract cancel: fixed missing [Cancel contract] button on Contract/Progress lists',
          'Optional cancel reason + double confirm; unpaid installments stop; paid amounts kept',
          'Merchant cancel default grant set to ON (Y); HQ can always cancel'
        ],
        JP: [
          '分割契約取消: 契約管理・進行管理の行に[契約取消]が出ない不具合を修正',
          '取消時に理由(任意)入力・二重確認後、未納回次停止・納付済みは認定',
          '加盟の契約取消デフォルト付与を使用(Y)に変更 — 総本社は常に取消可能'
        ],
        CH: [
          '分期合同取消：修复合同/进度列表行未显示[取消合同]按钮',
          '取消时可填原因(可选)并二次确认；未付期次停止、已付保留',
          '商户取消默认授予改为启用(Y) — 总部始终可取消'
        ],
        TH: [
          'ยกเลิกสัญญาแบ่งจ่าย: แก้ปุ่ม[ยกเลิกสัญญา]ไม่ขึ้นในรายการสัญญา/ความคืบหน้า',
          'ใส่เหตุผล(ไม่บังคับ)+ยืนยันสองครั้ง งวดค้างหยุด งวดที่ชำระแล้วยังนับ',
          'สิทธิ์ยกเลิกเริ่มต้นร้านเป็นเปิด(Y) — HQ ยกเลิกได้เสมอ'
        ]
      }
    },
    {
      version: '2.74',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '운영 메뉴얼 갱신: 분할결제 사용자·가맹점 운영·본사/총판 분할·신규가맹 추가 — 상위 직권 저장·DB 즉시 반영 안내',
          '메뉴얼 문서 버전 V2.74 (KO/EN/JA/ZH/TH) PDF·HTML 재생성',
          '가맹은 내 업체정보에서 분할결제 사용여부를 직접 변경할 수 없음을 명시'
        ],
        EN: [
          'Ops manuals updated: split-pay user, merchant ops, HQ/dist split, new-merchant add — HQ force-save persists to DB',
          'Manual doc version V2.74 (KO/EN/JA/ZH/TH) PDF/HTML regenerated',
          'Clarify merchants cannot toggle split-pay enable on My Company Info'
        ],
        JP: [
          '運営マニュアル更新: 分割払いユーザー・加盟運営・本社/総販分割・新規加盟 — 上位の直権保存がDB即反映',
          'マニュアル文書版 V2.74 (KO/EN/JA/ZH/TH) PDF/HTML再生成',
          '加盟は自社情報で分割払い使用可否を変更不可と明記'
        ],
        CH: [
          '运营手册更新：分期用户、商户运营、总部/总代分期、新商户添加 — 上级直权保存立即写入数据库',
          '手册文档版本 V2.74（KO/EN/JA/ZH/TH）重新生成 PDF/HTML',
          '明确商户无法在「我的企业信息」自行更改分期启用'
        ],
        TH: [
          'อัปเดตคู่มือ: ผู้ใช้แบ่งงวด ปฏิบัติการร้าน HQ/ตัวแทน และเพิ่มร้านใหม่ — การบันทึกจากต้นสังกัดลง DB ทันที',
          'เวอร์ชันเอกสารคู่มือ V2.74 (KO/EN/JA/ZH/TH) สร้าง PDF/HTML ใหม่',
          'ระบุว่าร้านเปลี่ยนสถานะเปิดใช้แบ่งงวดเองในข้อมูลร้านไม่ได้'
        ]
      }
    },
    {
      version: '2.73',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '근본 원인: 관리자 정적 JS(icopay.co.kr/site)가 구버전이라 분할결제 저장 수정이 브라우저에 미반영',
          '가맹 URL 분할결제 전용 저장 API(/api/comp/updateMerchantSplitPay) 추가 — 대용량 업체수정과 분리',
          '업체 저장 직후 분할결제 설정을 전용 API로 재저장하고, 관리자 정적 파일을 함께 배포'
        ],
        EN: [
          'Root cause: admin static JS on icopay.co.kr/site was stale so split-pay save fixes never reached the browser',
          'Added dedicated merchant URL split-pay save API (/api/comp/updateMerchantSplitPay), separate from bulk company update',
          'After company save, re-persist split-pay via dedicated API; deploy admin static files with the JAR'
        ],
        JP: [
          '根本原因: 管理画面の静的JS(icopay.co.kr/site)が旧版のため分割払い保存修正がブラウザに未反映',
          '加盟URL分割払い専用保存API(/api/comp/updateMerchantSplitPay)を追加 — 大容量の業者更新と分離',
          '業者保存直後に分割払い設定を専用APIで再保存し、管理静的ファイルも合わせて配布'
        ],
        CH: [
          '根本原因：管理端静态 JS（icopay.co.kr/site）为旧版，分期保存修复未到达浏览器',
          '新增商户 URL 分期专用保存 API（/api/comp/updateMerchantSplitPay），与大型公司更新分离',
          '公司保存后通过专用 API 再写入分期设置，并同步部署管理端静态文件'
        ],
        TH: [
          'สาเหตุหลัก: JS คงที่ของแอดมินบน icopay.co.kr/site เป็นเวอร์ชันเก่า จึงไม่ได้รับแพตช์บันทึกแบ่งงวด',
          'เพิ่ม API บันทึกแบ่งงวด URL แยก (/api/comp/updateMerchantSplitPay) จากอัปเดตบริษัทขนาดใหญ่',
          'หลังบันทึกบริษัท จะบันทึกแบ่งงวดซ้ำผ่าน API เฉพาะ และ배포ไฟล์ static ของแอดมินพร้อมกัน'
        ]
      }
    },
    {
      version: '2.72',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '총본사·상위 조직이 가맹 「URL 분할결제」사용여부·계약취소를 저장해도 DB에 반영되지 않던 오류 수정',
          '분할결제 필드는 카드 숨김 여부와 무관하게 저장 요청에 포함(압축 JSON 백업 포함)',
          '업체정보 조회 시 분할결제 회차 UI를 DB 값과 동기화'
        ],
        EN: [
          'Fixed HQ/parent save of merchant URL split-pay enable and contract-cancel not persisting to DB',
          'Split-pay fields are always included on save regardless of card visibility (with compact JSON backup)',
          'Merchant detail load syncs split-pay installment UI with stored values'
        ],
        JP: [
          '総本社・上位組織が加盟の「URL分割払い」使用可否・契約取消を保存してもDBに反映されない不具合を修正',
          'カード非表示でも分割払い項目を保存リクエストに含める(圧縮JSONバックアップ付き)',
          '加盟詳細表示時に分割払い回数UIをDB値と同期'
        ],
        CH: [
          '修复总总部/上级组织保存商户「URL 分期」启用与合同取消后未写入数据库的问题',
          '分期字段无论卡片是否隐藏均纳入保存请求（含压缩 JSON 备份）',
          '商户详情加载时将分期期数 UI 与数据库值同步'
        ],
        TH: [
          'แก้บั๊ก HQ/องค์กรบนบันทึกเปิดใช้แบ่งงวด URL และยกเลิกสัญญาของร้านแล้วไม่ถูกบันทึกใน DB',
          'ฟิลด์แบ่งงวดถูกรวมในคำขอบันทึกเสมอแม้การ์ดถูกซ่อน (พร้อม JSON สำรอง)',
          'โหลดรายละเอียดร้านให้ UI งวดแบ่งจ่ายตรงกับค่าใน DB'
        ]
      }
    },
    {
      version: '2.71',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '가맹 분할결제 사용 ON 시 분할결제내역·분할관리 메뉴가 본사권한 NONE이어도 표시되도록 보정',
          '가맹점등록 저장 시 URL 분할결제 사용여부·계약취소 필드 누락 방지',
          '가맹 기능 스위치 ON이면 클라이언트·서버 모두 메뉴 권한 최소 DELETE 확보'
        ],
        EN: [
          'When merchant split-pay is ON, Split history/ops menus show even if HQ ACL is NONE',
          'Merchant save always includes URL split-pay enable and contract-cancel fields',
          'Feature switch ON raises menu permission floor to DELETE on client and server'
        ],
        JP: [
          '加盟の分割払いON時、本社権限がNONEでも分割履歴・分割管理メニューを表示',
          '加盟登録保存時にURL分割払いの使用可否・契約取消フィールド欠落を防止',
          '機能スイッチON時はクライアント・サーバ双方でメニュー権限を最低DELETEに確保'
        ],
        CH: [
          '商户分期开启时，即使总部权限为 NONE 也显示分期明细/分期管理菜单',
          '商户登记保存时避免遗漏 URL 分期启用与合同取消字段',
          '功能开关开启时客户端与服务器均将菜单权限提升至至少 DELETE'
        ],
        TH: [
          'เมื่อเปิดแบ่งงวดของร้านค้า เมนูประวัติ/จัดการแบ่งงวดจะแสดงแม้สิทธิ์ HQ เป็น NONE',
          'บันทึกทะเบียนร้านค้าไม่ให้ตกหล่นฟิลด์เปิดใช้แบ่งงวดและยกเลิกสัญญา',
          'เมื่อสวิตช์ฟีเจอร์เปิด ฝั่งไคลเอนต์และเซิร์ฟเวอร์ยกสิทธิ์เมนูอย่างน้อย DELETE'
        ]
      }
    },
    {
      version: '2.70',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '분할 계약취소: 총본사는 항상 취소 가능(권한 플래그 누락 보정), 본사·총판은 결제 라우팅「본사·총판 권한」사용 시',
          '본사정책 위치 안내: 결제·URL → 결제 라우팅 → URL 분할결제 계약취소',
          '가맹 메뉴 게이트: URL/챗봇/분할/구독 결제내역은 가맹점등록 기능 미사용이면 접근권한과 무관하게 숨김'
        ],
        EN: [
          'Split contract cancel: headquarters always can cancel; regional/distributor when routing policy org permission is ON',
          'HQ policy location: Payments & URL → Payment routing → URL split-pay contract cancel',
          'Merchant menu gate: URL/chatbot/split/subscription history hidden if merchant registration feature is OFF (ignores page ACL)'
        ],
        JP: [
          '分割契約取消: 総本社は常に取消可、本社・総販は決済ルーティングの権限ON時',
          '本社ポリシー位置: 決済・URL → 決済ルーティング → URL分割払い契約取消',
          '加盟メニュー制御: URL/チャットボット/分割/定期履歴は加盟登録が未使用なら権限と無関係に非表示'
        ],
        CH: [
          '分期合同取消：总总部始终可取消；总部/总代在支付路由权限开启时可用',
          '总部政策位置：支付与 URL → 支付路由 → URL 分期合同取消',
          '商户菜单门控：URL/聊天机器人/分期/订阅明细在商户登记功能关闭时隐藏（无视页面权限）'
        ],
        TH: [
          'ยกเลิกสัญญาแบ่งจ่าย: HQ สูงสุดยกเลิกได้เสมอ; ภูมิภาค/ตัวแทนเมื่อเปิดสิทธิ์ใน Payment routing',
          'ตำแหน่งนโยบาย HQ: ชำระเงินและ URL → Payment routing → ยกเลิกสัญญาแบ่งจ่าย URL',
          'เกตเมนูร้าน: ประวัติ URL/แชทบอท/แบ่งงวด/สมาชิกซ่อนเมื่อฟีเจอร์ในลงทะเบียนร้านปิด (ไม่สน ACL)'
        ]
      }
    },
    {
      version: '2.69',
      kind: 'minor',
      date: '2026-07-24',
      items: {
        KO: [
          '분할결제 계약취소: 미납 회차 중단·기납부 인정(환불 없음)·결제내역 처리사유 기록',
          '본사정책 URL결제: 가맹 기본 부여·본사·총판 권한 / 가맹점등록 URL 분할결제: 본사설정 따름·사용·미사용',
          '취소 UI: 분할관리 계약관리·진행관리 (총본사 항상, 본사·총판·가맹은 권한 설정)'
        ],
        EN: [
          'Split-pay contract cancel: stop unpaid installments; keep paid as success (no auto-refund); annotate payment outcome reason',
          'HQ URL policy: merchant default grant + HQ/distributor permission; merchant registration FOLLOW_HQ/Y/N',
          'Cancel UI: Split mgmt Contract/Progress (HQ always; regional/dist/merchant by policy)'
        ],
        JP: [
          '分割払い契約取消: 未納停止・納付済みは成功金額として認定(自動返金なし)・決済明細に処理事由を記録',
          '本社URL決済: 加盟デフォルト付与・本社・総販権限 / 加盟登録: 本社設定に従う・使用・未使用',
          '取消UI: 分割管理の契約管理・進行管理（総本社は常時、本社・総販・加盟は権限設定）'
        ],
        CH: [
          '分期合同取消：停止未付期次、已付按成功金额保留（无自动退款）、支付明细写入处理事由',
          '总部 URL 政策：商户默认授予 + 总部/总代权限；商户登记 FOLLOW_HQ/使用/未使用',
          '取消界面：分期管理-合同管理/进度管理（总总部始终可用；总部/总代/商户按权限）'
        ],
        TH: [
          'ยกเลิกสัญญาแบ่งจ่าย: หยุดงวดค้างชำระ ยอดที่ชำระแล้วยังถือสำเร็จ (ไม่คืนเงินอัตโนมัติ) บันทึกเหตุผลในรายการชำระ',
          'นโยบาย HQ URL: สิทธิ์เริ่มต้นร้าน + สิทธิ์ HQ/ตัวแทน; ลงทะเบียนร้าน FOLLOW_HQ/ใช้/ไม่ใช้',
          'UI ยกเลิก: จัดการแบ่งงวด จัดการสัญญา/ความคืบหน้า (HQ สูงสุดใช้ได้เสมอ ตามสิทธิ์)'
        ]
      }
    },
    {
      version: '2.68',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '리스크 트리거 발동 소개 안내: 5개 언어 PDF가 동일(일본어) 파일이던 오류 수정 — KO/EN/JP/CH/TH 정식 문서로 교체',
          '운영 메뉴얼 목록 V배지: 플랫폼 라이브 일괄 표시 → 각 PDF 문서 버전과 동일하게 노출(URL·분할·구독=V2.66 등)'
        ],
        EN: [
          'Risk Trigger Introduction: fixed all five language PDFs being the same (Japanese) file — replaced with proper KO/EN/JP/CH/TH docs',
          'Ops manuals list V badge: no longer one live version for all — each row shows that PDF’s document version (URL/Split/Subscribe=V2.66, etc.)'
        ],
        JP: [
          'リスクトリガー発火案内: 5言語PDFが同一(日本語)だった不具合を修正 — 正式KO/EN/JP/CH/THに差し替え',
          '運営マニュアル一覧のV: ライブ一括表示をやめ、各PDFの文書版を表示(URL・分割・定期=V2.66等)'
        ],
        CH: [
          '风险触发介绍：修复五语 PDF 实为同一日文文件 — 已替换为正式 KO/EN/JP/CH/TH',
          '运营手册列表 V 标记：不再统一显示线上版本 — 与各 PDF 文档版本一致（URL/分期/订阅=V2.66 等）'
        ],
        TH: [
          'แนะนำทริกเกอร์ความเสี่ยง: แก้ PDF 5 ภาษาเป็นไฟล์เดียว (ญี่ปุ่น) — เปลี่ยนเป็นเอกสารจริง KO/EN/JP/CH/TH',
          'ป้าย V ในรายการคู่มือ: ไม่ใช้ไลฟ์เดียวกันทั้งรายการ — แสดงเวอร์ชันใน PDF (URL/แบ่งจ่าย/สมาชิก=V2.66 เป็นต้น)'
        ]
      }
    },
    {
      version: '2.67',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '가맹점 운영 메뉴얼: PDF가 V2.53에 고착되어 라이브와 어긋나던 문제 수정 → 플랫폼 라이브 버전(V2.67) 동기화',
          '챗봇결제 가맹점 메뉴얼과 동일 디자인(커버·권한표·STEP·FAQ)·5개국어 HTML/PDF 재생성'
        ],
        EN: [
          'Merchant Ops manual: fixed PDF stuck at V2.53 (out of sync with live) → now tracks platform live V2.67',
          'Same design as Chatbot Merchant Manual (cover, permission table, STEPs, FAQ) · 5-language HTML/PDF rebuilt'
        ],
        JP: [
          '加盟店運営マニュアル: PDFがV2.53のままライブと不一致だった問題を修正 → ライブV2.67に同期',
          'チャットボット加盟店マニュアルと同一デザイン・5言語HTML/PDF再生成'
        ],
        CH: [
          '商户运营手册：修复 PDF 停在 V2.53 与线上不一致 → 同步平台线上 V2.67',
          '与聊天机器人支付商户手册同一设计 · 五语 HTML/PDF 重建'
        ],
        TH: [
          'คู่มือปฏิบัติการร้าน: แก้ PDF ค้างที่ V2.53 ไม่ตรงไลฟ์ → ซิงก์ไลฟ์ V2.67',
          'ดีไซน์เดียวกับคู่มือร้านแชทบอท · สร้าง HTML/PDF 5 ภาษาใหม่'
        ]
      }
    },
    {
      version: '2.66',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '가맹점 사용자 메뉴얼(URL·분할·구독): 챗봇결제 가맹점 메뉴얼과 동일 디자인 패턴으로 전면 재작성(커버·권한표·STEP·FAQ)',
          '권한 요약 배지·흐름도·안내박스·5개국어 HTML/PDF — 이후 가맹점 사용자 메뉴얼 기본 디자인으로 고정'
        ],
        EN: [
          'Merchant user manuals (URL/Split/Subscription): fully rebuilt to match Chatbot Merchant Manual design (cover, permission table, STEPs, FAQ)',
          'Permission badges, flows, callout boxes · 5-language HTML/PDF — fixed as the default merchant-user manual pattern'
        ],
        JP: [
          '加盟店ユーザーマニュアル(URL・分割・定期): チャットボット決済加盟店マニュアルと同一デザインに全面再作成',
          '権限サマリー・STEP・FAQ・5言語HTML/PDF — 以降の標準デザインとして固定'
        ],
        CH: [
          '商户用户手册（URL/分期/订阅）：按聊天机器人支付商户手册同一设计全面重做（封面、权限表、STEP、FAQ）',
          '权限徽章·流程·提示框 · 五语 HTML/PDF — 固定为后续默认设计'
        ],
        TH: [
          'คู่มือผู้ใช้ร้าน (URL/แบ่งจ่าย/สมาชิก): สร้างใหม่ให้ตรงดีไซน์คู่มือร้านแชทบอท (ปก ตารางสิทธิ์ STEP FAQ)',
          'แบดจ์สิทธิ์ โฟลว์ กล่องคำแนะนำ · HTML/PDF 5 ภาษา — ใช้เป็นแพทเทิร์นมาตรฐานต่อไป'
        ]
      }
    },
    {
      version: '2.65',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '브라우저 탭 파비콘: URL 결제창·운영 매뉴얼(blob) 창이 총본사 브랜드 「파비콘 이미지」와 자동 연동',
          '총본사 파비콘이 없을 때만 URL결제 폼 전용 업로드를 폴백으로 사용 · 안내 문구 5개국어'
        ],
        EN: [
          'Browser tab favicon: URL payment and ops manual (blob) windows auto-link to headquarters brand Favicon',
          'URL-pay form upload is fallback only when HQ favicon is empty · 5-language help copy'
        ],
        JP: [
          'ブラウザタブのファビコン: URL決済・運営マニュアル(blob)画面が総本社ブランドのファビコンと自動連動',
          '総本社ファビコンが無い場合のみURL決済フォーム用アップロードをフォールバック · 案内文5言語'
        ],
        CH: [
          '浏览器标签图标：URL 支付与运营手册（blob）窗口自动连动总公司品牌网站图标',
          '仅当总公司图标为空时使用 URL 支付表单上传作为回退 · 五语说明'
        ],
        TH: [
          'ไอคอนแท็บเบราว์เซอร์: หน้าต่างชำระ URL และคู่มือปฏิบัติการ (blob) เชื่อมกับ Favicon แบรนด์สำนักงานใหญ่โดยอัตโนมัติ',
          'อัปโหลดฟอร์ม URL เป็นทางสำรองเมื่อไม่มีไอคอน HQ · คำอธิบาย 5 ภาษา'
        ]
      }
    },
    {
      version: '2.64',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '가맹점 사용자 메뉴얼(URL·분할·구독): 가맹점 운영 메뉴얼과 동일 표지·폰트·목차·본문 구성으로 재작성',
          '메뉴 경로·절차·주의·체크리스트 등 운영 메뉴얼과 같은 안내 톤·5개국어 PDF/HTML'
        ],
        EN: [
          'Merchant user manuals (URL/Split/Subscription): rebuilt to match Merchant Ops manual cover, fonts, TOC, body layout',
          'Same guidance tone (menu path / steps / notes / checklist) · 5-language PDF/HTML'
        ],
        JP: [
          '加盟店ユーザーマニュアル(URL・分割・定期): 加盟店運営マニュアルと同一の表紙・フォント・目次・本文構成に再作成',
          'メニュー経路・手順・注意・チェックリスト等、同トーンの5言語PDF/HTML'
        ],
        CH: [
          '商户用户手册（URL/分期/订阅）：按商户运营手册同一封面、字体、目录、正文结构重做',
          '菜单路径/步骤/注意/清单等同语气 · 五语 PDF/HTML'
        ],
        TH: [
          'คู่มือผู้ใช้ร้าน (URL/แบ่งจ่าย/สมาชิก): สร้างใหม่ให้ตรงปก ฟอนต์ สารบัญ เนื้อหากับคู่มือปฏิบัติการร้าน',
          'โทนเดียวกัน (เส้นทางเมนู/ขั้นตอน/ข้อควรระวัง/เช็คลิสต์) · PDF/HTML 5 ภาษา'
        ]
      }
    },
    {
      version: '2.63',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '가맹점 사용자 메뉴얼 PDF: 한글·일·중·태 글자가 ??로 깨지던 문제 수정(CJK 폰트 임베딩)',
          'URL결제·분할결제·구독결제 사용자 메뉴얼 5개국어 PDF 재생성'
        ],
        EN: [
          'Merchant user manual PDFs: fixed KO/JA/ZH/TH showing as ?? (proper CJK font embedding)',
          'Regenerated URL / Split / Subscription user manuals in 5 languages'
        ],
        JP: [
          '加盟店ユーザーマニュアルPDF: 韓日中タイ文字が??になる問題を修正(CJKフォント埋め込み)',
          'URL・分割・定期決済ユーザーマニュアルを5言語で再生成'
        ],
        CH: [
          '商户用户手册 PDF：修复韩/日/中/泰文显示为 ??（正确嵌入 CJK 字体）',
          '重新生成 URL/分期/订阅支付用户手册五语 PDF'
        ],
        TH: [
          'PDF คู่มือผู้ใช้ร้าน: แก้ตัวอักษร KO/JA/ZH/TH เป็น ?? (ฝังฟอนต์ CJK)',
          'สร้างใหม่คู่มือผู้ใช้ชำระ URL/แบ่งจ่าย/สมาชิก 5 ภาษา'
        ]
      }
    },
    {
      version: '2.62',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '가맹 API 출시 탭명: ①②③ 숫자 제거(공통설정·가맹 등록·키·문서)',
          '운영 메뉴얼(가맹점용): URL결제·분할결제·구독결제 사용자 메뉴얼 추가(연동 스펙 아님, 직원용 이용 안내)'
        ],
        EN: [
          'Merchant API launch tabs: removed ①②③ prefixes (Common / Register / Keys & docs)',
          'Ops manuals (merchant): added URL / Split / Subscription user manuals (staff how-to, not API specs)'
        ],
        JP: [
          '加盟店API公開タブ: ①②③番号を削除(共通設定・加盟店登録・キー・文書)',
          '運営マニュアル(加盟店向け): URL・分割・定期決済のユーザーマニュアルを追加(連携仕様ではなく利用案内)'
        ],
        CH: [
          '商户 API 发布标签：去掉 ①②③ 编号（通用设置·注册商户·密钥与文档）',
          '运营手册（商户）：新增 URL/分期/订阅支付用户手册（员工使用说明，非对接规格）'
        ],
        TH: [
          'แท็บเปิดใช้ Merchant API: ตัดเลข ①②③ (ตั้งค่าทั่วไป·ลงทะเบียนร้าน·คีย์และเอกสาร)',
          'คู่มือปฏิบัติการ (ร้าน): เพิ่มคู่มือผู้ใช้ชำระ URL/แบ่งจ่าย/สมาชิก (วิธีใช้ ไม่ใช่สเปก API)'
        ]
      }
    },
    {
      version: '2.61',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '노티관리: 가맹·전산·개발 노티 사용 체크 라벨이 테마(흰글자 상속)로 안 보이던 문제 수정',
          '본문(content-inner)은 흰 배경이므로 DARK 등 테마에서도 본문 글자색을 어둡게 고정'
        ],
        EN: [
          'NOTI management: fixed invisible merchant/ledger/dev NOTI checkbox labels under org themes',
          'Force dark body text on white content-inner even when DARK theme inherits white text'
        ],
        JP: [
          'ノティ管理: テーマにより加盟・電算・開発ノティ使用ラベルが見えない問題を修正',
          '白いcontent-innerではDARK等でも本文文字色を暗色に固定'
        ],
        CH: [
          'NOTI 管理：修复主题下商户/账务/开发 NOTI 勾选标签不可见',
          '白色正文区域在 DARK 等主题下强制深色文字'
        ],
        TH: [
          'จัดการ NOTI: แก้ป้ายกาช่องใช้ NOTI ร้าน/บัญชี/dev หายในธีม',
          'บังคับตัวอักษรเข้มบนพื้นขาวแม้ธีม DARK สืบทอดสีขาว'
        ]
      }
    },
    {
      version: '2.60',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '노티관리: SUPERVISOR 전용 접근 복원(조직 권한과 무관, SUPERVISOR·총본사 ADMIN만 사용)',
          '접근·권한·사용자 매트릭스: 권한값별 칸/행 색상(본사권한과 동일 팔레트)',
          'SUPERVISOR 전용 메뉴는 삭제(SUPERVISOR) 표기·보라색으로 구분'
        ],
        EN: [
          'NOTI management: restored SUPERVISOR-only access (org grant alone is not enough)',
          'Access/user matrices: cell/row colors by permission (same palette as HQ rights)',
          'SUPERVISOR-only menus show Delete (SUPERVISOR) with purple styling'
        ],
        JP: [
          'ノティ管理: SUPERVISOR専用アクセスを復元(組織権限だけでは不可)',
          'アクセス・ユーザーマトリクス: 権限値ごとのセル/行色(本社権限と同パレット)',
          'SUPERVISOR専用メニューは削除(SUPERVISOR)表示・紫色で区別'
        ],
        CH: [
          'NOTI 管理：恢复仅 SUPERVISOR 可访问（仅有组织权限不足）',
          '访问/用户矩阵：按权限值着色（与总部权限同色板）',
          'SUPERVISOR 专用菜单显示删除(SUPERVISOR)并用紫色区分'
        ],
        TH: [
          'จัดการ NOTI: คืนสิทธิ์เฉพาะ SUPERVISOR (สิทธิ์องค์กรอย่างเดียวไม่พอ)',
          'เมทริกซ์สิทธิ์/ผู้ใช้: แถบสีตามค่าสิทธิ์ (ชุดสีเดียวกับสิทธิ์ HQ)',
          'เมนูเฉพาะ SUPERVISOR แสดง ลบ(SUPERVISOR) และสีม่วง'
        ]
      }
    },
    {
      version: '2.59',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '노티관리: SUPERVISOR 전용 강제 해제 — 본사 접근·권한(삭제/수정 등)에 따라 총판·본사도 활성화',
          '출시 가이드: 가이드 권한만 있어도 본문·서브패널 표시(V2.58)'
        ],
        EN: [
          'NOTI management: removed SUPERVISOR-only override — follows HQ access rights (e.g. Master Dist DELETE)',
          'Launch Guide: parent permission opens guide body and sub-panels (V2.58)'
        ],
        JP: [
          'ノティ管理: SUPERVISOR専用強制を解除 — 本社アクセス権限に従い総代理店等も有効化',
          '公開ガイド: ガイド権限のみでも本文・サブパネル表示(V2.58)'
        ],
        CH: [
          'NOTI 管理：取消仅 SUPERVISOR 可用的强制限制 — 按总部访问权限对总代理等生效',
          '发布指南：仅有指南权限也可打开正文与子面板（V2.58）'
        ],
        TH: [
          'จัดการ NOTI: ยกเลิกบังคับเฉพาะ SUPERVISOR — ใช้ตามสิทธิ์ HQ (เช่น ตัวแทนใหญ่ DELETE)',
          'คู่มือเปิดตัว: มีสิทธิ์คู่มืออย่างเดียวก็เปิดเนื้อหา/แผงย่อยได้ (V2.58)'
        ]
      }
    },
    {
      version: '2.58',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '출시 가이드 권한만 부여해도 가이드 본문·내부 서브패널이 열리도록 수정(빈 “탭 없음” 메시지 해소)'
        ],
        EN: [
          'Launch Guide permission alone opens guide body and inner sub-panels (fixes empty “no tabs” message)'
        ],
        JP: [
          '公開ガイド権限のみでもガイド本文・内部サブパネルが開くよう修正(空の「タブなし」表示を解消)'
        ],
        CH: [
          '仅授予发布指南权限即可打开指南正文与内部子面板（修复空白“无标签”提示）'
        ],
        TH: [
          'ให้สิทธิ์คู่มือเปิดตัวอย่างเดียวก็เปิดเนื้อหา/แผงย่อยได้ (แก้ข้อความว่าง “ไม่มีแท็บ”)'
        ]
      }
    },
    {
      version: '2.57',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '접근·권한: 허브(가맹 API 출시 등) 탭을 조직별 권한에 맞게만 노출·전환',
          '형제 메뉴 URL끼리 권한이 섞이던 별칭 로직 제거(접근불가 누수 수정)',
          '출시 가이드 내부 서브패널(체크리스트·진행안 등)도 동일하게 권한 필터'
        ],
        EN: [
          'Access rights: hub tabs (Merchant API release, etc.) show/switch only by org page permission',
          'Removed sibling-menu permission alias bleed (NONE no longer inherits DELETE)',
          'Launch guide sub-panels (checklist, plans, etc.) filtered by the same rights'
        ],
        JP: [
          'アクセス権限: ハブ(加盟API公開など)のタブを組織権限どおりのみ表示・切替',
          '兄弟メニュー間の権限エイリアス混入を除去(アクセス不可の漏れ修正)',
          '公開ガイド内サブパネル(チェックリスト等)も同一権限でフィルタ'
        ],
        CH: [
          '访问权限：枢纽标签（商户 API 发布等）仅按组织权限显示/切换',
          '移除兄弟菜单权限别名串扰（不可访问不再继承删除权限）',
          '发布指南内子面板（检查清单等）同样按权限过滤'
        ],
        TH: [
          'สิทธิ์เข้าถึง: แท็บฮับ (เปิดตัว API ร้านค้า ฯลฯ) แสดง/สลับตามสิทธิ์องค์กรเท่านั้น',
          'ลบการปนสิทธิ์ระหว่างเมนูพี่น้อง (NONE ไม่รับ DELETE อีกต่อไป)',
          'แผงย่อยในคู่มือเปิดตัว (เช็คลิสต์ ฯลฯ) กรองด้วยสิทธิ์เดียวกัน'
        ]
      }
    },
    {
      version: '2.56',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '분할결제 안내 메일 제목: 템플릿 미설정 시 SAMPLE-CONTRACT 대신 실제 계약번호·회차({{contractNo}}·{{installmentNo}}) 사용'
        ],
        EN: [
          'Split-pay reminder subject: use real {{contractNo}}·{{installmentNo}} when phase subject template is empty (no SAMPLE-CONTRACT)'
        ],
        JP: [
          '分割払い案内メール件名: テンプレ未設定時も SAMPLE-CONTRACT ではなく実契約番号・回数を使用'
        ],
        CH: [
          '分期付款提醒邮件标题：未配置模板时使用真实合同号·期数，不再出现 SAMPLE-CONTRACT'
        ],
        TH: [
          'หัวข้ออีเมลแจ้งแบ่งงวด: ใช้เลขสัญญา/งวดจริงเมื่อไม่มีเทมเพลต (ไม่ใช้ SAMPLE-CONTRACT)'
        ]
      }
    },
    {
      version: '2.55',
      kind: 'minor',
      date: '2026-07-23',
      items: {
        KO: [
          '업체등록·업체정보: 「기타」→「영업정보 / 기타」(온라인·오프라인·기타 드롭다운, 온라인/오프라인 상세 필수)',
          '업체전화·이메일 필수 입력',
          '고객 거래명세서 이메일: 가맹점명 아래 「국가번호·전화 / 이메일」 표시'
        ],
        EN: [
          'Company reg/info: “Other” → “Sales info / Other” (Online/Offline/Other; detail required for Online/Offline)',
          'Company phone and email required',
          'Customer receipt email: show dial·phone / email under merchant name'
        ],
        JP: [
          '業者登録・業者情報:「その他」→「営業情報 / その他」(オンライン・オフライン・その他、詳細はオン/オフ必須)',
          '店舗電話・メール必須',
          '顧客取引明細メール: 加盟店名の下に「国番号・電話 / メール」表示'
        ],
        CH: [
          '商户注册/信息：「其他」→「营业信息 / 其他」（线上/线下/其他；线上线下须填明细）',
          '公司电话与邮箱必填',
          '客户交易明细邮件：商户名下方显示「国家区号·电话 / 邮箱」'
        ],
        TH: [
          'ลงทะเบียน/ข้อมูลร้าน: 「อื่นๆ」→「ข้อมูลธุรกิจ / อื่นๆ」 (ออนไลน์/ออฟไลน์/อื่นๆ รายละเอียดบังคับเมื่อออนไลน์/ออฟไลน์)',
          'โทรศัพท์ร้านและอีเมลเป็นค่าบังคับ',
          'อีเมลใบเสร็จลูกค้า: แสดง รหัสประเทศ·โทร / อีเมล ใต้ชื่อร้าน'
        ]
      }
    },
    {
      version: '2.54',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영 메뉴얼 문서언어 선택 순서·표기를 관리자 UI와 동일하게 통일(JP → KR → EN → CH → TH)'
        ],
        EN: [
          'Ops manuals document-language chips match admin UI order/labels (JP → KR → EN → CH → TH)'
        ],
        JP: [
          '運営マニュアルの文書言語選択を管理UIと同じ順・表記に統一(JP → KR → EN → CH → TH)'
        ],
        CH: [
          '运营手册文档语言选择顺序与标签与管理端 UI 一致（JP → KR → EN → CH → TH）'
        ],
        TH: [
          'ลำดับ/ป้ายภาษาเอกสารคู่มือให้ตรงกับ UI แอดมิน (JP → KR → EN → CH → TH)'
        ]
      }
    },
    {
      version: '2.53',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영 메뉴얼: 메뉴명 통일(운영매뉴얼 → 운영 메뉴얼), 목록·브레드크럼·다국어 동기화',
          '모든 매뉴얼 PDF 통일(가맹점 운영 메뉴얼 HTML → PDF, KO/EN/JP/CH/TH)',
          '새 창 헤더 로고: PDF 표지와 동일한 「on the line」 PNG(흰 바탕)만 사용'
        ],
        EN: [
          'Ops manuals: unify menu name (Ops manuals), sync list/breadcrumb/i18n',
          'All manuals as PDF (merchant ops HTML → PDF, KO/EN/JP/CH/TH)',
          'New-window header logo: same PDF-cover “on the line” PNG (white plate)'
        ],
        JP: [
          '運営マニュアル: メニュー名統一、一覧・パンくず・多言語同期',
          '全マニュアルをPDFに統一(加盟店運営HTML→PDF、KO/EN/JP/CH/TH)',
          '新窓ヘッダーロゴ: PDF表紙と同じ「on the line」PNG(白プレート)のみ'
        ],
        CH: [
          '运营手册：菜单名统一、列表/面包屑/多语言同步',
          '全部手册统一为 PDF（商户运营 HTML→PDF，KO/EN/JP/CH/TH）',
          '新窗口页眉 Logo：仅使用与 PDF 封面相同的「on the line」PNG（白底）'
        ],
        TH: [
          'คู่มือปฏิบัติการ: ชื่อเมนูให้ตรงกัน ซิงก์รายการ/breadcrumb/หลายภาษา',
          'คู่มือทั้งหมดเป็น PDF (คู่มือร้าน HTML→PDF, KO/EN/JP/CH/TH)',
          'โลโก้หัวหน้าต่างใหม่: PNG 「on the line」เดียวกับปก PDF (พื้นขาว)'
        ]
      }
    },
    {
      version: '2.52',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영매뉴얼 화면: JS 문법 오류로 「운영매뉴얼을 불러올 수 없습니다」만 나오던 문제 수정(목록·다국어·가맹점 운영 매뉴얼 정상 표시)'
        ],
        EN: [
          'Ops manuals screen: fix JS syntax error that showed only “Could not load ops manuals” (list, i18n, merchant ops manual restored)'
        ],
        JP: [
          '運営マニュアル画面: JS構文エラーで「読み込めません」のみ表示されていた不具合を修正(一覧・多言語・加盟店マニュアル復旧)'
        ],
        CH: [
          '运营手册画面：修复导致仅显示“无法加载运营手册”的 JS 语法错误（列表、多语言、商户手册恢复）'
        ],
        TH: [
          'หน้าคู่มือ: แก้ข้อผิดพลาด syntax JS ที่ทำให้ขึ้นข้อความโหลดไม่ได้เท่านั้น (รายการ หลายภาษา คู่มือร้านค้ากลับมา)'
        ]
      }
    },
    {
      version: '2.51',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '가맹점 운영 매뉴얼 추가(KO/EN/JP/CH/TH): 권한 화면 기준 — 업체정보·결제 URL 복사, 결제내역, 수수료·정산, API·챗봇, 일상 체크리스트',
          '운영매뉴얼 목록에서 가맹점용 HTML 매뉴얼을 새 창으로 열람·인쇄/PDF 저장'
        ],
        EN: [
          'Added Merchant Operations Manual (KO/EN/JP/CH/TH): permission screens — company info & payment URL copy, payment list, fees/settlement, API/chatbot, daily checklist',
          'Open the merchant HTML manual from Ops manuals in a new window (print / Save as PDF)'
        ],
        JP: [
          '加盟店運営マニュアル追加(KO/EN/JP/CH/TH): 権限画面 — 業者情報・決済URLコピー、決済一覧、手数料・精算、API・ボット、日常チェック',
          '運営マニュアル一覧から加盟向けHTMLを新窓で閲覧・印刷/PDF保存'
        ],
        CH: [
          '新增商户运营手册(KO/EN/JP/CH/TH)：按权限画面 — 企业信息与支付 URL 复制、支付列表、手续费/结算、API/机器人、日常清单',
          '运营手册列表中可新窗口打开商户 HTML 手册（打印/另存 PDF）'
        ],
        TH: [
          'เพิ่มคู่มือปฏิบัติการร้านค้า (KO/EN/JP/CH/TH): ตามหน้าจอสิทธิ์ — ข้อมูลบริษัทและคัดลอก URL ชำระ รายการชำระ ค่าธรรมเนียม/ชำระผล API/แชทบอท เช็คลิสต์',
          'เปิดคู่มือ HTML ของร้านจากเมนูคู่มือในหน้าต่างใหม่ (พิมพ์/บันทึก PDF)'
        ]
      }
    },
    {
      version: '2.50',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영매뉴얼 새 창 로고: 사이드바용 작은 아이콘 대신 PDF 표지와 동일한 「on the line」형 전체 브랜드 마크 표시',
          '로고 이미지에 브랜드명이 포함되면 상호명(OTL HQ 등) 중복 표기 제거'
        ],
        EN: [
          'Ops manual window logo: show full PDF-cover brand mark (not the small sidebar icon)',
          'When the logo image includes the brand name, omit duplicate company-name text'
        ],
        JP: [
          '運営マニュアル新窓ロゴ: サイドバー用の小さいアイコンではなくPDF表紙と同じフルブランドマークを表示',
          'ロゴ画像にブランド名が含まれる場合は社名の重複表示をやめる'
        ],
        CH: [
          '运营手册窗口 Logo：改用与 PDF 封面相同的完整品牌标识，而非侧栏小图标',
          'Logo 图已含品牌名时不再重复显示公司名'
        ],
        TH: [
          'โลโก้หน้าต่างคู่มือ: ใช้เครื่องหมายแบรนด์แบบปก PDF ไม่ใช่ไอคอนแถบข้างขนาดเล็ก',
          'ถ้าโลโก้มีชื่อแบรนด์แล้ว จะไม่ซ้ำชื่อบริษัท'
        ]
      }
    },
    {
      version: '2.49',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영매뉴얼: 한 장짜리 HTML 스텁 제거 → 정식 V3 PDF(총본사·본사·총판·리스크·챗봇가맹) 연동',
          '새 창 좌측 상단에 총본사 로고·업체정보 표시(예제 매뉴얼과 동일 레이아웃) · 언어별 PDF'
        ],
        EN: [
          'Ops manuals: replace one-page HTML stubs with formal V3 PDFs (MHQ, HQ, Distributor, Risk, Chatbot merchant)',
          'New window shows HQ logo and company info top-left (same layout as sample manuals) · per-language PDF'
        ],
        JP: [
          '運営マニュアル: 1枚HTMLスタブを廃止し正式V3 PDF(総本部・本社・総代理・リスク・チャットボット加盟)を連携',
          '新窓の左上に総本部ロゴ・会社情報を表示(サンプルと同じレイアウト) · 言語別PDF'
        ],
        CH: [
          '运营手册：移除单页 HTML 草稿，接入正式 V3 PDF（总本部、总部、总代理、风险、聊天机器人商户）',
          '新窗口左上显示总本部 Logo 与公司信息（与示例手册相同）· 按语言 PDF'
        ],
        TH: [
          'คู่มือปฏิบัติการ: เลิกใช้ HTML หน้าเดียว — เชื่อม PDF V3 จริง (MHQ, HQ, ตัวแทน, ความเสี่ยง, chatbot ร้านค้า)',
          'หน้าต่างใหม่แสดงโลโก้/ข้อมูลสำนักงานใหญ่ซ้ายบน (แบบตัวอย่าง) · PDF ตามภาษา'
        ]
      }
    },
    {
      version: '2.48',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영매뉴얼 새 창: 정적 경로 SPA(대시보드) 오인 수정 — API로 HTML 본문 수신 후 Blob으로 표시'
        ],
        EN: [
          'Ops manuals new window: fix SPA/dashboard mistaken load — fetch HTML via API and open as Blob'
        ],
        JP: [
          '運営マニュアル新窓: 静的パスがSPA(ダッシュボード)になる誤りを修正 — APIでHTML取得しBlob表示'
        ],
        CH: [
          '运营手册新窗口：修复静态路径误开 SPA/仪表盘 — 经 API 取 HTML 并以 Blob 打开'
        ],
        TH: [
          'คู่มือหน้าต่างใหม่: แก้กรณีเส้นทางสแตติกเปิด SPA/แดชบอร์ด — ดึง HTML ผ่าน API แล้วเปิดด้วย Blob'
        ]
      }
    },
    {
      version: '2.47',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영매뉴얼: 사이트 내 미리보기 대신 클릭 시 새 창에서 HTML 열기(브라우저 인쇄·PDF 저장)',
          '언어(KO/EN/JP/CH/TH) 선택 후 해당 HTML 문서 오픈 · 조직 단계 노출 필터 유지'
        ],
        EN: [
          'Ops manuals: open HTML in a new window on click (browser print / Save as PDF) instead of in-page preview',
          'Language KO/EN/JP/CH/TH selects the HTML document; org-tier filter unchanged'
        ],
        JP: [
          '運営マニュアル: 画面内プレビューではなくクリックで新しい窓にHTMLを開く(ブラウザ印刷・PDF保存)',
          '言語(KO/EN/JP/CH/TH)選択で該当HTMLを表示 · 組織段階フィルタは維持'
        ],
        CH: [
          '运营手册：点击后在新窗口打开 HTML（浏览器打印/另存 PDF），不再站内预览',
          '按语言(KO/EN/JP/CH/TH)打开对应 HTML；组织层级过滤不变'
        ],
        TH: [
          'คู่มือปฏิบัติการ: คลิกแล้วเปิด HTML ในหน้าต่างใหม่ (พิมพ์/บันทึก PDF จากเบราว์เซอร์) แทนพรีวิวในหน้า',
          'เลือกภาษา KO/EN/JP/CH/TH เพื่อเปิดเอกสาร HTML · กรองระดับองค์กรเหมือนเดิม'
        ]
      }
    },
    {
      version: '2.46',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '운영매뉴얼 메뉴 위치: 본사정책·플랫폼 → 운영관리(통합리포트 아래)로 이동',
          '로그인 조직 단계 이하 매뉴얼만 노출(총본사=전체, 본사·총판=본사/총판·가맹, 지사~가맹=가맹점용) · 5개국어'
        ],
        EN: [
          'Ops manuals menu moved: HQ Policy · Platform → Operations (below Integrated report)',
          'Show manuals for login org tier and below only (Super HQ=all; HQ/Distributor=hqdist+merchant; Branch~Merchant=merchant) · 5 languages'
        ],
        JP: [
          '運営マニュアルメニューを本社政策・プラットフォーム→運用管理(統合レポート下)へ移動',
          'ログイン組織段階以下のマニュアルのみ表示(総本部=全体、本社・総代理=hqdist+加盟、支社〜加盟=加盟向け) · 5言語'
        ],
        CH: [
          '运营手册菜单位置：总部策略·平台 → 运营管理（综合报表下方）',
          '仅显示登录组织层级及以下手册（总本部=全部；总部/总代理=hqdist+商户；支店~商户=商户）· 5 语'
        ],
        TH: [
          'ย้ายเมนูคู่มือปฏิบัติการ: นโยบาย HQ·แพลตฟอร์ม → การปฏิบัติการ (ใต้รายงานรวม)',
          'แสดงเฉพาะคู่มือระดับองค์กรที่ล็อกอินและต่ำกว่า (สำนักงานใหญ่สูงสุด=ทั้งหมด; HQ/ตัวแทน=hqdist+ร้าน; สาขา~ร้าน=ร้าน) · 5 ภาษา'
        ]
      }
    },
    {
      version: '2.45',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '접근·권한(본사 권한·사용자·업체 접근) 메뉴명·순서를 왼쪽 사이드바 허브·탭과 동일하게 맞춤(예: 수수료·리스크 → 결제·URL …)',
          '권한 목록 표시: 「허브 › 탭」 형식, 5개국어(KOR/ENG/JPN/CHN/THA)'
        ],
        EN: [
          'Access & permissions screens: menu names/order match left sidebar hubs & tabs (e.g. Fees & risk → Payments & URL …)',
          'Permission list labels use “Hub › Tab”; 5 languages (KO/EN/JP/CH/TH)'
        ],
        JP: [
          'アクセス・権限画面のメニュー名・順序を左サイドバーのハブ・タブと同一に(例: 手数料・リスク → 決済・URL …)',
          '権限一覧は「ハブ › タブ」表記、5言語対応'
        ],
        CH: [
          '访问与权限画面的菜单名称/顺序与左侧边栏枢纽·页签一致（例：手续费与风险 → 支付与 URL …）',
          '权限列表显示为「枢纽 › 页签」，支持 5 语'
        ],
        TH: [
          'หน้าจอสิทธิ์การเข้าถึง: ชื่อเมนูและลำดับตรงกับฮับ/แท็บแถบซ้าย (เช่น ค่าธรรมเนียมและความเสี่ยง → ชำระเงินและ URL …)',
          'รายการสิทธิ์แสดงแบบ “ฮับ › แท็บ” รองรับ 5 ภาษา'
        ]
      }
    },
    {
      version: '2.44',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '본사정책 → 플랫폼 → 운영매뉴얼 탭 추가(HTML·인쇄/PDF, 총본사 브랜드 연동, 라이브 버전 동기)',
          '역할별 매뉴얼 10종×5개국어: 총본사 3 · 본사/총판 6 · 가맹 1'
        ],
        EN: [
          'HQ Policy → Platform → Ops manuals tab (HTML, print/PDF, Super HQ branding, live version sync)',
          '10 manuals × 5 languages: Super HQ 3 · HQ/Distributor 6 · Merchant 1'
        ],
        JP: [
          '本社政策→プラットフォーム→運営マニュアルタブ追加(HTML・印刷/PDF、総本部ブランド、ライブ版同期)',
          '役割別10種×5言語: 総本部3・本社/総代理6・加盟1'
        ],
        CH: [
          '总部策略→平台→运营手册页签（HTML、打印/PDF、总本部品牌、与线上版本同步）',
          '分角色手册 10 种×5 语：总本部 3 · 总部/总代理 6 · 商户 1'
        ],
        TH: [
          'เพิ่มแท็บคู่มือปฏิบัติการในแพลตฟอร์ม (HTML พิมพ์/PDF แบรนด์สำนักงานใหญ่ สอดคล้องเวอร์ชันสด)',
          'คู่มือ 10 รายการ×5 ภาษา: สำนักงานใหญ่ 3 · HQ/ตัวแทน 6 · ร้านค้า 1'
        ]
      }
    },
    {
      version: '2.43',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '총판용 리스크 트리거 설명안(HTML·PDF) 추가 — 리스크설정·리스크 필터링 기본값 기준 발동 안내 (KO/EN/JP/CH/TH)'
        ],
        EN: [
          'Add Distributor Risk Trigger Guide (HTML/PDF): Risk Settings & Filtering defaults and when triggers fire (KO/EN/JP/CH/TH)'
        ],
        JP: [
          '総代理店向けリスクトリガー説明(HTML・PDF)追加 — リスク設定・フィルタ既定値の発火案内 (KO/EN/JP/CH/TH)'
        ],
        CH: [
          '新增总代理风险触发说明(HTML/PDF) — 基于风险设置与风险过滤默认值的触发说明 (KO/EN/JP/CH/TH)'
        ],
        TH: [
          'เพิ่มคู่มือทริกเกอร์ความเสี่ยงสำหรับตัวแทนจำหน่าย (HTML/PDF) — ค่าเริ่มต้นการตั้งค่า/ตัวกรองและความหมายเมื่อทำงาน (KO/EN/JP/CH/TH)'
        ]
      }
    },
    {
      version: '2.42',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '노티관리 생성 이력: 관리 열 Callback·Result·수정·삭제 버튼 크기 축소'
        ],
        EN: [
          'NOTI provision history: smaller Callback/Result/Edit/Delete action buttons'
        ],
        JP: [
          'ノティ管理の作成履歴: 管理列 Callback・Result・修正・削除ボタンを縮小'
        ],
        CH: [
          'NOTI 管理创建历史：管理列 Callback/Result/修改/删除按钮缩小'
        ],
        TH: [
          'ประวัติสร้าง NOTI: ย่อปุ่ม Callback/Result/แก้ไข/ลบ ในคอลัมน์จัดการ'
        ]
      }
    },
    {
      version: '2.41',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '리스크 필터링: 카드·이메일·IP 속도제한 창/횟수 분리 설정(기본 카드10/3·이메일30/5·IP15/10, 수정 가능)'
        ],
        EN: [
          'Risk filtering: separate card/email/IP velocity window and attempts (defaults 10/3, 30/5, 15/10; editable)'
        ],
        JP: [
          'リスクフィルタ: カード・メール・IP速度制限の窓/回数を分離(既定 カード10/3・メール30/5・IP15/10、変更可)'
        ],
        CH: [
          '风险过滤：卡/邮箱/IP 速度限制窗口与次数分开设置（默认 10/3、30/5、15/10，可改）'
        ],
        TH: [
          'ตัวกรองความเสี่ยง: แยกช่วง/ครั้งของบัตร·อีเมล·IP (ค่าเริ่ม 10/3, 30/5, 15/10 แก้ได้)'
        ]
      }
    },
    {
      version: '2.40',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '리스크 필터링 트리거 운영 매뉴얼(HTML·PDF) 추가 — 항목별 사용 동작·사전필터 검사순서 (KO/EN/JP/CH/TH)'
        ],
        EN: [
          'Add Risk Filtering Trigger Ops Manual (HTML/PDF): per-item Enabled behavior and presale check order (KO/EN/JP/CH/TH)'
        ],
        JP: [
          'リスクフィルタリングトリガー運用マニュアル(HTML・PDF)追加 — 項目別使用動作・事前フィルタ検査順 (KO/EN/JP/CH/TH)'
        ],
        CH: [
          '新增风险过滤触发运营手册(HTML/PDF) — 各项启用行为与预检顺序 (KO/EN/JP/CH/TH)'
        ],
        TH: [
          'เพิ่มคู่มือทริกเกอร์ตัวกรองความเสี่ยง (HTML/PDF) — พฤติกรรมเมื่อเปิดใช้และลำดับตรวจก่อนส่ง (KO/EN/JP/CH/TH)'
        ]
      }
    },
    {
      version: '2.39',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '상단 언어 전환 시 열린 탭 목록이 비는 오류 수정(노티관리 생성이력 등 커스텀 바인드 화면 재조회)',
          '언어 전환·탭 재진입 공통: 1회 바인드 플래그 초기화로 다국어 화면 공백 방지'
        ],
        EN: [
          'Fix empty lists on language switch for open tabs (NOTI provision history and other custom-bound screens reload)',
          'Locale switch and tab revisit: reset one-time bind flags so multilingual screens stay populated'
        ],
        JP: [
          '上部言語切替で開いているタブ一覧が空になる不具合を修正(ノティ管理の作成履歴などカスタムバインド画面を再読込)',
          '言語切替・タブ再入場共通: 1回バインドフラグ初期化で多言語画面の空白を防止'
        ],
        CH: [
          '修复切换顶部语言后已打开标签列表变空的问题（NOTI 管理创建记录等自定义绑定画面会重新加载）',
          '语言切换与标签重进：统一重置一次性绑定标志，避免多语言画面空白'
        ],
        TH: [
          'แก้รายการในแท็บที่เปิดอยู่หายเมื่อสลับภาษา (โหลดประวัติสร้าง NOTI และหน้า bind พิเศษใหม่)',
          'สลับภาษา/เปิดแท็บซ้ำ: รีเซ็ตแฟล็ก bind ครั้งเดียว เพื่อไม่ให้หน้าหลายภาษาว่าง'
        ]
      }
    },
    {
      version: '2.38',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          'JPAY 노티 생성: 연동방식 「URL 하이브리드」 추가 — URL 대체송부(개발) + 가맹점 노티 callback/result URL 송부',
          '노티 생성·이력 수정 UI·안내·오류 문구 다국어(EN/JP/CH/TH)'
        ],
        EN: [
          'JPAY NOTI provision: add URL hybrid mode — URL alt-dev send plus merchant callback/result URLs',
          'NOTI create/edit UI, hints, and errors localized (EN/JP/CH/TH)'
        ],
        JP: [
          'JPAYノティ作成: 連携方式「URLハイブリッド」追加 — URL代替送付(開発)+加盟店callback/result URL送付',
          'ノティ作成・履歴修正UI・案内・エラーの多言語(EN/JP/CH/TH)'
        ],
        CH: [
          'JPAY NOTI 创建：新增对接方式「URL 混合」— URL 替代开发发送 + 商户 callback/result URL',
          'NOTI 创建/编辑界面、提示与错误多语言(EN/JP/CH/TH)'
        ],
        TH: [
          'สร้าง NOTI JPAY: เพิ่มโหมด URL ไฮบริด — ส่งสำรองแบบ URL(dev) + ส่ง callback/result ของร้าน',
          'UI/คำแนะนำ/ข้อผิดพลาดหน้าสร้าง·แก้ไข NOTI รองรับหลายภาษา'
        ]
      }
    },
    {
      version: '2.37',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리 편집 행 색: 결제내역 「성공」파스텔(#d1fae5)과 동일'
        ],
        EN: [
          'Commission editing row uses Payment list Success pastel (#d1fae5)'
        ],
        JP: [
          '手数料管理の編集行色を決済一覧「成功」パステル(#d1fae5)に統一'
        ],
        CH: [
          '手续费管理编辑行颜色与支付列表「成功」淡绿(#d1fae5)一致'
        ],
        TH: [
          'สีแถวแก้ไขค่าธรรมเนียมให้ตรงกับพาสเทล「สำเร็จ」(#d1fae5)'
        ]
      }
    },
    {
      version: '2.36',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리 고정 열폭 조정(No3·업체코드7·관리7·적용시작일7)'
        ],
        EN: [
          'Commission list fixed widths: No. 3, company code 7, manage 7, start date 7'
        ],
        JP: [
          '手数料管理の固定列幅調整(No3・加盟店コード7・管理7・適用開始日7)'
        ],
        CH: [
          '手续费管理固定列宽调整（No.3、商户代码7、管理7、适用开始日7）'
        ],
        TH: [
          'ปรับความกว้างคอลัมน์คงที่ (No.3 / รหัสร้าน7 / จัดการ7 / วันเริ่ม7)'
        ]
      }
    },
    {
      version: '2.35',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리 고정 열폭 조정(체크2·No4·가맹12·코드8·통화4·관리6·적용일8)',
          '수수료관리: 금일 수정 행 하이라이트 제거, 현재 편집 행만 파스텔 연두 표시'
        ],
        EN: [
          'Commission list fixed column widths adjusted (Check2/No4/Merchant12/Code8/Cur4/Manage6/Start8)',
          'Commission list: remove today-changed highlight; editing row only in pastel lime'
        ],
        JP: [
          '手数料管理の固定列幅調整(チェック2・No4・加盟12・コード8・通貨4・管理6・適用日8)',
          '手数料管理: 本日変更ハイライト削除、編集中行のみパステル黄緑'
        ],
        CH: [
          '手续费管理固定列宽调整（勾选2/No4/商户12/代码8/货币4/管理6/开始日8）',
          '手续费管理：取消今日修改高亮，仅当前编辑行为淡绿黄'
        ],
        TH: [
          'ปรับความกว้างคอลัมน์คงที่ค่าธรรมเนียม (เช็ค2/No4/ร้าน12/รหัส8/สกุล4/จัดการ6/วันเริ่ม8)',
          'จัดการค่าธรรมเนียม: เลิกไฮไลต์แก้วันนี้ เหลือเฉพาะแถวที่กำลังแก้เป็นเขียวอ่อน'
        ]
      }
    },
    {
      version: '2.34',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리: 체크·No·가맹점·업체코드·통화·관리·적용시작일 열폭 고정, 나머지·헬로 추가 열은 잔여 폭 균등 분배',
          '수수료관리: 금일 수정·현재 편집 행 하이라이트를 파스텔 회색 톤으로 변경'
        ],
        EN: [
          'Commission list: fixed widths for Check/No/Merchant/Code/Currency/Manage/Start date; leftover shared by other & Hello columns',
          'Commission list: today-changed and editing row highlights use pastel gray'
        ],
        JP: [
          '手数料管理: チェック・No・加盟・コード・通貨・管理・適用開始日は固定幅、残り・ハロー追加列は残余均等',
          '手数料管理: 本日変更・編集中行のハイライトをパステルグレーに変更'
        ],
        CH: [
          '手续费管理：勾选/No/商户/代码/货币/管理/开始日列宽固定，其余及 Hello 追加列均分剩余宽度',
          '手续费管理：今日修改与编辑中行高亮改为淡灰'
        ],
        TH: [
          'จัดการค่าธรรมเนียม: ตรึงความกว้าง เช็ค/No/ร้าน/รหัส/สกุล/จัดการ/วันเริ่ม ส่วนอื่นและคอลัมน์ Hello แบ่งที่เหลือ',
          'จัดการค่าธรรมเนียม: ไฮไลต์แถวแก้วันนี้/กำลังแก้เป็นเทาพาสเทล'
        ]
      }
    },
    {
      version: '2.33',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리: 셀 클릭 수정 시 열 너비 고정(과확장 방지)',
          '수수료관리: 금일 수정 행 파스텔 녹색 · 현재 편집 행 진한 파스텔 연두 표시'
        ],
        EN: [
          'Commission list: keep column width when editing a cell (no expand)',
          'Commission list: pastel green for rows changed today; darker pastel lime for the row being edited'
        ],
        JP: [
          '手数料管理: セル編集時に列幅を固定(過拡張防止)',
          '手数料管理: 本日変更行はパステル緑・編集中行は濃いパステル黄緑'
        ],
        CH: [
          '手续费管理：点击单元格编辑时固定列宽（防撑开）',
          '手续费管理：今日修改行为淡绿；正在编辑行为较深的淡黄绿'
        ],
        TH: [
          'จัดการค่าธรรมเนียม: ล็อกความกว้างคอลัมน์ตอนแก้ไขเซลล์ (ไม่ขยาย)',
          'จัดการค่าธรรมเนียม: แถวที่แก้วันนี้เขียวพาสเทล · แถวที่กำลังแก้เขียวอ่อนเข้มกว่า'
        ]
      }
    },
    {
      version: '2.32',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리 합계(요율%·건당료) 숫자를 빨간색으로 강조 표시'
        ],
        EN: [
          'Commission list total amounts (rate % / per-txn) highlighted in red'
        ],
        JP: [
          '手数料管理の合計(料率%・件当)を赤色で強調表示'
        ],
        CH: [
          '手续费管理合计（费率%/按笔）以红色突出显示'
        ],
        TH: [
          'เน้นยอดรวมค่าธรรมเนียม (%/ต่อรายการ) ด้วยสีแดง'
        ]
      }
    },
    {
      version: '2.31',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리 합계(요율%·건당료) 읽기 전용 — 상위 조직 합산 표시만, 클릭 수정 불가'
        ],
        EN: [
          'Commission list totals (rate % / per-txn) read-only — display sum only, no click-to-edit'
        ],
        JP: [
          '手数料管理の合計(料率%・件当)は読取専用 — 上位組織合算表示のみ、クリック編集不可'
        ],
        CH: [
          '手续费管理合计（费率%/按笔）只读 — 仅显示上级汇总，不可点击修改'
        ],
        TH: [
          'ยอดรวมค่าธรรมเนียม (%/ต่อรายการ) อ่านอย่างเดียว — แสดงผลรวมเท่านั้น แก้ด้วยคลิกไม่ได้'
        ]
      }
    },
    {
      version: '2.30',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리 저장 OTP: 1회 인증 후 10분 유지·수수료 관련 작업 시 슬라이딩 연장(무활동 시 재요청)'
        ],
        EN: [
          'Commission save OTP: valid 10 minutes after one verify; sliding extend on fee-related activity; re-prompt after idle'
        ],
        JP: [
          '手数料管理保存OTP: 1回認証後10分維持・関連操作でスライディング延長(無操作時は再要求)'
        ],
        CH: [
          '手续费保存 OTP：验证一次后 10 分钟有效；相关操作滑动延长；无操作则再次要求'
        ],
        TH: [
          'OTP บันทึกค่าธรรมเนียม: ใช้ได้ 10 นาทีหลังยืนยันครั้งหนึ่ง ขยายเมื่อมีกิจกรรม ขอใหม่เมื่อว่าง'
        ]
      }
    },
    {
      version: '2.29',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '수수료관리: 본사정책 따름이어도 가맹 저장·변경이력 배분을 우선 표시(목록만 기본값으로 보이던 오류 수정)',
          '수수료관리 저장 시 Google OTP 필수 — 실수 저장·초기화 방지'
        ],
        EN: [
          'Commission list: prefer merchant-saved / history distribution even when Follow HQ (fix list showing defaults)',
          'Google OTP required to save commission fees — prevent accidental overwrite'
        ],
        JP: [
          '手数料管理: 本社ポリシー準拠でも加盟保存・変更履歴の配分を優先表示(一覧のみ既定値になる不具合修正)',
          '手数料保存時にGoogle OTP必須 — 誤保存・初期化防止'
        ],
        CH: [
          '手续费管理：即使跟随总部政策也优先显示商户已存/变更履历分成（修复列表显示默认值）',
          '保存手续费须 Google OTP — 防止误存与重置'
        ],
        TH: [
          'จัดการค่าธรรมเนียม: แม้ตามนโยบาย HQ ก็แสดงการแบ่งที่ร้านบันทึก/ประวัติก่อน (แก้รายการโชว์ค่าเริ่มต้น)',
          'บันทึกค่าธรรมเนียมต้อง Google OTP — กันบันทึกผิด/รีเซ็ต'
        ]
      }
    },
    {
      version: '2.28',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '본사정책 수수료·리스크 매뉴얼 HTML·PDF 출력(통합·필터 상세) — gen.mjs risk'
        ],
        EN: [
          'HQ Fees & risk manuals as HTML/PDF (integrated + filter detail) via gen.mjs risk'
        ],
        JP: [
          '本社ポリシー 手数料・リスク マニュアル HTML・PDF（統合・フィルタ詳細）— gen.mjs risk'
        ],
        CH: [
          '总部政策 手续费与风险手册 HTML/PDF（整合+过滤详情）— gen.mjs risk'
        ],
        TH: [
          'คู่มือค่าธรรมเนียม·ความเสี่ยง HQ เป็น HTML/PDF (รวม+รายละเอียดตัวกรอง) — gen.mjs risk'
        ]
      }
    },
    {
      version: '2.27',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '모든로그인제한: 조직 단계→검색·드롭다운으로 업체 선택 후 사용/미사용/일시중지(상위 조직은 하위 포함, 가맹은 해당만)'
        ],
        EN: [
          'Login restriction: pick org level → search/dropdown company, then Allow/Deny/Pause (parent orgs include subtree; merchants alone)'
        ],
        JP: [
          '全ログイン制限: 組織段階→検索・ドロップダウンで業者選択後に使用/未使用/一時停止(上位は下位含む、加盟は当該のみ)'
        ],
        CH: [
          '全部登录限制：选组织层级→搜索/下拉选商户后使用/停用/暂停（上级含下级，商户仅自身）'
        ],
        TH: [
          'จำกัดล็อกอินทั้งหมด: เลือกระดับ→ค้นหา/รายการเลือกร้าน แล้วใช้/ไม่ใช้/หยุดชั่วคราว (ระดับสูงรวมลูก ร้านเฉพาะร้าน)'
        ]
      }
    },
    {
      version: '2.26',
      kind: 'minor',
      date: '2026-07-22',
      items: {
        KO: [
          '본사정책 수수료·리스크 — 리스크설정·리스크필터링 운영 매뉴얼 정리(동작·항목·가맹 적용·FAQ)'
        ],
        EN: [
          'HQ Fees & risk: operator manuals for Risk settings and Risk filtering (behavior, fields, merchant override, FAQ)'
        ],
        JP: [
          '本社ポリシー 手数料・リスク — リスク設定・フィルタリング運用マニュアル整備'
        ],
        CH: [
          '总部政策 手续费与风险 — 风险设置/过滤运营手册整理'
        ],
        TH: [
          'นโยบาย HQ ค่าธรรมเนียม·ความเสี่ยง — คู่มือตั้งค่า/กรองความเสี่ยง'
        ]
      }
    },
    {
      version: '2.25',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티 JPAY 자동 생성 시 icopayMeta.compName(업체명) 전달 — NOTI 가맹점 목록에 업체코드+업체명 동시 표시'
        ],
        EN: [
          'JPAY NOTI provision sends icopayMeta.compName so the middleware merchant list shows company code + name'
        ],
        JP: [
          'JPAYノティ自動作成で icopayMeta.compName（業者名）を送信 — NOTI加盟一覧に業者コード+業者名を表示'
        ],
        CH: [
          'JPAY 通知自动创建时传递 icopayMeta.compName（商户名）— NOTI 商户列表同时显示商户代码与名称'
        ],
        TH: [
          'สร้าง JPAY NOTI อัตโนมัติส่ง icopayMeta.compName — รายการร้านในมิดเดิลแวร์แสดงรหัส+ชื่อร้าน'
        ]
      }
    },
    {
      version: '2.24',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티관리 JPAY URL 방식: 대체송부=개발(가공)·개발노티 전용사용·가맹 URL 비움·DEALMAI 웹훅 ON으로 NOTI 미들웨어 설정',
          'ICOPAY 노티 이력 삭제 시 NOTI 미들웨어 가맹도 삭제(PUT/DELETE API 연동)'
        ],
        EN: [
          'NOTI mgmt JPAY URL mode: alt-send=dev processed, exclusive dev NOTI, empty merchant URLs, DEALMAI webhook ON in middleware',
          'Deleting ICOPAY NOTI history also deletes the NOTI middleware merchant (PUT/DELETE API)'
        ],
        JP: [
          'ノティ管理 JPAY URL方式: 代替送付=開発(加工)・開発ノティ専用・加盟URL空・DEALMAI Webhook ON をミドルウェアに設定',
          'ICOPAYノティ履歴削除時にNOTIミドルウェア加盟も削除(PUT/DELETE API)'
        ],
        CH: [
          '通知管理 JPAY URL 方式：替代发送=开发(加工)、专用开发通知、商户 URL 留空、DEALMAI Webhook ON 写入中间件',
          'ICOPAY 删除通知履历时同步删除 NOTI 中间件商户（PUT/DELETE API）'
        ],
        TH: [
          'จัดการ NOTI โหมด JPAY URL: ส่งแทน=dev ประมวลผล·ใช้ NOTI dev เท่านั้น·ว่าง URL ร้าน·เปิด DEALMAI webhook ในมิดเดิลแวร์',
          'ลบประวัติ NOTI ใน ICOPAY แล้วลบร้านในมิดเดิลแวร์ด้วย (PUT/DELETE API)'
        ]
      }
    },
    {
      version: '2.23',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '가맹 「본사정책 따름」 수수료정책 변경 시 수수료관리 총본사·배분이 선택한 HQ 템플릿 값으로 즉시 반영(이전 이력 6.9 잔존 수정)'
        ],
        EN: [
          'When merchant Follow HQ fee policy changes, commission mgmt HQ rates update from the selected template (fix stale history overlay)'
        ],
        JP: [
          '加盟「本社ポリシーに従う」手数料政策変更時、手数料管理の総本部・配分が選択テンプレート値に即反映（旧履歴残存を修正）'
        ],
        CH: [
          '商户「跟随总部」手续费政策变更时，手续费管理总总部/分成立即按所选总部模板更新（修复旧履历覆盖）'
        ],
        TH: [
          'เมื่อร้านตามนโยบาย HQ เปลี่ยนนโยบายค่าธรรมเนียม หน้าจัดการค่าธรรมเนียมอัปเดตตามเทมเพลต HQ ทันที (แก้ประวัติเก่าค้าง)'
        ]
      }
    },
    {
      version: '2.22',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '가맹 「본사설정 따름」 경고메세지·로고: FOLLOW_HQ가 DEFAULT로 저장되던 버그 수정 → 본사 결제창 표시 기본값 그대로 노출'
        ],
        EN: [
          'Merchant Follow HQ warning/logo: fix FOLLOW_HQ saved as DEFAULT so HQ checkout display defaults apply'
        ],
        JP: [
          '加盟「本社設定に従う」警告・ロゴ: FOLLOW_HQがDEFAULT保存される不具合を修正→本社決済画面表示既定をそのまま表示'
        ],
        CH: [
          '商户「跟随总部」警告/Logo：修复 FOLLOW_HQ 被存成 DEFAULT，使总部支付窗显示默认生效'
        ],
        TH: [
          'ร้านตาม HQ ข้อความเตือน/โลโก้: แก้บั๊ก FOLLOW_HQ ถูกบันทึกเป็น DEFAULT ให้ค่าเริ่มต้น HQ แสดงตามจริง'
        ]
      }
    },
    {
      version: '2.21',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '결제창 표시 기본값: 상품명 사용「활성(직접입력)」시 본사 기본 상품(명·코드·금액·설명) 입력',
          '가맹 「본사설정 따름」이면 본사 기본 상품 노출, 가맹 직접 설정 시 가맹 값 우선',
          '리스크 필터링「자동기억 기본값」제거·결제창 표시 기본값으로 일원화(다국어)'
        ],
        EN: [
          'Checkout display defaults: when Product name is Active (custom), enter HQ default product (name/code/amount/desc)',
          'Merchants on Follow HQ see HQ default product; merchant override takes priority',
          'Removed Auto-remember from Risk filtering; unified under checkout display defaults (i18n)'
        ],
        JP: [
          '決済画面表示既定: 商品名「有効（直接入力）」時に本社基本商品（名・コード・金額・説明）入力',
          '加盟店「本社設定に従う」なら本社基本商品を表示、直接設定時は加盟店優先',
          'リスクフィルタの自動記憶既定を削除し決済画面表示既定に一元化（多言語）'
        ],
        CH: [
          '支付窗显示默认：商品名「启用（自定义）」时可填写总部默认商品（名称/代码/金额/说明）',
          '商户「跟随总部设置」时显示总部默认商品；商户自设优先',
          '风险过滤移除自动记忆默认值，统一到支付窗显示默认（多语言）'
        ],
        TH: [
          'ค่าเริ่มต้นการแสดงหน้าชำระ: เมื่อชื่อสินค้าเป็นเปิดใช้ (พิมพ์เอง) กรอกสินค้าเริ่มต้น HQ (ชื่อ/รหัส/ยอด/คำอธิบาย)',
          'ร้านตาม HQ แสดงสินค้าเริ่มต้น HQ ห้าร้านตั้งเองจะเหนือกว่า',
          'ลบจำอัตโนมัติออกจากกรองความเสี่ยง รวมที่ค่าเริ่มต้นการแสดง (หลายภาษา)'
        ]
      }
    },
    {
      version: '2.20',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '리스크설정 → 리스크 필터링: 「자동기억 기본값」 제거(본사정책 결제창 표시 기본값으로 일원화)'
        ],
        EN: [
          'Risk settings → Risk filtering: remove Auto-remember default (unified under HQ checkout display defaults)'
        ],
        JP: [
          'リスク設定→リスクフィルタ: 「自動記憶デフォルト」削除（本社決済画面表示既定に一元化）'
        ],
        CH: [
          '风险设置 → 风险过滤：移除「自动记忆默认值」（统一到总部支付窗显示默认）'
        ],
        TH: [
          'ตั้งค่าความเสี่ยง → กรองความเสี่ยง: ลบค่าเริ่มต้นจำอัตโนมัติ (รวมไว้ที่ค่าเริ่มต้นการแสดงหน้าชำระ HQ)'
        ]
      }
    },
    {
      version: '2.19',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '본사정책 결제 URL: 결제창 표시 기본값 카드(가맹점명·다국어·자동기억·로고·경고·배송주소·상품명)',
          '가맹 웹결제: 위 7항목에 「본사설정 따름」 기본·가맹 직접 설정 시 본사보다 우선'
        ],
        EN: [
          'HQ Payment URL: Checkout display defaults card (merchant name, language, remember, logo, warning, shipping, product name)',
          'Merchant web pay: 7 items default to Follow HQ; merchant override takes priority'
        ],
        JP: [
          '本社ポリシー決済URL: 決済画面表示既定カード（加盟店名・多言語・自動記憶・ロゴ・警告・配送・商品名）',
          '加盟店ウェブ決済: 上記7項目は「本社設定に従う」既定、直接設定時は本社より優先'
        ],
        CH: [
          '总部政策支付 URL：支付窗显示默认卡片（商户名·多语言·自动记忆·Logo·警告·配送·商品名）',
          '商户网页支付：上述 7 项默认跟随总部；商户自选优先于总部'
        ],
        TH: [
          'นโยบาย HQ URL ชำระ: การ์ดค่าเริ่มต้นการแสดง (ชื่อร้าน·ภาษา·จำ·โลโก้·เตือน·จัดส่ง·ชื่อสินค้า)',
          'ชำระเว็บร้าน: 7 รายการเริ่มต้นตาม HQ ห้าร้านเลือกเองจะเหนือกว่า HQ'
        ]
      }
    },
    {
      version: '2.18',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티 생성 이력 수정: OTP 라벨, API↔URL 전환 저장(PUT·동일 슬롯 교체)로 NOTI 양방향 유지',
          '노티 생성 이력 테이블: NOTI ID와 슬롯 사이에 방식(API/URL) 컬럼 추가'
        ],
        EN: [
          'NOTI history edit: OTP label; API↔URL save via PUT/same-slot replace keeps NOTI bi-directional sync',
          'NOTI history table: Method (API/URL) column between NOTI ID and Slot'
        ],
        JP: [
          'ノティ作成履歴修正: OTPラベル、API↔URL切替保存(PUT・同一スロット入替)でNOTI双方向維持',
          'ノティ作成履歴表: NOTI IDとスロットの間に方式(API/URL)列を追加'
        ],
        CH: [
          'NOTI 创建历史修改：OTP 标签；API↔URL 保存（PUT/同槽替换）保持与 NOTI 双向同步',
          'NOTI 创建历史表：在 NOTI ID 与槽位之间增加方式（API/URL）列'
        ],
        TH: [
          'แก้ไขประวัติ NOTI: ป้าย OTP; บันทึกสลับ API↔URL (PUT/แทนที่สล็อตเดิม) ให้ซิงก์สองทางกับ NOTI',
          'ตารางประวัติ NOTI: เพิ่มคอลัมน์วิธี (API/URL) ระหว่าง NOTI ID กับ Slot'
        ]
      }
    },
    {
      version: '2.17',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티 생성 이력: Notify URL→Callback, Callback URL→Result, 관리 복사 버튼 N복사→Callback·C복사→Result',
          'JPAY 노티 생성: DEALMAI Partner 코드 입력창을 OTP보다 약 10% 넓은 폭으로 축소'
        ],
        EN: [
          'NOTI history: Notify URL→Callback, Callback URL→Result; action copy buttons N→Callback, C→Result',
          'JPAY NOTI create: shrink DEALMAI Partner field to ~10% wider than OTP'
        ],
        JP: [
          'ノティ作成履歴: Notify URL→Callback、Callback URL→Result、管理コピー N→Callback・C→Result',
          'JPAYノティ作成: DEALMAI Partnerコード入力をOTPより約10%広い幅に縮小'
        ],
        CH: [
          'NOTI 创建历史：Notify URL→Callback，Callback URL→Result；管理复制按钮 N→Callback、C→Result',
          'JPAY NOTI 创建：DEALMAI Partner 代码输入框缩至比 OTP 宽约 10%'
        ],
        TH: [
          'ประวัติ NOTI: Notify URL→Callback, Callback URL→Result; ปุ่มคัดลอก N→Callback, C→Result',
          'สร้าง JPAY NOTI: ย่อช่อง DEALMAI Partner ให้กว้างกว่า OTP ประมาณ 10%'
        ]
      }
    },
    {
      version: '2.16',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          'JPAY 노티 생성 UI: 입력 행 여백 통일, OTP 라벨·좁은 입력칸, 안내문을 OTP 아래·버튼 위 여백 정리'
        ],
        EN: [
          'JPAY NOTI create UI: even field spacing, OTP label + compact input, hint under OTP with space before actions'
        ],
        JP: [
          'JPAYノティ作成UI: 入力行余白統一、OTPラベル・狭い入力、案内をOTP下・ボタン前余白整理'
        ],
        CH: [
          'JPAY NOTI 创建 UI：统一输入行间距，OTP 标签与窄输入框，说明置于 OTP 下方并与按钮留白'
        ],
        TH: [
          'UI สร้าง JPAY NOTI: ระยะห่างช่องกรอกสม่ำเสมอ ป้าย OTP + ช่องแคบ คำอธิบายใต้ OTP และเว้นก่อนปุ่ม'
        ]
      }
    },
    {
      version: '2.15',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티 생성 이력: 업체코드·조회·삭제를 한 줄로 배치, 행 선택·일괄삭제(OTP)',
          'Notify/Callback URL 컬럼명(JPAY 제거), URL 셀 복사 링크 제거 → 관리에 N복사·C복사',
          '이력 테이블 컬럼 단축: NOTI ID, 슬롯, DEALMAI'
        ],
        EN: [
          'NOTI history: one-line company code / Search / Delete; row select + bulk delete (OTP)',
          'Notify/Callback URL headers (drop JPAY); remove in-cell Copy → N Copy / C Copy in Actions',
          'History table shorter headers: NOTI ID, Slot, DEALMAI'
        ],
        JP: [
          'ノティ作成履歴: 業者コード・照会・削除を1行配置、行選択・一括削除(OTP)',
          'Notify/Callback URL列名(JPAY削除)、セル内コピー除去→管理にNコピー・Cコピー',
          '履歴テーブル列短縮: NOTI ID、スロット、DEALMAI'
        ],
        CH: [
          'NOTI 创建历史：商户代码/查询/删除同一行；行选择与批量删除(OTP)',
          'Notify/Callback URL 列名（去掉 JPAY）；单元格内复制改为管理栏 N复制/C复制',
          '历史表列名缩短：NOTI ID、槽位、DEALMAI'
        ],
        TH: [
          'ประวัติ NOTI: รหัสร้าน/ค้นหา/ลบในบรรทัดเดียว; เลือกแถว+ลบหลายรายการ (OTP)',
          'หัวคอลัมน์ Notify/Callback (ตัด JPAY); เอา Copy ในเซลล์ออก → Nคัดลอก/Cคัดลอก ที่จัดการ',
          'ย่อหัวตารางประวัติ: NOTI ID, สล็อต, DEALMAI'
        ]
      }
    },
    {
      version: '2.14',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티 생성 이력 수정: 동일 가맹에서 연동방식 API↔URL 전환 저장 허용(설정 충돌 시 동일 슬롯 재등록)',
          'JPAY 노티 생성·수정: 연동방식 아래 안내 문구 배치, 가맹·전산·개발 노티를 카드로 노출',
          'JPAY 연동방식·노티 카드·OTP·Partner 잠금 관련 UI·오류 문구 다국어(EN/JP/CH/TH)',
          '노티 생성 이력 수정 OTP 입력창 표시·다국어 — Google OTP(노티관리) 안내'
        ],
        EN: [
          'NOTI log edit: allow same merchant API↔URL integration mode switch (re-register same slot on settings conflict)',
          'JPAY NOTI create/edit: place mode hint under Integration mode; show merchant/internal/dev notify as cards',
          'i18n (EN/JP/CH/TH) for JPAY integration mode, NOTI cards, OTP, and Partner lock UI/errors',
          'NOTI log edit OTP field visibility and i18n — Google OTP (NOTI management) prompts'
        ],
        JP: [
          'ノティ生成履歴修正: 同一加盟店で連携方式API↔URL切替保存を許可（設定衝突時は同一スロット再登録）',
          'JPAYノティ作成・修正: 連携方式の下に説明文、加盟店・電算・開発ノティをカード表示',
          'JPAY連携方式・ノティカード・OTP・Partnerロック関連UI・エラーの多言語(EN/JP/CH/TH)',
          'ノティ生成履歴修正のOTP入力表示・多言語 — Google OTP（ノティ管理）案内'
        ],
        CH: [
          'NOTI 生成履历修改：同一商户允许 API↔URL 对接方式切换保存（设置冲突时同槽位重新注册）',
          'JPAY NOTI 创建/修改：对接方式下方显示说明，商户/系统/开发 NOTI 以卡片展示',
          'JPAY 对接方式、NOTI 卡片、OTP、Partner 锁定相关 UI/错误的多语言(EN/JP/CH/TH)',
          'NOTI 履历修改 OTP 输入显示与多语言 — Google OTP（NOTI 管理）提示'
        ],
        TH: [
          'แก้ไขประวัติ NOTI: อนุญาตสลับโหมด API↔URL ของร้านเดิม (ลงทะเบียนสล็อตเดิมใหม่เมื่อตั้งค่าชนกัน)',
          'สร้าง/แก้ไข JPAY NOTI: วางคำอธิบายใต้โหมดเชื่อมต่อ และแสดง NOTI ร้าน/ระบบ/dev เป็นการ์ด',
          'i18n (EN/JP/CH/TH) สำหรับโหมดเชื่อมต่อ JPAY การ์ด NOTI OTP และล็อก Partner',
          'แสดงช่อง OTP และ i18n ในแก้ไขประวัติ — คำแนะนำ Google OTP (จัดการ NOTI)'
        ]
      }
    },
    {
      version: '2.13',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '노티구성설정: 기본 DEALMAI Partner 코드 잠금·[수정]/[저장] — 전체 저장으로 값이 비워지지 않도록 보강'
        ],
        EN: [
          'Notify config: lock default DEALMAI Partner code with Edit/Save — page Save no longer clears it'
        ],
        JP: [
          'ノティ構成: 既定DEALMAI Partnerコードをロック・[修正]/[保存] — 画面全体保存で空にならないよう補強'
        ],
        CH: [
          '通知环境：默认 DEALMAI Partner 代码锁定及[修改]/[保存] — 整页保存不会再清空'
        ],
        TH: [
          'ตั้งค่าแจ้งเตือน: ล็อกรหัส DEALMAI Partner เริ่มต้น + แก้ไข/บันทึก — บันทึกทั้งหน้าจะไม่ล้างค่า'
        ]
      }
    },
    {
      version: '2.12',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          'JPAY 노티 생성: 연동방식(API/URL) 추가 — URL이면 가맹·전산·개발 노티 UI 잠금, 가맹 포워딩 끔·개발 대체 URL·RESULT AUTO·RouteNo=슬롯 자동'
        ],
        EN: [
          'JPAY NOTI provision: add integration mode (API/URL) — URL locks merchant/ledger/dev NOTI UI and auto-applies merchant-forward off, Dev alt URLs, RESULT AUTO, RouteNo=slot'
        ],
        JP: [
          'JPAYノティ作成: 連携方式(API/URL)追加 — URL時は加盟・全算・開発ノティUIをロックし、転送OFF・開発代替URL・RESULT AUTO・RouteNo=スロットを自動適用'
        ],
        CH: [
          'JPAY NOTI 创建：新增对接方式(API/URL) — URL 时锁定商户/账务/开发 NOTI UI，并自动应用关闭商户转发、开发替代 URL、RESULT AUTO、RouteNo=槽位'
        ],
        TH: [
          'สร้าง JPAY NOTI: เพิ่มโหมดเชื่อมต่อ (API/URL) — โหมด URL ล็อก UI NOTI ร้าน/บัญชี/dev และตั้ง forward ปิด, URL Dev แทน, RESULT AUTO, RouteNo=สล็อต อัตโนมัติ'
        ]
      }
    },
    {
      version: '2.11',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          'JPAY 노티 생성: 발급 URL을 가맹 JPAY 수신통보에 강제 저장(검증)하고, 미반영 시 업체 상세 조회에서 이력으로 자동 보강'
        ],
        EN: [
          'JPAY NOTI provision: force-save issued URLs to merchant JPAY receive fields (verified); auto-backfill from provision log on merchant detail if empty'
        ],
        JP: [
          'JPAYノティ生成: 発行URLを加盟JPAY受信へ強制保存(検証)。未反映時は加盟詳細で履歴から自動補完'
        ],
        CH: [
          'JPAY 通知创建：强制将签发 URL 写入商户 JPAY 接收字段并校验；若未写入，商户详情从创建历史自动补全'
        ],
        TH: [
          'สร้าง JPAY NOTI: บังคับบันทึก URL ที่ออกไปยัง JPAY รับของร้าน (ตรวจแล้ว); ถ้ายังว่าง เติมจากประวัติเมื่อเปิดรายละเอียดร้าน'
        ]
      }
    },
    {
      version: '2.10',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '가맹 API 키·문서: 「강제여부 저장」옆 「발급초기화」— 시크릿 재발급·키트 조회 후 목록 초기 화면으로 복귀'
        ],
        EN: [
          'Merchant API keys: 「Reset issue」 next to Save enforce — return to merchant list after secret rotate / kit view'
        ],
        JP: [
          '加盟店APIキー: 「強制可否保存」横の「発行初期化」— シークレット再発行・キット表示後に一覧へ戻る'
        ],
        CH: [
          '商户 API 密钥：在「保存强制」旁增加「发行重置」— 重新发密钥/查看套件后回到列表初始画面'
        ],
        TH: [
          'คีย์ Merchant API: 「รีเซ็ตการออกคีย์」ข้างบันทึกบังคับ — กลับหน้ารายการหลังออก secret/ดูชุด'
        ]
      }
    },
    {
      version: '2.9',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '가맹 API 출시: 우측 출시 가이드 패널 제거 → 「API 문서」옆 「출시 가이드」탭으로 이동',
          '공통설정·가맹 등록·키·문서·API 문서 화면을 전폭으로 사용'
        ],
        EN: [
          'Merchant API launch: move side launch guide into a full-width 「Launch guide」 tab next to API docs',
          'Common / register / keys / API docs steps use full width'
        ],
        JP: [
          '加盟店API公開: 右側ガイドを廃止し「API文書」隣の「公開ガイド」タブへ移設',
          '共通・加盟登録・キー・API文書を全幅表示'
        ],
        CH: [
          '商户 API 发布：取消右侧发布指南，改为「API 文档」旁的「发布指南」全宽标签',
          '通用/注册/密钥/API 文档步骤使用全宽'
        ],
        TH: [
          'เปิดใช้ Merchant API: ย้ายคู่มือด้านขวาไปแท็บ「คู่มือเปิดใช้」ข้างเอกสาร API แบบเต็มความกว้าง',
          'ขั้นตอนตั้งค่า/ลงทะเบียน/คีย์/เอกสาร API ใช้พื้นที่เต็มความกว้าง'
        ]
      }
    },
    {
      version: '2.8',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          'JPAY 노티 생성: 발급 Notify/Callback URL을 가맹점 JPAY 수신통보 URL에 다시 자동 반영(응답 필드 누락 시 슬롯 기준 조립)',
          '업체 수정: JPAY 수신통보 URL 미전달·카드 미노출 시 기존 URL이 지워지지 않도록 보존'
        ],
        EN: [
          'JPAY NOTI provision: auto-write issued Notify/Callback URLs to merchant JPAY receive URLs again (build from slot if response omits fields)',
          'Merchant update: keep existing JPAY receive URLs when params omitted or the card is hidden'
        ],
        JP: [
          'JPAYノティ生成: 発行Notify/Callback URLを加盟JPAY受信通知URLへ再自動反映（応答欠落時はスロットから組立）',
          '加盟更新: JPAY受信URL未送信・カード非表示時に既存URLを消さない'
        ],
        CH: [
          'JPAY 通知创建：再次将签发的 Notify/Callback URL 自动写入商户 JPAY 接收通知 URL（响应缺字段时按槽位组装）',
          '商户更新：未传参或卡片不可见时保留既有 JPAY 接收 URL'
        ],
        TH: [
          'สร้าง JPAY NOTI: เขียน Notify/Callback ที่ออกให้ลง URL รับแจ้ง JPAY ของร้านอีกครั้ง (ประกอบจากสล็อตถ้า response ไม่มีฟิลด์)',
          'แก้ร้าน: คง URL รับ JPAY เดิมเมื่อไม่ส่งพารามิเตอร์หรือซ่อนการ์ด'
        ]
      }
    },
    {
      version: '2.7',
      kind: 'minor',
      date: '2026-07-21',
      items: {
        KO: [
          '일괄운영(가맹점사용제한·URL결제제한): 사용·미사용·일시중지를 미사용 가맹 포함 전원에 적용',
          '일시중지 중 개별 「웹결제 사용」으로 우회 불가. 가맹 DB는 유지하며 중지해제 시 원래 사용/미사용으로 복귀'
        ],
        EN: [
          'Bulk ops (org use / URL pay): Use, Not use, and Temporary suspend apply to all merchants including previously unused',
          'No bypass via individual web-pay=Y while paused; merchant DB kept; Release restores original use/unused'
        ],
        JP: [
          '一括運用(加盟使用・URL決済): 使用・未使用・一時停止を未使用加盟含む全員に適用',
          '一時停止中の個別ウェブ決済=使用での回避不可。加盟DBは保持し、停止解除で元の使用/未使用に復帰'
        ],
        CH: [
          '批量运营(商户使用/URL支付)：使用、未使用、临时暂停适用于含原本未使用在内的全部商户',
          '暂停期间不可用单独网页支付=使用绕过；保留商户库值；解除后恢复原有使用/未使用'
        ],
        TH: [
          'ปฏิบัติการชุด (ใช้ร้าน/URL): ใช้/ไม่ใช้/ระงับชั่วคราวใช้กับร้านทั้งหมดรวมที่เดิมไม่ใช้',
          'ระหว่างระงับหลบด้วยเว็บชำระ=ใช้ไม่ได้ คงค่า DB ของร้าน เมื่อยกเลิกระงับกลับสู่ใช้/ไม่ใช้เดิม'
        ]
      }
    },
    {
      version: '2.6',
      kind: 'minor',
      date: '2026-07-20',
      items: {
        KO: [
          'PG사 연동: API Key·MD5를 앞3자+*****로 표시(등록 여부 확인), password 필드 제거로 마스킹 가시화',
          'PG사 연동: 거래명세서 Acquirer·Payment Switcher 저장 후 재진입 시 값 유지·표시 보강',
          '거래명세서 결제대행(총판): 총판 업체정보 이메일·전화(국가번호 포함) 자동 표시'
        ],
        EN: [
          'PG agency: show API Key/MD5 as first 3 + ***** (confirm registered); text fields so masks are visible',
          'PG agency: keep and redisplay Acquirer/Payment Switcher after save',
          'Receipt Payment Provider: auto-fill distributor email and phone (with country dial code)'
        ],
        JP: [
          'PG連携: API Key・MD5を先頭3+*****表示（登録確認）、マスクが見えるようtext化',
          'PG連携: 取引明細 Acquirer・Payment Switcher 保存後の再表示を補強',
          '取引明細の決済代行(総販): 総販会社情報のメール・電話(国番号含む)を自動表示'
        ],
        CH: [
          'PG对接：API Key/MD5 显示为前3+*****（确认已登记）；改为文本以便看到掩码',
          'PG对接：交易明细 Acquirer/Payment Switcher 保存后再打开仍显示',
          '交易明细支付服务商(总代)：自动显示总代公司邮箱与电话（含国家区号）'
        ],
        TH: [
          'PG agency: แสดง API Key/MD5 เป็น 3 ตัวแรก+***** (ยืนยันว่ามีคีย์); ใช้ text ให้เห็น mask',
          'PG agency: เก็บและแสดง Acquirer/Payment Switcher หลังบันทึกเมื่อเปิดใหม่',
          'ใบเสร็จ Payment Provider: ดึงอีเมลและโทรศัพท์ตัวแทน (รวมรหัสประเทศ) อัตโนมัติ'
        ]
      }
    },
    {
      version: '2.5',
      kind: 'minor',
      date: '2026-07-20',
      items: {
        KO: [
          '가맹 PG 바인딩: MID+API Key 쌍이 모두 있으면 본사(PG사) 자격보다 우선 사용',
          '하나만 있으면 본사 값 사용·IV(MD5)는 선택(가맹 있으면 가맹, 없으면 본사)',
          'PG 선택 시 MID·API Key(앞3자+*****) 자동 표시, 마스킹값은 원문으로 저장하지 않음'
        ],
        EN: [
          'Merchant PG binding: when both MID+API Key are set, they override HQ agency credentials',
          'Incomplete pair falls back to HQ; IV/MD5 is optional (merchant if set, else HQ)',
          'On PG select, auto-fill MID and masked API Key (first 3 + *****); masks are not stored as secrets'
        ],
        JP: [
          '加盟PGバインド: MID+API Keyが揃えば本部(PG)資格より優先',
          '片方のみは本部値。IV/MD5は任意（加盟があれば加盟、なければ本部）',
          'PG選択時にMID・API Key(先頭3+*****)を自動表示。マスク値は秘密として保存しない'
        ],
        CH: [
          '商户 PG 绑定：MID+API Key 同时填写时优先于总部(PG)凭证',
          '只填一项则用总部；IV/MD5 可选（有商户用商户，否则总部）',
          '选择 PG 时自动填 MID 与掩码 API Key（前3+*****）；掩码不作为密钥保存'
        ],
        TH: [
          'ผูก PG ร้านค้า: มีทั้ง MID+API Key จะใช้ก่อนค่าสำนักงานใหญ่(PG)',
          'มีอย่างเดียวใช้ค่า HQ; IV/MD5 ไม่บังคับ (มีของร้านใช้ของร้าน ไม่มีใช้ HQ)',
          'เลือก PG แล้วเติม MID และ API Key แบบปิดบัง (3 ตัวแรก+*****) อัตโนมัติ — ไม่บันทึกค่า mask เป็นความลับ'
        ]
      }
    },
    {
      version: '2.4',
      kind: 'minor',
      date: '2026-07-20',
      items: {
        KO: [
          'JPAY pay_notifyurl·pay_callbackurl — 가맹 JPAY 수신통보 URL(노티미들웨어)을 PG에 그대로 송부하도록 7/3 이전 방식 복원',
          '노티미들웨어 수신 후 ICOPAY 릴레이 구조 복구(외부 URL을 cbJpay/rsJpay로 강제 치환하지 않음)'
        ],
        EN: [
          'JPAY pay_notifyurl/pay_callbackurl — restore pre-7/3 behavior: send merchant JPAY notify-middleware URLs to PG as-is',
          'Restore MW → ICOPAY relay (no forced rewrite of external URLs to cbJpay/rsJpay)'
        ],
        JP: [
          'JPAY pay_notifyurl·pay_callbackurl — 加盟JPAY受信URL(ノティMW)をPGへそのまま送る7/3以前方式を復元',
          'ノティMW受信後ICOPAYリレーを復旧（外部URLのcbJpay/rsJpay強制置換なし）'
        ],
        CH: [
          'JPAY pay_notifyurl·pay_callbackurl — 恢复 7/3 前方式：将商户 JPAY 通知中间件 URL 原样发给 PG',
          '恢复中间件接收后转发 ICOPAY（不再强制改为 cbJpay/rsJpay）'
        ],
        TH: [
          'JPAY pay_notifyurl·pay_callbackurl — กู้คืนแบบก่อน 7/3 ส่ง URL แจ้งเตือน JPAY (middleware) ไป PG ตามเดิม',
          'กู้คืน MW รับแล้ว relay ไป ICOPAY (ไม่บังคับเปลี่ยนเป็น cbJpay/rsJpay)'
        ]
      }
    },
    {
      version: '2.3',
      kind: 'minor',
      date: '2026-07-20',
      items: {
        KO: [
          'JPAY URL·인라인 결제 — 시스템 ingress(cbJpay/rsJpay) 수신 허용 복구(가맹 노티미들웨어 외부 URL 강제 치환 후 복귀·거래 반영)',
          'rsJpay는 RESULT·cbJpay는 CALLBACK으로 처리 — 주문 기반 가맹 해석 및 노티미들웨어 리다이렉트 기존 흐름 유지'
        ],
        EN: [
          'JPAY URL/inline pay — restore system ingress (cbJpay/rsJpay) accept path after merchant notify-middleware URL rewrite',
          'rsJpay=RESULT, cbJpay=CALLBACK — keep order-based merchant resolve and redirect to notify middleware'
        ],
        JP: [
          'JPAY URL・インライン決済 — システム ingress(cbJpay/rsJpay) 受信を復旧（加盟店ノティMW外部URL置換後の復帰・取引反映）',
          'rsJpay=RESULT・cbJpay=CALLBACK — 注文による加盟店特定とノティMWリダイレクトの従来流れを維持'
        ],
        CH: [
          'JPAY URL/内嵌支付 — 恢复系统 ingress(cbJpay/rsJpay) 接收（商户通知中间件外域 URL 替换后的回跳与入账）',
          'rsJpay=RESULT、cbJpay=CALLBACK — 保持按订单解析商户并跳转通知中间件的原流程'
        ],
        TH: [
          'JPAY URL/อินไลน์ — กู้คืนการรับ ingress ระบบ (cbJpay/rsJpay) หลังแทนที่ URL แจ้งเตือนภายนอก',
          'rsJpay=RESULT, cbJpay=CALLBACK — คงการระบุร้านจากออเดอร์และ redirect ไป notify middleware'
        ]
      }
    },
    {
      version: '2.2',
      kind: 'minor',
      date: '2026-07-16',
      items: {
        KO: [
          '결제대행사(내부 PG) 연동 확장 — 카드인증(3DS/NONE)·통합 구독(정기결제) MIT 경로 (가맹·구매자 화면은 ICOPAY 중립)',
          '가맹 API 배포문서 — 해당 가맹에 활성화된 기능(정기결제·챗봇결제·분할결제) 매뉴얼만 노출',
          '운영 배포 자동화(Deploy-Prod) · 재시작 다운타임 단축(서비스 유지 중 준비 → 교체)',
          '본사·가맹 카드인증 설정(본사 기본 THREE_DS · 가맹 FOLLOW_HQ) URL·API 동일 적용',
          '버전 정책: 결제대행사 추가·신규 PG 연동 시 마이너 버전 +0.1 필수'
        ],
        EN: [
          'Expanded internal PG integration — card auth (3DS/NONE) & unified subscription MIT (merchant/buyer UI stays ICOPAY-neutral)',
          'Merchant API deploy docs — show only manuals for enabled features (subscription, chatbot pay, split pay)',
          'Production deploy automation (Deploy-Prod) & shorter restart downtime (prep while live, then switch)',
          'HQ/merchant card-auth settings (HQ default THREE_DS · merchant FOLLOW_HQ) for URL + API',
          'Version policy: adding a payment gateway / new PG integration requires minor +0.1'
        ],
        JP: [
          '決済代行(内部PG)連携拡張 — カード認証(3DS/NONE)・統合サブスク MIT（加盟店・購入者画面は ICOPAY 中立）',
          '加盟店API配布ドキュメント — 有効機能(定期課金・チャットボット決済・分割)のマニュアルのみ表示',
          '本番デプロイ自動化(Deploy-Prod)・再起動ダウンタイム短縮',
          '本社・加盟店カード認証設定（本社既定 THREE_DS · 加盟 FOLLOW_HQ）を URL・API に同一適用',
          'バージョン方針: 決済代行追加・新規PG連携時はマイナー +0.1 必須'
        ],
        CH: [
          '扩展内部 PG 对接 — 卡认证(3DS/NONE)·统一订阅 MIT（商户/买家界面保持 ICOPAY 中性）',
          '商户 API 部署文档 — 仅展示已启用功能（订阅·聊天机器人支付·分期）手册',
          '生产部署自动化(Deploy-Prod)·缩短重启停机',
          '总部/商户卡认证设置（总部默认 THREE_DS · 商户 FOLLOW_HQ）统一用于 URL 与 API',
          '版本策略：新增支付通道/PG 对接时必须次版本 +0.1'
        ],
        TH: [
          'ขยายการเชื่อม PG ภายใน — ยืนยันบัตร(3DS/NONE)·subscription MIT รวม (UI ร้าน/ผู้ซื้อเป็น ICOPAY กลาง)',
          'เอกสาร Deploy Merchant API — แสดงเฉพาะคู่มือฟีเจอร์ที่เปิด (สมัครสมาชิก·แชทบอท·แบ่งงวด)',
          'อัตโนมัติ Deploy-Prod · ลด downtime ตอนรีสตาร์ท',
          'ตั้งค่ายืนยันบัตร HQ/ร้าน (HQ ค่าเริ่ม THREE_DS · ร้าน FOLLOW_HQ) ใช้กับ URL+API',
          'นโยบายเวอร์ชัน: เพิ่มช่องทางชำระ/PG ใหม่ ต้อง minor +0.1'
        ]
      },
      howTo: {
        KO: [
          {
            title: '1) 카드인증방식 (3DS / 2DS)',
            steps: [
              '본사: 본사정책 → 결제·URL → 결제 라우팅 탭 → 「결제창·카드인증 기본값」카드의 「카드인증방식 기본값」을 3DS(권장) 또는 2DS(비인증)로 저장합니다.',
              '가맹: 업체관리 → 해당 가맹 상세 → 「카드인증방식」에서 「본사정책 따름」 또는 직접 3DS/2DS를 선택합니다. 가맹이 직접 고르면 본사보다 우선합니다.',
              '적용 범위: 공개 URL결제·API 인라인 Checkout에 같은 실효값이 적용됩니다.',
              '예외: 통합 구독(정기결제) 초회 결제는 항상 3DS이며, 카드인증방식 설정과 무관합니다. 이후 정기 청구(MIT)는 고객 창 없이 진행됩니다.',
              '가맹·구매자 화면·API 응답에는 운영 PG 이름이 나오지 않으며 항상 ICOPAY로 표시됩니다.'
            ]
          },
          {
            title: '2) 통합 구독(정기결제) API',
            steps: [
              '본사: 결제로직·API연동에서 구독 기능을 켜고, 가맹 바인딩에 구독 가능 운영 PG가 있어야 합니다.',
              '가맹: 업체 상세 → 「JPAY API 구독」사용 = Y (화면 명칭과 무관하게 ICOPAY 통합 subscription API를 씁니다).',
              '연동: 가맹점API(또는 본사 API배포문서)의 「통합 구독」엔드포인트 — prepare → embed-checkout-subscribe → status / cancel.',
              '초회: 고객 3DS 인증 후 토큰 저장 → 이후 회차는 스케줄러가 MIT로 청구합니다.',
              '가맹에 구독이 OFF이면 배포문서·체크리스트에 구독 안내가 나타나지 않습니다.'
            ]
          },
          {
            title: '3) 가맹 API 배포문서 — 활성 기능만 보기',
            steps: [
              '본사 미리보기: 연동·배포 → 가맹 API 출시 → API 문서(또는 본사정책 경로의 API배포문서)에서 가맹을 선택합니다.',
              '가맹 공식 문서: 가맹 로그인 → 업체관리 → 가맹점API.',
              '「활성화된 기능 안내」에는 그 가맹에 켜진 항목만 표시됩니다 — 정기결제 / 챗봇결제 / 분할결제.',
              '챗봇: 업체 상세 「챗봇결제 사용」= Y → 챗봇관리·챗봇결제 URL·위젯만 안내. 구독·분할 매뉴얼과 섞어 배포하지 마세요.',
              '분할: 「분할결제 사용여부」= Y → 분할관리·분할결제 URL만 안내. 일반 Checkout·구독 API와 별개입니다.',
              '미사용 기능 매뉴얼을 메일·ZIP으로 일괄 배포하면 혼란이 생기므로, 로그인 후 가맹점API 화면을 공식 문서로 사용하세요.'
            ]
          },
          {
            title: '4) 운영 배포 (자동화)',
            steps: [
              '개발 완료 후 에이전트에게 「배포해」라고 요청합니다. (자격증명: PC의 .pg-deploy/credentials.env)',
              '순서: bootJar → JAR 업로드(옛 서버 유지) → SQL(있을 때) → 재시작 → Started 확인.',
              '자동화 실패 시: JAR·SQL은 수동 FTP/적용 후 서버에서 ./restart-pg-app.sh 로 재시작하면 됩니다.',
              '다운타임은 재시작 구간만 짧습니다. 업로드 중에는 기존 프로세스가 계속 서비스합니다.'
            ]
          },
          {
            title: '5) 플랫폼 버전 (이 탭)',
            steps: [
              '라이브 버전·변경 요약·사용 방법은 본사정책 → 플랫폼 → 업데이트 내용에서 확인합니다. 탭만 바꿔도 동일 허브입니다.',
              '결제대행사(PG)를 새로 추가·연동하면 반드시 마이너 +0.1 (예: 2.2 → 2.3) 하고 이 탭에 기록합니다.'
            ]
          }
        ],
        EN: [
          {
            title: '1) Card authentication (3DS / 2DS)',
            steps: [
              'HQ: HQ Policy → Payment & URL → Payment routing → Card auth defaults card → set Card auth default to 3DS (recommended) or 2DS.',
              'Merchant: Company detail → Card auth = Follow HQ or choose 3DS/2DS (merchant choice wins).',
              'Applies to public URL pay and API inline checkout with the same effective value.',
              'Exception: subscription first charge is always 3DS; later MIT charges have no customer window.',
              'Merchant/buyer UI and APIs always show ICOPAY — never the operational PG name.'
            ]
          },
          {
            title: '2) Unified subscription API',
            steps: [
              'Enable subscription at HQ payment/API config and ensure an operational subscription-capable PG binding.',
              'Merchant: turn subscription use ON; use unified /checkout/subscription/* endpoints from Merchant API docs.',
              'Flow: prepare → embed-checkout-subscribe → status/cancel; first 3DS then scheduled MIT.',
              'If subscription is OFF for the merchant, subscription docs are hidden from the deploy kit.'
            ]
          },
          {
            title: '3) Feature-gated merchant API docs',
            steps: [
              'HQ preview: Merchant API deploy docs — select merchant. Merchant: Company → Merchant API.',
              '“Enabled features” lists only subscription / chatbot / split pay when each flag is ON.',
              'Do not hand out manuals for features the merchant does not use — use the in-app portal as the official doc.'
            ]
          },
          {
            title: '4) Production deploy',
            steps: [
              'Ask the agent “배포해”. Order: bootJar → upload JAR (old process stays up) → SQL if any → restart → Started.',
              'Fallback: upload JAR/SQL manually, then ./restart-pg-app.sh on the server.'
            ]
          },
          {
            title: '5) Platform version (this tab)',
            steps: [
              'Live version and how-to live under HQ Policy → Platform → Release notes (same hub tabs).',
              'Adding a new PG always requires minor +0.1 and a note here.'
            ]
          }
        ],
        JP: [
          {
            title: '1) カード認証（3DS / 2DS）',
            steps: [
              '本社: 本社ポリシー → 決済・URL → 決済ルーティング → 「決済画面・カード認証既定」で設定。',
              '加盟店: 業者詳細で「本社に従う」または 3DS/2DS を選択（加盟店優先）。',
              'URL決済・APIインラインに同一実効値。サブスク初回は常に3DS。',
              '画面・APIは常に ICOPAY 表記（運用PG名は非表示）。'
            ]
          },
          {
            title: '2) 統合サブスク API',
            steps: [
              '本社でサブスクON・運用PGバインド確認。加盟店でサブスク使用=Y。',
              'prepare → embed-checkout-subscribe → status/cancel。初回3DS後 MIT。',
              'OFFの加盟店にはサブスク文書を出さない。'
            ]
          },
          {
            title: '3) 機能別ドキュメント',
            steps: [
              '加盟店API画面の「有効機能」に定期・チャットボット・分割のみ表示。',
              '未使用機能マニュアルは配布しない。'
            ]
          },
          {
            title: '4) 本番デプロイ',
            steps: [
              '「배포해」で自動化。失敗時は JAR/SQL 手動後 ./restart-pg-app.sh。'
            ]
          },
          {
            title: '5) バージョン',
            steps: [
              '本タブで確認。PG追加時はマイナー +0.1 必須。'
            ]
          }
        ],
        CH: [
          {
            title: '1) 卡认证（3DS / 2DS）',
            steps: [
              '总部：总部政策 → 支付·URL → 支付路由 → 「支付窗·卡认证默认」中设置卡认证默认值。',
              '商户：商户详情选择「跟随总部」或 3DS/2DS（商户优先）。',
              '适用于 URL 支付与 API 内联；订阅首笔始终 3DS。',
              '界面与 API 恒为 ICOPAY，不显示运营 PG 名。'
            ]
          },
          {
            title: '2) 统一订阅 API',
            steps: [
              '总部开启订阅并配置可用 PG；商户开启订阅使用。',
              'prepare → embed-checkout-subscribe → status/cancel；首笔 3DS 后 MIT。',
              '未开启订阅的商户不展示订阅文档。'
            ]
          },
          {
            title: '3) 按功能展示文档',
            steps: [
              '商户 API 页「已启用功能」仅显示已打开的订阅/聊天机器人/分期。',
              '勿向未开通功能的商户发放对应手册。'
            ]
          },
          {
            title: '4) 生产部署',
            steps: [
              '对代理说「배포해」；失败则手动上传 JAR/SQL 后执行 ./restart-pg-app.sh。'
            ]
          },
          {
            title: '5) 版本',
            steps: [
              '在本页查看。新增 PG 必须次版本 +0.1。'
            ]
          }
        ],
        TH: [
          {
            title: '1) ยืนยันบัตร (3DS / 2DS)',
            steps: [
              'HQ: นโยบาย → การชำระ·URL → เส้นทางชำระเงิน → การ์ดค่าเริ่มต้นหน้าต่างชำระ/ยืนยันบัตร',
              'ร้าน: รายละเอียดร้าน เลือกตาม HQ หรือ 3DS/2DS (ร้านมาก่อน)',
              'ใช้กับ URL pay และ API inline; งวดแรกของ subscription เสมอ 3DS',
              'UI/API เป็น ICOPAY เสมอ ไม่โชว์ชื่อ PG ปฏิบัติการ'
            ]
          },
          {
            title: '2) API สมัครสมาชิกรวม',
            steps: [
              'เปิดที่ HQ + ผูก PG; ร้านเปิดใช้ subscription',
              'prepare → embed-checkout-subscribe → status/cancel แล้ว MIT',
              'ถ้าปิด จะไม่แสดงเอกสาร subscription'
            ]
          },
          {
            title: '3) เอกสารตามฟีเจอร์',
            steps: [
              'หน้า Merchant API แสดงเฉพาะฟีเจอร์ที่เปิด (สมัคร/แชทบอท/แบ่งงวด)',
              'อย่าแจกคู่มือฟีเจอร์ที่ร้านไม่ได้ใช้'
            ]
          },
          {
            title: '4) Deploy ผลิต',
            steps: [
              'สั่ง「배포해」; ถ้าล้มเหลว อัปโหลด JAR/SQL แล้ว ./restart-pg-app.sh'
            ]
          },
          {
            title: '5) เวอร์ชัน',
            steps: [
              'ดูในแท็บนี้ เมื่อเพิ่ม PG ต้อง minor +0.1'
            ]
          }
        ]
      }
    },
    {
      version: '2.0',
      kind: 'major',
      date: '2026-07',
      items: {
        KO: [
          '관리자 메뉴 허브 통합 — 본사정책·연동·배포 중심 구조 (기존 화면·API·권한 키 유지)',
          'ICOPAY 통합 checkout·가맹 API — pgVendor ICOPAY 중립 경로 (/checkout/{업체코드} 등)',
          '다중 PG 라우팅·통합 결제내역·통합정산 UI 정비',
          'JPAY·ChillPay 등 PG명은 통합 기능에서 제거 — JPAY 전용 기능만 명시적 표기',
          '운영·가맹점 매뉴얼 V2.0 및 5개 언어(i18n) 반영',
          '본사정책 대메뉴명(띄어쓰기 없음)·플랫폼 메뉴를 AI·챗봇 아래로 배치',
          '본사정책 > 플랫폼 > 업데이트 내용 탭 (버전 히스토리)'
        ],
        EN: [
          'Admin menu hub layout — HQ policy & integration/deploy (legacy screens, APIs, permissions preserved)',
          'Unified ICOPAY checkout & merchant API — neutral paths, pgVendor always ICOPAY',
          'Multi-PG routing, integrated payment & settlement UI improvements',
          'PG vendor names removed from unified flows; JPAY-only features explicitly labeled',
          'Operator & merchant manuals V2.0 with 5-language i18n',
          'HQ Policy menu label (본사정책); Platform hub placed below AI & Chatbot',
          'HQ Policy > Platform > Release notes tab'
        ],
        JP: [
          '管理メニューハブ統合 — 本社ポリシー・連携/デプロイ中心（既存画面・API・権限キー維持）',
          'ICOPAY統合checkout・加盟店API — 中立パス、pgVendorは常にICOPAY',
          'マルチPGルーティング・統合決済・統合精算UI改善',
          '統合機能からPG名を削除 — JPAY専用機能のみ明示',
          '運用・加盟店マニュアル V2.0 · 5言語i18n',
          '本社ポリシー表記・プラットフォームをAI・チャットボットの下に配置',
          '本社ポリシー > プラットフォーム > アップデート内容タブ'
        ],
        CH: [
          '管理员菜单 hub 整合 — 总部政策·联动/部署（保留原有画面·API·权限键）',
          'ICOPAY 统一 checkout 与商户 API — 中性路径，pgVendor 恒为 ICOPAY',
          '多 PG 路由·整合支付·整合结算 UI 优化',
          '统一流程去除 PG 名称 — 仅 JPAY 专用功能显式标注',
          '运营·商户手册 V2.0 · 五语 i18n',
          '总部政策菜单名·平台置于 AI 与聊天机器人下方',
          '总部政策 > 平台 > 更新内容标签页'
        ],
        TH: [
          'รวมเมนู hub — นโยบาย HQ·เชื่อมต่อ/ใช้งานจริง (คงหน้าจอ·API·สิทธิ์เดิม)',
          'ICOPAY checkout รวม·Merchant API — เส้นทางกลาง pgVendor เป็น ICOPAY',
          'Multi-PG routing·รายการชำระรวม·UI ชำระรวม',
          'เอา PG name ออกจาก flow รวม — ระบุ JPAY-only ชัดเจน',
          'คู่มือ V2.0 · i18n 5 ภาษา',
          'เมนูนโยบาย HQ (본사정책)·วางแพลตฟอร์มใต้ AI & Chatbot',
          'นโยบาย HQ > แพลตฟอร์ม > แท็บประวัติอัปเดต'
        ]
      }
    }
  ];

  function locale() {
    if (global.PG_UI_I18N && typeof global.PG_UI_I18N.getLocale === 'function') {
      return String(global.PG_UI_I18N.getLocale() || 'KO').toUpperCase();
    }
    return 'KO';
  }

  function L(key, ko, en, jp, ch, th) {
    var loc = locale();
    if (loc === 'EN') return en || ko;
    if (loc === 'JP') return jp || ko;
    if (loc === 'CH') return ch || ko;
    if (loc === 'TH') return th || ko;
    return ko;
  }

  function esc(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function kindLabel(kind) {
    if (kind === 'major') {
      return L('major', '메이저', 'Major', 'メジャー', '主版本', 'หลัก');
    }
    return L('minor', '마이너', 'Minor', 'マイナー', '次版本', 'ย่อย');
  }

  function renderHowTo(r, loc) {
    if (!r.howTo) return '';
    var blocks = r.howTo[loc] || r.howTo.KO || [];
    if (!blocks.length) return '';
    var h = '<div class="card-body border-top bg-light-subtle py-3">'
      + '<div class="fw-semibold text-body mb-2">' + esc(L('howto',
        '사용 방법',
        'How to use',
        '使い方',
        '使用方法',
        'วิธีใช้')) + '</div>';
    blocks.forEach(function (b) {
      h += '<div class="mb-3">'
        + '<div class="text-body fw-semibold small mb-1">' + esc(b.title || '') + '</div>'
        + '<ol class="mb-0 ps-3">';
      (b.steps || []).forEach(function (step) {
        h += '<li class="mb-1">' + esc(step) + '</li>';
      });
      h += '</ol></div>';
    });
    h += '</div>';
    return h;
  }

  function renderHtml() {
    var loc = locale();
    var h = '<div class="icopay-release-notes text-muted small">'
      + '<div class="d-flex flex-wrap align-items-center gap-2 mb-3">'
      + '<span class="badge bg-primary fs-6">ICOPAY V' + esc(CURRENT_LIVE) + '</span>'
      + '<span class="text-body">' + esc(L('live', '라이브 버전', 'Live version', 'ライブ版', '正式版', 'เวอร์ชันใช้งาน')) + '</span>'
      + '</div>'
      + '<p class="mb-2">' + esc(L('policy',
        '큰 변화는 메이저 버전(2.0, 3.0…)으로, 소소한 변경은 마이너(2.1, 2.2…)로 기록합니다.',
        'Major releases (2.0, 3.0…) for large changes; minor releases (2.1, 2.2…) for small updates.',
        '大きな変更はメジャー(2.0, 3.0…)、小さな変更はマイナー(2.1, 2.2…)で記録します。',
        '重大变更用主版本(2.0、3.0…)，小变更用次版本(2.1、2.2…)。',
        'การเปลี่ยนแปลงใหญ่เป็น major (2.0, 3.0…) การเปลี่ยนเล็กน้อยเป็น minor (2.1, 2.2…)')) + '</p>'
      + '<p class="mb-3 text-body">' + esc(L('pgBump',
        '결제대행사(PG)를 새로 추가·연동할 때는 반드시 마이너 버전을 +0.1 합니다. (예: 2.2 → 2.3). 본 탭에 릴리스 노트·사용 방법을 함께 기록합니다.',
        'When adding or integrating a new payment gateway (PG), always bump the minor version by +0.1 (e.g. 2.2 → 2.3) and record notes and how-to here.',
        '決済代行(PG)を新規追加・連携するときは必ずマイナーを +0.1（例: 2.2 → 2.3）し、本タブに記録します。',
        '新增或对接支付通道(PG)时，必须将次版本 +0.1（例: 2.2 → 2.3），并在本页记录。',
        'เมื่อเพิ่ม/เชื่อม PG ใหม่ ต้องขึ้น minor +0.1 เสมอ (เช่น 2.2 → 2.3) และบันทึกในแท็บนี้')) + '</p>';

    RELEASES.forEach(function (r) {
      var items = (r.items && r.items[loc]) || r.items.KO || [];
      h += '<div class="card mb-3 border-secondary-subtle"><div class="card-header py-2 d-flex flex-wrap align-items-center gap-2">'
        + '<strong class="text-body">V' + esc(r.version) + '</strong>'
        + '<span class="badge bg-secondary-subtle text-secondary">' + esc(kindLabel(r.kind)) + '</span>'
        + '<span class="text-muted">' + esc(r.date) + '</span></div><ul class="list-group list-group-flush">';
      items.forEach(function (line) {
        h += '<li class="list-group-item py-2 small">' + esc(line) + '</li>';
      });
      h += '</ul>' + renderHowTo(r, loc) + '</div>';
    });
    h += '</div>';
    return h;
  }

  function mount(root) {
    if (!root) return;
    root.innerHTML = renderHtml();
  }

  global.ICOPAY_PLATFORM_RELEASE = {
    currentLiveVersion: CURRENT_LIVE,
    releases: RELEASES,
    renderHtml: renderHtml,
    mount: mount
  };
}(typeof window !== 'undefined' ? window : this));
