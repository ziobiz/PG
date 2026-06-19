/**
 * 화면 문자열 다국어 — PG_UI_STRING_MAP(자동 생성) + STATIC 보강.
 * 로케일은 PG_PAY_LIST_I18N 과 동일(localStorage pg_pay_list_ui_locale).
 */
(function (g) {
  'use strict';

  var LOCALE_KEY = 'pg_pay_list_ui_locale';
  var USER_SET_KEY = 'pg_pay_list_ui_locale_user_set';

  var STATIC = {
    '화면 정보가 없습니다.': {
      EN: 'No screen definition for this URL.',
      JP: 'このURLの画面定義がありません。',
      CH: '此地址没有对应的界面定义。',
      TH: 'ไม่มีคำจำกัดความหน้าจอสำหรับ URL นี้'
    },
    '헬로': { EN: 'Hello', JP: 'Hello', CH: '提示', TH: 'Hello' },
    '태블릿': { EN: 'Tablet', JP: 'タブレット', CH: '平板', TH: 'แท็บเล็ต' },
    '태블릿 UI 전환': { EN: 'Toggle tablet UI', JP: 'タブレットUI切替', CH: '切换平板界面', TH: 'สลับ UI แท็บเล็ต' },
    '태블릿 UI 기능': { EN: 'Tablet UI feature', JP: 'タブレットUI機能', CH: '平板UI功能', TH: 'ฟีเจอร์ UI แท็บเล็ต' },
    '태블릿설정에서 이 조직 단계에 노출되지 않은 메뉴입니다. 접근불가(NONE)만 가능합니다.': {
      EN: 'This menu is not exposed for this org level in Tablet settings. Only NONE is allowed.',
      JP: 'タブレット設定でこの組織段階に表示されていないメニューです。アクセス不可(NONE)のみ可能です。',
      CH: '此菜单未在该组织级别的平板设置中开放，仅可为不可访问(NONE)。',
      TH: 'เมนูนี้ไม่เปิดในการตั้งค่าแท็บเล็ตสำหรับระดับองค์กรนี้ ใช้ได้เฉพาะ NONE'
    },
    '태블릿설정 미노출 — 접근불가 고정': {
      EN: 'Not in Tablet settings — fixed to no access',
      JP: 'タブレット設定で非表示 — アクセス不可固定',
      CH: '未在平板设置中开放 — 固定为不可访问',
      TH: 'ไม่เปิดในการตั้งค่าแท็บเล็ต — ปิดการเข้าถึง'
    },
    '태블릿설정 안내': {
      EN: 'Check menus for each organization column (headquarters through merchant). Tablet mode shows only menus enabled here and allowed in HQ permissions. Only HEADQUARTERS or ADMIN can save.',
      JP: '組織列（総本社〜加盟店）ごとにタブレットで使うメニューにチェックします。タブレットモードでは本社権限とここでONのメニューのみ表示。保存は総本社またはADMINのみ。',
      CH: '在各组织列（总部至商户）勾选平板可用菜单。开启平板模式后仅显示此处勾选且总部权限允许的菜单。仅总部或 ADMIN 可保存。',
      TH: 'ติ๊กเมนูตามคอลัมน์องค์กร (สำนักงานใหญ่ถึงร้านค้า) โหมดแท็บเล็ตแสดงเมนูที่เปิดที่นี่และสิทธิ์ HQ อนุญาต บันทึกได้เฉพาะสำนักงานใหญ่หรือ ADMIN'
    },
    '태블릿 모드에서 사용할 메뉴가 없습니다. 본사권한설정·태블릿설정을 확인하세요.': {
      EN: 'No menus are available for tablet mode. Check HQ permissions and Tablet settings.',
      JP: 'タブレットモードで使えるメニューがありません。本社権限設定・タブレット設定を確認してください。',
      CH: '没有可在平板模式下使用的菜单。请检查总部权限与平板设置。',
      TH: 'ไม่มีเมนูสำหรับโหมดแท็บเล็ต ตรวจสอบสิทธิ์ HQ และการตั้งค่าแท็บเล็ต'
    },
    '태블릿 메인 — 아이콘을 눌러 화면을 여세요.': {
      EN: 'Tablet home — tap an icon to open a screen.',
      JP: 'タブレットホーム — アイコンをタップして画面を開きます。',
      CH: '平板主页 — 点图标打开对应界面。',
      TH: 'หน้าแรกแท็บเล็ต — แตะไอคอนเพื่อเปิดหน้าจอ'
    },
    '태블릿 메뉴': {
      EN: 'Tablet menu',
      JP: 'タブレットメニュー',
      CH: '平板菜单',
      TH: 'เมนูแท็บเล็ต'
    },
    '아이콘을 눌러 업무 화면을 여세요.': {
      EN: 'Tap an icon to open a work screen.',
      JP: 'アイコンをタップして業務画面を開きます。',
      CH: '点击图标打开业务画面。',
      TH: 'แตะไอคอนเพื่อเปิดหน้าจอทำงาน'
    },
    '사용 가능한 메뉴가 없습니다': {
      EN: 'No menus available',
      JP: '利用できるメニューがありません',
      CH: '没有可用菜单',
      TH: 'ไม่มีเมนูที่ใช้ได้'
    },
    '나의 결제': {
      EN: 'My payments',
      JP: 'マイ決済',
      CH: '我的支付',
      TH: 'การชำระของฉัน'
    },
    '(별도 계약 시 지원 되는 서비스입니다,)': {
      EN: '(Available with a separate contract.)',
      JP: '（別途契約時にご利用いただけます。）',
      CH: '（需另行签约后提供。）',
      TH: '(ให้บริการเมื่อทำสัญญาแยก)'
    },
    '업무 메뉴': {
      EN: 'Work menus',
      JP: '業務メニュー',
      CH: '业务菜单',
      TH: 'เมนูงาน'
    },
    'URL 결제 가기': {
      EN: 'Open URL pay',
      JP: 'URL決済を開く',
      CH: '打开URL支付',
      TH: 'เปิด URL ชำระ'
    },
    'URL 결제 복사': {
      EN: 'Copy URL pay link',
      JP: 'URL決済をコピー',
      CH: '复制URL支付链接',
      TH: 'คัดลอกลิงก์ URL ชำระ'
    },
    '챗봇결제 가기': {
      EN: 'Open chatbot pay',
      JP: 'チャットボット決済を開く',
      CH: '打开聊天机器人支付',
      TH: 'เปิดชำระแชทบอท'
    },
    '챗봇결제 복사': {
      EN: 'Copy chatbot pay link',
      JP: 'チャットボット決済をコピー',
      CH: '复制聊天机器人支付链接',
      TH: 'คัดลอกลิงก์ชำระแชทบอท'
    },
    '복사되었습니다.': {
      EN: 'Copied.',
      JP: 'コピーしました。',
      CH: '已复制。',
      TH: 'คัดลอกแล้ว'
    },
    '복사 실패': {
      EN: 'Copy failed',
      JP: 'コピーに失敗しました',
      CH: '复制失败',
      TH: 'คัดลอกไม่สำเร็จ'
    },
    '해당 결제 기능이 「사용」으로 설정되어 있지 않습니다. 업체정보에서 웹결제·챗봇결제 사용여부를 확인하세요.': {
      EN: 'This payment feature is not enabled. Check web pay and chatbot pay in company info.',
      JP: 'この決済機能は「使用」になっていません。会社情報のウェブ決済・チャットボット決済を確認してください。',
      CH: '该支付功能未设为「使用」。请在企业信息中确认网页支付与聊天机器人支付。',
      TH: 'ฟีเจอร์ชำระนี้ยังไม่ได้เปิดใช้ ตรวจสอบการชำระเว็บและแชทบอทในข้อมูลร้าน'
    },
    '일별결제': {
      EN: 'Daily payments',
      JP: '日別決済',
      CH: '按日支付',
      TH: 'ชำระรายวัน'
    },
    '일별수수료': {
      EN: 'Daily fees',
      JP: '日別手数料',
      CH: '按日手续费',
      TH: 'ค่าธรรมเนียมรายวัน'
    },
    '게이트웨이 시간 초과(HTTP {0}). 조회 기간을 줄인 뒤 [검색]을 다시 시도해 주세요.': {
      EN: 'Gateway timeout (HTTP {0}). Narrow the date range and click [Search] again.',
      JP: 'ゲートウェイタイムアウト(HTTP {0})。期間を短くして[検索]を再実行してください。',
      CH: '网关超时(HTTP {0})。请缩短查询期间后再次点击【搜索】。',
      TH: 'Gateway timeout (HTTP {0}) ลดช่วงวันที่แล้วกด [ค้นหา] อีกครั้ง'
    },
    '일별수수료 집계 시간이 초과되었습니다. 조회 기간을 7일 이내로 줄인 뒤 [검색]을 다시 눌러 주세요.': {
      EN: 'Daily fee aggregation timed out. Narrow the range to 7 days or less, then click [Search] again.',
      JP: '日別手数料の集計がタイムアウトしました。7日以内に期間を短くして[検索]を押してください。',
      CH: '按日手续费汇总超时。请将查询期间缩短至7天内后再次点击【搜索】。',
      TH: 'รวมค่าธรรมเนียมรายวันหมดเวลา ลดช่วงไม่เกิน 7 วันแล้วกด [ค้นหา] อีกครั้ง'
    },
    '집계 대상 건수가 많아 일부만 반영되었을 수 있습니다. 기간을 줄이거나 feeList에서 상세 조회하세요.': {
      EN: 'Too many rows to aggregate; totals may be partial. Narrow the date range or use the fee list for details.',
      JP: '集計対象が多いため一部のみ反映された可能性があります。期間を短くするか手数料一覧で詳細を確認してください。',
      CH: '汇总数据量过大，可能仅反映部分结果。请缩短期间或在手续费列表中查看明细。',
      TH: 'ข้อมูลมากเกินไป อาจสะท้อนเพียงบางส่วน ลดช่วงวันที่หรือดูรายละเอียดในรายการค่าธรรมเนียม'
    },
    '집계 대상 건수가 많아 일부만 반영되었을 수 있습니다. 기간을 줄이거나 목록에서 상세 조회하세요.': {
      EN: 'Too many rows to aggregate; totals may be partial. Narrow the date range or open the list for details.',
      JP: '集計対象が多いため一部のみ反映された可能性があります。期間を短くするか一覧で詳細を確認してください。',
      CH: '汇总数据量过大，可能仅反映部分结果。请缩短期间或在列表中查看明细。',
      TH: 'ข้อมูลมากเกินไป อาจสะท้อนเพียงบางส่วน ลดช่วงวันที่หรือดูรายละเอียดในรายการ'
    },
    '해당 일자 거래가 많아 상세 목록이 일부만 표시됩니다.': {
      EN: 'Too many transactions for this day; only part of the detail list is shown.',
      JP: '当該日の取引が多いため、詳細一覧の一部のみ表示しています。',
      CH: '该日交易过多，详情列表仅显示部分。',
      TH: 'รายการวันนี้มากเกินไป แสดงรายละเอียดได้เพียงบางส่วน'
    },
    '화면 모드 (변경 시에만 선택)': {
      EN: 'Display mode (select only if changing)',
      JP: '表示モード（変更する場合のみ選択）',
      CH: '显示模式（仅在需要更改时选择）',
      TH: 'โหมดแสดงผล (เลือกเฉพาะเมื่อต้องการเปลี่ยน)'
    },
    '최근 로그인과 동일 (기본)': {
      EN: 'Same as last sign-in (default)',
      JP: '前回ログインと同じ（既定）',
      CH: '与上次登录相同（默认）',
      TH: 'เหมือนการเข้าล่าสุด (ค่าเริ่มต้น)'
    },
    '이번만 태블릿 보드': {
      EN: 'Tablet board — this sign-in only',
      JP: '今回のみタブレットボード',
      CH: '仅本次使用平板看板',
      TH: 'บอร์ดแท็บเล็ต — เฉพาะครั้งนี้'
    },
    '이번만 일반 화면': {
      EN: 'Standard layout — this sign-in only',
      JP: '今回のみ通常画面',
      CH: '仅本次使用标准界面',
      TH: 'หน้าจอมาตรฐาน — เฉพาะครั้งนี้'
    },
    '이번 로그인만 태블릿 보드 UI로 시작합니다.': {
      EN: 'This sign-in will start with the tablet board UI.',
      JP: '今回のログインのみタブレットボードUIで開始します。',
      CH: '仅本次登录以平板看板界面开始。',
      TH: 'การเข้าครั้งนี้จะเริ่มด้วย UI บอร์ดแท็บเล็ต'
    },
    '이번 로그인만 일반(좌측 메뉴) 화면으로 시작합니다.': {
      EN: 'This sign-in will start with the standard layout.',
      JP: '今回のログインのみ通常（左メニュー）画面で開始します。',
      CH: '仅本次登录以标准（左侧菜单）布局开始。',
      TH: 'การเข้าครั้งนี้จะเริ่มด้วยเลย์เอาต์มาตรฐาน'
    },
    '기본: 최근에 태블릿 UI로 접속했습니다. 바꾸려면 아래에서 선택하세요.': {
      EN: 'Default: your last sign-in used tablet UI. Change below only if needed.',
      JP: '既定：前回はタブレットUIでした。変更する場合のみ下から選んでください。',
      CH: '默认：上次为平板界面。仅在需要更改时于下方选择。',
      TH: 'ค่าเริ่มต้น: ครั้งก่อนใช้ UI แท็บเล็ต เปลี่ยนเฉพาะเมื่อจำเป็น'
    },
    '기본: 최근에 일반 화면으로 접속했습니다. 바꾸려면 아래에서 선택하세요.': {
      EN: 'Default: your last sign-in used the standard layout. Change below only if needed.',
      JP: '既定：前回は通常画面でした。変更する場合のみ下から選んでください。',
      CH: '默认：上次为标准界面。仅在需要更改时于下方选择。',
      TH: 'ค่าเริ่มต้น: ครั้งก่อนใช้หน้าจอมาตรฐาน เปลี่ยนเฉพาะเมื่อจำเป็น'
    },
    '첫 로그인: 기기에 따라 태블릿 또는 일반으로 시작합니다. 이후에는 최근 접속 방식이 기본입니다.': {
      EN: 'First sign-in: starts by device hint; later visits default to your last mode.',
      JP: '初回ログイン：端末に応じてタブレットまたは通常で開始します。以降は直近の接続方式が既定です。',
      CH: '首次登录：按设备提示进入平板或标准界面；之后默认沿用上次方式。',
      TH: 'เข้าครั้งแรก: เริ่มตามอุปกรณ์ ครั้งถัดไปใช้โหมดล่าสุดเป็นค่าเริ่มต้น'
    },
    /* login.html — 로그인 첫 화면 */
    '사칭 피해 주의 안내': {
      EN: 'Impersonation scam alert',
      JP: 'なりすまし被害にご注意',
      CH: '谨防冒充诈骗',
      TH: 'คำเตือนการแอบอ้างตัวตน'
    },
    '최근 본사나 PG사를 사칭해 금전적 요구를 하는 사례가 발생하고 있습니다.': {
      EN: 'There have been cases of people impersonating our company or a PG provider to request money.',
      JP: '最近、当社やPG会社を装って金銭を要求する事例が発生しています。',
      CH: '近期出现冒充本公司或PG公司要求汇款的情况。',
      TH: 'มีกรณีแอบอ้างเป็นบริษัทหรือ PG เพื่อเรียกรับเงิน'
    },
    '저희는 결제와 관련한 금전을 별도로 요청하지 않습니다. 의심스러운 연락을 받으셨다면 고객센터 또는 계약된 영업지사로 사실 여부를 반드시 확인해 주시기 바랍니다.': {
      KO: '저희는 결제와 관련한 금전을 별도로 요청하지 않습니다. 의심스러운 연락을 받으셨다면 <strong>고객센터</strong> 또는 <strong>계약된 영업지사</strong>로 사실 여부를 반드시 확인해 주시기 바랍니다.',
      EN: 'We never ask for money separately for payments. If you receive suspicious contact, please verify with <strong>customer service</strong> or your <strong>contracted sales office</strong>.',
      JP: '当社は決済に関して別途金銭を請求することはありません。不審な連絡があった場合は、<strong>カスタマーセンター</strong>または<strong>契約営業支店</strong>で事実関係を必ずご確認ください。',
      CH: '我们不会就支付另行索要款项。如收到可疑联系，请务必向<strong>客服中心</strong>或<strong>签约营业网点</strong>核实。',
      TH: 'เราไม่เรียกรับเงินแยกจากการชำระเงิน หากได้รับการติดต่อที่น่าสงสัย โปรดตรวจสอบกับ<strong>ศูนย์บริการลูกค้า</strong>หรือ<strong>สาขาขายที่ทำสัญญา</strong>'
    },
    '아이디': {
      EN: 'User ID',
      JP: 'ログインID',
      CH: '登录账号',
      TH: 'รหัสผู้ใช้'
    },
    '패스워드': {
      EN: 'Password',
      JP: 'パスワード',
      CH: '密码',
      TH: 'รหัสผ่าน'
    },
    '로그인': {
      EN: 'Sign in',
      JP: 'ログイン',
      CH: '登录',
      TH: 'เข้าสู่ระบบ'
    },
    '로그인 안내 호스트:': {
      EN: 'Login host:',
      JP: 'ログインホスト:',
      CH: '登录主机:',
      TH: 'โฮสต์เข้าสู่ระบบ:'
    },
    '보안 정책에 따라 새 비밀번호를 설정해야 합니다. 방금 입력하신 비밀번호는 확인용으로만 사용되며, 아래에 새 비밀번호(8자 이상)를 입력해 주세요.': {
      KO: '보안 정책에 따라 <strong>새 비밀번호</strong>를 설정해야 합니다. 방금 입력하신 비밀번호는 확인용으로만 사용되며, 아래에 새 비밀번호(8자 이상)를 입력해 주세요.',
      EN: 'Per security policy you must set a <strong>new password</strong>. The password you just entered is used for verification only; enter a new password (8+ characters) below.',
      JP: 'セキュリティポリシーにより<strong>新しいパスワード</strong>の設定が必要です。直前に入力したパスワードは確認用のみです。以下に新しいパスワード（8文字以上）を入力してください。',
      CH: '根据安全策略，您必须设置<strong>新密码</strong>。刚才输入的密码仅用于验证；请在下方输入新密码（至少8位）。',
      TH: 'ตามนโยบายความปลอดภัย ต้องตั้ง<strong>รหัสผ่านใหม่</strong> รหัสที่เพิ่งกรอกใช้เพื่อยืนยันเท่านั้น กรอกรหัสใหม่ (อย่างน้อย 8 ตัว) ด้านล่าง'
    },
    '새 비밀번호': {
      EN: 'New password',
      JP: '新しいパスワード',
      CH: '新密码',
      TH: 'รหัสผ่านใหม่'
    },
    '새 비밀번호 확인': {
      EN: 'Confirm new password',
      JP: '新しいパスワード（確認）',
      CH: '确认新密码',
      TH: 'ยืนยันรหัสผ่านใหม่'
    },
    '비밀번호 설정 후 입장': {
      EN: 'Set password and continue',
      JP: 'パスワード設定後に入場',
      CH: '设置密码并进入',
      TH: 'ตั้งรหัสผ่านแล้วเข้าใช้'
    },
    '변경 비밀번호': {
      EN: 'Change password',
      JP: '変更パスワード',
      CH: '变更密码',
      TH: 'เปลี่ยนรหัสผ่าน'
    },
    '인증번호': {
      EN: 'Verification code',
      JP: '認証番号',
      CH: '验证码',
      TH: 'รหัสยืนยัน'
    },
    'OTP번호': {
      EN: 'OTP code',
      JP: 'OTP番号',
      CH: 'OTP号码',
      TH: 'รหัส OTP'
    },
    'Google OTP 등록': {
      EN: 'Register Google OTP',
      JP: 'Google OTP 登録',
      CH: '注册 Google OTP',
      TH: 'ลงทะเบียน Google OTP'
    },
    '아이디·비밀번호 확인 후 이 화면에서 Google Authenticator(OTP)를 등록합니다. 구성용 이메일 인증번호는 서버에 설정된 수신처로 발송됩니다.': {
      KO: '아이디·비밀번호 확인 후 이 화면에서 Google Authenticator(OTP)를 등록합니다. 구성용 <strong>이메일 인증번호</strong>는 서버에 설정된 수신처로 발송됩니다.',
      EN: 'After verifying your user ID and password, register Google Authenticator (OTP) on this screen. The setup <strong>email verification code</strong> is sent to the address configured on the server.',
      JP: 'ログインID・パスワード確認後、この画面で Google Authenticator（OTP）を登録します。設定用の<strong>メール認証番号</strong>はサーバーに設定された宛先へ送信されます。',
      CH: '验证登录账号和密码后，在此界面注册 Google Authenticator (OTP)。配置用<strong>邮件验证码</strong>将发送至服务器配置的收件地址。',
      TH: 'หลังยืนยันรหัสผู้ใช้และรหัสผ่าน ลงทะเบียน Google Authenticator (OTP) ในหน้านี้ <strong>รหัสยืนยันทางอีเมล</strong>สำหรับตั้งค่าจะส่งไปยังที่อยู่ที่กำหนดบนเซิร์ฟเวอร์'
    },
    '인증 메일 발송': {
      EN: 'Send verification email',
      JP: '認証メール送信',
      CH: '发送验证邮件',
      TH: 'ส่งอีเมลยืนยัน'
    },
    '이메일 인증번호 (6자리)': {
      EN: 'Email verification code (6 digits)',
      JP: 'メール認証番号（6桁）',
      CH: '邮件验证码（6位）',
      TH: 'รหัสยืนยันอีเมล (6 หลัก)'
    },
    '인증 확인': {
      EN: 'Verify',
      JP: '認証確認',
      CH: '验证确认',
      TH: 'ยืนยัน'
    },
    'Google Authenticator에서 설정 키 입력으로 아래 시크릿을 등록하세요.': {
      KO: 'Google Authenticator에서 <strong>설정 키 입력</strong>으로 아래 시크릿을 등록하세요.',
      EN: 'In Google Authenticator, choose <strong>Enter a setup key</strong> and register the secret below.',
      JP: 'Google Authenticator で<strong>設定キーの入力</strong>を選び、以下のシークレットを登録してください。',
      CH: '在 Google Authenticator 中选择<strong>输入设置密钥</strong>并注册下方密钥。',
      TH: 'ใน Google Authenticator เลือก<strong>ป้อนคีย์ตั้งค่า</strong>แล้วลงทะเบียนคีย์ด้านล่าง'
    },
    'otpauth URI (앱에서 스캔·붙여넣기 가능)': {
      EN: 'otpauth URI (scan or paste in app)',
      JP: 'otpauth URI（アプリでスキャン・貼り付け可）',
      CH: 'otpauth URI（可在应用中扫描或粘贴）',
      TH: 'otpauth URI (สแกนหรือวางในแอปได้)'
    },
    '앱에 표시된 6자리 코드': {
      EN: '6-digit code shown in app',
      JP: 'アプリに表示された6桁コード',
      CH: '应用中显示的6位代码',
      TH: 'รหัส 6 หลักที่แสดงในแอป'
    },
    '등록 완료': {
      EN: 'Complete registration',
      JP: '登録完了',
      CH: '完成注册',
      TH: 'ลงทะเบียนเสร็จ'
    },
    '확인': {
      EN: 'OK',
      JP: '確認',
      CH: '确定',
      TH: 'ตกลง'
    },
    '아이디와 비밀번호를 입력하세요.': {
      EN: 'Enter user ID and password.',
      JP: 'ログインIDとパスワードを入力してください。',
      CH: '请输入登录账号和密码。',
      TH: 'กรอกรหัสผู้ใช้และรหัสผ่าน'
    },
    '로그인에 실패했습니다.': {
      EN: 'Sign-in failed.',
      JP: 'ログインに失敗しました。',
      CH: '登录失败。',
      TH: 'เข้าสู่ระบบไม่สำเร็จ'
    },
    '로그인 요청이 실패했습니다. API 서버를 확인하세요.': {
      EN: 'Sign-in request failed. Check the API server.',
      JP: 'ログイン要求に失敗しました。APIサーバーを確認してください。',
      CH: '登录请求失败。请检查 API 服务器。',
      TH: 'คำขอเข้าสู่ระบบล้มเหลว ตรวจสอบเซิร์ฟเวอร์ API'
    },
    '8자 이상': { EN: '8+ characters', JP: '8文字以上', CH: '至少8位', TH: 'อย่างน้อย 8 ตัว' },
    /* /main — 홈 대시보드(메인) */
    '오늘': { EN: 'Today', JP: '今日', CH: '今天', TH: 'วันนี้' },
    '최근 7일': { EN: 'Last 7 days', JP: '直近7日', CH: '最近7天', TH: '7 วันที่ผ่านมา' },
    '최근 30일': { EN: 'Last 30 days', JP: '直近30日', CH: '最近30天', TH: '30 วันที่ผ่านมา' },
    '통화별': { EN: 'By currency', JP: '通貨別', CH: '按币种', TH: 'ตามสกุลเงิน' },
    '해당 기간 거래가 없습니다.': {
      EN: 'No transactions in this period.',
      JP: 'この期間の取引はありません。',
      CH: '此期间没有交易。',
      TH: 'ไม่มีธุรกรรมในช่วงเวลานี้'
    },
    '서버 트래픽 요약을 사용할 수 없습니다.': {
      EN: 'Server traffic summary is unavailable.',
      JP: 'サーバートラフィック要約を利用できません。',
      CH: '无法使用服务器流量摘要。',
      TH: 'ไม่สามารถใช้สรุปทราฟฟิกของเซิร์ฟเวอร์ได้'
    },
    '금일 트래픽 약': { EN: 'Today traffic approx.', JP: '本日のトラフィック約', CH: '今日流量约', TH: 'ทราฟฟิกวันนี้ประมาณ' },
    '최근 7일 누적 약': { EN: 'Last 7 days total approx.', JP: '直近7日累計約', CH: '最近7天累计约', TH: 'รวม 7 วันประมาณ' },
    '메모리 피크': { EN: 'Memory peak', JP: 'メモリピーク', CH: '内存峰值', TH: 'หน่วยความจำพีค' },
    '수집된 서버 사용량 데이터가 없습니다.': {
      EN: 'No server usage data collected.',
      JP: '収集されたサーバー使用量データがありません。',
      CH: '没有收集到服务器使用量数据。',
      TH: 'ไม่มีข้อมูลการใช้งานเซิร์ฟเวอร์ที่ถูกเก็บรวบรวม'
    },
    '표시할 정산 실행 이력이 없습니다.': {
      EN: 'No settlement run history to display.',
      JP: '表示する精算実行履歴がありません。',
      CH: '没有可显示的结算执行记录。',
      TH: 'ไม่มีประวัติการรันชำระเงินให้แสดง'
    },
    '최근 7일 승인 금액 합': { EN: 'Approved amount sum (last 7 days)', JP: '直近7日 承認金額合計', CH: '最近7天批准金额合计', TH: 'ยอดอนุมัติรวม (7 วันล่าสุด)' },
    '통화 혼합·참고': { EN: 'Mixed currencies (reference)', JP: '通貨混合・参考', CH: '混合币种（参考）', TH: 'สกุลเงินผสม (อ้างอิง)' },
    '최근 7일 승인 건수': { EN: 'Approved count (last 7 days)', JP: '直近7日 承認件数', CH: '最近7天批准笔数', TH: 'จำนวนอนุมัติ (7 วันล่าสุด)' },
    '최근 7일 전체 거래 건수': { EN: 'Total transactions (last 7 days)', JP: '直近7日 取引総件数', CH: '最近7天交易总笔数', TH: 'ธุรกรรมทั้งหมด (7 วันล่าสุด)' },
    '조직 스냅샷': { EN: 'Org snapshot', JP: '組織スナップショット', CH: '组织快照', TH: 'สแนปช็อตองค์กร' },
    '소속 트리': { EN: 'Hierarchy', JP: '所属ツリー', CH: '所属层级', TH: 'ลำดับชั้น' },
    '가맹점 조직 수': { EN: 'Merchant org count', JP: '加盟店組織数', CH: '商户组织数', TH: 'จำนวนองค์กรร้านค้า' },
    '추이 데이터가 없습니다.': { EN: 'No trend data.', JP: '推移データがありません。', CH: '没有趋势数据。', TH: 'ไม่มีข้อมูลแนวโน้ม' },
    '7일 승인 금액 추이': { EN: '7-day approved amount trend', JP: '7日 承認金額推移', CH: '7天批准金额趋势', TH: 'แนวโน้มยอดอนุมัติ 7 วัน' },
    '일별': { EN: 'Daily', JP: '日別', CH: '按日', TH: 'รายวัน' },
    '최근 30일 거래 상태 믹스': { EN: 'Transaction status mix (last 30 days)', JP: '直近30日 取引ステータス構成', CH: '最近30天交易状态构成', TH: 'สัดส่วนสถานะธุรกรรม (30 วันล่าสุด)' },
    '무효계': { EN: 'Void family', JP: '無効系', CH: '无效类', TH: 'กลุ่มโมฆะ' },
    '업무 바로가기': { EN: 'Quick links', JP: '業務ショートカット', CH: '快捷入口', TH: 'ลิงก์ด่วน' },
    '생성': { EN: 'Created', JP: '作成', CH: '创建', TH: 'สร้าง' },
    '가맹': { EN: 'Merchant', JP: '加盟店', CH: '商户', TH: 'ร้านค้า' },
    '배포': { EN: 'Publish', JP: '配布', CH: '发布', TH: 'เผยแพร่' },
    '최근 정산 실행이 없습니다.': { EN: 'No recent settlement runs.', JP: '最近の精算実行はありません。', CH: '没有最近的结算执行。', TH: 'ไม่มีการรันชำระเงินล่าสุด' },
    '최근 정산 실행': { EN: 'Recent settlement runs', JP: '最近の精算実行', CH: '最近的结算执行', TH: 'การรันชำระเงินล่าสุด' },
    '정산실행': { EN: 'Run settlement', JP: '精算実行', CH: '执行结算', TH: 'รันชำระเงิน' },
    '엔진': { EN: 'Engine', JP: 'エンジン', CH: '引擎', TH: 'เอนจิน' },
    '거래시각': { EN: 'Txn time', JP: '取引時刻', CH: '交易时间', TH: 'เวลาออเดอร์' },
    '범위': { EN: 'Scope', JP: '範囲', CH: '范围', TH: 'ขอบเขต' },
    '리스크7일': { EN: 'Risk (7d)', JP: 'リスク7日', CH: '风险7天', TH: 'ความเสี่ยง 7 วัน' },
    '비교7일': { EN: 'Compare (7d)', JP: '比較7日', CH: '对比7天', TH: 'เปรียบเทียบ 7 วัน' },
    '의미': { EN: 'Meaning', JP: '意味', CH: '含义', TH: 'ความหมาย' },
    '이번 주 리스크 점수': { EN: 'This week risk score', JP: '今週のリスクスコア', CH: '本周风险分数', TH: 'คะแนนความเสี่ยงสัปดาห์นี้' },
    '지난주 대비': { EN: 'vs last week', JP: '先週比', CH: '较上周', TH: 'เทียบสัปดาห์ก่อน' },
    '직전': { EN: 'prev', JP: '直前', CH: '前值', TH: 'ก่อนหน้า' },
    '실패·무효·환불·취소 가중 합성(규칙)': {
      EN: 'Weighted composite of fail/void/refund/cancel (rules).',
      JP: '失敗・無効・返金・取消の加重合成（ルール）',
      CH: '失败/无效/退款/取消加权合成（规则）',
      TH: 'ถ่วงน้ำหนักจาก ล้มเหลว/โมฆะ/คืนเงิน/ยกเลิก (กฎ)'
    },
    '오늘의 운영 KPI': { EN: "Today's Ops KPI", JP: '本日の運用KPI', CH: '今日运营KPI', TH: 'KPI การปฏิบัติการวันนี้' },
    '어제의 운영 KPI': { EN: "Yesterday's Ops KPI", JP: '昨日の運用KPI', CH: '昨日运营KPI', TH: 'KPI การปฏิบัติการเมื่อวาน' },
    '전일 0시~24시 거래일시': { EN: '00:00–24:00 by txn time', JP: '前日0時～24時（取引時刻）', CH: '按交易时间 0:00–24:00', TH: '00:00–24:00 ตามเวลาออเดอร์' },
    '거래일시 기준 오늘 0시~현재': {
      EN: 'Txn time: today 00:00–now',
      JP: '取引時刻ベース 本日0時～現在',
      CH: '按交易时间：今日0点至今',
      TH: 'ตามเวลาทำรายการ: วันนี้ 00:00–ปัจจุบัน'
    },
    '성공 건수': {
      EN: 'Successful txns (approved)',
      JP: '成功件数（承認）',
      CH: '成功笔数（授权）',
      TH: 'ธุรกรรมสำเร็จ (อนุมัติ)'
    },
    '성공 금액': {
      EN: 'Successful amount',
      JP: '成功金額',
      CH: '成功金额',
      TH: 'ยอดสำเร็จ'
    },
    '통화별 성공 (건수·금액)': {
      EN: 'Success by currency (count & amount)',
      JP: '通貨別の成功（件数・金額）',
      CH: '按币种成功（笔数·金额）',
      TH: 'สำเร็จตามสกุลเงิน (จำนวน·ยอด)'
    },
    '해당 기간 성공(승인) 거래가 없습니다.': {
      EN: 'No successful (approved) transactions in this period.',
      JP: 'この期間に成功（承認）取引はありません。',
      CH: '该时段内没有成功（授权）交易。',
      TH: 'ในช่วงเวลานี้ไม่มีธุรกรรมที่สำเร็จ (อนุมัติ)'
    },
    '상품 등록': {
      EN: 'Product registration',
      JP: '商品登録',
      CH: '商品登记',
      TH: 'ลงทะเบียนสินค้า'
    },
    '챗봇-pay 상단 프로모션': {
      EN: 'Chatbot-pay top promotion',
      JP: 'チャットボット決済上部プロモーション',
      CH: '聊天机器人支付页顶部促销',
      TH: 'โปรโมชันด้านบน chatbot-pay'
    },
    '챗봇-pay 상단 프로모션 안내': {
      EN: 'Choose how the top promotion area is shown. Interval is in 30-second steps. If you turn the top off, saving clears every product’s “include in top pool” flag so it matches a fully hidden strip.',
      JP: '上部プロモーション領域の表示方式です。間隔は30秒単位。「上部オフ」で保存すると、全商品の上部候補フラグを解除し、帯を完全に隠す状態と揃えます。',
      CH: '设置顶部促销区展示方式；间隔为 30 秒步进。若选择「关闭顶部」并保存，将清除所有商品的「纳入顶部候选」标记，与完全隐藏一致。',
      TH: 'เลือกวิธีแสดงแถบโปรด้านบน ช่วงหมุนเป็นขั้น 30 วิ หากปิดด้านบนและบันทึก ระบบจะล้างทุกสินค้าออกจากพูลเดียวกับซ่อนแถบ'
    },
    '표시 방식': {
      EN: 'Display mode',
      JP: '表示方式',
      CH: '展示方式',
      TH: 'โหมดการแสดง'
    },
    '끔 (상단 숨김)': {
      EN: 'Off (hide top strip)',
      JP: 'オフ（上部非表示）',
      CH: '关（隐藏顶部条）',
      TH: 'ปิด (ซ่อนแถบบน)'
    },
    '프로모션 (전체 그리드)': {
      EN: 'Promotion (full grid)',
      JP: 'プロモーション（全グリッド）',
      CH: '促销（全网格）',
      TH: 'โปรโมชัน (กริดเต็ม)'
    },
    '다이나믹 (3칸 순환)': {
      EN: 'Dynamic (3-slot rotation)',
      JP: 'ダイナミック（3枠ローテーション）',
      CH: '动态（三格轮播）',
      TH: 'ไดนามิก (หมุน 3 ช่อง)'
    },
    '하이브리드 (좌1고정+2칸 순환)': {
      EN: 'Hybrid (1 fixed left + 2 rotating)',
      JP: 'ハイブリッド（左1固定＋2枠ローテーション）',
      CH: '混合（左一固定 + 两格轮播）',
      TH: 'ไฮบริด (ซ้าย 1 คงที่ + หมุน 2 ช่อง)'
    },
    '순환 간격(초)': {
      EN: 'Rotation interval (sec)',
      JP: 'ローテーション間隔（秒）',
      CH: '轮播间隔（秒）',
      TH: 'ช่วงหมุน (วินาที)'
    },
    '30의 배수': {
      EN: 'Multiples of 30',
      JP: '30秒単位',
      CH: '须为 30 的倍数',
      TH: 'ทวีคูณของ 30'
    },
    '편집 상품 · 상단 후보 포함': {
      EN: 'Editing product · include in top pool',
      JP: '編集中の商品・上部候補に含める',
      CH: '正在编辑的商品 · 纳入顶部候选',
      TH: 'สินค้าที่แก้ไข · รวมในพูลด้านบน'
    },
    '상단 후보 포함 안내': {
      EN: 'When the top strip is off, this is locked to “exclude”. When a layout mode is on, pick which products feed the strip (order uses the Sort field).',
      JP: '上部オフのときは「含めない」に固定されます。レイアウトをオンにしたら、帯に載せる商品を選びます（順序は並び順）。',
      CH: '顶部关闭时固定为「不包含」。开启布局后，选择参与顶部带的商品（顺序用排序字段）。',
      TH: 'ปิดแถบบนจะล็อกเป็นไม่รวม เปิดโหมดแล้วเลือกสินค้าที่เข้าพูล (ลำดับตามช่องเรียง)'
    },
    '상단 후보 포함 도움말': {
      EN: 'Top pool membership for the product you are editing.',
      JP: '編集中商品が上部プロモーション候補に入るか。',
      CH: '当前编辑商品是否纳入顶部促销候选。',
      TH: 'สินค้าที่แก้ไขอยู่เข้าพูลโปรด้านบนหรือไม่'
    },
    '상단 후보': {
      EN: 'Top pool',
      JP: '上部候補',
      CH: '顶部候选',
      TH: 'พูลบน'
    },
    '후보 포함': { EN: 'In pool', JP: '候補に含む', CH: '纳入候选', TH: 'ในพูล' },
    '후보 제외': { EN: 'Not in pool', JP: '候補外', CH: '不纳入候选', TH: 'ไม่ในพูล' },
    '판매 활성 상품 수는 플랜 상한을 넘을 수 없습니다. 등록(보관) 행은 플랜 대비 최대 +2건까지 가능합니다.(예: 10건 플랜 → 활성 최대 10, 등록 최대 12) 본사 판매금지·챗봇결제 미사용이면 노출이 제한됩니다. 상위 조직은 가맹 코드 입력 후 불러오기 하세요. 챗봇-pay 상단 프로모션(끔·그리드·다이나믹·하이브리드)과 편집 중 상품의 후보 포함은 상단 「챗봇-pay 상단 프로모션」카드에서 함께 설정합니다.': {
      EN: 'Active sale items cannot exceed the plan cap; you may keep up to plan+2 registered rows (e.g. plan 10 → max 10 active, 12 registered). HQ block or disabled chatbot payment may hide items. Parent orgs: enter a merchant code then Load. Chatbot-pay top promotion (off / grid / dynamic / hybrid) and whether the product you edit is in the top pool are configured together in the “Chatbot-pay top promotion” card above.',
      JP: '販売有効商品数はプラン上限を超えられません。登録（保管）行はプラン比で最大+2件まで可能です（例：10件プラン→有効最大10、登録最大12）。本社販売禁止・チャットボット決済未使用の場合は掲載が制限されます。上位組織は加盟店コード入力後に読み込んでください。チャットボット決済上部プロモーション（オフ/グリッド/ダイナミック/ハイブリッド）と編集中商品の上部候補への含有は、上部の「チャットボット決済上部プロモーション」カードでまとめて設定します。',
      CH: '在售商品数不能超过套餐上限；登记（暂存）行最多可比套餐多 2 条（例如套餐 10：在售最多 10，登记最多 12）。总部禁售或关闭聊天机器人支付会限制展示。上级组织请先输入商户代码再加载。聊天机器人支付页顶部促销（关/网格/动态/混合）与当前编辑商品是否纳入顶部候选，请在上方「聊天机器人支付页顶部促销」卡片中一并设置。',
      TH: 'สินค้าที่ขายอยู่เกินเพดานแพลนไม่ได้ แถวลงทะเบียนเก็บได้มากกว่าแพลน +2 รายการ ห้ามขายของ HQ หรือปิดชำระแชทบอทจะจำกัดการแสดง องค์กรแม่ใส่รหัสร้านแล้วโหลด โปรด้านบน chatbot-pay (ปิด/กริด/ไดนามิก/ไฮบริด) และการรวมสินค้าที่แก้ในพูล ตั้งร่วมกันในการ์ดด้านบน'
    },
    '등록된 상품': {
      EN: 'Registered products',
      JP: '登録済み商品',
      CH: '已登记商品',
      TH: 'สินค้าที่ลงทะเบียนแล้ว'
    },
    '신규 등록 중입니다.': {
      EN: 'Creating a new product.',
      JP: '新規登録モードです。',
      CH: '正在新建登记。',
      TH: 'กำลังลงทะเบียนใหม่'
    },
    '수정 중입니다. 저장하면 반영됩니다.': {
      EN: 'Editing. Save to apply.',
      JP: '編集中です。保存で反映されます。',
      CH: '编辑中。保存后生效。',
      TH: 'กำลังแก้ไข บันทึกเพื่อใช้'
    },
    '불러오기 후 목록이 표시됩니다. 상단 폼에서 신규등록하거나 목록에서 수정·삭제할 수 있습니다.': {
      EN: 'Load to show the list. Register new items in the form above; edit or delete from the list.',
      JP: '読み込み後に一覧が表示されます。上部フォームで新規登録、一覧で修正・削除できます。',
      CH: '加载后显示列表。在上方表单新建登记，或在列表中修改、删除。',
      TH: 'โหลดแล้วจะแสลิสต์ ฟอร์มด้านบนสำหรับลงทะเบียนใหม่ แก้ไข/ลบจากรายการ'
    },
    '상단 신규등록 폼에서 추가하세요.': {
      EN: 'Add products using the new-registration form above.',
      JP: '上部の新規登録フォームから追加してください。',
      CH: '请使用上方的新建登记表单添加。',
      TH: 'เพิ่มได้จากฟอร์มลงทะเบียนใหม่ด้านบน'
    },
    '전체 거래 건수': {
      EN: 'All transactions (count)',
      JP: '全取引件数',
      CH: '全部交易笔数',
      TH: 'ธุรกรรมทั้งหมด (จำนวน)'
    },
    '실패(99/F0)': { EN: 'Failures (99/F0)', JP: '失敗(99/F0)', CH: '失败(99/F0)', TH: 'ล้มเหลว(99/F0)' },
    '환불(30/31)': { EN: 'Refunds (30/31)', JP: '返金(30/31)', CH: '退款(30/31)', TH: 'คืนเงิน(30/31)' },
    '무효계열': { EN: 'Void family', JP: '無効系', CH: '无效类', TH: 'กลุ่มโมฆะ' },
    '취소(20)': { EN: 'Cancels (20)', JP: 'キャンセル(20)', CH: '取消(20)', TH: 'ยกเลิก(20)' },
    '미수 건수': { EN: 'Receivables (count)', JP: '未収 件数', CH: '应收(笔数)', TH: 'ลูกหนี้(จำนวน)' },
    '미수 잔액': { EN: 'Receivables (balance)', JP: '未収 残額', CH: '应收(余额)', TH: 'ลูกหนี้(ยอดคงเหลือ)' },
    '노티 미처리(7d)': { EN: 'Notify unprocessed (7d)', JP: 'ノティ未処理(7d)', CH: '通知未处理(7d)', TH: 'โนติค้าง(7d)' },
    '정산보류(30d)': { EN: 'Settlement holds (30d)', JP: '精算保留(30d)', CH: '结算暂缓(30d)', TH: 'พักชำระ(30d)' },
    'DASHBOARD': { EN: 'Dashboard', JP: 'ダッシュボード', CH: '仪表盘', TH: 'แดชบอร์ด' },
    'HEADQUARTERS': { EN: 'HEADQUARTERS', JP: '本社', CH: '总部', TH: 'สำนักงานใหญ่' },
    '총본사': { EN: 'Headquarters', JP: '本社', CH: '总部', TH: 'สำนักงานใหญ่' },
    '본사': { EN: 'Regional HQ', JP: '本社', CH: '区域总部', TH: 'สำนักงานใหญ่ย่อย' },
    '총판': { EN: 'Master distributor', JP: '総販', CH: '总代', TH: 'ตัวแทนหลัก' },
    '지사': { EN: 'Branch', JP: '支社', CH: '分支', TH: 'สาขา' },
    '대리점': { EN: 'Agency', JP: '代理店', CH: '代理', TH: 'ตัวแทน' },
    '영업점': { EN: 'Sales office', JP: '営業店', CH: '营业点', TH: 'สาขาการขาย' },
    '가맹점': { EN: 'Merchant', JP: '加盟店', CH: '商户', TH: 'ร้านค้า' },
    '성공': { EN: 'Success', JP: '成功', CH: '成功', TH: 'สำเร็จ' },
    '요청': { EN: 'Requested', JP: '要求', CH: '请求', TH: 'ขอ' },
    '대기': { EN: 'Pending', JP: '待機', CH: '等待', TH: 'รอ' },
    '오류': { EN: 'Error', JP: 'エラー', CH: '错误', TH: 'ข้อผิดพลาด' },
    '무효': { EN: 'Void', JP: '無効', CH: '作废', TH: 'โมฆะ' },
    '이메일 무효': { EN: 'Email void', JP: 'メール無効', CH: '邮件作废', TH: 'โมฆะอีเมล' },
    '이메일무효': { KO: '이메일 무효', EN: 'Email void', JP: 'メール無効', CH: '邮件作废', TH: 'โมฆะอีเมล' },
    '강제환불': { EN: 'Forced refund', JP: '強制返金', CH: '强制退款', TH: 'บังคับคืนเงิน' },
    '자동무효': { EN: 'Auto void', JP: '自動無効', CH: '自动作废', TH: 'โมฆะอัตโนมัติ' },
    '자동환불': { EN: 'Auto refund', JP: '自動返金', CH: '自动退款', TH: 'คืนเงินอัตโนมัติ' },

    /* 공통 — API/팝업 오류 메시지 */
    'ID 변경 실패': { EN: 'Failed to change ID', JP: 'ID変更に失敗しました', CH: 'ID更改失败', TH: 'เปลี่ยน ID ไม่สำเร็จ' },
    'ID변경': { EN: 'Change ID', JP: 'ID変更', CH: '更改 ID', TH: 'เปลี่ยน ID' },
    '로그인 ID 변경': { EN: 'Change login ID', JP: 'ログインID変更', CH: '更改登录 ID', TH: 'เปลี่ยนรหัสเข้าใช้' },
    '새 로그인 ID': { EN: 'New login ID', JP: '新しいログインID', CH: '新登录 ID', TH: 'รหัสเข้าใช้ใหม่' },
    '새 로그인 ID를 입력하세요.': { EN: 'Enter a new login ID.', JP: '新しいログインIDを入力してください。', CH: '请输入新登录 ID。', TH: 'กรุณากรอกรหัสเข้าใช้ใหม่' },
    '로그인 ID가 변경되었습니다.': { EN: 'Login ID changed.', JP: 'ログインIDが変更されました。', CH: '登录 ID 已更改。', TH: 'เปลี่ยนรหัสเข้าใช้แล้ว' },
    '변경(로그인 ID)': { EN: 'Change', JP: '変更', CH: '更改', TH: 'เปลี่ยน' },
    '로그인ID 변경 실패': { EN: 'Failed to change login ID', JP: 'ログインID変更に失敗しました', CH: '登录 ID 更改失败', TH: 'เปลี่ยนรหัสเข้าใช้ไม่สำเร็จ' },
    '발송 실패': { EN: 'Send failed', JP: '送信に失敗しました', CH: '发送失败', TH: 'ส่งไม่สำเร็จ' },
    '인증 실패': { EN: 'Verification failed', JP: '認証に失敗しました', CH: '验证失败', TH: 'ยืนยันไม่สำเร็จ' },
    '등록 실패': { EN: 'Registration failed', JP: '登録に失敗しました', CH: '注册失败', TH: 'ลงทะเบียนไม่สำเร็จ' },
    '삭제 실패': { EN: 'Delete failed', JP: '削除に失敗しました', CH: '删除失败', TH: 'ลบไม่สำเร็จ' },
    '엑셀 다운로드에 실패했습니다.': { EN: 'Excel download failed.', JP: 'Excelのダウンロードに失敗しました。', CH: 'Excel下载失败。', TH: 'ดาวน์โหลด Excel ไม่สำเร็จ' },
    '샘플 다운로드에 실패했습니다.': { EN: 'Sample download failed.', JP: 'サンプルのダウンロードに失敗しました。', CH: '示例下载失败。', TH: 'ดาวน์โหลดตัวอย่างไม่สำเร็จ' },

    '인증이 만료되었습니다.': { EN: 'Your session has expired.', JP: 'セッションの有効期限が切れました。', CH: '会话已过期。', TH: 'เซสชันหมดอายุแล้ว' },
    '인증이 만료되었습니다. 다시 로그인하세요.': { EN: 'Your session has expired. Please sign in again.', JP: 'セッションの有効期限が切れました。再ログインしてください。', CH: '会话已过期，请重新登录。', TH: 'เซสชันหมดอายุ โปรดเข้าสู่ระบบอีกครั้ง' },
    'API 오류': { EN: 'API error', JP: 'APIエラー', CH: 'API错误', TH: 'ข้อผิดพลาด API' },
    'API에 연결하지 못했습니다.': { EN: 'Unable to connect to API.', JP: 'APIに接続できません。', CH: '无法连接到API。', TH: 'ไม่สามารถเชื่อมต่อ API ได้' },
    '네트워크·호스팅 설정을 확인해 주세요.': { EN: 'Please check network and hosting settings.', JP: 'ネットワーク・ホスティング設定を確認してください。', CH: '请检查网络与托管设置。', TH: 'โปรดตรวจสอบเครือข่ายและการโฮสต์' },
    'API 경로가 없습니다.': { EN: 'Missing API URL/path.', JP: 'APIパスがありません。', CH: '缺少API路径。', TH: 'ไม่มีเส้นทาง API' },
    '(도메인/프록시·CORS 확인)': { EN: '(check domain/reverse proxy/CORS)', JP: '（ドメイン/プロキシ・CORS確認）', CH: '（检查域名/代理/CORS）', TH: '(ตรวจสอบโดเมน/พร็อกซี/CORS)' },
    '요청 처리에 실패했습니다.': { EN: 'Request failed.', JP: 'リクエストに失敗しました。', CH: '请求失败。', TH: 'คำขอล้มเหลว' },

    '서버 응답 오류': { EN: 'Server response error.', JP: 'サーバー応答エラー', CH: '服务器响应错误', TH: 'ข้อผิดพลาดการตอบกลับของเซิร์ฟเวอร์' },
    '서버 응답이 JSON이 아닙니다.': { EN: 'Server response is not valid JSON.', JP: 'サーバー応答がJSONではありません。', CH: '服务器响应不是JSON。', TH: 'การตอบกลับไม่ใช่ JSON' },
    '(최신 pg-app 배포·Nginx 용량·502 등 확인)': { EN: '(check deployment/reverse proxy limits/502)', JP: '（最新配布/プロキシ制限/502等を確認）', CH: '（检查部署/代理限制/502等）', TH: '(ตรวจสอบการดีพลอย/ข้อจำกัดพร็อกซี/502)' },

    '이미지 읽기에 실패했습니다.': { EN: 'Failed to read image.', JP: '画像の読み取りに失敗しました。', CH: '读取图片失败。', TH: 'อ่านรูปภาพไม่สำเร็จ' },
    '이미지 크기를 확인할 수 없습니다.': { EN: 'Unable to determine image dimensions.', JP: '画像サイズを確認できません。', CH: '无法确认图片尺寸。', TH: 'ตรวจสอบขนาดรูปภาพไม่ได้' },
    '이미지 압축 컨텍스트를 생성할 수 없습니다.': { EN: 'Unable to create image compression context.', JP: '画像圧縮コンテキストを作成できません。', CH: '无法创建图片压缩上下文。', TH: 'สร้างคอนเท็กซ์การบีบอัดรูปภาพไม่ได้' },
    '이미지 압축 결과가 비어 있습니다.': { EN: 'Image compression result is empty.', JP: '画像圧縮結果が空です。', CH: '图片压缩结果为空。', TH: 'ผลลัพธ์การบีบอัดรูปภาพว่างเปล่า' },
    '이미지 로딩에 실패했습니다.': { EN: 'Failed to load image.', JP: '画像の読み込みに失敗しました。', CH: '加载图片失败。', TH: 'โหลดรูปภาพไม่สำเร็จ' },
    '타임라인': { EN: 'Timeline', JP: 'タイムライン', CH: '时间线', TH: 'ไทม์ไลน์' },
    '최근 이벤트': { EN: 'Recent events', JP: '最近のイベント', CH: '最近事件', TH: 'อีเวนต์ล่าสุด' },
    '표시할 이벤트가 없습니다.': { EN: 'No events to display.', JP: '表示するイベントがありません。', CH: '没有可显示的事件。', TH: 'ไม่มีอีเวนต์ให้แสดง' },
    '오늘 처리 권장': { EN: 'Recommended today', JP: '本日の推奨対応', CH: '今日建议处理', TH: 'แนะนำให้จัดการวันนี้' },
    '우선 처리 항목이 없습니다.': { EN: 'No priority items.', JP: '優先対応項目はありません。', CH: '没有优先事项。', TH: 'ไม่มีรายการเร่งด่วน' },
    '이동': { EN: 'Open', JP: '移動', CH: '前往', TH: 'ไป' },
    '이상 탐지(가벼운 통계)': { EN: 'Anomaly detection (light stats)', JP: '異常検知（軽量統計）', CH: '异常检测（轻量统计）', TH: 'ตรวจจับความผิดปกติ (สถิติเบา)' },
    '환불계': { EN: 'Refund family', JP: '返金系', CH: '退款类', TH: 'กลุ่มคืนเงิน' },
    '지급 참고 구간': { EN: 'Payout reference range', JP: '支払参考区間', CH: '支付参考区间', TH: 'ช่วงอ้างอิงการจ่าย' },
    '정산주기': { EN: 'Settlement cycle', JP: '精算周期', CH: '结算周期', TH: 'รอบชำระเงิน' },
    '예약된 적용 정산주기: {0}{1}': {
      EN: 'Pending settlement cycle to apply: {0}{1}',
      JP: '適用予定の精算サイクル: {0}{1}',
      CH: '待应用的结算周期：{0}{1}',
      TH: 'รอบชำระที่จะใช้: {0}{1}'
    },
    ' ({0} 지정)': {
      EN: ' (effective {0})',
      JP: '（指定: {0}）',
      CH: '（指定：{0}）',
      TH: ' (มีผล {0})'
    },
    '최근 3회 지급액 중앙': { EN: 'Median payout (last 3)', JP: '直近3回 支払額中央値', CH: '最近3次支付额中位数', TH: 'ค่ามัธยฐานการจ่าย (3 ครั้งล่าสุด)' },
    '최소': { EN: 'Min', JP: '最小', CH: '最小', TH: 'ต่ำสุด' },
    '최대': { EN: 'Max', JP: '最大', CH: '最大', TH: 'สูงสุด' },
    '인사이트 집계 오류': { EN: 'Insights aggregation error', JP: 'インサイト集計エラー', CH: '洞察汇总错误', TH: 'ข้อผิดพลาดการสรุปอินไซต์' },
    '상단 매출 카드는 표시될 수 있으나, 리스크·KPI·타임라인 등은 집계 단계에서 실패했습니다. 서버 로그와 DB 스키마·데이터를 확인하세요.': {
      EN: 'Top sales cards may show, but risk/KPI/timeline failed during aggregation. Check server logs and DB schema/data.',
      JP: '上部の売上カードは表示される場合がありますが、リスク・KPI・タイムライン等は集計段階で失敗しました。サーバーログとDBスキーマ・データを確認してください。',
      CH: '顶部销售卡片可能显示，但风险/KPI/时间线在汇总阶段失败。请检查服务器日志与数据库结构/数据。',
      TH: 'การ์ดยอดขายอาจแสดงได้ แต่ Risk/KPI/Timeline ล้มเหลวในขั้นสรุป กรุณาตรวจสอบ 로그 และ DB schema/data'
    },
    '메인 확장 데이터가 아직 불완전합니다.': {
      EN: 'Main extended data is still incomplete.',
      JP: 'メイン拡張データがまだ不完全です。',
      CH: '主页扩展数据仍不完整。',
      TH: 'ข้อมูลเสริมหน้าหลักยังไม่สมบูรณ์'
    },
    '응답에': { EN: 'In the response,', JP: '応答に', CH: '响应中', TH: 'ในผลตอบกลับ' },
    '또는(총본사·관리자인 경우)': { EN: 'or (for HQ/Admin)', JP: 'または（HQ/管理者の場合）', CH: '或（总部/管理员时）', TH: 'หรือ (สำนักงานใหญ่/แอดมิน)' },
    '가 없습니다.': { EN: 'is missing.', JP: 'がありません。', CH: '缺少。', TH: 'หายไป' },
    '클라이언트는': { EN: 'The client', JP: 'クライアントは', CH: '客户端', TH: 'ไคลเอนต์' },
    '로 보강 조회를 시도합니다. 계속되면 네트워크 탭에서 두 요청의 JSON과 최신': {
      EN: 'attempts a supplemental fetch. If it persists, check both requests JSON and the latest',
      JP: 'で補強取得を試みます。続く場合はネットワークタブで2つのリクエストのJSONと最新の',
      CH: '将尝试补充请求。若持续发生，请在网络面板检查两个请求的JSON与最新',
      TH: 'จะพยายามดึงข้อมูลเสริม หากยังเป็นอยู่ ให้ตรวจ JSON ของทั้งสองคำขอ และเวอร์ชันล่าสุดของ'
    },
    '배포를 확인하세요.': { EN: 'deployment.', JP: 'デプロイを確認してください。', CH: '部署。', TH: 'การดีพลอย' },
    '만 없습니다. (DASHBOARD는 표시 중)': {
      EN: 'is missing only. (Dashboard is shown)',
      JP: 'のみありません。（DASHBOARDは表示中）',
      CH: '仅缺少此项。（仪表盘仍显示）',
      TH: 'ขาดแค่นี้ (แดชบอร์ดแสดงอยู่)'
    },
    'API·정적 리소스 버전을 맞춘 뒤': { EN: 'After matching API/static versions,', JP: 'API・静的リソースのバージョンを合わせた後', CH: '匹配 API/静态资源版本后', TH: 'หลังจากให้เวอร์ชัน API/สแตติกตรงกันแล้ว' },
    '로 새로고침하세요.': { EN: 'refresh.', JP: 'で更新してください。', CH: '请刷新。', TH: 'ให้รีเฟรช' },
    '규칙 기반 인사이트 (비 LLM)': { EN: 'Rule-based insights (non-LLM)', JP: 'ルールベースのインサイト（非LLM）', CH: '规则洞察（非LLM）', TH: 'อินไซต์แบบกฎ (ไม่ใช้ LLM)' },
    '숫자·근거는 서버 집계이며, LLM 요약은 비활성(1단계)입니다.': {
      EN: 'Numbers/evidence are server-aggregated; LLM summary is disabled (phase 1).',
      JP: '数値・根拠はサーバー集計で、LLM要約は無効（第1段階）です。',
      CH: '数值与依据由服务端汇总；LLM 摘要已关闭（第1阶段）。',
      TH: 'ตัวเลข/หลักฐานมาจากการสรุปบนเซิร์ฟเวอร์ และสรุปด้วย LLM ปิดอยู่ (เฟส 1)'
    },
    '서버 운영 · 트래픽 요약': { EN: 'Server ops · traffic summary', JP: 'サーバー運用・トラフィック要約', CH: '服务器运营·流量摘要', TH: 'สรุปการดูแลเซิร์ฟเวอร์·ทราฟฟิก' },
    '정산 달력 · 실행 이력': { EN: 'Settlement calendar · run history', JP: '精算カレンダー・実行履歴', CH: '结算日历·执行记录', TH: 'ปฏิทินชำระเงิน·ประวัติการรัน' },
    '좌측 메뉴에서 다른 화면을 선택하면 탭이 열립니다. 결제내역 컬럼은 해당 화면의 VIEW SETTING에서 조정할 수 있습니다.': {
      EN: 'Select a screen from the left menu to open a tab. You can adjust payment list columns in View Setting on that screen.',
      JP: '左メニューから画面を選ぶとタブが開きます。決済一覧の列は各画面のVIEW SETTINGで調整できます。',
      CH: '从左侧菜单选择界面会打开标签页。支付列表列可在该界面的 VIEW SETTING 中调整。',
      TH: 'เลือกหน้าจอจากเมนูซ้ายเพื่อเปิดแท็บ ปรับคอลัมน์รายการชำระเงินได้ที่ VIEW SETTING ของหน้าจอนั้น'
    },
    '기준일': { EN: 'As of', JP: '基準日', CH: '基准日', TH: 'ณ วันที่' },
    '대시보드 API를 불러올 수 없습니다.': {
      EN: 'Cannot load dashboard API.',
      JP: 'ダッシュボードAPIを読み込めません。',
      CH: '无法加载仪表盘 API。',
      TH: 'ไม่สามารถโหลด API แดชบอร์ดได้'
    },
    '불러오는 중…': { EN: 'Loading…', JP: '読み込み中…', CH: '加载中…', TH: 'กำลังโหลด…' },
    '조회 실패': { EN: 'Load failed', JP: '照会に失敗しました', CH: '查询失败', TH: 'โหลดล้มเหลว' },
    '잠시만 기다려주십시오': {
      EN: 'Please wait.',
      JP: 'しばらくお待ちください。',
      CH: '请稍候。',
      TH: 'โปรดรอสักครู่'
    },
    '접기': { EN: 'Collapse', JP: '折りたたみ', CH: '收起', TH: 'พับ' },
    '언어': { EN: 'Language', JP: '言語', CH: '语言', TH: 'ภาษา' },
    '접속 IP:': { EN: 'IP:', JP: 'IP:', CH: 'IP：', TH: 'IP:' },
    '접속시간:': { EN: 'Time:', JP: '時刻:', CH: '时间：', TH: 'เวลา:' },
    '나의정보': { EN: 'My profile', JP: 'マイ情報', CH: '我的信息', TH: 'ข้อมูลของฉัน' },
    '로그아웃': { EN: 'Log out', JP: 'ログアウト', CH: '退出登录', TH: 'ออกจากระบบ' },
    '전체닫기': { EN: 'Close all', JP: 'すべて閉じる', CH: '关闭全部', TH: 'ปิดทั้งหมด' },
    '메인': { EN: 'Home', JP: 'メイン', CH: '主页', TH: 'หน้าแรก' },
    '지급': { EN: 'Payout', JP: '支払', CH: '支付', TH: 'จ่าย' },
    '최근 7일 노티 미매핑/미적재 {0}건': {
      EN: 'Notify unmapped/unloaded (7d): {0}',
      JP: '直近7日 ノティ未マッピング/未取込 {0}件',
      CH: '最近7天 通知未映射/未入库 {0}笔',
      TH: 'โนติ ไม่แมป/ไม่โหลด (7 วัน): {0}'
    },
    '리스크 점수가 지난주 대비 {0} 상승': {
      EN: 'Risk score increased by {0} vs last week',
      JP: 'リスクスコアが先週比 {0} 上昇',
      CH: '风险分数较上周上升 {0}',
      TH: 'คะแนนความเสี่ยงเพิ่มขึ้น {0} เทียบสัปดาห์ก่อน'
    },
    '환불·무효 추이를 결제내역에서 필터로 확인': {
      EN: 'Check refund/void trends by filtering payment list.',
      JP: '返金・無効の推移は決済一覧でフィルタして確認してください。',
      CH: '在支付列表中通过筛选查看退款/无效趋势。',
      TH: 'ตรวจแนวโน้มคืนเงิน/โมฆะด้วยการกรองรายการชำระเงิน'
    },
    '최근 7일 리스크 내러티브 템플릿': {
      EN: 'Risk score (7d) is {score}; change vs previous 7d: {delta}. Breakdown: fail {fail}, void {void}, refund {refund}, cancel {cancel}. Notify unmapped/unloaded (7d): {notify}.',
      JP: '直近7日リスクスコアは {score}、直前7日比 {delta} です。内訳: 失敗 {fail}・無効系 {void}・返金 {refund}・キャンセル {cancel} 件。ノティ未マッピング/未取込（7日） {notify} 件。',
      CH: '最近7天风险分数为 {score}，较前7天变化 {delta}。构成：失败 {fail}、无效 {void}、退款 {refund}、取消 {cancel}。通知未映射/未入库（7天）{notify}。',
      TH: 'คะแนนความเสี่ยง 7 วัน = {score}, เปลี่ยนเทียบ 7 วันก่อน {delta}. รายละเอียด: ล้มเหลว {fail}, โมฆะ {void}, คืนเงิน {refund}, ยกเลิก {cancel}. โนติ ไม่แมป/ไม่โหลด (7 วัน) {notify}.'
    },
    '전사 기준 거래·매출 요약입니다. 서버 트래픽은 일간 수집 데이터 기반입니다.': {
      EN: 'Company-wide transaction/sales summary. Server traffic is based on daily collected data.',
      JP: '全社基準の取引・売上要約です。サーバートラフィックは日次収集データに基づきます。',
      CH: '这是全公司的交易/销售汇总。服务器流量基于每日采集数据。',
      TH: 'สรุปธุรกรรม/ยอดขายทั้งบริษัท ทราฟฟิกเซิร์ฟเวอร์อ้างอิงข้อมูลที่เก็บรายวัน'
    },
    /* /main HQ 허브 — 서버 제공 insightHint 및 타일 문자열 */
    '소속 조직 또는 허용 가맹 범위가 없어 거래 요약이 0으로 표시됩니다.': {
      EN: 'Trade summary shows 0 because there is no org or allowed merchant scope.',
      JP: '所属組織または許容加盟店範囲がなく、取概要約は0として表示されています。',
      CH: '无所属组织或允许的商户范围，交易汇总显示为0。',
      TH: 'ไม่มีองค์กรหรือขอบเขตร้านค้าที่อนุญาต สรุปธุรกรรมแสดงเป็น 0'
    },
    'DASHBOARD: 조직·7일 매출 추이·정산·업무 바로가기와 리스크 요약을 한 화면에서 확인할 수 있습니다.': {
      EN: 'Dashboard: organization, 7‑day revenue trend, settlement, shortcuts, and risk summary in one view.',
      JP: 'DASHBOARD: 組織・7日売上推移・精算・業務ショートカットとリスク要約を一画面で確認できます。',
      CH: '仪表板：组织、7天营收趋势、结算、快捷入口与风险摘要一屏汇总。',
      TH: 'แดชบอร์ด: องค์กร แนวโน้มยอด 7 วัน ชำระบัญชี ทางลัด และ 요약ความเสี่ยงในหน้าเดียว'
    },
    '본사 하위 가맹점 기준 결제·승인 금액 요약입니다.': {
      EN: 'Payment and approval amount summary for merchants under regional HQ.',
      JP: '本社傘下の加盟店基準の決済・承認金額要約です。',
      CH: '以本部下级商户为准的支付与批准金额汇总。',
      TH: 'สรุปยอดชำระ/อนุมัติตามร้านค้าใต้สำนักงานใหญ่ภูมิภาค'
    },
    '담당 가맹점 범위 내 결제·승인 건수 및 금액 요약입니다.': {
      EN: 'Payment/approval counts and amounts within assigned merchant scope.',
      JP: '担当加盟店範囲内の決済・承認件数および金額要約です。',
      CH: '负责范围内的支付/批准笔数与金额汇总。',
      TH: 'จำนวนและยอดชำระ/อนุมัติในขอบเขตร้านค้าที่รับผิดชอบ'
    },
    '가맹점 기준 거래 요약과 정산 실행 이력(정산 달력)을 제공합니다.': {
      EN: 'Merchant trade summary plus settlement runs (calendar).',
      JP: '加盟店基準の取引要約と精算実行履歴（精算カレンダー）を提供します。',
      CH: '提供商户维度交易摘要与结算执行记录（结算日历）。',
      TH: 'สรุปธุรกรรมตามร้านค้าและประวัติการรันชำระเงิน (ปฏิทิน)'
    },
    '로그인 조직 범위 내 거래 요약입니다.': {
      EN: 'Transaction summary within the logged‑in organization scope.',
      JP: 'ログイン組織の範囲内における取引要約です。',
      CH: '登录组织范围内的交易摘要。',
      TH: 'สรุปธุรกรรมภายในขอบเขตองค์กรที่เข้าสู่ระบบ'
    },
    '허용 가맹 범위가 없어 거래·정산 요약을 생략했습니다.': {
      EN: 'Settlement/trade summaries skipped: no merchant scope.',
      JP: '許容加盟店範囲がないため、取引・精算要約を省略しました。',
      CH: '无允许的商户范围，已省略交易/结算摘要。',
      TH: 'ไม่มีขอบเขตร้านค้าที่อนุญาต จึงข้ามสรุปธุรกรรม/ชำระ'
    },
    'DASHBOARD 집계 중 오류: ': {
      EN: 'Dashboard aggregation error: ',
      JP: 'DASHBOARD集計中のエラー: ',
      CH: '仪表板聚合错误：',
      TH: 'ข้อผิดพลาดระหว่างประมวลผลแดชบอร์ด: '
    },
    '서버 운영': {
      EN: 'Server ops',
      JP: 'サーバー運用',
      CH: '服务器运营',
      TH: 'ดูแลเซิร์ฟเวอร์'
    },
    '호스트·SSL·디스크·DB 요약': {
      EN: 'Host · SSL · disk · DB overview',
      JP: 'ホスト・SSL・ディスク・DB要約',
      CH: '主机·SSL·磁盘·数据库概要',
      TH: 'โฮสต์ SSL ดิสก์ DB สรุป'
    },
    '정산 관리설정': {
      EN: 'Settlement admin settings',
      JP: '精算管理設定',
      CH: '结算管理配置',
      TH: 'ตั้งค่าชำระบัญชีหลัก'
    },
    '주기·보류·환수 정책': {
      EN: 'Cycles · holds · recovery policy',
      JP: '周期・保留・回収ポリシー',
      CH: '周期·暂缓·回收策略',
      TH: 'รอบ พักเก็บ การเรียกคืน'
    },
    '노티 수신': {
      EN: 'Notify inbound',
      JP: 'ノティ受信',
      CH: '通知接入',
      TH: 'รับแจ้งเตือน'
    },
    '미매핑·재전송 점검': {
      EN: 'Unmapped · resend checks',
      JP: '未マッピング・再送確認',
      CH: '未映射·补发检查',
      TH: 'ที่ยังไม่แมป/ตรวจส่งซ้ำ'
    },
    '결제 내역': {
      EN: 'Payments',
      JP: '決済一覧',
      CH: '支付列表',
      TH: 'รายการชำระเงิน'
    },
    '승인·환불·무효 필터': {
      EN: 'Approve/refund/void filters',
      JP: '承認・返金・無効フィルター',
      CH: '授权/退款/无效筛选',
      TH: 'กรอง อนุมัติ/คืนเงิน/โมฆะ'
    },
    '비자동 가맹만 [수동실행]': {
      EN: 'Non‑auto merchants only — [manual run]',
      JP: '非自動加盟店のみ（手動実行）',
      CH: '仅非自动商户[手动运行]',
      TH: 'เฉพาะร้านค้าที่ไม่ใช้อัตโนมัติ [รันด้วยมือ]'
    },
    '유통망 정산': {
      EN: 'Distribution settlement',
      JP: '流通網の精算',
      CH: '分销链结算',
      TH: 'ชำระบัญชีเครือข่ายจำหน่าย'
    },
    '단계별 정산 내역': {
      EN: 'Stepwise settlement ledger',
      JP: '段階別精算履歴',
      CH: '分阶段结算明细',
      TH: 'รายละเอียดชำระตามระดับ'
    },
    '가맹점 정산': {
      EN: 'Merchant payout',
      JP: '加盟店精算',
      CH: '商户结算',
      TH: 'การชำระร้านค้า'
    },
    '가맹 지급·보류': {
      EN: 'Payouts · holds',
      JP: '加盟店支払・保留',
      CH: '门店支付·暂缓',
      TH: 'จ่ายให้ร้าน/พัก'
    },
    '잔액·환수': {
      EN: 'Balance · recovery',
      JP: '残額・回収',
      CH: '余额·回收',
      TH: 'ยอดเรียกคืน'
    },
    '업체 트리': {
      EN: 'Company tree',
      JP: '取引先ツリー',
      CH: '企业树',
      TH: 'ต้นไม้องค์กร'
    },
    '조직·가맹 구조': {
      EN: 'Org · merchant hierarchy',
      JP: '組織・加盟店構造',
      CH: '组织与商户层级',
      TH: 'โครงสร้างองค์กรและร้านค้า'
    },
    '요율·배분': {
      EN: 'Fees · splits',
      JP: '手数料率・配分',
      CH: '费率·分成',
      TH: 'เรทค่าธรรมเนียมและแบ่ง'
    },
    'PG사 연동': {
      EN: 'PG linkage',
      JP: 'PG連携',
      CH: 'PG对接',
      TH: 'เชื่อม PG'
    },
    'API·MID': {
      EN: 'API · MID',
      JP: 'API・MID',
      CH: 'API·MID',
      TH: 'API · MID'
    },
    '도메인·포털': {
      EN: 'Domain · portal',
      JP: 'ドメイン・ポータル',
      CH: '域名·门户',
      TH: 'โดเมน·พอร์ทัล'
    },
    '호스트·브랜딩': {
      EN: 'Host · branding',
      JP: 'ホスト・ブランディング',
      CH: '主机·品牌展示',
      TH: 'โฮสต์·แบรนด์'
    },
    '서버운영관리': {
      EN: 'Server ops',
      JP: 'サーバー運用管理',
      CH: '服务器运营管理',
      TH: 'จัดการเซิร์ฟเวอร์'
    },
    '정산관리설정': {
      EN: 'Settlement admin settings',
      JP: '精算管理設定',
      CH: '结算管理配置',
      TH: 'ตั้งค่าผู้ดูแลระบบชำระบัญชี'
    },
    '잔액': {
      EN: 'Balance',
      JP: '残額',
      CH: '余额',
      TH: 'ยอดคงเหลือ'
    },
    '정산 실행': {
      EN: 'Settlement run',
      JP: '精算実行',
      CH: '结算执行',
      TH: 'รันชำระเงิน'
    },
    '노티 미매핑/미적재': {
      EN: 'Notify unmapped / not loaded',
      JP: 'ノティ未マッピング／未取込',
      CH: '通知未映射/未入库',
      TH: 'โนติที่ยังไม่แมป/ยังไม่โหลด'
    },
    '미수금 잔액 {0}건 · 합계 약 {1} 원': {
      EN: 'Receivable balance {0} rows · approx. sum {1} KRW',
      JP: '未収金残 {0} 件・合計 約 {1} ウォン',
      CH: '应收余额 {0} 笔 · 合计约 {1} 韩元',
      TH: 'ลูกหนี้ {0} รายการ · รวมประมาณ {1} วอน'
    },
    '최근 30일 정산 보류/지급보류 실행 {0}건': {
      EN: '{0} settlement hold / payout-hold runs in the last 30 days',
      JP: '直近30日 精算保留／支払保留 実行 {0} 件',
      CH: '最近30天内结算暂缓/拨付暂缓运行 {0} 笔',
      TH: '{0} รายการพักชำระ/พักจ่ายในช่วง 30 วัน'
    },
    '다음 정산 실행 일시는 정산주기({cycle}) 및 정산실행 배치 기준입니다.': {
      EN: 'Next settlement run time follows calc cycle ({cycle}) and the settlement batch.',
      JP: '次の精算実行タイミングは精算周期（{cycle}）および精算実行バッチに従います。',
      CH: '下次结算执行时间取决于结算周期（{cycle}）与结算执行任务。',
      TH: 'การรันชำระครั้งถัดไปตามรอบ ({cycle}) และงาน batch ชำระ'
    },
    '허용된 가맹 범위가 없어 인사이트 집계를 생략했습니다.': {
      EN: 'Insights were skipped — no merchant scope.',
      JP: '許容加盟店範囲がないため、インサイト集計を省略しました。',
      CH: '无允许的商户范围，已跳过洞察汇总。',
      TH: 'ไม่มีขอบเขตร้านค้า จึงข้ามการสรุปอินไซต์'
    },
    '인사이트 집계 중 오류가 발생했습니다. DB 스키마·연결·서버 로그를 확인하세요.': {
      EN: 'Insight aggregation failed. Check DB schema, connectivity, and server logs.',
      JP: 'インサイト集計中にエラーが発生しました。DBスキーマ・接続・サーバーログを確認してください。',
      CH: '洞察汇总出错。请检查数据库结构、连接与服务器日志。',
      TH: 'การสรุปอินไซต์ล้มเหลว ตรวจสอบสคีมา DB เชื่อมต่อและล็อก'
    },
    '지난 7일 환불·강제환불 건수 상위(조직 범위 내)': {
      EN: 'Top refund/forced‑refund volume (last 7d, scope)',
      JP: '直近7日 返金・強制返金 件数上位（組織範囲内）',
      CH: '最近7天退款/强制退款笔数居前（范围内）',
      TH: 'อันดับยอดคืนเงิน/บังคับคืนย้อนหลัง 7 วัน (ภายใน scope)'
    },
    '최근 정산 실행이 없어 지급 구간을 산출하지 못했습니다.': {
      EN: 'No recent settlement runs; payout range unavailable.',
      JP: '最近の精算実行がないため、支払区間を算出できません。',
      CH: '无最近结算执行，无法计算支付区间。',
      TH: 'ไม่มีประวัติรันชำระล่าสุด คำนวณช่วงจ่ายไม่ได้'
    },
    '지급액 데이터가 없습니다.': {
      EN: 'No payout amounts available.',
      JP: '支払金額データがありません。',
      CH: '无支付金额数据。',
      TH: 'ไม่มีข้อมูลยอดจ่าย'
    },
    '최근 3회 지급액의 최소·최대·중앙값으로 참고 구간만 표시합니다. 보류·환수·수수료는 실행별로 다릅니다.': {
      EN: 'Shows reference range using min/max/median of the last three payouts; holds/recovery/fees vary by run.',
      JP: '最近3回の支払額の最小・最大・中央値で参考区間のみ表示します。保留・回収・手数料は実行ごとに異なります。',
      CH: '以最近3次支付额的最低/最高/中位数仅供参考；暂扣、回收与手续费因执行而异。',
      TH: 'แสดงช่วงอ้างอิงจาก ต่ำ/สูง/มัธยฐาน ของการจ่าย 3 ครั้งล่าสุด; พัก/เรียกคืนค่าธรรมเนียมต่างกันในแต่ละรัน'
    },
    /* 대시보드 explainers(서버) */
    '최근 7일(오늘 포함) 실패·무효·환불·취소 건수에 가중치를 둔 규칙 점수입니다. 지난 7일 대비 증감은 동일 규칙으로 비교합니다.': {
      EN: 'Rule score from weighted fail/void/refund/cancel counts over 7 days (including today). Week‑over‑week uses the same rule.',
      JP: '直近7日（当日含む）で失敗・無効・返金・取消件数に重みを付けた規則スコアです。先週7日との増減も同じ規則で比較します。',
      CH: '以近7日（含今天）失败、无效、退款、取消的加权规则分数；与上周七日对比沿用同一规则。',
      TH: 'คะแนนจากความถี่ของ ล้มเหลว/โมฆะ/คืน/ยกเลิก ใน 7 วัน (รวมวันนี้) แบบถ่วงน้ำหนัก และเทียบสัปดาห์ด้วยกฎเดียวกัน'
    },
    '오늘 0시 이후 결제일시 기준 성공(상태 10) 건수·통화별 승인금액 합, 전체 거래 건수, 실패·무효·환불·취소 건수이며, 미수(PENDING)·노티 미매핑·정산보류/지급보류 행 수가 함께 포함됩니다.': {
      EN: 'Since midnight by payment time: successful (status 10) count, approved amounts by billing currency, all txn counts, fail/void/refund/cancel, plus receivable/notify/hold rows.',
      JP: '本日0時以降の決済時刻ベースで、成功（状態10）件数・通貨別承認金額合計、全取引件数、失敗・無効・返金・取消件数に加え、未収(PENDING)・ノティ未マッピング・精算保留／支払保留件数を含みます。',
      CH: '自今日0点起按支付时刻：成功（状态10）笔数、按币种授权金额合计、全部交易笔数、失败/无效/退款/取消，并含应收、通知未映射、结算/拨付暂缓。',
      TH: 'ตั้งแต่เที่ยงคืนตามเวลาชำระ: จำนวนสำเร็จ (สถานะ 10) ยอดอนุมัติตามสกุลเงิน จำนวนธุรกรรมทั้งหมด ล้มเหลว/โมฆะ/คืน/ยกเลิก และลูกหนี้·โนติ·พักชำระ'
    },
    '전일 0시~24시(당일 0시 직전) 결제일시 기준 성공(상태 10) 건수·통화별 승인금액 합, 전체 거래 건수, 실패·무효·환불·취소 건수입니다. 미수·노티 등은 시점 스냅샷이 없어 제외합니다.': {
      EN: 'Yesterday 00:00–24:00 by payment time: successful (status 10) count, approved amounts by currency, all txn counts, fail/void/refund/cancel; receivable/notify KPIs omitted.',
      JP: '前日0〜24時（当日0時直前まで）決済時刻ベースで、成功（状態10）件数・通貨別承認金額合計、全取引件数、失敗・無効・返金・取消件数です。未収・ノティ等は同日スナップショットがないため対象外です。',
      CH: '昨日0–24点（至当日零点前）按支付时刻：成功（状态10）笔数、按币种授权金额合计、全部交易笔数、失败/无效/退款/取消；应收/通知等不含。',
      TH: 'เมื่อวาน 00:00–24:00 ตามเวลาชำระ: สำเร็จ (สถานะ 10) ยอดอนุมัติตามสกุลเงิน จำนวนทั้งหมด ล้มเหลว/โมฆะ/คืน/ยกเลิก ไม่รวม 미수·โนติ'
    },
    '정산 실행 생성·미수금 생성·노티 미처리(매핑 외) 중 최근 이벤트입니다.': {
      EN: 'Recent settlement runs, receivable creation, unprocessed notifies (beyond mapping).',
      JP: '精算実行作成・未収金作成・ノティ未処理（マッピング外）などの最新イベントです。',
      CH: '最近的结算创建、应收创建及未处理通知（映射外）事件。',
      TH: 'อีเวนต์ล่าสุดจากรันชำระ สร้างลูกหนี้ โนติที่ยังไม่ประมวลผล'
    },
    '규칙으로 정렬한 오늘 확인 권장 항목이며, 클릭 시 관리 화면으로 이동합니다.': {
      EN: 'Rule‑ranked checklist for today — click to open admin screens.',
      JP: '規則で並べた本日確認推奨項目で、クリックすると管理画面に移動します。',
      CH: '按规则排序的今日推荐确认项；点击跳转管理界面。',
      TH: 'รายการแนะนำวันนี้เรียงตามกฎ คลิกไปหน้าจัดการ'
    },
    '지난 7일 환불·강제환불 건수 상위 가맹(식별자 마스킹)입니다.': {
      EN: 'Top merchants by refunds/forced refunds in 7 days (masked IDs).',
      JP: '直近7日の返金・強制返金件数上位の加盟店です（識別子マスク）。',
      CH: '近7日退款/强制退款居前商户（标识已掩码）。',
      TH: 'ร้านค้าที่คืนบ่อยใน 7 วัน (รหัสถูกมาสก์)'
    },
    '최근 정산 실행 지급액으로 참고 구간만 제시합니다. 실제 지급은 보류·환수·정책에 따라 달라질 수 있습니다.': {
      EN: 'Reference range only from recent payout amounts; actual payout may differ under holds/recovery/policy.',
      JP: '最近の精算実行の支払額での参考区間のみです。実際の支払は保留・回収・ポリシーにより異なる場合があります。',
      CH: '仅根据最近执行的支付金额给出参考区间；实际支付可能因暂扣/回收/策略而不同。',
      TH: 'แสดงเฉพาะช่วงอ้างอิงจากยอดจ่ายที่ผ่านมา จริงอาจต่างจากพัก/เรียกคืน/นโยบาย'
    },
    /* /user/userMng — 검색 라벨·thead·그리드·알림 (STRING_MAP 보강) */
    '사용자 ID': {
      EN: 'User ID',
      JP: 'ユーザーID',
      CH: '用户ID',
      TH: 'รหัสผู้ใช้'
    },
    '사용자명': {
      EN: 'User name',
      JP: 'ユーザー名',
      CH: '用户名',
      TH: 'ชื่อผู้ใช้'
    },
    '사용자ID*': {
      EN: 'User ID*',
      JP: 'ユーザーID※',
      CH: '用户ID*',
      TH: 'รหัสผู้ใช้*'
    },
    '사용자명*': {
      EN: 'User name*',
      JP: 'ユーザー名※',
      CH: '用户名*',
      TH: 'ชื่อผู้ใช้*'
    },
    '연락처*': {
      EN: 'Contact*',
      JP: '連絡先※',
      CH: '联系方式*',
      TH: 'ติดต่อ*'
    },
    '권한그룹*': {
      EN: 'Permission group*',
      JP: '権限グループ※',
      CH: '权限组*',
      TH: 'กลุ่มสิทธิ์*'
    },
    '역할': { EN: 'Role', JP: 'ロール', CH: '角色', TH: 'บทบาท' },
    '사용여부*': {
      EN: 'Status*',
      JP: '使用状態※',
      CH: '使用状态*',
      TH: 'สถานะ*'
    },
    '전환사유': {
      EN: 'Status change reason',
      JP: '変更理由',
      CH: '变更原因',
      TH: 'เหตุผลการเปลี่ยนสถานะ'
    },
    '미사용전환사유': {
      EN: 'Status change reason',
      JP: '変更理由',
      CH: '变更原因',
      TH: 'เหตุผลการเปลี่ยนสถานะ'
    },
    '미사용 또는 영구정지로 변경할 때는 전환사유를 입력하세요.': {
      EN: 'Enter a status change reason when setting Inactive or Suspended.',
      JP: '未使用または永久停止に変更する場合は変更理由を入力してください。',
      CH: '设为未使用或永久停用时请填写变更原因。',
      TH: 'เมื่อตั้งเป็นไม่ใช้งานหรือระงับถาวร กรุณากรอกเหตุผลการเปลี่ยนสถานะ'
    },
    '초기화 권한 없음': {
      EN: 'No permission to reset',
      JP: '初期化する権限がありません',
      CH: '无权重置',
      TH: 'ไม่มีสิทธิ์รีเซ็ต'
    },
    'OTP': { EN: 'OTP', JP: 'OTP', CH: 'OTP', TH: 'OTP' },
    '저장 후 OTP를 관리할 수 있습니다.': {
      EN: 'Save the row first to manage OTP.',
      JP: '保存後にOTPを管理できます。',
      CH: '请先保存后再管理 OTP。',
      TH: 'บันทึกแถวก่อนจึงจะจัดการ OTP ได้'
    },
    '등록': { EN: 'Registered', JP: '登録済み', CH: '已注册', TH: 'ลงทะเบียนแล้ว' },
    '영구정지': { EN: 'Suspended', JP: '永久停止', CH: '永久停用', TH: 'ระงับถาวร' },
    '영구정지된 업체는 로그인할 수 없습니다.': {
      EN: 'This merchant is suspended. Please contact the system administrator.',
      JP: '該当加盟店は停止中です。運用管理者にお問い合わせください。',
      CH: '该商户已停用。请联系运营管理员。',
      TH: 'ร้านค้านี้ถูกระงับ กรุณาติดต่อผู้ดูแลระบบ'
    },
    '해당가맹점은 정지중입니다. 운영관리자에게 문의하세요.': {
      EN: 'This merchant is suspended. Please contact the system administrator.',
      JP: '該当加盟店は停止中です。運用管理者にお問い合わせください。',
      CH: '该商户已停用。请联系运营管理员。',
      TH: 'ร้านค้านี้ถูกระงับ กรุณาติดต่อผู้ดูแลระบบ'
    },
    '기본 조회는 업체사용상태가 사용인 업체만 표시합니다. 미사용·영구정지·전체는 셀렉트에서 선택하세요. 미사용(N)은 로그인은 가능하나 신규 결제·정산이 중단됩니다. 영구정지(S)는 연동 사용자 계정이 정지되며 로그인할 수 없습니다. 상위를 미사용/영구정지로 두면 하위 프로필도 함께 연쇄 처리됩니다.': {
      EN: 'By default only companies marked as active are listed. Select inactive, permanently suspended, or all in the filter. Inactive (N) allows login but stops new payments and settlement. Permanently suspended (S) disables linked user accounts and blocks login. Setting a parent to inactive or permanently suspended cascades to descendant profiles.',
      JP: '既定では使用中の加盟店のみ表示します。未使用・永久停止・すべてはセレクトで選択してください。未使用(N)はログイン可能ですが新規決済・精算が停止されます。永久停止(S)は連携ユーザーアカウントが停止されログインできません。上位を未使用/永久停止にすると下位プロフィールも連鎖処理されます。',
      CH: '默认仅显示「使用中」的商户；在筛选器选择未使用、永久停用或全部。未使用(N)可登录但停止新支付与结算。永久停用(S)会停用关联用户账户且无法登录。上级设为未使用/永久停用则下级档案一并级联处理。',
      TH: 'ค่าเริ่มต้นแสดงเฉพาะร้านที่สถานะใช้งาน เลือกไม่ใช้ ระงับถาวร หรือทั้งหมดในตัวกรอง ไม่ใช้(N) ยังเข้าสู่ระบบได้แต่หยุดการชำระและหักบัญชีใหม่ ระงับถาวร(S) จะระงับบัญชีผู้ใช้ที่เชื่อมโยงและเข้าสู่ระบบไม่ได้ ตั้งระดับบนเป็นไม่ใช้/ระงับถาวรจะส่งผลต่อโปรไฟล์ลูกด้วย'
    },
    '미등록': { EN: 'Not registered', JP: '未登録', CH: '未注册', TH: 'ยังไม่ลงทะเบียน' },
    '초기화': { EN: 'Reset', JP: '初期化', CH: '重置', TH: 'รีเซ็ต' },
    '소속 업체코드를 확인할 수 없습니다.': {
      EN: 'Cannot resolve your company code.',
      JP: '所属の加盟店コードを確認できません。',
      CH: '无法确认所属商户代码。',
      TH: 'ไม่สามารถยืนยันรหัสร้านของคุณได้'
    },
    '사용자 정보를 불러오지 못했습니다.': {
      EN: 'Could not load user information.',
      JP: 'ユーザー情報を読み込めませんでした。',
      CH: '无法加载用户信息。',
      TH: 'โหลดข้อมูลผู้ใช้ไม่สำเร็จ'
    },
    '추가 행: 사용자ID와 사용자명을 입력하세요.': {
      EN: 'New row: enter user ID and name.',
      JP: '追加行: ユーザーIDとユーザー名を入力してください。',
      CH: '新增行：请输入用户ID和姓名。',
      TH: 'แถวใหม่: กรอกรหัสผู้ใช้และชื่อ'
    },
    '비밀번호는 8자 이상이어야 합니다.': {
      EN: 'Password must be at least 8 characters.',
      JP: 'パスワードは8文字以上である必要があります。',
      CH: '密码至少需要8个字符。',
      TH: 'รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร'
    },
    '비밀번호(초기화)': {
      EN: 'Password (reset)',
      JP: 'パスワード（初期化）',
      CH: '密码（重置）',
      TH: 'รหัสผ่าน (รีเซ็ต)'
    },
    'OTP 미등록': {
      EN: 'OTP: clear registration',
      JP: 'OTP未登録にする',
      CH: 'OTP：解除注册',
      TH: 'OTP: ยกเลิกการลงทะเบียน'
    },
    '초기화 메뉴': {
      EN: 'Password / OTP actions',
      JP: 'パスワード・OTP操作',
      CH: '密码 / OTP 操作',
      TH: 'เมนูรหัสผ่าน / OTP'
    },
    '자동 (ID+1!)': {
      EN: 'Auto (ID+1!)',
      JP: '自動（ID+1!）',
      CH: '自动（ID+1!）',
      TH: 'อัตโนมัติ (ID+1!)'
    },
    '저장 시 사용자ID+1! 로 자동 설정됩니다. 첫 로그인에서 비밀번호를 변경합니다.': {
      EN: 'On save, the password is set to User ID + "1!". The user must set a new password on first login.',
      JP: '保存時にパスワードはユーザーID+「1!」で自動設定されます。初回ログイン時にパスワードを変更します。',
      CH: '保存时将密码自动设为用户ID+「1!」。首次登录时需重新设置密码。',
      TH: 'เมื่อบันทึก ตั้งรหัสเป็น ID+「1!」 ต้องเปลี่ยนรหัสเมื่อเข้าครั้งแรก'
    },
    '저장되었습니다.': {
      EN: 'Saved.',
      JP: '保存しました。',
      CH: '已保存。',
      TH: 'บันทึกแล้ว'
    },
    '저장 실패': {
      EN: 'Save failed',
      JP: '保存に失敗しました',
      CH: '保存失败',
      TH: 'บันทึกล้มเหลว'
    },
    'OTP 등록을 초기화할까요?': {
      EN: 'Reset OTP registration?',
      JP: 'OTP登録を初期化しますか？',
      CH: '要重置 OTP 注册吗？',
      TH: 'รีเซ็ตการลงทะเบียน OTP หรือไม่'
    },
    '사용자관리 초기화 재확인': {
      EN: 'Please confirm again. Proceed with this reset?',
      JP: 'もう一度確認します。この初期化を続行しますか？',
      CH: '请再次确认。是否继续执行此重置？',
      TH: 'ยืนยันอีกครั้ง ดำเนินการรีเซ็ตนี้ต่อหรือไม่'
    },
    'OTP가 초기화되었습니다.': {
      EN: 'OTP has been reset.',
      JP: 'OTPを初期化しました。',
      CH: '已重置 OTP。',
      TH: 'รีเซ็ต OTP แล้ว'
    },
    '초기화 실패': {
      EN: 'Reset failed',
      JP: '初期化に失敗しました',
      CH: '重置失败',
      TH: 'รีเซ็ตล้มเหลว'
    },
    '비밀번호를 아이디+1! 로 초기화할까요? 최초 로그인 시 새 창에서 비밀번호를 다시 설정합니다.': {
      EN: 'Reset password to user ID + 1! ? On first login, set a new password in the new window.',
      JP: 'パスワードを「ログインID+1!」に初期化しますか？初回ログイン時に新しいウィンドウでパスワードを再設定します。',
      CH: '是否将密码重置为「登录ID+1!」？首次登录时将在新窗口中重新设置密码。',
      TH: 'รีเซ็ตรหัสผ่านเป็นรหัสผู้ใช้+1! หรือไม่? ครั้งแรกที่ล็อกอินจะตั้งรหัสใหม่ในหน้าต่างใหม่'
    },
    '초기화 완료': {
      EN: 'Reset complete',
      JP: '初期化が完了しました',
      CH: '重置完成',
      TH: 'รีเซ็ตเสร็จแล้ว'
    },
    '사용자ID:': {
      EN: 'User ID:',
      JP: 'ユーザーID:',
      CH: '用户ID：',
      TH: 'รหัสผู้ใช้:'
    },
    '임시 비밀번호(아이디+1!):': {
      EN: 'Temporary password (ID+1!):',
      JP: '仮パスワード（ID+1!）:',
      CH: '临时密码（ID+1!）：',
      TH: 'รหัสผ่านชั่วคราว (ID+1!):'
    },
    /* /ops/opsMng 허브, /ops/mailLog thead·cells (STRING_MAP 보강) */
    '운영관리 그룹입니다. 운영 배치·점검·장애 대응 등 전용 화면을 여기에 둘 수 있습니다.': {
      EN: 'This is the Operations management group. You can place dedicated screens here for operational batch jobs, inspections, and incident response.',
      JP: '運用管理グループです。運用バッチ・点検・障害対応などの専用画面をここに置けます。',
      CH: '此为运营管理分组。可在此放置运营批处理、巡检与故障处理等专用界面。',
      TH: 'นี่คือกลุ่มการบริหารปฏิบัติการ สามารถวางหน้าจอเฉพาะสำหรับแบตช์ การตรวจสอบ และการตอบสนองเหตุขัดข้องได้ที่นี่'
    },
    '현재는 메뉴만 제공하며, 세부 기능은 이후 버전에서 연동합니다.': {
      EN: 'Only the menu is available for now; detailed features will be linked in a later version.',
      JP: '現時点ではメニューのみです。詳細機能は今後のバージョンで連携予定です。',
      CH: '目前仅提供菜单，详细功能将在后续版本中接入。',
      TH: 'ขณะนี้มีเฉพาะเมนู ฟีเจอร์ละเอียดจะเชื่อมในเวอร์ชันถัดไป'
    },
    'PG 연동·배포 참고 문서': {
      EN: 'PG integration & deployment reference',
      JP: 'PG連携・配布の参考ドキュメント',
      CH: 'PG 联动与部署参考文档',
      TH: 'เอกสารอ้างอิงการเชื่อม PG และการใช้งานจริง'
    },
    '아래 하위 메뉴는 배포설정의 동명 화면과 내용이 같습니다. JPAY 연동·가맹점 API·체크리스트 점검 시 활용하세요.': {
      EN: 'The submenus below mirror the same titles under Deployment settings with identical content. Use them when reviewing JPAY integration, merchant API rollout, and launch checklists.',
      JP: '以下のサブメニューはデプロイ設定の同名画面と内容が同じです。JPAY連携・加盟店API・チェックリスト点検にご利用ください。',
      CH: '以下子菜单与「部署设置」下同标题页面内容一致。JPAY 联动、商户 API、上线清单巡检时请使用。',
      TH: 'เมนูย่อยด้านล่างมีเนื้อหาเดียวกับหน้าชื่อเดียวกันในการตั้งค่า deploy ใช้ตรวจ JPAY การแจก Merchant API และเช็กลิสต์เปิดใช้'
    },
    '이메일 무효(거래)': {
      EN: 'Email void (transaction)',
      JP: 'メール無効（取引）',
      CH: '邮件作废（交易）',
      TH: 'อีเมลโมฆะ (ธุรกรรม)'
    },
    '이메일무효(거래)': {
      KO: '이메일 무효(거래)',
      EN: 'Email void (transaction)',
      JP: 'メール無効（取引）',
      CH: '邮件作废（交易）',
      TH: 'อีเมลโมฆะ (ธุรกรรม)'
    },
    'VOID 테스트': {
      EN: 'VOID test',
      JP: 'VOIDテスト',
      CH: 'VOID 测试',
      TH: 'ทดสอบ VOID'
    },
    '일시': {
      EN: 'Timestamp',
      JP: '日時',
      CH: '时间',
      TH: 'วันเวลา'
    },
    '상태': {
      EN: 'Status',
      JP: '状態',
      CH: '状态',
      TH: 'สถานะ'
    },
    '거래번호': {
      EN: 'Transaction no.',
      JP: '取引番号',
      CH: '交易号',
      TH: 'เลขธุรกรรม'
    },
    '본문 미리보기': {
      EN: 'Body preview',
      JP: '本文プレビュー',
      CH: '正文预览',
      TH: 'ตัวอย่างเนื้อหา'
    },
    '실행자': {
      EN: 'Actor',
      JP: '実行者',
      CH: '操作者',
      TH: 'ผู้ดำเนินการ'
    },
    '오류': {
      EN: 'Error',
      JP: 'エラー',
      CH: '错误',
      TH: 'ข้อผิดพลาด'
    },
    '수신': {
      EN: 'Recipient',
      JP: '宛先',
      CH: '收件人',
      TH: 'ผู้รับ'
    },
    '테스트 수신 이메일을 입력하세요.': {
      EN: 'Enter a test recipient email.',
      JP: 'テスト宛先メールアドレスを入力してください。',
      CH: '请输入测试收件邮箱。',
      TH: 'กรอกอีเมลผู้รับทดสอบ'
    },
    '테스트 메일 API를 사용할 수 없습니다.': {
      EN: 'The test mail API is not available.',
      JP: 'テストメールAPIを利用できません。',
      CH: '无法使用测试邮件 API。',
      TH: 'ไม่สามารถใช้ API จดหมายทดสอบได้'
    },
    '테스트 메일을 발송하시겠습니까?': {
      EN: 'Send a test email?',
      JP: 'テストメールを送信しますか？',
      CH: '要发送测试邮件吗？',
      TH: 'ส่งอีเมลทดสอบหรือไม่'
    },
    '서버에 이미 저장된 전산설정(SMTP·이메일무효 템플릿)으로 발송됩니다. 화면에만 입력하고 저장하지 않은 값은 반영되지 않습니다. 계속하시겠습니까?': {
      EN: 'The mail will be sent using ledger settings already saved on the server (SMTP and email-void template). Values typed on this screen but not saved will not be used. Continue?',
      JP: 'サーバーに保存済みの全算設定（SMTP・メール無効テンプレート）で送信します。画面上のみの入力で保存していない値は反映されません。続行しますか？',
      CH: '将使用服务器已保存的全算设置（SMTP 与邮件作废模板）发送。仅输入未保存的值不会生效。是否继续？',
      TH: 'จะส่งด้วยการตั้งค่าเลดเจอร์ที่บันทึกบนเซิร์ฟเวอร์แล้ว (SMTP และเทมเพลตอีเมลโมฆะ) ค่าที่พิมพ์ในหน้าจอแต่ยังไม่บันทึกจะไม่ถูกใช้ ดำเนินต่อหรือไม่'
    },
    '테스트 메일을 발송했습니다. 운영관리 → 메일로그에서 결과를 확인할 수 있습니다.': {
      EN: 'Test mail sent. Check the result under Operations → Mail log.',
      JP: 'テストメールを送信しました。結果は「運用管理 → メールログ」で確認できます。',
      CH: '测试邮件已发送。请在「运营管理 → 邮件日志」中查看结果。',
      TH: 'ส่งอีเมลทดสอบแล้ว ตรวจผลได้ที่ ปฏิบัติการ → บันทึกเมล'
    },
    /* TH tax report (/ops/taxReport), notices · grid · 검색 — 한글 키 = screens.js 문자열 그대로 */
    '형식: YYYY-MM-DD (예: 2026-05-09)': {
      EN: 'Format: YYYY-MM-DD (e.g. 2026-05-09)',
      JP: '書式: YYYY-MM-DD（例: 2026-05-09）',
      CH: '格式：YYYY-MM-DD（例：2026-05-09）',
      TH: 'รูปแบบ: YYYY-MM-DD (เช่น 2026-05-09)'
    },
    '보고구분': { EN: 'Report scope', JP: '報告区分', CH: '报表范围', TH: 'ขอบเขตรายงาน' },
    '귀속월': { EN: 'Attribution month', JP: '帰属月', CH: '归属月', TH: 'เดือนอ้างอิง' },
    '기간별(확정 정산 실행)': {
      EN: 'By period (confirmed runs)',
      JP: '期間別（確定精算実行）',
      CH: '按期间（已确认执行）',
      TH: 'ตามช่วง (รันที่ยืนยันแล้ว)'
    },
    '월 통합(귀속월)': {
      EN: 'Monthly roll-up',
      JP: '月次集約（帰属月）',
      CH: '按月汇总（归属月）',
      TH: 'รวมรายเดือน (เดือนอ้างอิง)'
    },
    '엑셀(xlsx)': {
      EN: 'Excel (xlsx)',
      JP: 'Excel(xlsx)',
      CH: 'Excel(xlsx)',
      TH: 'Excel (xlsx)'
    },
    '실행ID': { EN: 'Run ID', JP: '実行ID', CH: '执行 ID', TH: 'รหัสรัน' },
    '집계시작': { EN: 'Period from', JP: '集計開始', CH: '汇总开始', TH: 'เริ่มช่วง' },
    '집계종료': { EN: 'Period to', JP: '集計終了', CH: '汇总结束', TH: 'สิ้นช่วง' },
    '가맹명': { EN: 'Merchant name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้าน' },
    '가맹점명': { EN: 'Merchant name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้าน' },
    '가맹코드': { EN: 'Merchant code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้าน' },
    '거래건수': { EN: 'Txn count', JP: '取引件数', CH: '交易笔数', TH: 'จำนวนธุรกรรม' },
    '순매출': { EN: 'Net sales', JP: '純売上', CH: '净销售额', TH: 'ยอดขายสุทธิ' },
    '거래수수료합': {
      EN: 'Txn fees (total)',
      JP: '取引手数料合計',
      CH: '交易手续费合计',
      TH: 'รวมค่าธรรมเนียมธุรกรรม'
    },
    '정산 수수료': {
      EN: 'Settlement fee (per run)',
      JP: '精算手数料（実行単位）',
      CH: '结算手续费（按次）',
      TH: 'ค่าธรรมเนียมชำระบัญชี (ต่อรัน)'
    },
    '송금 수수료': {
      EN: 'Remittance fee',
      JP: '送金手数料',
      CH: '汇款手续费',
      TH: 'ค่าธรรมเนียมโอน'
    },
    '지급예정일': {
      EN: 'Payout due date',
      JP: '支払予定日',
      CH: '预计拨付日',
      TH: 'วันครบกำหนดจ่าย'
    },
    '보증금': {
      EN: 'Deposit (rolling reserve)',
      JP: '保証金（ローリング）',
      CH: '保证金（滚动）',
      TH: 'เงินประกัน (โรลลิง)'
    },
    '지급액(송금전)': {
      EN: 'Payout (before wire fee)',
      JP: '支払額（送金前）',
      CH: '拨付额（汇款前）',
      TH: 'ยอดจ่าย (ก่อนค่าธรรมเนียมโอน)'
    },
    '송금료(통화)': {
      EN: 'Wire fee (ccy)',
      JP: '送金料（通貨）',
      CH: '汇款费（币种）',
      TH: 'ค่าธรรมเนียมโอน (ตามสกุล)'
    },
    '송금료(USDT)': {
      EN: 'Wire fee (USDT)',
      JP: '送金料（USDT）',
      CH: '汇款费（USDT）',
      TH: 'ค่าธรรมเนียมโอน (USDT)'
    },
    '최종지급(은행기준)': {
      EN: 'Final payout (bank)',
      JP: '最終支払（銀行基準）',
      CH: '最终拨付（银行口径）',
      TH: 'จ่ายสุดท้าย (เกณฑ์ธนาคาร)'
    },
    'tb_settlement_run PK': {
      EN: 'tb_settlement_run primary key',
      JP: 'tb_settlement_run の主キー',
      CH: 'tb_settlement_run 主键',
      TH: 'คีย์หลัก tb_settlement_run'
    },
    '송금수수료 차감 전': {
      EN: 'Before remittance fee deduction',
      JP: '送金手数料控除前',
      CH: '扣减汇款手续费前',
      TH: 'ก่อนหักค่าธรรมเนียมโอน'
    },
    '세금·은행 대조용': {
      EN: 'Tax / bank reconciliation',
      JP: '税務・銀行照合用',
      CH: '税务/银行核对用',
      TH: 'ใช้กู้ภาษี/เทียบธนาคาร'
    },
    '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 다른 로그인은 목록이 비어 있거나 거부됩니다.': {
      EN: 'Only root HQ, regional HQ (REGIONAL), master distributor (MASTER_DIST), or ADMIN may use this screen. Other logins see an empty list or are denied.',
      JP: '総本部・本社(REGIONAL)・総販(MASTER_DIST) または ADMIN のみ利用できます。その他のログインでは一覧が空か拒否されます。',
      CH: '仅总总部、本部(REGIONAL)、总代(MASTER_DIST)或 ADMIN 可使用本画面；其他登录将看到空列表或被拒绝。',
      TH: 'ใช้ได้เฉพาะ HQ สูงสุด HQ ภูมิภาค(REGIONAL) ตัวแทนหลักหรือ ADMIN'
    },
    '로그인 조직 트리의 하위 가맹만 대상입니다(타 총판·타 본사 가맹 제외).': {
      EN: 'Only merchants under the logged-in org tree (excludes merchants under other distributors or other regional HQs).',
      JP: 'ログイン組織ツリー配下の加盟店のみが対象です（他総販・他本社配下は除く）。',
      CH: '仅限登录组织树下属商户（不含其他总代或其他本部下属）。',
      TH: 'เฉพาะร้านใต้องค์กรของผู้ล็อกอิน'
    },
    '행 원천: 확정정산(CALCULATED)·정산배포(DISTRIBUTED)·가맹점정산내역 노출 규칙을 통과한 정산 실행입니다.': {
      EN: 'Row source: settlement runs that passed merchant-statement visibility rules as CALCULATED and DISTRIBUTED.',
      JP: '行の元データ: 確定精算(CALCULATED)・精算配布(DISTRIBUTED)・加盟店精算明細の表示ルールを満たした精算実行です。',
      CH: '行来源：已通过商户结算明细展示规则的已确认(CALCULATED)、已下发(DISTRIBUTED)结算执行。',
      TH: 'แถวจากรันที่ผ่านกฎ CALCULATED/DISTRIBUTED'
    },
    '「월 통합」은 귀속월(YYYY-MM) 전체를 한 번에 조회합니다. 엑셀에는 실행 목록·TOTAL·가맹별 합계가 포함됩니다.': {
      EN: 'Monthly roll-up loads the entire attribution month (YYYY-MM) at once. Excel includes run lines, TOTAL, and per-merchant subtotals.',
      JP: '「月次集約」は帰属月(YYYY-MM)全体を一度に照会します。Excel には実行一覧・TOTAL・加盟店別集計が含まれます。',
      CH: '「按月汇总」一次性查询归属月（YYYY-MM）全月。Excel 含执行明细、TOTAL 与商户小计。',
      TH: 'โหมดรวมเดือนดึงทั้งเดือน YYYY-MM'
    },
    'FinalPayAfterRemittance는 송금 수수료 반영 후 지급 기준액으로, 실제 은행 송금과 일치시키는 용도로 검증하세요.': {
      EN: 'FinalPayAfterRemittance is the bank-alignment payout after remittance fees; use it to reconcile actual bank transfers.',
      JP: 'FinalPayAfterRemittance は送金手数料反映後の支払基準額で、実際の銀行送金との照合にご利用ください。',
      CH: 'FinalPayAfterRemittance 为扣减汇款手续费后的拨付基准金额，可与实际银行汇款核对。',
      TH: 'FinalPayAfterRemittance ใช้เทียบยอดโอนจริงหลังค่าธรรมเนียม'
    },
    '발송 실패': {
      EN: 'Send failed',
      JP: '送信に失敗しました',
      CH: '发送失败',
      TH: 'ส่งไม่สำเร็จ'
    },
    /* /main dashboard (pg-home-dashboard.js) */
    '영업일 달력': { EN: 'Business-day calendar', JP: '営業日カレンダー', CH: '营业日日历', TH: 'ปฏิทินวันทำการ' },
    '지난달·이번달·다음달 (3개월)': {
      EN: 'Last · this · next month (3 months)',
      JP: '先月・今月・翌月（3か月）',
      CH: '上月·本月·下月（3个月）',
      TH: 'เดือนที่แล้ว·เดือนนี้·เดือนหน้า (3 เดือน)'
    },
    '이전 3개월': { EN: 'Previous 3 months', JP: '前の3か月', CH: '前3个月', TH: 'ย้อน 3 เดือน' },
    '다음 3개월': { EN: 'Next 3 months', JP: '次の3か月', CH: '后3个月', TH: 'ถัดไป 3 เดือน' },
    '영업일 설정': { EN: 'Business-day settings', JP: '営業日設定', CH: '营业日设置', TH: 'ตั้งค่าวันทำการ' },
    '총본사 기준': { EN: 'HQ default', JP: '総本部基準', CH: '总本部基准', TH: 'มาตรฐานสำนักงานใหญ่' },
    '지정': { EN: 'Set', JP: '指定', CH: '指定', TH: 'กำหนด' },
    '이 영업일 설정을 총본사 기준(메인 영업일 달력)으로 지정하시겠습니까?': {
      EN: 'Set this business-day profile as the HQ default (main calendar)?',
      JP: 'この営業日設定を総本部基準（メイン営業日カレンダー）に指定しますか？',
      CH: '是否将此营业日设置指定为总本部基准（主营业日日历）？',
      TH: 'กำหนดการตั้งค่าวันทำการนี้เป็นมาตรฐานสำนักงานใหญ่ (ปฏิทินหลัก) หรือไม่?'
    },
    '총본사 기준 영업일로 지정되었습니다.': {
      EN: 'Set as HQ default business-day calendar.',
      JP: '総本部基準の営業日として指定しました。',
      CH: '已指定为总本部基准营业日。',
      TH: 'กำหนดเป็นวันทำการมาตรฐานสำนักงานใหญ่แล้ว'
    },
    '총본사 기준 영업일로 지정되었습니다. 메인 영업일 달력을 갱신했습니다.': {
      EN: 'Set as HQ default business-day calendar. The main business-day calendar has been refreshed.',
      JP: '総本部基準の営業日として指定し、メイン営業日カレンダーを更新しました。',
      CH: '已指定为总本部基准营业日，并已刷新主营业日日历。',
      TH: 'กำหนดเป็นวันทำการมาตรฐานสำนักงานใหญ่และอัปเดตปฏิทินหลักแล้ว'
    },
    '기준 프로필': { EN: 'Profile', JP: '基準プロファイル', CH: '基准配置', TH: 'โปรไฟล์อ้างอิง' },
    '기준국가': { EN: 'Country', JP: '基準国', CH: '基准国家', TH: 'ประเทศอ้างอิง' },
    '영업일 수': { EN: 'Business days', JP: '営業日数', CH: '营业日数', TH: 'จำนวนวันทำการ' },
    '영업일': { EN: 'Business day', JP: '営業日', CH: '营业日', TH: 'วันทำการ' },
    '휴일·주말': { EN: 'Holiday / weekend', JP: '休日・週末', CH: '假日·周末', TH: 'วันหยุด·สุดสัปดาห์' },
    '영업일 달력을 불러오는 중…': {
      EN: 'Loading business-day calendar…',
      JP: '営業日カレンダーを読み込み中…',
      CH: '正在加载营业日日历…',
      TH: 'กำลังโหลดปฏิทินวันทำการ…'
    },
    '영업일 달력 조회 실패': {
      EN: 'Failed to load business-day calendar.',
      JP: '営業日カレンダーの取得に失敗しました。',
      CH: '营业日日历查询失败。',
      TH: 'โหลดปฏิทินวันทำการไม่สำเร็จ'
    },
    '승인': { EN: 'Approved', JP: '承認', CH: '授权', TH: 'อนุมัติ' },
    '전체': { EN: 'All', JP: 'すべて', CH: '全部', TH: 'ทั้งหมด' },
    '건': { EN: '', JP: '件', CH: '笔', TH: 'รายการ' },
    '해당 기간 거래가 없습니다.': {
      EN: 'No transactions in this period.',
      JP: '該当期間の取引はありません。',
      CH: '该期间没有交易。',
      TH: 'ไม่มีธุรกรรมในช่วงเวลานี้'
    },
    '통화별': { EN: 'By currency', JP: '通貨別', CH: '按币种', TH: 'ตามสกุลเงิน' },
    '오늘': { EN: 'Today', JP: '本日', CH: '今天', TH: 'วันนี้' },
    '최근 7일': { EN: 'Last 7 days', JP: '直近7日', CH: '最近7天', TH: '7 วันที่ผ่านมา' },
    '최근 30일': { EN: 'Last 30 days', JP: '直近30日', CH: '最近30天', TH: '30 วันที่ผ่านมา' },
    '서버 트래픽 요약을 사용할 수 없습니다.': {
      EN: 'Server traffic summary is unavailable.',
      JP: 'サーバートラフィック要約を利用できません。',
      CH: '无法使用服务器流量摘要。',
      TH: 'ไม่สามารถใช้สรุปทราฟฟิกเซิร์ฟเวอร์ได้'
    },
    '수집된 서버 사용량 데이터가 없습니다.': {
      EN: 'No server usage data collected.',
      JP: '収集されたサーバー使用量データがありません。',
      CH: '没有收集到服务器使用量数据。',
      TH: 'ไม่มีข้อมูลการใช้งานเซิร์ฟเวอร์ที่เก็บไว้'
    },
    '표시할 정산 실행 이력이 없습니다.': {
      EN: 'No settlement runs to display.',
      JP: '表示できる精算実行履歴がありません。',
      CH: '没有可显示的结算执行记录。',
      TH: 'ไม่มีประวัติรันชำระบัญชีให้แสดง'
    },
    '승인합': { EN: 'Approved sum', JP: '承認合計', CH: '授权合计', TH: 'รวมอนุมัติ' },
    '포함건수': { EN: 'Included count', JP: '含む件数', CH: '包含笔数', TH: 'จำนวนที่รวม' },
    '주기': { EN: 'Cycle', JP: '周期', CH: '周期', TH: 'รอบ' },
    '번호': {
      EN: 'No.',
      JP: '番号',
      CH: '序号',
      TH: 'ลำดับ'
    },
    '조회 실패': {
      EN: 'Search failed',
      JP: '検索に失敗しました',
      CH: '查询失败',
      TH: 'ค้นหาไม่สำเร็จ'
    },
    '날짜 형식이 올바르지 않습니다. (YYYY-MM-DD)': {
      EN: 'Invalid date format. Use YYYY-MM-DD.',
      JP: '日付形式が正しくありません。(YYYY-MM-DD)',
      CH: '日期格式不正确。(YYYY-MM-DD)',
      TH: 'รูปแบบวันที่ไม่ถูกต้อง (YYYY-MM-DD)'
    },
    '이전 페이지 구간': {
      EN: 'Previous page range',
      JP: '前のページ範囲',
      CH: '上一页区间',
      TH: 'ช่วงหน้าก่อน'
    },
    '다음 페이지 구간': {
      EN: 'Next page range',
      JP: '次のページ範囲',
      CH: '下一页区间',
      TH: 'ช่วงหน้าถัดไป'
    },
    '입력 후 [저장]으로 확정한 뒤 하단 [수정 저장]으로 반영하세요.': {
      EN: 'Confirm with [Save] beside the field, then apply with [Save changes] at the bottom.',
      JP: '横の[保存]で確定後、下部の[修正保存]で反映してください。',
      CH: '先在旁侧【保存】确认，再通过底部【保存修改】应用。',
      TH: 'กด [บันทึก] ข้างช่องเพื่อยืนยัน แล้วใช้ [บันทึกการแก้ไข] ด้านล่างเพื่อนำไปใช้'
    },
    '보조 비밀번호 초기화': {
      EN: 'Reset assistant password',
      JP: '補助パスワード初期化',
      CH: '重置辅助密码',
      TH: 'รีเซ็ตรหัสผ่านผู้ช่วย'
    },
    '비밀번호 초기화': {
      EN: 'Reset password',
      JP: 'パスワード初期化',
      CH: '重置密码',
      TH: 'รีเซ็ตรหัสผ่าน'
    },
    '번호 입력': { EN: 'Enter number', JP: '番号入力', CH: '输入号码', TH: 'ป้อนหมายเลข' },
    'SMS수신': { EN: 'SMS', JP: 'SMS受信', CH: '接收短信', TH: 'SMS' },
    '파일명 (예: 사업자등록증)': {
      EN: 'Display name (e.g. business registration)',
      JP: '表示名（例：登記簿謄本）',
      CH: '显示名称（如营业执照）',
      TH: 'ชื่อแสดง (เช่น ทะเบียนธุรกิจ)'
    },
    '파일 선택': { EN: 'Choose file', JP: 'ファイル選択', CH: '选择文件', TH: 'เลือกไฟล์' },
    '파일이름': { EN: 'File name', JP: 'ファイル名', CH: '文件名', TH: 'ชื่อไฟล์' },
    '첨부된 파일': { EN: 'Attached file', JP: '添付ファイル', CH: '已附文件', TH: 'ไฟล์แนบ' },
    '수정': { EN: 'Edit', JP: '修正', CH: '修改', TH: 'แก้ไข' },
    '상세': { EN: 'Detail', JP: '詳細', CH: '详情', TH: 'รายละเอียด' },
    '첨부된 파일이 없습니다.': {
      EN: 'No attachments.',
      JP: '添付ファイルがありません。',
      CH: '暂无附件。',
      TH: 'ยังไม่มีไฟล์แนบ'
    },
    '허용 파일: 이미지, PDF, 문서 파일(doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx)': {
      EN: 'Allowed: images, PDF, documents (doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx).',
      JP: '許可: 画像、PDF、文書(doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx)。',
      CH: '允许：图片、PDF、文档（doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx）。',
      TH: 'อนุญาต: รูป PDF เอกสาร (doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx)'
    },
    '헤더 1행은 <strong>수수료 고정</strong>·<strong>수수료 %</strong>·<strong>담보율</strong>·<strong>기타</strong> 묶음입니다. <strong>수수료 %</strong> 열은 숫자만 표시(단위 % 생략). 결제·USDT·FX는 승인금액 기준 %이며, <strong>3DS</strong>는 정책통화 기준 <strong>건당 고정</strong>입니다. 담보(롤링) 비율은 승인금액 기준 %입니다. 열이 많아 표에 <strong>최소 너비</strong>를 두었으며, 화면이 좁으면 아래 표 영역을 <strong>가로 스크롤</strong>하여 전체 열을 볼 수 있습니다.': {
      EN: 'Row 1 groups <strong>fixed fees</strong>, <strong>fee %</strong>, <strong>collateral %</strong>, and <strong>other</strong>. The <strong>fee %</strong> columns show numbers only (% unit omitted). Pay·USDT·FX are % of approved amount; <strong>3DS</strong> is a fixed per-txn amount in policy currency. Rolling collateral is % of approved amount. Many columns use a <strong>minimum width</strong>; if the screen is narrow, <strong>scroll horizontally</strong> in the table area to see all.',
      JP: '1行目は<strong>手数料固定</strong>・<strong>手数料%</strong>・<strong>担保率</strong>・<strong>その他</strong>のまとまりです。<strong>手数料%</strong>列は数値のみ（%表記省略）。決済・USDT・FXは承認金額基準%、<strong>3DS</strong>は政策通貨基準の<strong>件当固定</strong>です。担保（ローリング）比率は承認金額基準%。列が多いため表に<strong>最小幅</strong>を設け、画面が狭いときは下の表領域を<strong>横スクロール</strong>して全列を表示できます。',
      CH: '第 1 行表头为<strong>手续费固定</strong>、<strong>手续费%</strong>、<strong>担保比例</strong>、<strong>其他</strong>分组。<strong>手续费%</strong>列仅显示数字（省略 %）。支付·USDT·FX 为按批准金额 %；<strong>3DS</strong> 为政策货币<strong>按笔固定</strong>。担保（滚动）比例为批准金额 %。列较多，表设<strong>最小宽度</strong>；屏幕较窄时请在表区域<strong>横向滚动</strong>查看全部列。',
      TH: 'แถวหัว 1 จัดกลุ่ม<strong>ค่าธรรมเนียมคงที่</strong>·<strong>% ค่าธรรมเนียม</strong>·<strong>% หลักประกัน</strong>·<strong>อื่นๆ</strong> คอลัมน์<strong>% ค่าธรรมเนียม</strong>แสดงตัวเลขอย่างเดียว (ไม่มีเครื่องหมาย %) ชำระ·USDT·FX เป็น % ของยอดที่อนุมัติ <strong>3DS</strong> เป็นคงที่<strong>ต่อรายการ</strong>ตามสกุลเงินนโยบาย หลักประกัน(โรลลิง) เป็น % ของยอดอนุมัติ มีคอลัมน์มากจึงตั้ง<strong>ความกว้างขั้นต่ำ</strong> หน้าจอแคบให้<strong>เลื่อนแนวนอน</strong>ในพื้นที่ตาราง'
    },
    '<strong>일반형</strong>은 표시·실결제 통화·FX·수동 환산·행 마진을 쓰지 않으며, 결제는 해당 <strong>총판(조직)에 설정된 통화</strong>로 진행됩니다(이 화면에서 선택 불가). <strong>DISPLAY</strong>·<strong>BLIND</strong>일 때 금액 모드·<strong>결제 방식(고정/멀티)</strong>·표시 통화·실결제 통화·FX를 PG별로 설정합니다. <strong>멀티</strong>이면 표의 <strong>표시 통화</strong> 칸은 비활성화되며, 공개 결제 페이지에서 고객이 통화를 고릅니다. <strong>BLIND+고정</strong>이면 공개 결제창에서 표시 통화 행·청구예상(환산) 행을 숨기고, <strong>BLIND+멀티</strong>이면 고객이 표시 통화·금액을 고른 뒤에도 <strong>청구예상(실결제 통화 환산)</strong>만 숨기며 견적·결제는 DISPLAY와 동일합니다. <strong>FX 자동(BOT)</strong>이면 아래 <strong>표시통화별 마진(7종)</strong>만 마진으로 쓰이고, 행의 <strong>수동 실결제/1표시</strong>·<strong>PG별 마진율</strong>은 비활성입니다. <strong>FX 수동</strong>이면 해당 행에서 실결제/1표시와 마진율을 직접 입력합니다. <strong>고정</strong>이면 공개 결제 페이지에는 <strong>표시 통화</strong>만 노출되고(셀렉트 없음), <strong>멀티</strong>이면 고객이 표시 통화를 고를 수 있는 셀렉트가 나옵니다(멀티 시 선택지는 본사 전역 순서이며, JSON에서 <code>displayCurrencies</code> 배열로 줄일 수 있음). 아래 <strong>표시→실결제(FX) 기능</strong>·견적 주기·<strong>BOT 환율 기준일</strong>·<strong>마진(표시통화별)</strong>은 DISPLAY·BLIND 모드를 쓰는 PG가 <strong>하나라도 있을 때만</strong> 활성화됩니다. BOT는 방콕 달력일당·모드당 <strong>서버에서 1회만</strong> 조회합니다. 실결제 통화가 표시와 다를 때는 BOT 일평균(THB 경유)으로 환산하고, <strong>표시=실결제</strong>이면 1:1입니다.': {
      EN: '<strong>STANDARD</strong> does not use display/settlement currency, FX, manual conversion, or row margins; checkout runs in the <strong>currency set on the distributor (org)</strong> (not selectable here). For <strong>DISPLAY</strong> / <strong>BLIND</strong>, set amount mode, <strong>checkout currency mode (fixed/multi)</strong>, display/settlement currency, and FX per PG. <strong>Multi</strong> disables the table’s <strong>display currency</strong> cell; customers pick currency on the public checkout page. <strong>BLIND + fixed</strong> hides the display-currency row and the settlement estimate row; <strong>BLIND + multi</strong> hides only the <strong>settlement estimate</strong> after the customer chooses currency and amount—quotes and payment match DISPLAY. <strong>FX auto (BOT)</strong> uses only the <strong>seven display-currency margins</strong> below; row <strong>manual settlement per display unit</strong> and <strong>per-PG margin rate</strong> are disabled. <strong>FX manual</strong> lets you enter settlement per display unit and margin on each row. <strong>Fixed</strong> shows a single display currency (no select); <strong>multi</strong> shows a currency select (order follows HQ defaults; narrow with JSON <code>displayCurrencies</code>). <strong>Display→settlement (FX)</strong>, quote cadence, <strong>BOT anchor date</strong>, and <strong>margins by display currency</strong> enable only when <strong>at least one</strong> PG uses DISPLAY or BLIND. BOT is fetched <strong>once per server</strong> per Bangkok calendar day and mode. When settlement ≠ display, conversion uses BOT daily average via THB; <strong>display = settlement</strong> is 1:1.',
      JP: '<strong>一般型</strong>は表示・実決済通貨・FX・手動換算・行マージンを使わず、決済は<strong>総販（組織）に設定された通貨</strong>で進みます（この画面では選択不可）。<strong>DISPLAY</strong>・<strong>BLIND</strong>では金額モード・<strong>決済方式（固定/マルチ）</strong>・表示通貨・実決済通貨・FXをPGごとに設定します。<strong>マルチ</strong>のとき表の<strong>表示通貨</strong>欄は無効化され、公開決済ページで顧客が通貨を選びます。<strong>BLIND+固定</strong>では表示通貨行・請求見積（換算）行を非表示、<strong>BLIND+マルチ</strong>では表示通貨・金額選択後も<strong>請求見積（実決済換算）</strong>のみ非表示にし、見積・決済はDISPLAYと同じです。<strong>FX自動（BOT）</strong>では下の<strong>表示通貨別マージン（7種）</strong>のみをマージンに使い、行の<strong>手動実決済/1表示</strong>・<strong>PG別マージン率</strong>は無効です。<strong>FX手動</strong>では行で実決済/1表示とマージン率を直接入力します。<strong>固定</strong>では公開ページに<strong>表示通貨</strong>のみ（セレクトなし）、<strong>マルチ</strong>では表示通貨セレクトが出ます（選択肢は本社既定順、JSONの<code>displayCurrencies</code>で絞れます）。下の<strong>表示→実決済（FX）</strong>・見積周期・<strong>BOTレート基準日</strong>・<strong>マージン（表示通貨別）</strong>はDISPLAY・BLINDを使うPGが<strong>1件でもあるときだけ</strong>有効です。BOTはバンコク暦日・モードごとに<strong>サーバで1回だけ</strong>取得します。実決済が表示と異なるときはBOT日平均（THB経由）で換算、<strong>表示=実決済</strong>なら1:1です。',
      CH: '<strong>标准型</strong>不使用展示/结算货币、FX、手动换算与行级边距；结算按<strong>总代（组织）配置的货币</strong>进行（本页不可选）。<strong>DISPLAY</strong>、<strong>BLIND</strong>时按 PG 设置金额模式、<strong>币种方式（固定/多选）</strong>、展示/结算货币与 FX。<strong>多选</strong>时表格<strong>展示货币</strong>列禁用，客户在公开支付页选择货币。<strong>BLIND+固定</strong>隐藏展示货币行与<strong>预计扣款（换算）</strong>行；<strong>BLIND+多选</strong>在客户选定展示货币与金额后仅隐藏<strong>预计扣款（结算货币换算）</strong>，报价与支付与 DISPLAY 相同。<strong>FX 自动（BOT）</strong>仅使用下方<strong>七种展示货币边距</strong>；行的<strong>手动结算/每展示单位</strong>与<strong>PG 边距率</strong>禁用。<strong>FX 手动</strong>在各行填写结算/展示与边距率。<strong>固定</strong>仅展示单一展示货币（无下拉）；<strong>多选</strong>显示货币下拉（顺序默认总部，可用 JSON <code>displayCurrencies</code> 收窄）。<strong>展示→结算（FX）</strong>、报价周期、<strong>BOT 基准日</strong>、<strong>按展示货币的边距</strong>仅在至少一个 PG 使用 DISPLAY 或 BLIND 时启用。BOT 按曼谷日历日与模式<strong>每服务器只查一次</strong>。结算≠展示时用 BOT 日均价经 THB 换算；<strong>展示=结算</strong>为 1:1。',
      TH: '<strong>มาตรฐาน</strong> ไม่ใช้สกุลแสดง/ชำระจริง FX แปลงมือและมาร์จิ้นรายแถว — ชำระตาม<strong>สกุลที่ตั้งที่ตัวแทนหลัก (องค์กร)</strong> (เลือกในหน้านี้ไม่ได้) สำหรับ <strong>DISPLAY</strong>/<strong>BLIND</strong> ตั้งโหมดจำนวน <strong>วิธีสกุล (คงที่/หลายสกุล)</strong> สกุลแสดง·ชำระจริง·FX ต่อ PG <strong>หลายสกุล</strong> ปิดช่อง<strong>สกุลแสดง</strong>ในตาราง ลูกค้าเลือกสกุลในหน้าชำระสาธารณะ <strong>BLIND+คงที่</strong> ซ่อนแถวสกุลแสดงและแถบประมาณการเรียกเก็บ <strong>BLIND+หลายสกุล</strong> ซ่อนเฉพาะ<strong>ประมาณการ (แปลงเป็นสกุลชำระจริง)</strong> หลังเลือก — ใบเสนอราคาและการชำระเหมือน DISPLAY <strong>FX อัตโนมัติ (BOT)</strong> ใช้เฉพาะ<strong>มาร์จิ้น 7 สกุลแสดง</strong>ด้านล่าง ปิด<strong>ชำระจริงต่อหน่วยแสดงแบบมือ</strong>และ<strong>อัตรามาร์จิ้นต่อ PG</strong> <strong>FX มือ</strong> กรอกในบรรทัด สำหรับ<strong>คงที่</strong> แสดงสกุลเดียว (ไม่มีดรอปดาวน์) <strong>หลายสกุล</strong> มีดรอปดาวน์ (ลำดับตาม HQ จำกัดด้วย <code>displayCurrencies</code>) <strong>แสดง→ชำระจริง (FX)</strong> รอบใบเสนอ <strong>วันที่อ้างอิง BOT</strong> <strong>มาร์จิ้นตามสกุลแสดง</strong> เปิดเมื่อมี PG ใช้ DISPLAY หรือ BLIND <strong>อย่างน้อยหนึ่งรายการ</strong> BOT ดึง<strong>ครั้งเดียวต่อเซิร์ฟเวอร์</strong>ต่อวันปฏิทินกรุงเทพและโหมด สกุลชำระจริง ≠ สกุลแสดง ใช้ค่าเฉลี่ย BOT ผ่าน THB <strong>แสดง=ชำระจริง</strong> คือ 1:1'
    },
    '연동용도 URL결제 PG 목록을 불러옵니다. 저장 시 <code>tb_hq_api_config.url_pay_display_fx_json</code>에 반영됩니다.': {
      EN: 'Loads URL-pay PG rows by integration scope. On save, values are written to <code>tb_hq_api_config.url_pay_display_fx_json</code>.',
      JP: '連携用途がURL決済のPG一覧を読み込みます。保存時に<code>tb_hq_api_config.url_pay_display_fx_json</code>へ反映されます。',
      CH: '按联动用途加载 URL 支付 PG 列表。保存时写入 <code>tb_hq_api_config.url_pay_display_fx_json</code>。',
      TH: 'โหลดรายการ PG ชำระ URL ตามขอบเขตการเชื่อมต่อ บันทึกลง <code>tb_hq_api_config.url_pay_display_fx_json</code>'
    },
    '목록을 불러오는 중…': {
      EN: 'Loading list…',
      JP: '一覧を読み込み中…',
      CH: '正在加载列表…',
      TH: 'กำลังโหลดรายการ…'
    },
    'JSON 편집은 <strong>본사설정 &gt; URL결제설정</strong>에서 합니다. 아래 숨김 필드는 결제로직설정 저장 시 기존 값이 유지되도록 동기화됩니다.': {
      EN: 'Edit the JSON under <strong>HQ settings &gt; URL payment settings</strong>. The hidden fields below stay in sync so existing values are kept when you save <strong>Payment orchestration</strong>.',
      JP: '<strong>本社設定 &gt; URL決済設定</strong>でJSONを編集します。下の非表示フィールドは<strong>決済ロジック設定</strong>保存時に既存値が維持されるよう同期されます。',
      CH: '请在<strong>总部设置 &gt; URL 支付设置</strong>中编辑 JSON。下方隐藏字段会在保存<strong>支付编排</strong>时同步，以保留已有值。',
      TH: 'แก้ JSON ที่<strong>ส่วนตั้งค่า HQ &gt; ตั้งค่าชำระ URL</strong> ช่องซ่อนด้านล่างซิงค์เพื่อคงค่าเดิมเมื่อบันทึก<strong>ลำดับการชำระ</strong>'
    },
    '표시→실결제(FX) 기능': {
      EN: 'Display→settlement (FX)',
      JP: '表示→実決済（FX）',
      CH: '展示→实结（FX）',
      TH: 'แสดง→ชำระจริง (FX)'
    },
    '견적 갱신(초)': {
      EN: 'Quote refresh (sec)',
      JP: '見積更新（秒）',
      CH: '报价刷新（秒）',
      TH: 'รีเฟรชใบเสนอ (วินาที)'
    },
    '견적 TTL(초)': {
      EN: 'Quote TTL (sec)',
      JP: '見積TTL（秒）',
      CH: '报价 TTL（秒）',
      TH: 'TTL ใบเสนอ (วินาที)'
    },
    'BOT 환율 기준일': {
      EN: 'BOT rate anchor date',
      JP: 'BOT為替レート基準日',
      CH: 'BOT 汇率基准日',
      TH: 'วันที่อ้างอิงอัตรา BOT'
    },
    '전일 종가(방콕)': {
      EN: 'Prior-day close (Bangkok)',
      JP: '前日終値（バンコク）',
      CH: '前日收盘（曼谷）',
      TH: 'ราคาปิดวันก่อน (กรุงเทพ)'
    },
    '당일·최신 고시일': {
      EN: 'Same-day / latest BOT period',
      JP: '当日・最新公表日',
      CH: '当日/最新公布日',
      TH: 'วันนี้ / รอบล่าสุดของ BOT'
    },
    '마진 JPY(표시)': {
      EN: 'Margin JPY (display)',
      JP: 'マージン JPY（表示）',
      CH: '保证金 JPY（展示）',
      TH: 'มาร์จิ้น JPY (แสดง)'
    },
    '마진 USD(표시)': {
      EN: 'Margin USD (display)',
      JP: 'マージン USD（表示）',
      CH: '保证金 USD（展示）',
      TH: 'มาร์จิ้น USD (แสดง)'
    },
    '마진 KRW(표시)': {
      EN: 'Margin KRW (display)',
      JP: 'マージン KRW（表示）',
      CH: '保证金 KRW（展示）',
      TH: 'มาร์จิ้น KRW (แสดง)'
    },
    '마진 THB(표시)': {
      EN: 'Margin THB (display)',
      JP: 'マージン THB（表示）',
      CH: '保证金 THB（展示）',
      TH: 'มาร์จิ้น THB (แสดง)'
    },
    '마진 SGD(표시)': {
      EN: 'Margin SGD (display)',
      JP: 'マージン SGD（表示）',
      CH: '保证金 SGD（展示）',
      TH: 'มาร์จิ้น SGD (แสดง)'
    },
    '마진 HKD(표시)': {
      EN: 'Margin HKD (display)',
      JP: 'マージン HKD（表示）',
      CH: '保证金 HKD（展示）',
      TH: 'มาร์จิ้น HKD (แสดง)'
    },
    '마진 CNY(표시)': {
      EN: 'Margin CNY (display)',
      JP: 'マージン CNY（表示）',
      CH: '保证金 CNY（展示）',
      TH: 'มาร์จิ้น CNY (แสดง)'
    },
    'URL결제설정': {
      EN: 'URL payment settings',
      JP: 'URL決済設定',
      CH: 'URL 支付设置',
      TH: 'ตั้งค่าชำระ URL'
    },
    '금액 모드': {
      EN: 'Amount mode',
      JP: '金額モード',
      CH: '金额模式',
      TH: 'โหมดจำนวนเงิน'
    },
    '결제 방식': {
      EN: 'Checkout mode (fixed / multi)',
      JP: '決済方式（固定／マルチ）',
      CH: '支付方式（固定/多选）',
      TH: 'โหมดชำระ (คงที่/หลายสกุล)'
    },
    '표시 통화': {
      EN: 'Display currency',
      JP: '表示通貨',
      CH: '展示货币',
      TH: 'สกุลแสดง'
    },
    '실결제': {
      EN: 'Settlement currency',
      JP: '実決済通貨',
      CH: '结算货币',
      TH: 'สกุลชำระจริง'
    },
    '마진율': {
      EN: 'Margin rate',
      JP: 'マージン率',
      CH: '边距率',
      TH: 'อัตรามาร์จิ้น'
    },
    '수동 실결제/1표시': {
      EN: 'Manual settlement per 1 display unit',
      JP: '手動実決済／1表示単位',
      CH: '手动结算/每展示单位',
      TH: 'ชำระจริงแบบมือต่อ 1 หน่วยแสดง'
    },
    '수동': {
      EN: 'Manual',
      JP: '手動',
      CH: '手动',
      TH: 'แบบมือ'
    },
    'FX': {
      EN: 'FX',
      JP: 'FX',
      CH: 'FX',
      TH: 'FX'
    },
    '설정을 불러온 뒤 다시 시도하세요.': {
      EN: 'Load settings first, then try again.',
      JP: '設定を読み込んでから再度お試しください。',
      CH: '请先加载设置后再试。',
      TH: 'โหลดการตั้งค่าก่อน แล้วลองอีกครั้ง'
    },
    '일반형': {
      EN: 'Standard',
      JP: '一般型',
      CH: '标准型',
      TH: 'มาตรฐาน'
    },
    '멀티': {
      EN: 'Multi',
      JP: 'マルチ',
      CH: '多选',
      TH: 'หลายสกุล'
    },
    '자동(BOT)': {
      EN: 'Auto (BOT)',
      JP: '自動（BOT）',
      CH: '自动（BOT）',
      TH: 'อัตโนมัติ (BOT)'
    },
    '실결제/1표시': {
      EN: 'Settlement per 1 display unit',
      JP: '実決済／1表示単位',
      CH: '结算/每展示单位',
      TH: 'ชำระจริงต่อ 1 หน่วยแสดง'
    },
    'PG전체 마진(선택)': {
      EN: 'Per-PG margin (optional)',
      JP: 'PG全体マージン（任意）',
      CH: 'PG 整体边距（可选）',
      TH: 'มาร์จิ้นต่อ PG (ไม่บังคับ)'
    },
    '일반형: 총판(조직) 설정 통화로 결제되며 이 항목은 적용되지 않습니다.': {
      EN: 'STANDARD: checkout uses the distributor (org) currency; this field does not apply.',
      JP: '一般型：総販（組織）の設定通貨で決済され、この項目は適用されません。',
      CH: '标准型：按总代（组织）设定货币结算；此项不适用。',
      TH: 'มาตรฐาน: ชำระตามสกุลที่ตั้งที่ตัวแทนหลัก (องค์กร) — ช่องนี้ไม่ใช้'
    },
    '자동(BOT)이면 BOT·표시/실결제 통화로 실결제/1표시가 정해지므로 이 칸은 사용하지 않습니다. 수동 환율·행 마진을 쓰려면 FX에서 「수동」을 선택하세요.': {
      EN: 'Auto (BOT) derives settlement per display unit from BOT and display/settlement currencies; this field is unused. Choose FX "Manual" for manual rates and row margins.',
      JP: '自動（BOT）ではBOTと表示/実決済通貨から実決済/1表示が決まるためこの欄は使いません。手動レート・行マージンにはFXで「手動」を選んでください。',
      CH: '自动（BOT）时由 BOT 与展示/结算货币决定每展示单位结算；此栏不用。若用手动汇率与行边距，请在 FX 中选择「手动」。',
      TH: 'อัตโนมัติ (BOT) คำนวณชำระต่อหน่วยแสดงจาก BOT และสกุลแสดง/ชำระ — ช่องนี้ไม่ใช้ เลือก FX เป็น "Manual" เพื่ออัตราและมาร์จิ้นรายแถวแบบมือ'
    },
    '자동(BOT)이면 위 표시통화별 마진(JPY·USD·… 7종)만 적용됩니다. PG별 마진율은 FX 「수동」일 때만 입력합니다.': {
      EN: 'Auto (BOT) applies only the seven display-currency margins above. Enter per-PG margin only when FX is "Manual".',
      JP: '自動（BOT）では上の表示通貨別マージン（7通貨）のみ適用されます。PG別マージン率はFXが「手動」のときだけ入力します。',
      CH: '自动（BOT）仅使用上方七种展示货币边距。仅在 FX 为「手动」时填写 PG 边距率。',
      TH: 'อัตโนมัติ (BOT) ใช้เฉพาะมาร์จิ้น 7 สกุลแสดงด้านบน กรอกมาร์จิ้นต่อ PG เมื่อ FX เป็น "Manual" เท่านั้น'
    },
    '결제 방식이 멀티일 때는 공개 결제 페이지에서 고객이 표시 통화를 고릅니다. 이 표의 표시 통화 칸은 사용하지 않습니다.': {
      EN: 'When checkout currency mode is multi, customers pick display currency on the public checkout page; this table’s display-currency cell is not used.',
      JP: '決済方式がマルチのときは公開決済ページで顧客が表示通貨を選びます。この表の表示通貨欄は使いません。',
      CH: '多选时客户在公开支付页选择展示货币；本表的展示货币列不使用。',
      TH: 'โหมดหลายสกุล ลูกค้าเลือกสกุลแสดงในหน้าชำระสาธารณะ — ช่องสกุลแสดงในตารางนี้ไม่ใช้'
    },
    'DISPLAY 모드 PG가 없어 비활성화됩니다. 일반형만 있으면 총판 통화로 결제됩니다.': {
      EN: 'Disabled because no PG uses DISPLAY/BLIND. STANDARD-only checkout uses distributor currency.',
      JP: 'DISPLAY/BLINDのPGがないため無効です。一般型のみの場合は総販通貨で決済されます。',
      CH: '没有 DISPLAY/BLIND 模式的 PG 时禁用。仅标准型时按总代货币结算。',
      TH: 'ปิดใช้งานเมื่อไม่มี PG ในโหมด DISPLAY/BLIND หากมีเฉพาะมาตรฐาน ชำระตามสกุลตัวแทนหลัก'
    },
    'URL결제(Y)로 등록된 PG가 없습니다. API연동설정에서 연동용도를 확인하세요.': {
      EN: 'No PG registered for URL pay (Y). Check integration purpose under API integration settings.',
      JP: 'URL決済(Y)で登録されたPGがありません。API連携設定の連携用途をご確認ください。',
      CH: '没有注册为 URL 支付(Y) 的 PG。请在 API 联动设置中查看联动用途。',
      TH: 'ไม่มี PG ที่ลงทะเบียนชำระ URL (Y) ตรวจสอบวัตถุประสงค์การเชื่อมต่อในการตั้งค่า API'
    },
    ' (업체정보조회)': {
      EN: ' (merchant lookup)',
      JP: '（加盟店情報照会）',
      CH: '（商户查询）',
      TH: ' (ค้นหาร้านค้า)'
    },
    '총본사 정보 상세': { EN: 'Head office details', JP: '総本社情報の詳細', CH: '总部详情', TH: 'รายละเอียดสำนักงานใหญ่' },
    '본사 정보 상세': { EN: 'Regional HQ details', JP: '本社情報の詳細', CH: '地区总部详情', TH: 'รายละเอียดสำนักงานภูมิภาค' },
    '총판 정보 상세': { EN: 'Master distributor details', JP: '総販情報の詳細', CH: '总代详情', TH: 'รายละเอียดตัวแทนหลัก' },
    '지사 정보 상세': { EN: 'Branch details', JP: '支社情報の詳細', CH: '分公司详情', TH: 'รายละเอียดสาขา' },
    '대리점 정보 상세': { EN: 'Agency details', JP: '代理店情報の詳細', CH: '代理店详情', TH: 'รายละเอียดตัวแทน' },
    '영업점 정보 상세': { EN: 'Sales office details', JP: '営業所情報の詳細', CH: '营业点详情', TH: 'รายละเอียดจุดขาย' },
    '가맹점 정보 상세': { EN: 'Merchant details', JP: '加盟店情報の詳細', CH: '商户详情', TH: 'รายละเอียดร้านค้า' },
    '업체 정보 상세': { EN: 'Organization details', JP: '取引先情報の詳細', CH: '企业详情', TH: 'รายละเอียดองค์กร' },
    '로그인한 계정에 연결된 가맹점 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.': {
      EN: 'The merchant linked to your login is shown automatically. View or edit below.',
      JP: 'ログインに紐づく加盟店情報が自動表示されます。下で照会・修正できます。',
      CH: '已自动显示与当前登录关联的商户信息。可在下方查看与修改。',
      TH: 'แสดงร้านค้าที่ผูกกับบัญชีเข้าใช้อัตโนมัติ ดู/แก้ไขด้านล่าง'
    },
    '로그인한 계정에 연결된 소속 업체 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.': {
      EN: 'Your organization linked to this login is shown automatically. View or edit below.',
      JP: 'ログインに紐づく所属取引先情報が自動表示されます。下で照会・修正できます。',
      CH: '已自动显示与当前登录关联的组织企业信息。可在下方查看与修改。',
      TH: 'แสดงองค์กรที่ผูกกับบัญชีเข้าใช้อัตโนมัติ ดู/แก้ไขด้านล่าง'
    },
    '대표 아이디 (중복검사)': {
      EN: 'Main login ID (duplicate check)',
      JP: '代表ログインID（重複確認）',
      CH: '主登录ID（查重）',
      TH: 'ไอดีเข้าใช้หลัก (ตรวจซ้ำ)'
    },
    중복확인: {
      EN: 'Check duplicate',
      JP: '重複確認',
      CH: '查重',
      TH: 'ตรวจสอบซ้ำ'
    },
    '중복확인 실패': {
      EN: 'Duplicate check failed',
      JP: '重複確認に失敗しました',
      CH: '查重失败',
      TH: 'การตรวจซ้ำล้มเหลว'
    },
    '대표 아이디 중복확인을 먼저 진행하세요.': {
      EN: 'Please run duplicate check for the main login ID first.',
      JP: '先に代表ログインIDの重複確認を行ってください。',
      CH: '请先完成主登录ID的查重。',
      TH: 'โปรดตรวจสอบซ้ำไอดีหลักก่อน'
    },
    '보조 아이디 중복확인을 먼저 진행하세요.': {
      EN: 'Please run duplicate check for the assistant login ID first.',
      JP: '先に補助ログインIDの重複確認を行ってください。',
      CH: '请先完成辅助登录ID的查重。',
      TH: 'โปรดตรวจสอบซ้ำไอดีผู้ช่วยก่อน'
    },
    '로그인ID 중복확인을 먼저 진행하세요.': {
      EN: 'Please run duplicate check for the login ID first.',
      JP: '先にログインIDの重複確認を行ってください。',
      CH: '请先完成登录ID的查重。',
      TH: 'โปรดตรวจสอบซ้ำรหัสเข้าใช้ก่อน'
    },
    '챗봇 관리자 로그인ID 중복확인을 먼저 진행하세요.': {
      EN: 'Please run duplicate check for the chatbot admin login ID first.',
      JP: '先にチャットボット管理者ログインIDの重複確認を行ってください。',
      CH: '请先完成聊天机器人管理员登录ID的查重。',
      TH: 'โปรดตรวจสอบซ้ำรหัสผู้ดูแลแชทบอตก่อน'
    },
    '챗봇 카탈로그(산하 허용·이미지)': {
      EN: 'Chatbot catalog (sub-org allowance & images)',
      JP: 'チャットボットカタログ（傘下許容・画像）',
      CH: '聊天机器人目录（下级允许与图片）',
      TH: 'แคตตาล็อกแชทบอต (สิทธิ์ลูกข่ายและรูป)'
    },
    '총본사·본사·총판만 설정합니다. 비우면 해당 단계에서 제한 없음(상위·시스템 기본). 산하 가맹 실효값은 체인 최소(교집합·이미지 장수)입니다.': {
      EN: 'Only root HQ, regional HQ, and master distributors can set this. Leave blank for no limit at that tier (inherits HQ/system defaults). Effective values for downstream merchants use the strictest cap along the chain (intersection of allowed types and image count).',
      JP: '設定できるのは総本部・本社・総販のみです。空欄なら当該段階では制限なし（上位・システム既定を継承）。傘下加盟店の実効値はチェーン上の最も厳しい値（許容タイプの共通部分・画像枚数）になります。',
      CH: '仅总总部、地区总部、总代可配置。留空表示该层级不设限（继承上级/系统默认）。下级商户生效值为整条链上最严限制（允许类型的交集与图片张数）。',
      TH: 'ตั้งได้เฉพาะ HQ สูงสุด HQ ภูมิภาค และตัวแทนหลัก ว่าง = ไม่จำกัดในระดับนั้น (ตามค่าเริ่มต้นระบบ/บน) ค่าที่มีผลกับร้านลูกข่ายใช้ค่าที่เข้มที่สุดในโซ่น (ทับซ้อนของประเภทและจำนวนรูป)'
    },
    '산하 허용 상품유형(CSV)': {
      EN: 'Allowed listing types for sub-orgs (CSV)',
      JP: '傘下で許可する商品タイプ（CSV）',
      CH: '下级允许的商品类型（CSV）',
      TH: 'ประเภทสินค้าที่อนุญาตใต้องค์กร (CSV)'
    },
    'SALE,RESERVATION_TIME,RESERVATION_PLACE': {
      EN: 'e.g. SALE,RESERVATION_TIME,RESERVATION_PLACE',
      JP: '例: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      CH: '例：SALE,RESERVATION_TIME,RESERVATION_PLACE',
      TH: 'เช่น SALE,RESERVATION_TIME,RESERVATION_PLACE'
    },
    '상품 이미지 장수 상한(1~4)': {
      EN: 'Max product images (1–4)',
      JP: '商品画像の上限枚数（1～4）',
      CH: '商品图片张数上限（1~4）',
      TH: 'จำนวนรูปสินค้าสูงสุด (1–4)'
    },
    '— 미지정': {
      EN: '— Not set',
      JP: '— 未指定',
      CH: '— 未指定',
      TH: '— ไม่ระบุ'
    },
    '브랜딩 설정': {
      EN: 'Branding',
      JP: 'ブランディング設定',
      CH: '品牌设置',
      TH: 'การตั้งค่าแบรนด์'
    },
    '본사에서 [배경/로고 변경권한]을 부여한 가맹점은 메인·로고·테마를 수정할 수 있습니다. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.': {
      EN: 'Merchants granted [background/logo edit rights] by HQ may change main image, logos, and theme. Main image = left background on the login page; logos = top of the login box and top of the sidebar.',
      JP: '本社が［背景／ロゴ変更権限］を付与した加盟店は、メイン・ロゴ・テーマを変更できます。メイン画像＝ログイン画面左の背景、ロゴ＝ログイン枠上・サイドバー上です。',
      CH: '总部授予【背景/Logo 修改权限】的商户可修改主图、Logo 与主题。主图=登录页左侧背景；Logo=登录框顶部与侧栏顶部。',
      TH: 'ร้านที่ HQ ให้สิทธิ์ [แก้พื้นหลัง/โลโก้] แก้ภาพหลัก โลโก้ และธีมได้ ภาพหลัก=พื้นหลังซ้ายหน้าเข้าใช้ โลโก้=ด้านบนกล่องล็อกอินและแถบด้านข้าง'
    },
    '수정 저장': {
      EN: 'Save changes',
      JP: '変更を保存',
      CH: '保存修改',
      TH: 'บันทึกการแก้ไข'
    },
    '선택한 가맹점의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.': {
      EN: 'This is the selected merchant. Pick one row in the grid, then use [Detail] to view or edit.',
      JP: '選択した加盟店の情報です。一覧で1件選び、[詳細]で照会・修正します。',
      CH: '所选商户的信息。请在表格中选择一行后点击【详情】查看或修改。',
      TH: 'ข้อมูลร้านค้าที่เลือก เลือกแถวในตารางแล้วกด [รายละเอียด] เพื่อดู/แก้ไข'
    },
    '상위 본사(우리)가 권한을 준 회사의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.': {
      EN: 'This company is under your HQ scope. Pick one row, then use [Detail] to view or edit.',
      JP: '上位本社（当方）が権限を付与した会社の情報です。一覧で1件選び、[詳細]で照会・修正します。',
      CH: '由上级总部授权的公司信息。请在表格中选择一行后点击【详情】查看或修改。',
      TH: 'บริษัทที่สำนักงานใหญ่ของคุณให้สิทธิ์ เลือกแถวแล้วกด [รายละเอียด]'
    },
    '무효·환불 정산 방식 카드 안내': {
      EN: 'Void / refund settlement (standard, revenue, hybrid 1/2, or follow distributor/HQ) is set in the four fields below. Standard: deduct net sales, no extra success fees on void/refund. Revenue: keep net sales, charge success fees again. Hybrid 1/2 split void vs refund rules. Follow uses HQ → recovery/receivables defaults; a value here overrides for this merchant.',
      JP: '下の4項目で無効・返金の精算方式（一般・収益・ハイブリッド1/2・総販/本社に従う）を設定します。一般は純売上控除・成功手数料は追加しません。収益は純売上維持・二重課金します。総販・本社に従うは本社の回収/未収設定を適用し、ここで選ぶと加盟店優先です。',
      CH: '在下方四项设置无效/退款结算（一般、收益、混合1/2、跟随总代/总部）。一般：扣净销售额，无效/退款不追加成功手续费。收益：不扣净销售额，再次收取成功手续费。跟随适用总部回款/应收默认值；此处选择则商户优先。',
      TH: 'ตั้งโมฆะ/คืนเงินในสี่ช่อง (มาตรฐาน·รายได้·ไฮบริด1/2·ตาม HQ) มาตรฐานหักยอด·ไม่เก็บค่าธรรมเนียมสำเร็จซ้ำ รายได้คงยอด·เก็บซ้ำ ตาม HQ ใช้ค่าเริ่มต้นจากหน้าตั้งค่า HQ'
    },
    '검색: 첫 줄에서 거래일·빠른기간을 정한 뒤, 둘째 줄에서 검색구분·검색어·상태그룹을 맞추고 오른쪽 [검색]을 누릅니다. 「전체」는 해당 조건으로 좁히지 않습니다. 앞쪽 열 순서(업체·거래일·거래시간·루트·승인번호·거래번호)는 통합 결제내역 기본과 같습니다. 건당수수료 열은 거래 성공 시 과금되는 성공(건당) 고정액만 표시합니다. 기타수수료: USDT·FX는 승인금액 대비 %(「결제(%)」 합계에 포함), 3DS는 정책통화 기준 건당 고정(합계 열에는 미포함·별도 열). 세 항목은 결제·건당 등과 별도로 동시 과금될 수 있습니다. 금액이 없으면 USDT·FX·3DS 열은 — 입니다. 정산 수수료는 정산 실행 시 1회 과금되며, 송금(이체) 수수료는 그 이후 송금 처리 시 과금되어 정산리포트에 정산 수수료·송금 수수료로 각각 표시됩니다. 이 화면의 총수수료·지급예상에는 정산·송금 건당액이 포함되지 않습니다. 결제(성공): 건당·%(승인 시 부과) 열, 담보(롤링%·추정액), 지급예상액, 정산액(지급예상−담보추정). 실패·취소·무효·환불 등은 상태별 수수료 규칙을 따르며, 무효·환불 계열은 성공 건과 동일한 건당·%가 추가로 과금될 수 있습니다(이중 과금). 차감(취소·환불·무효·실패 등): 지급예상액은 0, 총수수료·부가세는 과금액(양수), 정산액은 −(총수수료+부가세)입니다. 담보 추정은 승인 건에만 표시됩니다. 본사·총판 등은 로그인 조직 하위 가맹점만 조회됩니다.': {
      EN: 'Search: set transaction dates and quick range on the first row; on the second row set search field, keyword, and status group, then click [Search] on the right. [All] does not narrow that dimension. Leading columns (merchant, date, time, route, approval no., txn id) match the integrated payment list. The per-txn fee column shows only the flat success fee charged on successful transactions. Other fees: USDT·FX are % of approved amount (included in the Pay(%) total); 3DS is a fixed per-txn charge in policy currency (not in the sum totals, separate column). Those three may accrue alongside pay/per-txn fees. When there is no amount, USDT·FX·3DS show an em dash. Settlement fees are charged once per settlement run; wire/transfer fees are charged when the transfer is processed and appear separately on settlement reports as settlement fee and wire fee. This screen’s total fees and expected payout exclude settlement/wire per-txn rows. Pay (success): per-txn and % columns charged on approval; collateral (rolling % and estimate); expected payout; settlement amount (expected minus collateral estimate). Fail/cancel/void/refund follow state-specific fee rules; void/refund families may incur the same per-txn/% as success (double charge). Deductions (cancel/refund/void/fail, etc.): expected payout is 0; total fee and VAT are charged amounts (positive); settlement amount is −(total fee + VAT). Collateral estimate is shown only for approved transactions. HQ/distributors see only merchants under the logged-in organization.',
      JP: '検索：1行目で取引日・クイック期間を指定し、2行目で検索区分・キーワード・状態グループを合わせて右の［検索］を押します。「すべて」はその条件での絞り込みを行いません。先頭列（加盟店・取引日・時刻・ルート・承認番号・取引番号）は統合決済一覧と同じ順です。件当手数料列は取引成功時のみ課される成功（件当）固定額を表示します。その他手数料：USDT・FXは承認金額比の%（「決済(%)」合計に含む）、3DSは政策通貨基準の件当固定（合計列には含まず別列）。3つは決済・件当等とは別に同時課金され得ます。金額がない場合USDT・FX・3DSは「—」です。精算手数料は精算実行時に1回課金され、送金（振込）手数料はその後の送金処理で課金され精算レポートに精算手数料・送金手数料として表示されます。この画面の手数料合計・支払予定額には精算・送金の件当は含みません。決済（成功）：件当・%（承認時）列、担保（ロール%・見積額）、支払予定額、精算額（支払予定−担保見積）。失敗・取消・無効・返金等は状態別の手数料ルールに従い、無効・返金系は成功取引と同様の件当・%が追加課金され得ます（二重課金）。控除（取消・返金・無効・失敗等）：支払予定額は0、手数料合計・消費税は課金額（正）、精算額は−(手数料合計+消費税)です。担保見積は承認取引のみ表示されます。本社・総販等はログイン組織配下の加盟店のみ照会できます。',
      CH: '搜索：首行设交易日与快捷区间，次行设搜索字段、关键词、状态分组后点右侧【搜索】。「全部」不按该维度筛选。前列顺序（商户、交易日期、时间、路由、授权号、交易号）与综合支付列表一致。按笔手续费列仅显示成功交易时收取的固定成功费。其他费用：USDT·FX 为批准金额比例%（计入「支付(%)」合计）；3DS 为政策货币按笔固定（不计入合计列，单独列）。三者可与支付/按笔等同时计费。无金额时 USDT·FX·3DS 显示「—」。结算手续费在结算执行时收取一次；汇款（转账）手续费在后续汇款处理时收取并在结算报告中分列。本屏手续费合计与预计拨付不含结算/汇款按笔。支付（成功）：按笔与%（批准时）列、担保（滚动%·估计额）、预计拨付额、结算额（预计−担保估计）。失败·取消·作废·退款等按状态计费规则；作废·退款类可能与成功交易同样再收按笔/%（双重计费）。扣减（取消·退款·作废·失败等）：预计拨付为0；手续费合计与增值税为计费额（正）；结算额为−(手续费合计+增值税)。担保估计仅对批准交易显示。总部/总代等仅可查登录组织下属商户。',
      TH: 'ค้นหา: แถวแรกตั้งวันที่และช่วงด่วน แถวสองตั้งฟิลด์ค้นหา คำค้น กลุ่มสถานะ แล้วกด [ค้นหา] ขวา 「ทั้งหมด」ไม่กรองมิตินั้น คอลัมน์หน้าเหมือนรายการชำระรวม ค่าธรรมเนียมต่อรายการแสดงเฉพาะค่าคงที่ตอนสำเร็จ USDT·FX เป็น % ของยอดอนุมัติ (รวมใน「ชำระ(%)」) 3DS เป็นคงที่ต่อรายการตามสกุลนโยบาย (คอลัมน์แยก) ค่าธรรมเนียมชำระบัญชี/โอนต่อรายการไม่รวมในยอดรวมหน้านี้ สำเร็จ: คอลัมน์ต่อรายการ·% หลักประกัน ยอดจ่ายโดยประมาณ ยอดชำระ (ประมาณ−หลักประกัน) ล้มเหลว·ยกเลิก·โมฆะ·คืนเงิน ตามกฎสถานะ โมฆะ/คืนอาจถูกเก็บซ้ำ หัก: ยอดจ่ายโดยประมาณ=0 รวม+Vat เป็นบวก ยอดชำระ=−(รวม+Vat) ประมาณหลักประกันเฉพาะอนุมัติ เห็นเฉพาะร้านใต้องค์กรที่ล็อกอิน'
    },
    '총판이 허용·대표 주기를 쓰면 가맹 정산주기 셀렉트가 그 범위로 바뀝니다. 정산안함: 배치 적립 없음. RT·건별 / T0·TM·TH·당일합산 / M5·M10·M30·분마다 / H1~H12·시간마다 / D·W·WK·실행마다 1건. D0 자동: 당일 23:50까지(총판별 정산 크론 기준 Zone). 이체및송금: 수동·자동·자동(수동불가)·임의출금·사용안함. 이체주기(분)는 자동 계열만. 지급보류: 정산은 진행, 출금만 제한. 정산제외: D0 등 휴일 제외 등(세부는 설정 화면).': {
      EN: 'If the distributor allows a representative cycle, the merchant settlement-cycle list is limited to that range. No settlement: no batch accrual. RT per txn / T0·TM·TH same-day / M5·M10·M30 per minute / H1–H12 hourly / D·W·WK one row per run. D0 auto: until 23:50 same day (per distributor cron zone). Transfer/remit: manual, auto, auto(no manual), ad-hoc withdraw, disabled. Transfer interval (minutes) applies only to auto chains. Payout hold: settlement continues, payouts only restricted. Settlement exclude: e.g. D0 holiday rules (see settings screens).',
      JP: '総販が許可した代表周期がある場合、加盟店の精算周期リストはその範囲に制限されます。精算なし:バッチ積立なし。RTは件別/T0·TM·THは当日集計/M5·M10·M30は分ごと/H1~H12は時間ごと/D·W·WKは実行ごとに1件。D0自動:当日23:50まで(総販別の精算クロンZone)。振込・送金:手動・自動・自動(手動不可)・任意出金・未使用。振込周期(分)は自動系のみ。支給保留:精算は進行、出金のみ制限。精算除外:D0等の休日除外など(詳細は設定画面)。',
      CH: '若总代允许代表周期，商户结算周期选项将限制在该范围内。不结算：无批量计提。RT按笔/T0·TM·TH当日汇总/M5·M10·M30按分钟/H1–H12按小时/D·W·WK每次执行一条。D0自动：至当日23:50（按总代结算 cron 时区）。转账：手动、自动、自动(不可手动)、临时出金、停用。转账周期(分)仅自动链。支付暂缓：结算继续，仅限制出款。结算排除：如 D0 节假日等（详见设置）。',
      TH: 'หากตัวแทนหลักกำหนดรอบตัวแทน รายการรอบชำระของร้านจะถูกจำกัดในช่วงนั้น ฯลฯ'
    },
    '상세(지역본사정보)': {
      EN: 'Detail (regional HQ)',
      JP: '詳細（地域本社）',
      CH: '详情（地区总部）',
      TH: 'รายละเอียด (สำนักงานภูมิภาค)'
    },
    '가맹점 상세정보': { EN: 'Merchant details', JP: '加盟店の詳細', CH: '商户详细信息', TH: 'รายละเอียดร้านค้า' },
    '가맹점 상세 정보': { EN: 'Merchant details', JP: '加盟店の詳細', CH: '商户详细信息', TH: 'รายละเอียดร้านค้า' },
    '총판·본사 따름': {
      EN: 'Follow distributor / HQ',
      JP: '総販・本社に従う',
      CH: '跟随总代/总部',
      TH: 'ตามตัวแทนหลัก / HQ'
    },
    '일반형 (순매출 차감·무효·환불 시 성공 수수료 미추가)': {
      EN: 'Standard (deduct net sales; no extra success fees on void/refund)',
      JP: '一般型（純売上控除・無効/返金時は成功手数料を追加しない）',
      CH: '标准型（扣净销售额；无效/退款不追加成功手续费）',
      TH: 'มาตรฐาน (หักยอดขายสุทธิ·โมฆะ/คืนไม่เก็บค่าธรรมเนียมสำเร็จซ้ำ)'
    },
    '수익형 (순매출 미차감·무효·환불 시 성공 수수료 이중 과금)': {
      EN: 'Revenue (no net deduction; success fees charged again on void/refund)',
      JP: '収益型（純売上未控除・無効/返金時も成功手数料を二重課金）',
      CH: '收益型（不扣净销售额；无效/退款再次收取成功手续费）',
      TH: 'รายได้ (ไม่หักยอดขายสุทธิ·โมฆะ/คืนเก็บค่าธรรมเนียมสำเร็จซ้ำ)'
    },
    '하이브리드1 (무효·수무: 순매출 차감·이중과금 / 환불·강제: 순매출 유지·건당만)': {
      EN: 'Hybrid 1 (void/manual: deduct net + double fee / refund/force: keep net + per-txn only)',
      JP: 'ハイブリッド1（無効・手動: 純売上控除+二重 / 返金・強制: 純売上維持+件当のみ）',
      CH: '混合1（无效/手动作废：扣净销售额+双收 / 退款/强制：保留净销售额+仅按笔）',
      TH: 'ไฮบริด1 (โมฆะ/มือ: หักยอด+เก็บซ้ำ / คืน/บังคับ: คงยอด+ต่อรายการ)'
    },
    '하이브리드2 (환불·강제: 순매출 차감·이중과금 / 무효·수무: 순매출 유지·건당만)': {
      EN: 'Hybrid 2 (refund/force: deduct net + double fee / void/manual: keep net + per-txn only)',
      JP: 'ハイブリッド2（返金・強制: 純売上控除+二重 / 無効・手動: 純売上維持+件当のみ）',
      CH: '混合2（退款/强制：扣净销售额+双收 / 无效/手动作废：保留净销售额+仅按笔）',
      TH: 'ไฮบริด2 (คืน/บังคับ: หักยอด+เก็บซ้ำ / โมฆะ/มือ: คงยอด+ต่อรายการ)'
    },
    '일반형 (순매출 차감·환불·자동환불 금액 포함)': {
      EN: 'Standard (net sales less refunds & auto-refunds)',
      JP: '一般型（純売上控除・返金・自動返金を含む）',
      CH: '标准型（净销售额扣除退款与自动退款）',
      TH: 'มาตรฐาน (หักยอดขายสุทธิ·คืนเงิน·คืนอัตโนมัติ)'
    },
    '수익형 (해당 금액 순매출 미차감)': {
      EN: 'Revenue (amount not deducted from net sales)',
      JP: '収益型（当該金額は純売上から控除しない）',
      CH: '收益型（该金额不从净销售额扣除）',
      TH: 'รายได้ (ไม่หักจากยอดขายสุทธิ)'
    },
    '하이브리드 (무효·수무·강제환불만 차감, 환불·자동환불 미차감)': {
      EN: 'Hybrid (only void / manual void / force refund deduct; refund & auto-refund do not)',
      JP: 'ハイブリッド（無効・手動無効・強制返金のみ控除、返金・自動返金は控除しない）',
      CH: '混合（仅无效/手动作废/强制退款扣减；普通退款与自动退款不扣）',
      TH: 'ไฮบริด (หักเฉพาะโมฆะ·โมฆะมือ·บังคับคืน)'
    },
    '그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.': {
      EN: 'Select one row in the grid, then use [Detail] to view or edit.',
      JP: '一覧で1件選択し、[詳細]で照会・修正します。',
      CH: '在表格中选择一行后，使用【详情】查看或修改。',
      TH: 'เลือกแถวในตาราง แล้วกด [รายละเอียด] เพื่อดู/แก้ไข'
    },
    '가맹점만 표시됩니다. 결제 URL은 간편결제용으로, API 연동과 별도로 가맹점 생성 시 즉시 결제 페이지를 제공합니다.': {
      EN: 'Merchants only. The payment URL is for easy checkout; separate from API integration, a pay page is available as soon as the merchant is created.',
      JP: '加盟店のみ表示されます。決済URLは簡易決済用で、API連携とは別に加盟店作成時すぐに決済ページを提供します。',
      CH: '仅商户显示。支付 URL 用于简易支付；与 API 联动独立，创建商户后即可提供支付页。',
      TH: 'เฉพาะร้านค้า URL ชำระสำหรับชำระง่าย แยกจาก API สร้างร้านแล้วมีหน้าชำระทันที'
    },
    '배포설정 > API연동설정에서 사용(Y)으로 등록된 결제대행사가 목록에 표시됩니다. PG를 고르면 API연동설정의 MID·Route 등이 기본값으로 채워지며, 가맹점 전용 값은 수정·저장하면 됩니다. 예정모드·N·D시각은 위 「통합정산설정」에서 일괄 지정합니다. 실제 결제 운영 PG는 라디오(운영)로 하나만 지정합니다. 라디오가 켜진 행만 붉은 배경(파스텔)으로 표시됩니다. 하단 [저장] 시 한꺼번에 반영됩니다.': {
      EN: 'PGs enabled (Y) under Deployment → API integration are listed. Choosing a PG fills MID·Route defaults from integration; override per merchant and save. Scheduled mode·N·D time are set in bulk in Integrated settlement above. Only one live PG via the “live” radio; rows with the radio on use a pastel red highlight. [Save] at the bottom applies all rows.',
      JP: 'デプロイ設定＞API連携設定で使用(Y)の決済代行が一覧に表示されます。PGを選ぶと連携設定のMID・Route等が既定で入り、加盟店専用値は編集して保存します。予定モード・N・D時刻は上の「統合精算設定」で一括指定します。本番運用PGはラジオで1つのみ。ラジオONの行だけパステル赤背景です。下部[保存]で一括反映します。',
      CH: '列出部署设置中已启用(Y)的支付机构。选择 PG 后填入 API 联动中的 MID·Route 等默认值；可按商户修改保存。预定模式·N·D 时间在上方「整合结算设置」批量指定。实际运营 PG 用单选唯一指定；选中行为淡红底。底部【保存】一次应用。',
      TH: 'แสดง PG ที่เปิดใช้ (Y) ในการตั้งค่า API เลือก PG แล้วเติม MID·Route ค่าเฉพาะร้านแก้ได้ โหมดกำหนดเวลา·N·D ตั้งรวมด้านบน เลือก PG จริงทีละหนึ่ง แถวที่เลือกพื้นหลังแดงอ่อน กดบันทึกด้านล่าง'
    },
    '미사용 선택 시 WEB 결제 시스템이 중지됩니다. 아래 대표 기본상품정보는 온라인 URL 결제 기본값으로 사용됩니다.': {
      EN: 'If disabled, the WEB checkout system stops. The default product fields below are defaults for online URL payments.',
      JP: '未使用にするとWEB決済システムを停止します。下の代表デフォルト商品はオンラインURL決済の既定値です。',
      CH: '选择停用时将停止 WEB 支付系统。下方默认商品信息用作在线 URL 支付默认值。',
      TH: 'เลือกไม่ใช้จะหยุดระบบชำระ WEB สินค้าเริ่มต้นด้านล่างใช้เป็นค่าเริ่มต้น URL'
    },
    '결제통보 URL': { EN: 'Payment notify URLs', JP: '決済通知URL', CH: '支付通知 URL', TH: 'URL แจ้งชำระ' },
    '결제 응답을 가맹점에게 송부할 노티 주소. 등록 시 결제통보 URL관리에 자동 반영됩니다.': {
      EN: 'Notify endpoints for payment callbacks to the merchant. Saving also syncs to payment-notify URL management.',
      JP: '決済結果を加盟店へ送る通知先です。保存時に決済通知URL管理へ自動反映されます。',
      CH: '向商户发送支付回调的通知地址。保存时会同步到支付通知 URL 管理。',
      TH: 'ที่อยู่แจ้งผลชำระให้ร้านค้า บันทึกแล้วซิงค์ไปจัดการ URL แจ้งเตือน'
    },
    'JPAY 수신통보 URL': {
      EN: 'JPAY notify ingress URLs',
      JP: 'JPAY受信通知URL',
      CH: 'JPAY 接收通知 URL',
      TH: 'URL รับแจ้ง JPAY'
    },
    'J-Pay pay_index 전문의 pay_notifyurl·pay_callbackurl 에 사용됩니다. 노티미들웨어 가맹 수신 URL을 등록하세요. 비우면 ICOPAY ingress(cbJpay/rsJpay) 기본값을 사용합니다.': {
      EN: 'Used as pay_notifyurl and pay_callbackurl on J-Pay pay_index. Register your notify-middleware merchant receive URLs. If empty, ICOPAY ingress defaults (cbJpay/rsJpay) apply.',
      JP: 'J-Pay pay_indexのpay_notifyurl·pay_callbackurlに使用します。ノティミドルウェアの加盟店受信URLを登録してください。空欄の場合はICOPAY ingress(cbJpay/rsJpay)の既定値を使用します。',
      CH: '用于 J-Pay pay_index 的 pay_notifyurl·pay_callbackurl。请登记通知中间件的商户接收 URL。留空则使用 ICOPAY ingress(cbJpay/rsJpay) 默认值。',
      TH: 'ใช้เป็น pay_notifyurl และ pay_callbackurl ใน J-Pay pay_index ลงทะเบียน URL รับของร้านที่ notify middleware ว่างไว้ใช้ค่าเริ่มต้น ICOPAY ingress (cbJpay/rsJpay)'
    },
    'Notify (pay_notifyurl) / Callback URL (NOTI MW)': {
      EN: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)',
      JP: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)',
      CH: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)',
      TH: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)'
    },
    'Callback (pay_callbackurl) / Result URL (NOTI MW)': {
      EN: 'Callback (pay_callbackurl) / Result URL (NOTI MW)',
      JP: 'Callback (pay_callbackurl) / Result URL (NOTI MW)',
      CH: 'Callback (pay_callbackurl) / Result URL (NOTI MW)',
      TH: 'Callback (pay_callbackurl) / Result URL (NOTI MW)'
    },
    '본사에서 [배경/로고 변경권한]을 부여한 가맹점은 메인·로고·테마를 수정할 수 있습니다. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.': {
      EN: 'Merchants granted [background/logo edit] may change main image, logos, and theme. Main = login left background; logos = login top and sidebar top.',
      JP: '本社が[背景/ロゴ変更権]を付与した加盟店はメイン・ロゴ・テーマを変更できます。メイン画像=ログイン左背景、ロゴ=ログイン上・サイドバー上。',
      CH: '总部授予【背景/Logo 修改权】的商户可改主图、Logo、主题。主图=登录左侧背景；Logo=登录顶部与侧栏顶部。',
      TH: 'ร้านที่ HQ ให้สิทธิ์แก้พื้นหลัง/โลโก้ แก้ภาพหลัก·ธีมได้'
    },
    '본사/총판 공통 설정입니다. COPYRIGHT에 입력한 문구는 화면 하단에 표시됩니다.': {
      EN: 'Shared HQ / distributor settings. COPYRIGHT text is shown in the page footer.',
      JP: '本社/総販共通設定です。COPYRIGHTの文言は画面下部に表示されます。',
      CH: '总部/总代共用设置。COPYRIGHT 文案显示在页面底部。',
      TH: 'ตั้งค่าร่วม HQ/ตัวแทนหลัก ข้อความ COPYRIGHT แสดงท้ายหน้า'
    },
    '칠페이 통합정산 화면의 「예정(ICOPAY)」 표시에만 쓰입니다. 배포설정 API연동설정과 동일 규칙을 쓰려면 예정모드를 연동기본으로 두세요. 아래 값은 [저장] 시 등록된 모든 결제대행사 행에 동일하게 적용됩니다.': {
      EN: 'Used only for the “Scheduled (ICOPAY)” label on the integrated settlement screen. Set scheduled mode to “integration default” to follow Deployment API integration rules. Values below apply to every PG row when you save.',
      JP: 'ChillPay統合精算画面の「予定(ICOPAY)」表示専用です。デプロイAPI連携と同じ規則にするには予定モードを連携既定にしてください。下の値は[保存]で登録済みの全決済代行行に一括適用されます。',
      CH: '仅用于整合结算画面的「预计(ICOPAY)」显示。要与部署 API 联动规则一致，请将预定模式设为联动默认。保存时将下方值应用到所有已登记支付机构行。',
      TH: 'ใช้เฉพาะป้ายกำหนดการ(ICOPAY) ในหน้าชำระรวม ตั้งโหมดตามค่าเริ่มต้นการเชื่อมต่อ บันทึกแล้วใช้กับทุกแถว PG'
    },
    '관리자 화면의 자동무효·이메일무효·자동환불·강제환불 사용 여부입니다. 전산설정관리(전역) 및 본사권한설정의 조직 단계 상한과 함께 적용됩니다. [기본·종전]은 미설정과 동일(허용으로 해석)입니다.': {
      EN: 'Whether auto-void, email void, auto-refund, and force-refund are enabled in admin. Combined with global HQ ledger settings and per-org caps in HQ permissions. [Default / legacy] means unset (treated as allowed).',
      JP: '管理画面の自動無効・メール無効・自動返金・強制返金の使用可否です。電算設定(全体)と本社権限の組織段階上限と併せて適用されます。[既定・従前]は未設定と同義（許可として解釈）です。',
      CH: '管理员界面中自动作废、邮件作废、自动退款、强制退款的开关。与全局电算设置及总部权限中的组织级上限一并生效。【默认/沿用】等同未设置（视为允许）。',
      TH: 'เปิด/ปิด โมฆะอัตโนมัติ·อีเมล·คืนอัตโนมัติ·บังคับคืน ร่วมกับเพดานระดับองค์กร'
    },
    '본사정책 따름이면 위에서 고른 본사 정책 템플릿의 3DS·차지백 설정이 적용됩니다. 직접입력일 때만 아래를 저장할 수 있습니다.': {
      EN: 'When following HQ policy, 3DS and chargeback settings from the selected HQ template apply. You can save the fields below only in manual mode.',
      JP: '本社ポリシーに従う場合、上で選んだ本社テンプレートの3DS・チャージバック設定が適用されます。直接入力のときのみ下を保存できます。',
      CH: '跟随总部政策时，适用所选总部模板的 3DS 与拒付设置。仅在「直接输入」时可保存下方字段。',
      TH: 'ตามนโยบาย HQ ใช้ 3DS/chargeback จากเทมเพลตที่เลือก บันทึกช่องล่างได้เฉพาะโหมดกรอกเอง'
    },
    '본사정책 따름이면 [본사 정책선택]에서 사용합니다. 목록에는 배포(Y)인 템플릿만 나오며, 가맹점 기준통화와 정책 통화코드가 같거나 정책 통화가 비어 있는 항목만 표시됩니다. 본사·총판·가맹점에 동일하게 적용·저장됩니다. 첫 항목(본사 기본 템플릿)은 선택값이 비어 있을 때 본사의 기본(DEFAULT) 수수료 템플릿을 씁니다.': {
      EN: 'If following HQ policy, use [HQ policy pick]. Only deployed (Y) templates are listed; rows match the merchant base currency or have an empty policy currency. Saved the same for HQ, distributor, and merchant. The first row (HQ default template) uses the HQ DEFAULT fee template when left blank.',
      JP: '本社ポリシーに従う場合は[本社ポリシー選択]を使います。一覧は配布(Y)のテンプレのみ。加盟店基準通貨と一致するかポリシー通貨が空の行のみ表示。本社・総販・加盟店に同様に保存されます。先頭(本社既定テンプレ)は未選択時に本社DEFAULT手数料テンプレを使います。',
      CH: '跟随总部政策时使用【总部政策选择】。列表仅显示已部署(Y)的模板；仅显示与商户基准货币一致或政策货币为空的行。总部、总代、商户保存规则相同。首行（总部默认模板）留空时使用总部 DEFAULT 手续费模板。',
      TH: 'ตามนโยบาย HQ เลือกจากรายการเทมเพลตที่ deploy แล้ว บันทึกเหมือนกันทุกระดับ'
    },
    '결제 정산금 중 보류율(%)만큼 보류기간(일) 동안 지급하지 않으며, 정산일자+보류기간 경과 후 정산금으로 전환됩니다. 보류 해지일이 공휴일이면 익영업일에 전환됩니다. 본사정책 따름 시 본사 수수료 정책(롤링 비율/일수)에 연동됩니다.': {
      EN: 'A rolling hold % of settlement funds is withheld for N days; after settlement date + hold days it becomes payable. If release falls on a holiday, it moves to the next business day. When following HQ policy, it links to HQ fee rolling % / days.',
      JP: '精算金のうち保留率(%)分を保留日数の間は支払わず、精算日+保留日経過後に精算金へ移行します。解除日が公休日なら翌営業日へずれます。本社ポリシーに従う場合は本社手数料(ローリング率/日数)に連動します。',
      CH: '按保留比例暂扣结算款相应天数，结算日+保留天数后转为可付款；若解除日为节假日则顺延至下一工作日。跟随总部政策时与总部手续费滚动比例/天数联动。',
      TH: 'กันยอดตาม % ช่วงวัน หลังวันชำระ+วันกันถึงจ่าย วันหยุดเลื่อนเป็นวันทำการถัดไป'
    },
    '가맹점 출금 제한 유형입니다. 매일·공휴일·공휴일 전날(17·18시) 규칙은 본사 영업일·공휴일 캘린더와 함께 출금 처리 시 해석합니다. 평일 구간은 시작·종료 시각으로 좁힙니다.': {
      EN: 'Merchant payout restriction type. Daily, holiday, and pre-holiday (17h/18h) rules are interpreted with the HQ business-day calendar when processing payouts. Weekday windows use start/end times.',
      JP: '加盟店の出金制限タイプです。毎日・公休日・前日(17/18時)ルールは本社営業日・休日カレンダーと併せて出金処理時に解釈します。平日帯は開始・終了時刻で絞ります。',
      CH: '商户出款限制类型。每日、节假日、节假日前一日(17/18点)规则与总部工作日历一起在出款时解释。平日区间用起止时间限定。',
      TH: 'ประเภทจำกัดการจ่ายร้านค้า ใช้ปฏิทินวันทำการ HQ ร่วมกับกฎวันหยุด'
    },
    '운영': { EN: 'Live', JP: '運用', CH: '运营', TH: 'ใช้งานจริง' },
    '+ 결제대행사 추가': {
      EN: '+ Add payment provider',
      JP: '+ 決済代行を追加',
      CH: '+ 添加支付机构',
      TH: '+ เพิ่มผู้ให้บริการชำระ'
    },
    '가맹점 저장 후 조회': {
      EN: 'Save merchant to load',
      JP: '加盟店を保存後に表示',
      CH: '保存商户后显示',
      TH: 'บันทึกร้านค้าแล้วค่อยโหลด'
    },
    '가맹점 선택 후 조회': {
      EN: 'Select a merchant to load',
      JP: '加盟店を選択してから表示',
      CH: '选择商户后显示',
      TH: 'เลือกร้านค้าแล้วค่อยโหลด'
    },
    '결제 URL': { EN: 'Payment URL', JP: '決済URL', CH: '支付 URL', TH: 'URL ชำระ' },
    '웹결제 사용여부': { EN: 'WEB checkout', JP: 'WEB決済の使用', CH: 'WEB 支付使用', TH: 'การใช้ชำระเงิน WEB' },
    '8자 이상 → 옆 [저장] 확정': {
      EN: '8+ chars → confirm with [Save] beside, then register below',
      JP: '8文字以上→横の[保存]で確定後、下部[保存]で登録',
      CH: '至少8位→先在旁侧【保存】确认，再通过底部【保存】注册',
      TH: '8 ตัวขึ้นไป→กด [บันทึก] ข้างช่องเพื่อยืนยัน แล้วลงทะเบียนด้านล่าง'
    },
    '+ 터미널 추가': {
      EN: '+ Add terminal',
      JP: '+ 端末を追加',
      CH: '+ 添加终端',
      TH: '+ เพิ่มเทอร์มินัล'
    },
    전체선택: { EN: 'Select all', JP: '全選択', CH: '全选', TH: 'เลือกทั้งหมด' },
    '국가 선택 후': {
      EN: 'Select country first',
      JP: '国を先に選択',
      CH: '请先选择国家',
      TH: 'เลือกประเทศก่อน'
    },
    '은행명 직접입력': {
      EN: 'Enter bank name',
      JP: '銀行名を直接入力',
      CH: '手动输入银行名',
      TH: 'พิมพ์ชื่อธนาคารเอง'
    },
    '직접입력': { EN: 'Manual entry', JP: '直接入力', CH: '手动输入', TH: 'กรอกเอง' },
    '본사정책 따름': {
      EN: 'Follow HQ policy',
      JP: '本社ポリシーに従う',
      CH: '跟随总部政策',
      TH: 'ตามนโยบายสำนักงานใหญ่'
    },
    '본사 정책선택': {
      EN: 'HQ policy selection',
      JP: '本社ポリシー選択',
      CH: '总部政策选择',
      TH: 'เลือกนโยบายสำนักงานใหญ่'
    },
    '본사 기본 템플릿 (DEFAULT)': {
      EN: 'HQ default template (DEFAULT)',
      JP: '本社既定テンプレート (DEFAULT)',
      CH: '总部默认模板 (DEFAULT)',
      TH: 'เทมเพลตค่าเริ่มต้น HQ (DEFAULT)'
    },
    '본사 기본 템플릿': {
      EN: 'HQ default template',
      JP: '本社既定テンプレート',
      CH: '总部默认模板',
      TH: 'เทมเพลตค่าเริ่มต้น HQ'
    },
    '보조 아이디 (중복검사)': {
      EN: 'Assistant login ID (duplicate check)',
      JP: '副次ログインID（重複確認）',
      CH: '辅助登录账号（查重）',
      TH: 'รหัสผู้ช่วย (ตรวจซ้ำ)'
    },
    'SWIFT 코드': {
      EN: 'SWIFT code',
      JP: 'SWIFTコード',
      CH: 'SWIFT 代码',
      TH: 'รหัส SWIFT'
    },
    '무효·환불 정산 (안내)': {
      EN: 'Void / refund settlement (guide)',
      JP: '無効・返金精算（案内）',
      CH: '无效/退款结算（说明）',
      TH: 'โมฆะ/คืนเงิน (คำอธิบาย)'
    },
    '무효 정산(21·40)': {
      EN: 'Void settlement (21·40)',
      JP: '無効精算(21・40)',
      CH: '无效结算(21·40)',
      TH: 'ชำระโมฆะ (21·40)'
    },
    '수동무효 정산(22·41)': {
      EN: 'Manual void settlement (22·41)',
      JP: '手動無効精算(22・41)',
      CH: '手动作废结算(22·41)',
      TH: 'โมฆะด้วยมือ (22·41)'
    },
    '환불 정산(30·42)': {
      EN: 'Refund settlement (30·42)',
      JP: '返金精算(30・42)',
      CH: '退款结算(30·42)',
      TH: 'ชำระคืนเงิน (30·42)'
    },
    '강제환불(31)': {
      EN: 'Forced refund (31)',
      JP: '強制返金(31)',
      CH: '强制退款(31)',
      TH: 'บังคับคืนเงิน (31)'
    },
    '기본·종전과 동일': {
      EN: 'Same as default / legacy',
      JP: '既定・従前と同じ',
      CH: '与默认/沿用相同',
      TH: 'เหมือนค่าเริ่มต้น/เดิม'
    },
    '정산마감시간': {
      EN: 'Settlement cutoff time',
      JP: '精算締切時刻',
      CH: '结算截止时间',
      TH: 'เวลาปิดรอบชำระ'
    },
    '정산자동개시시간': {
      EN: 'Auto settlement start time',
      JP: '精算自動開始時刻',
      CH: '自动结算开始时间',
      TH: 'เวลาเริ่มชำระอัตโนมัติ'
    },
    '변경 비고': {
      EN: 'Change remark',
      JP: '変更備考',
      CH: '变更备注',
      TH: 'หมายเหตุการเปลี่ยนแปลง'
    },
    '오전': { EN: 'AM', JP: '午前', CH: '上午', TH: 'ก่อนเที่ยง' },
    '오후': { EN: 'PM', JP: '午後', CH: '下午', TH: 'หลังเที่ยง' },
    '이체시간': {
      EN: 'Transfer time',
      JP: '振込時刻',
      CH: '转账时间',
      TH: 'เวลาโอน'
    },
    '정산제외여부': {
      EN: 'Settlement exclusion (Y/N)',
      JP: '精算除外の有無',
      CH: '是否排除结算',
      TH: 'ยกเว้นการชำระ (Y/N)'
    },
    '미만 시 다음 주기': {
      EN: 'If below min, defer to next cycle',
      JP: '下限未満は次周期へ',
      CH: '低于下限则并入下一周期',
      TH: 'ต่ำกว่าขั้นต่ำ → รอบถัดไป'
    },
    '펌뱅킹 최소': {
      EN: 'Firm banking minimum',
      JP: 'ファームバンキング下限',
      CH: '银企直连最低额',
      TH: 'ขั้นต่ำเฟิร์มแบงกิ้ง'
    },
    '예정모드': {
      EN: 'Scheduled mode',
      JP: '予定モード',
      CH: '预定模式',
      TH: 'โหมดกำหนดการ'
    },
    '연동기본': {
      EN: 'Integration default',
      JP: '連携既定',
      CH: '联动默认',
      TH: 'ค่าเริ่มต้นการเชื่อม'
    },
    'D시각(일괄)': {
      EN: 'D time (batch)',
      JP: 'D時刻（一括）',
      CH: 'D 时刻（批量）',
      TH: 'เวลา D (รวม)'
    },
    '착신화': {
      EN: 'Inbound mapping',
      JP: '着信化',
      CH: '来账映射',
      TH: 'แมปปิงขาเข้า'
    },
    '결제구분': {
      EN: 'Pay method',
      JP: '決済区分',
      CH: '支付类型',
      TH: 'ประเภทชำระ'
    },
    카드사: {
      EN: 'Card issuer',
      JP: 'カード会社',
      CH: '发卡机构',
      TH: 'ค่ายบัตร'
    },
    등록사유: {
      EN: 'Registration reason',
      JP: '登録理由',
      CH: '登记原因',
      TH: 'เหตุผลที่ลงทะเบียน'
    },
    '루트번호': {
      EN: 'Route no.',
      JP: 'ルート番号',
      CH: '路由号',
      TH: 'หมายเลข Route'
    },
    '카드브랜드': {
      EN: 'Card brand',
      JP: 'カードブランド',
      CH: '卡品牌',
      TH: 'แบรนด์บัตร'
    },
    '웹결제 사용 / 대표 기본상품정보 (온라인 URL 결제용)': {
      EN: 'WEB checkout / default product (online URL pay)',
      JP: 'WEB決済 / 代表デフォルト商品（オンラインURL決済用）',
      CH: 'WEB 支付 / 默认商品（在线 URL 支付）',
      TH: 'ชำระ WEB / สินค้าเริ่มต้น (ชำระ URL)'
    },
    '대표 상품명': {
      EN: 'Default product name',
      JP: '代表商品名',
      CH: '默认商品名称',
      TH: 'ชื่อสินค้าหลัก'
    },
    '챗봇 상품등록 한도(건)': {
      EN: 'Chatbot product row cap',
      JP: 'チャットボット商品登録上限（件）',
      CH: '聊天机器人商品登记上限（条）',
      TH: 'จำกัดจำนวนสินค้าแชทบอท (แถว)'
    },
    '챗봇 관리자(로그인ID·중복검사)': {
      EN: 'Chatbot admin (login ID · duplicate check)',
      JP: 'チャットボット管理者（ログインID・重複確認）',
      CH: '聊天机器人管理员（登录 ID·查重）',
      TH: 'ผู้ดูแลแชทบอท (รหัสเข้า·ตรวจซ้ำ)'
    },
    '가맹당 1명 · 없는 ID는 저장 시 자동 등록(초기비밀번호: ID+1!) · 공개 챗봇 상품관리 로그인에는 OTP 필요 · 비우면 해제': {
      EN: 'One per merchant · unknown IDs auto-create on save (initial password: ID+1!) · public chatbot product admin needs OTP · leave blank to disable',
      JP: '加盟店あたり1名・未登録IDは保存時に自動登録（初期PW: ID+1!）・公開チャットボット商品管理ログインはOTP必須・空欄で解除',
      CH: '每商户一名 · 不存在 ID 保存时自动注册（初始密码：ID+1!）· 公开聊天机器人商品管理登录需 OTP · 留空解除',
      TH: 'ร้านละ 1 · ID ใหม่บันทึกแล้วสร้างอัตโนมัติ (รหัสเริ่ม: ID+1!) · แอดมินสินค้าสาธารณะต้อง OTP · ว่างเพื่อปิด'
    },
    '가맹 활성 카탈로그 유형(CSV)': {
      EN: 'Merchant active catalog types (CSV)',
      JP: '加盟店アクティブカタログ種別(CSV)',
      CH: '商户启用目录类型（CSV）',
      TH: 'ประเภทแคตตาล็อกที่ใช้ (CSV)'
    },
    '승인 알림메일': {
      EN: 'Approval notification email',
      JP: '承認通知メール',
      CH: '批准通知邮件',
      TH: 'อีเมลแจ้งอนุมัติ'
    },
    '업로드 시 자동 반영 · 또는 HTTPS URL 직접 입력': {
      EN: 'Auto-fill on upload · or paste HTTPS URL',
      JP: 'アップロードで自動反映・またはHTTPS URLを直接入力',
      CH: '上传后自动填入 · 或直接粘贴 HTTPS URL',
      TH: 'อัปโหลดแล้วเติมอัตโนมัติ · หรือวาง HTTPS URL'
    },
    'PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하(본사 AI챗봇설정 변경 가능)로 재압축합니다. chatbot_logo_llm_tune_yn=Y 일 때 AI챗봇설정 순위 LLM으로 권장 픽셀을 잡습니다.': {
      EN: 'PNG/JPEG, up to 40 MB source. Server recompresses toward ≤2 MB (HQ AI chatbot settings). When chatbot_logo_llm_tune_yn=Y, HQ AI chatbot LLM ranking picks suggested pixels.',
      JP: 'PNG/JPEG、原稿最大40MB。サーバで目標2MB以下に再圧縮（本社AIチャットボット設定で変更可）。chatbot_logo_llm_tune_yn=Y のとき本社AIチャットボット設定のLLM順位で推奨ピクセルを決めます。',
      CH: 'PNG/JPEG，原图最大 40MB。服务器再压缩至约 2MB 以下（可由总部 AI 聊天设置调整）。chatbot_logo_llm_tune_yn=Y 时由总部 AI 聊天 LLM 排序建议像素。',
      TH: 'PNG/JPEG สูงสุด 40MB เซิร์ฟเวอร์บีบอีกให้ ≤2MB (ตั้งใน HQ AI) ถ้า chatbot_logo_llm_tune_yn=Y ใช้ LLM แนะนำความละเอียด'
    },
    '배포설정 > API연동설정(사용 Y) 전체가 목록에 나오며, PG 선택 시 본사에 등록한 MID·Route가 기본 입력됩니다. 예정모드·N·D시각은 위 「통합정산설정」에서 일괄 지정합니다. API KEY·IV는 비우면 본사 연동 자격을 따를 수 있습니다(ChillPay 등). URL·챗봇·API 결제는 운영(체크)를 여러 행에 켤 수 있습니다. 체크된 행은 붉은 배경(파스텔)로 표시됩니다. 노티 전용 PG는 카드브랜드가 ALL로 고정됩니다. [추가]로 행을 열고, 업체정보(가맹점)에서는 [저장][삭제][수정]마다 확인창이 두 번 뜹니다.': {
      EN: 'Deployment → API integration (Y) rows are all listed; picking a PG fills MID·Route from HQ registration. Scheduled mode·N·D time are set in bulk under Integrated settlement above. Leave API KEY·IV blank to inherit HQ credentials (e.g. ChillPay). URL·chatbot·API can each have multiple “live” rows checked; checked rows use a pastel red highlight. Notify-only PGs fix card brand to ALL. Use [Add] to open a row; on merchant detail, [Save]/[Delete]/[Edit] each shows two confirmation dialogs.',
      JP: 'デプロイ設定＞API連携(使用Y)の全行が一覧に出ます。PGを選ぶと本社登録のMID・Routeが既定入力。予定モード・N・D時刻は上の「統合精算設定」で一括。API KEY・IVは空で本社連携資格を継承(ChillPay等)。URL・チャットボット・APIは運用チェックを複数行に可。チェック行はパステル赤背景。ノティ専用PGはカードブランドALL固定。[追加]で行を開き、加盟店情報では[保存][削除][修正]ごとに確認が二重です。',
      CH: '部署 API 联动（启用 Y）全部列出；选 PG 后填入总部登记的 MID·Route。预定模式·N·D 时间在上方「整合结算设置」批量设置。API KEY·IV 留空则继承总部凭证。URL·聊天机器人·API 可多行勾选运营；选中行淡红底。纯通知 PG 卡品牌固定 ALL。【添加】打开行；在商户信息中【保存】【删除】【修改】各触发两次确认。',
      TH: 'รายการ API ที่เปิด Y ทั้งหมด เลือก PG แล้วเติม MID·Route จาก HQ โหมดกำหนด·N·D ตั้งรวมด้านบน ว่าง KEY/IV ใช้สิทธิ์ HQ ได้ หลายแถว URL/แชทบอท/API เปิดใช้งานได้ แถวที่เลือกพื้นหลังแดงอ่อน PG แจ้งเตือนอย่างเดียวการ์ด ALL กดเพิ่มแถว ที่หน้าร้าน บันทึก/ลบ/แก้ แต่ละครั้งมียืนยัน 2 ครั้ง'
    },
    '배포설정 > API연동설정에서 사용(Y)으로 등록된 결제대행사가 목록에 표시됩니다. PG를 고르면 API연동설정의 MID·Route 등이 기본값으로 채워지며, 가맹점 전용 값은 수정·저장하면 됩니다. 예정모드·N·D시각은 위 「통합정산설정」에서 일괄 지정합니다. URL·챗봇·API 결제는 운영(체크)를 여러 행에 켤 수 있습니다. 체크된 행은 붉은 배경(파스텔)로 표시됩니다. 노티 전용 PG는 카드브랜드가 ALL로 고정됩니다. 하단 [저장] 시 한꺼번에 반영됩니다.': {
      EN: 'Deployment → API integration (Y) lists all acquirers. Picking a PG fills MID·Route from HQ defaults; edit and save merchant-specific values. Scheduled mode·N·D time are set in bulk under Integrated settlement above. URL·chatbot·API can have multiple live rows checked; checked rows use a pastel red highlight. Notify-only PGs fix card brand to ALL. Click [Save] at the bottom to apply all at once.',
      JP: 'デプロイ設定＞API連携(使用Y)の決済代行が一覧に出ます。PG選択で本社のMID・Routeが既定入力。予定モード・N・D時刻は上の「統合精算設定」で一括。URL・チャットボット・APIは運用チェックを複数行に可。チェック行はパステル赤背景。ノティ専用PGはカードブランドALL固定。下の［保存］で一括反映。',
      CH: '部署 API 联动（启用 Y）的收单行会全部列出；选 PG 后填入总部 MID·Route 默认值，商户专用值可改后保存。预定模式·N·D 时间在上方「整合结算设置」批量设置。URL·聊天机器人·API 可多行勾选运营；选中行淡红底。纯通知 PG 卡品牌固定 ALL。底部【保存】一次生效。',
      TH: 'API ที่เปิด Y แสดงครบ เลือก PG เติม MID·Route จาก HQ แก้ค่าร้านแล้วบันทึก โหมดกำหนด·N·D ตั้งรวมด้านบน หลายแถว URL/แชทบอท/API เปิดใช้ได้ แถวที่เลือกพื้นหลังแดงอ่อน PG แจ้งเตือนอย่างเดียวการ์ด ALL กด [บันทึก] ล่างสุดครั้งเดียว'
    },
    '배포설정 > API연동설정에서 사용(Y)으로 등록된 결제대행사가 목록에 표시됩니다. PG를 고르면 API연동설정의 MID·Route 등이 기본값으로 채워지며, 가맹점 전용 값은 수정·저장하면 됩니다. URL·챗봇·API 결제는 운영(체크)를 여러 행에 켤 수 있습니다. 체크된 행은 붉은 배경(파스텔)로 표시됩니다. 노티 전용 PG는 카드브랜드가 ALL로 고정됩니다. 등록 화면은 하단 [저장] 시 한꺼번에 반영됩니다.': {
      EN: 'Acquirers enabled (Y) under Deployment → API integration are listed. Selecting a PG prefills MID·Route from those defaults; edit merchant-specific values and save. URL, chatbot, and API checkout can each have multiple live rows; checked rows use a pastel red highlight. Notify-only PGs lock card brand to ALL. On this registration screen, click [Save] at the bottom to apply all changes at once.',
      JP: 'デプロイ設定＞API連携で使用(Y)の決済代行が一覧に表示されます。PGを選ぶとMID・Route等が連携設定の既定で埋まり、加盟店固有は編集して保存します。URL・チャットボット・API決済は運用チェックを複数行にできます。チェック行はパステル調の赤背景です。ノティ専用PGはカードブランドALL固定です。登録画面では下の［保存］で一括反映されます。',
      CH: '在「部署」>「API 联动」中设为启用 (Y) 的收单行会显示在列表中。选择 PG 后，会用联动配置的 MID·Route 等作为默认填入；商户专用值可修改后保存。URL、聊天机器人、API 支付可勾选多行运营；已勾选行以淡红底高亮。纯通知 PG 的卡品牌固定为 ALL。本注册页请使用底部【保存】一次全部提交。',
      TH: 'ผู้ให้บริการที่เปิดใช้ (Y) ในการเชื่อม API แสดงในรายการ เลือก PG แล้วเติม MID·Route เป็นค่าเริ่มต้นจากการตั้งค่า แก้ค่าเฉพาะร้านแล้วบันทึกได้ URL·แชทบอท·API เปิดหลายแถวได้ แถวที่เลือกพื้นหลังแดงอ่อน PG แจ้งเตือนอย่างเดียวการ์ด ALL คงที่ หน้าลงทะเบียนกด [บันทึก] ล่างสุดเพื่อยืนยันทั้งหมด'
    },
    N: {
      EN: 'N (T+N / D+N lag)',
      JP: 'N（T+N・D+Nの遅延日数）',
      CH: 'N（T+N / D+N 滞后天数）',
      TH: 'N (ช้า T+N / D+N วัน)'
    },
    'T/D 시 1~10': {
      EN: '1–10 when T or D',
      JP: 'T/D時は1～10',
      CH: 'T 或 D 时填 1~10',
      TH: 'เมื่อ T หรือ D ใส่ 1–10'
    },
    상품명: { EN: 'Product name', JP: '商品名', CH: '商品名称', TH: 'ชื่อสินค้า' },
    상품코드: { EN: 'Product code', JP: '商品コード', CH: '商品代码', TH: 'รหัสสินค้า' },
    기본금액: { EN: 'Default amount', JP: 'デフォルト金額', CH: '默认金额', TH: 'ยอดเริ่มต้น' },
    상품설명: { EN: 'Product description', JP: '商品説明', CH: '商品说明', TH: 'คำอธิบายสินค้า' },
    '챗봇 상단 로고': {
      EN: 'Chatbot header logo',
      JP: 'チャットボット上部ロゴ',
      CH: '聊天机器人顶部 Logo',
      TH: 'โลโก้หัวแชทบอท'
    },
    로고설정: {
      EN: 'Logo setting',
      JP: 'ロゴ設定',
      CH: 'Logo 设置',
      TH: 'ตั้งค่าโลโก้'
    },
    '웹결제 상단 로고': {
      EN: 'Web payment header logo',
      JP: 'ウェブ決済上部ロゴ',
      CH: '网页支付顶部 Logo',
      TH: 'โลโก้หัวหน้าชำระเงินเว็บ'
    },
    '기본(총판 로고)': {
      EN: 'Default (distributor logo)',
      JP: '基本（総販ロゴ）',
      CH: '默认（总代 Logo）',
      TH: 'ค่าเริ่มต้น (โลโก้ตัวแทนหลัก)'
    },
    비활성: {
      EN: 'Disabled',
      JP: '無効',
      CH: '禁用',
      TH: 'ปิดใช้งาน'
    },
    '활성(가맹 로고)': {
      EN: 'Active (merchant logo)',
      JP: '有効（加盟店ロゴ）',
      CH: '启用（商户 Logo）',
      TH: 'เปิดใช้งาน (โลโก้ร้านค้า)'
    },
    경고메세지: {
      EN: 'Warning message',
      JP: '警告メッセージ',
      CH: '警告信息',
      TH: 'ข้อความเตือน'
    },
    '경고메세지 문구': {
      EN: 'Warning message text',
      JP: '警告メッセージ文言',
      CH: '警告信息文案',
      TH: 'ข้อความเตือน'
    },
    '기본(3DS 안전 결제)': {
      EN: 'Default (3DS secure payment)',
      JP: '基本（3DSセキュア決済）',
      CH: '默认（3DS 安全支付）',
      TH: 'ค่าเริ่มต้น (3DS ชำระเงินปลอดภัย)'
    },
    '활성(직접 입력)': {
      EN: 'Active (custom text)',
      JP: '有効（直接入力）',
      CH: '启用（自定义文案）',
      TH: 'เปิดใช้งาน (พิมพ์เอง)'
    },
    '결제창 로고 아래에 표시할 문구': {
      EN: 'Text shown below checkout logo',
      JP: '決済画面ロゴ下に表示する文言',
      CH: '显示在支付页 Logo 下方的文案',
      TH: 'ข้อความใต้โลโก้หน้าชำระเงิน'
    },
    '「활성」일 때만 직접 입력 가능합니다. 「기본」은 3DS 안전 결제 문구가 언어별로 표시됩니다. 로고설정이 비활성이면 문구도 표시되지 않습니다.': {
      EN: 'Custom text is editable only when Active. Default shows 3DS secure payment per language. If logo is disabled, this text is hidden too.',
      JP: '「有効」時のみ直接入力できます。「基本」は言語別の3DSセキュア決済です。ロゴ設定が無効の場合は文言も非表示です。',
      CH: '仅「启用」时可编辑。「默认」按语言显示 3DS 安全支付。若 Logo 设为禁用，此文案也不显示。',
      TH: 'แก้ไขได้เมื่อเลือก「เปิดใช้งาน」เท่านั้น 「ค่าเริ่มต้น」แสดง 3DS ตามภาษา หากปิดโลโก้ ข้อความนี้จะไม่แสดง'
    },
    '「활성」일 때만 업로드 가능합니다. PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하(본사 AI챗봇설정과 동일)로 재압축합니다.': {
      EN: 'Upload is available only when Active is selected. PNG/JPEG, max 40MB original; server recompresses to about 2MB (same as HQ AI chatbot settings).',
      JP: '「有効」選択時のみアップロード可能。PNG・JPEG、原稿最大40MB。サーバーで目標2MB以下に再圧縮（本社AIチャットボット設定と同様）。',
      CH: '仅在选择「启用」时可上传。PNG/JPEG，原图最大 40MB；服务器压缩至约 2MB（与总部 AI 聊天机器人设置相同）。',
      TH: 'อัปโหลดได้เมื่อเลือก「เปิดใช้งาน」เท่านั้น PNG/JPEG ต้นฉบับสูงสุด 40MB เซิร์ฟเวอร์บีบอัดเป้าหมายไม่เกิน 2MB (เช่นเดียวกับตั้งค่า AI แชทบอทสำนักงานใหญ่)'
    },
    '업로드·최적화': {
      EN: 'Upload & optimize',
      JP: 'アップロード・最適化',
      CH: '上传并优化',
      TH: 'อัปโหลดและปรับให้เหมาะ'
    },
    '사용 (대표 이메일)': {
      EN: 'On (primary email)',
      JP: '使用（代表メール）',
      CH: '启用（主邮箱）',
      TH: 'ใช้ (อีเมลหลัก)'
    },
    '정산담당자연락처': {
      EN: 'Settlement contact phone',
      JP: '精算担当者連絡先',
      CH: '结算负责人联系电话',
      TH: 'เบอร์ติดต่อผู้รับผิดชอบการชำระเงิน'
    },
    '계좌정보': { EN: 'Account details', JP: '口座情報', CH: '账户信息', TH: 'ข้อมูลบัญชี' },
    '계좌은행': { EN: 'Bank (account)', JP: '口座銀行', CH: '开户银行', TH: 'ธนาคารบัญชี' },
    '국가': { EN: 'Country', JP: '国', CH: '国家', TH: 'ประเทศ' },
    '지점이름': { EN: 'Branch name', JP: '支店名', CH: '网点名称', TH: 'ชื่อสาขา' },
    '지점 주소': { EN: 'Branch address', JP: '支店住所', CH: '网点地址', TH: 'ที่อยู่สาขา' },
    '담당전화번호': { EN: 'Contact phone', JP: '担当電話番号', CH: '联系电话', TH: 'เบอร์ติดต่อผู้รับผิดชอบ' },
    '코인 지갑 주소': {
      EN: 'Crypto wallet address',
      JP: 'コインウォレットアドレス',
      CH: '加密钱包地址',
      TH: 'ที่อยู่กระเป๋าคริปโต'
    },
    '코인 수취 지갑 주소': {
      EN: 'Receiving wallet address',
      JP: 'コイン受取用ウォレットアドレス',
      CH: '收款钱包地址',
      TH: 'ที่อยู่กระเป๋ารับเหรียญ'
    },
    '네트워크': { EN: 'Network', JP: 'ネットワーク', CH: '网络', TH: 'เครือข่าย' },
    '네트워크 이름': {
      EN: 'Network name',
      JP: 'ネットワーク名',
      CH: '网络名称',
      TH: 'ชื่อเครือข่าย'
    },
    '기준 화폐1*': {
      EN: 'Base currency 1*',
      JP: '基準通貨1*',
      CH: '基准货币1*',
      TH: 'สกุลเงินฐาน 1*'
    },
    '기준 화폐2': {
      EN: 'Base currency 2',
      JP: '基準通貨2',
      CH: '基准货币2',
      TH: 'สกุลเงินฐาน 2'
    },
    '기준 화폐3': {
      EN: 'Base currency 3',
      JP: '基準通貨3',
      CH: '基准货币3',
      TH: 'สกุลเงินฐาน 3'
    },
    '기준 화폐*': {
      EN: 'Base currency*',
      JP: '基準通貨*',
      CH: '基准货币*',
      TH: 'สกุลเงินฐาน*'
    },
    '송금자명(입금시)': {
      EN: 'Remitter name (on deposit)',
      JP: '振込人名義（入金時）',
      CH: '汇款人名称（入账时）',
      TH: 'ชื่อผู้โอน (ตอนรับเงิน)'
    },
    '입금 시 송금자명': {
      EN: 'Remitter name as shown on deposit',
      JP: '入金時の振込人名義',
      CH: '入账时的汇款人名称',
      TH: 'ชื่อผู้โอนตามที่เห็นตอนรับเงิน'
    },
    '크립토 이체 수수료(USD)': {
      EN: 'Crypto transfer fee (USD)',
      JP: '暗号資産振込手数料(USD)',
      CH: '加密货币转账手续费(USD)',
      TH: 'ค่าธรรมเนียมโอนคริปโต (USD)'
    },
    이체수수료: {
      EN: 'Transfer fee',
      JP: '振込手数料',
      CH: '转账手续费',
      TH: 'ค่าธรรมเนียมโอน'
    },
    기준화폐: {
      EN: 'Base currency',
      JP: '基準通貨',
      CH: '基准货币',
      TH: 'สกุลเงินฐาน'
    },
    잔액알림금액: {
      EN: 'Balance alert amount',
      JP: '残高通知金額',
      CH: '余额提醒金额',
      TH: 'ยอดแจ้งเตือนคงเหลือ'
    },
    '의심거래/오류알림': {
      EN: 'Suspicious txn / error alert',
      JP: '疑わしい取引/エラー通知',
      CH: '可疑交易/错误通知',
      TH: 'แจ้งเตือนทุจริต/ข้อผิดพลาด'
    },
    해외로그인알림: {
      EN: 'Overseas login alert',
      JP: '海外ログイン通知',
      CH: '境外登录通知',
      TH: 'แจ้งเตือนล็อกอินต่างประเทศ'
    },
    임시비밀번호알림: {
      EN: 'Temporary password alert',
      JP: '仮パスワード通知',
      CH: '临时密码通知',
      TH: 'แจ้งเตือนรหัสผ่านชั่วคราว'
    },
    비거래기준월: {
      EN: 'Non-transaction base (months)',
      JP: '非取引基準（月）',
      CH: '非交易基准（月）',
      TH: 'เกณฑ์ไม่มีธุรกรรม (เดือน)'
    },
    '동일카드 중복결제 한도(WEB)*': {
      EN: 'Same-card duplicate pay limit (WEB)*',
      JP: '同一カード重複決済上限(WEB)*',
      CH: '同卡重复支付限额(WEB)*',
      TH: 'วงเงินชำระซ้ำบัตรเดียวกัน (WEB)*'
    },
    '동일카드 중복결제 한도(단말)*': {
      EN: 'Same-card duplicate pay limit (terminal)*',
      JP: '同一カード重複決済上限(端末)*',
      CH: '同卡重复支付限额(终端)*',
      TH: 'วงเงินชำระซ้ำบัตรเดียวกัน (เทอร์มินัล)*'
    },
    회: {
      EN: 'Times',
      JP: '回',
      CH: '次',
      TH: 'ครั้ง'
    },
    원: {
      EN: 'KRW',
      JP: 'ウォン',
      CH: '韩元',
      TH: 'วอน'
    },
    '신규회원 한도 자동전환*': {
      EN: 'Auto-switch new-member limit*',
      JP: '新規会員限度の自動切替*',
      CH: '新会员限额自动切换*',
      TH: 'สลับวงเงินสมาชิกใหม่อัตโนมัติ*'
    },
    '신규회원 일한도*': {
      EN: 'New-member daily limit*',
      JP: '新規会員の日次限度*',
      CH: '新会员日限额*',
      TH: 'วงเงินรายวันสมาชิกใหม่*'
    },
    '전환기준일*': {
      EN: 'Switch reference date*',
      JP: '切替基準日*',
      CH: '切换基准日*',
      TH: 'วันที่อ้างอิงการสลับ*'
    },
    '전환 일한도*': {
      EN: 'Post-switch daily limit*',
      JP: '切替後の日次限度*',
      CH: '切换后日限额*',
      TH: 'วงเงินรายวันหลังสลับ*'
    },
    '적용시작일*': {
      EN: 'Effective start date*',
      JP: '適用開始日*',
      CH: '生效开始日*',
      TH: 'วันเริ่มใช้*'
    },
    정산정보: {
      EN: 'Settlement info',
      JP: '精算情報',
      CH: '结算信息',
      TH: 'ข้อมูลการชำระ'
    },
    'PG수수료(일반)*': {
      EN: 'PG fee (general)*',
      JP: 'PG手数料（一般）*',
      CH: 'PG 手续费（一般）*',
      TH: 'ค่าธรรมเนียม PG (ทั่วไป)*'
    },
    '차액정산 월횟수': {
      EN: 'Net settlement runs / month',
      JP: '差額精算の月回数',
      CH: '差额结算月次数',
      TH: 'จำนวนรอบชำระสุทธิต่อเดือน'
    },
    '정산보고서 은행*': {
      EN: 'Settlement report bank*',
      JP: '精算レポート銀行*',
      CH: '结算报告银行*',
      TH: 'ธนาคารในรายงานชำระ*'
    },
    'PG수수료(삼성페이)': {
      EN: 'PG fee (Samsung Pay)',
      JP: 'PG手数料（Samsung Pay）',
      CH: 'PG 手续费（Samsung Pay）',
      TH: 'ค่าธรรมเนียม PG (Samsung Pay)'
    },
    SMS수수료: {
      EN: 'SMS fee',
      JP: 'SMS手数料',
      CH: '短信手续费',
      TH: 'ค่า SMS'
    },
    '세금계산서 이메일': {
      EN: 'Tax invoice email',
      JP: '税計算書メール',
      CH: '税务发票邮箱',
      TH: 'อีเมลใบกำกับภาษี'
    },
    직결수수료: {
      EN: 'Direct-connect fee',
      JP: '直結手数料',
      CH: '直连手续费',
      TH: 'ค่าธรรมเนียมเชื่อมตรง'
    },
    솔루션수수료: {
      EN: 'Solution fee',
      JP: 'ソリューション手数料',
      CH: '方案手续费',
      TH: 'ค่าโซลูชัน'
    },
    '예금주명*': {
      EN: 'Account holder*',
      JP: '口座名義*',
      CH: '账户户名*',
      TH: 'ชื่อบัญชี*'
    },
    '일 이용료': {
      EN: 'Daily usage fee',
      JP: '日額利用料',
      CH: '日使用费',
      TH: 'ค่าธรรมเนียมรายวัน'
    },
    '입금자명조회*': {
      EN: 'Depositor name lookup*',
      JP: '入金者名義照会*',
      CH: '汇款人名查询*',
      TH: 'การค้นหาชื่อผู้โอน*'
    },
    '이체/출금 인증번호': {
      EN: 'Transfer / payout OTP',
      JP: '振込/出金認証番号',
      CH: '转账/出款验证码',
      TH: 'รหัสยืนยันโอน/ถอน'
    },
    미조회: {
      EN: 'Do not look up',
      JP: '照会しない',
      CH: '不查询',
      TH: 'ไม่ค้นหา'
    },
    '결제 제한 시간 설정': {
      EN: 'Payment time restriction',
      JP: '決済制限時間の設定',
      CH: '支付限制时间设置',
      TH: 'จำกัดเวลาชำระเงิน'
    },
    '단말 결제제한*': {
      EN: 'Terminal pay restriction*',
      JP: '端末決済制限*',
      CH: '终端支付限制*',
      TH: 'จำกัดชำระผ่านเทอร์มินัล*'
    },
    'WEB 결제제한*': {
      EN: 'WEB pay restriction*',
      JP: 'WEB決済制限*',
      CH: 'WEB 支付限制*',
      TH: 'จำกัดชำระ WEB*'
    },
    '기본 수수료 설정': {
      EN: 'Default fee settings',
      JP: '基本手数料の設定',
      CH: '默认手续费设置',
      TH: 'ตั้งค่าค่าธรรมเนียมเริ่มต้น'
    },
    '기본 결제한도 설정': {
      EN: 'Default payment limit settings',
      JP: '基本決済限度の設定',
      CH: '默认支付限额设置',
      TH: 'ตั้งค่าวงเงินชำระเริ่มต้น'
    },
    '기본 터미널 정보': {
      EN: 'Default terminal info',
      JP: '基本端末情報',
      CH: '默认终端信息',
      TH: 'ข้อมูลเทอร์มินัลเริ่มต้น'
    },
    상세정보: {
      EN: 'Details',
      JP: '詳細情報',
      CH: '详细信息',
      TH: 'รายละเอียด'
    },
    정산형태: {
      EN: 'Settlement type',
      JP: '精算形態',
      CH: '结算形态',
      TH: 'รูปแบบการชำระ'
    },
    가맹점별: {
      EN: 'Per merchant',
      JP: '加盟店別',
      CH: '按商户',
      TH: 'ต่อร้านค้า'
    },
    '요율(%)': {
      EN: 'Rate (%)',
      JP: '料率(%)',
      CH: '费率(%)',
      TH: 'อัตรา (%)'
    },
    요율: {
      EN: 'Rate',
      JP: '料率',
      CH: '费率',
      TH: 'อัตรา'
    },
    사용한도: {
      EN: 'Usage limit',
      JP: '利用限度',
      CH: '使用额度',
      TH: 'วงเงินใช้งาน'
    },
    '필수 노티': {
      EN: 'Required notify',
      JP: '必須ノティ',
      CH: '必填通知',
      TH: 'แจ้งเตือนบังคับ'
    },
    '노티 쌍 선택': {
      EN: 'Pick notify pair',
      JP: 'ノティペアを選択',
      CH: '选择通知对',
      TH: 'เลือกคู่แจ้งเตือน'
    },
    '본사 노티구성에서 연결 시 자동 입력됩니다.': {
      EN: 'Auto-filled when wired in HQ notify configuration.',
      JP: '本社ノティ構成で接続すると自動入力されます。',
      CH: '在总部通知配置中连接后会自动填入。',
      TH: 'เชื่อมในตั้งค่าแจ้งเตือน HQ แล้วจะเติมอัตโนมัติ'
    },
    '본사설정 > 노티구성설정에서 총판에 노티 대상을 연결하세요.': {
      EN: 'In HQ settings → Notify wiring, link notify targets to this distributor.',
      JP: '本社設定＞ノティ構成でこの総販にノティ先を接続してください。',
      CH: '在总部设置 > 通知环境配置中为该总代连接通知对象。',
      TH: 'ที่ตั้งค่า HQ > การเชื่อมแจ้งเตือน ให้เชื่อมเป้าหมายกับตัวแทนหลัก'
    },
    '노티 CALLBACK (URL 1)*': {
      EN: 'Notify CALLBACK (URL 1)*',
      JP: 'ノティ CALLBACK (URL1)*',
      CH: '通知 CALLBACK (URL 1)*',
      TH: 'แจ้งเตือน CALLBACK (URL 1)*'
    },
    '노티 RESULT (URL 2)*': {
      EN: 'Notify RESULT (URL 2)*',
      JP: 'ノティ RESULT (URL2)*',
      CH: '通知 RESULT (URL 2)*',
      TH: 'แจ้งเตือน RESULT (URL 2)*'
    },
    노티선택: {
      EN: 'Pick notify',
      JP: 'ノティを選択',
      CH: '选择通知',
      TH: 'เลือกแจ้งเตือน'
    },
    '보조 노티': {
      EN: 'Auxiliary notify',
      JP: '補助ノティ',
      CH: '辅助通知',
      TH: 'แจ้งเตือนเสริม'
    },
    '보조 쌍 선택': {
      EN: 'Pick aux pair',
      JP: '補助ペアを選択',
      CH: '选择辅助对',
      TH: 'เลือกคู่เสริม'
    },
    'URL 3·4를 같은 쌍으로 채웁니다.': {
      EN: 'Fills URL 3 and 4 as one pair.',
      JP: 'URL3・4を同じペアで埋めます。',
      CH: '将 URL 3 与 4 作为一对填写。',
      TH: 'เติม URL 3 และ 4 เป็นคู่เดียวกัน'
    },
    '노티 URL 3(보조)': {
      EN: 'Notify URL 3 (aux)',
      JP: 'ノティURL3（補助）',
      CH: '通知 URL 3（辅助）',
      TH: 'แจ้งเตือน URL 3 (เสริม)'
    },
    '노티 URL 4(보조)': {
      EN: 'Notify URL 4 (aux)',
      JP: 'ノティURL4（補助）',
      CH: '通知 URL 4（辅助）',
      TH: 'แจ้งเตือน URL 4 (เสริม)'
    },
    '1회 한도*': {
      EN: 'Per-txn limit*',
      JP: '1回限度*',
      CH: '单笔限额*',
      TH: 'วงเงินต่อรายการ*'
    },
    '일 한도*': {
      EN: 'Daily limit*',
      JP: '日次限度*',
      CH: '日限额*',
      TH: 'วงเงินรายวัน*'
    },
    '월 한도*': {
      EN: 'Monthly limit*',
      JP: '月次限度*',
      CH: '月限额*',
      TH: 'วงเงินรายเดือน*'
    },
    '연 한도(법인)*': {
      EN: 'Annual limit (corp.)*',
      JP: '年次限度（法人）*',
      CH: '年限额（法人）*',
      TH: 'วงเงินรายปี (นิติบุคคล)*'
    },
    '연 한도(개인)*': {
      EN: 'Annual limit (individual)*',
      JP: '年次限度（個人）*',
      CH: '年限额（个人）*',
      TH: 'วงเงินรายปี (บุคคลธรรมดา)*'
    },
    '출금 제한 설정': {
      EN: 'Withdrawal restriction',
      JP: '出金制限設定',
      CH: '出款限制设置',
      TH: 'ตั้งค่าจำกัดการถอนเงิน'
    },
    '출금 제한 시간 설정': {
      EN: 'Withdrawal time window',
      JP: '出金制限時間設定',
      CH: '出款时间限制设置',
      TH: 'ตั้งค่าช่วงเวลาจำกัดการถอน'
    },
    '출금제한 유형': {
      EN: 'Restriction type',
      JP: '出金制限種別',
      CH: '出款限制类型',
      TH: 'ประเภทข้อจำกัดการถอน'
    },
    '시작시간': { EN: 'Start time', JP: '開始時刻', CH: '开始时间', TH: 'เวลาเริ่ม' },
    '종료시간': { EN: 'End time', JP: '終了時刻', CH: '结束时间', TH: 'เวลาสิ้นสุด' },
    '출금제한시작시간': {
      EN: 'Restriction start time',
      JP: '出金制限開始時刻',
      CH: '限制开始时间',
      TH: 'เวลาเริ่มจำกัดการถอน'
    },
    '출금제한종료시간': {
      EN: 'Restriction end time',
      JP: '出金制限終了時刻',
      CH: '限制结束时间',
      TH: 'เวลาสิ้นสุดจำกัดการถอน'
    },
    '지급한도 설정': {
      EN: 'Payout limits',
      JP: '支払限度設定',
      CH: '支付限额设置',
      TH: 'ตั้งค่าวงเงินจ่าย'
    },
    '기본한도(원)': {
      EN: 'Base limit (KRW)',
      JP: '基本限度（ウォン）',
      CH: '基本限额（韩元）',
      TH: 'วงเงินพื้นฐาน (วอน)'
    },
    '추가한도(원)': {
      EN: 'Additional limit (KRW)',
      JP: '追加限度（ウォン）',
      CH: '追加限额（韩元）',
      TH: 'วงเงินเพิ่มเติม (วอน)'
    },
    '한도알림': { EN: 'Limit alert', JP: '限度通知', CH: '限额提醒', TH: 'แจ้งเตือนวงเงิน' },
    '1회 지급한도': {
      EN: 'Per-payout limit',
      JP: '1回あたりの支払限度',
      CH: '单笔支付限额',
      TH: 'วงเงินต่อครั้งที่จ่าย'
    },
    '보류율 설정': {
      EN: 'Rolling hold rate',
      JP: '保留率設定',
      CH: '滚动保留比例设置',
      TH: 'ตั้งค่าอัตรากันวงเงิน (โรลลิง)'
    },
    '수수료정책': {
      EN: 'Fee policy',
      JP: '手数料ポリシー',
      CH: '手续费政策',
      TH: 'นโยบายค่าธรรมเนียม'
    },
    '결제 후속조치 (가맹점 관리자)': {
      EN: 'Post-payment actions (merchant admin)',
      JP: '決済後フォロー（加盟店管理者）',
      CH: '支付后续处理（商户管理员）',
      TH: 'การดำเนินการหลังชำระเงิน (ผู้ดูแลร้านค้า)'
    },
    '정산방법': {
      EN: 'Settlement method',
      JP: '精算方法',
      CH: '结算方式',
      TH: 'วิธีการชำระเงิน'
    },
    '통합정산설정': {
      EN: 'Integrated settlement',
      JP: '統合精算設定',
      CH: '整合结算设置',
      TH: 'การชำระเงินแบบรวม'
    },
    '결제대행사 설정': {
      EN: 'Payment provider (PG) setup',
      JP: '決済代行設定',
      CH: '支付机构设置',
      TH: 'ตั้งค่าผู้ให้บริการชำระเงิน (PG)'
    },
    '챗봇결제 설정': {
      EN: 'Chatbot checkout',
      JP: 'チャットボット決済設定',
      CH: '聊天机器人支付设置',
      TH: 'ตั้งค่าชำระผ่านแชทบอท'
    },
    'URL·챗봇 결제 승인 알림': {
      EN: 'URL / chatbot payment approval alerts',
      JP: 'URL・チャットボット決済承認通知',
      CH: 'URL / 聊天机器人支付批准通知',
      TH: 'การแจ้งเตือนอนุมัติชำระ URL / แชทบอท'
    },
    '미사용이면 로그인한 가맹점 관리자에게 챗봇관리의 상품관리 메뉴가 표시되지 않습니다. 챗봇결제 URL은 공개 결제 화면 진입용입니다. 프로모션 표시 방식·순환 간격은 챗봇관리 「상품관리」에서 설정합니다.': {
      EN: 'If disabled, the Product management menu under Chatbot admin is hidden for the logged-in merchant admin. The chatbot payment URL opens the public checkout. Promotion layout and rotation are set under Chatbot admin » Product management.',
      JP: '未使用の場合、ログイン中の加盟店管理者にはチャットボット管理の「商品管理」メニューが表示されません。チャットボット決済URLは公開決済画面への入口です。プロモーション表示方式・ローテーション間隔はチャットボット管理「商品管理」で設定します。',
      CH: '停用后，已登录的商户管理员将不显示聊天机器人管理中的「商品管理」菜单。聊天机器人支付 URL 用于进入公开支付页。促销展示方式与轮播间隔在聊天机器人管理「商品管理」中设置。',
      TH: 'ปิดใช้งานแล้ว ผู้ดูแลร้านที่ล็อกอินจะไม่เห็นเมนูจัดการสินค้าในแชทบอท URL ชำระแชทบอทเป็นทางเข้าหน้าชำระสาธารณะ โหมดโปรโมชันและช่วงหมุนเวียนตั้งในแชทบอท » จัดการสินค้า'
    },
    '인라인 DirectCredit(URL·챗봇) 승인 시 PG중계 JSON 전송과 함께 LINE Notify·대표 이메일(전산 SMTP) 알림을 보낼 수 있습니다. 토큰은 비우면 기존 유지, 삭제는 __CLEAR__.': {
      EN: 'On inline URL payment approval (ChillPay DirectCredit, JPAY pay_index, URL / chatbot), you can send LINE Notify and representative email (system SMTP) together with the PG relay JSON. Leave the token blank to keep the current value; use __CLEAR__ to remove it.',
      JP: 'インラインURL決済承認時（ChillPay DirectCredit・JPAY pay_index・URL/チャットボット）に、PG中継JSONとあわせてLINE Notify・代表メール（電算SMTP）通知を送れます。トークンは空欄で現状維持、削除は__CLEAR__です。',
      CH: '内联 URL 支付批准时（ChillPay DirectCredit、JPAY pay_index、URL / 聊天机器人），可与 PG 中继 JSON 一并发送 LINE Notify 与代表邮箱（系统 SMTP）通知。令牌留空保留原值；删除填 __CLEAR__。',
      TH: 'เมื่ออนุมัติชำระ URL แบบอินไลน์ (ChillPay DirectCredit, JPAY pay_index, URL/แชทบอท) สามารถส่ง LINE Notify และอีเมลตัวแทน (SMTP ระบบ) พร้อม JSON รีเลย์ PG ได้ เว้นโทเคนไว้คงค่าเดิม ลบใส่ __CLEAR__'
    },
    '즉시 적용': {
      EN: 'Apply immediately',
      JP: '即時適用',
      CH: '立即生效',
      TH: 'ใช้ทันที'
    },
    '다음 정산 실행 후(예약)': {
      EN: 'After next settlement run (scheduled)',
      JP: '次回精算実行後（予約）',
      CH: '下次结算执行后（预约）',
      TH: 'หลังรันชำระครั้งถัดไป (จอง)'
    },
    '해당없음': {
      EN: 'Not applicable',
      JP: '該当なし',
      CH: '不适用',
      TH: 'ไม่เกี่ยวข้อง'
    },
    'LINE Notify 토큰': {
      EN: 'LINE Notify token',
      JP: 'LINE Notify トークン',
      CH: 'LINE Notify 令牌',
      TH: 'โทเค็น LINE Notify'
    },
    '변경 시만 입력 · 삭제: __CLEAR__': {
      EN: 'Enter only when changing · delete: __CLEAR__',
      JP: '変更時のみ入力・削除：__CLEAR__',
      CH: '仅在变更时填写·删除：__CLEAR__',
      TH: 'กรอกเมื่อเปลี่ยนเท่านั้น · ลบ: __CLEAR__'
    },
    'SALE,RESERVATION_TIME 예: 시간예약만': {
      EN: 'e.g. SALE,RESERVATION_TIME — time-slot reservation only',
      JP: '例: SALE,RESERVATION_TIME — 時間枠予約のみ',
      CH: '例：SALE,RESERVATION_TIME — 仅时段预约',
      TH: 'เช่น SALE,RESERVATION_TIME — เฉพาะจองตามช่วงเวลา'
    },
    '기타 입력': { EN: 'Other (text)', JP: 'その他（入力）', CH: '其他（填写）', TH: 'อื่นๆ (พิมพ์)' },
    '비우면 연동(tb_pg_agency) 기본': {
      EN: 'If empty, use integration (tb_pg_agency) default',
      JP: '空欄なら連携(tb_pg_agency)の既定',
      CH: '留空则使用联动(tb_pg_agency)默认值',
      TH: 'ว่างใช้ค่าเริ่มต้นจากการเชื่อมต่อ (tb_pg_agency)'
    },
    '본사 설정 (환기준)': {
      EN: 'HQ settings (FX basis)',
      JP: '本社設定（為替基準）',
      CH: '总部设置（汇兑基准）',
      TH: 'ตั้งค่าสำนักงานใหญ่ (ฐาน FX)'
    },
    '카드사별 동일카드 제한': {
      EN: 'Per-card-brand same-card limits',
      JP: 'カード会社別・同一カード制限',
      CH: '按卡组织的同卡限制',
      TH: 'จำกัดบัตรเดียวกันตามค่ายการ์ด'
    },
    '총본사 로그인 시에만 본사를 추가할 수 있습니다. 본사는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.': {
      EN: 'Regional HQs can be added only when logged in as the root HQ. An HQ may set up to three base currencies.',
      JP: '本社の追加は総本社ログイン時のみ可能です。本社は基準通貨を最大3種類まで指定できます。',
      CH: '仅总总部登录时可添加地区总部。地区总部最多可指定三种基准货币。',
      TH: 'เพิ่มสำนักภูมิภาคได้เฉพาะเมื่อล็อกอินเป็นสำนักงานใหญ่ราก กำหนดสกุลฐานได้สูงสุด 3 สกุล'
    },
    '영업일 · 휴일 (본사)': {
      EN: 'Business days & holidays (HQ)',
      JP: '営業日・休日（本社）',
      CH: '营业日与节假日（总部）',
      TH: 'วันทำการและวันหยุด (สำนักงานใหญ่)'
    },
    '영업일 상세는 [본사설정 > 영업일설정]에서 관리합니다. 여기서는 적용할 설정 이름을 선택하세요.': {
      EN: 'Business-day details are managed under [HQ settings > Business-day settings]. Here, pick the profile name to apply.',
      JP: '営業日の詳細は[本社設定＞営業日設定]で管理します。ここでは適用する設定名を選択してください。',
      CH: '营业日细则在【总部设置 > 营业日设置】中维护。此处请选择要应用的配置名称。',
      TH: 'รายละเอียดวันทำการจัดการที่ [ตั้งค่า HQ > ตั้งค่าวันทำการ] ที่นี่เลือกชื่อโปรไฟล์ที่จะใช้'
    },
    '본사 업체 상세 정보': {
      EN: 'HQ company details',
      JP: '本社取引先の詳細',
      CH: '总部企业详细信息',
      TH: 'รายละเอียดบริษัทสำนักงานใหญ่'
    },
    '본사 등록 시 입력합니다.': {
      EN: 'Enter when registering an HQ.',
      JP: '本社登録時に入力します。',
      CH: '在注册总部时填写。',
      TH: 'กรอกเมื่อลงทะเบียนสำนักงานใหญ่'
    },
    '본사 기본 출금 제한 정책입니다. 매일: 시작~종료 매일 적용. 공휴일: 당일 00:00~23:59 전면 제한, 그 외 영업일은 시작~종료. 공휴일 전날 17시/18시 이후: 전영업일 해당 시각~공휴일 23:59(시작이 17·18시보다 이르면 시작시간부터), 그 외 날은 시작~종료. 실제 출금 시 본사 영업일·공휴일 데이터와 함께 판단합니다.': {
      EN: 'Default HQ payout restriction. Daily: start–end every day. Holidays: full block 00:00–23:59; other business days use start–end. Pre-holiday after 17:00/18:00: from that time on the prior business day through holiday 23:59 (if start is earlier than 17/18, from start). Otherwise start–end. Actual payouts also use HQ business/holiday data.',
      JP: '本社既定の出金制限です。毎日:開始～終了を毎日適用。公休日:当日00:00～23:59は全面制限、その他営業日は開始～終了。公休前日17時/18時以降:前営業日の該当時刻～公休日23:59(開始が17/18より早い場合は開始から)。それ以外は開始～終了。実際の出金は本社営業日・休日データと併せて判定します。',
      CH: '总部默认出款限制。每日：每天应用起止时段。节假日：当日 00:00–23:59 全面限制；其他工作日用起止。节假日前一日 17/18 点后：从前一工作日该时刻至节假日 23:59（若开始早于 17/18 则从开始）。其余日起止。实际出款结合总部工作日历判断。',
      TH: 'นโยบายจำกัดการถอนเริ่มต้นของ HQ รายวัน·วันหยุด·ก่อนวันหยุด ใช้ร่วมกับปฏิทินวันทำการ'
    },
    '총판일 때만 입력합니다. 총판은 1가지 화폐만 지정할 수 있습니다. 필수 노티(URL 1·2)는 본사설정 > 노티구성설정에서 이 총판에 노티 대상을 연결하면 자동 반영되며 화면에서 수정할 수 없습니다. 보조(URL 3·4)는 [보조 쌍 선택] 또는 드롭다운으로 추가할 수 있습니다. 연결된 본사 수신 URL로 유입되는 노티가 이 총판 트리로 분기됩니다.': {
      EN: 'For distributors only; one base currency. Mandatory notify URLs 1–2 sync from HQ notify wiring and cannot be edited here. Aux URLs 3–4 can be set via [Aux pair pick] or dropdown. HQ-bound inbound notifies route into this distributor tree.',
      JP: '総販のときのみ入力。総販は基準通貨を1種類のみ。必須ノティ(URL1・2)は本社設定＞ノティ構成でこの総販に接続すると自動反映され画面では変更不可。補助(URL3・4)は[補助ペア選択]またはドロップダウンで追加。接続された本社受信URLからのノティがこの総販ツリーに振り分けられます。',
      CH: '仅总代填写；总代只能指定一种基准货币。必填通知 URL1·2 由总部通知配置连接后自动同步，此处不可改。辅助 URL3·4 可用【辅助成对选择】或下拉添加。来自已绑定总部接收 URL 的通知会路由到该总代树。',
      TH: 'กรอกเฉพาะตัวแทนหลัก สกุลเดียว URL บังคับ 1–2 ซิงค์จากการเชื่อม HQ'
    },
    '가맹점일 때만 입력합니다. 기준 화폐를 비우고 저장하면 상위 총판·본사 프로필의 기준통화를 자동으로 상속합니다(결제내역 VIEW의 본사/총판/가맹 기준통화 열에 반영).': {
      EN: 'For merchants only. If base currency is left blank and saved, the merchant inherits the distributor/HQ profile base currency (shown in payment list columns for HQ/distributor/merchant).',
      JP: '加盟店のときのみ入力。基準通貨を空にして保存すると、上位総販・本社プロフィールの基準通貨を自動継承します（決済一覧VIEWの本社/総販/加盟店基準通貨列に反映）。',
      CH: '仅商户填写。若基准货币留空并保存，将自动继承上级总代/总部配置（体现在支付列表的总部/总代/商户基准货币列）。',
      TH: 'เฉพาะร้านค้า ว่างสกุลฐานแล้วบันทึกจะสืบทอดจากตัวแทนหลัก/HQ'
    },
    '칠페이 통합정산 화면의 「예정(ICOPAY)」 표시에만 쓰입니다. 배포설정 API연동설정과 동일 규칙을 쓰려면 예정모드를 연동기본으로 두세요. 아래 값은 등록 시 입력한 모든 결제대행사 행에 동일하게 적용됩니다.': {
      EN: 'Only for the “Scheduled (ICOPAY)” label on integrated settlement. Set scheduled mode to integration default to match Deployment API rules. Values below apply to every PG row entered at registration.',
      JP: 'ChillPay統合精算画面の「予定(ICOPAY)」表示専用です。デプロイAPI連携と同じ規則にするには予定モードを連携既定にしてください。下の値は登録時に入力した全決済代行行に同じく適用されます。',
      CH: '仅用于整合结算画面的「预计(ICOPAY)」显示。与部署 API 规则一致请将预定模式设为联动默认。下方值在注册时应用到所有已填支付机构行。',
      TH: 'ใช้เฉพาะป้ายกำหนดการ(ICOPAY) ค่าด้านล่างใช้กับทุกแถว PG ที่กรอกตอนลงทะเบียน'
    },
    '배포설정 > API연동설정에서 사용(Y)으로 등록된 결제대행사가 목록에 표시됩니다. PG를 고르면 API연동설정의 MID·Route 등이 기본값으로 채워지며, 가맹점 전용 값은 수정·저장하면 됩니다. 실제 결제 운영 PG는 라디오(운영)로 하나만 지정합니다. 라디오가 켜진 행만 붉은 배경(파스텔)으로 표시됩니다. 등록 화면은 하단 [저장] 시 한꺼번에 반영됩니다.': {
      EN: 'Lists PGs enabled (Y) under Deployment → API integration. Choosing a PG fills MID·Route defaults; per-merchant overrides can be edited and saved. Only one live PG via the “live” radio; enabled rows use a pastel red highlight. On registration, [Save] at the bottom applies all rows at once.',
      JP: 'デプロイ設定＞API連携で使用(Y)の決済代行が一覧に表示されます。PGを選ぶとMID・Route等が既定で入り、加盟店専用は編集して保存。本番運用PGはラジオで1件のみ。ラジオONの行はパステル赤背景。登録画面は下部[保存]で一括反映します。',
      CH: '列出部署 API 联动中启用(Y) 的支付机构。选择 PG 后填入 MID·Route 等默认值；可按商户修改保存。实际运营 PG 单选唯一；选中行为淡红底。注册页底部【保存】一次应用所有行。',
      TH: 'แสดง PG ที่เปิดใช้ในการตั้งค่า API เลือกแล้วเติม MID·Route ค่าเฉพาะร้านแก้ได้ เลือก PG จริงทีละหนึ่ง บันทึกด้านล่างใช้ทุกแถวพร้อมกัน'
    },
    '본사·총판만 설정 가능. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.': {
      EN: 'HQ and distributors only. Main image = login left background; logos = login top and sidebar top.',
      JP: '本社・総販のみ設定可能。メイン画像=ログイン左背景、ロゴ=ログイン上・サイドバー上。',
      CH: '仅总部与总代可设。主图=登录左侧背景；Logo=登录顶部与侧栏顶部。',
      TH: 'ตั้งได้เฉพาะ HQ/ตัวแทนหลัก ภาพหลัก=พื้นหลังซ้ายหน้าเข้า โลโก้=ด้านบน'
    },
    '총본사/본사/총판 공통 설정입니다. COPYRIGHT에 입력한 문구는 화면 하단에 표시됩니다.': {
      EN: 'Shared root HQ / HQ / distributor settings. COPYRIGHT text appears in the page footer.',
      JP: '総本社/本社/総販の共通設定です。COPYRIGHTの文言は画面下部に表示されます。',
      CH: '总总部/总部/总代共用设置。COPYRIGHT 文案显示在页面底部。',
      TH: 'ตั้งค่าร่วมสำนักราก/HQ/ตัวแทนหลัก ข้อความ COPYRIGHT ท้ายหน้า'
    },
    '메인이미지는 5MB, 로고·URL결제·파비콘 이미지는 1MB까지 업로드 가능합니다. 파비콘은 PNG/JPG 업로드 시 32x32 PNG로 자동 변환되어 적용됩니다.': {
      EN: 'Main image up to 5 MB; logo, URL-pay, and favicon images up to 1 MB. PNG/JPG favicons are auto-converted to 32×32 PNG.',
      JP: 'メイン画像は最大5MB、ロゴ・URL決済・ファビコンは最大1MBまでアップロード可能です。ファビコンはPNG/JPGアップロード時に32×32 PNGへ自動変換されます。',
      CH: '主图最大 5MB；Logo、URL 支付与网站图标最大 1MB。PNG/JPG 网站图标将自动转为 32×32 PNG。',
      TH: 'ภาพหลักสูงสุด 5MB โลโก้·URL·ไอคอน สูงสุด 1MB แปลง favicon เป็น PNG 32×32 อัตโนมัติ'
    },
    메인이미지: { EN: 'Main image', JP: 'メイン画像', CH: '主图', TH: 'ภาพหลัก' },
    '업로드 파일명': {
      EN: 'Uploaded file name',
      JP: 'アップロードファイル名',
      CH: '上传文件名',
      TH: 'ชื่อไฟล์ที่อัปโหลด'
    },
    '첫화면 로고이미지(로그인 페이지)': {
      EN: 'First-screen logo (login page)',
      JP: '初画面ロゴ（ログインページ）',
      CH: '首屏 Logo（登录页）',
      TH: 'โลโก้หน้าแรก (หน้าเข้า)'
    },
    '로그인 후 로고이미지(좌측 메뉴)': {
      EN: 'Post-login logo (left menu)',
      JP: 'ログイン後ロゴ（左メニュー）',
      CH: '登录后 Logo（左侧菜单）',
      TH: 'โลโก้หลังล็อกอิน (เมนูซ้าย)'
    },
    'URL결제이미지(공개 결제 페이지 상단)': {
      EN: 'URL-pay image (top of public checkout)',
      JP: 'URL決済画像（公開決済ページ上部）',
      CH: 'URL 支付图（公开支付页顶部）',
      TH: 'ภาพชำระ URL (ด้านบนหน้าชำระสาธารณะ)'
    },
    '비우면 URL 결제 상단에는 「로그인 후 로고」가 표시됩니다. 총판(상위) 브랜딩이 가맹점 결제 URL에 적용됩니다.': {
      EN: 'If empty, the post-login logo shows at the top of URL checkout. Parent distributor branding applies to merchant pay URLs.',
      JP: '空欄の場合、URL決済上部には「ログイン後ロゴ」が表示されます。上位総販のブランディングが加盟店決済URLに適用されます。',
      CH: '留空则在 URL 支付顶部显示「登录后 Logo」。上级总代的品牌会应用到商户支付 URL。',
      TH: 'ว่างจะแสดงโลโก้หลังล็อกอินด้านบน URL ชำระ แบรนด์ตัวแทนหลักระดับบนใช้กับ URL ร้านค้า'
    },
    '파비콘 이미지': { EN: 'Favicon', JP: 'ファビコン画像', CH: '网站图标', TH: 'ไอคอนเว็บ' },
    배경테마: { EN: 'Background theme', JP: '背景テーマ', CH: '背景主题', TH: 'ธีมพื้นหลัง' },
    '기본(현재)': { EN: 'Default (current)', JP: '既定（現在）', CH: '默认（当前）', TH: 'ค่าเริ่มต้น (ปัจจุบัน)' },
    'Light (흰배경/검정글씨)': {
      EN: 'Light (white background / black text)',
      JP: 'Light（白背景・黒文字）',
      CH: '浅色（白底黑字）',
      TH: 'Light (พื้นขาว ตัวอักษรดำ)'
    },
    'Gray (라이트·다크 중간 톤)': {
      EN: 'Gray (between light and dark)',
      JP: 'Gray（ライトとダークの中間）',
      CH: '灰色（介于浅色与深色）',
      TH: 'Gray (โทนกลาง)'
    },
    'Brown (상단 메뉴 톤 정렬)': {
      EN: 'Brown (align with top menu tone)',
      JP: 'Brown（上部メニュー系トーン）',
      CH: '棕色（与顶栏菜单色调对齐）',
      TH: 'Brown (โทนเมนูบน)'
    },
    'Dark (어두운배경/흰글씨)': {
      EN: 'Dark (dark background / white text)',
      JP: 'Dark（暗背景・白文字）',
      CH: '深色（暗底白字）',
      TH: 'Dark (พื้นมืด ตัวอักษรขาว)'
    },
    파스텔1: { EN: 'Pastel 1', JP: 'パステル1', CH: '柔和色 1', TH: 'พาสเทล 1' },
    파스텔2: { EN: 'Pastel 2', JP: 'パステル2', CH: '柔和色 2', TH: 'พาสเทล 2' },
    파스텔3: { EN: 'Pastel 3', JP: 'パステル3', CH: '柔和色 3', TH: 'พาสเทล 3' },
    파스텔4: { EN: 'Pastel 4', JP: 'パステル4', CH: '柔和色 4', TH: 'พาสเทล 4' },
    파스텔5: { EN: 'Pastel 5', JP: 'パステル5', CH: '柔和色 5', TH: 'พาสเทล 5' },
    '사이트 이름(브라우저 탭)': {
      EN: 'Site name (browser tab)',
      JP: 'サイト名（ブラウザタブ）',
      CH: '站点名称（浏览器标签）',
      TH: 'ชื่อไซต์ (แท็บเบราว์เซอร์)'
    },
    '예: OTL PAY 관리자': {
      EN: 'e.g. OTL PAY Admin',
      JP: '例: OTL PAY 管理',
      CH: '例如：OTL PAY 管理',
      TH: 'เช่น OTL PAY Admin'
    },
    '로그인 안내 호스트': {
      EN: 'Login notice host',
      JP: 'ログイン案内ホスト',
      CH: '登录提示主机名',
      TH: 'โฮสต์แจ้งเตือนการเข้า'
    },
    '예: api.example.com (선택)': {
      EN: 'e.g. api.example.com (optional)',
      JP: '例: api.example.com（任意）',
      CH: '例如 api.example.com（可选）',
      TH: 'เช่น api.example.com (ไม่บังคับ)'
    },
    '업체코드는 등록 저장 시에만 자동 부여되며(업체구분별 접두 2자리+순번 8자리), 부여 후에는 변경할 수 없습니다. 업체관리 목록에 동일 코드로 표시됩니다. 업체구분을 선택하면 해당 입력 항목이 표시됩니다. 조직 이동은 상위로만 가능하며(하위로 이동 불가), 이동 시 하위 전체가 함께 이동합니다. 사용여부 미사용 시 하위 전체 미사용, 가맹점은 상위 변경으로 개별 활성화할 수 있습니다. 비밀번호는 입력 후 옆 [저장]으로 확정한 뒤 하단 [저장]으로 등록하세요. 등록 후 비밀번호를 잊었거나 초기화가 필요하면 [업체정보조회] 또는 [업체정보] 상세에서 [비밀번호 초기화] 후 로그인ID+1! 로 로그인해 변경하면 됩니다.': {
      EN: 'Company codes are auto-assigned only on first save (2-letter org prefix + 8-digit sequence) and cannot be changed afterward; the same code appears in company management. Picking an org type shows the matching fields. Moves are upward only (not down); moving a node moves its subtree. Disabling a parent disables descendants; merchants can be re-enabled under another active parent. Confirm the password with [Save] beside the field, then register with [Save] at the bottom. If you forget it after registration, use [Merchant lookup] or [Company info] detail → [Reset password], then log in with loginId+1! and change it.',
      JP: '取引先コードは初回保存時のみ自動付与（区分別2文字接頭+8桁連番）し、付与後は変更不可です。一覧にも同じコードで表示されます。区分を選ぶと該当項目が表示されます。組織移動は上位のみ（下位へは不可）、移動時は配下すべてが一緒に移動します。未使用にすると配下も未使用、加盟店は上位変更で個別に有効化できます。パスワードは入力後横の[保存]で確定し、下部[保存]で登録してください。登録後に忘れた場合は[加盟店照会]または[取引先情報]詳細の[パスワード初期化]後、ログインID+1!でログインして変更してください。',
      CH: '企业代码仅在首次保存时自动分配（类型前缀2位+序号8位），分配后不可更改，并在企业管理列表中显示。选择企业类型后显示对应字段。组织仅可向上迁移（不可向下），迁移时子树一并迁移。父级停用时子级全部停用；商户可通过更换上级单独启用。密码先在旁侧【保存】确认，再通过底部【保存】注册。若遗忘，请在【商户查询】或【企业信息】详情中【重置密码】后使用 登录ID+1! 登录并修改。',
      TH: 'รหัสบริษัทออกอัตโนมัติตอนบันทึกครั้งแรก (คำนำหน้า 2 + เลข 8 หลัก) แก้ไม่ได้หลังออก ย้ายองค์กรได้เฉพาะขึ้นบน ปิดการใช้งานที่บนปิดลูกทั้งหมด ร้านค้าเปิดใหม่ได้เมื่อเปลี่ยนหัวหน้า รหัสผ่านกดบันทึกข้างช่องแล้วบันทึกด้านล่าง ลืมรหัสใช้รีเซ็ตจากเมนูข้อมูล'
    },
    건당료: { EN: 'Per-txn fee', JP: '件当手数料', CH: '按笔费用', TH: 'ค่าธรรมเนียมต่อรายการ' },
    '요율%': { EN: 'Rate %', JP: '料率%', CH: '费率%', TH: 'อัตรา %' },
    적용시작일: { EN: 'Effective from', JP: '適用開始日', CH: '生效开始日', TH: 'ใช้ตั้งแต่วันที่' },
    기준통화: { EN: 'Base currency', JP: '基準通貨', CH: '基准货币', TH: 'สกุลฐาน' },
    처리: { EN: 'Actions', JP: '操作', CH: '操作', TH: 'ดำเนินการ' },
    합계: { EN: 'Total', JP: '合計', CH: '合计', TH: 'รวม' },
    '업체선택(조직)': {
      EN: 'Company (org)',
      JP: '取引先選択（組織）',
      CH: '企业选择（组织）',
      TH: 'เลือกบริษัท (องค์กร)'
    },
    '적용시작일을 비우면 저장 시점(서버 시각) 기준으로 적용됩니다.': {
      EN: 'If the effective start date is blank, it is applied as of the save time (server clock).',
      JP: '適用開始日を空にすると、保存時点（サーバー時刻）基準で適用されます。',
      CH: '若留空生效开始日，则按保存时刻（服务器时间）生效。',
      TH: 'ว่างวันที่เริ่มใช้จะถือเวลาบันทึก (เซิร์ฟเวอร์)'
    },
    '동일 가맹점에 미래 적용일이 중복되지 않도록 한 번에 한 건만 등록하는 것을 권장합니다.': {
      EN: 'To avoid overlapping future effective dates for the same merchant, register one row at a time.',
      JP: '同一加盟店で将来の適用日が重複しないよう、一度に1件だけ登録することを推奨します。',
      CH: '为避免同一商户未来生效日重叠，建议每次只登记一行。',
      TH: 'แนะนำลงทะเบียนทีละแถวเพื่อไม่ให้วันที่ซ้ำในร้านเดียวกัน'
    },
    '상위 조직 수수료 정책이 바뀌면 이후 신규 가맹점 등록 시 하위 배분 설정에 반영될 수 있습니다.': {
      EN: 'If an upstream org fee policy changes, new merchant registrations may inherit updated downstream splits.',
      JP: '上位組織の手数料ポリシーが変わると、以降の新規加盟店登録時に下位の配分設定へ反映される場合があります。',
      CH: '上级组织手续费政策变更后，新注册商户的下级分成设置可能会随之变化。',
      TH: 'นโยบายระดับบนเปลี่ยน การลงทะเบียนร้านใหม่อาจได้สัดส่วนล่างที่อัปเดต'
    },
    'VIEW SETTING 열 목록은 본사설정 → 조직항목설정(화면: 수수료관리)에서 허용한 키와 동일합니다. 신규 열 「통화(policyCur)」는 적용 수수료 정책의 통화코드(ISO 숫자·알파)를 THB·JPY 등 알파로 표시합니다. 조직항목설정을 바꾼 뒤 새로고침·재조회하면 체크 목록·노출 제한이 반영됩니다.': {
      EN: 'VIEW SETTING columns match keys allowed in HQ settings → Org columns (screen: Commission management). The new 「Currency (policyCur)」 column shows the applied policy currency code (ISO numeric/alpha) as THB, JPY, etc. After changing org columns, refresh and search again to update the checklist and visibility rules.',
      JP: 'VIEW SETTING の列一覧は、本社設定 → 組織項目設定（画面：手数料管理）で許可したキーと同じです。新列「通貨(policyCur)」は適用手数料ポリシーの通貨コード（ISO 数字・アルファ）を THB・JPY 等のアルファで表示します。組織項目設定を変更した後は再読込・再検索でチェック一覧と表示制限が反映されます。',
      CH: 'VIEW SETTING 列与「总部设置 → 组织字段」（手续费管理）允许的键一致。新列「货币(policyCur)」将适用手续费政策的货币代码显示为 THB、JPY 等。修改组织字段后请刷新并重新查询以更新勾选与可见性。',
      TH: 'คอลัมน์ VIEW SETTING ตรงกับที่อนุญาตใน ตั้งค่า HQ → คอลัมน์องค์กร (หน้าจัดการค่าธรรมเนียม) คอลัมน์ใหม่ policyCur แสดงรหัสสกุลเงินเป็น THB/JPY ฯลฯ หลังแก้ให้รีเฟรชและค้นหาใหม่'
    },
    시작일시: { EN: 'Start time', JP: '開始日時', CH: '开始时间', TH: 'เวลาเริ่ม' },
    종료일시: { EN: 'End time', JP: '終了日時', CH: '结束时间', TH: 'เวลาสิ้นสุด' },
    변경자: { EN: 'Changed by', JP: '変更者', CH: '修改人', TH: 'ผู้แก้ไข' },
    '클릭하여 수정': {
      EN: 'Click to edit',
      JP: 'クリックして編集',
      CH: '点击编辑',
      TH: 'คลิกเพื่อแก้ไข'
    },
    '총본사 · 업체명': {
      EN: 'Root HQ · name',
      JP: '総本部・取引先名',
      CH: '总总部·名称',
      TH: 'สำนักงานใหญ่ราก·ชื่อ'
    },
    '본사 · 업체명': {
      EN: 'HQ · name',
      JP: '本社・取引先名',
      CH: '总部·名称',
      TH: 'สำนักงานใหญ่·ชื่อ'
    },
    '총판 · 업체명': {
      EN: 'Distributor · name',
      JP: '総販・取引先名',
      CH: '总代·名称',
      TH: 'ตัวแทนหลัก·ชื่อ'
    },
    '지사 · 업체명': {
      EN: 'Branch · name',
      JP: '支社・取引先名',
      CH: '分公司·名称',
      TH: 'สาขา·ชื่อ'
    },
    '대리점 · 업체명': {
      EN: 'Agency · name',
      JP: '代理店・取引先名',
      CH: '代理店·名称',
      TH: 'ตัวแทน·ชื่อ'
    },
    '영업점 · 업체명': {
      EN: 'Sales office · name',
      JP: '営業店・取引先名',
      CH: '营业点·名称',
      TH: 'สำนักงานขาย·ชื่อ'
    },
    '합계 · 가맹 기준통화(프로필)': {
      EN: 'Total · merchant base currency (profile)',
      JP: '合計・加盟店基準通貨（プロフィール）',
      CH: '合计·商户基准货币（档案）',
      TH: 'รวม·สกุลฐานร้านค้า (โปรไฟล์)'
    },
    '적용 정책 통화(THB·JPY 등)': {
      EN: 'Applied policy currency (THB, JPY, …)',
      JP: '適用ポリシー通貨（THB・JPY 等）',
      CH: '适用政策货币（THB、JPY 等）',
      TH: 'สกุลเงินนโยบายที่ใช้ (THB·JPY ฯลฯ)'
    },
    '수수료 변경 히스토리': {
      EN: 'Fee change history',
      JP: '手数料変更履歴',
      CH: '手续费变更历史',
      TH: 'ประวัติการเปลี่ยนค่าธรรมเนียม'
    },
    '조회 전': { EN: 'Before search', JP: '検索前', CH: '查询前', TH: 'ก่อนค้นหา' },
    '목록에서 가맹점 행을 클릭하면 해당 업체의 변경 이력이 표시됩니다.': {
      EN: 'Click a merchant row in the list to show that company’s change history.',
      JP: '一覧で加盟店の行をクリックすると、当該取引先の変更履歴が表示されます。',
      CH: '在列表中点击商户行即可显示该企业的变更历史。',
      TH: 'คลิกแถวร้านค้าในรายการเพื่อดูประวัติการเปลี่ยนแปลง'
    },
    '수수료 이력 부제목(선택됨)': {
      EN: 'Showing: {COMP_ID} — No.1 is the currently applied fee; later rows are past periods.',
      JP: '表示中: {COMP_ID} — No.1 は現在適用中の手数料、その他の行は過去の区間です。',
      CH: '显示：{COMP_ID} — 第 1 行为当前生效手续费，其余为历史区间。',
      TH: 'แสดง: {COMP_ID} — แถวที่ 1 เป็นค่าธรรมเนียมที่ใช้อยู่ แถวถัดไปเป็นช่วงที่ผ่านมา'
    },
    '업체를 선택하세요.': {
      EN: 'Select a company.',
      JP: '取引先を選択してください。',
      CH: '请选择企业。',
      TH: 'เลือกบริษัท'
    },
    '조회 결과가 없습니다. 업체코드를 확인하세요.': {
      EN: 'No results. Check the company code.',
      JP: '該当データがありません。取引先コードを確認してください。',
      CH: '无查询结果，请核对商户代码。',
      TH: 'ไม่พบข้อมูล ตรวจสอบรหัสบริษัท'
    },
    '히스토리 조회 실패': {
      EN: 'Failed to load history.',
      JP: '履歴の取得に失敗しました。',
      CH: '历史记录加载失败。',
      TH: 'โหลดประวัติไม่สำเร็จ'
    },
    '그리드에서 행을 클릭하거나 체크한 뒤 [수수료설정]을 눌러주세요.': {
      EN: 'Click or check a row in the grid, then press [Commission settings].',
      JP: '一覧で行をクリックまたはチェックしてから[手数料設定]を押してください。',
      CH: '请在表格中点击或勾选一行，再点击【手续费设置】。',
      TH: 'คลิกหรือเลือกแถวในตารางแล้วกด [ตั้งค่าค่าธรรมเนียม]'
    },
    '업체코드를 찾을 수 없습니다.': {
      EN: 'Company code not found.',
      JP: '取引先コードが見つかりません。',
      CH: '找不到企业代码。',
      TH: 'ไม่พบรหัสบริษัท'
    },
    '업체코드를 찾을 수 없습니다. 목록을 다시 검색해 주세요.': {
      EN: 'Company code not found. Please search the list again.',
      JP: '取引先コードが見つかりません。一覧を再検索してください。',
      CH: '找不到企业代码，请重新查询列表。',
      TH: 'ไม่พบรหัสบริษัท ค้นหารายการใหม่'
    },
    '행 정보가 목록과 맞지 않습니다. 검색을 다시 해 주세요.': {
      EN: 'Row data does not match the list. Please search again.',
      JP: '行の情報が一覧と一致しません。再検索してください。',
      CH: '行数据与列表不一致，请重新查询。',
      TH: 'ข้อมูลแถวไม่ตรงกับรายการ ค้นหาใหม่'
    },
    '저장할 행을 먼저 클릭하거나(또는 체크·셀 편집)한 뒤 [저장]을 눌러주세요.': {
      EN: 'Click a row (or check it / edit a cell), then press [Save].',
      JP: '保存する行を先にクリック（またはチェック・セル編集）してから[保存]を押してください。',
      CH: '请先点击要保存的行（或勾选/编辑单元格），再点【保存】。',
      TH: 'คลิกแถว (หรือติ๊ก/แก้เซลล์) แล้วกด [บันทึก]'
    },
    '선택된 행 중 수수료 인라인 열이 없는 행이 있습니다. 해당 행의 체크를 해제하거나 목록을 확인해 주세요.': {
      EN: 'Some checked rows have no fee inline columns. Uncheck those rows or verify the list.',
      JP: '選択した行の中に手数料インライン列がない行があります。該当行のチェックを外すか一覧を確認してください。',
      CH: '部分已选行没有手续费内联列，请取消勾选或检查列表。',
      TH: 'มีแถวที่เลือกแต่ไม่มีคอลัมน์แก้ไขค่าธรรมเนียม ยกเลิกการเลือกหรือตรวจสอบรายการ'
    },
    '체크된 {COUNT}건의 수수료를 한꺼번에 저장합니다. 계속할까요?': {
      EN: 'Save fees for {COUNT} checked row(s) at once. Continue?',
      JP: 'チェックした{COUNT}件の手数料を一括保存します。続行しますか？',
      CH: '将一次性保存已勾选的 {COUNT} 行手续费，是否继续？',
      TH: 'บันทึกค่าธรรมเนียม {COUNT} แถวที่เลือกพร้อมกัน ดำเนินต่อ?'
    },
    '각 행의 그리드 값이 서버에 순서대로 반영됩니다. 정말 저장할까요?': {
      EN: 'Each row’s grid values will be applied to the server in order. Save for sure?',
      JP: '各行のグリッド値がサーバーに順番に反映されます。本当に保存しますか？',
      CH: '各行的表格值将按顺序写入服务器，确定保存？',
      TH: 'ค่าในแต่ละแถวจะถูกบันทึกตามลำดับ ยืนยันบันทึก?'
    },
    '{COUNT}건 저장을 완료했습니다.': {
      EN: 'Finished saving {COUNT} row(s).',
      JP: '{COUNT}件の保存が完了しました。',
      CH: '已完成 {COUNT} 行保存。',
      TH: 'บันทึก {COUNT} แถวเสร็จแล้ว'
    },
    '[{COMP_ID}] 수수료 배분·건당 수수료를 0으로 초기화합니다. 계속할까요?': {
      EN: 'Reset [{COMP_ID}] fee split and per-txn fees to 0. Continue?',
      JP: '[{COMP_ID}] の手数料配分・件当手数料を0に初期化します。続行しますか？',
      CH: '将 [{COMP_ID}] 的分成与按笔费用重置为 0，是否继续？',
      TH: 'รีเซ็ตการแบ่งและค่าธรรมเนียมต่อรายการของ [{COMP_ID}] เป็น 0 ดำเนินต่อ?'
    },
    '초기화 내용이 서버에 반영됩니다. 정말 진행할까요?': {
      EN: 'Reset values will be written to the server. Proceed?',
      JP: '初期化内容がサーバーに反映されます。本当に進めますか？',
      CH: '重置内容将写入服务器，确定继续？',
      TH: 'ค่าที่รีเซ็ตจะถูกบันทึกลงเซิร์ฟเวอร์ ยืนยัน?'
    },
    '[{COMP_ID}] 그리드에서 수정한 수수료를 저장합니다. 계속할까요?': {
      EN: 'Save fee edits from the grid for [{COMP_ID}]. Continue?',
      JP: '[{COMP_ID}] のグリッドで修正した手数料を保存します。続行しますか？',
      CH: '保存 [{COMP_ID}] 在表格中修改的手续费，是否继续？',
      TH: 'บันทึกการแก้ค่าธรรมเนียมในกริดของ [{COMP_ID}] ดำเนินต่อ?'
    },
    '기존 상세 수수료와 병합되어 서버에 반영됩니다. 정말 저장할까요?': {
      EN: 'They will be merged with existing detail fees on the server. Save for sure?',
      JP: '既存の詳細手数料とマージされサーバーに反映されます。本当に保存しますか？',
      CH: '将与现有明细手续费合并后写入服务器，确定保存？',
      TH: 'จะรวมกับรายละเอียดเดิมบนเซิร์ฟเวอร์ ยืนยันบันทึก?'
    },
    '수수료를 0으로 초기화했습니다.': {
      EN: 'Fees have been reset to 0.',
      JP: '手数料を0に初期化しました。',
      CH: '已将手续费重置为 0。',
      TH: 'รีเซ็ตค่าธรรมเนียมเป็น 0 แล้ว'
    },
    '수수료가 저장되었습니다.': {
      EN: 'Fees saved.',
      JP: '手数料を保存しました。',
      CH: '手续费已保存。',
      TH: 'บันทึกค่าธรรมเนียมแล้ว'
    },
    '수수료 저장 실패': {
      EN: 'Failed to save fees.',
      JP: '手数料の保存に失敗しました。',
      CH: '保存手续费失败。',
      TH: 'บันทึกค่าธรรมเนียมไม่สำเร็จ'
    },
    '수수료 조회 실패': {
      EN: 'Failed to load fees.',
      JP: '手数料の取得に失敗しました。',
      CH: '查询手续费失败。',
      TH: 'โหลดค่าธรรมเนียมไม่สำเร็จ'
    },
    '접속일자': {
      EN: 'Access date',
      JP: 'アクセス日',
      CH: '访问日期',
      TH: 'วันที่เข้าใช้'
    },
    '변경자명': {
      EN: 'Changed-by name',
      JP: '変更者名',
      CH: '修改人姓名',
      TH: 'ชื่อผู้เปลี่ยนแปลง'
    },
    '변경일시': {
      EN: 'Changed at',
      JP: '変更日時',
      CH: '修改时间',
      TH: 'วันเวลาที่เปลี่ยน'
    },
    '변경대상': {
      EN: 'Field changed',
      JP: '変更対象',
      CH: '变更项',
      TH: 'รายการที่เปลี่ยน'
    },
    '변경 전': {
      EN: 'Before',
      JP: '変更前',
      CH: '变更前',
      TH: 'ก่อนเปลี่ยน'
    },
    '변경 후': {
      EN: 'After',
      JP: '変更後',
      CH: '变更后',
      TH: 'หลังเปลี่ยน'
    },
    '변경자': {
      EN: 'Changed by',
      JP: '変更者',
      CH: '修改人',
      TH: 'ผู้เปลี่ยนแปลง'
    },
    /* 업체변경이력 — DB audit fieldLabel·값(한글 고정) */
    '신규등록요약': {
      EN: 'Registered · {0} · code {1} · use {2}',
      JP: '登録 · {0} · コード {1} · 利用 {2}',
      CH: '注册 · {0} · 代码 {1} · 使用 {2}',
      TH: 'ลงทะเบียน · {0} · รหัส {1} · การใช้ {2}'
    },
    '신규등록': {
      EN: 'New registration',
      JP: '新規登録',
      CH: '新注册',
      TH: 'การลงทะเบียนใหม่'
    },
    '[업체정보]': {
      EN: '[Merchant profile]',
      JP: '[加盟店情報]',
      CH: '[商户资料]',
      TH: '[ข้อมูลร้าน]'
    },
    '[업체등록]': {
      EN: '[Merchant registration]',
      JP: '[加盟店登録]',
      CH: '[商户注册]',
      TH: '[การลงทะเบียนร้าน]'
    },
    '[정산설정]': {
      EN: '[Settlement settings]',
      JP: '[精算設定]',
      CH: '[结算设置]',
      TH: '[การตั้งค่าการชำระ]'
    },
    '[PG연동]': {
      EN: '[PG linkage]',
      JP: '[PG連携]',
      CH: '[PG 对接]',
      TH: '[เชื่อม PG]'
    },
    '[조직권한]': {
      EN: '[Org permissions]',
      JP: '[組織権限]',
      CH: '[组织权限]',
      TH: '[สิทธิ์องค์กร]'
    },
    '[도메인구성설정]': {
      EN: '[Domain configuration]',
      JP: '[ドメイン構成設定]',
      CH: '[域名配置]',
      TH: '[การตั้งค่าโดเมน]'
    },
    '[수수료관리]': {
      EN: '[Fee management]',
      JP: '[手数料管理]',
      CH: '[手续费管理]',
      TH: '[การจัดการค่าธรรมเนียม]'
    },
    '상위업체': {
      EN: 'Parent company',
      JP: '上位加盟店',
      CH: '上级商户',
      TH: 'ร้านแม่'
    },
    '업체사용여부': {
      EN: 'Merchant active (Y/N)',
      JP: '加盟店の使用',
      CH: '商户启用',
      TH: 'การใช้งานร้าน'
    },
    '업체사용여부(상위연쇄)': {
      EN: 'Merchant active (cascade from parent)',
      JP: '加盟店の使用（上位連鎖）',
      CH: '商户启用（上级级联）',
      TH: 'การใช้งานร้าน (สืบทอดจากแม่)'
    },
    '대표전화': {
      EN: 'Main phone',
      JP: '代表電話',
      CH: '总机电话',
      TH: 'โทรศัพท์หลัก'
    },
    '우편번호': {
      EN: 'Postal code',
      JP: '郵便番号',
      CH: '邮编',
      TH: 'รหัสไปรษณีย์'
    },
    '주소': {
      EN: 'Address',
      JP: '住所',
      CH: '地址',
      TH: 'ที่อยู่'
    },
    '상세주소': {
      EN: 'Address line 2',
      JP: '住所（詳細）',
      CH: '详细地址',
      TH: 'ที่อยู่เพิ่มเติม'
    },
    '기타주소': {
      EN: 'Other address',
      JP: 'その他住所',
      CH: '其他地址',
      TH: 'ที่อยู่อื่น'
    },
    '주소국가': {
      EN: 'Address country',
      JP: '住所の国',
      CH: '地址国家',
      TH: 'ประเทศที่อยู่'
    },
    '대표자명': {
      EN: 'Representative name',
      JP: '代表者名',
      CH: '负责人姓名',
      TH: 'ชื่อผู้แทน'
    },
    '휴대폰': {
      EN: 'Mobile phone',
      JP: '携帯電話',
      CH: '手机',
      TH: 'มือถือ'
    },
    '로그인ID': {
      EN: 'Login ID',
      JP: 'ログインID',
      CH: '登录 ID',
      TH: 'รหัสเข้าใช้'
    },
    '사업자번호': {
      EN: 'Business registration no.',
      JP: '事業者番号',
      CH: '营业执照号',
      TH: 'เลขทะเบียนธุรกิจ'
    },
    '업태': {
      EN: 'Business type',
      JP: '業態',
      CH: '业态',
      TH: 'ประเภทธุรกิจ'
    },
    '종목': {
      EN: 'Industry',
      JP: '業種',
      CH: '行业',
      TH: 'ประเภทอุตสาหกรรม'
    },
    '사업자형태': {
      EN: 'Business entity type',
      JP: '事業者形態',
      CH: '主体类型',
      TH: 'รูปแบบนิติบุคคล'
    },
    '취급물품': {
      EN: 'Products handled',
      JP: '取扱品目',
      CH: '经营商品',
      TH: 'สินค้าที่รับ'
    },
    '대표사이트': {
      EN: 'Main website',
      JP: '代表サイト',
      CH: '主站',
      TH: 'เว็บไซต์หลัก'
    },
    '정산담당자명': {
      EN: 'Settlement contact name',
      JP: '精算担当者名',
      CH: '结算联系人',
      TH: 'ชื่อผู้รับผิดชอบการชำระ'
    },
    '정산담당연락처': {
      EN: 'Settlement contact phone',
      JP: '精算担当連絡先',
      CH: '结算联系电话',
      TH: 'เบอร์ผู้รับผิดชอบการชำระ'
    },
    '팩스': {
      EN: 'Fax',
      JP: 'FAX',
      CH: '传真',
      TH: 'แฟกซ์'
    },
    '이메일': {
      EN: 'Email',
      JP: 'メール',
      CH: '邮箱',
      TH: 'อีเมล'
    },
    '은행코드': {
      EN: 'Bank code',
      JP: '銀行コード',
      CH: '银行代码',
      TH: 'รหัสธนาคาร'
    },
    '송금수수료': {
      EN: 'Remittance fee',
      JP: '送金手数料',
      CH: '汇款手续费',
      TH: 'ค่าธรรมเนียมโอน'
    },
    '가상자산송금수수료': {
      EN: 'Crypto remittance fee',
      JP: '仮想資産送金手数料',
      CH: '虚拟资产汇款费',
      TH: 'ค่าธรรมเนียมโอนคริปโต'
    },
    '계좌번호': {
      EN: 'Account number',
      JP: '口座番号',
      CH: '账号',
      TH: 'เลขบัญชี'
    },
    '예금주': {
      EN: 'Account holder',
      JP: '口座名義',
      CH: '开户名',
      TH: 'ชื่อบัญชี'
    },
    '비고': {
      EN: 'Remarks',
      JP: '備考',
      CH: '备注',
      TH: 'หมายเหตุ'
    },
    '수수료설정허용': {
      EN: 'Fee settings allowed',
      JP: '手数料設定の許可',
      CH: '允许手续费设置',
      TH: 'อนุญาตตั้งค่าค่าธรรมเนียม'
    },
    '웹결제사용여부': {
      EN: 'WEB checkout enabled',
      JP: 'WEB決済の使用',
      CH: 'WEB 支付启用',
      TH: 'เปิดใช้ชำระเงิน WEB'
    },
    '사이트URL': {
      EN: 'Site URL',
      JP: 'サイトURL',
      CH: '网站 URL',
      TH: 'URL เว็บไซต์'
    },
    '사이트개요': {
      EN: 'Site summary',
      JP: 'サイト概要',
      CH: '网站简介',
      TH: 'สรุปเว็บไซต์'
    },
    '본사/지역설정(JSON)': {
      EN: 'HQ/regional settings (JSON)',
      JP: '本社/地域設定(JSON)',
      CH: '总部/区域设置(JSON)',
      TH: 'การตั้งค่าสำนักงานใหญ่/ภูมิภาค (JSON)'
    },
    '대표비밀번호': {
      EN: 'Primary password',
      JP: '代表パスワード',
      CH: '主账号密码',
      TH: 'รหัสผ่านหลัก'
    },
    '보조비밀번호': {
      EN: 'Assistant password',
      JP: '補助パスワード',
      CH: '辅助账号密码',
      TH: 'รหัสผ่านผู้ช่วย'
    },
    '출금제한유형': {
      EN: 'Withdrawal restriction type',
      JP: '出金制限タイプ',
      CH: '出款限制类型',
      TH: 'ประเภทจำกัดการถอน'
    },
    '출금제한일수': {
      EN: 'Withdrawal restriction days',
      JP: '出金制限日数',
      CH: '出款限制天数',
      TH: 'จำนวนวันจำกัดการถอน'
    },
    '출금제한시작': {
      EN: 'Withdrawal window start',
      JP: '出金制限開始',
      CH: '出款限制开始',
      TH: 'เริ่มช่วงจำกัดการถอน'
    },
    '출금제한종료': {
      EN: 'Withdrawal window end',
      JP: '出金制限終了',
      CH: '出款限制结束',
      TH: 'สิ้นสุดช่วงจำกัดการถอน'
    },
    '기본지급한도': {
      EN: 'Default payout limit',
      JP: '基本支給限度',
      CH: '默认支付限额',
      TH: 'วงเงินจ่ายเริ่มต้น'
    },
    '추가지급한도': {
      EN: 'Extra payout limit',
      JP: '追加支給限度',
      CH: '追加支付限额',
      TH: 'วงเงินจ่ายเพิ่มเติม'
    },
    '보류율': {
      EN: 'Hold rate',
      JP: '保留率',
      CH: '保留比例',
      TH: 'อัตราการกันวงเงิน'
    },
    '보류일수': {
      EN: 'Hold days',
      JP: '保留日数',
      CH: '保留天数',
      TH: 'จำนวนวันกันวงเงิน'
    },
    '정산마감시각': {
      EN: 'Settlement close time',
      JP: '精算締切時刻',
      CH: '结算截止时间',
      TH: 'เวลาปิดรอบชำระ'
    },
    '정산개시시각': {
      EN: 'Settlement open time',
      JP: '精算開始時刻',
      CH: '结算开始时间',
      TH: 'เวลาเริ่มรอบชำระ'
    },
    '이체주기일수': {
      EN: 'Transfer cycle (days)',
      JP: '振替周期（日）',
      CH: '转账周期（天）',
      TH: 'รอบโอน (วัน)'
    },
    '정산구분': {
      EN: 'Settlement mode',
      JP: '精算区分',
      CH: '结算方式',
      TH: 'โหมดการชำระ'
    },
    '이체및송금구분': {
      EN: 'Transfer/remittance type',
      JP: '振替・送金区分',
      CH: '转账/汇款类型',
      TH: 'ประเภทโอน/โอนเงิน'
    },
    '자동이체최소금액': {
      EN: 'Auto transfer minimum',
      JP: '自動振替最小金額',
      CH: '自动转账最低额',
      TH: 'ยอดขั้นต่ำโอนอัตโนมัติ'
    },
    '정산최소금액': {
      EN: 'Settlement minimum',
      JP: '精算最小金額',
      CH: '结算最低额',
      TH: 'ยอดขั้นต่ำการชำระ'
    },
    '이체및송금최소금액': {
      EN: 'Transfer & remittance minimum',
      JP: '振込・送金最小金額',
      CH: '转账与汇款最低额',
      TH: 'ยอดขั้นต่ำโอนและโอนเงิน'
    },
    '이체실행시각': {
      EN: 'Transfer run time',
      JP: '振替実行時刻',
      CH: '转账执行时间',
      TH: 'เวลารันโอน'
    },
    '지급보류': {
      EN: 'Payout hold',
      JP: '支給保留',
      CH: '支付暂缓',
      TH: 'ระงับการจ่าย'
    },
    '보류해제': {
      EN: 'Release hold',
      JP: '保留解除',
      CH: '解除暂缓',
      TH: 'ปลดพัก'
    },
    'Y→N 해제': {
      EN: 'Y→N release',
      JP: 'Y→N解除',
      CH: 'Y→N 解除',
      TH: 'ปลด Y→N'
    },
    '이 실행 건만 지급보류 Y→N 해제': {
      EN: 'Release payout hold Y→N for this run only',
      JP: 'この実行のみ支給保留をY→N解除',
      CH: '仅解除本笔执行的支付暂缓 Y→N',
      TH: 'ปลดพักจ่าย Y→N เฉพาะรันนี้'
    },
    '클릭 시 이 실행 건만 지급보류 Y→N 해제(가맹점정산내역·유통 반영). 가맹 설정의 지급보류는 변경되지 않습니다.': {
      EN: 'Click to release payout hold Y→N for this run only (posts to merchant settlement and distributor totals). The merchant payout-hold setting is not changed.',
      JP: 'クリックで当該実行のみ支給保留をY→N解除（加盟店精算一覧・流通へ反映）。加盟店設定の支給保留は変更されません。',
      CH: '点击后仅解除该执行行的支付暂缓 Y→N（写入商户结算与分销汇总）。不修改商户的支付暂缓设置。',
      TH: 'คลิกเพื่อปลดพักจ่าย Y→N เฉพาะรันนี้ (สะท้อนในรายการชำระร้านและเครือข่าย) ไม่เปลี่ยนการตั้งค่าพักจ่ายของร้าน'
    },
    '정산제외': {
      EN: 'Exclude from settlement',
      JP: '精算除外',
      CH: '排除结算',
      TH: 'ยกเว้นจากการชำระ'
    },
    '정산제외대상': {
      EN: 'Settlement exclusion target',
      JP: '精算除外対象',
      CH: '结算排除对象',
      TH: 'เป้าหมายยกเว้นการชำระ'
    },
    '수수료VAT적용': {
      EN: 'Apply fee VAT',
      JP: '手数料VAT適用',
      CH: '手续费增值税',
      TH: 'ใช้ VAT ค่าธรรมเนียม'
    },
    '수수료VAT율(%)': {
      EN: 'Fee VAT rate (%)',
      JP: '手数料VAT率(%)',
      CH: '手续费增值税率(%)',
      TH: 'อัตรา VAT ค่าธรรมเนียม (%)'
    },
    '결제대행(MID)': {
      EN: 'Acquirer binding (MID)',
      JP: '決済代行(MID)',
      CH: '收单绑定(MID)',
      TH: 'การเชื่อมผู้ให้บริการ (MID)'
    },
    '결제대행 삭제': {
      EN: 'Acquirer binding removed',
      JP: '決済代行の削除',
      CH: '删除收单绑定',
      TH: 'ลบการเชื่อมผู้ให้บริการ'
    },
    '담당자별메뉴 오버라이드 건수': {
      EN: 'Per-role menu override count',
      JP: '担当者別メニューオーバーライド件数',
      CH: '按角色的菜单覆盖条数',
      TH: 'จำนวนเมนูแยกตามบทบาท'
    },
    '메뉴권한방식': {
      EN: 'Menu permission mode',
      JP: 'メニュー権限方式',
      CH: '菜单权限模式',
      TH: 'โหมดสิทธิ์เมนู'
    },
    '개별메뉴 건수': {
      EN: 'Per-menu row count',
      JP: '個別メニュー件数',
      CH: '单独菜单条数',
      TH: 'จำนวนเมนูแยกรายการ'
    },
    '개별 설정': {
      EN: 'Custom (per menu)',
      JP: '個別設定',
      CH: '单独设置',
      TH: 'กำหนดเอง'
    },
    '단계 기본': {
      EN: 'Level default',
      JP: '階層デフォルト',
      CH: '按层级默认',
      TH: 'ค่าเริ่มต้นตามระดับ'
    },
    '설정표시명': {
      EN: 'Display name',
      JP: '設定表示名',
      CH: '显示名称',
      TH: 'ชื่อที่แสดง'
    },
    '관리자 URL': {
      EN: 'Admin URL',
      JP: '管理画面URL',
      CH: '管理端 URL',
      TH: 'URL ผู้ดูแล'
    },
    'API URL': {
      EN: 'API URL',
      JP: 'API URL',
      CH: 'API URL',
      TH: 'API URL'
    },
    '수수료·배분 저장': {
      EN: 'Fee / distribution saved',
      JP: '手数料・配分の保存',
      CH: '保存手续费与分成',
      TH: 'บันทึกค่าธรรมเนียม/การแบ่ง'
    },
    '저장 반영(상세: 수수료관리 히스토리)': {
      EN: 'Saved (see fee management history)',
      JP: '保存反映（詳細: 手数料管理履歴）',
      CH: '已保存（详见手续费管理历史）',
      TH: 'บันทึกแล้ว (ดูประวัติค่าธรรมเนียม)'
    },
    '활성': {
      EN: 'Active',
      JP: '有効',
      CH: '启用',
      TH: 'เปิดใช้'
    },
    '운영': {
      EN: 'Live',
      JP: '本番',
      CH: '生产',
      TH: 'ใช้งานจริง'
    },
    'URL금액': {
      EN: 'URL amount mode',
      JP: 'URL金額',
      CH: 'URL 金额模式',
      TH: 'โหมดจำนวน URL'
    },
    '할부': {
      EN: 'Installment',
      JP: '分割払い',
      CH: '分期',
      TH: 'ผ่อนชำระ'
    },
    '(유지)': {
      EN: '(unchanged)',
      JP: '（維持）',
      CH: '（未改）',
      TH: '(คงเดิม)'
    },
    '(변경됨)': {
      EN: '(changed)',
      JP: '（変更済）',
      CH: '（已更改）',
      TH: '(เปลี่ยนแล้ว)'
    },
    '(초기화)': {
      EN: '(reset)',
      JP: '（初期化）',
      CH: '（重置）',
      TH: '(รีเซ็ต)'
    },
    '(삭제)': {
      EN: '(deleted)',
      JP: '（削除）',
      CH: '（已删除）',
      TH: '(ลบแล้ว)'
    },
    '허용': {
      EN: 'Allowed',
      JP: '許可',
      CH: '允许',
      TH: 'อนุญาต'
    },
    '미허용': {
      EN: 'Not allowed',
      JP: '不許可',
      CH: '不允许',
      TH: 'ไม่อนุญาต'
    },
    '업체명·업체코드': {
      EN: 'Company name · code',
      JP: '加盟店名・コード',
      CH: '商户名称·代码',
      TH: 'ชื่อร้าน·รหัส'
    },
    '관리자': {
      EN: 'Administrator',
      JP: '管理者',
      CH: '管理员',
      TH: 'ผู้ดูแล'
    },
    '시스템 관리자': {
      EN: 'System administrator',
      JP: 'システム管理者',
      CH: '系统管理员',
      TH: 'ผู้ดูแลระบบ'
    },
    '시스템관리자': {
      EN: 'System administrator',
      JP: 'システム管理者',
      CH: '系统管理员',
      TH: 'ผู้ดูแลระบบ'
    },
    'PG 통합관리자': {
      EN: 'PG admin console user',
      JP: 'PG 統合管理ユーザー',
      CH: 'PG 综合管理用户',
      TH: 'ผู้ใช้คอนโซล PG'
    },
    '대표': {
      EN: 'Primary account',
      JP: '代表',
      CH: '主账号',
      TH: 'บัญชีหลัก'
    },
    '관리담당': {
      EN: 'Management',
      JP: '管理担当',
      CH: '管理担当',
      TH: 'ฝ่ายบริหาร'
    },
    '운영담당': {
      EN: 'Operations',
      JP: '運用担当',
      CH: '运营担当',
      TH: 'ฝ่ายปฏิบัติการ'
    },
    '정산담당': {
      EN: 'Settlement',
      JP: '精算担当',
      CH: '结算担当',
      TH: 'ฝ่ายชำระบัญชี'
    },
    '기술담당': {
      EN: 'Technical',
      JP: '技術担当',
      CH: '技术担当',
      TH: 'ฝ่ายเทคนิค'
    },
    '업체사용자': {
      EN: 'Merchant user',
      JP: '加盟店ユーザー',
      CH: '商户用户',
      TH: 'ผู้ใช้ร้านค้า'
    },
    '승인금액': {
      EN: 'Approved amount',
      JP: '承認金額',
      CH: '授权金额',
      TH: 'ยอดอนุมัติ'
    },
    '취소금액': {
      EN: 'Cancelled amount',
      JP: '取消金額',
      CH: '取消金额',
      TH: 'ยอดยกเลิก'
    },
    '결제금액': {
      EN: 'Payment amount',
      JP: '決済金額',
      CH: '支付金额',
      TH: 'ยอดชำระเงิน'
    },
    '총수수료': {
      EN: 'Total fees',
      JP: '手数料合計',
      CH: '手续费合计',
      TH: 'ค่าธรรมเนียมรวม'
    },
    '보류금액': {
      EN: 'Hold amount',
      JP: '保留金額',
      CH: '暂扣金额',
      TH: 'ยอดพักรอ'
    },
    '지급액': {
      EN: 'Payout',
      JP: '支払額',
      CH: '拨付金额',
      TH: 'ยอดจ่าย'
    },
    '지급예상': {
      EN: 'Expected payout',
      JP: '支払予定',
      CH: '预计拨付',
      TH: 'ยอดจ่ายโดยประมาณ'
    },
    '정산예상': {
      EN: 'Expected settlement',
      JP: '精算予定',
      CH: '预计结算',
      TH: 'ยอดชำระบัญชีโดยประมาณ'
    },
    '총거래': {
      EN: 'Total txn amount',
      JP: '総取引',
      CH: '总交易',
      TH: 'ยอดธุรกรรมรวม'
    },
    '추정결산': {
      EN: 'Est. settlement',
      JP: '推定決算',
      CH: '预估结算',
      TH: 'ประมาณการชำระบัญชี'
    },
    '성공': {
      EN: 'Success',
      JP: '成功',
      CH: '成功',
      TH: 'สำเร็จ'
    },
    '실패': {
      EN: 'Failed',
      JP: '失敗',
      CH: '失败',
      TH: 'ล้มเหลว'
    },
    '—': {
      EN: 'None',
      JP: 'なし',
      CH: '无',
      TH: 'ไม่มี'
    },
    '(전체 중 일부만 집계)': {
      EN: '(Partial aggregate — subset of full data)',
      JP: '（全体の一部のみ集計）',
      CH: '（仅为全量的一部分汇总）',
      TH: '(สรุปเพียงบางส่วนของข้อมูลทั้งหมด)'
    },
    무효: {
      EN: 'Void',
      JP: '無効',
      CH: '作废',
      TH: 'โมฆะ'
    },
    수동무효: {
      EN: 'Manual void',
      JP: '手動無効',
      CH: '手动作废',
      TH: 'โมฆะด้วยมือ'
    },
    환불: {
      EN: 'Refund',
      JP: '返金',
      CH: '退款',
      TH: 'คืนเงิน'
    },
    강제환불: {
      EN: 'Forced refund',
      JP: '強制返金',
      CH: '强制退款',
      TH: 'บังคับคืนเงิน'
    },
    취소: {
      EN: 'Cancel',
      JP: 'キャンセル',
      CH: '取消',
      TH: 'ยกเลิก'
    },
    '목록은 정산마감일 또는 정산일자(배치 실행일)가 정산기간 안에 드는 실행을 보여 줍니다. 처음 열 때는 최근정산 모드(기본: 최근 1년·정산일 최신순, 동일 정산일은 실행 등록 시각 순)입니다. [검색]을 누르면 입력한 정산기간으로 조회하며 정산일·업체코드 순으로 정렬됩니다. 「정산실행」버튼: 기간·가맹을 지정해 실행합니다(AUTO·MANUAL 모두 동일 주기·마감·격자·영업일 규칙). 검색: 정산기간·빠른기간·검색구분·정산구분(전체·자동·수동)·검색어 순으로 가맹 정산설정 기준을 좁힌 뒤 [검색]합니다. 「전체」는 해당 조건으로 좁히지 않습니다(검색어가 있을 때만 전체 컬럼 OR 검색). D+N·W+N·WK 등 달력 주기 가맹은 기간 종료일(정산일)이 해당 주기의 실행일일 때만 집계됩니다(미도래일에는 실행되지 않음). 정산마감시각·정산제외 영업일·D0 시간대는 자동 배치와 동일합니다. RT·T0·격자(M/H/TM/TH)는 조회 기간 내 거래가 있을 때만 해당 기간으로 집계합니다. H1·M 등 시간 격자는 한 구간(동일 정산 슬롯) 안에서 매출·취소·공제가 ± 함께 집계되어 한 행의 지급액이 됩니다. 서버 크론은 AUTO 가맹만 자동 호출하며, 화면 「정산실행」은 AUTO·MANUAL을 추가로 수동 트리거할 수 있습니다. 목록의 정산주기·정산방법·루트는 가맹 정산설정·PG연동에서 가져옵니다. 지급액은 순매출에서 수수료·수수료부가세·담보금(신규)를 뺀 값으로, 수수료·담보가 매출을 넘으면 음수로 표시됩니다(0으로 보정하지 않음). 그 경우 부족분 동액이 「미수금관리」에 1건 자동 등록되며(사유코드·메모로 실행과 연결), 본 목록의 미수금 열과 맞춰 볼 수 있습니다. 환수모드 AUTO 가맹은 다음 정산에서 양(+) 지급액에 환수금·미수금이 FIFO로 먼저 반영되고, MANUAL 가맹은 「미수금관리」에서 환수처리 후 차기 마감·정산에서 차감됩니다.': {
      EN: 'The list shows runs whose settlement close date or settlement execution date (batch run day) falls in the selected period. On first open it is in Recent mode (default: last year, newest run registration time first). [Search] loads the period you entered and sorts by settlement date, then merchant code. Run settlement: pick period and merchants (AUTO and MANUAL share the same cycle, cutoff, grid, and business-day rules). Search narrows by period, quick range, field, settlement type (all/auto/manual), then keyword, then [Search]. “All” does not narrow that dimension (OR across columns only when a keyword is present). D+N, W+N, WK merchants aggregate only when the period end (settlement date) is that cycle’s run day (nothing runs on future days). Cutoff time, excluded business days, and D0 windows match the batch job. RT, T0, and grids (M/H/TM/TH) aggregate only when there are txns in the queried window. H1, M, etc. sum sales, cancels, and deductions ± together inside one slot into one payout row. Server cron auto-runs AUTO merchants only; this screen can also manually trigger AUTO and MANUAL. Cycle, method, and route come from merchant settlement settings and PG links. Payout is net sales minus fees, fee VAT, and rolling collateral (new); if fees and collateral exceed sales, the value stays negative (not clamped to zero). The shortfall is auto-posted once to Receivables (reason code and memo link the run); compare with the receivable column here. Recovery mode AUTO applies recoveries and receivables FIFO to the next positive payout; MANUAL merchants use Receivables recovery, then the next close/settlement.',
      JP: '一覧は精算締め日または精算日（バッチ実行日）が指定した精算期間に含まれる実行を表示します。初回は「直近の精算」モード（既定：過去1年・実行登録日時の新しい順）です。[検索]で入力した精算期間に切り替え、精算日・加盟店コード順に並びます。「精算実行」は期間と加盟店を指定して実行します（AUTO・MANUALとも同一の周期・締め・グリッド・営業日ルール）。検索は精算期間・クイック期間・検索区分・精算区分（全体・自動・手動）・検索語の順で精算設定を絞り、[検索]します。「全体」はその条件では絞りません（検索語があるときのみ全列OR検索）。D+N・W+N・WKなどカレンダー周期の加盟店は、期間終了日（精算日）がその周期の実行日に一致するときだけ集計されます（未到来日は実行されません）。精算締め時刻・精算除外営業日・D0の扱いは自動バッチと同じです。RT・T0・格子(M/H/TM/TH)は照会期間内に取引がある場合のみその期間で集計します。H1・Mなど時間格子は同一精算スロット内で売上・取消・控除を±まとめて一行の支払額にします。サーバークロンはAUTO加盟店のみ自動実行し、本画面の「精算実行」はAUTO・MANUALを手動で追加トリガーできます。一覧の精算サイクル・精算方法・ルートは加盟店精算設定・PG連携から取得します。支払額は純売上から手数料・手数料付加税・担保金（新規）を差し引いた値で、手数料・担保が売上を超えると負のまま表示します（0に補正しません）。その場合不足額相当が「未収管理」に1件自動登録され（理由コード・メモで実行と紐づけ）、本一覧の未収列と照合できます。回収モードAUTO加盟店は次回精算の正の支払額に回収金・未収金をFIFOで先に反映し、MANUAL加盟店は「未収管理」で回収処理後に次回締め・精算で相殺されます。',
      CH: '列表显示精算截止日或精算日（批处理执行日）落在所选精算期间内的执行。首次打开为「最近精算」模式（默认：近一年、按执行登记时间从新到旧）。[搜索]按输入的精算期间查询，并按精算日、商户代码排序。「执行结算」可指定期间与商户（AUTO 与 MANUAL 使用相同的周期、截止、网格与营业日规则）。搜索顺序为：精算期间、快捷期间、搜索字段、精算类型（全部·自动·手动）、关键词，然后[搜索]。「全部」不在该维度上收窄（仅在有搜索词时做全列 OR）。D+N、W+N、WK 等日历周期商户仅在期间结束日（精算日）等于该周期的执行日时才汇总（未到的日期不会执行）。精算截止时间、排除的营业日与 D0 处理与自动批处理一致。RT、T0、网格(M/H/TM/TH) 仅在查询窗口内有交易时才按该期间汇总。H1、M 等时间网格在同一精算槽内将销售、撤销、扣款±合并为一行的拨付额。服务器定时任务仅自动调用 AUTO 商户；本屏「执行结算」也可手动触发 AUTO 与 MANUAL。列表中的精算周期、方法与路由来自商户精算设置与 PG 联动。拨付额为净销售减去手续费、手续费增值税与滚动担保（新）；若手续费与担保超过销售额则保持负数（不钳到零）。差额会自动登记一条「应收管理」（理由码与备注关联执行），可与本列表的应收列对照。回收模式 AUTO 商户在下次正拨付额上按 FIFO 先扣回收与应收；MANUAL 商户在「应收管理」处理回收后于下次截止/精算扣减。',
      TH: 'รายการแสดงรันที่วันปิดชำระหรือวันชำระ (วันรัน batch) อยู่ในช่วงที่เลือก เมื่อเปิดครั้งแรกเป็นโหมดชำระล่าสุด (ค่าเริ่มต้น: 1 ปีล่าสุด เรียงเวลาลงทะเบียนรันใหม่ก่อน) [ค้นหา] โหลดช่วงที่กรอกและเรียงตามวันชำระ แล้วรหัสร้าน 「รันชำระ」เลือกช่วงและร้าน (AUTO กับ MANUAL ใช้กฎรอบ ปิด กริด และวันทำการเดียวกัน) การค้นหาคัดโดยช่วงชำระ ช่วงด่วน ฟิลด์ ประเภทชำระ (ทั้งหมด/อัตโนมัติ/ด้วยมือ) คำสำคัญ แล้ว [ค้นหา] 「ทั้งหมด」ไม่คัดในขั้นนั้น (OR ทุกคอลัมน์เมื่อมีคำค้น) ร้านรอบปฏิทิน D+N W+N WK สรุปเมื่อวันสิ้นช่วง (วันชำระ) ตรงกับวันรันของรอบนั้น (วันที่ยังไม่ถึงจะไม่รัน) เวลาปิด วันหยุดที่ยกเว้น และ D0 เหมือนงาน batch RT T0 กริด (M/H/TM/TH) สรุปเมื่อมีธุรกรรมในช่วงที่ถาม H1 M รวมยอดขาย ยกเลิก หัก ± ในสล็อตเดียวเป็นจ่ายหนึ่งแถว cron เซิร์ฟเวอร์รัน AUTO อัตโนมัติเท่านั้น หน้านี้สามารถกดรัน AUTO/MANUAL เพิ่มได้ รอบ วิธี และเส้นทางมาจากตั้งค่าชำระร้านและลิงก์ PG จ่าย = ยอดสุทธิ − ค่าธรรมเนียม VAT ค่าธรรมเนียม หลักประกัน (ใหม่) ถ้าเกินยอดขายคงติดลบ (ไม่บังคับเป็นศูนย์) ส่วนต่างลงลูกหนี้อัตโนมัติ 1 รายการใน「ลูกหนี้」(รหัสเหตุผลและบันทึกผูกรัน) เทียบกับคอลัมน์ลูกหนี้ที่นี่ โหมดกู้คืน AUTO หักกู้คืนและลูกหนี้ FIFO จากยอดบวกครั้งถัดไป MANUAL ใช้「ลูกหนี้」ก่อน แล้วหักในรอบปิด/ชำระถัดไป'
    },
    '이번 정산 실행에 집계에 포함된 거래 건수. 컬럼 도입 이전 실행 행은 비어 있을 수 있습니다.': {
      EN: 'Txn count included in this settlement run; rows from before the column was added may be blank.',
      JP: '今回の精算実行に集計対象となった取引件数。列導入前の実行行は空の場合があります。',
      CH: '本次结算执行纳入的交易笔数；列引入前的执行行可能为空。',
      TH: 'จำนวนรายการที่รวมในรันชำระนี้ แถวก่อนมีคอลัมน์นี้อาจว่าง'
    },
    '정산 실행당 1회 정산수수료.': {
      EN: 'One batch settlement fee per run.',
      JP: '精算実行あたり1回の精算手数料。',
      CH: '每次结算执行收取一次的结算手续费。',
      TH: 'ค่าธรรมเนียมชำระแบบรายบรรจุต่อหนึ่งรัน'
    },
    '주기별 노출 요약.': {
      EN: 'Short cadence display summary.',
      JP: '周期別の表示要約。',
      CH: '按周期的展示摘要。',
      TH: 'สรุปการแสดงตามรอบ'
    },
    'PENDING·DISTRIBUTED·HOLD — 가맹점정산내역 반영 전 단계.': {
      EN: 'PENDING / DISTRIBUTED / HOLD — stage before merchant settlement statement.',
      JP: 'PENDING·DISTRIBUTED·HOLD — 加盟店精算一覧へ反映する前の段階。',
      CH: 'PENDING / DISTRIBUTED / HOLD — 写入商户结算明细之前的阶段。',
      TH: 'PENDING / DISTRIBUTED / HOLD — ก่อนสะท้อนในรายการชำระร้านค้า'
    },
    'Y면 지급보류 가맹; 배포가 HOLD로 잡힐 수 있음.': {
      EN: 'Y = payout-hold merchant; distribution may be HOLD.',
      JP: 'Yは送金保留加盟店。配布がHOLDになることがあります。',
      CH: 'Y 表示付款暂缓商户；下发可能为 HOLD。',
      TH: 'Y = พักจ่าย การแจกจ่ายอาจเป็น HOLD'
    },
    '정산 실행 PK(tb_settlement_run). 추적용.': {
      EN: 'Settlement run PK (tb_settlement_run); for tracing.',
      JP: '精算実行PK(tb_settlement_run)。追跡用。',
      CH: '结算执行主键 (tb_settlement_run)，用于追踪。',
      TH: 'PK รันชำระ (tb_settlement_run) สำหรับติดตาม'
    },
    '정산 지급부족 시 해당 실행에 자동 등록된 미수금(발생액)': {
      EN: 'Receivable auto-posted to this run when payout was short (incurred amount).',
      JP: '支払不足時に当該実行へ自動登録された未収金（発生額）。',
      CH: '拨付不足时自动记入该执行的应收（发生额）。',
      TH: 'ลูกหนี้ที่ลงอัตโนมัติเมื่อจ่ายไม่พอ (ยอดเกิด)'
    },
    '거래 집계만. 정산 실행당 1회 정산수수료는 정산료 열.': {
      EN: 'Txn totals only; the per-run batch fee is in the settlement-fee column.',
      JP: '取引集計のみ。実行ごとの1回精算手数料は精算手数料列。',
      CH: '仅为交易汇总；每执行一次的结算手续费在结算费列。',
      TH: 'เฉพาะยอดธุรกรรม ค่าธรรมเนียมรายบรรจุอยู่คอลัมน์ค่าชำระ'
    },
    '0으로 보정하지 않음; 부족 시 음수·미수금 자동등록.': {
      EN: 'Not clamped to zero; shortfalls stay negative and auto-receivable.',
      JP: '0に補正しません。不足時は負のまま・未収金を自動登録。',
      CH: '不钳到零；不足时保持负数并自动登记应收。',
      TH: 'ไม่บังคับเป็นศูนย์ ขาดเหลือติดลบและลงลูกหนี้อัตโนมัติ'
    },
    'CALCULATED=확정, PENDING=미확정.': {
      EN: 'CALCULATED = confirmed; PENDING = not confirmed.',
      JP: 'CALCULATED=確定、PENDING=未確定。',
      CH: 'CALCULATED=已确认，PENDING=未确认。',
      TH: 'CALCULATED=ยืนยันแล้ว PENDING=ยังไม่ยืนยัน'
    },
    '정산실행상세 · 정산 대상 거래': {
      EN: 'Settlement run detail · included txns',
      JP: '精算実行詳細・精算対象取引',
      CH: '结算执行详情·结算相关交易',
      TH: 'รายละเอียดรันชำระ · ธุรกรรมที่รวม'
    },
    '상단 목록 행을 더블클릭하면 표시됩니다.': {
      EN: 'Double-click a row in the list above to load it.',
      JP: '上の一覧行をダブルクリックすると表示されます。',
      CH: '双击上方列表行即可显示。',
      TH: 'ดับเบิลคลิกแถวในรายการด้านบนเพื่อแสดง'
    },
    '정산실행 목록에서 한 행을 더블클릭하면, 해당 실행에 저장된 집계 건수(included_txn_cnt)가 있으면 그 건수만큼만, 같은 기간·정렬(승인일시 오름차순)으로 표시합니다. 상단 메타의 대상 매출액은 이 실행 집계 구간(예: H1 한 시간)에 대한 승인 매출 합(정산 실행 저장값)이며, 아래 표시 행의 단순 합이 아닙니다.': {
      EN: 'In the settlement run list, double-click a row. If the run stored a count (included_txn_cnt), that many rows are shown for the same window and sort (approval time ascending). The meta target sales is the sum of approved sales for this run’s slot (e.g. one H1 hour)—the value saved on the run—not a simple sum of the rows below.',
      JP: '精算実行一覧で行をダブルクリックすると、当該実行に保存された集計件数(included_txn_cnt)がある場合はその件数だけを、同一期間・並び（承認日時昇順）で表示します。上部メタの対象売上額は、この実行の集計区間（例：H1の1時間）における承認売上合計（精算実行に保存された値）であり、下表の行を単純合計した値ではありません。',
      CH: '在结算执行列表中双击一行。若该执行保存了汇总笔数（included_txn_cnt），则仅显示该笔数，同一区间与排序（批准时间升序）。顶部元数据中的目标销售额是该执行汇总区间（例如 H1 一小时）的批准销售额合计（保存在执行上的值），不是下方各行简单相加。',
      TH: 'ในรายการรันชำระ ดับเบิลคลิกแถว หากมีจำนวนที่บันทึก (included_txn_cnt) จะแสดงเท่านั้นในช่วงและเรียงเดียวกัน (เวลาอนุมัติ ascending) ยอดขายเป้าหมายในเมตาเป็นผลรวมยอดอนุมัติของช่วงสรุปของรันนี้ (เช่น H1 หนึ่งชม.) ค่าที่บันทึกบนรัน ไม่ใช่ผลรวมแถวด้านล่าง'
    },
    '정산배포 목록에서 한 행을 클릭하면, 해당 실행의 정산일(calc_dt) 달력 하루 동안 해당 가맹의 전체 결제 거래를 승인일시 오름차순으로 불러옵니다(최대 2,500건·초과 시 상한 안내). 격자 정산의 집계 구간(H1 등)과 범위가 다를 수 있습니다.': {
      EN: 'In settlement publish, click a row to load all payment transactions for that merchant on the settlement date (calc_dt) calendar day, approval time ascending (max 2,500; notice if truncated). Grid settlement windows (H1, etc.) may differ.',
      JP: '精算配布一覧で行をクリックすると、当該実行の精算日(calc_dt)暦日1日分の当該加盟店の全決済取引を承認日時昇順で読み込みます（最大2,500件・超過時は案内）。格子精算の集計区間(H1等)と範囲が異なる場合があります。',
      CH: '在结算发布列表中点击一行，将按批准时间升序加载该执行结算日(calc_dt)日历日内该商户的全部支付交易（最多2,500条，超出会提示）。网格结算的汇总区间(H1等)可能不同。',
      TH: 'ในรายการเผยแพร่ชำระ คลิกแถวเพื่อโหลดธุรกรรมทั้งหมดของร้านในวันชำระ (calc_dt) เรียงเวลาอนุมัติ (สูงสุด 2,500)'
    },
    '정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 해당 정산 실행에 포함된 거래를 정산실행 화면과 동일한 형식으로 불러옵니다. 정산집계표(SUM)는 요약 1행만 제공되고, 본사 지급 리포트의 정산실시(EXE)는 본사 합산 행이라 실행 ID가 없을 수 있습니다 — 이 경우 리포트 형식을 가맹점 정산 리포트로 바꾼 뒤 가맹 단위 행을 클릭하세요.': {
      EN: 'In aggregate/runs/confirmed settlement, click a row with a run ID to load included transactions like the run settlement screen. SUM is one summary row; HQ EXE may lack run ID—switch to merchant report and click a merchant row.',
      JP: '精算集計・実行・確定で実行IDがある行をクリックすると、精算実行画面と同形式で対象取引を読み込みます。SUMは要約1行のみ。本社EXEは実行IDがない場合あり—加盟店レポートに切替後クリック。',
      CH: '在汇总/执行/确认结算中，点击有执行 ID 的行，以与结算执行屏相同格式加载包含的交易。SUM 仅一行摘要；总部 EXE 可能无执行 ID—请切换为商户报表后点击商户行。',
      TH: 'ในสรุป/รัน/ยืนยัน คลิกแถวที่มี run ID เพื่อโหลดธุรกรรมแบบหน้ารันชำระ SUM เป็นหนึ่งแถวสรุป EXE สำนักงานใหญ่อาจไม่มี run ID'
    },
    '정산실행 행을 더블클릭하세요.': {
      EN: 'Double-click a settlement run row.',
      JP: '精算実行の行をダブルクリックしてください。',
      CH: '请双击一条结算执行记录。',
      TH: 'ดับเบิลคลิกแถวรันชำระ'
    },
    '건당(고정) 수수료': {
      EN: 'Per-txn fixed fee',
      JP: '件当（固定）手数料',
      CH: '按笔固定手续费',
      TH: 'ค่าธรรมเนียมคงที่ต่อรายการ'
    },
    '매출 대비 % 수수료(MDR)': {
      EN: '% fee vs sales (MDR)',
      JP: '売上比%手数料（MDR）',
      CH: '相对销售额的百分比手续费（MDR）',
      TH: 'ค่าธรรมเนียม % ต่อยอดขาย (MDR)'
    },
    '승인: 기타%·수수료VAT 합 / 그 외: 무효·환불·수동무효·강제환불·실패 등 건당 수수료': {
      EN: 'Approved: other % + fee VAT / else: void, refund, manual void, forced refund, failed, etc. per-txn fees.',
      JP: '承認：その他%・手数料VATの合計／それ以外：無効・返金・手動無効・強制返金・失敗などの件当手数料。',
      CH: '批准：其他%与手续费增值税之和／否则：作废、退款、手动作废、强制退款、失败等按笔手续费。',
      TH: 'อนุมัติ: % อื่น + VAT ค่าธรรมเนียม / อื่นๆ: โมฆะ คืนเงิน โมฆะมือ บังคับคืน ล้มเหลว ฯลฯ ต่อรายการ'
    },
    '정산배포: PENDING 만 표시. 과거 DB가 V101 백필로 전부 DISTRIBUTED였다면 운영 DB에 db/V111_settlement_publish_pending_reopen.sql 적용 후 목록이 채워집니다. 처음 열 때 정산기간이 비어 있으면 최근 1년입니다. 행 클릭 시 정산일 당일 해당 가맹 전체 거래를 아래에 표시합니다. 체크 후 배포실행 → DISTRIBUTED, 홀딩실행 → HOLD.': {
      EN: 'Settlement distribution: shows PENDING only. If an older DB was fully backfilled to DISTRIBUTED (V101), run db/V111_settlement_publish_pending_reopen.sql on the production DB to repopulate this list. When opened with an empty period, the default is the last year. Click a row to load that merchant’s payments for the settlement calendar day. After selecting rows: Deploy run → DISTRIBUTED; Hold run → HOLD.',
      JP: '精算配布: PENDINGのみ表示。過去DBがV101バックフィルで全件DISTRIBUTEDの場合は、本番DBにdb/V111_settlement_publish_pending_reopen.sqlを適用すると一覧が埋まります。精算期間が空のまま開いたときは直近1年です。行をクリックすると精算日当日の当該加盟店の全決済取引を下に表示します。チェック後: 配布実行→DISTRIBUTED、ホールド実行→HOLD。',
      CH: '结算下发：仅显示 PENDING。若历史库经 V101 回填全部为 DISTRIBUTED，请在生产库执行 db/V111_settlement_publish_pending_reopen.sql 后列表才会出现数据。首次打开若精算期间为空，默认为最近一年。单击行可在下方加载该商户精算日当天全部支付。勾选后：下发执行→DISTRIBUTED；暂缓执行→HOLD。',
      TH: 'แจกจ่ายผลชำระ: แสดงเฉพาะ PENDING หาก DB เก่าถูก backfill เป็น DISTRIBUTED ทั้งหมด (V101) ให้รัน db/V111_settlement_publish_pending_reopen.sql บน DB จริงแล้วรายการจะกลับมา เมื่อเปิดโดยช่วงว่าง ค่าเริ่มต้นคือ 1 ปีล่าสุด คลิกแถวเพื่อโหลดการชำระทั้งหมดของร้านในวันปฏิทินของวันชำระ หลังเลือก: รันแจกจ่าย→DISTRIBUTED; รันพัก→HOLD'
    },
    '정산배포 · 당일 거래 내역': {
      EN: 'Settlement distribution · same-day txns',
      JP: '精算配布・当日取引明細',
      CH: '结算下发·当日交易明细',
      TH: 'แจกจ่ายผลชำระ · ธุรกรรมวันเดียวกัน'
    },
    '목록 행을 클릭하면 정산일 기준 당일 00:00~24:00 가맹 전체 거래를 표시합니다.': {
      EN: 'Click a list row to show that merchant’s txns for the settlement calendar day (00:00–24:00).',
      JP: '一覧行をクリックすると、精算日基準の当日0:00～24:00の当該加盟店の全取引を表示します。',
      CH: '单击列表行可显示该商户在精算日自然日 00:00–24:00 的全部交易。',
      TH: 'คลิกแถวในรายการเพื่อแสดงธุรกรรมทั้งหมดของร้านในวันปฏิทินของวันชำระ (00:00–24:00)'
    },
    '정산배포 목록에서 한 행을 <strong>클릭</strong>하면, 해당 실행의 <strong>정산일(calc_dt) 달력 하루</strong> 동안 해당 가맹의 <strong>전체 결제 거래</strong>를 승인일시 오름차순으로 불러옵니다(최대 2,500건·초과 시 상한 안내). 격자 정산의 집계 구간(H1 등)과 범위가 다를 수 있습니다.': {
      EN: 'In the distribution list, <strong>click</strong> a row to load that merchant’s <strong>full payment txns</strong> for the <strong>settlement calendar day (calc_dt)</strong>, approval time ascending (max 2,500; message if capped). This may differ from grid settlement windows (e.g. H1).',
      JP: '精算配布一覧で行を<strong>クリック</strong>すると、当該実行の<strong>精算日(calc_dt)の暦日</strong>における当該加盟店の<strong>全決済取引</strong>を承認日時昇順で読み込みます（最大2,500件・超過時は上限案内）。格子精算の集計区間（H1など）と範囲が異なる場合があります。',
      CH: '在结算下发列表中<strong>单击</strong>一行，可按<strong>精算日(calc_dt) 自然日</strong>加载该商户<strong>全部支付交易</strong>，按批准时间升序（最多 2,500 条，超出时提示）。可能与网格精算汇总区间（如 H1）不一致。',
      TH: 'ในรายการแจกจ่าย <strong>คลิก</strong>แถวเพื่อโหลด<strong>การชำระทั้งหมด</strong>ของร้านในวันปฏิทิน<strong>วันชำระ (calc_dt)</strong> เรียงเวลาอนุมัติ (สูงสุด 2,500 รายการ) อาจต่างจากช่วงสรุปแบบกริด (เช่น H1)'
    },
    '목록에서 행을 클릭하세요.': {
      EN: 'Click a row in the list.',
      JP: '一覧の行をクリックしてください。',
      CH: '请点击列表中的一行。',
      TH: 'คลิกแถวในรายการ'
    },
    '정산 실행 PK.': {
      EN: 'Settlement run PK.',
      JP: '精算実行PK。',
      CH: '结算执行主键。',
      TH: 'PK รันชำระ'
    },
    '0으로 보정하지 않음; 음수 가능.': {
      EN: 'Not clamped to zero; may be negative.',
      JP: '0に補正しません。負の値があり得ます。',
      CH: '不钳到零；可能为负数。',
      TH: 'ไม่บังคับเป็นศูนย์ อาจติดลบได้'
    },
    '선택한 {COUNT}건을 가맹점정산내역·유통 집계에 반영(정산배포)합니다. 계속할까요?': {
      EN: 'Reflect selected {COUNT} run(s) to merchant settlement and channel aggregates (distribute). Continue?',
      JP: '選択した{COUNT}件を加盟店精算一覧・流通集計に反映（精算配布）します。続行しますか？',
      CH: '将所选 {COUNT} 条反映到商户结算与渠道汇总（下发）。是否继续？',
      TH: 'สะท้อน {COUNT} รายการที่เลือกไปยังรายการชำระร้านค้าและสรุปช่องทาง (แจกจ่าย) ต่อหรือไม่'
    },
    '정말 배포합니다. 서버에 반영됩니다.': {
      EN: 'Confirm distribution; it will be applied on the server.',
      JP: '配布を確定します。サーバーに反映されます。',
      CH: '确认下发；将写入服务器。',
      TH: 'ยืนยันการแจกจ่าย จะบันทึกบนเซิร์ฟเวอร์'
    },
    '배포할 행을 체크하세요.': {
      EN: 'Check the rows to distribute.',
      JP: '配布する行にチェックを入れてください。',
      CH: '请勾选要下发的行。',
      TH: 'เลือกแถวที่จะแจกจ่าย'
    },
    '선택한 {COUNT}건을 정산대기(HOLD)로 둡니다. 가맹점정산내역에는 올라가지 않습니다. 계속할까요?': {
      EN: 'Leave selected {COUNT} run(s) as settlement pending (HOLD). They will not appear on merchant settlement. Continue?',
      JP: '選択した{COUNT}件を精算待ち(HOLD)のままにします。加盟店精算一覧には載りません。続行しますか？',
      CH: '将所选 {COUNT} 条保持为结算待处理 (HOLD)，不会出现在商户结算中。是否继续？',
      TH: 'ค้าง {COUNT} รายการที่เลือกเป็นรอชำระ (HOLD) จะไม่ขึ้นรายการชำระร้านค้า ต่อหรือไม่'
    },
    '정말 홀딩합니다. 서버에 반영됩니다.': {
      EN: 'Confirm hold; it will be applied on the server.',
      JP: 'ホールドを確定します。サーバーに反映されます。',
      CH: '确认暂缓；将写入服务器。',
      TH: 'ยืนยันการพัก จะบันทึกบนเซิร์ฟเวอร์'
    },
    '홀딩할 행을 체크하세요.': {
      EN: 'Check the rows to hold.',
      JP: 'ホールドする行にチェックを入れてください。',
      CH: '请勾选要暂缓的行。',
      TH: 'เลือกแถวที่จะพัก'
    },
    '홀딩 사유(선택, 최대 800자)': {
      EN: 'Hold reason (optional, max 800 chars)',
      JP: 'ホールド理由（任意、最大800文字）',
      CH: '暂缓原因（可选，最多 800 字）',
      TH: 'เหตุผลการพัก (ไม่บังคับ สูงสุด 800 ตัวอักษร)'
    },
    '처리 완료: 요청 {REQ}건 중 {CHANGED}건 배포되었습니다.': {
      EN: 'Done: {CHANGED} of {REQ} requested run(s) distributed.',
      JP: '処理完了: 要求{REQ}件中{CHANGED}件を配布しました。',
      CH: '处理完成：请求的 {REQ} 条中已下发 {CHANGED} 条。',
      TH: 'เสร็จสิ้น: แจกจ่าย {CHANGED} จาก {REQ} รายการที่ขอ'
    },
    '처리 완료: {N}건이 HOLD 처리되었습니다.': {
      EN: 'Done: {N} run(s) set to HOLD.',
      JP: '処理完了: {N}件をHOLDにしました。',
      CH: '处理完成：已将 {N} 条设为 HOLD。',
      TH: 'เสร็จสิ้น: ตั้ง HOLD {N} รายการ'
    },
    '배포 요청 실패': {
      EN: 'Distribute request failed',
      JP: '配布リクエストに失敗しました',
      CH: '下发请求失败',
      TH: 'คำขอแจกจ่ายล้มเหลว'
    },
    '홀딩 요청 실패': {
      EN: 'Hold request failed',
      JP: 'ホールドリクエストに失敗しました',
      CH: '暂缓请求失败',
      TH: 'คำขอพักล้มเหลว'
    },
    '조회만 가능한 권한입니다.': {
      EN: 'Your role is read-only on this screen.',
      JP: 'この画面は参照のみの権限です。',
      CH: '您在此画面仅有只读权限。',
      TH: 'สิทธิ์ในหน้านี้เป็นแบบอ่านอย่างเดียว'
    },
    'API 미구성': {
      EN: 'API is not configured',
      JP: 'APIが構成されていません',
      CH: '未配置 API',
      TH: 'ยังไม่ได้ตั้งค่า API'
    },
    '표시 {N}건': {
      EN: 'Showing {N}',
      JP: '表示 {N} 件',
      CH: '显示 {N} 条',
      TH: 'แสดง {N} รายการ'
    },
    '저장 건수 {N}': {
      EN: 'Saved count {N}',
      JP: '保存件数 {N}',
      CH: '保存笔数 {N}',
      TH: 'จำนวนที่บันทึก {N}'
    },
    '실행 집계 건수에 맞춤': {
      EN: 'Aligned to run aggregate count',
      JP: '実行集計件数に合わせました',
      CH: '已按执行汇总笔数对齐',
      TH: 'จัดให้ตรงกับจำนวนรวมของรัน'
    },
    '상한 초과(일부만 표시)': {
      EN: 'Cap exceeded (partial list)',
      JP: '上限超過（一部のみ表示）',
      CH: '超过上限（仅显示部分）',
      TH: 'เกินขีดจำกัด (แสดงบางส่วน)'
    },
    '조회: 정산일 당일 전체': {
      EN: 'Query: full settlement calendar day',
      JP: '照会：精算日当日の全件',
      CH: '查询：精算日当天全部',
      TH: 'ค้นหา: ทั้งวันตามปฏิทินของวันชำระ'
    },
    '대상 매출액': {
      EN: 'Target sales',
      JP: '対象売上額',
      CH: '目标销售额',
      TH: 'ยอดขายเป้าหมาย'
    },
    '정산료 1회': {
      EN: 'Batch fee (once)',
      JP: '精算手数料（1回）',
      CH: '结算手续费（一次）',
      TH: 'ค่าธรรมเนียมรายบรรจุ (ครั้งเดียว)'
    },
    '예상지급액': {
      EN: 'Expected payout',
      JP: '見込み支払額',
      CH: '预计拨付额',
      TH: 'ยอดจ่ายโดยประมาณ'
    },
    '해당 구간 거래가 없습니다.': {
      EN: 'No transactions in this range.',
      JP: '該当区間の取引がありません。',
      CH: '该区间内没有交易。',
      TH: 'ไม่มีธุรกรรมในช่วงนี้'
    },
    '조회 실패': {
      EN: 'Load failed',
      JP: '照会に失敗しました',
      CH: '查询失败',
      TH: 'โหลดล้มเหลว'
    },
    '코드: {CODE}': {
      EN: 'Code: {CODE}',
      JP: 'コード: {CODE}',
      CH: '代码: {CODE}',
      TH: 'รหัส: {CODE}'
    },
    '요율 {PCT}%': {
      EN: 'Rate {PCT}%',
      JP: '料率 {PCT}%',
      CH: '费率 {PCT}%',
      TH: 'อัตรา {PCT}%'
    },
    '한 행은 정산실행이 저장한 가맹점 정산 결과입니다. 검색은 정산실행과 동일하게 상단 한 줄(정산일 구간·빠른기간·검색구분·검색어). 「전체」는 해당 필드로 좁히지 않으며, 검색어가 있을 때만 전체 컬럼 OR 검색입니다.': {
      EN: 'Each row is a merchant settlement result saved by a settlement run. Search matches settlement run: one top row (settlement date range, quick range, field, keyword). “All” does not narrow that field; OR across columns applies only when a keyword is present.',
      JP: '各行は精算実行が保存した加盟店精算の結果です。検索は精算実行と同じく上段1行（精算日範囲・クイック期間・検索区分・検索語）。「全体」はその項目では絞りません。検索語があるときのみ全列OR検索です。',
      CH: '每行是一条结算执行保存的商户结算结果。搜索与结算执行相同：顶部一行（精算日区间、快捷区间、搜索字段、关键词）。「全部」不在该字段上收窄；仅在有搜索词时进行全列 OR。',
      TH: 'แต่ละแถวคือผลชำระร้านที่รันชำระบันทึก การค้นหาเหมือนหน้ารันชำระ: แถวบน (ช่วงวันชำระ ช่วงด่วน ฟิลด์ คำสำคัญ) 「ทั้งหมด」ไม่คัดในฟิลด์นั้น OR ทุกคอลัมน์เมื่อมีคำค้น'
    },
    '금액·수수료(feeAmt)·담보금(holdAmt 등)·정산금액은 실행 시 저장값이며, 수수료(건)·부가세·건당·정산건당·기타(%)·보유율은 해당 실행의 집계 구간(정산대상기간·당일 누적 마감시각) 거래를 수수료내역과 동일한 건별 규칙으로 보조 계산합니다. 신규 실행부터 period가 저장되며, 그 이전 행은 정산일 하루 창으로 재조회합니다. 정산대상기간: RT는 거래번호·승인번호·마감(초) 한 줄, 그 외는 yyyy-MM-dd HH:mm:ss ~ 동일 형식(일 단위는 00:00:00~23:59:59, 분·시 격자는 구간 시각).': {
      EN: 'Amount, feeAmt, holdAmt, settlement amount are stored at run time. Per-txn fee, VAT, per-txn, per-run, extra %, and hold rate are auxiliary per-txn calculations (same rules as fee history) over the run’s aggregation window (target period and same-day cutoff). New runs persist period; older rows re-query a one-day settlement window. Target period: RT shows one line (txn no, approval no, cutoff seconds); others use yyyy-MM-dd HH:mm:ss ranges (day 00:00:00–23:59:59; minute/hour grids use slot times).',
      JP: '金額・手数料(feeAmt)・担保金(holdAmt等)・精算金額は実行時の保存値です。件当手数料・付加税・件当・精算件当・その他(%)・保有率は、当該実行の集計区間（精算対象期間・当日累積締め時刻）の取引を手数料履歴と同じ件別ルールで補助計算します。新規実行からperiodが保存され、それ以前の行は精算日1日窓で再照会します。精算対象期間: RTは取引番号・承認番号・締め(秒)の1行、その他はyyyy-MM-dd HH:mm:ss～同形式（日は00:00:00～23:59:59、分・時格子は区間時刻）。',
      CH: '金额、feeAmt、holdAmt、结算金额为执行时保存值。按笔手续费、增值税、按笔、按执行、其他%、留存率等，对执行汇总区间（结算目标期间与当日累计截止时间）的交易按与手续费明细相同的逐笔规则辅助计算。新执行起保存 period；更早的行按结算日单日窗口重查。目标期间：RT 为一行（交易号、批准号、截止秒）；其余为 yyyy-MM-dd HH:mm:ss 区间（自然日 00:00:00–23:59:59；分/时格为区间时刻）。',
      TH: 'ยอดเงิน feeAmt holdAmt ยอดชำระ เป็นค่าที่บันทึกตอนรัน ค่าธรรมเนียมต่อรายการ VAT ต่อรายการ ต่อรัน % อื่น อัตราพัก คำนวณเสริมต่อรายการเหมือนประวัติค่าธรรมเนียมในช่วงสรุปของรัน รันใหม่เก็บ period แถวเก่าใช้หน้าต่างวันชำระวันเดียว ช่วงเป้าหมาย: RT หนึ่งบรรทัด อื่นๆ เป็น yyyy-MM-dd HH:mm:ss'
    },
    '수수료(건)·보류율·건당수수료·정산건당·기타(%)수수료 열은 보조 참고용 분해이며, 글자색이 연한 회색으로 표시됩니다. 수수료(%), 수수료(금액), 수수료(부가세), 보류금액, 금액, 정산금액 등은 실행 저장값 기준입니다.': {
      EN: 'Fee (count), hold rate, per-txn fee, per-run fee, extra % fee columns are auxiliary breakdown (muted gray). Fee %, fee amount, fee VAT, hold amount, amount, settlement amount follow stored run values.',
      JP: '手数料(件)・保留率・件当手数料・精算件当・その他(%)手数料列は補助参考用の内訳で、文字色は薄いグレーです。手数料(%)、手数料(金額)、手数料(付加税)、保留金額、金額、精算金額などは実行保存値が基準です。',
      CH: '手续费（笔数）、留存率、按笔、按执行、其他% 手续费列为辅助分解（浅灰字）。手续费%、金额、增值税、留存金额、金额、结算金额等以执行保存值为准。',
      TH: 'คอลัมน์ค่าธรรมเนียม(จำนวนรายการ) อัตราพัก ต่อรายการ ต่อรัน % อื่น เป็นการแตกย่อยอ้างอิง (ตัวอักษรสีเทาอ่อน) % ยอด VAT ยอดพัก ยอดเงิน ชำระ ตามค่าที่บันทึกในรัน'
    },
    '본사·총판·지사·대리점·영업점 등 유통 구간 수익·수수료 분배는 유통망정산내역에서 동일 정산 실행분을 조직 단위로 집계합니다.': {
      EN: 'HQ, master, branch, agency, and sales-office revenue and fee splits for the same settlement run are aggregated by organization on the channel settlement list.',
      JP: '本社・総販・支社・代理店・営業店など流通区間の収益・手数料配分は、流通網精算一覧で同一精算実行分を組織単位に集計します。',
      CH: '总部、总代、分公司、代理、营业点等渠道区间的收益与手续费分成，在渠道结算明细中按组织对同一结算执行汇总。',
      TH: 'รายได้และส่วนแบ่งค่าธรรมเนียมตามช่วงช่องทาง (สำนักงานใหญ่ ตัวแทน ฯลฯ) สรุปตามองค์กรในหน้าชำระช่องทางสำหรับรันเดียวกัน'
    },
    '유통망 정산: 로그인 소속 조직·그 하위 가맹만 조회됩니다(가맹점정산내역과 동일한 범위). 가맹점 단위 행은 없으며, 하위 가맹 정산액이 조직 행에 합산됩니다. 총본사(HEADQUARTERS) 단계 행도 포함됩니다.': {
      EN: 'Channel settlement: only your logged-in org and its subordinate merchants are shown (same scope as merchant settlement). There are no per-merchant rows; sub-merchant settlement amounts roll up into org rows. Rows for the root HQ (HEADQUARTERS) tier are included.',
      JP: '流通網精算: ログイン所属組織およびその傘下の加盟店のみ照会されます（加盟店精算一覧と同じ範囲）。加盟店単位の行はなく、傘下加盟店の精算額は組織行に集約されます。総本部(HEADQUARTERS)段階の行も含みます。',
      CH: '渠道结算：仅显示登录所属组织及其下级商户（与商户结算明细范围相同）。无商户逐行；下级商户结算额汇总到组织行。包含总总部(HEADQUARTERS)层级行。',
      TH: 'ชำระช่องทาง: แสดงเฉพาะองค์กรที่ล็อกอินและร้านในลำดับชั้น (ขอบเขตเดียวกับรายการชำระร้านค้า) ไม่มีแถวต่อร้าน ยอดชำระของร้านล่างรวมในแถวองค์กร รวมระดับสำนักงานใหญ่สุด (HEADQUARTERS)'
    },
    '각 조직 행의 승인·취소 수수료 합계는 해당 행 조직 단계에 대응하는 구간만 합산합니다(예: 본사·총본사에만 비율이 있으면 총판 행 수수료는 0에 가깝게 나옵니다). 업체구분을 선택하면 해당 단계만 한 행으로 보입니다.': {
      EN: 'Per-row approve/cancel fee totals sum only the tier that matches that row’s org level (e.g. if only HQ tiers have rates, master-distributor rows show fees near zero). Choosing org type collapses the view to that tier in one row.',
      JP: '各組織行の承認・取消手数料の合計は、当該行の組織段階に対応する区間のみを集計します（例: 本社・総本部のみ率がある場合、総販行の手数料はほぼ0になります）。組織区分を選ぶと、その段階のみを1行で表示します。',
      CH: '每行组织对应的批准/取消手续费合计，仅汇总与该组织层级匹配的区间（例如仅总部层级有费率时，总代行手续费接近 0）。选择组织类型后，仅以该层级一行展示。',
      TH: 'ผลรวมค่าธรรมเนียมอนุมัติ/ยกเลิกต่อแถว รวมเฉพาะช่วงที่ตรงกับระดับองค์กรของแถวนั้น (เช่น มีเรทเฉพาะสำนักงานใหญ่ แถวตัวแทนหลักจะใกล้ 0) เลือกประเภทองค์กรแล้วเห็นเฉพาะระดับนั้นในแถวเดียว'
    },
    '「포함거래건」은 정산 실행에 저장된 집계 구간 결제 건수(tb_settlement_run.included_txn_cnt)의 합입니다. 구버전(null) 실행은 건수 1로 보정합니다. 「취소발생실행」은 해당 실행에 취소 합계 금액이 0보다 큰 정산 실행 개수입니다(결제 건수와 다릅니다).': {
      EN: '“Included txns” is the sum of saved per-run included payment counts (tb_settlement_run.included_txn_cnt). Legacy null runs are treated as count 1. “Runs with cancel” is how many settlement runs in that row have cancel totals > 0 (not the same as payment count).',
      JP: '「組込取引件数」は精算実行に保存された集計区間の決済件数(tb_settlement_run.included_txn_cnt)の合計です。旧版(null)実行は件数1に補正します。「取消発生実行」は、当該実行で取消合計金額が0より大きい精算実行の件数です（決済件数とは異なります）。',
      CH: '「含入交易笔数」为各结算执行保存的汇总区间内支付笔数 (tb_settlement_run.included_txn_cnt) 之和。旧版 null 执行按 1 笔修正。「含取发生运行」指该汇总下取消合计金额大于 0 的结算执行个数（与支付笔数不同）。',
      TH: '「จำนวนธุรกรรมรวม」คือผลรวม included_txn_cnt ที่บันทึกต่อรันชำระ รันเก่า null นับเป็น 1 「รันที่มียอดยกเลิก」คือจำนวนรันที่ยอดรวมยกเลิกมากกว่า 0 (ไม่ใช่จำนวนธุรกรรม)'
    },
    '동일 조직·정산일이라도 정산서 통화(열 통화)가 다르면 행이 나뉩니다. 승인수수료%·합계는 그 행의 유통 단계 분배액만으로 승인·취소 금액에 비례 배분한 값입니다(가맹 전체 PG 수수료 비율과 다를 수 있음).': {
      EN: 'Same org and settlement day can still split across rows when statement currency (column currency) differs. Approval fee % and totals are prorated from that row’s channel-tier share of approve/cancel amounts only (they may differ from the merchant-wide PG fee rate).',
      JP: '同一組織・精算日でも、精算書通貨（列の通貨）が異なると行が分かれます。承認手数料%・合計は、その行の流通段階の配分額のみで承認・取消金額に比例配分した値です（加盟店全体のPG手数料率と異なる場合があります）。',
      CH: '同一组织与结算日也可能因对账单货币（列货币）不同而拆行。批准手续费%与合计仅按该行流通层级在批准/取消金额中的分摊比例计算（可能与全店 PG 费率不同）。',
      TH: 'องค์กรและวันชำระเดียวกันอาจแยกแถวถ้าสกุลเงินในใบชำระต่างกัน % และรวมค่าธรรมเนียมอนุมัติคิดจากส่วนแบ่งระดับช่องทางของแถวนั้นเท่านั้น (อาจต่างจากเรท PG ทั้งร้าน)'
    },
    '조회기준·승인일자는 추후 거래일 기준 필터와 연동 예정이며, 현재는 정산일(calc_dt) 기준입니다. 가맹 지급액·유통 수수료 분배는 가맹점별 배분 설정(tb_distribution_fee_config) 비율을 바탕으로 합니다. 가맹점별 실행 한 줄은 가맹점정산내역에서 확인할 수 있습니다.': {
      EN: 'Search basis / approval date will later tie to a trade-date filter; today everything is by settlement date (calc_dt). Merchant payout and channel fee splits use per-merchant distribution settings (tb_distribution_fee_config). One line per merchant run is on the merchant settlement list.',
      JP: '照会基準・承認日は将来、取引日基準フィルタと連携予定であり、現在は精算日(calc_dt)基準です。加盟店支払額・流通手数料配分は、加盟店別配分設定(tb_distribution_fee_config)の率に基づきます。加盟店別の実行1行は加盟店精算一覧で確認できます。',
      CH: '查询基准/批准日后续将与交易日筛选联动，目前按结算日 (calc_dt)。商户拨付与渠道手续费分成按商户级分配配置 (tb_distribution_fee_config)。每商户每次执行一行请在商户结算明细查看。',
      TH: 'เกณฑ์ค้นหา/วันอนุมัติ จะเชื่อมกรองตามวันทำรายการภายหลัง ตอนนี้ใช้วันชำระ (calc_dt) การจ่ายร้านและส่วนแบ่งค่าธรรมเนียมช่องทางตาม tb_distribution_fee_config แถวต่อรันต่อร้านดูที่รายการชำระร้านค้า'
    },
    '미수금 차감: 「미수금차감」은 해당 정산 실행에서 지급액에 반영된 미수금 회수액입니다. 「미수금처리」는 수동 가맹은 환수처리·처리중·완료(확정), 자동 가맹은 자동화중·완료로 표시됩니다. 수동/자동 전환은 본사설정 「환수/미수금설정」입니다.': {
      EN: 'Receivable deduction: “Receivable deduction” is the receivable recovery applied to payout on that run. “Receivable handling” shows recovery in progress / completed (confirmed) for manual merchants, or automation in progress / completed for AUTO. Manual vs auto is configured under HQ settings → Recovery / receivables.',
      JP: '未収差引:「未収差引」は当該精算実行の支払額に反映された未収の回収額です。「未収処理」は手動加盟店は回収処理・処理中・完了(確定)、自動加盟店は自動化中・完了で表示されます。手動/自動の切替は本社設定「回収/未収設定」です。',
      CH: '应收扣减：「应收扣减」为该结算执行在拨付中反映的应收回收额。「应收处理」对手动商户显示回收处理/处理中/完成(确认)，对自动商户显示自动化中/完成。手动/自动在总部设置「回款/应收」中切换。',
      TH: 'หักลูกหนี้: 「หักลูกหนี้」คือยอดกู้คืนที่สะท้อนในจ่ายของรันนั้น 「การจัดการลูกหนี้」ร้านมือ: กู้คืน/กำลังดำเนินการ/เสร็จ(ยืนยัน) ร้านอัตโนมัติ: กำลังอัตโนมัติ/เสร็จ สลับที่ตั้งค่า HQ'
    },
    '정산금액(열): 수수료·담보가 순매출을 초과하면 음수로 표시됩니다. 동액은 미수금관리에 자동 등록되며, AUTO 가맹은 차기 정산에서 자동 차감, MANUAL 가맹은 환수처리 후 차기 마감·정산에서 차감됩니다.': {
      EN: 'Settlement amount column: if fees and collateral exceed net sales, the value stays negative. The shortfall is auto-posted to receivables; AUTO merchants recover on the next settlement, MANUAL merchants after recovery processing at the next close.',
      JP: '精算金額(列): 手数料・担保が純売上を超えると負のまま表示されます。同額は未収管理に自動登録され、AUTO加盟店は次回精算で自動相殺、MANUAL加盟店は回収処理後に次回締め・精算で相殺されます。',
      CH: '结算金额列：若手续费与担保超过净销售额则保持负数。差额自动记入应收管理；AUTO 商户在下次结算自动扣减，MANUAL 商户在回款处理后于下次截止/结算扣减。',
      TH: 'คอลัมน์ยอดชำระ: ถ้าค่าธรรมเนียมและหลักประกันเกินยอดสุทธิ คงติดลบ ลงลูกหนี้อัตโนมัติ AUTO หักรอบถัดไป MANUAL หลังกู้คืน'
    },
    '거래 집계 기반(정산 1회당 제외). 정산료는 별도 열.': {
      EN: 'Txn-aggregate basis (excludes per-run settlement fee). Batch fee is a separate column.',
      JP: '取引集計ベース（精算1回当たりの手数料は除く）。精算手数料は別列です。',
      CH: '按交易汇总（不含每次结算手续费）。结算手续费单独列。',
      TH: 'จากยอดรวมธุรกรรม (ไม่รวมค่าธรรมเนียมต่อรัน) ค่าธรรมเนียมรายบรรจุคอลัมน์แยก'
    },
    '해당 실행의 집계 구간 요약(JP·TH 두 줄).': {
      EN: 'Summary of this run’s aggregation window (two lines for JP/TH).',
      JP: '当該実行の集計区間の要約（JP・THは2行）。',
      CH: '该执行汇总区间的摘要（日泰两行）。',
      TH: 'สรุปช่วงสรุปของรันนี้ (JP/TH สองบรรทัด)'
    },
    '해당 정산 실행 지급액에서 미수금으로 차감된 금액': {
      EN: 'Amount deducted as receivable from this run’s payout.',
      JP: '当該精算実行の支払額から未収として差し引かれた金額。',
      CH: '从该次结算拨付中作为应收扣减的金额。',
      TH: 'ยอดที่หักเป็นลูกหนี้จากจ่ายของรันนี้'
    },
    '차감 발생 시 완료·처리중·자동화중; 지급부족만 등록된 행은 차기정산자동 또는 환수처리·차기마감': {
      EN: 'After deduction: completed / in progress / automation in progress; rows with only payout shortfall show next-settlement-auto or recovery / next close.',
      JP: '差引発生時は完了・処理中・自動化中。支払不足のみ登録された行は次回精算自動または回収処理・次回締め。',
      CH: '发生扣减后：完成/处理中/自动化中；仅登记拨付不足的行显示下次结算自动或回款/下次截止。',
      TH: 'เมื่อมีการหัก: เสร็จ/กำลังดำเนินการ/อัตโนมัติ แถวที่ลงเฉพาะขาดจ่าย: รอบถัดไปอัตโนมัติหรือกู้คืน'
    },
    '실행 저장 지급액; 수수료·담보 초과 시 음수·미수금 자동등록': {
      EN: 'Payout stored on the run; if fees and collateral exceed sales, negative with auto receivable.',
      JP: '実行に保存された支払額。手数料・担保が売上を超えると負の値・未収自動登録。',
      CH: '执行保存的拨付额；手续费与担保超过销售额时为负数并自动登记应收。',
      TH: 'ยอดจ่ายที่บันทึกในรัน ค่าธรรมเนียมและหลักประกันเกินยอดขาย ติดลบและลงลูกหนี้อัตโนมัติ'
    },
    '담보금(롤링): 결제(승인) 건별로 정산 실행 시 설정된 비율(%)만큼 예치되며, 보류 영업일(주말 제외·공휴일 미반영) 후 해지일에 정산 실행하면 지급액에 합산됩니다.': {
      EN: 'Rolling collateral: per approved payment, a settlement run withholds the configured %; after the hold business days (weekends excluded; holidays not applied), a run on/after the release date adds it to payout.',
      JP: '担保金（ローリング）: 決済（承認）ごとに精算実行で設定した率(%)を預かり、保留営業日（土日除く・祝日は未反映）経過後の解放日に精算実行すると支払額に合算されます。',
      CH: '滚动保证金：每笔批准支付在结算执行时按设定比例(%)暂扣；经过保留营业日（不含周末·不含节假日）后，在解放日及之后的结算执行并入拨付额。',
      TH: 'หลักประกัน(โรลลิง): ต่อรายการอนุมัติ หักตาม % ที่ตั้งตอนรันชำระ หลังวันทำการพัก (ไม่นับสุดสัปดาห์·ไม่นับวันหยุด) เมื่อรันชำระในวันปลดหรือหลัง รวมในยอดจ่าย'
    },
    '비율·보류 일수: 본사설정 수수료정책의 롤링(담보금) 또는 가맹점 정산설정에서 「보류율 본사정책 따름=N」일 때 개별 보류율·일수를 사용합니다.': {
      EN: 'Rate / hold days: when merchant settlement has “follow HQ hold policy = N”, use per-merchant hold rate and days from HQ fee policy rolling (collateral) or merchant settlement settings.',
      JP: '率・保留日数: 本社設定の手数料政策のローリング（担保金）、または加盟店精算設定で「保留率本社政策従う=N」のとき、個別の保留率・日数を使用します。',
      CH: '比例与保留天数：当商户结算设置「留存率跟随总部政策=N」时，使用总部手续费政策中的滚动（保证金）或商户结算里的个别留存率与天数。',
      TH: 'อัตรา/วันพัก: เมื่อตั้งค่าชำระร้านค้า「อัตราพักตาม HQ=N」 ใช้อัตราและวันพักรายร้านจากนโยบายค่าธรรมเนียม HQ หรือการตั้งค่าชำระ'
    },
    '해제일시·남은일자는 영업일 기준입니다. 루트는 해당 거래의 결제 루트(route_no)입니다.': {
      EN: 'Release time and remaining days are business-day based. Route is the payment route_no for that transaction.',
      JP: '解放日時・残日数は営業日基準です。ルートは当該取引の決済ルート(route_no)です。',
      CH: '解除日时间与剩余天数按营业日计。路由为该笔支付的 route_no。',
      TH: 'เวลาปลดและวันที่เหลือคิดตามวันทำการ Route คือ route_no ของรายการนั้น'
    },
    '보류: 영업일 기준 해제 예정일 00:00 표기. 해지: 정산 반영 처리 시각.': {
      EN: 'On hold: shows planned release date at 00:00 (business days). Released: timestamp when settlement reflection was processed.',
      JP: '保留: 営業日基準の解放予定日を00:00表示。解放: 精算反映を処理した日時。',
      CH: '暂扣：按营业日显示预计解除日 00:00。已释放：结算反映处理时刻。',
      TH: 'พัก: แสดงวันปลดโดยประมาณ 00:00 ตามวันทำการ ปลดแล้ว: เวลาที่ประมวลผลสะท้อนชำระ'
    },
    '오늘부터 해제 예정일까지 남은 영업일(주말 제외).': {
      EN: 'Business days from today until the planned release date (weekends excluded).',
      JP: '本日から解放予定日までの残り営業日（土日除く）。',
      CH: '从今天到预计解除日的剩余营业日（不含周末）。',
      TH: 'วันทำการเหลือจากวันนี้ถึงวันปลดโดยประมาณ (ไม่นับสุดสัปดาห์)'
    },
    보류: {
      EN: 'On hold',
      JP: '保留',
      CH: '暂扣',
      TH: 'พักอยู่'
    },
    '해지(정산반영)': {
      EN: 'Released (settled)',
      JP: '解放（精算反映）',
      CH: '已释放（已结算反映）',
      TH: 'ปลดแล้ว (สะท้อนชำระ)'
    },
    '해지일({DATE}) 이후 정산 실행 시 지급액에 합산': {
      EN: 'After release date {DATE}, added to payout on settlement run.',
      JP: '解放日{DATE}以降の精算実行で支払額に合算。',
      CH: '解放日{DATE}之后的结算执行并入拨付额。',
      TH: 'หลังวันปลด {DATE} รวมในยอดจ่ายเมื่อรันชำระ'
    },
    '정산 실행 시 지급액에 반영됨': {
      EN: 'Reflected in payout on settlement run.',
      JP: '精算実行時に支払額へ反映済み。',
      CH: '已在结算执行时计入拨付额。',
      TH: 'สะท้อนในยอดจ่ายเมื่อรันชำระแล้ว'
    },
    환수대기: {
      EN: 'Pending recovery',
      JP: '回収待ち',
      CH: '待回款',
      TH: 'รอกู้คืน'
    },
    부분환수: {
      EN: 'Partially recovered',
      JP: '一部回収',
      CH: '部分已回款',
      TH: 'กู้คืนบางส่วน'
    },
    환수완료: {
      EN: 'Fully recovered',
      JP: '回収完了',
      CH: '已回款完成',
      TH: 'กู้คืนครบแล้ว'
    },
    '정산후환불(자동)': {
      EN: 'Post-settlement refund (auto)',
      JP: '精算後返金（自動）',
      CH: '结算后退款（自动）',
      TH: 'คืนหลังชำระ (อัตโนมัติ)'
    },
    '포함(환수금)': {
      EN: 'Included (recovery)',
      JP: '含む（回収金）',
      CH: '含（回款）',
      TH: 'รวม (กู้คืน)'
    },
    '제외(환수금)': {
      EN: 'Excluded (recovery)',
      JP: '除く（回収金）',
      CH: '不含（回款）',
      TH: 'ไม่รวม (กู้คืน)'
    },
    'VAT적용(환수금)': {
      EN: 'VAT applies',
      JP: 'VAT 適用',
      CH: '适用 VAT',
      TH: 'ใช้ VAT'
    },
    'VAT미적용(환수금)': {
      EN: 'VAT not applied',
      JP: 'VAT 未適用',
      CH: '不适用 VAT',
      TH: 'ไม่ใช้ VAT'
    },
    '정산 반영 후 후속 상태({ST})': {
      EN: 'After settlement reflection, follow-up status ({ST})',
      JP: '精算反映後の後続状態（{ST}）',
      CH: '结算反映后的后续状态（{ST}）',
      TH: 'หลังสะท้อนชำระ สถานะถัดไป ({ST})'
    },
    '「환수금」은 정산이 반영된 뒤(승인 건이 settled 등으로 정산에 올라간 이후) 같은 거래가 환불·취소·무효·차지백 등으로 바뀔 때 정산에서 거둬야 할 금액이 자동으로 잡히는 내역입니다. 금액은 전산설정(환수금 수수료 포함) 및 수수료내역과 동일한 건별 산식입니다. 다음 정산 지급액에서는 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)을 차감합니다. 거래별 산출·검증은 「회수·거래기준」(/settlement/recallMng) 화면을 참고하세요.': {
      EN: 'Recovery rows are created when a transaction was already reflected in settlement (e.g. settled) and later changes to refund, cancel, void, chargeback, etc. Amounts follow ledger settings (whether fees are included in recovery) and the same per-txn rules as fee history. On the next payout, recovery (FIFO) is deducted first, then receivables (FIFO). For per-txn calculation and checks, use the “Recovery by transaction” screen (/settlement/recallMng).',
      JP: '「回収金」は、精算反映後（承認取引が settled 等で精算に載った後）に同一取引が返金・取消・無効・チャージバック等へ変わったとき、精算で回収すべき金額が自動計上される明細です。金額は全算設定（回収金に手数料を含むか）および手数料一覧と同じ件別計算式です。次回の支払額からは回収金(FIFO)を先に差し引いた後、未収金(FIFO)を差し引きます。取引別の算出・照合は「回収・取引基準」(/settlement/recallMng) を参照してください。',
      CH: '「回款」指：交易已参与结算（如 settled）之后又变为退款、取消、作废、拒付等时，系统自动生成的应从结算侧收回的金额。金额按账务设置（回款是否含手续费）及与手续费明细相同的逐笔规则计算。下次拨付时先按 FIFO 扣回款，再扣应收。逐笔计算与核对请使用「回款·按交易」(/settlement/recallMng) 画面。',
      TH: 'รายการกู้คืนเกิดเมื่อธุรกรรมถูกสะท้อนชำระแล้ว (เช่น settled) ต่อมาเปลี่ยนเป็นคืนเงิน·ยกเลิก·โมฆะ·ชาร์จแบ็ก ฯลฯ ยอดคิดตามการตั้งค่า (รวมค่าธรรมเนียมหรือไม่) และกฎรายรายการเดียวกับประวัติค่าธรรมเนียม รอบถัดไปหักกู้คืน FIFO แล้วจึงหักลูกหนี้ FIFO ตรวจรายรายการที่ /settlement/recallMng'
    },
    '정산대기: HOLD — 가맹 정산내역에 안 나감. 처음 열 때 정산기간이 비어 있으면 최근 1년입니다. 해제·배포는 「정산보류내역」 등 운영 절차. 노출 주기 요약은 정산배포와 같습니다.': {
      EN: 'Settlement pending: HOLD — not posted to merchant settlement statements. When opened with an empty settlement period, the default is the last year. Release and distribution follow operational procedures such as Settlement hold list. Cadence display summary matches Settlement distribution.',
      JP: '精算待ち: HOLD — 加盟店精算一覧には載りません。精算期間が空のまま開いたときは直近1年が既定です。解除・配布は「精算保留一覧」などの運用手順に従ってください。表示サイクル要約は精算配布と同じです。',
      CH: '结算待处理：HOLD — 不会出现在商户结算明细中。首次打开若精算期间为空，默认为最近一年。解除与下发请按「结算暂缓明细」等运营流程。展示周期说明与结算下发一致。',
      TH: 'รอชำระ: HOLD — ไม่ขึ้นรายการชำระร้านค้า เมื่อเปิดโดยช่วงชำระว่าง ค่าเริ่มต้นคือ 1 ปีล่าสุด การปลดและแจกจ่ายตามขั้นตอนเช่น รายการพักชำระ สรุปรอบแสดงเหมือนหน้าแจกจ่ายผลชำระ'
    },
    '미수금 등록·차감': {
      EN: 'Register / deduct receivable',
      JP: '未収金の登録・控除',
      CH: '登记/扣减应收',
      TH: 'ลงทะเบียน/หักลูกหนี้'
    },
    '미수금등록: 안내 본문': {
      EN: 'Search and select a merchant, or double-click a grid row to open with that merchant. **Add** creates a new receivable; **Deduct** reduces open receivables FIFO for that merchant. Enter amount and optional memo.',
      JP: '加盟店を検索して選択するか、一覧行をダブルクリックするとその店舗を選んだ状態で開きます。**追加**は新規未収、**控除**は当該店の残高未収を登録順（FIFO）で減らします。金額と任意のメモを入力してください。',
      CH: '搜索并选择商户，或双击表格行以该商户打开。**增加**为新增应收；**扣减**按登记顺序（FIFO）减少该商户未收余额。填写金额与可选备注。',
      TH: 'ค้นหาและเลือกร้าน หรือดับเบิลคลิกแถวตารางเพื่อเปิดพร้อมร้านนั้น **เพิ่ม** = ลูกหนี้ใหม่ **หัก** = ลดยอดคงค้าง FIFO ของร้าน กรอกจำนวนและเมโม (ถ้ามี)'
    },
    '미수금등록: 선택된 가맹': {
      EN: 'Selected merchant',
      JP: '選択した加盟店',
      CH: '已选商户',
      TH: 'ร้านที่เลือก'
    },
    '미수금등록: 가맹 미선택': {
      EN: 'None — search below and pick a merchant.',
      JP: '未選択 — 下で検索して加盟店を選んでください。',
      CH: '未选择 — 请在下方搜索并选择商户。',
      TH: 'ยังไม่เลือก — ค้นหาด้านล่างแล้วเลือกร้าน'
    },
    '미수금등록: 업체 검색 라벨': {
      EN: 'Merchant code / name',
      JP: '加盟店コード・店名',
      CH: '商户代码/名称',
      TH: 'รหัส/ชื่อร้าน'
    },
    '미수금등록: 검색 placeholder': {
      EN: 'Part of code or name',
      JP: 'コードまたは店名の一部',
      CH: '代码或名称片段',
      TH: 'บางส่วนของรหัสหรือชื่อ'
    },
    '미수금등록: 추가': {
      EN: 'Add (+)',
      JP: '追加（+）',
      CH: '增加（+）',
      TH: 'เพิ่ม (+)'
    },
    '미수금등록: 차감': {
      EN: 'Deduct (−)',
      JP: '控除（−）',
      CH: '扣减（−）',
      TH: 'หัก (−)'
    },
    '미수금등록: 금액 placeholder': {
      EN: 'e.g. 10000',
      JP: '例: 10000',
      CH: '例：10000',
      TH: 'เช่น 10000'
    },
    '미수금등록: 메모 라벨': {
      EN: 'Memo (optional)',
      JP: 'メモ（任意）',
      CH: '备注（可选）',
      TH: 'หมายเหตุ (ไม่บังคับ)'
    },
    '미수금등록: 메모 placeholder': {
      EN: 'Reason when deducting, etc.',
      JP: '控除時の理由など',
      CH: '扣减原因等',
      TH: 'เหตุผลตอนหัก ฯลฯ'
    },
    '미수금등록: 처리': {
      EN: 'Submit',
      JP: '実行',
      CH: '提交',
      TH: 'ดำเนินการ'
    },
    '환수처리': {
      EN: 'Request recovery',
      JP: '回収処理',
      CH: '回款处理',
      TH: 'ดำเนินการกู้คืน'
    },
    '요청됨': {
      EN: 'Requested',
      JP: '依頼済み',
      CH: '已请求',
      TH: 'ส่งคำขอแล้ว'
    },
    '미요청': {
      EN: 'Not requested',
      JP: '未依頼',
      CH: '未请求',
      TH: 'ยังไม่ขอ'
    },
    '자동화중': {
      EN: 'Automating',
      JP: '自動処理中',
      CH: '自动处理中',
      TH: 'กำลังประมวลอัตโนมัติ'
    },
    '미수금상태:대기': {
      EN: 'Open',
      JP: '未完了',
      CH: '未结',
      TH: 'ค้าง'
    },
    '미수금상태:부분': {
      EN: 'Partial',
      JP: '一部',
      CH: '部分',
      TH: 'บางส่วน'
    },
    '미수금상태:종료': {
      EN: 'Closed',
      JP: '完了',
      CH: '已关闭',
      TH: 'ปิดแล้ว'
    },
    '미수금상태:취소': {
      EN: 'Cancelled',
      JP: '取消',
      CH: '已取消',
      TH: 'ยกเลิก'
    },
    '검색어를 입력하세요.': {
      EN: 'Enter a search keyword.',
      JP: '検索語を入力してください。',
      CH: '请输入搜索词。',
      TH: 'กรุณากรอกคำค้น'
    },
    '가맹점을 선택하세요.': {
      EN: 'Select a merchant.',
      JP: '加盟店を選択してください。',
      CH: '请选择商户。',
      TH: 'เลือกร้านค้า'
    },
    '금액은 0보다 큰 숫자로 입력하세요.': {
      EN: 'Enter an amount greater than zero.',
      JP: '0より大きい金額を入力してください。',
      CH: '请输入大于 0 的金额。',
      TH: 'กรอกจำนวนเงินมากกว่า 0'
    },
    '요청 실패': {
      EN: 'Request failed',
      JP: 'リクエストに失敗しました',
      CH: '请求失败',
      TH: 'คำขอล้มเหลว'
    },
    '처리 실패': {
      EN: 'Operation failed',
      JP: '処理に失敗しました',
      CH: '处理失败',
      TH: 'ดำเนินการล้มเหลว'
    },
    '총액': {
      EN: 'Total amount',
      JP: '総額',
      CH: '总额',
      TH: 'ยอดรวม'
    },
    '잔액합계': {
      EN: 'Balance total',
      JP: '残高合計',
      CH: '余额合计',
      TH: 'ยอดคงเหลือรวม'
    },
    '미수금합계': {
      EN: 'Receivable total',
      JP: '未収金合計',
      CH: '应收合计',
      TH: 'ยอดลูกหนี้รวม'
    },
    /** 미수금관리(/calc/unpaidMng) 그리드 열 deductCnt — 잔여 미수금액 */
    '잔여': {
      EN: 'Remaining',
      JP: '残額',
      CH: '剩余',
      TH: 'คงเหลือ'
    },
    '다음 정산 마감 시 이 미수금을 지급액에서 차감하도록 요청할까요?': {
      EN: 'Request to deduct this receivable from payout on the next settlement close?',
      JP: '次回精算締め時に、この未収金を支払額から控除するよう依頼しますか？',
      CH: '是否在下次结算关账时从拨付额中扣减该笔应收？',
      TH: 'ต้องการขอหักลูกหนี้รายนี้จากยอดจ่ายเมื่อปิดรอบชำระถัดไปหรือไม่?'
    },
    '검색 결과가 없습니다.': {
      EN: 'No results.',
      JP: '該当がありません。',
      CH: '没有结果。',
      TH: 'ไม่พบผลลัพธ์'
    },
    '미수금 등록 UI를 불러올 수 없습니다.': {
      EN: 'Could not load the receivable entry UI.',
      JP: '未収金登録画面を読み込めません。',
      CH: '无法加载应收登记界面。',
      TH: 'โหลด UI ลงทะเบียนลูกหนี้ไม่ได้'
    },
    '정산 실행 ID(settlementRunId)를 찾을 수 없습니다.': {
      EN: 'Could not find settlement run ID (settlementRunId).',
      JP: '精算実行ID（settlementRunId）が見つかりません。',
      CH: '找不到结算执行 ID（settlementRunId）。',
      TH: 'ไม่พบ settlementRunId'
    },
    '정산 확정 리포트': {
      EN: 'Confirmed settlement report',
      JP: '確定精算レポート',
      CH: '已确认结算报表',
      TH: 'รายงานชำระที่ยืนยันแล้ว'
    },
    '정산리포트 RST: 거래 구간 안내': {
      EN: 'Range totals follow notify txn states (approved, cancelled, refunded, etc.); settlement and payout amounts are values stored on the run.',
      JP: '区間集計はノティ取引状態（承認・取消・返金等）基準です。精算金・支払額は実行時の保存値です。',
      CH: '区间汇总按通知交易状态（批准、取消、退款等）；结算款与拨付额为执行时保存值。',
      TH: 'ช่วงรวมตามสถานะแจ้ง (อนุมัติ ยกเลิก คืนเงิน ฯลฯ) ยอดชำระ/จ่ายเป็นค่าที่บันทึกตอนรัน'
    },
    '정산리포트 RST: 가맹 전달 요약 안내': {
      EN: 'Merchant-facing summary. For fee breakdown and collateral release timing, use Fee history and Collateral screens.',
      JP: '加盟店向け要約です。手数料内訳・担保解放予定は手数料履歴・担保金画面で確認してください。',
      CH: '面向商户的摘要。手续费明细与保证金解除时间请在手续费与保证金画面查看。',
      TH: 'สรุปส่งร้าน รายละเอียดค่าธรรมเนียมและกำหนดปลดหลักประกัน ดูที่หน้าประวัติค่าธรรมเนียมและหลักประกัน'
    },
    '불러오는 중…': {
      EN: 'Loading…',
      JP: '読み込み中…',
      CH: '加载中…',
      TH: 'กำลังโหลด…'
    },
    ' · 정산주기 ': {
      EN: ' · Settlement cycle ',
      JP: ' · 精算サイクル ',
      CH: ' · 结算周期 ',
      TH: ' · รอบชำระ '
    },
    '정산일시': {
      EN: 'Settlement date/time',
      JP: '精算日時',
      CH: '结算日期时间',
      TH: 'วันเวลาชำระบัญชี'
    },
    '집계 구간 거래(건수·금액)': {
      EN: 'Range transactions (count · amount)',
      JP: '集計区間の取引（件数・金額）',
      CH: '区间交易（笔数·金额）',
      TH: 'ธุรกรรมในช่วง (จำนวน·ยอด)'
    },
    '정산리포트 집계:승인매출': {
      EN: 'Approved (sales)',
      JP: '承認（売上）',
      CH: '批准（销售额）',
      TH: 'อนุมัติ (ยอดขาย)'
    },
    '정산리포트 집계:취소': {
      EN: 'Cancellations',
      JP: '取消',
      CH: '取消',
      TH: 'ยกเลิก'
    },
    '정산리포트 집계:환불': {
      EN: 'Refunds',
      JP: '返金',
      CH: '退款',
      TH: 'คืนเงิน'
    },
    '정산리포트 집계:기타': {
      EN: 'Other',
      JP: 'その他',
      CH: '其他',
      TH: 'อื่นๆ'
    },
    '거래총건수': {
      EN: 'Total txn count',
      JP: '取引総件数',
      CH: '交易总笔数',
      TH: 'จำนวนธุรกรรมทั้งหมด'
    },
    '정산 실행 확정값': {
      EN: 'Confirmed run totals',
      JP: '精算実行の確定値',
      CH: '结算执行确认值',
      TH: 'ยอดรันที่ยืนยันแล้ว'
    },
    '승인(매출)합': {
      EN: 'Approved (sales) total',
      JP: '承認（売上）合計',
      CH: '批准（销售）合计',
      TH: 'รวมอนุมัติ (ยอดขาย)'
    },
    '취소합': {
      EN: 'Cancellation total',
      JP: '取消合計',
      CH: '取消合计',
      TH: 'รวมยกเลิก'
    },
    '상세 조회 실패': {
      EN: 'Failed to load details',
      JP: '詳細の取得に失敗しました',
      CH: '加载详情失败',
      TH: 'โหลดรายละเอียดล้มเหลว'
    },
    '수동(환수처리 후 차감)': {
      EN: 'Manual (deduct after recovery)',
      JP: '手動（回収処理後に控除）',
      CH: '手动（回款处理后扣减）',
      TH: 'ด้วยมือ (หักหลังกู้คืน)'
    },
    '자동(차기정산 FIFO)': {
      EN: 'Auto (FIFO on next settlement)',
      JP: '自動（次回精算でFIFO控除）',
      CH: '自动（下次结算 FIFO 扣减）',
      TH: 'อัตโนมัติ (FIFO รอบถัดไป)'
    },
    '본사합산(가맹 혼합)': {
      EN: 'HQ aggregate (mixed merchants)',
      JP: '本社合算（加盟店混在）',
      CH: '本部汇总（多商户混合）',
      TH: 'รวม HQ (ร้านหลายราย)'
    },
    '건': {
      EN: ' txns',
      JP: '件',
      CH: '笔',
      TH: ' รายการ'
    },
    '예': {
      EN: 'Yes',
      JP: 'はい',
      CH: '是',
      TH: 'ใช่'
    },
    '아니오': {
      EN: 'No',
      JP: 'いいえ',
      CH: '否',
      TH: 'ไม่'
    },
    '가맹점코드': {
      EN: 'Merchant code',
      JP: '加盟店コード',
      CH: '商户代码',
      TH: 'รหัสร้านค้า'
    },
    '인쇄': {
      EN: 'Print',
      JP: '印刷',
      CH: '打印',
      TH: 'พิมพ์'
    },
    '본사 지급 리포트(총본사→본사)': {
      EN: 'HQ payout report (root HQ → regional HQ)',
      JP: '本社支払レポート（総本部→本社）',
      CH: '本部拨付报表（总总部→本部）',
      TH: 'รายงานจ่าย HQ (สำนักงานใหญ่สุด→ภูมิภาค)'
    },
    '가맹점 정산 리포트': {
      EN: 'Merchant settlement report',
      JP: '加盟店精算レポート',
      CH: '商户结算报表',
      TH: 'รายงานชำระร้านค้า'
    },
    '환불/취소': {
      EN: 'Refund / cancel',
      JP: '返金／取消',
      CH: '退款/取消',
      TH: 'คืนเงิน/ยกเลิก'
    },
    '순결제': {
      EN: 'Net payment',
      JP: '純決済',
      CH: '净支付',
      TH: 'ชำระสุทธิ'
    },
    '예치(10%)': {
      EN: 'Hold (10%)',
      JP: '預かり(10%)',
      CH: '暂扣(10%)',
      TH: 'เงินประกัน (10%)'
    },
    'Processing(5.6%)': {
      EN: 'Processing (5.6%)',
      JP: 'Processing(5.6%)',
      CH: 'Processing(5.6%)',
      TH: 'Processing (5.6%)'
    },
    '건당수수료합': {
      EN: 'Per-txn fee total',
      JP: '件当手数料合計',
      CH: '按笔手续费合计',
      TH: 'รวมค่าธรรมเนียมต่อรายการ'
    },
    '정산금(추정)': {
      EN: 'Settlement amount (est.)',
      JP: '精算金（見込）',
      CH: '结算款（估计）',
      TH: 'ยอดชำระ (ประมาณ)'
    },
    '지급예정일(+7영업일)': {
      EN: 'Payout due (same as settlement date)',
      JP: '支払予定日（精算日と同一）',
      CH: '预计拨付日（与结算日相同）',
      TH: 'กำหนดจ่าย (เท่ากับวันชำระ)'
    },
    '정산완료': {
      EN: 'Settlement done',
      JP: '精算完了',
      CH: '结算完成',
      TH: 'ชำระครบแล้ว'
    },
    '부분정산': {
      EN: 'Partial settlement',
      JP: '一部精算',
      CH: '部分结算',
      TH: 'ชำระบางส่วน'
    },
    '최근정산': {
      EN: 'Recent runs',
      JP: '直近精算',
      CH: '近期结算',
      TH: 'ชำระล่าสุด'
    },
    '수동실행': {
      EN: 'Manual run',
      JP: '手動実行',
      CH: '手动执行',
      TH: 'รันด้วยมือ'
    },
    '노출주기 안내': {
      EN: 'Cadence guide',
      JP: '表示周期ガイド',
      CH: '展示周期说明',
      TH: 'คำแนะนำรอบแสดงผล'
    },
    '확정여부': {
      EN: 'Confirmed',
      JP: '確定可否',
      CH: '确认状态',
      TH: 'สถานะการยืนยัน'
    },
    '정산대상기간': {
      EN: 'Settlement window',
      JP: '精算対象期間',
      CH: '结算对象期间',
      TH: 'ช่วงเวลาที่ชำระครอบคลุม'
    },
    '정산료': {
      EN: 'Settlement fee',
      JP: '精算手数料',
      CH: '结算手续费',
      TH: 'ค่าธรรมเนียมชำระบัญชี'
    },
    '배포상태': {
      EN: 'Publish status',
      JP: '配布状態',
      CH: '发布状态',
      TH: 'สถานะเผยแพร่'
    },
    '정산마감일': {
      EN: 'Settlement close date',
      JP: '精算締日',
      CH: '结算截止日',
      TH: 'วันปิดรอบชำระ'
    },
    '정산일자': {
      EN: 'Settlement date (batch)',
      JP: '精算日（バッチ）',
      CH: '结算日（批次）',
      TH: 'วันชำระ (แบทช์)'
    },
    '집계 구간 마감일.': {
      EN: 'Period close date (end of aggregation window).',
      JP: '集計区間の締日。',
      CH: '汇总区间截止日。',
      TH: 'วันปิดช่วงสรุป'
    },
    '정산주기·영업일 기준 배치 예정일.': {
      EN: 'Scheduled batch date by settlement cycle and business days.',
      JP: '精算サイクル・営業日基準のバッチ予定日。',
      CH: '按结算周期与营业日计算的批次预定日。',
      TH: 'วันแบทช์ตามรอบชำระและวันทำการ'
    },
    '집계 구간 마감일(예: W7이면 해당 주 마지막 날). 기존 calc_dt와 동일.': {
      EN: 'Period close date (e.g. W7 = last day of that week). Same as legacy calc_dt.',
      JP: '集計区間締日（例：W7なら当該週の最終日）。従来の calc_dt と同一。',
      CH: '汇总区间截止日（如 W7 为该周最后一天）。与原先 calc_dt 相同。',
      TH: 'วันปิดช่วงสรุป (เช่น W7 = วันสุดท้ายของสัปดาห์) เหมือน calc_dt เดิม'
    },
    '정산주기·영업일 기준 배치(정산) 예정일(W+N 영업일 등).': {
      EN: 'Scheduled settlement batch date by cycle and business days (e.g. W+N business days).',
      JP: '精算サイクル・営業日基準の精算バッチ予定日（W+N 営業日など）。',
      CH: '按结算周期与营业日的结算批次预定日（如 W+N 营业日等）。',
      TH: 'วันแบทช์ชำระตามรอบและวันทำการ (เช่น W+N วันทำการ)'
    },
    '정산주기·영업일 기준 배치(정산) 예정일(정산일자 열과 동일).': {
      EN: 'Scheduled settlement batch date by cycle and business days (same as Settlement date column).',
      JP: '精算サイクル・営業日基準の精算バッチ予定日（精算日列と同一）。',
      CH: '按结算周期与营业日的结算批次预定日（与「结算日」列相同）。',
      TH: 'วันแบทช์ชำระตามรอบและวันทำการ (ตรงคอลัมน์วันชำระ)'
    },
    '정산 실행 일시(표시 형식은 환경 설정).': {
      EN: 'Settlement run timestamp (display format follows environment settings).',
      JP: '精算実行日時（表示形式は環境設定）。',
      CH: '结算执行时间（显示格式随环境设置）。',
      TH: 'วันเวลารันชำระ (รูปแบบตามการตั้งค่าระบบ)'
    },
    '한 행은 정산 실행으로 저장된 귀사(가맹) 정산 결과입니다. 정산기간·빠른기간으로 조회한 뒤 [검색] 하세요.': {
      EN: 'Each row is a settlement run result saved for your merchant. Set the settlement period or quick range, then click [Search].',
      JP: '1行は精算実行として保存された貴社（加盟店）の精算結果です。精算期間・クイック期間を指定してから［検索］してください。',
      CH: '每行为您（商户）已保存的结算执行结果。请设定结算期间或快捷区间后点击【搜索】。',
      TH: 'แต่ละแถวคือผลชำระที่บันทึกจากรันชำระ ตั้งช่วงชำระหรือช่วงด่วนแล้วกด [ค้นหา]'
    },
    '정산대상기간·결제금액·수수료·보증금·정산료·VAT·지급액은 정산배포·정산실행과 동일한 실행 저장값·집계 규칙을 따릅니다. 수수료 열은 건당·결제%·취소·환불(무효 등) 구간을 합산한 거래수수료(tb_settlement_run.total_fee)입니다.': {
      EN: 'Target period, payment amount, fees, collateral, settlement fee, VAT, and payout follow the same stored run values and aggregation rules as settlement publish and runs. The fee column is total transaction fees (tb_settlement_run.total_fee) summing per-txn, pay %, cancel, refund (void, etc.) buckets.',
      JP: '精算対象期間・決済金額・手数料・担保金・精算料・VAT・支払額は、精算配布・精算実行と同じ実行保存値・集計ルールに従います。手数料列は件当・決済%・取消・返金（無効等）区間を合算した取引手数料（tb_settlement_run.total_fee）です。',
      CH: '结算目标期间、支付金额、手续费、保证金、结算费、VAT、拨付额与结算下发、结算执行采用相同的执行保存值与汇总规则。手续费列为按笔、支付%、取消、退款（作废等）区间汇总的交易手续费（tb_settlement_run.total_fee）。',
      TH: 'ช่วงเป้าหมาย ยอดชำระ ค่าธรรมเนียม หลักประกัน ค่าธรรมเนียมชำระ VAT และยอดจ่ายใช้กฎเดียวกับแจกจ่าย/รันชำระ คอลัมน์ค่าธรรมเนียมคือ total_fee รวมต่อรายการ % ยกเลิก คืน โมฆะ ฯลฯ'
    },
    '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 건당·취소·환불 등 세부 분해 열은 같은 거래 구간 합산 보조값입니다.': {
      EN: 'Toggle visible columns via VIEW SETTING next to Hello (saved per user). Per-txn, cancel, refund, etc. breakdown columns are auxiliary sums for the same transaction window.',
      JP: '表示列は［Hello］横の VIEW SETTING で調整できます（保存時ユーザー別に維持）。件当・取消・返金等の内訳列は同一取引区間の合算補助値です。',
      CH: '可通过 Hello 旁的 VIEW SETTING 调整显示列（按用户保存）。按笔、取消、退款等明细列为同一交易区间的汇总辅助值。',
      TH: 'สลับคอลัมน์ที่มองเห็นผ่าน VIEW SETTING ข้าง Hello (บันทึกต่อผู้ใช้) คอลัมน์แยกต่อรายการ ยกเลิก คืน ฯลฯ เป็นยอดรวมเสริมในช่วงธุรกรรมเดียวกัน'
    },
    '[하위 구분] 정산집계·정산실시·정산집계표·확정정산(리포트). 정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 하단에 해당 정산 실행에 포함된 거래 목록이 표시됩니다. 집계표(SUM)는 요약 1행만 조회되며, 본사 지급 리포트의 정산실시(EXE)는 합산 행이라 실행 ID가 없을 수 있습니다.': {
      EN: '[Sub-type] Aggregate, runs, summary sheet, confirmed (report). Click a row with a run ID in aggregate, runs, or confirmed to show included transactions below. SUM is one summary row; HQ payout report → Runs (EXE) may be an aggregate row without a run ID.',
      JP: '[下位区分] 集計・実行・集計表・確定精算（レポート）。集計・実行・確定精算で実行IDがある行をクリックすると、下部に当該精算実行に含まれる取引一覧が表示されます。集計表(SUM)は要約1行のみで、本社支払レポートの実行(EXE)は合算行のため実行IDがない場合があります。',
      CH: '[子类型] 结算汇总、执行、汇总表、已确认（报表）。在汇总、执行或已确认中点击含执行 ID 的行，可在下方显示该执行包含的交易列表。汇总表(SUM)仅一行摘要；本部拨付报表的执行(EXE)可能为汇总行而无执行 ID。',
      TH: '[ประเภทย่อย] สรุป / รัน / แผ่นสรุป / ยืนยันแล้ว คลิกแถวที่มี run ID ใน AGG/EXE/RST เพื่อแสดงธุรกรรมด้านล่าง SUM เป็นหนึ่งแถวสรุป EXE รายงานจ่าย HQ อาจไม่มี run ID'
    },
    '정산집계·정산실시의 비율형 수수료·건당수수료·부가세는 수수료 정책·거래 상태별 수수료내역 계산과 동일 규칙으로 집계합니다. 통화 열은 정책 통화(THB/KRW/USD/JPY 등)입니다.': {
      EN: 'Percentage fees, per-txn fees, and VAT in aggregate and runs are summed using the same rules as fee policy and per-status fee history. The currency column shows policy currency (THB/KRW/USD/JPY, etc.).',
      JP: '精算集計・実行の比率型手数料・件当手数料・消費税は、手数料政策・取引状態別の手数料一覧計算と同一ルールで集計します。通貨列は政策通貨（THB/KRW/USD/JPY 等）です。',
      CH: '结算汇总、执行中的比例手续费、按笔手续费、增值税按手续费政策及各交易状态的手续费明细相同规则汇总。货币列显示政策货币（THB/KRW/USD/JPY 等）。',
      TH: 'ค่าธรรมเนียม % ต่อรายการ และ VAT ใน AGG/EXE รวมตามกฎเดียวกับนโยบายและประวัติค่าธรรมเนียม คอลัมน์สกุลเงินแสดงสกุลนโยบาย (THB/KRW/USD/JPY ฯลฯ)'
    },
    '[배포 기준] 집계(AGG)·실시(EXE)·집계표(SUM)에는 정산배포가 완료된 실행(DISTRIBUTED, 레거시 null 허용)만 포함합니다. 가맹점정산내역·유통 집계와 동일합니다. 확정정산(RST)도 배포·확정된 실행만 표시합니다.': {
      EN: '[Publish gate] AGG/EXE/SUM include only settlement runs published as DISTRIBUTED (legacy null allowed)—same gate as merchant statements and distribution rollup. RST lists only published and CALCULATED runs.',
      JP: '[配布基準] AGG/EXE/SUM は配布済み(DISTRIBUTED、レガシーnull可)の実行のみ。加盟店精算・流通集計と同一。RST も配布・確定済みのみ。',
      CH: '[下发口径] AGG/EXE/SUM 仅含已下发(DISTRIBUTED，兼容历史 null)的执行，与商户结算明细、流通汇总一致。RST 亦仅已下发且已确认。',
      TH: '[เกณฑ์เผยแพร่] AGG/EXE/SUM รวมเฉพาะรันที่เผยแพร่แล้วเป็น DISTRIBUTED (รองรับ null เดิม) เหมือนรายการชำระร้านและสรุปห่วงโซ่ RST เฉพาะที่เผยแพร่และ CALCULATED'
    },
    '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 체크·보류해제 열은 항상 표시됩니다.': {
      EN: 'Toggle visible columns via VIEW SETTING next to Hello (saved per user). Checkbox and Release columns always stay visible.',
      JP: '表示列は［Hello］横の VIEW SETTING で調整できます（保存時ユーザー別に維持）。チェック・保留解除列は常に表示されます。',
      CH: '可通过 Hello 旁的 VIEW SETTING 调整显示列（按用户保存）。勾选与解除暂缓列始终显示。',
      TH: 'สลับคอลัมน์ที่มองเห็นผ่าน VIEW SETTING ข้าง Hello (บันทึกต่อผู้ใช้) คอลัมน์เลือกและปลดพักแสดงเสมอ'
    },
    '정산대상일': {
      EN: 'Settlement target date',
      JP: '精算対象日',
      CH: '结算目标日',
      TH: 'วันที่เป้าหมายชำระ'
    },
    '정산대상일(시작일~종료일)을 입력하세요.': {
      EN: 'Enter settlement target date (start date ~ end date).',
      JP: '精算対象日（開始日～終了日）を入力してください。',
      CH: '请输入结算目标日（开始日～结束日）。',
      TH: 'กรุณากรอกวันที่เป้าหมายชำระ (วันเริ่ม~วันสิ้นสุด)'
    },
    '결제액': {
      EN: 'Payment amount',
      JP: '決済額',
      CH: '支付金额',
      TH: 'ยอดชำระ'
    },
    '담보금': {
      EN: 'Collateral',
      JP: '担保金',
      CH: '保证金',
      TH: 'หลักประกัน'
    },
    '수수료': {
      EN: 'Fees',
      JP: '手数料',
      CH: '手续费',
      TH: 'ค่าธรรมเนียม'
    },
    '정산리포트 · 정산 대상 거래': {
      EN: 'Settlement report · target transactions',
      JP: '精算レポート・精算対象取引',
      CH: '结算报表·目标交易',
      TH: 'รายงานชำระ·ธุรกรรมเป้าหมาย'
    },
    '정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 해당 실행에 집계된 거래가 표시됩니다.': {
      EN: 'Click a row with a run ID in aggregate, runs, or confirmed to show transactions aggregated for that run.',
      JP: '集計・実行・確定精算で実行IDがある行をクリックすると、当該実行に集計された取引が表示されます。',
      CH: '在结算汇总、执行或已确认列表中点击含执行 ID 的行，可显示该执行汇总的交易。',
      TH: 'คลิกแถวที่มี run ID ในสรุป/รัน/ยืนยันเพื่อแสดงธุรกรรมที่รวมในรันนั้น'
    },
    '실행 ID가 있는 행을 클릭하세요.': {
      EN: 'Click a row that has a run ID.',
      JP: '実行IDがある行をクリックしてください。',
      CH: '请点击包含执行 ID 的行。',
      TH: 'คลิกแถวที่มี run ID'
    },
    '정산집계·정산실시·확정정산에서 <strong>실행 ID</strong>가 있는 행을 <strong>클릭</strong>하면 해당 정산 실행에 포함된 거래를 정산실행 화면과 동일한 형식으로 불러옵니다. <strong>정산집계표(SUM)</strong>는 요약 1행만 제공되고, <strong>본사 지급 리포트의 정산실시(EXE)</strong>는 본사 합산 행이라 실행 ID가 없을 수 있습니다 — 이 경우 리포트 형식을 가맹점 정산 리포트로 바꾼 뒤 가맹 단위 행을 클릭하세요.': {
      EN: 'In aggregate, runs, or confirmed, <strong>click</strong> a row that has a <strong>run ID</strong> to load included transactions in the same layout as settlement runs. <strong>Summary sheet (SUM)</strong> is a single summary row; <strong>HQ payout report → Runs (EXE)</strong> may be a regional aggregate without a run ID — switch the report format to the merchant report and click a merchant row.',
      JP: '集計・実行・確定精算で<strong>実行ID</strong>がある行を<strong>クリック</strong>すると、当該精算実行に含まれる取引を精算実行画面と同じ形式で読み込みます。<strong>集計表(SUM)</strong>は要約1行のみで、<strong>本社支払レポートの実行(EXE)</strong>は本社合算行のため実行IDがない場合があります。その場合はレポート形式を加盟店精算に切り替え、加盟店単位の行をクリックしてください。',
      CH: '在结算汇总、执行或已确认中<strong>单击</strong>含<strong>执行 ID</strong>的行，可按与结算执行相同格式加载包含的交易。<strong>汇总表(SUM)</strong>仅一行摘要；<strong>本部拨付报表的执行(EXE)</strong>可能为本部汇总行而无执行 ID——此时请将报表格式改为商户结算报表后点击商户行。',
      TH: 'ในสรุป/รัน/ยืนยัน <strong>คลิก</strong>แถวที่มี <strong>run ID</strong> เพื่อโหลดธุรกรรมในรูปแบบเดียวกับหน้ารัน SUM เป็นสรุปหนึ่งแถว รายงานจ่าย HQ แบบ EXE อาจไม่มี run ID — เปลี่ยนรูปแบบเป็นรายงานร้านแล้วคลิกแถวร้าน'
    },
    '지급액(배치)': {
      EN: 'Payout (batch)',
      JP: '支払額（バッチ）',
      CH: '拨付额（批次）',
      TH: 'ยอดจ่าย (แบตช์)'
    },
    '정산금합(추정)': {
      EN: 'Settlement total (est.)',
      JP: '精算金合計（見込）',
      CH: '结算款合计（估计）',
      TH: 'ยอดชำระรวม (ประมาณ)'
    },
    '기간FROM': {
      EN: 'Period FROM',
      JP: '期間FROM',
      CH: '期间FROM',
      TH: 'ช่วง FROM'
    },
    '기간TO': {
      EN: 'Period TO',
      JP: '期間TO',
      CH: '期间TO',
      TH: 'ช่วง TO'
    },
    '결제액합': {
      EN: 'Payment total',
      JP: '決済額合計',
      CH: '支付金额合计',
      TH: 'ยอดชำระรวม'
    },
    '환불합': {
      EN: 'Refund total',
      JP: '返金合計',
      CH: '退款合计',
      TH: 'ยอดคืนเงินรวม'
    },
    '순결제합': {
      EN: 'Net payment total',
      JP: '純決済合計',
      CH: '净支付合计',
      TH: 'ยอดชำระสุทธิรวม'
    },
    '예치합': {
      EN: 'Hold total',
      JP: '預かり合計',
      CH: '暂扣合计',
      TH: 'ยอดเงินประกันรวม'
    },
    'Processing합': {
      EN: 'Processing total',
      JP: 'Processing合計',
      CH: 'Processing合计',
      TH: 'Processing รวม'
    },
    '정산집계': {
      EN: 'Settlement aggregate',
      JP: '精算集計',
      CH: '结算汇总',
      TH: 'สรุปการชำระ'
    },
    '정산실시': {
      EN: 'Settlement runs',
      JP: '精算実行',
      CH: '结算执行',
      TH: 'รันชำระบัญชี'
    },
    '정산집계표': {
      EN: 'Summary sheet',
      JP: '精算集計表',
      CH: '结算汇总表',
      TH: 'แผ่นสรุป'
    },
    '확정정산(리포트)': {
      EN: 'Confirmed settlement (report)',
      JP: '確定精算（レポート）',
      CH: '已确认结算（报表）',
      TH: 'ชำระที่ยืนยัน (รายงาน)'
    },
    '가맹점수': {
      EN: 'Merchant count',
      JP: '加盟店数',
      CH: '商户数',
      TH: 'จำนวนร้าน'
    },
    '배치건수': {
      EN: 'Batch count',
      JP: 'バッチ件数',
      CH: '批次数',
      TH: 'จำนวนแบตช์'
    },
    '지급액합': {
      EN: 'Payout total',
      JP: '支払額合計',
      CH: '拨付额合计',
      TH: 'ยอดจ่ายรวม'
    },
    '담보금합': {
      EN: 'Collateral total',
      JP: '担保金合計',
      CH: '保证金合计',
      TH: 'ยอดหลักประกันรวม'
    },
    '미수금차감합': {
      EN: 'Receivable deduction total',
      JP: '未収金控除合計',
      CH: '应收扣减合计',
      TH: 'ยอดหักลูกหนี้รวม'
    },
    '지급액(추정)': {
      EN: 'Payout (est.)',
      JP: '支払額（見込）',
      CH: '拨付额（估计）',
      TH: 'ยอดจ่าย (ประมาณ)'
    },
    '지급액합(추정)': {
      EN: 'Payout total (est.)',
      JP: '支払額合計（見込）',
      CH: '拨付额合计（估计）',
      TH: 'ยอดจ่ายรวม (ประมาณ)'
    },
    '수수료합': {
      EN: 'Fee total',
      JP: '手数料合計',
      CH: '手续费合计',
      TH: 'ค่าธรรมเนียมรวม'
    },
    '결제일': {
      EN: 'Payment date',
      JP: '決済日',
      CH: '支付日',
      TH: 'วันที่ชำระ'
    },
    '정산일': {
      EN: 'Settlement date',
      JP: '精算日',
      CH: '结算日',
      TH: 'วันชำระ'
    },
    '완료': {
      EN: 'Done',
      JP: '完了',
      CH: '完成',
      TH: 'เสร็จ'
    },
    '집계행수': {
      EN: 'Aggregate rows',
      JP: '集計行数',
      CH: '汇总行数',
      TH: 'จำนวนแถวสรุป'
    },
    '거래 집계만. 정산 실행당 1회 정산수수료는 정산료 열.': {
      EN: 'Txn aggregates only; the per-run settlement fee appears in the Settlement fee column.',
      JP: '取引集計のみ。精算実行ごとの精算手数料は「精算手数料」列に表示されます。',
      CH: '仅为交易汇总；每笔执行的一次性结算手续费见「结算手续费」列。',
      TH: 'เฉพาะยอดรวมธุรกรรม; ค่าธรรมเนียมชำระต่อรันดูที่คอลัมน์ค่าธรรมเนียมชำระ'
    },
    '자동=차기정산 FIFO, 수동=환수처리 후 차감. 본사합산 행은 가맹 혼합.': {
      EN: 'Auto = FIFO on next settlement; manual = deduct after recovery. HQ aggregate rows may mix merchants.',
      JP: '自動=次回精算FIFO、手動=回収処理後に控除。本社合算行は加盟店混在の場合があります。',
      CH: '自动=下次结算 FIFO；手动=回款处理后扣减。本部汇总行可能混合多商户。',
      TH: 'อัตโนมัติ=FIFO รอบถัดไป ด้วยมือ=หักหลังกู้คืน แถวรวม HQ อาจผสมร้าน'
    },
    '해당 정산 실행에서 미수금으로 차감된 합계.': {
      EN: 'Total receivable deducted on this settlement run.',
      JP: '当該精算実行で未収金として控除した合計。',
      CH: '该笔结算执行中以应收扣减的合计。',
      TH: 'ยอดหักลูกหนี้ในรันชำระนี้'
    },
    '전송일자': {
      EN: 'Send date',
      JP: '送信日',
      CH: '发送日期',
      TH: 'วันที่ส่ง'
    },
    '전송일시': {
      EN: 'Sent at',
      JP: '送信日時',
      CH: '发送时间',
      TH: 'เวลาที่ส่ง'
    },
    '결과': {
      EN: 'Result',
      JP: '結果',
      CH: '结果',
      TH: 'ผลลัพธ์'
    },
    '재전송횟수': {
      EN: 'Retry count',
      JP: '再送信回数',
      CH: '重试次数',
      TH: 'จำนวนครั้งที่ส่งซ้ำ'
    },
    /* /hq/pgApiMng — 그리드·모달·알림·헬로 안내 */
    '연동 용도(노티·URL·챗봇·API)와 용도별 엔드포인트를 구분해 저장합니다. URL 용도 행은 「URL금액」에서 일반(일반형) / DP(DISPLAY) / BLIND를 지정할 수 있으며, 본사 URL결제설정(FX JSON)의 해당 PG 금액 모드와 동일합니다. 노티=미들웨어 수신 매칭, URL=공개 URL 결제 플로우, 챗봇/API=PG사 API 직연동(동일 연동 URL). 목록 「연동용도」는 파스텔 색으로 구분됩니다. API Key·MD5는 목록 미노출. [삭제]는 등록일 오른쪽, 신규는 [PG사 연동 추가]입니다.': {
      EN: 'Integration scopes (notify, URL, chatbot, API) and per-scope endpoints are stored separately. For URL rows, set Standard / DP (DISPLAY) / BLIND under URL amount — same as the HQ URL pay FX JSON amount mode for that PG. Notify = middleware receive matching; URL = public URL checkout; chatbot/API = direct PG API (same linkage URL). Scope badges use pastel colors. API Key and MD5 are hidden in the list. [Delete] is right of Registered; add new via [Add PG linkage].',
      JP: '連携用途（ノティ・URL・チャットボット・API）と用途別エンドポイントを分けて保存します。URL用途行は「URL金額」で標準 / DP(DISPLAY) / BLIND を指定でき、本社URL決済設定（FX JSON）の当該PG金額モードと同じです。ノティ=ミドルウェア受信マッチ、URL=公開URL決済、チャットボット/API=PG API直連携（同一連携URL）。一覧「連携用途」はパステル色で区別。API Key・MD5は一覧非表示。[削除]は登録日の右、[PG連携追加]で新規登録。',
      CH: '按对接用途（通知·URL·聊天机器人·API）分别保存各用途端点。URL 用途行可在「URL 金额」选择标准 / DP(DISPLAY) / BLIND，与总部 URL 支付设置（FX JSON）中该 PG 的金额模式一致。通知=中间件接收匹配，URL=公开 URL 支付流程，聊天机器人/API=直连 PG API（同一对接 URL）。列表「对接用途」以淡色区分。API Key·MD5 不在列表显示。[删除]在注册日期右侧，新增用 [添加 PG 对接]。',
      TH: 'แยกบันทึกขอบเขตการเชื่อม (แจ้งเตือน·URL·แชทบอท·API) และเอนด์พอยต์ตามขอบเขต แถว URL ตั้ง มาตรฐาน / DP (DISPLAY) / BLIND ที่「โหมดจำนวน URL» — ตรงกับโหมดจำนวน PG ในการตั้งค่า URL สำนักงานใหญ่ (FX JSON) แจ้งเตือน=จับคู่รับ middleware URL=เช็คเอาต์ URL สาธารณะ แชทบอท/API=เชื่อม PG โดยตรง (URL เดียวกัน) ป้ายขอบเขตใช้สีพาสเทล API Key·MD5 ไม่แสดงในรายการ [ลบ] อยู่ขวาของวันที่ลงทะเบียน สร้างใหม่ด้วย [เพิ่มการเชื่อม PG]'
    },
    '통합정산 「예정(ICOPAY)」열: PG사 연동 편집에서 T+N(주말 제외 영업일·결제와 동일 시각) 또는 D+N(달력+N일·일괄 시각)을 저장합니다. OFF면 예정일을 채우지 않습니다. D는 일괄 시각(HH:mm) 필수.': {
      EN: 'Integrated settlement Sched. (ICOPAY): in PG linkage edit, save T+N (business days excluding weekends, same time as payment) or D+N (calendar +N days, batch time). OFF leaves the expected date blank. D requires batch time (HH:mm).',
      JP: '統合精算「予定(ICOPAY)」列: PG連携編集で T+N（週末除く営業日・決済と同時刻）または D+N（暦+N日・一括時刻）を保存。OFF なら予定日を空に。D は一括時刻(HH:mm)必須。',
      CH: '综合结算「预计(ICOPAY)」列：在 PG 对接编辑中保存 T+N（不含周末的营业日·与支付同时刻）或 D+N（日历+N 天·批量时刻）。OFF 则不填预计日。D 必须填写批量时刻(HH:mm)。',
      TH: 'คอลัมน์กำหนด (ICOPAY) การชำระรวม: แก้การเชื่อม PG บันทึก T+N (วันทำการไม่รวมสุดสัปดาห์·เวลาเดียวกับชำระ) หรือ D+N (ปฏิทิน+N วัน·เวลารวม) OFF ไม่เติมวันคาด D ต้องมีเวลารวม (HH:mm)'
    },
    'ChillPay는 PG코드 CHILLPAY, API·URL 엔드포인트는 ChillPayService가 병합 반영합니다. 운영 DB는 db/V35_pg_agency_integration_scope.sql 적용 후 배포하세요.': {
      EN: 'ChillPay uses PG code CHILLPAY; API/URL endpoints are merged by ChillPayService. Apply db/V35_pg_agency_integration_scope.sql on the production DB before deploy.',
      JP: 'ChillPay は PGコード CHILLPAY。API・URL エンドポイントは ChillPayService がマージ反映。本番 DB に db/V35_pg_agency_integration_scope.sql を適用してからデプロイしてください。',
      CH: 'ChillPay 使用 PG 代码 CHILLPAY；API·URL 端点由 ChillPayService 合并写入。请在运营 DB 执行 db/V35_pg_agency_integration_scope.sql 后再部署。',
      TH: 'ChillPay ใช้รหัส PG CHILLPAY เอนด์พอยต์ API·URL ถูกรวมโดย ChillPayService ใช้ db/V35_pg_agency_integration_scope.sql บน DB โปรduction ก่อน deploy'
    },
    '운영 저장': {
      EN: 'Save operational',
      JP: '運用保存',
      CH: '保存运营',
      TH: 'บันทึกการทำงานจริง'
    },
    'PG사 연동 추가': {
      EN: 'Add PG linkage',
      JP: 'PG連携追加',
      CH: '添加 PG 对接',
      TH: 'เพิ่มการเชื่อม PG'
    },
    '조회된 데이터가 없습니다.': {
      EN: 'No records found.',
      JP: '該当データがありません。',
      CH: '没有查询到数据。',
      TH: 'ไม่พบข้อมูล'
    },
    '결제대행사': {
      EN: 'Acquirer / PSP',
      JP: '決済代行',
      CH: '支付机构',
      TH: 'ผู้ให้บริการชำระเงิน'
    },
    터미널ID: {
      EN: 'Terminal ID',
      JP: '端末ID',
      CH: '终端 ID',
      TH: 'รหัสเทอร์มินัล'
    },
    'PG코드': { EN: 'PG code', JP: 'PGコード', CH: 'PG 代码', TH: 'รหัส PG' },
    '연동용도': { EN: 'Integration scope', JP: '連携用途', CH: '对接用途', TH: 'ขอบเขตการเชื่อม' },
    'URL금액': { EN: 'URL amount mode', JP: 'URL金額', CH: 'URL 金额', TH: 'โหมดจำนวน URL' },
    '엔드포인트': { EN: 'Endpoint', JP: 'エンドポイント', CH: '端点', TH: 'เอนด์พอยต์' },
    'MD5': { EN: 'MD5', JP: 'MD5', CH: 'MD5', TH: 'MD5' },
    'RT': { EN: 'RT', JP: 'RT', CH: 'RT', TH: 'RT' },
    '환경': { EN: 'Environment', JP: '環境', CH: '环境', TH: 'สภาพแวดล้อม' },
    '예정': { EN: 'Sched.', JP: '予定', CH: '预计', TH: 'กำหนด' },
    'D시각': { EN: 'D time', JP: 'D時刻', CH: 'D 时刻', TH: 'เวลา D' },
    '사용': { EN: 'Active', JP: '使用', CH: '使用', TH: 'ใช้งาน' },
    '미사용': { EN: 'Inactive', JP: '未使用', CH: '未使用', TH: 'ไม่ใช้งาน' },
    '등록일': { EN: 'Registered', JP: '登録日', CH: '注册日期', TH: 'วันที่ลงทะเบียน' },
    '등록일자': { EN: 'Registered date', JP: '登録日付', CH: '注册日期', TH: 'วันที่ลงทะเบียน' },
    '수정일자': { EN: 'Last updated', JP: '更新日時', CH: '修改日期时间', TH: 'วันที่แก้ไข' },
    '관리': { EN: 'Actions', JP: '管理', CH: '操作', TH: 'จัดการ' },
    'URL결제: 일반형 / DP(DISPLAY) / BLIND': {
      EN: 'URL pay: Standard / DP (DISPLAY) / BLIND',
      JP: 'URL決済: 標準 / DP(DISPLAY) / BLIND',
      CH: 'URL 支付：标准 / DP(DISPLAY) / BLIND',
      TH: 'ชำระ URL: มาตรฐาน / DP(DISPLAY) / BLIND'
    },
    'Route 번호': { EN: 'Route no.', JP: 'ルート番号', CH: '路由号', TH: 'หมายเลข Route' },
    'Sandbox / Production': {
      EN: 'Sandbox / Production',
      JP: 'Sandbox / Production',
      CH: 'Sandbox / Production',
      TH: 'Sandbox / Production'
    },
    '통합정산 ICOPAY 예정: OFF/T/D': {
      EN: 'Integrated settlement expected (ICOPAY): OFF / T / D',
      JP: '統合精算 ICOPAY 予定: OFF/T/D',
      CH: '综合结算 ICOPAY 预计：OFF/T/D',
      TH: 'คาดการณ์ ICOPAY: OFF/T/D'
    },
    '노티': { EN: 'Notify', JP: 'ノティ', CH: '通知', TH: 'แจ้งเตือน' },
    'URL': { EN: 'URL', JP: 'URL', CH: 'URL', TH: 'URL' },
    'Sandbox': { EN: 'Sandbox', JP: 'Sandbox', CH: '沙盒', TH: 'Sandbox' },
    'Production': { EN: 'Production', JP: '本番', CH: '生产', TH: 'Production' },
    '챗봇': { EN: 'Chatbot', JP: 'チャットボット', CH: '聊天机器人', TH: 'แชทบอท' },
    '복합(레거시)': {
      EN: 'Multi (legacy)',
      JP: '複合（レガシー）',
      CH: '复合（遗留）',
      TH: 'หลายประเภท (เลกาซี)'
    },
    '본사미운영': {
      EN: 'Not HQ-operational',
      JP: '本社未運用',
      CH: '总部未运营',
      TH: 'สำนักงานใหญ่ไม่เปิดใช้'
    },
    '구버전': { EN: 'Legacy', JP: '旧版', CH: '旧版', TH: 'รุ่นเก่า' },
    '일반': { EN: 'Standard', JP: '標準', CH: '标准', TH: 'มาตรฐาน' },
    '연동 자격 수정': {
      EN: 'Edit linkage credentials',
      JP: '連携資格を修正',
      CH: '编辑对接凭据',
      TH: 'แก้ไขข้อมูลเชื่อมต่อ'
    },
    'PG 연동 삭제': {
      EN: 'Remove PG linkage',
      JP: 'PG連携を削除',
      CH: '删除 PG 对接',
      TH: 'ลบการเชื่อม PG'
    },
    '미사용 PG는 운영 지정 불가': {
      EN: 'Inactive PG cannot be set operational',
      JP: '未使用のPGは運用指定できません',
      CH: '未启用的 PG 不可设为运营',
      TH: 'PG ที่ปิดใช้ไม่สามารถตั้งเป็นใช้งานจริง'
    },
    '결제 운영(가맹점 연동·PG 선택 노출)': {
      EN: 'Operational (merchant linkage & PG picker)',
      JP: '決済運用（加盟店連携・PG選択表示）',
      CH: '支付运营（商户对接与 PG 选择展示）',
      TH: 'การทำงานจริง (เชื่อมร้านและแสดงเลือก PG)'
    },
    '결제대행사 운영 설정을 저장하시겠습니까?': {
      EN: 'Save operational acquirer settings?',
      JP: '決済代行の運用設定を保存しますか？',
      CH: '要保存支付机构运营设置吗？',
      TH: 'บันทึกการตั้งค่าผู้ให้บริการชำระแบบใช้งานจริงหรือไม่'
    },
    '체크한 PG만 운영(가맹점 PG 선택·연동)으로 저장됩니다. 계속하시겠습니까?': {
      EN: 'Only checked PGs will be saved as operational (merchant PG selection/linkage). Continue?',
      JP: 'チェックしたPGのみ運用（加盟店のPG選択・連携）として保存されます。続行しますか？',
      CH: '仅勾选的 PG 将保存为运营（商户 PG 选择与对接）。是否继续？',
      TH: 'เฉพาะ PG ที่ติ๊กจะบันทึกเป็นใช้งานจริง (การเลือกและเชื่อม PG ของร้าน) ดำเนินต่อหรือไม่'
    },
    '운영 설정이 저장되었습니다.': {
      EN: 'Operational settings saved.',
      JP: '運用設定を保存しました。',
      CH: '运营设置已保存。',
      TH: 'บันทึกการตั้งค่าการทำงานจริงแล้ว'
    },
    'PG 연동을 삭제하시겠습니까?': {
      EN: 'Delete this PG linkage?',
      JP: 'このPG連携を削除しますか？',
      CH: '要删除此 PG 对接吗？',
      TH: 'ลบการเชื่อม PG นี้หรือไม่'
    },
    '삭제 후 복구할 수 없습니다. 가맹점 결제대행사에서 이 PG를 사용 중이면 삭제할 수 없습니다. 계속하시겠습니까?': {
      EN: 'Deletion cannot be undone. If any merchant still uses this PG as acquirer, delete will be blocked. Continue?',
      JP: '削除後は元に戻せません。加盟店の決済代行でこのPGを使用中の場合は削除できません。続行しますか？',
      CH: '删除后无法恢复。若商户仍在使用该 PG 作为支付机构则无法删除。是否继续？',
      TH: 'ลบแล้วกู้คืนไม่ได้ หากร้านยังใช้ PG นี้เป็นผู้ให้บริการชำระจะลบไม่ได้ ดำเนินต่อหรือไม่'
    },
    '삭제되었습니다.': {
      EN: 'Deleted.',
      JP: '削除しました。',
      CH: '已删除。',
      TH: 'ลบแล้ว'
    },
    '삭제 실패': { EN: 'Delete failed', JP: '削除に失敗しました', CH: '删除失败', TH: 'ลบไม่สำเร็จ' },
    '가맹점에서 이 PG를 사용 중이어서 삭제할 수 없습니다.': {
      EN: 'Merchants are still using this PG; it cannot be deleted.',
      JP: '加盟店がこのPGを使用中のため削除できません。',
      CH: '商户仍在使用该 PG，无法删除。',
      TH: 'ร้านค้ายังใช้ PG นี้อยู่ จึงลบไม่ได้'
    },
    'PG사 연동 정보를 저장하시겠습니까?': {
      EN: 'Save PG linkage information?',
      JP: 'PG連携情報を保存しますか？',
      CH: '要保存 PG 对接信息吗？',
      TH: 'บันทึกข้อมูลการเชื่อม PG หรือไม่'
    },
    '정말 저장하시겠습니까?': {
      EN: 'Really save?',
      JP: '本当に保存しますか？',
      CH: '确定要保存吗？',
      TH: 'ยืนยันบันทึกหรือไม่'
    },
    'PG코드와 결제대행사는 필수입니다.': {
      EN: 'PG code and acquirer name are required.',
      JP: 'PGコードと決済代行名は必須です。',
      CH: 'PG 代码与支付机构名称为必填。',
      TH: 'ต้องกรอกรหัส PG และชื่อผู้ให้บริการชำระ'
    },
    '연동 용도를 선택하세요. 용도별로 PG코드를 나누어 등록합니다.': {
      EN: 'Select an integration scope. Register separate PG codes per scope.',
      JP: '連携用途を選択してください。用途ごとにPGコードを分けて登録します。',
      CH: '请选择对接用途。不同用途请分别注册 PG 代码。',
      TH: 'เลือกประเภทการเชื่อมต่อ ลงทะเบียนรหัส PG แยกตามประเภท'
    },
    'PG사 연동': {
      EN: 'PG linkage',
      JP: 'PG連携',
      CH: 'PG 对接',
      TH: 'การเชื่อม PG'
    },
    '닫기': { EN: 'Close', JP: '閉じる', CH: '关闭', TH: 'ปิด' },
    'PG사코드': { EN: 'PG code', JP: 'PG社コード', CH: 'PG 代码', TH: 'รหัส PG' },
    '연동 용도': { EN: 'Integration scope', JP: '連携用途', CH: '对接用途', TH: 'ประเภทการเชื่อม' },
    '엔드포인트 URL': {
      EN: 'Endpoint URL',
      JP: 'エンドポイントURL',
      CH: '端点 URL',
      TH: 'URL เอนด์พอยต์'
    },
    '(선택)': { EN: '(optional)', JP: '（任意）', CH: '（可选）', TH: '(ไม่บังคับ)' },
    'URL 결제 금액 모드': {
      EN: 'URL pay amount mode',
      JP: 'URL決済金額モード',
      CH: 'URL 支付金额模式',
      TH: 'โหมดจำนวนเงิน URL'
    },
    '구버전 통합 URL': {
      EN: 'Legacy combined URL',
      JP: '旧版統合URL',
      CH: '旧版综合 URL',
      TH: 'URL รวมรุ่นเก่า'
    },
    '(선택, api_endpoint 호환)': {
      EN: '(optional, api_endpoint compat.)',
      JP: '（任意、api_endpoint互換）',
      CH: '（可选，兼容 api_endpoint）',
      TH: '(ไม่บังคับ เข้ากันได้กับ api_endpoint)'
    },
    'MID / Merchant Code': {
      EN: 'MID / Merchant Code',
      JP: 'MID / Merchant Code',
      CH: 'MID / Merchant Code',
      TH: 'MID / Merchant Code'
    },
    'API Key': { EN: 'API Key', JP: 'API Key', CH: 'API Key', TH: 'API Key' },
    'MD5 / 서명 시크릿': {
      EN: 'MD5 / signing secret',
      JP: 'MD5 / 署名シークレット',
      CH: 'MD5 / 签名密钥',
      TH: 'MD5 / ความลับลายเซ็น'
    },
    '추가 설정(JSON)': {
      EN: 'Extra settings (JSON)',
      JP: '追加設定（JSON）',
      CH: '附加设置（JSON）',
      TH: 'การตั้งค่าเพิ่ม (JSON)'
    },
    '통합정산 예정일(우리↔PG)': {
      EN: 'Integrated settlement expected date (us↔PG)',
      JP: '統合精算予定日（当社↔PG）',
      CH: '综合结算预计日（我方↔PG）',
      TH: 'วันที่คาดการณ์ชำระรวม (เรา↔PG)'
    },
    'D모드 일괄시각': {
      EN: 'D-mode batch time',
      JP: 'Dモード一括時刻',
      CH: 'D 模式批量时刻',
      TH: 'เวลารวมโหมด D'
    },
    '사용여부': { EN: 'In use', JP: '使用状態', CH: '使用状态', TH: 'สถานะการใช้งาน' },
    '사용안함': { EN: 'Off', JP: '使用しない', CH: '关闭', TH: 'ปิดใช้' },
    'T+N (영업일·동일시각)': {
      EN: 'T+N (business days, same time)',
      JP: 'T+N（営業日・同一時刻）',
      CH: 'T+N（营业日·同时刻）',
      TH: 'T+N (วันทำการ เวลาเดียวกัน)'
    },
    'D+N (달력·일괄시각)': {
      EN: 'D+N (calendar, batch time)',
      JP: 'D+N（暦・一括時刻）',
      CH: 'D+N（日历·批量时刻）',
      TH: 'D+N (ปฏิทิน เวลารวม)'
    },
    'D모드: 결제일 기준 N일 후 같은 달력일의 지정 시각에 일괄 정산 예정으로 표시합니다. D시각(HH:mm)을 입력하세요.': {
      EN: 'D mode: shows batch settlement on the same calendar day N days after payment date at the given time. Enter D time (HH:mm).',
      JP: 'Dモード: 決済日基準N日後の同一暦日の指定時刻に一括精算予定として表示します。D時刻(HH:mm)を入力してください。',
      CH: 'D 模式：在支付日起第 N 个自然日的指定时刻显示为批量结算预计。请输入 D 时刻(HH:mm)。',
      TH: 'โหมด D: แสดงการชำระรวมตามวันปฏิทินเดียวกัน N วันหลังวันชำระ ณ เวลาที่กำหนด กรอกเวลา D (HH:mm)'
    },
    'T모드: 주말 제외 N영업일 후, 결제와 동일 시각으로 예정일을 표시합니다. D시각은 사용하지 않습니다.': {
      EN: 'T mode: expected date is N business days after payment (weekends excluded), same time as payment. D time is not used.',
      JP: 'Tモード: 週末を除くN営業日後、決済と同一時刻で予定日を表示します。D時刻は使いません。',
      CH: 'T 模式：支付后第 N 个工作日（不含周末）的与支付同时刻显示预计日。不使用 D 时刻。',
      TH: 'โหมด T: แสดงวันที่คาดการณ์หลัง N วันทำการไม่นับสุดสัปดาห์ เวลาเดียวกับการชำระ ไม่ใช้เวลา D'
    },
    'OFF면 통합정산에 ICOPAY 예정일을 채우지 않습니다. T는 주말 제외 영업일 기준입니다.': {
      EN: 'OFF leaves ICOPAY expected date empty. T uses business days excluding weekends.',
      JP: 'OFFのときは統合精算のICOPAY予定日を埋めません。Tは土日を除く営業日基準です。',
      CH: 'OFF 时不填写综合结算的 ICOPAY 预计日。T 按不含周末的营业日。',
      TH: 'ถ้า OFF ไม่เติมวันที่คาด ICOPAY ในการชำระรวม T คิดตามวันทำการไม่นับสุดสัปดาห์'
    },
    'OFF면 통합정산에 ICOPAY 예정일을 채우지 않습니다.': {
      EN: 'When OFF, ICOPAY expected date is not filled in integrated settlement.',
      JP: 'OFFのときは統合精算にICOPAY予定日を入れません。',
      CH: 'OFF 时不写入综合结算的 ICOPAY 预计日。',
      TH: 'เมื่อ OFF ไม่เติมวันที่คาด ICOPAY ในการชำระรวม'
    },
    /* /hq/defaultCommission — HQ default fee templates & policy list */
    '기본 수수료 정책': {
      EN: 'Default fee policy',
      JP: '基本手数料ポリシー',
      CH: '默认手续费政策',
      TH: 'นโยบายค่าธรรมเนียมเริ่มต้น'
    },
    '총본사~영업점은 조직 배분(결제율·건당)에 반영됩니다. 가맹 열은 가맹점에 적용되는 합계(기본값)이며, 가맹점이 본사설정을 따를 때 기준이 됩니다. 업체관리 수수료에서 수정하면 그 값이 우선합니다. 결제·USDT·FX는 승인금액 기준 %, 3DS는 정책통화 기준 건당 고정, 나머지 건당·월간은 통화 단위입니다.': {
      EN: 'Head office through sales office tiers feed organization splits (pay % and per-txn). The merchant column is the merchant-facing total (default); merchants that follow HQ settings use it as the baseline. Values edited in merchant management override. Pay, USDT, and FX are % of approved amount; 3DS is a per-txn fixed fee in the policy currency; other per-txn and monthly amounts use the currency unit.',
      JP: '総本社〜営業店は組織配分（決済率・件当）に反映されます。加盟店列は加盟店に適用される合計（既定）で、本社設定に従う加盟店の基準になります。加盟店管理で変更した値が優先されます。決済・USDT・FXは承認金額基準の%、3DSはポリシー通貨基準の件当固定、その他の件当・月次は通貨単位です。',
      CH: '总本部至营业点参与组织分成（支付费率·笔数）。加盟列为加盟店适用合计（默认值），跟随总部设置的商户以此为基准。在商户管理中修改的值优先。支付·USDT·FX 为授权金额基准的百分比；3DS 为政策货币的每笔固定；其余每笔与月度为货币单位。',
      TH: 'สำนักงานใหญ่สุดถึงสาขาขายสะท้อนการแบ่งองค์กร (% ชำระ และต่อรายการ) คอลัมน์ร้านคือยอดรวมที่ร้านได้รับ (ค่าเริ่มต้น) และเป็นฐานเมื่อร้านตามการตั้งค่า HQ ค่าที่แก้ในการจัดการร้านจะมีผลก่อน การชำระ·USDT·FX เป็น % ของยอดอนุมัติ 3DS เป็นค่าคงที่ต่อรายในสกุลนโยบาย อื่นๆ ต่อรายและรายเดือนเป็นหน่วยสกุลเงิน'
    },
    '정책코드': {
      EN: 'Policy code',
      JP: 'ポリシーコード',
      CH: '政策代码',
      TH: 'รหัสนโยบาย'
    },
    '코드는 저장 시 자동 부여되며, 수정할 수 없습니다.': {
      EN: 'The code is assigned automatically on save and cannot be edited.',
      JP: 'コードは保存時に自動付与され、変更できません。',
      CH: '代码在保存时自动分配，不可修改。',
      TH: 'รหัสถูกกำหนดอัตโนมัติเมื่อบันทึกและแก้ไขไม่ได้'
    },
    '(신규) 저장 시 자동 부여': {
      EN: '(New) assigned on save',
      JP: '（新規）保存時に自動付与',
      CH: '（新建）保存时自动分配',
      TH: '(ใหม่) กำหนดเมื่อบันทึก'
    },
    '고유 코드는 시스템이 부여합니다. 목록에서 정책을 불러와 편집만 할 수 있습니다.': {
      EN: 'The system assigns the unique code. Load a policy from the list to edit it.',
      JP: '固有コードはシステムが付与します。一覧からポリシーを読み込み、編集のみできます。',
      CH: '唯一代码由系统分配。请从列表加载政策后仅可编辑。',
      TH: 'ระบบกำหนดรหัสเฉพาะ โหลดนโยบายจากรายการแล้วแก้ไขได้เท่านั้น'
    },
    '정책명': {
      EN: 'Policy name',
      JP: 'ポリシー名',
      CH: '政策名称',
      TH: 'ชื่อนโยบาย'
    },
    '예: 기본정책 A': {
      EN: 'e.g. Default policy A',
      JP: '例: 基本ポリシーA',
      CH: '例：默认政策 A',
      TH: 'เช่น นโยบายเริ่มต้น A'
    },
    '배포': {
      EN: 'Published',
      JP: '配布',
      CH: '发布',
      TH: 'เผยแพร่'
    },
    '미배포': {
      EN: 'Unpublished',
      JP: '未配布',
      CH: '未发布',
      TH: 'ยังไม่เผยแพร่'
    },
    '통화코드': {
      EN: 'Currency code',
      JP: '通貨コード',
      CH: '货币代码',
      TH: 'รหัสสกุลเงิน'
    },
    '차지백 구간정책': {
      EN: 'Chargeback tier policy',
      JP: 'チャージバック段階ポリシー',
      CH: '拒付分段政策',
      TH: 'นโยบายชาร์จแบ็กแบบช่วง'
    },
    '(미사용) 건당 차지백만': {
      EN: '(Off) per-txn chargeback fee only',
      JP: '（未使用）件当チャージバックのみ',
      CH: '（未使用）仅每笔拒付费',
      TH: '(ปิด) ค่าชาร์จแบ็กต่อรายเท่านั้น'
    },
    '정책비고(저장)': {
      EN: 'Policy remark (saved)',
      JP: 'ポリシー備考（保存）',
      CH: '政策备注（保存）',
      TH: 'หมายเหตุนโยบาย (บันทึก)'
    },
    '총본사': {
      EN: 'Root HQ',
      JP: '総本社',
      CH: '总本部',
      TH: 'สำนักงานใหญ่สุด'
    },
    '본사': {
      EN: 'Regional HQ',
      JP: '本社',
      CH: '本部',
      TH: 'สำนักงานใหญ่'
    },
    '총판': {
      EN: 'Master distributor',
      JP: '総販',
      CH: '总代',
      TH: 'ตัวแทนหลัก'
    },
    '지사': {
      EN: 'Branch',
      JP: '支社',
      CH: '分公司',
      TH: 'สาขา'
    },
    '대리점': {
      EN: 'Agency',
      JP: '代理店',
      CH: '代理店',
      TH: 'ตัวแทน'
    },
    '영업점': {
      EN: 'Sales office',
      JP: '営業店',
      CH: '营业点',
      TH: 'จุดขาย'
    },
    '가맹점': {
      EN: 'Merchant',
      JP: '加盟店',
      CH: '商户',
      TH: 'ร้านค้า'
    },
    '결제수수료율': {
      EN: 'Payment fee rate',
      JP: '決済手数料率',
      CH: '支付手续费率',
      TH: 'อัตราค่าธรรมเนียมชำระ'
    },
    '건당수수료': {
      EN: 'Per-txn fee',
      JP: '件当手数料',
      CH: '每笔手续费',
      TH: 'ค่าธรรมเนียมต่อรายการ'
    },
    '실패수수료': {
      EN: 'Failed-txn fee',
      JP: '失敗手数料',
      CH: '失败手续费',
      TH: 'ค่าธรรมเนียมเมื่อล้มเหลว'
    },
    '취소수수료': {
      EN: 'Cancel fee',
      JP: '取消手数料',
      CH: '撤销手续费',
      TH: 'ค่าธรรมเนียมยกเลิก'
    },
    '무효수수료': {
      EN: 'Void fee',
      JP: '無効手数料',
      CH: '作废手续费',
      TH: 'ค่าธรรมเนียมโมฆะ'
    },
    '수무효수수료': {
      EN: 'Manual void fee',
      JP: '手動無効手数料',
      CH: '手动作废手续费',
      TH: 'ค่าธรรมเนียมโมฆะด้วยมือ'
    },
    '환불수수료': {
      EN: 'Refund fee',
      JP: '返金手数料',
      CH: '退款手续费',
      TH: 'ค่าธรรมเนียมคืนเงิน'
    },
    '정산수수료': {
      EN: 'Settlement fee',
      JP: '精算手数料',
      CH: '结算手续费',
      TH: 'ค่าธรรมเนียมชำระบัญชี'
    },
    '송금수수료': {
      EN: 'Remittance fee',
      JP: '送金手数料',
      CH: '汇款手续费',
      TH: 'ค่าธรรมเนียมโอน'
    },
    'USDT 송금수수료': {
      EN: 'USDT transfer fee',
      JP: 'USDT送金手数料',
      CH: 'USDT 汇款手续费',
      TH: 'ค่าธรรมเนียมโอน USDT'
    },
    '3DS 고정': {
      EN: '3DS fixed fee',
      JP: '3DS固定',
      CH: '3DS 固定费',
      TH: 'ค่าคงที่ 3DS'
    },
    'USDT수수료율': {
      EN: 'USDT fee rate',
      JP: 'USDT手数料率',
      CH: 'USDT 手续费率',
      TH: 'อัตราค่าธรรมเนียม USDT'
    },
    'FX수수료율': {
      EN: 'FX fee rate',
      JP: 'FX手数料率',
      CH: 'FX 手续费率',
      TH: 'อัตราค่าธรรมเนียม FX'
    },
    '월간이용료': {
      EN: 'Monthly usage fee',
      JP: '月次利用料',
      CH: '月度使用费',
      TH: 'ค่าธรรมเนียมรายเดือน'
    },
    '월': {
      EN: 'mo.',
      JP: '月',
      CH: '月',
      TH: 'ด.'
    },
    '%': {
      EN: '%',
      JP: '%',
      CH: '%',
      TH: '%'
    },
    '차지백수수료': {
      EN: 'Chargeback fee',
      JP: 'チャージバック手数料',
      CH: '拒付手续费',
      TH: 'ค่าธรรมเนียมชาร์จแบ็ก'
    },
    '내용': {
      EN: 'Item',
      JP: '内容',
      CH: '项目',
      TH: 'รายการ'
    },
    '단위': {
      EN: 'Unit',
      JP: '単位',
      CH: '单位',
      TH: 'หน่วย'
    },
    '(건)': {
      EN: '(per txn)',
      JP: '（件）',
      CH: '（笔）',
      TH: '(ต่อรายการ)'
    },
    '(%)': {
      EN: '(%)',
      JP: '(%)',
      CH: '(%)',
      TH: '(%)'
    },
    '유형': {
      EN: 'Type',
      JP: '種類',
      CH: '类型',
      TH: 'ประเภท'
    },
    '수수료명': {
      EN: 'Fee name',
      JP: '手数料名',
      CH: '手续费名称',
      TH: 'ชื่อค่าธรรมเนียม'
    },
    '고정': {
      EN: 'Fixed',
      JP: '固定',
      CH: '固定',
      TH: 'คงที่'
    },
    '이름': {
      EN: 'Name',
      JP: '名前',
      CH: '名称',
      TH: 'ชื่อ'
    },
    '기타 수수료 (비고 · 최대 4건)': {
      EN: 'Other fees (remark · up to 4)',
      JP: 'その他手数料（備考・最大4件）',
      CH: '其他手续费（备注·最多4条）',
      TH: 'ค่าธรรมเนียมอื่น (หมายเหตุ·สูงสุด 4)'
    },
    '이름·유형·조직별 값을 넣은 슬롯만 반영됩니다. 가맹 열은 총본사~영업점 합계로 표시·저장됩니다.': {
      EN: 'Only slots with name, type, and per-tier values are saved. The merchant column shows and stores the sum from root HQ through sales office.',
      JP: '名前・種類・組織別の値を入れたスロットのみ反映されます。加盟店列は総本社〜営業店の合計で表示・保存されます。',
      CH: '仅填写名称、类型与各组织值的槽位会保存。加盟列显示并保存总本部至营业点的合计。',
      TH: 'เฉพาะช่องที่กรอกชื่อ ประเภท และค่าระดับองค์กรจะถูกบันทึก คอลัมน์ร้านแสดงและเก็บผลรวมจาก HQ ถึงจุดขาย'
    },
    '기본 보류율 정책': {
      EN: 'Default hold (rolling) policy',
      JP: '基本留保率ポリシー',
      CH: '默认留存率政策',
      TH: 'นโยบายการกันวงเงินเริ่มต้น'
    },
    '가맹점 등록의 [보류율 설정]과 동일한 개념입니다. 승인(결제) 금액 중 롤링(담보금) 비율(%)만큼 보류하고, 설정한 보류 영업일 수가 지나면 정산 실행 시 지급액에 합산됩니다. 본사정책 따름(Y)이면 가맹점은 아래 본사 템플릿의 롤링 비율·일수를 따릅니다.': {
      EN: 'Same idea as merchant registration “hold rate”. A rolling (collateral) % of approved payment is held; after the configured business days, it is added back to payout on settlement runs. If the merchant follows HQ policy (Y), they use the rolling % and days from the HQ template below.',
      JP: '加盟店登録の「留保率設定」と同じ概念です。承認（決済）金額のうちローリング（担保）率(%)を留保し、設定した留保営業日数が経過すると精算実行時に支払額へ合算されます。本社ポリシーに従う(Y)の場合、加盟店は下の本社テンプレートのローリング率・日数に従います。',
      CH: '与商户注册的「留存率设置」概念相同。对授权（支付）金额按滚动（保证金）比例(%)留存；经过设定的留存营业日后，在结算执行时并入拨付额。若商户跟随总部政策(Y)，则采用下方总部模板的滚动比例与天数。',
      TH: 'เหมือนการตั้งค่า “อัตราการกัน” ตอนลงทะเบียนร้าน กันส่วนของยอดอนุมัติตาม % หลักประกัน (rolling) หลังครบวันทำการที่ตั้งไว้ จะรวมในยอดจ่ายเมื่อรันชำระ ถ้าร้านตามนโยบาย HQ (Y) จะใช้ % และจำนวนวันจากเทมเพลตด้านล่าง'
    },
    '롤링(담보금)비율(%)': {
      EN: 'Rolling (collateral) %',
      JP: 'ローリング（担保）率(%)',
      CH: '滚动（保证金）比例(%)',
      TH: 'อัตรา rolling (หลักประกัน) (%)'
    },
    '5 또는 10': {
      EN: '5 or 10',
      JP: '5 または 10',
      CH: '5 或 10',
      TH: '5 หรือ 10'
    },
    '롤링보류일수': {
      EN: 'Rolling hold (business days)',
      JP: 'ローリング留保営業日数',
      CH: '滚动留存营业日数',
      TH: 'วันทำการกัน rolling'
    },
    '120 또는 180': {
      EN: '120 or 180',
      JP: '120 または 180',
      CH: '120 或 180',
      TH: '120 หรือ 180'
    },
    '신규정책': {
      EN: 'New policy',
      JP: '新規ポリシー',
      CH: '新政策',
      TH: 'นโยบายใหม่'
    },
    '가맹점 수수료 정책': {
      EN: 'Merchant fee policies',
      JP: '加盟店手数料ポリシー',
      CH: '商户手续费政策',
      TH: 'นโยบายค่าธรรมเนียมร้านค้า'
    },
    '위 [저장] 후 목록이 갱신됩니다. 수치 열은 총본사~영업점 합계(가맹 적용분) 기준입니다. 체크 후 [수정] 또는 행 클릭으로 폼에 불러옵니다. [신규정책]으로 초기화한 뒤 입력·저장하면 코드가 자동 부여되어 목록에 나타납니다. 체크한 항목만 [선택 정책 삭제]할 수 있습니다(여러 건 가능). 표 머리의 체크박스로 전체 선택·해제합니다.': {
      EN: 'After [Save], the list refreshes. Amount columns are totals from root HQ through sales office (merchant-facing). Check one row and use [Edit], or click a row to load the form. Use [New policy] to reset, then enter and save to auto-assign a code and appear in the list. Checked rows can be removed with [Delete selected policies] (multi-select). The header checkbox selects or clears all.',
      JP: '上の[保存]後に一覧が更新されます。数値列は総本社〜営業店の合計（加盟店適用分）基準です。チェック後[修正]または行クリックでフォームに読み込みます。[新規ポリシー]で初期化して入力・保存するとコードが自動付与され一覧に表示されます。チェックした項目のみ[選択ポリシー削除]できます（複数可）。表頭のチェックボックスで全選択・解除します。',
      CH: '点击上方[保存]后列表会刷新。数值列为总本部至营业点合计（加盟适用部分）。勾选后[修改]或点击行可加载表单。[新政策]清空后填写并保存会自动分配代码并出现在列表。仅勾选项可用[删除所选政策]（可多选）。表头复选框可全选/清除。',
      TH: 'หลัง [บันทึก] ด้านบน รายการจะรีเฟรช คอลัมน์ตัวเลขเป็นผลรวม HQ ถึงจุดขาย (ส่วนที่ร้านได้รับ) เลือกแล้วกด [แก้ไข] หรือคลิกแถวเพื่อโหลดฟอร์ม ใช้ [นโยบายใหม่] เคลียร์แล้วกรอก·บันทึกจะได้รหัสอัตโนมัติและแสดงในรายการ รายการที่เลือกลบได้ด้วย [ลบนโยบายที่เลือก] (หลายรายการ) ช่องหัวตารางเลือก/ยกเลิกทั้งหมด'
    },
    '헤더 1행은 <strong>수수료 고정</strong>·<strong>수수료 %</strong>·<strong>담보율</strong>·<strong>기타</strong> 묶음입니다. <strong>수수료 %</strong> 열은 숫자만 표시(단위 % 생략). 결제·USDT·FX는 승인금액 기준 %이며, <strong>3DS</strong>는 정책통화 기준 <strong>건당 고정</strong>입니다. 담보(롤링) 비율은 승인금액 기준 %입니다. 열이 많아 표에 <strong>최소 너비</strong>를 두었으며, 화면이 좁으면 아래 표 영역을 <strong>가로 스크롤</strong>하여 전체 열을 볼 수 있습니다.': {
      EN: 'Row 1 groups <strong>fixed fees</strong>, <strong>fee %</strong>, <strong>collateral %</strong>, and <strong>other</strong>. <strong>Fee %</strong> cells show numbers only (no % sign). Pay, USDT, and FX are % of approved amount; <strong>3DS</strong> is a <strong>per-txn fixed</strong> fee in the policy currency. Rolling collateral % is % of approved amount. Many columns use a <strong>minimum width</strong>; on narrow screens, <strong>scroll horizontally</strong> in the table area to see all.',
      JP: 'ヘッダ1行は<strong>手数料固定</strong>・<strong>手数料%</strong>・<strong>担保率</strong>・<strong>その他</strong>のまとまりです。<strong>手数料%</strong>列は数値のみ表示（%記号省略）。決済・USDT・FXは承認金額基準の%、<strong>3DS</strong>はポリシー通貨基準の<strong>件当固定</strong>です。担保（ローリング）率は承認金額基準の%です。列が多いため表に<strong>最小幅</strong>を設けており、画面が狭いときは下の表領域を<strong>横スクロール</strong>して全列を確認できます。',
      CH: '第一行表头将<strong>固定手续费</strong>、<strong>手续费%</strong>、<strong>担保率</strong>与<strong>其他</strong>分组。<strong>手续费%</strong>列仅显示数字（省略%符号）。支付·USDT·FX 为授权金额基准的百分比；<strong>3DS</strong> 为政策货币下的<strong>每笔固定</strong>。滚动担保比例为授权金额基准的%。列较多，表格设有<strong>最小宽度</strong>；屏幕较窄时可在下方表区域<strong>横向滚动</strong>查看全部列。',
      TH: 'แถวหัวแรกรวม <strong>ค่าคงที่</strong>·<strong>% ค่าธรรมเนียม</strong>·<strong>% หลักประกัน</strong> และ <strong>อื่นๆ</strong> คอลัมน์ <strong>% ค่าธรรมเนียม</strong> แสดงตัวเลขอย่างเดียว (ไม่มีเครื่องหมาย %) การชำระ·USDT·FX เป็น % ของยอดอนุมัติ <strong>3DS</strong> เป็นค่า<strong>คงที่ต่อราย</strong>ตามสกุลนโยบาย อัตรา rolling เป็น % ของยอดอนุมัติ มีหลายคอลัมน์จึงตั้ง<strong>ความกว้างขั้นต่ำ</strong> หน้าจอแคบให้<strong>เลื่อนแนวนอน</strong>ในพื้นที่ตาราง'
    },
    '전체 선택': {
      EN: 'Select all',
      JP: 'すべて選択',
      CH: '全选',
      TH: 'เลือกทั้งหมด'
    },
    '선택': {
      EN: 'Select',
      JP: '選択',
      CH: '选择',
      TH: 'เลือก'
    },
    '코드': {
      EN: 'Code',
      JP: 'コード',
      CH: '代码',
      TH: 'รหัส'
    },
    '차지백': {
      EN: 'Chargeback',
      JP: 'チャージバック',
      CH: '拒付',
      TH: 'ชาร์จแบ็ก'
    },
    '구간정책': {
      EN: 'Tier policy',
      JP: '段階ポリシー',
      CH: '分段政策',
      TH: 'นโยบายแบบช่วง'
    },
    '적용': {
      EN: 'Status',
      JP: '適用',
      CH: '状态',
      TH: 'สถานะ'
    },
    '통화': {
      EN: 'CCY',
      JP: '通貨',
      CH: '货币',
      TH: 'สกุล'
    },
    '수수료 고정': {
      EN: 'Fixed fees',
      JP: '手数料固定',
      CH: '固定手续费',
      TH: 'ค่าธรรมเนียมคงที่'
    },
    '수수료 %': {
      EN: 'Fee %',
      JP: '手数料%',
      CH: '手续费%',
      TH: 'ค่าธรรมเนียม %'
    },
    '담보율': {
      EN: 'Collateral %',
      JP: '担保率',
      CH: '担保比例',
      TH: 'อัตราหลักประกัน'
    },
    '월간': {
      EN: 'Monthly',
      JP: '月次',
      CH: '月度',
      TH: 'รายเดือน'
    },
    '기타': {
      EN: 'Other',
      JP: 'その他',
      CH: '其他',
      TH: 'อื่นๆ'
    },
    '건당': {
      EN: 'Per txn',
      JP: '件当',
      CH: '每笔',
      TH: 'ต่อรายการ'
    },
    '정산': {
      EN: 'Settle',
      JP: '精算',
      CH: '结算',
      TH: 'ชำระ'
    },
    '송금': {
      EN: 'Remit',
      JP: '送金',
      CH: '汇款',
      TH: 'โอน'
    },
    'U송금': {
      EN: 'U remit',
      JP: 'U送金',
      CH: 'U汇款',
      TH: 'โอน U'
    },
    '수무효': {
      EN: 'Man. void',
      JP: '手無効',
      CH: '手动作废',
      TH: 'โมฆะมือ'
    },
    '결제': {
      EN: 'Pay',
      JP: '決済',
      CH: '支付',
      TH: 'ชำระ'
    },
    '비율': {
      EN: 'Rate',
      JP: '比率',
      CH: '比例',
      TH: 'อัตรา'
    },
    '일': {
      EN: 'Days',
      JP: '日',
      CH: '天',
      TH: 'วัน'
    },
    '기타1': {
      EN: 'Other 1',
      JP: 'その他1',
      CH: '其他1',
      TH: 'อื่น 1'
    },
    '기타2': {
      EN: 'Other 2',
      JP: 'その他2',
      CH: '其他2',
      TH: 'อื่น 2'
    },
    '기타3': {
      EN: 'Other 3',
      JP: 'その他3',
      CH: '其他3',
      TH: 'อื่น 3'
    },
    '기타4': {
      EN: 'Other 4',
      JP: 'その他4',
      CH: '其他4',
      TH: 'อื่น 4'
    },
    '등록된 템플릿이 없습니다. 위에서 [신규정책] 후 [저장]하세요.': {
      EN: 'No templates yet. Use [New policy] above, then [Save].',
      JP: '登録されたテンプレートがありません。上で[新規ポリシー]の後[保存]してください。',
      CH: '尚无模板。请先在上方使用[新政策]，再[保存]。',
      TH: 'ยังไม่มีเทมเพลต ใช้ [นโยบายใหม่] ด้านบน แล้ว [บันทึก]'
    },
    '정책 삭제': {
      EN: 'Delete policy',
      JP: 'ポリシー削除',
      CH: '删除政策',
      TH: 'ลบนโยบาย'
    },
    '선택 정책 삭제': {
      EN: 'Delete selected policies',
      JP: '選択ポリシーを削除',
      CH: '删除所选政策',
      TH: 'ลบนโยบายที่เลือก'
    },
    '정책 목록을 불러오지 못했습니다.': {
      EN: 'Could not load the policy list.',
      JP: 'ポリシー一覧を読み込めませんでした。',
      CH: '无法加载政策列表。',
      TH: 'โหลดรายการนโยบายไม่สำเร็จ'
    },
    '신규 정책 입력 모드입니다. 내용을 입력한 뒤 [저장]하면 코드가 자동 부여되고 목록에 반영됩니다.': {
      EN: 'New policy mode: enter details and [Save] to auto-assign a code and update the list.',
      JP: '新規ポリシー入力モードです。内容を入力し[保存]するとコードが自動付与され一覧に反映されます。',
      CH: '新政策输入模式：填写内容后[保存]将自动分配代码并更新列表。',
      TH: 'โหมดนโยบายใหม่: กรอกข้อมูลแล้ว [บันทึก] เพื่อกำหนดรหัสอัตโนมัติและอัปเดตรายการ'
    },
    '클릭하여 이 정책 불러오기': {
      EN: 'Click to load this policy',
      JP: 'クリックしてこのポリシーを読み込む',
      CH: '点击加载此政策',
      TH: 'คลิกเพื่อโหลดนโยบายนี้'
    },
    '행 선택': {
      EN: 'Select row',
      JP: '行を選択',
      CH: '选择行',
      TH: 'เลือกแถว'
    },
    '정책을 불러왔습니다. 수정 후 [저장]하세요.': {
      EN: 'Policy loaded. Edit and [Save].',
      JP: 'ポリシーを読み込みました。修正後[保存]してください。',
      CH: '已加载政策。请修改后[保存]。',
      TH: 'โหลดนโยบายแล้ว แก้ไขแล้ว [บันทึก]'
    },
    '수정할 정책을 목록에서 한 건 체크하세요.': {
      EN: 'Check exactly one policy in the list to edit.',
      JP: '修正するポリシーを一覧で1件だけチェックしてください。',
      CH: '请在列表中仅勾选一条要修改的政策。',
      TH: 'เลือกนโยบายหนึ่งรายการในรายการเพื่อแก้ไข'
    },
    '[수정]은 한 번에 한 건만 선택할 수 있습니다.': {
      EN: '[Edit] allows only one selection at a time.',
      JP: '[修正]は一度に1件のみ選択できます。',
      CH: '[修改]一次只能选择一项。',
      TH: '[แก้ไข] เลือกได้ครั้งละหนึ่งรายการเท่านั้น'
    },
    '저장되었습니다. 아래 목록이 갱신되었습니다.': {
      EN: 'Saved. The list below has been refreshed.',
      JP: '保存しました。下の一覧を更新しました。',
      CH: '已保存。下方列表已刷新。',
      TH: 'บันทึกแล้ว รีเฟรชรายการด้านล่างแล้ว'
    },
    '저장 또는 정책 추가에 실패했습니다.': {
      EN: 'Saving or adding the policy failed.',
      JP: '保存またはポリシー追加に失敗しました。',
      CH: '保存或添加政策失败。',
      TH: 'บันทึกหรือเพิ่มนโยบายล้มเหลว'
    },
    '신규 정책 입력 모드로 전환합니다. 계속하시겠습니까?': {
      EN: 'Switch to new policy entry mode. Continue?',
      JP: '新規ポリシー入力モードに切り替えます。続行しますか？',
      CH: '切换到新政策输入模式。是否继续？',
      TH: 'สลับไปโหมดนโยบายใหม่ ดำเนินการต่อหรือไม่'
    },
    '폼이 초기값으로 바뀝니다. 진행할까요?': {
      EN: 'The form will reset to defaults. Proceed?',
      JP: 'フォームが初期値に戻ります。進めますか？',
      CH: '表单将恢复为初始值。是否继续？',
      TH: 'ฟอร์มจะกลับเป็นค่าเริ่มต้น ดำเนินการต่อหรือไม่'
    },
    '삭제할 정책을 목록에서 체크하세요.': {
      EN: 'Check the policies to delete in the list.',
      JP: '削除するポリシーを一覧でチェックしてください。',
      CH: '请在列表中勾选要删除的政策。',
      TH: 'เลือกนโยบายที่จะลบในรายการ'
    },
    '{COUNT}건을 삭제 절차를 시작합니다. 삭제 확인 단계로 진행할까요?': {
      EN: 'Start the delete process for {COUNT} template(s). Proceed to confirmation?',
      JP: '{COUNT}件の削除手続きを開始します。確認に進みますか？',
      CH: '开始删除 {COUNT} 个模板的流程。是否进入确认？',
      TH: 'เริ่มลบ {COUNT} เทมเพลต ไปขั้นยืนยันหรือไม่'
    },
    '삭제는 되돌리기 어렵습니다. 계속하시겠습니까?': {
      EN: 'Deletion is hard to undo. Continue?',
      JP: '削除は元に戻しにくいです。続行しますか？',
      CH: '删除难以撤销。是否继续？',
      TH: 'การลบแก้คืนยาก ดำเนินการต่อหรือไม่'
    },
    '아래 {COUNT}건 템플릿을 삭제합니다. 배포 중이면 가맹점 기본 부여에 영향이 있을 수 있습니다.': {
      EN: 'The {COUNT} template(s) below will be deleted. If published, merchant defaults may be affected.',
      JP: '以下{COUNT}件のテンプレートを削除します。配布中の場合、加盟店の既定付与に影響する可能性があります。',
      CH: '将删除以下 {COUNT} 个模板。若已发布，可能影响商户默认分配。',
      TH: 'จะลบเทมเพลต {COUNT} รายการด้านล่าง หากเผยแพร่แล้วอาจกระทบค่าเริ่มต้นของร้าน'
    },
    '모달을 열 수 없어 바로 삭제 확인을 진행합니다. 선택한 {COUNT}건을 삭제할까요?': {
      EN: 'Cannot open the modal; continuing delete confirmation in place. Delete the selected {COUNT} template(s)?',
      JP: 'モーダルを開けないため、その場で削除確認に進みます。選択した{COUNT}件を削除しますか？',
      CH: '无法打开对话框，将就地继续删除确认。删除所选 {COUNT} 项？',
      TH: 'เปิดโมดัลไม่ได้ ดำเนินการยืนยันการลบต่อทันที ลบ {COUNT} รายการที่เลือกหรือไม่'
    },
    '삭제를 최종 확인합니다. 실행할까요?': {
      EN: 'Final confirmation: run the delete?',
      JP: '削除を最終確認します。実行しますか？',
      CH: '最终确认删除。是否执行？',
      TH: 'ยืนยันการลบขั้นสุดท้าย ดำเนินการหรือไม่'
    },
    '선택한 {COUNT}건 템플릿을 서버에서 영구 삭제합니다. 진행할까요?': {
      EN: 'Permanently delete the selected {COUNT} template(s) on the server. Proceed?',
      JP: '選択した{COUNT}件のテンプレートをサーバーから永続削除します。進めますか？',
      CH: '将从服务器永久删除所选 {COUNT} 个模板。是否继续？',
      TH: 'ลบถาวร {COUNT} เทมเพลตที่เลือกบนเซิร์ฟเวอร์ ดำเนินการต่อหรือไม่'
    },
    '삭제 후에는 복구할 수 없습니다. 정말 실행하시겠습니까?': {
      EN: 'This cannot be recovered after deletion. Really proceed?',
      JP: '削除後は復元できません。本当に実行しますか？',
      CH: '删除后无法恢复。确定执行？',
      TH: 'หลังลบกู้คืนไม่ได้ ยืนยันดำเนินการหรือไม่'
    },
    '선택한 정책이 삭제되었습니다.': {
      EN: 'Selected policies have been deleted.',
      JP: '選択したポリシーを削除しました。',
      CH: '所选政策已删除。',
      TH: 'ลบนโยบายที่เลือกแล้ว'
    },
    '정책 삭제 실패': {
      EN: 'Policy delete failed',
      JP: 'ポリシー削除に失敗しました',
      CH: '删除政策失败',
      TH: 'ลบนโยบายล้มเหลว'
    },
    '선택한 정책이 삭제되었습니다. 목록을 갱신했습니다.': {
      EN: 'Selected policies were deleted and the list was refreshed.',
      JP: '選択したポリシーを削除し、一覧を更新しました。',
      CH: '所选政策已删除并已刷新列表。',
      TH: 'ลบนโยบายที่เลือกแล้วและรีเฟรชรายการ'
    },
    /* /hq/chargebackPolicy — HQ chargeback tier policies */
    '차지백설정': {
      EN: 'Chargeback settings',
      JP: 'チャージバック設定',
      CH: '拒付（退单）设置',
      TH: 'ตั้งค่า chargeback'
    },
    '월간 환불·강제환불(거래 상태 30·31) 건수로 구간을 정합니다. 해당 월 누적 건수에 맞는 첫 구간의 건당 금액을, 정산 배치에 포함된 환불·강제환불 건수만큼 곱해 합산합니다. 구간 정책을 쓰지 않으면 [수수료설정]의 차지백수수료(건)만 적용됩니다.': {
      EN: 'Tiers are defined by monthly refund and forced-refund counts (txn statuses 30 and 31). For the month, the per-case amount from the first tier that matches cumulative count is multiplied by refund/forced-refund counts included in the settlement batch and summed. If no tier policy is used, only the per-txn chargeback fee from [Fee settings] applies.',
      JP: '月次の返金・強制返金（取引ステータス30・31）件数で段階を定めます。当月の累計件数に合致する最初の段階の件当金額を、精算バッチに含まれる返金・強制返金件数分だけ掛けて合算します。段階ポリシーを使わない場合は[手数料設定]のチャージバック手数料（件）のみが適用されます。',
      CH: '按月度退款与强制退款（交易状态 30、31）笔数划分区间。对应该月累计笔数命中的首个区间的每笔金额，乘以结算批次中包含的退款与强制退款笔数后汇总。若不使用区间政策，则仅适用[手续费设置]中的每笔拒付手续费。',
      TH: 'กำหนดช่วงจากจำนวนการคืนเงินและคืนบังคับรายเดือน (สถานะ 30·31) คูณจำนวนต่อเคสของช่วงแรกที่ตรงกับยอดสะสมของเดือนนั้น ด้วยจำนวนคืน/คืนบังคับในรอบชำระ หากไม่ใช้นโยบายช่วง จะใช้เฉพาะค่า chargeback ต่อรายจาก[ตั้งค่าค่าธรรมเนียม]'
    },
    '저장된 유형': {
      EN: 'Saved types',
      JP: '保存済みタイプ',
      CH: '已保存类型',
      TH: 'ประเภทที่บันทึกแล้ว'
    },
    '새 유형': {
      EN: 'New type',
      JP: '新規タイプ',
      CH: '新建类型',
      TH: 'ประเภทใหม่'
    },
    '편집': {
      EN: 'Edit',
      JP: '編集',
      CH: '编辑',
      TH: 'แก้ไข'
    },
    '예: 월간 차지백 단가표': {
      EN: 'e.g. Monthly chargeback tier table',
      JP: '例: 月次チャージバック単価表',
      CH: '例：月度拒付分段单价表',
      TH: 'เช่น ตาราง chargeback รายเดือน'
    },
    '구간 건당 금액의 표시·집계 단위 안내용입니다.': {
      EN: 'For display and aggregation of per-tier per-case amounts.',
      JP: '段階の件当金額の表示・集計単位の案内です。',
      CH: '用于区间每笔金额的展示与汇总单位说明。',
      TH: 'ใช้อธิบายหน่วยแสดงผลและสรุปยอดต่อเคสของแต่ละช่วง'
    },
    '내부 메모': {
      EN: 'Internal memo',
      JP: '内部メモ',
      CH: '内部备注',
      TH: 'บันทึกภายใน'
    },
    '구간 (해당 월 강제환불 31 건수)': {
      EN: 'Tiers (forced-refund code 31 count for the month)',
      JP: '段階（当月の強制返金31件数）',
      CH: '区间（当月强制退款 31 笔数）',
      TH: 'ช่วง (จำนวนคืนบังคับรหัส 31 ของเดือนนั้น)'
    },
    '행 추가': {
      EN: 'Add row',
      JP: '行を追加',
      CH: '添加行',
      TH: 'เพิ่มแถว'
    },
    'sort 오름차순으로 검사하며, 건수 ≥ 최소건 and (최대건 비움 = 상한 없음 or 건수 ≤ 최대건) 인 첫 행이 적용됩니다.': {
      EN: 'Tiers are checked in ascending sort order. The first row where the monthly count is ≥ min and (max is blank = no upper limit, or count ≤ max) applies.',
      JP: 'ソート昇順で各行を評価します。件数が最小件数以上かつ（最大件数が空欄＝上限なし、または件数が最大件数以下）である最初の行が適用されます。',
      CH: '按排序升序逐行检查：件数≥最小件数，且（最大件数留空表示无上限，或件数≤最大件数）时，首条匹配行生效。',
      TH: 'ตรวจตามลำดับ sort จากน้อยไปมาก แถวแรกที่จำนวน ≥ ค่าต่ำสุด และ (เว้นค่าสูงสุดว่าง = ไม่มีเพดานบน หรือ จำนวน ≤ ค่าสูงสุด) จะถูกนำไปใช้'
    },
    sort: {
      EN: 'Sort',
      JP: 'ソート',
      CH: '排序',
      TH: 'เรียง'
    },
    '최소건': {
      EN: 'Min count',
      JP: '最小件数',
      CH: '最小笔数',
      TH: 'ขั้นต่ำ (รายการ)'
    },
    '최대건': {
      EN: 'Max count',
      JP: '最大件数',
      CH: '最大笔数',
      TH: 'สูงสุด (รายการ)'
    },
    '건당금액': {
      EN: 'Per-case amount',
      JP: '件当金額',
      CH: '每笔金额',
      TH: 'จำนวนเงินต่อเคส'
    },
    '목록 새로고침': {
      EN: 'Refresh list',
      JP: '一覧を再読み込み',
      CH: '刷新列表',
      TH: 'รีเฟรชรายการ'
    },
    '무제한': {
      EN: 'No limit',
      JP: '上限なし',
      CH: '无上限',
      TH: 'ไม่จำกัด'
    },
    /* /hq/pgAgencyCostPolicy — 대행수수료설정 (ICOPAY↔PG contract) */
    '대행수수료설정': {
      EN: 'Agency fee settings',
      JP: '代行手数料設定',
      CH: '代理手续费设置',
      TH: 'ตั้งค่าค่าธรรมเนียมตัวแทน'
    },
    '노티·거래는 동일 저장소를 사용합니다. 여기서는 <strong>ICOPAY↔PG대행사 계약</strong> 관점의 수수료·담보·정산 주기(T+N 등)만 설정합니다. PG는 <strong>API연동설정</strong>에 등록된 PG코드별로 1건씩 저장합니다.': {
      EN: 'The same noti/transaction store is used. Configure <strong>ICOPAY↔PG agency contract</strong> fees, collateral, and settlement timing (T+N, etc.) only. One row per PG code from <strong>API integration</strong>.',
      JP: 'ノティ・取引は同一ストアです。<strong>ICOPAY↔PG代行契約</strong>の手数료・担保・精算のみ設定します。PGは<strong>API連携設定</strong>のPGコードごとに1件です。',
      CH: '通知与交易共用存储。仅配置 <strong>ICOPAY↔PG 代理商合同</strong> 手续费、保证金与结算周期。每个 API 联动中的 PG 代码一条。',
      TH: 'ใช้คลังเดียวกัน ตั้งค่า<strong>สัญญา ICOPAY↔PG</strong> ค่าธรรมเนียม หลักประกัน รอบชำระ เท่านั้น หนึ่งรายการต่อรหัส PG จาก<strong>การเชื่อม API</strong>'
    },
    '가맹 수수료(수수료설정)와 별도로, PG사와의 계약 원가·정산 주기를 PG코드(MID) 단위로 관리합니다. 운영 PG결제·PG정산 화면(예정)에서 이 정책을 참조합니다.': {
      EN: 'Separate from merchant fees ([Commission]). Manage PG contract cost and settlement per PG code (MID). Planned PG payment/settlement ops screens will reference this policy.',
      JP: '加盟店手数料（手数料設定）とは別に、PG契約原価・精算をPGコード（MID）単位で管理します。予定のPG決済・PG精算画面が参照します。',
      CH: '与商户手续费（手续费设置）分开，按 PG 代码（MID）管理 PG 合同成本与结算周期。计划中的 PG 支付/结算运营画面将引用此政策。',
      TH: 'แยกจากค่าธรรมเนียมร้าน ([Commission]) จัดการต้นทุนสัญญา PG และรอบชำระต่อรหัส PG (MID) หน้าจอ PG ชำระ/สรุป (ตามแผน) จะอ้างอิงนโยบายนี้'
    },
    '저장된 원가 정책': {
      EN: 'Saved cost policies',
      JP: '保存済み原価ポリシー',
      CH: '已保存成本政策',
      TH: 'นโยบายต้นทุนที่บันทึก'
    },
    '신규 PG': {
      EN: 'New PG',
      JP: '新規PG',
      CH: '新建 PG',
      TH: 'PG ใหม่'
    },
    '등록된 원가 정책이 없습니다.': {
      EN: 'No cost policies registered.',
      JP: '登録された原価ポリシーがありません。',
      CH: '尚无已登记的成本政策。',
      TH: 'ยังไม่มีนโยบายต้นทุน'
    },
    'PG 선택': {
      EN: 'Select PG',
      JP: 'PGを選択',
      CH: '选择 PG',
      TH: 'เลือก PG'
    },
    '신규 PG 원가를 입력한 뒤 저장하세요.': {
      EN: 'Enter a new PG cost policy and save.',
      JP: '新規PG原価を入力して保存してください。',
      CH: '请输入新的 PG 成本政策后保存。',
      TH: 'กรอกต้นทุน PG ใหม่แล้วบันทึก'
    },
    'PG대행사를 선택하세요.': {
      EN: 'Select a PG agency.',
      JP: 'PG代行を選択してください。',
      CH: '请选择 PG 代理商。',
      TH: 'เลือกตัวแทน PG'
    },
    '결제%': {
      EN: 'Pay %',
      JP: '決済%',
      CH: '支付%',
      TH: '%ชำระ'
    },
    '정산주기': {
      EN: 'Settlement cycle',
      JP: '精算サイクル',
      CH: '结算周期',
      TH: 'รอบชำระ'
    },
    '수수료(건당·%)': {
      EN: 'Fees (per txn / %)',
      JP: '手数料（件当・%）',
      CH: '手续费（每笔·%）',
      TH: 'ค่าธรรมเนียม (ต่อรายการ / %)'
    },
    '3DS(건)': {
      EN: '3DS (per txn)',
      JP: '3DS（件）',
      CH: '3DS（笔）',
      TH: '3DS (ต่อรายการ)'
    },
    'USDT송금(USD)': {
      EN: 'USDT transfer (USD)',
      JP: 'USDT送金(USD)',
      CH: 'USDT 汇款 (USD)',
      TH: 'โอน USDT (USD)'
    },
    'PG 정산 주기 (거래 시각 기준)': {
      EN: 'PG settlement timing (by txn time)',
      JP: 'PG精算サイクル（取引時刻基準）',
      CH: 'PG 结算周期（按交易时刻）',
      TH: 'รอบชำระ PG (ตามเวลาธุรกรรม)'
    },
    '기준은 항상 TRANSACTION(결제 시각)입니다. T=영업일 N일 후 동일 시각, H=24×N시간, D=달력 N일·일괄 시각.': {
      EN: 'Basis is always TRANSACTION (payment time). T = same clock time after N business days; H = 24×N hours; D = calendar N days at batch time.',
      JP: '基準は常にTRANSACTION（決済時刻）。T=営業日N日後同一時刻、H=24×N時間、D=暦日N日・一括時刻。',
      CH: '基准始终为 TRANSACTION（支付时刻）。T=营业日 N 日后同一时刻；H=24×N 小时；D=日历 N 日批量时刻。',
      TH: 'ฐานคือ TRANSACTION เสมอ T=เวลาเดิมหลัง N วันทำการ H=24×N ชม. D=ปฏิทิน N วันเวลารวม'
    },
    'D 일괄시각': {
      EN: 'D batch time',
      JP: 'D一括時刻',
      CH: 'D 批量时刻',
      TH: 'เวลารวม D'
    },
    '정책비고': {
      EN: 'Policy memo',
      JP: 'ポリシー備考',
      CH: '政策备注',
      TH: 'หมายเหตุนโยบาย'
    },
    '값': {
      EN: 'Value',
      JP: '値',
      CH: '数值',
      TH: 'ค่า'
    },
    '결제수수료(%)': {
      EN: 'Payment fee (%)',
      JP: '決済手数料(%)',
      CH: '支付手续费(%)',
      TH: 'ค่าธรรมเนียมชำระ (%)'
    },
    '무효(건)': {
      EN: 'Void (per txn)',
      JP: '無効（件）',
      CH: '作废（笔）',
      TH: 'โมฆะ (ต่อรายการ)'
    },
    '수동무효(건)': {
      EN: 'Manual void (per txn)',
      JP: '手動無効（件）',
      CH: '手动作废（笔）',
      TH: 'โมฆะด้วยมือ (ต่อรายการ)'
    },
    '환불(건)': {
      EN: 'Refund (per txn)',
      JP: '返金（件）',
      CH: '退款（笔）',
      TH: 'คืนเงิน (ต่อรายการ)'
    },
    '월이용료': {
      EN: 'Monthly usage fee',
      JP: '月額利用料',
      CH: '月使用费',
      TH: 'ค่าบริการรายเดือน'
    },
    '차지백(건)': {
      EN: 'Chargeback (per txn)',
      JP: 'チャージバック（件）',
      CH: '拒付（笔）',
      TH: 'Chargeback (ต่อรายการ)'
    },
    '롤링(%)': {
      EN: 'Rolling (%)',
      JP: 'ローリング(%)',
      CH: '滚动保证金(%)',
      TH: 'Rolling (%)'
    },
    '송금이체': {
      EN: 'Remittance transfer',
      JP: '送金振込',
      CH: '汇款转账',
      TH: 'โอนเงิน'
    },
    'USDT(%)': {
      EN: 'USDT (%)',
      JP: 'USDT(%)',
      CH: 'USDT(%)',
      TH: 'USDT (%)'
    },
    'FX(%)': {
      EN: 'FX (%)',
      JP: 'FX(%)',
      CH: 'FX(%)',
      TH: 'FX (%)'
    },
    '기준': {
      EN: 'Basis',
      JP: '基準',
      CH: '基准',
      TH: 'ฐาน'
    },
    'PG정산유형 T': {
      EN: 'T (business days, same time)',
      JP: 'T（営業日・同一時刻）',
      CH: 'T（营业日·同时刻）',
      TH: 'T (วันทำการ เวลาเดิม)'
    },
    'PG정산유형 H': {
      EN: 'H (24×N hours)',
      JP: 'H（24×N時間）',
      CH: 'H（24×N 小时）',
      TH: 'H (24×N ชม.)'
    },
    'PG정산유형 D': {
      EN: 'D (calendar days, batch time)',
      JP: 'D（暦日・一括時刻）',
      CH: 'D（日历日·批量时刻）',
      TH: 'D (ปฏิทิน เวลารวม)'
    },
    'PG코드를 선택하세요.': {
      EN: 'Select a PG code.',
      JP: 'PGコードを選択してください。',
      CH: '请选择 PG 代码。',
      TH: 'เลือกรหัส PG'
    },
    '등록된 PG대행사가 없습니다. API연동설정에서 먼저 등록하세요.': {
      EN: 'No PG agency registered. Register it first under API integration.',
      JP: '登録されたPG代行がありません。API連携設定で先に登録してください。',
      CH: '未登记 PG 代理商。请先在 API 联动设置中登记。',
      TH: 'ยังไม่มีตัวแทน PG ลงทะเบียนในการเชื่อม API ก่อน'
    },
    '정산 주기 유형은 T, H, D 중 하나여야 합니다.': {
      EN: 'Settlement schedule type must be T, H, or D.',
      JP: '精算サイクル種別は T、H、D のいずれかです。',
      CH: '结算周期类型须为 T、H 或 D。',
      TH: 'ประเภทรอบชำระต้องเป็น T, H หรือ D'
    },
    '정산 N은 1~30 사이여야 합니다.': {
      EN: 'Settlement N must be between 1 and 30.',
      JP: '精算 N は 1～30 です。',
      CH: '结算 N 须在 1～30 之间。',
      TH: 'ค่า N ต้องอยู่ระหว่าง 1–30'
    },
    'D(달력일) 모드는 일괄 시각(HH:mm)이 필요합니다.': {
      EN: 'D (calendar day) mode requires a batch time (HH:mm).',
      JP: 'D（暦日）モードは一括時刻（HH:mm）が必要です。',
      CH: 'D（日历日）模式需要批量时刻（HH:mm）。',
      TH: 'โหมด D ต้องระบุเวลารวม (HH:mm)'
    },
    /* HQ 영업일설정 / 미니달력 (screens.js, app.js, hq-holiday-calendar.js) */
    '{Y}년': {
      EN: '{Y}',
      JP: '{Y}年',
      CH: '{Y}年',
      TH: '{Y}'
    },
    '{Y}년 {M}월': {
      EN: '{Y}-{Mm}',
      JP: '{Y}年{M}月',
      CH: '{Y}年{M}月',
      TH: '{Mm}/{Y}'
    },
    달력요일_일: { KO: '일', EN: 'Su', JP: '日', CH: '日', TH: 'อา' },
    달력요일_월: { KO: '월', EN: 'Mo', JP: '月', CH: '一', TH: 'จ' },
    달력요일_화: { KO: '화', EN: 'Tu', JP: '火', CH: '二', TH: 'อ' },
    달력요일_수: { KO: '수', EN: 'We', JP: '水', CH: '三', TH: 'พ' },
    달력요일_목: { KO: '목', EN: 'Th', JP: '木', CH: '四', TH: 'พฤ' },
    달력요일_금: { KO: '금', EN: 'Fr', JP: '金', CH: '五', TH: 'ศ' },
    달력요일_토: { KO: '토', EN: 'Sa', JP: '土', CH: '六', TH: 'ส' },
    'API가 준비되지 않았습니다.': {
      EN: 'The API is not available.',
      JP: 'APIが利用できません。',
      CH: 'API 尚未就绪。',
      TH: 'API ยังไม่พร้อม'
    },
    '공휴일 불러오기 실패': {
      EN: 'Failed to load public holidays.',
      JP: '祝日の読み込みに失敗しました。',
      CH: '加载公共假日失败。',
      TH: 'โหลดวันหยุดราชการไม่สำเร็จ'
    },
    연도: { EN: 'Year', JP: '年', CH: '年份', TH: 'ปี' },
    '공휴일 프리셋 불러오기': {
      EN: 'Load holiday presets',
      JP: '祝日プリセットを読み込む',
      CH: '加载公共假日预设',
      TH: 'โหลดพรีเซ็ตวันหยุด'
    },
    '달력 동기화': {
      EN: 'Sync calendar',
      JP: 'カレンダーを同期',
      CH: '同步日历',
      TH: 'ซิงก์ปฏิทิน'
    },
    신규: { EN: 'New', JP: '新規', CH: '新建', TH: 'ใหม่' },
    '날짜를 클릭하면 비영업일에서 추가/제거됩니다. [공휴일 프리셋 불러오기]는 기준국가에 따라 병합합니다. KR/US/JP/TH/CN은 연도별 법정·공지 연휴, GLOBAL은 해당 연도 토·일만 포함합니다.': {
      EN: 'Click a date to add/remove it as a non-business day. [Load holiday presets] merges dates by the selected base country. KR/US/JP/TH/CN use year-specific legal/announced holidays; GLOBAL includes only Sat/Sun for that year.',
      JP: '日付をクリックすると非営業日として追加・解除できます。[祝日プリセットを読み込む]は基準国に応じて日付をマージします。KR/US/JP/TH/CNは年次の法定・公示休日、GLOBALはその年の土日のみを含みます。',
      CH: '点击日期可添加/移除非营业日。[加载公共假日预设]会按基准国家合并日期。KR/US/JP/TH/CN 为当年法定及公告连休；GLOBAL 仅含该年的周六日。',
      TH: 'คลิกวันที่เพื่อเพิ่ม/ถอนเป็นวันหยุดทำการ [โหลดพรีเซ็ตวันหยุด] จะรวมตามประเทศอ้างอิง KR/US/JP/TH/CN ใช้วันหยุดตามกฎหมาย/ประกาศรายปี GLOBAL มีเฉพาะเสาร์-อาทิตย์ของปีนั้น'
    },
    '휴일·비영업일 구간 등록': {
      EN: 'Register holiday / non-business day ranges',
      JP: '休日・非営業日の期間を登録',
      CH: '登记假日/非营业日区间',
      TH: 'ลงทะเบียนช่วงวันหยุด/วันหยุดทำการ'
    },
    '시작·종료일·구분·내용을 입력한 뒤 [구간 추가]로 넣거나, 목록의 [수정]으로 불러온 뒤 [수정 반영]으로 바꿉니다. [삭제]로 행을 제거할 수 있습니다. 하단 달력에 반영됩니다.': {
      EN: 'Enter start/end, category, and note, then add with [Add range], or use [Edit] on a row and [Apply edit] to update. [Delete] removes a row. Changes are reflected in the calendar below.',
      JP: '開始・終了・区分・内容を入力し、[区間を追加]で追加するか、一覧の[修正]で読み込み[修正を反映]で更新します。[削除]で行を削除できます。下のカレンダーに反映されます。',
      CH: '输入起止日、分类与说明后点[添加区间]，或在列表用[修改]载入后用[应用修改]更新。[删除]可移除行。下方日历会同步。',
      TH: 'กรอกเริ่ม/สิ้น/ประเภท/หมายเหตุ แล้วกด[เพิ่มช่วง] หรือกด[แก้ไข]ในตารางแล้วกด[บันทึกการแก้ไข] [ลบ]ลบแถวได้ ปฏิทินด้านล่างจะอัปเดต'
    },
    시작일: { EN: 'Start date', JP: '開始日', CH: '开始日', TH: 'วันเริ่ม' },
    종료일: { EN: 'End date', JP: '終了日', CH: '结束日', TH: 'วันสิ้น' },
    '일자 구분': {
      EN: 'Day category',
      JP: '日付区分',
      CH: '日期分类',
      TH: 'ประเภทวัน'
    },
    '예: 설날 연휴': {
      EN: 'e.g. Lunar New Year break',
      JP: '例: 旧正月連休',
      CH: '例：春节连休',
      TH: 'เช่น ช่วงตรุษจีน'
    },
    '연도-월-일': {
      EN: 'Year-month-day (YYYY-MM-DD)',
      JP: '年-月-日',
      CH: '年-月-日',
      TH: 'ปี-เดือน-วัน (YYYY-MM-DD)'
    },
    '구간 추가': {
      EN: 'Add range',
      JP: '区間を追加',
      CH: '添加区间',
      TH: 'เพิ่มช่วง'
    },
    '편집 취소': {
      EN: 'Cancel edit',
      JP: '編集をキャンセル',
      CH: '取消编辑',
      TH: 'ยกเลิกการแก้ไข'
    },
    '등록된 구간이 없습니다.': {
      EN: 'No ranges registered.',
      JP: '登録された区間がありません。',
      CH: '暂无已登记区间。',
      TH: 'ยังไม่มีช่วงที่ลงทะเบียน'
    },
    '저장된 영업일 설정 목록': {
      EN: 'Saved business-day profiles',
      JP: '保存済み営業日設定一覧',
      CH: '已保存的营业日设置列表',
      TH: 'รายการตั้งค่าวันทำการที่บันทึกแล้ว'
    },
    '행의 [수정]으로 불러오거나, 데이터 열을 눌러 선택할 수 있습니다.': {
      EN: 'Use [Edit] on a row to load it, or click a data row to select.',
      JP: '行の[修正]で読み込むか、データ行をクリックして選択できます。',
      CH: '可用行的[修改]载入，或点击数据行选择。',
      TH: 'กด[แก้ไข]ที่แถวเพื่อโหลด หรือคลิกแถวข้อมูลเพื่อเลือก'
    },
    '저장된 설정이 없습니다.': {
      EN: 'No saved settings.',
      JP: '保存された設定がありません。',
      CH: '暂无已保存设置。',
      TH: 'ยังไม่มีการตั้งค่าที่บันทึก'
    },
    시작: { EN: 'Start', JP: '開始', CH: '开始', TH: 'เริ่ม' },
    종료: { EN: 'End', JP: '終了', CH: '结束', TH: 'สิ้น' },
    구분: { EN: 'Category', JP: '区分', CH: '分类', TH: 'ประเภท' },
    등록자: { EN: 'Registered by', JP: '登録者', CH: '登记人', TH: 'ผู้ลงทะเบียน' },
    작성일: { EN: 'Created', JP: '作成日', CH: '创建日', TH: 'วันที่สร้าง' },
    수정일: { EN: 'Updated', JP: '更新日', CH: '更新日', TH: 'วันที่แก้ไข' },
    기준국가: { EN: 'Base country', JP: '基準国', CH: '基准国家', TH: 'ประเทศอ้างอิง' },
    공식공휴일: { EN: 'Official holidays', JP: '法定祝日', CH: '法定公休', TH: 'วันหยุดราชการ' },
    추가공휴일: { EN: 'Additional off-days', JP: '追加休日', CH: '额外休息日', TH: 'วันหยุดเพิ่ม' },
    총공휴일: { EN: 'Total off-days', JP: '休日合計', CH: '休息总日数', TH: 'วันหยุดรวม' },
    '저장된 비영업일 중 토·일·기준국가 법정(프리셋) 공휴일에 해당하는 일수.': {
      EN: 'Count of saved non-business days that are Sat/Sun or legal (preset) public holidays for the base country.',
      JP: '保存された非営業日のうち、土日および基準国の法定（プリセット）祝日に該当する日数。',
      CH: '已保存非营业日中，属于周末或基准国法定（预设）公假的天数。',
      TH: 'จำนวนวันหยุดทำการที่บันทึกไว้ซึ่งเป็นวันเสาร์-อาทิตย์หรือวันหยุดราชการตามประเทศอ้างอิง (พรีเซ็ต)'
    },
    '저장된 비영업일 중 위 공식에 해당하지 않는 일수(추가 지정 평일 등).': {
      EN: 'Saved non-business days that are not counted as official above (e.g. extra weekdays marked off).',
      JP: '上記の「法定」に含まれない保存済み非営業日（追加指定の平日など）。',
      CH: '不属于上述“法定”统计的已保存非营业日（如额外指定的工作日休息）。',
      TH: 'วันหยุดทำการที่บันทึกไว้ที่ไม่นับเป็นวันหยุดราชการข้างต้น (เช่น วันธรรมดาที่กำหนดเพิ่ม)'
    },
    '저장된 비영업 일자 수(중복 1회). 공식+추가와 일치.': {
      EN: 'Total saved non-business dates (each date once). Matches official + additional.',
      JP: '保存された非営業日数（日付の重複は1回）。公式+追加と一致。',
      CH: '已保存的非营业日期总数（同日只计一次）。与法定+额外一致。',
      TH: 'จำนวนวันหยุดทำการที่บันทึก (นับวันซ้ำครั้งเดียว) สอดคล้องกับราชการ+เพิ่มเติม'
    },
    공휴일: { EN: 'Public holiday', JP: '祝日', CH: '公共假日', TH: 'วันหยุดนักขัตฤกษ์' },
    국경일: { EN: 'National day', JP: '国民の祝日', CH: '国庆日', TH: 'วันชาติ' },
    기념일: { EN: 'Commemorative day', JP: '記念日', CH: '纪念日', TH: 'วันครบรอบ' },
    종교휴일: { EN: 'Religious holiday', JP: '宗教上の休日', CH: '宗教假日', TH: 'วันหยุดทางศาสนา' },
    임시공휴일: { EN: 'Ad hoc public holiday', JP: '臨時祝日', CH: '临时公休', TH: 'วันหยุดชั่วคราว' },
    대체공휴일: { EN: 'Substitute holiday', JP: '振替休日', CH: '调休公假', TH: 'วันหยุดชดเชย' },
    '영업일 설정': {
      EN: 'Business day settings',
      JP: '営業日設定',
      CH: '营业日设置',
      TH: 'ตั้งค่าวันทำการ'
    },
    '영업일 설정 이름': {
      EN: 'Business-day profile name',
      JP: '営業日設定名',
      CH: '营业日设置名称',
      TH: 'ชื่อโปรไฟล์วันทำการ'
    },
    'KR/US/JP/TH/CN 및 GLOBAL(토·일만 휴일) 기준으로 이름별 영업일 설정을 저장합니다. CN은 중국 국무원 공지 연휴(조정일 포함)를 반영합니다. 신규 저장 시 등록자(로그인 아이디)가 자동 기록됩니다. 업체(본사) 정보에서 영업일 설정 이름을 선택하면 해당 국가·휴일이 적용됩니다. 휴일 구간은 아래에서 추가하며, [공휴일 프리셋 불러오기]로 일자를 합칠 수 있습니다. 목록 집계: 공식공휴일=저장된 비영업일 중 토·일·해당국 법정(프리셋) 일자, 추가공휴일=그 외 저장 일자, 총공휴일=저장된 비영업 일수(공식+추가).': {
      EN: 'Save named business-day profiles by KR/US/JP/TH/CN or GLOBAL (Sat/Sun only). CN follows PRC State Council announced holidays (including adjusted workdays). On first save, the registrant (login ID) is recorded automatically. Selecting a profile name in company (HQ) info applies that country and holidays. Add holiday ranges below and merge dates with [Load holiday presets]. List columns: Official = saved non-business days that are weekend or legal (preset) for the country; Additional = other saved dates; Total = saved non-business day count (official + additional).',
      JP: 'KR/US/JP/TH/CN または GLOBAL（土日のみ休日）基準で、名前ごとの営業日設定を保存します。CNは中国国務院公示の連休（振替含む）を反映します。新規保存時は登録者（ログインID）が自動記録されます。会社（本社）情報で設定名を選ぶと、その国・祝日が適用されます。休日期間は下で追加し、[祝日プリセットを読み込む]で日付をマージできます。一覧集計：法定祝日＝保存された非営業日のうち土日・当該国の法定（プリセット）に該当する日、追加休日＝それ以外の保存日、合計＝保存された非営業日数（法定＋追加）。',
      CH: '按 KR/US/JP/TH/CN 或 GLOBAL（仅周六日为休）保存命名的营业日设置。CN 反映中国国务院公告的连休（含调休）。首次保存会自动记录登记人（登录ID）。在公司（总部）信息中选择设置名称即应用对应国家与假日。下方添加假日区间，可用[加载公共假日预设]合并日期。列表统计：法定＝已保存非营业日中属于周末或该国法定（预设）的日期；额外＝其余已保存日期；合计＝已保存非营业日总数（法定+额外）。',
      TH: 'บันทึกโปรไฟล์วันทำการตามชื่อด้วย KR/US/JP/TH/CN หรือ GLOBAL (หยุดเฉพาะเสาร์-อาทิตย์) CN สะท้อนวันหยุดตามประกาศรัฐบาลจีน (รวมวันชดเชย) เมื่อบันทึกครั้งแรกจะบันทึกผู้ลงทะเบียน (login ID) อัตโนมัติ เลือกชื่อตั้งค่าในข้อมูลบริษัท (สำนักงานใหญ่) เพื่อใช้ประเทศและวันหยุดนั้น เพิ่มช่วงวันหยุดด้านล่างและรวมวันที่ด้วย[โหลดพรีเซ็ตวันหยุด] คอลัมน์: ราชการ = วันหยุดทำการที่บันทึกซึ่งเป็นวันหยุดสุดสัปดาห์หรือวันหยุดตามกฎหมาย (พรีเซ็ต); เพิ่มเติม = วันที่บันทึกอื่น; รวม = จำนวนวันหยุดทำการที่บันทึก (ราชการ+เพิ่ม)'
    },
    기준국가선택: {
      EN: 'Base country',
      JP: '基準国の選択',
      CH: '基准国家',
      TH: 'เลือกประเทศอ้างอิง'
    },
    '예: KR 기본 영업일': {
      EN: 'e.g. KR default business days',
      JP: '例: KR 既定営業日',
      CH: '例：KR 默认营业日',
      TH: 'เช่น KR วันทำการเริ่มต้น'
    },
    'KR (대한민국)': { EN: 'KR (South Korea)', JP: 'KR（大韓民国）', CH: 'KR（韩国）', TH: 'KR (เกาหลีใต้)' },
    'US (미국)': { EN: 'US (United States)', JP: 'US（米国）', CH: 'US（美国）', TH: 'US (สหรัฐฯ)' },
    'JP (일본)': { EN: 'JP (Japan)', JP: 'JP（日本）', CH: 'JP（日本）', TH: 'JP (ญี่ปุ่น)' },
    'TH (태국)': { EN: 'TH (Thailand)', JP: 'TH（タイ）', CH: 'TH（泰国）', TH: 'TH (ไทย)' },
    'CN (중국)': { EN: 'CN (China)', JP: 'CN（中国）', CH: 'CN（中国）', TH: 'CN (จีน)' },
    'GLOBAL (토·일만 휴일)': {
      EN: 'GLOBAL (Sat/Sun only)',
      JP: 'GLOBAL（土日のみ休日）',
      CH: 'GLOBAL（仅周末为休）',
      TH: 'GLOBAL (หยุดเฉพาะเสาร์-อาทิตย์)'
    },
    '시작일을 선택하세요.': {
      EN: 'Please select a start date.',
      JP: '開始日を選択してください。',
      CH: '请选择开始日。',
      TH: 'กรุณาเลือกวันเริ่ม'
    },
    '이름을 입력하세요.': {
      EN: 'Please enter a name.',
      JP: '名前を入力してください。',
      CH: '请输入名称。',
      TH: 'กรุณากรอกชื่อ'
    },
    '영업일 설정 조회 실패': {
      EN: 'Failed to load business-day settings.',
      JP: '営業日設定の取得に失敗しました。',
      CH: '营业日设置查询失败。',
      TH: 'โหลดตั้งค่าวันทำการไม่สำเร็จ'
    },
    '수정 반영': {
      EN: 'Apply edit',
      JP: '修正を反映',
      CH: '应用修改',
      TH: 'บันทึกการแก้ไข'
    },
    '[1단계] 신규를 누르면 편집 중인 이름·기준국가·휴일 구간·달력에 반영된 데이터가 모두 초기화됩니다.\n진행하시겠습니까? (취소 시 아무 변화 없음)': {
      EN: '[Step 1] New will clear the name, base country, holiday ranges, and calendar data you are editing.\nContinue? (Cancel: no change)',
      JP: '[第1段階] 新規を押すと、編集中の名前・基準国・休日期間・カレンダーに反映されたデータがすべて初期化されます。\n続行しますか？（キャンセル：変更なし）',
      CH: '[第1步] 点击新建将清空正在编辑的名称、基准国家、假日区间及日历数据。\n是否继续？（取消：不变）',
      TH: '[ขั้นที่ 1] กดใหม่จะล้างชื่อ ประเทศอ้างอิง ช่วงวันหยุด และข้อมูลในปฏิทินที่กำลังแก้\nดำเนินต่อหรือไม่ (ยกเลิก: ไม่เปลี่ยนแปลง)'
    },
    '[2단계] 최종 확인: 모든 입력을 비우고 신규 작성 화면으로 전환합니다.\n정말 진행하시겠습니까?': {
      EN: '[Step 2] Final confirmation: all inputs will be cleared and you will switch to a blank new form.\nProceed?',
      JP: '[第2段階] 最終確認：入力をすべてクリアし、新規作成画面に切り替えます。\n本当に続行しますか？',
      CH: '[第2步] 最终确认：将清空所有输入并切换到新建界面。\n确定继续？',
      TH: '[ขั้นที่ 2] ยืนยันสุดท้าย: จะล้างข้อมูลทั้งหมดและเปลี่ยนเป็นหน้าสร้างใหม่\nดำเนินต่อจริงหรือไม่'
    },
    '[1단계] 현재 화면의 영업일 설정을 서버에 저장합니다.\n진행하시겠습니까? (취소 시 저장 안 함)': {
      EN: '[Step 1] Save the current business-day settings to the server.\nContinue? (Cancel: do not save)',
      JP: '[第1段階] 現在の営業日設定をサーバーに保存します。\n続行しますか？（キャンセル：保存しない）',
      CH: '[第1步] 将当前营业日设置保存到服务器。\n是否继续？（取消：不保存）',
      TH: '[ขั้นที่ 1] บันทึกตั้งค่าวันทำการปัจจุบันลงเซิร์ฟเวอร์\nดำเนินต่อหรือไม่ (ยกเลิก: ไม่บันทึก)'
    },
    '[2단계] 최종 확인: 저장하면 목록 및 적용 데이터가 갱신됩니다.\n저장하시겠습니까?': {
      EN: '[Step 2] Final confirmation: saving will refresh the list and applied data.\nSave?',
      JP: '[第2段階] 最終確認：保存すると一覧と適用データが更新されます。\n保存しますか？',
      CH: '[第2步] 最终确认：保存后将刷新列表与应用数据。\n是否保存？',
      TH: '[ขั้นที่ 2] ยืนยันสุดท้าย: บันทึกแล้วรายการและข้อมูลที่ใช้จะรีเฟรช\nบันทึกหรือไม่'
    },
    '[1단계] 영업일 설정 [{NAME}]을(를) 삭제합니다.\n진행하시겠습니까? (취소 시 삭제 안 함)': {
      EN: '[Step 1] Delete business-day profile [{NAME}].\nContinue? (Cancel: do not delete)',
      JP: '[第1段階] 営業日設定 [{NAME}] を削除します。\n続行しますか？（キャンセル：削除しない）',
      CH: '[第1步] 将删除营业日设置 [{NAME}]。\n是否继续？（取消：不删除）',
      TH: '[ขั้นที่ 1] จะลบโปรไฟล์วันทำการ [{NAME}]\nดำเนินต่อหรือไม่ (ยกเลิก: ไม่ลบ)'
    },
    '[2단계] 최종 확인: 삭제 후에는 복구할 수 없습니다.\n삭제하시겠습니까?': {
      EN: '[Step 2] Final confirmation: deletion cannot be undone.\nDelete?',
      JP: '[第2段階] 最終確認：削除後は復元できません。\n削除しますか？',
      CH: '[第2步] 最终确认：删除后无法恢复。\n是否删除？',
      TH: '[ขั้นที่ 2] ยืนยันสุดท้าย: ลบแล้วกู้คืนไม่ได้\nลบหรือไม่'
    },
    '총본사에서 상위 본사 영업일을 지정하여 이 총판의 영업일 설정은 상속 고정됩니다.': {
      EN: 'Head office has locked this distributor to the HQ business-day profile; settings here are inherited and fixed.',
      JP: '本社が上位本社の営業日を指定しているため、この総販の営業日設定は継承で固定されています。',
      CH: '总部已指定上级总部的营业日，本分销商的营业日设置继承且锁定。',
      TH: 'สำนักงานใหญ่กำหนดวันทำการของสำนักใหญ่ไว้ การตั้งค่าที่นี่ถูกล็อกตามการสืบทอด'
    },
    '언제부터 날짜를 입력하세요.': {
      EN: 'Please enter the “from” date.',
      JP: '「いつから」の日付を入力してください。',
      CH: '请输入“从”日期。',
      TH: 'กรุณากรอกวันที่เริ่ม'
    },
    '언제까지 날짜를 입력하세요.': {
      EN: 'Please enter the “to” date.',
      JP: '「いつまで」の日付を入力してください。',
      CH: '请输入“到”日期。',
      TH: 'กรุณากรอกวันที่สิ้นสุด'
    },
    '시작일은 종료일보다 클 수 없습니다.': {
      EN: 'Start date cannot be after end date.',
      JP: '開始日は終了日より後にできません。',
      CH: '开始日不能晚于结束日。',
      TH: 'วันเริ่มต้องไม่เกินวันสิ้น'
    },
    수정확인: {
      EN: 'Confirm edit',
      JP: '修正を確定',
      CH: '确认修改',
      TH: 'ยืนยันการแก้ไข'
    },
    '확인 완료: {FROM} ~ {TO}': {
      EN: 'Confirmed: {FROM} ~ {TO}',
      JP: '確認済み: {FROM} ～ {TO}',
      CH: '已确认：{FROM} ~ {TO}',
      TH: 'ยืนยันแล้ว: {FROM} ~ {TO}'
    },
    '해당 영업일 기간을 삭제하시겠습니까?': {
      EN: 'Delete this business-day range?',
      JP: 'この営業日期間を削除しますか？',
      CH: '要删除此营业日区间吗？',
      TH: 'ลบช่วงวันทำการนี้หรือไม่'
    },
    언제부터: { EN: 'From', JP: 'いつから', CH: '从', TH: 'ตั้งแต่' },
    언제까지: { EN: 'To', JP: 'いつまで', CH: '到', TH: 'ถึง' },
    '예: 설 연휴': {
      EN: 'e.g. Lunar New Year',
      JP: '例: 旧正月',
      CH: '例：春节',
      TH: 'เช่น ตรุษจีน'
    },
    작성자: { EN: 'Author', JP: '作成者', CH: '填写人', TH: 'ผู้เขียน' },
    추가한날짜: { EN: 'Added on', JP: '追加日', CH: '添加日期', TH: 'วันที่เพิ่ม' },
    처리: { EN: 'Actions', JP: '操作', CH: '操作', TH: 'ดำเนินการ' },
    '추가된 기간이 없습니다.': {
      EN: 'No ranges added yet.',
      JP: '追加された期間がありません。',
      CH: '尚未添加区间。',
      TH: 'ยังไม่มีช่วงที่เพิ่ม'
    },
    확인: { EN: 'OK', JP: '確認', CH: '确认', TH: 'ตกลง' },
    추가: { EN: 'Add', JP: '追加', CH: '添加', TH: 'เพิ่ม' },
    /* 본사권한설정 /hq/permissionMng (screens.js renderOrgPagePermissionShell, app.js matrix) */
    '조직 구분(총본사~가맹점)별로 메뉴(URL) 접근 권한을 설정합니다. <strong>총본사</strong>는 DB에 별도 저장이 없을 때 기본으로 <strong>모든 메뉴 전체 권한(삭제·전체)</strong>입니다. 각 대메뉴(본사설정·업체관리·배포설정 등) 구역 제목 오른쪽 <strong>간편</strong>에서 권한을 고르면 그 구역의 하위 메뉴가 한 번에 동일하게 맞춰집니다. <strong>옵저버</strong>는 조회만, <strong>수정</strong>은 쓰기·수정(삭제·일괄삭제 등 제한), <strong>삭제</strong>는 해당 화면의 삭제·수정·저장 등 모든 작업을 허용합니다. <strong>접근불가</strong>는 메뉴에서 숨깁니다. <strong>업체접근설정</strong>에 등록된 업체와 교집합으로 사용자관리 목록이 제한됩니다. 아래 <strong>담당자 권한그룹별 메뉴</strong>는 조직 최종 권한(상단 개별 조직 권한) 이내에서 관리/운영/정산/기술 담당 계정(ASSISTANT)의 메뉴를 한 단계 더 조입니다.': {
      EN: 'Set menu (URL) access by organization type (head office through merchant). <strong>Head office</strong> defaults to <strong>full access (delete / all)</strong> for every menu when nothing is stored in the DB. In each top-level section (HQ settings, company management, deploy settings, etc.), use <strong>Quick</strong> on the right of the section title to apply the same permission to all child menus at once. <strong>Observer</strong> is view-only; <strong>Edit</strong> allows write/edit with limits (delete / bulk delete, etc.); <strong>Delete</strong> allows all actions including delete, edit, and save on that screen. <strong>No access</strong> hides the menu from the tree. User management lists are intersected with companies registered in <strong>Company access settings</strong>. <strong>Assistant role menus</strong> below further narrow menus for MANAGER/OPERATOR/SETTLEMENT/TECH assistants (ASSISTANT), within the organization’s effective ceiling (per-org matrix above).',
      JP: '組織区分（本社～加盟店）ごとにメニュー（URL）のアクセス権を設定します。<strong>本社</strong>はDBに別途保存がない場合、既定で<strong>全メニュー・全権限（削除・全体）</strong>です。各大メニュー（本社設定・加盟店管理・デプロイ設定など）ブロック見出し右の<strong>一括</strong>で権限を選ぶと、その配下メニューに一括で同じ権限を適用します。<strong>閲覧のみ</strong>は参照のみ、<strong>修正</strong>は作成・変更（削除・一括削除など制限あり）、<strong>削除</strong>は当該画面の削除・変更・保存などすべて許可します。<strong>アクセス不可</strong>はメニューから非表示にします。<strong>加盟店アクセス設定</strong>に登録した加盟店との積集合でユーザー管理の一覧が絞られます。下の<strong>担当者権限グループ別メニュー</strong>は、組織の最終権限（上段の個別組織権限）の範囲内で、管理／運用／精算／技術の担当（ASSISTANT）向けメニューをさらに調整します。',
      CH: '按组织类型（总部～商户）设置各菜单（URL）访问权限。<strong>总部</strong>在数据库无单独记录时，默认对<strong>所有菜单拥有完整权限（含删除/全部）</strong>。各大区（本社设置、商户管理、部署设置等）标题右侧的<strong>快捷</strong>可一次性将同权限应用到该区下所有子菜单。<strong>只读</strong>仅可查看；<strong>修改</strong>可写入/编辑（删除、批量删除等受限）；<strong>删除</strong>允许该页全部操作（删、改、存等）。<strong>不可访问</strong>会在菜单中隐藏。用户管理列表会与<strong>商户访问设置</strong>中登记的公司取交集。下方的<strong>按担当权限组菜单</strong>在组织最终权限（上方各组织矩阵）范围内，进一步约束管理/运营/结算/技术担当（ASSISTANT）可见菜单。',
      TH: 'กำหนดสิทธิ์เมนู (URL) ตามประเภทองค์กร (สำนักใหญ่ถึงร้านค้า) <strong>สำนักงานใหญ่</strong> ถ้าไม่มีข้อมูลใน DB จะถือว่า<strong>ทุกเมนูมีสิทธิ์เต็ม (ลบ/ทั้งหมด)</strong> ในแต่ละกลุ่มเมนูหลัก (ตั้งค่า HQ, จัดการร้าน, ตั้งค่า deploy ฯลฯ) ใช้<strong>ทางลัด</strong>ทางขวาของหัวข้อเพื่อใส่สิทธิ์เดียวกันให้เมนูย่อยทั้งหมดในบล็อกนั้น <strong>ดูอย่างเดียว</strong> อ่านได้อย่างเดียว <strong>แก้ไข</strong> เขียน/แก้ (จำกัดการลบ/ลบหมู่ ฯลฯ) <strong>ลบ</strong> อนุญาตทุกอย่างรวมลบ/แก้/บันทึก <strong>ไม่มีสิทธิ์</strong> ซ่อนเมนู รายการจัดการผู้ใช้ตัดกับร้านใน<strong>ตั้งค่าการเข้าถึงร้าน</strong> <strong>เมนูตามกลุ่มสิทธิ์ผู้ช่วย</strong> ด้านล่างจำกัดเมนูสำหรับ MANAGER/OPERATOR/SETTLEMENT/TECH (ASSISTANT) ภายในเพดานสิทธิ์ขององค์กร (เมทริกซ์ด้านบน)'
    },
    '행 색:': { EN: 'Row color:', JP: '行の色:', CH: '行颜色：', TH: 'สีแถว:' },
    접근불가: { EN: 'No access', JP: 'アクセス不可', CH: '不可访问', TH: 'ไม่มีสิทธิ์' },
    옵저버: { EN: 'Observer', JP: '閲覧のみ', CH: '只读', TH: 'ดูอย่างเดียว' },
    '간편': { EN: 'Quick', JP: '一括', CH: '快捷', TH: 'ทางลัด' },
    '이 구역 일괄…': {
      EN: 'Bulk for this section…',
      JP: 'この区画を一括…',
      CH: '本区批量…',
      TH: 'ชุดนี้ทั้งหมด…'
    },
    '전체 · ': { EN: 'All · ', JP: '全体 · ', CH: '全部 · ', TH: 'ทั้งหมด · ' },
    '이 대메뉴 구역의 하위 메뉴에 동일 권한을 한 번에 적용합니다': {
      EN: 'Apply the same permission to all child menus in this top-level section at once.',
      JP: 'この大メニュー区画の配下メニューに同じ権限を一度に適用します。',
      CH: '将相同权限一次性应用到该大菜单区下的所有子菜单。',
      TH: 'ใช้สิทธิ์เดียวกันกับเมนูย่อยทั้งหมดในส่วนนี้ในครั้งเดียว'
    },
    '메뉴ID': { EN: 'Menu ID', JP: 'メニューID', CH: '菜单ID', TH: 'รหัสเมนู' },
    화면: { EN: 'Screen', JP: '画面', CH: '界面', TH: 'หน้าจอ' },
    권한: { EN: 'Permission', JP: '権限', CH: '权限', TH: 'สิทธิ์' },
    '담당자 권한': {
      EN: 'Assistant permission',
      JP: '担当者権限',
      CH: '担当权限',
      TH: 'สิทธิ์ผู้ช่วย'
    },
    '다시 불러오기': {
      EN: 'Reload',
      JP: '再読み込み',
      CH: '重新加载',
      TH: 'โหลดใหม่'
    },
    '서버에 저장된 단계별 기본 권한을 다시 불러옵니다(저장하지 않은 편집은 사라질 수 있습니다)': {
      EN: 'Reloads tier default permissions from the server (unsaved edits may be lost).',
      JP: 'サーバーに保存された段階別の既定権限を再読み込みします（未保存の編集は失われることがあります）。',
      CH: '从服务器重新加载各级默认权限（未保存的编辑可能会丢失）。',
      TH: 'โหลดค่าเริ่มต้นตามขั้นจากเซิร์ฟเวอร์ใหม่ (การแก้ที่ยังไม่บันทึกอาจหาย)'
    },
    '권한 저장': {
      EN: 'Save permissions',
      JP: '権限を保存',
      CH: '保存权限',
      TH: 'บันทึกสิทธิ์'
    },
    '개별 조직 권한': {
      EN: 'Per-organization permissions',
      JP: '個別組織の権限',
      CH: '各组织权限',
      TH: 'สิทธิ์ตามองค์กร'
    },
    '총본사~가맹점 <strong>각 조직</strong>을 선택해, 단계별 기본과 다른 권한을 둘 수 있습니다. <strong>단계 기본 따름</strong>이면 위 탭의 조직 구분 기준만 적용되고, <strong>개별 설정</strong>이면 아래 표에서만 덮어씁니다. 조직을 고르면 <strong>현재 적용되는 권한(최종)</strong>이 표시됩니다.': {
      EN: 'Pick each <strong>organization</strong> from head office through merchant to set permissions that differ from tier defaults. <strong>Follow tier default</strong> uses only the matrix in the tabs above; <strong>Custom</strong> overrides only in the table below. When you select an organization, the <strong>effective (final) permissions</strong> are shown.',
      JP: '本社～加盟店の<strong>各組織</strong>を選び、段階別の既定と異なる権限を設定できます。<strong>段階既定に従う</strong>の場合は上タブの組織区分マトリクスのみが適用され、<strong>個別設定</strong>の場合は下の表だけで上書きします。組織を選ぶと<strong>現在適用されている権限（最終）</strong>が表示されます。',
      CH: '选择<strong>各组织</strong>（总部～商户）以设置与阶段默认不同的权限。<strong>跟随阶段默认</strong>仅应用上方标签页的组织矩阵；<strong>单独设置</strong>仅在下方表格覆盖。选择组织后显示<strong>当前生效（最终）权限</strong>。',
      TH: 'เลือก<strong>แต่ละองค์กร</strong>จากสำนักใหญ่ถึงร้าน เพื่อตั้งสิทธิ์ที่ต่างจากค่าเริ่มต้นตามขั้น <strong>ตามค่าเริ่มต้นของขั้น</strong> ใช้เฉพาะเมทริกซ์แท็บด้านบน <strong>กำหนดเอง</strong> เขียนทับเฉพาะในตารางด้านล่าง เมื่อเลือกองค์กรจะแสดง<strong>สิทธิ์ที่ใช้จริง (สุดท้าย)</strong>'
    },
    '조직구분': { EN: 'Org type', JP: '組織区分', CH: '组织类型', TH: 'ประเภทองค์กร' },
    '현재방식': { EN: 'Current mode', JP: '現在の方式', CH: '当前方式', TH: 'โหมดปัจจุบัน' },
    '적용방식': { EN: 'Apply mode', JP: '適用方式', CH: '应用方式', TH: 'โหมดที่ใช้' },
    '단계 기본 따름': {
      EN: 'Follow tier default',
      JP: '段階既定に従う',
      CH: '跟随阶段默认',
      TH: 'ตามค่าเริ่มต้นของขั้น'
    },
    '개별 설정': {
      EN: 'Custom',
      JP: '個別設定',
      CH: '单独设置',
      TH: 'กำหนดเอง'
    },
    설정저장: {
      EN: 'Save settings',
      JP: '設定を保存',
      CH: '保存设置',
      TH: 'บันทึกการตั้งค่า'
    },
    '조직을 선택하면 적용 방식과 권한 표가 채워집니다.': {
      EN: 'Select an organization to fill apply mode and the permission table.',
      JP: '組織を選ぶと適用方式と権限表が表示されます。',
      CH: '选择组织后将填充应用方式与权限表。',
      TH: 'เลือกองค์กรเพื่อเติมโหมดการใช้และตารางสิทธิ์'
    },
    '— 업체를 선택하세요 —': {
      EN: '— Select a company —',
      JP: '— 加盟店を選択 —',
      CH: '— 请选择公司 —',
      TH: '— เลือกบริษัท —'
    },
    '담당자 권한그룹별 메뉴 (조직 상한 내)': {
      EN: 'Assistant role menus (within org ceiling)',
      JP: '担当者権限グループ別メニュー（組織上限内）',
      CH: '按担当权限组菜单（在组织上限内）',
      TH: 'เมนูตามกลุ่มสิทธิ์ผู้ช่วย (ภายในเพดานองค์กร)'
    },
    '위에서 조직을 선택하면, 해당 조직에 <strong>접근 가능한 메뉴</strong>만 표시됩니다. 값을 <strong>조직 기본(상한)</strong>으로 두면 담당자에게도 조직과 동일한 권한이 적용됩니다. 본사·총판·총본사는 자기 조직만 저장할 수 있습니다.': {
      EN: 'After you select an organization above, only menus <strong>accessible to that org</strong> are shown. Leave the value as <strong>Org default (ceiling)</strong> to give assistants the same permission as the org. Regional / distributor / head office can save only their own org.',
      JP: '上で組織を選ぶと、その組織が<strong>アクセス可能なメニュー</strong>だけが表示されます。値を<strong>組織既定（上限）</strong>にすると担当者にも組織と同じ権限が適用されます。本社・総販・本社は自組織のみ保存できます。',
      CH: '在上方选择组织后，仅显示该组织<strong>可访问的菜单</strong>。值设为<strong>组织默认（上限）</strong>时，担当也将获得与组织相同的权限。本社/总贩/总部仅能保存自身组织。',
      TH: 'เมื่อเลือกองค์กรด้านบน จะแสดงเฉพาะเมนูที่องค์กรนั้น<strong>เข้าถึงได้</strong> หากตั้งเป็น<strong>ค่าเริ่มต้นขององค์กร (เพดาน)</strong> ผู้ช่วยจะได้สิทธิ์เดียวกับองค์กร สำนักงานใหญ่/ตัวแทน/สำนักใหญ่บันทึกได้เฉพาะองค์กรตนเอง'
    },
    '권한그룹 저장': {
      EN: 'Save role groups',
      JP: '権限グループを保存',
      CH: '保存权限组',
      TH: 'บันทึกกลุ่มสิทธิ์'
    },
    '옵저버(조회만)': {
      EN: 'Observer (view only)',
      JP: '閲覧のみ',
      CH: '只读（仅查看）',
      TH: 'ดูอย่างเดียว (อ่านอย่างเดียว)'
    },
    '수정(삭제제한)': {
      EN: 'Edit (delete limited)',
      JP: '修正（削除制限あり）',
      CH: '修改（限制删除）',
      TH: 'แก้ไข (จำกัดการลบ)'
    },
    '삭제(전체)': {
      EN: 'Delete (full)',
      JP: '削除（全体）',
      CH: '删除（全部）',
      TH: 'ลบ (เต็มสิทธิ์)'
    },
    '태블릿설정': {
      EN: 'Tablet settings',
      JP: 'タブレット設定',
      CH: '平板设置',
      TH: 'ตั้งค่าแท็บเล็ต'
    },
    '사용자설정': {
      EN: 'User defaults',
      JP: 'ユーザー設定',
      CH: '用户设置',
      TH: 'ค่าผู้ใช้'
    },
    '업체접근설정': {
      EN: 'Company access',
      JP: '加盟店アクセス設定',
      CH: '商户访问设置',
      TH: 'ตั้งค่าการเข้าถึงร้าน'
    },
    본사설정: {
      EN: 'HQ settings',
      JP: '本社設定',
      CH: '总部设置',
      TH: 'ตั้งค่า HQ'
    },
    업체관리: {
      EN: 'Company management',
      JP: '加盟店管理',
      CH: '企业管理',
      TH: 'จัดการบริษัท'
    },
    결제관리: {
      EN: 'Payment management',
      JP: '決済管理',
      CH: '支付管理',
      TH: 'จัดการการชำระเงิน'
    },
    정산관리: {
      EN: 'Settlement management',
      JP: '精算管理',
      CH: '结算管理',
      TH: 'จัดการการชำระรอบ'
    },
    통보관리: {
      EN: 'Notification management',
      JP: '通知管理',
      CH: '通知管理',
      TH: 'จัดการการแจ้งเตือน'
    },
    사용자관리: {
      EN: 'User management',
      JP: 'ユーザー管理',
      CH: '用户管理',
      TH: 'จัดการผู้ใช้'
    },
    운영관리: {
      EN: 'Operations management',
      JP: '運用管理',
      CH: '运营管理',
      TH: 'จัดการปฏิบัติการ'
    },
    리스크관리: {
      EN: 'Risk management',
      JP: 'リスク管理',
      CH: '风险管理',
      TH: 'จัดการความเสี่ยง'
    },
    배포설정: {
      EN: 'Deploy settings',
      JP: 'デプロイ設定',
      CH: '部署设置',
      TH: 'ตั้งค่า deploy'
    },
    챗봇관리: {
      EN: 'Chatbot management',
      JP: 'チャットボット管理',
      CH: '聊天机器人管理',
      TH: 'จัดการ Chatbot'
    },
    기타: { EN: 'Other', JP: 'その他', CH: '其他', TH: 'อื่นๆ' },
    '조직 단계별 기본 권한은 <strong>총본사</strong>(또는 시스템 관리자)만 편집합니다.': {
      EN: 'Tier default permissions can be edited only by the <strong>head office</strong> (or a system administrator).',
      JP: '段階別の既定権限は<strong>本社</strong>（またはシステム管理者）のみが編集できます。',
      CH: '各级默认权限仅可由<strong>总部</strong>（或系统管理员）编辑。',
      TH: 'ค่าเริ่มต้นตามขั้นแก้ได้เฉพาะ<strong>สำนักงานใหญ่</strong> (หรือผู้ดูแลระบบ)'
    },
    '화면을 불러오지 못했습니다. 탭을 닫았다가 다시 열거나 새로고침 후 시도하세요.': {
      EN: 'Could not load the screen. Close the tab and reopen, or refresh and try again.',
      JP: '画面を読み込めませんでした。タブを閉じて再度開くか、更新してからお試しください。',
      CH: '无法加载界面。请关闭标签页后重开，或刷新后再试。',
      TH: 'โหลดหน้าจอไม่สำเร็จ ปิดแท็บแล้วเปิดใหม่ หรือรีเฟรชแล้วลองอีกครั้ง'
    },
    '권한 설정을 불러오지 못했습니다.': {
      EN: 'Could not load permission settings.',
      JP: '権限設定を読み込めませんでした。',
      CH: '无法加载权限设置。',
      TH: 'โหลดตั้งค่าสิทธิ์ไม่สำเร็จ'
    },
    '응답 시간이 초과되었습니다. PG START로 서버를 재시작한 뒤 다시 시도하세요.': {
      EN: 'The request timed out. Restart the server with PG START and try again.',
      JP: '応答がタイムアウトしました。PG STARTでサーバーを再起動してから再度お試しください。',
      CH: '响应超时。请用 PG START 重启服务器后重试。',
      TH: 'หมดเวลารอ รีสตาร์ทเซิร์ฟเวอร์ด้วย PG START แล้วลองใหม่'
    },
    '저장할 데이터가 없습니다.': {
      EN: 'Nothing to save.',
      JP: '保存するデータがありません。',
      CH: '没有可保存的数据。',
      TH: 'ไม่มีข้อมูลให้บันทึก'
    },
    '서버에 저장된 단계별 기본 권한을 다시 불러옵니다. 저장하지 않은 편집은 취소됩니다. 계속할까요?': {
      EN: 'Reload tier default permissions from the server. Unsaved edits will be discarded. Continue?',
      JP: 'サーバーに保存された段階別の既定権限を再読み込みします。未保存の編集は破棄されます。続行しますか？',
      CH: '从服务器重新加载各级默认权限，未保存的编辑将放弃。是否继续？',
      TH: 'โหลดค่าเริ่มต้นตามขั้นจากเซิร์ฟเวอร์ใหม่ การแก้ที่ยังไม่บันทึกจะถูกยกเลิก ดำเนินต่อหรือไม่'
    },
    '불러오면 편집 중인 내용이 사라집니다. 정말 진행할까요?': {
      EN: 'Reloading will discard your in-progress edits. Proceed?',
      JP: '読み込むと編集中の内容が失われます。本当に続行しますか？',
      CH: '重新加载将丢失正在编辑的内容。确定继续？',
      TH: 'การโหลดใหม่จะล้างการแก้ที่ยังไม่เสร็จ ดำเนินต่อจริงหรือไม่'
    },
    '관리(MANAGER)': {
      EN: 'Manager (MANAGER)',
      JP: '管理 (MANAGER)',
      CH: '管理 (MANAGER)',
      TH: 'ผู้จัดการ (MANAGER)'
    },
    '운영(OPERATOR)': {
      EN: 'Operations (OPERATOR)',
      JP: '運用 (OPERATOR)',
      CH: '运营 (OPERATOR)',
      TH: 'ปฏิบัติการ (OPERATOR)'
    },
    '정산(SETTLEMENT)': {
      EN: 'Settlement (SETTLEMENT)',
      JP: '精算 (SETTLEMENT)',
      CH: '结算 (SETTLEMENT)',
      TH: 'ชำระรอบ (SETTLEMENT)'
    },
    '기술(TECH)': {
      EN: 'Technical (TECH)',
      JP: '技術 (TECH)',
      CH: '技术 (TECH)',
      TH: 'เทคนิค (TECH)'
    },
    '조직 기본(상한)': {
      EN: 'Org default (ceiling)',
      JP: '組織既定（上限）',
      CH: '组织默认（上限）',
      TH: 'ค่าเริ่มต้นขององค์กร (เพดาน)'
    },
    '조직 상한: ': {
      EN: 'Org ceiling: ',
      JP: '組織上限: ',
      CH: '组织上限：',
      TH: 'เพดานองค์กร: '
    },
    '이 조직에서는 접근 가능한 메뉴가 없습니다.': {
      EN: 'No accessible menus for this organization.',
      JP: 'この組織でアクセス可能なメニューはありません。',
      CH: '该组织没有可访问的菜单。',
      TH: 'องค์กรนี้ไม่มีเมนูที่เข้าถึงได้'
    },
    '개별 설정이 저장되어 있습니다. 아래는 <strong>로그인 시 적용되는 최종 권한</strong>입니다.': {
      EN: '<strong>Custom</strong> settings are saved. Below is the <strong>effective permission at login</strong>.',
      JP: '<strong>個別設定</strong>が保存されています。以下は<strong>ログイン時に適用される最終権限</strong>です。',
      CH: '已保存<strong>单独设置</strong>。下方为<strong>登录时生效的最终权限</strong>。',
      TH: 'บันทึก<strong>กำหนดเอง</strong>ไว้ ด้านล่างคือ<strong>สิทธิ์สุดท้ายเมื่อล็อกอิน</strong>'
    },
    '단계 기본 따름 — 아래는 해당 조직 단계의 <strong>기본 매트릭스와 동일한 적용 결과</strong>입니다.': {
      EN: '<strong>Follow tier default</strong> — below matches the <strong>same effective result as the tier default matrix</strong> for this org level.',
      JP: '<strong>段階既定に従う</strong> — 以下は当該組織段階の<strong>既定マトリクスと同じ適用結果</strong>です。',
      CH: '<strong>跟随阶段默认</strong> — 下方与该组织阶段的<strong>默认矩阵应用结果一致</strong>。',
      TH: '<strong>ตามค่าเริ่มต้นของขั้น</strong> — ด้านล่างตรงกับ<strong>ผลลัพธ์เดียวกับเมทริกซ์เริ่มต้นของขั้นนี้</strong>'
    },
    '단계 기본 따름(저장 시 개별 덮어쓰기가 제거됩니다). 미리보기는 기본 매트릭스와 동일합니다.': {
      EN: '<strong>Follow tier default</strong> (saving removes per-org overrides). Preview matches the default matrix.',
      JP: '<strong>段階既定に従う</strong>（保存時に個別上書きが削除されます）。プレビューは既定マトリクスと同じです。',
      CH: '<strong>跟随阶段默认</strong>（保存时将移除单独覆盖）。预览与默认矩阵一致。',
      TH: '<strong>ตามค่าเริ่มต้นของขั้น</strong> (บันทึกแล้วจะลบการเขียนทับรายองค์กร) ตัวอย่างเหมือนเมทริกซ์เริ่มต้น'
    },
    '개별 설정 — 아래에서 수정 후 상단 [설정저장]을 누르세요.': {
      EN: '<strong>Custom</strong> — edit below, then press [Save settings] at the top.',
      JP: '<strong>個別設定</strong> — 下で修正したら上の［設定を保存］を押してください。',
      CH: '<strong>单独设置</strong> — 在下方修改后点击顶部的[保存设置]。',
      TH: '<strong>กำหนดเอง</strong> — แก้ด้านล่างแล้วกด [บันทึกการตั้งค่า] ด้านบน'
    },
    '담당자 권한그룹별 메뉴를 저장하시겠습니까?': {
      EN: 'Save assistant role menus?',
      JP: '担当者権限グループ別メニューを保存しますか？',
      CH: '要保存按担当权限组的菜单吗？',
      TH: 'บันทึกเมนูตามกลุ่มสิทธิ์ผู้ช่วยหรือไม่'
    },
    '조직을 선택하세요.': {
      EN: 'Select an organization.',
      JP: '組織を選択してください。',
      CH: '请选择组织。',
      TH: 'กรุณาเลือกองค์กร'
    },
    /* /hq/userSettings — 로그인·OTP·비밀번호 정책 (tb_hq_notify_env_config 동일 API) */
    '로그인·OTP 정책 (ziobiz/NOTI 계정관리 대응)': {
      EN: 'Login & OTP policy (ziobiz / NOTI account management)',
      JP: 'ログイン・OTP方針（ziobiz / NOTI アカウント管理対応）',
      CH: '登录与 OTP 策略（对应 ziobiz/NOTI 账户管理）',
      TH: 'นโยบายล็อกอินและ OTP (รองรับบัญชี ziobiz/NOTI)'
    },
    '모든 사용자에 OTP를 요구할지 본사(총본사) 설정에서 통일합니다. OTP 필수 시 로그인·등록 단계에서 OTP 검증을 붙일 수 있습니다(연동 예정). 사용자관리 그리드의 OTP 등록 여부와 연계됩니다. 저장은 노티·결제환경 설정(tb_hq_notify_env_config)과 동일 API를 사용합니다.': {
      EN: 'Whether OTP is required for all users is controlled centrally at HQ (root HQ). When OTP is mandatory, OTP verification can be attached at login and registration (integration planned). This links to the OTP enrollment column in User management. Saving uses the same API as Notification & payment environment settings (<code>tb_hq_notify_env_config</code>).',
      JP: '全ユーザーにOTPを必須にするかは本社（総本部）設定で統一します。OTP必須時はログイン・登録段階でOTP検証を付与できます（連携予定）。ユーザー管理グリッドのOTP登録有無と連動します。保存はノティ・決済環境設定（<code>tb_hq_notify_env_config</code>）と同一APIです。',
      CH: '是否对所有用户强制 OTP 由总部（总总部）统一设置。OTP 为必填时，可在登录与注册环节附加 OTP 验证（对接待定）。与用户管理网格中的 OTP 登记状态联动。保存与通知·支付环境设置（<code>tb_hq_notify_env_config</code>）使用相同 API。',
      TH: 'บังคับ OTP ทุกผู้ใช้หรือไม่กำหนดที่สำนักงานใหญ่ (สำนักใหญ่) เมื่อ OTP บังคับ สามารถผูกการยืนยัน OTP ตอนล็อกอิน/ลงทะเบียน (เชื่อมต่อตามแผน) เชื่อมกับคอลัมน์ OTP ในการจัดการผู้ใช้ การบันทึกใช้ API เดียวกับการตั้งค่าแจ้งเตือน·สภาพแวดล้อมการชำระ (<code>tb_hq_notify_env_config</code>)'
    },
    'OTP 사용 필수': {
      EN: 'OTP required',
      JP: 'OTP必須',
      CH: '强制 OTP',
      TH: 'บังคับ OTP'
    },
    '예 (전 사용자)': {
      EN: 'Yes (all users)',
      JP: 'はい（全ユーザー）',
      CH: '是（全体用户）',
      TH: 'ใช่ (ทุกผู้ใช้)'
    },
    'OTP 형식 정책': {
      EN: 'OTP format policy',
      JP: 'OTP形式ポリシー',
      CH: 'OTP 格式策略',
      TH: 'นโยบายรูปแบบ OTP'
    },
    'NOTI 동일': {
      EN: 'Same as NOTI',
      JP: 'NOTIと同じ',
      CH: '与 NOTI 相同',
      TH: 'เหมือน NOTI'
    },
    '커스텀': { EN: 'Custom', JP: 'カスタム', CH: '自定义', TH: 'กำหนดเอง' },
    '비밀번호 정책': {
      EN: 'Password policy',
      JP: 'パスワード方針',
      CH: '密码策略',
      TH: 'นโยบายรหัสผ่าน'
    },
    '비밀번호찾기 기능': {
      EN: 'Forgot-password feature',
      JP: 'パスワード忘れ機能',
      CH: '找回密码功能',
      TH: 'ฟีเจอร์ลืมรหัสผ่าน'
    },
    '관리담당 사용자관리 권한': {
      EN: 'Managers: user management permission',
      JP: '管理者向け：ユーザー管理権限',
      CH: '管理员：用户管理权限',
      TH: 'ผู้ดูแล: สิทธิ์จัดการผู้ใช้'
    },
    '관리담당 비밀번호 초기화': {
      EN: 'Managers: password reset',
      JP: '管理者向け：パスワード初期化',
      CH: '管理员：密码重置',
      TH: 'ผู้ดูแล: รีเซ็ตรหัสผ่าน'
    },
    /* /hq/userSettings — 담당자(보조) 메뉴 기본 권한 */
    '담당자(보조) 메뉴 기본 권한': {
      EN: 'Assistant role default menu permissions',
      JP: '担当者（補助）メニュー既定権限',
      CH: '担当人（辅助）菜单默认权限',
      TH: 'สิทธิ์เมนูเริ่มต้นของผู้ช่วย'
    },
    '카탈로그의 일반(데스크톱) 메뉴(URL)를 조직 단계(총본사~가맹점)별로 담당자 역할 상한을 둡니다. 태블릿 전용 메뉴는 아래 「태블릿모드」에서 설정합니다. 상단에서 편집할 조직 단계를 고른 뒤 표를 수정합니다. 본사권한설정의 개별 조직 「담당자 권한그룹별 메뉴」 저장값이 여기 기본값보다 우선합니다.': {
      EN: 'Set permission ceilings for desktop catalog menus (URLs) by organization level (headquarters through merchant) for each assistant role. Tablet-only menus are configured in the Tablet mode section below. Choose the edit level above, then adjust the table. Per-organization assistant group menus in HQ permissions override these defaults.',
      JP: 'カタログの一般（デスクトップ）メニュー(URL)について、組織段階（総本社〜加盟店）ごとに担当者ロールの上限を設定します。タブレット専用メニューは下の「タブレットモード」で設定します。上部で編集する組織段階を選び表を修正します。本社権限設定の個別組織「担当者権限グループ別メニュー」保存値がここより優先されます。',
      CH: '按组织级别（总总部至商户）为各担当角色设置桌面目录菜单(URL)权限上限。平板专用菜单在下方「平板模式」中设置。请在上方选择要编辑的组织级别后修改表格。总部权限设置中各组织的「担当权限组菜单」保存值优先于本处默认值。',
      TH: 'กำหนดเพดานสิทธิ์เมนูเดสก์ท็อป(URL) ตามระดับองค์กรสำหรับแต่ละบทบาทผู้ช่วย เมนูเฉพาะแท็บเล็ตตั้งในส่วนโหมดแท็บเล็ตด้านล่าง เลือกระดับองค์กรด้านบนแล้วแก้ตาราง ค่าที่บันทึกในเมนูกลุ่มผู้ช่วยต่อองค์กร (สิทธิ์ HQ) มีลำดับก่อนค่าเริ่มต้นที่นี่'
    },
    '태블릿모드 (담당자 권한)': {
      EN: 'Tablet mode (assistant permissions)',
      JP: 'タブレットモード（担当者権限）',
      CH: '平板模式（担当人权限）',
      TH: 'โหมดแท็บเล็ต (สิทธิ์ผู้ช่วย)'
    },
    '카탈로그의 일반(데스크톱) 메뉴(URL)를 조직 단계(총본사~가맹점)별로 담당자 역할 상한을 둡니다. 태블릿 전용 메뉴는 아래 「태블릿모드」에서 설정합니다. 본사권한설정과 같이 조직 단계 탭을 선택한 뒤 표를 수정합니다. 본사권한설정의 개별 조직 「담당자 권한그룹별 메뉴」 저장값이 여기 기본값보다 우선합니다.': {
      EN: 'Set permission ceilings for desktop catalog menus by organization level (headquarters through merchant). Tablet-only menus are configured in Tablet mode below. Select an organization level tab as in HQ permissions, then edit the table. Per-organization assistant menus in HQ permissions override these defaults.',
      JP: 'デスクトップ向けカタログメニュー(URL)について、組織段階（総本社〜加盟店）ごとに担当者ロールの上限を設定します。タブレット専用メニューは下の「タブレットモード」で設定します。本社権限設定と同様に組織段階タブを選んで表を編集します。本社権限設定の個別組織「担当者権限グループ別メニュー」保存値がここより優先されます。',
      CH: '按组织级别（总总部至商户）为桌面目录菜单(URL)设置担当角色权限上限。平板专用菜单在下方「平板模式」中配置。请像总部权限设置一样选择组织级别标签后编辑表格。总部权限中各组织的「担当权限组菜单」保存值优先于本处默认值。',
      TH: 'กำหนดเพดานสิทธิ์เมนูเดสก์ท็อป(URL) ตามระดับองค์กร (สำนักงานใหญ่ถึงร้านค้า) เมนูเฉพาะแท็บเล็ตตั้งในส่วนโหมดแท็บเล็ตด้านล่าง เลือกแท็บระดับองค์กรแบบสิทธิ์ HQ แล้วแก้ตาราง ค่าที่บันทึกในเมนูกลุ่มผู้ช่วยต่อองค์กร (สิทธิ์ HQ) มีลำดับก่อนค่าเริ่มต้นที่นี่'
    },
    '태블릿 로그인·사이드바에 노출되는 메뉴만 담당자 역할별 기본 권한을 설정합니다. [태블릿설정]에서 해당 조직 단계에 노출하지 않은 메뉴는 접근불가(NONE)로 고정되며 선택이 비활성화됩니다(태블릿설정이 우선). 아래 조직 단계 탭으로 편집할 단계를 선택합니다.': {
      EN: 'Set default assistant-role permissions only for menus shown in tablet login and the sidebar. Menus not exposed in [Tablet settings] for that organization level are fixed to no access (NONE) and disabled (tablet settings take priority). Select the organization level tab below to edit.',
      JP: 'タブレットログイン・サイドバーに表示するメニューのみ、担当者ロール別の既定権限を設定します。［タブレット設定］で当該組織段階に表示しないメニューはアクセス不可(NONE)で固定され選択不可です（タブレット設定が優先）。下の組織段階タブで編集する段階を選択します。',
      CH: '仅为平板登录与侧栏中显示的菜单设置各担当角色的默认权限。[平板设置]中未对该组织级别开放的菜单固定为不可访问(NONE)且禁用（平板设置优先）。请在下方组织级别标签中选择要编辑的级别。',
      TH: 'ตั้งสิทธิ์เริ่มต้นต่อบทบาทผู้ช่วยเฉพาะเมนูที่แสดงในการล็อกอินแท็บเล็ตและแถบด้านข้าง เมนูที่ไม่เปิดใน[การตั้งค่าแท็บเล็ต]จะคงเป็น NONE และปิดใช้งาน (การตั้งค่าแท็บเล็ตมีลำดับก่อน) เลือกแท็บระดับองค์กรด้านล่างเพื่อแก้ไข'
    },
    '조직 단계(본사권한설정과 동일)': {
      EN: 'Organization level (same as HQ permissions)',
      JP: '組織段階（本社権限設定と同様）',
      CH: '组织级别（与总部权限相同）',
      TH: 'ระดับองค์กร (เหมือนสิทธิ์ HQ)'
    },
    '조직 단계(태블릿 권한 편집)': {
      EN: 'Organization level (tablet permission edit)',
      JP: '組織段階（タブレット権限の編集）',
      CH: '组织级别（编辑平板权限）',
      TH: 'ระดับองค์กร (แก้ไขสิทธิ์แท็บเล็ต)'
    },
    '태블릿 · 대메뉴·역할 일괄 적용': {
      EN: 'Tablet · bulk apply by main menu & role',
      JP: 'タブレット・大メニュー・ロール一括適用',
      CH: '平板 · 按主菜单与角色批量应用',
      TH: 'แท็บเล็ต · นำไปใช้กลุ่มตามเมนูหลักและบทบาท'
    },
    '태블릿 · 역할 일괄 적용': {
      EN: 'Tablet · bulk apply by role',
      JP: 'タブレット・ロール一括適用',
      CH: '平板 · 按角色批量应用',
      TH: 'แท็บเล็ต · นำไปใช้กลุ่มตามบทบาท'
    },
    '태블릿 메뉴 전체': {
      EN: 'All tablet menus',
      JP: 'タブレットメニュー全体',
      CH: '全部平板菜单',
      TH: 'เมนูแท็บเล็ตทั้งหมด'
    },
    '체크한 조직 단계·역할에만 동일 권한이 채워집니다. 태블릿설정에서 미노출된 메뉴는 적용되지 않습니다.': {
      EN: 'Fills the same permission only for checked organization levels and roles. Menus not exposed in Tablet settings are skipped.',
      JP: 'チェックした組織段階・ロールにのみ同じ権限を入れます。タブレット設定で非表示のメニューには適用されません。',
      CH: '仅为勾选的组织级别与角色填入相同权限。平板设置中未开放的菜单不会应用。',
      TH: 'เติมสิทธิ์เดียวกันเฉพาะระดับองค์กรและบทบาทที่เลือก ข้ามเมนูที่ไม่เปิดในการตั้งค่าแท็บเล็ต'
    },
    '태블릿설정에서 이 조직 단계에 노출되지 않은 메뉴입니다.': {
      EN: 'This menu is not exposed for this organization level in Tablet settings.',
      JP: 'この組織段階ではタブレット設定で表示されていないメニューです。',
      CH: '此菜单在该组织级别的平板设置中未开放。',
      TH: 'เมนูนี้ไม่ได้เปิดสำหรับระดับองค์กรนี้ในการตั้งค่าแท็บเล็ต'
    },
    '편집 조직 단계': {
      EN: 'Edit organization level',
      JP: '編集する組織段階',
      CH: '编辑组织级别',
      TH: 'ระดับองค์กรที่แก้ไข'
    },
    '표의 변경은 선택한 조직 단계에 반영됩니다. 저장 시 전체 조직 단계가 일괄 전송됩니다.': {
      EN: 'Table edits apply to the selected organization level. Saving sends all levels in one batch.',
      JP: '表の変更は選択した組織段階に反映されます。保存時は全組織段階を一括送信します。',
      CH: '表格变更应用于所选组织级别。保存时将批量提交全部组织级别。',
      TH: 'การแก้ตารางมีผลกับระดับองค์กรที่เลือก บันทึกจะส่งทุกระดับพร้อมกัน'
    },
    '대메뉴·역할 일괄 적용': {
      EN: 'Bulk apply by main menu & role',
      JP: '大メニュー・ロール一括適用',
      CH: '按主菜单·角色批量应用',
      TH: 'นำไปใช้กลุ่มตามเมนูหลัก·บทบาท'
    },
    '적용 대상 조직 단계': {
      EN: 'Target organization levels',
      JP: '適用対象の組織段階',
      CH: '适用组织级别',
      TH: 'ระดับองค์กรเป้าหมาย'
    },
    '역할': { EN: 'Role', JP: 'ロール', CH: '角色', TH: 'บทบาท' },
    '권한': { EN: 'Permission', JP: '権限', CH: '权限', TH: 'สิทธิ์' },
    '적용': { EN: 'Apply', JP: '適用', CH: '应用', TH: 'นำไปใช้' },
    '그룹 일괄': {
      EN: 'Group bulk',
      JP: 'グループ一括',
      CH: '分组批量',
      TH: 'กลุ่มรวม'
    },
    '전체 메뉴': {
      EN: 'All menus',
      JP: '全メニュー',
      CH: '全部菜单',
      TH: 'เมนูทั้งหมด'
    },
    '(미분류)': {
      EN: '(Uncategorized)',
      JP: '（未分類）',
      CH: '（未分类）',
      TH: '(ไม่จัดหมวด)'
    },
    '태블릿모드': {
      EN: 'Tablet mode',
      JP: 'タブレットモード',
      CH: '平板模式',
      TH: 'โหมดแท็บเล็ต'
    },
    '체크한 조직 단계·역할에만 동일 권한이 채워집니다. 대메뉴에서 「전체 메뉴」를 고르면 카탈로그 전체 URL이 대상입니다.': {
      EN: 'Fills the same permission only for checked organization levels and roles. Choosing “All menus” under main menu targets every catalog URL.',
      JP: 'チェックした組織段階・ロールにのみ同じ権限を入れます。大メニューで「全メニュー」を選ぶとカタログの全URLが対象です。',
      CH: '仅为勾选的组织级别与角色填入相同权限。主菜单选择「全部菜单」时对象为目录中全部 URL。',
      TH: 'เติมสิทธิ์เดียวกันเฉพาะระดับองค์กรและบทบาทที่เลือก เมนูหลัก「เมนูทั้งหมด」ครอบคลุม URL ทั้งหมดในแคตตาล็อก'
    },
    'MANAGER': { EN: 'MANAGER', JP: 'MANAGER', CH: 'MANAGER', TH: 'MANAGER' },
    'OPERATOR': { EN: 'OPERATOR', JP: 'OPERATOR', CH: 'OPERATOR', TH: 'OPERATOR' },
    'SETTLEMENT': { EN: 'SETTLEMENT', JP: 'SETTLEMENT', CH: 'SETTLEMENT', TH: 'SETTLEMENT' },
    'TECH': { EN: 'TECH', JP: 'TECH', CH: 'TECH', TH: 'TECH' },
    'CHATBOT': { EN: 'CHATBOT', JP: 'CHATBOT', CH: 'CHATBOT', TH: 'CHATBOT' },
    '챗봇관리자': {
      EN: 'Chatbot admin',
      JP: 'チャットボット管理者',
      CH: '聊天机器人管理员',
      TH: 'ผู้ดูแลแชทบอท'
    },
    메뉴: { EN: 'Menu', JP: 'メニュー', CH: '菜单', TH: 'เมนู' },
    대메뉴: { EN: 'Main menu', JP: '大メニュー', CH: '主菜单', TH: 'เมนูหลัก' },
    /* /hq/accountMng — 加盟店アクセス（HQ: which companies a login may access） */
    '등록된 업체별 접근 규칙이 없습니다.': {
      EN: 'No merchant access rules are registered.',
      JP: '登録された加盟店アクセス権限がありません。',
      CH: '暂无已登记的商户访问规则。',
      TH: 'ยังไม่มีกฎการเข้าถึงร้านที่ลงทะเบียน'
    },
    '<strong>총본사·본사·총판</strong> 소속 로그인 ID만 등록할 수 있고, <strong>허용 업체코드</strong>는 <strong>전 업체 코드</strong> 중에서 선택합니다. 허용 업체를 고른 뒤 사용자 ID를 선택하면, 그 사용자는 사용자관리 등에서 <strong>지정한 업체 코드에만</strong> 접근할 수 있으며(하위 가맹점을 자동으로 넓혀 주지 않음), 상위 조직 권한으로 이미 볼 수 있는 범위와는 별개로 여기서는 <strong>명시한 코드</strong>만큼만 열어 줍니다.': {
      EN: 'You may register login IDs only for users under the root HQ, regional HQ, or master distributor. Pick an allowed company code from all company codes. After you choose the allowed merchant and the user ID, that user can access only the specified company codes in User management, etc. (child merchants are not expanded automatically). Independently of what their parent-org role already lets them see, this screen opens only the codes you list here.',
      JP: '「本社・支社・総代理店」所属のログインIDのみ登録できます。「許可する加盟店コード」は「すべての取引先コード」から選択します。許可取引先を選び、その後ユーザーIDを選ぶと、そのユーザーはユーザー管理等で「指定した加盟店コードのみ」にアクセスできます（下位加盟店へ自動拡張しません）。上位組織の権限で閲覧できる範囲とは別に、ここでは「明示したコード」分だけを開きます。',
      CH: '仅可登记隶属于「总总部·分公司·总代理」的登录ID。「允许的商户代码」从「全部商户代码」中选择。选定允许的商户后再选用户ID后，该用户在用户管理等画面中只能访问「指定的商户代码」（不会自动包含下级加盟店）。与上级组织权限已可见范围无关，此处仅开放您明确列出的代码。',
      TH: 'ลงทะเบียนได้เฉพาะล็อกอิน ID ภายใต้「สำนักใหญ่·สำนักงานใหญ่·ตัวแทนหลัก」เลือก「รหัสร้านที่อนุญาต」จาก「รหัสร้านทั้งหมด」หลังเลือกร้านที่อนุญาตแล้วเลือกผู้ใช้ ผู้ใช้จะเข้าถึงได้เฉพาะ「รหัสร้านที่ระบุ」ในการจัดการผู้ใช้ ฯลฯ (ไม่ขยายลูกร้านอัตโนมัติ) แยกจากขอบเขตที่เห็นตามสิทธิ์องค์กรระดับบน ที่นี่เปิดเฉพาะรหัสที่ระบุเท่านั้น'
    },
    '행이 하나라도 있으면 사용자관리 목록·등록·초기화 범위는 <strong>하위 조직 ∩ 여기서 지정한 업체</strong>로만 제한됩니다. 담당자(ASSISTANT) 메뉴는 [본사권한설정]의 담당자 권한그룹별 메뉴에서 조정하고, OTP·로그인 정책은 [사용자설정]을 따릅니다.': {
      EN: 'If any row exists, User management list, registration, and reset scope is limited to the intersection of sub-organizations and the merchants specified here. ASSISTANT menus are adjusted under HQ permissions → per-assistant permission groups; OTP and login policy follow User settings.',
      JP: '1行でもある場合、ユーザー管理の一覧・登録・初期化の範囲は「下位組織 ∩ ここで指定した取引先」に限定されます。担当者(ASSISTANT)のメニューは「本社権限設定」の担当者権限グループ別メニューで調整し、OTP・ログイン方針は「ユーザー設定」に従います。',
      CH: '只要存在一行，用户管理的列表、登记与重置范围将限制为「下级组织 ∩ 此处指定的商户」。担当(ASSISTANT)菜单在「总部权限设置」的担当权限组菜单中调整；OTP 与登录策略遵循「用户设置」。',
      TH: 'มีแถวใดก็ตาม ขอบเขตรายการ·ลงทะเบียน·รีเซ็ตในการจัดการผู้ใช้จำกัดเพียง「องค์กรลำดับล่าง ∩ ร้านที่ระบุที่นี่」 เมนู ASSISTANT ปรับที่เมนูตามกลุ่มสิทธิ์ผู้ช่วยใน「สิทธิ์ HQ」 นโยบาย OTP·ล็อกอินตาม「การตั้งค่าผู้ใช้」'
    },
    '목록 <strong>수정</strong>·<strong>삭제</strong>, 상단 <strong>저장</strong>·<strong>삭제</strong>(행 체크), 추가·수정 창의 <strong>저장</strong>으로 적용합니다.': {
      EN: 'Use list Edit / Delete, top Save / Delete (row check), and Save in the add/edit dialog to apply changes.',
      JP: '一覧の「修正」「削除」、上部の「保存」「削除」（行チェック）、追加・修正ウィンドウの「保存」で反映します。',
      CH: '通过列表「修改」「删除」、顶部「保存」「删除」（勾选行）以及新增/编辑窗口中的「保存」来应用。',
      TH: 'ใช้ 「แก้ไข」「ลบ」ในรายการ 「บันทึก」「ลบ」ด้านบน (เลือกแถว) และ 「บันทึก」ในหน้าต่างเพิ่ม/แก้ไข เพื่อบันทึก'
    },
    '새로고침': {
      EN: 'Refresh',
      JP: '更新',
      CH: '刷新',
      TH: 'รีเฟรช'
    },
    '접근권한 추가': {
      EN: 'Add access rule',
      JP: 'アクセス権限を追加',
      CH: '添加访问权限',
      TH: 'เพิ่มสิทธิ์การเข้าถึง'
    },
    '접근권한 수정': {
      EN: 'Edit access rule',
      JP: 'アクセス権限を修正',
      CH: '修改访问权限',
      TH: 'แก้ไขสิทธิ์การเข้าถึง'
    },
    '등록일시': {
      EN: 'Registered at',
      JP: '登録日時',
      CH: '登记时间',
      TH: 'วันที่ลงทะเบียน'
    },
    '사용자ID': {
      EN: 'User ID',
      JP: 'ユーザーID',
      CH: '用户ID',
      TH: 'รหัสผู้ใช้'
    },
    '사용자 ID (로그인 ID)': {
      EN: 'User ID (login ID)',
      JP: 'ユーザーID（ログインID）',
      CH: '用户ID（登录ID）',
      TH: 'รหัสผู้ใช้ (ล็อกอิน)'
    },
    '허용 업체코드': {
      EN: 'Allowed company code',
      JP: '許可する加盟店コード',
      CH: '允许的商户代码',
      TH: 'รหัสร้านที่อนุญาต'
    },
    '업체를 선택하세요': {
      EN: 'Select a company.',
      JP: '取引先を選択してください。',
      CH: '请选择商户。',
      TH: 'กรุณาเลือกร้านค้า'
    },
    '사용자를 선택하세요': {
      EN: 'Select a user.',
      JP: 'ユーザーを選択してください。',
      CH: '请选择用户。',
      TH: 'กรุณาเลือกผู้ใช้'
    },
    '소속': {
      EN: 'Org',
      JP: '所属',
      CH: '所属',
      TH: 'สังกัด'
    },
    '먼저 사용자 ID를 고릅니다(목록은 총본사·본사·총판 소속만). 이어서 허용 업체코드를 선택합니다(전 업체). 저장 후 사용자관리 등에서는 지정한 업체 코드와만 교집합됩니다. 저장 시 서버에서 검증합니다.': {
      EN: 'First pick a user ID (the list shows only root HQ, regional HQ, and master-distributor users). Then choose the allowed company code (any company). After saving, User management, etc. intersects with only the codes you specify. The server validates on save.',
      JP: '先にユーザーIDを選びます（一覧は本社・支社・総代理店所属のみ）。続けて許可する加盟店コードを選びます（全取引先）。保存後、ユーザー管理等では指定した加盟店コードとの積集合のみになります。保存時にサーバーで検証します。',
      CH: '先选择用户ID（列表仅显示总总部·分公司·总代理所属）。再选择允许的商户代码（全部商户）。保存后，用户管理等仅与指定的商户代码求交集。保存时由服务器校验。',
      TH: 'เลือกรหัสผู้ใช้ก่อน (รายการมีเฉพาะสังกัดสำนักใหญ่·สำนักงานใหญ่·ตัวแทนหลัก) จากนั้นเลือกรหัสร้านที่อนุญาต (ทุกร้าน) หลังบันทึก การจัดการผู้ใช้ ฯลฯ จะตัดกับเฉพาะรหัสที่ระบุ ตรวจสอบที่เซิร์ฟเวอร์ตอนบันทึก'
    },
    '사용자 ID를 선택하세요.': {
      EN: 'Select a user ID.',
      JP: 'ユーザーIDを選択してください。',
      CH: '请选择用户ID。',
      TH: 'กรุณาเลือกรหัสผู้ใช้'
    },
    '허용 업체코드(업체)를 선택하세요.': {
      EN: 'Select an allowed company code.',
      JP: '許可する加盟店コードを選択してください。',
      CH: '请选择允许的商户代码。',
      TH: 'กรุณาเลือกรหัสร้านที่อนุญาต'
    },
    '사용자ID(로그인 ID)를 입력하세요.': {
      EN: 'Enter the user ID (login ID).',
      JP: 'ユーザーID（ログインID）を入力してください。',
      CH: '请输入用户ID（登录ID）。',
      TH: 'กรอกรหัสผู้ใช้ (ล็อกอิน)'
    },
    '허용할 업체코드(본사·총판·가맹점 코드)를 입력하세요.': {
      EN: 'Enter the allowed company code (HQ, distributor, or merchant code).',
      JP: '許可する加盟店コード（本社・総代理・加盟店コード）を入力してください。',
      CH: '请输入允许的商户代码（总部·总代·加盟店代码）。',
      TH: 'กรอกรหัสร้านที่อนุญาต (สำนักงานใหญ่·ตัวแทน·ร้าน)'
    },
    '추가 실패': {
      EN: 'Add failed',
      JP: '追加に失敗しました',
      CH: '添加失败',
      TH: 'เพิ่มไม่สำเร็จ'
    },
    '이 접근 규칙을 삭제할까요?': {
      EN: 'Delete this access rule?',
      JP: 'このアクセス権限を削除しますか？',
      CH: '要删除此访问规则吗？',
      TH: 'ลบกฎการเข้าถึงนี้หรือไม่'
    },
    '{COUNT}건의 접근 규칙을 삭제할까요?': {
      EN: 'Delete {COUNT} access rule(s)?',
      JP: '{COUNT}件のアクセス権限を削除しますか？',
      CH: '要删除 {COUNT} 条访问规则吗？',
      TH: 'ลบกฎการเข้าถึง {COUNT} รายการหรือไม่'
    },
    '먼저 [접근권한 추가] 또는 목록의 [수정]을 눌러 창을 연 뒤 [저장]을 사용하세요.': {
      EN: 'Open the dialog with [Add access rule] or [Edit] in the list, then use [Save].',
      JP: '先に「アクセス権限を追加」または一覧の「修正」でウィンドウを開き、「保存」を押してください。',
      CH: '请先通过「添加访问权限」或列表中的「修改」打开窗口，再使用「保存」。',
      TH: 'เปิดหน้าต่างด้วย [เพิ่มสิทธิ์] หรือ [แก้ไข] ในรายการ แล้วใช้ [บันทึก]'
    },
    '삭제할 행을 체크하세요.': {
      EN: 'Check the rows to delete.',
      JP: '削除する行にチェックを入れてください。',
      CH: '请勾选要删除的行。',
      TH: 'เลือกแถวที่จะลบ'
    },
    /* /hq/orgViewColumnAllowance — 組織項目設定 */
    '조직항목설정': {
      EN: 'Organization column settings',
      JP: '組織項目設定',
      CH: '组织字段设置',
      TH: 'การตั้งค่าคอลัมน์องค์กร'
    },
    '총본사가 각 본사(REGIONAL) 트리마다, 조직 유형·화면별로 VIEW SETTING에서 노출·선택 가능한 열을 지정합니다. 본사·총판·지사·대리점·영업점(동일 설정)·가맹점 네 가지로 나누어 저장합니다. 지사·대리점·영업점과 가맹점에 별도 저장이 없으면 해당 화면의 총판 설정을 그대로 따릅니다. <strong>결제관리</strong>(결제내역·분류 화면·URL/챗봇·상계 및 <strong>통합내역</strong>)과 <strong>정산관리</strong>의 <strong>통합정산</strong>은 화면·조직 유형을 바꿀 때 <strong>기본 체크안</strong>이 자동 적용되며(본사=전체 허용, 총판·지사·가맹 순으로 축소), 체크되지 않은 열은 목록에서 제거되지 않고 꺼진 상태로 둡니다. 서버에 이미 저장된 정책이 있으면 [불러오기]·정책 행 클릭 시 그대로 불러옵니다. 고정 열(번호·업체명·거래일·Route No·TransactionId 등)은 항상 표시되며 여기 목록에 나오지 않습니다. [불러오기]는 현재 선택한 본사·조직 유형·화면에 대해 서버에 저장된 체크 상태를 가져와 반영합니다. 아래 [추가 VIEW 항목]은 화면마다 다르게 본사 전용 열을 등록합니다. 등록된 항목은 해당 화면의 VIEW SETTING에 나타나며, 기본 체크안에 포함된 경우에만 조직 설정에서 자동 체크됩니다.': {
      EN: 'Root HQ defines, per regional HQ (REGIONAL) tree, which columns can appear and be toggled in VIEW SETTING for each org type and screen. Values are saved separately for regional HQ, master distributor, branch/agency/sales office (shared), and merchant. If branch group or merchant has no saved row, that screen follows the master-distributor policy. <strong>Payment management</strong> (payment list, category screens, URL/chatbot, offset cancel, and <strong>integrated transactions</strong>) and <strong>Settlement management</strong> → <strong>integrated settlement</strong> apply a <strong>default checked set</strong> when you change screen or org type (HQ = all allowed; progressively narrower for distributor, branch, merchant). Unchecked columns stay in the list but off. If a policy already exists on the server, use [Load] or click a policy row to restore it. Fixed columns (row no., merchant name, transaction date, route no., transaction id, etc.) are always shown and do not appear in this checklist. [Load] fetches the saved check state for the currently selected HQ, org type, and screen. [Add VIEW item] registers HQ-only columns per screen; they appear in that screen’s VIEW SETTING and are auto-checked here only when included in the default set.',
      JP: '総本部は各本社（REGIONAL）ツリーごとに、組織区分・画面別に VIEW SETTING で表示・選択できる列を指定します。本社・総代理・支社・代理店・営業店（同一設定）・加盟店の4区分で保存します。支社・代理店・営業店および加盟店に別途保存がない場合は、その画面の総代理設定に従います。<strong>決済管理</strong>（決済一覧・分類画面・URL/チャットボット・相殺取消および<strong>統合取引</strong>）と<strong>精算管理</strong>の<strong>統合精算</strong>は、画面・組織区分を変えると<strong>既定のチェック案</strong>が自動適用されます（本社＝すべて許可、総代理・支社・加盟店の順で絞込）。チェックされていない列は一覧から消えずオフのままです。サーバーに既存ポリシーがある場合は［読み込み］・ポリシー行クリックで復元します。固定列（番号・加盟店名・取引日・Route No・TransactionId 等）は常に表示され、ここには出ません。［読み込み］は現在選択中の本社・組織区分・画面についてサーバーに保存されたチェック状態を取り込みます。下の［追加 VIEW 項目］で画面ごとに本社専用列を登録します。登録項目は当該画面の VIEW SETTING に現れ、既定チェック案に含まれる場合のみ組織設定で自動チェックされます。',
      CH: '总本部按各分公司（REGIONAL）树，为每种组织类型与画面指定可在 VIEW SETTING 中显示与勾选的列。分为本社、总代、支社·代理店·营业点（同配置）、加盟店四类保存。若支社组或加盟店无单独保存，则沿用该画面的总代设置。切换画面或组织类型时，<strong>支付管理</strong>（支付列表、分类画面、URL/聊天机器人、轧差及<strong>整合交易</strong>）与<strong>结算管理</strong>下的<strong>整合结算</strong>会自动套用<strong>默认勾选方案</strong>（本社=全允许，总代、支社、加盟店逐级收窄）。未勾选的列不会从列表移除，仅保持关闭。若服务器已有策略，可用［加载］或点击策略行恢复。固定列（序号、商户名、交易日、Route No、TransactionId 等）始终显示且不出现在此列表。［加载］会拉取当前所选本社、组织类型与画面在服务器上的勾选状态。下方［添加 VIEW 项］可按画面登记本社专用列；登记项会出现在该画面的 VIEW SETTING，且仅当包含在默认方案中时才在组织设置里自动勾选。',
      TH: 'สำนักใหญ่กำหนดต่อต้นไม้สำนักงานใหญ่ (REGIONAL) ว่าแต่ละประเภทองค์กรและหน้าจอให้คอลัมน์ใดแสดง/เลือกได้ใน VIEW SETTING แยกบันทึกเป็นสำนักงานใหญ่ ตัวแทนหลัก สาขา·ตัวแทน·สำนักขาย (ชุดเดียวกัน) และร้านค้า หากกลุ่มสาขาหรือร้านไม่มีการบันทึกแยก จะใช้ค่าของตัวแทนหลักของหน้านั้น <strong>การจัดการชำระเงิน</strong> (รายการชำระ หมวดหน้า URL/แชทบอท หักกลบ และ<strong>ธุรกรรมรวม</strong>) กับ<strong>การจัดการชำระ</strong> → <strong>ชำระรวม</strong> จะใช้<strong>ชุดติ๊กเริ่มต้น</strong>เมื่อเปลี่ยนหน้าหรือประเภทองค์กร (สำนักงานใหญ่ = อนุญาตทั้งหมด ค่อยๆ แคบลง) คอลัมน์ที่ไม่ติ๊กยังอยู่ในรายการแต่ปิด หากมีนโยบายบนเซิร์ฟเวอร์แล้ว ใช้［โหลด］หรือคลิกแถวนโยบายเพื่อกู้ค่า คอลัมน์คงที่ (ลำดับ ชื่อร้าน วันที่ทำรายการ Route No TransactionId ฯลฯ) แสดงเสมอและไม่อยู่ในรายการนี้ ［โหลด］ดึงสถานะติ๊กที่บันทึกสำหรับสำนักงานใหญ่ ประเภทองค์กร และหน้าที่เลือก ［เพิ่มรายการ VIEW］ลงทะเบียนคอลัมน์เฉพาะสำนักงานใหญ่ต่อหน้า ปรากฏใน VIEW SETTING ของหน้านั้น และติ๊กอัตโนมัติที่นี่เฉพาะเมื่ออยู่ในชุดเริ่มต้น'
    },
    '설정 대상 본사': {
      EN: 'Target regional HQ',
      JP: '設定対象の本社',
      CH: '设置目标分公司',
      TH: 'สำนักงานใหญ่เป้าหมาย'
    },
    '노출 대상 조직': {
      EN: 'Target org type',
      JP: '表示対象の組織',
      CH: '显示目标组织',
      TH: 'ประเภทองค์กรที่แสดง'
    },
    '설정 대상 화면': {
      EN: 'Target screen',
      JP: '設定対象の画面',
      CH: '设置目标画面',
      TH: 'หน้าจอเป้าหมาย'
    },
    '지사·대리점·영업점': {
      EN: 'Branch / agency / sales office',
      JP: '支社・代理店・営業店',
      CH: '支社·代理店·营业点',
      TH: 'สาขา·ตัวแทน·สำนักขาย'
    },
    '추가 VIEW 항목 (화면별 목록 · 본사 등록)': {
      EN: 'Extra VIEW items (per-screen list · HQ registered)',
      JP: '追加 VIEW 項目（画面別一覧・本社登録）',
      CH: '附加 VIEW 项（分画面列表·总部登记）',
      TH: 'รายการ VIEW เพิ่ม (รายการต่อหน้า·ลงทะเบียน HQ)'
    },
    '설정 대상 화면을 먼저 선택한 뒤, 표시명을 넣고 [항목 추가]하세요. 목록에서 이름을 바꾸거나 삭제할 수 있습니다. 내부 키는 자동 부여됩니다.': {
      EN: 'Pick the target screen first, enter a display name, then [Add item]. You can rename or delete from the list. Internal keys are assigned automatically.',
      JP: '先に設定対象の画面を選び、表示名を入力して［項目追加］してください。一覧で名前の変更や削除ができます。内部キーは自動付与されます。',
      CH: '请先选择目标画面，输入显示名称后点击［添加项］。可在列表中改名或删除。内部键自动生成。',
      TH: 'เลือกหน้าที่ตั้งค่าก่อน ใส่ชื่อที่แสดง แล้วกด［เพิ่มรายการ］ แก้ชื่อหรือลบได้ในรายการ คีย์ภายในระบบสร้างอัตโนมัติ'
    },
    표시명: {
      EN: 'Display name',
      JP: '表示名',
      CH: '显示名称',
      TH: 'ชื่อที่แสดง'
    },
    '예: 비고란': {
      EN: 'e.g. Notes column',
      JP: '例: 備考欄',
      CH: '例：备注栏',
      TH: 'เช่น คอลัมน์หมายเหตุ'
    },
    '항목 추가': {
      EN: 'Add item',
      JP: '項目を追加',
      CH: '添加项',
      TH: 'เพิ่มรายการ'
    },
    '화면을 선택하세요.': {
      EN: 'Select a screen.',
      JP: '画面を選択してください。',
      CH: '请选择画面。',
      TH: 'กรุณาเลือกหน้าจอ'
    },
    '선택한 조직 유형에 노출할 열 (VIEW SETTING에서 선택 가능)': {
      EN: 'Columns to expose for the selected org type (selectable in VIEW SETTING)',
      JP: '選択した組織区分に表示する列（VIEW SETTING で選択可能）',
      CH: '对所选组织类型显示的列（可在 VIEW SETTING 中选择）',
      TH: 'คอลัมน์ที่แสดงตามประเภทองค์กรที่เลือก (เลือกได้ใน VIEW SETTING)'
    },
    현재리스트순위: {
      EN: 'List order',
      JP: 'リスト順位',
      CH: '当前列表顺序',
      TH: 'ลำดับในรายการ'
    },
    항목이름: {
      EN: 'Item name',
      JP: '項目名',
      CH: '项目名称',
      TH: 'ชื่อรายการ'
    },
    '위 아래': {
      EN: 'Up / Down',
      JP: '上・下',
      CH: '上 / 下',
      TH: 'ขึ้น / ลง'
    },
    '열 노출 저장·일괄 선택': {
      EN: 'Save column visibility · bulk select',
      JP: '列の表示を保存・一括選択',
      CH: '保存列显示·批量选择',
      TH: 'บันทึกการแสดงคอลัมน์·เลือกเป็นกลุ่ม'
    },
    전체해제: {
      EN: 'Clear all',
      JP: 'すべて解除',
      CH: '全部清除',
      TH: 'ยกเลิกทั้งหมด'
    },
    '체크한 열만 해당 조직 유형 사용자 화면의 VIEW SETTING에 나타납니다. 지사·대리점·영업점·가맹점은 저장이 없으면 총판 설정을 사용합니다.': {
      EN: 'Only checked columns appear in VIEW SETTING for users of that org type. Branch, agency, sales office, and merchant use the distributor policy when nothing is saved.',
      JP: 'チェックした列のみ、当該組織区分のユーザーの VIEW SETTING に表示されます。支社・代理店・営業店・加盟店は保存がない場合は総代理設定を使います。',
      CH: '仅勾选的列会出现在该组织类型用户的 VIEW SETTING 中。支社、代理店、营业点、加盟店若无保存则使用总代设置。',
      TH: 'เฉพาะคอลัมน์ที่ติ๊กจะปรากฏใน VIEW SETTING ของผู้ใช้ประเภทองค์กรนั้น สาขา·ตัวแทน·สำนักขาย·ร้าน หากไม่มีการบันทึกจะใช้ค่าตัวแทนหลัก'
    },
    '체크한 열만 해당 조직 유형 사용자 화면의 VIEW SETTING에 나타납니다. ▲▼ 버튼으로 체크된 항목의 순서를 바꾼 뒤 [저장]하면 VIEW SETTING에서의 기본 나열 순서에 반영됩니다. 지사·대리점·영업점·가맹점은 저장이 없으면 총판 설정을 사용합니다.': {
      EN: 'Only checked columns appear in VIEW SETTING for users of that org type. Use ▲▼ to reorder checked items; [Save] updates the default order in VIEW SETTING. Branch, agency, sales office, and merchant use the distributor policy when nothing is saved.',
      JP: 'チェックした列のみ、当該組織区分ユーザーの VIEW SETTING に表示されます。▲▼ でチェック済み項目の順序を変え、［保存］で VIEW SETTING の既定並び順に反映します。支社・代理店・営業店・加盟店は保存がない場合は総代理設定を使います。',
      CH: '仅勾选的列会出现在该组织类型用户的 VIEW SETTING 中。用 ▲▼ 调整已勾选项顺序，［保存］后反映到 VIEW SETTING 的默认排列。支社、代理店、营业点、加盟店若无保存则使用总代设置。',
      TH: 'เฉพาะคอลัมน์ที่ติ๊กจะแสดงใน VIEW SETTING ของประเภทองค์กรนั้น ใช้ ▲▼ จัดลำดับรายการที่ติ๊ก แล้ว［บันทึก］เพื่อสะท้อนลำดับเริ่มต้นใน VIEW SETTING สาขา·ตัวแทน·สำนักขาย·ร้าน หากไม่บันทึกจะใช้ค่าตัวแทนหลัก'
    },
    '저장된 설정 요약 (선택한 본사)': {
      EN: 'Saved policy summary (selected HQ)',
      JP: '保存済み設定の要約（選択した本社）',
      CH: '已保存设置摘要（所选本社）',
      TH: 'สรุปการตั้งค่าที่บันทึก (สำนักงานใหญ่ที่เลือก)'
    },
    '행을 클릭하면 위의 화면·조직 유형이 맞춰지고 서버에 저장된 체크 상태가 불러와집니다.': {
      EN: 'Click a row to align the screen and org type above and load the saved check state from the server.',
      JP: '行をクリックすると上の画面・組織区分が合わせられ、サーバーに保存されたチェック状態が読み込まれます。',
      CH: '点击一行可同步上方的画面与组织类型，并从服务器加载已保存的勾选状态。',
      TH: 'คลิกแถวเพื่อจับคู่หน้าจอและประเภทองค์กรด้านบน และโหลดสถานะติ๊กจากเซิร์ฟเวอร์'
    },
    '허용 열 수': {
      EN: 'Allowed columns',
      JP: '許可列数',
      CH: '允许列数',
      TH: 'จำนวนคอลัมน์ที่อนุญาต'
    },
    '내부 키': {
      EN: 'Internal key',
      JP: '内部キー',
      CH: '内部键',
      TH: 'คีย์ภายใน'
    },
    '노출 항목 저장': {
      EN: 'Save column allowance',
      JP: '表示項目を保存',
      CH: '保存显示项',
      TH: 'บันทึกรายการที่แสดง'
    },
    '노출 제한 해제': {
      EN: 'Remove column restriction',
      JP: '表示制限を解除',
      CH: '解除显示限制',
      TH: 'ยกเลิกข้อจำกัดการแสดง'
    },
    '불러오기': {
      EN: 'Load',
      JP: '読み込み',
      CH: '加载',
      TH: 'โหลด'
    },
    '등록된 추가 항목이 없습니다.': {
      EN: 'No extra items are registered.',
      JP: '登録された追加項目がありません。',
      CH: '暂无已登记的附加项。',
      TH: 'ยังไม่มีรายการเพิ่มที่ลงทะเบียน'
    },
    '화면 정의를 찾을 수 없습니다.': {
      EN: 'No screen definition found.',
      JP: '画面定義が見つかりません。',
      CH: '未找到画面定义。',
      TH: 'ไม่พบนิยามหน้าจอ'
    },
    '선택 가능한 열이 없습니다.': {
      EN: 'No selectable columns.',
      JP: '選択可能な列がありません。',
      CH: '没有可选择的列。',
      TH: 'ไม่มีคอลัมน์ที่เลือกได้'
    },
    '설정 대상 본사를 선택하면 저장된 정책이 표시됩니다.': {
      EN: 'Select a target regional HQ to see saved policies.',
      JP: '設定対象の本社を選ぶと保存済みポリシーが表示されます。',
      CH: '请选择目标本社以显示已保存的策略。',
      TH: 'เลือกสำนักงานใหญ่เป้าหมายเพื่อดูนโยบายที่บันทึก'
    },
    '저장된 정책이 없습니다. 열을 체크한 뒤 [저장] 또는 하단 [노출 항목 저장]으로 저장하세요.': {
      EN: 'No saved policy. Check columns, then save with [Save] in the panel or [Save column allowance] below.',
      JP: '保存されたポリシーがありません。列にチェックを入れ、パネルの［保存］または下部の［表示項目を保存］で保存してください。',
      CH: '暂无已保存策略。请勾选列后使用面板中的［保存］或底部的［保存显示项］保存。',
      TH: 'ยังไม่มีนโยบายที่บันทึก ติ๊กคอลัมน์แล้วบันทึกด้วย［บันทึก］ในแผงหรือ［บันทึกรายการที่แสดง］ด้านล่าง'
    },
    '행을 클릭하면 위에서 해당 화면·조직 유형으로 전환하고 저장된 체크를 불러옵니다.': {
      EN: 'Click a row to switch the screen and org type above and load the saved checks.',
      JP: '行をクリックすると上で該当する画面・組織区分に切り替え、保存されたチェックを読み込みます。',
      CH: '点击一行可在上方切换到对应画面与组织类型并加载已保存的勾选。',
      TH: 'คลิกแถวเพื่อสลับหน้าจอและประเภทองค์กรด้านบนและโหลดการติ๊กที่บันทึก'
    },
    '목록 조회 실패': {
      EN: 'Failed to load the list.',
      JP: '一覧の取得に失敗しました。',
      CH: '列表加载失败。',
      TH: 'โหลดรายการไม่สำเร็จ'
    },
    '대상 본사를 선택하세요.': {
      EN: 'Select a target regional HQ.',
      JP: '対象の本社を選択してください。',
      CH: '请选择目标本社。',
      TH: 'กรุณาเลือกสำนักงานใหญ่เป้าหมาย'
    },
    '허용 열이 하나도 없습니다. (선택 컬럼 없음) 저장할까요?': {
      EN: 'No columns are allowed (none selected). Save anyway?',
      JP: '許可する列がありません（選択なし）。このまま保存しますか？',
      CH: '未允许任何列（未选择）。仍要保存吗？',
      TH: 'ไม่มีคอลัมน์ที่อนุญาต (ไม่ได้เลือก) บันทึกต่อหรือไม่'
    },
    '표시명을 입력하세요.': {
      EN: 'Enter a display name.',
      JP: '表示名を入力してください。',
      CH: '请输入显示名称。',
      TH: 'กรุณากรอกชื่อที่แสดง'
    },
    '추가되었습니다.': {
      EN: 'Added.',
      JP: '追加しました。',
      CH: '已添加。',
      TH: 'เพิ่มแล้ว'
    },
    '표시명 수정': {
      EN: 'Edit display name',
      JP: '表示名を修正',
      CH: '修改显示名称',
      TH: 'แก้ไขชื่อที่แสดง'
    },
    '표시명을 비울 수 없습니다.': {
      EN: 'Display name cannot be empty.',
      JP: '表示名を空にできません。',
      CH: '显示名称不能为空。',
      TH: 'ชื่อที่แสดงต้องไม่ว่าง'
    },
    '수정 실패': {
      EN: 'Update failed',
      JP: '更新に失敗しました',
      CH: '更新失败',
      TH: 'อัปเดตไม่สำเร็จ'
    },
    '이 추가 항목을 삭제할까요?': {
      EN: 'Delete this extra item?',
      JP: 'この追加項目を削除しますか？',
      CH: '要删除此附加项吗？',
      TH: 'ลบรายการเพิ่มนี้หรือไม่'
    },
    '선택한 본사·조직 유형·화면에 대한 컬럼 제한만 해제합니다. 계속할까요?': {
      EN: 'This removes only the column restriction for the selected HQ, org type, and screen. Continue?',
      JP: '選択した本社・組織区分・画面に対する列制限のみを解除します。続行しますか？',
      CH: '将仅解除所选本社、组织类型与画面的列限制。是否继续？',
      TH: 'จะยกเลิกเฉพาะข้อจำกัดคอลัมน์สำหรับสำนักงานใหญ่ ประเภทองค์กร และหน้าที่เลือก ดำเนินต่อหรือไม่'
    },
    '제한이 해제되었습니다.': {
      EN: 'The restriction has been removed.',
      JP: '制限を解除しました。',
      CH: '已解除限制。',
      TH: 'ยกเลิกข้อจำกัดแล้ว'
    },
    '정산배포': {
      EN: 'Settlement publish',
      JP: '精算配布',
      CH: '结算发布',
      TH: 'เผยแพร่การชำระ'
    },
    '정산대기': {
      EN: 'Settlement hold',
      JP: '精算待ち',
      CH: '结算待处理',
      TH: 'พักชำระ'
    },
    '통합내역': {
      EN: 'Integrated transactions',
      JP: '統合明細',
      CH: '统合明细',
      TH: 'รายการรวม'
    },
    '통합정산': {
      EN: 'Integrated settlement',
      JP: '統合精算',
      CH: '统合结算',
      TH: 'การชำระแบบรวม'
    },
    '결제내역': {
      EN: 'Payment list',
      JP: '決済履歴',
      CH: '支付记录',
      TH: 'รายการชำระเงิน'
    },
    '성공내역': {
      EN: 'Successful payments',
      JP: '成功履歴',
      CH: '成功记录',
      TH: 'รายการสำเร็จ'
    },
    '실패내역': {
      EN: 'Failed payments',
      JP: '失敗履歴',
      CH: '失败记录',
      TH: 'รายการล้มเหลว'
    },
    '취소내역': {
      EN: 'Cancellations',
      JP: '取消履歴',
      CH: '取消记录',
      TH: 'รายการยกเลิก'
    },
    '무효처리': {
      EN: 'Void processing',
      JP: '無効処理',
      CH: '作废处理',
      TH: 'โมฆะ'
    },
    '이메일 무효': {
      EN: 'Email void',
      JP: 'メール無効',
      CH: '邮件作废',
      TH: 'โมฆะอีเมล'
    },
    '이메일무효': {
      KO: '이메일 무효',
      EN: 'Email void',
      JP: 'メール無効',
      CH: '邮件作废',
      TH: 'โมฆะอีเมล'
    },
    '환불처리': {
      EN: 'Refund processing',
      JP: '返金処理',
      CH: '退款处理',
      TH: 'คืนเงิน'
    },
    '강제환불': {
      EN: 'Force refund',
      JP: '強制返金',
      CH: '强制退款',
      TH: 'บังคับคืน'
    },
    '무효내역': {
      EN: 'Void processing',
      JP: '無効処理',
      CH: '作废处理',
      TH: 'โมฆะ'
    },
    '환불내역': {
      EN: 'Refund processing',
      JP: '返金処理',
      CH: '退款处理',
      TH: 'คืนเงิน'
    },
    '강제환불내역': {
      EN: 'Force refund',
      JP: '強制返金',
      CH: '强制退款',
      TH: 'บังคับคืน'
    },
    'URL결제내역': {
      EN: 'URL payment list',
      JP: 'URL決済履歴',
      CH: 'URL支付记录',
      TH: 'รายการชำระ URL'
    },
    '챗봇결제내역': {
      EN: 'Chatbot payment list',
      JP: 'チャットボット決済履歴',
      CH: '聊天机器人支付记录',
      TH: 'รายการชำระแชทบอท'
    },
    '상계취소내역': {
      EN: 'Offset cancellation list',
      JP: '相殺取消履歴',
      CH: '冲销取消记录',
      TH: 'รายการยกเลิกหักกลบ'
    },
    '업체관리': {
      EN: 'Merchant management',
      JP: '取引先管理',
      CH: '商户管理',
      TH: 'จัดการร้านค้า'
    },
    '수수료관리': {
      EN: 'Commission management',
      JP: '手数料管理',
      CH: '佣金管理',
      TH: 'จัดการค่าคอมมิชชัน'
    },
    '유통망정산내역': {
      EN: 'Distribution settlement list',
      JP: '流通網精算履歴',
      CH: '流通网结算记录',
      TH: 'รายการชำระเครือข่ายจำหน่าย'
    },
    '가맹점정산내역': {
      EN: 'Merchant settlement list',
      JP: '加盟店精算履歴',
      CH: '加盟店结算记录',
      TH: 'รายการชำระร้านค้า'
    },
    '정산보류내역': {
      EN: 'Settlement hold list',
      JP: '精算保留履歴',
      CH: '结算暂缓记录',
      TH: 'รายการพักชำระ'
    },
    '수수료내역': {
      EN: 'Fee list',
      JP: '手数料履歴',
      CH: '手续费记录',
      TH: 'รายการค่าธรรมเนียม'
    },
    '환수금관리': {
      EN: 'Recovery management',
      JP: '回収金管理',
      CH: '回收金管理',
      TH: 'จัดการเงินคืน'
    },
    '환수금내역': {
      EN: 'Recovery list',
      JP: '回収金履歴',
      CH: '回收金明细',
      TH: 'รายการเงินคืน'
    },
    '정산실행': {
      EN: 'Run settlement',
      JP: '精算実行',
      CH: '执行结算',
      TH: 'รันชำระ'
    },
    '정산리포트': {
      EN: 'Settlement report',
      JP: '精算レポート',
      CH: '结算报表',
      TH: 'รายงานการชำระ'
    },
    '담보금내역': {
      EN: 'Collateral list',
      JP: '担保金履歴',
      CH: '保证金记录',
      TH: 'รายการหลักประกัน'
    },
    '미수금관리': {
      EN: 'Receivables management',
      JP: '未収金管理',
      CH: '应收款管理',
      TH: 'จัดการลูกหนี้'
    },
    '미수금내역': {
      EN: 'Receivables list',
      JP: '未収金履歴',
      CH: '应收款明细',
      TH: 'รายการลูกหนี้'
    },
    /* /hq/paymentOrchestration — 決済ロジック設定 */
    '결제로직설정': {
      EN: 'Payment orchestration',
      JP: '決済ロジック設定',
      CH: '支付编排',
      TH: 'การเรียงลำดับการชำระเงิน'
    },
    '결제대행 연동 핵심 정책입니다. 통합유형(API_BROKER/URL_PAY)별 결제 실행방식(INLINE/REDIRECT) 기본값과 URL결제 경로를 설정합니다.': {
      EN: 'Core policy for payment-acquirer integration. Set the default payment flow (INLINE/REDIRECT) per integration type (API_BROKER/URL_PAY) and the URL payment path.',
      JP: '決済代行連携の中核ポリシーです。統合タイプ（API_BROKER／URL_PAY）ごとに決済実行方式（INLINE／REDIRECT）の既定値とURL決済パスを設定します。',
      CH: '支付机构对接的核心策略。按集成类型（API_BROKER/URL_PAY）设置默认执行方式（INLINE/REDIRECT）及 URL 支付路径。',
      TH: 'นโยบายหลักของการเชื่อมต่อผู้ให้บริการชำระ ตั้งค่าโฟว์เริ่มต้น (INLINE/REDIRECT) ตามประเภท (API_BROKER/URL_PAY) และเส้นทาง URL ชำระ'
    },
    'API 중계형 기본 방식': {
      EN: 'API broker default flow',
      JP: 'API中継型の既定方式',
      CH: 'API 中继型默认方式',
      TH: 'โฟว์เริ่มต้นแบบ API broker'
    },
    'URL 결제형 기본 방식': {
      EN: 'URL payment default flow',
      JP: 'URL決済型の既定方式',
      CH: 'URL 支付型默认方式',
      TH: 'โฟว์เริ่มต้นแบบชำระ URL'
    },
    'URL 결제 경로 템플릿': {
      EN: 'URL payment path template',
      JP: 'URL決済パステンプレート',
      CH: 'URL 支付路径模板',
      TH: 'เทมเพลต path การชำระ URL'
    },
    'API 중계형 INLINE 제공': {
      EN: 'Offer API broker INLINE',
      JP: 'API中継型 INLINE の提供',
      CH: '提供 API 中继 INLINE',
      TH: 'เปิดใช้ INLINE แบบ API broker'
    },
    'API 중계형 REDIRECT 제공': {
      EN: 'Offer API broker REDIRECT',
      JP: 'API中継型 REDIRECT の提供',
      CH: '提供 API 中继 REDIRECT',
      TH: 'เปิดใช้ REDIRECT แบบ API broker'
    },
    'URL 결제형 INLINE 제공': {
      EN: 'Offer URL payment INLINE',
      JP: 'URL決済型 INLINE の提供',
      CH: '提供 URL 支付 INLINE',
      TH: 'เปิดใช้ INLINE แบบชำระ URL'
    },
    'URL 결제형 REDIRECT 제공': {
      EN: 'Offer URL payment REDIRECT',
      JP: 'URL決済型 REDIRECT の提供',
      CH: '提供 URL 支付 REDIRECT',
      TH: 'เปิดใช้ REDIRECT แบบชำระ URL'
    },
    'URL 재결제형 제공': {
      EN: 'Offer URL re-pay (saved card)',
      JP: 'URL再決済型の提供',
      CH: '提供 URL 再支付（保存卡）',
      TH: 'เปิดใช้ URL ชำระซ้ำ (บัตรที่บันทึก)'
    },
    'URL 재결제 경로 템플릿': {
      EN: 'URL re-pay path template',
      JP: 'URL再決済パステンプレート',
      CH: 'URL 再支付路径模板',
      TH: 'เทมเพลตเส้นทาง URL ชำระซ้ำ'
    },
    'URL 재결제 URL': {
      EN: 'URL re-pay URL',
      JP: 'URL再決済URL',
      CH: 'URL 再支付 URL',
      TH: 'URL ชำระซ้ำ'
    },
    'URL 결제 방식': {
      EN: 'URL checkout mode',
      JP: 'URL決済方式',
      CH: 'URL 结账方式',
      TH: 'โหมดชำระ URL'
    },
    '상품명 사용': {
      EN: 'Product name field',
      JP: '商品名入力',
      CH: '商品名称字段',
      TH: 'ช่องชื่อสินค้า'
    },
    '회사명 노출': {
      EN: 'Show company name',
      JP: '会社名表示',
      CH: '显示公司名称',
      TH: 'แสดงชื่อบริษัท'
    },
    '다국어 메뉴': {
      EN: 'Language menu',
      JP: '多言語メニュー',
      CH: '多语言菜单',
      TH: 'เมนูภาษา'
    },
    '일반 URL 결제': {
      EN: 'Standard URL payment',
      JP: '通常URL決済',
      CH: '普通 URL 支付',
      TH: 'ชำระ URL ปกติ'
    },
    '재결제 URL (저장 카드)': {
      EN: 'Re-pay URL (saved cards)',
      JP: '再決済URL（保存カード）',
      CH: '再支付 URL（保存卡）',
      TH: 'URL ชำระซ้ำ (บัตรที่บันทึก)'
    },
    'URL 결제 방식(공통)': {
      EN: 'URL checkout mode (shared)',
      JP: 'URL決済方式（共通）',
      CH: 'URL 结账方式（共用）',
      TH: 'โหมดชำระ URL (ใช้ร่วมกัน)'
    },
    '웹결제 설정에서 선택': {
      EN: 'Selected in web payment settings',
      JP: 'WEB決済設定で選択',
      CH: '在 WEB 支付设置中选择',
      TH: 'เลือกในการตั้งค่าชำระ WEB'
    },
    '공개 URL·챗봇 결제·API URL 인라인 중계 결제에 공통 적용됩니다. 변경은 위 「웹결제 사용 / 대표 기본상품정보」의 URL 결제 방식에서 하세요.': {
      EN: 'Applies to public URL, chatbot checkout, and API URL inline relay alike. Change it under Web payment » URL checkout mode.',
      JP: '公開URL・チャットボット決済・API URLインライン中継決済に共通適用されます。変更は上の「WEB決済使用/代表基本商品情報」のURL決済方式で行います。',
      CH: '同时适用于公开 URL、聊天机器人结账与 API URL 内联中继。请在上方「WEB 支付使用 / 代表基本商品信息」的 URL 结账方式中修改。',
      TH: 'ใช้ร่วมกับ URL สาธารณะ ชำระแชทบอท และ API URL inline relay เปลี่ยนที่ 「การใช้ชำระ WEB / ข้อมูลสินค้าหลัก」ด้านบน'
    },
    '미사용이면 로그인한 가맹점 관리자에게 챗봇관리의 상품관리 메뉴가 표시되지 않습니다. 「URL 결제 방식」은 챗봇 주문·카탈로그 결제에만 적용되며 공개 URL·API 중계와 별도로 선택할 수 있습니다. 재결제 URL 은 본사 URL 재결제 기능 ON 및 URL재결제 PG 바인딩이 필요합니다. 챗봇결제 URL은 챗봇 쇼핑·주문 진입용입니다.': {
      EN: 'If disabled, Product management under Chatbot admin is hidden. URL checkout mode applies to chatbot orders and catalog checkout only, and can be set separately from public URL and API relay. Re-pay URL requires HQ re-pay enabled and a URL re-pay PG binding. The chatbot payment URL is the chatbot storefront entry.',
      JP: '未使用の場合、チャットボット管理の商品管理メニューは非表示です。「URL決済方式」はチャットボット注文・カタログ決済のみに適用され、公開URL・API中継とは別に選択できます。再決済URLは本社URL再決済機能ONおよびURL再決済PGバインディングが必要です。チャットボット決済URLはチャットボットショップ・注文の入口です。',
      CH: '停用后隐藏聊天机器人管理中的商品管理。「URL 结账方式」仅用于聊天机器人订单与目录结账，可与公开 URL、API 中继分开设置。再支付 URL 需开启总部再支付功能并绑定 URL 再支付 PG。聊天机器人支付 URL 为商城入口。',
      TH: 'ปิดใช้งานแล้วซ่อนเมนูจัดการสินค้าแชทบอท 「โหมดชำระ URL」ใช้กับคำสั่งซื้อและแคตตาล็อกแชทบอทเท่านั้น ตั้งแยกจาก URL สาธารณะและ API relay ได้ URL ชำระซ้ำต้องเปิดฟีเจอร์ที่สำนักงานใหญ่และผูก PG URL ชำระแชทบอทเป็นทางเข้าร้าน'
    },
    'API URL 인라인 중계 결제': {
      EN: 'API URL inline relay checkout',
      JP: 'API URLインライン中継決済',
      CH: 'API URL 内联中继支付',
      TH: 'ชำระ API URL inline relay'
    },
    '가맹 API inline-checkout/prepare 호출 시 payUrl·결제창에 적용됩니다. 공개 URL·챗봇과 별도로 일반 URL/재결제 URL 을 선택할 수 있습니다. 재결제 URL 은 본사 URL 재결제 기능 ON 및 URL재결제 PG 바인딩(연동용도 URL재결제·운영 Y)이 필요합니다.': {
      EN: 'Applies to payUrl and checkout when calling merchant API inline-checkout/prepare. Standard / re-pay URL can be set separately from public URL and chatbot. Re-pay URL requires HQ re-pay enabled and an operational URL re-pay PG binding.',
      JP: '加盟店API inline-checkout/prepare 呼び出し時の payUrl・決済画面に適用されます。公開URL・チャットボットとは別に通常URL/再決済URLを選択できます。再決済URLは本社URL再決済機能ONおよびURL再決済PGバインディング(連動用途URL再決済・運用Y)が必要です。',
      CH: '调用商户 API inline-checkout/prepare 时应用于 payUrl 与结账页。可与公开 URL、聊天机器人分开选择普通 URL/再支付 URL。再支付 URL 需开启总部再支付功能并绑定 URL 再支付 PG（用途 URL 再支付·运营 Y）。',
      TH: 'ใช้กับ payUrl และหน้าชำระเมื่อเรียก merchant API inline-checkout/prepare เลือก URL ปกติ/ชำระซ้ำแยกจาก URL สาธารณะและแชทบอทได้ URL ชำระซ้ำต้องเปิดฟีเจอร์ที่สำนักงานใหญ่และผูก PG URL ชำระซ้ำ (operational Y)'
    },
    '본사 결제로직설정에서 URL 결제형 INLINE 제공이 Y 이어야 합니다. prepare 응답 payUrl 에 variant=repay 가 포함되면 재결제 URL 모드입니다.': {
      EN: 'HQ payment logic must have URL payment INLINE enabled (Y). If prepare response payUrl includes variant=repay, re-pay URL mode is active.',
      JP: '本社決済ロジック設定でURL決済型INLINE提供がYである必要があります。prepare応答 payUrl に variant=repay が含まれると再決済URLモードです。',
      CH: '总部支付逻辑设置中 URL 支付型 INLINE 提供须为 Y。若 prepare 响应 payUrl 含 variant=repay 则为再支付 URL 模式。',
      TH: 'ต้องเปิด URL payment INLINE (Y) ในการตั้งค่าตรรกะชำระที่สำนักงานใหญ่ หาก payUrl ใน prepare มี variant=repay คือโหมด URL ชำระซ้ำ'
    },
    '미사용 선택 시 WEB 결제 시스템이 중지됩니다. 「URL 결제 방식」은 공개 URL(/pay/업체코드) 결제에만 적용됩니다. API·챗봇 결제 방식은 각 설정 카드에서 별도 선택합니다. 재결제 URL 은 본사 URL 재결제 기능 ON 및 URL재결제 PG 바인딩이 필요합니다.': {
      EN: 'If disabled, web payment stops. URL checkout mode applies to public URL (/pay/{merchantCode}) only. API and chatbot modes are set on their own cards. Re-pay URL requires HQ re-pay enabled and a URL re-pay PG binding.',
      JP: '未使用の場合WEB決済は停止します。「URL決済方式」は公開URL(/pay/加盟店コード)のみに適用。API・チャットボットは各設定カードで別途選択。再決済URLは本社URL再決済機能ONおよびURL再決済PGバインディングが必要です。',
      CH: '选「未使用」则停止 WEB 支付。「URL 结账方式」仅用于公开 URL(/pay/商户代码)。API 与聊天机器人在各自设置卡片中单独选择。再支付 URL 需开启总部再支付功能并绑定 URL 再支付 PG。',
      TH: 'เลือกไม่ใช้จะหยุดระบบชำระ WEB 「โหมดชำระ URL」ใช้กับ URL สาธารณะ (/pay/รหัสร้าน) เท่านั้น API·แชทบอทเลือกแยกในการ์ดของตน URL ชำระซ้ำต้องเปิดฟีเจอร์ที่สำนักงานใหญ่และผูก PG'
    },
    'URL재결제': {
      EN: 'URL re-pay',
      JP: 'URL再決済',
      CH: 'URL再支付',
      TH: 'URLชำระซ้ำ'
    },
    '②-R URL재결제': {
      EN: '②-R URL re-pay',
      JP: '②-R URL再決済',
      CH: '②-R URL再支付',
      TH: '②-R URLชำระซ้ำ'
    },
    '저장 카드(CreditToken) 재결제 전용 공개 URL. 가맹 「URL 결제 방식」(공개 URL)·「API URL 인라인 중계 결제」·「챗봇결제 설정」 각각 재결제 URL 이면 해당 채널에 적용됩니다.': {
      EN: 'Public URL for saved-card (CreditToken) re-pay. When public URL, API URL inline relay, or chatbot URL checkout mode is re-pay, it applies to that channel.',
      JP: '保存カード(CreditToken)再決済用の公開URL。加盟店「URL決済方式」(公開URL)・「API URLインライン中継決済」・「チャットボット決済設定」それぞれが再決済URLの場合、該当チャネルに適用。',
      CH: '保存卡(CreditToken)再支付公开 URL。商户「URL 结账方式」（公开 URL）、「API URL 内联中继支付」、「聊天机器人支付设置」各自为再支付 URL 时，应用于对应渠道。',
      TH: 'URL สาธารณะสำหรับชำระซ้ำด้วยบัตรที่บันทึก (CreditToken) เมื่อโหมด URL สาธารณะ·API inline relay·แชทบอทเป็นชำระซ้ำ ใช้กับช่องทางนั้น'
    },
    /* JPAY API 구독 (③ 인라인) */
    'JPAY구독설정': {
      EN: 'JPAY subscription settings',
      JP: 'JPAY定期設定',
      CH: 'JPAY 订阅设置',
      TH: 'ตั้งค่าสมัคร JPAY'
    },
    '구독결제내역': {
      EN: 'Subscription payments',
      JP: '定期決済一覧',
      CH: '订阅支付记录',
      TH: 'รายการชำระสมัคร'
    },
    'JPAY구독내역': {
      EN: 'Subscription payments',
      JP: '定期決済一覧',
      CH: '订阅支付记录',
      TH: 'รายการชำระสมัคร'
    },
    'JPAY API 구독': {
      EN: 'JPAY API subscription',
      JP: 'JPAY API定期',
      CH: 'JPAY API 订阅',
      TH: 'สมัคร JPAY API'
    },
    'JPAY API 구독 사용': {
      EN: 'Use JPAY API subscription',
      JP: 'JPAY API定期利用',
      CH: '使用 JPAY API 订阅',
      TH: 'ใช้สมัคร JPAY API'
    },
    'JPAY API 구독 제공': {
      EN: 'Offer JPAY API subscription',
      JP: 'JPAY API定期提供',
      CH: '提供 JPAY API 订阅',
      TH: 'เปิดให้สมัคร JPAY API'
    },
    '구독 INLINE 제공': {
      EN: 'Subscription INLINE enabled',
      JP: '定期INLINE提供',
      CH: '提供订阅 INLINE',
      TH: 'เปิด Subscription INLINE'
    },
    '구독 경로 템플릿': {
      EN: 'Subscription path template',
      JP: '定期パステンプレート',
      CH: '订阅路径模板',
      TH: 'เทมเพลตพath สมัคร'
    },
    '기본 plan JSON': {
      EN: 'Default plan JSON',
      JP: '既定 plan JSON',
      CH: '默认 plan JSON',
      TH: 'plan JSON เริ่มต้น'
    },
    'API구독': {
      EN: 'API subscription',
      JP: 'API定期',
      CH: 'API 订阅',
      TH: 'สมัคร API'
    },
    '⑤ API구독': {
      EN: '⑤ API subscription',
      JP: '⑤ API定期',
      CH: '⑤ API 订阅',
      TH: '⑤ สมัคร API'
    },
    '가맹 API 인라인 구독(③) 전용입니다. URL·챗봇·1회 jpay-pay 와 분리됩니다.': {
      EN: 'For merchant API inline subscription (channel ③) only. Separate from URL, chatbot, and one-time jpay-pay.',
      JP: '加盟店APIインライン定期(③)専用です。URL・チャットボット・1回 jpay-pay とは分離されます。',
      CH: '仅用于商户 API 内联订阅（③）。与 URL、聊天机器人、一次性 jpay-pay 分离。',
      TH: 'สำหรับสมัคร inline ทาง Merchant API (ช่อง ③) เท่านั้น แยกจาก URL แชทบอท และ jpay-pay ครั้งเดียว'
    },
    '가맹 API subscription/prepare · jpay-subscribe.html 전용입니다. URL·챗봇·1회 inline-checkout 과 분리됩니다. 본사 결제로직설정 구독 ON + API연동설정 API구독 PG 바인딩 필요.': {
      EN: 'For merchant API subscription/prepare and jpay-subscribe.html only. Separate from URL, chatbot, and one-time inline-checkout. Requires HQ payment logic subscription ON and an API subscription PG binding in API integration.',
      JP: '加盟店API subscription/prepare・jpay-subscribe.html 専用です。URL・チャットボット・1回 inline-checkout とは分離。本社決済ロジック設定の定期ONおよびAPI連携設定のAPI定期PGバインディングが必要です。',
      CH: '仅用于商户 API subscription/prepare 与 jpay-subscribe.html。与 URL、聊天机器人、一次性 inline-checkout 分离。需在总部「支付逻辑设置」中开启订阅并在 API 联动中绑定 API 订阅 PG。',
      TH: 'สำหรับ merchant API subscription/prepare และ jpay-subscribe.html เท่านั้น แยกจาก URL แชทบอท และ inline-checkout ครั้งเดียว ต้องเปิดสมัครใน「ตั้งค่าตรรกะการชำระเงิน」ที่สำนักงานใหญ่และผูก PG สมัคร API'
    },
    'prepare: POST /api/middleware/v1/merchant/jpay/subscription/prepare · 해지: POST .../subscription/cancel (최초 orderNo)': {
      EN: 'prepare: POST /api/middleware/v1/merchant/jpay/subscription/prepare · cancel: POST .../subscription/cancel (original orderNo)',
      JP: 'prepare: POST /api/middleware/v1/merchant/jpay/subscription/prepare · 解約: POST .../subscription/cancel（初回 orderNo）',
      CH: 'prepare: POST /api/middleware/v1/merchant/jpay/subscription/prepare · 解约: POST .../subscription/cancel（首次 orderNo）',
      TH: 'prepare: POST /api/middleware/v1/merchant/jpay/subscription/prepare · ยกเลิก: POST .../subscription/cancel (orderNo แรก)'
    },
    '조회된 구독이 없습니다.': {
      EN: 'No subscriptions found.',
      JP: '該当する定期がありません。',
      CH: '没有查询到订阅。',
      TH: 'ไม่พบการสมัคร'
    },
    'JPAY API 구독 마스터(tb_merchant_jpay_subscription). 회차별 결제는 통합내역 origin=SUBSCRIPTION 을 참고하세요.': {
      EN: 'JPAY API subscription master (tb_merchant_jpay_subscription). For per-period charges, see integrated list with origin=SUBSCRIPTION.',
      JP: 'JPAY API定期マスタ(tb_merchant_jpay_subscription)。回次別決済は統合一覧 origin=SUBSCRIPTION を参照してください。',
      CH: 'JPAY API 订阅主表 (tb_merchant_jpay_subscription)。各期扣款请查 origin=SUBSCRIPTION 的整合明细。',
      TH: 'มาสเตอร์สมัคร JPAY API (tb_merchant_jpay_subscription) ชำระรายงวดดูรายการรวม origin=SUBSCRIPTION'
    },
    '구독TX': {
      EN: 'Subscription TX',
      JP: '定期TX',
      CH: '订阅 TX',
      TH: 'TX สมัคร'
    },
    '회차': {
      EN: 'Period',
      JP: '回次',
      CH: '期次',
      TH: 'งวด'
    },
    '해지': {
      EN: 'Cancelled',
      JP: '解約',
      CH: '解约',
      TH: 'ยกเลิกสมัคร'
    },
    'URL 결제 폼 설정': {
      EN: 'URL payment form settings',
      JP: 'URL決済フォーム設定',
      CH: 'URL 支付表单设置',
      TH: 'ตั้งค่าฟอร์มชำระ URL'
    },
    '공개 결제 URL(/pay/업체코드 등) 입력 화면입니다. 간편(SIMPLE)은 성명·상품·금액·DirectCreditToken(카드 데이터는 토큰/CCD에 포함)만 받고, 전체(FULL)는 연락처·배송지까지 받습니다. <strong>브라우저 탭 이름</strong>·<strong>파비콘</strong>은 이 결제 폼(탭 제목·탭 아이콘) 전용이며, 화면 하단 <strong>저장</strong>으로 DB에 반영됩니다. 인라인/리다이렉트는 위 「URL 결제형 기본 방식」과 제공 여부로 결정됩니다.': {
      EN: 'Public payment URL entry screen (/pay/{merchantCode}, etc.). SIMPLE collects name, product, amount, and DirectCreditToken (card data stays in token/CCD); FULL also collects contact and shipping address. <strong>Browser tab title</strong> and <strong>favicon</strong> apply only to this payment form (tab title/icon) and are saved to the DB with <strong>Save</strong> at the bottom. Inline vs redirect follows the “URL payment default flow” and availability above.',
      JP: '公開決済URL（/pay/加盟店コード 等）の入力画面です。簡易（SIMPLE）は氏名・商品・金額・DirectCreditToken（カードデータはトークン/CCD内）のみ、全体（FULL）は連絡先・配送先まで取得します。<strong>ブラウザタブ名</strong>・<strong>ファビコン</strong>はこの決済フォーム（タブタイトル・アイコン）専用で、画面下部の<strong>保存</strong>でDBに反映されます。インライン/リダイレクトは上の「URL決済型の既定方式」と提供可否で決まります。',
      CH: '公开支付 URL（/pay/商户代码 等）输入界面。简易（SIMPLE）仅收集姓名、商品、金额、DirectCreditToken（卡数据在 token/CCD）；完整（FULL）还收集联系方式与配送地址。<strong>浏览器标签标题</strong>与<strong>网站图标</strong>仅用于该支付表单（标签标题/图标），在页面底部<strong>保存</strong>写入数据库。内联/重定向由上方「URL 支付型默认方式」及是否提供决定。',
      TH: 'หน้าป้อนข้อมูล URL ชำระเงินสาธารณะ SIMPLE รับชื่อ·สินค้า·ยอด·DirectCreditToken เท่านั้น FULL เพิ่มที่อยู่ติดต่อ·ที่อยู่จัดส่ง ชื่อแท็บและ favicon ใช้กับฟอร์มนี้เท่านั้น บันทึกที่ปุ่มล่าง INLINE/REDIRECT ตามค่าเริ่มต้น URL ด้านบน'
    },
    '공개 결제 URL(/pay/업체코드 등) 입력 화면입니다. 간편(SIMPLE)은 성명·상품·금액·DirectCreditToken(카드 데이터는 토큰/CCD에 포함)만 받고, 전체(FULL)는 연락처·청구지까지 받습니다. <strong>브라우저 탭 이름</strong>·<strong>파비콘</strong>은 이 결제 폼(탭 제목·탭 아이콘) 전용이며, 화면 하단 <strong>저장</strong>으로 DB에 반영됩니다. 인라인/리다이렉트는 위 「URL 결제형 기본 방식」과 제공 여부로 결정됩니다.': {
      EN: 'Public payment URL entry screen (/pay/{merchantCode}, etc.). SIMPLE collects name, product, amount, and DirectCreditToken (card data stays in token/CCD); FULL also collects contact and billing address. <strong>Browser tab title</strong> and <strong>favicon</strong> apply only to this payment form (tab title/icon) and are saved to the DB with <strong>Save</strong> at the bottom. Inline vs redirect follows the “URL payment default flow” and availability above.',
      JP: '公開決済URL（/pay/加盟店コード 等）の入力画面です。簡易（SIMPLE）は氏名・商品・金額・DirectCreditToken（カードデータはトークン/CCD内）のみ、全体（FULL）は連絡先・請求先まで取得します。<strong>ブラウザタブ名</strong>・<strong>ファビコン</strong>はこの決済フォーム（タブタイトル・アイコン）専用で、画面下部の<strong>保存</strong>でDBに反映されます。インライン/リダイレクトは上の「URL決済型の既定方式」と提供可否で決まります。',
      CH: '公开支付 URL（/pay/商户代码 等）输入界面。简易（SIMPLE）仅收集姓名、商品、金额、DirectCreditToken（卡数据在 token/CCD）；完整（FULL）还收集联系方式与账单地址。<strong>浏览器标签标题</strong>与<strong>网站图标</strong>仅用于该支付表单（标签标题/图标），在页面底部<strong>保存</strong>写入数据库。内联/重定向由上方「URL 支付型默认方式」及是否提供决定。',
      TH: 'หน้าป้อนข้อมูล URL ชำระเงินสาธารณะ SIMPLE รับชื่อ·สินค้า·ยอด·DirectCreditToken เท่านั้น FULL เพิ่มที่อยู่เรียกเก็บ ชื่อแท็บและ favicon ใช้กับฟอร์มนี้เท่านั้น บันทึกที่ปุ่มล่าง INLINE/REDIRECT ตามค่าเริ่มต้น URL ด้านบน'
    },
    'URL 결제 입력 폼': {
      EN: 'URL payment input form',
      JP: 'URL決済入力フォーム',
      CH: 'URL 支付输入表单',
      TH: 'ฟอร์มป้อน URL ชำระ'
    },
    '전체 입력 (배송지·성명 분리)': {
      EN: 'Full input (shipping address · name split)',
      JP: '全体入力（配送先・氏名を分離）',
      CH: '完整输入（配送地址·姓名分开）',
      TH: 'กรอกแบบเต็ม (ที่อยู่จัดส่ง·แยกชื่อ)'
    },
    '전체 입력 (청구지·성명 분리)': {
      EN: 'Full input (billing address · name split)',
      JP: '全体入力（請求先・氏名を分離）',
      CH: '完整输入（账单地址·姓名分开）',
      TH: 'กรอกแบบเต็ม (ที่อยู่เรียกเก็บ·แยกชื่อ)'
    },
    '간편 입력 (필수 최소)': {
      EN: 'Simple input (minimum required)',
      JP: '簡易入力（必須最小）',
      CH: '简易输入（最少必填）',
      TH: 'กรอกแบบย่อ (ขั้นต่ำที่จำเป็น)'
    },
    '브라우저 탭 이름 (한국어)': {
      EN: 'Browser tab title (Korean)',
      JP: 'ブラウザタブ名（韓国語）',
      CH: '浏览器标签标题（韩语）',
      TH: 'ชื่อแท็บเบราว์เซอร์ (เกาหลี)'
    },
    '비우면 기본 «Payment»': {
      EN: 'If empty, defaults to «Payment»',
      JP: '空欄なら既定は «Payment»',
      CH: '留空则默认为 «Payment»',
      TH: 'ว่างไว้ใช้ค่าเริ่มต้น «Payment»'
    },
    '탭 제목 다국어': {
      EN: 'Tab title languages',
      JP: 'タブタイトル多言語',
      CH: '标签标题多语言',
      TH: 'ชื่อแท็บหลายภาษา'
    },
    '다국어는 숨김 JSON에 저장됩니다. 한국어를 바꾼 뒤 필요 시 다시 「탭 제목 다국어」를 누르세요.': {
      EN: 'Translations are stored in hidden JSON. After changing Korean, press “Tab title languages” again if needed.',
      JP: '多言語は非表示のJSONに保存されます。韓国語を変更したあと、必要に応じて再度「タブタイトル多言語」を押してください。',
      CH: '多语言保存在隐藏 JSON 中。修改韩语后如需可再次点击「标签标题多语言」。',
      TH: 'คำแปลเก็บใน JSON ที่ซ่อน แก้ภาษาเกาหลีแล้วกด「ชื่อแท็บหลายภาษา」อีกครั้งได้'
    },
    '파비콘 (탭 아이콘)': {
      EN: 'Favicon (tab icon)',
      JP: 'ファビコン（タブアイコン）',
      CH: '网站图标（标签图标）',
      TH: 'ไอคอนแท็บ (favicon)'
    },
    '업로드 후 경로가 표시됩니다': {
      EN: 'Path appears after upload',
      JP: 'アップロード後にパスが表示されます',
      CH: '上传后显示路径',
      TH: 'อัปโหลดแล้วจะแสดง path'
    },
    '찾기': { EN: 'Browse', JP: '参照', CH: '浏览', TH: 'เลือกไฟล์' },
    '업로드': { EN: 'Upload', JP: 'アップロード', CH: '上传', TH: 'อัปโหลด' },
    '제거': { EN: 'Remove', JP: '削除', CH: '移除', TH: 'ลบออก' },
    'PNG·JPG, 1MB 이하. 서버에서 32×32 PNG로 변환됩니다.': {
      EN: 'PNG/JPG, up to 1 MB. The server converts to 32×32 PNG.',
      JP: 'PNG/JPG、1MB以下。サーバーで32×32 PNGに変換されます。',
      CH: 'PNG/JPG，最大 1MB。服务器会转为 32×32 PNG。',
      TH: 'PNG/JPG ไม่เกิน 1MB เซิร์ฟเวอร์แปลงเป็น PNG 32×32'
    },
    '결제통화로직설정': {
      EN: 'Payment currency scale rules',
      JP: '決済通貨ロジック設定',
      CH: '支付币种换算规则',
      TH: 'กฎสเกลสกุลเงินชำระ'
    },
    '공개 결제 폼 금액은 아래 규칙에 따라 PG(칠리페이 등) API 금액으로 변환됩니다. 가맹점 URL 결제 <strong>운영</strong> PG(pg_cd)와 결제 통화가 일치하는 <strong>첫 번째</strong> 행이 적용됩니다. (예: ×100 — 입력 800 → 전송 80000) 목록을 바꾼 뒤 <strong>목록 저장(폼 반영)</strong> 또는 행별 <strong>수정 적용</strong>으로 숨김 필드를 맞춘 다음, 화면 맨 아래 <strong>저장</strong>으로 서버에 반영하세요.': {
      EN: 'Public payment form amounts are converted to the PG API amount (ChillPay, etc.) using the rules below. The <strong>first</strong> row whose operating URL-pay PG (pg_cd) and currency match the merchant applies (e.g. ×100: enter 800 → send 80000). After changing the list, use <strong>Save list (apply to form)</strong> or per-row <strong>Apply edit</strong> to sync the hidden field, then <strong>Save</strong> at the bottom to persist.',
      JP: '公開決済フォームの金額は下記ルールに従いPG（ChillPay等）API金額に変換されます。加盟店URL決済の<strong>運用</strong>PG（pg_cd）と決済通貨が一致する<strong>最初の</strong>行が適用されます（例: ×100 — 入力800→送信80000）。一覧を変更したら<strong>一覧保存（フォーム反映）</strong>または行ごとの<strong>修正適用</strong>で非表示フィールドを合わせ、画面最下部の<strong>保存</strong>でサーバーに反映してください。',
      CH: '公开支付表单金额按下列规则转换为 PG API 金额。与商户 URL 支付<strong>运营</strong> PG（pg_cd）及币种匹配的<strong>第一行</strong>生效。修改列表后请用<strong>保存列表（写入表单）</strong>或<strong>应用修改</strong>同步隐藏字段，再用底部<strong>保存</strong>提交服务器。',
      TH: 'ยอดในฟอร์มถูกแปลงตามกฎด้านล่าง ใช้แถวแรกที่ PG ปฏิบัติการ URL และสกุลเงินตรงกัน หลังแก้รายการให้กด「บันทึกรายการ」หรือ「นำการแก้ไขไปใช้」แล้วกด「บันทึก」ล่างสุด'
    },
    '결제대행사는 <strong>연동용도 URL결제(Y)</strong>인 PG만 선택할 수 있습니다. 목록·선택 상자 옆의 <strong>연동용도</strong>는 API연동설정의 노티·URL결제·챗봇·API 연동 여부를 나타냅니다. 아래에서 추가·수정·삭제한 뒤 <strong>목록 저장(폼 반영)</strong> → 화면 하단 <strong>저장</strong> 순서로 저장합니다.': {
      EN: 'Only PGs with <strong>integration use URL payment (Y)</strong> can be selected. <strong>Integration use</strong> beside the list shows notify / URL pay / chatbot / API flags from API integration settings. Add, edit, or delete below, then <strong>Save list (apply to form)</strong> → <strong>Save</strong> at the bottom.',
      JP: '<strong>連携用途がURL決済(Y)</strong>のPGのみ選択できます。一覧・選択欄横の<strong>連携用途</strong>はAPI連携設定のノティ・URL決済・チャットボット・APIの有無を示します。下で追加・修正・削除したあと<strong>一覧保存（フォーム反映）</strong>→画面下部<strong>保存</strong>の順で保存します。',
      CH: '仅可选择<strong>集成用途为 URL 支付(Y)</strong> 的 PG。列表旁的<strong>集成用途</strong>表示 API 联动设置中的通知/URL/聊天/API 标志。在下方增删改后按<strong>保存列表（写入表单）</strong>→底部<strong>保存</strong>。',
      TH: 'เลือกได้เฉพาะ PG ที่<strong>การเชื่อม URL ชำระ (Y)</strong> คอลัมน์「การเชื่อม」แสดงแจ้งเตือน/URL/แชทบอท/API แก้ด้านล่างแล้วกด「บันทึกรายการ」แล้ว「บันทึก」'
    },
    '추가': { EN: 'Add', JP: '追加', CH: '添加', TH: 'เพิ่ม' },
    '수정 적용': {
      EN: 'Apply edit',
      JP: '修正を適用',
      CH: '应用修改',
      TH: 'นำการแก้ไขไปใช้'
    },
    '수정 취소': {
      EN: 'Cancel edit',
      JP: '修正をキャンセル',
      CH: '取消编辑',
      TH: 'ยกเลิกการแก้ไข'
    },
    '목록 저장(폼 반영)': {
      EN: 'Save list (apply to form)',
      JP: '一覧保存（フォーム反映）',
      CH: '保存列表（写入表单）',
      TH: 'บันทึกรายการ (ลงฟอร์ม)'
    },
    '이전 방식 호환: ': {
      EN: 'Legacy compatibility: ',
      JP: '旧方式互換: ',
      CH: '旧版兼容：',
      TH: 'โหมดเก่า: '
    },
    '빈 행을 바로 목록에 넣기': {
      EN: 'Insert empty row into list',
      JP: '空行を一覧に直接追加',
      CH: '将空行直接插入列表',
      TH: 'แทรกแถวว่างในรายการ'
    },
    'BOT(태국은행) 일평균 환율 API': {
      EN: 'BOT (Bank of Thailand) daily average FX API',
      JP: 'BOT（タイ国銀）日平均為替API',
      CH: '泰国央行日均汇率 API',
      TH: 'API อัตราเฉลี่ยรายวัน BOT'
    },
    'URL 표시통화 → 실결제 THB': {
      EN: 'URL display currency → settled THB',
      JP: 'URL表示通貨→実決済THB',
      CH: 'URL 展示币种→实际结算 THB',
      TH: 'สกุลแสดง URL → THB จริง'
    },
    '결제구문설정': {
      EN: 'Payment copy settings',
      JP: '決済文言設定',
      CH: '支付文案设置',
      TH: 'ข้อความหน้าชำระ'
    },
    '확장형 PG 레지스트리': {
      EN: 'Extended PG registry',
      JP: '拡張型PGレジストリ',
      CH: '扩展型 PG 注册表',
      TH: 'ทะเบียน PG แบบขยาย'
    },
    '향후 PG사 추가를 위해 벤더별 기능/방식/엔드포인트를 JSON으로 관리합니다. 기본 구조를 유지한 채 vendors 배열에 계속 추가하면 됩니다.': {
      EN: 'Manage per-vendor features, flows, and endpoints in JSON for future PG additions. Keep the base shape and keep appending to the vendors array.',
      JP: '将来のPG追加のため、ベンダー別の機能・方式・エンドポイントをJSONで管理します。基本構造を保ったままvendors配列に追加し続けられます。',
      CH: '以 JSON 管理各厂商功能、方式与端点，便于后续新增 PG。保持基础结构并向 vendors 数组追加即可。',
      TH: 'จัดการฟีเจอร์/โฟว์/เอนด์พอยต์ต่อเวนเดอร์เป็น JSON เพื่อเพิ่ม PG ภายหลัง คงโครงพื้นฐานแล้วเพิ่มใน vendors'
    },
    '결제연동 레지스트리(JSON)': {
      EN: 'Payment integration registry (JSON)',
      JP: '決済連携レジストリ(JSON)',
      CH: '支付对接注册表(JSON)',
      TH: 'ทะเบียนเชื่อมชำระ (JSON)'
    },
    'URL결제': {
      EN: 'URL pay',
      JP: 'URL決済',
      CH: 'URL 支付',
      TH: 'ชำระ URL'
    },
    '배율': { EN: 'Scale', JP: '倍率', CH: '换算倍率', TH: 'อัตราสเกล' },
    'API': { EN: 'API', JP: 'API', CH: 'API', TH: 'API' },
    '= 동일': {
      EN: '= same',
      JP: '= 同一',
      CH: '= 相同',
      TH: '= เท่าเดิม'
    },
    '×100': { EN: '×100', JP: '×100', CH: '×100', TH: '×100' },
    '÷100': { EN: '÷100', JP: '÷100', CH: '÷100', TH: '÷100' },
    'URL 표시통화→THB 등에 쓰는 BOT Stat-ExchangeRate 호출값입니다. 칸을 비우면 서버 application.yml·환경변수(BOT_THAILAND_*)를 따릅니다. (A) 레거시 iAPI: Base https://iapi.bot.or.th, Path /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/, 헤더 이름 api-key. (B) API 포털 v2: Base https://gateway.api.bot.or.th/Stat-ExchangeRate/v2, Path /DAILY_AVG_EXG_RATE/, 헤더 이름 Authorization(값=구독 Client ID).': {
      EN: 'BOT Stat-ExchangeRate settings for URL display currency → THB, etc. Leave fields blank to use server application.yml and env vars (BOT_THAILAND_*). (A) Legacy iAPI: Base https://iapi.bot.or.th, Path /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/, header name api-key. (B) API portal v2: Base https://gateway.api.bot.or.th/Stat-ExchangeRate/v2, Path /DAILY_AVG_EXG_RATE/, header name Authorization (value = subscription Client ID).',
      JP: 'URL表示通貨→THB等に使うBOT Stat-ExchangeRateの呼び出し値です。空欄ならサーバーのapplication.yml・環境変数(BOT_THAILAND_*)に従います。(A) レガシーiAPI: Base https://iapi.bot.or.th, Path /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/, ヘッダ名 api-key。(B) APIポータルv2: Base https://gateway.api.bot.or.th/Stat-ExchangeRate/v2, Path /DAILY_AVG_EXG_RATE/, ヘッダ名 Authorization（値=サブスクClient ID）。',
      CH: '用于 URL 展示币种→THB 等的 BOT Stat-ExchangeRate 调用参数。留空则遵循服务器 application.yml 与环境变量 BOT_THAILAND_*。(A) 旧版 iAPI：Base https://iapi.bot.or.th，Path /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/，请求头名称 api-key。(B) API 门户 v2：Base https://gateway.api.bot.or.th/Stat-ExchangeRate/v2，Path /DAILY_AVG_EXG_RATE/，请求头名称 Authorization（值=订阅 Client ID）。',
      TH: 'ค่าเรียก BOT Stat-ExchangeRate สำหรับสกุลแสดง URL→THB เป็นต้น ว่างไว้จะใช้ application.yml ของเซิร์ฟเวอร์ / ตัวแปร BOT_THAILAND_* (A) iAPI แบบเดิม: Base https://iapi.bot.or.th, Path /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/, ชื่อหัวข้อ api-key (B) พอร์ทัล API v2: Base https://gateway.api.bot.or.th/Stat-ExchangeRate/v2, Path /DAILY_AVG_EXG_RATE/, ชื่อหัวข้อ Authorization (ค่า=Client ID จากการสมัคร)'
    },
    'Base URL': {
      EN: 'Base URL',
      JP: 'Base URL',
      CH: 'Base URL',
      TH: 'Base URL'
    },
    'API 키(Client ID)': {
      EN: 'API key (Client ID)',
      JP: 'APIキー（Client ID）',
      CH: 'API 密钥（Client ID）',
      TH: 'API key (Client ID)'
    },
    '인증 헤더 이름': {
      EN: 'Auth header name',
      JP: '認証ヘッダ名',
      CH: '认证头名称',
      TH: 'ชื่อเฮดเดอร์ยืนยันตัวตน'
    },
    '비우면 BOT_THAILAND_API_KEY': {
      EN: 'If empty: BOT_THAILAND_API_KEY',
      JP: '空欄なら BOT_THAILAND_API_KEY',
      CH: '留空则使用 BOT_THAILAND_API_KEY',
      TH: 'ว่างไว้ใช้ BOT_THAILAND_API_KEY'
    },
    'api-key 또는 Authorization': {
      EN: 'api-key or Authorization',
      JP: 'api-key または Authorization',
      CH: 'api-key 或 Authorization',
      TH: 'api-key หรือ Authorization'
    },
    '일평균 경로': {
      EN: 'Daily average path',
      JP: '日平均レートのパス',
      CH: '日均汇率路径',
      TH: 'พาธอัตราเฉลี่ยรายวัน'
    },
    'PG': { EN: 'PG', JP: 'PG', CH: 'PG', TH: 'PG' },
    '공개 결제 폼의 <strong>카드 *</strong> 제목·안내 문단과 <strong>결제 금액</strong> 입력란 아래 통화(×100/÷100) 안내는 <strong>PG(결제대행사)별</strong>로 함께 저장됩니다. 금액 안내는 「내용 1」 위에서 노출 여부·문구를 넣습니다(비우면 페이지 기본 다국어 문구). <strong>URL 결제 결과 문구</strong>는 <code>pay-result.html</code> 및 결제 페이지 인라인 완료 카드의 성공/실패 큰 제목·하단 안내를 바꿉니다(비우면 기본 문구). 취소 화면은 실패 문구와 동일 설정을 씁니다. (탭 제목·파비콘은 위 「URL 결제 폼 설정」에서 설정합니다.) URL 결제 연동(<strong>연동용도 URL결제</strong>) PG만 선택할 수 있습니다. 입력 후 <strong>저장</strong>으로 아래 목록에 넣고, 목록에서 <strong>활성</strong>을 켜야 반영됩니다. <strong>저장(다국어)</strong>은 본사 API가 MyMemory로 프록시하여 ENG·CHN·JPN·THA 초안을 채웁니다. 화면 맨 아래 <strong>저장</strong>으로 DB에 반영합니다. 총판 로고가 있으면 결제 폼 상단은 ICOPAY 대신 로고가 나옵니다(별도 연동).': {
      EN: 'Card section title/body and the amount-field footnotes (×100/÷100) are stored <strong>per PG</strong>. Amount note visibility/text is set above “Body 1” (empty = page default i18n). <strong>URL payment result copy</strong> changes success/fail titles on pay-result.html and inline completion cards. Cancel uses the fail copy. (Tab title/favicon: “URL payment form settings” above.) Only URL-pay PGs may be selected. Use <strong>Save</strong> to add rows, then enable <strong>Active</strong>. <strong>Save (i18n)</strong> drafts ENG/CHN/JPN/THA via HQ API (MyMemory). Persist with bottom <strong>Save</strong>. Distributor logo replaces ICOPAY header when configured.',
      JP: '公開決済フォームの<strong>カード*</strong>見出し・本文と<strong>決済金額</strong>入力欄下の通貨(×100/÷100)案内は<strong>PG（決済代行）ごと</strong>に保存されます。金額案内は「内容1」上で表示可否・文言を設定（空欄はページ既定の多言語）。<strong>URL決済結果の文言</strong>はpay-result.htmlとインライン完了カードの成功/失敗見出し・脚注を変更します。キャンセル画面は失敗と同じ設定。タブ名・ファビコンは上の「URL決済フォーム設定」。<strong>連携用途URL決済</strong>のPGのみ選択。<strong>保存</strong>で一覧に追加し<strong>有効</strong>をオンに。MyMemory経由の<strong>保存（多言語）</strong>。最下部<strong>保存</strong>でDB反映。総代理ロゴがあるとヘッダはICOPAYの代わりにロゴ。',
      CH: '公开支付表单的「卡*」标题/说明与「支付金额」下方的 ×100/÷100 说明按 **PG** 保存。金额说明在「内容1」上方设置。URL 支付结果文案影响 pay-result.html 等。仅可选择 URL 支付对接 PG。保存加入列表后需开启**启用**。**保存(多语言)** 通过 MyMemory 填草稿。底部**保存**写入数据库。有总代 logo 时顶部显示 logo。',
      TH: 'หัวข้อการ์ดและคำอธิบายยอด ×100/÷100 เก็บต่อ PG ผลลัพธ์ URL แก้ pay-result.html ฯลฯ เลือกเฉพาะ PG ที่เชื่อม URL ชำระ บันทึกลงรายการแล้วเปิดใช้งาน บันทึกหลายภาษาเติม ENG/CHN/JPN/THA'
    },
    '결제대행사 (URL결제)': {
      EN: 'PG (URL payment)',
      JP: '決済代行（URL決済）',
      CH: '支付机构（URL 支付）',
      TH: 'PG (ชำระ URL)'
    },
    '제목 (한국어) — 결제 폼 «카드 *» 제목': {
      EN: 'Title (Korean) — “Card *” heading on the form',
      JP: 'タイトル（韓国語）— 決済フォーム「カード*」見出し',
      CH: '标题（韩语）— 支付表单「卡*」标题',
      TH: 'หัวข้อ (เกาหลี) — หัวข้อ «บัตร*»'
    },
    '예: 카드': {
      EN: 'e.g. Card',
      JP: '例: カード',
      CH: '例：卡',
      TH: 'เช่น บัตร'
    },
    '결제 금액 하단 안내 (통화 스케일·PG별)': {
      EN: 'Footnote under payment amount (scale · per PG)',
      JP: '決済金額下の案内（通貨スケール・PG別）',
      CH: '支付金额下方说明（换算·按 PG）',
      TH: 'คำอธิบายใต้ยอด (สเกล·ต่อ PG)'
    },
    '금액 입력란 아래 안내 문구 노출': {
      EN: 'Show amount footnote',
      JP: '金額入力欄下の案内文を表示',
      CH: '显示金额输入框下方说明',
      TH: 'แสดงคำอธิบายใต้ช่องยอด'
    },
    '안내 문구 (한국어, 비우면 페이지 기본 ×100/÷100 문구)': {
      EN: 'Notice (Korean; empty = page default ×100/÷100 text)',
      JP: '案内文（韓国語。空欄ならページ既定の×100/÷100）',
      CH: '说明（韩语；留空用页面默认 ×100/÷100 文案）',
      TH: 'ข้อความ (เกาหลี) ว่างใช้ค่าเริ่มต้น ×100/÷100'
    },
    '예: 본사 「결제통화로직설정」에 따라 입력 금액과 결제 대행사로 전달되는 금액의 관계를 안내합니다.': {
      EN: 'e.g. Explain how the entered amount maps to the amount sent to the acquirer per HQ “payment currency scale rules”.',
      JP: '例: 本社の「決済通貨ロジック設定」に従い、入力金額と決済代行へ送る金額の関係を案内します。',
      CH: '例：根据总部「支付币种换算规则」说明输入金额与提交给支付机构的金额关系。',
      TH: 'เช่น อธิบายความสัมพันธ์ยอดที่ป้อนกับยอดที่ส่งไป PG ตามกฎสเกลของสำนักงานใหญ่'
    },
    '내용 1 (한국어)': {
      EN: 'Body 1 (Korean)',
      JP: '本文1（韓国語）',
      CH: '内容1（韩语）',
      TH: 'เนื้อหา 1 (เกาหลี)'
    },
    '내용 2 (한국어)': {
      EN: 'Body 2 (Korean)',
      JP: '本文2（韓国語）',
      CH: '内容2（韩语）',
      TH: 'เนื้อหา 2 (เกาหลี)'
    },
    '내용 3 (한국어)': {
      EN: 'Body 3 (Korean)',
      JP: '本文3（韓国語）',
      CH: '内容3（韩语）',
      TH: 'เนื้อหา 3 (เกาหลี)'
    },
    'URL 결제 결과 화면 (성공/실패 큰 글씨·하단 안내)': {
      EN: 'URL payment result screen (success/fail titles · footnotes)',
      JP: 'URL決済結果画面（成功/失敗の大見出し・脚注）',
      CH: 'URL 支付结果页（成功/失败大标题与底部说明）',
      TH: 'หน้าผล URL (หัวใหญ่สำเร็จ/ล้มเหลว·คำล่าง)'
    },
    '성공 — 안내 제목 (한국어)': {
      EN: 'Success — title (Korean)',
      JP: '成功 — 案内タイトル（韓国語）',
      CH: '成功 — 标题（韩语）',
      TH: 'สำเร็จ — หัวข้อ (เกาหลี)'
    },
    '성공 — 하단 안내 (한국어)': {
      EN: 'Success — footnote (Korean)',
      JP: '成功 — 下段案内（韓国語）',
      CH: '成功 — 底部说明（韩语）',
      TH: 'สำเร็จ — คำล่าง (เกาหลี)'
    },
    '실패·미완료 — 안내 제목 (한국어)': {
      EN: 'Fail / incomplete — title (Korean)',
      JP: '失敗・未完了 — 案内タイトル（韓国語）',
      CH: '失败/未完成 — 标题（韩语）',
      TH: 'ล้มเหลว/ไม่สมบูรณ์ — หัวข้อ (เกาหลี)'
    },
    '실패·미완료 — 하단 안내 (한국어)': {
      EN: 'Fail / incomplete — footnote (Korean)',
      JP: '失敗・未完了 — 下段案内（韓国語）',
      CH: '失败/未完成 — 底部说明（韩语）',
      TH: 'ล้มเหลว/ไม่สมบูรณ์ — คำล่าง (เกาหลี)'
    },
    '저장(다국어)': {
      EN: 'Save (i18n)',
      JP: '保存（多言語）',
      CH: '保存（多语言）',
      TH: 'บันทึก (หลายภาษา)'
    },
    '이 블록의 <strong>저장</strong>은 목록에만 반영됩니다. 서버(DB) 반영은 화면 맨 아래 <strong>저장</strong>이 필요합니다.': {
      EN: 'This block’s <strong>Save</strong> updates the list only. Use the bottom <strong>Save</strong> to persist to the server (DB).',
      JP: 'このブロックの<strong>保存</strong>は一覧のみ更新します。サーバー(DB)反映は画面最下部の<strong>保存</strong>が必要です。',
      CH: '此区域的<strong>保存</strong>仅更新列表。写入服务器（数据库）需使用页面底部<strong>保存</strong>。',
      TH: '「บันทึก」ในบล็อกนี้อัปเดตเฉพาะรายการ ต้องกด「บันทึก」ล่างสุดเพื่อลง DB'
    },
    '결제대행사를 선택하세요.': {
      EN: 'Select a payment provider.',
      JP: '決済代行を選択してください。',
      CH: '请选择支付机构。',
      TH: 'เลือกผู้ให้บริการชำระ'
    },
    '통화를 선택하세요.': {
      EN: 'Select a currency.',
      JP: '通貨を選択してください。',
      CH: '请选择币种。',
      TH: 'เลือกสกุลเงิน'
    },
    '이 규칙 행을 삭제할까요?': {
      EN: 'Delete this rule row?',
      JP: 'この規則行を削除しますか？',
      CH: '要删除此行规则吗？',
      TH: 'ลบแถวกฎนี้หรือไม่'
    },
    '수정 중인 행이 있습니다. 추가하면 수정 모드가 취소됩니다. 계속할까요?': {
      EN: 'A row is being edited. Adding will cancel edit mode. Continue?',
      JP: '修正中の行があります。追加すると修正モードが解除されます。続行しますか？',
      CH: '正在编辑一行。添加将取消编辑模式。是否继续？',
      TH: 'กำลังแก้แถวอยู่ การเพิ่มจะยกเลิกโหมดแก้ไข ดำเนินต่อหรือไม่'
    },
    '먼저 목록에서 「수정」을 눌러 편집할 행을 선택하세요.': {
      EN: 'First click “Edit” on a list row to select it.',
      JP: '先に一覧で「修正」を押して編集する行を選んでください。',
      CH: '请先在列表中点击「修改」选择要编辑的行。',
      TH: 'กด「แก้ไข」ในรายการเพื่อเลือกแถวก่อน'
    },
    '선택한 행이 반영되었습니다. 서버 저장은 화면 하단 「저장」을 누르세요.': {
      EN: 'The selected row was applied. Press Save at the bottom to persist to the server.',
      JP: '選択行を反映しました。サーバー保存は画面下部の「保存」を押してください。',
      CH: '已应用所选行。保存到服务器请点击底部「保存」。',
      TH: 'นำแถวที่เลือกไปใช้แล้ว กด「บันทึก」ล่างสุดเพื่อบันทึกเซิร์ฟเวอร์'
    },
    '목록이 숨김 필드에 반영되었습니다. 서버에 저장하려면 화면 하단 「저장」을 누르세요.': {
      EN: 'The list was written to the hidden field. Press Save at the bottom to persist to the server.',
      JP: '一覧を非表示フィールドに反映しました。サーバー保存は画面下部の「保存」を押してください。',
      CH: '列表已写入隐藏字段。保存到服务器请点击底部「保存」。',
      TH: 'เขียนรายการลงฟิลด์ซ่อนแล้ว กด「บันทึก」ล่างสุดเพื่อบันทึกเซิร์ฟเวอร์'
    },
    '수정 중인 행이 있습니다. 취소하고 빈 행을 추가할까요?': {
      EN: 'A row is being edited. Cancel editing and add an empty row?',
      JP: '修正中の行があります。キャンセルして空行を追加しますか？',
      CH: '正在编辑一行。取消并添加空行吗？',
      TH: 'กำลังแก้แถวอยู่ ยกเลิกแล้วเพิ่มแถวว่างหรือไม่'
    },
    '등록된 규칙이 없습니다. 위에서 PG·통화·배율을 고른 뒤 「추가」하거나, 아래 링크로 빈 행을 넣을 수 있습니다. (없으면 금액은 PG에 그대로 전달됩니다.)': {
      EN: 'No rules yet. Pick PG, currency, and scale above then “Add”, or use the link below for an empty row. (Without rules, amounts are sent to the PG unchanged.)',
      JP: '登録された規則がありません。上でPG・通貨・倍率を選んで「追加」するか、下のリンクで空行を入れられます（無い場合は金額はそのままPGへ）。',
      CH: '尚无规则。在上方选择 PG、币种、倍率后点「添加」，或用下方链接插入空行（无规则时金额原样传给 PG）。',
      TH: 'ยังไม่มีกฎ เลือก PG สกุลเงิน อัตราแล้วกด「เพิ่ม」หรือใช้ลิงก์แถวว่าง (ไม่มีกฎส่งยอดเท่าเดิม)'
    },
    '한국어 탭 제목을 입력하세요.': {
      EN: 'Enter the Korean tab title.',
      JP: '韓国語のタブタイトルを入力してください。',
      CH: '请输入韩语标签标题。',
      TH: 'กรอกชื่อแท็บภาษาเกาหลี'
    },
    '번역 요청 실패': {
      EN: 'Translation request failed',
      JP: '翻訳リクエストに失敗しました',
      CH: '翻译请求失败',
      TH: 'คำขอแปลล้มเหลว'
    },
    '파일을 선택하세요.': {
      EN: 'Choose a file.',
      JP: 'ファイルを選択してください。',
      CH: '请选择文件。',
      TH: 'เลือกไฟล์'
    },
    'API를 사용할 수 없습니다.': {
      EN: 'The API is not available.',
      JP: 'APIを使用できません。',
      CH: '无法使用 API。',
      TH: 'ใช้ API ไม่ได้'
    },
    '업로드 API를 사용할 수 없습니다.': {
      EN: 'Upload API is not available.',
      JP: 'アップロードAPIを使用できません。',
      CH: '无法使用上传 API。',
      TH: 'ใช้ API อัปโหลดไม่ได้'
    },
    '업로드 응답에 URL이 없습니다.': {
      EN: 'Upload response had no URL.',
      JP: 'アップロード応答にURLがありません。',
      CH: '上传响应中没有 URL。',
      TH: 'การตอบกลับอัปโหลดไม่มี URL'
    },
    '업로드 실패': {
      EN: 'Upload failed',
      JP: 'アップロードに失敗しました',
      CH: '上传失败',
      TH: 'อัปโหลดล้มเหลว'
    },
    '목록에서 선택한 행을 수정 중입니다. 반영하려면 「저장」 또는 「저장(다국어)」을 누르세요.': {
      EN: 'Editing the selected row. Press “Save” or “Save (i18n)” to apply.',
      JP: '一覧で選んだ行を修正中です。「保存」または「保存（多言語）」で反映してください。',
      CH: '正在编辑所选行。点击「保存」或「保存(多语言)」以应用。',
      TH: 'กำลังแก้แถวที่เลือก กด「บันทึก」หรือ「บันทึก (หลายภาษา)」เพื่อนำไปใช้'
    },
    '등록된 결제구문이 없습니다. 위에서 입력 후 「저장」으로 목록에 추가하세요.': {
      EN: 'No payment copy rows yet. Fill in above and press “Save” to add to the list.',
      JP: '登録された決済文言がありません。上で入力し「保存」で一覧に追加してください。',
      CH: '暂无支付文案。在上方填写后点「保存」加入列表。',
      TH: 'ยังไม่มีข้อความชำระ กรอกด้านบนแล้วกด「บันทึก」เพื่อเพิ่ม'
    },
    '숨김': { EN: 'Hidden', JP: '非表示', CH: '隐藏', TH: 'ซ่อน' },
    '제목·내용1·내용2·내용3(한국어)를 모두 채운 항목만 활성화할 수 있습니다.': {
      EN: 'Only rows with Korean title and bodies 1–3 filled can be activated.',
      JP: 'タイトル・本文1〜3（韓国語）をすべて埋めた行のみ有効化できます。',
      CH: '仅当韩语标题与内容1–3全部填写时可启用。',
      TH: 'เปิดใช้ได้เฉพาะแถวที่กรอกหัวข้อและเนื้อหา 1–3 ครบ'
    },
    '제목·내용1·내용2·내용3(한국어)를 모두 입력하세요.': {
      EN: 'Enter Korean title and bodies 1–3.',
      JP: 'タイトル・本文1〜3（韓国語）をすべて入力してください。',
      CH: '请填写韩语标题与内容1–3。',
      TH: 'กรอกหัวข้อและเนื้อหา 1–3 ภาษาเกาหลีให้ครบ'
    },
    '목록이 갱신되었습니다. 서버 반영은 화면 하단 「저장」을 누르세요.': {
      EN: 'List updated. Press Save at the bottom to persist to the server.',
      JP: '一覧を更新しました。サーバー反映は画面下部の「保存」を押してください。',
      CH: '列表已更新。保存到服务器请点击底部「保存」。',
      TH: 'อัปเดตรายการแล้ว กด「บันทึก」ล่างสุดเพื่อบันทึกเซิร์ฟเวอร์'
    },
    '목록이 갱신되었습니다(다국어). 서버 반영은 화면 하단 「저장」을 누르세요.': {
      EN: 'List updated (i18n). Press Save at the bottom to persist to the server.',
      JP: '一覧を更新しました（多言語）。サーバー反映は画面下部の「保存」を押してください。',
      CH: '列表已更新（多语言）。保存到服务器请点击底部「保存」。',
      TH: 'อัปเดตรายการ (หลายภาษา) แล้ว กด「บันทึก」ล่างสุด'
    },
    '번역 요청 중 오류가 있었습니다. 한국어만 반영합니다.': {
      EN: 'Translation request had an error. Korean only was applied.',
      JP: '翻訳リクエストでエラーがありました。韓国語のみ反映します。',
      CH: '翻译请求出错，仅应用韩语。',
      TH: 'แปลผิดพลาด ใช้เฉพาะภาษาเกาหลี'
    },
    '행 {N} 수정 중 — 값을 바꾼 뒤 「수정 적용」을 누르세요.': {
      EN: 'Editing row {N} — change values then press “Apply edit”.',
      JP: '行{N}を修正中 — 値を変更してから「修正を適用」を押してください。',
      CH: '正在编辑第 {N} 行 — 修改值后点击「应用修改」。',
      TH: 'กำลังแก้แถว {N} — แก้ค่าแล้วกด「นำการแก้ไขไปใช้」'
    },
    '카드 정보는 위 각 칸 안의 ChillPay 보안 입력(iframe)에서만 입력합니다. 카드 명의(Name on card)도 위 칸에만 입력하면 되며, iframe 밖에서는 값을 읽을 수 없어 하단 이름·성 입력은 표시하지 않습니다. ChillPay가 안전하게 처리하며, 당사 서버로 카드번호 평문이 전달되지 않습니다.': {
      EN: 'Enter card data only in ChillPay’s secure iframe fields above. Name on card is entered there too; name fields below are hidden because values cannot be read outside the iframe. ChillPay handles security; full card numbers are not sent to our server in clear text.',
      JP: 'カード情報は上の各欄内のChillPayセキュア入力(iframe)でのみ入力します。カード名義も同じくiframe内のみ。外では値を読めないため下の氏名入力は表示しません。ChillPayが安全に処理し、当社サーバーにカード番号の平文は送られません。',
      CH: '卡信息仅在上方 ChillPay 安全 iframe 中输入。卡面姓名也在该 iframe 内填写；iframe 外无法读取值故不显示下方姓名栏。ChillPay 安全处理，卡号明文不会传到本公司服务器。',
      TH: 'กรอกข้อมูลบัตรเฉพาะใน iframe ของ ChillPay ด้านบน ชื่อบนบัตรกรอกใน iframe เช่นกัน ไม่แสดงช่องชื่อล่างเพราะอ่านค่านอก iframe ไม่ได้ ChillPay จัดการความปลอดภัย หมายเลขบัตรไม่ส่งแบบข้อความเปล่า'
    },
    '카드 입력은 연동된 PG사 보안 위젯(iframe 등)에서 제공합니다. 브랜드별 자릿수·CVV 규칙은 해당 PG가 처리하며, ChillPay는 AMEX 미지원입니다. 다른 PG 연동 시 같은 결제 껍데기 안에서 벤더별 위젯으로 갈아끼우는 형태가 일반적입니다.': {
      EN: 'Card entry is provided by the linked PG’s secure widget (iframe, etc.). Digit/CVV rules are handled by that PG; ChillPay does not support AMEX. With other PGs, the same payment shell typically swaps vendor-specific widgets.',
      JP: 'カード入力は連携したPGのセキュアウィジェット(iframe等)で提供されます。桁数・CVVは各PGが処理し、ChillPayはAMEX非対応です。他PGでは同じ決済枠内でベンダー別ウィジェットに切り替える形が一般的です。',
      CH: '卡输入由所对接 PG 的安全组件（iframe 等）提供。位数与 CVV 规则由该 PG 处理；ChillPay 不支持 AMEX。对接其他 PG 时通常在相同支付外壳内切换各厂商组件。',
      TH: 'การป้อนบัตรมาจากวิดเจ็ตของ PG ที่เชื่อม กฎหลัก·CVV ตาม PG ChillPay ไม่รองรับ AMEX'
    },
    '예: 사용카드 안내(VISA, MASTER 등), 카드 표기와 동일한 명의 입력 안내 등': {
      EN: 'e.g. Accepted cards (VISA, MASTER, …), enter name exactly as on card, etc.',
      JP: '例: 利用可能カード案内(VISA, MASTER等)、カード表記と同じ名義入力の案内など',
      CH: '例：可用卡说明（VISA、MASTER 等）、与卡面一致的持卡人姓名提示等',
      TH: 'เช่น บัตรที่รับ (VISA, MASTER) ชื่อตรงกับบัตร ฯลฯ'
    },
    '예: 결제가 완료되었습니다.': {
      EN: 'e.g. Payment completed.',
      JP: '例: お支払いが完了しました。',
      CH: '例：支付已完成。',
      TH: 'เช่น ชำระเงินเสร็จสมบูรณ์'
    },
    '예: 팝업으로 열렸다면 이 창을 닫아 주세요.…': {
      EN: 'e.g. If this opened in a popup, please close this window.…',
      JP: '例: ポップアップで開いた場合はこのウィンドウを閉じてください。…',
      CH: '例：若以弹窗打开，请关闭此窗口。…',
      TH: 'เช่น ถ้าเปิดเป็นป๊อปอัป ให้ปิดหน้าต่างนี้…'
    },
    '예: 결제가 완료되지 않았거나 실패했습니다.': {
      EN: 'e.g. Payment was not completed or failed.',
      JP: '例: お支払いが完了していないか、失敗しました。',
      CH: '例：支付未完成或失败。',
      TH: 'เช่น การชำระไม่สำเร็จหรือล้มเหลว'
    },
    '행 삭제': {
      EN: 'Delete row',
      JP: '行を削除',
      CH: '删除行',
      TH: 'ลบแถว'
    },
    '불러오기 실패': {
      EN: 'Load failed',
      JP: '読み込みに失敗しました',
      CH: '加载失败',
      TH: 'โหลดล้มเหลว'
    },
    '등록된 정책이 없습니다.': {
      EN: 'No policies registered.',
      JP: '登録されたポリシーがありません。',
      CH: '暂无已登记政策。',
      TH: 'ยังไม่มีนโยบายที่ลงทะเบียน'
    },
    '목록을 불러오지 못했습니다.': {
      EN: 'Could not load the list.',
      JP: '一覧を読み込めませんでした。',
      CH: '无法加载列表。',
      TH: 'โหลดรายการไม่สำเร็จ'
    },
    '새 유형을 입력한 뒤 [저장]하세요.': {
      EN: 'Enter a new type, then [Save].',
      JP: '新規タイプを入力し、[保存]してください。',
      CH: '请输入新类型，然后[保存]。',
      TH: 'กรอกประเภทใหม่ แล้ว [บันทึก]'
    },
    '삭제할 항목을 목록에서 선택하세요.': {
      EN: 'Select an item to delete from the list.',
      JP: '削除する項目を一覧から選択してください。',
      CH: '请从列表中选择要删除的项。',
      TH: 'เลือกรายการที่จะลบจากรายการ'
    },
    '이 차지백 정책을 삭제하시겠습니까?': {
      EN: 'Delete this chargeback policy?',
      JP: 'このチャージバックポリシーを削除しますか？',
      CH: '要删除此拒付政策吗？',
      TH: 'ลบนโยบาย chargeback นี้หรือไม่'
    },
    '삭제': { EN: 'Delete', JP: '削除', CH: '删除', TH: 'ลบ' },
    '저장': { EN: 'Save', JP: '保存', CH: '保存', TH: 'บันทึก' },
    '예 CHILLPAY_API, CHILLPAY_URL': {
      EN: 'e.g. CHILLPAY_API, CHILLPAY_URL',
      JP: '例 CHILLPAY_API, CHILLPAY_URL',
      CH: '例 CHILLPAY_API, CHILLPAY_URL',
      TH: 'เช่น CHILLPAY_API, CHILLPAY_URL'
    },
    '표시 이름 (예 ChillPay · API)': {
      EN: 'Display name (e.g. ChillPay · API)',
      JP: '表示名（例 ChillPay · API）',
      CH: '显示名称（如 ChillPay · API）',
      TH: 'ชื่อที่แสดง (เช่น ChillPay · API)'
    },
    '— 선택 —': { EN: '— Select —', JP: '— 選択 —', CH: '— 请选择 —', TH: '— เลือก —' },
    /* HQ /hq/notifyEnv — 노티구성 (screens L + app.js pgAdminUiT) */
    '노티구성설정': {
      EN: 'Notify configuration',
      JP: 'ノティ構成',
      CH: '通知环境配置',
      TH: 'ตั้งค่าแจ้งเตือน'
    },
    '전산 노티 수신 (NOTI 전산노티대상 연동)': {
      EN: 'System notify ingress (NOTI system notify target)',
      JP: 'システムノティ受信（NOTI システムノティ先連携）',
      CH: '系统通知接入（NOTI 系统通知目标联动）',
      TH: 'รับแจ้งเตือนระบบ (เชื่อมเป้า NOTI)'
    },
    '아래 URL을 ziobiz/NOTI 전산노티대상 설정에 등록하세요. 경로 끝 토큰으로 무단 호출을 막습니다. 운영 배포 후 [공개 URL 베이스]에 https://실제도메인 을 넣으면 안내 URL이 고정됩니다. 배포설정 > API연동설정에서 연동용도가 노티(등)인 PG는 노티를 MID+루트로 분기합니다. URL 결제만인 PG는 동일 MID가 여러 가맹점이면 본문에 업체코드(compId) 또는 icopayCompId= 가 필요합니다.': {
      EN: 'Register the URL below in ziobiz/NOTI system notify target settings. The path suffix token blocks unauthorized calls. After production deploy, set [Public URL base] to https://your-domain so the displayed URL is stable. In Deployment > API integration, PSPs whose scope includes notify route by MID+root. URL-pay-only PSPs with one MID for many merchants need compId or icopayCompId= in the body.',
      JP: '以下のURLをziobiz/NOTIのシステムノティ先設定に登録してください。パス末尾のトークンで不正呼び出しを防ぎます。本番公開後は［公開URLベース］にhttps://実際のドメインを入れると案内URLが固定されます。デプロイ設定＞API連携設定で連携用途にノティ等が含まれるPGは、MID+ルートでノティを振り分けます。URL決済のみのPGで同一MIDが複数加盟店にまたがる場合は本文に加盟店コード(compId)またはicopayCompId=が必要です。',
      CH: '请将下方 URL 登记到 ziobiz/NOTI 的系统通知目标设置。路径末尾令牌可防止未授权调用。生产部署后，在［公开 URL 基址］填入 https://实际域名 可固定提示 URL。在部署设置＞API 联动中，联动用途含通知等的 PG 会按 MID+路由分流通知。仅 URL 支付的 PG 若同一 MID 对应多商户，则正文需包含商户代码(compId)或 icopayCompId=。',
      TH: 'ลงทะเบียน URL ด้านล่างในการตั้งค่าเป้ารับแจ้งเตือนระบบ ziobiz/NOTI โทเคนท้ายพาธกันยิงโดยไม่ได้รับอนุญาต หลัง deploy ใส่ [ฐาน URL สาธารณะ] เป็น https://โดเมนจริง เพื่อคง URL ที่แสดง ในการตั้งค่า deploy > เชื่อม API หากขอบเขตมีแจ้งเตือน PG จะแบ่งตาม MID+รูท หาก PG แค่ URL pay และ MID เดียวหลายร้าน ต้องมี compId หรือ icopayCompId= ในตัวแบบ'
    },
    '권장: 아래 「노티 수신 URL(미들웨어)」을 ziobiz/NOTI·ChillPay·JPAY 등에 등록하세요(/api/middleware/notify/v1/pg-notify/…). 레거시 open 경로는 하단 참고 필드와 동일 처리입니다. 경로 끝 토큰으로 무단 호출을 막습니다. 운영 배포 후 [공개 URL 베이스]에 https://실제도메인 을 넣으면 안내 URL이 고정됩니다. 배포설정 > API연동설정에서 연동용도가 노티(등)인 PG는 노티를 MID+루트로 분기합니다. URL 결제만인 PG는 동일 MID가 여러 가맹점이면 본문에 업체코드(compId) 또는 icopayCompId= 가 필요합니다.': {
      EN: 'Recommended: register the 「Notify ingress URL (middleware)」 below with ziobiz/NOTI, ChillPay, JPAY, etc. (/api/middleware/notify/v1/pg-notify/…). The legacy open path is handled the same as the reference field at the bottom. The path suffix token blocks unauthorized calls. After production deploy, set [Public URL base] to https://your-domain so the displayed URL is stable. In Deployment > API integration, PSPs whose scope includes notify route by MID+root. URL-pay-only PSPs sharing one MID across merchants need compId or icopayCompId= in the body.',
      JP: '推奨: 下の「ノティ受信URL（ミドルウェア）」をziobiz/NOTI・ChillPay・JPAY等に登録してください（/api/middleware/notify/v1/pg-notify/…）。レガシーopenパスは下の参考フィールドと同じ処理です。パス末尾のトークンで不正呼び出しを防ぎます。本番公開後、［公開URLベース］にhttps://実際のドメインを入れると案内URLが固定されます。デプロイ設定＞API連携設定で連携用途にノティ（等）が含まれるPGは、MID+ルートでノティを振り分けます。URL決済のみのPGで同一MIDが複数加盟店にまたがる場合は、本文に加盟店コード(compId)またはicopayCompId=が必要です。',
      CH: '建议：将下方「通知接入 URL（中间件）」登记到 ziobiz/NOTI、ChillPay、JPAY 等（/api/middleware/notify/v1/pg-notify/…）。旧版 open 路径与底部参考字段同等处理。路径末尾令牌防止未授权调用。生产部署后在［公开 URL 基址］填入 https://实际域名 可固定提示 URL。部署设置＞API 联动中，联动用途含通知等的 PG 按 MID+路由分流。仅 URL 支付的 PG 若同一 MID 对应多商户，正文需 compId 或 icopayCompId=。',
      TH: 'แนะนำ: ลงทะเบียน「URL รับแจ้งเตือน (มิดเดิลแวร์)」ด้านล่างกับ ziobiz/NOTI·ChillPay·JPAY ฯลฯ (/api/middleware/notify/v1/pg-notify/…) เส้นทาง open แบบเก่าจัดการเหมือนช่องอ้างอิงด้านล่าง โทเคนท้ายพาธกันยิงโดยไม่ได้รับอนุญาต หลัง deploy ใส่ [ฐาน URL สาธารณะ] เป็น https://โดเมนจริง PG ที่ขอบเขตมีแจ้งเตือนจะแบ่งตาม MID+รูท หาก PG แค่ URL pay และ MID เดียวหลายร้าน ต้องมี compId หรือ icopayCompId= ในตัวแบบ'
    },
    '노티 수신 URL(미들웨어 권장)': {
      EN: 'Notify ingress URL (middleware, recommended)',
      JP: 'ノティ受信URL（ミドルウェア推奨）',
      CH: '通知接入 URL（中间件，推荐）',
      TH: 'URL รับแจ้งเตือน (มิดเดิลแวร์ แนะนำ)'
    },
    '노티 수신 URL(open·레거시)': {
      EN: 'Notify ingress URL (open · legacy)',
      JP: 'ノティ受信URL（open・レガシー）',
      CH: '通知接入 URL（open·旧版）',
      TH: 'URL รับแจ้งเตือน (open·เลกาซี)'
    },
    '노티 수신 URL': {
      EN: 'Notify ingress URL',
      JP: 'ノティ受信URL',
      CH: '通知接入 URL',
      TH: 'URL รับแจ้งเตือน'
    },
    'Ingress 토큰(참고)': {
      EN: 'Ingress token (reference)',
      JP: 'Ingressトークン（参考）',
      CH: 'Ingress 令牌（参考）',
      TH: 'โทเคน Ingress (อ้างอิง)'
    },
    '공개 URL 베이스': {
      EN: 'Public URL base',
      JP: '公開URLベース',
      CH: '公开 URL 基址',
      TH: 'ฐาน URL สาธารณะ'
    },
    '노티 성공 응답 본문': {
      EN: 'Successful notify response body',
      JP: 'ノティ成功応答本文',
      CH: '通知成功响应正文',
      TH: 'เนื้อหาตอบกลับเมื่อแจ้งเตือนสำเร็จ'
    },
    '비우면 브라우저 접속 기준(예: http://localhost:8080)': {
      EN: 'If empty, uses browser origin (e.g. http://localhost:8080)',
      JP: '空欄ならブラウザの起点（例: http://localhost:8080）',
      CH: '留空则按浏览器访问基准（如 http://localhost:8080）',
      TH: 'ว่างไว้ใช้ต้นทางจากเบราว์เซอร์ (เช่น http://localhost:8080)'
    },
    '총판 노티 대상 생성': {
      EN: 'Create master-distributor notify targets',
      JP: '総販ノティ先の作成',
      CH: '总代通知目标创建',
      TH: 'สร้างเป้าแจ้งเตือนตัวแทนหลัก'
    },
    '먼저 [연결 총판]에서 총판을 선택한 뒤 노티 대상명을 입력하고 [노티자동생성]을 누르세요. CALLBACK·RESULT URL이 발급되며 선택한 총판과 즉시 연결됩니다. 이때 해당 총판 업체 상세의 필수 노티(URL 1·2)도 발급 URL로 자동 반영됩니다(보조 URL 3·4는 유지). 목록의 [연결수정]으로 연결 총판을 바꾸면 동일하게 필수 노티가 갱신됩니다. 총판 저장 시 노티 URL에 동일 주소를 넣어 두면 저장 시 연결이 유지·갱신됩니다.': {
      EN: 'First pick a master distributor under [Linked master distributor], enter a target name, then click [Auto-create notify]. CALLBACK·RESULT URLs are issued and linked to that distributor. Required notify URLs 1·2 on the distributor detail are updated to the issued URLs (auxiliary 3·4 stay). Use [Edit link] in the list to change the linked distributor and refresh required URLs the same way. If distributor save keeps the same notify URL text, the link stays in sync on save.',
      JP: 'まず［紐付け総販］で総販を選び、ノティ先名を入力して［ノティ自動生成］を押してください。CALLBACK·RESULTのURLが発行され、選択した総販に直ちに紐づきます。このとき当該総販の加盟店詳細の必須ノティ(URL 1·2)も発行URLへ自動反映されます(補助URL 3·4は維持)。一覧の［紐付け修正］で紐付け総販を変えると、同様に必須ノティが更新されます。総販保存時にノティURLに同一アドレスを入れておくと、保存時に紐付けが維持・更新されます。',
      CH: '请先在［关联总代］选择总代，输入通知目标名称后点击［通知自动生成］。将签发 CALLBACK·RESULT URL 并立即关联所选总代。此时该总代商户详情的必填通知(URL 1·2)也会自动改为签发 URL（辅助 3·4 保留）。通过列表中的［修改关联］更换关联总代时，同样会刷新必填通知。总代保存时若在通知 URL 中填入相同地址，保存时会保持或更新关联。',
      TH: 'เลือกตัวแทนหลักที่［เชื่อมตัวแทนหลัก］ ใส่ชื่อเป้าแจ้งเตือน แล้วกด［สร้างแจ้งเตือนอัตโนมัติ］ ระบบจะออก URL CALLBACK·RESULT และเชื่อมกับตัวแทนหลักที่เลือกทันที ขณะนั้น URL แจ้งเตือนบังคับ (1·2) ในรายละเอียดร้านของตัวแทนหลักจะถูกอัปเดตเป็น URL ที่ออก (URL 3·4 เสริมคงเดิม) ใช้［แก้การเชื่อม］ในรายการเพื่อเปลี่ยนตัวแทนหลักและรีเฟรช URL บังคับเช่นกัน หากบันทึกตัวแทนหลักโดยใส่ที่อยู่เดียวกันในช่อง URL แจ้งเตือน การเชื่อมจะคง/อัปเดตเมื่อบันทึก'
    },
    '연결 총판': {
      EN: 'Linked master distributor',
      JP: '紐付け総販',
      CH: '关联总代',
      TH: 'เชื่อมตัวแทนหลัก'
    },
    '연결 총판을 선택하세요.': {
      EN: 'Select a linked master distributor.',
      JP: '紐付け総販を選択してください。',
      CH: '请选择关联总代。',
      TH: 'โปรดเลือกตัวแทนหลักที่เชื่อม'
    },
    '선택하세요': {
      EN: 'Please select',
      JP: '選択してください',
      CH: '请选择',
      TH: 'โปรดเลือก'
    },
    '노티 대상명': {
      EN: 'Notify target name',
      JP: 'ノティ先名',
      CH: '通知目标名称',
      TH: 'ชื่อเป้าแจ้งเตือน'
    },
    '예: 총판A 수신': {
      EN: 'e.g. Master A inbound',
      JP: '例: 総販A 受信',
      CH: '例：总代A 接收',
      TH: 'เช่น รับตัวแทนหลัก A'
    },
    '노티자동생성': {
      EN: 'Auto-create notify',
      JP: 'ノティ自動生成',
      CH: '通知自动生成',
      TH: 'สร้างแจ้งเตือนอัตโนมัติ'
    },
    '토큰 재발급': {
      EN: 'Reissue token',
      JP: 'トークン再発行',
      CH: '重新签发令牌',
      TH: 'ออกโทเคนใหม่'
    },
    '생성일시': {
      EN: 'Created at',
      JP: '作成日時',
      CH: '创建时间',
      TH: 'วันที่สร้าง'
    },
    '노티 주소': {
      EN: 'Notify URL',
      JP: 'ノティURL',
      CH: '通知地址',
      TH: 'ที่อยู่แจ้งเตือน'
    },
    '복사': {
      EN: 'Copy',
      JP: 'コピー',
      CH: '复制',
      TH: 'คัดลอก'
    },
    '노티 성격': {
      EN: 'Notify channel',
      JP: 'ノティ種別',
      CH: '通知类型',
      TH: 'ลักษณะแจ้งเตือน'
    },
    '등록된 노티 대상이 없습니다.': {
      EN: 'No notify targets registered.',
      JP: '登録されたノティ先がありません。',
      CH: '尚未注册通知目标。',
      TH: 'ยังไม่มีเป้าแจ้งเตือนที่ลงทะเบียน'
    },
    '연결수정': {
      EN: 'Edit link',
      JP: '紐付け修正',
      CH: '修改关联',
      TH: 'แก้การเชื่อม'
    },
    '총판 선택': {
      EN: 'Select master distributor',
      JP: '総販を選択',
      CH: '选择总代',
      TH: 'เลือกตัวแทนหลัก'
    },
    'CALLBACK·RESULT 쌍에 동일 연결이 반영됩니다.': {
      EN: 'The same link applies to the CALLBACK·RESULT pair.',
      JP: 'CALLBACK·RESULTのペアに同じ紐付けが反映されます。',
      CH: 'CALLBACK·RESULT 成对使用相同关联。',
      TH: 'การเชื่อมเดียวกันถูกใช้กับคู่ CALLBACK·RESULT'
    },
    '대상이 없습니다.': {
      EN: 'No target selected.',
      JP: '対象がありません。',
      CH: '没有目标。',
      TH: 'ไม่มีเป้าหมาย'
    },
    'API를 사용할 수 없습니다.': {
      EN: 'API is not available.',
      JP: 'APIを利用できません。',
      CH: '无法使用 API。',
      TH: 'ใช้ API ไม่ได้'
    },
    '연결되었습니다.': {
      EN: 'Linked.',
      JP: '紐付けました。',
      CH: '已关联。',
      TH: 'เชื่อมแล้ว'
    },
    '연결 실패': {
      EN: 'Link failed',
      JP: '紐付けに失敗しました',
      CH: '关联失败',
      TH: 'เชื่อมล้มเหลว'
    },
    '총판 목록을 불러오지 못했습니다.': {
      EN: 'Failed to load master distributor list.',
      JP: '総販一覧を読み込めませんでした。',
      CH: '无法加载总代列表。',
      TH: 'โหลดรายการตัวแทนหลักไม่สำเร็จ'
    },
    '노티 URL 토큰이 바뀝니다. NOTI/칠페이에 등록된 URL도 함께 바꿔야 합니다. 계속하시겠습니까?': {
      EN: 'The notify URL token will change. Update the URL registered in NOTI/ChillPay as well. Continue?',
      JP: 'ノティURLのトークンが変わります。NOTI/チルペイに登録したURLも合わせて変更してください。続行しますか？',
      CH: '通知 URL 令牌将变更。请在 NOTI/ChillPay 中同步更新已登记的 URL。是否继续？',
      TH: 'โทเคน URL แจ้งเตือนจะเปลี่ยน ต้องอัปเดต URL ที่ลงใน NOTI/ChillPay ด้วย ดำเนินต่อหรือไม่'
    },
    '토큰이 재발급되었습니다. 새 URL을 NOTI에 반영하세요.': {
      EN: 'Token reissued. Apply the new URL in NOTI.',
      JP: 'トークンを再発行しました。新しいURLをNOTIに反映してください。',
      CH: '令牌已重新签发。请将新 URL 同步到 NOTI。',
      TH: 'ออกโทเคนใหม่แล้ว โปรดอัปเดต URL ใหม่ใน NOTI'
    },
    '노티 대상명을 입력하세요.': {
      EN: 'Enter a notify target name.',
      JP: 'ノティ先名を入力してください。',
      CH: '请输入通知目标名称。',
      TH: 'กรุณาใส่ชื่อเป้าแจ้งเตือน'
    },
    'CALLBACK·RESULT 노티 URL이 자동 생성되었습니다. 아래 목록에서 확인하세요.': {
      EN: 'CALLBACK·RESULT notify URLs were created. Check the list below.',
      JP: 'CALLBACK·RESULTのノティURLを自動生成しました。下の一覧で確認してください。',
      CH: '已自动生成 CALLBACK·RESULT 通知 URL。请在下方列表确认。',
      TH: 'สร้าง URL แจ้งเตือน CALLBACK·RESULT อัตโนมัติแล้ว ตรวจสอบในรายการด้านล่าง'
    },
    '노티 자동생성 실패': {
      EN: 'Auto-create notify failed',
      JP: 'ノティ自動生成に失敗しました',
      CH: '通知自动生成失败',
      TH: 'สร้างแจ้งเตือนอัตโนมัติล้มเหลว'
    },
    '복사에 실패했습니다. 주소를 직접 선택해 복사하세요.': {
      EN: 'Copy failed. Select the address and copy manually.',
      JP: 'コピーに失敗しました。アドレスを選択して手動でコピーしてください。',
      CH: '复制失败。请手动选择地址后复制。',
      TH: 'คัดลอกไม่สำเร็จ เลือกที่อยู่แล้วคัดลอกด้วยมือ'
    },
    '이 노티 URL을 삭제하시겠습니까?': {
      EN: 'Delete this notify URL?',
      JP: 'このノティURLを削除しますか？',
      CH: '要删除此通知 URL 吗？',
      TH: 'ลบ URL แจ้งเตือนนี้หรือไม่'
    },
    /* HQ /hq/ledgerSysSettings — 全算設定 (screens L + app.js pgAdminUiT) */
    '시간 및 동기화 설정': {
      EN: 'Time & synchronization',
      JP: '時刻・同期設定',
      CH: '时间与同步',
      TH: 'เวลาและการซิงโครไนซ์'
    },
    'ziobiz/NOTI 노티미들웨어의 시스템·환경설정(시간·NTP·동기화)과 동일 목적입니다. 실제 OS 시각 동기화는 VPS에서 chrony/systemd-timesyncd 등으로 수행하고, 여기 표준시는 전산 배치·목록 표시·결제 후속조치(무효·이메일무효) 경과 판단의 기준 ZoneId로 사용합니다. 신규·미설정 시 기본은 태국(Asia/Bangkok)입니다.': {
      EN: 'Same intent as ziobiz/NOTI middleware system settings (time, NTP, sync). OS clock sync is done on the VPS (chrony/systemd-timesyncd, etc.). The timezone here is the ZoneId used for ledger batch jobs, list display, and pay-follow elapsed rules (auto void / email void). Default for new/unset installs is Thailand (Asia/Bangkok).',
      JP: 'ziobiz/NOTI ノティミドルウェアのシステム・環境設定（時刻・NTP・同期）と同じ目的です。OS時刻の同期はVPS側の chrony / systemd-timesyncd 等で行い、ここでは全算バッチ・一覧表示・決済フォロー（無効・メール無効）の経過判定に使う ZoneId です。新規・未設定時の既定はタイ（Asia/Bangkok）です。',
      CH: '与 ziobiz/NOTI 中间件的系统环境配置（时间、NTP、同步）目的一致。OS 对时在 VPS 上用 chrony/systemd-timesyncd 等完成；此处时区作为账务批处理、列表展示、支付后续（自动无效、邮件无效）判断的 ZoneId。新建或未设置时默认为泰国（Asia/Bangkok）。',
      TH: 'มีวัตถุประสงค์เดียวกับการตั้งค่าระบบของ ziobiz/NOTI middleware (เวลา NTP sync) การซิงก์เวลา OS ทำบน VPS (chrony/systemd-timesyncd ฯลฯ) ส่วนเขตเวลาที่นี่คือ ZoneId สำหรับแบตช์บัญชี การแสดงรายการ และกฎระยะเวลาติดตามการชำระ (void/void อีเมล) ค่าเริ่มต้นคือไทย (Asia/Bangkok)'
    },
    '표준 시간대 (IANA)': {
      EN: 'Timezone (IANA)',
      JP: '標準タイムゾーン（IANA）',
      CH: '标准时区（IANA）',
      TH: 'เขตเวลา (IANA)'
    },
    'NTP 동기화 사용': {
      EN: 'Use NTP sync',
      JP: 'NTP同期を使用',
      CH: '使用 NTP 同步',
      TH: 'ใช้การซิงก์ NTP'
    },
    '동기화 주기(분)': {
      EN: 'Sync interval (minutes)',
      JP: '同期間隔（分）',
      CH: '同步周期（分钟）',
      TH: 'รอบซิงก์ (นาที)'
    },
    '예: 60': { EN: 'e.g. 60', JP: '例: 60', CH: '例：60', TH: 'เช่น 60' },
    'NTP 서버 목록': {
      EN: 'NTP server list',
      JP: 'NTPサーバー一覧',
      CH: 'NTP 服务器列表',
      TH: 'รายการเซิร์ฟเวอร์ NTP'
    },
    '쉼표 구분, 예: pool.ntp.org, time.google.com': {
      EN: 'Comma-separated, e.g. pool.ntp.org, time.google.com',
      JP: 'カンマ区切り 例: pool.ntp.org, time.google.com',
      CH: '逗号分隔，例如 pool.ntp.org, time.google.com',
      TH: 'คั่นด้วยจุลภาค เช่น pool.ntp.org, time.google.com'
    },
    '서버 기준 시각(조회 시점)': {
      EN: 'Server time (at load)',
      JP: 'サーバー基準時刻（取得時点）',
      CH: '服务器时间（查询时）',
      TH: 'เวลาเซิร์ฟเวอร์ (ตอนโหลด)'
    },
    '적용 ZoneId': {
      EN: 'Applied ZoneId',
      JP: '適用 ZoneId',
      CH: '应用的 ZoneId',
      TH: 'ZoneId ที่ใช้'
    },
    '헬로 타임라인': {
      EN: 'Hello timeline',
      JP: 'Helloタイムライン',
      CH: 'Hello 时间线',
      TH: 'ไทม์ไลน์ Hello'
    },
    '「사용」이면 헬로(안내·VIEW SETTING 영역) 표시가 로그인 브라우저(sessionStorage)에서 전 페이지에 동기화됩니다. 한 페이지에서 헬로를 켜면 설정한 분(기본 10분) 동안 모든 목록 화면에서 동일하게 표시되며, 시간이 지나면 전 페이지에서 자동으로 숨김(비활성)으로 돌아갑니다. 「비사용」이면 기존과 같이 페이지(탭)마다 헬로를 따로 토글합니다.': {
      EN: 'When set to Active, Hello (guide / VIEW SETTING area) visibility syncs across all pages in the logged-in browser (sessionStorage). Turning Hello on in one page shows it on every list screen for the configured minutes (default 10), then hides it everywhere. When Inactive, each page (tab) toggles Hello independently as before.',
      JP: '「使用」の場合、Hello（案内・VIEW SETTING 領域）の表示はログインブラウザの sessionStorage で全ページに同期されます。あるページで Hello をオンにすると、設定した分（既定10分）の間は全リスト画面で同じ表示になり、時間が経つと全ページで自動的に非表示（無効）に戻ります。「未使用」の場合は従来どおりページ（タブ）ごとに Hello を個別に切り替えます。',
      CH: '「使用」时，Hello（提示与 VIEW SETTING 区域）在登录浏览器的 sessionStorage 下全页同步。任一页开启 Hello 后，在设定分钟数（默认 10）内所有列表页一致显示，超时后全页自动隐藏（停用）。「未使用」则仍按页（标签）分别切换。',
      TH: 'เมื่อ「ใช้」 Hello จะซิงก์ทุกหน้าในเบราว์เซอร์ที่ล็อกอิน (sessionStorage) เปิดบนหนึ่งหน้าแล้วจะแสดงบนทุกหน้ารายการตามนาทีที่ตั้ง (ค่าเริ่ม 10) แล้วซ่อนทั้งหมดเมื่อครบเวลา 「ไม่ใช้」 ยังสลับแยกตามแท็บเหมือนเดิม'
    },
    '유지 시간(분)': {
      EN: 'Keep-alive (minutes)',
      JP: '維持時間（分）',
      CH: '保持时间（分钟）',
      TH: 'ระยะเวลาคงอยู่ (นาที)'
    },
    '기본 10': { EN: 'Default 10', JP: '既定 10', CH: '默认 10', TH: 'ค่าเริ่ม 10' },
    '1~1440(24시간). 사용일 때만 적용됩니다.': {
      EN: '1–1440 (24h). Applies only when enabled.',
      JP: '1～1440（24時間）。使用時のみ有効です。',
      CH: '1–1440（24 小时）。仅启用时生效。',
      TH: '1–1440 (24 ชม.) ใช้เมื่อเปิดใช้เท่านั้น'
    },
    '헬로 타임라인 저장': {
      EN: 'Save Hello timeline',
      JP: 'Helloタイムラインを保存',
      CH: '保存 Hello 时间线',
      TH: 'บันทึกไทม์ไลน์ Hello'
    },
    '다른 전산설정 항목은 건드리지 않고, 위 두 값만 서버에 반영합니다. 하단 「저장」은 화면 전체를 저장합니다.': {
      EN: 'Only the two values above are sent to the server; other ledger settings are untouched. The bottom Save stores the entire screen.',
      JP: '他の全算設定は変更せず、上の2値のみサーバーに反映します。下の「保存」は画面全体を保存します。',
      CH: '不改动其他账务设置，仅将上述两项提交服务器。底部「保存」会保存整个画面。',
      TH: 'ไม่แตะตั้งค่าอื่น ส่งเฉพาะสองค่าด้านบนไปเซิร์ฟเวอร์ ปุ่มบันทึกด้านล่างบันทึกทั้งหน้า'
    },
    '데이터 보관 기간': {
      EN: 'Data retention',
      JP: 'データ保持期間',
      CH: '数据保留期限',
      TH: 'ระยะเวลาเก็บข้อมูล'
    },
    '쌓이는 데이터 유형별로 DB·로그·버퍼 보관 목표 일수를 지정합니다. 표에는 업체정보(등록)·업체관리·정산관리·가맹점 정산내역(수수료내역)·정산 리포트 등 모듈별 유형이 포함됩니다. 「자동삭제」를 켠 항목만 매일 새벽 스케줄로 초과분 삭제를 시도합니다(스케줄 대상만 체크 가능). 그 외 유형은 보관 목표(일)만 저장됩니다. 아래 표는 하단 「수수료·정산 로직」과 같은 테이블 래핑(둥근 테두리·작은 표 스타일)을 사용합니다. 상단 빨간 「전체 데이터 초기화」는 보관 일수와 별도로, 등록된 조직·가맹 프로필만 남기고 거래·정산·노티·수수료 정책 등 넓게 비웁니다. 파란 「정산 데이터 초기화」는 <strong>수수료내역·거래·본사 정산 설정·통합정산(외부)</strong>은 두고 정산 실행·미수·환수·담보·공제·보류/유통/리포트 근거 행만 지웁니다(복구 불가, 동일 권한).': {
      EN: 'Set target retention days per data type for DB, logs, and buffers. The table lists module types (company registration, company admin, settlement, merchant settlement / fee history, settlement reports, etc.). Only rows with Auto delete enabled are purged nightly by the scheduler (checkbox only for scheduler-linked types). Other types store retention targets only. The table uses the same wrapper style as Fee & settlement logic below. Red Reset all operational data clears broadly (transactions, settlement, notify, fee policies) while keeping registered org and merchant profiles. Blue Reset settlement data keeps <strong>fee history, transactions, HQ settlement settings, and external integrated settlement</strong> while deleting settlement runs, receivables, recoveries, collateral, deductions, and hold/distribution/report basis rows (irreversible; same permission).',
      JP: 'データ種別ごとにDB・ログ・バッファの保持目標日数を指定します。「自動削除」をオンにした行のみ毎晩スケジュールで超過分削除を試みます（スケジュール対象のみチェック可）。それ以外は保持日数のみ保存します。下の「手数料・精算ロジック」と同じ表ラッパーです。赤の「全データ初期化」は保持日数とは別に、登録済み組織・加盟店プロフィール以外の運用データを広く削除します。青の「精算データ初期化」は<strong>手数料履歴・取引・本社精算設定・外部連携精算</strong>は残し、精算実行・未収・回収・担保・控除・保留/流通/レポート根拠行のみ削除します（復旧不可・同一権限）。',
      CH: '按数据类型设置 DB、日志、缓冲的保留天数。表内包含各模块类型（注册、商户管理、结算、商户结算/手续费、结算报表等）。仅勾选「自动删除」且纳入调度项会在每日凌晨尝试删除超期数据；其余类型只保存保留目标。表格样式与下方「手续费·结算逻辑」一致。红色「全部运营数据初始化」在保留已登记组织与商户档案的前提下清空交易、结算、通知、手续费策略等。蓝色「结算数据初始化」保留<strong>手续费明细、交易、总部结算设置、外部集成结算</strong>，仅删除结算执行、应收、回收、担保、抵扣及保留/分润/报表依据行（不可恢复，权限相同）。',
      TH: 'กำหนดวันเก็บตามประเภทข้อมูลสำหรับ DB/ล็อก/บัฟเฟอร์ เฉพาะแถวที่เปิด「ลบอัตโนมัติ」และอยู่ในสเกจูลเท่าที่ลบเกินกลางคืน ประเภทอื่นเก็บเฉพาณเป้าหมายวัน ตารางใช้สไตล์เดียวกับบล็อกด้านล่าง ปุ่มแดงล้างข้อมูลดำเนินงานกว้างๆ คงโปรไฟล์องค์กร/ร้าน ปุ่มน้ำเงินล้างข้อมูลชำระคง<strong>ประวัติค่าธรรมเนียม ธุรกรรม การตั้งค่าชำระ HQ ชำระรวมภายนอก</strong> ลบรัน ลูกหนี้ กู้คืน หลักประกัน หัก ฯลฯ (กู้คืนไม่ได้)'
    },
    '전산설정 전체 데이터 초기화 카드 본문': {
      EN: 'After clicking, you go through a <strong>double confirm</strong> (two browser prompts, each <strong>OK / Cancel</strong>). First message explains that data will be reset while <strong>registered company information</strong> is kept. Only <strong>organization and merchant profile</strong> rows remain; operational data is removed. HQ ledger, notify, PG bindings and login accounts are kept. <strong class="text-danger">This cannot be undone.</strong>',
      JP: 'クリック後に<strong>二重確認</strong>（ブラウザ確認を2回、それぞれ<strong>OK・キャンセル</strong>）を行います。最初の案内は「データ初期化が行われます。登録済みの会社情報は保持されます。」登録済みの<strong>組織・加盟店プロフィール</strong>行のみ残し運用データを削除します。本社全算・ノティ・PG連携などHQ全体設定とログインアカウントは保持します。<strong class="text-danger">元に戻せません。</strong>',
      CH: '点击后将进行<strong>双重确认</strong>（两次浏览器提示，每次<strong>确定/取消</strong>）。首次说明会清空数据但<strong>保留已登记的公司信息</strong>。仅保留<strong>组织与商户档案</strong>行并删除运营数据。总部账务、通知、PG 绑定及登录账号保留。<strong class="text-danger">不可恢复。</strong>',
      TH: 'หลังคลิกมี<strong>ยืนยันสองครั้ง</strong> (เบราว์เซอร์สองรอบ <strong>ตกลง/ยกเลิก</strong>) ข้อความแรกอธิบายการล้างข้อมูลโดย<strong>เก็บข้อมูลบริษัทที่ลงทะเบียน</strong> คงเฉพาะแถว<strong>โปรไฟล์องค์กร/ร้านค้า</strong> ลบข้อมูลดำเนินงาน การตั้งค่า HQ แจ้งเตือน PG และบัญชีล็อกอินคงอยู่ <strong class="text-danger">กู้คืนไม่ได้</strong>'
    },
    '전체 데이터 초기화': {
      EN: 'Reset all operational data',
      JP: '全データ初期化',
      CH: '全部运营数据初始化',
      TH: 'ล้างข้อมูลดำเนินงานทั้งหมด'
    },
    '전체 데이터 초기화…': {
      EN: 'Reset all operational data…',
      JP: '全データ初期化…',
      CH: '全部运营数据初始化…',
      TH: 'ล้างข้อมูลดำเนินงาน…'
    },
    '정산 데이터 초기화': {
      EN: 'Reset settlement data',
      JP: '精算データ初期化',
      CH: '结算数据初始化',
      TH: 'ล้างข้อมูลการชำระ'
    },
    '정산 데이터 초기화…': {
      EN: 'Reset settlement data…',
      JP: '精算データ初期化…',
      CH: '结算数据初始化…',
      TH: 'ล้างข้อมูลการชำระ…'
    },
    '금일 결제·노티 삭제': {
      EN: 'Delete today\'s payments & notify',
      JP: '本日決済・ノティ削除',
      CH: '删除当日支付与通知',
      TH: 'ลบการชำระและแจ้งเตือนวันนี้'
    },
    '금일 결제·노티 삭제…': {
      EN: 'Delete today\'s payments & notify…',
      JP: '本日決済・ノティ削除…',
      CH: '删除当日支付与通知…',
      TH: 'ลบการชำระและแจ้งเตือนวันนี้…'
    },
    '전산설정 금일 결제 노티 삭제 카드 본문': {
      EN: 'Deletes <strong>pg_trnsctn</strong> rows for the selected calendar day (HQ display timezone) and optionally <strong>tb_pg_notify_inbound</strong> for the same window. Use <strong>before NOTI middleware resends</strong> notifications so ICOPAY can re-process cleanly. Merchant registration, fee policies, and settlement settings are kept. Leave merchant ID empty to affect <strong>all merchants</strong> for that day.',
      JP: '選択した暦日（本社表示タイムゾーン）の<strong>pg_trnsctn</strong>を削除し、オプションで同期間の<strong>tb_pg_notify_inbound</strong>も削除します。<strong>NOTIミドルウェア再送の直前</strong>に使用し、ICOPAYで再処理できるようにします。加盟店登録・手数料・精算設定は保持。加盟店IDを空にするとその日の<strong>全加盟店</strong>が対象です。',
      CH: '删除所选日历日（总部显示时区）的 <strong>pg_trnsctn</strong>，可选同时删除同期 <strong>tb_pg_notify_inbound</strong>。请在 <strong>NOTI 中间件重发通知之前</strong>使用，以便 ICOPAY 重新处理。保留商户登记、手续费与结算设置。商户 ID 留空则影响该日<strong>全部商户</strong>。',
      TH: 'ลบ <strong>pg_trnsctn</strong> ตามวันที่เลือก (เขตเวลาแสดง HQ) และเลือกลบ <strong>tb_pg_notify_inbound</strong> ช่วงเดียวกัน ใช้<strong>ก่อน NOTI ส่งซ้ำ</strong> เก็บการลงทะเบียนร้าน นโยบายค่าธรรมเนียม การตั้งค่าชำระ เว้นรหัสร้านว่าง =<strong>ทุกร้าน</strong>ในวันนั้น'
    },
    '대상 일자': { EN: 'Target date', JP: '対象日', CH: '目标日期', TH: 'วันที่เป้าหมาย' },
    '가맹점 ID(선택)': { EN: 'Merchant ID (optional)', JP: '加盟店ID(任意)', CH: '商户 ID（可选）', TH: 'รหัสร้าน (ไม่บังคับ)' },
    '비우면 전체 가맹': { EN: 'All merchants if empty', JP: '空欄は全加盟店', CH: '留空为全部商户', TH: 'ว่าง = ทุกร้าน' },
    '노티수령정보도 삭제': { EN: 'Also delete notify inbound log', JP: 'ノティ受信情報も削除', CH: '同时删除通知接收记录', TH: 'ลบบันทึกแจ้งเตือนด้วย' },
    '대상 일자를 선택하세요.': { EN: 'Select a target date.', JP: '対象日を選択してください。', CH: '请选择目标日期。', TH: 'เลือกวันที่เป้าหมาย' },
    '전체 가맹': { EN: 'All merchants', JP: '全加盟店', CH: '全部商户', TH: 'ทุกร้านค้า' },
    '삭제함': { EN: 'Delete', JP: '削除', CH: '删除', TH: 'ลบ' },
    '유지함': { EN: 'Keep', JP: '保持', CH: '保留', TH: 'คงไว้' },
    '금일 결제·노티 삭제가 완료되었습니다.': {
      EN: 'Today\'s payment and notify data purge completed.',
      JP: '本日の決済・ノティ削除が完了しました。',
      CH: '当日支付与通知数据已删除完成。',
      TH: 'ลบข้อมูลการชำระและแจ้งเตือนวันนี้เสร็จแล้ว'
    },
    '지정 일자의 결제내역(pg_trnsctn)을 삭제합니다.\n\n일자: {0}\n범위: {1}\n노티수령정보: {2}\n\nNOTI 재전송 직전에 사용하세요. 가맹·수수료·정산 설정은 유지됩니다.\n\n[확인]으로 다음 안내로 진행합니다.': {
      EN: 'This deletes pg_trnsctn for the selected day.\n\nDate: {0}\nScope: {1}\nNotify inbound: {2}\n\nUse before NOTI resend. Merchant/fee/settlement settings are kept.\n\nClick OK for the final confirmation.',
      JP: '指定日の pg_trnsctn を削除します。\n\n日付: {0}\n範囲: {1}\nノティ受信: {2}\n\nNOTI再送の直前に使用。加盟店・手数料・精算設定は保持。\n\n[OK]で最終確認へ。',
      CH: '将删除指定日的 pg_trnsctn。\n\n日期: {0}\n范围: {1}\n通知接收: {2}\n\n请在 NOTI 重发前使用。保留商户/手续费/结算设置。\n\n[确定]进入最终确认。',
      TH: 'ลบ pg_trnsctn ตามวันที่\n\nวันที่: {0}\nขอบเขต: {1}\nแจ้งเตือน: {2}\n\nใช้ก่อน NOTI ส่งซ้ำ คงการตั้งค่าร้าน/ค่าธรรมเนียม/ชำระ\n\n[ตกลง]ยืนยันขั้นสุดท้าย'
    },
    '마지막 확인입니다.\n\n일자 {0} · {1}의 결제·노티 데이터가 삭제됩니다. 복구할 수 없습니다.\n\n실행하시겠습니까?': {
      EN: 'Final confirmation.\n\nPayment and notify data for {0} · {1} will be deleted. This cannot be undone.\n\nProceed?',
      JP: '最終確認です。\n\n{0} · {1} の決済・ノティデータを削除します。元に戻せません。\n\n実行しますか？',
      CH: '最终确认。\n\n将删除 {0} · {1} 的支付与通知数据，不可恢复。\n\n是否执行？',
      TH: 'ยืนยันขั้นสุดท้าย\n\nลบข้อมูลชำระ/แจ้งเตือน {0} · {1} กู้คืนไม่ได้\n\nดำเนินการ?'
    },
    '결제 {0}': { EN: 'Payments {0}', JP: '決済 {0}', CH: '支付 {0}', TH: 'ชำระ {0}' },
    '노티 {0}': { EN: 'Notify {0}', JP: 'ノティ {0}', CH: '通知 {0}', TH: 'แจ้งเตือน {0}' },
    '삭제에 실패했습니다.': { EN: 'Purge failed.', JP: '削除に失敗しました。', CH: '删除失败。', TH: 'ลบไม่สำเร็จ' },
    '전산설정 정산 데이터 초기화 카드 본문': {
      EN: 'Keep: <strong>tb_commission_history</strong> (fee history), <strong>pg_trnsctn</strong> rows, HQ/merchant settlement settings (tb_settlement_setting, fee policy, distribution ratios, HQ cycles, etc.), ChillPay integrated settlement payloads. Delete: settlement runs (tb_settlement_run), receivable/recovery/collateral (rolling)/balance deduction rows that back merchant settlement, holds, distribution, and reports; reset only settled_yn on transactions to N.',
      JP: '保持: <strong>tb_commission_history</strong>（手数料履歴）、<strong>pg_trnsctn</strong> 取引行、本社・加盟店精算設定（tb_settlement_setting・手数料政策・配分比率・本社精算周期等）、チルペイ連携精算の原文。削除: 精算実行(tb_settlement_run)、加盟店精算・保留・流通・レポートの根拠となる実行・未収・回収・担保(ローリング)・残高控除、取引の settled_yn のみ N に初期化。',
      CH: '保留：<strong>tb_commission_history</strong>（手续费明细）、<strong>pg_trnsctn</strong> 交易行、总部/商户结算设置（tb_settlement_setting、手续费政策、分成比例、总部周期等）、ChillPay 集成结算原文。删除：结算执行(tb_settlement_run)、支撑商户结算/保留/分润/报表的执行、应收、回收、担保(滚动)、余额抵扣，仅将交易的 settled_yn 置为 N。',
      TH: 'คง: <strong>tb_commission_history</strong> ประวัติค่าธรรมเนียม <strong>pg_trnsctn</strong> การตั้งค่าชำระ HQ/ร้าน ต้นฉบับชำระรวม ChillPay ลบ: settlement run ลูกหนี้ กู้คืน หลักประกัน หัก settled_yn=N'
    },
    '부분:': { EN: 'Partial:', JP: '部分:', CH: '部分：', TH: 'บางส่วน:' },
    '미수금': { EN: 'Receivables', JP: '未収金', CH: '应收', TH: 'ลูกหนี้' },
    '미수금 환수요청·미수금': {
      EN: 'Receivable recovery requests & receivables',
      JP: '未収回収リクエスト・未収金',
      CH: '应收回收请求与应收',
      TH: 'คำขอกู้ลูกหนี้และลูกหนี้'
    },
    '환수금': { EN: 'Recoveries', JP: '回収金', CH: '回收款', TH: 'การกู้คืน' },
    '환수금(tb_settlement_recovery)': {
      EN: 'Recoveries (tb_settlement_recovery)',
      JP: '回収金(tb_settlement_recovery)',
      CH: '回收款(tb_settlement_recovery)',
      TH: 'การกู้คืน (tb_settlement_recovery)'
    },
    '담보': { EN: 'Collateral', JP: '担保', CH: '担保', TH: 'หลักประกัน' },
    '담보·롤링(tb_rolling_reserve)': {
      EN: 'Collateral / rolling (tb_rolling_reserve)',
      JP: '担保・ローリング(tb_rolling_reserve)',
      CH: '担保·滚动(tb_rolling_reserve)',
      TH: 'หลักประกัน/โรลลิ่ง (tb_rolling_reserve)'
    },
    '공제로그': { EN: 'Deduction log', JP: '控除ログ', CH: '抵扣日志', TH: 'บันทึกการหัก' },
    '잔액공제 로그(tb_balance_deduction)': {
      EN: 'Balance deduction log (tb_balance_deduction)',
      JP: '残高控除ログ(tb_balance_deduction)',
      CH: '余额抵扣日志(tb_balance_deduction)',
      TH: 'บันทึกหักยอด (tb_balance_deduction)'
    },
    '실행+연동': { EN: 'Runs + linked', JP: '実行+連動', CH: '执行+联动', TH: 'รัน+เชื่อม' },
    '실행+위 연동 일괄(미수·환수·담보·공제·실행·settled)': {
      EN: 'Batch runs + linked (receivables, recoveries, collateral, deductions, runs, settled)',
      JP: '実行+上記連動一括（未収・回収・担保・控除・実行・settled）',
      CH: '执行+上述联动批量（应收、回收、担保、抵扣、执行、settled）',
      TH: 'รัน+เชื่อมแบบกลุ่ม (ลูกหนี้ กู้คืน หลักประกัน หัก รัน settled)'
    },
    '데이터 유형': {
      EN: 'Data type',
      JP: 'データ種別',
      CH: '数据类型',
      TH: 'ประเภทข้อมูล'
    },
    '자동삭제': {
      EN: 'Auto delete',
      JP: '自動削除',
      CH: '自动删除',
      TH: 'ลบอัตโนมัติ'
    },
    '삭제(일)': {
      EN: 'Purge (days)',
      JP: '削除（日）',
      CH: '删除（天）',
      TH: 'ลบ (วัน)'
    },
    '보관(일)': {
      EN: 'Retain (days)',
      JP: '保持（日）',
      CH: '保留（天）',
      TH: 'เก็บ (วัน)'
    },
    '설명·연동': {
      EN: 'Description / linkage',
      JP: '説明・連携',
      CH: '说明·联动',
      TH: 'คำอธิบาย/การเชื่อม'
    },
    '불러오는 중…': {
      EN: 'Loading…',
      JP: '読み込み中…',
      CH: '加载中…',
      TH: 'กำลังโหลด…'
    },
    '전산설정 데이터 보관 표 하단 안내': {
      EN: 'Per row, <strong>Save</strong> posts all retention values in the table at once. <strong>Edit</strong> discards unsaved changes and reloads from the server. <strong>Reset</strong> clears only the saved override for that data type.',
      JP: '各行の<strong>[保存]</strong>は表に入力した保持値をまとめてサーバーに反映します。<strong>[修正]</strong>は未保存の変更を破棄してサーバー値を再読込します。<strong>[初期化]</strong>は当該データ種別の保存上書きのみ削除します。',
      CH: '每行<strong>[保存]</strong>将表中输入的保留值一次性提交服务器。<strong>[修改]</strong>放弃未保存更改并从服务器重新加载。<strong>[初始化]</strong>仅移除该数据类型的已保存覆盖。',
      TH: 'แต่ละแถว <strong>[บันทึก]</strong> ส่งค่าทั้งตาราง <strong>[แก้ไข]</strong> ยกเลิกการเปลี่ยนที่ยังไม่บันทึกแล้วโหลดใหม่ <strong>[รีเซ็ต]</strong> ลบเฉพาะ override ของประเภทนั้น'
    },
    '통합내역(칠페이) 동기화·로그 보관': {
      EN: 'ChillPay integrated list sync & log retention',
      JP: '統合明細（チルペイ）同期・ログ保持',
      CH: '集成明细（ChillPay）同步与日志保留',
      TH: 'รายการรวม (ChillPay) ซิงก์และเก็บล็อก'
    },
    '통합내역 화면에서 날짜를 비운 채 조회하면 「최근 동기화 범위」일만큼 TransactionDate 구간을 채웁니다. [검색 초기화]는 「피지거래내역 초기화 동기화(개월)」만큼 넓은 구간으로 맞춥니다. 로그 파일 보관(일)은 매일 새벽 데이터 보관 스케줄에서 <code>logs</code> 등의 오래된 .log/.gz 파일 삭제에 반영됩니다. 로그 메모리 보관(일)은 정책 저장용(추후 진단 버퍼 연동 시 사용).': {
      EN: 'Searching the integrated list with empty dates fills a TransactionDate window of Recent sync days. Search reset widens to ChillPay transaction init sync (months). Log file retention (days) is applied nightly by the retention job to delete old <code>logs</code> .log/.gz files. Log memory retention (days) is policy storage (for future diagnostic buffers).',
      JP: '統合明細で日付を空のまま検索すると「最近同期範囲」の日数分だけ TransactionDate 区間を埋めます。[検索リセット]は「PG取引明細初期化同期（月）」の広さに合わせます。ログファイル保持（日）は毎晩のデータ保持ジョブで<code>logs</code>等の古い .log/.gz 削除に反映されます。ログメモリ保持（日）は政策保存用（将来の診断バッファ連携）。',
      CH: '集成明细在日期为空查询时按「最近同步范围」天数填充 TransactionDate 区间。[搜索重置]会放宽到「PG 交易明细初始化同步（月）」。日志文件保留（天）由每日凌晨保留任务删除旧 <code>logs</code> 下 .log/.gz。日志内存保留（天）为策略占位（后续诊断缓冲）。',
      TH: 'หน้ารายการรวมค้นหาโดยไม่ใส่วันที่จะเติมช่วง TransactionDate ตามวัน「ช่วงซิงก์ล่าสุด」 [รีเซ็ตค้นหา] ขยายตามเดือน「ซิงก์เริ่มต้นธุรกรรม PG」 การเก็บไฟล์ล็อก (วัน) ลบ .log/.gz เก่าใน <code>logs</code> ทุกคืน หน่วยความจำล็อกเป็นที่เก็บนโยบาย'
    },
    '피지거래내역 초기화 동기화(개월)': {
      EN: 'ChillPay txn init sync (months)',
      JP: 'PG取引明細初期化同期（月）',
      CH: 'PG 交易明细初始化同步（月）',
      TH: 'ซิงก์เริ่มต้นธุรกรรม PG (เดือน)'
    },
    '피지거래내역 최근 동기화 범위(일)': {
      EN: 'ChillPay recent sync window (days)',
      JP: 'PG取引明細の最近同期範囲（日）',
      CH: 'PG 交易明细最近同步范围（天）',
      TH: 'ช่วงซิงก์ล่าสุดของธุรกรรม PG (วัน)'
    },
    '기본 3': { EN: 'Default 3', JP: '既定 3', CH: '默认 3', TH: 'ค่าเริ่ม 3' },
    '기본 2': { EN: 'Default 2', JP: '既定 2', CH: '默认 2', TH: 'ค่าเริ่ม 2' },
    '로그 메모리 보관(일)': {
      EN: 'Log memory retention (days)',
      JP: 'ログメモリ保持（日）',
      CH: '日志内存保留（天）',
      TH: 'เก็บหน่วยความจำล็อก (วัน)'
    },
    '기본 30': { EN: 'Default 30', JP: '既定 30', CH: '默认 30', TH: 'ค่าเริ่ม 30' },
    '로그 파일 보관(일)': {
      EN: 'Log file retention (days)',
      JP: 'ログファイル保持（日）',
      CH: '日志文件保留（天）',
      TH: 'เก็บไฟล์ล็อก (วัน)'
    },
    '기본 90': { EN: 'Default 90', JP: '既定 90', CH: '默认 90', TH: 'ค่าเริ่ม 90' },
    '수수료·정산 로직 (수수료내역)': {
      EN: 'Fee & settlement logic (fee list)',
      JP: '手数料・精算ロジック（手数料明細）',
      CH: '手续费·结算逻辑（手续费明细）',
      TH: 'ค่าธรรมเนียมและการชำระ (รายการค่าธรรมเนียม)'
    },
    '통화별 표는 결제·정산 통화(알파 코드)마다 소수 자릿수·잘리는 자리 처리를 지정합니다. 소수 자릿수가 0이면 금액은 정수만 의미하므로 「잘리는 자리 처리」는 비활성화되며 저장 시 그대로(버림, DOWN)로 통일됩니다. 목록 API는 행의 결제통화·거래통화에 맞춰 이 설정을 적용합니다. JSON에 없는 통화는 아래 「기본(통화 미지정)」값을 따릅니다. 조직항목설정 VIEW SETTING의 통화 열은 가맹 정책통화·거래통화를 표시하며, 총판 하위 가맹이 쓰는 모든 통화가 데이터에 존재하면 각 행에 그대로 나타납니다.': {
      EN: 'Per currency (alpha), set decimal places and rounding. If decimals are 0, amounts are integers so rounding mode is disabled and saved as DOWN. List APIs apply this per row payment/trade currency. Currencies missing from JSON use Default (unspecified currency) below. Org column settings show merchant policy and trade currencies; every currency used under a master distributor appears on its rows when present in data.',
      JP: '通貨（アルファ）ごとに小数桁と端数処理を指定します。小数桁が0の場合は金額は整数のみとなり「端数処理」は無効で保存時はDOWNに統一されます。一覧APIは行の決済通貨・取引通貨に合わせて適用します。JSONにない通貨は下の「既定（通貨未指定）」に従います。',
      CH: '按支付/结算货币（字母码）设置小数位与舍入。小数为 0 时金额为整数，舍入模式禁用并保存为 DOWN。列表 API 按行的支付/交易货币应用。JSON 未列货币使用下方「默认（未指定货币）」。',
      TH: 'ต่อสกุลเงิน กำหนนทศนิยมและปัดเศษ ถ้า 0 ปิดโหมดปัดและบันทึกเป็น DOWN'
    },
    '전산설정 수수료 기본 통화 미지정 안내': {
      EN: '<strong>Default (no currency)</strong> — used for currencies not listed below and unmapped rows.',
      JP: '<strong>既定（通貨未指定）</strong> — 下表にない通貨・未マッピング行に使用します。',
      CH: '<strong>默认（未指定货币）</strong> — 用于下表未列出的货币及未映射行。',
      TH: '<strong>ค่าเริ่ม (ไม่ระบุสกุล)</strong> — ใช้กับสกุลที่ไม่อยู่ในตาราง'
    },
    '소수 자릿수': {
      EN: 'Decimal places',
      JP: '小数桁',
      CH: '小数位数',
      TH: 'ทศนิยม'
    },
    '잘리는 자리 처리': {
      EN: 'Rounding mode',
      JP: '端数処理',
      CH: '舍入方式',
      TH: 'โหมดปัดเศษ'
    },
    '절상': { EN: 'Round up', JP: '切り上げ', CH: '向上取整', TH: 'ปัดขึ้น' },
    '반올림': { EN: 'Half up', JP: '四捨五入', CH: '四舍五入', TH: 'ปัดครึ่งขึ้น' },
    '그대로(버림)': {
      EN: 'Truncate (DOWN)',
      JP: 'そのまま（切り捨て）',
      CH: '直接截断（DOWN）',
      TH: 'ตัดทิ้ง (DOWN)'
    },
    '기준통화': {
      EN: 'Base currency',
      JP: '基準通貨',
      CH: '基准货币',
      TH: 'สกุลฐาน'
    },
    'PG대행사': {
      EN: 'PG agency',
      JP: 'PG代行',
      CH: 'PG代理商',
      TH: 'ตัวแทน PG'
    },
    '상위 총판': {
      EN: 'Parent master dist.',
      JP: '上位総販',
      CH: '上级总代',
      TH: 'ตัวแทนหลักต้นสาย'
    },
    '전산설정 수수료 통화 표 하단 안내': {
      EN: 'Per currency, <strong>decimals / rounding</strong> are locked by default. Use <strong>Edit</strong> (double confirm) to enter edit mode. Persist with <strong>Save</strong> in the Actions column (double confirm) or the bottom <strong>Save</strong>. <strong>Cancel</strong> reverts unsaved changes for that row only. <strong>Copy global</strong> is available in edit mode and copies the Default (unspecified) decimals/mode into the row (still requires Save).',
      JP: '通貨ごとの<strong>小数・端数</strong>は既定でロックです。<strong>[修正]</strong>で二重確認後に編集モードへ。反映は管理列の<strong>[保存]</strong>（二重確認）または画面下の<strong>保存</strong>。<strong>[キャンセル]</strong>は当該通貨行の未保存変更のみ戻します。<strong>[全体値]</strong>は編集モードでのみ使用でき、上の既定（通貨未指定）を行にコピーします（コピー後も[保存]が必要）。',
      CH: '各货币的<strong>小数·舍入</strong>默认锁定。<strong>[修改]</strong>经双重确认进入编辑模式。通过操作列<strong>[保存]</strong>（双重确认）或底部<strong>保存</strong>提交。<strong>[取消]</strong>仅还原该行未保存更改。<strong>[全局值]</strong>仅在编辑模式可用，将上方默认复制到该行（仍需保存）。',
      TH: 'ทศนิยม/ปัดล็อกตามค่าเริ่ม ใช้ <strong>แก้ไข</strong> ยืนยันสองครั้ง <strong>บันทึก</strong> ในคอลัมน์หรือด้านล่าง <strong>ยกเลิก</strong> เฉพาะแถว <strong>คัดลอกค่าทั่วโลก</strong> ในโหมดแก้ไข'
    },
    '결제 통화 (전역 표시 기준)': {
      EN: 'Payment currency (global display)',
      JP: '決済通貨（全体表示基準）',
      CH: '支付货币（全局显示基准）',
      TH: 'สกุลเงินชำระ (แสดงทั่วทั้งระบบ)'
    },
    '위 두 필드는 DB에 저장된 전역 기준(ISO 숫자·그에 대응하는 알파)만 보여 주며 이 화면에서는 변경할 수 없습니다. 아래 표는 서버에 정의된 지원 ISO 4217 숫자와 표시 통화(알파) 매핑 전체를 노출합니다(목록에 없는 숫자는 저장 시 기본값으로 정규화될 수 있습니다). 수수료 정책·조직 기준통화 등으로 통화가 정해지지 않을 때 결제내역·통합내역 상단 집계(단일통화 뷰)·칠페이 목록 meta의 기본 통화 폴백으로 전역 기준이 사용됩니다. API에는 <code>payDisplayCurrencyIsoNum</code>·<code>payDisplayCurrencyCode</code>·<code>payDisplayCurrencyCatalog</code>가 포함됩니다.': {
      EN: 'The two fields show the global ISO numeric and alpha stored in DB; they cannot be changed here. The table lists the full server mapping of supported ISO 4217 numerics to display alphas (unknown numerics may normalize on save). When fee policy or org base currency does not fix a currency, the global default backs pay list / integrated list totals (single-currency view) and ChillPay list meta. APIs include <code>payDisplayCurrencyIsoNum</code>, <code>payDisplayCurrencyCode</code>, <code>payDisplayCurrencyCatalog</code>.',
      JP: '上の2項目はDB保存の全体基準（ISO数字・対応アルファ）の表示のみで、この画面では変更できません。下表はサーバー定義のISO4217数字と表示通貨（アルファ）の対応一覧です。',
      CH: '两个字段只读显示数据库中的全局 ISO 数字与字母映射。下表列出服务器支持的全部 ISO 4217 数字与显示字母（未列数字可能在保存时规范化）。',
      TH: 'สองช่องแสดงค่า ISO ในฐานข้อมูล แก้ไขที่นี่ไม่ได้ ตารางด้านล่างแสดงแมปทั้งหมด'
    },
    '결제 통화 (ISO 숫자)': {
      EN: 'Payment currency (ISO numeric)',
      JP: '決済通貨（ISO数字）',
      CH: '支付货币（ISO 数字）',
      TH: 'สกุลเงิน ISO ตัวเลข'
    },
    '조회 시 서버 값': {
      EN: 'Server value when loaded',
      JP: '取得時のサーバー値',
      CH: '加载时的服务器值',
      TH: 'ค่าจากเซิร์ฟเวอร์ตอนโหลด'
    },
    '표시 통화(알파)': {
      EN: 'Display currency (alpha)',
      JP: '表示通貨（アルファ）',
      CH: '显示货币（字母码）',
      TH: 'สกุลแสดงผล (อักษร)'
    },
    'ISO 숫자 기준 자동': {
      EN: 'Auto from ISO numeric',
      JP: 'ISO数字基準で自動',
      CH: '按 ISO 数字自动',
      TH: 'อัตโนมัติจากตัวเลข ISO'
    },
    '지원 ISO 숫자 ↔ 표시 통화(알파) — 읽기 전용': {
      EN: 'Supported ISO numeric ↔ display currency (alpha) — read only',
      JP: '対応ISO数字 ↔ 表示通貨（アルファ）— 参照のみ',
      CH: '支持的 ISO 数字 ↔ 显示货币（字母）— 只读',
      TH: 'ISO ตัวเลข ↔ สกุลแสดงผล — อ่านอย่างเดียว'
    },
    'ISO 4217 숫자': {
      EN: 'ISO 4217 numeric',
      JP: 'ISO 4217 数字',
      CH: 'ISO 4217 数字',
      TH: 'ตัวเลข ISO 4217'
    },
    '전역 기준': {
      EN: 'Global default',
      JP: '全体基準',
      CH: '全局基准',
      TH: 'ค่าฐานทั่วโลก'
    },
    '결제 후속조치 (NOTI 환경설정 대응)': {
      EN: 'Pay follow-up (NOTI env)',
      JP: '決済フォロー（NOTI環境対応）',
      CH: '支付后续（NOTI 环境）',
      TH: 'การติดตามการชำระ (NOTI)'
    },
    '시간 선택 국가(기준 Zone)는 무효·이메일무효에 적용됩니다. 무효 기본은 당일 <strong>0:00~21:00</strong>. 수동무효(이메일)도 당일 <strong>시작~마감</strong>을 지정(마감 비우면 23:59). 환불은 <strong>태국</strong> 기준 결제일 <strong>익일</strong>의 <strong>시작 시각</strong>부터 일수입니다. 「설정(사용)」이 사용일 때만 편집할 수 있습니다. 아래 표에서 <strong>본사·총판</strong> 등 조직 단계별로 동일 네 기능의 허용 여부를 둡니다.': {
      EN: 'Selected country (Zone) applies to void and email void. Auto void default is same-day <strong>0:00–21:00</strong>. Manual void (email) uses same-day <strong>start–end</strong> (end empty = 23:59). Refunds count days from the <strong>next calendar day</strong> in <strong>Thailand</strong> after the payment date at the <strong>start time</strong>. Fields are editable only when the feature is Active. The table below caps the four features per org tier (HQ, master distributor, etc.).',
      JP: '時間選択国（基準Zone）は無効・メール無効に適用。自動無効の既定は当日<strong>0:00～21:00</strong>。手動無効（メール）も当日<strong>開始～終了</strong>（終了空欄は23:59）。返金は<strong>タイ</strong>基準で決済日の<strong>翌日</strong>の<strong>開始時刻</strong>から日数。「設定（使用）」が使用のときのみ編集可。下表で<strong>本社・総販</strong>など段階ごとに同じ4機能の許可上限を設定します。',
      CH: '所选国家（Zone）用于自动无效与邮件无效。自动无效默认当日<strong>0:00–21:00</strong>。邮件无效同样指定当日<strong>开始–结束</strong>（结束空为 23:59）。退款从<strong>泰国</strong>时区支付日<strong>次日</strong>的<strong>开始时刻</strong>起计天数。仅「启用」时可编辑。下表按<strong>总部·总代</strong>等阶段限制四项功能。',
      TH: 'โซนที่เลือกใช้กับ void/void อีเมล ค่าเริ่ม void 0:00–21:00 คืนเงินนับจากวันถัดไปในไทย'
    },
    '자동화 이메일 설정': {
      EN: 'Automated email',
      JP: '自動メール設定',
      CH: '自动化邮件设置',
      TH: 'อีเมลอัตโนมัติ'
    },
    'SMTP는 배치 알림·기타 자동 메일과 「이메일무효」 수동 요청 메일 발송에 공통으로 사용합니다. 아래 「이메일무효(ChillPay 등)」에서 수신처·제목·본문을 지정하면, 결제내역에서 이메일무효 실행 시 치환된 본문이 발송됩니다. 자동무효·자동환불·강제환불은 ChillPay Transaction API(무효/환불 요청)로 처리됩니다. 비밀번호는 저장 시에만 갱신하며, 조회 시에는 설정 여부만 표시됩니다.': {
      EN: 'SMTP is shared for batch alerts, other automated mail, and manual email-void requests. Recipients, subject, and body configured under Email void (ChillPay, etc.) are merged when you run email void from the pay list. Auto void / auto refund / force refund use the ChillPay Transaction API. Passwords update only on save; on load you only see whether one is set.',
      JP: 'SMTPはバッチ通知・その他自動メールと「メール無効」手動依頼メール送信に共通利用します。下の「メール無効」で宛先・件名・本文を指定すると、決済明細からメール無効実行時に差し替え本文が送信されます。自動無効・自動返金・強制返金はチルペイ取引APIで処理。パスワードは保存時のみ更新、取得時は設定有無のみ表示。',
      CH: 'SMTP 用于批量通知、其他自动邮件及「邮件无效」手动请求。在下方配置收件人、主题、正文后，从支付明细执行邮件无效会发送替换后的正文。自动无效/自动退款/强制退款走 ChillPay 交易 API。密码仅在保存时更新，查询时只显示是否已设置。',
      TH: 'SMTP ใช้ร่วมกับแจ้งเตือนแบตช์และ void อีเมล การตั้งค่าด้านล่างใช้เมื่อรัน void จากรายการชำระ'
    },
    'SMTP 호스트': { EN: 'SMTP host', JP: 'SMTPホスト', CH: 'SMTP 主机', TH: 'โฮสต์ SMTP' },
    'SMTP 포트': { EN: 'SMTP port', JP: 'SMTPポート', CH: 'SMTP 端口', TH: 'พอร์ต SMTP' },
    'SMTP 인증': { EN: 'SMTP auth', JP: 'SMTP認証', CH: 'SMTP 认证', TH: 'การยืนยัน SMTP' },
    'SMTP 사용자': { EN: 'SMTP user', JP: 'SMTPユーザー', CH: 'SMTP 用户', TH: 'ผู้ใช้ SMTP' },
    'SMTP 비밀번호 (변경 시만 입력)': {
      EN: 'SMTP password (only when changing)',
      JP: 'SMTPパスワード（変更時のみ入力）',
      CH: 'SMTP 密码（仅变更时填写）',
      TH: 'รหัสผ่าน SMTP (กรอกเมื่อเปลี่ยน)'
    },
    '비워두면 기존 유지': {
      EN: 'Leave blank to keep existing',
      JP: '空欄で既存を維持',
      CH: '留空则保持原值',
      TH: 'เว้นว่างเพื่อคงเดิม'
    },
    '비밀번호 저장됨': {
      EN: 'Password saved',
      JP: 'パスワード保存済み',
      CH: '密码已保存',
      TH: 'บันทึกรหัสผ่านแล้ว'
    },
    '저장됨': { EN: 'Saved', JP: '保存済み', CH: '已保存', TH: 'บันทึกแล้ว' },
    '미설정': { EN: 'Not set', JP: '未設定', CH: '未设置', TH: 'ยังไม่ตั้ง' },
    '발신 메일': { EN: 'From address', JP: '送信元メール', CH: '发件邮箱', TH: 'อีเมลผู้ส่ง' },
    '발신 표시명': { EN: 'From display name', JP: '送信表示名', CH: '发件显示名', TH: 'ชื่อผู้ส่ง' },
    '알림 수신(쉼표 구분)': {
      EN: 'Alert recipients (comma-separated)',
      JP: '通知受信（カンマ区切り）',
      CH: '告警收件人（逗号分隔）',
      TH: 'ผู้รับแจ้งเตือน (คั่นด้วยจุลภาค)'
    },
    '동기화 실패 시 메일': {
      EN: 'Email on sync failure',
      JP: '同期失敗時メール',
      CH: '同步失败时发邮件',
      TH: 'อีเมลเมื่อซิงก์ล้มเหลว'
    },
    '일일 요약 메일': {
      EN: 'Daily digest email',
      JP: '日次サマリーメール',
      CH: '每日摘要邮件',
      TH: 'อีเมลสรุปรายวัน'
    },
    '무효 배치 알림(예정)': {
      EN: 'Void batch notify (planned)',
      JP: '無効バッチ通知（予定）',
      CH: '无效批量通知（计划）',
      TH: 'แจ้งเตือน void แบตช์ (วางแผน)'
    },
    '환불 배치 알림(예정)': {
      EN: 'Refund batch notify (planned)',
      JP: '返金バッチ通知（予定）',
      CH: '退款批量通知（计划）',
      TH: 'แจ้งเตือนคืนเงินแบตช์ (วางแผน)'
    },
    '메모': { EN: 'Memo', JP: 'メモ', CH: '备注', TH: 'บันทึกย่อ' },
    '최종 수정': { EN: 'Last updated', JP: '最終更新', CH: '最后修改', TH: 'แก้ไขล่าสุด' },
    '이메일무효(수동 VOID 요청 메일)': {
      EN: 'Email void (manual VOID request mail)',
      JP: 'メール無効（手動VOID依頼メール）',
      CH: '邮件无效（手动 VOID 请求邮件）',
      TH: 'void อีเมล (คำขอ VOID แบบแมนนวล)'
    },
    '수신 이메일': { EN: 'Recipient email', JP: '受信メール', CH: '收件邮箱', TH: 'อีเมลผู้รับ' },
    '예: help@chillpay.co': {
      EN: 'e.g. help@chillpay.co',
      JP: '例: help@chillpay.co',
      CH: '例：help@chillpay.co',
      TH: 'เช่น help@chillpay.co'
    },
    '회사명(본문 치환)': {
      EN: 'Company name (body merge)',
      JP: '会社名（本文差し替え）',
      CH: '公司名称（正文替换）',
      TH: 'ชื่อบริษัท (แทนที่ในเนื้อหา)'
    },
    '담당자 성명(본문 치환)': {
      EN: 'Contact name (body merge)',
      JP: '担当者名（本文差し替え）',
      CH: '联系人姓名（正文替换）',
      TH: 'ชื่อผู้ติดต่อ (แทนที่ในเนื้อหา)'
    },
    '메일 제목': { EN: 'Subject', JP: '件名', CH: '邮件主题', TH: 'หัวข้ออีเมล' },
    '메일 본문': { EN: 'Body', JP: '本文', CH: '正文', TH: 'เนื้อหาอีเมล' },
    '영문 샘플·플레이스홀더는 저장 없이도 서버 기본값이 적용됩니다. 비우면 기본 영문 본문이 사용됩니다.': {
      EN: 'English samples and placeholders use server defaults even without saving. If empty, the default English body is used.',
      JP: '英語サンプル・プレースホルダーは未保存でもサーバー既定が適用されます。空欄なら既定の英語本文を使用します。',
      CH: '英文示例与占位符即使未保存也应用服务器默认。留空则使用默认英文正文。',
      TH: 'ตัวอย่างภาษาอังกฤษใช้ค่าเริ่มจากเซิร์ฟเวอร์ ว่างใช้เนื้อหาอังกฤษเริ่มต้น'
    },
    '테스트 수신 이메일': {
      EN: 'Test recipient email',
      JP: 'テスト受信メール',
      CH: '测试收件邮箱',
      TH: 'อีเมลรับทดสอบ'
    },
    '예: ziobizm@gmail.com': {
      EN: 'e.g. ziobizm@gmail.com',
      JP: '例: ziobizm@gmail.com',
      CH: '例：ziobizm@gmail.com',
      TH: 'เช่น ziobizm@gmail.com'
    },
    '실제 PG 수신처가 아닌, 본인 확인용 주소입니다. SMTP·템플릿으로 샘플 본문이 발송됩니다.': {
      EN: 'Not the live PSP inbox—only for self-check. A sample body is sent using saved SMTP and template.',
      JP: '実PGの受信先ではなく本人確認用です。保存済みSMTP・テンプレートでサンプル本文を送信します。',
      CH: '非线上 PG 收件箱，仅供本人校验。使用已保存的 SMTP 与模板发送示例正文。',
      TH: 'ไม่ใช่กล่องจริงของ PSP ใช้ตรวจสอบตนเอง ส่งตัวอย่างด้วย SMTP และเทมเพลตที่บันทึก'
    },
    '테스트 메일 발송': {
      EN: 'Send test mail',
      JP: 'テストメール送信',
      CH: '发送测试邮件',
      TH: 'ส่งอีเมลทดสอบ'
    },
    '전산설정 이메일무효 테스트 발송 안내': {
      EN: 'Subject is prefixed with [TEST] and a notice line is added at the top of the body. Results are logged under <strong>Ops → Mail log</strong>.',
      JP: '件名に[TEST]が付き、本文先頭に案内文が追加されます。結果は<strong>運用管理 → メールログ</strong>に記録されます。',
      CH: '主题会加上 [TEST]，正文顶部增加说明行。结果记录在<strong>运营管理 → 邮件日志</strong>。',
      TH: 'หัวข้อมี [TEST] และมีบรรทัดแนะนำด้านบนของเนื้อหา บันทึกที่<strong>ปฏิบัติการ → ล็อกอีเมล</strong>'
    },
    'Asia/Bangkok — 태국 (기본)': {
      EN: 'Asia/Bangkok — Thailand (default)',
      JP: 'Asia/Bangkok — タイ（既定）',
      CH: 'Asia/Bangkok — 泰国（默认）',
      TH: 'Asia/Bangkok — ไทย (ค่าเริ่ม)'
    },
    'Asia/Seoul — 대한민국': {
      EN: 'Asia/Seoul — South Korea',
      JP: 'Asia/Seoul — 韓国',
      CH: 'Asia/Seoul — 韩国',
      TH: 'Asia/Seoul — เกาหลีใต้'
    },
    'Asia/Tokyo — 일본': {
      EN: 'Asia/Tokyo — Japan',
      JP: 'Asia/Tokyo — 日本',
      CH: 'Asia/Tokyo — 日本',
      TH: 'Asia/Tokyo — ญี่ปุ่น'
    },
    'Asia/Shanghai — 중국': {
      EN: 'Asia/Shanghai — China',
      JP: 'Asia/Shanghai — 中国',
      CH: 'Asia/Shanghai — 中国',
      TH: 'Asia/Shanghai — จีน'
    },
    'Asia/Ho_Chi_Minh — 베트남': {
      EN: 'Asia/Ho_Chi_Minh — Vietnam',
      JP: 'Asia/Ho_Chi_Minh — ベトナム',
      CH: 'Asia/Ho_Chi_Minh — 越南',
      TH: 'Asia/Ho_Chi_Minh — เวียดนาม'
    },
    'Asia/Singapore — 싱가포르': {
      EN: 'Asia/Singapore — Singapore',
      JP: 'Asia/Singapore — シンガポール',
      CH: 'Asia/Singapore — 新加坡',
      TH: 'Asia/Singapore — สิงคโปร์'
    },
    'Asia/Manila — 필리핀': {
      EN: 'Asia/Manila — Philippines',
      JP: 'Asia/Manila — フィリピン',
      CH: 'Asia/Manila — 菲律宾',
      TH: 'Asia/Manila — ฟิลิปปินส์'
    },
    'Asia/Jakarta — 인도네시아(서)': {
      EN: 'Asia/Jakarta — Indonesia (Western)',
      JP: 'Asia/Jakarta — インドネシア（西部）',
      CH: 'Asia/Jakarta — 印尼（西部）',
      TH: 'Asia/Jakarta — อินโดนีเซีย (ตะวันตก)'
    },
    'Asia/Dubai — UAE': {
      EN: 'Asia/Dubai — UAE',
      JP: 'Asia/Dubai — UAE',
      CH: 'Asia/Dubai — 阿联酋',
      TH: 'Asia/Dubai — สหรัฐอาหรับเอมิเรตส์'
    },
    'America/New_York (미 동부)': {
      EN: 'America/New_York (US East)',
      JP: 'America/New_York（米東部）',
      CH: 'America/New_York（美东）',
      TH: 'America/New_York (สหรัฐฝั่งตะวันออก)'
    },
    'America/Los_Angeles (미 서부)': {
      EN: 'America/Los_Angeles (US West)',
      JP: 'America/Los_Angeles（米西部）',
      CH: 'America/Los_Angeles（美西）',
      TH: 'America/Los_Angeles (สหรัฐฝั่งตะวันตก)'
    },
    '(전산 표준시와 동일)': {
      EN: '(Same as ledger timezone)',
      JP: '（全算標準時と同じ）',
      CH: '（与账务标准时相同）',
      TH: '(เหมือนเขตเวลามาตรฐานบัญชี)'
    },
    '태국 (Asia/Bangkok)': {
      EN: 'Thailand (Asia/Bangkok)',
      JP: 'タイ（Asia/Bangkok）',
      CH: '泰国（Asia/Bangkok）',
      TH: 'ไทย (Asia/Bangkok)'
    },
    '일본 (Asia/Tokyo)': {
      EN: 'Japan (Asia/Tokyo)',
      JP: '日本（Asia/Tokyo）',
      CH: '日本（Asia/Tokyo）',
      TH: 'ญี่ปุ่น (Asia/Tokyo)'
    },
    '대한민국 (Asia/Seoul)': {
      EN: 'South Korea (Asia/Seoul)',
      JP: '韓国（Asia/Seoul）',
      CH: '韩国（Asia/Seoul）',
      TH: 'เกาหลีใต้ (Asia/Seoul)'
    },
    '중국 (Asia/Shanghai)': {
      EN: 'China (Asia/Shanghai)',
      JP: '中国（Asia/Shanghai）',
      CH: '中国（Asia/Shanghai）',
      TH: 'จีน (Asia/Shanghai)'
    },
    '베트남 (Asia/Ho_Chi_Minh)': {
      EN: 'Vietnam (Asia/Ho_Chi_Minh)',
      JP: 'ベトナム（Asia/Ho_Chi_Minh）',
      CH: '越南（Asia/Ho_Chi_Minh）',
      TH: 'เวียดนาม (Asia/Ho_Chi_Minh)'
    },
    '싱가포르 (Asia/Singapore)': {
      EN: 'Singapore (Asia/Singapore)',
      JP: 'シンガポール（Asia/Singapore）',
      CH: '新加坡（Asia/Singapore）',
      TH: 'สิงคโปร์ (Asia/Singapore)'
    },
    '필리핀 (Asia/Manila)': {
      EN: 'Philippines (Asia/Manila)',
      JP: 'フィリピン（Asia/Manila）',
      CH: '菲律宾（Asia/Manila）',
      TH: 'ฟิลิปปินส์ (Asia/Manila)'
    },
    '인도네시아 (Asia/Jakarta)': {
      EN: 'Indonesia (Asia/Jakarta)',
      JP: 'インドネシア（Asia/Jakarta）',
      CH: '印尼（Asia/Jakarta）',
      TH: 'อินโดนีเซีย (Asia/Jakarta)'
    },
    'UAE (Asia/Dubai)': {
      EN: 'UAE (Asia/Dubai)',
      JP: 'UAE（Asia/Dubai）',
      CH: '阿联酋（Asia/Dubai）',
      TH: 'UAE (Asia/Dubai)'
    },
    '미 동부 (America/New_York)': {
      EN: 'US East (America/New_York)',
      JP: '米東部（America/New_York）',
      CH: '美国东部（America/New_York）',
      TH: 'สหรัฐตะวันออก (America/New_York)'
    },
    '미 서부 (America/Los_Angeles)': {
      EN: 'US West (America/Los_Angeles)',
      JP: '米西部（America/Los_Angeles）',
      CH: '美国西部（America/Los_Angeles）',
      TH: 'สหรัฐตะวันตก (America/Los_Angeles)'
    },
    '0일 (메뉴·버튼 숨김)': {
      EN: '0 days (hide menu & buttons)',
      JP: '0日（メニュー・ボタン非表示）',
      CH: '0 天（隐藏菜单与按钮）',
      TH: '0 วัน (ซ่อนเมนูและปุ่ม)'
    },
    '168시간 (7일)': {
      EN: '168 hours (7 days)',
      JP: '168時間（7日）',
      CH: '168 小时（7 天）',
      TH: '168 ชั่วโมง (7 วัน)'
    },
    '1시간': { EN: '1 hour', JP: '1時間', CH: '1 小时', TH: '1 ชั่วโมง' },
    '2시간': { EN: '2 hours', JP: '2時間', CH: '2 小时', TH: '2 ชั่วโมง' },
    '3시간': { EN: '3 hours', JP: '3時間', CH: '3 小时', TH: '3 ชั่วโมง' },
    '6시간': { EN: '6 hours', JP: '6時間', CH: '6 小时', TH: '6 ชั่วโมง' },
    '12시간': { EN: '12 hours', JP: '12時間', CH: '12 小时', TH: '12 ชั่วโมง' },
    '18시간': { EN: '18 hours', JP: '18時間', CH: '18 小时', TH: '18 ชั่วโมง' },
    '24시간': { EN: '24 hours', JP: '24時間', CH: '24 小时', TH: '24 ชั่วโมง' },
    '36시간': { EN: '36 hours', JP: '36時間', CH: '36 小时', TH: '36 ชั่วโมง' },
    '48시간': { EN: '48 hours', JP: '48時間', CH: '48 小时', TH: '48 ชั่วโมง' },
    '72시간': { EN: '72 hours', JP: '72時間', CH: '72 小时', TH: '72 ชั่วโมง' },
    '1일': { EN: '1 day', JP: '1日', CH: '1 天', TH: '1 วัน' },
    '2일': { EN: '2 days', JP: '2日', CH: '2 天', TH: '2 วัน' },
    '3일': { EN: '3 days', JP: '3日', CH: '3 天', TH: '3 วัน' },
    '4일': { EN: '4 days', JP: '4日', CH: '4 天', TH: '4 วัน' },
    '5일': { EN: '5 days', JP: '5日', CH: '5 天', TH: '5 วัน' },
    '6일': { EN: '6 days', JP: '6日', CH: '6 天', TH: '6 วัน' },
    '7일': { EN: '7 days', JP: '7日', CH: '7 天', TH: '7 วัน' },
    '8일': { EN: '8 days', JP: '8日', CH: '8 天', TH: '8 วัน' },
    '9일': { EN: '9 days', JP: '9日', CH: '9 天', TH: '9 วัน' },
    '10일': { EN: '10 days', JP: '10日', CH: '10 天', TH: '10 วัน' },
    '11일': { EN: '11 days', JP: '11日', CH: '11 天', TH: '11 วัน' },
    '12일': { EN: '12 days', JP: '12日', CH: '12 天', TH: '12 วัน' },
    '13일': { EN: '13 days', JP: '13日', CH: '13 天', TH: '13 วัน' },
    '14일': { EN: '14 days', JP: '14日', CH: '14 天', TH: '14 วัน' },
    '15일': { EN: '15 days', JP: '15日', CH: '15 天', TH: '15 วัน' },
    '16일': { EN: '16 days', JP: '16日', CH: '16 天', TH: '16 วัน' },
    '17일': { EN: '17 days', JP: '17日', CH: '17 天', TH: '17 วัน' },
    '18일': { EN: '18 days', JP: '18日', CH: '18 天', TH: '18 วัน' },
    '19일': { EN: '19 days', JP: '19日', CH: '19 天', TH: '19 วัน' },
    '20일': { EN: '20 days', JP: '20日', CH: '20 天', TH: '20 วัน' },
    '21일': { EN: '21 days', JP: '21日', CH: '21 天', TH: '21 วัน' },
    '22일': { EN: '22 days', JP: '22日', CH: '22 天', TH: '22 วัน' },
    '23일': { EN: '23 days', JP: '23日', CH: '23 天', TH: '23 วัน' },
    '24일': { EN: '24 days', JP: '24日', CH: '24 天', TH: '24 วัน' },
    '25일': { EN: '25 days', JP: '25日', CH: '25 天', TH: '25 วัน' },
    '26일': { EN: '26 days', JP: '26日', CH: '26 天', TH: '26 วัน' },
    '27일': { EN: '27 days', JP: '27日', CH: '27 天', TH: '27 วัน' },
    '28일': { EN: '28 days', JP: '28日', CH: '28 天', TH: '28 วัน' },
    '29일': { EN: '29 days', JP: '29日', CH: '29 天', TH: '29 วัน' },
    '30일': { EN: '30 days', JP: '30日', CH: '30 天', TH: '30 วัน' },
    '45일': { EN: '45 days', JP: '45日', CH: '45 天', TH: '45 วัน' },
    '60일': { EN: '60 days', JP: '60日', CH: '60 天', TH: '60 วัน' },
    '90일': { EN: '90 days', JP: '90日', CH: '90 天', TH: '90 วัน' },
    '(저장값)': {
      EN: '(saved)',
      JP: '（保存値）',
      CH: '（已保存值）',
      TH: '(ค่าที่บันทึก)'
    },
    '일 (저장값)': {
      EN: 'd (saved)',
      JP: '日（保存値）',
      CH: '天（已保存值）',
      TH: 'วัน (ค่าที่บันทึก)'
    },
    '목록이 없습니다.': {
      EN: 'No rows.',
      JP: '一覧がありません。',
      CH: '没有数据。',
      TH: 'ไม่มีรายการ'
    },
    '현재': { EN: 'Current', JP: '現在', CH: '当前', TH: 'ปัจจุบัน' },
    '통화별 설정이 없습니다.': {
      EN: 'No per-currency settings.',
      JP: '通貨別設定がありません。',
      CH: '没有分货币设置。',
      TH: 'ไม่มีการตั้งค่ารายสกุลเงิน'
    },
    '보관 설정 항목이 없습니다.': {
      EN: 'No retention rows.',
      JP: '保持設定の行がありません。',
      CH: '没有保留配置行。',
      TH: 'ไม่มีแถวการเก็บรักษา'
    },
    /* 전산설정 데이터 보관 표 — 서버 id별 (보관:{id}:label|desc). KO는 서버 한글 fallback */
    '보관:PG_NOTIFY_INBOUND:label': {
      EN: 'PG notify inbound payload (DB)',
      JP: 'PGノティ受信原本(DB)',
      CH: 'PG 通知入库原文（数据库）',
      TH: 'ข้อความแจ้งเตือน PG ที่รับ (DB)'
    },
    '보관:PG_NOTIFY_INBOUND:desc': {
      EN: 'tb_pg_notify_inbound — notify body/meta from PG/NOTI, etc. Auto-delete by created_at when enabled.',
      JP: 'tb_pg_notify_inbound。PG/NOTI等で受信したノティ本文・メタ。自動削除はcreated_at基準。',
      CH: 'tb_pg_notify_inbound。PG/NOTI 等收到的通知正文与元数据。启用自动删除时按 created_at。',
      TH: 'tb_pg_notify_inbound เนื้อหา/เมตาแจ้งเตือนจาก PG/NOTI ลบอัตโนมัติตาม created_at'
    },
    '보관:PG_TRNSCTN:label': {
      EN: 'Payment transactions (DB)',
      JP: '決済取引明細(DB)',
      CH: '支付交易明细（数据库）',
      TH: 'ธุรกรรมชำระเงิน (DB)'
    },
    '보관:PG_TRNSCTN:desc': {
      EN: 'pg_trnsctn master (approve/cancel/void/refund, etc.). Auto-delete not wired (legal/settlement retention — values are policy/audit only).',
      JP: 'pg_trnsctn マスタ（承認・取消・無効・返金等）。自動削除は未連携（法的・精算保存 — 値は政策・監査用）。',
      CH: 'pg_trnsctn 主数据（批准、取消、作废、退款等）。未接自动删除（法律与结算留存 — 数值仅作政策/审计参考）。',
      TH: 'pg_trnsctn หลัก (อนุมัติ/ยกเลิก/void/คืนเงิน) ไม่ลิงก์ลบอัตโนมัติ (กฎหมาย/ชำระ)'
    },
    '보관:MERCHANT_REGISTRATION:label': {
      EN: 'Company registration (onboarding)',
      JP: '加盟店情報（登録）',
      CH: '商户信息（注册）',
      TH: 'ข้อมูลร้าน (ลงทะเบียน)'
    },
    '보관:MERCHANT_REGISTRATION:desc': {
      EN: 'tb_merchant_profile, etc. — first registration/application master. Auto-delete not wired (legal/contract — only retention target days stored).',
      JP: 'tb_merchant_profile 等。初回登録・申請マスタ。自動削除未連携（法的・契約 — 保持日数のみ政策保存）。',
      CH: 'tb_merchant_profile 等首次注册/申请主数据。未接自动删除（法律与合同 — 仅保存保留天数目标）。',
      TH: 'tb_merchant_profile ฯลฯ ลงทะเบียนครั้งแรก ไม่ลบอัตโนมัติ เก็บเฉพาณเป้าหมายวัน'
    },
    '보관:MERCHANT_MANAGEMENT:label': {
      EN: 'Merchant operations data',
      JP: '加盟店運用データ',
      CH: '商户运营数据',
      TH: 'ข้อมูลดำเนินงานร้านค้า'
    },
    '보관:MERCHANT_MANAGEMENT:desc': {
      EN: 'tb_org_unit, tb_merchant_pg_binding, tb_merchant_notify_url, permissions, VIEW settings, etc. Auto-delete not wired.',
      JP: 'tb_org_unit、tb_merchant_pg_binding、tb_merchant_notify_url、権限・VIEW設定等。自動削除未連携。',
      CH: 'tb_org_unit、tb_merchant_pg_binding、tb_merchant_notify_url、权限与 VIEW 设置等。未接自动删除。',
      TH: 'tb_org_unit ฯลฯ สิทธิ์ VIEW ไม่ลบอัตโนมัติ'
    },
    '보관:SETTLEMENT_RUN:label': {
      EN: 'Settlement run history',
      JP: '精算実行履歴',
      CH: '结算执行历史',
      TH: 'ประวัติรันชำระเงิน'
    },
    '보관:SETTLEMENT_RUN:desc': {
      EN: 'tb_settlement_run — batch settlement execution log. Auto-delete not wired.',
      JP: 'tb_settlement_run。精算バッチ実行記録。自動削除未連携。',
      CH: 'tb_settlement_run 结算批处理执行记录。未接自动删除。',
      TH: 'tb_settlement_run บันทึกรันชำระ ไม่ลบอัตโนมัติ'
    },
    '보관:SETTLEMENT_MANAGEMENT:label': {
      EN: 'Settlement administration',
      JP: '精算管理',
      CH: '结算管理',
      TH: 'การบริหารชำระเงิน'
    },
    '보관:SETTLEMENT_MANAGEMENT:desc': {
      EN: 'tb_settlement_setting, tb_rolling_reserve, tb_settlement_recovery, tb_merchant_receivable, etc. — settings, balances, recovery, receivables. Auto-delete not wired.',
      JP: 'tb_settlement_setting、tb_rolling_reserve、tb_settlement_recovery、tb_merchant_receivable 等。設定・残高・回収・未収。自動削除未連携。',
      CH: 'tb_settlement_setting、tb_rolling_reserve、tb_settlement_recovery、tb_merchant_receivable 等结算设置、余额、回收、应收。未接自动删除。',
      TH: 'tb_settlement_setting ฯลฯ การตั้งค่า ลูกหนี้ ไม่ลบอัตโนมัติ'
    },
    '보관:COMMISSION_HISTORY:label': {
      EN: 'Fee / settlement linkage history',
      JP: '手数料・精算連動履歴',
      CH: '手续费·结算联动历史',
      TH: 'ประวัติเชื่อมค่าธรรมเนียม/ชำระ'
    },
    '보관:COMMISSION_HISTORY:desc': {
      EN: 'tb_commission_history. Auto-delete not wired.',
      JP: 'tb_commission_history。自動削除未連携。',
      CH: 'tb_commission_history。未接自动删除。',
      TH: 'tb_commission_history ไม่ลบอัตโนมัติ'
    },
    '보관:MERCHANT_SETTLEMENT_LIST:label': {
      EN: 'Merchant settlement (fee list basis)',
      JP: '加盟店精算明細（手数料明細根拠）',
      CH: '商户结算明细（手续费依据）',
      TH: 'ชำระร้าน (ฐานค่าธรรมเนียม)'
    },
    '보관:MERCHANT_SETTLEMENT_LIST:desc': {
      EN: 'Query/aggregation basis for fee list and merchant settlement specs (trades, policy, per-currency rounding). Linked to payment transactions; retention target is stated separately for UI/audit. Auto-delete not wired.',
      JP: '精算管理の手数料明細・加盟店単位明細の照会・集計根拠。「決済取引明細」と連動するが画面・監査観点の保持目標は別記。自動削除未連携。',
      CH: '结算管理中手续费与商户结算规格的查询/聚合依据。与「支付交易明细」关联；界面与审计角度的保留目标单独标注。未接自动删除。',
      TH: 'ฐานรวมรายการค่าธรรมเนียม/ชำระร้าน เชื่อมธุรกรรม เป้าหมายเก็บแยก ไม่ลบอัตโนมัติ'
    },
    '보관:SETTLEMENT_REPORT_DATA:label': {
      EN: 'Settlement reports',
      JP: '精算レポート',
      CH: '结算报表',
      TH: 'รายงานชำระเงิน'
    },
    '보관:SETTLEMENT_REPORT_DATA:desc': {
      EN: 'Integrated settlement / report data (aggregates, summaries, runs, statements). Distinct from tb_settlement_run — retention target for report outputs. Auto-delete not wired.',
      JP: '統合精算・精算レポートの照会・算出・保存データ。「精算実行履歴(tb_settlement_run)」と区別 — レポート・集計結果の保持目標。自動削除未連携。',
      CH: '集成结算与报表的查询、计算、保存数据。与「结算执行历史(tb_settlement_run)」区分 — 报表与汇总结果的保留目标。未接自动删除。',
      TH: 'ข้อมูลรายงาน/สรุปชำระ แยกจาก settlement run ไม่ลบอัตโนมัติ'
    },
    '보관:SERVER_USAGE_DAILY:label': {
      EN: 'Server usage — daily aggregate',
      JP: 'サーバー使用量日次集計',
      CH: '服务器用量日汇总',
      TH: 'การใช้เซิร์ฟเวอร์รายวัน'
    },
    '보관:SERVER_USAGE_DAILY:desc': {
      EN: 'tb_server_usage_daily — traffic and memory peaks. Auto-delete by usage_date when enabled.',
      JP: 'tb_server_usage_daily。トラフィック・メモリピーク。自動削除はusage_date基準。',
      CH: 'tb_server_usage_daily 流量与内存峰值。启用自动删除时按 usage_date。',
      TH: 'tb_server_usage_daily ทราฟฟิก/หน่วยความจำ ลบตาม usage_date'
    },
    '보관:ORG_CHANGE_LOG:label': {
      EN: 'Org / code change log',
      JP: '組織・コード変更ログ',
      CH: '组织与代码变更日志',
      TH: 'บันทึกการเปลี่ยนองค์กร/รหัส'
    },
    '보관:ORG_CHANGE_LOG:desc': {
      EN: 'tb_org_unit_change_log. Auto-delete not wired.',
      JP: 'tb_org_unit_change_log。自動削除未連携。',
      CH: 'tb_org_unit_change_log。未接自动删除。',
      TH: 'tb_org_unit_change_log ไม่ลบอัตโนมัติ'
    },
    '보관:AUTH_TOKEN:label': {
      EN: 'Login / API tokens',
      JP: 'ログイン・APIトークン',
      CH: '登录与 API 令牌',
      TH: 'โทเค็นล็อกอิน/API'
    },
    '보관:AUTH_TOKEN:desc': {
      EN: 'auth_token — invalidation on expiry is primary. Policy days are guidance only (auto-delete not wired).',
      JP: 'auth_token。失効時の無効化中心。政策日数は案内用（自動削除未連携）。',
      CH: 'auth_token 以过期失效为主。政策天数仅供说明（未接自动删除）。',
      TH: 'auth_token หมดอายุเป็นหลัก วันเป็นแนวทาง ไม่ลบอัตโนมัติ'
    },
    '보관:NOTICE_BOARD:label': {
      EN: 'Notices',
      JP: 'お知らせ',
      CH: '公告',
      TH: 'ประกาศ'
    },
    '보관:NOTICE_BOARD:desc': {
      EN: 'pg_notice. Auto-delete not wired.',
      JP: 'pg_notice。自動削除未連携。',
      CH: 'pg_notice。未接自动删除。',
      TH: 'pg_notice ไม่ลบอัตโนมัติ'
    },
    '보관:NOTIFY_LOG_MEMORY:label': {
      EN: 'Notify / log memory (buffer)',
      JP: 'ノティ・ログメモリ（バッファ）',
      CH: '通知与日志内存（缓冲）',
      TH: 'หน่วยความจำแจ้งเตือน/ล็อก (บัฟเฟอร์)'
    },
    '보관:NOTIFY_LOG_MEMORY:desc': {
      EN: 'In-app/middleware memory retention target (days), NOTI-like setting. Not DB — policy only.',
      JP: 'アプリ・ミドルウェアのメモリ保持目標（日）。NOTI類似設定。DB別 — 政策のみ保存。',
      CH: '应用与中间件内存保留目标（日）。类 NOTI 设置。非数据库 — 仅存策略。',
      TH: 'เป้าหมายหน่วยความจำแอป/มิดเดิลแวร์ (วัน) ไม่ใช่ DB'
    },
    '보관:NOTIFY_LOG_FILE:label': {
      EN: 'Notify / audit file logs',
      JP: 'ノティ・監査ファイルログ',
      CH: '通知与审计文件日志',
      TH: 'ไฟล์ล็อกแจ้งเตือน/ตรวจสอบ'
    },
    '보관:NOTIFY_LOG_FILE:desc': {
      EN: 'VPS / external log file retention (days). Filesystem is separate — policy only.',
      JP: 'VPS・外部ログファイル保持（日）。ファイルシステムは別 — 政策のみ保存。',
      CH: 'VPS/外部日志文件保留（天）。文件系统另管 — 仅存策略。',
      TH: 'เก็บไฟล์ล็อก VPS (วัน) ระบบไฟล์แยก เก็บแค่นโยบาย'
    },
    '보관:USER_VIEW_SETTING:label': {
      EN: 'User VIEW / screen settings',
      JP: 'ユーザーVIEW・画面設定',
      CH: '用户 VIEW 与界面设置',
      TH: 'การตั้งค่า VIEW/หน้าจอผู้ใช้'
    },
    '보관:USER_VIEW_SETTING:desc': {
      EN: 'tb_user_view_setting. Auto-delete not wired.',
      JP: 'tb_user_view_setting。自動削除未連携。',
      CH: 'tb_user_view_setting。未接自动删除。',
      TH: 'tb_user_view_setting ไม่ลบอัตโนมัติ'
    },
    '보관:HQ_NOTIFY_MAPPING_AUDIT:label': {
      EN: 'Notify mapping / ledger change history',
      JP: 'ノティマッピング・全算設定変更履歴',
      CH: '通知映射与账务设置变更历史',
      TH: 'ประวัติแมปแจ้งเตือน/การตั้งค่าบัญชี'
    },
    '보관:HQ_NOTIFY_MAPPING_AUDIT:desc': {
      EN: 'Mapping JSON snapshots, etc. (extensible). Auto-delete not wired.',
      JP: 'マッピングJSONスナップショット等（将来拡張）。自動削除未連携。',
      CH: '映射 JSON 快照等（可扩展）。未接自动删除。',
      TH: 'สแนปชอต JSON แมป ฯลฯ ไม่ลบอัตโนมัติ'
    },
    '스케줄': { EN: 'Scheduler', JP: 'スケジュール', CH: '调度', TH: 'ตารางงาน' },
    '정책': { EN: 'Policy', JP: 'ポリシー', CH: '策略', TH: 'นโยบาย' },
    '자동삭제(스케줄)': {
      EN: 'Auto delete (scheduler)',
      JP: '自動削除（スケジュール）',
      CH: '自动删除（调度）',
      TH: 'ลบอัตโนมัติ (สเกจูล)'
    },
    '스케줄 미연동': {
      EN: 'Not linked to scheduler',
      JP: 'スケジュール未連携',
      CH: '未接入调度',
      TH: 'ไม่เชื่อมสเกจูล'
    },
    '전산설정을 저장하시겠습니까?': {
      EN: 'Save ledger settings?',
      JP: '全算設定を保存しますか？',
      CH: '要保存账务设置吗？',
      TH: 'บันทึกการตั้งค่าบัญชีหรือไม่'
    },
    '입력한 내용이 서버에 반영됩니다. 계속하시겠습니까?': {
      EN: 'Your input will be written to the server. Continue?',
      JP: '入力内容がサーバーに反映されます。続行しますか？',
      CH: '输入将写入服务器。是否继续？',
      TH: 'ข้อมูลจะถูกบันทึกที่เซิร์ฟเวอร์ ดำเนินการต่อหรือไม่'
    },
    '헬로 타임라인 설정을 저장하시겠습니까?': {
      EN: 'Save Hello timeline settings?',
      JP: 'Helloタイムライン設定を保存しますか？',
      CH: '要保存 Hello 时间线设置吗？',
      TH: 'บันทึกการตั้งค่าไทม์ไลน์ Hello หรือไม่'
    },
    '사용 여부·유지 시간(분)만 서버에 반영됩니다. 계속하시겠습니까?': {
      EN: 'Only enabled flag and duration (minutes) are saved. Continue?',
      JP: '使用可否・維持時間（分）のみサーバーに反映されます。続行しますか？',
      CH: '仅将开关与保持时间（分钟）提交服务器。是否继续？',
      TH: 'บันทึกเฉพาะการเปิด/ปิดและนาที ดำเนินการต่อหรือไม่'
    },
    '헬로 타임라인 저장을 사용할 수 없습니다.': {
      EN: 'Hello timeline save is unavailable.',
      JP: 'Helloタイムライン保存を利用できません。',
      CH: '无法保存 Hello 时间线。',
      TH: 'ไม่สามารถบันทึกไทม์ไลน์ Hello'
    },
    '헬로 타임라인이 저장되었습니다.': {
      EN: 'Hello timeline saved.',
      JP: 'Helloタイムラインを保存しました。',
      CH: 'Hello 时间线已保存。',
      TH: 'บันทึกไทม์ไลน์ Hello แล้ว'
    },
    '조직 단계별 후속조치 기능 허용': {
      EN: 'Per-level post-payment action caps',
      JP: '組織段階別の後続措置許可',
      CH: '按组织层级后续措施上限',
      TH: 'ขีดจำกัดการหลังชำระตามระดับองค์กร'
    },
    '단계별 허용 저장': {
      EN: 'Save per-level caps',
      JP: '段階別許可を保存',
      CH: '保存分级上限',
      TH: 'บันทึกตามระดับ'
    },
    '총본사가 단계마다 사용할 수 있는 네 가지 후속조치를 제한합니다. 전역 NOTI 설정이 꺼져 있으면 해당 기능은 동작하지 않습니다. 가맹점은 등록 시 개별 선택과 함께 적용되며(미선택 시 미사용), 이 표는 단계별 상한입니다.': {
      EN: 'HQ caps the four post-payment actions (auto void, email void, auto refund, force refund) per organization level. If global NOTI settings disable a feature, it does not run. Merchants also follow per-registration choices (unset = disabled); this grid is the per-level ceiling.',
      JP: '総本社が組織段階ごとに4種の後続措置（自動無効・メール無効・自動返金・強制返金）の使用上限を設定します。NOTI全体設定でオフの機能は動作しません。加盟店は登録時の個別選択も適用され（未選択は未使用）、この表は段階別の上限です。',
      CH: '总部分别限制四种后续措施（自动作废、邮件作废、自动退款、强制退款）的使用上限。若全局 NOTI 设置关闭某功能则不生效。商户另按注册时的选择（未选=未使用）；本表为各层级上限。',
      TH: 'สำนักงานใหญ่กำหนดเพดาน 4 การหลังชำระ (โมฆะอัตโนมัติ โมฆะอีเมล คืนเงินอัตโนมัติ บังคับคืนเงิน) ตามระดับองค์กร หาก NOTI ปิดฟีเจอร์จะไม่ทำงาน ร้านค้ายังตามที่เลือกตอนลงทะเบียน (ไม่เลือก=ไม่ใช้) ตารางนี้คือเพดานต่อระดับ'
    },
    '[단계별 허용 저장]으로만 반영됩니다(하단 전체 저장과 별도). 총본사·시스템 관리자만 변경할 수 있습니다.': {
      EN: 'Applies only via [Save per-level caps] (separate from Save at the bottom of the screen). Only root HQ and system administrators can change this.',
      JP: '「段階別許可を保存」でのみ反映されます（画面下部の一括保存とは別です）。総本社・システム管理者のみ変更できます。',
      CH: '仅通过【保存分级上限】生效（与画面底部整页保存分开）。仅总总部与系统管理员可修改。',
      TH: 'มีผลเฉพาะเมื่อกด [บันทึกตามระดับ] (แยกจากบันทึกทั้งหน้าด้านล่าง) เฉพาะสำนักงานใหญ่และผู้ดูแลระบบเท่านั้นที่แก้ไขได้'
    },
    '조직 단계별 후속조치 허용을 저장하시겠습니까?': {
      EN: 'Save per-tier pay follow caps?',
      JP: '組織段階別フォロー許可を保存しますか？',
      CH: '要保存各组织层级的后续功能上限吗？',
      TH: 'บันทึกขีดจำกัดตามระดับองค์กรหรือไม่'
    },
    '각 단계의 자동무효·이메일무효·자동환불·강제환불 사용 상한이 서버에 반영됩니다. 계속하시겠습니까?': {
      EN: 'Caps for auto void, email void, auto refund, and force refund per tier will be saved. Continue?',
      JP: '各段階の自動無効・メール無効・自動返金・強制返金の上限がサーバーに反映されます。続行しますか？',
      CH: '将把各层级的四项后续功能上限写入服务器。是否继续？',
      TH: 'บันทึกเพดานสี่ฟังก์ชันต่อระดับ ดำเนินการต่อหรือไม่'
    },
    'API를 사용할 수 없습니다.': {
      EN: 'API is not available.',
      JP: 'APIを利用できません。',
      CH: '无法调用 API。',
      TH: 'ใช้ API ไม่ได้'
    },
    '단계별 허용이 저장되었습니다.': {
      EN: 'Per-tier caps saved.',
      JP: '段階別許可を保存しました。',
      CH: '各层级上限已保存。',
      TH: 'บันทึกขีดจำกัดตามระดับแล้ว'
    },
    '헬로 타임라인을 서버 저장값으로 되돌릴까요?': {
      EN: 'Reload Hello timeline from the server?',
      JP: 'Helloタイムラインをサーバー保存値に戻しますか？',
      CH: '要从服务器重新加载 Hello 时间线吗？',
      TH: 'โหลดไทม์ไลน์ Hello จากเซิร์ฟเวอร์ใหม่หรือไม่'
    },
    '이 항목만 서버에서 다시 읽어 옵니다. 다른 입력란은 그대로입니다. 계속하시겠습니까?': {
      EN: 'Only this block is re-read from the server; other fields stay as-is. Continue?',
      JP: 'この項目のみサーバーから再取得します。他の入力はそのままです。続行しますか？',
      CH: '仅从服务器刷新此项，其他输入不变。是否继续？',
      TH: 'โหลดเฉพาะส่วนนี้จากเซิร์ฟเวอร์ ช่องอื่นคงเดิม'
    },
    '통화 {0} — 수수료·정산 형식을 수정하시겠습니까?': {
      EN: 'Currency {0} — edit fee/settlement format?',
      JP: '通貨 {0} — 手数料・精算形式を修正しますか？',
      CH: '货币 {0} — 要修改手续费/结算格式吗？',
      TH: 'สกุล {0} — แก้รูปแบบค่าธรรมเนียม/ชำระหรือไม่'
    },
    '소수 자릿수·잘리는 자리 처리는 수수료내역·정산 목록 API에 직접 반영됩니다. 편집 모드로 전환할까요?': {
      EN: 'Decimal places and rounding affect fee and settlement list APIs directly. Switch to edit mode?',
      JP: '小数桁・端数処理は手数料明細・精算一覧APIに直接反映されます。編集モードに切り替えますか？',
      CH: '小数位与舍入直接影响手续费与结算列表 API。切换到编辑模式？',
      TH: 'ทศนิยมและการปัดมีผลกับ API รายการ เปลี่ยนเป็นโหมดแก้ไขหรือไม่'
    },
    '전산설정을 서버에 저장하시겠습니까? (수수료·정산 통화 형식 포함)': {
      EN: 'Save ledger settings to the server (including fee/settlement currency formats)?',
      JP: '全算設定をサーバーに保存しますか？（手数料・精算通貨形式を含む）',
      CH: '要将账务设置保存到服务器吗（含手续费/结算货币格式）？',
      TH: 'บันทึกการตั้งค่าบัญชีรวมรูปแบบสกุลหรือไม่'
    },
    '통화별 소수·라운딩을 포함해 화면의 전산설정 전체가 기록됩니다. 저장 즉시 목록 API·정산 표시에 영향을 줄 수 있습니다. 정말 저장하시겠습니까?': {
      EN: 'The whole ledger screen—including per-currency decimals and rounding—will be persisted. This may affect list APIs and settlement display immediately. Save for real?',
      JP: '通貨別小数・丸めを含む画面の全算設定全体が記録されます。保存直後に一覧API・精算表示へ影響する場合があります。本当に保存しますか？',
      CH: '将保存整个账务画面（含各货币小数与舍入），可能立即影响列表 API 与结算展示。确定保存？',
      TH: 'บันทึกทั้งหน้ารวมทศนิยม/ปัด อาจกระทบ API ทันที ยืนยันหรือไม่'
    },
    '이 통화 행 편집을 취소하시겠습니까?': {
      EN: 'Cancel edits for this currency row?',
      JP: 'この通貨行の編集を取り消しますか？',
      CH: '要取消该货币行的编辑吗？',
      TH: 'ยกเลิกการแก้แถวสกุลนี้หรือไม่'
    },
    '이 행에서 저장하지 않은 변경만 되돌립니다. 다른 입력란은 그대로입니다. 계속하시겠습니까?': {
      EN: 'Only unsaved changes on this row are reverted; other inputs stay. Continue?',
      JP: 'この行の未保存変更のみ戻します。他の入力はそのままです。続行しますか？',
      CH: '仅还原此行未保存更改，其他输入不变。是否继续？',
      TH: 'ย้อนเฉพาะแถวนี้ ช่องอื่นคงเดิม'
    },
    '먼저 해당 통화 행의 [수정]을 눌러 편집 모드로 전환한 뒤 [전역값]을 사용할 수 있습니다.': {
      EN: 'Press Edit on that currency row to enter edit mode before using Copy global.',
      JP: '先に当該通貨行の[修正]で編集モードにしてから[全体値]を使用してください。',
      CH: '请先点击该货币行的 [修改] 进入编辑模式后再使用 [全局值]。',
      TH: 'กดแก้ไขที่แถวสกุลนี้ก่อน จึงใช้ค่าทั่วโลกได้'
    },
    '전역 기본값을 이 통화에 복사하시겠습니까?': {
      EN: 'Copy global defaults into this currency?',
      JP: '全体既定をこの通貨にコピーしますか？',
      CH: '要将全局默认值复制到该货币吗？',
      TH: 'คัดลอกค่าเริ่มทั่วไปมาที่สกุลนี้หรือไม่'
    },
    '위쪽 「기본(통화 미지정)」의 소수 자릿수·잘리는 자리 처리로 이 행을 덮어씁니다. 서버 반영은 [저장]이 필요합니다. 계속하시겠습니까?': {
      EN: 'This overwrites the row with the Default (unspecified currency) decimals and rounding. You still need Save to persist. Continue?',
      JP: '上の「既定（通貨未指定）」の小数桁・端数処理でこの行を上書きします。反映には[保存]が必要です。続行しますか？',
      CH: '将用上方「默认（未指定货币）」的小数位与舍入覆盖此行，仍需保存才生效。是否继续？',
      TH: 'เขียนทับด้วยค่าเริ่มด้านบน ต้องบันทึกเพื่อลงเซิร์ฟเวอร์'
    },
    '보관 정책을 저장하시겠습니까?': {
      EN: 'Save retention policy?',
      JP: '保持政策を保存しますか？',
      CH: '要保存保留策略吗？',
      TH: 'บันทึกนโยบายการเก็บหรือไม่'
    },
    '표에 입력한 전체 보관 값이 함께 저장됩니다. 계속하시겠습니까?': {
      EN: 'All retention values in the table will be saved together. Continue?',
      JP: '表に入力した保持値がまとめて保存されます。続行しますか？',
      CH: '将一并保存表中全部保留值。是否继续？',
      TH: 'บันทึกค่าทั้งตารางพร้อมกัน'
    },
    '서버에 저장된 값으로 다시 불러오시겠습니까?': {
      EN: 'Reload saved values from the server?',
      JP: 'サーバー保存値を再読込しますか？',
      CH: '要从服务器重新加载已保存的值吗？',
      TH: 'โหลดค่าที่บันทึกจากเซิร์ฟเวอร์ใหม่หรือไม่'
    },
    '저장하지 않은 변경이 사라집니다. 계속하시겠습니까?': {
      EN: 'Unsaved changes will be lost. Continue?',
      JP: '未保存の変更が失われます。続行しますか？',
      CH: '未保存的更改将丢失。是否继续？',
      TH: 'การเปลี่ยนที่ยังไม่บันทึกจะหาย ดำเนินการต่อหรือไม่'
    },
    '이 데이터 유형의 저장된 보관 설정을 초기화하시겠습니까?': {
      EN: 'Reset saved retention overrides for this data type?',
      JP: 'このデータ種別の保存された保持設定を初期化しますか？',
      CH: '要重置该数据类型的已保存保留覆盖吗？',
      TH: 'รีเซ็ตการเก็บที่บันทึกของประเภทนี้หรือไม่'
    },
    '해당 유형의 덮어쓰기만 제거되고 기본값이 적용됩니다. 계속하시겠습니까?': {
      EN: 'Only overrides for that type are removed; defaults apply. Continue?',
      JP: '当該種別の上書きのみ削除され既定が適用されます。続行しますか？',
      CH: '仅移除该类型的覆盖并应用默认值。是否继续？',
      TH: 'ลบเฉพาะ override ใช้ค่าเริ่ม'
    },
    '데이터 초기화가 됩니다.\n\n전체 데이터를 초기화합니다.\n단, 등록된 업체 정보는 유지됩니다.\n\n[확인]으로 다음 안내로 진행하고, [취소]로 중단합니다.': {
      EN: 'Data will be reset.\n\nAll operational data will be cleared.\nRegistered company records are kept.\n\nPress OK to continue to the next prompt, or Cancel to stop.',
      JP: 'データを初期化します。\n\n運用データを初期化します。\nただし登録済みの会社情報は保持します。\n\n[OK]で次の確認へ、[キャンセル]で中止します。',
      CH: '将初始化数据。\n\n将清空运营数据。\n已登记的公司信息会保留。\n\n[确定] 进入下一步确认，[取消] 中止。',
      TH: 'จะล้างข้อมูล\n\nล้างข้อมูลดำเนินงาน\nคงข้อมูลบริษัทที่ลงทะเบียน\n\nตกลงไปขั้นถัดไป ยกเลิกหยุด'
    },
    '마지막 확인입니다.\n\n확인을 누르면 서버에서 운영 데이터가 삭제됩니다. 복구할 수 없습니다.\n취소를 누르면 아무 작업도 하지 않습니다.\n\n정말 전체 데이터 초기화를 실행하시겠습니까?': {
      EN: 'Final confirmation.\n\nOK deletes operational data on the server. This cannot be undone.\nCancel does nothing.\n\nReally run full operational reset?',
      JP: '最終確認です。\n\nOKでサーバーの運用データが削除されます。元に戻せません。\nキャンセルは何もしません。\n\n本当に全データ初期化を実行しますか？',
      CH: '最后确认。\n\n确定将删除服务器上的运营数据，不可恢复。\n取消则不执行任何操作。\n\n确定执行全部运营数据初始化？',
      TH: 'ยืนยันครั้งสุดท้าย\n\nตกลงลบข้อมูลดำเนินงาน กู้คืนไม่ได้\nยกเลิกไม่ทำอะไร\n\nยืนยันล้างทั้งหมดหรือไม่'
    },
    '운영 데이터 초기화가 완료되었습니다.': {
      EN: 'Operational data reset completed.',
      JP: '運用データの初期化が完了しました。',
      CH: '运营数据初始化已完成。',
      TH: 'ล้างข้อมูลดำเนินงานเสร็จแล้ว'
    },
    '초기화에 실패했습니다.': {
      EN: 'Reset failed.',
      JP: '初期化に失敗しました。',
      CH: '初始化失败。',
      TH: 'ล้างข้อมูลล้มเหลว'
    },
    '정산 데이터 일부만 삭제합니다.\n\n범위: {0}\n수수료내역·거래·본사 정산 설정은 유지됩니다.\n\n[확인]으로 다음 안내로 진행합니다.': {
      EN: 'Delete part of settlement data only.\n\nScope: {0}\nFee history, transactions, and HQ settlement settings are kept.\n\nPress OK to continue.',
      JP: '精算データの一部のみ削除します。\n\n範囲: {0}\n手数料履歴・取引・本社精算設定は保持します。\n\n[OK]で次の確認へ進みます。',
      CH: '仅删除部分结算数据。\n\n范围：{0}\n保留手续费明细、交易与总部结算设置。\n\n[确定] 继续下一步。',
      TH: 'ลบบางส่วนของข้อมูลชำระ\n\nขอบเขต: {0}\nคงประวัติค่าธรรมเนียมและธุรกรรม'
    },
    '마지막 확인입니다.\n\n복구할 수 없습니다. 실행하시겠습니까?': {
      EN: 'Final confirmation.\n\nThis cannot be undone. Proceed?',
      JP: '最終確認です。\n\n元に戻せません。実行しますか？',
      CH: '最后确认。\n\n不可恢复。是否执行？',
      TH: 'ยืนยันครั้งสุดท้าย\n\nกู้คืนไม่ได้ ดำเนินการต่อหรือไม่'
    },
    '미수금·요청': {
      EN: 'Receivables & requests',
      JP: '未収金・リクエスト',
      CH: '应收与请求',
      TH: 'ลูกหนี้และคำขอ'
    },
    '정산실행+연동 일괄': {
      EN: 'Settlement runs + linked batch',
      JP: '精算実行+連動一括',
      CH: '结算执行+联动批量',
      TH: 'รันชำระ+ชุดเชื่อม'
    },
    '담보(롤링)': {
      EN: 'Collateral (rolling)',
      JP: '担保（ローリング）',
      CH: '担保（滚动）',
      TH: 'หลักประกัน (โรลลิ่ง)'
    },
    '정산 데이터 초기화가 완료되었습니다.': {
      EN: 'Settlement data reset completed.',
      JP: '精算データの初期化が完了しました。',
      CH: '结算数据初始化已完成。',
      TH: 'ล้างข้อมูลชำระเสร็จแล้ว'
    },
    '정산 운영 데이터를 삭제합니다.\n\n범위: 전체(ALL)\n유지: 수수료내역(tb_commission_history), 거래(pg_trnsctn), 본사·가맹 정산 설정, 칠페이 통합정산 원문.\n삭제: 정산실행·미수·환수·담보·공제·보류/유통/리포트 근거 행, 거래의 정산반영 플래그(settled_yn)만 N.\n\n[확인]으로 다음 안내로 진행합니다.': {
      EN: 'Delete settlement operational data.\n\nScope: ALL\nKeep: fee history (tb_commission_history), transactions (pg_trnsctn), HQ/merchant settlement settings, ChillPay integrated payloads.\nDelete: settlement runs, receivables, recoveries, collateral, deductions, hold/distribution/report basis rows; set settled_yn to N only.\n\nPress OK to continue.',
      JP: '精算運用データを削除します。\n\n範囲: 全体(ALL)\n保持: 手数料履歴、取引、本社・加盟店精算設定、チルペイ統合原文。\n削除: 精算実行・未収・回収・担保・控除・保留/流通/レポート根拠行、取引のsettled_ynのみN。\n\n[OK]で次へ。',
      CH: '删除结算运营数据。\n\n范围：全部(ALL)\n保留：手续费明细、交易、总部/商户结算设置、ChillPay 集成原文。\n删除：结算执行、应收、回收、担保、抵扣及相关依据行；仅将 settled_yn 置 N。\n\n[确定] 继续。',
      TH: 'ลบข้อมูลชำระ ขอบเขต ALL คงค่าธรรมเนียมและธุรกรรม'
    },
    '실행 {0}건, settled 해제 {1}건': {
      EN: 'Runs {0}, settled cleared {1}',
      JP: '実行 {0}件、settled解除 {1}件',
      CH: '执行 {0} 条，解除 settled {1} 条',
      TH: 'รัน {0} รายการ เคลียร์ settled {1}'
    },
    '미수요청 {0}': { EN: 'Recv. req. {0}', JP: '未収リクエスト {0}', CH: '应收请求 {0}', TH: 'คำขอลูกหนี้ {0}' },
    '미수 {0}': { EN: 'Recv. {0}', JP: '未収 {0}', CH: '应收 {0}', TH: 'ลูกหนี้ {0}' },
    '환수 {0}': { EN: 'Recovery {0}', JP: '回収 {0}', CH: '回收 {0}', TH: 'กู้คืน {0}' },
    '담보 {0}': { EN: 'Collateral {0}', JP: '担保 {0}', CH: '担保 {0}', TH: 'หลักประกัน {0}' },
    '공제 {0}': { EN: 'Deduction {0}', JP: '控除 {0}', CH: '抵扣 {0}', TH: 'หัก {0}' },
    '실행 {0}': { EN: 'Runs {0}', JP: '実行 {0}', CH: '执行 {0}', TH: 'รัน {0}' },
    'settled해제 {0}': {
      EN: 'settled cleared {0}',
      JP: 'settled解除 {0}',
      CH: 'settled 解除 {0}',
      TH: 'เคลียร์ settled {0}'
    },
    '총본사 또는 시스템 관리자만 저장할 수 있습니다.': {
      EN: 'Only HQ or system admin can save.',
      JP: '総本社またはシステム管理者のみ保存できます。',
      CH: '仅总部或系统管理员可保存。',
      TH: 'เฉพาะสำนักงานใหญ่หรือผู้ดูแลระบบ'
    },
    '자동무효·이메일무효를 함께 켠 경우: 시작은 지정할 수 없고 자동무효 마감({0}) 다음 분부터입니다. 마감은 오른쪽 시간으로 설정합니다.': {
      EN: 'When auto void and email void are both on: start is fixed to the minute after auto void end ({0}). Set end on the right.',
      JP: '自動無効・メール無効を同時にオン: 開始は指定できず、自動無効終了({0})の次分からです。終了は右の時刻で設定します。',
      CH: '同时开启自动无效与邮件无效时：开始时间不可选，从自动无效结束 ({0}) 的下一分钟起。结束在右侧时间设置。',
      TH: 'เปิด void สองแบบพร้อมกัน: เริ่มหลังเวลาปิด void อัตโนมัติ ({0})'
    },
    '자동무효·이메일무효를 함께 켠 경우: 시작 입력은 비활성입니다. 마감은 오른쪽 시간으로 설정합니다.': {
      EN: 'When auto void and email void are both on: start input is disabled. Set end on the right.',
      JP: '自動無効・メール無効を同時にオン: 開始入力は無効です。終了は右の時刻で設定します。',
      CH: '同时开启时：开始输入禁用，在右侧设置结束。',
      TH: 'เปิดคู่กัน: ปิดการเริ่ม ตั้งเวลาปิดทางขวา'
    },
    '비우면 당일 0:00부터. 마감은 오른쪽 시간(비우면 23:59).': {
      EN: 'Empty start = 00:00 same day. End on the right (empty = 23:59).',
      JP: '開始空欄は当日0:00。終了は右（空欄は23:59）。',
      CH: '开始留空为当日 0:00；结束在右侧（留空 23:59）。',
      TH: 'เริ่มว่าง=00:00 วันนั้น ปิดทางขวา ว่าง=23:59'
    },
    '소수 자릿수가 0이면 금액은 정수이며, 잘리는 자리 처리는 적용되지 않습니다.': {
      EN: 'With 0 decimals, amounts are integers and rounding does not apply.',
      JP: '小数桁0の場合、金額は整数で端数処理は適用されません。',
      CH: '小数为 0 时金额为整数，不适用舍入。',
      TH: 'ทศนิยม 0 จำนวนเป็นจำนวนเต็ม ไม่ปัด'
    },
    '관리 열 [수정]으로 편집 모드 전환 후 변경할 수 있습니다.': {
      EN: 'Switch to edit mode with Edit in the Actions column before changing.',
      JP: '管理列の[修正]で編集モードにしてから変更してください。',
      CH: '请先在管理列使用 [修改] 进入编辑模式再更改。',
      TH: 'กดแก้ไขในคอลัมน์จัดการก่อน'
    },
    '소수 자릿수가 0이면 잘리는 자리 처리는 적용되지 않습니다.': {
      EN: 'With 0 decimals, rounding does not apply.',
      JP: '小数桁0の場合、端数処理は適用されません。',
      CH: '小数为 0 时不适用舍入。',
      TH: 'ทศนิยม 0 ไม่ปัดเศษ'
    },
    '편집 모드(소수·잘리는 자리) 전환 — 연속 확인 후 활성화됩니다.': {
      EN: 'Enter edit mode (decimals / rounding) — enabled after double confirm.',
      JP: '編集モード（小数・端数）— 二重確認後に有効化。',
      CH: '进入编辑模式（小数/舍入）— 双重确认后启用。',
      TH: 'โหมดแก้ไข (ทศนิยม/ปัด) หลังยืนยันสองครั้ง'
    },
    '전산설정 전체를 서버에 저장합니다. 수수료·정산 통화 형식이 즉시 반영될 수 있습니다.': {
      EN: 'Saves the whole ledger screen; fee/settlement formats may apply immediately.',
      JP: '画面の全算設定をサーバーに保存します。手数料・精算通貨形式が即時反映される場合があります。',
      CH: '保存整个账务画面；手续费/结算货币格式可能立即生效。',
      TH: 'บันทึกทั้งหน้า รูปแบบอาจมีผลทันที'
    },
    '이 통화 행의 미저장 편집만 되돌리고 잠급니다.': {
      EN: 'Reverts unsaved edits for this currency row and locks it.',
      JP: 'この通貨行の未保存編集のみ戻してロックします。',
      CH: '仅还原该货币行未保存编辑并锁定。',
      TH: 'ย้อนแก้ไขที่ยังไม่บันทึกของแถวนี้แล้วล็อก'
    },
    '편집 모드에서만 사용 가능. 기본(통화 미지정) 소수·처리를 이 통화에 복사합니다.': {
      EN: 'Edit mode only. Copies Default (unspecified) decimals/mode into this currency.',
      JP: '編集モードのみ。既定（通貨未指定）の小数・処理をこの通貨にコピーします。',
      CH: '仅编辑模式可用。将默认（未指定货币）的小数与处理方式复制到该货币。',
      TH: 'ใช้ได้ในโหมดแก้ไขเท่านั้น คัดลอกค่าเริ่มมาที่สกุลนี้'
    },
    '전역값': {
      EN: 'Global default',
      JP: '全体値',
      CH: '全局值',
      TH: 'ค่าทั่วโลก'
    },
    '전산 표준시와 동일': {
      EN: 'Same as ledger timezone',
      JP: '全算標準時と同じ',
      CH: '与账务标准时相同',
      TH: 'เหมือนเขตเวลามาตรฐานบัญชี'
    },
    /* HQ /hq/notifyInbound — 노티受信情報 (screens L + app.js pgAdminUiT) */
    '노티 수령 정보': {
      EN: 'Notify inbound log',
      JP: 'ノティ受信ログ',
      CH: '通知接收记录',
      TH: 'บันทึกการรับแจ้งเตือน'
    },
    '노티수령정보': {
      EN: 'Notify inbound',
      JP: 'ノティ受信情報',
      CH: '通知接收信息',
      TH: 'ข้อมูลรับแจ้งเตือน'
    },
    '노티미들웨어·PG(칠페이 등)가 본 시스템의 노티 수신 URL(<code>/api/open/pg-notify/…</code>)로 전송한 요청을 저장한 로그입니다. 목록의 채널 열은 수신 경로 정보 표시용입니다. 대상코드·채널은 신규 수신 건부터 채워집니다(V72). 노티 대상에 연결 총판이 있으면 동일 MID라도 그 총판 트리 안에서만 분기하며, 총판 기준통화와 본문 통화가 다르면 처리 열에 통화불일치(수신경로)로 격리됩니다. <strong>수신성격</strong>은 NOTI가 요청 시 <code>X-Icopay-Notify-Delivery: LIVE|RETRY</code> 또는 <code>X-Noti-Attempt</code>(1=라이브, 2+=재전송) 헤더를 보낼 때만 구분되며, 없으면 「미표시」입니다. 바인딩·매핑을 고친 뒤 과거 건을 결제내역에 붙이려면 본문 보기 모달의 <strong>결제내역 재반영</strong>을 사용하세요(원문이 잘린 건은 불가).': {
      EN: 'Log of requests that the notify middleware and PSPs (e.g. ChillPay) sent to this system’s notify ingress URL (<code>/api/open/pg-notify/…</code>). The Channel column shows the ingress path. Target code and channel are populated from newly received rows onward (V72). When a master distributor is linked to the notify target, the same MID is routed only inside that distributor tree; if the distributor base currency differs from the payload currency, the Process column isolates the row as currency mismatch (ingress path). <strong>Ingress delivery kind</strong> is distinguished only when NOTI sends <code>X-Icopay-Notify-Delivery: LIVE|RETRY</code> or <code>X-Noti-Attempt</code> (1=live, 2+=retry); otherwise it shows 「Not shown」. To attach past rows to payment history after fixing bindings or mapping, use <strong>Replay to payment list</strong> in the body modal (not available if the stored body was truncated).',
      JP: 'ノティミドルウェア・PG（チルペイ等）が本システムのノティ受信URL（<code>/api/open/pg-notify/…</code>）へ送信したリクエストを保存したログです。一覧のチャネル列は受信経路の表示用です。対象コード・チャネルは新規受信分から埋まります（V72）。ノティ先に紐付け総販がある場合、同一MIDでもその総販ツリー内でのみ振り分けられ、総販の基準通貨と本文の通貨が異なると処理列に通貨不一致（受信経路）として隔離されます。<strong>受信性質</strong>は、NOTIがリクエスト時に<code>X-Icopay-Notify-Delivery: LIVE|RETRY</code>または<code>X-Noti-Attempt</code>（1=ライブ、2以上=再送）ヘッダを送った場合のみ区別され、無い場合は「非表示」です。バインディング・マッピング修正後に過去分を決済明細へ紐づけるには、本文表示モーダルの<strong>決済明細への再反映</strong>を使用してください（原文が切り詰められた件は不可）。',
      CH: '由通知中间件与 PSP（如 ChillPay）发往本系统通知接入 URL（<code>/api/open/pg-notify/…</code>）并保存的请求日志。列表中的「渠道」列用于展示接收路径。目标代码与渠道自新接收记录起填充（V72）。若通知目标关联了总代，则相同 MID 仅在该总代树内分流；若总代基准货币与报文货币不一致，「处理」列会按货币不一致（接收路径）隔离。<strong>接收性质</strong>仅在 NOTI 请求携带 <code>X-Icopay-Notify-Delivery: LIVE|RETRY</code> 或 <code>X-Noti-Attempt</code>（1=实时，2+=重试）头时区分，否则显示「未显示」。修正绑定或映射后要把历史记录挂回支付明细，请在正文弹窗中使用<strong>重放到支付明细</strong>（原文被截断的记录不可用）。',
      TH: 'บันทึกคำขอที่ middleware แจ้งเตือนและ PSP (เช่น ChillPay) ส่งมายัง URL รับแจ้งเตือนของระบบนี้ (<code>/api/open/pg-notify/…</code>) คอลัมน์ช่องทางแสดงเส้นทางรับ รหัสเป้าหมายและช่องทางจะถูกเติมตั้งแต่แถวที่รับใหม่ (V72) หากเชื่อมตัวแทนหลักกับเป้าแจ้งเตือน MID เดียวกันจะถูกจัดเส้นทางเฉพาะในต้นไม้ตัวแทนหลักนั้น หากสกุลเงินฐานของตัวแทนหลักต่างจากสกุลในเนื้อหา คอลัมน์การประมวลผลจะแยกเป็นไม่ตรงกันของสกุลเงิน (เส้นทางรับ) <strong>ลักษณะการรับ</strong> แยกได้เฉพาะเมื่อ NOTI ส่งหัว <code>X-Icopay-Notify-Delivery: LIVE|RETRY</code> หรือ <code>X-Noti-Attempt</code> (1=ไลฟ์, 2+=ลองใหม่) มิฉะนั้นแสดง「ไม่แสดง」หลังแก้ binding/mapping หากต้องการผูกแถวเก่ากับประวัติการชำระ ให้ใช้<strong>เล่นกลับไปยังรายการชำระ</strong>ในโมดัลเนื้อหา (ไม่รองรับหากเนื้อหาต้นฉบับถูกตัด)'
    },
    '수신일(부터)': {
      EN: 'Received from',
      JP: '受信日（から）',
      CH: '接收日期（起）',
      TH: 'วันที่รับ (ตั้งแต่)'
    },
    '수신일(까지)': {
      EN: 'Received to',
      JP: '受信日（まで）',
      CH: '接收日期（止）',
      TH: 'วันที่รับ (ถึง)'
    },
    '검색 항목': {
      EN: 'Search field',
      JP: '検索項目',
      CH: '搜索项',
      TH: 'ฟิลด์ค้นหา'
    },
    '검색어': {
      EN: 'Search text',
      JP: '検索語',
      CH: '搜索词',
      TH: 'คำค้น'
    },
    '부분 일치': {
      EN: 'Partial match',
      JP: '部分一致',
      CH: '部分匹配',
      TH: 'ตรงบางส่วน'
    },
    'CALL (Callback URL)': {
      EN: 'CALL (Callback URL)',
      JP: 'CALL（コールバックURL）',
      CH: 'CALL（回调 URL）',
      TH: 'CALL (Callback URL)'
    },
    'RESULT (Result URL)': {
      EN: 'RESULT (Result URL)',
      JP: 'RESULT（結果URL）',
      CH: 'RESULT（结果 URL）',
      TH: 'RESULT (Result URL)'
    },
    'BOTH (전체)': {
      EN: 'BOTH (all)',
      JP: 'BOTH（両方）',
      CH: 'BOTH（全部）',
      TH: 'BOTH (ทั้งคู่)'
    },
    '[조회]를 누르세요.': {
      EN: 'Press [Search].',
      JP: '「検索」を押してください。',
      CH: '请点击［查询］。',
      TH: 'กด [ค้นหา]'
    },
    '노티 원문': {
      EN: 'Notify raw body',
      JP: 'ノティ原文',
      CH: '通知原文',
      TH: 'เนื้อหาแจ้งเตือนดิบ'
    },
    '결제내역 재반영': {
      EN: 'Replay to payment list',
      JP: '決済明細へ再反映',
      CH: '重放到支付明细',
      TH: 'เล่นกลับไปยังรายการชำระ'
    },
    '처리상태': {
      EN: 'Process status',
      JP: '処理状態',
      CH: '处理状态',
      TH: 'สถานะประมวลผล'
    },
    '승인번호': {
      EN: 'Approval / txn ID',
      JP: '承認番号',
      CH: '授权号',
      TH: 'หมายเลขอนุมัติ/รายการ'
    },
    '미표시': {
      EN: 'Not shown',
      JP: '非表示',
      CH: '未显示',
      TH: 'ไม่แสดง'
    },
    '조회된 노티가 없습니다.': {
      EN: 'No notify rows found.',
      JP: '該当するノティがありません。',
      CH: '未找到通知记录。',
      TH: 'ไม่พบแถวแจ้งเตือน'
    },
    '노티수령 표 결제': {
      EN: 'Payment',
      JP: '決済',
      CH: '支付',
      TH: 'การชำระ'
    },
    '노티수령 표 처리': {
      EN: 'Process',
      JP: '処理',
      CH: '处理',
      TH: 'ประมวลผล'
    },
    '본문': {
      EN: 'Body',
      JP: '本文',
      CH: '正文',
      TH: 'เนื้อหา'
    },
    '대상 ID가 없습니다.': {
      EN: 'No target ID.',
      JP: '対象IDがありません。',
      CH: '没有目标 ID。',
      TH: 'ไม่มีรหัสเป้าหมาย'
    },
    '저장된 노티 원문으로 가맹 분기·결제내역 적재를 다시 시도합니다. 계속할까요?': {
      EN: 'Retry merchant routing and payment ingestion using the stored notify body. Continue?',
      JP: '保存されたノティ原文で加盟店振り分け・決済明細の取り込みを再試行します。続行しますか？',
      CH: '使用已保存的通知原文重试商户分流与支付明细入库。是否继续？',
      TH: 'ลองแยกร้านและบันทึกประวัติชำระอีกครั้งจากเนื้อแจ้งเตือนที่เก็บไว้ ดำเนินต่อหรือไม่'
    },
    '노티수령 재처리 결과 처리상태': {
      EN: 'Process status',
      JP: '処理状態',
      CH: '处理状态',
      TH: 'สถานะประมวลผล'
    },
    '노티수령 재처리 결과 가맹점코드': {
      EN: 'Merchant code',
      JP: '加盟店コード',
      CH: '商户代码',
      TH: 'รหัสร้านค้า'
    },
    '노티수령 재처리 결과 오류안내': {
      EN: 'Error / notice',
      JP: 'エラー・案内',
      CH: '错误/提示',
      TH: 'ข้อผิดพลาด/แจ้งเตือน'
    },
    '노티수령 재처리 결과 적재경고': {
      EN: 'Ingest warning',
      JP: '取り込み警告',
      CH: '入库警告',
      TH: 'คำเตือนการบันทึก'
    },
    'dispatch 예외': {
      EN: 'Dispatch error',
      JP: 'ディスパッチ例外',
      CH: '分发异常',
      TH: 'ข้อผิดพลาด dispatch'
    },
    '재처리가 완료되었습니다.': {
      EN: 'Replay finished.',
      JP: '再処理が完了しました。',
      CH: '重处理已完成。',
      TH: 'เล่นกลับเสร็จแล้ว'
    },
    '재처리 실패': {
      EN: 'Replay failed',
      JP: '再処理に失敗しました',
      CH: '重处理失败',
      TH: 'เล่นกลับล้มเหลว'
    },
    '노티수령 메타 수신': {
      EN: 'Received',
      JP: '受信',
      CH: '接收',
      TH: 'รับเมื่อ'
    },
    '노티수령 메타 결제': {
      EN: 'Payment',
      JP: '決済',
      CH: '支付',
      TH: 'ชำระ'
    },
    '노티수령 메타 처리': {
      EN: 'Process',
      JP: '処理',
      CH: '处理',
      TH: 'ประมวลผล'
    },
    '노티수령 메타 처리코드': {
      EN: 'Process code',
      JP: '処理コード',
      CH: '处理代码',
      TH: 'รหัสประมวลผล'
    },
    '노티수령 메타 수신성격': {
      EN: 'Ingress kind',
      JP: '受信性質',
      CH: '接收性质',
      TH: 'ลักษณะการรับ'
    },
    '대상코드': {
      EN: 'Target code',
      JP: '対象コード',
      CH: '目标代码',
      TH: 'รหัสเป้าหมาย'
    },
    '보기': {
      EN: 'View',
      JP: '表示',
      CH: '查看',
      TH: 'ดู'
    },
    '수신시각': {
      EN: 'Received at',
      JP: '受信日時',
      CH: '接收时间',
      TH: 'เวลาที่รับ'
    },
    '오류메시지': {
      EN: 'Error message',
      JP: 'エラーメッセージ',
      CH: '错误信息',
      TH: 'ข้อความข้อผิดพลาด'
    },
    '본문 미리보기': {
      EN: 'Body preview',
      JP: '本文プレビュー',
      CH: '正文预览',
      TH: 'ตัวอย่างเนื้อหา'
    },
    '결제·처리': {
      EN: 'Payment / process',
      JP: '決済・処理',
      CH: '支付/处理',
      TH: 'ชำระ/ประมวลผล'
    },
    '수신성격': {
      EN: 'Ingress delivery',
      JP: '受信性質',
      CH: '接收性质',
      TH: 'ลักษณะการรับ'
    },
  /* API 한글 라벨 — /hq/notifyInbound 목록·상세 (HqNotifyInboundQueryService) */
    매핑완료: {
      EN: 'Mapping complete',
      JP: 'マッピング完了',
      CH: '映射完成',
      TH: 'แมปเสร็จแล้ว'
    },
    가맹점미매핑: {
      EN: 'Merchant not mapped',
      JP: '加盟店未マッピング',
      CH: '商户未映射',
      TH: 'ร้านยังไม่แมป'
    },
    '업체차단(프로필N·구버전)': {
      EN: 'Company blocked (profile N / legacy)',
      JP: '取引先ブロック（プロファイルN・旧版）',
      CH: '企业已屏蔽（档案 N·旧版）',
      TH: 'บริษัทถูกบล็อก (โปรไฟล์ N / เวอร์ชันเก่า)'
    },
    'URL결제·업체코드필요': {
      EN: 'URL pay · company code required',
      JP: 'URL決済・取引先コード必須',
      CH: 'URL 支付·需企业代码',
      TH: 'ชำระ URL · ต้องมีรหัสบริษัท'
    },
    업체미확인: {
      EN: 'Company unresolved',
      JP: '取引先未確認',
      CH: '企业未确认',
      TH: 'ยังไม่ยืนยันบริษัท'
    },
    바인딩없음: {
      EN: 'No binding',
      JP: 'バインディングなし',
      CH: '无绑定',
      TH: 'ไม่มี binding'
    },
    MID불일치: {
      EN: 'MID mismatch',
      JP: 'MID不一致',
      CH: 'MID 不一致',
      TH: 'MID ไม่ตรง'
    },
    URL결제PG아님: {
      EN: 'Not URL-pay PG',
      JP: 'URL決済PGではない',
      CH: '非 URL 支付 PG',
      TH: 'ไม่ใช่ PG ชำระ URL'
    },
    '통화불일치(수신경로)': {
      EN: 'Currency mismatch (ingress)',
      JP: '通貨不一致（受信経路）',
      CH: '货币不一致（接收路径）',
      TH: 'สกุลเงินไม่ตรง (เส้นทางรับ)'
    },
    수신경로업체불일치: {
      EN: 'Ingress company mismatch',
      JP: '受信経路の取引先不一致',
      CH: '接收路径企业不一致',
      TH: 'บริษัทไม่ตรงเส้นทางรับ'
    },
    라이브: {
      EN: 'Live',
      JP: 'ライブ',
      CH: '实时',
      TH: 'ไลฟ์'
    },
    재전송: {
      EN: 'Retry',
      JP: '再送',
      CH: '重试',
      TH: 'ส่งซ้ำ'
    },
    성공: {
      EN: 'Success',
      JP: '成功',
      CH: '成功',
      TH: 'สำเร็จ'
    },
    실패: {
      EN: 'Failed',
      JP: '失敗',
      CH: '失败',
      TH: 'ล้มเหลว'
    },
    취소: {
      EN: 'Cancelled',
      JP: 'キャンセル',
      CH: '取消',
      TH: 'ยกเลิก'
    },
    오류: {
      EN: 'Error',
      JP: 'エラー',
      CH: '错误',
      TH: 'ข้อผิดพลาด'
    },
    요청: {
      EN: 'Requested',
      JP: 'リクエスト',
      CH: '请求',
      TH: 'ร้องขอ'
    },
    무효: {
      EN: 'Void',
      JP: '無効',
      CH: '无效',
      TH: 'void'
    },
    이메일무효: {
      KO: '이메일 무효',
      EN: 'Email void',
      JP: 'メール無効',
      CH: '邮件无效',
      TH: 'void อีเมล'
    },
    '이메일 무효': {
      EN: 'Email void',
      JP: 'メール無効',
      CH: '邮件无效',
      TH: 'void อีเมล'
    },
    환불: {
      EN: 'Refund',
      JP: '返金',
      CH: '退款',
      TH: 'คืนเงิน'
    },
    강제환불: {
      EN: 'Forced refund',
      JP: '強制返金',
      CH: '强制退款',
      TH: 'คืนเงินบังคับ'
    },
    /* HQ /hq/notifyMapping — GUI·알림 (screens L + app.js pgAdminUiT) */
    '노티매핑설정 (GUI)': {
      EN: 'Notify mapping (GUI)',
      JP: 'ノティマッピング設定 (GUI)',
      CH: '通知映射设置 (GUI)',
      TH: 'ตั้งค่าแมปปิงแจ้งเตือน (GUI)'
    },
    '노티매핑설정': {
      EN: 'Notify mapping',
      JP: 'ノティマッピング設定',
      CH: '通知映射设置',
      TH: 'ตั้งค่าแมปปิงแจ้งเตือน'
    },
    '노티매핑설정 안내': {
      EN: 'Overview: pick a PSP, then in CALLBACK/RESULT map PG parameter names to our column keys (e.g. customId → internal column). If inbound history exists, keys attach automatically; CHILL-like bundles are enriched. Saving syncs with pay list and org column settings. If API fails (CSP connect-src), use <html data-pg-api-base="same-origin"> for /api on the same host or allow https://api.icopay.co.kr in CSP. See docs/노티매핑설정.md for DB/Bearer.',
      JP: '概要: 決済代行(PSP)を選び、CALLBACK/RESULTでPGパラメータ名を当社列キーに対応付けます(例: customId→内部列)。受信履歴があればキーが自動付与され、CHILL系は一般パラメータ群も補完されます。保存で決済一覧・組織列設定と同期します。API接続エラー(CSP・connect-src)時は<html data-pg-api-base="same-origin">で同一ホストの/apiを使うか、静的ホストのCSPにhttps://api.icopay.co.krを許可してください。DB・Bearerはdocs/노티매핑설정.mdを参照。',
      CH: '概览：选择支付服务商后，在 CALLBACK/RESULT 将 PG 参数名映射到内部列键（如 customId→内部列）。有接收记录时会自动附带键，CHILL 系会补全常用参数。保存后与支付列表及组织列设置同步。若 API 失败（CSP connect-src），请用 <html data-pg-api-base="same-origin"> 走同域 /api，或在静态站点 CSP 放行 https://api.icopay.co.kr。DB/Bearer 见 docs/노티매핑설정.md。',
      TH: 'สรุป: เลือก PSP แล้วใน CALLBACK/RESULT แมปชื่อพารามิเตอร์ PG ไปคีย์คอลัมน์ภายใน (เช่น customId→คอลัมน์ภายใน) หากมีประวัติรับเข้า จะผูกคีย์อัตโนมัติ และสาย CHILL จะเติมชุดพารามิเตอร์ทั่วไป การบันทึกจะซิงก์กับรายการชำระและคอลัมน์องค์กร หาก API ล้ม (CSP connect-src) ให้ใช้ <html data-pg-api-base="same-origin"> เพื่อ /api โฮสต์เดียวกัน หรืออนุญาต https://api.icopay.co.kr ใน CSP ดู docs/노티매핑설정.md สำหรับ DB/Bearer'
    },
    '최종 수정일시': {
      EN: 'Last updated at',
      JP: '最終更新日時',
      CH: '最后修改时间',
      TH: 'อัปเดตล่าสุด'
    },
    '전문가용: JSON 직접 편집': {
      EN: 'Expert: edit JSON directly',
      JP: '上級者向け: JSONを直接編集',
      CH: '专家：直接编辑 JSON',
      TH: 'ผู้เชี่ยวชาญ: แก้ JSON โดยตรง'
    },
    '전문가용: JSON 편집 닫기': {
      EN: 'Expert: close JSON editor',
      JP: '上級者向け: JSON編集を閉じる',
      CH: '专家：关闭 JSON 编辑',
      TH: 'ผู้เชี่ยวชาญ: ปิดตัวแก้ JSON'
    },
    '매핑 정의 JSON (필드명)': {
      EN: 'Mapping definition JSON (field name)',
      JP: 'マッピング定義JSON（フィールド名）',
      CH: '映射定义 JSON（字段名）',
      TH: 'JSON คำจำกัดความแมป (ชื่อฟิลด์)'
    },
    '저장된 매핑 JSON을 파싱할 수 없어 빈 설정으로 표시합니다. [전문가용: JSON 직접 편집]에서 고치거나 [기본 카탈로그·화면연결 삽입]을 사용하세요.': {
      EN: 'Could not parse saved mapping JSON; showing empty settings. Fix in [Expert: edit JSON directly] or use [Insert default catalog & page links].',
      JP: '保存されたマッピングJSONを解析できないため空の設定を表示します。[上級者向け: JSONを直接編集]で修正するか、[既定カタログ・画面紐付けを挿入]を使用してください。',
      CH: '无法解析已保存的映射 JSON，显示为空。请在[专家：直接编辑 JSON]中修复，或使用[插入默认目录与页面关联]。',
      TH: 'แยกวิเคราะห์ JSON แมปที่บันทึกไว้ไม่ได้ จึงแสดงค่าว่าง แก้ใน[ผู้เชี่ยวชาญ: แก้ JSON โดยตรง] หรือใช้[แทรกแคตตาล็อกค่าเริ่มและเชื่อมหน้าจอ]'
    },
    'CALLBACK (서버 노티)': {
      EN: 'CALLBACK (server notify)',
      JP: 'CALLBACK（サーバ通知）',
      CH: 'CALLBACK（服务器通知）',
      TH: 'CALLBACK (แจ้งเตือนฝั่งเซิร์ฟเวอร์)'
    },
    'RESULT (브라우저 리다이렉트)': {
      EN: 'RESULT (browser redirect)',
      JP: 'RESULT（ブラウザリダイレクト）',
      CH: 'RESULT（浏览器重定向）',
      TH: 'RESULT (รีไดเร็กต์เบราว์เซอร์)'
    },
    'RETURN (동기 응답·return_url)': {
      EN: 'RETURN (sync response / return_url)',
      JP: 'RETURN（同期応答・return_url）',
      CH: 'RETURN（同步响应 / return_url）',
      TH: 'RETURN (ตอบกลับแบบซิงก์ / return_url)'
    },
    '통합 결제내역': {
      EN: 'Integrated pay list',
      JP: '統合決済一覧',
      CH: '整合支付列表',
      TH: 'รายการชำระแบบรวม'
    },
    '결제(리다이렉트) 화면': {
      EN: 'Pay (redirect) page',
      JP: '決済（リダイレクト）画面',
      CH: '支付（重定向）页面',
      TH: 'หน้าชำระ (รีไดเร็กต์)'
    },
    '— 열 선택 —': {
      EN: '— Pick column —',
      JP: '— 列を選択 —',
      CH: '— 选择列 —',
      TH: '— เลือกคอลัมน์ —'
    },
    ' (카탈로그外)': {
      EN: ' (outside catalog)',
      JP: '（カタログ外）',
      CH: '（目录外）',
      TH: ' (นอกแคตตาล็อก)'
    },
    '— 결제대행사 선택 —': {
      EN: '— Select PSP —',
      JP: '— 決済代行を選択 —',
      CH: '— 请选择支付服务商 —',
      TH: '— เลือก PSP —'
    },
    'PG 필드명': {
      EN: 'PG field name',
      JP: 'PGフィールド名',
      CH: 'PG 字段名',
      TH: 'ชื่อฟิลด์ PG'
    },
    '카탈로그 기본': {
      EN: 'Catalog default',
      JP: 'カタログ既定',
      CH: '目录默认',
      TH: 'ค่าเริ่มแคตตาล็อก'
    },
    'AI·자동 제안 시 이 행 유지': {
      EN: 'Keep this row on AI/auto suggest',
      JP: 'AI・自動提案時にこの行を維持',
      CH: 'AI/自动建议时保留此行',
      TH: 'คงแถวนี้เมื่อ AI/คำแนะแบบอัตโนมัติ'
    },
    '등록된 필드 매핑이 없습니다. 위 매핑 작업 표에서 추가하거나 PG 목록 동기화 후 설정하세요.': {
      EN: 'No field mappings yet. Add them in the mapping table above, or sync the PG list first.',
      JP: '登録されたフィールドマッピングがありません。上のマッピング作業表で追加するか、PG一覧同期後に設定してください。',
      CH: '尚无字段映射。请在上方映射工作表中添加，或先同步 PG 列表后再配置。',
      TH: 'ยังไม่มีแมปปิงฟิลด์ เพิ่มในตารางด้านบน หรือซิงก์รายการ PG ก่อน'
    },
    '노티내역': {
      EN: 'Notify list',
      JP: 'ノティ一覧',
      CH: '通知列表',
      TH: 'รายการแจ้งเตือน'
    },
    'AI 분석 가능': {
      EN: 'AI analysis available',
      JP: 'AI分析可能',
      CH: '可使用 AI 分析',
      TH: 'วิเคราะห์ด้วย AI ได้'
    },
    'AI 미설정(규칙만)': {
      EN: 'AI not configured (rules only)',
      JP: 'AI未設定（ルールのみ）',
      CH: '未配置 AI（仅规则）',
      TH: 'ไม่ได้ตั้งค่า AI (กฎอย่างเดียว)'
    },
    'AI 상태 확인 실패': {
      EN: 'Could not check AI status',
      JP: 'AI状態の確認に失敗',
      CH: '无法检查 AI 状态',
      TH: 'ตรวจสถานะ AI ไม่สำเร็จ'
    },
    'PG 파라미터': {
      EN: 'PG parameter',
      JP: 'PGパラメータ',
      CH: 'PG 参数',
      TH: 'พารามิเตอร์ PG'
    },
    '우리 항목 (열 key)': {
      EN: 'Our item (column key)',
      JP: '当社項目（列key）',
      CH: '我方项（列 key）',
      TH: 'รายการของเรา (คีย์คอลัมน์)'
    },
    'AI잠금': {
      EN: 'AI lock',
      JP: 'AIロック',
      CH: 'AI 锁定',
      TH: 'ล็อก AI'
    },
    '매핑 행이 없습니다. [행 추가] 또는 CALLBACK 샘플로 자동 제안': {
      EN: 'No mapping rows. [Add row] or auto-suggest from a CALLBACK sample.',
      JP: 'マッピング行がありません。[行追加]またはCALLBACKサンプルで自動提案してください。',
      CH: '没有映射行。请[添加行]或使用 CALLBACK 样例自动建议。',
      TH: 'ไม่มีแถวแมป ใช้[เพิ่มแถว]หรือตัวอย่าง CALLBACK เพื่อคำแนะอัตโนมัติ'
    },
    '샘플 CALLBACK JSON (자동 제안)': {
      EN: 'Sample CALLBACK JSON (auto-suggest)',
      JP: 'CALLBACK JSONサンプル（自動提案）',
      CH: 'CALLBACK 样例 JSON（自动建议）',
      TH: 'ตัวอย่าง JSON CALLBACK (คำแนะอัตโนมัติ)'
    },
    '파라미터 자동 매핑 제안': {
      EN: 'Suggest auto field mapping',
      JP: 'パラメータ自動マッピングを提案',
      CH: '建议自动参数映射',
      TH: 'เสนอแมปปิงพารามิเตอร์อัตโนมัติ'
    },
    '④ 결제대행사 추가설정 (표시값)': {
      EN: '④ PSP extra settings (display values)',
      JP: '④ 決済代行の追加設定（表示値）',
      CH: '④ 支付服务商附加设置（显示值）',
      TH: '④ ตั้งค่าเพิ่ม PSP (ค่าที่แสดง)'
    },
    '노티매핑 displayMaps 안내': {
      EN: 'Per column key, JSON like { "raw": "display" }. currency and chillPaymentStatus prefer PG displayMaps; if a key is missing, server defaults apply (currency short codes; status localized).',
      JP: '列キーごとに { "原文": "表示文字" } 形式のJSON。currency・chillPaymentStatusはPGのdisplayMapsを優先し、キーが無い場合はサーバ既定（通貨短縮表記・状態の多言語化）が適用されます。',
      CH: '按列键使用 { "原文": "显示文本" } 形式 JSON。currency、chillPaymentStatus 优先 PG displayMaps；缺键时用服务端默认（货币简写、状态本地化）。',
      TH: 'ต่อคีย์คอลัมน์ ใช้ JSON แบบ { "ดิบ": "ข้อความแสดง" } currency และ chillPaymentStatus ให้ใช้ displayMaps ของ PG ก่อน หากไม่มีคีย์ จะใช้ค่าเริ่มของเซิร์ฟเวอร์'
    },
    '등록된 PG가 없습니다. PG 목록 동기화를 실행하세요.': {
      EN: 'No PSPs registered. Run PG list sync.',
      JP: '登録されたPGがありません。PG一覧同期を実行してください。',
      CH: '尚未登记 PG。请执行 PG 列表同步。',
      TH: 'ยังไม่มี PG ที่ลงทะเบียน ให้รันซิงก์รายการ PG'
    },
    '매핑 작업 표': {
      EN: 'Mapping work table',
      JP: 'マッピング作業表',
      CH: '映射工作表',
      TH: 'ตารางงานแมปปิง'
    },
    'PG 수신 파라미터 → 우리 항목(열)': {
      EN: 'PG inbound params → our columns',
      JP: 'PG受信パラメータ→当社項目（列）',
      CH: 'PG 入参 → 我方列',
      TH: 'พารามิเตอร์ที่ PG ส่งมา → คอลัมน์ของเรา'
    },
    '결제대행사 선택': {
      EN: 'Select PSP',
      JP: '決済代行の選択',
      CH: '选择支付服务商',
      TH: 'เลือก PSP'
    },
    '선택 시 실제 수신 노티에서 본 파라미터 이름을 자동으로 표에 합칩니다': {
      EN: 'When checked, parameter names seen in real inbound notifies are merged into the table automatically.',
      JP: 'オンにすると、実際の受信ノティで観測したパラメータ名を表に自動で取り込みます。',
      CH: '勾选后，会将实际接收通知中出现的参数名自动合并到表格。',
      TH: 'เมื่อเลือก จะรวมชื่อพารามิเตอร์จากแจ้งเตือนที่รับจริงลงในตารางอัตโนมัติ'
    },
    '노티 채널': {
      EN: 'Notify channel',
      JP: 'ノティチャネル',
      CH: '通知渠道',
      TH: 'ช่องทางแจ้งเตือน'
    },
    '표시명이 반영될 카탈로그': {
      EN: 'Catalog to apply display names',
      JP: '表示名を反映するカタログ',
      CH: '要应用显示名的目录',
      TH: 'แคตตาล็อกที่จะใช้ชื่อที่แสดง'
    },
    '노티매핑 카탈로그 표시명 안내': {
      EN: 'Saving/applies “Our display name” updates this catalog’s column labels, same as org column settings and pay grids.',
      JP: '「当社表示名」を保存・適用すると、このカタログの列名が変わり、組織項目設定・決済グリッドと同じになります。',
      CH: '保存/应用「我方显示名」会更新此目录的列名，与组织列设置及支付网格一致。',
      TH: 'เมื่อบันทึก/ใช้「ชื่อที่แสดงของเรา」 จะอัปเดตชื่อคอลัมน์ในแคตตาล็อกนี้ให้ตรงกับการตั้งค่าคอลัมน์องค์กรและกริดชำระ'
    },
    '저장분 불러오기': {
      EN: 'Load saved mapping',
      JP: '保存分を読み込む',
      CH: '加载已保存',
      TH: 'โหลดที่บันทึกไว้'
    },
    '표 내용 → 매핑 반영': {
      EN: 'Apply table → mapping',
      JP: '表の内容→マッピングに反映',
      CH: '将表格内容应用到映射',
      TH: 'นำตารางไปใช้กับแมป'
    },
    '수신 노티에서 파라미터 다시 불러오기': {
      EN: 'Reload parameters from inbound notifies',
      JP: '受信ノティからパラメータを再取得',
      CH: '从接收通知重新加载参数',
      TH: 'โหลดพารามิเตอร์จากแจ้งเตือนที่รับอีกครั้ง'
    },
    'AI·자동 제안': {
      EN: 'AI / auto suggest',
      JP: 'AI・自動提案',
      CH: 'AI / 自动建议',
      TH: 'AI / คำแนะอัตโนมัติ'
    },
    '규칙만 제안': {
      EN: 'Rules-only suggest',
      JP: 'ルールのみ提案',
      CH: '仅规则建议',
      TH: 'เสนอเฉพาะกฎ'
    },
    '행 추가': {
      EN: 'Add row',
      JP: '行を追加',
      CH: '添加行',
      TH: 'เพิ่มแถว'
    },
    '샘플 JSON으로 키 추출·제안 (선택)': {
      EN: 'Extract keys / suggest from sample JSON (optional)',
      JP: 'サンプルJSONでキー抽出・提案（任意）',
      CH: '从样例 JSON 提取键/建议（可选）',
      TH: 'ดึงคีย์/เสนอจาก JSON ตัวอย่าง (ไม่บังคับ)'
    },
    '샘플 노티 JSON': {
      EN: 'Sample notify JSON',
      JP: 'サンプルノティJSON',
      CH: '样例通知 JSON',
      TH: 'JSON แจ้งเตือนตัวอย่าง'
    },
    '노티 본문 예시를 붙여 넣으면 키 목록·제안에 사용합니다': {
      EN: 'Paste a sample notify body; keys and suggestions use it.',
      JP: 'ノティ本文の例を貼り付けると、キー一覧・提案に使われます。',
      CH: '粘贴通知正文示例，用于键列表和建议。',
      TH: 'วางตัวอย่างเนื้อหาแจ้งเตือน ใช้สำหรับรายการคีย์และคำแนะ'
    },
    'JSON에서 키 만든 뒤 자동 매핑( AI 가능하면 우선 )': {
      EN: 'After extracting keys, auto-map (prefer AI when available)',
      JP: 'キー作成後に自動マッピング（AI可なら優先）',
      CH: '提取键后自动映射（若可用则优先 AI）',
      TH: 'หลังดึงคีย์ แมปอัตโนมัติ (ถ้ามี AI ให้ใช้ก่อน)'
    },
    'JSON에서 파라미터 키 목록': {
      EN: 'Parameter keys from JSON',
      JP: 'JSONからパラメータキー一覧',
      CH: '从 JSON 取参数键列表',
      TH: 'รายการคีย์พารามิเตอร์จาก JSON'
    },
    'CHILLPAY 일반 파라미터 넣기': {
      EN: 'Insert CHILLPAY common parameters',
      JP: 'CHILLPAY一般パラメータを入れる',
      CH: '插入 CHILLPAY 常用参数',
      TH: 'ใส่พารามิเตอร์ทั่วไป CHILLPAY'
    },
    'PG에서 온 파라미터 이름': {
      EN: 'Parameter name from PG',
      JP: 'PGから来たパラメータ名',
      CH: '来自 PG 的参数名',
      TH: 'ชื่อพารามิเตอร์จาก PG'
    },
    '우리 표시명': {
      EN: 'Our display name',
      JP: '当社表示名',
      CH: '我方显示名',
      TH: 'ชื่อที่แสดงของเรา'
    },
    '노티매핑 표 하단 안내': {
      EN: 'After Apply table → mapping, the advanced PG section stays in sync. Press Save at the bottom to persist.',
      JP: '「表の内容→マッピング反映」後は下の詳細PG領域と同期します。サーバに残すには画面下の保存を押してください。',
      CH: '将表格应用到映射后，与下方高级 PG 区域同步。要保存到服务器请点击页面底部保存。',
      TH: 'หลังนำตารางไปแมป จะซิงก์กับรายละเอียด PG ด้านล่าง กดบันทึกด้านล่างเพื่อเก็บที่เซิร์ฟเวอร์'
    },
    '서버에 저장된 이 PG·채널 매핑을 표에 불러옵니다': {
      EN: 'Load this PSP/channel mapping saved on the server into the table.',
      JP: 'サーバに保存されたこのPG・チャネルのマッピングを表に読み込みます。',
      CH: '将服务器上保存的此 PG/渠道映射加载到表格。',
      TH: 'โหลดแมป PG/ช่องทางที่บันทึกบนเซิร์ฟเวอร์ลงในตาราง'
    },
    'AI 가능 시 우선': {
      EN: 'Prefer AI when available',
      JP: 'AIが利用可能なら優先',
      CH: '可用时优先 AI',
      TH: 'ถ้ามี AI ให้ใช้ก่อน'
    },
    '자동·AI 제안 시 이 줄 유지': {
      EN: 'Keep this row on auto/AI suggest',
      JP: '自動・AI提案時にこの行を維持',
      CH: '自动/AI 建议时保持此行',
      TH: 'คงแถวนี้เมื่อคำแนะอัตโนมัติ/AI'
    },
    '등록된 매핑 전체 목록': {
      EN: 'All registered mappings',
      JP: '登録済みマッピング一覧',
      CH: '已登记映射总览',
      TH: 'รายการแมปที่ลงทะเบียนทั้งหมด'
    },
    '노티매핑 요약 표 안내': {
      EN: 'Stacked by PSP and channel. To edit, pick the PSP in the table above and use Load saved mapping.',
      JP: 'PG・チャネル別の積み上げです。編集は上の表でPGを選び、[保存分を読み込む]を使ってください。',
      CH: '按 PSP 与渠道汇总。编辑请在上方表选择 PSP 后使用「加载已保存」。',
      TH: 'เรียงตาม PSP และช่องทาง แก้ไขโดยเลือก PSP ในตารางด้านบนแล้วกดโหลดที่บันทึก'
    },
    'PG코드': {
      EN: 'PSP code',
      JP: 'PGコード',
      CH: 'PG 代码',
      TH: 'รหัส PG'
    },
    '채널': {
      EN: 'Channel',
      JP: 'チャネル',
      CH: '渠道',
      TH: 'ช่องทาง'
    },
    '우리 열(key)': {
      EN: 'Our column (key)',
      JP: '当社列（key）',
      CH: '我方列（key）',
      TH: 'คอลัมน์ของเรา (key)'
    },
    '작업': {
      EN: 'Actions',
      JP: '操作',
      CH: '操作',
      TH: 'การทำงาน'
    },
    '비고': {
      EN: 'Note',
      JP: '備考',
      CH: '备注',
      TH: 'หมายเหตุ'
    },
    'GUI로 설정하는 순서': {
      EN: 'Setup order (GUI)',
      JP: 'GUIでの設定手順',
      CH: 'GUI 设置顺序',
      TH: 'ลำดับการตั้งค่า (GUI)'
    },
    '노티매핑 단계1': {
      EN: 'PG list sync — rows appear from Deployment > API integration.',
      JP: 'PG一覧同期 — デプロイ設定>API連動設定に登録された決済代行の行ができます。',
      CH: 'PG 列表同步 — 来自部署 > API 联动配置中登记的支付服务商行。',
      TH: 'ซิงก์รายการ PG — สร้างแถวจากการตั้งค่า API ในการปรับใช้'
    },
    '노티매핑 단계2': {
      EN: 'In the mapping table, pick PSP & channel. Optionally merge parameter names from real inbound notifies.',
      JP: 'マッピング作業表でPG・チャネルを選択。（任意）実際の受信ノティで見たパラメータ名を自動で表に付けます。',
      CH: '在映射工作表选择 PSP 与渠道。（可选）将实际接收通知中的参数名自动并入表格。',
      TH: 'ในตารางแมป เลือก PSP และช่องทาง (ถ้าต้องการ) รวมชื่อพารามิเตอร์จากแจ้งเตือนที่รับจริง'
    },
    '노티매핑 단계3': {
      EN: 'Per row pick Our column (key); optionally edit Our display name. Key-only fills catalog default. AI lock blocks auto-suggest from changing that row.',
      JP: '各行で当社項目（列key）を選び、必要なら当社表示名を修正。keyのみならカタログ既定で自動入力。AIロックは自動提案でその行を変えられなくします。',
      CH: '每行选择我方列键；可选修改显示名。仅选键则用目录默认填充。AI 锁定可防止自动建议改该行。',
      TH: 'แต่ละแถวเลือกคีย์คอลัมน์ แก้ชื่อที่แสดงได้ ถ้ามีแค่คีย์จะเติมจากค่าเริ่มแคตตาล็อก ล็อก AI กันไม่ให้คำแนะเปลี่ยนแถว'
    },
    '노티매핑 단계4': {
      EN: 'Apply table → mapping, then Save at the bottom — display names update the catalog and match org/pay grids.',
      JP: '「表の内容→マッピング反映」後、画面下の「保存」—表示名はカタログに反映され、組織項目・決済グリッドと一致します。',
      CH: '「应用到映射」后点底部「保存」—显示名写入目录，与组织/支付网格一致。',
      TH: 'หลังนำตารางไปใช้กับแมป กดบันทึกด้านล่าง — ชื่อที่แสดงอัปเดตแคตตาล็อกให้ตรงกับองค์กร/กริดชำระ'
    },
    '노티매핑 단계5': {
      EN: 'Changing channel loads that channel’s saved mapping into the table. Apply mapping before switching if you edited only in the table.',
      JP: 'チャネルを変えると、そのチャネルに保存されたマッピングを表に読み込みます。表のみ編集した場合は切り替え前に「表の内容→マッピング反映」を推奨します。',
      CH: '切换渠道会加载该渠道已保存映射到表格。若仅在表中编辑，切换前建议先「应用到映射」。',
      TH: 'เปลี่ยนช่องทางจะโหลดแมปที่บันทึกของช่องนั้น หากแก้เฉพาะในตาราง ควรกดนำไปใช้กับแมปก่อนสลับ'
    },
    '고급: 카탈로그·화면별 연결 (JSON 편집)': {
      EN: 'Advanced: catalog & per-screen links (JSON edit)',
      JP: '上級: カタログ・画面別紐付け（JSON編集）',
      CH: '高级：目录与各页面关联（JSON 编辑）',
      TH: 'ขั้นสูง: แคตตาล็อกและเชื่อมหน้าจอ (แก้ JSON)'
    },
    '고급: PG별 채널 표 · 표시값(displayMaps) · 전체 매핑 목록': {
      EN: 'Advanced: per-PSP channel table, displayMaps, full mapping list',
      JP: '上級: PG別チャネル表・表示値(displayMaps)・全マッピング一覧',
      CH: '高级：按 PG 的渠道表、displayMaps、完整映射列表',
      TH: 'ขั้นสูง: ตารางช่องทางต่อ PG, displayMaps, รายการแมปทั้งหมด'
    },
    '노티매핑 고급 PG 영역 안내': {
      EN: 'Same data as the mapping table above; review here or edit rarely.',
      JP: '上のマッピング作業表と同じデータです。ここで確認するか、稀に直接修正できます。',
      CH: '与上方映射工作表相同的数据；在此核对或极少直接修改。',
      TH: 'ข้อมูลเดียวกับตารางแมปด้านบน ตรวจที่นี่หรือแก้โดยตรงเป็นครั้งคราว'
    },
    '컬럼 카탈로그 (열 정의 · JSON)': {
      EN: 'Column catalog (column defs · JSON)',
      JP: 'カラムカタログ（列定義・JSON）',
      CH: '列目录（列定义 · JSON）',
      TH: 'แคตตาล็อกคอลัมน์ (นิยามคอลัมน์ · JSON)'
    },
    '노티매핑 카탈로그 JSON 안내': {
      EN: 'Usually insert defaults only. Edit JSON only when you must change column structure.',
      JP: '通常は既定挿入のみで十分です。列構造を直接変える場合のみJSONを編集してください。',
      CH: '通常只需插入默认。仅在必须改列结构时编辑 JSON。',
      TH: 'โดยทั่วไปแทรกค่าเริ่มพอ แก้ JSON เมื่อจำเป็นต้องเปลี่ยนโครงคอลัมน์'
    },
    '표시 제목': {
      EN: 'Display title',
      JP: '表示タイトル',
      CH: '显示标题',
      TH: 'ชื่อที่แสดง'
    },
    '카탈로그 없음. [기본 삽입] 또는 [추가]': {
      EN: 'No catalog. [Insert defaults] or [Add].',
      JP: 'カタログがありません。[既定挿入]または[追加]を使用してください。',
      CH: '无目录。请[插入默认]或[添加]。',
      TH: 'ไม่มีแคตตาล็อก ใช้[แทรกค่าเริ่ม]หรือ[เพิ่ม]'
    },
    '결제관리 화면별 카탈로그 연결': {
      EN: 'Per pay-admin screen → catalog link',
      JP: '決済管理画面別のカタログ紐付け',
      CH: '支付管理各画面与目录关联',
      TH: 'เชื่อมแคตตาล็อกต่อหน้าจัดการชำระ'
    },
    '메뉴': {
      EN: 'Menu',
      JP: 'メニュー',
      CH: '菜单',
      TH: 'เมนู'
    },
    '카탈로그': {
      EN: 'Catalog',
      JP: 'カタログ',
      CH: '目录',
      TH: 'แคตตาล็อก'
    },
    'PG 목록 동기화': {
      EN: 'Sync PG list',
      JP: 'PG一覧を同期',
      CH: '同步 PG 列表',
      TH: 'ซิงก์รายการ PG'
    },
    '기본 카탈로그·화면연결 삽입': {
      EN: 'Insert default catalog & screen links',
      JP: '既定カタログ・画面紐付けを挿入',
      CH: '插入默认目录与页面关联',
      TH: 'แทรกแคตตาล็อกค่าเริ่มและเชื่อมหน้าจอ'
    },
    '카탈로그 추가': {
      EN: 'Add catalog',
      JP: 'カタログを追加',
      CH: '添加目录',
      TH: 'เพิ่มแคตตาล็อก'
    },
    '새 카탈로그': {
      EN: 'New catalog',
      JP: '新規カタログ',
      CH: '新目录',
      TH: 'แคตตาล็อกใหม่'
    },
    '노티 매핑 설정을 불러오지 못했습니다.': {
      EN: 'Could not load notify mapping settings.',
      JP: 'ノティマッピング設定を読み込めませんでした。',
      CH: '无法加载通知映射设置。',
      TH: 'โหลดการตั้งค่าแมปแจ้งเตือนไม่สำเร็จ'
    },
    '노티 매핑 로드 실패 안내': {
      EN: ' Check login, deployment, CSP connect-src, or <html data-pg-api-base="same-origin"> (admin and API on same host). The editor opens empty.',
      JP: ' ログイン・デプロイ・(CSP時)connect-src、または<html data-pg-api-base="same-origin">(管理画面とAPI同一ホスト)を確認してください。編集画面は空で開きます。',
      CH: ' 请检查登录、部署、CSP connect-src，或<html data-pg-api-base="same-origin">（管理与 API 同主机）。编辑器以空状态打开。',
      TH: ' ตรวจล็อกอิน การปรับใช้ connect-src ของ CSP หรือ<html data-pg-api-base="same-origin"> (แอดมินกับ API โฮสต์เดียวกัน) ตัวแก้ไขเปิดแบบว่าง'
    },
    '표 입력값을 확인하세요. 카탈로그/채널 JSON 형식 오류일 수 있습니다.': {
      EN: 'Check form values; catalog/channel JSON may be invalid.',
      JP: '表の入力を確認してください。カタログ/チャネルJSON形式エラーの可能性があります。',
      CH: '请检查表格输入；目录/渠道 JSON 格式可能有误。',
      TH: 'ตรวจค่าในตาราง; JSON แคตตาล็อก/ช่องทางอาจผิดรูปแบบ'
    },
    'JSON 형식이 올바르지 않습니다. 중괄호·쉼표·따옴표를 확인하세요.': {
      EN: 'JSON is invalid. Check braces, commas, and quotes.',
      JP: 'JSON形式が正しくありません。波括弧・カンマ・引用符を確認してください。',
      CH: 'JSON 格式不正确。请检查大括号、逗号和引号。',
      TH: 'รูปแบบ JSON ไม่ถูกต้อง ตรวจวงเล็บปีกกา จุลภาค และเครื่องหมายคำพูด'
    },
    '저장되었습니다. 카탈로그에 넣은 「우리 표시명」은 결제내역 계열 그리드와 조직항목설정(VIEW)에서 보이는 열 이름과 동일하게 적용됩니다.': {
      EN: 'Saved. “Our display names” in the catalog match column titles in pay-list grids and org column settings (VIEW).',
      JP: '保存しました。カタログの「当社表示名」は決済一覧系グリッドと組織項目設定(VIEW)の列名と同じように適用されます。',
      CH: '已保存。目录中的「我方显示名」与支付列表系网格及组织列设置（VIEW）中的列标题一致。',
      TH: 'บันทึกแล้ว ชื่อคอลัมน์「ชื่อที่แสดงของเรา」ในแคตตาล็อกตรงกับกริดรายการชำระและการตั้งค่าคอลัมน์องค์กร (VIEW)'
    },
    '결제대행사를 선택하세요. 없으면 [PG 목록 동기화]를 실행합니다.': {
      EN: 'Select a PSP. If none, run PG list sync.',
      JP: '決済代行を選択してください。無ければ[PG一覧同期]を実行してください。',
      CH: '请选择支付服务商。若没有请先执行「同步 PG 列表」。',
      TH: 'เลือก PSP หากไม่มีให้รันซิงก์รายการ PG'
    },
    '결제대행사를 선택하세요.': {
      EN: 'Select a PSP.',
      JP: '決済代行を選択してください。',
      CH: '请选择支付服务商。',
      TH: 'เลือก PSP'
    },
    '매핑 행이 없습니다. 이 채널의 매핑을 모두 비울까요?': {
      EN: 'No mapping rows. Clear all mappings for this channel?',
      JP: 'マッピング行がありません。このチャネルのマッピングをすべて空にしますか？',
      CH: '没有映射行。要清空此渠道的全部映射吗？',
      TH: 'ไม่มีแถวแมป จะล้างแมปของช่องทางนี้ทั้งหมดหรือไม่'
    },
    '매핑이 반영되었습니다. 서버에 남기려면 화면 하단 [저장]을 누르세요.': {
      EN: 'Mapping applied. Press Save at the bottom to persist to the server.',
      JP: 'マッピングを反映しました。サーバに残すには画面下の[保存]を押してください。',
      CH: '映射已应用。要保存到服务器请点击页面底部「保存」。',
      TH: 'นำแมปไปใช้แล้ว กดบันทึกด้านล่างเพื่อเก็บที่เซิร์ฟเวอร์'
    },
    '샘플 JSON을 입력하세요.': {
      EN: 'Enter sample JSON.',
      JP: 'サンプルJSONを入力してください。',
      CH: '请输入样例 JSON。',
      TH: 'กรอก JSON ตัวอย่าง'
    },
    '제안된 매핑이 없습니다.': {
      EN: 'No suggested mappings.',
      JP: '提案されたマッピングがありません。',
      CH: '没有建议的映射。',
      TH: 'ไม่มีแมปที่เสนอ'
    },
    '제안된 매핑이 없습니다. (출처: {SRC})': {
      EN: 'No suggested mappings. (Source: {SRC})',
      JP: '提案されたマッピングがありません。（出典: {SRC}）',
      CH: '没有建议的映射。（来源：{SRC}）',
      TH: 'ไม่มีแมปที่เสนอ (ที่มา: {SRC})'
    },
    '매핑 작업 표를 제안 결과 {N}건으로 채울까요? (AI잠금 행은 유지·병합됩니다)': {
      EN: 'Fill the mapping table with {N} suggested row(s)? (AI-locked rows are kept/merged.)',
      JP: 'マッピング作業表を提案結果{N}件で埋めますか？（AIロック行は維持・マージされます）',
      CH: '用 {N} 条建议结果填充映射工作表？（AI 锁定行会保留/合并。）',
      TH: 'เติมตารางแมปด้วยผลเสนอ {N} แถว? (แถวล็อก AI จะคง/รวม)'
    },
    '매핑 작업 표를 제안 결과 {N}건으로 채울까요? (AI잠금 행은 유지·병합됩니다) 출처: {SRC}.': {
      EN: 'Fill the mapping table with {N} suggested row(s)? (AI-locked rows are kept/merged.) Source: {SRC}.',
      JP: 'マッピング作業表を提案結果{N}件で埋めますか？（AIロック行は維持・マージされます）出典: {SRC}。',
      CH: '用 {N} 条建议填充映射工作表？（AI 锁定行保留/合并。）来源：{SRC}。',
      TH: 'เติมตารางแมป {N} แถวจากคำแนะ? (ล็อก AI คง/รวม) ที่มา: {SRC}.'
    },
    '자동 제안 API 호출에 실패했습니다.': {
      EN: 'Auto-suggest API call failed.',
      JP: '自動提案APIの呼び出しに失敗しました。',
      CH: '自动建议 API 调用失败。',
      TH: 'เรียก API คำแนะอัตโนมัติล้มเหลว'
    },
    '수집된 키가 없습니다. 노티 적재 이력이 없거나 MID 필터에 맞는 건이 없을 수 있습니다.': {
      EN: 'No keys collected. No notify ingest history, or no rows match the MID filter.',
      JP: '収集されたキーがありません。ノティ取込履歴がないか、MIDフィルタに合う件がありません。',
      CH: '未收集到键。可能没有通知入库记录，或无符合 MID 筛选的数据。',
      TH: 'ไม่มีคีย์ที่รวบรวม ไม่มีประวัติบันทึกแจ้งเตือน หรือไม่มีแถวตรงกับตัวกรอง MID'
    },
    '수집된 키가 없습니다. 노티 적재 이력이 없거나 MID 필터에 맞는 건이 없을 수 있습니다. ({SRC})': {
      EN: 'No keys collected. No notify ingest history, or no rows match the MID filter. ({SRC})',
      JP: '収集されたキーがありません。ノティ取込履歴がないか、MIDフィルタに合う件がありません。（{SRC}）',
      CH: '未收集到键。可能没有通知入库记录，或无符合 MID 筛选的数据。（{SRC}）',
      TH: 'ไม่มีคีย์ ({SRC})'
    },
    '파라미터 키 {N}개를 표에 추가했습니다. (스캔 노티 {R}건, {SRC})': {
      EN: 'Added {N} parameter key(s) to the table. (Notifies scanned: {R}, {SRC})',
      JP: 'パラメータキー{N}件を表に追加しました。（スキャンしたノティ{R}件、{SRC}）',
      CH: '已向表格添加 {N} 个参数键。（扫描通知 {R} 条，{SRC}）',
      TH: 'เพิ่ม {N} คีย์ลงในตาราง (สแกน {R} แถว, {SRC})'
    },
    '수신 노티 키 API 호출에 실패했습니다.': {
      EN: 'Inbound notify keys API call failed.',
      JP: '受信ノティキーAPIの呼び出しに失敗しました。',
      CH: '接收通知键 API 调用失败。',
      TH: 'เรียก API คีย์แจ้งเตือนที่รับล้มเหลว'
    },
    'JSON에서 키를 읽을 수 없습니다.': {
      EN: 'Could not read keys from JSON.',
      JP: 'JSONからキーを読み取れません。',
      CH: '无法从 JSON 读取键。',
      TH: 'อ่านคีย์จาก JSON ไม่ได้'
    },
    '키 목록을 반영했습니다. 표를 확인한 뒤 「표 내용 → 매핑 반영」과 「저장」을 하세요.': {
      EN: 'Key list applied. Review the table, then use Apply table → mapping and Save.',
      JP: 'キー一覧を反映しました。表を確認し、「表の内容→マッピング反映」と「保存」を行ってください。',
      CH: '已应用键列表。请检查表格后执行「将表格应用到映射」和「保存」。',
      TH: 'นำรายการคีย์แล้ว ตรวจตาราง แล้วใช้นำตารางไปแมปและบันทึก'
    },
    '키 목록을 반영했습니다. 자동 매핑 출처: {SRC}. 표를 확인한 뒤 「표 내용 → 매핑 반영」과 「저장」을 하세요.': {
      EN: 'Key list applied. Auto-map source: {SRC}. Review the table, then Apply table → mapping and Save.',
      JP: 'キー一覧を反映しました。自動マッピング出典: {SRC}。表を確認し、「表の内容→マッピング反映」と「保存」を行ってください。',
      CH: '已应用键列表。自动映射来源：{SRC}。请检查表格后执行「将表格应用到映射」和「保存」。',
      TH: 'นำคีย์แล้ว ที่มาแมปอัตโนมัติ: {SRC} ตรวจตารางแล้วนำไปแมปและบันทึก'
    },
    '자동 매핑 API 호출에 실패했습니다. 키 목록만 반영했습니다.': {
      EN: 'Auto-map API failed; only the key list was applied.',
      JP: '自動マッピングAPIに失敗しました。キー一覧のみ反映しました。',
      CH: '自动映射 API 失败；仅应用了键列表。',
      TH: 'API แมปอัตโนมัติล้มเหลว ใช้เฉพาะรายการคีย์'
    },
    '결제대행사를 먼저 선택하세요.': {
      EN: 'Select a PSP first.',
      JP: '先に決済代行を選択してください。',
      CH: '请先选择支付服务商。',
      TH: 'เลือก PSP ก่อน'
    },
    'CALLBACK 샘플 JSON을 붙여 넣은 뒤 다시 시도하세요.': {
      EN: 'Paste a CALLBACK sample JSON and try again.',
      JP: 'CALLBACKサンプルJSONを貼り付けてから再試行してください。',
      CH: '请粘贴 CALLBACK 样例 JSON 后重试。',
      TH: 'วาง JSON ตัวอย่าง CALLBACK แล้วลองอีกครั้ง'
    },
    '제안된 매핑이 없습니다. JSON 키·카탈로그 열을 확인하세요.': {
      EN: 'No suggested mappings. Check JSON keys and catalog columns.',
      JP: '提案されたマッピングがありません。JSONキー・カタログ列を確認してください。',
      CH: '没有建议映射。请检查 JSON 键与目录列。',
      TH: 'ไม่มีแมปที่เสนอ ตรวจคีย์ JSON และคอลัมน์แคตตาล็อก'
    },
    '이 CALLBACK 채널의 매핑을 제안 결과 {N}건으로 덮어씁니다. 계속할까요?': {
      EN: 'Overwrite this CALLBACK channel mapping with {N} suggested row(s)? Continue?',
      JP: 'このCALLBACKチャネルのマッピングを提案結果{N}件で上書きします。続行しますか？',
      CH: '用 {N} 条建议覆盖此 CALLBACK 渠道映射？继续？',
      TH: 'เขียนทับแมป CALLBACK ด้วย {N} แถวจากคำแนะ? ดำเนินต่อ?'
    },
    '기본 columnCatalogs·pageCatalogAssignments 를 덮어씁니다. vendors 는 유지됩니다. 계속할까요?': {
      EN: 'Overwrite default columnCatalogs & pageCatalogAssignments. Vendors are kept. Continue?',
      JP: '既定のcolumnCatalogs・pageCatalogAssignmentsを上書きします。vendorsは維持されます。続行しますか？',
      CH: '将覆盖默认 columnCatalogs 与 pageCatalogAssignments。保留 vendors。继续？',
      TH: 'เขียนทับ columnCatalogs และ pageCatalogAssignments ค่าเริ่ม คง vendors ต่อ?'
    },
    '기본값을 불러오지 못했습니다.': {
      EN: 'Could not load defaults.',
      JP: '既定値を読み込めませんでした。',
      CH: '无法加载默认值。',
      TH: 'โหลดค่าเริ่มไม่สำเร็จ'
    },
    'PG 목록을 불러오지 못했습니다.': {
      EN: 'Could not load PG list.',
      JP: 'PG一覧を読み込めませんでした。',
      CH: '无法加载 PG 列表。',
      TH: 'โหลดรายการ PG ไม่สำเร็จ'
    },
    '카탈로그 {ID}의 headerGroups JSON이 올바르지 않습니다.': {
      EN: 'Catalog {ID}: headerGroups JSON is invalid.',
      JP: 'カタログ{ID}: headerGroups JSONが正しくありません。',
      CH: '目录 {ID}：headerGroups JSON 不正确。',
      TH: 'แคตตาล็อก {ID}: JSON headerGroups ไม่ถูกต้อง'
    },
    '카탈로그 {ID}의 columns JSON이 올바르지 않습니다.': {
      EN: 'Catalog {ID}: columns JSON is invalid.',
      JP: 'カタログ{ID}: columns JSONが正しくありません。',
      CH: '目录 {ID}：columns JSON 不正确。',
      TH: 'แคตตาล็อก {ID}: JSON columns ไม่ถูกต้อง'
    },
    'PG {CODE}의 displayMaps JSON이 올바르지 않습니다.': {
      EN: 'PSP {CODE}: displayMaps JSON is invalid.',
      JP: 'PG{CODE}: displayMaps JSONが正しくありません。',
      CH: 'PG {CODE}：displayMaps JSON 不正确。',
      TH: 'PG {CODE}: JSON displayMaps ไม่ถูกต้อง'
    },
    'AI·자동 제안 시 유지': {
      EN: 'Keep on AI/auto suggest',
      JP: 'AI・自動提案時に維持',
      CH: 'AI/自动建议时保持',
      TH: 'คงเมื่อ AI/คำแนะอัตโนมัติ'
    },
    '예: TransactionId': {
      EN: 'e.g. TransactionId',
      JP: '例: TransactionId',
      CH: '例：TransactionId',
      TH: 'เช่น TransactionId'
    },
    'headerGroups JSON': { EN: 'headerGroups JSON', JP: 'headerGroups JSON', CH: 'headerGroups JSON', TH: 'headerGroups JSON' },
    'columns JSON': { EN: 'columns JSON', JP: 'columns JSON', CH: 'columns JSON', TH: 'columns JSON' },
    '① 노티': { EN: '① Notify', JP: '① ノティ', CH: '① 通知', TH: '① แจ้งเตือน' },
    '② URL': { EN: '② URL', JP: '② URL', CH: '② URL', TH: '② URL' },
    '③ 챗봇': { EN: '③ Chatbot', JP: '③ チャットボット', CH: '③ 聊天机器人', TH: '③ แชทบอท' },
    '④ API': { EN: '④ API', JP: '④ API', CH: '④ API', TH: '④ API' },
    '⑤ API구독': { EN: '⑤ API subscription', JP: '⑤ API定期', CH: '⑤ API 订阅', TH: '⑤ สมัคร API' },
    'API구독': { EN: 'API subscription', JP: 'API定期', CH: 'API 订阅', TH: 'สมัคร API' },
    '한 행당 용도 1개. 다른 용도는 PG코드를 달리 해 추가 등록하세요.': {
      EN: 'One scope per row. For another scope, register again with a different PG code.',
      JP: '1行につき用途は1つ。別用途はPGコードを変えて追加登録してください。',
      CH: '每行一个用途。其他用途请使用不同的 PG 代码另行注册。',
      TH: 'หนึ่งแถวหนึ่งประเภท ประเภทอื่นลงทะเบียนใหม่ด้วยรหัส PG ต่างกัน'
    },
    '선택한 용도에 맞는 https://…': {
      EN: 'https://… matching the selected scope',
      JP: '選択した用途に合う https://…',
      CH: '与所选用途匹配的 https://…',
      TH: 'https://… ให้ตรงกับประเภทที่เลือก'
    },
    '일반(일반형)': {
      EN: 'Standard (normal)',
      JP: '一般（一般型）',
      CH: '标准（普通型）',
      TH: 'มาตรฐาน (แบบทั่วไป)'
    },
    'DP (DISPLAY)': { EN: 'DP (DISPLAY)', JP: 'DP (DISPLAY)', CH: 'DP (DISPLAY)', TH: 'DP (DISPLAY)' },
    'BLIND': { EN: 'BLIND', JP: 'BLIND', CH: 'BLIND', TH: 'BLIND' },
    '기존 api_endpoint': {
      EN: 'Legacy api_endpoint',
      JP: '既存 api_endpoint',
      CH: '原 api_endpoint',
      TH: 'api_endpoint เดิม'
    },
    '본사 URL결제설정(FX)과 동일 저장소': {
      EN: 'Same store as HQ URL pay settings (FX)',
      JP: '本社URL決済設定(FX)と同一の保存先',
      CH: '与总部 URL 支付设置(FX)同一存储',
      TH: 'ที่เก็บเดียวกับตั้งค่าชำระ URL (FX) ของ HQ'
    },
    '예 M035594': { EN: 'e.g. M035594', JP: '例 M035594', CH: '例 M035594', TH: 'เช่น M035594' },
    'Environment: Sandbox=테스트, Production=LIVE(본상품)': {
      EN: 'Environment: Sandbox=test, Production=LIVE',
      JP: '環境: Sandbox=テスト、Production=LIVE（本番）',
      CH: '环境：Sandbox=测试，Production=正式',
      TH: 'สภาพแวดล้อม: Sandbox=ทดสอบ Production=LIVE'
    },
    '신규만 입력·수정 시 변경 불가. 용도마다 PG코드를 나누어 등록하면 가맹점 결제대행사 선택 시 구분됩니다. ChillPay 계열은 CHILLPAY로 시작하는 코드가 URL·자격 병합에 함께 쓰입니다.': {
      EN: 'New rows only; cannot change after save. Split PG codes per scope so merchants can tell them apart. ChillPay family: codes starting with CHILLPAY merge URL and credentials.',
      JP: '新規のみ入力・修正時は変更不可。用途ごとにPGコードを分けると加盟店の決済代行選択で区別できます。ChillPay系はCHILLPAYで始まるコードがURL・資格マージに使われます。',
      CH: '仅新建可填，保存后不可改。按用途拆分 PG 代码以便商户区分。ChillPay 系列以 CHILLPAY 开头的代码会合并 URL 与凭据。',
      TH: 'กรอกเฉพาะแถวใหม่ แก้แล้วเปลี่ยนไม่ได้ แยกรหัส PG ตามประเภทเพื่อให้ร้านเลือกได้ ChillPay ใช้รหัสขึ้นต้น CHILLPAY รวม URL และข้อมูลยืนยัน'
    },
    '표시통화→실결제 FX는 「DP」또는 UI만 숨기는 「BLIND」이며, 본사 URL결제설정에서 FX·통화를 맞춥니다. 「일반」은 가맹점 청구통화 그대로 결제합니다.': {
      EN: 'Display→settlement FX uses DP or BLIND (UI-only hide); align FX/currency in HQ URL pay settings. Standard charges in the merchant billing currency as-is.',
      JP: '表示→実決済FXは「DP」またはUIのみ隠す「BLIND」。本社URL決済設定でFX・通貨を合わせます。「一般」は加盟店請求通貨のまま決済します。',
      CH: '展示→实结 FX 为 DP 或 BLIND（仅隐藏 UI）；在总部 URL 支付设置中对齐 FX 与货币。「一般」按商户账单货币原样支付。',
      TH: 'แสดง→ชำระจริง FX ใช้ DP หรือ BLIND (ซ่อน UI) ตั้ง FX/สกุลในตั้งค่า URL ของ HQ แบบ「ทั่วไป」ชำระตามสกุลเรียกเก็บของร้าน'
    },
    'ChillPay 등: CHILLPAY로 시작하는 사용(Y) 행들의 엔드포인트·이 필드가 URL 설정에 병합됩니다.': {
      EN: 'ChillPay: endpoints from active (Y) rows whose code starts with CHILLPAY, plus this field, merge into URL settings.',
      JP: 'ChillPay等: CHILLPAYで始まる使用(Y)行のエンドポイントと本フィールドがURL設定にマージされます。',
      CH: 'ChillPay 等：以 CHILLPAY 开头的启用(Y) 行的端点与本字段会并入 URL 设置。',
      TH: 'ChillPay ฯลฯ: endpoint ของแถวที่ใช้ (Y) ขึ้นต้น CHILLPAY และช่องนี้ รวมในตั้งค่า URL'
    },
    '아래는 PG사별 연동 자격입니다. ChillPay는 MID·API Key·MD5·Route·Environment (Sandbox / Production)을 입력하세요. 수정 시 API Key·MD5는 바꿀 때만 입력(비우면 기존 유지).': {
      EN: 'Below are per-PG credentials. For ChillPay enter MID, API Key, MD5, Route, Environment (Sandbox / Production). When editing, fill API Key / MD5 only to rotate; leave blank to keep existing.',
      JP: '以下はPG別の連携資格です。ChillPayはMID・API Key・MD5・Route・Environmentを入力。修正時はAPI Key・MD5は変更時のみ入力（空なら維持）。',
      CH: '以下为各 PG 的对接凭据。ChillPay 请填写 MID、API Key、MD5、Route、Environment。编辑时仅在轮换时填写 API Key/MD5；留空则保留原值。',
      TH: 'ด้านล่างคือข้อมูลยืนยันต่อ PG สำหรับ ChillPay กรอก MID API Key MD5 Route Environment แก้ไขกรอก API Key/MD5 เมื่อต้องการเปลี่ยนเท่านั้น เว้นว่างคงค่าเดิม'
    },
    '해당 PG 전용 확장 파라미터(선택).': {
      EN: 'Optional extra parameters for this PG.',
      JP: '当該PG専用の拡張パラメータ（任意）。',
      CH: '该 PG 的可选扩展参数。',
      TH: 'พารามิเตอร์เสริมเฉพาะ PG นี้ (ไม่บังคับ)'
    },
    'OFF=미사용, T+N=영업일·결제와 동일 시각, D+N=달력+N일·일괄 시각': {
      EN: 'OFF=off; T+N=business days, same time as payment; D+N=calendar days + batch time',
      JP: 'OFF=未使用、T+N=営業日・決済と同一時刻、D+N=暦日+N・一括時刻',
      CH: 'OFF=关闭；T+N=营业日、与支付同时刻；D+N=自然日+N、批量时刻',
      TH: 'OFF=ปิด T+N=วันทำการ เวลาเดียวกับชำระ D+N=ปฏิทิน+N เวลารวม'
    },
    'N(1~10)': { EN: 'N (1–10)', JP: 'N(1～10)', CH: 'N（1~10）', TH: 'N (1–10)' },
    '이 행은 선택한 용도 한 가지만 담당합니다': {
      EN: 'This row handles only the selected scope.',
      JP: 'この行は選択した用途の1つのみを担当します。',
      CH: '本行仅负责所选的一种用途。',
      TH: 'แถวนี้รับผิดชอบเพียงประเภทที่เลือกหนึ่งประเภท'
    },
    'OFF': { EN: 'OFF', JP: 'OFF', CH: 'OFF', TH: 'OFF' },
    'T': { EN: 'T', JP: 'T', CH: 'T', TH: 'T' },
    'D': { EN: 'D', JP: 'D', CH: 'D', TH: 'D' },
    /* /hq/apiConfig — API配信設定 */
    API배포설정: {
      EN: 'API deployment',
      JP: 'API配信設定',
      CH: 'API 部署',
      TH: 'การตั้งค่า API'
    },
    '가맹점에 발급하는 통합 API의 기본 URL·인증·타임아웃입니다. PG사별 MID·API Key·시크릿은 배포설정 > 「API연동설정」에서 PG코드 단위로 추가·저장하세요(여 PG 병행).': {
      EN: 'Base URL, authentication, and timeout for the unified API issued to merchants. Per-PG MID, API key, and secrets are added and saved per PG code under Deployment > “API integration” (multiple PGs supported).',
      JP: '加盟店向け統合APIのベースURL・認証・タイムアウトです。PG別のMID・APIキー・シークレットはデプロイ設定＞「API連携設定」でPGコード単位に追加・保存してください（複数PG併用可）。',
      CH: '面向商户的统一 API 的基础 URL、认证与超时。各 PG 的 MID、API 密钥请在「部署设置 > API 联动设置」中按 PG 代码添加并保存（可多 PG 并行）。',
      TH: 'URL พื้นฐาน การยืนยันตัวตน และ timeout ของ API รวมสำหรับร้าน MID/คีย์/ความลับต่อ PG ให้เพิ่มที่การตั้งค่าเชื่อม API ตามรหัส PG (รองรับหลาย PG)'
    },
    'API 기본 URL': {
      EN: 'API base URL',
      JP: 'APIベースURL',
      CH: 'API 基础 URL',
      TH: 'URL ฐาน API'
    },
    인증방식: {
      EN: 'Authentication',
      JP: '認証方式',
      CH: '认证方式',
      TH: 'การยืนยันตัวตน'
    },
    '타임아웃(초)': {
      EN: 'Timeout (sec)',
      JP: 'タイムアウト(秒)',
      CH: '超时（秒）',
      TH: 'หมดเวลา (วินาที)'
    },
    비고: {
      EN: 'Notes',
      JP: '備考',
      CH: '备注',
      TH: 'หมายเหตุ'
    },
    'PG 자격 증명 (등록 위치)': {
      EN: 'PG credentials (where to register)',
      JP: 'PG資格情報（登録場所）',
      CH: 'PG 凭据（登记位置）',
      TH: 'ข้อมูลรับรอง PG (ตำแหน่งที่ลงทะเบียน)'
    },
    '[PG사 연동 추가]로 PG코드·표시명을 만든 뒤, 동일 화면에서 MID·API Key·MD5(또는 서명키)·Route·Environment (Sandbox/Production)을 입력합니다. ChillPay 결제는 PG코드 CHILLPAY 행에 값이 있면 그것을 최우선으로 사용하고, 비어 있을 때만 아래 레거시 필드를 사용합니다.': {
      EN: 'Use [Add PG linkage] to create the PG code and display name, then on the same screen enter MID, API Key, MD5 (or signing key), Route, and Environment (Sandbox/Production). For ChillPay, if the CHILLPAY row has values they take highest priority; the legacy fields below are used only when those are empty.',
      JP: '「PG連携追加」でPGコード・表示名を作成し、同一画面でMID・APIキー・MD5（または署名キー）・Route・Environment (Sandbox/Production)を入力します。ChillPay決済はPGコードCHILLPAY行に値があれば最優先で使用し、空のときのみ下のレガシー欄を使います。',
      CH: '通过「添加 PG 对接」创建 PG 代码与显示名，并在同一画面填写 MID、API Key、MD5（或签名密钥）、Route、Environment (Sandbox/Production)。ChillPay 若 CHILLPAY 行有值则优先使用，仅在为空时使用下方兼容字段。',
      TH: 'ใช้ [เพิ่มการเชื่อม PG] สร้างรหัส PG และชื่อที่แสดง แล้วกรอก MID, API Key, MD5, Route, Environment บนหน้าเดียวกัน ChillPay ถ้าแถว CHILLPAY มีค่าใช้ก่อน ช่องเลกาซีด้านล่างใช้เมื่อว่างเท่านั้น'
    },
    'API연동설정 화면 열기': {
      EN: 'Open API integration screen',
      JP: 'API連携設定画面を開く',
      CH: '打开 API 联动设置',
      TH: 'เปิดหน้าการเชื่อม API'
    },
    '목록에서 행을 더블클릭하면 자격 증명을 편집할 수 있습니다.': {
      EN: 'Double-click a row in the list to edit credentials.',
      JP: '一覧の行をダブルクリックすると資格情報を編集できます。',
      CH: '在列表中双击一行即可编辑凭据。',
      TH: 'ดับเบิลคลิกแถวในรายการเพื่อแก้ไขข้อมูลรับรอง'
    },
    'ChillPay 레거시 (tb_hq_api_config 호환)': {
      EN: 'ChillPay legacy (tb_hq_api_config compatible)',
      JP: 'ChillPayレガシー（tb_hq_api_config互換）',
      CH: 'ChillPay 兼容（tb_hq_api_config）',
      TH: 'ChillPay เลกาซี (tb_hq_api_config)'
    },
    '배포설정 > API연동설정에 CHILLPAY로 API Key·MD5가 등록되어 있으면 이 블록은 무시됩니다. 기존 DB만 쓰는 환경용입니다.': {
      EN: 'If API Key and MD5 for CHILLPAY are registered under Deployment > API integration, this block is ignored. For environments that use the legacy DB only.',
      JP: 'デプロイ設定＞API連携設定にCHILLPAYのAPIキー・MD5が登録されている場合、このブロックは無視されます。従来DBのみを使う環境向けです。',
      CH: '若在「部署设置 > API 联动设置」中已登记 CHILLPAY 的 API Key 与 MD5，则忽略本块。适用于仅使用旧库的环境。',
      TH: 'ถ้ามี API Key และ MD5 ของ CHILLPAY ในการตั้งค่าเชื่อม API บล็อกนี้จะถูกละเว้น สำหรับระบบที่ใช้ DB เดิมเท่านั้น'
    },
    'ChillPay에서 발급': {
      EN: 'Issued by ChillPay',
      JP: 'ChillPayで発行',
      CH: '由 ChillPay 签发',
      TH: 'ออกโดย ChillPay'
    },
    'CheckSum 생성용': {
      EN: 'For checksum generation',
      JP: 'チェックサム生成用',
      CH: '用于生成校验和',
      TH: 'สำหรับสร้าง checksum'
    },
    '정산/환수 정책': {
      EN: 'Settlement / recovery policy',
      JP: '精算／回収ポリシー',
      CH: '结算/回款政策',
      TH: 'นโยบายชำระ/กู้คืน'
    },
    '환수금 처리 시 수수료 포함 여부와 정산 VAT 부과 여부를 본사 정책으로 설정합니다.': {
      EN: 'Set whether recovery amounts include fees and whether settlement VAT applies, as HQ policy.',
      JP: '回収金処理時の手数料込み可否と、精算時のVAT課否を本社方針で設定します。',
      CH: '设置回款处理是否含手续费，以及结算是否征收 VAT（总部策略）。',
      TH: 'กำหนดว่าการกู้คืนรวมค่าธรรมเนียมหรือไม่ และ VAT การชำระ — นโยบาย HQ'
    },
    '환수금 수수료 포함': {
      EN: 'Include fees in recovery amount',
      JP: '回収金に手数料を含める',
      CH: '回款金额含手续费',
      TH: 'รวมค่าธรรมเนียมในการกู้คืน'
    },
    '정산 VAT 부과': {
      EN: 'Apply settlement VAT',
      JP: '精算VATの課税',
      CH: '结算征收 VAT',
      TH: 'เก็บ VAT การชำระ'
    },
    포함: {
      EN: 'Include',
      JP: '含む',
      CH: '包含',
      TH: 'รวม'
    },
    제외: {
      EN: 'Exclude',
      JP: '除く',
      CH: '不含',
      TH: 'ไม่รวม'
    },
    부과: {
      EN: 'Apply',
      JP: '課す',
      CH: '征收',
      TH: 'เรียกเก็บ'
    },
    미부과: {
      EN: 'Do not apply',
      JP: '課さない',
      CH: '不征收',
      TH: 'ไม่เรียกเก็บ'
    },
    'Merchant Code': {
      EN: 'Merchant Code',
      JP: 'マーチャントコード',
      CH: '商户代码',
      TH: 'รหัสร้านค้า'
    },
    'MD5 Secret Key': {
      EN: 'MD5 Secret Key',
      JP: 'MD5シークレットキー',
      CH: 'MD5 密钥',
      TH: 'MD5 Secret Key'
    },
    'Route No': {
      EN: 'Route No',
      JP: 'ルート番号',
      CH: '路由号',
      TH: 'Route No'
    },
    Environment: {
      EN: 'Environment',
      JP: '環境',
      CH: '环境',
      TH: 'สภาพแวดล้อม'
    },
    Sandbox: {
      EN: 'Sandbox',
      JP: 'Sandbox',
      CH: 'Sandbox',
      TH: 'Sandbox'
    },
    Production: {
      EN: 'Production',
      JP: 'Production',
      CH: 'Production',
      TH: 'Production'
    },
    'Bearer Token': {
      EN: 'Bearer Token',
      JP: 'Bearerトークン',
      CH: 'Bearer Token',
      TH: 'Bearer Token'
    },
    Basic: {
      EN: 'Basic',
      JP: 'Basic',
      CH: 'Basic',
      TH: 'Basic'
    },
    /* /hq/merchantApiGenerate — 안내·폼·그리드·알림 */
    '화면 안내': {
      EN: 'Screen guide',
      JP: '画面の案内',
      CH: '页面说明',
      TH: 'คำแนะนำหน้าจอ'
    },
    '이 화면은 뭘 하나요?': {
      EN: 'What does this screen do?',
      JP: 'この画面は何をする？',
      CH: '这个页面做什么？',
      TH: 'หน้านี้ใช้ทำอะไร'
    },
    '결제를 여기서 승인하는 곳이 아닙니다. 다른 서버(가맹·브로커)에 넣을 연동 설정 글자 묶음(JSON)을 받아 가거나, 그 서버들이 쓰는 비밀번호(브로커 시크릿)를 새로 뽑거나, 보안을 더 켜 두는 곳입니다.': {
      EN: 'This is not where payments are approved. Fetch the integration settings bundle (JSON) for merchant/broker servers, rotate the broker secret they use, or tighten security.',
      JP: 'ここで決済を承認する画面ではありません。加盟店・ブローカー向けの連携設定(JSON)を取得したり、ブローカーシークレットを再発行したり、セキュリティを強化するための画面です。',
      CH: '这里不是审批支付的地方。用于获取给商户/经纪服务器的对接配置 JSON、轮换其使用的 broker secret，或加强安全。',
      TH: 'ที่นี่ไม่ใช่จุดอนุมัติการชำระ ใช้ดึงชุดการตั้งค่าเชื่อม (JSON) สำหรับเซิร์ฟเวอร์ร้าน/โบรกเกอร์ หมุน broker secret หรือเพิ่มความปลอดภัย'
    },
    '많은 경우 이 순서만 하면 됩니다': {
      EN: 'In most cases, follow these steps',
      JP: '多くの場合はこの順番だけで大丈夫です',
      CH: '多数情况按此顺序即可',
      TH: 'ส่วนใหญ่ทำตามลำดับนี้'
    },
    '업체명에 가맹 이름 일부를 넣고 가맹점 검색을 누릅니다. (이미 업체코드를 알면 검색 없이 코드만 입력해도 됩니다.)': {
      EN: 'Enter part of the merchant name and click Merchant search. (If you already know the company code, you can type only the code without searching.)',
      JP: '加盟店名の一部を入力して「加盟店検索」を押します。（すでに加盟店コードが分かっている場合は検索せずコードだけでも可）',
      CH: '输入商户名称的一部分并点击「商户搜索」。（若已知商户代码，可直接只输入代码。）',
      TH: 'พิมพ์ชื่อร้านบางส่วนแล้วกดค้นหาร้าน (ถ้ารู้รหัสร้านแล้วพิมพ์แค่รหัสได้โดยไม่ค้นหา)'
    },
    '표에서 해당 가맹 줄의 선택을 누릅니다. 위쪽 업체코드 칸이 채워집니다.': {
      EN: 'Click Select on the merchant row. The company code field above is filled.',
      JP: '一覧の該当加盟店行で「選択」を押します。上の加盟店コード欄に入ります。',
      CH: '在表格中对应商户行点击「选择」。上方商户代码框会被填入。',
      TH: 'คลิกเลือกที่แถวร้าน ช่องรหัสร้านด้านบนจะถูกเติม'
    },
    'PG 범위에서 실제 붙일 결제사(PG) 하나를 고릅니다. 잘 모르겠으면 일단 전체 PG로 받아 보고, 담당자에게 물어도 됩니다.': {
      EN: 'Under PG scope, pick one PG to attach. If unsure, try All PGs first and ask your contact.',
      JP: '「PG範囲」で実際に接続するPGを1つ選びます。分からなければまず「全PG」で取得し、担当者に確認しても構いません。',
      CH: '在「PG 范围」中选择实际要对接的一个 PG。不清楚可先选「全部 PG」获取后再问负责人。',
      TH: 'ใน PG เลือกหนึ่งตัวที่จะเชื่อมจริง ไม่แน่ใจลองเลือกทั้งหมดก่อนแล้วถามผู้รับผิดชอบ'
    },
    '연동 키트 JSON을 누릅니다. 맨 아래 회색 큰 칸에 나오는 글 전체를 복사해, 서버·연동 담당자에게 보내거나 설정에 넣습니다.': {
      EN: 'Click Integration kit JSON. Copy everything shown in the large grey box at the bottom and send it to your server/integration team or paste it into config.',
      JP: '「連携キットJSON」を押します。一番下の大きなグレー欄に出た内容をすべてコピーし、サーバー・連携担当へ送るか設定に貼り付けます。',
      CH: '点击「对接套件 JSON」。复制底部灰色大框中的全部内容发给服务器/对接同事或写入配置。',
      TH: 'กดชุด JSON คัดลอกข้อความทั้งหมดในกล่องสีเทาด้านล่างส่งทีมเซิร์ฟเวอร์/การเชื่อม'
    },
    '아래 두 가지는 꼭 구분하세요': {
      EN: 'Please distinguish the two below',
      JP: '次の2つは必ず区別してください',
      CH: '请务必区分以下两项',
      TH: 'โปรดแยกความแตกต่างของสองอย่างนี้'
    },
    '연동 키트 JSON — 지금 서버에 저장된 연동 정보를 읽기만 합니다. 가맹이나 PG가 망가지지는 않습니다.': {
      EN: 'Integration kit JSON — only reads linkage data stored on the server now. It does not break merchants or PGs.',
      JP: '連携キットJSON — 今サーバーに保存されている連携情報を読むだけです。加盟店やPGを壊しません。',
      CH: '对接套件 JSON — 仅读取当前服务器上保存的对接信息，不会破坏商户或 PG。',
      TH: 'ชุด JSON — อ่านข้อมูลเชื่อมที่เก็บบนเซิร์ฟเวอร์เท่านั้น ไม่ทำลายร้านหรือ PG'
    },
    '브로커 시크릿 재발급 — 누르는 순간 예전 비밀번호는 쓸 수 없게 됩니다. 유출·도용이 의심될 때나, 담당자가 교체하라고 했을 때만 누르세요. 브로커 서버 설정도 같은 날 맞춰 바꿔야 결제가 끊기지 않습니다.': {
      EN: 'Broker secret re-issue — the old password stops working immediately. Use only if you suspect leakage or your contact told you to rotate. Update broker server settings the same day or payments will break.',
      JP: 'ブローカーシークレット再発行 — 押した瞬間に旧パスワードは使えなくなります。漏えい・不正利用が疑われる時や担当者の指示がある時だけ。ブローカーサーバー側も同日に合わせて変更しないと決済が途切れます。',
      CH: 'Broker 密钥重发 — 点击后旧密码立即失效。仅在怀疑泄露或负责人要求时操作；broker 服务器须同日更新配置以免中断支付。',
      TH: 'ออก secret ใหม่ — รหัสเก่าใช้ไม่ได้ทันที ใช้เมื่อสงสัยรั่วหรือตามคำสั่ง ต้องปรับเซิร์ฟเวอร์โบรกเกอร์ในวันเดียวกัน'
    },
    '입력 칸은 이렇게 읽으면 됩니다': {
      EN: 'How to read the input fields',
      JP: '入力欄の読み方',
      CH: '输入框说明',
      TH: 'วิธีอ่านช่องป้อนข้อมูล'
    },
    '업체코드 — 그 가맹을 시스템에서 부른 번호(M000… 같은 것). 목록 없이 직접 쳐도 됩니다.': {
      EN: 'Company code — the system ID for that merchant (e.g. M000…). You may type it directly without the list.',
      JP: '加盟店コード — その加盟店をシステムで呼ぶ番号（M000…など）。一覧なしで直接入力しても構いません。',
      CH: '商户代码 — 系统中该商户的编号（如 M000…）。可不通过列表直接输入。',
      TH: 'รหัสร้าน — รหัสระบบของร้าน (เช่น M000…) พิมพ์ตรงได้โดยไม่ต้องใช้รายการ'
    },
    '업체명 — 사람이 부르는 상호 일부. 검색용입니다.': {
      EN: 'Company name — part of the trade name people use. For search only.',
      JP: '加盟店名 — 人が呼ぶ商号の一部。検索用です。',
      CH: '商户名称 — 日常称呼中的商号片段，仅用于搜索。',
      TH: 'ชื่อร้าน — ส่วนชื่อที่เรียกใช้ สำหรับค้นหาเท่านั้น'
    },
    'PG 범위 — 방금 말한 JSON·시크릿·강제 저장이 어느 PG 줄에 적용될지 고르는 것입니다.': {
      EN: 'PG scope — chooses which PG row the JSON, secret, and enforce save apply to.',
      JP: 'PG範囲 — 先ほどのJSON・シークレット・強制保存がどのPG行に適用されるかを選びます。',
      CH: 'PG 范围 — 选择上述 JSON、密钥与强制保存作用于哪一行 PG。',
      TH: 'ขอบเขต PG — เลือกว่า JSON/secret/บังคับบันทึกใช้กับแถว PG ใด'
    },
    '강제(시크릿 헤더 필수)란?': {
      EN: 'What is “enforce (secret header required)”?',
      JP: '「強制（シークレットヘッダ必須）」とは？',
      CH: '什么是「强制（必须带密钥头）」？',
      TH: 'บังคับ (ต้องมี secret header) คืออะไร'
    },
    '체크하면 외부에서 우리 미들웨어를 부를 때 비밀번호 헤더를 반드시 붙이게 합니다. 보안을 올리는 설정이라, 개발·연동 담당과 말 맞춘 뒤 강제여부 저장을 누르는 것이 안전합니다.': {
      EN: 'When checked, callers must include the password header when hitting our middleware. This raises security; align with dev/integration contacts before clicking Save enforce flag.',
      JP: 'チェックすると、外部からミドルウェアを呼ぶときに必ずパスワードヘッダが必要になります。セキュリティを上げる設定なので、開発・連携担当と合意してから「強制フラグ保存」が安全です。',
      CH: '勾选后，外部调用我们的中间件时必须带密码头。会提高安全性，请先与开发/对接负责人确认后再点保存强制标志。',
      TH: 'เมื่อติ๊ก ผู้เรียกต้องแนบ header รหัสเมื่อเรียก middleware เพิ่มความปลอดภัย ควรคุยกับทีม dev/เชื่อมก่อนกดบันทึก'
    },
    '표에 아무도 안 나올 때': {
      EN: 'When the table shows no rows',
      JP: '表に誰も出ないとき',
      CH: '表格没有数据时',
      TH: 'เมื่อตารางไม่มีแถว'
    },
    '검색어를 줄이거나 바꿔 보세요. 그래도 0건이면, 지금 로그인한 계정으로는 그 가맹이 안 보이는 것입니다(상위 조직·권한). 본사 관리자에게 조회 범위를 물어보세요.': {
      EN: 'Try fewer or different search keywords. If it is still zero rows, this account cannot see that merchant (org/permissions). Ask an HQ admin about your search scope.',
      JP: '検索語を減らすか変えてみてください。それでも0件なら、今ログインしているアカウントではその加盟店が見えません（上位組織・権限）。本社管理者に照会範囲を確認してください。',
      CH: '请缩短或更换搜索词。若仍为零行，当前登录账户看不到该商户（上级组织/权限）。请向总部管理员确认可查范围。',
      TH: 'ลองลดหรือเปลี่ยนคำค้น ถ้ายัง 0 แถว บัญชีนี้มองไม่เห็นร้าน (องค์กร/สิทธิ์) ถามผู้ดูแล HQ เรื่องขอบเขตการค้น'
    },
    '개발자용 한 줄(접기)': {
      EN: 'One-line developer note (expand)',
      JP: '開発者向け一行（折りたたみ）',
      CH: '开发者一行说明（可展开）',
      TH: 'บันทึกสั้นสำหรับนักพัฒนา (ขยาย)'
    },
    '키트에는 바인딩·공개 API 베이스·노티 URL·브로커 등이 JSON으로 포함됩니다. 강제 시 헤더 X-Icopay-Merchant-Broker-Secret 없이 /api/middleware/v1/pg/... 호출이 막힙니다. 신규 PG는 본사 PG 연동 설정에 올라오면 PG 범위 목록에 자동 반영됩니다.': {
      EN: 'The kit JSON includes bindings, public API base, notify URL, broker, etc. When enforced, calls to /api/middleware/v1/pg/... without header X-Icopay-Merchant-Broker-Secret are blocked. New PGs appear in the PG scope list once added in HQ PG linkage settings.',
      JP: 'キットのJSONにはバインド、公開APIベース、ノティURL、ブローカー等が含まれます。強制時はヘッダ X-Icopay-Merchant-Broker-Secret なしの /api/middleware/v1/pg/... 呼び出しは拒否されます。新規PGは本社のPG連携設定に載るとPG範囲リストに自動反映されます。',
      CH: '套件 JSON 含绑定、公开 API 基址、通知 URL、broker 等。启用强制后，不带 X-Icopay-Merchant-Broker-Secret 头调用 /api/middleware/v1/pg/... 会被拒绝。新 PG 在总部 PG 对接设置登记后会自动出现在 PG 范围列表。',
      TH: 'ชุด JSON มี binding, public API base, notify URL, broker ฯลฯ เมื่อบังคับ การเรียก /api/middleware/v1/pg/... โดยไม่มี header X-Icopay-Merchant-Broker-Secret จะถูกบล็อก PG ใหม่จะโผล่ในรายการเมื่อตั้งค่าเชื่อม PG ที่ HQ'
    },
    '업체코드 (직접 입력)': {
      EN: 'Company code (direct entry)',
      JP: '加盟店コード（直接入力）',
      CH: '商户代码（直接输入）',
      TH: 'รหัสร้าน (พิมพ์ตรง)'
    },
    '예: M000123': {
      EN: 'e.g. M000123',
      JP: '例: M000123',
      CH: '例：M000123',
      TH: 'เช่น M000123'
    },
    '업체명 (검색)': {
      EN: 'Company name (search)',
      JP: '加盟店名（検索）',
      CH: '商户名称（搜索）',
      TH: 'ชื่อร้าน (ค้นหา)'
    },
    'PG 범위 (키트·시크릿)': {
      EN: 'PG scope (kit & secret)',
      JP: 'PG範囲（キット・シークレット）',
      CH: 'PG 范围（套件与密钥）',
      TH: 'ขอบเขต PG (ชุดและ secret)'
    },
    '가맹점 검색': {
      EN: 'Merchant search',
      JP: '加盟店検索',
      CH: '商户搜索',
      TH: 'ค้นหาร้านค้า'
    },
    '연동 패키지 생성(JSON)': {
      EN: 'Build integration package (JSON)',
      JP: '連携パッケージ生成(JSON)',
      CH: '生成对接包（JSON）',
      TH: 'สร้างแพ็กเกจเชื่อม (JSON)'
    },
    'JSON 연동 패키지': {
      EN: 'JSON integration package',
      JP: 'JSON 連携パッケージ',
      CH: 'JSON 对接包',
      TH: 'แพ็กเกจเชื่อมต่อ (JSON)'
    },
    'PHP 연동 패키지': {
      EN: 'PHP integration package',
      JP: 'PHP 連携パッケージ',
      CH: 'PHP 对接包',
      TH: 'แพ็กเกจเชื่อมต่อ (PHP)'
    },
    '연동 패키지 (JSON / PHP)': {
      EN: 'Integration package (JSON / PHP)',
      JP: '連携パッケージ（JSON / PHP）',
      CH: '对接包（JSON / PHP）',
      TH: 'แพ็กเกจเชื่อมต่อ (JSON / PHP)'
    },
    '업체를 고른 뒤 JSON 또는 PHP 연동 패키지 버튼을 누르세요.': {
      EN: 'Pick a merchant, then click the JSON or PHP package button.',
      JP: '加盟店を選択して、JSON または PHP パッケージボタンを押してください。',
      CH: '选择商户后，请点击 JSON 或 PHP 套件按钮。',
      TH: 'เลือกร้านค้าแล้วกดปุ่มแพ็กเกจ JSON หรือ PHP'
    },
    '연동 키트 JSON·PHP 중 가맹 환경에 맞는 패키지 버튼을 누릅니다.': {
      EN: 'Click the JSON/PHP package button that matches your merchant environment.',
      JP: '加盟店環境に合う JSON/PHP パッケージボタンを押します。',
      CH: '点击符合商户环境的 JSON/PHP 套件按钮。',
      TH: 'คลิกปุ่มแพ็กเกจ JSON/PHP ให้ตรงกับสภาพแวดล้อมของร้าน'
    },
    '브로커 시크릿 재발급': {
      EN: 'Re-issue broker secret',
      JP: 'ブローカーシークレット再発行',
      CH: '重新签发 broker 密钥',
      TH: 'ออก broker secret ใหม่'
    },
    '강제(시크릿 헤더 필수)': {
      EN: 'Enforce (secret header required)',
      JP: '強制（シークレットヘッダ必須）',
      CH: '强制（必须带密钥头）',
      TH: 'บังคับ (ต้องมี secret header)'
    },
    '강제여부 저장': {
      EN: 'Save enforce flag',
      JP: '強制フラグを保存',
      CH: '保存强制标志',
      TH: 'บันทึกการบังคับ'
    },
    '로딩 중…': {
      EN: 'Loading…',
      JP: '読み込み中…',
      CH: '加载中…',
      TH: 'กำลังโหลด…'
    },
    '응답 / 키트 (JSON)': {
      EN: 'Response / kit (JSON)',
      JP: '応答 / キット(JSON)',
      CH: '响应 / 套件（JSON）',
      TH: 'ตอบกลับ / ชุด (JSON)'
    },
    '아래 JSON·PHP 패키지 중 가맹 환경에 맞는 것을 복사해 전달하세요.': {
      EN: 'Copy and share the JSON/PHP package that matches the merchant environment below.',
      JP: '下の JSON/PHP パッケージから、加盟店環境に合うものをコピーして共有してください。',
      CH: '请复制并提供下方与商户环境匹配的 JSON/PHP 套件。',
      TH: 'คัดลอกและส่งแพ็กเกจ JSON/PHP ที่เหมาะกับสภาพแวดล้อมของร้านด้านล่าง'
    },
    'JSON: REST 직접 호출 — curl·buyer 스키마 포함': {
      EN: 'JSON: Direct REST calls — includes curl & buyer schema',
      JP: 'JSON: REST 直接呼び出し — curl・buyer スキーマ含む',
      CH: 'JSON：直接调用 REST — 含 curl 与 buyer 结构',
      TH: 'JSON: เรียก REST โดยตรง — มี curl และสคีมา buyer'
    },
    'PHP: IcopayMerchantApi.php · checkout_unified.php': {
      EN: 'PHP: IcopayMerchantApi.php · checkout_unified.php',
      JP: 'PHP: IcopayMerchantApi.php · checkout_unified.php',
      CH: 'PHP：IcopayMerchantApi.php · checkout_unified.php',
      TH: 'PHP: IcopayMerchantApi.php · checkout_unified.php'
    },
    '샘플 다운로드:': {
      EN: 'Sample download:',
      JP: 'サンプルダウンロード:',
      CH: '示例下载：',
      TH: 'ดาวน์โหลดตัวอย่าง:'
    },
    '연동 파라미터 규격(표):': {
      EN: 'Integration parameter spec (table):',
      JP: '連携パラメータ仕様（表）:',
      CH: '对接参数规范（表）：',
      TH: 'สเปคพารามิเตอร์เชื่อมต่อ (ตาราง):'
    },
    /* /comp/merchantApiPortal — 가맹점 API 연동(조회 전용) */
    '가맹점API': {
      EN: 'Merchant API',
      JP: '加盟店API',
      CH: '商户 API',
      TH: 'API ร้านค้า'
    },
    '가맹점 API 연동': {
      EN: 'Merchant API integration',
      JP: '加盟店API連携',
      CH: '商户 API 对接',
      TH: 'การเชื่อมต่อ API ร้านค้า'
    },
    '가맹점API 안내 본문': {
      EN: 'View only the API keys, endpoints, and samples deployed by headquarters. Key issuance and re-issue are HQ-only.',
      JP: '本社が配布したAPI連携キー・エンドポイント・サンプルのみ閲覧できます。キー発行・再発行は本社のみ可能です。',
      CH: '仅可查看总部部署的 API 对接密钥、端点与样本。密钥签发与重发仅限总部。',
      TH: 'ดูได้เฉพาะคีย์ API จุดเชื่อมต่อ และตัวอย่างที่สำนักงานใหญ่แจกจ่าย การออกคีย์ใหม่ทำได้เฉพาะ HQ'
    },
    '가맹점API 안내 보안': {
      EN: 'Store broker secrets on your server only — never in browsers, apps, or public storage.',
      JP: 'ブローカーシークレットは加盟店サーバーにのみ保管し、ブラウザ・アプリ・公開ストレージに置かないでください。',
      CH: 'Broker 密钥仅保存在商户服务器，切勿放在浏览器、应用或公开存储中。',
      TH: 'เก็บ broker secret บนเซิร์ฟเวอร์ร้านเท่านั้น ห้ามใส่ในเบราว์เซอร์ แอป หรือที่เก็บสาธารณะ'
    },
    '가맹 API 연동 시 유의사항': {
      EN: 'Notes for merchant API integration',
      JP: '加盟店API連携時の注意',
      CH: '商户 API 对接注意事项',
      TH: 'ข้อควรระวังการเชื่อมต่อ API ร้านค้า'
    },
    '가맹 API 유의: prepare 서버 전용': {
      EN: 'Call the Prepare API only on your <strong>server</strong>. Never expose the broker secret in browsers, apps, or public storage.',
      JP: 'Prepare APIは加盟店<strong>サーバー</strong>でのみ呼び出してください。ブローカーシークレットをブラウザ・アプリ・公開ストレージに露出しないでください。',
      CH: 'Prepare API 仅在商户<strong>服务器</strong>调用。切勿在浏览器、应用或公开存储中暴露 broker 密钥。',
      TH: 'เรียก Prepare API เฉพาะบน<strong>เซิร์ฟเวอร์</strong>ร้านเท่านั้น ห้ามเปิดเผย broker secret ในเบราว์เซอร์ แอป หรือที่เก็บสาธารณะ'
    },
    '가맹 API 유의: sessionToken embed': {
      EN: 'Pass only the Prepare response <code>sessionToken</code> and the Embed script to the browser.',
      JP: 'ブラウザには Prepare 応答の <code>sessionToken</code> と Embed スクリプトのみ渡してください。',
      CH: '浏览器仅传递 Prepare 响应中的 <code>sessionToken</code> 与 Embed 脚本。',
      TH: 'ส่งให้เบราว์เซอร์เฉพาะ <code>sessionToken</code> จาก Prepare และสคริปต์ Embed'
    },
    '가맹 API 유의: 결제 확정': {
      EN: 'Confirm payment on your <strong>server</strong> via webhook (<code>merchantNotifyUrls</code>) or the Status API.',
      JP: '決済確定はウェブフック（<code>merchantNotifyUrls</code>）または Status API で<strong>サーバー</strong>側で確認してください。',
      CH: '请在<strong>服务器</strong>通过 webhook（<code>merchantNotifyUrls</code>）或 Status API 确认支付。',
      TH: 'ยืนยันการชำระบน<strong>เซิร์ฟเวอร์</strong>ผ่าน webhook (<code>merchantNotifyUrls</code>) หรือ Status API'
    },
    '연동 키': {
      EN: 'Integration keys',
      JP: '連携キー',
      CH: '对接密钥',
      TH: 'คีย์เชื่อมต่อ'
    },
    'PG 바인딩 (MID)': {
      EN: 'PG binding (MID)',
      JP: 'PGバインディング（MID）',
      CH: 'PG 绑定 (MID)',
      TH: 'PG binding (MID)'
    },
    '브로커 시크릿': {
      EN: 'Broker secret',
      JP: 'ブローカーシークレット',
      CH: 'Broker 密钥',
      TH: 'Broker secret'
    },
    '미발행': {
      EN: 'Not issued',
      JP: '未発行',
      CH: '未发行',
      TH: 'ยังไม่ออก'
    },
    '발행': {
      EN: 'Issued',
      JP: '発行',
      CH: '已发行',
      TH: 'ออกแล้ว'
    },
    '재발행': {
      EN: 'Reissued',
      JP: '再発行',
      CH: '重新发行',
      TH: 'ออกใหม่'
    },
    '발행일자': {
      EN: 'Issue date',
      JP: '発行日',
      CH: '发行日期',
      TH: 'วันที่ออก'
    },
    '발행자': {
      EN: 'Issued by',
      JP: '発行者',
      CH: '发行人',
      TH: 'ผู้ออก'
    },
    '공개 API 베이스 URL': {
      EN: 'Public API base URL',
      JP: '公開APIベースURL',
      CH: '公开 API 基础 URL',
      TH: 'Public API base URL'
    },
    'HTTP 헤더명': {
      EN: 'HTTP header name',
      JP: 'HTTPヘッダー名',
      CH: 'HTTP 头名称',
      TH: 'ชื่อ HTTP header'
    },
    '강제(헤더 필수)': {
      EN: 'Enforced (header required)',
      JP: '強制（ヘッダー必須）',
      CH: '强制（必填头）',
      TH: 'บังคับ (ต้องมี header)'
    },
    '항목': {
      EN: 'Item',
      JP: '項目',
      CH: '项目',
      TH: 'รายการ'
    },
    '값': {
      EN: 'Value',
      JP: '値',
      CH: '值',
      TH: 'ค่า'
    },
    '작업': {
      EN: 'Actions',
      JP: '操作',
      CH: '操作',
      TH: 'การทำงาน'
    },
    '숨기기': {
      EN: 'Hide',
      JP: '非表示',
      CH: '隐藏',
      TH: 'ซ่อน'
    },
    '복사할 값이 없습니다.': {
      EN: 'Nothing to copy.',
      JP: 'コピーする値がありません。',
      CH: '没有可复制的内容。',
      TH: 'ไม่มีค่าที่จะคัดลอก'
    },
    'API 연동 배포가 완료되지 않았습니다. 본사에 API 배포를 요청하세요.': {
      EN: 'API integration has not been deployed yet. Please ask headquarters to deploy API access.',
      JP: 'API連携の配布が完了していません。本社にAPI配布を依頼してください。',
      CH: 'API 对接尚未部署。请联系总部开通 API 部署。',
      TH: 'ยังไม่ได้แจกจ่าย API การเชื่อมต่อ โปรดติดต่อสำนักงานใหญ่'
    },
    '전체(ALL)': {
      EN: 'All (ALL)',
      JP: '全体(ALL)',
      CH: '全部(ALL)',
      TH: 'ทั้งหมด (ALL)'
    },
    '복사에 실패했습니다. 값을 직접 선택해 복사하세요.': {
      EN: 'Copy failed. Select the value and copy manually.',
      JP: 'コピーに失敗しました。値を選択して手動でコピーしてください。',
      CH: '复制失败。请手动选择并复制该值。',
      TH: 'คัดลอกไม่สำเร็จ โปรดเลือกค่าแล้วคัดลอกเอง'
    },
    /* /hq/merchantApiDeployDocs — API 배포 문서 */
    'API 배포 문서': {
      EN: 'API deployment documents',
      JP: 'API配布ドキュメント',
      CH: 'API 部署文档',
      TH: 'เอกสาร API สำหรับร้าน'
    },
    '가맹점 연동용 자료': {
      EN: 'Merchant integration materials',
      JP: '加盟店向け連携資料',
      CH: '商户对接资料',
      TH: 'เอกสารเชื่อมต่อสำหรับร้าน'
    },
    'API배포문서 안내 본문': {
      EN: 'View and download integration samples, parameter specs, and endpoints for merchants on this screen. Re-issue broker secrets on the “Merchant API (generate)” screen.',
      JP: 'この画面で加盟店向けの連携サンプル・パラメータ仕様・エンドポイントを確認・ダウンロードできます。ブローカーシークレットの再発行は「加盟店API生成」画面で行います。',
      CH: '在此页面查看并下载商户对接样本、参数规范与端点。Broker 密钥重发请在「生成商户 API」页面操作。',
      TH: 'ดูและดาวน์โหลดตัวอย่างเชื่อมต่อ สเปคพารามิเตอร์ และ endpoint สำหรับร้านได้ที่หน้านี้ ออก broker secret ใหม่ที่หน้า「สร้าง Merchant API」'
    },
    'API배포문서 안내 보안': {
      EN: 'Keep broker secrets on the merchant server only — never expose them in browsers or apps.',
      JP: 'ブローカーシークレットは加盟店サーバーにのみ置き、ブラウザ・アプリに露出しないでください。',
      CH: 'Broker 密钥仅保存在商户服务器，切勿在浏览器或应用中暴露。',
      TH: 'เก็บ broker secret ไว้ที่เซิร์ฟเวอร์ร้านเท่านั้น ห้ามเปิดเผยในเบราว์เซอร์หรือแอป'
    },
    '다운로드 자료': {
      EN: 'Download materials',
      JP: 'ダウンロード資料',
      CH: '下载资料',
      TH: 'เอกสารดาวน์โหลด'
    },
    '연동 파라미터 규격': {
      EN: 'Integration parameter specification',
      JP: '連携パラメータ仕様',
      CH: '对接参数规范',
      TH: 'สเปคพารามิเตอร์เชื่อมต่อ'
    },
    'HTTP 헤더': {
      EN: 'HTTP headers',
      JP: 'HTTPヘッダ',
      CH: 'HTTP 头',
      TH: 'HTTP headers'
    },
    'Prepare 본문 파라미터': {
      EN: 'Prepare body parameters',
      JP: 'Prepare 本文パラメータ',
      CH: 'Prepare 请求体参数',
      TH: 'พารามิเตอร์ body Prepare'
    },
    'buyer 객체 파라미터': {
      EN: 'buyer object parameters',
      JP: 'buyer オブジェクトパラメータ',
      CH: 'buyer 对象参数',
      TH: 'พารามิเตอร์อ็อบเจ็กต์ buyer'
    },
    'Status 조회 파라미터': {
      EN: 'Status query parameters',
      JP: 'Status 照会パラメータ',
      CH: 'Status 查询参数',
      TH: 'พารามิเตอร์ query Status'
    },
    '오류 코드': {
      EN: 'Error codes',
      JP: 'エラーコード',
      CH: '错误代码',
      TH: 'รหัสข้อผิดพลาด'
    },
    '오류코드': {
      EN: 'Error code',
      JP: 'エラーコード',
      CH: '错误代码',
      TH: 'รหัสข้อผิดพลาด'
    },
    '의미': {
      EN: 'Meaning',
      JP: '意味',
      CH: '含义',
      TH: 'ความหมาย'
    },
    '통합 Checkout 엔드포인트': {
      EN: 'Unified Checkout endpoints',
      JP: '統合 Checkout エンドポイント',
      CH: '统一 Checkout 端点',
      TH: 'Unified Checkout endpoints'
    },
    '통합 Checkout 연동 흐름 설명서': {
      EN: 'Unified Checkout integration flow guide',
      JP: '統合 Checkout 連携フロー説明書',
      CH: '统一 Checkout 对接流程说明',
      TH: 'คู่มือ flow Unified Checkout'
    },
    '통합 Checkout 연동 흐름 설명서 안내': {
      EN: 'Roles of Prepare, Session, Status, and the Embed script, plus the recommended integration sequence. The HTML guide below matches your screen language.',
      JP: 'Prepare・Session・Status・Embed スクリプトの役割と推奨連携手順です。下の HTML は画面言語に合わせて表示されます。',
      CH: '说明 Prepare、Session、Status 与 Embed 脚本的作用及推荐对接顺序。下方 HTML 随界面语言显示。',
      TH: 'บทบาทของ Prepare, Session, Status และ Embed script พร้อมลำดับเชื่อมต่อที่แนะนำ ด้านล่างเป็น HTML ตามภาษาหน้าจอ'
    },
    '연동 흐름 설명서': {
      EN: 'Integration flow guide',
      JP: '連携フロー説明書',
      CH: '对接流程说明',
      TH: 'คู่มือ flow การเชื่อมต่อ'
    },
    '연동설명서': {
      EN: 'Integration guide',
      JP: '連携説明書',
      CH: '对接说明',
      TH: 'คู่มือการเชื่อมต่อ'
    },
    '통합 Checkout 엔드포인트 설명서 (HTML)': {
      EN: 'Unified Checkout endpoint guide (HTML)',
      JP: '統合 Checkout エンドポイント説明書 (HTML)',
      CH: '统一 Checkout 端点说明 (HTML)',
      TH: 'คู่มือ Unified Checkout endpoints (HTML)'
    },
    '통합 Checkout 엔드포인트 설명서 (텍스트)': {
      EN: 'Unified Checkout endpoint guide (text)',
      JP: '統合 Checkout エンドポイント説明書 (テキスト)',
      CH: '统一 Checkout 端点说明（文本）',
      TH: 'คู่มือ Unified Checkout endpoints (ข้อความ)'
    },
    '연동 흐름 설명서 새 탭에서 열기': {
      EN: 'Open integration flow guide in a new tab',
      JP: '連携フロー説明書を新しいタブで開く',
      CH: '在新标签页打开对接流程说明',
      TH: 'เปิดคู่มือ flow ในแท็บใหม่'
    },
    '문서를 열 수 없습니다.': {
      EN: 'Could not open the document.',
      JP: '文書を開けませんでした。',
      CH: '无法打开文档。',
      TH: 'เปิดเอกสารไม่ได้'
    },
    '문서를 불러올 수 없습니다.': {
      EN: 'Could not load the document.',
      JP: '文書を読み込めませんでした。',
      CH: '无法加载文档。',
      TH: 'โหลดเอกสารไม่ได้'
    },
    '연동 체크리스트': {
      EN: 'Integration checklist',
      JP: '連携チェックリスト',
      CH: '对接检查清单',
      TH: 'เช็กลิสต์การเชื่อมต่อ'
    },
    'Checkout API 엔드포인트': {
      EN: 'Checkout API endpoints',
      JP: 'Checkout API エンドポイント',
      CH: 'Checkout API 端点',
      TH: 'Checkout API endpoints'
    },
    'WordPress JPAY 플러그인': {
      EN: 'WordPress JPAY plugins',
      JP: 'WordPress JPAY プラグイン',
      CH: 'WordPress JPAY 插件',
      TH: 'WordPress JPAY plugins'
    },
    '통합 Checkout (인라인)': {
      EN: 'Unified Checkout (inline)',
      JP: '統合 Checkout（インライン）',
      CH: '统一 Checkout（内联）',
      TH: 'Unified Checkout (inline)'
    },
    '통합 Checkout (리다이렉트)': {
      EN: 'Unified Checkout (redirect)',
      JP: '統合 Checkout（リダイレクト）',
      CH: '统一 Checkout（重定向）',
      TH: 'Unified Checkout (redirect)'
    },
    'JPAY 인라인': {
      EN: 'JPAY inline',
      JP: 'JPAY インライン',
      CH: 'JPAY 内联',
      TH: 'JPAY inline'
    },
    'JPAY 리다이렉트': {
      EN: 'JPAY redirect',
      JP: 'JPAY リダイレクト',
      CH: 'JPAY 重定向',
      TH: 'JPAY redirect'
    },
    'ChillPay 인라인': {
      EN: 'ChillPay inline',
      JP: 'ChillPay インライン',
      CH: 'ChillPay 内联',
      TH: 'ChillPay inline'
    },
    'ChillPay 리다이렉트': {
      EN: 'ChillPay redirect',
      JP: 'ChillPay リダイレクト',
      CH: 'ChillPay 重定向',
      TH: 'ChillPay redirect'
    },
    '결제 페이지 URL 템플릿': {
      EN: 'Payment page URL template',
      JP: '決済ページ URL テンプレート',
      CH: '支付页 URL 模板',
      TH: 'เทมเพลต URL หน้าชำระเงิน'
    },
    '일반 WordPress': {
      EN: 'General WordPress',
      JP: '一般 WordPress',
      CH: '一般 WordPress',
      TH: 'WordPress ทั่วไป'
    },
    '배포 가이드': {
      EN: 'Deployment guide',
      JP: '配布ガイド',
      CH: '部署指南',
      TH: 'คู่มือ deploy'
    },
    'WordPress JPAY 플러그인 ZIP — 기본 inline, redirect는 HQ REDIRECT Y': {
      EN: 'WordPress JPAY plugin ZIPs — default inline; redirect requires HQ REDIRECT Y',
      JP: 'WordPress JPAY プラグイン ZIP — 既定 inline、redirect は HQ REDIRECT Y',
      CH: 'WordPress JPAY 插件 ZIP — 默认 inline；redirect 需 HQ REDIRECT Y',
      TH: 'WordPress JPAY plugin ZIP — inline ค่าเริ่มต้น redirect ต้อง HQ REDIRECT Y'
    },
    '리다이렉트 checkout API — returnUrl/cancelUrl HTTPS 필수': {
      EN: 'Redirect checkout API — HTTPS returnUrl/cancelUrl required',
      JP: 'リダイレクト checkout API — returnUrl/cancelUrl は HTTPS 必須',
      CH: '重定向 checkout API — returnUrl/cancelUrl 须 HTTPS',
      TH: 'Redirect checkout API — ต้อง returnUrl/cancelUrl แบบ HTTPS'
    },
    '결제 통보 (Webhook) 안내': {
      EN: 'Payment notify (webhook) guide',
      JP: '決済通知 (Webhook) 案内',
      CH: '支付通知 (Webhook) 说明',
      TH: 'คู่มือการแจ้งชำระ (Webhook)'
    },
    'PG→ICOPAY 노티 (본사 설정)': {
      EN: 'PG→ICOPAY notify (HQ config)',
      JP: 'PG→ICOPAY ノティ（本社設定）',
      CH: 'PG→ICOPAY 通知（总部配置）',
      TH: 'PG→ICOPAY notify (ตั้งค่า HQ)'
    },
    '등록된 가맹 결제 통보 URL': {
      EN: 'Registered merchant payment notify URLs',
      JP: '登録済み加盟店決済通知 URL',
      CH: '已登记的商户支付通知 URL',
      TH: 'URL แจ้งชำระร้านที่ลงทะเบียนแล้ว'
    },
    '등록된 merchantNotifyUrls가 없습니다. 본사 업체관리에서 등록하세요.': {
      EN: 'No merchantNotifyUrls registered. Ask HQ to register them in merchant management.',
      JP: 'merchantNotifyUrls が未登録です。本社の加盟店管理で登録してください。',
      CH: '尚未登记 merchantNotifyUrls。请在总部商户管理中登记。',
      TH: 'ยังไม่มี merchantNotifyUrls — ให้ HQ ลงทะเบียนในการจัดการร้าน'
    },
    '아래 연동 배포 요약에서 엔드포인트·Webhook·체크리스트를 확인하세요.': {
      EN: 'See endpoints, webhook guide, and checklist in the deployment summary below.',
      JP: '下の連携配布サマリーでエンドポイント・Webhook・チェックリストを確認してください。',
      CH: '请在下方对接部署摘要中查看端点、Webhook 与检查清单。',
      TH: 'ดู endpoints, webhook และเช็กลิสต์ในสรุปการ deploy ด้านล่าง'
    },
    'WordPress JPAY 배포 가이드': {
      EN: 'WordPress JPAY deployment guide',
      JP: 'WordPress JPAY 配布ガイド',
      CH: 'WordPress JPAY 部署指南',
      TH: 'คู่มือ deploy WordPress JPAY'
    },
    'WooCommerce 플러그인 ZIP': {
      EN: 'WooCommerce plugin ZIP',
      JP: 'WooCommerce プラグイン ZIP',
      CH: 'WooCommerce 插件 ZIP',
      TH: 'ZIP ปลั๊กอิน WooCommerce'
    },
    '일반 WordPress 플러그인 ZIP': {
      EN: 'General WordPress plugin ZIP',
      JP: '一般 WordPress プラグイン ZIP',
      CH: '一般 WordPress 插件 ZIP',
      TH: 'ZIP ปลั๊กอิน WordPress ทั่วไป'
    },
    '가이드 열기': {
      EN: 'Open guide',
      JP: 'ガイドを開く',
      CH: '打开指南',
      TH: 'เปิดคู่มือ'
    },
    'ZIP 열기': {
      EN: 'Open ZIP',
      JP: 'ZIP を開く',
      CH: '打开 ZIP',
      TH: 'เปิด ZIP'
    },
    '가맹 API 연동 채널': {
      EN: 'Merchant API integration channels',
      JP: '加盟店 API 連携チャネル',
      CH: '商户 API 对接渠道',
      TH: 'ช่องเชื่อมต่อ Merchant API'
    },
    'API 인라인 연동': {
      EN: 'API inline integration',
      JP: 'API インライン連携',
      CH: 'API 内联对接',
      TH: 'API inline'
    },
    'API 리다이렉트 연동': {
      EN: 'API redirect integration',
      JP: 'API リダイレクト連携',
      CH: 'API 重定向对接',
      TH: 'API redirect'
    },
    'WordPress/WooCommerce': {
      EN: 'WordPress/WooCommerce',
      JP: 'WordPress/WooCommerce',
      CH: 'WordPress/WooCommerce',
      TH: 'WordPress/WooCommerce'
    },
    'WordPress 플러그인 제공': {
      EN: 'WordPress plugin rollout',
      JP: 'WordPress プラグイン提供',
      CH: 'WordPress 插件提供',
      TH: 'WordPress plugin'
    },
    'WordPress/WooCommerce 연동 채널이 꺼져 있습니다. 업체관리에서 켜거나 본사에 요청하세요.': {
      EN: 'WordPress/WooCommerce channel is off. Enable it in company management or ask HQ.',
      JP: 'WordPress/WooCommerce チャネルが OFF です。加盟店管理で ON にするか本社へ依頼してください。',
      CH: 'WordPress/WooCommerce 渠道已关闭。请在商户管理中开启或联系总部。',
      TH: 'ช่อง WordPress/WooCommerce ปิด — เปิดในการจัดการร้าน หรือติดต่อ HQ'
    },
    /* /comp/compDetail · /comp/compReg — 가맹 정보(加盟店情報) 카드 */
    '업체구분에 따라 해당하는 입력 항목이 표시됩니다. 사용여부를 미사용으로 변경하면 하위 조직 전체가 미사용됩니다. 가맹점은 상위 지점을 변경하여 다른 사용 중인 상위 아래로 활성화할 수 있습니다.': {
      EN: 'Fields shown depend on org type. Setting usage to Disabled disables all sub-orgs. Merchants can be re-enabled by moving them under another active parent branch.',
      JP: '組織区分に応じて入力項目が表示されます。使用可否を「未使用」にすると下位組織全体が未使用になります。加盟店は上位店舗を変更し、使用中の別の上位の下で有効化できます。',
      CH: '根据组织类型显示对应输入项。将使用状态改为停用时，下级组织全部停用。商户可通过更换上级网点，挂到另一个使用中的上级下以重新启用。',
      TH: 'ฟิลด์ที่แสดงขึ้นอยู่กับประเภทองค์กร หากตั้งการใช้งานเป็นไม่ใช้ องค์กรย่อยทั้งหมดจะถูกปิด ร้านค้าสามารถเปิดใช้ใหม่ได้โดยย้ายไปอยู่ใต้หน่วยงานที่ใช้งานอยู่'
    },
    '미사용 선택 시 WEB 결제 시스템이 중지됩니다. 「결제 URL」은 운영·WEB·URL결제 PG별 공개 경로로 자동 표시됩니다(예: JPAY /jpay-pay/업체코드, ChillPay /pay/업체코드). 「URL 재결제 URL」은 해당 PG가 저장 카드 재결제를 지원하고 본사 URL 재결제 기능·URL재결제 PG 바인딩이 있을 때만 표시됩니다. 「URL 결제 방식」은 공개 URL 결제에만 적용됩니다. API·챗봇은 각 설정 카드에서 별도 선택합니다.': {
      EN: 'When Disabled is selected, the WEB payment system stops. 「Payment URL」 is shown automatically per active WEB/URL PG (e.g. JPAY /jpay-pay/{merchant code}, ChillPay /pay/{merchant code}). 「URL repay URL」 appears only when the PG supports stored-card repay and HQ URL repay plus URL-repay PG binding are configured. 「URL checkout mode」 applies to public URL checkout only. API and chatbot are set in their own cards.',
      JP: '「未使用」を選ぶとWEB決済システムは停止します。「決済URL」は運用・WEB・URL決済PGごとの公開パスが自動表示されます（例: JPAY /jpay-pay/加盟店コード, ChillPay /pay/加盟店コード）。「URL再決済URL」は当該PGが保存カード再決済に対応し、本社URL再決済機能・URL再決済PGバインドがある場合のみ表示されます。「URL決済方式」は公開URL決済のみに適用されます。API・チャットボットは各設定カードで別途選択します。',
      CH: '选择停用时 WEB 支付系统停止。「支付 URL」按运营·WEB·URL 支付 PG 自动显示公开路径（例：JPAY /jpay-pay/商户代码，ChillPay /pay/商户代码）。「URL 再支付 URL」仅在该 PG 支持保存卡再支付且总部 URL 再支付功能与 URL 再支付 PG 绑定均有时显示。「URL 支付方式」仅用于公开 URL 支付。API 与聊天机器人请在各自卡片中单独设置。',
      TH: 'เลือกไม่ใช้งานแล้วระบบชำระ WEB จะหยุด 「Payment URL」 แสดงเส้นทางสาธารณะตาม PG WEB/URL ที่เปิด (เช่น JPAY /jpay-pay/รหัสร้าน, ChillPay /pay/รหัสร้าน) 「URL repay URL」 แสดงเมื่อ PG รองรับชำระซ้ำด้วยบัตรที่บันทึก และ HQ เปิด URL repay พร้อม binding PG URL repay 「URL checkout mode」 ใช้กับ checkout URL สาธารณะเท่านั้น API และ chatbot ตั้งในการ์ดแยก'
    },
    'JPAY 결제창 입력 필드': {
      EN: 'JPAY checkout input fields',
      JP: 'JPAY決済画面入力項目',
      CH: 'JPAY 收银台输入字段',
      TH: 'ฟิลด์ป้อนข้อมูลหน้าชำระ JPAY'
    },
    'JPAY URL 인라인 결제창(jpay-pay.html) 입력 필드입니다. JPAY 필수: (1)카드·CVV (2)성명 (3)이메일 (4)국가코드(ISO2) (5)전화(국가코드 제외). (6)배송 주소는 선택. <strong>본사 기본 따름</strong>이면 본사설정 → 결제로직설정 값을 사용합니다.': {
      EN: 'Input fields for the JPAY URL inline checkout (jpay-pay.html). JPAY required: (1) card·CVV (2) name (3) email (4) country code (ISO2) (5) phone (local number, no country code). (6) shipping address optional. If <strong>Follow HQ default</strong>, values from HQ Settings → Payment orchestration apply.',
      JP: 'JPAY URLインライン決済画面(jpay-pay.html)の入力項目です。JPAY必須: (1)カード・CVV (2)氏名 (3)メール (4)国コード(ISO2) (5)電話(国コード除く). (6)配送先は任意。<strong>本社デフォルトに従う</strong>場合は本社設定 → 決済ロジック設定の値を使用します。',
      CH: 'JPAY URL 内联收银台 (jpay-pay.html) 输入字段。JPAY 必填：(1)卡·CVV (2)姓名 (3)邮箱 (4)国家代码(ISO2) (5)电话(不含国家代码)。(6)配送地址可选。若<strong>跟随总部默认</strong>，使用总部设置 → 支付逻辑设置的值。',
      TH: 'ฟิลด์ป้อนข้อมูลหน้า JPAY URL inline (jpay-pay.html) JPAY จำเป็น: (1)บัตร·CVV (2)ชื่อ (3)อีเมล (4)รหัสประเทศ ISO2 (5)โทร(ไม่รวมรหัสประเทศ) (6)ที่อยู่จัดส่งเป็นทางเลือก หาก<strong>ตามค่าเริ่มต้น HQ</strong> ใช้ค่าจาก HQ Settings → Payment orchestration'
    },
    '본사 기본 따름': {
      EN: 'Follow HQ default',
      JP: '本社デフォルトに従う',
      CH: '跟随总部默认',
      TH: 'ตามค่าเริ่มต้น HQ'
    },
    '1형 전체 (카드·성명·이메일·전화·배송)': {
      EN: 'Type 1 full (card·name·email·phone·shipping)',
      JP: '1型 全体 (カード・氏名・メール・電話・配送)',
      CH: '1型 全部 (卡·姓名·邮箱·电话·配送)',
      TH: 'แบบ 1 เต็ม (บัตร·ชื่อ·อีเมล·โทร·จัดส่ง)'
    },
    '2형 필수 4항목 (카드·성명·이메일·전화)': {
      EN: 'Type 2 required 4 (card·name·email·phone)',
      JP: '2型 必須4項目 (カード・氏名・メール・電話)',
      CH: '2型 必填4项 (卡·姓名·邮箱·电话)',
      TH: 'แบบ 2 จำเป็น 4 รายการ (บัตร·ชื่อ·อีเมล·โทร)'
    },
    '3형 카드·성명 + 가맹 prefill': {
      EN: 'Type 3 card·name + merchant prefill',
      JP: '3型 カード・氏名 + 加盟店 prefill',
      CH: '3型 卡·姓名 + 商户 prefill',
      TH: 'แบบ 3 บัตร·ชื่อ + merchant prefill'
    },
    'JPAY 필수: 국가코드(ISO2)·전화번호는 분리 입력(전화에 +82 등 붙이지 않음). 1·2형은 접속국가가 국가코드 드롭다운 기본값. 3형은 prepare buyerPrefill 의 countryIso2·phone(국가코드 제외). 2형: 주소 숨김. 3형: 카드·성명만 고객 입력.': {
      EN: 'JPAY required: country code (ISO2) and phone are separate (do not prefix phone with +82, etc.). Types 1·2: visitor country is the default for the country dropdown. Type 3: prepare buyerPrefill countryIso2·phone (no country code). Type 2: hide address. Type 3: customer enters card·name only.',
      JP: 'JPAY必須: 国コード(ISO2)・電話番号は分離入力(電話に+82等を付けない)。1・2型は接続国が国コードドロップダウンの初期値。3型はprepare buyerPrefillのcountryIso2・phone(国コード除く)。2型: 住所非表示。3型: カード・氏名のみ顧客入力。',
      CH: 'JPAY 必填：国家代码(ISO2)与电话号码分开输入（电话勿加 +82 等）。1·2 型：访问国家作为国家代码下拉默认值。3 型：prepare buyerPrefill 的 countryIso2·phone（不含国家代码）。2 型：隐藏地址。3 型：客户仅输入卡·姓名。',
      TH: 'JPAY จำเป็น: รหัสประเทศ ISO2 และโทรแยกกัน (อย่าใส่ +82 ในโทร) แบบ 1·2: ประเทศผู้เข้าชมเป็นค่าเริ่มต้นของ dropdown แบบ 3: prepare buyerPrefill countryIso2·phone (ไม่รวมรหัสประเทศ) แบบ 2: ซ่อนที่อยู่ แบบ 3: ลูกค้ากรอกบัตร·ชื่อเท่านั้น'
    },
    '가맹점 API(prepare·embed·redirect·WordPress) 연동 방식을 가맹별로 오픈합니다. 본사 전역 상한은 배포설정 → 결제로직설정입니다. WordPress 사용 시 API 인라인 또는 리다이렉트 중 하나 이상을 켜야 합니다.': {
      EN: 'Enable merchant API integration modes (prepare·embed·redirect·WordPress) per merchant. HQ global caps: Deployment → Payment orchestration. WordPress requires at least one of API inline or redirect.',
      JP: '加盟店API(prepare・embed・redirect・WordPress)連携方式を加盟店別に開きます。本社全体上限はデプロイ設定 → 決済ロジック設定です。WordPress使用時はAPIインラインまたはリダイレクトのいずれか以上をONにしてください。',
      CH: '按商户开启 API 对接方式 (prepare·embed·redirect·WordPress)。总部全局上限：部署设置 → 支付逻辑设置。使用 WordPress 时须至少开启 API 内联或重定向之一。',
      TH: 'เปิดช่อง Merchant API (prepare·embed·redirect·WordPress) ต่อร้าน ขีดจำกัด HQ: Deployment → Payment orchestration ใช้ WordPress ต้องเปิด API inline หรือ redirect อย่างน้อยหนึ่งช่อง'
    },
    '가맹점 API 생성·배포문서·가맹점API 화면에는 여기서 켠 채널만 노출됩니다. prepare API도 비활성 채널은 INTEGRATION_CHANNEL_DISABLED 로 거부됩니다.': {
      EN: 'Merchant API generate, deployment docs, and merchant API portal show only channels enabled here. prepare API rejects disabled channels with INTEGRATION_CHANNEL_DISABLED.',
      JP: '加盟店API生成・配布ドキュメント・加盟店API画面にはここでONにしたチャネルのみ表示されます。prepare APIも無効チャネルはINTEGRATION_CHANNEL_DISABLEDで拒否されます。',
      CH: '商户 API 生成、部署文档、商户 API 门户仅显示此处开启的渠道。prepare API 对未启用渠道返回 INTEGRATION_CHANNEL_DISABLED。',
      TH: 'หน้าสร้าง Merchant API เอกสาร deploy และ portal แสดงเฉพาะช่องที่เปิดที่นี่ prepare API ปฏิเสธช่องปิดด้วย INTEGRATION_CHANNEL_DISABLED'
    },
    '챗봇 플로팅 위젯(홈페이지·쇼핑몰 삽입)': {
      EN: 'Chatbot floating widget (homepage / store embed)',
      JP: 'チャットボットフローティングウィジェット（ホームページ・ショップ埋め込み）',
      CH: '聊天机器人浮动组件（网站/商城嵌入）',
      TH: 'วิดเจ็ตแชทบอทลอย (ฝังหน้าเว็บ/ร้านค้า)'
    },
    '모든 페이지에 공통으로 넣으려면 HTML 하단의 body 태그 직전(또는 쇼핑몰 공통 스크립트)에 아래 한 줄을 붙여 넣으세요.': {
      EN: 'To show on every page, paste the line below just before the closing </body> tag (or in your store’s shared script include).',
      JP: '全ページで共通表示するには、HTML下部の</body>直前（またはショップ共通スクリプト）に下記1行を貼り付けてください。',
      CH: '若要在所有页面共用，请将下面一行粘贴到 HTML 底部 </body> 标签前（或商城公共脚本中）。',
      TH: 'หากต้องการใช้ทุกหน้า ให้วางบรรทัดด้านล่างก่อนแท็ก </body> (หรือในสคริปต์ร่วมของร้านค้า)'
    },
    '챗봇 결제 QR': {
      EN: 'Chatbot payment QR',
      JP: 'チャットボット決済 QR',
      CH: '聊天机器人支付 QR',
      TH: 'QR ชำระ chatbot'
    },
    '카메라로 스캔하면 챗봇 결제 페이지로 이동합니다. 전단·POP·매장 안내에 사용할 수 있습니다.': {
      EN: 'Scan with a camera to open the chatbot payment page. Use on flyers, POP, or in-store signage.',
      JP: 'カメラでスキャンするとチャットボット決済ページへ移動します。チラシ・POP・店頭案内に使えます。',
      CH: '用相机扫描即可进入聊天机器人支付页。可用于传单、POP、店内指引。',
      TH: 'สแกนด้วยกล้องเพื่อเปิดหน้าชำระ chatbot ใช้กับใบปลิว POP หรือป้ายในร้านได้'
    },
    '챗봇 QR를 불러올 수 없습니다. 챗봇 결제 사용이 켜져 있는지 확인하세요.': {
      EN: 'Cannot load chatbot QR. Check that chatbot payment is enabled.',
      JP: 'チャットボットQRを読み込めません。チャットボット決済がONか確認してください。',
      CH: '无法加载聊天机器人 QR。请确认已开启聊天机器人支付。',
      TH: 'โหลด QR chatbot ไม่ได้ ตรวจสอบว่าเปิดชำระ chatbot แล้ว'
    },
    'QR 열기': {
      EN: 'Open QR',
      JP: 'QRを開く',
      CH: '打开 QR',
      TH: 'เปิด QR'
    },
    'PNG 저장': {
      EN: 'Save PNG',
      JP: 'PNG保存',
      CH: '保存 PNG',
      TH: 'บันทึก PNG'
    },
    '업체를 선택하면 연동 자료가 표시됩니다.': {
      EN: 'Select a merchant to show integration materials.',
      JP: '加盟店を選択すると連携資料が表示されます。',
      CH: '选择商户后将显示对接资料。',
      TH: 'เลือกร้านเพื่อแสดงเอกสารเชื่อมต่อ'
    },
    '파라미터 규격': {
      EN: 'Parameter specification',
      JP: 'パラメータ仕様',
      CH: '参数规范',
      TH: 'สเปคพารามิเตอร์'
    },
    '통합 Checkout prepare API 필드 표(HTML)': {
      EN: 'Unified Checkout prepare API field table (HTML)',
      JP: '統合 Checkout prepare API フィールド表(HTML)',
      CH: '统一 Checkout prepare API 字段表（HTML）',
      TH: 'ตารางฟิลด์ Unified Checkout prepare API (HTML)'
    },
    '통합 Checkout prepare API 필드 표(텍스트)': {
      EN: 'Unified Checkout prepare API field table (text)',
      JP: '統合 Checkout prepare API フィールド表(テキスト)',
      CH: '统一 Checkout prepare API 字段表（文本）',
      TH: 'ตารางฟิลด์ Unified Checkout prepare API (ข้อความ)'
    },
    'HTML 열기': {
      EN: 'Open HTML',
      JP: 'HTMLを開く',
      CH: '打开 HTML',
      TH: 'เปิด HTML'
    },
    'TXT 열기': {
      EN: 'Open TXT',
      JP: 'TXTを開く',
      CH: '打开 TXT',
      TH: 'เปิด TXT'
    },
    'prepare 요청 샘플': {
      EN: 'Prepare request sample',
      JP: 'prepare リクエストサンプル',
      CH: 'Prepare 请求示例',
      TH: 'ตัวอย่างคำขอ prepare'
    },
    'prepare 응답 예시': {
      EN: 'Prepare response example',
      JP: 'prepare レスポンス例',
      CH: 'Prepare 响应示例',
      TH: 'ตัวอย่างการตอบ prepare'
    },
    'JSON 열기': {
      EN: 'Open JSON',
      JP: 'JSONを開く',
      CH: '打开 JSON',
      TH: 'เปิด JSON'
    },
    'IcopayMerchantApi.php 클라이언트': {
      EN: 'IcopayMerchantApi.php client',
      JP: 'IcopayMerchantApi.php クライアント',
      CH: 'IcopayMerchantApi.php 客户端',
      TH: 'ไคลเอนต์ IcopayMerchantApi.php'
    },
    'checkout_unified.php 샘플': {
      EN: 'checkout_unified.php sample',
      JP: 'checkout_unified.php サンプル',
      CH: 'checkout_unified.php 示例',
      TH: 'ตัวอย่าง checkout_unified.php'
    },
    'icopay_config.example.php': {
      EN: 'icopay_config.example.php',
      JP: 'icopay_config.example.php',
      CH: 'icopay_config.example.php',
      TH: 'icopay_config.example.php'
    },
    'PHP 열기': {
      EN: 'Open PHP',
      JP: 'PHPを開く',
      CH: '打开 PHP',
      TH: 'เปิด PHP'
    },
    '샘플 패키지': {
      EN: 'Sample package',
      JP: 'サンプルパッケージ',
      CH: '示例包',
      TH: 'แพ็กเกจตัวอย่าง'
    },
    'merchant-api-samples 전체 목록': {
      EN: 'Full merchant-api-samples index',
      JP: 'merchant-api-samples 一覧',
      CH: 'merchant-api-samples 完整列表',
      TH: 'ดัชนี merchant-api-samples ทั้งหมด'
    },
    '목록 열기': {
      EN: 'Open index',
      JP: '一覧を開く',
      CH: '打开列表',
      TH: 'เปิดดัชนี'
    },
    '가맹 compId·엔드포인트·curl 예시(JSON)': {
      EN: 'Merchant compId, endpoints & curl examples (JSON)',
      JP: '加盟店 compId・エンドポイント・curl 例(JSON)',
      CH: '商户 compId、端点与 curl 示例（JSON）',
      TH: 'compId ร้าน, endpoints และตัวอย่าง curl (JSON)'
    },
    'JSON 다운로드': {
      EN: 'Download JSON',
      JP: 'JSONダウンロード',
      CH: '下载 JSON',
      TH: 'ดาวน์โหลด JSON'
    },
    'PHP 설정 템플릿·파일 경로(PHP)': {
      EN: 'PHP config template & file paths',
      JP: 'PHP 設定テンプレート・ファイルパス',
      CH: 'PHP 配置模板与文件路径',
      TH: 'เทมเพลต config PHP และ path ไฟล์'
    },
    'PHP 설정 다운로드': {
      EN: 'Download PHP config',
      JP: 'PHP設定ダウンロード',
      CH: '下载 PHP 配置',
      TH: 'ดาวน์โหลด config PHP'
    },
    '문서 조회 실패': {
      EN: 'Failed to load documents',
      JP: 'ドキュメント取得失敗',
      CH: '文档加载失败',
      TH: 'โหลดเอกสารไม่สำเร็จ'
    },
    '다운로드 실패': {
      EN: 'Download failed',
      JP: 'ダウンロード失敗',
      CH: '下载失败',
      TH: 'ดาวน์โหลดไม่สำเร็จ'
    },
    '다운로드': {
      EN: 'Download',
      JP: 'ダウンロード',
      CH: '下载',
      TH: 'ดาวน์โหลด'
    },
    '열기': {
      EN: 'Open',
      JP: '開く',
      CH: '打开',
      TH: 'เปิด'
    },
    'JSON 경로': {
      EN: 'JSON path',
      JP: 'JSONパス',
      CH: 'JSON 路径',
      TH: 'JSON path'
    },
    '타입': {
      EN: 'Type',
      JP: '型',
      CH: '类型',
      TH: 'ชนิด'
    },
    '최대길이': {
      EN: 'Max length',
      JP: '最大長',
      CH: '最大长度',
      TH: 'ความยาวสูงสุด'
    },
    '예시값': {
      EN: 'Example value',
      JP: '例示値',
      CH: '示例值',
      TH: 'ค่าตัวอย่าง'
    },
    '필수(M)': {
      EN: 'Required (M)',
      JP: '必須(M)',
      CH: '必填(M)',
      TH: 'จำเป็น (M)'
    },
    '선택(O)': {
      EN: 'Optional (O)',
      JP: '任意(O)',
      CH: '可选(O)',
      TH: 'ไม่บังคับ (O)'
    },
    '조건부(C)': {
      EN: 'Conditional (C)',
      JP: '条件付(C)',
      CH: '条件(C)',
      TH: 'ตามเงื่อนไข (C)'
    },
    Prepare: {
      EN: 'Prepare',
      JP: 'Prepare',
      CH: 'Prepare',
      TH: 'Prepare'
    },
    Session: {
      EN: 'Session',
      JP: 'Session',
      CH: 'Session',
      TH: 'Session'
    },
    Status: {
      EN: 'Status',
      JP: 'Status',
      CH: 'Status',
      TH: 'Status'
    },
    'Embed 스크립트': {
      EN: 'Embed script',
      JP: 'Embed スクリプト',
      CH: 'Embed 脚本',
      TH: 'สคริปต์ Embed'
    },
    '업체를 고른 뒤 「연동 패키지 생성(JSON)」을 누르면 요약과 JSON이 채워집니다.': {
      EN: 'Pick a merchant and click “Build integration package (JSON)” to fill the summary and JSON.',
      JP: '加盟店を選び「連携パッケージ生成(JSON)」を押すと、要約とJSONが入ります。',
      CH: '选择商户后点击「生成对接包（JSON）」将填入摘要与 JSON。',
      TH: 'เลือกร้านแล้วกดสร้างแพ็กเกจเชื่อม (JSON) เพื่อเติมสรุปและ JSON'
    },
    선택: {
      EN: 'Select',
      JP: '選択',
      CH: '选择',
      TH: 'เลือก'
    },
    업체코드: {
      EN: 'Company code',
      JP: '加盟店コード',
      CH: '商户代码',
      TH: 'รหัสร้าน'
    },
    업체명: {
      EN: 'Company name',
      JP: '加盟店名',
      CH: '商户名称',
      TH: 'ชื่อร้าน'
    },
    '조회된 가맹점이 없습니다.': {
      EN: 'No merchants found.',
      JP: '該当する加盟店がありません。',
      CH: '未找到商户。',
      TH: 'ไม่พบร้านค้า'
    },
    '페이지 {P} / {TP} (총 {TE}건)': {
      EN: 'Page {P} / {TP} ({TE} total)',
      JP: 'ページ {P} / {TP}（全{TE}件）',
      CH: '第 {P} / {TP} 页（共 {TE} 条）',
      TH: 'หน้า {P} / {TP} (ทั้งหมด {TE} รายการ)'
    },
    활성: {
      EN: 'Active',
      JP: '有効',
      CH: '启用',
      TH: 'ใช้งาน'
    },
    '가맹 PG 바인딩이 없습니다. 업체정보에서 결제대행사를 저장하세요.': {
      EN: 'No merchant PG bindings. Save an acquirer in merchant detail.',
      JP: '加盟店PGバインドがありません。「加盟店情報」で決済代行を保存してください。',
      CH: '没有商户 PG 绑定。请在商户信息中保存支付机构。',
      TH: 'ไม่มีการผูก PG ของร้าน บันทึกผู้ให้บริการชำระในข้อมูลร้าน'
    },
    '공개 API 베이스:': {
      EN: 'Public API base:',
      JP: '公開APIベース:',
      CH: '公开 API 基址：',
      TH: 'Public API base:'
    },
    '기준통화:': {
      EN: 'Base currency:',
      JP: '基準通貨:',
      CH: '基准货币：',
      TH: 'สกุลเงินฐาน:'
    },
    '채널:': {
      EN: 'Channel:',
      JP: 'チャネル:',
      CH: '渠道：',
      TH: 'ช่องทาง:'
    },
    'IN=INLINE · RE=REDIRECT · WO=WordPress': {
      EN: 'IN=INLINE · RE=REDIRECT · WO=WordPress',
      JP: 'IN=INLINE · RE=REDIRECT · WO=WordPress',
      CH: 'IN=INLINE · RE=REDIRECT · WO=WordPress',
      TH: 'IN=INLINE · RE=REDIRECT · WO=WordPress'
    },
    '가맹 PG 바인딩': {
      EN: 'Merchant PG bindings',
      JP: '加盟店PGバインド',
      CH: '商户 PG 绑定',
      TH: 'การผูก PG ของร้าน'
    },
    '아래 JSON 전체를 복사해 가맹·개발 담당에게 전달하세요.': {
      EN: 'Copy the entire JSON below and send it to merchant and development contacts.',
      JP: '下のJSON全体をコピーし、加盟店・開発担当へ渡してください。',
      CH: '请复制下方完整 JSON 发给商户与开发负责人。',
      TH: 'คัดลอก JSON ทั้งหมดด้านล่างส่งให้ร้านและทีมพัฒนา'
    },
    '업체코드를 입력하거나 목록에서 선택하세요.': {
      EN: 'Enter a company code or pick one from the list.',
      JP: '加盟店コードを入力するか、一覧から選択してください。',
      CH: '请输入商户代码或从列表中选择。',
      TH: 'กรอกรหัสร้านหรือเลือกจากรายการ'
    },
    '업체코드를 입력하세요.': {
      EN: 'Enter a company code.',
      JP: '加盟店コードを入力してください。',
      CH: '请输入商户代码。',
      TH: 'กรุณากรอกรหัสร้าน'
    },
    '키트 조회 실패': {
      EN: 'Failed to load kit',
      JP: 'キットの取得に失敗しました',
      CH: '套件加载失败',
      TH: 'โหลดชุดไม่สำเร็จ'
    },
    '전체 PG(문서용)': {
      EN: 'All PGs (documentation)',
      JP: '全PG（ドキュメント用）',
      CH: '全部 PG（文档用）',
      TH: 'PG ทั้งหมด (สำหรับเอกสาร)'
    },
    '기존 브로커 시크릿을 폐기하고 새 시크릿을 발급합니다. 계속할까요?': {
      EN: 'The existing broker secret will be revoked and a new one issued. Continue?',
      JP: '既存のブローカーシークレットを失効し、新しいシークレットを発行します。続行しますか？',
      CH: '将作废现有 broker 密钥并签发新的。是否继续？',
      TH: 'ยกเลิก secret เดิมและออกใหม่ ดำเนินต่อหรือไม่'
    },
    '가맹점 서버에 이미 배포된 키는 즉시 무효가 됩니다. 정말 진행할까요?': {
      EN: 'Keys already deployed on merchant servers become invalid immediately. Really proceed?',
      JP: '加盟店サーバーに既に配布済みのキーは直ちに無効になります。本当に進めますか？',
      CH: '已部署在商户服务器上的密钥将立即失效。确定继续？',
      TH: 'คีย์ที่ติดตั้งบนเซิร์ฟเวอร์ร้านจะใช้ไม่ได้ทันที ยืนยันหรือไม่'
    },
    '발급되었습니다. JSON에 brokerSecretPlain 이 표시됩니다.': {
      EN: 'Issued. brokerSecretPlain appears in the JSON.',
      JP: '発行しました。JSONに brokerSecretPlain が表示されます。',
      CH: '已签发。JSON 中会显示 brokerSecretPlain。',
      TH: 'ออกแล้ว จะเห็น brokerSecretPlain ใน JSON'
    },
    '발급 실패': {
      EN: 'Issue failed',
      JP: '発行に失敗しました',
      CH: '签发失败',
      TH: 'ออกไม่สำเร็จ'
    },
    '강제 여부가 저장되었습니다: {YN}': {
      EN: 'Enforce flag saved: {YN}',
      JP: '強制フラグを保存しました: {YN}',
      CH: '已保存强制标志：{YN}',
      TH: 'บันทึกการบังคับแล้ว: {YN}'
    },
    MID: { EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' },
    /* /hq/apiMerchantDeployReg */
    '1. API 가맹점 등록': {
      EN: '1. Register API merchants',
      JP: '1. API加盟店登録',
      CH: '1. API 商户注册',
      TH: '1. ลงทะเบียนร้าน API'
    },
    순서: {
      EN: 'Steps',
      JP: '手順',
      CH: '步骤',
      TH: 'ขั้นตอน'
    },
    'API연동설정에서 PG사 연동 추가 후, 연동용도에 API를 켭니다.': {
      EN: 'In API integration settings, add a PG linkage, then turn on API in the integration scope.',
      JP: '「API連携設定」でPG連携を追加し、連携用途でAPIを有効にします。',
      CH: '在「API 联动设置」中添加 PG 对接后，在对接用途中开启 API。',
      TH: 'ใน「การเชื่อม API」ให้เพิ่มการเชื่อม PG แล้วเปิด API ในขอบเขตการเชื่อม'
    },
    '업체등록에서 조직을 가맹점으로 등록하고, 결제대행사에서 아래 목록의 PG 중 하나를 선택·저장합니다.': {
      EN: 'In merchant registration, register the organization as a merchant, then pick one of the PGs below as acquirer and save.',
      JP: '「加盟店登録」で組織を加盟店として登録し、決済代行として下の一覧のPGのいずれかを選択して保存します。',
      CH: '在「商户注册」中将组织注册为商户，并在支付机构中从下列 PG 中选择一个保存。',
      TH: 'ใน「ลงทะเบียนร้าน」ให้ลงทะเบียนองค์กรเป็นร้านค้า แล้วเลือก PG จากรายการด้านล่างเป็นผู้ให้บริการชำระและบันทึก'
    },
    '콜백·결과 URL은 업체정보 또는 통보관리 메뉴에서 등록합니다.': {
      EN: 'Register callback and result URLs in merchant detail or notification management.',
      JP: 'コールバック・結果URLは「加盟店情報」または「通知管理」で登録します。',
      CH: '回调与结果 URL 请在「商户详情」或「通知管理」中登记。',
      TH: 'ลงทะเบียน URL แจ้งผลและ callback ได้ที่เมนูข้อมูลร้านหรือการแจ้งเตือน'
    },
    '등록이 끝나면 2. 가맹점 API 생성에서 MID·엔드포인트·연동 JSON을 발급합니다.': {
      EN: 'When done, use “2. Generate merchant API” to issue MID, endpoints, and integration JSON.',
      JP: '登録後は「2. 加盟店API生成」でMID・エンドポイント・連携JSONを発行します。',
      CH: '完成后在「2. 生成商户 API」中签发 MID、端点与对接 JSON。',
      TH: 'เมื่อครบแล้ว ใช้「2. สร้าง Merchant API」เพื่อออก MID เอนด์พอยต์และ JSON การเชื่อม'
    },
    '이 표는 API연동설정 DB를 읽어, 연동용도에 API가 포함된 행만 보여 줍니다.': {
      EN: 'This table reads the API integration settings DB and shows only rows whose scope includes API.',
      JP: 'この表はAPI連携設定DBを参照し、連携用途にAPIが含まれる行のみ表示します。',
      CH: '本表读取 API 联动设置数据库，仅显示对接用途中包含 API 的行。',
      TH: 'ตารางนี้อ่านจาก DB การตั้งค่าเชื่อม API และแสดงเฉพาะแถวที่ขอบเขตมี API'
    },
    API연동설정: {
      EN: 'API integration',
      JP: 'API連携設定',
      CH: 'API 联动设置',
      TH: 'การเชื่อม API'
    },
    업체등록: {
      EN: 'Register merchant',
      JP: '加盟店登録',
      CH: '商户注册',
      TH: 'ลงทะเบียนร้าน'
    },
    업체관리: {
      EN: 'Merchant tree',
      JP: '加盟店管理',
      CH: '商户管理',
      TH: 'จัดการร้านค้า'
    },
    '2. 가맹점 API 생성': {
      EN: '2. Generate merchant API',
      JP: '2. 加盟店API生成',
      CH: '2. 生成商户 API',
      TH: '2. สร้าง Merchant API'
    },
    '연동용도에 API가 켜진 결제대행사': {
      EN: 'Acquirers with API enabled in integration scope',
      JP: '連携用途でAPIが有効な決済代行',
      CH: '对接用途中已开启 API 的支付机构',
      TH: 'ผู้ให้บริการชำระที่เปิด API ในขอบเขตการเชื่อม'
    },
    결제대행사명: {
      EN: 'Acquirer name',
      JP: '決済代行名',
      CH: '支付机构名称',
      TH: 'ชื่อผู้ให้บริการชำระ'
    },
    '본사 MID': {
      EN: 'HQ MID',
      JP: '本社MID',
      CH: '总部 MID',
      TH: 'MID สำนักงานใหญ่'
    },
    본사운영: {
      EN: 'HQ operational',
      JP: '本社運用',
      CH: '总部运营',
      TH: 'การทำงานสำนักงานใหญ่'
    },
    '가맹 전용 MID·키는 업체 저장 시 가맹 바인딩에 들어갑니다. 본사 행과 다를 수 있습니다.': {
      EN: 'Merchant-specific MID and keys go into merchant binding when the company is saved; they may differ from HQ rows.',
      JP: '加盟店専用のMID・キーは加盟店保存時に加盟店バインドへ入ります。本社行と異なる場合があります。',
      CH: '商户专用 MID 与密钥在保存商户时写入商户绑定，可能与总部行不同。',
      TH: 'MID/คีย์เฉพาะร้านจะถูกผูกตอนบันทึกร้าน อาจต่างจากแถวสำนักงานใหญ่'
    },
    'API연동설정에서 연동용도에 API를 켠 결제대행사가 없습니다. 먼저 PG사 연동 추가 후 저장하세요.': {
      EN: 'No acquirer has API enabled in integration scope. Add a PG linkage in API integration settings first, then save.',
      JP: '連携用途でAPIを有効にした決済代行がありません。先に「API連携設定」でPG連携を追加して保存してください。',
      CH: '没有在对接用途中开启 API 的支付机构。请先在「API 联动设置」中添加 PG 对接并保存。',
      TH: 'ไม่มีผู้ให้บริการชำระที่เปิด API ในขอบเขต ให้เพิ่มการเชื่อม PG ในการตั้งค่าเชื่อม API ก่อนแล้วบันทึก'
    },
    '목록 조회 실패': {
      EN: 'Failed to load list',
      JP: '一覧の取得に失敗しました',
      CH: '列表加载失败',
      TH: 'โหลดรายการไม่สำเร็จ'
    },
    '메뉴 이동을 초기화하지 못했습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.': {
      EN: 'Menu navigation could not be initialized. Refresh the page and try again.',
      JP: 'メニュー遷移を初期化できませんでした。ページを更新してから再度お試しください。',
      CH: '无法初始化菜单跳转。请刷新页面后重试。',
      TH: 'เริ่มการนำทางเมนูไม่สำเร็จ โปรดรีเฟรชหน้าแล้วลองอีกครั้ง'
    },
    /* HQ /hq/settlementAdmin — 精算管理設定 */
    '정산관리 안내': {
      EN: 'Settlement management guide',
      JP: '精算管理の案内',
      CH: '结算管理说明',
      TH: 'คำแนะนำการจัดการชำระบัญชี'
    },
    '배치·수동 정산은 가맹 정산주기·AUTO·마감과 동일합니다. 표는 정산일과 집계기간(from~to), 자동가맹 수 요약입니다.': {
      EN: 'Batch and manual settlement follow the same rules as merchant settlement cycle, AUTO, and cutoff. The tables summarize settlement date, aggregation period (from–to), and AUTO merchant counts.',
      JP: 'バッチ・手動精算は加盟店の精算サイクル・AUTO・締めと同じです。表は精算日・集計期間(from~to)・AUTO加盟店数の要約です。',
      CH: '批量与手动结算与商户结算周期、AUTO、截止时间一致。表格汇总结算日、汇总区间(from~to)与自动商户数量。',
      TH: 'แบตช์และชำระด้วยมือใช้กฎเดียวกับรอบชำระ AUTO และปิดรอบของร้าน ตารางสรุปวันช่วงรวมและจำนวนร้าน AUTO'
    },
    'D+N · W+N: 일·주 단위, 실행마다 1건.': {
      EN: 'D+N / W+N: daily/weekly; one settlement row per run.',
      JP: 'D+N・W+N: 日/週単位、実行ごとに1件。',
      CH: 'D+N / W+N：按日/周，每次执行一行。',
      TH: 'D+N / W+N: รายวัน/สัปดาห์ หนึ่งแถวต่อรัน'
    },
    'WK: 주(또는 격주) 마감 뒤 영업일 3·10·30일째 등, 1건.': {
      EN: 'WK: after weekly/biweekly close, one row on the Nth business day (e.g. 3/10/30), etc.',
      JP: 'WK: 週(隔週)締め後の営業日3・10・30日目など、1件。',
      CH: 'WK：周/隔周截止后，在第 N 个营业日等生成一行。',
      TH: 'WK: หลังปิดรายสัปดาห์/คู่สัปดาห์ หนึ่งแถวตามวันทำการที่ N'
    },
    'RT: 건별. T0 · TM · TH: 당일 합산 갱신.': {
      EN: 'RT: per transaction. T0 / TM / TH: intraday totals refresh.',
      JP: 'RT: 件別。T0・TM・TH: 当日合算の更新。',
      CH: 'RT：逐笔。T0/TM/TH：当日汇总刷新。',
      TH: 'RT: รายธุรกรรม T0/TM/TH รีเฟรชยอดรายวัน'
    },
    'M5·M10·M30: 분마다. H1~H12: 시간마다(예: H1 하루 24회).': {
      EN: 'M5/M10/M30: every minute. H1–H12: hourly (e.g. H1 = 24 runs per day).',
      JP: 'M5・M10・M30: 分ごと。H1~H12: 時間ごと(例: H1 は1日24回)。',
      CH: 'M5/M10/M30：每分钟。H1–H12：每小时（如 H1 一天 24 次）。',
      TH: 'M5/M10/M30: ทุกนาที H1–H12: ทุกชม. (เช่น H1 = 24 ครั้ง/วัน)'
    },
    '무효·환불 정산(본사 기본·총판별)은 본사설정 → 환수/미수금설정에서 설정합니다.': {
      EN: 'Void/refund settlement (HQ default / per distributor) is configured under HQ settings → receivables & recovery.',
      JP: '無効・返金精算（本社デフォルト・総販別）は本社設定→回収/未収金設定で行います。',
      CH: '无效/退款结算（总部默认/总代）在总部设置→回款/应收中配置。',
      TH: 'ชำระโมฆะ/คืนเงิน (ค่าเริ่ม HQ/ตามตัวแทน) ตั้งที่ HQ → ลูกหนี้/กู้คืน'
    },
    '자동 정산 배치 (총 스위치)': {
      EN: 'Automatic settlement batch (master switch)',
      JP: '自動精算バッチ（総スイッチ）',
      CH: '自动结算批处理（总开关）',
      TH: 'แบตช์ชำระอัตโนมัติ (สวิตช์หลัก)'
    },
    '가맹 정산구분 AUTO·정산주기와는 별개입니다. ① 서버 타이머가 켜져 있고 ② 본사 DB 모드가 허용일 때만 스케줄 tick 이 본문을 실행합니다. RT 건별 정산은 이 스위치와 무관합니다.': {
      EN: 'Independent of merchant settlement type AUTO and settlement cycle. The scheduled tick runs body only when ① the server timer is on and ② HQ DB mode allows it. RT per-txn settlement ignores this switch.',
      JP: '加盟店の精算区分AUTO・精算サイクルとは別です。①サーバータイマーがオンかつ②本社DBモードが許可のときだけスケジュールtickが本体を実行します。RT件別精算はこのスイッチと無関係です。',
      CH: '与商户结算类型 AUTO 及结算周期无关。仅当①服务器定时器开启且②总部数据库模式允许时，调度 tick 才执行主体。RT 逐笔结算不受此开关影响。',
      TH: 'แยกจาก AUTO และรอบชำระร้าน tick ทำงานเมื่อ①ตัวจับเวลาเซิร์ฟเวอร์เปิดและ②โหมด DB HQ อนุญาต ชำระ RT รายรายการไม่ขึ้นกับสวิตช์นี้'
    },
    '① 서버(Java) — 서버가 시작될 때 읽는 설정으로, 주기적으로 정산을 돌릴 타이머를 켤지 말지 정합니다. 이 관리자 화면에서는 상태만 표시합니다.': {
      EN: '① Server (Java) — startup config decides whether the periodic settlement timer is enabled. This admin screen shows status only.',
      JP: '①サーバ(Java) — 起動時に読む設定で、定期的に精算を回すタイマーをオン/オフにします。この管理画面では状態のみ表示します。',
      CH: '① 服务器（Java）— 启动时读取的配置决定是否启用周期性结算定时器。本管理端仅显示状态。',
      TH: '① เซิร์ฟเวอร์ (Java) — ตั้งค่าตอนสตาร์ทว่าเปิดตัวจับเวลาชำระหรือไม่ หน้านี้แสดงสถานะอย่างเดียว'
    },
    '② 본사 DB — 활성(항상 tick 본문 시도)·비활성(tick 본문 끔)·자동(이번 주기에 돌릴 AUTO 가맹이 있을 때만) 중 하나를 저장합니다.': {
      EN: '② HQ DB — save one of Active (always try tick body), Inactive (tick body off), or Auto (only when AUTO merchants are due this tick).',
      JP: '②本社DB — 活性(常にtick本体を試行)・非活性(tick本体オフ)・自動(今回の周期に回すAUTO加盟店があるときのみ)のいずれかを保存します。',
      CH: '② 总部数据库—保存为：启用（总是尝试 tick 主体）、停用（关闭 tick 主体）或自动（仅当本周期有需执行的 AUTO 商户）。',
      TH: '② DB HQ — บันทึก เปิดใช้ / ปิด / อัตโนมัติ (เฉพาะเมื่อมีร้าน AUTO ในช่วงนี้)'
    },
    '① 서버 — 자동 정산 타이머': {
      EN: '① Server — auto settlement timer',
      JP: '①サーバ — 自動精算タイマー',
      CH: '① 服务器 — 自动结算定时器',
      TH: '① เซิร์ฟเวอร์ — ตัวจับเวลาชำระอัตโนมัติ'
    },
    '확인 중…': {
      EN: 'Checking…',
      JP: '確認中…',
      CH: '检查中…',
      TH: 'กำลังตรวจสอบ…'
    },
    '서버에서 응답을 불러오는 중입니다.': {
      EN: 'Loading response from server…',
      JP: 'サーバーから応答を読み込んでいます。',
      CH: '正在从服务器加载响应…',
      TH: 'กำลังโหลดจากเซิร์ฟเวอร์…'
    },
    '② 본사 DB — 배치 모드': {
      EN: '② HQ DB — batch mode',
      JP: '②本社DB — バッチモード',
      CH: '② 总部数据库 — 批处理模式',
      TH: '② DB HQ — โหมดแบตช์'
    },
    '활성 (항상)': {
      EN: 'Active (always)',
      JP: '有効（常時）',
      CH: '启用（始终）',
      TH: 'เปิดใช้ (เสมอ)'
    },
    '자동 (대상 있을 때만)': {
      EN: 'Auto (only when targets exist)',
      JP: '自動（対象があるときのみ）',
      CH: '自动（仅当有对象时）',
      TH: 'อัตโนมัติ (เมื่อมีเป้าหมาย)'
    },
    '비활성': {
      EN: 'Inactive',
      JP: '無効',
      CH: '停用',
      TH: 'ปิดใช้งาน'
    },
    '②가 비활성이면 ①이 켜져 있어도 tick 본문은 실행되지 않습니다. 자동은 이번 주기에 실행할 AUTO 가맹이 없으면 스킵합니다.': {
      EN: 'If ② is inactive, tick body does not run even when ① is on. In Auto mode, the tick is skipped when no AUTO merchant is due this cycle.',
      JP: '②が無効なら①がオンでもtick本体は実行されません。自動は今回の周期に実行するAUTO加盟店がなければスキップします。',
      CH: '若②停用，即使①开启也不会执行 tick 主体。自动模式下本周期无待执行 AUTO 商户则跳过。',
      TH: 'ถ้า②ปิด แม้①เปิดก็ไม่รัน tick โหมดอัตโนมัติข้ามเมื่อไม่มีร้าน AUTO ในช่วงนี้'
    },
    '현재 자동 배치': {
      EN: 'Current auto batch',
      JP: '現在の自動バッチ',
      CH: '当前自动批处理',
      TH: 'แบตช์อัตโนมัติปัจจุบัน'
    },
    '총판별 기준 영업일 및 정산 크론 기준': {
      EN: 'Per-distributor business day & settlement cron basis',
      JP: '総販別の基準営業日および精算クロン基準',
      CH: '按总代的基准营业日与结算 cron 基准',
      TH: 'วันทำการและฐาน cron ชำระตามตัวแทนหลัก'
    },
    '현재영업일 열은 총판 업체등록 시 저장된 영업일·휴일(프로필명·기준국가 등)을 보여 주며, 비어 있으면 상위 본사(REGIONAL) 설정을 참고해 표시할 수 있습니다. 거래시간(1줄)은 결제·통합내역 그리드의 첫 번째 시각 줄입니다. 정산 크론(2줄)은 격자·마감·D0 및 두 번째 시각 줄에 쓰는 Zone입니다. 셀렉트만 바꿔서는 저장되지 않습니다. 행의 저장을 눌러 주세요.': {
      EN: 'Current business day shows holidays saved at distributor registration (profile, country, etc.); if empty, HQ (REGIONAL) may apply. Transaction time (line 1) is the first time row in pay/integrated grids. Settlement cron (line 2) is the Zone for grid/cutoff/D0 and the second time row. Changing selects alone does not save—press Save on the row.',
      JP: '現在営業日列は総販登録時の営業日・休日(プロファイル名・基準国など)を表示し、空なら上位本社(REGIONAL)を参照できます。取引時間(1行目)は決済・統合明細グリッドの最初の時刻行です。精算クロン(2行目)は格子・締め・D0および2行目の時刻に使うZoneです。セレクトだけでは保存されません。行の保存を押してください。',
      CH: '当前营业日列显示总代注册时保存的营业日/假日（档案名、基准国等）；为空时可参考上级总部(REGIONAL)。交易时间（第 1 行）为支付/综合明细表的首行时间。结算 cron（第 2 行）用于网格/截止/D0 及第二行时间。仅改下拉不会保存，请点行内保存。',
      TH: 'วันทำการปัจจุบันแสดงวันหยุดจากการลงทะเบียนตัวแทน ว่างอ้าง HQ เวลาธุรกรรม (บรรทัด 1) และ cron (บรรทัด 2) ตามโซน เปลี่ยน select อย่างเดียวไม่บันทึก กดบันทึกในแถว'
    },
    '저장 후 서버 값을 다시 보려면 새로고침을 누르세요.': {
      EN: 'Press refresh to reload server values after saving.',
      JP: '保存後にサーバー値を再表示するには更新を押してください。',
      CH: '保存后请点击刷新以重新加载服务器值。',
      TH: 'หลังบันทึกกดรีเฟรชเพื่อโหลดค่าจากเซิร์ฟเวอร์อีกครั้ง'
    },
    '거래시간(1줄)': {
      EN: 'Txn time (line 1)',
      JP: '取引時間(1行目)',
      CH: '交易时间（第 1 行）',
      TH: 'เวลาธุรกรรม (บรรทัด 1)'
    },
    '정산 크론(2줄)': {
      EN: 'Settlement cron (line 2)',
      JP: '精算クロン(2行目)',
      CH: '结算 cron（第 2 行）',
      TH: 'cron ชำระ (บรรทัด 2)'
    },
    '현재영업일': {
      EN: 'Current business day',
      JP: '現在営業日',
      CH: '当前营业日',
      TH: 'วันทำการปัจจุบัน'
    },
    '총판별 가맹 정산주기 (최대 10건·대표)': {
      EN: 'Merchant settlement cycles per distributor (up to 10, default)',
      JP: '総販別加盟店精算サイクル（最大10件・代表）',
      CH: '按总代的商户结算周期（最多 10 项·默认）',
      TH: 'รอบชำระร้านตามตัวแทน (สูงสุด 10 · ค่าเริ่ม)'
    },
    '총판(MASTER_DIST)마다 가맹점 등록 시 선택 가능한 정산주기를 최대 10개까지 지정합니다(2개·5개처럼 일부만 채워도 됩니다). 서로 다른 주기는 최소 2개 필요하며, 대표는 신규 가맹 시 셀렉트 기본값입니다. 아래 슬롯 셀렉트는 본사 표준 병합 전체(미사용 N 포함)이며, 코드·행 순서는 위 정산주기관리의 표준 주기(시스템)·DB등록 표와 동일합니다. 미설정 총판이거나 상위에 총판이 없으면 가맹 화면은 기존처럼 사용(Y)만 노출됩니다.': {
      EN: 'Per MASTER_DIST, up to 10 selectable settlement cycles at merchant registration (partial slots OK). At least two distinct cycles are required; the default slot is the merchant form default. Slot options are the HQ merged catalog (including inactive N); code/order matches the standard/system and DB tables above. If unset or no parent distributor, the merchant UI shows only active (Y) as before.',
      JP: 'MASTER_DISTごとに加盟店登録で選べる精算サイクルを最大10件指定します（2件・5件など一部のみでも可）。異なる周期は最低2件必要で、代表は新規加盟店のセレクト既定です。下のスロットは本社標準マージ全体（未使用N含む）で、コード・行順は上の精算サイクル管理の標準(システム)・DB登録表と同じです。未設定または上位に総販がない場合は加盟店画面は従来どおり使用(Y)のみ表示されます。',
      CH: '每个 MASTER_DIST 在商户注册时最多可选 10 个结算周期（可只填部分）。至少需 2 个不同周期；代表项为新建商户下拉默认值。下方槽位来自总部标准合并（含未用 N），代码与行序与上方标准/系统与 DB 表一致。未配置或无上级总代时，商户端仍仅显示启用(Y)。',
      TH: 'ต่อ MASTER_DIST เลือกได้สูงสุด 10 รอบตอนลงทะเบียนร้าน ต้องมีอย่างน้อย 2 รอบที่ต่างกัน ค่าเริ่มคือช่องตัวแทน รายการรวม HQ รวม N สายโค้ดตรงกับตารางด้านบน ถ้าไม่ตั้งหรือไม่มีตัวแทน แสดงเฉพาะ Y เหมือนเดิม'
    },
    '정산주기': {
      EN: 'Settlement cycle',
      JP: '精算サイクル',
      CH: '结算周期',
      TH: 'รอบชำระ'
    },
    '정산주기관리 (DB 등록)': {
      EN: 'Settlement cycle management (DB)',
      JP: '精算サイクル管理（DB登録）',
      CH: '结算周期管理（数据库）',
      TH: 'จัดการรอบชำระ (DB)'
    },
    'D+N (일)': {
      EN: 'D+N (days)',
      JP: 'D+N（日）',
      CH: 'D+N（日）',
      TH: 'D+N (วัน)'
    },
    'W+N (주)': {
      EN: 'W+N (weeks)',
      JP: 'W+N（週）',
      CH: 'W+N（周）',
      TH: 'W+N (สัปดาห์)'
    },
    'WK 코드': {
      EN: 'WK code',
      JP: 'WKコード',
      CH: 'WK 代码',
      TH: 'รหัส WK'
    },
    '예: 12': {
      EN: 'e.g. 12',
      JP: '例: 12',
      CH: '例：12',
      TH: 'เช่น 12'
    },
    '예: D+12': {
      EN: 'e.g. D+12',
      JP: '例: D+12',
      CH: '例：D+12',
      TH: 'เช่น D+12'
    },
    '내부 안내용': {
      EN: 'Internal note',
      JP: '内部メモ',
      CH: '内部说明',
      TH: 'หมายเหตุภายใน'
    },
    '내장 표준 코드가 DB에 없을 때만 삽입합니다': {
      EN: 'Inserts only built-in standard codes missing from the DB',
      JP: '内蔵標準コードがDBにないときのみ挿入します',
      CH: '仅当内置标准代码在数据库中不存在时插入',
      TH: 'แทรกเฉพาะรหัสมาตรฐานในตัวที่ยังไม่มีใน DB'
    },
    '표준주기 DB복원': {
      EN: 'Restore standard cycles to DB',
      JP: '標準周期をDBに復元',
      CH: '将标准周期恢复到数据库',
      TH: 'คืนค่ารอบมาตรฐานลง DB'
    },
    '표준 주기(시스템) — 설명은 DB 행으로 덮어쓸 수 있습니다. DB가 비었을 때는 표준주기 DB복원으로 내장 목록과 동일한 행을 한 번에 넣을 수 있습니다.': {
      EN: 'Standard cycles (system)—descriptions can be overridden by DB rows. When the DB is empty, Restore standard cycles inserts the built-in list in one step.',
      JP: '標準周期(システム) — 説明はDB行で上書きできます。DBが空のときは「標準周期をDBに復元」で内蔵一覧と同じ行を一括投入できます。',
      CH: '标准周期（系统）—说明可被数据库行覆盖。数据库为空时可用「恢复标准周期」一次性写入内置列表。',
      TH: 'รอบมาตรฐาน (ระบบ) — คำอธิบายแก้ใน DB ได้ ถ้า DB ว่างใช้ปุ่มคืนค่าเพื่อใส่รายการในตัว'
    },
    'DB 등록 주기 — 저장·삭제(본사·관리자만)': {
      EN: 'DB-registered cycles — save/delete (HQ admin only)',
      JP: 'DB登録周期 — 保存・削除（本社・管理者のみ）',
      CH: '数据库登记周期 — 保存/删除（仅总部管理员）',
      TH: 'รอบที่ลง DB — บันทึก/ลบ (แอดมิน HQ เท่านั้น)'
    },
    'RT·T0 및 TM·TH(당일 누적 재집계)': {
      EN: 'RT/T0 and TM/TH (intraday cumulative re-aggregation)',
      JP: 'RT・T0およびTM・TH（当日累積の再集計）',
      CH: 'RT/T0 与 TM/TH（当日累计再汇总）',
      TH: 'RT/T0 และ TM/TH (รวมยอดรายวันใหม่)'
    },
    '설명': {
      EN: 'Description',
      JP: '説明',
      CH: '说明',
      TH: 'คำอธิบาย'
    },
    '방식': {
      EN: 'Method',
      JP: '方式',
      CH: '方式',
      TH: 'รูปแบบ'
    },
    '가맹 결제 연동: 웹결제(Y) 및 브로커 시크릿 발급 시 API, 미발급 시 URL': {
      EN: 'Merchant integration: API when web payment (Y) and broker secret are active; URL otherwise.',
      JP: '加盟店連携: ウェブ決済(Y)かつブローカーシークレット発行時はAPI、未発行時はURL。',
      CH: '商户联动：网页支付(Y)且已发放 broker 密钥时为 API，否则为 URL。',
      TH: 'การเชื่อมร้าน: API เมื่อเปิดเว็บชำระ (Y) และมี broker secret มิฉะนั้น URL'
    },
    '가맹 API 연동 채널: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce. 복수 사용 시 IN/RE 형식.': {
      EN: 'Merchant API channels: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce. Multiple: IN/RE format.',
      JP: '加盟店APIチャネル: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce。複数は IN/RE 形式。',
      CH: '商户 API 渠道：IN=INLINE，RE=REDIRECT，WO=WordPress/WooCommerce。多个时为 IN/RE 格式。',
      TH: 'ช่อง Merchant API: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce หลายช่องแสดง IN/RE'
    },
    '순서': {
      EN: 'Order',
      JP: '順序',
      CH: '顺序',
      TH: 'ลำดับ'
    },
    '항목ID': {
      EN: 'Item ID',
      JP: '項目ID',
      CH: '项目 ID',
      TH: 'รหัสรายการ'
    },
    '항목명': {
      EN: 'Item name',
      JP: '項目名',
      CH: '项目名称',
      TH: 'ชื่อรายการ'
    },
    '표시여부': {
      EN: 'Visible',
      JP: '表示有無',
      CH: '是否显示',
      TH: 'แสดงหรือไม่'
    },
    '표시명': {
      EN: 'Display name',
      JP: '表示名',
      CH: '显示名称',
      TH: 'ชื่อที่แสดง'
    },
    '자동가맹': {
      EN: 'AUTO merchants',
      JP: 'AUTO加盟店',
      CH: '自动商户',
      TH: 'ร้าน AUTO'
    },
    '정산일정 미리보기': {
      EN: 'Settlement schedule preview',
      JP: '精算スケジュールプレビュー',
      CH: '结算日程预览',
      TH: 'ดูตัวอย่างตารางชำระ'
    },
    '시작일': {
      EN: 'Start date',
      JP: '開始日',
      CH: '开始日期',
      TH: 'วันเริ่ม'
    },
    '종료일': {
      EN: 'End date',
      JP: '終了日',
      CH: '结束日期',
      TH: 'วันสิ้นสุด'
    },
    '조회': {
      EN: 'Search',
      JP: '照会',
      CH: '查询',
      TH: 'ค้นหา'
    },
    '주기': {
      EN: 'Cycle',
      JP: '周期',
      CH: '周期',
      TH: 'รอบ'
    },
    '대상 from': {
      EN: 'Period from',
      JP: '対象 from',
      CH: '区间 from',
      TH: 'ช่วง from'
    },
    '대상 to': {
      EN: 'Period to',
      JP: '対象 to',
      CH: '区间 to',
      TH: 'ช่วง to'
    },
    '일중(M·H·TM·TH)는 당일 행·비고는 요약입니다. 상세는 서버 집계 규칙과 동일합니다.': {
      EN: 'Intraday (M/H/TM/TH) rows and notes are summarized; details follow server aggregation rules.',
      JP: '日中(M・H・TM・TH)は当日行・備考は要約です。詳細はサーバー集計ルールと同じです。',
      CH: '日内（M/H/TM/TH）行与备注为摘要，细则与服务器汇总规则一致。',
      TH: 'ภายในวัน (M/H/TM/TH) แถวและหมายเหตุเป็นสรุป รายละเอียดตามกฎรวมบนเซิร์ฟเวอร์'
    },
    '가맹 정산주기 변경 이력': {
      EN: 'Merchant settlement cycle change history',
      JP: '加盟店精算サイクル変更履歴',
      CH: '商户结算周期变更历史',
      TH: 'ประวัติการเปลี่ยนรอบชำระร้าน'
    },
    '전체 (업체 미선택)': {
      EN: 'All (no merchant selected)',
      JP: '全体（加盟店未選択）',
      CH: '全部（未选商户）',
      TH: 'ทั้งหมด (ยังไม่เลือกร้าน)'
    },
    '조회 중…': {
      EN: 'Loading…',
      JP: '照会中…',
      CH: '查询中…',
      TH: 'กำลังค้นหา…'
    },
    '가맹점 (업체번호 / 이름)': {
      EN: 'Merchant (code / name)',
      JP: '加盟店（店番号／名称）',
      CH: '商户（编号/名称）',
      TH: 'ร้านค้า (รหัส/ชื่อ)'
    },
    '이전': {
      EN: 'Before',
      JP: '変更前',
      CH: '变更前',
      TH: 'ก่อน'
    },
    '변경': {
      EN: 'After',
      JP: '変更後',
      CH: '变更后',
      TH: 'หลัง'
    },
    '작업자': {
      EN: 'Actor',
      JP: '作業者',
      CH: '操作人',
      TH: 'ผู้ดำเนินการ'
    },
    '건별': {
      EN: 'Per transaction',
      JP: '件別',
      CH: '逐笔',
      TH: 'รายรายการ'
    },
    '누계': {
      EN: 'Cumulative',
      JP: '累計',
      CH: '累计',
      TH: 'สะสม'
    },
    /* 정산관리설정 — 내장 정산주기 표 설명 (정산주기:{CODE}:desc). DB 커스텀 행은 원문 유지 */
    '정산주기:_EMPTY:desc': {
      KO: '주기 선택.',
      EN: 'Select a cycle.',
      JP: '周期を選択してください。',
      CH: '请选择周期。',
      TH: 'เลือกรอบ'
    },
    '정산주기:NONE:desc': {
      KO: '자동 정산 배치에서 제외됩니다.',
      EN: 'Excluded from the automatic settlement batch.',
      JP: '自動精算バッチの対象外です。',
      CH: '不包含在自动结算批处理中。',
      TH: 'ไม่อยู่ในแบตช์ชำระอัตโนมัติ'
    },
    '정산주기:RT:desc': {
      KO: '정산구분 AUTO일 때 승인(결제완료) 노티마다 건당 마감에 해당하도록 당일 00:00~현재까지 즉시 재집계합니다(결제 단위 실시간 마감).',
      EN: 'When settlement type is AUTO, each approval (payment-complete) notify immediately re-aggregates same-day 00:00–now for per-transaction close (real-time per payment).',
      JP: '精算区分AUTOのとき、承認(決済完了)ノティごとに当日0:00～現在までを即時再集計し、件別締めとします(決済単位のリアルタイム締め)。',
      CH: '结算类型为 AUTO 时，每笔批准（支付完成）通知会立即将当日 0:00 至今再汇总，按笔关闭（按支付实时）。',
      TH: 'เมื่อประเภทชำระเป็น AUTO แต่ละแจ้งอนุมัติจะรวมยอดใหม่ทันทีตั้งแต่ 00:00 ถึงปัจจุบัน ปิดรายรายการ'
    },
    '정산주기:T0:desc': {
      KO: 'RT와 동일하게 승인 노티 직후 건당 마감·당일 누적 자동정산에 사용합니다.',
      EN: 'Same as RT: immediately after each approval notify, per-transaction close and same-day cumulative auto-settlement.',
      JP: 'RTと同様に、承認ノティ直後の件別締め・当日累積の自動精算に使います。',
      CH: '与 RT 相同：批准通知后立即按笔关闭并用于当日累计自动结算。',
      TH: 'เหมือน RT หลังแจ้งอนุมัติทันที ปิดรายรายการและสะสมภายในวัน'
    },
    '정산주기:M5:desc': {
      KO: '5분 격자 마감 정산: 매 0·5·10…분 정각에 당일 00:00~현재까지 누적을 재집계합니다(노티 직후 실행 아님).',
      EN: '5-minute grid close: at each :00,:05,:10… re-aggregate same-day 00:00–now (not immediately on notify).',
      JP: '5分格子締め：毎時0・5・10…分に当日0:00～現在までを再集計します(ノティ直後ではありません)。',
      CH: '5 分钟网格：在每个整 5 分钟点重新汇总当日 0:00 至今（非通知后立即执行）。',
      TH: 'ตาราง 5 นาที รวมใหม่ทุก :00,:05… ไม่ใช่ทันทีหลังแจ้งเตือน'
    },
    '정산주기:M10:desc': {
      KO: '10분 격자 마감 정산: 매 0·10·20…분 정각에 당일 누적을 재집계합니다.',
      EN: '10-minute grid: at :00,:10,:20… re-aggregate same-day cumulative totals.',
      JP: '10分格子：毎時0・10・20…分に当日累積を再集計します。',
      CH: '10 分钟网格：在 :00、:10、:20… 重新汇总当日累计。',
      TH: 'ช่วง 10 นาที รวมยอดซ้ำที่ :00,:10,…'
    },
    '정산주기:M30:desc': {
      KO: '30분 격자 마감 정산: 매 0·30분 정각에 당일 누적을 재집계합니다.',
      EN: '30-minute grid: at :00 and :30 re-aggregate same-day cumulative totals.',
      JP: '30分格子：毎時0・30分に当日累積を再集計します。',
      CH: '30 分钟网格：在 :00 与 :30 重新汇总当日累计。',
      TH: 'ช่วง 30 นาที รวมที่ :00 และ :30'
    },
    '정산주기:H1:desc': {
      KO: '하루 24회: 매시 정각(HH:00)에 전 시각 구간 마감 정산. AUTO 배치는 매분 크론에서 정각에만 당일 누적을 재집계합니다.',
      EN: '24 runs/day: close each hour at HH:00. AUTO batch cron re-aggregates same-day cumulative totals only on the hour.',
      JP: '1日24回：毎正時(HH:00)に前時間帯を締めます。AUTOバッチは毎分のcronで正時のみ当日累積を再集計します。',
      CH: '每天 24 次：每小时整点关闭上一时段。AUTO 批处理在每分钟 cron 中仅在整点重算当日累计。',
      TH: '24 ครั้ง/วัน ปิดทุกชั่วโมงที่นาที 00'
    },
    '정산주기:H2:desc': {
      KO: '하루 12회: 0·2·4…시 00분에 2시간 단위 마감 정산.',
      EN: '12 runs/day: at 0,2,4… o’clock, 2-hour bucket close.',
      JP: '1日12回：0・2・4…時00分に2時間単位で締めます。',
      CH: '每天 12 次：在 0、2、4… 点整按 2 小时桶关闭。',
      TH: '12 ครั้ง/วัน ทุก 2 ชม. ที่นาที 00'
    },
    '정산주기:H4:desc': {
      KO: '하루 6회: 0·4·8…시 00분에 4시간 단위 마감 정산.',
      EN: '6 runs/day: at 0,4,8… o’clock, 4-hour bucket close.',
      JP: '1日6回：0・4・8…時00分に4時間単位で締めます。',
      CH: '每天 6 次：在 0、4、8… 点整按 4 小时桶关闭。',
      TH: '6 ครั้ง/วัน ทุก 4 ชม.'
    },
    '정산주기:H6:desc': {
      KO: '하루 4회: 0·6·12·18시 00분에 6시간 단위 마감 정산.',
      EN: '4 runs/day: at 0,6,12,18:00, 6-hour bucket close.',
      JP: '1日4回：0・6・12・18時00分に6時間単位で締めます。',
      CH: '每天 4 次：在 0、6、12、18 点整按 6 小时桶关闭。',
      TH: '4 ครั้ง/วัน ทุก 6 ชม.'
    },
    '정산주기:H8:desc': {
      KO: '하루 3회: 0·8·16시 00분에 8시간 단위 마감 정산.',
      EN: '3 runs/day: at 0,8,16:00, 8-hour bucket close.',
      JP: '1日3回：0・8・16時00分に8時間単位で締めます。',
      CH: '每天 3 次：在 0、8、16 点整按 8 小时桶关闭。',
      TH: '3 ครั้ง/วัน ทุก 8 ชม.'
    },
    '정산주기:H12:desc': {
      KO: '하루 2회: 0·12시 00분에 12시간 단위 마감 정산.',
      EN: '2 runs/day: at 0:00 and 12:00, 12-hour bucket close.',
      JP: '1日2回：0・12時00分に12時間単位で締めます。',
      CH: '每天 2 次：在 0 点与 12 点整按 12 小时桶关闭。',
      TH: '2 ครั้ง/วัน ที่ 00:00 และ 12:00'
    },
    '정산주기:TM5:desc': {
      KO: '당일 누적, 5분 격자.',
      EN: 'Same-day cumulative, 5-minute grid.',
      JP: '当日累積、5分格子。',
      CH: '当日累计，5 分钟网格。',
      TH: 'สะสมภายในวัน ตาราง 5 นาที'
    },
    '정산주기:TM10:desc': {
      KO: '당일 누적, 10분 격자.',
      EN: 'Same-day cumulative, 10-minute grid.',
      JP: '当日累積、10分格子。',
      CH: '当日累计，10 分钟网格。',
      TH: 'สะสมภายในวัน ตาราง 10 นาที'
    },
    '정산주기:TM30:desc': {
      KO: '당일 누적, 30분 격자.',
      EN: 'Same-day cumulative, 30-minute grid.',
      JP: '当日累積、30分格子。',
      CH: '当日累计，30 分钟网格。',
      TH: 'สะสมภายในวัน ตาราง 30 นาที'
    },
    '정산주기:TH1:desc': {
      KO: '당일 누적, 1시간 격자.',
      EN: 'Same-day cumulative, 1-hour grid.',
      JP: '当日累積、1時間格子。',
      CH: '当日累计，1 小时网格。',
      TH: 'สะสมภายในวัน ตาราง 1 ชม.'
    },
    '정산주기:TH2:desc': {
      KO: '당일 누적, 2시간 격자.',
      EN: 'Same-day cumulative, 2-hour grid.',
      JP: '当日累積、2時間格子。',
      CH: '当日累计，2 小时网格。',
      TH: 'สะสมภายในวัน ตาราง 2 ชม.'
    },
    '정산주기:TH4:desc': {
      KO: '당일 누적, 4시간 격자.',
      EN: 'Same-day cumulative, 4-hour grid.',
      JP: '当日累積、4時間格子。',
      CH: '当日累计，4 小时网格。',
      TH: 'สะสมภายในวัน ตาราง 4 ชม.'
    },
    '정산주기:TH6:desc': {
      KO: '당일 누적, 6시간 격자.',
      EN: 'Same-day cumulative, 6-hour grid.',
      JP: '当日累積、6時間格子。',
      CH: '当日累计，6 小时网格。',
      TH: 'สะสมภายในวัน ตาราง 6 ชม.'
    },
    '정산주기:TH8:desc': {
      KO: '당일 누적, 8시간 격자.',
      EN: 'Same-day cumulative, 8-hour grid.',
      JP: '当日累積、8時間格子。',
      CH: '当日累计，8 小时网格。',
      TH: 'สะสมภายในวัน ตาราง 8 ชม.'
    },
    '정산주기:TH12:desc': {
      KO: '당일 누적, 12시간 격자.',
      EN: 'Same-day cumulative, 12-hour grid.',
      JP: '当日累積、12時間格子。',
      CH: '当日累计，12 小时网格。',
      TH: 'สะสมภายในวัน ตาราง 12 ชม.'
    },
    '정산주기:D0:desc': {
      KO: '정산일(달력 당일) 당일 하루 승인분을 집계합니다. 자동 배치는 서울 기준 당일 00:00~23:50 구간에서만 실행되며, 정산마감시간이 있으면 그 이후부터 위 구간 안에서만 실행됩니다.',
      EN: 'On the settlement date (calendar day), aggregate that day’s approvals. AUTO batch runs only 00:00–23:50 Seoul time; if a close time is set, runs only after it within that window.',
      JP: '精算日(暦当日)の承認分を集計します。自動バッチはソウル基準当日0:00～23:50のみ。精算締め時刻がある場合はその後～上記枠内のみ実行されます。',
      CH: '在结算日（日历当日）汇总当日批准。自动批处理仅在首尔当日 0:00–23:50；若配置结算截止时间，则仅在该时间之后至上述窗口内执行。',
      TH: 'วันชำระ (ปฏิทิน) รวมการอนุมัติของวันนั้น แบตช์ AUTO เฉพาะ 00:00–23:50 โซล'
    },
    '정산주기:D1:desc': {
      KO: '정산일 당일에 마감·배치로 처리합니다. 집계 기준일=정산일에서 1영업일 역산한 하루(주말 제외). ‘전일’이 아니라 정산일·집계기준일 관계입니다.',
      EN: 'Settles on the settlement date. Basis day = one business day before the settlement date (weekends excluded). Not simply “yesterday”; it is the relationship between settlement date and basis day.',
      JP: '精算日当日に締め・バッチ処理。集計基準日＝精算日から営業日1日逆算(土日除く)。「前日」ではなく精算日と集計基準日の関係です。',
      CH: '在结算日当日关账并批处理。汇总基准日 = 从结算日逆推 1 个工作日（不含周末）。不是简单的“前一天”，而是结算日与基准日的关系。',
      TH: 'ชำระในวันชำระ วันฐาน = ย้อน 1 วันทำการจากวันชำระ ไม่ใช่แค่เมื่อวาน'
    },
    '정산주기:D2:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 2영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 2 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日2日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 2 个工作日（不含周末）。',
      TH: 'แบตช์วันชำระ ฐาน = ย้อน 2 วันทำการ'
    },
    '정산주기:D3:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 3영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 3 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日3日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 3 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 3 วันทำการ'
    },
    '정산주기:D5:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 5영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 5 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日5日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 5 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 5 วันทำการ'
    },
    '정산주기:D7:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 7영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 7 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日7日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 7 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 7 วันทำการ'
    },
    '정산주기:D10:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 10영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 10 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日10日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 10 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 10 วันทำการ'
    },
    '정산주기:D15:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 15영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 15 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日15日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 15 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 15 วันทำการ'
    },
    '정산주기:D20:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 20영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 20 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日20日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 20 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 20 วันทำการ'
    },
    '정산주기:D30:desc': {
      KO: '정산일 당일 배치. 집계 기준일=정산일에서 30영업일 역산한 하루(주말 제외).',
      EN: 'Batch on settlement date; basis day is 30 business days before the settlement date (weekends excluded).',
      JP: '精算日当日バッチ。集計基準日＝精算日から営業日30日逆算(土日除く)。',
      CH: '结算日当天批处理；基准日为结算日前 30 个工作日（不含周末）。',
      TH: 'ฐาน = ย้อน 30 วันทำการ'
    },
    '정산주기:W3:desc': {
      KO: '직전 주(월~일) 구간을 정산하고, 주 종료 후 3영업일째 되는 날이 정산일일 때 실행됩니다.',
      EN: 'Settles the prior Mon–Sun week; runs when the settlement date is the 3rd business day after week end.',
      JP: '直前週(月～日)を精算し、週終了後の営業日3日目が精算日のときに実行されます。',
      CH: '结算上一自然周（周一至周日）；当周结束后的第 3 个工作日为结算日时执行。',
      TH: 'สัปดาห์ก่อน (จ–อา) รันวันที่เป็นวันชำระ = วันทำการที่ 3 หลังปิดสัปดาห์'
    },
    '정산주기:W5:desc': {
      KO: '전주 구간 + 5영업일 오프셋 규칙입니다.',
      EN: 'Prior week window plus a 5-business-day offset rule.',
      JP: '前週区間＋営業日5日オフセットのルールです。',
      CH: '上一周区间 + 5 个工作日偏移规则。',
      TH: 'ช่วงสัปดาห์ก่อน + เลื่อน 5 วันทำการ'
    },
    '정산주기:W7:desc': {
      KO: '월~일(달력 1주) 집계. 일요일 마감 후 7영업일째가 정산일(주말·공휴는 익영업일). AUTO 가맹은 그날 정산마감시각 이후 자동 1회 실행.',
      EN: 'Calendar Mon–Sun window. Settlement on the 7th business day after Sunday close (rolls forward on weekends/holidays). AUTO merchants: one auto run after close time that day.',
      JP: '月〜日(暦週)集計。日曜締めの翌営業日から7営業日目が精算日(休日は翌営業日)。AUTOは当日精算締め時刻以降に1回自動実行。',
      CH: '按日历周一至周日汇总。周日截止后第7个工作日结算（遇周末/假日顺延）。AUTO 商户在当日结算截止时间后自动执行一次。',
      TH: 'สรุปจันทร์–อาทิตย์ (ปฏิทิน) ปิดสัปดาห์วันอาทิตย์ +7 วันทำการ (เลื่อนถ้าหยุด) AUTO: รันหลังเวลาปิดยอดวันนั้น 1 ครั้ง'
    },
    '정산주기:W10:desc': {
      KO: '전주 구간 + 10영업일 오프셋 규칙입니다.',
      EN: 'Prior week window plus a 10-business-day offset rule.',
      JP: '前週区間＋営業日10日オフセットのルールです。',
      CH: '上一周区间 + 10 个工作日偏移规则。',
      TH: 'ช่วงสัปดาห์ก่อน + เลื่อน 10 วันทำการ'
    },
    '정산주기:W14:desc': {
      KO: '전주 구간 + 14영업일 오프셋 규칙입니다.',
      EN: 'Prior week window plus a 14-business-day offset rule.',
      JP: '前週区間＋営業日14日オフセットのルールです。',
      CH: '上一周区间 + 14 个工作日偏移规则。',
      TH: 'ช่วงสัปดาห์ก่อน + เลื่อน 14 วันทำการ'
    },
    '정산주기:WK1W:desc': {
      KO: '전주 기준, 주 종료 후 수요일+1주(영업일 보정)에 맞춰 격주 아님 주에 실행됩니다.',
      EN: 'Based on the prior week; runs on the Wednesday +1 week (business-day adjusted) pattern for non-biweekly weeks.',
      JP: '前週基準で、週終了後の水曜＋1週(営業日補正)に合わせ、隔週でない週に実行されます。',
      CH: '按上一周；在周结束后对齐“周三+1 周”（工作日校正），在非隔周周执行。',
      TH: 'อิงสัปดาห์ก่อน รันตามพุธ+1 สัปดาห์ (ปรับวันทำการ)'
    },
    '정산주기:WK2W:desc': {
      KO: '2주(격주) 단위 전주 묶음에 대해 동일 규칙으로 실행됩니다.',
      EN: 'Same rule applied to bi-weekly bundles of prior weeks.',
      JP: '2週(隔週)単位の前週まとめに対し、同じルールで実行されます。',
      CH: '对每两周（隔周）的前周组合按相同规则执行。',
      TH: 'กลุ่มสองสัปดาห์ ใช้กฎเดียวกัน'
    },
    '정산주기:WK1WT:desc': {
      KO: 'WK+1W 변형(수요일 오프셋이 다름). 자동 창은 SettlementPeriodResolver 규칙을 따릅니다.',
      EN: 'WK+1W variant (Wednesday offset differs). Auto windows follow SettlementPeriodResolver rules.',
      JP: 'WK+1W変形(水曜オフセットが異なる)。自動ウィンドウはSettlementPeriodResolverの規則に従います。',
      CH: 'WK+1W 变体（周三偏移不同）。自动窗口遵循 SettlementPeriodResolver 规则。',
      TH: 'แบบ WK+1W (ออฟเซ็ตพุธต่าง) ตาม SettlementPeriodResolver'
    },
    '정산주기:WK2WT:desc': {
      KO: 'WK+2W 변형(수요일 오프셋이 다름).',
      EN: 'WK+2W variant (Wednesday offset differs).',
      JP: 'WK+2W変形(水曜オフセットが異なる)。',
      CH: 'WK+2W 变体（周三偏移不同）。',
      TH: 'แบบ WK+2W (ออฟเซ็ตพุธต่าง)'
    },
    '정산주기:WK1WM:desc': {
      KO: '주 마감 후 30영업일.',
      EN: '30 business days after week close.',
      JP: '週締め後30営業日。',
      CH: '周结束后 30 个工作日。',
      TH: 'หลังปิดสัปดาห์ 30 วันทำการ'
    },
    '정산주기:WK2WM:desc': {
      KO: '2주 마감 후 30영업일.',
      EN: '30 business days after bi-week close.',
      JP: '2週締め後30営業日。',
      CH: '双周结束后 30 个工作日。',
      TH: 'หลังปิดสองสัปดาห์ 30 วันทำการ'
    },
    '켜짐': {
      EN: 'On',
      JP: 'オン',
      CH: '开',
      TH: 'เปิด'
    },
    '꺼짐': {
      EN: 'Off',
      JP: 'オフ',
      CH: '关',
      TH: 'ปิด'
    },
    '지금 기동 중인 서버에는 「매 주기마다 AUTO 정산을 시도하는」백그라운드 타이머가 들어가 있습니다. 이 타이머를 끄려면 운영 쪽에서 자동 정산 시작 설정을 끈 설정으로 JAR를 올리고 서버를 다시 시작해야 하며, 이 화면만으로는 타이머 자체를 제거할 수 없습니다.': {
      EN: 'This running server has a background timer that attempts AUTO settlement every cycle. To disable it, deploy a JAR with auto-run disabled in ops config and restart; this screen cannot remove the timer itself.',
      JP: '起動中のサーバーには「毎周期AUTO精算を試行する」バックグラウンドタイマーが入っています。オフにするには運用側で自動精算開始をオフにした設定のJARを上げて再起動が必要で、この画面だけではタイマー自体を外せません。',
      CH: '当前运行的服务器包含每个周期尝试 AUTO 结算的后台定时器。要关闭需在运维配置中禁用自动结算并重新部署 JAR 后重启；本页面无法单独移除定时器。',
      TH: 'เซิร์ฟเวอร์นี้มีตัวจับเวลาพื้นหลังที่พยายามชำระ AUTO ทุกรอบ ต้องปิดใน ops และรีสตาร์ท JAR หน้านี้ถอดตัวจับเวลาเองไม่ได้'
    },
    '지금 기동 중인 서버에는 위 타이머가 들어가 있지 않습니다. 즉, Java 앱이 「자동 정산 스케줄 사용 안 함」으로 올라온 상태입니다. 켜려면 운영 서버의 기동 설정에 환경 변수 APP_SETTLEMENT_AUTO_RUN=true(또는 app.settlement.autoRunEnabled=true)를 넣은 뒤 서버를 다시 시작해야 합니다. 이 작업은 관리자 웹이 아니라 서버 배포·재시작 절차에서 합니다.': {
      EN: 'This server does not include that timer—the Java app started with auto settlement scheduling off. To enable, set APP_SETTLEMENT_AUTO_RUN=true (or app.settlement.autoRunEnabled=true) in startup config and restart. This is done in deployment/restart, not in this admin UI.',
      JP: '起動中のサーバーには上記タイマーが入っていません。Javaアプリが「自動精算スケジュール未使用」で上がっている状態です。オンにするには起動設定に APP_SETTLEMENT_AUTO_RUN=true（または app.settlement.autoRunEnabled=true）を入れて再起動します。管理者Webではなくデプロイ・再起動手順で行います。',
      CH: '当前服务器未启用该定时器，即 Java 以「不使用自动结算调度」启动。启用需在启动配置设置 APP_SETTLEMENT_AUTO_RUN=true（或 app.settlement.autoRunEnabled=true）并重启，由部署流程完成而非本管理端。',
      TH: 'เซิร์ฟเวอร์นี้ไม่มีตัวจับเวลา — แอป Java เริ่มแบบปิดตารางชำระอัตโนมัติ เปิดด้วย APP_SETTLEMENT_AUTO_RUN=true แล้วรีสตาร์ท ทำในการดีพลอย ไม่ใช่ในเว็บแอดมิน'
    },
    '실행 중 — 서버 타이머·자동 모드이며, 이번 조회 시점에 tick 본문이 허용된 상태입니다.': {
      EN: 'Running — server timer and Auto mode; tick body is allowed at this check.',
      JP: '実行中 — サーバタイマー・自動モードで、今回の照会時点でtick本体が許可されています。',
      CH: '运行中 — 服务器定时器与自动模式；本次查询时允许执行 tick 主体。',
      TH: 'กำลังทำงาน — ตัวจับเวลาและโหมดอัตโนมัติ อนุญาต tick ณ เวลาตรวจ'
    },
    '실행 중 — 서버 타이머와 본사 활성 모드로 주기 tick 이 돕니다.': {
      EN: 'Running — server timer and HQ Active mode; periodic tick runs.',
      JP: '実行中 — サーバタイマーと本社有効モードで周期tickが動きます。',
      CH: '运行中 — 服务器定时器与总部启用模式；周期 tick 运行。',
      TH: 'กำลังทำงาน — ตัวจับเวลาและโหมด Active ของ HQ tick รัน'
    },
    '실행 안 됨 — 아래 안내를 확인하세요.': {
      EN: 'Not running — see hints below.',
      JP: '未実行 — 下の案内を確認してください。',
      CH: '未执行 — 请查看下方说明。',
      TH: 'ไม่รัน — ดูคำแนะนำด้านล่าง'
    },
    '자동 모드: 대상이 없는 주기에는 tick 본문이 실행되지 않습니다. 항상 돌리려면 「활성」으로 저장하세요.': {
      EN: 'Auto: tick body is skipped on cycles with no targets. Save as Active to always run.',
      JP: '自動モード: 対象がない周期ではtick本体は実行されません。常に回すには「有効」で保存してください。',
      CH: '自动模式：无对象的周期不执行 tick 主体。要始终运行请保存为「启用」。',
      TH: 'อัตโนมัติ: ข้ามเมื่อไม่มีเป้าหมาย ต้องการรันตลอดให้บันทึกเป็น เปิดใช้'
    },
    '배치를 멈추려면 ②를 「비활성」으로 저장하세요.': {
      EN: 'To stop batching, save ② as Inactive.',
      JP: 'バッチを止めるには②を「無効」で保存してください。',
      CH: '要停止批处理，请将②保存为「停用」。',
      TH: 'หยุดแบตช์ให้บันทึก②เป็น ปิดใช้งาน'
    },
    '②가 비활성이면 서버 타이머가 켜져 있어도 tick 본문은 실행되지 않습니다.': {
      EN: 'When ② is inactive, tick body does not run even if the server timer is on.',
      JP: '②が無効ならサーバタイマーがオンでもtick本体は実行されません。',
      CH: '②停用后，即使服务器定时器开启也不执行 tick 主体。',
      TH: 'ถ้า②ปิด แม้ตัวจับเวลาเปิดก็ไม่รัน tick'
    },
    '①은 켜져 있고 자동 모드입니다. 지금 이 순간에는 실행할 AUTO 가맹(주기·시각 조건)이 없어 tick 본문이 스킵됩니다.': {
      EN: '① is on and mode is Auto; right now no AUTO merchant matches cycle/time, so tick body is skipped.',
      JP: '①はオンで自動モードです。今この瞬間は条件に合うAUTO加盟店がないためtick本体はスキップされます。',
      CH: '①已开启且为自动模式；当前无满足周期/时间的 AUTO 商户，故跳过 tick 主体。',
      TH: '①เปิดและโหมดอัตโนมัติ ตอนนี้ไม่มีร้าน AUTO ตรงเงื่อนไขจึงข้าม tick'
    },
    '① 서버 타이머가 꺼져 있습니다. 운영 기동 설정으로 타이머를 켠 뒤 앱을 다시 시작해야 합니다.': {
      EN: '① Server timer is off. Enable it in ops startup config and restart the app.',
      JP: '①サーバタイマーがオフです。運用起動設定でオンにしてからアプリを再起動してください。',
      CH: '① 服务器定时器已关闭。请在运维启动配置中启用并重启应用。',
      TH: '① ตัวจับเวลาปิด เปิดในการตั้งค่าเริ่มระบบแล้วรีสตาร์ทแอป'
    },
    '①이 켜져 있으면 활성 모드는 매 주기 tick 본문을 시도합니다.': {
      EN: 'When ① is on, Active mode attempts tick body every cycle.',
      JP: '①がオンなら有効モードは毎周期tick本体を試行します。',
      CH: '① 开启时，启用模式每个周期都会尝试 tick 主体。',
      TH: 'เมื่อ①เปิด โหมด Active พยายาม tick ทุกรอบ'
    },
    '①·② 상태를 확인하세요.': {
      EN: 'Check ① and ② status.',
      JP: '①・②の状態を確認してください。',
      CH: '请检查①与②状态。',
      TH: 'ตรวจสถานะ①และ②'
    },
    '일정 조회 실패': {
      EN: 'Schedule preview failed',
      JP: 'スケジュール照会に失敗しました',
      CH: '日程查询失败',
      TH: 'ดูตารางไม่สำเร็จ'
    },
    '이력이 없습니다.': {
      EN: 'No history rows.',
      JP: '履歴がありません。',
      CH: '暂无历史记录。',
      TH: 'ไม่มีประวัติ'
    },
    '이력 조회 실패': {
      EN: 'Failed to load history',
      JP: '履歴の取得に失敗しました',
      CH: '历史查询失败',
      TH: 'โหลดประวัติไม่สำเร็จ'
    },
    '등록되었습니다.': {
      EN: 'Created.',
      JP: '登録しました。',
      CH: '已创建。',
      TH: 'ลงทะเบียนแล้ว'
    },
    '등록 실패': {
      EN: 'Create failed',
      JP: '登録に失敗しました',
      CH: '创建失败',
      TH: 'ลงทะเบียนไม่สำเร็จ'
    },
    '내장 표준 정산주기 코드 중 DB(tb_hq_settlement_cycle_def)에 없는 행만 추가합니다. 계속할까요?': {
      EN: 'Insert only built-in standard settlement cycle codes missing from tb_hq_settlement_cycle_def. Continue?',
      JP: '内蔵標準精算周期コードのうちDB(tb_hq_settlement_cycle_def)にない行のみ追加します。続行しますか？',
      CH: '仅向数据库 tb_hq_settlement_cycle_def 插入缺失的内置标准结算周期代码。是否继续？',
      TH: 'แทรกเฉพาะรหัสรอบมาตรฐานในตัวที่ยังไม่มีใน tb_hq_settlement_cycle_def ต่อหรือไม่'
    },
    '추가된 행 수: {N} (이미 있던 코드는 건너뜁니다)': {
      EN: 'Rows inserted: {N} (existing codes skipped)',
      JP: '追加行数: {N}（既存コードはスキップ）',
      CH: '插入行数：{N}（已存在的代码已跳过）',
      TH: 'แถวที่เพิ่ม: {N} (ข้ามรหัสที่มีแล้ว)'
    },
    '복원 실패': {
      EN: 'Restore failed',
      JP: '復元に失敗しました',
      CH: '恢复失败',
      TH: 'คืนค่าไม่สำเร็จ'
    },
    '삭제할까요?': {
      EN: 'Delete this row?',
      JP: '削除しますか？',
      CH: '要删除吗？',
      TH: 'ลบแถวนี้หรือไม่'
    },
    '(저장값)': {
      EN: ' (saved)',
      JP: '（保存値）',
      CH: '（已保存）',
      TH: ' (ที่บันทึก)'
    },
    '기준국가 ': {
      EN: 'Country ',
      JP: '基準国 ',
      CH: '基准国家 ',
      TH: 'ประเทศอ้างอิง '
    },
    '미등록 · 총판 업체정보 영업일·휴일에서 설정': {
      EN: 'Not set — configure business days/holidays in distributor company profile',
      JP: '未登録 · 総販の会社情報の営業日・休日で設定',
      CH: '未登记 · 请在总代公司信息的营业日/假日中设置',
      TH: 'ยังไม่ตั้ง · ตั้งวันทำการ/วันหยุดในโปรไฟล์ตัวแทน'
    },
    '거래시간·정산 크론 설정을 DB에 저장': {
      EN: 'Save transaction time & settlement cron to DB',
      JP: '取引時間・精算クロン設定をDBに保存',
      CH: '将交易时间与结算 cron 保存到数据库',
      TH: 'บันทึกเวลาธุรกรรมและ cron ชำระลง DB'
    },
    '등록된 총판이 없습니다.': {
      EN: 'No distributors registered.',
      JP: '登録された総販がありません。',
      CH: '没有已登记的总代。',
      TH: 'ไม่มีตัวแทนหลักที่ลงทะเบียน'
    },
    '저장되었습니다: {CID} · 거래시간 {TXN} · 크론 {ZONE}': {
      EN: 'Saved: {CID} · txn time {TXN} · cron {ZONE}',
      JP: '保存しました: {CID} · 取引時間 {TXN} · クロン {ZONE}',
      CH: '已保存：{CID} · 交易时间 {TXN} · cron {ZONE}',
      TH: 'บันทึกแล้ว: {CID} · เวลา {TXN} · cron {ZONE}'
    },
    '저장되었습니다: {TXN} / {ZONE}': {
      EN: 'Saved: {TXN} / {ZONE}',
      JP: '保存しました: {TXN} / {ZONE}',
      CH: '已保存：{TXN} / {ZONE}',
      TH: 'บันทึกแล้ว: {TXN} / {ZONE}'
    },
    '(미지정)': {
      EN: '(unset)',
      JP: '（未指定）',
      CH: '（未指定）',
      TH: '(ยังไม่ระบุ)'
    },
    '본사 정산주기 병합 목록에 없거나 비활성인 코드입니다. 저장값은 유지됩니다.': {
      EN: 'Code not in HQ merged list or inactive; saved value is kept.',
      JP: '本社精算サイクル結合一覧にないか非活性のコードです。保存値は維持されます。',
      CH: '代码不在总部合并列表或已停用；保留已保存值。',
      TH: 'รหัสไม่อยู่ในรายการรวม HQ หรือปิดใช้ ค่าที่บันทึกคงอยู่'
    },
    '{0} (저장값)': {
      EN: '{0} (saved)',
      JP: '{0}（保存値）',
      CH: '{0}（已保存）',
      TH: '{0} (ที่บันทึก)'
    },
    '총판을 선택하세요': {
      EN: 'Select a distributor',
      JP: '総販を選択してください',
      CH: '请选择总代',
      TH: 'เลือกตัวแทนหลัก'
    },
    '목록 로드 실패': {
      EN: 'List load failed',
      JP: '一覧の読み込みに失敗しました',
      CH: '列表加载失败',
      TH: 'โหลดรายการไม่สำเร็จ'
    },
    '총판 목록을 불러오지 못했습니다.': {
      EN: 'Could not load distributor list.',
      JP: '総販一覧を読み込めませんでした。',
      CH: '无法加载总代列表。',
      TH: 'โหลดรายการตัวแทนไม่สำเร็จ'
    },
    '총판을 선택하세요.': {
      EN: 'Please select a distributor.',
      JP: '総販を選択してください。',
      CH: '请选择总代。',
      TH: 'โปรดเลือกตัวแทนหลัก'
    },
    '서로 다른 정산주기는 최소 2개 이상 선택해야 합니다.': {
      EN: 'Pick at least two different settlement cycles.',
      JP: '異なる精算サイクルを最低2つ選択してください。',
      CH: '至少选择两个不同的结算周期。',
      TH: 'เลือกอย่างน้อย 2 รอบที่ต่างกัน'
    },


    /* noticeList toolbar + modal + hq receivable (2026) */
    '글작성': {
      EN: 'Write post', JP: '投稿', CH: '撰写', TH: 'เขียนประกาศ'
    },
    '첫화면': {
      EN: 'Home screen', JP: 'トップ画面', CH: '首页', TH: 'หน้าแรก'
    },
    '팝업': {
      EN: 'Popup', JP: 'ポップアップ', CH: '弹窗', TH: 'ป๊อปอัป'
    },
    '접속팝업': {
      EN: 'Access popup', JP: '接続ポップアップ', CH: '访问弹窗', TH: 'ป๊อปอัปเมื่อเข้าใช้'
    },
    '메인공지': {
      EN: 'Main notice', JP: 'メイン告知', CH: '主页公告', TH: 'ประกาศหน้าหลัก'
    },
    '공지일자': {
      EN: 'Notice date', JP: '告知日', CH: '公告日期', TH: 'วันที่ประกาศ'
    },
    '공지사항': {
      EN: 'Notices', JP: 'お知らせ', CH: '公告', TH: 'ประกาศ'
    },
    '자세히 보기': {
      EN: 'View details', JP: '詳細を見る', CH: '查看详情', TH: 'ดูรายละเอียด'
    },
    '접속팝업 공지로 지정되었습니다.': {
      EN: 'Set as access popup notice.',
      JP: '接続ポップアップのお知らせに設定しました。',
      CH: '已设为访问弹窗公告。',
      TH: 'ตั้งเป็นป๊อปอัปเมื่อเข้าใช้แล้ว'
    },
    '로그인 후 팝업 공지로 지정되었습니다.': {
      EN: 'Set as post-login popup notice.',
      JP: 'ログイン後ポップアップのお知らせに設定しました。',
      CH: '已设为登录后弹窗公告。',
      TH: 'ตั้งเป็นป๊อปอัปหลังเข้าสู่ระบบแล้ว'
    },
    '메인공지로 지정되었습니다.': {
      EN: 'Set as main dashboard notice.',
      JP: 'メイン告知に設定しました。',
      CH: '已设为主页公告。',
      TH: 'ตั้งเป็นประกาศหน้าหลักแล้ว'
    },
    '접속팝업·첫화면은 총본사만 사용할 수 있습니다.': {
      EN: 'Access popup and home screen are for headquarters only.',
      JP: '接続ポップアップ・トップ画面は総本社のみ利用できます。',
      CH: '访问弹窗与首页仅总本部可用。',
      TH: 'ป๊อปอัปเมื่อเข้าใช้และหน้าแรกใช้ได้เฉพาะสำนักงานใหญ่'
    },
    '공지 등록은 총본사·본사·총판(화면 권한 수정 이상)만 가능합니다. 접속팝업·첫화면은 총본사 전용이며, 본사·총판은 팝업·메인공지만 사용할 수 있습니다.': {
      EN: 'Only HQ, regional HQ, and master distributors (with edit permission) can post notices. Access popup and home screen are HQ-only; regional/master may use post-login popup and main notice only.',
      JP: 'お知らせ登録は総本社・本社・総販（画面権限修正以上）のみ可能です。接続ポップアップ・トップ画面は総本社専用、本社・総販はポップアップ・メイン告知のみ利用できます。',
      CH: '仅总本部、本部、总代（具备修改权限）可发布公告。访问弹窗与首页仅总本部可用；本部与总代仅可使用登录后弹窗与主页公告。',
      TH: 'ลงทะเบียนประกาศได้เฉพาะสำนักงานใหญ่·สำนักงานภูมิภาค·ตัวแทนหลัก (สิทธิ์แก้ไข) ป๊อปอัปเมื่อเข้าใช้และหน้าแรกเฉพาะสำนักงานใหญ่ สำนักงานภูมิภาค·ตัวแทนใช้ป๊อปอัปหลังล็อกอินและประกาศหน้าหลักเท่านั้น'
    },
    '배포 대상 「특정지점」은 업체코드/이름 검색 또는 조직레벨→지점 선택으로 추가합니다. 검색·선택은 본인 하위 조직만 가능합니다.': {
      EN: 'For NOTI deploy target, add branches by code/name search or org level → branch. Search and selection are limited to your downstream org.',
      JP: '配信対象「特定店舗」は店舗コード/名称検索または組織レベル→店舗選択で追加します。検索・選択は自組織の下位のみ可能です。',
      CH: '发布对象「特定网点」可通过商户代码/名称搜索或组织层级→网点选择添加。搜索与选择仅限本人下级组织。',
      TH: 'กลุ่มเป้าหมาย NOTI เพิ่มสาขาด้วยค้นหารหัส/ชื่อหรือระดับองค์กร→เลือกสาขา จำกัดเฉพาะองค์กรลูกข่ายของคุณ'
    },
    '작성자': {
      EN: 'Author', JP: '作成者', CH: '作者', TH: 'ผู้เขียน'
    },
    '비고': {
      EN: 'Remarks', JP: '備考', CH: '备注', TH: 'หมายเหตุ'
    },
    '로그인 첫 화면 노출 (AI 다국어·단말 언어)': {
      EN: 'Show on login home (AI i18n · device language)',
      JP: 'ログイン初回画面に表示（AI多言語・端末言語）',
      CH: '登录首页展示（AI 多语言·终端语言）',
      TH: 'แสดงหน้าเข้าสู่ระบบ (AI หลายภาษา·ภาษาเครื่อง)'
    },
    '로그인 팝업 노출 (AI 다국어·단말 언어)': {
      EN: 'Show as login popup (AI i18n · device language)',
      JP: 'ログインポップアップ表示（AI多言語・端末言語）',
      CH: '登录弹窗展示（AI 多语言·终端语言）',
      TH: 'แสดงป๊อปอัปเข้าสู่ระบบ (AI หลายภาษา·ภาษาเครื่อง)'
    },
    '공지 수정': {
      EN: 'Edit notice', JP: 'お知らせ修正', CH: '编辑公告', TH: 'แก้ไขประกาศ'
    },
    '공지 등록': {
      EN: 'New notice', JP: 'お知らせ登録', CH: '发布公告', TH: 'ลงทะเบียนประกาศ'
    },
    '공지 상세': {
      EN: 'Notice detail', JP: 'お知らせ詳細', CH: '公告详情', TH: 'รายละเอียดประกาศ'
    },
    '로그인 첫 화면 공지로 지정되었습니다.': {
      EN: 'Set as login home notice.',
      JP: 'ログイン初回画面のお知らせに設定しました。',
      CH: '已设为登录首页公告。',
      TH: 'ตั้งเป็นประกาศหน้าเข้าสู่ระบบแล้ว'
    },
    '로그인 팝업 공지로 지정되었습니다.': {
      EN: 'Set as login popup notice.',
      JP: 'ログインポップアップのお知らせに設定しました。',
      CH: '已设为登录弹窗公告。',
      TH: 'ตั้งเป็นป๊อปอัปเข้าสู่ระบบแล้ว'
    },
    '목록에서 공지 한 건을 선택(체크)하세요.': {
      EN: 'Select one notice row (checkbox) in the list.',
      JP: '一覧でお知らせ1件を選択（チェック）してください。',
      CH: '请在列表中勾选一条公告。',
      TH: 'เลือกประกาศหนึ่งรายการในรายการ (ติ๊กถูก)'
    },
    '적용되었습니다.': {
      EN: 'Applied.', JP: '適用しました。', CH: '已应用。', TH: 'นำไปใช้แล้ว'
    },
    '적용 실패': {
      EN: 'Apply failed', JP: '適用失敗', CH: '应用失败', TH: 'นำไปใช้ไม่สำเร็จ'
    },
    '수정되었습니다.': {
      EN: 'Updated.', JP: '修正しました。', CH: '已更新。', TH: 'แก้ไขแล้ว'
    },
    '등록되었습니다.': {
      EN: 'Registered.', JP: '登録しました。', CH: '已登记。', TH: 'ลงทะเบียนแล้ว'
    },
    '배포대상': {
      EN: 'Deploy target', JP: '配信対象', CH: '发布对象', TH: 'กลุ่มเป้าหมาย'
    },
    '최근': {
      EN: 'Recent', JP: '最新', CH: '最新', TH: 'ล่าสุด'
    },
    '배포 대상': {
      EN: 'Deploy target', JP: '配信対象', CH: '发布对象', TH: 'กลุ่มเป้าหมาย'
    },
    '배포 대상을 선택하세요.': {
      EN: 'Select a deploy target.',
      JP: '配信対象を選択してください。',
      CH: '请选择发布对象。',
      TH: 'เลือกกลุ่มเป้าหมาย'
    },
    '본사&총판': {
      EN: 'HQ & distributors', JP: '本社&総販', CH: '总部与总代', TH: 'สำนักงANใหญ่และตัวแทน'
    },
    '본사이하': {
      EN: 'HQ and below', JP: '本社以下', CH: '总部及以下', TH: 'สำนักงANใหญ่ลงไป'
    },
    '총판이하': {
      EN: 'Distributor and below', JP: '総販以下', CH: '总代及以下', TH: 'ตัวแทนลงไป'
    },
    '마케팅': {
      EN: 'Marketing (branch/agency/sales)', JP: 'マーケティング', CH: '营销(支社/代理/营业)', TH: 'การตลาด'
    },
    '특정지점': {
      EN: 'Specific branches (NOTI)', JP: '特定拠点', CH: '指定网点', TH: 'สาขาเฉพาะ'
    },
    '특정지점 안내: 업체코드·업체명으로 검색하거나, 조직레벨을 고른 뒤 지점을 선택하여 배포 대상에 추가하세요.': {
      EN: 'Specific branches: search by code/name, or pick org level then branch to add targets.',
      JP: '特定拠点: コード・名称検索、または組織レベル→拠点選択で追加。',
      CH: '指定网点：按代码/名称搜索，或选组织层级后选网点添加。',
      TH: 'สาขาเฉพาะ: ค้นหาหรือเลือกระดับองค์กรแล้วเลือกสาขา'
    },
    '코드/이름 검색': {
      EN: 'Code / name search', JP: 'コード/名称検索', CH: '代码/名称搜索', TH: 'ค้นหารหัส/ชื่อ'
    },
    '업체코드·이름 검색': {
      EN: 'Search by code or name', JP: '店舗コード・名称検索', CH: '按代码/名称搜索', TH: 'ค้นหาด้วยรหัส/ชื่อ'
    },
    '조직레벨별 지점 선택': {
      EN: 'Select branch by org level', JP: '組織レベル別店舗選択', CH: '按组织层级选择网点', TH: 'เลือกสาขาตามระดับองค์กร'
    },
    '조직레벨 선택': {
      EN: 'Select org level', JP: '組織レベル選択', CH: '选择组织层级', TH: 'เลือกระดับองค์กร'
    },
    '조직레벨': {
      EN: 'Org level', JP: '組織レベル', CH: '组织层级', TH: 'ระดับองค์กร'
    },
    '지점 선택': {
      EN: 'Select branch', JP: '拠点選択', CH: '选择网点', TH: 'เลือกสาขา'
    },
    '지점을 선택하세요.': {
      EN: 'Select a branch.', JP: '拠点を選択してください。', CH: '请选择网点。', TH: 'เลือกสาขา'
    },
    '지점 필터(선택)': {
      EN: 'Branch filter (optional)', JP: '拠点フィルタ(任意)', CH: '网点筛选(可选)', TH: 'กรองสาขา(ไม่บังคับ)'
    },
    '코드/이름으로 좁히기': {
      EN: 'Narrow by code/name', JP: 'コード/名称で絞る', CH: '按代码/名称缩小', TH: 'กรองด้วยรหัส/ชื่อ'
    },
    '지점 목록': {
      EN: 'Load branches', JP: '拠点一覧', CH: '加载网点列表', TH: 'โหลดรายการสาขา'
    },
    '선택 지점 추가': {
      EN: 'Add selected branch', JP: '選択拠点を追加', CH: '添加所选网点', TH: 'เพิ่มสาขาที่เลือก'
    },
    '선택된 특정지점': {
      EN: 'Selected branches', JP: '選択した特定拠点', CH: '已选指定网点', TH: 'สาขาที่เลือก'
    },
    '업체코드 또는 업체명': {
      EN: 'Company code or name', JP: '会社コードまたは名称', CH: '公司代码或名称', TH: 'รหัสหรือชื่อบริษัท'
    },
    '업체코드 또는 업체명을 입력하세요.': {
      EN: 'Enter company code or name.', JP: '会社コードまたは名称を入力してください。', CH: '请输入公司代码或名称。', TH: 'ใส่รหัสหรือชื่อบริษัท'
    },
    '불러오는 중…': {
      EN: 'Loading…', JP: '読み込み中…', CH: '加载中…', TH: 'กำลังโหลด…'
    },
    '특정지점(NOTI) 배포 시 대상 업체를 1곳 이상 선택하세요.': {
      EN: 'For NOTI deploy, select at least one target branch.',
      JP: '特定拠点(NOTI)配信では対象を1件以上選択してください。',
      CH: 'NOTI 发布请至少选择一个目标网点。',
      TH: 'NOTI ต้องเลือกสาขาเป้าหมายอย่างน้อย 1'
    },
    '특정지점(NOTI): 업체코드·업체명으로 검색하여 추가하세요. 조직 등급별 최소 선택 단계가 적용됩니다.': {
      EN: 'NOTI: search by code/name and add targets. Minimum org level applies by your role.',
      JP: '特定拠点(NOTI): コード・名称で検索して追加。組織等級ごとの最小選択があります。',
      CH: 'NOTI：按代码/名称搜索添加。按组织等级有最低可选层级。',
      TH: 'NOTI: ค้นหาและเพิ่มตามระดับองค์กรขั้นต่ำ'
    },
    '선택된 배포 대상 업체가 없습니다.': {
      EN: 'No target branches selected.', JP: '選択された配信先がありません。', CH: '未选择目标网点。', TH: 'ยังไม่เลือกเป้าหมาย'
    },
    '공지 등록은 총본사·본사·총판(화면 권한 수정 이상)만 가능합니다. 작성 시 배포 대상을 선택하며, 총판 공지는 해당 총판 하위(지사·대리점·영업점·가맹점)에만 노출됩니다.': {
      EN: 'Only super-HQ/HQ/distributor (modify+ on this screen) can post. Distributor notices show only under that distributor tree.',
      JP: 'お知らせ登録は総本社・本社・総販(修正以上)のみ。総販のお知らせは自組織配下のみ表示。',
      CH: '仅总本部/总部/总代(修改权限以上)可发。总代公告仅在其下级显示。',
      TH: 'ประกาศได้เฉพาะสำนักงANใหญ่/ตัวแทน(แก้ไข+) ตัวแทนเห็นเฉพาะลูกข่าย'
    },
    '총본사·본사는 본사/총판/마케팅/가맹점/ALL/특정지점 등 배포 범위를 선택할 수 있습니다. 특정지점(NOTI)은 조직 등급별 최소 선택 단계(본사→본사부터, 본사 작성→총판부터, 총판 작성→대리점부터)가 적용됩니다.': {
      EN: 'Super-HQ/HQ can pick deploy scope. NOTI min level: super-HQ from regional, HQ from distributor, distributor from agency.',
      JP: '総本社・本社は配信範囲を選択。NOTIは等級別最小選択(本社から/総販から/代理店から)。',
      CH: '总本部/总部可选发布范围。NOTI 按等级有最低可选层级。',
      TH: 'เลือกขอบเขต NOTI ตามระดับองค์กร'
    },
    '삭제되었습니다.': {
      EN: 'Deleted.', JP: '削除しました。', CH: '已删除。', TH: 'ลบแล้ว'
    },
    '삭제 실패': {
      EN: 'Delete failed', JP: '削除失敗', CH: '删除失败', TH: 'ลบไม่สำเร็จ'
    },
    '거래 21·40 무효, 22·41 수동무효, 30·42 환불·자동환불, 31 강제환불 각각에 대해 순매출·이중 과금(성공 건당·%) 방식을 둡니다. 일반형은 승인 시 성공 수수료만, 무효·환불 건에는 무효/환불 건당만 과금합니다. 수익형은 순매출 미차감·무효·환불에도 성공 수수료를 다시 붙입니다. 하이브리드1·2는 무효 계열과 환불 계열을 나눕니다. 31 강제환불만 차지백 수수료(구간·건당)가 부과됩니다.': {
      EN: 'For void 21/40, manual void 22/41, refund/auto-refund 30/42, and force-refund 31, set net-sales and double-charge (per-txn success fees) rules. Standard: success fees on approval only; void/refund rows charge void/refund per-txn only. Revenue: no net deduction; success fees charged again on void/refund. Hybrid 1/2 split void vs refund families. Only 31 may incur chargeback fees (tier or per txn).',
      JP: '取引21/40無効、22/41手動無効、30/42返金・自動返金、31強制返金について、純売上・二重課金（成功件当・%）方式を設定します。一般型は承認時の成功手数料のみ、無効・返金は件当のみ。収益型は純売上未控除・成功手数料を再課金。ハイブリッド1/2で無効系と返金系を分けます。31のみCB手数料。',
      CH: '针对 21/40 无效、22/41 手动作废、30/42 退款/自动退款、31 强制退款，分别设置净销售额与双重计费（成功按笔·%）。一般型仅批准时收成功手续费；无效/退款仅按笔。收益型不扣净销售额且再次收成功手续费。混合1/2区分无效系与退款系。仅 31 可收拒付手续费。',
      TH: 'ตั้งยอดขายสุทธิและเก็บซ้ำสำหรับโมฆะ/คืน 31 มี chargeback มาตรฐานเก็บซ้ำไม่ รายได้เก็บซ้ำ ไฮบริด1/2 แยกประเภท'
    },
    '총판(MASTER_DIST)마다 무효·수동무효·환불·강제환불 정산 방식을 둡니다. 비우면 본사 기본과 동일합니다. 가맹 「총판·본사 따름」이면 총판 값(없으면 본사)을 따르고, 가맹에서 모드를 고르면 가맹이 우선합니다.': {
      EN: 'Per MASTER_DIST, set void/manual void/refund/force-refund settlement modes. Empty = HQ default. If merchant follows distributor/HQ, use distributor value (else HQ); explicit merchant choice wins.',
      JP: 'MASTER_DISTごとに無効・手動無効・返金・強制返金の精算方式を設定。空欄は本社デフォルトと同じ。「総販・本社に従う」は総販値（なければ本社）、加盟店で選べば加盟店優先。',
      CH: '按总代设置无效/手动作废/退款/强制退款结算方式。留空等同总部默认。商户「跟随总代/总部」用总代值（无则总部）；商户自选优先。',
      TH: 'ต่อตัวแทนหลัก ตั้งโมฆะ/คืน ว่างเท่า HQ ร้านตามตัวแทน ร้านเลือกเองชนะ'
    },
    /* HQ /hq/receivableRecoverySettings — 回収・未収設定 */
    '거래 21·40 무효, 22·41 수동무효, 30·42 환불·자동환불, 31 강제환불 각각에 대해 순매출 반영 방식을 둡니다. 31 강제환불만 차지백 수수료(구간 정책 또는 건당)가 부과되며, 30·42는 환불 건당 수수료만 적용됩니다. 가맹이 본사정책 따름이면 템플릿에 저장된 값이 복사됩니다. 가맹 직접입력에서 「본사 따름」을 선택하면 이 본사 기본을 사용합니다.': {
      EN: 'For txn codes 21/40 void, 22/41 manual void, 30/42 refund & auto-refund, and 31 force-refund, choose how each affects net sales. Only 31 force-refund may incur chargeback fees (tier policy or per txn); 30/42 use refund per-txn fees only. Merchants following HQ policy copy template values; choosing “Follow HQ” in merchant entry uses these HQ defaults.',
      JP: '取引21/40無効、22/41手動無効、30/42返金・自動返金、31強制返金それぞれについて純売上への反映方式を設定します。31強制返金のみチャージバック手数料（段階政策または件別）が課され、30/42は返金件別手数料のみです。加盟店が本社方針に従う場合はテンプレ値がコピーされ、「本社に従う」を選べば本デフォルトを使います。',
      CH: '针对 21/40 无效、22/41 手动作废、30/42 退款与自动退款、31 强制退款分别设置对净销售额的影响。仅 31 可产生退单手续费（阶梯或按笔）；30/42 仅退款项手续费。商户跟随总部策略时复制模板值；商户录入中选择「跟随总部」则使用此处默认。',
      TH: 'กำหนดผลต่อยอดขายสุทธิสำหรับโมฆะ 21/40, 22/41, คืน 30/42, บังคับคืน 31 เฉพาะ 31 มีค่าธรรมเนียม chargeback 30/42 คิดตามธุรกรรม ร้านตาม HQ คัดลอกจากเทมเพลต'
    },
    '총판(MASTER_DIST)마다 무효·수동무효·환불·강제환불 순매출 반영 방식을 둡니다. 비우면 본사 기본과 동일합니다. 가맹점등록의 수수료정책에서 「총판·본사 따름」이면 여기 저장된 총판 값(없으면 본사)을 따르고, 가맹에서 모드를 고르면 가맹이 우선합니다.': {
      EN: 'Per MASTER_DIST, set void / manual void / refund / force-refund net-sales modes. Empty means same as HQ default. If merchant fee policy is “Follow distributor/HQ”, saved distributor values here apply (else HQ); if the merchant picks a mode explicitly, the merchant wins.',
      JP: 'MASTER_DISTごとに無効・手動無効・返金・強制返金の純売上反映を設定します。空欄は本社デフォルトと同じです。加盟店登録の手数料政策で「総販・本社に従う」の場合はここに保存した総販値（なければ本社）を使い、加盟店側でモードを選べば加盟店が優先されます。',
      CH: '按总代设置无效/手动作废/退款/强制退款对净销售额的影响。留空等同总部默认。商户手续费策略为「跟随总代/总部」时使用此处总代值（无则用总部）；商户自行选择模式则以商户为准。',
      TH: 'ต่อตัวแทนหลัก ตั้งโมฆะ/คืน ฯลฯ ว่างเท่า HQ ถ้าร้านเลือกตามตัวแทนใช้ค่าที่บันทึก ร้านเลือกเองชนะ'
    },
    '자동이면 미수금이 생긴 뒤 다음 정산 실행에서 지급액에 FIFO로 반영됩니다. 수동이면 다음 정산에 자동 반영하지 않고 잔액이 쌓이며, 미수금관리 화면에서 환수처리를 누른 건만 차기 정산에서 차감됩니다. 저장 시 아래 체크를 켜면 모든 가맹 tb_settlement_setting.receivable_recovery_mode도 같은 값으로 갱신됩니다(개별 오버라이드가 아닌 가맹만; 아래 「가맹」에서 가맹별로 다시 조정 가능).': {
      EN: 'AUTO: after a receivable is created, the next settlement run applies it FIFO to payout. MANUAL: balances accrue without auto-apply on the next run; only rows you mark for recovery on the receivables screen deduct on the following settlement. If the checkbox below is checked when saving, all merchants’ tb_settlement_setting.receivable_recovery_mode is updated to the same value (merchants without individual override only; adjust per merchant under “Merchant”).',
      JP: '自動なら未収が発生した次の精算実行で支払額にFIFO反映されます。手動なら次の精算に自動反映せず残高が積み、「未収金管理」で回収処理を押した件だけ次回精算で控除されます。保存時に下のチェックをオンにすると、個別上書きでない全加盟店の tb_settlement_setting.receivable_recovery_mode も同じ値に更新されます（下の「加盟店」で個別調整可）。',
      CH: '自动：产生应收后，下次结算执行按 FIFO 计入支付额。手动：不在下次结算自动冲减，余额累积；仅在「应收管理」中点击回收处理的行在后续结算扣除。保存时勾选下方复选框会将所有无单独覆盖的商户 tb_settlement_setting.receivable_recovery_mode 同步为相同值（可在下方「商户」逐项调整）。',
      TH: 'AUTO หัก FIFO รอบถัด MANUAL สะสม หักเฉพาะที่กดกู้ในเมนูลูกหนี้ บันทึกพร้อมติ๊กจะซิงก์ receivable_recovery_mode ให้ร้านที่ไม่มี override'
    },
    '총판(MASTER_DIST)마다 자동/수동을 두고, 소속 가맹은 기본으로 그 값을 따릅니다. 특정 가맹만 개별로 바꾸면 가맹 설정이 우선합니다. 수동이면 「미수금관리」에서 환수처리 요청 건만 다음 정산 마감 시 차감되고, 자동이면 정산 시 FIFO로 차감합니다. 위 「미수금관리설정 (본사 기본)」이 총판·가맹 상속의 출발값이 됩니다(가맹 개별 오버라이드 제외).': {
      EN: 'Per MASTER_DIST, choose AUTO/MANUAL; child merchants inherit by default. Per-merchant overrides win. MANUAL: only recovery requests from “Receivables” deduct at the next settlement close; AUTO: FIFO at settlement time. The “HQ receivables default” card above is the inheritance root (except per-merchant overrides).',
      JP: 'MASTER_DISTごとに自動/手動を置き、所属加盟店は既定でそれに従います。個別変更した加盟店設定が優先します。手動なら「未収金管理」の回収処理依頼分だけ次回精算締めで控除、自動なら精算時FIFO控除です。上の「未収金管理設定（本社デフォルト）」が総販・加盟店継承の起点です（個別上書き除く）。',
      CH: '各总代设自动/手动，下属商户默认继承。商户单独设置优先。手动：仅「应收管理」中的回收请求在下次结算截止时扣除；自动：结算时 FIFO 扣除。上方「应收管理设置（总部默认）」为继承起点（商户单独覆盖除外）。',
      TH: 'ต่อตัวแทนหลัก AUTO/MANUAL ร้านสืบทอด ร้าน override ชนะ MANUAL หักตามคำขอกู้ในเมนูลูกหนี้'
    },
    '무효·환불 정산 방식 (본사 기본)': {
      EN: 'Void / refund settlement (HQ default)',
      JP: '無効・返金精算方式（本社デフォルト）',
      CH: '无效/退款结算方式（总部默认）',
      TH: 'โมฆะ/คืนเงิน (ค่าเริ่ม HQ)'
    },
    '무효 (21·40)': {
      EN: 'Void (21·40)',
      JP: '無効 (21·40)',
      CH: '无效 (21·40)',
      TH: 'โมฆะ (21·40)'
    },
    '수동무효 (22·41)': {
      EN: 'Manual void (22·41)',
      JP: '手動無効 (22·41)',
      CH: '手动作废 (22·41)',
      TH: 'โมฆะมือ (22·41)'
    },
    '환불 (30·42)': {
      EN: 'Refund (30·42)',
      JP: '返金 (30·42)',
      CH: '退款 (30·42)',
      TH: 'คืนเงิน (30·42)'
    },
    '강제환불 (31)': {
      EN: 'Force refund (31)',
      JP: '強制返金 (31)',
      CH: '强制退款 (31)',
      TH: 'บังคับคืน (31)'
    },
    '무효·환불 정산 방식 (총판별)': {
      EN: 'Void / refund settlement (per distributor)',
      JP: '無効・返金精算方式（総販別）',
      CH: '无效/退款结算方式（按总代）',
      TH: 'โมฆะ/คืน (ตามตัวแทน)'
    },
    '총판 저장': {
      EN: 'Save distributor',
      JP: '総販を保存',
      CH: '保存总代',
      TH: 'บันทึกตัวแทนหลัก'
    },
    '미수금관리설정 (본사 기본)': {
      EN: 'Receivables policy (HQ default)',
      JP: '未収金管理設定（本社デフォルト）',
      CH: '应收管理设置（总部默认）',
      TH: 'การตั้งค่าลูกหนี้ (ค่าเริ่ม HQ)'
    },
    '미수금처리 방식': {
      EN: 'Receivable handling mode',
      JP: '未収金の処理方式',
      CH: '应收处理方式',
      TH: 'โหมดจัดการลูกหนี้'
    },
    '자동': {
      EN: 'Auto',
      JP: '自動',
      CH: '自动',
      TH: 'อัตโนมัติ'
    },
    '모든 가맹 정산설정에 동일 적용': {
      EN: 'Apply the same value to all merchant settlement settings',
      JP: '全加盟店の精算設定に同一適用',
      CH: '同步应用到所有商户结算设置',
      TH: 'ใช้ค่าเดียวกับการตั้งค่าชำระทุกร้าน'
    },
    '미수금관리(수동 환수처리)로 이동': {
      EN: 'Go to receivables (manual recovery)',
      JP: '未収金管理（手動回収処理）へ',
      CH: '前往应收管理（手动回收）',
      TH: 'ไปหน้าลูกหนี้ (กู้ด้วยมือ)'
    },
    '환수 / 미수금 설정': {
      EN: 'Recovery / receivables settings',
      JP: '回収 / 未収金設定',
      CH: '回收 / 应收设置',
      TH: 'การตั้งค่ากู้คืน / ลูกหนี้'
    },
    '총판 — 소속 가맹 기본': {
      EN: 'Distributor — default for child merchants',
      JP: '総販 — 所属加盟店の既定',
      CH: '总代 — 下属商户默认',
      TH: 'ตัวแทนหลัก — ค่าเริ่มร้านใต้สาย'
    },
    '모드': {
      EN: 'Mode',
      JP: 'モード',
      CH: '模式',
      TH: 'โหมด'
    },
    '가맹 — 총판과 동일 또는 개별': {
      EN: 'Merchant — same as distributor or individual',
      JP: '加盟店 — 総販と同じまたは個別',
      CH: '商户 — 与总代相同或单独',
      TH: 'ร้าน — เหมือนตัวแทนหรือแยก'
    },
    '총판·본사 설정 따름': {
      EN: 'Follow distributor / HQ settings',
      JP: '総販・本社設定に従う',
      CH: '跟随总代/总部设置',
      TH: 'ตามตัวแทน/HQ'
    },
    '개별 모드': {
      EN: 'Override mode',
      JP: '個別モード',
      CH: '单独模式',
      TH: 'โหมดเฉพาะร้าน'
    },
    '가맹 저장': {
      EN: 'Save merchant',
      JP: '加盟店を保存',
      CH: '保存商户',
      TH: 'บันทึกร้าน'
    },
    '가맹을 선택하면 유효 모드·상속 출처가 여기에 표시됩니다.': {
      EN: 'Select a merchant to see effective mode and inheritance source here.',
      JP: '加盟店を選ぶと有効モード・継承元がここに表示されます。',
      CH: '选择商户后，此处显示生效模式与继承来源。',
      TH: 'เลือกร้านเพื่อดูโหมดที่ใช้และแหล่งที่มา'
    },
    '유효 모드가 수동인 가맹': {
      EN: 'Merchants whose effective mode is manual',
      JP: '有効モードが手動の加盟店',
      CH: '生效模式为手动的商户',
      TH: 'ร้านที่โหมดที่ใช้เป็น MANUAL'
    },
    'URL구분': {
      EN: 'URL type',
      JP: 'URL区分',
      CH: 'URL类型',
      TH: 'ประเภท URL'
    },
    '통보URL': {
      EN: 'Notify URL',
      JP: '通報URL',
      CH: '通知 URL',
      TH: 'URL แจ้งเตือน'
    },
    '현금영수증 통보URL': {
      EN: 'Cash receipt notify URL',
      JP: '現金領収通知URL',
      CH: '现金收据通知 URL',
      TH: 'URL แจ้งใบเสร็จ'
    },
    '현금영수증통보 URL': {
      EN: 'Cash receipt notify URL',
      JP: '現金領収通知URL',
      CH: '现金收据通知 URL',
      TH: 'URL แจ้งใบเสร็จ'
    },
    '내림차순': {
      EN: 'Descending',
      JP: '降順',
      CH: '降序',
      TH: 'จากมากไปน้อย'
    },
    '오름차순': {
      EN: 'Ascending',
      JP: '昇順',
      CH: '升序',
      TH: 'จากน้อยไปมาก'
    },
    '정렬 순서': {
      EN: 'Sort order',
      JP: '並び順',
      CH: '排序',
      TH: 'ลำดับการเรียง'
    },
    '상세 정렬 순서': {
      EN: 'Detail sort order',
      JP: '詳細の並び順',
      CH: '明细排序',
      TH: 'ลำดับเรียงรายละเอียด'
    },
    '검색': {
      EN: 'Search',
      JP: '検索',
      CH: '搜索',
      TH: 'ค้นหา'
    },
    '업체코드': {
      EN: 'Company code',
      JP: '店番号',
      CH: '商户代码',
      TH: 'รหัสร้าน'
    },
    '업체명': {
      EN: 'Company name',
      JP: '店名',
      CH: '商户名称',
      TH: 'ชื่อร้าน'
    },
    '출처': {
      EN: 'Source',
      JP: '出所',
      CH: '来源',
      TH: 'ที่มา'
    },
    '선택…': {
      EN: 'Select…',
      JP: '選択…',
      CH: '请选择…',
      TH: 'เลือก…'
    },
    '본사 기본과 동일(비움)': {
      EN: 'Same as HQ default (empty)',
      JP: '本社デフォルトと同じ（空欄）',
      CH: '与总部默认相同（留空）',
      TH: 'เหมือน HQ (ว่าง)'
    },
    '가맹 개별': {
      EN: 'Merchant override',
      JP: '加盟店個別',
      CH: '商户单独',
      TH: 'ร้านแยกตั้งค่า'
    },
    '총판 설정': {
      EN: 'Distributor setting',
      JP: '総販設定',
      CH: '总代设置',
      TH: 'ตั้งค่าตัวแทนหลัก'
    },
    '본사 기본': {
      EN: 'HQ default',
      JP: '本社デフォルト',
      CH: '总部默认',
      TH: 'ค่าเริ่ม HQ'
    },
    '저장되었습니다.': {
      EN: 'Saved.',
      JP: '保存しました。',
      CH: '已保存。',
      TH: 'บันทึกแล้ว'
    },
    '미수금관리설정이 저장되었습니다.': {
      EN: 'Receivables policy saved.',
      JP: '未収金管理設定を保存しました。',
      CH: '应收管理设置已保存。',
      TH: 'บันทึกการตั้งค่าลูกหนี้แล้ว'
    },
    '유효: {EFF} · 상속(총판/본사): {INH} · 소속 총판: {MD} · 개별오버라이드: {OVR}': {
      EN: 'Effective: {EFF} · Inherited (dist./HQ): {INH} · Distributor: {MD} · Override: {OVR}',
      JP: '有効: {EFF} · 継承(総販/本社): {INH} · 所属総販: {MD} · 個別上書き: {OVR}',
      CH: '生效：{EFF} · 继承（总代/总部）：{INH} · 所属总代：{MD} · 单独覆盖：{OVR}',
      TH: 'ใช้: {EFF} · สืบทอด: {INH} · ตัวแทน: {MD} · override: {OVR}'
    },
    '(총판 없음)': {
      EN: '(no distributor)',
      JP: '（総販なし）',
      CH: '（无总代）',
      TH: '(ไม่มีตัวแทน)'
    },
    '아니오(총판·본사 따름)': {
      EN: 'No (follow distributor/HQ)',
      JP: 'いいえ（総販・本社に従う）',
      CH: '否（跟随总代/总部）',
      TH: 'ไม่ (ตามตัวแทน/HQ)'
    },
    '없음': {
      EN: 'None',
      JP: 'なし',
      CH: '无',
      TH: 'ไม่มี'
    },
    '가맹점 선택란을 찾을 수 없습니다. 화면을 새로 열어 주세요.': {
      EN: 'Merchant selector not found. Re-open this screen.',
      JP: '加盟店選択欄が見つかりません。画面を開き直してください。',
      CH: '未找到商户选择框，请重新打开本页面。',
      TH: 'ไม่พบช่องเลือกร้าน เปิดหน้าใหม่'
    },

    /* HQ /hq/domainConfig — 도메인구성 (screens L + app.js pgAdminUiT) */
    'Let\u2019s Encrypt · 도메인구성설정 연동': {
      EN: 'Let\u2019s Encrypt · Domain configuration linkage',
      JP: 'Let\u2019s Encrypt · ドメイン構成連携',
      CH: 'Let\u2019s Encrypt · 域名配置联动',
      TH: 'Let\u2019s Encrypt · การเชื่อมโยงการตั้งค่าโดเมน'
    },
    '전사 기본 URL': {
      EN: 'Company-wide default URL',
      JP: '全社デフォルトURL',
      CH: '全公司默认 URL',
      TH: 'URL เริ่มต้นทั้งองค์กร'
    },
    '노티·문서·가맹점 안내에 쓰는 기본 공개 URL입니다. 저장은 시스템 관리자(ADMIN)만 가능합니다.': {
      EN: 'Default public URLs used for notify, documents, and merchant notices. Only system administrators (ADMIN) can save.',
      JP: 'ノティ・文書・加盟店案内に使う公開URLの既定です。保存はシステム管理者(ADMIN)のみ可能です。',
      CH: '用于通知、文档与商户提示的默认公开 URL。仅系统管理员(ADMIN)可保存。',
      TH: 'URL สาธารณะเริ่มต้นสำหรับแจ้งเตือน เอกสาร และข้อความถึงร้าน บันทึกได้เฉพาะผู้ดูแลระบบ (ADMIN)'
    },
    '관리자(웹) 공개 URL': {
      EN: 'Public admin (web) URL',
      JP: '管理画面（Web）公開URL',
      CH: '管理端（Web）公开 URL',
      TH: 'URL สาธารณะแอดมิน (เว็บ)'
    },
    '관리자(웹) URL': {
      EN: 'Admin (web) URL',
      JP: '管理（Web）URL',
      CH: '管理端（Web）URL',
      TH: 'URL แอดมิน (เว็บ)'
    },
    'API 공개 베이스 URL': {
      EN: 'Public API base URL',
      JP: 'API公開ベースURL',
      CH: 'API 公开基址 URL',
      TH: 'ฐาน URL สาธารณะของ API'
    },
    '전사 URL 저장': {
      EN: 'Save company-wide URLs',
      JP: '全社URLを保存',
      CH: '保存全公司 URL',
      TH: 'บันทึก URL ทั้งองค์กร'
    },
    '이 서버의 <code>fullchain.pem</code> 에서 읽은 <strong>SAN(호스트명)</strong>과, 전사 URL·본사·총판에 저장된 URL의 호스트를 비교합니다. 표시·저장 시 주소에 <code>http://</code> 또는 <code>https://</code> 가 없으면 <strong>https://</strong> 를 붙입니다. 불일치 시 브라우저 인증서 경고가 날 수 있습니다. 서브도메인 추가 시 DNS A 레코드·Nginx <code>server_name</code>·<code>certbot --nginx -d …</code> 를 함께 적용하세요. 상세 SSL 경로·Certbot 타이머는 <strong>본사설정 → 서버운영관리</strong>를 참고하세요.': {
      EN: 'This compares <strong>SAN (hostnames)</strong> read from this server\u2019s <code>fullchain.pem</code> with hosts from the company-wide URLs and HQ / master-distributor URLs. When displaying or saving, if the address has no <code>http://</code> or <code>https://</code>, <strong>https://</strong> is prefixed. Mismatches may trigger browser certificate warnings. When adding a subdomain, apply DNS A records, Nginx <code>server_name</code>, and <code>certbot --nginx -d …</code> together. For SSL paths and Certbot timers, see <strong>HQ settings → Server operations</strong>.',
      JP: '本サーバの<code>fullchain.pem</code>から読んだ<strong>SAN（ホスト名）</strong>と、全社URL・本社・総販に保存したURLのホストを突き合わせます。表示・保存時、アドレスに<code>http://</code>または<code>https://</code>がなければ<strong>https://</strong>を付与します。不一致時はブラウザの証明書警告が出ることがあります。サブドメイン追加時はDNSのAレコード・Nginxの<code>server_name</code>・<code>certbot --nginx -d …</code>を併せて適用してください。SSLパスやCertbotタイマーの詳細は<strong>本社設定 → サーバ運用管理</strong>を参照してください。',
      CH: '将本服务器 <code>fullchain.pem</code> 中读取的 <strong>SAN（主机名）</strong> 与全公司 URL、总部/总代已保存 URL 的主机进行比较。显示与保存时若地址没有 <code>http://</code> 或 <code>https://</code>，会加上 <strong>https://</strong>。不一致可能导致浏览器证书警告。新增子域名时请同时配置 DNS A 记录、Nginx <code>server_name</code>、<code>certbot --nginx -d …</code>。SSL 路径与 Certbot 定时任务详见 <strong>总部设置 → 服务器运维</strong>。',
      TH: 'เปรียบเทียบ <strong>SAN (โฮสต์)</strong> จาก <code>fullchain.pem</code> ของเซิร์ฟเวอร์นี้กับโฮสต์จาก URL ทั้งองค์กรและสำนักงานใหญ่/ตัวแทนหลัก ตอนแสดงหรือบันทึก หากไม่มี <code>http://</code> หรือ <code>https://</code> จะเติม <strong>https://</strong> ไม่ตรงกันอาจมีคำเตือนใบรับรอง กรณีเพิ่มซับโดเมนให้ตั้ง DNS A, Nginx <code>server_name</code>, <code>certbot --nginx -d …</code> พร้อมกัน รายละเอียด SSL และ Certbot ดูที่ <strong>ตั้งค่า HQ → ดูแลเซิร์ฟเวอร์</strong>'
    },
    '본사·총판 도메인 설정': {
      EN: 'HQ / master-distributor domain settings',
      JP: '本社・総販ドメイン設定',
      CH: '总部/总代域名设置',
      TH: 'การตั้งค่าโดเมน HQ / ตัวแทนหลัก'
    },
    '업체명에서 <strong>본사</strong> 또는 <strong>총판</strong>만 선택할 수 있습니다. 선택 후 설정 이름·URL을 입력하고 [설정저장]하면 하단 목록에 반영됩니다. URL에 스킴이 없으면 <strong>https://</strong> 가 자동으로 붙습니다. <strong>본사</strong> 관리자 URL: <strong>총본사·해당 본사</strong> 소속 계정만 로그인됩니다(하위 총판·가맹 등은 불가). <strong>총판</strong> URL: <strong>총본사·이 총판을 소속 트리에 두는 본사·해당 총판 및 그 하위</strong>만 로그인됩니다(다른 총판·다른 본사 트리는 불가). 브랜딩(로그인 화면 등)은 접속한 URL에 매칭된 본사·총판 조직의 설정을 따릅니다.': {
      EN: 'Under company name, only <strong>HQ</strong> or <strong>master distributor</strong> can be selected. After selection, enter a setting name and URLs, then <strong>[Save settings]</strong> to update the list below. If a URL has no scheme, <strong>https://</strong> is added automatically. <strong>HQ</strong> admin URL: only accounts under <strong>head office and that HQ org</strong> may log in (subordinate distributors or merchants cannot). <strong>Master distributor</strong> URL: only <strong>head office, HQs that include this distributor in their tree, that distributor, and their descendants</strong> may log in (other distributors or other HQ trees cannot). Branding (login screen, etc.) follows the HQ / distributor org matched to the URL you use.',
      JP: '店名から<strong>本社</strong>または<strong>総販</strong>のみ選択できます。選択後に設定名・URLを入力し［設定を保存］で下の一覧に反映されます。URLにスキームがなければ<strong>https://</strong>を自動付与します。<strong>本社</strong>管理URLは<strong>本社および当該本社</strong>に所属するアカウントのみログイン可能です（配下の総販・加盟店などは不可）。<strong>総販</strong>URLは<strong>本社・この総販をツリーに含む本社・当該総販およびその配下</strong>のみログイン可能です（他総販・他本社ツリーは不可）。ブランディング（ログイン画面など）はアクセスしたURLに一致した本社・総販組織の設定に従います。',
      CH: '在商户名称中仅可选择<strong>总部</strong>或<strong>总代</strong>。选择后输入设置名称与 URL，点击<strong>[保存设置]</strong>更新下方列表。URL 无协议时自动添加 <strong>https://</strong>。<strong>总部</strong>管理端 URL：仅<strong>总部及该总部组织</strong>下属账号可登录（下级总代、商户等不可）。<strong>总代</strong> URL：仅<strong>总部、在组织树中包含该总代的总部、该总代及其下级</strong>可登录（其他总代或其他总部树不可）。品牌（登录页等）遵循与访问 URL 匹配的总部/总代组织设置。',
      TH: 'ในรายชื่อร้านเลือกได้เฉพาะ<strong>สำนักงานใหญ่</strong>หรือ<strong>ตัวแทนหลัก</strong> หลังเลือกให้กรอกชื่อการตั้งค่าและ URL แล้วกด<strong>[บันทึกการตั้งค่า]</strong>เพื่ออัปเดตตารางด้านล่าง หาก URL ไม่มีสคีมจะเติม <strong>https://</strong> อัตโนมัติ URL แอดมิน<strong>สำนักงานใหญ่</strong>: เฉพาะบัญชีภายใต้<strong>สำนักงานใหญ่และสำนักงานใหญ่นั้น</strong> (ไม่รวมตัวแทน/ร้านลูก) URL<strong>ตัวแทนหลัก</strong>: เฉพาะ<strong>สำนักงานใหญ่ สำนักงานใหญ่ที่มีตัวแทนนี้ในทรี ตัวแทนนั้น และลูกสาย</strong> แบรนด์ (หน้าเข้าสู่ระบบ ฯลฯ) ตามองค์กร HQ/ตัวแทนที่จับคู่กับ URL'
    },
    '표시용 이름': {
      EN: 'Display name',
      JP: '表示用の名前',
      CH: '显示用名称',
      TH: 'ชื่อที่แสดง'
    },
    '설정 이름': {
      EN: 'Setting name',
      JP: '設定名',
      CH: '设置名称',
      TH: 'ชื่อการตั้งค่า'
    },
    'API URL': {
      EN: 'API URL',
      JP: 'API URL',
      CH: 'API URL',
      TH: 'API URL'
    },
    '설정저장': {
      EN: 'Save settings',
      JP: '設定を保存',
      CH: '保存设置',
      TH: 'บันทึกการตั้งค่า'
    },
    '업체를 선택하면 입력란이 활성화됩니다.': {
      EN: 'Select a company to enable the input fields.',
      JP: '加盟店を選ぶと入力欄が有効になります。',
      CH: '选择公司后输入框将启用。',
      TH: 'เลือกร้านเพื่อเปิดช่องกรอก'
    },
    '수정일시': {
      EN: 'Updated at',
      JP: '更新日時',
      CH: '修改时间',
      TH: 'อัปเดตเมื่อ'
    },
    '등록된 본사·총판 조직이 없습니다.': {
      EN: 'No HQ / master-distributor organizations are registered.',
      JP: '登録された本社・総販組織がありません。',
      CH: '暂无已登记的总部/总代组织。',
      TH: 'ยังไม่มีองค์กร HQ / ตัวแทนหลักที่ลงทะเบียน'
    },
    '연동 요약을 불러오지 못했습니다.': {
      EN: 'Could not load the linkage summary.',
      JP: '連携サマリを読み込めませんでした。',
      CH: '无法加载联动摘要。',
      TH: 'โหลดสรุปการเชื่อมโยงไม่สำเร็จ'
    },
    '인증서 SAN에 없는 호스트': {
      EN: 'Hosts not in certificate SAN',
      JP: '証明書SANにないホスト',
      CH: '不在证书 SAN 中的主机',
      TH: 'โฮสต์ที่ไม่อยู่ใน SAN ของใบรับรอง'
    },
    'URL은 저장됐으나 PEM의 SAN과 불일치': {
      EN: 'URL saved but PEM SAN mismatch',
      JP: 'URLは保存済みだがPEMのSANと不一致',
      CH: 'URL 已保存但与 PEM 的 SAN 不一致',
      TH: 'บันทึก URL แล้วแต่ SAN ของ PEM ไม่ตรง'
    },
    '호스트명': {
      EN: 'Hostname',
      JP: 'ホスト名',
      CH: '主机名',
      TH: 'ชื่อโฮสต์'
    },
    'SAN 포함': {
      EN: 'In SAN',
      JP: 'SANに含む',
      CH: '含于 SAN',
      TH: 'อยู่ใน SAN'
    },
    '비교할 URL이 없습니다. 전사 URL 또는 본사·총판 URL을 입력하세요.': {
      EN: 'No URLs to compare. Enter the company-wide URL or HQ / distributor URLs.',
      JP: '比較するURLがありません。全社URLまたは本社・総販のURLを入力してください。',
      CH: '没有可比较的 URL。请输入全公司 URL 或总部/总代 URL。',
      TH: 'ไม่มี URL ให้เปรียบเทียบ กรอก URL ทั้งองค์กรหรือ HQ/ตัวแทน'
    },
    '— 업체를 선택하세요 —': {
      EN: '— Select a company —',
      JP: '— 加盟店を選択してください —',
      CH: '— 请选择公司 —',
      TH: '— เลือกร้าน —'
    },
    'No.': {
      EN: 'No.',
      JP: 'No.',
      CH: '序号',
      TH: 'ลำดับ'
    },
    '전사 관리자(웹) 공개 URL': {
      EN: 'Company-wide public admin (web) URL',
      JP: '全社の管理画面（Web）公開URL',
      CH: '全公司管理端（Web）公开 URL',
      TH: 'URL สาธารณะแอดมิน (เว็บ) ทั้งองค์กร'
    },
    '전사 API 공개 베이스 URL': {
      EN: 'Company-wide public API base URL',
      JP: '全社のAPI公開ベースURL',
      CH: '全公司 API 公开基址 URL',
      TH: 'ฐาน URL สาธารณะของ API ทั้งองค์กร'
    },
    '조직 · 관리자 URL': {
      EN: 'Organization · Admin URL',
      JP: '組織 · 管理URL',
      CH: '组织 · 管理 URL',
      TH: 'องค์กร · URL แอดมิน'
    },
    '조직: {0} · 관리자 URL': {
      EN: 'Organization: {0} · Admin URL',
      JP: '組織: {0} · 管理URL',
      CH: '组织：{0} · 管理 URL',
      TH: 'องค์กร: {0} · URL แอดมิน'
    },
    '조직 · API URL': {
      EN: 'Organization · API URL',
      JP: '組織 · API URL',
      CH: '组织 · API URL',
      TH: 'องค์กร · URL API'
    },
    '조직: {0} · API URL': {
      EN: 'Organization: {0} · API URL',
      JP: '組織: {0} · API URL',
      CH: '组织：{0} · API URL',
      TH: 'องค์กร: {0} · URL API'
    },
    '연동 PEM 상태|OK': {
      EN: 'OK',
      JP: 'OK',
      CH: '正常',
      TH: 'ปกติ'
    },
    '연동 PEM 상태|N/A': {
      EN: 'N/A',
      JP: 'N/A',
      CH: '不适用',
      TH: 'ไม่มี'
    },
    '연동 PEM 상태|ERROR': {
      EN: 'ERROR',
      JP: 'ERROR',
      CH: '错误',
      TH: 'ข้อผิดพลาด'
    },
    '도메인구성설정 URL의 호스트명이 인증서 SAN에 없으면 HTTPS 경고가 납니다. SAN에만 있고 여기 미기재인 호스트는 운영용으로 쓰는지 검토하세요.': {
      EN: 'If a domain configuration URL hostname is not in the certificate SAN, browsers may warn on HTTPS. Hostnames that appear only in the SAN and not here should be reviewed to see if they are used in production.',
      JP: 'ドメイン構成URLのホスト名が証明書SANにないとHTTPSで警告が出ることがあります。SANにのみありここに未記載のホストは本番利用か確認してください。',
      CH: '若域名配置 URL 的主机名不在证书 SAN 中，HTTPS 可能出现浏览器警告。仅在 SAN 中出现、此处未登记的主机请评估是否用于生产。',
      TH: 'ถ้าโฮสต์ของ URL การตั้งค่าโดเมนไม่อยู่ใน SAN ของใบรับรอง อาจมีคำเตือน HTTPS โฮสต์ที่มีเฉพาะใน SAN แต่ไม่ได้ระบุที่นี่ ควรทบทวนว่าใช้ในโปรดักชันหรือไม่'
    },
    '브라우저 호스트명 (SAN dNSName)': {
      EN: 'Browser hostname (SAN dNSName)',
      JP: 'ブラウザホスト名（SAN dNSName）',
      CH: '浏览器主机名（SAN dNSName）',
      TH: 'ชื่อโฮสต์เบราว์เซอร์ (SAN dNSName)'
    },
    'SAN 목록을 읽지 못했습니다. 서버운영관리에서 LE 경로를 확인하세요.': {
      EN: 'Could not read the SAN list. Check the LE path under Server operations.',
      JP: 'SAN一覧を読み取れませんでした。サーバ運用管理でLEパスを確認してください。',
      CH: '无法读取 SAN 列表。请在服务器运维中检查 LE 路径。',
      TH: 'อ่านรายการ SAN ไม่ได้ ตรวจเส้นทาง LE ในการดูแลเซิร์ฟเวอร์'
    },
    'SAN에만 있고 도메인구성설정 URL에 없는 호스트:': {
      EN: 'Hosts present in SAN only (not in domain configuration URLs):',
      JP: 'SANにのみありドメイン構成URLにないホスト：',
      CH: '仅在 SAN 中、域名配置 URL 中不存在的主机：',
      TH: 'โฮสต์ที่มีเฉพาะใน SAN (ไม่มีใน URL การตั้งค่าโดเมน):'
    },
    'PEM 상태': {
      EN: 'PEM status',
      JP: 'PEM状態',
      CH: 'PEM 状态',
      TH: 'สถานะ PEM'
    },
    'LE 인증서 이름': {
      EN: 'LE certificate name',
      JP: 'LE証明書名',
      CH: 'LE 证书名称',
      TH: 'ชื่อใบรับรอง LE'
    },
    '만료까지(일)': {
      EN: 'Days to expiry',
      JP: '有効期限まで（日）',
      CH: '距过期（天）',
      TH: 'วันจนหมดอายุ'
    },
    '인증서 SAN': {
      EN: 'Certificate SAN',
      JP: '証明書SAN',
      CH: '证书 SAN',
      TH: 'SAN ของใบรับรอง'
    },
    '전사 URL이 저장되었습니다.': {
      EN: 'Company-wide URLs saved.',
      JP: '全社URLを保存しました。',
      CH: '已保存全公司 URL。',
      TH: 'บันทึก URL ทั้งองค์กรแล้ว'
    },
    '업체를 먼저 선택하세요.': {
      EN: 'Select a company first.',
      JP: '先に加盟店を選択してください。',
      CH: '请先选择公司。',
      TH: 'เลือกร้านก่อน'
    },
    '도메인 설정이 저장되었습니다.': {
      EN: 'Domain settings saved.',
      JP: 'ドメイン設定を保存しました。',
      CH: '域名设置已保存。',
      TH: 'บันทึกการตั้งค่าโดเมนแล้ว'
    },
    '도메인 설정을 삭제했습니다.': {
      EN: 'Domain settings deleted.',
      JP: 'ドメイン設定を削除しました。',
      CH: '已删除域名设置。',
      TH: 'ลบการตั้งค่าโดเมนแล้ว'
    },
    '[{0}] 조직의 도메인 설정(설정 이름·관리자 URL·API URL)을 삭제합니다. 계속하시겠습니까?': {
      EN: 'Delete domain settings (name, admin URL, API URL) for organization [{0}]? Continue?',
      JP: '組織[{0}]のドメイン設定（設定名・管理URL・API URL）を削除します。続行しますか？',
      CH: '将删除组织 [{0}] 的域名设置（设置名称、管理 URL、API URL）。是否继续？',
      TH: 'ลบการตั้งค่าโดเมน (ชื่อ, URL แอดมิน, URL API) ขององค์กร [{0}] ต่อหรือไม่'
    },
    '한 번 더 확인합니다. 삭제 후 입력 내용은 서버에서 비워집니다. 정말 삭제하시겠습니까?': {
      EN: 'Second confirmation. After delete, saved values are cleared on the server. Delete for sure?',
      JP: '再確認です。削除後、入力内容はサーバー側で空になります。本当に削除しますか？',
      CH: '再次确认。删除后服务器将清空已保存内容。确定删除吗？',
      TH: 'ยืนยันอีกครั้ง หลังลบค่าที่บันทึกจะถูกล้างบนเซิร์ฟเวอร์ ลบจริงหรือไม่'
    },

    /* HQ /hq/chatbotAiSettings — AI챗봇설정 */
    '챗봇관리': {
      EN: 'Chatbot management', JP: 'チャットボット管理', CH: '聊天机器人管理', TH: 'จัดการ Chatbot'
    },
    '기본설정': {
      EN: 'Basic settings', JP: '基本設定', CH: '基本设置', TH: 'การตั้งค่าพื้นฐาน'
    },
    '상품관리': {
      EN: 'Products', JP: '商品管理', CH: '商品管理', TH: 'สินค้า'
    },
    '주문관리': {
      EN: 'Orders', JP: '注文管理', CH: '订单管理', TH: 'คำสั่งซื้อ'
    },
    'AI챗봇설정': {
      EN: 'AI chatbot settings', JP: 'AIチャットボット設定', CH: 'AI 聊天机器人设置', TH: 'ตั้งค่าแชทบอท AI'
    },
    '챗봇용 AI 제공자 순위 (1순위부터, 비사용은 건너뜀)': {
      EN: 'AI provider priority for chatbot (from 1st; skip unused)',
      JP: 'チャットボット用AIプロバイダ順位（1位から、未使用はスキップ）',
      CH: '聊天机器人 AI 提供商优先级（从第 1 位起，跳过未使用）',
      TH: 'ลำดับผู้ให้บริการ AI แชทบอท (เริ่ม 1 ข้ามที่ไม่ใช้)'
    },
    '1순위': { EN: '1st', JP: '1位', CH: '第1', TH: 'อันดับ 1' },
    '2순위': { EN: '2nd', JP: '2位', CH: '第2', TH: 'อันดับ 2' },
    '3순위': { EN: '3rd', JP: '3位', CH: '第3', TH: 'อันดับ 3' },
    '4순위': { EN: '4th', JP: '4位', CH: '第4', TH: 'อันดับ 4' },
    '키가 저장되어 있습니다. 바꿀 때만 새 값을 입력하세요.': {
      EN: 'A key is saved. Enter a new value only when changing it.',
      JP: 'キーが保存されています。変更時のみ新しい値を入力してください。',
      CH: '密钥已保存。仅在更改时输入新值。',
      TH: 'มีการบันทึกคีย์แล้ว ใส่ค่าใหม่เมื่อต้องการเปลี่ยนเท่านั้น'
    },
    '미등록입니다.': {
      EN: 'Not registered.', JP: '未登録です。', CH: '未注册。', TH: 'ยังไม่ลงทะเบียน'
    },
    'Google Gemini API 키': {
      EN: 'Google Gemini API key', JP: 'Google Gemini APIキー', CH: 'Google Gemini API 密钥', TH: 'คีย์ Google Gemini API'
    },
    'Groq API 키': {
      EN: 'Groq API key', JP: 'Groq APIキー', CH: 'Groq API 密钥', TH: 'คีย์ Groq API'
    },
    'Anthropic(Claude) API 키': {
      EN: 'Anthropic (Claude) API key', JP: 'Anthropic(Claude) APIキー', CH: 'Anthropic (Claude) API 密钥', TH: 'คีย์ Anthropic (Claude) API'
    },
    'OpenAI API 키': {
      EN: 'OpenAI API key', JP: 'OpenAI APIキー', CH: 'OpenAI API 密钥', TH: 'คีย์ OpenAI API'
    },
    '챗봇 상품등록 플랜(월 이용료)': {
      EN: 'Chatbot product registration plan (monthly fee)',
      JP: 'チャットボット商品登録プラン（月額）',
      CH: '聊天机器人商品登记方案（月费）',
      TH: 'แพลนลงทะเบียนสินค้าแชทบอท (ค่ารายเดือน)'
    },
    '우선 지시 (시스템)': {
      EN: 'Priority instructions (system)',
      JP: '優先指示（システム）',
      CH: '优先指令（系统）',
      TH: 'คำสั่งระบบ (ลำดับความสำคัญ)'
    },
    '프롬프트 (챗봇)': {
      EN: 'Prompts (chatbot)',
      JP: 'プロンプト（チャットボット）',
      CH: '提示词（聊天机器人）',
      TH: 'พรอมต์ (แชทบอท)'
    },
    '리포트 API 키(챗봇·상품안내 공용)': {
      EN: 'Report API keys (shared for chatbot & product guidance)',
      JP: 'レポートAPIキー（チャットボット・商品案内共通）',
      CH: '报告 API 密钥（聊天机器人与商品说明共用）',
      TH: 'คีย์ API รายงาน (แชทบอท·แนะนำสินค้า ร่วมกัน)'
    },
    '등록 상품 안내 시 반드시 지킬 규칙, 언어, 금액 왜곡 금지 등': {
      EN: 'Rules for registered-product guidance: language, do not distort amounts, etc.',
      JP: '登録商品の案内で必ず守るルール（言語、金額のねじれ・誇張の禁止など）',
      CH: '介绍登记商品时必须遵守的规则（语言、禁止歪曲金额等）',
      TH: 'กฎเมื่อแนะนำสินค้าที่ลงทะเบียน (ภาษา ห้ามบิดเบือนยอด ฯลฯ)'
    },
    '상품 목록·가격 매칭 시 사용할 역할 안내': {
      EN: 'Role guidance when matching the product list to prices',
      JP: '商品一覧・価格照合時に使う役割（ロール）の案内',
      CH: '匹配商品列表与价格时使用的角色说明',
      TH: 'คำแนะบทบาทเมื่อจับคู่รายการสินค้ากับราคา'
    },
    '상품 카탈로그 사용자 프롬프트 템플릿': {
      EN: 'Product catalog user prompt template',
      JP: '商品カタログユーザープロンプトテンプレート',
      CH: '商品目录用户提示模板',
      TH: 'เทมเพลตพรอมต์ผู้ใช้แคตตาล็อกสินค้า'
    },
    '고급 — 출력 형식 제한(ai_system_options_chatbot JSON)': {
      EN: 'Advanced — output format limits (ai_system_options_chatbot JSON)',
      JP: '高度 — 出力形式制限（ai_system_options_chatbot JSON）',
      CH: '高级 — 输出格式限制（ai_system_options_chatbot JSON）',
      TH: 'ขั้นสูง — จำกัดรูปแบบผลลัพธ์ (ai_system_options_chatbot JSON)'
    },
    '고급 JSON 형식 오류입니다.': {
      EN: 'Advanced JSON format error.', JP: '高度JSONの形式エラーです。', CH: '高级 JSON 格式错误。', TH: 'รูปแบบ JSON ขั้นสูงไม่ถูกต้อง'
    },
    '모델명 직접입력': {
      EN: 'Enter model name', JP: 'モデル名を直接入力', CH: '手动输入模型名', TH: 'พิมพ์ชื่อโมเดล'
    },

    /* chatbot KB / product / order + collateral summary */
    '담보금액': {
      EN: 'Collateral amount', JP: '担保金額', CH: '担保金额', TH: 'ยอดหลักประกัน'
    },
    '산하 가맹 챗봇 기본설정 현황': {
      EN: 'Sub-merchant chatbot basic settings overview',
      JP: '傘下加盟店チャットボット基本設定状況',
      CH: '下级商户聊天机器人基本设置概况',
      TH: 'ภาพรวมการตั้งค่าพื้นฐานแชทบอทร้านในเครือ'
    },
    '플랜구매설정': {
      EN: 'Plan purchase settings',
      JP: 'プラン購入設定',
      CH: '套餐购买设置',
      TH: 'การตั้งค่าซื้อแพลน'
    },
    '챗봇·고객 안내 설정': {
      EN: 'Chatbot & customer guidance',
      JP: 'チャットボット・顧客案内設定',
      CH: '聊天机器人与客户说明设置',
      TH: 'การตั้งค่าแชทบอทและคำแนะนำลูกค้า'
    },
    '총본사·본사·총판 등 상위 조직은 산하 가맹점 중 챗봇결제 사용(Y) 가맹점만 표시됩니다(등록 정보와 병합된 안내 표시값). 「상업 기능」열에서 운영 보류를 두면 고객 챗봇에는 상품·예약·결제가 보이지 않지만 문의 채팅은 유지됩니다(챗봇 미사용과 다름). 가맹점 계정은 이 블록이 보이지 않으며, 하단에서 본인 업체 안내만 편집합니다.': {
      EN: 'Parent orgs (root HQ, HQ, master distributor, etc.) see only sub-merchants with chatbot payment enabled (Y), with merged registration display values. Setting commerce hold in the Commerce column hides products, reservations, and checkout in the public chatbot but keeps inquiry chat (different from chatbot off). Merchant accounts do not see this block and edit only their own guidance below.',
      JP: '総本社・本社・総販など上位組織は、傘下加盟店のうちチャットボット決済使用(Y)の加盟店のみ表示します（登録情報とマージした案内表示値）。「商業機能」列で運用保留にすると、顧客チャットボットでは商品・予約・決済は非表示ですが問い合わせチャットは維持されます（チャットボット未使用とは異なります）。加盟店アカウントはこのブロックは表示されず、下で自社案内のみ編集します。',
      CH: '总本部、本部、总代等上级组织仅显示启用聊天机器人支付(Y)的下级商户（合并注册信息的展示值）。在「商业功能」列设为运营暂停时，客户聊天机器人不显示商品、预约、支付，但保留咨询聊天（与关闭聊天机器人不同）。商户账号不显示此块，仅在下方编辑本公司说明。',
      TH: 'องค์กรแม่ (HQ ราก, HQ, ตัวแทนหลัก ฯลฯ) แสดงเฉพาะร้านในเครือที่เปิดชำระแชทบอท (Y) พร้อมค่าแสดงที่รวมจากการลงทะเบียน ตั้งพักการค้าในคอลัมน์ฟังก์ชันพาณิชย์จะซ่อนสินค้า/จอง/ชำระในแชทบอทสาธารณะแต่คงแชทสอบถาม (ต่างจากปิดแชทบอท) บัญชีร้านไม่เห็นบล็อกนี้ แก้เฉพาะคำแนะนำของตนเองด้านล่าง'
    },
    '등록 가능 건수(플랜)과 본사 AI챗봇설정의 월 요금표가 같은 통화로 표시됩니다. 「현재 플랜」은 당월 즉시 적용 기준이며, 상향만 「즉시 상향」에서 반영됩니다(잔여일 차액 미수금). 「다음 플랜(예약)」은 변경이 없으면 예약이 없는 상태로 유지되고, 바꾸면 익월(서울 달력)부터 적용되며 하향·상향 모두 동일합니다.': {
      EN: 'Registration cap (plan) and HQ AI chatbot monthly fee table use the same currency. Current plan applies immediately this month; only upgrades apply via Immediate upgrade (remaining-day difference as receivable). Next plan (reserved) stays empty if unchanged; if changed, it applies from next calendar month (Seoul) for both downgrades and upgrades.',
      JP: '登録可能件数（プラン）と本社AIチャットボット設定の月額料金表は同じ通貨で表示されます。「現在プラン」は当月即時適用基準で、件数増の上げのみ「即時上げ」で反映されます（残日数差額は未収金）。「次プラン（予約）」は変更がなければ予約なしのまま、変更すると翌月（ソウル暦）から適用され、下げ・上げとも同じです。',
      CH: '可注册件数（套餐）与总部 AI 聊天机器人月费表以相同货币显示。「当前套餐」按当月即时生效；仅升级在「即时升级」中反映（剩余天数差额记为未收款）。「下月套餐（预约）」无变更则保持无预约；变更后从次月（首尔日历）起生效，降档与升档相同。',
      TH: 'เพดานลงทะเบียน (แพลน) และตารางค่าบริการรายเดือน AI แชทบอท HQ ใช้สกุลเงินเดียวกัน แพลนปัจจุบันมีผลทันทีในเดือนนี้ อัปเกรดเท่านั้นผ่านอัปเกรดทันที (ส่วนต่างวันคงเหลือเป็นค้างรับ) แพลนถัดไป (จอง) ว่างถ้าไม่เปลี่ยน เปลี่ยนแล้วมีผลเดือนถัดไป (ปฏิทินโซล) ทั้งลดและเพิ่ม'
    },
    '고객 공개 챗봇에 노출되는 회사 안내·운영방식·예약 옵션입니다. 가맹은 본인 정보를 저장하고, 상위 조직은 가맹 코드로 불러온 뒤 저장합니다.': {
      EN: 'Company guidance, operation mode, and reservation options shown on the public chatbot. Merchants save their own info; parent orgs load by merchant code then save.',
      JP: '顧客公開チャットボットに表示する会社案内・運用方式・予約オプションです。加盟店は自社情報を保存し、上位組織は加盟店コードで読み込んでから保存します。',
      CH: '在公开聊天机器人上展示的公司说明、运营方式与预约选项。商户保存本人信息；上级组织按商户代码加载后保存。',
      TH: 'คำแนะนำบริษัท โหมดการทำงาน และตัวเลือกจองที่แสดงบนแชทบอทสาธารณะ ร้านบันทึกข้อมูลตนเอง องค์กรแม่โหลดด้วยรหัสร้านแล้วบันทึก'
    },
    '챗봇 고객이 제출한 주문·예약 정보입니다. 결제가 완료되면 접수(확정)로 바뀌며 PG 거래번호가 연결됩니다.': {
      EN: 'Orders and reservations submitted by chatbot customers. After payment completes, status becomes confirmed and a PG transaction ID is linked.',
      JP: 'チャットボット顧客が提出した注文・予約情報です。決済完了後は受付（確定）に変わり、PG取引番号が紐づきます。',
      CH: '聊天机器人客户提交的订单与预约信息。支付完成后变为受理（确认），并关联 PG 交易号。',
      TH: 'คำสั่งซื้อและการจองที่ลูกค้าแชทบอทส่ง หลังชำระเสร็จสถานะเป็นยืนยันและเชื่อมหมายเลขธุรกรรม PG'
    },
    '운영방식': {
      EN: 'Operation mode', JP: '運用方式', CH: '运营方式', TH: 'โหมดการทำงาน'
    },
    '등록·활성': {
      EN: 'Registered · active', JP: '登録・有効', CH: '注册·活跃', TH: 'ลงทะเบียน·ใช้งาน'
    },
    '안내 회사명': {
      EN: 'Guidance company name', JP: '案内会社名', CH: '说明用公司名', TH: 'ชื่อบริษัทในคำแนะนำ'
    },
    '판매안내': {
      EN: 'Sales guidance', JP: '販売案内', CH: '销售说明', TH: 'คำแนะนำการขาย'
    },
    '회사소개': {
      EN: 'Company intro', JP: '会社紹介', CH: '公司介绍', TH: 'แนะนำบริษัท'
    },
    '상업 기능': {
      EN: 'Commerce', JP: '商業機能', CH: '商业功能', TH: 'ฟังก์ชันพาณิชย์'
    },
    '상품판매 · 선불': {
      EN: 'Product sales · prepaid', JP: '商品販売・前払い', CH: '商品销售·预付', TH: 'ขายสินค้า · จ่ายล่วงหน้า'
    },
    '상품판매 · 후불': {
      EN: 'Product sales · postpaid', JP: '商品販売・後払い', CH: '商品销售·后付', TH: 'ขายสินค้า · จ่ายหลัง'
    },
    '예약방식 · 선불': {
      EN: 'Reservation · prepaid', JP: '予約方式・前払い', CH: '预约方式·预付', TH: 'จอง · จ่ายล่วงหน้า'
    },
    '예약방식 · 후불': {
      EN: 'Reservation · postpaid', JP: '予約方式・後払い', CH: '预约方式·后付', TH: 'จอง · จ่ายหลัง'
    },
    '하이브리드 (판매+예약, 예약은 선불 고정)': {
      EN: 'Hybrid (sales + reservation; reservation prepaid only)',
      JP: 'ハイブリッド（販売+予約、予約は前払い固定）',
      CH: '混合（销售+预约，预约固定预付）',
      TH: 'ไฮบริด (ขาย+จอง จองจ่ายล่วงหน้าเท่านั้น)'
    },
    '대면거래 (판매+예약 · 후불)': {
      EN: 'Face-to-face (sales + reservation · postpaid)',
      JP: '対面取引（販売+予約・後払い）',
      CH: '面对面（销售+预约·后付）',
      TH: 'พบหน้า (ขาย+จอง · จ่ายหลัง)'
    },
    '정상': {
      EN: 'Normal', JP: '正常', CH: '正常', TH: 'ปกติ'
    },
    '운영 보류': {
      EN: 'Commerce hold', JP: '運用保留', CH: '运营暂停', TH: 'พักการค้า'
    },
    '보류 해제': {
      EN: 'Release hold', JP: '保留解除', CH: '解除暂停', TH: 'ปลดพัก'
    },
    '플랜·과금': {
      EN: 'Plan & billing', JP: 'プラン・課金', CH: '套餐与计费', TH: 'แพลนและการเรียกเก็บ'
    },
    '현재 플랜(건)': {
      EN: 'Current plan (items)', JP: '現在プラン（件）', CH: '当前套餐（件）', TH: 'แพลนปัจจุบัน (รายการ)'
    },
    '청구 통화': {
      EN: 'Billing currency', JP: '請求通貨', CH: '计费货币', TH: 'สกุลเงินเรียกเก็บ'
    },
    '과금 기간(서울)': {
      EN: 'Billing period (Seoul)', JP: '課金期間（ソウル）', CH: '计费期间（首尔）', TH: 'ช่วงเรียกเก็บ (โซล)'
    },
    '플랜 변경': {
      EN: 'Change plan', JP: 'プラン変更', CH: '更改套餐', TH: 'เปลี่ยนแพลน'
    },
    '즉시 상향(당월 반영)': {
      EN: 'Immediate upgrade (this month)', JP: '即時上げ（当月反映）', CH: '即时升级（当月生效）', TH: 'อัปเกรดทันที (เดือนนี้)'
    },
    '다음 플랜(예약·익월 적용)': {
      EN: 'Next plan (reserve · next month)', JP: '次プラン（予約・翌月適用）', CH: '下月套餐（预约·次月生效）', TH: 'แพลนถัดไป (จอง · เดือนถัดไป)'
    },
    '예약 없음(익월에도 현재 플랜 유지)': {
      EN: 'No reservation (keep current plan next month)',
      JP: '予約なし（翌月も現在プラン維持）',
      CH: '无预约（下月仍保持当前套餐）',
      TH: 'ไม่จอง (คงแพลนปัจจุบันเดือนถัดไป)'
    },
    '미수금 내역(플랜)': {
      EN: 'Receivable history (plan)', JP: '未収金内訳（プラン）', CH: '未收款明细（套餐）', TH: 'ประวัติค้างรับ (แพลน)'
    },
    '상품관리에서 등록·수정': {
      EN: 'Register / edit in Products', JP: '商品管理で登録・修正', CH: '在商品管理中注册/修改', TH: 'ลงทะเบียน/แก้ในเมนูสินค้า'
    },
    '챗봇 운영방식': {
      EN: 'Chatbot operation mode', JP: 'チャットボット運用方式', CH: '聊天机器人运营方式', TH: 'โหมดแชทบอท'
    },
    '접수': {
      EN: 'Confirmed', JP: '受付', CH: '受理', TH: 'ยืนยันแล้ว'
    },
    '접수일시': {
      EN: 'Confirmed at', JP: '受付日時', CH: '受理时间', TH: 'เวลายืนยัน'
    },
    '결제대기': {
      EN: 'Awaiting payment', JP: '決済待ち', CH: '待支付', TH: 'รอชำระ'
    },
    '상품판매': {
      EN: 'Product sales', JP: '商品販売', CH: '商品销售', TH: 'ขายสินค้า'
    },
    '시간 예약': {
      EN: 'Time reservation', JP: '時間予約', CH: '时间预约', TH: 'จองเวลา'
    },
    '장소 예약': {
      EN: 'Place reservation', JP: '場所予約', CH: '地点预约', TH: 'จองสถานที่'
    },
    '목록에서 [수정]을 누르면 코드가 채워지고 아래 플랜·미수금을 고칠 수 있습니다.': {
      EN: 'Click [Edit] in the list to fill the code and edit plan and receivables below.',
      JP: '一覧で［修正］を押すとコードが入力され、下のプラン・未収金を編集できます。',
      CH: '在列表中点击［修改］将填入代码，可在下方编辑套餐与未收款。',
      TH: 'กด［แก้ไข］ในรายการเพื่อเติมรหัสและแก้แพลน/ค้างรับด้านล่าง'
    },
    '고객이 주문서를 제출하고 결제를 완료하면 접수(확정)됩니다. 예약 시간은 기본설정·상품별 슬롯으로 검증됩니다.': {
      EN: 'When the customer submits an order form and completes payment, it is confirmed. Reservation times are validated against basic settings and per-product slots.',
      JP: '顧客が注文書を提出し決済を完了すると受付（確定）になります。予約時間は基本設定・商品別スロットで検証されます。',
      CH: '客户提交订单并完成支付后变为受理（确认）。预约时间按基本设置与各商品时段验证。',
      TH: 'เมื่อลูกค้าส่งใบสั่งและชำระเสร็จจะยืนยัน เวลาจองตรวจกับการตั้งค่าพื้นฐานและสล็อตต่อสินค้า'
    },
    '선택한 운영방식에 맞춰 공개 챗봇 응대(선불·후불·예약 안내)가 적용됩니다.': {
      EN: 'Public chatbot responses (prepaid/postpaid/reservation guidance) follow the selected operation mode.',
      JP: '選択した運用方式に合わせ、公開チャットボットの応対（前払い・後払い・予約案内）が適用されます。',
      CH: '公开聊天机器人应答（预付/后付/预约说明）按所选运营方式应用。',
      TH: 'การตอบแชทบอทสาธารณะ (จ่ายล่วงหน้า/หลัง/คำแนะนำจอง) ตามโหมดที่เลือก'
    },
    '월 정기·업그레이드 차액은 미수금으로 등록되며 정산에서 환수됩니다.': {
      EN: 'Monthly recurring and upgrade differences are posted as receivables and recovered in settlement.',
      JP: '月次定期・アップグレード差額は未収金として登録され、精算で回収されます。',
      CH: '月定期与升级差额记为未收款，在结算中扣回。',
      TH: 'ค่ารายเดือนและส่วนต่างอัปเกรดลงเป็นค้างรับและหักในการชำระ'
    },
    '챗봇 결제 및 공개 챗봇 노출 상품은 「상품관리」에서 설정합니다. 플랜마다 동시 「판매 활성」 가능 개수가 있으며, 「사용=Y」만 고객에게 판매·노출됩니다. 그보다 2건 더 많게 상품 행은 등록해 두되 판매 비활성(사용=N)으로 둘 수 있습니다. 안내(LLM·카탈로그)에는 판매 활성이면서 본사 판매금지 아닌 상품만 포함됩니다.': {
      EN: 'Chatbot checkout and public chatbot products are set under Products. Each plan caps concurrent sale-active items; only Use=Y is sold/shown to customers. You may register up to plan+2 rows with sale inactive (Use=N). Guidance (LLM/catalog) includes only sale-active items not blocked by HQ.',
      JP: 'チャットボット決済および公開チャットボット表示商品は「商品管理」で設定します。プランごとに同時「販売有効」可能件数があり、「使用=Y」のみ顧客に販売・表示されます。それより2件多く商品行を登録し販売非有効（使用=N）にできます。案内（LLM・カタログ）には販売有効かつ本社販売禁止でない商品のみ含まれます。',
      CH: '聊天机器人支付与公开聊天机器人展示商品在「商品管理」中设置。各套餐限制同时「销售活跃」数量；仅使用=Y 向客户销售/展示。可多登记 plan+2 行并设为销售非活跃(使用=N)。说明(LLM/目录)仅含销售活跃且未被总部禁售的商品。',
      TH: 'ชำระแชทบอทและสินค้าแสดงบนแชทบอทสาธารณะตั้งใน「สินค้า」 แต่ละแพลนจำกัดสินค้าขายอยู่พร้อมกัน ใช้=Y เท่านั้นที่ขาย/แสดงให้ลูกค้า ลงทะเบียนได้มากกว่าแพลน +2 แถวโดยปิดการขาย(ใช้=N) คำแนะนำ(LLM/แคตตalog) รวมเฉพาะสินค้าขายอยู่ที่ HQ ไม่ห้ามขาย'
    },
    '판매 활성(현재/플랜)': {
      EN: 'Sale active (current/plan)', JP: '販売有効（現在/プラン）', CH: '销售活跃（当前/套餐）', TH: 'ขายอยู่ (ปัจจุบัน/แพลน)'
    },
    '플랜 미설정(무제한)': {
      EN: 'No plan (unlimited)', JP: 'プラン未設定（無制限）', CH: '未设套餐（不限）', TH: 'ไม่มีแพลน (ไม่จำกัด)'
    },
    '당월에는 건수가 늘어나는 상향만 즉시 적용됩니다(잔여일 차액 미수금). 익월에 바꿀 플랜(상향·하향)은 아래 예약에서 선택하세요.': {
      EN: 'Only upgrades that increase the item cap apply immediately this month (remaining-day difference as receivable). Choose next-month plan changes (up or down) in the reservation field below.',
      JP: '当月は件数が増える上げのみ即時適用されます（残日数差額は未収金）。翌月に変更するプラン（上げ・下げ）は下の予約で選択してください。',
      CH: '当月仅增加件数的上调即时生效（剩余天数差额记未收款）。下月变更套餐（升/降）请在下方预约中选择。',
      TH: 'เดือนนี้อัปเกรดที่เพิ่มเพดานเท่านั้นมีผลทันที (ส่วนต่างวันคงเหลือเป็นค้างรับ) เลือกแพลนเดือนถัดไป (ขึ้น/ลง) ในช่องจองด้านล่าง'
    },
    '예약이 없으면 익월에도 현재 플랜이 유지됩니다. 값을 바꾸면 익월(서울 달력)부터 그 플랜이 적용됩니다.': {
      EN: 'Without a reservation, the current plan continues next month. Changing the value applies from next calendar month (Seoul).',
      JP: '予約がなければ翌月も現在プランが維持されます。値を変えると翌月（ソウル暦）からそのプランが適用されます。',
      CH: '无预约则下月仍保持当前套餐。更改值后从次月（首尔日历）起生效。',
      TH: 'ไม่จองจะคงแพลนปัจจุบันเดือนถัดไป เปลี่ยนค่าแล้วมีผลเดือนถัดไป (ปฏิทินโซล)'
    },
    '다음 플랜(예약): 없음(익월에도 현재와 동일)': {
      EN: 'Next plan (reserved): none (same as current next month)',
      JP: '次プラン（予約）: なし（翌月も現在と同じ）',
      CH: '下月套餐（预约）：无（下月与当前相同）',
      TH: 'แพลนถัดไป (จอง): ไม่มี (เดือนถัดไปเหมือนปัจจุบัน)'
    },
    '다음 플랜(예약): {0}건 · 적용월 {1}~': {
      EN: 'Next plan (reserved): {0} items · from {1}',
      JP: '次プラン（予約）: {0}件 · 適用月 {1}~',
      CH: '下月套餐（预约）：{0} 件 · 生效月 {1}~',
      TH: 'แพลนถัดไป (จอง): {0} รายการ · ตั้งแต่ {1}'
    },
    '다음 플랜(예약): {0}건': {
      EN: 'Next plan (reserved): {0} items',
      JP: '次プラン（予約）: {0}件',
      CH: '下月套餐（预约）：{0} 件',
      TH: 'แพลนถัดไป (จอง): {0} รายการ'
    },
    '다음 달({0})부터 {1}건 플랜으로 변경 예정입니다. 이번 달은 기존 플랜이 유지됩니다.': {
      EN: 'Plan will change to {1} items from {0}. Current plan remains this month.',
      JP: '翌月（{0}）から{1}件プランに変更予定です。今月は既存プランが維持されます。',
      CH: '预计从 {0} 起改为 {1} 件套餐。本月仍保持原套餐。',
      TH: 'จะเปลี่ยนเป็นแพลน {1} รายการตั้งแต่ {0} เดือนนี้ยังคงแพลนเดิม'
    },
    '챗봇결제가 «미사용»이면 플랜을 바꿀 수 없습니다. 업체등록 등에서 챗봇결제를 켠 뒤 다시 불러오세요.': {
      EN: 'If chatbot payment is disabled, you cannot change the plan. Enable chatbot payment in merchant registration, then reload.',
      JP: 'チャットボット決済が「未使用」ならプランを変更できません。加盟店登録等でチャットボット決済を有効にしてから再読み込みしてください。',
      CH: '聊天机器人支付为「未使用」时无法更改套餐。请在商户注册等处启用后重新加载。',
      TH: 'ถ้าปิดชำระแชทบอทจะเปลี่ยนแพลนไม่ได้ เปิดในการลงทะเบียนร้านแล้วโหลดใหม่'
    },
    '챗봇결제가 «미사용»이면 상품 등록·공개 챗봇이 제한될 수 있습니다. 업체 정보에서 챗봇결제를 활성화하세요.': {
      EN: 'If chatbot payment is disabled, product registration and the public chatbot may be limited. Enable chatbot payment in company info.',
      JP: 'チャットボット決済が「未使用」なら商品登録・公開チャットボットが制限される場合があります。加盟店情報でチャットボット決済を有効にしてください。',
      CH: '聊天机器人支付为「未使用」时可能限制商品注册与公开聊天机器人。请在商户信息中启用。',
      TH: 'ถ้าปิดชำระแชทบอทอาจจำกัดการลงทะเบียนสินค้าและแชทบอทสาธารณะ เปิดในข้อมูลร้าน'
    },
    '운영 보류 중입니다. 고객 공개 챗봇에서는 상품·예약·결제가 비활성이며 문의 채팅만 가능합니다. 해제는 상위 조직 「기본설정」 산하 현황 또는 관리자에서 설정합니다.': {
      EN: 'Commerce is on hold. The public chatbot disables products, reservations, and checkout; inquiry chat remains. Release via parent org Basic settings overview or admin.',
      JP: '運用保留中です。顧客公開チャットボットでは商品・予約・決済は無効で、問い合わせチャットのみ可能です。解除は上位組織「基本設定」傘下状況または管理者で設定します。',
      CH: '运营暂停中。公开聊天机器人禁用商品、预约、支付，仅保留咨询聊天。在上级组织「基本设置」下级概况或管理员处解除。',
      TH: 'พักการค้าอยู่ แชทบอทสาธารณะปิดสินค้า/จอง/ชำระ เหลือแชทสอบถาม ปลดที่ภาพรวมการตั้งค่าพื้นฐานขององค์กรแม่หรือแอดมิน'
    },
    '운영 보류를 해제하면 고객에게 상품·결제 기능이 다시 노출됩니다. 계속할까요?': {
      EN: 'Releasing commerce hold will show products and checkout to customers again. Continue?',
      JP: '運用保留を解除すると、顧客に商品・決済機能が再表示されます。続行しますか？',
      CH: '解除运营暂停后客户将再次看到商品与支付功能。是否继续？',
      TH: 'ปลดพักการค้าจะแสดงสินค้าและชำระให้ลูกค้าอีกครั้ง ดำเนินการต่อ?'
    },
    '챗봇 기본설정 조회에 실패했습니다. 로그인·업체코드를 확인하세요.': {
      EN: 'Failed to load chatbot basic settings. Check login and merchant code.',
      JP: 'チャットボット基本設定の取得に失敗しました。ログイン・加盟店コードを確認してください。',
      CH: '加载聊天机器人基本设置失败。请检查登录与商户代码。',
      TH: 'โหลดการตั้งค่าพื้นฐานแชทบอทไม่สำเร็จ ตรวจสอบการเข้าสู่ระบบและรหัสร้าน'
    },
    '가맹 카탈로그 유형을 하나 이상 선택하세요.': {
      EN: 'Select at least one merchant catalog type.',
      JP: '加盟店カタログ種別を1つ以上選択してください。',
      CH: '请至少选择一种商户目录类型。',
      TH: 'เลือกประเภทแคตตalog ร้านอย่างน้อย 1 รายการ'
    },
    '주문 내역이 없습니다.': {
      EN: 'No orders.', JP: '注文履歴がありません。', CH: '没有订单。', TH: 'ไม่มีคำสั่งซื้อ'
    },
    '업그레이드 차액': {
      EN: 'Upgrade difference', JP: 'アップグレード差額', CH: '升级差额', TH: 'ส่วนต่างอัปเกรด'
    },
    '월 정기': {
      EN: 'Monthly recurring', JP: '月次定期', CH: '月定期', TH: 'รายเดือน'
    },
    '월 정기 ': {
      EN: 'Monthly ', JP: '月次 ', CH: '月 ', TH: 'รายเดือน '
    },
    '본사 요금 미설정 또는 0': {
      EN: 'HQ fee not set or zero', JP: '本社料金未設定または0', CH: '总部费用未设或为0', TH: 'ยังไม่ตั้งค่าธรรมเนียม HQ หรือเป็น 0'
    },
    ' 활성 동시 최대 · ': {
      EN: ' max concurrent active · ', JP: ' 有効同時最大 · ', CH: ' 最大同时活跃 · ', TH: ' ใช้งานพร้อมกันสูงสุด · '
    },
    '건까지 등록': {
      EN: ' items registered max', JP: '件まで登録', CH: ' 件可注册', TH: ' รายการลงทะเบียนได้'
    },
    '무제한으로(즉시)': {
      EN: 'To unlimited (immediate)', JP: '無制限に（即時）', CH: '改为不限（即时）', TH: 'ไม่จำกัด (ทันที)'
    },
    '년 ': {
      EN: ' ', JP: '年 ', CH: '年 ', TH: ' '
    },
    '월 1일 — ': {
      EN: ' — ', JP: '月1日 — ', CH: '月1日 — ', TH: ' — '
    },
    '월 ': {
      EN: ' ', JP: '月 ', CH: '月 ', TH: ' '
    },
    ' · 상품 이미지 상한 ': {
      EN: ' · product image cap ', JP: ' · 商品画像上限 ', CH: ' · 商品图片上限 ', TH: ' · เพดานรูปสินค้า '
    },
    '카탈로그 실효 허용: ': {
      EN: 'Catalog effective allowed: ', JP: 'カタログ実効許可: ', CH: '目录实际允许: ', TH: 'แคตตalog ที่อนุญาตจริง: '
    },
    '가맹에서 사용할 카탈로그 유형': {
      EN: 'Catalog types for merchant use',
      JP: '加盟店で使用するカタログ種別',
      CH: '商户使用的目录类型',
      TH: 'ประเภทแคตตalog ที่ร้านใช้'
    },
    'AI로 회사소개 초안': {
      EN: 'AI draft for company intro',
      JP: 'AIで会社紹介草案',
      CH: 'AI 生成公司介绍草案',
      TH: 'ร่างแนะนำบริษัทด้วย AI'
    },
    '운영방식(선불·후불·예약)과 별개로, 업종에 맞는 주문·예약 질문 흐름을 잡는 분류입니다. 공개 챗봇 AI가 카탈로그·운영방식과 모순 없이 필요한 항목만 묻도록 서버에서 안내 블록으로 전달됩니다.': {
      EN: 'Separate from operation mode (prepaid/postpaid/reservation), this classifies order/reservation question flows by industry. The server sends guidance blocks so the public chatbot AI asks only needed items without contradicting catalog or operation mode.',
      JP: '運用方式（前払い・後払い・予約）とは別に、業種に合った注文・予約の質問フローを決める分類です。公開チャットボットAIがカタログ・運用方式と矛盾なく必要項目だけ尋ねるよう、サーバから案内ブロックを渡します。',
      CH: '与运营方式（预付/后付/预约）分开，按行业设定订单/预约提问流程。服务器下发说明块，使公开聊天机器人 AI 在不与目录/运营方式矛盾的前提下只询问必要项。',
      TH: 'แยกจากโหมดการทำงาน (จ่ายล่วงหน้า/หลัง/จอง) ใช้จัดลำดับคำถามสั่ง/จองตามอุตสาหกรรม เซิร์ฟเวอร์ส่งบล็อกคำแนะนำให้ AI แชทบอทสาธารณะถามเฉพาะที่จำเป็นโดยไม่ขัดกับแคตตalog/โหมด'
    },
    '예약 상품은 동일 시간대가 겹치지 않게 막습니다. 상품마다 다른 슬롯(분)은 「상품관리」에서 덮어쓸 수 있습니다.': {
      EN: 'Reservation products cannot overlap the same time slot. Per-product slot minutes can be overridden in Products.',
      JP: '予約商品は同一時間帯が重ならないようにします。商品ごとのスロット（分）は「商品管理」で上書きできます。',
      CH: '预约商品禁止同一时段重叠。各商品的时段（分钟）可在「商品管理」中覆盖。',
      TH: 'สินค้าจองห้ามทับช่วงเวลาเดียวกัน สล็อต (นาที) ต่อสินค้าแก้ใน「สินค้า」ได้'
    },
    '예약 시간은 기본설정과 상품 슬롯(분)으로 겹침을 검증합니다.': {
      EN: 'Reservation times are validated for overlap using basic settings and product slot minutes.',
      JP: '予約時間は基本設定と商品スロット（分）で重複を検証します。',
      CH: '预约时间按基本设置与商品时段（分钟）验证是否重叠。',
      TH: 'เวลาจองตรวจทับซ้อนด้วยการตั้งค่าพื้นฐานและสล็อตสินค้า (นาที)'
    },
    '숙박형: 고객이 체크아웃 날짜를 넣으면 그날 정오까지 예약이 잡히고, 비우면 예약 슬롯(분)만큼만 잡힙니다. 타임존은 기본설정을 따릅니다.': {
      EN: 'Lodging: if the customer enters a checkout date, the reservation runs until noon that day; if empty, only the reservation slot minutes apply. Timezone follows basic settings.',
      JP: '宿泊型: 顧客がチェックアウト日を入れるとその日正午まで予約が入り、空なら予約スロット（分）分だけ入ります。タイムゾーンは基本設定に従います。',
      CH: '住宿型：客户填写退房日则预约至该日中午；留空则仅按预约时段（分钟）。时区跟随基本设置。',
      TH: 'ที่พัก: ถ้าลูกค้าใส่วันช็คเอาต์จองถึงเที่ยงวันนั้น ว่างใช้เฉพาะสล็อตจอง (นาที) โซนเวลาตามการตั้งค่าพื้นฐาน'
    },
    '코드를 비우고 불러오기하면 로그인 조직 산하 가맹점의 등록 상품을 한 목록으로 봅니다. 본사·총판 열 「본사 판매금지」가 Y면 가맹이 사용=ON이어도 고객 챗봇·카탈로그에 노출되지 않습니다.': {
      EN: 'Load with an empty code to see registered products of sub-merchants under the logged-in org in one list. If HQ block is Y, items stay hidden from the public chatbot/catalog even when the merchant sets Use=ON.',
      JP: 'コードを空にして読み込むと、ログイン組織傘下加盟店の登録商品を一覧で見られます。本社・総販列「本社販売禁止」がYなら、加盟店が使用=ONでも顧客チャットボット・カタログに表示されません。',
      CH: '代码留空并加载可在一个列表中查看登录组织下级商户的注册商品。若总部禁售列为 Y，即使商户使用=ON 也不在客户聊天机器人/目录中展示。',
      TH: 'โหลดโดยเว้นรหัสว่างจะเห็นสินค้าที่ลงทะเบียนของร้านในเครือในรายการเดียว ถ้าคอลัมน์ HQ ห้ามขายเป็น Y จะไม่แสดงในแชทบอท/แคตตalog แม้ร้านตั้งใช้=ON'
    },
    '하이브리드 (좌1고정+2칸 순환)': {
      EN: 'Hybrid (fixed left + 2 rotating slots)',
      JP: 'ハイブリッド（左1固定+2枠ローテーション）',
      CH: '混合（左侧固定+两格轮播）',
      TH: 'ไฮบริด (ซ้ายคงที่ + 2 ช่องหมุน)'
    },
    '챗봇 상단 로고 자동축소( config_json 최상위, 선택 ): chatbot_logo_target_max_bytes(기본 2097152), chatbot_logo_max_edge_px(기본 1024), chatbot_logo_jpeg_quality_start(0~1, 기본 0.92), chatbot_logo_llm_tune_yn=Y(순위 LLM이 권장 변 길이 제안 → 서버 JPEG 재압축).': {
      EN: 'Chatbot top logo auto-resize (top-level config_json, optional): chatbot_logo_target_max_bytes (default 2097152), chatbot_logo_max_edge_px (default 1024), chatbot_logo_jpeg_quality_start (0–1, default 0.92), chatbot_logo_llm_tune_yn=Y (ranked LLM suggests edge length → server JPEG recompress).',
      JP: 'チャットボット上部ロゴ自動縮小（config_json最上位、任意）: chatbot_logo_target_max_bytes（既定2097152）, chatbot_logo_max_edge_px（既定1024）, chatbot_logo_jpeg_quality_start（0~1、既定0.92）, chatbot_logo_llm_tune_yn=Y（順位LLMが推奨辺長提案→サーバJPEG再圧縮）。',
      CH: '聊天机器人顶部 logo 自动缩小（config_json 顶层，可选）：chatbot_logo_target_max_bytes（默认 2097152）、chatbot_logo_max_edge_px（默认 1024）、chatbot_logo_jpeg_quality_start（0~1，默认 0.92）、chatbot_logo_llm_tune_yn=Y（按 LLM 排序建议边长→服务器 JPEG 再压缩）。',
      TH: 'ย่อโลโก้ด้านบนแชทบอทอัตโนมัติ (config_json ระดับบน ไม่บังคับ): chatbot_logo_target_max_bytes (ค่าเริ่ม 2097152), chatbot_logo_max_edge_px (1024), chatbot_logo_jpeg_quality_start (0~1, 0.92), chatbot_logo_llm_tune_yn=Y (LLM แนะนำความยาวขอบ→บีบ JPEG บนเซิร์ฟเวอร์)'
    },
    '미수금이 차감되었습니다.': {
      EN: 'Receivable has been deducted.',
      JP: '未収金が差し引かれました。',
      CH: '未收款已扣减。',
      TH: 'หักค้างรับแล้ว'
    },
    '결제대행사명': {
      EN: 'Acquirer name', JP: '決済代行名', CH: '支付机构名称', TH: 'ชื่อผู้ให้บริการชำระเงิน'
    },

    /* chatbot KB / product / order — labels & hints (screens data-pg-ui-t + app.js pgAdminUiT) */
    '전화': { EN: 'Phone', JP: '電話', CH: '电话', TH: 'โทรศัพท์' },
    '담당자': { EN: 'Contact', JP: '担当者', CH: '负责人', TH: 'ผู้ติดต่อ' },
    '대상 가맹점 코드': {
      EN: 'Target merchant code', JP: '対象加盟店コード', CH: '目标商户代码', TH: 'รหัสร้านเป้าหมาย'
    },
    '총판·가맹 기준통화': {
      EN: 'Master dist·merchant base currency', JP: '総販・加盟店基準通貨', CH: '总代·商户基准货币', TH: 'สกุลเงินฐานตัวแทนหลัก·ร้าน'
    },
    '동일 달력월 말까지': {
      EN: 'Through end of same calendar month', JP: '同一暦月の末まで', CH: '至同一日历月末', TH: 'ถึงสิ้นเดือนปฏิทินเดียวกัน'
    },
    '본사 AI챗봇설정과 동일 슬롯별 월 요금': {
      EN: 'Same per-slot monthly fees as HQ AI chatbot settings',
      JP: '本社AIチャットボット設定と同じスロット別月額',
      CH: '与总部 AI 聊天机器人设置相同的分档月费',
      TH: 'ค่ารายเดือนต่อสล็อตเดียวกับการตั้งค่า AI แชทบอท HQ'
    },
    '챗봇 등록·이용 상품': {
      EN: 'Chatbot registration & usage products', JP: 'チャットボット登録・利用商品', CH: '聊天机器人注册·使用商品', TH: 'สินค้าลงทะเบียน·ใช้แชทบอท'
    },
    '선택 안 함': { EN: 'Do not select', JP: '選択しない', CH: '不选择', TH: 'ไม่เลือก' },
    '판매 활성·등록 한도': {
      EN: 'Sale active · registration cap', JP: '販売有効・登録上限', CH: '销售活跃·注册上限', TH: 'ขายอยู่ · เพดานลงทะเบียน'
    },
    '등록 건수(전체)': {
      EN: 'Registered items (total)', JP: '登録件数（全体）', CH: '注册件数（全部）', TH: 'จำนวนลงทะเบียน (ทั้งหมด)'
    },
    '남은 등록 슬롯': {
      EN: 'Remaining registration slots', JP: '残り登録スロット', CH: '剩余注册名额', TH: 'สล็อตลงทะเบียนคงเหลือ'
    },
    '챗봇결제': { EN: 'Chatbot payment', JP: 'チャットボット決済', CH: '聊天机器人支付', TH: 'ชำระแชทบอท' },
    '고객 챗봇 문의 시 참고되는 안내입니다. 아래 비우면 1~5는 업체등록 정보와 동일하게 안내됩니다.': {
      EN: 'Guidance referenced when customers inquire via chatbot. If fields 1–5 below are empty, registration info is used instead.',
      JP: '顧客チャットボット問い合わせ時に参照される案内です。下を空にすると1~5は加盟店登録情報と同じ案内になります。',
      CH: '客户通过聊天机器人咨询时参考的说明。下方留空则 1~5 与商户注册信息相同。',
      TH: 'คำแนะนำเมื่อลูกค้าสอบถามผ่านแชทบอท ว่างด้านล่างจะใช้ข้อมูลลงทะเบียน 1~5 แทน'
    },
    '가맹점 업체성격': {
      EN: 'Merchant business type', JP: '加盟店業種性格', CH: '商户业态', TH: 'ลักษณะธุรกิจร้าน'
    },
    '업체성격 보조 메모(선택)': {
      EN: 'Business type notes (optional)', JP: '業種性格補助メモ（任意）', CH: '业态补充备注（可选）', TH: 'บันทึกเสริมลักษณะธุรกิจ (ไม่บังคับ)'
    },
    '특화 업종에서 반드시 받을 정보·피할 표현 등을 적으면 AI 수집 안내에 반영됩니다. 비우면 업체성격 기본 지침만 사용합니다.': {
      EN: 'Notes on required info or phrases to avoid for specialized industries are applied to AI collection guidance. Empty uses default business-type rules only.',
      JP: '特化業種で必ず受け取る情報・避ける表現などを書くとAI収集案内に反映されます。空なら業種性格の基本指針のみ使用します。',
      CH: '填写 specialized 行业必填信息或应避免表述后将反映到 AI 采集说明。留空则仅用业态默认规则。',
      TH: 'ระบุข้อมูลที่ต้องรับหรือถ้อยคำที่ควรหลีกเลี่ยงในอุตสาหกรรมเฉพาะจะสะท้อนใน AI ว่างใช้แนวทางลักษณะธุรกิจเท่านั้น'
    },
    '챗봇 주문·예약 시트 UI(JSON, 선택)': {
      EN: 'Chatbot order/reservation sheet UI (JSON, optional)',
      JP: 'チャットボット注文・予約シートUI（JSON、任意）',
      CH: '聊天机器人订单/预约表单 UI（JSON，可选）',
      TH: 'UI แผ่นสั่ง/จองแชทบอท (JSON ไม่บังคับ)'
    },
    '고객 챗봇 「주문·결제」시트 필드 표시·라벨을 가맹별로 덮어씁니다. 최상위 fields 아래 키: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo, reservationLocal, reservationCheckout, guestCount, serviceMinutes. 속성 예: hidden(true/false), labelKo, placeholderKo, showWhenReservation(이용시간 분, serviceMinutes만). 주소를 숨기면 prefillWhenHidden 을 4자 이상 필수. 이메일·전화는 숨길 수 없습니다. 비우면 업체성격 기본만 적용됩니다.': {
      EN: 'Overrides public chatbot order/payment sheet field visibility and labels per merchant. Top-level fields keys: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo, reservationLocal, reservationCheckout, guestCount, serviceMinutes. Attributes e.g. hidden(true/false), labelKo, placeholderKo, showWhenReservation (minutes, serviceMinutes only). If address is hidden, prefillWhenHidden (4+ chars) is required. Email and phone cannot be hidden. Empty uses business-type defaults only.',
      JP: '顧客チャットボット「注文・決済」シートのフィールド表示・ラベルを加盟店ごとに上書きします。最上位fieldsのキー: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo, reservationLocal, reservationCheckout, guestCount, serviceMinutes。属性例: hidden(true/false), labelKo, placeholderKo, showWhenReservation（利用時間分、serviceMinutesのみ）。住所を非表示にする場合prefillWhenHiddenは4文字以上必須。メール・電話は非表示不可。空なら業種性格デフォルトのみ。',
      CH: '按商户覆盖客户聊天机器人「订单·支付」表单字段显示与标签。顶层 fields 键：ordererName、ordererEmail、ordererPhone、ordererAddr、orderMemo、reservationLocal、reservationCheckout、guestCount、serviceMinutes。属性示例：hidden、labelKo、placeholderKo、showWhenReservation 等。隐藏地址时 prefillWhenHidden 至少 4 字。邮箱·电话不可隐藏。留空则仅用业态默认。',
      TH: 'แทนที่การแสดง/ป้ายฟิลด์แผ่นสั่ง·ชำระแชทบอทต่อร้าน คีย์ fields: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo, reservationLocal, reservationCheckout, guestCount, serviceMinutes ซ่อนที่อยู่ต้องมี prefillWhenHidden 4+ ตัว อีเมล·โทรซ่อนไม่ได้ ว่างใช้ค่าเริ่มต้นลักษณะธุรกิจ'
    },
    '예약 기본 슬롯(분)': {
      EN: 'Default reservation slot (min)', JP: '予約基本スロット（分）', CH: '预约默认时段（分钟）', TH: 'สล็อตจองเริ่มต้น (นาที)'
    },
    '예약 타임존(IANA)': {
      EN: 'Reservation timezone (IANA)', JP: '予約タイムゾーン（IANA）', CH: '预约时区（IANA）', TH: 'โซนเวลาจอง (IANA)'
    },
    '허용된 유형 안에서 하나 이상 선택하세요.': {
      EN: 'Select at least one allowed type.', JP: '許可された種別の中から1つ以上選択してください。', CH: '请在允许的类型中至少选择一种。', TH: 'เลือกอย่างน้อย 1 ประเภทที่อนุญาต'
    },
    '회사이름': { EN: 'Company name', JP: '会社名', CH: '公司名称', TH: 'ชื่อบริษัท' },
    '전화번호': { EN: 'Phone number', JP: '電話番号', CH: '电话号码', TH: 'หมายเลขโทรศัพท์' },
    '회사주소': { EN: 'Company address', JP: '会社住所', CH: '公司地址', TH: 'ที่อยู่บริษัท' },
    '담당자 성명': { EN: 'Contact name', JP: '担当者氏名', CH: '负责人姓名', TH: 'ชื่อผู้ติดต่อ' },
    '기본 안내 (첫 화면 상단)': {
      EN: 'Default greeting (top of first screen)', JP: '基本案内（最初の画面上部）', CH: '默认说明（首屏顶部）', TH: 'คำแนะนำเริ่มต้น (ด้านบนหน้าแรก)'
    },
    'AI로 기본 안내 초안': {
      EN: 'AI draft for default greeting', JP: 'AIで基本案内草案', CH: 'AI 生成默认说明草案', TH: 'ร่างคำแนะนำเริ่มต้นด้วย AI'
    },
    '비우면 시스템 기본 문구가 챗봇 첫 상단에 표시됩니다.': {
      EN: 'If empty, the system default message appears at the top of the chatbot.',
      JP: '空にするとシステム既定文がチャットボット上部に表示されます。',
      CH: '留空则显示系统默认文案于聊天机器人顶部。',
      TH: 'ว่างจะแสดงข้อความเริ่มต้นของระบบด้านบนแชทบอท'
    },
    '판매상품 안내(개요)': {
      EN: 'Product sales guidance (overview)', JP: '販売商品案内（概要）', CH: '销售商品说明（概要）', TH: 'คำแนะนำสินค้า (ภาพรวม)'
    },
    'AI로 판매상품 안내 초안': {
      EN: 'AI draft for product sales guidance', JP: 'AIで販売商品案内草案', CH: 'AI 生成销售商品说明草案', TH: 'ร่างคำแนะนำสินค้าด้วย AI'
    },
    '안내 저장': { EN: 'Save guidance', JP: '案内保存', CH: '保存说明', TH: 'บันทึกคำแนะนำ' },
    '일반판매': { EN: 'General retail', JP: '一般販売', CH: '一般销售', TH: 'ขายทั่วไป' },
    '이커머스': { EN: 'E-commerce', JP: 'EC', CH: '电商', TH: 'อีคอมเมิร์ซ' },
    '컨설팅': { EN: 'Consulting', JP: 'コンサル', CH: '咨询', TH: 'ที่ปรึกษา' },
    '부동산': { EN: 'Real estate', JP: '不動産', CH: '房地产', TH: 'อสังหาริมทรัพย์' },
    '자동차판매': { EN: 'Auto sales', JP: '自動車販売', CH: '汽车销售', TH: 'ขายรถยนต์' },
    '서비스업': { EN: 'Service industry', JP: 'サービス業', CH: '服务业', TH: 'ธุรกิจบริการ' },
    '일반마사지': { EN: 'General massage', JP: '一般マッサージ', CH: '一般按摩', TH: 'นวดทั่วไป' },
    '코스메틱': { EN: 'Cosmetics', JP: 'コスメ', CH: '化妆品', TH: 'เครื่องสำอาง' },
    '클럽(유흥)': { EN: 'Club (entertainment)', JP: 'クラブ（遊興）', CH: '俱乐部（娱乐）', TH: 'คลับ (บันเทิง)' },
    '클럽(마사지)': { EN: 'Club (massage)', JP: 'クラブ（マッサージ）', CH: '俱乐部（按摩）', TH: 'คลับ (นวด)' },
    '음식점': { EN: 'Restaurant', JP: '飲食店', CH: '餐饮店', TH: 'ร้านอาหาร' },
    'VIP 클럽': { EN: 'VIP club', JP: 'VIPクラブ', CH: 'VIP 俱乐部', TH: 'คลับ VIP' },
    '기타': { EN: 'Other', JP: 'その他', CH: '其他', TH: 'อื่นๆ' },
    '주문번호': { EN: 'Order no.', JP: '注文番号', CH: '订单号', TH: 'เลขคำสั่งซื้อ' },
    '주문자': { EN: 'Orderer', JP: '注文者', CH: '订购人', TH: 'ผู้สั่ง' },
    '예약': { EN: 'Reservation', JP: '予約', CH: '预约', TH: 'การจอง' },
    '상품': { EN: 'Product', JP: '商品', CH: '商品', TH: 'สินค้า' },
    '이미지(슬롯)': { EN: 'Image (slots)', JP: '画像（スロット）', CH: '图片（槽位）', TH: 'รูปภาพ (สล็อต)' },
    '상품 이미지 #1': { EN: 'Product image #1', JP: '商品画像 #1', CH: '商品图片 #1', TH: 'รูปสินค้า #1' },
    '상품 이미지 #2': { EN: 'Product image #2', JP: '商品画像 #2', CH: '商品图片 #2', TH: 'รูปสินค้า #2' },
    '상품 이미지 #3': { EN: 'Product image #3', JP: '商品画像 #3', CH: '商品图片 #3', TH: 'รูปสินค้า #3' },
    '상품 이미지 #4': { EN: 'Product image #4', JP: '商品画像 #4', CH: '商品图片 #4', TH: 'รูปสินค้า #4' },
    '{0}장': { EN: '{0} images', JP: '{0}枚', CH: '{0} 张', TH: '{0} รูป' },
    '조직 설정 기준 이미지 {0}장까지 업로드 가능합니다.': {
      EN: 'Up to {0} images can be uploaded per organization settings.',
      JP: '組織設定基準で画像{0}枚までアップロードできます。',
      CH: '按组织设置最多可上传 {0} 张图片。',
      TH: 'อัปโหลดรูปได้สูงสุด {0} รูปตามการตั้งค่าองค์กร'
    },
    '판매허용': { EN: 'Sale allowed', JP: '販売許可', CH: '允许销售', TH: 'อนุญาตขาย' },
    '차단': { EN: 'Blocked', JP: 'ブロック', CH: '屏蔽', TH: 'บล็อก' },
    '공산품(일반)': { EN: 'Goods (general)', JP: '日用品（一般）', CH: '工业品（一般）', TH: 'สินค้าทั่วไป' },
    '음식': { EN: 'Food', JP: '食品', CH: '食品', TH: 'อาหาร' },
    '동물': { EN: 'Animals', JP: '動物', CH: '动物', TH: 'สัตว์' },
    '서비스(일반)': { EN: 'Service (general)', JP: 'サービス（一般）', CH: '服务（一般）', TH: 'บริการ (ทั่วไป)' },
    '사람(서비스)': { EN: 'Person (service)', JP: '人（サービス）', CH: '人员（服务）', TH: 'บุคคล (บริการ)' },
    '판매(예약)': { EN: 'On sale (reservation)', JP: '販売（予約）', CH: '销售（预约）', TH: 'ขาย (จอง)' },
    '중지(대기)': { EN: 'Paused (standby)', JP: '停止（待機）', CH: '暂停（待机）', TH: 'หยุด (รอ)' },
    '시간예약': { EN: 'Time reservation', JP: '時間予約', CH: '时间预约', TH: 'จองเวลา' },
    '장소예약': { EN: 'Place reservation', JP: '場所予約', CH: '地点预约', TH: 'จองสถานที่' },
    '판매': { EN: 'Sale', JP: '販売', CH: '销售', TH: 'ขาย' },
    '예약금': { EN: 'Deposit', JP: '予約金', CH: '预约金', TH: 'มัดจำจอง' },
    '전액': { EN: 'Full amount', JP: '全額', CH: '全额', TH: 'เต็มจำนวน' },
    '프로모션': { EN: 'Promotion', JP: 'プロモーション', CH: '促销', TH: 'โปรโมชัน' },
    '예약결제': { EN: 'Reservation payment', JP: '予約決済', CH: '预约支付', TH: 'ชำระจอง' },
    '예약 결제': { EN: 'Reservation payment', JP: '予約決済', CH: '预约支付', TH: 'ชำระจอง' },
    '예약금액': { EN: 'Deposit amount', JP: '予約金額', CH: '预约金额', TH: 'จำนวนมัดจำ' },
    '예약슬롯(분)': { EN: 'Reservation slot (min)', JP: '予約スロット（分）', CH: '预约时段（分钟）', TH: 'สล็อตจอง (นาที)' },
    '판매·예약': { EN: 'Sale · reservation', JP: '販売・予約', CH: '销售·预约', TH: 'ขาย · จอง' },
    '판매상태': { EN: 'Sale status', JP: '販売状態', CH: '销售状态', TH: 'สถานะการขาย' },
    '항목구성': { EN: 'Item type', JP: '項目構成', CH: '项目类型', TH: 'ประเภทรายการ' },
    '본사 판매금지': {
      EN: 'HQ sale block', JP: '本社販売禁止', CH: '总部禁售', TH: 'HQ ห้ามขาย'
    },
    '가맹점명': { EN: 'Merchant name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้าน' },
    '상단 후보': { EN: 'Top promo candidate', JP: '上部候補', CH: '顶部候选', TH: 'ตัวเลือกด้านบน' },
    '이미지': { EN: 'Image', JP: '画像', CH: '图片', TH: 'รูปภาพ' },
    '관리': { EN: 'Actions', JP: '管理', CH: '管理', TH: 'จัดการ' },
    '신규등록': { EN: 'New registration', JP: '新規登録', CH: '新注册', TH: 'ลงทะเบียนใหม่' },
    '수정 취소': { EN: 'Cancel edit', JP: '修正取消', CH: '取消修改', TH: 'ยกเลิกแก้ไข' },
    '신규 등록 중입니다.': {
      EN: 'Registering new item.', JP: '新規登録中です。', CH: '正在新注册。', TH: 'กำลังลงทะเบียนใหม่'
    },
    '수정 중입니다. 저장하면 반영됩니다.': {
      EN: 'Editing. Save to apply.', JP: '修正中です。保存すると反映されます。', CH: '正在编辑。保存后生效。', TH: 'กำลังแก้ไข บันทึกเพื่อใช้'
    },
    '불러오기를 눌러 주세요.': {
      EN: 'Click Load.', JP: '読み込みを押してください。', CH: '请点击加载。', TH: 'กดโหลด'
    },
    '불러오기 후 목록이 표시됩니다. 상단 폼에서 신규등록하거나 목록에서 수정·삭제할 수 있습니다.': {
      EN: 'The list appears after Load. Register new items in the form above or edit/delete from the list.',
      JP: '読み込み後に一覧が表示されます。上部フォームで新規登録するか、一覧から修正・削除できます。',
      CH: '加载后显示列表。可在上方表单新注册或于列表中修改·删除。',
      TH: 'รายการแสดงหลังโหลด ลงทะเบียนใหม่ในฟอร์มด้านบนหรือแก้/ลบจากรายการ'
    },
    '열 너비: 표 헤더 각 칸의 오른쪽 가장자리를 드래그하면 열 너비를 조절할 수 있습니다. 설정은 이 브라우저에 저장됩니다.': {
      EN: 'Column width: drag the right edge of each header cell to resize. Settings are saved in this browser.',
      JP: '列幅: 表ヘッダ各セルの右端をドラッグして列幅を調整できます。設定はこのブラウザに保存されます。',
      CH: '列宽：拖动表头各格右缘可调整。设置保存在本浏览器。',
      TH: 'ความกว้างคอลัมน์: ลากขอบขวาหัวตารางเพื่อปรับ บันทึกในเบราว์เซอร์นี้'
    },
    '등록된 상품이 없습니다.': {
      EN: 'No registered products.', JP: '登録された商品がありません。', CH: '没有已注册商品。', TH: 'ไม่มีสินค้าที่ลงทะเบียน'
    },
    '상단 신규등록 폼에서 추가하세요.': {
      EN: 'Add from the new registration form above.', JP: '上部の新規登録フォームから追加してください。', CH: '请从上方新注册表单添加。', TH: 'เพิ่มจากฟอร์มลงทะเบียนใหม่ด้านบน'
    },
    '30분 단위': { EN: '30-min steps', JP: '30分単位', CH: '30 分钟步进', TH: 'ทีละ 30 นาที' },
    '기본': { EN: 'Default', JP: '基本', CH: '默认', TH: 'ค่าเริ่มต้น' },
    '고객 결제 금액은 예약금으로 징수되며, 잔액은 별도 정산·현장 수납 등 운영 정책에 따릅니다.': {
      EN: 'Customer payment is collected as a deposit; balance follows your settlement or on-site collection policy.',
      JP: '顧客決済金額は予約金として徴収し、残額は別途精算・現場収納など運用方針に従います。',
      CH: '客户支付金额作为预约金收取，余额按另行结算或现场收款等运营政策处理。',
      TH: 'เก็บเงินลูกค้าเป็นมัดจำ ส่วนที่เหลือตามนโยบายชำระ/รับหน้างาน'
    },
    '호텔·펜션 등: 고객 챗봇에서는 체크인 일시와(권장) 체크아웃 날짜를 받습니다. 객실 수·인원 등은 요청사항에 적도록 안내할 수 있습니다.': {
      EN: 'Hotels/pensions: the chatbot collects check-in time and (recommended) checkout date. Room count and guests can be guided in order notes.',
      JP: 'ホテル・ペンション等: 顧客チャットボットではチェックイン日時と（推奨）チェックアウト日を受け取ります。客室数・人数などは要望欄への記載を案内できます。',
      CH: '酒店/民宿等：聊天机器人收集入住时间与（建议）退房日。房间数·人数等可在备注中填写。',
      TH: 'โรงแรม/พension: แชทบอทรับเวลาเช็คอินและ(แนะนำ)วันช็คเอาต์ จำนวนห้อง/คนแนะนำในหมายเหตุ'
    },
    'Y=고객 챗봇·공개 카탈로그 비노출(개발·검수 등)': {
      EN: 'Y = hidden from public chatbot/catalog (dev/review etc.)',
      JP: 'Y=顧客チャットボット・公開カタログ非表示（開発・検収等）',
      CH: 'Y=不在客户聊天机器人/公开目录展示（开发/验收等）',
      TH: 'Y=ไม่แสดงในแชทบอท/แคตตalog สาธารณะ (dev/ตรวจ ฯลฯ)'
    },
    '저장 시 시스템이 코드를 자동 부여합니다.': {
      EN: 'Code is assigned automatically on save.', JP: '保存時にシステムがコードを自動付与します。', CH: '保存时系统自动分配代码。', TH: 'ระบบกำหนดรหัสอัตโนมัติเมื่อบันทึก'
    },
    '일반 판매 또는 예약 상품': {
      EN: 'Regular sale or reservation product', JP: '一般販売または予約商品', CH: '一般销售或预约商品', TH: 'ขายทั่วไปหรือสินค้าจอง'
    },
    '판매=고객 챗봇·카탈로그 노출, 대기=등록만(본사 차단 등 별개)': {
      EN: 'Sale = shown in chatbot/catalog; standby = registered only (separate from HQ block)',
      JP: '販売=顧客チャットボット・カタログ表示、待機=登録のみ（本社ブロック等は別）',
      CH: '销售=在聊天机器人/目录展示；待机=仅注册（与总部屏蔽无关）',
      TH: 'ขาย=แสดงในแชทบอท/แคตตalog รอ=ลงทะเบียนอย่างเดียว (แยกจาก HQ บล็อก)'
    },
    'Y=상단 프로모션 후보(표시 방식이 끔이면 고객 화면에는 안 나옴)': {
      EN: 'Y = top promo candidate (hidden on customer UI if display mode is off)',
      JP: 'Y=上部プロモーション候補（表示方式がオフなら顧客画面に出ません）',
      CH: 'Y=顶部促销候选（显示方式为关则客户界面不显示）',
      TH: 'Y=ตัวเลือกโปรโมด้านบน (โหมดปิดจะไม่แสดงให้ลูกค้า)'
    },
    '항목 성격(사람 서비스 등)을 지정하면 챗봇 응대 톤이 보정됩니다.': {
      EN: 'Specifying item nature (e.g. person service) adjusts chatbot tone.',
      JP: '項目性格（人サービス等）を指定するとチャットボット応対トーンが補正されます。',
      CH: '指定项目性质（如人员服务）可调整聊天机器人语气。',
      TH: 'ระบุลักษณะรายการ (เช่น บริการบุคคล) ปรับโทนแชทบอท'
    },
    '예약 상품만. 비우면 기본설정(분)': {
      EN: 'Reservation products only. Empty uses basic settings (min).',
      JP: '予約商品のみ。空なら基本設定（分）',
      CH: '仅预约商品。留空则用基本设置（分钟）',
      TH: 'เฉพาะสินค้าจอง ว่างใช้การตั้งค่าพื้นฐาน (นาที)'
    },
    '시간·장소 예약 상품의 선결제 금액 방식입니다.': {
      EN: 'Prepayment mode for time/place reservation products.',
      JP: '時間・場所予約商品の前払い金額方式です。',
      CH: '时间/地点预约商品的预付金额方式。',
      TH: 'โหมดจ่ายล่วงหน้าสำหรับสินค้าจองเวลา/สถานที่'
    },
    '예약금 모드일 때 결제에서 징수할 금액입니다.': {
      EN: 'Amount collected at checkout in deposit mode.',
      JP: '予約金モード時に決済で徴収する金額です。',
      CH: '预约金模式下支付时收取的金额。',
      TH: 'จำนวนที่เก็บเมื่อชำระในโหมดมัดจำ'
    },
    '등록건수 / 등록상한 · 판매활성/플랜활성상한 (0 또는 미설정이면 무제한)': {
      EN: 'Registered / reg. cap · sale active / plan active cap (0 or unset = unlimited)',
      JP: '登録件数/登録上限 · 販売有効/プラン有効上限（0または未設定は無制限）',
      CH: '注册数/注册上限 · 销售活跃/套餐活跃上限（0 或未设=不限）',
      TH: 'ลงทะเบียน/เพดาน · ขายอยู่/แพลน (0 หรือไม่ตั้ง=ไม่จำกัด)'
    },
    '채팅은 가능하나 고객용 상품·예약·결제만 일시 중지': {
      EN: 'Chat stays on; customer products, reservations, and checkout are paused',
      JP: 'チャットは可能だが顧客向け商品・予約・決済のみ一時停止',
      CH: '聊天可用，仅暂停客户商品·预约·支付',
      TH: 'แชทยังใช้ได้ หยุดเฉพาะสินค้า·จอง·ชำระของลูกค้า'
    },
    '구분': { EN: 'Type', JP: '区分', CH: '类型', TH: 'ประเภท' },
    '청구월': { EN: 'Billing month', JP: '請求月', CH: '计费月', TH: 'เดือนเรียกเก็บ' },
    '비용처리 내역이 없습니다.': {
      EN: 'No billing history.', JP: '費用処理履歴がありません。', CH: '没有费用处理记录。', TH: 'ไม่มีประวัติค่าใช้จ่าย'
    },
    '최근 청구월 ': { EN: 'Latest billing month ', JP: '最近請求月 ', CH: '最近计费月 ', TH: 'เดือนเรียกเก็บล่าสุด ' },
    '금액 ': { EN: 'Amount ', JP: '金額 ', CH: '金额 ', TH: 'จำนวน ' },
    '표시 방식': { EN: 'Display mode', JP: '表示方式', CH: '显示方式', TH: 'โหมดแสดง' },
    '순환 간격(초)': { EN: 'Rotation interval (sec)', JP: 'ローテーション間隔（秒）', CH: '轮播间隔（秒）', TH: 'ช่วงหมุน (วินาที)' },
    '편집 상품 · 상단 후보 포함': {
      EN: 'Editing product · include in top candidates', JP: '編集商品・上部候補に含む', CH: '编辑商品 · 纳入顶部候选', TH: 'แก้สินค้า · รวมตัวเลือกด้านบน'
    },
    '상단 후보 포함 도움말': {
      EN: 'Top candidate help', JP: '上部候補ヘルプ', CH: '顶部候选帮助', TH: 'คำช่วยตัวเลือกด้านบน'
    },
    '상단 후보 포함 안내': {
      EN: 'Include in top promo candidates for the product being edited.',
      JP: '編集中の商品を上部プロモーション候補に含めます。',
      CH: '将正在编辑的商品纳入顶部促销候选。',
      TH: 'รวมสินค้าที่แก้ในตัวเลือกโปรโมด้านบน'
    },
    '끔 (상단 숨김)': { EN: 'Off (hide top)', JP: 'オフ（上部非表示）', CH: '关（隐藏顶部）', TH: 'ปิด (ซ่อนด้านบน)' },
    '프로모션 (전체 그리드)': {
      EN: 'Promotion (full grid)', JP: 'プロモーション（全体グリッド）', CH: '促销（全网格）', TH: 'โปรโม (กริดเต็ม)'
    },
    '다이나믹 (3칸 순환)': {
      EN: 'Dynamic (3-slot rotation)', JP: 'ダイナミック（3枠ローテーション）', CH: '动态（三格轮播）', TH: 'ไดนามิก (3 ช่องหมุน)'
    },
    '미노출': { EN: 'Hidden', JP: '非表示', CH: '不展示', TH: 'ไม่แสดง' },
    '상단 프로모션': { EN: 'Top promotion', JP: '上部プロモーション', CH: '顶部促销', TH: 'โปรโมด้านบน' },
    '30의 배수': { EN: 'Multiples of 30', JP: '30の倍数', CH: '30 的倍数', TH: 'ทวีคูณของ 30' },

    /* HQ /hq/serverManage — 서버운영관리 (screens L + app.js pgAdminUiT) */
    'SSL 인증서 모니터링': {
      EN: 'SSL certificate monitoring',
      JP: 'SSL証明書モニタリング',
      CH: 'SSL 证书监控',
      TH: 'การตรวจสอบใบรับรอง SSL'
    },
    'Let\u2019s Encrypt: Nginx가 사용하는 fullchain.pem 을 모니터링합니다. live 폴더명은 certbot 인증서 이름(예: api.icopay.co.kr)과 동일합니다. 다중 서브도메인(SAN)은 한 장의 인증서에 포함됩니다. 카페24 등 권한 DNS에 A 레코드가 VPS IP를 가리키는지·일부 ISP DNS 캐시로 예전 IP가 남지 않는지 확인하세요. 조회·저장은 시스템 관리자(ADMIN)만 가능합니다.': {
      EN: 'Let\u2019s Encrypt: monitors the <code>fullchain.pem</code> used by Nginx. The live folder name matches the certbot certificate name (e.g. api.icopay.co.kr). Multiple subdomains (SAN) are on one certificate. Check that authoritative DNS (e.g. Cafe24) A records point to the VPS IP and that stale IPs are not cached by some ISPs. View/save: system administrators (ADMIN) only.',
      JP: 'Let\u2019s Encrypt: Nginxが使う<code>fullchain.pem</code>を監視します。liveフォルダ名はcertbotの証明書名（例: api.icopay.co.kr）と同じです。複数サブドメイン（SAN）は1枚の証明書に含まれます。権威DNS（例: カフェ24）のAレコードがVPSのIPを指しているか、一部ISPのDNSキャッシュに古いIPが残っていないか確認してください。参照・保存はシステム管理者(ADMIN)のみ可能です。',
      CH: 'Let\u2019s Encrypt：监控 Nginx 使用的 <code>fullchain.pem</code>。live 文件夹名与 certbot 证书名（如 api.icopay.co.kr）一致。多子域名（SAN）在同一张证书上。请确认权威 DNS（如 Cafe24）A 记录指向 VPS IP，且部分 ISP DNS 缓存未残留旧 IP。查看与保存仅限系统管理员(ADMIN)。',
      TH: 'Let\u2019s Encrypt: ตรวจ <code>fullchain.pem</code> ที่ Nginx ใช้ โฟลเดอร์ live ตรงกับชื่อใบรับรอง certbot (เช่น api.icopay.co.kr) หลายซับโดเมน (SAN) อย่างใบเดียว ตรวจ DNS หลักว่า A ชี้ IP ของ VPS และแคช ISP ไม่ค้าง IP เก่า ดู/บันทึกได้เฉพาะ ADMIN'
    },
    'fullchain.pem 경로': {
      EN: 'fullchain.pem path',
      JP: 'fullchain.pem パス',
      CH: 'fullchain.pem 路径',
      TH: 'พาธ fullchain.pem'
    },
    'LE live 폴더명(인증서 이름)': {
      EN: 'LE live folder name (certificate name)',
      JP: 'LE liveフォルダ名（証明書名）',
      CH: 'LE live 文件夹名（证书名）',
      TH: 'ชื่อโฟลเดอร์ live ของ LE (ชื่อใบรับรอง)'
    },
    '실시간 대시보드 자동 갱신(분)': {
      EN: 'Live dashboard auto-refresh (minutes)',
      JP: 'リアルタイムダッシュボード自動更新（分）',
      CH: '实时仪表盘自动刷新（分钟）',
      TH: 'รีเฟรชดชบอร์ดอัตโนมัติ (นาที)'
    },
    '비우면 서버 기본': {
      EN: 'Leave empty for server default',
      JP: '空欄でサーバ既定',
      CH: '留空则使用服务器默认',
      TH: 'ว่างไว้ใช้ค่าเริ่มต้นของเซิร์ฟเวอร์'
    },
    '1~60분만 저장됩니다(내부는 초로 환산). 비우면 <code>application.yml</code>의 <code>app.serverManage.uiAutoRefreshSeconds</code>가 적용됩니다. 아래 [설정 저장]과 동일하게 전체 폼을 저장합니다.': {
      EN: 'Only 1–60 minutes are stored (converted to seconds internally). If empty, <code>app.serverManage.uiAutoRefreshSeconds</code> in <code>application.yml</code> applies. Saves the whole form, same as [Save settings] below.',
      JP: '1〜60分のみ保存されます（内部は秒に換算）。空欄なら<code>application.yml</code>の<code>app.serverManage.uiAutoRefreshSeconds</code>が適用されます。下の［設定を保存］と同様にフォーム全体を保存します。',
      CH: '仅保存 1–60 分钟（内部换算为秒）。留空则使用 <code>application.yml</code> 中的 <code>app.serverManage.uiAutoRefreshSeconds</code>。与下方[保存设置]相同，保存整个表单。',
      TH: 'บันทึกได้ 1–60 นาที (แปลงเป็นวินาทีภายใน) ว่างไว้ใช้ <code>app.serverManage.uiAutoRefreshSeconds</code> ใน <code>application.yml</code> เหมือนปุ่ม [บันทึกการตั้งค่า] ด้านล่าง บันทึกทั้งฟอร์ม'
    },
    '호스팅 약정': {
      EN: 'Hosting contract',
      JP: 'ホスティング契約',
      CH: '主机托管合约',
      TH: 'สัญญาโฮสติ้ง'
    },
    '디스크·트래픽은 GB 단위로 입력합니다(소수 가능). 저장 시 서버에 MB로 환산되어 저장됩니다. 디스크 사용량은 서버 조회값과 약정을 비교합니다. 트래픽 누적은 호스팅 패널 값을 넣거나, 약정 시작일이 있으면 앱이 수집한 일별 트래픽 합으로 폼을 자동 채웁니다(패널과 다를 수 있으니 확인 후 저장).': {
      EN: 'Enter disk and traffic in GB (decimals allowed). On save, values are converted to MB on the server. Disk usage compares live server readings to the contract. For cumulative traffic, enter the hosting panel value, or if a contract start date exists, the form may auto-fill from daily traffic collected by the app (may differ from the panel—verify before saving).',
      JP: 'ディスク・トラフィックはGB単位で入力（小数可）。保存時にサーバーへMB換算で保存されます。ディスク使用量はサーバー取得値と契約を比較します。累積トラフィックはパネル値を入れるか、契約開始日がある場合はアプリが収集した日次トラフィック合計で自動入力されることがあります（パネルと異なる場合があるため保存前に確認）。',
      CH: '磁盘与流量以 GB 为单位输入（可小数）。保存时在服务器上换算为 MB。磁盘用量对比服务器读数与合约。累计流量可填主机面板值；若有合约开始日期，表单可能用应用采集的日流量合计自动填充（可能与面板不一致，保存前请确认）。',
      TH: 'ดิสก์และทราฟฟิกกรอกเป็น GB (ทศนิยมได้) บันทึกเป็น MB บนเซิร์ฟเวอร์ การใช้ดิสก์เทียบค่าจริงกับสัญญา ทราฟฟิกสะสมใส่จากแพเนล หรือถ้ามีวันเริ่มสัญญา ระบบอาจเติมจากยอดรายวันที่แอปรวบรวม (อาจไม่ตรงแพเนล ตรวจก่อนบันทึก)'
    },
    '약정 디스크 (GB)': {
      EN: 'Contracted disk (GB)',
      JP: '契約ディスク（GB）',
      CH: '约定磁盘（GB）',
      TH: 'ดิสก์ตามสัญญา (GB)'
    },
    '약정 트래픽 (GB/기간)': {
      EN: 'Contracted traffic (GB / period)',
      JP: '契約トラフィック（GB/期間）',
      CH: '约定流量（GB/周期）',
      TH: 'ทราฟฟิกตามสัญญา (GB/รอบ)'
    },
    '트래픽 누적 사용 (GB)': {
      EN: 'Cumulative traffic used (GB)',
      JP: 'トラフィック累積使用量（GB）',
      CH: '累计已用流量（GB）',
      TH: 'ทราฟฟิกสะสมที่ใช้ (GB)'
    },
    '예: 1 또는 0.977': {
      EN: 'e.g. 1 or 0.977',
      JP: '例: 1 または 0.977',
      CH: '例：1 或 0.977',
      TH: 'เช่น 1 หรือ 0.977'
    },
    '예: 1.5': {
      EN: 'e.g. 1.5',
      JP: '例: 1.5',
      CH: '例：1.5',
      TH: 'เช่น 1.5'
    },
    '패널 누적': {
      EN: 'Panel cumulative',
      JP: 'パネル累積',
      CH: '面板累计',
      TH: 'ยอดสะสมจากแพเนล'
    },
    '약정 시작일': {
      EN: 'Contract start',
      JP: '契約開始日',
      CH: '合约开始日',
      TH: 'วันเริ่มสัญญา'
    },
    '약정 종료일': {
      EN: 'Contract end',
      JP: '契約終了日',
      CH: '合约结束日',
      TH: 'วันสิ้นสัญญา'
    },
    '실시간 대시보드': {
      EN: 'Live dashboard',
      JP: 'リアルタイムダッシュボード',
      CH: '实时仪表盘',
      TH: 'แดชบอร์ดแบบเรียลไทม์'
    },
    'SSL 카드에 인증서 SAN(호스트명) 목록과 운영 안내(카페24 DNS·Cloudflare·다중 -d)가 포함됩니다. 도메인구성설정 화면에서는 전사·조직 URL과 SAN 대조 표가 함께 표시됩니다. 레이아웃은 NOTI GitHub 저장소의 /admin/system-monitor를 참고했습니다. PG는 Spring API(JSON)로 채웁니다. 교차 출처 접속 시 상단 안내를 확인하세요.': {
      EN: 'The SSL card lists certificate SAN (hostnames) and ops notes (Cafe24 DNS, Cloudflare, multiple -d). On Domain configuration, company-wide and org URLs are shown with a SAN comparison table. Layout follows NOTI GitHub /admin/system-monitor. PG fills via Spring API (JSON). If cross-origin, read the notice at the top.',
      JP: 'SSLカードに証明書SAN（ホスト名）と運用案内（カフェ24 DNS・Cloudflare・複数-d）が含まれます。ドメイン構成画面では全社・組織URLとSAN突合せ表が一緒に表示されます。レイアウトはNOTIのGitHub /admin/system-monitorを参考にしています。PGはSpring API(JSON)で埋めます。クロスオリジン時は上の案内を確認してください。',
      CH: 'SSL 卡含证书 SAN（主机名）与运维说明（Cafe24 DNS、Cloudflare、多 -d）。域名配置页同时显示全公司与组织 URL 及 SAN 对照表。布局参考 NOTI GitHub 的 /admin/system-monitor。PG 由 Spring API(JSON) 填充。跨域访问时请查看顶部说明。',
      TH: 'การ์ด SSL มี SAN และคำแนะ DNS/Cloudflare/-d หน้าตั้งค่าโดเมนมีตารางเทียบ URL กับ SAN โครงร่างอ้างอิง NOTI /admin/system-monitor เติมด้วย Spring API ข้ามโดเมนดูประกาศด้านบน'
    },
    '자동 갱신': {
      EN: 'Auto refresh',
      JP: '自動更新',
      CH: '自动刷新',
      TH: 'รีเฟรชอัตโนมัติ'
    },
    '간격': {
      EN: 'Interval',
      JP: '間隔',
      CH: '间隔',
      TH: 'ช่วงเวลา'
    },
    '트래픽 · 메모리 피크': {
      EN: 'Traffic · memory peak',
      JP: 'トラフィック・メモリピーク',
      CH: '流量 · 内存峰值',
      TH: 'ทราฟฟิก · พีคหน่วยความจำ'
    },
    '일간': { EN: 'Daily', JP: '日次', CH: '按日', TH: 'รายวัน' },
    '주간': { EN: 'Weekly', JP: '週次', CH: '按周', TH: 'รายสัปดาห์' },
    '원본 JSON (디버그)': {
      EN: 'Raw JSON (debug)',
      JP: '生JSON（デバッグ）',
      CH: '原始 JSON（调试）',
      TH: 'JSON ดิบ (ดีบัก)'
    },
    '기간': {
      EN: 'Period',
      JP: '期間',
      CH: '周期',
      TH: 'ช่วง'
    },
    '설정 저장': {
      EN: 'Save settings',
      JP: '設定を保存',
      CH: '保存设置',
      TH: 'บันทึกการตั้งค่า'
    },
    '요약 새로고침': {
      EN: 'Refresh summary',
      JP: '要約を再読込',
      CH: '刷新摘要',
      TH: 'รีเฟรชสรุป'
    },
    '일간/주간/월간 전환 시 그래프·요약이 바뀝니다. 수집은 앱이 주기적으로 수행합니다. 레이아웃은 <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 시스템 모니터를 참고했습니다.': {
      EN: 'Switching daily/weekly/monthly changes the chart and summary. Collection runs on a schedule in the app. Layout follows the <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> system monitor.',
      JP: '日次/週次/月次の切替でグラフ・要約が変わります。収集はアプリが周期実行します。レイアウトは<a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a>のシステムモニタを参考にしています。',
      CH: '切换日/周/月会更改图表与摘要。采集由应用定期执行。布局参考 <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 系统监控。',
      TH: 'สลับรายวัน/สัปดาห์/เดือนเปลี่ยนกราฟและสรุป แอปเก็บตามรอบ โครงร่างอ้างอิง <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a>'
    },
    '<strong>구조 안내 (NOTI 대비)</strong> <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 서버관리는 Node가 <em>같은 출처</em>로 HTML을 내려 세션만으로 조회합니다. PG 관리자는 브라우저가 <code>{0}</code> 로 API를 호출합니다. 목록이 비면 CORS·방화벽·최신 JAR를 확인하거나, <strong>API와 동일 호스트</strong>에서 관리자를 여는 것을 권장합니다.': {
      EN: '<strong>Architecture (vs NOTI)</strong> <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> server admin serves HTML on the <em>same origin</em> and queries with session only. This PG admin calls the API from the browser at <code>{0}</code>. If the list is empty, check CORS, firewall, latest JAR, or open the admin on the <strong>same host as the API</strong>.',
      JP: '<strong>構成案内（NOTI比較）</strong> <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> のサーバ管理はNodeが<em>同一オリジン</em>でHTMLを返しセッションのみで参照します。PG管理画面はブラウザから<code>{0}</code>へAPIを呼びます。一覧が空ならCORS・ファイアウォール・最新JARを確認するか、<strong>APIと同一ホスト</strong>で管理画面を開くことを推奨します。',
      CH: '<strong>架构说明（对照 NOTI）</strong> <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 服务端管理由 Node 在<em>同源</em>下提供 HTML，仅用会话查询。PG 管理端由浏览器调用 <code>{0}</code> 的 API。若列表为空，请检查 CORS、防火墙、最新 JAR，或在<strong>与 API 相同主机</strong>上打开管理端。',
      TH: '<strong>โครงสร้าง (เทียบ NOTI)</strong> <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> ใช้ Node ส่ง HTML <em>same origin</em> กับเซสชันเท่านั้น แอดมิน PG เรียก API ที่ <code>{0}</code> หากว่าง ตรวจ CORS/ไฟร์วอลล์/JAR ล่าสุด หรือเปิดแอดมินบน<strong>โฮสต์เดียวกับ API</strong>'
    },
    '{0}일 {1}시간 {2}분': {
      EN: '{0}d {1}h {2}m',
      JP: '{0}日{1}時間{2}分',
      CH: '{0}天{1}小时{2}分',
      TH: '{0} วัน {1} ชม. {2} นาที'
    },
    '위험': { EN: 'Critical', JP: '危険', CH: '危险', TH: 'วิกฤต' },
    '주의': { EN: 'Warning', JP: '注意', CH: '注意', TH: 'คำเตือน' },
    '양호': { EN: 'OK', JP: '良好', CH: '正常', TH: 'ปกติ' },
    '현황 요약': {
      EN: 'Summary',
      JP: '状況サマリ',
      CH: '现状摘要',
      TH: 'สรุปสถานะ'
    },
    '아래 그래프와 동일 데이터': {
      EN: 'same data as the charts below',
      JP: '下のグラフと同じデータ',
      CH: '与下方图表相同的数据',
      TH: 'ข้อมูลเดียวกับกราฟด้านล่าง'
    },
    '아직 누적 데이터가 거의 없습니다. 앱이 서버에서 수집(기본 10분 간격)을 수행하면 일별로 쌓입니다.': {
      EN: 'Little aggregated data yet. Once the app collects from the server (default every 10 minutes), it accumulates by day.',
      JP: 'まだ累積データがほとんどありません。アプリがサーバーから収集（既定10分間隔）すると日次で溜まります。',
      CH: '累计数据尚少。应用从服务器采集（默认每 10 分钟）后会按日累积。',
      TH: 'ข้อมูลสะสมยังน้อย เมื่อแอปเก็บจากเซิร์ฟเวอร์ (ค่าเริ่มต้นทุก 10 นาที) จะสะสมรายวัน'
    },
    '그래프 구간 수:': {
      EN: 'Bars/points in chart:',
      JP: 'グラフ区間数:',
      CH: '图表区间数：',
      TH: 'จำนวนช่วงในกราฟ:'
    },
    '일간 최대 {0}일': {
      EN: 'daily max {0} days',
      JP: '日次最大{0}日',
      CH: '日视图最多 {0} 天',
      TH: 'รายวันสูงสุด {0} วัน'
    },
    '최근 7일 트래픽 합:': {
      EN: 'Last 7 days traffic sum:',
      JP: '直近7日トラフィック合計:',
      CH: '近 7 日流量合计：',
      TH: 'ผลรวมทราฟฟิก 7 วัน:'
    },
    '그래프 기간 트래픽 합:': {
      EN: 'Traffic sum for chart period:',
      JP: 'グラフ期間のトラフィック合計:',
      CH: '图表周期内流量合计：',
      TH: 'ผลรวมทราฟฟิกในช่วงกราฟ:'
    },
    '가장 최근 일({0}) 트래픽': {
      EN: 'Latest day ({0}) traffic',
      JP: '直近日({0})のトラフィック',
      CH: '最近一日（{0}）流量',
      TH: 'ทราฟฟิกวันล่าสุด ({0})'
    },
    '전일': { EN: 'Previous day', JP: '前日', CH: '前一日', TH: 'วันก่อน' },
    '증감': { EN: 'Delta', JP: '増減', CH: '增减', TH: 'เดลตา' },
    '최근 31일 기준 일일 트래픽 최대:': {
      EN: 'Max daily traffic in last 31 days:',
      JP: '直近31日基準の日次トラフィック最大:',
      CH: '近 31 天单日流量峰值：',
      TH: 'ทราฟฟิกสูงสุดต่อวันใน 31 วัน:'
    },
    '일평균 트래픽(트래픽이 있었던 날만):': {
      EN: 'Avg daily traffic (days with traffic only):',
      JP: '日平均トラフィック（トラフィックがあった日のみ）:',
      CH: '日均流量（仅计有流量的天）：',
      TH: 'เฉลี่ย/วัน (เฉพาะวันที่มีทราฟฟิก):'
    },
    '메모리 일일 피크(%): 그래프 최근 값': {
      EN: 'Memory daily peak (%): latest on chart',
      JP: 'メモリ日次ピーク(%)：グラフの直近値',
      CH: '内存日峰值(%)：图表最近值',
      TH: 'พีคหน่วยความจำรายวัน (%): ค่าล่าสุดบนกราฟ'
    },
    '기간 최대': {
      EN: 'period max',
      JP: '期間最大',
      CH: '周期内最大',
      TH: 'สูงสุดในช่วง'
    },
    '오른쪽 붉은 그래프': {
      EN: 'red chart on the right',
      JP: '右の赤いグラフ',
      CH: '右侧红色图',
      TH: 'กราฟสีแดงทางขวา'
    },
    '차트 데이터를 불러오지 못했습니다. ADMIN·최신 JAR·DB V45·/api/hq/serverUsage 를 확인하세요.': {
      EN: 'Could not load chart data. Check ADMIN, latest JAR, DB V45, and /api/hq/serverUsage.',
      JP: 'チャートデータを読み込めませんでした。ADMIN・最新JAR・DB V45・/api/hq/serverUsageを確認してください。',
      CH: '无法加载图表数据。请检查 ADMIN、最新 JAR、DB V45 与 /api/hq/serverUsage。',
      TH: 'โหลดข้อมูลกราฟไม่สำเร็จ ตรวจ ADMIN, JAR ล่าสุด, DB V45 และ /api/hq/serverUsage'
    },
    '트래픽 (송수신 합, GB)': {
      EN: 'Traffic (RX+TX, GB)',
      JP: 'トラフィック（送受信合計, GB）',
      CH: '流量（收发合计，GB）',
      TH: 'ทราฟฟิก (รับ+ส่ง, GB)'
    },
    '메모리 피크 (%)': {
      EN: 'Memory peak (%)',
      JP: 'メモリピーク（%）',
      CH: '内存峰值（%）',
      TH: 'พีคหน่วยความจำ (%)'
    },
    '조회 시각:': {
      EN: 'Fetched at:',
      JP: '取得時刻:',
      CH: '查询时间：',
      TH: 'เวลาดึงข้อมูล:'
    },
    '{0}분': { EN: '{0} min', JP: '{0}分', CH: '{0} 分钟', TH: '{0} นาที' },
    '{0}분 {1}초': { EN: '{0} min {1} s', JP: '{0}分{1}秒', CH: '{0} 分 {1} 秒', TH: '{0} นาที {1} วินาที' },
    '{0}초': { EN: '{0} s', JP: '{0}秒', CH: '{0} 秒', TH: '{0} วินาที' },
    '헬스 경고': {
      EN: 'Health alerts',
      JP: 'ヘルス警告',
      CH: '健康告警',
      TH: 'คำเตือนสุขภาพระบบ'
    },
    '데이터가 없습니다.': {
      EN: 'No data.',
      JP: 'データがありません。',
      CH: '无数据。',
      TH: 'ไม่มีข้อมูล'
    },
    '대시보드 데이터를 불러오지 못했습니다': {
      EN: 'Could not load dashboard data',
      JP: 'ダッシュボードデータを読み込めませんでした',
      CH: '无法加载仪表盘数据',
      TH: 'โหลดแดชบอร์ดไม่สำเร็จ'
    },
    '서버운영 대시보드 오류 안내 앞': {
      EN: 'Click [Refresh summary] and check the',
      JP: '[要約を再読込]を押し、',
      CH: '点击[刷新摘要]并在',
      TH: 'กด [รีเฟรชสรุป] แล้วดู'
    },
    '서버운영 대시보드 오류 안내 뒤': {
      EN: 'response in F12 Network. Check ADMIN, API base URL, and CORS (latest JAR).',
      JP: 'F12のNetworkで応答を確認してください。ADMIN・API基準URL・CORS（最新JAR）を点検してください。',
      CH: 'F12 Network 中的响应。检查 ADMIN、API 基址与 CORS（最新 JAR）。',
      TH: 'คำตอบใน F12 Network ตรวจ ADMIN, URL ฐาน API และ CORS (JAR ล่าสุด)'
    },
    '시스템 메모리': {
      EN: 'System memory',
      JP: 'システムメモリ',
      CH: '系统内存',
      TH: 'หน่วยความจำระบบ'
    },
    '가용 {0} / 총 {1} MB': {
      EN: 'Avail {0} / total {1} MB',
      JP: '利用可能 {0} / 合計 {1} MB',
      CH: '可用 {0} / 共 {1} MB',
      TH: 'ว่าง {0} / รวม {1} MB'
    },
    'JVM 힙': {
      EN: 'JVM heap',
      JP: 'JVMヒープ',
      CH: 'JVM 堆',
      TH: 'JVM heap'
    },
    'Load(1m) · CPU': {
      EN: 'Load (1m) · CPU',
      JP: 'Load(1m)・CPU',
      CH: '负载(1m) · CPU',
      TH: 'Load(1m) · CPU'
    },
    '논리 {0} 코어': {
      EN: '{0} logical cores',
      JP: '論理{0}コア',
      CH: '{0} 个逻辑核心',
      TH: '{0} คอร์ลอจิคัล'
    },
    '업타임': { EN: 'Uptime', JP: '稼働時間', CH: '运行时间', TH: 'เวลาทำงาน' },
    '디스크 사용': {
      EN: 'Disk usage',
      JP: 'ディスク使用',
      CH: '磁盘使用',
      TH: 'การใช้ดิสก์'
    },
    '조회 불가': {
      EN: 'Unavailable',
      JP: '取得不可',
      CH: '无法查询',
      TH: 'ไม่พร้อมใช้'
    },
    'renewal .conf {0}개': {
      EN: 'renewal .conf files: {0}',
      JP: 'renewal .conf: {0}件',
      CH: 'renewal .conf：{0} 个',
      TH: 'renewal .conf: {0} ไฟล์'
    },
    '실제 읽은 경로': {
      EN: 'Resolved path read',
      JP: '実際に読んだパス',
      CH: '实际读取路径',
      TH: 'พาธที่อ่านจริง'
    },
    'DB 저장 경로': {
      EN: 'Path stored in DB',
      JP: 'DB保存パス',
      CH: '数据库保存路径',
      TH: 'พาธที่บันทึกใน DB'
    },
    'LE live 폴더명': {
      EN: 'LE live folder name',
      JP: 'LE liveフォルダ名',
      CH: 'LE live 文件夹名',
      TH: 'ชื่อโฟลเดอร์ live ของ LE'
    },
    '유효 기간': {
      EN: 'Validity',
      JP: '有効期間',
      CH: '有效期',
      TH: 'ช่วงความถูกต้อง'
    },
    '잔여 일수': {
      EN: 'Days remaining',
      JP: '残り日数',
      CH: '剩余天数',
      TH: 'วันที่เหลือ'
    },
    'SAN — 브라우저 호스트명 (dNSName)': {
      EN: 'SAN — browser hostname (dNSName)',
      JP: 'SAN — ブラウザホスト名（dNSName）',
      CH: 'SAN — 浏览器主机名（dNSName）',
      TH: 'SAN — ชื่อโฮสต์ (dNSName)'
    },
    '환경변수': {
      EN: 'Environment variable',
      JP: '環境変数',
      CH: '环境变量',
      TH: 'ตัวแปรสภาพแวดล้อม'
    },
    '선택(옵션)': {
      EN: 'optional',
      JP: '任意',
      CH: '可选',
      TH: 'ไม่บังคับ'
    },
    '운영 안내 (DNS·SAN·프록시)': {
      EN: 'Ops notes (DNS · SAN · proxy)',
      JP: '運用案内（DNS・SAN・プロキシ）',
      CH: '运维说明（DNS·SAN·代理）',
      TH: 'คำแนะการดำเนินงาน (DNS·SAN·พร็อกซี)'
    },
    '인증서 파일을 찾을 수 없습니다. 경로 또는 LE live 폴더명(예: api.icopay.co.kr)을 저장하세요.': {
      EN: 'Certificate file not found. Save the path or LE live folder name (e.g. api.icopay.co.kr).',
      JP: '証明書ファイルが見つかりません。パスまたはLE liveフォルダ名（例: api.icopay.co.kr）を保存してください。',
      CH: '找不到证书文件。请保存路径或 LE live 文件夹名（如 api.icopay.co.kr）。',
      TH: 'ไม่พบไฟล์ใบรับรอง บันทึกพาธหรือชื่อโฟลเดอร์ live ของ LE (เช่น api.icopay.co.kr)'
    },
    'PEM 형식이 아닙니다.': {
      EN: 'Not PEM format.',
      JP: 'PEM形式ではありません。',
      CH: '不是 PEM 格式。',
      TH: 'ไม่ใช่รูปแบบ PEM'
    },
    'Certbot timer (systemd)': {
      EN: 'Certbot timer (systemd)',
      JP: 'Certbot タイマー（systemd）',
      CH: 'Certbot 定时器（systemd）',
      TH: 'Certbot timer (systemd)'
    },
    '관리자만 조회할 수 있습니다.': {
      EN: 'Only administrators can view this.',
      JP: '管理者のみ参照できます。',
      CH: '仅管理员可查看。',
      TH: 'เฉพาะผู้ดูแลระบบเท่านั้นที่ดูได้'
    },
    '관리자만 저장할 수 있습니다.': {
      EN: 'Only administrators can save.',
      JP: '管理者のみ保存できます。',
      CH: '仅管理员可保存。',
      TH: 'เฉพาะผู้ดูแลระบบเท่านั้นที่บันทึกได้'
    },
    '비밀번호가 초기화되었습니다.': {
      EN: 'Password has been reset.',
      JP: 'パスワードを初期化しました。',
      CH: '密码已重置。',
      TH: 'รีเซ็ตรหัสผ่านแล้ว'
    },
    '비밀번호가 초기화되었습니다. 임시비밀번호: {0}': {
      EN: 'Password has been reset. Temporary password: {0}',
      JP: 'パスワードを初期化しました。仮パスワード: {0}',
      CH: '密码已重置。临时密码: {0}',
      TH: 'รีเซ็ตรหัสผ่านแล้ว รหัสชั่วคราว: {0}'
    },
    '보조 노티(URL 3·4)를 쓰려면 본사 노티구성설정에서 이 총판에 필수 노티(URL 1·2)가 연결되어 있어야 합니다.': {
      EN: 'To use auxiliary notifications (URL 3·4), mandatory notifications (URL 1·2) must be linked to this master distributor in HQ notification settings.',
      JP: '補助通知（URL 3·4）を使うには、本社の通知構成設定でこの総販に必須通知（URL 1·2）が接続されている必要があります。',
      CH: '要使用辅助通知（URL 3·4），须在本部通知配置中为该总经销关联必填通知（URL 1·2）。',
      TH: 'หากต้องการใช้การแจ้งเตือนเสริม (URL 3·4) ต้องเชื่อมการแจ้งเตือนบังคับ (URL 1·2) กับตัวแทนหลักนี้ในการตั้งค่าการแจ้งเตือนของสำนักงานใหญ่'
    },
    '노티 URL 3·4(보조)를 쓰려면 URL 1·2(CALLBACK·RESULT)가 모두 필요합니다.': {
      EN: 'To use notification URL 3·4 (auxiliary), both URL 1·2 (CALLBACK·RESULT) are required.',
      JP: '通知 URL 3·4（補助）を使うには URL 1·2（CALLBACK·RESULT）が両方必要です。',
      CH: '要使用通知 URL 3·4（辅助），必须同时填写 URL 1·2（CALLBACK·RESULT）。',
      TH: 'หากต้องการใช้ URL การแจ้งเตือน 3·4 (เสริม) ต้องมี URL 1·2 (CALLBACK·RESULT) ครบทั้งคู่'
    },
    '초기화되었습니다.': {
      EN: 'Reset completed.',
      JP: '初期化しました。',
      CH: '已初始化。',
      TH: 'รีเซ็ตแล้ว'
    },
    '처리되었습니다.': {
      EN: 'Processed successfully.',
      JP: '処理しました。',
      CH: '已处理。',
      TH: 'ดำเนินการแล้ว'
    },
    '인증번호를 발송했습니다.\n수신: {0}': {
      EN: 'Verification code sent.\nSent to: {0}',
      JP: '認証番号を送信しました。\n送信先: {0}',
      CH: '验证码已发送。\n发送至: {0}',
      TH: 'ส่งรหัสยืนยันแล้ว\nส่งไปที่: {0}'
    },
    'Let\u2019s Encrypt fullchain.pem 을 읽어 만료·SAN·지문을 표시합니다. 상단 폼의 LE live 폴더명(인증서 이름)을 저장하면 경로가 맞춰집니다. 도메인 URL과 SAN 대조는 <strong>도메인구성설정</strong> 화면을 사용하세요.': {
      EN: 'Reads Let\u2019s Encrypt <code>fullchain.pem</code> to show expiry, SAN, and fingerprint. Saving the LE live folder name (certificate name) in the form above aligns the path. Compare domain URLs and SAN on the <strong>Domain configuration</strong> screen.',
      JP: 'Let\u2019s Encrypt の<code>fullchain.pem</code>を読み取り、有効期限・SAN・フィンガープリントを表示します。上のフォームでLE liveフォルダ名（証明書名）を保存するとパスが揃います。ドメインURLとSANの突合せは<strong>ドメイン構成</strong>画面を使用してください。',
      CH: '读取 Let\u2019s Encrypt <code>fullchain.pem</code> 显示过期、SAN 与指纹。在上方表单保存 LE live 文件夹名（证书名）可对齐路径。域名 URL 与 SAN 对照请使用<strong>域名配置</strong>页面。',
      TH: 'อ่าน Let\u2019s Encrypt <code>fullchain.pem</code> แสดงหมดอายุ SAN และลายนิ้ว บันทึกชื่อโฟลเดอร์ live ด้านบนเพื่อจัดพาธ เปรียบ URL กับ SAN ที่<strong>การตั้งค่าโดเมน</strong>'
    },
    'SSL 인증서': {
      EN: 'SSL certificate',
      JP: 'SSL証明書',
      CH: 'SSL 证书',
      TH: 'ใบรับรอง SSL'
    },
    'Certbot · 갱신': {
      EN: 'Certbot · renewal',
      JP: 'Certbot・更新',
      CH: 'Certbot · 续期',
      TH: 'Certbot · ต่ออายุ'
    },
    '<code>certbot.timer</code> 가 주기적으로 <code>certbot renew</code> 를 실행합니다. 만료 30일 전부터 갱신이 시도됩니다. 서브도메인 추가 시에는 수동으로 <code>certbot --nginx -d …</code> 로 인증서를 확장한 뒤 Nginx를 리로드하세요.': {
      EN: '<code>certbot.timer</code> runs <code>certbot renew</code> periodically. Renewal is attempted from 30 days before expiry. To add a subdomain, manually extend the cert with <code>certbot --nginx -d …</code> then reload Nginx.',
      JP: '<code>certbot.timer</code>が周期的に<code>certbot renew</code>を実行します。期限30日前から更新が試みられます。サブドメイン追加時は手動で<code>certbot --nginx -d …</code>により証明書を拡張した後、Nginxをリロードしてください。',
      CH: '<code>certbot.timer</code> 定期执行 <code>certbot renew</code>。到期前 30 天起尝试续期。新增子域名请手动用 <code>certbot --nginx -d …</code> 扩展证书后重载 Nginx。',
      TH: '<code>certbot.timer</code> รัน <code>certbot renew</code> เป็นระยะ ลองต่ออายุก่อนหมด 30 วัน เพิ่มซับโดเมนขยายด้วย <code>certbot --nginx -d …</code> แล้ว reload Nginx'
    },
    '다음 실행(원시):': {
      EN: 'Next run (raw):',
      JP: '次回実行（生）:',
      CH: '下次执行（原始）：',
      TH: 'รันถัดไป (ดิบ):'
    },
    'renewal/*.conf ({0})': {
      EN: 'renewal/*.conf ({0})',
      JP: 'renewal/*.conf（{0}）',
      CH: 'renewal/*.conf（{0}）',
      TH: 'renewal/*.conf ({0})'
    },
    '… 외 {0}개': {
      EN: '… and {0} more',
      JP: '… 他{0}件',
      CH: '… 另有 {0} 项',
      TH: '… อีก {0} รายการ'
    },
    '상태:': { EN: 'Status:', JP: '状態:', CH: '状态：', TH: 'สถานะ:' },
    'stub_status URL 미설정 (<code>NGINX_STUB_STATUS_URL</code> 또는 <code>app.serverManage.nginxStubStatusUrl</code>).': {
      EN: 'stub_status URL not set (<code>NGINX_STUB_STATUS_URL</code> or <code>app.serverManage.nginxStubStatusUrl</code>).',
      JP: 'stub_status URLが未設定です（<code>NGINX_STUB_STATUS_URL</code>または<code>app.serverManage.nginxStubStatusUrl</code>）。',
      CH: '未设置 stub_status URL（<code>NGINX_STUB_STATUS_URL</code> 或 <code>app.serverManage.nginxStubStatusUrl</code>）。',
      TH: 'ยังไม่ตั้ง stub_status URL (<code>NGINX_STUB_STATUS_URL</code> หรือ <code>app.serverManage.nginxStubStatusUrl</code>)'
    },
    'Nginx stub': {
      EN: 'Nginx stub',
      JP: 'Nginx stub',
      CH: 'Nginx stub',
      TH: 'Nginx stub'
    },
    'stub_status 연동 시 활성 접속 등을 표시합니다.': {
      EN: 'When stub_status is wired, shows active connections, etc.',
      JP: 'stub_status連携時にアクティブ接続などを表示します。',
      CH: '接入 stub_status 后显示活跃连接等。',
      TH: 'เมื่อเชื่อม stub_status จะแสดงการเชื่อมติดที่ใช้งาน ฯลฯ'
    },
    '미저장 · 앱 수집 추정': {
      EN: 'Not saved · app estimate',
      JP: '未保存・アプリ集計推定',
      CH: '未保存 · 应用采集估算',
      TH: 'ยังไม่บันทึก · ประมาณจากแอป'
    },
    '미입력': {
      EN: 'Not entered',
      JP: '未入力',
      CH: '未填写',
      TH: 'ยังไม่กรอก'
    },
    '상단 <strong>호스팅 약정</strong> 폼에서 저장한 값입니다. 표시는 GB이며 서버에는 MB로 저장됩니다.': {
      EN: 'Values saved from the <strong>Hosting contract</strong> form above. Display is GB; stored as MB on the server.',
      JP: '上の<strong>ホスティング契約</strong>フォームから保存した値です。表示はGB、サーバーにはMBで保存されます。',
      CH: '来自上方<strong>主机托管合约</strong>表单的已保存值。界面为 GB，服务器以 MB 存储。',
      TH: 'ค่าที่บันทึกจากฟอร์ม<strong>สัญญาโฮสติ้ง</strong>ด้านบน แสดงเป็น GB บันทึกเป็น MB บนเซิร์ฟเวอร์'
    },
    '디스크 약정:': { EN: 'Disk contract:', JP: 'ディスク契約:', CH: '磁盘合约：', TH: 'สัญญาดิสก์:' },
    '트래픽 약정:': { EN: 'Traffic contract:', JP: 'トラフィック契約:', CH: '流量合约：', TH: 'สัญญาทราฟฟิก:' },
    '기간당': { EN: 'per period', JP: '期間あたり', CH: '每周期', TH: 'ต่อรอบ' },
    '트래픽 누적 입력:': {
      EN: 'Cumulative traffic input:',
      JP: 'トラフィック累積入力:',
      CH: '累计流量输入：',
      TH: 'การกรอกทราฟฟิกสะสม:'
    },
    '약정기간:': {
      EN: 'Contract period:',
      JP: '契約期間:',
      CH: '合约期间：',
      TH: 'ระยะสัญญา:'
    },
    '일반 항목은 NOTI와 동일한 비율 임계치입니다. <strong>약정 디스크·트래픽</strong> 행은 약정(GB) 대비 사용률(주의 ≥75%, 위험 ≥90%)입니다.': {
      EN: 'General rows use the same ratio thresholds as NOTI. <strong>Contract disk / traffic</strong> rows use usage vs contract (GB): warning ≥75%, critical ≥90%.',
      JP: '一般項目はNOTIと同じ比率しきい値です。<strong>契約ディスク・トラフィック</strong>行は契約（GB）に対する使用率（注意≥75%、危険≥90%）です。',
      CH: '常规项与 NOTI 相同的比例阈值。<strong>合约磁盘/流量</strong>行按合约（GB）的使用率：注意≥75%，危险≥90%。',
      TH: 'แถวทั่วไปใช้เกณฑ์เหมือน NOTI แถว<strong>ดิสก์/ทราฟฟิกตามสัญญา</strong>ใช้ % เทียบสัญญา (GB) เตือน ≥75% วิกฤต ≥90%'
    },
    '헬스 요약': {
      EN: 'Health summary',
      JP: 'ヘルスサマリ',
      CH: '健康摘要',
      TH: 'สรุปสุขภาพระบบ'
    },
    '항목': { EN: 'Item', JP: '項目', CH: '项', TH: 'รายการ' },
    '양호·주의·위험 기준': {
      EN: 'OK / warn / critical criteria',
      JP: '良好・注意・危険の基準',
      CH: '正常/注意/危险标准',
      TH: 'เกณฑ์ OK/เตือน/วิกฤต'
    },
    '값': { EN: 'Value', JP: '値', CH: '值', TH: 'ค่า' },
    '종합:': {
      EN: 'Overall:',
      JP: '総合:',
      CH: '综合：',
      TH: 'รวม:'
    },
    '자동 갱신 꺼짐 · [요약 새로고침]으로 수동 조회': {
      EN: 'Auto refresh off · use [Refresh summary] to load manually',
      JP: '自動更新オフ・［要約を再読込］で手動取得',
      CH: '自动刷新已关闭 · 使用[刷新摘要]手动加载',
      TH: 'ปิดรีเฟรชอัตโนมัติ · ใช้ [รีเฟรชสรุป] โหลดเอง'
    },
    '다음 자동 갱신까지 약 {0}분 {1}초': {
      EN: 'Next auto refresh in about {0} min {1} s',
      JP: '次回自動更新まで約{0}分{1}秒',
      CH: '约 {0} 分 {1} 秒后自动刷新',
      TH: 'รีเฟรชอัตโนมัติประมาณอีก {0} นาที {1} วินาที'
    },
    '다음 자동 갱신까지 약 {0}초': {
      EN: 'Next auto refresh in about {0} s',
      JP: '次回自動更新まで約{0}秒',
      CH: '约 {0} 秒后自动刷新',
      TH: 'รีเฟรชอัตโนมัติประมาณอีก {0} วินาที'
    },
    '조회 실패 (ADMIN 권한·네트워크 확인)': {
      EN: 'Load failed (check ADMIN role and network)',
      JP: '取得失敗（ADMIN権限・ネットワークを確認）',
      CH: '加载失败（请检查 ADMIN 权限与网络）',
      TH: 'โหลดล้มเหลว (ตรวจสิทธิ์ ADMIN และเครือข่าย)'
    },
    '조회 실패 — [요약 새로고침]을 눌러 주세요': {
      EN: 'Load failed — click [Refresh summary]',
      JP: '取得失敗 — ［要約を再読込］を押してください',
      CH: '加载失败 — 请点击[刷新摘要]',
      TH: 'โหลดล้มเหลว — กด [รีเฟรชสรุป]'
    },
    '서버운영관리 설정(SSL·호스팅 약정·갱신 간격)이 저장되었습니다. 대시보드가 갱신되었습니다.': {
      EN: 'Server operations settings (SSL, hosting contract, refresh interval) saved. Dashboard refreshed.',
      JP: 'サーバー運用設定（SSL・ホスティング契約・更新間隔）を保存しました。ダッシュボードを更新しました。',
      CH: '已保存服务器运维设置（SSL、主机合约、刷新间隔）。仪表盘已更新。',
      TH: 'บันทึกการตั้งค่าเซิร์ฟเวอร์ (SSL, สัญญาโฮสติ้ง, ช่วงรีเฟรช) แล้ว รีเฟรชแดชบอร์ดแล้ว'
    },

    /* HQ serverManage — API health rows/alerts/sslOps (HqServerManageService + app.js) */
    'hqSrv.health.lbl.sysMem': { KO: '시스템 메모리', EN: 'System memory', JP: 'システムメモリ', CH: '系统内存', TH: 'หน่วยความจำระบบ' },
    'hqSrv.health.lbl.jvmHeap': { KO: 'JVM 힙', EN: 'JVM heap', JP: 'JVMヒープ', CH: 'JVM 堆', TH: 'ฮีป JVM' },
    'hqSrv.health.lbl.loadAvg': { KO: 'Load average (1m)', EN: 'Load average (1m)', JP: 'ロードアベレージ（1分）', CH: '1 分钟平均负载', TH: 'โหลดเฉลี่ย (1 นาที)' },
    'hqSrv.health.lbl.diskAppPath': { KO: '디스크 (앱 기준 경로)', EN: 'Disk (app path)', JP: 'ディスク（アプリ基準パス）', CH: '磁盘（应用路径基准）', TH: 'ดิสก์ (พาธแอป)' },
    'hqSrv.health.lbl.sslCert': { KO: 'SSL 인증서', EN: 'SSL certificate', JP: 'SSL証明書', CH: 'SSL 证书', TH: 'ใบรับรอง SSL' },
    'hqSrv.health.lbl.contractDisk': { KO: '약정 디스크', EN: 'Contract disk', JP: '契約ディスク', CH: '约定磁盘', TH: 'ดิสก์ตามสัญญา' },
    'hqSrv.health.lbl.contractTraffic': { KO: '약정 트래픽', EN: 'Contract traffic', JP: '契約トラフィック', CH: '约定流量', TH: 'ทราฟฟิกตามสัญญา' },
    'hqSrv.health.lbl.dbTables': { KO: 'DB 테이블 수', EN: 'DB table count', JP: 'DBテーブル数', CH: '数据库表数量', TH: 'จำนวนตาราง DB' },
    'hqSrv.health.criteria.sysMem': {
      KO: '주의: 사용률 {0}% 이상 · 위험: {1}% 이상 (그 미만은 양호, RAM)',
      EN: 'Warn: usage ≥{0}% · Danger: ≥{1}% (below is OK, RAM)',
      JP: '注意：使用率{0}%以上・危険：{1}%以上（未満は良好、RAM）',
      CH: '注意：使用率≥{0}% · 危险：≥{1}%（低于为正常，RAM）',
      TH: 'เตือน: ใช้ ≥{0}% · วิกฤต: ≥{1}% (ต่ำกว่านี้โอเค RAM)'
    },
    'hqSrv.health.criteria.jvmHeap': {
      KO: '주의: 사용률 {0}% 이상 · 위험: {1}% 이상 (최대힙 ≥{2}MB일 때만 위험 판정)',
      EN: 'Warn: usage ≥{0}% · Danger: ≥{1}% (danger only when max heap ≥{2} MB)',
      JP: '注意：使用率{0}%以上・危険：{1}%以上（最大ヒープ≥{2}MBのときのみ危険）',
      CH: '注意：使用率≥{0}% · 危险：≥{1}%（仅当最大堆≥{2} MB 时判为危险）',
      TH: 'เตือน: ใช้ ≥{0}% · วิกฤต: ≥{1}% (อันตรายเมื่อ heap สูงสุด ≥{2} MB)'
    },
    'hqSrv.health.criteria.loadAvg': {
      KO: '주의: 1분 평균 > CPU 코어({0}) · 위험: > 코어×{1} ({2}) · 없음/N/A는 양호',
      EN: 'Warn: 1m avg > CPU cores ({0}) · Danger: > cores×{1} ({2}) · none/N/A is OK',
      JP: '注意：1分平均＞CPUコア({0})・危険：＞コア×{1}({2})・なし/N/Aは良好',
      CH: '注意：1 分钟均值 > CPU 核数（{0}）· 危险：> 核×{1}（{2}）· 无/N/A 为正常',
      TH: 'เตือน: เฉลี่ย 1 นาที > คอร์ ({0}) · วิกฤต: > คอร์×{1} ({2}) · ไม่มี/N/A โอเค'
    },
    'hqSrv.health.criteria.disk': {
      KO: '주의: 사용률 {0}% 이상 · 위험: {1}% 이상 (그 미만은 양호)',
      EN: 'Warn: usage ≥{0}% · Danger: ≥{1}% (below is OK)',
      JP: '注意：使用率{0}%以上・危険：{1}%以上（未満は良好）',
      CH: '注意：使用率≥{0}% · 危险：≥{1}%（低于为正常）',
      TH: 'เตือน: ใช้ ≥{0}% · วิกฤต: ≥{1}%'
    },
    'hqSrv.health.criteria.ssl': {
      KO: '주의: 만료 잔여 {0}일 미만 · 위험: {1}일 미만',
      EN: 'Warn: days left < {0} · Danger: < {1} days',
      JP: '注意：残り日数{0}日未満・危険：{1}日未満',
      CH: '注意：剩余天数 < {0} · 危险：< {1} 天',
      TH: 'เตือน: เหลือ < {0} วัน · วิกฤต: < {1} วัน'
    },
    'hqSrv.health.criteria.contractDisk': {
      KO: '앱 경로 디스크 사용량 ÷ 약정 {0} · 주의: {1}% 이상 · 위험: {2}% 이상{3}',
      EN: 'App path disk ÷ quota {0} · Warn ≥{1}% · Danger ≥{2}%{3}',
      JP: 'アプリパス使用量÷契約{0}・注意≥{1}%・危険≥{2}%{3}',
      CH: '应用路径用量÷合约 {0} · 注意≥{1}% · 危险≥{2}%{3}',
      TH: 'ดิสก์พาธแอป÷โควตา {0} · เตือน ≥{1}% · วิกฤต ≥{2}%{3}'
    },
    'hqSrv.health.criteria.contractTraffic': {
      KO: '약정 {0} 대비 사용률 · 주의 ≥{1}% · 위험 ≥{2}%{3} (저장값 또는 앱 수집 합산)',
      EN: 'Usage vs quota {0} · Warn ≥{1}% · Danger ≥{2}%{3} (saved total or app aggregate)',
      JP: '契約{0}に対する使用率・注意≥{1}%・危険≥{2}%{3}（保存値またはアプリ集計）',
      CH: '相对配额 {0} 的使用率 · 注意≥{1}% · 危险≥{2}%{3}（已保存或应用汇总）',
      TH: 'เทียบโควตา {0} · เตือน ≥{1}% · วิกฤต ≥{2}%{3} (ยอดที่บันทึกหรือรวมจากแอป)'
    },
    'hqSrv.health.criteria.dbTables': {
      KO: 'JDBC 연결 기준 public 스키마 BASE TABLE 개수',
      EN: 'Count of BASE TABLE in public schema (JDBC connection)',
      JP: 'JDBC接続のpublicスキーマ内BASE TABLE件数',
      CH: 'JDBC 所连 public 架构中 BASE TABLE 数量',
      TH: 'จำนวน BASE TABLE ใน public schema (JDBC)'
    },
    'hqSrv.health.value.sysMem': {
      KO: '{0} (가용 {1} / 총 {2} MB)',
      EN: '{0} (available {1} / total {2} MB)',
      JP: '{0}（空き{1}/合計{2}MB）',
      CH: '{0}（可用 {1} / 共 {2} MB）',
      TH: '{0} (ว่าง {1} / รวม {2} MB)'
    },
    'hqSrv.health.value.jvmHeap': {
      KO: '{0} / {1} MB ({2}%)',
      EN: '{0} / {1} MB ({2}%)',
      JP: '{0}/{1}MB（{2}%）',
      CH: '{0} / {1} MB（{2}%）',
      TH: '{0} / {1} MB ({2}%)'
    },
    'hqSrv.health.value.loadAvg': { KO: '{0}', EN: '{0}', JP: '{0}', CH: '{0}', TH: '{0}' },
    'hqSrv.health.value.diskPct': { KO: '{0}', EN: '{0}', JP: '{0}', CH: '{0}', TH: '{0}' },
    'hqSrv.health.value.sslDays': {
      KO: '만료까지 약 {0}일',
      EN: '~{0} days until expiry',
      JP: '有効期限まで約{0}日',
      CH: '距过期约 {0} 天',
      TH: 'หมดอายุในอีก ~{0} วัน'
    },
    'hqSrv.health.value.sslNa': {
      KO: '인증서 없음/미설정',
      EN: 'No certificate / not configured',
      JP: '証明書なし/未設定',
      CH: '无证书/未配置',
      TH: 'ไม่มีใบรับรอง/ยังไม่ตั้งค่า'
    },
    'hqSrv.health.value.sslErr': { KO: '{0}', EN: '{0}', JP: '{0}', CH: '{0}', TH: '{0}' },
    'hqSrv.health.value.contractDisk': {
      KO: '약정 {0} 중 약 {1} 사용 ({2}%)',
      EN: '~{1} of {0} used ({2}%)',
      JP: '契約{0}のうち約{1}使用（{2}%）',
      CH: '合约 {0} 中约用 {1}（{2}%）',
      TH: 'ใช้ประมาณ {1} จาก {0} ({2}%)'
    },
    'hqSrv.health.value.contractTraffic': {
      KO: '{0} / {1} ({2}%)',
      EN: '{0} / {1} ({2}%)',
      JP: '{0}/{1}（{2}%）',
      CH: '{0} / {1}（{2}%）',
      TH: '{0} / {1} ({2}%)'
    },
    'hqSrv.health.value.contractTrafficSuggested': {
      KO: 'DB 미저장 — 앱 수집(약정기간 내 일별 합) 약 {0} · 호스팅 패널과 다를 수 있음. 폼에 반영된 뒤 [저장]하면 비율 판정에 쓰입니다.',
      EN: 'Not saved in DB — app estimate (sum of daily in contract window) ~{0}. May differ from hosting panel; save the form to use it for ratio checks.',
      JP: 'DB未保存—アプリ推定（契約期間内の日次合計）約{0}。パネルと異なる場合があります。[保存]で比率判定に反映。',
      CH: '未入库 — 应用估算（合约期内日流量合计）约 {0}。可能与面板不一致；保存表单后用于比例判定。',
      TH: 'ยังไม่บันทึก DB — ประมาณจากแอป (รวมรายวันในช่วงสัญญา) ~{0} อาจไม่ตรงแพเนล บันทึกฟอร์มเพื่อใช้คำนวณสัดส่วน'
    },
    'hqSrv.health.value.contractTrafficEmpty': {
      KO: 'DB 미저장 — 호스팅 패널 누적(GB)을 입력하거나, 일별 수집이 쌓이면 추정이 표시됩니다.',
      EN: 'Not saved in DB — enter cumulative GB from the hosting panel, or wait for daily collection to show an estimate.',
      JP: 'DB未保存—パネルの累積(GB)を入力するか、日次収集が溜まると推定が表示されます。',
      CH: '未入库 — 请输入主机面板累计(GB)，或待日采集积累后显示估算。',
      TH: 'ยังไม่บันทึก DB — กรอกยอดสะสม GB จากแพเนล หรือรอเก็บรายวันเพื่อประมาณ'
    },
    'hqSrv.health.value.dbTables': { KO: '{0}', EN: '{0}', JP: '{0}', CH: '{0}', TH: '{0}' },
    'hqSrv.health.value.dbFail': { KO: '{0}', EN: '{0}', JP: '{0}', CH: '{0}', TH: '{0}' },
    'hqSrv.health.value.dash': { KO: '—', EN: '—', JP: '—', CH: '—', TH: '—' },
    'hqSrv.alert.hostingContractExpired': {
      KO: '호스팅 약정 종료일이 지났습니다. ({0})',
      EN: 'Hosting contract end date has passed ({0}).',
      JP: 'ホスティング契約の終了日を過ぎています（{0}）。',
      CH: '主机合约结束日已过（{0}）。',
      TH: 'วันสิ้นสัญญาโฮสติ้งเลยแล้ว ({0})'
    },
    'hqSrv.alert.hostingContractEndingSoon': {
      KO: '호스팅 약정 종료 {0}일 전입니다.',
      EN: 'Hosting contract ends in {0} day(s).',
      JP: 'ホスティング契約終了まであと{0}日です。',
      CH: '距离主机合约结束还有 {0} 天。',
      TH: 'สัญญาโฮสติ้งเหลืออีก {0} วัน'
    },
    'hqSrv.alert.systemMemoryHigh': {
      KO: '시스템 메모리 사용률이 {0}% 이상입니다.',
      EN: 'System memory usage is at or above {0}%.',
      JP: 'システムメモリ使用率が{0}%以上です。',
      CH: '系统内存使用率已达 {0}% 或以上。',
      TH: 'การใช้หน่วยความจำระบบ ≥ {0}%'
    },
    'hqSrv.alert.jvmHeapHigh': {
      KO: 'JVM 힙 사용률이 {0}% 이상입니다.',
      EN: 'JVM heap usage is at or above {0}%.',
      JP: 'JVMヒープ使用率が{0}%以上です。',
      CH: 'JVM 堆使用率已达 {0}% 或以上。',
      TH: 'การใช้ฮีป JVM ≥ {0}%'
    },
    'hqSrv.alert.loadAverageHigh': {
      KO: '시스템 부하(1분 평균)가 CPU 코어 수의 {0}배를 넘었습니다.',
      EN: '1-minute load average exceeds {0}× the CPU core count.',
      JP: '1分負荷平均がCPUコア数の{0}倍を超えています。',
      CH: '1 分钟平均负载超过 CPU 核数的 {0} 倍。',
      TH: 'โหลดเฉลี่ย 1 นาทีเกิน {0} เท่าของจำนวนคอร์'
    },
    'hqSrv.alert.diskUsageHigh': {
      KO: '디스크 사용률이 {0}% 이상입니다.',
      EN: 'Disk usage is at or above {0}%.',
      JP: 'ディスク使用率が{0}%以上です。',
      CH: '磁盘使用率已达 {0}% 或以上。',
      TH: 'การใช้ดิสก์ ≥ {0}%'
    },
    'hqSrv.alert.contractDiskHigh': {
      KO: '약정 디스크({0}) 대비 사용률이 {1}% 이상입니다.',
      EN: 'Disk usage vs contract ({0}) is at or above {1}%.',
      JP: '契約ディスク（{0}）に対する使用率が{1}%以上です。',
      CH: '相对约定磁盘（{0}）的使用率已达 {1}% 或以上。',
      TH: 'การใช้ดิสก์เทียบสัญญา ({0}) ≥ {1}%'
    },
    'hqSrv.alert.contractTrafficHigh': {
      KO: '약정 트래픽({0}) 대비 사용이 {1}% 이상입니다.',
      EN: 'Traffic vs contract ({0}) is at or above {1}%.',
      JP: '契約トラフィック（{0}）に対する使用が{1}%以上です。',
      CH: '相对约定流量（{0}）的使用已达 {1}% 或以上。',
      TH: 'ทราฟฟิกเทียบสัญญา ({0}) ≥ {1}%'
    },
    'hqSrv.alert.sslExpiresCritical': {
      KO: 'SSL 인증서 만료가 {0}일 이내입니다.',
      EN: 'SSL certificate expires within {0} day(s).',
      JP: 'SSL証明書の有効期限が{0}日以内です。',
      CH: 'SSL 证书将在 {0} 天内过期。',
      TH: 'ใบรับรอง SSL หมดอายุภายใน {0} วัน'
    },
    'hqSrv.alert.sslExpiresSoon': {
      KO: 'SSL 인증서 만료가 {0}일 이내입니다.',
      EN: 'SSL certificate expires within {0} day(s).',
      JP: 'SSL証明書の有効期限が{0}日以内です。',
      CH: 'SSL 证书将在 {0} 天内过期。',
      TH: 'ใบรับรอง SSL หมดอายุภายใน {0} วัน'
    },
    'hqSrv.alert.sslReadFailed': {
      KO: 'SSL 인증서를 읽지 못했습니다.',
      EN: 'Could not read the SSL certificate.',
      JP: 'SSL証明書を読み取れませんでした。',
      CH: '无法读取 SSL 证书。',
      TH: 'อ่านใบรับรอง SSL ไม่ได้'
    },
    'hqSrv.alert.dbMetaFailed': {
      KO: 'DB 메타 조회 실패: {0}',
      EN: 'DB metadata query failed: {0}',
      JP: 'DBメタデータ取得に失敗しました: {0}',
      CH: '数据库元数据查询失败：{0}',
      TH: 'สอบถามเมตาดาต้า DB ล้มเหลว: {0}'
    },
    'hqSrv.sslOps.dns': {
      KO: '권한 네임서버(예: 카페24)에 서브도메인별 A 레코드가 VPS 공인 IP를 가리키는지 확인하세요. 일부 ISP DNS 캐시로 예전(프록시) IP가 남을 수 있어, 접속 PC에서 8.8.8.8 등으로 조회해 비교할 수 있습니다.',
      EN: 'Check that authoritative DNS (e.g. Cafe24) A records for each subdomain point to the VPS public IP. Some ISP DNS caches may keep an old (proxy) IP—compare using 8.8.8.8 from your PC.',
      JP: '権威DNS（例: カフェ24）で各サブドメインのAレコードがVPSのグローバルIPを指しているか確認してください。一部ISPのDNSキャッシュに古い(プロキシ)IPが残ることがあるため、PCから8.8.8.8等で照会して比較できます。',
      CH: '请确认权威 DNS（如 Cafe24）上各子域名的 A 记录指向 VPS 公网 IP。部分 ISP DNS 缓存可能保留旧（代理）IP，可在本机用 8.8.8.8 等对比查询。',
      TH: 'ตรวจ DNS หลัก (เช่น Cafe24) ว่า A ของแต่ละซับโดเมนชี้ IP สาธารณะของ VPS แคช ISP อาจค้าง IP เก่า ลองเทียบด้วย 8.8.8.8 จากเครื่องคุณ'
    },
    'hqSrv.sslOps.leSan': {
      KO: 'Let\u2019s Encrypt는 한 장의 인증서(SAN)에 여러 호스트명을 넣을 수 있습니다. 서브도메인을 추가하면 certbot --nginx -d … 로 재발급하고, Nginx에 해당 server_name 과 동일 ssl_certificate 경로를 맞춥니다.',
      EN: 'Let\u2019s Encrypt can put multiple hostnames on one certificate (SAN). When adding a subdomain, re-issue with certbot --nginx -d … and align ssl_certificate in Nginx with that server_name.',
      JP: 'Let\u2019s Encryptは1枚の証明書(SAN)に複数ホスト名を載せられます。サブドメイン追加時は certbot --nginx -d … で再発行し、Nginxのserver_nameに合わせてssl_certificateを揃えます。',
      CH: 'Let\u2019s Encrypt 可将多个主机名放在一张证书（SAN）上。新增子域名后用 certbot --nginx -d … 重新签发，并使 Nginx 的 ssl_certificate 与该 server_name 一致。',
      TH: 'Let\u2019s Encrypt ใส่หลายโฮสต์ใน SAN ได้ เพิ่มซับโดเมนแล้วออกใหม่ด้วย certbot --nginx -d … ให้ ssl_certificate ตรง server_name'
    },
    'hqSrv.sslOps.cloudflare': {
      KO: 'Cloudflare 프록시(주황 구름)를 쓰는 동안에는 원본 인증서 검증(Full strict) 오류(526 등)가 날 수 있습니다. DNS 전용(회색 구름)이거나 카페24 직접 A 레코드로 통일하는 편이 단순합니다.',
      EN: 'With Cloudflare proxy (orange cloud) on, origin cert checks (Full strict) may fail (526, etc.). DNS-only (grey cloud) or direct Cafe24 A records are simpler.',
      JP: 'Cloudflareプロキシ（オレンジ雲）有効中はオリジン証明書検証（Full strict）でエラー(526等)が出ることがあります。DNSのみ（グレー雲）か、カフェ24直Aの方が単純です。',
      CH: '开启 Cloudflare 代理（橙色云）时，源站证书校验（Full strict）可能报错（526 等）。仅用 DNS（灰色云）或 Cafe24 直连 A 记录更简单。',
      TH: 'พร็อกซี Cloudflare (เมฆส้ม) อาจทำให้ Full strict ล้ม (526 ฯลฯ) ใช้ DNS only (เมฆเทา) หรือ A ตรงง่ายกว่า'
    },

    /* 통합 리포트·일별통합/결제/수수료 — 그리드·엑셀·상세 */
    '일자': { EN: 'Date', JP: '日付', CH: '日期', TH: 'วันที่' },
    '총결제액': { EN: 'Total payment', JP: '総決済額', CH: '总支付额', TH: 'ยอดชำระรวม' },
    '총보증금': { EN: 'Total deposit', JP: '総保証金', CH: '总保证金', TH: 'เงินประกันรวม' },
    '총거래건수': { EN: 'Total transactions', JP: '総取引件数', CH: '总交易笔数', TH: 'จำนวนธุรกรรมทั้งหมด' },
    '통합리포트': { EN: 'Integrated report', JP: '統合レポート', CH: '综合报表', TH: 'รายงานรวม' },
    '통합 리포트': { EN: 'Integrated report', JP: '統合レポート', CH: '综合报表', TH: 'รายงานรวม' },
    '보증금액': { EN: 'Deposit amount', JP: '保証金額', CH: '保证金金额', TH: 'ยอดเงินประกัน' },
    '결제주기': { EN: 'Pay cycle', JP: '決済サイクル', CH: '支付周期', TH: 'รอบการชำระ' },
    '금액': { EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' },
    '건수': { EN: 'Count', JP: '件数', CH: '笔数', TH: 'จำนวน' },
    '수수료(변동·% / 건당)': { EN: 'Fee (var.% / per txn)', JP: '手数料（変動%／件）', CH: '手续费（浮动%/笔）', TH: 'ค่าธรรมเนียม (% แปรผัน / รายการ)' },
    '총건수': { EN: 'Total count', JP: '総件数', CH: '总笔数', TH: 'จำนวนรวม' },
    '성공건수': { EN: 'Success count', JP: '成功件数', CH: '成功笔数', TH: 'จำนวนสำเร็จ' },
    '총금액': { EN: 'Total amount', JP: '総金額', CH: '总金额', TH: 'ยอดรวม' },
    '통화(총금액)': { EN: 'Currency (total amt)', JP: '通貨（総金額）', CH: '币种（总金额）', TH: 'สกุล (ยอดรวม)' },
    '예상지급액(통화별)': { EN: 'Expected payout (by ccy)', JP: '見込み支払（通貨別）', CH: '预计拨付（按币种）', TH: 'ยอดจ่ายโดยประมาณ (ตามสกุล)' },
    '취소': { EN: 'Cancel', JP: '取消', CH: '取消', TH: 'ยกเลิก' },
    '환불': { EN: 'Refund', JP: '返金', CH: '退款', TH: 'คืนเงิน' },
    '데이터 없음': { EN: 'No data', JP: 'データがありません', CH: '无数据', TH: 'ไม่มีข้อมูล' },
    '불러오는 중…': { EN: 'Loading…', JP: '読み込み中…', CH: '加载中…', TH: 'กำลังโหลด…' },
    '선택 일자 상세': { EN: 'Selected date detail', JP: '選択日の詳細', CH: '所选日期明细', TH: 'รายละเอียดวันที่เลือก' },
    '선택 일자 상세 (통합 결제내역)': { EN: 'Selected date — integrated payments', JP: '選択日の詳細（統合決済）', CH: '所选日期明细（综合支付）', TH: 'รายละเอียดวันที่เลือก (ชำระรวม)' },
    '엑셀리스트다운': { EN: 'Excel list download', JP: 'Excelリストダウン', CH: 'Excel列表下载', TH: 'ดาวน์โหลด Excel รายการ' },
    '위 일자별 표에서 일자를 클릭하여 상세를 불러온 뒤 [엑셀리스트다운]을 다시 눌러 주세요.': {
      EN: 'Click a date row in the summary table to load details, then click [Excel list download] again.',
      JP: '上の日別表で日付をクリックして詳細を読み込んだ後、[Excelリストダウン]を再度押してください。',
      CH: '请在上方日别表中点击日期加载明细后，再点击[Excel列表下载]。',
      TH: 'คลิกแถววันที่ในตารางสรุปเพื่อโหลดรายละเอียด แล้วกด [ดาวน์โหลด Excel รายการ] อีกครั้ง'
    },
    '위 일자별 표에서 일자를 더블클릭하여 상세를 불러온 뒤 [엑셀리스트다운]을 다시 눌러 주세요.': {
      EN: 'Double-click a date row in the summary table to load details, then click [Excel list download] again.',
      JP: '上の日別表で日付をダブルクリックして詳細を読み込んだ後、[Excelリストダウン]を再度押してください。',
      CH: '请在上方日别表中双击日期加载明细后，再点击[Excel列表下载]。',
      TH: 'ดับเบิลคลิกแถววันที่ในตารางสรุปเพื่อโหลดรายละเอียด แล้วกด [ดาวน์โหลด Excel รายการ] อีกครั้ง'
    },
    '위에서 일자를 클릭하세요.': { EN: 'Click a date row above.', JP: '上の日付行をクリックしてください。', CH: '请点击上方的日期行。', TH: 'คลิกแถววันที่ด้านบน' },
    '위에서 일자를 더블클릭하세요.': {
      EN: 'Double-click a date row above.',
      JP: '上の日付行をダブルクリックしてください。',
      CH: '请双击上方的日期行。',
      TH: 'ดับเบิลคลิกแถววันที่ด้านบน'
    },
    '검증 리포트': { EN: 'Verify report', JP: '検証レポート', CH: '验证报表', TH: 'รายงานตรวจสอบ' },
    '비활성카드등록': { EN: 'Inactive card register', JP: '非活性カード登録', CH: '非活跃卡登记', TH: 'ลงทะเบียนบัตรปิดใช้' },
    '대행거래내역': { EN: 'Agency transaction list', JP: '代行取引明細', CH: '代行交易明细', TH: 'รายการธุรกรรมตัวแทน' },
    'PG정산유무': { EN: 'PG settlement due', JP: 'PG精算可否', CH: 'PG 是否应结算', TH: 'ครบกำหนด PG' },
    'PG명': { EN: 'PG name', JP: 'PG名', CH: 'PG 名称', TH: 'ชื่อ PG' },
    'PG코드': { EN: 'PG code', JP: 'PGコード', CH: 'PG 代码', TH: 'รหัส PG' },
    '대행수수료합': { EN: 'Agency fee total', JP: '代行手数料合計', CH: '代行手续费合计', TH: 'รวมค่าธรรมเนียมตัวแทน' },
    'TAX 리포트': { EN: 'TAX report', JP: 'TAXレポート', CH: 'TAX 报表', TH: 'รายงาน TAX' },
    '메일로그': { EN: 'Mail log', JP: 'メールログ', CH: '邮件日志', TH: 'บันทึกเมล' },
    'API배포문서': { EN: 'API deployment docs', JP: 'API配布ドキュメント', CH: 'API 部署文档', TH: 'เอกสาร API สำหรับร้าน' },
    '가맹점API': { EN: 'Merchant API', JP: '加盟店API', CH: '商户 API', TH: 'API ร้านค้า' },
    'API 가맹점 등록': { EN: 'API merchant registration', JP: 'API加盟店登録', CH: 'API 商户注册', TH: 'ลงทะเบียนร้าน API' },
    '가맹점 API 생성': { EN: 'Merchant API (generate)', JP: '加盟店API生成', CH: '生成商户 API', TH: 'สร้าง Merchant API' },
    'API배포설정': { EN: 'API deploy settings', JP: 'API配信設定', CH: 'API 部署设置', TH: 'ตั้งค่า deploy API' },
    'JPAY 단계 계획': { EN: 'JPAY rollout plan', JP: 'JPAY段階計画', CH: 'JPAY 阶段计划', TH: 'แผน JPAY' },
    '가맹점 API 배포': { EN: 'Merchant API rollout', JP: '加盟店API配布', CH: '商户 API 发布', TH: 'นโยบาย Merchant API' },
    '배포 체크리스트': { EN: 'Launch checklist', JP: '配布チェックリスト', CH: '上线检查清单', TH: 'เช็กลิสต์เปิดใช้' },
    '본사설정': { EN: 'HQ settings', JP: '本社設定', CH: '总部设置', TH: 'ส่วนตั้งค่า HQ' },
    '배포설정': { EN: 'Deployment', JP: 'デプロイ設定', CH: '部署设置', TH: 'การใช้งานจริง' },
    '통보관리': { EN: 'Notifications', JP: '通知管理', CH: '通知管理', TH: 'การแจ้งเตือน' },
    '리스크관리': { EN: 'Risk', JP: 'リスク管理', CH: '风险管理', TH: 'ความเสี่ยง' },
    '리스크 현황': { EN: 'Risk dashboard', JP: 'リスク状況', CH: '风险看板', TH: 'ภาพรวมความเสี่ยง' },
    '비활성 카드 등록': { EN: 'Register inactive card', JP: '非活性カード登録', CH: '登记非活跃卡', TH: 'ลงทะเบียนบัตรปิดใช้' },
    'OTP 해지': { EN: 'Release (OTP)', JP: 'OTP解除', CH: 'OTP 解除', TH: 'ยกเลิก (OTP)' },
    '해지하려면 Google OTP 6자리를 입력하세요.': {
      EN: 'Enter your 6-digit Google OTP to release.',
      JP: '解除するには Google OTP 6桁を入力してください。',
      CH: '请输入 6 位 Google OTP 以解除。',
      TH: 'กรอก Google OTP 6 หลักเพื่อยกเลิก'
    },
    '등록되었습니다.': { EN: 'Registered.', JP: '登録しました。', CH: '已登记。', TH: 'ลงทะเบียนแล้ว' },
    '해지되었습니다.': { EN: 'Released.', JP: '解除しました。', CH: '已解除。', TH: 'ยกเลิกแล้ว' },
    '등록 실패': { EN: 'Register failed', JP: '登録失敗', CH: '登记失败', TH: 'ลงทะเบียนล้มเหลว' },
    '카드번호를 입력하세요.': { EN: 'Enter card number.', JP: 'カード番号を入力してください。', CH: '请输入卡号。', TH: 'กรอกเลขบัตร' },
    '카드번호·사유를 입력한 뒤 [등록]을 누르세요. 등록일시·등록자는 자동 저장됩니다.': {
      EN: 'Enter card number and reason, then click [Register]. Date/time and user are saved automatically.',
      JP: 'カード番号・理由を入力し[登録]を押してください。登録日時・登録者は自動保存されます。',
      CH: '输入卡号与原因后点击[登记]。登记时间与登记人自动保存。',
      TH: 'กรอกเลขบัตรและเหตุผล แล้วกด [ลงทะเบียน] วันที่และผู้ลงทะเบียนบันทึกอัตโนมัติ'
    },
    '본사권한설정에서 이 화면에 삭제(전체) 또는 수정 권한이 있어야 등록·해지할 수 있습니다.': {
      EN: 'Grant Delete (full) or Modify for this screen in HQ permissions to register or release.',
      JP: '本社権限でこの画面に削除(全体)または修正権限が必要です。',
      CH: '须在本社权限中为本画面授予删除(全部)或修改权限方可登记/解除。',
      TH: 'ต้องมีสิทธิ์ลบ(เต็ม)หรือแก้ไขสำหรับหน้านี้ในสิทธิ์สำนักงานใหญ่'
    },
    '등록·해지 권한이 없습니다. 본사권한설정에서 삭제(전체) 또는 수정 권한을 부여하세요.': {
      EN: 'No register/release permission. Grant Delete (full) or Modify in HQ permissions.',
      JP: '登録・解除権限がありません。本社権限で削除(全体)または修正を付与してください。',
      CH: '无登记/解除权限。请在本社权限中授予删除(全部)或修改。',
      TH: 'ไม่มีสิทธิ์ลงทะเบียน/ยกเลิก ให้สิทธิ์ลบ(เต็ม)หรือแก้ไขในสิทธิ์สำนักงานใหญ่'
    },
    '등록·해지는 본사권한설정에서 이 화면 권한을 삭제(전체) 또는 수정으로 부여한 계정만 가능합니다.': {
      EN: 'Only accounts with Delete (full) or Modify on this screen in HQ permissions can register or release.',
      JP: '本社権限でこの画面に削除(全体)または修正があるアカウントのみ登録・解除できます。',
      CH: '仅在本社权限中对该画面拥有删除(全部)或修改的账户可登记/解除。',
      TH: 'เฉพาะบัญชีที่มีสิทธิ์ลบ(เต็ม)หรือแก้ไขหน้านี้ในสิทธิ์สำนักงานใหญ่'
    },
    '해지 실패': { EN: 'Release failed', JP: '解除失敗', CH: '解除失败', TH: 'ยกเลิกล้มเหลว' },
    '카드번호(마스킹)': { EN: 'Card no. (masked)', JP: 'カード番号(マスク)', CH: '卡号(掩码)', TH: 'เลขบัตร (ปิดบางส่วน)' },
    '등록카드': { EN: 'Registered card', JP: '登録カード', CH: '登记卡', TH: 'บัตรที่ลงทะเบียน' },
    '해지됨': { EN: 'Released', JP: '解除済', CH: '已解除', TH: 'ยกเลิกแล้ว' },
    '총본사·본사·총판(ADMIN 포함) 운영자용입니다. 메뉴 접근은 본사권한설정에서 부여합니다.': {
      EN: 'For root HQ, regional HQ, master distributor (incl. ADMIN). Grant menu access in HQ permissions.',
      JP: '総本部・本社・総販(ADMIN含む)の運用者向けです。メニューアクセスは本社権限設定で付与します。',
      CH: '供总总部、本部、总代(含 ADMIN)使用。请在总部权限设置中授予菜单访问。',
      TH: 'สำหรับ HQ สูงสุด/ภูมิภาค/ตัวแทนหลัก (รวม ADMIN) ให้สิทธิ์เมนูในสิทธิ์สำนักงานใหญ่'
    },
    '카드번호는 15자리(AMEX)·16자리만 등록할 수 있습니다. 저장 시 마스킹 표시(원번호는 해시만 보관).': {
      EN: 'Only 15-digit (AMEX) or 16-digit card numbers. Stored masked (hash only in DB).',
      JP: 'カード番号は15桁(AMEX)・16桁のみ登録できます。保存時はマスク表示(原本はハッシュのみ保管)。',
      CH: '仅可登记 15 位(AMEX)或 16 位卡号。保存为掩码显示(仅存哈希)。',
      TH: 'ลงทะเบียนได้เฉพาะ 15 หลัก (AMEX) หรือ 16 หลัก แสดงแบบปิดบางส่วน (เก็บแฮชใน DB)'
    },
    '해지 시 Google OTP 6자리가 필요합니다.': {
      EN: 'Google OTP (6 digits) required to release.',
      JP: '解除時は Google OTP 6桁が必要です。',
      CH: '解除时需要 Google OTP 6 位。',
      TH: 'ยกเลิกต้องใช้ Google OTP 6 หลัก'
    },
    '사용불가카드 등록': { EN: 'Blocked card (BIN)', JP: '使用不可カード登録', CH: '不可用卡(BIN)', TH: 'ลงทะเบียนบัตรใช้ไม่ได้' },
    'PG별 카드번호 <strong>앞자리(BIN) 접두</strong>만 등록합니다. 입력 시 결제창·승인 API에서 즉시 차단됩니다. 개별 카드번호(비활성카드)는 <strong>운영관리 → 비활성카드등록</strong> 메뉴에서 관리합니다.': {
      EN: 'Register only <strong>card number prefixes (BIN)</strong> per PG. Blocked immediately at checkout and approval API. Manage individual card numbers (inactive cards) under <strong>Operations → Inactive card register</strong>.',
      JP: 'PGごとにカード番号の<strong>先頭(BIN)接頭</strong>のみ登録します。入力時に決済画面・承認APIで即時ブロックします。個別カード番号(非活性カード)は<strong>運用管理 → 非活性カード登録</strong>メニューで管理します。',
      CH: '仅按 PG 登记卡号<strong>前缀(BIN)</strong>。在支付页与批准 API 即时拦截。个别卡号(非活跃卡)请在<strong>运营管理 → 非活跃卡登记</strong>管理。',
      TH: 'ลงทะเบียนเฉพาะ<strong>คำนำหน้า BIN</strong> ต่อ PG บล็อกทันทีที่หน้าชำระและ API อนุมัติ บัตรรายใบ(ปิดใช้)จัดการที่<strong>ปฏิบัติการ → ลงทะเบียนบัตรปิดใช้</strong>'
    },
    '등록일시': { EN: 'Registered at', JP: '登録日時', CH: '登记时间', TH: 'วันที่ลงทะเบียน' },
    '등록자': { EN: 'Registered by', JP: '登録者', CH: '登记人', TH: 'ผู้ลงทะเบียน' },
    '15·16자리': {
      EN: '15–16 digits',
      JP: '15〜16桁',
      CH: '15–16位',
      TH: '15–16 หลัก'
    },
    '카드 종류': { EN: 'Card type', JP: 'カード種別', CH: '卡种', TH: 'ประเภทบัตร' },
    '카드 종류를 먼저 선택하세요.': {
      EN: 'Select a card type first.',
      JP: '先にカード種別を選択してください。',
      CH: '请先选择卡种。',
      TH: 'กรุณาเลือกประเภทบัตรก่อน'
    },
    '카드 종류를 선택한 뒤 카드번호·사유를 입력하고 [등록]을 누르세요. 등록일시·등록자는 자동 저장됩니다.': {
      EN: 'Select card type, enter card number and reason, then click Register. Date/time and user are saved automatically.',
      JP: 'カード種別を選び、カード番号・理由を入力して［登録］を押してください。登録日時・登録者は自動保存されます。',
      CH: '选择卡种后输入卡号与原因，点击［登记］。登记时间与登记人自动保存。',
      TH: 'เลือกประเภทบัตร กรอกหมายเลขและเหตุผล แล้วกดลงทะเบียน วันที่และผู้ลงทะเบียนบันทึกอัตโนมัติ'
    },
    '카드 종류(VISA·Mastercard 등)를 선택하면 브랜드에 맞는 입력칸이 열립니다. AMEX는 15자리(4-6-5), 그 외는 16자리(4칸×4자리)입니다. 저장 시 마스킹 표시(원번호는 해시만 보관).': {
      EN: 'Select card type (Visa, Mastercard, etc.) to show matching fields. AMEX: 15 digits (4-6-5); others: 16 digits (4×4). Stored masked (hash only in DB).',
      JP: 'カード種別(VISA・Mastercard等)を選ぶと入力欄が開きます。AMEXは15桁(4-6-5)、その他は16桁(4枠×4桁)。保存時はマスク表示(原本はハッシュのみ)。',
      CH: '选择卡种(Visa、Mastercard 等)后显示对应输入框。运通 15 位(4-6-5)，其他 16 位(4×4)。保存为掩码(仅存哈希)。',
      TH: 'เลือกประเภทบัตรแล้วช่องจะเปิดตามแบรนด์ AMEX 15 หลัก (4-6-5) อื่นๆ 16 หลัก (4×4) เก็บแบบปิดบางส่วน (แฮชใน DB)'
    },
    'AMEX 카드번호 15자리를 모두 입력하세요.': {
      EN: 'Enter all 15 digits of the AMEX card number.',
      JP: 'AMEXカード番号15桁をすべて入力してください。',
      CH: '请输入完整的 15 位运通卡号。',
      TH: 'กรุณากรอกหมายเลข AMEX ครบ 15 หลัก'
    },
    '카드번호 16자리를 모두 입력하세요.': {
      EN: 'Enter all 16 digits of the card number.',
      JP: 'カード番号16桁をすべて入力してください。',
      CH: '请输入完整的 16 位卡号。',
      TH: 'กรุณากรอกหมายเลขบัตรครบ 16 หลัก'
    },
    'Visa': { EN: 'Visa', JP: 'Visa', CH: 'Visa', TH: 'Visa' },
    'Mastercard': { EN: 'Mastercard', JP: 'Mastercard', CH: 'Mastercard', TH: 'Mastercard' },
    'UnionPay': { EN: 'UnionPay', JP: 'UnionPay', CH: '银联', TH: 'UnionPay' },
    'American Express': { EN: 'American Express', JP: 'American Express', CH: '美国运通', TH: 'American Express' },
    'Diners Club': { EN: 'Diners Club', JP: 'Diners Club', CH: '大来卡', TH: 'Diners Club' },
    'Discover': { EN: 'Discover', JP: 'Discover', CH: 'Discover', TH: 'Discover' },
    '국내 전용(9)': { EN: 'Domestic (9…)', JP: '国内専用(9)', CH: '国内专用(9)', TH: 'ในประเทศ (9)' },
    '기타': { EN: 'Other', JP: 'その他', CH: '其他', TH: 'อื่นๆ' },
    '선택한 카드 종류와 카드번호 형식(접두·자릿수)이 일치하지 않습니다.': {
      EN: 'Card type does not match the number format (prefix/length).',
      JP: '選択したカード種別とカード番号の形式(接頭・桁数)が一致しません。',
      CH: '所选卡种与卡号格式(前缀/位数)不一致。',
      TH: 'ประเภทบัตรไม่ตรงกับรูปแบบหมายเลข (คำนำหน้า/ความยาว)'
    },
    'Visa 카드번호는 13자리 또는 16자리입니다.': {
      EN: 'Visa card numbers are 13 or 16 digits.',
      JP: 'Visaカード番号は13桁または16桁です。',
      CH: 'Visa 卡号为 13 位或 16 位。',
      TH: 'Visa ต้องมี 13 หรือ 16 หลัก'
    },
    'Diners Club 카드번호 14자리를 모두 입력하세요.': {
      EN: 'Enter all 14 digits of the Diners Club card number.',
      JP: 'Diners Clubカード番号14桁をすべて入力してください。',
      CH: '请输入完整的 14 位大来卡号。',
      TH: 'กรอกหมายเลข Diners Club ครบ 14 หลัก'
    },
    'UnionPay 카드번호는 16자리입니다. (17~19자리는 기타를 선택하세요.)': {
      EN: 'UnionPay numbers are 16 digits here. For 17–19 digits, choose Other.',
      JP: '銀聯は16桁です。(17~19桁は「その他」を選択)',
      CH: '银联此处为 16 位。(17~19 位请选择「其他」)',
      TH: 'UnionPay 16 หลักที่นี่ (17–19 หลักเลือก อื่นๆ)'
    },
    'UnionPay 17~19자리는 기타를 선택하세요.': {
      EN: 'For UnionPay 17–19 digits, choose Other.',
      JP: '銀聯17~19桁は「その他」を選択してください。',
      CH: '银联 17~19 位请选择「其他」。',
      TH: 'UnionPay 17–19 หลักให้เลือก อื่นๆ'
    },
    '국내 카드번호 16자리를 모두 입력하세요.': {
      EN: 'Enter all 16 digits of the domestic card number.',
      JP: '国内カード番号16桁をすべて入力してください。',
      CH: '请输入完整的 16 位国内卡号。',
      TH: 'กรอกหมายเลขบัตรในประเทศครบ 16 หลัก'
    },
    '기타 카드번호는 13자리 이상 입력하세요.': {
      EN: 'Other: enter at least 13 digits.',
      JP: 'その他: 13桁以上入力してください。',
      CH: '其他: 至少输入 13 位。',
      TH: 'อื่นๆ: กรอกอย่างน้อย 13 หลัก'
    },
    '기타 카드번호는 16자리 이하로 입력하세요.': {
      EN: 'Other: up to 16 digits.',
      JP: 'その他: 16桁以下で入力してください。',
      CH: '其他: 最多 16 位。',
      TH: 'อื่นๆ: ไม่เกิน 16 หลัก'
    },
    '기타 카드번호는 13~16자리로 입력하세요.': {
      EN: 'Other: enter 13–16 digits.',
      JP: 'その他: 13~16桁で入力してください。',
      CH: '其他: 输入 13~16 位。',
      TH: 'อื่นๆ: 13–16 หลัก'
    },
    '카드번호는 13~19자리 숫자여야 합니다.': {
      EN: 'Card number must be 13–19 digits.',
      JP: 'カード番号は13~19桁の数字である必要があります。',
      CH: '卡号须为 13~19 位数字。',
      TH: 'หมายเลขบัตรต้องเป็นตัวเลข 13–19 หลัก'
    },
    '카드번호는 15자리(AMEX) 또는 16자리여야 합니다.': {
      EN: 'Card number must be 15 digits (AMEX) or 16 digits.',
      JP: 'カード番号は15桁(AMEX)または16桁である必要があります。',
      CH: '卡号须为 15 位(AMEX)或 16 位。',
      TH: 'หมายเลขบัตรต้องมี 15 หลัก (AMEX) หรือ 16 หลัก'
    },
    '이미 비활성 등록된 카드입니다.': {
      EN: 'This card is already registered as inactive.',
      JP: 'このカードは既に非活性登録されています。',
      CH: '该卡已登记为非活跃。',
      TH: 'บัตรนี้ลงทะเบียนปิดใช้แล้ว'
    },
    '사유': { EN: 'Reason', JP: '理由', CH: '原因', TH: 'เหตุผล' },
    '해지': { EN: 'Release', JP: '解除', CH: '解除', TH: 'ยกเลิก' },
    '권한 없음': { EN: 'No permission', JP: '権限なし', CH: '无权限', TH: 'ไม่มีสิทธิ์' },
    '본사권한설정에서 삭제(전체) 또는 수정 권한이 필요합니다.': {
      EN: 'Delete (full) or Modify permission is required in HQ permissions.',
      JP: '本社権限設定で削除(全体)または修正権限が必要です。',
      CH: '需要在本社权限设置中授予删除(全部)或修改权限。',
      TH: 'ต้องมีสิทธิ์ลบ(เต็ม)หรือแก้ไขในสิทธิ์สำนักงานใหญ่'
    },
    'VIEW SETTING의 「OTP 해지」 열은 항상 표시됩니다. 해지는 목록 맨 오른쪽 버튼에서 실행하세요.': {
      EN: 'The OTP Release column is always shown. Use the button at the right end of the list to release.',
      JP: 'VIEW SETTINGの「OTP解除」列は常に表示されます。解除は一覧右端のボタンから実行してください。',
      CH: 'VIEW SETTING 中「OTP 解除」列始终显示。请在列表最右侧按钮执行解除。',
      TH: 'คอลัมน์ OTP ยกเลิกแสดงเสมอ กดปุ่มขวาสุดของรายการเพื่อยกเลิก'
    },
    '해지일시': { EN: 'Released at', JP: '解除日時', CH: '解除时间', TH: 'วันที่ยกเลิก' },
    '해지자': { EN: 'Released by', JP: '解除者', CH: '解除人', TH: 'ผู้ยกเลิก' },
    '등록된 비활성 카드가 없습니다.': {
      EN: 'No inactive cards registered.',
      JP: '登録された非活性カードはありません。',
      CH: '没有已登记的非活跃卡。',
      TH: 'ไม่มีบัตรปิดใช้ที่ลงทะเบียน'
    },
    '전체 PG': { EN: 'All PG', JP: '全PG', CH: '全部 PG', TH: 'PG ทั้งหมด' },
    '미지원': { EN: 'Not supported', JP: '未対応', CH: '不支持', TH: 'ไม่รองรับ' },
    '미사용 PG는 운영 지정 불가': {
      EN: 'Inactive PG cannot be set operational',
      JP: '未使用PGは運用指定不可',
      CH: '未使用的 PG 不可设为运营',
      TH: 'PG ที่ไม่ใช้งานกำหนดเป็นปฏิบัติการไม่ได้'
    },
    '통합(Chill)건수': { EN: 'Integrated (Chill) count', JP: '統合(Chill)件数', CH: '综合(Chill)笔数', TH: 'จำนวนรวม (Chill)' },
    '일치건수': { EN: 'Matched count', JP: '一致件数', CH: '一致笔数', TH: 'จำนวนที่ตรงกัน' },
    '불일치건수': { EN: 'Mismatch count', JP: '不一致件数', CH: '不一致笔数', TH: 'จำนวนไม่ตรงกัน' },
    '선택 일자 불일치': { EN: 'Selected date — mismatches', JP: '選択日の不一致', CH: '所选日期不一致', TH: 'วันที่เลือก — ไม่ตรงกัน' },
    '통합 결제액': { EN: 'Integrated pay amt', JP: '統合決済額', CH: '综合支付额', TH: 'ยอดชำระรวม' },
    'NOTI 결제액': { EN: 'NOTI pay amt', JP: 'NOTI決済額', CH: 'NOTI 支付额', TH: 'ยอด NOTI' },
    '통합 상태': { EN: 'Integrated status', JP: '統合状態', CH: '综合状态', TH: 'สถานะรวม' },
    'NOTI 상태': { EN: 'NOTI status', JP: 'NOTI状態', CH: 'NOTI 状态', TH: 'สถานะ NOTI' },
    '일치': { EN: 'Match', JP: '一致', CH: '一致', TH: 'ตรงกัน' },
    '불일치 없음': { EN: 'No mismatches', JP: '不一致なし', CH: '无不一致', TH: 'ไม่มีความไม่ตรงกัน' },
    'NOTI 미수신': { EN: 'NOTI not received', JP: 'NOTI未受信', CH: '未收到 NOTI', TH: 'ไม่ได้รับ NOTI' },
    '상태 불일치': { EN: 'Status mismatch', JP: '状態不一致', CH: '状态不一致', TH: 'สถานะไม่ตรงกัน' },
    '결제액 불일치': { EN: 'Amount mismatch', JP: '決済額不一致', CH: '支付额不一致', TH: 'ยอดไม่ตรงกัน' },
    '상태·결제액 불일치': { EN: 'Status & amount mismatch', JP: '状態・決済額不一致', CH: '状态·支付额不一致', TH: 'สถานะและยอดไม่ตรงกัน' },
    '통합 기준 맞춤': { EN: 'Sync to integrated', JP: '統合基準で合わせる', CH: '按综合对齐', TH: 'ปรับตามรวม' },
    '상태 불일치 일괄 맞춤': { EN: 'Sync all status mismatches', JP: '状態不一致を一括合わせ', CH: '批量按综合对齐状态', TH: 'ปรับสถานะไม่ตรงกันทั้งหมด' },
    '선택 일자의 상태 불일치 건을 통합(ChillPay) 기준으로 일괄 맞춥니다. 계속할까요?': {
      EN: 'Sync all status mismatches for this day to ChillPay integrated status. Continue?',
      JP: '選択日の状態不一致をChillPay統合基準で一括合わせします。続行しますか？',
      CH: '将该日所有状态不一致按 ChillPay 综合标准批量对齐。继续吗？',
      TH: 'ปรับสถานะไม่ตรงกันของวันนี้ตาม ChillPay รวม ดำเนินการต่อหรือไม่'
    },
    '상태 일괄 맞춤 완료': { EN: 'Batch status sync done', JP: '状態一括合わせ完了', CH: '批量状态对齐完成', TH: 'ปรับสถานะแบบกลุ่มเสร็จ' },
    '상태 일괄 맞춤에 실패했습니다.': { EN: 'Batch status sync failed.', JP: '状態一括合わせに失敗しました。', CH: '批量状态对齐失败。', TH: 'ปรับสถานะแบบกลุ่มล้มเหลว' },
    '요청·대기': { EN: 'Request/pending', JP: 'リクエスト・待機', CH: '请求/等待', TH: 'คำขอ/รอ' },
    '건 제외': { EN: ' excluded', JP: '件除外', CH: '件排除', TH: ' รายการยกเว้น' },
    '통합(ChillPay) 상태에 맞춰 NOTI 결제내역 상태를 갱신합니다. 계속할까요?': {
      EN: 'Update NOTI payment row status to match integrated (ChillPay). Continue?',
      JP: 'NOTI決済内訳の状態を統合(ChillPay)に合わせて更新します。続行しますか？',
      CH: '将 NOTI 支付记录状态更新为与综合(ChillPay)一致。是否继续？',
      TH: 'อัปเดตสถานะ NOTI ให้ตรงกับรวม (ChillPay) ต่อไหม'
    },
    '상태 맞춤에 실패했습니다.': {
      EN: 'Status sync failed.',
      JP: '状態合わせに失敗しました。',
      CH: '状态对齐失败。',
      TH: 'ปรับสถานะไม่สำเร็จ'
    },
    '상태 맞춤 완료': {
      EN: 'Status synced',
      JP: '状態を合わせました',
      CH: '状态已对齐',
      TH: 'ปรับสถานะแล้ว'
    },
    '처리': { EN: 'Action', JP: '処理', CH: '处理', TH: 'ดำเนินการ' },
    '검증 리포트 조회에 실패했습니다.': {
      EN: 'Verify report request failed.',
      JP: '検証レポートの照会に失敗しました。',
      CH: '验证报表查询失败。',
      TH: 'โหลดรายงานตรวจสอบไม่สำเร็จ'
    },
    '검증 리포트 응답을 해석할 수 없습니다.': {
      EN: 'Unable to parse verify report response.',
      JP: '検証レポート応答を解釈できません。',
      CH: '无法解析验证报表响应。',
      TH: 'แปลงผลรายงานตรวจสอบไม่ได้'
    },
    '결제시간': { EN: 'Payment time', JP: '決済時刻', CH: '支付时间', TH: 'เวลาชำระเงิน' },
    '거래일': { EN: 'Txn date', JP: '取引日', CH: '交易日期', TH: 'วันที่ทำรายการ' },
    '적재일': { EN: 'Ingest date', JP: '取込日', CH: '入库日', TH: 'วันที่บันทึก' },
    '전체건수': { EN: 'All txns', JP: '全件数', CH: '全部笔数', TH: 'ทุกรายการ' },
    '총건수(칠페이)': { EN: 'Total (ChillPay)', JP: '総件数（ChillPay）', CH: '总笔数（ChillPay）', TH: 'จำนวนรวม (ChillPay)' },
    '정산유무': { EN: 'Settlement status', JP: '精算有無', CH: '结算状态', TH: 'สถานะการชำระ' },
    '정산예정일': { EN: 'Expected settlement date', JP: '精算予定日', CH: '预计结算日', TH: 'วันที่คาดว่าจะชำระ' },
    '정산예정': {
      EN: 'Est. settlement',
      JP: '精算予定',
      CH: '预计结算',
      TH: 'คาดชำระ'
    },
    '복수가맹': { EN: 'Multiple merchants', JP: '複数加盟店', CH: '多家商户', TH: 'หลายร้านค้า' },
    '복수': { EN: 'Multiple', JP: '複数', CH: '多种', TH: 'หลายรายการ' },
    'VAT': { EN: 'VAT', JP: 'VAT', CH: '增值税', TH: 'VAT' },

    /* 목록 하단 페이지네이션 — screens.js data-pg-ui-t(한 번에 보기 / 건 (총 N건)) 공통 */
    '한 번에 보기:': {
      EN: 'Page size:',
      JP: '表示件数:',
      CH: '每页条数:',
      TH: 'จำนวนต่อหน้า:'
    },
    '건 (총': {
      EN: 'Total ',
      JP: '合計 ',
      CH: '共 ',
      TH: 'รวม '
    },
    '건)': {
      EN: ' rows',
      JP: ' 件',
      CH: ' 条',
      TH: ' รายการ'
    },
    '1000건': {
      EN: '1,000 rows',
      JP: '1000件',
      CH: '1000条',
      TH: '1,000 รายการ'
    },
    '모두': {
      EN: 'All',
      JP: 'すべて',
      CH: '全部',
      TH: 'ทั้งหมด'
    },
    '모두다운로드': {
      EN: 'Download all',
      JP: 'すべてダウンロード',
      CH: '全部下载',
      TH: 'ดาวน์โหลดทั้งหมด'
    },
    '모든리스트': {
      EN: 'All list',
      JP: 'すべてのリスト',
      CH: '全部列表',
      TH: 'รายการทั้งหมด'
    },
    '먼저 [검색]으로 조회한 뒤 「모두」를 선택하세요.': {
      EN: 'Run [Search] first, then choose All.',
      JP: '先に［検索］で照会してから「すべて」を選んでください。',
      CH: '请先[搜索]查询，再选择「全部」。',
      TH: 'กด [ค้นหา] ก่อน แล้วจึงเลือก「ทั้งหมด」'
    },
    '검색 결과 전체를 불러오는 중입니다…': {
      EN: 'Loading all search results…',
      JP: '検索結果全体を読み込み中…',
      CH: '正在加载全部搜索结果…',
      TH: 'กำลังโหลดผลการค้นหาทั้งหมด…'
    },
    '이 화면에서는 모두다운로드를 지원하지 않습니다.': {
      EN: 'Download all is not supported on this screen.',
      JP: 'この画面では一括ダウンロードに対応していません。',
      CH: '此画面不支持全部下载。',
      TH: 'หน้านี้ไม่รองรับการดาวน์โหลดทั้งหมด'
    },
    '다운로드할 검색 결과가 없습니다. [검색] 후 다시 시도하세요.': {
      EN: 'No search results to download. Run [Search] and try again.',
      JP: 'ダウンロードする検索結果がありません。［検索］後に再試行してください。',
      CH: '没有可下载的搜索结果。请先[搜索]后再试。',
      TH: 'ไม่มีผลค้นหาให้ดาวน์โหลด กด [ค้นหา] แล้วลองอีกครั้ง'
    },
    '검색 결과가 {MAX}건을 초과하여 상위 {MAX}건만 다운로드합니다.': {
      EN: 'Search results exceed {MAX} rows; only the first {MAX} will be downloaded.',
      JP: '検索結果が{MAX}件を超えるため、先頭{MAX}件のみダウンロードします。',
      CH: '搜索结果超过 {MAX} 条，仅下载前 {MAX} 条。',
      TH: 'ผลค้นหาเกิน {MAX} รายการ จะดาวน์โหลดเพียง {MAX} รายการแรก'
    },

    /* 수수료내역 (/calc/feeList, /settlement/feeList) — 2단 그룹 헤더·열·VIEW SETTING 가이드 */
    '거래': { EN: 'Transaction', JP: '取引', CH: '交易', TH: 'ธุรกรรม' },
    '승인 / 결제수수료(%)': {
      EN: 'Approval / pay fee (%)',
      JP: '承認 / 決済手数料(%)',
      CH: '授权 / 支付手续费(%)',
      TH: 'อนุมัติ / ค่าธรรมเนียมชำระ (%)'
    },
    '기타수수료': { EN: 'Other fees', JP: 'その他手数料', CH: '其他手续费', TH: 'ค่าธรรมเนียมอื่น' },
    '실패·취소·무효·환불·차지백': {
      EN: 'Fail · Cancel · Void · Refund · Chargeback',
      JP: '失敗・取消・無効・返金・チャージバック',
      CH: '失败·取消·作废·退款·拒付',
      TH: 'ล้มเหลว·ยกเลิก·โมฆะ·คืนเงิน·ชาร์จแบ็ก'
    },
    '차감·지급': { EN: 'Deduction · Payout', JP: '控除・支払', CH: '扣减·拨付', TH: 'หัก·จ่าย' },
    /* 대행거래내역 (/ops/agencyTxnList) — 2단 그룹 헤더·열 */
    'PG·가맹': { EN: 'PG · Merchant', JP: 'PG・加盟店', CH: 'PG·商户', TH: 'PG·ร้านค้า' },
    '승인 / 대행수수료(%)': {
      EN: 'Approval / agency fee (%)',
      JP: '承認 / 代行手数料(%)',
      CH: '授权 / 代行手续费(%)',
      TH: 'อนุมัติ / ค่าธรรมเนียมตัวแทน (%)'
    },
    '합계·정산': { EN: 'Total · settlement', JP: '合計・精算', CH: '合计·结算', TH: 'รวม·ชำระ' },
    '대행수수료설정 T/H/D 기준 도래 Y/N': {
      EN: 'Agency fee policy T/H/D due (Y/N)',
      JP: '代行手数料設定 T/H/D 到来 Y/N',
      CH: '代行手续费设置 T/H/D 到期 Y/N',
      TH: 'ครบกำหนด T/H/D นโยบายตัวแทน Y/N'
    },
    '거래번호(우리)': { EN: 'Txn ID (ours)', JP: '取引番号（自社）', CH: '交易号（我方）', TH: 'รหัสธุรกรรม (ภายใน)' },
    '결제통화': { EN: 'Pay currency', JP: '決済通貨', CH: '支付币种', TH: 'สกุลชำระ' },
    '정책통화': { EN: 'Policy currency', JP: '政策通貨', CH: '政策币种', TH: 'สกุลนโยบาย' },
    '결제(%)': { EN: 'Pay (%)', JP: '決済(%)', CH: '支付(%)', TH: 'ชำระ (%)' },
    '담보율(%)': { EN: 'Collateral rate (%)', JP: '担保率(%)', CH: '担保比例(%)', TH: 'อัตราหลักประกัน (%)' },
    '보류일': { EN: 'Hold days', JP: '保留日数', CH: '留存天数', TH: 'วันพัก' },
    '담보추정액': { EN: 'Collateral estimate', JP: '担保見積額', CH: '担保估计额', TH: 'ประมาณการหลักประกัน' },
    '지급예상액': { EN: 'Expected payout', JP: '支払予定額', CH: '预计拨付额', TH: 'ยอดจ่ายโดยประมาณ' },
    '정산액': { EN: 'Settlement amount', JP: '精算額', CH: '结算额', TH: 'ยอดชำระบัญชี' },
    '부가세': { EN: 'VAT', JP: '消費税', CH: '增值税', TH: 'VAT' },
    '루트': { EN: 'Route', JP: 'ルート', CH: '路由', TH: 'เส้นทาง' },
    '거래시간': { EN: 'Txn time', JP: '取引時刻', CH: '交易时间', TH: 'เวลาทำรายการ' },
    'USDT': { EN: 'USDT', JP: 'USDT', CH: 'USDT', TH: 'USDT' },
    '3DS': { EN: '3DS', JP: '3DS', CH: '3DS', TH: '3DS' },
    '성공제외': { EN: 'Exclude success', JP: '成功を除く', CH: '不含成功', TH: 'ยกเว้นสำเร็จ' },
    '가맹 정산설정 정산주기': {
      EN: 'Settlement cycle (merchant setting)',
      JP: '精算周期（加盟店設定）',
      CH: '结算周期（商户设置）',
      TH: 'รอบชำระ (ตั้งค่าร้าน)'
    },
    'USDT(%) 과금액(승인금액 기준)': {
      EN: 'USDT (%) charge (on approved amount)',
      JP: 'USDT(%) 課金額（承認金額基準）',
      CH: 'USDT(%) 计费额（按批准金额）',
      TH: 'ค่า USDT (%) ตามยอดอนุมัติ'
    },
    'FX(%) 과금액(승인금액 기준)': {
      EN: 'FX (%) charge (on approved amount)',
      JP: 'FX(%) 課金額（承認金額基準）',
      CH: 'FX(%) 计费额（按批准金额）',
      TH: 'ค่า FX (%) ตามยอดอนุมัติ'
    },
    '3DS 건당 고정 과금액': {
      EN: '3DS fixed per-txn charge',
      JP: '3DS 件当固定課金額',
      CH: '3DS 按笔固定计费',
      TH: 'ค่า 3DS คงที่ต่อรายการ'
    },

    URL: { EN: 'URL', JP: 'URL', CH: 'URL', TH: 'URL' },
    API: { EN: 'API', JP: 'API', CH: 'API', TH: 'API' },
    /* --- server API fail messages (auto) --- */
    '가맹 업체코드를 확인할 수 없습니다.': {
      EN: 'merchant company code could not be verified.',
      JP: '加盟店 企業コードを確認できません。',
      CH: '无法确认加盟店 企业代码。',
      TH: 'ไม่สามารถยืนยันร้านค้า รหัสบริษัทได้'
    },
    '가맹 정보를 확인할 수 없습니다.': {
      EN: 'merchant information could not be verified.',
      JP: '加盟店 情報を確認できません。',
      CH: '无法确认加盟店 信息。',
      TH: 'ไม่สามารถยืนยันร้านค้า ข้อมูลได้'
    },
    '가맹 프로필을 찾을 수 없습니다.': {
      EN: 'merchant profile could not be found.',
      JP: '加盟店 プロフィールが見つかりません。',
      CH: '未找到加盟店 资料。',
      TH: 'ไม่พบร้านค้า โปรไฟล์'
    },
    '가맹점 계정만 조회할 수 있습니다.': {
      EN: 'Only merchant account can view this.',
      JP: '加盟店アカウントのみ参照できます。',
      CH: '仅加盟店账号可查看。',
      TH: 'เฉพาะบัญชีร้านค้าเท่านั้นที่ดูได้'
    },
    '가맹점 계정에서는 산하 목록 조회가 제공되지 않습니다.': {
      EN: 'Sub-list lookup is not available for merchant accounts.',
      JP: '加盟店アカウントでは配下リストの照会は提供されません。',
      CH: '加盟店账号不提供下属列表查询。',
      TH: 'บัญชีร้านค้าไม่สามารถเรียกดูรายการในสังกัดได้'
    },
    '가맹점 계정에서는 산하 운영 보류 설정을 변경할 수 없습니다.': {
      EN: 'subordinate 운영 보류 setting을 변경할 수 없습니다. (merchant account).',
      JP: '加盟店アカウントでは配下 운영 보류 設定을 변경할 수 없습니다.',
      CH: '加盟店账号无法下属 운영 보류 设置을 변경할 수 없습니다.。',
      TH: 'บัญชีร้านค้า: ในสังกัด 운영 보류 การตั้งค่า을 변경할 수 없습니다.'
    },
    '가맹점 계정에서는 이 설정을 변경할 수 없습니다.': {
      EN: '이 setting을 변경할 수 없습니다. (merchant account).',
      JP: '加盟店アカウントでは이 設定을 변경할 수 없습니다.',
      CH: '加盟店账号无法이 设置을 변경할 수 없습니다.。',
      TH: 'บัญชีร้านค้า: 이 การตั้งค่า을 변경할 수 없습니다.'
    },
    '가맹점 정보에서 결제 후속조치 사용이 꺼져 있습니다.': {
      EN: 'merchant information에서 결제 후속조치 사용이 꺼져 있습니다.',
      JP: '加盟店 情報에서 결제 후속조치 사용이 꺼져 있습니다.',
      CH: '加盟店 信息에서 결제 후속조치 사용이 꺼져 있습니다.',
      TH: 'ร้านค้า ข้อมูล에서 결제 후속조치 사용이 꺼져 있습니다.'
    },
    '가맹점 정보에서 해당 후속조치가 허용되지 않습니다.': {
      EN: 'merchant information에서 해당 후속조치가 허용되지 않습니다.',
      JP: '加盟店 情報에서 해당 후속조치가 허용되지 않습니다.',
      CH: '加盟店 信息에서 해당 후속조치가 허용되지 않습니다.',
      TH: 'ร้านค้า ข้อมูล에서 해당 후속조치가 허용되지 않습니다.'
    },
    '가맹점 조직 정보가 없습니다.': {
      EN: 'merchant organization information가 없습니다.',
      JP: '加盟店 組織 情報가 없습니다.',
      CH: '加盟店 组织 信息가 없습니다.',
      TH: 'ร้านค้า องค์กร ข้อมูล가 없습니다.'
    },
    '가맹점 코드가 필요합니다.': {
      EN: 'merchant code is required.',
      JP: '加盟店 コードが必要です。',
      CH: '需要加盟店 代码。',
      TH: 'ต้องระบุร้านค้า รหัส'
    },
    '가맹점 코드만 업로드 가능합니다.': {
      EN: 'Only merchant code can upload.',
      JP: '加盟店 コードのみアップロードできます。',
      CH: '仅加盟店 代码可上传。',
      TH: 'เฉพาะร้านค้า รหัสเท่านั้นที่อัปโหลดได้'
    },
    '가맹점 프로필을 찾을 수 없습니다.': {
      EN: 'merchant profile could not be found.',
      JP: '加盟店 プロフィールが見つかりません。',
      CH: '未找到加盟店 资料。',
      TH: 'ไม่พบร้านค้า โปรไฟล์'
    },
    '가맹점(조직단계 MERCHANT)만 조회할 수 있습니다.': {
      EN: 'Only merchant (MERCHANT level) can view this.',
      JP: '加盟店（組織段階 MERCHANT）のみ参照できます。',
      CH: '仅加盟店（组织层级 MERCHANT）可查看。',
      TH: 'เฉพาะร้านค้า (ระดับ MERCHANT)เท่านั้นที่ดูได้'
    },
    '가맹점(조직단계 MERCHANT)만 키트를 조회할 수 있습니다.': {
      EN: 'Only merchant (MERCHANT level) can view the kit.',
      JP: '加盟店（組織段階 MERCHANT）のみキットを参照できます。',
      CH: '仅加盟店（组织层级 MERCHANT）可查看工具包。',
      TH: 'เฉพาะร้านค้า (ระดับ MERCHANT)เท่านั้นที่ดูชุดเครื่องมือได้'
    },
    '가맹점(MERCHANT) 또는 총판(MASTER_DIST) 코드만 지원합니다.': {
      EN: 'merchant (MERCHANT) 또는 master distributor (MASTER_DIST) code만 지원합니다.',
      JP: '加盟店（MERCHANT） 또는 総販（MASTER_DIST） コード만 지원합니다.',
      CH: '加盟店（MERCHANT） 또는 总经销（MASTER_DIST） 代码만 지원합니다.',
      TH: 'ร้านค้า (MERCHANT) 또는 ตัวแทนหลัก (MASTER_DIST) รหัส만 지원합니다.'
    },
    '가맹점만 결제대행사를 등록할 수 있습니다.': {
      EN: 'merchant만 payment gateway를 등록할 수 있습니다.',
      JP: '加盟店만 決済代行를 등록할 수 있습니다.',
      CH: '加盟店만 支付机构를 등록할 수 있습니다.',
      TH: 'ร้านค้า만 ผู้ให้บริการชำระเงิน를 등록할 수 있습니다.'
    },
    '가맹점만 상품을 등록할 수 있습니다.': {
      EN: 'merchant만 상품을 등록할 수 있습니다.',
      JP: '加盟店만 상품을 등록할 수 있습니다.',
      CH: '加盟店만 상품을 등록할 수 있습니다.',
      TH: 'ร้านค้า만 상품을 등록할 수 있습니다.'
    },
    '가맹점만 시크릿을 발급할 수 있습니다.': {
      EN: 'merchant만 시크릿을 발급할 수 있습니다.',
      JP: '加盟店만 시크릿을 발급할 수 있습니다.',
      CH: '加盟店만 시크릿을 발급할 수 있습니다.',
      TH: 'ร้านค้า만 시크릿을 발급할 수 있습니다.'
    },
    '가맹점만 업로드할 수 있습니다.': {
      EN: 'Only merchant can upload.',
      JP: '加盟店のみアップロードできます。',
      CH: '仅加盟店可上传。',
      TH: 'เฉพาะร้านค้าเท่านั้นที่อัปโหลดได้'
    },
    '가맹점만 챗봇 기본안내를 저장할 수 있습니다.': {
      EN: 'merchant만 챗봇 기본안내를 저장할 수 있습니다.',
      JP: '加盟店만 챗봇 기본안내를 저장할 수 있습니다.',
      CH: '加盟店만 챗봇 기본안내를 저장할 수 있습니다.',
      TH: 'ร้านค้า만 챗봇 기본안내를 저장할 수 있습니다.'
    },
    '가맹점만 초안 생성이 가능합니다.': {
      EN: 'merchant만 초안 생성이 가능합니다.',
      JP: '加盟店만 초안 생성이 가능합니다.',
      CH: '加盟店만 초안 생성이 가능합니다.',
      TH: 'ร้านค้า만 초안 생성이 가능합니다.'
    },
    '가맹점만 해당 기능을 사용할 수 있습니다.': {
      EN: 'merchant만 해당 기능을 사용할 수 있습니다.',
      JP: '加盟店만 해당 기능을 사용할 수 있습니다.',
      CH: '加盟店만 해당 기능을 사용할 수 있습니다.',
      TH: 'ร้านค้า만 해당 기능을 사용할 수 있습니다.'
    },
    '가맹점을 찾을 수 없습니다.': {
      EN: 'merchant could not be found.',
      JP: '加盟店が見つかりません。',
      CH: '未找到加盟店。',
      TH: 'ไม่พบร้านค้า'
    },
    '가맹점을 찾을 수 없습니다. compId 또는 merchantId를 넣으세요.': {
      EN: 'merchant을 찾을 수 없습니다. compId 또는 merchantId를 넣으세요.',
      JP: '加盟店을 찾을 수 없습니다. compId 또는 merchantId를 넣으세요.',
      CH: '加盟店을 찾을 수 없습니다. compId 또는 merchantId를 넣으세요.',
      TH: 'ร้านค้า을 찾을 수 없습니다. compId 또는 merchantId를 넣으세요.'
    },
    '가맹코드·아이디·비밀번호·OTP를 입력하세요.': {
      EN: 'merchantcode·아이디·비밀번호·OTP를 입력하세요.',
      JP: '加盟店コード·아이디·비밀번호·OTP를 입력하세요.',
      CH: '加盟店代码·아이디·비밀번호·OTP를 입력하세요.',
      TH: 'ร้านค้ารหัส·아이디·비밀번호·OTP를 입력하세요.'
    },
    '강제환불 처리 가능 기간이 아닙니다. (태국 기준 일반 환불 종료 시점 다음날 동일 시각부터 강제환불 일수 확인)': {
      EN: '강제환불 처리 가능 기간이 아닙니다. (태국 기준 일반 환불 종료 시점 다음날 동일 시각부터 강제환불 일수 확인)',
      JP: '강제환불 처리 가능 기간이 아닙니다. (태국 기준 일반 환불 종료 시점 다음날 동일 시각부터 강제환불 일수 확인)',
      CH: '강제환불 처리 가능 기간이 아닙니다. (태국 기준 일반 환불 종료 시점 다음날 동일 시각부터 강제환불 일수 확인)',
      TH: '강제환불 처리 가능 기간이 아닙니다. (태국 기준 일반 환불 종료 시점 다음날 동일 시각부터 강제환불 일수 확인)'
    },
    '개발용 기능이 비활성화되어 있습니다. app.features.comp-dev-tree-remove=true 인 프로파일에서만 사용할 수 있습니다.': {
      EN: 'Only 개발용 기능이 비활성화되어 있습니다. app.features.comp-dev-tree-remove=true 인 프로file에서 can use this.',
      JP: '개발용 기능이 비활성화되어 있습니다. app.features.comp-dev-tree-remove=true 인 프로ファイル에서のみ使用できます。',
      CH: '仅개발용 기능이 비활성화되어 있습니다. app.features.comp-dev-tree-remove=true 인 프로文件에서可使用。',
      TH: 'เฉพาะ개발용 기능이 비활성화되어 있습니다. app.features.comp-dev-tree-remove=true 인 프로ไฟล์에서เท่านั้นที่ใช้ได้'
    },
    '개별 설정 시 mode는 AUTO 또는 MANUAL 이어야 합니다.': {
      EN: '개별 setting 시 mode는 AUTO 또는 MANUAL 이어야 합니다.',
      JP: '개별 設定 시 mode는 AUTO 또는 MANUAL 이어야 합니다.',
      CH: '개별 设置 시 mode는 AUTO 또는 MANUAL 이어야 합니다.',
      TH: '개별 การตั้งค่า 시 mode는 AUTO 또는 MANUAL 이어야 합니다.'
    },
    '개별 조직 권한은 총본사(또는 시스템 관리자)만 저장할 수 있습니다.': {
      EN: 'Only 개별 organization permission은 headquarters(또는 system administrator) can save.',
      JP: '개별 組織 権限은 総本社(또는 システム管理者)のみ保存できます。',
      CH: '仅개별 组织 权限은 总总部(또는 系统管理员)可保存。',
      TH: 'เฉพาะ개별 องค์กร สิทธิ์은 สำนักงานใหญ่(또는 ผู้ดูแลระบบ)เท่านั้นที่บันทึกได้'
    },
    '거래를 찾을 수 없습니다.': {
      EN: 'transaction could not be found.',
      JP: '取引が見つかりません。',
      CH: '未找到交易。',
      TH: 'ไม่พบธุรกรรม'
    },
    '거래번호(trnId)가 필요합니다.': {
      EN: 'transaction번호(trnId) is required.',
      JP: '取引번호(trnId)が必要です。',
      CH: '需要交易번호(trnId)。',
      TH: 'ต้องระบุธุรกรรม번호(trnId)'
    },
    '거래에 가맹점 코드가 없습니다.': {
      EN: 'transaction에 merchant code가 없습니다.',
      JP: '取引에 加盟店 コード가 없습니다.',
      CH: '交易에 加盟店 代码가 없습니다.',
      TH: 'ธุรกรรม에 ร้านค้า รหัส가 없습니다.'
    },
    '거래일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).': {
      EN: 'transaction일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).',
      JP: '取引일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).',
      CH: '交易일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).',
      TH: 'ธุรกรรม일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).'
    },
    '거래일(day)은 필수입니다.': {
      EN: 'transaction일(day)은 필수입니다.',
      JP: '取引일(day)은 필수입니다.',
      CH: '交易일(day)은 필수입니다.',
      TH: 'ธุรกรรม일(day)은 필수입니다.'
    },
    '거래일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.': {
      EN: 'transaction일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.',
      JP: '取引일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.',
      CH: '交易일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.',
      TH: 'ธุรกรรม일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.'
    },
    '거래일자 시작이 종료보다 늦을 수 없습니다.': {
      EN: 'transaction일자 시작이 종료보다 늦을 수 없습니다.',
      JP: '取引일자 시작이 종료보다 늦을 수 없습니다.',
      CH: '交易일자 시작이 종료보다 늦을 수 없습니다.',
      TH: 'ธุรกรรม일자 시작이 종료보다 늦을 수 없습니다.'
    },
    '거래일자(searchFromDate, searchToDate)는 필수입니다.': {
      EN: 'transaction일자(searchFromDate, searchToDate)는 필수입니다.',
      JP: '取引일자(searchFromDate, searchToDate)는 필수입니다.',
      CH: '交易일자(searchFromDate, searchToDate)는 필수입니다.',
      TH: 'ธุรกรรม일자(searchFromDate, searchToDate)는 필수입니다.'
    },
    '결제 링크를 이메일로 받으려면 이메일을 입력하세요.': {
      EN: '결제 링크를 이메일로 받으려면 이메일을 입력하세요.',
      JP: '결제 링크를 이메일로 받으려면 이메일을 입력하세요.',
      CH: '결제 링크를 이메일로 받으려면 이메일을 입력하세요.',
      TH: '결제 링크를 이메일로 받으려면 이메일을 입력하세요.'
    },
    '결제대행사(JSON) 형식이 올바르지 않습니다.': {
      EN: 'Invalid payment gateway(JSON) format.',
      JP: '決済代行(JSON)の形式が正しくありません。',
      CH: '支付机构(JSON)格式不正确。',
      TH: 'รูปแบบผู้ให้บริการชำระเงิน(JSON)ไม่ถูกต้อง'
    },
    '결제대행사(PG) 코드가 비었습니다.': {
      EN: 'payment gateway (PG) code가 비었습니다.',
      JP: '決済代行（PG） コード가 비었습니다.',
      CH: '支付机构（PG） 代码가 비었습니다.',
      TH: 'ผู้ให้บริการชำระเงิน (PG) รหัส가 비었습니다.'
    },
    '결제대행사(PG)를 선택하세요.': {
      EN: 'payment gateway (PG)를 선택하세요.',
      JP: '決済代行（PG）를 선택하세요.',
      CH: '支付机构（PG）를 선택하세요.',
      TH: 'ผู้ให้บริการชำระเงิน (PG)를 선택하세요.'
    },
    '결제액 불일치가 있는 건은 상태만 맞출 수 없습니다. 금액을 먼저 확인하세요.': {
      EN: '결제액 불일치가 있는 건은 상태만 맞출 수 없습니다. amount을 먼저 확인하세요.',
      JP: '결제액 불일치가 있는 건은 상태만 맞출 수 없습니다. 金額을 먼저 확인하세요.',
      CH: '결제액 불일치가 있는 건은 상태만 맞출 수 없습니다. 金额을 먼저 확인하세요.',
      TH: '결제액 불일치가 있는 건은 상태만 맞출 수 없습니다. จำนวนเงิน을 먼저 확인하세요.'
    },
    '결제자 성명(이름·성)을 입력하세요.': {
      EN: '결제자 성명(이름·성)을 입력하세요.',
      JP: '결제자 성명(이름·성)을 입력하세요.',
      CH: '결제자 성명(이름·성)을 입력하세요.',
      TH: '결제자 성명(이름·성)을 입력하세요.'
    },
    '공개 API 베이스 URL이 없습니다.': {
      EN: '공개 API 베이스 URL이 없습니다.',
      JP: '공개 API 베이스 URL이 없습니다.',
      CH: '공개 API 베이스 URL이 없습니다.',
      TH: '공개 API 베이스 URL이 없습니다.'
    },
    '공개 API 베이스 URL이 없습니다. 배포설정에 publicApiBaseUrl 또는 노티 publicBaseUrl을 넣으세요.': {
      EN: '공개 API 베이스 URL이 없습니다. 배포setting에 publicApiBaseUrl 또는 notification publicBaseUrl을 넣으세요.',
      JP: '공개 API 베이스 URL이 없습니다. 배포設定에 publicApiBaseUrl 또는 通知 publicBaseUrl을 넣으세요.',
      CH: '공개 API 베이스 URL이 없습니다. 배포设置에 publicApiBaseUrl 또는 通知 publicBaseUrl을 넣으세요.',
      TH: '공개 API 베이스 URL이 없습니다. 배포การตั้งค่า에 publicApiBaseUrl 또는 การแจ้งเตือน publicBaseUrl을 넣으세요.'
    },
    '공지를 찾을 수 없습니다.': {
      EN: 'notice could not be found.',
      JP: 'お知らせが見つかりません。',
      CH: '未找到公告。',
      TH: 'ไม่พบประกาศ'
    },
    '공지사항 등록 권한이 없습니다.': {
      EN: 'You do not have notice사항 등록 permission.',
      JP: 'お知らせ사항 등록権限がありません。',
      CH: '无公告사항 등록权限。',
      TH: 'ไม่มีสิทธิ์ประกาศ사항 등록'
    },
    '공지사항 등록 권한이 없습니다. (총본사·본사·총판이면서 해당 화면이 수정 이상으로 설정된 경우만 가능)': {
      EN: 'notice사항 등록 permission이 없습니다. (headquarters·head office·master distributor이면서 해당 화면이 수정 이상으로 setting된 경우만 가능)',
      JP: 'お知らせ사항 등록 権限이 없습니다. (総本社·本社·総販이면서 해당 화면이 수정 이상으로 設定된 경우만 가능)',
      CH: '公告사항 등록 权限이 없습니다. (总总部·总部·总经销이면서 해당 화면이 수정 이상으로 设置된 경우만 가능)',
      TH: 'ประกาศ사항 등록 สิทธิ์이 없습니다. (สำนักงานใหญ่·สำนักงานใหญ่·ตัวแทนหลัก이면서 해당 화면이 수정 이상으로 การตั้งค่า된 경우만 가능)'
    },
    '관리자(ADMIN)만 사용할 수 있는 개발용 기능입니다.': {
      EN: 'administrator(ADMIN)만 사용할 수 있는 개발용 기능입니다.',
      JP: '管理者(ADMIN)만 사용할 수 있는 개발용 기능입니다.',
      CH: '管理员(ADMIN)만 사용할 수 있는 개발용 기능입니다.',
      TH: 'ผู้ดูแลระบบ(ADMIN)만 사용할 수 있는 개발용 기능입니다.'
    },
    '관리자만 삭제할 수 있습니다.': {
      EN: 'Only administrator can delete.',
      JP: '管理者のみ削除できます。',
      CH: '仅管理员可删除。',
      TH: 'เฉพาะผู้ดูแลระบบเท่านั้นที่ลบได้'
    },
    '구독 plan 정보가 없습니다.': {
      EN: '구독 plan information가 없습니다.',
      JP: '구독 plan 情報가 없습니다.',
      CH: '구독 plan 信息가 없습니다.',
      TH: '구독 plan ข้อมูล가 없습니다.'
    },
    '권한 범위 내 사용자만 삭제할 수 있습니다.': {
      EN: 'Only permission 범위 내 사용자 can delete.',
      JP: '権限 범위 내 사용자のみ削除できます。',
      CH: '仅权限 범위 내 사용자可删除。',
      TH: 'เฉพาะสิทธิ์ 범위 내 사용자เท่านั้นที่ลบได้'
    },
    '권한 범위 내 사용자만 수정할 수 있습니다.': {
      EN: 'Only permission 범위 내 사용자 can modify.',
      JP: '権限 범위 내 사용자のみ修正できます。',
      CH: '仅权限 범위 내 사용자可修改。',
      TH: 'เฉพาะสิทธิ์ 범위 내 사용자เท่านั้นที่แก้ไขได้'
    },
    '권한그룹 CHATBOT 계정은 Google OTP 등록 후 챗봇 상품 관리를 사용할 수 있습니다. 로그인 시 안내에 따라 OTP를 등록하세요.': {
      EN: 'permission그룹 CHATBOT account은 Google OTP 등록 후 챗봇 상품 관리를 사용할 수 있습니다. login 시 안내에 따라 OTP를 등록하세요.',
      JP: '権限그룹 CHATBOT アカウント은 Google OTP 등록 후 챗봇 상품 관리를 사용할 수 있습니다. ログイン 시 안내에 따라 OTP를 등록하세요.',
      CH: '权限그룹 CHATBOT 账号은 Google OTP 등록 후 챗봇 상품 관리를 사용할 수 있습니다. 登录 시 안내에 따라 OTP를 등록하세요.',
      TH: 'สิทธิ์그룹 CHATBOT บัญชี은 Google OTP 등록 후 챗봇 상품 관리를 사용할 수 있습니다. เข้าสู่ระบบ 시 안내에 따라 OTP를 등록하세요.'
    },
    '권한이 없습니다.': {
      EN: 'You do not have permission.',
      JP: '権限がありません。',
      CH: '无权限。',
      TH: 'ไม่มีสิทธิ์'
    },
    '그리드에서 한 건을 체크한 뒤 진행하세요.': {
      EN: '그리드에서 한 건을 체크한 뒤 진행하세요.',
      JP: '그리드에서 한 건을 체크한 뒤 진행하세요.',
      CH: '그리드에서 한 건을 체크한 뒤 진행하세요.',
      TH: '그리드에서 한 건을 체크한 뒤 진행하세요.'
    },
    '금액 형식이 올바르지 않습니다.': {
      EN: 'Invalid amount format.',
      JP: '金額の形式が正しくありません。',
      CH: '金额格式不正确。',
      TH: 'รูปแบบจำนวนเงินไม่ถูกต้อง'
    },
    '금액(amount) 형식이 올바르지 않습니다.': {
      EN: 'Invalid amount(amount) format.',
      JP: '金額(amount)の形式が正しくありません。',
      CH: '金额(amount)格式不正确。',
      TH: 'รูปแบบจำนวนเงิน(amount)ไม่ถูกต้อง'
    },
    '금액은 0보다 커야 합니다.': {
      EN: 'amount은 0보다 커야 합니다.',
      JP: '金額은 0보다 커야 합니다.',
      CH: '金额은 0보다 커야 합니다.',
      TH: 'จำนวนเงิน은 0보다 커야 합니다.'
    },
    '금액을 입력하세요.': {
      EN: 'amount을 입력하세요.',
      JP: '金額을 입력하세요.',
      CH: '金额을 입력하세요.',
      TH: 'จำนวนเงิน을 입력하세요.'
    },
    '금액을 확인하세요.': {
      EN: 'amount을 확인하세요.',
      JP: '金額을 확인하세요.',
      CH: '金额을 확인하세요.',
      TH: 'จำนวนเงิน을 확인하세요.'
    },
    '금액이 올바르지 않습니다.': {
      EN: 'amount이 올바르지 않습니다.',
      JP: '金額이 올바르지 않습니다.',
      CH: '金额이 올바르지 않습니다.',
      TH: 'จำนวนเงิน이 올바르지 않습니다.'
    },
    '기준국가는 KR/US/JP/TH/CN/GLOBAL만 가능합니다.': {
      EN: '기준국가는 KR/US/JP/TH/CN/GLOBAL만 가능합니다.',
      JP: '기준국가는 KR/US/JP/TH/CN/GLOBAL만 가능합니다.',
      CH: '기준국가는 KR/US/JP/TH/CN/GLOBAL만 가능합니다.',
      TH: '기준국가는 KR/US/JP/TH/CN/GLOBAL만 가능합니다.'
    },
    '내용이 너무 깁니다.': {
      EN: '내용이 너무 깁니다.',
      JP: '내용이 너무 깁니다.',
      CH: '내용이 너무 깁니다.',
      TH: '내용이 너무 깁니다.'
    },
    '노출 대상 조직 유형이 올바르지 않습니다.': {
      EN: '노출 대상 organization 유형이 올바르지 않습니다.',
      JP: '노출 대상 組織 유형이 올바르지 않습니다.',
      CH: '노출 대상 组织 유형이 올바르지 않습니다.',
      TH: '노출 대상 องค์กร 유형이 올바르지 않습니다.'
    },
    '노티 대상 이름을 입력하세요.': {
      EN: 'notification 대상 이름을 입력하세요.',
      JP: '通知 대상 이름을 입력하세요.',
      CH: '通知 대상 이름을 입력하세요.',
      TH: 'การแจ้งเตือน 대상 이름을 입력하세요.'
    },
    '노티 대상을 찾을 수 없습니다.': {
      EN: 'notification 대상 could not be found.',
      JP: '通知 대상が見つかりません。',
      CH: '未找到通知 대상。',
      TH: 'ไม่พบการแจ้งเตือน 대상'
    },
    '노티 코드 생성에 실패했습니다. 잠시 후 다시 시도하세요.': {
      EN: 'notification code 생성에 실패했습니다. 잠시 후 다시 시도하세요.',
      JP: '通知 コード 생성에 실패했습니다. 잠시 후 다시 시도하세요.',
      CH: '通知 代码 생성에 실패했습니다. 잠시 후 다시 시도하세요.',
      TH: 'การแจ้งเตือน รหัส 생성에 실패했습니다. 잠시 후 다시 시도하세요.'
    },
    '다음 플랜(예약) 건수 값이 올바르지 않습니다.': {
      EN: '다음 플랜(예약) 건수 값이 올바르지 않습니다.',
      JP: '다음 플랜(예약) 건수 값이 올바르지 않습니다.',
      CH: '다음 플랜(예약) 건수 값이 올바르지 않습니다.',
      TH: '다음 플랜(예약) 건수 값이 올바르지 않습니다.'
    },
    '다음 플랜(예약)은 양의 정수 건수만 지정할 수 있습니다.': {
      EN: '다음 플랜(예약)은 양의 정수 건수만 지정할 수 있습니다.',
      JP: '다음 플랜(예약)은 양의 정수 건수만 지정할 수 있습니다.',
      CH: '다음 플랜(예약)은 양의 정수 건수만 지정할 수 있습니다.',
      TH: '다음 플랜(예약)은 양의 정수 건수만 지정할 수 있습니다.'
    },
    '담당자 권한그룹 설정을 저장할 수 없습니다.': {
      EN: '담당자 permission그룹 setting을 저장할 수 없습니다.',
      JP: '담당자 権限그룹 設定을 저장할 수 없습니다.',
      CH: '담당자 权限그룹 设置을 저장할 수 없습니다.',
      TH: '담당자 สิทธิ์그룹 การตั้งค่า을 저장할 수 없습니다.'
    },
    '대기 상태만 취소할 수 있습니다.': {
      EN: '대기 상태만 취소할 수 있습니다.',
      JP: '대기 상태만 취소할 수 있습니다.',
      CH: '대기 상태만 취소할 수 있습니다.',
      TH: '대기 상태만 취소할 수 있습니다.'
    },
    '대기·부분차감 상태만 환수처리할 수 있습니다.': {
      EN: '대기·부분차감 상태만 환수처리할 수 있습니다.',
      JP: '대기·부분차감 상태만 환수처리할 수 있습니다.',
      CH: '대기·부분차감 상태만 환수처리할 수 있습니다.',
      TH: '대기·부분차감 상태만 환수처리할 수 있습니다.'
    },
    '대상 노티를 지정하세요.': {
      EN: '대상 notification를 지정하세요.',
      JP: '대상 通知를 지정하세요.',
      CH: '대상 通知를 지정하세요.',
      TH: '대상 การแจ้งเตือน를 지정하세요.'
    },
    '대상은 본사(REGIONAL) 업체만 지정할 수 있습니다.': {
      EN: '대상은 head office(REGIONAL) company만 지정할 수 있습니다.',
      JP: '대상은 本社(REGIONAL) 企業만 지정할 수 있습니다.',
      CH: '대상은 总部(REGIONAL) 企业만 지정할 수 있습니다.',
      TH: '대상은 สำนักงานใหญ่(REGIONAL) บริษัท만 지정할 수 있습니다.'
    },
    '대표 아이디가 없어 비밀번호를 변경할 수 없습니다.': {
      EN: '대표 아이디가 없어 비밀번호를 변경할 수 없습니다.',
      JP: '대표 아이디가 없어 비밀번호를 변경할 수 없습니다.',
      CH: '대표 아이디가 없어 비밀번호를 변경할 수 없습니다.',
      TH: '대표 아이디가 없어 비밀번호를 변경할 수 없습니다.'
    },
    '대표로 지정한 슬롯에 정산주기가 없습니다.': {
      EN: '대표로 지정한 슬롯에 settlement주기가 없습니다.',
      JP: '대표로 지정한 슬롯에 精算주기가 없습니다.',
      CH: '대표로 지정한 슬롯에 结算주기가 없습니다.',
      TH: '대표로 지정한 슬롯에 การชำระบัญชี주기가 없습니다.'
    },
    '동일 경로 접미(cb·rs 이후)의 쌍만 연결할 수 있습니다.': {
      EN: '동일 경로 접미(cb·rs 이후)의 쌍만 연결할 수 있습니다.',
      JP: '동일 경로 접미(cb·rs 이후)의 쌍만 연결할 수 있습니다.',
      CH: '동일 경로 접미(cb·rs 이후)의 쌍만 연결할 수 있습니다.',
      TH: '동일 경로 접미(cb·rs 이후)의 쌍만 연결할 수 있습니다.'
    },
    '동일 노티 대상명의 CALLBACK·RESULT 쌍만 함께 연결할 수 있습니다.': {
      EN: '동일 notification 대상명의 CALLBACK·RESULT 쌍만 함께 연결할 수 있습니다.',
      JP: '동일 通知 대상명의 CALLBACK·RESULT 쌍만 함께 연결할 수 있습니다.',
      CH: '동일 通知 대상명의 CALLBACK·RESULT 쌍만 함께 연결할 수 있습니다.',
      TH: '동일 การแจ้งเตือน 대상명의 CALLBACK·RESULT 쌍만 함께 연결할 수 있습니다.'
    },
    '동일 총판 산하에서만 상위를 변경할 수 있습니다. 해당 총판 또는 그 하위 조직만 선택하세요.': {
      EN: '동일 master distributor subordinate에서만 상위를 변경할 수 있습니다. 해당 master distributor 또는 그 하위 organization만 선택하세요.',
      JP: '동일 総販 配下에서만 상위를 변경할 수 있습니다. 해당 総販 또는 그 하위 組織만 선택하세요.',
      CH: '동일 总经销 下属에서만 상위를 변경할 수 있습니다. 해당 总经销 또는 그 하위 组织만 선택하세요.',
      TH: '동일 ตัวแทนหลัก ในสังกัด에서만 상위를 변경할 수 있습니다. 해당 ตัวแทนหลัก 또는 그 하위 องค์กร만 선택하세요.'
    },
    '동일 PG·결제구분 조합이 이미 있습니다.': {
      EN: '동일 PG·결제구분 조합이 이미 있습니다.',
      JP: '동일 PG·결제구분 조합이 이미 있습니다.',
      CH: '동일 PG·결제구분 조합이 이미 있습니다.',
      TH: '동일 PG·결제구분 조합이 이미 있습니다.'
    },
    '동일한 이름이 이미 있습니다.': {
      EN: '동일한 이름이 이미 있습니다.',
      JP: '동일한 이름이 이미 있습니다.',
      CH: '동일한 이름이 이미 있습니다.',
      TH: '동일한 이름이 이미 있습니다.'
    },
    '등록 가능 플랜(건수) 값이 올바르지 않습니다.': {
      EN: '등록 가능 플랜(건수) 값이 올바르지 않습니다.',
      JP: '등록 가능 플랜(건수) 값이 올바르지 않습니다.',
      CH: '등록 가능 플랜(건수) 값이 올바르지 않습니다.',
      TH: '등록 가능 플랜(건수) 값이 올바르지 않습니다.'
    },
    '등록 건을 찾을 수 없습니다.': {
      EN: '등록 건 could not be found.',
      JP: '등록 건が見つかりません。',
      CH: '未找到등록 건。',
      TH: 'ไม่พบ등록 건'
    },
    '등록·해지 권한이 없습니다(본사권한설정).': {
      EN: '등록·해지 permission이 없습니다(head officepermissionsetting).',
      JP: '등록·해지 権限이 없습니다(本社権限設定).',
      CH: '등록·해지 权限이 없습니다(总部权限设置).',
      TH: '등록·해지 สิทธิ์이 없습니다(สำนักงานใหญ่สิทธิ์การตั้งค่า).'
    },
    '등록되지 않은 PG사코드입니다. 배포설정 > API연동설정에서 먼저 등록하세요.': {
      EN: '등록되지 않은 PG사code입니다. 배포setting > API연동setting에서 먼저 등록하세요.',
      JP: '등록되지 않은 PG사コード입니다. 배포設定 > API연동設定에서 먼저 등록하세요.',
      CH: '등록되지 않은 PG사代码입니다. 배포设置 > API연동设置에서 먼저 등록하세요.',
      TH: '등록되지 않은 PG사รหัส입니다. 배포การตั้งค่า > API연동การตั้งค่า에서 먼저 등록하세요.'
    },
    '등록된 총본사가 없어 본사를 등록할 수 없습니다.': {
      EN: '등록된 headquarters가 없어 head office를 등록할 수 없습니다.',
      JP: '등록된 総本社가 없어 本社를 등록할 수 없습니다.',
      CH: '등록된 总总部가 없어 总部를 등록할 수 없습니다.',
      TH: '등록된 สำนักงานใหญ่가 없어 สำนักงานใหญ่를 등록할 수 없습니다.'
    },
    '로그인 사용자 정보를 확인할 수 없습니다.': {
      EN: 'login 사용자 information could not be verified.',
      JP: 'ログイン 사용자 情報を確認できません。',
      CH: '无法确认登录 사용자 信息。',
      TH: 'ไม่สามารถยืนยันเข้าสู่ระบบ 사용자 ข้อมูลได้'
    },
    '로그인 정보가 없습니다.': {
      EN: 'login information가 없습니다.',
      JP: 'ログイン 情報가 없습니다.',
      CH: '登录 信息가 없습니다.',
      TH: 'เข้าสู่ระบบ ข้อมูล가 없습니다.'
    },
    '로그인 처리 중 오류가 발생했습니다.': {
      EN: 'login 처리 중 오류가 발생했습니다.',
      JP: 'ログイン 처리 중 오류가 발생했습니다.',
      CH: '登录 처리 중 오류가 발생했습니다.',
      TH: 'เข้าสู่ระบบ 처리 중 오류가 발생했습니다.'
    },
    '로그인이 필요합니다.': {
      EN: 'Login is required.',
      JP: 'ログインが必要です。',
      CH: '需要登录。',
      TH: 'ต้องเข้าสู่ระบบ'
    },
    '로그인ID는 필수입니다.': {
      EN: 'loginID는 필수입니다.',
      JP: 'ログインID는 필수입니다.',
      CH: '登录ID는 필수입니다.',
      TH: 'เข้าสู่ระบบID는 필수입니다.'
    },
    '매핑 JSON 이 비어 있습니다.': {
      EN: '매핑 JSON 이 비어 있습니다.',
      JP: '매핑 JSON 이 비어 있습니다.',
      CH: '매핑 JSON 이 비어 있습니다.',
      TH: '매핑 JSON 이 비어 있습니다.'
    },
    '먼저 이메일 인증을 완료하세요.': {
      EN: '먼저 이메일 authentication을 완료하세요.',
      JP: '먼저 이메일 認証을 완료하세요.',
      CH: '먼저 이메일 认证을 완료하세요.',
      TH: '먼저 이메일 การยืนยันตัวตน을 완료하세요.'
    },
    '먼저 인증 메일을 발송하세요.': {
      EN: '먼저 authentication 메일을 발송하세요.',
      JP: '먼저 認証 메일을 발송하세요.',
      CH: '먼저 认证 메일을 발송하세요.',
      TH: '먼저 การยืนยันตัวตน 메일을 발송하세요.'
    },
    '미사용 조직은 연결 총판으로 선택할 수 없습니다.': {
      EN: '미사용 organization은 연결 master distributor으로 선택할 수 없습니다.',
      JP: '미사용 組織은 연결 総販으로 선택할 수 없습니다.',
      CH: '미사용 组织은 연결 总经销으로 선택할 수 없습니다.',
      TH: '미사용 องค์กร은 연결 ตัวแทนหลัก으로 선택할 수 없습니다.'
    },
    '미수금 건을 찾을 수 없습니다.': {
      EN: '미수금 건 could not be found.',
      JP: '미수금 건が見つかりません。',
      CH: '未找到미수금 건。',
      TH: 'ไม่พบ미수금 건'
    },
    '미수금 수동 등록은 본사권한설정에서 「미수금관리」화면 권한이 수정(M) 이상인 계정만 가능합니다.': {
      EN: '미수금 수동 등록은 head officepermissionsetting에서 「미수금관리」화면 permission이 수정(M) 이상인 account만 가능합니다.',
      JP: '미수금 수동 등록은 本社権限設定에서 「미수금관리」화면 権限이 수정(M) 이상인 アカウント만 가능합니다.',
      CH: '미수금 수동 등록은 总部权限设置에서 「미수금관리」화면 权限이 수정(M) 이상인 账号만 가능합니다.',
      TH: '미수금 수동 등록은 สำนักงานใหญ่สิทธิ์การตั้งค่า에서 「미수금관리」화면 สิทธิ์이 수정(M) 이상인 บัญชี만 가능합니다.'
    },
    '미수금 처리는 본사권한설정에서 「미수금관리」화면 권한이 수정(M) 이상인 계정만 가능합니다.': {
      EN: '미수금 처리는 head officepermissionsetting에서 「미수금관리」화면 permission이 수정(M) 이상인 account만 가능합니다.',
      JP: '미수금 처리는 本社権限設定에서 「미수금관리」화면 権限이 수정(M) 이상인 アカウント만 가능합니다.',
      CH: '미수금 처리는 总部权限设置에서 「미수금관리」화면 权限이 수정(M) 이상인 账号만 가능합니다.',
      TH: '미수금 처리는 สำนักงานใหญ่สิทธิ์การตั้งค่า에서 「미수금관리」화면 สิทธิ์이 수정(M) 이상인 บัญชี만 가능합니다.'
    },
    '미수금을 찾을 수 없습니다.': {
      EN: '미수금 could not be found.',
      JP: '미수금が見つかりません。',
      CH: '未找到미수금。',
      TH: 'ไม่พบ미수금'
    },
    '발신 메일 주소가 비어 있습니다.': {
      EN: '발신 메일 주소가 비어 있습니다.',
      JP: '발신 메일 주소가 비어 있습니다.',
      CH: '발신 메일 주소가 비어 있습니다.',
      TH: '발신 메일 주소가 비어 있습니다.'
    },
    '보조 계정을 찾을 수 없습니다.': {
      EN: '보조 account could not be found.',
      JP: '보조 アカウントが見つかりません。',
      CH: '未找到보조 账号。',
      TH: 'ไม่พบ보조 บัญชี'
    },
    '보조 아이디 비밀번호를 입력하세요.': {
      EN: '보조 아이디 비밀번호를 입력하세요.',
      JP: '보조 아이디 비밀번호를 입력하세요.',
      CH: '보조 아이디 비밀번호를 입력하세요.',
      TH: '보조 아이디 비밀번호를 입력하세요.'
    },
    '보조 아이디 신규 생성 시 비밀번호가 필요합니다.': {
      EN: '보조 아이디 신규 생성 시 비밀번호 is required.',
      JP: '보조 아이디 신규 생성 시 비밀번호が必要です。',
      CH: '需要보조 아이디 신규 생성 시 비밀번호。',
      TH: 'ต้องระบุ보조 아이디 신규 생성 시 비밀번호'
    },
    '본사 「URL 표시통화(THB정산)」 설정이 꺼져 있거나 비어 있습니다.': {
      EN: 'head office 「URL 표시통화(THBsettlement)」 setting이 꺼져 있거나 비어 있습니다.',
      JP: '本社 「URL 표시통화(THB精算)」 設定이 꺼져 있거나 비어 있습니다.',
      CH: '总部 「URL 표시통화(THB结算)」 设置이 꺼져 있거나 비어 있습니다.',
      TH: 'สำนักงานใหญ่ 「URL 표시통화(THBการชำระบัญชี)」 การตั้งค่า이 꺼져 있거나 비어 있습니다.'
    },
    '본사 설정에서 URL 재결제 기능이 꺼져 있습니다.': {
      EN: 'head office setting에서 URL 재결제 기능이 꺼져 있습니다.',
      JP: '本社 設定에서 URL 재결제 기능이 꺼져 있습니다.',
      CH: '总部 设置에서 URL 재결제 기능이 꺼져 있습니다.',
      TH: 'สำนักงานใหญ่ การตั้งค่า에서 URL 재결제 기능이 꺼져 있습니다.'
    },
    '본사 설정에서 URL 재결제 기능이 꺼져 있어 재결제 URL 방식을 선택할 수 없습니다.': {
      EN: 'head office setting에서 URL 재결제 기능이 꺼져 있어 재결제 URL 방식을 선택할 수 없습니다.',
      JP: '本社 設定에서 URL 재결제 기능이 꺼져 있어 재결제 URL 방식을 선택할 수 없습니다.',
      CH: '总部 设置에서 URL 재결제 기능이 꺼져 있어 재결제 URL 방식을 선택할 수 없습니다.',
      TH: 'สำนักงานใหญ่ การตั้งค่า에서 URL 재결제 기능이 꺼져 있어 재결제 URL 방식을 선택할 수 없습니다.'
    },
    '본사 업체코드를 선택하세요.': {
      EN: 'head office company code를 선택하세요.',
      JP: '本社 企業コード를 선택하세요.',
      CH: '总部 企业代码를 선택하세요.',
      TH: 'สำนักงานใหญ่ รหัสบริษัท를 선택하세요.'
    },
    '본사 업체코드와 화면 경로(pageUrl)가 필요합니다.': {
      EN: 'head office company code와 화면 경로(pageUrl) is required.',
      JP: '本社 企業コード와 화면 경로(pageUrl)が必要です。',
      CH: '需要总部 企业代码와 화면 경로(pageUrl)。',
      TH: 'ต้องระบุสำนักงานใหญ่ รหัสบริษัท와 화면 경로(pageUrl)'
    },
    '본사·총판만 도메인 설정 대상입니다.': {
      EN: 'head office·master distributor만 도메인 setting 대상입니다.',
      JP: '本社·総販만 도메인 設定 대상입니다.',
      CH: '总部·总经销만 도메인 设置 대상입니다.',
      TH: 'สำนักงานใหญ่·ตัวแทนหลัก만 도메인 การตั้งค่า 대상입니다.'
    },
    '본사·총판만 도메인을 설정할 수 있습니다.': {
      EN: 'head office·master distributor만 도메인을 setting할 수 있습니다.',
      JP: '本社·総販만 도메인을 設定할 수 있습니다.',
      CH: '总部·总经销만 도메인을 设置할 수 있습니다.',
      TH: 'สำนักงานใหญ่·ตัวแทนหลัก만 도메인을 การตั้งค่า할 수 있습니다.'
    },
    '본사권한설정의 조직 단계별 결제 후속조치 상한에서 이 기능이 꺼져 있습니다.': {
      EN: 'head officepermissionsetting의 organization 단계별 결제 후속조치 상한에서 이 기능이 꺼져 있습니다.',
      JP: '本社権限設定의 組織 단계별 결제 후속조치 상한에서 이 기능이 꺼져 있습니다.',
      CH: '总部权限设置의 组织 단계별 결제 후속조치 상한에서 이 기능이 꺼져 있습니다.',
      TH: 'สำนักงานใหญ่สิทธิ์การตั้งค่า의 องค์กร 단계별 결제 후속조치 상한에서 이 기능이 꺼져 있습니다.'
    },
    '본사는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.': {
      EN: 'head office는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.',
      JP: '本社는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.',
      CH: '总部는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.',
      TH: 'สำนักงานใหญ่는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.'
    },
    '본인 계정은 삭제할 수 없습니다.': {
      EN: '본인 account은 삭제할 수 없습니다.',
      JP: '본인 アカウント은 삭제할 수 없습니다.',
      CH: '본인 账号은 삭제할 수 없습니다.',
      TH: '본인 บัญชี은 삭제할 수 없습니다.'
    },
    '본인 소속 업체 정보는 조회 전용입니다. 변경은 상위 조직·관리자에서 진행하세요.': {
      EN: '본인 소속 company information는 조회 전용입니다. 변경은 상위 organization·administrator에서 진행하세요.',
      JP: '본인 소속 企業 情報는 조회 전용입니다. 변경은 상위 組織·管理者에서 진행하세요.',
      CH: '본인 소속 企业 信息는 조회 전용입니다. 변경은 상위 组织·管理员에서 진행하세요.',
      TH: '본인 소속 บริษัท ข้อมูล는 조회 전용입니다. 변경은 상위 องค์กร·ผู้ดูแลระบบ에서 진행하세요.'
    },
    '본인 소속 업체는 조회만 가능합니다. 변경·업로드는 상위 조직 또는 관리자에서 진행하세요.': {
      EN: '본인 소속 company는 조회만 가능합니다. 변경·업로드는 상위 organization 또는 administrator에서 진행하세요.',
      JP: '본인 소속 企業는 조회만 가능합니다. 변경·업로드는 상위 組織 또는 管理者에서 진행하세요.',
      CH: '본인 소속 企业는 조회만 가능합니다. 변경·업로드는 상위 组织 또는 管理员에서 진행하세요.',
      TH: '본인 소속 บริษัท는 조회만 가능합니다. 변경·업로드는 상위 องค์กร 또는 ผู้ดูแลระบบ에서 진행하세요.'
    },
    '본인 조직의 사용자만 등록할 수 있습니다. 하위 가맹점 사용자는 이 화면에서 등록할 수 없습니다.': {
      EN: '본인 organization의 사용자만 등록할 수 있습니다. 하위 merchant 사용자는 이 화면에서 등록할 수 없습니다.',
      JP: '본인 組織의 사용자만 등록할 수 있습니다. 하위 加盟店 사용자는 이 화면에서 등록할 수 없습니다.',
      CH: '본인 组织의 사용자만 등록할 수 있습니다. 하위 加盟店 사용자는 이 화면에서 등록할 수 없습니다.',
      TH: '본인 องค์กร의 사용자만 등록할 수 있습니다. 하위 ร้านค้า 사용자는 이 화면에서 등록할 수 없습니다.'
    },
    '본인·동일 조직·하위 조직 사용자만 초기화할 수 있습니다. 상위 조직 사용자는 대상이 아닙니다.': {
      EN: '본인·동일 organization·하위 organization 사용자만 초기화할 수 있습니다. 상위 organization 사용자는 대상이 아닙니다.',
      JP: '본인·동일 組織·하위 組織 사용자만 초기화할 수 있습니다. 상위 組織 사용자는 대상이 아닙니다.',
      CH: '본인·동일 组织·하위 组织 사용자만 초기화할 수 있습니다. 상위 组织 사용자는 대상이 아닙니다.',
      TH: '본인·동일 องค์กร·하위 องค์กร 사용자만 초기화할 수 있습니다. 상위 องค์กร 사용자는 대상이 아닙니다.'
    },
    '브랜딩(배경/로고) 변경권한이 없습니다.': {
      EN: '브랜딩(배경/로고) 변경permission이 없습니다.',
      JP: '브랜딩(배경/로고) 변경権限이 없습니다.',
      CH: '브랜딩(배경/로고) 변경权限이 없습니다.',
      TH: '브랜딩(배경/로고) 변경สิทธิ์이 없습니다.'
    },
    '브로커 시크릿이 올바르지 않습니다.': {
      EN: '브로커 시크릿이 올바르지 않습니다.',
      JP: '브로커 시크릿이 올바르지 않습니다.',
      CH: '브로커 시크릿이 올바르지 않습니다.',
      TH: '브로커 시크릿이 올바르지 않습니다.'
    },
    '비밀번호·OTP 초기화는 관리담당(MANAGER) 권한이 있는 사용자만 가능합니다.': {
      EN: '비밀번호·OTP 초기화는 관리담당(MANAGER) permission이 있는 사용자만 가능합니다.',
      JP: '비밀번호·OTP 초기화는 관리담당(MANAGER) 権限이 있는 사용자만 가능합니다.',
      CH: '비밀번호·OTP 초기화는 관리담당(MANAGER) 权限이 있는 사용자만 가능합니다.',
      TH: '비밀번호·OTP 초기화는 관리담당(MANAGER) สิทธิ์이 있는 사용자만 가능합니다.'
    },
    '비밀번호는 필수입니다.': {
      EN: '비밀번호는 필수입니다.',
      JP: '비밀번호는 필수입니다.',
      CH: '비밀번호는 필수입니다.',
      TH: '비밀번호는 필수입니다.'
    },
    '비밀번호를 초기화할 사용자를 찾을 수 없습니다.': {
      EN: '비밀번호를 초기화할 사용자 could not be found.',
      JP: '비밀번호를 초기화할 사용자が見つかりません。',
      CH: '未找到비밀번호를 초기화할 사용자。',
      TH: 'ไม่พบ비밀번호를 초기화할 사용자'
    },
    '비활성화된 계정입니다.': {
      EN: '비활성화된 account입니다.',
      JP: '비활성화된 アカウント입니다.',
      CH: '비활성화된 账号입니다.',
      TH: '비활성화된 บัญชี입니다.'
    },
    '사용 가능한 LLM 설정이 없습니다. 본사 AI설정에서 API 키를 등록하거나, 해당 제공자의 「사용중지」를 해제하세요.': {
      EN: '사용 가능한 LLM setting이 없습니다. head office AIsetting에서 API 키를 등록하거나, 해당 제공자의 「사용중지」를 해제하세요.',
      JP: '사용 가능한 LLM 設定이 없습니다. 本社 AI設定에서 API 키를 등록하거나, 해당 제공자의 「사용중지」를 해제하세요.',
      CH: '사용 가능한 LLM 设置이 없습니다. 总部 AI设置에서 API 키를 등록하거나, 해당 제공자의 「사용중지」를 해제하세요.',
      TH: '사용 가능한 LLM การตั้งค่า이 없습니다. สำนักงานใหญ่ AIการตั้งค่า에서 API 키를 등록하거나, 해당 제공자의 「사용중지」를 해제하세요.'
    },
    '사용(Y)인 경우 연동 용도를 한 가지 이상 지정하세요.': {
      EN: '사용(Y)인 경우 연동 용도를 한 가지 이상 지정하세요.',
      JP: '사용(Y)인 경우 연동 용도를 한 가지 이상 지정하세요.',
      CH: '사용(Y)인 경우 연동 용도를 한 가지 이상 지정하세요.',
      TH: '사용(Y)인 경우 연동 용도를 한 가지 이상 지정하세요.'
    },
    '사용자 아이디가 없어 초기화할 수 없습니다.': {
      EN: '사용자 아이디가 없어 초기화할 수 없습니다.',
      JP: '사용자 아이디가 없어 초기화할 수 없습니다.',
      CH: '사용자 아이디가 없어 초기화할 수 없습니다.',
      TH: '사용자 아이디가 없어 초기화할 수 없습니다.'
    },
    '사용자 정보를 확인할 수 없습니다.': {
      EN: '사용자 information could not be verified.',
      JP: '사용자 情報を確認できません。',
      CH: '无法确认사용자 信息。',
      TH: 'ไม่สามารถยืนยัน사용자 ข้อมูลได้'
    },
    '사용자를 찾을 수 없습니다.': {
      EN: '사용자 could not be found.',
      JP: '사용자が見つかりません。',
      CH: '未找到사용자。',
      TH: 'ไม่พบ사용자'
    },
    '사용자의 소속 업체코드가 없습니다. 사용자관리에서 소속을 지정한 뒤 등록하세요.': {
      EN: '사용자의 소속 company code가 없습니다. 사용자관리에서 소속을 지정한 뒤 등록하세요.',
      JP: '사용자의 소속 企業コード가 없습니다. 사용자관리에서 소속을 지정한 뒤 등록하세요.',
      CH: '사용자의 소속 企业代码가 없습니다. 사용자관리에서 소속을 지정한 뒤 등록하세요.',
      TH: '사용자의 소속 รหัสบริษัท가 없습니다. 사용자관리에서 소속을 지정한 뒤 등록하세요.'
    },
    '사용자ID를 입력하세요.': {
      EN: '사용자ID를 입력하세요.',
      JP: '사용자ID를 입력하세요.',
      CH: '사용자ID를 입력하세요.',
      TH: '사용자ID를 입력하세요.'
    },
    '사용할 수 없는 계정입니다.': {
      EN: '사용할 수 없는 account입니다.',
      JP: '사용할 수 없는 アカウント입니다.',
      CH: '사용할 수 없는 账号입니다.',
      TH: '사용할 수 없는 บัญชี입니다.'
    },
    '삭제할 사용자 ID가 필요합니다.': {
      EN: '삭제할 사용자 ID is required.',
      JP: '삭제할 사용자 IDが必要です。',
      CH: '需要삭제할 사용자 ID。',
      TH: 'ต้องระบุ삭제할 사용자 ID'
    },
    '삭제할 사용자를 찾을 수 없습니다.': {
      EN: '삭제할 사용자 could not be found.',
      JP: '삭제할 사용자が見つかりません。',
      CH: '未找到삭제할 사용자。',
      TH: 'ไม่พบ삭제할 사용자'
    },
    '삭제할 연동이 없습니다.': {
      EN: '삭제할 연동이 없습니다.',
      JP: '삭제할 연동이 없습니다.',
      CH: '삭제할 연동이 없습니다.',
      TH: '삭제할 연동이 없습니다.'
    },
    '삭제할 정책 scope가 올바르지 않습니다.': {
      EN: '삭제할 정책 scope가 올바르지 않습니다.',
      JP: '삭제할 정책 scope가 올바르지 않습니다.',
      CH: '삭제할 정책 scope가 올바르지 않습니다.',
      TH: '삭제할 정책 scope가 올바르지 않습니다.'
    },
    '삭제할 행이 없습니다.': {
      EN: '삭제할 행이 없습니다.',
      JP: '삭제할 행이 없습니다.',
      CH: '삭제할 행이 없습니다.',
      TH: '삭제할 행이 없습니다.'
    },
    '삭제할 ID가 없습니다.': {
      EN: '삭제할 ID가 없습니다.',
      JP: '삭제할 ID가 없습니다.',
      CH: '삭제할 ID가 없습니다.',
      TH: '삭제할 ID가 없습니다.'
    },
    '삭제할 ID가 필요합니다.': {
      EN: '삭제할 ID is required.',
      JP: '삭제할 IDが必要です。',
      CH: '需要삭제할 ID。',
      TH: 'ต้องระบุ삭제할 ID'
    },
    '삭제할 PG사 ID가 필요합니다.': {
      EN: '삭제할 PG사 ID is required.',
      JP: '삭제할 PG사 IDが必要です。',
      CH: '需要삭제할 PG사 ID。',
      TH: 'ต้องระบุ삭제할 PG사 ID'
    },
    '산하 허용 상품 유형이 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE': {
      EN: 'subordinate 허용 상품 유형이 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      JP: '配下 허용 상품 유형이 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      CH: '下属 허용 상품 유형이 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      TH: 'ในสังกัด 허용 상품 유형이 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE'
    },
    '상위 본사 프로필을 찾을 수 없습니다. 본사 등록 상태를 확인하세요.': {
      EN: '상위 head office profile을 찾을 수 없습니다. head office 등록 상태를 확인하세요.',
      JP: '상위 本社 プロフィール을 찾을 수 없습니다. 本社 등록 상태를 확인하세요.',
      CH: '상위 总部 资料을 찾을 수 없습니다. 总部 등록 상태를 확인하세요.',
      TH: '상위 สำนักงานใหญ่ โปรไฟล์을 찾을 수 없습니다. สำนักงานใหญ่ 등록 상태를 확인하세요.'
    },
    '상위 본사에 기준 화폐가 설정되어 있지 않습니다. 본사에서 기준 화폐를 먼저 설정한 뒤 총판을 등록·수정하세요.': {
      EN: '상위 head office에 기준 화폐가 setting되어 있지 않습니다. head office에서 기준 화폐를 먼저 setting한 뒤 master distributor을 등록·수정하세요.',
      JP: '상위 本社에 기준 화폐가 設定되어 있지 않습니다. 本社에서 기준 화폐를 먼저 設定한 뒤 総販을 등록·수정하세요.',
      CH: '상위 总部에 기준 화폐가 设置되어 있지 않습니다. 总部에서 기준 화폐를 먼저 设置한 뒤 总经销을 등록·수정하세요.',
      TH: '상위 สำนักงานใหญ่에 기준 화폐가 การตั้งค่า되어 있지 않습니다. สำนักงานใหญ่에서 기준 화폐를 먼저 การตั้งค่า한 뒤 ตัวแทนหลัก을 등록·수정하세요.'
    },
    '상위업체로 자기 자신을 선택할 수 없습니다.': {
      EN: '상위company로 자기 자신을 선택할 수 없습니다.',
      JP: '상위企業로 자기 자신을 선택할 수 없습니다.',
      CH: '상위企业로 자기 자신을 선택할 수 없습니다.',
      TH: '상위บริษัท로 자기 자신을 선택할 수 없습니다.'
    },
    '상태 불일치 건만 처리할 수 있습니다.': {
      EN: '상태 불일치 건만 처리할 수 있습니다.',
      JP: '상태 불일치 건만 처리할 수 있습니다.',
      CH: '상태 불일치 건만 처리할 수 있습니다.',
      TH: '상태 불일치 건만 처리할 수 있습니다.'
    },
    '상품 유형 코드가 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE': {
      EN: '상품 유형 code가 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      JP: '상품 유형 コード가 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      CH: '상품 유형 代码가 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE',
      TH: '상품 유형 รหัส가 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE'
    },
    '상품 코드 자동 생성에 실패했습니다. 잠시 후 다시 저장하세요.': {
      EN: '상품 code 자동 생성에 실패했습니다. 잠시 후 다시 저장하세요.',
      JP: '상품 コード 자동 생성에 실패했습니다. 잠시 후 다시 저장하세요.',
      CH: '상품 代码 자동 생성에 실패했습니다. 잠시 후 다시 저장하세요.',
      TH: '상품 รหัส 자동 생성에 실패했습니다. 잠시 후 다시 저장하세요.'
    },
    '상품 ID가 필요합니다.': {
      EN: '상품 ID is required.',
      JP: '상품 IDが必要です。',
      CH: '需要상품 ID。',
      TH: 'ต้องระบุ상품 ID'
    },
    '상품명은 필수입니다.': {
      EN: '상품명은 필수입니다.',
      JP: '상품명은 필수입니다.',
      CH: '상품명은 필수입니다.',
      TH: '상품명은 필수입니다.'
    },
    '상품명이 필요합니다.': {
      EN: '상품명이 필요합니다.',
      JP: '상품명이 필요합니다.',
      CH: '상품명이 필요합니다.',
      TH: '상품명이 필요합니다.'
    },
    '상품을 찾을 수 없습니다.': {
      EN: '상품 could not be found.',
      JP: '상품が見つかりません。',
      CH: '未找到상품。',
      TH: 'ไม่พบ상품'
    },
    '새 로그인ID를 입력하세요.': {
      EN: 'Enter a new login ID.',
      JP: '新しいログインIDを入力してください。',
      CH: '请输入新登录 ID。',
      TH: 'กรุณากรอกรหัสเข้าใช้ใหม่'
    },
    '새 비밀번호는 8자 이상이어야 합니다.': {
      EN: '새 비밀번호는 8자 이상이어야 합니다.',
      JP: '새 비밀번호는 8자 이상이어야 합니다.',
      CH: '새 비밀번호는 8자 이상이어야 합니다.',
      TH: '새 비밀번호는 8자 이상이어야 합니다.'
    },
    '새 비밀번호를 입력하세요.': {
      EN: '새 비밀번호를 입력하세요.',
      JP: '새 비밀번호를 입력하세요.',
      CH: '새 비밀번호를 입력하세요.',
      TH: '새 비밀번호를 입력하세요.'
    },
    '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.': {
      EN: '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.',
      JP: '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.',
      CH: '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.',
      TH: '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.'
    },
    '서로 다른 정산주기는 최소 2개 이상 지정하세요.': {
      EN: '서로 다른 settlement주기는 최소 2개 이상 지정하세요.',
      JP: '서로 다른 精算주기는 최소 2개 이상 지정하세요.',
      CH: '서로 다른 结算주기는 최소 2개 이상 지정하세요.',
      TH: '서로 다른 การชำระบัญชี주기는 최소 2개 이상 지정하세요.'
    },
    '서비스가 중지된 업체입니다.': {
      EN: '서비스가 중지된 company입니다.',
      JP: '서비스가 중지된 企業입니다.',
      CH: '서비스가 중지된 企业입니다.',
      TH: '서비스가 중지된 บริษัท입니다.'
    },
    '서비스가 중지된 업체입니다. (미사용 또는 상위 조직 미사용)': {
      EN: '서비스가 중지된 company입니다. (미사용 또는 상위 organization 미사용)',
      JP: '서비스가 중지된 企業입니다. (미사용 또는 상위 組織 미사용)',
      CH: '서비스가 중지된 企业입니다. (미사용 또는 상위 组织 미사용)',
      TH: '서비스가 중지된 บริษัท입니다. (미사용 또는 상위 องค์กร 미사용)'
    },
    '선택한 가맹점에 대한 등록 권한이 없습니다.': {
      EN: 'You do not have 선택한 merchant에 대한 등록 permission.',
      JP: '선택한 加盟店에 대한 등록権限がありません。',
      CH: '无선택한 加盟店에 대한 등록权限。',
      TH: 'ไม่มีสิทธิ์선택한 ร้านค้า에 대한 등록'
    },
    '선택한 가맹점에 대한 차감 권한이 없습니다.': {
      EN: 'You do not have 선택한 merchant에 대한 차감 permission.',
      JP: '선택한 加盟店에 대한 차감権限がありません。',
      CH: '无선택한 加盟店에 대한 차감权限。',
      TH: 'ไม่มีสิทธิ์선택한 ร้านค้า에 대한 차감'
    },
    '세션 orderNo와 요청 orderNo가 일치하지 않습니다.': {
      EN: '세션 orderNo와 요청 orderNo가 일치하지 않습니다.',
      JP: '세션 orderNo와 요청 orderNo가 일치하지 않습니다.',
      CH: '세션 orderNo와 요청 orderNo가 일치하지 않습니다.',
      TH: '세션 orderNo와 요청 orderNo가 일치하지 않습니다.'
    },
    '세션이 유효하지 않거나 만료되었습니다.': {
      EN: '세션이 유효하지 않거나 만료되었습니다.',
      JP: '세션이 유효하지 않거나 만료되었습니다.',
      CH: '세션이 유효하지 않거나 만료되었습니다.',
      TH: '세션이 유효하지 않거나 만료되었습니다.'
    },
    '소속 가맹점 거래만 후속조치할 수 있습니다.': {
      EN: '소속 merchant transaction만 후속조치할 수 있습니다.',
      JP: '소속 加盟店 取引만 후속조치할 수 있습니다.',
      CH: '소속 加盟店 交易만 후속조치할 수 있습니다.',
      TH: '소속 ร้านค้า ธุรกรรม만 후속조치할 수 있습니다.'
    },
    '소속 가맹점이 일치하지 않습니다.': {
      EN: '소속 merchant이 일치하지 않습니다.',
      JP: '소속 加盟店이 일치하지 않습니다.',
      CH: '소속 加盟店이 일치하지 않습니다.',
      TH: '소속 ร้านค้า이 일치하지 않습니다.'
    },
    '소속 업체 및 하위 가맹점 거래만 후속조치할 수 있습니다.': {
      EN: '소속 company 및 하위 merchant transaction만 후속조치할 수 있습니다.',
      JP: '소속 企業 및 하위 加盟店 取引만 후속조치할 수 있습니다.',
      CH: '소속 企业 및 하위 加盟店 交易만 후속조치할 수 있습니다.',
      TH: '소속 บริษัท 및 하위 ร้านค้า ธุรกรรม만 후속조치할 수 있습니다.'
    },
    '소속 업체 및 하위 가맹점만 조회할 수 있습니다.': {
      EN: 'Only 소속 company 및 하위 merchant can view this.',
      JP: '소속 企業 및 하위 加盟店のみ参照できます。',
      CH: '仅소속 企业 및 하위 加盟店可查看。',
      TH: 'เฉพาะ소속 บริษัท 및 하위 ร้านค้าเท่านั้นที่ดูได้'
    },
    '소속 업체 및 하위 업체만 수정할 수 있습니다.': {
      EN: 'Only 소속 company 및 하위 company can modify.',
      JP: '소속 企業 및 하위 企業のみ修正できます。',
      CH: '仅소속 企业 및 하위 企业可修改。',
      TH: 'เฉพาะ소속 บริษัท 및 하위 บริษัทเท่านั้นที่แก้ไขได้'
    },
    '소속 업체 및 하위 업체만 조회할 수 있습니다.': {
      EN: 'Only 소속 company 및 하위 company can view this.',
      JP: '소속 企業 및 하위 企業のみ参照できます。',
      CH: '仅소속 企业 및 하위 企业可查看。',
      TH: 'เฉพาะ소속 บริษัท 및 하위 บริษัทเท่านั้นที่ดูได้'
    },
    '소속 업체 및 하위 업체에 대해서만 비밀번호를 초기화할 수 있습니다.': {
      EN: '소속 company 및 하위 company에 대해서만 비밀번호를 초기화할 수 있습니다.',
      JP: '소속 企業 및 하위 企業에 대해서만 비밀번호를 초기화할 수 있습니다.',
      CH: '소속 企业 및 하위 企业에 대해서만 비밀번호를 초기화할 수 있습니다.',
      TH: '소속 บริษัท 및 하위 บริษัท에 대해서만 비밀번호를 초기화할 수 있습니다.'
    },
    '소속 하위 조직을 상위로 지정할 수 없습니다.': {
      EN: '소속 하위 organization을 상위로 지정할 수 없습니다.',
      JP: '소속 하위 組織을 상위로 지정할 수 없습니다.',
      CH: '소속 하위 组织을 상위로 지정할 수 없습니다.',
      TH: '소속 하위 องค์กร을 상위로 지정할 수 없습니다.'
    },
    '수신 이메일이 비어 있습니다.': {
      EN: '수신 이메일이 비어 있습니다.',
      JP: '수신 이메일이 비어 있습니다.',
      CH: '수신 이메일이 비어 있습니다.',
      TH: '수신 이메일이 비어 있습니다.'
    },
    '수정할 규칙을 찾을 수 없습니다.': {
      EN: '수정할 규칙 could not be found.',
      JP: '수정할 규칙が見つかりません。',
      CH: '未找到수정할 규칙。',
      TH: 'ไม่พบ수정할 규칙'
    },
    '수정할 사용자 ID가 필요합니다.': {
      EN: '수정할 사용자 ID is required.',
      JP: '수정할 사용자 IDが必要です。',
      CH: '需要수정할 사용자 ID。',
      TH: 'ต้องระบุ수정할 사용자 ID'
    },
    '수정할 사용자를 찾을 수 없습니다.': {
      EN: '수정할 사용자 could not be found.',
      JP: '수정할 사용자が見つかりません。',
      CH: '未找到수정할 사용자。',
      TH: 'ไม่พบ수정할 사용자'
    },
    '숙박 기간이 올바르지 않습니다. 체크인·체크아웃을 확인하세요.': {
      EN: '숙박 기간이 올바르지 않습니다. 체크인·체크아웃을 확인하세요.',
      JP: '숙박 기간이 올바르지 않습니다. 체크인·체크아웃을 확인하세요.',
      CH: '숙박 기간이 올바르지 않습니다. 체크인·체크아웃을 확인하세요.',
      TH: '숙박 기간이 올바르지 않습니다. 체크인·체크아웃을 확인하세요.'
    },
    '승인(결제) 완료 건만 후속조치할 수 있습니다.': {
      EN: '승인(결제) 완료 건만 후속조치할 수 있습니다.',
      JP: '승인(결제) 완료 건만 후속조치할 수 있습니다.',
      CH: '승인(결제) 완료 건만 후속조치할 수 있습니다.',
      TH: '승인(결제) 완료 건만 후속조치할 수 있습니다.'
    },
    '승인번호(approvalNo)는 필수입니다.': {
      EN: '승인번호(approvalNo)는 필수입니다.',
      JP: '승인번호(approvalNo)는 필수입니다.',
      CH: '승인번호(approvalNo)는 필수입니다.',
      TH: '승인번호(approvalNo)는 필수입니다.'
    },
    '신규 PG 연동은 연동용도를 선택해야 합니다. (PG코드는 용도별로 나누어 등록하세요)': {
      EN: '신규 PG 연동은 연동용도를 선택해야 합니다. (PGcode는 용도별로 나누어 등록하세요)',
      JP: '신규 PG 연동은 연동용도를 선택해야 합니다. (PGコード는 용도별로 나누어 등록하세요)',
      CH: '신규 PG 연동은 연동용도를 선택해야 합니다. (PG代码는 용도별로 나누어 등록하세요)',
      TH: '신규 PG 연동은 연동용도를 선택해야 합니다. (PGรหัส는 용도별로 나누어 등록하세요)'
    },
    '아이디 또는 비밀번호가 올바르지 않습니다.': {
      EN: '아이디 또는 비밀번호가 올바르지 않습니다.',
      JP: '아이디 또는 비밀번호가 올바르지 않습니다.',
      CH: '아이디 또는 비밀번호가 올바르지 않습니다.',
      TH: '아이디 또는 비밀번호가 올바르지 않습니다.'
    },
    '아이디+1! 형태의 초기 비밀번호는 새 비밀번호로 사용할 수 없습니다.': {
      EN: '아이디+1! 형태의 초기 비밀번호는 새 비밀번호로 사용할 수 없습니다.',
      JP: '아이디+1! 형태의 초기 비밀번호는 새 비밀번호로 사용할 수 없습니다.',
      CH: '아이디+1! 형태의 초기 비밀번호는 새 비밀번호로 사용할 수 없습니다.',
      TH: '아이디+1! 형태의 초기 비밀번호는 새 비밀번호로 사용할 수 없습니다.'
    },
    '알 수 없는 사용여부입니다.': {
      EN: '알 수 없는 사용여부입니다.',
      JP: '알 수 없는 사용여부입니다.',
      CH: '알 수 없는 사용여부입니다.',
      TH: '알 수 없는 사용여부입니다.'
    },
    '업로드 권한이 없습니다.': {
      EN: 'You do not have 업로드 permission.',
      JP: '업로드権限がありません。',
      CH: '无업로드权限。',
      TH: 'ไม่มีสิทธิ์업로드'
    },
    '업로드 이미지는 40MB 이하여야 합니다.': {
      EN: '업로드 이미지는 40MB 이하여야 합니다.',
      JP: '업로드 이미지는 40MB 이하여야 합니다.',
      CH: '업로드 이미지는 40MB 이하여야 합니다.',
      TH: '업로드 이미지는 40MB 이하여야 합니다.'
    },
    '업체 또는 정산설정을 찾을 수 없습니다.': {
      EN: 'company 또는 settlementsetting could not be found.',
      JP: '企業 또는 精算設定が見つかりません。',
      CH: '未找到企业 또는 结算设置。',
      TH: 'ไม่พบบริษัท 또는 การชำระบัญชีการตั้งค่า'
    },
    '업체 정보가 없습니다.': {
      EN: 'company information가 없습니다.',
      JP: '企業 情報가 없습니다.',
      CH: '企业 信息가 없습니다.',
      TH: 'บริษัท ข้อมูล가 없습니다.'
    },
    '업체 코드(compId)는 필수입니다.': {
      EN: 'company code(compId)는 필수입니다.',
      JP: '企業 コード(compId)는 필수입니다.',
      CH: '企业 代码(compId)는 필수입니다.',
      TH: 'บริษัท รหัส(compId)는 필수입니다.'
    },
    '업체 코드를 찾을 수 없습니다.': {
      EN: 'company code could not be found.',
      JP: '企業 コードが見つかりません。',
      CH: '未找到企业 代码。',
      TH: 'ไม่พบบริษัท รหัส'
    },
    '업체를 찾을 수 없거나 대표 로그인ID가 없습니다.': {
      EN: 'company를 찾을 수 없거나 대표 loginID가 없습니다.',
      JP: '企業를 찾을 수 없거나 대표 ログインID가 없습니다.',
      CH: '企业를 찾을 수 없거나 대표 登录ID가 없습니다.',
      TH: 'บริษัท를 찾을 수 없거나 대표 เข้าสู่ระบบID가 없습니다.'
    },
    '업체를 찾을 수 없습니다.': {
      EN: 'company could not be found.',
      JP: '企業が見つかりません。',
      CH: '未找到企业。',
      TH: 'ไม่พบบริษัท'
    },
    '업체접근설정은 총본사·본사·총판 소속 계정만 등록할 수 있습니다.': {
      EN: 'Only company접근setting은 headquarters·head office·master distributor 소속 account can register.',
      JP: '企業접근設定은 総本社·本社·総販 소속 アカウントのみ登録できます。',
      CH: '仅企业접근设置은 总总部·总部·总经销 소속 账号可注册。',
      TH: 'เฉพาะบริษัท접근การตั้งค่า은 สำนักงานใหญ่·สำนักงานใหญ่·ตัวแทนหลัก 소속 บัญชีเท่านั้นที่ลงทะเบียนได้'
    },
    '업체코드(compId)는 필수입니다.': {
      EN: 'company code(compId)는 필수입니다.',
      JP: '企業コード(compId)는 필수입니다.',
      CH: '企业代码(compId)는 필수입니다.',
      TH: 'รหัสบริษัท(compId)는 필수입니다.'
    },
    '엑셀 파일을 선택하세요.': {
      EN: '엑셀 file을 선택하세요.',
      JP: '엑셀 ファイル을 선택하세요.',
      CH: '엑셀 文件을 선택하세요.',
      TH: '엑셀 ไฟล์을 선택하세요.'
    },
    '연결 총판 조직을 찾을 수 없습니다.': {
      EN: '연결 master distributor organization could not be found.',
      JP: '연결 総販 組織が見つかりません。',
      CH: '未找到연결 总经销 组织。',
      TH: 'ไม่พบ연결 ตัวแทนหลัก องค์กร'
    },
    '연결 총판은 총판(MASTER_DIST)만 선택할 수 있습니다.': {
      EN: '연결 master distributor은 master distributor (MASTER_DIST)만 선택할 수 있습니다.',
      JP: '연결 総販은 総販（MASTER_DIST）만 선택할 수 있습니다.',
      CH: '연결 总经销은 总经销（MASTER_DIST）만 선택할 수 있습니다.',
      TH: '연결 ตัวแทนหลัก은 ตัวแทนหลัก (MASTER_DIST)만 선택할 수 있습니다.'
    },
    '연동 정보를 찾을 수 없습니다.': {
      EN: '연동 information could not be found.',
      JP: '연동 情報が見つかりません。',
      CH: '未找到연동 信息。',
      TH: 'ไม่พบ연동 ข้อมูล'
    },
    '연동용도는 NOTI, URL_PAY, URL_PAY_REPAY, WEB_CHATBOT, API, API_SUBSCRIPTION 중 하나여야 합니다.': {
      EN: '연동용도는 NOTI, URL_PAY, URL_PAY_REPAY, WEB_CHATBOT, API, API_SUBSCRIPTION 중 하나여야 합니다.',
      JP: '연동용도는 NOTI, URL_PAY, URL_PAY_REPAY, WEB_CHATBOT, API, API_SUBSCRIPTION 중 하나여야 합니다.',
      CH: '연동용도는 NOTI, URL_PAY, URL_PAY_REPAY, WEB_CHATBOT, API, API_SUBSCRIPTION 중 하나여야 합니다.',
      TH: '연동용도는 NOTI, URL_PAY, URL_PAY_REPAY, WEB_CHATBOT, API, API_SUBSCRIPTION 중 하나여야 합니다.'
    },
    '연락처(전화)를 입력하세요.': {
      EN: '연락처(전화)를 입력하세요.',
      JP: '연락처(전화)를 입력하세요.',
      CH: '연락처(전화)를 입력하세요.',
      TH: '연락처(전화)를 입력하세요.'
    },
    '영업일 설정은 총본사(또는 시스템 관리자)만 변경할 수 있습니다.': {
      EN: '영업일 setting은 headquarters(또는 system administrator)만 변경할 수 있습니다.',
      JP: '영업일 設定은 総本社(또는 システム管理者)만 변경할 수 있습니다.',
      CH: '영업일 设置은 总总部(또는 系统管理员)만 변경할 수 있습니다.',
      TH: '영업일 การตั้งค่า은 สำนักงานใหญ่(또는 ผู้ดูแลระบบ)만 변경할 수 있습니다.'
    },
    '영업일 설정은 총본사(또는 시스템 관리자)만 저장할 수 있습니다.': {
      EN: 'Only 영업일 setting은 headquarters(또는 system administrator) can save.',
      JP: '영업일 設定은 総本社(또는 システム管理者)のみ保存できます。',
      CH: '仅영업일 设置은 总总部(또는 系统管理员)可保存。',
      TH: 'เฉพาะ영업일 การตั้งค่า은 สำนักงานใหญ่(또는 ผู้ดูแลระบบ)เท่านั้นที่บันทึกได้'
    },
    '예약·방문 인원(1~999명)을 입력하세요.': {
      EN: '예약·방문 인원(1~999명)을 입력하세요.',
      JP: '예약·방문 인원(1~999명)을 입력하세요.',
      CH: '예약·방문 인원(1~999명)을 입력하세요.',
      TH: '예약·방문 인원(1~999명)을 입력하세요.'
    },
    '예약금 형식이 올바르지 않습니다.': {
      EN: 'Invalid 예약금 format.',
      JP: '예약금の形式が正しくありません。',
      CH: '예약금格式不正确。',
      TH: 'รูปแบบ예약금ไม่ถูกต้อง'
    },
    '예약금(DEPOSIT) 모드에는 예약금액을 0보다 크게 입력하세요.': {
      EN: '예약금(DEPOSIT) 모드에는 예약amount을 0보다 크게 입력하세요.',
      JP: '예약금(DEPOSIT) 모드에는 예약金額을 0보다 크게 입력하세요.',
      CH: '예약금(DEPOSIT) 모드에는 예약金额을 0보다 크게 입력하세요.',
      TH: '예약금(DEPOSIT) 모드에는 예약จำนวนเงิน을 0보다 크게 입력하세요.'
    },
    '예약금은 상품 금액(전체)보다 작아야 합니다.': {
      EN: '예약금은 상품 amount(전체)보다 작아야 합니다.',
      JP: '예약금은 상품 金額(전체)보다 작아야 합니다.',
      CH: '예약금은 상품 金额(전체)보다 작아야 합니다.',
      TH: '예약금은 상품 จำนวนเงิน(전체)보다 작아야 합니다.'
    },
    '요청 본문이 없습니다.': {
      EN: '요청 본문이 없습니다.',
      JP: '요청 본문이 없습니다.',
      CH: '요청 본문이 없습니다.',
      TH: '요청 본문이 없습니다.'
    },
    '운영 보류 중입니다. 상품 주문·예약·결제는 접수되지 않습니다.': {
      EN: '운영 보류 중입니다. 상품 주문·예약·결제는 접수되지 않습니다.',
      JP: '운영 보류 중입니다. 상품 주문·예약·결제는 접수되지 않습니다.',
      CH: '운영 보류 중입니다. 상품 주문·예약·결제는 접수되지 않습니다.',
      TH: '운영 보류 중입니다. 상품 주문·예약·결제는 접수되지 않습니다.'
    },
    '운영 보류 중입니다. 상품 주문·예약은 접수되지 않습니다.': {
      EN: '운영 보류 중입니다. 상품 주문·예약은 접수되지 않습니다.',
      JP: '운영 보류 중입니다. 상품 주문·예약은 접수되지 않습니다.',
      CH: '운영 보류 중입니다. 상품 주문·예약은 접수되지 않습니다.',
      TH: '운영 보류 중입니다. 상품 주문·예약은 접수되지 않습니다.'
    },
    '원문이 길이 제한으로 잘린 건은 재처리할 수 없습니다.': {
      EN: '원문이 길이 제한으로 잘린 건은 재처리할 수 없습니다.',
      JP: '원문이 길이 제한으로 잘린 건은 재처리할 수 없습니다.',
      CH: '원문이 길이 제한으로 잘린 건은 재처리할 수 없습니다.',
      TH: '원문이 길이 제한으로 잘린 건은 재처리할 수 없습니다.'
    },
    '웹결제 경고메세지는 200자 이하여야 합니다.': {
      EN: '웹결제 경고메세지는 200자 이하여야 합니다.',
      JP: '웹결제 경고메세지는 200자 이하여야 합니다.',
      CH: '웹결제 경고메세지는 200자 이하여야 합니다.',
      TH: '웹결제 경고메세지는 200자 이하여야 합니다.'
    },
    '웹결제 상단 로고 URL은 500자 이하여야 합니다.': {
      EN: '웹결제 상단 로고 URL은 500자 이하여야 합니다.',
      JP: '웹결제 상단 로고 URL은 500자 이하여야 합니다.',
      CH: '웹결제 상단 로고 URL은 500자 이하여야 합니다.',
      TH: '웹결제 상단 로고 URL은 500자 이하여야 합니다.'
    },
    '유형(일/주/WK)을 선택하세요.': {
      EN: '유형(일/주/WK)을 선택하세요.',
      JP: '유형(일/주/WK)을 선택하세요.',
      CH: '유형(일/주/WK)을 선택하세요.',
      TH: '유형(일/주/WK)을 선택하세요.'
    },
    '유효한 결제 금액을 입력하세요.': {
      EN: '유효한 결제 amount을 입력하세요.',
      JP: '유효한 결제 金額을 입력하세요.',
      CH: '유효한 결제 金额을 입력하세요.',
      TH: '유효한 결제 จำนวนเงิน을 입력하세요.'
    },
    '유효한 업체코드가 아닙니다.': {
      EN: '유효한 company code가 아닙니다.',
      JP: '유효한 企業コード가 아닙니다.',
      CH: '유효한 企业代码가 아닙니다.',
      TH: '유효한 รหัสบริษัท가 아닙니다.'
    },
    '유효한 이메일을 입력하세요.': {
      EN: '유효한 이메일을 입력하세요.',
      JP: '유효한 이메일을 입력하세요.',
      CH: '유효한 이메일을 입력하세요.',
      TH: '유효한 이메일을 입력하세요.'
    },
    '이 가맹점 URL 결제는 표시통화(THB정산) 모드가 아닙니다.': {
      EN: '이 merchant URL 결제는 표시통화(THBsettlement) 모드가 아닙니다.',
      JP: '이 加盟店 URL 결제는 표시통화(THB精算) 모드가 아닙니다.',
      CH: '이 加盟店 URL 결제는 표시통화(THB结算) 모드가 아닙니다.',
      TH: '이 ร้านค้า URL 결제는 표시통화(THBการชำระบัญชี) 모드가 아닙니다.'
    },
    '이 가맹점은 웹결제(URL 결제)가 미사용으로 설정되어 있습니다.': {
      EN: '이 merchant은 웹결제(URL 결제)가 미사용으로 setting되어 있습니다.',
      JP: '이 加盟店은 웹결제(URL 결제)가 미사용으로 設定되어 있습니다.',
      CH: '이 加盟店은 웹결제(URL 결제)가 미사용으로 设置되어 있습니다.',
      TH: '이 ร้านค้า은 웹결제(URL 결제)가 미사용으로 การตั้งค่า되어 있습니다.'
    },
    '이 가맹점의 챗봇 관리자 계정이 아닙니다.': {
      EN: '이 merchant의 챗봇 administrator account이 아닙니다.',
      JP: '이 加盟店의 챗봇 管理者 アカウント이 아닙니다.',
      CH: '이 加盟店의 챗봇 管理员 账号이 아닙니다.',
      TH: '이 ร้านค้า의 챗봇 ผู้ดูแลระบบ บัญชี이 아닙니다.'
    },
    '이 계정은 Google OTP 등록 대상이 아닙니다.': {
      EN: '이 account은 Google OTP 등록 대상이 아닙니다.',
      JP: '이 アカウント은 Google OTP 등록 대상이 아닙니다.',
      CH: '이 账号은 Google OTP 등록 대상이 아닙니다.',
      TH: '이 บัญชี은 Google OTP 등록 대상이 아닙니다.'
    },
    '이 로그인ID는 이미 다른 가맹점의 챗봇 관리자로 등록되어 있습니다.': {
      EN: '이 loginID는 이미 다른 merchant의 챗봇 administrator로 등록되어 있습니다.',
      JP: '이 ログインID는 이미 다른 加盟店의 챗봇 管理者로 등록되어 있습니다.',
      CH: '이 登录ID는 이미 다른 加盟店의 챗봇 管理员로 등록되어 있습니다.',
      TH: '이 เข้าสู่ระบบID는 이미 다른 ร้านค้า의 챗봇 ผู้ดูแลระบบ로 등록되어 있습니다.'
    },
    '이 메뉴를 열 권한이 없습니다.': {
      EN: 'You do not have 이 메뉴를 열 permission.',
      JP: '이 메뉴를 열権限がありません。',
      CH: '无이 메뉴를 열权限。',
      TH: 'ไม่มีสิทธิ์이 메뉴를 열'
    },
    '이 상품의 예약금 설정을 확인하세요.': {
      EN: '이 상품의 예약금 setting을 확인하세요.',
      JP: '이 상품의 예약금 設定을 확인하세요.',
      CH: '이 상품의 예약금 设置을 확인하세요.',
      TH: '이 상품의 예약금 การตั้งค่า을 확인하세요.'
    },
    '이 차지백 정책을 사용 중인 수수료 템플릿이 있어 삭제할 수 없습니다.': {
      EN: '이 차지백 정책을 사용 중인 수수료 템플릿이 있어 삭제할 수 없습니다.',
      JP: '이 차지백 정책을 사용 중인 수수료 템플릿이 있어 삭제할 수 없습니다.',
      CH: '이 차지백 정책을 사용 중인 수수료 템플릿이 있어 삭제할 수 없습니다.',
      TH: '이 차지백 정책을 사용 중인 수수료 템플릿이 있어 삭제할 수 없습니다.'
    },
    '이름은 필수입니다.': {
      EN: '이름은 필수입니다.',
      JP: '이름은 필수입니다.',
      CH: '이름은 필수입니다.',
      TH: '이름은 필수입니다.'
    },
    '이메일 인증번호가 올바르지 않습니다.': {
      EN: '이메일 authentication번호가 올바르지 않습니다.',
      JP: '이메일 認証번호가 올바르지 않습니다.',
      CH: '이메일 认证번호가 올바르지 않습니다.',
      TH: '이메일 การยืนยันตัวตน번호가 올바르지 않습니다.'
    },
    '이메일무효 마감 시각이 올바르지 않습니다(0:00~23:59).': {
      EN: '이메일무효 마감 시각이 올바르지 않습니다(0:00~23:59).',
      JP: '이메일무효 마감 시각이 올바르지 않습니다(0:00~23:59).',
      CH: '이메일무효 마감 시각이 올바르지 않습니다(0:00~23:59).',
      TH: '이메일무효 마감 시각이 올바르지 않습니다(0:00~23:59).'
    },
    '이메일무효 마감은 자동무효 마감 다음 분 이후(또는 동일)여야 합니다.': {
      EN: '이메일무효 마감은 자동무효 마감 다음 분 이후(또는 동일)여야 합니다.',
      JP: '이메일무효 마감은 자동무효 마감 다음 분 이후(또는 동일)여야 합니다.',
      CH: '이메일무효 마감은 자동무효 마감 다음 분 이후(또는 동일)여야 합니다.',
      TH: '이메일무효 마감은 자동무효 마감 다음 분 이후(또는 동일)여야 합니다.'
    },
    '이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).': {
      EN: '이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).',
      JP: '이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).',
      CH: '이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).',
      TH: '이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).'
    },
    '이메일무효 시작은 마감 시각과 같거나 이전이어야 합니다.': {
      EN: '이메일무효 시작은 마감 시각과 같거나 이전이어야 합니다.',
      JP: '이메일무효 시작은 마감 시각과 같거나 이전이어야 합니다.',
      CH: '이메일무효 시작은 마감 시각과 같거나 이전이어야 합니다.',
      TH: '이메일무효 시작은 마감 시각과 같거나 이전이어야 합니다.'
    },
    '이메일무효 시작은 자동무효 마감 다음 분 이후여야 합니다. 자동무효·이메일무효를 함께 쓰는 경우 시작 입력은 비우면 자동무효 직후부터 적용됩니다.': {
      EN: '이메일무효 시작은 자동무효 마감 다음 분 이후여야 합니다. 자동무효·이메일무효를 함께 쓰는 경우 시작 입력은 비우면 자동무효 직후부터 적용됩니다.',
      JP: '이메일무효 시작은 자동무효 마감 다음 분 이후여야 합니다. 자동무효·이메일무효를 함께 쓰는 경우 시작 입력은 비우면 자동무효 직후부터 적용됩니다.',
      CH: '이메일무효 시작은 자동무효 마감 다음 분 이후여야 합니다. 자동무효·이메일무효를 함께 쓰는 경우 시작 입력은 비우면 자동무효 직후부터 적용됩니다.',
      TH: '이메일무효 시작은 자동무효 마감 다음 분 이후여야 합니다. 자동무효·이메일무효를 함께 쓰는 경우 시작 입력은 비우면 자동무효 직후부터 적용됩니다.'
    },
    '이메일무효는 승인일(기준 Zone) 당일, 설정한 시작~마감 시각 안에서만 가능합니다.': {
      EN: '이메일무효는 승인일(기준 Zone) 당일, setting한 시작~마감 시각 안에서만 가능합니다.',
      JP: '이메일무효는 승인일(기준 Zone) 당일, 設定한 시작~마감 시각 안에서만 가능합니다.',
      CH: '이메일무효는 승인일(기준 Zone) 당일, 设置한 시작~마감 시각 안에서만 가능합니다.',
      TH: '이메일무효는 승인일(기준 Zone) 당일, การตั้งค่า한 시작~마감 시각 안에서만 가능합니다.'
    },
    '이미 동일한 접근 규칙이 있습니다.': {
      EN: '이미 동일한 접근 규칙이 있습니다.',
      JP: '이미 동일한 접근 규칙이 있습니다.',
      CH: '이미 동일한 접근 규칙이 있습니다.',
      TH: '이미 동일한 접근 규칙이 있습니다.'
    },
    '이미 등록된 PG사코드입니다.': {
      EN: '이미 등록된 PG사code입니다.',
      JP: '이미 등록된 PG사コード입니다.',
      CH: '이미 등록된 PG사代码입니다.',
      TH: '이미 등록된 PG사รหัส입니다.'
    },
    '이미 사용 중인 로그인ID입니다. 다른 가맹점 소속 계정이거나 사용자관리 등에 등록된 ID는 지정할 수 없습니다.': {
      EN: '이미 사용 중인 loginID입니다. 다른 merchant 소속 account이거나 사용자관리 등에 등록된 ID는 지정할 수 없습니다.',
      JP: '이미 사용 중인 ログインID입니다. 다른 加盟店 소속 アカウント이거나 사용자관리 등에 등록된 ID는 지정할 수 없습니다.',
      CH: '이미 사용 중인 登录ID입니다. 다른 加盟店 소속 账号이거나 사용자관리 등에 등록된 ID는 지정할 수 없습니다.',
      TH: '이미 사용 중인 เข้าสู่ระบบID입니다. 다른 ร้านค้า 소속 บัญชี이거나 사용자관리 등에 등록된 ID는 지정할 수 없습니다.'
    },
    '이미 존재하는 사용자ID입니다.': {
      EN: '이미 존재하는 사용자ID입니다.',
      JP: '이미 존재하는 사용자ID입니다.',
      CH: '이미 존재하는 사용자ID입니다.',
      TH: '이미 존재하는 사용자ID입니다.'
    },
    '이미 존재하는 정책코드입니다.': {
      EN: '이미 존재하는 정책code입니다.',
      JP: '이미 존재하는 정책コード입니다.',
      CH: '이미 존재하는 정책代码입니다.',
      TH: '이미 존재하는 정책รหัส입니다.'
    },
    '이미 해지된 카드입니다.': {
      EN: '이미 해지된 카드입니다.',
      JP: '이미 해지된 카드입니다.',
      CH: '이미 해지된 카드입니다.',
      TH: '이미 해지된 카드입니다.'
    },
    '이미 환수처리 요청이 등록되어 있습니다.': {
      EN: '이미 환수처리 요청이 등록되어 있습니다.',
      JP: '이미 환수처리 요청이 등록되어 있습니다.',
      CH: '이미 환수처리 요청이 등록되어 있습니다.',
      TH: '이미 환수처리 요청이 등록되어 있습니다.'
    },
    '이미지 파일을 읽을 수 없습니다.': {
      EN: '이미지 file을 읽을 수 없습니다.',
      JP: '이미지 ファイル을 읽을 수 없습니다.',
      CH: '이미지 文件을 읽을 수 없습니다.',
      TH: '이미지 ไฟล์을 읽을 수 없습니다.'
    },
    '이미지가 비어 있습니다.': {
      EN: '이미지가 비어 있습니다.',
      JP: '이미지가 비어 있습니다.',
      CH: '이미지가 비어 있습니다.',
      TH: '이미지가 비어 있습니다.'
    },
    '이미지는 2MB 이하여야 합니다.': {
      EN: '이미지는 2MB 이하여야 합니다.',
      JP: '이미지는 2MB 이하여야 합니다.',
      CH: '이미지는 2MB 이하여야 합니다.',
      TH: '이미지는 2MB 이하여야 합니다.'
    },
    '이미지를 읽을 수 없습니다.': {
      EN: '이미지를 읽을 수 없습니다.',
      JP: '이미지를 읽을 수 없습니다.',
      CH: '이미지를 읽을 수 없습니다.',
      TH: '이미지를 읽을 수 없습니다.'
    },
    '이용 시간(분)은 5~2880 범위로 입력하세요.': {
      EN: '이용 시간(분)은 5~2880 범위로 입력하세요.',
      JP: '이용 시간(분)은 5~2880 범위로 입력하세요.',
      CH: '이용 시간(분)은 5~2880 범위로 입력하세요.',
      TH: '이용 시간(분)은 5~2880 범위로 입력하세요.'
    },
    '인증번호가 만료되었습니다. 다시 발송하세요.': {
      EN: 'authentication번호가 만료되었습니다. 다시 발송하세요.',
      JP: '認証번호가 만료되었습니다. 다시 발송하세요.',
      CH: '认证번호가 만료되었습니다. 다시 발송하세요.',
      TH: 'การยืนยันตัวตน번호가 만료되었습니다. 다시 발송하세요.'
    },
    '인증이 필요합니다.': {
      EN: 'Authentication is required.',
      JP: '認証が必要です。',
      CH: '需要认证。',
      TH: 'ต้องยืนยันตัวตน'
    },
    '자동무효 시각이 올바르지 않습니다(0:00~23:59).': {
      EN: '자동무효 시각이 올바르지 않습니다(0:00~23:59).',
      JP: '자동무효 시각이 올바르지 않습니다(0:00~23:59).',
      CH: '자동무효 시각이 올바르지 않습니다(0:00~23:59).',
      TH: '자동무효 시각이 올바르지 않습니다(0:00~23:59).'
    },
    '자동무효: 시작 시각은 마감 시각과 같거나 이전이어야 합니다.': {
      EN: '자동무효: 시작 시각은 마감 시각과 같거나 이전이어야 합니다.',
      JP: '자동무효: 시작 시각은 마감 시각과 같거나 이전이어야 합니다.',
      CH: '자동무효: 시작 시각은 마감 시각과 같거나 이전이어야 합니다.',
      TH: '자동무효: 시작 시각은 마감 시각과 같거나 이전이어야 합니다.'
    },
    '자동무효는 승인일(시간 선택 국가 기준) 당일, 설정한 시작~마감 시각 안에서만 가능합니다.': {
      EN: '자동무효는 승인일(시간 선택 국가 기준) 당일, setting한 시작~마감 시각 안에서만 가능합니다.',
      JP: '자동무효는 승인일(시간 선택 국가 기준) 당일, 設定한 시작~마감 시각 안에서만 가능합니다.',
      CH: '자동무효는 승인일(시간 선택 국가 기준) 당일, 设置한 시작~마감 시각 안에서만 가능합니다.',
      TH: '자동무효는 승인일(시간 선택 국가 기준) 당일, การตั้งค่า한 시작~마감 시각 안에서만 가능합니다.'
    },
    '자동환불 처리 가능 기간이 아닙니다. (태국 기준 결제일 익일 설정 시각부터 환불 가능 일수 확인)': {
      EN: '자동환불 처리 가능 기간이 아닙니다. (태국 기준 결제일 익일 setting 시각부터 환불 가능 일수 확인)',
      JP: '자동환불 처리 가능 기간이 아닙니다. (태국 기준 결제일 익일 設定 시각부터 환불 가능 일수 확인)',
      CH: '자동환불 처리 가능 기간이 아닙니다. (태국 기준 결제일 익일 设置 시각부터 환불 가능 일수 확인)',
      TH: '자동환불 처리 가능 기간이 아닙니다. (태국 기준 결제일 익일 การตั้งค่า 시각부터 환불 가능 일수 확인)'
    },
    '재결제 URL 방식을 사용하려면 운영(Y)·연동용도 URL재결제 결제대행사 바인딩이 필요합니다.': {
      EN: '재결제 URL 방식을 사용하려면 운영(Y)·연동용도 URL재결제 payment gateway 바인딩이 필요합니다.',
      JP: '재결제 URL 방식을 사용하려면 운영(Y)·연동용도 URL재결제 決済代行 바인딩이 필요합니다.',
      CH: '재결제 URL 방식을 사용하려면 운영(Y)·연동용도 URL재결제 支付机构 바인딩이 필요합니다.',
      TH: '재결제 URL 방식을 사용하려면 운영(Y)·연동용도 URL재결제 ผู้ให้บริการชำระเงิน 바인딩이 필요합니다.'
    },
    '저장 권한이 없습니다.': {
      EN: 'You do not have 저장 permission.',
      JP: '저장権限がありません。',
      CH: '无저장权限。',
      TH: 'ไม่มีสิทธิ์저장'
    },
    '저장 중 DB 제약 오류가 났습니다. 노티 URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.': {
      EN: '저장 중 DB 제약 오류가 났습니다. notification URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.',
      JP: '저장 중 DB 제약 오류가 났습니다. 通知 URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.',
      CH: '저장 중 DB 제약 오류가 났습니다. 通知 URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.',
      TH: '저장 중 DB 제약 오류가 났습니다. การแจ้งเตือน URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.'
    },
    '저장된 사용자는 삭제할 수 없습니다. 미사용·영구정지 처리로 변경하세요.': {
      EN: 'Saved users cannot be deleted. Change their status to inactive or permanently suspended instead.',
      JP: '保存済みユーザーは削除できません。未使用・永久停止に変更してください。',
      CH: '已保存的用户无法删除。请改为设为未使用或永久停用。',
      TH: 'ไม่สามารถลบผู้ใช้ที่บันทึกแล้วได้ กรุณาเปลี่ยนเป็นสถานะไม่ใช้หรือระงับถาวรแทน'
    },
    '저장된 영업일 설정 목록에 없는 ID입니다.': {
      EN: '저장된 영업일 setting list에 없는 ID입니다.',
      JP: '저장된 영업일 設定 リスト에 없는 ID입니다.',
      CH: '저장된 영업일 设置 列表에 없는 ID입니다.',
      TH: '저장된 영업일 การตั้งค่า รายการ에 없는 ID입니다.'
    },
    '저장된 원문(raw_body)이 비어 있습니다.': {
      EN: '저장된 원문(raw_body)이 비어 있습니다.',
      JP: '저장된 원문(raw_body)이 비어 있습니다.',
      CH: '저장된 원문(raw_body)이 비어 있습니다.',
      TH: '저장된 원문(raw_body)이 비어 있습니다.'
    },
    '전산설정(SMTP)이 없습니다. 메일 발송을 위해 전산설정관리에서 SMTP를 등록하세요.': {
      EN: '전산setting(SMTP)이 없습니다. 메일 발송을 위해 전산setting관리에서 SMTP를 등록하세요.',
      JP: '전산設定(SMTP)이 없습니다. 메일 발송을 위해 전산設定관리에서 SMTP를 등록하세요.',
      CH: '전산设置(SMTP)이 없습니다. 메일 발송을 위해 전산设置관리에서 SMTP를 등록하세요.',
      TH: '전산การตั้งค่า(SMTP)이 없습니다. 메일 발송을 위해 전산การตั้งค่า관리에서 SMTP를 등록하세요.'
    },
    '전산설정을 찾을 수 없습니다.': {
      EN: '전산setting could not be found.',
      JP: '전산設定が見つかりません。',
      CH: '未找到전산设置。',
      TH: 'ไม่พบ전산การตั้งค่า'
    },
    '접두 숫자가 필요합니다.': {
      EN: '접두 숫자 is required.',
      JP: '접두 숫자が必要です。',
      CH: '需要접두 숫자。',
      TH: 'ต้องระบุ접두 숫자'
    },
    '정산 실행 건을 찾을 수 없습니다.': {
      EN: 'settlement 실행 건 could not be found.',
      JP: '精算 실행 건が見つかりません。',
      CH: '未找到结算 실행 건。',
      TH: 'ไม่พบการชำระบัญชี 실행 건'
    },
    '정산배포(DISTRIBUTED)된 실행만 조회할 수 있습니다.': {
      EN: 'Only settlement배포(DISTRIBUTED)된 실행 can view this.',
      JP: '精算배포(DISTRIBUTED)된 실행のみ参照できます。',
      CH: '仅结算배포(DISTRIBUTED)된 실행可查看。',
      TH: 'เฉพาะการชำระบัญชี배포(DISTRIBUTED)된 실행เท่านั้นที่ดูได้'
    },
    '정산주기 정의를 찾을 수 없습니다.': {
      EN: 'settlement주기 정의 could not be found.',
      JP: '精算주기 정의が見つかりません。',
      CH: '未找到结算주기 정의。',
      TH: 'ไม่พบการชำระบัญชี주기 정의'
    },
    '정산주기 코드가 비어 있습니다.': {
      EN: 'settlement주기 code가 비어 있습니다.',
      JP: '精算주기 コード가 비어 있습니다.',
      CH: '结算주기 代码가 비어 있습니다.',
      TH: 'การชำระบัญชี주기 รหัส가 비어 있습니다.'
    },
    '정책을 찾을 수 없습니다.': {
      EN: '정책 could not be found.',
      JP: '정책が見つかりません。',
      CH: '未找到정책。',
      TH: 'ไม่พบ정책'
    },
    '정책코드는 영문 대문자/숫자(1~20자)로 입력하세요.': {
      EN: '정책code는 영문 대문자/숫자(1~20자)로 입력하세요.',
      JP: '정책コード는 영문 대문자/숫자(1~20자)로 입력하세요.',
      CH: '정책代码는 영문 대문자/숫자(1~20자)로 입력하세요.',
      TH: '정책รหัส는 영문 대문자/숫자(1~20자)로 입력하세요.'
    },
    '조직 단계별 기본 권한은 총본사(또는 시스템 관리자)만 저장할 수 있습니다.': {
      EN: 'Only organization 단계별 기본 permission은 headquarters(또는 system administrator) can save.',
      JP: '組織 단계별 기본 権限은 総本社(또는 システム管理者)のみ保存できます。',
      CH: '仅组织 단계별 기본 权限은 总总部(또는 系统管理员)可保存。',
      TH: 'เฉพาะองค์กร 단계별 기본 สิทธิ์은 สำนักงานใหญ่(또는 ผู้ดูแลระบบ)เท่านั้นที่บันทึกได้'
    },
    '조직 레벨 정보가 없습니다. 관리자에게 문의하세요.': {
      EN: 'organization 레벨 information가 없습니다. administrator에게 문의하세요.',
      JP: '組織 레벨 情報가 없습니다. 管理者에게 문의하세요.',
      CH: '组织 레벨 信息가 없습니다. 管理员에게 문의하세요.',
      TH: 'องค์กร 레벨 ข้อมูล가 없습니다. ผู้ดูแลระบบ에게 문의하세요.'
    },
    '조직 정보를 찾을 수 없습니다.': {
      EN: 'organization information could not be found.',
      JP: '組織 情報が見つかりません。',
      CH: '未找到组织 信息。',
      TH: 'ไม่พบองค์กร ข้อมูล'
    },
    '조직 정보를 확인할 수 없습니다.': {
      EN: 'organization information could not be verified.',
      JP: '組織 情報を確認できません。',
      CH: '无法确认组织 信息。',
      TH: 'ไม่สามารถยืนยันองค์กร ข้อมูลได้'
    },
    '조직 ID가 올바르지 않습니다.': {
      EN: 'organization ID가 올바르지 않습니다.',
      JP: '組織 ID가 올바르지 않습니다.',
      CH: '组织 ID가 올바르지 않습니다.',
      TH: 'องค์กร ID가 올바르지 않습니다.'
    },
    '조직(업체)을 선택하세요.': {
      EN: 'organization(company)을 선택하세요.',
      JP: '組織(企業)을 선택하세요.',
      CH: '组织(企业)을 선택하세요.',
      TH: 'องค์กร(บริษัท)을 선택하세요.'
    },
    '조직(업체)을 지정하세요.': {
      EN: 'organization(company)을 지정하세요.',
      JP: '組織(企業)을 지정하세요.',
      CH: '组织(企业)을 지정하세요.',
      TH: 'องค์กร(บริษัท)을 지정하세요.'
    },
    '조직별 수수료(tierCommission) 형식이 올바르지 않습니다.': {
      EN: 'Invalid organization별 수수료(tierCommission) format.',
      JP: '組織별 수수료(tierCommission)の形式が正しくありません。',
      CH: '组织별 수수료(tierCommission)格式不正确。',
      TH: 'รูปแบบองค์กร별 수수료(tierCommission)ไม่ถูกต้อง'
    },
    '조직을 찾을 수 없습니다.': {
      EN: 'organization could not be found.',
      JP: '組織が見つかりません。',
      CH: '未找到组织。',
      TH: 'ไม่พบองค์กร'
    },
    '조회 가능한 가맹 범위가 없습니다.': {
      EN: '조회 가능한 merchant 범위가 없습니다.',
      JP: '조회 가능한 加盟店 범위가 없습니다.',
      CH: '조회 가능한 加盟店 범위가 없습니다.',
      TH: '조회 가능한 ร้านค้า 범위가 없습니다.'
    },
    '조회 가능한 가맹점이 없습니다.': {
      EN: '조회 가능한 merchant이 없습니다.',
      JP: '조회 가능한 加盟店이 없습니다.',
      CH: '조회 가능한 加盟店이 없습니다.',
      TH: '조회 가능한 ร้านค้า이 없습니다.'
    },
    '조회 권한이 없습니다.': {
      EN: 'You do not have 조회 permission.',
      JP: '조회権限がありません。',
      CH: '无조회权限。',
      TH: 'ไม่มีสิทธิ์조회'
    },
    '존재하지 않는 업체코드입니다.': {
      EN: '존재하지 않는 company code입니다.',
      JP: '존재하지 않는 企業コード입니다.',
      CH: '존재하지 않는 企业代码입니다.',
      TH: '존재하지 않는 รหัสบริษัท입니다.'
    },
    '주문 시트 UI JSON 형식이 올바르지 않습니다.': {
      EN: 'Invalid 주문 시트 UI JSON format.',
      JP: '주문 시트 UI JSONの形式が正しくありません。',
      CH: '주문 시트 UI JSON格式不正确。',
      TH: 'รูปแบบ주문 시트 UI JSONไม่ถูกต้อง'
    },
    '주문 시트 UI JSON은 12000자 이내로 입력하세요.': {
      EN: '주문 시트 UI JSON은 12000자 이내로 입력하세요.',
      JP: '주문 시트 UI JSON은 12000자 이내로 입력하세요.',
      CH: '주문 시트 UI JSON은 12000자 이내로 입력하세요.',
      TH: '주문 시트 UI JSON은 12000자 이내로 입력하세요.'
    },
    '주문번호 생성에 실패했습니다. 잠시 후 다시 시도하세요.': {
      EN: '주문번호 생성에 실패했습니다. 잠시 후 다시 시도하세요.',
      JP: '주문번호 생성에 실패했습니다. 잠시 후 다시 시도하세요.',
      CH: '주문번호 생성에 실패했습니다. 잠시 후 다시 시도하세요.',
      TH: '주문번호 생성에 실패했습니다. 잠시 후 다시 시도하세요.'
    },
    '주소(ordererAddr)를 숨길 때는 prefillWhenHidden 을 4자 이상으로 지정하세요.': {
      EN: '주소(ordererAddr)를 숨길 때는 prefillWhenHidden 을 4자 이상으로 지정하세요.',
      JP: '주소(ordererAddr)를 숨길 때는 prefillWhenHidden 을 4자 이상으로 지정하세요.',
      CH: '주소(ordererAddr)를 숨길 때는 prefillWhenHidden 을 4자 이상으로 지정하세요.',
      TH: '주소(ordererAddr)를 숨길 때는 prefillWhenHidden 을 4자 이상으로 지정하세요.'
    },
    '즉시 변경은 양의 정수 건수 또는 무제한 항목만 선택할 수 있습니다.': {
      EN: '즉시 변경은 양의 정수 건수 또는 무제한 항목만 선택할 수 있습니다.',
      JP: '즉시 변경은 양의 정수 건수 또는 무제한 항목만 선택할 수 있습니다.',
      CH: '즉시 변경은 양의 정수 건수 또는 무제한 항목만 선택할 수 있습니다.',
      TH: '즉시 변경은 양의 정수 건수 또는 무제한 항목만 선택할 수 있습니다.'
    },
    '즉시 적용은 상위 플랜(건수 증가)만 가능합니다. 감소·동일 반영은 「다음 플랜(예약)」에서 설정하세요.': {
      EN: '즉시 적용은 상위 플랜(건수 증가)만 가능합니다. 감소·동일 반영은 「다음 플랜(예약)」에서 setting하세요.',
      JP: '즉시 적용은 상위 플랜(건수 증가)만 가능합니다. 감소·동일 반영은 「다음 플랜(예약)」에서 設定하세요.',
      CH: '즉시 적용은 상위 플랜(건수 증가)만 가능합니다. 감소·동일 반영은 「다음 플랜(예약)」에서 设置하세요.',
      TH: '즉시 적용은 상위 플랜(건수 증가)만 가능합니다. 감소·동일 반영은 「다음 플랜(예약)」에서 การตั้งค่า하세요.'
    },
    '즉시 플랜(건수) 값이 올바르지 않습니다.': {
      EN: '즉시 플랜(건수) 값이 올바르지 않습니다.',
      JP: '즉시 플랜(건수) 값이 올바르지 않습니다.',
      CH: '즉시 플랜(건수) 값이 올바르지 않습니다.',
      TH: '즉시 플랜(건수) 값이 올바르지 않습니다.'
    },
    '지급보류 적치 건은 리포트 상세를 열 수 없습니다.': {
      EN: '지급보류 적치 건은 리포트 상세를 열 수 없습니다.',
      JP: '지급보류 적치 건은 리포트 상세를 열 수 없습니다.',
      CH: '지급보류 적치 건은 리포트 상세를 열 수 없습니다.',
      TH: '지급보류 적치 건은 리포트 상세를 열 수 없습니다.'
    },
    '지원하지 않는 가맹점 업체성격 코드입니다. GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, ': {
      EN: '지원하지 않는 merchant company성격 code입니다. GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, ',
      JP: '지원하지 않는 加盟店 企業성격 コード입니다. GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, ',
      CH: '지원하지 않는 加盟店 企业성격 代码입니다. GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, ',
      TH: '지원하지 않는 ร้านค้า บริษัท성격 รหัส입니다. GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, '
    },
    '지원하지 않는 챗봇 운영방식 코드입니다. 허용: SALE_PREPAID, SALE_POSTPAID, ': {
      EN: '지원하지 않는 챗봇 운영방식 code입니다. 허용: SALE_PREPAID, SALE_POSTPAID, ',
      JP: '지원하지 않는 챗봇 운영방식 コード입니다. 허용: SALE_PREPAID, SALE_POSTPAID, ',
      CH: '지원하지 않는 챗봇 운영방식 代码입니다. 허용: SALE_PREPAID, SALE_POSTPAID, ',
      TH: '지원하지 않는 챗봇 운영방식 รหัส입니다. 허용: SALE_PREPAID, SALE_POSTPAID, '
    },
    '지정할 영업일 설정 ID가 필요합니다.': {
      EN: '지정할 영업일 setting ID is required.',
      JP: '지정할 영업일 設定 IDが必要です。',
      CH: '需要지정할 영업일 设置 ID。',
      TH: 'ต้องระบุ지정할 영업일 การตั้งค่า ID'
    },
    '차감 금액(amount) 형식이 올바르지 않습니다.': {
      EN: 'Invalid 차감 amount(amount) format.',
      JP: '차감 金額(amount)の形式が正しくありません。',
      CH: '차감 金额(amount)格式不正确。',
      TH: 'รูปแบบ차감 จำนวนเงิน(amount)ไม่ถูกต้อง'
    },
    '차감 금액은 0보다 커야 합니다.': {
      EN: '차감 amount은 0보다 커야 합니다.',
      JP: '차감 金額은 0보다 커야 합니다.',
      CH: '차감 金额은 0보다 커야 합니다.',
      TH: '차감 จำนวนเงิน은 0보다 커야 합니다.'
    },
    '차감할 미수금 잔액이 없습니다. (해당 업체에 PENDING·PARTIAL 건 없음)': {
      EN: '차감할 미수금 잔액이 없습니다. (해당 company에 PENDING·PARTIAL 건 없음)',
      JP: '차감할 미수금 잔액이 없습니다. (해당 企業에 PENDING·PARTIAL 건 없음)',
      CH: '차감할 미수금 잔액이 없습니다. (해당 企业에 PENDING·PARTIAL 건 없음)',
      TH: '차감할 미수금 잔액이 없습니다. (해당 บริษัท에 PENDING·PARTIAL 건 없음)'
    },
    '차감할 잔여 미수금이 없습니다.': {
      EN: '차감할 잔여 미수금이 없습니다.',
      JP: '차감할 잔여 미수금이 없습니다.',
      CH: '차감할 잔여 미수금이 없습니다.',
      TH: '차감할 잔여 미수금이 없습니다.'
    },
    '차지백 구간정책 ID 형식이 올바르지 않습니다.': {
      EN: 'Invalid 차지백 구간정책 ID format.',
      JP: '차지백 구간정책 IDの形式が正しくありません。',
      CH: '차지백 구간정책 ID格式不正确。',
      TH: 'รูปแบบ차지백 구간정책 IDไม่ถูกต้อง'
    },
    '차지백 정책을 찾을 수 없습니다.': {
      EN: '차지백 정책 could not be found.',
      JP: '차지백 정책が見つかりません。',
      CH: '未找到차지백 정책。',
      TH: 'ไม่พบ차지백 정책'
    },
    '챗봇 결제가 비활성인 가맹점입니다.': {
      EN: '챗봇 결제가 비활성인 merchant입니다.',
      JP: '챗봇 결제가 비활성인 加盟店입니다.',
      CH: '챗봇 결제가 비활성인 加盟店입니다.',
      TH: '챗봇 결제가 비활성인 ร้านค้า입니다.'
    },
    '챗봇 결제가 비활성입니다.': {
      EN: '챗봇 결제가 비활성입니다.',
      JP: '챗봇 결제가 비활성입니다.',
      CH: '챗봇 결제가 비활성입니다.',
      TH: '챗봇 결제가 비활성입니다.'
    },
    '챗봇 관리자 로그인ID는 50자 이하여야 합니다.': {
      EN: '챗봇 administrator loginID는 50자 이하여야 합니다.',
      JP: '챗봇 管理者 ログインID는 50자 이하여야 합니다.',
      CH: '챗봇 管理员 登录ID는 50자 이하여야 합니다.',
      TH: '챗봇 ผู้ดูแลระบบ เข้าสู่ระบบID는 50자 이하여야 합니다.'
    },
    '챗봇 관리자 로그인ID를 입력하세요.': {
      EN: '챗봇 administrator loginID를 입력하세요.',
      JP: '챗봇 管理者 ログインID를 입력하세요.',
      CH: '챗봇 管理员 登录ID를 입력하세요.',
      TH: '챗봇 ผู้ดูแลระบบ เข้าสู่ระบบID를 입력하세요.'
    },
    '챗봇 관리자가 지정되지 않았습니다. 본사/상위에서 업체 정보에 등록하세요.': {
      EN: '챗봇 administrator가 지정되지 않았습니다. head office/상위에서 company information에 등록하세요.',
      JP: '챗봇 管理者가 지정되지 않았습니다. 本社/상위에서 企業 情報에 등록하세요.',
      CH: '챗봇 管理员가 지정되지 않았습니다. 总部/상위에서 企业 信息에 등록하세요.',
      TH: '챗봇 ผู้ดูแลระบบ가 지정되지 않았습니다. สำนักงานใหญ่/상위에서 บริษัท ข้อมูล에 등록하세요.'
    },
    '챗봇 관리자는 해당 가맹점 소속 사용자만 지정할 수 있습니다.': {
      EN: '챗봇 administrator는 해당 merchant 소속 사용자만 지정할 수 있습니다.',
      JP: '챗봇 管理者는 해당 加盟店 소속 사용자만 지정할 수 있습니다.',
      CH: '챗봇 管理员는 해당 加盟店 소속 사용자만 지정할 수 있습니다.',
      TH: '챗봇 ผู้ดูแลระบบ는 해당 ร้านค้า 소속 사용자만 지정할 수 있습니다.'
    },
    '챗봇 관리자는 Google OTP 등록이 필요합니다. 관리자 웹에서 OTP를 먼저 등록하세요.': {
      EN: '챗봇 administrator는 Google OTP 등록이 필요합니다. administrator 웹에서 OTP를 먼저 등록하세요.',
      JP: '챗봇 管理者는 Google OTP 등록이 필요합니다. 管理者 웹에서 OTP를 먼저 등록하세요.',
      CH: '챗봇 管理员는 Google OTP 등록이 필요합니다. 管理员 웹에서 OTP를 먼저 등록하세요.',
      TH: '챗봇 ผู้ดูแลระบบ는 Google OTP 등록이 필요합니다. ผู้ดูแลระบบ 웹에서 OTP를 먼저 등록하세요.'
    },
    '챗봇 관리자로 지정할 수 없는 계정입니다.': {
      EN: '챗봇 administrator로 지정할 수 없는 account입니다.',
      JP: '챗봇 管理者로 지정할 수 없는 アカウント입니다.',
      CH: '챗봇 管理员로 지정할 수 없는 账号입니다.',
      TH: '챗봇 ผู้ดูแลระบบ로 지정할 수 없는 บัญชี입니다.'
    },
    '챗봇 상단 로고 URL은 500자 이하여야 합니다.': {
      EN: '챗봇 상단 로고 URL은 500자 이하여야 합니다.',
      JP: '챗봇 상단 로고 URL은 500자 이하여야 합니다.',
      CH: '챗봇 상단 로고 URL은 500자 이하여야 합니다.',
      TH: '챗봇 상단 로고 URL은 500자 이하여야 합니다.'
    },
    '챗봇 상품관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.': {
      EN: 'Only 챗봇 상품관리는 company 대표 또는 permission그룹 CHATBOT account can use this.',
      JP: '챗봇 상품관리는 企業 대표 또는 権限그룹 CHATBOT アカウントのみ使用できます。',
      CH: '仅챗봇 상품관리는 企业 대표 또는 权限그룹 CHATBOT 账号可使用。',
      TH: 'เฉพาะ챗봇 상품관리는 บริษัท 대표 또는 สิทธิ์그룹 CHATBOT บัญชีเท่านั้นที่ใช้ได้'
    },
    '챗봇 주문관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.': {
      EN: 'Only 챗봇 주문관리는 company 대표 또는 permission그룹 CHATBOT account can use this.',
      JP: '챗봇 주문관리는 企業 대표 또는 権限그룹 CHATBOT アカウントのみ使用できます。',
      CH: '仅챗봇 주문관리는 企业 대표 또는 权限그룹 CHATBOT 账号可使用。',
      TH: 'เฉพาะ챗봇 주문관리는 บริษัท 대표 또는 สิทธิ์그룹 CHATBOT บัญชีเท่านั้นที่ใช้ได้'
    },
    '챗봇결제가 활성화된 가맹점만 운영 보류를 적용할 수 있습니다.': {
      EN: '챗봇결제가 활성화된 merchant만 운영 보류를 적용할 수 있습니다.',
      JP: '챗봇결제가 활성화된 加盟店만 운영 보류를 적용할 수 있습니다.',
      CH: '챗봇결제가 활성화된 加盟店만 운영 보류를 적용할 수 있습니다.',
      TH: '챗봇결제가 활성화된 ร้านค้า만 운영 보류를 적용할 수 있습니다.'
    },
    '체크아웃 날짜는 체크인 다음 날 이후여야 합니다.': {
      EN: '체크아웃 날짜는 체크인 다음 날 이후여야 합니다.',
      JP: '체크아웃 날짜는 체크인 다음 날 이후여야 합니다.',
      CH: '체크아웃 날짜는 체크인 다음 날 이후여야 합니다.',
      TH: '체크아웃 날짜는 체크인 다음 날 이후여야 합니다.'
    },
    '체크아웃 날짜는 YYYY-MM-DD 형식이어야 합니다.': {
      EN: '체크아웃 날짜는 YYYY-MM-DD 형식이어야 합니다.',
      JP: '체크아웃 날짜는 YYYY-MM-DD 형식이어야 합니다.',
      CH: '체크아웃 날짜는 YYYY-MM-DD 형식이어야 합니다.',
      TH: '체크아웃 날짜는 YYYY-MM-DD 형식이어야 합니다.'
    },
    '초기화할 사용자 ID가 필요합니다.': {
      EN: '초기화할 사용자 ID is required.',
      JP: '초기화할 사용자 IDが必要です。',
      CH: '需要초기화할 사용자 ID。',
      TH: 'ต้องระบุ초기화할 사용자 ID'
    },
    '총본사 환경설정에서 「관리담당 사용자관리 권한」이 꺼져 있습니다. 본사설정 → 사용자설정에서 사용으로 저장한 뒤 다시 시도하세요.': {
      EN: 'headquarters 환경setting에서 「관리담당 사용자관리 permission」이 꺼져 있습니다. head officesetting → 사용자setting에서 사용으로 저장한 뒤 다시 시도하세요.',
      JP: '総本社 환경設定에서 「관리담당 사용자관리 権限」이 꺼져 있습니다. 本社設定 → 사용자設定에서 사용으로 저장한 뒤 다시 시도하세요.',
      CH: '总总部 환경设置에서 「관리담당 사용자관리 权限」이 꺼져 있습니다. 总部设置 → 사용자设置에서 사용으로 저장한 뒤 다시 시도하세요.',
      TH: 'สำนักงานใหญ่ 환경การตั้งค่า에서 「관리담당 사용자관리 สิทธิ์」이 꺼져 있습니다. สำนักงานใหญ่การตั้งค่า → 사용자การตั้งค่า에서 사용으로 저장한 뒤 다시 시도하세요.'
    },
    '총본사 환경설정에서 비밀번호 초기화가 비활성화되어 있습니다.': {
      EN: 'headquarters 환경setting에서 비밀번호 초기화가 비활성화되어 있습니다.',
      JP: '総本社 환경設定에서 비밀번호 초기화가 비활성화되어 있습니다.',
      CH: '总总部 환경设置에서 비밀번호 초기화가 비활성화되어 있습니다.',
      TH: 'สำนักงานใหญ่ 환경การตั้งค่า에서 비밀번호 초기화가 비활성화되어 있습니다.'
    },
    '총본사, 본사 또는 총판만 브랜딩을 설정할 수 있습니다.': {
      EN: 'headquarters, head office 또는 master distributor만 브랜딩을 setting할 수 있습니다.',
      JP: '総本社, 本社 또는 総販만 브랜딩을 設定할 수 있습니다.',
      CH: '总总部, 总部 또는 总经销만 브랜딩을 设置할 수 있습니다.',
      TH: 'สำนักงานใหญ่, สำนักงานใหญ่ 또는 ตัวแทนหลัก만 브랜딩을 การตั้งค่า할 수 있습니다.'
    },
    '총본사(또는 ADMIN)만 관리할 수 있습니다.': {
      EN: 'headquarters(또는 ADMIN)만 관리할 수 있습니다.',
      JP: '総本社(또는 ADMIN)만 관리할 수 있습니다.',
      CH: '总总部(또는 ADMIN)만 관리할 수 있습니다.',
      TH: 'สำนักงานใหญ่(또는 ADMIN)만 관리할 수 있습니다.'
    },
    '총본사(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.': {
      EN: 'headquarters(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.',
      JP: '総本社(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.',
      CH: '总总部(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.',
      TH: 'สำนักงานใหญ่(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.'
    },
    '총본사(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.': {
      EN: 'headquarters(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.',
      JP: '総本社(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.',
      CH: '总总部(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.',
      TH: 'สำนักงานใหญ่(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.'
    },
    '총본사(HEADQUARTERS) 또는 시스템 관리자만 등록할 수 있습니다.': {
      EN: 'Only headquarters (HEADQUARTERS) 또는 system administrator can register.',
      JP: '総本社（HEADQUARTERS） 또는 システム管理者のみ登録できます。',
      CH: '仅总总部（HEADQUARTERS） 또는 系统管理员可注册。',
      TH: 'เฉพาะสำนักงานใหญ่ (HEADQUARTERS) 또는 ผู้ดูแลระบบเท่านั้นที่ลงทะเบียนได้'
    },
    '총본사(HEADQUARTERS) 또는 시스템 관리자만 삭제할 수 있습니다.': {
      EN: 'Only headquarters (HEADQUARTERS) 또는 system administrator can delete.',
      JP: '総本社（HEADQUARTERS） 또는 システム管理者のみ削除できます。',
      CH: '仅总总部（HEADQUARTERS） 또는 系统管理员可删除。',
      TH: 'เฉพาะสำนักงานใหญ่ (HEADQUARTERS) 또는 ผู้ดูแลระบบเท่านั้นที่ลบได้'
    },
    '총본사(HEADQUARTERS) 또는 시스템 관리자만 수정할 수 있습니다.': {
      EN: 'Only headquarters (HEADQUARTERS) 또는 system administrator can modify.',
      JP: '総本社（HEADQUARTERS） 또는 システム管理者のみ修正できます。',
      CH: '仅总总部（HEADQUARTERS） 또는 系统管理员可修改。',
      TH: 'เฉพาะสำนักงานใหญ่ (HEADQUARTERS) 또는 ผู้ดูแลระบบเท่านั้นที่แก้ไขได้'
    },
    '총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.': {
      EN: 'Only headquarters (HEADQUARTERS) 또는 system administrator can execute.',
      JP: '総本社（HEADQUARTERS） 또는 システム管理者のみ実行できます。',
      CH: '仅总总部（HEADQUARTERS） 또는 系统管理员可执行。',
      TH: 'เฉพาะสำนักงานใหญ่ (HEADQUARTERS) 또는 ผู้ดูแลระบบเท่านั้นที่ดำเนินการได้'
    },
    '총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.': {
      EN: 'Only headquarters (HEADQUARTERS) 또는 system administrator can save.',
      JP: '総本社（HEADQUARTERS） 또는 システム管理者のみ保存できます。',
      CH: '仅总总部（HEADQUARTERS） 또는 系统管理员可保存。',
      TH: 'เฉพาะสำนักงานใหญ่ (HEADQUARTERS) 또는 ผู้ดูแลระบบเท่านั้นที่บันทึกได้'
    },
    '총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.': {
      EN: 'Only headquarters (HEADQUARTERS) 또는 system administrator can view this.',
      JP: '総本社（HEADQUARTERS） 또는 システム管理者のみ参照できます。',
      CH: '仅总总部（HEADQUARTERS） 또는 系统管理员可查看。',
      TH: 'เฉพาะสำนักงานใหญ่ (HEADQUARTERS) 또는 ผู้ดูแลระบบเท่านั้นที่ดูได้'
    },
    '총본사는 처리 대상에서 제외됩니다.': {
      EN: 'headquarters는 처리 대상에서 제외됩니다.',
      JP: '総本社는 처리 대상에서 제외됩니다.',
      CH: '总总部는 처리 대상에서 제외됩니다.',
      TH: 'สำนักงานใหญ่는 처리 대상에서 제외됩니다.'
    },
    '총판 기준 화폐는 상위 본사가 지정한 기준 화폐 중에서만 선택할 수 있습니다.': {
      EN: 'master distributor 기준 화폐는 상위 head office가 지정한 기준 화폐 중에서만 선택할 수 있습니다.',
      JP: '総販 기준 화폐는 상위 本社가 지정한 기준 화폐 중에서만 선택할 수 있습니다.',
      CH: '总经销 기준 화폐는 상위 总部가 지정한 기준 화폐 중에서만 선택할 수 있습니다.',
      TH: 'ตัวแทนหลัก 기준 화폐는 상위 สำนักงานใหญ่가 지정한 기준 화폐 중에서만 선택할 수 있습니다.'
    },
    '총판 노티 URL 1(기본)은 필수입니다.': {
      EN: 'master distributor notification URL 1(기본)은 필수입니다.',
      JP: '総販 通知 URL 1(기본)은 필수입니다.',
      CH: '总经销 通知 URL 1(기본)은 필수입니다.',
      TH: 'ตัวแทนหลัก การแจ้งเตือน URL 1(기본)은 필수입니다.'
    },
    '총판 노티 URL 2(RESULT)는 필수입니다. CALLBACK(URL 1)과 함께 입력하세요.': {
      EN: 'master distributor notification URL 2(RESULT)는 필수입니다. CALLBACK(URL 1)과 함께 입력하세요.',
      JP: '総販 通知 URL 2(RESULT)는 필수입니다. CALLBACK(URL 1)과 함께 입력하세요.',
      CH: '总经销 通知 URL 2(RESULT)는 필수입니다. CALLBACK(URL 1)과 함께 입력하세요.',
      TH: 'ตัวแทนหลัก การแจ้งเตือน URL 2(RESULT)는 필수입니다. CALLBACK(URL 1)과 함께 입력하세요.'
    },
    '총판(MASTER_DIST) 조직만 설정할 수 있습니다.': {
      EN: 'master distributor (MASTER_DIST) organization만 setting할 수 있습니다.',
      JP: '総販（MASTER_DIST） 組織만 設定할 수 있습니다.',
      CH: '总经销（MASTER_DIST） 组织만 设置할 수 있습니다.',
      TH: 'ตัวแทนหลัก (MASTER_DIST) องค์กร만 การตั้งค่า할 수 있습니다.'
    },
    '총판(MASTER_DIST) 코드만 저장할 수 있습니다.': {
      EN: 'Only master distributor (MASTER_DIST) code can save.',
      JP: '総販（MASTER_DIST） コードのみ保存できます。',
      CH: '仅总经销（MASTER_DIST） 代码可保存。',
      TH: 'เฉพาะตัวแทนหลัก (MASTER_DIST) รหัสเท่านั้นที่บันทึกได้'
    },
    '총판(MASTER_DIST)만 정산 크론 기준을 설정할 수 있습니다.': {
      EN: 'master distributor (MASTER_DIST)만 settlement 크론 기준을 setting할 수 있습니다.',
      JP: '総販（MASTER_DIST）만 精算 크론 기준을 設定할 수 있습니다.',
      CH: '总经销（MASTER_DIST）만 结算 크론 기준을 设置할 수 있습니다.',
      TH: 'ตัวแทนหลัก (MASTER_DIST)만 การชำระบัญชี 크론 기준을 การตั้งค่า할 수 있습니다.'
    },
    '총판은 1가지 화폐만 지정할 수 있습니다.': {
      EN: 'master distributor은 1가지 화폐만 지정할 수 있습니다.',
      JP: '総販은 1가지 화폐만 지정할 수 있습니다.',
      CH: '总经销은 1가지 화폐만 지정할 수 있습니다.',
      TH: 'ตัวแทนหลัก은 1가지 화폐만 지정할 수 있습니다.'
    },
    '통합정산 예정일 N은 정수입니다.': {
      EN: '통합settlement 예정일 N은 정수입니다.',
      JP: '통합精算 예정일 N은 정수입니다.',
      CH: '통합结算 예정일 N은 정수입니다.',
      TH: '통합การชำระบัญชี 예정일 N은 정수입니다.'
    },
    '통합정산 예정일: 모드는 OFF, T, D 중 하나입니다.': {
      EN: '통합settlement 예정일: 모드는 OFF, T, D 중 하나입니다.',
      JP: '통합精算 예정일: 모드는 OFF, T, D 중 하나입니다.',
      CH: '통합结算 예정일: 모드는 OFF, T, D 중 하나입니다.',
      TH: '통합การชำระบัญชี 예정일: 모드는 OFF, T, D 중 하나입니다.'
    },
    '파비콘 이미지는 1MB 이하여야 합니다.': {
      EN: '파비콘 이미지는 1MB 이하여야 합니다.',
      JP: '파비콘 이미지는 1MB 이하여야 합니다.',
      CH: '파비콘 이미지는 1MB 이하여야 합니다.',
      TH: '파비콘 이미지는 1MB 이하여야 합니다.'
    },
    '판매 중인 상품이 아닙니다.': {
      EN: '판매 중인 상품이 아닙니다.',
      JP: '판매 중인 상품이 아닙니다.',
      CH: '판매 중인 상품이 아닙니다.',
      TH: '판매 중인 상품이 아닙니다.'
    },
    '표시명은 200자 이내입니다.': {
      EN: '표시명은 200자 이내입니다.',
      JP: '표시명은 200자 이내입니다.',
      CH: '표시명은 200자 이내입니다.',
      TH: '표시명은 200자 이내입니다.'
    },
    '한 번에 연결할 노티 대상은 2건 이하입니다.': {
      EN: '한 번에 연결할 notification 대상은 2건 이하입니다.',
      JP: '한 번에 연결할 通知 대상은 2건 이하입니다.',
      CH: '한 번에 연결할 通知 대상은 2건 이하입니다.',
      TH: '한 번에 연결할 การแจ้งเตือน 대상은 2건 이하입니다.'
    },
    '항목 표시명을 입력하세요.': {
      EN: '항목 표시명을 입력하세요.',
      JP: '항목 표시명을 입력하세요.',
      CH: '항목 표시명을 입력하세요.',
      TH: '항목 표시명을 입력하세요.'
    },
    '항목을 찾을 수 없습니다.': {
      EN: '항목 could not be found.',
      JP: '항목が見つかりません。',
      CH: '未找到항목。',
      TH: 'ไม่พบ항목'
    },
    '해당 가맹점은 자동 미수금 환수 모드입니다. 환수/미수금설정(총판·가맹) 또는 본사 기본에서 수동으로 적용된 경우에만 이용하세요.': {
      EN: '해당 merchant은 자동 미수금 환수 모드입니다. 환수/미수금setting(master distributor·merchant) 또는 head office 기본에서 수동으로 적용된 경우에만 이용하세요.',
      JP: '해당 加盟店은 자동 미수금 환수 모드입니다. 환수/미수금設定(総販·加盟店) 또는 本社 기본에서 수동으로 적용된 경우에만 이용하세요.',
      CH: '해당 加盟店은 자동 미수금 환수 모드입니다. 환수/미수금设置(总经销·加盟店) 또는 总部 기본에서 수동으로 적용된 경우에만 이용하세요.',
      TH: '해당 ร้านค้า은 자동 미수금 환수 모드입니다. 환수/미수금การตั้งค่า(ตัวแทนหลัก·ร้านค้า) 또는 สำนักงานใหญ่ 기본에서 수동으로 적용된 경우에만 이용하세요.'
    },
    '해당 거래일 ChillPay 통합내역에서 승인번호를 찾을 수 없습니다.': {
      EN: '해당 transaction일 ChillPay 통합내역에서 승인번호 could not be found.',
      JP: '해당 取引일 ChillPay 통합내역에서 승인번호が見つかりません。',
      CH: '未找到해당 交易일 ChillPay 통합내역에서 승인번호。',
      TH: 'ไม่พบ해당 ธุรกรรม일 ChillPay 통합내역에서 승인번호'
    },
    '해당 상품은 노출되지 않습니다.': {
      EN: '해당 상품은 노출되지 않습니다.',
      JP: '해당 상품은 노출되지 않습니다.',
      CH: '해당 상품은 노출되지 않습니다.',
      TH: '해당 상품은 노출되지 않습니다.'
    },
    '해당 시간대는 이미 예약이 있습니다. 다른 시간을 선택하세요.': {
      EN: '해당 시간대는 이미 예약이 있습니다. 다른 시간을 선택하세요.',
      JP: '해당 시간대는 이미 예약이 있습니다. 다른 시간을 선택하세요.',
      CH: '해당 시간대는 이미 예약이 있습니다. 다른 시간을 선택하세요.',
      TH: '해당 시간대는 이미 예약이 있습니다. 다른 시간을 선택하세요.'
    },
    '해당 업체를 관리할 수 없습니다.': {
      EN: '해당 company를 관리할 수 없습니다.',
      JP: '해당 企業를 관리할 수 없습니다.',
      CH: '해당 企业를 관리할 수 없습니다.',
      TH: '해당 บริษัท를 관리할 수 없습니다.'
    },
    '해당 정산 실행에 대한 조회 권한이 없습니다.': {
      EN: 'You do not have 해당 settlement 실행에 대한 조회 permission.',
      JP: '해당 精算 실행에 대한 조회権限がありません。',
      CH: '无해당 结算 실행에 대한 조회权限。',
      TH: 'ไม่มีสิทธิ์해당 การชำระบัญชี 실행에 대한 조회'
    },
    '해당 조직 권한을 조회할 수 없습니다.': {
      EN: '해당 organization permission을 조회할 수 없습니다.',
      JP: '해당 組織 権限을 조회할 수 없습니다.',
      CH: '해당 组织 权限을 조회할 수 없습니다.',
      TH: '해당 องค์กร สิทธิ์을 조회할 수 없습니다.'
    },
    '해당 조직의 정산설정이 없습니다. 업체 등록·정산설정을 먼저 완료하세요.': {
      EN: '해당 organization의 settlementsetting이 없습니다. company 등록·settlementsetting을 먼저 완료하세요.',
      JP: '해당 組織의 精算設定이 없습니다. 企業 등록·精算設定을 먼저 완료하세요.',
      CH: '해당 组织의 结算设置이 없습니다. 企业 등록·结算设置을 먼저 완료하세요.',
      TH: '해당 องค์กร의 การชำระบัญชีการตั้งค่า이 없습니다. บริษัท 등록·การชำระบัญชีการตั้งค่า을 먼저 완료하세요.'
    },
    '해당 조직의 정산설정이 없습니다. 업체 등록을 먼저 완료하세요.': {
      EN: '해당 organization의 settlementsetting이 없습니다. company 등록을 먼저 완료하세요.',
      JP: '해당 組織의 精算設定이 없습니다. 企業 등록을 먼저 완료하세요.',
      CH: '해당 组织의 结算设置이 없습니다. 企业 등록을 먼저 완료하세요.',
      TH: '해당 องค์กร의 การชำระบัญชีการตั้งค่า이 없습니다. บริษัท 등록을 먼저 완료하세요.'
    },
    '허용되지 않은 통화입니다. 목록에서 선택하세요.': {
      EN: '허용되지 않은 통화입니다. list에서 선택하세요.',
      JP: '허용되지 않은 통화입니다. リスト에서 선택하세요.',
      CH: '허용되지 않은 통화입니다. 列表에서 선택하세요.',
      TH: '허용되지 않은 통화입니다. รายการ에서 선택하세요.'
    },
    '현재 로그인 계정으로는 사용자를 추가·수정할 수 없습니다. 시스템 관리자(ADMIN), 조직 대표(대표) 계정, 또는 담당자(ASSISTANT) 중 관리담당(MANAGER)만 가능합니다.': {
      EN: '현재 login account으로는 사용자를 추가·수정할 수 없습니다. system administrator(ADMIN), organization 대표(대표) account, 또는 담당자(ASSISTANT) 중 관리담당(MANAGER)만 가능합니다.',
      JP: '현재 ログイン アカウント으로는 사용자를 추가·수정할 수 없습니다. システム管理者(ADMIN), 組織 대표(대표) アカウント, 또는 담당자(ASSISTANT) 중 관리담당(MANAGER)만 가능합니다.',
      CH: '현재 登录 账号으로는 사용자를 추가·수정할 수 없습니다. 系统管理员(ADMIN), 组织 대표(대표) 账号, 또는 담당자(ASSISTANT) 중 관리담당(MANAGER)만 가능합니다.',
      TH: '현재 เข้าสู่ระบบ บัญชี으로는 사용자를 추가·수정할 수 없습니다. ผู้ดูแลระบบ(ADMIN), องค์กร 대표(대표) บัญชี, 또는 담당자(ASSISTANT) 중 관리담당(MANAGER)만 가능합니다.'
    },
    '현재 비밀번호가 올바르지 않습니다.': {
      EN: '현재 비밀번호가 올바르지 않습니다.',
      JP: '현재 비밀번호가 올바르지 않습니다.',
      CH: '현재 비밀번호가 올바르지 않습니다.',
      TH: '현재 비밀번호가 올바르지 않습니다.'
    },
    '현재 비밀번호를 입력하세요.': {
      EN: '현재 비밀번호를 입력하세요.',
      JP: '현재 비밀번호를 입력하세요.',
      CH: '현재 비밀번호를 입력하세요.',
      TH: '현재 비밀번호를 입력하세요.'
    },
    '현재 비밀번호와 다른 비밀번호를 입력하세요.': {
      EN: '현재 비밀번호와 다른 비밀번호를 입력하세요.',
      JP: '현재 비밀번호와 다른 비밀번호를 입력하세요.',
      CH: '현재 비밀번호와 다른 비밀번호를 입력하세요.',
      TH: '현재 비밀번호와 다른 비밀번호를 입력하세요.'
    },
    '화면 경로(pageUrl)가 필요합니다.': {
      EN: '화면 경로(pageUrl) is required.',
      JP: '화면 경로(pageUrl)が必要です。',
      CH: '需要화면 경로(pageUrl)。',
      TH: 'ต้องระบุ화면 경로(pageUrl)'
    },
    '확정(CALCULATED)된 정산만 리포트로 조회할 수 있습니다.': {
      EN: '확정(CALCULATED)된 settlement만 리포트로 조회할 수 있습니다.',
      JP: '확정(CALCULATED)된 精算만 리포트로 조회할 수 있습니다.',
      CH: '확정(CALCULATED)된 结算만 리포트로 조회할 수 있습니다.',
      TH: '확정(CALCULATED)된 การชำระบัญชี만 리포트로 조회할 수 있습니다.'
    },
    '환불 익일 구간 시작 시각이 올바르지 않습니다(0:00~23:59).': {
      EN: '환불 익일 구간 시작 시각이 올바르지 않습니다(0:00~23:59).',
      JP: '환불 익일 구간 시작 시각이 올바르지 않습니다(0:00~23:59).',
      CH: '환불 익일 구간 시작 시각이 올바르지 않습니다(0:00~23:59).',
      TH: '환불 익일 구간 시작 시각이 올바르지 않습니다(0:00~23:59).'
    },
    '환율 견적을 만들 수 없습니다. 본사설정 「URL결제설정」에서 해당 PG의 FX(자동: BOT API 키, 수동: THB/표시단위)와 DISPLAY 설정을 확인하세요.': {
      EN: '환율 견적을 만들 수 없습니다. head officesetting 「URL결제setting」에서 해당 PG의 FX(자동: BOT API 키, 수동: THB/표시단위)와 DISPLAY setting을 확인하세요.',
      JP: '환율 견적을 만들 수 없습니다. 本社設定 「URL결제設定」에서 해당 PG의 FX(자동: BOT API 키, 수동: THB/표시단위)와 DISPLAY 設定을 확인하세요.',
      CH: '환율 견적을 만들 수 없습니다. 总部设置 「URL결제设置」에서 해당 PG의 FX(자동: BOT API 키, 수동: THB/표시단위)와 DISPLAY 设置을 확인하세요.',
      TH: '환율 견적을 만들 수 없습니다. สำนักงานใหญ่การตั้งค่า 「URL결제การตั้งค่า」에서 해당 PG의 FX(자동: BOT API 키, 수동: THB/표시단위)와 DISPLAY การตั้งค่า을 확인하세요.'
    },
    '활성 시크릿이 없습니다. 먼저 발급하세요.': {
      EN: '활성 시크릿이 없습니다. 먼저 발급하세요.',
      JP: '활성 시크릿이 없습니다. 먼저 발급하세요.',
      CH: '활성 시크릿이 없습니다. 먼저 발급하세요.',
      TH: '활성 시크릿이 없습니다. 먼저 발급하세요.'
    },
    'action이 필요합니다.': {
      EN: 'action이 필요합니다.',
      JP: 'action이 필요합니다.',
      CH: 'action이 필요합니다.',
      TH: 'action이 필요합니다.'
    },
    'ADMIN 계정은 삭제할 수 없습니다.': {
      EN: 'ADMIN account은 삭제할 수 없습니다.',
      JP: 'ADMIN アカウント은 삭제할 수 없습니다.',
      CH: 'ADMIN 账号은 삭제할 수 없습니다.',
      TH: 'ADMIN บัญชี은 삭제할 수 없습니다.'
    },
    'amount는 0보다 커야 합니다.': {
      EN: 'amount는 0보다 커야 합니다.',
      JP: 'amount는 0보다 커야 합니다.',
      CH: 'amount는 0보다 커야 합니다.',
      TH: 'amount는 0보다 커야 합니다.'
    },
    'assistantRoleDefaultMatrix 는 객체여야 합니다.': {
      EN: 'assistantRoleDefaultMatrix 는 객체여야 합니다.',
      JP: 'assistantRoleDefaultMatrix 는 객체여야 합니다.',
      CH: 'assistantRoleDefaultMatrix 는 객체여야 합니다.',
      TH: 'assistantRoleDefaultMatrix 는 객체여야 합니다.'
    },
    'buyer 객체(email·phone·countryIso2)가 필요합니다.': {
      EN: 'buyer 객체(email·phone·countryIso2) is required.',
      JP: 'buyer 객체(email·phone·countryIso2)が必要です。',
      CH: '需要buyer 객체(email·phone·countryIso2)。',
      TH: 'ต้องระบุbuyer 객체(email·phone·countryIso2)'
    },
    'buyer JSON 형식이 올바르지 않습니다.': {
      EN: 'Invalid buyer JSON format.',
      JP: 'buyer JSONの形式が正しくありません。',
      CH: 'buyer JSON格式不正确。',
      TH: 'รูปแบบbuyer JSONไม่ถูกต้อง'
    },
    'buyer.countryIso2(ISO2 국가코드)가 필요합니다.': {
      EN: 'buyer.countryIso2(ISO2 국가code) is required.',
      JP: 'buyer.countryIso2(ISO2 국가コード)が必要です。',
      CH: '需要buyer.countryIso2(ISO2 국가代码)。',
      TH: 'ต้องระบุbuyer.countryIso2(ISO2 국가รหัส)'
    },
    'buyer.email 이 필요합니다.': {
      EN: 'buyer.email 이 필요합니다.',
      JP: 'buyer.email 이 필요합니다.',
      CH: 'buyer.email 이 필요합니다.',
      TH: 'buyer.email 이 필요합니다.'
    },
    'buyer.phone 이 필요합니다.': {
      EN: 'buyer.phone 이 필요합니다.',
      JP: 'buyer.phone 이 필요합니다.',
      CH: 'buyer.phone 이 필요합니다.',
      TH: 'buyer.phone 이 필요합니다.'
    },
    'buyerPrefill 저장 형식 오류': {
      EN: 'buyerPrefill 저장 형식 오류',
      JP: 'buyerPrefill 저장 형식 오류',
      CH: 'buyerPrefill 저장 형식 오류',
      TH: 'buyerPrefill 저장 형식 오류'
    },
    'buyerPrefill JSON 형식이 올바르지 않습니다.': {
      EN: 'Invalid buyerPrefill JSON format.',
      JP: 'buyerPrefill JSONの形式が正しくありません。',
      CH: 'buyerPrefill JSON格式不正确。',
      TH: 'รูปแบบbuyerPrefill JSONไม่ถูกต้อง'
    },
    'buyerPrefill.email (JPAY 필수)이 필요합니다.': {
      EN: 'buyerPrefill.email (JPAY 필수)이 필요합니다.',
      JP: 'buyerPrefill.email (JPAY 필수)이 필요합니다.',
      CH: 'buyerPrefill.email (JPAY 필수)이 필요합니다.',
      TH: 'buyerPrefill.email (JPAY 필수)이 필요합니다.'
    },
    'buyerPrefill.phone (JPAY 필수)이 필요합니다.': {
      EN: 'buyerPrefill.phone (JPAY 필수)이 필요합니다.',
      JP: 'buyerPrefill.phone (JPAY 필수)이 필요합니다.',
      CH: 'buyerPrefill.phone (JPAY 필수)이 필요합니다.',
      TH: 'buyerPrefill.phone (JPAY 필수)이 필요합니다.'
    },
    'CALLBACK·RESULT 한 쌍만 연결할 수 있습니다.': {
      EN: 'CALLBACK·RESULT 한 쌍만 연결할 수 있습니다.',
      JP: 'CALLBACK·RESULT 한 쌍만 연결할 수 있습니다.',
      CH: 'CALLBACK·RESULT 한 쌍만 연결할 수 있습니다.',
      TH: 'CALLBACK·RESULT 한 쌍만 연결할 수 있습니다.'
    },
    'CARD_PREFILL 모드에서는 buyerPrefill 객체가 필요합니다.': {
      EN: 'CARD_PREFILL 모드에서는 buyerPrefill 객체 is required.',
      JP: 'CARD_PREFILL 모드에서는 buyerPrefill 객체が必要です。',
      CH: '需要CARD_PREFILL 모드에서는 buyerPrefill 객체。',
      TH: 'ต้องระบุCARD_PREFILL 모드에서는 buyerPrefill 객체'
    },
    'CARD_PREFILL 모드에서는 buyerPrefill(이메일·전화 등)이 필요합니다.': {
      EN: 'CARD_PREFILL 모드에서는 buyerPrefill(이메일·전화 등)이 필요합니다.',
      JP: 'CARD_PREFILL 모드에서는 buyerPrefill(이메일·전화 등)이 필요합니다.',
      CH: 'CARD_PREFILL 모드에서는 buyerPrefill(이메일·전화 등)이 필요합니다.',
      TH: 'CARD_PREFILL 모드에서는 buyerPrefill(이메일·전화 등)이 필요합니다.'
    },
    'ChillPay 거래만 API 무효·환불을 호출할 수 있습니다.': {
      EN: 'ChillPay transaction만 API 무효·환불을 호출할 수 있습니다.',
      JP: 'ChillPay 取引만 API 무효·환불을 호출할 수 있습니다.',
      CH: 'ChillPay 交易만 API 무효·환불을 호출할 수 있습니다.',
      TH: 'ChillPay ธุรกรรม만 API 무효·환불을 호출할 수 있습니다.'
    },
    'ChillPay 루트(Route) 번호가 설정되지 않았습니다. (1) 가맹점 결제대행사의 루트번호 또는 ': {
      EN: 'ChillPay 루트(Route) 번호가 setting되지 않았습니다. (1) merchant payment gateway의 루트번호 또는 ',
      JP: 'ChillPay 루트(Route) 번호가 設定되지 않았습니다. (1) 加盟店 決済代行의 루트번호 또는 ',
      CH: 'ChillPay 루트(Route) 번호가 设置되지 않았습니다. (1) 加盟店 支付机构의 루트번호 또는 ',
      TH: 'ChillPay 루트(Route) 번호가 การตั้งค่า되지 않았습니다. (1) ร้านค้า ผู้ให้บริการชำระเงิน의 루트번호 또는 '
    },
    'ChillPay 루트(Route) 번호가 설정되지 않았습니다. 가맹점 등록 > 결제대행사 설정에서 해당 PG(': {
      EN: 'ChillPay 루트(Route) 번호가 setting되지 않았습니다. merchant 등록 > payment gateway setting에서 해당 PG(',
      JP: 'ChillPay 루트(Route) 번호가 設定되지 않았습니다. 加盟店 등록 > 決済代行 設定에서 해당 PG(',
      CH: 'ChillPay 루트(Route) 번호가 设置되지 않았습니다. 加盟店 등록 > 支付机构 设置에서 해당 PG(',
      TH: 'ChillPay 루트(Route) 번호가 การตั้งค่า되지 않았습니다. ร้านค้า 등록 > ผู้ให้บริการชำระเงิน การตั้งค่า에서 해당 PG('
    },
    'ChillPay 루트(Route) 번호가 설정되지 않았습니다. 배포설정 > API연동설정(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, ': {
      EN: 'ChillPay 루트(Route) 번호가 setting되지 않았습니다. 배포setting > API연동setting(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, ',
      JP: 'ChillPay 루트(Route) 번호가 設定되지 않았습니다. 배포設定 > API연동設定(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, ',
      CH: 'ChillPay 루트(Route) 번호가 设置되지 않았습니다. 배포设置 > API연동设置(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, ',
      TH: 'ChillPay 루트(Route) 번호가 การตั้งค่า되지 않았습니다. 배포การตั้งค่า > API연동การตั้งค่า(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, '
    },
    'ChillPay 루트(Route) 번호가 설정되지 않았습니다. 배포설정 > API연동설정에서 해당 ChillPay 행(pg_cd=': {
      EN: 'ChillPay 루트(Route) 번호가 setting되지 않았습니다. 배포setting > API연동setting에서 해당 ChillPay 행(pg_cd=',
      JP: 'ChillPay 루트(Route) 번호가 設定되지 않았습니다. 배포設定 > API연동設定에서 해당 ChillPay 행(pg_cd=',
      CH: 'ChillPay 루트(Route) 번호가 设置되지 않았습니다. 배포设置 > API연동设置에서 해당 ChillPay 행(pg_cd=',
      TH: 'ChillPay 루트(Route) 번호가 การตั้งค่า되지 않았습니다. 배포การตั้งค่า > API연동การตั้งค่า에서 해당 ChillPay 행(pg_cd='
    },
    'ChillPay 루트(Route) 번호를 설정할 수 없습니다. 배포설정 > API연동설정에서 ChillPay 결제대행사(Key·MD5·Route)를 등록하거나, ': {
      EN: 'ChillPay 루트(Route) 번호를 setting할 수 없습니다. 배포setting > API연동setting에서 ChillPay payment gateway(Key·MD5·Route)를 등록하거나, ',
      JP: 'ChillPay 루트(Route) 번호를 設定할 수 없습니다. 배포設定 > API연동設定에서 ChillPay 決済代行(Key·MD5·Route)를 등록하거나, ',
      CH: 'ChillPay 루트(Route) 번호를 设置할 수 없습니다. 배포设置 > API연동设置에서 ChillPay 支付机构(Key·MD5·Route)를 등록하거나, ',
      TH: 'ChillPay 루트(Route) 번호를 การตั้งค่า할 수 없습니다. 배포การตั้งค่า > API연동การตั้งค่า에서 ChillPay ผู้ให้บริการชำระเงิน(Key·MD5·Route)를 등록하거나, '
    },
    'ChillPay 응답 본문이 비어 있습니다.': {
      EN: 'ChillPay 응답 본문이 비어 있습니다.',
      JP: 'ChillPay 응답 본문이 비어 있습니다.',
      CH: 'ChillPay 응답 본문이 비어 있습니다.',
      TH: 'ChillPay 응답 본문이 비어 있습니다.'
    },
    'ChillPay 정산 검색 요청 JSON 직렬화 실패': {
      EN: 'ChillPay settlement 검색 요청 JSON 직렬화 실패',
      JP: 'ChillPay 精算 검색 요청 JSON 직렬화 실패',
      CH: 'ChillPay 结算 검색 요청 JSON 직렬화 실패',
      TH: 'ChillPay การชำระบัญชี 검색 요청 JSON 직렬화 실패'
    },
    'ChillPay API Key·MD5(또는 가맹점 IV)가 비어 있습니다. 가맹점 결제대행사 행에 입력하거나, ': {
      EN: 'ChillPay API Key·MD5(또는 merchant IV)가 비어 있습니다. merchant payment gateway 행에 입력하거나, ',
      JP: 'ChillPay API Key·MD5(또는 加盟店 IV)가 비어 있습니다. 加盟店 決済代行 행에 입력하거나, ',
      CH: 'ChillPay API Key·MD5(또는 加盟店 IV)가 비어 있습니다. 加盟店 支付机构 행에 입력하거나, ',
      TH: 'ChillPay API Key·MD5(또는 ร้านค้า IV)가 비어 있습니다. ร้านค้า ผู้ให้บริการชำระเงิน 행에 입력하거나, '
    },
    'ChillPay API Key가 설정되지 않았습니다.': {
      EN: 'ChillPay API Key가 setting되지 않았습니다.',
      JP: 'ChillPay API Key가 設定되지 않았습니다.',
      CH: 'ChillPay API Key가 设置되지 않았습니다.',
      TH: 'ChillPay API Key가 การตั้งค่า되지 않았습니다.'
    },
    'ChillPay API Key가 설정되지 않았습니다. 배포설정 > API배포설정 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.': {
      EN: 'ChillPay API Key가 setting되지 않았습니다. 배포setting > API배포setting 또는 merchant 등록 > payment gateway setting에서 ChillPay information를 입력하세요.',
      JP: 'ChillPay API Key가 設定되지 않았습니다. 배포設定 > API배포設定 또는 加盟店 등록 > 決済代行 設定에서 ChillPay 情報를 입력하세요.',
      CH: 'ChillPay API Key가 设置되지 않았습니다. 배포设置 > API배포设置 또는 加盟店 등록 > 支付机构 设置에서 ChillPay 信息를 입력하세요.',
      TH: 'ChillPay API Key가 การตั้งค่า되지 않았습니다. 배포การตั้งค่า > API배포การตั้งค่า 또는 ร้านค้า 등록 > ผู้ให้บริการชำระเงิน การตั้งค่า에서 ChillPay ข้อมูล를 입력하세요.'
    },
    'ChillPay MD5 Key가 설정되지 않았습니다.': {
      EN: 'ChillPay MD5 Key가 setting되지 않았습니다.',
      JP: 'ChillPay MD5 Key가 設定되지 않았습니다.',
      CH: 'ChillPay MD5 Key가 设置되지 않았습니다.',
      TH: 'ChillPay MD5 Key가 การตั้งค่า되지 않았습니다.'
    },
    'ChillPay MD5 Key가 설정되지 않았습니다. 배포설정 > API배포설정 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.': {
      EN: 'ChillPay MD5 Key가 setting되지 않았습니다. 배포setting > API배포setting 또는 merchant 등록 > payment gateway setting에서 ChillPay information를 입력하세요.',
      JP: 'ChillPay MD5 Key가 設定되지 않았습니다. 배포設定 > API배포設定 또는 加盟店 등록 > 決済代行 設定에서 ChillPay 情報를 입력하세요.',
      CH: 'ChillPay MD5 Key가 设置되지 않았습니다. 배포设置 > API배포设置 또는 加盟店 등록 > 支付机构 设置에서 ChillPay 信息를 입력하세요.',
      TH: 'ChillPay MD5 Key가 การตั้งค่า되지 않았습니다. 배포การตั้งค่า > API배포การตั้งค่า 또는 ร้านค้า 등록 > ผู้ให้บริการชำระเงิน การตั้งค่า에서 ChillPay ข้อมูล를 입력하세요.'
    },
    'ChillPay TransactionId가 없어 API 후속조치를 호출할 수 없습니다.': {
      EN: 'ChillPay TransactionId가 없어 API 후속조치를 호출할 수 없습니다.',
      JP: 'ChillPay TransactionId가 없어 API 후속조치를 호출할 수 없습니다.',
      CH: 'ChillPay TransactionId가 없어 API 후속조치를 호출할 수 없습니다.',
      TH: 'ChillPay TransactionId가 없어 API 후속조치를 호출할 수 없습니다.'
    },
    'compId 또는 merchantId가 필요합니다.': {
      EN: 'compId 또는 merchantId is required.',
      JP: 'compId 또는 merchantIdが必要です。',
      CH: '需要compId 또는 merchantId。',
      TH: 'ต้องระบุcompId 또는 merchantId'
    },
    'compId가 필요합니다.': {
      EN: 'compId is required.',
      JP: 'compIdが必要です。',
      CH: '需要compId。',
      TH: 'ต้องระบุcompId'
    },
    'D 모드는 일괄 정산 시각(HH:mm)이 필요합니다.': {
      EN: 'D 모드는 일괄 settlement 시각(HH:mm)이 필요합니다.',
      JP: 'D 모드는 일괄 精算 시각(HH:mm)이 필요합니다.',
      CH: 'D 모드는 일괄 结算 시각(HH:mm)이 필요합니다.',
      TH: 'D 모드는 일괄 การชำระบัญชี 시각(HH:mm)이 필요합니다.'
    },
    'D 모드는 정산 일괄 시각(HH:mm)이 필요합니다.': {
      EN: 'D 모드는 settlement 일괄 시각(HH:mm)이 필요합니다.',
      JP: 'D 모드는 精算 일괄 시각(HH:mm)이 필요합니다.',
      CH: 'D 모드는 结算 일괄 시각(HH:mm)이 필요합니다.',
      TH: 'D 모드는 การชำระบัญชี 일괄 시각(HH:mm)이 필요합니다.'
    },
    'D+N 은 0~90만 허용됩니다.': {
      EN: 'D+N 은 0~90만 허용됩니다.',
      JP: 'D+N 은 0~90만 허용됩니다.',
      CH: 'D+N 은 0~90만 허용됩니다.',
      TH: 'D+N 은 0~90만 허용됩니다.'
    },
    'D+N 일수를 입력하세요.': {
      EN: 'D+N 일수를 입력하세요.',
      JP: 'D+N 일수를 입력하세요.',
      CH: 'D+N 일수를 입력하세요.',
      TH: 'D+N 일수를 입력하세요.'
    },
    'dev 프로파일로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.': {
      EN: 'dev 프로file로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.',
      JP: 'dev 프로ファイル로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.',
      CH: 'dev 프로文件로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.',
      TH: 'dev 프로ไฟล์로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.'
    },
    'dev 프로파일에서만 사용할 수 있습니다.': {
      EN: 'Only dev 프로file에서 can use this.',
      JP: 'dev 프로ファイル에서のみ使用できます。',
      CH: '仅dev 프로文件에서可使用。',
      TH: 'เฉพาะdev 프로ไฟล์에서เท่านั้นที่ใช้ได้'
    },
    'feeCurrencyFormatJson 은 배열(JSON)이어야 합니다.': {
      EN: 'feeCurrencyFormatJson 은 배열(JSON)이어야 합니다.',
      JP: 'feeCurrencyFormatJson 은 배열(JSON)이어야 합니다.',
      CH: 'feeCurrencyFormatJson 은 배열(JSON)이어야 합니다.',
      TH: 'feeCurrencyFormatJson 은 배열(JSON)이어야 합니다.'
    },
    'fieldMappings.internalKey 은 비울 수 없습니다.': {
      EN: 'fieldMappings.internalKey 은 비울 수 없습니다.',
      JP: 'fieldMappings.internalKey 은 비울 수 없습니다.',
      CH: 'fieldMappings.internalKey 은 비울 수 없습니다.',
      TH: 'fieldMappings.internalKey 은 비울 수 없습니다.'
    },
    'Google OTP 등록 후 진행할 수 있습니다.': {
      EN: 'Google OTP 등록 후 진행할 수 있습니다.',
      JP: 'Google OTP 등록 후 진행할 수 있습니다.',
      CH: 'Google OTP 등록 후 진행할 수 있습니다.',
      TH: 'Google OTP 등록 후 진행할 수 있습니다.'
    },
    'Google OTP 앱의 6자리 코드가 올바르지 않습니다.': {
      EN: 'Google OTP 앱의 6자리 code가 올바르지 않습니다.',
      JP: 'Google OTP 앱의 6자리 コード가 올바르지 않습니다.',
      CH: 'Google OTP 앱의 6자리 代码가 올바르지 않습니다.',
      TH: 'Google OTP 앱의 6자리 รหัส가 올바르지 않습니다.'
    },
    'headers가 비어 있습니다.': {
      EN: 'headers가 비어 있습니다.',
      JP: 'headers가 비어 있습니다.',
      CH: 'headers가 비어 있습니다.',
      TH: 'headers가 비어 있습니다.'
    },
    'ID 형식이 올바르지 않습니다.': {
      EN: 'Invalid ID format.',
      JP: 'IDの形式が正しくありません。',
      CH: 'ID格式不正确。',
      TH: 'รูปแบบIDไม่ถูกต้อง'
    },
    'id가 필요합니다.': {
      EN: 'id is required.',
      JP: 'idが必要です。',
      CH: '需要id。',
      TH: 'ต้องระบุid'
    },
    'imageSlot는 1~4만 허용됩니다.': {
      EN: 'imageSlot는 1~4만 허용됩니다.',
      JP: 'imageSlot는 1~4만 허용됩니다.',
      CH: 'imageSlot는 1~4만 허용됩니다.',
      TH: 'imageSlot는 1~4만 허용됩니다.'
    },
    'imageType은 main, logo, first, popcon 또는 urlPay이어야 합니다.': {
      EN: 'imageType은 main, logo, first, popcon 또는 urlPay이어야 합니다.',
      JP: 'imageType은 main, logo, first, popcon 또는 urlPay이어야 합니다.',
      CH: 'imageType은 main, logo, first, popcon 또는 urlPay이어야 합니다.',
      TH: 'imageType은 main, logo, first, popcon 또는 urlPay이어야 합니다.'
    },
    'JPAY 거래는 결제 후속조치(무효·환불)를 지원하지 않습니다. PG 운영 처리 및 노티 반영으로 확인하세요.': {
      EN: 'JPAY transaction는 결제 후속조치(무효·환불)를 지원하지 않습니다. PG 운영 처리 및 notification 반영으로 확인하세요.',
      JP: 'JPAY 取引는 결제 후속조치(무효·환불)를 지원하지 않습니다. PG 운영 처리 및 通知 반영으로 확인하세요.',
      CH: 'JPAY 交易는 결제 후속조치(무효·환불)를 지원하지 않습니다. PG 운영 처리 및 通知 반영으로 확인하세요.',
      TH: 'JPAY ธุรกรรม는 결제 후속조치(무효·환불)를 지원하지 않습니다. PG 운영 처리 및 การแจ้งเตือน 반영으로 확인하세요.'
    },
    'JPAY 구독은 /api/pay/jpay/subscribe 또는 subscription/prepare API를 사용하세요.': {
      EN: 'JPAY 구독은 /api/pay/jpay/subscribe 또는 subscription/prepare API를 사용하세요.',
      JP: 'JPAY 구독은 /api/pay/jpay/subscribe 또는 subscription/prepare API를 사용하세요.',
      CH: 'JPAY 구독은 /api/pay/jpay/subscribe 또는 subscription/prepare API를 사용하세요.',
      TH: 'JPAY 구독은 /api/pay/jpay/subscribe 또는 subscription/prepare API를 사용하세요.'
    },
    'JPAY API 구독(운영) 바인딩이 없습니다.': {
      EN: 'JPAY API 구독(운영) 바인딩이 없습니다.',
      JP: 'JPAY API 구독(운영) 바인딩이 없습니다.',
      CH: 'JPAY API 구독(운영) 바인딩이 없습니다.',
      TH: 'JPAY API 구독(운영) 바인딩이 없습니다.'
    },
    'JPAY MID·API Key(tb_pg_agency)를 설정하세요.': {
      EN: 'JPAY MID·API Key(tb_pg_agency)를 setting하세요.',
      JP: 'JPAY MID·API Key(tb_pg_agency)를 設定하세요.',
      CH: 'JPAY MID·API Key(tb_pg_agency)를 设置하세요.',
      TH: 'JPAY MID·API Key(tb_pg_agency)를 การตั้งค่า하세요.'
    },
    'JPAY MID·API Key를 설정하세요.': {
      EN: 'JPAY MID·API Key를 setting하세요.',
      JP: 'JPAY MID·API Key를 設定하세요.',
      CH: 'JPAY MID·API Key를 设置하세요.',
      TH: 'JPAY MID·API Key를 การตั้งค่า하세요.'
    },
    'JPAY URL 결제(운영) 바인딩이 없습니다. 결제대행사에 JPAY·URL결제를 등록하세요.': {
      EN: 'JPAY URL 결제(운영) 바인딩이 없습니다. payment gateway에 JPAY·URL결제를 등록하세요.',
      JP: 'JPAY URL 결제(운영) 바인딩이 없습니다. 決済代行에 JPAY·URL결제를 등록하세요.',
      CH: 'JPAY URL 결제(운영) 바인딩이 없습니다. 支付机构에 JPAY·URL결제를 등록하세요.',
      TH: 'JPAY URL 결제(운영) 바인딩이 없습니다. ผู้ให้บริการชำระเงิน에 JPAY·URL결제를 등록하세요.'
    },
    'JSON 은 객체 형태여야 합니다.': {
      EN: 'JSON 은 객체 형태여야 합니다.',
      JP: 'JSON 은 객체 형태여야 합니다.',
      CH: 'JSON 은 객체 형태여야 합니다.',
      TH: 'JSON 은 객체 형태여야 합니다.'
    },
    'LINE Notify 토큰이 비어 있습니다.': {
      EN: 'LINE Notify 토큰이 비어 있습니다.',
      JP: 'LINE Notify 토큰이 비어 있습니다.',
      CH: 'LINE Notify 토큰이 비어 있습니다.',
      TH: 'LINE Notify 토큰이 비어 있습니다.'
    },
    'LLM 응답이 비어 있습니다. 잠시 후 다시 시도하거나 본사 AI설정(모델·키)을 확인하세요.': {
      EN: 'LLM 응답이 비어 있습니다. 잠시 후 다시 시도하거나 head office AIsetting(모델·키)을 확인하세요.',
      JP: 'LLM 응답이 비어 있습니다. 잠시 후 다시 시도하거나 本社 AI設定(모델·키)을 확인하세요.',
      CH: 'LLM 응답이 비어 있습니다. 잠시 후 다시 시도하거나 总部 AI设置(모델·키)을 확인하세요.',
      TH: 'LLM 응답이 비어 있습니다. 잠시 후 다시 시도하거나 สำนักงานใหญ่ AIการตั้งค่า(모델·키)을 확인하세요.'
    },
    'mappingDefinitionJson 이 필요합니다.': {
      EN: 'mappingDefinitionJson 이 필요합니다.',
      JP: 'mappingDefinitionJson 이 필요합니다.',
      CH: 'mappingDefinitionJson 이 필요합니다.',
      TH: 'mappingDefinitionJson 이 필요합니다.'
    },
    'matrix 형식이 올바르지 않습니다.': {
      EN: 'Invalid matrix format.',
      JP: 'matrixの形式が正しくありません。',
      CH: 'matrix格式不正确。',
      TH: 'รูปแบบmatrixไม่ถูกต้อง'
    },
    'merchantId·amount(양수)': {
      EN: 'merchantId·amount(양수)',
      JP: 'merchantId·amount(양수)',
      CH: 'merchantId·amount(양수)',
      TH: 'merchantId·amount(양수)'
    },
    'messages가 비어 있습니다.': {
      EN: 'messages가 비어 있습니다.',
      JP: 'messages가 비어 있습니다.',
      CH: 'messages가 비어 있습니다.',
      TH: 'messages가 비어 있습니다.'
    },
    'mode는 AUTO 또는 MANUAL 이어야 합니다.': {
      EN: 'mode는 AUTO 또는 MANUAL 이어야 합니다.',
      JP: 'mode는 AUTO 또는 MANUAL 이어야 합니다.',
      CH: 'mode는 AUTO 또는 MANUAL 이어야 합니다.',
      TH: 'mode는 AUTO 또는 MANUAL 이어야 합니다.'
    },
    'NONE 또는 빈 코드는 여기서 등록하지 않습니다.': {
      EN: 'NONE 또는 빈 code는 여기서 등록하지 않습니다.',
      JP: 'NONE 또는 빈 コード는 여기서 등록하지 않습니다.',
      CH: 'NONE 또는 빈 代码는 여기서 등록하지 않습니다.',
      TH: 'NONE 또는 빈 รหัส는 여기서 등록하지 않습니다.'
    },
    'NOTI 미수신 건은 본 기능으로 처리할 수 없습니다.': {
      EN: 'NOTI 미수신 건은 본 기능으로 처리할 수 없습니다.',
      JP: 'NOTI 미수신 건은 본 기능으로 처리할 수 없습니다.',
      CH: 'NOTI 미수신 건은 본 기능으로 처리할 수 없습니다.',
      TH: 'NOTI 미수신 건은 본 기능으로 처리할 수 없습니다.'
    },
    'operationalPgCds 형식이 올바르지 않습니다.': {
      EN: 'Invalid operationalPgCds format.',
      JP: 'operationalPgCdsの形式が正しくありません。',
      CH: 'operationalPgCds格式不正确。',
      TH: 'รูปแบบoperationalPgCdsไม่ถูกต้อง'
    },
    'orderNo가 필요합니다.': {
      EN: 'orderNo is required.',
      JP: 'orderNoが必要です。',
      CH: '需要orderNo。',
      TH: 'ต้องระบุorderNo'
    },
    'orgUnitId 형식이 올바르지 않습니다.': {
      EN: 'Invalid orgUnitId format.',
      JP: 'orgUnitIdの形式が正しくありません。',
      CH: 'orgUnitId格式不正确。',
      TH: 'รูปแบบorgUnitIdไม่ถูกต้อง'
    },
    'orgUnitId가 필요합니다.': {
      EN: 'orgUnitId is required.',
      JP: 'orgUnitIdが必要です。',
      CH: '需要orgUnitId。',
      TH: 'ต้องระบุorgUnitId'
    },
    'OTP 시크릿이 비어 있습니다.': {
      EN: 'OTP 시크릿이 비어 있습니다.',
      JP: 'OTP 시크릿이 비어 있습니다.',
      CH: 'OTP 시크릿이 비어 있습니다.',
      TH: 'OTP 시크릿이 비어 있습니다.'
    },
    'OTP 코드가 올바르지 않습니다.': {
      EN: 'OTP code가 올바르지 않습니다.',
      JP: 'OTP コード가 올바르지 않습니다.',
      CH: 'OTP 代码가 올바르지 않습니다.',
      TH: 'OTP รหัส가 올바르지 않습니다.'
    },
    'OTP 코드를 입력하세요.': {
      EN: 'OTP code를 입력하세요.',
      JP: 'OTP コード를 입력하세요.',
      CH: 'OTP 代码를 입력하세요.',
      TH: 'OTP รหัส를 입력하세요.'
    },
    'OTP URI 생성 실패': {
      EN: 'OTP URI 생성 실패',
      JP: 'OTP URI 생성 실패',
      CH: 'OTP URI 생성 실패',
      TH: 'OTP URI 생성 실패'
    },
    'OTP를 입력하세요.': {
      EN: 'OTP를 입력하세요.',
      JP: 'OTP를 입력하세요.',
      CH: 'OTP를 입력하세요.',
      TH: 'OTP를 입력하세요.'
    },
    'OTP를 초기화할 사용자를 찾을 수 없습니다.': {
      EN: 'OTP를 초기화할 사용자 could not be found.',
      JP: 'OTP를 초기화할 사용자が見つかりません。',
      CH: '未找到OTP를 초기화할 사용자。',
      TH: 'ไม่พบOTP를 초기화할 사용자'
    },
    'payCountryIsoCode2(국가코드 ISO2)가 필요합니다.': {
      EN: 'payCountryIsoCode2(국가code ISO2) is required.',
      JP: 'payCountryIsoCode2(국가コード ISO2)が必要です。',
      CH: '需要payCountryIsoCode2(국가代码 ISO2)。',
      TH: 'ต้องระบุpayCountryIsoCode2(국가รหัส ISO2)'
    },
    'payEmailAddress(이메일)가 필요합니다.': {
      EN: 'payEmailAddress(이메일) is required.',
      JP: 'payEmailAddress(이메일)が必要です。',
      CH: '需要payEmailAddress(이메일)。',
      TH: 'ต้องระบุpayEmailAddress(이메일)'
    },
    'payFollowLevelCaps 항목이 필요합니다.': {
      EN: 'payFollowLevelCaps 항목이 필요합니다.',
      JP: 'payFollowLevelCaps 항목이 필요합니다.',
      CH: 'payFollowLevelCaps 항목이 필요합니다.',
      TH: 'payFollowLevelCaps 항목이 필요합니다.'
    },
    'PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.': {
      EN: 'PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.',
      JP: 'PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.',
      CH: 'PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.',
      TH: 'PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.'
    },
    'payTelephone(전화번호)가 필요합니다.': {
      EN: 'payTelephone(전화번호) is required.',
      JP: 'payTelephone(전화번호)が必要です。',
      CH: '需要payTelephone(전화번호)。',
      TH: 'ต้องระบุpayTelephone(전화번호)'
    },
    'PG 정산예정 모드는 INHERIT, OFF, T, D 중 하나입니다.': {
      EN: 'PG settlement예정 모드는 INHERIT, OFF, T, D 중 하나입니다.',
      JP: 'PG 精算예정 모드는 INHERIT, OFF, T, D 중 하나입니다.',
      CH: 'PG 结算예정 모드는 INHERIT, OFF, T, D 중 하나입니다.',
      TH: 'PG การชำระบัญชี예정 모드는 INHERIT, OFF, T, D 중 하나입니다.'
    },
    'PG 정산예정 N은 1~10 정수입니다.': {
      EN: 'PG settlement예정 N은 1~10 정수입니다.',
      JP: 'PG 精算예정 N은 1~10 정수입니다.',
      CH: 'PG 结算예정 N은 1~10 정수입니다.',
      TH: 'PG การชำระบัญชี예정 N은 1~10 정수입니다.'
    },
    'PG 정산예정 N은 1~10만 허용됩니다.': {
      EN: 'PG settlement예정 N은 1~10만 허용됩니다.',
      JP: 'PG 精算예정 N은 1~10만 허용됩니다.',
      CH: 'PG 结算예정 N은 1~10만 허용됩니다.',
      TH: 'PG การชำระบัญชี예정 N은 1~10만 허용됩니다.'
    },
    'PG 정산예정: T/D 모드일 때 N(1~10)이 필요합니다.': {
      EN: 'PG settlement예정: T/D 모드일 때 N(1~10)이 필요합니다.',
      JP: 'PG 精算예정: T/D 모드일 때 N(1~10)이 필요합니다.',
      CH: 'PG 结算예정: T/D 모드일 때 N(1~10)이 필요합니다.',
      TH: 'PG การชำระบัญชี예정: T/D 모드일 때 N(1~10)이 필요합니다.'
    },
    'PG사 연동 행을 찾을 수 없습니다.': {
      EN: 'PG사 연동 행 could not be found.',
      JP: 'PG사 연동 행が見つかりません。',
      CH: '未找到PG사 연동 행。',
      TH: 'ไม่พบPG사 연동 행'
    },
    'PG사 연동(tb_pg_agency) 행을 찾을 수 없습니다.': {
      EN: 'PG사 연동(tb_pg_agency) 행 could not be found.',
      JP: 'PG사 연동(tb_pg_agency) 행が見つかりません。',
      CH: '未找到PG사 연동(tb_pg_agency) 행。',
      TH: 'ไม่พบPG사 연동(tb_pg_agency) 행'
    },
    'PG사 정보를 찾을 수 없습니다.': {
      EN: 'PG사 information could not be found.',
      JP: 'PG사 情報が見つかりません。',
      CH: '未找到PG사 信息。',
      TH: 'ไม่พบPG사 ข้อมูล'
    },
    'PG중계 콜백 시크릿은 256자 이하여야 합니다.': {
      EN: 'PG중계 콜백 시크릿은 256자 이하여야 합니다.',
      JP: 'PG중계 콜백 시크릿은 256자 이하여야 합니다.',
      CH: 'PG중계 콜백 시크릿은 256자 이하여야 합니다.',
      TH: 'PG중계 콜백 시크릿은 256자 이하여야 합니다.'
    },
    'PNG 또는 JPG 파일만 업로드 가능합니다.': {
      EN: 'Only PNG 또는 JPG file can upload.',
      JP: 'PNG 또는 JPG ファイルのみアップロードできます。',
      CH: '仅PNG 또는 JPG 文件可上传。',
      TH: 'เฉพาะPNG 또는 JPG ไฟล์เท่านั้นที่อัปโหลดได้'
    },
    'PNG 또는 JPG만 가능합니다.': {
      EN: 'PNG 또는 JPG만 가능합니다.',
      JP: 'PNG 또는 JPG만 가능합니다.',
      CH: 'PNG 또는 JPG만 가능합니다.',
      TH: 'PNG 또는 JPG만 가능합니다.'
    },
    'PNG 또는 JPG만 업로드할 수 있습니다.': {
      EN: 'Only PNG 또는 JPG can upload.',
      JP: 'PNG 또는 JPGのみアップロードできます。',
      CH: '仅PNG 또는 JPG可上传。',
      TH: 'เฉพาะPNG 또는 JPGเท่านั้นที่อัปโหลดได้'
    },
    'PNG 변환 저장에 실패했습니다.': {
      EN: 'PNG 변환 저장에 실패했습니다.',
      JP: 'PNG 변환 저장에 실패했습니다.',
      CH: 'PNG 변환 저장에 실패했습니다.',
      TH: 'PNG 변환 저장에 실패했습니다.'
    },
    'publishTab 은 PENDING 또는 HOLD 여야 합니다.': {
      EN: 'publishTab 은 PENDING 또는 HOLD 여야 합니다.',
      JP: 'publishTab 은 PENDING 또는 HOLD 여야 합니다.',
      CH: 'publishTab 은 PENDING 또는 HOLD 여야 합니다.',
      TH: 'publishTab 은 PENDING 또는 HOLD 여야 합니다.'
    },
    'roundMode 는 CEILING, HALF_UP, DOWN 중 하나여야 합니다.': {
      EN: 'roundMode 는 CEILING, HALF_UP, DOWN 중 하나여야 합니다.',
      JP: 'roundMode 는 CEILING, HALF_UP, DOWN 중 하나여야 합니다.',
      CH: 'roundMode 는 CEILING, HALF_UP, DOWN 중 하나여야 합니다.',
      TH: 'roundMode 는 CEILING, HALF_UP, DOWN 중 하나여야 합니다.'
    },
    'Route No는 정수여야 합니다.': {
      EN: 'Route No는 정수여야 합니다.',
      JP: 'Route No는 정수여야 합니다.',
      CH: 'Route No는 정수여야 합니다.',
      TH: 'Route No는 정수여야 합니다.'
    },
    'scope=MASTER_DIST 인 경우 총판 코드여야 합니다.': {
      EN: 'scope=MASTER_DIST 인 경우 master distributor code여야 합니다.',
      JP: 'scope=MASTER_DIST 인 경우 総販 コード여야 합니다.',
      CH: 'scope=MASTER_DIST 인 경우 总经销 代码여야 합니다.',
      TH: 'scope=MASTER_DIST 인 경우 ตัวแทนหลัก รหัส여야 합니다.'
    },
    'scope=MERCHANT 인 경우 가맹점 코드여야 합니다.': {
      EN: 'scope=MERCHANT 인 경우 merchant code여야 합니다.',
      JP: 'scope=MERCHANT 인 경우 加盟店 コード여야 합니다.',
      CH: 'scope=MERCHANT 인 경우 加盟店 代码여야 합니다.',
      TH: 'scope=MERCHANT 인 경우 ร้านค้า รหัส여야 합니다.'
    },
    'scope는 ALL, RUNS, RECEIVABLES, RECOVERY, ROLLING, DEDUCTIONS 중 하나입니다.': {
      EN: 'scope는 ALL, RUNS, RECEIVABLES, RECOVERY, ROLLING, DEDUCTIONS 중 하나입니다.',
      JP: 'scope는 ALL, RUNS, RECEIVABLES, RECOVERY, ROLLING, DEDUCTIONS 중 하나입니다.',
      CH: 'scope는 ALL, RUNS, RECEIVABLES, RECOVERY, ROLLING, DEDUCTIONS 중 하나입니다.',
      TH: 'scope는 ALL, RUNS, RECEIVABLES, RECOVERY, ROLLING, DEDUCTIONS 중 하나입니다.'
    },
    'sessionToken(세션)이 필요합니다.': {
      EN: 'sessionToken(세션)이 필요합니다.',
      JP: 'sessionToken(세션)이 필요합니다.',
      CH: 'sessionToken(세션)이 필요합니다.',
      TH: 'sessionToken(세션)이 필요합니다.'
    },
    'settlementCronZoneId(또는 zoneOrPreset)가 필요합니다.': {
      EN: 'settlementCronZoneId(또는 zoneOrPreset) is required.',
      JP: 'settlementCronZoneId(또는 zoneOrPreset)が必要です。',
      CH: '需要settlementCronZoneId(또는 zoneOrPreset)。',
      TH: 'ต้องระบุsettlementCronZoneId(또는 zoneOrPreset)'
    },
    'settlementRunIds 또는 settlementRunId가 필요합니다.': {
      EN: 'settlementRunIds 또는 settlementRunId is required.',
      JP: 'settlementRunIds 또는 settlementRunIdが必要です。',
      CH: '需要settlementRunIds 또는 settlementRunId。',
      TH: 'ต้องระบุsettlementRunIds 또는 settlementRunId'
    },
    'SMTP 인증이 켜져 있으나 사용자·비밀번호가 없습니다.': {
      EN: 'SMTP authentication이 켜져 있으나 사용자·비밀번호가 없습니다.',
      JP: 'SMTP 認証이 켜져 있으나 사용자·비밀번호가 없습니다.',
      CH: 'SMTP 认证이 켜져 있으나 사용자·비밀번호가 없습니다.',
      TH: 'SMTP การยืนยันตัวตน이 켜져 있으나 사용자·비밀번호가 없습니다.'
    },
    'SMTP 호스트가 설정되지 않았습니다. 전산설정관리에서 SMTP를 입력하세요.': {
      EN: 'SMTP 호스트가 setting되지 않았습니다. 전산setting관리에서 SMTP를 입력하세요.',
      JP: 'SMTP 호스트가 設定되지 않았습니다. 전산設定관리에서 SMTP를 입력하세요.',
      CH: 'SMTP 호스트가 设置되지 않았습니다. 전산设置관리에서 SMTP를 입력하세요.',
      TH: 'SMTP 호스트가 การตั้งค่า되지 않았습니다. 전산การตั้งค่า관리에서 SMTP를 입력하세요.'
    },
    'subscriptionPlan이 필요합니다.': {
      EN: 'subscriptionPlan이 필요합니다.',
      JP: 'subscriptionPlan이 필요합니다.',
      CH: 'subscriptionPlan이 필요합니다.',
      TH: 'subscriptionPlan이 필요합니다.'
    },
    'T/D 모드일 때 N(1~10)이 필요합니다.': {
      EN: 'T/D 모드일 때 N(1~10)이 필요합니다.',
      JP: 'T/D 모드일 때 N(1~10)이 필요합니다.',
      CH: 'T/D 모드일 때 N(1~10)이 필요합니다.',
      TH: 'T/D 모드일 때 N(1~10)이 필요합니다.'
    },
    'targetIds가 필요합니다.': {
      EN: 'targetIds is required.',
      JP: 'targetIdsが必要です。',
      CH: '需要targetIds。',
      TH: 'ต้องระบุtargetIds'
    },
    'targetIds에 유효하지 않은 값이 있습니다.': {
      EN: 'targetIds에 유효하지 않은 값이 있습니다.',
      JP: 'targetIds에 유효하지 않은 값이 있습니다.',
      CH: 'targetIds에 유효하지 않은 값이 있습니다.',
      TH: 'targetIds에 유효하지 않은 값이 있습니다.'
    },
    'titleKo·body1Ko·body2Ko·body3Ko는 모두 필요합니다.': {
      EN: 'titleKo·body1Ko·body2Ko·body3Ko는 모두 필요합니다.',
      JP: 'titleKo·body1Ko·body2Ko·body3Ko는 모두 필요합니다.',
      CH: 'titleKo·body1Ko·body2Ko·body3Ko는 모두 필요합니다.',
      TH: 'titleKo·body1Ko·body2Ko·body3Ko는 모두 필요합니다.'
    },
    'TOTP 계산 실패': {
      EN: 'TOTP 계산 실패',
      JP: 'TOTP 계산 실패',
      CH: 'TOTP 계산 실패',
      TH: 'TOTP 계산 실패'
    },
    'URL 결제를 처리할 결제대행사(운영·연동용도 URL결제)가 없습니다.': {
      EN: 'URL 결제를 처리할 payment gateway(운영·연동용도 URL결제)가 없습니다.',
      JP: 'URL 결제를 처리할 決済代行(운영·연동용도 URL결제)가 없습니다.',
      CH: 'URL 결제를 처리할 支付机构(운영·연동용도 URL결제)가 없습니다.',
      TH: 'URL 결제를 처리할 ผู้ให้บริการชำระเงิน(운영·연동용도 URL결제)가 없습니다.'
    },
    'URL 재결제를 처리할 결제대행사(운영·연동용도 URL재결제)가 없습니다.': {
      EN: 'URL 재결제를 처리할 payment gateway(운영·연동용도 URL재결제)가 없습니다.',
      JP: 'URL 재결제를 처리할 決済代行(운영·연동용도 URL재결제)가 없습니다.',
      CH: 'URL 재결제를 처리할 支付机构(운영·연동용도 URL재결제)가 없습니다.',
      TH: 'URL 재결제를 처리할 ผู้ให้บริการชำระเงิน(운영·연동용도 URL재결제)가 없습니다.'
    },
    'vendors[].displayMaps 는 객체(JSON)여야 합니다.': {
      EN: 'vendors[].displayMaps 는 객체(JSON)여야 합니다.',
      JP: 'vendors[].displayMaps 는 객체(JSON)여야 합니다.',
      CH: 'vendors[].displayMaps 는 객체(JSON)여야 합니다.',
      TH: 'vendors[].displayMaps 는 객체(JSON)여야 합니다.'
    },
    'vendorScope 가 올바르지 않습니다.': {
      EN: 'vendorScope 가 올바르지 않습니다.',
      JP: 'vendorScope 가 올바르지 않습니다.',
      CH: 'vendorScope 가 올바르지 않습니다.',
      TH: 'vendorScope 가 올바르지 않습니다.'
    },
    'W+N 은 1~28만 허용됩니다.': {
      EN: 'W+N 은 1~28만 허용됩니다.',
      JP: 'W+N 은 1~28만 허용됩니다.',
      CH: 'W+N 은 1~28만 허용됩니다.',
      TH: 'W+N 은 1~28만 허용됩니다.'
    },
    'W+N 일수를 입력하세요.': {
      EN: 'W+N 일수를 입력하세요.',
      JP: 'W+N 일수를 입력하세요.',
      CH: 'W+N 일수를 입력하세요.',
      TH: 'W+N 일수를 입력하세요.'
    },
    'WK 코드를 선택하세요.': {
      EN: 'WK code를 선택하세요.',
      JP: 'WK コード를 선택하세요.',
      CH: 'WK 代码를 선택하세요.',
      TH: 'WK รหัส를 선택하세요.'
    },
    'xlsx 또는 xls 파일만 업로드 가능합니다.': {
      EN: 'Only xlsx 또는 xls file can upload.',
      JP: 'xlsx 또는 xls ファイルのみアップロードできます。',
      CH: '仅xlsx 또는 xls 文件可上传。',
      TH: 'เฉพาะxlsx 또는 xls ไฟล์เท่านั้นที่อัปโหลดได้'
    },
    기타: {
      EN: 'Other',
      JP: 'その他',
      CH: '其他',
      TH: 'อื่นๆ'
    }
  };

  function normalizeLocale(loc) {
    var u = String(loc || 'KO').toUpperCase();
    if (u === 'KR') return 'KO';
    return ['KO', 'EN', 'JP', 'CH', 'TH'].indexOf(u) >= 0 ? u : 'KO';
  }

  function autoDetectLocaleFromNavigator() {
    try {
      var raw = '';
      if (typeof navigator !== 'undefined') {
        raw = String(navigator.language || (navigator.languages && navigator.languages[0]) || '').trim();
      }
      if (!raw) return 'EN';
      var s = raw.replace('_', '-').toLowerCase(); // e.g. ja-jp, zh-cn, th-th
      var lang = s.split('-')[0] || '';
      var region = (s.split('-')[1] || '').toUpperCase();
      if (lang === 'ko') return 'KO';
      if (lang === 'ja') return 'JP';
      if (lang === 'th') return 'TH';
      if (lang === 'zh') return 'CH';
      if (lang === 'en') return 'EN';
      // If region strongly implies a supported locale
      if (region === 'KR') return 'KO';
      if (region === 'JP') return 'JP';
      if (region === 'TH') return 'TH';
      if (region === 'CN' || region === 'TW' || region === 'HK' || region === 'MO' || region === 'SG') return 'CH';
    } catch (e) {}
    return 'EN';
  }

  function ensureAutoLocaleOnce() {
    try {
      var cur = localStorage.getItem(LOCALE_KEY);
      if (cur && String(cur).trim() !== '') return;
      var guessed = normalizeLocale(autoDetectLocaleFromNavigator()) || 'EN';
      localStorage.setItem(LOCALE_KEY, guessed);
    } catch (e) {}
  }

  function ensureNavigatorLocaleIfNotUserSet() {
    try {
      var userSet = localStorage.getItem(USER_SET_KEY);
      if (userSet && String(userSet).trim() !== '') return;
      // 호환: 과거 버전에서 사용자가 언어를 바꿨다면 locale 키만 남아있을 수 있음.
      // 이 경우 "사용자 설정"으로 간주하고 유지한다.
      var existing = localStorage.getItem(LOCALE_KEY);
      if (existing && String(existing).trim() !== '') {
        try { localStorage.setItem(USER_SET_KEY, '1'); } catch (eMark) {}
        return;
      }
      var guessed = normalizeLocale(autoDetectLocaleFromNavigator()) || 'EN';
      localStorage.setItem(LOCALE_KEY, guessed);
    } catch (e) {}
  }


  function getLocale() {
    if (g.PG_PAY_LIST_I18N && typeof g.PG_PAY_LIST_I18N.getLocale === 'function') {
      return g.PG_PAY_LIST_I18N.getLocale();
    }
    try {
      var v = localStorage.getItem(LOCALE_KEY);
      if (v) return normalizeLocale(v);
    } catch (e0) {}
    return 'EN';
  }

  /**
   * Native input type=date empty/format chrome follows the element lang attribute, not STRING_MAP.
   * en-CA yields ISO-style yyyy-mm-dd numerics across KO/EN/JP/CH/TH UI without OS-locale bleed (e.g. Korean placeholder on a Japanese screen).
   */
  function dateInputBcp47() {
    return 'en-CA';
  }

  /** type=time 네이티브 픽커의 오전/오후·표기를 UI 로케에 맞춤(브라우저/OS에 따라 다름). */
  function timeInputBcp47() {
    var loc = getLocale();
    if (loc === 'JP') return 'ja-JP';
    if (loc === 'KO') return 'ko-KR';
    if (loc === 'CH') return 'zh-CN';
    if (loc === 'TH') return 'th-TH';
    return 'en-US';
  }

  function syncDateInputLangUnder(root) {
    if (!root || typeof root.querySelectorAll !== 'function') return;
    var bcp = dateInputBcp47();
    var bcpTime = timeInputBcp47();
    try {
      root.querySelectorAll('input[type="date"]').forEach(function (el) {
        el.setAttribute('lang', bcp);
      });
    } catch (eDt) {}
    try {
      root.querySelectorAll('input[type="time"]').forEach(function (el) {
        el.setAttribute('lang', bcpTime);
      });
    } catch (eTm) {}
  }

  function pickRow(row, loc) {
    if (!row) return '';
    if (loc === 'KO') return row.KO != null ? String(row.KO) : '';
    return row[loc] || row.EN || (row.KO != null ? String(row.KO) : '');
  }

  function t(ko) {
    if (ko == null || ko === '') return ko;
    var loc = getLocale();
    var sKo = String(ko);
    if (sKo === '이메일무효') sKo = '이메일 무효';
    else if (sKo === '이메일무효(거래)') sKo = '이메일 무효(거래)';
    var sk0 = STATIC[sKo];
    /* 폼 라벨: pgUiFormLabelSpan이 필수 * 표시용으로 끝의 *를 떼어 data-pg-ui-t에 넣는 경우가 많아, STATIC/맵은 * 포함 키만 있는 경우가 있다. */
    if (!sk0 && sKo.indexOf('*') === -1) {
      sk0 = STATIC[sKo + '*'];
    }
    if (loc === 'KO') {
      if (sk0 && sk0.KO != null && String(sk0.KO).length) return String(sk0.KO);
      return sKo;
    }
    if (loc !== 'KO') {
      var mDay = sKo.match(/^(\d{1,3})일$/);
      if (mDay) {
        var nd = mDay[1];
        if (loc === 'EN') return nd + (nd === '1' ? ' day' : ' days');
        if (loc === 'JP') return nd + '日';
        if (loc === 'CH') return nd + '天';
        if (loc === 'TH') return nd + ' วัน';
      }
      var mHour = sKo.match(/^(\d{1,3})시간$/);
      if (mHour) {
        var nh = mHour[1];
        if (loc === 'EN') return nh + (nh === '1' ? ' hour' : ' hours');
        if (loc === 'JP') return nh + '時間';
        if (loc === 'CH') return nh + '小时';
        if (loc === 'TH') return nh + ' ชม.';
      }
      var mMin = sKo.match(/^(\d{1,4})분$/);
      if (mMin) {
        var nm = mMin[1];
        var rowMin = g.PG_UI_STRING_MAP && g.PG_UI_STRING_MAP['분'];
        var suffMin = rowMin ? (rowMin[loc] || rowMin.EN) : ' min';
        if (suffMin != null && String(suffMin).length) return String(nm) + String(suffMin);
        return String(nm);
      }
      var mGeon = sKo.match(/^(\d{1,4})건$/);
      if (mGeon) {
        var ng = mGeon[1];
        var rowG = g.PG_UI_STRING_MAP && g.PG_UI_STRING_MAP['건'];
        var suffG = rowG ? (rowG[loc] || rowG.EN) : '';
        if (suffG != null && String(suffG).trim().length) return String(ng) + String(suffG);
        return String(ng);
      }
    }
    if (sk0) return pickRow(sk0, loc) || sKo;
    var m = g.PG_UI_STRING_MAP && g.PG_UI_STRING_MAP[sKo];
    if (!m && sKo.indexOf('*') === -1) {
      m = g.PG_UI_STRING_MAP && g.PG_UI_STRING_MAP[sKo + '*'];
    }
    if (m) {
      var v = m[loc] || m.EN;
      if (v != null && String(v).length) return String(v);
    }
    return sKo;
  }

  /**
   * Apply translations under root for stamped markup:
   * - data-pg-ui-t: textContent from t(key) (Korean key in attribute)
   * - data-pg-ui-title / data-pg-ui-placeholder / data-pg-ui-aria-label: attribute from t(key)
   */
  function applyDom(root) {
    if (!root || typeof root.querySelectorAll !== 'function') return;
    root.querySelectorAll('[data-pg-ui-t]').forEach(function (el) {
      var k = el.getAttribute('data-pg-ui-t');
      if (k == null || k === '') return;
      el.textContent = t(k);
    });
    root.querySelectorAll('[data-pg-ui-title]').forEach(function (el) {
      var k = el.getAttribute('data-pg-ui-title');
      if (k == null || k === '') return;
      el.setAttribute('title', t(k));
    });
    root.querySelectorAll('[data-pg-ui-placeholder]').forEach(function (el) {
      var k = el.getAttribute('data-pg-ui-placeholder');
      if (k == null || k === '') return;
      el.setAttribute('placeholder', t(k));
    });
    root.querySelectorAll('[data-pg-ui-aria-label]').forEach(function (el) {
      var k = el.getAttribute('data-pg-ui-aria-label');
      if (k == null || k === '') return;
      el.setAttribute('aria-label', t(k));
    });
    root.querySelectorAll('option[data-pg-ui-t]').forEach(function (el) {
      var kOpt = el.getAttribute('data-pg-ui-t');
      if (kOpt == null || kOpt === '') return;
      el.textContent = t(kOpt);
    });
    root.querySelectorAll('[data-pg-ui-html]').forEach(function (el) {
      var kHtml = el.getAttribute('data-pg-ui-html');
      if (kHtml == null || kHtml === '') return;
      el.innerHTML = t(kHtml);
    });
    syncDateInputLangUnder(root);
  }

  /** login.html 등 독립 페이지 — 로케일 확정 후 DOM 일괄 반영 */
  function initLoginPageI18n(root) {
    try { ensureNavigatorLocaleIfNotUserSet(); } catch (eLoc) { /* ignore */ }
    var r = root;
    if (!r && typeof document !== 'undefined') r = document.body;
    if (r) applyDom(r);
    try {
      var loc = getLocale();
      var langMap = { KO: 'ko', EN: 'en', JP: 'ja', CH: 'zh-Hans', TH: 'th' };
      if (typeof document !== 'undefined' && document.documentElement) {
        document.documentElement.setAttribute('lang', langMap[loc] || 'ko');
      }
    } catch (eLang) { /* ignore */ }
  }

  g.PG_UI_I18N = {
    t: t,
    getLocale: getLocale,
    applyDom: applyDom,
    initLoginPageI18n: initLoginPageI18n,
    dateInputBcp47: dateInputBcp47,
    syncDateInputLangUnder: syncDateInputLangUnder,
    ensureAutoLocaleOnce: ensureAutoLocaleOnce,
    ensureNavigatorLocaleIfNotUserSet: ensureNavigatorLocaleIfNotUserSet
  };

  // 첫 진입(특히 login.html)에서 사용자 선택 전 자동 로케일을 1회 설정
  try { ensureNavigatorLocaleIfNotUserSet(); } catch (eInitLocale) {}
})(typeof window !== 'undefined' ? window : globalThis);
