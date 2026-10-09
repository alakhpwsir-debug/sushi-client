// Builds the java command line from a resolved version + account.
const { allowed } = require('./rules');
const { dirs } = require('./paths');

// Flattens Mojang "arguments" arrays, keeping only entries whose rules pass.
function flatten(list) {
  return list
    .filter((a) => typeof a === 'string' || allowed(a.rules))
    .flatMap((a) => (typeof a === 'string' ? [a] : Array.isArray(a.value) ? a.value : [a.value]));
}

function fill(str, vars) {
  return str.replace(/\$\{([^}]+)\}/g, (match, key) => (vars[key] !== undefined ? vars[key] : match));
}

function buildCommand({
  root,
  resolved,
  versionId,
  classpath,
  nativesDir,
  assetsIndexId,
  gameDir,
  account,
  javaPath,
  memoryMB = 4096,
  extraJvm = [],
  width = 1280,
  height = 720,
}) {
  const d = dirs(root);
  const sep = process.platform === 'win32' ? ';' : ':';

  const vars = {
    auth_player_name: account.username,
    version_name: versionId,
    game_directory: gameDir,
    assets_root: d.assets,
    assets_index_name: assetsIndexId,
    auth_uuid: account.uuid,
    auth_access_token: account.accessToken || '0',
    auth_session: account.accessToken || '0',
    clientid: account.clientId || '',
    auth_xuid: account.xuid || '0',
    user_type: account.type === 'microsoft' ? 'msa' : 'legacy',
    user_properties: '{}',
    version_type: resolved.type || 'release',
    natives_directory: nativesDir,
    launcher_name: 'SushiClient',
    launcher_version: '0.1.0',
    classpath: classpath.join(sep),
    classpath_separator: sep,
    library_directory: d.libraries,
    resolution_width: String(width),
    resolution_height: String(height),
  };

  // Modern versions ship jvm/game argument arrays; 1.8.9 uses a single string.
  const jvmTpl =
    resolved.arguments && resolved.arguments.jvm
      ? flatten(resolved.arguments.jvm)
      : ['-Djava.library.path=${natives_directory}', '-cp', '${classpath}'];
  const gameTpl =
    resolved.arguments && resolved.arguments.game
      ? flatten(resolved.arguments.game)
      : (resolved.minecraftArguments || '').split(' ').filter(Boolean);

  const jvm = jvmTpl.filter((a) => !a.includes('FabricMcEmu')).map((a) => fill(a, vars));
  const game = gameTpl.map((a) => fill(a, vars));

  const args = [`-Xmx${memoryMB}M`, '-Xms512M', ...extraJvm, ...jvm, resolved.mainClass, ...game];
  return { exe: javaPath, args };
}

module.exports = { buildCommand, fill, flatten };
