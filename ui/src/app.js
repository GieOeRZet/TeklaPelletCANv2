'use strict';
// TeklaPellet - strona WWW (ESP v2). Bez bibliotek. Dane: ui/gen/model.min.json (z data/device_model.json), zywe wartosci z /ws, nastawy z REST.
// Konwencje /ws: wartosci "live" w jednostkach wyswietlanych (temperatury w C), PID-y wyjsc (pompy, podajniki, mieszacze, ruszt) jako stan = mlodszy bajt SDO (0/1/2).
const ST = { token: null, mode: 'readonly', level: 0, vals: {}, model: null, screen: 'home', ws: null, wsOk: false, info: {}, listVals: {} };
const $ = (s, e = document) => e.querySelector(s);
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const IC = n => `assets/ic/${n}.png`;
const PUMP = [138, 139, 140, 141, 142, 143];
const num = p => (ST.vals[p] === undefined ? undefined : Number(ST.vals[p]));
const f1 = (x, d = 1) => (x === undefined || isNaN(x) ? '--' : Number(x).toFixed(d));
const BURNER = ['Wyłączony', 'Rozpalanie', 'Rozżarzanie', 'Moc 1', 'Moc 2', 'Modulacja', 'Wygaszanie', 'Czyszczenie', 'Podtrzymanie', 'Postój', 'Alarm'];
const SOLAR = ['Stop', 'Praca', 'Przepłukiwanie', 'Zamarzanie', 'Przegrzanie', 'Woda max', 'Wyłączone', 'Chłodzenie', 'Test', 'Alarm'];
const CHST = ['', 'Grzanie', 'Wybieg', 'Oczekiwanie', 'Stop', 'Ochrona kotła', 'Alarm', 'Lato', 'Źródło blokuje'];
const PROGICON = [133, 135, 122, 128, 120];                              // 0x2002 (4 gdy kociol wylaczony 0x2005=0)
// ikona paliwa: kropki wg % pojemnosci (progi 8,25,38,57,74,87 %) -> 0,2,5,9,14,19,24; 'E' gdy rezerwa>0 i paliwo<=rezerwa (z analizy panelu)
function fuelIcon(fuel, cap, res) {
  if (!cap) return { dots: 0, E: false };
  if (res > 0 && fuel <= res) return { dots: 0, E: true };
  const pm = Math.floor(fuel * 1000 / cap), th = [80, 250, 380, 570, 740, 870], dd = [2, 5, 9, 14, 19, 24];
  let d = 0; for (let i = 5; i >= 0; i--) if (pm >= th[i]) { d = dd[i]; break; } return { dots: d, E: false };
}
// komorki dolnych belek: {k:rodzaj,p:PID stanu,fr:klatki,n:liczba nad ikona}
const SCREENS = {
  kociol: { t: 'Kocioł', list: '0x00|KOCIOL|inst1', big: ['0x2001', '°C'], pill: '0x2004',
    kv: [['T. powrotu', '0x4000', '°C', 1, '0x350A'], ['Ciśnienie', '0x3333', 'mbar', 0, '0x350D'], ['Energia', '0x3628', 'kWh', 0]],
    bar: [{ k: 'pump', p: '0x2277' }, { k: 'anim', p: '0x216A', fr: [34, 35] }, { k: 'mixer', p: '0x216F' }] },
  palnik: { t: 'Palnik', list: '0x0A|PALNIK|inst1', big: ['0x2007', ''], state: BURNER,
    kv: [['Spalanie', '0x3710', 'kg/h', 1], ['Moc', '0x4005', '%', 0], ['Moc', '0x3706', 'kW', 1], ['Jasność', '0x2250', '', 0], ['T. palnika', '0x226C', '°C', 1], ['Obroty', '0x299C', '', 0]],
    bar: [{ k: 'anim', p: '0x2254', fr: [8, 9, 10], n: '0x2254' }, { k: 'anim', p: '0x2251', fr: [36, 37, 38], n: '0x226D', nd: 1 }, { k: 'anim', p: '0x2252', fr: [40, 41, 42] },
      { k: 'anim', p: '0x2253', fr: [65, 66, 67], n: '0x3711', hm: 1 }, { k: 'grate', p: '0x2276' }] },
  co1: { t: 'Ogrzewanie 1', list: '0x01|OGRZEWANIE|inst1', big: ['0x4001', '°C'], pill: '0x22F5', status: '0x22FD', kv: [['Pokojowa', '0x2016', '°C', 1]], bar: [{ k: 'pump', p: '0x2299' }, { k: 'mixer', p: '0x22B9' }] },
  co2: { t: 'Ogrzewanie 2', list: '0x02|OGRZEWANIE|inst1', big: ['0x4002', '°C'], pill: '0x22F6', status: '0x22FE', kv: [['Pokojowa', '0x2017', '°C', 1]], bar: [{ k: 'pump', p: '0x229A' }, { k: 'mixer', p: '0x22BA' }] },
  co3: { t: 'Ogrzewanie 3', list: '0x03|OGRZEWANIE|inst1', big: ['0x4003', '°C'], pill: '0x22F7', status: '0x22FF', kv: [['Pokojowa', '0x2018', '°C', 1]], bar: [{ k: 'pump', p: '0x229B' }, { k: 'mixer', p: '0x22BB' }] },
  co4: { t: 'Ogrzewanie 4', list: '0x04|OGRZEWANIE|inst1', big: ['0x4004', '°C'], pill: '0x22F8', status: '0x2300', kv: [['Pokojowa', '0x2019', '°C', 1]], bar: [{ k: 'pump', p: '0x229C' }, { k: 'mixer', p: '0x22BC' }] },
  cwu: { t: 'Ciepła woda', list: '0x05|CWU|inst1', big: ['0x2110', '°C'], pill: '0x201A', kv: [['Potrzebna', '0x2111', '°C', 0]], bar: [{ k: 'pump', p: '0x2278' }, { k: 'pump', p: '0x3749' }] },
  bufor: { t: 'Bufor', list: '0x07|BUFOR|inst1', big: ['0x2112', '°C'], kv: [['Dół', '0x2113', '°C', 1], ['Góra zadana', '0x2280', '°C', 0], ['Dół zadana', '0x2115', '°C', 0], ['T1', '0x3171', '°C', 1], ['T2', '0x3172', '°C', 1], ['T3', '0x3173', '°C', 1], ['Energia', '0x3174', '%', 0]], bar: [{ k: 'pump', p: '0x2277' }] },
  solary: { t: 'Solary', list: '0x06|SOLARY|inst1', big: ['0x2109', '°C'], state: SOLAR, stateP: '0x3120', kv: [['Temp. 2', '0x2102', '°C', 1], ['Temp. 3', '0x2103', '°C', 1], ['Moc', '0x2101', 'W', 0]], bar: [{ k: 'pump', p: '0x311C' }] },
  magazyn: { t: 'Magazyn paliwa', list: '0x09|MAGAZYN|inst1', kv: [['Silnik 1', '0x3522', '', 0], ['Silnik 2', '0x3523', '', 0], ['Czujnik poziomu', '0x3524', '', 0], ['Czujnik rezerwy', '0x352A', '', 0]], bar: [{ k: 'anim', p: '0x3522', fr: [36, 37, 38] }, { k: 'anim', p: '0x3523', fr: [40, 41, 42] }] },
  kaskada: { t: 'Kaskada', list: '0x08|KASKADA|inst1', kv: [['Poziom mocy', '0x3519', '', 0], ['Master', '0x3511', '', 0], ['Slave 1', '0x3512', '', 0], ['Slave 2', '0x3513', '', 0], ['Slave 3', '0x3514', '', 0]], bar: [] },
};
const NAV = [['home', 'Pulpit'], ['kociol', 'Kocioł'], ['palnik', 'Palnik'], ['co1', 'Ogrzewanie 1'], ['co2', 'Ogrzewanie 2'], ['co3', 'Ogrzewanie 3'], ['co4', 'Ogrzewanie 4'], ['cwu', 'Ciepła woda'], ['bufor', 'Bufor'], ['solary', 'Solary'], ['magazyn', 'Magazyn'], ['kaskada', 'Kaskada'], ['alarms', 'Alarmy'], ['config', 'Konfiguracja'], ['info', 'Informacje']];
const TABS = [['home', 'Pulpit', '⌂'], ['kociol', 'Kocioł', '♨'], ['palnik', 'Palnik', '♦'], ['cwu', 'Woda', '◍'], ['more', 'Więcej', '≡']];

// ---------- API
async function api(method, path, body) {
  const r = await fetch(path, { method, headers: { 'Content-Type': 'application/json', ...(ST.token ? { Authorization: 'Bearer ' + ST.token } : {}) }, body: body ? JSON.stringify(body) : undefined });
  let j = {}; try { j = await r.json(); } catch (e) { }
  if (r.status === 401) { ST.token = null; sessionStorage.removeItem('tp'); showLogin(); }
  return { status: r.status, ...j };
}
function toast(m) { const t = $('#toast'); t.textContent = m; t.classList.add('on'); clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove('on'), 2800); }
const ERR = { read_only_mode: 'Tryb „tylko odczyt” - zmień tryb, aby zapisywać.', blocked: 'Parametr zablokowany (poza listami panelu).', level_too_low: 'Wymagany wyższy poziom PIN.', out_of_range: 'Wartość poza zakresem.', not_writable: 'Parametr tylko do odczytu.', unknown_pid: 'Nieznany parametr.', no_encoding: 'Zapis tego parametru nie jest obsługiwany.', bad_password: 'Błędne hasło.', bad_pin: 'Błędny PIN.', locked: 'Zbyt wiele prób - spróbuj za chwilę.' };

// ---------- WebSocket (zywe dane)
function connectWs() {
  if (!ST.token) return; const u = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws';
  const ws = new WebSocket(u); ST.ws = ws;
  ws.onopen = () => { ws.send(JSON.stringify({ t: 'auth', token: ST.token })); ws.send(JSON.stringify({ t: 'sub', sets: ['home', ST.screen] })); };
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.t === 'hello') { ST.wsOk = true; renderTop(); } if (m.t === 'live') { Object.assign(ST.vals, m.v); draw(true); } if (m.t === 'param') { ST.listVals[m.pid] = m.v; if (ST.screen.startsWith('set:')) draw(); } };
  ws.onclose = () => { ST.wsOk = false; renderTop(); setTimeout(connectWs, 2000); };
}
function subscribe() { if (ST.ws && ST.ws.readyState === 1) ST.ws.send(JSON.stringify({ t: 'sub', sets: ['home', ST.screen] })); }

// ---------- widoki
function cellHtml(c) {
  const s = num(c.p) || 0; let img = '', n = '';
  if (c.k === 'pump') return `<div class="cell pump" title="Pompa" ${s ? '' : 'hidden'}><img data-fr="${PUMP.join(',')}" src="${IC(PUMP[0])}" alt="pompa"></div>`;
  if (c.k === 'mixer') return `<div class="cell" ${s ? '' : 'hidden'}><img class="mix" src="${IC(s === 1 ? 106 : 107)}" alt="${s === 1 ? 'zamykanie' : 'otwieranie'}"><small>mieszacz</small></div>`;
  if (c.k === 'grate') return `<div class="cell" ${s ? '' : 'hidden'}><img src="${IC(59)}" alt="ruszt"><small>${s === 1 ? '◀◀◀' : '▶▶▶'}</small></div>`;
  if (c.n) { const v = num(c.n); if (c.hm) { const m = Math.round(v || 0); n = Math.floor(m / 60) + ':' + String(m % 60).padStart(2, '0'); } else if (c.nd) n = f1(v, 1); else n = v ? String(Math.round(v)) : ''; }
  return `<div class="cell" ${s ? '' : 'hidden'}><small>${esc(n)}</small><img data-fr="${c.fr.join(',')}" src="${IC(c.fr[0])}" alt=""></div>`;
}
const bar = cells => cells && cells.length ? `<div class="bar" aria-label="Wyjścia">${cells.map(cellHtml).join('')}</div>` : '';
function homeView() {
  const t = num('0x2001'), set = num('0x2004'), bs = num('0x2007') || 0, on = num('0x2005') !== 0;
  const fuel = num('0x3601'), cap = num('0x3600'), res = num('0x3603') || 0, fi = fuelIcon(fuel || 0, cap || 0, res);
  const dots = Array.from({ length: 24 }, (_, i) => { const r = Math.floor(i / 4), c = i % 4; const idx = (5 - r) * 4 + c; return `<circle cx="${14 + c * 18}" cy="${16 + r * 14}" r="5" fill="${idx < fi.dots ? 'var(--acc2)' : 'transparent'}" stroke="var(--line)"/>`; }).join('');
  const led = (n, cls, a) => `<span class="${a ? 'on ' : ''}${cls}"><i></i>${n}</span>`;
  const prog = PROGICON[on ? Math.min(num('0x2002') || 0, 4) : 4];
  return `<div class="grid">
   <div class="card hero"><h3>Kocioł</h3><div class="big">${f1(t)}<small>°C</small></div><div class="st">${BURNER[bs] || '?'}</div><div><span class="pill">${f1(set, 0)}°C</span> <img src="${IC(prog)}" alt="program" width="35" height="37" style="vertical-align:middle"></div></div>
   <div class="card"><h3>Pogoda</h3><div class="val">${f1(num('0x215F'))}<small>°C</small></div><div class="kv" style="margin-top:8px"><div>Średnia<b>${f1(num('0x3721'))} °C</b></div><div>Pora roku<b><img src="${IC(num('0x3729') === 1 ? 180 : 181)}" alt="" width="26" height="14" style="vertical-align:middle"></b></div></div></div>
   <div class="card"><h3>Paliwo</h3><div class="fuel ${fi.E ? 'E' : ''}"><svg viewBox="0 0 84 104"><rect x="2" y="2" width="80" height="100" rx="8" fill="none" stroke="${fi.E ? 'var(--acc)' : 'var(--mut)'}" stroke-width="2"/>${dots}${fi.E ? '<text x="42" y="64" text-anchor="middle" font-size="40" fill="var(--acc)" font-weight="700">E</text>' : ''}</svg><div><div class="t">${f1(fuel, 0)} kg</div><div class="kv" style="margin-top:6px"><div>Pojemność<b>${f1(cap, 0)}</b></div></div></div></div></div>
   <div class="card wide"><h3>Diody</h3><div class="led">${led('Zasilanie', 'g', on)}${led('Palnik', 'y', bs >= 1 && bs <= 8)}${led('CO 1', 'w', num('0x2064') > 0)}${led('CO 2', 'w', num('0x2065') > 0)}${led('CWU', 'w', num('0x2014') > 0)}</div></div>
   <div class="card"><h3>Pompy</h3>${bar([{ k: 'pump', p: '0x2277' }, { k: 'pump', p: '0x2299' }, { k: 'pump', p: '0x2278' }])}</div>
   <div class="card"><h3>Moc palnika</h3><div class="val">${f1(num('0x4005'), 0)}<small>%</small></div></div>
  </div>`;
}
function screenView(id) {
  const s = SCREENS[id]; const big = s.big ? s.big : null; let bigHtml = '';
  if (big) { const v = num(big[0]); bigHtml = s.state ? `<div class="big" style="font-size:30px">${esc((s.state[num(s.stateP || big[0])] || '?'))}</div>` : `<div class="big">${f1(v)}<small>${big[1]}</small></div>`; }
  const st = s.status ? `<div class="st">${esc(CHST[num(s.status)] || '')}</div>` : (s.state && s.stateP ? '' : '');
  const kv = s.kv.filter(r => !r[4] || num(r[4])).map(r => `<div>${esc(r[0])}<b>${f1(num(r[1]), r[3])} ${r[2]}</b></div>`).join('');
  return `<div class="grid"><div class="card hero wide"><h3>${esc(s.t)}</h3>${bigHtml}${st}${s.pill ? `<div><span class="pill">${f1(num(s.pill), 0)}°C</span></div>` : ''}${bar(s.bar)}<div style="margin-top:12px"><button class="btn sec" data-go="set:${esc(s.list)}">Nastawy</button></div></div>
   <div class="card wide"><h3>Wartości</h3><div class="kv">${kv || '<div>Brak danych</div>'}</div></div></div>`;
}
function listView(key) {
  const rows = (ST.model.lists[key] || []); const title = key.split('|')[1] || key;
  return `<div class="card rows"><h3>Nastawy - ${esc(title)}</h3>${rows.map((r, i) => {
    const lock = ST.level < r.level_min; const v = ST.listVals[r.pid];
    return `<div class="row ${lock ? 'lock' : ''}" data-row="${i}"><div class="l">${esc(r.label_pl)}${lock ? ' 🔒' : ''}</div><div class="v">${v === undefined ? '…' : esc(v)}</div></div>`;
  }).join('')}</div>`;
}
function alarmsView() { return `<div class="card"><h3>Historia alarmów</h3><table id="alarms"><tr><th>Nr</th><th>Kod</th><th>Alarm</th><th>Stan</th></tr>${(ST.alarms || []).map(a => `<tr><td>${a.n}</td><td>${a.code}</td><td>${esc(a.text)}</td><td>${a.active ? 'AKTYWNY' : a.acked ? 'potwierdzony' : 'nowy'}</td></tr>`).join('') || '<tr><td colspan="4">Brak alarmów</td></tr>'}</table></div>`; }
function infoView() { const i = ST.info || {}; return `<div class="card"><h3>Urządzenie</h3><div class="kv">${Object.entries({ 'Firmware ESP': i.fw, 'Wersja strony': i.ui, 'Model (SHA)': (i.model || '').slice(0, 12), 'Węzeł CAN': i.node, 'CAN': i.can, 'Czas pracy [s]': i.uptime, 'Tryb': ST.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt', 'Poziom': ST.level }).map(([k, v]) => `<div>${k}<b>${esc(v ?? '--')}</b></div>`).join('')}</div></div>`; }
function configView() { return `<div class="card rows"><h3>Konfiguracja</h3><div class="row" data-go="set:0x10|KONFIGURACJA|inst1"><div class="l">Lista konfiguracji sterownika</div><div class="v">›</div></div><div class="row" data-act="mode"><div class="l">Tryb pracy ESP</div><div class="v">${ST.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt'}</div></div><div class="row" data-act="pin"><div class="l">Poziom PIN</div><div class="v">${ST.level}</div></div></div>`; }
function moreView() { return `<div class="card rows"><h3>Więcej</h3>${NAV.filter(n => !['home', 'kociol', 'palnik', 'cwu'].includes(n[0])).map(n => `<div class="row" data-go="${n[0]}"><div class="l">${n[1]}</div><div class="v">›</div></div>`).join('')}</div>`; }

function renderTop() {
  const t = $('#top'); const title = (NAV.find(n => n[0] === ST.screen) || [0, ST.screen.startsWith('set:') ? 'Nastawy' : ''])[1];
  t.innerHTML = `<h1>${esc(title)}</h1><span class="sp"></span><span class="chip ${ST.wsOk ? '' : 'bad'}" title="Połączenie"><i></i><span class="hid">${ST.wsOk ? 'na żywo' : 'offline'}</span></span><span class="chip ${ST.mode === 'full' ? 'warn' : ''}" data-act="mode"><i></i>${ST.mode === 'full' ? 'pełna' : 'odczyt'}</span><span class="chip" data-act="pin">PIN ${ST.level}</span>`;
  $('#side').innerHTML = `<div class="brand"><i></i><span>TeklaPellet</span></div>` + NAV.map(n => `<a data-go="${n[0]}" class="${ST.screen === n[0] ? 'on' : ''}"><span>${n[1]}</span></a>`).join('');
  $('#tabs').innerHTML = TABS.map(t => `<a data-go="${t[0]}" class="${ST.screen === t[0] ? 'on' : ''}"><b>${t[2]}</b>${t[1]}</a>`).join('');
}
let lastKey = '';
function draw(live) {
  if (!ST.model) return; const s = ST.screen; let html;
  if (s === 'home') html = homeView(); else if (SCREENS[s]) html = screenView(s); else if (s.startsWith('set:')) { if (live) return; html = listView(s.slice(4)); }
  else if (s === 'alarms') html = alarmsView(); else if (s === 'info') html = infoView(); else if (s === 'config') html = configView(); else html = moreView();
  const m = $('#main'); if (html !== lastKey) { const top = m.scrollTop; m.innerHTML = html; m.scrollTop = top; lastKey = html; }
}
async function go(s) {
  ST.screen = s; lastKey = ''; renderTop(); draw(); subscribe(); $('#main').scrollTop = 0;
  if (s.startsWith('set:')) { const r = await api('GET', '/api/params?list=' + encodeURIComponent(s.slice(4))); if (r.values) { Object.assign(ST.listVals, r.values); lastKey = ''; draw(); } }
  if (s === 'alarms') { const r = await api('GET', '/api/alarms'); ST.alarms = r.alarms || []; lastKey = ''; draw(); }
  if (s === 'info') { const r = await api('GET', '/api/info'); ST.info = r; lastKey = ''; draw(); }
}

// ---------- dialogi
const dlg = html => { const d = $('#dlg'); d.innerHTML = `<div class="box" role="dialog" aria-modal="true">${html}</div>`; d.hidden = false; return d; };
const closeDlg = () => { $('#dlg').hidden = true; };
function showLogin(msg) {
  $('#app').style.display = 'none'; closeDlg();
  const d = $('#dlg'); d.innerHTML = `<div class="box login"><h2>TeklaPellet</h2><div class="card" style="border:0;padding:0"><input id="pw" type="password" placeholder="Hasło" autocomplete="current-password" aria-label="Hasło"><div class="err" id="lerr">${msg || ''}</div><button class="btn" id="lok">Zaloguj</button></div></div>`; d.hidden = false;
  const go1 = async () => { const r = await fetch('/api/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password: $('#pw').value }) }); const j = await r.json().catch(() => ({})); if (j.token) { ST.token = j.token; ST.mode = j.mode || 'readonly'; ST.level = j.level || 0; sessionStorage.setItem('tp', JSON.stringify({ t: j.token })); start(); } else $('#lerr').textContent = ERR[j.code] || 'Błąd logowania'; };
  $('#lok').onclick = go1; $('#pw').onkeydown = e => { if (e.key === 'Enter') go1(); }; $('#pw').focus();
}
function askText(title, ph, cb) { const d = dlg(`<h2>${esc(title)}</h2><input id="inp" type="password" placeholder="${esc(ph)}" aria-label="${esc(ph)}"><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">OK</button></div>`); $('#dc').onclick = closeDlg; $('#dk').onclick = () => cb($('#inp').value, e => $('#derr').textContent = e); $('#inp').focus(); }
async function doPin() { askText('PIN poziomu', 'PIN', async (v, err) => { const r = await api('POST', '/api/level', { pin: v }); if (r.ok) { ST.level = r.level; closeDlg(); renderTop(); lastKey = ''; draw(); toast('Poziom ' + r.level); } else err(ERR[r.code] || 'Błąd'); }); }
async function doMode() { const to = ST.mode === 'full' ? 'readonly' : 'full'; askText(to === 'full' ? 'Włączyć pełną kontrolę?' : 'Przełączyć na tylko odczyt?', 'Hasło', async (v, err) => { const r = await api('POST', '/api/mode', { mode: to, password: v }); if (r.ok) { ST.mode = r.mode; closeDlg(); renderTop(); lastKey = ''; draw(); toast('Tryb: ' + (r.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt')); } else err(ERR[r.code] || 'Błąd'); }); }
function editParam(r) {
  if (ST.level < r.level_min) { toast('Wymagany PIN poziomu ' + r.level_min); return doPin(); }
  const pm = ST.model.pids[r.pid] || []; const sc = pm[5] || 1; const cur = Number(ST.listVals[r.pid]); const step = r.step || 1;
  const opts = r.options ? Object.keys(r.options).sort((a, b) => a - b).map(k => r.options[k]) : (r.options_dialog ? r.options_dialog.slice(1) : null);
  const save = async v => { const x = await api('PUT', '/api/param/' + r.pid, { value: v }); if (x.ok) { ST.listVals[r.pid] = x.v; closeDlg(); lastKey = ''; draw(); toast('Zapisano'); } else { $('#derr') && ($('#derr').textContent = ERR[x.code] || x.msg || 'Błąd'); } };
  if (r.dialog === 'radio' && opts) { dlg(`<h2>${esc(r.label_pl)}</h2>${opts.map((o, i) => `<div class="opt" data-opt="${i}"><input type="radio" ${i === cur ? 'checked' : ''}> ${esc(o)}</div>`).join('')}<div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button></div>`); $('#dc').onclick = closeDlg; document.querySelectorAll('#dlg .opt').forEach(el => el.onclick = () => save(Number(el.dataset.opt))); return; }
  if (r.kind === 'switch' || r.dialog === 'switch') { dlg(`<h2>${esc(r.label_pl)}</h2><div class="opt" id="sw"><input type="checkbox" ${cur ? 'checked' : ''}> Włączone</div><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">Zapisz</button></div>`); $('#dc').onclick = closeDlg; $('#dk').onclick = () => save($('#sw input').checked ? 1 : 0); return; }
  dlg(`<h2>${esc(r.label_pl)}</h2><div class="num" id="nv">${cur}</div><input type="range" id="rg" min="${r.min * sc}" max="${r.max * sc}" step="${step * sc}" value="${cur}" aria-label="${esc(r.label_pl)}"><div class="rng"><span>${r.min * sc}</span><span>${r.max * sc}</span></div><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">Zapisz</button></div>`);
  $('#rg').oninput = () => { $('#nv').textContent = $('#rg').value; }; $('#dc').onclick = closeDlg; $('#dk').onclick = () => save(Number($('#rg').value));
}

// ---------- zdarzenia i start
document.addEventListener('click', e => {
  const a = e.target.closest('[data-go]'); if (a) { if (!$('#dlg').hidden && a.closest('#dlg')) return; return go(a.dataset.go); }
  const c = e.target.closest('[data-act]'); if (c) return c.dataset.act === 'mode' ? doMode() : doPin();
  const r = e.target.closest('[data-row]'); if (r && ST.screen.startsWith('set:')) return editParam(ST.model.lists[ST.screen.slice(4)][Number(r.dataset.row)]);
});
let fr = 0; setInterval(() => { fr++; document.querySelectorAll('img[data-fr]').forEach(im => { const a = im.dataset.fr.split(','); im.src = IC(a[fr % a.length]); }); }, 130);
async function start() {
  closeDlg(); $('#app').style.display = '';
  if (!ST.model) ST.model = await (await fetch('model.min.json')).json();
  const inf = await api('GET', '/api/info'); ST.info = inf; if (inf.mode) ST.mode = inf.mode; if (inf.level !== undefined) ST.level = inf.level;
  const s = await api('GET', '/api/state'); if (s.v) Object.assign(ST.vals, s.v);
  connectWs(); renderTop(); go(location.hash.slice(1) || 'home');
}
window.addEventListener('hashchange', () => { const h = location.hash.slice(1); if (h && h !== ST.screen) go(h); });
(function init() { const k = sessionStorage.getItem('tp'); if (k) { try { ST.token = JSON.parse(k).t; } catch (e) { } } if (ST.token) start(); else showLogin(); })();
