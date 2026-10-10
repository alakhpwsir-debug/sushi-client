// Mods, resource packs and shader packs from Modrinth (no key needed) and CurseForge (free API key).
// Downloads go into a shared library folder; each profile picks what it uses.
const fs = require('fs');
const path = require('path');
const { fetchJson, downloadFile } = require('./http');
const { dirs } = require('./paths');

const TYPES = {
  mod: { folder: 'mods', modrinth: 'mod', loaders: ['fabric'], curseClass: 6, curseLoader: 4 },
  resourcepack: { folder: 'resourcepacks', modrinth: 'resourcepack', loaders: ['minecraft'], curseClass: 12, curseLoader: null },
  shader: { folder: 'shaderpacks', modrinth: 'shader', loaders: ['iris'], curseClass: 6552, curseLoader: null },
};

const CURSE_GAME_ID = 432;
const CURSE_API = 'https://api.curseforge.com/v1';

function libraryDir(root, type) {
  const dir = path.join(dirs(root).root, 'library', TYPES[type].folder);
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

function assertType(type) {
  if (!TYPES[type]) throw new Error(`Unknown content type: ${type}`);
}

// ---------- Modrinth ----------
async function searchModrinth({ type, query, mc, limit = 20 }) {
  assertType(type);
  const facets = [[`project_type:${TYPES[type].modrinth}`], [`versions:${mc}`]];
  if (type === 'mod') facets.push(['categories:fabric']);
  if (type === 'shader') facets.push(['categories:iris']);
  const url = `https://api.modrinth.com/v2/search?query=${encodeURIComponent(query || '')}` +
    `&facets=${encodeURIComponent(JSON.stringify(facets))}&limit=${limit}&index=downloads`;
  const data = await fetchJson(url);
  return (data.hits || []).map((h) => ({
    source: 'modrinth',
    type,
    id: h.project_id,
    title: h.title,
    author: h.author,
    desc: h.description,
    icon: h.icon_url || '',
    downloads: h.downloads || 0,
  }));
}

async function installModrinth({ root, type, item, mc }) {
  const loaders = encodeURIComponent(JSON.stringify(TYPES[type].loaders));
  const gv = encodeURIComponent(JSON.stringify([mc]));
  const versions = await fetchJson(`https://api.modrinth.com/v2/project/${item.id}/version?loaders=${loaders}&game_versions=${gv}`);
  if (!versions.length) throw new Error(`${item.title} has no build for ${mc}`);
  const file = versions[0].files.find((f) => f.primary) || versions[0].files[0];
  if (!/^https:\/\//.test(file.url)) throw new Error('Refusing a non-https download link');
  const dest = path.join(libraryDir(root, type), file.filename);
  await downloadFile(file.url, dest, file.hashes && file.hashes.sha1);
  return file.filename;
}

// ---------- CurseForge ----------
function curseHeaders(apiKey) {
  return { 'x-api-key': apiKey, Accept: 'application/json' };
}

async function searchCurseForge({ type, query, mc, apiKey, limit = 20 }) {
  assertType(type);
  if (!apiKey) throw new Error('Add your CurseForge API key in Settings → Launcher first.');
  const t = TYPES[type];
  const params = new URLSearchParams({
    gameId: String(CURSE_GAME_ID),
    classId: String(t.curseClass),
    searchFilter: query || '',
    gameVersion: mc,
    pageSize: String(limit),
    sortField: '2',
    sortOrder: 'desc',
  });
  if (t.curseLoader) params.set('modLoaderType', String(t.curseLoader));
  const data = await fetchJson(`${CURSE_API}/mods/search?${params}`, { headers: curseHeaders(apiKey) });
  return (data.data || []).map((m) => ({
    source: 'curseforge',
    type,
    id: String(m.id),
    title: m.name,
    author: (m.authors && m.authors[0] && m.authors[0].name) || '',
    desc: m.summary || '',
    icon: (m.logo && (m.logo.thumbnailUrl || m.logo.url)) || '',
    downloads: m.downloadCount || 0,
  }));
}

async function installCurseForge({ root, type, item, mc, apiKey }) {
  if (!apiKey) throw new Error('Add your CurseForge API key in Settings → Launcher first.');
  const t = TYPES[type];
  const params = new URLSearchParams({ gameVersion: mc, pageSize: '20' });
  if (t.curseLoader) params.set('modLoaderType', String(t.curseLoader));
  const data = await fetchJson(`${CURSE_API}/mods/${item.id}/files?${params}`, { headers: curseHeaders(apiKey) });
  const file = (data.data || []).find((f) => f.gameVersions.includes(mc)
    && (type !== 'mod' || f.gameVersions.includes('Fabric')));
  if (!file) throw new Error(`${item.title} has no file for ${mc}`);
  if (!file.downloadUrl) {
    throw new Error(`${item.title} cannot be downloaded by the launcher (the author turned off third-party downloads). Download it from the CurseForge website and copy it into the library folder.`);
  }
  if (!/^https:\/\//.test(file.downloadUrl)) throw new Error('Refusing a non-https download link');
  const sha1 = (file.hashes || []).find((h) => h.algo === 1);
  const dest = path.join(libraryDir(root, type), file.fileName);
  await downloadFile(file.downloadUrl, dest, sha1 && sha1.value);
  return file.fileName;
}

// ---------- shared ----------
async function search({ source, type, query, mc, apiKey }) {
  if (source === 'curseforge') return searchCurseForge({ type, query, mc, apiKey });
  return searchModrinth({ type, query, mc });
}

// Downloads an item into the library. Returns the file name to store in the profile.
async function install({ root, source, type, item, mc, apiKey }) {
  assertType(type);
  const file = source === 'curseforge'
    ? await installCurseForge({ root, type, item, mc, apiKey })
    : await installModrinth({ root, type, item, mc });
  return { file, type, source, id: item.id, name: item.title };
}

module.exports = { TYPES, libraryDir, search, install, searchModrinth, searchCurseForge };
