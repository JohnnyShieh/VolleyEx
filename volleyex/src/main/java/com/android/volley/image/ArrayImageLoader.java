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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-02
 *
 * Helper that handles an array of request from remote URLs
 */
public class ArrayImageLoader {

    /**
     * Interface for the ImageView handlers on image requests.
     */
    public interface LoadListener extends Response.ErrorListener, Request.FinishListener {
        /** Called when a bitmap  is received. */
        void onSuccess(String requestUrl, Bitmap bitmap);
    }

    public static final String TAG = "ArrayImageLoader";

    /** The default decode config for bitmap. */
    private static final Bitmap.Config DEFAULT_DECODE_CONFIG = Bitmap.Config.RGB_565;

    /** The default Time to live of image cache. */
    private static final long DEFAULT_IMAGE_TTL = 1 * 24 * 3600 * 1000;

    /** The default memory cache factor of max memory. */
    private static final float DEFAULT_MEMORY_FACTOR = 0.125f;

    private static final int ULR_TAG_KEY = TAG.hashCode();

    private static final int POSITION_TAG_KEY = TAG.hashCode() + 1;

    /** RequestQueue for dispatching ImageRequests onto. */
    private final RequestQueue mRequestQueue;

    /** Listener interface for the loaded bitmap. */
    private final LoadListener mLoadListener;

    /** Memory cache for the loaded bitmap. */
    private LruCache<String, Bitmap> mMemoryCache;

    /** The array stores the value whether the position is loaded or not. */
    protected SparseBooleanArray mLoadedArray = new SparseBooleanArray();

    /** The queue of image requests currently being processed by this RequestQueue. */
    private final ImageRequestMap mCurrentRequestMap;

    /** The queue of image requests waiting to process. */
    private final ImageRequestMap mWaitingRequestMap = new ImageRequestMap(10);

    private boolean mPaused = false;

    /** Decode config for loaded bitmap, default is {@code #Bitmap.Config.RGB_565}. */
    private Bitmap.Config mDecodeConfig = DEFAULT_DECODE_CONFIG;

    /** The default time to live of image cache, Default is 24 hours. */
    private long mDefaultTTL = DEFAULT_IMAGE_TTL;

    /** Whether enable or disable the cross fade of the drawables, Cross fade is disabled by default. */
    private boolean mCrossFade = true;

    /** The default drawable resource Id before bitmap loaded. */
    private int mDefaultResId = 0;

    /**
     * Constructs a new ArrayImageLoader.
     * @param requestQueue The RequestQueue to use for making image requests.
     * @param loadListener The listener interface for the async loaded bitmap.
     * @param memoryCacheFactor The memory cache factor of the max app memory.
     * @param defaultResId The default drawable resource ID displayed when bitmap not loaded.
     */
    public ArrayImageLoader(RequestQueue requestQueue, LoadListener loadListener, float memoryCacheFactor, int defaultResId) {
        mRequestQueue = requestQueue;
        mLoadListener = loadListener;
        mDefaultResId = defaultResId;
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        mMemoryCache = new LruCache<String, Bitmap>((int) (maxMemory * memoryCacheFactor)){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return (value.getRowBytes() * value.getHeight());
            }
        };
        mCurrentRequestMap = new ImageRequestMap();
    }

    /** Constructs a new ArrayImageLoader with default memory factor and request count. */
    public ArrayImageLoader(RequestQueue requestQueue, LoadListener loadListener) {
        this(requestQueue, loadListener, DEFAULT_MEMORY_FACTOR, 0);
    }

    /** Return the bitmap decode config. */
    public Bitmap.Config getDecodeConfig() {
        return mDecodeConfig;
    }

    /** Set the bitmap decode config, default is {@code #Bitmap.Config.RGB_565}*/
    public void setDecodeConfig(Bitmap.Config decodeConfig) {
        mDecodeConfig = decodeConfig;
    }

    /** Set the image cache default time to live. */
    public void setDefaultImageTTL(long ttl) {
        mDefaultTTL = ttl;
    }

    /**
     * When {@link ImageView} has fixed width and height, cross-fade transition behaves nice.
     * If {@link android.view.ViewGroup.LayoutParams} is {@code WRAP_CONTENT}, you'd better not use it.
     * @param enabled True to enable cross fading, false otherwise.
     */
    public void setCrossFadeEnabled(boolean enabled) {
        mCrossFade = enabled;
    }

    /**
     * Lode the bitmap from memory cache or the requestUrl.
     * If the memory cache exist the bitmap, just return it.
     * Else loading request from remotes URL, and return null.
     * */
    public void loadBitmap(ImageView imageView, String requestUrl, int maxWidth, int maxHeight, int position) {
        if(!mLoadedArray.get(position, false)) {
            // If this position has not loaded, then set the default resource.
            if(0 != mDefaultResId) {
                imageView.setImageResource(mDefaultResId);
            }else {
                imageView.setImageDrawable(null);
            }
        }
        // Ignore it if requestUrl is empty.
        if(TextUtils.isEmpty(requestUrl)) {
            return;
        }
        imageView.setTag(ULR_TAG_KEY, requestUrl);
        imageView.setTag(POSITION_TAG_KEY, position);

        // Firstly find at the memory cache.
        Bitmap cache = mMemoryCache.get(requestUrl);
        if(null != cache) {
            if(mCrossFade && !mLoadedArray.get(position, false)) {
                CrossFadeDrawable.setBitmap(imageView, cache);
            }else {
                imageView.setImageBitmap(cache);
            }
            return;
        }

        // Then make a image request
        ImageRequest imageRequest = new ImageRequest(requestUrl, new ResponseListener(imageView), maxWidth, maxHeight, imageView.getScaleType(), mDecodeConfig, mLoadListener);
        imageRequest.setFinishListener(mLoadListener);
        imageRequest.setDefaultSoftTtl(mDefaultTTL);
        imageRequest.setDefaultTtl(mDefaultTTL);

        if(mPaused) {
            mWaitingRequestMap.put(imageView, imageRequest);
        }else {
            mCurrentRequestMap.put(imageView, imageRequest);
            mRequestQueue.add(imageRequest);
        }
    }

    /** Avoid the redundant loadings. */
    public void pause() {
        mPaused = true;
    }

    public synchronized void resume() {
        if(!mPaused || mWaitingRequestMap.size() == 0) {
            return;
        }
        if(VolleyLog.DEBUG) {
            Log.d(TAG, "resume the waiting tasks.");
        }
        for(Entry<ImageView, ImageRequest> entry : mWaitingRequestMap.entrySet()) {
            mCurrentRequestMap.put(entry.getKey(), entry.getValue());
            mRequestQueue.add(entry.getValue());
            if(VolleyLog.DEBUG) {
                Log.d(TAG, "resume the task which position is " + entry.getKey().getTag(POSITION_TAG_KEY).toString());
            }
        }
        mWaitingRequestMap.clear();
        mPaused = false;
    }

    /** Clear the loading request and memory cache. */
    public void destroy() {
        mCurrentRequestMap.evictAll();
        mMemoryCache.evictAll();
        mLoadedArray.clear();
    }

    private class ResponseListener implements Response.Listener<Bitmap> {

        private ImageView imageView;

        public ResponseListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public void onResponse(String requestUrl, Bitmap bitmap) {
            if(null == mMemoryCache.get(requestUrl)) {
                mMemoryCache.put(requestUrl, bitmap);
            }
            if(TextUtils.equals(requestUrl, (CharSequence) imageView.getTag(ULR_TAG_KEY)) && imageView.isShown() && null != bitmap) {
                if(mCrossFade) {
                    CrossFadeDrawable.setBitmap(imageView, bitmap);
                }else {
                    imageView.setImageBitmap(bitmap);
                }
                int position = (int) imageView.getTag(POSITION_TAG_KEY);
                mLoadedArray.put(position, true);
            }
            if(null != mLoadListener) {
                mLoadListener.onSuccess(requestUrl, bitmap);
            }
        }
    }
}
