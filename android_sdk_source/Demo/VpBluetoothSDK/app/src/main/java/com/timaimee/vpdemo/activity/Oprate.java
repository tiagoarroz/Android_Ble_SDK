package com.timaimee.vpdemo.activity;

/**
 * Created by Administrator on 2017/11/25.
 */

public interface Oprate {
    String PWD_COMFIRM = "1dispositivopalavra-passe-";
    String NEED_COMFIRM = "palavra-passe";
    String UNNEED_COMFIRM = "palavra-passe";
    String PWD_COMFIRM_2_DISCONNECT = "BT";
    String PWD_COMFIRM_2_DISCONNECT_ = "BLE";
    String PERSONINFO_SYNC = "2-Configurar";
    String SETTING_FIRST = "<--12";
    String PWD_MODIFY = "dispositivopalavra-passe-";
    String TEMPTURE_DETECT_START = "temperatura-Iniciar";
    String TEMPTURE_DETECT_STOP = "temperatura-Terminar";
    String HEART_DETECT_START = "Frequência cardíaca-Iniciar";
    String HEART_DETECT_STOP = "Frequência cardíaca-Terminar";
    String BP_DETECT_START = "pressão arterial-Iniciar";
    String BP_DETECT_STOP = "pressão arterial-Terminar";
    String BP_DETECTMODEL_SETTING = "pressão arterial-Configurar";
    String BP_DETECTMODEL_SETTING_ADJUSTE = "pressão arterial[]-Configurar";
    String BP_DETECTMODEL_SETTING_ADJUSTE_CANCEL = "pressão arterial[]-Cancelar";
    String BP_DETECTMODEL_READ = "pressão arterial-Ler";
    String SPORT_CURRENT_READ = "-Ler";
    String CAMERA_START = "-Iniciar";
    String CAMERA_STOP = "-";
    String ALARM_SETTING = "alarme-Configurar";
    String ALARM_READ = "alarme-Ler";
    String ALARM_NEW_READ = "alarme-Ler";
    String ALARM_NEW_ADD = "alarme-Adicionar";
    String ALARM_NEW_MODIFY = "alarme-";
    String ALARM_NEW_DELETE = "alarme-Eliminar";
    String ALARM_NEW_LISTENER = "alarme-estado";
    String ALARM_NEW_ = "alarme";
    String LONGSEAT_SETTING_OPEN = "-Ativar";
    String LONGSEAT_SETTING_CLOSE = "-Desativar";
    String LONGSEAT_READ = "-Ler";
    String LANGUAGE_CHINESE = "Configurar-";
    String LANGUAGE_ENGLISH = "Configurar-";
    String BATTERY = "estado-Ler";
    String NIGHT_TURN_WRIST_OPEN = "-Ativar";
    String NIGHT_TURN_WRIST_CLOSE = "-Desativar";
    String NIGHT_TURN_WRIST_READ = "-Ler";
    String NIGHT_TURN_WRIST_CUSTOM_TIME = "-";
    String NIGHT_TURN_WRIST_CUSTOM_TIME_LEVEL = "-";
    String FINDPHONE = "";
    String CHECK_WEAR_SETING_OPEN = "-Ativar";
    String CHECK_WEAR_SETING_CLOSE = "-Desativar";
    String FINDDEVICE_SETTING_OPEN = "dispositivo-Ativar";
    String FINDDEVICE_SETTING_CLOSE = "dispositivo-Desativar";
    String FINDDEVICE_READ = "dispositivo-Ler";
    String DEVICE_COUSTOM_READ = "-Ler";
    String DEVICE_COUSTOM_SETTING = "-Configurar";
    String DEVICE_ECG_ALWAYS_OPEN = "ECG-";
    String DEVICE_ECG_ALWAYS_CLOSE = "ECG-";
    String SOCIAL_MSG_SETTING = "1-Configurar";
    String SOCIAL_MSG_SETTING2 = "2-Configurar";
    String SOCIAL_MSG_READ = "-LerConfigurar";
    String SOCIAL_MSG_SEND = "-Enviar";
    String SOCIAL_PHONE_IDLE_OR_OFFHOOK = "-";
    String DEVICE_CONTROL_PHONE = "-，";
    String HEARTWRING_READ = "Frequência cardíaca-Ler";
    String HEARTWRING_OPEN = "Frequência cardíaca-Ativar";
    String HEARTWRING_CLOSE = "Frequência cardíaca-Desativar";
    String SPO2H_OPEN = "SpO2-Ler";
    String SPO2H_CLOSE = "SpO2-Terminar";
    String SPO2H_AUTO_DETECT_READ = "SpO2-Ler";
    String SPO2H_AUTO_DETECT_OPEN = "SpO2-Ativar";
    String SPO2H_AUTO_DETECT_CLOSE = "SpO2-Desativar";
    String FATIGUE_OPEN = "fadiga-Ler";
    String FATIGUE_CLOSE = "fadiga-Terminar";
    String WOMEN_SETTING = "Femininoestado-Configurar";
    String WOMEN_READ = "Femininoestado-Ler";
    String COUNT_DOWN_WATCH_CLOSE_UI = "contagem decrescente-(Desativar)";
    String COUNT_DOWN_WATCH_OPEN_UI = "contagem decrescente-(Ativar)";
    String COUNT_DOWN_APP = "contagem decrescente-App";
    String COUNT_DOWN_APP_READ = "contagem decrescente-Ler";
    String GPS_KAABA = "GPS&";
    String GPS_REPORT_START = "GPS";
    String READ_CHANTING = "Ler";
    String SCREEN_LIGHT_SETTING = "-Configurar";
    String SCREEN_LIGHT_READ = "-Ler";
    String SCREEN_STYLE_READ = "-Ler";
    String SCREEN_STYLE_SETTING = "-Configurar";
    String AIM_SPROT_CALC = "-";
    String INSTITUTION_TRANSLATION = "Conversão métrico/imperial";
    String READ_TEMPTURE_DATA = "Lertemperaturadados";
    String READ_HEALTH_DRINK = "Lerdados-";
    String READ_HEALTH_SLEEP = "Lerdados-";
    String READ_HEALTH_SLEEP_FROM = "Lerdados--";
    String READ_HEALTH_SLEEP_SINGLEDAY = "Lerdados--";
    String READ_HEALTH_ORIGINAL = "Lerdados-5";
    String READ_HEALTH_ORIGINAL_FROM = "Lerdados-";
    String READ_HEALTH_ORIGINAL_SINGLEDAY = "Lerdados-";
    String READ_HEALTH = "Lerdados-";
    String OAD = "Atualização de firmware";
    String SHOW_SP = "sp";
    String SPORT_MODE_ORIGIN_READ = "Lerdados-";
    String SPORT_MODE_ORIGIN_READSTAUTS = "Ler estado-";
    String SPORT_MODE_ORIGIN_START = "Ativar-";
    String SPORT_MODE_START_INDOOR = "Ativar-";
    String SPORT_MODE_ORIGIN_END = "Terminar-";
    String SPO2H_ORIGIN_READ = "Lerdados-SpO2dados";
    String HRV_ORIGIN_READ = "Lerdados-HRVdados";
    String HRV_START_DETECT = "IniciarHRV";
    String HRV_STOP_DETECT = "HRV";
    String CLEAR_DEVICE_DATA = "dados";
    String DISCONNECT = "-";
    String DETECT_PTT = "PTT";
    String DETECT_START_ECG = "IniciarECG";
    String DETECT_STOP_ECG = "TerminarECG";
    String LOW_POWER_READ = "-Ler";
    String LOW_POWER_OPEN = "-";
    String LOW_POWER_CLOSE = "-";
    String S22_READ_DATA = "S22-dadosLer";
    String S22_READ_STATE = "S22-estadoLer";
    String S22_SETTING_STATE_OPEN = "S22-estadoConfigurar()";
    String S22_SETTING_STATE_CLOSE = "S22-estadoConfigurar()";
    String BP_FUNCTION_READ = "pressão arterialestado(Ler)";
    String BP_FUNCTION_SETTING = "pressão arterialestado(Configurar)";
    String WEATHER_READ_STATUEINFO = "meteorologiaestado(Ler)";
    String SET_WATCH_TIME = "Configurar";
    String WEATHER_SETTING_STATUEINFO_ON = "meteorologiaestado()";
    String WEATHER_SETTING_STATUEINFO_OFF = "meteorologiaestado()";
    String WEATHER_SETTING_DATA = "meteorologiadados(Configurar)";

    String LIANSUO_SOS = "-SOS";
    String LIANSUO_SEND_ORDER = "-Enviar";
    String LIANSUO_SEND_CONTENT = "-Enviar";
    String UI_UPDATE_AGPS = "Atualização UI - AGPS";
    String UI_UPDATE_CUSTOM = "Atualização UI - mostrador personalizado";
    String UI_UPDATE_SERVER = "Atualização UI - mostrador do servidor";
    String SYNC_MUSIC_INFO_PLAY = "música-";
    String SYNC_MUSIC_INFO_PAUSE = "música-";
    String VOLUME = "volume";
    String UI_UPDATE_G15IMG = "Atualização UI - transferência de imagem G15";
    String TEXT_ALARM_ADD = "alarmeAdicionar";
    String TEXT_ALARM_MODIFY = "alarme";
    String TEXT_ALARM_READ = "alarmeLer";
    String TEXT_ALARM_DELETE = "alarmeEliminar";
    String TEXT_ALARM = "alarme";
    String ORIGIN_LOG = "Dados brutosregisto";
    String RR = "RR";
    String G15_QR_CODE = "G15";
    String ECG_AUTO_REPORT_TEXT = "ECGdados";
    String START_BLOOD_GLUCOSE = "Iniciar";
    String STOP_BLOOD_GLUCOSE = "";
    String BLOOD_GLUCOSE_P_READ = "Ler";
    String BLOOD_GLUCOSE_P_SETTING = "Configurar";

    String BLOOD_GLUCOSE_MULTIPLE_READ = "Ler";
    String BLOOD_GLUCOSE_MULTIPLE_SETTING = "Configurar";
    String FIND_DEVICE = "";
    String BLE_RENAME = "4.0";
    String BT_RENAME = "3.0";
    String BT_CONNECT = "BT";
    String BT_CLOSE = "DesativarBT";
    String BLE_DISCONNECT = "BLE";
    String BT_READ = "LerBT";
    String HEALTH_REMIND = "";
    String JL_NOTIFY_OPEN = "Ativar";
    String JL_AUTH = "";
    String JL_INIT_FILE_SYS = "";
    String JL_SET_PHOTO_DIAL = "Configurar";

    String JL_DEVICE = "";

    String JL_DEVICE_OTA = "OTA";

    String CONTACT = "Contactos";
    String GATT_CLOSE = "Gatt-Close";
    String FUNCTION_SWITCH = "";
    String READ_ECG_ID = "LerECG ID";
    String READ_ECG_DATA = "LerECG dados";
    String SET_ECG_NEW_DATA_REPORT = "ecgdados";

    String DETECT_START_BODY_COMPONENT = "Iniciar";
    String DETECT_STOP_BODY_COMPONENT = "Terminar";
    String READ_BODY_COMPONENT_ID = "LerID";
    String READ_BODY_COMPONENT_DATA = "Lerdados";

    String SET_BODY_COMPONENT_NEW_DATA_REPORT = "dados";
    String SHARE_LOG = "partilharregisto";
    String READ_BLOOD_COMPOSITION_CALIBRATION = "Ler";
    String SETTING_BLOOD_COMPOSITION_CALIBRATION = "Configurar";

    String DETECT_START_BLOOD_COMPONENT = "Iniciar";
    String DETECT_STOP_BLOOD_COMPONENT = "Terminar";
    String DETECT_MULTI_ECG_DETECT = "ECG";
    String WORLD_CLOCK = "Relógio mundial";
    String G08W_HEALTH_ALARM_INTERVAL = "G08W-";
    String G08W_PPG_DATA_CALLBACK = "G08W-PPGdados";
    String MAGNETIC_OPEN = "";
    String TEXT_IMAGE_MSG_PUSH = "Envio de texto e imagem";
    String JH58_PPG = "JH58PPG";
    String MINI_CHECKUP = "Mini check-up";

    String GSR_START = "Iniciar";
    String GSR_STOP = "Terminar";
    String ZT163_DEVICE_ALWAYS_OFF_SCREEN = "ZT163: ecrã sempre apagado";

    String GNSS_SOS_SAFETY_PROTECTION = "GNSS&&SOS";
    String FUN_SWITCH_READ = "-Ler";
    String FUN_SWITCH_SETTING = "-Configurar";
    String FUN_4G = "4Gfuncionalidade";
    String AUTO_MEASURE = "";
    String NRF_OTA = "NRF OTA";
    String QIU_GUO_TCM = "dados TCM";
    String QX17_DATA_ACQUISITION = "QX17dados";

    String NONE = "NONE";
    String[] oprateStr = new String[]{
            /*PWD_COMFIRM,NEED_COMFIRM,UNNEED_COMFIRM,*/ PWD_COMFIRM_2_DISCONNECT, PWD_COMFIRM_2_DISCONNECT_, PERSONINFO_SYNC, SETTING_FIRST, DISCONNECT, BT_CONNECT, BLE_DISCONNECT, PWD_MODIFY,
            SHARE_LOG, GPS_KAABA, GPS_REPORT_START, READ_CHANTING, HEART_DETECT_START, HEART_DETECT_STOP, TEMPTURE_DETECT_START, TEMPTURE_DETECT_STOP, READ_TEMPTURE_DATA, BP_DETECT_START, BP_DETECT_STOP, BP_DETECTMODEL_SETTING, BP_DETECTMODEL_READ,
            BP_DETECTMODEL_SETTING_ADJUSTE_CANCEL, BP_DETECTMODEL_SETTING_ADJUSTE,
            SPORT_CURRENT_READ, CAMERA_START, CAMERA_STOP, ALARM_SETTING, ALARM_READ, /*ALARM_NEW_READ, ALARM_NEW_ADD, ALARM_NEW_MODIFY, ALARM_NEW_DELETE,*/ ALARM_NEW_LISTENER, ALARM_NEW_,
            LONGSEAT_SETTING_OPEN, LONGSEAT_SETTING_CLOSE, LONGSEAT_READ, LANGUAGE_CHINESE, LANGUAGE_ENGLISH,
            BATTERY, NIGHT_TURN_WRIST_OPEN, NIGHT_TURN_WRIST_CLOSE, NIGHT_TURN_WRIST_READ, NIGHT_TURN_WRIST_CUSTOM_TIME, NIGHT_TURN_WRIST_CUSTOM_TIME_LEVEL,
            DEVICE_COUSTOM_READ, DEVICE_COUSTOM_SETTING, DEVICE_ECG_ALWAYS_OPEN, DEVICE_ECG_ALWAYS_CLOSE, FINDPHONE,
            CHECK_WEAR_SETING_OPEN, CHECK_WEAR_SETING_CLOSE,
            FINDDEVICE_SETTING_OPEN, FINDDEVICE_SETTING_CLOSE, FINDDEVICE_READ,
            SOCIAL_MSG_SETTING, SOCIAL_MSG_SETTING2, SOCIAL_MSG_READ, SOCIAL_MSG_SEND, DEVICE_CONTROL_PHONE, SOCIAL_PHONE_IDLE_OR_OFFHOOK, HEARTWRING_READ, HEARTWRING_OPEN, HEARTWRING_CLOSE,
            SPO2H_OPEN, SPO2H_CLOSE, SPO2H_AUTO_DETECT_READ, SPO2H_AUTO_DETECT_OPEN, SPO2H_AUTO_DETECT_CLOSE, FATIGUE_OPEN, FATIGUE_CLOSE, WOMEN_SETTING, WOMEN_READ, COUNT_DOWN_WATCH_CLOSE_UI, COUNT_DOWN_WATCH_OPEN_UI, COUNT_DOWN_APP_READ, SCREEN_LIGHT_SETTING, SCREEN_LIGHT_READ, SCREEN_STYLE_READ, SCREEN_STYLE_SETTING, AIM_SPROT_CALC, INSTITUTION_TRANSLATION,
            READ_HEALTH_SLEEP, READ_HEALTH_SLEEP_FROM, READ_HEALTH_SLEEP_SINGLEDAY, READ_HEALTH_DRINK, READ_HEALTH_ORIGINAL,
            READ_HEALTH_ORIGINAL_FROM, READ_HEALTH_ORIGINAL_SINGLEDAY, READ_HEALTH, SET_WATCH_TIME,
            OAD, SHOW_SP, SPORT_MODE_ORIGIN_READ, SPORT_MODE_ORIGIN_READSTAUTS, SPORT_MODE_START_INDOOR, SPORT_MODE_ORIGIN_START, SPORT_MODE_ORIGIN_END, SPO2H_ORIGIN_READ, HRV_ORIGIN_READ, HRV_START_DETECT, HRV_STOP_DETECT, CLEAR_DEVICE_DATA
            , DETECT_START_ECG, DETECT_STOP_ECG, NONE, LOW_POWER_READ, LOW_POWER_OPEN, LOW_POWER_CLOSE, S22_READ_DATA, S22_READ_STATE, S22_SETTING_STATE_OPEN, S22_SETTING_STATE_CLOSE, DETECT_PTT, BP_FUNCTION_READ, BP_FUNCTION_SETTING
            , WEATHER_READ_STATUEINFO, WEATHER_SETTING_STATUEINFO_ON, WEATHER_SETTING_STATUEINFO_OFF, WEATHER_SETTING_DATA, LIANSUO_SOS, LIANSUO_SEND_ORDER, LIANSUO_SEND_CONTENT, UI_UPDATE_AGPS, UI_UPDATE_CUSTOM, UI_UPDATE_SERVER
            , UI_UPDATE_G15IMG, SYNC_MUSIC_INFO_PLAY, SYNC_MUSIC_INFO_PAUSE, VOLUME,/*TEXT_ALARM_READ,TEXT_ALARM_ADD,TEXT_ALARM_MODIFY,TEXT_ALARM_DELETE,*/TEXT_ALARM, ORIGIN_LOG, RR, G15_QR_CODE, ECG_AUTO_REPORT_TEXT
            , START_BLOOD_GLUCOSE, STOP_BLOOD_GLUCOSE, BLOOD_GLUCOSE_P_READ, BLOOD_GLUCOSE_P_SETTING, BLOOD_GLUCOSE_MULTIPLE_READ, BLOOD_GLUCOSE_MULTIPLE_SETTING, FIND_DEVICE, BLE_RENAME, BT_RENAME, BT_READ, BLE_DISCONNECT, HEALTH_REMIND, FUNCTION_SWITCH,
            /*JL_NOTIFY_OPEN, JL_AUTH, JL_INIT_FILE_SYS, JL_SET_PHOTO_DIAL, JL_DEVICE_OTA, */JL_DEVICE, CONTACT, GATT_CLOSE, READ_ECG_ID, READ_ECG_DATA, SET_ECG_NEW_DATA_REPORT, DETECT_START_BODY_COMPONENT, DETECT_STOP_BODY_COMPONENT, READ_BODY_COMPONENT_ID, READ_BODY_COMPONENT_DATA, SET_BODY_COMPONENT_NEW_DATA_REPORT,
            READ_BLOOD_COMPOSITION_CALIBRATION, SETTING_BLOOD_COMPOSITION_CALIBRATION, DETECT_START_BLOOD_COMPONENT, DETECT_STOP_BLOOD_COMPONENT, DETECT_MULTI_ECG_DETECT, WORLD_CLOCK, G08W_HEALTH_ALARM_INTERVAL, G08W_PPG_DATA_CALLBACK, MAGNETIC_OPEN,
            TEXT_IMAGE_MSG_PUSH, JH58_PPG, MINI_CHECKUP,GSR_START,GSR_STOP,ZT163_DEVICE_ALWAYS_OFF_SCREEN, GNSS_SOS_SAFETY_PROTECTION, FUN_SWITCH_READ, FUN_SWITCH_SETTING, FUN_4G, AUTO_MEASURE, NRF_OTA, QIU_GUO_TCM, QX17_DATA_ACQUISITION
    };
}