# Architecture: Ports and Adapters (Hexagonal)

PriceForge is a modular monolith: one deployable Spring Boot application (`app`), where each
bounded context (`catalog`, `pricing`, `promotions`, `audit`) is an independent Gradle module
structured as ports-and-adapters.

## Package layout per bounded context

Each context lives under `com.priceforge.<context>` with three subpackages:

- `domain/` — business rules: aggregates, value objects, domain events. Plain Java, zero
  framework imports.
- `application/port/in` — use case interfaces (`*UseCase`).
- `application/port/out` — outbound port interfaces (`*Port`), e.g. persistence, event
  publishing, external services.
- `application/service` — use case implementations (`*Service`), depending only on `domain` and
  the ports it declares.
- `application/dto` — Command/Result objects crossing the use-case boundary.
- `infrastructure/in/*` — driving adapters (HTTP controllers, message listeners, scheduled jobs).
  Call only `*UseCase` interfaces, never a `*Service` class directly.
- `infrastructure/out/*` — driven adapters (persistence, cache, external clients). Implement
  `*Port` interfaces.

## Rules

- Dependencies point inward only: `infrastructure` → `application` → `domain`.
- `domain` has zero framework imports (no Spring, no JPA, no Jackson annotations).
- Entities/DTOs never cross a layer boundary unmapped — a JPA entity never leaves
  `infrastructure/out`, an HTTP DTO never leaves `infrastructure/in`.
- `@Transactional` and `@PreAuthorize` live in `application/service`, not in controllers — see
  arquitectura-tecnica.md's authorization decision.
- Controllers/listeners depend on `*UseCase` interfaces, never on `*Service` classes directly.

## Cross-context dependency rule

A bounded-context module may depend only on:

1. `shared-kernel` (`Money`, `DomainEvent`, typed identifiers, `EventPublisher`).
2. The `application/port` interfaces published by another context.

It may never depend on another context's `domain` or `infrastructure` packages. Cross-context
reactions to domain events (e.g. Pricing invalidating its cache when Catalog changes a product's
price) are implemented as an inbound adapter in the *consuming* module, listening to the emitting
module's outbox — never by calling the emitting module's internals directly.

See the `hexagonal-boundary-check`, `new-use-case`, and `new-adapter` Claude Code skills for
enforcement and scaffolding of this convention.
