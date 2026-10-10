// Orchestrates a launch: install -> mods + content -> FPS preset -> java -> spawn game.
const fs = require('fs');
const path = require('path');
const { dirs } = require('./paths');
const { installVersion } = require('./install');
const { ensureJava } = require('./java');
const { buildCommand } = require('./launch');
const { FPS_PRESETS, presetJvmArgs, applyOptionsTxt } = require('./fpsboost');
const { installMods, MOD_CATALOG } = require('./mods');
const { installFabric } = require('./fabric');
const { listVersions } = require('./versions');
const { startGameProcess, coreReserveArgs } = require('./gameprocess');
const { libraryDir } = require('./content');

/**
 * opts: { root, mc, fabric, account, memoryMB, javaPath, fpsPreset, mods, content,
 *         width, height, quickServer, bundledModsDir }
 * content: [{ type: 'mod' | 'resourcepack' | 'shader', file, name }] from the profile, files live in the library.
 * emit(channel, payload): used to stream status/log lines to the UI.
 * Resolves with the game process id once Minecraft has started.
 */

// --quickPlayMultiplayer exists from 1.20 onward. Older versions would reject it.
function supportsQuickPlay(mc) {
  return /^1\.2\d(\.|$)/.test(mc);
}

function copyFromLibrary(root, type, file, destDir, status) {
  const src = path.join(libraryDir(root, type), file);
  if (!fs.existsSync(src)) {
    status(`${file}: missing from the library, skipped`);
    return false;
  }
  fs.mkdirSync(destDir, { recursive: true });
  fs.copyFileSync(src, path.join(destDir, file));
  return true;
}

async function launchGame(opts, emit) {
  const { root, mc, fabric, account, bundledModsDir } = opts;
  const content = Array.isArray(opts.content) ? opts.content : [];
  const d = dirs(root);
  const status = (text) => emit('game:status', { text });

  const versionId = fabric ? await installFabric(root, mc, status) : mc;
  const inst = await installVersion(root, versionId, status);

  const gameDir = path.join(d.instances, versionId);
  fs.mkdirSync(gameDir, { recursive: true });

  // Shader packs run through Iris, so Iris is added to the mod list when a shader is picked.
  const shader = fabric ? content.find((c) => c.type === 'shader') : null;
  const mods = (opts.mods || []).slice();
  if (shader && !mods.includes('iris')) mods.push('iris');

  if (fabric) {
    status('Installing mods...');
    const extraMods = content
      .filter((c) => c.type === 'mod')
      .map((c) => path.join(libraryDir(root, 'mod'), c.file));
    await installMods(gameDir, mc, mods, status, bundledModsDir, extraMods);
  }

  // Resource packs: copy into the instance and list them in options.txt. Vanilla stays first.
  const packFiles = content
    .filter((c) => c.type === 'resourcepack')
    .filter((c) => copyFromLibrary(root, 'resourcepack', c.file, path.join(gameDir, 'resourcepacks'), status))
    .map((c) => `"file/${c.file}"`);

  // Shader packs: copy into the instance and point Iris at the chosen one.
  if (shader && copyFromLibrary(root, 'shader', shader.file, path.join(gameDir, 'shaderpacks'), status)) {
    const irisDir = path.join(gameDir, 'config');
    fs.mkdirSync(irisDir, { recursive: true });
    fs.writeFileSync(path.join(irisDir, 'iris.properties'), `shaderPack=${shader.file}\nenableShaders=true\n`);
  }

  const preset = FPS_PRESETS[opts.fpsPreset] || FPS_PRESETS.off;
  applyOptionsTxt(gameDir, { ...preset.options, resourcePacks: `[${['"vanilla"', ...packFiles].join(',')}]` });

  const javaPath = opts.javaPath || (await ensureJava(root, inst.javaMajor, status));
  const extraJvm = [...presetJvmArgs(opts.fpsPreset, inst.javaMajor), ...coreReserveArgs()];

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
    width: opts.width || 1280,
    height: opts.height || 720,
    quickServer: supportsQuickPlay(mc) ? opts.quickServer || null : null,
  });

  status('Starting Minecraft...');
  const child = startGameProcess({
    exe,
    args,
    cwd: gameDir,
    logFile: path.join(root, 'logs', 'game-latest.log'),
    emit,
  });
  return child.pid;
}

module.exports = { launchGame, listVersions, FPS_PRESETS, MOD_CATALOG };
