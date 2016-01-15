package com.johnnyshieh.volleyex.view.adapter;
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

import com.android.volley.image.ArrayImageLoader;
import com.johnnyshieh.volleyex.R;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Johnny Shieh
 * @version 1.0
 */
public class ArrayListAdapter extends RecyclerView.Adapter<ArrayListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder{

        @Bind(R.id.net_image) ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private List<String> mImageUrlList;

    private ArrayImageLoader mArrayImageLoader;

    public ArrayListAdapter(ArrayImageLoader arrayImageLoader) {
        mArrayImageLoader = arrayImageLoader;
    }

    public void setImageUrlList(List<String> urlList) {
        mImageUrlList = urlList;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.array_loader_list_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mArrayImageLoader.loadBitmap(holder.imageView, mImageUrlList.get(position), holder.imageView.getMeasuredWidth(),
            holder.imageView.getMeasuredHeight(), position);
    }

    @Override
    public int getItemCount() {
        return null == mImageUrlList ? 0 : mImageUrlList.size();
    }


}
