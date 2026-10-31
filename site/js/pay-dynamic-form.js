/**
 * ICOPAY 카드 결제창 — 다이나믹 폼 (모바일·태블릿).
 * HQ dynamicCheckoutFormYn=Y 이고 PC가 아닐 때: 단계 노출·포커스 줌·자동 스크롤·길이 충족 시 다음 필드.
 * iframe embed·PC·HQ=N → 일반(무동작).
 */
(function (g) {
  'use strict';

  var STATE = { on: false, form: null, steps: [], idx: 0, unbound: null };

  function deviceKind() {
    var ua = String(g.navigator && g.navigator.userAgent || '');
    var w = Math.min(
      (g.screen && g.screen.width) || 1024,
      (g.innerWidth || (g.document && g.document.documentElement && g.document.documentElement.clientWidth) || 1024)
    );
    if (/iPad|Tablet|Android(?!.*Mobile)/i.test(ua) || (w >= 600 && w <= 1024 && 'ontouchstart' in g)) {
      return 'tablet';
    }
    if (/Mobi|Android.*Mobile|iPhone|iPod/i.test(ua) || w < 768) {
      return 'mobile';
    }
    return 'pc';
  }

  function inIframe() {
    try {
      return g.self !== g.top;
    } catch (e) {
      return true;
    }
  }

  function hqActive(ctx) {
    var yn = ctx && (ctx.dynamicCheckoutFormYn != null ? ctx.dynamicCheckoutFormYn : ctx.hqDynamicCheckoutFormYn);
    return String(yn == null ? 'Y' : yn).trim().toUpperCase() === 'Y';
  }

  function shouldEnable(ctx) {
    if (!hqActive(ctx)) return false;
    if (inIframe()) return false;
    var q = '';
    try {
      q = String((g.location && g.location.search) || '');
    } catch (e0) {}
    if (/[?&]embed=1(?:&|$)/i.test(q)) return false;
    var d = deviceKind();
    return d === 'mobile' || d === 'tablet';
  }

  function isFieldVisible(el) {
    if (!el || el.disabled) return false;
    if (el.type === 'hidden') return false;
    if (el.getAttribute('aria-hidden') === 'true') return false;
    var st = g.getComputedStyle(el);
    if (st.display === 'none' || st.visibility === 'hidden') return false;
    var wrap = el.closest('.pay-row, .row, .mb-2, .mb-3, .col-12, .col-6, .col-4');
    if (wrap) {
      var ws = g.getComputedStyle(wrap);
      if (ws.display === 'none') return false;
    }
    return true;
  }

  function stepRootFor(el) {
    return el.closest('.pay-dyn-step')
      || el.closest('.pay-row')
      || el.closest('.row.g-2, .row.mb-2, .row.mb-3')
      || el.closest('.mb-2, .mb-3')
      || el.parentElement;
  }

  function collectSteps(form) {
    var nodes = form.querySelectorAll('input, select, textarea');
    var steps = [];
    var seen = new Set();
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      if (!isFieldVisible(el)) continue;
      if (el.readOnly && el.id !== 'amount') continue;
      var root = stepRootFor(el);
      if (!root || seen.has(root)) continue;
      seen.add(root);
      root.classList.add('pay-dyn-step');
      steps.push({ root: root, fields: [] });
    }
    steps.forEach(function (s) {
      s.fields = Array.prototype.slice.call(s.root.querySelectorAll('input, select, textarea')).filter(isFieldVisible);
    });
    return steps.filter(function (s) { return s.fields.length > 0; });
  }

  function stepFilled(step) {
    for (var i = 0; i < step.fields.length; i++) {
      var el = step.fields[i];
      if (el.disabled || !isFieldVisible(el)) continue;
      if (el.type === 'checkbox' || el.type === 'radio') {
        if (el.required && !el.checked) return false;
        continue;
      }
      var v = String(el.value || '').trim();
      if (el.required && !v) return false;
      var max = parseInt(el.getAttribute('maxlength') || '0', 10);
      if (max > 0 && v.replace(/\s/g, '').length < max && /card|cvv|month|year|payCard/i.test(el.id || '')) {
        /* 카드 고정길이 필드는 길이 충족 전 미완료 */
        if (el.id === 'payCardno' || el.id === 'payCardmonth' || el.id === 'payCardyear' || el.id === 'payCardcvv') {
          var digits = v.replace(/\D/g, '');
          if (el.id === 'payCardno' && digits.length < 15) return false;
          if (el.id === 'payCardmonth' && digits.length < 2) return false;
          if (el.id === 'payCardyear' && digits.length < 2) return false;
          if (el.id === 'payCardcvv' && digits.length < 3) return false;
        }
      }
    }
    return true;
  }

  /** 필드를 숨기지 않음 — 포커스 표시만 (성명·이메일·전화 등 누락 방지) */
  function paintSteps() {
    STATE.steps.forEach(function (s, i) {
      s.root.classList.remove('pay-dyn-hidden');
      s.root.classList.toggle('is-active', i === STATE.idx);
      s.root.removeAttribute('aria-hidden');
    });
  }

  function focusStep(i) {
    if (i < 0 || i >= STATE.steps.length) return;
    STATE.idx = i;
    paintSteps();
    var fields = STATE.steps[i].fields;
    var target = null;
    for (var j = 0; j < fields.length; j++) {
      if (!fields[j].readOnly && isFieldVisible(fields[j])) {
        target = fields[j];
        break;
      }
    }
    if (!target) return;
    try {
      target.focus({ preventScroll: true });
    } catch (e1) {
      try { target.focus(); } catch (e2) {}
    }
    try {
      target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
    } catch (e3) {
      try { target.scrollIntoView(true); } catch (e4) {}
    }
  }

  function maybeAdvanceFrom(el) {
    if (!STATE.on || !el) return;
    var stepIndex = -1;
    for (var i = 0; i < STATE.steps.length; i++) {
      if (STATE.steps[i].fields.indexOf(el) >= 0) {
        stepIndex = i;
        break;
      }
    }
    if (stepIndex < 0) return;
    var max = parseInt(el.getAttribute('maxlength') || '0', 10);
    var raw = String(el.value || '');
    var digits = raw.replace(/\D/g, '');
    var id = el.id || '';
    var ready = false;
    if (id === 'payCardno' && digits.length >= 15) ready = true;
    else if (id === 'payCardmonth' && digits.length >= 2) ready = true;
    else if (id === 'payCardyear' && digits.length >= 2) ready = true;
    else if (id === 'payCardcvv' && digits.length >= 3) ready = true;
    else if (max > 0 && raw.length >= max) ready = true;
    else if (el.tagName === 'SELECT' && raw) ready = true;

    /* 길이 충족·선택 완료 시에만 다음 필드로 포커스 이동 (필드는 항상 화면에 유지) */
    if (ready && stepIndex + 1 < STATE.steps.length) {
      focusStep(stepIndex + 1);
    }
  }

  function onFocusIn(ev) {
    if (!STATE.on) return;
    var el = ev.target;
    if (!el || !STATE.form || !STATE.form.contains(el)) return;
    if (!/^(INPUT|SELECT|TEXTAREA)$/.test(el.tagName)) return;
    STATE.steps.forEach(function (s) {
      s.root.classList.toggle('is-focused', s.fields.indexOf(el) >= 0);
    });
    g.document.body.classList.add('pay-dyn-focus');
    try {
      el.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
    } catch (e) {}
  }

  function onFocusOut() {
    setTimeout(function () {
      if (!STATE.on) return;
      var a = g.document.activeElement;
      if (!a || !STATE.form || !STATE.form.contains(a)) {
        g.document.body.classList.remove('pay-dyn-focus');
        STATE.steps.forEach(function (s) { s.root.classList.remove('is-focused'); });
      }
    }, 0);
  }

  function onInput(ev) {
    maybeAdvanceFrom(ev.target);
  }

  function tearDown() {
    if (STATE.unbound) {
      try { STATE.unbound(); } catch (e) {}
      STATE.unbound = null;
    }
    g.document.body.classList.remove('pay-dyn-on', 'pay-dyn-focus');
    if (STATE.steps) {
      STATE.steps.forEach(function (s) {
        s.root.classList.remove('pay-dyn-step', 'pay-dyn-hidden', 'is-active', 'is-focused');
        s.root.removeAttribute('aria-hidden');
      });
    }
    STATE.on = false;
    STATE.form = null;
    STATE.steps = [];
    STATE.idx = 0;
  }

  function resolveForm(optForm) {
    if (optForm) return optForm;
    return g.document.getElementById('payForm')
      || g.document.getElementById('jpayForm')
      || g.document.getElementById('omsForm')
      || g.document.getElementById('qbForm')
      || g.document.querySelector('.pay-card form')
      || g.document.querySelector('form');
  }

  function apply(opts) {
    opts = opts || {};
    var ctx = opts.ctx || opts.checkoutCtx || {};
    tearDown();
    if (!shouldEnable(ctx)) {
      return { enabled: false, device: deviceKind() };
    }
    var form = resolveForm(opts.form);
    if (!form) return { enabled: false, reason: 'no-form' };
    var steps = collectSteps(form);
    if (steps.length < 2) {
      /* 필드가 적으면 줌·스크롤만 */
    }
    STATE.on = true;
    STATE.form = form;
    STATE.steps = steps;
    STATE.idx = 0;
    g.document.body.classList.add('pay-dyn-on');
    paintSteps();
    form.addEventListener('focusin', onFocusIn, true);
    form.addEventListener('focusout', onFocusOut, true);
    form.addEventListener('input', onInput, true);
    form.addEventListener('change', onInput, true);
    STATE.unbound = function () {
      form.removeEventListener('focusin', onFocusIn, true);
      form.removeEventListener('focusout', onFocusOut, true);
      form.removeEventListener('input', onInput, true);
      form.removeEventListener('change', onInput, true);
    };
    /* 진입 시 자동 포커스/숨김 없음 — 사용자가 탭한 필드만 강조 */
    return { enabled: true, device: deviceKind(), steps: steps.length };
  }

  g.PG_PAY_DYNAMIC = {
    apply: apply,
    tearDown: tearDown,
    deviceKind: deviceKind,
    shouldEnable: shouldEnable
  };
})(window);
