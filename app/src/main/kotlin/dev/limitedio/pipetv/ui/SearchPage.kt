package dev.limitedio.pipetv.ui

import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Yt

/** Keyboard on the left, live results on the right. Typing searches after a short pause. */
class SearchPage(c: Context) : ViewGroup(c) {
    val keyboard = Keyboard(c)
    val grid = Grid(c, "Results").apply { cols = 2; lit = false; busy = false }
    private var onGrid = false
    private var on = true
    private var token = 0
    private val go = Runnable { run() }

    init {
        addView(keyboard); addView(grid)
        keyboard.onKey = { k ->
            when (k) {
                "Space" -> type(" "); "Del" -> keyboard.text = keyboard.text.dropLast(1).also { schedule() }
                "Clear" -> { keyboard.text = ""; schedule() }
                "Go" -> run()
                else -> type(k)
            }
            keyboard.invalidate()
        }
    }

    var onPick: (Item) -> Unit
        get() = grid.onPick
        set(v) { grid.onPick = v }

    fun type(s: String) { keyboard.text += s; keyboard.invalidate(); schedule() }
    fun backspace() { keyboard.text = keyboard.text.dropLast(1); keyboard.invalidate(); schedule() }

    private fun schedule() { Bg.main.removeCallbacks(go); Bg.main.postDelayed(go, 700) }

    private fun run() {
        val q = keyboard.text.trim()
        val t = ++token
        if (q.isEmpty()) { grid.items = emptyList(); grid.busy = false; return }
        grid.busy = true; grid.message = "Searching…"; grid.invalidate()
        Bg.io({ Yt.search(q) }) { r ->
            if (t != token) return@io
            grid.message = if (r == null) "Search failed. Check your connection." else null
            grid.items = r ?: emptyList(); grid.busy = false
        }
    }

    fun key(code: Int): Boolean {
        if (onGrid) {
            if (grid.key(code)) return true
            if (code != KeyEvent.KEYCODE_DPAD_LEFT) return false
            focus(false); return true
        }
        if (keyboard.key(code)) return true
        if (code == KeyEvent.KEYCODE_DPAD_RIGHT && grid.items.isNotEmpty()) { focus(true); return true }
        return false
    }

    fun lit(v: Boolean) { on = v; focus(onGrid) }

    private fun focus(g: Boolean) { onGrid = g; grid.lit = on && g; keyboard.lit = on && !g; grid.invalidate(); keyboard.invalidate() }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val kw = ((r - l) * .38f).toInt()
        keyboard.layout(0, 0, kw, b - t); grid.layout(kw, 0, r - l, b - t)
    }
}
