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

import android.support.v7.widget.RecyclerView;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-17
 *
 * Listener-helper for {@linkplain RecyclerView recycler view} which can {@linkplain ArrayImageLoader#pause() pause ArrayImageLoader's tasks}
 * while recycler view is scrolling (touch scrolling and/or fling). It prevents redundant loadings.
 */
public class RecyclerViewPauseOnScrollListener extends RecyclerView.OnScrollListener{

    private ArrayImageLoader imageLoader;

    private final boolean pauseOnScroll;
    private final boolean pauseOnFling;

    public RecyclerViewPauseOnScrollListener(ArrayImageLoader imageLoader, boolean pauseOnScroll, boolean pauseOnFling) {
        this.imageLoader = imageLoader;
        this.pauseOnScroll = pauseOnScroll;
        this.pauseOnFling = pauseOnFling;
    }

    public RecyclerViewPauseOnScrollListener(ArrayImageLoader imageLoader) {
        this(imageLoader, false, true);
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if(!pauseOnFling && !pauseOnScroll) {
            return;
        }
        switch (newState) {
            case RecyclerView.SCROLL_STATE_IDLE:
                imageLoader.resume();
                break;
            case RecyclerView.SCROLL_STATE_DRAGGING:
                if (pauseOnScroll) {
                    imageLoader.pause();
                }else {
                    imageLoader.resume();
                }
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                if (pauseOnFling) {
                    imageLoader.pause();
                }else {
                    imageLoader.resume();
                }
                break;
        }
    }
}
