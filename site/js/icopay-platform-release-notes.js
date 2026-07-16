/**
 * ICOPAY 라이브 버전 · 릴리스 노트 (본사정책 > 플랫폼 > 업데이트 내용).
 * 큰 변화: 메이저(2.0, 3.0) · 소소한 변경: 마이너(2.1, 2.2…).
 * 결제대행사(PG) 신규 추가·연동 시: 반드시 마이너 +0.1 (예: 2.2 → 2.3).
 */
(function (global) {
  'use strict';

  var CURRENT_LIVE = '2.2';

  /**
   * howTo: { KO|EN|JP|CH|TH: Array<{ title:string, steps:string[] }> }
   * @type {Array<{version:string,kind:string,date:string,items:object,howTo?:object}>}
   */
  var RELEASES = [
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
