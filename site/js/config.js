/**
 * PG 통합관리자 - 사이트/API 설정
 * - icopay.co.kr: API는 https://api.icopay.co.kr 로 호출 (홈페이지 분리 가능)
 * - URL에 ?api=주소 넣으면 해당 주소로 강제 사용
 */
(function () {
  window.PG_BASE_URL = window.PG_BASE_URL || '';

  var host = (typeof window !== 'undefined' && window.location && window.location.hostname) ? window.location.hostname : '';
  var params = typeof window !== 'undefined' && window.location && window.location.search ? new URLSearchParams(window.location.search) : null;
  var forceApi = params && params.get('api');
  var savedApi = typeof localStorage !== 'undefined' && localStorage.getItem('pg_api_base');

  if (forceApi) {
    window.PG_API_BASE = forceApi;
    if (typeof localStorage !== 'undefined') localStorage.setItem('pg_api_base', forceApi);
  } else if (savedApi !== null && savedApi !== '') {
    window.PG_API_BASE = savedApi;
  } else if (window.PG_API_BASE === undefined) {
    if (host === 'icopay.co.kr' || host === 'www.icopay.co.kr') {
      window.PG_API_BASE = '';
    } else {
      window.PG_API_BASE = '';
    }
  }
})();
