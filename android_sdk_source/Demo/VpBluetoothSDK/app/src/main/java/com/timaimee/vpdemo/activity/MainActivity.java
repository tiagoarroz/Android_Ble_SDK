package com.timaimee.vpdemo.activity;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.veepoo.protocol.util.VpBleByteUtil.isBeyondVp;
import static com.veepoo.protocol.util.VpBleByteUtil.isBrandDevice;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.log.VPLocalLogger;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothUtils;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.orhanobut.logger.LogLevel;
import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.DeviceCompare;
import com.timaimee.vpdemo.MyService;
import com.timaimee.vpdemo.R;
import com.timaimee.vpdemo.adapter.BleScanViewAdapter;
import com.timaimee.vpdemo.adapter.CustomLogAdapter;
import com.timaimee.vpdemo.adapter.DividerItemDecoration;
import com.timaimee.vpdemo.adapter.OnRecycleViewClickCallback;
import com.timaimee.vpdemo.demo.DemoStepLogger;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IABleConnectStatusListener;
import com.veepoo.protocol.listener.base.IABluetoothStateListener;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;
import com.veepoo.protocol.util.VPLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import tech.gujin.toast.ToastUtil;

public class MainActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, OnRecycleViewClickCallback {
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static String YOUR_APPLICATION = "timaimee";
    Context mContext = MainActivity.this;
    private final int REQUEST_CODE = 1;
    List<SearchResult> mListData = new ArrayList<>();
    List<String> mListAddress = new ArrayList<>();
    SwipeRefreshLayout mSwipeRefreshLayout;
    BleScanViewAdapter bleConnectAdatpter;
    Handler mHandler = new Handler();
    private BluetoothManager mBManager;
    private BluetoothAdapter mBAdapter;
    private BluetoothLeScanner mBScanner;
    final static int MY_PERMISSIONS_REQUEST_BLUETOOTH = 0x55;
    RecyclerView mRecyclerView;
    TextView mTitleTextView;
    TextView tvTips;
    private boolean mIsOadModel;
    BluetoothLeScannerCompat mScanner;
    private boolean mHasActiveBleSession;
    private boolean mBleSessionHandedOff;

    @Override
    protected void onDestroy() {
        VPLocalLogger.stopMonitor();
        if (mHasActiveBleSession && !mBleSessionHandedOff) {
            VPOperateManager.getInstance().disconnectWatch(new IBleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    DemoStepLogger.featureEvent("BLE_DISCONNECT", "Disconnect no onDestroy. code=" + code);
                }
            });
        } else if (mBleSessionHandedOff) {
            DemoStepLogger.featureEvent("BLE_DISCONNECT", "Sessão BLE entregue ao ecrã de funcionalidades; sem disconnect no MainActivity.onDestroy");
        } else {
            DemoStepLogger.featureEvent("BLE_DISCONNECT", "onDestroy sem sessão BLE ativa; sem disconnect");
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DemoStepLogger.stepStart("MAIN_INIT", "Arranque da activity principal de demonstração");
        ToastUtil.initialize(this);
        VPOperateManager.getInstance().init(this);
//        if (BuildConfig.IS_DEBUG || true) {
        //registo
        com.jieli.jl_rcsp.util.JL_Log.setTagPrefix("HBand-JLFace");
        com.jieli.jl_rcsp.util.JL_Log.configureLog(this, true, true);
        JL_Log.setLog(true);
        JL_Log.setIsSaveLogFile(this, true);
//        }
        initLog();
        Logger.t(TAG).i("onSearchStarted");
        VPOperateManager.getInstance().init(this);
//        VPOperateManager.getInstance().setAutoConnectBTBySdk(false);
        mScanner = BluetoothLeScannerCompat.getScanner();
        VPLogger.setDebug(true);
        initRecyleView();
        checkPermission();
        registerBluetoothStateListener();
        createFile();
        VPLocalLogger.startMonitor(this);
        DemoStepLogger.stepSuccess("MAIN_INIT", "Inicialização base concluída e monitorização de logs ativa");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBleSessionHandedOff) {
            mBleSessionHandedOff = false;
            DemoStepLogger.featureEvent("BLE_SESSION", "MainActivity retomou a posse da sessão BLE");
        }
    }


    /**
     * Cria um ficheiro de suporte usado por operações GNSS/AGPS na demo.
     * O objetivo é garantir que os fluxos que exigem este ficheiro podem arrancar de imediato.
     */
    private void createFile() {
        String fileSDK = getExternalFilesDir(null) + File.separator + "LTEPH_GPS_1.rtcm";
        File file = new File(fileSDK);
        if (!file.exists()) {
            try {
                file.createNewFile();
                Logger.t(TAG).i("createNewFile");
                DemoStepLogger.stepSuccess("MAIN_FILE_PREPARE", "Ficheiro de suporte criado: " + fileSDK);
            } catch (IOException e) {
                e.printStackTrace();
                DemoStepLogger.stepError("MAIN_FILE_PREPARE", "Falha ao criar ficheiro de suporte: " + e.getMessage());
            }
        } else {
            Logger.t(TAG).i("exist file");
            DemoStepLogger.featureEvent("MAIN_FILE_PREPARE", "Ficheiro de suporte já existia: " + fileSDK);
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.scan) {
            scanDevice();
        }
    }

    private void initRecyleView() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) super.findViewById(R.id.mian_swipeRefreshLayout);
        mRecyclerView = (RecyclerView) super.findViewById(R.id.main_recylerlist);
        mTitleTextView = (TextView) super.findViewById(R.id.main_title);
        tvTips = (TextView) super.findViewById(R.id.tvTips);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bleConnectAdatpter = new BleScanViewAdapter(this, mListData);
        mRecyclerView.setAdapter(bleConnectAdatpter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST));
        bleConnectAdatpter.setBleItemOnclick(this);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mTitleTextView.setText("Procurar dispositivos V" + getAppVersion(mContext));
    }


    private boolean isGetPermission() {
        boolean isScanPermissionGranted;
        if (Build.VERSION.SDK_INT <= 22) {
            isScanPermissionGranted = true; //android 6.0 
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isScanPermissionGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN)
                    == PERMISSION_GRANTED; //android 12 BLUETOOTH_SCAN
        } else {
            isScanPermissionGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PERMISSION_GRANTED;
        }
        return isScanPermissionGranted;
    }

    private void checkPermission() {
        Logger.t(TAG).i("Build.VERSION.SDK_INT =" + Build.VERSION.SDK_INT);
        DemoStepLogger.stepStart("MAIN_PERMISSION", "Validação de permissões BLE em runtime");
        if (Build.VERSION.SDK_INT <= 22) {
            initBLE();
            DemoStepLogger.stepSuccess("MAIN_PERMISSION", "Sem necessidade de runtime permission (API <= 22)");
            return;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            initDialogBluetoothScan();
            checkBLEScanPermissionAboveAndroid11();
        } else {
            int permissionCheck = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                Logger.t(TAG).i("checkPermission,PERMISSION_GRANTED");
                initBLE();
                DemoStepLogger.stepSuccess("MAIN_PERMISSION", "Permissão de localização já concedida");
            } else if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                showMsg("Android6.0-Android11 ");
                requestPermission();
                Logger.t(TAG).i("checkPermission,PERMISSION_DENIED");
                DemoStepLogger.stepError("MAIN_PERMISSION", "Permissão de localização ainda não concedida");
            }
        }

    }

    public void showMsg(String msg) {
        runOnUiThread(() -> Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show());
    }

    private void commandSettingUI() {
        try {
            Logger.t("^^^^^").e("=========================================>>>>> commandSettingUI");
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 1115);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Dialog mDialogBluetoothScan;

    private void initDialogBluetoothScan() {
        mDialogBluetoothScan = new Dialog(this, R.style.loading_dialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pop_cancelok, null);

        mDialogBluetoothScan.setContentView(dialogView);
        mDialogBluetoothScan.setCancelable(false);
        mDialogBluetoothScan.setCanceledOnTouchOutside(false);

        TextView okTv = (TextView) dialogView.findViewById(R.id.dialog_ok);
        TextView cancelTv = (TextView) dialogView.findViewById(R.id.dialog_cancel);
        TextView contentTv = (TextView) dialogView.findViewById(R.id.dialog_content);

        contentTv.setText("Procurar dispositivosdispositivoAtivar");
        okTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commandSettingUI();
                mDialogBluetoothScan.dismiss();
            }
        });
        cancelTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialogBluetoothScan.dismiss();
            }
        });
    }

    private boolean checkBLEScanPermissionAboveAndroid11() {
        Logger.t(TAG).e("**Android12BLE");
        if (Build.VERSION.SDK_INT < 31) {
            Logger.t(TAG).e("Android12");
            return false;
        }
        boolean hasScanPermission = isGetPermission();
        Logger.t(TAG).e("**Android12BLE hasScanPermission = " + hasScanPermission);
        if (!hasScanPermission) {
            boolean isNeedExplanation = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_SCAN);
            Logger.t(TAG).e("**Android12BLE hasScanPermission = false , isNeedExplanation = " + isNeedExplanation);
            if (isNeedExplanation) {
                showMsg("，Ativarandroid12 ");
                mDialogBluetoothScan.show();
            } else {
                List<String> permissionList = new ArrayList<>();
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
                permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
                Dexter.withContext(this)
                        .withPermissions(permissionList)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                                for (PermissionGrantedResponse grantedResponse : multiplePermissionsReport.getGrantedPermissionResponses()) {
                                    Logger.t(TAG).e("onPermissionsChecked:Granted::::" + grantedResponse.getRequestedPermission().toString());
                                }
                                for (PermissionDeniedResponse deniedResponse : multiplePermissionsReport.getDeniedPermissionResponses()) {
                                    Logger.t(TAG).e("onPermissionsChecked:Denied::::" + deniedResponse.getRequestedPermission().toString());
                                }
                                if (multiplePermissionsReport.getGrantedPermissionResponses().size() == 3) {
                                    showMsg("");
                                    DemoStepLogger.stepSuccess("MAIN_PERMISSION", "Permissões BLE Android 12+ concedidas");
                                } else {
                                    showMsg("");
                                    DemoStepLogger.stepError("MAIN_PERMISSION", "Permissões BLE Android 12+ rejeitadas parcialmente");
                                }
                            }


                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                                for (PermissionRequest permissionRequest : list) {
                                    Logger.t(TAG).e("onPermissionRationaleShouldBeShown::::" + permissionRequest.toString());
                                }
                            }
                        }).check();
            }
            return false;
        } else {
            DemoStepLogger.stepSuccess("MAIN_PERMISSION", "Permissão BLE_SCAN já concedida em Android 12+");
        }
        return false;
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Logger.t(TAG).i("requestPermission,shouldShowRequestPermissionRationale");

            } else {
                Logger.t(TAG).i("requestPermission,shouldShowRequestPermissionRationale else");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                /*Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,*/Manifest.permission.BLUETOOTH_PRIVILEGED},
                        MY_PERMISSIONS_REQUEST_BLUETOOTH);
            }
        } else {
            Logger.t(TAG).i("requestPermission,shouldShowRequestPermissionRationale hehe");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BLUETOOTH: {
                Logger.t(TAG).i("onRequestPermissionsResult,MY_PERMISSIONS_REQUEST_BLUETOOTH ");
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initBLE();
                } else {
                }
                return;
            }
        }
    }

    private void initLog() {
        Logger.init(YOUR_APPLICATION)
                .methodCount(0)
                .methodOffset(0)
                .hideThreadInfo()
                .logLevel(LogLevel.FULL)
                .logAdapter(new CustomLogAdapter());
    }

    private boolean scanDevice() {
        DemoStepLogger.stepStart("BLE_SCAN", "Início de varrimento BLE");
        if (!mListAddress.isEmpty()) {
            mListAddress.clear();
        }
        if (!mListData.isEmpty()) {
            mListData.clear();
            tvTips.setVisibility(mListData.isEmpty() ? View.VISIBLE : View.GONE);
            bleConnectAdatpter.notifyDataSetChanged();
        }

        if (!BluetoothUtils.isBluetoothEnabled()) {
            Toast.makeText(mContext, "Bluetooth está desligado", Toast.LENGTH_SHORT).show();
            DemoStepLogger.stepError("BLE_SCAN", "Bluetooth está desligado no dispositivo");
            return true;
        }
//        startScan();
        VPOperateManager.getInstance().startScanDevice(mSearchResponse);
        DemoStepLogger.featureEvent("BLE_SCAN", "Pedido de scan enviado ao SDK");
        return false;
    }


    private void startScan() {
        //
        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1000)
                .setUseHardwareBatchingIfSupported(false)//true，，;false
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scanFilter;

        ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
        scanFilter = scanFilterBuilder.build();
        filters.add(scanFilter);
        final ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

            }

            @Override
            public void onBatchScanResults(final List<ScanResult> results) {
                super.onBatchScanResults(results);
                Logger.t(TAG).i("onBatchScanResults:" + results.size());
                //Logger.t(TAG).i("address," + bluetoothDevice.getAddress());
                //05,09,42,31,35,50|03,19,41,03|02,01,06|03,03,FF,FF|09,FF,F8,F8,CF,86,07,90,82,DD,
                //flag 03
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < results.size(); i++) {
                            ScanResult scanResult = results.get(i);

                            BluetoothDevice device = scanResult.getDevice();
                            if (!mListAddress.contains(device.getAddress())) {
                                mListData.add(new SearchResult(device, scanResult.getRssi(), scanResult.getScanRecord().getBytes()));
                                mListAddress.add(device.getAddress());
                            }
                        }
                        Collections.sort(mListData, new DeviceCompare());
                        tvTips.setVisibility(mListData.isEmpty() ? View.VISIBLE : View.GONE);
                        bleConnectAdatpter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.i(TAG, "onScanFailed:" + errorCode);
            }
        };
        mScanner.startScan(filters, settings, mScanCallback);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanner.stopScan(mScanCallback);
                refreshStop();
            }
        }, 6 * 1000);
    }


    private void connectDevice(final String mac, final String deviceName) {
        DemoStepLogger.stepStart("BLE_CONNECT", "Tentativa de ligação ao dispositivo " + mac + " (" + deviceName + ")");

        VPOperateManager.getInstance().registerConnectStatusListener(mac, mBleConnectStatusListener);

        VPOperateManager.getInstance().connectDevice(mac, deviceName, new IConnectResponse() {

            @Override
            public void connectState(int code, BleGattProfile profile, boolean isoadModel) {
                if (code == Code.REQUEST_SUCCESS) {
                    //dispositivoestado
                    Logger.t(TAG).i("");
                    Logger.t(TAG).i("Atualização de firmware=" + isoadModel);
                    mIsOadModel = isoadModel;
                    mHasActiveBleSession = true;
                    DemoStepLogger.stepSuccess("BLE_CONNECT", "Ligação BLE estabelecida. isOadModel=" + isoadModel);
                } else {
                    Logger.t(TAG).i("");
                    mHasActiveBleSession = false;
                    DemoStepLogger.stepError("BLE_CONNECT", "Falha de ligação BLE. code=" + code);
                }
            }
        }, new INotifyResponse() {
            @Override
            public void notifyState(int state) {
                if (state == Code.REQUEST_SUCCESS) {
                    //dispositivoestado
                    Logger.t(TAG).i("-");
                    DemoStepLogger.stepSuccess("BLE_NOTIFY", "Canal notify ativo; segue para validação de password");

                    Intent intent = new Intent(mContext, PwdConfirmActivity.class);
                    intent.putExtra("isoadmodel", mIsOadModel);
                    intent.putExtra("deviceaddress", mac);
                    mBleSessionHandedOff = true;
                    startActivity(intent);

//                    VPOperateManager.getInstance().confirmDevicePwd(new IBleWriteResponse() {
//                        @Override
//                        public void onResponse(int code) {
//
//                        }
//                    }, new IPwdDataListener() {
//                        @Override
//                        public void onPwdDataChange(PwdData pwdData) {
//                            String message = "PwdData:\n" + pwdData.toString();
//                            Logger.t(TAG).i(message);
//                            int deviceNumber = pwdData.getDeviceNumber();
//                            String deviceVersion = pwdData.getDeviceVersion();
//                            String deviceTestVersion = pwdData.getDeviceTestVersion();
//                            Logger.t(TAG).e("dispositivo：" + deviceNumber + ",：" + deviceVersion + ",\n：" + deviceTestVersion);
//                        }
//                    }, new IDeviceFuctionDataListener() {
//                        @Override
//                        public void onFunctionSupportDataChange(FunctionDeviceSupportData functionSupport) {
//                            String message = "FunctionDeviceSupportData:\n" + functionSupport.toString();
//                            Logger.t(TAG).i(message);
//                        }
//                    }, new ISocialMsgDataListener() {
//                        @Override
//                        public void onSocialMsgSupportDataChange(FunctionSocailMsgData socailMsgData) {
//
//                        }
//
//                        @Override
//                        public void onSocialMsgSupportDataChange2(FunctionSocailMsgData socailMsgData) {
//
//                        }
//                    }, new ICustomSettingDataListener() {
//                        @Override
//                        public void OnSettingDataChange(CustomSettingData customSettingData) {
//                            String message = "CustomSettingData:\n" + customSettingData.toString();
//                            Logger.t(TAG).i(message);
//                        }
//                    }, "0000", true);

                } else {
                    Logger.t(TAG).i("，");
                    DemoStepLogger.stepError("BLE_NOTIFY", "Falha ao ativar notify. state=" + state);
                }
            }
        });
    }

    /**
     * AtivarorDesativarestado
     */
    private void registerBluetoothStateListener() {
        VPOperateManager.getInstance().registerBluetoothStateListener(mBluetoothStateListener);
    }

    /**
     * AtivarDesativarestado
     */
    private final IABleConnectStatusListener mBleConnectStatusListener = new IABleConnectStatusListener() {

        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == Constants.STATUS_CONNECTED) {
                Logger.t(TAG).i("STATUS_CONNECTED");
                mHasActiveBleSession = true;
            } else if (status == Constants.STATUS_DISCONNECTED) {
                Logger.t(TAG).i("STATUS_DISCONNECTED");
                mHasActiveBleSession = false;
            }
        }
    };

    /**
     * dispositivoestado
     */
    private final IABluetoothStateListener mBluetoothStateListener = new IABluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            Logger.t(TAG).i("open=" + openOrClosed);
        }
    };


    public static boolean isShowDevice(byte[] scanRecord) {
        if (isBeyondVp(scanRecord)) {
            if (isBrandDevice(scanRecord)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 
     */
    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            Logger.t(TAG).i("onSearchStarted");
            DemoStepLogger.stepStart("BLE_SCAN", "SDK notificou início do scan");
        }

        @Override
        public void onDeviceFounded(final SearchResult device) {
            Logger.t(TAG).i(String.format("device for %s-%s-%d", device.getName(), device.getAddress(), device.rssi));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!mListAddress.contains(device.getAddress()) /*&& isShowDevice(device.scanRecord)*/) {
                        mListData.add(device);
                        mListAddress.add(device.getAddress());
                    }
                    mListData.sort(new DeviceCompare());
                    tvTips.setVisibility(mListData.isEmpty() ? View.VISIBLE : View.GONE);
                    bleConnectAdatpter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onSearchStopped() {
            refreshStop();
            Logger.t(TAG).i("onSearchStopped");
            DemoStepLogger.stepSuccess("BLE_SCAN", "Scan terminado");
        }

        @Override
        public void onSearchCanceled() {
            refreshStop();
            Logger.t(TAG).i("onSearchCanceled");
            DemoStepLogger.stepError("BLE_SCAN", "Scan cancelado");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (BluetoothUtils.isBluetoothEnabled()) {
                scanDevice();
            } else {
                refreshStop();
            }
        }
    }

    @Override
    public void onRefresh() {
        if (checkBLE()) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Logger.t(TAG).i("onRefresh");
                    scanDevice();
                }
            }, 3000);
        }
    }

    private void initBLE() {
        mBManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (null != mBManager) {
            mBAdapter = mBManager.getAdapter();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBScanner = mBAdapter.getBluetoothLeScanner();
        }
        checkBLE();
    }

    /**
     * dispositivoAtivar
     *
     * @return
     */
    private boolean checkBLE() {
        if (!BluetoothUtils.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    /**
     * TerminarAtualizar
     */
    void refreshStop() {
        Logger.t(TAG).i("refreshComlete");
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void OnRecycleViewClick(int position) {
        DemoStepLogger.featureEvent("BLE_DEVICE_SELECT", "Dispositivo selecionado na lista. posição=" + position);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, "A ligar, aguarde...", Toast.LENGTH_SHORT).show();
            }
        });
        SearchResult searchResult = mListData.get(position);
        connectDevice(searchResult.getAddress(), searchResult.getName());
    }

    public String getAppVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
