// Starts Minecraft as an independent process.
//
// - The game is detached and unref'd, so it keeps running after the launcher window closes.
// - Its output goes to a log file (not a pipe), so it never depends on the launcher staying open.
//   While the launcher is open, new lines are streamed into the Logs view.
// - The game runs at Below Normal priority, so other apps get the CPU first when the PC is busy.
// - A couple of CPU cores are left free: the JVM sizes its GC and JIT threads from this count.
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');
const { StringDecoder } = require('string_decoder');

const TAIL_MS = 500;

/** JVM flags that stop the game from using every core. */
function coreReserveArgs(cores = os.cpus().length) {
  if (cores < 4) return [];
  const reserve = cores >= 8 ? 2 : 1;
  return [`-XX:ActiveProcessorCount=${cores - reserve}`];
}

/**
 * exe, args, cwd: the java command (from buildCommand).
 * logFile: where the game's stdout and stderr are written (replaced on each launch).
 * emit(channel, payload): sends game:log, game:status and game:exit to the UI while it is open.
 * Returns the child process (its pid is the game's pid).
 */
function startGameProcess({ exe, args, cwd, logFile, emit }) {
  fs.mkdirSync(path.dirname(logFile), { recursive: true });
  const out = fs.openSync(logFile, 'w');
  let child;
  try {
    child = spawn(exe, args, {
      cwd,
      windowsHide: true,
      detached: true, // own process group (and no console on Windows), so it outlives the launcher
      stdio: ['ignore', out, out],
    });
  } finally {
    fs.closeSync(out); // the child keeps its own copy of the handle
  }

  if (child.pid) {
    try {
      os.setPriority(child.pid, os.constants.priority.PRIORITY_BELOW_NORMAL);
    } catch {
      // Not fatal: the game still runs, just at normal priority.
    }
  }

  // Streams new log text to the UI while the launcher is open.
  let offset = 0;
  const decoder = new StringDecoder('utf8');
  const readNew = () => {
    let fd;
    try {
      fd = fs.openSync(logFile, 'r');
    } catch {
      return;
    }
    try {
      const size = fs.fstatSync(fd).size;
      if (size > offset) {
        const buf = Buffer.alloc(size - offset);
        fs.readSync(fd, buf, 0, buf.length, offset);
        offset = size;
        emit('game:log', { text: decoder.write(buf) });
      }
    } finally {
      fs.closeSync(fd);
    }
  };

  const timer = setInterval(readNew, TAIL_MS);
  let finished = false;
  const finish = (code) => {
    if (finished) return;
    finished = true;
    clearInterval(timer);
    readNew();
    emit('game:exit', { code });
  };

  child.on('error', (err) => {
    emit('game:status', { text: `Could not start Java: ${err.message}` });
    finish(null);
  });
  child.on('exit', (code) => finish(code));
  child.unref();
  return child;
}

module.exports = { startGameProcess, coreReserveArgs };
