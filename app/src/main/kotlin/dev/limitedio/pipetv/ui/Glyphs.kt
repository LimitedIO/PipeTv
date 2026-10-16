package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/** Extra vector shapes: the favourite star and the infinity sign (endless playlists). */
object Glyphs {
    const val FAV = 99
    private val path = Path()

    fun star(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint, filled: Boolean) {
        path.reset()
        for (i in 0 until 10) {
            val a = -Math.PI / 2 + i * Math.PI / 5; val rr = if (i % 2 == 0) r else r * .45f
            val x = cx + cos(a).toFloat() * rr; val y = cy + sin(a).toFloat() * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        p.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE; p.strokeWidth = r * .16f; p.strokeJoin = Paint.Join.ROUND
        c.drawPath(path, p); p.style = Paint.Style.FILL
    }

    fun infinity(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        path.reset()
        path.moveTo(cx, cy)
        path.cubicTo(cx + r * .45f, cy - r * .75f, cx + r * 1.25f, cy - r * .75f, cx + r * 1.25f, cy)
        path.cubicTo(cx + r * 1.25f, cy + r * .75f, cx + r * .45f, cy + r * .75f, cx, cy)
        path.cubicTo(cx - r * .45f, cy - r * .75f, cx - r * 1.25f, cy - r * .75f, cx - r * 1.25f, cy)
        path.cubicTo(cx - r * 1.25f, cy + r * .75f, cx - r * .45f, cy + r * .75f, cx, cy)
        p.style = Paint.Style.STROKE; p.strokeWidth = r * .22f; p.strokeCap = Paint.Cap.ROUND
        c.drawPath(path, p); p.style = Paint.Style.FILL
    }
}
