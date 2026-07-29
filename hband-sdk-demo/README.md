# H Band SDK Demo — MF91

Aplicação Ionic 8, Angular 20 e Capacitor 8 focada nas métricas de saúde da
pulseira MF91:

- frequência cardíaca;
- pressão arterial;
- oxigénio no sangue;
- temperatura corporal;
- glicemia;
- HRV;
- ECG;
- composição corporal;
- MET;
- stress.

A aplicação apresenta medição atual e histórico de um dia escolhido. Cada
métrica usa uma representação adequada: séries, medidores, barras, grelha de
composição corporal ou traçado ECG. As restantes superfícies funcionais do SDK
não fazem parte deste demonstrador.

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
tal como no exemplo oficial.

MET (equivalente metabólico) representa a proporção entre o gasto energético
durante a atividade e em repouso. A referência de 1 MET corresponde a
3,5 ml de oxigénio por kg de peso corporal por minuto. A visualização classifica
os valores como repouso (cerca de 1,0 MET), atividade leve (1,6–2,9), moderada
(3,0–5,9) ou vigorosa (6,0+).

O SDK não disponibiliza um comando de medição MET em tempo real; a bridge
apresenta diretamente os valores automáticos/históricos recebidos, sem os
recalcular. As capacidades reportadas pelo firmware bloqueiam ações declaradas
como não suportadas.

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
