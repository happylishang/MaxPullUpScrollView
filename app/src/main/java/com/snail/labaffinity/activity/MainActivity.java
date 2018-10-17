package com.snail.labaffinity.activity;

import android.os.Bundle;

import com.snail.labaffinity.R;
import com.snail.labaffinity.view.MaxPullUpScrollView;


public class MainActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaxPullUpScrollView m= (MaxPullUpScrollView) findViewById(R.id.mv);
        m.setScrollContentView(R.id.tv);
    }
}
