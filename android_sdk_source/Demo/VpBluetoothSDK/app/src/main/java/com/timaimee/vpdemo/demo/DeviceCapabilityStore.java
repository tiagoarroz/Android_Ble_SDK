package com.timaimee.vpdemo.demo;

import static com.timaimee.vpdemo.activity.Oprate.*;
import static com.veepoo.protocol.model.enums.EFunctionStatus.SUPPORT;
import static com.veepoo.protocol.model.enums.EFunctionStatus.SUPPORT_CLOSE;
import static com.veepoo.protocol.model.enums.EFunctionStatus.SUPPORT_OPEN;

import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.enums.DeviceManualDataType;
import com.veepoo.protocol.model.enums.EFunctionStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mantém em memória as capacidades reportadas no handshake BLE.
 * As classes do SDK não garantem serialização entre activities, por isso este
 * estado fica centralizado no processo da demo e é reposto a cada validação.
 */
public final class DeviceCapabilityStore {
    private static FunctionDeviceSupportData functionSupport;
    private static DeviceFunctionPackage1 package1;
    private static DeviceFunctionPackage2 package2;
    private static DeviceFunctionPackage3 package3;
    private static DeviceFunctionPackage4 package4;
    private static DeviceFunctionPackage5 package5;
    private static boolean socialMessageSupportReported;
    private static final Set<DeviceManualDataType> manualDetectTypes = new HashSet<>();

    private static final Set<String> ALWAYS_AVAILABLE = new HashSet<>(Arrays.asList(
            PWD_COMFIRM_2_DISCONNECT,
            PWD_COMFIRM_2_DISCONNECT_,
            PERSONINFO_SYNC,
            SETTING_FIRST,
            DISCONNECT,
            BT_CONNECT,
            BLE_DISCONNECT,
            PWD_MODIFY,
            SHARE_LOG,
            SPORT_CURRENT_READ,
            LANGUAGE_CHINESE,
            LANGUAGE_ENGLISH,
            BATTERY,
            CLEAR_DEVICE_DATA,
            SET_WATCH_TIME,
            SHOW_SP,
            GATT_CLOSE
    ));

    private DeviceCapabilityStore() {
    }

    public static void reset() {
        functionSupport = null;
        package1 = null;
        package2 = null;
        package3 = null;
        package4 = null;
        package5 = null;
        socialMessageSupportReported = false;
        manualDetectTypes.clear();
    }

    public static void setFunctionSupport(FunctionDeviceSupportData data) {
        functionSupport = data;
    }

    public static void setPackage1(DeviceFunctionPackage1 data) {
        package1 = data;
    }

    public static void setPackage2(DeviceFunctionPackage2 data) {
        package2 = data;
    }

    public static void setPackage3(DeviceFunctionPackage3 data) {
        package3 = data;
    }

    public static void setPackage4(DeviceFunctionPackage4 data) {
        package4 = data;
    }

    public static void setPackage5(DeviceFunctionPackage5 data) {
        package5 = data;
    }

    public static void setSocialMessageSupport(FunctionSocailMsgData data) {
        socialMessageSupportReported = data != null;
    }

    /**
     * Guarda os tipos que o firmware aceita para medições manuais iniciadas pela app.
     * Estes dados vêm do próprio SDK depois dos pacotes A7/B8 do handshake.
     */
    public static void setManualDetectTypes(List<DeviceManualDataType> dataTypes) {
        manualDetectTypes.clear();
        if (dataTypes != null) {
            manualDetectTypes.addAll(dataTypes);
        }
    }

    public static boolean hasDeviceCapabilities() {
        return functionSupport != null
                || package1 != null
                || package2 != null
                || package3 != null
                || package4 != null
                || package5 != null;
    }

    public static boolean isOperationEnabled(int index, String operation) {
        if (ALWAYS_AVAILABLE.contains(operation) || isHistoricalRead(operation)) {
            return true;
        }
        if (!hasDeviceCapabilities()) {
            return false;
        }
        if (isEmptyOperation(index)) {
            return resolveEmptyOperation(index);
        }
        return resolveOperation(operation);
    }

    public static int countEnabledOperations(String[] operations) {
        int enabled = 0;
        for (int i = 0; i < operations.length; i++) {
            if (isOperationEnabled(i, operations[i])) {
                enabled++;
            }
        }
        return enabled;
    }

    private static boolean resolveOperation(String operation) {
        if (matches(operation, HEART_DETECT_START, HEART_DETECT_STOP)) {
            return isManualSupported(DeviceManualDataType.HEART_RATE);
        }
        if (matches(operation, TEMPTURE_DETECT_START, TEMPTURE_DETECT_STOP)) {
            return isManualSupported(DeviceManualDataType.BODY_TEMPERATURE);
        }
        if (matches(operation, READ_TEMPTURE_DATA)) {
            return isSupported(firstStatus(statusPackage3Temperature(), statusLegacyTemperature()));
        }
        if (matches(operation, BP_DETECT_START, BP_DETECT_STOP)) {
            return isManualSupported(DeviceManualDataType.BLOOD_PRESSURE);
        }
        if (matches(operation, BP_DETECTMODEL_SETTING, BP_DETECTMODEL_READ)) {
            return isSupported(firstStatus(statusPackage1BloodPressure(), statusLegacyBloodPressure()));
        }
        if (matches(operation, BP_DETECTMODEL_SETTING_ADJUSTE, BP_DETECTMODEL_SETTING_ADJUSTE_CANCEL,
                BP_FUNCTION_READ, BP_FUNCTION_SETTING)) {
            return isSupported(firstStatus(statusPackage1BloodPressureAdjust(), statusLegacyBloodPressureAdjust()));
        }
        if (matches(operation, CAMERA_START, CAMERA_STOP)) {
            return isSupported(firstStatus(statusPackage1Camera(), statusLegacyCamera()));
        }
        if (matches(operation, ALARM_SETTING, ALARM_READ, ALARM_NEW_LISTENER, ALARM_NEW_)) {
            return isSupported(firstStatus(statusPackage1Alarm(), statusLegacyAlarm()));
        }
        if (matches(operation, LONGSEAT_SETTING_OPEN, LONGSEAT_SETTING_CLOSE, LONGSEAT_READ)) {
            return isSupported(firstStatus(statusPackage1Sedentary(), statusLegacySedentary()));
        }
        if (matches(operation, NIGHT_TURN_WRIST_OPEN, NIGHT_TURN_WRIST_CLOSE, NIGHT_TURN_WRIST_READ,
                NIGHT_TURN_WRIST_CUSTOM_TIME, NIGHT_TURN_WRIST_CUSTOM_TIME_LEVEL)) {
            return isSupported(firstStatus(statusPackage1NightTurn(), statusLegacyNightTurn()));
        }
        if (matches(operation, FINDDEVICE_SETTING_OPEN, FINDDEVICE_SETTING_CLOSE, FINDDEVICE_READ, FIND_DEVICE)) {
            return isSupported(firstStatus(statusPackage3FindDevice(), statusLegacyFindDevice()));
        }
        if (matches(operation, SOCIAL_MSG_SETTING, SOCIAL_MSG_SETTING2, SOCIAL_MSG_READ, SOCIAL_MSG_SEND,
                SOCIAL_PHONE_IDLE_OR_OFFHOOK, DEVICE_CONTROL_PHONE)) {
            return socialMessageSupportReported;
        }
        if (matches(operation, HEARTWRING_READ, HEARTWRING_OPEN, HEARTWRING_CLOSE)) {
            return isSupported(firstStatus(statusPackage1HeartWarning(), statusLegacyHeartWarning()));
        }
        if (matches(operation, SPO2H_OPEN, SPO2H_CLOSE)) {
            return isManualSupported(DeviceManualDataType.BLOOD_OXYGEN);
        }
        if (matches(operation, SPO2H_AUTO_DETECT_READ, SPO2H_AUTO_DETECT_OPEN,
                SPO2H_AUTO_DETECT_CLOSE, SPO2H_ORIGIN_READ)) {
            return isSupported(firstStatus(statusPackage1Spo2h(), statusLegacySpo2h()));
        }
        if (matches(operation, FATIGUE_OPEN, FATIGUE_CLOSE)) {
            return isManualSupported(DeviceManualDataType.FATIGUE);
        }
        if (matches(operation, WOMEN_SETTING, WOMEN_READ)) {
            return isSupported(firstStatus(statusPackage1Women(), statusLegacyWomen()));
        }
        if (matches(operation, COUNT_DOWN_WATCH_CLOSE_UI, COUNT_DOWN_WATCH_OPEN_UI, COUNT_DOWN_APP,
                COUNT_DOWN_APP_READ)) {
            return isSupported(firstStatus(statusPackage2CountDown(), statusLegacyCountDown()));
        }
        if (matches(operation, AIM_SPROT_CALC)) {
            return isSupported(firstStatus(statusPackage1NewSportCalc(), statusLegacyNewSportCalc()));
        }
        if (matches(operation, SCREEN_LIGHT_SETTING, SCREEN_LIGHT_READ)) {
            return isSupported(firstStatus(statusPackage1ScreenLight(), statusLegacyScreenLight()));
        }
        if (matches(operation, SCREEN_STYLE_READ, SCREEN_STYLE_SETTING)) {
            return isSupported(firstStatus(statusPackage3ScreenStyle(), statusLegacyScreenStyle()));
        }
        if (matches(operation, WEATHER_READ_STATUEINFO, WEATHER_SETTING_STATUEINFO_ON,
                WEATHER_SETTING_STATUEINFO_OFF, WEATHER_SETTING_DATA)) {
            return isSupported(firstStatus(statusPackage2Weather(), statusLegacyWeather()));
        }
        if (matches(operation, LOW_POWER_READ, LOW_POWER_OPEN, LOW_POWER_CLOSE)) {
            return isSupported(firstStatus(statusPackage2LowPower(), statusLegacyLowPower()));
        }
        if (matches(operation, DETECT_START_ECG, DETECT_STOP_ECG, DEVICE_ECG_ALWAYS_OPEN,
                DEVICE_ECG_ALWAYS_CLOSE, ECG_AUTO_REPORT_TEXT, READ_ECG_ID, READ_ECG_DATA,
                SET_ECG_NEW_DATA_REPORT, RR)) {
            return isSupported(firstStatus(statusPackage2Ecg(), statusLegacyEcg()));
        }
        if (matches(operation, SPORT_MODE_ORIGIN_READ, SPORT_MODE_ORIGIN_READSTAUTS,
                SPORT_MODE_START_INDOOR, SPORT_MODE_ORIGIN_START, SPORT_MODE_ORIGIN_END)) {
            return isSupported(firstStatus(statusPackage2SportModel(), statusLegacySportModel()));
        }
        if (matches(operation, HRV_START_DETECT, HRV_STOP_DETECT)) {
            return isManualSupported(DeviceManualDataType.HRV);
        }
        if (matches(operation, HRV_ORIGIN_READ)) {
            return isSupported(firstStatus(statusPackage2Hrv(), statusLegacyHrv()));
        }
        if (matches(operation, TEXT_ALARM_READ, TEXT_ALARM_ADD, TEXT_ALARM_MODIFY, TEXT_ALARM_DELETE,
                TEXT_ALARM)) {
            return isSupported(firstStatus(statusPackage1TextAlarm(), statusLegacyTextAlarm()));
        }
        if (matches(operation, CONTACT)) {
            return isSupported(firstStatus(statusPackage3Contact(), statusLegacyContact()));
        }
        if (matches(operation, UI_UPDATE_AGPS)) {
            return isSupported(firstStatus(statusPackage3Agps(), statusLegacyAgps()));
        }
        if (matches(operation, UI_UPDATE_CUSTOM)) {
            return getWatchUiCustomCount() > 0;
        }
        if (matches(operation, UI_UPDATE_SERVER)) {
            return getWatchUiServerCount() > 0;
        }
        if (matches(operation, START_BLOOD_GLUCOSE, STOP_BLOOD_GLUCOSE)) {
            return isManualSupported(DeviceManualDataType.BLOOD_GLUCOSE);
        }
        if (matches(operation, BLOOD_GLUCOSE_P_READ, BLOOD_GLUCOSE_P_SETTING)) {
            return isSupported(firstStatus(statusPackage3BloodGlucoseAdjust(), statusLegacyBloodGlucoseAdjust()));
        }
        if (matches(operation, BLOOD_GLUCOSE_MULTIPLE_READ, BLOOD_GLUCOSE_MULTIPLE_SETTING)) {
            return isSupported(firstStatus(statusPackage3BloodGlucoseMultiAdjust(), statusLegacyBloodGlucoseMultiAdjust()));
        }
        if (matches(operation, READ_BODY_COMPONENT_ID, READ_BODY_COMPONENT_DATA, DETECT_START_BODY_COMPONENT,
                DETECT_STOP_BODY_COMPONENT, SET_BODY_COMPONENT_NEW_DATA_REPORT)) {
            return isSupported(firstStatus(statusPackage4BodyComponent(), statusLegacyBodyComponent()));
        }
        if (matches(operation, READ_BLOOD_COMPOSITION_CALIBRATION, SETTING_BLOOD_COMPOSITION_CALIBRATION)) {
            return isSupported(firstStatus(statusPackage4BloodComponentCalibration(), statusLegacyBloodComponentCalibration()));
        }
        if (matches(operation, DETECT_START_BLOOD_COMPONENT, DETECT_STOP_BLOOD_COMPONENT)) {
            return isManualSupported(DeviceManualDataType.BLOOD_COMPOSITION);
        }
        if (matches(operation, WORLD_CLOCK)) {
            return isSupported(firstStatus(statusPackage4WorldClock(), statusLegacyWorldClock()));
        }
        if (matches(operation, TEXT_IMAGE_MSG_PUSH)) {
            return isSupported(firstStatus(statusPackage5TextImagePush(), statusLegacyTextImagePush()));
        }
        if (matches(operation, GSR_START, GSR_STOP)) {
            return isManualSupported(DeviceManualDataType.SKIN_CONDUCTANCE);
        }
        if (matches(operation, GNSS_SOS_SAFETY_PROTECTION)) {
            return isSupported(firstStatus(statusPackage5SafetyProtection(), statusLegacySafetyProtection()));
        }
        if (matches(operation, MINI_CHECKUP)) {
            return isManualSupported(DeviceManualDataType.MINI_CHECKUP);
        }
        if (matches(operation, FUN_4G)) {
            return isSupported(statusLegacy4g());
        }
        if (matches(operation, AUTO_MEASURE)) {
            return isSupported(firstStatus(statusPackage4AutoMeasure(), statusLegacyAutoMeasure()));
        }
        return false;
    }

    private static boolean resolveEmptyOperation(int index) {
        if (index == 45 || index == 137) {
            return isSupported(firstStatus(statusPackage3FindDevice(), statusLegacyFindDevice()));
        }
        if (index == 132) {
            return isSupported(firstStatus(statusPackage3BloodGlucose(), statusLegacyBloodGlucose()));
        }
        if (index == 142) {
            return isSupported(firstStatus(statusPackage1HealthRemind(), statusLegacyHealthRemind()));
        }
        if (index == 174) {
            return isSupported(firstStatus(statusPackage4AutoMeasure(), statusLegacyAutoMeasure()));
        }
        return false;
    }

    private static boolean isEmptyOperation(int index) {
        return index == 45 || index == 132 || index == 137 || index == 142
                || index == 143 || index == 144 || index == 163 || index == 174;
    }

    private static boolean isHistoricalRead(String operation) {
        return matches(operation, READ_HEALTH_SLEEP, READ_HEALTH_SLEEP_FROM, READ_HEALTH_SLEEP_SINGLEDAY,
                READ_HEALTH_DRINK, READ_HEALTH_ORIGINAL, READ_HEALTH_ORIGINAL_FROM,
                READ_HEALTH_ORIGINAL_SINGLEDAY, READ_HEALTH, ORIGIN_LOG);
    }

    private static boolean matches(String operation, String... candidates) {
        for (String candidate : candidates) {
            if (operation.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSupported(EFunctionStatus status) {
        return status == SUPPORT || status == SUPPORT_OPEN || status == SUPPORT_CLOSE;
    }

    private static boolean isManualSupported(DeviceManualDataType dataType) {
        return manualDetectTypes.contains(DeviceManualDataType.ALL) || manualDetectTypes.contains(dataType);
    }

    private static EFunctionStatus firstStatus(EFunctionStatus first, EFunctionStatus second) {
        return first != null ? first : second;
    }

    private static int getWatchUiServerCount() {
        if (package3 != null) {
            return package3.getWatchUiServerCount();
        }
        return functionSupport != null ? functionSupport.getWatchUiServerCount() : 0;
    }

    private static int getWatchUiCustomCount() {
        if (package3 != null) {
            return package3.getWatchUiCustomCount();
        }
        return functionSupport != null ? functionSupport.getWatchUiCoustomCount() : 0;
    }

    private static EFunctionStatus statusPackage1Heart() { return package1 != null ? package1.getHeartRateDetect() : null; }
    private static EFunctionStatus statusPackage1BloodPressure() { return package1 != null ? package1.getBloodPressure() : null; }
    private static EFunctionStatus statusPackage1BloodPressureAdjust() { return package1 != null ? package1.getAmbulatoryBPAdjustment() : null; }
    private static EFunctionStatus statusPackage1Camera() { return package1 != null ? package1.getCamera() : null; }
    private static EFunctionStatus statusPackage1Alarm() { return package1 != null ? package1.getAlarm() : null; }
    private static EFunctionStatus statusPackage1Sedentary() { return package1 != null ? package1.getSedentaryRemind() : null; }
    private static EFunctionStatus statusPackage1NightTurn() { return package1 != null ? package1.getNightTurnSetting() : null; }
    private static EFunctionStatus statusPackage1HeartWarning() { return package1 != null ? package1.getHeartRateWarning() : null; }
    private static EFunctionStatus statusPackage1Spo2h() { return package1 != null ? package1.getSpo2H() : null; }
    private static EFunctionStatus statusPackage1Fatigue() { return package1 != null ? package1.getFatigue() : null; }
    private static EFunctionStatus statusPackage1Women() { return package1 != null ? package1.getWomen() : null; }
    private static EFunctionStatus statusPackage1NewSportCalc() { return package1 != null ? package1.getNewCalcSport() : null; }
    private static EFunctionStatus statusPackage1ScreenLight() { return package1 != null ? package1.getScreenLight() : null; }
    private static EFunctionStatus statusPackage1TextAlarm() { return package1 != null ? package1.getTextAlarm() : null; }
    private static EFunctionStatus statusPackage1HealthRemind() { return package1 != null ? package1.getHealthRemind() : null; }
    private static EFunctionStatus statusPackage2CountDown() { return package2 != null ? package2.getCountDown() : null; }
    private static EFunctionStatus statusPackage2Weather() { return package2 != null ? package2.getWeatherFunction() : null; }
    private static EFunctionStatus statusPackage2LowPower() { return package2 != null ? package2.getLowPower() : null; }
    private static EFunctionStatus statusPackage2Ecg() { return package2 != null ? package2.getEcgFunction() : null; }
    private static EFunctionStatus statusPackage2SportModel() { return package2 != null ? package2.getSportModelFunction() : null; }
    private static EFunctionStatus statusPackage2Hrv() { return package2 != null ? firstStatus(package2.getHrvAppDetectFunction(), package2.getHrvFunction()) : null; }
    private static EFunctionStatus statusPackage3Temperature() { return package3 != null ? package3.getTemperatureFunction() : null; }
    private static EFunctionStatus statusPackage3FindDevice() { return package3 != null ? package3.getFindDeviceByPhoneFunction() : null; }
    private static EFunctionStatus statusPackage3ScreenStyle() { return package3 != null ? package3.getScreenStyleFunction() : null; }
    private static EFunctionStatus statusPackage3Contact() { return package3 != null ? package3.getContactFunction() : null; }
    private static EFunctionStatus statusPackage3Agps() { return package3 != null ? package3.getAgpsFunction() : null; }
    private static EFunctionStatus statusPackage3BloodGlucose() { return package3 != null ? package3.getBloodGlucose() : null; }
    private static EFunctionStatus statusPackage3BloodGlucoseAdjust() { return package3 != null ? package3.getBloodGlucoseAdjusting() : null; }
    private static EFunctionStatus statusPackage3BloodGlucoseMultiAdjust() { return package3 != null ? package3.getBloodGlucoseMultipleAdjusting() : null; }
    private static EFunctionStatus statusPackage4BodyComponent() { return package4 != null ? package4.getBodyComponent() : null; }
    private static EFunctionStatus statusPackage4BloodComponent() { return package4 != null ? package4.getBloodComponent() : null; }
    private static EFunctionStatus statusPackage4BloodComponentCalibration() { return package4 != null ? package4.getBloodComponentSingleCalibration() : null; }
    private static EFunctionStatus statusPackage4WorldClock() { return package4 != null ? package4.getWorldClock() : null; }
    private static EFunctionStatus statusPackage4MiniCheckup() { return package4 != null ? package4.getMiniCheckup() : null; }
    private static EFunctionStatus statusPackage4AutoMeasure() { return package4 != null ? package4.getAutoMeasure() : null; }
    private static EFunctionStatus statusPackage5TextImagePush() { return package5 != null ? package5.getTextImagePush() : null; }
    private static EFunctionStatus statusPackage5Gsr() { return package5 != null ? package5.getGSR() : null; }
    private static EFunctionStatus statusPackage5SafetyProtection() { return package5 != null ? package5.getSafetyProtection() : null; }
    private static EFunctionStatus statusLegacyHeart() { return functionSupport != null ? functionSupport.getHeartDetect() : null; }
    private static EFunctionStatus statusLegacyTemperature() { return functionSupport != null ? functionSupport.getTemperatureFunction() : null; }
    private static EFunctionStatus statusLegacyBloodPressure() { return functionSupport != null ? functionSupport.getBp() : null; }
    private static EFunctionStatus statusLegacyBloodPressureAdjust() { return functionSupport != null ? functionSupport.getAngioAdjuster() : null; }
    private static EFunctionStatus statusLegacyCamera() { return functionSupport != null ? functionSupport.getCamera() : null; }
    private static EFunctionStatus statusLegacyAlarm() { return functionSupport != null ? functionSupport.getAlarm2() : null; }
    private static EFunctionStatus statusLegacySedentary() { return functionSupport != null ? functionSupport.getLongseat() : null; }
    private static EFunctionStatus statusLegacyNightTurn() { return functionSupport != null ? functionSupport.getNightTurnSetting() : null; }
    private static EFunctionStatus statusLegacyFindDevice() { return functionSupport != null ? functionSupport.getFindDeviceByPhone() : null; }
    private static EFunctionStatus statusLegacyHeartWarning() { return functionSupport != null ? functionSupport.getHeartWaring() : null; }
    private static EFunctionStatus statusLegacySpo2h() { return functionSupport != null ? functionSupport.getSpo2H() : null; }
    private static EFunctionStatus statusLegacyFatigue() { return functionSupport != null ? functionSupport.getFatigue() : null; }
    private static EFunctionStatus statusLegacyWomen() { return functionSupport != null ? functionSupport.getWomen() : null; }
    private static EFunctionStatus statusLegacyCountDown() { return functionSupport != null ? functionSupport.getCountDown() : null; }
    private static EFunctionStatus statusLegacyNewSportCalc() { return functionSupport != null ? functionSupport.getNewCalcSport() : null; }
    private static EFunctionStatus statusLegacyScreenLight() { return functionSupport != null ? functionSupport.getScreenLight() : null; }
    private static EFunctionStatus statusLegacyScreenStyle() { return functionSupport != null ? functionSupport.getScreenStyleFunction() : null; }
    private static EFunctionStatus statusLegacyWeather() { return functionSupport != null ? functionSupport.getWeatherFunction() : null; }
    private static EFunctionStatus statusLegacyLowPower() { return functionSupport != null ? functionSupport.getLowPower() : null; }
    private static EFunctionStatus statusLegacyEcg() { return functionSupport != null ? functionSupport.getEcg() : null; }
    private static EFunctionStatus statusLegacySportModel() { return functionSupport != null ? firstStatus(functionSupport.getSportModel(), functionSupport.getMultSportModel()) : null; }
    private static EFunctionStatus statusLegacyHrv() { return functionSupport != null ? firstStatus(functionSupport.getHrvAppDetectFunction(), functionSupport.getHrvFunction()) : null; }
    private static EFunctionStatus statusLegacyTextAlarm() { return functionSupport != null ? functionSupport.getTextAlarm() : null; }
    private static EFunctionStatus statusLegacyContact() { return functionSupport != null ? functionSupport.getContactFunction() : null; }
    private static EFunctionStatus statusLegacyAgps() { return functionSupport != null ? functionSupport.getAgps() : null; }
    private static EFunctionStatus statusLegacyBloodGlucose() { return functionSupport != null ? functionSupport.getBloodGlucose() : null; }
    private static EFunctionStatus statusLegacyBloodGlucoseAdjust() { return functionSupport != null ? functionSupport.getBloodGlucoseAdjusting() : null; }
    private static EFunctionStatus statusLegacyBloodGlucoseMultiAdjust() { return functionSupport != null ? functionSupport.getBloodGlucoseMultipleAdjusting() : null; }
    private static EFunctionStatus statusLegacyBodyComponent() { return functionSupport != null ? functionSupport.getBodyComponent() : null; }
    private static EFunctionStatus statusLegacyBloodComponent() { return functionSupport != null ? functionSupport.getBloodComponent() : null; }
    private static EFunctionStatus statusLegacyBloodComponentCalibration() { return functionSupport != null ? functionSupport.getBloodComponentSingleCalibration() : null; }
    private static EFunctionStatus statusLegacyWorldClock() { return functionSupport != null ? functionSupport.getWorldClock() : null; }
    private static EFunctionStatus statusLegacyHealthRemind() { return functionSupport != null ? functionSupport.getHealthRemind() : null; }
    private static EFunctionStatus statusLegacyAutoMeasure() { return functionSupport != null ? functionSupport.getAutoMeasure() : null; }
    private static EFunctionStatus statusLegacyTextImagePush() { return functionSupport != null ? functionSupport.getTextImagePush() : null; }
    private static EFunctionStatus statusLegacyGsr() { return functionSupport != null ? functionSupport.getGSR() : null; }
    private static EFunctionStatus statusLegacySafetyProtection() { return functionSupport != null ? functionSupport.getSafetyProtection() : null; }
    private static EFunctionStatus statusLegacyMiniCheckup() { return functionSupport != null ? functionSupport.getMiniCheckup() : null; }
    private static EFunctionStatus statusLegacy4g() { return functionSupport != null ? functionSupport.getServer4g() : null; }
}
