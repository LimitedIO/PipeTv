package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Shared glyphs for the compact menu player. */
object Actions {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun icon(c: Canvas, i: Int, x: Float, y: Float, s: Float) {
        ink.color = Ui.TEXT
        ink.style = Paint.Style.STROKE; ink.strokeWidth = s * .16f; ink.strokeCap = Paint.Cap.ROUND; ink.strokeJoin = Paint.Join.ROUND
        path.reset()
        when (i) {
            0 -> { val a = s * .5f; for ((sx, sy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) { path.moveTo(x + sx * s, y + sy * (s - a)); path.lineTo(x + sx * s, y + sy * s); path.lineTo(x + sx * (s - a), y + sy * s) } }
            1 -> if (Engine.exo.playWhenReady) { Icons.pause(c, x, y, s * .8f, ink.apply { style = Paint.Style.FILL }) } else { ink.style = Paint.Style.FILL; path.moveTo(x - s * .6f, y - s); path.lineTo(x + s, y); path.lineTo(x - s * .6f, y + s); path.close() }
            2 -> { ink.style = Paint.Style.FILL; path.moveTo(x - s, y - s * .9f); path.lineTo(x + s * .5f, y); path.lineTo(x - s, y + s * .9f); path.close(); path.addRect(x + s * .65f, y - s * .9f, x + s, y + s * .9f, Path.Direction.CW) }
            3 -> { Glyphs.star(c, x, y, s * 1.1f, ink, Session.item?.let { Favs.has(it.id) } == true); return }
            4 -> for (n in -1..1) { path.moveTo(x - s, y + n * s * .65f); path.lineTo(x + s, y + n * s * .65f) }
            else -> { path.moveTo(x - s * .8f, y - s * .8f); path.lineTo(x + s * .8f, y + s * .8f); path.moveTo(x + s * .8f, y - s * .8f); path.lineTo(x - s * .8f, y + s * .8f) }
        }
        c.drawPath(path, ink); ink.style = Paint.Style.FILL
    }
}
