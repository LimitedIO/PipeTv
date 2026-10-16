package dev.limitedio.pipetv.i18n

import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.i18n.lang.*
import dev.limitedio.pipetv.ui.Ui
import java.util.Locale

/** Runtime translations. Pure Kotlin maps: no resource XML, instant lookups. */
object S {
    class Lang(val code: String, val name: String, val map: Map<K, String>, val rtl: Boolean = false)

    val all = listOf(
        Lang("en", "English", En), Lang("ar", "العربية", Ar, true), Lang("es", "Español", Es),
        Lang("fr", "Français", Fr), Lang("de", "Deutsch", De), Lang("ru", "Русский", Ru),
        Lang("tr", "Türkçe", Tr), Lang("pt", "Português", Pt), Lang("hi", "हिन्दी", Hi), Lang("zh", "中文", Zh),
    )
    var cur: Lang = all[0]; private set

    /** Resolves the saved language ("auto" follows the system); the layout is mirrored only if the user chose RTL. */
    fun apply() {
        val pick = Prefs.language
        cur = all.firstOrNull { it.code == pick } ?: all.firstOrNull { it.code == Locale.getDefault().language } ?: all[0]
        Ui.rtl = Prefs.rtl
    }

    /** Separator between facts ("views · time ago"); in RTL languages it is wrapped in direction marks so the pieces never mix. */
    val sep get() = if (cur.rtl) " \u200F\u2022\u200F " else " · "

    fun t(k: K): String = cur.map[k] ?: En[k] ?: k.name
    fun f(k: K, vararg a: Any): String = String.format(Locale.ROOT, t(k), *a)
}
