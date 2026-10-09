// Sushi Client - Electron main process (talks to the launcher core in src/launcher).
const { app, BrowserWindow, ipcMain, shell, safeStorage } = require('electron');
const path = require('path');
const fs = require('fs');
const { ROOT, dirs } = require('./src/launcher/paths');
const core = require('./src/launcher');
const auth = require('./src/launcher/auth');

const paths = dirs(ROOT);

const DEFAULT_MODS = ['fabric-api', 'sushi-core', 'sodium', 'lithium', 'ferrite-core', 'modernfix', 'entityculling', 'immediatelyfast', 'modmenu'];

// Profiles bundle a version, loader, FPS preset and mod list. The active one is used for Launch.
const DEFAULT_PROFILES = [
  { id: 'sushi', name: 'Sushi Default', mc: '1.21.1', fabric: true, fpsPreset: 'max', mods: DEFAULT_MODS },
  { id: 'balanced', name: 'Balanced', mc: '1.21.1', fabric: true, fpsPreset: 'balanced', mods: DEFAULT_MODS },
  { id: 'vanilla', name: 'Vanilla 1.21.1', mc: '1.21.1', fabric: false, fpsPreset: 'off', mods: [] },
];

const DEFAULT_SETTINGS = {
  memoryMB: 6144,
  javaPath: '',
  msClientId: '',
  width: 1280,
  height: 720,
  profiles: DEFAULT_PROFILES,
  activeProfileId: 'sushi',
};

function readJson(file, fallback) {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    return fallback;
  }
}

function writeJson(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(data, null, 2));
}

const readSettings = () => ({ ...DEFAULT_SETTINGS, ...readJson(paths.settings, {}) });

// Tokens are encrypted with the OS keychain (DPAPI on Windows) when available.
function encryptSecret(obj) {
  const text = JSON.stringify(obj);
  if (safeStorage.isEncryptionAvailable()) {
    return { enc: true, data: safeStorage.encryptString(text).toString('base64') };
  }
  return { enc: false, data: Buffer.from(text).toString('base64') };
}

function decryptSecret(box) {
  const buf = Buffer.from(box.data, 'base64');
  return JSON.parse(box.enc ? safeStorage.decryptString(buf) : buf.toString('utf8'));
}

const loadStore = () => readJson(paths.accounts, { activeId: null, accounts: [] });
const publicAccount = (a) => ({ id: a.id, type: a.type, username: a.username, uuid: a.uuid });

function saveAccount(acc) {
  const store = loadStore();
  const rec = {
    id: acc.uuid,
    type: acc.type,
    username: acc.username,
    uuid: acc.uuid,
    expiresAt: acc.expiresAt || null,
    box: encryptSecret({ accessToken: acc.accessToken, refreshToken: acc.refreshToken || null }),
  };
  store.accounts = store.accounts.filter((a) => a.id !== rec.id).concat(rec);
  store.activeId = rec.id;
  writeJson(paths.accounts, store);
  return publicAccount(rec);
}

// Returns a launch-ready account, refreshing the Microsoft token when needed.
async function activeAccount() {
  const store = loadStore();
  const rec = store.accounts.find((a) => a.id === store.activeId);
  if (!rec) throw new Error('No account selected. Add one on the Accounts page first.');

  if (rec.type === 'offline') return { ...rec, accessToken: '0' };

  const secret = decryptSecret(rec.box);
  if (!rec.expiresAt || rec.expiresAt - Date.now() < 5 * 60 * 1000) {
    const clientId = readSettings().msClientId;
    const ms = await auth.msRefresh(clientId, secret.refreshToken);
    const fresh = await auth.minecraftLogin(ms.access_token, ms.refresh_token || secret.refreshToken);
    saveAccount(fresh);
    return fresh;
  }
  return { ...rec, accessToken: secret.accessToken };
}

function registerIpc() {
  ipcMain.handle('settings:get', () => readSettings());
  ipcMain.handle('settings:set', (_e, patch) => {
    const next = { ...readSettings(), ...patch };
    writeJson(paths.settings, next);
    return next;
  });

  ipcMain.handle('settings:resetProfiles', () => {
    const next = { ...readSettings(), profiles: DEFAULT_PROFILES, activeProfileId: DEFAULT_SETTINGS.activeProfileId };
    writeJson(paths.settings, next);
    return next;
  });

  ipcMain.handle('versions:list', () => core.listVersions());

  ipcMain.handle('accounts:list', () => {
    const store = loadStore();
    return { activeId: store.activeId, accounts: store.accounts.map(publicAccount) };
  });
  ipcMain.handle('accounts:offline', (_e, name) => {
    const clean = String(name || '').trim();
    if (!/^[A-Za-z0-9_]{3,16}$/.test(clean)) throw new Error('Offline names must be 3-16 letters, numbers or _');
    return saveAccount(auth.offlineAccount(clean));
  });
  ipcMain.handle('accounts:microsoft', async (e) => {
    const clientId = readSettings().msClientId;
    if (!clientId) throw new Error('Paste your Azure Client ID in Settings first.');
    const dc = await auth.msDeviceStart(clientId);
    e.sender.send('ms:code', { code: dc.user_code, url: dc.verification_uri });
    shell.openExternal(dc.verification_uri);
    const token = await auth.msDevicePoll(clientId, dc);
    const acc = await auth.minecraftLogin(token.access_token, token.refresh_token);
    return saveAccount(acc);
  });
  ipcMain.handle('accounts:select', (_e, id) => {
    const store = loadStore();
    store.activeId = id;
    writeJson(paths.accounts, store);
    return true;
  });
  ipcMain.handle('accounts:remove', (_e, id) => {
    const store = loadStore();
    store.accounts = store.accounts.filter((a) => a.id !== id);
    if (store.activeId === id) store.activeId = store.accounts[0] ? store.accounts[0].id : null;
    writeJson(paths.accounts, store);
    return true;
  });

  ipcMain.handle('game:launch', async (e, profileId) => {
    const s = readSettings();
    const profile = s.profiles.find((p) => p.id === (profileId || s.activeProfileId));
    if (!profile) throw new Error('That profile no longer exists.');
    const account = await activeAccount();
    const emit = (channel, data) => {
      if (!e.sender.isDestroyed()) e.sender.send(channel, data);
    };
    return core.launchGame(
      {
        root: ROOT,
        mc: profile.mc,
        fabric: profile.fabric,
        account,
        memoryMB: s.memoryMB,
        javaPath: s.javaPath,
        fpsPreset: profile.fpsPreset,
        mods: profile.fabric ? profile.mods : [],
        width: s.width,
        height: s.height,
        bundledModsDir: path.join(__dirname, 'assets', 'mods'),
      },
      emit,
    );
  });

  ipcMain.handle('mods:catalog', () => core.MOD_CATALOG);
  ipcMain.handle('fps:presets', () =>
    Object.fromEntries(Object.entries(core.FPS_PRESETS).map(([k, p]) => [k, { label: p.label, description: p.description }])),
  );
  ipcMain.handle('app:openFolder', () => shell.openPath(ROOT));
  ipcMain.handle('app:openUrl', (_e, url) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url);
  });
  ipcMain.handle('app:root', () => ROOT);
}

let win = null;

function createWindow() {
  win = new BrowserWindow({
    width: 1360,
    height: 840,
    minWidth: 980,
    minHeight: 640,
    title: 'Sushi Client',
    backgroundColor: '#070910',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });
  win.loadFile(path.join(__dirname, 'src', 'renderer', 'index.html'));
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (win) {
      if (win.isMinimized()) win.restore();
      win.focus();
    }
  });
  app.whenReady().then(() => {
    fs.mkdirSync(ROOT, { recursive: true });
    registerIpc();
    createWindow();
  });
  app.on('window-all-closed', () => app.quit());
}
