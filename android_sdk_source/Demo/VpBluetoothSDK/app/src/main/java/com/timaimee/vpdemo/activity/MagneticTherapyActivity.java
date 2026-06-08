package com.timaimee.vpdemo.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.listener.data.IMagneticTherapyListener;
import com.veepoo.protocol.model.datas.MagneticTherapy;
import com.veepoo.protocol.model.enums.MagneticTherapyType;

//import cn.carbs.android.segmentcontrolview.library.SegmentControlView;

public class MagneticTherapyActivity extends AppCompatActivity implements IMagneticTherapyListener {
//    SegmentControlView scvType, scvState;

    SeekBar sbDuration, sbLevel;

    TextView tvDuration, tvLevel, tvResult;

    Button btnSetting;

    private int duration = 1;
    private int level = 1;
    private boolean isOpen = true;
    private MagneticTherapyType type = MagneticTherapyType.PULSE_MAGNETIC_THERAPY;
    //Configurar
//            VPOperateManager.getInstance().setGlobalMagneticTherapy(new IMagneticTherapyListener() {
//                @Override
//                public void functionNotSupport() {
//
//                }
//
//                @Override
//                public void onMagneticTherapyChange(@NonNull MagneticTherapy data) {
//
//                }
//
//                @Override
//                public void onMagneticTherapyOpen(@NonNull MagneticTherapy data) {
//
//                }
//
//                @Override
//                public void onMagneticTherapyClose(@NonNull MagneticTherapy data) {
//
//                }
//            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetic_therapy);
//        scvType = findViewById(R.id.magneticTherapyType);
//        scvState = findViewById(R.id.scvStatus);
//        tvDuration = findViewById(R.id.tvDurationValue);
//        tvLevel = findViewById(R.id.tvLevelValue);
//        btnSetting = findViewById(R.id.btnSetting);
//        sbDuration = findViewById(R.id.sbDuration);
//        sbLevel = findViewById(R.id.sbLevel);
//        tvResult = findViewById(R.id.tvResult);
//        sbDuration.setMin(1);
//        sbDuration.setMax(15);
//        sbLevel.setMax(7);
//        sbLevel.setMin(1);
//        initListener();
    }

//    private void initListener() {
//        scvType.setOnSegmentChangedListener(newSelectedIndex -> {
//            if (newSelectedIndex == 0) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    sbDuration.setMin(1);
//                }
//                sbDuration.setMax(255);
//                tvDuration.setText("1");
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    sbDuration.setMin(1);
//                }
//                sbDuration.setMax(15);
//                tvDuration.setText("1");
//            }
//        });
//
//        scvState.setOnSegmentChangedListener(newSelectedIndex -> isOpen = newSelectedIndex == 0);
//
//        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                duration = progress;
//                tvDuration.setText(duration + "");
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//
//        sbLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                level = progress;
//                tvLevel.setText("" + level);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//
//        btnSetting.setOnClickListener(v -> {
//            if (isOpen) {
//                MagneticTherapy magneticTherapy = new MagneticTherapy(type, true, level);
//                VPOperateManager.getInstance().openMagneticTherapy(magneticTherapy, duration, response, MagneticTherapyActivity.this);
//            } else {
//                VPOperateManager.getInstance().closeMagneticTherapy(type, response, MagneticTherapyActivity.this);
//            }
//        });
//    }

    @Override
    public void functionNotSupport() {
        setResult("funcionalidade");
    }

    @Override
    public void onMagneticTherapyDurationError(int min, int max) {
        setResult("Configurar Configurar" + min + "~" + max + "（）");
    }

    @Override
    public void onMagneticTherapyChange(@NonNull MagneticTherapy data) {
        setResult("dispositivo：" + data.toString());
    }

    @Override
    public void onMagneticTherapyOpen(@NonNull MagneticTherapy data) {
        setResult("Ativar：" + data.toString());
    }

    @Override
    public void onMagneticTherapyClose(@NonNull MagneticTherapy data) {
        setResult("Desativar：" + data.toString());
    }

    public BleWriteResponse response = new BleWriteResponse() {
        @Override
        public void onResponse(int code) {

        }
    };

    private void setResult(String info) {
        runOnUiThread(() -> tvResult.setText(info));
    }
}
