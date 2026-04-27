import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '..');
const files = ['site/js/screens.js', 'site/js/app.js'];
const set = new Set();
const re = /['"]([^'"]*[\u3131-\u318E\uAC00-\uD7A3][^'"]*)['"]/g;
for (const rel of files) {
  const s = fs.readFileSync(path.join(root, rel), 'utf8');
  let m;
  while ((m = re.exec(s))) {
    const t = m[1].replace(/\\n/g, '\n');
    if (t.length > 0 && t.length < 500) set.add(t);
  }
}
const arr = [...set].sort((a, b) => a.localeCompare(b, 'ko'));
console.log('count', arr.length);
fs.writeFileSync(path.join(root, 'tools/ko-strings-raw.txt'), arr.join('\n'), 'utf8');
console.log('wrote tools/ko-strings-raw.txt');
