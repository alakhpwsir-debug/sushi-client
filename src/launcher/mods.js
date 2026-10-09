// Built-in mod catalog. Open-source mods are fetched from Modrinth; Sushi Core ships inside the launcher.
const fs = require('fs');
const path = require('path');
const { fetchJson, downloadFile } = require('./http');

// Mods the game cannot run without. They are always installed when Fabric is on and cannot be disabled.
const REQUIRED_SLUGS = ['fabric-api'];

// Sushi Core (HUD + in-game module menu) is bundled as assets/mods/sushi-core-<mc>.jar.
const BUNDLED_SLUGS = ['sushi-core'];

const MOD_CATALOG = [
  { slug: 'fabric-api', name: 'Fabric API', category: 'Required', required: true, desc: 'Core library every Fabric mod needs.' },
  { slug: 'sushi-core', name: 'Sushi Core', category: 'HUD', bundled: true, recommended: true, desc: 'Sushi HUD: FPS, CPS, keystrokes, ping, coordinates, and the Right Shift module menu.' },
  { slug: 'sodium', name: 'Sodium', category: 'Performance', recommended: true, desc: 'Rebuilt rendering engine. Big FPS gains.' },
  { slug: 'lithium', name: 'Lithium', category: 'Performance', recommended: true, desc: 'Game-logic optimizations (ticks, AI, physics).' },
  { slug: 'ferrite-core', name: 'FerriteCore', category: 'Performance', recommended: true, desc: 'Cuts memory usage.' },
  { slug: 'modernfix', name: 'ModernFix', category: 'Performance', recommended: true, desc: 'Faster startup and lower memory use.' },
  { slug: 'immediatelyfast', name: 'ImmediatelyFast', category: 'Performance', recommended: false, desc: 'Faster immediate-mode rendering (HUD, text).' },
  { slug: 'entityculling', name: 'Entity Culling', category: 'Performance', recommended: false, desc: 'Skips drawing entities you cannot see.' },
  { slug: 'modmenu', name: 'Mod Menu', category: 'Utility', recommended: true, desc: 'In-game menu to configure your mods.' },
  { slug: 'iris', name: 'Iris Shaders', category: 'Visuals', recommended: false, desc: 'Shader support (works with Sodium).' },
];

// Installs the enabled mods for one Minecraft version and removes mods that were
// turned off since the last launch (tracked in mods/.sushi-mods.json).
// Required mods throw on failure; optional ones are logged and skipped.
async function installMods(gameDir, mc, enabledSlugs, log = () => {}, bundledDir = null) {
  const modsDir = path.join(gameDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  const manifestFile = path.join(modsDir, '.sushi-mods.json');
  const previous = fs.existsSync(manifestFile) ? JSON.parse(fs.readFileSync(manifestFile, 'utf8')) : {};
  const next = {};

  const wanted = [...new Set([...REQUIRED_SLUGS, ...enabledSlugs])];
  const q = `loaders=${encodeURIComponent('["fabric"]')}&game_versions=${encodeURIComponent(JSON.stringify([mc]))}`;

  for (const slug of wanted) {
    const required = REQUIRED_SLUGS.includes(slug);
    const label = (MOD_CATALOG.find((m) => m.slug === slug) || { name: slug }).name;
    try {
      if (BUNDLED_SLUGS.includes(slug)) {
        const jar = bundledDir && findBundledJar(bundledDir, mc);
        if (!jar) throw new Error(`bundled jar for ${mc} is missing`);
        const filename = path.basename(jar);
        fs.copyFileSync(jar, path.join(modsDir, filename));
        next[slug] = filename;
        log(`${label}: ready (bundled)`);
        continue;
      }

      const versions = await fetchJson(`https://api.modrinth.com/v2/project/${slug}/version?${q}`);
      if (!versions.length) throw new Error(`no Fabric build for ${mc}`);
      const file = versions[0].files.find((f) => f.primary) || versions[0].files[0];
      await downloadFile(file.url, path.join(modsDir, file.filename), file.hashes && file.hashes.sha1);
      next[slug] = file.filename;
      log(`${label}: ready`);
    } catch (err) {
      if (required) throw new Error(`${label} is required but could not be installed: ${err.message}`);
      log(`${label}: skipped (${err.message})`);
    }
  }

  for (const [slug, filename] of Object.entries(previous)) {
    if (next[slug] !== filename) fs.rmSync(path.join(modsDir, filename), { force: true });
  }
  fs.writeFileSync(manifestFile, JSON.stringify(next, null, 2));
  return next;
}

function findBundledJar(bundledDir, mc) {
  const exact = path.join(bundledDir, `sushi-core-${mc}.jar`);
  if (fs.existsSync(exact)) return exact;
  const any = fs.readdirSync(bundledDir).find((f) => /^sushi-core-.*\.jar$/.test(f));
  return any ? path.join(bundledDir, any) : null;
}

module.exports = { MOD_CATALOG, REQUIRED_SLUGS, installMods };
