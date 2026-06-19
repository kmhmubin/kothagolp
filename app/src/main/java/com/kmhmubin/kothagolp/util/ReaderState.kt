package com.kmhmubin.kothagolp.util

import java.util.concurrent.atomic.AtomicBoolean

object ReaderState {
    // True while ReaderContainer is in composition; suppresses native text ActionMode
    val readerActive = AtomicBoolean(false)
}
