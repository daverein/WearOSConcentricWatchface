package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.*
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM

class PassiveDataService : PassiveListenerService() {
    private lateinit var dataRepo: PassiveDataRepository
    override fun onCreate() {
        super.onCreate()
        dataRepo = PassiveDataRepository(this)
    }

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val bpmList: List<SampleDataPoint<Double>> =
            dataPoints.getData(HEART_RATE_BPM)
        if (bpmList.isNotEmpty()) {
            val savedValue = bpmList// dataPoints can have multiple types (e.g. if the app is registered for multiple types).
                .filter { it.dataType == HEART_RATE_BPM }
                // where accuracy information is available, only show readings that are of medium or
                // high accuracy. (Where accuracy information isn't available, show the reading if it is
                // a positive value).
                .filter {
                    it.accuracy == null ||
                        setOf(
                            HeartRateAccuracy.SensorStatus.ACCURACY_HIGH,
                            HeartRateAccuracy.SensorStatus.ACCURACY_MEDIUM
                        ).contains((it.accuracy as HeartRateAccuracy).sensorStatus)
                }
                .filter {it.value > 0}
                // HEART_RATE_BPM is a SAMPLE type, so start and end times are the same.
                .maxByOrNull { it.timeDurationFromBoot }?.value
            Log.i(
                TAG,
                "List of ${bpmList.size} items, FINAL SAVED HEART RATE IS: $savedValue")
            savedValue?.let{
                dataRepo.putHeartRateValue(it.toFloat())
            }
        }
    }

    companion object {
        var TAG = "PassiveDataService"
    }
}
