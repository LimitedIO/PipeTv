package dev.limitedio.pipetv.ui.input

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dev.limitedio.pipetv.i18n.S

/** Voice search through whatever speech service the device provides (no Google lock-in in our code). */
object Voice {
    var active = false; private set
    var level = 0f; private set
    var partial = ""; private set
    var onState: () -> Unit = {}
    var onResult: (String) -> Unit = {}
    var onFail: (Boolean) -> Unit = {}
    private var rec: SpeechRecognizer? = null

    fun available(c: Context) = SpeechRecognizer.isRecognitionAvailable(c)

    fun start(c: Context) {
        stop()
        val r = SpeechRecognizer.createSpeechRecognizer(c).also { rec = it }
        r.setRecognitionListener(Listener)
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, S.cur.code)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
        active = true; partial = ""; level = 0f; onState()
    }

    fun stop() {
        rec?.destroy(); rec = null
        if (active) { active = false; onState() }
    }

    private object Listener : RecognitionListener {
        override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2) / 12f).coerceIn(0f, 1f); onState() }
        override fun onPartialResults(b: Bundle) { partial = first(b) ?: partial; onState() }
        override fun onResults(b: Bundle) { val t = first(b); stop(); if (t.isNullOrBlank()) onFail(true) else onResult(t) }
        override fun onError(error: Int) { stop(); onFail(error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) }
        override fun onReadyForSpeech(p: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(b: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(t: Int, p: Bundle?) {}
        private fun first(b: Bundle) = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
    }
}
