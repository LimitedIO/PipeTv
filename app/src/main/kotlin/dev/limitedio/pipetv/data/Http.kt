package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.Config
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** HttpURLConnection backed downloader for NewPipeExtractor. Falls back to [Fallback] when the normal route fails. */
object Http : Downloader() {
    override fun execute(request: Request): Response = try { once(request) } catch (e: IOException) {
        try { Thread.sleep(500); once(request) } catch (e2: IOException) { viaFallback(request) }
    }

    private fun setup(request: Request): (HttpURLConnection) -> Unit = { c ->
        c.requestMethod = request.httpMethod(); c.connectTimeout = 15_000; c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", Config.UA)
        request.headers().forEach { (k, v) -> c.setRequestProperty(k, v.joinToString(", ")) }
        request.dataToSend()?.let { body -> c.doOutput = true; c.outputStream.use { it.write(body) } }
    }

    private fun once(request: Request): Response {
        val c = URL(request.url()).openConnection() as HttpURLConnection
        setup(request)(c); return read(request, c, c.url.toString())
    }

    private fun viaFallback(request: Request): Response {
        var out: Response? = null
        Fallback.open(URL(request.url()), setup(request)) { out = read(request, it, request.url()) }
        return out!!
    }

    private fun read(request: Request, c: HttpURLConnection, finalUrl: String): Response {
        val code = c.responseCode
        if (code == 429) throw ReCaptchaException("Rate limited", request.url())
        val text = (if (code >= 400) c.errorStream else c.inputStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
        return Response(code, c.responseMessage ?: "", c.headerFields.filterKeys { it != null }, text, finalUrl)
    }
}
