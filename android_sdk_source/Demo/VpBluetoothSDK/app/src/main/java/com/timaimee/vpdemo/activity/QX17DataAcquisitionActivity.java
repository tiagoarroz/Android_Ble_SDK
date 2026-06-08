package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IQX17DataAcquisitionListener;
import com.veepoo.protocol.listener.data.IQX17DataAcquisitionStateListener;
import com.veepoo.protocol.model.datas.QX17GPSData;
import com.veepoo.protocol.model.datas.QX17HeartRateData;
import com.veepoo.protocol.model.datas.QX17IMUData;
import com.veepoo.protocol.model.enums.EQX17VibrationMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * QX17dadosActivity
 */
public class QX17DataAcquisitionActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "QX17Demo";
    private VPOperateManager mVpOperateManager;

    private Button btnStart, btnStop, btnContinue, btnClearLog, btnSendVibration;
    private TextView tvStatus, tvLog;
    private ScrollView scrollLog;
    private Spinner spinnerVibrationMode;
    private EditText etVibrationDuration;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private int imuCount = 0;
    private int gpsCount = 0;
    private int hrCount = 0;

    // estado，OperaterActivityAtualizar
    public static Boolean lastKnownState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qx17_data_acquisition);

        mVpOperateManager = VPOperateManager.getInstance();

        btnStart = (Button) findViewById(R.id.btn_qx17_start);
        btnStop = (Button) findViewById(R.id.btn_qx17_stop);
        btnContinue = (Button) findViewById(R.id.btn_qx17_continue);
        btnClearLog = (Button) findViewById(R.id.btn_qx17_clear_log);
        tvStatus = (TextView) findViewById(R.id.tv_qx17_status);
        tvLog = (TextView) findViewById(R.id.tv_qx17_log);
        scrollLog = (ScrollView) findViewById(R.id.scroll_qx17_log);
        spinnerVibrationMode = (Spinner) findViewById(R.id.spinner_qx17_vibration_mode);
        etVibrationDuration = (EditText) findViewById(R.id.et_qx17_vibration_duration);
        btnSendVibration = (Button) findViewById(R.id.btn_qx17_send_vibration);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnContinue.setOnClickListener(this);
        btnClearLog.setOnClickListener(this);
        btnSendVibration.setOnClickListener(this);

        // Spinner
        String[] vibrationModes = {"Iniciar(0)", "Terminar(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vibrationModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVibrationMode.setAdapter(adapter);

        // estado
        updateStatusDisplay();

        // Configurarestado，Atualizarestado
        mVpOperateManager.setVpQX17DataAcquisitionStateListener(mStateListener);
    }

    private void updateStatusDisplay() {
        if (lastKnownState != null) {
            tvStatus.setText("estado: " + (lastKnownState ? "" : ""));
        } else {
            tvStatus.setText("estado: ");
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_qx17_start) {
            imuCount = 0;
            gpsCount = 0;
            hrCount = 0;
            appendLog(">>> EnviarAtivardados");
            mVpOperateManager.vpQX17StartDataAcquisition(new IBleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    appendLog("Ativar: code=" + code);
                }
            }, mDataListener);
        } else if (id == R.id.btn_qx17_stop) {
            appendLog(">>> EnviarDesativardados");
            mVpOperateManager.vpQX17StopDataAcquisition(new IBleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    appendLog("Desativar: code=" + code);
                }
            });
        } else if (id == R.id.btn_qx17_continue) {
            appendLog(">>> Enviardados()");
            mVpOperateManager.vpQX17ContinueDataAcquisition(new IBleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    appendLog(": code=" + code);
                }
            }, mDataListener);
        } else if (id == R.id.btn_qx17_clear_log) {
            tvLog.setText("");
            imuCount = 0;
            gpsCount = 0;
            hrCount = 0;
        } else if (id == R.id.btn_qx17_send_vibration) {
            EQX17VibrationMode mode = EQX17VibrationMode.values()[spinnerVibrationMode.getSelectedItemPosition()];
            String durationStr = etVibrationDuration.getText().toString().trim();
            int duration = 0;
            try {
                duration = Integer.parseInt(durationStr);
            } catch (NumberFormatException e) {
                // ignore
            }
            appendLog(">>> Enviar: mode=" + mode + ", duration=" + duration);
            mVpOperateManager.vpQX17SetVibrationMode(new IBleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    appendLog(": code=" + code);
                }
            }, mode, duration);
        }
    }

    // estado：dispositivoestado
    private final IQX17DataAcquisitionStateListener mStateListener = new IQX17DataAcquisitionStateListener() {
        @Override
        public void onQX17DataAcquisitionStatus(boolean isOpen) {
            appendLog("[estado] isOpen=" + isOpen);
            lastKnownState = isOpen;
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateStatusDisplay();
                }
            });
        }
    };

    // dados：IMU/GPS/Frequência cardíacadados
    private final IQX17DataAcquisitionListener mDataListener = new IQX17DataAcquisitionListener() {
        @Override
        public void onQX17DataAcquisitionStatus(boolean isOpen) {
            appendLog("[dados-estado] isOpen=" + isOpen);
        }

        @Override
        public void onQX17IMUData(List<QX17IMUData> imuDataList) {
            imuCount += imuDataList.size();
            QX17IMUData last = imuDataList.get(imuDataList.size() - 1);
            Log.d(TAG, "IMUdados: count=" + imuDataList.size() + ", last=" + last.toString());
            appendLog("[IMU] +" + imuDataList.size() + " (" + imuCount + ") ts=" + last.getTimestamp());
        }

        @Override
        public void onQX17GPSData(QX17GPSData gpsData) {
            gpsCount++;
            appendLog("[GPS] #" + gpsCount + " lat=" + gpsData.getLatitude()
                    + " lon=" + gpsData.getLongitude()
                    + " acc=" + gpsData.getAccuracy() + "m"
                    + " ts=" + gpsData.getTimestamp());
        }

        @Override
        public void onQX17HeartRateData(QX17HeartRateData heartRateData) {
            hrCount++;
            appendLog("[Frequência cardíaca] #" + hrCount + " HR=" + heartRateData.getHeartRate()
                    + " ts=" + heartRateData.getTimestamp());
        }
    };

    private void appendLog(String message) {
        String time = mTimeFormat.format(new Date());
        String line = time + " " + message + "\n";
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                tvLog.append(line);
                scrollLog.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollLog.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
        Log.d(TAG, message);
    }
}
