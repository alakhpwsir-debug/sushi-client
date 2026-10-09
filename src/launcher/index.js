// Orchestrates a launch: install -> mods -> FPS preset -> java -> spawn game.
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { dirs } = require('./paths');
const { installVersion } = require('./install');
const { ensureJava } = require('./java');
const { buildCommand } = require('./launch');
const { FPS_PRESETS, presetJvmArgs, applyOptionsTxt } = require('./fpsboost');
const { installMods, MOD_CATALOG } = require('./mods');
const { installFabric } = require('./fabric');
const { listVersions } = require('./versions');

/**
 * opts: { root, mc, fabric, account, memoryMB, javaPath, fpsPreset, mods }
 * emit(channel, payload): used to stream status/log lines to the UI.
 * Resolves with the game process id once Minecraft has started.
 */
async function launchGame(opts, emit) {
  const { root, mc, fabric, account } = opts;
  const d = dirs(root);
  const status = (text) => emit('game:status', { text });

  const versionId = fabric ? await installFabric(root, mc, status) : mc;
  const inst = await installVersion(root, versionId, status);

  const gameDir = path.join(d.instances, versionId);
  fs.mkdirSync(gameDir, { recursive: true });

  if (fabric && opts.mods && opts.mods.length) {
    status('Installing mods...');
    await installMods(gameDir, mc, opts.mods, status, opts.bundledModsDir);
  }

  const preset = FPS_PRESETS[opts.fpsPreset] || FPS_PRESETS.off;
  applyOptionsTxt(gameDir, preset.options);

  const javaPath = opts.javaPath || (await ensureJava(root, inst.javaMajor, status));
  const extraJvm = presetJvmArgs(opts.fpsPreset, inst.javaMajor);

  const { exe, args } = buildCommand({
    root,
    resolved: inst.resolved,
    versionId,
    classpath: inst.classpath,
    nativesDir: inst.nativesDir,
    assetsIndexId: inst.assetsIndexId,
    gameDir,
    account,
    javaPath,
    memoryMB: opts.memoryMB || 4096,
    extraJvm,
  });

  status('Starting Minecraft...');
  const child = spawn(exe, args, { cwd: gameDir, windowsHide: true });
  child.stdout.on('data', (b) => emit('game:log', { text: b.toString() }));
  child.stderr.on('data', (b) => emit('game:log', { text: b.toString() }));
  child.on('error', (err) => emit('game:status', { text: `Could not start Java: ${err.message}` }));
  child.on('exit', (code) => emit('game:exit', { code }));
  return child.pid;
}

module.exports = { launchGame, listVersions, FPS_PRESETS, MOD_CATALOG };
