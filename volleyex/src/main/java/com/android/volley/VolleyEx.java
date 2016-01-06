package com.android.volley;
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

import com.android.volley.cache.DiskLruBasedCache;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Environment;

import java.io.File;

/**
 * @author: Johnny Shieh
 * @date: 2015-11-30
 */
public class VolleyEx {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volleyex";

    private static RequestQueue mRequestQueue;

    public static synchronized void initialize(Context context) {
        if(null == mRequestQueue) {
            mRequestQueue = newRequestQueue(context);
        }
    }

    public static synchronized void initialize(Context context, int maxDiskCacheBytes) {
        if(null == mRequestQueue) {
            mRequestQueue = newRequestQueue(context, maxDiskCacheBytes);
        }
    }

    public static synchronized void initialize(Context context, File cacheDir, int maxDiskCacheBytes) {
        if(null == mRequestQueue) {
            mRequestQueue = newRequestQueue(context, cacheDir, maxDiskCacheBytes);
        }
    }

    public static synchronized void initialize(Context context, HttpStack stack, File cacheDir, int maxDiskCacheBytes) {
        if(null == mRequestQueue) {
            mRequestQueue = newRequestQueue(context, stack, cacheDir, maxDiskCacheBytes);
        }
    }

    public static RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public static void destroyRequestQueue() {
        if(null != mRequestQueue) {
            mRequestQueue.destroy();
            mRequestQueue = null;
        }
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     * You may set a maximum size of the disk cache in bytes.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @param cacheDir the cache directory of the disk cache, or null for default.
     * @param maxDiskCacheBytes the maximum size of the disk cache, in bytes. Use -1 for default size.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, File cacheDir, int maxDiskCacheBytes) {
        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);

        if(null == cacheDir) {
            String path;
            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
                path = context.getExternalCacheDir().getPath();
            }else {
                path = context.getCacheDir().getPath();
            }
            cacheDir = new File(path + File.separator + DEFAULT_CACHE_DIR);
        }

        int appVersion = 1;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        RequestQueue queue;
        if(maxDiskCacheBytes <= -1) {
            // No maximum size specified
            queue = new RequestQueue(context, new DiskLruBasedCache(cacheDir, appVersion), network);
        }else {
            queue = new RequestQueue(context, new DiskLruBasedCache(cacheDir, maxDiskCacheBytes, appVersion), network);
        }

        queue.start();

        return queue;
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     * You may set a maximum size of the disk cache in bytes.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param cacheDir the cache directory of the cache.
     * @param maxDiskCacheBytes the maximum size of the disk cache, in bytes. Use -1 for default size.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, File cacheDir, int maxDiskCacheBytes) {
        return newRequestQueue(context, null, cacheDir, maxDiskCacheBytes);
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     * You may set a maximum size of the disk cache in bytes.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param maxDiskCacheBytes the maximum size of the disk cache, in bytes. Use -1 for default size.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, int maxDiskCacheBytes) {
        return newRequestQueue(context, null, maxDiskCacheBytes);
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, -1);
    }

}
