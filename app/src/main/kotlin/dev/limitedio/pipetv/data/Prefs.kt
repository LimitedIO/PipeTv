package dev.limitedio.pipetv.data

import android.content.Context
import dev.limitedio.pipetv.App

/** User settings, backed by SharedPreferences. */
object Prefs {
    val qualities = intArrayOf(0, 360, 480, 720, 1080, 1440, 2160)
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    private val sp by lazy { App.ctx.getSharedPreferences("pipetv", Context.MODE_PRIVATE) }

    var quality: Int
        get() = sp.getInt("quality", 0)
        set(v) = sp.edit().putInt("quality", v).apply()
    var autoplay: Boolean
        get() = sp.getBoolean("autoplay", true)
        set(v) = sp.edit().putBoolean("autoplay", v).apply()
    var speed: Float
        get() = sp.getFloat("speed", 1f)
        set(v) = sp.edit().putFloat("speed", v).apply()

    fun qualityLabel(q: Int = quality) = if (q == 0) "Auto" else "${q}p"
}
