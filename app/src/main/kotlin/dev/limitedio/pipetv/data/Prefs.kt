package dev.limitedio.pipetv.data

import android.content.Context
import dev.limitedio.pipetv.App
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** User settings, backed by SharedPreferences. */
object Prefs {
    val qualities = intArrayOf(0, 360, 480, 720, 1080, 1440, 2160)
    val speeds = (1..12).map { it * 0.25f }
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

    var language: String
        get() = sp.getString("lang", "auto") ?: "auto"
        set(v) = sp.edit().putString("lang", v).apply()
    var kbLangs: List<String>
        get() = (sp.getString("kb", "") ?: "").split(',').filter { it.isNotEmpty() }
        set(v) = sp.edit().putString("kb", v.joinToString(",")).apply()
    var subs: Boolean
        get() = sp.getBoolean("subs", false)
        set(v) = sp.edit().putBoolean("subs", v).apply()
    var volume: Float
        get() = sp.getFloat("volume", 1f)
        set(v) = sp.edit().putFloat("volume", v).apply()

    var content: String
        get() = sp.getString("content", "") ?: ""
        set(v) = sp.edit().putString("content", v).apply()
    var sfx: Boolean
        get() = sp.getBoolean("sfx", true)
        set(v) = sp.edit().putBoolean("sfx", v).apply()
    var music: Boolean
        get() = sp.getBoolean("music", true)
        set(v) = sp.edit().putBoolean("music", v).apply()
    var ambient: Boolean
        get() = sp.getBoolean("ambient", true)
        set(v) = sp.edit().putBoolean("ambient", v).apply()

    /** Mirror the layout (right-to-left). Off by default, even for Arabic: the user is asked. */
    var rtl: Boolean
        get() = sp.getBoolean("rtl", false)
        set(v) = sp.edit().putBoolean("rtl", v).apply()

    fun qualityLabel(q: Int = quality) = if (q == 0) S.t(K.AUTO) else "${q}p"
}
