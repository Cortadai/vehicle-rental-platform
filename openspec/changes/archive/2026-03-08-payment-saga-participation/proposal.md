## Why

Payment Service ya publica eventos via Outbox (change #16) pero no puede **recibir** comandos por RabbitMQ. Customer y Fleet ya tienen listeners para participar en la SAGA; Payment es el unico servicio participante sin esa capacidad. Sin esto, el orquestador SAGA (change #21) no puede enviar comandos de cobro ni de reembolso.

## What Changes

- Anadir `PaymentProcessListener` — recibe comandos de cobro desde `payment.process.command.queue` e invoca `ProcessPaymentUseCase`
- Anadir `PaymentRefundListener` — recibe comandos de reembolso desde `payment.refund.command.queue` e invoca `RefundPaymentUseCase`
- Extender `RabbitMQConfig` — declarar las 2 command queues como Spring beans (actualmente solo tiene event queues)
- Anadir tests de integracion para los listeners

**Nota**: A diferencia de Customer (#18) y Fleet (#19), Payment ya tiene toda la infraestructura de outbox y messaging. Este change es mas reducido — solo agrega la recepcion de comandos SAGA.

## Capabilities

### New Capabilities
- `payment-saga-listeners`: Listeners RabbitMQ para recibir comandos SAGA (process payment + refund payment) y delegarlos a los use cases existentes

### Modified Capabilities
- `payment-rabbitmq-topology`: Anadir declaracion de command queues (`payment.process.command.queue`, `payment.refund.command.queue`) como Spring beans en RabbitMQConfig. Las queues ya existen en `definitions.json` pero no estan declaradas como beans.

## Impact

- **payment-infrastructure**: 2 nuevos listeners + RabbitMQConfig extendido con command queues
- **payment-container**: Nuevos ITs para verificar que los listeners reciben y procesan comandos
- **Dependencias**: Ninguna nueva — `common-messaging` y `spring-boot-starter-amqp` ya estan en el classpath
- **Otros servicios**: Sin impacto — los use cases y eventos de Payment no cambian
