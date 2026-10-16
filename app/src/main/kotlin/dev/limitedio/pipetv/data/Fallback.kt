package dev.limitedio.pipetv.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * Last resort when the TV's own DNS or IPv6 is broken: look the host up over HTTPS (Google's resolver, by IP) and
 * connect straight to the IPv4 address while still sending the real host name for SNI, Host and certificate checks.
 */
object Fallback {
    private val cache = HashMap<String, List<String>>()

    private fun ips(host: String): List<String> = synchronized(cache) { cache[host] } ?: run {
        val c = URL("https://8.8.8.8/resolve?name=$host&type=A").openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        val a = JSONObject(c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }).getJSONArray("Answer")
        (0 until a.length()).map { a.getJSONObject(it) }.filter { it.getInt("type") == 1 }.map { it.getString("data") }
            .also { synchronized(cache) { cache[host] = it } }
    }

    /** Opens [url] through each resolved address in turn; returns the first connection that answers. */
    fun open(url: URL, setup: (HttpURLConnection) -> Unit, use: (HttpURLConnection) -> Unit) {
        val host = url.host; var last: Throwable? = null
        val base = HttpsURLConnection.getDefaultSSLSocketFactory(); val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
        for (ip in ips(host).ifEmpty { throw java.net.UnknownHostException(host) }) {
            try {
                val c = URL(url.protocol, ip, url.port, url.file).openConnection() as HttpURLConnection
                (c as? HttpsURLConnection)?.let { s -> s.sslSocketFactory = Sni(base, host); s.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, ses -> verifier.verify(host, ses) } }
                c.setRequestProperty("Host", host); setup(c); use(c); return
            } catch (e: java.io.IOException) { last = e }
        }
        throw last ?: java.net.UnknownHostException(host)
    }

    private class Sni(val b: SSLSocketFactory, val host: String) : SSLSocketFactory() {
        override fun getDefaultCipherSuites(): Array<String> = b.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = b.supportedCipherSuites
        override fun createSocket(s: Socket, h: String, p: Int, a: Boolean): Socket = b.createSocket(s, host, p, a)
        override fun createSocket(h: String, p: Int): Socket = b.createSocket(h, p)
        override fun createSocket(h: String, p: Int, la: InetAddress, lp: Int): Socket = b.createSocket(h, p, la, lp)
        override fun createSocket(a: InetAddress, p: Int): Socket = b.createSocket(a, p)
        override fun createSocket(a: InetAddress, p: Int, la: InetAddress, lp: Int): Socket = b.createSocket(a, p, la, lp)
    }
}
