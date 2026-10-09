// Accounts: offline (any name) and Microsoft (official Xbox / Minecraft login).
const crypto = require('crypto');
const { fetchJson } = require('./http');

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const MS_BASE = 'https://login.microsoftonline.com/consumers/oauth2/v2.0';
const FORM = { 'Content-Type': 'application/x-www-form-urlencoded' };
const JSON_HDR = { 'Content-Type': 'application/json', Accept: 'application/json' };

function formatUuid(hex) {
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

// Same UUID the vanilla game makes for offline players.
function offlineAccount(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`).digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  return { type: 'offline', username: name, uuid: formatUuid(h.toString('hex')), accessToken: '0' };
}

// Step 1 of Microsoft sign-in: device code (the user types a code on microsoft.com/link).
async function msDeviceStart(clientId) {
  const body = new URLSearchParams({ client_id: clientId, scope: 'XboxLive.signin offline_access' });
  return fetchJson(`${MS_BASE}/devicecode`, { method: 'POST', headers: FORM, body });
}

// Step 2: poll until the user has approved the code.
async function msDevicePoll(clientId, dc) {
  const deadline = Date.now() + dc.expires_in * 1000;
  let interval = dc.interval || 5;
  while (Date.now() < deadline) {
    await sleep(interval * 1000);
    const res = await fetch(`${MS_BASE}/token`, {
      method: 'POST',
      headers: FORM,
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
        client_id: clientId,
        device_code: dc.device_code,
      }),
    });
    const json = await res.json();
    if (json.access_token) return json;
    if (json.error === 'authorization_pending') continue;
    if (json.error === 'slow_down') {
      interval += 5;
      continue;
    }
    throw new Error(json.error_description || json.error || 'Microsoft sign-in failed');
  }
  throw new Error('Microsoft sign-in timed out, please try again');
}

async function msRefresh(clientId, refreshToken) {
  const body = new URLSearchParams({
    client_id: clientId,
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    scope: 'XboxLive.signin offline_access',
  });
  return fetchJson(`${MS_BASE}/token`, { method: 'POST', headers: FORM, body });
}

// Step 3: Microsoft token -> Xbox Live -> XSTS -> Minecraft token -> profile.
async function minecraftLogin(msAccessToken, msRefreshToken) {
  const xbl = await fetchJson('https://user.auth.xboxlive.com/user/authenticate', {
    method: 'POST',
    headers: JSON_HDR,
    body: JSON.stringify({
      Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccessToken}` },
      RelyingParty: 'http://auth.xboxlive.com',
      TokenType: 'JWT',
    }),
  });

  const xsts = await fetchJson('https://xsts.auth.xboxlive.com/xsts/authorize', {
    method: 'POST',
    headers: JSON_HDR,
    body: JSON.stringify({
      Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.Token] },
      RelyingParty: 'rp://api.minecraftservices.com/',
      TokenType: 'JWT',
    }),
  });
  const uhs = xsts.DisplayClaims.xui[0].uhs;

  const mc = await fetchJson('https://api.minecraftservices.com/authentication/login_with_xbox', {
    method: 'POST',
    headers: JSON_HDR,
    body: JSON.stringify({ identityToken: `XBL3.0 x=${uhs};${xsts.Token}` }),
  });

  const profile = await fetchJson('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${mc.access_token}` },
  }).catch(() => {
    throw new Error('This Microsoft account does not own Minecraft Java Edition.');
  });

  return {
    type: 'microsoft',
    username: profile.name,
    uuid: formatUuid(profile.id),
    accessToken: mc.access_token,
    refreshToken: msRefreshToken,
    expiresAt: Date.now() + mc.expires_in * 1000,
  };
}

module.exports = { offlineAccount, msDeviceStart, msDevicePoll, msRefresh, minecraftLogin };
