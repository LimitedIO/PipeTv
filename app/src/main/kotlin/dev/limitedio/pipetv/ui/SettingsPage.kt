package dev.limitedio.pipetv.ui

import android.content.Context
import android.view.ViewGroup
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.widgets.RowList
import dev.limitedio.pipetv.ui.widgets.SubMenu

/** The settings list with its pop-up menu on top. Keys go to the pop-up while it is open. */
class SettingsPage(c: Context, onLang: () -> Unit, onKb: () -> Unit, toast: (String) -> Unit, onAbout: () -> Unit) : ViewGroup(c) {
    private val list = RowList(c, { S.t(K.SETTINGS) }, centerFocus = true)
    private val sub = SubMenu(c).apply { visibility = GONE }

    init {
        addView(list); addView(sub)
        list.rows = Settings.rows(sub, onLang, onKb, toast, onAbout)
    }

    fun key(code: Int, repeat: Int): Boolean = if (sub.open) sub.key(code, repeat).also { list.invalidate() } else list.key(code, repeat)
    fun closeSub(): Boolean = sub.open.also { if (it) { sub.close(); list.invalidate() } }
    fun lit(on: Boolean) { list.lit = on; list.invalidate() }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) { list.layout(0, 0, r - l, b - t); sub.layout(0, 0, r - l, b - t) }
}
