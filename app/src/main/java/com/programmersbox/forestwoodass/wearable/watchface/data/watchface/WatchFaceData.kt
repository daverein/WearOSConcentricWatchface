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
const val DRAW_TIME_AOD = true
const val COMPAOD_DEFAULT = true
const val MINUTEDIALAOD_DEFAULT = true

// Because the minute length is something the user can edit, we make it publicly
// accessible as a default. We also specify the minimum and maximum values for the user
// settings as well.
const val SHIFT_PIXEL_AOD_FRACTION_DEFAULT = 5.0f
const val SHIFT_PIXEL_AOD_FRACTION_MINIMUM = 0.0f
const val SHIFT_PIXEL_AOD_FRACTION_MAXIMUM = 16.00000f

private const val NUMBER_RADIUS_FRACTION = 0.45f

/**
 * Represents all data needed to render an analog watch face.
 */
data class WatchFaceData(
    val activeColorStyle: ColorStyleIdAndResourceIds = ColorStyleIdAndResourceIds.BLUE,
    val ambientColorStyle: ColorStyleIdAndResourceIds = ColorStyleIdAndResourceIds.AMBIENT,
    val layoutStyle: LayoutStyleIdAndResourceIds = LayoutStyleIdAndResourceIds.FULLFACE,
    val timeAOD: Boolean = DRAW_TIME_AOD,
    val compAOD: Boolean = COMPAOD_DEFAULT,
    val minuteDialAOD: Boolean = MINUTEDIALAOD_DEFAULT,
    val shiftPixelAmount: Float = SHIFT_PIXEL_AOD_FRACTION_DEFAULT,
    val numberRadiusFraction: Float = NUMBER_RADIUS_FRACTION,
)
