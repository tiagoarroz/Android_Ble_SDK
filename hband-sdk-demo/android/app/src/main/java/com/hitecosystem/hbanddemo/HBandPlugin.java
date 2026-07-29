package com.hitecosystem.hbanddemo;

import android.Manifest;
import android.os.Build;

import androidx.annotation.NonNull;

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
import com.veepoo.protocol.listener.data.IBloodGlucoseChangeListener;
import com.veepoo.protocol.listener.data.IBreathDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.IFatigueDataListener;
import com.veepoo.protocol.listener.data.IGsrDetectListener;
import com.veepoo.protocol.listener.data.IHRVOriginDataListener;
import com.veepoo.protocol.listener.data.IHeartDataListener;
import com.veepoo.protocol.listener.data.IHrvDetectListener;
import com.veepoo.protocol.listener.data.IOriginDataListener;
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
import com.veepoo.protocol.model.datas.BpData;
import com.veepoo.protocol.model.datas.BreathData;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FatigueData;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.datas.GsrDetectResult;
import com.veepoo.protocol.model.datas.HRVOriginData;
import com.veepoo.protocol.model.datas.HeartData;
import com.veepoo.protocol.model.datas.OriginData;
import com.veepoo.protocol.model.datas.OriginHalfHourData;
import com.veepoo.protocol.model.datas.PersonInfoData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.Spo2hData;
import com.veepoo.protocol.model.datas.Spo2hOriginData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.model.datas.TemptureData;
import com.veepoo.protocol.model.datas.TemptureDetectData;
import com.veepoo.protocol.model.enums.EBPDetectModel;
import com.veepoo.protocol.model.enums.EBloodGlucoseRiskLevel;
import com.veepoo.protocol.model.enums.EBloodGlucoseStatus;
import com.veepoo.protocol.model.enums.EFunctionStatus;
import com.veepoo.protocol.model.enums.EPwdStatus;
import com.veepoo.protocol.model.enums.ESex;
import com.veepoo.protocol.model.enums.ETimeMode;
import com.veepoo.protocol.model.enums.GsrDetectAck;
import com.veepoo.protocol.model.enums.HrvDetectState;
import com.veepoo.protocol.model.enums.PressureDetectState;
import com.veepoo.protocol.model.settings.DeviceTimeSetting;
import com.veepoo.protocol.model.settings.ReadOriginSetting;

import java.time.Instant;
import java.util.Calendar;
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
    private final IBleWriteResponse writeResponse = code -> emitLog(
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

    @Override
    public void load() {
        manager.init(getContext().getApplicationContext());
        VPOperateManager.setShowFunctionNotSupportToast(false);
        manager.registerBluetoothStateListener(new IABluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean isOpen) {
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
                setConnectionState("idle");
                emitLog("success", "BLE scan completed", "session.scan");
            }

            @Override
            public void onSearchCanceled() {
                setConnectionState("idle");
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
        manager.stopScanDevice();
        setConnectionState("connecting");
        manager.registerConnectStatusListener(deviceId, connectStatusListener);
        manager.connectDevice(deviceId, currentName, new IConnectResponse() {
            @Override
            public void connectState(int code, BleGattProfile profile, boolean isOadModel) {
                if (code != Code.REQUEST_SUCCESS) {
                    rejectPendingConnect("BLE_CONNECT_FAILED_" + code);
                    setConnectionState("error");
                }
            }
        }, new INotifyResponse() {
            @Override
            public void notifyState(int state) {
                if (state != Code.REQUEST_SUCCESS) {
                    rejectPendingConnect("BLE_NOTIFY_FAILED_" + state);
                    setConnectionState("error");
                    return;
                }
                authenticateCurrentDevice();
            }
        });
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        manager.disconnectWatch(code -> {
            setConnectionState("disconnected");
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
            case "measure.hrv.start":
                startHrv(call);
                return;
            case "measure.hrv.stop":
                manager.stopDetectHrv(null, hrvListener);
                accept(call, operation);
                return;
            case "measure.stress.start":
                startStress(call);
                return;
            case "measure.stress.stop":
                manager.stopDetectPressure(null);
                accept(call, operation);
                return;
            case "measure.gsr.start":
                startGsr(call);
                return;
            case "measure.gsr.stop":
                manager.stopDetectGsr(null);
                accept(call, operation);
                return;
            case "measure.bloodGlucose.start":
                startBloodGlucose(call);
                return;
            case "measure.bloodGlucose.stop":
                manager.stopBloodGlucoseDetect(writeResponse, bloodGlucoseListener);
                accept(call, operation);
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
            case "history.hrv":
                readHrvHistory(call);
                return;
            case "history.temperature":
                readTemperatureHistory(call);
                return;
            default:
                call.reject("UNIMPLEMENTED_OPERATION: " + operation);
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
                setConnectionState("connected");
                resolvePendingConnect();
            }

            @Override
            public void onConnectionConfirmTimeout() {
                rejectPendingConnect("CONNECTION_CONFIRM_TIMEOUT");
                setConnectionState("error");
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
        putCapability("heartRate", data.getHeartDetect());
        putCapability("bloodPressure", data.getBp());
        putCapability("bloodOxygen", data.getSpo2H());
        putCapability("breathing", data.getBeathFunction());
        putCapability("temperature", data.getTemperatureFunction());
        putCapability("hrv", data.getHrvFunction());
        putCapability("ecg", data.getEcg());
        putCapability("bloodGlucose", data.getBloodGlucose());
        putCapability("fatigue", data.getFatigue());
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
            values.put("percent", data.isPercent() ? data.getBatteryPercent() : data.getBatteryLevel());
            values.put("lowBattery", data.isLowBattery());
            values.put("state", data.getState());
            emitData("battery", values, null, data.toString());
        });
        accept(call, "device.battery");
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
        manager.settingTime(writeResponse, state -> emitLog("success", "Time sync state: " + state, "device.time"), setting);
        accept(call, "device.time");
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

    private final IHrvDetectListener hrvListener = new IHrvDetectListener() {
        @Override
        public void onHrvDetect(int value) {
            emitMetric("hrv", "milliseconds", value, null);
        }

        @Override
        public void onDetectFailed(HrvDetectState state) {
            emitLog("error", "HRV failed: " + state, "measure.hrv.start");
        }

        @Override
        public void onDetectStop() {
            emitLog("info", "HRV stopped", "measure.hrv.stop");
        }
    };

    private void startHrv(PluginCall call) {
        manager.startDetectHrv(null, hrvListener);
        accept(call, "measure.hrv.start");
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
        manager.startDetectPressure(null, pressureListener);
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
        manager.startDetectGsr(null, gsrListener);
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

    private void readCurrentActivity(PluginCall call) {
        manager.readSportStep(writeResponse, data -> {
            JSObject values = new JSObject();
            values.put("steps", data.getStep());
            values.put("distanceKm", data.getDis());
            values.put("caloriesKcal", data.getKcal());
            emitData("activity", values, null, data.toString());
        });
        accept(call, "history.activity.current");
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

    private void readHrvHistory(PluginCall call) {
        manager.readHRVOrigin(writeResponse, new IHRVOriginDataListener() {
            @Override public void onReadOriginProgress(float progress) { emitMetric("hrvHistory", "progress", progress, null); }
            @Override public void onReadOriginProgressDetail(int day, String date, int allPackage, int currentPackage) {}
            @Override public void onHRVOriginListener(HRVOriginData data) {
                emitData("hrvHistory", new JSObject().put("summary", data.toString()), null, data.toString());
            }
            @Override public void onDayHrvScore(int day, String date, int score) { emitMetric("hrvHistory", "score", score, date); }
            @Override public void onReadOriginComplete() { emitLog("success", "HRV history complete", "history.hrv"); }
        }, 0);
        accept(call, "history.hrv");
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
                setConnectionState("connecting");
            } else if (status == Constants.STATUS_DISCONNECTED) {
                setConnectionState("disconnected");
            }
        }
    };

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
