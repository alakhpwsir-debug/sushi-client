const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const UA = 'SushiClient/0.1.0 (https://github.com/sushi-client)';

async function fetchJson(url, init = {}) {
  const res = await fetch(url, {
    ...init,
    headers: { 'User-Agent': UA, ...(init.headers || {}) },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status} from ${url} ${body.slice(0, 200)}`);
  }
  return res.json();
}

function sha1Buffer(buf) {
  return crypto.createHash('sha1').update(buf).digest('hex');
}

function sha1File(file) {
  return sha1Buffer(fs.readFileSync(file));
}

// Downloads url -> dest. Skips when dest already exists and matches sha1.
async function downloadFile(url, dest, sha1, opts = {}) {
  const checkExisting = opts.checkExisting !== false;
  if (fs.existsSync(dest)) {
    if (!checkExisting || !sha1 || sha1File(dest) === sha1) return dest;
  }
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error(`Download failed (${res.status}): ${url}`);
  const buf = Buffer.from(await res.arrayBuffer());
  if (sha1 && sha1Buffer(buf) !== sha1) throw new Error(`Checksum mismatch: ${url}`);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const tmp = `${dest}.part`;
  fs.writeFileSync(tmp, buf);
  fs.renameSync(tmp, dest);
  return dest;
}

// Runs worker over items with at most `limit` in flight.
async function pool(items, limit, worker) {
  let next = 0;
  const run = async () => {
    while (next < items.length) {
      const i = next++;
      await worker(items[i], i);
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, run));
}

module.exports = { UA, fetchJson, downloadFile, pool, sha1File, sha1Buffer };
