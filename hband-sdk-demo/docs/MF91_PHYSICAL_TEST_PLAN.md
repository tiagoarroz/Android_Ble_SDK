# Plano de validação física da MF91

## Preparação

- usar uma MF91 com bateria suficiente e sem ligação ativa à aplicação H Band;
- registar versão de firmware, sistema operativo e modelo do telefone;
- manter uma segunda forma de recuperar/atualizar a pulseira antes de qualquer
  ensaio futuro de OTA;
- usar um perfil de teste conhecido e consentido.

## Ligação

- confirmar pedidos de permissão em Android 11, Android 12+ e iOS;
- procurar durante 12 segundos e confirmar nome, endereço/identificador e RSSI;
- testar password correta e incorreta;
- desligar e voltar a ligar sem reiniciar a aplicação;
- confirmar que nenhum comando é enviado antes da autenticação.

## Dados

Para cada capacidade que a MF91 declarar como suportada:

1. iniciar apenas uma medição;
2. verificar progresso e valor recebido;
3. parar explicitamente a medição;
4. comparar o valor com o apresentado pela aplicação H Band oficial;
5. repetir após desligar/voltar a ligar;
6. confirmar que um módulo não suportado permanece bloqueado.

Validar separadamente ritmo cardíaco, pressão arterial, SpO₂, respiração,
temperatura, fadiga, stress, glicose e GSR. ECG, composição corporal,
composição sanguínea e microexame só devem ser ensaiados se o firmware os
declarar e se a MF91 tiver os contactos/sensores necessários.

## Histórico e robustez

- ler atividade, sono e históricos após existirem dados reais guardados;
- interromper uma leitura com desligamento Bluetooth e confirmar recuperação;
- colocar a aplicação em background e regressar;
- confirmar que a fila não inicia duas operações demoradas em paralelo;
- recolher logs nativos e capturas da interface para cada falha.

## Registo NFC de pulseira próxima

Com duas MF91 ligadas e sem sessão ativa na aplicação:

- confirmar que a pulseira escolhida é a que está mais perto do telemóvel;
- confirmar a vibração e o ecrã aceso apenas nessa pulseira;
- escrever a etiqueta e voltar a lê-la com «Ligar por NFC»;
- confirmar que a aplicação continua sem sessão depois do registo, sem
  notificação de serviço nem sincronização de histórico;
- repetir o registo numa segunda pulseira sem reiniciar a aplicação;
- cancelar a operação durante a pesquisa e durante a espera pela etiqueta,
  confirmando que a pulseira deixa de vibrar e fica desligada;
- registar o comportamento quando o firmware não confirma a vibração.

## Critério de conclusão

Um fluxo só é considerado validado quando existe evidência no dispositivo
físico: pedido aceite, callback final, valor visível e encerramento limpo.
Build web, compilação Android/iOS e simulação não substituem esta evidência.
