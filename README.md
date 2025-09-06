# Food Delivery – Final (JWT HS256, Java 21, Boot 3.5.5, MapStruct, Avro, Kafka, Postgres)

- Avro logical types → Java: `uuid -> java.util.UUID`, `timestamp-millis -> java.time.Instant`.
- All mappers import `UUID` and `Instant` and map every target field (unmappedTargetPolicy=ERROR).
- Catalog boolean fixed: entity uses `open`; Avro uses `isOpen`. Mapper maps both ways.
- Added missing Avro: `fd.restaurant.OrderReadyForPickupV1`.

## Infra
```bash
cd deploy && docker compose up -d
```

## Build
```bash
mvn -U -DskipTests clean install
```

## Run examples
```bash
cd services/auth-service && mvn spring-boot:run
cd services/order-service && mvn spring-boot:run
cd services/payment-service && mvn spring-boot:run
...
```
