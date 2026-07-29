# H Band SDK Demo — MF91

Aplicação de demonstração Ionic 8, Angular 20 e Capacitor 8 para explorar os
SDKs H Band/Veepoo com a pulseira MF91 em Android e iOS.

## O que inclui

- catálogo visual de 43 módulos funcionais, agrupados em ligação, medições,
  histórico, automação, interação e funções avançadas;
- visualizações próprias para séries, medidores, barras, sono, tabelas,
  cronologias e eventos técnicos;
- pesquisa, filtro por grupo, capacidades reportadas pelo firmware e bloqueio
  de funções declaradas como não suportadas;
- simulação web para avaliar a interface sem uma pulseira;
- bridge Capacitor Android sobre `VPOperateManager`;
- bridge Capacitor iOS sobre `VPBleCentralManage` e
  `VPPeripheralBaseManage`;
- português de Portugal, português do Brasil, inglês, espanhol e francês;
- fila única de comandos BLE, porque o SDK não suporta operações demoradas em
  paralelo.

O catálogo integral e a cobertura nativa são conceitos distintos. Os módulos
sem contrato confirmado continuam visíveis para documentar a superfície do
SDK, mas os respetivos botões ficam desativados numa aplicação nativa. Consulte
[`docs/SDK_COVERAGE.md`](docs/SDK_COVERAGE.md).

## Executar

Requisitos:

- Node.js 20 ou superior;
- Android Studio/JDK 21 para Android;
- Xcode 16 ou superior para iOS;
- um dispositivo físico iOS. Os frameworks fornecidos são binários ARM para
  dispositivo e não contêm slices de simulador.

```bash
npm install
npm start
```

Build web e sincronização:

```bash
npm run build
npm run cap:sync
```

Build Android:

```bash
npm run android:build
```

Build iOS sem assinatura:

```bash
npm run ios:build
```

Para instalar num telefone, abra `android/` no Android Studio ou
`ios/App/App.xcodeproj` no Xcode, escolha a equipa de assinatura e selecione um
dispositivo físico.

## Fluxo de ligação

1. Conceder permissão Bluetooth.
2. Procurar a MF91.
3. Selecionar o periférico e confirmar a password do dispositivo. O valor
   predefinido pelo SDK é `0000`.
4. Aguardar a confirmação de notify e da password.
5. Consultar as capacidades do firmware.
6. Executar uma medição de cada vez e pará-la explicitamente quando aplicável.

A sincronização do perfil só é enviada quando altura, peso, ano de nascimento,
idade, objetivo de passos e sexo foram todos preenchidos. Não existem valores
pessoais implícitos.

## Segurança

- os resultados apresentados são dados do dispositivo e não constituem
  diagnóstico médico;
- atualização de firmware, limpeza de dados, reset e transferências de conteúdo
  não são executáveis neste bridge sem um contrato e ficheiro validados;
- a simulação web serve apenas para validar interface e navegação;
- um build bem-sucedido não confirma ligação, precisão clínica nem suporte
  efetivo do firmware MF91.

O ensaio em hardware está descrito em
[`docs/MF91_PHYSICAL_TEST_PLAN.md`](docs/MF91_PHYSICAL_TEST_PLAN.md).
