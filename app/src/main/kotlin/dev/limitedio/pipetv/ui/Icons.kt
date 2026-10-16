package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/** Vector icons drawn with plain canvas paths: no drawables, no XML. */
object Icons {
    const val HOME = 0; const val SEARCH = 1; const val TREND = 2; const val HISTORY = 3; const val SETTINGS = 4; const val MIC = 5; const val DEL = 6; const val SHIFT = 7
    private val path = Path()
    private val RowRect = android.graphics.RectF()

    fun draw(c: Canvas, icon: Int, cx: Float, cy: Float, r: Float, p: Paint) {
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.17f
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        path.reset()
        when (icon) {
            HOME -> {
                path.moveTo(cx - r, cy); path.lineTo(cx, cy - r); path.lineTo(cx + r, cy)
                path.moveTo(cx - r * .7f, cy - r * .2f); path.lineTo(cx - r * .7f, cy + r * .8f)
                path.lineTo(cx + r * .7f, cy + r * .8f); path.lineTo(cx + r * .7f, cy - r * .2f)
                c.drawPath(path, p)
            }
            SEARCH -> {
                c.drawCircle(cx - r * .15f, cy - r * .15f, r * .65f, p)
                c.drawLine(cx + r * .35f, cy + r * .35f, cx + r * .9f, cy + r * .9f, p)
            }
            TREND -> {
                path.moveTo(cx + r * .25f, cy - r); path.lineTo(cx - r * .5f, cy + r * .1f)
                path.lineTo(cx, cy + r * .1f); path.lineTo(cx - r * .25f, cy + r)
                path.lineTo(cx + r * .5f, cy - r * .15f); path.lineTo(cx, cy - r * .15f); path.close()
                c.drawPath(path, p)
            }
            HISTORY -> {
                c.drawCircle(cx, cy, r * .88f, p)
                c.drawLine(cx, cy, cx, cy - r * .5f, p); c.drawLine(cx, cy, cx + r * .4f, cy + r * .2f, p)
            }
            MIC -> {
                RowRect.set(cx - r * .38f, cy - r, cx + r * .38f, cy + r * .2f); c.drawRoundRect(RowRect, r * .38f, r * .38f, p)
                RowRect.set(cx - r * .75f, cy - r * .55f, cx + r * .75f, cy + r * .6f); c.drawArc(RowRect, 20f, 140f, false, p)
                c.drawLine(cx, cy + r * .6f, cx, cy + r, p); c.drawLine(cx - r * .4f, cy + r, cx + r * .4f, cy + r, p)
            }
            DEL -> {
                path.moveTo(cx - r, cy); path.lineTo(cx - r * .4f, cy - r * .7f); path.lineTo(cx + r, cy - r * .7f)
                path.lineTo(cx + r, cy + r * .7f); path.lineTo(cx - r * .4f, cy + r * .7f); path.close(); c.drawPath(path, p)
                c.drawLine(cx, cy - r * .3f, cx + r * .5f, cy + r * .3f, p); c.drawLine(cx + r * .5f, cy - r * .3f, cx, cy + r * .3f, p)
            }
            SHIFT -> {
                path.moveTo(cx, cy - r); path.lineTo(cx + r * .8f, cy); path.lineTo(cx + r * .35f, cy); path.lineTo(cx + r * .35f, cy + r * .8f)
                path.lineTo(cx - r * .35f, cy + r * .8f); path.lineTo(cx - r * .35f, cy); path.lineTo(cx - r * .8f, cy); path.close(); c.drawPath(path, p)
            }
            SETTINGS -> {
                c.drawCircle(cx, cy, r * .38f, p)
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    c.drawLine(cx + cos(a).toFloat() * r * .65f, cy + sin(a).toFloat() * r * .65f,
                        cx + cos(a).toFloat() * r, cy + sin(a).toFloat() * r, p)
                }
            }
        }
        p.style = Paint.Style.FILL
    }

    fun play(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        path.reset()
        path.moveTo(cx - r * .6f, cy - r); path.lineTo(cx - r * .6f, cy + r); path.lineTo(cx + r, cy); path.close()
        c.drawPath(path, p)
    }

    fun pause(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        c.drawRoundRect(cx - r * .8f, cy - r, cx - r * .15f, cy + r, r * .15f, r * .15f, p)
        c.drawRoundRect(cx + r * .15f, cy - r, cx + r * .8f, cy + r, r * .15f, r * .15f, p)
    }
}
