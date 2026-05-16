/**
 * 메인(/main) 대시보드 — /api/dashboard/home
 */
(function () {
  'use strict';

  var _loading = false;
  var _lastKey = '';
  /** 마지막으로 성공 로드된 대시보드 페이로드 — 언어 전환 시 API 재조회 없이 재렌더 */
  var _lastPayload = null;
  /** 이전에 받은 본문이 insights(및 HQ 시 hqHub)까지 포함된 완전한 페이로드였는지 — 불완전하면 캐시하지 않고 다시 조회한다 */
  var _lastPayloadComplete = false;
  var _incompleteRefetchAttempts = 0;

  function esc(s) {
    if (s == null) return '';
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function uiT(s) {
    try {
      if (window.PG_UI_I18N && typeof window.PG_UI_I18N.t === 'function') return window.PG_UI_I18N.t(String(s));
    } catch (e) {}
    return String(s);
  }

  /**
   * 서버가 내려주는 문장/요약 안의 한글 라벨을 로케일별로 치환한다.
   * (정산일/가맹/지급/상태 등) 숫자·코드는 원문 유지.
   */
  function translateInlineKo(s) {
    if (s == null) return '';
    var raw = String(s);
    if (!/[가-힣]/.test(raw)) return raw;

    var m;
    // 정산일 2026-05-09 · 가맹 ****0024 · 지급 3390
    m = raw.match(/^정산일\s*([0-9]{4}-[0-9]{2}-[0-9]{2})\s*·\s*가맹\s*([^\s·]+)\s*·\s*지급\s*([\-0-9.,]+)\s*$/);
    if (m) {
      return uiT('정산일') + ' ' + m[1] + ' · ' + uiT('가맹') + ' ' + m[2] + ' · ' + uiT('지급') + ' ' + m[3];
    }
    // 상태 MERCHANT_UNRESOLVED · —
    m = raw.match(/^상태\s*([A-Z0-9_]+)\s*·\s*(.+)\s*$/);
    if (m) {
      return uiT('상태') + ' ' + m[1] + ' · ' + m[2];
    }
    // 최근 7일 노티 미매핑/미적재 43건
    m = raw.match(/^최근\s*7일\s*노티\s*미매핑\/미적재\s*([0-9,]+)\s*건\s*$/);
    if (m) {
      return uiT('최근 7일 노티 미매핑/미적재 {0}건').replace('{0}', m[1]);
    }
    // 리스크 점수가 지난주 대비 +17 상승
    m = raw.match(/^리스크\s*점수가\s*지난주\s*대비\s*([+\-]?[0-9,]+)\s*상승\s*$/);
    if (m) {
      return uiT('리스크 점수가 지난주 대비 {0} 상승').replace('{0}', m[1]);
    }
    // 환불·무효 추이를 결제내역에서 필터로 확인
    if (raw === '환불·무효 추이를 결제내역에서 필터로 확인') {
      return uiT(raw);
    }

    // 긴 규칙 기반 내러티브(서버 문장) — 구조가 고정된 경우만 번역
    m = raw.match(/^최근\s*7일\s*리스크\s*점수는\s*([0-9,]+)점이며,\s*직전\s*7일\s*대비\s*([+\-]?[0-9,]+)입니다\.\s*구성은\s*실패\s*([0-9,]+)·무효계열\s*([0-9,]+)·환불\s*([0-9,]+)·취소\s*([0-9,]+)건입니다\.\s*노티\s*미매핑\/미적재가\s*7일간\s*([0-9,]+)건입니다\.\s*$/);
    if (m) {
      var tpl = uiT('최근 7일 리스크 내러티브 템플릿');
      return tpl
        .replace('{score}', m[1])
        .replace('{delta}', m[2])
        .replace('{fail}', m[3])
        .replace('{void}', m[4])
        .replace('{refund}', m[5])
        .replace('{cancel}', m[6])
        .replace('{notify}', m[7]);
    }

    // 미수금 라인 상세 · 잔액 · (메모)
    m = raw.match(/^([\s\S]+?)\s*·\s*잔액\s*([\s\S]+?)\s*·\s*([\s\S]+)$/);
    if (m) {
      return m[1] + ' · ' + uiT('잔액') + ' ' + m[2] + ' · ' + m[3];
    }

    // 우선 처리 TOP5 제목 패턴들
    m = raw.match(/^미수금\s*잔액\s*([0-9,]+)건\s*·\s*합계\s*약\s*(.+)\s*원$/);
    if (m) {
      return uiT('미수금 잔액 {0}건 · 합계 약 {1} 원').replace('{0}', m[1]).replace('{1}', String(m[2]).trim());
    }
    m = raw.match(/^최근\s*30일\s*정산\s*보류\/지급보류\s*실행\s*([0-9,]+)건$/);
    if (m) {
      return uiT('최근 30일 정산 보류/지급보류 실행 {0}건').replace('{0}', m[1]);
    }

    // 가맹 지급 참고 다음 정산 힌트 (정산주기 값 가변)
    m = raw.match(/^다음\s*정산\s*실행\s*일시는\s*정산주기\((.+)\)\s*및\s*정산실행\s*배치\s*기준입니다\.\s*$/);
    if (m) {
      return uiT('다음 정산 실행 일시는 정산주기({cycle}) 및 정산실행 배치 기준입니다.').replace('{cycle}', m[1]);
    }

    // 마지막 fallback: 부분 라벨만 치환
    return raw
      .split('정산일').join(uiT('정산일'))
      .split('가맹').join(uiT('가맹'))
      .split('지급').join(uiT('지급'))
      .split('상태').join(uiT('상태'));
  }

  /** 결제내역·수수료와 동일: ISO 4217 숫자(764=THB, 392=JPY…) → 알파 */
  function normalizeDashboardCurrency(cur) {
    if (cur == null || cur === '') return 'KRW';
    var s = String(cur).trim();
    if (!s) return 'KRW';
    if (/^[A-Za-z]+\s+\(\d+\)$/.test(s)) {
      return s.replace(/\s+\(\d+\)$/, '').trim().toUpperCase();
    }
    var upper = s.toUpperCase();
    if (/^\d+$/.test(upper)) {
      var key = upper;
      if (key.length < 3) key = ('000' + key).slice(-3);
      else if (key.length > 3) {
        key = key.replace(/^0+/, '');
        if (key.length > 3) key = key.slice(-3);
        else if (key.length < 3) key = ('000' + key).slice(-3);
      }
      var map = {
        '036': 'AUD', '392': 'JPY', '410': 'KRW', '458': 'MYR', '702': 'SGD', '764': 'THB',
        '826': 'GBP', '840': 'USD', '978': 'EUR', '156': 'CNY', '344': 'HKD', '554': 'NZD', '756': 'CHF'
      };
      return map[key] || key;
    }
    return upper;
  }

  function fmtNum(n) {
    if (n == null || n === '') return '0';
    var x = Number(n);
    if (!isFinite(x)) return esc(n);
    return x.toLocaleString();
  }

  function fmtMoney(v) {
    if (v == null || v === '') return '0';
    var x = Number(v);
    if (!isFinite(x)) return esc(String(v));
    return x.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  }

  /** 표시명이 i18n 키(권한·역할 라벨 등)이면 번역, 그 외(실명·영문 상호)는 그대로 */
  function localizeDisplayLabel(s) {
    if (s == null || s === '') return '';
    try {
      if (window.PG_UI_I18N && typeof window.PG_UI_I18N.t === 'function') return String(window.PG_UI_I18N.t(String(s).trim()));
    } catch (e) {}
    return String(s).trim();
  }

  function fmtMoneyForCurrency(v, cur) {
    var c = String(cur || '').toUpperCase();
    var maxFrac =
      c === 'KRW' || c === 'JPY' || c === 'VND'
        ? 0
        : c === 'THB' || c === 'USD' || c === 'EUR' || c === 'GBP'
          ? 2
          : 8;
    if (v == null || v === '') return '0';
    var x = Number(v);
    if (!isFinite(x)) return esc(String(v));
    return x.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: maxFrac });
  }

  function salesBlock(title, seg) {
    if (!seg || typeof seg !== 'object') seg = {};
    return (
      '<div class="col-md-4 mb-3">' +
      '<div class="card h-100 pg-dash-kpi">' +
      '<div class="card-body">' +
      '<div class="text-muted small mb-1">' + esc(title) + '</div>' +
      '<div class="display-6 mb-2">' + fmtMoney(seg.amtApprovedSum) + ' <span class="fs-6 text-muted">KRW</span></div>' +
      '<div class="small text-muted">' + esc(uiT('승인')) + ' ' + fmtNum(seg.txnApproved) + esc(uiT('건')) + ' / ' + esc(uiT('전체')) + ' ' + fmtNum(seg.txnTotal) + esc(uiT('건')) + '</div>' +
      '</div></div></div>'
    );
  }

  function salesByCurrencyBlock(title, rows) {
    rows = Array.isArray(rows) ? rows : [];
    if (!rows.length) {
      return (
        '<div class="col-md-4 mb-3">' +
        '<div class="card h-100 pg-dash-kpi">' +
        '<div class="card-body">' +
        '<div class="text-muted small mb-1">' + esc(title) + '</div>' +
        '<p class="small text-muted mb-0">' + esc(uiT('해당 기간 거래가 없습니다.')) + '</p>' +
        '</div></div></div>'
      );
    }
    var lines = rows.map(function (r) {
      var cur = normalizeDashboardCurrency(r && r.currency ? String(r.currency) : 'KRW');
      var amt = fmtMoneyForCurrency(r && r.amtApprovedSum != null ? r.amtApprovedSum : 0, cur);
      var tx = fmtNum(r && r.txnApproved != null ? r.txnApproved : 0);
      var tot = fmtNum(r && r.txnTotal != null ? r.txnTotal : 0);
      return (
        '<div class="mb-2 pb-2 border-bottom">' +
        '<div class="d-flex justify-content-between align-items-baseline">' +
        '<span class="badge bg-secondary">' + esc(cur) + '</span>' +
        '<span class="fs-5 fw-semibold">' + amt + '</span></div>' +
        '<div class="small text-muted">' + esc(uiT('승인')) + ' ' + tx + esc(uiT('건')) + ' / ' + esc(uiT('전체')) + ' ' + tot + esc(uiT('건')) + '</div></div>'
      );
    }).join('');
    return (
      '<div class="col-md-4 mb-3">' +
      '<div class="card h-100 pg-dash-kpi">' +
      '<div class="card-body">' +
      '<div class="text-muted small mb-1">' + esc(title) + ' <span class="badge bg-light text-dark border">' + esc(uiT('통화별')) + '</span></div>' +
      lines +
      '</div></div></div>'
    );
  }

  function salesRowFromPayload(d) {
    var sbc = d && d.salesByCurrency;
    if (sbc && typeof sbc === 'object' && (Array.isArray(sbc.today) || Array.isArray(sbc.last7d) || Array.isArray(sbc.last30d))) {
      return (
        '<div class="row">' +
        salesByCurrencyBlock(uiT('오늘'), sbc.today) +
        salesByCurrencyBlock(uiT('최근 7일'), sbc.last7d) +
        salesByCurrencyBlock(uiT('최근 30일'), sbc.last30d) +
        '</div>'
      );
    }
    var sales = (d && d.sales) || {};
    return (
      '<div class="row">' +
      salesBlock(uiT('오늘'), sales.today) +
      salesBlock(uiT('최근 7일'), sales.last7d) +
      salesBlock(uiT('최근 30일'), sales.last30d) +
      '</div>'
    );
  }

  function serverBlock(sum) {
    if (!sum || typeof sum !== 'object') {
      return '<p class="text-muted small mb-0">' + esc(uiT('서버 트래픽 요약을 사용할 수 없습니다.')) + '</p>';
    }
    var lines = [];
    if (sum.latestTrafficMb != null) {
      lines.push(esc(uiT('금일 트래픽 약')) + ' <strong>' + esc(String(sum.latestTrafficMb)) + '</strong> MB');
    }
    if (sum.trafficTotalLast7DaysMb != null) {
      lines.push(esc(uiT('최근 7일 누적 약')) + ' <strong>' + esc(String(sum.trafficTotalLast7DaysMb)) + '</strong> MB');
    }
    if (sum.memoryLatestPeakPct != null) {
      lines.push(esc(uiT('메모리 피크')) + ' <strong>' + esc(String(sum.memoryLatestPeakPct)) + '</strong> %');
    }
    if (!lines.length) {
      return '<p class="text-muted small mb-0">' + esc(uiT('수집된 서버 사용량 데이터가 없습니다.')) + '</p>';
    }
    return '<ul class="small mb-0 ps-3">' + lines.map(function (h) { return '<li>' + h + '</li>'; }).join('') + '</ul>';
  }

  function settlementTable(events) {
    if (!events || !events.length) {
      return '<p class="text-muted small mb-0">' + esc(uiT('표시할 정산 실행 이력이 없습니다.')) + '</p>';
    }
    var rows = events.map(function (e) {
      var dt = e.calcDt != null ? esc(e.calcDt) : '';
      var pay = fmtMoney(e.payAmt);
      var ap = fmtMoney(e.approveAmt);
      var cnt = e.includedTxnCnt != null ? fmtNum(e.includedTxnCnt) : '—';
      var cyc = e.calcCycleSnapshot ? esc(String(e.calcCycleSnapshot)) : '—';
      return '<tr><td>' + dt + '</td><td class="text-end">' + ap + '</td><td class="text-end">' + pay + '</td><td class="text-end">' + cnt + '</td><td><code>' + cyc + '</code></td></tr>';
    }).join('');
    return (
      '<div class="table-responsive pg-dash-cal-wrap">' +
      '<table class="table table-sm table-hover mb-0">' +
      '<thead><tr><th>' + esc(uiT('정산일')) + '</th><th class="text-end">' + esc(uiT('승인합')) + '</th><th class="text-end">' + esc(uiT('지급액')) + '</th><th class="text-end">' + esc(uiT('포함건수')) + '</th><th>' + esc(uiT('주기')) + '</th></tr></thead>' +
      '<tbody>' + rows + '</tbody></table></div>'
    );
  }

  function quickLinksHtml(links) {
    if (!links || !links.length) return '';
    var btns = links.map(function (l) {
      var u = l.url || '#';
      var lab = l.label || u;
      return '<button type="button" class="btn btn-outline-primary btn-sm pg-dash-open-url" data-url="' + esc(u) + '">' + esc(uiT(String(lab))) + '</button>';
    }).join('');
    return '<div class="mb-3 pg-dash-quick">' + btns + '</div>';
  }

  var ORG_LVL_LABEL = {
    HEADQUARTERS: '총본사', REGIONAL: '본사', MASTER_DIST: '총판', BRANCH: '지사',
    AGENCY: '대리점', SALES_OFFICE: '영업점', MERCHANT: '가맹점'
  };

  /** 서버 note: 고정 접두사 + 동적 텍스트(오류 등) 처리 */
  function localizeHubNote(raw) {
    if (raw == null || raw === '') return '';
    var s = String(raw);
    var prefixKo = 'DASHBOARD 집계 중 오류: ';
    if (s.indexOf(prefixKo) === 0) {
      return '<p class="small text-warning mb-2">' + esc(uiT(prefixKo)) + '<span class="text-break">' + esc(s.slice(prefixKo.length)) + '</span></p>';
    }
    return '<p class="small text-warning mb-2">' + esc(uiT(s)) + '</p>';
  }

  function hqHubSection(d) {
    var h = d.hqHub;
    if (!h || typeof h !== 'object') return '';
    var hl = h.headline || {};
    var rawTitle = h.title != null ? String(h.title) : 'DASHBOARD';
    var title = esc(uiT(rawTitle));
    var note = h.note ? localizeHubNote(h.note) : '';

    var kpiTop = '';
    var insHub = d.insights;
    if (insHub && typeof insHub === 'object' && !insHub.loadError) {
      var exHub = insHub.explainers || {};
      kpiTop = kpiStripHtml(insHub.kpiStrip, exHub) + kpiStripYesterdayHtml(insHub.kpiStripYesterday, exHub);
    }

    var headRow =
      '<div class="row g-3 mb-3">' +
      '<div class="col-md-4"><div class="card border-primary h-100 shadow-sm">' +
      '<div class="card-body"><div class="text-muted small">' + esc(uiT('최근 7일 승인 금액 합')) + ' <span class="text-muted">(' + esc(uiT('통화 혼합·참고')) + ')</span></div>' +
      '<div class="fs-3 fw-bold text-primary">' + fmtMoney(hl.approveAmt7d) + '</div></div></div></div>' +
      '<div class="col-md-4"><div class="card h-100 shadow-sm">' +
      '<div class="card-body"><div class="text-muted small">' + esc(uiT('최근 7일 승인 건수')) + '</div>' +
      '<div class="fs-3 fw-bold">' + fmtNum(hl.approveCnt7d) + '</div></div></div></div>' +
      '<div class="col-md-4"><div class="card h-100 shadow-sm">' +
      '<div class="card-body"><div class="text-muted small">' + esc(uiT('최근 7일 전체 거래 건수')) + '</div>' +
      '<div class="fs-3 fw-bold">' + fmtNum(hl.txnTotal7d) + '</div></div></div></div>' +
      '</div>';

    var byLv = h.orgUnitsByLevel || {};
    var orgChips = ['MERCHANT', 'REGIONAL', 'MASTER_DIST', 'BRANCH', 'AGENCY', 'SALES_OFFICE'].map(function (k) {
      var n = byLv[k] != null ? Number(byLv[k]) : 0;
      var lab = uiT(ORG_LVL_LABEL[k] || k);
      return '<span class="badge rounded-pill bg-secondary me-1 mb-1">' + esc(lab) + ' ' + fmtNum(n) + '</span>';
    }).join('');
    var orgRow =
      '<div class="card mb-3"><div class="card-header py-2"><strong>' + esc(uiT('조직 스냅샷')) + '</strong> <span class="text-muted small">(' + esc(uiT('소속 트리')) + ')</span></div>' +
      '<div class="card-body"><p class="small text-muted mb-2">' + esc(uiT('가맹점 조직 수')) + ' <strong>' + fmtNum(h.merchantOrgCount) + '</strong></p>' + orgChips + '</div></div>';

    var trend = h.revenueTrend7d || [];
    var trendBody;
    if (!trend.length) {
      trendBody = '<p class="text-muted small mb-0">' + esc(uiT('추이 데이터가 없습니다.')) + '</p>';
    } else {
      var maxAmt = 0;
      trend.forEach(function (t) {
        var v = t && t.amtApprovedSum != null ? Number(t.amtApprovedSum) : 0;
        if (v > maxAmt) maxAmt = v;
      });
      if (maxAmt <= 0) maxAmt = 1;
      var bars = trend.map(function (t) {
        var amt = t && t.amtApprovedSum != null ? Number(t.amtApprovedSum) : 0;
        var pct = Math.round((amt / maxAmt) * 100);
        var dt = t && t.date ? String(t.date).slice(5) : '';
        return '<div class="pg-dash-trend-col text-center"><div class="pg-dash-trend-bar-wrap"><div class="pg-dash-trend-bar" style="height:' + pct + '%"></div></div>' +
          '<div class="small text-muted mt-1">' + esc(dt) + '</div><div class="small">' + fmtMoney(amt) + '</div></div>';
      }).join('');
      trendBody = '<div class="d-flex align-items-end justify-content-between gap-1 pg-dash-trend">' + bars + '</div>';
    }
    var trendCard =
      '<div class="card mb-3"><div class="card-header py-2"><strong>' + esc(uiT('7일 승인 금액 추이')) + '</strong> <span class="text-muted small">(' + esc(uiT('일별')) + ')</span></div>' +
      '<div class="card-body">' + trendBody + '</div></div>';

    var mix = h.statusMix30d || {};
    var mixRow =
      '<div class="card mb-3"><div class="card-header py-2"><strong>' + esc(uiT('최근 30일 거래 상태 믹스')) + '</strong></div><div class="card-body">' +
      '<span class="badge bg-danger me-1">' + esc(uiT('실패')) + ' ' + fmtNum(mix.fail) + '</span>' +
      '<span class="badge bg-secondary me-1">' + esc(uiT('무효계')) + ' ' + fmtNum(mix.voidFamily) + '</span>' +
      '<span class="badge bg-warning text-dark me-1">' + esc(uiT('환불')) + ' ' + fmtNum(mix.refund) + '</span>' +
      '<span class="badge bg-info text-dark me-1">' + esc(uiT('취소')) + ' ' + fmtNum(mix.cancel) + '</span>' +
      '</div></div>';

    var tiles = (h.tiles || []).map(function (t) {
      var ic = t.icon ? esc(String(t.icon)) : 'bi-grid';
      return '<div class="col-6 col-md-4 col-lg-3 mb-3">' +
        '<button type="button" class="btn btn-light border w-100 h-100 text-start pg-dash-tile pg-dash-open-url shadow-sm" data-url="' + esc(t.url || '') + '">' +
        '<i class="bi ' + ic + ' d-block mb-2 fs-4 text-primary"></i>' +
        '<div class="fw-semibold">' + esc(uiT(String(t.title || ''))) + '</div>' +
        '<div class="small text-muted">' + esc(uiT(String(t.subtitle || ''))) + '</div></button></div>';
    }).join('');
    var tileCard =
      '<div class="card mb-3"><div class="card-header py-2"><strong>' + esc(uiT('업무 바로가기')) + '</strong></div><div class="card-body"><div class="row">' + tiles + '</div></div></div>';

    var rs = h.recentSettlements || [];
    var rsRows = rs.map(function (r) {
      return '<tr><td class="text-nowrap small">' + esc(r.at || '') + '</td><td>' + esc(r.calcDt || '') + '</td>' +
        '<td>' + esc(r.merchantIdMasked || '') + '</td><td class="text-end">' + fmtMoney(r.payAmt) + '</td>' +
        '<td><code class="small">' + esc(String(r.settlementPublishSts || '')) + '</code></td></tr>';
    }).join('');
    var rsTable = rs.length
      ? '<div class="table-responsive"><table class="table table-sm table-hover mb-0"><thead><tr><th>' + esc(uiT('생성')) + '</th><th>' + esc(uiT('정산일')) + '</th><th>' + esc(uiT('가맹')) + '</th><th class="text-end">' + esc(uiT('지급액')) + '</th><th>' + esc(uiT('배포')) + '</th></tr></thead><tbody>' + rsRows + '</tbody></table></div>'
      : '<p class="text-muted small mb-0">' + esc(uiT('최근 정산 실행이 없습니다.')) + '</p>';
    var rsCard =
      '<div class="card mb-3"><div class="card-header py-2 d-flex justify-content-between align-items-center">' +
      '<strong>' + esc(uiT('최근 정산 실행')) + '</strong>' +
      '<button type="button" class="btn btn-sm btn-outline-primary pg-dash-open-url" data-url="/calc/exCalcList">' + esc(uiT('정산실행')) + '</button></div>' +
      '<div class="card-body pt-0">' + rsTable + '</div></div>';

    return (
      '<section class="pg-dash-hq-hub mb-3">' +
      '<div class="d-flex align-items-center justify-content-between flex-wrap mb-2">' +
      '<h4 class="mb-0">' + title + '</h4>' +
      (d.orgLevel ? '<span class="badge bg-dark">' + esc(uiT(String(d.orgLevel))) + '</span>' : '') +
      '</div>' +
      note + kpiTop + headRow + orgRow + trendCard + mixRow + tileCard + rsCard +
      '</section>'
    );
  }

  function bindQuick(root) {
    root.querySelectorAll('.pg-dash-open-url').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var u = btn.getAttribute('data-url') || '';
        if (!u) return;
        var lab = btn.textContent.trim();
        if (typeof window.fnTopMenuMove === 'function') {
          try {
            window.fnTopMenuMove(u, '', lab);
            return;
          } catch (e1) { /* fall through */ }
        }
        try { window.location.hash = u; } catch (e2) {}
      });
    });
  }

  function evidenceHtml(ev) {
    if (!ev || typeof ev !== 'object') return '';
    var parts = [];
    if (ev.insightEngine) parts.push(esc(uiT('엔진')) + ' ' + esc(String(ev.insightEngine)));
    if (ev.txnTimeField) parts.push(esc(uiT('거래시각')) + ' ' + esc(String(ev.txnTimeField)));
    if (ev.scope) parts.push(esc(uiT('범위')) + ' ' + esc(String(ev.scope)));
    if (ev.rolling7dThis) parts.push(esc(uiT('리스크7일')) + ' ' + esc(String(ev.rolling7dThis)));
    if (ev.rolling7dPrev) parts.push(esc(uiT('비교7일')) + ' ' + esc(String(ev.rolling7dPrev)));
    if (!parts.length) return '';
    return '<p class="text-muted small mb-2 pg-dash-evidence">' + parts.join(' · ') + '</p>';
  }

  function explainerBtn(key, explainers) {
    var rawEx = explainers && explainers[key] ? String(explainers[key]) : '';
    var t = rawEx ? uiT(rawEx) : '';
    if (!t) return '';
    var id = 'pg-dash-exp-' + key + '-' + Math.random().toString(36).slice(2, 8);
    return (
      '<button type="button" class="btn btn-link btn-sm text-decoration-none p-0 ms-1 align-baseline pg-dash-i" data-bs-toggle="collapse" data-bs-target="#' + id + '" aria-expanded="false" title="' + esc(uiT('의미')) + '">ⓘ</button>' +
      '<div class="collapse small text-muted mt-1" id="' + id + '">' + esc(t) + '</div>'
    );
  }

  /** 성공(승인): 통화별 건수·금액 — rows: currency, txnApproved, amtApprovedSum (통화코드 KRW·USD·JPY·THB 등) */
  function kpiApprovedByCurrencyBlock(rows) {
    rows = Array.isArray(rows) ? rows : [];
    function curNorm(r) {
      return normalizeDashboardCurrency(r && r.currency != null ? String(r.currency) : 'KRW');
    }
    rows = rows.slice().sort(function (a, b) {
      return curNorm(a).localeCompare(curNorm(b));
    });
    var head =
      '<div class="text-muted small mb-2">' + esc(uiT('통화별 성공 (건수·금액)')) + '</div>';
    if (!rows.length) {
      return (
        '<div class="col-12 mb-2">' +
        '<div class="border rounded p-3 bg-light">' +
        head +
        '<p class="small text-muted mb-0">' +
        esc(uiT('해당 기간 성공(승인) 거래가 없습니다.')) +
        '</p>' +
        '</div></div>'
      );
    }
    var body = rows
      .map(function (r) {
        var cur = curNorm(r);
        var amt = fmtMoneyForCurrency(r && r.amtApprovedSum != null ? r.amtApprovedSum : 0, cur);
        var cnt = fmtNum(r && r.txnApproved != null ? r.txnApproved : 0);
        return (
          '<tr>' +
          '<td class="text-center align-middle"><span class="badge bg-secondary">' +
          esc(cur) +
          '</span></td>' +
          '<td class="text-center align-middle">' +
          cnt +
          esc(uiT('건')) +
          '</td>' +
          '<td class="text-center align-middle fw-semibold text-success">' +
          amt +
          '</td>' +
          '</tr>'
        );
      })
      .join('');
    var tbl =
      '<div class="table-responsive table-no-col-resize-wrap">' +
      '<table class="table table-sm table-bordered mb-0">' +
      '<thead><tr>' +
      '<th class="text-center align-middle">' +
      esc(uiT('통화')) +
      '</th>' +
      '<th class="text-center align-middle">' +
      esc(uiT('성공 건수')) +
      '</th>' +
      '<th class="text-center align-middle">' +
      esc(uiT('성공 금액')) +
      '</th>' +
      '</tr></thead><tbody>' +
      body +
      '</tbody></table></div>';
    return '<div class="col-12 mb-2"><div class="border rounded p-3 bg-light">' + head + tbl + '</div></div>';
  }

  function riskScorecardHtml(rs, explainers) {
    if (!rs || typeof rs !== 'object') return '';
    var sc = rs.score != null ? Number(rs.score) : 0;
    var prev = rs.scorePrevWeek != null ? Number(rs.scorePrevWeek) : 0;
    var d = rs.deltaVsPrevWeek != null ? Number(rs.deltaVsPrevWeek) : sc - prev;
    var dCls = d > 0 ? 'text-danger' : d < 0 ? 'text-success' : 'text-muted';
    var dLab = (d > 0 ? '+' : '') + fmtNum(d);
    return (
      '<div class="col-lg-4 col-md-6 mb-3">' +
      '<div class="card h-100 border shadow-sm pg-dash-risk">' +
      '<div class="card-body">' +
      '<div class="d-flex justify-content-between align-items-center mb-1">' +
      '<span class="text-muted small">' + esc(uiT('이번 주 리스크 점수')) + '</span>' + explainerBtn('riskScore', explainers) +
      '</div>' +
      '<div class="display-5 mb-0">' + esc(String(sc)) + ' <span class="fs-6 text-muted">/ 100</span></div>' +
      '<div class="small ' + dCls + '">' + esc(uiT('지난주 대비')) + ' ' + dLab + ' (' + esc(uiT('직전')) + ' ' + esc(String(prev)) + ')</div>' +
      '<div class="small text-muted mt-2">' + esc(uiT('실패·무효·환불·취소 가중 합성(규칙)')) + '</div>' +
      '</div></div></div>'
    );
  }

  function kpiStripHtml(kpi, explainers) {
    if (!kpi || typeof kpi !== 'object') return '';
    function chip(label, val, tone) {
      tone = tone || 'secondary';
      return '<div class="col-6 col-md-3 mb-2"><div class="border rounded p-2 h-100 bg-light pg-dash-kpi-chip">' +
        '<div class="text-muted small">' + esc(label) + '</div>' +
        '<div class="fw-semibold text-' + esc(tone) + '">' + esc(String(val)) + '</div></div></div>';
    }
    return (
      '<div class="card mb-3">' +
      '<div class="card-header py-2 d-flex align-items-center flex-wrap">' +
      '<strong>' + esc(uiT('오늘의 운영 KPI')) + '</strong> <span class="text-muted small ms-1">(' + esc(uiT('거래일시 기준 오늘 0시~현재')) + ')</span>' +
      explainerBtn('kpiStrip', explainers) +
      '</div>' +
      '<div class="card-body">' +
      '<div class="row">' +
      chip(uiT('성공 건수'), fmtNum(kpi.todayTxnApproved), 'success') +
      chip(uiT('전체 거래 건수'), fmtNum(kpi.todayTxnTotal), 'secondary') +
      chip(uiT('실패(99/F0)'), fmtNum(kpi.todayFailures), 'danger') +
      chip(uiT('환불(30/31)'), fmtNum(kpi.todayRefunds), 'warning') +
      chip(uiT('무효계열'), fmtNum(kpi.todayVoids), 'secondary') +
      chip(uiT('취소(20)'), fmtNum(kpi.todayCancels), 'secondary') +
      chip(uiT('미수 건수'), fmtNum(kpi.receivableOpenCount), kpi.receivableOpenCount > 0 ? 'danger' : 'secondary') +
      chip(uiT('미수 잔액'), fmtMoney(kpi.receivableRemainingSum) + ' ' + esc(uiT('원')), kpi.receivableOpenCount > 0 ? 'danger' : 'secondary') +
      chip(uiT('노티 미처리(7d)'), fmtNum(kpi.notifyNotParsedLast7d), kpi.notifyNotParsedLast7d > 0 ? 'warning' : 'secondary') +
      chip(uiT('정산보류(30d)'), fmtNum(kpi.settlementHoldOrPayoutHoldRows30d), kpi.settlementHoldOrPayoutHoldRows30d > 0 ? 'warning' : 'secondary') +
      '</div>' +
      '<div class="row">' +
      kpiApprovedByCurrencyBlock(kpi.todayApprovedByCurrency) +
      '</div></div></div>'
    );
  }

  function kpiStripYesterdayHtml(yp, explainers) {
    yp = yp && typeof yp === 'object' ? yp : {};
    function yn(k) {
      var v = yp[k];
      return v != null ? Number(v) : 0;
    }
    function chip(label, val, tone) {
      tone = tone || 'secondary';
      return '<div class="col-6 col-md-3 mb-2"><div class="border rounded p-2 h-100 bg-light pg-dash-kpi-chip">' +
        '<div class="text-muted small">' + esc(label) + '</div>' +
        '<div class="fw-semibold text-' + esc(tone) + '">' + esc(String(val)) + '</div></div></div>';
    }
    return (
      '<div class="card mb-3">' +
      '<div class="card-header py-2 d-flex align-items-center flex-wrap">' +
      '<strong>' + esc(uiT('어제의 운영 KPI')) + '</strong> <span class="text-muted small ms-1">(' + esc(uiT('전일 0시~24시 거래일시')) + ')</span>' +
      explainerBtn('kpiStripYesterday', explainers) +
      '</div>' +
      '<div class="card-body">' +
      '<div class="row">' +
      chip(uiT('성공 건수'), fmtNum(yn('yesterdayTxnApproved')), 'success') +
      chip(uiT('전체 거래 건수'), fmtNum(yn('yesterdayTxnTotal')), 'secondary') +
      chip(uiT('실패(99/F0)'), fmtNum(yn('yesterdayFailures')), 'danger') +
      chip(uiT('환불(30/31)'), fmtNum(yn('yesterdayRefunds')), 'warning') +
      chip(uiT('무효계열'), fmtNum(yn('yesterdayVoids')), 'secondary') +
      chip(uiT('취소(20)'), fmtNum(yn('yesterdayCancels')), 'secondary') +
      '</div>' +
      '<div class="row">' +
      kpiApprovedByCurrencyBlock(yp.yesterdayApprovedByCurrency) +
      '</div></div></div>'
    );
  }

  function timelineHtml(items, explainers) {
    var head =
      '<div class="card-header py-2 d-flex align-items-center flex-wrap">' +
      '<strong>' + esc(uiT('타임라인')) + '</strong> <span class="text-muted small ms-1">(' + esc(uiT('최근 이벤트')) + ')</span>' + explainerBtn('timeline', explainers) +
      '</div>';
    if (!items || !items.length) {
      return '<div class="card mb-3">' + head + '<div class="card-body"><p class="text-muted small mb-0">' + esc(uiT('표시할 이벤트가 없습니다.')) + '</p></div></div>';
    }
    var rows = items.map(function (it) {
      var u = it.refUrl || '';
      var title = it.title || '';
      return '<div class="d-flex pg-dash-tl-row">' +
        '<div class="pg-dash-tl-dot"></div><div class="flex-grow-1 min-w-0">' +
        '<div class="small text-muted">' + esc(it.at || '') + '</div>' +
        '<div><button type="button" class="btn btn-link btn-sm text-start p-0 pg-dash-open-url" data-url="' + esc(u) + '">' + esc(uiT(String(title || ''))) + '</button></div>' +
        '<div class="small">' + esc(translateInlineKo(it.detail || '')) + '</div></div></div>';
    }).join('');
    return '<div class="card mb-3 pg-dash-tl">' + head + '<div class="card-body">' + rows + '</div></div>';
  }

  function priorityQueueHtml(pq, explainers) {
    var head =
      '<div class="card-header py-2 d-flex align-items-center flex-wrap">' +
      '<strong>' + esc(uiT('오늘 처리 권장')) + '</strong> <span class="text-muted small ms-1">TOP 5</span>' + explainerBtn('priorityQueue', explainers) +
      '</div>';
    if (!pq || !pq.length) {
      return '<div class="card mb-3">' + head + '<div class="card-body"><p class="text-muted small mb-0">' + esc(uiT('우선 처리 항목이 없습니다.')) + '</p></div></div>';
    }
    var lis = pq.map(function (p) {
      return '<li class="list-group-item d-flex justify-content-between align-items-center">' +
        '<span><span class="badge bg-primary me-2">' + esc(String(p.rank)) + '</span>' + esc(translateInlineKo(p.title || '')) + '</span>' +
        '<button type="button" class="btn btn-sm btn-outline-primary pg-dash-open-url" data-url="' + esc(p.url || '') + '">' + esc(uiT('이동')) + '</button></li>';
    }).join('');
    return '<div class="card mb-3">' + head + '<ul class="list-group list-group-flush">' + lis + '</ul></div>';
  }

  function anomaliesHtml(an, explainers) {
    if (!an || !an.length) return '';
    var head =
      '<div class="card-header py-2 d-flex align-items-center flex-wrap">' +
      '<strong>' + esc(uiT('이상 탐지(가벼운 통계)')) + '</strong>' + explainerBtn('anomalies', explainers) +
      '</div>';
    var body = an.map(function (a) {
      return '<div class="border-bottom py-2"><div class="fw-semibold">' + esc(uiT(String(a.message || ''))) + '</div>' +
        '<div class="small text-muted">' + esc(String(a.merchantMasked || '')) + ' · ' + esc(uiT('환불계')) + ' ' + fmtNum(a.refundCount7d) + esc(uiT('건')) + '</div>' +
        '<button type="button" class="btn btn-link btn-sm p-0 pg-dash-open-url" data-url="' + esc(a.refUrl || '/calc/payList') + '">' + esc(uiT('결제내역')) + '</button></div>';
    }).join('');
    return '<div class="card mb-3">' + head + '<div class="card-body">' + body + '</div></div>';
  }

  function payoutOutlookHtml(po, explainers) {
    if (!po || typeof po !== 'object') return '';
    var head =
      '<div class="card-header py-2 d-flex align-items-center flex-wrap">' +
      '<strong>' + esc(uiT('지급 참고 구간')) + '</strong>' + explainerBtn('payoutOutlook', explainers) +
      '</div>';
    var parts = [];
    if (po.calcCycle) parts.push('<div class="small text-muted">' + esc(uiT('정산주기')) + ' <code>' + esc(String(po.calcCycle)) + '</code></div>');
    if (po.recentPayAmtMedian != null && po.recentPayAmtMin != null && po.recentPayAmtMax != null) {
      parts.push('<div class="mt-2">' + esc(uiT('최근 3회 지급액 중앙')) + ' <strong>' + fmtMoney(po.recentPayAmtMedian) + '</strong> ' + esc(uiT('원')) + ' ' +
        '(' + esc(uiT('최소')) + ' ' + fmtMoney(po.recentPayAmtMin) + ' ~ ' + esc(uiT('최대')) + ' ' + fmtMoney(po.recentPayAmtMax) + ')</div>');
    }
    if (po.nextSettlementHint) parts.push('<div class="small mt-2">' + esc(uiT(String(po.nextSettlementHint))) + '</div>');
    if (po.disclaimer) parts.push('<div class="small text-muted mt-2">' + esc(uiT(String(po.disclaimer))) + '</div>');
    return '<div class="card mb-3">' + head + '<div class="card-body">' + parts.join('') + '</div></div>';
  }

  function hasHqHubPayload(d) {
    var h = d.hqHub;
    if (h == null || typeof h !== 'object') return false;
    return !!(h.variant || h.title);
  }

  function insightsSection(d) {
    var ins = d.insights;
    if (ins && typeof ins === 'object' && ins.loadError) {
      return (
        '<div class="alert alert-warning mb-3" role="alert">' +
        '<strong>' + esc(uiT('인사이트 집계 오류')) + '</strong> <span class="text-break">' + esc(String(ins.loadError)) + '</span>' +
        '</div>' +
        '<p class="small text-muted mb-0">' + esc(uiT('상단 매출 카드는 표시될 수 있으나, 리스크·KPI·타임라인 등은 집계 단계에서 실패했습니다. 서버 로그와 DB 스키마·데이터를 확인하세요.')) + '</p>'
      );
    }
    if (!ins || typeof ins !== 'object') {
      if (!hasHqHubPayload(d)) {
        return (
          '<div class="alert alert-warning mb-3" role="alert">' +
          '<strong>' + esc(uiT('메인 확장 데이터가 아직 불완전합니다.')) + '</strong> ' +
          esc(uiT('응답에')) + ' <code>insights</code> ' + esc(uiT('또는(총본사·관리자인 경우)')) + ' <code>hqHub</code>' + esc(uiT('가 없습니다.')) + ' ' +
          esc(uiT('클라이언트는')) + ' <code>/api/dashboard/ext</code>' + esc(uiT('로 보강 조회를 시도합니다. 계속되면 네트워크 탭에서 두 요청의 JSON과 최신')) + ' <code>pg-app</code> ' + esc(uiT('배포를 확인하세요.')) +
          '</div>'
        );
      }
      return (
        '<details class="mb-3 pg-dash-api-hint border rounded px-3 py-2 bg-light">' +
        '<summary class="small text-muted" style="cursor:pointer;"><code>insights</code>' + esc(uiT('만 없습니다. (DASHBOARD는 표시 중)')) + '</summary>' +
        '<div class="small text-muted mt-2 pb-1">' +
        esc(uiT('API·정적 리소스 버전을 맞춘 뒤')) + ' <kbd>Ctrl+F5</kbd> ' + esc(uiT('로 새로고침하세요.')) +
        '</div></details>'
      );
    }
    var ex = ins.explainers || {};
    var kpiBlocks = hasHqHubPayload(d) ? '' : (kpiStripHtml(ins.kpiStrip, ex) + kpiStripYesterdayHtml(ins.kpiStripYesterday, ex));
    var narr = ins.ruleNarrative ? '<div class="alert alert-light border mb-3 pg-dash-narr"><div class="small text-uppercase text-muted mb-1">' + esc(uiT('규칙 기반 인사이트 (비 LLM)')) + '</div><p class="mb-0">' + esc(translateInlineKo(ins.ruleNarrative)) + '</p></div>' : '';
    var llm = ins.llmNarrativeEnabled ? '' : '<p class="small text-muted mb-2">' + esc(uiT('숫자·근거는 서버 집계이며, LLM 요약은 비활성(1단계)입니다.')) + '</p>';
    var row = '<div class="row">' + riskScorecardHtml(ins.riskScorecard, ex) + '</div>';
    return (
      '<section class="pg-dash-insights mb-2">' +
      kpiBlocks +
      narr + llm + evidenceHtml(ins.evidenceBase) +
      row +
      timelineHtml(ins.timeline, ex) +
      priorityQueueHtml(ins.priorityQueue, ex) +
      anomaliesHtml(ins.anomalies, ex) +
      payoutOutlookHtml(ins.payoutOutlook, ex) +
      '</section>'
    );
  }

  function render(d, mount) {
    var orgLv = d.orgLevel ? String(d.orgLevel) : '';
    var comp = d.compNm ? localizeDisplayLabel(d.compNm) : '';
    var uid = d.userNm ? localizeDisplayLabel(d.userNm) : '';
    var head = '<div class="d-flex flex-wrap justify-content-between align-items-start mb-3">' +
      '<div><h5 class="mb-1">' + esc(uid) + (comp ? ' <span class="text-muted">· ' + esc(comp) + '</span>' : '') + '</h5>' +
      (orgLv ? '<span class="badge bg-secondary">' + esc(uiT(orgLv)) + '</span>' : '') +
      '</div>' +
      (d.asOfDate ? '<div class="text-muted small">' + esc(uiT('기준일')) + ' ' + esc(d.asOfDate) + '</div>' : '') +
      '</div>';

    var hint = d.insightHint ? '<div class="pg-dash-insight mb-3">' + esc(uiT(String(d.insightHint))) + '</div>' : '';

    var salesRow = salesRowFromPayload(d);

    var srv = '';
    if (d.serverUsageSummary) {
      srv =
        '<div class="card mb-3">' +
        '<div class="card-header py-2"><strong>' + esc(uiT('서버 운영 · 트래픽 요약')) + '</strong></div>' +
        '<div class="card-body">' + serverBlock(d.serverUsageSummary) + '</div></div>';
    }

    var cal = '';
    if (d.settlementCalendar && d.settlementCalendar.events) {
      cal =
        '<div class="card mb-3">' +
        '<div class="card-header py-2"><strong>' + esc(uiT('정산 달력 · 실행 이력')) + '</strong> <span class="text-muted small">(' +
        esc(String(d.settlementCalendar.from || '')) + ' ~ ' + esc(String(d.settlementCalendar.to || '')) + ')</span></div>' +
        '<div class="card-body pt-2">' + settlementTable(d.settlementCalendar.events) + '</div></div>';
    }

    var ql = quickLinksHtml(d.quickLinks);
    var hq = hqHubSection(d);
    var ins = insightsSection(d);
    var foot =
      '<p class="text-muted small mb-0 mt-3">' + esc(uiT('좌측 메뉴에서 다른 화면을 선택하면 탭이 열립니다. 결제내역 컬럼은 해당 화면의 VIEW SETTING에서 조정할 수 있습니다.')) + '</p>';

    mount.innerHTML = head + hint + hq + ins + ql + salesRow + srv + cal + foot;
    bindQuick(mount);
  }

  function sessionKey() {
    try {
      var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
      return (u && u.userId ? String(u.userId) : '') + '|' + (u && u.compId ? String(u.compId) : '') + '|dh3';
    } catch (e) {
      return '|dh3';
    }
  }

  function isDashboardPayloadComplete(d) {
    if (!d || typeof d !== 'object') return false;
    if (d.ok === false) return false;
    if (!d.sales || typeof d.sales !== 'object') return false;
    if (!d.insights || typeof d.insights !== 'object') return false;
    var role = String(d.role || '').toUpperCase();
    var ol = String(d.orgLevel || '').toUpperCase();
    var needHub = role === 'ADMIN' || ol === 'HEADQUARTERS';
    if (!needHub) return true;
    var h = d.hqHub;
    if (h == null || typeof h !== 'object') return false;
    return !!(h.variant || h.title);
  }

  function onMainShown() {
    if (typeof window.PG_shouldShowTabletLaunchBoard === 'function' && window.PG_shouldShowTabletLaunchBoard()) {
      return;
    }
    var mount = document.getElementById('pgHomeDashboardMount');
    if (!mount) return;
    if (!window.PG_API || typeof window.PG_API.dashboardHome !== 'function') {
      mount.innerHTML = '<p class="text-danger small">' + esc(uiT('대시보드 API를 불러올 수 없습니다.')) + '</p>';
      return;
    }
    var k = sessionKey();
    if (_loading) return;
    if (k && k === _lastKey && mount.getAttribute('data-pg-dash-loaded') === '1' && _lastPayloadComplete) {
      return;
    }
    _loading = true;
    var fetchOk = false;
    mount.innerHTML = '<div class="text-center text-muted py-5"><div class="spinner-border spinner-border-sm" role="status"></div> ' + esc(uiT('불러오는 중…')) + '</div>';
    window.PG_API.dashboardHome()
      .then(function (d) {
        fetchOk = true;
        _lastKey = k;
        _lastPayload = d && typeof d === 'object' ? d : null;
        _lastPayloadComplete = isDashboardPayloadComplete(d);
        if (_lastPayloadComplete) {
          mount.setAttribute('data-pg-dash-loaded', '1');
          _incompleteRefetchAttempts = 0;
        } else {
          mount.removeAttribute('data-pg-dash-loaded');
        }
        render(d, mount);
      })
      .catch(function (err) {
        mount.removeAttribute('data-pg-dash-loaded');
        var msg = err && err.message ? String(err.message) : uiT('조회 실패');
        mount.innerHTML = '<div class="alert alert-warning mb-0">' + esc(msg) + '</div>';
      })
      .finally(function () {
        _loading = false;
        if (!fetchOk) return;
        if (_lastPayloadComplete) return;
        if (_incompleteRefetchAttempts >= 5) return;
        _incompleteRefetchAttempts++;
        setTimeout(function () {
          var mp = document.getElementById('main');
          if (!mp || !mp.classList.contains('active') || !mp.classList.contains('show')) return;
          try {
            window.PG_HOME_DASHBOARD.onMainShown();
          } catch (eR) { /* ignore */ }
        }, 400);
      });
  }

  /** 로그아웃·다른 사용자 로그인 후 재조회 */
  function invalidate() {
    _lastKey = '';
    _lastPayloadComplete = false;
    _lastPayload = null;
    _incompleteRefetchAttempts = 0;
    var mount = document.getElementById('pgHomeDashboardMount');
    if (mount) mount.removeAttribute('data-pg-dash-loaded');
  }

  /** 현재 로케일로 동일 페이로드 재그리기 (언어 변경 시) */
  function refreshLocale() {
    if (!_lastPayload) return;
    var mount = document.getElementById('pgHomeDashboardMount');
    if (!mount) return;
    try {
      render(_lastPayload, mount);
    } catch (eRf) {}
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('pg-pay-list-locale-changed', function () {
      try { refreshLocale(); } catch (eLc) {}
    });
  }

  window.PG_HOME_DASHBOARD = {
    onMainShown: onMainShown,
    invalidate: invalidate,
    refreshLocale: refreshLocale
  };
})();
