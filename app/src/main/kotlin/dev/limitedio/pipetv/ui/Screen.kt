package dev.limitedio.pipetv.ui

/** A full-content page pushed on top of the main tabs (video details, channel, playlist). */
interface Screen {
    /** Returns true if the key was used; false lets the caller move focus elsewhere (e.g. to the nav rail). */
    fun key(code: Int, repeat: Int): Boolean
    fun lit(on: Boolean)

    /** Back pressed: return true if the page handled it itself (e.g. moved focus up to its menu). */
    fun back(): Boolean = false

    /** The item the focus is on (for the favourite key), if any. */
    fun focused(): dev.limitedio.pipetv.data.Item? = null
}
