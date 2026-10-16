package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.detail.Block
import dev.limitedio.pipetv.ui.detail.TextList
import dev.limitedio.pipetv.update.Updater
import android.view.ViewGroup

/** App details, an offline repository QR, and readable bundled license texts. */
class AboutPage(c: Context) : ViewGroup(c), Screen {
    private data class Notice(val name: String, val license: String, val source: String, val asset: String)
    private val notices = listOf(
        Notice("PipeTv · LimitedIO", "GPL-3.0", "https://github.com/LimitedIO/PipeTv", "GPL-3.0"),
        Notice("NewPipeExtractor · Team NewPipe", "GPL-3.0-or-later", "https://github.com/TeamNewPipe/NewPipeExtractor", "GPL-3.0"),
        Notice("AndroidX Media3 · Android Open Source Project", "Apache-2.0", "https://github.com/androidx/media", "Apache-2.0"),
        Notice("Kotlin · JetBrains", "Apache-2.0", "https://github.com/JetBrains/kotlin", "Apache-2.0"),
        Notice("desugar_jdk_libs · Google / OpenJDK", "GPL-2.0 with Classpath Exception", "https://github.com/google/desugar_jdk_libs", "GPL-2.0-classpath"),
        Notice("jsoup · Jonathan Hedley", "MIT", "https://jsoup.org", "jsoup"),
        Notice("nanojson · The nanojson Authors / Team NewPipe", "Apache-2.0", "https://github.com/TeamNewPipe/nanojson", "Apache-2.0"),
        Notice("Rhino · Mozilla", "MPL-2.0", "https://github.com/mozilla/rhino", "MPL-2.0"),
        Notice("Protocol Buffers · Google", "BSD-3-Clause", "https://github.com/protocolbuffers/protobuf", "protobuf"),
        Notice("Guava · Google", "Apache-2.0", "https://github.com/google/guava", "Apache-2.0"),
        Notice("JSR-305 · FindBugs / JSR-305 contributors", "Apache-2.0", "https://github.com/findbugsproject/findbugs", "Apache-2.0"),
    )
    private val reader = TextList(c)
    private val qr = RepoQr(c)
    private var reading = false; private var selected = 0; private var pageTitle = S.t(K.ABOUT_APP)
    private val header = object : AnimView(c) {
        private val title = Ui.paint(Ui.TEXT, 44f, true)
        override fun render(c: Canvas, dt: Float) { Txt.draw(c, Txt.fit(title, pageTitle, width - Ui.u(112)), Ui.u(56), Ui.u(84), width - Ui.u(112), title) }
    }

    init {
        addView(header); addView(reader); addView(qr)
        reader.hint = S.t(K.LICENSE_HINT); showDetails()
        reader.onSelect = { i ->
            if (!reading && i > 0) {
                val notice = notices[i - 1]
                val text = c.assets.open("licenses/${notice.asset}.txt").bufferedReader().use { it.readText() }
                selected = i; reading = true; pageTitle = notice.license
                reader.blocks = listOf(Block(notice.name, notice.license, text, max = 3000))
                reader.reset(); header.invalidate()
            }
        }
    }

    private fun showDetails() {
        val details = S.t(K.APP_DESCRIPTION) + "\n\n" + S.t(K.DEVELOPER) + ": LimitedIO\n" +
            S.t(K.REPOSITORY) + ": https://github.com/LimitedIO/PipeTv\n\n" + S.t(K.LICENSES)
        reader.blocks = listOf(Block("PipeTv", Updater.current, details)) + notices.map {
            Block(it.name, it.license, it.source, more = true)
        }
        pageTitle = S.t(K.ABOUT_APP); header.invalidate()
    }

    override fun back(): Boolean {
        if (!reading) return false
        reading = false; showDetails(); reader.reset(); reader.focusAt(selected); return true
    }
    override fun key(code: Int, repeat: Int) = reader.key(code)
    override fun lit(on: Boolean) { reader.lit = on; reader.invalidate() }
    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l; val h = b - t; val top = Ui.u(130).toInt(); val qw = minOf(Ui.u(400).toInt(), (w * .35f).toInt())
        val rx = if (Ui.rtl) qw else 0; val qx = if (Ui.rtl) 0 else w - qw
        header.layout(0, 0, w, top); reader.layout(rx, top, rx + w - qw, h); qr.layout(qx, top, qx + qw, h)
    }
}
