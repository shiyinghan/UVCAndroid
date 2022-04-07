package com.herohan.uvcdemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.XXPermissions;

public class EntryActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        initListeners();
    }

    private void initListeners() {
        Button btnBasicPreview = findViewById(R.id.btnBasicPreview);
        btnBasicPreview.setOnClickListener(this);
        Button btnCustomPreview = findViewById(R.id.btnCustomPreview);
        btnCustomPreview.setOnClickListener(this);
        Button btnMultiPreview = findViewById(R.id.btnMultiPreview);
        btnMultiPreview.setOnClickListener(this);
        Button btnMultiCamera = findViewById(R.id.btnMultiCamera);
        btnMultiCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        XXPermissions.with(this)
                .permission(Manifest.permission.CAMERA)
                .request((permissions, all) -> {

                    if (v.getId() == R.id.btnBasicPreview) {
                        startActivity(new Intent(this, BasicPreviewActivity.class));
                    } else if (v.getId() == R.id.btnCustomPreview) {
                        startActivity(new Intent(this, CustomPreviewActivity.class));
                    } else if (v.getId() == R.id.btnMultiPreview) {
                        startActivity(new Intent(this, MultiPreviewActivity.class));
                    } else if (v.getId() == R.id.btnMultiCamera) {
                        startActivity(new Intent(this, MultiCameraActivity.class));
                    }

                });

    }
}