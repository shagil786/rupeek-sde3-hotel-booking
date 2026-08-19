# Rupeek SDE-3 Hotel Booking

Backend implementation for Rupeek's SDE-3 machine-coding exercise, Question A.

## Scope

The service will expose hotel discovery, property onboarding, booking, payment, and cancellation. The implementation will prioritize clean domain boundaries, availability correctness, extensible filters/payment/refund policies, and meaningful unit tests.

## Planned structure

- `domain`: entities, value objects, policies, and state transitions
- `application`: use-case services and ports
- `adapter`: in-memory repositories, payment adapter, and REST controllers
- `common`: validation and error handling

The runnable implementation uses JPA entities with Flyway-managed H2 persistence. Repository interfaces keep the application layer independent of the database adapter; unit-level policy tests and H2 integration tests provide fast verification.

## Requirements source

The requirements are based on the Rupeek email attachment `Rupeek_SDE3_MachineCoding_HotelBooking.docx.pdf` received on 19 August 2026. The requested submission deadline is Friday, 21 August EOD.

## Run

Requires Java 17+ and Maven. The application uses H2 with Flyway migrations by default. Set local-only demo credentials before starting:

```bash
export APP_DEMO_USERNAME=demo
export APP_DEMO_PASSWORD='replace-with-a-local-password'
```

Run with:

```bash
mvn spring-boot:run
```

Tests:

```bash
mvn test
```

The REST API is versioned under `/api/v1` and uses HTTP Basic authentication. The payment provider is deliberately mocked, authentication is a local demo boundary, and no production secrets or gateway credentials are required.

## Design notes

- Booking creation creates a `PENDING_PAYMENT` hold; successful payment confirms it.
- Overlapping inventory is rejected with `409 Conflict`.
- Cancellation refunds the full amount before check-in and releases the booking.
- JPA entities are persisted through Flyway-managed H2 tables; repository interfaces are the replacement seam for PostgreSQL.
- `Idempotency-Key` is required for booking, payment, and cancellation mutations.
