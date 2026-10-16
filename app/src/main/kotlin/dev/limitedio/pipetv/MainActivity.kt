package dev.limitedio.pipetv

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import dev.limitedio.pipetv.audio.Sfx
import dev.limitedio.pipetv.audio.Sound
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.ui.Root
import dev.limitedio.pipetv.ui.Ui
import dev.limitedio.pipetv.ui.input.Voice
import dev.limitedio.pipetv.update.Updater

/** Single activity. The whole UI is one custom view tree built in code. */
class MainActivity : Activity() {
    private lateinit var root: Root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.init(this)
        S.apply()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = Root(this)
        root.controls.onExit = { finish() }
        setContentView(root)
        immersive()
        Looper.myQueue().addIdleHandler { Engine.exo; false } // build the player only once the first frame is out
        Sfx.init()
        Updater.onChange = { root.update.refresh() }
        root.askMic = ::startVoice
        Bg.main.postDelayed({ Updater.check() }, 4000) // quietly looks for a new version shortly after launch
    }

    private fun immersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) immersive() }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        when (root.controls.hold.handle(e)) { 1 -> return true; 2 -> { click(e.keyCode); return true } }
        if (e.action == KeyEvent.ACTION_DOWN && root.controls.key(e)) { click(e.keyCode); return true }
        if (e.action == KeyEvent.ACTION_UP && e.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(e)
    }

    /** A soft sound for each handled key (no ticks while a video is fullscreen, so seeking stays quiet). */
    private fun click(k: Int) {
        val fx = when (k) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> Sfx.Fx.SELECT
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> Sfx.Fx.BACK
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> Sfx.Fx.OPEN
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> if (root.full) null else Sfx.Fx.TICK
            else -> null
        }
        fx?.let { Sfx.play(it) }
    }

    private fun startVoice() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
        else Voice.start(this)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        if (code != 7) return
        if (res.firstOrNull() == PackageManager.PERMISSION_GRANTED) Voice.start(this) else root.toast.show(S.t(K.MIC_DENIED))
    }

    override fun onStart() { super.onStart(); Sound.foreground = true; Sound.update() }

    override fun onStop() {
        super.onStop(); Voice.stop(); Sound.foreground = false; Sound.update()
        if (Session.item != null) Engine.exo.pause()
    }

    override fun onDestroy() { super.onDestroy(); if (isFinishing) { Session.stop(); Engine.exo.clearVideoSurface() } }
}
