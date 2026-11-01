package com.kmhmubin.kothagolp.ui.screens.home.tabs.history

/** Every user-facing string on History (spec 2026-10-31-history-design, "Copy"). */
object HistoryCopy {
    const val SCREEN_TITLE = "History"
    fun subtitle(n: Int) = "$n ${if (n == 1) "novel" else "novels"} you've opened"
    const val SEARCH_HINT = "Search your history"

    const val TODAY = "Today"
    const val YESTERDAY = "Yesterday"
    const val THIS_WEEK = "This week"
    const val THIS_MONTH = "This month"
    const val EARLIER = "Earlier"

    const val JUST_NOW = "Just now"
    fun minutesAgo(n: Long) = "${n}m ago"
    fun hoursAgo(n: Long) = "${n}h ago"

    const val REMOVE = "Remove"
    const val SELECT = "Select"
    const val SELECT_ALL = "Select all"
    fun removeN(n: Int) = "Remove $n"
    fun selected(n: Int) = "$n selected"
    const val COPY_TITLE = "Copy title"
    const val TITLE_COPIED = "Title copied"
    const val CONTINUE = "Continue"
    const val NOVEL_PAGE = "Novel page"
    const val IN_LIBRARY = "In library"
    const val ADD_LIBRARY = "＋ Library"
    const val ADDED_TO_LIBRARY = "Added to your library"
    const val UNDO = "Undo"
    fun removedOne(title: String) = "Removed \"$title\" from history"
    fun removedMany(n: Int) = "Removed $n novels from history"

    const val CLEAR_MENU = "Clear history…"
    const val CLEAR_TITLE = "Clear your history?"
    fun clearBody(n: Int) =
        "This removes ${if (n == 1) "the 1 novel" else "all $n novels"} from History. Your library, downloads and reading progress stay as they are."
    const val CANCEL = "Cancel"
    const val CLEAR_CONFIRM = "Clear history"

    fun results(n: Int) = "$n ${if (n == 1) "result" else "results"}"
    fun noMatch(q: String) = "No novels match \"$q\""
    const val NO_MATCH_BODY = "Only novels you've opened are in History."
    const val SEARCH_ALL_SOURCES = "Search all sources"

    const val EMPTY_TITLE = "Nothing here yet"
    const val EMPTY_BODY = "Novels you open show up here, so you can jump back in."
    const val FIND_NOVELS = "Find novels"

    fun percentRead(p: Int) = "$p% read"
    fun chapterOf(n: Int, total: Int) = "Ch. $n of $total"

    // Accessibility labels for icon-only controls.
    const val CD_SEARCH = "Search history"
    const val CD_MORE = "More options"
    const val CD_DONE_SELECTING = "Done selecting"
    const val CD_BACK = "Back"
    const val CD_CLEAR_QUERY = "Clear search"
}
