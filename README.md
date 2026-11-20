# DHYRA BROWSER

Premium Acrylic Android browser starter project built with Kotlin and Android WebView.

## Build APK automatically with GitHub
1. Create a new empty GitHub repository.
2. Upload the contents of this folder to the repository root.
3. Open **Actions** and enable workflows if GitHub asks.
4. Run **Build DHYRA Browser APK** (or push to `main` / `master`).
5. Download `dhyra-browser-debug-apk` from the completed workflow run's Artifacts section.

## Build locally
Requires Android SDK Platform 35, JDK 17 and Gradle 8.9:
```bash
gradle assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Current implementation
- Android WebView browsing, search/address bar, back/forward/reload
- Basic tab switching and new/close current tab
- Bookmarks and browsing history stored locally
- Android DownloadManager integration
- Find in page, share page, desktop user-agent toggle
- Session private-mode toggle, clear history/cache
- Bitcoin donation page with exact wallet address, copy and `bitcoin:` URI
- GitHub Actions debug APK workflow

## Important limitations / setup still required
- This is a functional first release foundation, not a claim that every item in the original specification is production-complete.
- Private mode is session-level and needs deeper isolation before marketing it as hardened incognito. Android WebView does not provide full browser-grade tracking protection by itself.
- Download pause/resume, QR generation, advanced tab previews, robust permission controls, cookie controls, Safe Browsing configuration, and full settings center are not implemented yet.
- AdMob is intentionally not activated: add your own AdMob app/ad-unit IDs, configure consent (UMP), publish required privacy disclosures, and test policy compliance before enabling production ads. No fake ads or revenue figures are included.
- Release APK signing requires your own private keystore. Never commit signing keys or passwords to GitHub.
- Test on real Android devices before public release.

## Security
HTTPS certificate errors are cancelled, not bypassed. JavaScript is enabled for modern sites; use only trusted browsing content and review WebView hardening before production release.
