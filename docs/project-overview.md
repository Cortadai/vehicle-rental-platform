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
 |   |  Domain Events   |      |  Mappers             |
 |   |  Domain Ports    |      |                      |
 |   +------------------+      +---------------------+
 |            ^                          ^
 |            |                          |
 |   +--------+--------------------------+--------+
 |   |              infrastructure                 |
 |   |         (Spring components)                 |
 |   |                                             |
 |   |  REST Controllers    JPA Repositories       |
 |   |  Persistence Mapper  Event Publisher         |
 |   |  RabbitMQ Config     Payment Gateway (*)     |
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
 19 changes completados                              Fase actual
 ========================                            ==========

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
  #18 customer-outbox-and-messaging    }  Conectar al bus
  #19 fleet-outbox-and-messaging       }  Conectar al bus  <-- ESTAMOS AQUI

  --- Proximos pasos -------------------------------------------------

  #20 reservation-saga-orchestration   }  SAGA         <-- OBJETIVO
```

## Mapa de Modulos (19)

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
|  | OUTBOX -> RabbitMQ        |     | OUTBOX -> RabbitMQ        |  |
|  +---------------------------+     +---------------------------+  |
|                                                                  |
|  Reservation Service (:8182)       Payment Service (:8184)       |
|  +--------+--------+--------+     +--------+--------+--------+  |
|  | domain | app    | infra  |     | domain | app    | infra  |  |
|  | 80 T   | 18 T   | 0 T    |     | 51 T   | 18 T   | 0 T    |  |
|  +--------+--------+--------+     +--------+--------+--------+  |
|  | container        13 IT   |     | container        16 IT   |  |
|  | OUTBOX -> RabbitMQ        |     | OUTBOX -> RabbitMQ        |  |
|  +---------------------------+     +---------------------------+  |
|                                                                  |
|  T = unit tests    IT = integration tests                        |
|  Total: 433 tests, 0 failures                                   |
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
|  Event Queues (8)               | customer.dlq       |          |
|  +---------------------------+  | payment.dlq        |          |
|  | reservation.created.queue |  | fleet.dlq          |          |
|  | customer.validated.queue  |  +---------------------+          |
|  | customer.rejected.queue   |                                   |
|  | payment.completed.queue   |  Command Queues (5) -- SAGA      |
|  | payment.failed.queue      |  +--------------------------------+
|  | payment.refunded.queue    |  | customer.validate.command.queue |
|  | fleet.confirmed.queue     |  | payment.process.command.queue  |
|  | fleet.rejected.queue      |  | payment.refund.command.queue   |
|  +---------------------------+  | fleet.confirm.command.queue    |
|                                 | fleet.release.command.queue    |
|                                 +--------------------------------+
|  Total: 5 exchanges, 17 queues, 26 bindings                     |
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
  | reservation-domain        |    80 |     100%  |
  | payment-domain            |    51 |     100%  |
  | fleet-domain              |    63 |      96%  |
  | reservation-application   |    18 |     100%  |
  | customer-application      |    21 |      98%  |
  | fleet-application         |    22 |      98%  |
  | payment-application       |    18 |      98%  |
  | common                    |    31 |      85%  |
  | common-messaging          |     5 |      83%  |
  | customer-container        |    14 |      83%  |
  | fleet-container           |    15 |      83%  |
  | reservation-container     |    13 |      78%  |
  | payment-container         |    16 |      78%  |
  +---------------------------+-------+-----------+
  | TOTAL                     |   433 |           |
  +---------------------------+-------+-----------+

  Generado con: mvn clean verify -Pcoverage
```

## Patrones Implementados

```
  +--------------------+--------------------------------------------+
  | Patron             | Estado                                     |
  +--------------------+--------------------------------------------+
  | Hexagonal Arch     | Completo - 4 servicios x 4 modulos         |
  | DDD Tactical       | Aggregates, VOs, Domain Events, Ports      |
  | Outbox Pattern     | Completo - 4 servicios con outbox           |
  | SAGA Orchestration | Pendiente - topologia lista                |
  | Database per Svc   | 4 PostgreSQL databases independientes      |
  | Event-Driven       | RabbitMQ con topic exchanges + DLQ         |
  | Typed IDs          | ReservationId, PaymentId, etc. (records)   |
  | Test Strategy      | Domain Test-First, Infra Test-After        |
  +--------------------+--------------------------------------------+
```

## Roadmap

```
  COMPLETADO                          PENDIENTE
  ==========                          =========

  [x] 4 servicios hexagonales         [ ] SAGA Orchestrator
  [x] 4 capas por servicio            [ ] Compensation flows
  [x] Outbox en 4 servicios           [ ] MDC / Correlation ID
  [x] Topologia RabbitMQ completa     [ ] ArchUnit tests
  [x] Command queues para SAGA (5)    [ ] OpenAPI docs
  [x] 3 @RabbitListeners (consumers)
  [x] 433 tests pasando
  [x] JaCoCo (profile)
```

## Stack Tecnologico

```
  Java 21 | Spring Boot 3.4.1 | Maven multi-module
  PostgreSQL | Flyway | Spring Data JPA
  RabbitMQ | Spring AMQP | Outbox Pattern
  JUnit 5 | Mockito | Testcontainers | Awaitility
  JaCoCo (via -Pcoverage) | Lombok
```
