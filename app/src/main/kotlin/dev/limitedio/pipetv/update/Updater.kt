package dev.limitedio.pipetv.update

import dev.limitedio.pipetv.App
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.Config
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Self-update from GitHub Releases (FOSS, no store): check, download in background, install on request. */
object Updater {
    @Volatile var status = ""
    @Volatile var ready: File? = null
    var onChange: () -> Unit = {}
    var onReady: (String) -> Unit = {}

    val current: String get() = App.ctx.packageManager.getPackageInfo(App.ctx.packageName, 0).versionName ?: "0"

    private fun say(s: String) { status = s; Bg.main.post { onChange() } }
    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000; readTimeout = 30_000; setRequestProperty("Accept", "application/vnd.github+json")
    }

    fun check() = Bg.io { runCatching { run() }.onFailure { say(S.t(K.UPD_FAIL)) } }

    private fun run() {
        say(S.t(K.UPD_CHECKING))
        val rel = JSONObject(open("https://api.github.com/repos/${Config.REPO}/releases/latest").inputStream.use { it.readBytes().decodeToString() })
        val tag = rel.getString("tag_name")
        val assets = rel.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(".apk") }
        if (apk == null || !newer(tag, current)) return say(S.f(K.UPD_OK, current))
        download(tag, apk.getString("browser_download_url"))
    }

    private fun download(tag: String, url: String) {
        val file = File(App.ctx.cacheDir, "update-$tag.apk")
        if (!file.exists()) {
            val c = open(url); val total = c.contentLengthLong.coerceAtLeast(1)
            val tmp = File(file.path + ".part"); var done = 0L; var last = -1
            c.inputStream.use { i -> tmp.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = i.read(buf); if (n < 0) break
                    o.write(buf, 0, n); done += n
                    val pct = (done * 100 / total).toInt(); if (pct != last) { last = pct; say(S.f(K.UPD_DL, tag, pct)) }
                }
            } }
            tmp.renameTo(file)
        }
        ready = file
        say(S.f(K.UPD_READY, tag))
        Bg.main.post { onReady(tag) }
    }

    fun install() { ready?.let { Installer.install(it) } ?: check() }

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
