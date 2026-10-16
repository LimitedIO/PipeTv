package dev.limitedio.pipetv.ui

import android.content.Context
import android.view.SurfaceView
import android.view.ViewGroup
import dev.limitedio.pipetv.media.Engine

/** Hosts the video surface; Root resizes it between the docked and fullscreen rectangles. */
class Stage(c: Context) : ViewGroup(c) {
    private val surface = SurfaceView(c)

    init {
        addView(surface)
        Engine.surface = surface
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) = surface.layout(0, 0, r - l, b - t)
}
