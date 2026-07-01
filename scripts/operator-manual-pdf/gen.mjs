/**
 * ICOPAY 운영자 HTML 메뉴얼 → PDF (Playwright)
 *
 * 사용 (저장소 루트 scripts/ 에 playwright 설치 후):
 *   cd scripts && npm install
 *   node operator-manual-pdf/gen.mjs
 *   node operator-manual-pdf/gen.mjs hq          # 본사만 (기본)
 *   node operator-manual-pdf/gen.mjs all         # 총본사·본사·총판 전체
 *   node operator-manual-pdf/gen.mjs hq "C:\Users\...\ICOPAY 메뉴얼"
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath, pathToFileURL } from 'url';
import { chromium } from 'playwright';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..', '..');
const DOCS = path.join(REPO_ROOT, 'docs');
const VERSION = '260625_V3.0';

const CATALOG = {
  super: {
    label: 'Super Admin',
    filePrefix: 'ICOPAY Super Admin Operation Manual',
    items: [
      { lang: 'KR', html: 'icopay-operator-manual.html' },
      { lang: 'EN', html: 'icopay-operator-manual-en.html' },
      { lang: 'JP', html: 'icopay-operator-manual-ja.html' },
      { lang: 'TH', html: 'icopay-operator-manual-th.html' },
      { lang: 'CH', html: 'icopay-operator-manual-zh.html' },
    ],
  },
  hq: {
    label: 'Headquarters',
    filePrefix: 'ICOPAY Headquarters Operation Manual',
    items: [
      { lang: 'KR', html: 'icopay-operator-manual-hq.html' },
      { lang: 'EN', html: 'icopay-operator-manual-hq-en.html' },
      { lang: 'JP', html: 'icopay-operator-manual-hq-ja.html' },
      { lang: 'TH', html: 'icopay-operator-manual-hq-th.html' },
      { lang: 'CH', html: 'icopay-operator-manual-hq-zh.html' },
    ],
  },
  dist: {
    label: 'Distributor',
    filePrefix: 'ICOPAY Distributor Operation Manual',
    items: [
      { lang: 'KR', html: 'icopay-operator-manual-dist.html' },
      { lang: 'EN', html: 'icopay-operator-manual-dist-en.html' },
      { lang: 'JP', html: 'icopay-operator-manual-dist-ja.html' },
      { lang: 'TH', html: 'icopay-operator-manual-dist-th.html' },
      { lang: 'CH', html: 'icopay-operator-manual-dist-zh.html' },
    ],
  },
};

const mode = (process.argv[2] || 'hq').toLowerCase();
const extraOut = process.argv[3] ? path.resolve(process.argv[3]) : null;
const outDir = path.join(DOCS, 'manual-pdf');

const roles = mode === 'all' ? ['super', 'hq', 'dist'] : [mode === 'dist' ? 'dist' : mode === 'super' ? 'super' : 'hq'];

async function exportOne(browser, roleKey, item) {
  const role = CATALOG[roleKey];
  const htmlPath = path.join(DOCS, item.html);
  if (!fs.existsSync(htmlPath)) {
    console.warn('SKIP missing', htmlPath);
    return null;
  }
  const pdfName = `${role.filePrefix}_${item.lang}_${VERSION}.pdf`;
  const pdfPath = path.join(outDir, pdfName);
  const page = await browser.newPage();
  const url = pathToFileURL(htmlPath).href;
  await page.goto(url, { waitUntil: 'networkidle' });
  await page.emulateMedia({ media: 'print' });
  await page.pdf({
    path: pdfPath,
    format: 'A4',
    printBackground: true,
    margin: { top: '18mm', right: '16mm', bottom: '18mm', left: '16mm' },
  });
  await page.close();
  console.log('OK', pdfPath);
  if (extraOut) {
    fs.mkdirSync(extraOut, { recursive: true });
    const copyPath = path.join(extraOut, pdfName);
    fs.copyFileSync(pdfPath, copyPath);
    console.log('COPY', copyPath);
  }
  return pdfPath;
}

async function main() {
  fs.mkdirSync(outDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    for (const roleKey of roles) {
      const role = CATALOG[roleKey];
      console.log(`--- ${role.label} (${roleKey}) ---`);
      for (const item of role.items) {
        await exportOne(browser, roleKey, item);
      }
    }
  } finally {
    await browser.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
