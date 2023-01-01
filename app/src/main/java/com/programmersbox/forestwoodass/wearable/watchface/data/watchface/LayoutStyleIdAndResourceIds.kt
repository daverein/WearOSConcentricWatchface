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
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import com.programmersbox.forestwoodass.wearable.watchface.R

// Defaults for all styles.
// X_COLOR_STYLE_ID - id in watch face database for each style id.
// X_COLOR_STYLE_NAME_RESOURCE_ID - String name to display in the user settings UI for the style.
// X_COLOR_STYLE_ICON_ID - Icon to display in the user settings UI for the style.
@Suppress("SpellCheckingInspection")
const val FULL_FACE_STYLE_ID = "fullface_style_id"
private const val FULL_FACE_STYLE_NAME_RESOURCE_ID = R.string.fullface_style_name
private const val FULL_FACE_STYLE_ICON_ID = R.drawable.fullface

@Suppress("SpellCheckingInspection")
const val HALF_FACE_STYLE_ID = "halfface_style_id"
private const val HALF_FACE_STYLE_NAME_RESOURCE_ID = R.string.halfface_style_name
private const val HALF_FACE_STYLE_ICON_ID = R.drawable.halfface

@Suppress("SpellCheckingInspection")
const val SCALED_HALF_FACE_STYLE_ID = "scaled_halfface_style_id"
private const val SCALED_HALF_FACE_STYLE_NAME_RESOURCE_ID = R.string.scaled_halfface_style_name
private const val SCALED_HALF_FACE_STYLE_ICON_ID = R.drawable.scaled_halfface

/**
 * Represents watch face color style options the user can select (includes the unique id, the
 * complication style resource id, and general watch face color style resource ids).
 *
 * The companion object offers helper functions to translate a unique string id to the correct enum
 * and convert all the resource ids to their correct resources (with the Context passed in). The
 * renderer will use these resources to render the actual colors and ComplicationDrawables of the
 * watch face.
 */
@Suppress("SpellCheckingInspection")
enum class LayoutStyleIdAndResourceIds(
    val id: String,
    @StringRes val nameResourceId: Int,
    @DrawableRes val iconResourceId: Int,
    @DrawableRes val complicationStyleDrawableId: Int,
) {

    FULLFACE (
        id = FULL_FACE_STYLE_ID,
        nameResourceId = FULL_FACE_STYLE_NAME_RESOURCE_ID,
        iconResourceId = FULL_FACE_STYLE_ICON_ID,
        complicationStyleDrawableId = R.drawable.fullface,
    ),


    HALFFACE(
        id = HALF_FACE_STYLE_ID,
        nameResourceId = HALF_FACE_STYLE_NAME_RESOURCE_ID,
        iconResourceId = HALF_FACE_STYLE_ICON_ID,
        complicationStyleDrawableId = R.drawable.halfface,
    ),

    SCALED_HALFFACE(
        id = SCALED_HALF_FACE_STYLE_ID,
        nameResourceId = SCALED_HALF_FACE_STYLE_NAME_RESOURCE_ID,
        iconResourceId = SCALED_HALF_FACE_STYLE_ICON_ID,
        complicationStyleDrawableId = R.drawable.scaled_halfface,
    );

    companion object {
        /**
         * Translates the string id to the correct ColorStyleIdAndResourceIds object.
         */
        fun getLayoutStyleConfig(id: String): LayoutStyleIdAndResourceIds {
            return when (id) {
                FULLFACE.id -> FULLFACE
                HALFFACE.id -> HALFFACE
                SCALED_HALFFACE.id -> SCALED_HALFFACE
                else -> FULLFACE
            }
        }

        /**
         * Returns a list of [UserStyleSetting.ListUserStyleSetting.ListOption] for all
         * ColorStyleIdAndResourceIds enums. The watch face settings APIs use this to set up
         * options for the user to select a style.
         */
        fun toOptionList(context: Context): List<ListUserStyleSetting.ListOption> {
            val layoutStyleIdAndResourceIdsList = enumValues<LayoutStyleIdAndResourceIds>()

            return layoutStyleIdAndResourceIdsList.map { layoutStyleIdAndResourceIds ->
                ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id(layoutStyleIdAndResourceIds.id),
                    context.resources,
                    layoutStyleIdAndResourceIds.nameResourceId,
                    Icon.createWithResource(
                        context,
                        layoutStyleIdAndResourceIds.iconResourceId
                    )
                )
            }
        }
    }
}
