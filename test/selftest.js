// Offline-safe checks for the launcher core. Uses the real Mojang / Fabric APIs
// (needs internet) but does NOT download the game. Run: npm test
const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');

const root = fs.mkdtempSync(path.join(os.tmpdir(), 'sushi-test-'));
process.env.SUSHI_HOME = root;

const { allowed } = require('../src/launcher/rules');
const { offlineAccount } = require('../src/launcher/auth');
const { resolveVersion, listVersions } = require('../src/launcher/versions');
const { installFabric } = require('../src/launcher/fabric');
const { buildCommand, flatten } = require('../src/launcher/launch');
const { mavenPath } = require('../src/launcher/install');
const { presetJvmArgs, applyOptionsTxt } = require('../src/launcher/fpsboost');
const { MOD_CATALOG } = require('../src/launcher/mods');

const step = (name) => console.log(`- ${name}`);

(async () => {
  step('rules');
  assert.strictEqual(allowed(undefined), true);
  assert.strictEqual(allowed([{ action: 'allow', features: { is_demo_user: true } }]), false);
  assert.strictEqual(allowed([{ action: 'allow', features: { has_custom_resolution: false } }]), true);

  step('offline UUID matches vanilla');
  const notch = offlineAccount('Notch');
  assert.strictEqual(notch.uuid, 'b50ad385-829d-3141-a216-7e7d7539ba7f');

  step('maven path');
  assert.strictEqual(mavenPath('org.ow2.asm:asm:9.6'), 'org/ow2/asm/asm/9.6/asm-9.6.jar');

  step('mojang manifest');
  const versions = await listVersions();
  assert(versions.find((v) => v.id === '1.20.4'), '1.20.4 missing from manifest');
  assert(versions.find((v) => v.id === '1.8.9'), '1.8.9 missing from manifest');

  step('resolve 1.20.4 (vanilla, modern arguments)');
  const v1204 = await resolveVersion(root, '1.20.4');
  assert.strictEqual(v1204.mainClass, 'net.minecraft.client.main.Main');
  assert.strictEqual(v1204.javaVersion.majorVersion, 17);
  assert(v1204.libraries.length > 10);

  step('install Fabric for 1.20.4 and resolve it');
  const fabricId = await installFabric(root, '1.20.4');
  const fab = await resolveVersion(root, fabricId);
  assert(/knot/i.test(fab.mainClass), `unexpected main class ${fab.mainClass}`);
  assert.strictEqual(fab.jarId, '1.20.4');
  assert(fab.libraries.length > v1204.libraries.length - 5, 'fabric libraries not merged');

  step('build launch command (Fabric 1.20.4)');
  const account = offlineAccount('SushiTester');
  const cmd = buildCommand({
    root,
    resolved: fab,
    versionId: fabricId,
    classpath: ['C:/x/a.jar'],
    nativesDir: '/tmp/natives',
    assetsIndexId: fab.assetIndex.id,
    gameDir: '/tmp/game',
    account,
    javaPath: 'java',
    memoryMB: 6144,
    extraJvm: presetJvmArgs('max', 21),
  });
  const line = cmd.args.join(' ');
  assert(cmd.args.includes('-Xmx6144M'));
  assert(cmd.args.includes('-XX:+UseZGC'));
  assert(line.includes('--username SushiTester'));
  assert(!line.includes('FabricMcEmu'));
  assert(!line.includes('${'), 'unfilled placeholder left in command');

  step('1.8.9 uses legacy minecraftArguments');
  const v189 = await resolveVersion(root, '1.8.9');
  const cmd189 = buildCommand({
    root,
    resolved: v189,
    versionId: '1.8.9',
    classpath: ['C:/x/a.jar'],
    nativesDir: '/tmp/natives',
    assetsIndexId: v189.assetIndex.id,
    gameDir: '/tmp/game',
    account,
    javaPath: 'java',
  });
  const line189 = cmd189.args.join(' ');
  assert(cmd189.args.includes('-cp'));
  assert(line189.includes('--username SushiTester'));
  assert(!line189.includes('${'), 'unfilled placeholder left in 1.8.9 command');
  assert.strictEqual(v189.javaVersion.majorVersion, 8);

  step('flatten keeps rule-allowed strings only');
  assert.deepStrictEqual(flatten(['a', { rules: [{ action: 'allow', features: { is_demo_user: true } }], value: '--demo' }]), ['a']);

  step('FPS options.txt merge');
  const gameDir = path.join(root, 'g');
  fs.mkdirSync(gameDir, { recursive: true });
  fs.writeFileSync(path.join(gameDir, 'options.txt'), 'fov:0.5\nmaxFps:60\n');
  applyOptionsTxt(gameDir, { maxFps: '144', particles: '1' });
  const opt = fs.readFileSync(path.join(gameDir, 'options.txt'), 'utf8');
  assert(opt.includes('fov:0.5') && opt.includes('maxFps:144') && opt.includes('particles:1'));

  step('mod catalog');
  assert(MOD_CATALOG.length >= 5);

  fs.rmSync(root, { recursive: true, force: true });
  console.log('\nAll checks passed.');
})().catch((err) => {
  console.error('\nFAILED:', err.message);
  fs.rmSync(root, { recursive: true, force: true });
  process.exit(1);
});
