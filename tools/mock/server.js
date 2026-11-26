// Serwer-atrapa ESP v2: te same endpointy co firmware (docs/ESP_API_V2.md) + symulacja danych. Tylko do testow strony. Haslo demo: "demo"; PIN-y demo: 1111/2222/3333 (poziomy 1/2/3).
const http = require('http'), fs = require('fs'), path = require('path');
const { WebSocketServer } = require('ws');
const ROOT = path.join(__dirname, '..', '..');
const PORT = Number(process.env.PORT || 8099);
const model = JSON.parse(fs.readFileSync(path.join(ROOT, 'ui', 'gen', 'model.min.json'), 'utf8'));
const dm = JSON.parse(fs.readFileSync(path.join(ROOT, 'data', 'device_model.json'), 'utf8'));
const BLOCK = new Set(((dm.firmware_row_table || {}).rows_not_in_any_panel_list || []).filter(h => h.pid).map(h => h.pid.toLowerCase()));
const PW = {}; for (const [p, v] of Object.entries(dm.pids.items)) PW[p.toLowerCase()] = v;
const LEVEL = {}; for (const rows of Object.values(model.lists)) for (const r of rows) if (r.pid && r.level_min != null) LEVEL[r.pid.toLowerCase()] = Math.min(LEVEL[r.pid.toLowerCase()] ?? 9, r.level_min);
const sessions = new Map(); const fails = new Map();
const S = { mode: 'readonly' };
const settings = {}; for (const rows of Object.values(model.lists)) for (const r of rows) if (r.pid) settings[r.pid.toLowerCase()] = r.default ?? 0;
let tick = 0;
function live() {                                  // symulacja: cykl pracy palnika co ~60 s
  tick++; const ph = Math.floor(tick / 8) % 8, bs = [0, 1, 2, 5, 5, 6, 7, 0][ph], run = bs >= 1 && bs <= 8;
  const w = (a, b) => a + Math.sin(tick / 6) * b;
  return { '0x2001': +w(62, 3).toFixed(1), '0x2004': 60, '0x2005': 1, '0x2002': 0, '0x2007': bs, '0x4000': +w(41, 1).toFixed(1), '0x350A': 1, '0x350D': 1, '0x3333': 1500, '0x3628': 12345,
    '0x215F': -3.5, '0x3721': -2.9, '0x3729': 0, '0x3600': 135, '0x3601': 21, '0x3603': 0, '0x2064': 1, '0x2065': 0, '0x2014': 1, '0x4005': run ? 30 : 0, '0x3706': run ? 5.2 : 0, '0x3710': run ? 1.4 : 0,
    '0x2250': run ? 100 : 0, '0x226C': run ? 31 : 0, '0x299C': run ? 52 : 0, '0x2277': 1, '0x2299': 1, '0x2278': 1, '0x22B9': ph === 2 ? 2 : ph === 5 ? 1 : 0, '0x216F': 0,
    '0x2254': run ? 1 : 0, '0x2251': run && tick % 6 < 3 ? 1 : 0, '0x226D': 2.1, '0x2252': bs === 1 || bs === 5 ? 1 : 0, '0x2253': bs === 1 ? 1 : 0, '0x2276': bs === 7 ? 1 : 0, '0x3711': 180, '0x2216': 0,
    '0x4001': +w(48, 2).toFixed(1), '0x22F5': 45, '0x22FD': 1, '0x2016': 21.5, '0x2110': +w(53, 1).toFixed(1), '0x201A': 55, '0x2111': 50, '0x2112': 55, '0x2113': 41, '0x2280': 60, '0x2115': 50, '0x3171': 56, '0x3172': 48, '0x3173': 41, '0x3174': 63,
    '0x2109': 30, '0x2102': 28, '0x2103': 26, '0x2101': 0, '0x3120': 0, '0x311C': 0, '0x3522': 0, '0x3523': 0, '0x3524': 1, '0x352A': 0 };
}
const types = { html: 'text/html; charset=utf-8', js: 'text/javascript', css: 'text/css', png: 'image/png', json: 'application/json' };
function serve(res, f) { fs.readFile(f, (e, d) => { if (e) { res.writeHead(404); return res.end('404'); } res.writeHead(200, { 'Content-Type': types[path.extname(f).slice(1)] || 'application/octet-stream' }); res.end(d); }); }
const json = (res, code, o) => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(o)); };
function body(req) { return new Promise(r => { let b = ''; req.on('data', c => b += c); req.on('end', () => { try { r(JSON.parse(b || '{}')); } catch (e) { r({}); } }); }); }
// strazniki zapisu = lustro tekla_core/guard.hpp
function plan(mode, level, pidStr, v) {
  const p = pidStr.toLowerCase(), e = PW[p];
  if (mode !== 'full') return { code: 'read_only_mode' }; if (!e) return { code: 'unknown_pid' }; if (BLOCK.has(p)) return { code: 'blocked' };
  if (!['setting', 'command'].includes(e.mode)) return { code: 'not_writable' }; if (!e.panel_write) return { code: 'no_encoding' };
  if (level < (LEVEL[p] ?? 3)) return { code: 'level_too_low' };
  const sc = (model.pids[pidStr] || [])[5] || 1; const raw = Math.floor(v / sc + 0.5); if (raw < e.min || raw > e.max) return { code: 'out_of_range' };
  return { ok: true, raw, v: raw * sc };
}
const srv = http.createServer(async (req, res) => {
  const u = new URL(req.url, 'http://x'); const p = u.pathname;
  if (!p.startsWith('/api/')) {
    if (p === '/' || p === '/index.html') return serve(res, path.join(ROOT, 'ui', 'src', 'index.html'));
    if (p === '/model.min.json') return serve(res, path.join(ROOT, 'ui', 'gen', 'model.min.json'));
    if (p.startsWith('/assets/')) return serve(res, path.join(ROOT, 'ui', p));
    return serve(res, path.join(ROOT, 'ui', 'src', path.basename(p)));
  }
  const ip = req.socket.remoteAddress;
  if (p === '/api/login' && req.method === 'POST') {
    const b = await body(req), f = fails.get(ip) || { n: 0, t: 0 }; if (f.n >= 5 && Date.now() - f.t < 60000) return json(res, 429, { ok: false, code: 'locked' });
    if (b.password !== 'demo') { fails.set(ip, { n: f.n + 1, t: Date.now() }); return json(res, 401, { ok: false, code: 'bad_password' }); }
    fails.delete(ip); const t = Math.random().toString(36).slice(2) + Date.now().toString(36); sessions.set(t, { level: 0 }); return json(res, 200, { ok: true, token: t, mode: S.mode, level: 0 });
  }
  const tok = (req.headers.authorization || '').replace('Bearer ', ''), ses = sessions.get(tok);
  if (!ses) return json(res, 401, { ok: false, code: 'unauthorized' });
  if (p === '/api/info') return json(res, 200, { ok: true, fw: 'mock-0.1', ui: '0.1', model: dm.firmware_sha256, node: 95, can: 'mock', uptime: Math.floor(process.uptime()), mode: S.mode, level: ses.level });
  if (p === '/api/state') return json(res, 200, { ok: true, v: live() });
  if (p === '/api/params') { const rows = model.lists[u.searchParams.get('list')] || []; const values = {}; for (const r of rows) if (r.pid) values[r.pid] = settings[r.pid.toLowerCase()]; return json(res, 200, { ok: true, values }); }
  if (p === '/api/alarms') return json(res, 200, { ok: true, alarms: [{ n: 1, code: 12, text: 'Przykładowy alarm (atrapa)', active: false, acked: true }] });
  if (p === '/api/mode' && req.method === 'POST') { const b = await body(req); if (b.password !== 'demo') return json(res, 403, { ok: false, code: 'bad_password' }); S.mode = b.mode === 'full' ? 'full' : 'readonly'; return json(res, 200, { ok: true, mode: S.mode }); }
  if (p === '/api/level' && req.method === 'POST') { const b = await body(req), lv = { '1111': 1, '2222': 2, '3333': 3 }[b.pin]; if (!lv) return json(res, 403, { ok: false, code: 'bad_pin' }); ses.level = lv; return json(res, 200, { ok: true, level: lv }); }
  const m = p.match(/^\/api\/param\/(0x[0-9a-fA-F]+)$/);
  if (m && req.method === 'PUT') { const b = await body(req), r = plan(S.mode, ses.level, m[1], Number(b.value)); if (!r.ok) return json(res, r.code === 'level_too_low' ? 403 : 409, { ok: false, code: r.code }); settings[m[1].toLowerCase()] = r.v; wss.clients.forEach(c => c.readyState === 1 && c.send(JSON.stringify({ t: 'param', pid: m[1], v: r.v }))); return json(res, 200, { ok: true, v: r.v }); }
  json(res, 404, { ok: false, code: 'not_found' });
});
const wss = new WebSocketServer({ server: srv, path: '/ws' });
wss.on('connection', ws => { let ok = false; ws.on('message', d => { const m = JSON.parse(d); if (m.t === 'auth') { ok = sessions.has(m.token); ws.send(JSON.stringify(ok ? { t: 'hello' } : { t: 'err' })); if (ok) ws.send(JSON.stringify({ t: 'live', v: live() })); } }); });
setInterval(() => { const v = live(); wss.clients.forEach(c => c.readyState === 1 && c.send(JSON.stringify({ t: 'live', v }))); }, 1000);
srv.listen(PORT, () => console.log('mock http://localhost:' + PORT));
