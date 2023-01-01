/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

// Information needed for complications.
// Creates bounds for the locations of both right and left complications. (This is the
// location from 0.0 - 1.0.)
// Both left and right complications use the same top and bottom bounds.
const val CONCENTRIC_LEFT_COMPLICATIONS_TOP_BOUND = 0.12f
const val CONCENTRIC_LEFT_COMPLICATIONS_BOTTOM_BOUND = 0.36f
const val CONCENTRIC_LEFT_COMPLICATION_LEFT_BOUND = 0.38f
const val CONCENTRIC_LEFT_COMPLICATION_RIGHT_BOUND = 0.62f

const val CONCENTRIC_RIGHT_COMPLICATIONS_TOP_BOUND = 0.65f
const val CONCENTRIC_RIGHT_COMPLICATIONS_BOTTOM_BOUND = 0.89f
const val CONCENTRIC_RIGHT_COMPLICATION_LEFT_BOUND = 0.38f
const val CONCENTRIC_RIGHT_COMPLICATION_RIGHT_BOUND = 0.62f

const val CONCENTRIC_MIDDLE_COMPLICATIONS_TOP_BOUND = 0.38f
const val CONCENTRIC_MIDDLE_COMPLICATIONS_BOTTOM_BOUND = 0.62f
const val CONCENTRIC_MIDDLE_COMPLICATION_LEFT_BOUND = 0.48f
const val CONCENTRIC_MIDDLE_COMPLICATION_RIGHT_BOUND = 0.72f


// Utility function that initializes default complication slots (left and right).
fun createConcentricComplicationSlotManager(
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
                CONCENTRIC_LEFT_COMPLICATION_LEFT_BOUND,
                CONCENTRIC_LEFT_COMPLICATIONS_TOP_BOUND,
                CONCENTRIC_LEFT_COMPLICATION_RIGHT_BOUND,
                CONCENTRIC_LEFT_COMPLICATIONS_BOTTOM_BOUND
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
                CONCENTRIC_MIDDLE_COMPLICATION_LEFT_BOUND,
                CONCENTRIC_MIDDLE_COMPLICATIONS_TOP_BOUND,
                CONCENTRIC_MIDDLE_COMPLICATION_RIGHT_BOUND,
                CONCENTRIC_MIDDLE_COMPLICATIONS_BOTTOM_BOUND
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
                CONCENTRIC_RIGHT_COMPLICATION_LEFT_BOUND,
                CONCENTRIC_RIGHT_COMPLICATIONS_TOP_BOUND,
                CONCENTRIC_RIGHT_COMPLICATION_RIGHT_BOUND,
                CONCENTRIC_RIGHT_COMPLICATIONS_BOTTOM_BOUND
            )
        )
    ).build()

    return ComplicationSlotsManager(
        listOf(leftComplication, rightComplication, middleComplication),
        currentUserStyleRepository
    )

}
