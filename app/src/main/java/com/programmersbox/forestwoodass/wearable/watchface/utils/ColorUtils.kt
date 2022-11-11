package com.programmersbox.forestwoodass.wearable.watchface.utils

import android.graphics.Color
import kotlin.math.roundToInt

class ColorUtils {
    companion object {
        fun darkenColor(color: Int): Int {
            val factor = 0.5f
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(
                a,
                r.coerceAtMost(255),
                g.coerceAtMost(255),
                b.coerceAtMost(255)
            )
        }
    }
}
