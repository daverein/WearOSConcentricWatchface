package com.programmersbox.forestwoodass.wearable.watchface.utils

import android.content.Context
import android.graphics.RectF
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.programmersbox.forestwoodass.wearable.watchface.R

private const val LEFT_COMPLICATIONS_TOP_BOUND = 0.69f
private const val LEFT_COMPLICATIONS_BOTTOM_BOUND = 0.93f
private const val LEFT_COMPLICATION_LEFT_BOUND = 0.38f
private const val LEFT_COMPLICATION_RIGHT_BOUND = 0.62f

private const val RIGHT_COMPLICATIONS_TOP_BOUND = 0.53f
private const val RIGHT_COMPLICATIONS_BOTTOM_BOUND = 0.77f
private const val RIGHT_COMPLICATION_LEFT_BOUND = 0.13f
private const val RIGHT_COMPLICATION_RIGHT_BOUND = 0.37f

private const val MIDDLE_COMPLICATIONS_TOP_BOUND = 0.53f
private const val MIDDLE_COMPLICATIONS_BOTTOM_BOUND = 0.77f
private const val MIDDLE_COMPLICATION_LEFT_BOUND = 0.63f
private const val MIDDLE_COMPLICATION_RIGHT_BOUND = 0.87f

// Utility function that initializes default complication slots (left and right).
fun createDigitalComplicationSlotManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository
): ComplicationSlotsManager {
    val defaultCanvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(context, R.drawable.complication_style)!!,
                watchState,
                listener
            )
        }

    val leftComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Left.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Left.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.NO_DATA_SOURCE,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                LEFT_COMPLICATION_LEFT_BOUND,
                LEFT_COMPLICATIONS_TOP_BOUND,
                LEFT_COMPLICATION_RIGHT_BOUND,
                LEFT_COMPLICATIONS_BOTTOM_BOUND
            )
        )
    )
        .build()

    val middleComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Middle.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Middle.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.NO_DATA_SOURCE,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                MIDDLE_COMPLICATION_LEFT_BOUND,
                MIDDLE_COMPLICATIONS_TOP_BOUND,
                MIDDLE_COMPLICATION_RIGHT_BOUND,
                MIDDLE_COMPLICATIONS_BOTTOM_BOUND
            )
        )
    )
        .build()

    val rightComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Right.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Right.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.NO_DATA_SOURCE,
            ComplicationType.RANGED_VALUE
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                RIGHT_COMPLICATION_LEFT_BOUND,
                RIGHT_COMPLICATIONS_TOP_BOUND,
                RIGHT_COMPLICATION_RIGHT_BOUND,
                RIGHT_COMPLICATIONS_BOTTOM_BOUND
            )
        )
    ).build()

    return ComplicationSlotsManager(
        listOf(leftComplication, rightComplication, middleComplication),
        currentUserStyleRepository
    )
}
