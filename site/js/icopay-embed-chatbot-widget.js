/**
 * ICOPAY 챗봇 결제 플로팅 위젯 (가맹점 외부 사이트 삽입용).
 * 부트스트랩: /v1/embed-chatbot/{compId} 가 window.__ICOPAY_EMBED_CHATBOT__ 를 설정한 뒤 이 파일을 로드합니다.
 *
 * 패널은 iframe 만 두어 chatbot-pay.html 의 자체 헤더가 최상단에 오도록 한다(이중 프레임 방지).
 * 크기: 가로 약 28vw~420px, 세로 약 78vh(Siamly/dmchamp 류 위젯 비율에 근접).
 */
(function () {
  'use strict';
  var cfg = window.__ICOPAY_EMBED_CHATBOT__;
  if (!cfg || !cfg.compId || !cfg.origin) {
    return;
  }
  if (document.getElementById('icopay-embed-chatbot-host')) {
    return;
  }

  var compEnc = encodeURIComponent(String(cfg.compId).trim());
  /* embed=1 → chatbot-pay.html 이 iframe 전용 레이아웃·글자 기준(15px)으로 표시 */
  var frameSrc = String(cfg.origin).replace(/\/$/, '') + '/chatbot-pay/' + compEnc + '?embed=1';

  var Z = 2147483000;
  var root = document.createElement('div');
  root.id = 'icopay-embed-chatbot-host';
  root.setAttribute('data-icopay-embed', '1');
  root.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:' + Z + ';font-family:system-ui,-apple-system,sans-serif;';

  var panel = document.createElement('div');
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-label', 'ICOPAY chatbot');
  panel.style.cssText = 'display:none;pointer-events:auto;position:fixed;right:20px;bottom:84px;'
      + 'width:min(max(28vw,320px),420px,calc(100vw - 24px));'
      + 'height:min(78vh,calc(100vh - 96px));max-height:80vh;'
      + 'padding:0;box-sizing:border-box;'
      + 'background:#fff;border-radius:14px;overflow:hidden;'
      + 'border:1px solid #e5e7eb;box-shadow:0 4px 24px rgba(0,0,0,.1);';

  var iframe = document.createElement('iframe');
  iframe.title = 'ICOPAY chatbot payment';
  iframe.style.cssText = 'display:block;width:100%;height:100%;border:0;margin:0;padding:0;vertical-align:top;background:#fff;';
  iframe.setAttribute('referrerpolicy', 'strict-origin-when-cross-origin');

  panel.appendChild(iframe);

  var fab = document.createElement('button');
  fab.type = 'button';
  fab.setAttribute('aria-label', 'Open chatbot');
  fab.style.cssText = 'pointer-events:auto;position:fixed;right:20px;bottom:18px;width:52px;height:52px;border-radius:50%;'
      + 'border:0;cursor:pointer;background:#0d6efd;color:#fff;'
      + 'box-shadow:0 4px 14px rgba(13,110,253,.4);display:flex;align-items:center;justify-content:center;padding:0;';

  fab.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>';

  var open = false;
  function setOpen(v) {
    open = v;
    panel.style.display = v ? 'block' : 'none';
    if (v && !iframe.getAttribute('src')) {
      iframe.setAttribute('src', frameSrc);
    }
    fab.setAttribute('aria-expanded', v ? 'true' : 'false');
    fab.setAttribute('aria-label', v ? 'Close chatbot panel' : 'Open chatbot');
    fab.innerHTML = v
        ? '<svg xmlns="http://www.w3.org/2000/svg" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true"><path d="M6 9l6 6 6-6"/></svg>'
        : '<svg xmlns="http://www.w3.org/2000/svg" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>';
  }

  fab.addEventListener('click', function () {
    setOpen(!open);
  });

  document.addEventListener('keydown', function (ev) {
    if (ev.key === 'Escape' && open) {
      setOpen(false);
    }
  });

  root.appendChild(panel);
  root.appendChild(fab);
  document.body.appendChild(root);
})();
