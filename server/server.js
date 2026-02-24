/**
 * PG Admin - Express server: static site, mock login API, optional API stubs for menus
 */
var path = require('path');
var express = require('express');

var app = express();
var PORT = process.env.PORT || 3000;
var SITE_DIR = path.join(__dirname, '..', 'site');

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Static files from site/
app.use(express.static(SITE_DIR));

// 루트(/) 및 /main 접속 시 index.html (fxhj 동일)
app.get('/', function (req, res) {
  res.sendFile(path.join(SITE_DIR, 'index.html'));
});
app.get('/main', function (req, res) {
  res.sendFile(path.join(SITE_DIR, 'index.html'));
});

// Mock login: POST /api/login -> 200 + Set-Cookie or token
app.post('/api/login', function (req, res) {
  var userId = (req.body && req.body.userId) || (req.body && req.body.username) || '';
  var userPwd = req.body && req.body.userPwd || req.body && req.body.password || '';
  if (!userId || !userPwd) {
    return res.status(400).json({ success: false, message: '아이디와 비밀번호를 입력하세요.' });
  }
  res.cookie('pg_admin_token', 'mock-' + userId, { httpOnly: false, maxAge: 24 * 60 * 60 * 1000 });
  res.status(200).json({ success: true, token: 'mock-' + userId, user: { userId: userId, userNm: '관리자' } });
});

// Optional API stubs: return empty or mock array for grid data
var apiStubs = [
  '/api/system/noticeList',
  '/api/comp/myCompMng',
  '/api/comp/compMngTree',
  '/api/commission/commisionList',
  '/api/comp/compInfoHistList',
  '/api/calc/payList',
  '/api/calc/payListNew',
  '/api/calc/payFailList',
  '/api/calc/offsetCancList',
  '/api/pay/easyPay',
  '/api/calc/cashReceiptList',
  '/api/calc/calcList',
  '/api/calc/calcGmList',
  '/api/calc/compPointMngList',
  '/api/calc/balcInfo',
  '/api/calc/exCalcList',
  '/api/pay/payHoldList',
  '/api/noti/notiUrlMng',
  '/api/noti/notiSendMngList',
  '/api/noti/notiCashReceiptUrlMng',
  '/api/noti/notiCashReceiptSendMngList',
  '/api/user/userMng',
  '/api/set/gridSetMng'
];

apiStubs.forEach(function (route) {
  app.get(route, function (req, res) {
    res.json({ list: [], totalCount: 0 });
  });
});

// SPA fallback: serve index.html for non-file requests under /
app.get('*', function (req, res, next) {
  if (req.path.indexOf('.') !== -1) return next();
  res.sendFile(path.join(SITE_DIR, 'index.html'));
});

app.listen(PORT, function () {
  console.log('PG Admin server at http://localhost:' + PORT + ' (static: site/)');
});
