package dev.limitedio.pipetv.ui.timeline

import dev.limitedio.pipetv.data.Entry
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.Ui
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One line in the timeline: a day heading or a watched video. */
sealed class Line {
    class Day(val label: String, val summary: String) : Line()
    class Watch(val e: Entry) : Line()
}

/** Groups history entries by calendar day (local time) with friendly headings. */
object Days {
    private fun day(t: Long) = (t + TimeZone.getDefault().getOffset(t)) / 86_400_000L

    fun time(t: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(S.cur.code + "-u-nu-latn")).format(Date(t))

    private fun label(t: Long): String {
        val d = day(t) ; val now = day(System.currentTimeMillis())
        return when (d) {
            now -> S.t(K.TODAY)
            now - 1 -> S.t(K.YESTERDAY)
            else -> DateFormat.getDateInstance(DateFormat.FULL, Locale.forLanguageTag(S.cur.code + "-u-nu-latn")).format(Date(t))
        }
    }

    fun group(list: List<Entry>): List<Line> {
        val out = ArrayList<Line>()
        for ((_, es) in list.groupBy { day(it.at) }.entries.sortedByDescending { it.key }) {
            val seen = es.sumOf { it.pos }
            out.add(Line.Day(label(es[0].at), S.f(K.VIDEOS_N, es.size) + " · " + S.f(K.WATCHED_FOR, Ui.fmt(seen))))
            es.sortedByDescending { it.at }.forEach { out.add(Line.Watch(it)) }
        }
        return out
    }
}
