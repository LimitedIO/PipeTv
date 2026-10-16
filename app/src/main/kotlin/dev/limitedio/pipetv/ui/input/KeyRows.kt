package dev.limitedio.pipetv.ui.input

import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** One key: [token] is what is sent when pressed ("@del", "@go"... or the character itself). */
class Key(val token: String, val label: String, val span: Float, val row: Int, var x: Float = 0f)

/** Builds the rows of keys for a layout / shift / symbols state and centres each row (13 units wide). */
object KeyRows {
    fun build(l: Layout, shift: Boolean, sym: Boolean): List<List<Key>> {
        val body = if (sym) Layouts.symbols else l.rows.map { if (shift && l.cased) it.uppercase() else it }
        val list = ArrayList<List<Key>>()
        list.add("1234567890".map { Key(it.toString(), it.toString(), 1f, 0) })
        body.forEachIndexed { i, s -> list.add(s.map { Key(it.toString(), it.toString(), 1f, i + 1) }) }
        val b = body.size + 1
        list.add(listOf(Key("@shift", "", 1.3f, b), Key("@sym", if (sym) "ABC" else "?123", 1.4f, b), Key("@lang", l.code.uppercase(), 1.2f, b),
            Key("@space", S.t(K.SPACE), 2.8f, b), Key("@mic", "", 1.2f, b), Key("@del", "", 1.2f, b), Key("@clear", S.t(K.CLEAR), 1.6f, b), Key("@go", S.t(K.GO), 1.6f, b)))
        list.forEach { row ->
            var x = (13f - row.sumOf { it.span.toDouble() }.toFloat()) / 2f
            for (k in row) { k.x = x; x += k.span }
        }
        return list
    }
}
