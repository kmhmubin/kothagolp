<img width="300" height="300" alt="Kothagolp" src="kothagolp.png" />

# Kothagolp

<div align="center">

**A modern Android novel reader with multi-source support, TTS, and offline reading**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-3.9.0-brightgreen.svg)](../../releases/latest)

[Download](#installation) · [Features](#features) · [Screenshots](#screenshots)

</div>

---

## 📖 About

**Kothagolp** is a free, open-source Android app for reading web novels from multiple sources. It is a heavily extended fork of [Novery](https://github.com/1Finn2me/Novery), which provided the UI foundation and core architecture. On top of that base, Kothagolp expands the source library to **19 built-in providers**, adds a local periodic backup system, a global theme-aware UI across the reader and TTS player, an improved onboarding flow, and a host of bug fixes and quality-of-life improvements.

Built with **Jetpack Compose**, **Material 3**, and a clean multi-module architecture (`core:domain`, `core:data`, `core:ui`, `core:common`, `source-api`).

---

<div id="features"></div>

## ✨ Features

### 📚 Multi-Source Browse & Search *(expanded from Novery)*
- **19 built-in sources**: AllNovel, Cyrisia, FenrirRealm, FreeWebNovel, LibRead, LightNovelTranslations, LightNovelWorld, Lnori, NovelArchive, NovelBin, NovelBuddy, NovelDex, NovelFire, NovelsOnline, PawRead, Ranobes, RoyalRoad, Webnovel, and more
- All sources are built-in — no plugins or external loading required
- Progressive streaming search with fuzzy matching and history
- Enable/disable individual sources per-device

### 📖 Reader *(from Novery, extended)*
- Continuous scroll or chaptered view
- Customizable fonts, sizing, spacing, alignment, and colors
- Independent reader color scheme — 20+ presets including AMOLED, Sepia, Nord, and more
- Reader chrome (chapter title, nav buttons, dividers, scene breaks) follows the global app accent color
- Fullscreen mode with auto-hiding controls and volume key navigation
- Bookmarks and text highlighting with notes
- Stable position tracking across sessions

### 🎧 Text-to-Speech *(from Novery, extended)*
- Background playback with screen off
- Real-time sentence highlighting — highlight color follows the global app theme
- Auto-scroll to the active sentence
- Lock screen and Bluetooth media controls
- Automatic chapter advancement
- Full voice selection from device TTS engines
- TTS player UI follows the global app accent color

### 💾 Offline Reading *(from Novery)*
- Background download queue with priority levels
- Auto-download new chapters from library novels
- Per-novel storage management

### 📚 Library *(from Novery, extended)*
- Track status: Reading, Completed, On Hold, Plan to Read, Dropped, Spicy
- New chapter detection with badge indicators
- Filter chips always navigate to the correct page (fixed a blank-screen bug)
- Sorting, searching, and batch operations (change category, mark all read, delete)

### 🤖 Recommendations *(from Novery)*
- Tag-based matching engine that learns from reading patterns
- Categories: *For You*, *Because You Read X*, *From Authors You Like*
- Configurable filters to block, reduce, or boost tags, authors, or sources

### 🎨 Theming *(from Novery, extended)*
- Material You dynamic colors (Android 12+)
- Light, Dark, and AMOLED black modes
- 7 preset color themes + full custom color picker
- Global accent color propagates to the reader chrome, TTS player, and bottom bar

### 💾 Backup & Sync *(added in Kothagolp)*
- **Local periodic backup** via WorkManager — configurable interval: Off / 6h / 12h / 24h / 48h / Weekly
- Backup saved to user-chosen storage folder or internal storage; keeps last 5 auto-backups
- Google Drive cloud backup and sync
- Storage folder auto-creates `downloads/`, `autobackup/`, `logs/`, `notes/` sub-folders on first pick

### 🚀 Onboarding *(improved in Kothagolp)*
- Quick-seed discovery across all 19 sources on first launch
- Sensible content defaults (mature content on, BL/GL off)
- Genre preference selection (liked / neutral / disliked) seeds the recommendation engine
- Toast notifications if any source fails during initial discovery

### 📊 Statistics & History *(from Novery)*
- Automatic reading time tracking
- Daily reading streaks
- History timeline grouped by date
- Chapter completion progress

### 📤 EPUB Export *(from Novery)*
- Export any novel or selection of chapters as a standard EPUB file

---

<div id="screenshots"></div>

## 📸 Screenshots

<div align="center" style="display: flex; flex-wrap: wrap; justify-content: center; gap: 16px;">

  <img src="https://github.com/user-attachments/assets/c0679bb9-4204-42e2-a71a-3faf9e3e4c87" alt="Onboard" width="250" />
  <img src="https://github.com/user-attachments/assets/281da9b1-4d71-41d1-89b8-264d085eebb7" alt="Library" width="250" />
  <img src="https://github.com/user-attachments/assets/b715a6ec-2a40-4aa8-bb92-609b34d7c0d4" alt="TTS" width="250" />
  <img src="https://github.com/user-attachments/assets/084957c8-2a01-4535-8318-9dfb4708ba9f" alt="Settings" width="250" />
  <img src="https://github.com/user-attachments/assets/bea55bae-24fa-4694-8dc9-9e631c1772f5" alt="Themes" width="250" />
  <img src="https://github.com/user-attachments/assets/2171786b-337c-4e26-a505-cbc15092e4b8" alt="Stats" width="250" />
  <img src="https://github.com/user-attachments/assets/349b1de7-ffb8-47b9-8d37-307392715e2c" alt="History" width="250" />
  <img src="https://github.com/user-attachments/assets/75247556-b5aa-4756-87c5-0f01e3a81803" alt="Download" width="250" />
  <img src="https://github.com/user-attachments/assets/5953d4c2-2e31-4a8b-b2f7-981628ce22ac" alt="Reader 2" width="250" />
  <img src="https://github.com/user-attachments/assets/2473aaa3-9c43-4a67-b089-6fa7a71be0c6" alt="Reader" width="250" />
  <img src="https://github.com/user-attachments/assets/1b517ca6-3e54-45e3-923a-61b5f8c0759a" alt="Reader" width="250" />

</div>

---

<div id="installation"></div>

## 📥 Installation

### Download
1. Grab the latest APK from [Releases](../../releases)  
2. Install and grant notification permissions when prompted  

> Requires **Android 8.0+ (API 26)**

### Build from Source
```bash
git clone https://github.com/kmhmubin/kothagolp.git
cd kothagolp
./gradlew installDebug
```
Requires **Android Studio Koala+**, **JDK 17+**, **Kotlin 2.2.10**.

---

## 🙏 Acknowledgments
- [Novery](https://github.com/1Finn2me/Novery) — the UI design and base application that Kothagolp is forked from; the reader, TTS, recommendation engine, theming system, and overall architecture originate here
- [Tachiyomi](https://github.com/tachiyomiorg/tachiyomi) — pioneered the multi-source reader concept
- [QuickNovel](https://github.com/LagradOst/QuickNovel) — inspiration for novel-specific reader design
- [LNReader](https://github.com/LNReader/lnreader-sources) — extensible source architecture
- [Komikku](https://github.com/komikku-app/komikku) — inspiration for storage folder structure and periodic backup system

---

## ⚠️ Disclaimer
Kothagolp does not host, store, or distribute any content. The app functions as a search
engine and aggregator — it crawls and displays content from third-party websites that
are publicly accessible through any standard web browser. Kothagolp has no affiliation
with, and no control over, the content provided by these sources.

Any legal concerns regarding content should be directed to the respective website
operators and content hosts. In cases of copyright infringement, please contact the
responsible parties or file hosts directly.

This application is intended for personal and educational use only. Users are solely
responsible for ensuring their use of the app complies with all applicable local,
national, and international laws. Use Kothagolp at your own risk.

By using this application, you acknowledge that the developers of Kothagolp bear no
responsibility for any content accessed through third-party sources, nor for any
consequences arising from the use of this app.

---

<div align="center">

⭐ **Star the repo if you find Kothagolp useful!**  

[Report Bug](../../issues) · [Request Feature](../../issues)

</div>
