import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '..');
const s = fs.readFileSync(path.join(root, 'site/js/screens.js'), 'utf8');
const set = new Set();
function grab(re) {
  let m;
  const rx = new RegExp(re.source, re.flags.includes('g') ? re.flags : re.flags + 'g');
  while ((m = rx.exec(s))) set.add(m[1].trim());
}
grab(/label:\s*'([^']*)'/g);
grab(/label:\s*"([^"]*)"/g);
grab(/placeholder:\s*'([^']*)'/g);
grab(/placeholder:\s*"([^"]*)"/g);
grab(/title:\s*'([^']*)'/g);
grab(/emptyMessage:\s*'([^']*)'/g);
grab(/pairLabel:\s*'([^']*)'/g);
grab(/buttonText:\s*'([^']*)'/g);
grab(/hint:\s*'([^']*)'/g);
grab(/notice:\s*'([^']*)'/g);
grab(/titleHint:\s*'([^']*)'/g);
const arr = [...set].filter((x) => /[\uAC00-\uD7A3]/.test(x) && x.length < 200);
arr.sort((a, b) => a.localeCompare(b, 'ko'));
console.log('count', arr.length);
fs.writeFileSync(path.join(root, 'tools/screen-labels-ko.txt'), arr.join('\n'), 'utf8');
