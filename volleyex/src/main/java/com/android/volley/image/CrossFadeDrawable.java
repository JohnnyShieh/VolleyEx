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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.widget.ImageView;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-11
 *
 * An extension of BitmapDrawables that is intended to cross-fade from the
 * previous drawable to current bitmap. When this drawable is set to a imageView,
 * the imageView automatically starts to cross-fade between previous drawable and
 * current drawable. The cross-fade transition can run only one time, the previous
 * drawable is set to null after the transition.
 *
 * If the drawables have the same size, the another choice is
 * {@link android.graphics.drawable.TransitionDrawable}. TransitionDrawable also can be
 * defined in an XML file. But when the drawables have the different size,
 * TransitionDrawable may have a poor showing. Because {@link ImageView} use scale matrix
 * to draw its drawable, the scale matrix is decided by its width-height, width-height of
 * drawable and {@link ImageView.ScaleType}. TransitionDrawable uses the max width and
 * height of the layers as its width-height, so each layer may show stretched by the
 * improper scale matrix.
 *
 */
public class CrossFadeDrawable extends BitmapDrawable{

    /** The default cross-fade duration, the value is 300 ms. */
    private static final int DEFAULT_FADE_DURATION = 300;

    /**
     * A transition is about to start.
     */
    private static final int TRANSITION_STARTING = 0;

    /**
     * The transition has started and the animation is in progress
     */
    private static final int TRANSITION_RUNNING = 1;

    /**
     * The transition has ended.
     */
    private static final int TRANSITION_DONE = 2;

    /**
     * The current state of the transition. One of {@link #TRANSITION_STARTING},
     * {@link #TRANSITION_RUNNING} and {@link #TRANSITION_DONE}
     */
    private int mTransitionState = TRANSITION_STARTING;

    /** The widget used to show the drawable. */
    private final ImageView mImageView;

    /** The drawable which the imageView displays before. */
    private Drawable mPreviousDrawable;

    /** The matrix applied to the previous drawable when it is drawn. */
    private Matrix mPreviousDrawMatrix;

    private long mStartTimeMillis;
    private int mDuration;

    public static void setBitmap(ImageView imageView, Bitmap bitmap, int duration) {
        imageView.setImageDrawable(new CrossFadeDrawable(imageView, bitmap, duration));
    }

    public static void setBitmap(ImageView imageView, Bitmap bitmap) {
        setBitmap(imageView, bitmap, DEFAULT_FADE_DURATION);
    }

    private CrossFadeDrawable(ImageView imageView, Bitmap bitmap, int duration) {
        super(imageView.getResources(), bitmap);
        mImageView = imageView;
        mPreviousDrawable = imageView.getDrawable();
        mPreviousDrawMatrix = new Matrix(imageView.getImageMatrix());
        mDuration = duration;
    }

    @Override
    public void draw(Canvas canvas) {
        boolean done = true;
        final int alpha = getAlpha();
        int partialAlpha = alpha;

        switch (mTransitionState) {
            case TRANSITION_STARTING:
                mStartTimeMillis = SystemClock.uptimeMillis();
                done = false;
                partialAlpha = 0;
                mTransitionState = TRANSITION_RUNNING;
                break;

            case TRANSITION_RUNNING:
                if(mStartTimeMillis >= 0) {
                    if(mDuration <= 0) {
                        // skip to the end.
                        done = true;
                    }else {
                        float normalized = (float)
                            (SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;
                        done = normalized >= 1.0f;
                        normalized = Math.min(normalized, 1.0f);
                        partialAlpha = (int) (alpha * normalized);
                    }

                    if(done) {
                        mTransitionState = TRANSITION_DONE;
                        mPreviousDrawable = null;
                    }
                }
                break;
        }

        if(done) {
            // Just normally display the current drawable when cross fade transition is end.
            super.draw(canvas);
            return;
        }

        if(null != mPreviousDrawable) {
            mPreviousDrawable.setAlpha(alpha - partialAlpha);
            canvas.save();
            if(!mImageView.getImageMatrix().isIdentity()) {
                Matrix inverseMatrix = new Matrix();
                boolean inverted = mImageView.getImageMatrix().invert(inverseMatrix);
                if(inverted) {
                    // restore the canvas to the time when the imageView only display the previous drawable.
                    canvas.concat(inverseMatrix);
                }
            }
            canvas.concat(mPreviousDrawMatrix);
            mPreviousDrawable.draw(canvas);
            canvas.restore();
            mPreviousDrawable.setAlpha(alpha);
        }

        if(partialAlpha > 0) {
            setAlpha(partialAlpha);
            super.draw(canvas);
            setAlpha(alpha);
        }
        invalidateSelf();
    }

}
