// Evaluates Mojang-style "rules" arrays (OS / feature gates) from version JSON.

const OS = process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'osx' : 'linux';

function ruleMatches(rule) {
  if (rule.os) {
    if (rule.os.name && rule.os.name !== OS) return false;
    if (rule.os.arch === 'x86') return false; // we only ship 64-bit
  }
  // Sushi never enables optional features (demo mode, custom resolution...),
  // so feature rules only match when every feature is false.
  if (rule.features && Object.values(rule.features).some(Boolean)) return false;
  return true;
}

// No rules => allowed. Otherwise the last matching rule decides.
function allowed(rules) {
  if (!rules || rules.length === 0) return true;
  let result = false;
  for (const rule of rules) {
    if (ruleMatches(rule)) result = rule.action === 'allow';
  }
  return result;
}

module.exports = { OS, allowed };
