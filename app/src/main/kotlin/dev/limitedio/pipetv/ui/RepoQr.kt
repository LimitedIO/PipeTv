package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** Offline QR for https://github.com/LimitedIO/PipeTv, including its four-module quiet zone. */
class RepoQr(c: Context) : AnimView(c) {
    private val black = Paint().apply { color = android.graphics.Color.BLACK }
    private val white = Paint().apply { color = android.graphics.Color.WHITE }
    private val title = Ui.paint(Ui.TEXT, 28f, true)
    private val dim = Ui.paint(Ui.DIM, 26f)
    private val matrix = """
        0000000000000000000000000000000000000
        0000000000000000000000000000000000000
        0000000000000000000000000000000000000
        0000000000000000000000000000000000000
        0000111111101000101110100011111110000
        0000100000100111011000101010000010000
        0000101110101000101001111010111010000
        0000101110100100001011100010111010000
        0000101110100011011000100010111010000
        0000100000101110000000101010000010000
        0000111111101010101010101011111110000
        0000000000000111001010110000000000000
        0000101000110001011100001001001010000
        0000101111000001111011011111000110000
        0000110100110001100100010011111010000
        0000111111001001010010101100110000000
        0000111101100011010110001011000010000
        0000011011001100100110111011000110000
        0000000001111001111111010111100010000
        0000010100001111001000111011000000000
        0000010000100001011100001110000010000
        0000000001001110011110110011001110000
        0000111100100100100100110000110010000
        0000000110011010110000101010100000000
        0000110101101011110010001111110100000
        0000000000001110100111001000111010000
        0000111111101010111111001010100010000
        0000100000100100101000011000100000000
        0000101110100011011100011111110010000
        0000101110100010111011011100111010000
        0000101110101011110101100100100110000
        0000100000100110111110101001110000000
        0000111111101101000010101000110010000
        0000000000000000000000000000000000000
        0000000000000000000000000000000000000
        0000000000000000000000000000000000000
        0000000000000000000000000000000000000
    """.trimIndent().lines()

    override fun render(c: Canvas, dt: Float) {
        val pad = Ui.u(20); val tw = width - pad * 2
        Txt.draw(c, Txt.fit(title, S.t(K.REPOSITORY), tw), pad, Ui.u(46), tw, title, true)
        val cell = (minOf(tw, Ui.u(330)) / matrix.size).toInt().coerceAtLeast(1)
        val size = cell * matrix.size; val x = ((width - size) / 2).toFloat(); val y = Ui.u(80).toInt().toFloat()
        c.drawRect(x, y, x + size, y + size, white)
        matrix.forEachIndexed { row, cells -> cells.forEachIndexed { col, value ->
            if (value == '1') c.drawRect(x + col * cell, y + row * cell, x + (col + 1) * cell, y + (row + 1) * cell, black)
        } }
        var ty = y + size + Ui.u(46)
        for (line in Txt.wrap(dim, S.t(K.SCAN_REPO), tw, 3)) { Txt.draw(c, line, pad, ty, tw, dim, true); ty += Ui.u(34) }
        Txt.draw(c, Txt.fit(title, "LimitedIO/PipeTv", tw), pad, ty + Ui.u(20), tw, title, true)
    }
}
