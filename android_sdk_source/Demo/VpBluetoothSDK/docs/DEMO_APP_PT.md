# Aplicação de Demonstração do Android_Ble_SDK (PT)

## 1) Objetivo
Esta aplicação demonstra, de forma prática e validável, as funcionalidades expostas pelo SDK BLE.
O fluxo foi instrumentado com logs estruturados para que cada passo seja rastreável em runtime.

## 2) Arquitetura da demo

### 2.1 Fluxo principal de utilização
1. `MainActivity`
- Inicializa `VPOperateManager`.
- Valida permissões BLE conforme versão Android.
- Faz scan de dispositivos BLE.
- Permite selecionar e ligar ao dispositivo.

2. `PwdConfirmActivity`
- Executa o handshake obrigatório com password (`confirmDevicePwd`).
- Obtém versão/dispositivo e pacotes de funcionalidades suportadas.
- Recolhe estados necessários para habilitar testes seguintes.

3. `OperaterActivity`
- Agrega o catálogo de operações (`Oprate.oprateStr`).
- Executa leitura/escrita/início/fim de medições e fluxos avançados (OTA, UI, ECG, etc.).

### 2.2 Instrumentação de logs adicionada
Foi criada a classe:
- `com.timaimee.vpdemo.demo.DemoStepLogger`

Formato dos logs:
- `[START][STEP_ID] ...`
- `[SUCCESS][STEP_ID] ...`
- `[ERROR][STEP_ID] ...`
- `[FEATURE][NOME_FEATURE] ...`

Passos críticos já instrumentados:
- `MAIN_INIT`, `MAIN_PERMISSION`, `BLE_SCAN`, `BLE_CONNECT`, `BLE_NOTIFY`
- `PWD_SCREEN_INIT`, `PWD_VALIDATE`, `PWD_CAPABILITIES`, `OPERATE_SCREEN_OPEN`
- `OPERATE_INIT`, `OPERATION_SELECT`, `OPERATION_RESULT`, `BLE_WRITE`, `CALLBACK_SETUP`

## 3) Como executar a demo

### 3.1 Pré-requisitos
- Android API 19+
- Dispositivo com Bluetooth LE
- Permissões BLE/localização aceites em runtime
- Bibliotecas `.aar`/`.jar` já incluídas em `app/libs`

### 3.2 Execução
1. Abrir o projeto `android_sdk_source/Demo/VpBluetoothSDK` no Android Studio.
2. Compilar e instalar a app módulo `:app`.
3. Abrir a app e conceder permissões solicitadas.
4. Fazer scan e ligar ao relógio/dispositivo BLE.
5. Validar password no ecrã dedicado.
6. Entrar no ecrã de operações e testar funcionalidades por grupo.

## 4) Validação em runtime por logs
Filtrar no Logcat por:
- Tag principal: `SDK_DEMO`
- Tags existentes do projeto: `MainActivity`, `-密码校验-`, `OperaterActivity`

Exemplos de validação:
1. Scan BLE
- Esperado: `[START][BLE_SCAN]` -> `[SUCCESS][BLE_SCAN]` ou `[ERROR][BLE_SCAN]`

2. Ligação BLE + notify
- Esperado: `[START][BLE_CONNECT]` -> `[SUCCESS][BLE_CONNECT]`
- Depois: `[SUCCESS][BLE_NOTIFY]`

3. Password e capacidades
- Esperado: `[START][PWD_VALIDATE]`
- Sucesso: `[SUCCESS][PWD_VALIDATE]` e eventos `[FEATURE][PWD_CAPABILITIES]`

4. Operações individuais
- Ao clicar num item: `[FEATURE][OPERATION_SELECT]`
- Resultado textual: `[FEATURE][OPERATION_RESULT]`
- Escrita de comando BLE: `[SUCCESS][BLE_WRITE]` ou `[ERROR][BLE_WRITE]`

## 5) Cobertura funcional
A cobertura total de operações disponíveis no ecrã de demonstração encontra-se no catálogo:
- [`CATALOGO_OPERACOES_PT.md`](./CATALOGO_OPERACOES_PT.md)
- [`DEMO_FUNCIONALIDADES_PT.md`](./DEMO_FUNCIONALIDADES_PT.md)

Esse catálogo foi construído a partir de `Oprate.oprateStr`, garantindo alinhamento com o que é efetivamente executável na app.

## 6) Notas técnicas importantes
1. O SDK não é orientado a operações concorrentes agressivas.
- Evitar disparar múltiplas operações longas em paralelo sobre o mesmo dispositivo.

2. A validação de password é pré-condição funcional.
- Sem este passo, várias operações podem falhar ou devolver dados incompletos.

3. Nem todos os relógios suportam todas as funções.
- A disponibilidade depende dos pacotes de capacidades recebidos no handshake.

4. Correção funcional aplicada nesta iteração.
- O ecrã de password agora envia para o SDK a password realmente introduzida pelo utilizador (em vez de valor fixo).
