package com.johnnyshieh.volleyex;

import com.android.volley.VolleyEx;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static class HomeFragment extends Fragment {

        @Bind(R.id.simple_loader_sample) Button mSimpleLoaderSample;
        @Bind(R.id.array_loader_sample) Button mArrayLoaderSample;
        @Bind(R.id.clear_disk_cache) Button mClearDiskCache;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            View contentView = inflater.inflate(R.layout.fragment_home, container, false);
            ButterKnife.bind(this, contentView);
            return contentView;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            ButterKnife.unbind(this);
        }

        @OnClick(R.id.simple_loader_sample)
        public void clickSingleLoaderSample() {
            getActivity().startActivity(new Intent(getActivity(), SingleLoaderSampleActivity.class));
        }

        @OnClick(R.id.array_loader_sample)
        public void clickarrayLoaderSample() {
            getActivity().startActivity(new Intent(getActivity(), ArrayLoaderSampleActivity.class));
        }

        @OnClick(R.id.clear_disk_cache)
        public void clickClearDiskCache() {
            VolleyEx.getRequestQueue().getCache().clear();
            Toast.makeText(getActivity(), "The Disk cache is clear.", Toast.LENGTH_SHORT).show();
        }

    }

}
