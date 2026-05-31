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

Rode a aplicacao:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Endpoints uteis:

- API ping: `http://localhost:8080/api/v1/ping`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## API REST

Processar evento:

```bash
curl -X POST http://localhost:8080/api/v1/events \
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

## Kafka

Topico de entrada: `stock-events`

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

O consumer usa ack manual: mensagens validas sao confirmadas depois do processamento; mensagens malformadas sao rejeitadas com log estruturado e confirmadas para evitar reprocessamento infinito.

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

## Organizacao

```text
domain      Regras de negocio e portas
application Casos de uso e orquestracao
adapter     Entradas e saidas externas, como REST, Kafka e persistencia
config      Configuracoes Spring
shared      Utilitarios compartilhados de baixo acoplamento
```

As decisoes tecnicas estao documentadas em `DECISIONS.md`.
