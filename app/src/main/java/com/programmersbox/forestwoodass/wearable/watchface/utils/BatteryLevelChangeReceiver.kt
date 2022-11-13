package com.programmersbox.forestwoodass.wearable.watchface.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

class BatteryLevelChangeReceiver(private val listener: BatteryLevelChangeListener) :
    BroadcastReceiver() {

    var batteryLow: Boolean = false
    private var batteryPct: Float? = 0f

    override fun onReceive(context: Context?, batteryStatus: Intent?) {

        batteryPct = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        val low: Boolean = batteryStatus?.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)!!
        val oldValue = batteryLow
        batteryLow = low
        listener.onBatteryLevelChanged(oldValue, batteryLow)
    }

    interface BatteryLevelChangeListener {
        fun onBatteryLevelChanged(oldValue: Boolean, newValue: Boolean)
    }

}
