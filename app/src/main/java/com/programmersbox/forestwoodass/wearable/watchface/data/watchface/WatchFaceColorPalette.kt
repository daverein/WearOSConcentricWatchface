/*
 * Copyright 2021 The Android Open Source Project
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
package com.programmersbox.forestwoodass.wearable.watchface.data.watchface

import android.content.Context
import androidx.wear.watchface.complications.rendering.ComplicationDrawable

/**
 * Color resources and drawable id needed to render the watch face. Translated from
 * [ColorStylesDynamic] constant ids to actual resources with context at run time.
 *
 * This is only needed when the watch face is active.
 *
 * Note: We do not use the context to generate a [ComplicationDrawable] from the
 * complicationStyleDrawableId (representing the style), because a new, separate
 * [ComplicationDrawable] is needed for each complication. Because the renderer will loop through
 * all the complications and there can be more than one, this also allows the renderer to create
 * as many [ComplicationDrawable]s as needed.
 */
data class WatchFaceColorPalette(
    val activePrimaryColor: Int,
    val activePrimaryTextColor: Int,
    val activeSecondaryColor: Int,
    val activeBackgroundColor: Int,
    val activeOuterElementColor: Int,
    val ambientPrimaryColor: Int,
    val ambientPrimaryTextColor: Int,
    val ambientSecondaryColor: Int,
    val ambientBackgroundColor: Int,
    val ambientOuterElementColor: Int
) {
    companion object {
        /**
         * Converts [ColorStylesDynamic] to [WatchFaceColorPalette].
         */
        fun convertToWatchFaceColorPalette(
            context: Context,
            activeColorStyle: ColorStylesDynamic,
            ambientColorStyle: ColorStylesDynamic
        ): WatchFaceColorPalette {
            return WatchFaceColorPalette(
                // Active colors
                activePrimaryColor = activeColorStyle.getPrimaryColor(context),
                activePrimaryTextColor = activeColorStyle.getPrimaryColorText(context),
                activeSecondaryColor = activeColorStyle.getSecondaryColor(context),
                activeBackgroundColor = activeColorStyle.getBackgroundColor(context),
                activeOuterElementColor = activeColorStyle.getOuterElementColor(context),
                // Ambient colors
                ambientPrimaryColor = ambientColorStyle.getPrimaryColor(context),
                ambientPrimaryTextColor = ambientColorStyle.getPrimaryColorText(context),
                ambientSecondaryColor = ambientColorStyle.getSecondaryColor(context),
                ambientBackgroundColor = ambientColorStyle.getBackgroundColor(context),
                ambientOuterElementColor = ambientColorStyle.getOuterElementColor(context)
            )
        }
    }
}
