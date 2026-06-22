/**
 * JPAY 가맹 포털 — 로그인 → 주문 Export → 엑셀 저장
 *
 * 사용:
 *   cd scripts && npm install && npx playwright install chromium
 *   node jpay-portal-export.js --from=2026-06-01 --to=2026-06-22 --out=D:\tmp\out.xlsx
 *
 * 환경변수: JPAY_PORTAL_USER, JPAY_PORTAL_PASSWORD
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE = 'https://merchant.j-pay.net';
const ORDER_PATHS = [
  '#/order/all',
  '#/order/list',
  '#/order/orderList',
  '#/order/trade',
  '#/order/orders',
  '#/trade/order',
  '#/order'
];

function arg(name, def) {
  const p = process.argv.find((a) => a.startsWith('--' + name + '='));
  if (!p) return def;
  return p.split('=').slice(1).join('=');
}

async function fillLogin(page, user, pw) {
  await page.goto(BASE + '/#/login', { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForTimeout(1500);
  const userBox = page.locator('input[type="text"], input[type="email"]').first();
  const pwBox = page.locator('input[type="password"]').first();
  await userBox.fill(user);
  await pwBox.fill(pw);
  const loginBtn = page.getByRole('button', { name: /登\s*录|登录|Login/i });
  await loginBtn.click();
  await page.waitForTimeout(3000);
  if (page.url().includes('/login')) {
    throw new Error('JPAY 포털 로그인 실패 — ID·비밀번호를 확인하세요.');
  }
}

async function tryNavigateOrders(page) {
  for (const p of ORDER_PATHS) {
    await page.goto(BASE + '/' + p, { waitUntil: 'domcontentloaded', timeout: 45000 });
    await page.waitForTimeout(2000);
    const exportBtn = page.getByRole('button', { name: /export|导出|Export/i }).first();
    if (await exportBtn.count()) {
      return exportBtn;
    }
    const link = page.getByText(/all orders|全部订单|订单列表|Order/i).first();
    if (await link.count()) {
      await link.click().catch(() => null);
      await page.waitForTimeout(2000);
      const exportBtn2 = page.getByRole('button', { name: /export|导出|Export/i }).first();
      if (await exportBtn2.count()) {
        return exportBtn2;
      }
    }
  }
  throw new Error('JPAY 주문 목록·Export 버튼을 찾지 못했습니다. 포털 UI가 변경되었을 수 있습니다.');
}

async function setDateRange(page, from, to) {
  const inputs = page.locator('input');
  const count = await inputs.count();
  for (let i = 0; i < count; i++) {
    const el = inputs.nth(i);
    const ph = ((await el.getAttribute('placeholder')) || '').toLowerCase();
    const cls = ((await el.getAttribute('class')) || '').toLowerCase();
    if (ph.includes('date') || cls.includes('date') || ph.includes('日期')) {
      await el.fill(from).catch(() => null);
    }
  }
  const range = page.locator('.ant-picker-input input, input[type="date"]').all();
  const ranges = await range;
  if (ranges.length >= 2) {
    await ranges[0].fill(from).catch(() => null);
    await ranges[1].fill(to).catch(() => null);
  }
  const searchBtn = page.getByRole('button', { name: /search|查询|検索|Search/i }).first();
  if (await searchBtn.count()) {
    await searchBtn.click().catch(() => null);
    await page.waitForTimeout(2500);
  }
}

async function waitForDownload(page, outPath, exportBtn) {
  const downloadPromise = page.waitForEvent('download', { timeout: 120000 });
  await exportBtn.click();
  let download;
  try {
    download = await downloadPromise;
  } catch (e) {
    const dlLink = page.getByText(/download of exported|导出下载|Download/i).first();
    if (await dlLink.count()) {
      const dl2 = page.waitForEvent('download', { timeout: 90000 });
      await dlLink.click();
      download = await dl2;
    } else {
      throw e;
    }
  }
  await download.saveAs(outPath);
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

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ acceptDownloads: true });
  const page = await context.newPage();
  try {
    await fillLogin(page, user, pw);
    const exportBtn = await tryNavigateOrders(page);
    if (from && to) {
      await setDateRange(page, from, to);
      const exportBtn2 = page.getByRole('button', { name: /export|导出|Export/i }).first();
      if (await exportBtn2.count()) {
        await waitForDownload(page, outPath, exportBtn2);
      } else {
        await waitForDownload(page, outPath, exportBtn);
      }
    } else {
      await waitForDownload(page, outPath, exportBtn);
    }
    if (!fs.existsSync(outPath) || fs.statSync(outPath).size < 100) {
      throw new Error('다운로드 파일이 비어 있습니다.');
    }
    console.log('OK ' + outPath);
    process.exit(0);
  } catch (e) {
    console.error(String(e && e.message ? e.message : e));
    process.exit(1);
  } finally {
    await browser.close();
  }
}

main();
