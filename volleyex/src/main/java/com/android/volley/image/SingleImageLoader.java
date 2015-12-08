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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-02
 *
 * Helper that handles loading images from remote URLs and update the ImageView.
 */
public class SingleImageLoader implements Response.Listener<Bitmap>{

    /** The default decode config for bitmap. */
    private static final Bitmap.Config DEFAULT_DECODE_CONFIG = Bitmap.Config.RGB_565;

    /** The default Time to live of image cache. */
    private static final long DEFAULT_IMAGE_TTL = 1 * 24 * 3600 * 1000;

    /** RequestQueue for dispatching ImageRequests onto. */
    private final RequestQueue mRequestQueue;

    /** ImageView for display the image. */
    private final ImageView mImageView;

    /** The request image url. */
    private String mRequestUrl;

    /** The current request loading images. */
    private ImageRequest mImageRequest;

    /** The default time to live of image cache, Default is 24 hours. */
    private long mDefaultTTL = DEFAULT_IMAGE_TTL;

    /** The listener for the process of loading image. */
    private final LoadListener mLoadListener;

    public SingleImageLoader(RequestQueue requestQueue, ImageView imageView) {
        this(requestQueue, imageView, null);
    }

    public SingleImageLoader(RequestQueue requestQueue, ImageView imageView, LoadListener loadListener) {
        mRequestQueue = requestQueue;
        mImageView = imageView;
        mLoadListener = loadListener;
    }

    /** Set the image cache default time to live. */
    public void setDefaultImageTTL(long ttl) {
        mDefaultTTL = ttl;
    }

    /** Return the animation for ImageView display the image. */
    private Animation getBitmapAnimation() {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        return animation;
    }

    /** Called when a response is received. */
    @Override
    public void onResponse(String requestUrl, Bitmap bitmap) {
        if(null != bitmap) {
            mImageView.setImageBitmap(bitmap);
            mImageView.startAnimation(getBitmapAnimation());
        }
        if(null != mLoadListener) {
            mLoadListener.onLoadEnd(bitmap);
        }
    }

    /**
     * Load and display image from request URL.
     *
     * @param requestUrl The request image url.
     * @param maxWidth The maxWidth of bitmap.
     * @param maxHeight The maxHeight of bitmap.
     * @param scaleType The scaleType of imageView.
     * @param config The bitmap decode config.
     */
    public void load(String requestUrl, int maxWidth, int maxHeight, ImageView.ScaleType scaleType, Bitmap.Config config) {
        // Ignore it if requestUrl is empty.
        if(TextUtils.isEmpty(requestUrl)) {
            return;
        }
        // Ignore it if last same request is not finished.
        if(TextUtils.equals(requestUrl, mRequestUrl) && null != mImageRequest && !mImageRequest.hasHadResponseDelivered()) {
            return;
        }
        if(null != mImageRequest) {
            mImageRequest.cancel();
        }

        mRequestUrl = requestUrl;
        mImageRequest = new ImageRequest(requestUrl, this, maxWidth, maxHeight,
            mImageView.getScaleType(), config, mLoadListener);
        mImageRequest.setDefaultSoftTtl(mDefaultTTL);
        mImageRequest.setDefaultTtl(mDefaultTTL);

        if(null != mLoadListener) {
            mLoadListener.onLoadStart();
        }
        mRequestQueue.add(mImageRequest);
    }

    /**
     * Equivalent to calling {@link #load(String, int, int, ImageView.ScaleType, Bitmap.Config)} with
     * {@code scaleType == mImageView.getScaleType()}, {@code Bitmap.Config == Bitmap.Config.RGB_565}.
     */
    public void load(String requestUrl, int maxWidth, int maxHeight) {
        load(requestUrl, maxWidth, maxHeight, mImageView.getScaleType(), DEFAULT_DECODE_CONFIG);
    }

    /**
     * Equivalent to calling {@link #load(String, int, int, ImageView.ScaleType, Bitmap.Config)} with
     * {@code maxWidth == mImageView.getMeasuredWidth()}, {@code maxHeight == mImageView.getMeasuredHeight()},
     * {@code scaleType == mImageView.getScaleType()}, {@code Bitmap.Config == Bitmap.Config.RGB_565}.
     */
    public void load(String requestUrl) {
        load(requestUrl, mImageView.getMeasuredWidth(), mImageView.getMeasuredHeight(), mImageView.getScaleType(), DEFAULT_DECODE_CONFIG);
    }

    /**
     * Interface for the ImageView handlers on image requests.
     */
    public interface LoadListener extends Response.ErrorListener {
        /** Listens for the time before the loading of the image request. */
        void onLoadStart();

        /** Listens for the time after the loading of the image request. */
        void onLoadEnd(Bitmap bitmap);
    }
}
