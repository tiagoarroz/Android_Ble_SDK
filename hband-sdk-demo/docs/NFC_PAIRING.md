# Registo e ligação de pulseiras por NFC

## Formato guardado

A aplicação escreve uma mensagem NDEF com um único registo RTD Text. O texto
tem este contrato versionado:

```text
HITECOSYSTEM-HBAND|1|1B:F0:06:E2:86:FC
```

- `HITECOSYSTEM-HBAND` identifica inequivocamente uma pulseira desta aplicação;
- `1` é a versão do contrato;
- o último campo é o MAC Address exato apresentado pelo SDK H Band.

A leitura só aceita três campos, o marcador e a versão conhecidos e um MAC com
seis pares hexadecimais separados por `:`. O valor capturado não é normalizado
nem substituído antes da pesquisa Bluetooth.

## Registar NFC

O botão só aparece quando há uma pulseira autenticada. A app abre uma sessão
NFC isolada, substitui a mensagem NDEF da tag e termina a sessão. Tags apenas de
leitura, demasiado pequenas ou sem suporte NDEF produzem um erro explícito.

## Registar NFC de uma pulseira próxima

Este botão fica sob o painel da pulseira e existe apenas quando não há sessão
ativa. Serve para preparar etiquetas de pulseiras que não pertencem à sessão da
aplicação:

1. pede as permissões Bluetooth normais da aplicação;
2. recolhe anúncios BLE durante seis segundos e escolhe a pulseira com o RSSI
   mais forte dessa janela;
3. liga e autentica essa pulseira numa sessão temporária;
4. envia o comando de procura, que faz a pulseira vibrar e acender o ecrã;
5. apresenta o nome, o endereço e o sinal, pedindo a pulseira que vibrou;
6. escreve a etiqueta com o mesmo contrato NDEF descrito acima;
7. para a vibração e termina a sessão temporária.

O passo 7 é executado em qualquer desfecho, incluindo cancelamento, tag
inválida ou falha de escrita, para não deixar a pulseira ligada nem a vibrar.

### Sessão temporária

Fazer vibrar uma pulseira exige um comando escrito no canal GATT, pelo que a
ligação e a autenticação são inevitáveis. Essa sessão é deliberadamente
separada da sessão normal:

- não publica o estado `connected` nem preenche o dispositivo do estado;
- não inicia o serviço foreground Android nem a religação automática;
- não guarda a sessão em `localStorage` nem muda o arquivo de histórico ativo;
- não substitui as capacidades nem a retenção publicadas pela sessão atual;
- é recusada com `PROVISIONING_SESSION_BUSY` enquanto existir uma pulseira
  ligada, porque o SDK mantém uma única ligação.

A autenticação usa o mesmo `confirmDevicePwd` da sessão normal, que confirma
também o formato de hora de 24 horas por ser um parâmetro obrigatório desse
contrato. A password continua a ser a password local do fluxo BLE existente.

### Pulseiras sem vibração

O comando é enviado mesmo quando a capacidade `findDevice` não foi anunciada,
porque o próprio SDK responde a indicar que a função não é suportada. Quando
não chega nenhuma confirmação de vibração em dois segundos e meio, a sessão
continua utilizável e a interface passa a pedir a confirmação pelo nome e pelo
endereço apresentados em vez de pedir a pulseira que vibrou.

Contratos usados:

- Android: `startFindDeviceByPhone` / `stopFindDeviceByPhone` com
  `IFindDevicelistener`;
- iOS: `veepooSDK_searchDeviceFuntionWithState:result:` com
  `VPSearchDeviceFunctionState`.

## Ligar por NFC

O botão só aparece sem uma pulseira ligada. Depois de ler e validar a tag, a app:

1. pede as permissões Bluetooth normais da aplicação;
2. inicia uma pesquisa BLE;
3. aguarda até 12 segundos pelo endereço exato;
4. termina a pesquisa;
5. autentica a pulseira encontrada através do fluxo H Band existente.

Não encontrar o MAC é apresentado como pulseira fora de alcance ou ainda ligada
a outra aplicação. A app não tenta ligar a outro dispositivo com nome semelhante.

## Plataformas

- Android usa `NfcAdapter` em reader mode e suporta tags NDEF já formatadas ou
  `NdefFormatable`.
- iOS usa `NFCNDEFReaderSession`, a capacidade `Near Field Communication Tag
  Reading` e o formato NDEF. O alerta de sistema apresenta o texto no idioma
  atualmente selecionado na aplicação.

O NFC transporta apenas o identificador Bluetooth, sem medições, credenciais ou
dados pessoais. A password de autenticação continua a ser tratada localmente
pelo fluxo BLE existente.

## Limites de validação

- A escolha pelo RSSI usa o último anúncio recebido de cada pulseira dentro da
  janela; duas pulseiras à mesma distância podem alternar entre ensaios, e a
  vibração é precisamente o que permite confirmar qual foi escolhida.
- A compilação das bridges confirma os contratos do SDK incluído; a resposta do
  firmware da MF91 ao comando de procura só é confirmada numa pulseira física.
- Uma pulseira ligada a outra aplicação não aceita a sessão temporária.
