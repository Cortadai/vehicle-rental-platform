# Área 13: Mensajería Asíncrona - RabbitMQ Patterns

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, RabbitMQ 3.13+, Spring AMQP

---

## 1. Conceptos Fundamentales

### Referencia Rápida (Seniors)

```
Producer → Exchange → Binding (routing key) → Queue → Consumer

Tipos de Exchange:
  - Direct:  routing key exacto
  - Topic:   routing key con wildcards (* y #)
  - Fanout:  broadcast a todas las queues
  - Headers: match por headers del mensaje
```

### Guía Detallada (Junior/Mid)

#### Modelo AMQP

```
┌──────────┐    ┌───────────┐    ┌─────────┐    ┌───────────┐
│ Producer │───▶│ Exchange  │───▶│  Queue   │───▶│ Consumer  │
└──────────┘    └───────────┘    └─────────┘    └───────────┘
                     │                │
                     └── Binding ─────┘
                      (routing key)
```

- **Producer**: Publica mensajes a un Exchange, nunca directamente a una Queue.
- **Exchange**: Recibe mensajes y los enruta a una o más Queues según reglas de binding.
- **Binding**: Relación entre un Exchange y una Queue con una routing key.
- **Queue**: Buffer que almacena mensajes hasta que un Consumer los procesa.
- **Consumer**: Suscriptor que recibe y procesa mensajes de una Queue.
- **Routing Key**: Clave que el Exchange usa para decidir a qué Queue enviar el mensaje.

#### Tipos de Exchange

| Tipo | Routing | Caso de Uso | Ejemplo E-commerce |
|------|---------|-------------|-------------------|
| **Direct** | Routing key exacto | Punto a punto, tareas específicas | `order.created` va a la queue de procesamiento de pedidos |
| **Topic** | Wildcards (`*` = una palabra, `#` = cero o más) | Enrutamiento flexible por categoría | `order.created.#` captura `order.created.premium`, `order.created.standard` |
| **Fanout** | Ignora routing key, broadcast | Notificaciones a múltiples servicios | Evento de pedido creado va a inventario, email, analytics |
| **Headers** | Match por headers del mensaje | Routing complejo sin routing key | Enrutar por `region=eu` y `priority=high` |

#### Cuándo usar cada Exchange

| Escenario | Exchange Recomendado | Razón |
|-----------|---------------------|-------|
| Un productor, un consumidor | Direct | Enrutamiento simple 1:1 |
| Un evento, múltiples consumidores | Fanout | Broadcast sin filtro |
| Enrutamiento por categoría jerárquica | Topic | Wildcards permiten flexibilidad |
| Routing basado en múltiples atributos | Headers | No depende de routing key |
| Trabajo distribuido (work queue) | Direct (default exchange) | Round-robin entre consumers |

#### Propiedades de Queue

```java
// Queue durable: sobrevive reinicios del broker
QueueBuilder.durable("order.created.queue")

// Queue exclusiva: solo una conexión, se elimina al desconectar
QueueBuilder.nonDurable("temp.queue").exclusive()

// Queue con TTL: mensajes expiran después de 60 segundos
QueueBuilder.durable("order.pending.queue")
    .withArgument("x-message-ttl", 60000)

// Queue con límite de longitud
QueueBuilder.durable("order.notifications.queue")
    .withArgument("x-max-length", 10000)
```

---

## 2. Spring Boot Configuration

### Referencia Rápida (Seniors)

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    listener:
      simple:
        acknowledge-mode: auto
        concurrency: 3
        max-concurrency: 10
        prefetch: 10
        retry:
          enabled: true
          initial-interval: 1s
          max-attempts: 3
          multiplier: 2.0
        default-requeue-rejected: false
```

### Guía Detallada (Junior/Mid)

#### Dependencias Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Para testing con Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

#### Configuración Completa application.yml

```yaml
spring:
  rabbitmq:
    # --- Conexión ---
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /

    # --- SSL (producción) ---
    # ssl:
    #   enabled: true
    #   key-store: classpath:keystore.p12
    #   key-store-password: ${RABBITMQ_KEYSTORE_PASSWORD}

    # --- Listener (consumidores) ---
    listener:
      simple:
        # auto: Spring maneja ack/nack automáticamente
        # manual: tu código debe hacer channel.basicAck()
        # none: sin acknowledge (mensajes se pierden si el consumer falla)
        acknowledge-mode: auto

        # Número de consumers concurrentes (threads)
        concurrency: 3
        max-concurrency: 10

        # Cuántos mensajes enviar al consumer antes de esperar ack
        prefetch: 10

        # --- Reintentos ---
        retry:
          enabled: true
          initial-interval: 1s    # Espera inicial entre reintentos
          max-attempts: 3          # Máximo número de intentos
          multiplier: 2.0          # Factor multiplicador (1s, 2s, 4s)
          max-interval: 10s        # Intervalo máximo entre reintentos

        # No reencolar mensajes rechazados (enviar a DLQ)
        default-requeue-rejected: false

    # --- Template (publicación) ---
    template:
      exchange: ""                 # Exchange por defecto
      routing-key: ""              # Routing key por defecto
      retry:
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        multiplier: 2.0
```

#### Explicación de acknowledge-mode

| Modo | Comportamiento | Cuándo Usar |
|------|---------------|-------------|
| `auto` | Spring hace ack si el listener retorna sin error, nack si lanza excepción | Mayoría de los casos |
| `manual` | Tu código llama `channel.basicAck()` / `channel.basicNack()` | Control fino sobre el acknowledgment |
| `none` | Ack inmediato al recibir, sin importar si se procesó | Solo si perder mensajes es aceptable |

---

## 3. Declaring Exchanges, Queues, and Bindings

### Referencia Rápida (Seniors)

```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange("order.exchange")
            .durable(true).build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable("order.created.queue")
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .withArgument("x-dead-letter-routing-key", "order.created.dlq")
            .build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
            .to(orderExchange())
            .with("order.created.#");
    }
}
```

### Guía Detallada (Junior/Mid)

#### Estructura de Paquetes

```
com.acme.ecommerce.infrastructure.messaging/
├── config/
│   ├── RabbitMQConfig.java
│   ├── RabbitMQExchangeConfig.java
│   └── RabbitMQQueueConfig.java
├── converter/
│   └── MessageConverterConfig.java
├── publisher/
│   ├── OrderEventPublisher.java
│   └── InventoryEventPublisher.java
├── consumer/
│   ├── OrderCreatedConsumer.java
│   ├── PaymentProcessedConsumer.java
│   └── InventoryReservedConsumer.java
└── model/
    ├── OrderCreatedEvent.java
    ├── PaymentProcessedEvent.java
    └── InventoryReservedEvent.java
```

#### Configuración Completa de Exchanges y Queues

```java
package com.acme.ecommerce.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // =============================================
    // Constantes
    // =============================================

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_CREATED_INVENTORY_QUEUE = "order.created.inventory.queue";
    public static final String ORDER_CREATED_NOTIFICATION_QUEUE = "order.created.notification.queue";
    public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";
    public static final String ORDER_CREATED_DLQ = "order.created.dlq";

    public static final String ORDER_CREATED_KEY = "order.created";

    // =============================================
    // Exchanges
    // =============================================

    /**
     * Topic Exchange para eventos de pedidos.
     * Permite enrutamiento flexible con wildcards.
     */
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Direct Exchange para pagos.
     * Routing key exacto para cada tipo de evento.
     */
    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder.directExchange(PAYMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Fanout Exchange para notificaciones.
     * Broadcast a todos los servicios suscritos.
     */
    @Bean
    public FanoutExchange notificationExchange() {
        return ExchangeBuilder.fanoutExchange(NOTIFICATION_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Exchange para mensajes fallidos.
     */
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    // =============================================
    // Queues
    // =============================================

    /**
     * Queue principal para procesamiento de pedidos creados.
     * Configura DLX para mensajes que fallan después de reintentos.
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.created.dlq")
                .build();
    }

    /**
     * Queue para que el servicio de inventario procese pedidos nuevos.
     */
    @Bean
    public Queue orderCreatedInventoryQueue() {
        return QueueBuilder.durable(ORDER_CREATED_INVENTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.created.inventory.dlq")
                .build();
    }

    /**
     * Queue para que el servicio de notificaciones envíe confirmación al cliente.
     */
    @Bean
    public Queue orderCreatedNotificationQueue() {
        return QueueBuilder.durable(ORDER_CREATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.created.notification.dlq")
                .build();
    }

    /**
     * Queue de pagos procesados.
     */
    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "payment.processed.dlq")
                .build();
    }

    /**
     * Dead Letter Queue para pedidos creados que fallaron.
     */
    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    // =============================================
    // Bindings
    // =============================================

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with("order.created.#");
    }

    @Bean
    public Binding orderCreatedInventoryBinding() {
        return BindingBuilder.bind(orderCreatedInventoryQueue())
                .to(orderExchange())
                .with("order.created.#");
    }

    @Bean
    public Binding orderCreatedNotificationBinding() {
        return BindingBuilder.bind(orderCreatedNotificationQueue())
                .to(orderExchange())
                .with("order.created.#");
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder.bind(paymentProcessedQueue())
                .to(paymentExchange())
                .with("payment.processed");
    }

    @Bean
    public Binding orderCreatedDlqBinding() {
        return BindingBuilder.bind(orderCreatedDlq())
                .to(dlxExchange())
                .with("order.created.dlq");
    }
}
```

#### Configuración del Message Converter

```java
package com.acme.ecommerce.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageConverterConfig {

    /**
     * Converter JSON para serializar/deserializar mensajes automáticamente.
     * Usa Jackson con soporte para Java Time API (Instant, LocalDateTime, etc.).
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate configurado con el converter JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
```

---

## 4. Publishing Messages

### Referencia Rápida (Seniors)

```java
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend("order.exchange", "order.created", event,
            message -> {
                message.getMessageProperties().setCorrelationId(event.correlationId());
                message.getMessageProperties().setHeader("X-Event-Type", "ORDER_CREATED");
                return message;
            });
    }
}
```

### Guía Detallada (Junior/Mid)

#### Modelo de Eventos

```java
package com.acme.ecommerce.infrastructure.messaging.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Evento publicado cuando se crea un nuevo pedido.
 * Inmutable (record) para garantizar thread-safety en mensajería.
 */
public record OrderCreatedEvent(
    String eventId,
    String correlationId,
    Instant timestamp,
    Long orderId,
    Long customerId,
    String customerEmail,
    List<OrderItemEvent> items,
    BigDecimal totalAmount,
    String currency
) {
    /**
     * Factory method con generación automática de eventId y timestamp.
     */
    public static OrderCreatedEvent of(
            Long orderId, Long customerId, String customerEmail,
            List<OrderItemEvent> items, BigDecimal totalAmount) {
        return new OrderCreatedEvent(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            Instant.now(),
            orderId, customerId, customerEmail,
            items, totalAmount, "USD"
        );
    }
}

public record OrderItemEvent(
    String productId,
    String productName,
    int quantity,
    BigDecimal unitPrice
) {}
```

#### Publisher Completo con Headers y Correlation ID

```java
package com.acme.ecommerce.infrastructure.messaging.publisher;

import com.acme.ecommerce.infrastructure.messaging.config.RabbitMQConfig;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica evento de pedido creado al exchange de pedidos.
     * Incluye correlation ID para trazabilidad end-to-end.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent: orderId={}, correlationId={}",
                event.orderId(), event.correlationId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_KEY,
                event,
                message -> {
                    var properties = message.getMessageProperties();
                    properties.setCorrelationId(event.correlationId());
                    properties.setMessageId(event.eventId());
                    properties.setHeader("X-Event-Type", "ORDER_CREATED");
                    properties.setHeader("X-Source-Service", "order-service");
                    properties.setContentType("application/json");
                    return message;
                }
        );

        log.debug("OrderCreatedEvent published successfully: {}", event.eventId());
    }
}
```

#### Publicación Transaccional

```java
package com.acme.ecommerce.application.service;

import com.acme.ecommerce.domain.model.Order;
import com.acme.ecommerce.domain.repository.OrderRepository;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import com.acme.ecommerce.infrastructure.messaging.model.OrderItemEvent;
import com.acme.ecommerce.infrastructure.messaging.publisher.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Crea un pedido y publica el evento.
     * NOTA: La publicación del mensaje NO es transaccional con la DB.
     * Para garantía, usar Transactional Outbox Pattern (ver sección 9).
     */
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        // 1. Validar y crear la orden
        Order order = Order.create(
                command.customerId(),
                command.items(),
                command.shippingAddress()
        );

        // 2. Persistir en la base de datos
        Order savedOrder = orderRepository.save(order);

        // 3. Publicar evento (después del save para tener el ID)
        OrderCreatedEvent event = OrderCreatedEvent.of(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getCustomerEmail(),
                savedOrder.getItems().stream()
                        .map(item -> new OrderItemEvent(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice()))
                        .toList(),
                savedOrder.getTotalAmount()
        );

        orderEventPublisher.publishOrderCreated(event);

        log.info("Order created and event published: orderId={}", savedOrder.getId());
        return savedOrder;
    }
}
```

---

## 5. Consuming Messages

### Referencia Rápida (Seniors)

```java
@Component
@Slf4j
public class OrderCreatedConsumer {

    @RabbitListener(queues = "order.created.inventory.queue", concurrency = "3-5")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing order: {}", event.orderId());
        // Lógica de negocio
    }
}
```

### Guía Detallada (Junior/Mid)

#### Consumer Básico con @RabbitListener

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import com.acme.ecommerce.application.service.InventoryService;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedInventoryConsumer {

    private final InventoryService inventoryService;

    /**
     * Consume eventos de pedido creado para reservar inventario.
     * Spring deserializa automáticamente el JSON a OrderCreatedEvent
     * gracias al Jackson2JsonMessageConverter configurado.
     *
     * concurrency = "3-5": mínimo 3 threads, máximo 5.
     */
    @RabbitListener(
            queues = "${app.rabbitmq.queues.order-created-inventory:order.created.inventory.queue}",
            concurrency = "3-5"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, correlationId={}",
                event.orderId(), event.correlationId());

        try {
            inventoryService.reserveInventory(event.orderId(), event.items());
            log.info("Inventory reserved for order: {}", event.orderId());

        } catch (Exception e) {
            log.error("Failed to reserve inventory for order: {}",
                    event.orderId(), e);
            // La excepción propaga → Spring hace nack → reintento o DLQ
            throw e;
        }
    }
}
```

#### Consumer con Acceso a Message y Headers

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCreatedNotificationConsumer {

    /**
     * Consumer que accede al payload y headers del mensaje.
     * @Payload: el cuerpo del mensaje deserializado.
     * @Header: headers individuales del mensaje.
     */
    @RabbitListener(queues = "order.created.notification.queue")
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(value = "X-Event-Type", required = false) String eventType,
            @Header(value = "amqp_correlationId", required = false) String correlationId,
            Message rawMessage) {

        log.info("Received notification event: type={}, correlationId={}, orderId={}",
                eventType, correlationId, event.orderId());

        // Acceso al raw message para metadata adicional
        var properties = rawMessage.getMessageProperties();
        log.debug("Message received at: {}, delivery tag: {}",
                properties.getTimestamp(), properties.getDeliveryTag());

        // Enviar email de confirmación
        sendOrderConfirmationEmail(event);
    }

    private void sendOrderConfirmationEmail(OrderCreatedEvent event) {
        log.info("Sending confirmation email to: {} for order: {}",
                event.customerEmail(), event.orderId());
        // Lógica de envío de email
    }
}
```

#### Consumer con Acknowledgment Manual

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import com.acme.ecommerce.infrastructure.messaging.model.PaymentProcessedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumer con acknowledgment manual para control total sobre
 * cuándo se confirma el procesamiento del mensaje.
 *
 * Requiere: spring.rabbitmq.listener.simple.acknowledge-mode=manual
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessedConsumer {

    @RabbitListener(queues = "payment.processed.queue")
    public void handlePaymentProcessed(
            @Payload PaymentProcessedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        try {
            log.info("Processing payment event: orderId={}, amount={}",
                    event.orderId(), event.amount());

            // Lógica de negocio...
            processPayment(event);

            // Ack exitoso: el mensaje se elimina de la queue
            channel.basicAck(deliveryTag, false);
            log.info("Payment processed and acknowledged: orderId={}", event.orderId());

        } catch (Exception e) {
            log.error("Payment processing failed: orderId={}", event.orderId(), e);

            // Nack: false = no reencolar (irá al DLQ si está configurado)
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void processPayment(PaymentProcessedEvent event) {
        // Lógica de procesamiento de pago
    }
}
```

---

## 6. Dead Letter Queues (DLQ)

### Referencia Rápida (Seniors)

```java
// Queue con DLX
QueueBuilder.durable("order.created.queue")
    .withArgument("x-dead-letter-exchange", "dlx.exchange")
    .withArgument("x-dead-letter-routing-key", "order.created.dlq")
    .build();

// Rechazar sin reencolar (enviar a DLQ)
throw new AmqpRejectAndDontRequeueException("Processing failed permanently");
```

### Guía Detallada (Junior/Mid)

#### Flujo de Dead Letter Queue

```
┌─────────────┐     ┌──────────┐     ┌──────────────┐
│   Producer   │────▶│  Queue   │────▶│   Consumer   │
└─────────────┘     └──────────┘     └──────────────┘
                         │                   │
                         │            (excepción después
                         │             de N reintentos)
                         │                   │
                         ▼                   ▼
                    ┌──────────┐     ┌──────────────┐
                    │   DLX    │◀────│ nack/reject  │
                    │ Exchange │     └──────────────┘
                    └──────────┘
                         │
                         ▼
                    ┌──────────┐     ┌──────────────┐
                    │   DLQ    │────▶│ DLQ Consumer │
                    │  (Dead   │     │ (monitoreo / │
                    │  Letter) │     │ reproceso)   │
                    └──────────┘     └──────────────┘
```

#### Configuración Completa de DLQ

```java
package com.acme.ecommerce.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeadLetterConfig {

    public static final String DLX_EXCHANGE = "dlx.exchange";

    // --- Dead Letter Exchange ---
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    // --- Dead Letter Queues ---
    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable("order.created.dlq")
                // TTL opcional: mensajes en DLQ expiran después de 7 días
                .withArgument("x-message-ttl", 604800000)
                .build();
    }

    @Bean
    public Queue paymentProcessedDlq() {
        return QueueBuilder.durable("payment.processed.dlq")
                .withArgument("x-message-ttl", 604800000)
                .build();
    }

    // --- Parking Lot Queue (para mensajes que fallaron reproceso) ---
    @Bean
    public Queue parkingLotQueue() {
        return QueueBuilder.durable("parking-lot.queue").build();
    }

    // --- Bindings ---
    @Bean
    public Binding orderCreatedDlqBinding() {
        return BindingBuilder.bind(orderCreatedDlq())
                .to(dlxExchange())
                .with("order.created.dlq");
    }

    @Bean
    public Binding paymentProcessedDlqBinding() {
        return BindingBuilder.bind(paymentProcessedDlq())
                .to(dlxExchange())
                .with("payment.processed.dlq");
    }
}
```

#### Consumer de DLQ para Monitoreo

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Monitorea la DLQ de pedidos creados.
     * Opciones: logging, alerta, reproceso, o parking lot.
     */
    @RabbitListener(queues = "order.created.dlq")
    public void handleDeadLetter(Message message) {
        var properties = message.getMessageProperties();
        Map<String, Object> headers = properties.getHeaders();

        // Obtener información de la muerte del mensaje
        List<Map<String, Object>> xDeath =
                (List<Map<String, Object>>) headers.get("x-death");

        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, Object> firstDeath = xDeath.get(0);
            String originalQueue = (String) firstDeath.get("queue");
            String reason = (String) firstDeath.get("reason");
            Long count = (Long) firstDeath.get("count");

            log.error("Dead letter received: queue={}, reason={}, count={}, messageId={}",
                    originalQueue, reason, count, properties.getMessageId());
        }

        // Decidir qué hacer con el mensaje
        long deathCount = getDeathCount(xDeath);

        if (deathCount <= 3) {
            // Intentar reprocesar
            log.info("Reprocessing dead letter, attempt: {}", deathCount);
            reprocessMessage(message);
        } else {
            // Enviar a Parking Lot (ya no intentar más)
            log.warn("Moving message to parking lot after {} deaths", deathCount);
            rabbitTemplate.send("", "parking-lot.queue", message);
        }
    }

    private long getDeathCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) return 0;
        return (Long) xDeath.get(0).getOrDefault("count", 0L);
    }

    private void reprocessMessage(Message message) {
        // Reenviar al exchange original con un delay
        String originalExchange = message.getMessageProperties()
                .getReceivedExchange();
        String originalRoutingKey = message.getMessageProperties()
                .getReceivedRoutingKey();

        rabbitTemplate.send(originalExchange, originalRoutingKey, message);
    }
}
```

---

## 7. Retry Patterns

### Referencia Rápida (Seniors)

```yaml
# Retry automático en listener (Spring Retry)
spring.rabbitmq.listener.simple.retry:
  enabled: true
  initial-interval: 1s
  max-attempts: 3
  multiplier: 2.0
  max-interval: 10s
```

```java
// Custom recovery: enviar a DLQ cuando se agotan reintentos
@Bean
public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
    return new RepublishMessageRecoverer(rabbitTemplate, "dlx.exchange", "order.created.dlq");
}
```

### Guía Detallada (Junior/Mid)

#### Retry con Spring Retry (Automático)

Cuando `spring.rabbitmq.listener.simple.retry.enabled=true`, Spring Retry intercepta
las excepciones del listener y reintenta localmente (sin devolver el mensaje al broker).

```
Intento 1: falla → espera 1s
Intento 2: falla → espera 2s (1s * 2.0)
Intento 3: falla → espera 4s (2s * 2.0, pero max-interval=10s)
Agotado: → MessageRecoverer decide qué hacer
```

#### Custom RecoveryCallback

```java
package com.acme.ecommerce.infrastructure.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RetryConfig {

    /**
     * Cuando se agotan los reintentos de Spring Retry,
     * RepublishMessageRecoverer envía el mensaje al DLX con
     * información adicional en los headers (exception, stack trace).
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(
                rabbitTemplate,
                DeadLetterConfig.DLX_EXCHANGE,
                "order.created.dlq"
        );
    }
}
```

#### Retry Condicional: Cuándo Reintentar vs Cuándo Enviar a DLQ

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    /**
     * Estrategia de reintentos:
     * - Errores transitorios (red, timeout): REINTENTAR
     * - Errores de validación (datos inválidos): DLQ directo
     * - Errores de negocio (stock insuficiente): DLQ directo
     */
    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            validateEvent(event);
            processOrder(event);
        } catch (TransientException e) {
            // Excepción normal → Spring Retry reintenta automáticamente
            log.warn("Transient error processing order {}, will retry: {}",
                    event.orderId(), e.getMessage());
            throw e;
        } catch (ValidationException | BusinessException e) {
            // Enviar directo a DLQ, no tiene sentido reintentar
            log.error("Non-retryable error for order {}: {}",
                    event.orderId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException(
                    "Non-retryable: " + e.getMessage(), e);
        }
    }

    private void validateEvent(OrderCreatedEvent event) {
        if (event.orderId() == null) {
            throw new ValidationException("Order ID is required");
        }
        if (event.items() == null || event.items().isEmpty()) {
            throw new ValidationException("Order must have at least one item");
        }
    }

    private void processOrder(OrderCreatedEvent event) {
        // Lógica de procesamiento que puede lanzar TransientException
    }
}
```

#### Tabla de Decisión: Reintentar vs DLQ

| Tipo de Error | Reintentar | DLQ Directo | Ejemplo |
|---------------|:----------:|:-----------:|---------|
| Timeout de red | Si | - | Servicio externo no responde |
| Base de datos temporalmente caída | Si | - | Connection pool agotado |
| JSON inválido / deserialización | - | Si | Mensaje corrupto |
| Validación de negocio | - | Si | Producto no existe |
| NullPointerException | - | Si | Bug en el código |
| Rate limit externo | Si | - | API de pagos limita requests |
| Datos duplicados | - | Si | Pedido ya procesado |

---

## 8. Idempotent Consumers

### Referencia Rápida (Seniors)

```java
@RabbitListener(queues = "order.created.queue")
public void handleOrderCreated(OrderCreatedEvent event) {
    if (idempotencyService.isProcessed(event.eventId())) {
        log.info("Event already processed: {}", event.eventId());
        return;
    }
    processOrder(event);
    idempotencyService.markAsProcessed(event.eventId());
}
```

### Guía Detallada (Junior/Mid)

#### Por qué Idempotencia

En mensajería asíncrona, un mensaje puede entregarse más de una vez por:
- Rebalanceo de consumers
- Timeout antes de recibir el ack
- Reintento manual desde DLQ
- Fallo de red después de procesar pero antes de hacer ack

El consumer debe ser **idempotente**: procesar el mismo mensaje N veces produce el mismo resultado que procesarlo 1 vez.

#### Tabla de Deduplicación en Base de Datos

```sql
-- Tabla para tracking de mensajes procesados
CREATE TABLE processed_messages (
    event_id        VARCHAR(36) PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumer_name   VARCHAR(100) NOT NULL
);

-- Índice para limpieza periódica de registros antiguos
CREATE INDEX idx_processed_messages_date ON processed_messages(processed_at);
```

#### Servicio de Idempotencia con Base de Datos

```java
package com.acme.ecommerce.infrastructure.messaging.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Verifica si un evento ya fue procesado.
     */
    public boolean isProcessed(String eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_messages WHERE event_id = ?",
                Integer.class, eventId
        );
        return count != null && count > 0;
    }

    /**
     * Marca un evento como procesado.
     * Retorna false si ya existía (duplicado).
     */
    @Transactional
    public boolean markAsProcessed(String eventId, String eventType, String consumerName) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO processed_messages (event_id, event_type, processed_at, consumer_name) VALUES (?, ?, ?, ?)",
                    eventId, eventType, Instant.now(), consumerName
            );
            return true;
        } catch (DuplicateKeyException e) {
            log.info("Event already processed (duplicate): eventId={}", eventId);
            return false;
        }
    }

    /**
     * Limpia registros antiguos (ejecutar periódicamente con @Scheduled).
     */
    @Transactional
    public int cleanOldRecords(int daysToKeep) {
        return jdbcTemplate.update(
                "DELETE FROM processed_messages WHERE processed_at < NOW() - INTERVAL ? DAY",
                daysToKeep
        );
    }
}
```

#### Consumer Idempotente Completo

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import com.acme.ecommerce.application.service.InventoryService;
import com.acme.ecommerce.infrastructure.messaging.idempotency.IdempotencyService;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentOrderConsumer {

    private final IdempotencyService idempotencyService;
    private final InventoryService inventoryService;

    /**
     * Consumer idempotente: mismo mensaje procesado múltiples veces
     * produce el mismo resultado.
     *
     * El markAsProcessed y la lógica de negocio se ejecutan en la
     * misma transacción para garantizar consistencia.
     */
    @RabbitListener(queues = "order.created.inventory.queue")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String eventId = event.eventId();

        // 1. Verificar idempotencia
        if (idempotencyService.isProcessed(eventId)) {
            log.info("Skipping duplicate event: eventId={}, orderId={}",
                    eventId, event.orderId());
            return;
        }

        // 2. Procesar lógica de negocio
        log.info("Processing order for inventory: orderId={}", event.orderId());
        inventoryService.reserveInventory(event.orderId(), event.items());

        // 3. Marcar como procesado (misma transacción)
        idempotencyService.markAsProcessed(
                eventId,
                "ORDER_CREATED",
                "inventory-consumer"
        );

        log.info("Order processed and marked idempotent: orderId={}, eventId={}",
                event.orderId(), eventId);
    }
}
```

#### Idempotencia con Redis (Alta Performance)

```java
package com.acme.ecommerce.infrastructure.messaging.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "msg:processed:";
    private static final Duration TTL = Duration.ofDays(7);

    /**
     * Intenta marcar como procesado usando SET NX (set if not exists).
     * Retorna true si es la primera vez (no duplicado).
     * Retorna false si ya fue procesado (duplicado).
     *
     * Atómico y thread-safe gracias a Redis SETNX.
     */
    public boolean tryMarkAsProcessed(String eventId) {
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * Verifica si un evento ya fue procesado.
     */
    public boolean isProcessed(String eventId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(KEY_PREFIX + eventId)
        );
    }
}
```

---

## 9. Event-Driven Patterns

### Referencia Rápida (Seniors)

```java
// Transactional Outbox Pattern
@Transactional
public Order createOrder(CreateOrderCommand command) {
    Order order = orderRepository.save(buildOrder(command));
    outboxRepository.save(new OutboxEvent("ORDER_CREATED", toJson(order)));
    return order;
    // Un scheduler/CDC publica los eventos del outbox a RabbitMQ
}
```

### Guía Detallada (Junior/Mid)

#### Problema: Dual Write

Al guardar en la DB y publicar en RabbitMQ, puede ocurrir:
1. DB commit exitoso, pero publicación a RabbitMQ falla = evento perdido
2. Publicación a RabbitMQ exitosa, pero DB commit falla = evento fantasma

```
┌─────────────────────────────────────────────────┐
│          PROBLEMA: DUAL WRITE                    │
│                                                  │
│  @Transactional                                  │
│  void createOrder() {                            │
│      orderRepo.save(order);    ← DB commit OK    │
│      publisher.publish(event); ← MQ FALLA!       │
│  }                             → Inconsistencia  │
└─────────────────────────────────────────────────┘
```

#### Solución: Transactional Outbox Pattern

```
┌──────────────────────────────────────────────────────────────┐
│            TRANSACTIONAL OUTBOX PATTERN                       │
│                                                              │
│  @Transactional (una sola transacción DB)                    │
│  ┌───────────────────────────────┐                           │
│  │ 1. Save Order                 │                           │
│  │ 2. Save OutboxEvent           │ ── Misma transacción      │
│  └───────────────────────────────┘                           │
│                                                              │
│  Scheduler (cada 100ms - 1s)                                 │
│  ┌───────────────────────────────┐    ┌─────────────┐        │
│  │ 3. Read pending OutboxEvents  │───▶│  RabbitMQ   │        │
│  │ 4. Publish to RabbitMQ        │    └─────────────┘        │
│  │ 5. Mark as published          │                           │
│  └───────────────────────────────┘                           │
└──────────────────────────────────────────────────────────────┘
```

#### Implementación del Outbox

```java
package com.acme.ecommerce.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;  // "ORDER", "PAYMENT"

    @Column(nullable = false)
    private String aggregateId;    // "12345"

    @Column(nullable = false)
    private String eventType;      // "ORDER_CREATED"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;        // JSON del evento

    @Column(nullable = false)
    private String routingKey;     // "order.created"

    @Column(nullable = false)
    private String exchange;       // "order.exchange"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;   // PENDING, PUBLISHED, FAILED

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    private int retryCount;

    public enum OutboxStatus {
        PENDING, PUBLISHED, FAILED
    }
}
```

#### Repository del Outbox

```java
package com.acme.ecommerce.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxEvent.OutboxStatus status);

    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :before")
    int deletePublishedBefore(Instant before);
}
```

#### Scheduler que Publica Eventos del Outbox

```java
package com.acme.ecommerce.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Cada 500ms, lee eventos pendientes del outbox y los publica
     * a RabbitMQ. Si la publicación falla, incrementa el retry count.
     */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.OutboxStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                rabbitTemplate.convertAndSend(
                        event.getExchange(),
                        event.getRoutingKey(),
                        event.getPayload(),
                        message -> {
                            message.getMessageProperties()
                                    .setMessageId(event.getId().toString());
                            message.getMessageProperties()
                                    .setHeader("X-Aggregate-Type", event.getAggregateType());
                            message.getMessageProperties()
                                    .setHeader("X-Aggregate-Id", event.getAggregateId());
                            return message;
                        }
                );

                event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);

                log.debug("Outbox event published: id={}, type={}",
                        event.getId(), event.getEventType());

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    log.error("Outbox event permanently failed: id={}", event.getId(), e);
                }
                outboxRepository.save(event);
            }
        }
    }

    /**
     * Limpia eventos publicados con más de 7 días de antigüedad.
     */
    @Scheduled(cron = "0 0 3 * * *")  // Cada día a las 3 AM
    @Transactional
    public void cleanOldEvents() {
        int deleted = outboxRepository.deletePublishedBefore(
                Instant.now().minusSeconds(7 * 24 * 3600));
        log.info("Cleaned {} old outbox events", deleted);
    }
}
```

#### Uso del Outbox en el Service

```java
package com.acme.ecommerce.application.service;

import com.acme.ecommerce.domain.model.Order;
import com.acme.ecommerce.domain.repository.OrderRepository;
import com.acme.ecommerce.infrastructure.messaging.config.RabbitMQConfig;
import com.acme.ecommerce.infrastructure.outbox.OutboxEvent;
import com.acme.ecommerce.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceWithOutbox {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea un pedido y guarda el evento en el outbox en la MISMA transacción.
     * El OutboxPublisher se encargará de publicar a RabbitMQ.
     */
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        // 1. Crear y persistir la orden
        Order order = Order.create(
                command.customerId(),
                command.items(),
                command.shippingAddress()
        );
        Order savedOrder = orderRepository.save(order);

        // 2. Guardar evento en outbox (misma transacción)
        try {
            String payload = objectMapper.writeValueAsString(
                    OrderCreatedEvent.from(savedOrder));

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(savedOrder.getId().toString())
                    .eventType("ORDER_CREATED")
                    .payload(payload)
                    .routingKey(RabbitMQConfig.ORDER_CREATED_KEY)
                    .exchange(RabbitMQConfig.ORDER_EXCHANGE)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();

            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }

        log.info("Order created with outbox event: orderId={}", savedOrder.getId());
        return savedOrder;
    }
}
```

#### Saga Pattern (Simplificado) - Flujo de Pedido E-commerce

```
┌──────────────┐     ┌───────────────┐     ┌──────────────┐     ┌──────────────┐
│ Order Service │────▶│ Inventory Svc │────▶│ Payment Svc  │────▶│ Shipping Svc │
│              │     │               │     │              │     │              │
│ order.created│     │ inventory.    │     │ payment.     │     │ shipping.    │
│              │     │ reserved      │     │ processed    │     │ scheduled    │
└──────────────┘     └───────────────┘     └──────────────┘     └──────────────┘
       ▲                     │                     │                     │
       │                     ▼                     ▼                     ▼
       │              (Si falla)            (Si falla)            (Si falla)
       │              inventory.            payment.              shipping.
       └──────────── compensation ◀──────── compensation ◀─────── compensation
```

```java
package com.acme.ecommerce.infrastructure.messaging.consumer;

import com.acme.ecommerce.infrastructure.messaging.model.InventoryReservedEvent;
import com.acme.ecommerce.infrastructure.messaging.model.InventoryReservationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrderConsumer {

    private final OrderSagaService orderSagaService;

    /**
     * Paso exitoso: el inventario fue reservado, continuar con pago.
     */
    @RabbitListener(queues = "inventory.reserved.queue")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Saga step completed: inventory reserved for order {}",
                event.orderId());
        orderSagaService.onInventoryReserved(event);
    }

    /**
     * Compensación: la reserva de inventario falló, cancelar pedido.
     */
    @RabbitListener(queues = "inventory.reservation-failed.queue")
    public void handleInventoryReservationFailed(
            InventoryReservationFailedEvent event) {
        log.warn("Saga compensation: inventory reservation failed for order {}",
                event.orderId());
        orderSagaService.onInventoryReservationFailed(event);
    }
}
```

---

## 10. Testing con Testcontainers

### Referencia Rápida (Seniors)

```java
@Testcontainers
@SpringBootTest
class OrderEventIT {
    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @Test
    void shouldPublishOrderCreatedEvent() { }
}
```

### Guía Detallada (Junior/Mid)

#### Dependencias de Test

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

#### Base Class para Integration Tests con RabbitMQ

```java
package com.acme.ecommerce;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseRabbitMQIntegrationTest {

    @Container
    @ServiceConnection
    protected static RabbitMQContainer rabbit =
            new RabbitMQContainer("rabbitmq:3.13-management");
}
```

#### Test de Publicación y Consumo de Eventos

```java
package com.acme.ecommerce.infrastructure.messaging;

import com.acme.ecommerce.BaseRabbitMQIntegrationTest;
import com.acme.ecommerce.infrastructure.messaging.config.RabbitMQConfig;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import com.acme.ecommerce.infrastructure.messaging.model.OrderItemEvent;
import com.acme.ecommerce.infrastructure.messaging.publisher.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderEventPublisherIT extends BaseRabbitMQIntegrationTest {

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldPublishOrderCreatedEvent() {
        // given
        OrderCreatedEvent event = OrderCreatedEvent.of(
                1L,
                100L,
                "cliente@acme.com",
                List.of(new OrderItemEvent("PROD-001", "Laptop", 1,
                        BigDecimal.valueOf(999.99))),
                BigDecimal.valueOf(999.99)
        );

        // when
        orderEventPublisher.publishOrderCreated(event);

        // then - verificar que el mensaje llegó a la queue
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Object received = rabbitTemplate.receiveAndConvert(
                    RabbitMQConfig.ORDER_CREATED_QUEUE,
                    2000  // timeout en ms
            );
            assertThat(received).isNotNull();
            assertThat(received).isInstanceOf(OrderCreatedEvent.class);

            OrderCreatedEvent receivedEvent = (OrderCreatedEvent) received;
            assertThat(receivedEvent.orderId()).isEqualTo(1L);
            assertThat(receivedEvent.totalAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(999.99));
        });
    }

    @Test
    void shouldRouteToMultipleQueues() {
        // given
        OrderCreatedEvent event = OrderCreatedEvent.of(
                2L, 200L, "otro@acme.com",
                List.of(new OrderItemEvent("PROD-002", "Mouse", 2,
                        BigDecimal.valueOf(25.00))),
                BigDecimal.valueOf(50.00)
        );

        // when
        orderEventPublisher.publishOrderCreated(event);

        // then - el mensaje debe llegar a inventory queue Y notification queue
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Object inventoryMsg = rabbitTemplate.receiveAndConvert(
                    RabbitMQConfig.ORDER_CREATED_INVENTORY_QUEUE, 2000);
            Object notificationMsg = rabbitTemplate.receiveAndConvert(
                    RabbitMQConfig.ORDER_CREATED_NOTIFICATION_QUEUE, 2000);

            assertThat(inventoryMsg).isNotNull();
            assertThat(notificationMsg).isNotNull();
        });
    }
}
```

#### Test de Consumer con @RabbitListener

```java
package com.acme.ecommerce.infrastructure.messaging;

import com.acme.ecommerce.BaseRabbitMQIntegrationTest;
import com.acme.ecommerce.application.service.InventoryService;
import com.acme.ecommerce.infrastructure.messaging.config.RabbitMQConfig;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import com.acme.ecommerce.infrastructure.messaging.model.OrderItemEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class OrderCreatedConsumerIT extends BaseRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void shouldConsumeOrderCreatedEvent() {
        // given
        OrderCreatedEvent event = OrderCreatedEvent.of(
                10L, 100L, "test@acme.com",
                List.of(new OrderItemEvent("PROD-001", "Teclado", 1,
                        BigDecimal.valueOf(79.99))),
                BigDecimal.valueOf(79.99)
        );

        // when - publicar directamente en la queue del consumer
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_KEY,
                event
        );

        // then - verificar que el consumer procesó el mensaje
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(inventoryService).reserveInventory(
                        eq(10L), any()
                )
        );
    }
}
```

#### Test de Dead Letter Queue

```java
package com.acme.ecommerce.infrastructure.messaging;

import com.acme.ecommerce.BaseRabbitMQIntegrationTest;
import com.acme.ecommerce.application.service.InventoryService;
import com.acme.ecommerce.infrastructure.messaging.config.RabbitMQConfig;
import com.acme.ecommerce.infrastructure.messaging.model.OrderCreatedEvent;
import com.acme.ecommerce.infrastructure.messaging.model.OrderItemEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

class DeadLetterQueueIT extends BaseRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void shouldSendToDeadLetterQueueAfterRetries() {
        // given - el servicio de inventario siempre falla
        doThrow(new RuntimeException("Inventory service unavailable"))
                .when(inventoryService).reserveInventory(anyLong(), any());

        OrderCreatedEvent event = OrderCreatedEvent.of(
                99L, 100L, "fail@acme.com",
                List.of(new OrderItemEvent("PROD-999", "Item Fallido", 1,
                        BigDecimal.valueOf(10.00))),
                BigDecimal.valueOf(10.00)
        );

        // when
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_KEY,
                event
        );

        // then - después de reintentos, el mensaje debe estar en la DLQ
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Object dlqMessage = rabbitTemplate.receiveAndConvert(
                    "order.created.dlq", 2000);
            assertThat(dlqMessage).isNotNull();
        });
    }
}
```

---

## ✅ Hacer

1. **Usar Jackson2JsonMessageConverter** - Serialización consistente JSON para todos los mensajes
2. **Configurar Dead Letter Queues** - En toda queue de producción para capturar mensajes fallidos
3. **Implementar consumers idempotentes** - Deduplicación por `eventId` en DB o Redis
4. **Usar Topic Exchange por defecto** - Mayor flexibilidad con wildcards que Direct
5. **Incluir correlation ID** - En cada mensaje para trazabilidad end-to-end
6. **Configurar retry con backoff exponencial** - `multiplier: 2.0` para no saturar el sistema
7. **Separar errores transitorios de permanentes** - Solo reintentar lo que tiene sentido
8. **Usar `@ServiceConnection` en tests** - Auto-configuración de Testcontainers con Spring Boot 3.4
9. **Implementar Transactional Outbox** - Para garantizar consistencia entre DB y mensajería
10. **Externalizar nombres de queues** - Usar constantes o `application.yml`, no strings hardcodeados

## ❌ Evitar

1. **`acknowledge-mode: none`** - Mensajes se pierden si el consumer falla
2. **Queues no durables en producción** - Se pierden al reiniciar RabbitMQ
3. **Default exchange para todo** - Sin Exchange explícito se pierde control del routing
4. **Publicar dentro de `@Transactional` sin outbox** - Dual write causa inconsistencias
5. **Reintentar errores de validación** - Si el mensaje es inválido, reintentarlo no lo arregla
6. **`prefetch: 1` sin justificación** - Reduce throughput significativamente
7. **Consumers sin logging** - Dificulta diagnóstico de problemas en producción
8. **Mensajes enormes (>1MB)** - Usar claim check pattern (referencia a storage externo)
9. **Ignorar DLQ** - Mensajes se acumulan sin monitoreo ni alertas
10. **Tests sin Testcontainers** - Mocks de RabbitMQ no validan bindings ni routing real

---

## Checklist de Implementación

### Configuración Base
- [ ] `spring-boot-starter-amqp` como dependencia
- [ ] `application.yml` con host, port, credentials externalizados
- [ ] `Jackson2JsonMessageConverter` configurado como bean
- [ ] `RabbitTemplate` con message converter inyectado
- [ ] Constantes para nombres de exchanges, queues y routing keys

### Exchanges y Queues
- [ ] Exchanges declarados como `durable(true)`
- [ ] Queues de producción con `x-dead-letter-exchange` configurado
- [ ] Bindings entre exchanges y queues definidos
- [ ] Dead Letter Exchange y DLQs creados
- [ ] Naming convention consistente (`dominio.accion.queue`)

### Producers
- [ ] `RabbitTemplate.convertAndSend()` con exchange y routing key
- [ ] Correlation ID incluido en cada mensaje
- [ ] Headers de metadata (`X-Event-Type`, `X-Source-Service`)
- [ ] Eventos modelados como records inmutables
- [ ] Transactional Outbox para consistencia DB-MQ

### Consumers
- [ ] `@RabbitListener` con queue name externalizado
- [ ] Concurrency configurada (`concurrency = "3-5"`)
- [ ] Error handling con distinción transitorios vs permanentes
- [ ] Idempotencia implementada (DB o Redis)
- [ ] Logging de recepción y procesamiento de mensajes

### Retry y DLQ
- [ ] Spring Retry habilitado con backoff exponencial
- [ ] `MessageRecoverer` configurado (RepublishMessageRecoverer)
- [ ] `default-requeue-rejected: false`
- [ ] DLQ consumer para monitoreo y alertas
- [ ] Parking lot queue para mensajes irrecuperables

### Testing
- [ ] Testcontainers con `@ServiceConnection` para RabbitMQ
- [ ] Tests de publicación de eventos
- [ ] Tests de consumo con `@MockitoBean`
- [ ] Tests de Dead Letter Queue flow
- [ ] Tests de idempotencia (mensaje duplicado)

---

*Documento generado con Context7 - Spring Boot 3.4.x + RabbitMQ 3.13+*
