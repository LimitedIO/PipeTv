package dev.limitedio.pipetv.update

import dev.limitedio.pipetv.App
import dev.limitedio.pipetv.BuildConfig
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.Config
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Downloads new releases automatically; installation remains an explicit action. */
object Updater {
    enum class St { IDLE, CHECKING, FOUND, DOWNLOADING, READY, FAILED }
    @Volatile var st = St.IDLE; private set
    @Volatile var tag = ""; private set
    @Volatile var size = 0L; private set
    @Volatile var progress = 0f; private set
    /** The user said "Later" for this version: no dialog until the next manual check. */
    @Volatile var snoozed = false
    var onChange: () -> Unit = {}
    private var url = ""
    private var file: File? = null

    val current: String get() = App.ctx.packageManager.getPackageInfo(App.ctx.packageName, 0).versionName ?: "0"
    val status get() = when (st) {
        St.CHECKING -> S.t(K.UPD_CHECKING); St.FOUND -> S.f(K.UPD_FOUND, tag); St.DOWNLOADING -> S.f(K.UPD_DL, tag, (progress * 100).toInt())
        St.READY -> S.f(K.UPD_READY, tag); St.FAILED -> S.t(K.UPD_FAIL); St.IDLE -> S.f(K.UPD_OK, current)
    }

    private fun set(s: St) { st = s; Bg.main.post { onChange() } }
    private fun open(u: String) = (URL(u).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000; readTimeout = 30_000; setRequestProperty("Accept", "application/vnd.github+json")
    }

    /** Looks for a newer release and downloads it in the background. */
    fun check(manual: Boolean = false) {
        if (!BuildConfig.UPDATER) return
        if (st == St.CHECKING || st == St.DOWNLOADING) return
        if (manual) snoozed = false
        if (st == St.FOUND) return download()
        if (st == St.READY) return set(st)
        set(St.CHECKING)
        Bg.io { runCatching { look() }.onFailure { set(St.FAILED) } }
    }

    private fun look() {
        val rel = JSONObject(open("https://api.github.com/repos/${Config.REPO}/releases/latest").inputStream.use { it.readBytes().decodeToString() })
        val assets = rel.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(".apk") }
        tag = rel.getString("tag_name")
        if (apk == null || !newer(tag, current)) return set(St.IDLE)
        url = apk.getString("browser_download_url"); size = apk.optLong("size")
        file = File(App.ctx.cacheDir, "update-$tag.apk")
        val cached = file!!.takeIf { it.exists() && it.length() > 0 && (size <= 0 || it.length() == size) }
        if (cached != null) set(St.READY) else { set(St.FOUND); download() }
    }

    /** Starts the download (progress is published as it goes). */
    fun download() {
        if (st != St.FOUND && st != St.FAILED) return
        if (url.isBlank() || file == null) return check()
        progress = 0f; set(St.DOWNLOADING)
        Bg.io { runCatching { fetch() }.onFailure { set(St.FAILED) } }
    }

    private fun fetch() {
        val out = file ?: return; val c = open(url); val total = c.contentLengthLong.takeIf { it > 0 } ?: size.coerceAtLeast(1)
        val tmp = File(out.path + ".part"); var done = 0L; var last = -1
        c.inputStream.use { i -> tmp.outputStream().use { o ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = i.read(buf); if (n < 0) break
                o.write(buf, 0, n); done += n
                progress = (done.toFloat() / total).coerceIn(0f, 1f); val pct = (progress * 100).toInt()
                if (pct != last) { last = pct; Bg.main.post { onChange() } }
            }
        } }
        kotlin.check(done > 0 && (size <= 0 || done == size)) { "Incomplete update download" }
        kotlin.check(tmp.renameTo(out)) { "Could not save update" }
        snoozed = false; set(St.READY)
    }

    fun install() { file?.takeIf { st == St.READY }?.let { Installer.install(it) } }

    private fun newer(tag: String, cur: String): Boolean {
        val a = Regex("\\d+").findAll(tag).map { it.value.toInt() }.toList()
        val b = Regex("\\d+").findAll(cur).map { it.value.toInt() }.toList()
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
