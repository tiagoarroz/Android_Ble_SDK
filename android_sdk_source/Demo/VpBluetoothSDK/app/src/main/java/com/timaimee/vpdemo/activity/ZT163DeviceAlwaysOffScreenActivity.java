package com.timaimee.vpdemo.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.data.IZT163DeviceAlwaysOffScreenOptListener;

public class ZT163DeviceAlwaysOffScreenActivity extends AppCompatActivity implements IZT163DeviceAlwaysOffScreenOptListener {

    TextView tvInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zt163_device_always_off_screen);
        tvInfo = findViewById(R.id.tvInfo);
        findViewById(R.id.btnOpen).setOnClickListener(v -> {
            VPOperateManager.getInstance().setZT163DeviceAlwaysOffScreen(true, code -> {
            },this);
        });

        findViewById(R.id.btnClose).setOnClickListener(v -> {
            VPOperateManager.getInstance().setZT163DeviceAlwaysOffScreen(false, code -> {
            },this);
        });

        findViewById(R.id.btnRead).setOnClickListener(v -> {
            VPOperateManager.getInstance().readZT163DeviceAlwaysOffScreen(code -> {
            },this);
        });
    }

    @Override
    public void onZT163DeviceAlwaysOffScreenSettingSuccess(boolean isOpen) {
        tvInfo.setText("Configurar：" + (isOpen?"Ativar":"Desativar"));
    }

    @Override
    public void onZT163DeviceAlwaysOffScreenSettingFailed() {
        tvInfo.setText("Configurar" );
    }

    @Override
    public void onZT163DeviceAlwaysOffScreenReport(boolean isOpen) {
        tvInfo.setText("Ler：" + (isOpen?"Ativar":"Desativar"));
    }

    @Override
    public void onFunctionNotSupport() {
        tvInfo.setText("dispositivofuncionalidade" );
    }
}
