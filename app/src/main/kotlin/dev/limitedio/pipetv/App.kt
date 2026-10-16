package dev.limitedio.pipetv

import android.app.Application
import java.io.File

/** Does nothing heavy on purpose: launch must be instant. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ctx = this
        Bg.io { prune(File(cacheDir, "img")) }
    }

    /** Keeps the thumbnail cache under ~100 MB. */
    private fun prune(dir: File) {
        val files = dir.listFiles() ?: return
        if (files.sumOf { it.length() } > 100_000_000L) files.forEach { it.delete() }
    }

    companion object { lateinit var ctx: App }
}
