# History redesign — spec

Approved by the user on 2026-10-31. Same philosophy as For You v4: **Apple UX principles for structure and copy, Material 3 Expressive for every UI element, animation and micro-animation.**

Visual source of truth (open in a browser):
- `2026-10-31-history-design-screens.html` — screens 0–10 (list, swipe-remove, select several, after bulk remove, ⋮ menu, clear dialog, search, no match, empty, loading) + action table + copy additions. **Ignore its screen 2 sheet** — superseded below.
- `2026-10-31-history-design-sheet.html` — the press-and-hold sheet, compact version (the two "After" phones). **No progress bar anywhere** — progress is text ("88% read · 5h ago").

## Structure
- Large flexible top app bar: "History", subtitle "N novels you've opened" ("1 novel you've opened"); actions: search (⌕) and ⋮, both hidden when history is empty.
- Grouped cards (layout B): one rounded surface (24dp corners, surfaceContainerLow) per date group; header text above each card: Today · Yesterday · This week · This month · Earlier (sentence case, no counts). Hairline dividers inset past the cover.
- Row: cover 44×64 (10dp corners); line 1 title (1 line, ellipsis) + short time right-aligned; line 2 chapter ("Chapter 808 - 737: Jealous?" shortened: leading "Chapter " → "Ch. "); line 3 "Source · N%" (omit " · N%" when progress unknown). Whole row taps → continue reading (open reader at chapterUrl). No buttons on rows.
- Short time: today "2h ago" / "Just now" (<1 min) / "Nm ago"; yesterday "9:40 pm" (device 12/24h); within this week weekday short ("Tue"); older "12 Oct" (add year if not this year: "12 Oct 2025").
- Progress %: from the library entry for that novel url: `(lastReadChapterIndex + 1) * 100 / totalChapterCount`, clamped 0..100, only when totalChapterCount > 0. Library status also from the same lookup.

## Actions
| Action | Entry | Safety |
|---|---|---|
| Continue | tap row; sheet Continue | — |
| Novel page | sheet | — |
| Add to library | sheet "＋ Library" chip (only when not in library) | toast "Added to your library" · Undo |
| Copy title | sheet | toast "Title copied" |
| Remove one | swipe end→start past threshold; sheet Remove | toast `Removed "{title}" from history` · Undo (restores with original timestamp) |
| Select several | sheet Select; ⋮ → Select | — |
| Remove several | select mode floating toolbar "Remove N" | toast "Removed N novels from history" · Undo (no dialog) |
| Select all | floating toolbar | — |
| Clear history | ⋮ → "Clear history…" (error-colored, own menu group) | dialog "Clear your history?" / "This removes all N novels from History. Your library, downloads and reading progress stay as they are." / Cancel · Clear history |
| Search | ⌕ expands search bar (filters live by title, highlights match, results in one card under "N results") | ← / Back closes |
| No match | — | "No novels match \"{q}\"" / "Only novels you've opened are in History." / button "Search all sources" → Browse search with same query |
| Empty | — | "Nothing here yet" / "Novels you open show up here, so you can jump back in." / button "Find novels" → Browse |

Removing from history never touches read chapters / reading progress.

## Press-and-hold sheet (compact)
Header row: cover 46×68 · title · "Ch. {n} of {total} · {source}" (or "{chapter} · {source}" when total unknown) · "{N}% read · {short time}" (drop "{N}% read · " when unknown) · trailing "In library" label (secondaryContainer) or "＋ Library" assist chip. Then connected ButtonGroup [▶ Continue (wider) | Novel page]. Then one row of three tonal icon+label buttons: Copy title · Select · Remove (error color, rightmost). All targets ≥ 48dp.

## Material 3 Expressive — components
LargeFlexibleTopAppBar · FilledTonalIconButton (⌕, ⋮) · Expressive grouped menu (DropdownMenuPopup + DropdownMenuGroup) · SwipeToDismissBox with errorContainer background + icon/label · ModalBottomSheet · ButtonGroup (connected) · AssistChip / label · HorizontalFloatingToolbar for select mode · AlertDialog with icon · expanding SearchBar · LoadingIndicator for first load is NOT used — use skeleton cards shaped like the real list · Material shapes (Cookie9Sided for empty, Sunny for no-match) · Snackbar with action.

## Motion & micro-animations (required)
- Screen uses `MotionScheme.expressive()` (wrap History content in MaterialTheme(motionScheme = MotionScheme.expressive()) if the app theme doesn't already).
- List: `Modifier.animateItem()` on rows and group headers (spring fade + placement) so removes/undos/filters move smoothly; a group card that empties collapses away.
- Swipe: background color + icon scale animate with swipe progress; icon "pops" (spring scale 1→1.2→1) when passing the threshold; haptic tick at threshold.
- Row press: subtle scale 0.98 with spring on press; long-press haptic.
- Select mode: checkbox shape morphs circle → rounded square (MaterialShapes / animated corner) with check icon scale-in; selected row background animates to secondaryContainer; the floating toolbar enters with slide-up + fade (spring) and exits reverse; top bar title crossfades "History" ↔ "N selected" with the count animating (AnimatedContent).
- Search: search bar expands from the ⌕ button (container transform / expand animation), results animate in with animateItem.
- Sheet: default ModalBottomSheet spring; "＋ Library" chip morphs to "In library" label after adding (AnimatedContent).
- Empty / no-match: shape gently scales in on appear.
- Respect system "remove animations" setting (Compose does by default; don't hard-code durations that bypass it).

## Copy (all strings)
History · "N novels you've opened" / "1 novel you've opened" · Search your history · Today · Yesterday · This week · This month · Earlier · Just now · Nm ago · Nh ago · Remove · Select · Select all · Remove N · N selected · Copy title · Title copied · Continue · Novel page · In library · ＋ Library · Added to your library · Undo · Removed "{title}" from history · Removed N novels from history · Clear history… · Clear your history? · (dialog body above) · Cancel · Clear history · N results · No novels match "{q}" · Only novels you've opened are in History. · Search all sources · Nothing here yet · Novels you open show up here, so you can jump back in. · Find novels · "{N}% read" · "Ch. {n} of {total}".
