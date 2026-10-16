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
        if (root.controls.hold.handle(e)) return true
        if (e.action == KeyEvent.ACTION_DOWN && root.controls.key(e)) return true
        if (e.action == KeyEvent.ACTION_UP && e.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(e)
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

    override fun onStop() {
        super.onStop(); Voice.stop()
        if (Session.item != null) Engine.exo.pause()
    }

    override fun onDestroy() { super.onDestroy(); if (isFinishing) { Session.stop(); Engine.exo.clearVideoSurface() } }
}
