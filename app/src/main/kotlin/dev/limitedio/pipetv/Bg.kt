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

    fun io(work: () -> Unit) { pool.execute { runCatching(work) } }

    /** Runs [work] off-thread; [done] gets the result, or null on failure. */
    fun <T : Any> io(work: () -> T?, done: (T?) -> Unit) {
        pool.execute {
            val r = runCatching(work).getOrNull()
            main.post { done(r) }
        }
    }

    /** Runs tasks in parallel and returns their results (null where a task failed). */
    fun <T : Any> par(tasks: List<() -> T?>): List<T?> =
        parPool.invokeAll(tasks.map { t -> Callable { runCatching(t).getOrNull() } }).map { it.get() }
}
