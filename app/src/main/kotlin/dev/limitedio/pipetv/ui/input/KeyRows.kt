package dev.limitedio.pipetv.ui.input

import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** One block of the keyboard grid: [token] is what is sent when pressed ("@del", "@go"... or the character itself). */
class Key(val token: String, val label: String, val col: Int, val row: Int, val cs: Int = 1, val rs: Int = 1)

/** A keyboard as a plain X-Y grid of equal blocks (like Tetris), [cols] x [rows] cells. */
class Pad(val keys: List<Key>, val cols: Int, val rows: Int)

/** Frequent actions sit above the letters; numbers are available on the symbols layout. */
object KeyRows {
    fun build(l: Layout, shift: Boolean, sym: Boolean): Pad {
        val letters = if (sym) Layouts.symbols.joinToString("") else l.rows.joinToString("").let { if (shift && l.cased) it.uppercase() else it }
        val chars = if (sym) "1234567890" + letters else letters
        val lc = if (chars.length > 42) 9 else 7; val u = lc * 2; val rows = (chars.length + lc - 1) / lc
        val keys = ArrayList<Key>()
        val spans = if (u == 14) listOf(4, 3, 3, 4) else listOf(5, 4, 4, 5)
        val labels = listOf(S.t(K.CLEAR), "", "", S.t(K.GO))
        var x = 0
        listOf("@clear", "@del", "@mic", "@go").forEachIndexed { i, t -> keys.add(Key(t, labels[i], x, 0, spans[i])); x += spans[i] }
        chars.forEachIndexed { i, ch -> keys.add(Key(ch.toString(), ch.toString(), i % lc * 2, 1 + i / lc, 2)) }
        val tools = if (u == 14) listOf(2, 3, 3, 6) else listOf(3, 3, 3, 9)
        val toolLabels = listOf("", if (sym) "ABC" else "?123", l.code.uppercase(), S.t(K.SPACE))
        x = 0
        listOf("@shift", "@sym", "@lang", "@space").forEachIndexed { i, t -> keys.add(Key(t, toolLabels[i], x, rows + 1, tools[i])); x += tools[i] }
        return Pad(keys, u, rows + 2)
    }

    /** Colour code: digits blue, delete red, clear amber, shift/symbols/language violet, mic sky, space grey, search teal. */
    fun face(t: String) = when (t) {
        "@del" -> 0xFF3B1A24.toInt(); "@clear" -> 0xFF3A2C12.toInt(); "@shift", "@sym", "@lang" -> 0xFF261E47.toInt()
        "@mic" -> 0xFF14304A.toInt(); "@space" -> 0xFF1D2136.toInt(); "@go" -> 0xFF0F4A44.toInt()
        else -> if (t[0].isDigit()) 0xFF172338.toInt() else 0xFF171A2B.toInt()
    }

    fun ink(t: String) = when (t) {
        "@del" -> 0xFFFF7A8C.toInt(); "@clear" -> 0xFFFFC857.toInt(); "@shift", "@sym", "@lang" -> 0xFFB9A6FF.toInt()
        "@mic" -> 0xFF6CC4FF.toInt(); "@space" -> 0xFFAEB3CC.toInt(); "@go" -> 0xFF00E0C6.toInt()
        else -> if (t[0].isDigit()) 0xFF9CC4FF.toInt() else 0xFFF2F3FA.toInt()
    }
}
