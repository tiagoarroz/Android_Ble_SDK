# Cobertura dos SDKs

Estado desta versão da aplicação:

| Grupo | Interface | Android | iOS |
| --- | --- | --- | --- |
| Pesquisa, ligação, password e desligar | Completa | Bridge direto | Bridge direto |
| Bateria, RSSI e sincronização de hora | Completa | Bridge direto | Bridge direto |
| Perfil pessoal | Formulário explícito | Bridge direto | Bridge direto |
| Ritmo cardíaco | Série/valor | Iniciar/parar | Iniciar/parar |
| Pressão arterial | Barras/progresso | Iniciar/parar | Iniciar/parar |
| SpO₂ | Medidor/série | Iniciar/parar | Iniciar/parar |
| Respiração | Série/progresso | Iniciar/parar | Iniciar/parar |
| Temperatura | Série/valores | Iniciar/parar | Iniciar/parar |
| HRV | Série/valor | Iniciar/parar | Iniciar/parar |
| Fadiga e stress | Medidor/progresso | Iniciar/parar | Iniciar/parar |
| Glicose | Série/valor | Iniciar/parar | Iniciar/parar |
| GSR | Série/progresso | Iniciar/parar | Iniciar/parar |
| ECG | Série/progresso | Catalogado | Iniciar/parar |
| Composição corporal e sanguínea | Barras/progresso | Catalogado | Iniciar/parar |
| Microexame | Progresso/resultado | Catalogado | Iniciar |
| Atividade do dia | Barras | Leitura direta | Catalogado |
| Sono e históricos clínicos | Sono/tabela | Leitura direta para sono, dados brutos, SpO₂, HRV e temperatura | Catalogado |
| Automação, alarmes e lembretes | Cronologia/tabela | Catalogado | Catalogado |
| Ecrã, idioma, unidades e procura | Tabela/estado | Catalogado | Catalogado |
| Câmara, notificações, música e tempo | Eventos/cronologia | Catalogado | Catalogado |
| Contactos, SOS, relógios e contagem | Tabela/cronologia | Catalogado | Catalogado |
| Mostradores, GPS/GNSS/AGPS e OTA | Tabela/eventos | Catalogado e bloqueado | Catalogado e bloqueado |
| Sensores brutos e projetos especiais | Série/eventos | Catalogado | Catalogado |
| BT clássico, 4G, IA e conteúdo | Eventos | Catalogado | Catalogado |

## Significado dos estados

- **Bridge direto**: existe implementação nativa compilada e os callbacks são
  normalizados como eventos Capacitor.
- **Catalogado**: a função aparece no módulo adequado com descrição,
  visualização prevista e identificador de operação, mas não é executável no
  bridge desta versão.
- **Bloqueado**: função potencialmente destrutiva ou dependente de um ficheiro,
  serviço ou payload que não foi fornecido. A aplicação não inventa esses
  contratos.

Após autenticação, a aplicação sobrepõe as capacidades declaradas pelo
firmware. Uma capacidade `unsupported` bloqueia todos os comandos desse módulo;
`unknown` não é apresentado como confirmação de suporte.
