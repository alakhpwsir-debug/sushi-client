# Run Sushi Client on your Windows PC

There are two ways. Option A needs no coding tools and gives you the `.exe`. Option B runs the launcher straight from the source folder.

## Option A: Get the .exe from GitHub (recommended)

1. Create a free GitHub account at https://github.com (no card needed).
2. Click **+ > New repository**. Name it `sushi-client`, keep it **Private** or Public, and do NOT tick "Add a README". Click **Create repository**.
3. Install Git for Windows: https://git-scm.com/download/win (default options are fine).
4. Unzip `sushi-client.zip` to a folder, e.g. `C:\Users\YOU\Desktop\sushi-client`.
5. Open that folder, click the address bar, type `powershell`, press Enter. Then run:
   ```powershell
   git init
   git add .
   git commit -m "Sushi Client 0.1.0"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/sushi-client.git
   git push -u origin main
   ```
   Replace `YOUR_USERNAME`. A browser window will ask you to sign in to GitHub once.
6. On GitHub, open the repo, go to the **Actions** tab, click **Build Windows EXE**, then **Run workflow**. Wait about 5-10 minutes for the green check.
7. Click the finished run, scroll to **Artifacts**, and download **SushiClient-windows**. Unzip it. You get `SushiClient-0.1.0.exe`.
8. Double-click the `.exe`. Windows SmartScreen may warn "Windows protected your PC" because the app is unsigned. Click **More info > Run anyway**.

## Option B: Run from source (no GitHub)

1. Install **Node.js LTS (20 or newer)** from https://nodejs.org (default options).
2. Unzip `sushi-client.zip` to e.g. `C:\Users\YOU\Desktop\sushi-client`.
3. Open PowerShell inside that folder (click the address bar, type `powershell`, Enter).
4. Run:
   ```powershell
   npm install
   npm test
   npm start
   ```
   `npm install` downloads Electron (about 100 MB). It takes a few minutes the first time.
5. The Sushi Client window opens. To make a portable `.exe` on your PC, run `npm run dist:win`. The file appears in the `dist` folder.

## First launch checklist

1. Go to **Accounts**, type a name (3-16 letters/numbers/_) and press **Add** to use an offline account. Microsoft login needs the Azure setup described in README.md.
2. Go to **Mods** and tick what you want.
3. Go to **FPS Boost** and pick a preset (Balanced is a good start).
4. Go to **Play**, pick a version (1.20.4 is a good default), keep **Use Fabric** ticked, and press **Play**.
   - The first launch downloads Minecraft (about 700 MB), libraries, assets, and Java 17 automatically. This can take several minutes.
   - Progress shows in the status bar and the console.

## Where your data is

Everything is stored in `%APPDATA%\.sushi` (Minecraft versions, mods, worlds, Java runtimes, settings). Press **Settings > Open Sushi folder** to open it.

## If something goes wrong

- **Java error on launch:** delete `%APPDATA%\.sushi\runtimes` and press Play again to reinstall Java.
- **Download stops partway:** press Play again. Finished files are not downloaded twice.
- **Build fails on GitHub:** open the failed step in the Actions log and send me the red error lines.
- **Microsoft sign-in fails:** Microsoft needs approval for new launcher apps. Use an offline account until it's approved.
