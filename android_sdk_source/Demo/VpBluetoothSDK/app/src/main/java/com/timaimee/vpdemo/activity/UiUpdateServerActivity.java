package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IUIBaseInfoFormServerListener;
import com.veepoo.protocol.listener.data.IUiUpdateListener;
import com.veepoo.protocol.listener.oad.OnDownLoadListener;
import com.veepoo.protocol.model.TUiTheme;
import com.veepoo.protocol.model.datas.UIDataServer;
import com.veepoo.protocol.model.enums.EUIFromType;
import com.veepoo.protocol.model.enums.EUiUpdateError;
import com.veepoo.protocol.util.UiServerHttpUtil;
import com.veepoo.protocol.util.UiUpdateUtil;
import com.veepoo.protocol.util.VPLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

/**
 * ConfigurarUI，：
 * 1.Configurar
 * 2.（）
 * 3.UI
 * 4.UI
 * 5.ConfigurarUI
 */
public class UiUpdateServerActivity extends Activity {
    private final static String TAG = UiUpdateServerActivity.class.getSimpleName();
    Context mContext;
    IBleWriteResponse bleWriteResponse = new IBleWriteResponse() {
        @Override
        public void onResponse(int code) {
            Logger.t(TAG).i("onResponse code:" + code);
        }
    };

    public TextView mUiServerSupportTV;
    public TextView mUiServerBaseInfoTV;
    public TextView downListTv;
    public TextView fileStateTv;
    public TextView sendProgressTv;

    String deviceNumber, deviceVersion, deviceTestVersion;
    UiServerHttpUtil uiUpdateCheckOprate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uiupdate_server);
        {
            mUiServerSupportTV = findViewById(R.id.ui_issupport);
            mUiServerBaseInfoTV = findViewById(R.id.ui_baseinfo);
            downListTv = findViewById(R.id.ui_downlist);
            fileStateTv = findViewById(R.id.ui_file_state);
            sendProgressTv = findViewById(R.id.ui_send_progress);
        }
        mContext = UiUpdateServerActivity.this;
        UiUpdateUtil.getInstance().init(this);
        uiUpdateCheckOprate = new UiServerHttpUtil();
        deviceNumber = getIntent().getStringExtra("deviceNumber");
        deviceVersion = getIntent().getStringExtra("deviceVersion");
        deviceTestVersion = getIntent().getStringExtra("deviceTestVersion");
    }


    public void isSupportServerUi(View view) {
        if (UiUpdateUtil.getInstance().isSupportChangeServerUi()) {
            mUiServerSupportTV.setText("1.");
        } else {
            mUiServerSupportTV.setText("1.");
            Toast.makeText(mContext, "", Toast.LENGTH_LONG).show();
        }
    }

    UIDataServer mUiDataServer;

    public void readBaseInfoFromServer(View view) {
        UiUpdateUtil.getInstance().getServerWatchUiInfo(new IUIBaseInfoFormServerListener() {

            @Override
            public void onBaseUiInfoFormServer(UIDataServer uiDataServer) {
                mUiDataServer = uiDataServer;
                mUiServerBaseInfoTV.setText("2.Informação base uiDataServer:" + uiDataServer.toString());
            }
        });
    }


    TUiTheme tUiThemeDown;
    List<TUiTheme> themeInfoList;
    /**
     * demo，1，
     */
    public void serverUI(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String appPackName = "com.timaimee.watch";
                String appVersion = "3.1.9";
                themeInfoList = uiUpdateCheckOprate.getThemeInfo(mUiDataServer, deviceNumber, deviceTestVersion, appPackName, appVersion);
                final StringBuffer stringBuffer = new StringBuffer();
                for (TUiTheme tUiTheme : themeInfoList) {
                    Logger.t(TAG).i("tUiTheme item:" + tUiTheme.toString());
                    stringBuffer.append(tUiTheme.getFileUrl());
                    stringBuffer.append("\n");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downListTv.setText(stringBuffer.toString());
                    }
                });
                if (themeInfoList != null && themeInfoList.size() > 0) {
                    tUiThemeDown = themeInfoList.get(0);
                }
            }
        }).start();
    }

    File mUpdatefile;
    public void onDownFile(View view) {
        if(themeInfoList == null || themeInfoList.size() == 0) {
            return;
        }
        int index = new Random().nextInt(themeInfoList.size());
        tUiThemeDown = themeInfoList.get(index);


        String filePath = getExternalFilesDir(null) + File.separator;
        final String fileUrl = tUiThemeDown.getFileUrl();
        if (!TextUtils.isEmpty(fileUrl)) {
            String fileName = getFileName(fileUrl);
            final String fileSave = filePath + fileName;
            Logger.t(TAG).i("tUiTheme fileUrl:" + fileUrl);
            Logger.t(TAG).i("tUiTheme fileSave:" + fileSave);
            mUpdatefile = new File(fileSave);
            if (mUpdatefile.exists()) {
                fileStateTv.setText("，");
                Logger.t(TAG).i("，");
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uiUpdateCheckOprate.downloadFile(fileUrl, fileSave, new OnDownLoadListener() {
                            @Override
                            public void onProgress(final float progress) {
                                Logger.t(TAG).i(":" + progress);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        fileStateTv.setText(":" + progress * 100);
                                    }
                                });
                            }

                            @Override
                            public void onFinish() {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        fileStateTv.setText("");
                                    }
                                });
                                VPLogger.i("");
                                mUpdatefile = new File(fileSave);
                            }
                        });
                    }
                }).start();
            }
        }
    }

    private String getFileName(String fileUrl) {
        String[] split = fileUrl.split("/");
        if (split.length > 0) {
            int length = split.length;
            return split[length - 1];
        }
        return "ui.bin";
    }

    public void onServerSet(View view) {
        if (mUpdatefile == null || !mUpdatefile.exists()) {
            Logger.t(TAG).i("");
            return;
        }
        try {
            Uri mUritempFile = Uri.fromFile(mUpdatefile);
            InputStream inputStream = mContext.getContentResolver().openInputStream(mUritempFile);
            Logger.t(TAG).i("IniciarConfigurar");

            /**
             * ui：Iniciar atualização-dados-EnviarUIdados-TerminarEnviar
             */
            UiUpdateUtil.getInstance().startSetUiStream(EUIFromType.SERVER, inputStream, new IUiUpdateListener() {

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
                    sendProgressTv.setText("Enviar：" + progress + "%");
                }


                @Override
                public void onUiUpdateSuccess() {
                    Logger.t(TAG).i("onUiUpdateSuccess");
                    sendProgressTv.setText("Configurar");
                }

                @Override
                public void onUiUpdateFail(EUiUpdateError eUiUpdateError) {
                    Logger.t(TAG).i("onUiUpdateFail:" + eUiUpdateError);
                    switch (eUiUpdateError) {
                        case LISTENTER_IS_NULL:
                            break;
                        case NEED_READ_BASE_INFO:
                            break;
                        case FILE_UNEXIST:
                            break;
                        case LOW_BATTERY:
                            break;
                        case INTO_UPDATE_MODE_FAIL:
                            break;
                        case FILE_LENGTH_NOT_4_POWER:
                            break;
                        case CHECK_CRC_FAIL:
                            Logger.t(TAG).i("，crc");
                            break;
                        case APP_CRC_SAME_DEVICE_CRC:
                            Logger.t(TAG).i("CRC，Enviar");
                            break;
                    }
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }


}
