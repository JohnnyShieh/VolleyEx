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

import com.android.volley.request.ImageRequest;

import android.widget.ImageView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Johnny Shieh
 * @version  1.0
 *
 *
 */
public class ImageRequestMap {

    private final HashMap<ImageView, ImageRequest> map;

    public ImageRequestMap() {
        map = new HashMap<>();
    }

    public ImageRequestMap(int capacity) {
        map = new HashMap<>(capacity);
    }

    public ImageRequest get(ImageView key) {
        return map.get(key);
    }

    public ImageRequest put(ImageView key, ImageRequest value) {
        ImageRequest previous = map.put(key, value);
        if(null != previous) {
            entryRemoved(previous);
        }
        return previous;
    }

    protected void entryRemoved(ImageRequest oldValue) {
        oldValue.cancel();
    }

    public void clear() {
        map.clear();
    }

    public synchronized void evictAll() {
        Iterator<ImageRequest> iterator = map.values().iterator();
        while (iterator.hasNext()){
            entryRemoved(iterator.next());
        }
        map.clear();
    }

    public Set<Entry<ImageView, ImageRequest>> entrySet() {
        return map.entrySet();
    }

    public final int size() {
        return map.size();
    }

}
