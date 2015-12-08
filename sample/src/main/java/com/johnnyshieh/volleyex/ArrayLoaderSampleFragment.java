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

import com.android.volley.image.ArrayImageLoader;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author: Johnny Shieh
 * @date: 2015-12-08
 */
public class ArrayLoaderSampleFragment extends Fragment {

    @Bind(R.id.gridview) GridView mGridView;

    private MyAdapter mAdapter;

    private ArrayImageLoader mArrayImageLoader;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_array_loader, container, false);
        ButterKnife.bind(this, contentView);

        mAdapter = new MyAdapter();
        mArrayImageLoader = new ArrayImageLoader(RequestQueueHolder.getInstance(), mAdapter, null);
        mGridView.setAdapter(mAdapter);
        return contentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mArrayImageLoader.destroy();
        ButterKnife.unbind(this);
    }

    private List<View> findViewByTag(String tag) {
        List<View> list = new ArrayList<>();
        int firstPos = mGridView.getFirstVisiblePosition();
        int lastPos = mGridView.getLastVisiblePosition();
        for(int i = firstPos; i <= lastPos; i ++) {
            View view = mGridView.getChildAt(i - firstPos);
            if(null != view) {
                View tagView = view.findViewWithTag(tag);
                if(null != tagView) {
                    list.add(tagView);
                }
            }
        }
        return list;
    }

    public class MyAdapter extends BaseAdapter implements ArrayImageLoader.SuccessListener{

        @Override
        public void onSuccess(String requestUrl, Bitmap bitmap) {
            if(null == bitmap) {
                return;
            }
            List<View> list = findViewByTag(requestUrl);
            if(null != list && list.size() > 0) {
                for (View view : list) {
                    if(view instanceof ImageView) {
                        ((ImageView)view).setImageBitmap(bitmap);
                    }
                }
            }
        }

        public class Holder {
            @Bind(R.id.net_image) ImageView imageView;

            public Holder(View containerView) {
                ButterKnife.bind(this, containerView);
            }
        }

        @Override
        public int getCount() {
            return Constant.imageUrls.length;
        }

        @Override
        public Object getItem(int position) {
            return Constant.imageUrls[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
                convertView.setTag(new Holder(convertView));
            }
            Holder holder = (Holder) convertView.getTag();
            String requestUrl = (String) getItem(position);
            holder.imageView.setTag(requestUrl);
            holder.imageView.setImageBitmap(mArrayImageLoader.loadBitmap(requestUrl, holder.imageView.getMeasuredWidth(),
                holder.imageView.getMeasuredHeight(), holder.imageView.getScaleType()));
            return convertView;
        }
    }
}
