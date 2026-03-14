# Vehicle Rental Platform — Project Overview

## Arquitectura General

```
                          +-----------------------+
                          |    API Gateway /       |
                          |    Cliente REST        |
                          +-----------+-----------+
                                      |
            +------------+------------+------------+------------+
            |            |                         |            |
            v            v                         v            v
     +------+------+  +--+----------+  +-----------+--+  +-----+-------+
     |  Customer   |  | Reservation |  |   Payment    |  |    Fleet    |
     |  Service    |  |   Service   |  |   Service    |  |   Service   |
     |  :8181      |  |   :8182     |  |   :8184      |  |   :8183     |
     +------+------+  +------+------+  +------+-------+  +------+------+
            |                |                |                  |
            v                v                v                  v
     +------+------+  +------+------+  +------+-------+  +------+------+
     | PostgreSQL  |  | PostgreSQL  |  | PostgreSQL   |  | PostgreSQL  |
     | customer_db |  | reservation |  | payment_db   |  |  fleet_db   |
     +-------------+  |    _db      |  +--------------+  +-------------+
                       +------------+
                                         +-------------+
                                         |  RabbitMQ   |
                                         |  :5672      |
                                         +-------------+
```

## Estructura Hexagonal por Servicio

Cada servicio sigue la misma arquitectura de 4 modulos:

```
{service}-service/
 |
 |   +------------------+      +---------------------+
 |   |     domain       | <--- |    application       |
 |   |  (Zero Spring)   |      |  (spring-tx only)    |
 |   |                  |      |                      |
 |   |  Aggregate Root  |      |  Use Cases (ports)   |
 |   |  Value Objects   |      |  Application Service |
 |   |  Domain Events   |      |  SAGA Orchestrator   |
 |   |  Domain Ports    |      |  SAGA Steps          |
 |   +------------------+      +---------------------+
 |            ^                          ^
 |            |                          |
 |   +--------+--------------------------+--------+
 |   |              infrastructure                 |
 |   |         (Spring components)                 |
 |   |                                             |
 |   |  REST Controllers    JPA Repositories       |
 |   |  Persistence Mapper  Event Publisher         |
 |   |  RabbitMQ Config     SAGA Response Listeners |
 |   |  Command Listeners   Payment Gateway (*)     |
 |   +---------------------------------------------+
 |            ^
 |            |
 |   +--------+------------------------------------+
 |   |              container                      |
 |   |      (@SpringBootApplication)               |
 |   |                                             |
 |   |  BeanConfiguration   application.yml        |
 |   |  Flyway migrations   Integration Tests      |
 |   +---------------------------------------------+
 |
 |   Dependencias (unidireccional):
 |   container --> infrastructure --> application --> domain --> common
```

## Estado Actual del Proyecto

```
 26 changes completados — SAGA Orchestration + Quality Gates + E2E
 ================================================================

  #1  parent-pom-multi-module          }
  #2  customer-domain                  }
  #3  customer-application             }
  #4  common-shared-kernel             }  Fundacion
  #5  customer-infra-container         }
  #6  fleet-domain                     }
  #7  fleet-application                }
  #8  fleet-infra-container            }

  #9  reservation-domain               }
  #10 reservation-application          }  Reservations
  #11 reservation-infra-container      }

  #12 reservation-outbox-and-messaging }  Messaging
  #13 payment-domain                   }
  #14 payment-application              }  Payments
  #15 payment-infra-container          }
  #16 payment-outbox-and-messaging     }  Messaging

  #17 pre-saga-alignment               }  Alignment
  #18 customer-outbox-and-messaging    }  Participante SAGA
  #19 fleet-outbox-and-messaging       }  Participante SAGA
  #20 payment-saga-participation       }  Participante SAGA
  #21 reservation-saga-orchestration   }  SAGA Orchestrator

  #22 jacoco-permanent-coverage        }  Quality Gate
  #23 archunit-architecture-tests      }  Quality Gate
  #24 docker-compose-services          }  Infrastructure
  #25 bruno-e2e-tests                  }  E2E Testing + bugfix payment events
  #26 database-indexes                 }  Flyway indexes for frequent filters
```

## Flujo SAGA End-to-End

```
  POST /reservations
        |
        v
  Reservation Service (Orchestrator)
  +-----------------------------------------------------------------+
  |  1. create() -> PENDING + ReservationCreatedEvent               |
  |  2. sagaOrchestrator.start() -> SagaState(PROCESSING, step=0)  |
  |  3. Step[0]: customer.validate.command (via outbox)             |
  +-----------------------------------------------------------------+
        |                                       |
        v                                       v (si rechaza)
  Customer Service                         CANCELLED + SAGA FAILED
  customer.validated ->                    (sin compensacion)
        |
        v
  Reservation: CUSTOMER_VALIDATED, step=1
  Step[1]: payment.process.command
        |                                       |
        v                                       v (si falla)
  Payment Service                          CANCELLED + SAGA FAILED
  payment.completed ->                     (sin compensacion — no hay que revertir)
        |
        v
  Reservation: PAID, step=2
  Step[2]: fleet.confirm.command
        |                           |
        v                           v (si rechaza)
  Fleet Service                 CANCELLING + SAGA COMPENSATING
  fleet.confirmed ->            -> payment.refund.command
        |                           |
        v                           v
  CONFIRMED                    payment.refunded ->
  SAGA SUCCEEDED               CANCELLED + SAGA FAILED
```

## Mapa de Modulos (20)

```
+------------------------------------------------------------------+
|                    vehicle-rental-platform                        |
|                         (root POM)                               |
+------------------------------------------------------------------+
|                                                                  |
|  Shared                                                          |
|  +------------------+  +--------------------------------------+  |
|  |     common       |  |         common-messaging             |  |
|  |   31 tests       |  |   Outbox Pattern + RabbitMQ          |  |
|  |   Value Objects  |  |   5 tests                            |  |
|  |   Base Entity    |  |   OutboxPublisher, OutboxEvent,      |  |
|  |   AggregateRoot  |  |   OutboxCleanupScheduler,            |  |
|  +------------------+  |   MessageConverterConfig             |  |
|                         +--------------------------------------+  |
|                                                                  |
|  Customer Service (:8181)          Fleet Service (:8183)         |
|  +--------+--------+--------+     +--------+--------+--------+  |
|  | domain | app    | infra  |     | domain | app    | infra  |  |
|  | 66 T   | 21 T   | 0 T    |     | 63 T   | 22 T   | 0 T    |  |
|  +--------+--------+--------+     +--------+--------+--------+  |
|  | container        14 IT   |     | container        15 IT   |  |
|  | 1 @RabbitListener         |     | 2 @RabbitListeners        |  |
|  +---------------------------+     +---------------------------+  |
|                                                                  |
|  Reservation Service (:8182)       Payment Service (:8184)       |
|  +--------+--------+--------+     +--------+--------+--------+  |
|  | domain | app    | infra  |     | domain | app    | infra  |  |
|  | 110 T  | 38 T   | 0 T    |     | 51 T   | 18 T   | 0 T    |  |
|  +--------+--------+--------+     +--------+--------+--------+  |
|  | container        20 IT   |     | container        18 IT   |  |
|  | 7 @RabbitListeners (SAGA) |     | 2 @RabbitListeners        |  |
|  | SAGA Orchestrator          |     |                           |  |
|  +---------------------------+     +---------------------------+  |
|                                                                  |
|  T = unit tests    IT = integration tests                        |
|  Total: 492 tests (425 unit + 67 IT), 0 failures               |
+------------------------------------------------------------------+
```

## Topologia RabbitMQ

```
+------------------------------------------------------------------+
|                         RabbitMQ                                 |
+------------------------------------------------------------------+
|                                                                  |
|  Exchanges (topic)              Dead Letter Exchange (direct)    |
|  +---------------------+       +---------------------+          |
|  | reservation.exchange |       |    dlx.exchange     |          |
|  | customer.exchange    |       +----------+----------+          |
|  | payment.exchange     |                  |                     |
|  | fleet.exchange       |                  v                     |
|  +---------------------+       +---------------------+          |
|                                 | reservation.dlq    |          |
|  Response Queues (8)            | customer.dlq       |          |
|  consumidas por Orchestrator    | payment.dlq        |          |
|  +---------------------------+  | fleet.dlq          |          |
|  | customer.validated.queue  |  +---------------------+          |
|  | customer.rejected.queue   |                                   |
|  | payment.completed.queue   |  Command Queues (5) -- SAGA      |
|  | payment.failed.queue      |  consumidas por Participantes    |
|  | payment.refunded.queue    |  +--------------------------------+
|  | fleet.confirmed.queue     |  | customer.validate.command.queue |
|  | fleet.rejected.queue      |  | payment.process.command.queue  |
|  +---------------------------+  | payment.refund.command.queue   |
|                                 | fleet.confirm.command.queue    |
|  Event Queue (1)                | fleet.release.command.queue    |
|  +---------------------------+  +--------------------------------+
|  | reservation.created.queue |                                   |
|  +---------------------------+                                   |
|                                                                  |
|  @RabbitListeners: 12 (1 Customer + 2 Fleet + 2 Payment         |
|                        + 7 Reservation SAGA response)            |
|  Total: 5 exchanges, 18 queues, 28 bindings                     |
+------------------------------------------------------------------+
```

## Cobertura de Tests

```
Cobertura por capa (lineas):

  Domain          |==========================================| 96-100%
  Application     |=========================================|  98-100%
  Container (ITs) |==============================|            77-83%
  Common          |================================|            83-85%

  Detalles:
  +---------------------------+-------+-----------+
  | Modulo                    | Tests | Cobertura |
  +---------------------------+-------+-----------+
  | customer-domain           |    66 |     100%  |
  | reservation-domain        |   110 |     100%  |
  | payment-domain            |    51 |     100%  |
  | fleet-domain              |    63 |      96%  |
  | reservation-application   |    38 |     100%  |
  | customer-application      |    21 |      98%  |
  | fleet-application         |    22 |      98%  |
  | payment-application       |    18 |      98%  |
  | common                    |    31 |      85%  |
  | common-messaging          |     5 |      83%  |
  | customer-container        |    14 |      83%  |
  | fleet-container           |    15 |      83%  |
  | reservation-container     |    20 |      78%  |
  | payment-container         |    18 |      78%  |
  +---------------------------+-------+-----------+
  | TOTAL                     |   500 |           |
  +---------------------------+-------+-----------+

  JaCoCo permanente (sin profile). Umbrales: domain/common 80%,
  application 75%, infrastructure 60%. Containers excluidos.

  Bruno E2E: 4 requests, 6 assertions — happy path SAGA
  (PENDING → CONFIRMED) contra Docker Compose.
```

## Patrones Implementados

```
  +--------------------+--------------------------------------------+
  | Patron             | Estado                                     |
  +--------------------+--------------------------------------------+
  | Hexagonal Arch     | Completo - 4 servicios x 4 modulos         |
  | DDD Tactical       | Aggregates, VOs, Domain Events, Ports      |
  | Outbox Pattern     | Completo - 4 servicios con outbox           |
  | SAGA Orchestration | Completo - 3 steps, compensacion inversa   |
  | Database per Svc   | 4 PostgreSQL databases independientes      |
  | Event-Driven       | RabbitMQ con topic exchanges + DLQ         |
  | Typed IDs          | ReservationId, PaymentId, etc. (records)   |
  | Optimistic Locking | @Version en SagaState para concurrencia    |
  | Test Strategy      | Domain Test-First, Infra Test-After        |
  | JaCoCo Enforcement | Permanente: 80/75/60% por capa             |
  +--------------------+--------------------------------------------+
```

## Roadmap

```
  COMPLETADO                          PENDIENTE
  ==========                          =========

  COMPLETADO                          PENDIENTE
  ==========                          =========

  [x] 4 servicios hexagonales         [ ] MDC / Correlation ID
  [x] 4 capas por servicio            [ ] OpenAPI docs
  [x] Outbox en 4 servicios           [ ] E2E compensation flow
  [x] Topologia RabbitMQ completa
  [x] 12 @RabbitListeners
  [x] SAGA Orchestrator
  [x] Compensation flows
  [x] 507 tests pasando
  [x] JaCoCo permanente (80/75/60%)
  [x] ArchUnit hexagonal boundaries
  [x] Docker Compose (4 servicios)
  [x] Bruno E2E happy path SAGA
  [x] Database indexes (Flyway)
  [x] SAGA timeout/retry (diferido)
  [x] Idempotencia listeners (diferido)
```

## Stack Tecnologico

```
  Java 21 | Spring Boot 3.4.13 | Maven multi-module
  PostgreSQL | Flyway | Spring Data JPA
  RabbitMQ | Spring AMQP | Outbox Pattern
  JUnit 5 | Mockito | Testcontainers | Awaitility
  JaCoCo 0.8.12 (permanente, check con umbrales) | Lombok
  ArchUnit | Bruno CLI (E2E) | Docker Compose (Paketo images)
```
