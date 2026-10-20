# Product catalog API

A product catalog assignment built incrementally as two Spring Boot applications.

## Requirements
- JDK 21

## Modules

| Module | Purpose | Default port |
| --- | --- | --- |
| `catalog-api` | Product catalog REST API | 8080 |
| `mock-pricing-system` | Mock external pricing system | 8081 |


## Build and test

```bash
./gradlew clean build
```

Run a single module's tests:

```bash
./gradlew :catalog-api:test
./gradlew :mock-pricing-system:test
```

## Run locally

In separate terminals:

```bash
./gradlew :mock-pricing-system:bootRun
```

```bash
./gradlew :catalog-api:bootRun
```

Applications are currently unauthenticated. For this local assignment demo,
bind them to loopback when running outside an isolated development environment:

```bash
SERVER_ADDRESS=127.0.0.1 ./gradlew :catalog-api:bootRun
```

