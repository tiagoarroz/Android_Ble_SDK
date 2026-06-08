package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.multi_lead.data.EcgMultiLeadDetectResult;
import com.veepoo.protocol.multi_lead.data.EcgMultiLeadDetectState;
import com.veepoo.protocol.multi_lead.data.EcgMultiLeadPreInfo;
import com.veepoo.protocol.multi_lead.enums.ELeadFlag;
import com.veepoo.protocol.multi_lead.listener.IECGMultiLeadDetectListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Description ECG
 *
 * @author KYM.
 * @date 2023/11/2 15:55
 */
public class EcgMultiLeadDetect1Activity extends Activity implements View.OnClickListener {
    private final static String TAG = EcgMultiLeadDetect1Activity.class.getSimpleName();
    private List<EcgDetectView> viewList = new ArrayList<>();
    TextView start, stop, tvProgress, tvInfo;
    Context mContext;
    WriteResponse writeResponse = new WriteResponse();
    private boolean isDetecting = false;

    private int allLeadOffCount = 0;

    /**
     * 
     */
    private int detectSeconds = 0;

    /**
     * 
     */
    private int leadOffCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg_multi_lead_detect);
        mContext = EcgMultiLeadDetect1Activity.this;
        EcgDetectView v1 = (EcgDetectView) findViewById(R.id.ehrv1);
        EcgDetectView v2 = (EcgDetectView) findViewById(R.id.ehrv2);
        EcgDetectView v3 = (EcgDetectView) findViewById(R.id.ehrv3);
        EcgDetectView v4 = (EcgDetectView) findViewById(R.id.ehrv4);
        EcgDetectView v5 = (EcgDetectView) findViewById(R.id.ehrv5);
        EcgDetectView v6 = (EcgDetectView) findViewById(R.id.ehrv6);
        viewList.add(v1);
        viewList.add(v2);
        viewList.add(v3);
        viewList.add(v4);
        viewList.add(v5);
        viewList.add(v6);
        start = (TextView) findViewById(R.id.start);
        stop = (TextView) findViewById(R.id.stop);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        tvInfo = (TextView) findViewById(R.id.tvInfo);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.start) {
            if (isDetecting) {
                return;
            }
            for (int i = 0; i < viewList.size(); i++) {
                viewList.get(i).clearData();
            }
            isDetecting = true;
            leadOffCount = 0;
            VPOperateManager.getInstance().startMultiLeadDetectECG(writeResponse, true, new IECGMultiLeadDetectListener() {


                @Override
                public void onEcgDetectSuccess() {
                    isDetecting = false;
                    Log.e("Test", "onEcgDetectSuccess");
                    Toast.makeText(EcgMultiLeadDetect1Activity.this, "", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onEcgDetectPreInfoChange(EcgMultiLeadPreInfo ecgDetectInfo) {
                    Log.e("Test", "onEcgDetectPreInfoChange:" + ecgDetectInfo.toString());
                    if (isDetecting) {
                        tvInfo.setText("...");
                    }
                    detectSeconds = 0;
                }

                @Override
                public void onEcgDetectStateChange(EcgMultiLeadDetectState ecgDetectState) {
                    Log.e("Test", "onEcgDetectStateChange:" + ecgDetectState.toString());
                    tvProgress.setText(ecgDetectState.getProgress() + "%");
                    if (isDetecting) {
                        tvInfo.setText("...\nFrequência cardíaca:" + ecgDetectState.getHeart() + "   QT:" + ecgDetectState.getQt() + "   HRV:" + ecgDetectState.getHrv());
                    }
                    detectSeconds++;

                    //4
                    if (detectSeconds > 4) {
                        //4，I；I;
                        if (ecgDetectState.getLeadI() == 1) {
                            tvInfo.setText("");
                            leadOffCount++;
                            //44，
                            if (leadOffCount > 4) {
                                isDetecting = false;
                                VPOperateManager.getInstance().stopMultiLeadDetectECG(writeResponse);
                                tvInfo.setText("，Terminar");
                            }
                        } else {
                            leadOffCount = 0;
                        }

                    }
                }

                @Override
                public void onDiseaseDiagnosisResults(EcgMultiLeadDetectResult ecgDetectResult) {
                    Log.e("Test", "onEcgDetectResultChange:" + ecgDetectResult.toString());
                    tvInfo.setText("!\nFrequência cardíaca:" + ecgDetectResult.getAvgHeart() + "   QT:" + ecgDetectResult.getAvgQT() + "   HRV:" + ecgDetectResult.getAvgHRV());
                }

                @Override
                public void onEcgDetectFail() {
                    isDetecting = false;
                    Log.e("Test", "onEcgDetectFail");
                    tvInfo.setText("");
                }

                @Override
                public void onEcgADCChange(@NonNull ELeadFlag leadFlag, @NonNull int[] data, int gain, int packNum) {
                    viewList.get(leadFlag.getValue() - 1).changeData(data, data.length);
                }


            });
        } else if(id == R.id.stop) {
            for (int i = 0; i < viewList.size(); i++) {
                viewList.get(i).clearData();
            }
            isDetecting = false;
            VPOperateManager.getInstance().stopMultiLeadDetectECG(writeResponse);
        }

    }

    /**
     * estado
     */
    class WriteResponse implements IBleWriteResponse {

        @Override
        public void onResponse(int code) {
            Logger.t(TAG).i("write cmd status:" + code);
        }
    }
}
