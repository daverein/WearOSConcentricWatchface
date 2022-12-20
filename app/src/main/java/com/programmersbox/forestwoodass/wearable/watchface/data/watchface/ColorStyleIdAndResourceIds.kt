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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import com.programmersbox.forestwoodass.wearable.watchface.R

// Defaults for all styles.
// X_COLOR_STYLE_ID - id in watch face database for each style id.
// X_COLOR_STYLE_NAME_RESOURCE_ID - String name to display in the user settings UI for the style.
// X_COLOR_STYLE_ICON_ID - Icon to display in the user settings UI for the style.
const val AMBIENT_COLOR_STYLE_ID = "ambient_style_id"
private const val AMBIENT_COLOR_STYLE_NAME_RESOURCE_ID = R.string.ambient_style_name

const val RED_COLOR_STYLE_ID = "red_style_id"
private const val RED_COLOR_STYLE_NAME_RESOURCE_ID = R.string.red_style_name

const val GREEN_COLOR_STYLE_ID = "green_style_id"
private const val GREEN_COLOR_STYLE_NAME_RESOURCE_ID = R.string.green_style_name

const val YELLOW_COLOR_STYLE_ID = "yellow_style_id"
private const val YELLOW_COLOR_STYLE_NAME_RESOURCE_ID = R.string.yellow_style_name

const val PURPLE_COLOR_STYLE_ID = "purple_style_id"
private const val PURPLE_COLOR_STYLE_NAME_RESOURCE_ID = R.string.purple_style_name

const val BLUE_COLOR_STYLE_ID = "blue_style_id"
private const val BLUE_COLOR_STYLE_NAME_RESOURCE_ID = R.string.blue_style_name

const val WHITE_COLOR_STYLE_ID = "white_style_id"
private const val WHITE_COLOR_STYLE_NAME_RESOURCE_ID = R.string.white_style_name

const val BITMAP_SIZE = 128f

/**
 * Represents watch face color style options the user can select (includes the unique id, the
 * complication style resource id, and general watch face color style resource ids).
 *
 * The companion object offers helper functions to translate a unique string id to the correct enum
 * and convert all the resource ids to their correct resources (with the Context passed in). The
 * renderer will use these resources to render the actual colors and ComplicationDrawables of the
 * watch face.
 */
enum class ColorStyleIdAndResourceIds(
    val id: String,
    @StringRes val nameResourceId: Int,
    @ColorRes val primaryColorId: Int,
    @ColorRes val primaryColorTextId: Int,
    @ColorRes val secondaryColorId: Int,
    @ColorRes val backgroundColorId: Int,
    @ColorRes val outerElementColorId: Int
) {
    BLUE(
        id = BLUE_COLOR_STYLE_ID,
        nameResourceId = BLUE_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.blue_primary_color,
        primaryColorTextId = R.color.blue_color_text_primary,
        secondaryColorId = R.color.blue_secondary_color,
        backgroundColorId = R.color.blue_background_color,
        outerElementColorId = R.color.blue_outer_element_color
    ),

    RED(
        id = RED_COLOR_STYLE_ID,
        nameResourceId = RED_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.red_primary_color,
        primaryColorTextId = R.color.red_color_text_primary,
        secondaryColorId = R.color.red_secondary_color,
        backgroundColorId = R.color.red_background_color,
        outerElementColorId = R.color.red_outer_element_color
    ),

    GREEN(
        id = GREEN_COLOR_STYLE_ID,
        nameResourceId = GREEN_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.green_primary_color,
        primaryColorTextId = R.color.green_color_text_primary,
        secondaryColorId = R.color.green_secondary_color,
        backgroundColorId = R.color.green_background_color,
        outerElementColorId = R.color.green_outer_element_color
    ),

    YELLOW(
        id = YELLOW_COLOR_STYLE_ID,
        nameResourceId = YELLOW_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.yellow_primary_color,
        primaryColorTextId = R.color.yellow_color_text_primary,
        secondaryColorId = R.color.yellow_secondary_color,
        backgroundColorId = R.color.yellow_background_color,
        outerElementColorId = R.color.yellow_outer_element_color
    ),

    PURPLE (
        id = PURPLE_COLOR_STYLE_ID,
        nameResourceId = PURPLE_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.purple_primary_color,
        primaryColorTextId = R.color.purple_color_text_primary,
        secondaryColorId = R.color.purple_secondary_color,
        backgroundColorId = R.color.purple_background_color,
        outerElementColorId = R.color.purple_outer_element_color
    ),

    WHITE(
        id = WHITE_COLOR_STYLE_ID,
        nameResourceId = WHITE_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.white_primary_color,
        primaryColorTextId = R.color.white_color_text_primary,
        secondaryColorId = R.color.white_secondary_color,
        backgroundColorId = R.color.white_background_color,
        outerElementColorId = R.color.white_outer_element_color
    ),

    AMBIENT(
        id = AMBIENT_COLOR_STYLE_ID,
        nameResourceId = AMBIENT_COLOR_STYLE_NAME_RESOURCE_ID,
        primaryColorId = R.color.ambient_primary_color,
        primaryColorTextId = R.color.ambient_primary_text_color,
        secondaryColorId = R.color.ambient_secondary_color,
        backgroundColorId = R.color.ambient_background_color,
        outerElementColorId = R.color.ambient_outer_element_color
    )
    ;

    companion object {
        private const val TAG = "ColorStyleIDAndResourceIds"
        /**
         * Translates the string id to the correct ColorStyleIdAndResourceIds object.
         */
        fun getColorStyleConfig(id: String): ColorStyleIdAndResourceIds {
            return when (id) {
                AMBIENT.id -> AMBIENT
                RED.id -> RED
                GREEN.id -> GREEN
                BLUE.id -> BLUE
                YELLOW.id -> YELLOW
                WHITE.id -> WHITE
                PURPLE.id -> PURPLE
                else -> BLUE
            }
        }


        // Initializes paint object for painting the clock hands with default values.
        private val stylePainter = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = true
        }

        // Initializes paint object for painting the clock hands with default values.
        private val textPainter = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        fun getBitmap(context: Context, name: Int, color1: Int, color2: Int): Bitmap
        {
            Log.d(TAG, "getBitmap colors 1: ${Integer.toHexString(color1)} and color 2: ${Integer.toHexString(color2)}")
            val bitmap: Bitmap = Bitmap.createBitmap(BITMAP_SIZE.toInt(), BITMAP_SIZE.toInt(), Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            stylePainter.color = context.getColor(color1)
            //canvas.drawCircle(0f, 0f, 24f, stylePainter)
            canvas.drawArc(0f, 0f, BITMAP_SIZE, BITMAP_SIZE, 90f, 180f, false, stylePainter)
            stylePainter.color = context.getColor(color2)
            canvas.drawArc(0f, 0f, BITMAP_SIZE, BITMAP_SIZE, -90f, 180f, false, stylePainter)

            val nameString: String = context.getString((name))

            textPainter.typeface = context.resources.getFont(R.font.rubik_medium)
            textPainter.textSize = BITMAP_SIZE*0.2f
            textPainter.color = 0xffffffff.toInt()
            val rect = Rect()
            textPainter.getTextBounds(nameString, 0, nameString.length, rect)
            canvas.drawText(nameString, BITMAP_SIZE/2-rect.width()/2, BITMAP_SIZE/2+rect.height()/2, textPainter)
            return bitmap
        }

        /**
         * Returns a list of [UserStyleSetting.ListUserStyleSetting.ListOption] for all
         * ColorStyleIdAndResourceIds enums. The watch face settings APIs use this to set up
         * options for the user to select a style.
         */
        fun toOptionList(context: Context): List<ListUserStyleSetting.ListOption> {
            val colorStyleIdAndResourceIdsList = enumValues<ColorStyleIdAndResourceIds>()

            return colorStyleIdAndResourceIdsList.map { colorStyleIdAndResourceIds ->
                ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id(colorStyleIdAndResourceIds.id),
                    context.resources,
                    colorStyleIdAndResourceIds.nameResourceId,
                    Icon.createWithBitmap(getBitmap(context,
                        colorStyleIdAndResourceIds.nameResourceId,
                        colorStyleIdAndResourceIds.primaryColorId,
                        colorStyleIdAndResourceIds.secondaryColorId
                    )
                    )
                )
            }
        }
    }
}
