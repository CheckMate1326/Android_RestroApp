# Restro interaction contract

- Server identity owns profile, addresses, favorites, cart and orders; client never supplies an owner ID.
- Quote and checkout calculate integer paise from current catalog, quantities and allowed add-ons. Orders use an idempotency key, immutable address and price snapshots. Cash orders remain unpaid until operational confirmation.
- Loading retains content on refresh; errors are visible with retry. No fallback demo data on network failure. Checkout errors preserve cart and idempotency key.
- Native back returns through detail screens; bottom destinations are Home, Favorites, Orders and Profile. Cart is reachable from the shell.
- Search has clear, categories, vegetarian filter and price sorting. Empty results explain how to reset filters.
- Save returns to the owning list; deletion requires explicit confirmation. Passwords are masked. Logout revokes the server token and erases local session state.
- Tracking reflects stored operational events only. No countdowns or invented courier coordinates.
- Sources: user brief, AUDIT.md and DeliveryService invariants. Existing web demo auth is explicitly outside the customer trust boundary.

## Canonical UI Map
| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Form | DeliveryApp Field, native Compose Material controls | DeliveryService input bounds | create / edit / password | service input tests and emulator flow |
| CRUD | DeliveryViewModel and DeliveryService | delivery API ownership and transactional invariants | list / detail / confirmation | DeliveryServiceTests |
| Feedback | DeliveryApp error banner and Material progress | DeliveryViewModel error/busy | request / refresh | offline and HTTP tests |
| Navigation | DeliveryApp shell and DeliveryViewModel back | user flow | bottom destinations / detail | emulator flow |
