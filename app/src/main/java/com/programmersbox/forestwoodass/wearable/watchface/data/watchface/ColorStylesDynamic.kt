package com.programmersbox.forestwoodass.wearable.watchface.data.watchface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.wear.watchface.style.UserStyleSetting
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.utils.ColorUtils.Companion.darkenColor

// Defaults for all styles.
// X_COLOR_STYLE_ID - id in watch face database for each style id.
// X_COLOR_STYLE_NAME_RESOURCE_ID - String name to display in the user settings UI for the style.
// X_COLOR_STYLE_ICON_ID - Icon to display in the user settings UI for the style.

@Suppress("SpellCheckingInspection")
const val AMBIENT_COLOR_STYLEID = "ambient_style_id"
private const val AMBIENT_COLOR_STYLE_NAME_RESOURCE_ID = R.string.ambient_style_name

@Suppress("SpellCheckingInspection")
const val RED_COLOR_STYLEID = "red_style_id"
private const val RED_COLOR_STYLE_NAME_RESOURCE_ID = R.string.red_style_name

@Suppress("SpellCheckingInspection")
const val GREEN_COLOR_STYLEID = "green_style_id"
private const val GREEN_COLOR_STYLE_NAME_RESOURCE_ID = R.string.green_style_name

@Suppress("SpellCheckingInspection")
const val YELLOW_COLOR_STYLEID = "yellow_style_id"
private const val YELLOW_COLOR_STYLE_NAME_RESOURCE_ID = R.string.yellow_style_name

@Suppress("SpellCheckingInspection")
const val PURPLE_COLOR_STYLEID = "purple_style_id"
private const val PURPLE_COLOR_STYLE_NAME_RESOURCE_ID = R.string.purple_style_name

@Suppress("SpellCheckingInspection")
const val BLUE_COLOR_STYLEID = "blue_style_id"
private const val BLUE_COLOR_STYLE_NAME_RESOURCE_ID = R.string.blue_style_name

@Suppress("SpellCheckingInspection")
const val GOLD_COLOR_STYLEID = "gold_style_id"
private const val GOLD_COLOR_STYLE_NAME_RESOURCE_ID = R.string.gold_style_name

@Suppress("SpellCheckingInspection")
const val BLACK_GOLD_COLOR_STYLEID = "black_gold_style_id"
private const val BLACK_GOLD_COLOR_STYLE_NAME_RESOURCE_ID = R.string.black_gold_style_name

@Suppress("SpellCheckingInspection")
const val ROSEGOLD_COLOR_STYLEID = "rosegold_style_id"
@Suppress("SpellCheckingInspection")
private const val ROSEGOLD_COLOR_STYLE_NAME_RESOURCE_ID = R.string.rosegold_style_name

@Suppress("SpellCheckingInspection")
const val SILVER_COLOR_STYLEID = "silver_style_id"
private const val SILVER_COLOR_STYLE_NAME_RESOURCE_ID = R.string.silver_style_name

@Suppress("SpellCheckingInspection")
const val WHITE_COLOR_STYLEID = "white_style_id"
private const val WHITE_COLOR_STYLE_NAME_RESOURCE_ID = R.string.white_style_name

const val BITMAP_SIZE = 128f

class ColorStylesDynamic(
    val id: String,
    @StringRes val nameResourceId: Int = 0,
    @ColorRes val primaryColorId: Int = 0,
    @ColorRes val primaryColorTextId: Int = 0,
    @ColorRes val secondaryColorId: Int = 0,
    @ColorRes val backgroundColorId: Int = 0,
    @ColorRes val outerElementColorId: Int = 0,
    var name: String? = null,
    val primaryColor: Int = 0,
    val primaryColorText: Int = 0,
    val secondaryColor: Int = 0,
    val backgroundColor: Int = 0,
    val outerElementColor: Int = 0
)  {
    fun getName(context: Context): String {
        return if ( nameResourceId == 0 ) {
            if ( name != null ) {
                name!!
            } else {
                context.getString(BLUE_COLOR_STYLE_NAME_RESOURCE_ID)
            }
        } else
            context.getString(nameResourceId)
    }
    fun getPrimaryColor(context: Context): Int {
        return if ( primaryColorId == 0 )
            primaryColor
        else
            context.getColor(primaryColorId)
    }
    fun getPrimaryColorText(context: Context): Int {
        return if ( primaryColorTextId == 0 )
            primaryColorText
        else
            context.getColor(primaryColorTextId)
    }
    fun getSecondaryColor(context: Context): Int {
        return if ( secondaryColorId == 0 )
            secondaryColor
        else
            context.getColor(secondaryColorId)
    }
    fun getBackgroundColor(context: Context): Int {
        return if ( backgroundColorId == 0 )
            backgroundColor
        else
            context.getColor(backgroundColorId)
    }
    fun getOuterElementColor(context: Context): Int {
        return if ( outerElementColorId == 0 )
            outerElementColor
        else
            context.getColor(outerElementColorId)
    }
    companion object {
        @Suppress("unused")
        private const val TAG = "ColorStylesDynamic"

        var instance = initArray()

        private fun initArray(): MutableList<ColorStylesDynamic> {
            val colorList = mutableListOf<ColorStylesDynamic>()
            colorList.add(ColorStylesDynamic(
                id = BLUE_COLOR_STYLEID,
                nameResourceId = BLUE_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.blue_primary_color,
                primaryColorTextId = R.color.blue_color_text_primary,
                secondaryColorId = R.color.blue_secondary_color,
                backgroundColorId = R.color.blue_background_color,
                outerElementColorId = R.color.blue_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = RED_COLOR_STYLEID,
                nameResourceId = RED_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.red_primary_color,
                primaryColorTextId = R.color.red_color_text_primary,
                secondaryColorId = R.color.red_secondary_color,
                backgroundColorId = R.color.red_background_color,
                outerElementColorId = R.color.red_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = GREEN_COLOR_STYLEID,
                nameResourceId = GREEN_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.green_primary_color,
                primaryColorTextId = R.color.green_color_text_primary,
                secondaryColorId = R.color.green_secondary_color,
                backgroundColorId = R.color.green_background_color,
                outerElementColorId = R.color.green_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = YELLOW_COLOR_STYLEID,
                nameResourceId = YELLOW_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.yellow_primary_color,
                primaryColorTextId = R.color.yellow_color_text_primary,
                secondaryColorId = R.color.yellow_secondary_color,
                backgroundColorId = R.color.yellow_background_color,
                outerElementColorId = R.color.yellow_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = PURPLE_COLOR_STYLEID,
                nameResourceId = PURPLE_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.purple_primary_color,
                primaryColorTextId = R.color.purple_color_text_primary,
                secondaryColorId = R.color.purple_secondary_color,
                backgroundColorId = R.color.purple_background_color,
                outerElementColorId = R.color.purple_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = GOLD_COLOR_STYLEID,
                nameResourceId = GOLD_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.gold_primary_color,
                primaryColorTextId = R.color.gold_color_text_primary,
                secondaryColorId = R.color.gold_secondary_color,
                backgroundColorId = R.color.gold_background_color,
                outerElementColorId = R.color.gold_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = BLACK_GOLD_COLOR_STYLEID,
                nameResourceId = BLACK_GOLD_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.black_gold_primary_color,
                primaryColorTextId = R.color.black_gold_color_text_primary,
                secondaryColorId = R.color.black_gold_secondary_color,
                backgroundColorId = R.color.black_gold_background_color,
                outerElementColorId = R.color.black_gold_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = ROSEGOLD_COLOR_STYLEID,
                nameResourceId = ROSEGOLD_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.rosegold_primary_color,
                primaryColorTextId = R.color.rosegold_color_text_primary,
                secondaryColorId = R.color.rosegold_secondary_color,
                backgroundColorId = R.color.rosegold_background_color,
                outerElementColorId = R.color.rosegold_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = SILVER_COLOR_STYLEID,
                nameResourceId = SILVER_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.silver_primary_color,
                primaryColorTextId = R.color.silver_color_text_primary,
                secondaryColorId = R.color.silver_secondary_color,
                backgroundColorId = R.color.silver_background_color,
                outerElementColorId = R.color.silver_outer_element_color
            )
            )
            colorList.add(ColorStylesDynamic(
                id = WHITE_COLOR_STYLEID,
                nameResourceId = WHITE_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.white_primary_color,
                primaryColorTextId = R.color.white_color_text_primary,
                secondaryColorId = R.color.white_secondary_color,
                backgroundColorId = R.color.white_background_color,
                outerElementColorId = R.color.white_outer_element_color
            )
            )
            var entries : ArrayList<Int> = ArrayList()
            for(r in 1..3) {
                for (g in 1..3) {
                    for (b in 1..3) {
                        val color = (0xff shl 24) or (r*(255/3) shl 16) or (g*(255/3) shl 8) or b*(255/3)
                        entries.add(color)
                    }
                }
            }
            entries = entries.distinct().toList() as ArrayList<Int>

            for (color in entries) {
                val displayName: String = Integer.toHexString(color)
                colorList.add(ColorStylesDynamic(
                    id = "${displayName}_style_id",
                    name = "${displayName.subSequence(2, displayName.length)}",
                    primaryColor = color,
                    primaryColorText = 0xffffffff.toInt(),
                    secondaryColor = darkenColor(color),
                    backgroundColor = 0xff000000.toInt(),
                    outerElementColor = 0x80FFFFFF.toInt()
                )
                )
            }

            colorList.add(ColorStylesDynamic(
                id = AMBIENT_COLOR_STYLEID,
                nameResourceId = AMBIENT_COLOR_STYLE_NAME_RESOURCE_ID,
                primaryColorId = R.color.ambient_primary_color,
                primaryColorTextId = R.color.ambient_primary_text_color,
                secondaryColorId = R.color.ambient_secondary_color,
                backgroundColorId = R.color.ambient_background_color,
                outerElementColorId = R.color.ambient_outer_element_color
            )
            )
            return colorList
        }

        fun getColorStyleConfig(colorId: String): ColorStylesDynamic {
            return instance.find { colorId == it.id }!!
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

        fun getBitmap(context: Context, name: String, color1: Int, color2: Int): Bitmap
        {
            val bitmap: Bitmap = Bitmap.createBitmap(BITMAP_SIZE.toInt(), BITMAP_SIZE.toInt(), Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            stylePainter.color = (color1)
            //canvas.drawCircle(0f, 0f, 24f, stylePainter)
            canvas.drawArc(0f, 0f, BITMAP_SIZE, BITMAP_SIZE, 90f, 180f, false, stylePainter)
            stylePainter.color = (color2)
            canvas.drawArc(0f, 0f, BITMAP_SIZE, BITMAP_SIZE, -90f, 180f, false, stylePainter)

            val nameString: String = name

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
        fun toOptionList(context: Context): List<UserStyleSetting.ListUserStyleSetting.ListOption> {
            val colorStyleId = instance

            return colorStyleId.map { colorStylesDynamic ->
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id(colorStylesDynamic.id),
                    context.resources,
                    if ( colorStylesDynamic.nameResourceId == 0 ) {BLUE_COLOR_STYLE_NAME_RESOURCE_ID} else {colorStylesDynamic.nameResourceId},
                    Icon.createWithBitmap(
                        getBitmap(
                            context,
                            colorStylesDynamic.getName(context),
                            colorStylesDynamic.getPrimaryColor(context),
                            colorStylesDynamic.getSecondaryColor(context)
                        )
                    )
                )
            }
        }
    }


}
