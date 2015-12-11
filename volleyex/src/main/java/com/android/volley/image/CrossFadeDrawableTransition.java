package com.android.volley.image;
/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-10
 *
 * Helper that use cross-fade {@link android.graphics.drawable.TransitionDrawable} to show
 * {@link android.widget.ImageView} drawable animation.
 */
public class CrossFadeDrawableTransition {

    /** Default transition duration is 300 ms.*/
    private static final int DEFAULT_TRANSITION_DURATION = 300;

    /** The transition duration, default is 300 ms.*/
    private final int mDuration;

    public CrossFadeDrawableTransition(int duration) {
        mDuration = duration;
    }

    public CrossFadeDrawableTransition() {
        this(DEFAULT_TRANSITION_DURATION);
    }

    /**
     * Cross fade from the previous drawable to the next bitmap.
     *
     * Note: The two drawable size had better be the same, otherwise the next drawable
     * will use the previous drawable matrix to scale, it seems strange.
     *
     * @param bitmap The next bitmap which the ImageView will show.
     * @param defaultImageId The default drawable ID when bitmap is null.
     * @param imageView The imageView show the cross-fade animation.
     */
    public void transition(Bitmap bitmap, int defaultImageId, ImageView imageView) {
        if(null == imageView) {
            throw new IllegalArgumentException("imageView can not be null!");
        }
        Drawable current = imageView.getDrawable();
        if(null == current && null == bitmap) {
            return;
        }

        Drawable previous;
        Drawable next;

        if(null == bitmap) {
            next = (0 != defaultImageId) ? imageView.getResources().getDrawable(defaultImageId, null) : new ColorDrawable(Color.TRANSPARENT);
        }else {
            next = new BitmapDrawable(imageView.getResources(), bitmap);
        }

        if(null == current) {
            previous = new ColorDrawable(Color.TRANSPARENT);
        }else if(current instanceof TransitionDrawable) {
            int num = ((TransitionDrawable) current).getNumberOfLayers();
            if(num <= 0) {
                previous = new ColorDrawable(Color.TRANSPARENT);
            }else {
                int index = (num >= 2) ? 1 : 0;
                previous = ((TransitionDrawable) current).getDrawable(index);
            }
        }else {
            previous = current;
        }

        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{previous, next});
        transitionDrawable.setCrossFadeEnabled(true);
        transitionDrawable.startTransition(mDuration);
        imageView.setImageDrawable(transitionDrawable);
    }

    /**
     * Equivalent to calling {@link #transition(Bitmap, int, ImageView)} with {@code defaultImageId == 0}.
     */
    public void transition(Bitmap bitmap, ImageView imageView) {
        transition(bitmap, 0, imageView);
    }

}
