package dev.limitedio.pipetv.data

/** Content regions: which language and country YouTube should use for recommendations, trending and search. */
class Region(val lang: String, val country: String, val name: String)

object Regions {
    val all = listOf(
        Region("en", "US", "English · United States"), Region("en", "GB", "English · United Kingdom"),
        Region("ar", "SA", "العربية · السعودية"), Region("ar", "EG", "العربية · مصر"), Region("ar", "AE", "العربية · الإمارات"),
        Region("es", "ES", "Español · España"), Region("es", "MX", "Español · México"),
        Region("fr", "FR", "Français · France"), Region("de", "DE", "Deutsch · Deutschland"),
        Region("ru", "RU", "Русский · Россия"), Region("uk", "UA", "Українська · Україна"),
        Region("tr", "TR", "Türkçe · Türkiye"), Region("pt", "BR", "Português · Brasil"),
        Region("it", "IT", "Italiano · Italia"), Region("pl", "PL", "Polski · Polska"),
        Region("hi", "IN", "हिन्दी · भारत"), Region("zh", "TW", "中文 · 台灣"), Region("ja", "JP", "日本語 · 日本"),
        Region("ko", "KR", "한국어 · 대한민국"), Region("id", "ID", "Indonesia · Indonesia"),
        Region("fa", "IR", "فارسی · ایران"), Region("he", "IL", "עברית · ישראל"), Region("nl", "NL", "Nederlands · Nederland"),
    )

    /** The saved region, or null to follow the app language. */
    fun saved(): Region? = all.firstOrNull { "${it.lang}-${it.country}" == Prefs.content }
}
