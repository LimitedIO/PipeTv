package dev.limitedio.pipetv

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/** Tiny threading helpers: background work, results delivered on the main thread. */
object Bg {
    val main = Handler(Looper.getMainLooper())
    private val pool = Executors.newFixedThreadPool(4)
    private val parPool = Executors.newCachedThreadPool()
    val imgPool = Executors.newFixedThreadPool(3)

    /** Short description of the latest background failure, shown next to "no connection" so problems can be diagnosed. */
    @Volatile var lastError = ""
    private fun <T> safe(work: () -> T): T? = try { work() } catch (e: Throwable) {
        lastError = "${e.javaClass.simpleName}: ${(e.cause ?: e).message.orEmpty().take(70)}"; null
    }

    fun io(work: () -> Unit) { pool.execute { safe(work) } }

    /** Runs [work] off-thread; [done] gets the result, or null on failure. */
    fun <T : Any> io(work: () -> T?, done: (T?) -> Unit) {
        pool.execute {
            val r = safe(work)
            main.post { done(r) }
        }
    }

    /** Runs tasks in parallel and returns their results (null where a task failed). */
    fun <T : Any> par(tasks: List<() -> T?>): List<T?> =
        parPool.invokeAll(tasks.map { t -> Callable { safe(t) } }).map { it.get() }
}
