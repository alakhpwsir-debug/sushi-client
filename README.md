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
| Fabric loader install for 1.14+ (default 1.21.1, Java 21 auto-installed) | Done |
| Built-in mod picker (Sodium, Lithium, FerriteCore, Mod Menu, ...) from Modrinth | Done |
| FPS Boost presets (Off / Balanced / Max FPS): JVM GC tuning and options.txt | Done |
| Offline accounts | Done |
| Microsoft login (device code flow) | Done, needs your Azure Client ID (see below) |
| Windows portable `.exe` build (GitHub Actions) | Done |
| FPS mods: Sodium, Lithium, FerriteCore, Entity Culling, ImmediatelyFast, ModernFix, Dynamic FPS | Done |
| Sushi Core in-game HUD mod for 1.21.1 (FPS, CPS, keystrokes, ping, coords, Right Shift module menu) | Done, compiled against Fabric API 0.116.17 |
| Sushi Core zoom, minimap, waypoints, Sushi settings screen | Planned |
| Cosmetics, server list, news feed | Planned |
| Android app | Planned, after Windows feedback |

## Sushi Core (in-game mod)

Source lives in `mods/sushi-core`. The built jar is committed at `assets/mods/sushi-core-1.21.1.jar` and copied into each Fabric instance by the launcher.

Rebuild it (needs Java 21 and Gradle 8.10+; this repo has no Gradle wrapper yet):
```bash
cd mods/sushi-core
gradle build
cp build/libs/sushi-core-0.2.0.jar ../../assets/mods/sushi-core-1.21.1.jar
```

HUD colors are cyan and purple (`SushiTheme.java`). Each module is its own HUD element. Use **Options** on a module card for scale, background and module toggles, and **Edit HUD layout** to drag elements into place. Positions are saved in `config/sushi-core.properties`. Press **Right Shift** in game to open the module menu and switch FPS, CPS, keystrokes, ping and coordinates on or off. 
Fabric API is required by Sushi Core. The launcher always installs it (`REQUIRED_SLUGS` in `src/launcher/mods.js`).

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

## Home content (news and servers)

News and servers come from `assets/content.json`. Edit that file and rebuild, or set **Settings → Launcher → News and servers source** to an https JSON URL with the same shape:

```json
{ "news": [{ "tag": "Update", "title": "...", "text": "...", "url": "https://..." }],
  "servers": [{ "name": "...", "address": "play.example.com", "tag": "SMP", "desc": "..." }] }
```

**Join** launches the active profile and connects straight to the server (Minecraft's quick play, 1.20 and newer). **Copy IP** copies the address. The sample servers are placeholders.

## Launcher features

- **Header account switcher:** click the account chip to switch accounts.
- **Play bar profile switcher:** pick a profile without leaving the page.
- **Open mods folder:** on the Mods page or in a profile's editor. Launch a Fabric profile once first so its folder exists.
- **Settings → Appearance:** Aurora, Midnight, Nebula, or your own image (PNG or JPG, up to 2.5 MB).

## Sushi title screen

Sushi Core replaces the vanilla title screen with its own, using a small mixin on `MinecraftClient.setScreen`. It has a centered menu (Singleplayer, Multiplayer, Options, Keybinds, Quit), an account bar, shortcuts to the Sushi menu and HUD layout editor, and a tips card. Turn it off from the Sushi menu (**Title: SUSHI / VANILLA**). The change applies the next time the title screen opens.

## Title screen background and launch performance

- **Panorama:** the Sushi title screen slowly roams across an original scenic panorama (`mods/sushi-core/src/main/resources/assets/sushi-core/textures/gui/title_panorama.png`). Replace that file (keep the same size) to use your own picture.
- **Mods are cached:** a launch reuses the mod jars from the last run when the Minecraft version hasn't changed. It skips the Modrinth lookups and downloads.
- **Memory is capped to your PC:** the Java heap never exceeds 40% of your physical RAM (minimum 2 GB). A bigger heap pushes Windows into paging, which makes every app lag.
- **Frame rate follows your monitor:** the FPS presets are capped to your monitor's refresh rate. Frames above that are never shown, so they only load the GPU.
- **Background FPS limit:** when the game window isn't focused (another app is in front, or the game is minimized), Sushi Core drops the game to 30 FPS. It returns to your normal FPS as soon as you click back into the game.
- **Other apps stay responsive:** Minecraft runs at Below Normal priority, so your browser and other apps get the CPU first when the PC is busy. Two CPU cores (one on 4-core PCs) are left free for them. This is automatic.
- **Closing the launcher doesn't close the game:** Minecraft runs as its own process. Its output goes to `%APPDATA%\.sushi\logs\game-latest.log`. While the launcher is open, the Logs page keeps showing the output.

## Browse: mods, resource packs and shader packs

The **Browse** page searches **Modrinth** (no key needed) and **CurseForge** (free API key) for mods, resource packs and shader packs, and installs them into a shared library (`%APPDATA%\.sushi\library`). Each profile then picks what it uses:

- **Mods** need a Fabric profile. Added mods are copied into the game on launch. A mod that's already on the Mods page can't be added twice.
- **Resource packs** are copied into the game and listed in the profile's packs. Vanilla stays first. The launcher sets the pack list on every launch.
- **Shader packs** need Iris. Sushi adds Iris to the profile when a shader is chosen and writes `config/iris.properties` for you. Only one shader is active at a time.

**CurseForge:** add the key in Settings → Launcher. Some CurseForge authors block third-party downloads. For those, the launcher shows a message: download the file from the CurseForge website and copy it into the library folder.
