// Built-in mod catalog. Mods are fetched from Modrinth (official API, open source mods only).
const fs = require('fs');
const path = require('path');
const { fetchJson, downloadFile } = require('./http');

const MOD_CATALOG = [
  { slug: 'sodium', name: 'Sodium', category: 'Performance', recommended: true, desc: 'Rebuilt rendering engine. Big FPS gains.' },
  { slug: 'lithium', name: 'Lithium', category: 'Performance', recommended: true, desc: 'Game-logic optimizations (ticks, AI, physics).' },
  { slug: 'ferrite-core', name: 'FerriteCore', category: 'Performance', recommended: true, desc: 'Cuts memory usage.' },
  { slug: 'immediatelyfast', name: 'ImmediatelyFast', category: 'Performance', recommended: false, desc: 'Faster immediate-mode rendering (HUD, text).' },
  { slug: 'entityculling', name: 'Entity Culling', category: 'Performance', recommended: false, desc: 'Skips drawing entities you cannot see.' },
  { slug: 'modmenu', name: 'Mod Menu', category: 'Utility', recommended: true, desc: 'In-game menu to configure your mods.' },
  { slug: 'iris', name: 'Iris Shaders', category: 'Visuals', recommended: false, desc: 'Shader support (works with Sodium).' },
];

// Installs the enabled mods for one Minecraft version and removes mods that were
// turned off since the last launch (tracked in mods/.sushi-mods.json).
async function installMods(gameDir, mc, enabledSlugs, log = () => {}) {
  const modsDir = path.join(gameDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  const manifestFile = path.join(modsDir, '.sushi-mods.json');
  const previous = fs.existsSync(manifestFile) ? JSON.parse(fs.readFileSync(manifestFile, 'utf8')) : {};
  const next = {};

  const q = `loaders=${encodeURIComponent('["fabric"]')}&game_versions=${encodeURIComponent(JSON.stringify([mc]))}`;

  for (const slug of enabledSlugs) {
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

module.exports = { MOD_CATALOG, installMods };
