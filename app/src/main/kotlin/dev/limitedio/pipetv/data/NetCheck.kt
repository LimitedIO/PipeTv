package dev.limitedio.pipetv.data

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/** Explains in one short line why loading failed: DNS, TLS/HTTPS or the extractor, so a broken TV network is easy to spot. */
object NetCheck {
    fun probe(): String {
        val dns = try { "dns ok (${InetAddress.getAllByName("www.youtube.com").size})" }
            catch (e: Throwable) { return "dns failed: ${e.javaClass.simpleName}" }
        return try {
            val c = URL("https://www.youtube.com/generate_204").openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 8000
            val code = c.responseCode; c.disconnect()
            "$dns · https $code"
        } catch (e: Throwable) { "$dns · https failed: ${e.javaClass.simpleName} ${(e.cause ?: e).message.orEmpty().take(60)}" }
    }
}
