# Área 17: Arquitectura Hexagonal (Puertos y Adaptadores) + DDD Táctico con Spring Boot 3.x

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 21+, Maven 3.9+, Spring Framework 6.2

---

## 1. Estructura de Módulos Maven para Hexagonal

### Referencia Rápida (Seniors)

```
order-service/                  ← Parent POM
├── order-domain/               ← Puro Java, sin Spring
├── order-application/          ← Puertos, Application Services
├── order-infrastructure/       ← Adaptadores (JPA, REST, Messaging)
└── order-container/            ← Main class, BeanConfiguration
```

**Dependencia entre módulos** (quién depende de quién):
```
container → infrastructure → application → domain
```

El dominio NO depende de nadie. La infraestructura depende de application y domain. El container orquesta todo.

### Guía Detallada (Junior/Mid)

La Arquitectura Hexagonal (también llamada Puertos y Adaptadores) separa tu aplicación en capas con una regla fundamental: **las dependencias siempre apuntan hacia adentro, hacia el dominio**.

```
                    ┌─────────────────────────────────┐
                    │          INFRASTRUCTURE          │
                    │  ┌───────────────────────────┐   │
                    │  │       APPLICATION          │   │
                    │  │  ┌─────────────────────┐   │   │
                    │  │  │       DOMAIN         │   │   │
                    │  │  │                     │   │   │
   HTTP Request ────┼──┼──┼──→ Lógica Negocio   │   │   │
                    │  │  │                     │   │   │
                    │  │  │       ──────┼───┼──┼──→ Database
                    │  │  └─────────────────────┘   │   │
                    │  └───────────────────────────┘   │
                    └─────────────────────────────────┘
```

**¿Por qué módulos Maven separados y no solo paquetes?** Porque Maven **fuerza** la dirección de dependencias en tiempo de compilación. Si pones un `import org.springframework` en el módulo `domain`, Maven rompe el build. Con paquetes simples, nada te impide importar Spring en tu dominio.

---

### 1.1. Parent POM

```xml
<!-- order-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <groupId>com.acme</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>order-domain</module>
        <module>order-application</module>
        <module>order-infrastructure</module>
        <module>order-container</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.6.3</mapstruct.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Módulos internos -->
            <dependency>
                <groupId>com.acme</groupId>
                <artifactId>order-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.acme</groupId>
                <artifactId>order-application</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.acme</groupId>
                <artifactId>order-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 1.2. Módulo Domain

✅ **Hacer**: El módulo domain NO tiene dependencias de Spring. Solo Java puro.

```xml
<!-- order-domain/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.acme</groupId>
        <artifactId>order-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>order-domain</artifactId>

    <dependencies>
        <!-- SIN dependencias de Spring -->
        <!-- Solo lo mínimo necesario -->
    </dependencies>
</project>
```

**Estructura de paquetes**:
```
order-domain/src/main/java/com/acme/order/domain/
├── model/
│   ├── aggregate/
│   │   └── Order.java                  ← Aggregate Root
│   ├── entity/
│   │   └── OrderItem.java              ← Entity dentro del Aggregate
│   └── valueobject/
│       ├── OrderId.java                ← Typed ID (record)
│       ├── CustomerId.java             ← Typed ID (record)
│       ├── Money.java                  ← Value Object (record)
│       ├── OrderStatus.java            ← Enum
│       └── Address.java                ← Value Object (record)
├── event/
│   ├── DomainEvent.java                ← Clase base abstracta
│   ├── OrderCreatedEvent.java          ← Evento de dominio
│   └── OrderCancelledEvent.java        ← Evento de dominio
├── exception/
│   ├── DomainException.java            ← Base de excepciones de dominio
│   ├── OrderNotFoundException.java
│   └── InvalidOrderStateException.java
└── service/
    └── OrderDomainService.java         ← Domain Service (clase, no interfaz)
```

### 1.3. Módulo Application

```xml
<!-- order-application/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.acme</groupId>
        <artifactId>order-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>order-application</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>order-domain</artifactId>
        </dependency>
        <!-- Solo Spring Context para @Transactional (opcional, ver nota) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
    </dependencies>
</project>
```

> **Nota sobre `@Transactional` en Application**: Existe debate sobre si `@Transactional` acopla esta capa a Spring. Es una dependencia mínima y pragmática. Si prefieres pureza total, puedes usar un `TransactionPort` como interfaz y delegar la gestión transaccional al adaptador.

**Estructura de paquetes**:
```
order-application/src/main/java/com/acme/order/application/
├── port/
│   ├── input/
│   │   ├── CreateOrderUseCase.java         ← Input Port (interfaz)
│   │   ├── CancelOrderUseCase.java         ← Input Port (interfaz)
│   │   └── GetOrderUseCase.java            ← Input Port (interfaz)
│   └── output/
│       ├── OrderRepository.java            ← Output Port (interfaz)
│       ├── OrderEventPublisher.java        ← Output Port (interfaz)
│       └── PaymentGateway.java             ← Output Port (interfaz)
├── service/
│   └── OrderApplicationService.java        ← Implementa Input Ports
├── dto/
│   ├── command/
│   │   ├── CreateOrderCommand.java         ← Command (record)
│   │   └── CancelOrderCommand.java         ← Command (record)
│   └── response/
│       ├── OrderResponse.java              ← Response (record)
│       └── OrderItemResponse.java          ← Response (record)
└── mapper/
    └── OrderApplicationMapper.java         ← Domain ↔ DTO
```

### 1.4. Módulo Infrastructure

```xml
<!-- order-infrastructure/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.acme</groupId>
        <artifactId>order-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>order-infrastructure</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>order-application</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

**Estructura de paquetes**:
```
order-infrastructure/src/main/java/com/acme/order/infrastructure/
├── adapter/
│   ├── input/
│   │   └── rest/
│   │       ├── OrderController.java            ← Adaptador primario
│   │       └── dto/
│   │           ├── CreateOrderRequest.java      ← DTO de entrada REST
│   │           └── OrderRestResponse.java       ← DTO de salida REST
│   └── output/
│       ├── persistence/
│       │   ├── OrderJpaRepository.java          ← Spring Data JPA
│       │   ├── OrderRepositoryAdapter.java      ← Implementa Output Port
│       │   ├── entity/
│       │   │   ├── OrderJpaEntity.java          ← JPA Entity (≠ Domain Entity)
│       │   │   └── OrderItemJpaEntity.java      ← JPA Entity
│       │   └── mapper/
│       │       └── OrderPersistenceMapper.java  ← Domain ↔ JPA Entity
│       └── event/
│           └── OrderEventPublisherAdapter.java  ← Implementa Output Port
```

### 1.5. Módulo Container

```xml
<!-- order-container/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.acme</groupId>
        <artifactId>order-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>order-container</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>order-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>order-application</artifactId>
        </dependency>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>order-infrastructure</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Estructura de paquetes**:
```
order-container/src/main/java/com/acme/order/
├── OrderServiceApplication.java        ← @SpringBootApplication
└── config/
    └── BeanConfiguration.java          ← Registro manual de beans de dominio
```

---

## 2. Capa Domain — DDD Táctico

### Referencia Rápida (Seniors)

| Concepto | Implementación Java 21 | Característica clave |
|----------|------------------------|---------------------|
| Base Entity | Clase abstracta `BaseEntity<ID>` | equals/hashCode por ID |
| Aggregate Root | `AggregateRoot<ID>` extends `BaseEntity<ID>` | Acumula domain events |
| Value Object | Java `record` | Validación en constructor compacto |
| Domain Event | Clase abstracta `DomainEvent` | ID, timestamp, aggregateId |
| Domain Service | Clase sin anotaciones Spring | Coordina entre aggregates |
| Domain Exception | Jerarquía desde `DomainException` | Sin dependencias de framework |

### Guía Detallada (Junior/Mid)

### 2.1. BaseEntity — Clase Base para Entities

✅ **Hacer**: Crear una clase base abstracta con identity equality

**Por qué**: En DDD, dos entities son iguales si tienen el mismo ID, independientemente de sus atributos. Esto es diferente de un Value Object, donde la igualdad se basa en todos los atributos.

```java
package com.acme.order.domain.model;

import java.util.Objects;

/**
 * Clase base para todas las entities del dominio.
 * Implementa identity equality: dos entities son iguales si tienen el mismo ID.
 *
 * @param <ID> Tipo del identificador (preferiblemente un Value Object tipado)
 */
public abstract class BaseEntity<ID> {

    private ID id;

    protected BaseEntity() {
        // Constructor vacío necesario para frameworks de persistencia vía adaptador
    }

    protected BaseEntity(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    protected void setId(ID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Usar un hash constante cuando el ID es null (entity nueva)
        // para evitar problemas con colecciones hash cuando el ID se asigna después
        return id != null ? Objects.hashCode(id) : 31;
    }
}
```

❌ **Evitar**: Generar equals/hashCode con todos los campos (estilo Lombok `@Data` o `@EqualsAndHashCode`)

```java
// MAL — igualdad por atributos, no por identidad
@Data  // Lombok genera equals con TODOS los campos
public class Order {
    private Long id;
    private String description;
    private BigDecimal total;
}

// BIEN — igualdad por identidad
public class Order extends BaseEntity<OrderId> {
    // equals/hashCode heredados, basados solo en OrderId
}
```

### 2.2. AggregateRoot — Raíz del Aggregate

✅ **Hacer**: Extender de `BaseEntity` y agregar gestión de domain events

```java
package com.acme.order.domain.model;

import com.acme.order.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase base para Aggregate Roots.
 * Un Aggregate Root es el punto de entrada a un cluster de entities y value objects.
 * Solo el Aggregate Root puede ser referenciado desde fuera del aggregate.
 * Gestiona domain events que se despachan tras la persistencia.
 *
 * @param <ID> Tipo del identificador
 */
public abstract class AggregateRoot<ID> extends BaseEntity<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot() {
        super();
    }

    protected AggregateRoot(ID id) {
        super(id);
    }

    /**
     * Registra un domain event. Se acumulan internamente y se despachan
     * DESPUÉS de que el Application Service persista el aggregate.
     */
    protected void registerDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * Retorna los domain events acumulados (copia inmutable).
     * Usado por el Application Service para despacharlos.
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Limpia los domain events. Se llama DESPUÉS de despacharlos.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

**¿Cuándo una Entity debe ser Aggregate Root?**

| Criterio | Entity normal | Aggregate Root |
|----------|--------------|----------------|
| ¿Se accede directamente desde fuera? | No, solo a través de su AR | Sí |
| ¿Tiene su propio repositorio? | No | Sí |
| ¿Mantiene invariantes de negocio? | Las suyas propias | Las del aggregate completo |
| ¿Puede existir sin su parent? | No, pertenece a un aggregate | Sí, es independiente |

**Ejemplo**: En un sistema de pedidos, `Order` es Aggregate Root y `OrderItem` es una Entity dentro del aggregate. No puedes acceder a un `OrderItem` sin pasar por `Order`, y no existe un `OrderItemRepository`.

### 2.3. Aggregate Root — Implementación Completa (Order)

✅ **Hacer**: El Aggregate Root encapsula toda la lógica de cambio de estado con validaciones

```java
package com.acme.order.domain.model.aggregate;

import com.acme.order.domain.event.OrderCancelledEvent;
import com.acme.order.domain.event.OrderCreatedEvent;
import com.acme.order.domain.exception.InvalidOrderStateException;
import com.acme.order.domain.model.AggregateRoot;
import com.acme.order.domain.model.entity.OrderItem;
import com.acme.order.domain.model.valueobject.Address;
import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.Money;
import com.acme.order.domain.model.valueobject.OrderId;
import com.acme.order.domain.model.valueobject.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot<OrderId> {

    private CustomerId customerId;
    private Address shippingAddress;
    private OrderStatus status;
    private final List<OrderItem> items = new ArrayList<>();
    private Money totalAmount;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructor vacío para frameworks de persistencia (vía adaptador)
    private Order() {
        super();
    }

    /**
     * Factory method — único punto de creación.
     * Las validaciones de negocio se ejecutan aquí.
     */
    public static Order create(CustomerId customerId, Address shippingAddress,
                               List<OrderItem> items) {
        if (customerId == null) {
            throw new IllegalArgumentException("El customerId es obligatorio");
        }
        if (shippingAddress == null) {
            throw new IllegalArgumentException("La dirección de envío es obligatoria");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un item");
        }

        var order = new Order();
        order.setId(new OrderId(UUID.randomUUID()));
        order.customerId = customerId;
        order.shippingAddress = shippingAddress;
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        order.updatedAt = Instant.now();

        items.forEach(order::addItem);
        order.recalculateTotal();

        // Registrar domain event
        order.registerDomainEvent(new OrderCreatedEvent(
                order.getId(),
                order.customerId,
                order.totalAmount,
                order.createdAt
        ));

        return order;
    }

    /**
     * Agrega un item al pedido. Solo permitido en estado PENDING.
     */
    public void addItem(OrderItem item) {
        assertOrderIsPending();
        items.add(item);
        recalculateTotal();
        this.updatedAt = Instant.now();
    }

    /**
     * Confirma el pedido — transición de estado con validación.
     */
    public void confirm() {
        assertOrderIsPending();
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    /**
     * Cancela el pedido — solo si no fue enviado.
     */
    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(
                    "No se puede cancelar un pedido en estado " + status
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();

        registerDomainEvent(new OrderCancelledEvent(
                getId(), customerId, reason, Instant.now()
        ));
    }

    // --- Métodos privados de validación ---

    private void assertOrderIsPending() {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Operación solo permitida en estado PENDING, estado actual: " + status
            );
        }
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    // --- Getters (sin setters públicos) ---

    public CustomerId getCustomerId() { return customerId; }
    public Address getShippingAddress() { return shippingAddress; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Money getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

❌ **Evitar**: Setters públicos que permiten modificar el estado sin validación

```java
// MAL — Anemic Domain Model
public class Order {
    private OrderStatus status;

    public void setStatus(OrderStatus status) {  // Cualquiera puede cambiar el estado
        this.status = status;
    }
}

// BIEN — Rich Domain Model
public class Order extends AggregateRoot<OrderId> {

    public void cancel(String reason) {           // Transición de estado controlada
        if (status == OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException("...");
        }
        this.status = OrderStatus.CANCELLED;
        registerDomainEvent(new OrderCancelledEvent(...));
    }
}
```

### 2.4. Entity dentro del Aggregate

```java
package com.acme.order.domain.model.entity;

import com.acme.order.domain.model.BaseEntity;
import com.acme.order.domain.model.valueobject.Money;

import java.util.UUID;

/**
 * OrderItem es una Entity dentro del Aggregate Order.
 * No tiene su propio repositorio — se persiste junto con Order.
 */
public class OrderItem extends BaseEntity<UUID> {

    private String productId;
    private String productName;
    private int quantity;
    private Money unitPrice;

    private OrderItem() {
        super();
    }

    public OrderItem(String productId, String productName, int quantity, Money unitPrice) {
        super(UUID.randomUUID());
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money getSubtotal() {
        return unitPrice.multiply(quantity);
    }

    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.quantity = newQuantity;
    }

    // Getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
}
```

### 2.5. Value Objects con Java Records

✅ **Hacer**: Usar records de Java para Value Objects. Validar en el constructor compacto.

**Por qué**: Los records son inmutables por diseño, tienen equals/hashCode basado en todos los campos (exactamente lo que necesita un Value Object), y la validación en el constructor compacto garantiza que nunca exista una instancia inválida.

#### Typed IDs

```java
package com.acme.order.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed ID para Order. Evita confundir un OrderId con un CustomerId
 * a nivel de compilación (type safety).
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "El OrderId no puede ser null");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

```java
package com.acme.order.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "El CustomerId no puede ser null");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

#### Money

```java
package com.acme.order.domain.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object que representa dinero con moneda.
 * Inmutable y con operaciones aritméticas seguras.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("USD"));

    public Money {
        Objects.requireNonNull(amount, "El monto no puede ser null");
        Objects.requireNonNull(currency, "La moneda no puede ser null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("USD"));
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "No se pueden operar montos con monedas diferentes: "
                    + this.currency + " vs " + other.currency
            );
        }
    }
}
```

#### Address

```java
package com.acme.order.domain.model.valueobject;

public record Address(
        String street,
        String city,
        String state,
        String zipCode,
        String country
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("La calle es obligatoria");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("La ciudad es obligatoria");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("El país es obligatorio");
        }
    }
}
```

#### OrderStatus (Enum como Value Object)

```java
package com.acme.order.domain.model.valueobject;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
```

### 2.6. Domain Events

✅ **Hacer**: Crear una clase base abstracta para domain events. El aggregate root los acumula y el application service los despacha DESPUÉS de persistir.

```java
package com.acme.order.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Clase base para todos los Domain Events.
 * Un Domain Event representa algo que OCURRIÓ en el dominio.
 * Se nombran en pasado: OrderCreated, OrderCancelled.
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredOn() { return occurredOn; }
}
```

```java
package com.acme.order.domain.event;

import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.Money;
import com.acme.order.domain.model.valueobject.OrderId;

import java.time.Instant;

public class OrderCreatedEvent extends DomainEvent {

    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money totalAmount;
    private final Instant createdAt;

    public OrderCreatedEvent(OrderId orderId, CustomerId customerId,
                             Money totalAmount, Instant createdAt) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
}
```

```java
package com.acme.order.domain.event;

import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.OrderId;

import java.time.Instant;

public class OrderCancelledEvent extends DomainEvent {

    private final OrderId orderId;
    private final CustomerId customerId;
    private final String reason;
    private final Instant cancelledAt;

    public OrderCancelledEvent(OrderId orderId, CustomerId customerId,
                               String reason, Instant cancelledAt) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.reason = reason;
        this.cancelledAt = cancelledAt;
    }

    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public String getReason() { return reason; }
    public Instant getCancelledAt() { return cancelledAt; }
}
```

### 2.7. Domain Service

✅ **Hacer**: Usar Domain Service cuando la lógica no pertenece naturalmente a un solo Aggregate Root. Sin anotaciones Spring.

**Cuándo usar Domain Service vs Aggregate Root**:

| Lógica en... | Cuándo |
|-------------|--------|
| Aggregate Root | La lógica opera sobre un solo aggregate y sus datos internos |
| Domain Service | La lógica necesita coordinar entre múltiples aggregates o necesita información externa al aggregate |

```java
package com.acme.order.domain.service;

import com.acme.order.domain.model.aggregate.Order;
import com.acme.order.domain.model.valueobject.Money;

import java.util.List;

/**
 * Domain Service para lógica que involucra múltiples aggregates
 * o que no pertenece naturalmente a un solo aggregate.
 *
 * SIN anotaciones de Spring (@Service, @Component).
 * Se registra como bean en BeanConfiguration.
 */
public class OrderDomainService {

    private static final Money MINIMUM_ORDER_AMOUNT = Money.of(10.00);
    private static final int MAX_ACTIVE_ORDERS_PER_CUSTOMER = 5;

    /**
     * Valida que un nuevo pedido cumple las reglas de negocio que
     * requieren conocer el contexto global (otros pedidos del cliente).
     */
    public void validateNewOrder(Order newOrder, List<Order> activeCustomerOrders) {
        // Regla: monto mínimo de pedido
        if (!newOrder.getTotalAmount().isGreaterThan(MINIMUM_ORDER_AMOUNT)) {
            throw new IllegalArgumentException(
                    "El monto mínimo de pedido es " + MINIMUM_ORDER_AMOUNT
            );
        }

        // Regla: máximo de pedidos activos por cliente
        if (activeCustomerOrders.size() >= MAX_ACTIVE_ORDERS_PER_CUSTOMER) {
            throw new IllegalArgumentException(
                    "El cliente ya tiene el máximo de " + MAX_ACTIVE_ORDERS_PER_CUSTOMER
                    + " pedidos activos"
            );
        }
    }
}
```

### 2.8. Domain Exceptions

✅ **Hacer**: Crear una jerarquía de excepciones de dominio SIN dependencias de framework

```java
package com.acme.order.domain.exception;

/**
 * Base de todas las excepciones del dominio.
 * No extiende de excepciones de Spring ni usa HttpStatus.
 * El mapeo a HTTP status se hace en la capa de infrastructure.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String message, String code) {
        super(message);
        this.code = code;
    }

    protected DomainException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

```java
package com.acme.order.domain.exception;

public class OrderNotFoundException extends DomainException {

    public OrderNotFoundException(String orderId) {
        super("Pedido con id '" + orderId + "' no encontrado", "ORDER_NOT_FOUND");
    }
}
```

```java
package com.acme.order.domain.exception;

public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(String message) {
        super(message, "INVALID_ORDER_STATE");
    }
}
```

---

## 3. Capa Application — Orquestación

### Referencia Rápida (Seniors)

| Componente | Responsabilidad |
|-----------|----------------|
| Input Port | Interfaz que define un caso de uso |
| Output Port | Interfaz que el dominio necesita del exterior |
| Application Service | Implementa Input Ports. Orquesta: transacción → repository → domain → persist → events |
| Command | Record inmutable con datos de entrada |
| Response | Record inmutable con datos de salida |

**El Application Service NO contiene lógica de negocio**. Solo orquesta.

### Guía Detallada (Junior/Mid)

### 3.1. Input Ports (Puertos de Entrada)

✅ **Hacer**: Definir una interfaz por caso de uso

**Por qué**: Cada puerto representa un caso de uso específico. Esto cumple con el Interface Segregation Principle (ISP) y hace explícitos los puntos de entrada de tu aplicación.

```java
package com.acme.order.application.port.input;

import com.acme.order.application.dto.command.CreateOrderCommand;
import com.acme.order.application.dto.response.OrderResponse;

/**
 * Puerto de entrada — Caso de uso: Crear Pedido.
 * Implementado por OrderApplicationService.
 * Usado por adaptadores primarios (REST Controller, CLI, etc.)
 */
public interface CreateOrderUseCase {

    OrderResponse execute(CreateOrderCommand command);
}
```

```java
package com.acme.order.application.port.input;

import com.acme.order.application.dto.command.CancelOrderCommand;

public interface CancelOrderUseCase {

    void execute(CancelOrderCommand command);
}
```

```java
package com.acme.order.application.port.input;

import com.acme.order.application.dto.response.OrderResponse;

public interface GetOrderUseCase {

    OrderResponse execute(String orderId);
}
```

### 3.2. Output Ports (Puertos de Salida)

✅ **Hacer**: Definir interfaces para todo lo que el dominio necesita del exterior

```java
package com.acme.order.application.port.output;

import com.acme.order.domain.model.aggregate.Order;
import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida — Repositorio de pedidos.
 * Definido en application, implementado en infrastructure.
 * El dominio habla con esta interfaz, nunca con JPA directamente.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    List<Order> findActiveByCustomerId(CustomerId customerId);
}
```

```java
package com.acme.order.application.port.output;

import com.acme.order.domain.event.DomainEvent;

import java.util.List;

/**
 * Puerto de salida — Publicación de domain events.
 * El application service despacha los events del aggregate a través de este puerto.
 */
public interface OrderEventPublisher {

    void publish(List<DomainEvent> events);
}
```

### 3.3. Commands y Responses (DTOs)

✅ **Hacer**: Usar records inmutables. Los Commands llevan la data de entrada, los Responses la de salida.

```java
package com.acme.order.application.dto.command;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderCommand(
        String customerId,
        String street,
        String city,
        String state,
        String zipCode,
        String country,
        List<OrderItemCommand> items
) {
    public record OrderItemCommand(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
```

```java
package com.acme.order.application.dto.command;

public record CancelOrderCommand(String orderId, String reason) {}
```

```java
package com.acme.order.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemResponse> items,
        Instant createdAt
) {}
```

```java
package com.acme.order.application.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
```

### 3.4. Application Service — Implementación

✅ **Hacer**: El Application Service implementa los Input Ports y orquesta el flujo. NO contiene lógica de negocio.

```java
package com.acme.order.application.service;

import com.acme.order.application.dto.command.CancelOrderCommand;
import com.acme.order.application.dto.command.CreateOrderCommand;
import com.acme.order.application.dto.response.OrderResponse;
import com.acme.order.application.mapper.OrderApplicationMapper;
import com.acme.order.application.port.input.CancelOrderUseCase;
import com.acme.order.application.port.input.CreateOrderUseCase;
import com.acme.order.application.port.input.GetOrderUseCase;
import com.acme.order.application.port.output.OrderEventPublisher;
import com.acme.order.application.port.output.OrderRepository;
import com.acme.order.domain.exception.OrderNotFoundException;
import com.acme.order.domain.model.aggregate.Order;
import com.acme.order.domain.model.entity.OrderItem;
import com.acme.order.domain.model.valueobject.Address;
import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.Money;
import com.acme.order.domain.model.valueobject.OrderId;
import com.acme.order.domain.service.OrderDomainService;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application Service — Orquesta los casos de uso.
 *
 * Responsabilidades:
 * 1. Recibir Commands desde los Input Ports
 * 2. Convertir DTOs a objetos de dominio
 * 3. Abrir transacción
 * 4. Delegar lógica de negocio al Domain Service / Aggregate Root
 * 5. Persistir a través del Output Port (Repository)
 * 6. Despachar Domain Events DESPUÉS de persistir
 * 7. Convertir resultado a Response DTO
 *
 * NO contiene: validaciones de negocio, cálculos, reglas.
 * NO tiene: @Service (se registra manualmente en BeanConfiguration)
 */
public class OrderApplicationService implements CreateOrderUseCase,
                                                CancelOrderUseCase,
                                                GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final OrderDomainService orderDomainService;
    private final OrderApplicationMapper mapper;

    public OrderApplicationService(OrderRepository orderRepository,
                                   OrderEventPublisher eventPublisher,
                                   OrderDomainService orderDomainService,
                                   OrderApplicationMapper mapper) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.orderDomainService = orderDomainService;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OrderResponse execute(CreateOrderCommand command) {
        // 1. Convertir Command a objetos de dominio
        var customerId = new CustomerId(UUID.fromString(command.customerId()));
        var address = new Address(
                command.street(), command.city(), command.state(),
                command.zipCode(), command.country()
        );
        var items = command.items().stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        Money.of(item.unitPrice())
                ))
                .toList();

        // 2. Crear aggregate (la lógica de negocio está en Order.create)
        var order = Order.create(customerId, address, items);

        // 3. Validaciones que requieren contexto global (Domain Service)
        var activeOrders = orderRepository.findActiveByCustomerId(customerId);
        orderDomainService.validateNewOrder(order, activeOrders);

        // 4. Persistir
        var savedOrder = orderRepository.save(order);

        // 5. Despachar domain events DESPUÉS de persistir
        eventPublisher.publish(savedOrder.getDomainEvents());
        savedOrder.clearDomainEvents();

        // 6. Retornar Response
        return mapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public void execute(CancelOrderCommand command) {
        var orderId = new OrderId(UUID.fromString(command.orderId()));

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // La lógica de cancelación está en el Aggregate Root
        order.cancel(command.reason());

        orderRepository.save(order);

        eventPublisher.publish(order.getDomainEvents());
        order.clearDomainEvents();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse execute(String orderId) {
        var id = new OrderId(UUID.fromString(orderId));

        var order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return mapper.toResponse(order);
    }
}
```

### 3.5. Mapper en la Capa Application

✅ **Hacer**: Crear un mapper que convierta entre objetos de dominio y DTOs de application

```java
package com.acme.order.application.mapper;

import com.acme.order.application.dto.response.OrderItemResponse;
import com.acme.order.application.dto.response.OrderResponse;
import com.acme.order.domain.model.aggregate.Order;
import com.acme.order.domain.model.entity.OrderItem;

/**
 * Mapper manual Domain → Application DTOs.
 * Alternativa: MapStruct con @Mapper(componentModel = "default")
 * para no acoplar a Spring.
 */
public class OrderApplicationMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId().value().toString(),
                order.getCustomerId().value().toString(),
                order.getStatus().name(),
                order.getTotalAmount().amount(),
                order.getTotalAmount().currency().getCurrencyCode(),
                order.getItems().stream()
                        .map(this::toItemResponse)
                        .toList(),
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getSubtotal().amount()
        );
    }
}
```

---

## 4. Capa Infrastructure — Adaptadores

### Referencia Rápida (Seniors)

| Adaptador | Tipo | Implementa | Usa |
|-----------|------|-----------|-----|
| REST Controller | Primario (in) | — | Input Port |
| JPA Repository Adapter | Secundario (out) | Output Port (OrderRepository) | Spring Data JPA |
| Event Publisher Adapter | Secundario (out) | Output Port (OrderEventPublisher) | ApplicationEventPublisher |

**Clave**: Las JPA Entities son clases DIFERENTES de las Domain Entities. Necesitas un mapper entre ambas.

### Guía Detallada (Junior/Mid)

### 4.1. Adaptador Primario — REST Controller

✅ **Hacer**: El controller solo delega al Input Port. No tiene lógica.

```java
package com.acme.order.infrastructure.adapter.input.rest;

import com.acme.order.application.dto.command.CreateOrderCommand;
import com.acme.order.application.dto.response.OrderResponse;
import com.acme.order.application.port.input.CancelOrderUseCase;
import com.acme.order.application.port.input.CreateOrderUseCase;
import com.acme.order.application.port.input.GetOrderUseCase;
import com.acme.order.infrastructure.adapter.input.rest.dto.CancelOrderRequest;
import com.acme.order.infrastructure.adapter.input.rest.dto.CreateOrderRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador primario (de entrada) — REST API.
 * SOLO delega al Input Port. NO tiene lógica de negocio.
 * NO accede directamente al repository.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        var command = request.toCommand();
        var response = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        var response = getOrderUseCase.execute(orderId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId,
                                            @RequestBody CancelOrderRequest request) {
        cancelOrderUseCase.execute(request.toCommand(orderId));
        return ResponseEntity.noContent().build();
    }
}
```

**DTOs de REST** (viven en infrastructure, no en application):

```java
package com.acme.order.infrastructure.adapter.input.rest.dto;

import com.acme.order.application.dto.command.CreateOrderCommand;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        String customerId,
        String street,
        String city,
        String state,
        String zipCode,
        String country,
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(
                customerId, street, city, state, zipCode, country,
                items.stream()
                        .map(item -> new CreateOrderCommand.OrderItemCommand(
                                item.productId(), item.productName(),
                                item.quantity(), item.unitPrice()
                        ))
                        .toList()
        );
    }
}
```

```java
package com.acme.order.infrastructure.adapter.input.rest.dto;

import com.acme.order.application.dto.command.CancelOrderCommand;

public record CancelOrderRequest(String reason) {

    public CancelOrderCommand toCommand(String orderId) {
        return new CancelOrderCommand(orderId, reason);
    }
}
```

### 4.2. Adaptador Secundario — JPA Repository

✅ **Hacer**: Separar JPA Entity de Domain Entity. Son clases DIFERENTES.

**Por qué**: Las JPA Entities necesitan anotaciones `@Entity`, `@Id`, `@Column`, constructores vacíos, setters, etc. Si usas la Domain Entity directamente con JPA, contaminas tu dominio con preocupaciones de persistencia. Tu dominio terminaría dictado por las reglas de JPA, no por las reglas de negocio.

#### JPA Entities (clases de persistencia)

```java
package com.acme.order.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity — representa la tabla 'orders' en la base de datos.
 * Esta clase NO es la Domain Entity Order. Es un modelo de persistencia.
 * El mapper convierte entre ambas.
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country")
    private String country;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected OrderJpaEntity() {}

    // Getters y setters (necesarios para JPA)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public List<OrderItemJpaEntity> getItems() { return items; }
    public void setItems(List<OrderItemJpaEntity> items) { this.items = items; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void addItem(OrderItemJpaEntity item) {
        items.add(item);
        item.setOrder(this);
    }
}
```

```java
package com.acme.order.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItemJpaEntity() {}

    // Getters y setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public OrderJpaEntity getOrder() { return order; }
    public void setOrder(OrderJpaEntity order) { this.order = order; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
```

#### Spring Data JPA Repository

```java
package com.acme.order.infrastructure.adapter.output.persistence;

import com.acme.order.infrastructure.adapter.output.persistence.entity.OrderJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository — trabaja con JPA Entities, no con Domain Entities.
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.customerId = :customerId " +
           "AND o.status NOT IN ('CANCELLED', 'DELIVERED')")
    List<OrderJpaEntity> findActiveByCustomerId(UUID customerId);
}
```

#### Repository Adapter (implementa el Output Port)

```java
package com.acme.order.infrastructure.adapter.output.persistence;

import com.acme.order.application.port.output.OrderRepository;
import com.acme.order.domain.model.aggregate.Order;
import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.OrderId;
import com.acme.order.infrastructure.adapter.output.persistence.mapper.OrderPersistenceMapper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador secundario — implementa el Output Port OrderRepository.
 * Convierte entre Domain Entity y JPA Entity usando el mapper.
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository,
                                  OrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        var jpaEntity = mapper.toJpaEntity(order);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value())
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<Order> findActiveByCustomerId(CustomerId customerId) {
        return jpaRepository.findActiveByCustomerId(customerId.value())
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }
}
```

#### Mapper de Persistencia (Domain Entity ↔ JPA Entity)

```java
package com.acme.order.infrastructure.adapter.output.persistence.mapper;

import com.acme.order.domain.model.aggregate.Order;
import com.acme.order.domain.model.entity.OrderItem;
import com.acme.order.domain.model.valueobject.Address;
import com.acme.order.domain.model.valueobject.CustomerId;
import com.acme.order.domain.model.valueobject.Money;
import com.acme.order.domain.model.valueobject.OrderId;
import com.acme.order.domain.model.valueobject.OrderStatus;
import com.acme.order.infrastructure.adapter.output.persistence.entity.OrderItemJpaEntity;
import com.acme.order.infrastructure.adapter.output.persistence.entity.OrderJpaEntity;

import org.springframework.stereotype.Component;

import java.util.Currency;

/**
 * Mapper entre Domain Entity (Order) y JPA Entity (OrderJpaEntity).
 * Necesario porque son clases completamente diferentes.
 */
@Component
public class OrderPersistenceMapper {

    public OrderJpaEntity toJpaEntity(Order order) {
        var entity = new OrderJpaEntity();
        entity.setId(order.getId().value());
        entity.setCustomerId(order.getCustomerId().value());
        entity.setStatus(order.getStatus().name());
        entity.setTotalAmount(order.getTotalAmount().amount());
        entity.setCurrency(order.getTotalAmount().currency().getCurrencyCode());
        entity.setStreet(order.getShippingAddress().street());
        entity.setCity(order.getShippingAddress().city());
        entity.setState(order.getShippingAddress().state());
        entity.setZipCode(order.getShippingAddress().zipCode());
        entity.setCountry(order.getShippingAddress().country());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        order.getItems().forEach(item -> {
            var itemEntity = toJpaEntity(item);
            entity.addItem(itemEntity);
        });

        return entity;
    }

    private OrderItemJpaEntity toJpaEntity(OrderItem item) {
        var entity = new OrderItemJpaEntity();
        entity.setId(item.getId());
        entity.setProductId(item.getProductId());
        entity.setProductName(item.getProductName());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice().amount());
        return entity;
    }

    public Order toDomainEntity(OrderJpaEntity entity) {
        // Reconstruir domain entity desde la persistencia
        // Usamos reflection o un constructor package-private dedicado
        // para reconstruir el aggregate sin disparar eventos ni validaciones
        return Order.reconstruct(
                new OrderId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                new Address(
                        entity.getStreet(), entity.getCity(),
                        entity.getState(), entity.getZipCode(), entity.getCountry()
                ),
                OrderStatus.valueOf(entity.getStatus()),
                new Money(entity.getTotalAmount(), Currency.getInstance(entity.getCurrency())),
                entity.getItems().stream()
                        .map(this::toDomainEntity)
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OrderItem toDomainEntity(OrderItemJpaEntity entity) {
        return new OrderItem(
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                new Money(entity.getUnitPrice(), Currency.getInstance("USD"))
        );
    }
}
```

> **Nota**: El método `Order.reconstruct()` es un factory method estático que reconstruye un aggregate desde la persistencia sin disparar eventos ni ejecutar validaciones de creación. Es diferente de `Order.create()`.

**Agregar a la clase `Order`**:
```java
// En Order.java — método de reconstrucción desde persistencia
public static Order reconstruct(OrderId id, CustomerId customerId,
                                 Address shippingAddress, OrderStatus status,
                                 Money totalAmount, List<OrderItem> items,
                                 Instant createdAt, Instant updatedAt) {
    var order = new Order();
    order.setId(id);
    order.customerId = customerId;
    order.shippingAddress = shippingAddress;
    order.status = status;
    order.totalAmount = totalAmount;
    order.items.addAll(items);
    order.createdAt = createdAt;
    order.updatedAt = updatedAt;
    // NO registrar domain events — es una reconstrucción, no una creación
    return order;
}
```

### 4.3. Adaptador Secundario — Event Publisher

```java
package com.acme.order.infrastructure.adapter.output.event;

import com.acme.order.application.port.output.OrderEventPublisher;
import com.acme.order.domain.event.DomainEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptador que implementa el Output Port de publicación de eventos.
 * Usa ApplicationEventPublisher de Spring Framework 6.2.
 *
 * Los handlers pueden usar @TransactionalEventListener para ejecutarse
 * DESPUÉS del commit de la transacción.
 */
@Component
public class OrderEventPublisherAdapter implements OrderEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(applicationEventPublisher::publishEvent);
    }
}
```

**Handler de eventos con `@TransactionalEventListener`** (Spring Framework 6.2):

```java
package com.acme.order.infrastructure.adapter.output.event;

import com.acme.order.domain.event.OrderCreatedEvent;
import com.acme.order.domain.event.OrderCancelledEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handler de domain events.
 * @TransactionalEventListener se ejecuta DESPUÉS del commit (por defecto AFTER_COMMIT).
 * Ideal para side effects como notificaciones, auditoría, etc.
 */
@Component
public class OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    @TransactionalEventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Pedido creado: {} para cliente {} por {}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount());
        // Aquí: enviar email, notificar a sistemas externos, etc.
    }

    @TransactionalEventListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Pedido cancelado: {} razón: {}",
                event.getOrderId(), event.getReason());
        // Aquí: procesar reembolso, notificar al cliente, etc.
    }
}
```

### 4.4. BeanConfiguration — Registro Manual de Beans

✅ **Hacer**: Usar una clase `@Configuration` en el módulo container para registrar manualmente los beans de dominio y application. De esta forma, las clases de dominio NO necesitan `@Service` ni `@Component`.

**Por qué**: Si usas `@Service` en tu `OrderDomainService`, estás importando Spring en el módulo domain. Esto rompe la independencia del dominio. En su lugar, registras los beans explícitamente en una clase de configuración.

```java
package com.acme.order.config;

import com.acme.order.application.mapper.OrderApplicationMapper;
import com.acme.order.application.port.input.CancelOrderUseCase;
import com.acme.order.application.port.input.CreateOrderUseCase;
import com.acme.order.application.port.input.GetOrderUseCase;
import com.acme.order.application.port.output.OrderEventPublisher;
import com.acme.order.application.port.output.OrderRepository;
import com.acme.order.application.service.OrderApplicationService;
import com.acme.order.domain.service.OrderDomainService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración manual de beans de dominio y application.
 * Registra beans SIN necesidad de @Service/@Component en las capas internas.
 *
 * Ubicación: módulo container (order-container).
 * Así, Spring solo vive en infrastructure y container.
 */
@Configuration
public class BeanConfiguration {

    // --- Domain Layer Beans ---

    @Bean
    public OrderDomainService orderDomainService() {
        return new OrderDomainService();
    }

    // --- Application Layer Beans ---

    @Bean
    public OrderApplicationMapper orderApplicationMapper() {
        return new OrderApplicationMapper();
    }

    @Bean
    public OrderApplicationService orderApplicationService(
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher,
            OrderDomainService orderDomainService,
            OrderApplicationMapper mapper) {
        return new OrderApplicationService(
                orderRepository, eventPublisher, orderDomainService, mapper
        );
    }

    // --- Exponer como Input Ports (opcional, para tipado explícito) ---

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderApplicationService service) {
        return service;
    }

    @Bean
    public CancelOrderUseCase cancelOrderUseCase(OrderApplicationService service) {
        return service;
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderApplicationService service) {
        return service;
    }
}
```

### 4.5. Clase Main de Spring Boot

```java
package com.acme.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

## 5. Flujo de Datos Completo

### Referencia Rápida (Seniors)

```
HTTP POST /api/v1/orders
        │
        ▼
┌─────────────────┐     ┌───────────────────┐     ┌───────────────────┐
│  OrderController │────▶│ CreateOrderUseCase │────▶│ OrderApplication  │
│  (Adaptador IN)  │     │   (Input Port)     │     │    Service        │
└─────────────────┘     └───────────────────┘     └────────┬──────────┘
                                                           │
                        ┌──────────────────────────────────┘
                        │
                        ▼
              ┌──────────────────┐     ┌────────────────────┐
              │ Order.create()   │     │ OrderDomainService  │
              │ (Aggregate Root) │     │  .validateNewOrder()│
              └──────────────────┘     └────────────────────┘
                        │
                        ▼
              ┌──────────────────┐     ┌────────────────────┐
              │ OrderRepository  │────▶│ OrderRepository     │
              │ (Output Port)    │     │   Adapter (JPA)     │
              └──────────────────┘     └────────┬───────────┘
                                                │
                                                ▼
                                       ┌────────────────────┐
                                       │  OrderJpaRepository │
                                       │  (Spring Data)      │
                                       └────────┬───────────┘
                                                │
                                                ▼
                                           PostgreSQL
```

### Guía Detallada (Junior/Mid) — Ejemplo End-to-End: Crear Pedido

**Paso 1**: El cliente envía un HTTP POST

```json
POST /api/v1/orders
{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "street": "Av. Corrientes 1234",
    "city": "Buenos Aires",
    "state": "CABA",
    "zipCode": "C1043",
    "country": "Argentina",
    "items": [
        {
            "productId": "PROD-001",
            "productName": "Laptop ThinkPad",
            "quantity": 1,
            "unitPrice": 1299.99
        },
        {
            "productId": "PROD-002",
            "productName": "Mouse Inalámbrico",
            "quantity": 2,
            "unitPrice": 29.99
        }
    ]
}
```

**Paso 2**: `OrderController` recibe el request, convierte a Command, y delega al Input Port

```
OrderController.createOrder(request)
  └─► request.toCommand()           → CreateOrderCommand (record)
  └─► createOrderUseCase.execute()  → delega al Application Service
```

**Paso 3**: `OrderApplicationService` orquesta todo el flujo

```
OrderApplicationService.execute(command)
  │
  ├─► Convierte Command → objetos de dominio (CustomerId, Address, List<OrderItem>)
  │
  ├─► Order.create(customerId, address, items)
  │     └─► Valida datos obligatorios
  │     └─► Genera OrderId (UUID)
  │     └─► Establece status = PENDING
  │     └─► Calcula totalAmount sumando subtotales
  │     └─► Registra OrderCreatedEvent internamente
  │
  ├─► orderRepository.findActiveByCustomerId()     ← Output Port
  ├─► orderDomainService.validateNewOrder()         ← Validaciones globales
  │
  ├─► orderRepository.save(order)                   ← Output Port → JPA Adapter → DB
  │     └─► OrderPersistenceMapper.toJpaEntity()    ← Domain → JPA
  │     └─► jpaRepository.save()                    ← Spring Data JPA
  │     └─► OrderPersistenceMapper.toDomainEntity() ← JPA → Domain
  │
  ├─► eventPublisher.publish(events)                ← Output Port → Spring Events
  │     └─► @TransactionalEventListener             ← Se ejecuta DESPUÉS del commit
  │
  └─► mapper.toResponse(savedOrder)                 ← Domain → Response DTO
```

**Paso 4**: El cliente recibe la respuesta

```json
HTTP 201 Created
{
    "orderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PENDING",
    "totalAmount": 1359.97,
    "currency": "USD",
    "items": [
        {
            "productId": "PROD-001",
            "productName": "Laptop ThinkPad",
            "quantity": 1,
            "unitPrice": 1299.99,
            "subtotal": 1299.99
        },
        {
            "productId": "PROD-002",
            "productName": "Mouse Inalámbrico",
            "quantity": 2,
            "unitPrice": 29.99,
            "subtotal": 59.98
        }
    ],
    "createdAt": "2025-01-15T14:30:00Z"
}
```

---

## 6. Errores Comunes y Anti-patterns

### 6.1. Lógica de Negocio en el Application Service

❌ **Anti-pattern**: Poner validaciones y cálculos en el Application Service

```java
// MAL — El Application Service decide la lógica de negocio
public class OrderApplicationService {

    @Transactional
    public OrderResponse createOrder(CreateOrderCommand cmd) {
        // Lógica de negocio FUERA del dominio
        if (cmd.items().isEmpty()) {
            throw new ValidationException("Debe tener items");
        }

        var total = cmd.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.TEN) < 0) {
            throw new ValidationException("Monto mínimo $10");
        }

        var order = new Order();      // Constructor vacío
        order.setStatus("PENDING");   // Setter público
        order.setTotal(total);        // Cálculo fuera del dominio
        // ...
    }
}
```

✅ **Correcto**: Delegar al Aggregate Root

```java
// BIEN — El Application Service solo orquesta
public class OrderApplicationService {

    @Transactional
    public OrderResponse execute(CreateOrderCommand cmd) {
        var items = mapItems(cmd);
        var order = Order.create(customerId, address, items);  // Lógica en el dominio
        orderDomainService.validateNewOrder(order, activeOrders);
        var saved = orderRepository.save(order);
        return mapper.toResponse(saved);
    }
}
```

### 6.2. JPA Entity como Domain Entity (Anemic Domain)

❌ **Anti-pattern**: Usar la misma clase para JPA y para el dominio

```java
// MAL — La "entidad de dominio" tiene anotaciones JPA
@Entity
@Table(name = "orders")
@Data   // Lombok genera setters públicos para todo
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status")
    private String status;

    @Column(name = "total")
    private BigDecimal total;

    // Sin métodos de negocio — modelo anémico
    // Los setters permiten cualquier cambio sin validación
}
```

✅ **Correcto**: Separar ambas clases con un mapper

```java
// BIEN — Domain Entity rica, sin JPA
public class Order extends AggregateRoot<OrderId> {
    public void cancel(String reason) { /* lógica de negocio */ }
    // Sin setters públicos, sin anotaciones JPA
}

// BIEN — JPA Entity separada, solo para persistencia
@Entity @Table(name = "orders")
public class OrderJpaEntity {
    @Id private UUID id;
    // Setters OK aquí, es infraestructura
}
```

### 6.3. Dominio Acoplado a Spring

❌ **Anti-pattern**: Usar anotaciones Spring en la capa domain

```java
// MAL — El dominio depende de Spring
package com.acme.order.domain.service;

import org.springframework.stereotype.Service;        // ❌ Dependencia de Spring
import org.springframework.beans.factory.annotation.Value; // ❌ Dependencia de Spring

@Service  // ❌ El dominio no debería saber que Spring existe
public class OrderDomainService {

    @Value("${order.min-amount}")  // ❌ Config de Spring en el dominio
    private BigDecimal minAmount;
}
```

✅ **Correcto**: Sin anotaciones de Spring, registrar en BeanConfiguration

```java
// BIEN — Puro Java
package com.acme.order.domain.service;

public class OrderDomainService {
    private static final Money MINIMUM_ORDER_AMOUNT = Money.of(10.00);
    // Sin @Service, sin @Value, sin imports de Spring
}

// BeanConfiguration.java (en módulo container)
@Bean
public OrderDomainService orderDomainService() {
    return new OrderDomainService();
}
```

### 6.4. No Separar Domain Events de Integration Events

❌ **Anti-pattern**: Publicar el Domain Event tal cual hacia sistemas externos

```java
// MAL — El Domain Event sale directo a RabbitMQ
@TransactionalEventListener
public void onOrderCreated(OrderCreatedEvent event) {
    // Publicar el domain event directamente a la cola
    rabbitTemplate.convertAndSend("orders", event);  // ❌
    // Problema: cambiar el domain event rompe contratos con sistemas externos
}
```

✅ **Correcto**: Convertir Domain Event a Integration Event antes de publicar

```java
// BIEN — Convertir a Integration Event
@TransactionalEventListener
public void onOrderCreated(OrderCreatedEvent domainEvent) {
    // Convertir a Integration Event (contrato con sistemas externos)
    var integrationEvent = new OrderCreatedIntegrationEvent(
            domainEvent.getOrderId().toString(),
            domainEvent.getCustomerId().toString(),
            domainEvent.getTotalAmount().amount(),
            domainEvent.getCreatedAt()
    );
    rabbitTemplate.convertAndSend("orders.exchange", "order.created", integrationEvent);
}
```

**Diferencia clave**:
| Aspecto | Domain Event | Integration Event |
|---------|-------------|-------------------|
| Alcance | Dentro del bounded context | Entre bounded contexts / servicios |
| Formato | Objetos de dominio (Value Objects) | DTOs serializables (String, BigDecimal) |
| Versionado | Puede cambiar libremente | Requiere backward compatibility |
| Transporte | ApplicationEventPublisher (in-process) | RabbitMQ, Kafka (out-of-process) |

### 6.5. Aggregate Root sin Validaciones

❌ **Anti-pattern**: Setters públicos que permiten estados inválidos

```java
// MAL — Cualquier clase externa puede dejar el aggregate en estado inconsistente
public class Order {
    public void setStatus(OrderStatus status) { this.status = status; }  // Sin validación
    public void setItems(List<OrderItem> items) { this.items = items; }  // Sin validación
    public void setTotal(Money total) { this.total = total; }            // Sin validación
}

// En algún servicio:
order.setStatus(OrderStatus.SHIPPED);  // ¿Se puede enviar si no está pagado?
order.setItems(List.of());             // ¿Un pedido sin items?
order.setTotal(Money.of(-50));         // ¿Monto negativo?
```

✅ **Correcto**: Métodos de negocio que protegen invariantes

```java
// BIEN — Solo métodos con significado de negocio
public class Order extends AggregateRoot<OrderId> {

    public void confirm() {
        assertOrderIsPending();          // Validación de estado
        this.status = OrderStatus.CONFIRMED;
    }

    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new InvalidOrderStateException("Solo se puede enviar un pedido pagado");
        }
        this.status = OrderStatus.SHIPPED;
    }
    // Sin setters públicos
}
```

### 6.6. Value Objects Mutables

❌ **Anti-pattern**: Value Objects que permiten mutación

```java
// MAL — Value Object mutable (class, no record)
public class Money {
    private BigDecimal amount;
    private String currency;

    public void setAmount(BigDecimal amount) {    // ❌ Mutable
        this.amount = amount;
    }

    public void add(Money other) {
        this.amount = this.amount.add(other.amount);  // ❌ Modifica el original
    }
}
```

✅ **Correcto**: Value Objects inmutables con records

```java
// BIEN — Inmutable por diseño (record)
public record Money(BigDecimal amount, Currency currency) {

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);  // ✅ Retorna nuevo
    }
}
```

---

## 7. Checklist de Implementación

### Módulos Maven

- [ ] Parent POM con `<modules>` declarados
- [ ] Módulo `domain` sin dependencias de Spring en su `pom.xml`
- [ ] Módulo `application` depende solo de `domain` (y opcionalmente `spring-tx`)
- [ ] Módulo `infrastructure` depende de `application` (transitivamente de `domain`)
- [ ] Módulo `container` depende de los tres módulos, tiene `spring-boot-maven-plugin`
- [ ] El build compila con `mvn clean install` desde el parent

### Capa Domain

- [ ] `BaseEntity<ID>` con equals/hashCode por identidad
- [ ] `AggregateRoot<ID>` extiende `BaseEntity`, gestiona domain events internamente
- [ ] Aggregate Root usa factory method (`create`) en vez de constructor público
- [ ] Aggregate Root tiene método `reconstruct` para reconstrucción desde persistencia
- [ ] Todos los cambios de estado se hacen via métodos de negocio (sin setters públicos)
- [ ] Las transiciones de estado validan pre-condiciones
- [ ] Value Objects implementados como records con validación en constructor compacto
- [ ] Typed IDs (OrderId, CustomerId) en vez de `Long`/`UUID` crudo
- [ ] Domain Events nombrados en pasado (OrderCreated, NOT OrderCreate)
- [ ] Domain Events acumulados en el Aggregate Root, NO publicados inmediatamente
- [ ] Domain Service sin anotaciones de Spring
- [ ] Domain Exceptions sin dependencias de HttpStatus

### Capa Application

- [ ] Input Ports definidos como interfaces (un caso de uso = un puerto)
- [ ] Output Ports definidos como interfaces (repository, event publisher)
- [ ] Application Service implementa los Input Ports
- [ ] Application Service NO contiene lógica de negocio
- [ ] `@Transactional` en los métodos del Application Service
- [ ] Domain Events despachados DESPUÉS de persistir
- [ ] `clearDomainEvents()` llamado después de publicar
- [ ] Commands y Responses como records inmutables

### Capa Infrastructure

- [ ] JPA Entities separadas de Domain Entities
- [ ] Mapper entre JPA Entity y Domain Entity
- [ ] Repository Adapter implementa el Output Port
- [ ] REST Controller solo delega al Input Port (sin lógica)
- [ ] Event Publisher Adapter usa ApplicationEventPublisher
- [ ] `@TransactionalEventListener` para side effects post-commit

### Container

- [ ] `@SpringBootApplication` en el paquete raíz
- [ ] `BeanConfiguration` registra manualmente beans de domain y application
- [ ] Los beans de infrastructure usan `@Component`/`@Repository` (están en su módulo, OK)

---

*Documento generado con Context7 - Spring Framework 6.2, Spring Boot 3.4.x*
