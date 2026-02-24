/**
 * PG 통합관리자 - 사이트/API 설정
 * - 로컬(localhost/127.0.0.1): API는 http://localhost:8080 으로 자동 설정 (백엔드 별도 실행 시)
 * - 운영(otlpay.cafe24.com 등): 같은 도메인으로 API 호출 (PG_API_BASE = '')
 * - URL에 ?api=http://localhost:8080 넣으면 해당 주소로 강제 사용 (개발용)
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
    if (host === 'localhost' || host === '127.0.0.1' || host === '') {
      window.PG_API_BASE = 'http://localhost:8080';
    } else {
      window.PG_API_BASE = '';
    }
  }
})();
