package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation

/** Every user-facing string on For You (spec v4 copy deck, "New" column). Sentence case, reader words only. */
object ForYouCopy {
    const val SCREEN_TITLE = "For You"
    fun leadPickedFrom(count: Int) = "Picked from $count novels you've read"
    fun becauseYouLiked(title: String) = "Because you liked $title"
    fun readersAlsoRead(title: String) = "Readers of $title read this too"
    fun somethingNew(genre: String) = "Something new: ${genre.lowercase()}"

    // Hint
    const val HINT_LONG_PRESS = "Press and hold a novel to see why it's here"
    const val HINT_DISMISS = "Got it"

    // Row titles
    const val MORE_LIKE = "More like"
    const val BY_AUTHORS = "By authors you like"
    const val PICKED_BY_AI = "Picked by AI"
    fun topGenre(genre: String) = "Top ${genre.lowercase()} for you"
    const val SOMETHING_DIFFERENT = "Something different"
    fun genreYouMightEnjoy(genre: String) = "$genre you might enjoy"
    const val JUST_ADDED = "Just added"
    const val BROWSE_BY_GENRE = "Browse by genre"

    // Top-bar menu
    const val MENU_REFRESH = "Refresh picks"
    const val MENU_TASTE = "Your taste"
    const val MENU_TASTE_SUPPORTING = "Genres you like and avoid"
    const val MENU_SHOW_MORE_LESS = "Show more or less"
    const val MENU_SHOW_MORE_LESS_SUPPORTING = "Boost or mute a genre"
    const val MENU_HIDDEN = "Hidden"
    fun novels(n: Int) = "$n ${if (n == 1) "novel" else "novels"}"
    fun authors(n: Int) = "$n ${if (n == 1) "author" else "authors"}"
    fun menuHiddenSupporting(novels: Int, authors: Int) = "${novels(novels)} · ${authors(authors)}"
    const val MENU_USE_ALL_SOURCES = "Use all my sources"
    const val MENU_USE_ALL_SOURCES_OFF = "Off: only show novels from the source you read most"
    const val MENU_AI_PICKS = "AI picks"
    const val MENU_AI_ON_FREE = "On · Free model"
    const val MENU_ON = "On"
    const val MENU_OFF = "Off"
    const val MORE_OPTIONS = "More options"
    const val SEE_ALL = "See all"
    const val CLOSE = "Close"

    // Show more or less
    const val FILTER_NEVER = "Never show"
    const val FILTER_LESS = "Show less"
    const val FILTER_MORE = "Show more"
    fun neverShow(genre: String) = "Never show $genre"
    fun showLess(genre: String) = "Show less $genre"
    fun showMore(genre: String) = "Show more $genre"
    const val COUNT_NEVER = "never shown"
    const val COUNT_LESS = "shown less"
    const val COUNT_MORE = "shown more"
    const val RESET_ALL = "Reset all"
    const val SEARCH_GENRES = "Search genres…"
    const val JUMP_TO = "Jump to"
    fun showingMore(genre: String) = "Showing more $genre"

    // Long-press sheet
    const val WHY_HEADER = "Why it's here"
    const val STRONG_MATCH = "Strong match for you"
    const val GOOD_MATCH = "Good match for you"
    const val WHY_YOUR_GENRES = "Your genres"
    const val WHY_LIKE_FAVOURITES = "Like your favourites"
    const val WHY_READERS = "Readers of your novels"
    const val WHY_RATING = "Reader rating"
    const val ACTION_OPEN = "Open"
    const val ACTION_ADD_LIBRARY = "Add to library"
    const val ACTION_NOT_FOR_ME = "Not for me"
    const val ACTION_HIDE_AUTHOR = "Hide everything by this author"
    const val UNDO = "Undo"
    fun notForMeToast(title: String) = "Got it, you won't see \"$title\" again"
    fun hideAuthorToast(author: String) = "You won't see novels by $author"

    // Hidden screen
    const val HIDDEN_NOVELS = "Hidden novels"
    const val HIDDEN_AUTHORS = "Hidden authors"
    const val SHOW_AGAIN = "Show again"
    const val SHOW_ALL_AGAIN = "Show all again"
    const val NOTHING_HIDDEN = "Nothing hidden"
    const val NOTHING_HIDDEN_BODY = "Novels you mark Not for me show up here, so you can bring them back."

    // Taste screen
    const val TASTE_TITLE = "Your taste"
    const val TASTE_BODY = "Tap a genre once to like it, twice to avoid it."
    const val TASTE_LEGEND = "Tap once to like · twice to avoid · again to reset"
    const val TASTE_DONE = "Done"
    const val TASTE_CORE = "Main genres"
    const val TASTE_WEB = "Web novel genres"
    const val TASTE_CONTENT = "Mature content"
    const val TASTE_CONTENT_SECTION = "Content"
    const val TASTE_CONTENT_BODY = "Adult, NSFW, smut"
    const val TASTE_BL = "Boys' love (BL)"
    const val TASTE_BL_BODY = "Yaoi, M/M romance"
    const val TASTE_GL = "Girls' love (GL)"
    const val TASTE_GL_BODY = "Yuri, F/F romance"

    // Loading
    const val LOADING_TITLE = "Finding novels for you"
    const val LOADING_BODY = "This takes a minute, only the first time."
    fun lookingThrough(source: String) = "Looking through $source…"
    const val LOADING_SUBTITLE = "Getting to know your taste…"

    // Empty states
    const val EMPTY_TITLE = "Your picks will show up here"
    const val EMPTY_BODY = "Add a few novels to your library and For You will learn what you enjoy."
    const val EMPTY_ACTION = "Find novels"
    fun nothingLike(title: String) = "Nothing like \"$title\" yet"
    const val NOTHING_LIKE_BODY = "Try another novel, or check back after more novels are found."
    fun noGenreNovels(genre: String) = "No $genre novels yet"
    const val NO_GENRE_BODY = "Browse a source to find some."

    // AI states
    const val AI_OFF = "Get picks chosen by AI. It's free."
    const val AI_TURN_ON = "Turn on"
    const val TRY_AGAIN = "Try again"
    const val AI_LOADING = "The AI is choosing novels for you…"
    const val AI_UNREADABLE = "The AI's answer didn't come through. Try again."
    const val AI_BUSY = "The free AI is busy right now. Try again in a minute."
    const val AI_DAILY_LIMIT = "You've used today's free AI picks. They'll be back tomorrow."
    const val AI_BAD_KEY = "That AI key didn't work. Check it in AI picks."
    const val AI_NETWORK = "Couldn't reach the AI. Check your connection and try again."
    const val AI_NEED_BROWSE = "Browse a few novels first so the AI has something to choose from."

    // Errors / toasts
    const val LOAD_ERROR = "Couldn't load your picks. Pull down to try again."
    const val ADDED_TO_LIBRARY = "Added to your library"
    fun foundNewNovels(count: Int) = "Found $count new novels for you"
}
