package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.veepoo.protocol.listener.data.IUIBaseInfoFormG15ImgListener;
import com.veepoo.protocol.listener.data.IUiUpdateListener;
import com.veepoo.protocol.model.datas.UIDataG15Img;
import com.veepoo.protocol.model.enums.EUIFromType;
import com.veepoo.protocol.model.enums.EUiUpdateError;
import com.veepoo.protocol.util.UiUpdateUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Author: YWX
 * Date: 2021/9/18 10:19
 * Description: G15 
 */
public class UiUpdateG15ImgActivity extends Activity implements View.OnClickListener {

    private static final String TAG = UiUpdateG15ImgActivity.class.getSimpleName();


    UiUpdateUtil mUiUpdateUtil;

    private UIDataG15Img mUIDataProfile, mUIDataQRCode1, mUIDataQRCode2, mUIDataAppDownloadQRCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uiupdate_g15img);
        findViewById(R.id.btnA2).setOnClickListener(this);
        findViewById(R.id.btnA3).setOnClickListener(this);
        findViewById(R.id.btnA4).setOnClickListener(this);
        findViewById(R.id.btnA5).setOnClickListener(this);

        mUiUpdateUtil = UiUpdateUtil.getInstance();
        mUiUpdateUtil.init(this);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnA2) {
            setG15ImgAppDownloadAppQRCode();
        } else  if (id == R.id.btnA3) {
            setG15ImgProfile();
        } else if (id == R.id.btnA4) {
            setG15ImgTheme1();
        } else if (id == R.id.btnA5) {
            setG15ImgTheme2();
        }
    }

    /**
     * 
     * 
     * @param color
     * @return
     */
    private Bitmap createBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.RGB_565);
        bitmap.eraseColor(color);
        return bitmap;
    }

    private void setG15ImgAppDownloadAppQRCode() {
        readAppDownloadQRCode();
    }

    private void readAppDownloadQRCode() {
        mUiUpdateUtil.getG15ImgAppDownloadQRCode(new IUIBaseInfoFormG15ImgListener() {
            @Override
            public void onBaseUiInfoFormG15Img(UIDataG15Img uiDataG15Img) {
                Logger.t(TAG).i("G15AppUIConfigurar:" + uiDataG15Img.toString());
                mUIDataAppDownloadQRCode = uiDataG15Img;
                /*
                 * 
                 */
                startTransmission(EUIFromType.G15_IMG_QR_CODE_APP_DOWNLOAD, createBitmap(Color.parseColor("#8F205F")));
            }
        });
    }

    /**
     * Configurar
     */
    private void setG15ImgProfile() {
        //LerConfigurar，LerstartTransmissionConfigurar
        readProfileInfo();
    }

    private void readProfileInfo() {
        mUiUpdateUtil.getG15ImgProfileInfo(new IUIBaseInfoFormG15ImgListener() {
            @Override
            public void onBaseUiInfoFormG15Img(UIDataG15Img uiDataG15Img) {
                Logger.t(TAG).i("G15UIConfigurar:" + uiDataG15Img.toString());
                mUIDataProfile = uiDataG15Img;
                startTransmission(EUIFromType.G15_IMG_PROFILE, createBitmap(Color.parseColor("#F830A0")));
            }
        });
    }

    /**
     * Projeto G15: tema de imagem 1
     */
    private void setG15ImgTheme1() {
        readTheme1Info();
    }

    private void readTheme1Info() {
        mUiUpdateUtil.getG15ImgTheme1Info(new IUIBaseInfoFormG15ImgListener() {
            @Override
            public void onBaseUiInfoFormG15Img(UIDataG15Img uiDataG15Img) {
                Logger.t(TAG).i("G15Projeto G15: tema de imagem 1UIConfigurar:" + uiDataG15Img.toString());
                mUIDataQRCode1 = uiDataG15Img;
                startTransmission(EUIFromType.G15_IMG_THEME_1, createBitmap(Color.parseColor("#0FC2FF")));
            }
        });
    }

    /**
     * G15Tema de imagem 2
     */
    private void setG15ImgTheme2() {
        readTheme2Info();
    }

    private void readTheme2Info() {
        mUiUpdateUtil.getG15ImgTheme2Info(new IUIBaseInfoFormG15ImgListener() {
            @Override
            public void onBaseUiInfoFormG15Img(UIDataG15Img uiDataG15Img) {
                Logger.t(TAG).i("G15G15Tema de imagem 2UIConfigurar:" + uiDataG15Img.toString());
                mUIDataQRCode2 = uiDataG15Img;
                startTransmission(EUIFromType.G15_IMG_THEME_2, createBitmap(Color.parseColor("#5F80FF")));
            }
        });
    }

    /**
     * bitmap
     *
     * @param bitmap
     * @return
     */
    private static byte[] getBitmapBytes(Bitmap bitmap) {
        int rowBytes = bitmap.getRowBytes();//byte，*
        int height = bitmap.getHeight();
        int size = rowBytes * height;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        //dispositivoEnviar，
        changeBigSmall(byteArray);
        //4byte
        if (byteArray.length % 4 == 0) {
            return byteArray;
        } else {
            int reSize = byteArray.length / 4 + 1;
            reSize = reSize * 4;
            byte[] byteArrays = new byte[reSize];
            Arrays.fill(byteArrays, (byte) 0xff);
            System.arraycopy(byteArray, 0, byteArrays, 0, byteArray.length);
            return byteArrays;
        }
    }

    /**
     * [1,2,3,4]-->[2,1,4,3]
     *
     * @param array
     */
    private static void changeBigSmall(byte[] array) {
        int size = array.length / 2;
        for (int j = 0; j < size; j++) {
            byte temp = array[j * 2 + 1];
            array[j * 2 + 1] = array[j * 2];
            array[j * 2] = temp;
        }
    }

    private void startTransmission(EUIFromType type, Bitmap bitmap) {
        Logger.t(TAG).i("Iniciar:" + type);
        int bytes = bitmap.getByteCount();
        ByteBuffer buf = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buf);
        byte[] byteArray = buf.array();
        InputStream inputStream = new ByteArrayInputStream(byteArray);

        try {
            long timeStampAGPS = 0;
            mUiUpdateUtil.setAGPSTimeStamp(timeStampAGPS);

            mUiUpdateUtil.startSetUiStream(type, inputStream, new IUiUpdateListener() {

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
                }


                @Override
                public void onUiUpdateSuccess() {
                    Logger.t(TAG).i("onUiUpdateSuccess");
                }

                @Override
                public void onUiUpdateFail(EUiUpdateError eUiUpdateError) {
                    Logger.t(TAG).i("onUiUpdateFail:" + eUiUpdateError);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
