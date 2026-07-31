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

1. o timestamp e a origem identificam o registo;
2. um callback posterior atualiza os valores desse timestamp;
3. amostras novas substituem as anteriores quando estão presentes;
4. timestamps diferentes são mantidos e ordenados cronologicamente;
5. eventos vazios não apagam dados já arquivados.

Esta estratégia corresponde ao comportamento de upsert observado na G Band e
permite juntar dados automáticos e medições manuais.

### Origem das leituras

Cada registo persistido conserva `source: automatic | manual`. A classificação
é atribuída antes de os dados atravessarem a bridge Capacitor:

- `history.daily` e a atividade por passos são blocos da monitorização
  automática;
- os leitores dedicados de ECG e composição corporal são medições manuais;
- uma medição em tempo real só recebe a origem manual depois de a pessoa
  confirmar que a quer guardar.

O campo faz parte da chave de deduplicação. Por isso, uma leitura automática e
uma medição manual com o mesmo timestamp não se substituem. Registos locais
criados por versões anteriores, sem origem, são apresentados como automáticos,
exceto ECG e composição corporal, que pertencem às tabelas manuais do SDK.

Na interface, o seletor de dia existe apenas dentro da métrica. A vista
`Todos os dados` conserva o dia escolhido e apresenta listas separadas para
monitorização automática e medições manuais. Fechar a métrica repõe o dia atual
no estado da aplicação.

### Medições manuais confirmadas

Uma medição em tempo real não é arquivada automaticamente. Depois de a leitura
terminar, a aplicação pede confirmação e só então cria um registo histórico:

- a pulseira e a métrica mantêm o isolamento já descrito;
- o timestamp real do callback determina a data do arquivo;
- valores, amostras e payload bruto disponíveis são conservados;
- a união por timestamp impede que a mesma confirmação seja duplicada;
- recusar a gravação não altera o histórico diário;
- um callback que contenha apenas estado ou progresso não é tratado como um
  resultado guardável.

Esta regra impede que uma medição atual seja associada ao dia antigo que a
pessoa pudesse estar a consultar quando iniciou a leitura.

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
4. executa `history.daily` para os blocos automáticos do dia;
5. usa o leitor dedicado de ECG, quando suportado;
6. usa o leitor dedicado de composição corporal, quando suportado;
7. repete apenas as leituras históricas para os restantes dias dentro de
   `historyRetentionDays`.

Na interface, a sincronização de hoje tem seis operações quando ECG e
composição corporal são suportados. A repetição dos dias anteriores é executada
em segundo plano e não volta a enviar bateria, hora ou atividade atual.

`history.daily` descarrega uma vez `readOriginDataSingleDay` e distribui os
blocos pelas métricas:

- passos;
- frequência cardíaca;
- pressão arterial;
- oxigénio;
- temperatura;
- glicemia;
- stress.

ECG e composição corporal permanecem separados porque não fazem parte do bloco
diário comum.

### Incidente do quinto passo anterior

A primeira implementação apresentava sete leituras individuais: bateria, hora,
atividade atual, histórico diário, histórico manual, ECG e composição corporal.
No ensaio físico de 31 de julho de 2026, a quinta operação
`history.manual.daily` recebeu a confirmação de escrita BLE, mas o SDK não
chamou `onReadComplete` nem `onReadFail`. A fila só foi libertada pelo timeout de
45 segundos.

Este comportamento coincide com a documentação oficial da versão incluída:
`readDeviceManualData` só suporta atualmente pressão arterial quando o
dispositivo tem pressão pneumática (`isSupportBumpBp`). Os restantes tipos
manuais ainda não são suportados por esse leitor. A MF91 estava a receber um
pedido agrupado com frequência cardíaca, pressão, oxigénio, temperatura,
glicemia e stress, pelo que não existia um callback final garantido.

`history.manual.daily` foi retirado da sincronização geral. Os dados destas
métricas continuam a vir de `history.daily`; um eventual leitor manual de
pressão pneumática só deve ser ativado para um dispositivo cuja capacidade
específica seja confirmada pelo SDK.

No ensaio seguinte, o leitor dedicado de ECG também não terminou. A bridge
estava a combinar `EEcgDataType.ALL` com a data do dia selecionado. O contrato
documentado exige `TimeData(0, 0, 0, 0, 0, 0)` quando o tipo é `ALL`.

Mesmo com o parâmetro corrigido, a MF91 respondeu `04`, que o binário desta
versão processa como ausência de IDs e entrega internamente através de
`readIdFinish(null)`. O método de conveniência `readECGData` não encaminha esse
caso para `readDataFinish`, deixando o pedido pendente. A bridge passou a usar
explicitamente `readECGId`; IDs nulos concluem com um histórico vazio e IDs
existentes são descarregados por `readECGManuallyData`. O dia solicitado é
filtrado depois de receber os registos.

O serviço foreground Android mantém uma notificação `connectedDevice`. Perdas
não intencionais iniciam uma tentativa de reconexão ao mesmo MAC. Uma
desconexão pedida pelo utilizador cancela esse comportamento.

### Timeouts e recuperação da fila

A Promise devolvida pela bridge não pode ficar pendente indefinidamente quando
o SDK não entrega o callback esperado:

- bateria, hora, início/fim de medição e restantes comandos de controlo têm um
  timeout de 10 segundos;
- leituras de histórico, que transferem mais dados, têm um timeout de 45
  segundos;
- ao expirar, a operação falha com `SDK_OPERATION_TIMEOUT`;
- `busyOperation` é sempre libertado, voltando a disponibilizar os controlos;
- a sincronização é marcada como parcial/erro e não envia os comandos seguintes
  dessa sequência;
- uma resolução tardia da Promise nativa é ignorada pelo estado da operação que
  já expirou.

Quando isto acontece, a barra deixa de parecer ativa e o painel apresenta:

- `Não foi possível atualizar este dia`, ou a variante que preserva os dados
  locais já existentes;
- quantas leituras foram processadas;
- qual a leitura que falhou;
- uma indicação explícita quando a causa foi o tempo de resposta esgotado.

Parar a sequência no primeiro timeout é importante: sem confirmação de fim não
é seguro assumir que a pulseira já está pronta para outro comando de histórico.
Uma nova sincronização pode ser iniciada numa interação posterior ou na próxima
ligação.

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
