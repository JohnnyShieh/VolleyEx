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
import com.android.volley.toolbox.ImageRequest;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.ImageView;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-02
 *
 * Helper that handles an array of request from remote URLs
 */
public class ArrayImageLoader {

    /** Callback interface for delivering parsed bitmap. */
    public interface SuccessListener {
        /** Called when a bitmap  is received. */
        public void onSuccess(String requestUrl, Bitmap bitmap);
    }

    /** The default decode config for bitmap. */
    private static final Bitmap.Config DEFAULT_DECODE_CONFIG = Bitmap.Config.RGB_565;

    /** The default Time to live of image cache. */
    private static final long DEFAULT_IMAGE_TTL = 1 * 24 * 3600 * 1000;

    /** The default memory cache factor of max memory. */
    private static final float DEFAULT_MEMORY_FACTOR = 0.125f;

    /** The default max request count. */
    private static final int DEFAULT_REQUEST_COUNT = 10;

    /** RequestQueue for dispatching ImageRequests onto. */
    private final RequestQueue mRequestQueue;

    /** Listener interface for the loaded bitmap. */
    private final SuccessListener mSuccessListener;

    /** Listener interface for errors. */
    private final Response.ErrorListener mErrorListener;

    /** Memory cache for the loaded bitmap. */
    private LruCache<String, Bitmap> mMemoryCache;

    /** The queue of image requests currently being processed by this RequestQueue. */
    private ImageRequestList mCurrentRequestList;

    /** Decode config for loaded bitmap, default is {@code #Bitmap.Config.RGB_565}. */
    private Bitmap.Config mDecodeConfig = DEFAULT_DECODE_CONFIG;

    /** The default time to live of image cache, Default is 24 hours. */
    private long mDefaultTTL = DEFAULT_IMAGE_TTL;

    private Response.Listener<Bitmap> mLoadedListener = new Response.Listener<Bitmap>() {
        @Override
        public void onResponse(String requestUrl, Bitmap bitmap) {
            if(null == mMemoryCache.get(requestUrl)) {
                mMemoryCache.put(requestUrl, bitmap);
            }
            mSuccessListener.onSuccess(requestUrl, bitmap);
        }
    };

    /** Listener interface for the time request is finished. */
    private Request.FinishListener mFinishListener = new Request.FinishListener() {
        @Override
        public void onFinish(Request request) {
            // If the request is normally finished, not cancel by evicted from the queue.
            if(!request.isCanceled()) {
                mCurrentRequestList.remove(request.getOriginUrl());
            }
        }
    };

    /**
     * Constructs a new ArrayImageLoader.
     * @param requestQueue The RequestQueue to use for making image requests.
     * @param successListener The listener interface for the async loaded bitmap.
     * @param errorListener The listener interface for loading errors.
     * @param memoryCacheFactor The memory cache factor of the max app memory.
     * @param maxRequestCount The max count of request queue.
     */
    public ArrayImageLoader(RequestQueue requestQueue, SuccessListener successListener, Response.ErrorListener errorListener, float memoryCacheFactor, int maxRequestCount) {
        mRequestQueue = requestQueue;
        mSuccessListener = successListener;
        mErrorListener = errorListener;
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        mMemoryCache = new LruCache<String, Bitmap>((int) (maxMemory * memoryCacheFactor)){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mCurrentRequestList = new ImageRequestList(maxRequestCount);
    }

    /** Constructs a new ArrayImageLoader with default memory factor and request count. */
    public ArrayImageLoader(RequestQueue requestQueue, SuccessListener successListener, Response.ErrorListener errorListener) {
        this(requestQueue, successListener, errorListener, DEFAULT_MEMORY_FACTOR, DEFAULT_REQUEST_COUNT);
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
     * Lode the bitmap from memory cache or the requestUrl.
     * If the memory cache exist the bitmap, just return it.
     * Else loading request from remotes URL, and return null.
     * */
    public Bitmap loadBitmap(String requestUrl, int maxWidth, int maxHeight, ImageView.ScaleType scaleType) {
        // Ignore it if requestUrl is empty.
        if(TextUtils.isEmpty(requestUrl)) {
            return null;
        }

        // Firstly find at the memory cache.
        Bitmap cache = mMemoryCache.get(requestUrl);
        if(null != cache) {
            return cache;
        }

        // Then make a image request
        ImageRequest imageRequest = new ImageRequest(requestUrl, mLoadedListener, maxWidth, maxHeight, scaleType, mDecodeConfig, mErrorListener);
        imageRequest.setFinishListener(mFinishListener);
        imageRequest.setDefaultSoftTtl(mDefaultTTL);
        imageRequest.setDefaultTtl(mDefaultTTL);

        ImageRequest previous = mCurrentRequestList.add(requestUrl, imageRequest);
        // If there exists a previous same requestUrl request, do not loading again.
        if(null == previous) {
            mRequestQueue.add(imageRequest);
        }
        return null;
    }

    /** Clear the loading request and memory cache. */
    public void destroy() {
        mCurrentRequestList.evictAll();
        mMemoryCache.evictAll();
    }

    /**
     * RequestList is a fixed-length queue, All optional operations including add, remove element.
     * It maintains an array of image request, use request url as the key.
     *
     * There has the following operations:
     * 1. A new image request is added to the queue,
     *      (1) it is exist, just move it to the head of queue.
     *      (2) it is not exist, put it to the head of queue, if size is greater than the max size, remove and cancel the eldest request.
     * 2. A exist image request is finished, so remove it from the queue.
     */
    private class ImageRequestList extends LruCache<String, ImageRequest> {

        public ImageRequestList(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, ImageRequest value) {
            return 1;
        }

        /**
         * Add a new request to the queue.
         * Note: you should not call put(K key, V value) method, it doesn't handle the same request url.
         *
         * @return the previous value mapped by {@code requestUrl}.
         */
        public ImageRequest add(String requestUrl, ImageRequest request) {
            ImageRequest previous = get(requestUrl);
            // there has a same request, just return it.
            if(null != previous) {
                return previous;
            }

            return put(requestUrl, request);
        }

        /** Cancel the request when it is evicted. */
        @Override
        protected void entryRemoved(boolean evicted, String key, ImageRequest oldValue,
            ImageRequest newValue) {
            // Do not loading request that was evicted from the queue
            if(evicted && null != oldValue) {
                oldValue.cancel();
            }
        }
    }
}
