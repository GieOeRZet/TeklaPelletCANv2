'use strict';
// TeklaPellet - strona WWW v0.2 (ESP v2). Bez bibliotek. Dane: gen/app.json (menu, ekrany, konfiguracja, harmonogramy), model.min.json (listy nastaw, PID-y), zywe wartosci z /ws.
// Konwencje /ws: wartosci "live" w jednostkach wyswietlanych; PID-y wyjsc (pompy, podajniki, mieszacze, ruszt) jako stan = mlodszy bajt SDO (0/1/2).
const ST = { token: null, mode: 'readonly', level: 0, vals: {}, model: null, app: null, screen: 'home', ws: null, wsOk: false, info: {}, listVals: {}, sched: {}, schedDay: 0 };
const $ = (s, e = document) => e.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const IC = n => `assets/ic/${n}.png`;
const num = p => (ST.vals[p] === undefined ? undefined : Number(ST.vals[p]));
const f1 = (x, d = 1) => (x === undefined || x === null || isNaN(x) ? '--' : Number(x).toFixed(d));
const PROGICON = [133, 135, 122, 128, 120];                              // ikona programu kotla 0x2002 (4 gdy kociol wylaczony 0x2005=0)
function fuelIcon(fuel, cap, res) {                                      // kropki wg % pojemnosci (progi 8,25,38,57,74,87 %); 'E' gdy rezerwa>0 i paliwo<=rezerwa
  if (!cap) return { dots: 0, E: false }; if (res > 0 && fuel <= res) return { dots: 0, E: true };
  const pm = Math.floor(fuel * 1000 / cap), th = [80, 250, 380, 570, 740, 870], dd = [2, 5, 9, 14, 19, 24]; let d = 0; for (let i = 5; i >= 0; i--) if (pm >= th[i]) { d = dd[i]; break; } return { dots: d, E: false };
}
const ROUTE = { KOCIOL: 'kociol', PALNIK: 'palnik', CWU: 'cwu', SOLARY: 'solary', BUFOR: 'bufor', KASKADA: 'kaskada', MAGAZYN: 'magazyn', ALARMY: 'alarms', INFO: 'info', KONFIGURACJA: 'config', TEST_WYJSC: 'outtest', STATYSTYKI: 'stats', SIEC: 'network', INTERNET: 'module', DLG_PIN: 'pin' };
const routeOf = r => r.target === 'OGRZEWANIE' ? 'co' + r.title.slice(-1) : ROUTE[r.target];
const condOk = c => c.every(x => { const v = num(x.pid); if (v === undefined) return x.op === '!=' ? false : false; return x.op === '>=' ? v >= x.val : x.op === '!=' ? v !== x.val : v === x.val; });
function navItems() { return ST.app.menu.filter(r => routeOf(r) && r.level_min <= ST.level && condOk(r.cond)).map(r => ({ id: routeOf(r), t: r.title, icon: r.icon_sel, lv: r.level_min })).concat([{ id: 'module', t: 'Moduł ESP', icon: 101 }]).filter((x, i, a) => a.findIndex(y => y.id === x.id) === i); }
const TABS = [['home', 'Pulpit', '⌂'], ['circuits', 'Obwody', '▥'], ['sched:co1', 'Harmonogram', '▦'], ['more', 'Menu', '≡']];

// ---------- API
async function api(method, path, body) {
  const r = await fetch(path, { method, headers: { 'Content-Type': 'application/json', ...(ST.token ? { Authorization: 'Bearer ' + ST.token } : {}) }, body: body ? JSON.stringify(body) : undefined });
  let j = {}; try { j = await r.json(); } catch (e) { }
  if (r.status === 401 && path !== '/api/login') { ST.token = null; sessionStorage.removeItem('tp'); showLogin(); }
  return { status: r.status, ...j };
}
function toast(m) { const t = $('#toast'); t.textContent = m; t.classList.add('on'); clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove('on'), 2800); }
const ERR = { read_only_mode: 'Tryb „tylko odczyt” - zmień tryb, aby zapisywać.', blocked: 'Parametr zablokowany (poza listami panelu).', level_too_low: 'Wymagany wyższy poziom PIN.', out_of_range: 'Wartość poza zakresem.', not_writable: 'Parametr tylko do odczytu.', unknown_pid: 'Nieznany parametr.', no_encoding: 'Zapis tego parametru nie jest obsługiwany.', bad_password: 'Błędne hasło.', bad_pin: 'Błędny PIN.', locked: 'Zbyt wiele prób - spróbuj za chwilę.', can_timeout: 'Brak odpowiedzi kotła.', verify_failed: 'Odczyt zwrotny nie potwierdził zapisu.', bad_signature: 'Błędny podpis pliku.', bad_pack: 'Uszkodzona paczka UI.' };
async function writeParam(pid, value) { const x = await api('PUT', '/api/param/' + pid, { value }); if (x.ok) { ST.listVals[pid] = x.v; ST.vals[pid] = x.v; } return x; }

// ---------- WebSocket
function connectWs() {
  if (!ST.token) return; const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws'); ST.ws = ws;
  ws.onopen = () => { ws.send(JSON.stringify({ t: 'auth', token: ST.token })); subscribe(); };
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.t === 'hello') { ST.wsOk = true; renderTop(); } if (m.t === 'live') { Object.assign(ST.vals, m.v); draw(true); if (ST.screen === 'home') renderTop(); } if (m.t === 'param') { ST.listVals[m.pid] = m.v; ST.vals[m.pid] = m.v; if (!ST.screen.startsWith('set:') && ST.screen !== 'config') draw(true); } };
  ws.onclose = () => { ST.wsOk = false; renderTop(); setTimeout(connectWs, 2000); };
}
function subscribe() { if (ST.ws && ST.ws.readyState === 1) ST.ws.send(JSON.stringify({ t: 'sub', sets: ['home', ST.screen.split(':')[0]] })); }

// ---------- widoki
const bar = cells => cells && cells.length ? `<div class="bar" aria-label="Wyjścia">${cells.map(cellHtml).join('')}</div>` : '';
function cellHtml(c) {
  const s = num(c.p) || 0, hid = s ? '' : 'hidden';
  if (c.k === 'pump') return `<div class="cell pump" ${hid}><small></small><span class="ib"><img data-fr="${ST.app.pump_frames.join(',')}" src="${IC(ST.app.pump_frames[0])}" alt="pompa"></span></div>`;
  if (c.k === 'mixer') return `<div class="cell" ${hid}><small>mieszacz</small><span class="ib"><img class="mix" src="${IC(s === 1 ? 106 : 107)}" alt="${s === 1 ? 'zamykanie' : 'otwieranie'}"></span></div>`;
  if (c.k === 'grate') return `<div class="cell" ${hid}><small>${s === 1 ? '◀◀◀' : '▶▶▶'}</small><span class="ib"><img src="${IC(59)}" alt="ruszt"></span></div>`;
  let n = ''; if (c.n) { const v = num(c.n); if (c.hm) { const m = Math.round(v || 0); n = Math.floor(m / 60) + ':' + String(m % 60).padStart(2, '0'); } else if (c.nd) n = f1(v, 1); else n = v ? String(Math.round(v)) : ''; }
  return `<div class="cell" ${hid}><small>${esc(n)}</small><span class="ib"><img data-fr="${c.fr.join(',')}" src="${IC(c.fr[0])}" alt=""></span></div>`;
}
const boilerImg = () => `<img class="boiler" src="${IC(ST.app.boiler_images[String(num('0x370A') || 2)] || 12)}" alt="kocioł">`;
const modeLabel = v => (ST.app.program_modes.find(m => m.v === Math.round(v)) || {}).label || '--';
const modeIcon = v => (ST.app.program_modes.find(m => m.v === Math.round(v)) || {}).icon;
// delikatny wskaznik wyjscia: mala kropka (swieci kolorem funkcji gdy aktywne, inaczej ledwie widoczna) + drobna etykieta. Mieszacz: O (otwieranie, czerwony) / Z (zamykanie, niebieski).
const PC = { on: '#4fe38a', burn: '#ffb066', open: '#ff5d5d', close: '#5ab4ff' };
function ix(label, val, on, col, extra) { return `<span class="ix ${on ? 'on' : ''}" style="--c:${col}"><i></i>${esc(label)}${val ? ` <em>${esc(val)}</em>` : ''}</span>`; }
function barPill(c, id) {
  const v = num(c.p) || 0, on = v !== 0;
  if (c.k === 'mixer') return ix('Mieszacz', v === 1 ? 'Z' : v === 2 ? 'O' : '', on, v === 1 ? PC.close : PC.open);
  if (c.k === 'pump') return ix(c.l || 'Pompa', '', on, PC.on);
  if (c.k === 'grate') return ix('Ruszt', v === 1 ? '◀' : on ? '▶' : '', on, PC.on);
  let val = ''; if (c.n && on) { const x = num(c.n) || 0; if (c.hm) { const m = Math.round(x); val = Math.floor(m / 60) + ':' + String(m % 60).padStart(2, '0'); } else if (c.nd) val = f1(x, 1) + ' s'; else if (x) val = Math.round(x) + '%'; }
  return ix(c.l, val, on, id === 'palnik' ? PC.burn : PC.on);
}
function hopper(fi, w = 40, h = 46) {
  // zasobnik z lejkiem: siatka 4 x 6 kropek zapalanych od dolu (progi wg promili pojemnosci); przy rezerwie litera E
  const dots = Array.from({ length: 24 }, (_, i) => { const r = Math.floor(i / 4), c = i % 4, idx = (5 - r) * 4 + c; const lit = idx < fi.dots; return `<circle cx="${11 + c * 6}" cy="${8 + r * 5}" r="${lit ? 1.9 : 1.5}" fill="${lit ? '#ffb066' : '#2b2b33'}"${lit ? ' style="filter:drop-shadow(0 0 2px #ffb066aa)"' : ''}/>`; }).join('');
  return `<svg viewBox="0 0 40 46" width="${w}" height="${h}" aria-hidden="true"><path d="M4 2H36V30L25 42H15L4 30Z" fill="#17171c" stroke="#7d7d8a" stroke-width="1.5" stroke-linejoin="round"/>${fi.E ? '<text x="20" y="25" text-anchor="middle" font-size="18" font-weight="800" fill="#ffb066">E</text>' : dots}</svg>`;
}
function tileTitle(id, sc) { return /^co\d$/.test(id) ? 'CO ' + id.slice(2) : id === 'cwu' ? 'CWU' : sc.t; }
function tile(id) {
  const sc = ST.app.screens[id]; if (!sc) return '';
  const act = (/^co\d$/.test(id) || id === 'cwu') && (num(sc.mode) || 0) > 0;
  const mi = sc.mode ? modeIcon(num(sc.mode)) : null;
  const big = sc.state ? `<span style="font-size:20px">${ST.app.status[sc.state][num(sc.stateP || sc.big[0])] || '?'}</span>` : sc.big ? `${f1(num(sc.big[0]))}<small>${sc.big[1] === '°C' ? '°' : sc.big[1]}${sc.pill ? ' / ' + f1(num(sc.pill), 0) + '°' : ''}</small>` : '';
  const pills = sc.bar.map(c => barPill(c, id)).join('');
  return `<button class="tl" data-go="${id}"><i class="led ${act ? 'w' : ''}"></i><div class="th"><h3>${tileTitle(id, sc)}</h3>${mi ? `<img src="${IC(mi)}" alt="">` : ''}</div><div class="tv">${big}</div><div class="ixs">${pills}</div></button>`;
}
const visibleIds = () => ST.app.menu.filter(r => condOk(r.cond) && r.level_min <= ST.level).map(routeOf).filter(r => r && ST.app.screens[r] && r !== 'kociol' && r !== 'palnik');
function arcPath(cx, cy, r, a0, a1) { const f = a => [cx + r * Math.cos(a * Math.PI / 180), cy + r * Math.sin(a * Math.PI / 180)]; const [x0, y0] = f(a0), [x1, y1] = f(a1); return `M${x0.toFixed(1)} ${y0.toFixed(1)}A${r} ${r} 0 ${a1 - a0 > 180 ? 1 : 0} 1 ${x1.toFixed(1)} ${y1.toFixed(1)}`; }
function homeView() {
  // Pulpit: karta kotla (luk z temperatura, metryki, delikatne wskazniki wyjsc palnika) + siatka kompaktowych kafli bez przewijania (CO, CWU, inne wg konfiguracji, Paliwo, Zasilanie).
  const t = num('0x2001'), set = num('0x2004'), bs = num('0x2007') || 0, on = num('0x2005') !== 0;
  const prog = PROGICON[on ? Math.min(num('0x2002') || 0, 4) : 4];
  const fuel = num('0x3601'), cap = num('0x3600'), res = num('0x3603') || 0, fi = fuelIcon(fuel || 0, cap || 0, res);
  const run = bs >= 1 && bs <= 8, frac = Math.max(0, Math.min(1, ((t ?? 20) - 20) / 70)), a1 = 135 + 270 * frac;
  const m = (l, v, show = true) => show ? `<div>${l}<b>${v}</b></div>` : '';
  const metrics = m('Moc palnika', f1(num('0x4005'), 0) + ' %') + m('Spalanie', f1(num('0x3710'), 1) + ' kg/h') + m('Powrót', f1(num('0x4000'), 1) + ' °C', (num('0x350A') || 0) !== 0) + m('Ciśnienie', f1(num('0x3333'), 0) + ' mbar', (num('0x350D') || 0) !== 0) + m('Temp. palnika', f1(num('0x226C'), 0) + ' °C');
  const kp = [...ST.app.screens.palnik.bar.map(c => barPill(c, 'palnik')), ...ST.app.screens.kociol.bar.filter(c => c.k !== 'anim').map(c => barPill(c, 'kociol'))].join('');
  const fuelTile = `<button class="tl" data-fuel="1"><i class="led ${fi.E ? 'o' : ''}"></i><div class="th"><h3>Paliwo</h3></div><div class="tv fl">${hopper(fi)}<span>${f1(fuel, 0)}<small>kg</small></span></div><div class="ixs">${ix(fi.E ? 'rezerwa' : 'z ' + f1(cap, 0) + ' kg', '', fi.E, PC.burn)}</div></button>`;
  const powerTile = `<button class="tl" data-go="kociol"><i class="led ${on ? 'g' : ''}"></i><div class="th"><h3>Zasilanie</h3><img src="${IC(prog)}" alt=""></div><div class="tv sm">${on ? 'Włączone' : 'Wyłączone'}</div><div class="ixs">${ix('Kocioł', '', on, PC.on)}</div></button>`;
  return `<div class="dash"><div class="kcard"><i class="led ${run ? 'y' : ''}"></i>
   <button class="gwrap" data-go="kociol" aria-label="Kocioł"><svg viewBox="0 0 140 140" class="arc" aria-hidden="true"><defs><linearGradient id="ag" x1="0" y1="1" x2="1" y2="0"><stop offset="0" stop-color="#ffc233"/><stop offset="1" stop-color="#ff7a1a"/></linearGradient></defs>
     <path d="${arcPath(70, 70, 58, 135, 405)}" fill="none" stroke="var(--line)" stroke-width="8" stroke-linecap="round"/><path d="${arcPath(70, 70, 58, 135, Math.max(136, a1))}" fill="none" stroke="url(#ag)" stroke-width="8" stroke-linecap="round" class="arcv"/></svg>
     <div class="gc"><div class="gv">${f1(t)}<small>°C</small></div><div class="gs">${ST.app.status.burner[bs] || '?'}</div><div class="gm"><img src="${IC(prog)}" alt="">zadana ${f1(set, 0)} °C</div></div></button>
   <div class="kv gk">${metrics}</div><div class="ixs kx">${kp}</div></div>
   <div class="tg">${visibleIds().map(tile).join('')}${fuelTile}${powerTile}</div></div>`;
}
function circuitsView() { const ids = visibleIds().filter(i => /^co\d$/.test(i) || i === 'cwu'); return `<div class="tg">${ids.map(tile).join('') || '<div class="card">Brak skonfigurowanych obwodów</div>'}</div>`; }
function fuelDialog() {
  const fuel = num('0x3601') || 0, cap = num('0x3600') || 0, res = num('0x3603') || 0, fi = fuelIcon(fuel, cap, res);
  dlg(`<h2>Paliwo</h2><div style="display:flex;align-items:center;justify-content:center;gap:14px;margin:6px 0">${hopper(fi, 76, 87)}<div class="num" style="margin:0">${f1(fuel, 0)}<small style="font-size:16px"> kg</small></div></div>
   <input type="range" min="0" max="${cap || 100}" value="${fuel}" disabled aria-label="Poziom paliwa"><div class="rng"><span>0</span><span>${f1(cap, 0)} kg</span></div>
   <div class="r2"><span class="k">rezerwa</span><b>${f1(res, 0)} kg</b></div>
   <p class="k" style="margin-top:10px">Zmiana poziomu paliwa z aplikacji nie jest jeszcze obsługiwana: panel nie ma zmierzonego kodowania zapisu dla tego parametru (<code>0x3601</code>).</p>
   <div class="acts"><button class="btn sec" id="dc">Zamknij</button></div>`); $('#dc').onclick = closeDlg;
}
function modeChips(s) {
  if (!s.mode) return ''; const cur = num(s.mode);
  return `<div class="chips" role="group" aria-label="Tryb">${ST.app.program_modes.map(m => `<button class="chipb ${cur === m.v ? 'on' : ''}" data-mode="${s.mode}|${m.v}"><img src="${IC(m.icon)}" alt="" width="22" height="22">${m.label}</button>`).join('')}</div>`;
}
function screenView(id) {
  const s = ST.app.screens[id]; let bigHtml = '';
  if (s.big) { const v = num(s.big[0]); bigHtml = s.state ? `<div class="big" style="font-size:30px">${esc(ST.app.status[s.state][num(s.stateP || s.big[0])] || '?')}</div>` : `<div class="big">${f1(v)}<small>${s.big[1]}</small></div>`; }
  const st = s.status ? `<div class="st">${esc(ST.app.status.ch[num(s.status)] || '')}</div>` : '';
  const kv = s.kv.filter(r => !r[4] || num(r[4])).map(r => `<div>${esc(r[0])}<b>${f1(num(r[1]), r[3])} ${r[2]}</b></div>`).join('');
  return `<div class="grid"><div class="card hero wide"><h3>${esc(s.t)}</h3>${s.image ? boilerImg() : ''}${bigHtml}${st}${s.pill ? `<div><span class="pill">${f1(num(s.pill), 0)}°C</span></div>` : ''}${modeChips(s)}${bar(s.bar)}
    <div class="acts"><button class="btn sec" data-go="set:${esc(s.list)}">Nastawy</button>${s.sched ? `<button class="btn sec" data-go="sched:${s.sched}">Harmonogram</button>` : ''}</div></div>
   <div class="card wide"><h3>Wartości</h3><div class="kv">${kv || '<div>Brak danych</div>'}</div></div></div>`;
}
function listView(key) {
  const rows = (ST.model.lists[key] || []); const title = key.split('|')[1] || key;
  return `<div class="card rows"><h3>Nastawy - ${esc(title)}</h3>${rows.map((r, i) => {
    if (!r.pid || !r.label_pl) return ''; const lock = ST.level < r.level_min; const v = ST.listVals[r.pid];
    return `<div class="row ${lock ? 'lock' : ''}" data-row="${i}"><div class="l">${esc(r.label_pl)}${lock ? ' 🔒' : ''}</div><div class="v">${v === undefined ? '…' : esc(optLabel(r, v))}</div></div>`;
  }).join('')}</div>`;
}
function optLabel(r, v) { const o = r.options ? r.options[String(v)] : (r.options_dialog ? r.options_dialog[Number(v) + 1] : null); return o ?? v; }
function configView() {
  const rows = ST.app.config.filter(r => r.level_min <= ST.level);
  return `<div class="card rows"><h3>Konfiguracja kotła</h3>${rows.map((r, i) => `<div class="row" data-cfg="${i}"><div class="l">${esc(r.label)}</div><div class="v">${esc(cfgLabel(r, num(r.pid)))}</div></div>`).join('') || '<div class="row"><div class="l">Wymagany PIN poziomu 1</div></div>'}</div>
  <div class="card rows" style="margin-top:12px"><h3>Dostęp</h3><div class="row" data-act="mode"><div class="l">Tryb pracy ESP</div><div class="v">${ST.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt'}</div></div><div class="row" data-act="pin"><div class="l">Poziom PIN</div><div class="v">${ST.level}</div></div></div>`;
}
function cfgLabel(r, v) { if (v === undefined) return '…'; if (r.options) { const o = r.options.find(x => x.v === v); return o ? o.label : v; } return v; }
function schedView(id) {
  const c = ST.app.schedules.circuits.find(x => x.id === id); const d = ST.schedDay, pid = c.pids[ST.app.schedules.days[d]], mask = ST.sched[pid];
  const cells = Array.from({ length: 24 }, (_, h) => { const on = mask !== undefined && ((mask >> h) & 1); return `<button class="hr ${on ? 'on' : ''}" data-hour="${h}" aria-label="${h}:00 ${on ? 'komfort' : 'eko'}"><span>${h}</span></button>`; }).join('');
  return `<div class="card"><h3>Harmonogram - ${esc(c.title)}</h3><div class="chips">${ST.app.schedules.days.map((n, i) => `<button class="chipb ${i === d ? 'on' : ''}" data-day="${i}">${n}</button>`).join('')}</div>
   <div class="day">${ST.app.schedules.day_names[d]}</div><div class="hours">${cells}</div><div class="legend"><span class="lg on"></span> komfort <span class="lg"></span> eko</div>
   <div class="acts"><button class="btn sec" data-copy="1">Kopiuj na wszystkie dni</button><button class="btn" data-save="${pid}">Zapisz</button></div></div>`;
}
function statsView() { const rows = ST.model.lists['0x11|STATYSTYKI|inst1'] || []; return `<div class="card rows"><h3>Statystyki</h3>${rows.map(r => r.pid && r.label_pl ? `<div class="row"><div class="l">${esc(r.label_pl)}</div><div class="v">${esc(ST.listVals[r.pid] ?? '…')}</div></div>` : '').join('')}</div>`; }
function networkView() { return `<div class="card"><h3>Sieć CAN</h3><table><tr><th>Węzeł</th><th>Nazwa</th><th>Stan</th></tr>${(ST.net || []).map(n => `<tr><td>${n.node}</td><td>${esc(n.name)}</td><td>${esc(n.state)}${n.age_s != null ? ' (' + n.age_s + ' s temu)' : ''}</td></tr>`).join('') || '<tr><td colspan="3">Brak danych</td></tr>'}</table></div>`; }
function outtestView() { const rows = [['Pompa kotłowa', '0x2277'], ['Pompa CWU', '0x2278'], ['Pompa CO1', '0x2299'], ['Podajnik zasobnika', '0x2251'], ['Podajnik palnika', '0x2252'], ['Zapalarka', '0x2253'], ['Dmuchawa', '0x2254'], ['Ruszt', '0x2276'], ['Mieszacz CO1', '0x22B9'], ['Mieszacz kotła', '0x216F']]; return `<div class="card rows"><h3>Test wyjść - podgląd stanów</h3>${rows.map(r => `<div class="row"><div class="l">${r[0]}</div><div class="v">${['wyłączone', 'włączone', 'włączone (2)'][num(r[1]) || 0]}</div></div>`).join('')}<div class="row"><div class="l" style="color:var(--mut)">Zapis testów wyjść jest wyłączony (wymaga osobnej zgody właściciela).</div></div></div>`; }
function alarmsView() { return `<div class="card"><h3>Historia alarmów</h3><table id="alarms"><tr><th>Nr</th><th>Kod</th><th>Alarm</th><th>Stan</th></tr>${(ST.alarms || []).map(a => `<tr><td>${a.n}</td><td>${a.code}</td><td>${esc(a.text)}</td><td>${a.active ? 'AKTYWNY' : a.acked ? 'potwierdzony' : 'nowy'}</td></tr>`).join('') || '<tr><td colspan="4">Brak alarmów</td></tr>'}</table></div>`; }
function infoView() { const i = ST.info || {}; return `<div class="card"><h3>Informacje o programie</h3><div class="kv">${Object.entries({ 'Firmware ESP': i.fw, 'Wersja strony': i.ui, 'Węzeł CAN': i.node, 'CAN': i.can, 'Czas pracy [s]': i.uptime, 'Tryb': ST.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt', 'Poziom': ST.level }).map(([k, v]) => `<div>${k}<b>${esc(v ?? '--')}</b></div>`).join('')}</div></div>`; }
function moduleView() {
  const w = ST.wifi || {};
  return `<div class="card rows"><h3>Moduł ESP</h3>
   <div class="row" data-act="wifi"><div class="l">Wi-Fi</div><div class="v">${esc(w.ssid || '--')} ${w.connected ? '●' : '○'}</div></div>
   <div class="row" data-act="password"><div class="l">Hasło urządzenia</div><div class="v">zmień ›</div></div>
   <div class="row" data-act="ota"><div class="l">Aktualizacja oprogramowania</div><div class="v">wgraj ›</div></div>
   <div class="row" data-go="log"><div class="l">Dziennik</div><div class="v">›</div></div>
   <div class="row" data-go="network"><div class="l">Sieć CAN (moduły)</div><div class="v">›</div></div>
   <div class="row" data-act="restart"><div class="l">Restart modułu</div><div class="v">›</div></div>
   <div class="row" data-act="factory"><div class="l">Reset fabryczny</div><div class="v">›</div></div></div>`;
}
function logView() { return `<div class="card"><h3>Dziennik</h3><pre class="log">${esc((ST.log || []).join('\n') || 'Brak wpisów')}</pre></div>`; }
function moreView() {
  if (ST.level >= 1 && ST.beep === undefined) { ST.beep = null; api('GET', '/api/params?pids=0x3506').then(r => { ST.beep = r.values ? Number(r.values['0x3506'] || 0) : 0; lastKey = ''; draw(); }); }
  const items = navItems().filter(n => !['home', 'kociol', 'palnik', 'cwu', 'pin', 'co1', 'co2', 'co3', 'co4'].includes(n.id) || n.id === 'kociol' || n.id === 'palnik');
  const row = n => `<div class="row" data-go="${n.id}"><div class="l"><img src="${IC(n.icon)}" width="22" height="22" alt="" style="vertical-align:middle;margin-right:8px">${esc(n.t)}</div><div class="v">›</div></div>`;
  const sep = t => `<div class="sep"><span>${t}</span></div>`;
  const NAMES = { 1: 'POZIOM 1 — INSTALATOR', 2: 'POZIOM 2', 3: 'POZIOM 3' };
  let html = `<div class="card rows"><h3>Dostęp</h3><div class="row" data-act="mode"><div class="l">Tryb pracy modułu</div><div class="v">${ST.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt'}</div></div><div class="row" data-act="pin"><div class="l">Poziom PIN</div><div class="v">${ST.level}</div></div></div><div class="card rows" style="margin-top:12px"><h3>Menu</h3>`;
  for (let lv = 0; lv <= 3; lv++) { const g = items.filter(n => (n.lv || 0) === lv); if (!g.length) continue; if (lv >= 1) html += sep(NAMES[lv]); html += g.map(row).join(''); }
  html += `${ST.level >= 1 ? `<div class="row" data-act="beep"><div class="l">Alarm dźwiękowy</div><div class="v">${ST.beep ? 'włączony' : 'wyłączony'}</div></div>` : ''}${row({ id: 'module', t: 'Moduł ESP', icon: 101 })}</div>`;
  return html;
}

function renderTop() {
  const t = $('#top'); const nav = ST.app ? navItems() : []; const cur = nav.find(n => n.id === ST.screen); const title = cur ? cur.t : ({ home: 'Pulpit', more: 'Więcej', log: 'Dziennik' }[ST.screen] || (ST.screen.startsWith('set:') ? 'Nastawy' : ST.screen.startsWith('sched:') ? 'Harmonogram' : ''));
  $('#app').classList.toggle('home', ST.screen === 'home');
  const now = new Date(), hh = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0'), dd = now.toLocaleDateString('pl-PL', { weekday: 'short', day: 'numeric', month: 'short' });
    if (ST.screen === 'home') {                          // AppBar wg przewodnika: status polaczenia + nazwa, pogoda, zegar i data, ikona alarmu (tylko gdy aktywny)
    const wx = num('0x215F'), al = (ST.alarms || []).find(x => x.active);
    t.innerHTML = `<div class="ab"><button class="conn" data-go="module"><i class="cd ${ST.wsOk ? 'on' : ''}"></i>${esc((ST.info && ST.info.name) || 'TeklaPellet')}</button><span class="sp"></span>${wx !== undefined ? `<div class="wx"><img src="${IC(num('0x3729') === 1 ? 180 : 181)}" alt="pora roku"><div><b>${f1(wx)}°</b><small>${f1(num('0x3721'))}°</small></div></div>` : ''}<div class="clk"><b>${hh}</b><small>${esc(dd)}</small></div>${al ? `<button class="alb" data-go="alarms" aria-label="Alarm"><img src="${IC(78)}" alt=""></button>` : ''}</div>`;
  } else t.innerHTML = `<div class="tt"><h1>${esc(title)}</h1>${ST.screen === 'home' ? `<span class="date">${esc(dd)} · ${hh}</span>` : ''}</div>` + `<span class="sp"></span><span class="chip ${ST.wsOk ? '' : 'bad'}" title="Połączenie"><i></i><span class="hid">${ST.wsOk ? 'na żywo' : 'offline'}</span></span><span class="chip ${ST.mode === 'full' ? 'warn' : ''}" data-act="mode"><i></i>${ST.mode === 'full' ? '🔓 Pełna kontrola' : '🔒 Tylko odczyt'}</span><span class="chip" data-act="pin">PIN ${ST.level}</span>`;
  $('#side').innerHTML = `<div class="brand"><i></i><span>TeklaPellet</span></div><a data-go="home" class="${ST.screen === 'home' ? 'on' : ''}"><span>Pulpit</span></a>` + nav.filter(n => n.id !== 'pin').map(n => `<a data-go="${n.id}" class="${ST.screen === n.id ? 'on' : ''}"><img src="${IC(n.icon)}" width="22" height="22" alt=""><span>${esc(n.t)}</span></a>`).join('');
  $('#tabs').innerHTML = TABS.map(x => `<a data-go="${x[0]}" class="${ST.screen === x[0] ? 'on' : ''}"><b>${x[2]}${x[0] === 'alarms' && (ST.alarms || []).length ? `<em class="bdg">${ST.alarms.length}</em>` : ''}</b>${x[1]}</a>`).join('');
}
let lastKey = '';
function draw(live) {
  if (!ST.model || !ST.app) return; const s = ST.screen; let html;
  if (s === 'home') html = homeView(); else if (s === 'circuits') html = circuitsView(); else if (ST.app.screens[s]) html = screenView(s); else if (s.startsWith('set:')) { if (live) return; html = listView(s.slice(4)); }
  else if (s.startsWith('sched:')) { if (live) return; html = schedView(s.slice(6)); } else if (s === 'config') { if (live) return; html = configView(); } else if (s === 'alarms') html = alarmsView(); else if (s === 'info') html = infoView();
  else if (s === 'module') html = moduleView(); else if (s === 'log') html = logView(); else if (s === 'stats') html = statsView(); else if (s === 'network') html = networkView(); else if (s === 'outtest') html = outtestView(); else html = moreView();
  const m = $('#main'); if (html !== lastKey) { const top = m.scrollTop; m.innerHTML = html; m.scrollTop = top; lastKey = html; }
}
async function go(s) {
  ST.screen = s; lastKey = ''; renderTop(); draw(); subscribe(); $('#main').scrollTop = 0;
  if (s.startsWith('set:')) { const r = await api('GET', '/api/params?list=' + encodeURIComponent(s.slice(4))); if (r.values) { Object.assign(ST.listVals, r.values); lastKey = ''; draw(); } }
  if (s === 'stats') { const r = await api('GET', '/api/params?list=' + encodeURIComponent('0x11|STATYSTYKI|inst1')); if (r.values) { Object.assign(ST.listVals, r.values); lastKey = ''; draw(); } }
  if (s.startsWith('sched:')) { const c = ST.app.schedules.circuits.find(x => x.id === s.slice(6)); const r = await api('GET', '/api/params?pids=' + Object.values(c.pids).join(',')); if (r.values) { Object.assign(ST.sched, Object.fromEntries(Object.entries(r.values).map(([k, v]) => [k, Number(v)]))); lastKey = ''; draw(); } }
  if (s === 'config') { const r = await api('GET', '/api/params?pids=' + ST.app.config.map(x => x.pid).join(',')); if (r.values) { Object.assign(ST.vals, r.values); lastKey = ''; draw(); } }
  if (s === 'home' || s === 'alarms') { const r = await api('GET', '/api/alarms'); ST.alarms = r.alarms || []; lastKey = ''; draw(); }
  if (s === 'info') { const r = await api('GET', '/api/info'); ST.info = r; lastKey = ''; draw(); }
  if (s === 'network') { const r = await api('GET', '/api/network'); ST.net = r.nodes || []; lastKey = ''; draw(); }
  if (s === 'module') { const r = await api('GET', '/api/wifi'); ST.wifi = r; lastKey = ''; draw(); }
  if (s === 'log') { const r = await api('GET', '/api/log'); ST.log = r.lines || []; lastKey = ''; draw(); }
}

// ---------- dialogi
const dlg = html => { const d = $('#dlg'); d.innerHTML = `<div class="box" role="dialog" aria-modal="true">${html}</div>`; d.hidden = false; return d; };
const closeDlg = () => { $('#dlg').hidden = true; };
function showLogin(msg) {
  $('#app').style.display = 'none'; const d = $('#dlg'); d.innerHTML = `<div class="box login"><h2>TeklaPellet</h2><div class="card" style="border:0;padding:0"><input id="pw" type="password" placeholder="Hasło" autocomplete="current-password" aria-label="Hasło"><div class="err" id="lerr">${msg || ''}</div><button class="btn" id="lok">Zaloguj</button></div></div>`; d.hidden = false;
  const go1 = async () => { const r = await fetch('/api/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password: $('#pw').value }) }); const j = await r.json().catch(() => ({})); if (j.token) { ST.token = j.token; ST.mode = j.mode || 'readonly'; ST.level = j.level || 0; sessionStorage.setItem('tp', JSON.stringify({ t: j.token })); start(); } else $('#lerr').textContent = ERR[j.code] || 'Błąd logowania'; };
  $('#lok').onclick = go1; $('#pw').onkeydown = e => { if (e.key === 'Enter') go1(); }; $('#pw').focus();
}
function askText(title, ph, cb, type = 'password') { dlg(`<h2>${esc(title)}</h2><input id="inp" type="${type}" placeholder="${esc(ph)}" aria-label="${esc(ph)}"><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">OK</button></div>`); $('#dc').onclick = closeDlg; $('#dk').onclick = () => cb($('#inp').value, e => $('#derr').textContent = e); $('#inp').focus(); }
async function doPin() { askText('PIN poziomu', 'PIN', async (v, err) => { const r = await api('POST', '/api/level', { pin: v }); if (r.ok) { ST.level = r.level; closeDlg(); renderTop(); lastKey = ''; draw(); toast('Poziom ' + r.level); } else err(ERR[r.code] || 'Błąd'); }); }
async function doMode() { const to = ST.mode === 'full' ? 'readonly' : 'full'; askText(to === 'full' ? 'Włączyć pełną kontrolę?' : 'Przełączyć na tylko odczyt?', 'Hasło', async (v, err) => { const r = await api('POST', '/api/mode', { mode: to, password: v }); if (r.ok) { ST.mode = r.mode; closeDlg(); renderTop(); lastKey = ''; draw(); toast('Tryb: ' + (r.mode === 'full' ? 'pełna kontrola' : 'tylko odczyt')); } else err(ERR[r.code] || 'Błąd'); }); }
function choose(title, opts, cur, onPick) { dlg(`<h2>${esc(title)}</h2>${opts.map((o, i) => `<div class="opt" data-opt="${i}"><input type="radio" ${o.v === cur ? 'checked' : ''}> ${esc(o.label)}</div>`).join('')}<div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button></div>`); $('#dc').onclick = closeDlg; document.querySelectorAll('#dlg .opt').forEach(el => el.onclick = () => onPick(opts[Number(el.dataset.opt)].v)); }
function editParam(r) {
  if (ST.level < r.level_min) { toast('Wymagany PIN poziomu ' + r.level_min); return doPin(); }
  const pm = ST.model.pids[r.pid] || []; const sc = pm[5] || 1; const cur = Number(ST.listVals[r.pid]); const step = r.step || 1;
  const opts = r.options ? Object.keys(r.options).sort((a, b) => a - b).map(k => ({ v: Number(k), label: r.options[k] ?? (k === '0' ? 'Wyłączone' : 'Włączone') })) : (r.options_dialog ? r.options_dialog.slice(1).map((l, i) => ({ v: i, label: l })) : null);
  const save = async v => { const x = await writeParam(r.pid, v); if (x.ok) { closeDlg(); lastKey = ''; draw(); toast('Zapisano'); } else { $('#derr') && ($('#derr').textContent = ERR[x.code] || x.msg || 'Błąd'); } };
  if (r.dialog === 'radio' && opts) return choose(r.label_pl, opts, cur, save);
  if (r.kind === 'switch' || r.dialog === 'switch') { dlg(`<h2>${esc(r.label_pl)}</h2><div class="opt" id="sw"><input type="checkbox" ${cur ? 'checked' : ''}> Włączone</div><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">Zapisz</button></div>`); $('#dc').onclick = closeDlg; $('#dk').onclick = () => save($('#sw input').checked ? 1 : 0); return; }
  dlg(`<h2>${esc(r.label_pl)}</h2><div class="num" id="nv">${cur}</div><input type="range" id="rg" min="${r.min * sc}" max="${r.max * sc}" step="${step * sc}" value="${cur}" aria-label="${esc(r.label_pl)}"><div class="rng"><span>${r.min * sc}</span><span>${r.max * sc}</span></div><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">Zapisz</button></div>`);
  $('#rg').oninput = () => { $('#nv').textContent = $('#rg').value; }; $('#dc').onclick = closeDlg; $('#dk').onclick = () => save(Number($('#rg').value));
}
function editCfg(r) {
  const cur = num(r.pid); const save = async v => { const x = await writeParam(r.pid, v); if (x.ok) { closeDlg(); lastKey = ''; renderTop(); draw(); toast('Zapisano'); } else { $('#derr') && ($('#derr').textContent = ERR[x.code] || 'Błąd'); } };
  if (r.options) return choose(r.label, r.options, cur, save);
  dlg(`<h2>${esc(r.label)}</h2><div class="num" id="nv">${cur ?? ''}</div><input type="range" id="rg" min="${r.min}" max="${r.max}" step="1" value="${cur ?? r.default}"><div class="rng"><span>${r.min}</span><span>${r.max}</span></div><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">Zapisz</button></div>`);
  $('#rg').oninput = () => { $('#nv').textContent = $('#rg').value; }; $('#dc').onclick = closeDlg; $('#dk').onclick = () => save(Number($('#rg').value));
}
async function moduleAct(a) {
  if (a === 'beep') { const x = await writeParam('0x3506', ST.beep ? 0 : 1); if (x.ok) { ST.beep = x.v ? 1 : 0; lastKey = ''; draw(); toast('Zapisano'); } else toast(ERR[x.code] || 'Błąd'); return; }
  if (a === 'wifi') { const r = await api('GET', '/api/wifi/scan'); const nets = r.networks || []; dlg(`<h2>Wi-Fi</h2>${nets.map((n, i) => `<div class="opt" data-n="${i}">${esc(n.ssid)} <span style="margin-left:auto;color:var(--mut)">${n.rssi} dBm</span></div>`).join('') || '<div class="opt">Brak sieci</div>'}<div class="acts"><button class="btn sec" id="dc">Zamknij</button></div>`); $('#dc').onclick = closeDlg;
    document.querySelectorAll('#dlg .opt[data-n]').forEach(el => el.onclick = () => askText('Hasło sieci ' + nets[Number(el.dataset.n)].ssid, 'Hasło Wi-Fi', async (v, err) => { const x = await api('POST', '/api/wifi', { ssid: nets[Number(el.dataset.n)].ssid, pass: v }); if (x.ok) { closeDlg(); toast('Zapisano, moduł połączy się z siecią'); } else err(ERR[x.code] || 'Błąd'); })); }
  if (a === 'password') askText('Nowe hasło (min. 8 znaków)', 'Nowe hasło', async (v, err) => { const x = await api('POST', '/api/password', { new: v }); if (x.ok) { closeDlg(); toast('Hasło zmienione'); } else err(ERR[x.code] || 'Błąd'); });
  if (a === 'ota') { dlg(`<h2>Aktualizacja oprogramowania</h2><p style="color:var(--mut);margin-bottom:10px">Wybierz plik podpisany (firmware lub paczka UI). Wymaga trybu „pełna kontrola”.</p><input type="file" id="fw"><div class="err" id="derr"></div><div class="acts"><button class="btn sec" id="dc">Anuluj</button><button class="btn" id="dk">Wgraj</button></div>`); $('#dc').onclick = closeDlg;
    $('#dk').onclick = async () => { const f = $('#fw').files[0]; if (!f) return; const kind = /ui/i.test(f.name) ? 'ui' : 'fw'; const r = await fetch('/api/ota/' + kind, { method: 'POST', headers: { Authorization: 'Bearer ' + ST.token }, body: f }); const j = await r.json().catch(() => ({})); if (j.ok) { closeDlg(); toast(kind === 'ui' ? 'UI wgrane - odśwież stronę' : 'Firmware wgrany, moduł restartuje'); if (kind === 'ui') api('POST', '/api/ui/confirm'); } else $('#derr').textContent = ERR[j.code] || j.code || 'Błąd'; }; }
  if (a === 'restart') askText('Restart modułu - podaj hasło', 'Hasło', async (v, err) => { const x = await api('POST', '/api/restart', { password: v }); if (x.ok) { closeDlg(); toast('Restart…'); } else err(ERR[x.code] || 'Błąd'); });
  if (a === 'factory') askText('Reset fabryczny kasuje hasło, sieć i ustawienia modułu. Podaj hasło', 'Hasło', async (v, err) => { const x = await api('POST', '/api/factory_reset', { password: v }); if (x.ok) { closeDlg(); toast('Reset wykonany'); } else err(ERR[x.code] || 'Błąd'); });
}
// ---------- zdarzenia
document.addEventListener('click', async e => {
  const fu = e.target.closest('[data-fuel]'); if (fu) return fuelDialog();
  const a = e.target.closest('[data-go]'); if (a) { if (!$('#dlg').hidden && a.closest('#dlg')) return; if (a.dataset.go === 'pin') return doPin(); return go(a.dataset.go); }
  const c = e.target.closest('[data-act]'); if (c) { const k = c.dataset.act; return k === 'mode' ? doMode() : k === 'pin' ? doPin() : moduleAct(k); }
  const r = e.target.closest('[data-row]'); if (r && ST.screen.startsWith('set:')) return editParam(ST.model.lists[ST.screen.slice(4)][Number(r.dataset.row)]);
  const cf = e.target.closest('[data-cfg]'); if (cf) return editCfg(ST.app.config.filter(x => x.level_min <= ST.level)[Number(cf.dataset.cfg)]);
  const md = e.target.closest('[data-mode]'); if (md) { const [pid, v] = md.dataset.mode.split('|'); const x = await writeParam(pid, Number(v)); if (x.ok) { draw(); toast('Zapisano'); } else toast(ERR[x.code] || 'Błąd'); return; }
  const dy = e.target.closest('[data-day]'); if (dy) { ST.schedDay = Number(dy.dataset.day); const c = ST.app.schedules.circuits.find(x => x.id === ST.screen.slice(6)); const pid = c.pids[ST.app.schedules.days[ST.schedDay]]; if (ST.sched[pid] === undefined) { const r2 = await api('GET', '/api/params?pids=' + pid); if (r2.values) ST.sched[pid] = Number(r2.values[pid]); } lastKey = ''; return draw(); }
  const hr = e.target.closest('[data-hour]'); if (hr) { const c = ST.app.schedules.circuits.find(x => x.id === ST.screen.slice(6)); const pid = c.pids[ST.app.schedules.days[ST.schedDay]]; ST.sched[pid] = (ST.sched[pid] ?? 0) ^ (1 << Number(hr.dataset.hour)); lastKey = ''; return draw(); }
  const cp = e.target.closest('[data-copy]'); if (cp) { const c = ST.app.schedules.circuits.find(x => x.id === ST.screen.slice(6)); const m = ST.sched[c.pids[ST.app.schedules.days[ST.schedDay]]]; for (const d of ST.app.schedules.days) ST.sched[c.pids[d]] = m; toast('Skopiowano na wszystkie dni (zapisz każdy dzień)'); return; }
  const sv = e.target.closest('[data-save]'); if (sv) { if (ST.level < 1) { toast('Wymagany PIN poziomu 1'); return doPin(); } const x = await writeParam(sv.dataset.save, ST.sched[sv.dataset.save]); toast(x.ok ? 'Zapisano' : (ERR[x.code] || 'Błąd')); return; }
});
let fr = 0; setInterval(() => { fr++; document.querySelectorAll('img[data-fr]').forEach(im => { const a = im.dataset.fr.split(','); im.src = IC(a[fr % a.length]); }); }, 130);
async function start() {
  closeDlg(); $('#app').style.display = '';
  if (!ST.model) ST.model = await (await fetch('model.min.json')).json(); if (!ST.app) ST.app = await (await fetch('gen/app.json')).json();
  const inf = await api('GET', '/api/info'); ST.info = inf; if (inf.mode) ST.mode = inf.mode; if (inf.level !== undefined) ST.level = inf.level;
  const s = await api('GET', '/api/state'); if (s.v) Object.assign(ST.vals, s.v);
  connectWs(); renderTop(); go(location.hash.slice(1) || 'home');
}
window.addEventListener('hashchange', () => { const h = location.hash.slice(1); if (h && h !== ST.screen) go(h); });
(function init() { const k = sessionStorage.getItem('tp'); if (k) { try { ST.token = JSON.parse(k).t; } catch (e) { } } if (ST.token) start(); else showLogin(); })();
