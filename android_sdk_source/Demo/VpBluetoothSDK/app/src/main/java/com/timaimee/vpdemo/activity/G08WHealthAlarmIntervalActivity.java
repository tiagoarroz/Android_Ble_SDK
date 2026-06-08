package com.timaimee.vpdemo.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IHealthAlarmIntervalListener;
import com.veepoo.protocol.model.datas.HealthAlarmInterval;
import com.veepoo.protocol.model.enums.EHealthAlarmType;

public class G08WHealthAlarmIntervalActivity extends AppCompatActivity {

    private static final String TAG = "G08WHealthAlarmInterval";

    Spinner spinner, spinner1;
    EditText etSX, etXX;
    ToggleButton tbSwitch;
    Button btnSetting, btnRead;
    TextView tvHealthAlarmIntervalInfo;
    String[] data = {"Frequência cardíaca", "pressão arterial", "", "SpO2"};
    HealthAlarmInterval healthAlarmInterval;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_g08w_health_alarm_interval);
        spinner = findViewById(R.id.spHealthType);
        spinner1 = findViewById(R.id.spHealthType1);
        etSX = findViewById(R.id.etSX);
        etXX = findViewById(R.id.etXX);
        tbSwitch = findViewById(R.id.tgSwitch);
        btnSetting = findViewById(R.id.btnSetting);
        btnRead = findViewById(R.id.btnRead);
        tvHealthAlarmIntervalInfo = findViewById(R.id.tvHealthAlarmIntervalInfo);
        initSpinner(spinner);
        initSpinner(spinner1);
        btnSetting.setOnClickListener(v -> setHealthAlarmInterval2Device());
        btnRead.setOnClickListener(v -> readHealthAlarmIntervalFDevice());
    }

    private void initSpinner(Spinner sp) {
        //
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(this, R.layout.item_select, data);
        //Configurar
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        //sp_dialog
        //Configurar，Configurar
        sp.setPrompt("");
        //Configurar
        sp.setAdapter(starAdapter);
        //Configurar
        sp.setSelection(0);
        //Configurar，，onItemSelected
        sp.setOnItemSelectedListener(new MySelectedListener(sp));
    }

    private HealthAlarmInterval getHealthAlarmInterval() {
        int selectIndex = spinner.getSelectedItemPosition();
        EHealthAlarmType eHealthAlarmType = EHealthAlarmType.Companion.getEHealthAlarmTypeWithCMD((byte) selectIndex);
        float ceilingValue = 0;
        float floorValue = 0;
        String sxStr = etSX.getText().toString();
        String xxStr = etXX.getText().toString();
        if (!TextUtils.isEmpty(sxStr)) {
            ceilingValue = Float.parseFloat(sxStr);
        }
        if (!TextUtils.isEmpty(xxStr)) {
            floorValue = Float.parseFloat(xxStr);
        }
        boolean isOpen = tbSwitch.isChecked();
        if (selectIndex == 3) {
            //
            ceilingValue = 0;
        }
        return new HealthAlarmInterval(eHealthAlarmType, ceilingValue, floorValue, isOpen);
    }

    public void setHealthAlarmInterval2Device() {
        HealthAlarmInterval health = getHealthAlarmInterval();
        VPOperateManager.getInstance().setHealthAlarmInterval(health, new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {

            }
        }, new IHealthAlarmIntervalListener() {
            @Override
            public void functionNotSupport() {

            }

            @Override
            public void onHealthAlarmIntervalReadSuccess(@NonNull HealthAlarmInterval data, boolean isOptSuccess) {

            }

            @Override
            public void onHealthAlarmIntervalSetting(@NonNull HealthAlarmInterval data, boolean isOptSuccess) {
                if (isOptSuccess) {
                    Logger.t(TAG).i("Configurar-" + data.toString());
                    tvHealthAlarmIntervalInfo.setText("Configurar-" + data.toString());
                } else {
                    Logger.t(TAG).i("Configurar-" + data.toString());
                    tvHealthAlarmIntervalInfo.setText("Configurar-" + data.toString());
                }
            }
        });
    }

    public void readHealthAlarmIntervalFDevice() {
        int selectIndex = spinner1.getSelectedItemPosition();
        EHealthAlarmType eHealthAlarmType = EHealthAlarmType.Companion.getEHealthAlarmTypeWithCMD((byte) selectIndex);
        VPOperateManager.getInstance().readHealthAlarmInterval(eHealthAlarmType, new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {

            }
        }, new IHealthAlarmIntervalListener() {
            @Override
            public void functionNotSupport() {

            }

            @Override
            public void onHealthAlarmIntervalReadSuccess(@NonNull HealthAlarmInterval data, boolean isOptSuccess) {
                if (isOptSuccess) {
                    Logger.t(TAG).i("Ler-" + data.toString());
                    tvHealthAlarmIntervalInfo.setText("Ler-" + data.toString());
                } else {
                    Logger.t(TAG).i("Ler-" + data.toString());
                    tvHealthAlarmIntervalInfo.setText("Ler-" + data.toString());
                }
            }

            @Override
            public void onHealthAlarmIntervalSetting(@NonNull HealthAlarmInterval data, boolean isOptSuccess) {

            }
        });
    }


    class MySelectedListener implements AdapterView.OnItemSelectedListener {

        Spinner sp;

        MySelectedListener(Spinner spinner) {
            this.sp = spinner;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (sp == spinner) {
                Toast.makeText(G08WHealthAlarmIntervalActivity.this, "Configurar：" + data[i], Toast.LENGTH_SHORT).show();
                if (i == 2) {
                    etSX.setText("");
                    etSX.setHint("");
                    etSX.setHintTextColor(Color.GRAY);
                    etSX.setEnabled(true);
                    etSX.setFocusable(true);

                    etXX.setText("");
                    etXX.setEnabled(false);
                    etXX.setHint("Configurar");
                    etXX.setHintTextColor(Color.RED);

                } else if (i == 3) {
                    etSX.setText("");
                    etSX.setHint("Configurar");
                    etSX.setHintTextColor(Color.RED);
                    etSX.setEnabled(false);

                    etXX.setEnabled(true);
                    etXX.setHint("");
                    etXX.setHintTextColor(Color.GRAY);
                    etXX.setFocusable(true);
                } else {
                    etSX.setFocusable(true);
                    etSX.setEnabled(true);
                    etXX.setEnabled(true);
                    etSX.setText("");
                    etXX.setText("");
                    etSX.setHint("");
                    etXX.setHint("");
                }


            }
            if (sp == spinner1) {
                Toast.makeText(G08WHealthAlarmIntervalActivity.this, "Ler：" + data[i], Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

}
