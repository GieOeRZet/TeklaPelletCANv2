// Test e2e strony przeciw serwerowi-atrapie: node tools/mock/e2e.js (serwer musi dzialac na :8099). Zrzuty -> /tmp/tp_shots
const { chromium } = require('playwright'); const fs = require('fs');
const URL = 'http://localhost:8099/'; fs.mkdirSync('/tmp/tp_shots', { recursive: true });
let fails = 0; const ok = (c, m) => { if (!c) { console.log('FAIL', m); fails++; } else console.log('ok  ', m); };
(async () => {
  const b = await chromium.launch(); const errs = [];
  const reset = async () => { const r = await (await fetch(URL + 'api/login', { method: 'POST', body: JSON.stringify({ password: 'demo' }) })).json(); await fetch(URL + 'api/mode', { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + r.token }, body: JSON.stringify({ mode: 'readonly', password: 'demo' }) }); };   // tryb jest globalny dla urzadzenia
  for (const [name, vp] of [['phone', { width: 412, height: 915 }], ['desktop', { width: 1280, height: 800 }]]) {
    await reset(); const ctx = await b.newContext({ viewport: vp, deviceScaleFactor: 2 }); const p = await ctx.newPage();
    p.on('pageerror', e => errs.push(name + ': ' + e.message)); p.on('console', m => { if (m.type() === 'error' && !m.text().includes('Failed to load resource')) errs.push(name + ' console: ' + m.text()); });
    await p.goto(URL); await p.fill('#pw', 'zle'); await p.click('#lok'); await p.waitForTimeout(300);
    ok((await p.textContent('#lerr')).includes('Błędne'), name + ': błędne hasło odrzucone');
    await p.fill('#pw', 'demo'); await p.click('#lok'); await p.waitForSelector('.home', { timeout: 5000 });
    await p.waitForTimeout(1500); await p.screenshot({ path: `/tmp/tp_shots/${name}_home.png` });
    const ov = await p.evaluate(() => ({ sw: document.documentElement.scrollWidth, cw: document.documentElement.clientWidth, tabs: (document.querySelector('#tabs') || {}).getBoundingClientRect ? document.querySelector('#tabs').getBoundingClientRect().bottom : 0, vh: innerHeight }));
    ok(ov.sw <= ov.cw, name + ': brak poziomego przewijania'); 
    // palnik
    await p.evaluate(() => { location.hash = 'palnik'; }); await p.waitForTimeout(1500); await p.screenshot({ path: `/tmp/tp_shots/${name}_palnik.png` });
    ok(await p.locator('.bar .cell').count() >= 5, name + ': dolna belka PALNIK ma komórki');
    if (name === 'phone') { const tb = await p.evaluate(() => { const r = document.querySelector('#tabs').getBoundingClientRect(); return [r.bottom, innerHeight]; }); ok(tb[0] <= tb[1] + 0.5 && tb[0] > 0, name + ': pasek zakładek w obrębie ekranu'); }
    await p.evaluate(() => { location.hash = 'home'; }); await p.waitForTimeout(500); ok(await p.locator('.gauge .arc').count() === 1 && await p.locator('.hcard').count() >= 3, name + ': Pulpit ma łuk i karty'); await p.click('.hcard[data-go=palnik]'); await p.waitForTimeout(500); ok((await p.textContent('#top')).includes('Palnik'), name + ': karta Palnik prowadzi do ekranu palnika'); await p.evaluate(() => { location.hash = 'palnik'; });
    { const bs = await p.evaluate(() => ST.vals['0x2007']); const vis = await p.locator('.bar .cell:visible').count(); ok(bs !== 0 || vis === 0, name + ': przy wyłączonym palniku belka pusta (stan ' + bs + ', widoczne ' + vis + ')'); }
    // nastawy kociola
    await p.evaluate(() => { location.hash = 'set:0x00|KOCIOL|inst1'; }); await p.waitForSelector('.rows .row'); await p.waitForTimeout(500);
    ok(await p.locator('.rows .row').count() === 12, name + ': lista KOCIOŁ ma 12 wierszy z danymi');
    await p.screenshot({ path: `/tmp/tp_shots/${name}_settings.png` });
    await p.click('.rows .row >> nth=0'); await p.waitForSelector('#rg'); await p.fill('#rg', '66'); await p.click('#dk'); await p.waitForTimeout(400);
    ok((await p.textContent('#derr')).includes('tylko odczyt'), name + ': zapis w trybie tylko odczyt odrzucony');
    await p.click('#dc');
    if (name === 'phone') {
      // tryb pelny + PIN
      await p.click('#top [data-act=mode]'); await p.fill('#inp', 'demo'); await p.click('#dk'); await p.waitForTimeout(400);
      ok((await p.textContent('#top')).includes('Pełna'), name + ': tryb pełna kontrola');
      await p.click('.rows .row >> nth=0'); await p.waitForSelector('#rg'); await p.fill('#rg', '66'); await p.click('#dk'); await p.waitForTimeout(500);
      ok((await p.textContent('.rows .row >> nth=0')).includes('66'), name + ': zapis 66 przyjęty (poziom 0 wystarcza dla wiersza 0)');
      await p.fill('#rg', '66').catch(() => { });
      // wiersz wymagajacy poziomu >=1 bez PIN -> okno PIN
      const idx = await p.evaluate(() => ST.model.lists['0x00|KOCIOL|inst1'].findIndex(r => r.level_min >= 1 && r.dialog === 'slider'));
      if (idx >= 0) { await p.click(`.rows .row >> nth=${idx}`); await p.waitForTimeout(300); ok(await p.locator('#inp').count() === 1, name + ': wiersz poziomu ≥1 prosi o PIN'); await p.click('#dc'); }
      await p.screenshot({ path: `/tmp/tp_shots/${name}_full.png` });
    }
    // nowe ekrany v0.2
    for (const h of ['kociol', 'co1', 'co2', 'cwu', 'config', 'sched:co1', 'sched:cwu', 'stats', 'network', 'module', 'alarms', 'info', 'outtest', 'log', 'more']) {
      await p.evaluate(x => { location.hash = x; }, h); await p.waitForTimeout(450); const n = await p.locator('#main *').count(); ok(n > 2, name + ': ekran ' + h + ' renderuje (' + n + ')');
    }
    ok(await p.locator('#side a, #tabs a').count() > 4, name + ': nawigacja obecna');
    { const m = await p.evaluate(() => navItems().map(x => x.id)); ok(m.includes('co2') && !m.includes('co3') && !m.includes('bufor') && m.includes('cwu'), name + ': menu wg konfiguracji (2 obwody, CWU, bez bufora): ' + m.join(',')); }
    await p.evaluate(() => { location.hash = 'sched:co1'; }); await p.waitForSelector('.hr'); ok(await p.locator('.hr').count() === 24, name + ': harmonogram ma 24 godziny');
    await p.evaluate(() => { location.hash = 'config'; }); await p.waitForSelector('[data-cfg]'); ok(await p.locator('[data-cfg]').count() >= 5, name + ': konfiguracja kotła ma wiersze');
    await p.screenshot({ path: `/tmp/tp_shots/${name}_config.png` });
    await ctx.close();
  }
  // zapis zablokowanego parametru przez API
  const r = await (await fetch(URL + 'api/login', { method: 'POST', body: JSON.stringify({ password: 'demo' }) })).json();
  const H = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + r.token };
  await fetch(URL + 'api/mode', { method: 'POST', headers: H, body: JSON.stringify({ mode: 'full', password: 'demo' }) }); await fetch(URL + 'api/level', { method: 'POST', headers: H, body: JSON.stringify({ pin: '3333' }) });
  const w = async (pid, v) => (await (await fetch(URL + 'api/param/' + pid, { method: 'PUT', headers: H, body: JSON.stringify({ value: v }) })).json());
  ok((await w('0x3750', 1)).code === 'blocked', 'API: kominiarz 0x3750 zablokowany'); ok((await w('0x299F', 1)).code === 'blocked', 'API: typ paliwa 0x299F zablokowany');
  ok((await w('0x2003', 95)).code === 'out_of_range', 'API: poza zakresem odrzucone'); ok((await w('0x2001', 20)).code === 'not_writable', 'API: parametr live niezapisywalny'); ok((await w('0x2003', 70)).ok === true, 'API: poprawny zapis');
  ok(errs.length === 0, 'brak błędów JS/konsoli ' + JSON.stringify(errs)); await b.close(); console.log(fails ? 'E2E: ' + fails + ' BLEDOW' : 'E2E OK'); process.exit(fails ? 1 : 0);
})();
