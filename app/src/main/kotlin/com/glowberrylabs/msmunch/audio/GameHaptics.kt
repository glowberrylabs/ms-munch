package com.glowberrylabs.msmunch.audio

import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.ref.WeakReference

class GameHaptics {

    private var viewReference: WeakReference<View>? = null

    fun attach(view: View) {
        viewReference = WeakReference(view)
    }

    fun detach(view: View) {
        if (viewReference?.get() === view) {
            viewReference = null
        }
    }

    fun tick() {
        viewReference?.get()?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun thump() {
        viewReference?.get()?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
