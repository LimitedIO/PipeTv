package dev.limitedio.pipetv

import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.ui.Root
import dev.limitedio.pipetv.ui.Ui
import dev.limitedio.pipetv.update.Updater

/** Single activity. The whole UI is one custom view tree built in code. */
class MainActivity : Activity() {
    private lateinit var root: Root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = Root(this)
        root.controls.onExit = { finish() }
        setContentView(root)
        immersive()
        Looper.myQueue().addIdleHandler { Engine.exo; false } // build the player only once the first frame is out
        Updater.onReady = { root.toast.show("Update $it downloaded. Open Settings to install.") }
        Bg.main.postDelayed({ Updater.check() }, 5000)
    }

    private fun immersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) immersive() }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN && root.controls.key(e)) return true
        if (e.action == KeyEvent.ACTION_UP && e.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(e)
    }

    override fun onStop() { super.onStop(); if (Session.active) Engine.exo.pause() }

    override fun onDestroy() { super.onDestroy(); if (isFinishing) { Session.stop(); Engine.exo.clearVideoSurface() } }
}
