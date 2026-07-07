/**
 * ICOPAY 라이브 버전 · 릴리스 노트 (본사정책 > 플랫폼 > 업데이트 내용).
 * 큰 변화: 메이저(2.0, 3.0) · 소소한 변경: 마이너(2.1, 2.2).
 */
(function (global) {
  'use strict';

  var CURRENT_LIVE = '2.0';

  /** @type {Array<{version:string,kind:string,date:string,items:{KO:string[],EN:string[],JP:string[],CH:string[],TH:string[]}}>} */
  var RELEASES = [
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

  function renderHtml() {
    var loc = locale();
    var h = '<div class="icopay-release-notes text-muted small">'
      + '<div class="d-flex flex-wrap align-items-center gap-2 mb-3">'
      + '<span class="badge bg-primary fs-6">ICOPAY V' + esc(CURRENT_LIVE) + '</span>'
      + '<span class="text-body">' + esc(L('live', '라이브 버전', 'Live version', 'ライブ版', '正式版', 'เวอร์ชันใช้งาน')) + '</span>'
      + '</div>'
      + '<p class="mb-3">' + esc(L('policy',
        '큰 변화는 메이저 버전(2.0, 3.0…)으로, 소소한 변경은 마이너(2.1, 2.2…)로 기록합니다.',
        'Major releases (2.0, 3.0…) for large changes; minor releases (2.1, 2.2…) for small updates.',
        '大きな変更はメジャー(2.0, 3.0…)、小さな変更はマイナー(2.1, 2.2…)で記録します。',
        '重大变更用主版本(2.0、3.0…)，小变更用次版本(2.1、2.2…)。',
        'การเปลี่ยนแปลงใหญ่เป็น major (2.0, 3.0…) การเปลี่ยนเล็กน้อยเป็น minor (2.1, 2.2…)')) + '</p>';

    RELEASES.forEach(function (r) {
      var items = (r.items && r.items[loc]) || r.items.KO || [];
      h += '<div class="card mb-3 border-secondary-subtle"><div class="card-header py-2 d-flex flex-wrap align-items-center gap-2">'
        + '<strong class="text-body">V' + esc(r.version) + '</strong>'
        + '<span class="badge bg-secondary-subtle text-secondary">' + esc(kindLabel(r.kind)) + '</span>'
        + '<span class="text-muted">' + esc(r.date) + '</span></div><ul class="list-group list-group-flush">';
      items.forEach(function (line) {
        h += '<li class="list-group-item py-2 small">' + esc(line) + '</li>';
      });
      h += '</ul></div>';
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
