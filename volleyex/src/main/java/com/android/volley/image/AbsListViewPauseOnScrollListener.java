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

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-17
 *
 * Listener-helper for {@linkplain AbsListView list views} ({@link android.widget.ListView}, {@link android.widget.GridView}) which can
 * {@linkplain ArrayImageLoader#pause() pause ArrayImageLoader's tasks} while list view is scrolling (touch scrolling and/or
 * fling). It prevents redundant loadings.<br />
 * Set it to your list view's {@link AbsListView#setOnScrollListener(OnScrollListener) setOnScrollListener(...)}.<br />
 * This listener can wrap your custom {@linkplain OnScrollListener listener}.
 */
public class AbsListViewPauseOnScrollListener implements OnScrollListener {

    private ArrayImageLoader imageLoader;

    private final boolean pauseOnScroll;
    private final boolean pauseOnFling;
    private final OnScrollListener externalListener;

    /**
     * Constructor
     *
     * @param imageLoader    {@linkplain ArrayImageLoader} instance for controlling
     * @param pauseOnScroll  Whether {@linkplain ArrayImageLoader#pause() pause ImageLoader} during touch scrolling
     * @param pauseOnFling   Whether {@linkplain ArrayImageLoader#pause() pause ImageLoader} during fling
     * @param customListener Your custom {@link OnScrollListener} for {@linkplain AbsListView list view} which also
     *                       will be get scroll events
     */
    public AbsListViewPauseOnScrollListener(ArrayImageLoader imageLoader, boolean pauseOnScroll,
        boolean pauseOnFling,
        OnScrollListener customListener) {
        this.imageLoader = imageLoader;
        this.pauseOnScroll = pauseOnScroll;
        this.pauseOnFling = pauseOnFling;
        externalListener = customListener;
    }

    public AbsListViewPauseOnScrollListener(ArrayImageLoader imageLoader, OnScrollListener customListener) {
        this(imageLoader, false, true, customListener);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(!pauseOnFling && !pauseOnScroll) {
            return;
        }
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                imageLoader.resume();
                break;
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                if (pauseOnScroll) {
                    imageLoader.pause();
                }else {
                    imageLoader.resume();
                }
                break;
            case OnScrollListener.SCROLL_STATE_FLING:
                if (pauseOnFling) {
                    imageLoader.pause();
                }else {
                    imageLoader.resume();
                }
                break;
        }
        if (externalListener != null) {
            externalListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
        int totalItemCount) {
        if (externalListener != null) {
            externalListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }
}
