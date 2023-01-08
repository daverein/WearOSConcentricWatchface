package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class PassiveDataRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILENAME, Service.MODE_PRIVATE)
    }

    fun putHeartRateValue(value: Float)
    {
        sharedPreferences.edit()
            .putFloat(LATEST_HEART_RATE, value)
            .apply()
        val request =
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, HeartRateComplicationProviderService::class.java)
            )
        request.requestUpdateAll()
    }

    fun getHeartRateValue(): Float {
        return sharedPreferences.getFloat(LATEST_HEART_RATE, NO_HEART_RATE)
    }

    fun getPermissionDeclined(): Boolean {
        return sharedPreferences.getBoolean(DECLINED_PERMISSION, false)
    }

    fun putPermissionDeclined(value: Boolean) {
        sharedPreferences.edit()
            .putBoolean(DECLINED_PERMISSION, value)
            .apply()
    }

    companion object {
        private const val PREFERENCES_FILENAME = "passive_data_points"
        private const val LATEST_HEART_RATE = "HeartRate"
        private const val DECLINED_PERMISSION = "permissions_declined"
        const val NO_HEART_RATE = -100f
        const val NOT_HEART_RATE_CAPABLE = -200f
    }
}
