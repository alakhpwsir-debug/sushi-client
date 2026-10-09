// Finds or downloads a Java runtime (Eclipse Temurin via the Adoptium API).
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const AdmZip = require('adm-zip');
const { downloadFile } = require('./http');
const { dirs } = require('./paths');

function findJava(dir, depth = 0) {
  if (!fs.existsSync(dir) || depth > 5) return null;
  const bin = process.platform === 'win32' ? 'java.exe' : 'java';
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const e of entries) {
    if (e.isFile() && e.name === bin && path.basename(dir) === 'bin') return path.join(dir, e.name);
  }
  for (const e of entries) {
    if (e.isDirectory()) {
      const found = findJava(path.join(dir, e.name), depth + 1);
      if (found) return found;
    }
  }
  return null;
}

async function ensureJava(root, major, log = () => {}) {
  const base = path.join(dirs(root).runtimes, `java-${major}`);
  const existing = findJava(base);
  if (existing) return existing;

  const osName = process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'mac' : 'linux';
  const arch = process.arch === 'arm64' ? 'aarch64' : 'x64';
  const url = `https://api.adoptium.net/v3/binary/latest/${major}/ga/${osName}/${arch}/jre/hotspot/normal/eclipse`;
  const ext = osName === 'windows' ? 'zip' : 'tar.gz';

  log(`Downloading Java ${major} runtime (one time)...`);
  fs.mkdirSync(base, { recursive: true });
  const archive = path.join(base, `runtime.${ext}`);
  await downloadFile(url, archive, null);

  if (ext === 'zip') new AdmZip(archive).extractAllTo(base, true);
  else execFileSync('tar', ['-xzf', archive, '-C', base]);
  fs.rmSync(archive, { force: true });

  const bin = findJava(base);
  if (!bin) throw new Error(`Java ${major} download finished but no java binary was found`);
  if (process.platform !== 'win32') fs.chmodSync(bin, 0o755);
  return bin;
}

module.exports = { ensureJava, findJava };
