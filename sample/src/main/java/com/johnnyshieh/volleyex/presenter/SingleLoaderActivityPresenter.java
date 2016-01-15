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
import com.android.volley.image.SingleImageLoader;
import com.johnnyshieh.volleyex.Constant;
import com.johnnyshieh.volleyex.R;

import android.app.Activity;
import android.widget.Button;
import android.widget.ImageView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Johnny Shieh
 * @version 1.0
 */
public class SingleLoaderActivityPresenter implements Presenter {

    @Bind(R.id.load_btn) Button mLoadBtn;
    @Bind(R.id.imageView) ImageView mImageView;

    private Activity mActivity;
    private SingleImageLoader mImageLoader;

    public SingleLoaderActivityPresenter(Activity activity) {
        mActivity = activity;
        ButterKnife.bind(this, activity);
        mImageLoader = new SingleImageLoader(VolleyEx.getRequestQueue(), mImageView, null);
        mImageLoader.setCrossFadeEnabled(true);
    }

    @Override
    public void onCreate() {
        mImageLoader.load("drawable://" + R.drawable.ic_launcher);
    }

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    public void onDestroy() {
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.load_btn)
    public void clickLoadBtn() {
        int index = (int) (Math.random() * Constant.imageUrls.length);
        mImageLoader.load(Constant.imageUrls[index]);
    }
}
