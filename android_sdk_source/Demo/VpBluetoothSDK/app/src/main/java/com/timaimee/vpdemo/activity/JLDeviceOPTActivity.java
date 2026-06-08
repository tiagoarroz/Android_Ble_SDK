package com.timaimee.vpdemo.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.jieli.RcspAuthManager;
import com.inuker.bluetooth.library.jieli.dial.JLWatchFaceManager;
import com.inuker.bluetooth.library.jieli.dial.JLWatchHolder;
import com.inuker.bluetooth.library.jieli.ota.JLOTAHolder;
import com.inuker.bluetooth.library.jieli.response.RcspAuthResponse;
import com.jieli.jl_fatfs.model.FatFile;
import com.jieli.jl_rcsp.model.base.BaseError;
import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.timaimee.vpdemo.activity.image_selector.ImageVideoSelectorManager;
import com.timaimee.vpdemo.activity.image_selector.MediaInfo;
import com.timaimee.vpdemo.utils.ImageUtils;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.customui.WatchUIType;
import com.veepoo.protocol.listener.data.IMtuChangeListener;
import com.veepoo.protocol.listener.data.IUIBaseInfoFormCustomListener;
import com.veepoo.protocol.model.datas.UIDataCustom;
import com.veepoo.protocol.model.datas.UIDataImagePush;
import com.veepoo.protocol.model.enums.EWatchUIElementPosition;
import com.veepoo.protocol.model.enums.EWatchUIElementType;
import com.veepoo.protocol.model.enums.EWatchUIType;
import com.veepoo.protocol.util.UiUpdateUtil;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import tech.gujin.toast.ToastUtil;

public class JLDeviceOPTActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "JLDeviceOPTActivity";
    Button btnOpenNotify, btnFileSystem, btnPhotoDial, btnServerDial, btnSwitchPhotoDial, btnSwitchServerDial, btnOTA, btnAuth, btnSelectImage;
    TextView tvOpenInfo, tvFileSystemInfo, tvDialProgress, tvServerDialProgress, tvOTAProgress, tvOTAInfo,
            tvDialInfo, tvServerDialInfo, tvAuthInfo;
    ProgressBar pbPhotoDial, pbOTAProgress, pbServerDial;

    ImageView ivWatchFace;
    EditText etWidth;
    EditText etHeight;

    CustomProgressDialog loadingDialog;

    int serverDialFlag = 0;
    int photoDialFlag = 0;
    UIDataCustom mUIDataCustom = null;
    String pushImagePath = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageVideoSelectorManager.launch(this);
        setContentView(R.layout.activity_jl_device);
        loadingDialog = new CustomProgressDialog(this);
        btnOpenNotify = findViewById(R.id.btnOpenNotify);
        btnFileSystem = findViewById(R.id.btnFileSystem);
        btnPhotoDial = findViewById(R.id.btnPhotoDial);
        btnServerDial = findViewById(R.id.btnServerDial);
        btnOTA = findViewById(R.id.btnOTA);
        btnAuth = findViewById(R.id.btnAuth);
        tvOpenInfo = findViewById(R.id.tvOpenInfo);
        tvFileSystemInfo = findViewById(R.id.tvFileSystemInfo);
        tvDialProgress = findViewById(R.id.tvDialProgress);
        tvOTAProgress = findViewById(R.id.tvOTAProgress);
        tvServerDialProgress = findViewById(R.id.tvServerDialProgress);
        tvServerDialInfo = findViewById(R.id.tvServerDialInfo);
        tvOTAInfo = findViewById(R.id.tvOTAInfo);
        tvDialInfo = findViewById(R.id.tvDialInfo);
        tvAuthInfo = findViewById(R.id.tvAuthInfo);
        pbPhotoDial = findViewById(R.id.pbPhotoDial);
        pbOTAProgress = findViewById(R.id.pbOTAProgress);
        pbServerDial = findViewById(R.id.pbServerDial);
        btnSwitchPhotoDial = findViewById(R.id.btnSwitchPhotoDial);
        btnSwitchServerDial = findViewById(R.id.btnSwitchServerDial);

        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivWatchFace = findViewById(R.id.ivWatchFace);
        etHeight = findViewById(R.id.etHeight);
        etWidth = findViewById(R.id.etWidth);

        btnOpenNotify.setOnClickListener(this);
        btnAuth.setOnClickListener(this);
        btnFileSystem.setOnClickListener(this);
        btnPhotoDial.setOnClickListener(this);
        btnServerDial.setOnClickListener(this);
        btnSwitchPhotoDial.setOnClickListener(this);
        btnSwitchServerDial.setOnClickListener(this);
        btnOTA.setOnClickListener(this);

        tvOpenInfo.setText(VPOperateManager.getInstance().isJLNotifyOpened() ? "Ativar" : "Ativar");
        tvAuthInfo.setText(RcspAuthManager.getInstance().isAuthPass() ? "dispositivo" : "dispositivo");

        File dir = new File("/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/imageMsgPush/");
        if(!dir.exists()) {
            dir.mkdirs();
        }

        readUIInfo();
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUIDataCustom == null) {
                    showMsg("");
                    return;
                }
                String widthStr = etWidth.getText().toString();
                String heightStr = etHeight.getText().toString();
                if (TextUtils.isEmpty(widthStr) || TextUtils.isEmpty(heightStr)) {
                    selectAndCropPicture(466, 466, true);
                } else {
                    selectAndCropPicture(Integer.parseInt(widthStr), Integer.parseInt(heightStr), false);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ImageVideoSelectorManager.handlerActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ImageVideoSelectorManager.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageVideoSelectorManager.release();
    }

    public void readUIInfo() {
        UiUpdateUtil.getInstance().init(this);
        UiUpdateUtil.getInstance().getCustomWatchUiInfo(new IUIBaseInfoFormCustomListener() {
            @Override
            public void onBaseUiInfoFormCustom(UIDataCustom uiDataCustom) {
                Logger.t(TAG).i("2.Informação base uiDataCustom:" + uiDataCustom.toString());
                mUIDataCustom = uiDataCustom;
                WatchUIType watchUIType = WatchUIType.getInstance(mUIDataCustom.getCustomUIType());
                etWidth.setText(watchUIType.getBigBitmapWidth() + "");
                etHeight.setText(watchUIType.getBigBitmapHeight() + "");
                EWatchUIType customUIType = mUIDataCustom.getCustomUIType();
                EWatchUIElementPosition timePosition = mUIDataCustom.getTimePosition();
                EWatchUIElementType downTimeType = mUIDataCustom.getDownTimeType();
                EWatchUIElementType upTimeType = mUIDataCustom.getUpTimeType();
                int color888 = mUIDataCustom.getColor888();
            }
        });

    }

    private void selectAndCropPicture(int aspectRatioX, int aspectRatioY, boolean isCircle) {
        Logger.t(TAG).i("selectAndCropPicture  aspectRatioX = " + aspectRatioX + " , aspectRatioY = " + aspectRatioY);
        ImageVideoSelectorManager
                .getInstance()
                .width(aspectRatioX)
                .height(aspectRatioY)
                .isCircle(isCircle)
                .selectAndCropSingleImage(new ImageVideoSelectorManager.OnSingleImageSelectionListener() {
                    @Override
                    public void onSingleImageSelected(MediaInfo info) {

                    }

                    @Override
                    public void onCropSuccess(String cropFileName, String outputPath, Bitmap bitmap, Uri cropFileUri) {
                        Drawable drawable = new BitmapDrawable(null, bitmap);
                        Logger.t(TAG).e("-onCropSuccess-  cropFileName = " + cropFileName);
                        Logger.t(TAG).e("-onCropSuccess-  outputPath = " + outputPath);
                        Logger.t(TAG).e("-onCropSuccess-  bitmap = " + bitmap.getByteCount());
                        if (isCircle) {
                            Bitmap finalBitmap = ImageUtils.getCircularBitmap(bitmap);
                            ImageUtils.saveBitmap(finalBitmap, outputPath);
                            ivWatchFace.setImageBitmap(finalBitmap);
                        } else {
                            ivWatchFace.setImageBitmap(bitmap);
                        }
                        pushImagePath = outputPath;
                    }

                    @Override
                    public void onCropFailed(String errorMsg) {

                    }

                    @Override
                    public void onError(String message) {

                    }
                });
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnOpenNotify) {
            openJLNotify();
        } else if (id == R.id.btnAuth) {
            startDeviceAuth();
        } else if (id == R.id.btnFileSystem) {
            getJLFileSystem();
        } else if (id == R.id.btnPhotoDial) {
            setPhotoDial();
        } else if (id == R.id.btnOTA) {
            startOTA();
        } else if (id == R.id.btnServerDial) {
            setServerDial();
        } else if (id == R.id.btnSwitchPhotoDial) {
            switch2PhotoDial();
        } else if (id == R.id.btnSwitchServerDial) {
            switch2ServerDial();
        }
    }

    private void switch2PhotoDial() {
        showMsg("");
        JLWatchFaceManager.switch2PicDial();
    }

    private void switch2ServerDial() {
        showMsg("");
        JLWatchFaceManager.switch2ServerDial();
    }

    private void openJLNotify() {
        if (VPOperateManager.getInstance().isJLNotifyOpened()) {
            ToastUtil.show("Ativar");
            return;
        }
        VPOperateManager.getInstance().openJLDataNotify(new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {

            }

            @Override
            public void onResponse(int code) {
                tvOpenInfo.setText("Ativar");
                VPOperateManager.getInstance().changeMTU(247, new IMtuChangeListener() {
                    @Override
                    public void onChangeMtuLength(int cmdLength) {

                    }
                });

            }
        });
    }

    /**
     * Iniciardispositivo
     */
    private void startDeviceAuth() {
        if (RcspAuthManager.getInstance().isAuthPass()) {
            ToastUtil.show("dispositivo");
            return;
        }

        VPOperateManager.getInstance().startJLDeviceAuth(new RcspAuthResponse() {
            @Override
            public void onRcspAuthStart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.showNoTips();
                        tvAuthInfo.setText("Iniciardispositivo");
                    }
                });
            }

            @Override
            public void onRcspAuthSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.disMissDialog();
                        tvAuthInfo.setText("dispositivo");
                    }
                });
            }

            @Override
            public void onRcspAuthFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.disMissDialog();
                        tvAuthInfo.setText("dispositivo");
                    }
                });
            }
        });
    }

    /**
     * Atualizar
     */
    private void getJLFileSystem() {
        //
        VPOperateManager.getInstance().listJLWatchList(new JLWatchFaceManager.OnWatchDialInfoGetListener() {
            @Override
            public void onGettingWatchDialInfo() {
                //... 
                ToastUtil.show("...");
                loadingDialog.showNoTips();
            }

            @Override
            public void onWatchDialInfoGetStart() {
                //Iniciar
                ToastUtil.show("-Iniciar");
                tvFileSystemInfo.setText("-Iniciar");
                loadingDialog.showNoTips();
            }

            @Override
            public void onWatchDialInfoGetComplete() {
                //
                Logger.t(TAG).e("--->Complete");
                ToastUtil.show("-");
                loadingDialog.disMissDialog();
            }

            @Override
            public void onWatchDialInfoGetSuccess(List<FatFile> systemFatFiles, List<FatFile> serverFatFiles, FatFile picFatFile) {
                //
                StringBuilder sb = new StringBuilder("Atualizar=============================Start");
                sb.append("\t\t\t\n").append("[] picFatFile = ").append(picFatFile == null ? "NULL" : picFatFile.getPath());
                for (FatFile serverFatFile : serverFatFiles) {
                    sb.append("\t\t\t\n").append("[] serverFatFile = ").append(serverFatFile == null ? "NULL" : serverFatFile.getPath());
                }
                for (FatFile systemFatFile : systemFatFiles) {
                    sb.append("\t\t\t\n").append("[] systemFatFile = ")
                            .append(systemFatFile == null ? "NULL" : systemFatFile.getPath());
                }
                sb.append("\t\t\t\n").append("[] serverFatFile = ")
                        .append(serverFatFiles.isEmpty() ? "Configurar" : serverFatFiles.get(0).getPath())
                        .append("\n")
                        .append("Atualizar=============================End");
                Log.e(TAG, sb.toString());
                tvFileSystemInfo.setText(sb.toString());
                for (FatFile systemFatFile : systemFatFiles) {
                    Logger.t(TAG).e("--->" + systemFatFile.toString());

                }
                for (FatFile serverFatFile : serverFatFiles) {
                    Logger.t(TAG).e("--->" + serverFatFile.toString());
                }
                Logger.t(TAG).e("--->" + picFatFile.toString());
                loadingDialog.disMissDialog();
            }

            @Override
            public void onWatchDialInfoGetFailed(BaseError error) {
                //
                Logger.t(TAG).e("--->Error:" + error.toString());
                ToastUtil.show("-");
                tvFileSystemInfo.setText("-:\n" + error.toString());
                loadingDialog.disMissDialog();
            }
        });
    }

    /**
     * Configurar
     */
    private void setPhotoDial() {
//        String dialPhotoPath = "/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/hband/jlDail/img_wathch_face1.png";
//        if (photoDialFlag % 2 == 0) {
//            dialPhotoPath = "/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/hband/jlDail/img_wathch_face2.png";
//        }

        if (TextUtils.isEmpty(pushImagePath)) {
            showMsg("");
            return;
        }

        File dialFile = new File(pushImagePath);
        if (!dialFile.exists() || !dialFile.isFile() || dialFile.length() <=100) {
            showMsg("");
            return;
        }

        photoDialFlag++;
        tvDialInfo.setText(pushImagePath);
        VPOperateManager.getInstance().setJLWatchPhotoDial(pushImagePath, new JLWatchFaceManager.JLTransferPicDialListener() {
            @Override
            public void onLowPower() {

            }

            @Override
            public void onJLTransferPicDialStart() {
                tvDialInfo.setText("Iniciar");
                Logger.t(TAG).e("onJLTransferPicDialStart--->" + Thread.currentThread().toString());
            }

            @Override
            public void onTransferPicDialProgress(int progress) {
                Logger.t(TAG).e("--->progress = " + progress + " : Thread = " + Thread.currentThread().toString());
                pbPhotoDial.setProgress(progress);
                tvDialProgress.setText(progress + " %");
                tvDialInfo.setText("");
            }

            @Override
            public void onScaleBGPFileTransferComplete() {
                Logger.t(TAG).e("--->" + " : Thread = " + Thread.currentThread().toString());
                tvDialInfo.setText("");
            }

            @Override
            public void onAIPreviewTransferComplete() {

            }

            @Override
            public void onBigBGPFileTransferComplete() {
                Logger.t(TAG).e("--->" + " : Thread = " + Thread.currentThread().toString());
                tvDialInfo.setText("");
            }

            @Override
            public void onTransferComplete() {
                Logger.t(TAG).e("--->" + " : Thread = " + Thread.currentThread().toString());
                tvDialInfo.setText("");
            }

            @Override
            public void onTransferError(int code, String errorMsg) {
                Logger.t(TAG).e("---> code = " + code + ", errorMsg = " + errorMsg + " : Thread = " + Thread.currentThread().toString());
                tvDialInfo.setText("，code = " + code + " , errorMsg = " + errorMsg);
            }
        });
    }

    /**
     * Configurar
     */
    private void setServerDial() {
        String localServerDialPath = "/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/hband/jlDail/watch040";
        serverDialFlag++;
        if (serverDialFlag % 2 == 0) {
            localServerDialPath = "/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/hband/jlDail/watch046";
        }
        tvServerDialInfo.setText(localServerDialPath);
        VPOperateManager.getInstance().setJLWatchDial(localServerDialPath, new JLWatchHolder.OnSetJLWatchDialListener() {
            @Override
            public void onStart() {
                Logger.t(TAG).e("onStart--->" + Thread.currentThread().toString());
                tvServerDialInfo.setText("Iniciar");
            }

            @Override
            public void onProgress(int progress) {
                Logger.t(TAG).e("--->progress = " + progress + " : Thread = " + Thread.currentThread().toString());
                pbServerDial.setProgress(progress);
                tvServerDialProgress.setText(progress + " %");
                tvServerDialInfo.setText("");
            }

            @Override
            public void onComplete(String watchPath) {
                pbServerDial.setProgress(100);
                tvServerDialProgress.setText("100%");
                tvServerDialInfo.setText("Configurar:" + watchPath);
            }

            @Override
            public void onFiled(int code, String errorMsg) {
                tvServerDialInfo.setText("，code = " + code + " , msg = " + errorMsg);
            }
        });
    }

    private void startOTA() {
//        String otaFileName = "KH32_9626_00320800_OTA_UI_230421_19.zip";
        String otaFileName = "JE51P_5057_00510064_OTA_UI_KEY_240402_15.zip";
        String otaFileName1 = "9664_00.70.01.zip";
        String firmwareFilePath = "/storage/emulated/0/Android/data/com.timaimee.vpdemo/files/hband/jlOta/" + otaFileName;
        tvOTAInfo.setText(firmwareFilePath);
        VPOperateManager.getInstance().startJLDeviceOTAUpgrade(firmwareFilePath, new JLOTAHolder.OnJLDeviceOTAListener() {
            @Override
            public void onOTAStart() {
                Logger.t(TAG).e("OTA--->OTAIniciar");
                tvOTAInfo.setText("Iniciar atualização");
            }

            @Override
            public void onProgress(float progress) {
                Logger.t(TAG).e("OTA--->OTA:" + progress + "%");
                tvOTAProgress.setText(String.format(Locale.CHINA, "%.2f", progress) + "%");
                pbOTAProgress.setProgress((int) (progress * 100));
            }

            @Override
            public void onNeedReconnect(String address, String dfuLangAddress, boolean isReconnectBySdk) {
                Logger.t(TAG).e("OTA--->OTAdfuLang: address = " + address + " , dfuLangAddress = " + dfuLangAddress + " , SDK = " + isReconnectBySdk);
                tvOTAInfo.setText("dadosTerminar，IniciarDFULangdispositivo->dispositivo");
            }

            @Override
            public void onDFULangConnectSuccess(String dfuLangAddress) {
                tvOTAInfo.setText("DFULangdispositivo->dispositivo");
            }

            @Override
            public void onDFULangConnectFailed(String dfuLangAddress) {
                tvOTAInfo.setText("DFULangdispositivo，DFULangdispositivo");
            }

            @Override
            public void onOTASuccess() {
                Logger.t(TAG).e("OTA--->OTA");
                tvOTAInfo.setText("OTA");
                tvOTAProgress.setText("100%");
            }

            @Override
            public void onOTAFailed(com.jieli.jl_bt_ota.model.base.BaseError error) {
                Logger.t(TAG).e("OTA--->OTA:" + error.toString());
                tvOTAInfo.setText("Falha na atualização，error: code = " + error.getSubCode() + " , msg = " + error.getMessage());
            }
        });
    }

    public void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
