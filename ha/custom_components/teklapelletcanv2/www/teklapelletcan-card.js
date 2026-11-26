/* TeklaPelletCAN-card v0.1 - karta Lovelace: panel Estyma igneoTouch 1:1 (ekrany renderowane z oprogramowania panelu FW2213TK) z danymi na zywo z modulu.
   Typ karty: custom:teklapelletcan-card   Opcje: entry_id (gdy kilka modulow), level (0-3, nadpisuje poziom z konfiguratora integracji), title.
   Zrodlo danych: integracja TeklaPelletCANv2 (polecenie websocket teklapelletcanv2/subscribe) - bez tworzenia encji. TYLKO ODCZYT: karta niczego nie zapisuje do kotla.
   Czesc ekranow panelu to obrazy wyrenderowane z firmware; zywe sa pola tekstowe z powiazaniem PID (reszta, np. ikony, pokazuje stan z renderu). Szczegoly: ha/README.md. */
const BASE = "/teklapelletcanv2/";
const FONT_PX = [12, 20, 26, 36, 64, 92];
const SCREEN_BY_ROW = {0:"KOCIOL",1:"PALNIK",2:"OGRZEWANIE1",3:"OGRZEWANIE2",4:"OGRZEWANIE3",5:"OGRZEWANIE4",6:"CWU",7:"SOLARY",8:"BUFOR",9:"KASKADA",10:"MAGAZYN",11:"ALARMY",12:"INFO",17:"TEST_WYJSC",18:"STATYSTYKI",19:"SIEC",20:"INTERNET"};
const OPS = {">=":(a,b)=>a>=b,"!=":(a,b)=>a!=b,"==":(a,b)=>a==b,"<=":(a,b)=>a<=b,">":(a,b)=>a>b,"<":(a,b)=>a<b};
const CFG_DEFAULT = {"0x2105":2,"0x2106":1,"0x2107":0,"0x2108":0,"0x350C":0,"0x3525":0};
const CSS = `:host{display:block}ha-card{overflow:hidden;background:#0f1218}
.wrap{position:relative;width:100%;background:#0f1218}.stage{position:relative;width:100%}
.scr{position:relative;width:480px;height:272px;background-size:480px 272px;cursor:pointer;transform-origin:0 0}.dlg{position:absolute;left:0;top:0;width:480px;height:272px;background-size:480px 272px}.dyn{position:absolute;white-space:nowrap;overflow:visible;font-stretch:condensed}
.list{max-width:480px;margin:0 auto;color:#e8ecf4;font:15px system-ui,sans-serif;min-height:272px;background:#1a1f2a}
.hd{display:flex;align-items:center;gap:10px;padding:10px 14px;border-bottom:1px solid #2a3040;font-weight:600}.hd button{background:none;border:0;color:#e8703a;font-size:20px;cursor:pointer}
.row{display:flex;justify-content:space-between;gap:8px;padding:12px 14px;border-bottom:1px solid #232938;cursor:pointer}.row:hover{background:#222a38}.row .v{color:#e8703a;white-space:nowrap}.mut{color:#8b93a7;padding:14px}
.st{position:absolute;right:6px;top:4px;font:11px system-ui,sans-serif;color:#ffb066;background:#000a;border-radius:6px;padding:2px 6px;pointer-events:none;z-index:5}`;
let LOAD = null;
function loadAll() {
  if (LOAD) return LOAD;
  const names = ["HOME", "PROGRAM"].concat(Object.values(SCREEN_BY_ROW)), S = {};
  LOAD = Promise.all(names.map(n => fetch(BASE + "screens/" + n + ".json").then(r => r.ok ? r.json() : null).then(j => { if (j) S[n] = j; }).catch(() => {})))
    .then(() => fetch(BASE + "model.json")).then(r => r.json()).then(M => ({ S, M }));
  return LOAD;
}
class TeklaPelletCANCard extends HTMLElement {
  setConfig(c) { this._cfg = Object.assign({}, c || {}); this._stack = ["SCR:HOME:Ekran glowny:HOME:-1"]; this._live = {}; this._read = {}; this._conn = null; this._lvl = 0; if (this._root) this._render(); }
  connectedCallback() {
    if (!this._root) { this._root = this.attachShadow({ mode: "open" }); this._root.innerHTML = "<style>" + CSS + "</style><ha-card><div class='wrap'><div class='stage'></div></div></ha-card>"; this._stage = this._root.querySelector(".stage"); this._wrap = this._root.querySelector(".wrap");
      new ResizeObserver(() => this._fit()).observe(this._wrap); }
    loadAll().then(d => { this._d = d; this._render(); this._timer = setInterval(() => this._tick(), 1000); }).catch(e => { this._stage.textContent = "Błąd ładowania panelu: " + e; });
    this._subscribe();
  }
  disconnectedCallback() { clearInterval(this._timer); if (this._unsub) { this._unsub.then(f => f()).catch(() => {}); this._unsub = null; } }
  set hass(h) { this._hass = h; if (this.isConnected && !this._unsub) this._subscribe(); }
  _subscribe() {
    if (!this._hass || this._unsub || !this._hass.connection) return;
    const msg = { type: "teklapelletcanv2/subscribe" }; if (this._cfg && this._cfg.entry_id) msg.entry_id = this._cfg.entry_id;
    this._unsub = this._hass.connection.subscribeMessage(ev => { this._live = ev.values || {}; this._conn = !!ev.connected; this._lvl = ev.level || 0; this._tick(); }, msg);
    this._unsub.catch(() => { this._unsub = null; this._conn = null; });
  }
  getCardSize() { return 6; }
  static getStubConfig() { return {}; }
  get level() { return this._cfg && this._cfg.level !== undefined ? +this._cfg.level : this._lvl; }
  _cfgVal(pid) { const v = this._live[pid]; return v === undefined ? CFG_DEFAULT[pid] : v; }
  _visible(r) { if ((r.level_min || 0) > this.level) return false; return (r.config_cond || []).every(x => (OPS[x.op] || (() => true))(this._cfgVal(x.pid) ?? 0, x.val)); }
  _fit() { const sc = this._stage.querySelector(".scr"); if (!sc) return; const k = this._wrap.clientWidth / 480; sc.style.transform = "scale(" + k + ")"; this._stage.style.height = Math.round(272 * k) + "px"; }
  _liveVal(b) { const v = this._live[b.pid]; return v === undefined ? undefined : v; }
  _text(f) {
    const d = new Date();
    if (f.widget === "0x20016a24") return d.getHours() + ":" + String(d.getMinutes()).padStart(2, "0");          // HOME: zegar (czas HA)
    if (f.widget === "0x2001b398" && this._live["0x3601"] !== undefined) return Math.round(this._live["0x3601"]) + "kg";   // HOME: paliwo (0x3601, kg)
    const b = (f.bind || []).find(x => this._liveVal(x) !== undefined); if (!b) return (f.template || "").replace("{}", "");
    const m = /(\d+\.(\d+))/.exec(f.sample || ""), dec = m ? m[2].length : 0; return (f.sample || "").replace(/-?\d+(\.\d+)?/, this._liveVal(b).toFixed(dec));
  }
  _tick() {
    if (!this._stage) return;
    this._stage.querySelectorAll(".dyn").forEach(e => { e.textContent = this._text(e._f); });
    let st = this._stage.querySelector(".st"); const txt = this._conn === false ? "offline - ostatnie wartości" : this._conn === null ? "brak danych z modułu" : "";
    if (txt) { if (!st) { st = document.createElement("div"); st.className = "st"; this._wrap.append(st); } st.textContent = txt; } else if (st) st.remove();
  }
  _go(stack) { this._stack = stack; this._render(); }
  _render() {
    if (!this._d || !this._stage) return; const { M } = this._d, cur = this._stack[this._stack.length - 1], s = this._stage; s.innerHTML = ""; s.style.height = "";
    if (cur === "MENU") { const w = document.createElement("div"); w.className = "list"; w.append(this._head("MENU")); M.menu_rows.filter(r => this._visible(r)).forEach(r => this._row(w, r.title_pl, "", () => this._open(r))); s.append(w); return; }
    if (cur.startsWith("SCR:")) { const [, node, title, target, row] = cur.split(":"); this._screen(s, node, title, target, row); return; }
    if (cur.startsWith("LISTA:")) { const [, key, title] = cur.split(":"); const w = document.createElement("div"); w.className = "list"; w.append(this._head(title + " - nastawy"));
      const rows = (M.lists[key] || []).filter(r => r.pid && (r.level_min || 0) <= this.level);
      if (!rows.length) { const m = document.createElement("div"); m.className = "mut"; m.textContent = "Brak wierszy dla tego poziomu."; w.append(m); }
      const fmt = r => { const v = this._read[r.pid]; if (v === undefined) return "…"; const d = (r.fmt && r.fmt.decimals) || 0; return (+v).toFixed(d); };
      rows.forEach(r => this._row(w, r.name, fmt(r), null)); s.append(w);
      if (this._hass) this._hass.callWS(Object.assign({ type: "teklapelletcanv2/read", pids: rows.map(r => r.pid).slice(0, 40) }, this._cfg.entry_id ? { entry_id: this._cfg.entry_id } : {})).then(v => { Object.assign(this._read, v); if (this._stack[this._stack.length - 1] === cur) { w.querySelectorAll(".row .v").forEach((e, i) => { if (rows[i]) e.textContent = fmt(rows[i]); }); } }).catch(() => {});
      return; }
    const m = document.createElement("div"); m.className = "list"; m.append(this._head((M.nodes[cur] || {}).title || cur)); s.append(m);
  }
  _head(t) { const e = document.createElement("div"); e.className = "hd"; if (this._stack.length > 1) { const b = document.createElement("button"); b.textContent = "<"; b.onclick = () => { this._go(this._stack.slice(0, -1)); }; e.append(b); } e.append(t); return e; }
  _row(p, n, v, cb) { const e = document.createElement("div"); e.className = "row"; e.innerHTML = '<span class="n"></span><span class="v"></span>'; e.children[0].textContent = n; e.children[1].textContent = v; if (cb) e.onclick = cb; else e.style.cursor = "default"; p.append(e); }
  _open(r) {
    const sn = SCREEN_BY_ROW[r.row], st = this._stack.slice();
    if (sn && this._d.S[sn]) st.push("SCR:" + sn + ":" + r.title_pl + ":" + r.target + ":" + r.row);
    else if (r.target === "KOCIOL") st.push("LISTA:0x00|KOCIOL|inst1:" + r.title_pl);
    else if (r.target) st.push(r.target);
    this._go(st);
  }
  _listKey(target, row) { const M = this._d.M, ks = Object.keys(M.lists).filter(k => k.split("|")[1] === target).sort(); return ks[target === "OGRZEWANIE" ? Math.min(Math.max(row - 2, 0), ks.length - 1) : 0]; }
  _screen(s, node, title, target, row) {
    const sc = this._d.S[node]; if (!sc) { this._go(["SCR:HOME:Ekran glowny:HOME:-1"]); return; }
    const box = document.createElement("div"); box.className = "scr"; box.style.backgroundImage = "url(" + BASE + "screens/" + node + ".png)";
    (sc.dynamic || []).forEach(f => { const e = document.createElement("div"); e.className = "dyn"; Object.assign(e.style, { left: f.rect[0] + "px", top: f.rect[1] + "px", width: f.rect[2] + "px", height: f.rect[3] + "px", color: f.color, font: FONT_PX[f.font] + "px/" + f.rect[3] + "px 'Roboto Condensed','Arial Narrow',sans-serif", textAlign: ["left", "center", "right"][f.align] }); e._f = f; e.textContent = this._text(f); box.append(e); });
    const dialog = img => { const d = document.createElement("div"); d.className = "dlg"; d.style.backgroundImage = "url(" + BASE + "dialogs/" + img + ".png)"; box.append(d); return d; };
    box.onclick = ev => {
      if (box._swiped) { box._swiped = false; return; }
      const r = box.getBoundingClientRect(), k = 480 / r.width, x = (ev.clientX - r.left) * k, y = (ev.clientY - r.top) * k, dlg = box.querySelector(".dlg");
      if (node === "PROGRAM") { if (dlg) { if (x > 385 && y > 205 && y < 250) dlg.remove(); else if (dlg.dataset.k === "program_day" && x > 190 && x < 290 && y > 185 && y < 215) { dlg.dataset.k = "program_copy"; dlg.style.backgroundImage = "url(" + BASE + "dialogs/program_copy.png)"; } return; }
        if (y > 45 && x > 55 && x < 430) { const d = dialog("program_day"); d.dataset.k = "program_day"; return; } }
      if (node === "HOME") { if (dlg) { if (x > 385 && y > 205 && y < 250) dlg.remove(); return; }
        const hit = [["home_paliwo", x >= 70 && x < 135 && y > 195], ["home_zasilanie", x > 415 && y > 195], ["home_auto", x > 340 && y < 100 && y > 50], ["home_czas", x < 90 && y < 45], ["home_data", x > 160 && x < 320 && y < 45]].find(h => h[1]);
        if (hit) { dialog(hit[0]); return; }
        if (x < 70 && y > 195) this._go(["SCR:HOME:Ekran glowny:HOME:-1", "MENU"]); return; }
      if (x > 420 && y < 45) this._go(["SCR:HOME:Ekran glowny:HOME:-1"]);
      else if (x < 130 && y < 45) this._go(this._stack.slice(0, -1));
      else if (node === "KOCIOL" && x > 345 && y > 110 && y < 150) this._go(this._stack.concat(["SCR:PROGRAM:Program:PROGRAM:-1"]));
      else if (x > 240 && y > 225) { const key = this._listKey(target, +row); if (key) this._go(this._stack.concat(["LISTA:" + key + ":" + title])); }
    };
    if (node === "PROGRAM") { const page = n => { box.style.backgroundImage = "url(" + BASE + "screens/" + (n ? "PROGRAM2" : "PROGRAM") + ".png)"; };
      box.onwheel = ev => { ev.preventDefault(); page(ev.deltaY > 0 ? 1 : 0); }; let y0 = null; box.addEventListener("pointerdown", ev => { y0 = ev.clientY; }); box.addEventListener("pointerup", ev => { if (y0 !== null && Math.abs(ev.clientY - y0) > 30) { page(ev.clientY < y0 ? 1 : 0); box._swiped = true; } y0 = null; }); }
    s.append(box); this._fit();
  }
}
if (!customElements.get("teklapelletcan-card")) customElements.define("teklapelletcan-card", TeklaPelletCANCard);
window.customCards = window.customCards || [];
window.customCards.push({ type: "teklapelletcan-card", name: "TeklaPelletCAN (panel 1:1)", description: "Panel Estyma igneoTouch 1:1 z danymi na żywo z modułu TeklaPelletCANv2 (tylko odczyt)", preview: false });
