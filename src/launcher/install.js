// Downloads everything a version needs: client jar, libraries, natives, assets.
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const { downloadFile, pool } = require('./http');
const { dirs } = require('./paths');
const { allowed, OS } = require('./rules');
const { resolveVersion, getRawVersion } = require('./versions');

// "org.ow2.asm:asm:9.6" -> "org/ow2/asm/asm/9.6/asm-9.6.jar" (used by Fabric libs)
function mavenPath(name) {
  const [group, artifact, version, classifier] = name.split(':');
  const file = `${artifact}-${version}${classifier ? `-${classifier}` : ''}.jar`;
  return `${group.replace(/\./g, '/')}/${artifact}/${version}/${file}`;
}

function extractNatives(jarFile, dir) {
  fs.mkdirSync(dir, { recursive: true });
  const zip = new AdmZip(jarFile);
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory || entry.entryName.startsWith('META-INF')) continue;
    zip.extractEntryTo(entry, dir, false, true);
  }
}

async function installVersion(root, versionId, log = () => {}) {
  const d = dirs(root);
  const v = await resolveVersion(root, versionId);
  const vanilla = await getRawVersion(root, v.jarId);

  // 1. Client jar
  const jarPath = path.join(d.versions, v.jarId, `${v.jarId}.jar`);
  log(`Checking Minecraft ${v.jarId} client...`);
  await downloadFile(vanilla.downloads.client.url, jarPath, vanilla.downloads.client.sha1);

  // 2. Libraries + natives
  const classpath = [jarPath];
  const libJobs = [];
  const nativeJobs = [];

  for (const lib of v.libraries) {
    if (!allowed(lib.rules)) continue;

    let artifact = lib.downloads ? lib.downloads.artifact : null;
    if (!lib.downloads && lib.name) {
      // Maven-style entry (Fabric etc.): build the URL ourselves
      const p = mavenPath(lib.name);
      const base = (lib.url || 'https://libraries.minecraft.net/').replace(/\/?$/, '/');
      artifact = { path: p, url: `${base}${p}`, sha1: lib.sha1 || null };
    }
    if (artifact) {
      libJobs.push(artifact);
      classpath.push(path.join(d.libraries, artifact.path));
    }

    const cls = lib.natives && lib.natives[OS];
    if (cls) {
      const key = cls.replace('${arch}', '64');
      const nat = lib.downloads && lib.downloads.classifiers && lib.downloads.classifiers[key];
      if (nat) nativeJobs.push(nat);
    }
  }

  log(`Libraries: ${libJobs.length}`);
  await pool(libJobs, 8, (a) => downloadFile(a.url, path.join(d.libraries, a.path), a.sha1));

  const nativesDir = path.join(d.natives, versionId);
  fs.rmSync(nativesDir, { recursive: true, force: true });
  for (const nat of nativeJobs) {
    const file = path.join(d.libraries, nat.path);
    await downloadFile(nat.url, file, nat.sha1);
    extractNatives(file, nativesDir);
  }

  // 3. Assets (sounds, language files, etc.)
  const ai = v.assetIndex;
  const indexFile = path.join(d.assets, 'indexes', `${ai.id}.json`);
  await downloadFile(ai.url, indexFile, ai.sha1);
  const index = JSON.parse(fs.readFileSync(indexFile, 'utf8'));
  const entries = Object.entries(index.objects);
  const isVirtual = Boolean(index.virtual || index.map_to_resources);

  log(`Assets: ${entries.length} files`);
  await pool(entries, 16, async ([name, obj]) => {
    const sub = obj.hash.slice(0, 2);
    const file = path.join(d.assets, 'objects', sub, obj.hash);
    await downloadFile(`https://resources.download.minecraft.net/${sub}/${obj.hash}`, file, obj.hash, {
      checkExisting: false, // content-addressed: existence is enough
    });
    if (isVirtual) {
      const vf = path.join(d.assets, 'virtual', ai.id, name);
      if (!fs.existsSync(vf)) {
        fs.mkdirSync(path.dirname(vf), { recursive: true });
        fs.copyFileSync(file, vf);
      }
    }
  });

  return {
    versionId,
    jarId: v.jarId,
    resolved: v,
    classpath: [...new Set(classpath)],
    nativesDir,
    assetsIndexId: ai.id,
    javaMajor: v.javaVersion ? v.javaVersion.majorVersion : 8,
  };
}

module.exports = { installVersion, mavenPath };
