package dev.limitedio.pipetv.ui.input

import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** One block of the keyboard grid: [token] is what is sent when pressed ("@del", "@go"... or the character itself). */
class Key(val token: String, val label: String, val col: Int, val row: Int, val cs: Int = 1, val rs: Int = 1)

/** A keyboard as a plain X-Y grid of equal blocks (like Tetris), [cols] x [rows] cells. */
class Pad(val keys: List<Key>, val cols: Int, val rows: Int)

/** Characters fill the left block row by row; two columns of big action blocks sit on the right. */
object KeyRows {
    fun build(l: Layout, shift: Boolean, sym: Boolean): Pad {
        val letters = if (sym) Layouts.symbols.joinToString("") else l.rows.joinToString("").let { if (shift && l.cased) it.uppercase() else it }
        val chars = letters + "1234567890"
        val lc = if (chars.length > 42) 9 else 7
        val rows = maxOf(5, (chars.length + lc - 1) / lc)
        val keys = ArrayList<Key>()
        chars.forEachIndexed { i, ch -> keys.add(Key(ch.toString(), ch.toString(), i % lc, i / lc)) }
        keys.addAll(listOf(
            Key("@del", "", lc, 0), Key("@clear", S.t(K.CLEAR), lc + 1, 0),
            Key("@shift", "", lc, 1), Key("@sym", if (sym) "ABC" else "?123", lc + 1, 1),
            Key("@lang", l.code.uppercase(), lc, 2), Key("@mic", "", lc + 1, 2),
            Key("@space", S.t(K.SPACE), lc, 3, 2, rows - 4), Key("@go", S.t(K.GO), lc, rows - 1, 2, 1)))
        return Pad(keys, lc + 2, rows)
    }
}
