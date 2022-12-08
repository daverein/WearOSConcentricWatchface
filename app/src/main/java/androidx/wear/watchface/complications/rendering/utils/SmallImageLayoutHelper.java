/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchface.complications.rendering.utils;

import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getCentralSquare;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getLeftPart;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.isWideRectangle;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.scaledAroundCenter;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.support.wearable.complications.ComplicationData;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Layout helper for {@link ComplicationData#TYPE_SMALL_IMAGE}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("RestrictedApi")
public class SmallImageLayoutHelper extends LayoutHelper {

    /** Used to apply padding to ranged value indicator. */
    private static final float RANGED_VALUE_SIZE_FRACTION = 0.95f;

    @Override
    public void getSmallImageBounds(@NonNull Rect outRect) {
        getBounds(outRect);
        getCentralSquare(outRect, outRect);
        scaledAroundCenter(outRect, outRect, 0.7f);
    }

    @Override
    public void getRangedValueBounds(@NonNull Rect outRect) {
        getBounds(outRect);
        ComplicationData data = getComplicationData();
        if ((data!= null && data.getShortText() == null) || !isWideRectangle(outRect)) {
            getCentralSquare(outRect, outRect);
            scaledAroundCenter(outRect, outRect, RANGED_VALUE_SIZE_FRACTION);
        } else {
            getLeftPart(outRect, outRect);
            scaledAroundCenter(outRect, outRect, RANGED_VALUE_SIZE_FRACTION);
        }
    }
}
