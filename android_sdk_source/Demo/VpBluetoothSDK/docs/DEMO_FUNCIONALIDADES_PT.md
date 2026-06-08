# Demo VpBluetoothSDK - Funcionalidades Existentes (PT)

## 1) Âmbito e objetivo
Este documento descreve, com base no código atual da demo Android (`VpBluetoothSDK`), o fluxo funcional principal e o catálogo de operações disponíveis no ecrã de operações.

Base técnica analisada:
- `app/src/main/java/com/timaimee/vpdemo/activity/MainActivity.java`
- `app/src/main/java/com/timaimee/vpdemo/activity/PwdConfirmActivity.java`
- `app/src/main/java/com/timaimee/vpdemo/activity/OperaterActivity.java`
- `app/src/main/java/com/timaimee/vpdemo/activity/Oprate.java`
- `app/src/main/java/com/timaimee/vpdemo/demo/DemoStepLogger.java`

## 2) Fluxo principal da demo

### 2.1 Scan BLE
Ponto de entrada: `MainActivity`.

- A inicialização chama `VPOperateManager.getInstance().init(this)`, ativa logs (`VPLogger.setDebug(true)`) e monitorização local (`VPLocalLogger.startMonitor(this)`).
- O scan é disparado por `scanDevice()` usando `VPOperateManager.getInstance().startScanDevice(mSearchResponse)`.
- No callback `SearchResponse`:
  - `onSearchStarted()` marca início de descoberta.
  - `onDeviceFounded(...)` atualiza a lista de dispositivos.
  - `onSearchStopped()`/`onSearchCanceled()` encerram o ciclo de scan.

### 2.2 Ligação ao dispositivo
- Ao selecionar um item da lista (`OnRecycleViewClick`), é chamado `connectDevice(mac, name)`.
- A demo regista `registerConnectStatusListener(mac, mBleConnectStatusListener)`.
- A ligação é feita por `VPOperateManager.getInstance().connectDevice(...)`.
- Em caso de sucesso, o callback `connectState(...)` recebe `Code.REQUEST_SUCCESS`.

### 2.3 Ativação de notify
- Após ligação, o callback `INotifyResponse.notifyState(...)` valida o canal de notificação.
- Quando `state == Code.REQUEST_SUCCESS`, a demo considera o canal pronto para operações BLE.
- Neste ponto é aberto o ecrã `PwdConfirmActivity`.

### 2.4 Password (confirmação de dispositivo)
No `PwdConfirmActivity`:

- O utilizador introduz password e aciona `confirmPassword()`.
- A validação usa `VPOperateManager.getInstance().confirmDevicePwd(...)`.
- Resultado esperado:
  - escrita OK (`"密码校验指令写入成功"`);
  - callback `onPwdDataChange(PwdData)` com `deviceNumber`, `deviceVersion`, `deviceTestVersion`.
- Em paralelo, a demo recebe e mostra:
  - pacotes de capacidades do dispositivo (`DeviceFunctionPackage1..5`);
  - suporte de mensagens (`FunctionSocailMsgData`, pacote 1 e 2);
  - estado de custom settings (`CustomSettingData`).

### 2.5 Sync info
- O sync explícito de dados pessoais existe como operação `PERSONINFO_SYNC` (`"2、个人信息-设置"`) em `OperaterActivity`.
- Implementação: `VPOperateManager.getInstance().syncPersonInfo(...)` com callback `IPersonInfoDataListener`.
- A demo mostra o estado via log: `"同步个人信息:" + EOprateStauts`.

### 2.6 Ecrã de operações
- Após confirmação de password, o botão "ir para funcionalidades" abre `OperaterActivity`.
- `initGridView()` popula a grelha diretamente com `Oprate.oprateStr`.
- Cada clique no item executa uma ação específica no `onItemClick(...)`.

Observação operacional importante:
- A string `SETTING_FIRST` (`"<--先操作1、2"`) indica o procedimento recomendado da demo: validar password e sincronizar informação pessoal antes de operações avançadas.

## 3) Catálogo de operações (`Oprate.oprateStr`)

Critério:
- A lista abaixo reflete **todas** as entradas efetivas do array `Oprate.oprateStr` (178 itens), agrupadas por categoria lógica para navegação funcional.
- A coluna `#` representa a posição da operação no array (ordem real de apresentação da grelha).
- Existem entradas repetidas por desenho (ex.: `BLE_DISCONNECT` aparece em mais de uma posição).

### Ligacao e Sessao (11)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 1 | `PWD_COMFIRM_2_DISCONNECT` | 发起BT立马断开 |
| 2 | `PWD_COMFIRM_2_DISCONNECT_` | 直接断开BLE |
| 5 | `DISCONNECT` | 蓝牙连接-断开 |
| 6 | `BT_CONNECT` | 连接BT |
| 7 | `BLE_DISCONNECT` | 断开BLE |
| 8 | `PWD_MODIFY` | 设备密码-修改 |
| 139 | `BLE_RENAME` | 蓝牙4.0重命名 |
| 140 | `BT_RENAME` | 蓝牙3.0重命名 |
| 141 | `BT_READ` | 读取BT |
| 142 | `BLE_DISCONNECT` | 断开BLE |
| 147 | `GATT_CLOSE` | Gatt-Close |

### Fluxo Inicial e Suporte (4)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 4 | `SETTING_FIRST` | <--先操作1、2 |
| 9 | `SHARE_LOG` | 分享日志 |
| 89 | `SHOW_SP` | 显示sp |
| 102 | `NONE` | NONE |

### Tempo, GPS e Seguranca (13)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 10 | `GPS_KAABA` | GPS时区&克尔白 |
| 11 | `GPS_REPORT_START` | GPS上报 |
| 12 | `READ_CHANTING` | 读取诵经 |
| 46 | `FINDPHONE` | 手机防丢 |
| 49 | `FINDDEVICE_SETTING_OPEN` | 设备防丢-打开 |
| 50 | `FINDDEVICE_SETTING_CLOSE` | 设备防丢-关闭 |
| 51 | `FINDDEVICE_READ` | 设备防丢-读取 |
| 117 | `LIANSUO_SOS` | 联硕-监听SOS |
| 118 | `LIANSUO_SEND_ORDER` | 联硕-发送命令 |
| 119 | `LIANSUO_SEND_CONTENT` | 联硕-发送内容 |
| 138 | `FIND_DEVICE` | 查找手机 |
| 161 | `WORLD_CLOCK` | 世界时钟 |
| 171 | `GNSS_SOS_SAFETY_PROTECTION` | GNSS&亲情安全守护&SOS |

### Dados Historicos e Sincronizacao (15)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 3 | `PERSONINFO_SYNC` | 2、个人信息-设置 |
| 24 | `SPORT_CURRENT_READ` | 当前计步-读取 |
| 79 | `READ_HEALTH_SLEEP` | 读取健康数据-睡眠 |
| 80 | `READ_HEALTH_SLEEP_FROM` | 读取健康数据-睡眠-从哪天起 |
| 81 | `READ_HEALTH_SLEEP_SINGLEDAY` | 读取健康数据-睡眠-读这天 |
| 82 | `READ_HEALTH_DRINK` | 读取健康数据-饮酒 |
| 83 | `READ_HEALTH_ORIGINAL` | 读取健康数据-5分钟 |
| 84 | `READ_HEALTH_ORIGINAL_FROM` | 读取健康数据-从哪天起 |
| 85 | `READ_HEALTH_ORIGINAL_SINGLEDAY` | 读取健康数据-读这天 |
| 86 | `READ_HEALTH` | 读取健康数据-全部 |
| 95 | `SPO2H_ORIGIN_READ` | 读取数据-血氧数据 |
| 96 | `HRV_ORIGIN_READ` | 读取数据-HRV数据 |
| 99 | `CLEAR_DEVICE_DATA` | 清除数据 |
| 128 | `ORIGIN_LOG` | 原始数据日志 |
| 129 | `RR` | RR逐跳帧 |

### Alarmes, Lembretes e Temporizadores (14)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 27 | `ALARM_SETTING` | 闹钟-设置 |
| 28 | `ALARM_READ` | 闹钟-读取 |
| 29 | `ALARM_NEW_LISTENER` | 新闹钟-监听状态改变 |
| 30 | `ALARM_NEW_` | 新闹钟 |
| 31 | `LONGSEAT_SETTING_OPEN` | 久坐-打开 |
| 32 | `LONGSEAT_SETTING_CLOSE` | 久坐-关闭 |
| 33 | `LONGSEAT_READ` | 久坐-读取 |
| 68 | `WOMEN_SETTING` | 女性状态-设置 |
| 69 | `WOMEN_READ` | 女性状态-读取 |
| 70 | `COUNT_DOWN_WATCH_CLOSE_UI` | 倒计时-手表单独使用(关闭界面) |
| 71 | `COUNT_DOWN_WATCH_OPEN_UI` | 倒计时-手表单独使用(打开界面) |
| 72 | `COUNT_DOWN_APP_READ` | 倒计时-读取 |
| 127 | `TEXT_ALARM` | 文字闹钟 |
| 146 | `CONTACT` | 联系人 |

### Mensagens e Telefonia (8)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 52 | `SOCIAL_MSG_SETTING` | 社交消息提醒1包-设置 |
| 53 | `SOCIAL_MSG_SETTING2` | 社交消息提醒2包-设置 |
| 54 | `SOCIAL_MSG_READ` | 社交消息提醒-读取设置 |
| 55 | `SOCIAL_MSG_SEND` | 社交消息提醒-发送内容 |
| 56 | `DEVICE_CONTROL_PHONE` | 监听手环-挂断，静音 |
| 57 | `SOCIAL_PHONE_IDLE_OR_OFFHOOK` | 社交消息提醒-接听了来电 |
| 130 | `G15_QR_CODE` | G15二维码 |
| 165 | `TEXT_IMAGE_MSG_PUSH` | 图文推送 |

### Ecra, Idioma e Personalizacao (22)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 34 | `LANGUAGE_CHINESE` | 语言设置-中文 |
| 35 | `LANGUAGE_ENGLISH` | 语言设置-英文 |
| 36 | `BATTERY` | 电池状态-读取 |
| 37 | `NIGHT_TURN_WRIST_OPEN` | 夜间转腕-打开 |
| 38 | `NIGHT_TURN_WRIST_CLOSE` | 夜间转腕-关闭 |
| 39 | `NIGHT_TURN_WRIST_READ` | 夜间转腕-读取 |
| 40 | `NIGHT_TURN_WRIST_CUSTOM_TIME` | 夜间转腕-自定义时间 |
| 41 | `NIGHT_TURN_WRIST_CUSTOM_TIME_LEVEL` | 夜间转腕-自定义时间和等级 |
| 42 | `DEVICE_COUSTOM_READ` | 个性化-读取 |
| 43 | `DEVICE_COUSTOM_SETTING` | 个性化-设置 |
| 44 | `DEVICE_ECG_ALWAYS_OPEN` | ECG常开-开 |
| 45 | `DEVICE_ECG_ALWAYS_CLOSE` | ECG常开-关 |
| 47 | `CHECK_WEAR_SETING_OPEN` | 佩戴检测-打开 |
| 48 | `CHECK_WEAR_SETING_CLOSE` | 佩戴检测-关闭 |
| 73 | `SCREEN_LIGHT_SETTING` | 屏幕调节-设置 |
| 74 | `SCREEN_LIGHT_READ` | 屏幕调节-读取 |
| 75 | `SCREEN_STYLE_READ` | 屏幕样式-读取 |
| 76 | `SCREEN_STYLE_SETTING` | 屏幕样式-设置 |
| 77 | `AIM_SPROT_CALC` | 目标步数-计算 |
| 78 | `INSTITUTION_TRANSLATION` | 公英制转换 |
| 87 | `SET_WATCH_TIME` | 设置时间 |
| 170 | `ZT163_DEVICE_ALWAYS_OFF_SCREEN` | 合镁ZT163设备常灭屏 |

### Medicoes Pontuais (27)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 13 | `HEART_DETECT_START` | 测量心率-开始 |
| 14 | `HEART_DETECT_STOP` | 测量心率-结束 |
| 15 | `TEMPTURE_DETECT_START` | 测量温度-开始 |
| 16 | `TEMPTURE_DETECT_STOP` | 测量温度-结束 |
| 17 | `READ_TEMPTURE_DATA` | 读取温度数据 |
| 18 | `BP_DETECT_START` | 测量血压-开始 |
| 19 | `BP_DETECT_STOP` | 测量血压-结束 |
| 61 | `SPO2H_OPEN` | 血氧-读取 |
| 62 | `SPO2H_CLOSE` | 血氧-结束 |
| 66 | `FATIGUE_OPEN` | 疲劳度-读取 |
| 67 | `FATIGUE_CLOSE` | 疲劳度-结束 |
| 97 | `HRV_START_DETECT` | 开始HRV测量 |
| 98 | `HRV_STOP_DETECT` | 停止HRV测量 |
| 100 | `DETECT_START_ECG` | 开始测量ECG |
| 101 | `DETECT_STOP_ECG` | 结束测量ECG |
| 110 | `DETECT_PTT` | PTT |
| 132 | `START_BLOOD_GLUCOSE` | 开始血糖监测 |
| 133 | `STOP_BLOOD_GLUCOSE` | 停止血糖监测 |
| 151 | `DETECT_START_BODY_COMPONENT` | 开始测量身体成分 |
| 152 | `DETECT_STOP_BODY_COMPONENT` | 结束测量身体成分 |
| 158 | `DETECT_START_BLOOD_COMPONENT` | 开始测量血液成分 |
| 159 | `DETECT_STOP_BLOOD_COMPONENT` | 结束测量血液成分 |
| 164 | `MAGNETIC_OPEN` | 磁疗 |
| 167 | `MINI_CHECKUP` | 微体检 |
| 168 | `GSR_START` | 开始皮电测量 |
| 169 | `GSR_STOP` | 结束皮电测量 |
| 177 | `QIU_GUO_TCM` | 秋果中医数据 |

### Parametros de Medicao e Alertas (26)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 20 | `BP_DETECTMODEL_SETTING` | 血压模式-设置 |
| 21 | `BP_DETECTMODEL_READ` | 血压模式-读取 |
| 22 | `BP_DETECTMODEL_SETTING_ADJUSTE_CANCEL` | 血压模式[动态调整]-取消 |
| 23 | `BP_DETECTMODEL_SETTING_ADJUSTE` | 血压模式[动态调整]-设置 |
| 58 | `HEARTWRING_READ` | 心率报警-读取 |
| 59 | `HEARTWRING_OPEN` | 心率报警-打开 |
| 60 | `HEARTWRING_CLOSE` | 心率报警-关闭 |
| 63 | `SPO2H_AUTO_DETECT_READ` | 血氧自动检测-读取 |
| 64 | `SPO2H_AUTO_DETECT_OPEN` | 血氧自动检测-打开 |
| 65 | `SPO2H_AUTO_DETECT_CLOSE` | 血氧自动检测-关闭 |
| 103 | `LOW_POWER_READ` | 低功耗-读取 |
| 104 | `LOW_POWER_OPEN` | 低功耗-开 |
| 105 | `LOW_POWER_CLOSE` | 低功耗-关 |
| 111 | `BP_FUNCTION_READ` | 血压状态(读取) |
| 112 | `BP_FUNCTION_SETTING` | 血压状态(设置) |
| 113 | `WEATHER_READ_STATUEINFO` | 天气状态(读取) |
| 114 | `WEATHER_SETTING_STATUEINFO_ON` | 天气状态(开) |
| 115 | `WEATHER_SETTING_STATUEINFO_OFF` | 天气状态(关) |
| 116 | `WEATHER_SETTING_DATA` | 天气数据(设置) |
| 143 | `HEALTH_REMIND` | 健康提醒 |
| 144 | `FUNCTION_SWITCH` | 全局监听开关 |
| 162 | `G08W_HEALTH_ALARM_INTERVAL` | G08W-健康报警区间 |
| 172 | `FUN_SWITCH_READ` | 健康辅助-读取 |
| 173 | `FUN_SWITCH_SETTING` | 健康辅助-设置 |
| 174 | `FUN_4G` | 4G功能 |
| 175 | `AUTO_MEASURE` | 自动测量 |

### Desporto e Modos (6)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 90 | `SPORT_MODE_ORIGIN_READ` | 读取数据-运动模式 |
| 91 | `SPORT_MODE_ORIGIN_READSTAUTS` | 读取状态-运动模式 |
| 92 | `SPORT_MODE_START_INDOOR` | 开启-室内步行 |
| 93 | `SPORT_MODE_ORIGIN_START` | 开启-运动模式 |
| 94 | `SPORT_MODE_ORIGIN_END` | 结束-运动模式 |
| 178 | `QX17_DATA_ACQUISITION` | QX17数据采集 |

### Multimedia (5)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 25 | `CAMERA_START` | 拍照模式-开始 |
| 26 | `CAMERA_STOP` | 拍照模式-停止 |
| 124 | `SYNC_MUSIC_INFO_PLAY` | 音乐-播放 |
| 125 | `SYNC_MUSIC_INFO_PAUSE` | 音乐-暂停 |
| 126 | `VOLUME` | 音量 |

### Firmware, UI e OTA (8)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 88 | `OAD` | 固件升级 |
| 120 | `UI_UPDATE_AGPS` | UI升级-AGPS |
| 121 | `UI_UPDATE_CUSTOM` | UI升级-自定义表盘 |
| 122 | `UI_UPDATE_SERVER` | UI升级-服务器表盘 |
| 123 | `UI_UPDATE_G15IMG` | UI升级-G15图片传输 |
| 145 | `JL_DEVICE` | 杰理手錶相關 |
| 166 | `JH58_PPG` | JH58PPG相关 |
| 176 | `NRF_OTA` | NRF OTA |

### ECG, Corpo e Composicao (14)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 131 | `ECG_AUTO_REPORT_TEXT` | ECG常开数据监听 |
| 134 | `BLOOD_GLUCOSE_P_READ` | 血糖私人模式读取 |
| 135 | `BLOOD_GLUCOSE_P_SETTING` | 血糖私人模式设置 |
| 136 | `BLOOD_GLUCOSE_MULTIPLE_READ` | 血糖多校准读取 |
| 137 | `BLOOD_GLUCOSE_MULTIPLE_SETTING` | 血糖多校准设置 |
| 148 | `READ_ECG_ID` | 读取ECG ID |
| 149 | `READ_ECG_DATA` | 读取ECG 数据 |
| 150 | `SET_ECG_NEW_DATA_REPORT` | 监听新ecg数据上报 |
| 153 | `READ_BODY_COMPONENT_ID` | 读取身体成分ID |
| 154 | `READ_BODY_COMPONENT_DATA` | 读取身体成分数据 |
| 155 | `SET_BODY_COMPONENT_NEW_DATA_REPORT` | 监听新身体成分数据上报 |
| 156 | `READ_BLOOD_COMPOSITION_CALIBRATION` | 读取血液成分校准值 |
| 157 | `SETTING_BLOOD_COMPOSITION_CALIBRATION` | 设置血液成分校准值 |
| 160 | `DETECT_MULTI_ECG_DETECT` | ECG多导联 |

### Modelos Especificos (5)

| # | Constante (`Oprate`) | String exibida (`oprateStr`) |
|---:|---|---|
| 106 | `S22_READ_DATA` | S22-数据读取 |
| 107 | `S22_READ_STATE` | S22-状态读取 |
| 108 | `S22_SETTING_STATE_OPEN` | S22-状态设置(开) |
| 109 | `S22_SETTING_STATE_CLOSE` | S22-状态设置(关) |
| 163 | `G08W_PPG_DATA_CALLBACK` | G08W-PPG数据监听回调 |

## 4) Validação por logs em runtime

### 4.1 Tags e fontes de log
- `SDK_DEMO`: logs estruturados de passos (`DemoStepLogger`) com padrão `[START]`, `[SUCCESS]`, `[ERROR]`, `[FEATURE]`.
- `MainActivity` / `OperaterActivity` / `-密码校验-`: logs funcionais detalhados por callback.
- `write cmd status:<code>`: confirmação transversal de escrita BLE (`IBleWriteResponse`).

### 4.2 Comando recomendado (logcat)
Exemplo prático para validar o fluxo principal sem alterar código:

```bash
adb logcat -v time | rg "SDK_DEMO|onSearchStarted|连接成功|监听成功-可进行其他操作|PwdData|同步个人信息|write cmd status|STATUS_CONNECTED|STATUS_DISCONNECTED"
```

### 4.3 Checklist de evidências por etapa
1. Scan BLE iniciado
- Esperado: `[START][BLE_SCAN] ...` e/ou `onSearchStarted`.

2. Dispositivo descoberto
- Esperado: `device for <name>-<mac>-<rssi>`.

3. Ligação BLE estabelecida
- Esperado: `连接成功` e `[SUCCESS][BLE_CONNECT] ...`.

4. Canal notify ativo
- Esperado: `监听成功-可进行其他操作` e `[SUCCESS][BLE_NOTIFY] ...`.

5. Password confirmada
- Esperado: `密码校验指令写入成功` + bloco `PwdData:`.

6. Capacidades recebidas
- Esperado: blocos `【功能第1包】...【功能第5包】`, `【消息开关第1包】`, `【消息开关第2包】`, `【开关设置】`.

7. Sync de informação pessoal
- Ao executar operação `PERSONINFO_SYNC`: `同步个人信息:` seguido de `EOprateStauts`.

8. Escritas BLE de operações
- Esperado: `write cmd status:<code>` (normalmente `Code.REQUEST_SUCCESS`).

### 4.4 Validação do ficheiro de logs da demo
- A operação `SHARE_LOG` chama `VPLocalLogger.getInstance().shareLogFile(...)` em `OperaterActivity`.
- Esta operação permite exportar o histórico de logs recolhidos em runtime para auditoria funcional.

## 5) Notas finais
- Este rascunho documenta apenas funcionalidades **já existentes** no código atual da demo.
- Não foram inferidas APIs novas nem alterado comportamento de Java/Kotlin/XML.
