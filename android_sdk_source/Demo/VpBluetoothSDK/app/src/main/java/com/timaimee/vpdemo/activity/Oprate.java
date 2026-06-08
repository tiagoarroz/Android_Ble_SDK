package com.timaimee.vpdemo.activity;

/**
 * Created by Administrator on 2017/11/25.
 */

public interface Oprate {
    String PWD_COMFIRM = "Password do dispositivo - Validar";
    String NEED_COMFIRM = "Confirmação no dispositivo - Obrigatória";
    String UNNEED_COMFIRM = "Confirmação no dispositivo - Ignorar";
    String PWD_COMFIRM_2_DISCONNECT = "BT clássico - Desligar imediatamente";
    String PWD_COMFIRM_2_DISCONNECT_ = "BLE - Desligar imediatamente";
    String PERSONINFO_SYNC = "Perfil pessoal - Sincronizar";
    String SETTING_FIRST = "Pré-requisito - Executar ligação e password";
    String PWD_MODIFY = "Password do dispositivo - Alterar";
    String TEMPTURE_DETECT_START = "Temperatura - Iniciar medição";
    String TEMPTURE_DETECT_STOP = "Temperatura - Parar medição";
    String HEART_DETECT_START = "Frequência cardíaca - Iniciar medição";
    String HEART_DETECT_STOP = "Frequência cardíaca - Parar medição";
    String BP_DETECT_START = "Pressão arterial - Iniciar medição";
    String BP_DETECT_STOP = "Pressão arterial - Parar medição";
    String BP_DETECTMODEL_SETTING = "Pressão arterial - Configurar modo privado";
    String BP_DETECTMODEL_SETTING_ADJUSTE = "Pressão arterial - Ativar ajuste dinâmico";
    String BP_DETECTMODEL_SETTING_ADJUSTE_CANCEL = "Pressão arterial - Cancelar ajuste dinâmico";
    String BP_DETECTMODEL_READ = "Pressão arterial - Ler configuração";
    String SPORT_CURRENT_READ = "Passos atuais - Ler";
    String CAMERA_START = "Câmara remota - Iniciar";
    String CAMERA_STOP = "Câmara remota - Parar";
    String ALARM_SETTING = "Alarme simples - Configurar";
    String ALARM_READ = "Alarme simples - Ler";
    String ALARM_NEW_READ = "Alarmes avançados - Ler";
    String ALARM_NEW_ADD = "Alarmes avançados - Adicionar";
    String ALARM_NEW_MODIFY = "Alarmes avançados - Alterar";
    String ALARM_NEW_DELETE = "Alarmes avançados - Eliminar";
    String ALARM_NEW_LISTENER = "Alarmes avançados - Monitorizar alterações";
    String ALARM_NEW_ = "Alarmes avançados - Abrir ecrã";
    String LONGSEAT_SETTING_OPEN = "Sedentarismo - Ativar lembrete";
    String LONGSEAT_SETTING_CLOSE = "Sedentarismo - Desativar lembrete";
    String LONGSEAT_READ = "Sedentarismo - Ler configuração";
    String LANGUAGE_CHINESE = "Idioma do dispositivo - Chinês";
    String LANGUAGE_ENGLISH = "Idioma do dispositivo - Inglês";
    String BATTERY = "Bateria - Ler estado";
    String NIGHT_TURN_WRIST_OPEN = "Levantar pulso à noite - Ativar";
    String NIGHT_TURN_WRIST_CLOSE = "Levantar pulso à noite - Desativar";
    String NIGHT_TURN_WRIST_READ = "Levantar pulso à noite - Ler configuração";
    String NIGHT_TURN_WRIST_CUSTOM_TIME = "Levantar pulso à noite - Definir horário";
    String NIGHT_TURN_WRIST_CUSTOM_TIME_LEVEL = "Levantar pulso à noite - Definir horário e brilho";
    String FINDPHONE = "Encontrar telefone - Ativar alarme";
    String CHECK_WEAR_SETING_OPEN = "Deteção de uso - Ativar";
    String CHECK_WEAR_SETING_CLOSE = "Deteção de uso - Desativar";
    String FINDDEVICE_SETTING_OPEN = "Encontrar dispositivo - Ativar";
    String FINDDEVICE_SETTING_CLOSE = "Encontrar dispositivo - Desativar";
    String FINDDEVICE_READ = "Encontrar dispositivo - Ler configuração";
    String DEVICE_COUSTOM_READ = "Personalização do dispositivo - Ler";
    String DEVICE_COUSTOM_SETTING = "Personalização do dispositivo - Configurar";
    String DEVICE_ECG_ALWAYS_OPEN = "ECG contínuo - Ativar";
    String DEVICE_ECG_ALWAYS_CLOSE = "ECG contínuo - Desativar";
    String SOCIAL_MSG_SETTING = "Notificações sociais - Configurar pacote 1";
    String SOCIAL_MSG_SETTING2 = "Notificações sociais - Configurar pacote 2";
    String SOCIAL_MSG_READ = "Notificações sociais - Ler configuração";
    String SOCIAL_MSG_SEND = "Notificações sociais - Enviar teste";
    String SOCIAL_PHONE_IDLE_OR_OFFHOOK = "Chamadas - Simular atendida/ocupada";
    String DEVICE_CONTROL_PHONE = "Telefone - Monitorizar comandos do relógio";
    String HEARTWRING_READ = "Alerta de frequência cardíaca - Ler";
    String HEARTWRING_OPEN = "Alerta de frequência cardíaca - Ativar";
    String HEARTWRING_CLOSE = "Alerta de frequência cardíaca - Desativar";
    String SPO2H_OPEN = "SpO2 - Iniciar medição";
    String SPO2H_CLOSE = "SpO2 - Parar medição";
    String SPO2H_AUTO_DETECT_READ = "SpO2 automático - Ler configuração";
    String SPO2H_AUTO_DETECT_OPEN = "SpO2 automático - Ativar";
    String SPO2H_AUTO_DETECT_CLOSE = "SpO2 automático - Desativar";
    String FATIGUE_OPEN = "Fadiga - Iniciar medição";
    String FATIGUE_CLOSE = "Fadiga - Parar medição";
    String WOMEN_SETTING = "Saúde feminina - Configurar";
    String WOMEN_READ = "Saúde feminina - Ler";
    String COUNT_DOWN_WATCH_CLOSE_UI = "Temporizador no relógio - Fechar ecrã";
    String COUNT_DOWN_WATCH_OPEN_UI = "Temporizador no relógio - Abrir ecrã";
    String COUNT_DOWN_APP = "Temporizador pela app - Configurar";
    String COUNT_DOWN_APP_READ = "Temporizador pela app - Ler";
    String GPS_KAABA = "GPS - Fuso horário e Kaaba";
    String GPS_REPORT_START = "GPS - Enviar localização";
    String READ_CHANTING = "Recitação - Ler dados";
    String SCREEN_LIGHT_SETTING = "Ecrã - Configurar brilho";
    String SCREEN_LIGHT_READ = "Ecrã - Ler brilho";
    String SCREEN_STYLE_READ = "Ecrã - Ler estilo";
    String SCREEN_STYLE_SETTING = "Ecrã - Configurar estilo";
    String AIM_SPROT_CALC = "Objetivo diário - Calcular passos";
    String INSTITUTION_TRANSLATION = "Conversão métrico/imperial";
    String READ_TEMPTURE_DATA = "Temperatura - Ler histórico";
    String READ_HEALTH_DRINK = "Saúde - Ler dados de ingestão de álcool";
    String READ_HEALTH_SLEEP = "Sono - Ler histórico";
    String READ_HEALTH_SLEEP_FROM = "Sono - Ler desde dia específico";
    String READ_HEALTH_SLEEP_SINGLEDAY = "Sono - Ler dia específico";
    String READ_HEALTH_ORIGINAL = "Dados originais - Ler a cada 5 minutos";
    String READ_HEALTH_ORIGINAL_FROM = "Dados originais - Ler desde dia específico";
    String READ_HEALTH_ORIGINAL_SINGLEDAY = "Dados originais - Ler dia específico";
    String READ_HEALTH = "Saúde - Ler todos os dados";
    String OAD = "Atualização de firmware";
    String SHOW_SP = "Preferências locais - Mostrar";
    String SPORT_MODE_ORIGIN_READ = "Modo desportivo - Ler dados";
    String SPORT_MODE_ORIGIN_READSTAUTS = "Modo desportivo - Ler estado";
    String SPORT_MODE_ORIGIN_START = "Modo desportivo - Iniciar";
    String SPORT_MODE_START_INDOOR = "Modo desportivo - Iniciar caminhada interior";
    String SPORT_MODE_ORIGIN_END = "Modo desportivo - Terminar";
    String SPO2H_ORIGIN_READ = "SpO2 - Ler histórico bruto";
    String HRV_ORIGIN_READ = "HRV - Ler histórico";
    String HRV_START_DETECT = "HRV - Iniciar medição";
    String HRV_STOP_DETECT = "HRV - Parar medição";
    String CLEAR_DEVICE_DATA = "Dispositivo - Limpar dados";
    String DISCONNECT = "Bluetooth - Desligar";
    String DETECT_PTT = "PTT";
    String DETECT_START_ECG = "ECG - Iniciar medição";
    String DETECT_STOP_ECG = "ECG - Parar medição";
    String LOW_POWER_READ = "Baixo consumo - Ler estado";
    String LOW_POWER_OPEN = "Baixo consumo - Ativar";
    String LOW_POWER_CLOSE = "Baixo consumo - Desativar";
    String S22_READ_DATA = "S22 - Ler dados automáticos";
    String S22_READ_STATE = "S22 - Ler estado automático";
    String S22_SETTING_STATE_OPEN = "S22 - Ativar estado automático";
    String S22_SETTING_STATE_CLOSE = "S22 - Desativar estado automático";
    String BP_FUNCTION_READ = "Pressão arterial - Ler estado da função";
    String BP_FUNCTION_SETTING = "Pressão arterial - Configurar estado da função";
    String WEATHER_READ_STATUEINFO = "Meteorologia - Ler estado";
    String SET_WATCH_TIME = "Relógio - Sincronizar hora";
    String WEATHER_SETTING_STATUEINFO_ON = "Meteorologia - Ativar estado";
    String WEATHER_SETTING_STATUEINFO_OFF = "Meteorologia - Desativar estado";
    String WEATHER_SETTING_DATA = "Meteorologia - Enviar dados";

    String LIANSUO_SOS = "Liansuo - Monitorizar SOS";
    String LIANSUO_SEND_ORDER = "Liansuo - Enviar comando";
    String LIANSUO_SEND_CONTENT = "Liansuo - Enviar conteúdo";
    String UI_UPDATE_AGPS = "Atualização UI - AGPS";
    String UI_UPDATE_CUSTOM = "Atualização UI - mostrador personalizado";
    String UI_UPDATE_SERVER = "Atualização UI - mostrador do servidor";
    String SYNC_MUSIC_INFO_PLAY = "Música - Enviar reprodução";
    String SYNC_MUSIC_INFO_PAUSE = "Música - Enviar pausa";
    String VOLUME = "Música - Ajustar volume";
    String UI_UPDATE_G15IMG = "Atualização UI - transferência de imagem G15";
    String TEXT_ALARM_ADD = "Alarme de texto - Adicionar";
    String TEXT_ALARM_MODIFY = "Alarme de texto - Alterar";
    String TEXT_ALARM_READ = "Alarme de texto - Ler";
    String TEXT_ALARM_DELETE = "Alarme de texto - Eliminar";
    String TEXT_ALARM = "Alarme de texto - Abrir ecrã";
    String ORIGIN_LOG = "Dados brutos - Abrir registo";
    String RR = "Intervalos RR - Ler";
    String G15_QR_CODE = "G15 - Enviar QR code";
    String ECG_AUTO_REPORT_TEXT = "ECG contínuo - Monitorizar dados";
    String START_BLOOD_GLUCOSE = "Glicose - Iniciar medição";
    String STOP_BLOOD_GLUCOSE = "Glicose - Parar medição";
    String BLOOD_GLUCOSE_P_READ = "Glicose - Ler modo privado";
    String BLOOD_GLUCOSE_P_SETTING = "Glicose - Configurar modo privado";

    String BLOOD_GLUCOSE_MULTIPLE_READ = "Glicose - Ler múltiplas calibrações";
    String BLOOD_GLUCOSE_MULTIPLE_SETTING = "Glicose - Configurar múltiplas calibrações";
    String FIND_DEVICE = "Encontrar relógio - Tocar";
    String BLE_RENAME = "BLE - Renomear dispositivo";
    String BT_RENAME = "BT clássico - Renomear dispositivo";
    String BT_CONNECT = "BT clássico - Ligar";
    String BT_CLOSE = "BT clássico - Desligar";
    String BLE_DISCONNECT = "BLE - Desligar";
    String BT_READ = "BT clássico - Ler informação";
    String HEALTH_REMIND = "Lembretes de saúde - Abrir";
    String JL_NOTIFY_OPEN = "JL - Ativar notificações";
    String JL_AUTH = "JL - Autenticar";
    String JL_INIT_FILE_SYS = "JL - Inicializar sistema de ficheiros";
    String JL_SET_PHOTO_DIAL = "JL - Configurar mostrador com fotografia";

    String JL_DEVICE = "JL - Operações do relógio";

    String JL_DEVICE_OTA = "OTA";

    String CONTACT = "Contactos";
    String GATT_CLOSE = "GATT - Fechar ligação";
    String FUNCTION_SWITCH = "Interruptores globais - Abrir";
    String READ_ECG_ID = "ECG - Ler ID";
    String READ_ECG_DATA = "ECG - Ler dados";
    String SET_ECG_NEW_DATA_REPORT = "ECG - Monitorizar novos dados";

    String DETECT_START_BODY_COMPONENT = "Composição corporal - Iniciar medição";
    String DETECT_STOP_BODY_COMPONENT = "Composição corporal - Parar medição";
    String READ_BODY_COMPONENT_ID = "Composição corporal - Ler ID";
    String READ_BODY_COMPONENT_DATA = "Composição corporal - Ler dados";

    String SET_BODY_COMPONENT_NEW_DATA_REPORT = "Composição corporal - Monitorizar novos dados";
    String SHARE_LOG = "Registos - Partilhar";
    String READ_BLOOD_COMPOSITION_CALIBRATION = "Composição sanguínea - Ler calibração";
    String SETTING_BLOOD_COMPOSITION_CALIBRATION = "Composição sanguínea - Configurar calibração";

    String DETECT_START_BLOOD_COMPONENT = "Composição sanguínea - Iniciar medição";
    String DETECT_STOP_BLOOD_COMPONENT = "Composição sanguínea - Parar medição";
    String DETECT_MULTI_ECG_DETECT = "ECG multi-derivação - Iniciar";
    String WORLD_CLOCK = "Relógio mundial";
    String G08W_HEALTH_ALARM_INTERVAL = "G08W - Configurar intervalo de alerta de saúde";
    String G08W_PPG_DATA_CALLBACK = "G08W - Monitorizar dados PPG";
    String MAGNETIC_OPEN = "Terapia magnética - Ativar";
    String TEXT_IMAGE_MSG_PUSH = "Envio de texto e imagem";
    String JH58_PPG = "JH58 - PPG";
    String MINI_CHECKUP = "Mini check-up - Iniciar";

    String GSR_START = "Condutância da pele - Iniciar medição";
    String GSR_STOP = "Condutância da pele - Parar medição";
    String ZT163_DEVICE_ALWAYS_OFF_SCREEN = "ZT163: ecrã sempre apagado";

    String GNSS_SOS_SAFETY_PROTECTION = "GNSS e SOS - Proteção de segurança";
    String FUN_SWITCH_READ = "Funções auxiliares de saúde - Ler";
    String FUN_SWITCH_SETTING = "Funções auxiliares de saúde - Configurar";
    String FUN_4G = "4G - Configurar função";
    String AUTO_MEASURE = "Medição automática - Configurar";
    String NRF_OTA = "NRF OTA";
    String QIU_GUO_TCM = "QiuGuo TCM - Ler dados";
    String QX17_DATA_ACQUISITION = "QX17 - Aquisição de dados";

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
