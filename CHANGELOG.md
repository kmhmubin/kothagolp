# Changelog

## [3.12.1] — 2026-07-11

### Added

- **Komikku-style book cards** — Flat covers with squircle-free tight grid, joined badge groups, and three distinct card styles per density: Cover only / Compact (title overlay) / Comfortable (title below)
- **Shelf icon badges** — Library shelf badges on cards now show each shelf's dedicated icon on its status color instead of a text pill
- **Library options sheet** — Double-tap the Library nav button to open a Komikku-style tabbed sheet with Sort (all six orders with direction arrows) and Display (mode, grid size, card style) options
- **Downloaded shelf color** — Distinct cyan for the Downloaded shelf (was identical to Reading's blue)

### Fixed

- **FreeWebNovel chapter list** — Site added chapter pagination which limited books to 40 chapters; the full chapter list is fetched again, with a pagination-walking fallback
- **App icon cropped/zoomed on other devices** — Launcher, themed (Android 13+), and splash icons now fit their official safe zones, so circle/squircle/teardrop launcher masks no longer crop the logo
- **Library options sheet replay** — Sheet no longer opens by itself when returning to the Library screen; the gesture is a true double-tap and resets after each open

### Performance

- **Fenrir Realm details load** — Metadata and chapter list fetch in parallel (remaining latency is the source's own server)
- **Library grid** — Removed per-frame settings writes from the grid size slider and per-press recomposition from card press feedback

## [3.11.0] — 2026-07-06

### Added

- **Custom shelf ordering** — Move shelves (Reading, Downloaded, etc.) in any order via up/down arrows in Library settings
- **Unified Scheduled Tasks screen** — Centralized management of all time-based automation: chapter update intervals, local backup schedules, and cloud sync frequency. Extended intervals: Daily, Every 2 days, Weekly, Every 2 weeks, Monthly
- **Metadata backfill for dark source recovery** — "Refresh All Metadata" action caches offline metadata for all library books, preserving reading data if a source goes offline. Gracefully handles unavailable sources and supports cancellation
- **Settings information architecture reorganization** — Restructured main settings hub (GENERAL → CONTENT & DISCOVERY → DATA & SYSTEM) and Library sub-screen (Shelves first, then Behavior) for improved user flow

### Fixed

- **Google Drive sync cross-source book migration** — Detects and deduplicates books migrated from one source to another during sync. Prevents duplicate entries on other devices after source migration

## [3.10.1] — 2026-06-26

### Fixed

- Reader position no longer shifts when chrome (top bar / bottom bar) is shown or hidden.
- Library list card covers now display in correct portrait aspect ratio (matching history page).
- Source list items now show real provider icons with gradient card style.
- Source cards properly adapt layout at 3–5 column grids without content overlap.
- Chapter update notification now shows app icon instead of download icon.
- Cancel button on chapter update notification now correctly stops the background job.

## [3.6.2] — 2026-06-22

### Changed

- Added per-book related novel suggestions using tag-based matching.
- Improved list scrolling smoothness and reduced unnecessary recomposition work.
- Fixed a crash caused by the Coil disk cache directory override on new books.

## [1.0.0] — 2025-02-14

### Initial Release

- Browse and search novels from 6 sources
- Customizable reader with continuous scroll and paged modes
- Text-to-Speech with background playback and media controls
- Download chapters for offline reading
- Library management with reading status tracking
- Personalized recommendations
- Material You theming with custom color support
- Reading statistics and history tracking
- Backup and restore (including QuickNovel import)
