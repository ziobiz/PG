import fs from 'fs';
const c = fs.readFileSync('site/js/screens.js', 'utf8');
const koRe = /[\u3131-\u318E\uAC00-\uD7A3]/;
const urls = [
  '/hq/defaultCommission', '/hq/chargebackPolicy', '/hq/businessDaySetting',
  '/hq/permissionMng', '/hq/opsModeMng', '/hq/userSettings', '/hq/accountMng',
  '/hq/orgViewColumnAllowance', '/hq/pgApiMng', '/hq/apiConfig',
  '/hq/paymentOrchestration', '/hq/urlPayDeploy', '/hq/notifyEnv',
  '/hq/notifyMapping', '/hq/notifyInbound', '/hq/ledgerSysSettings',
  '/hq/settlementAdmin', '/hq/receivableRecoverySettings', '/hq/domainConfig',
  '/hq/serverManage', '/hq/chatbotAiSettings'
];
let idx = 0;
const blocks = [];
for (const u of urls) {
  const key = "'" + u + "':";
  const start = c.indexOf(key, idx);
  if (start < 0) continue;
  let end = c.length;
  for (const u2 of urls) {
    if (u2 === u) continue;
    const p = c.indexOf("'" + u2 + "':", start + 10);
    if (p > start && p < end) end = p;
  }
  blocks.push({ u, text: c.slice(start, end) });
}
const gaps = [];
for (const { u, text } of blocks) {
  const lines = text.split('\n');
  lines.forEach((line, i) => {
    if (!koRe.test(line)) return;
    if (/data-pg-ui-t=|data-pg-ui-html=|data-pg-ui-placeholder=|data-pg-i18n-lbl=|pgUiParagraph|pgUiSpanText|pgUiFormLabelSpan|pgUiCardHeaderT|pgUiLiT|L\(|escUi\(L\(/.test(line)) return;
    if (/title:\s*'|notice:\s*'|label:\s*'|placeholder:\s*'/.test(line) && !/html:\s*function/.test(line)) return;
    gaps.push({ u, snippet: line.trim().slice(0, 120) });
  });
}
console.log('HQ i18n gaps (screens.js static, pass-2 scan):', gaps.length);
gaps.slice(0, 40).forEach((g) => console.log(g.u, '|', g.snippet));
if (gaps.length > 40) console.log('... and', gaps.length - 40, 'more');
