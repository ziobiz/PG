/**
 * docs/가맹점_PG_API_연동가이드.md → 동명.pdf
 * 사용: npm install 후 node gen.mjs [입력.md [출력.pdf]]
 * 일괄: npm run build-manuals (목차 + ChillPay + JPAY PDF)
 * Windows: C:\\Windows\\Fonts\\malgun.ttf 필요(한글).
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import PDFDocument from 'pdfkit';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..', '..');
const DEFAULT_MD = path.join(REPO_ROOT, 'docs', '가맹점_PG_API_연동가이드.md');
const MALGUN = path.join(process.env.SystemRoot || 'C:\\Windows', 'Fonts', 'malgun.ttf');

const mdPath = process.argv[2] ? path.resolve(process.argv[2]) : DEFAULT_MD;
const pdfPath = process.argv[3] ? path.resolve(process.argv[3]) : mdPath.replace(/\.md$/i, '.pdf');

if (!fs.existsSync(mdPath)) {
  console.error('MD not found:', mdPath);
  process.exit(1);
}

const raw = fs.readFileSync(mdPath, 'utf8');
const lines = raw.split(/\r?\n/);

const doc = new PDFDocument({ size: 'A4', margin: 48, bufferPages: false });
const stream = fs.createWriteStream(pdfPath);
doc.pipe(stream);

if (fs.existsSync(MALGUN)) {
  doc.registerFont('KR', MALGUN);
} else {
  console.warn('WARN: malgun.ttf not found, Korean may not render:', MALGUN);
  doc.registerFont('KR', 'Helvetica');
}
const textWidth = doc.page.width - 96;
const bottomY = doc.page.height - 52;

function ensureSpace(need = 36) {
  if (doc.y > bottomY - need) {
    doc.addPage();
    doc.font('KR');
  }
}

let codeMode = false;
const codeLines = [];

function flushCode() {
  if (codeLines.length === 0) return;
  ensureSpace(48);
  doc.font('Courier').fontSize(8.5);
  for (const cl of codeLines) {
    ensureSpace(14);
    doc.text(cl.length ? cl : ' ', { width: textWidth, lineGap: 1 });
  }
  codeLines.length = 0;
  doc.font('KR').fontSize(10);
}

function stripMd(s) {
  return s
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1');
}

for (const line of lines) {
  const trimmed = line.trim();
  if (trimmed.startsWith('```')) {
    if (!codeMode) {
      codeMode = true;
      codeLines.length = 0;
    } else {
      flushCode();
      codeMode = false;
    }
    continue;
  }
  if (codeMode) {
    codeLines.push(line);
    continue;
  }

  const t = stripMd(line);

  if (trimmed === '---') {
    ensureSpace(24);
    const y = doc.y + 4;
    doc.moveTo(48, y).lineTo(doc.page.width - 48, y).strokeColor('#cccccc').stroke();
    doc.strokeColor('#000000');
    doc.moveDown(0.6);
    continue;
  }

  if (t.startsWith('# ')) {
    ensureSpace(72);
    doc.font('KR').fontSize(17).text(t.slice(2), { width: textWidth });
    doc.moveDown(0.55);
    doc.fontSize(10);
    continue;
  }
  if (t.startsWith('## ')) {
    ensureSpace(56);
    doc.font('KR').fontSize(13.5).text(t.slice(3), { width: textWidth });
    doc.moveDown(0.45);
    doc.fontSize(10);
    continue;
  }
  if (t.startsWith('### ')) {
    ensureSpace(44);
    doc.font('KR').fontSize(11.5).text(t.slice(4), { width: textWidth });
    doc.moveDown(0.35);
    doc.fontSize(10);
    continue;
  }

  if (trimmed.startsWith('|')) {
    ensureSpace(18);
    doc.font('KR').fontSize(8.8).text(t, { width: textWidth, lineGap: 1 });
    continue;
  }

  if (trimmed.startsWith('- [ ]')) {
    ensureSpace(22);
    doc.font('KR').fontSize(10).text('□ ' + stripMd(trimmed.replace(/^- \[[ x]\]\s*/, '')), { width: textWidth - 12 });
    continue;
  }
  if (trimmed.startsWith('- ')) {
    ensureSpace(22);
    doc.font('KR').fontSize(10).text('• ' + stripMd(trimmed.slice(2)), { width: textWidth - 12 });
    continue;
  }

  if (trimmed === '') {
    doc.moveDown(0.25);
    continue;
  }

  ensureSpace(28);
  doc.font('KR').fontSize(10).text(t.length ? t : ' ', { width: textWidth, lineGap: 2 });
}

doc.end();

stream.on('finish', () => {
  console.log('OK', pdfPath);
});

stream.on('error', (e) => {
  console.error(e);
  process.exit(1);
});
