package dev.limitedio.pipetv.ui

import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.input.Layouts
import dev.limitedio.pipetv.ui.widgets.*
import dev.limitedio.pipetv.update.Updater

/** Settings page content: sliders, steppers, switches and actions, grouped by topic. */
object Settings {
    /** [onLang] runs after the language changed, [onKb] after the keyboard layouts changed. */
    fun rows(onLang: () -> Unit, onKb: () -> Unit, toast: (String) -> Unit): List<Row> = listOf(
        HeaderRow { S.t(K.S_PLAYBACK) },
        StepRow({ S.t(K.MAX_Q) }, { S.t(K.MAX_Q_HELP) }, { Prefs.qualities.map { Prefs.qualityLabel(it) } },
            { Prefs.qualities.indexOf(Prefs.quality).coerceAtLeast(0) }, { Prefs.quality = Prefs.qualities[it] }),
        SliderRow({ S.t(K.SPEED) }, { S.t(K.SPEED_HELP) }, 0.25f, 3f, 0.25f, { Prefs.speed }, { Prefs.speed = it }, { "%.2fx".format(it) }),
        ToggleRow({ S.t(K.AUTOPLAY) }, { S.t(K.AUTOPLAY_HELP) }, { Prefs.autoplay }, { Prefs.autoplay = it }),
        ToggleRow({ S.t(K.SUBS_DEFAULT) }, { S.t(K.SUBS_HELP) }, { Prefs.subs }, { Prefs.subs = it }),
        HeaderRow { S.t(K.S_GENERAL) },
        ChoiceRow({ S.t(K.LANGUAGE) }, { S.t(K.LANGUAGE_HELP) }, { listOf(S.t(K.AUTO)) + S.all.map { it.name } },
            { if (Prefs.language == "auto") 0 else S.all.indexOfFirst { it.code == Prefs.language } + 1 },
            { Prefs.language = if (it == 0) "auto" else S.all[it - 1].code; S.apply(); onLang() }),
        MultiRow({ S.t(K.KB_LAYOUTS) }, { S.t(K.KB_HELP) }, Layouts.all.map { it.name },
            { i -> Layouts.enabled().any { it.code == Layouts.all[i].code } },
            { i ->
                val codes = Layouts.enabled().map { it.code }.toMutableList(); val c = Layouts.all[i].code
                if (c in codes) { if (codes.size > 1) codes.remove(c) } else codes.add(c)
                Prefs.kbLangs = codes; onKb()
            }),
        HeaderRow { S.t(K.S_DATA) },
        ActionRow({ S.t(K.CLEAR_HISTORY) }, { S.t(K.CLEAR_HELP) }, { "OK" }) { Store.clearHistory(); toast(S.t(K.NOTHING_WATCHED)) },
        ActionRow({ S.t(K.UPDATES) }, { S.t(K.UPDATES_HELP) }, { Updater.status }) { if (Updater.ready != null) Updater.install() else Updater.check() },
        ActionRow({ S.t(K.ABOUT_APP) }, { S.t(K.ABOUT_HELP) }, { "PipeTv ${Updater.current} · LimitedIO" }) {},
    )
}
