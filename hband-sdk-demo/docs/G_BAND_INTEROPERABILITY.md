# Análise funcional da G Band

Esta análise foi feita no telemóvel de ensaio com a G Band 2.1.17
(`com.vpgband.app`) e a MF91 `1B:F0:06:E2:86:FC`. Foram observados apenas o
comportamento, as superfícies funcionais e os contratos públicos do mesmo SDK.
Não foram copiados código, imagens, marca ou identidade visual.

## Ligação e sincronização observadas

1. A aplicação conserva o endereço da pulseira e tenta voltar a ligá-la.
2. Mantém um serviço foreground BLE.
3. Depois de ligar e autenticar, lê bateria e informação de produção.
4. Sincroniza o perfil e pede os blocos de dados originais dos dias retidos.
5. Recebe progresso e listas `OriginData3`, guarda-as localmente e atualiza os
   cartões do dia.

Na demonstração, o equivalente foi implementado com:

- serviço foreground Android de tipo `connectedDevice`;
- reconexão ao último MAC autenticado quando a queda não foi intencional;
- redescoberta limitada ao mesmo MAC após reinício do adaptador Bluetooth, com
  timeout independente dos callbacks do SDK;
- fila única para bateria, hora, atividade e histórico;
- uma leitura diária `readOriginDataSingleDay` no Android;
- separação dos blocos originais em passos, frequência cardíaca, pressão
  arterial, oxigénio, temperatura, glicemia e stress;
- arquivo persistente por pulseira, métrica, data e instante;
- união de blocos automáticos, medições manuais e leitores dedicados;
- calendário com destaque dos dias existentes no arquivo.

## Histórico acumulado observado

Com a G Band desligada da pulseira, o calendário mostrou dados em 8 e 11 de
junho de 2026. Foi possível abrir 8 de junho e consultar a frequência cardíaca
desse dia, incluindo média diária de 71 bpm, mínimo de 56 bpm e máximo de
93 bpm.

Essa data estava muito além da janela disponível na MF91. A análise do APK
confirmou Room/SQLite, chaves compostas por conta/MAC/data ou
conta/MAC/timestamp e upserts equivalentes a `INSERT OR REPLACE`. A interface
combina, portanto, dados que já estão na base da aplicação com a janela ainda
disponível na pulseira.

## Formatos reproduzidos

| Métrica | Formato funcional observado e aplicado |
| --- | --- |
| Passos | Barras ao longo de 0–24 h, intervalo selecionado, total, distância e calorias |
| Frequência cardíaca | Média do intervalo, linha 0–24 h, média diária, mínimo e máximo |
| Pressão arterial | Par sistólica/diastólica, duas séries 0–24 h, média, mínimo e máximo |
| Oxigénio | Média do intervalo, linha 0–24 h, média diária, mínimo e máximo |
| Temperatura | Linha 0–24 h, corpo/pele, média diária, mínimo e máximo |
| Glicemia | Linha 0–24 h, média diária, mínimo e máximo; medição em curso e resultado final separados |
| ECG | Traçado sobre grelha com os valores finais disponibilizados pelo SDK |
| Composição corporal | Progresso da leitura e grelha dos componentes devolvidos |
| Stress | Barras 0–24 h, média do intervalo, média diária, mínimo, máximo e faixas de classificação |

Os intervalos usados são 10 minutos para oxigénio, 60 minutos para pressão e
temperatura e 30 minutos para frequência cardíaca, glicemia e stress. A
identidade visual, tipografia, cores, cartões e navegação continuam a ser as da
aplicação H Band SDK Demo.

## Fluxo das medições manuais

A separação funcional observada na G Band foi aplicada sem copiar a sua
identidade visual:

1. o detalhe de uma métrica abre no histórico do dia selecionado;
2. o botão de medição abre uma superfície dedicada aos dados em tempo real;
3. a leitura pode terminar pelo progresso final do SDK ou pelo comando de paragem;
4. quando existe um resultado real, a interface pede confirmação antes de o
   arquivar;
5. ao confirmar, o timestamp do callback determina o dia correto e o resultado
   é unido ao histórico local;
6. ao recusar, o resultado continua visível apenas como último dado da sessão e
   não é introduzido no arquivo.

Estados isolados do protocolo, como progresso sem valores clínicos, não são
oferecidos para gravação. Os passos também não entram neste fluxo porque o SDK
os fornece como atividade diária acumulada, não como medição manual iniciável.

## Ensaio físico de 29 de julho de 2026

A sincronização da demonstração autenticou a MF91 indicada, leu bateria a
100%, firmware `02.73.01`, hardware `5966`, 112 passos atuais e, numa só leitura
diária, recebeu:

- 5 intervalos de passos;
- 20 registos de frequência cardíaca;
- 24 registos de pressão arterial;
- 30 registos de oxigénio;
- 25 registos de temperatura;
- 26 registos de glicemia;
- 23 registos de stress.

O serviço foreground ficou ativo durante a sessão. O ensaio confirma os
callbacks, a serialização e a apresentação do histórico real neste dispositivo;
não atribui validade clínica aos valores.

## Limites

- A G Band pode conservar dados já sincronizados na sua base local, enquanto a
  pulseira só expõe os blocos ainda retidos.
- Na MF91 ensaiada, o SDK reportou hoje e até aos três dias anteriores; outros
  firmwares podem reportar uma retenção diferente.
- ECG e composição corporal não fazem parte do bloco diário comum e mantêm os
  leitores próprios.
- O serviço foreground reduz suspensões enquanto o processo existe, mas não
  implementa arranque após reinício ou encerramento forçado.
- A tentativa de composição corporal na aplicação de referência chegou ao fim
  sem resultado válido, compatível com contacto insuficiente dos elétrodos; não
  foi tratada como confirmação clínica nem funcional do resultado.
- A arquitetura histórica completa da demonstração está descrita em
  [`HISTORY_AND_SYNC.md`](HISTORY_AND_SYNC.md).
