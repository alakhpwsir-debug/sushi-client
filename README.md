# Sushi Client

A Minecraft launcher and performance-focused client, inspired by the feature set of Lunar Client.
Built with Electron. Windows `.exe` first, Android later.

> Sushi Client is an original project. It does not use Lunar Client's code, assets, or trademarks.

## What works in this build (0.1.0)

| Area | Status |
|---|---|
| Launcher UI (sidebar, Home, Play, Mods, FPS Boost, Accounts, Settings) | Done |
| Download Minecraft versions from Mojang (client, libraries, natives, assets) | Done |
| Java runtime auto-install (Temurin via Adoptium) | Done |
| Fabric loader install for 1.14+ | Done |
| Built-in mod picker (Sodium, Lithium, FerriteCore, Mod Menu, ...) from Modrinth | Done |
| FPS Boost presets (Off / Balanced / Max FPS): JVM GC tuning and options.txt | Done |
| Offline accounts | Done |
| Microsoft login (device code flow) | Done, needs your Azure Client ID (see below) |
| Windows portable `.exe` build (GitHub Actions) | Done |
| Sushi Core in-game mod (CPS, keystrokes, minimap, zoom) | Planned, phase 2 |
| Cosmetics, server list, news feed | Planned |
| Android app | Planned, after Windows feedback |

## Project layout

```
main.js                  Electron main process (IPC, accounts, launching)
preload.js               Safe bridge exposed to the UI as window.sushi
src/renderer/            UI (HTML / CSS / JS), the Sushi theme lives in styles.css
src/launcher/           Launcher core in plain Node (reusable on Android later)
  versions.js            Mojang version manifest + inheritsFrom merging
  install.js             Client jar, libraries, natives, assets downloads
  java.js                Java runtime finder / installer
  launch.js              Builds the java command line
  fabric.js              Fabric profile install
  fpsboost.js            FPS presets (JVM flags + options.txt)
  mods.js                Built-in mod catalog (Modrinth)
  auth.js                Offline + Microsoft login
  index.js               launchGame(): ties it all together
test/selftest.js         Checks against the live Mojang / Fabric APIs
.github/workflows/       Builds the Windows .exe on a Windows runner
```

## Run it locally (Windows)

1. Install Node.js 20 LTS.
2. In this folder:
   ```bash
   npm install
   npm test        # launcher core self-tests
   npm start       # opens the launcher window
   ```

## Build the .exe

Either:
- **GitHub (recommended):** push to `main`, then download `SushiClient-windows` from the Actions tab. The file is `SushiClient-0.1.0.exe`, a single portable file.
- **On a Windows PC:** `npm install` then `npm run dist:win`. The output goes to `dist/`.

## Microsoft login setup (required for real accounts)

1. Go to the Azure portal, App registrations, New registration.
2. Name: `Sushi Client`. Supported account types: personal Microsoft accounts.
3. Under Authentication, enable **Allow public client flows**.
4. Copy the **Application (client) ID** into Sushi Client, Settings, Azure Client ID.

Important: Mojang/Microsoft restrict Minecraft API access for new launcher apps. You may need to apply for approval through Mojang's launcher program before real logins work for other players. Until then, use offline accounts for testing.

## Legal notes

- Minecraft is owned by Mojang/Microsoft. Players need their own legal copy of Minecraft Java Edition to play with Microsoft login.
- Do not ship Lunar Client assets, code, or the name "Lunar". Keep the "Sushi" branding original.
- Mods are downloaded from Modrinth and remain under their own licenses.

## Roadmap

1. **0.1.x:** Windows beta, feedback from testers, bug fixes, performance pass.
2. **0.2:** Sushi Core in-game Fabric mod: CPS counter, keystrokes, minimap, zoom, custom HUD.
3. **0.3:** Cosmetics system, server list, news from a JSON feed you control.
4. **0.4:** Android app (Capacitor wrapper around the same UI, with a native launcher core for mobile).
