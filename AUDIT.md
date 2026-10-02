# Restro engineering audit

## Existing systems
- Android: Kotlin 2.1, Compose Material 3, single Activity with POS, kitchen, tables, customer loyalty and analytics. HttpURLConnection API client. No customer authentication, persistence, delivery navigation or image loading. Failure paths return fabricated sample data. Cart uses transient remember state.
- Kotlin server: Spring Boot 3.4.3, ConcurrentHashMap repository, seeded menu/categories/tables/customers, checkout and kitchen tickets. Server looks up item prices but accepts negative quantities, missing IDs and arbitrary status transitions. No authentication or ownership checks. All data disappears on restart.
- Web sibling: Next 16, Dexie and optional Supabase; independent schema and client-trusted checkout. Predictable auth tokens, unsalted password hashes, user-selected roles and simulated email delivery. It is not a trusted authentication provider for Android.
- Build: wrappers point to Gradle 9.5.1 despite Android Gradle Plugin 8.7.3; mobile daemon file is an existing untracked user file and must be preserved. Local JDK 21 and Android emulator are available.

## Implementation direction
Preserve legacy staff source and API shape behind explicit staff authentication. Extend Spring with transactional JDBC storage for delivery, bearer sessions, server quotes and idempotent orders. Keep customer UI separate from staff tools. Catalog records own all commercial content and photography. Demo catalog must be opt-in. No simulated online payment or courier movement. Cash on delivery is the initial supported payment method; online providers require a configured integration before becoming selectable.

## Release boundaries
The independent Next application requires its own security migration before public deployment. Android delivery must never depend on its demo authentication or local orders. Deployment needs TLS, database backup/access controls, rate limiting at the edge, verified merchant catalog, payment provider and courier integration.
