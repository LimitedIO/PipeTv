package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.i18n.S
import org.schabi.newpipe.extractor.localization.DateWrapper
import java.time.Duration
import java.time.OffsetDateTime

/**
 * "2 years ago" in the app language with correct plurals (Arabic dual/few/many, Russian few/many...).
 * Each unit lists its forms as one | two | few | other; a form without %d is a complete word ("ساعتين").
 */
object Ago {
    private class L(val pattern: String, val units: List<String>)
    private val langs = mapOf(
        "en" to L("%s ago", listOf("%d second|||%d seconds", "%d minute|||%d minutes", "%d hour|||%d hours", "%d day|||%d days", "%d week|||%d weeks", "%d month|||%d months", "%d year|||%d years")),
        "ar" to L("منذ %s", listOf("ثانية|ثانيتين|%d ثوانٍ|%d ثانية", "دقيقة|دقيقتين|%d دقائق|%d دقيقة", "ساعة|ساعتين|%d ساعات|%d ساعة", "يوم|يومين|%d أيام|%d يومًا",
            "أسبوع|أسبوعين|%d أسابيع|%d أسبوعًا", "شهر|شهرين|%d أشهر|%d شهرًا", "سنة|سنتين|%d سنوات|%d سنة")),
        "es" to L("hace %s", listOf("%d segundo|||%d segundos", "%d minuto|||%d minutos", "%d hora|||%d horas", "%d día|||%d días", "%d semana|||%d semanas", "%d mes|||%d meses", "%d año|||%d años")),
        "fr" to L("il y a %s", listOf("%d seconde|||%d secondes", "%d minute|||%d minutes", "%d heure|||%d heures", "%d jour|||%d jours", "%d semaine|||%d semaines", "%d mois|||%d mois", "%d an|||%d ans")),
        "de" to L("vor %s", listOf("%d Sekunde|||%d Sekunden", "%d Minute|||%d Minuten", "%d Stunde|||%d Stunden", "%d Tag|||%d Tagen", "%d Woche|||%d Wochen", "%d Monat|||%d Monaten", "%d Jahr|||%d Jahren")),
        "ru" to L("%s назад", listOf("%d секунду||%d секунды|%d секунд", "%d минуту||%d минуты|%d минут", "%d час||%d часа|%d часов", "%d день||%d дня|%d дней", "%d неделю||%d недели|%d недель", "%d месяц||%d месяца|%d месяцев", "%d год||%d года|%d лет")),
        "tr" to L("%s önce", listOf("%d saniye|||%d saniye", "%d dakika|||%d dakika", "%d saat|||%d saat", "%d gün|||%d gün", "%d hafta|||%d hafta", "%d ay|||%d ay", "%d yıl|||%d yıl")),
        "pt" to L("há %s", listOf("%d segundo|||%d segundos", "%d minuto|||%d minutos", "%d hora|||%d horas", "%d dia|||%d dias", "%d semana|||%d semanas", "%d mês|||%d meses", "%d ano|||%d anos")),
        "hi" to L("%s पहले", listOf("%d सेकंड|||%d सेकंड", "%d मिनट|||%d मिनट", "%d घंटे|||%d घंटे", "%d दिन|||%d दिन", "%d सप्ताह|||%d सप्ताह", "%d महीने|||%d महीने", "%d साल|||%d साल")),
        "zh" to L("%s前", listOf("%d秒|||%d秒", "%d分钟|||%d分钟", "%d小时|||%d小时", "%d天|||%d天", "%d周|||%d周", "%d个月|||%d个月", "%d年|||%d年")),
    )
    private val secs = longArrayOf(1, 60, 3600, 86400, 604800, 2592000, 31536000)

    /** The upload time as text; falls back to YouTube's own words when no exact date is known. */
    fun of(d: DateWrapper?, fallback: String?): String {
        val t = d?.offsetDateTime() ?: return fallback.orEmpty()
        val s = Duration.between(t, OffsetDateTime.now()).seconds.coerceAtLeast(0)
        if (s < 45) return when (S.cur.code) { "ar" -> "الآن"; "es" -> "ahora"; "fr" -> "à l'instant"; "de" -> "gerade eben"; "ru" -> "только что"; "tr" -> "şimdi"; "pt" -> "agora"; "hi" -> "अभी"; "zh" -> "刚刚"; else -> "just now" }
        val u = secs.indexOfLast { s >= it }.coerceAtLeast(1); val n = (s / secs[u]).toInt()
        val l = langs[S.cur.code] ?: langs["en"]!!
        val f = l.units[u].split("|"); val m10 = n % 10; val m100 = n % 100
        val form = when (S.cur.code) {
            "ar" -> when { n == 1 -> f[0]; n == 2 -> f[1]; n in 3..10 -> f[2]; else -> f[3] }
            "ru" -> when { m10 == 1 && m100 != 11 -> f[0]; m10 in 2..4 && m100 !in 12..14 -> f[2]; else -> f[3] }
            else -> if (n == 1) f[0] else f[3]
        }
        return l.pattern.format(form.replace("%d", n.toString()))
    }
}
