const fs = require('fs');
const path = require('path');
const { fetchJson } = require('./http');
const { dirs } = require('./paths');

const MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';

let manifestCache = null;

async function listVersions() {
  if (!manifestCache) manifestCache = await fetchJson(MANIFEST_URL);
  return manifestCache.versions.map((v) => ({
    id: v.id,
    type: v.type,
    url: v.url,
    releaseTime: v.releaseTime,
  }));
}

function versionFile(root, id) {
  return path.join(dirs(root).versions, id, `${id}.json`);
}

// Raw version JSON: a vanilla version from Mojang, or a locally stored
// profile (Fabric) that inherits from a vanilla version.
async function getRawVersion(root, id) {
  const local = versionFile(root, id);
  if (fs.existsSync(local)) return JSON.parse(fs.readFileSync(local, 'utf8'));

  const entry = (await listVersions()).find((v) => v.id === id);
  if (!entry) throw new Error(`Unknown Minecraft version: ${id}`);
  const json = await fetchJson(entry.url);
  fs.mkdirSync(path.dirname(local), { recursive: true });
  fs.writeFileSync(local, JSON.stringify(json, null, 2));
  return json;
}

// Libraries are de-duplicated by group:artifact[:classifier]; child entries win.
function dedupeLibraries(libs) {
  const seen = new Set();
  return libs.filter((lib) => {
    const p = lib.name.split(':');
    const key = [p[0], p[1], p[3] || ''].join(':');
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

// Resolves inheritsFrom chains into one flat version description.
async function resolveVersion(root, id, depth = 0) {
  if (depth > 5) throw new Error(`inheritsFrom loop at ${id}`);
  const raw = await getRawVersion(root, id);

  if (!raw.inheritsFrom) {
    return { ...raw, jarId: id };
  }

  const parent = await resolveVersion(root, raw.inheritsFrom, depth + 1);
  const hasArgs = parent.arguments || raw.arguments;

  return {
    ...parent,
    ...raw,
    id,
    jarId: parent.jarId,
    libraries: dedupeLibraries([...(raw.libraries || []), ...(parent.libraries || [])]),
    mainClass: raw.mainClass || parent.mainClass,
    arguments: hasArgs
      ? {
          jvm: [...((parent.arguments && parent.arguments.jvm) || []), ...((raw.arguments && raw.arguments.jvm) || [])],
          game: [...((parent.arguments && parent.arguments.game) || []), ...((raw.arguments && raw.arguments.game) || [])],
        }
      : undefined,
    minecraftArguments: raw.minecraftArguments || parent.minecraftArguments,
    assetIndex: parent.assetIndex,
    downloads: parent.downloads,
    javaVersion: parent.javaVersion || { component: 'jre-legacy', majorVersion: 8 },
    type: parent.type,
  };
}

module.exports = { listVersions, getRawVersion, resolveVersion };
