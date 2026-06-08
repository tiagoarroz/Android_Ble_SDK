package com.timaimee.vpdemo.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.nordic.OnMcuMgrOtaListener;
import com.veepoo.protocol.nordic.McuMgrOtaManager;
import com.orhanobut.logger.Logger;

import io.runtime.mcumgr.dfu.FirmwareUpgradeController;
import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;

import java.io.File;
import java.util.Locale;

public class NRFOtaActivity extends AppCompatActivity {
    TextView tvOTAFilePath, tvUpgradeInfo;
    Button btnStartNRFOta;

    private String firmwareFilePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nrf_ota);
        tvOTAFilePath = findViewById(R.id.tvOTAFilePath);
        tvUpgradeInfo = findViewById(R.id.tvUpgradeInfo);
        btnStartNRFOta = findViewById(R.id.btnStartNRFOta);//
        firmwareFilePath = "/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/8600_99.99.98.bin";
        initEvent();
        tvOTAFilePath.setText("Caminho do ficheiro de atualização：" + firmwareFilePath + "()");
//        McuMgrOtaManager.getInstance().setMtu(240);
        Logger.t("NRF-OTA").e("ConfigurarNRF-OTA MTU=240");
    }


    private int lastBytesSent = 0;
    private long lastTimestamp = 0;

    private void initEvent() {
        btnStartNRFOta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!new File(firmwareFilePath).exists()) {
                    Toast.makeText(NRFOtaActivity.this, "", Toast.LENGTH_SHORT).show();
                    return;
                }
                VPOperateManager.getInstance().startNordicOtaUpgrade(firmwareFilePath, new OnMcuMgrOtaListener() {
                    @Override
                    public void onUpgradeStarted(FirmwareUpgradeController controller) {
                        tvUpgradeInfo.setText("Iniciar atualização");
                    }

                    @Override
                    public void onStateChanged(FirmwareUpgradeManager.State prevState, FirmwareUpgradeManager.State newState) {
                        tvUpgradeInfo.setText("Mudança de estado da atualização： prevState=" + prevState + " , newState=" + newState);
                    }

                    @Override
                    public void onUpgradeCompleted() {
                        tvUpgradeInfo.setText("Atualização concluída");
                    }

                    @Override
                    public void onUpgradeFailed(FirmwareUpgradeManager.State state, McuMgrException error) {
                        tvUpgradeInfo.setText("Falha na atualização：state=" + state + "， error=" + error);
                    }

                    @Override
                    public void onUpgradeCanceled(FirmwareUpgradeManager.State state) {
                        tvUpgradeInfo.setText("Atualização cancelada：state=" + state);
                    }

                    @Override
                    public void onUploadProgressChanged(int bytesSent, int imageSize, long timestamp) {
                        int progress = (int) ((bytesSent * 1f / imageSize) * 100);
                        tvUpgradeInfo.setText(String.format(Locale.US, "Progresso da atualização：%d/%d >> %d%%", bytesSent, imageSize, progress));
                        Logger.t("NRF-OTA").e(String.format(Locale.US, "Progresso da atualização：%d/%d >> %d%%", bytesSent, imageSize, progress));

                        if (lastTimestamp > 0) {
                            // 1. Enviardados (Bytes)
                            int deltaBytes = bytesSent - lastBytesSent;
                            // 2.  ()
                            long deltaTime = timestamp - lastTimestamp;
                            if (deltaTime > 0) {
                                // 3.  (bytes/ms  KB/s)
                                // ：(deltaBytes / 1024) / (deltaTime / 1000)
                                // ：(deltaBytes * 1000) / (deltaTime * 1024)
                                double speedKbps = (deltaBytes * 1000.0) / (deltaTime * 1024.0);
                                Logger.t("NRF-OTA").e("-onUploadProgressChanged-: | Atualização de firmware " + progress + "%, : " + String.format("%.2f", speedKbps) + " KB/s");
                                tvUpgradeInfo.setText("Atualização de firmware: " + progress + "%" + String.format(" : %.2f", speedKbps) + " KB/s");
                            }
                        }

                        // Atualizar，
                        lastBytesSent = bytesSent;
                        lastTimestamp = timestamp;
                        //  ()
                        System.out.println(": " + progress + "%");

                    }
                });
            }
        });
    }

}
