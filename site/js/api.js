/**
 * PG 통합관리자 - 백엔드 API 클라이언트 (인증·공공사항·결제·업체)
 */
(function () {
  'use strict';

  var AUTH_TOKEN_KEY = 'pg_admin_token';
  var AUTH_USER_KEY = 'pg_admin_user';
  var AUTH_FLAG_KEY = 'pg_admin_auth';

  function apiT(ko, en) {
    try {
      if (window.PG_UI_I18N && typeof window.PG_UI_I18N.t === 'function') return String(window.PG_UI_I18N.t(String(ko)));
    } catch (e) {}
    return (en != null && String(en).trim() !== '') ? String(en) : String(ko);
  }
  /** 서버 ApiResponse.message — STATIC/맵 키 있으면 현재 언어로, 없으면 원문 */
  function serverMsgT(msg, fallbackKo, fallbackEn) {
    if (msg != null && String(msg).trim() !== '') return apiT(String(msg).trim(), fallbackEn);
    return apiT(fallbackKo, fallbackEn);
  }

  /** 운영 API 기본 (config.js 의 PG_PUBLIC_ICOPAY_API 와 동일 개념) */
  function publicApiRoot() {
    var p = (typeof window !== 'undefined' && window.PG_PUBLIC_ICOPAY_API) ? String(window.PG_PUBLIC_ICOPAY_API) : 'https://api.icopay.co.kr';
    return p.replace(/\/$/, '').trim() || 'https://api.icopay.co.kr';
  }

  /** icopay.co.kr / *.icopay.co.kr 관리자 정적( api 서브도메인 제외 ) — CSP상 api 직접 호출을 피할 때 config.js 와 동일 판별 */
  function isIcopayAdminPageHost(h) {
    if (!h || h === 'localhost' || h === '127.0.0.1' || h === 'api.icopay.co.kr') return false;
    return h === 'icopay.co.kr' || /\.icopay\.co\.kr$/i.test(h);
  }

  /**
   * 절대 URL만 반환. base 가 비면 상대 경로 /api 가 "현재 페이지 호스트"로 나가 카페24 등에서 전부 실패함.
   */
  function getBaseUrl() {
    var raw = window.PG_API_BASE != null ? String(window.PG_API_BASE) : '';
    var b = raw.replace(/\/$/, '').trim();
    if (b && !/^https?:\/\//i.test(b)) {
      b = '';
    }
    if (b) {
      try {
        var pageH = window.location && window.location.hostname;
        var bu = new URL(b);
        var bh = String(bu.hostname || '').toLowerCase();
        var ph = String(pageH || '').toLowerCase();
        if (isIcopayAdminPageHost(pageH) && bh === 'api.icopay.co.kr') {
          var og = (window.location.origin || '').replace(/\/$/, '').trim();
          if (og) return og;
        }
        /**
         * www·대표 도메인은 통합 배포(동일 origin /api 프록시)일 수 있어 그대로 둔다.
         * 그 외 *.icopay.co.kr 에서 PG_API_BASE 가 페이지와 같은 호스트면 정적 전용일 가능성이 높다.
         */
        if (isIcopayAdminPageHost(pageH) && ph === bh
            && ph !== 'icopay.co.kr' && ph !== 'www.icopay.co.kr'
            && /\.icopay\.co\.kr$/i.test(ph)) {
          return publicApiRoot();
        }
      } catch (eBaseNorm) { /* ignore */ }
      return b;
    }
    try {
      var h = window.location && window.location.hostname;
      if (h === 'api.icopay.co.kr' || h === 'localhost' || h === '127.0.0.1') {
        var og = (window.location.origin || '').replace(/\/$/, '').trim();
        if (og) return og;
      }
    } catch (e1) { /* ignore */ }
    return publicApiRoot();
  }

  function getToken() {
    return sessionStorage.getItem(AUTH_TOKEN_KEY) || '';
  }

  function setAuth(token, user) {
    if (token) sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    if (user) sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    sessionStorage.setItem(AUTH_FLAG_KEY, token ? '1' : '');
    if (token) sessionStorage.removeItem('pg_post_login_popup_shown');
  }

  function clearAuth() {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_USER_KEY);
    sessionStorage.removeItem(AUTH_FLAG_KEY);
    sessionStorage.removeItem('pg_post_login_popup_shown');
  }

  function acceptLanguageHeaders(extra) {
    var hdr = extra && typeof extra === 'object' ? Object.assign({}, extra) : {};
    try {
      var loc = window.PG_UI_I18N && typeof window.PG_UI_I18N.getLocale === 'function'
        ? String(window.PG_UI_I18N.getLocale() || 'KO').toUpperCase()
        : 'KO';
      var map = { KO: 'ko', EN: 'en', JP: 'ja', CH: 'zh', TH: 'th' };
      hdr['Accept-Language'] = map[loc] || 'ko';
    } catch (eAl) {
      hdr['Accept-Language'] = 'ko';
    }
    return hdr;
  }

  /**
   * JSON이 아닌 HTML(프록시 502/413, 옛 JAR에 없는 경로 등)이 와도 throw 대신 읽을 수 있는 메시지로 변환.
   */
  function fetchTextThenJson(url, init, badParseHead) {
    init = init || {};
    if (init.credentials === undefined) init.credentials = 'omit';
    if (init.mode === undefined) init.mode = 'cors';
    return fetch(url, init).then(function (res) {
      return res.text().then(function (text) {
        if (res.status === 401) {
          clearAuth();
          if (typeof window.location !== 'undefined') window.location.replace((window.location.origin || '') + '/login.html');
          return Promise.reject(new Error(apiT('인증이 만료되었습니다. 다시 로그인하세요.', 'Your session has expired. Please sign in again.')));
        }
        if (res.status === 413) {
          return Promise.reject(new Error(apiT(
            '업로드 파일 용량이 서버 제한을 초과했습니다. 파일 크기를 줄여 다시 시도하세요. (HTTP 413, 프록시/Nginx의 client_max_body_size도 6MB 이상 필요)',
            'Upload exceeds the server limit. Reduce file size and try again. (HTTP 413: check reverse proxy/Nginx client_max_body_size)'
          )));
        }
        if (res.status === 504 || res.status === 502 || res.status === 503) {
          var gwKo = '게이트웨이 시간 초과(HTTP ' + res.status + '). 조회 기간을 줄인 뒤 [검색]을 다시 시도해 주세요.';
          var gwTpl = '게이트웨이 시간 초과(HTTP {0}). 조회 기간을 줄인 뒤 [검색]을 다시 시도해 주세요.';
          var gwMsg = apiT(gwTpl, 'Gateway timeout (HTTP {0}). Narrow the date range and click [Search] again.');
          if (url.indexOf('jpayTrSync') >= 0) {
            gwKo = 'JPAY 동기화 시간 초과(HTTP ' + res.status + '). Playwright Export에 5~15분 걸릴 수 있습니다. 서버 Nginx proxy_read_timeout(900초) 설정 후, 거래일자를 1~3일로 줄여 다시 시도하세요.';
            gwMsg = apiT(gwKo, 'JPAY sync timed out (HTTP ' + res.status + '). Export may take 5–15 min. Increase Nginx proxy_read_timeout (900s) and try a shorter date range (1–3 days).');
          } else if (url.indexOf('jpayTrSearch') >= 0) {
            gwKo = '통합조회 시간 초과(HTTP ' + res.status + '). [JPAY 동기화]는 별도 실행하세요. 동기화 중 504면 Nginx proxy_read_timeout(900초)를 확인하세요.';
            gwMsg = apiT(gwKo, 'Integrated query timed out (HTTP ' + res.status + '). Run [JPAY Sync] separately; if sync times out, check Nginx proxy_read_timeout (900s).');
          }
          if (gwMsg.indexOf('{0}') >= 0) gwMsg = gwMsg.replace('{0}', String(res.status));
          else if (url.indexOf('jpayTrSync') < 0 && url.indexOf('jpayTrSearch') < 0) {
            gwMsg = apiT(gwKo, 'Gateway timeout (HTTP ' + res.status + '). Narrow the date range and click [Search] again.');
          }
          return Promise.reject(new Error(gwMsg));
        }
        var data;
        try {
          data = text ? JSON.parse(text) : {};
        } catch (e1) {
          var flat = text ? String(text).replace(/<script[^>]*>[\s\S]*?<\/script>/gi, ' ').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 200) : '';
          var head = badParseHead || apiT('서버 응답이 JSON이 아닙니다.', 'Server response is not valid JSON.');
          return Promise.reject(new Error(head + ' HTTP ' + res.status + (flat ? (' — ' + flat) : '') + ' ' + apiT('(최신 pg-app 배포·Nginx 용량·502 등 확인)', '(check deployment/reverse proxy limits/502)')));
        }
        if (!res.ok) {
          var hint = (data && data.message) ? apiT(String(data.message)) : (text ? String(text).slice(0, 120) : '');
          return Promise.reject(new Error(apiT('API 오류', 'API error') + ' HTTP ' + res.status + (hint ? ': ' + hint : '')));
        }
        return data;
      });
    }).catch(function (err) {
      var msg = (err && err.message) ? err.message : '';
      if (msg === 'Failed to fetch' || msg.indexOf('NetworkError') !== -1 || msg.indexOf('Load failed') !== -1 || msg === 'Network request failed') {
        return Promise.reject(new Error(apiT('API에 연결하지 못했습니다.', 'Unable to connect to API.') + ' (' + url + ') ' + apiT('네트워크·호스팅 설정을 확인해 주세요.', 'Please check network and hosting settings.')));
      }
      return Promise.reject(err);
    });
  }

  function request(options) {
    var base = (options && options.baseOverride) ? String(options.baseOverride).replace(/\/$/, '') : getBaseUrl();
    var path = options.path || options.url || '';
    if (path && path.charAt(0) !== '/') {
      path = '/' + path;
    }
    var url = base + path;
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = getToken();
    if (token && !options.anonymous) headers['Authorization'] = 'Bearer ' + token;
    if (options.headers) {
      for (var k in options.headers) headers[k] = options.headers[k];
    }
    var init = { method: options.method || 'GET', headers: headers };
    if (options.body) init.body = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
    if (options.params && (options.method === 'GET' || !options.method)) {
      var q = [];
      for (var p in options.params) {
        if (options.params[p] !== undefined && options.params[p] !== null && options.params[p] !== '') {
          q.push(encodeURIComponent(p) + '=' + encodeURIComponent(options.params[p]));
        }
      }
      if (q.length) url += (url.indexOf('?') >= 0 ? '&' : '?') + q.join('&');
    }
    if (!url) return Promise.reject(new Error(apiT('API 경로가 없습니다.', 'Missing API URL/path.')));
    // 교차 출처(관리자 도메인 ≠ API 도메인)에서 same-origin + Allow-Credentials 조합이 막히는 경우 방지.
    // Authorization: Bearer 는 omit 이어도 전송됨(쿠키만 생략).
    init.credentials = 'omit';
    init.mode = 'cors';
    init.redirect = 'manual';
    return fetch(url, init).then(function (res) {
      if (res.status === 401) {
        if (!options.anonymous) {
          clearAuth();
          if (typeof window.location !== 'undefined') window.location.replace((window.location.origin || '') + '/login.html');
        }
        return Promise.reject(new Error(apiT('인증이 만료되었습니다. 다시 로그인하세요.', 'Your session has expired. Please sign in again.')));
      }
      if (res.status === 301 || res.status === 302 || res.status === 303 || res.status === 307 || res.status === 308) {
        var loc = res.headers && res.headers.get ? res.headers.get('Location') : '';
        return Promise.reject(new Error(apiT(
          'API가 HTML 로그인·정적 페이지로 리다이렉트되었습니다(' + res.status + (loc ? (': ' + loc) : '') + '). Nginx/Apache에서 location /api/ 가 pg-app(8080)으로 프록시되는지 확인하세요.',
          'API was redirected to HTML (' + res.status + '). Ensure /api/ is proxied to pg-app.'
        )));
      }
      return res.text().then(function (text) {
        var data;
        var trimmed = text ? String(text).trim() : '';
        try { data = trimmed ? JSON.parse(trimmed) : {}; } catch (e) { data = {}; }
        var looksHtml = trimmed && (
          trimmed.charAt(0) === '<' ||
          /^<!DOCTYPE/i.test(trimmed) ||
          /^<html[\s>]/i.test(trimmed)
        );
        if (res.ok && looksHtml) {
          var canRetryPublicApi = !options._retriedPublicApi
            && path.indexOf('/api/') === 0
            && base !== publicApiRoot();
          if (canRetryPublicApi) {
            var retryApiOpt = {};
            for (var rkApi in options) retryApiOpt[rkApi] = options[rkApi];
            retryApiOpt.baseOverride = publicApiRoot();
            retryApiOpt._retriedPublicApi = true;
            return request(retryApiOpt);
          }
          return Promise.reject(new Error(apiT(
            'API 응답이 HTML입니다(정적 index·로그인 페이지). 최신 pg-app JAR 배포 후 Nginx에서 location /api/ → 127.0.0.1:8080 프록시를 확인하세요. 요청: ' + url.split('?')[0],
            'API returned HTML instead of JSON. Deploy pg-app and proxy /api/ to port 8080. URL: ' + url.split('?')[0]
          )));
        }
        if (!res.ok) {
          var hint = (data && data.message) ? apiT(String(data.message)) : (text ? String(text).slice(0, 120) : '');
          return Promise.reject(new Error(
            apiT('API 오류', 'API error') + ' HTTP ' + res.status + (hint ? (': ' + hint) : '') +
            ' ' + apiT('(도메인/프록시·CORS 확인)', '(check domain/reverse proxy/CORS)')
          ));
        }
        if (data.success === false) {
          /* 서버 실패 메시지 — STATIC/맵 키 있으면 현재 언어로 (모든 alert(e.message)에 일괄 적용) */
          var failMsg = (data.message != null && String(data.message).trim() !== '')
            ? apiT(String(data.message).trim())
            : (apiT('요청 처리에 실패했습니다.', 'Request failed.') + (data.errorCode ? ' [' + data.errorCode + ']' : ''));
          var failErr = new Error(failMsg);
          if (data.errorCode) failErr.errorCode = data.errorCode;
          return Promise.reject(failErr);
        }
        return data;
      });
    }).catch(function (err) {
      var msg = (err && err.message) ? err.message : '';
      var isNetworkFail = (msg === 'Failed to fetch' || msg.indexOf('NetworkError') !== -1 || msg.indexOf('Load failed') !== -1 || msg === 'Network request failed');
      if (isNetworkFail) {
        // 정적 호스팅/리버스프록시 환경에서 공용 API 도메인 실패 시 현재 도메인 /api 로 1회 폴백
        var canRetrySameOrigin = !options._retriedSameOrigin
          && path.indexOf('/api/') === 0
          && typeof window !== 'undefined'
          && window.location
          && window.location.origin
          && base !== String(window.location.origin).replace(/\/$/, '');
        if (canRetrySameOrigin) {
          var retryOpt = {};
          for (var rk in options) retryOpt[rk] = options[rk];
          retryOpt.baseOverride = String(window.location.origin).replace(/\/$/, '');
          retryOpt._retriedSameOrigin = true;
          return request(retryOpt);
        }
        var apiRootHint = publicApiRoot();
        return Promise.reject(new Error(
          apiT('API에 연결하지 못했습니다.', 'Unable to connect to API.') + ' (' + url + ') ' +
            apiT('인터넷 연결, 정적 호스팅 보안 정책(connect-src에', 'Check internet connection, static hosting security policy (allow connect-src') + ' ' +
            apiRootHint + apiT('허용), 리버스 프록시·SSL·방화벽을 확인해 주세요.', '), reverse proxy, SSL, and firewall.')
        ));
      }
      return Promise.reject(err);
    });
  }

  function get(path, params, extra) {
    extra = extra || {};
    return request({
      path: path,
      method: 'GET',
      params: params || {},
      headers: extra.headers || {},
      anonymous: !!extra.anonymous
    });
  }

  function post(path, body, extra) {
    extra = extra || {};
    return request({
      path: path,
      method: 'POST',
      body: body || {},
      headers: extra.headers || {}
    });
  }

  function put(path, body) {
    return request({ path: path, method: 'PUT', body: body || {} });
  }

  function del(path) {
    return request({ path: path, method: 'DELETE' });
  }

  function fileToDataUrl(file) {
    return new Promise(function (resolve, reject) {
      try {
        var fr = new FileReader();
        fr.onload = function () { resolve(String(fr.result || '')); };
        fr.onerror = function () { reject(new Error(apiT('이미지 읽기에 실패했습니다.', 'Failed to read image.'))); };
        fr.readAsDataURL(file);
      } catch (e) {
        reject(e);
      }
    });
  }

  function dataUrlToBlob(dataUrl) {
    var arr = String(dataUrl || '').split(',');
    var mime = (arr[0].match(/:(.*?);/) || [])[1] || 'image/jpeg';
    var bstr = atob(arr[1] || '');
    var n = bstr.length;
    var u8arr = new Uint8Array(n);
    while (n--) u8arr[n] = bstr.charCodeAt(n);
    return new Blob([u8arr], { type: mime });
  }

  function compressImageForUpload(file, maxBytes) {
    var safeMax = Math.max(200 * 1024, Number(maxBytes) || (900 * 1024));
    return fileToDataUrl(file).then(function (dataUrl) {
      return new Promise(function (resolve, reject) {
        var img = new Image();
        img.onload = function () {
          try {
            var canvas = document.createElement('canvas');
            var width = img.width || 0;
            var height = img.height || 0;
            if (!width || !height) {
              return reject(new Error(apiT('이미지 크기를 확인할 수 없습니다.', 'Unable to determine image dimensions.')));
            }
            // 과도한 고해상도 이미지는 먼저 축소
            var maxSide = 2200;
            if (width > maxSide || height > maxSide) {
              var scale = Math.min(maxSide / width, maxSide / height);
              width = Math.max(1, Math.round(width * scale));
              height = Math.max(1, Math.round(height * scale));
            }
            canvas.width = width;
            canvas.height = height;
            var ctx = canvas.getContext('2d');
            if (!ctx) return reject(new Error(apiT('이미지 압축 컨텍스트를 생성할 수 없습니다.', 'Unable to create image compression context.')));
            ctx.drawImage(img, 0, 0, width, height);

            var q = 0.9;
            var outData = '';
            var outBlob = null;
            while (q >= 0.45) {
              outData = canvas.toDataURL('image/jpeg', q);
              outBlob = dataUrlToBlob(outData);
              if (outBlob.size <= safeMax) break;
              q -= 0.1;
            }
            if (!outBlob) return reject(new Error(apiT('이미지 압축 결과가 비어 있습니다.', 'Image compression result is empty.')));
            var ext = outBlob.type === 'image/png' ? 'png' : 'jpg';
            var baseName = String(file.name || 'upload').replace(/\.[^/.]+$/, '');
            var outName = baseName + '_compressed.' + ext;
            resolve(new File([outBlob], outName, { type: outBlob.type || 'image/jpeg', lastModified: Date.now() }));
          } catch (e2) {
            reject(e2);
          }
        };
        img.onerror = function () { reject(new Error(apiT('이미지 로딩에 실패했습니다.', 'Failed to load image.'))); };
        img.src = dataUrl;
      });
    });
  }

  function dashboardPayloadNeedsExt(d) {
    if (!d || typeof d !== 'object') return true;
    if (!d.insights || typeof d.insights !== 'object') return true;
    var role = String(d.role || '').toUpperCase();
    var ol = String(d.orgLevel || '').toUpperCase();
    var needHub = role === 'ADMIN' || ol === 'HEADQUARTERS';
    if (!needHub) return false;
    var h = d.hqHub;
    if (h == null || typeof h !== 'object') return true;
    return !(h.variant || h.title);
  }

  function mergeDashboardExt(d, extBody) {
    if (!d || typeof d !== 'object' || !extBody || typeof extBody !== 'object') return d;
    if (extBody.insights && typeof extBody.insights === 'object') {
      d.insights = extBody.insights;
    }
    if (Object.prototype.hasOwnProperty.call(extBody, 'hqHub')) {
      d.hqHub = extBody.hqHub;
    }
    if (extBody.orgLevel && (d.orgLevel == null || String(d.orgLevel).trim() === '')) {
      d.orgLevel = extBody.orgLevel;
    }
    if (extBody.role && (d.role == null || String(d.role).trim() === '')) {
      d.role = extBody.role;
    }
    return d;
  }

  /** ApiResponse { success, data:{ list, meta } } · 이중 래핑·data가 배열·래퍼 없는 {list,meta} 정규화 */
  function unwrapListMetaApiPayload(r) {
    if (!r || typeof r !== 'object') return null;
    if (r.success === false) return null;
    if (r.error && r.status != null && r.data == null && !Array.isArray(r.list)) return null;
    var p = r.data;
    if (p == null && (Array.isArray(r.list) || (r.meta && typeof r.meta === 'object'))) {
      p = r;
    }
    if (Array.isArray(p)) {
      return { list: p, meta: (r.meta && typeof r.meta === 'object') ? r.meta : {} };
    }
    if (p && typeof p === 'object') {
      if (p.success === true && p.data != null) {
        p = p.data;
      }
      if (Array.isArray(p)) {
        return { list: p, meta: (r.meta && typeof r.meta === 'object') ? r.meta : {} };
      }
      if (p && typeof p === 'object' && !Array.isArray(p)) {
        var out = p;
        if (!Array.isArray(out.list)) {
          if (Array.isArray(out.content)) out.list = out.content;
          else out.list = [];
        }
        if (!out.meta || typeof out.meta !== 'object') out.meta = {};
        return out;
      }
    }
    if (Array.isArray(r.list)) {
      return { list: r.list, meta: (r.meta && typeof r.meta === 'object') ? r.meta : {} };
    }
    return null;
  }

  var FLOW_DOC_LANG_PATHS = {
    EN: 'merchant-api-samples/docs/unified-checkout-api-flow.html',
    KO: 'merchant-api-samples/docs/unified-checkout-api-flow.ko.html',
    JP: 'merchant-api-samples/docs/unified-checkout-api-flow.ja.html',
    CH: 'merchant-api-samples/docs/unified-checkout-api-flow.ch.html',
    TH: 'merchant-api-samples/docs/unified-checkout-api-flow.th.html'
  };
  var PARAM_DOC_LANG_PATHS = {
    EN: 'merchant-api-samples/docs/unified-checkout-api-parameters.html',
    KO: 'merchant-api-samples/docs/unified-checkout-api-parameters.ko.html',
    JP: 'merchant-api-samples/docs/unified-checkout-api-parameters.ja.html',
    CH: 'merchant-api-samples/docs/unified-checkout-api-parameters.ch.html',
    TH: 'merchant-api-samples/docs/unified-checkout-api-parameters.th.html'
  };
  var PARAM_TXT_LANG_PATHS = {
    EN: 'merchant-api-samples/docs/unified-checkout-api-parameters.txt',
    KO: 'merchant-api-samples/docs/unified-checkout-api-parameters.ko.txt',
    JP: 'merchant-api-samples/docs/unified-checkout-api-parameters.ja.txt',
    CH: 'merchant-api-samples/docs/unified-checkout-api-parameters.ch.txt',
    TH: 'merchant-api-samples/docs/unified-checkout-api-parameters.th.txt'
  };

  function sampleDocLangFromPath(docPath, family) {
    var p = String(docPath || '');
    if (/\.ko\.(html|txt)$/i.test(p)) return 'KO';
    if (/\.ja\.(html|txt)$/i.test(p)) return 'JP';
    if (/\.ch\.(html|txt)$/i.test(p)) return 'CH';
    if (/\.th\.(html|txt)$/i.test(p)) return 'TH';
    if (family === 'flow' && /unified-checkout-api-flow\.html$/i.test(p)) return 'EN';
    if (family === 'param' && /unified-checkout-api-parameters\.html$/i.test(p)) return 'EN';
    if (family === 'param' && /unified-checkout-api-parameters\.txt$/i.test(p)) return 'EN';
    if (family === 'flow' && /unified-checkout-api-flow\.txt$/i.test(p)) return 'EN';
    return '';
  }

  function sampleDocLangInfo(docPath) {
    var p = String(docPath || '');
    if (/unified-checkout-api-flow/i.test(p)) {
      return { family: 'flow', cur: sampleDocLangFromPath(p, 'flow'), paths: FLOW_DOC_LANG_PATHS };
    }
    if (/unified-checkout-api-parameters/i.test(p)) {
      return { family: 'param', cur: sampleDocLangFromPath(p, 'param'), paths: PARAM_DOC_LANG_PATHS };
    }
    return null;
  }

  function sampleLangNavLabel(cur) {
    if (cur === 'KO') return '언어:';
    if (cur === 'JP') return '言語:';
    if (cur === 'CH') return '语言:';
    if (cur === 'TH') return 'ภาษา:';
    return 'Language:';
  }

  function buildSampleLangNavHtml(info) {
    var langs = [
      { c: 'EN', t: 'English' },
      { c: 'KO', t: '한국어' },
      { c: 'JP', t: '日本語' },
      { c: 'CH', t: '中文' },
      { c: 'TH', t: 'ไทย' }
    ];
    var parts = langs.map(function (x) {
      if (x.c === info.cur) return '<strong>' + x.t + '</strong>';
      return '<a href="#" data-pg-doc-lang="' + x.c + '" data-pg-doc-family="' + info.family + '">' + x.t + '</a>';
    });
    return '<p class="lang-nav pg-sample-doc-lang-nav"><strong>' + sampleLangNavLabel(info.cur) + '</strong> ' + parts.join(' · ') + '</p>';
  }

  function sampleMimeForPath(path) {
    var lower = String(path || '').toLowerCase();
    if (lower.endsWith('.txt')) return 'text/plain;charset=UTF-8';
    if (lower.endsWith('.json')) return 'application/json;charset=UTF-8';
    if (lower.endsWith('.php')) return 'application/x-php;charset=UTF-8';
    if (lower.endsWith('.java') || lower.endsWith('.properties')) return 'text/plain;charset=UTF-8';
    if (lower.endsWith('.js')) return 'application/javascript;charset=UTF-8';
    return 'text/html;charset=UTF-8';
  }

  function isMerchantSampleHtmlPath(docPath) {
    return /merchant-api-samples\/.*\.html$/i.test(String(docPath || ''));
  }

  function sampleHrefToClasspath(href, docPath) {
    var h = String(href || '').trim();
    if (!h || /^https?:/i.test(h) || h.charAt(0) === '#') return null;
    if (h.indexOf('merchant-api-samples/') === 0) return h.replace(/^\//, '');
    if (h.charAt(0) === '/') return h.replace(/^\//, '');

    var dp = String(docPath || '').replace(/^\//, '');
    var baseDir = 'merchant-api-samples/';
    if (/merchant-api-samples\/docs\//i.test(dp)) baseDir = 'merchant-api-samples/docs/';
    else if (/merchant-api-samples\/index\.html$/i.test(dp)) baseDir = 'merchant-api-samples/';
    else if (/merchant-api-samples\//i.test(dp)) {
      var slash = dp.lastIndexOf('/');
      baseDir = slash >= 0 ? dp.substring(0, slash + 1) : 'merchant-api-samples/';
    }

    if (h.indexOf('../') === 0) {
      var parts = baseDir.replace(/\/$/, '').split('/');
      var rel = h.split('/');
      for (var i = 0; i < rel.length; i++) {
        if (rel[i] === '..') {
          if (parts.length) parts.pop();
        } else if (rel[i] && rel[i] !== '.') {
          parts.push(rel[i]);
        }
      }
      return parts.join('/');
    }

    if (/^(docs|json|php|jsp|common)\//i.test(h)) return 'merchant-api-samples/' + h;
    if (baseDir === 'merchant-api-samples/docs/') return baseDir + h;
    return 'merchant-api-samples/' + h;
  }

  function rewriteSampleDocAnchors(html, docPath) {
    return String(html || '').replace(/<a\s+([^>]*?)href="([^"]+)"([^>]*)>/gi, function (full, pre, href, post) {
      if (/\bdata-pg-sample-doc=/.test(pre + post)) return full;
      var path = sampleHrefToClasspath(href, docPath);
      if (!path || !/^merchant-api-samples\//i.test(path)) return full;
      var mime = sampleMimeForPath(path);
      return '<a ' + pre + 'href="#" data-pg-sample-doc="' + path + '" data-pg-sample-mime="' + mime + '"' + post + '>';
    });
  }

  function appendSampleDocBlobNavScript(out) {
    if (String(out || '').indexOf('pgSampleDocBlobNav') >= 0) return out;
    var script = '<script id="pgSampleDocBlobNav">(function(){'
      + 'var FLOW={EN:"' + FLOW_DOC_LANG_PATHS.EN + '",KO:"' + FLOW_DOC_LANG_PATHS.KO + '",JP:"' + FLOW_DOC_LANG_PATHS.JP
      + '",CH:"' + FLOW_DOC_LANG_PATHS.CH + '",TH:"' + FLOW_DOC_LANG_PATHS.TH + '"};'
      + 'var PARAM={EN:"' + PARAM_DOC_LANG_PATHS.EN + '",KO:"' + PARAM_DOC_LANG_PATHS.KO + '",JP:"' + PARAM_DOC_LANG_PATHS.JP
      + '",CH:"' + PARAM_DOC_LANG_PATHS.CH + '",TH:"' + PARAM_DOC_LANG_PATHS.TH + '"};'
      + 'function openDoc(path,mime){if(!path)return;mime=mime||"text/html;charset=UTF-8";'
      + 'try{if(window.opener&&window.opener.PG_API&&window.opener.PG_API.openSampleDoc){window.opener.PG_API.openSampleDoc(path,mime);try{window.close();}catch(x){}return;}}catch(e1){}'
      + 'try{if(window.opener){window.opener.postMessage({type:"pg-open-sample-doc",path:path,mime:mime},"*");try{window.close();}catch(x2){}}}catch(e2){}}'
      + 'document.querySelectorAll("[data-pg-sample-doc]").forEach(function(el){el.addEventListener("click",function(ev){'
      + 'ev.preventDefault();openDoc(el.getAttribute("data-pg-sample-doc"),el.getAttribute("data-pg-sample-mime"));});});'
      + 'document.querySelectorAll("[data-pg-doc-lang]").forEach(function(el){el.addEventListener("click",function(ev){'
      + 'ev.preventDefault();var fam=el.getAttribute("data-pg-doc-family")||"flow";var lang=el.getAttribute("data-pg-doc-lang");'
      + 'var map=fam==="param"?PARAM:FLOW;var p=map[lang];if(p)openDoc(p,"text/html;charset=UTF-8");});});'
      + '})();</script>';
    if (/<\/body>/i.test(out)) return out.replace(/<\/body>/i, script + '</body>');
    return out + script;
  }

  function injectMerchantSampleDocHtml(html, docPath) {
    if (!isMerchantSampleHtmlPath(docPath)) return String(html || '');
    var out = String(html || '');
    var info = sampleDocLangInfo(docPath);
    if (info && info.cur) {
      var navHtml = buildSampleLangNavHtml(info);
      if (/<p class="lang-nav[^"]*">[\s\S]*?<\/p>/i.test(out)) {
        out = out.replace(/<p class="lang-nav[^"]*">[\s\S]*?<\/p>/i, navHtml);
      } else {
        out = out.replace(/<body([^>]*)>/i, '<body$1>' + navHtml);
      }
    }
    out = rewriteSampleDocAnchors(out, docPath);
    return appendSampleDocBlobNavScript(out);
  }

  function fetchSampleDoc(path) {
    var p = String(path || '').replace(/^\//, '').trim();
    if (!p) {
      return Promise.reject(new Error(apiT('문서 경로가 없습니다.', 'Document path is missing.')));
    }
    var url = getBaseUrl() + '/api/merchant-api-samples/doc?path=' + encodeURIComponent(p);
    return fetch(url, {
      credentials: 'omit',
      mode: 'cors',
      headers: {
        Authorization: 'Bearer ' + getToken(),
        Accept: 'text/html, text/plain, application/json, */*'
      }
    }).then(function (res) {
      if (res.status === 401) {
        clearAuth();
        if (typeof window.location !== 'undefined') {
          window.location.replace((window.location.origin || '') + '/login.html');
        }
        return Promise.reject(new Error(apiT('인증이 만료되었습니다. 다시 로그인하세요.', 'Your session has expired. Please sign in again.')));
      }
      if (!res.ok) {
        return Promise.reject(new Error(apiT(
          '문서를 불러올 수 없습니다. (HTTP ' + res.status + ')',
          'Could not load document. (HTTP ' + res.status + ')'
        )));
      }
      return res.text();
    });
  }

  window.PG_API = {
    getToken: getToken,
    setAuth: setAuth,
    clearAuth: clearAuth,
    getBaseUrl: getBaseUrl,

    login: function (username, password, totpCode) {
      var ch = '';
      try {
        if (typeof location !== 'undefined' && location.host) ch = location.host;
      } catch (e) {}
      var body = { username: username, password: password, clientHost: ch };
      if (totpCode != null && String(totpCode).trim() !== '') {
        body.totpCode = String(totpCode).trim();
      }
      return post('/api/auth/login', body);
    },

    /** 현재 토큰 기준 사용자·소속 업체 (업체정보조회 등) */
    authMe: function () {
      return get('/api/auth/me');
    },

    /** 메인(/main) 대시보드: 조직별 거래 요약·서버 요약·가맹 정산 달력 */
    /** 메인 영업일 3개월 — anchorMonth: YYYY-MM (당월=기준, 표시는 전·당·익월) */
    dashboardBusinessDayCalendar: function (anchorMonth) {
      var params = {};
      if (anchorMonth) params.anchorMonth = String(anchorMonth).substring(0, 7);
      return request({
        path: '/api/dashboard/businessDayCalendar',
        method: 'GET',
        params: params,
        headers: { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' }
      }).then(function (r) {
        if (r && r.success === false && r.success !== undefined) {
          throw new Error(serverMsgT(r.message, '영업일 달력 조회 실패', 'Failed to load business-day calendar.'));
        }
        var d = r && r.data != null ? r.data : r;
        if (d && typeof d === 'object' && d.data && typeof d.data === 'object' && d.data.months) {
          d = d.data;
        }
        return d && typeof d === 'object' ? d : {};
      });
    },

    dashboardHome: function () {
      var noStore = { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' };
      return request({
        path: '/api/dashboard/home',
        method: 'GET',
        params: {},
        headers: acceptLanguageHeaders(noStore)
      }).then(function (r) {
        if (r && r.success === false && r.success !== undefined) {
          throw new Error(serverMsgT(r.message, '대시보드 조회 실패', 'Failed to load dashboard.'));
        }
        var d = r && r.data != null ? r.data : r;
        if (d && typeof d === 'object' && d.data && typeof d.data === 'object' && d.data.sales) {
          d = d.data;
        }
        if (!d || typeof d !== 'object') return {};
        try {
          var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
          if ((d.orgLevel == null || String(d.orgLevel).trim() === '') && u.orgLevel) {
            d.orgLevel = u.orgLevel;
          }
          if ((d.role == null || String(d.role).trim() === '') && u.role) {
            d.role = u.role;
          }
        } catch (eDashUser) { /* ignore */ }
        if (!dashboardPayloadNeedsExt(d)) return d;
        return request({
          path: '/api/dashboard/ext',
          method: 'GET',
          params: {},
          headers: noStore
        }).then(function (r2) {
          if (r2 && r2.success === false && r2.success !== undefined) {
            return d;
          }
          var ext = r2 && r2.data != null ? r2.data : r2;
          if (ext && typeof ext === 'object' && ext.data && typeof ext.data === 'object' && ext.data.insights) {
            ext = ext.data;
          }
          return mergeDashboardExt(d, ext && typeof ext === 'object' ? ext : {});
        }).catch(function () {
          return d;
        });
      });
    },

    authChangePassword: function (currentPassword, newPassword, confirmPassword) {
      return post('/api/auth/change-password', {
        currentPassword: currentPassword || '',
        newPassword: newPassword || '',
        confirmPassword: confirmPassword || ''
      });
    },
    authChangeName: function (newName) {
      return post('/api/auth/change-name', {
        newName: newName || ''
      });
    },

    /** Google OTP 등록 — 이메일 인증번호는 서버에서 ziobizm@gmail.com 으로만 발송 */
    otpEnrollRequestEmail: function () {
      return post('/api/auth/otp/enroll/request-email', {});
    },
    otpEnrollVerifyEmail: function (emailCode) {
      return post('/api/auth/otp/enroll/verify-email', { emailCode: emailCode || '' });
    },
    otpEnrollActivate: function (totpCode) {
      return post('/api/auth/otp/enroll/activate', { totpCode: totpCode || '' });
    },

    noticeDeployTargets: function () {
      return get('/api/system/notice/deploy-targets').then(function (r) { return r.data; });
    },

    noticeList: function (params) {
      return get('/api/system/notice', params).then(function (r) { return r.data; });
    },

    noticeGet: function (id) {
      return get('/api/system/notice/' + encodeURIComponent(id)).then(function (r) { return r.data; });
    },

    noticeDisplayGet: function (id) {
      return get('/api/system/notice/' + encodeURIComponent(id) + '/display', {}, { headers: acceptLanguageHeaders({}) }).then(function (r) { return r.data; });
    },

    noticeCreate: function (title, content, opts) {
      opts = opts || {};
      return post('/api/system/notice', {
        title: title || '',
        content: content || '',
        showOnLogin: !!opts.showOnLogin,
        showAsPopup: !!opts.showAsPopup,
        showPostLoginPopup: !!opts.showPostLoginPopup,
        showOnMain: !!opts.showOnMain,
        deployTarget: opts.deployTarget || '',
        targetOrgUnitIds: Array.isArray(opts.targetOrgUnitIds) ? opts.targetOrgUnitIds : []
      });
    },

    noticeUpdate: function (id, payload) {
      return request({ path: '/api/system/notice/' + encodeURIComponent(id), method: 'PUT', body: payload || {} });
    },

    noticeDelete: function (id) {
      return request({ path: '/api/system/notice/' + encodeURIComponent(id), method: 'DELETE' });
    },

    noticePinLoginHome: function (id) {
      return post('/api/system/notice/' + encodeURIComponent(id) + '/login-home', {});
    },

    noticePinLoginPopup: function (id) {
      return post('/api/system/notice/' + encodeURIComponent(id) + '/login-popup', {});
    },

    noticePinPostLoginPopup: function (id) {
      return post('/api/system/notice/' + encodeURIComponent(id) + '/post-login-popup', {});
    },

    noticePinMainNotice: function (id) {
      return post('/api/system/notice/' + encodeURIComponent(id) + '/main-notice', {});
    },

    noticePostLoginPopupDisplay: function () {
      return get('/api/system/notice/display/post-login-popup', {}, { headers: acceptLanguageHeaders({}) }).then(function (r) { return r.data; });
    },

    /** 로그인 첫 화면·접속팝업 공지(비로그인). Accept-Language 는 단말 기본 언어(navigator) 기준으로 전달합니다. */
    loginNoticePublic: function () {
      var hdr = {};
      try {
        hdr['Accept-Language'] = (navigator.languages && navigator.languages.length)
          ? navigator.languages.join(',')
          : (navigator.language || 'ko');
      } catch (eAl) { hdr['Accept-Language'] = 'ko'; }
      return get('/api/pub/login-notice', {}, { anonymous: true, headers: hdr }).then(function (r) { return r.data; });
    },

    payList: function (params) {
      return get('/api/calc/payList', params, { headers: acceptLanguageHeaders({}) }).then(function (r) { return r.data; });
    },
    /** 결제내역 처리사유 — 언어 전환 시 캐시·사전 번역만(AI 호출 없음, 저장 시 예열) */
    outcomeReasonTranslate: function (texts, locale) {
      var loc = locale;
      try {
        if (!loc && window.PG_PAY_LIST_I18N && typeof window.PG_PAY_LIST_I18N.getLocale === 'function') {
          loc = window.PG_PAY_LIST_I18N.getLocale();
        }
        if (!loc && window.PG_UI_I18N && typeof window.PG_UI_I18N.getLocale === 'function') {
          loc = window.PG_UI_I18N.getLocale();
        }
      } catch (eLoc) { /* ignore */ }
      return post('/api/calc/outcomeReasonTranslate',
        { texts: texts || [], locale: loc || 'KO' },
        { headers: acceptLanguageHeaders({}) }
      ).then(function (r) { return r.data; });
    },
    /** 노티매핑 반영: 결제내역 계열 화면별 그리드 레이아웃·표시 제목 */
    payListScreenLayout: function (pageUrl) {
      return get('/api/calc/payListScreenLayout', { pageUrl: pageUrl || '' }).then(function (r) { return r.data; });
    },
    /** ChillPay Transaction API — Search Payment Transaction (실시간) */
    chillPayTrSearch: function (params) {
      return get('/api/calc/chillPayTrSearch', params).then(function (r) { return r.data; });
    },
    jpayTrSearch: function (params) {
      return get('/api/calc/jpayTrSearch', params).then(function (r) { return r.data; });
    },
    splitPayMerchantConfig: function (compId) {
      return get('/api/pay/split/merchant-config', { compId: compId || '' }).then(function (r) { return r.data; });
    },
    splitPayPreview: function (body) {
      return post('/api/pay/split/preview', body || {}).then(function (r) { return r.data; });
    },
    splitPayCreateContract: function (body) {
      return post('/api/pay/split/contracts', body || {}).then(function (r) { return r.data; });
    },
    splitPayInstallmentContext: function (token) {
      return get('/api/pay/split/installment', { token: token || '' }).then(function (r) { return r.data; });
    },
    splitPayListSearch: function (params) {
      return get('/api/calc/splitPayList', params).then(function (r) { return r.data; });
    },
    splitPayProgressListSearch: function (params) {
      return get('/api/splitpay/progressList', params).then(function (r) { return r.data; });
    },
    splitPayMailListSearch: function (params) {
      return get('/api/splitpay/mailList', params).then(function (r) { return r.data; });
    },
    splitPayResendMail: function (body) {
      return post('/api/splitpay/resendMail', body || {}).then(function (r) { return r.data; });
    },
    splitPayEmailSettingsGet: function () {
      return get('/api/splitpay/emailSettings').then(function (r) { return r.data; });
    },
    splitPayEmailSettingsSave: function (body) {
      return post('/api/splitpay/emailSettings/save', body || {}).then(function (r) { return r.data; });
    },
    splitPayEmailSettingsTest: function (body) {
      return post('/api/splitpay/emailSettings/test', body || {}).then(function (r) { return r.data; });
    },
    jpayTrSync: function (params) {
      var q = params || {};
      var parts = [];
      Object.keys(q).forEach(function (k) {
        if (q[k] != null && String(q[k]).trim() !== '') parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(String(q[k])));
      });
      var path = '/api/calc/jpayTrSync' + (parts.length ? '?' + parts.join('&') : '');
      return post(path, {}).then(function (r) { return r.data; });
    },
    /** JPAY 통합내역 비동기 동기화 진행 상태 폴링 */
    jpayTrSyncStatus: function () {
      return get('/api/calc/jpayTrSyncStatus', {}).then(function (r) { return r.data; });
    },
    jpayTradeQuery: function (body) {
      return post('/api/calc/jpayTradeQuery', body || {}).then(function (r) { return r.data; });
    },
    dailyChillIntegratedSummary: function (params) {
      return get('/api/calc/dailyChillIntegratedSummary', params).then(function (r) { return r.data; });
    },
    dailyJpayIntegratedSummary: function (params) {
      return get('/api/calc/dailyJpayIntegratedSummary', params).then(function (r) { return r.data; });
    },
    dailyPaySummary: function (params) {
      return get('/api/calc/dailyPaySummary', params).then(function (r) { return r.data; });
    },
    /** ChillPay 통합정산 — Search Settlement Transaction(/api/v1/settlement/search) */
    chillPaySettlementSearch: function (params) {
      return get('/api/calc/chillPaySettlementSearch', params).then(function (r) { return r.data; });
    },

    /** TEMP_REMOVE_AFTER_DEV — 임시: 선택 조직+하위 프로필 미사용(N). ADMIN + comp-dev-tree-remove */
    compDevTreeRemove: function (compId) {
      return post('/api/comp/dev-tree-remove', { compId: compId || '' });
    },

    /** TEMP_REMOVE_AFTER_DEV — 임시: 업체 전체 초기화. ADMIN + allow-org-hierarchy-reset */
    compAdminResetOrgHierarchy: function () {
      return post('/api/comp/admin-reset-org-hierarchy', {});
    },

    compList: function (params) {
      return get('/api/comp/list', params).then(function (r) {
        var d = r && r.data;
        if (!d || typeof d !== 'object') return { list: [], totalElements: 0, totalPages: 1, page: 1, size: 20 };
        var list = d.list || d.content || [];
        var total = d.totalElements != null ? d.totalElements : (d.total != null ? d.total : list.length);
        var totalPages = d.totalPages != null ? d.totalPages : 1;
        return { list: list, totalElements: total, totalPages: totalPages, page: d.page != null ? d.page : 1, size: d.size != null ? d.size : 20 };
      });
    },

    pgAgencyList: function () {
      return get('/api/hq/pgAgencyList').then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패', 'Failed to load.'));
        return r.data || [];
      });
    },

    compRegister: function (data) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/register', {
        method: 'POST',
        headers: headers,
        body: new URLSearchParams(data)
      }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류 (API 서버가 실행 중인지, 주소가 맞는지 확인하세요)', 'Server response error (check API server status and address).'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '등록 실패', 'Registration failed.'));
          return r;
        });
      });
    },
    compCheckLoginId: function (loginId) {
      return get('/api/comp/check-login-id', { loginId: loginId || '' }).then(function (r) { return r.data; });
    },
    compCheckChatbotAdminUsername: function (compId, chatbotAdminUsername) {
      return get('/api/comp/check-chatbot-admin-username', {
        compId: String(compId || '').trim(),
        chatbotAdminUsername: chatbotAdminUsername != null ? String(chatbotAdminUsername) : ''
      }).then(function (r) { return r.data; });
    },

    /** 바이너리 응답 (xlsx 등) — JSON 파싱 안 함 */
    fetchBinary: function (path, init) {
      var base = getBaseUrl();
      var url = base + (path || '');
      var headers = {};
      var token = getToken();
      if (token) headers['Authorization'] = 'Bearer ' + token;
      if (init && init.headers) {
        for (var hk in init.headers) headers[hk] = init.headers[hk];
      }
      var opt = { method: (init && init.method) || 'GET', headers: headers, credentials: 'same-origin' };
      if (init && init.body) opt.body = init.body;
      return fetch(url, opt).then(function (res) {
        if (res.status === 401) {
          clearAuth();
          if (typeof window.location !== 'undefined') window.location.replace((window.location.origin || '') + '/login.html');
          return Promise.reject(new Error(apiT('인증이 만료되었습니다. 다시 로그인하세요.', 'Your session has expired. Please sign in again.')));
        }
        if (!res.ok) {
          return res.text().then(function (t) {
            throw new Error(t || (apiT('다운로드 실패', 'Download failed') + ' (HTTP ' + res.status + ')'));
          });
        }
        return res.blob();
      });
    },

    compExcelSample: function () {
      return this.fetchBinary('/api/comp/excelSample');
    },

    exportStyledExcel: function (payload) {
      return this.fetchBinary('/api/comp/exportStyledExcel', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        },
        body: JSON.stringify(payload || {})
      });
    },

    compExcelRegister: function (file) {
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('file', file);
      var headers = {};
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/excelRegister', {
        method: 'POST',
        headers: headers,
        body: fd
      }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '엑셀 등록 실패'));
          return r;
        });
      });
    },

    compDetail: function (compId) {
      return get('/api/comp/detail', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패'));
        return r.data;
      });
    },

    compPgBindingSave: function (body) {
      return post('/api/comp/pgBinding/save', body || {}).then(function (r) {
        return r.data;
      });
    },

    compPgBindingDelete: function (compId, bindingId) {
      var path = '/api/comp/pgBinding/' + encodeURIComponent(String(bindingId)) + '?compId=' + encodeURIComponent(compId || '');
      return del(path).then(function (r) {
        return r.data;
      });
    },

    compUpdate: function (data) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/update', {
        method: 'POST',
        headers: headers,
        body: new URLSearchParams(data)
      }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r;
          try {
            r = text ? JSON.parse(text) : {};
          } catch (e) {
            // 일부 운영 환경에서 업데이트 성공 후 빈 본문/비JSON 본문이 반환될 수 있으므로 성공으로 간주
            if (res.ok) return { success: true, data: {} };
            return Promise.reject(new Error(apiT('서버 응답 오류 (API 서버가 실행 중인지, 주소가 맞는지 확인하세요)', 'Server response error (check API server status and address).')));
          }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '수정 실패'));
          return r;
        });
      });
    },

    compChangeHistory: function (params) {
      return get('/api/comp/changeHistory', params).then(function (r) { return r.data; });
    },
    compResetPassword: function (compId) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/resetPassword', { method: 'POST', headers: headers, body: new URLSearchParams({ compId: compId }) })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
            if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '비밀번호 초기화 실패'));
            return r;
          });
        });
    },
    compResetAssistantPassword: function (compId) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/resetAssistantPassword', { method: 'POST', headers: headers, body: new URLSearchParams({ compId: compId }) })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
            if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '비밀번호 초기화 실패'));
            return r;
          });
        });
    },
    /** 가맹 챗봇 상단 로고 — 서버에서 목표 용량까지 자동 재압축 */
    compChatbotHeaderLogoUpload: function (compId, file) {
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('compId', String(compId || '').trim());
      fd.append('file', file);
      var headers = {};
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/chatbotHeaderLogo/upload', { method: 'POST', headers: headers, body: fd }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '업로드 실패'));
          return r.data || {};
        });
      });
    },
    /** 가맹 웹결제 상단 로고 — 서버에서 목표 용량까지 자동 재압축 */
    compWebPaymentHeaderLogoUpload: function (compId, file) {
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('compId', String(compId || '').trim());
      fd.append('file', file);
      var headers = {};
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/webPaymentHeaderLogo/upload', { method: 'POST', headers: headers, body: fd }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '업로드 실패'));
          return r.data || {};
        });
      });
    },
    /** 가맹 URL 분할결제 상단 로고 — 서버에서 목표 용량까지 자동 재압축 */
    compSplitPayHeaderLogoUpload: function (compId, file) {
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('compId', String(compId || '').trim());
      fd.append('file', file);
      var headers = {};
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/splitPayHeaderLogo/upload', { method: 'POST', headers: headers, body: fd }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '업로드 실패'));
          return r.data || {};
        });
      });
    },
    compChatbotKbGet: function (compId) {
      return get('/api/comp/chatbotKb', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패'));
        return r.data || {};
      });
    },
    compChatbotKbMerchantList: function (params) {
      return get('/api/comp/chatbotKb/merchantList', params || {}).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '목록 실패'));
        return r.data || { list: [], page: 1, size: 20, totalElements: 0, totalPages: 1 };
      });
    },
    compChatbotKbCommerceHold: function (compId, hold) {
      var body = new URLSearchParams({
        compId: String(compId || '').trim(),
        holdYn: hold === true || hold === 'Y' ? 'Y' : 'N'
      });
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/chatbotKb/commerceHold', { method: 'POST', headers: headers, body: body }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '저장 실패'));
          return r.data || {};
        });
      });
    },
    compChatbotKbSave: function (compId, fields) {
      var body = new URLSearchParams({ compId: String(compId || '').trim() });
      var f = fields || {};
      ['chatbotKbCompanyNm', 'chatbotKbAddr', 'chatbotKbTel', 'chatbotKbEmail', 'chatbotKbContactNm', 'chatbotKbWelcomeHint', 'chatbotKbIntro', 'chatbotKbProductDesc', 'chatbotOperationMode', 'chatbotMerchantVertical', 'chatbotMerchantVerticalNotes', 'chatbotOrderSheetUiJson', 'chatbotReservationSlotMinutes', 'chatbotReservationZoneId', 'chatbotCatalogListingEnabled'].forEach(function (k) {
        if (Object.prototype.hasOwnProperty.call(f, k) && f[k] != null) body.append(k, String(f[k]));
      });
      if (Object.prototype.hasOwnProperty.call(f, 'chatbotProductSlotLimit')) {
        body.append('chatbotProductSlotLimit', String(f.chatbotProductSlotLimit != null ? f.chatbotProductSlotLimit : ''));
      }
      if (Object.prototype.hasOwnProperty.call(f, 'chatbotProductSlotPlanUseSplit')) {
        body.append('chatbotProductSlotPlanUseSplit', String(f.chatbotProductSlotPlanUseSplit != null ? f.chatbotProductSlotPlanUseSplit : ''));
      }
      if (Object.prototype.hasOwnProperty.call(f, 'chatbotProductSlotLimitNext')) {
        body.append('chatbotProductSlotLimitNext', String(f.chatbotProductSlotLimitNext != null ? f.chatbotProductSlotLimitNext : ''));
      }
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/chatbotKb/save', { method: 'POST', headers: headers, body: body }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '저장 실패'));
          return r.data || {};
        });
      });
    },
    compChatbotKbSuggest: function (compId, kind) {
      var body = new URLSearchParams({ compId: String(compId || '').trim(), kind: String(kind || 'intro') });
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/chatbotKb/suggest', { method: 'POST', headers: headers, body: body }).then(function (res) {
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
        return res.text().then(function (text) {
          var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
          if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '생성 실패'));
          return (r.data && r.data.text != null) ? String(r.data.text) : '';
        });
      });
    },

    /** 챗봇관리 — 비용관리(히스토리): 챗봇 상품등록 플랜 월이용료 미수금 내역 */
    compChatbotKbBillingHistory: function (compId, params) {
      var p = params || {};
      var q = { compId: String(compId || '').trim() };
      if (p.page != null) q.page = p.page;
      if (p.size != null) q.size = p.size;
      return get('/api/comp/chatbotKb/billingHistory', q).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패'));
        return r.data || { list: [], page: 1, size: 12, totalElements: 0, totalPages: 1, meta: {} };
      });
    },

    compChangeLoginId: function (compId, newLoginId) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/changeLoginId', { method: 'POST', headers: headers, body: new URLSearchParams({ compId: compId, newLoginId: newLoginId }) })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
            if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '로그인ID 변경 실패'));
            return r;
          });
        });
    },
    settlementSetting: function (compId) {
      return get('/api/comp/settlementSetting', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패'));
        return r.data;
      });
    },
    settlementSettingSave: function (compId, data) {
      var body = new URLSearchParams({ compId: compId });
      for (var k in data) if (data[k] !== undefined && data[k] !== null) body.append(k, data[k]);
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/settlementSetting/save', { method: 'POST', headers: headers, body: body })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error'))); }
            if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '저장 실패'));
            return r;
          });
        });
    },

    bankCountries: function () {
      return get('/api/bank/countries').then(function (r) { return r.data || []; });
    },
    bankListByCountry: function (countryCd) {
      return get('/api/bank/list', { countryCd: countryCd || '' }).then(function (r) { return r.data || []; });
    },

    /** 공휴일 프리셋 (연도·국가): KR/US/JP/TH/CN은 JSON, GLOBAL은 해당 연도 토·일 전체 */
    holidayPresets: function (year, countries) {
      return get('/api/holiday/presets', {
        year: year != null ? year : new Date().getFullYear(),
        countries: countries != null ? countries : 'KR,US,JP,TH'
      }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패'));
        return r.data || {};
      });
    },

    commissionList: function (params) {
      return get('/api/commission/list', params).then(function (r) { return r.data; });
    },
    commissionDetail: function (compId) {
      return get('/api/commission/detail', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패'));
        return r.data;
      });
    },
    commissionHistory: function (compId, params) {
      var q = Object.assign({ compId: compId || '' }, params || {});
      return get('/api/commission/history', q).then(function (r) { return r.data; });
    },
    commissionSave: function (compId, data) {
      var cid = compId != null ? String(compId).trim() : '';
      if (!cid) return Promise.reject(new Error(apiT('업체코드가 없습니다.', 'Missing company code.')));
      var body = new URLSearchParams({ compId: cid });
      for (var k in data) {
        if (!Object.prototype.hasOwnProperty.call(data, k)) continue;
        var v = data[k];
        if (v === undefined || v === null) continue;
        if (typeof v === 'object') continue;
        body.append(k, v);
      }
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/commission/save', { method: 'POST', headers: headers, body: body })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Your session has expired.'))); }
          return res.text().then(function (text) {
            var r = {};
            try {
              r = text ? JSON.parse(text) : {};
            } catch (eParse) {
              if (!res.ok) {
                var flat = text ? String(text).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 160) : '';
                return Promise.reject(new Error(apiT('저장 실패', 'Save failed') + ' HTTP ' + res.status + (flat ? ' — ' + flat : '') + ' ' + apiT('(JSON이 아닌 응답)', '(non-JSON response)')));
              }
              return Promise.reject(new Error(apiT('서버 응답 오류', 'Server response error.')));
            }
            if (!res.ok) {
              var hint = (r && r.message) ? r.message : (text ? String(text).trim().slice(0, 200) : '');
              return Promise.reject(new Error(apiT('저장 실패', 'Save failed') + ' HTTP ' + res.status + (hint ? ': ' + hint : '')));
            }
            if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '저장 실패', 'Save failed.'));
            return r;
          });
        });
    },

    userList: function (params) {
      return get('/api/user/list', params).then(function (r) { return r.data; });
    },
    userCapability: function () {
      return get('/api/user/capability').then(function (r) { return r.data || {}; });
    },
    userAdd: function (body) {
      return post('/api/user/add', body || {}).then(function (r) { return r.data || r; });
    },
    userUpdate: function (body) {
      return post('/api/user/update', body || {}).then(function (r) { return r.data || r; });
    },
    userDelete: function (id) {
      return post('/api/user/delete', { id: id }).then(function (r) { return r.data || r; });
    },
    userResetPassword: function (id) {
      return post('/api/user/resetPassword', { id: id }).then(function (r) { return r.data || r; });
    },
    userResetOtp: function (id) {
      return post('/api/user/resetOtp', { id: id }).then(function (r) { return r.data || r; });
    },

    menuOrderMng: function (params) {
      return get('/api/user/menuOrderMng', params).then(function (r) { return r.data; });
    },
    userViewSetting: function (pageUrl) {
      return get('/api/user/viewSetting', { pageUrl: pageUrl || '' }).then(function (r) { return r.data; });
    },
    userViewSettingSave: function (pageUrl, selectedKeysJson) {
      return post('/api/user/viewSetting/save', {
        pageUrl: pageUrl || '',
        selectedKeysJson: selectedKeysJson || '[]'
      }).then(function (r) { return r.data; });
    },

    settlementDistributionList: function (params) {
      return get('/api/settlement/distributionList', params).then(function (r) { return r.data; });
    },
    settlementFranchiseList: function (params) {
      return get('/api/settlement/franchiseList', params).then(function (r) { return r.data; });
    },
    settlementRecallMng: function (params) {
      return get('/api/settlement/recallMng', params).then(function (r) { return r.data; });
    },
    settlementRecoveryList: function (params) {
      return get('/api/settlement/recoveryList', params).then(function (r) { return r.data; });
    },
    settlementReceivableList: function (params) {
      return get('/api/settlement/receivableList', params).then(function (r) { return r.data; });
    },
    settlementReceivableCreate: function (body) {
      return post('/api/settlement/receivable', body).then(function (r) { return r.data; });
    },
    settlementReceivableRecoveryRequest: function (id) {
      return post('/api/settlement/receivable/' + encodeURIComponent(id) + '/recoveryRequest', {}).then(function (r) { return r.data; });
    },
    settlementBalanceMng: function (params) {
      return get('/api/settlement/balanceMng', params).then(function (r) { return r.data; });
    },
    settlementBalanceDeduct: function (body) {
      return post('/api/settlement/balance/deduct', body).then(function (r) { return r.data; });
    },
    settlementBalanceList: function (params) {
      return get('/api/settlement/balanceList', params).then(function (r) { return r.data; });
    },
    settlementUnpaidMng: function (params) {
      return get('/api/settlement/unpaidMng', params).then(function (r) { return r.data; });
    },
    settlementPayoutHoldList: function (params) {
      return get('/api/settlement/payoutHoldList', params).then(function (r) { return r.data; });
    },
    settlementPayoutHoldRelease: function (body) {
      return post('/api/settlement/payoutHold/release', body || {}).then(function (r) { return r.data; });
    },
    settlementCollateralList: function (params) {
      return get('/api/settlement/collateralList', params).then(function (r) { return r.data; });
    },
    settlementFeeList: function (params) {
      return get('/api/settlement/feeList', params).then(function (r) { return r.data; });
    },
    dailyFeeSummary: function (params) {
      return get('/api/settlement/dailyFeeSummary', params).then(function (r) { return r.data; });
    },
    settlementExecute: function (params) {
      return get('/api/settlement/execute', params).then(function (r) { return r.data; });
    },
    settlementExecuteRunTransactions: function (params) {
      return get('/api/settlement/execute/runTransactions', params || {}).then(function (r) { return r.data; });
    },
    settlementResultList: function (params) {
      return get('/api/settlement/result/list', params).then(function (r) { return r.data; });
    },
    settlementResultDistribute: function (body) {
      return post('/api/settlement/result/distribute', body || {}).then(function (r) { return r.data; });
    },
    settlementResultHold: function (body) {
      return post('/api/settlement/result/hold', body || {}).then(function (r) { return r.data; });
    },
    settlementReportAggregate: function (params) {
      return get('/api/settlement/report/aggregate', params).then(function (r) { return r.data; });
    },
    settlementReportExecute: function (params) {
      return get('/api/settlement/report/execute', params).then(function (r) { return r.data; });
    },
    settlementReportSummary: function (params) {
      return get('/api/settlement/report/summary', params).then(function (r) { return r.data; });
    },
    settlementReportAccess: function () {
      return get('/api/settlement/report/access', {}).then(function (r) { return r.data; });
    },
    settlementReportConfirmedRuns: function (params) {
      return get('/api/settlement/report/confirmedRuns', params).then(function (r) { return r.data; });
    },
    settlementReportConfirmedRunDetail: function (params) {
      return get('/api/settlement/report/confirmedRunDetail', params).then(function (r) { return r.data; });
    },
    settlementExecuteRun: function (params) {
      var q = (params && typeof params === 'object') ? params : {};
      var path = '/api/settlement/execute/run';
      var arr = [];
      if (q.fromDate) arr.push('fromDate=' + encodeURIComponent(q.fromDate));
      if (q.toDate) arr.push('toDate=' + encodeURIComponent(q.toDate));
      if (q.merchantId) arr.push('merchantId=' + encodeURIComponent(q.merchantId));
      if (q.reconcile !== false) arr.push('reconcile=true');
      if (arr.length) path += (path.indexOf('?') >= 0 ? '&' : '?') + arr.join('&');
      return request({ path: path, method: 'POST' }).then(function (r) { return r.data; });
    },

    notifyPayUrlMng: function (params) {
      return get('/api/notify/payUrlMng', params).then(function (r) { return r.data; });
    },
    notifyPaySendMng: function (params) {
      return get('/api/notify/paySendMng', params).then(function (r) { return r.data; });
    },
    notifyCashReceiptUrlMng: function (params) {
      return get('/api/notify/cashReceiptUrlMng', params).then(function (r) { return r.data; });
    },
    notifyCashReceiptSendMng: function (params) {
      return get('/api/notify/cashReceiptSendMng', params).then(function (r) { return r.data; });
    },

    hqPgApiMng: function (params) {
      return get('/api/hq/pgApiMng', params).then(function (r) { return r.data; });
    },
    hqPgApiMngSave: function (body) {
      return post('/api/hq/pgApiMng/save', body).then(function (r) { return r.data; });
    },
    hqPgApiMngDelete: function (body) {
      return post('/api/hq/pgApiMng/delete', body).then(function (r) { return r.data; });
    },
    hqPgApiMngOperationalSave: function (body) {
      return post('/api/hq/pgApiMng/operational', body).then(function (r) { return r.data; });
    },
    hqMerchantApiDeploymentVendors: function () {
      return get('/api/hq/merchant-api-deployment/vendors').then(function (r) { return r.data; });
    },
    hqMerchantApiDeploymentMerchants: function (params) {
      return get('/api/hq/merchant-api-deployment/merchants', params || {}).then(function (r) { return r.data; });
    },
    hqMerchantApiDeploymentKit: function (params) {
      return get('/api/hq/merchant-api-deployment/kit', params || {}).then(function (r) { return r.data; });
    },
    hqMerchantApiDeploymentDocsPortal: function (params) {
      return get('/api/hq/merchant-api-deployment/docs-portal', params || {}).then(function (r) { return r.data; });
    },
    merchantApiPortalSelf: function () {
      return get('/api/merchant/api-portal/self').then(function (r) { return r.data; });
    },
    fetchSampleDoc: fetchSampleDoc,
    openSampleDoc: function (path, mime) {
      return fetchSampleDoc(path).then(function (text) {
        var m = mime || 'text/html;charset=UTF-8';
        var body = text;
        if (/text\/html/i.test(m)) body = injectMerchantSampleDocHtml(body, path);
        var blob = new Blob([body], { type: m });
        var u = URL.createObjectURL(blob);
        /* noopener 제거 — blob 탭에서 언어 전환 시 opener.PG_API 로 재오픈 */
        var w = window.open(u, '_blank');
        if (!w) {
          URL.revokeObjectURL(u);
          return Promise.reject(new Error(apiT('팝업이 차단되었습니다. 팝업 허용 후 다시 시도하세요.', 'Popup blocked. Allow popups and try again.')));
        }
        setTimeout(function () { URL.revokeObjectURL(u); }, 120000);
      });
    },
    hqMerchantApiDeploymentRotate: function (body) {
      return post('/api/hq/merchant-api-deployment/credential/rotate', body || {}).then(function (r) { return r.data; });
    },
    hqMerchantApiDeploymentEnforce: function (body) {
      return post('/api/hq/merchant-api-deployment/credential/enforce', body || {}).then(function (r) { return r.data; });
    },
    hqDefaultCommission: function () {
      return get('/api/hq/defaultCommission').then(function (r) { return r.data; });
    },
    hqDefaultCommissionSave: function (body) {
      return post('/api/hq/defaultCommission/save', body).then(function (r) { return r.data; });
    },
    hqDefaultCommissionTemplateAdd: function (body) {
      return post('/api/hq/defaultCommission/template/add', body || {}).then(function (r) { return r.data; });
    },
    hqDefaultCommissionTemplateDelete: function (scope) {
      return post('/api/hq/defaultCommission/template/delete', { scope: scope || '' }).then(function (r) { return r.data; });
    },
    hqRiskCardPolicy: function () {
      return get('/api/hq/riskCardPolicy').then(function (r) { return r.data; });
    },
    hqRiskCardPolicySave: function (body) {
      return post('/api/hq/riskCardPolicy/save', body || {}).then(function (r) { return r.data; });
    },
    hqChargebackPolicyList: function () {
      return get('/api/hq/chargebackPolicy/list').then(function (r) { return r.data || []; });
    },
    hqChargebackPolicyDetail: function (id) {
      return get('/api/hq/chargebackPolicy/' + encodeURIComponent(id)).then(function (r) { return r.data; });
    },
    hqChargebackPolicySave: function (body) {
      return post('/api/hq/chargebackPolicy/save', body || {}).then(function (r) { return r.data; });
    },
    hqChargebackPolicyDelete: function (id) {
      return post('/api/hq/chargebackPolicy/delete', { id: id }).then(function (r) { return r.data; });
    },
    hqPgAgencyCostPolicy: function () {
      return get('/api/hq/pgAgencyCostPolicy').then(function (r) { return r.data; });
    },
    hqPgAgencyCostPolicyDetail: function (pgCd) {
      return get('/api/hq/pgAgencyCostPolicy/' + encodeURIComponent(pgCd || '')).then(function (r) { return r.data; });
    },
    hqPgAgencyCostPolicySave: function (body) {
      return post('/api/hq/pgAgencyCostPolicy/save', body || {}).then(function (r) { return r.data; });
    },
    hqApiConfig: function () {
      return get('/api/hq/apiConfig').then(function (r) { return r.data; });
    },
    hqApiConfigSave: function (body) {
      return post('/api/hq/apiConfig/save', body).then(function (r) { return r.data; });
    },
    hqJpayPortalAccounts: function () {
      return get('/api/hq/jpayPortalAccount').then(function (r) { return r.data; });
    },
    hqJpayPortalAccountSave: function (body) {
      return post('/api/hq/jpayPortalAccount', body || {}).then(function (r) { return r.data; });
    },
    hqJpayPortalAccountDelete: function (body) {
      return post('/api/hq/jpayPortalAccount/delete', body || {}).then(function (r) { return r.data; });
    },
    hqJpaySubscriptions: function (compId) {
      var q = compId ? ('?compId=' + encodeURIComponent(compId)) : '';
      return get('/api/hq/jpaySubscriptions' + q).then(function (r) { return r.data; });
    },
    hqPayCopyTranslateFromKo: function (body) {
      return post('/api/hq/payCopyTranslateFromKo', body || {}).then(function (r) {
        return r && r.data != null ? r.data : {};
      });
    },
    /** URL 결제 폼 설정 — 브라우저 탭 제목(한국어) 다국어 초안 */
    hqUrlPayTabTitleTranslateFromKo: function (body) {
      return post('/api/hq/urlPayTabTitleTranslateFromKo', body || {}).then(function (r) {
        return r && r.data != null ? r.data : {};
      });
    },
    /** 본사 URL 결제 폼 결제구문설정용 파비콘 — PNG·JPG, 서버에서 32×32 PNG 저장 */
    hqUrlPayFaviconUpload: function (file) {
      if (!file || typeof file.size !== 'number') {
        return Promise.reject(new Error('업로드할 파일을 선택하세요.'));
      }
      var max = 1 * 1024 * 1024;
      if (file.size > max) {
        return Promise.reject(new Error('파비콘 이미지는 1MB 이하만 업로드할 수 있습니다.'));
      }
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      var uploadOnce = function (uploadFile) {
        var fd = new FormData();
        fd.append('file', uploadFile);
        return fetchTextThenJson(base + '/api/hq/url-pay/favicon-upload', {
          method: 'POST',
          headers: headers,
          body: fd
        }, 'URL 결제 파비콘 업로드 응답이 JSON이 아닙니다.');
      };
      return uploadOnce(file).catch(function (err) {
        var msg = err && err.message ? String(err.message) : '';
        if (msg.indexOf('HTTP 413') === -1) return Promise.reject(err);
        return compressImageForUpload(file, 900 * 1024).then(function (compressed) {
          return uploadOnce(compressed);
        }).catch(function (e2) {
          return Promise.reject(e2 || err);
        });
      }).then(function (r) {
        if (r && r.success === false) throw new Error(serverMsgT(r.message, '파비콘 업로드 실패'));
        return r.data || r;
      });
    },
    hqDomainConfig: function () {
      return get('/api/hq/domainConfig').then(function (r) { return r.data; });
    },
    hqDomainConfigSave: function (body) {
      return post('/api/hq/domainConfig/save', body || {}).then(function (r) { return r.data; });
    },
    hqDomainConfigOrgSave: function (body) {
      return post('/api/hq/domainConfig/orgSave', body || {}).then(function (r) { return r.data; });
    },
    hqDomainConfigOrgDelete: function (body) {
      return post('/api/hq/domainConfig/orgDelete', body || {}).then(function (r) { return r.data; });
    },
    hqServerManage: function () {
      return get('/api/hq/serverManage').then(function (r) { return r.data; });
    },
    hqServerManageSave: function (body) {
      return post('/api/hq/serverManage/save', body || {}).then(function (r) { return r.data; });
    },
    /** grain: daily | weekly | monthly — NOTI 유사 트래픽·메모리 피크 시계열 */
    hqServerUsage: function (grain) {
      return get('/api/hq/serverUsage', { grain: grain || 'daily' }).then(function (r) { return r.data; });
    },
    hqBusinessDaySettings: function () {
      return get('/api/hq/businessDaySettings').then(function (r) { return r.data || []; });
    },
    hqBusinessDaySettingsSave: function (body) {
      return post('/api/hq/businessDaySettings/save', body || {}).then(function (r) { return r.data || r; });
    },
    hqBusinessDaySetHqDefault: function (id) {
      return post('/api/hq/businessDaySettings/setHqDefault', { id: id || '' }).then(function (r) { return r.data || r; });
    },

    hqSettlementCycleOptions: function () {
      return get('/api/hq/settlement/cycleOptions').then(function (r) { return r.data || []; });
    },
    /** 병합 표준 전체(미사용 N 포함) — 총판별 허용 주기 설정 슬롯용 */
    hqSettlementCycleOptionsCatalog: function () {
      return get('/api/hq/settlement/cycleOptionsCatalog').then(function (r) { return r.data || []; });
    },
    /** 가맹 상위(parent) 조직 기준 정산주기 셀렉트용: options, defaultCalcCycle, scoped */
    hqSettlementCycleOptionsScoped: function (params) {
      return get('/api/hq/settlement/cycleOptionsScoped', params || {}).then(function (r) { return r.data || {}; });
    },
    hqMasterDistOrgOptions: function () {
      return get('/api/hq/settlement/masterDistOrgOptions').then(function (r) { return r.data || []; });
    },
    /** 총판별 영업일 표시 + 정산 크론 Zone + 거래시간(1줄) 프리셋 — { rows, presets, txnTimePresets } */
    hqMasterDistBizCronZoneGet: function () {
      return get('/api/hq/settlement/masterDistBizCronZone', {}).then(function (r) { return r.data || {}; });
    },
    hqMasterDistSettlementCronZoneSave: function (body) {
      return post('/api/hq/settlement/masterDistSettlementCronZone', body || {}).then(function (r) { return r.data != null ? r.data : r; });
    },
    hqMasterDistCalcCycleConfigGet: function (orgUnitId) {
      return get('/api/hq/settlement/masterDistCalcCycleConfig', { orgUnitId: orgUnitId }).then(function (r) { return r.data || {}; });
    },
    hqMasterDistCalcCycleConfigSave: function (body) {
      return post('/api/hq/settlement/masterDistCalcCycleConfig', body || {}).then(function (r) { return r.data != null ? r.data : r; });
    },
    hqSettlementCycleDefs: function () {
      return get('/api/hq/settlement/cycleDefs').then(function (r) { return r.data || []; });
    },
    hqSettlementSchedulePreview: function (params) {
      return get('/api/hq/settlement/schedulePreview', params || {}).then(function (r) { return r.data || []; });
    },
    hqSettlementCalcCycleChangeHistory: function (params) {
      return get('/api/hq/settlement/calcCycleChangeHistory', params || {}).then(function (r) { return r.data || []; });
    },
    hqSettlementMerchantAutoCounts: function () {
      return get('/api/hq/settlement/merchantAutoCounts').then(function (r) { return r.data || {}; });
    },
    hqSettlementAutoBatchGet: function () {
      return get('/api/hq/settlement/autoBatch').then(function (r) { return r.data || {}; });
    },
    hqSettlementAutoBatchSave: function (body) {
      return post('/api/hq/settlement/autoBatch', body || {}).then(function (r) { return r.data != null ? r.data : r; });
    },
    hqVoidRefundSettlementModesGet: function () {
      return get('/api/hq/settlement/voidRefundSettlementModes').then(function (r) { return r.data != null ? r.data : r; });
    },
    hqVoidRefundSettlementModesSave: function (body) {
      return post('/api/hq/settlement/voidRefundSettlementModes', body || {}).then(function (r) { return r.data != null ? r.data : r; });
    },
    hqSettlementCycleDefCreate: function (body) {
      return post('/api/hq/settlement/cycleDefs', body || {}).then(function (r) { return r.data || r; });
    },
    hqSettlementCycleDefUpdate: function (id, body) {
      return request({ path: '/api/hq/settlement/cycleDefs/' + encodeURIComponent(id), method: 'PUT', body: body || {} })
        .then(function (r) { return r && r.data != null ? r.data : r; });
    },
    hqSettlementCycleDefDelete: function (id) {
      return del('/api/hq/settlement/cycleDefs/' + encodeURIComponent(id)).then(function (r) { return r.data || r; });
    },
    /** 표준 주기(내장 목록) 중 DB에 없는 코드만 INSERT */
    hqSettlementCycleDefsSeedMissing: function () {
      return post('/api/hq/settlement/cycleDefs/seedMissing', {}).then(function (r) { return r.data != null ? r.data : r; });
    },
    hqReceivableRecoverySettingsGet: function () {
      return get('/api/hq/settlement/receivableRecoverySettings').then(function (r) { return r.data; });
    },
    hqReceivableRecoverySettingsSave: function (body) {
      return post('/api/hq/settlement/receivableRecoverySettings', body || {}).then(function (r) { return r.data != null ? r.data : r; });
    },

    hqNotifyEnv: function () {
      return get('/api/hq/notifyEnv').then(function (r) { return r.data; });
    },
    hqNotifyEnvSave: function (body) {
      return post('/api/hq/notifyEnv/save', body || {}).then(function (r) { return r.data; });
    },
    hqNotifyEnvRegenerateToken: function () {
      return post('/api/hq/notifyEnv/regenerateToken', {}).then(function (r) { return r.data; });
    },
    hqNotifyTargets: function () {
      return get('/api/hq/notifyEnv/targets').then(function (r) { return r.data || []; });
    },
    hqNotifyMasterDistOptions: function () {
      return get('/api/hq/notifyEnv/targets/masterDistOptions').then(function (r) { return r.data || []; });
    },
    hqNotifyTargetCreate: function (targetName, boundOrgUnitId) {
      var body = { targetName: targetName || '' };
      if (boundOrgUnitId != null && String(boundOrgUnitId).trim() !== '') {
        body.boundOrgUnitId = String(boundOrgUnitId).trim();
      }
      return post('/api/hq/notifyEnv/targets/create', body).then(function (r) { return r.data || r; });
    },
    hqNotifyTargetsBindBoundOrg: function (targetIds, boundOrgUnitId) {
      return post('/api/hq/notifyEnv/targets/bindBoundOrg', {
        targetIds: targetIds,
        boundOrgUnitId: boundOrgUnitId != null ? String(boundOrgUnitId).trim() : ''
      }).then(function (r) { return r.data || r; });
    },
    hqNotifyTargetDelete: function (id) {
      return del('/api/hq/notifyEnv/targets/' + encodeURIComponent(id)).then(function (r) { return r.data || r; });
    },
    hqNotifyMapping: function () {
      return get('/api/hq/notifyMapping').then(function (r) { return r.data; });
    },
    hqNotifyMappingDefaults: function () {
      return get('/api/hq/notifyMapping/defaults').then(function (r) { return r.data; });
    },
    hqNotifyMappingSave: function (body) {
      return post('/api/hq/notifyMapping/save', body || {}).then(function (r) { return r.data; });
    },
    hqNotifyMappingSuggest: function (body) {
      return post('/api/hq/notifyMapping/suggest', body || {}).then(function (r) { return r.data; });
    },
    hqNotifyMappingAiStatus: function () {
      return get('/api/hq/notifyMapping/aiStatus').then(function (r) { return r.data; });
    },
    hqNotifyMappingInboundKeys: function (vendorCode, limit) {
      return get('/api/hq/notifyMapping/inboundParamKeys', { vendorCode: vendorCode || '', limit: limit != null ? limit : 120 }).then(function (r) { return r.data; });
    },
    hqNotifyInboundList: function (params) {
      return get('/api/hq/notifyInbound', params || {}).then(function (r) { return r.data; });
    },
    hqNotifyInboundDetail: function (id) {
      return get('/api/hq/notifyInbound/' + encodeURIComponent(id)).then(function (r) { return r.data; });
    },
    hqNotifyInboundReplay: function (id, opts) {
      opts = opts || {};
      var q = '';
      if (opts.icopayCompId) {
        q = '?icopayCompId=' + encodeURIComponent(opts.icopayCompId);
      }
      var payload = {};
      if (opts.rawBody != null && String(opts.rawBody).length) {
        payload.rawBody = opts.rawBody;
      }
      if (opts.customerNm != null && String(opts.customerNm).trim() !== '') {
        payload.customerNm = String(opts.customerNm).trim();
      }
      if (opts.customerEmail != null && String(opts.customerEmail).trim() !== '') {
        payload.customerEmail = String(opts.customerEmail).trim();
      }
      if (opts.cardPanDisplay != null && String(opts.cardPanDisplay).trim() !== '') {
        payload.cardPanDisplay = String(opts.cardPanDisplay).trim();
      }
      if (opts.icopayCompId && !q) {
        payload.icopayCompId = opts.icopayCompId;
      }
      return post('/api/hq/notifyInbound/' + encodeURIComponent(id) + '/replay' + q, payload).then(function (r) { return r.data; });
    },
    hqNotifyInboundUpdateRawBody: function (id, rawBody) {
      return put('/api/hq/notifyInbound/' + encodeURIComponent(id) + '/rawBody', { rawBody: rawBody }).then(function (r) { return r.data; });
    },
    hqNotifyInboundReplayOrders: function (payload) {
      return post('/api/hq/notifyInbound/replay-orders', payload || {}).then(function (r) { return r.data; });
    },
    hqLedgerSysSettings: function () {
      return get('/api/hq/ledgerSysSettings').then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsSave: function (body) {
      return post('/api/hq/ledgerSysSettings/save', body || {}).then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsSaveHelloTimeline: function (body) {
      return post('/api/hq/ledgerSysSettings/saveHelloTimeline', body || {}).then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsSavePayFollowLevelCaps: function (body) {
      return post('/api/hq/ledgerSysSettings/savePayFollowLevelCaps', body || {}).then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsResetOperationalData: function () {
      return post('/api/hq/ledgerSysSettings/resetOperationalData', {}).then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsResetSettlementData: function (body) {
      return post('/api/hq/ledgerSysSettings/resetSettlementData', body || {}).then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsPurgePayNotifyForDay: function (body) {
      return post('/api/hq/ledgerSysSettings/purgePayAndNotifyForDay', body || {}).then(function (r) { return r.data; });
    },
    hqPayCardBlockPrefixAdd: function (body) {
      return post('/api/hq/ledgerSysSettings/payCardBlockPrefix', body).then(function (r) { return r.data; });
    },
    hqPayCardBlockPrefixDelete: function (body) {
      return post('/api/hq/ledgerSysSettings/payCardBlockPrefix/delete', body).then(function (r) { return r.data; });
    },
    opsInactiveCardAccess: function () {
      return get('/api/ops/inactiveCard/access').then(function (r) { return r.data; });
    },
    opsInactiveCardList: function (params) {
      return get('/api/ops/inactiveCard/list', params).then(function (r) { return r.data; });
    },
    opsInactiveCardRegister: function (body) {
      return post('/api/ops/inactiveCard/register', body).then(function (r) { return r.data; });
    },
    opsInactiveCardRelease: function (body) {
      return post('/api/ops/inactiveCard/release', body).then(function (r) { return r.data; });
    },
    opsInactiveCardUpdate: function (body) {
      return post('/api/ops/inactiveCard/update', body).then(function (r) { return r.data; });
    },
    hqLedgerSysSettingsTestVoidEmail: function (body) {
      return post('/api/hq/ledgerSysSettings/testVoidEmail', body || {}).then(function (r) { return r.data; });
    },
    hqChatbotAiSettings: function () {
      return get('/api/hq/chatbotAiSettings').then(function (r) { return r.data; });
    },
    hqChatbotAiSettingsSave: function (body) {
      return post('/api/hq/chatbotAiSettings/save', body || {}).then(function (r) { return r.data; });
    },
    chatbotOrdersList: function (compId) {
      return get('/api/chatbot/orders', { compId: String(compId || '').trim() }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '목록 실패'));
        return Array.isArray(r.data) ? r.data : [];
      });
    },
    chatbotProductsList: function (compId) {
      var params = {};
      if (compId != null && String(compId).trim() !== '') {
        params.compId = String(compId).trim();
      }
      return get('/api/chatbot/products', params).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '조회 실패', 'Lookup failed'));
        var raw = r.data;
        return Array.isArray(raw) ? raw : [];
      });
    },
    chatbotProductsCurrencyMeta: function (compId) {
      return get('/api/chatbot/products/currency-meta', { compId: String(compId || '').trim() }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '통화 정보 조회 실패', 'Currency lookup failed'));
        var raw = r.data || {};
        var fallback = ['JPY', 'KRW', 'USD', 'CNY', 'THB'];
        var maxImg = raw.effectiveMaxProductImages != null ? parseInt(String(raw.effectiveMaxProductImages), 10) : 1;
        if (isNaN(maxImg) || maxImg < 1) maxImg = 1;
        if (maxImg > 4) maxImg = 4;
        var allowedLt = Array.isArray(raw.allowedListingTypes) && raw.allowedListingTypes.length
          ? raw.allowedListingTypes.map(function (x) { return String(x).trim().toUpperCase(); })
          : ['SALE', 'RESERVATION_TIME', 'RESERVATION_PLACE'];
        return {
          defaultCurrency: raw.defaultCurrency ? String(raw.defaultCurrency).trim().toUpperCase() : 'KRW',
          allowedCurrencies: Array.isArray(raw.allowedCurrencies) && raw.allowedCurrencies.length ? raw.allowedCurrencies.map(function (x) { return String(x).trim().toUpperCase(); }) : fallback,
          effectiveMaxProductImages: maxImg,
          allowedListingTypes: allowedLt,
          promotionShelfMode: raw.promotionShelfMode != null ? String(raw.promotionShelfMode).trim().toUpperCase() : 'PROMOTION',
          promotionRotateSeconds: (function () {
            var v = raw.promotionRotateSeconds != null ? parseInt(String(raw.promotionRotateSeconds), 10) : 30;
            return isNaN(v) ? 30 : v;
          })()
        };
      });
    },
    chatbotProductsPromotionShelfSave: function (body) {
      return post('/api/chatbot/products/promotion-shelf-settings', body || {}).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '저장 실패', 'Save failed'));
        return r.data || {};
      });
    },
    chatbotProductsSave: function (body) {
      return post('/api/chatbot/products/save', body || {}).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '저장 실패', 'Save failed'));
        return r.data;
      });
    },
    chatbotProductsDelete: function (compId, id) {
      return del('/api/chatbot/products/' + encodeURIComponent(String(id)) + '?compId=' + encodeURIComponent(String(compId || '').trim())).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(serverMsgT(r.message, '삭제 실패', 'Delete failed'));
        return !!(r.data === true || r.data === '' || r.data == null || r.success);
      });
    },
    chatbotProductsUpload: function (compId, productId, file, imageSlot) {
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('compId', String(compId || '').trim());
      if (productId != null && String(productId).trim() !== '') fd.append('productId', String(productId).trim());
      if (imageSlot != null && String(imageSlot).trim() !== '') fd.append('imageSlot', String(imageSlot).trim());
      fd.append('file', file);
      var headers = {};
      if (token) headers['Authorization'] = 'Bearer ' + token;
      var url = base + '/api/chatbot/products/upload';
      return fetch(url, { method: 'POST', headers: headers, body: fd, credentials: 'omit', mode: 'cors' }).then(function (res) {
        return res.text().then(function (text) {
          if (res.status === 401) {
            clearAuth();
            if (typeof window.location !== 'undefined') window.location.replace((window.location.origin || '') + '/login.html');
            return Promise.reject(new Error(apiT('인증이 만료되었습니다.', 'Session expired.')));
          }
          if (res.status === 413) {
            return Promise.reject(new Error(apiT(
              '업로드 파일 용량이 서버 제한을 초과했습니다. (HTTP 413)',
              'Upload exceeds the server limit (HTTP 413).'
            )));
          }
          var r;
          try {
            r = text ? JSON.parse(text) : {};
          } catch (eJ) {
            var flat = text ? String(text).replace(/<script[^>]*>[\s\S]*?<\/script>/gi, ' ').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 200) : '';
            return Promise.reject(new Error(apiT('서버 응답이 JSON이 아닙니다.', 'Invalid JSON response.') + ' HTTP ' + res.status + (flat ? (' — ' + flat) : '')));
          }
          if (!res.ok) {
            var hint = (r && r.message) ? r.message : (text ? String(text).slice(0, 120) : '');
            return Promise.reject(new Error(apiT('API 오류', 'API error') + ' HTTP ' + res.status + (hint ? ': ' + hint : '')));
          }
          if (!r.success) throw new Error(serverMsgT(r.message, '업로드 실패', 'Upload failed'));
          return r.data || {};
        });
      }).catch(function (err) {
        var msg = (err && err.message) ? err.message : '';
        if (msg === 'Failed to fetch' || msg.indexOf('NetworkError') !== -1 || msg.indexOf('Load failed') !== -1 || msg === 'Network request failed') {
          return Promise.reject(new Error(apiT('API에 연결하지 못했습니다.', 'Unable to connect to API.') + ' (' + url + ')'));
        }
        return Promise.reject(err);
      });
    },
    opsMailLogList: function (params) {
      var p = params || {};
      var q = { page: p.page || 1, size: p.size || 20 };
      if (p.searchMailKind) q.mailKind = p.searchMailKind;
      if (p.searchMailStatus) q.status = p.searchMailStatus;
      if (p.searchFromDate) q.fromDate = p.searchFromDate;
      if (p.searchToDate) q.toDate = p.searchToDate;
      return get('/api/ops/mailLog', q).then(function (r) { return r.data; });
    },
    opsTaxReportAccess: function () {
      return get('/api/ops/taxReport/access').then(function (r) { return r.data; });
    },
    opsTaxReportList: function (params) {
      var p = params || {};
      var q = { page: p.page || 1, size: p.size || 50 };
      if (p.searchFromDate) q.searchFromDate = p.searchFromDate;
      if (p.searchToDate) q.searchToDate = p.searchToDate;
      if (p.searchCompId) q.searchCompId = p.searchCompId;
      if (p.searchOrderDir) q.searchOrderDir = p.searchOrderDir;
      return get('/api/ops/taxReport/list', q).then(function (r) { return r.data; });
    },
    opsTaxReportExport: function (payload) {
      return this.fetchBinary('/api/ops/taxReport/export', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        },
        body: JSON.stringify(payload || {})
      });
    },
    opsIntegratedReportAccess: function () {
      return get('/api/ops/integratedReport/access').then(function (r) { return r.data; });
    },
    unwrapListMetaApiPayload: unwrapListMetaApiPayload,
    opsIntegratedReportDaily: function (params) {
      var p = params || {};
      var q = {};
      for (var k in p) {
        if (!Object.prototype.hasOwnProperty.call(p, k)) continue;
        if (k === 'page' || k === 'size') continue;
        var v = p[k];
        if (v === undefined || v === null) continue;
        if (String(v).trim() === '') continue;
        q[k] = v;
      }
      return get('/api/ops/integratedReport/daily', q).then(function (r) {
        if (!r || r.success === false) {
          var failMsg = (r && r.message) ? String(r.message).trim() : '';
          return Promise.reject(new Error(failMsg || apiT('통합 리포트 조회에 실패했습니다.', 'Integrated report request failed.')));
        }
        if (r && r.error && r.status != null && !unwrapListMetaApiPayload(r)) {
          var st = Number(r.status);
          var pathHint = r.path ? String(r.path) : '/api/ops/integratedReport/daily';
          if (st === 404) {
            return Promise.reject(new Error(apiT(
              '통합 리포트 API(' + pathHint + ')를 찾을 수 없습니다. 최신 pg-app JAR 배포 후 서버를 재시작하세요.',
              'Integrated report API not found (' + pathHint + '). Deploy the latest pg-app JAR and restart.'
            )));
          }
        }
        var payload = unwrapListMetaApiPayload(r);
        if (!payload) {
          return Promise.reject(new Error(apiT(
            '통합 리포트 응답을 해석할 수 없습니다. 브라우저 개발자도구 네트워크에서 /api/ops/integratedReport/daily 응답(JSON)과 API 서버(pg-app) 배포를 확인하세요.',
            'Unable to parse integrated report response. Check the /api/ops/integratedReport/daily JSON in the network tab and pg-app deployment.'
          )));
        }
        return payload;
      });
    },
    opsAgencyTxnListAccess: function () {
      return get('/api/ops/agencyTxnList/access').then(function (r) { return r.data; });
    },
    opsAgencyTxnList: function (params) {
      var p = params || {};
      var q = { page: p.page || 1, size: p.size || 50 };
      ['searchFromDate', 'searchToDate', 'searchCompId', 'searchCompNm', 'searchFieldType',
        'searchKeyword', 'searchStatusGroup', 'searchOrderDir'].forEach(function (k) {
        if (p[k] != null && String(p[k]).trim() !== '') q[k] = p[k];
      });
      return get('/api/ops/agencyTxnList', q).then(function (r) { return r.data; });
    },
    opsVerifyReportAccess: function () {
      return get('/api/ops/verifyReport/access').then(function (r) { return r.data; });
    },
    opsVerifyReportDaily: function (params) {
      var p = params || {};
      var q = {};
      for (var k in p) {
        if (!Object.prototype.hasOwnProperty.call(p, k)) continue;
        if (k === 'page' || k === 'size') continue;
        var v = p[k];
        if (v === undefined || v === null) continue;
        if (String(v).trim() === '') continue;
        q[k] = v;
      }
      return get('/api/ops/verifyReport/daily', q).then(function (r) {
        if (!r || r.success === false) {
          var failMsg = (r && r.message) ? String(r.message).trim() : '';
          return Promise.reject(new Error(failMsg || apiT('검증 리포트 조회에 실패했습니다.', 'Verify report request failed.')));
        }
        var payload = unwrapListMetaApiPayload(r);
        if (!payload) {
          return Promise.reject(new Error(apiT('검증 리포트 응답을 해석할 수 없습니다.', 'Unable to parse verify report response.')));
        }
        return payload;
      });
    },
    opsVerifyReportSyncStatus: function (body) {
      return post('/api/ops/verifyReport/syncStatus', body || {}).then(function (r) {
        if (!r || r.success === false) {
          var failMsg = (r && r.message) ? String(r.message).trim() : '';
          return Promise.reject(new Error(failMsg || apiT('상태 맞춤에 실패했습니다.', 'Status sync failed.')));
        }
        return r.data;
      });
    },
    opsVerifyReportSyncStatusBatch: function (body) {
      return post('/api/ops/verifyReport/syncStatusBatch', body || {}).then(function (r) {
        if (!r || r.success === false) {
          var failMsg = (r && r.message) ? String(r.message).trim() : '';
          return Promise.reject(new Error(failMsg || apiT('상태 일괄 맞춤에 실패했습니다.', 'Batch status sync failed.')));
        }
        return r.data;
      });
    },
    hqOrgViewColumnRegionalBranches: function () {
      return get('/api/hq/orgViewColumnAllowance/regionalBranches').then(function (r) { return r.data || []; });
    },
    hqOrgViewColumnAllowanceList: function (regionalOrgCode) {
      return get('/api/hq/orgViewColumnAllowance/list', { regionalOrgCode: regionalOrgCode || '' }).then(function (r) { return r.data || []; });
    },
    hqOrgViewColumnAllowanceGet: function (regionalOrgCode, pageUrl, viewerScope) {
      var params = {
        regionalOrgCode: regionalOrgCode || '',
        pageUrl: pageUrl || ''
      };
      if (viewerScope) params.viewerScope = viewerScope;
      return get('/api/hq/orgViewColumnAllowance', params).then(function (r) { return r.data; });
    },
    hqOrgViewColumnAllowanceSave: function (body) {
      return post('/api/hq/orgViewColumnAllowance/save', body || {}).then(function (r) { return r.data; });
    },
    hqOrgViewColumnAllowanceDelete: function (body) {
      return post('/api/hq/orgViewColumnAllowance/delete', body || {}).then(function (r) { return r.data; });
    },
    hqOrgViewCustomColumns: function (pageUrl) {
      return get('/api/hq/orgViewColumnAllowance/customColumns', { pageUrl: pageUrl || '' }).then(function (r) { return r.data || []; });
    },
    hqOrgViewCustomColumnAdd: function (body) {
      return post('/api/hq/orgViewColumnAllowance/customColumns/add', body || {}).then(function (r) { return r.data; });
    },
    hqOrgViewCustomColumnUpdate: function (body) {
      return post('/api/hq/orgViewColumnAllowance/customColumns/update', body || {}).then(function (r) { return r.data; });
    },
    hqOrgViewCustomColumnDelete: function (body) {
      return post('/api/hq/orgViewColumnAllowance/customColumns/delete', body || {}).then(function (r) { return r.data; });
    },

    payAction: function (trnId, action, reason) {
      return post('/api/calc/payAction', { trnId: trnId, action: action, reason: reason || '' }).then(function (r) { return r.data; });
    },
    hqPermissionMng: function (params) {
      return get('/api/hq/permissionMng', params).then(function (r) { return r.data; });
    },
    hqPermissionMngSave: function (body) {
      return post('/api/hq/permissionMng/save', body || {}).then(function (r) { return r.data; });
    },
    hqOpsModeMng: function (params) {
      return get('/api/hq/opsModeMng', params).then(function (r) { return r.data; });
    },
    hqOpsModeMngSave: function (body) {
      return post('/api/hq/opsModeMng/save', body || {}).then(function (r) { return r.data; });
    },
    hqOrgUnitPermission: function (params) {
      return get('/api/hq/orgUnitPermission', params || {}).then(function (r) { return r.data; });
    },
    hqOrgUnitPermissionSave: function (body) {
      return post('/api/hq/orgUnitPermission/save', body || {}).then(function (r) { return r.data; });
    },
    hqOrgUnitAssistantPermissionSave: function (body) {
      return post('/api/hq/orgUnitAssistantPermission/save', body || {}).then(function (r) { return r.data; });
    },
    hqAccountAccessList: function (params) {
      return get('/api/hq/accountAccess', params || {}).then(function (r) {
        var d = r.data || r;
        var list = d.list || [];
        return {
          list: list,
          totalElements: list.length,
          totalPages: 1,
          page: 1,
          size: list.length || 20,
          users: Array.isArray(d.users) ? d.users : [],
          comps: Array.isArray(d.comps) ? d.comps : []
        };
      });
    },
    hqAccountAccessAdd: function (body) {
      return post('/api/hq/accountAccess/add', body || {}).then(function (r) { return r.data || r; });
    },
    hqAccountAccessUpdate: function (id, body) {
      return put('/api/hq/accountAccess/' + encodeURIComponent(id), body || {}).then(function (r) { return r.data || r; });
    },
    hqAccountAccessDelete: function (id) {
      return del('/api/hq/accountAccess/' + encodeURIComponent(id)).then(function (r) { return r.data || r; });
    },

    /** 본사·총판 관리자(웹) URL 호스트와 일치하는 포털 + 브랜딩 (로그인·비밀번호 설정 페이지용) */
    orgPortalByHost: function (host) {
      var h = host || '';
      var base = getBaseUrl();
      var url = base + '/api/public/org/portalByHost?host=' + encodeURIComponent(h);
      return fetchTextThenJson(url, { method: 'GET', headers: { 'Accept': 'application/json' }, credentials: 'same-origin' }, '포털(host) 조회 응답이 JSON이 아닙니다.');
    },
    /** 본사/총판 브랜딩 - 로그인 페이지용 (인증 불필요, host 우선 조회 가능) */
    orgBrandingPublic: function (compId, host) {
      var base = getBaseUrl();
      var q = [];
      if (host) q.push('host=' + encodeURIComponent(host));
      if (compId) q.push('compId=' + encodeURIComponent(compId));
      var url = base + '/api/public/org/branding' + (q.length ? ('?' + q.join('&')) : '');
      return fetchTextThenJson(url, { method: 'GET', headers: { 'Accept': 'application/json' }, credentials: 'same-origin' }, '공개 브랜딩 조회 응답이 JSON이 아닙니다.')
        .then(function (r) { return r.data || r; });
    },
    /** 본사/총판 브랜딩 조회 */
    orgBranding: function (compId) {
      return get('/api/org/branding', compId ? { compId: compId } : {}).then(function (r) { return r.data || r; });
    },
    /** 브랜딩 이미지 업로드 */
    orgBrandingUpload: function (compId, imageType, file) {
      if (!file || typeof file.size !== 'number') {
        return Promise.reject(new Error('업로드할 파일을 선택하세요.'));
      }
      var max = (imageType === 'main') ? (5 * 1024 * 1024) : (1 * 1024 * 1024); // main 5MB, first/logo/popcon 1MB
      if (file.size > max) {
        var typeNm = imageType === 'main'
          ? '메인이미지'
          : (imageType === 'popcon' ? '팝콘이미지'
            : (imageType === 'first' ? '첫화면 로고이미지'
              : (imageType === 'urlPay' ? 'URL결제이미지' : '로고이미지')));
        var maxMb = imageType === 'main' ? '5MB' : '1MB';
        return Promise.reject(new Error(typeNm + '는 ' + maxMb + ' 이하만 업로드할 수 있습니다.'));
      }
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('compId', compId);
      fd.append('imageType', imageType);
      fd.append('file', file);
      var headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      var uploadOnce = function (uploadFile) {
        var sendFd = new FormData();
        sendFd.append('compId', compId);
        sendFd.append('imageType', imageType);
        sendFd.append('file', uploadFile);
        return fetchTextThenJson(base + '/api/org/branding/upload', {
          method: 'POST',
          headers: headers,
          body: sendFd
        }, '브랜딩 이미지 업로드 응답이 JSON이 아닙니다. 운영 서버에 최신 API가 배포됐는지, Nginx client_max_body_size(용량)를 확인하세요.');
      };
      return uploadOnce(file).catch(function (err) {
        var msg = err && err.message ? String(err.message) : '';
        if (msg.indexOf('HTTP 413') === -1) return Promise.reject(err);
        // 프록시 제한(예: 1MB) 환경 대응: 자동 압축 후 1회 재시도
        return compressImageForUpload(file, 900 * 1024).then(function (compressed) {
          return uploadOnce(compressed);
        }).catch(function (e2) {
          return Promise.reject(e2 || err);
        });
      }).then(function (r) {
        if (r && r.success === false) throw new Error(serverMsgT(r.message, '브랜딩 업로드 실패'));
        return r.data || r;
      });
    },
    /** 브랜딩 테마 저장 */
    orgBrandingSave: function (compId, theme, brandHost, siteName) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      var params = new URLSearchParams();
      params.set('compId', compId);
      params.set('theme', theme || 'DEFAULT');
      if (typeof brandHost === 'string') params.set('brandHost', brandHost);
      if (typeof siteName === 'string') params.set('siteName', siteName);
      return fetchTextThenJson(base + '/api/org/branding/save', {
        method: 'POST',
        headers: headers,
        body: params
      }, '브랜딩(테마) 저장 응답이 JSON이 아닙니다.').then(function (r) {
        if (r && r.success === false) throw new Error(serverMsgT(r.message, '브랜딩 저장 실패'));
        return r.data || r;
      });
    },
    /** 브랜딩 이미지 삭제 */
    orgBrandingDeleteImage: function (compId, imageType) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      var params = new URLSearchParams();
      params.set('compId', compId || '');
      params.set('imageType', imageType || '');
      return fetchTextThenJson(base + '/api/org/branding/delete-image', {
        method: 'POST',
        headers: headers,
        body: params
      }, '브랜딩 이미지 삭제 응답이 JSON이 아닙니다.').then(function (r) {
        if (r && r.success === false) throw new Error(serverMsgT(r.message, '브랜딩 이미지 삭제 실패'));
        return r.data || r;
      });
    }
  };
})();
