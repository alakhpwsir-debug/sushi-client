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

// Polls until cond() is true, or fails after timeoutMs.
async function waitFor(cond, timeoutMs, label) {
  const start = Date.now();
  while (!cond()) {
    if (Date.now() - start > timeoutMs) throw new Error(`timed out waiting for ${label}`);
    await new Promise((r) => setTimeout(r, 50));
  }
}

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

  step('mod catalog + bundled Sushi Core jar');
  assert(MOD_CATALOG.length >= 5);
  assert(MOD_CATALOG.find((m) => m.slug === 'sushi-core' && m.bundled));
  assert(fs.existsSync(path.join(__dirname, '..', 'assets', 'mods', 'sushi-core-1.21.1.jar')), 'bundled jar missing');

  step('CPU reserve flags');
  const { coreReserveArgs, startGameProcess } = require('../src/launcher/gameprocess');
  assert.deepStrictEqual(coreReserveArgs(2), []);
  assert.deepStrictEqual(coreReserveArgs(6), ['-XX:ActiveProcessorCount=5']);
  assert.deepStrictEqual(coreReserveArgs(12), ['-XX:ActiveProcessorCount=10']);

  step('game process: streams log, reports exit, below-normal priority');
  const fakeGame = path.join(root, 'fake-game.js');
  const marker = path.join(root, 'fake-game.done');
  fs.writeFileSync(
    fakeGame,
    `const fs = require('fs');
let n = 0;
const t = setInterval(() => {
  console.log('tick ' + (++n));
  if (n === 4) { clearInterval(t); fs.writeFileSync(process.argv[2], 'done'); process.exit(7); }
}, 150);`,
  );
  const events = [];
  const logFile = path.join(root, 'logs', 'game-latest.log');
  const child = startGameProcess({
    exe: process.execPath,
    args: [fakeGame, marker],
    cwd: root,
    logFile,
    emit: (ch, data) => events.push({ ch, data }),
  });
  assert(child.pid > 0);
  if (process.platform !== 'win32') {
    // Priority is set synchronously after spawn; 10 is PRIORITY_BELOW_NORMAL.
    assert.strictEqual(os.getPriority(child.pid), os.constants.priority.PRIORITY_BELOW_NORMAL);
  }
  await waitFor(() => events.some((e) => e.ch === 'game:exit'), 15000, 'game:exit');
  const exit = events.find((e) => e.ch === 'game:exit');
  assert.strictEqual(exit.data.code, 7);
  const streamed = events.filter((e) => e.ch === 'game:log').map((e) => e.data.text).join('');
  assert(streamed.includes('tick 1') && streamed.includes('tick 4'), 'log lines were not streamed');
  assert(fs.readFileSync(logFile, 'utf8').includes('tick 4'), 'log file missing output');

  step('game keeps running after the launcher process exits');
  const marker2 = path.join(root, 'fake-long.done');
  fs.writeFileSync(
    path.join(root, 'fake-long.js'),
    `const fs = require('fs');
let n = 0;
const t = setInterval(() => {
  console.log('long ' + (++n));
  if (n === 8) { clearInterval(t); fs.writeFileSync(process.argv[2], 'done'); }
}, 150);`,
  );
  const launcherSim = path.join(root, 'launcher-sim.js');
  fs.writeFileSync(
    launcherSim,
    `const { startGameProcess } = require(${JSON.stringify(path.join(__dirname, '..', 'src', 'launcher', 'gameprocess.js'))});
const c = startGameProcess({ exe: process.execPath, args: [${JSON.stringify(path.join(root, 'fake-long.js'))}, ${JSON.stringify(marker2)}],
  cwd: ${JSON.stringify(root)}, logFile: ${JSON.stringify(path.join(root, 'logs', 'long.log'))}, emit: () => {} });
console.log('pid ' + c.pid);
process.exit(0); // the launcher closes right away`,
  );
  const sim = require('child_process').spawnSync(process.execPath, [launcherSim], { encoding: 'utf8' });
  assert.strictEqual(sim.status, 0, 'launcher simulation failed: ' + sim.stderr);
  assert(!fs.existsSync(marker2), 'game finished before the launcher exited (test is not meaningful)');
  await waitFor(() => fs.existsSync(marker2), 10000, 'game to finish after launcher exit');
  assert(fs.readFileSync(path.join(root, 'logs', 'long.log'), 'utf8').includes('long 8'), 'game stopped early');

  fs.rmSync(root, { recursive: true, force: true });
  console.log('\nAll checks passed.');
})().catch((err) => {
  console.error('\nFAILED:', err.message);
  fs.rmSync(root, { recursive: true, force: true });
  process.exit(1);
});
