# FoodMap Backend

## 1. Purpose

This directory contains the Java microservice backend skeleton for FoodMap.

The backend follows the project rules in:

- `CODEX-after.md`
- `AGENTS.md`
- `.agents/backend-agent.md`
- `skills/foodmap-backend-service/SKILL.md`
- `harness/`

## 2. Technology Baseline

- Java 21
- Maven
- Spring Boot 3.3.x
- Spring Cloud 2023.0.x
- Spring Cloud Alibaba Nacos Discovery
- Docker Compose deployment template under `deploy/`

## 3. Modules

```text
after
├── pom.xml
├── foodmap-common
├── foodmap-gateway-service
├── foodmap-auth-service
├── foodmap-user-service
├── foodmap-relation-service
├── foodmap-store-service
├── foodmap-recommendation-service
├── foodmap-community-service
└── foodmap-media-service
```

## 4. Current Scope

This iteration creates the compileable service skeleton only:

- Maven parent and modules.
- Spring Boot application entry points.
- Basic internal health/info endpoints.
- Gateway route placeholders.
- Shared common API response and enums.

Business APIs, database migrations, service-specific DTOs, and persistence are planned for later iterations.

## 5. Build

From this directory:

```sh
mvn validate
```

From project root:

```sh
./harness/scripts/run-all.sh
```
