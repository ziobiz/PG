/**

 * JPAY·URL 결제창 — 이메일·전화·성명 localStorage 자동기억 (카드번호 미저장).

 * window.PG_JPAY_CONTACT_REMEMBER

 */

(function (global) {

  'use strict';



  var STORAGE_PREFIX = 'icopay_jpay_contact_';



  var FIELD_PRESETS = {

    jpay: {

      email: 'payEmailAddress',

      phone: 'payTelephone',

      firstName: 'payFirstname',

      lastName: 'payLastname',

      country: 'payCountryIsoCode2',

      countrySelect: 'payContactCountryCode'

    },

    payHtml: {

      email: 'custEmail',

      phone: 'phoneNumber',

      firstName: 'firstName',

      lastName: 'lastName',

      countrySelect: 'country'

    }

  };



  function resolveFieldIds(preset) {

    if (!preset) return FIELD_PRESETS.jpay;

    if (typeof preset === 'string' && FIELD_PRESETS[preset]) return FIELD_PRESETS[preset];

    if (typeof preset === 'object') return preset;

    return FIELD_PRESETS.jpay;

  }



  function storageKey(compId) {

    var c = String(compId || '').trim();

    return c ? STORAGE_PREFIX + c : '';

  }



  function isEnabled(ctx) {

    if (!ctx) return false;

    return ctx.checkoutContactRememberEnabled === true

      || String(ctx.checkoutContactRememberEnabled || '').toLowerCase() === 'true';

  }



  function readStored(compId) {

    var key = storageKey(compId);

    if (!key) return null;

    try {

      var raw = localStorage.getItem(key);

      if (!raw) return null;

      var o = JSON.parse(raw);

      return o && typeof o === 'object' ? o : null;

    } catch (e) {

      return null;

    }

  }



  function writeStored(compId, data) {

    var key = storageKey(compId);

    if (!key || !data) return;

    try {

      localStorage.setItem(key, JSON.stringify(data));

    } catch (e) { /* ignore quota */ }

  }



  function clearStored(compId) {

    var key = storageKey(compId);

    if (!key) return;

    try { localStorage.removeItem(key); } catch (e) { /* ignore */ }

  }



  function fieldVal(form, id) {

    if (!form || !id) return '';

    var el = form.querySelector('#' + id);

    return el ? String(el.value || '').trim() : '';

  }



  function readCountryIso2(form, fields) {

    if (global.PG_JPAY_CONTACT && fields.countrySelect) {

      var iso = global.PG_JPAY_CONTACT.readCountryIso2(form);

      if (iso) return iso;

    }

    if (fields.country) {

      return fieldVal(form, fields.country).toUpperCase();

    }

    if (fields.countrySelect) {

      var sel = form.querySelector('#' + fields.countrySelect);

      if (sel && sel.value) {

        var v = String(sel.value).trim().toUpperCase();

        if (v.length === 2) return v;

      }

    }

    return '';

  }



  function applyRemembered(form, compId, ctx, prefill, fieldPreset) {

    if (!form || !isEnabled(ctx)) return;

    var fields = resolveFieldIds(fieldPreset);

    var stored = readStored(compId);

    if (!stored) return;

    var p = prefill && typeof prefill === 'object' ? prefill : {};

    function hasPrefill(k) {

      var v = p[k];

      return v != null && String(v).trim() !== '';

    }

    if (!hasPrefill('email') && !hasPrefill('payEmailAddress') && stored.email) {

      var em = form.querySelector('#' + fields.email);

      if (em && !em.readOnly) em.value = stored.email;

    }

    if (!hasPrefill('phone') && !hasPrefill('payTelephone') && stored.phone) {

      var tel = form.querySelector('#' + fields.phone);

      if (tel && !tel.readOnly) tel.value = stored.phone;

    }

    if (!hasPrefill('firstName') && !hasPrefill('payFirstname') && stored.firstName) {

      var fn = form.querySelector('#' + fields.firstName);

      if (fn && !fn.readOnly) fn.value = stored.firstName;

    }

    if (!hasPrefill('lastName') && !hasPrefill('payLastname') && stored.lastName) {

      var ln = form.querySelector('#' + fields.lastName);

      if (ln && !ln.readOnly) ln.value = stored.lastName;

    }

    var iso = stored.countryIso2 ? String(stored.countryIso2).trim().toUpperCase() : '';

    if (iso.length === 2 && !hasPrefill('countryIso2') && !hasPrefill('payCountryIsoCode2')) {

      if (fields.country && form.querySelector('#' + fields.country)) {

        var cc = form.querySelector('#' + fields.country);

        if (cc.tagName === 'SELECT' && cc.querySelector('option[value="' + iso + '"]')) {

          cc.value = iso;

        } else if (!cc.readOnly) {

          cc.value = iso;

        }

      } else if (fields.countrySelect && form.querySelector('#' + fields.countrySelect)) {

        var cs = form.querySelector('#' + fields.countrySelect);

        if (cs.querySelector('option[value="' + iso + '"]')) cs.value = iso;

        if (global.PG_JPAY_CONTACT) global.PG_JPAY_CONTACT.syncBeforeSubmit(form);

      } else if (fields.country) {

        var ch = form.querySelector('#' + fields.country);

        if (ch && !ch.readOnly) ch.value = iso;

      }

    }

  }



  function collectFromForm(form, fieldPreset) {

    if (!form) return null;

    var fields = resolveFieldIds(fieldPreset);

    return {

      email: fieldVal(form, fields.email),

      phone: fieldVal(form, fields.phone),

      firstName: fieldVal(form, fields.firstName),

      lastName: fieldVal(form, fields.lastName),

      countryIso2: readCountryIso2(form, fields)

    };

  }



  function saveIfChecked(form, compId, checkboxEl, fieldPreset) {

    if (!form || !checkboxEl || !checkboxEl.checked) return;

    var data = collectFromForm(form, fieldPreset);

    if (!data || (!data.email && !data.phone && !data.firstName && !data.lastName)) return;

    writeStored(compId, data);

  }



  function ensureCheckbox(contactRow, ctx, tFn) {

    // 자동기억은 정책(ctx)으로 켜지면 사용자 선택 없이 '항상' 저장한다.

    // 이전의 '다음에도 사용' 체크박스는 노출하지 않고, 저장을 강제하기 위한

    // 숨김·상시 체크 입력만 둔다(기존 saveIfChecked 호출부와의 호환 유지).

    if (!contactRow || !isEnabled(ctx)) return null;

    var existing = contactRow.querySelector('#jpayContactRemember');

    if (existing) {

      existing.checked = true;

      return existing;

    }

    var wrap = document.createElement('div');

    wrap.id = 'jpayContactRememberWrap';

    wrap.style.display = 'none';

    wrap.setAttribute('aria-hidden', 'true');

    wrap.innerHTML =

      '<input type="checkbox" id="jpayContactRemember" checked hidden>';

    contactRow.appendChild(wrap);

    return wrap.querySelector('#jpayContactRemember');

  }



  global.PG_JPAY_CONTACT_REMEMBER = {

    isEnabled: isEnabled,

    applyRemembered: applyRemembered,

    saveIfChecked: saveIfChecked,

    ensureCheckbox: ensureCheckbox,

    clearStored: clearStored,

    FIELD_PRESETS: FIELD_PRESETS

  };

})(typeof window !== 'undefined' ? window : global);

