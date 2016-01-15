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

import com.johnnyshieh.volleyex.R;
import com.johnnyshieh.volleyex.view.activity.ArrayLoaderActivity;
import com.johnnyshieh.volleyex.view.activity.SingleLoaderActivity;
import com.johnnyshieh.volleyex.model.ActivityInfo;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * The adapter provides a list of VolleyEx sample entries.
 *
 * @author Johnny Shieh
 * @version 1.0
 */
public class SampleListAdapter extends RecyclerView.Adapter<SampleListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder{

        @Bind(android.R.id.text1) TextView text1;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            text1.setBackgroundResource(R.drawable.clickabe_view_background);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnItemClickListener.onItemClick(view, getLayoutPosition());
                }
            });
        }
    }

    private List<ActivityInfo> mData = Arrays.asList(
        new ActivityInfo("SingleLoader", SingleLoaderActivity.class),
        new ActivityInfo("ArrayLoader", ArrayLoaderActivity.class)
    );

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(View itemView, int position) {
            Intent intent = new Intent(mActivity, mData.get(position).classInfo);
            mActivity.startActivity(intent);
        }
    };

    private Activity mActivity;

    public SampleListAdapter(Activity activity) {
        mActivity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.text1.setText(mData.get(position).displayName);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


}
