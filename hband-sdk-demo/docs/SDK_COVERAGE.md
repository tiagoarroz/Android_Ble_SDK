# Cobertura das métricas

| Métrica | Medição atual | Histórico por data | Formato diário |
| --- | --- | --- | --- |
| Frequência cardíaca | bpm, estado e botão único iniciar/parar | Origem diária | Média de 30 min, linha 0–24 h, média, mínimo e máximo |
| Pressão arterial | Sistólica/diastólica em texto e progresso horizontal | Origem diária | Média horária, duas linhas, média, mínimo e máximo |
| Oxigénio no sangue | SpO₂ e progresso circular | Origem diária | Média de 10 min, linha 0–24 h, média, mínimo e máximo |
| Temperatura corporal | Corpo e superfície, uma casa decimal | Origem diária | Média horária, linha 0–24 h, média, mínimo, máximo e seletor corpo/pele |
| Glicemia | Valores em curso, resultado final e progresso horizontal | Origem diária | Média de 30 min, linha 0–24 h, média, mínimo e máximo |
| ECG | Traçado de amostras em grelha | Leitor ECG dedicado | Traçado e resultados guardados |
| Composição corporal | Grelha de componentes e progresso horizontal | Leitor dedicado | Componentes devolvidos pelo dispositivo |
| Passos | Total, distância e calorias | Origem diária | Barras por hora 0–24 h, total, distância e calorias |
| Stress | Pontuação, classificação e progresso circular | Origem diária | Média de 30 min, barras 0–24 h, média, mínimo e máximo |

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
