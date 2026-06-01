# Decisões técnicas

## Contexto

Este serviço, `gubee-stock-reconciliation`, tem como objetivo receber eventos de estoque e pedidos, manter uma visão atual confiável do estoque por conta e SKU e permitir auditoria das alterações que levaram ao saldo atual.

A solução foi implementada em Java com Spring Boot, MySQL, Kafka real em Docker, arquitetura hexagonal, Testcontainers, logs estruturados, OpenAPI/Swagger e documentação operacional clara.

## Objetivos de arquitetura

- Preservar todos os eventos recebidos para auditoria e reprocessamento.
- Manter uma projeção atual de estoque para consultas rápidas.
- Garantir idempotência por evento recebido.
- Reduzir risco de inconsistências em concorrência.
- Isolar regra de negócio de frameworks e infraestrutura.
- Tornar as decisões técnicas claras, testáveis e justificáveis.

## Arquitetura hexagonal

A aplicação será organizada em torno do domínio, com adaptadores para entrada e saída.

Estrutura adotada:

```text
com.gubee.stockreconciliation
  domain
    model
    event
    policy
    exception
    port
      in
      out
  application
    usecase
    service
  adapter
    in
      web
      kafka
    out
      persistence
      observability
      messaging
  config
  shared
```

O domínio não deve depender de Spring, JPA, Kafka, MySQL ou detalhes HTTP. Ele deve representar as regras de estoque, idempotência, duplicidade lógica, auditoria e aplicação de eventos.

Adaptadores de entrada:

- REST, para submissão manual/testável de eventos e consulta de estoque/histórico.
- Kafka consumer, para processamento assíncrono dos eventos reais.

Adaptadores de saída:

- MySQL/JPA, para eventos, projeções e auditoria.
- Kafka producer, caso seja necessário publicar eventos processados ou falhas técnicas em tópicos específicos.
- Observabilidade, para métricas Micrometer e health checks de infraestrutura.

## Fonte da verdade

A fonte da verdade adotada será o ledger de eventos persistidos, não apenas a tabela de saldo atual.

A tabela de saldo atual será tratada como uma projeção derivada para leitura eficiente. Em caso de divergência, a projeção pode ser reconstruída a partir dos eventos persistidos.

Motivos:

- Permite explicar como o saldo atual foi formado.
- Facilita auditoria.
- Permite reprocessamento após correção de regra.
- Evita depender exclusivamente do último estado gravado.

Trade-off:

- A solução fica mais complexa do que um CRUD simples de estoque.
- Reprocessar eventos exige cuidado com ordenação, idempotência e efeitos colaterais.

## Chave de estoque

A chave inicial do saldo de estoque será:

```text
accountId + sku
```

O `marketplace` será mantido nos eventos e no histórico, mas não fará parte da chave canônica inicial do saldo físico.

Justificativa:

- O desafio pede visão atual por conta e SKU.
- O mesmo SKU em contas diferentes não pode se afetar.
- O estoque físico disponível normalmente pertence ao seller/conta, enquanto marketplaces representam canais de venda ou sincronização.

Trade-off:

- Caso a operação real exija estoque segregado por marketplace, anúncio, fulfillment ou depósito, será necessário evoluir a chave para incluir essas dimensões.
- Para uma versão de produção, eu avaliaria criar projeções adicionais por marketplace/anúncio sem abandonar a visão canônica por conta e SKU.

## Tipos de evento

Eventos inicialmente suportados:

- `STOCK_ADJUSTED`: define o saldo disponível absoluto para uma conta e SKU.
- `ORDER_CREATED`: reduz o saldo disponível conforme a quantidade vendida.
- `ORDER_CANCELLED`: devolve estoque de um pedido previamente criado.
- `STOCK_SYNC_SENT`: registra envio de saldo ao marketplace, sem alterar o saldo canônico.
- `MARKETPLACE_STOCK_RESTORED`: registra recomposição feita pelo marketplace. Por padrão, não altera automaticamente o saldo canônico sem regra explícita.

Decisão importante:

Eventos de sincronização e recomposição de marketplace serão auditáveis, mas não devem sobrescrever automaticamente o saldo canônico sem uma política clara. Isso evita que um canal externo altere o estoque físico sem reconciliação.

## Idempotência

A idempotência técnica será garantida por `eventId`.

Cada evento recebido será persistido com uma restrição única por `eventId`. Caso o mesmo evento seja recebido novamente, ele será reconhecido como duplicado e não será aplicado de novo ao estoque.

Comportamento esperado:

- Mesmo `eventId` e mesmo payload: retornar/processar como duplicado idempotente.
- Mesmo `eventId` e payload divergente: registrar erro de consistência, pois indica reutilização indevida de identificador.

## Duplicidade lógica

Além da idempotência por `eventId`, existe duplicidade lógica de negócio. Exemplo: dois eventos diferentes representando o mesmo pedido.

Para pedidos, a chave lógica será:

```text
accountId + marketplace + externalOrderId + sku
```

Comportamento:

- `ORDER_CREATED` para a mesma chave lógica não deve baixar estoque duas vezes.
- `ORDER_CANCELLED` só deve devolver estoque se houver pedido criado correspondente ainda não cancelado.

Trade-off:

- Essa decisão assume que `externalOrderId` identifica de forma estável o pedido dentro do marketplace.
- Se um pedido tiver múltiplos itens do mesmo SKU em eventos separados, será necessário modelar linha de pedido ou item externo com uma chave mais granular.

## Eventos fora de ordem

Eventos podem chegar fora de ordem. A aplicação deve persistir todos os eventos aceitos e aplicar o saldo considerando `occurredAt`.

Estratégia inicial:

1. Persistir o evento recebido.
2. Identificar o agregado afetado: `accountId + sku`.
3. Recalcular a projeção desse agregado a partir dos eventos persistidos, ordenados por `occurredAt` e, em caso de empate, por ordem de recebimento.

Justificativa:

- Esta abordagem simplifica a consistência do saldo para um desafio técnico.
- Torna o comportamento determinístico e fácil de testar.
- Evita regras frágeis baseadas somente na ordem de chegada.

Trade-off:

- Recalcular o agregado a cada evento pode ter custo maior com alto volume.
- Em produção, eu avaliaria snapshots, particionamento por agregado, compactação de histórico e processamento incremental com controle de versão.

## Concorrência

Dois eventos podem afetar o mesmo SKU ao mesmo tempo. A solução deve evitar atualizações perdidas e saldo negativo acidental.

Estratégia inicial:

- Processamento transacional por evento.
- Lock por agregado `accountId + sku` durante o recálculo/aplicação da projeção.
- Restrições únicas para idempotência e duplicidade lógica.
- Versionamento da projeção de estoque com optimistic locking quando aplicável.

Em MySQL, o lock pode ser implementado com uma linha de projeção de estoque bloqueada via `SELECT ... FOR UPDATE` ou por mecanismo equivalente via JPA com lock pessimista.

Trade-off:

- Locks por agregado reduzem paralelismo para o mesmo SKU.
- Em compensação, mantém alta concorrência entre SKUs e contas diferentes.

## Estoque negativo

A regra padrão será não permitir saldo negativo acidentalmente.

Eventos relativos, como `ORDER_CREATED`, não devem levar o saldo abaixo de zero. Caso isso ocorra, o evento deve ser registrado com status de rejeição de negócio ou falha de reconciliação, preservando rastreabilidade.

Eventos absolutos, como `STOCK_ADJUSTED`, podem corrigir o saldo para um valor válido informado pelo cliente/sistema de origem.

Decisão:

- Saldo negativo não será permitido por padrão.
- Se um cenário real exigir backorder ou venda sem estoque, isso deverá virar uma regra explícita e documentada.

## Auditoria e rastreabilidade

A aplicação deve permitir explicar por que o estoque atual chegou ao valor apresentado.

São mantidos:

- Evento bruto recebido.
- Status de processamento do evento.
- Movimento gerado no estoque, quando aplicável.
- Saldo anterior e saldo posterior.
- Motivo ou tipo de alteração.
- Data de ocorrência (`occurredAt`) e data de recebimento/processamento.

Também são registrados campos de contexto em logs de Kafka via MDC, como `eventId`, `eventType`, `accountId`, `sku`, tópico, partição e offset.

Nota:

- `correlationId` entre HTTP, logs e Kafka ainda não foi implementado. É uma evolução recomendada para produção ou para uma segunda iteração do desafio.

Consultas previstas:

- Saldo atual por `accountId + sku`.
- Histórico de movimentos por `accountId + sku`.
- Detalhe de processamento por `eventId`.

## Kafka

Kafka será usado como canal real de entrada de eventos.

Tópicos propostos:

- `stock-events`: eventos recebidos para processamento.
- `stock-events-dlt`: eventos que falharam por erro técnico após retentativas.

Decisões:

- A chave da mensagem Kafka deve ser `accountId:sku`, favorecendo ordenação por agregado dentro da mesma partição.
- O consumer deve ser idempotente, pois Kafka trabalha com entrega ao menos uma vez em configurações comuns.
- Erros de negócio devem ser persistidos como status do evento, não necessariamente enviados para DLT.
- DLT deve ser reservada principalmente para erros técnicos ou payloads inválidos que não puderam ser processados.

Implementação atual:

- Payload JSON inválido é confirmado sem retry infinito, com métrica de rejeição.
- Falhas técnicas durante o processamento passam por retry com backoff e depois são publicadas em `stock-events-dlt`.

## API REST e OpenAPI

A API REST terá dois objetivos:

- Facilitar testes e demonstração do desafio.
- Expor consultas de saldo e histórico.

Endpoints iniciais:

```text
POST /api/v1/events
GET  /api/v1/stocks/{accountId}/{sku}
GET  /api/v1/stocks/{accountId}/{sku}/history
GET  /api/v1/events/{eventId}
```

O contrato será documentado via OpenAPI/Swagger usando Springdoc.

Erros HTTP serão padronizados com `ProblemDetail`, incluindo campos como código, mensagem, detalhe e correlation id.

## Persistência

Banco principal: MySQL.

Tabelas conceituais:

- `stock_events`: ledger dos eventos recebidos.
- `stock_projections`: saldo atual por `accountId + sku`.
- `stock_movements`: movimentos aplicados ao saldo.
- `order_reservations`: controle lógico de pedidos criados/cancelados.

Flyway será usado para versionamento de schema.

Índices importantes:

- `stock_events.event_id` único.
- `stock_events.account_id, stock_events.sku, stock_events.occurred_at`.
- `stock_projections.account_id, stock_projections.sku` único.
- `order_reservations.account_id, marketplace, external_order_id, sku` único.

## Logs estruturados

Fora dos perfis `local`, `test` e `integration-test`, os logs são emitidos em JSON. Campos de contexto são adicionados quando disponíveis:

- `timestamp`
- `level`
- `message`
- `eventId`
- `accountId`
- `sku`
- `eventType`
- `kafkaTopic`
- `kafkaPartition`
- `kafkaOffset`

Não devem ser logados segredos, credenciais, tokens ou payloads sensíveis sem necessidade.

## Segurança

Mesmo sendo um desafio técnico, a aplicação deve seguir boas práticas básicas:

- Validação de entrada com Bean Validation.
- Rejeição de payloads inválidos.
- Não expor stack traces em respostas HTTP.
- Não versionar credenciais reais.
- Configurações por variáveis de ambiente.
- Usuário de banco com privilégios mínimos necessários.
- Logs sem dados sensíveis.
- Actuator restrito ao necessário.
- Segredos de runtime fornecidos por variáveis de ambiente ou secret manager, nunca por valores fixos versionados.

Decisão implementada para o desafio:

- Endpoints de escrita exigem Basic Auth.
- Endpoints de consulta ficam públicos para facilitar a avaliação manual.
- `/actuator/health` fica público para health checks.
- Swagger UI fica público para melhorar a experiência de avaliação.
- Usuário/senha da API e credenciais do MySQL são obrigatórios via ambiente. No Docker Compose local, devem ser preenchidos em `.env`, que é ignorado pelo Git.
- Em ambientes reais, credenciais devem ser injetadas por secrets do provedor/orquestrador, como Azure Key Vault, AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets, Docker Secrets ou GitHub Actions Secrets.

Nota:

- Um limite explícito de tamanho de payload por endpoint ainda não foi configurado. Para produção, eu adicionaria limite de body no servidor/gateway e testes específicos para payload excessivo.

Trade-off:

- Basic Auth é simples e suficiente para o desafio. Em produção, eu adicionaria OAuth2/JWT, mTLS ou integração com gateway/API management, além de autorização por conta.

## Testes

Tipos de teste previstos:

- Testes unitários do domínio, sem Spring.
- Testes de aplicação para casos de uso.
- Testes de integração com MySQL via Testcontainers.
- Testes de integração com Kafka via Testcontainers.
- Testes REST com MockMvc ou WebTestClient.

Cenários mínimos:

- `STOCK_ADJUSTED available=10` resulta em saldo 10.
- `ORDER_CREATED quantity=2` após saldo 10 resulta em saldo 8.
- `ORDER_CANCELLED quantity=2` devolve estoque corretamente.
- Evento duplicado por `eventId` não altera saldo duas vezes.
- Pedido duplicado com outro `eventId` não baixa estoque duas vezes.
- Eventos fora de ordem mantêm saldo determinístico.
- Contas diferentes com mesmo SKU não se afetam.
- Concorrência no mesmo SKU não gera atualização perdida.
- Tentativa de baixar estoque abaixo de zero é rejeitada.

## Observabilidade operacional

A aplicação expõe:

- Health checks via Spring Actuator.
- Health de banco via datasource e health customizado de Kafka via AdminClient.
- Logs estruturados em JSON fora dos perfis locais/teste; nos perfis `local`, `test` e `integration-test`, os logs ficam em console simples para reduzir ruído.
- Métricas básicas de eventos processados, rejeitados e duplicados.
- Workflow de CI com testes unitários/slice e integrações Testcontainers.

O Swagger UI foi mantido público para avaliação e recebeu configuração visual específica para deixar a seção de schemas mais legível, sem remover constraints do contrato OpenAPI.

Em produção, eu adicionaria tracing distribuído com OpenTelemetry.

## Trade-offs assumidos

- Recálculo por agregado simplifica a corretude, mas pode ser menos eficiente em alto volume.
- Chave canônica `accountId + sku` atende ao desafio, mas pode precisar evoluir para marketplace/anúncio/depósito.
- Kafka será usado como entrada real, mas REST será mantido para demonstração e testes manuais.
- Basic Auth foi usado no desafio por simplicidade; uma solução produtiva exigiria autenticação forte e autorização por conta.
- Eventos de marketplace serão inicialmente auditáveis, não autoritativos sobre o estoque canônico.

## O Que Faria Diferente em Produção

- Avaliaria particionamento e sharding por conta/SKU em cenários de alto volume.
- Criaria snapshots de estoque para reduzir custo de reprocessamento.
- Adicionaria controle de esquema de eventos com Schema Registry.
- Implementaria DLQ operacional com painel de reprocessamento.
- Adicionaria autenticação forte, autorização por conta e auditoria de acesso.
- Usaria tracing distribuído e métricas com dashboards.
- Validaria regras específicas por marketplace e por tipo de integração.
- Consideraria separar leitura e escrita com CQRS se o volume justificar.
