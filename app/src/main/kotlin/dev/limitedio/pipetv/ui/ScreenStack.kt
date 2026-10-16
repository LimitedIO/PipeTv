package dev.limitedio.pipetv.ui

import android.view.View

/** Pages pushed above the main tabs (details, channel, playlist). Back pops them one by one. */
class ScreenStack(private val root: Root) {
    private val list = ArrayList<View>()
    val top: Screen? get() = list.lastOrNull() as? Screen
    val any get() = list.isNotEmpty()
    val views: List<View> get() = list

    fun push(v: View) {
        list.lastOrNull()?.visibility = View.GONE
        root.pages.views[root.tab].visibility = View.GONE
        list.add(v)
        root.addView(v, root.indexOfChild(root.nav))
        v.alpha = 0f; v.translationX = Ui.u(if (Ui.rtl) -60 else 60)
        v.animate().alpha(1f).translationX(0f).setDuration(240).start()
        root.requestLayout()
    }

    fun pop() {
        val v = list.removeLastOrNull() ?: return
        root.removeView(v)
        (list.lastOrNull() ?: root.pages.views[root.tab]).visibility = View.VISIBLE
    }

    fun clear() { while (any) pop() }
}
