# Decisoes tecnicas

## Contexto

Este servico, `gubee-stock-reconciliation`, tem como objetivo receber eventos de estoque e pedidos, manter uma visao atual confiavel do estoque por conta e SKU, e permitir auditoria das alteracoes que levaram ao saldo atual.

A solucao sera implementada em Java com Spring Boot, MySQL, Kafka real em Docker, arquitetura hexagonal, Testcontainers, logs estruturados, OpenAPI/Swagger e documentacao operacional clara.

## Objetivos de arquitetura

- Preservar todos os eventos recebidos para auditoria e reprocessamento.
- Manter uma projecao atual de estoque para consultas rapidas.
- Garantir idempotencia por evento recebido.
- Reduzir risco de inconsistencias em concorrencia.
- Isolar regra de negocio de frameworks e infraestrutura.
- Tornar as decisoes tecnicas claras, testaveis e justificaveis.

## Arquitetura hexagonal

A aplicacao sera organizada em torno do dominio, com adaptadores para entrada e saida.

Estrutura proposta:

```text
com.gubee.stockreconciliation
  domain
    model
    event
    policy
    exception
    port
  application
    usecase
    service
  adapter
    in
      web
      kafka
    out
      persistence
      messaging
  config
  shared
```

O dominio nao deve depender de Spring, JPA, Kafka, MySQL ou detalhes HTTP. Ele deve representar as regras de estoque, idempotencia, duplicidade logica, auditoria e aplicacao de eventos.

Adaptadores de entrada:

- REST, para submissao manual/testavel de eventos e consulta de estoque/historico.
- Kafka consumer, para processamento assincrono dos eventos reais.

Adaptadores de saida:

- MySQL/JPA, para eventos, projecoes e auditoria.
- Kafka producer, caso seja necessario publicar eventos processados ou falhas tecnicas em topicos especificos.

## Fonte da verdade

A fonte da verdade adotada sera o ledger de eventos persistidos, nao apenas a tabela de saldo atual.

A tabela de saldo atual sera tratada como uma projecao derivada para leitura eficiente. Em caso de divergencia, a projecao pode ser reconstruida a partir dos eventos persistidos.

Motivos:

- Permite explicar como o saldo atual foi formado.
- Facilita auditoria.
- Permite reprocessamento apos correcao de regra.
- Evita depender exclusivamente do ultimo estado gravado.

Trade-off:

- A solucao fica mais complexa do que um CRUD simples de estoque.
- Reprocessar eventos exige cuidado com ordenacao, idempotencia e efeitos colaterais.

## Chave de estoque

A chave inicial do saldo de estoque sera:

```text
accountId + sku
```

O `marketplace` sera mantido nos eventos e no historico, mas nao fara parte da chave canonica inicial do saldo fisico.

Justificativa:

- O desafio pede visao atual por conta e SKU.
- O mesmo SKU em contas diferentes nao pode se afetar.
- O estoque fisico disponivel normalmente pertence ao seller/conta, enquanto marketplaces representam canais de venda ou sincronizacao.

Trade-off:

- Caso a operacao real exija estoque segregado por marketplace, anuncio, fulfillment ou deposito, sera necessario evoluir a chave para incluir essas dimensoes.
- Para uma versao de producao, eu avaliaria criar projecoes adicionais por marketplace/anuncio sem abandonar a visao canonica por conta e SKU.

## Tipos de evento

Eventos inicialmente suportados:

- `STOCK_ADJUSTED`: define o saldo disponivel absoluto para uma conta e SKU.
- `ORDER_CREATED`: reduz o saldo disponivel conforme a quantidade vendida.
- `ORDER_CANCELLED`: devolve estoque de um pedido previamente criado.
- `STOCK_SYNC_SENT`: registra envio de saldo ao marketplace, sem alterar o saldo canonico.
- `MARKETPLACE_STOCK_RESTORED`: registra recomposicao feita pelo marketplace. Por padrao, nao altera automaticamente o saldo canonico sem regra explicita.

Decisao importante:

Eventos de sincronizacao e recomposicao de marketplace serao auditaveis, mas nao devem sobrescrever automaticamente o saldo canonico sem uma politica clara. Isso evita que um canal externo altere o estoque fisico sem reconciliacao.

## Idempotencia

A idempotencia tecnica sera garantida por `eventId`.

Cada evento recebido sera persistido com uma restricao unica por `eventId`. Caso o mesmo evento seja recebido novamente, ele sera reconhecido como duplicado e nao sera aplicado de novo ao estoque.

Comportamento esperado:

- Mesmo `eventId` e mesmo payload: retornar/processar como duplicado idempotente.
- Mesmo `eventId` e payload divergente: registrar erro de consistencia, pois indica reutilizacao indevida de identificador.

## Duplicidade logica

Alem da idempotencia por `eventId`, existe duplicidade logica de negocio. Exemplo: dois eventos diferentes representando o mesmo pedido.

Para pedidos, a chave logica sera:

```text
accountId + marketplace + externalOrderId + sku
```

Comportamento:

- `ORDER_CREATED` para a mesma chave logica nao deve baixar estoque duas vezes.
- `ORDER_CANCELLED` so deve devolver estoque se houver pedido criado correspondente ainda nao cancelado.

Trade-off:

- Essa decisao assume que `externalOrderId` identifica de forma estavel o pedido dentro do marketplace.
- Se um pedido tiver multiplos itens do mesmo SKU em eventos separados, sera necessario modelar linha de pedido ou item externo com uma chave mais granular.

## Eventos fora de ordem

Eventos podem chegar fora de ordem. A aplicacao deve persistir todos os eventos aceitos e aplicar o saldo considerando `occurredAt`.

Estrategia inicial:

1. Persistir o evento recebido.
2. Identificar o agregado afetado: `accountId + sku`.
3. Recalcular a projecao desse agregado a partir dos eventos persistidos, ordenados por `occurredAt` e, em caso de empate, por ordem de recebimento.

Justificativa:

- Esta abordagem simplifica a consistencia do saldo para um desafio tecnico.
- Torna o comportamento deterministico e facil de testar.
- Evita regras frágeis baseadas somente na ordem de chegada.

Trade-off:

- Recalcular o agregado a cada evento pode ter custo maior com alto volume.
- Em producao, eu avaliaria snapshots, particionamento por agregado, compactacao de historico e processamento incremental com controle de versao.

## Concorrencia

Dois eventos podem afetar o mesmo SKU ao mesmo tempo. A solucao deve evitar atualizacoes perdidas e saldo negativo acidental.

Estrategia inicial:

- Processamento transacional por evento.
- Lock por agregado `accountId + sku` durante o recálculo/aplicacao da projecao.
- Restricoes unicas para idempotencia e duplicidade logica.
- Versionamento da projecao de estoque com optimistic locking quando aplicavel.

Em MySQL, o lock pode ser implementado com uma linha de projecao de estoque bloqueada via `SELECT ... FOR UPDATE` ou por mecanismo equivalente via JPA com lock pessimista.

Trade-off:

- Locks por agregado reduzem paralelismo para o mesmo SKU.
- Em compensacao, mantem alta concorrencia entre SKUs e contas diferentes.

## Estoque negativo

A regra padrao sera nao permitir saldo negativo acidentalmente.

Eventos relativos, como `ORDER_CREATED`, nao devem levar o saldo abaixo de zero. Caso isso ocorra, o evento deve ser registrado com status de rejeicao de negocio ou falha de reconciliacao, preservando rastreabilidade.

Eventos absolutos, como `STOCK_ADJUSTED`, podem corrigir o saldo para um valor valido informado pelo cliente/sistema de origem.

Decisao:

- Saldo negativo nao sera permitido por padrao.
- Se um cenario real exigir backorder ou venda sem estoque, isso devera virar uma regra explicita e documentada.

## Auditoria e rastreabilidade

A aplicacao deve permitir explicar por que o estoque atual chegou ao valor apresentado.

Serao mantidos:

- Evento bruto recebido.
- Status de processamento do evento.
- Movimento gerado no estoque, quando aplicavel.
- Saldo anterior e saldo posterior.
- Motivo ou tipo de alteracao.
- Data de ocorrencia (`occurredAt`) e data de recebimento/processamento.
- Correlation id para rastreamento entre logs, API e Kafka.

Consultas previstas:

- Saldo atual por `accountId + sku`.
- Historico de movimentos por `accountId + sku`.
- Detalhe de processamento por `eventId`.

## Kafka

Kafka sera usado como canal real de entrada de eventos.

Topicos propostos:

- `stock-events`: eventos recebidos para processamento.
- `stock-events-dlt`: eventos que falharam por erro tecnico apos retentativas.

Decisoes:

- A chave da mensagem Kafka deve ser `accountId:sku`, favorecendo ordenacao por agregado dentro da mesma particao.
- O consumer deve ser idempotente, pois Kafka trabalha com entrega ao menos uma vez em configuracoes comuns.
- Erros de negocio devem ser persistidos como status do evento, nao necessariamente enviados para DLT.
- DLT deve ser reservada principalmente para erros tecnicos ou payloads invalidos que nao puderam ser processados.

## API REST e OpenAPI

A API REST tera dois objetivos:

- Facilitar testes e demonstracao do desafio.
- Expor consultas de saldo e historico.

Endpoints iniciais:

```text
POST /api/v1/events
GET  /api/v1/stocks/{accountId}/{sku}
GET  /api/v1/stocks/{accountId}/{sku}/history
GET  /api/v1/events/{eventId}
```

O contrato sera documentado via OpenAPI/Swagger usando Springdoc.

Erros HTTP serao padronizados com `ProblemDetail`, incluindo campos como codigo, mensagem, detalhe e correlation id.

## Persistencia

Banco principal: MySQL.

Tabelas conceituais:

- `stock_events`: ledger dos eventos recebidos.
- `stock_projections`: saldo atual por `accountId + sku`.
- `stock_movements`: movimentos aplicados ao saldo.
- `order_reservations`: controle logico de pedidos criados/cancelados.

Flyway sera usado para versionamento de schema.

Indices importantes:

- `stock_events.event_id` unico.
- `stock_events.account_id, stock_events.sku, stock_events.occurred_at`.
- `stock_projections.account_id, stock_projections.sku` unico.
- `order_reservations.account_id, marketplace, external_order_id, sku` unico.

## Logs estruturados

Logs serao emitidos em JSON, com campos consistentes:

- `timestamp`
- `level`
- `message`
- `correlationId`
- `eventId`
- `accountId`
- `sku`
- `eventType`
- `processingStatus`

Nao devem ser logados segredos, credenciais, tokens ou payloads sensiveis sem necessidade.

## Seguranca

Mesmo sendo um desafio tecnico, a aplicacao deve seguir boas praticas basicas:

- Validacao de entrada com Bean Validation.
- Limites de tamanho para payloads.
- Nao expor stack traces em respostas HTTP.
- Nao versionar credenciais reais.
- Configuracoes por variaveis de ambiente.
- Usuario de banco com privilegios minimos necessarios.
- Logs sem dados sensiveis.
- Actuator restrito ao necessario.

Autenticacao/autorizacao podem ser deixadas fora do escopo inicial se o desafio nao exigir, mas em producao eu adicionaria OAuth2/JWT ou integracao com gateway/API management.

## Testes

Tipos de teste previstos:

- Testes unitarios do dominio, sem Spring.
- Testes de aplicacao para casos de uso.
- Testes de integracao com MySQL via Testcontainers.
- Testes de integracao com Kafka via Testcontainers.
- Testes REST com MockMvc ou WebTestClient.

Cenarios minimos:

- `STOCK_ADJUSTED available=10` resulta em saldo 10.
- `ORDER_CREATED quantity=2` apos saldo 10 resulta em saldo 8.
- `ORDER_CANCELLED quantity=2` devolve estoque corretamente.
- Evento duplicado por `eventId` nao altera saldo duas vezes.
- Pedido duplicado com outro `eventId` nao baixa estoque duas vezes.
- Eventos fora de ordem mantem saldo deterministico.
- Contas diferentes com mesmo SKU nao se afetam.
- Concorrencia no mesmo SKU nao gera atualizacao perdida.
- Tentativa de baixar estoque abaixo de zero e rejeitada.

## Observabilidade operacional

A aplicacao deve expor:

- Health checks via Spring Actuator.
- Readiness/liveness para banco e Kafka.
- Logs estruturados.
- Metricas basicas de eventos processados, rejeitados e duplicados.

Em producao, eu adicionaria tracing distribuido com OpenTelemetry.

## Trade-offs assumidos

- Recalculo por agregado simplifica corretude, mas pode ser menos eficiente em alto volume.
- Chave canonica `accountId + sku` atende ao desafio, mas pode precisar evoluir para marketplace/anuncio/deposito.
- Kafka sera usado como entrada real, mas REST sera mantido para demonstracao e testes manuais.
- Autenticacao pode ficar fora do escopo inicial para focar consistencia e arquitetura, desde que isso esteja claro.
- Eventos de marketplace serao inicialmente auditaveis, nao autoritativos sobre o estoque canonico.

## O que faria diferente em producao

- Avaliaria particionamento e sharding por conta/SKU em cenarios de alto volume.
- Criaria snapshots de estoque para reduzir custo de reprocessamento.
- Adicionaria controle de schema de eventos com Schema Registry.
- Implementaria DLQ operacional com painel de reprocessamento.
- Adicionaria autenticacao forte, autorizacao por conta e auditoria de acesso.
- Usaria tracing distribuido e metricas com dashboards.
- Validaria regras especificas por marketplace e por tipo de integracao.
- Consideraria separar leitura e escrita com CQRS se o volume justificar.

