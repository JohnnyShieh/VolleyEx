package com.johnnyshieh.volleyex;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyEx;
import com.android.volley.toolbox.ImageRequest;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SampleActivity extends AppCompatActivity {

    @Bind(R.id.load_btn)
    TextView mLoadBtn;

    @Bind(R.id.net_image)
    ImageView mNetworkImageView;

    RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        mRequestQueue = VolleyEx.newRequestQueue(this);

        ButterKnife.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRequestQueue.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        mRequestQueue.destroy();
    }

    @OnClick(R.id.load_btn)
    public void clickLoadBtn() {
        ImageRequest request = new ImageRequest(
            "http://johnnyshieh.github.io/assets/screenshot_googleplay.png",
            new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    mNetworkImageView.setImageBitmap(response);
                }
            }, 0, 0, Bitmap.Config.RGB_565, null);
        request.setTtl(200);
        request.setSoftTtl(200);
        mRequestQueue.add(request);
    }
}
