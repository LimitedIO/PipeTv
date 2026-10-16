package dev.limitedio.pipetv.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Shader
import android.os.SystemClock
import android.util.LruCache
import dev.limitedio.pipetv.App
import dev.limitedio.pipetv.Bg
import java.io.File
import java.net.URL

class Pic(val bmp: Bitmap) {
    val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val born = SystemClock.uptimeMillis()
}

/** Memory + disk thumbnail loader. Decodes at card size so weak TVs stay within budget. */
object Img {
    private val cache = object : LruCache<String, Pic>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, v: Pic) = v.bmp.byteCount
    }
    private val pending = HashSet<String>()
    private val listeners = ArrayList<() -> Unit>()
    private val dir by lazy { File(App.ctx.cacheDir, "img").apply { mkdirs() } }

    fun listen(l: () -> Unit) { listeners.add(l) }

    /** The cached image if it is already loaded (never starts a download). */
    fun peek(url: String): Pic? = cache.get(url)

    /** Returns the image if ready, otherwise starts loading and returns null. */
    fun get(url: String, w: Int): Pic? {
        cache.get(url)?.let { return it }
        if (pending.add(url)) Bg.imgPool.execute {
            val bmp = runCatching { load(url, w) }.getOrNull()
            Bg.main.post {
                pending.remove(url)
                if (bmp != null) { cache.put(url, Pic(bmp)); listeners.forEach { it() } }
            }
        }
        return null
    }

    private fun load(url: String, w: Int): Bitmap? {
        val f = File(dir, url.hashCode().toString())
        if (!f.exists()) {
            val tmp = File(dir, "${f.name}.tmp")
            val save = { i: java.io.InputStream -> i.use { s -> tmp.outputStream().use { o -> s.copyTo(o) } } }
            try { save(URL(url).openStream()) } catch (e: java.io.IOException) { dev.limitedio.pipetv.data.Fallback.open(URL(url), {}) { c -> save(c.inputStream) } }
            tmp.renameTo(f)
        }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= w) s *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
    }
}
