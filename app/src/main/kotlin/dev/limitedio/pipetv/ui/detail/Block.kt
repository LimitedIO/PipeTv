package dev.limitedio.pipetv.ui.detail

/** A block of text with a heading (a comment with its author, or the video description). */
class Block(val head: String, val tag: String, val body: String, val max: Int = 400, val more: Boolean = false) {
    var lines: List<String> = emptyList(); var w = 0
}
