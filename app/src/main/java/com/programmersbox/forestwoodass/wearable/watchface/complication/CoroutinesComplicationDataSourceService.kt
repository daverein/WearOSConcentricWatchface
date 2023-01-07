package com.programmersbox.forestwoodass.wearable.watchface.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoroutinesComplicationDataSourceService : ComplicationDataSourceService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }


    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        // Default override
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        // default override
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        serviceScope.launch(Dispatchers.IO) {
            listener.onComplicationData(onComplicationUpdate(request))
        }
    }

    abstract suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest): ComplicationData?
}
