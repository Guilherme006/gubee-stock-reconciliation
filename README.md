# Gubee Stock Reconciliation

[![CI](https://github.com/Guilherme006/gubee-stock-reconciliation/actions/workflows/ci.yml/badge.svg)](https://github.com/Guilherme006/gubee-stock-reconciliation/actions/workflows/ci.yml)

Serviço para reconciliação de estoque a partir de eventos de pedidos, ajustes e sincronizações com marketplaces.

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

## Como Rodar Localmente

Suba a aplicação completa, incluindo MySQL, Kafka e API:

```bash
docker compose up --build -d
```

Depois acesse:

```text
http://localhost:8080/swagger-ui.html
```

O MySQL fica publicado em `localhost:3307` para evitar conflito com instalações locais em `3306`. Dentro do Compose, ele continua usando a porta padrão `3306`.

Para rodar a aplicação fora do Docker durante desenvolvimento, suba apenas a infraestrutura e execute o Maven:

```bash
docker compose up -d mysql kafka
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

As credenciais de escrita podem ser sobrescritas por variáveis de ambiente:

```bash
GUBEE_SECURITY_USER=admin \
GUBEE_SECURITY_PASSWORD=gubee-admin \
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Configurações locais de banco também podem ser sobrescritas:

```text
GUBEE_MYSQL_HOST=localhost
GUBEE_MYSQL_PORT=3307
GUBEE_MYSQL_DATABASE=gubee_stock
GUBEE_MYSQL_USER=gubee
GUBEE_MYSQL_PASSWORD=gubee
GUBEE_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Use `.env.example` como referência das variáveis disponíveis.

Endpoints úteis:

- API ping: `http://localhost:8080/api/v1/ping`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Metrics autenticado: `http://localhost:8080/actuator/metrics`

## Segurança

Os endpoints de consulta ficam públicos para facilitar a avaliação. Endpoints de escrita exigem Basic Auth.

Credenciais locais do desafio:

```text
usuário: admin
senha: gubee-admin
```

Esses valores são defaults locais para facilitar a avaliação do desafio. Não use essas credenciais em produção. Para outro ambiente, configure:

```text
GUBEE_SECURITY_USER
GUBEE_SECURITY_PASSWORD
```

Decisões:

- `GET /api/v1/**` público.
- `POST /api/v1/events` protegido com Basic Auth.
- `/actuator/health` público para health checks.
- Swagger UI público para simplificar a avaliação manual.
- Demais endpoints exigem autenticação.

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

O contrato OpenAPI está disponível em:

```text
GET /v3/api-docs
GET /swagger-ui.html
```

Também há uma coleção HTTP pronta em `http/requests.http`, com chamadas para health, Swagger, processamento de evento, saldo atual, histórico, evento processado e métricas.

## Kafka

Tópico de entrada: `stock-events`

Tópico de dead-letter: `stock-events-dlt`

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

O consumer usa ack manual imediato: mensagens válidas são confirmadas depois do processamento; mensagens malformadas são rejeitadas com log estruturado e confirmadas para evitar reprocessamento infinito.

Falhas transitórias de processamento passam por retry com backoff antes de serem publicadas no tópico de dead-letter. Configuração padrão:

```yaml
stock-reconciliation:
  kafka:
    stock-events-topic: stock-events
    dead-letter-topic: stock-events-dlt
    retry-max-attempts: 3
    retry-backoff-millis: 1000
```

Exemplo de publicação via container Kafka:

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

- `db`: health indicator padrão do datasource MySQL.
- `kafka`: health indicator customizado usando Kafka AdminClient.

Métricas customizadas:

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

Teste de integração com MySQL real via Testcontainers:

```bash
mvn -Dtest=StockReconciliationPersistenceIT test
```

Teste de integração com Kafka e MySQL reais via Testcontainers:

```bash
mvn -Dtest=StockEventKafkaListenerIT test
```

Teste de integração do dead-letter topic:

```bash
mvn -Dtest=StockEventKafkaDeadLetterIT test
```

O projeto também possui GitHub Actions em `.github/workflows/ci.yml`, executando `mvn test` e os testes de integração com Testcontainers.

O status do CI pode ser acompanhado em:

```text
https://github.com/Guilherme006/gubee-stock-reconciliation/actions/workflows/ci.yml
```

## Smoke test

Com Docker Compose ativo, execute:

```bash
./scripts/smoke-test.sh
```

O script valida health, Swagger, segurança do POST, processamento REST autenticado, consulta de saldo/evento, publicação Kafka real e métrica de eventos processados.

## Organização

```text
domain      Regras de negócio puras, modelos e portas
application Casos de uso e orquestração
adapter     Entradas e saídas externas: REST, Kafka, JPA, health e métricas
config      Configurações Spring, Kafka, OpenAPI e segurança
shared      Utilitários compartilhados de baixo acoplamento
```

## Arquitetura

A aplicação segue arquitetura hexagonal:

- Entradas: REST controllers e Kafka listener.
- Casos de uso: comandos e consultas da camada `application`.
- Domínio: regras de estoque sem dependência de Spring, JPA ou Kafka.
- Saídas: repositórios JPA/MySQL, métricas e componentes de infraestrutura.

Esse desenho deixa o domínio testável sem framework, reduz acoplamento e permite trocar entrada REST/Kafka ou persistência sem contaminar as regras de negócio.

## Robustez

Comportamentos cobertos:

- Idempotência por `eventId`.
- Concorrência com o mesmo `eventId`, aplicando o ledger uma única vez.
- Rejeição de duplicidade com payload divergente.
- Payload Kafka inválido confirmado sem entrar em retry infinito.
- Retry com backoff e DLT para falhas transitórias de processamento.

## Trade-offs

- Basic Auth foi escolhido por simplicidade e clareza no contexto do desafio. Em produção, a recomendação seria OAuth2/OIDC, mTLS ou API Gateway com política centralizada.
- Swagger ficou público para melhorar a experiência de avaliação. Em produção, poderia ser protegido ou exposto apenas em ambiente interno.
- O domínio não depende de Spring/JPA/Kafka para preservar a fronteira hexagonal e acelerar testes unitários.
- A serialização do evento processado é armazenada para detectar `eventId` repetido com payload divergente.
- A atualização de estoque usa lock transacional no MySQL para consistência forte por par `accountId`/`sku`.

As decisões técnicas estão documentadas em `DECISIONS.md`.
