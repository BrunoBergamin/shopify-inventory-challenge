# Inventory Tracking API

A REST API that tracks inventory for a logistics company: create, list, update and delete items, and build
**shipments** that take stock out of the inventory.

Built for the Shopify Backend Developer Challenge with Java 21, Spring Boot 4, PostgreSQL and Flyway.

## How to run

You only need **Docker**.

```bash
docker compose up --build
```

Then open:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs

The database starts with three sample items, so you can try the API right away.

To stop: `docker compose down`. Add `-v` to also delete the database volume.

### Running without Docker

Requires Java 21+ and a PostgreSQL running on `localhost:5432` (database, user and password `inventory`, or
set `DB_URL`, `DB_USER` and `DB_PASSWORD`).

```bash
./mvnw spring-boot:run    # Windows: mvnw.cmd spring-boot:run
```

### Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database in PostgreSQL mode, so they need no Docker. There are two kinds:

- `ShipmentTest`: plain unit tests for the stock rules in the domain model.
- `InventoryApiIntegrationTest`: runs real HTTP requests through controllers, services, JPA and the Flyway migrations.

## Additional feature: Shipments

I chose **shipments** because it is the only option with real business rules: stock can never go negative, and
a shipment and its stock changes must be saved together or not at all.

Lifecycle:

```
            add / remove items
               ┌─────────┐
               ▼         │
  create ──► OPEN ───────┘
               │
       ┌───────┴────────┐
       ▼                ▼
    SHIPPED          CANCELLED
 (stock stays out)  (stock returned)
```

| Action | Effect on stock |
|---|---|
| Add an item to an `OPEN` shipment | Quantity is taken out of stock immediately (a reservation) |
| Add the same item again | The existing line grows, no duplicate line |
| Remove a line | Its quantity goes back to stock |
| Cancel | Every reserved unit goes back to stock |
| Ship | Stock stays out. The shipment becomes read-only |

## API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/items` | List all items |
| `GET` | `/api/items/{id}` | Get one item |
| `POST` | `/api/items` | Create an item (201 + `Location`) |
| `PUT` | `/api/items/{id}` | Update an item |
| `DELETE` | `/api/items/{id}` | Delete an item (204) |
| `GET` | `/api/shipments` | List shipments, newest first |
| `GET` | `/api/shipments/{id}` | Get one shipment with its lines |
| `POST` | `/api/shipments` | Create an empty shipment |
| `POST` | `/api/shipments/{id}/items` | Add an item: `{"inventoryItemId": 1, "quantity": 5}` |
| `DELETE` | `/api/shipments/{id}/items/{lineId}` | Remove a line |
| `POST` | `/api/shipments/{id}/ship` | Mark as shipped |
| `POST` | `/api/shipments/{id}/cancel` | Cancel and return stock |

Errors use the [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format:

| Status | When |
|---|---|
| 400 | Invalid input. The `errors` field lists each invalid field |
| 404 | Item, shipment or shipment line not found |
| 409 | Duplicate SKU, deleting an item used in a shipment, changing a shipment that is not `OPEN`, or a concurrent update |
| 422 | Not enough stock for the requested quantity |

Example:

```bash
curl -X POST localhost:8080/api/shipments -H "Content-Type: application/json" -d '{"destination":"Store 42, Campinas"}'
curl -X POST localhost:8080/api/shipments/1/items -H "Content-Type: application/json" -d '{"inventoryItemId":1,"quantity":20}'
curl -X POST localhost:8080/api/shipments/1/ship
curl localhost:8080/api/items/1     # quantity went from 500 to 480
```

## Item fields

| Field | Why |
|---|---|
| `sku` | The code warehouses use to identify a product. Unique and stored in upper case, so `box-1` and `BOX-1` cannot become two items |
| `name` | What people read in lists |
| `description` | Optional detail (size, material) |
| `quantity` | Units in stock. Never negative, checked in the API, the domain model and a database `CHECK` constraint |
| `unitPrice` | Lets the company value its stock. `BigDecimal` / `NUMERIC(12,2)`, because floating point loses cents |
| `createdAt`, `updatedAt` | Basic audit trail, filled in automatically |

I left out fields like category, supplier or weight. The challenge does not use them, and adding them later is a
single migration.

## Design decisions

- **Layered architecture** (`web` → `service` → `repository`, with `domain` in the middle). It is the standard
  Spring layout, so a reviewer knows where to look.
- **Business rules live in the entities.** `Shipment.addItem()` reserves stock and `InventoryItem.removeStock()`
  refuses to go below zero. The services only load, call and save. That is why the domain rules can be tested
  with plain JUnit in milliseconds.
- **DTOs (Java records) instead of exposing entities.** The API contract does not change when the database does,
  and clients cannot set fields like `id` or `createdAt`.
- **Stock is reserved when the item is added, not when the shipment ships.** Otherwise two open shipments could
  both promise the last 10 units. Cancelling or removing a line gives the units back.
- **Concurrency: optimistic locking (`@Version`).** If two requests change the same item's stock at the same time,
  the second one gets `409` and can retry, instead of silently overwriting the first. I chose it over pessimistic
  locks because conflicts on the same item are rare, and one mechanism covers every write path.
- **One transaction per operation.** Adding a line and taking stock out are committed together or not at all.
- **Deleting is a hard delete, but blocked for items used in shipments** (`409`). Deleting those would erase
  shipment history.
- **Flyway owns the schema** and Hibernate only validates it (`ddl-auto=validate`). Schema changes are versioned
  SQL, and the same migrations run in the tests.
- **PostgreSQL** in Docker for a realistic setup, **H2 in PostgreSQL mode** for tests so they run anywhere.

## What I would add next

- Pagination and search (`?sku=`, `?name=`) on the item list.
- Testcontainers, so integration tests run on the real PostgreSQL.
- Authentication, and recording who created or shipped each shipment.
- A stock movement log table (`IN`, `OUT`, `ADJUSTMENT`), so every quantity change has a history.

## Project structure

```
src/main/java/com/bruno/inventory
├── domain/        entities and stock rules (InventoryItem, Shipment, ShipmentItem)
├── repository/    Spring Data JPA interfaces
├── service/       transactions and orchestration
├── web/           REST controllers
│   └── dto/       request and response records
└── exception/     domain exceptions and the global error handler
src/main/resources/db/migration   Flyway SQL migrations
```
