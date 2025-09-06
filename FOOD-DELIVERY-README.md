
# Food Delivery – Microservices Demo (Java 21, Spring Boot 3.5.5, Kafka + Avro, MapStruct, Lombok, Postgres, JWT HS256)

This repository is a **reference microservices implementation** for a food delivery workflow built with **Spring Boot 3.5.5**, **Java 21**, **Apache Kafka** (with **Confluent Schema Registry**), **Avro**, **MapStruct**, **Lombok**, **Spring Security (JWT HS256)** and **PostgreSQL**.

> **Highlights**
> - Strong typing from **Avro logical types** → Java (`uuid` → `java.util.UUID`, `timestamp-millis` → `java.time.Instant`).
> - All Kafka **keys are `String`** (`UUID.toString()`), values are Avro (`KafkaAvroSerializer`).
> - **MapStruct** for DTO↔Entity↔Event mapping. Global `unmappedTargetPolicy=ERROR` enforces complete mappings (fewer surprises).
> - **JWT HS256** (shared secret) for authN/Z: an **Auth** service issues tokens; other services are **resource servers**.
> - **Outbox** pattern in `order-service` to publish `OrderCreatedV1` and `PaymentRequestedV1` reliably.

---

## 1) Architecture & modules

```
food-delivery/
├─ common/common-avro         # Avro contracts, generated SpecificRecords
├─ services/
│  ├─ auth-service            # User register/login, emits fd.user.UserRegisteredV1
│  ├─ user-service            # Builds a user profile from UserRegistered events
│  ├─ catalog-service         # Restaurants & menu items, emits catalog events
│  ├─ cart-service            # Simple cart; emits CartCheckedOutV1
│  ├─ order-service           # Creates orders (REST), outbox → events, saga listeners
│  ├─ payment-service         # Authorizes payments on PaymentRequested
│  ├─ restaurant-ops-service  # Accept/Reject/Ready → emits restaurant lifecycle events
│  ├─ delivery-service        # Assigns courier on DeliveryRequested, pickup/delivered
│  └─ notification-service    # Logs lifecycle events
└─ deploy/                    # Docker Compose: Postgres, Kafka (KRaft), Schema Registry, Kafdrop
```

**Key topics (examples)**  
- `fd.user.registered.v1`
- `fd.catalog.restaurant.created.v1`, `fd.catalog.menu-item.updated.v1`
- `fd.order.created.v1`
- `fd.payment.requested.v1`, `fd.payment.authorized.v1`
- `fd.restaurant.acceptance-requested.v1`, `fd.restaurant.accepted.v1`, `fd.restaurant.rejected.v1`, `fd.restaurant.order-ready.v1`
- `fd.delivery.requested.v1`, `fd.delivery.courier-assigned.v1`, `fd.delivery.picked-up.v1`, `fd.delivery.delivered.v1`

---

## 2) Prerequisites

- **Java 21** (JDK 21)
- **Maven 3.9+**
- **Docker** + **Docker Compose**
- Ports used locally:
  - Kafka `:9092`
  - Schema Registry `:8081`
  - Kafdrop `:9000`
  - Postgres `:5432`
  - Services: `auth:8081`, `order:8082`, `payment:8083`, `catalog:8084`, `cart:8085`, `ops:8086`, `delivery:8087`, `notify:8088`, `user:8089`

---

## 3) Quick start

### 3.1 Start infra
```bash
cd deploy
docker compose up -d
# Postgres:       localhost:5432 (postgres/postgres)
# Kafka:          localhost:9092
# SchemaRegistry: http://localhost:8081
# Kafdrop:        http://localhost:9000
```

### 3.2 Build all modules
From the **repo root** (where `pom.xml` with `<packaging>pom</packaging>` lives):
```bash
mvn -U -DskipTests clean install
```

### 3.3 Run services (in separate terminals)
```bash
cd services/auth-service && mvn spring-boot:run    # :8081
cd services/order-service && mvn spring-boot:run   # :8082
cd services/payment-service && mvn spring-boot:run # :8083
cd services/catalog-service && mvn spring-boot:run # :8084
cd services/cart-service && mvn spring-boot:run    # :8085
cd services/restaurant-ops-service && mvn spring-boot:run # :8086
cd services/delivery-service && mvn spring-boot:run # :8087
cd services/notification-service && mvn spring-boot:run # :8088
cd services/user-service && mvn spring-boot:run # :8089
```

---

## 4) Environment variables

All services ship with sensible defaults in `application.yml`. You can override them with environment variables:

| Purpose | Property | Env var override | Default |
|---|---|---|---|
| **JWT (shared HS256 secret)** | `security.jwt.secret` | `SECURITY_JWT_SECRET` | `u81ShczV/.../qjI=` |
| Kafka bootstrap | `spring.kafka.bootstrap-servers` | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| Schema Registry | `spring.kafka.properties.schema.registry.url` | `SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL` | `http://localhost:8081` |
| DB URL | `spring.datasource.url` | `SPRING_DATASOURCE_URL` | e.g. `jdbc:postgresql://localhost:5432/auth` |
| DB user | `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` |
| DB password | `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `postgres` |

> **Note**: Spring automatically maps environment variables to properties by uppercasing and replacing dots with underscores. Example: `SECURITY_JWT_SECRET` overrides `security.jwt.secret`.

---

## 5) Using the app (End‑to‑End)

Below are minimal **cURL** examples. Adjust ports to the service you’re calling and pass a **JWT** where required.

### 5.1 Register & login (auth-service)
```bash
# Register (returns JWT in JSON)
curl -X POST http://localhost:8081/auth/register   -H 'Content-Type: application/json'   -d '{"username":"alice","email":"alice@example.com","password":"p@ss"}'
# => {"accessToken":"<JWT>","expiresAtEpochSeconds":..., "tokenType":"Bearer"}

# Optionally login later
curl -X POST http://localhost:8081/auth/login   -H 'Content-Type: application/json'   -d '{"username":"alice","password":"p@ss"}'
```

**Get your User ID (UUID)**  
- The **JWT `sub`** claim is the userId. You can decode the middle part of the token (Base64URL) to view claims, or look in the `auth` database:  
  ```sql
  -- In Postgres 'auth' DB
  SELECT id, username, email FROM users WHERE username='alice';
  ```

### 5.2 Grant roles (so you can call protected endpoints)
Some endpoints require specific roles. Use SQL to grant roles to your user (demo only):

```sql
-- Connect to Postgres 'auth' DB (postgres/postgres)
-- Find the role IDs
SELECT id, name FROM roles;
-- Get your user UUID
SELECT id FROM users WHERE username='alice';

-- Grant roles by inserting into the join table user_roles
INSERT INTO user_roles(user_id, role_id)
VALUES ('<alice_user_uuid>', (SELECT id FROM roles WHERE name='ADMIN')),
       ('<alice_user_uuid>', (SELECT id FROM roles WHERE name='RESTAURANT_OWNER')),
       ('<alice_user_uuid>', (SELECT id FROM roles WHERE name='COURIER'));
```

> After you add roles, keep using the same token (roles are **not** re-read from the database on each request). For a fresh token including new roles, **login again** to get a new JWT.

### 5.3 Create a restaurant & menu (catalog-service)
```bash
TOKEN="<paste JWT>"
OWNER_ID="<alice_user_uuid>"  # from DB or JWT 'sub'
# Create restaurant
curl -X POST http://localhost:8084/restaurants   -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d "{"ownerUserId":"$OWNER_ID","name":"Pasta Place","address":"123 Main","isOpen":true}"

# Add menu item
REST_ID="<uuid returned from previous call>"
curl -X POST http://localhost:8084/restaurants/$REST_ID/menu/items   -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d "{"restaurantId":"$REST_ID","name":"Spaghetti","description":"Classic","priceCents":1299,"available":true,"version":1}"
```

*Watch events in **Kafdrop** → topics `fd.catalog.*`*

### 5.4 Create a cart (cart-service) and/or create an order (order-service)

**Option A – Direct order (simpler demo)**  
```bash
# Create order (as CUSTOMER)
curl -X POST http://localhost:8082/orders   -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d '{
        "restaurantId":"<REST_ID>",
        "currency":"USD",
        "items":[
          {"menuItemId":"<menu_item_id>","name":"Spaghetti","unitPriceCents":1299,"quantity":1}
        ]
      }'
# OrderService outbox emits: fd.order.created.v1 and fd.payment.requested.v1
# PaymentService emits: fd.payment.authorized.v1
```

**Option B – Use cart-service (emits fd.cart.checked-out.v1)**  
> Note: the demo leaves **order creation from cart** as an extension (order-service does not consume `fd.cart.checked-out.v1` yet).  
```bash
CART_ID=$(uuidgen) # or any UUID string
curl -X POST http://localhost:8085/carts/$CART_ID/items   -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d "{"menuItemId":"<menu_item_id>","name":"Spaghetti","unitPriceCents":1299,"quantity":1}"

curl -X POST http://localhost:8085/carts/$CART_ID/checkout   -H "Authorization: Bearer $TOKEN"
# See topic: fd.cart.checked-out.v1
```

### 5.5 Restaurant accepts (restaurant-ops-service)
```bash
ORDER_ID="<uuid returned by /orders>"
# As RESTAURANT_OWNER or ADMIN
curl -X POST "http://localhost:8086/ops/orders/$ORDER_ID/accept?etaMinutes=15"   -H "Authorization: Bearer $TOKEN"
# Emits: fd.restaurant.accepted.v1
# OrderService listens and requests delivery: fd.delivery.requested.v1
```

### 5.6 Delivery assignment, pickup & delivered (delivery-service)
- `delivery-service` assigns a courier on `fd.delivery.requested.v1` and emits `fd.delivery.courier-assigned.v1`.
- To **pickup/deliver** via API, you need the **assignmentId** (stored in the `delivery` DB).

```sql
-- In Postgres 'delivery' DB
SELECT id as assignment_id, order_id, courier_id, status FROM assignments WHERE order_id = '<ORDER_ID>';
```

```bash
ASSIGNMENT_ID="<value from query>"
# As COURIER or ADMIN:
curl -X POST http://localhost:8087/delivery/tasks/$ASSIGNMENT_ID/pickup   -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8087/delivery/tasks/$ASSIGNMENT_ID/delivered   -H "Authorization: Bearer $TOKEN"
```

*All lifecycle events are logged by **notification-service**.*

---

## 6) Configuration details

- **Security**: JWT HS256 with shared secret. In production you’d switch to asymmetric keys & JWKS.
- **Kafka**: Producer key serializer = `StringSerializer`. Value serializer = `KafkaAvroSerializer`. Consumers set `specific.avro.reader=true`.
- **Avro**: See `common/common-avro/src/main/avro/*.avsc`. The Maven Avro plugin runs at `generate-sources` and outputs SpecificRecords.
- **Outbox**: `order-service` stores events in DB, a scheduler publishes to Kafka (at-least-once demo).
- **MapStruct**: All mappers specify `imports = {UUID.class, Instant.class}`; expressions create typed values to match Avro classes exactly.

---

## 7) Troubleshooting

- **Build fails with parent not found** → Run `mvn clean install` **from the repository root** (aggregator POM).
- **Decoders class not found** → Use this final project; resource services rely on `java.util.Base64`; only auth-service uses JJWT (runtime).
- **Schema registry / Kafka errors** → Ensure Docker stack is up and that services can reach `localhost:9092` and `http://localhost:8081`.
- **DB connection refused** → Make sure Postgres container is up (`docker compose ps`) and ports are free.
- **403 Forbidden** → Endpoint may require a specific role. Grant roles via SQL and re-login to get a token with updated claims.

---

## 8) Extending the system (ideas)
- Consume `fd.cart.checked-out.v1` in `order-service` to auto-create orders.
- Add **query** endpoints (GET) for orders, restaurants, and delivery tasks.
- Use **Debezium** or DB triggers for outbox instead of a scheduler.
- Switch to **asymmetric JWT** and central authorization server.
- Add **testcontainers** for integration tests.

---

## 9) License
For demo/educational purposes.
