/**
 * fxhj.soonpay.co.kr 로그인 후 메인 페이지 HTML 저장
 * - 메뉴 구조 분석용 참고 자료 수집 (우리 PG 사이트 메뉴 구성을 동일하게 하기 위함)
 *
 * 사용법:
 *   cd d:\Delopment\PG\scripts
 *   node fxhj-login-save-menu.js
 *
 * 환경변수(선택):
 *   FXHJ_ID=fxhj
 *   FXHJ_PW=adminfx!@
 *   FXHJ_OTP=254303
 *
 * OTP는 로그인 시점에 바뀌므로, 프롬프트로 입력받거나 환경변수로 전달.
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE = 'https://fxhj.soonpay.co.kr';
const MAIN_URL = BASE + '/main';
const OUT_DIR = path.join(__dirname, '..', 'output');
const OUT_HTML = path.join(OUT_DIR, 'fxhj-main-after-login.html');

async function main() {
  const id = process.env.FXHJ_ID || 'fxhj';
  const pw = process.env.FXHJ_PW || 'adminfx!@';
  let otp = process.env.FXHJ_OTP || '';

  console.log('브라우저 실행 중...');
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    const loginUrl = BASE + '/login';
    console.log('로그인 페이지 이동:', loginUrl);
    await page.goto(loginUrl, { waitUntil: 'networkidle', timeout: 30000 });

    await page.waitForSelector('input[name="memberId"], input[name="userId"], input#memberId, input[name="id"], input[type="text"]', { timeout: 10000 }).catch(() => null);
    const idSel = 'input[name="memberId"], input[name="userId"], input#memberId, input[name="id"]';
    const idInput = await page.$(idSel);
    if (idInput) await idInput.fill(id);

    const pwSel = 'input[name="memberPassword"], input[name="password"], input#memberPassword, input[type="password"]';
    const pwInput = await page.$(pwSel);
    if (pwInput) await pwInput.fill(pw);

    if (otp) {
      const otpSel = 'input[name="otp"], input#otp, input[placeholder*="OTP"], input[name="otpNo"]';
      const otpInput = await page.$(otpSel);
      if (otpInput) await otpInput.fill(otp);
    }

    const loginBtn = 'button:has-text("로그인"), input[type="submit"][value*="로그인"], button[type="submit"], a:has-text("로그인")';
    await page.click(loginBtn).catch(() => null);
    await page.waitForTimeout(2000);

    if (!otp) {
      const otpSel = 'input[name="otp"], input#otp, input[placeholder*="OTP"], input[name="otpNo"]';
      const otpInput = await page.$(otpSel);
      if (otpInput) {
        console.log('OTP 입력란이 보입니다. OTP를 입력한 뒤 브라우저에서 로그인 버튼을 눌러 주세요. (30초 대기)');
        await page.waitForTimeout(30000);
      }
    } else {
      await page.waitForTimeout(3000);
    }

    const url = page.url();
    if (url.includes('/main') || url.includes('/index') || !url.includes('/login')) {
      console.log('로그인 성공으로 추정. 메인 HTML 저장 중...');
      if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });
      const html = await page.content();
      fs.writeFileSync(OUT_HTML, html, 'utf8');
      console.log('저장 경로:', OUT_HTML);
    } else {
      console.log('현재 URL:', url);
      const html = await page.content();
      if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });
      fs.writeFileSync(OUT_HTML, html, 'utf8');
      console.log('현재 페이지 HTML도 저장함:', OUT_HTML);
    }
  } catch (e) {
    console.error(e);
  } finally {
    await browser.close();
  }
}

main();
