package dev.limitedio.pipetv.ui.search

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.ui.Grid
import dev.limitedio.pipetv.ui.Ui
import dev.limitedio.pipetv.ui.input.Keyboard
import dev.limitedio.pipetv.ui.input.Voice

/** Keyboard + live suggestions, then filterable, endlessly scrolling results. */
class SearchPage(c: Context) : ViewGroup(c) {
    val keyboard = Keyboard(c)
    val grid = Grid(c) { "" }.apply { cols = 3; upExit = true; busy = false }
    private val suggest = SuggestList(c); private val chips = Chips(c)
    private val orb = VoiceOrb(c).apply { visibility = GONE }
    private val data = SearchData(grid, suggest)
    var onVoice: () -> Unit = {}
    private var zone = 0; private var on = true // zone: 0 keyboard, 1 list/results, 2 chips
    private val ask = Runnable { data.suggestions(keyboard.text.trim()) }

    fun picks(pick: (Item) -> Unit, menu: (Item) -> Unit) { grid.onPick = pick; grid.onMenu = menu }

    init {
        listOf<View>(suggest, chips, grid, keyboard, orb).forEach { addView(it) }
        keyboard.onKey = { k ->
            when (k) {
                "@space" -> type(" "); "@del" -> backspace(); "@clear" -> setText(""); "@go" -> submit(keyboard.text); "@mic" -> onVoice()
                else -> type(k)
            }
        }
        suggest.onPick = { submit(it) }
        chips.onChange = { if (data.results) submit(keyboard.text) }
        grid.onMore = { data.more() }
        Voice.onResult = { post { setText(it); submit(it) } }
        Voice.onState = { keyboard.listening = Voice.active; orb.visibility = if (Voice.active) VISIBLE else GONE; orb.invalidate(); keyboard.invalidate() }
        data.showRecents(); showList(true)
    }

    fun type(s: String) = setText(keyboard.text + s)
    fun backspace() = setText(keyboard.text.dropLast(1))

    /** A physical-keyboard character: types it (returns false for navigation and system keys). */
    fun typed(e: KeyEvent): Boolean {
        val ch = e.unicodeChar; val k = e.keyCode
        if (k == KeyEvent.KEYCODE_DEL) { backspace(); return true }
        if (ch < 32 || e.isSystem || k in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) return false
        type(String(Character.toChars(ch))); return true
    }

    private fun showList(list: Boolean) {
        suggest.visibility = if (list) VISIBLE else GONE
        grid.visibility = if (list) GONE else VISIBLE; chips.visibility = if (list) GONE else VISIBLE
    }

    private fun setText(t: String) {
        keyboard.text = t; keyboard.invalidate()
        data.typing(); showList(true); relayout(); Bg.main.removeCallbacks(ask)
        if (t.isBlank()) data.showRecents() else Bg.main.postDelayed(ask, 250)
    }

    /** Results show a slim query bar instead of the keyboard; Left or Up on them brings the keyboard back. */
    private fun expand() { data.typing(); showList(true); setZone(0); relayout(); data.suggestions(keyboard.text.trim()) }

    /** Back on results: clear the search and open the keyboard again. */
    fun back(): Boolean { if (!data.results) return false; setText(""); setZone(0); relayout(); return true }

    fun submit(q: String) {
        val query = q.trim(); if (query.isEmpty()) return
        keyboard.text = query; keyboard.invalidate()
        Bg.main.removeCallbacks(ask); showList(false)
        data.search(query, chips.values[chips.sel]); setZone(1); relayout()
    }

    private fun setZone(z: Int) {
        zone = z
        keyboard.lit = on && z == 0; suggest.lit = on && z == 1 && !data.results; grid.lit = on && z == 1 && data.results; chips.lit = on && z == 2
        listOf(keyboard, suggest, grid, chips).forEach { it.invalidate() }
    }

    fun lit(v: Boolean) { on = v; setZone(zone) }

    fun key(code: Int): Boolean = when (zone) {
        0 -> keyboard.key(code) || (code == KeyEvent.KEYCODE_DPAD_RIGHT && (data.results || suggest.items.isNotEmpty())).also { if (it) setZone(1) }
        1 -> (if (data.results) grid.key(code) else suggest.key(code)) || when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { if (data.results) expand() else setZone(0); true }
            KeyEvent.KEYCODE_DPAD_UP -> if (data.results) { setZone(2); true } else false
            else -> false
        }
        else -> chips.key(code) || (code == KeyEvent.KEYCODE_DPAD_UP).also { if (it) expand() } || (code == KeyEvent.KEYCODE_DPAD_DOWN).also { if (it) setZone(1) }
    }

    private fun relayout() = onLayout(true, left, top, right, bottom)
    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) =
        SearchLayout.apply(r - l, b - t, data.results, keyboard, suggest, chips, grid, orb)
}
