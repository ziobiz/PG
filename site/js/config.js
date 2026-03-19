/**
 * PG 통합관리자 - 사이트/API 설정
 * 운영 API 기본값: https://api.icopay.co.kr (단일)
 * - localhost: http://localhost:8080
 * - 우선순위: URL ?api= > <html data-pg-api-base="..."> > localStorage pg_api_base > 호스트 규칙
 */
(function () {
  window.PG_BASE_URL = window.PG_BASE_URL || '';

  /** 공개 API 루트 (NOTI·SSL 도메인과 동일) */
  var PG_PUBLIC_ICOPAY_API = 'https://api.icopay.co.kr';
  window.PG_PUBLIC_ICOPAY_API = PG_PUBLIC_ICOPAY_API;

  var host = (typeof window !== 'undefined' && window.location && window.location.hostname) ? window.location.hostname : '';
  var params = typeof window !== 'undefined' && window.location && window.location.search ? new URLSearchParams(window.location.search) : null;
  var forceApi = params && params.get('api');
  var savedApi = typeof localStorage !== 'undefined' ? localStorage.getItem('pg_api_base') : null;

  var fromHtml = '';
  try {
    if (typeof document !== 'undefined' && document.documentElement) {
      fromHtml = (document.documentElement.getAttribute('data-pg-api-base') || '').trim();
    }
  } catch (e) { /* ignore */ }

  if (forceApi) {
    window.PG_API_BASE = forceApi.replace(/\/$/, '');
    if (typeof localStorage !== 'undefined') localStorage.setItem('pg_api_base', window.PG_API_BASE);
  } else if (fromHtml) {
    window.PG_API_BASE = fromHtml.replace(/\/$/, '');
  } else if (savedApi !== null && savedApi !== '') {
    var s = savedApi.replace(/\/$/, '');
    var apiLooksLocal = /^https?:\/\/(localhost|127\.0\.0\.1)(:|\/|$)/i.test(s);
    var pageIsLocal = host === 'localhost' || host === '127.0.0.1';
    if (apiLooksLocal && !pageIsLocal) {
      try { localStorage.removeItem('pg_api_base'); } catch (e) { /* ignore */ }
      // 삭제만 하고 BASE 미설정이면 이후 분기에 안 걸려 8080/빈 URL 오류 → 운영 API로 고정
      window.PG_API_BASE = PG_PUBLIC_ICOPAY_API;
    } else {
      window.PG_API_BASE = s;
    }
  } else if (window.PG_API_BASE === undefined) {
    if (host === 'localhost' || host === '127.0.0.1') {
      window.PG_API_BASE = 'http://localhost:8080';
    } else {
      // icopay.co.kr / 카페24 / 기타 → 모두 api.icopay.co.kr (CORS는 application.yml 에 도메인 등록)
      window.PG_API_BASE = PG_PUBLIC_ICOPAY_API;
    }
  }

  /**
   * https://api.icopay.co.kr/login.html 처럼 "API 도메인"에서 열면 항상 현재 origin 과 동일하게 맞춤.
   * (localStorage에 localhost가 남아 있거나, http/https 혼선 시에도 same-origin 유지)
   */
  if (!forceApi && !fromHtml && host === 'api.icopay.co.kr') {
    try {
      var originBase = (window.location.origin || '').replace(/\/$/, '');
      if (originBase) window.PG_API_BASE = originBase;
    } catch (e) { /* ignore */ }
  }

  /**
   * 브랜딩 이미지 등 절대 URL 조합
   */
  window.PG_assetApiBase = function () {
    if (host === 'api.icopay.co.kr') {
      try {
        var og = (window.location.origin || '').replace(/\/$/, '');
        if (og) return og;
      } catch (e2) { /* ignore */ }
    }
    var b = (window.PG_API_BASE || '').replace(/\/$/, '');
    if (b) return b;
    if (host === 'localhost' || host === '127.0.0.1') return 'http://localhost:8080';
    return PG_PUBLIC_ICOPAY_API;
  };
})();
