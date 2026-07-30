# Histórico e sincronização da MF91

Este documento regista o funcionamento confirmado do histórico na H Band SDK
Demo, a evidência recolhida na G Band e as limitações conhecidas. Deve ser
atualizado sempre que um novo firmware, callback ou comportamento físico seja
confirmado.

## Resumo do modelo

O histórico apresentado por uma aplicação não corresponde necessariamente ao
que ainda está guardado na pulseira:

1. a MF91 conserva uma janela curta de dados;
2. cada aplicação descarrega essa janela enquanto a pulseira está ligada;
3. a aplicação guarda localmente os dados recebidos;
4. dias antigos são depois apresentados a partir do arquivo do telemóvel;
5. uma nova leitura da pulseira é unida ao arquivo existente sem duplicar o
   mesmo instante.

Por este motivo, dois telemóveis associados à mesma pulseira podem apresentar
históricos diferentes. Cada um conhece apenas os dados que sincronizou ou que
recebeu por um serviço de conta próprio.

## Retenção da pulseira

No firmware ensaiado da MF91, o SDK Android reportou `getWathcDay() == 3`. O SDK
iOS expõe o equivalente em `VPPeripheralModel.saveDays`.

Estes valores são tratados como o deslocamento máximo aceite pelo SDK:

- `0`: hoje;
- `1`: ontem;
- `2`: anteontem;
- `3`: três dias antes.

Assim, a pulseira ensaiada disponibiliza hoje e até aos três dias anteriores.
O valor não está fixo na camada Angular: as bridges publicam
`historyRetentionDays`, para que outro firmware possa indicar uma janela
diferente.

O Android rejeita pedidos cujo deslocamento seja superior ao valor reportado
com `HISTORY_DATE_OUTSIDE_DEVICE_RETENTION`. O iOS usa a mesma restrição para
leituras diretas de passos. Para datas mais antigas, a bridge iOS pode consultar
a base local Veepoo sem voltar a pedir esse dia à pulseira.

## Evidência observada na G Band

A G Band 2.1.17 (`com.vpgband.app`) foi observada no Android de ensaio com a
MF91 `1B:F0:06:E2:86:FC`.

Com a pulseira desligada da G Band:

- o calendário assinalou 8 e 11 de junho como dias com dados;
- foi possível abrir 8 de junho;
- o detalhe de frequência cardíaca mostrou média diária de 71 bpm, mínimo de
  56 bpm e máximo de 93 bpm.

Como 8 de junho estava muito além da retenção da pulseira, essa apresentação
veio do arquivo da aplicação no Android.

A análise estática do APK confirmou:

- Room/SQLite para persistência local;
- dados diários de atividade identificados por conta, MAC e data;
- medições manuais identificadas por conta, MAC e timestamp;
- consultas por MAC e data;
- operações equivalentes a `INSERT OR REPLACE`, que atualizam um registo
  existente em vez de criar duplicados.

O APK também contém componentes de comunicação remota. Isto não prova que o
histórico observado tenha sido restaurado por cloud; o cenário ensaiado e as
consultas de interface confirmam apenas que a apresentação usa a base local.

Os dados privados da G Band não são importados pela demonstração. O isolamento
de aplicações do Android e do iOS impede o acesso normal à base de outra
aplicação.

## Arquivo da H Band SDK Demo

A demonstração usa IndexedDB, disponível na WebView do Capacitor em Android e
iOS, com:

- base: `hband-history`;
- versão atual: `2`;
- store: `history-days`;
- chave: `[deviceId, metric, date]`;
- índice `device-date`, para abrir todas as métricas de um dia;
- índice `device-id`, para listar os dias existentes no calendário.

O evento integral é conservado, incluindo:

- valores normalizados;
- registos com timestamp;
- amostras, incluindo ECG;
- payload bruto quando disponibilizado pela bridge;
- data da última atualização local.

### União e deduplicação

Dentro da mesma pulseira, métrica e data:

1. o timestamp identifica o registo;
2. um callback posterior atualiza os valores desse timestamp;
3. amostras novas substituem as anteriores quando estão presentes;
4. timestamps diferentes são mantidos e ordenados cronologicamente;
5. eventos vazios não apagam dados já arquivados.

Esta estratégia corresponde ao comportamento de upsert observado na G Band e
permite juntar dados automáticos e medições manuais.

### Isolamento e duração

- Arquivos de pulseiras diferentes nunca são misturados.
- O identificador do último arquivo é conservado mesmo após uma desconexão
  voluntária, sem conservar por isso a password de sessão.
- O arquivo sobrevive ao fecho e às atualizações normais da aplicação.
- Desinstalar a aplicação, limpar os respetivos dados ou perder a WebView
  elimina o arquivo.
- Não existe atualmente sincronização cloud nem transferência entre
  telemóveis.

Uma futura sincronização remota necessita de um contrato de API confirmado,
incluindo identidade da pessoa, identidade da pulseira, conflitos, paginação,
eliminação e proteção de dados de saúde. Esses contratos não devem ser
inventados pela aplicação.

## Sequência de sincronização Android

Todos os comandos BLE passam pela mesma fila. Ao ligar e autenticar:

1. lê a bateria;
2. sincroniza a hora;
3. lê a atividade atual;
4. percorre hoje e cada deslocamento até `historyRetentionDays`;
5. executa `history.daily` para os blocos automáticos;
6. executa `history.manual.daily` para as medições manuais suportadas;
7. usa leitores dedicados para ECG e composição corporal.

`history.daily` descarrega uma vez `readOriginDataSingleDay` e distribui os
blocos pelas métricas:

- passos;
- frequência cardíaca;
- pressão arterial;
- oxigénio;
- temperatura;
- glicemia;
- stress.

`history.manual.daily` pede numa única operação as tabelas manuais suportadas e
volta a distribuir os resultados pelas mesmas métricas. ECG e composição
corporal permanecem separados porque não fazem parte do bloco diário comum.

O serviço foreground Android mantém uma notificação `connectedDevice`. Perdas
não intencionais iniciam uma tentativa de reconexão ao mesmo MAC. Uma
desconexão pedida pelo utilizador cancela esse comportamento.

## Sequência de sincronização iOS

No iOS, a bridge:

1. sincroniza `AllData`;
2. sincroniza oxigénio quando a área dedicada é suportada;
3. sincroniza temperatura dedicada quando o tipo do firmware assim o exige;
4. executa estas leituras em série;
5. consulta `VPDataBaseOperation` por MAC, métrica e data;
6. emite o mesmo contrato de histórico usado pelo Android.

Depois da primeira atualização Veepoo, os restantes dias retidos são copiados
da base nativa para o arquivo Angular sem repetir o download BLE completo.
Datas mais antigas podem igualmente ser consultadas na base Veepoo e unidas ao
arquivo da aplicação.

## Calendário e estados apresentados

O seletor usa `ion-datetime`. Os dias devolvidos pelo índice `device-id` são
destacados com a legenda **Dia com medições guardadas**.

O estado abaixo do seletor distingue:

- arquivo local;
- dados recebidos da pulseira;
- arquivo local atualizado pela pulseira;
- consulta offline;
- data fora da retenção;
- sincronização em curso;
- atualização parcial ou com erro.

Mudar a data carrega primeiro o arquivo local. Se a pulseira estiver ligada e a
data ainda estiver na retenção, a atualização BLE é iniciada automaticamente.

## Limitações e validação

- O valor de três dias foi confirmado na MF91 ensaiada; não deve ser assumido
  para todos os modelos ou firmwares.
- Uma instalação nova não recupera automaticamente o histórico privado da
  G Band.
- Builds e testes simulados não confirmam callbacks físicos.
- Os valores do SDK não constituem diagnóstico médico.
- A exatidão clínica das métricas não foi avaliada.
- Uma sincronização interrompida conserva os dados que já tinham sido
  arquivados e pode ser retomada na ligação seguinte.

Os ensaios físicos devem continuar a ser registados em
[`MF91_PHYSICAL_TEST_PLAN.md`](MF91_PHYSICAL_TEST_PLAN.md), e diferenças
funcionais observadas na G Band em
[`G_BAND_INTEROPERABILITY.md`](G_BAND_INTEROPERABILITY.md).
