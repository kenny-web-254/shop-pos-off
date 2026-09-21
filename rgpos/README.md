# RG POS — Phase 1 (database + core business logic)

Open this folder in Android Studio (Ladybug or newer), let Gradle sync, then run the unit tests:
`./gradlew test` (pure JVM, no emulator).

## Layout
- `domain/`  pure Kotlin engines: Money, UnitConversion, Pricing, Payment, Profit, Permissions, Errors
- `data/`    Room entities (27 tables), DAOs, AppDatabase (seed units/expense categories + integrity triggers)
- `data/repo/` transactional flows: ProductRepository, PurchaseRepository, SaleRepository, CreditRepository, AuditRepository

## Core rules implemented
- Stock only ever stored in the product's base unit. Buy 5 cartons (72) -> +360 pieces.
- Every product has retail + wholesale prices (per unit, with quantity tiers) and a buying price (per unit you buy in).
- Sales are one atomic transaction; stock decrement is guarded (never below zero); the DB also enforces it with triggers.
- Cash + M-Pesa + Credit must equal the total. Credit needs a customer and respects the credit limit.
- Cost = weighted average; profit = revenue - actual cost snapshotted on each sale line.
- No tax/VAT anywhere.
- Audit log is hash-chained and append-only; stock and customer ledgers and sales cannot be edited/deleted.
