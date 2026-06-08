package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.data.IPPGRawDataReadListener;
import com.veepoo.protocol.listener.data.IPPGRealTimeTransmissionListener;
import com.veepoo.protocol.listener.data.IPPGSwitchOperaterListener;
import com.veepoo.protocol.model.datas.AccelerationData;
import com.veepoo.protocol.model.datas.PPGRawData;
import com.veepoo.protocol.model.datas.PPGReadData;
import com.veepoo.protocol.model.datas.PPGSecondData;
import com.veepoo.protocol.model.datas.TimeData;
import com.veepoo.protocol.model.enums.PPGSwitchStatus;
import com.veepoo.protocol.model.enums.PPGTestMode;
//import com.veepoo.protocol.util.thread.HBThreadPools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;

public class JH58PPGOptTestActivity extends Activity implements View.OnClickListener {

    TimePickerDialog timePickerDialog = null;
    DatePickerDialog datePickerDialog = null;

    Button btnReadPPGTestStatus;
    Button btnStartRealTimePPGRawDataTransfer;
    Button btnStopRealTimePPGRawDataTransfer;
    Button btnReadPPGRawData;
    Button btnDatePicker;
    Button btnTimePicker;
    Button btnShareData;
    TextView tvPPGTestStatus;
    TextView tvPPGOptInfo;

    RadioGroup rgPPGTestMode;
    RadioGroup rgReadPPGTestMode;
    ScrollView svInfo;

    TimeData timeData = null;

    PPGSwitchStatus ppgSwitchStatus = PPGSwitchStatus.MODE1_ON;
    PPGTestMode ppgTestMode = PPGTestMode.MODE1;

    StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jh58ppg_test);
        initView();
        initData();
    }

    private void initView() {
        btnStartRealTimePPGRawDataTransfer = findViewById(R.id.btnStartRealTimePPGRawDataTransfer);
        btnStopRealTimePPGRawDataTransfer = findViewById(R.id.btnStopRealTimePPGRawDataTransfer);
        btnReadPPGTestStatus = findViewById(R.id.btnReadPPGTestStatus);
        btnReadPPGRawData = findViewById(R.id.btnReadPPGRawData);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        btnTimePicker = findViewById(R.id.btnTimePicker);
        btnShareData = findViewById(R.id.btnShareData);
        tvPPGTestStatus = findViewById(R.id.tvPPGTestStatus);
        tvPPGOptInfo = findViewById(R.id.tvPPGOptInfo);
        rgPPGTestMode = findViewById(R.id.rgPPGTestMode);
        rgReadPPGTestMode = findViewById(R.id.rgReadPPGTestMode);
        svInfo = findViewById(R.id.svInfo);
    }

    private void initData() {
        rgReadPPGTestMode.check(R.id.rbReadMode1On);
        btnDatePicker.setOnClickListener(this);
        btnTimePicker.setOnClickListener(this);
        btnReadPPGTestStatus.setOnClickListener(this);
        btnReadPPGRawData.setOnClickListener(this);
        btnShareData.setOnClickListener(this);
        btnStartRealTimePPGRawDataTransfer.setOnClickListener(this);
        btnStopRealTimePPGRawDataTransfer.setOnClickListener(this);
        timeData = new TimeData();
        timeData.setCurrentTime();
        timeData.setHour(0);
        timeData.setMinute(0);
        timeData.setSecond(0);
        btnDatePicker.setText(timeData.toDatabaseDateString());
        btnTimePicker.setText(timeData.getClock() + ":00");
        //LerPPGdados
        rgReadPPGTestMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbReadMode1On) {
                ppgTestMode = PPGTestMode.MODE1;
            }
            if (checkedId == R.id.rbReadMode2On) {
                ppgTestMode = PPGTestMode.MODE2;
            }
        });
        //Configurar
        rgPPGTestMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAllOff) {
                ppgSwitchStatus = PPGSwitchStatus.ALL_OFF;
            } else if (checkedId == R.id.rbMode1On) {
                ppgSwitchStatus = PPGSwitchStatus.MODE1_ON;
            } else if (checkedId == R.id.rbMode2On) {
                ppgSwitchStatus = PPGSwitchStatus.MODE2_ON;
            }
            /*Configurar estado do PPG*/
            VPOperateManager.getInstance().setPPGSwitchStatus(ppgSwitchStatus, code -> tvPPGOptInfo.setText("ConfigurarPPGestadoEnviar" + (code == Code.REQUEST_SUCCESS ? "" : "")));
        });
        readPPGTestStatus();
        /*AdicionarPPGestado*/
        VPOperateManager.getInstance().addPPGTestSwitchStatusListener(new IPPGSwitchOperaterListener() {
            @Override
            public void onPPGSwitchStatusRead(@NonNull PPGSwitchStatus switchStatus) {
                tvPPGTestStatus.setText(switchStatus.getDes());
                if (switchStatus != PPGSwitchStatus.ALL_OFF) {
                    ppgSwitchStatus = switchStatus;
                } /*else if(switchStatus == PPGSwitchStatus.MODE1_ON) {
                    rgReadPPGTestMode.check(R.id.rbReadMode1On);
                } else if(switchStatus == PPGSwitchStatus.MODE2_ON) {
                    rgReadPPGTestMode.check(R.id.rbReadMode2On);
                }*/
            }

            @Override
            public void onPPGSwitchStatusSetting(@NonNull PPGSwitchStatus switchStatus) {
                tvPPGOptInfo.setText("PPGConfigurar：" + switchStatus);
            }

            @Override
            public void onPPGSwitchStatusReport(@NonNull PPGSwitchStatus switchStatus) {
                tvPPGOptInfo.setText("PPG：" + switchStatus);
            }
        });
        /*AdicionardispositivoPPG*/
        VPOperateManager.getInstance().addDevicePPGRealTimeTransferListener(new IPPGRealTimeTransmissionListener() {

            @Override
            public void onDeviceRequestPPGRealTimeTransfer(boolean isRequestOpen) {
                appendMsg("dispositivoPPG：" + (isRequestOpen ? "Ativar" : "Desativar"));
            }

            @Override
            public void onAppRequestPPGRealTimeTransfer(boolean isSuccess) {
                appendMsg("AppPPG：" + (isSuccess ? "" : ""));
            }

            @Override
            public void onGreenLightDataReport(@NonNull List<Integer> greenLightDataList) {
                appendMsg(">>> " + greenLightDataList);
            }

            @Override
            public void onAccelerationDataReport(@NonNull List<AccelerationData> accDataList) {
                appendMsg(">>> " + accDataList);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnDatePicker) {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                timeData.setYear(year);
                timeData.setMonth(month + 1);
                timeData.setDay(dayOfMonth);
                btnDatePicker.setText(String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, dayOfMonth));
            }, TimeData.getSystemYear(), TimeData.getSystemMonth(), TimeData.getSystemDay()).show();
        } else if (id == R.id.btnTimePicker) {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeData.setHour(hourOfDay);
                timeData.setMinute(minute);
                timeData.setSecond(0);
                btnTimePicker.setText(String.format(Locale.CHINA, "%02d:%02d:00", hourOfDay, minute));
            }, timeData.getHour(), timeData.getMinute(), true).show();
        } else if (id == R.id.btnReadPPGRawData) {
            readPPGRawData();
        } else if (id == R.id.btnShareData) {
            shareData();
        } else if (id == R.id.tvAutoMeasureType) {
            readPPGTestStatus();
        } else if (id == R.id.btnStartRealTimePPGRawDataTransfer) {
            startPPGRawDataRealTimeTransfer();
        } else if (id == R.id.btnStopRealTimePPGRawDataTransfer) {
            stopPPGRawDataRealTimeTransfer();
        }
    }

    private void readPPGTestStatus() {
        VPOperateManager.getInstance().readPPGSwitchStatus(code -> tvPPGOptInfo.setText("LerPPGestadoEnviar" + (code == Code.REQUEST_SUCCESS ? "" : "")));
    }

    private PPGReadData ppgReadData = null;

    private void readPPGRawData() {
        sb.setLength(0);
        appendMsg("LerPPGDados brutos~");
        appendMsg(":" + timeData.toFullDateTimeString() + " , :" + ppgTestMode);
        VPOperateManager.getInstance().readPPGRawData(timeData, ppgTestMode, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                appendMsg("~LerPPGestadoEnviar" + (code == Code.REQUEST_SUCCESS ? "" : ""));
            }
        }, new IPPGRawDataReadListener() {
            @Override
            public void onPPGReadStart(int count) {
                appendMsg("IniciarLerPPGDados brutos\n" + count + "dados");
            }

            @Override
            public void onPPGRawDataRead(int index, int count, @NonNull PPGRawData ppgRawData) {
                appendMsg("PPGDados brutosLer\n>>>>>>>>>" + index + "/" + count + " --> " + ppgRawData.toString());
            }

            @Override
            public void onPPGRawDataReadComplete(@NonNull PPGReadData ppgReadData) {
                appendMsg("PPGDados brutosLer\n" + ppgReadData);
                JH58PPGOptTestActivity.this.ppgReadData = ppgReadData;
            }

            @Override
            public void onPPGRawDataReadStop() {
                String content = tvPPGOptInfo.getText().toString();
                appendMsg(content + "\nPPGDados brutosLer");
            }
        });
    }

    private void startPPGRawDataRealTimeTransfer() {
        sb.setLength(0);
        appendMsg("APPIniciarPPG");
        VPOperateManager.getInstance().startPPGRealTimeTransmission(code -> appendMsg("IniciarPPGEnviar" + (code == Code.REQUEST_SUCCESS ? "" : "")), new IPPGRealTimeTransmissionListener() {
            @Override
            public void onDeviceRequestPPGRealTimeTransfer(boolean isRequestOpen) {
                appendMsg("dispositivoPPG：" + (isRequestOpen ? "Ativar" : "Desativar"));
            }

            @Override
            public void onAppRequestPPGRealTimeTransfer(boolean isSuccess) {
                appendMsg("AppPPG：" + (isSuccess ? "" : ""));
            }

            @Override
            public void onGreenLightDataReport(@NonNull List<Integer> greenLightDataList) {
                appendMsg(">>> " + greenLightDataList);
            }

            @Override
            public void onAccelerationDataReport(@NonNull List<AccelerationData> accDataList) {
                appendMsg(">>> " + accDataList);
            }
        }/*, new IPPGRealTimeTransferOptListener() {
            @Override
            public void onDeviceRequestPPGRealTimeTransfer(boolean isRequestOpen) {
                tvPPGOptInfo.setText("start，dispositivoPPG：" + (isRequestOpen ? "Ativar" : "Desativar"));
            }

            @Override
            public void onAppRequestPPGRealTimeTransfer(boolean isSuccess) {
                tvPPGOptInfo.setText("start，AppPPG：" + (isSuccess ? "" : ""));
            }
        }*/);
    }

    private void stopPPGRawDataRealTimeTransfer() {
        sb.setLength(0);
        appendMsg("APPPPG");
        VPOperateManager.getInstance().stopPPGRealTimeTransmission(code -> appendMsg("PPGEnviar" + (code == Code.REQUEST_SUCCESS ? "" : ""))/*, new IPPGRealTimeTransferOptListener() {
            @Override
            public void onDeviceRequestPPGRealTimeTransfer(boolean isRequestOpen) {
                tvPPGOptInfo.setText("stop，dispositivoPPG：" + (isRequestOpen ? "Ativar" : "Desativar"));
            }

            @Override
            public void onAppRequestPPGRealTimeTransfer(boolean isSuccess) {
                tvPPGOptInfo.setText("stop，AppPPG：" + (isSuccess ? "" : ""));
            }
        }*/);
    }

    private void shareData() {
        if (ppgReadData == null) {
            Toast.makeText(this, "Lerdadospartilhar", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = "JH58PPGdadosLer.txt";
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("dados:").append(ppgReadData.getDataCount()).append("\n");
            for (PPGRawData ppgRawData : ppgReadData.getPpgRawDataList()) {
                sb.append("").append(ppgRawData.getIndex()).append("/").append(ppgReadData.getDataCount()).append(":")
                        .append(TimeData.getTimeBeanByTimestampSecond((int) ppgRawData.getTimestamp()).toFullDateTimeString())
                        .append(", dados=").append(ppgRawData.getCount()).append(",").append(ppgRawData.getPpgSecondDataList().size()).append("dados\n");
                for (PPGSecondData ppgSecondData : ppgRawData.getPpgSecondDataList()) {
                    sb.append(ppgSecondData.toDataStr()).append("\n");
                }
            }
            appendTextToExternalFilesDir(JH58PPGOptTestActivity.this, fileName, sb.toString());
            runOnUiThread(() -> {
                File externalFilesDir = getExternalFilesDir(null);

                if (externalFilesDir == null) {
                    Log.e("FileSave", "，dispositivoestado");
                    return;
                }

                File targetFile = new File(externalFilesDir, fileName);

                shareTxtContent(JH58PPGOptTestActivity.this, targetFile.getAbsolutePath(), "partilharPPGLerDados brutos");
            });
        }).start();

    }


    /**
     * Ler TXT partilhar（partilhar）
     *
     * @param context      
     * @param filePath     
     * @param chooserTitle partilhar
     */
    public void shareTxtContent(Context context, String filePath, String chooserTitle) {
        File file = new File(filePath);

        if (!file.exists()) {
            Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = getUri(context, filePath);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/txt");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle));
    }

    private Uri getUri(Context context, String filePath) {
        Uri dfuFileUri;
        if (Build.VERSION.SDK_INT >= 24) {
            dfuFileUri = FileProvider.getUriForFile(
                    context,
                    "com.timaimee.vpdemo.fileProvider",
                    new File(filePath));
        } else {
            dfuFileUri = Uri.fromFile(new File(filePath));
        }
        return dfuFileUri;
    }

    // 
    public static void appendTextToExternalFilesDir(Context context, String filename, String textContent) {
        File externalFilesDir = context.getExternalFilesDir(null);

        if (externalFilesDir == null) {
            Log.e("FileSave", "，dispositivoestado");
            return;
        }

        File targetFile = new File(externalFilesDir, filename);
//        if (targetFile.exists()) {
//            targetFile.delete();
//        }
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;

        try {
            //  FileOutputStream 
            fos = new FileOutputStream(targetFile, false);
            osw = new OutputStreamWriter(fos);

            osw.write(textContent);
            osw.flush();

            Log.i("FileSave", ": " + targetFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e("FileSave", ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e("FileSave", "Desativar: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void appendMsg(String msg) {
        runOnUiThread(() -> {
            sb.append(msg).append("\n");
            tvPPGOptInfo.setText(sb.toString());
            svInfo.fullScroll(View.FOCUS_DOWN);
        });
    }
}
