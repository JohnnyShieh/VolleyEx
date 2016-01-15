package com.johnnyshieh.volleyex.presenter;
/*
 * Copyright (C) 2016 Johnny Shieh Open Source Project
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

import com.android.volley.VolleyEx;
import com.android.volley.image.ArrayImageLoader;
import com.android.volley.image.RecyclerViewPauseOnScrollListener;
import com.johnnyshieh.volleyex.Constant;
import com.johnnyshieh.volleyex.R;
import com.johnnyshieh.volleyex.view.adapter.ArrayListAdapter;

import android.app.Activity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * description
 *
 * @author Johnny Shieh
 * @version 1.0
 */
public class ArrayLoaderActivityPresenter implements Presenter{

    @Bind(R.id.recycler_view) RecyclerView mRecyclerView;

    private Activity mActivity;
    private ArrayListAdapter mAdapter;
    private ArrayImageLoader mArrayImageLoader;

    public ArrayLoaderActivityPresenter(Activity activity) {
        mActivity = activity;
        ButterKnife.bind(this, activity);
    }

    @Override
    public void onCreate() {
        mRecyclerView.setLayoutManager(new GridLayoutManager(mRecyclerView.getContext(), 2));
        mRecyclerView.setHasFixedSize(true);

        mArrayImageLoader = new ArrayImageLoader(VolleyEx.getRequestQueue(), null, 0.125f, R.color.color_unloaded);
        mAdapter = new ArrayListAdapter(mArrayImageLoader);
        mAdapter.setImageUrlList(Arrays.asList(Constant.imageUrls));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerViewPauseOnScrollListener(mArrayImageLoader));
    }

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    public void onDestroy() {
        mArrayImageLoader.destroy();
        ButterKnife.unbind(this);
    }
}
