# Changelog

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
