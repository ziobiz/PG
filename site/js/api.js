/**
 * PG 통합관리자 - 백엔드 API 클라이언트 (인증·공공사항·결제·업체)
 */
(function () {
  'use strict';

  var AUTH_TOKEN_KEY = 'pg_admin_token';
  var AUTH_USER_KEY = 'pg_admin_user';
  var AUTH_FLAG_KEY = 'pg_admin_auth';

  function getBaseUrl() {
    return (window.PG_API_BASE || '').replace(/\/$/, '');
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

  function request(options) {
    var base = getBaseUrl();
    var url = base ? (base + (options.path || options.url)) : (options.path || options.url);
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
    init.credentials = 'same-origin';
    return fetch(url, init).then(function (res) {
      if (res.status === 401) {
        clearAuth();
        if (typeof window.location !== 'undefined') window.location.replace((window.location.origin || '') + '/login.html');
        return Promise.reject(new Error('인증이 만료되었습니다. 다시 로그인하세요.'));
      }
      return res.text().then(function (text) {
        var data;
        try { data = text ? JSON.parse(text) : {}; } catch (e) { data = {}; }
        if (data.success === false) {
          return Promise.reject(new Error(data.message || '요청 처리에 실패했습니다.'));
        }
        return data;
      });
    }).catch(function (err) {
      var msg = (err && err.message) ? err.message : '';
      if (msg === 'Failed to fetch' || msg.indexOf('NetworkError') !== -1 || msg.indexOf('Load failed') !== -1 || msg === 'Network request failed') {
        return Promise.reject(new Error('서버에 연결할 수 없습니다. API 서버(8080)가 실행 중인지 확인하세요.'));
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

  window.PG_API = {
    getToken: getToken,
    setAuth: setAuth,
    clearAuth: clearAuth,
    getBaseUrl: getBaseUrl,

    login: function (username, password) {
      return post('/api/auth/login', { username: username, password: password });
    },

    noticeList: function (params) {
      return get('/api/system/notice', params).then(function (r) { return r.data; });
    },

    payList: function (params) {
      return get('/api/calc/payList', params).then(function (r) { return r.data; });
    },

    seedDev: function () {
      return get('/api/dev/seed');
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

    compDetail: function (compId) {
      return get('/api/comp/detail', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
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

    commissionList: function (params) {
      return get('/api/commission/list', params).then(function (r) { return r.data; });
    },
    commissionDetail: function (compId) {
      return get('/api/commission/detail', { compId: compId }).then(function (r) {
        if (r.success === false && r.success !== undefined) throw new Error(r.message || '조회 실패');
        return r.data;
      });
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

    menuOrderMng: function (params) {
      return get('/api/user/menuOrderMng', params).then(function (r) { return r.data; });
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
    settlementHoldList: function (params) {
      return get('/api/settlement/holdList', params).then(function (r) { return r.data; });
    },
    settlementExecute: function (params) {
      return get('/api/settlement/execute', params).then(function (r) { return r.data; });
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
    hqApiConfig: function () {
      return get('/api/hq/apiConfig').then(function (r) { return r.data; });
    },
    hqApiConfigSave: function (body) {
      return post('/api/hq/apiConfig/save', body).then(function (r) { return r.data; });
    },
    hqPermissionMng: function (params) {
      return get('/api/hq/permissionMng', params).then(function (r) { return r.data; });
    },
    hqPermissionMngSave: function (body) {
      return post('/api/hq/permissionMng/save', body).then(function (r) { return r.data; });
    }
  };
})();
