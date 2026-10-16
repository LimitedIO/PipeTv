package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.Config
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.net.HttpURLConnection
import java.net.URL

/** HttpURLConnection backed downloader for NewPipeExtractor (no extra deps). */
object Http : Downloader() {
    /** One retry: a TV that has just woken up often fails the very first request. */
    override fun execute(request: Request): Response = try { once(request) } catch (e: java.io.IOException) { Thread.sleep(600); once(request) }

    private fun once(request: Request): Response {
        val c = URL(request.url()).openConnection() as HttpURLConnection
        c.requestMethod = request.httpMethod()
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", Config.UA)
        request.headers().forEach { (k, v) -> c.setRequestProperty(k, v.joinToString(", ")) }
        request.dataToSend()?.let { body ->
            c.doOutput = true
            c.outputStream.use { it.write(body) }
        }
        val code = c.responseCode
        if (code == 429) throw ReCaptchaException("Rate limited", request.url())
        val text = (if (code >= 400) c.errorStream else c.inputStream)
            ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
        val headers = c.headerFields.filterKeys { it != null }
        return Response(code, c.responseMessage ?: "", headers, text, c.url.toString())
    }
}
