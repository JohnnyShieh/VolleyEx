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

import com.johnnyshieh.volleyex.R;
import com.johnnyshieh.volleyex.view.adapter.SampleListAdapter;
import com.johnnyshieh.volleyex.view.component.DividerItemDecoration;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Johnny Shieh
 * @version 1.0
 */
public class MainActivityPresenter implements Presenter {

    private Activity mActivity;

    @Bind(R.id.recycler_view) RecyclerView mRecyclerView;

    public MainActivityPresenter(Activity activity) {
        mActivity = activity;
        ButterKnife.bind(this, activity);
    }

    @Override
    public void onCreate() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mRecyclerView.getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext()));
        mRecyclerView.setAdapter(new SampleListAdapter(mActivity));
    }

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    public void onDestroy() {
        ButterKnife.unbind(this);
    }
}
