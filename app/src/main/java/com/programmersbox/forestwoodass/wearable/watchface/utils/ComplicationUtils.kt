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
private const val LEFT_COMPLICATIONS_TOP_BOUND = 0.14f
private const val LEFT_COMPLICATIONS_BOTTOM_BOUND = 0.39f
const val LEFT_COMPLICATION_LEFT_BOUND = 0.375f
const val LEFT_COMPLICATION_RIGHT_BOUND = 0.625f

private const val RIGHT_COMPLICATIONS_TOP_BOUND = 0.65f
private const val RIGHT_COMPLICATIONS_BOTTOM_BOUND = 0.90f
const val RIGHT_COMPLICATION_LEFT_BOUND = 0.375f
const val RIGHT_COMPLICATION_RIGHT_BOUND = 0.625f

private const val MIDDLE_COMPLICATIONS_TOP_BOUND = 0.35f
private const val MIDDLE_COMPLICATIONS_BOTTOM_BOUND = 0.65f
const val MIDDLE_COMPLICATION_LEFT_BOUND = 0.47f
const val MIDDLE_COMPLICATION_RIGHT_BOUND = 0.82f


private const val DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID = R.drawable.complication_blue_style

// Unique IDs for each complication. The settings activity that supports allowing users
// to select their complication data provider requires numbers to be >= 0.
internal const val LEFT_COMPLICATION_ID = 100
internal const val RIGHT_COMPLICATION_ID = 101
internal const val MIDDLE_COMPLICATION_ID = 102

/**
 * Represents the unique id associated with a complication and the complication types it supports.
 */
sealed class ComplicationConfig(val id: Int, val supportedTypes: List<ComplicationType>) {
    object Left : ComplicationConfig(
        LEFT_COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT
        )
    )

    object Right : ComplicationConfig(
        RIGHT_COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT
        )
    )

    object Middle : ComplicationConfig(
        MIDDLE_COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT
        )
    )
}

// Utility function that initializes default complication slots (left and right).
fun createComplicationSlotManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository,
    drawableId: Int = DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID
): ComplicationSlotsManager {
    val defaultCanvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(context, drawableId)!!,
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
            ComplicationType.SHORT_TEXT
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
