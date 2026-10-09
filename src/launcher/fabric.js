// Fabric loader support (Fabric runs on 1.14+; 1.8.9 stays vanilla-only for now).
const fs = require('fs');
const path = require('path');
const { fetchJson } = require('./http');
const { dirs } = require('./paths');

async function installFabric(root, mc, log = () => {}) {
  const loaders = await fetchJson(`https://meta.fabricmc.net/v2/versions/loader/${mc}`);
  const pick = loaders.find((l) => l.loader.stable) || loaders[0];
  if (!pick) throw new Error(`Fabric does not support Minecraft ${mc}`);

  log(`Installing Fabric loader ${pick.loader.version} for ${mc}...`);
  const profile = await fetchJson(
    `https://meta.fabricmc.net/v2/versions/loader/${mc}/${pick.loader.version}/profile/json`,
  );

  const dir = path.join(dirs(root).versions, profile.id);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${profile.id}.json`), JSON.stringify(profile, null, 2));
  return profile.id;
}

module.exports = { installFabric };
