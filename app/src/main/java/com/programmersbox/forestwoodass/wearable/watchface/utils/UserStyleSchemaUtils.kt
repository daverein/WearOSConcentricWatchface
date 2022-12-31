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
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.*

// Keys to matched content in the  the user style settings. We listen for changes to these
// values in the renderer and if new, we will update the database and update the watch face
// being rendered.
const val COLOR_STYLE_SETTING = "color_style_setting"
const val LAYOUT_STYLE_SETTING = "layout_style_setting"
const val STYLE_ICON_STYLE_SETTING = "style_icon_style_setting"
const val DRAW_TIME_AOD_STYLE_SETTING = "draw_time_aod_style_setting"
const val DRAW_DATE_STYLE_SETTING = "draw_date_style_setting"
const val DRAW_COMP_CIRCLES_STYLE_SETTING = "draw_comp_circles_style_setting"
const val COMPAOD_STYLE_SETTING = "compaod_style_setting"
const val ACTIVE_AS_AMBIENT_STYLE_SETTING = "active_as_ambient_style_setting"
const val MINUTEDIALAOD_STYLE_SETTING = "minutedialaod_style_setting"
const val SHIFT_PIXEL_STYLE_SETTING = "shift_pixels_style_setting"
const val LOW_POWER_STYLE_SETTING = "low_power_style_setting"
/*
 * Creates user styles in the settings activity associated with the watch face, so users can
 * edit different parts of the watch face. In the renderer (after something has changed), the
 * watch face listens for a flow from the watch face API data layer and updates the watch face.
 */
private fun getColorStyleSetting(context: Context): UserStyleSetting.ListUserStyleSetting {
   return UserStyleSetting.ListUserStyleSetting(
       UserStyleSetting.Id(COLOR_STYLE_SETTING),
       context.resources,
       R.string.colors_style_setting,
       R.string.colors_style_setting_description,
       null,
       ColorStylesDynamic.toOptionList(context),
       listOf(
           WatchFaceLayer.BASE,
           WatchFaceLayer.COMPLICATIONS,
           WatchFaceLayer.COMPLICATIONS_OVERLAY
       )
   )
}

private fun getLayoutStyleSetting(context: Context): UserStyleSetting.ListUserStyleSetting {
    return UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id(LAYOUT_STYLE_SETTING),
        context.resources,
        R.string.layout_style_setting,
        R.string.layout_style_setting_description,
        null,
        LayoutStyleIdAndResourceIds.toOptionList(context),
        listOf(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
    )
}

private fun getDrawDateStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(DRAW_DATE_STYLE_SETTING),
        context.resources,
        R.string.watchface_draw_date_setting,
        R.string.watchface_draw_date_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        DRAW_DATE
    )
}

private fun getLowPowerStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(LOW_POWER_STYLE_SETTING),
        context.resources,
        R.string.watchface_low_power_setting,
        R.string.watchface_low_power_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        LOW_POWER
    )
}

private fun getStyleIconSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(STYLE_ICON_STYLE_SETTING),
        context.resources,
        R.string.watchface_style_icon_setting,
        R.string.watchface_style_icon_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        STYLE_ICON
    )
}

private fun getDrawTimeOnAODStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(DRAW_TIME_AOD_STYLE_SETTING),
        context.resources,
        R.string.watchface_pips_setting,
        R.string.watchface_pips_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        DRAW_TIME_AOD
    )
}

private fun getDrawCompCirclesStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(DRAW_COMP_CIRCLES_STYLE_SETTING),
        context.resources,
        R.string.watchface_draw_comp_circles_setting,
        R.string.watchface_draw_comp_circles_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        DRAW_COMP_CIRCLES
    )
}

private fun getCompaodStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(COMPAOD_STYLE_SETTING),
        context.resources,
        R.string.watchface_compaod_setting,
        R.string.watchface_compaod_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        COMPAOD_DEFAULT
    )
}

private fun getMinuteaodStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(MINUTEDIALAOD_STYLE_SETTING),
        context.resources,
        R.string.watchface_minutedialaod_setting,
        R.string.watchface_minutedialaod_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        COMPAOD_DEFAULT
    )
}

private fun getActiveAsAmbientStyleSetting(context: Context): UserStyleSetting.BooleanUserStyleSetting {
    return UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(ACTIVE_AS_AMBIENT_STYLE_SETTING),
        context.resources,
        R.string.watchface_active_as_ambient_setting,
        R.string.watchface_active_as_ambient_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        AAA_DEFAULT
    )
}

private fun getShiftPixelStyleSetting(context: Context): UserStyleSetting.DoubleRangeUserStyleSetting {
    return UserStyleSetting.DoubleRangeUserStyleSetting(
        UserStyleSetting.Id(SHIFT_PIXEL_STYLE_SETTING),
        context.resources,
        R.string.watchface_hand_length_setting,
        R.string.watchface_hand_length_setting_description,
        null,
        SHIFT_PIXEL_AOD_FRACTION_MINIMUM.toDouble(),
        SHIFT_PIXEL_AOD_FRACTION_MAXIMUM.toDouble(),
        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
        SHIFT_PIXEL_AOD_FRACTION_DEFAULT.toDouble()
    )
}

fun createUserStyleSchemaConcentric(context: Context): UserStyleSchema {
    // 4. Create style settings to hold all options.
    return UserStyleSchema(
        listOf(
            getColorStyleSetting(context),
            getLayoutStyleSetting(context),
            getDrawDateStyleSetting(context),
            getStyleIconSetting(context),
            getDrawCompCirclesStyleSetting(context),
            getDrawTimeOnAODStyleSetting(context),
            getCompaodStyleSetting(context),
            getMinuteaodStyleSetting(context),
            getActiveAsAmbientStyleSetting(context),
            getShiftPixelStyleSetting(context)
        )
    )
}

fun createUserStyleSchemaAnalog(context: Context): UserStyleSchema {
    // 4. Create style settings to hold all options.
    return UserStyleSchema(
        listOf(
            getColorStyleSetting(context),
            getDrawDateStyleSetting(context),
            getStyleIconSetting(context),
            getDrawCompCirclesStyleSetting(context),
            getLowPowerStyleSetting(context),
            getDrawTimeOnAODStyleSetting(context),
            getCompaodStyleSetting(context),
            getActiveAsAmbientStyleSetting(context),
            getShiftPixelStyleSetting(context)
        )
    )
}
