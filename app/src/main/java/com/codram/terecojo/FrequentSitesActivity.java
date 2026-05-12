package com.codram.terecojo;

import android.os.Bundle;

public class FrequentSitesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequent_sites);
        setupDrawer();
    }
}
