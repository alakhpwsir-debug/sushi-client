// RAM given to Minecraft (the JVM -Xmx value). The user picks the amount. These rules only keep it
// inside what this PC can actually give: at least 1 GB, and at least 1 GB left for Windows and
// other apps. The recommended amount is 40% of the PC's RAM (rounded to 512 MB, 2 GB to 8 GB).
const MB = 1024 * 1024;
const MIN_MB = 1024;
const OS_RESERVE_MB = 1024;
const STEP_MB = 256;

function memoryLimits(totalBytes) {
  const totalMB = Math.max(0, Math.floor(Number(totalBytes) / MB) || 0);
  const maxMB = Math.max(MIN_MB, Math.floor((totalMB - OS_RESERVE_MB) / STEP_MB) * STEP_MB);
  let recommendedMB = Math.floor((totalMB * 0.4) / 512) * 512;
  recommendedMB = Math.min(8192, Math.max(2048, recommendedMB));
  recommendedMB = Math.min(maxMB, recommendedMB);
  return { totalMB, minMB: MIN_MB, maxMB, stepMB: STEP_MB, recommendedMB };
}

// Returns the amount to launch with. 0, empty or invalid means "use the recommended amount".
function resolveMemory(requestedMB, totalBytes) {
  const lim = memoryLimits(totalBytes);
  const n = Math.round(Number(requestedMB));
  if (!Number.isFinite(n) || n <= 0) return lim.recommendedMB;
  return Math.min(lim.maxMB, Math.max(lim.minMB, n));
}

module.exports = { memoryLimits, resolveMemory };
