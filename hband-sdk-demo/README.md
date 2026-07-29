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

Ao autenticar a pulseira, a aplicação executa uma sincronização serializada do
dia atual. O botão **Sincronizar dia** repete o mesmo fluxo para a data
selecionada e mostra o progresso:

1. lê a bateria;
2. acerta a hora do dispositivo;
3. lê os totais de atividade atuais;
4. importa os blocos originais do dia.

No Android, uma única operação `history.daily` usa `readOriginDataSingleDay`.
Os blocos `OriginData3` são convertidos em registos de passos, frequência
cardíaca, pressão arterial, oxigénio, temperatura, glicemia e stress sem lançar
em paralelo leitores incompatíveis. ECG e composição corporal continuam a usar
os leitores dedicados do SDK. No iOS, as sincronizações e consultas à base
local do SDK são igualmente executadas em série e filtradas pela data
`yyyy-MM-dd`.

Os históricos apresentam o intervalo mais recente, gráfico posicionado entre
0–24 horas, média diária, mínimo e máximo. A pressão arterial conserva as duas
séries; a temperatura permite alternar entre corporal e pele; stress e passos
usam barras; ECG mantém o traçado.

## Continuidade da ligação

Depois da autenticação Android, um serviço foreground de tipo
`connectedDevice` mantém a sessão BLE ativa com uma notificação persistente. Se
a ligação cair enquanto a aplicação continua em execução, a bridge tenta ligar
novamente ao mesmo MAC com espera progressiva até 30 segundos. Depois de o
adaptador Bluetooth reiniciar, recompõe o cliente GATT e faz pesquisas curtas
dirigidas exclusivamente ao MAC recordado. Um watchdog impede que a fila fique
presa se o SDK omitir o fim da pesquisa. Uma desconexão pedida pelo utilizador
cancela estas tentativas.

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
- O serviço de ligação não substitui um arranque automático após reinício do
  telemóvel ou encerramento forçado do processo.
- Os valores apresentados vêm do SDK/dispositivo e não constituem diagnóstico
  médico.
- A análise funcional usada para alinhar a experiência está documentada em
  [`docs/G_BAND_INTEROPERABILITY.md`](docs/G_BAND_INTEROPERABILITY.md).
- A validação física está descrita em
  [`docs/MF91_PHYSICAL_TEST_PLAN.md`](docs/MF91_PHYSICAL_TEST_PLAN.md).
