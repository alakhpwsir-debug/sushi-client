// Sushi Client UI logic. Talks to the main process through window.sushi (see preload.js).
const $ = (sel) => document.querySelector(sel);
const api = window.sushi;

let settings = {};
let presetLabels = {};
let busy = false;

function show(view) {
  document.querySelectorAll('.view').forEach((el) => el.classList.toggle('active', el.id === `view-${view}`));
  document.querySelectorAll('.rail-btn').forEach((el) => el.classList.toggle('active', el.dataset.view === view));
}

function status(text) {
  $('#status').textContent = text;
}

function setProgress(pct) {
  $('#progress').style.width = `${pct}%`;
}

function consoleLog(text) {
  const c = $('#console');
  c.textContent += text;
  c.scrollTop = c.scrollHeight;
}

async function saveSettings(patch) {
  settings = await api.settings.set(patch);
  refreshHome();
}

function refreshHome() {
  $('#homePreset').textContent = presetLabels[settings.fpsPreset] || 'Max FPS';
  const count = (settings.enabledMods || []).length;
  $('#homeMods').textContent = `${count} mod${count === 1 ? '' : 's'} enabled`;
}

function renderMods(enabled, catalog) {
  const list = $('#modList');
  list.innerHTML = '';
  for (const mod of catalog) {
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = `
      <div class="tag"></div>
      <h3></h3>
      <p class="muted small"></p>
      <label class="toggle"><input type="checkbox"> Enabled</label>`;
    card.querySelector('.tag').textContent = mod.mcOnly ? `${mod.category} · ${mod.mcOnly} only` : mod.category;
    card.querySelector('h3').textContent = mod.name;
    card.querySelector('p').textContent = mod.desc;
    const box = card.querySelector('input');
    box.checked = enabled.includes(mod.slug);
    card.classList.toggle('selected', box.checked);
    box.addEventListener('change', async () => {
      const set = new Set(settings.enabledMods || []);
      if (box.checked) set.add(mod.slug);
      else set.delete(mod.slug);
      await saveSettings({ enabledMods: [...set] });
      card.classList.toggle('selected', box.checked);
    });
    list.appendChild(card);
  }
}

async function renderFps(current) {
  const presets = await api.fps.presets();
  presetLabels = Object.fromEntries(Object.entries(presets).map(([k, p]) => [k, p.label]));
  const list = $('#fpsList');
  list.innerHTML = '';
  for (const [key, p] of Object.entries(presets)) {
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = `<h3></h3><p class="muted small"></p><button class="btn small"></button>`;
    card.querySelector('h3').textContent = p.label;
    card.querySelector('p').textContent = p.description;
    const btn = card.querySelector('button');
    const isOn = key === current;
    btn.textContent = isOn ? 'Selected' : 'Use this preset';
    btn.className = `btn small ${isOn ? 'primary' : ''}`;
    card.classList.toggle('selected', isOn);
    btn.addEventListener('click', async () => {
      await saveSettings({ fpsPreset: key });
      renderFps(key);
      status(`FPS preset: ${p.label}`);
    });
    list.appendChild(card);
  }
  refreshHome();
}

async function renderAccounts() {
  const { activeId, accounts } = await api.accounts.list();
  const list = $('#accountList');
  list.innerHTML = '';
  const active = accounts.find((a) => a.id === activeId);
  $('#activeUser').textContent = active ? active.username : 'No account';

  if (!accounts.length) {
    list.innerHTML = '<p class="muted">No accounts yet.</p>';
    return;
  }
  for (const a of accounts) {
    const row = document.createElement('div');
    row.className = `row ${a.id === activeId ? 'active' : ''}`;
    row.innerHTML = `<div><b></b><small></small></div><div class="row-actions compact"><button class="btn small use"></button><button class="btn small danger">Remove</button></div>`;
    row.querySelector('b').textContent = a.username;
    row.querySelector('small').textContent = `${a.type === 'microsoft' ? 'Microsoft' : 'Offline'} · ${a.uuid}`;
    const use = row.querySelector('.use');
    use.textContent = a.id === activeId ? 'Active' : 'Use';
    use.disabled = a.id === activeId;
    use.addEventListener('click', async () => {
      await api.accounts.select(a.id);
      renderAccounts();
    });
    row.querySelector('.danger').addEventListener('click', async () => {
      await api.accounts.remove(a.id);
      renderAccounts();
    });
    list.appendChild(row);
  }
}

async function launch() {
  if (busy) return;
  const mc = $('#versionSelect').value;
  const fabric = $('#fabricToggle').checked;
  busy = true;
  $('#playBtn').disabled = true;
  $('#playBtn').textContent = 'LOADING';
  setProgress(15);
  await saveSettings({ lastVersion: mc, fabric });
  consoleLog(`\n> Launching Minecraft ${mc}${fabric ? ' + Fabric' : ''}\n`);
  try {
    const pid = await api.game.launch({ mc, fabric });
    status(`Minecraft is running (pid ${pid})`);
    setProgress(100);
    $('#playBtn').textContent = 'RUNNING';
  } catch (err) {
    status(`Error: ${err.message}`);
    consoleLog(`\n[error] ${err.message}\n`);
    setProgress(0);
    $('#playBtn').textContent = 'PLAY';
    $('#playBtn').disabled = false;
    busy = false;
  }
}

async function init() {
  document.querySelectorAll('.rail-btn').forEach((el) => el.addEventListener('click', () => show(el.dataset.view)));
  document.querySelectorAll('[data-go]').forEach((el) => el.addEventListener('click', () => show(el.dataset.go)));
  $('#activeUser').addEventListener('click', () => show('accounts'));

  settings = await api.settings.get();

  const versions = await api.versions.list();
  const sel = $('#versionSelect');
  versions.filter((v) => v.type === 'release').forEach((v) => sel.add(new Option(v.id, v.id)));
  sel.value = settings.lastVersion;
  $('#fabricToggle').checked = Boolean(settings.fabric);
  sel.addEventListener('change', () => saveSettings({ lastVersion: sel.value }));
  $('#fabricToggle').addEventListener('change', (e) => saveSettings({ fabric: e.target.checked }));

  const memory = $('#memory');
  memory.value = settings.memoryMB;
  const showMem = () => ($('#memoryOut').textContent = `${(memory.value / 1024).toFixed(1)} GB`);
  memory.addEventListener('input', showMem);
  showMem();
  $('#javaPath').value = settings.javaPath || '';
  $('#msClientId').value = settings.msClientId || '';

  renderMods(settings.enabledMods || [], await api.mods.catalog());
  await renderFps(settings.fpsPreset);
  await renderAccounts();

  $('#playBtn').addEventListener('click', launch);

  $('#saveSettings').addEventListener('click', async () => {
    await saveSettings({
      memoryMB: Number(memory.value),
      javaPath: $('#javaPath').value.trim(),
      msClientId: $('#msClientId').value.trim(),
    });
    status('Settings saved');
  });
  $('#openFolder').addEventListener('click', () => api.app.openFolder());

  $('#offlineAdd').addEventListener('click', async () => {
    try {
      await api.accounts.offline($('#offlineName').value);
      $('#offlineName').value = '';
      status('Offline account added');
      renderAccounts();
    } catch (err) {
      status(`Error: ${err.message}`);
    }
  });

  $('#msLogin').addEventListener('click', async () => {
    $('#msLogin').disabled = true;
    status('Waiting for Microsoft sign-in...');
    try {
      await api.accounts.microsoft();
      status('Signed in with Microsoft');
      renderAccounts();
    } catch (err) {
      status(`Error: ${err.message}`);
    } finally {
      $('#msLogin').disabled = false;
      $('#msCode').classList.add('hidden');
    }
  });

  api.on('ms:code', ({ code, url }) => {
    $('#msCodeValue').textContent = code;
    $('#msCode').classList.remove('hidden');
    $('#msOpen').onclick = () => api.app.openUrl(url);
  });
  api.on('game:status', ({ text }) => {
    status(text);
    if (busy) setProgress(Math.min(90, Number(($('#progress').style.width || '15%').replace('%', '')) + 6));
  });
  api.on('game:log', ({ text }) => consoleLog(text));
  api.on('game:exit', ({ code }) => {
    status(`Minecraft closed (exit code ${code})`);
    consoleLog(`\n[game exited with code ${code}]\n`);
    setProgress(0);
    busy = false;
    $('#playBtn').disabled = false;
    $('#playBtn').textContent = 'PLAY';
  });
}

init().catch((err) => status(`Startup error: ${err.message}`));
