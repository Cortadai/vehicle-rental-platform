# Área 11: Migraciones de Base de Datos con Flyway

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, PostgreSQL, Flyway 10.x

---

## 1. Naming Conventions

### Referencia Rápida (Seniors)

| Tipo | Prefijo | Formato | Ejemplo |
|------|---------|---------|---------|
| **Versionada** | `V` | `V{version}__{descripcion}.sql` | `V1.0.0__Create_customer_table.sql` |
| **Repetible** | `R` | `R__{descripcion}.sql` | `R__Update_customer_view.sql` |
| **Undo** | `U` | `U{version}__{descripcion}.sql` | `U1.0.0__Undo_create_customer.sql` |

```
V1.0.0__Create_schema.sql          ← Migración versionada (se ejecuta una vez)
V1.0.1__Create_customer_table.sql   ← Siguiente versión
V1.1.0__Add_email_index.sql         ← Minor version
R__Update_customer_view.sql         ← Repetible (se ejecuta si cambia el checksum)
U1.0.1__Undo_create_customer.sql    ← Undo (solo Teams/Enterprise)
```

### Guía Detallada (Junior/Mid)

#### Anatomía del nombre de archivo

```
V  1.0.0  __  Create_customer_table  .sql
│  │       │   │                      │
│  │       │   │                      └─ Sufijo obligatorio
│  │       │   └─ Descripción (underscores como separador)
│  │       └─ Separador (doble underscore, OBLIGATORIO)
│  └─ Versión (puntos o underscores)
└─ Prefijo (V=versionada, R=repetible, U=undo)
```

**Por que importa**: Flyway ordena las migraciones por version. Si el nombre no cumple el patron, Flyway ignora el archivo silenciosamente.

#### Versionado semantico vs timestamps

**Opcion 1: Versionado semantico (recomendado para equipos pequenos)**

```
V1.0.0__Create_schema.sql
V1.0.1__Create_customer_table.sql
V1.0.2__Create_order_table.sql
V1.1.0__Add_email_index.sql
V2.0.0__Restructure_orders.sql
```

**Opcion 2: Timestamps (recomendado para equipos grandes)**

```
V20250115100000__Create_customer_table.sql
V20250115110000__Create_order_table.sql
V20250120093000__Add_email_index.sql
```

**Por que timestamps**: Eliminan conflictos de version cuando multiples desarrolladores crean migraciones simultaneamente.

#### Buenas practicas de nombres

```sql
-- BIEN: Descriptivo, indica la accion y la tabla afectada
V1.0.1__Create_customer_table.sql
V1.0.2__Add_email_column_to_customer.sql
V1.0.3__Create_index_on_customer_email.sql
V1.1.0__Add_shipping_address_to_order.sql

-- MAL: Ambiguo, no se sabe que hace sin abrir el archivo
V1__update.sql
V2__changes.sql
V3__fix.sql
```

---

## 2. Estructura de Proyecto

### Referencia Rápida (Seniors)

```
src/main/resources/
├── db/
│   ├── migration/              ← Migraciones versionadas
│   │   ├── V1.0.0__Create_schema.sql
│   │   ├── V1.0.1__Create_customer_table.sql
│   │   ├── V1.0.2__Create_order_table.sql
│   │   ├── V1.0.3__Create_product_table.sql
│   │   ├── V1.1.0__Add_email_index.sql
│   │   └── V2.0.0__Restructure_orders.sql
│   ├── callback/               ← Callbacks SQL
│   │   ├── afterMigrate__refresh_views.sql
│   │   └── beforeClean__check_schema.sql
│   └── testdata/               ← Datos de prueba (solo en dev/test)
│       └── R__Insert_test_data.sql
```

### Guía Detallada (Junior/Mid)

#### Ubicacion por defecto

Spring Boot auto-configura Flyway para buscar migraciones en `classpath:db/migration`. No es necesario configurar nada adicional si se usa esta ruta.

```
src/main/resources/
└── db/
    └── migration/
        ├── V1.0.0__Create_schema.sql
        └── V1.0.1__Create_customer_table.sql
```

#### Estructura avanzada con multiples locations

```yaml
# application.yml - Separar migraciones por modulo
spring:
  flyway:
    locations:
      - classpath:db/migration/common
      - classpath:db/migration/customers
      - classpath:db/migration/orders
```

```
src/main/resources/
└── db/
    ├── migration/
    │   ├── common/
    │   │   ├── V1.0.0__Create_extensions.sql
    │   │   └── V1.0.1__Create_audit_table.sql
    │   ├── customers/
    │   │   ├── V1.1.0__Create_customer_table.sql
    │   │   └── V1.1.1__Add_customer_indexes.sql
    │   └── orders/
    │       ├── V1.2.0__Create_order_table.sql
    │       └── V1.2.1__Create_order_item_table.sql
    ├── callback/
    │   ├── afterMigrate__refresh_views.sql
    │   └── afterMigrate__vacuum_analyze.sql
    └── testdata/
        └── R__Insert_test_data.sql
```

#### Datos de prueba solo en dev/test

```yaml
# application-dev.yml
spring:
  flyway:
    locations:
      - classpath:db/migration
      - classpath:db/testdata       # Solo en dev/test

# application-prod.yml
spring:
  flyway:
    locations:
      - classpath:db/migration      # Solo migraciones en prod
```

---

## 3. Configuración en Spring Boot

### Referencia Rápida (Seniors)

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    out-of-order: false
    validate-on-migrate: true
    clean-disabled: true              # SIEMPRE true en produccion
    table: flyway_schema_history
    default-schema: ${DB_SCHEMA:public}
    connect-retries: 3
    connect-retries-interval: 1
    installed-by: ${spring.application.name}
```

### Guía Detallada (Junior/Mid)

#### Dependencia Maven

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- Driver de PostgreSQL para Flyway 10.x -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

#### Configuracion por ambiente

```yaml
# application.yml - Configuracion base (todos los ambientes)
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:acme_sales}
    username: ${DB_USER:acme}
    password: ${DB_PASSWORD:secret}

  flyway:
    enabled: true
    locations: classpath:db/migration
    table: flyway_schema_history
    validate-on-migrate: true
    baseline-on-migrate: false
    installed-by: acme-sales-service
```

```yaml
# application-dev.yml - Desarrollo local
spring:
  flyway:
    locations:
      - classpath:db/migration
      - classpath:db/testdata         # Datos de prueba en dev
    clean-disabled: false              # Permitir clean en dev
    out-of-order: true                 # Permitir migraciones fuera de orden en dev
```

```yaml
# application-prod.yml - Produccion
spring:
  flyway:
    locations: classpath:db/migration
    clean-disabled: true               # NUNCA permitir clean en prod
    out-of-order: false                # Orden estricto en prod
    baseline-on-migrate: false
    validate-on-migrate: true
    connect-retries: 5
    connect-retries-interval: 2
```

#### Tabla de propiedades clave

| Propiedad | Default | Dev | Prod | Descripcion |
|-----------|---------|-----|------|-------------|
| `enabled` | `true` | `true` | `true` | Habilita/deshabilita Flyway |
| `clean-disabled` | `true` | `false` | **`true`** | Evita borrado accidental del schema |
| `out-of-order` | `false` | `true` | `false` | Permite aplicar migraciones fuera de secuencia |
| `baseline-on-migrate` | `false` | `false` | solo si migra DB existente | Crea baseline en BD existente |
| `validate-on-migrate` | `true` | `true` | `true` | Valida checksums antes de migrar |
| `locations` | `classpath:db/migration` | + testdata | solo migration | Rutas de migraciones |

---

## 4. Escritura de Migraciones

### Referencia Rápida (Seniors)

```sql
-- V1.0.0__Create_schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS sales;

-- V1.0.1__Create_customer_table.sql
CREATE TABLE IF NOT EXISTS sales.customer (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuid_generate_v4() UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    telefono        VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_customer_email ON sales.customer(email);
```

### Guía Detallada (Junior/Mid)

#### CREATE TABLE con constraints

```sql
-- V1.0.1__Create_customer_table.sql
-- Descripcion: Crea la tabla de clientes con restricciones de integridad
-- Autor: equipo-ventas
-- Fecha: 2025-01-15

CREATE TABLE IF NOT EXISTS sales.customer (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    telefono        VARCHAR(20),
    direccion       TEXT,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Restricciones
    CONSTRAINT uk_customer_uuid UNIQUE (uuid),
    CONSTRAINT uk_customer_email UNIQUE (email),
    CONSTRAINT chk_customer_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Comentarios en tabla y columnas (buena practica en PostgreSQL)
COMMENT ON TABLE sales.customer IS 'Tabla principal de clientes del sistema de ventas';
COMMENT ON COLUMN sales.customer.uuid IS 'Identificador publico del cliente (expuesto en APIs)';
```

#### CREATE TABLE con relaciones (pedidos)

```sql
-- V1.0.2__Create_order_tables.sql

CREATE TABLE IF NOT EXISTS sales.pedido (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuid_generate_v4() UNIQUE,
    customer_id     BIGINT NOT NULL,
    estado          VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    total           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    moneda          VARCHAR(3) NOT NULL DEFAULT 'USD',
    notas           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pedido_customer FOREIGN KEY (customer_id)
        REFERENCES sales.customer(id) ON DELETE RESTRICT,
    CONSTRAINT chk_pedido_estado CHECK (
        estado IN ('PENDIENTE', 'CONFIRMADO', 'EN_PROCESO', 'ENVIADO', 'ENTREGADO', 'CANCELADO')
    ),
    CONSTRAINT chk_pedido_total CHECK (total >= 0)
);

CREATE TABLE IF NOT EXISTS sales.pedido_item (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    cantidad        INTEGER NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    subtotal        NUMERIC(12, 2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,

    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id)
        REFERENCES sales.pedido(id) ON DELETE CASCADE,
    CONSTRAINT chk_item_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_item_precio CHECK (precio_unitario > 0)
);

-- Indices para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_pedido_customer ON sales.pedido(customer_id);
CREATE INDEX IF NOT EXISTS idx_pedido_estado ON sales.pedido(estado);
CREATE INDEX IF NOT EXISTS idx_pedido_created ON sales.pedido(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_item_pedido ON sales.pedido_item(pedido_id);
```

#### ALTER TABLE para cambios de schema

```sql
-- V1.1.0__Add_shipping_address_to_order.sql
-- Cambio no destructivo: agrega columnas nuevas con defaults

ALTER TABLE sales.pedido
    ADD COLUMN IF NOT EXISTS direccion_envio TEXT,
    ADD COLUMN IF NOT EXISTS ciudad VARCHAR(100),
    ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(10),
    ADD COLUMN IF NOT EXISTS pais VARCHAR(3) DEFAULT 'ARG';

COMMENT ON COLUMN sales.pedido.pais IS 'Codigo ISO 3166-1 alpha-3 del pais de envio';
```

#### Migraciones de datos

```sql
-- V1.1.1__Migrate_customer_addresses.sql
-- Migra direcciones existentes del campo texto libre a columnas separadas
-- IMPORTANTE: Este tipo de migraciones debe ser idempotente

UPDATE sales.pedido p
SET direccion_envio = c.direccion
FROM sales.customer c
WHERE p.customer_id = c.id
  AND p.direccion_envio IS NULL
  AND c.direccion IS NOT NULL;
```

#### Creacion de indices (CONCURRENTLY)

```sql
-- V1.2.0__Add_product_search_index.sql
-- IMPORTANTE: CREATE INDEX CONCURRENTLY no puede ejecutarse dentro de una transaccion.
-- Flyway por defecto ejecuta cada migracion en una transaccion.
-- Para PostgreSQL, se necesita desactivar la transaccion para esta migracion.

-- flyway:executeInTransaction=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_producto_nombre_gin
    ON sales.producto USING gin(to_tsvector('spanish', nombre));
```

#### Patrones idempotentes

```sql
-- V1.3.0__Create_producto_table_idempotent.sql
-- Patron idempotente: la migracion puede ejecutarse multiples veces sin error

-- Tabla
CREATE TABLE IF NOT EXISTS sales.producto (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuid_generate_v4() UNIQUE,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    precio          NUMERIC(10, 2) NOT NULL,
    stock           INTEGER NOT NULL DEFAULT 0,
    categoria       VARCHAR(50),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_producto_precio CHECK (precio > 0),
    CONSTRAINT chk_producto_stock CHECK (stock >= 0)
);

-- Indice
CREATE INDEX IF NOT EXISTS idx_producto_categoria ON sales.producto(categoria);
CREATE INDEX IF NOT EXISTS idx_producto_activo ON sales.producto(activo) WHERE activo = TRUE;

-- Funcion para actualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION sales.trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger (usar DO block para idempotencia)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_producto_updated_at'
    ) THEN
        CREATE TRIGGER trg_producto_updated_at
            BEFORE UPDATE ON sales.producto
            FOR EACH ROW
            EXECUTE FUNCTION sales.trigger_set_updated_at();
    END IF;
END $$;
```

#### Funcionalidades especificas de PostgreSQL

```sql
-- V1.4.0__Add_postgresql_features.sql

-- Tipo ENUM personalizado
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_pedido') THEN
        CREATE TYPE sales.estado_pedido AS ENUM (
            'PENDIENTE', 'CONFIRMADO', 'EN_PROCESO',
            'ENVIADO', 'ENTREGADO', 'CANCELADO'
        );
    END IF;
END $$;

-- Columna JSONB para metadata flexible
ALTER TABLE sales.pedido
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- Indice GIN para consultas JSONB
CREATE INDEX IF NOT EXISTS idx_pedido_metadata ON sales.pedido USING gin(metadata);

-- Tabla particionada por fecha (PostgreSQL 10+)
CREATE TABLE IF NOT EXISTS sales.pedido_auditoria (
    id              BIGSERIAL,
    pedido_id       BIGINT NOT NULL,
    accion          VARCHAR(20) NOT NULL,
    datos_anteriores JSONB,
    datos_nuevos    JSONB,
    usuario         VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Particiones por trimestre
CREATE TABLE IF NOT EXISTS sales.pedido_auditoria_2025_q1
    PARTITION OF sales.pedido_auditoria
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');

CREATE TABLE IF NOT EXISTS sales.pedido_auditoria_2025_q2
    PARTITION OF sales.pedido_auditoria
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
```

---

## 5. Estrategia de Versionado

### Referencia Rápida (Seniors)

```
Versionado semantico:     V{major}.{minor}.{patch}__Descripcion.sql
Timestamps:               V{yyyyMMddHHmmss}__Descripcion.sql
Conflictos en branches:   Usar timestamps o reservar rangos de version por modulo
```

### Guía Detallada (Junior/Mid)

#### Versionado semantico para migraciones

```
V1.0.0 → V1.0.1 → V1.0.2 → V1.1.0 → V2.0.0
  │         │         │        │         │
  │         │         │        │         └─ Cambio mayor (restructura)
  │         │         │        └─ Funcionalidad nueva (indice, columna)
  │         │         └─ Correccion de datos
  │         └─ Nueva tabla dentro del mismo modulo
  └─ Schema inicial
```

#### Resolucion de conflictos entre desarrolladores

**Problema**: Dos desarrolladores crean `V1.0.5` en branches diferentes.

**Solucion 1: Rangos por modulo**

```
Modulo Clientes:   V1.1.xxx
Modulo Pedidos:    V1.2.xxx
Modulo Productos:  V1.3.xxx
Modulo Reportes:   V1.4.xxx
```

**Solucion 2: Timestamps (mas escalable)**

```
-- Developer A (branch feature/add-discount)
V20250120143000__Add_discount_column.sql

-- Developer B (branch feature/add-tags)
V20250120150000__Add_tags_table.sql
```

#### Estrategia en feature branches

```
main:
  V1.0.0__Create_schema.sql
  V1.0.1__Create_customer_table.sql
  V1.0.2__Create_order_table.sql

feature/loyalty-program:
  V1.0.3__Create_loyalty_points_table.sql   ← Puede conflicturar

Mejor con timestamps:
feature/loyalty-program:
  V20250120160000__Create_loyalty_points_table.sql   ← Sin conflicto
```

**Regla del equipo**: Si se usa versionado semantico, el merge a main debe validar que no existan versiones duplicadas. En el pipeline de CI se ejecuta `flyway validate` para detectar conflictos.

---

## 6. Migraciones Repetibles

### Referencia Rápida (Seniors)

```sql
-- R__Create_customer_summary_view.sql
-- Se re-ejecuta automaticamente si cambia el checksum del archivo
CREATE OR REPLACE VIEW sales.v_customer_summary AS
SELECT c.id, c.nombre, c.email, COUNT(p.id) AS total_pedidos, COALESCE(SUM(p.total), 0) AS monto_total
FROM sales.customer c
LEFT JOIN sales.pedido p ON p.customer_id = c.id
GROUP BY c.id, c.nombre, c.email;
```

### Guía Detallada (Junior/Mid)

Las migraciones repetibles (prefijo `R__`) se ejecutan cada vez que cambia su checksum. Se aplican **despues** de todas las migraciones versionadas. Son ideales para objetos que se recrean completamente.

#### Vistas

```sql
-- R__Create_order_dashboard_view.sql
CREATE OR REPLACE VIEW sales.v_order_dashboard AS
SELECT
    p.id AS pedido_id,
    p.uuid AS pedido_uuid,
    c.nombre || ' ' || c.apellido AS cliente,
    c.email AS cliente_email,
    p.estado,
    p.total,
    p.moneda,
    COUNT(pi.id) AS total_items,
    p.created_at AS fecha_pedido,
    p.updated_at AS ultima_actualizacion
FROM sales.pedido p
JOIN sales.customer c ON c.id = p.customer_id
LEFT JOIN sales.pedido_item pi ON pi.pedido_id = p.id
GROUP BY p.id, p.uuid, c.nombre, c.apellido, c.email,
         p.estado, p.total, p.moneda, p.created_at, p.updated_at;
```

#### Funciones almacenadas

```sql
-- R__Create_calculate_order_total_function.sql
CREATE OR REPLACE FUNCTION sales.calculate_order_total(p_pedido_id BIGINT)
RETURNS NUMERIC AS $$
DECLARE
    v_total NUMERIC(12, 2);
BEGIN
    SELECT COALESCE(SUM(cantidad * precio_unitario), 0)
    INTO v_total
    FROM sales.pedido_item
    WHERE pedido_id = p_pedido_id;

    -- Actualizar el total en la tabla de pedidos
    UPDATE sales.pedido
    SET total = v_total,
        updated_at = NOW()
    WHERE id = p_pedido_id;

    RETURN v_total;
END;
$$ LANGUAGE plpgsql;
```

#### Datos de prueba (solo dev/test)

```sql
-- R__Insert_test_data.sql (en carpeta testdata/)
-- Se regenera cada vez que cambia este archivo

TRUNCATE sales.customer CASCADE;
TRUNCATE sales.producto CASCADE;

INSERT INTO sales.customer (email, nombre, apellido, telefono) VALUES
    ('maria.garcia@test.com', 'Maria', 'Garcia', '+54 11 5555-0001'),
    ('juan.perez@test.com', 'Juan', 'Perez', '+54 11 5555-0002'),
    ('ana.lopez@test.com', 'Ana', 'Lopez', '+54 11 5555-0003');

INSERT INTO sales.producto (nombre, descripcion, precio, stock, categoria) VALUES
    ('Laptop Pro 15', 'Laptop profesional 15 pulgadas', 1299.99, 50, 'ELECTRONICA'),
    ('Mouse Inalambrico', 'Mouse ergonomico bluetooth', 29.99, 200, 'ACCESORIOS'),
    ('Teclado Mecanico', 'Teclado mecanico RGB', 89.99, 100, 'ACCESORIOS');
```

**Importante**: Las migraciones repetibles no tienen version. Flyway las ejecuta en orden alfabetico de su descripcion.

---

## 7. Testing de Migraciones

### Referencia Rápida (Seniors)

```java
@SpringBootTest
@Testcontainers
class FlywayMigrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private Flyway flyway;

    @Test
    void allMigrationsApplySuccessfully() {
        var result = flyway.migrate();
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThan(0);
    }
}
```

### Guía Detallada (Junior/Mid)

#### Dependencias de test

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

#### Test completo de migraciones con Testcontainers

```java
package com.acme.sales.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("acme_sales_test")
                    .withUsername("test")
                    .withPassword("test");

    private Flyway flyway;

    @BeforeEach
    void setUp() {
        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        // Limpiar antes de cada test para empezar desde cero
        flyway.clean();
    }

    @Test
    void allMigrationsShouldApplySuccessfully() {
        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThan(0);
    }

    @Test
    void migrationsShouldBeValidAfterApplying() {
        flyway.migrate();

        // Validate verifica que las migraciones aplicadas coinciden con los archivos
        flyway.validate();
    }

    @Test
    void shouldHaveExpectedNumberOfMigrations() {
        flyway.migrate();

        MigrationInfoService info = flyway.info();
        MigrationInfo[] applied = info.applied();

        // Verificar que se aplicaron todas las migraciones esperadas
        assertThat(applied).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void shouldCreateExpectedTables() {
        flyway.migrate();

        // Verificar que las tablas existen
        try (var connection = postgres.createConnection("")) {
            var metaData = connection.getMetaData();

            assertThat(tableExists(metaData, "customer")).isTrue();
            assertThat(tableExists(metaData, "pedido")).isTrue();
            assertThat(tableExists(metaData, "pedido_item")).isTrue();
            assertThat(tableExists(metaData, "producto")).isTrue();
        } catch (Exception e) {
            throw new RuntimeException("Error verificando tablas", e);
        }
    }

    private boolean tableExists(java.sql.DatabaseMetaData metaData, String tableName)
            throws java.sql.SQLException {
        try (var rs = metaData.getTables(null, "sales", tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
```

#### Validacion de migraciones en CI/CD

```yaml
# .github/workflows/flyway-validate.yml
name: Validate Flyway Migrations

on:
  pull_request:
    paths:
      - 'src/main/resources/db/**'

jobs:
  validate-migrations:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: acme_sales_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Flyway Validate
        run: ./mvnw flyway:validate -Dflyway.url=jdbc:postgresql://localhost:5432/acme_sales_test -Dflyway.user=test -Dflyway.password=test

      - name: Run Flyway Migrate (dry run)
        run: ./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/acme_sales_test -Dflyway.user=test -Dflyway.password=test
```

---

## 8. Consideraciones de Produccion

### Referencia Rápida (Seniors)

```yaml
# PRODUCCION - Configuracion critica
spring:
  flyway:
    clean-disabled: true          # NUNCA false en produccion
    out-of-order: false           # Orden estricto
    validate-on-migrate: true     # Validar checksums
    baseline-on-migrate: false    # Solo true para adoptar Flyway en BD existente
    connect-retries: 5
    connect-retries-interval: 2
```

### Guía Detallada (Junior/Mid)

#### NUNCA usar clean en produccion

`flyway.clean()` **elimina todos los objetos** del schema (tablas, vistas, funciones, datos). Configurar `clean-disabled: true` es **obligatorio** en produccion.

```yaml
# application-prod.yml
spring:
  flyway:
    clean-disabled: true   # Si alguien ejecuta clean, Flyway lanza FlywayException
```

#### baseline-on-migrate para bases de datos existentes

Cuando se adopta Flyway en un proyecto con una base de datos existente:

```yaml
# Solo la PRIMERA vez que se integra Flyway en una BD existente
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: '1.0.0'
    baseline-description: 'Baseline de BD existente'
```

```sql
-- V1.0.0__Baseline.sql (no se ejecuta, es el punto de partida)
-- Este archivo representa el estado actual de la BD
-- Flyway lo marca como "aplicado" via baseline

-- V1.0.1__Add_new_feature.sql (esta SI se ejecuta)
ALTER TABLE sales.customer ADD COLUMN IF NOT EXISTS loyalty_points INTEGER DEFAULT 0;
```

**Importante**: Despues de la primera ejecucion, cambiar `baseline-on-migrate` a `false`.

#### Migraciones en tablas grandes (zero-downtime)

```sql
-- MAL: Bloquea la tabla completa durante ALTER
ALTER TABLE sales.pedido ADD COLUMN descuento NUMERIC(5,2) NOT NULL DEFAULT 0;

-- BIEN: Agregar columna nullable primero (no bloquea)
-- V2.0.0__Add_discount_nullable.sql
ALTER TABLE sales.pedido ADD COLUMN IF NOT EXISTS descuento NUMERIC(5,2);

-- V2.0.1__Backfill_discount_values.sql (en batches)
DO $$
DECLARE
    batch_size INT := 10000;
    rows_updated INT;
BEGIN
    LOOP
        UPDATE sales.pedido
        SET descuento = 0
        WHERE id IN (
            SELECT id FROM sales.pedido
            WHERE descuento IS NULL
            LIMIT batch_size
            FOR UPDATE SKIP LOCKED
        );

        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        EXIT WHEN rows_updated = 0;

        RAISE NOTICE 'Actualizados % registros', rows_updated;
        PERFORM pg_sleep(0.1);  -- Pausa para no saturar
    END LOOP;
END $$;

-- V2.0.2__Set_discount_not_null.sql
ALTER TABLE sales.pedido ALTER COLUMN descuento SET DEFAULT 0;
ALTER TABLE sales.pedido ALTER COLUMN descuento SET NOT NULL;
```

#### Creacion de indices sin downtime

```sql
-- V2.1.0__Add_index_concurrently.sql
-- flyway:executeInTransaction=false

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pedido_fecha_estado
    ON sales.pedido(created_at, estado);
```

#### Patrones de zero-downtime para renombrar columnas

```sql
-- Paso 1: Agregar nueva columna
-- V3.0.0__Add_new_column_name.sql
ALTER TABLE sales.customer ADD COLUMN IF NOT EXISTS nombre_completo VARCHAR(200);

-- Paso 2: Copiar datos y mantener sincronizado con trigger
-- V3.0.1__Sync_column_data.sql
UPDATE sales.customer SET nombre_completo = nombre || ' ' || apellido WHERE nombre_completo IS NULL;

CREATE OR REPLACE FUNCTION sales.sync_nombre_completo()
RETURNS TRIGGER AS $$
BEGIN
    NEW.nombre_completo := NEW.nombre || ' ' || NEW.apellido;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_nombre_completo
    BEFORE INSERT OR UPDATE ON sales.customer
    FOR EACH ROW EXECUTE FUNCTION sales.sync_nombre_completo();

-- Paso 3: (en siguiente release) Marcar columnas viejas como deprecated
-- Paso 4: (en release posterior) Eliminar columnas viejas y trigger
```

#### Monitoreo de flyway_schema_history

```sql
-- Consulta para verificar el estado de migraciones en produccion
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 20;

-- Detectar migraciones fallidas
SELECT * FROM flyway_schema_history WHERE success = FALSE;
```

---

## 9. Integracion con Spring Boot 3.4.x

### Referencia Rápida (Seniors)

```java
// Auto-configuracion: Spring Boot detecta flyway-core en el classpath
// y ejecuta migraciones automaticamente al iniciar la aplicacion.
// Para personalizar: FlywayMigrationStrategy bean.

@Bean
public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
        flyway.validate();
        flyway.migrate();
    };
}
```

### Guía Detallada (Junior/Mid)

#### Auto-configuracion

Spring Boot 3.4.x detecta automaticamente `flyway-core` en el classpath y:
1. Crea un bean `Flyway` configurado con las propiedades `spring.flyway.*`
2. Usa el mismo `DataSource` de la aplicacion
3. Ejecuta `flyway.migrate()` al iniciar la aplicacion (antes de que Hibernate valide el schema)

No se necesita ninguna configuracion adicional si se sigue la estructura estandar (`db/migration`).

#### Callback personalizado en Java

```java
package com.acme.sales.infrastructure.persistence.flyway;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlywayAuditCallback implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE
            || event == Event.AFTER_MIGRATE_ERROR
            || event == Event.BEFORE_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        switch (event) {
            case BEFORE_MIGRATE -> log.info(
                "Iniciando migracion de BD. Schema: {}",
                context.getConfiguration().getDefaultSchema()
            );
            case AFTER_MIGRATE -> log.info(
                "Migracion completada exitosamente"
            );
            case AFTER_MIGRATE_ERROR -> log.error(
                "ERROR en migracion de BD. Revisar flyway_schema_history"
            );
            default -> { }
        }
    }

    @Override
    public String getCallbackName() {
        return "AuditCallback";
    }
}
```

#### Registrar callbacks como beans de Spring

```java
package com.acme.sales.infrastructure.persistence.flyway;

import org.flywaydb.core.api.callback.Callback;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FlywayCallbackConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(
            List<Callback> callbacks) {
        return configuration -> configuration.callbacks(
            callbacks.toArray(new Callback[0])
        );
    }
}
```

#### Multiples datasources

```java
package com.acme.sales.infrastructure.persistence.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class MultiDataSourceFlywayConfig {

    /**
     * Flyway para la base de datos principal (sales).
     */
    @Primary
    @Bean(initMethod = "migrate")
    public Flyway primaryFlyway(@Qualifier("primaryDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sales")
                .table("flyway_schema_history")
                .defaultSchema("sales")
                .load();
    }

    /**
     * Flyway para la base de datos de reportes.
     */
    @Bean(initMethod = "migrate")
    public Flyway reportsFlyway(@Qualifier("reportsDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/reports")
                .table("flyway_schema_history")
                .defaultSchema("reports")
                .load();
    }
}
```

```yaml
# application.yml - Multiples datasources
app:
  datasource:
    primary:
      url: jdbc:postgresql://localhost:5432/acme_sales
      username: sales_user
      password: ${DB_SALES_PASSWORD}
    reports:
      url: jdbc:postgresql://localhost:5432/acme_reports
      username: reports_user
      password: ${DB_REPORTS_PASSWORD}
```

#### @FlywayTest para tests

```java
package com.acme.sales.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class FlywaySpringBootIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("acme_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void flywayMigrationsRunOnStartup() {
        var info = flyway.info();
        assertThat(info.applied()).isNotEmpty();
        assertThat(info.pending()).isEmpty();
    }

    @Test
    void schemaIsCorrectAfterMigrations() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Verificar que las tablas del dominio existen
            ResultSet rs = conn.getMetaData()
                    .getTables(null, "sales", "customer", new String[]{"TABLE"});
            assertThat(rs.next()).isTrue();

            rs = conn.getMetaData()
                    .getTables(null, "sales", "pedido", new String[]{"TABLE"});
            assertThat(rs.next()).isTrue();
        }
    }
}
```

---

## Hacer / Evitar

### Hacer

1. **Usar `IF NOT EXISTS` / `IF EXISTS`** - Hacer migraciones idempotentes cuando sea posible
2. **Comentar las migraciones** - Incluir descripcion, autor y fecha al inicio del archivo SQL
3. **Un cambio logico por migracion** - No mezclar cambios de schema con migraciones de datos
4. **Testear migraciones con Testcontainers** - Validar contra PostgreSQL real, no H2
5. **Usar `CREATE INDEX CONCURRENTLY`** - En tablas con datos para evitar bloqueos
6. **Configurar `clean-disabled: true` en produccion** - Sin excepciones
7. **Validar en CI/CD** - Ejecutar `flyway validate` y `flyway migrate` en cada PR
8. **Nombres descriptivos** - `V1.2.0__Add_email_index_to_customer.sql` explica que hace sin abrirlo
9. **Usar migraciones repetibles para vistas y funciones** - `CREATE OR REPLACE` con prefijo `R__`
10. **Separar datos de prueba** - Usar carpeta `testdata/` y locations por perfil
11. **Backfill en batches** - Para migraciones de datos en tablas grandes
12. **Monitorear `flyway_schema_history`** - Verificar que no hay migraciones fallidas

### Evitar

1. **Modificar migraciones ya aplicadas** - Nunca cambiar un archivo SQL ya ejecutado en algun ambiente
2. **Usar `flyway.clean()` en produccion** - Borra todo el schema incluyendo datos
3. **ALTER TABLE con NOT NULL sin default en tablas grandes** - Bloquea la tabla completa
4. **Migraciones acopladas a datos existentes** - No asumir IDs especificos o datos que pueden no existir
5. **Nombres genericos** - `V3__update.sql` no comunica nada util
6. **Mezclar DDL y DML en la misma migracion** - Separar cambios de schema de migraciones de datos
7. **Ignorar el orden de ejecucion** - Las FK requieren que la tabla referenciada exista primero
8. **H2 para testear migraciones PostgreSQL** - La sintaxis difiere; usar Testcontainers
9. **`out-of-order: true` en produccion** - Puede causar estados inconsistentes
10. **Omitir transacciones** - Solo usar `executeInTransaction=false` cuando sea necesario (ej: `CONCURRENTLY`)
11. **Hardcodear credenciales** - Usar variables de entorno o gestores de secretos
12. **Saltear la validacion** - `validate-on-migrate: true` detecta modificaciones no autorizadas

---

## Checklist de Implementacion

### Configuracion Inicial
- [ ] Dependencia `flyway-core` y `flyway-database-postgresql` en `pom.xml`
- [ ] Carpeta `src/main/resources/db/migration/` creada
- [ ] `application.yml` con propiedades basicas de Flyway
- [ ] Perfiles separados para dev y prod (`clean-disabled`, `out-of-order`)
- [ ] Primera migracion `V1.0.0__Create_schema.sql` creada

### Naming y Estructura
- [ ] Convencion de nombres definida (semantico o timestamps)
- [ ] Nombres descriptivos en todas las migraciones
- [ ] Separador doble underscore (`__`) usado correctamente
- [ ] Migraciones repetibles (`R__`) para vistas y funciones
- [ ] Datos de prueba en carpeta separada (`testdata/`)

### Calidad de Migraciones
- [ ] Patrones idempotentes (`IF NOT EXISTS`, `IF EXISTS`, `CREATE OR REPLACE`)
- [ ] Constraints nombrados explicitamente (`CONSTRAINT fk_xxx`)
- [ ] Indices creados con `CONCURRENTLY` en tablas con datos
- [ ] Comentarios en tablas y columnas (`COMMENT ON`)
- [ ] Un cambio logico por archivo de migracion

### Produccion
- [ ] `clean-disabled: true` en produccion
- [ ] `validate-on-migrate: true` habilitado
- [ ] `out-of-order: false` en produccion
- [ ] Estrategia de zero-downtime para tablas grandes
- [ ] Monitoreo de `flyway_schema_history` configurado
- [ ] `baseline-on-migrate` configurado solo si se adopta Flyway en BD existente

### Testing
- [ ] Tests de migracion con Testcontainers (PostgreSQL real)
- [ ] Validacion de migraciones en pipeline CI/CD
- [ ] Tests verifican creacion de tablas, indices y constraints
- [ ] `flyway validate` ejecutado en cada PR
- [ ] Tests de integracion usan el mismo motor que produccion

### Integracion Spring Boot
- [ ] Auto-configuracion verificada (Flyway migra antes de Hibernate)
- [ ] Callbacks Java registrados como beans si se necesitan
- [ ] Configuracion de multiples datasources si aplica
- [ ] `FlywayMigrationStrategy` personalizado si se requiere logica extra

---

*Documento generado con Context7 - Flyway 10.x + Spring Boot 3.4.x*
