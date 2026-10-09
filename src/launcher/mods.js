// Built-in mod catalog. Most mods come from Modrinth (open-source, Fabric).
// "sushi-core" is our own in-game HUD and ships inside the launcher (assets/mods).
const fs = require('fs');
const path = require('path');
const { fetchJson, downloadFile } = require('./http');

const SUSHI_CORE_MC = '1.21.1';

const MOD_CATALOG = [
  { slug: 'sushi-core', name: 'Sushi Core', category: 'Client HUD', recommended: true, bundled: true, mcOnly: SUSHI_CORE_MC, desc: 'Sushi in-game HUD: FPS, CPS, keystrokes, ping and coordinates. Right Shift toggles it.' },
  { slug: 'sodium', name: 'Sodium', category: 'Performance', recommended: true, desc: 'Rebuilt rendering engine. The biggest FPS gain.' },
  { slug: 'lithium', name: 'Lithium', category: 'Performance', recommended: true, desc: 'Game-logic optimizations (ticks, AI, physics).' },
  { slug: 'ferrite-core', name: 'FerriteCore', category: 'Performance', recommended: true, desc: 'Cuts memory usage.' },
  { slug: 'entityculling', name: 'Entity Culling', category: 'Performance', recommended: true, desc: 'Skips drawing entities you cannot see.' },
  { slug: 'immediatelyfast', name: 'ImmediatelyFast', category: 'Performance', recommended: true, desc: 'Faster immediate-mode rendering (HUD, text, maps).' },
  { slug: 'dynamic-fps', name: 'Dynamic FPS', category: 'Performance', recommended: false, desc: 'Lowers FPS when the window is unfocused or minimized.' },
  { slug: 'modernfix', name: 'ModernFix', category: 'Performance', recommended: true, desc: 'Faster startup, lower memory, fewer stutters.' },
  { slug: 'sodium-extra', name: 'Sodium Extra', category: 'Visuals', recommended: false, desc: 'More Sodium options: fog, sky, and fade settings.' },
  { slug: 'reeses-sodium-options', name: "Reese's Sodium Options", category: 'Visuals', recommended: false, desc: 'Adds a full video settings screen for Sodium.' },
  { slug: 'modmenu', name: 'Mod Menu', category: 'Utility', recommended: true, desc: 'In-game menu to configure your mods.' },
  { slug: 'iris', name: 'Iris Shaders', category: 'Visuals', recommended: false, desc: 'Shader support (works with Sodium).' },
];

// Installs the enabled mods for one Minecraft version and removes mods that were
// turned off since the last launch (tracked in mods/.sushi-mods.json).
async function installMods(gameDir, mc, enabledSlugs, log = () => {}, bundledDir) {
  const modsDir = path.join(gameDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  const manifestFile = path.join(modsDir, '.sushi-mods.json');
  const previous = fs.existsSync(manifestFile) ? JSON.parse(fs.readFileSync(manifestFile, 'utf8')) : {};
  const next = {};

  const q = `loaders=${encodeURIComponent('["fabric"]')}&game_versions=${encodeURIComponent(JSON.stringify([mc]))}`;

  for (const slug of enabledSlugs) {
    const entry = MOD_CATALOG.find((m) => m.slug === slug);

    if (entry && entry.bundled) {
      if (mc !== SUSHI_CORE_MC) {
        log(`${entry.name}: only available for Minecraft ${SUSHI_CORE_MC}, skipped`);
        continue;
      }
      const src = path.join(bundledDir || '', `sushi-core-${mc}.jar`);
      if (!bundledDir || !fs.existsSync(src)) {
        log(`${entry.name}: bundled jar not found, skipped`);
        continue;
      }
      const filename = `sushi-core-${mc}.jar`;
      fs.writeFileSync(path.join(modsDir, filename), fs.readFileSync(src)); // readFileSync works inside app.asar
      next[slug] = filename;
      log(`${entry.name}: ready`);
      continue;
    }

    const versions = await fetchJson(`https://api.modrinth.com/v2/project/${slug}/version?${q}`);
    if (!versions.length) {
      log(`${slug}: no Fabric build for ${mc}, skipped`);
      continue;
    }
    const file = versions[0].files.find((f) => f.primary) || versions[0].files[0];
    await downloadFile(file.url, path.join(modsDir, file.filename), file.hashes && file.hashes.sha1);
    next[slug] = file.filename;
    log(`${slug}: ready`);
  }

  for (const [slug, filename] of Object.entries(previous)) {
    if (next[slug] !== filename) fs.rmSync(path.join(modsDir, filename), { force: true });
  }
  fs.writeFileSync(manifestFile, JSON.stringify(next, null, 2));
  return next;
}

module.exports = { MOD_CATALOG, installMods, SUSHI_CORE_MC };
