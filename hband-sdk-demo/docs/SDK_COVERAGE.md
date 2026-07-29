# Cobertura das métricas

| Métrica | Visualização | Android | iOS | Histórico por data |
| --- | --- | --- | --- | --- |
| Frequência cardíaca | Série em bpm | Iniciar/parar | Iniciar/parar | Sim |
| Pressão arterial | Barras sistólica/diastólica | Iniciar/parar | Iniciar/parar | Sim |
| Oxigénio no sangue | Medidor SpO₂ | Iniciar/parar | Iniciar/parar | Sim |
| Temperatura corporal | Série em °C | Iniciar/parar | Iniciar/parar | Sim |
| Glicemia | Série em mmol/L | Iniciar/parar | Iniciar/parar | Sim |
| ECG | Traçado | Iniciar/parar | Iniciar/parar | Sim |
| Composição corporal | Grelha de componentes | Iniciar/parar | Iniciar/parar | Sim |
| Passos | Barras de atividade | Leitura atual | Leitura atual | Sim |
| Stress | Medidor 0–100 | Iniciar/parar | Iniciar/parar | Sim |

## Origem do histórico

- Android: `readDeviceManualData`, `readECGData` e
  `readBodyComponentData`.
- iOS: sincronização serializada do tipo de dados e consultas de
  `VPDataBaseOperation` pela data exata.
- Stress no iOS é extraído dos dados originais diários, onde o próprio SDK
  documenta o campo `stress`.

O estado `supported`, `unsupported` ou `unknown` é calculado depois da
autenticação a partir das capacidades do dispositivo. A compilação das bridges
confirma apenas a compatibilidade com as versões de SDK incluídas; a cobertura
real da MF91 continua dependente de ensaio físico.
