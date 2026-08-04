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
