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

import java.util.Locale;

/**
 * @author Johnny Shieh
 * @version 1.0
 *
 * Represents supported schemes(protocols) of URI. Provides convenient methods for work with schemes and URIs.
 * This is almost copy from Android-Universal-Image-Loader.
 *
 * Examples:
 * "http://site.com/image.png" // from Web
 * "file:///mnt/sdcard/image.png" // from SD card
 * "file:///mnt/sdcard/video.mp4" // from SD card (video thumbnail)
 * "content://media/external/images/media/13" // from content provider
 * "content://media/external/video/media/13" // from content provider (video thumbnail)
 * "assets://image.png" // from assets
 * "drawable://" + R.drawable.img // from drawables (non-9patch images)
 */
public enum  ImageScheme {

    HTTP("http"), HTTPS("https"), FILE("file"), CONTENT("content"), ASSETS("assets"), DRAWABLE("drawable"), UNKNOWN("");

    private String scheme;
    private String uriPrefix;

    ImageScheme(String scheme) {
        this.scheme = scheme;
        uriPrefix = scheme + "://";
    }

    /**
     * Defines scheme of incoming URI
     *
     * @param uri URI for scheme detection
     * @return Scheme of incoming URI
     */
    public static ImageScheme ofUri(String uri) {
        if (uri != null) {
            for (ImageScheme s : values()) {
                if (s.belongsTo(uri)) {
                    return s;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Jude whether uri is HTTP|HTTPS scheme or not.
     */
    public static boolean isHttpScheme(String uri) {
        ImageScheme scheme = ofUri(uri);
        return scheme == HTTP || scheme == HTTPS;
    }

    private boolean belongsTo(String uri) {
        return uri.toLowerCase(Locale.US).startsWith(uriPrefix);
    }

    /** Appends scheme to incoming path */
    public String wrap(String path) {
        return uriPrefix + path;
    }

    /** Removed scheme part ("scheme://") from incoming URI */
    public String crop(String uri) {
        if (!belongsTo(uri)) {
            throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected scheme [%2$s]", uri, scheme));
        }
        return uri.substring(uriPrefix.length());
    }
}
