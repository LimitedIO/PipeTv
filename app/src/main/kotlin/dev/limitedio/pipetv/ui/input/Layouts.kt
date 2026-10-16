package dev.limitedio.pipetv.ui.input

import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.i18n.S

class Layout(val code: String, val name: String, val rows: List<String>, val cased: Boolean = true)

/** On-screen keyboard layouts. Add a language by adding one line here. */
object Layouts {
    val all = listOf(
        Layout("en", "English", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")),
        Layout("ar", "العربية", listOf("ضصثقفغعهخحجد", "شسيبلاتنمكط", "ئءؤرىةوزظذ"), false),
        Layout("es", "Español", listOf("qwertyuiop", "asdfghjklñ", "zxcvbnmáéíóú")),
        Layout("fr", "Français", listOf("azertyuiop", "qsdfghjklm", "wxcvbnéèàùç")),
        Layout("de", "Deutsch", listOf("qwertzuiopü", "asdfghjklöä", "yxcvbnmß")),
        Layout("ru", "Русский", listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбюё")),
        Layout("tr", "Türkçe", listOf("qwertyuıopğü", "asdfghjklşi", "zxcvbnmöç")),
        Layout("pt", "Português", listOf("qwertyuiop", "asdfghjklç", "zxcvbnmáéãõ")),
        Layout("it", "Italiano", listOf("qwertyuiop", "asdfghjkl", "zxcvbnmèéàòù")),
        Layout("pl", "Polski", listOf("qwertyuiopęó", "asdfghjkląłś", "zxcvbnmćńźż")),
        Layout("uk", "Українська", listOf("йцукенгшщзхї", "фівапролджє", "ячсмитьбюґ")),
        Layout("fa", "فارسی", listOf("ضصثقفغعهخحجچ", "شسیبلاتنمکگ", "ظطزرذدپوئ"), false),
        Layout("he", "עברית", listOf("קראטוןםפ", "שדגכעיחלךף", "זסבהנמצתץ"), false),
        Layout("hi", "हिन्दी", listOf("अआइईउऊएऐओऔ", "कखगघचछजझटठड", "ढणतथदधनपफबभ", "मयरलवशषसह", "ािीुूेैोौंँ्"), false),
    )
    val symbols = listOf("@#$%&-+()/", "*\"':;!?,._", "=<>[]{}~^|")

    /** Layouts the user switched on; defaults to English plus the app language. */
    fun enabled(): List<Layout> {
        val saved = Prefs.kbLangs.mapNotNull { c -> all.firstOrNull { it.code == c } }
        if (saved.isNotEmpty()) return saved
        return listOfNotNull(all.firstOrNull { it.code == S.cur.code }, all[0]).distinct()
    }
}
