package dev.limitedio.pipetv.ui

import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Regions
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.input.Layouts
import dev.limitedio.pipetv.ui.widgets.*
import dev.limitedio.pipetv.update.Updater

/** Settings content. Anything with more than on/off opens its own focused pop-up ([SubMenu]). */
object Settings {
    /** [onLang] runs after the app or content language changed, [onKb] after the keyboard layouts changed. */
    fun rows(sub: SubMenu, onLang: () -> Unit, onKb: () -> Unit, toast: (String) -> Unit, onAbout: () -> Unit): List<Row> {
        fun open(label: K, help: K, value: () -> String, act: () -> Unit) = ActionRow({ S.t(label) }, { S.t(help) }, value, act)
        fun toggle(label: K, help: K, get: () -> Boolean, set: (Boolean) -> Unit) = ToggleRow({ S.t(label) }, { S.t(help) }, get, set)
        return listOf(
            HeaderRow { S.t(K.S_PLAYBACK) },
            open(K.MAX_Q, K.MAX_Q_HELP, { Prefs.qualityLabel() }) {
                sub.choose(S.t(K.MAX_Q), S.t(K.MAX_Q_HELP), Prefs.qualities.map { Prefs.qualityLabel(it) }, Prefs.qualities.indexOf(Prefs.quality).coerceAtLeast(0)) { Prefs.quality = Prefs.qualities[it] }
            },
            open(K.SPEED, K.SPEED_HELP, { "%.2fx".format(java.util.Locale.ROOT, Prefs.speed) }) {
                sub.slider(S.t(K.SPEED), S.t(K.SPEED_HELP), 0.25f, 3f, 0.25f, { Prefs.speed }, { Prefs.speed = it }) { "%.2fx".format(java.util.Locale.ROOT, it) }
            },
            toggle(K.AUTOPLAY, K.AUTOPLAY_HELP, { Prefs.autoplay }) { Prefs.autoplay = it },
            toggle(K.SUBS_DEFAULT, K.SUBS_HELP, { Prefs.subs }) { Prefs.subs = it },
            HeaderRow { S.t(K.S_GENERAL) },
            open(K.LANGUAGE, K.LANGUAGE_HELP, { if (Prefs.language == "auto") S.t(K.AUTO) + " · " + S.cur.name else S.cur.name }) {
                val cur = if (Prefs.language == "auto") 0 else S.all.indexOfFirst { it.code == Prefs.language } + 1
                sub.choose(S.t(K.LANGUAGE), S.t(K.LANGUAGE_HELP), listOf(S.t(K.AUTO)) + S.all.map { it.name }, cur) {
                    Prefs.language = if (it == 0) "auto" else S.all[it - 1].code; S.apply()
                    if (!S.cur.rtl && Prefs.rtl) { Prefs.rtl = false; S.apply() }
                    onLang()
                    if (S.cur.rtl && !Prefs.rtl) Bg.main.post { direction(sub, onLang, ask = true) } // offered, never forced
                }
            },
            open(K.LAYOUT, K.LAYOUT_HELP, { S.t(if (Prefs.rtl) K.RTL else K.LTR) }) { direction(sub, onLang, ask = false) },
            open(K.CONTENT_LANG, K.CONTENT_HELP, { Regions.saved()?.name ?: S.t(K.SAME_AS_APP) }) {
                val cur = Regions.all.indexOf(Regions.saved()) + 1
                sub.choose(S.t(K.CONTENT_LANG), S.t(K.CONTENT_HELP), listOf(S.t(K.SAME_AS_APP)) + Regions.all.map { it.name }, cur) {
                    Prefs.content = if (it == 0) "" else Regions.all[it - 1].let { r -> "${r.lang}-${r.country}" }; Yt.relocalize(); onLang()
                }
            },
            open(K.KB_LAYOUTS, K.KB_HELP, { Layouts.enabled().joinToString(" · ") { it.code.uppercase() } }) {
                sub.multi(S.t(K.KB_LAYOUTS), S.t(K.KB_HELP), Layouts.all.map { it.name }, { i -> Layouts.enabled().any { it.code == Layouts.all[i].code } }) { i ->
                    val codes = Layouts.enabled().map { it.code }.toMutableList(); val c = Layouts.all[i].code
                    if (c in codes) { if (codes.size > 1) codes.remove(c) } else codes.add(c)
                    Prefs.kbLangs = codes; onKb()
                }
            },
            toggle(K.AMBIENT, K.AMBIENT_HELP, { Prefs.ambient }) { Prefs.ambient = it },
            HeaderRow { S.t(K.S_DATA) },
            ActionRow({ S.t(K.CLEAR_HISTORY) }, { S.t(K.CLEAR_HELP) }, { "OK" }) { Store.clearHistory(); toast(S.t(K.NOTHING_WATCHED)) },
            ActionRow({ S.t(K.UPDATES) }, { S.t(K.UPDATES_HELP) }, { Updater.status }) { Updater.check(true) },
            ActionRow({ S.t(K.ABOUT_APP) }, { S.t(K.ABOUT_HELP) }, { "PipeTv ${Updater.current} · LimitedIO" }, onAbout),
        )
    }

    /** Asks for (or lets the user change) the layout direction. */
    private fun direction(sub: SubMenu, onLang: () -> Unit, ask: Boolean) {
        val title = S.t(if (ask) K.RTL_ASK else K.LAYOUT)
        val opts = if (ask) listOf(S.t(K.KEEP_LTR), S.t(K.USE_RTL)) else listOf(S.t(K.LTR), S.t(K.RTL))
        sub.choose(title, S.t(K.LAYOUT_HELP), opts, if (Prefs.rtl) 1 else 0) { Prefs.rtl = it == 1; S.apply(); onLang() }
    }
}
