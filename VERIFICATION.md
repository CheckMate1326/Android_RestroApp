# Verification and release status

## Verified so far
- Kotlin backend: `test bootJar` passes. Six tests include ownership, session revocation, invalid quantities/add-ons, price verification, concurrent idempotent checkout, ordered status transitions and review eligibility.
- Public HTTP smoke suite passes against a temporary H2 database: authentication, address isolation, cart validation, reviewed totals, duplicate submission, private order access, favorites and logout.
- Android: debug APK assembles; lint reports zero errors. Existing dependency-version warnings are retained to preserve project compatibility. `testDebugUnitTest` has no source tests; it is not counted as test coverage.
- The native sign-in screen renders on an isolated Android 16 emulator. Registration and address creation passed during the first instrumented run. That run exposed and led to a lazy-list selector fix before checkout.
- A later full instrumented rerun could not complete because the emulator process crashed and its UI automation tree became unavailable. Checkout-to-tracking therefore remains covered by backend/HTTP tests and compiled Android test code, but is not claimed as a passing device test.
- Food image failure states were verified because the isolated emulator could not resolve the remote image host.
- Static design contract audit has zero findings. This is not proof of visual or accessibility conformance.

## Flow coverage
Customer screens are connected through the native Compose shell: splash, sign-in/sign-up, discovery/search/filters, restaurant/menu, customization, cart, addresses, checkout, cash payment selection, confirmation, stored status tracking, orders, reviews, favorites, profile and settings. Staff POS/KDS/tables/analytics remain available through a separate staff sign-in. Delivery operations use the authenticated delivery-kitchen page.

## Required before a public launch
- Supply a verified merchant catalog and licensed photographs. The example catalog is for development only.
- Supply the production HTTPS endpoint and release signing configuration. Release builds require an explicit HTTPS API URL.
- Select and integrate an online payment provider for card/UPI and a courier provider for live GPS. Only cash on delivery and real staff-entered status events are implemented; no payment success or courier movement is simulated.
- Configure secure database credentials, backups, ingress request/rate limits and staff credentials. The supplied persistence is a single-node H2 deployment, not a multi-region marketplace architecture.
- Complete device/accessibility verification and merchant acceptance testing. The independent Next.js `os` app still has its original demo security model and must not be deployed as the customer backend.

The original mobile daemon configuration is preserved locally as `gradle-daemon-jvm.properties.before-restro`; the active daemon now uses JDK 21 so Android lint can run.
