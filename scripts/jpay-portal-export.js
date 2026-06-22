/**
 * JPAY 가맹 포털 — 로그인 → 주문 Export → 엑셀 저장
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const SCRIPT_VERSION = '2026-06-23-v10';
const BASE = 'https://merchant.j-pay.net';

/** 통합조회·대조에 필요한 JPAY Export 컬럼 (Card BIN 포함) */
const REQUIRED_EXPORT_FIELDS = [
  'Gateway Access Number',
  'Transaction ID',
  'Merchant Order Number',
  'Transaction Currency',
  'Original Currency',
  'Transaction Amount',
  'Fee',
  'Trading Status',
  'Refund Status',
  'Is it a chargeback?',
  'RDR',
  'Transaction Date',
  'URL Source',
  'Card BIN',
];

function arg(name, def) {
  const p = process.argv.find((a) => a.startsWith('--' + name + '='));
  if (!p) return def;
  return p.split('=').slice(1).join('=');
}

function log(step, detail) {
  const msg = detail ? `[jpay-export] ${step}: ${detail}` : `[jpay-export] ${step}`;
  console.error(msg);
}

function launchOptions() {
  const headed = process.env.JPAY_EXPORT_HEADED === '1'
    || (process.env.DISPLAY && process.env.DISPLAY.length > 0);
  return {
    headless: !headed,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--disable-blink-features=AutomationControlled',
    ],
  };
}

async function dumpDebug(page, tag) {
  try {
    const dir = path.join(__dirname, 'debug');
    fs.mkdirSync(dir, { recursive: true });
    const ts = Date.now();
    const png = path.join(dir, `${tag}-${ts}.png`);
    const html = path.join(dir, `${tag}-${ts}.html`);
    if (page && !page.isClosed()) {
      await page.screenshot({ path: png, fullPage: true }).catch(() => null);
      const content = await page.content().catch(() => '');
      if (content) fs.writeFileSync(html, content, 'utf8');
      log('debug', `saved ${png}`);
    }
  } catch (_) { /* ignore */ }
}

async function dismissOverlays(page) {
  await page.keyboard.press('Escape').catch(() => null);
  await page.waitForTimeout(400);
  await page.keyboard.press('Escape').catch(() => null);
  await page.waitForTimeout(300);
}

async function fillLogin(page, user, pw) {
  log('login', 'open login page');
  await page.goto(BASE + '/#/login', { waitUntil: 'networkidle', timeout: 90000 });
  await page.waitForTimeout(2000);
  const userBox = page.locator('input[type="text"], input[type="email"]').first();
  const pwBox = page.locator('input[type="password"]').first();
  await userBox.waitFor({ state: 'visible', timeout: 30000 });
  await userBox.fill(user);
  await pwBox.fill(pw);
  await page.getByRole('button', { name: /登\s*录|登录|Login/i }).click();
  log('login', 'submitted, waiting');
  await page.waitForTimeout(5000);
  const url = page.url();
  log('login', 'url=' + url);
  if (url.includes('/login')) {
    await dumpDebug(page, 'login-fail');
    throw new Error('JPAY 포털 로그인 실패 — ID·비밀번호를 확인하세요.');
  }
}

async function openAllOrders(page) {
  log('orders', 'navigate All Orders');
  await page.goto(BASE + '/#/order/all', { waitUntil: 'networkidle', timeout: 90000 }).catch(async () => {
    await page.goto(BASE + '/#/order/all', { waitUntil: 'domcontentloaded', timeout: 60000 });
  });
    await page.waitForTimeout(2000);
  if (!page.url().includes('/order')) {
    const menu = page.getByText(/^All Orders$/i).first();
    if (await menu.isVisible().catch(() => false)) {
      await menu.click();
      await page.waitForTimeout(2500);
    }
  }
  await page.getByText(/^All Orders$/i).first().waitFor({ state: 'visible', timeout: 30000 }).catch(() => null);
  await waitForOrdersTable(page);
}

async function waitForOrdersTable(page) {
  log('orders', 'wait for table load');
  await page.locator('.ant-spin-spinning').waitFor({ state: 'hidden', timeout: 90000 }).catch(() => null);
  await page.waitForTimeout(1500);
  await page.locator('table tbody tr, .ant-table-row, .ant-table-tbody tr')
    .first().waitFor({ state: 'visible', timeout: 90000 }).catch(() => null);
  await dismissOverlays(page);
}

async function setDateRange(page, from, to) {
  log('date', from + ' ~ ' + to);
  await dismissOverlays(page);
  const rangePicker = page.locator('.ant-picker-range').first();
  if (!(await rangePicker.isVisible().catch(() => false))) {
    log('date', 'picker not found, skip');
    return;
  }
  await rangePicker.click().catch(() => null);
  await page.waitForTimeout(800);
  const inputs = page.locator('.ant-picker-dropdown:visible .ant-picker-input input');
  const n = await inputs.count().catch(() => 0);
  if (n < 2) {
    log('date', 'picker inputs not ready, skip search');
    await dismissOverlays(page);
    return;
  }
  await inputs.nth(0).click();
  await inputs.nth(0).fill(from);
  await inputs.nth(1).click();
  await inputs.nth(1).fill(to);
  await page.keyboard.press('Enter').catch(() => null);
  await page.waitForTimeout(400);
  await dismissOverlays(page);
  const searchBtn = page.getByRole('button', { name: /^Search$|^查询$/i }).first();
  if (await searchBtn.isVisible().catch(() => false)) {
    await searchBtn.click();
    await waitForOrdersTable(page);
  }
}

async function findExportButton(page) {
  const locators = [
    page.locator('button, .ant-btn, a, [role="button"]').filter({ hasText: /^Export$/i }),
    page.locator('button, .ant-btn, a, [role="button"]').filter({ hasText: /\bExport\b/i }),
    page.getByRole('button', { name: /Export/i }),
    page.getByText(/^Export$/i),
  ];

  let best = null;
  let bestX = -1;
  for (const loc of locators) {
    const count = await loc.count().catch(() => 0);
  for (let i = 0; i < count; i++) {
      const el = loc.nth(i);
      if (!(await el.isVisible().catch(() => false))) continue;
      if (await el.isDisabled().catch(() => false)) continue;
      const box = await el.boundingBox();
      if (!box || box.top > 450 || box.top < 0) continue;
      if (box.width < 16 || box.height < 12) continue;
      if (box.x > bestX) {
        bestX = box.x;
        best = el;
      }
    }
    if (best) break;
  }

  if (!best) {
    const marked = await page.evaluate(() => {
      const norm = (s) => (s || '').replace(/\s+/g, ' ').trim();
      const isExport = (t) => /^Export$/i.test(norm(t)) || norm(t) === '导出' || /\bExport\b/i.test(norm(t));
      document.querySelectorAll('[data-jpay-export-btn]').forEach((n) => n.removeAttribute('data-jpay-export-btn'));
      let bestEl = null;
      let bestX = -1;
      for (const el of document.querySelectorAll('button, .ant-btn, a, [role="button"], span, div')) {
        if (!isExport(el.textContent || '')) continue;
        const btn = el.closest('button') || el.closest('.ant-btn') || el.closest('[role="button"]') || el;
        const r = btn.getBoundingClientRect();
        if (r.width < 16 || r.height < 12 || r.top > 450 || r.top < 0) continue;
        const st = window.getComputedStyle(btn);
        if (st.display === 'none' || st.visibility === 'hidden') continue;
        if (r.x > bestX) {
          bestX = r.x;
          bestEl = btn;
        }
      }
      if (!bestEl) return null;
      bestEl.setAttribute('data-jpay-export-btn', '1');
      const r = bestEl.getBoundingClientRect();
      return { x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width) };
    });
    if (marked) {
      log('orders', 'Export (dom) x=' + marked.x + ' y=' + marked.y + ' w=' + marked.w);
      return page.locator('[data-jpay-export-btn="1"]');
    }
    await dumpDebug(page, 'export-btn-not-found');
    throw new Error('All Orders 오른쪽 상단 Export 버튼을 찾지 못했습니다.');
  }

  const box = await best.boundingBox();
  log('orders', 'Export button x=' + Math.round(box.x) + ' y=' + Math.round(box.y) + ' w=' + Math.round(box.width));
  return best;
}

async function probeDrawerState(page) {
  return page.evaluate(() => {
    const norm = (s) => (s || '').replace(/\s+/g, ' ').trim();
    const text = document.body.innerText || '';
    const drawers = Array.from(document.querySelectorAll('.ant-drawer, .ant-drawer-content-wrapper'));
    const openDrawer = drawers.find((d) => {
      const st = window.getComputedStyle(d);
      return d.classList.contains('ant-drawer-open')
        || (st.display !== 'none' && /Select Fields To Export|选择导出字段/i.test(d.textContent || ''));
    });
    return {
      hasTitle: /Select Fields To Export|选择导出字段/i.test(text),
      hasSelectAll: /Select all|全选/i.test(text),
      hasConfirm: /Confirm Export|确认导出/i.test(text),
      drawerCount: drawers.length,
      openDrawer: !!openDrawer,
    };
  }).catch(() => ({}));
}

async function clickExportButton(page, exportBtn) {
  await exportBtn.scrollIntoViewIfNeeded().catch(() => null);
  await exportBtn.hover().catch(() => null);
  await page.waitForTimeout(200);
  try {
    await exportBtn.click({ timeout: 8000 });
    log('export', 'clicked Export (playwright)');
    return;
  } catch (_) { /* fallback */ }
  const box = await exportBtn.boundingBox();
  if (box) {
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
    log('export', 'clicked Export (mouse)');
    return;
  }
  await exportBtn.click({ force: true });
  log('export', 'clicked Export (force)');
}

async function waitForExportDrawer(page, timeoutMs = 60000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const state = await probeDrawerState(page);
    if ((state.hasSelectAll && state.hasConfirm) || state.hasTitle) {
      log('export', 'drawer open ' + JSON.stringify(state));
      await page.getByText(/^Select all$|^全选$/i).first()
        .waitFor({ state: 'visible', timeout: 10000 }).catch(() => null);
      await page.waitForTimeout(800);
      return;
    }
    await page.waitForTimeout(500);
  }
  const last = await probeDrawerState(page);
  log('export', 'drawer timeout state=' + JSON.stringify(last));
  throw new Error('drawer timeout');
}

/** Select all → 필요 필드만 남기고 나머지 해제 (Card BIN 포함) */
async function configureExportFields(page) {
  log('export', 'click Select all');
  const selectAll = page.locator('label, .ant-checkbox-wrapper, span, div')
    .filter({ hasText: /^Select all$|^全选$/i });
  await selectAll.first().waitFor({ state: 'visible', timeout: 30000 });
  await selectAll.first().click({ force: true });
  await page.waitForTimeout(1000);

  log('export', 'uncheck unused fields');
  const result = await page.evaluate((required) => {
    const norm = (s) => (s || '').replace(/\s+/g, ' ').trim();
    const isSelectAll = (t) => /^Select all$/i.test(t) || t === '全选';
    const requiredSet = new Set(required.map((r) => norm(r)));
    const wrappers = Array.from(document.querySelectorAll('label.ant-checkbox-wrapper, .ant-checkbox-wrapper, .el-checkbox'));
    let kept = 0;
    let unchecked = 0;
    for (const w of wrappers) {
      const t = norm(w.textContent);
      if (isSelectAll(t)) continue;
      const input = w.querySelector('input[type="checkbox"]');
      if (!input) continue;
      const r = w.getBoundingClientRect();
      if (r.width < 1 || r.height < 1) continue;
      const matched = requiredSet.has(t)
        || Array.from(requiredSet).some((rn) => t === rn || t.indexOf(rn) === 0);
      if (matched) {
        if (!input.checked) w.click();
        kept++;
        continue;
      }
      if (input.checked) {
        w.click();
        unchecked++;
      }
    }
    return { ok: kept > 0, kept, unchecked };
  }, REQUIRED_EXPORT_FIELDS).catch(() => ({ ok: false, reason: 'evaluate failed' }));

  log('export', 'fields: ' + JSON.stringify(result));
  if (!result.ok) {
    await dumpDebug(page, 'fields-config-fail');
    throw new Error('Export 필드 선택에 실패했습니다. reason=' + (result.reason || 'no fields kept'));
  }
  await page.waitForTimeout(600);
}

async function clickConfirmExport(page) {
  log('export', 'click Confirm Export');

  const playwrightClicked = await page.getByText(/^Confirm\s*Export$/i).first()
    .click({ force: true, timeout: 10000 }).then(() => true).catch(() => false);
  if (playwrightClicked) {
    log('export', 'Confirm Export clicked');
    return;
  }

  const clicked = await page.evaluate(() => {
    function findExportPanel() {
      const ant = document.querySelector('.ant-drawer-open')
        || Array.from(document.querySelectorAll('.ant-drawer-content-wrapper, .ant-drawer-content'))
          .find((d) => /Select Fields To Export|选择导出字段|Select all/i.test(d.textContent || ''));
      if (ant) return ant;
      const panels = Array.from(document.querySelectorAll('div, aside, section')).filter((el) => {
        const t = el.textContent || '';
        if (!/Select all|全选/i.test(t)) return false;
        if (!/Confirm Export|确认导出/i.test(t)) return false;
        if (!/Gateway Access Number|Card BIN|Transaction ID/i.test(t)) return false;
        const r = el.getBoundingClientRect();
        return r.width >= 200 && r.height >= 200;
      });
      if (!panels.length) return document.body;
      panels.sort((a, b) => {
        const ra = a.getBoundingClientRect();
        const rb = b.getBoundingClientRect();
        return ra.width * ra.height - rb.width * rb.height;
      });
      return panels[0];
    }

    const norm = (s) => (s || '').replace(/\s+/g, ' ').trim();
    const isConfirm = (t) => /^Confirm\s*Export$/i.test(norm(t)) || norm(t) === '确认导出';
    const panel = findExportPanel() || document.body;
    const header = panel.querySelector('.ant-drawer-header, .ant-drawer-extra, header') || panel;
    const nodes = header.querySelectorAll('button, a, span, div, .ant-btn');
    for (const el of nodes) {
      if (!isConfirm(el.textContent)) continue;
      const r = el.getBoundingClientRect();
      if (r.width < 20 || r.height < 10 || r.top > 320) continue;
      el.click();
      return true;
    }
    for (const el of document.querySelectorAll('button, a, span, div, .ant-btn')) {
      if (!isConfirm(el.textContent)) continue;
      const r = el.getBoundingClientRect();
      if (r.width < 20 || r.height < 10 || !r.top || r.top > 320) continue;
      el.click();
      return true;
    }
    return false;
  }).catch(() => false);

  if (!clicked) {
    await dumpDebug(page, 'confirm-export-not-found');
    throw new Error('Export 패널 상단 Confirm Export(确认导出) 버튼을 찾지 못했습니다.');
  }
  log('export', 'Confirm Export clicked (dom)');
}

async function waitForDownload(page, outPath) {
  let drawerOpened = false;
  for (let attempt = 1; attempt <= 3; attempt++) {
    await dismissOverlays(page);
    await waitForOrdersTable(page);
    const exportBtn = await findExportButton(page);
    log('export', 'click Export attempt ' + attempt);
    await clickExportButton(page, exportBtn);
    await page.waitForTimeout(3000);
    try {
      await waitForExportDrawer(page, 20000);
      drawerOpened = true;
      break;
    } catch (e) {
      log('export', 'drawer not shown attempt ' + attempt);
      if (attempt === 2) {
        log('export', 'reload orders page and retry without date change');
        await openAllOrders(page);
      }
      if (attempt === 3) {
        await dumpDebug(page, 'drawer-not-shown');
        throw new Error('Export 클릭 후 Select Fields To Export 패널이 열리지 않았습니다.');
      }
    }
  }
  if (!drawerOpened) {
    throw new Error('Export 패널을 열 수 없습니다.');
  }

  await configureExportFields(page);

  const downloadPromise = page.waitForEvent('download', { timeout: 180000 });
  await clickConfirmExport(page);

  let download;
  try {
    download = await downloadPromise;
  } catch (e) {
    const dlLink = page.getByText(/download of exported|导出下载|Download|下载/i).first();
    if (await dlLink.count().catch(() => 0)) {
      const dl2 = page.waitForEvent('download', { timeout: 120000 });
      await dlLink.click();
      download = await dl2;
    } else {
      await dumpDebug(page, 'export-fail');
      throw e;
    }
  }
  await download.saveAs(outPath);
  log('export', 'saved ' + outPath);
}

async function main() {
  const user = process.env.JPAY_PORTAL_USER || '';
  const pw = process.env.JPAY_PORTAL_PASSWORD || '';
  const from = arg('from', '');
  const to = arg('to', '');
  const out = arg('out', path.join(__dirname, 'jpay-export-out.xlsx'));
  if (!user || !pw) {
    console.error('JPAY_PORTAL_USER / JPAY_PORTAL_PASSWORD required');
    process.exit(2);
  }
  const outPath = path.resolve(out);
  fs.mkdirSync(path.dirname(outPath), { recursive: true });

  log('start', 'version=' + SCRIPT_VERSION + ' headed=' + (launchOptions().headless ? 'false' : 'true'));
  let browser;
  try {
    browser = await chromium.launch(launchOptions());
  } catch (e) {
    console.error('Chromium launch 실패.');
    throw e;
  }

  const context = await browser.newContext({
    acceptDownloads: true,
    viewport: { width: 1440, height: 900 },
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    locale: 'en-US',
  });
  const page = await context.newPage();

  try {
    await fillLogin(page, user, pw);
    await openAllOrders(page);
    if (from && to && process.env.JPAY_SKIP_DATE !== '1') {
      await setDateRange(page, from, to);
    } else if (from && to) {
      log('date', 'skipped (JPAY_SKIP_DATE=1)');
    }
    await waitForDownload(page, outPath);
    if (!fs.existsSync(outPath) || fs.statSync(outPath).size < 100) {
      throw new Error('다운로드 파일이 비어 있습니다.');
    }
    console.log('OK ' + outPath);
    process.exit(0);
  } catch (e) {
    await dumpDebug(page, 'error');
    const msg = String(e && e.message ? e.message : e);
    console.error(msg.length > 800 ? msg.slice(0, 800) + '…' : msg);
    process.exit(1);
  } finally {
    if (browser) await browser.close().catch(() => null);
  }
}

main();
