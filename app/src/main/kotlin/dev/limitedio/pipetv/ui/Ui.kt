package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface

/** Design tokens. Sizes are authored for a 1080p screen and scaled to any TV via [u]. */
object Ui {
    const val BG = 0xFF07080F.toInt()
    const val SURFACE = 0xFF141725.toInt()
    const val ACCENT = 0xFF7C5CFF.toInt()
    const val ACCENT2 = 0xFF00E0C6.toInt()
    const val TEXT = 0xFFF2F3FA.toInt()
    const val DIM = 0xFF8A8FA8.toInt()
    private var scale = 1f

    fun init(c: Context) {
        val m = c.resources.displayMetrics
        scale = minOf(m.widthPixels, m.heightPixels) / 1080f
    }

    fun u(n: Float) = n * scale
    fun u(n: Int) = n * scale

    fun paint(color: Int = TEXT, size: Float = 0f, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        if (size > 0) textSize = u(size)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    fun fmt(ms: Long): String {
        val s = ms / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
    }
}
