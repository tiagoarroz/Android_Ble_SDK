package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.customui.WatchUIType;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IUIBaseInfoFormAGPSListener;
import com.veepoo.protocol.listener.data.IUiUpdateListener;
import com.veepoo.protocol.model.datas.UIDataAGPS;
import com.veepoo.protocol.model.enums.EUIFromType;
import com.veepoo.protocol.model.enums.EUiUpdateError;
import com.veepoo.protocol.util.UiUpdateUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class UiUpdateAGPSActivity extends Activity {
    private final static String TAG = UiUpdateAGPSActivity.class.getSimpleName();
    Context mContext;
    IBleWriteResponse bleWriteResponse = new IBleWriteResponse() {
        @Override
        public void onResponse(int code) {
            Logger.t(TAG).i("onResponse code:" + code);
        }
    };

    public TextView mUiAGPSSupportTV;
    public TextView mUiAGPSBaseInfoTV;
    public TextView mSendProgressTv;

    WatchUIType mWatchUIType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uiupdate_agps);
        {
            mUiAGPSSupportTV = findViewById(R.id.ui_issupport_agps);
            mUiAGPSBaseInfoTV = findViewById(R.id.ui_baseinfo_agps);
            mSendProgressTv = findViewById(R.id.ui_agps_progress);
        }
        mContext = UiUpdateAGPSActivity.this;
        UiUpdateUtil.getInstance().init(this);

    }

    /**
     * 
     */
    public void isSupport(View view) {
        if (UiUpdateUtil.getInstance().isSupportChangeCustomAGPS()) {
            mUiAGPSSupportTV.setText("1.AGPS");
        } else {
            mUiAGPSSupportTV.setText("1.AGPS");
            Toast.makeText(mContext, "AGPS", Toast.LENGTH_LONG).show();
        }
    }

    UIDataAGPS mUiDataAGPS;

    /**
     * Ler()
     */
    public void readBaseInfo(View view) {
        UiUpdateUtil.getInstance().getAGPSWacthUiInfo(new IUIBaseInfoFormAGPSListener() {
            @Override
            public void onBaseUiInfoFormAgps(UIDataAGPS uiDataAGPS) {
                Logger.t(TAG).i("2.AGPSInformação base uiAGPS:" + uiDataAGPS.toString());
                mUiDataAGPS = uiDataAGPS;
                mUiAGPSBaseInfoTV.setText(mUiDataAGPS.toString());
            }
        });

    }



    /**
     * apgs
     */
    public void setAgps(View view){

        //LTEPH_GPS_1.rtcm
        String fileRtcm = getExternalFilesDir(null) + File.separator + "LTEPH_GPS_1.rtcm";
        File file = new File(fileRtcm);
        if (file.exists()) {
            Uri mUritempFile = Uri.fromFile(file);
            InputStream inputStream = null;
            try {
                inputStream = mContext.getContentResolver().openInputStream(mUritempFile);

                long timeStampAGPS=0;
                UiUpdateUtil.getInstance().setAGPSTimeStamp(timeStampAGPS);

                UiUpdateUtil.getInstance().startSetUiStream(EUIFromType.A_GPS,inputStream, new IUiUpdateListener() {

                    @Override
                    public void onUiUpdateStart() {
                        Logger.t(TAG).i("onUiUpdateStart");
                    }

                    @Override
                    public void onStartClearCache(int sumCount) {
                        Logger.t(TAG).i("onStartClearCache:" + sumCount);
                    }

                    @Override
                    public void onClearCacheProgress(int currentCount, int sumCount, int progress) {
                        Logger.t(TAG).i("onClearCacheProgress:" + currentCount + "," + sumCount + "," + progress + "%");
                    }

                    @Override
                    public void onFinishClearCache() {
                        Logger.t(TAG).i("onFinishClearCache");
                    }

                    @Override
                    public void onUiUpdateProgress(int currentBlock, int sumBlock, int progress) {
                        Logger.t(TAG).i("onUiUpdateProgress:" + currentBlock + "," + sumBlock + "," + progress + "%");
                        mSendProgressTv.setText("Enviar：" + progress + "%");
                    }


                    @Override
                    public void onUiUpdateSuccess() {
                        Logger.t(TAG).i("onUiUpdateSuccess");
                        mSendProgressTv.setText("Configurar");
                    }

                    @Override
                    public void onUiUpdateFail(EUiUpdateError eUiUpdateError) {
                        Logger.t(TAG).i("onUiUpdateFail:" + eUiUpdateError);
                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Logger.t(TAG).i("can not find rtcm");
        }
    }


}
