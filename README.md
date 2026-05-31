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

## Testes

```bash
mvn test
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
