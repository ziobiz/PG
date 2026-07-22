/**
 * ICOPAY 라이브 버전 · 릴리스 노트 (본사정책 > 플랫폼 > 업데이트 내용).
 * 큰 변화: 메이저(2.0, 3.0) · 소소한 변경: 마이너(2.1, 2.2…).
 * 결제대행사(PG) 신규 추가·연동 시: 반드시 마이너 +0.1 (예: 2.2 → 2.3).
 */
(function (global) {
  'use strict';

  var CURRENT_LIVE = '2.54';

  /**
   * howTo: { KO|EN|JP|CH|TH: Array<{ title:string, steps:string[] }> }
   * @type {Array<{version:string,kind:string,date:string,items:object,howTo?:object}>}
   */
  var RELEASES = [
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
