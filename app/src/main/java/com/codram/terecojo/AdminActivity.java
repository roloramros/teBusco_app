package com.codram.terecojo;

import android.os.Bundle;
import com.codram.terecojo.databinding.ActivityAdminBinding;

public class AdminActivity extends BaseActivity {
    private ActivityAdminBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer();


    }
}
