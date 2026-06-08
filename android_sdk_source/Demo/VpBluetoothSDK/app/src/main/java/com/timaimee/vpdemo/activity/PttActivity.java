package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IPttDetectListener;
import com.veepoo.protocol.model.datas.EcgDetectInfo;
import com.veepoo.protocol.model.datas.EcgDetectResult;
import com.veepoo.protocol.model.datas.EcgDetectState;
import com.veepoo.protocol.model.datas.EcgDiagnosis;

import java.util.Arrays;


public class PttActivity extends Activity {
    private final static String TAG = PttActivity.class.getSimpleName();
    WriteResponse writeResponse = new WriteResponse();
    TextView mPttModelTv;
    EcgHeartRealthView ecgHeartRealthView;

    IPttDetectListener iPttDetectListener = new IPttDetectListener() {
        @Override
        public void onEcgDetectInfoChange(EcgDetectInfo ecgDetectInfo) {
            Logger.t(TAG).i("ECGInformação base(,):" + ecgDetectInfo.toString());


        }

        @Override
        public void onEcgDetectStateChange(EcgDetectState ecgDetectState) {
            Logger.t(TAG).i("ECGestado,Configurar:" + ecgDetectState.toString());


        }

        @Override
        public void onEcgDetectResultChange(EcgDetectResult ecgDetectResult) {
            Logger.t(TAG).i("ptt(ECG,PTT，（）,)");


        }

        @Override
        public void onEcgDetectDiagnosisChange(EcgDiagnosis ecgDiagnosis) {
            Logger.t(TAG).i("====>>>onEcgDetectDiagnosisChange" + ecgDiagnosis.toString());
        }

        @Override
        public void onEcgADCChange(int[] ints, int[] ints1) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Logger.t(TAG).i("PTTdados:" + Arrays.toString(ints));
                    ecgHeartRealthView.changeData(ints, ints1, 25);
                }
            });
        }

//        @Override
//        public void onEcgADCChange(final int[] data) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Logger.t(TAG).i("PTTdados:" + Arrays.toString(data));
//                    ecgHeartRealthView.changeData(data, 25);
//                }
//            });
//
//        }

        @Override
        public void inPttModel() {
            Logger.t(TAG).i("ptt");
            mPttModelTv.setText("PTT");
        }

        @Override
        public void outPttModel() {
            Logger.t(TAG).i("ptt");
            mPttModelTv.setText("PTT");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ptt);
        mPttModelTv = findViewById(R.id.ptt_model);
        ecgHeartRealthView = findViewById(R.id.ptt_real_view);
        boolean inPttModel = getIntent().getBooleanExtra("inPttModel", false);
        String ptStr = inPttModel ? "PTT" : "PTT";
        mPttModelTv.setText(ptStr);
        listenModel();
    }

    private void listenModel() {
        VPOperateManager.getMangerInstance(getApplicationContext()).settingPttModelListener(iPttDetectListener);
    }

    public void enter(View view) {
        ecgHeartRealthView.clearData();
        Logger.t(TAG).i("Ler sinal PTT");
        VPOperateManager.getInstance().startReadPttSignData(writeResponse, true, iPttDetectListener);
    }

    public void exitModel(View view) {
        Logger.t(TAG).i("Desativar sinal PTT");
        VPOperateManager.getInstance().stopReadPttSignData(writeResponse, false, iPttDetectListener);
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
