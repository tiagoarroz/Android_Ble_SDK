# H Band SDK Demo — MF91

Aplicação Ionic 8, Angular 20 e Capacitor 8 focada nas métricas de saúde da
pulseira MF91:

- frequência cardíaca;
- pressão arterial;
- oxigénio no sangue;
- temperatura corporal;
- glicemia;
- ECG;
- composição corporal;
- passos;
- stress.

A aplicação apresenta medição atual e histórico de um dia escolhido. Cada
métrica usa uma representação adequada: séries, medidores, barras, grelha de
composição corporal, atividade ou traçado ECG. A bateria da pulseira é
apresentada na área de ligação. As restantes superfícies funcionais do SDK não
fazem parte deste demonstrador.

## Medição e logs

Cada métrica em tempo real usa um único botão. O botão inicia a medição quando
esta está inativa e passa a pará-la depois de a bridge aceitar o início. Ao
desligar o dispositivo, todos os estados de medição são limpos.

No fundo de cada modal existe um painel com scroll próprio. O painel apresenta
as operações pedidas e aceites, mensagens da bridge e cada evento de dados
recebido do dispositivo, incluindo valores, amostras, registos históricos e o
valor bruto disponibilizado pelo SDK.

## Histórico diário

O botão **Ler histórico** envia `history.metric` com:

```json
{
  "metric": "heartRate",
  "date": "2026-07-29"
}
```

No Android, os registos manuais são pedidos a partir do início do dia e
filtrados até ao início do dia seguinte. ECG e composição corporal usam os
leitores próprios do SDK. No iOS, os dados são primeiro sincronizados de forma
serializada e depois consultados na base local do SDK pela data `yyyy-MM-dd`,
tal como no exemplo oficial. Os passos usam o leitor de atividade do SDK e
incluem também distância e calorias.

As capacidades reportadas pelo firmware bloqueiam ações declaradas como não
suportadas.

## Executar

Requisitos:

- Node.js 20 ou superior;
- Android Studio/JDK 21 para Android;
- Xcode 16 ou superior para iOS;
- dispositivo físico iOS, porque os frameworks fornecidos não têm slices de
  simulador.

```bash
npm install
npm run build
npm run cap:sync
```

Build Android:

```bash
cd android
./gradlew assembleDebug
```

Build iOS sem assinatura:

```bash
cd ios/App
xcodebuild -project App.xcodeproj -scheme App -configuration Debug \
  -sdk iphoneos CODE_SIGNING_ALLOWED=NO build
```

## Limites de validação

- A simulação web valida interface e visualizações, não o dispositivo.
- Um build bem-sucedido não confirma que um firmware MF91 concreto suporta
  todas as métricas.
- Os valores apresentados vêm do SDK/dispositivo e não constituem diagnóstico
  médico.
- A validação física está descrita em
  [`docs/MF91_PHYSICAL_TEST_PLAN.md`](docs/MF91_PHYSICAL_TEST_PLAN.md).
