# PriceForge — Pricing & Promotions Platform

A modular-monolith pricing and promotions engine built with Java 21 and Spring Boot, structured
as four hexagonal (ports-and-adapters) bounded contexts: **Catalog**, **Pricing**, **Promotions**
and **Audit**.

> This README is a placeholder. It will be expanded with the architecture diagram, local setup
> instructions and example requests once the bounded contexts are implemented.

## Modules

| Module | Responsibility |
|---|---|
| `shared-kernel` | `Money`, `DomainEvent`, typed identifiers, `EventPublisher` port |
| `catalog` | Product catalog and base pricing |
| `pricing` | Pricing rule engine and final price calculation |
| `promotions` | Campaigns, coupons and eligibility |
| `audit` | Traceability of calculations and rule changes |
| `app` | Spring Boot entry point: configuration and wiring |

## Local development

```
docker-compose up -d
./gradlew bootRun
```
