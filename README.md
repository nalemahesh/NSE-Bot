# NSE F&O Bot — Android App

## 🚀 Build APK via GitHub Actions (No Android Studio needed!)

### Step 1 — Create a GitHub repository
1. Go to [github.com](https://github.com) → Sign in or create free account
2. Click **"New repository"** (green button, top right)
3. Name it: `NSEBot`
4. Keep it **Public** (free Actions minutes)
5. Click **"Create repository"**

### Step 2 — Upload this project
On the next screen GitHub shows, click **"uploading an existing file"**:
1. Drag and drop the entire **NSEBot folder contents** into the browser
2. Make sure `.github/workflows/build.yml` is included!
3. Click **"Commit changes"**

### Step 3 — Watch it build!
1. Click the **"Actions"** tab in your repo
2. You'll see **"Build APK"** workflow running (takes ~3-5 minutes)
3. Click on it to watch live logs

### Step 4 — Download your APK
1. Once build is ✅ green, click on the workflow run
2. Scroll down to **"Artifacts"**
3. Click **"NSEBot-debug-apk"** to download
4. Install on your Android phone!

---

## Bot Commands
| Command | Example |
|---|---|
| F&O stock list | `show fno list` |
| Live price | `price of RELIANCE` |
| Quarterly results | `results of TCS` |
| Top gainers | `top gainers` |
| Top losers | `top losers` |
| Set price alert | `alert INFY above 1500` |
| Help | `help` |
