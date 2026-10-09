// Sushi Client UI. Plain JS, no framework. All data comes from window.sushi (see preload.js).
(() => {
  const app = document.getElementById('app');
  const toastEl = document.getElementById('toast');

  const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const PRESET_ORDER = ['off', 'balanced', 'max'];

  const ICON = {
    logo: '<path d="M4 12c0-4.4 3.6-8 8-8s8 3.6 8 8-3.6 8-8 8-8-3.6-8-8z" fill="none" stroke="#0a0b18" stroke-width="2"/><circle cx="12" cy="12" r="3.2" fill="#0a0b18"/>',
    home: '<path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
    profiles: '<rect x="3" y="3" width="7.5" height="7.5" rx="2"/><rect x="13.5" y="3" width="7.5" height="7.5" rx="2"/><rect x="3" y="13.5" width="7.5" height="7.5" rx="2"/><rect x="13.5" y="13.5" width="7.5" height="7.5" rx="2"/>',
    mods: '<path d="M12 2.5l8.5 4.9v9.2L12 21.5l-8.5-4.9V7.4z"/><path d="M3.5 7.4L12 12.3l8.5-4.9M12 12.3v9.2"/>',
    fps: '<path d="M13 2.5L4.5 13.5H11l-1 8 8.5-11H12z"/>',
    accounts: '<circle cx="12" cy="8.5" r="4"/><path d="M4.5 20.5c.8-3.8 3.8-6 7.5-6s6.7 2.2 7.5 6"/>',
    console: '<path d="M4 6l6 6-6 6M12.5 18.5H20"/>',
    settings: '<path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2.2"/><circle cx="8" cy="17" r="2.2"/>',
  };
  const svg = (name) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${ICON[name]}</svg>`;

  const PAGES = [
    { id: 'home', label: 'Home', title: 'Home' },
    { id: 'profiles', label: 'Profiles', title: 'Profiles' },
    { id: 'mods', label: 'Mods', title: 'Mods' },
    { id: 'fps', label: 'FPS', title: 'FPS Boost' },
    { id: 'accounts', label: 'Accounts', title: 'Accounts' },
    { id: 'console', label: 'Console', title: 'Console' },
  ];

  const S = {
    page: 'home',
    settings: null,
    versions: [],
    accounts: { activeId: null, accounts: [] },
    catalog: [],
    presets: {},
    running: false,
    status: 'Ready to play',
    busy: false,
    logs: [],
    msCode: null,
    editId: null,
    modCat: 'All',
    modQuery: '',
    settingsTab: 'game',
    versionAll: false,
  };

  const profiles = () => S.settings.profiles;
  const active = () => profiles().find((p) => p.id === S.settings.activeProfileId) || profiles()[0];
  const editing = () => profiles().find((p) => p.id === S.editId) || active();
  const activeAcc = () => S.accounts.accounts.find((a) => a.id === S.accounts.activeId) || null;
  const presetLabel = (key) => (S.presets[key] && S.presets[key].label) || key;
  const loaderLabel = (p) => (p.fabric ? 'Fabric' : 'Vanilla');
  const initials = (name) => esc((name || '?').slice(0, 2).toUpperCase());

  function toast(text, kind = '') {
    toastEl.textContent = text;
    toastEl.className = `toast ${kind}`;
    toastEl.hidden = false;
    clearTimeout(toast.t);
    toast.t = setTimeout(() => { toastEl.hidden = true; }, 4200);
  }

  async function saveSettings(patch) {
    S.settings = await window.sushi.settings.set(patch);
    render();
  }
  const saveProfiles = (list) => saveSettings({ profiles: list });
  const updateProfile = (id, patch) => saveProfiles(profiles().map((p) => (p.id === id ? { ...p, ...patch } : p)));

  /* ---------- shell ---------- */
  function shell() {
    const acc = activeAcc();
    const p = active();
    const rail = PAGES.map((pg) => `<button class="nav ${S.page === pg.id ? 'on' : ''}" data-act="page" data-page="${pg.id}">${svg(pg.id === 'profiles' ? 'profiles' : pg.id)}<span>${pg.label}</span></button>`).join('');
    const page = PAGES.find((pg) => pg.id === S.page);
    return `
      <aside class="rail">
        <div class="logo"><svg viewBox="0 0 24 24">${ICON.logo}</svg></div>
        ${rail}
        <div class="spacer"></div>
        <button class="nav ${S.page === 'settings' ? 'on' : ''}" data-act="page" data-page="settings">${svg('settings')}<span>Settings</span></button>
      </aside>
      <main class="main">
        <header class="topbar">
          <div>
            <div class="crumb">Sushi Client</div>
            <h2 class="title">${S.page === 'settings' ? 'Settings' : esc(page.title)}</h2>
          </div>
          <div class="right-tools">
            <span class="status-pill"><span class="dot"></span>${esc(p.name)} · ${esc(p.mc)} ${p.fabric ? '· Fabric' : ''}</span>
            <button class="chip" data-act="page" data-page="accounts">
              <span class="avatar">${acc ? initials(acc.username) : '?'}</span>
              <span>${acc ? esc(acc.username) : 'Sign in'}<small>${acc ? (acc.type === 'microsoft' ? 'Microsoft' : 'Offline') : 'Add an account'}</small></span>
            </button>
          </div>
        </header>
        <section class="content" id="content">${pageHtml()}</section>
      </main>
      <footer class="playbar" id="playbar">${playbarHtml()}</footer>`;
  }

  function playbarHtml() {
    const p = active();
    const busy = S.busy || (S.running && S.status !== 'Minecraft is running');
    const acc = activeAcc();
    const mods = p.fabric ? p.mods.length : 0;
    return `
      <div class="pb-info">
        <div class="name">${esc(p.name)}</div>
        <div class="meta">${esc(p.mc)} · ${loaderLabel(p)}${p.fabric ? ` · ${mods} mods` : ''} · ${esc(presetLabel(p.fpsPreset))}</div>
      </div>
      <div class="pb-mid">
        <div class="pb-status">${esc(acc ? `Playing as ${acc.username}` : 'Add an account to play')} — ${esc(S.status)}</div>
        <div class="bar ${busy ? 'busy' : ''}">${busy ? '<i></i>' : (S.running ? '<i style="width:100%"></i>' : '')}</div>
      </div>
      <button class="launch" data-act="launch" ${S.running || !acc ? 'disabled' : ''}>${S.running ? (busy ? 'Preparing…' : 'Running') : 'Launch'}</button>`;
  }

  function pageHtml() {
    switch (S.page) {
      case 'profiles': return profilesPage();
      case 'mods': return modsPage();
      case 'fps': return fpsPage();
      case 'accounts': return accountsPage();
      case 'console': return consolePage();
      case 'settings': return settingsPage();
      default: return homePage();
    }
  }

  /* ---------- home ---------- */
  function homePage() {
    const p = active();
    const acc = activeAcc();
    const news = [
      { tag: 'Update', tagc: '', title: 'Sushi Core: module menu', text: 'Press Right Shift in game to toggle FPS, CPS, keystrokes, ping and coordinates.', grad: 'linear-gradient(135deg,#0e7490,#22d3ee)' },
      { tag: 'Guide', tagc: 'p', title: 'Pick an FPS preset', text: 'Max FPS trades visuals for speed. Balanced keeps shadows and particles.', grad: 'linear-gradient(135deg,#6d28d9,#c084fc)' },
      { tag: 'Roadmap', tagc: 'g', title: 'Coming next', text: 'Cosmetics, server list and a news feed are planned. Android comes after Windows.', grad: 'linear-gradient(135deg,#1e1b4b,#7c3aed)' },
    ];
    const newsHtml = news.map((n) => `
      <article class="news-card">
        <div class="news-top" style="background:${n.grad}"></div>
        <div class="news-body"><span class="tag ${n.tagc}">${esc(n.tag)}</span><h4>${esc(n.title)}</h4><p>${esc(n.text)}</p></div>
      </article>`).join('');
    return `
      <div class="stack">
        <section class="hero">
          <svg class="hero-art" viewBox="0 0 400 400" aria-hidden="true">
            <defs>
              <radialGradient id="g1" cx="50%" cy="50%" r="50%"><stop offset="0%" stop-color="#22d3ee" stop-opacity="0.9"/><stop offset="100%" stop-color="#a855f7" stop-opacity="0.2"/></radialGradient>
              <linearGradient id="g2" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#67e8f9"/><stop offset="100%" stop-color="#a855f7"/></linearGradient>
            </defs>
            <circle cx="200" cy="200" r="190" fill="url(#g1)" opacity="0.25"/>
            <circle cx="200" cy="200" r="150" fill="none" stroke="url(#g2)" stroke-width="2" opacity="0.6"/>
            <circle cx="200" cy="200" r="118" fill="none" stroke="#22d3ee" stroke-width="1" stroke-dasharray="4 8" opacity="0.5"/>
            <circle cx="200" cy="200" r="86" fill="#0d1024" stroke="url(#g2)" stroke-width="3"/>
            <circle cx="200" cy="200" r="46" fill="url(#g2)" opacity="0.85"/>
            <circle cx="200" cy="200" r="20" fill="#0d1024"/>
          </svg>
          <div class="hero-copy">
            <div class="eyebrow">Sushi Client · Beta</div>
            <h1>Play smoother.<br/><span class="grad">See everything.</span></h1>
            <p>Fabric ${esc(p.mc)}, the Sushi Core HUD and tuned FPS presets, all in one portable launcher.</p>
            <div class="row">
              <span class="tag">${esc(p.mc)}</span><span class="tag p">${loaderLabel(p)}</span><span class="tag g">${esc(presetLabel(p.fpsPreset))}</span>
            </div>
          </div>
        </section>
        <div class="grid2">
          <section class="panel">
            <h3>Latest from Sushi</h3>
            <div class="news">${newsHtml}</div>
          </section>
          <aside class="stack">
            <section class="panel profile-card">
              <h3>Active profile</h3>
              <div class="big">${esc(p.name)}</div>
              <div class="kv"><span>Version</span><span>${esc(p.mc)}</span></div>
              <div class="kv"><span>Loader</span><span>${loaderLabel(p)}</span></div>
              <div class="kv"><span>FPS preset</span><span>${esc(presetLabel(p.fpsPreset))}</span></div>
              <div class="kv"><span>Mods</span><span>${p.fabric ? p.mods.length : 0}</span></div>
              <div class="kv"><span>Account</span><span>${acc ? esc(acc.username) : 'None'}</span></div>
              <div class="row" style="margin-top:12px"><button class="btn sm" data-act="page" data-page="profiles">Change profile</button></div>
            </section>
            <section class="panel">
              <h3>Sushi Core HUD</h3>
              <div class="hud-preview">
                <div class="hud-box"><div style="color:#67e8f9">FPS 184</div><div>CPS 6 | 2</div><div>Ping 38 ms</div></div>
                <div class="hud-keys">
                  <b></b><b class="w on">W</b><b></b>
                  <b>A</b><b>S</b><b>D</b>
                  <b class="space on">SPACE</b>
                </div>
              </div>
              <p class="muted small" style="margin:12px 0 0">Press <b>Right Shift</b> in game to open the module menu.</p>
            </section>
          </aside>
        </div>
      </div>`;
  }

  /* ---------- profiles ---------- */
  function profilesPage() {
    const ed = editing();
    const cards = profiles().map((p) => `
      <button class="pcard ${p.id === ed.id ? 'sel' : ''}" data-act="edit-profile" data-id="${p.id}">
        <div class="thumb" style="opacity:${p.id === ed.id ? 1 : 0.55}"></div>
        ${p.id === S.settings.activeProfileId ? '<span class="tag">Active</span>' : ''}
        <h4>${esc(p.name)}</h4>
        <div class="sub">${esc(p.mc)} · ${loaderLabel(p)} · ${esc(presetLabel(p.fpsPreset))}</div>
        <div class="sub">${p.fabric ? `${p.mods.length} mods` : 'No mods'}</div>
      </button>`).join('');
    const versions = S.versions.filter((v) => v.type === 'release' && (S.versionAll || v.id.startsWith('1.21') || v.id === ed.mc));
    const verOpts = versions.map((v) => `<option value="${esc(v.id)}" ${v.id === ed.mc ? 'selected' : ''}>${esc(v.id)}</option>`).join('');
    const presetOpts = PRESET_ORDER.map((k) => `<option value="${k}" ${k === ed.fpsPreset ? 'selected' : ''}>${esc(presetLabel(k))}</option>`).join('');
    const isActive = ed.id === S.settings.activeProfileId;
    return `
      <div class="split">
        <div class="grid3">
          ${cards}
          <button class="pcard add" data-act="new-profile"><b>+</b><span>New profile</span></button>
        </div>
        <section class="panel">
          <h3>Edit profile</h3>
          <label class="field"><span>Name</span><input type="text" id="pname" value="${esc(ed.name)}" data-change="profile-name" data-id="${ed.id}" maxlength="32"/></label>
          <label class="field"><span>Minecraft version</span>
            <select data-change="profile-mc" data-id="${ed.id}">${verOpts}</select>
          </label>
          <div class="row small" style="margin:-6px 0 14px"><label class="row" style="gap:6px;cursor:pointer"><input type="checkbox" data-change="ver-all" ${S.versionAll ? 'checked' : ''}/> Show all releases</label></div>
          <div class="setting" style="padding-top:0;border-top:0">
            <div class="txt"><b>Fabric loader</b><span>Required for mods and Sushi Core.</span></div>
            <label class="switch"><input type="checkbox" data-change="profile-fabric" data-id="${ed.id}" ${ed.fabric ? 'checked' : ''}/><span></span></label>
          </div>
          <label class="field"><span>FPS preset</span><select data-change="profile-preset" data-id="${ed.id}">${presetOpts}</select></label>
          <div class="row" style="margin-top:6px">
            ${isActive ? '<span class="tag">Active profile</span>' : `<button class="btn primary sm" data-act="activate" data-id="${ed.id}">Set as active</button>`}
            <button class="btn sm" data-act="duplicate" data-id="${ed.id}">Duplicate</button>
            <button class="btn sm danger" data-act="delete" data-id="${ed.id}" ${profiles().length < 2 ? 'disabled' : ''}>Delete</button>
          </div>
        </section>
      </div>`;
  }

  /* ---------- mods ---------- */
  const MOD_CATS = ['All', 'Performance', 'HUD', 'Visuals', 'Utility', 'Required'];

  function modsPage() {
    const p = active();
    const notice = p.fabric ? '' : `<div class="notice">${esc(p.name)} runs without Fabric, so mods are off. Turn on Fabric in Profiles to use them.</div>`;
    const cats = MOD_CATS.map((c) => `<button class="cat ${S.modCat === c ? 'on' : ''}" data-act="mod-cat" data-cat="${c}">${c}</button>`).join('');
    return `
      <div class="muted small" style="margin-bottom:10px">Editing mods for <b style="color:var(--text)">${esc(p.name)}</b></div>
      ${notice}
      <div class="toolbar">
        <input type="text" id="modq" placeholder="Search mods" value="${esc(S.modQuery)}" data-input="mod-q"/>
        ${cats}
      </div>
      <div class="mgrid" id="modgrid">${modCards()}</div>`;
  }

  function modCards() {
    const p = active();
    const q = S.modQuery.trim().toLowerCase();
    const list = S.catalog.filter((m) => (S.modCat === 'All' || m.category === S.modCat)
      && (!q || m.name.toLowerCase().includes(q) || m.desc.toLowerCase().includes(q)));
    if (!list.length) return '<div class="empty">No mods match your search.</div>';
    return list.map((m) => {
      const required = m.required || m.slug === 'fabric-api';
      const on = required || (p.fabric && p.mods.includes(m.slug));
      const disabled = required || !p.fabric;
      const initialsTxt = m.name.split(/\s+/).map((w) => w[0]).join('').slice(0, 2).toUpperCase();
      return `
        <div class="mcard ${on ? 'on' : ''}">
          <div class="icon">${esc(initialsTxt)}</div>
          <div class="body">
            <h4>${esc(m.name)} ${m.category ? `<span class="tag g">${esc(m.category)}</span>` : ''} ${m.bundled ? '<span class="tag p">Bundled</span>' : ''}</h4>
            <p>${esc(m.desc)}</p>
            <div class="row" style="margin-top:10px;justify-content:space-between">
              ${required ? '<span class="small muted">Always installed</span>' : `<span class="small muted">${p.fabric ? (on ? 'Enabled' : 'Disabled') : 'Needs Fabric'}</span>`}
              <label class="switch"><input type="checkbox" data-change="mod" data-slug="${esc(m.slug)}" ${on ? 'checked' : ''} ${disabled ? 'disabled' : ''}/><span></span></label>
            </div>
          </div>
        </div>`;
    }).join('');
  }

  /* ---------- fps ---------- */
  function fpsPage() {
    const p = active();
    const cards = PRESET_ORDER.filter((k) => S.presets[k]).map((k) => {
      const pr = S.presets[k];
      const level = { off: 15, balanced: 60, max: 100 }[k];
      const sel = p.fpsPreset === k;
      return `
        <button class="preset ${sel ? 'sel' : ''}" data-act="preset" data-key="${k}">
          <div class="row" style="justify-content:space-between"><h4>${esc(pr.label)}</h4>${sel ? '<span class="tag">Selected</span>' : ''}</div>
          <p>${esc(pr.description)}</p>
          <div class="small muted">Speed</div>
          <div class="meter"><i style="width:${level}%"></i></div>
        </button>`;
    }).join('');
    return `
      <div class="notice">Presets apply to the active profile (<b>${esc(p.name)}</b>) and rewrite Minecraft's options.txt on launch.</div>
      <div class="grid3" style="grid-template-columns:repeat(auto-fill,minmax(260px,1fr))">${cards}</div>
      <section class="panel" style="margin-top:18px">
        <h3>Notes</h3>
        <p class="muted small" style="margin:0;line-height:1.6">Max FPS lowers render distance, particles and mipmaps, and uses the ZGC garbage collector on Java 17+. Frame rate gains depend on your GPU and CPU. Sodium and Lithium are included for Balanced and Max FPS.</p>
      </section>`;
  }

  /* ---------- accounts ---------- */
  function accountsPage() {
    const list = S.accounts.accounts.map((a) => `
      <div class="acc ${a.id === S.accounts.activeId ? 'on' : ''}">
        <span class="avatar">${initials(a.username)}</span>
        <div style="flex:1;min-width:0">
          <div class="name">${esc(a.username)}</div>
          <div class="small muted">${a.type === 'microsoft' ? 'Microsoft account' : 'Offline account'}</div>
        </div>
        ${a.id === S.accounts.activeId ? '<span class="tag">Active</span>' : `<button class="btn sm" data-act="acc-select" data-id="${esc(a.id)}">Use</button>`}
        <button class="btn sm danger" data-act="acc-remove" data-id="${esc(a.id)}">Remove</button>
      </div>`).join('') || '<div class="empty">No accounts yet. Sign in with Microsoft or add an offline name.</div>';
    const hasClient = !!S.settings.msClientId;
    return `
      <div class="grid2">
        <section class="stack">
          <div class="panel">
            <h3>Microsoft</h3>
            <p class="muted small" style="margin:0 0 14px;line-height:1.5">Sign in with your Minecraft account. A code appears here; enter it on the Microsoft page that opens.</p>
            ${S.msCode ? `<div class="small muted">Enter this code at the Microsoft page:</div><div class="code">${esc(S.msCode.code)}</div><div class="row"><button class="btn sm" data-act="open-ms" data-url="${esc(S.msCode.url)}">Open page again</button></div>` : ''}
            ${hasClient ? '' : '<div class="notice" style="margin:12px 0 0">Add your Azure Client ID in Settings first.</div>'}
            <div class="row" style="margin-top:14px"><button class="btn primary" data-act="ms-login" ${S.msCode || !hasClient ? 'disabled' : ''}>Sign in with Microsoft</button></div>
          </div>
          <div class="panel">
            <h3>Offline</h3>
            <p class="muted small" style="margin:0 0 12px">Play with any name. Offline mode cannot join servers that require a Microsoft account.</p>
            <div class="row"><input type="text" id="offname" placeholder="Username (3-16 letters, numbers, _)" maxlength="16" style="flex:1"/><button class="btn" data-act="offline-add">Add</button></div>
          </div>
        </section>
        <section class="panel">
          <h3>Saved accounts</h3>
          ${list}
        </section>
      </div>`;
  }

  /* ---------- console ---------- */
  function consolePage() {
    return `
      <div class="row" style="justify-content:space-between;margin-bottom:12px"><span class="muted small">Live output from Minecraft and the launcher</span><button class="btn sm" data-act="log-clear">Clear</button></div>
      <pre class="console" id="console">${esc(S.logs.join('\n')) || 'Nothing yet. Launch the game to see output here.'}</pre>`;
  }

  /* ---------- settings ---------- */
  function settingsPage() {
    const s = S.settings;
    const tabs = [['game', 'Game'], ['java', 'Java'], ['azure', 'Microsoft'], ['launcher', 'Launcher']]
      .map(([k, l]) => `<button class="tab ${S.settingsTab === k ? 'on' : ''}" data-act="stab" data-tab="${k}">${l}</button>`).join('');
    let body = '';
    if (S.settingsTab === 'game') {
      body = `
        <div class="setting"><div class="txt"><b>Memory</b><span>RAM given to Minecraft. Keep 2 GB free for Windows.</span></div>
          <div class="ctl"><div class="row" style="justify-content:space-between"><span class="small muted">Allocated</span><b id="memlabel">${(s.memoryMB / 1024).toFixed(1)} GB</b></div>
            <input type="range" min="2048" max="16384" step="512" value="${s.memoryMB}" data-input="mem" data-set="memoryMB"/></div></div>
        <div class="setting"><div class="txt"><b>Resolution</b><span>Starting window size.</span></div>
          <div class="ctl row"><input type="number" min="640" max="7680" value="${s.width}" data-change="set" data-set="width" style="flex:1"/><span class="muted">×</span><input type="number" min="480" max="4320" value="${s.height}" data-change="set" data-set="height" style="flex:1"/></div></div>`;
    } else if (S.settingsTab === 'java') {
      body = `
        <div class="setting"><div class="txt"><b>Java path</b><span>Leave empty to let Sushi download Java 21 for you.</span></div>
          <div class="ctl"><input type="text" value="${esc(s.javaPath)}" placeholder="Automatic" data-change="set" data-set="javaPath"/></div></div>`;
    } else if (S.settingsTab === 'azure') {
      body = `
        <div class="setting"><div class="txt"><b>Azure Client ID</b><span>Needed for Microsoft sign-in. Not a secret. Create a free app in Azure, then paste its ID here.</span></div>
          <div class="ctl"><input type="text" value="${esc(s.msClientId)}" placeholder="00000000-0000-0000-0000-000000000000" data-change="set" data-set="msClientId"/></div></div>`;
    } else {
      body = `
        <div class="setting"><div class="txt"><b>Sushi folder</b><span>Instances, mods, settings and accounts live here.</span></div>
          <div class="ctl"><button class="btn" data-act="open-folder">Open folder</button></div></div>
        <div class="setting"><div class="txt"><b>Reset profiles</b><span>Restore the built-in profiles. Your accounts are kept.</span></div>
          <div class="ctl"><button class="btn danger" data-act="reset-profiles">Reset profiles</button></div></div>`;
    }
    return `<div class="panel" style="max-width:820px"><div class="tabs">${tabs}</div>${body}</div>`;
  }

  /* ---------- render ---------- */
  function render() {
    if (!S.settings) return;
    app.innerHTML = shell();
  }

  function refreshLive() {
    const pb = document.getElementById('playbar');
    if (pb) pb.innerHTML = playbarHtml();
    const con = document.getElementById('console');
    if (con && S.page === 'console') {
      const atBottom = con.scrollTop + con.clientHeight >= con.scrollHeight - 40;
      con.textContent = S.logs.join('\n');
      if (atBottom) con.scrollTop = con.scrollHeight;
    }
  }

  /* ---------- actions ---------- */
  async function launch() {
    if (S.running) return;
    if (!activeAcc()) { toast('Add an account first.', 'bad'); S.page = 'accounts'; render(); return; }
    const p = active();
    S.running = true;
    S.busy = true;
    S.status = 'Preparing…';
    S.logs.push(`> Launching ${p.name} (${p.mc}${p.fabric ? ', Fabric' : ''})`);
    render();
    try {
      await window.sushi.game.launch(p.id);
      S.busy = false;
      S.status = 'Minecraft is running';
      toast('Minecraft started. Right Shift opens the Sushi menu.', 'ok');
    } catch (err) {
      S.running = false;
      S.busy = false;
      S.status = 'Launch failed';
      S.logs.push(`ERROR: ${err.message}`);
      toast(err.message, 'bad');
    }
    render();
  }

  async function act(name, el) {
    switch (name) {
      case 'page':
        S.page = el.dataset.page;
        render();
        break;
      case 'launch': await launch(); break;
      case 'edit-profile':
        S.editId = el.dataset.id; render(); break;
      case 'activate':
        await saveSettings({ activeProfileId: el.dataset.id }); toast(`${active().name} is now active.`, 'ok'); break;
      case 'new-profile': {
        const id = `p${Date.now().toString(36)}`;
        const base = active();
        const list = profiles().concat({ ...base, id, name: `Profile ${profiles().length + 1}`, mods: base.mods.slice() });
        S.editId = id;
        await saveProfiles(list);
        break;
      }
      case 'duplicate': {
        const src = profiles().find((p) => p.id === el.dataset.id);
        const id = `p${Date.now().toString(36)}`;
        S.editId = id;
        await saveProfiles(profiles().concat({ ...src, id, name: `${src.name} copy`, mods: src.mods.slice() }));
        break;
      }
      case 'delete': {
        const list = profiles().filter((p) => p.id !== el.dataset.id);
        if (!list.length) return;
        const activeId = list.some((p) => p.id === S.settings.activeProfileId) ? S.settings.activeProfileId : list[0].id;
        S.editId = activeId;
        await saveSettings({ profiles: list, activeProfileId: activeId });
        break;
      }
      case 'mod-cat': S.modCat = el.dataset.cat; render(); break;
      case 'preset':
        await updateProfile(active().id, { fpsPreset: el.dataset.key });
        break;
      case 'ms-login': {
        S.msCode = null;
        render();
        try {
          const acc = await window.sushi.accounts.microsoft();
          S.msCode = null;
          S.accounts = await window.sushi.accounts.list();
          toast(`Signed in as ${acc.username}`, 'ok');
        } catch (err) {
          S.msCode = null;
          toast(err.message, 'bad');
        }
        render();
        break;
      }
      case 'open-ms': window.sushi.app.openUrl(el.dataset.url); break;
      case 'offline-add': {
        const input = document.getElementById('offname');
        try {
          await window.sushi.accounts.offline(input.value);
          S.accounts = await window.sushi.accounts.list();
          toast('Offline account added.', 'ok');
          render();
        } catch (err) { toast(err.message, 'bad'); }
        break;
      }
      case 'acc-select':
        await window.sushi.accounts.select(el.dataset.id);
        S.accounts = await window.sushi.accounts.list();
        render();
        break;
      case 'acc-remove':
        await window.sushi.accounts.remove(el.dataset.id);
        S.accounts = await window.sushi.accounts.list();
        render();
        break;
      case 'log-clear': S.logs = []; render(); break;
      case 'stab': S.settingsTab = el.dataset.tab; render(); break;
      case 'open-folder': window.sushi.app.openFolder(); break;
      case 'reset-profiles':
        S.settings = await window.sushi.settings.resetProfiles();
        S.editId = S.settings.activeProfileId;
        toast('Profiles reset.', 'ok');
        render();
        break;
      default: break;
    }
  }

  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-act]');
    if (!el || el.disabled) return;
    act(el.dataset.act, el).catch((err) => toast(err.message, 'bad'));
  });

  document.addEventListener('change', async (e) => {
    const el = e.target;
    const kind = el.dataset.change;
    if (!kind) return;
    try {
      if (kind === 'profile-name') {
        const name = el.value.trim() || 'Untitled';
        await updateProfile(el.dataset.id, { name });
      } else if (kind === 'profile-mc') {
        await updateProfile(el.dataset.id, { mc: el.value });
      } else if (kind === 'profile-fabric') {
        await updateProfile(el.dataset.id, { fabric: el.checked });
      } else if (kind === 'profile-preset') {
        await updateProfile(el.dataset.id, { fpsPreset: el.value });
      } else if (kind === 'ver-all') {
        S.versionAll = el.checked; render();
      } else if (kind === 'mod') {
        const p = active();
        const slug = el.dataset.slug;
        const mods = el.checked ? [...new Set([...p.mods, slug])] : p.mods.filter((m) => m !== slug);
        await updateProfile(p.id, { mods });
      } else if (kind === 'set') {
        const key = el.dataset.set;
        let value = el.value;
        if (['memoryMB', 'width', 'height'].includes(key)) value = Math.round(Number(value));
        if (key === 'memoryMB') value = Math.min(16384, Math.max(2048, value));
        await saveSettings({ [key]: value });
        toast('Saved.', 'ok');
      }
    } catch (err) { toast(err.message, 'bad'); }
  });

  document.addEventListener('input', (e) => {
    const el = e.target;
    if (el.dataset.input === 'mod-q') {
      S.modQuery = el.value;
      const grid = document.getElementById('modgrid');
      if (grid) grid.innerHTML = modCards();
    } else if (el.dataset.input === 'mem') {
      const label = document.getElementById('memlabel');
      if (label) label.textContent = `${(Number(el.value) / 1024).toFixed(1)} GB`;
    }
  });

  document.addEventListener('change', (e) => {
    if (e.target.dataset.input === 'mem') {
      saveSettings({ memoryMB: Number(e.target.value) });
    }
  });

  window.sushi.on('game:status', (d) => {
    S.status = d.text;
    S.busy = true;
    refreshLive();
  });
  window.sushi.on('game:log', (d) => {
    String(d.text).split(/\r?\n/).filter(Boolean).forEach((l) => S.logs.push(l));
    if (S.logs.length > 1500) S.logs.splice(0, S.logs.length - 1500);
    refreshLive();
  });
  window.sushi.on('game:exit', (d) => {
    S.running = false;
    S.busy = false;
    S.status = `Game closed (code ${d.code})`;
    S.logs.push(`Minecraft exited with code ${d.code}`);
    render();
  });
  window.sushi.on('ms:code', (d) => {
    S.msCode = d;
    if (S.page === 'accounts') render();
  });

  async function boot() {
    const [settings, accounts, catalog, presets, versions] = await Promise.all([
      window.sushi.settings.get(),
      window.sushi.accounts.list(),
      window.sushi.mods.catalog(),
      window.sushi.fps.presets(),
      window.sushi.versions.list().catch(() => []),
    ]);
    S.settings = settings;
    S.accounts = accounts;
    S.catalog = catalog;
    S.presets = presets;
    S.versions = versions;
    S.editId = settings.activeProfileId;
    render();
  }

  boot().catch((err) => {
    app.innerHTML = `<div class="empty" style="margin:auto">Could not start: ${esc(err.message)}</div>`;
  });
})();
