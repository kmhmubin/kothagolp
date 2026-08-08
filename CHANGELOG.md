# Changelog

## [3.12.10] — 2026-08-08

### Fixed

- **Infinite scroll jumping to a random chapter** — With Infinite Scroll on, reading through chapters could occasionally snap forward or backward to an unrelated chapter mid-scroll, then overshoot again trying to scroll back to where you were. Caused by background chapter preloading/unloading updating the page while a scroll was still in motion; it now waits for scrolling to settle first

## [3.12.9] — 2026-08-06

### Added

- **Copy title button** — The library's quick-action sheet (long-press a book) now has a tap-to-copy icon next to the title. Handy for pasting the exact title into another source's search when one goes dead

### Fixed

- **NovelsOnline source removed** — this source has been fully down and no longer works
- **FreeWebNovel cover images** — covers stopped loading after the site changed its markup; fixed
- **Cloud Sync "Every 2 weeks" / "Monthly" intervals** — picking either of these silently disabled auto-sync entirely instead of scheduling it; fixed
- **Downloads shelf not updating live** — a title downloading or downloaded didn't appear on the Downloaded shelf until the app was restarted; now updates immediately

## [3.12.8] — 2026-08-04

### Added

- **Reading Stats redesign** — Streak & Activity is now a single card with a dual-ring gauge (streak progress + today's goal), a weekly checkmark row, and a personal-best bar. Added a Week/Month "recapped" card (toggle Last/This), a Top Genres breakdown (by title count or time), and a Reader Type badge (Completionist, Speed Reader, Devoted, and more) derived from your actual reading habits. Screen reorganized into a clear now → recent → all-time → rewards flow, with consistent sizing and spacing across every card

### Fixed

- Duplicate "top novel" entries when the same book was read from two sources now merge into one
- Reader Type's stat chips no longer mismatch in size

## [3.12.7] — 2026-08-04

### Fixed

- **App performance pass** — Reader scrolling and text-to-speech no longer redraw the whole visible page on every scroll pixel or every sentence; only what actually changed on screen updates now. Fixed a chapter-list bug where sorting or filtering chapters could make the list forget which rows it had already drawn. Removed an artificial ~1.1s wait from every app launch. Added missing database indexes so the library and reading-history screens no longer scan the whole table on every load, and batched several bulk operations (backup restore, offline-data backfill) that were making the database work far harder than necessary. No features changed — everything works the same, just faster and smoother

## [3.12.6] — 2026-08-03

### Fixed

- **Next/Previous chapter stuck on loading** — With Infinite Scroll off, tapping Next or Previous could leave the reader showing the loading overlay forever even though the chapter had fully loaded, only clearing after closing and reopening it. This also explains downloaded chapters occasionally looking "corrupted" or refusing to open — same stuck state, since it wasn't gated on being online

## [3.12.5] — 2026-08-03

### Added

- **Vertical seekbar (reader)** — Thumb-draggable progress bar pinned to the right edge of the reader. Tap or drag to jump anywhere in the current chapter in one gesture instead of flinging through screens; hides and reappears with the rest of the reader controls

## [3.12.4] — 2026-07-19

### Fixed

- **Reader jumping to the continue position while scrolling with Infinite Scroll on** — Opening a chapter with Infinite Scroll enabled preloads neighboring chapters right after the chapter opens; two separate pieces of code both raced to snap the reader back to the open position as those neighbors loaded, occasionally winning after you'd already started scrolling. Scroll restoration now happens at most once per chapter open.

## [3.12.3] — 2026-07-18

### Fixed

- **Google Drive sync merge** — Complete overhaul so local data is never removed or replaced by the online copy. Every book row now carries a change counter and syncs through a proper 3-way merge: reading positions no longer reset to an old chapter, deleted and migrated books stay deleted instead of reappearing, re-adding a previously deleted book keeps it in the library, and mark-as-unread now propagates to other devices instead of flipping back to read
- **Forced scroll on chapter open** — Tapping a chapter in the list now opens it at the top; only the Continue button and continue-reading cards restore your saved position
- **Reading position lost on rotation** — Rotating the device no longer jumps the reader to a random position; the page reflows in place
- **Library data safety** — A failed database migration can no longer silently wipe the library
- **Grid badge alignment** — The new-chapter count now sits centered in its badge on library cards

### Added

- **Reader text selection rework** — Selection now feels like native Android: long-press selects a word and dragging in the same gesture extends it, handles grab accurately, and a floating toolbar (Copy / Highlight / Dictionary / Add Note) hovers by the selection instead of a modal sheet interrupting every adjustment. One tap looks a word up in the dictionary or starts a note on the selection; tapping anywhere dismisses
- **Chapter progress indicator** — Partially read chapters show a "N%" hint in the chapter list
- **Select above / below** — In chapter selection mode, extend the selection to everything above or below the tapped chapter, then mark read / download in one go

## [3.12.2] — 2026-07-12

### Added

- **Remove Bloat (Reader)** — New toggle in Settings → Reader that strips per-chapter noise from every page: the chapter title duplicated inside the content and translator / editor / proofreader / quality-checker credit lines (including combined labels like "Translator/Editor:" and note-style lines). Cleans reading and TTS alike; off by default
- **Bundled source icons** — All source icons now ship inside the app in high quality instead of being fetched from the web each time. Source lists and headers render instantly with zero network use
- **NovelArrow** — Added as a new source (novelarrow.com)

### Fixed

- **NovelBuddy** — Followed the move to novelbuddy.me and rebuilt chapter loading for their site redesign; chapters open again. Old library entries keep working
- **NovelBin** — Restored at its new home novel-bin.com after the old domain went offline; browsing, details, full chapter lists, and reading all work again
- **NovelFire** — Chapters no longer fail to open (the site was serving a "Loading…" bot page to the app)
- **NovelDex** — Chapter list now sorts in correct numeric order (chapter 2 before chapter 10)
- **Reader crash on image-heavy chapters** — Novels with large embedded illustrations (e.g. Lnori) no longer crash the app while rendering; image decode size is now capped
- **App icon** — Fixed the icon appearing cropped or zoomed on devices with circular / squircle launcher masks

### Performance

- **Smooth scrolling on low-end devices** — Ship Compose baseline profiles so the app AOT-compiles hot paths at install time; measured scroll jank dropped from ~10% to under 1% on a low-end test device
- **Faster source switching** — Backing out of a loading source now cancels its network request immediately instead of letting it finish in the background, so opening a second source no longer stutters while the first keeps working

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
