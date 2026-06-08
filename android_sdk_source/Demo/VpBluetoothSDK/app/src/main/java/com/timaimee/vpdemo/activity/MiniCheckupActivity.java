package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.inuker.bluetooth.library.Code;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.IMiniCheckupOptListener;
import com.veepoo.protocol.model.datas.MiniCheckupBPAirPump;
import com.veepoo.protocol.model.datas.MiniCheckupBPPhotoelectric;
import com.veepoo.protocol.model.datas.MiniCheckupBasePersonalInfo;
import com.veepoo.protocol.model.datas.MiniCheckupBloodComponent;
import com.veepoo.protocol.model.datas.MiniCheckupBodyComponent;
import com.veepoo.protocol.model.datas.MiniCheckupDetailData;
import com.veepoo.protocol.model.datas.MiniCheckupResultData;
import com.veepoo.protocol.model.datas.MiniCheckupSkinElectricity;
import com.veepoo.protocol.model.enums.EMiniCheckupTestErrorCode;

import java.util.Locale;

public class MiniCheckupActivity extends Activity implements IMiniCheckupOptListener, View.OnClickListener {
    Button btnStartMiniCheckup;
    Button btnStopMiniCheckup;
    TextView tvMiniCheckupInfo;

    StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mini_checkup);
        btnStopMiniCheckup = findViewById(R.id.btnStopRealTimePPGRawDataTransfer);
        btnStartMiniCheckup = findViewById(R.id.btnReadPPGRawData);
        tvMiniCheckupInfo = findViewById(R.id.tvMiniCheckupInfo);
        btnStopMiniCheckup.setOnClickListener(this);
        btnStartMiniCheckup.setOnClickListener(this);
    }

    @Override
    public void onMiniCheckupTestProgress(int progress) {
        tvMiniCheckupInfo.setText(String.format(Locale.CHINA, "IniciarMini check-up！\n ... %d%%", progress));
    }

    @Override
    public void onMiniCheckupStopSuccess() {
        appendMsg("Mini check-up");
    }

    @Override
    public void onMiniCheckupTestFailed(@NonNull EMiniCheckupTestErrorCode errorCode) {
        appendMsg("Mini check-up:" + errorCode);
    }

    @Override
    public void onMiniCheckupSuccess(@NonNull MiniCheckupResultData testResultData) {
        appendMsg("Mini check-up-->");
        appendMsg("Frequência cardíaca:" + testResultData.getHeartRate() + "bmp");
        appendMsg("SpO2:" + testResultData.getBloodOxygen() + "%");
        appendMsg(":" + testResultData.getStress());
        appendMsg(":" + testResultData.getEmotion());
        appendMsg("fadiga:" + testResultData.getFatigue());
        appendMsg(":" + testResultData.getBloodGlucose() + "mmol/L");
        appendMsg(":" + testResultData.getBodyTemperature() + "℃");
        appendMsg("pressão arterial（）:" + testResultData.getSystolicBloodPressure());
        appendMsg("pressão arterial（）:" + testResultData.getDiastolicBloodPressure());
        appendMsg("HRV:" + testResultData.getHrv());
    }

    @Override
    public void onMiniCheckupDetailTestSuccess(@NonNull MiniCheckupDetailData data) {
        appendMsg("Mini check-up-->");
        appendMsg(":" + getBasePersonalInfo(data.getBasePersonalInfo()));
        appendMsg("Frequência cardíaca:" + data.getHeartRate() + " bmp");
        appendMsg("SpO2:" + data.getBloodOxygen() + "%");
        appendMsg(":" + data.getStress());
        appendMsg(":" + data.getEmotion());
        appendMsg("fadiga:" + data.getFatigue());
        appendMsg(":" + getBloodGlucoseTypeDes(data.getBloodGlucoseType()));
        appendMsg(":" + data.getBloodGlucose() + " mmol/L");
        appendMsg(":" + ((data.getBodyTemperature() == -273.15f) ? "" : (data.getBodyTemperature() + "℃")));
        appendMsg("temperatura:" + ((data.getOriginalTemperature() == -273.15f) ? "" : (data.getBodyTemperature() + "℃")));
        appendMsg("HRV:" + data.getHrv());
        appendMsg("pressão arterial:" + getBpDS(data.getBpAirPump()));
        appendMsg("pressão arterial:" + getBpDS(data.getBpPhotoelectric()));
        appendMsg(":" + getBloodComponent(data.getBloodComponent()));
        appendMsg(":" + getBodyComponent(data.getBodyComponent()));
        appendMsg(":" + getSkinElectricity(data.getSkinElectricity()));
    }

    private String getBasePersonalInfo(MiniCheckupBasePersonalInfo personalInfo) {
        if (personalInfo == null) return "";
        return "Sexo:" + personalInfo.getGender()
                + " :" + personalInfo.getAge()
                + " :" + personalInfo.getHeight()
                + " :" + personalInfo.getWeight();
    }

    private String getBpDS(MiniCheckupBPAirPump bpAirPump) {
        if (bpAirPump == null) return "";
        return bpAirPump.getDiastolicBloodPressure() + " - " + bpAirPump.getSystolicBloodPressure();
    }

    private String getBpDS(MiniCheckupBPPhotoelectric bpPhotoelectric) {
        if (bpPhotoelectric == null) return "";
        return bpPhotoelectric.getDiastolicBloodPressure() + " - " + bpPhotoelectric.getSystolicBloodPressure();
    }

    public String getBloodComponent(MiniCheckupBloodComponent bloodComponent) {
        if (bloodComponent == null) return "";
        return ":" + bloodComponent.getUricAcid()
                + " :" + bloodComponent.gettCHO()
                + " :" + bloodComponent.gettAG()
                + " :" + bloodComponent.gethDL()
                + " :" + bloodComponent.getlDL();
    }

    public String getSkinElectricity(MiniCheckupSkinElectricity skinElectricity) {
        if (skinElectricity == null) return "";
        return ":" + skinElectricity.getEmotion()
                + " :" + skinElectricity.getSkinMoistureContent()
                + " :" + skinElectricity.getDepressionRisk()
                + " :" + skinElectricity.getSympatheticActivity()
                + " :" + skinElectricity.getCortisolConcentration();
    }

    public String getBodyComponent(MiniCheckupBodyComponent bodyComponent) {
        if (bodyComponent == null) return "";
        return "Sexo:" + bodyComponent.getGender()
                + " :" + bodyComponent.getAge()
                + " :" + bodyComponent.getHeight()
                + " :" + bodyComponent.getWeight()
                + " BMI:" + bodyComponent.getBMI()
                + " :" + bodyComponent.getBodyFatRate()
                + " :" + bodyComponent.getFatRate()
                + " :" + bodyComponent.getFFM()
                + " :" + bodyComponent.getMuscleRate()
                + " :" + bodyComponent.getMuscleMass()
                + " :" + bodyComponent.getSubcutaneousFat()
                + " :" + bodyComponent.getBodyWater()
                + " :" + bodyComponent.getWaterContent()
                + " :" + bodyComponent.getSkeletalMuscleRate()
                + " :" + bodyComponent.getBoneMass()
                + " :" + bodyComponent.getProteinProportion()
                + " :" + bodyComponent.getProteinMass()
                + " :" + bodyComponent.getBasalMetabolicRate()
                ;
    }

    private String getBloodGlucoseTypeDes(int type) {
        switch (type) {
            case 0:
                return "";
            case 1:
                return "";
            case 2:
                return "";
        }
        return "";
    }

    @Override
    public void onClick(View v) {
        if (v == btnStartMiniCheckup) {
            sb.setLength(0);
            VPOperateManager.getInstance().startMiniCheckup(code -> {
                if (code == Code.REQUEST_SUCCESS) {
                    appendMsg("IniciarMini check-up！");
                } else {
                    appendMsg("IniciarMini check-up！");
                }
            }, this);
        }
        if (v == btnStopMiniCheckup) {
            VPOperateManager.getInstance().stopMiniCheckup(code -> {
                if (code == Code.REQUEST_SUCCESS) {
                    appendMsg("Mini check-up！");
                } else {
                    appendMsg("Mini check-up！");
                }
            }, this);
        }
    }

    private void appendMsg(String msg) {
        sb.append(msg).append("\n");
        tvMiniCheckupInfo.setText(sb.toString());
    }

}
