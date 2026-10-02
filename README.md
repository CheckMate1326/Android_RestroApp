# Restro

Native Android customer app in `kotlin-restaurant-mobile`; Kotlin/Spring service in `kotlin-restaurant-os`. Existing POS/KDS/table/analytics source is preserved. The separate `os` Next app remains a legacy demo and must not be exposed as a production authentication or payment service.

## Run locally (PowerShell)
Use JDK 21 (or the installed compatible Gradle runtime) and Android SDK 35. From repository root:

```powershell
$env:RESTRO_CATALOG_FILE = (Resolve-Path config/catalog.example.json).Path
# This is a demonstration catalog, not a real merchant listing. Replace it for deployment.
.\kotlin-restaurant-os\gradlew.bat -p kotlin-restaurant-os bootRun
```

In another terminal:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\kotlin-restaurant-mobile\gradlew.bat -p kotlin-restaurant-mobile :app:installDebug
```

Debug connects to the emulator host at `http://10.0.2.2:8080`. Release rejects cleartext traffic and uses `-PrestroApiUrl=https://YOUR_HOST/api/delivery`. Set a real endpoint before release. No merchant catalog is loaded unless `RESTRO_CATALOG_FILE` is set. Customer data is stored in the server's `data/restro` H2 file. Back it up and restrict filesystem access. Database URL/user/password are configurable through `RESTRO_DATABASE_URL`, `RESTRO_DATABASE_USER`, `RESTRO_DATABASE_PASSWORD`.

Set `RESTRO_STAFF_USER` and `RESTRO_STAFF_PASSWORD` securely in the server process environment to enable protected staff tools at `/` and `/delivery.html`. Never embed these credentials in an app. The delivery kitchen updates actual order events; Android refreshes tracking while that screen is active. API access to staff orders and status transitions requires the same staff credentials. The default bind address is loopback; deploy behind a TLS reverse proxy with request limits and rate limiting. Do not expose Basic authentication without TLS.

## Data and security
- Customer sign-up uses salted PBKDF2-HMAC-SHA256 (600,000 iterations); sessions are random, hashed at rest, expire after seven days and can be revoked.
- Android stores the bearer token encrypted with an Android Keystore AES-GCM key. Backup is disabled. No credentials or payment data are logged.
- Ownership is derived from the session for every customer endpoint. Server validates catalog IDs, add-ons, quantity bounds and addresses.
- Prices use integer paise and catalog-defined tax-inclusive amounts. Checkout locks transactionally, verifies the reviewed total, snapshots the address and prices, and deduplicates requests.
- Cash on delivery is supported. Card/UPI payments are rejected until a real provider implementation is configured. No fake successful payments or invented courier locations.
- Catalog can contain `offers`: `{ "code": "...", "description": "...", "minimum": 50000, "discount": 5000, "expiresAt": "ISO-8601 UTC instant" }`. Minimum and expiry are enforced by the server.
- Reviews require an owned, delivered order. Restaurant favorites and addresses are server-persisted.

## Verification
```powershell
.\kotlin-restaurant-os\gradlew.bat -p kotlin-restaurant-os test
.\kotlin-restaurant-mobile\gradlew.bat -p kotlin-restaurant-mobile :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

See AUDIT.md for the starting architecture and release boundaries; DESIGN.md and UX-CONTRACT.md own design and interaction decisions.

## Device flow test
With the temporary verification server running, use an isolated emulator and run:

```powershell
.\kotlin-restaurant-mobile\gradlew.bat -p kotlin-restaurant-mobile :app:connectedDebugAndroidTest
```

The instrumented test creates a generated account and exercises registration, address creation, customization, checkout, tracking and activity recreation. It expects a fresh application session and the example catalog. `scripts/emulator_ui.py` provides targeted accessibility-tree inspection and screenshots for manual verification. See VERIFICATION.md for current evidence and release boundaries.
