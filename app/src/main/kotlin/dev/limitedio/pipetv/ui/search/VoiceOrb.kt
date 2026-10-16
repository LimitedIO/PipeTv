package dev.limitedio.pipetv.ui.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.RadialGradient
import android.graphics.Paint
import android.graphics.Shader
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Icons
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui
import dev.limitedio.pipetv.ui.input.Voice

/** Pulsing microphone orb shown while the device is listening. */
class VoiceOrb(c: Context) : AnimView(c) {
    private val pulse = spring(0f, 120f, 12f)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val core = Ui.paint(Ui.ACCENT)
    private val ico = Ui.paint(Ui.TEXT)
    private val big = Ui.paint(Ui.TEXT, 40f, true)
    private val small = Ui.paint(Ui.DIM, 28f)
    override val animating get() = Voice.active

    override fun render(c: Canvas, dt: Float) {
        pulse.target = Voice.level
        val cx = width / 2f; val cy = height * .4f
        val rad = Ui.u(90) + pulse.value * Ui.u(110) + (System.nanoTime() / 1_000_000 % 1600) / 1600f * Ui.u(30)
        glow.shader = RadialGradient(cx, cy, rad, 0x887C5CFF.toInt(), 0, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, rad, glow)
        c.drawCircle(cx, cy, Ui.u(80), core)
        Icons.draw(c, Icons.MIC, cx, cy, Ui.u(34), ico)
        Txt.draw(c, S.t(K.LISTENING), 0f, cy + Ui.u(190), width.toFloat(), big, true)
        Txt.draw(c, Voice.partial.ifEmpty { S.t(K.SPEAK_NOW) }, Ui.u(40), cy + Ui.u(250), width - Ui.u(80), small, true)
    }
}
