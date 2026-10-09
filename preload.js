// Safe bridge between the UI and the main process. Only these channels are exposed.
const { contextBridge, ipcRenderer } = require('electron');

const invoke = (channel) => (...args) => ipcRenderer.invoke(channel, ...args);
const EVENTS = ['game:status', 'game:log', 'game:exit', 'ms:code'];

contextBridge.exposeInMainWorld('sushi', {
  settings: { get: invoke('settings:get'), set: invoke('settings:set'), resetProfiles: invoke('settings:resetProfiles') },
  versions: { list: invoke('versions:list') },
  accounts: {
    list: invoke('accounts:list'),
    offline: invoke('accounts:offline'),
    microsoft: invoke('accounts:microsoft'),
    select: invoke('accounts:select'),
    remove: invoke('accounts:remove'),
  },
  game: { launch: invoke('game:launch') },
  content: { get: invoke('content:get') },
  profile: { openMods: invoke('profile:openMods') },
  mods: { catalog: invoke('mods:catalog') },
  fps: { presets: invoke('fps:presets') },
  app: { root: invoke('app:root'), openFolder: invoke('app:openFolder'), openUrl: invoke('app:openUrl'), copy: invoke('app:copy') },
  on: (channel, cb) => {
    if (EVENTS.includes(channel)) ipcRenderer.on(channel, (_e, data) => cb(data));
  },
});
