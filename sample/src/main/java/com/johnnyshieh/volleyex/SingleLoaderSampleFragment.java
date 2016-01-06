package com.johnnyshieh.volleyex;
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
import com.android.volley.VolleyError;
import com.android.volley.VolleyEx;
import com.android.volley.image.SingleImageLoader;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-07
 */
public class SingleLoaderSampleFragment extends Fragment {

    @Bind(R.id.load_btn) Button mLoadBtn;

    @Bind(R.id.imageView) ImageView mImageView1;

    @Bind(R.id.imageView2) ImageView mImageView2;

    @Bind(R.id.textView1) TextView mTextView1;

    @Bind(R.id.textView2) TextView mTextView2;

    private SingleImageLoader mImageLoader1;

    private SingleImageLoader mImageLoader2;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_simple_loader, container, false);
        ButterKnife.bind(this, contentView);

        RequestQueue requestQueue = VolleyEx.getRequestQueue();
        mImageLoader1 = new SingleImageLoader(requestQueue, mImageView1,
            new SingleImageLoader.LoadListener() {
                @Override
                public void onFinish(Request request) {

                }

                @Override
                public void onStart() {
                }

                @Override
                public void onSuccess(Bitmap bitmap) {
                    mTextView1.setVisibility(View.VISIBLE);
                }

                @Override
                public void onErrorResponse(String requestUrl, VolleyError error) {
                    //
                }
            });
        mImageLoader2 = new SingleImageLoader(requestQueue, mImageView2,
            new SingleImageLoader.LoadListener() {
                @Override
                public void onFinish(Request request) {

                }

                @Override
                public void onStart() {
                }

                @Override
                public void onSuccess(Bitmap bitmap) {
                    mTextView2.setVisibility(View.VISIBLE);
                }

                @Override
                public void onErrorResponse(String requestUrl, VolleyError error) {
                    //
                }
            });
        mImageLoader1.setCrossFadeEnabled(true);
        mImageLoader2.setCrossFadeEnabled(true);
        mImageLoader1.load("drawable://" + R.drawable.ic_launcher);
        mImageLoader2.load("drawable://" + R.drawable.ic_launcher);
        return contentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.load_btn)
    public void clickLoadBtn() {
        int index = (int) (Math.random() * Constant.imageUrls.length);

        mImageLoader1.load(Constant.imageUrls[index]);
        mImageLoader2.load(Constant.imageUrls[index]);
    }

}
