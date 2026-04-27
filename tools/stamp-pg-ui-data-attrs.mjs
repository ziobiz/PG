/**
 * Stamp data-pg-ui-t / data-pg-ui-title on <th> in screens.js so PG_UI_I18N.applyDom can translate after render.
 * Only touches <th> that contain Hangul and do not already have data-pg-ui-t.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const file = path.join(__dirname, '../site/js/screens.js');
let s = fs.readFileSync(file, 'utf8');
const reKo = /[\uAC00-\uD7A3]/;

function escAttr(val) {
  return String(val).replace(/\\/g, '\\\\').replace(/"/g, '&quot;');
}

let n = 0;
s = s.replace(/<th([^>]*)>([\s\S]*?)<\/th>/gi, function (full, attrs, inner) {
  if (/data-pg-ui-t\s*=/.test(attrs)) return full;
  if (!reKo.test(inner)) return full;
  if (/L\s*\(|escA\s*\(|escUi\s*\(|\+'/.test(inner)) return full;
  if (/<br\s*\/?>/i.test(inner)) return full;
  if (/<[^b]/i.test(inner.replace(/<br\s*\/?>/gi, ''))) return full;
  var text = inner
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  if (!text || !reKo.test(text)) return full;
  var titleM = attrs.match(/\btitle\s*=\s*"([^"]*)"/i);
  var titleKo = titleM ? titleM[1] : '';
  var newAttrs = attrs;
  if (titleKo && reKo.test(titleKo) && !/data-pg-ui-title\s*=/.test(attrs)) {
    newAttrs = newAttrs.replace(/\btitle\s*=\s*"[^"]*"/i, '');
    newAttrs += ' data-pg-ui-title="' + escAttr(titleKo) + '" title="' + escAttr(titleKo) + '"';
  }
  newAttrs += ' data-pg-ui-t="' + escAttr(text) + '"';
  n++;
  return '<th' + newAttrs + '>' + inner + '</th>';
});

fs.writeFileSync(file, s, 'utf8');
console.log('stamped th count', n);
