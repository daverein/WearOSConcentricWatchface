package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class PassiveDataRepository(private var context: Context) {

    private var sharedPreferences: SharedPreferences? = null

    init {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_FILENAME, Service.MODE_PRIVATE)
    }

    fun putHeartRateValue(value: Float)
    {
        val myEdit = sharedPreferences!!.edit()
        myEdit.putFloat(LATEST_HEART_RATE, value)
        myEdit.commit()
        val request =
            ComplicationDataSourceUpdateRequester.create(
                context, ComponentName(
                    context, HeartRateComplicationProviderService::class.java
                )
            )
        request.requestUpdateAll()
    }

    fun getHeartRateValue(): Float {
        return sharedPreferences?.getFloat(LATEST_HEART_RATE, -100f)!!
    }

    companion object {
        private const val PREFERENCES_FILENAME = "passive_data_points"
        private const val LATEST_HEART_RATE = "HeartRate"
    }
}
