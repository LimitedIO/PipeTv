package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.media.Playback
import dev.limitedio.pipetv.media.SponsorBlock

/** The only SponsorBlock chrome: colored spans inside the existing progress track. */
object SponsorMarks {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun draw(c: Canvas, left: Float, top: Float, right: Float, bottom: Float, alpha: Float = 1f) {
        if (!Prefs.sponsorBlock || Playback.live || Playback.duration <= 0) return
        val duration = Playback.duration.toDouble()
        for (s in SponsorBlock.segments) {
            if (!SponsorBlock.matches(s)) continue
            val x0 = left + (right - left) * (s.start / duration).coerceIn(0.0, 1.0).toFloat()
            val x1 = left + (right - left) * (s.end / duration).coerceIn(0.0, 1.0).toFloat()
            paint.color = s.color; paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
            c.drawRect(x0, top, x1, bottom, paint)
        }
    }
}
