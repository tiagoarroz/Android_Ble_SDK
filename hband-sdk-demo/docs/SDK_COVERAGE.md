# Cobertura das métricas

| Métrica | Medição atual | Histórico por data | Monitorização automática | Formato diário |
| --- | --- | --- | --- | --- |
| Frequência cardíaca | bpm, estado e botão único iniciar/parar | Origem diária | Configuração dinâmica do SDK | Média de 30 min, linha 0–24 h, média, mínimo e máximo |
| Pressão arterial | Sistólica/diastólica em texto e progresso horizontal | Origem diária | Configuração dinâmica do SDK | Média horária, duas linhas, média, mínimo e máximo |
| Oxigénio no sangue | SpO₂ e progresso circular | Origem diária | Configuração dinâmica do SDK | Média de 10 min, linha 0–24 h, média, mínimo e máximo |
| Temperatura corporal | Corpo e superfície, uma casa decimal | Origem diária | Configuração dinâmica do SDK | Média horária, linha 0–24 h, média, mínimo, máximo e seletor corpo/pele |
| Glicemia | Valores em curso, resultado final e progresso horizontal | Origem diária | Configuração dinâmica do SDK | Média de 30 min, linha 0–24 h, média, mínimo e máximo |
| ECG | Traçado de amostras em grelha | Leitor ECG dedicado | Não exposta pelo SDK automático | Traçado e resultados guardados |
| Composição corporal | Grelha de componentes e progresso horizontal | Leitor dedicado | Não exposta pelo SDK automático | Componentes devolvidos pelo dispositivo |
| Passos | Total, distância e calorias | Origem diária | Não é uma medição configurável | Barras por hora 0–24 h, total, distância e calorias |
| Stress | Pontuação, classificação e progresso circular | Origem diária | Configuração dinâmica do SDK | Média de 30 min, barras 0–24 h, média, mínimo e máximo |

## Monitorização automática

As bridges preferem a lista dinâmica devolvida por dispositivos recentes. No
Android o contrato é `readAutoMeasureSettingData` / `setAutoMeasureSettingData`;
no iOS é `veepooSDKReadAutoMonitSwitchInfo` / `veepooSDKSetAutoMonitSwitch`.

A MF91 ensaiada, com firmware `02.73.01`, declara essa API dinâmica como não
suportada. Tal como a G Band, a aplicação usa então os contratos legados:

- Android: `readCustomSetting` / `changeCustomSetting` para frequência
  cardíaca, pressão, temperatura, glicemia e stress, mais
  `readSpo2hAutoDetect` / `settingSpo2hAutoDetect` para oxigénio;
- iOS: `veepooSDKSettingBaseFunctionType` com os tipos individuais
  correspondentes.

No Android, a personalização chega em dois callbacks. A bridge só termina o
comando depois do segundo pacote, evitando iniciar a leitura de oxigénio ou uma
nova escrita enquanto ainda chegam temperatura, glicemia e stress.

Ao abrir uma métrica, a aplicação:

1. lê os modelos reais suportados pela pulseira;
2. apresenta estado e, quando o protocolo os fornece, intervalo e janela
   horária apenas se existir configuração dessa métrica;
3. ao ativar ou desativar, altera exclusivamente o campo do interruptor;
4. conserva o intervalo, a janela horária e as restrições recebidas do firmware;
5. mantém o estado anterior se o SDK rejeitar a escrita.

O enum automático dos SDKs inclui frequência cardíaca, pressão arterial,
glicemia, stress, oxigénio e temperatura. Inclui ainda tipos que não pertencem
ao catálogo atual. Passos, ECG e composição corporal não fazem parte desse
contrato e, por isso, não recebem controlos artificiais na interface.

## Origem do histórico

- Android: uma leitura `readOriginDataSingleDay` fornece os dados automáticos
  comuns em `OriginData3`. `readECGData` e `readBodyComponentData` tratam os
  dois formatos dedicados.
- iOS: sincronização serializada do tipo de dados e consultas de
  `VPDataBaseOperation` pela data exata.
- Stress no iOS é extraído dos dados originais diários, onde o próprio SDK
  documenta o campo `stress`.

`readDeviceManualData` não é usado como sincronização geral: na versão do SDK
incluída, a documentação limita esse leitor manual à pressão pneumática. Esta
restrição evita bloquear a fila BLE ao pedir tipos não suportados.

## Sessão Android

- `HBandConnectionService` corre em foreground com o tipo
  `connectedDevice` depois da autenticação.
- A bridge conserva o MAC autenticado e agenda reconexão com espera progressiva
  até 30 segundos em quedas não intencionais.
- Quando o adaptador é reativado, a bridge reinicializa o cliente do SDK e
  redescobre apenas o MAC guardado; cada pesquisa tem um watchdog próprio.
- Bateria, hora, atividade e histórico são pedidos em série e cada operação só
  termina quando chega o callback do SDK.
- Desligar explicitamente cancela a reconexão e termina o serviço.

O estado `supported`, `unsupported` ou `unknown` é calculado depois da
autenticação a partir das capacidades do dispositivo. A compilação das bridges
confirma apenas a compatibilidade com as versões de SDK incluídas; a cobertura
real da MF91 continua dependente de ensaio físico.

## Nome Bluetooth da pulseira

O botão de lápis junto ao nome executa `device.rename` apenas numa sessão
ligada. A bridge espera pelo callback final antes de atualizar a interface:

- Android: `VPOperateManager.bleDeviceRename` e `IDeviceRenameListener`;
- iOS: `veepooSDKSettingDeviceNameWithString:resultBlock:`.

O nome é validado em bytes UTF-8 e limitado conservadoramente a oito bytes, o
valor suportado em todas as plataformas descritas pelo SDK. Alguns dispositivos
JL aceitam dezoito bytes, mas esse limite não é usado sem confirmação explícita
do hardware. O SDK pode aceitar a escrita e o sistema operativo continuar a
mostrar temporariamente o nome antigo durante uma nova pesquisa devido à cache
Bluetooth. A compilação confirma o contrato; a aceitação pelo firmware da MF91
é confirmada apenas pelo callback numa pulseira física.
