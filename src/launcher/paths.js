const path = require('path');
const os = require('os');

// Where Sushi keeps everything (like .minecraft for Lunar / .sushi here).
// Override with the SUSHI_HOME env var (used by the tests).
function defaultRoot() {
  if (process.env.SUSHI_HOME) return process.env.SUSHI_HOME;
  const base = process.platform === 'win32' ? (process.env.APPDATA || os.homedir()) : os.homedir();
  return path.join(base, '.sushi');
}

const ROOT = defaultRoot();

function dirs(root = ROOT) {
  return {
    root,
    versions: path.join(root, 'versions'),
    libraries: path.join(root, 'libraries'),
    assets: path.join(root, 'assets'),
    runtimes: path.join(root, 'runtimes'),
    instances: path.join(root, 'instances'),
    natives: path.join(root, 'natives'),
    settings: path.join(root, 'settings.json'),
    accounts: path.join(root, 'accounts.json'),
  };
}

module.exports = { ROOT, dirs };
