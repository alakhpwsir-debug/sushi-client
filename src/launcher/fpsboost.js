// FPS Boost presets: JVM tuning + options.txt tweaks.
const fs = require('fs');
const path = require('path');

const G1_TUNING = [
  '-XX:+UseG1GC',
  '-XX:+ParallelRefProcEnabled',
  '-XX:MaxGCPauseMillis=50',
  '-XX:+DisableExplicitGC',
  '-XX:+UseStringDeduplication',
];

const FPS_PRESETS = {
  off: {
    label: 'Off',
    description: 'Vanilla Java and graphics settings. Nothing is changed.',
    jvm: [],
    options: {},
  },
  balanced: {
    label: 'Balanced',
    description: 'Tuned G1 garbage collector, lower particles and shadows. Good for most PCs.',
    jvm: G1_TUNING,
    options: {
      maxFps: '144',
      enableVsync: 'false',
      graphicsMode: '0',
      renderDistance: '10',
      particles: '1',
      entityShadows: 'false',
      mipmapLevels: '2',
      biomeBlendRadius: '2',
    },
  },
  max: {
    label: 'Max FPS',
    description: 'Lowest visual quality, highest frame rate, capped to your monitor\'s refresh rate. Uses ZGC on Java 17+.',
    jvm: null, // chosen per Java version in presetJvmArgs
    options: {
      maxFps: '260',
      enableVsync: 'false',
      graphicsMode: '0',
      renderDistance: '6',
      particles: '2',
      entityShadows: 'false',
      mipmapLevels: '0',
      biomeBlendRadius: '0',
      ao: 'false',
    },
  },
};

function presetJvmArgs(key, javaMajor = 8) {
  const preset = FPS_PRESETS[key] || FPS_PRESETS.off;
  if (key === 'max') {
    // ZUncommitDelay: give unused heap back to Windows after 30 s (default 300 s).
    if (javaMajor >= 21) return ['-XX:+UseZGC', '-XX:+ZGenerational', '-XX:ZUncommitDelay=30'];
    if (javaMajor >= 17) return ['-XX:+UseZGC', '-XX:ZUncommitDelay=30'];
    return G1_TUNING;
  }
  return preset.jvm;
}

// Frames above the monitor's refresh rate are never shown, so they only waste GPU and CPU.
// hz: the primary display's refresh rate (0 when unknown, which keeps the preset value).
function fitMaxFps(options, hz) {
  if (!options || !options.maxFps || !hz) return options;
  return { ...options, maxFps: String(Math.min(Number(options.maxFps), hz)) };
}

// Merges key:value pairs into options.txt (keeps every other setting).
function applyOptionsTxt(gameDir, options) {
  if (!options || Object.keys(options).length === 0) return;
  fs.mkdirSync(gameDir, { recursive: true });
  const file = path.join(gameDir, 'options.txt');
  const map = new Map();
  if (fs.existsSync(file)) {
    for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
      const i = line.indexOf(':');
      if (i > 0) map.set(line.slice(0, i), line.slice(i + 1));
    }
  }
  for (const [k, v] of Object.entries(options)) map.set(k, v);
  fs.writeFileSync(file, [...map].map(([k, v]) => `${k}:${v}`).join('\n') + '\n');
}

module.exports = { FPS_PRESETS, presetJvmArgs, applyOptionsTxt, fitMaxFps };
