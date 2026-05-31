# Gubee Stock Reconciliation

Servico para reconciliacao de estoque a partir de eventos de pedidos, ajustes e sincronizacoes com marketplaces.

## Stack

- Java 21
- Spring Boot 3.5
- MySQL 8.4
- Kafka real via Docker
- Flyway
- OpenAPI/Swagger
- Testcontainers
- Logs estruturados
- Arquitetura hexagonal

## Como rodar localmente

Suba a infraestrutura:

```bash
docker compose up -d
```

O MySQL fica publicado em `localhost:3307` para evitar conflito com instalacoes locais em `3306`. Dentro do Compose ele continua usando a porta padrao `3306`.

Rode a aplicacao:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

As credenciais de escrita podem ser sobrescritas por variaveis de ambiente:

```bash
GUBEE_SECURITY_USER=admin \
GUBEE_SECURITY_PASSWORD=gubee-admin \
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Configuracoes locais de banco tambem podem ser sobrescritas:

```text
GUBEE_MYSQL_HOST=localhost
GUBEE_MYSQL_PORT=3307
GUBEE_MYSQL_DATABASE=gubee_stock
GUBEE_MYSQL_USER=gubee
GUBEE_MYSQL_PASSWORD=gubee
```

Endpoints uteis:

- API ping: `http://localhost:8080/api/v1/ping`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Metrics autenticado: `http://localhost:8080/actuator/metrics`

## Seguranca

Os endpoints de consulta ficam publicos para facilitar a avaliacao. Endpoints de escrita exigem Basic Auth.

Credenciais locais do desafio:

```text
usuario: admin
senha: gubee-admin
```

Esses valores sao defaults locais. Para outro ambiente, configure:

```text
GUBEE_SECURITY_USER
GUBEE_SECURITY_PASSWORD
```

Decisoes:

- `GET /api/v1/**` publico.
- `POST /api/v1/events` protegido com Basic Auth.
- `/actuator/health` publico para health checks.
- Swagger UI publico para simplificar a avaliacao manual.
- Demais endpoints exigem autenticacao.

## API REST

Processar evento:

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -u admin:gubee-admin \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "evt-001",
    "type": "STOCK_ADJUSTED",
    "occurredAt": "2026-05-28T10:00:00Z",
    "accountId": "account-001",
    "sku": "ABC-123",
    "available": 10,
    "reason": "manual_adjustment"
  }'
```

Consultas:

```text
GET /api/v1/stocks/{accountId}/{sku}
GET /api/v1/stocks/{accountId}/{sku}/history
GET /api/v1/events/{eventId}
```

O contrato OpenAPI esta disponivel em:

```text
GET /v3/api-docs
GET /swagger-ui.html
```

Tambem ha uma colecao HTTP pronta em `http/requests.http`, com chamadas para health, Swagger, processamento de evento, saldo atual, historico, evento processado e metricas.

## Kafka

Topico de entrada: `stock-events`

Topico de dead-letter: `stock-events-dlt`

Key recomendada: `{accountId}:{sku}`

Payload:

```json
{
  "eventId": "evt-001",
  "type": "STOCK_ADJUSTED",
  "occurredAt": "2026-05-28T10:00:00Z",
  "accountId": "account-001",
  "sku": "ABC-123",
  "available": 10,
  "reason": "manual_adjustment"
}
```

Tipos suportados:

```text
STOCK_ADJUSTED
ORDER_CREATED
ORDER_CANCELLED
STOCK_SYNC_SENT
MARKETPLACE_STOCK_RESTORED
```

O consumer usa ack manual imediato: mensagens validas sao confirmadas depois do processamento; mensagens malformadas sao rejeitadas com log estruturado e confirmadas para evitar reprocessamento infinito.

Falhas transitorias de processamento passam por retry com backoff antes de serem publicadas no topico de dead-letter. Configuracao padrao:

```yaml
stock-reconciliation:
  kafka:
    stock-events-topic: stock-events
    dead-letter-topic: stock-events-dlt
    retry-max-attempts: 3
    retry-backoff-millis: 1000
```

Exemplo de publicacao via container Kafka:

```bash
docker compose exec kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic stock-events \
  --property parse.key=true \
  --property key.separator='|' \
  <<< 'account-001:ABC-123|{"eventId":"evt-kafka-001","type":"STOCK_ADJUSTED","occurredAt":"2026-05-28T10:00:00Z","accountId":"account-001","sku":"ABC-123","available":10,"reason":"manual_adjustment"}'
```

## Observabilidade

Health checks:

- `db`: health indicator padrao do datasource MySQL.
- `kafka`: health indicator customizado usando Kafka AdminClient.

Metricas customizadas:

- `stock.events.processed`: eventos processados por `status` e `source`.
- `stock.events.rejected`: eventos rejeitados por `reason` e `source`.
- `stock.events.dead_letter`: eventos enviados para DLT por `topic`.

Exemplos:

```bash
curl -u admin:gubee-admin http://localhost:8080/actuator/metrics/stock.events.processed
curl -u admin:gubee-admin http://localhost:8080/actuator/metrics/stock.events.rejected
curl -u admin:gubee-admin http://localhost:8080/actuator/metrics/stock.events.dead_letter
```

## Testes

```bash
mvn test
```

Teste de integracao com MySQL real via Testcontainers:

```bash
mvn -Dtest=StockReconciliationPersistenceIT test
```

Teste de integracao com Kafka e MySQL reais via Testcontainers:

```bash
mvn -Dtest=StockEventKafkaListenerIT test
```

Teste de integracao do dead-letter topic:

```bash
mvn -Dtest=StockEventKafkaDeadLetterIT test
```

O projeto tambem possui GitHub Actions em `.github/workflows/ci.yml`, executando `mvn test` e os testes de integracao com Testcontainers.

## Organizacao

```text
domain      Regras de negocio puras, modelos e portas
application Casos de uso e orquestracao
adapter     Entradas e saidas externas: REST, Kafka, JPA, health e metricas
config      Configuracoes Spring, Kafka, OpenAPI e seguranca
shared      Utilitarios compartilhados de baixo acoplamento
```

## Arquitetura

A aplicacao segue arquitetura hexagonal:

- Entradas: REST controllers e Kafka listener.
- Casos de uso: comandos e consultas da camada `application`.
- Dominio: regras de estoque sem dependencia de Spring, JPA ou Kafka.
- Saidas: repositorios JPA/MySQL, metricas e componentes de infraestrutura.

Esse desenho deixa o dominio testavel sem framework, reduz acoplamento e permite trocar entrada REST/Kafka ou persistencia sem contaminar as regras de negocio.

## Robustez

Comportamentos cobertos:

- Idempotencia por `eventId`.
- Concorrencia com o mesmo `eventId`, aplicando o ledger uma unica vez.
- Rejeicao de duplicidade com payload divergente.
- Payload Kafka invalido confirmado sem entrar em retry infinito.
- Retry com backoff e DLT para falhas transitorias de processamento.

## Trade-offs

- Basic Auth foi escolhido por simplicidade e clareza no contexto do desafio. Em producao, a recomendacao seria OAuth2/OIDC, mTLS ou API Gateway com politica centralizada.
- Swagger ficou publico para melhorar a experiencia de avaliacao. Em producao, poderia ser protegido ou exposto apenas em ambiente interno.
- O dominio nao depende de Spring/JPA/Kafka para preservar a fronteira hexagonal e acelerar testes unitarios.
- A serializacao do evento processado e armazenada para detectar `eventId` repetido com payload divergente.
- A atualizacao de estoque usa lock transacional no MySQL para consistencia forte por par `accountId`/`sku`.

As decisoes tecnicas estao documentadas em `DECISIONS.md`.
