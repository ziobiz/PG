/**
 * PG 통합관리자 - 백엔드 API 클라이언트 (인증·공공사항·결제·업체)
 */
(function () {
  'use strict';

  var AUTH_TOKEN_KEY = 'pg_admin_token';
  var AUTH_USER_KEY = 'pg_admin_user';
  var AUTH_FLAG_KEY = 'pg_admin_auth';

  /** 운영 API 기본 (config.js 의 PG_PUBLIC_ICOPAY_API 와 동일 개념) */
  function publicApiRoot() {
    var p = (typeof window !== 'undefined' && window.PG_PUBLIC_ICOPAY_API) ? String(window.PG_PUBLIC_ICOPAY_API) : 'https://api.icopay.co.kr';
    return p.replace(/\/$/, '').trim() || 'https://api.icopay.co.kr';
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
    if (b) return b;
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
  }

  function clearAuth() {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_USER_KEY);
    sessionStorage.removeItem(AUTH_FLAG_KEY);
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
          return Promise.reject(new Error('인증이 만료되었습니다. 다시 로그인하세요.'));
        }
        var data;
        try {
          data = text ? JSON.parse(text) : {};
        } catch (e1) {
          var flat = text ? String(text).replace(/<script[^>]*>[\s\S]*?<\/script>/gi, ' ').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 200) : '';
          var head = badParseHead || '서버 응답이 JSON이 아닙니다.';
          return Promise.reject(new Error(head + ' HTTP ' + res.status + (flat ? (' — ' + flat) : '') + ' (최신 pg-app 배포·Nginx 용량·502 등 확인)'));
        }
        if (!res.ok) {
          var hint = (data && data.message) ? data.message : (text ? String(text).slice(0, 120) : '');
          return Promise.reject(new Error('API 오류 HTTP ' + res.status + (hint ? ': ' + hint : '')));
        }
        return data;
      });
    }).catch(function (err) {
      var msg = (err && err.message) ? err.message : '';
      if (msg === 'Failed to fetch' || msg.indexOf('NetworkError') !== -1 || msg.indexOf('Load failed') !== -1 || msg === 'Network request failed') {
        return Promise.reject(new Error('API에 연결하지 못했습니다. 요청: ' + url));
      }
      return Promise.reject(err);
    });
  }

  function request(options) {
    var base = getBaseUrl();
    var path = options.path || options.url || '';
    if (path && path.charAt(0) !== '/') {
      path = '/' + path;
    }
    var url = base + path;
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
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
    if (!url) return Promise.reject(new Error('API 경로가 없습니다.'));
    // 교차 출처(관리자 도메인 ≠ API 도메인)에서 same-origin + Allow-Credentials 조합이 막히는 경우 방지.
    // Authorization: Bearer 는 omit 이어도 전송됨(쿠키만 생략).
    init.credentials = 'omit';
    init.mode = 'cors';
    return fetch(url, init).then(function (res) {
      if (res.status === 401) {
        clearAuth();
        if (typeof window.location !== 'undefined') window.location.replace((window.location.origin || '') + '/login.html');
        return Promise.reject(new Error('인증이 만료되었습니다. 다시 로그인하세요.'));
      }
      return res.text().then(function (text) {
        var data;
        try { data = text ? JSON.parse(text) : {}; } catch (e) { data = {}; }
        if (!res.ok) {
          var hint = (data && data.message) ? data.message : (text ? String(text).slice(0, 120) : '');
          return Promise.reject(new Error(
            'API 오류 HTTP ' + res.status + (hint ? (': ' + hint) : '') +
            ' (도메인/프록시·CORS 확인)'
          ));
        }
        if (data.success === false) {
          return Promise.reject(new Error(data.message || '요청 처리에 실패했습니다.'));
        }
        return data;
      });
    }).catch(function (err) {
      var msg = (err && err.message) ? err.message : '';
      if (msg === 'Failed to fetch' || msg.indexOf('NetworkError') !== -1 || msg.indexOf('Load failed') !== -1 || msg === 'Network request failed') {
        return Promise.reject(new Error(
          'API에 연결하지 못했습니다. 요청: ' + url +
            ' — 브라우저 F12 Network에서 해당 요청이 빨간색인지 확인하세요. ' +
            '카페24 등 정적 호스팅이면 응답 헤더 Content-Security-Policy의 connect-src에 https://api.icopay.co.kr 가 허용되는지, ' +
            'Nginx 앞단이 OPTIONS·POST를 API로 전달하는지, SSL·방화벽을 점검하세요. ' +
            '(이 메시지는 서버 로그인 검증 전 단계 오류이며, 본사/총판 포털 규칙과는 별개입니다.)'
        ));
      }
      return Promise.reject(err);
    });
  }

  function get(path, params) {
    return request({ path: path, method: 'GET', params: params || {} });
  }

  function post(path, body) {
    return request({ path: path, method: 'POST', body: body || {} });
  }

  function del(path) {
    return request({ path: path, method: 'DELETE' });
  }

  window.PG_API = {
    getToken: getToken,
    setAuth: setAuth,
    clearAuth: clearAuth,
    getBaseUrl: getBaseUrl,

    login: function (username, password) {
      var ch = '';
      try {
        if (typeof location !== 'undefined' && location.host) ch = location.host;
      } catch (e) {}
      return post('/api/auth/login', { username: username, password: password, clientHost: ch });
    },

    /** 현재 토큰 기준 사용자·소속 업체 (업체정보조회 등) */
    authMe: function () {
      return get('/api/auth/me');
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

    noticeList: function (params) {
      return get('/api/system/notice', params).then(function (r) { return r.data; });
    },

    noticeCreate: function (title, content) {
      return post('/api/system/notice', { title: title || '', content: content || '' });
    },

    payList: function (params) {
      return get('/api/calc/payList', params).then(function (r) { return r.data; });
    },

    seedDev: function () {
      return get('/api/dev/seed');
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
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
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
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류 (API 서버가 실행 중인지, 주소가 맞는지 확인하세요)')); }
          if (r.success === false && r.success !== undefined) throw new Error(r.message || '등록 실패');
          return r;
        });
      });
    },
    compCheckLoginId: function (loginId) {
      return get('/api/comp/check-login-id', { loginId: loginId || '' }).then(function (r) { return r.data; });
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
          return Promise.reject(new Error('인증이 만료되었습니다. 다시 로그인하세요.'));
        }
        if (!res.ok) {
          return res.text().then(function (t) {
            throw new Error(t || ('다운로드 실패 (HTTP ' + res.status + ')'));
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
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류')); }
          if (r.success === false && r.success !== undefined) throw new Error(r.message || '엑셀 등록 실패');
          return r;
        });
      });
    },

    compDetail: function (compId) {
      return get('/api/comp/detail', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
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
        if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
        return res.text().then(function (text) {
          var r;
          try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류 (API 서버가 실행 중인지, 주소가 맞는지 확인하세요)')); }
          if (r.success === false && r.success !== undefined) throw new Error(r.message || '수정 실패');
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
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류')); }
            if (r.success === false && r.success !== undefined) throw new Error(r.message || '비밀번호 초기화 실패');
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
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류')); }
            if (r.success === false && r.success !== undefined) throw new Error(r.message || '비밀번호 초기화 실패');
            return r;
          });
        });
    },
    compChangeLoginId: function (compId, newLoginId) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/comp/changeLoginId', { method: 'POST', headers: headers, body: new URLSearchParams({ compId: compId, newLoginId: newLoginId }) })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류')); }
            if (r.success === false && r.success !== undefined) throw new Error(r.message || '로그인ID 변경 실패');
            return r;
          });
        });
    },
    settlementSetting: function (compId) {
      return get('/api/comp/settlementSetting', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
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
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류')); }
            if (r.success === false && r.success !== undefined) throw new Error(r.message || '저장 실패');
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
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
        return r.data || {};
      });
    },

    commissionList: function (params) {
      return get('/api/commission/list', params).then(function (r) { return r.data; });
    },
    commissionDetail: function (compId) {
      return get('/api/commission/detail', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
        return r.data;
      });
    },
    commissionHistory: function (compId, params) {
      var q = Object.assign({ compId: compId || '' }, params || {});
      return get('/api/commission/history', q).then(function (r) { return r.data; });
    },
    commissionSave: function (compId, data) {
      var body = new URLSearchParams({ compId: compId });
      for (var k in data) if (data[k] !== undefined && data[k] !== null) body.append(k, data[k]);
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetch(base + '/api/commission/save', { method: 'POST', headers: headers, body: body })
        .then(function (res) {
          if (res.status === 401) { clearAuth(); if (window.location) window.location.replace((window.location.origin || '') + '/login.html'); return Promise.reject(new Error('인증이 만료되었습니다.')); }
          return res.text().then(function (text) {
            var r; try { r = text ? JSON.parse(text) : {}; } catch (e) { return Promise.reject(new Error('서버 응답 오류')); }
            if (r.success === false && r.success !== undefined) throw new Error(r.message || '저장 실패');
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
    settlementBalanceMng: function (params) {
      return get('/api/settlement/balanceMng', params).then(function (r) { return r.data; });
    },
    settlementBalanceDeduct: function (body) {
      var base = getBaseUrl();
      return fetch(base + '/api/settlement/balance/deduct', {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(body || {})
      }).then(handleResponse).then(function (r) { return r.data; });
    },
    settlementBalanceList: function (params) {
      return get('/api/settlement/balanceList', params).then(function (r) { return r.data; });
    },
    settlementUnpaidMng: function (params) {
      return get('/api/settlement/unpaidMng', params).then(function (r) { return r.data; });
    },
    settlementHoldList: function (params) {
      return get('/api/settlement/holdList', params).then(function (r) { return r.data; });
    },
    settlementCollateralList: function (params) {
      return get('/api/settlement/collateralList', params).then(function (r) { return r.data; });
    },
    settlementFeeList: function (params) {
      return get('/api/settlement/feeList', params).then(function (r) { return r.data; });
    },
    settlementExecute: function (params) {
      return get('/api/settlement/execute', params).then(function (r) { return r.data; });
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
    settlementExecuteRun: function (params) {
      var q = (params && typeof params === 'object') ? params : {};
      var path = '/api/settlement/execute/run';
      if (q.fromDate || q.toDate || q.merchantId) {
        var arr = [];
        if (q.fromDate) arr.push('fromDate=' + encodeURIComponent(q.fromDate));
        if (q.toDate) arr.push('toDate=' + encodeURIComponent(q.toDate));
        if (q.merchantId) arr.push('merchantId=' + encodeURIComponent(q.merchantId));
        path += (path.indexOf('?') >= 0 ? '&' : '?') + arr.join('&');
      }
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
    hqApiConfig: function () {
      return get('/api/hq/apiConfig').then(function (r) { return r.data; });
    },
    hqApiConfigSave: function (body) {
      return post('/api/hq/apiConfig/save', body).then(function (r) { return r.data; });
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
    hqNotifyTargetCreate: function (targetName) {
      return post('/api/hq/notifyEnv/targets/create', { targetName: targetName || '' }).then(function (r) { return r.data || r; });
    },
    hqNotifyTargetDelete: function (id) {
      return del('/api/hq/notifyEnv/targets/' + encodeURIComponent(id)).then(function (r) { return r.data || r; });
    },
    hqNotifyMapping: function () {
      return get('/api/hq/notifyMapping').then(function (r) { return r.data; });
    },
    hqNotifyMappingSave: function (body) {
      return post('/api/hq/notifyMapping/save', body || {}).then(function (r) { return r.data; });
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

    payAction: function (trnId, action) {
      return post('/api/calc/payAction', { trnId: trnId, action: action }).then(function (r) { return r.data; });
    },
    hqPermissionMng: function (params) {
      return get('/api/hq/permissionMng', params).then(function (r) { return r.data; });
    },
    hqPermissionMngSave: function (body) {
      return post('/api/hq/permissionMng/save', body || {}).then(function (r) { return r.data; });
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
          size: list.length || 20
        };
      });
    },
    hqAccountAccessAdd: function (body) {
      return post('/api/hq/accountAccess/add', body || {}).then(function (r) { return r.data || r; });
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
    /** 본사/총판 브랜딩 - 로그인 페이지용 (인증 불필요) */
    orgBrandingPublic: function (compId) {
      var base = getBaseUrl();
      var url = base + '/api/public/org/branding' + (compId ? '?compId=' + encodeURIComponent(compId) : '');
      return fetchTextThenJson(url, { method: 'GET', headers: { 'Accept': 'application/json' }, credentials: 'same-origin' }, '공개 브랜딩 조회 응답이 JSON이 아닙니다.')
        .then(function (r) { return r.data || r; });
    },
    /** 본사/총판 브랜딩 조회 */
    orgBranding: function (compId) {
      return get('/api/org/branding', compId ? { compId: compId } : {}).then(function (r) { return r.data || r; });
    },
    /** 브랜딩 이미지 업로드 */
    orgBrandingUpload: function (compId, imageType, file) {
      var base = getBaseUrl();
      var token = getToken();
      var fd = new FormData();
      fd.append('compId', compId);
      fd.append('imageType', imageType);
      fd.append('file', file);
      var headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      return fetchTextThenJson(base + '/api/org/branding/upload', {
        method: 'POST',
        headers: headers,
        body: fd
      }, '브랜딩 이미지 업로드 응답이 JSON이 아닙니다. 운영 서버에 최신 API가 배포됐는지, Nginx client_max_body_size(용량)를 확인하세요.').then(function (r) {
        if (r && r.success === false) throw new Error(r.message || '브랜딩 업로드 실패');
        return r.data || r;
      });
    },
    /** 브랜딩 테마 저장 */
    orgBrandingSave: function (compId, theme, brandHost) {
      var base = getBaseUrl();
      var token = getToken();
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      var params = new URLSearchParams();
      params.set('compId', compId);
      params.set('theme', theme || 'DEFAULT');
      if (typeof brandHost === 'string') params.set('brandHost', brandHost);
      return fetchTextThenJson(base + '/api/org/branding/save', {
        method: 'POST',
        headers: headers,
        body: params
      }, '브랜딩(테마) 저장 응답이 JSON이 아닙니다.').then(function (r) {
        if (r && r.success === false) throw new Error(r.message || '브랜딩 저장 실패');
        return r.data || r;
      });
    }
  };
})();
