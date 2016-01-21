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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.request.ImageRequest;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.ViewGroup;
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
    protected final RequestQueue mRequestQueue;

    /** ImageView for display the image. */
    protected final ImageView mImageView;

    /** The request image url. */
    protected String mRequestUrl;

    /** The current request loading images. */
    protected ImageRequest mImageRequest;

    /** The default time to live of image cache, Default is 24 hours. */
    protected long mDefaultTTL = DEFAULT_IMAGE_TTL;

    /** Whether enable or disable the cross fade of the drawables, Cross fade is disabled by default. */
    protected boolean mCrossFade = false;

    /** The listener for the process of loading image. */
    protected final LoadListener mLoadListener;

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

    /** Returns the request url. */
    public String getRequestUrl() {
        return mRequestUrl;
    }

    /**
     * When {@link ImageView} has fixed width and height, cross-fade transition behaves nice.
     * If {@link android.view.ViewGroup.LayoutParams} is {@code WRAP_CONTENT}, you'd better not use it.
     * @param enabled True to enable cross fading, false otherwise.
     */
    public void setCrossFadeEnabled(boolean enabled) {
        mCrossFade = enabled;
    }

    /** Called when a response is received. */
    @Override
    public void onResponse(String requestUrl, Bitmap bitmap) {
        if(!TextUtils.equals(mRequestUrl, requestUrl)) {
            return;
        }
        if(mCrossFade) {
            CrossFadeDrawable.setBitmap(mImageView, bitmap);
        }else {
            mImageView.setImageBitmap(bitmap);
        }
        if(null != mLoadListener) {
            mLoadListener.onSuccess(bitmap);
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
        mImageRequest.setFinishListener(mLoadListener);
        mImageRequest.setDefaultSoftTtl(mDefaultTTL);
        mImageRequest.setDefaultTtl(mDefaultTTL);

        if(null != mLoadListener) {
            mLoadListener.onStart();
        }
        mRequestQueue.add(mImageRequest);
    }

    /**
     * Equivalent to calling {@link #load(String, int, int, ImageView.ScaleType, Bitmap.Config)} with
     * {@code scaleType == mImageView.getScaleType()}
     */
    public void load(String requestUrl, int maxWidth, int maxHeight, Bitmap.Config config) {
        load(requestUrl, maxWidth, maxHeight, mImageView.getScaleType(), config);
    }

    /**
     * Equivalent to calling {@link #load(String, int, int, Bitmap.Config)} with
     * maxWidth == the width of imageView, maxHeight == the height of imageView.
     */
    public void load(String requestUrl, Bitmap.Config config) {
        int maxWidth = 0;
        int maxHeight = 0;
        ViewGroup.LayoutParams layoutParams = mImageView.getLayoutParams();
        if(null != layoutParams) {
            maxWidth = (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) ? 0 : mImageView.getMeasuredWidth();
            maxHeight = (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) ? 0 : mImageView.getMeasuredHeight();
        }
        load(requestUrl, maxWidth, maxHeight, config);
    }

    /**
     * Equivalent to calling {@link #load(String, Bitmap.Config)} with
     * {@code Bitmap.Config == Bitmap.Config.RGB_565}.
     */
    public void load(String requestUrl) {
        load(requestUrl, DEFAULT_DECODE_CONFIG);
    }

    /**
     * Interface for the ImageView handlers on image requests.
     */
    public interface LoadListener extends Response.ErrorListener, Request.FinishListener{
        /** Listens for the time before the loading of the image request. */
        void onStart();

        /** Listens for the time after response is received. */
        void onSuccess(Bitmap bitmap);
    }
}
