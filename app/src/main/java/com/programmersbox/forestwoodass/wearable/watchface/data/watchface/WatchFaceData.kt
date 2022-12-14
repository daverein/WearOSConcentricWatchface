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

// Defaults for the watch face. All private values aren't editable by the user, so they don't need
// to be exposed as settings defaults.
const val DRAW_COMP_CIRCLES = true
const val DRAW_TIME_AOD = true
const val DRAW_DATE = true
@Suppress("SpellCheckingInspection")
const val COMPAOD_DEFAULT = true
@Suppress("SpellCheckingInspection")
const val MINUTEDIALAOD_DEFAULT = true
const val AAA_DEFAULT = false
const val STYLE_ICON = true
const val LOW_POWER = false

// Because the minute length is something the user can edit, we make it publicly
// accessible as a default. We also specify the minimum and maximum values for the user
// settings as well.
const val SHIFT_PIXEL_AOD_FRACTION_DEFAULT = 5.0f
const val SHIFT_PIXEL_AOD_FRACTION_MINIMUM = 0.0f
const val SHIFT_PIXEL_AOD_FRACTION_MAXIMUM = 10.00000f
const val LAYOUT_ALT_CLOCK_SHIFT = 0.30f
const val LAYOUT_ALT_CLOCK_SHIFT_SCALED = 0.45f
const val AOD_ZOOM_LEVEL_1 = 1.15f
const val AOD_ZOOM_LEVEL_2 = 1.20f
const val AOD_ZOOM_LEVEL_3 = 0.95f
const val AOD_ZOOM_LEVEL_4 = 1.12f
@Suppress("SpellCheckingInspection")
const val SCALED_CLOCKFACE_AMOUNT = 1.35f
const val SCALED_WATCHFACE_SHIFT_X = 1.00f
const val SCALED_WATCHFACE_SHIFTY = 1.26f
const val FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTER = 0.15f
const val FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER = 0.85f
const val HOUR_TEXT_CENTER_OFFSET_SHIFT = 0.45f
const val MINUTE_TEXT_CENTER_OFFSET_SHIFT = 0.55f
const val MINUTE_HIGHLIGHT_WIDTH_FRACTION = 1.5f
const val CAL_CIRCLE_RADIUS = 2f
const val CAL_TEXT_OFFSET = 4.5f
const val SECOND_DIAL_TICK_START = 0.98f
const val SECOND_NUMBER_START = 0.03f
const val MINUTE_NUMBER_START = 0.76f
const val MINUTE_DIAL_START_OFFSET = 0.88f
const val MINUTE_DIAL_MAJOR_MARK_END_OFFSET = 0.85f
const val MINUTE_DIAL_MINOR_MARK_END_OFFSET = 0.86f


const val HOUR_FONT_SIZE = 0.27848f
const val MINUTE_FONT_SIZE = 0.12658f
const val SECOND_DIAL_FONT_SIZE = 0.06323f
const val MINUTE_DIAL_FONT_SIZE = 0.06323f
const val MONTH_FONT_SIZE = 0.04f
const val DAY_FONT_SIZE = 0.055f

/**
 * Represents all data needed to render an analog watch face.
 */
data class WatchFaceData(
    val activeColorStyle: ColorStylesDynamic = ColorStylesDynamic.getColorStyleConfig(BLUE_COLOR_STYLEID),
    val ambientColorStyle: ColorStylesDynamic = ColorStylesDynamic.getColorStyleConfig(AMBIENT_COLOR_STYLEID),
    val layoutStyle: LayoutStyleIdAndResourceIds = LayoutStyleIdAndResourceIds.FULLFACE,
    val styleIcon: Boolean = STYLE_ICON,
    val drawDate: Boolean = DRAW_DATE,
    val lowPower: Boolean = LOW_POWER,
    val timeAOD: Boolean = DRAW_TIME_AOD,
    val drawCompCircles: Boolean = DRAW_COMP_CIRCLES,
    val compAOD: Boolean = COMPAOD_DEFAULT,
    val minuteDialAOD: Boolean = MINUTEDIALAOD_DEFAULT,
    val activeAsAmbient: Boolean = AAA_DEFAULT,
    val shiftPixelAmount: Float = SHIFT_PIXEL_AOD_FRACTION_DEFAULT
)
