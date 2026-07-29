package com.hitecosystem.hbanddemo;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IABleConnectStatusListener;
import com.veepoo.protocol.listener.base.IABluetoothStateListener;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;
import com.veepoo.protocol.listener.data.IBPDetectDataListener;
import com.veepoo.protocol.listener.data.IBatteryDataListener;
import com.veepoo.protocol.listener.data.IBodyComponentDetectListener;
import com.veepoo.protocol.listener.data.IBodyComponentReadDataListener;
import com.veepoo.protocol.listener.data.IBloodGlucoseChangeListener;
import com.veepoo.protocol.listener.data.IBreathDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.AbsDeviceManualDetectDataListener;
import com.veepoo.protocol.listener.data.IECGDetectListener;
import com.veepoo.protocol.listener.data.IECGReadDataListener;
import com.veepoo.protocol.listener.data.IFatigueDataListener;
import com.veepoo.protocol.listener.data.IGsrDetectListener;
import com.veepoo.protocol.listener.data.IHeartDataListener;
import com.veepoo.protocol.listener.data.IOriginDataListener;
import com.veepoo.protocol.listener.data.IOriginData3Listener;
import com.veepoo.protocol.listener.data.IPressureDetectListener;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.listener.data.IResponseListener;
import com.veepoo.protocol.listener.data.ISleepDataListener;
import com.veepoo.protocol.listener.data.ISocialMsgDataListener;
import com.veepoo.protocol.listener.data.ISpo2hDataListener;
import com.veepoo.protocol.listener.data.ISpo2hOriginDataListener;
import com.veepoo.protocol.listener.data.ISportDataListener;
import com.veepoo.protocol.listener.data.ITemptureDataListener;
import com.veepoo.protocol.listener.data.ITemptureDetectDataListener;
import com.veepoo.protocol.model.datas.BatteryData;
import com.veepoo.protocol.model.datas.BloodGlucoseManualData;
import com.veepoo.protocol.model.datas.BloodOxygenManualData;
import com.veepoo.protocol.model.datas.BloodPressureManualData;
import com.veepoo.protocol.model.datas.BodyComponent;
import com.veepoo.protocol.model.datas.BodyTemperatureManualData;
import com.veepoo.protocol.model.datas.BpData;
import com.veepoo.protocol.model.datas.BreathData;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FatigueData;
import com.veepoo.protocol.model.datas.EcgDetectInfo;
import com.veepoo.protocol.model.datas.EcgDetectResult;
import com.veepoo.protocol.model.datas.EcgDetectState;
import com.veepoo.protocol.model.datas.EcgDiagnosis;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.datas.GsrDetectResult;
import com.veepoo.protocol.model.datas.HRVOriginData;
import com.veepoo.protocol.model.datas.HeartRateManualData;
import com.veepoo.protocol.model.datas.HrvManualData;
import com.veepoo.protocol.model.datas.MetoManualData;
import com.veepoo.protocol.model.datas.HeartData;
import com.veepoo.protocol.model.datas.OriginData;
import com.veepoo.protocol.model.datas.OriginData3;
import com.veepoo.protocol.model.datas.OriginHalfHourData;
import com.veepoo.protocol.model.datas.PersonInfoData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.model.datas.PressureManualData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.Spo2hData;
import com.veepoo.protocol.model.datas.Spo2hOriginData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.model.datas.TemptureData;
import com.veepoo.protocol.model.datas.TemptureDetectData;
import com.veepoo.protocol.model.datas.TimeData;
import com.veepoo.protocol.model.enums.DetectState;
import com.veepoo.protocol.model.enums.DeviceManualDataType;
import com.veepoo.protocol.model.enums.EBPDetectModel;
import com.veepoo.protocol.model.enums.EBloodGlucoseRiskLevel;
import com.veepoo.protocol.model.enums.EBloodGlucoseStatus;
import com.veepoo.protocol.model.enums.EFunctionStatus;
import com.veepoo.protocol.model.enums.EEcgDataType;
import com.veepoo.protocol.model.enums.EPwdStatus;
import com.veepoo.protocol.model.enums.ESex;
import com.veepoo.protocol.model.enums.ETimeMode;
import com.veepoo.protocol.model.enums.GsrDetectAck;
import com.veepoo.protocol.model.enums.PressureDetectState;
import com.veepoo.protocol.model.settings.DeviceTimeSetting;
import com.veepoo.protocol.model.settings.ReadOriginSetting;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CapacitorPlugin(
    name = "HBand",
    permissions = {
        @Permission(
            alias = "bluetooth",
            strings = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            }
        ),
        @Permission(
            alias = "location",
            strings = { Manifest.permission.ACCESS_FINE_LOCATION }
        )
    }
)
public class HBandPlugin extends Plugin {
    private final VPOperateManager manager = VPOperateManager.getInstance();
    private final Map<String, SearchResult> discoveredDevices = new LinkedHashMap<>();
    private final JSObject capabilities = new JSObject();
    private int watchDataDays = 3;
    private int originProtocolVersion = 1;
    private final IBleWriteResponse writeResponse = code -> emitLog(
        code == Code.REQUEST_SUCCESS ? "success" : "warning",
        "BLE write response: " + code,
        null
    );
    private final BleWriteResponse directWriteResponse = code -> emitLog(
        code == Code.REQUEST_SUCCESS ? "success" : "warning",
        "BLE write response: " + code,
        null
    );
    private String connectionState = "idle";
    private String currentAddress;
    private String currentName;
    private String currentPassword = "0000";
    private String firmwareVersion;
    private String hardwareVersion;
    private PluginCall pendingConnectCall;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private boolean intentionalDisconnect;
    private int reconnectAttempts;
    private final Runnable reconnectRunnable = () -> {
        if (intentionalDisconnect
            || currentAddress == null
            || manager.isCurrentDeviceConnected()) {
            return;
        }
        reconnectAttempts += 1;
        emitLog(
            "info",
            "Automatic reconnect attempt " + reconnectAttempts,
            "session.reconnect"
        );
        scanForReconnect();
    };

    @Override
    public void load() {
        manager.init(getContext().getApplicationContext());
        VPOperateManager.setShowFunctionNotSupportToast(false);
        manager.registerBluetoothStateListener(new IABluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean isOpen) {
                /*
                 * O SDK pode conservar o estado interno "connected" quando o
                 * adaptador fecha. Corrigir o estado aqui garante que o canal
                 * GATT é recriado quando o Bluetooth voltar a abrir.
                 */
                if (!isOpen) {
                    reconnectHandler.removeCallbacks(reconnectRunnable);
                    if (currentAddress != null && !intentionalDisconnect) {
                        setConnectionState("disconnected");
                    } else {
                        emitStatus();
                    }
                    return;
                }
                if (
                    currentAddress != null
                    && !intentionalDisconnect
                    && !manager.isCurrentDeviceConnected()
                ) {
                    /*
                     * Reiniciar o adaptador invalida o registo GATT interno da
                     * biblioteca. Uma nova inicialização recompõe esse cliente
                     * antes de voltar a usar o endereço conhecido.
                     */
                    manager.init(getContext().getApplicationContext());
                    setConnectionState("disconnected");
                    scheduleReconnect();
                    return;
                }
                emitStatus();
            }
        });
    }

    /**
     * Devolve um retrato imutável do estado usado pela interface Angular.
     */
    @PluginMethod
    public void getStatus(PluginCall call) {
        call.resolve(buildStatus());
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (getPermissionState("bluetooth") == PermissionState.GRANTED) {
                call.resolve(permissionResult(true));
                return;
            }
            requestPermissionForAlias("bluetooth", call, "permissionCallback");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || getPermissionState("location") == PermissionState.GRANTED) {
            call.resolve(permissionResult(true));
            return;
        }
        requestPermissionForAlias("location", call, "permissionCallback");
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        boolean granted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? getPermissionState("bluetooth") == PermissionState.GRANTED
            : getPermissionState("location") == PermissionState.GRANTED;
        call.resolve(permissionResult(granted));
    }

    @PluginMethod
    public void startScan(PluginCall call) {
        int timeoutMs = call.getInt("timeoutMs", 12_000);
        discoveredDevices.clear();
        setConnectionState("scanning");
        manager.startScanDevice(timeoutMs, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                emitLog("info", "BLE scan started", "session.scan");
            }

            @Override
            public void onDeviceFounded(SearchResult result) {
                if (result == null || result.getAddress() == null) {
                    return;
                }
                discoveredDevices.put(result.getAddress(), result);
                JSObject device = devicePayload(result);
                notifyListeners("deviceFound", device, true);
            }

            @Override
            public void onSearchStopped() {
                finishScanState();
                emitLog("success", "BLE scan completed", "session.scan");
            }

            @Override
            public void onSearchCanceled() {
                finishScanState();
                emitLog("warning", "BLE scan cancelled", "session.scan");
            }
        });
        call.resolve();
    }

    @PluginMethod
    public void stopScan(PluginCall call) {
        manager.stopScanDevice();
        setConnectionState("idle");
        call.resolve();
    }

    /**
     * Liga o BLE, ativa notify e só conclui a Promise depois da password.
     */
    @PluginMethod
    public void connect(PluginCall call) {
        String deviceId = call.getString("deviceId");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            call.reject("DEVICE_ID_REQUIRED");
            return;
        }
        SearchResult discovered = discoveredDevices.get(deviceId);
        currentAddress = deviceId;
        currentName = discovered != null && discovered.getName() != null
            ? discovered.getName()
            : deviceId;
        currentPassword = call.getString("password", "0000");
        pendingConnectCall = call;
        intentionalDisconnect = false;
        reconnectAttempts = 0;
        reconnectHandler.removeCallbacks(reconnectRunnable);
        connectCurrentDevice();
    }

    /**
     * Reutiliza exatamente o mesmo handshake na ligação inicial e nas
     * religações automáticas após uma perda inesperada do sinal.
     */
    private void connectCurrentDevice() {
        manager.stopScanDevice();
        setConnectionState("connecting");
        manager.registerConnectStatusListener(currentAddress, connectStatusListener);
        manager.connectDevice(currentAddress, currentName, new IConnectResponse() {
            @Override
            public void connectState(int code, BleGattProfile profile, boolean isOadModel) {
                if (code != Code.REQUEST_SUCCESS) {
                    if (pendingConnectCall != null) {
                        rejectPendingConnect("BLE_CONNECT_FAILED_" + code);
                        setConnectionState("error");
                    } else {
                        setConnectionState("disconnected");
                    }
                    scheduleReconnect();
                }
            }
        }, new INotifyResponse() {
            @Override
            public void notifyState(int state) {
                if (state != Code.REQUEST_SUCCESS) {
                    rejectPendingConnect("BLE_NOTIFY_FAILED_" + state);
                    setConnectionState("error");
                    scheduleReconnect();
                    return;
                }
                authenticateCurrentDevice();
            }
        });
    }

    /**
     * Depois de o adaptador reiniciar, volta a descobrir apenas o MAC
     * recordado antes de ligar. Isto recompõe o dispositivo interno usado pelo
     * SDK e nunca seleciona uma das outras MF91 próximas.
     */
    private void scanForReconnect() {
        if (intentionalDisconnect || currentAddress == null) {
            return;
        }
        final boolean[] targetFound = { false };
        final Runnable[] scanTimeout = new Runnable[1];
        scanTimeout[0] = () -> {
            if (targetFound[0] || intentionalDisconnect) {
                return;
            }
            targetFound[0] = true;
            manager.stopScanDevice();
            setConnectionState("disconnected");
            emitLog("warning", "Reconnect scan timed out", "session.reconnect");
            scheduleReconnect();
        };
        manager.startScanDevice(8_000, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                emitLog("info", "Reconnect scan started", "session.reconnect");
            }

            @Override
            public void onDeviceFounded(SearchResult result) {
                if (
                    result == null
                    || result.getAddress() == null
                    || !currentAddress.equalsIgnoreCase(result.getAddress())
                    || targetFound[0]
                ) {
                    return;
                }
                targetFound[0] = true;
                reconnectHandler.removeCallbacks(scanTimeout[0]);
                discoveredDevices.put(result.getAddress(), result);
                if (result.getName() != null && !result.getName().trim().isEmpty()) {
                    currentName = result.getName();
                }
                manager.stopScanDevice();
                connectCurrentDevice();
            }

            @Override
            public void onSearchStopped() {
                if (!targetFound[0] && !intentionalDisconnect) {
                    targetFound[0] = true;
                    reconnectHandler.removeCallbacks(scanTimeout[0]);
                    setConnectionState("disconnected");
                    scheduleReconnect();
                }
            }

            @Override
            public void onSearchCanceled() {
                if (!targetFound[0] && !intentionalDisconnect) {
                    targetFound[0] = true;
                    reconnectHandler.removeCallbacks(scanTimeout[0]);
                    setConnectionState("disconnected");
                    scheduleReconnect();
                }
            }
        });
        reconnectHandler.postDelayed(scanTimeout[0], 10_000L);
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        intentionalDisconnect = true;
        reconnectHandler.removeCallbacks(reconnectRunnable);
        manager.disconnectWatch(code -> {
            setConnectionState("disconnected");
            stopConnectionService();
            call.resolve();
        });
    }

    /**
     * Encaminha apenas contratos cujos parâmetros foram confirmados no demo oficial.
     */
    @PluginMethod
    public void execute(PluginCall call) {
        String operation = call.getString("operation");
        if (operation == null) {
            call.reject("OPERATION_REQUIRED");
            return;
        }
        if (!manager.isCurrentDeviceConnected() && !operation.startsWith("session.")) {
            call.reject("DEVICE_NOT_CONNECTED");
            return;
        }

        try {
            switch (operation) {
            case "session.authenticate":
                pendingConnectCall = call;
                authenticateCurrentDevice();
                return;
            case "session.disconnect":
                disconnect(call);
                return;
            case "device.battery":
                readBattery(call);
                return;
            case "device.rssi":
                readRssi(call);
                return;
            case "device.time":
                syncTime(call);
                return;
            case "device.profile":
                syncProfile(call);
                return;
            case "measure.heartRate.start":
                startHeartRate(call);
                return;
            case "measure.heartRate.stop":
                manager.stopDetectHeart(writeResponse);
                accept(call, operation);
                return;
            case "measure.bloodPressure.start":
                startBloodPressure(call);
                return;
            case "measure.bloodPressure.stop":
                manager.stopDetectBP(writeResponse, EBPDetectModel.DETECT_MODEL_PUBLIC);
                accept(call, operation);
                return;
            case "measure.oxygen.start":
                startOxygen(call);
                return;
            case "measure.oxygen.stop":
                manager.stopDetectSPO2H(writeResponse, oxygenListener);
                accept(call, operation);
                return;
            case "measure.breathing.start":
                startBreathing(call);
                return;
            case "measure.breathing.stop":
                manager.stopDetectBreath(writeResponse, breathingListener);
                accept(call, operation);
                return;
            case "measure.temperature.start":
                startTemperature(call);
                return;
            case "measure.temperature.stop":
                manager.stopDetectTempture(writeResponse, temperatureListener);
                accept(call, operation);
                return;
            case "measure.fatigue.start":
                startFatigue(call);
                return;
            case "measure.fatigue.stop":
                manager.stopDetectFatigue(writeResponse, fatigueListener);
                accept(call, operation);
                return;
            case "measure.stress.start":
                startStress(call);
                return;
            case "measure.stress.stop":
                manager.stopDetectPressure(directWriteResponse);
                accept(call, operation);
                return;
            case "measure.gsr.start":
                startGsr(call);
                return;
            case "measure.gsr.stop":
                manager.stopDetectGsr(directWriteResponse);
                accept(call, operation);
                return;
            case "measure.bloodGlucose.start":
                startBloodGlucose(call);
                return;
            case "measure.bloodGlucose.stop":
                manager.stopBloodGlucoseDetect(writeResponse, bloodGlucoseListener);
                accept(call, operation);
                return;
            case "measure.ecg.start":
                /*
                 * O segundo parâmetro ativa o callback ADC usado para desenhar
                 * a curva. Com false o SDK devolve apenas estado e diagnóstico.
                 */
                manager.startDetectECG(directWriteResponse, true, ecgListener);
                accept(call, operation);
                return;
            case "measure.ecg.stop":
                manager.stopDetectECG(directWriteResponse, true, ecgListener);
                accept(call, operation);
                return;
            case "measure.bodyComposition.start":
                manager.startDetectBodyComponent(directWriteResponse, bodyComponentListener);
                accept(call, operation);
                return;
            case "measure.bodyComposition.stop":
                manager.stopDetectBodyComponent(directWriteResponse);
                accept(call, operation);
                return;
            case "history.metric":
                readMetricHistory(call);
                return;
            case "history.daily":
                readDailyHistory(call);
                return;
            case "history.activity.current":
                readCurrentActivity(call);
                return;
            case "history.sleep":
                readSleep(call);
                return;
            case "history.origin":
                readOrigin(call);
                return;
            case "history.oxygen":
                readOxygenHistory(call);
                return;
            case "history.temperature":
                readTemperatureHistory(call);
                return;
            default:
                call.reject("UNIMPLEMENTED_OPERATION: " + operation);
            }
        } catch (RuntimeException error) {
            /*
             * Alguns métodos do SDK propagam exceções internas por reflexão.
             * Rejeitar a chamada evita terminar o processo Capacitor e mantém
             * o erro disponível nos logs da medição.
             */
            emitLog("error", "SDK operation failed: " + error, operation);
            call.reject("SDK_OPERATION_FAILED: " + operation, error);
        }
    }

    private void authenticateCurrentDevice() {
        setConnectionState("authenticating");
        manager.confirmDevicePwd(writeResponse, new IPwdDataListener() {
            @Override
            public void onPwdDataChange(PwdData pwdData) {
                EPwdStatus status = pwdData.getmStatus();
                if (status == EPwdStatus.CHECK_FAIL || status == EPwdStatus.SETTING_FAIL) {
                    rejectPendingConnect("PASSWORD_REJECTED");
                    setConnectionState("error");
                    return;
                }
                firmwareVersion = pwdData.getDeviceVersion();
                hardwareVersion = String.valueOf(pwdData.getDeviceNumber());
                reconnectAttempts = 0;
                reconnectHandler.removeCallbacks(reconnectRunnable);
                setConnectionState("connected");
                startConnectionService();
                resolvePendingConnect();
                emitLog("success", "Device session ready", "session.connect");
            }

            @Override
            public void onConnectionConfirmTimeout() {
                /*
                 * O SDK pode publicar este timeout depois de já ter entregue
                 * uma confirmação válida. Nesse caso a sessão BLE continua
                 * operacional e não deve regressar visualmente a erro.
                 */
                if ("connected".equals(connectionState) && manager.isCurrentDeviceConnected()) {
                    emitLog(
                        "warning",
                        "Late connection confirmation timeout ignored after authentication",
                        "session.authenticate"
                    );
                    return;
                }
                rejectPendingConnect("CONNECTION_CONFIRM_TIMEOUT");
                setConnectionState("error");
                scheduleReconnect();
            }
        }, new IDeviceFuctionDataListener() {
            @Override
            public void onFunctionSupportDataChange(FunctionDeviceSupportData data) {
                mapCapabilities(data);
                emitStatus();
            }

            @Override public void onDeviceFunctionPackage1Report(DeviceFunctionPackage1 data) {}
            @Override public void onDeviceFunctionPackage2Report(DeviceFunctionPackage2 data) {}
            @Override public void onDeviceFunctionPackage3Report(DeviceFunctionPackage3 data) {}
            @Override public void onDeviceFunctionPackage4Report(DeviceFunctionPackage4 data) {}
            @Override public void onDeviceFunctionPackage5Report(DeviceFunctionPackage5 data) {}
        }, new ISocialMsgDataListener() {
            @Override public void onSocialMsgSupportDataChange(FunctionSocailMsgData data) {}
            @Override public void onSocialMsgSupportDataChange2(FunctionSocailMsgData data) {}
        }, currentPassword, true);
    }

    private void mapCapabilities(FunctionDeviceSupportData data) {
        watchDataDays = Math.max(1, data.getWathcDay());
        originProtocolVersion = data.getOriginProtcolVersion();
        capabilities.put("steps", data.getWathcDay() > 0 ? "supported" : "unknown");
        putCapability("heartRate", data.getHeartDetect());
        putCapability("bloodPressure", data.getBp());
        putCapability("bloodOxygen", data.getSpo2H());
        putCapability("breathing", data.getBeathFunction());
        putCapability("temperature", data.getTemperatureFunction());
        putCapability("ecg", data.getEcg());
        putCapability("bloodGlucose", data.getBloodGlucose());
        putCapability("stress", data.getStress());
        putCapability("bodyComposition", data.getBodyComponent());
        putCapability("bloodComposition", data.getBloodComponent());
        putCapability("gsr", data.getGSR());
        putCapability("sleep", data.getPrecisionSleep());
        putCapability("sport", data.getSportModel());
        putCapability("autoMeasure", data.getAutoMeasure());
        putCapability("alarms", data.getAlarm2());
        putCapability("textAlarms", data.getTextAlarm());
        putCapability("lowPower", data.getLowPower());
        putCapability("femaleHealth", data.getWomen());
        putCapability("display", data.getScreenStyleFunction());
        putCapability("findDevice", data.getFindDeviceByPhone());
        putCapability("camera", data.getCamera());
        putCapability("weather", data.getWeatherFunction());
        putCapability("contacts", data.getContactFunction());
        putCapability("gps", data.getAgps());
        putCapability("watchFaces", data.getBigDataTranType() > 0 ? EFunctionStatus.SUPPORT : EFunctionStatus.UNSUPPORT);
    }

    private void putCapability(String key, EFunctionStatus status) {
        if (status == null || status == EFunctionStatus.UNKONW) {
            capabilities.put(key, "unknown");
        } else {
            capabilities.put(key, status == EFunctionStatus.UNSUPPORT ? "unsupported" : "supported");
        }
    }

    private void readBattery(PluginCall call) {
        manager.readBattery(writeResponse, data -> {
            JSObject values = new JSObject();
            /*
             * Alguns firmwares devolvem quatro níveis discretos em vez de uma
             * percentagem. Cada nível representa 25%, conforme o SDK oficial.
             */
            int percent = data.isPercent()
                ? data.getBatteryPercent()
                : data.getBatteryLevel() * 25;
            values.put("percent", Math.min(100, Math.max(0, percent)));
            values.put("lowBattery", data.isLowBattery());
            values.put("state", data.getState());
            emitData("battery", values, null, data.toString());
            accept(call, "device.battery");
        });
    }

    private void readRssi(PluginCall call) {
        if (currentAddress == null) {
            call.reject("DEVICE_NOT_CONNECTED");
            return;
        }
        manager.readRssi(currentAddress, new BleReadRssiResponse() {
            @Override
            public void onResponse(int code, Integer rssi) {
                JSObject values = new JSObject();
                values.put("dbm", rssi);
                emitData("rssi", values, null, "code=" + code);
            }
        });
        accept(call, "device.rssi");
    }

    private void syncTime(PluginCall call) {
        Calendar now = Calendar.getInstance();
        DeviceTimeSetting setting = new DeviceTimeSetting(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            now.get(Calendar.SECOND),
            ETimeMode.MODE_24
        );
        manager.settingTime(writeResponse, state -> {
            emitLog("success", "Time sync state: " + state, "device.time");
            accept(call, "device.time");
        }, setting);
    }

    private void syncProfile(PluginCall call) {
        JSObject params = call.getObject("params", new JSObject());
        if (!params.has("sex")
            || !params.has("heightCm")
            || !params.has("weightKg")
            || !params.has("age")
            || !params.has("targetSteps")) {
            call.reject("PROFILE_PARAMS_REQUIRED");
            return;
        }
        String sex = params.getString("sex");
        Integer height = params.getInteger("heightCm");
        Integer weight = params.getInteger("weightKg");
        Integer age = params.getInteger("age");
        Integer target = params.getInteger("targetSteps");
        if ((!"male".equals(sex) && !"female".equals(sex))
            || height == null
            || weight == null
            || age == null
            || target == null) {
            call.reject("PROFILE_PARAMS_INVALID");
            return;
        }
        manager.syncPersonInfo(writeResponse, status ->
            emitLog("success", "Profile sync state: " + status, "device.profile"),
            new PersonInfoData("female".equals(sex) ? ESex.WOMEN : ESex.MAN, height, weight, age, target)
        );
        accept(call, "device.profile");
    }

    private void startHeartRate(PluginCall call) {
        manager.startDetectHeart(writeResponse, data -> {
            JSObject values = new JSObject();
            values.put("bpm", data.getData());
            values.put("state", String.valueOf(data.getHeartStatus()));
            emitData("heartRate", values, null, data.toString());
        });
        accept(call, "measure.heartRate.start");
    }

    private void startBloodPressure(PluginCall call) {
        manager.startDetectBP(writeResponse, data -> {
            JSObject values = new JSObject();
            values.put("systolic", data.getHighPressure());
            values.put("diastolic", data.getLowPressure());
            values.put("progress", data.getProgress());
            values.put("state", String.valueOf(data.getStatus()));
            emitData("bloodPressure", values, null, data.toString());
        }, EBPDetectModel.DETECT_MODEL_PUBLIC);
        accept(call, "measure.bloodPressure.start");
    }

    private final ISpo2hDataListener oxygenListener = data -> {
        JSObject values = new JSObject();
        values.put("percent", data.getValue());
        values.put("pulseBpm", data.getRateValue());
        values.put("progress", data.getCheckingProgress());
        values.put("state", String.valueOf(data.getSpState()));
        emitData("oxygen", values, null, data.toString());
    };

    private void startOxygen(PluginCall call) {
        manager.startDetectSPO2H(writeResponse, oxygenListener);
        accept(call, "measure.oxygen.start");
    }

    private final IBreathDataListener breathingListener = data -> {
        JSObject values = new JSObject();
        values.put("breathsPerMinute", data.getValue());
        values.put("progress", data.getProgressValue());
        values.put("state", String.valueOf(data.getDeviceStateEnum()));
        emitData("breathing", values, null, data.toString());
    };

    private void startBreathing(PluginCall call) {
        manager.startDetectBreath(writeResponse, breathingListener);
        accept(call, "measure.breathing.start");
    }

    private final ITemptureDetectDataListener temperatureListener = data -> {
        JSObject values = new JSObject();
        values.put("celsius", data.getTempture());
        values.put("baselineCelsius", data.getTemptureBase());
        values.put("progress", data.getProgress());
        emitData("temperature", values, null, data.toString());
    };

    private void startTemperature(PluginCall call) {
        manager.startDetectTempture(writeResponse, temperatureListener);
        accept(call, "measure.temperature.start");
    }

    private final IFatigueDataListener fatigueListener = data -> {
        JSObject values = new JSObject();
        values.put("score", data.getValue());
        values.put("progress", data.getProgress());
        values.put("state", String.valueOf(data.getFatigueState()));
        emitData("fatigue", values, null, data.toString());
    };

    private void startFatigue(PluginCall call) {
        manager.startDetectFatigue(writeResponse, fatigueListener);
        accept(call, "measure.fatigue.start");
    }

    private final IPressureDetectListener pressureListener = new IPressureDetectListener() {
        @Override
        public void onDetecting(int progress) {
            emitMetric("stress", "progress", progress, null);
        }

        @Override
        public void onDetectSuccess(int value) {
            emitMetric("stress", "score", value, null);
        }

        @Override
        public void onDetectFailed(PressureDetectState state) {
            emitLog("error", "Stress failed: " + state, "measure.stress.start");
        }

        @Override
        public void onDetectStop() {
            emitLog("info", "Stress stopped", "measure.stress.stop");
        }
    };

    private void startStress(PluginCall call) {
        manager.startDetectPressure(directWriteResponse, pressureListener);
        accept(call, "measure.stress.start");
    }

    private final IGsrDetectListener gsrListener = new IGsrDetectListener() {
        @Override
        public void onGsrDetectProgress(int progress) {
            emitMetric("gsr", "progress", progress, null);
        }

        @Override
        public void onGsrDetectSuccess(GsrDetectResult result) {
            emitData("gsr", new JSObject().put("result", result.toString()), null, result.toString());
        }

        @Override
        public void onGsrDetectFailed(GsrDetectAck ack) {
            emitLog("error", "GSR failed: " + ack, "measure.gsr.start");
        }

        @Override
        public void onGsrDetectStop() {
            emitLog("info", "GSR stopped", "measure.gsr.stop");
        }
    };

    private void startGsr(PluginCall call) {
        manager.startDetectGsr(directWriteResponse, gsrListener);
        accept(call, "measure.gsr.start");
    }

    private final IBloodGlucoseChangeListener bloodGlucoseListener = new IBloodGlucoseChangeListener() {
        @Override
        public void onDetectError(int progress, EBloodGlucoseStatus status) {
            emitLog("error", "Blood glucose failed: " + status + " (" + progress + ")", "measure.bloodGlucose.start");
        }

        @Override
        public void onBloodGlucoseDetect(int progress, float value, EBloodGlucoseRiskLevel risk) {
            JSObject values = new JSObject();
            values.put("mmolL", value);
            values.put("progress", progress);
            values.put("risk", String.valueOf(risk));
            emitData("bloodGlucose", values, null, null);
        }

        @Override public void onBloodGlucoseStopDetect() {}
        @Override public void onBloodGlucoseAdjustingSettingSuccess(boolean open, float value) {}
        @Override public void onBloodGlucoseAdjustingSettingFailed() {}
        @Override public void onBloodGlucoseAdjustingReadSuccess(boolean open, float value) {}
        @Override public void onBloodGlucoseAdjustingReadFailed() {}
        @Override public void onBGMultipleAdjustingReadSuccess(boolean open, com.veepoo.protocol.model.datas.MealInfo before, com.veepoo.protocol.model.datas.MealInfo after, com.veepoo.protocol.model.datas.MealInfo random) {}
        @Override public void onBGMultipleAdjustingReadFailed() {}
        @Override public void onBGMultipleAdjustingSettingSuccess() {}
        @Override public void onBGMultipleAdjustingSettingFailed() {}
    };

    private void startBloodGlucose(PluginCall call) {
        manager.startBloodGlucoseDetect(writeResponse, bloodGlucoseListener);
        accept(call, "measure.bloodGlucose.start");
    }

    private final IECGDetectListener ecgListener = new IECGDetectListener() {
        @Override public void onEcgDetectInfoChange(EcgDetectInfo info) {
            emitData("ecg", new JSObject().put("state", info.toString()), null, info.toString());
        }

        @Override public void onEcgDetectStateChange(EcgDetectState state) {
            JSObject values = new JSObject();
            values.put("state", String.valueOf(state.getDeviceState()));
            values.put("progress", state.getProgress());
            values.put("bpm", state.getHr2() > 0 ? state.getHr2() : state.getHr1());
            values.put("hrvMilliseconds", state.getHrv());
            values.put("qtcMilliseconds", state.getQtc());
            emitData("ecg", values, null, state.toString());
        }

        @Override public void onEcgDetectResultChange(EcgDetectResult result) {
            emitData("ecg", ecgValues(result), intArray(result.getFilterSignals()), result.toString());
        }

        @Override public void onEcgDetectDiagnosisChange(EcgDiagnosis diagnosis) {
            JSObject values = new JSObject();
            values.put("success", diagnosis.isSuccess());
            values.put("bpm", diagnosis.getHeartRate());
            values.put("hrvMilliseconds", diagnosis.getHrv());
            values.put("qtMilliseconds", diagnosis.getQtTime());
            values.put("durationSeconds", diagnosis.getDuration());
            emitData("ecg", values, ecgSamples(diagnosis.getFilterSignals()), diagnosis.toString());
        }

        @Override public void onEcgADCChange(int[] ecgData, int[] powerData) {
            JSArray samples = ecgSamples(ecgData);
            emitData("ecg", new JSObject().put("sampleCount", samples.length()), samples, null);
        }
    };

    private final IBodyComponentDetectListener bodyComponentListener = new IBodyComponentDetectListener() {
        @Override public void onDetecting(int progress, int impedance) {
            emitData(
                "bodyComposition",
                new JSObject().put("progress", progress).put("impedance", impedance),
                null,
                null
            );
        }

        @Override public void onDetectSuccess(BodyComponent component) {
            emitData("bodyComposition", bodyComponentValues(component), null, component.toString());
        }

        @Override public void onDetectFailed(DetectState state) {
            emitLog("error", "Body composition failed: " + state, "measure.bodyComposition.start");
        }

        @Override public void onDetectStop() {
            emitLog("info", "Body composition stopped", "measure.bodyComposition.stop");
        }
    };

    /**
     * Lê um único dia civil. Os registos manuais podem incluir dados posteriores
     * ao instante pedido, pelo que o intervalo é novamente filtrado na bridge.
     */
    private void readMetricHistory(PluginCall call) {
        JSObject params = call.getObject("params");
        String metric = params == null ? null : params.getString("metric");
        String dateText = params == null ? null : params.getString("date");
        if (metric == null || dateText == null) {
            call.reject("HISTORY_METRIC_AND_DATE_REQUIRED");
            return;
        }

        final LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (RuntimeException error) {
            call.reject("HISTORY_DATE_INVALID");
            return;
        }

        if ("steps".equals(metric)) {
            if (!readStepHistory(call, metric, date)) {
                return;
            }
        } else if ("ecg".equals(metric)) {
            readEcgHistory(call, metric, date);
        } else if ("bodyComposition".equals(metric)) {
            readBodyComponentHistory(call, metric, date);
        } else {
            DeviceManualDataType dataType = manualType(metric);
            if (dataType == null) {
                call.reject("HISTORY_METRIC_UNSUPPORTED: " + metric);
                return;
            }
            readManualMetricHistory(call, metric, date, dataType);
        }
    }

    /**
     * Lê os blocos de atividade do dia pedido. O protocolo 3 agrupa vários
     * blocos no mesmo callback; os protocolos anteriores entregam-nos um a um.
     */
    private boolean readStepHistory(PluginCall call, String metric, LocalDate date) {
        long difference = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (difference < 0 || difference > watchDataDays) {
            call.reject("HISTORY_DATE_OUTSIDE_DEVICE_RETENTION");
            return false;
        }

        int dayOffset = (int) difference;
        JSArray records = new JSArray();
        if (originProtocolVersion >= 3) {
            manager.readOriginDataSingleDay(
                writeResponse,
                new IOriginData3Listener() {
                    @Override public void onOriginFiveMinuteListDataChange(List<OriginData3> data) {
                        for (OriginData3 item : data) {
                            appendStepHistoryRecord(records, item, date);
                        }
                    }
                    @Override public void onOriginHalfHourDataChange(OriginHalfHourData data) {}
                    @Override public void onOriginHRVOriginListDataChange(List<HRVOriginData> data) {}
                    @Override public void onOriginSpo2OriginListDataChange(List<Spo2hOriginData> data) {}
                    @Override public void onReadOriginProgressDetail(int day, String value, int total, int current) {}
                    @Override public void onReadOriginProgress(float progress) {}
                    @Override public void onReadOriginComplete() {
                        emitHistory(metric, date.toString(), records);
                        accept(call, "history.metric");
                    }
                },
                dayOffset,
                1,
                watchDataDays
            );
        } else {
            manager.readOriginDataSingleDay(
                writeResponse,
                new IOriginDataListener() {
                    @Override public void onOringinFiveMinuteDataChange(OriginData data) {
                        appendStepHistoryRecord(records, data, date);
                    }
                    @Override public void onOringinHalfHourDataChange(OriginHalfHourData data) {}
                    @Override public void onReadOriginProgressDetail(int day, String value, int total, int current) {}
                    @Override public void onReadOriginProgress(float progress) {}
                    @Override public void onReadOriginComplete() {
                        emitHistory(metric, date.toString(), records);
                        accept(call, "history.metric");
                    }
                },
                dayOffset,
                1,
                watchDataDays
            );
        }
        return true;
    }

    private void appendStepHistoryRecord(JSArray records, OriginData item, LocalDate date) {
        if (!sameDate(item.getmTime(), date)
            || (item.getStepValue() <= 0 && item.getDisValue() <= 0 && item.getCalValue() <= 0)) {
            return;
        }
        records.put(historyRecord(
            item.getmTime(),
            new JSObject()
                .put("steps", item.getStepValue())
                .put("distanceKm", item.getDisValue())
                .put("caloriesKcal", item.getCalValue()),
            null
        ));
    }

    /**
     * Lê uma única vez os blocos automáticos de cinco minutos e distribui-os
     * pelas métricas que a MF91 inclui no protocolo de origem. É a sequência
     * usada para preencher os gráficos diários sem repetir o mesmo download.
     */
    private void readDailyHistory(PluginCall call) {
        JSObject params = call.getObject("params");
        String dateText = params == null ? null : params.getString("date");
        if (dateText == null) {
            call.reject("HISTORY_DATE_REQUIRED");
            return;
        }
        final LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (RuntimeException error) {
            call.reject("HISTORY_DATE_INVALID");
            return;
        }
        long difference = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (difference < 0 || difference > watchDataDays) {
            call.reject("HISTORY_DATE_OUTSIDE_DEVICE_RETENTION");
            return;
        }

        JSArray steps = new JSArray();
        JSArray heartRate = new JSArray();
        JSArray bloodPressure = new JSArray();
        JSArray oxygen = new JSArray();
        JSArray temperature = new JSArray();
        JSArray bloodGlucose = new JSArray();
        JSArray stress = new JSArray();
        int dayOffset = (int) difference;

        if (originProtocolVersion >= 3) {
            manager.readOriginDataSingleDay(
                writeResponse,
                new IOriginData3Listener() {
                    @Override public void onOriginFiveMinuteListDataChange(List<OriginData3> data) {
                        for (OriginData3 item : data) {
                            appendOriginRecords(
                                item, date, steps, heartRate, bloodPressure,
                                oxygen, temperature, bloodGlucose, stress
                            );
                        }
                    }
                    @Override public void onOriginHalfHourDataChange(OriginHalfHourData data) {}
                    @Override public void onOriginHRVOriginListDataChange(List<HRVOriginData> data) {}
                    @Override public void onOriginSpo2OriginListDataChange(List<Spo2hOriginData> data) {}
                    @Override public void onReadOriginProgressDetail(int day, String value, int total, int current) {}
                    @Override public void onReadOriginProgress(float progress) {
                        emitMetric("sync", "progress", progress, null);
                    }
                    @Override public void onReadOriginComplete() {
                        emitDailyHistories(
                            date, steps, heartRate, bloodPressure, oxygen,
                            temperature, bloodGlucose, stress
                        );
                        accept(call, "history.daily");
                    }
                },
                dayOffset,
                1,
                watchDataDays
            );
            return;
        }

        manager.readOriginDataSingleDay(
            writeResponse,
            new IOriginDataListener() {
                @Override public void onOringinFiveMinuteDataChange(OriginData item) {
                    appendLegacyOriginRecords(
                        item, date, steps, heartRate, bloodPressure, temperature
                    );
                }
                @Override public void onOringinHalfHourDataChange(OriginHalfHourData data) {}
                @Override public void onReadOriginProgressDetail(int day, String value, int total, int current) {}
                @Override public void onReadOriginProgress(float progress) {
                    emitMetric("sync", "progress", progress, null);
                }
                @Override public void onReadOriginComplete() {
                    emitDailyHistories(
                        date, steps, heartRate, bloodPressure, oxygen,
                        temperature, bloodGlucose, stress
                    );
                    accept(call, "history.daily");
                }
            },
            dayOffset,
            1,
            watchDataDays
        );
    }

    private void appendOriginRecords(
        OriginData3 item,
        LocalDate date,
        JSArray steps,
        JSArray heartRate,
        JSArray bloodPressure,
        JSArray oxygen,
        JSArray temperature,
        JSArray bloodGlucose,
        JSArray stress
    ) {
        if (!sameDate(item.getmTime(), date)) {
            return;
        }
        appendStepHistoryRecord(steps, item, date);
        Double bpm = averagePositive(item.getPpgs());
        if (bpm != null) {
            heartRate.put(historyRecord(
                item.getmTime(),
                new JSObject().put("bpm", bpm),
                intArray(item.getPpgs())
            ));
        }
        appendBloodPressureOrigin(bloodPressure, item);
        Double percent = averagePositive(item.getOxygens());
        if (percent != null) {
            oxygen.put(historyRecord(
                item.getmTime(),
                new JSObject().put("percent", percent),
                intArray(item.getOxygens())
            ));
        }
        appendTemperatureOrigin(temperature, item);
        if (item.getBloodGlucose() > 0) {
            bloodGlucose.put(historyRecord(
                item.getmTime(),
                new JSObject()
                    .put("mmolL", item.getBloodGlucose())
                    .put("riskLevel", String.valueOf(item.getBloodGlucoseRiskLevel())),
                null
            ));
        }
        if (item.getPressure() > 0) {
            stress.put(historyRecord(
                item.getmTime(),
                new JSObject().put("score", item.getPressure()),
                null
            ));
        }
    }

    private void appendLegacyOriginRecords(
        OriginData item,
        LocalDate date,
        JSArray steps,
        JSArray heartRate,
        JSArray bloodPressure,
        JSArray temperature
    ) {
        if (!sameDate(item.getmTime(), date)) {
            return;
        }
        appendStepHistoryRecord(steps, item, date);
        if (item.getRateValue() > 0) {
            heartRate.put(historyRecord(
                item.getmTime(),
                new JSObject().put("bpm", item.getRateValue()),
                null
            ));
        }
        appendBloodPressureOrigin(bloodPressure, item);
        appendTemperatureOrigin(temperature, item);
    }

    private void appendBloodPressureOrigin(JSArray records, OriginData item) {
        if (item.getHighValue() < 60 || item.getLowValue() <= 0) {
            return;
        }
        records.put(historyRecord(
            item.getmTime(),
            new JSObject()
                .put("systolic", item.getHighValue())
                .put("diastolic", item.getLowValue()),
            null
        ));
    }

    private void appendTemperatureOrigin(JSArray records, OriginData item) {
        if (item.getTemperature() <= 0) {
            return;
        }
        records.put(historyRecord(
            item.getmTime(),
            new JSObject()
                .put("celsius", item.getTemperature())
                .put("baselineCelsius", item.getBaseTemperature()),
            null
        ));
    }

    private Double averagePositive(int[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        int total = 0;
        int count = 0;
        for (int value : values) {
            if (value > 0) {
                total += value;
                count += 1;
            }
        }
        return count == 0 ? null : (double) total / count;
    }

    private void emitDailyHistories(
        LocalDate date,
        JSArray steps,
        JSArray heartRate,
        JSArray bloodPressure,
        JSArray oxygen,
        JSArray temperature,
        JSArray bloodGlucose,
        JSArray stress
    ) {
        String day = date.toString();
        emitHistory("steps", day, steps);
        emitHistory("heartRate", day, heartRate);
        emitHistory("bloodPressure", day, bloodPressure);
        emitHistory("oxygen", day, oxygen);
        emitHistory("temperature", day, temperature);
        emitHistory("bloodGlucose", day, bloodGlucose);
        emitHistory("stress", day, stress);
    }

    private DeviceManualDataType manualType(String metric) {
        switch (metric) {
            case "heartRate": return DeviceManualDataType.HEART_RATE;
            case "bloodPressure": return DeviceManualDataType.BLOOD_PRESSURE;
            case "oxygen": return DeviceManualDataType.BLOOD_OXYGEN;
            case "temperature": return DeviceManualDataType.BODY_TEMPERATURE;
            case "bloodGlucose": return DeviceManualDataType.BLOOD_GLUCOSE;
            case "stress": return DeviceManualDataType.STRESS;
            default: return null;
        }
    }

    private void readManualMetricHistory(
        PluginCall call,
        String metric,
        LocalDate date,
        DeviceManualDataType dataType
    ) {
        long start = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        JSArray records = new JSArray();
        List<DeviceManualDataType> requested = Collections.singletonList(dataType);
        manager.readDeviceManualData(
            writeResponse,
            start,
            requested,
            requested,
            new AbsDeviceManualDetectDataListener() {
                @Override public void onHeartRateDataChange(List<HeartRateManualData> data) {
                    for (HeartRateManualData item : data) {
                        if (inDay(item.getTimeStamp(), start, end)) {
                            int[] rates = item.getRate();
                            records.put(historyRecord(item.getTimeStamp(), new JSObject()
                                .put("bpm", rates != null && rates.length > 0 ? rates[rates.length - 1] : 0), intArray(rates)));
                        }
                    }
                }

                @Override public void onBloodPressureDataChange(List<BloodPressureManualData> data) {
                    for (BloodPressureManualData item : data) {
                        if (inDay(item.getTimeStamp(), start, end)) {
                            records.put(historyRecord(item.getTimeStamp(), new JSObject()
                                .put("systolic", item.getSystolic())
                                .put("diastolic", item.getDiastolic())
                                .put("pulseBpm", item.getHeartRate()), null));
                        }
                    }
                }

                @Override public void onBloodOxygenDataChange(List<BloodOxygenManualData> data) {
                    for (BloodOxygenManualData item : data) {
                        if (inDay(item.getTimeStamp(), start, end)) {
                            int[] values = item.getOxygen();
                            records.put(historyRecord(item.getTimeStamp(), new JSObject()
                                .put("percent", values != null && values.length > 0 ? values[values.length - 1] : 0), intArray(values)));
                        }
                    }
                }

                @Override public void onBodyTemperatureDataChange(List<BodyTemperatureManualData> data) {
                    for (BodyTemperatureManualData item : data) {
                        if (inDay(item.getTimeStamp(), start, end)) {
                            records.put(historyRecord(item.getTimeStamp(), new JSObject()
                                .put("celsius", item.getTemperature())
                                .put("baselineCelsius", item.getBaseTemperature()), null));
                        }
                    }
                }

                @Override public void onBloodGlucoseDataChange(List<BloodGlucoseManualData> data) {
                    for (BloodGlucoseManualData item : data) {
                        if (inDay(item.getTimeStamp(), start, end)) {
                            records.put(historyRecord(item.getTimeStamp(), new JSObject()
                                .put("mmolL", item.getBloodGlucoseValue())
                                .put("riskLevel", String.valueOf(item.getRisk())), null));
                        }
                    }
                }

                @Override public void onHrvManualDataChange(List<HrvManualData> data) {}

                @Override public void onMetoManualDataChange(List<MetoManualData> data) {}

                @Override public void onPressureManualDataChange(List<PressureManualData> data) {
                    for (PressureManualData item : data) {
                        if (inDay(item.getTimeStamp(), start, end)) {
                            records.put(historyRecord(item.getTimeStamp(), new JSObject().put("score", item.getPressure()), null));
                        }
                    }
                }

                @Override public void onReadComplete() {
                    emitHistory(metric, date.toString(), records);
                    accept(call, "history.metric");
                }

                @Override public void onReadFail() {
                    emitLog("error", "Manual history read failed", "history.metric");
                    emitHistory(metric, date.toString(), records);
                    accept(call, "history.metric");
                }
            }
        );
    }

    private void readEcgHistory(PluginCall call, String metric, LocalDate date) {
        TimeData day = new TimeData(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        manager.readECGData(directWriteResponse, day, EEcgDataType.ALL, new IECGReadDataListener() {
            @Override public void readDataFinish(List<EcgDetectResult> data) {
                JSArray records = new JSArray();
                for (EcgDetectResult item : data) {
                    records.put(historyRecord(item.getTimeBean(), ecgValues(item), intArray(item.getFilterSignals())));
                }
                emitHistory(metric, date.toString(), records);
                accept(call, "history.metric");
            }

            @Override public void readDiagnosisDataFinish(List<EcgDiagnosis> data) {}
        });
    }

    private void readBodyComponentHistory(PluginCall call, String metric, LocalDate date) {
        manager.readBodyComponentData(directWriteResponse, data -> {
            JSArray records = new JSArray();
            for (BodyComponent item : data) {
                if (sameDate(item.getTimeBean(), date)) {
                    records.put(historyRecord(item.getTimeBean(), bodyComponentValues(item), null));
                }
            }
            emitHistory(metric, date.toString(), records);
            accept(call, "history.metric");
        });
    }

    private JSObject ecgValues(EcgDetectResult result) {
        return new JSObject()
            .put("bpm", result.getAveHeart())
            .put("hrvMilliseconds", result.getAveHrv())
            .put("qtMilliseconds", result.getAveQT())
            .put("durationSeconds", result.getDuration());
    }

    private JSObject bodyComponentValues(BodyComponent item) {
        return new JSObject()
            .put("bmi", item.getBMI())
            .put("bodyFatPercent", item.getBodyFatRate())
            .put("waterPercent", item.getBodyWater())
            .put("muscleMassKg", item.getMuscleMass())
            .put("boneMassKg", item.getBoneMass())
            .put("basalMetabolismKcal", item.getBasalMetabolicRate());
    }

    private JSObject historyRecord(long timestamp, JSObject values, JSArray samples) {
        JSObject record = new JSObject()
            .put("timestamp", Instant.ofEpochSecond(timestamp).toString())
            .put("values", values);
        if (samples != null) {
            record.put("samples", samples);
        }
        return record;
    }

    private JSObject historyRecord(TimeData time, JSObject values, JSArray samples) {
        JSObject record = new JSObject()
            .put("timestamp", time == null ? Instant.now().toString() : time.toFullDateTimeString())
            .put("values", values);
        if (samples != null) {
            record.put("samples", samples);
        }
        return record;
    }

    private boolean inDay(long timestamp, long start, long end) {
        return timestamp >= start && timestamp < end;
    }

    private boolean sameDate(TimeData time, LocalDate date) {
        return time != null
            && time.getYear() == date.getYear()
            && time.getMonth() == date.getMonthValue()
            && time.getDay() == date.getDayOfMonth();
    }

    private JSArray intArray(int[] values) {
        if (values == null) {
            return null;
        }
        JSArray result = new JSArray();
        for (int value : values) {
            result.put(value);
        }
        return result;
    }

    /**
     * O primeiro array do callback ADC contém o sinal ECG. Remove o marcador
     * Integer.MAX_VALUE que o SDK usa para amostras sem valor.
     */
    private JSArray ecgSamples(int[] values) {
        JSArray result = new JSArray();
        if (values == null) {
            return result;
        }
        for (int value : values) {
            if (value != Integer.MAX_VALUE) {
                result.put(value);
            }
        }
        return result;
    }

    private void emitHistory(String metric, String date, JSArray records) {
        JSObject event = new JSObject();
        event.put("type", "history");
        event.put("metric", metric);
        event.put("date", date);
        event.put("timestamp", Instant.now().toString());
        event.put("values", new JSObject().put("records", records.length()));
        event.put("records", records);
        notifyListeners("data", event, true);
    }

    private void readCurrentActivity(PluginCall call) {
        manager.readSportStep(writeResponse, data -> {
            JSObject values = new JSObject();
            values.put("steps", data.getStep());
            values.put("distanceKm", data.getDis());
            values.put("caloriesKcal", data.getKcal());
            emitData("steps", values, null, data.toString());
            accept(call, "history.activity.current");
        });
    }

    private void readSleep(PluginCall call) {
        manager.readSleepData(writeResponse, new ISleepDataListener() {
            @Override
            public void onSleepDataChange(String date, SleepData data) {
                emitData("sleep", new JSObject().put("date", date).put("summary", data.toString()), null, data.toString());
            }
            @Override public void onSleepProgress(float progress) { emitMetric("sleep", "progress", progress, null); }
            @Override public void onSleepProgressDetail(String date, int progress) {}
            @Override public void onReadSleepComplete() { emitLog("success", "Sleep history complete", "history.sleep"); }
        }, 1);
        accept(call, "history.sleep");
    }

    private void readOrigin(PluginCall call) {
        manager.readOriginData(writeResponse, new IOriginDataListener() {
            @Override public void onOringinFiveMinuteDataChange(OriginData data) {
                emitData("origin", new JSObject().put("interval", "5m").put("summary", data.toString()), null, data.toString());
            }
            @Override public void onOringinHalfHourDataChange(OriginHalfHourData data) {
                emitData("origin", new JSObject().put("interval", "30m").put("summary", data.toString()), null, data.toString());
            }
            @Override public void onReadOriginProgressDetail(int day, String date, int allPackage, int currentPackage) {}
            @Override public void onReadOriginProgress(float progress) { emitMetric("origin", "progress", progress, null); }
            @Override public void onReadOriginComplete() { emitLog("success", "Origin history complete", "history.origin"); }
        }, 3);
        accept(call, "history.origin");
    }

    private void readOxygenHistory(PluginCall call) {
        manager.readSpo2hOrigin(writeResponse, new ISpo2hOriginDataListener() {
            @Override public void onReadOriginProgress(float progress) { emitMetric("oxygenHistory", "progress", progress, null); }
            @Override public void onReadOriginProgressDetail(int day, String date, int allPackage, int currentPackage) {}
            @Override public void onSpo2hOriginListener(Spo2hOriginData data) {
                emitData("oxygenHistory", new JSObject().put("summary", data.toString()), null, data.toString());
            }
            @Override public void onReadOriginComplete() { emitLog("success", "Oxygen history complete", "history.oxygen"); }
        }, 0);
        accept(call, "history.oxygen");
    }

    private void readTemperatureHistory(PluginCall call) {
        manager.readTemptureDataBySetting(writeResponse, new ITemptureDataListener() {
            @Override public void onTemptureDataListDataChange(List<TemptureData> data) {
                emitData("temperatureHistory", new JSObject().put("records", data.size()).put("summary", data.toString()), null, data.toString());
            }
            @Override public void onReadOriginProgressDetail(int day, String date, int allPackage, int currentPackage) {}
            @Override public void onReadOriginProgress(float progress) { emitMetric("temperatureHistory", "progress", progress, null); }
            @Override public void onReadOriginComplete() { emitLog("success", "Temperature history complete", "history.temperature"); }
        }, new ReadOriginSetting(0, 1, false, 1));
        accept(call, "history.temperature");
    }

    private final IABleConnectStatusListener connectStatusListener = new IABleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == Constants.STATUS_CONNECTED) {
                if (!"connected".equals(connectionState) && !"authenticating".equals(connectionState)) {
                    setConnectionState("connecting");
                }
            } else if (status == Constants.STATUS_DISCONNECTED) {
                setConnectionState("disconnected");
                if (!intentionalDisconnect) {
                    scheduleReconnect();
                }
            }
        }
    };

    /**
     * Aplica espera exponencial limitada para evitar ciclos agressivos de
     * ligação quando a pulseira fica temporariamente fora de alcance.
     */
    private void scheduleReconnect() {
        if (intentionalDisconnect || currentAddress == null) {
            return;
        }
        long delayMs = Math.min(30_000L, 3_000L * (1L << Math.min(reconnectAttempts, 3)));
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, delayMs);
        emitLog("warning", "Reconnect scheduled in " + delayMs + " ms", "session.reconnect");
    }

    private void startConnectionService() {
        Intent intent = new Intent(getContext(), HBandConnectionService.class);
        intent.putExtra(HBandConnectionService.EXTRA_DEVICE_NAME, currentName);
        ContextCompat.startForegroundService(getContext(), intent);
    }

    private void stopConnectionService() {
        getContext().stopService(new Intent(getContext(), HBandConnectionService.class));
    }

    private JSObject buildStatus() {
        JSObject status = new JSObject();
        status.put("available", true);
        status.put("bluetoothEnabled", manager.isBluetoothOpened());
        status.put("state", connectionState);
        status.put("capabilities", capabilities);
        status.put("sdkVersion", VPOperateManager.VPPROTOCOL_VERSION);
        status.put("platform", "android");
        if (currentAddress != null) {
            JSObject device = new JSObject();
            device.put("id", currentAddress);
            device.put("name", currentName == null ? currentAddress : currentName);
            device.put("firmware", firmwareVersion);
            device.put("hardware", hardwareVersion);
            SearchResult result = discoveredDevices.get(currentAddress);
            if (result != null) {
                device.put("rssi", result.rssi);
            }
            status.put("device", device);
        }
        return status;
    }

    private JSObject devicePayload(SearchResult result) {
        JSObject device = new JSObject();
        device.put("id", result.getAddress());
        device.put("name", result.getName() == null ? "" : result.getName());
        device.put("rssi", result.rssi);
        return device;
    }

    private void setConnectionState(String state) {
        connectionState = state;
        emitStatus();
    }

    /**
     * Um cancelamento de scan pode chegar depois de a ligação já ter começado.
     * Só o scan que ainda está ativo tem autorização para repor o estado idle.
     */
    private void finishScanState() {
        if ("scanning".equals(connectionState)) {
            setConnectionState("idle");
        }
    }

    private void emitStatus() {
        notifyListeners("statusChanged", buildStatus(), true);
    }

    private void emitMetric(String type, String key, Object value, String raw) {
        emitData(type, new JSObject().put(key, value), null, raw);
    }

    private void emitData(String type, JSObject values, JSArray samples, String raw) {
        JSObject event = new JSObject();
        event.put("type", type);
        event.put("timestamp", Instant.now().toString());
        event.put("values", values);
        if (samples != null) {
            event.put("samples", samples);
        }
        if (raw != null) {
            event.put("raw", raw);
        }
        notifyListeners("data", event, true);
    }

    private void emitLog(String level, String message, String operation) {
        JSObject log = new JSObject();
        log.put("id", System.nanoTime() + "-" + (operation == null ? "sdk" : operation));
        log.put("timestamp", Instant.now().toString());
        log.put("level", level);
        log.put("message", message);
        if (operation != null) {
            log.put("operation", operation);
        }
        notifyListeners("log", log, true);
    }

    private void accept(PluginCall call, String operation) {
        JSObject result = new JSObject();
        result.put("operation", operation);
        result.put("accepted", true);
        call.resolve(result);
    }

    private JSObject permissionResult(boolean granted) {
        return new JSObject().put("granted", granted);
    }

    private void resolvePendingConnect() {
        if (pendingConnectCall == null) {
            return;
        }
        pendingConnectCall.resolve();
        pendingConnectCall = null;
        emitLog("success", "Device authenticated", "session.authenticate");
    }

    private void rejectPendingConnect(String message) {
        if (pendingConnectCall == null) {
            return;
        }
        pendingConnectCall.reject(message);
        pendingConnectCall = null;
        emitLog("error", message, "session.authenticate");
    }
}
