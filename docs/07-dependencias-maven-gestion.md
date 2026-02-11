# Área 7: Dependencias Maven y Gestión

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, Maven 3.9+

---

## 1. Spring Boot Parent vs BOM

### Referencia Rápida (Seniors)

| Enfoque | Cuándo usar | Ventaja |
|---------|-------------|---------|
| `spring-boot-starter-parent` | Proyecto nuevo, sin parent corporativo | Configuración completa automática |
| `spring-boot-dependencies` BOM | Parent corporativo existente | Flexibilidad, solo versiones |

### Guía Detallada (Junior/Mid)

#### Opción 1: Heredar de spring-boot-starter-parent (Recomendado)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Heredar de Spring Boot -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <groupId>com.acme</groupId>
    <artifactId>pedido-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Sin versión - gestionada por parent -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Sin versión ni configuración - heredada de parent -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Beneficios del parent**:
- Gestión de versiones de todas las dependencias Spring
- Configuración de plugins preconfigurada
- Resource filtering para `@project.version@`
- Configuración de encoding UTF-8
- Perfiles para native images

#### Opción 2: Usar BOM sin parent (Cuando hay parent corporativo)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <!-- Parent corporativo -->
    <parent>
        <groupId>com.acme</groupId>
        <artifactId>acme-parent</artifactId>
        <version>2.0.0</version>
    </parent>

    <artifactId>pedido-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.4.1</spring-boot.version>
    </properties>

    <!-- Importar BOM de Spring Boot -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- Sin versión - gestionada por BOM -->
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <!-- Con BOM, necesitas especificar versión y executions -->
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 2. Estructura Multi-Módulo

### Referencia Rápida

```
acme-platform/
├── pom.xml                          ← Parent POM
├── acme-common/                     ← Librería compartida
│   └── pom.xml
├── acme-pedido-service/             ← Microservicio
│   └── pom.xml
├── acme-cliente-service/            ← Microservicio
│   └── pom.xml
└── acme-gateway/                    ← API Gateway
    └── pom.xml
```

### Parent POM Completo

```xml
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
    <artifactId>acme-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>ACME Platform</name>
    <description>Plataforma de microservicios ACME</description>

    <!-- ==================== MÓDULOS ==================== -->
    <modules>
        <module>acme-common</module>
        <module>acme-pedido-service</module>
        <module>acme-cliente-service</module>
        <module>acme-gateway</module>
    </modules>

    <!-- ==================== PROPIEDADES ==================== -->
    <properties>
        <!-- Java -->
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Versiones de módulos internos -->
        <acme.version>1.0.0-SNAPSHOT</acme.version>

        <!-- Versiones de dependencias no gestionadas por Spring Boot -->
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <springdoc.version>2.3.0</springdoc.version>
        <problem-spring-web.version>0.29.1</problem-spring-web.version>
        <archunit.version>1.2.1</archunit.version>

        <!-- Versiones de plugins -->
        <jacoco.version>0.8.11</jacoco.version>
        <sonar.version>3.10.0.2594</sonar.version>
        <versions-maven-plugin.version>2.16.2</versions-maven-plugin.version>
        <maven-enforcer-plugin.version>3.4.1</maven-enforcer-plugin.version>

        <!-- Configuración de SonarQube -->
        <sonar.coverage.jacoco.xmlReportPaths>
            ${project.build.directory}/site/jacoco/jacoco.xml
        </sonar.coverage.jacoco.xmlReportPaths>
    </properties>

    <!-- ==================== GESTIÓN DE DEPENDENCIAS ==================== -->
    <dependencyManagement>
        <dependencies>
            <!-- Módulos internos -->
            <dependency>
                <groupId>com.acme</groupId>
                <artifactId>acme-common</artifactId>
                <version>${acme.version}</version>
            </dependency>

            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- OpenAPI / Swagger -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>

            <!-- Problem Details -->
            <dependency>
                <groupId>org.zalando</groupId>
                <artifactId>problem-spring-web</artifactId>
                <version>${problem-spring-web.version}</version>
            </dependency>

            <!-- ArchUnit para tests de arquitectura -->
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- ==================== DEPENDENCIAS COMUNES A TODOS LOS MÓDULOS ==================== -->
    <dependencies>
        <!-- Lombok (compile-time only) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing común -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- ==================== GESTIÓN DE PLUGINS ==================== -->
    <build>
        <pluginManagement>
            <plugins>
                <!-- Spring Boot Maven Plugin -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                        <layers>
                            <enabled>true</enabled>
                        </layers>
                    </configuration>
                </plugin>

                <!-- Compiler con MapStruct y Lombok -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <parameters>true</parameters>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactId>
                                <version>${lombok-mapstruct-binding.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                        <compilerArgs>
                            <arg>-Amapstruct.defaultComponentModel=spring</arg>
                            <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>

                <!-- Surefire para unit tests -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/*Test.java</include>
                        </includes>
                        <excludes>
                            <exclude>**/*IT.java</exclude>
                        </excludes>
                    </configuration>
                </plugin>

                <!-- Failsafe para integration tests -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/*IT.java</include>
                        </includes>
                    </configuration>
                    <executions>
                        <execution>
                            <goals>
                                <goal>integration-test</goal>
                                <goal>verify</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>

                <!-- JaCoCo para cobertura -->
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>${jacoco.version}</version>
                    <executions>
                        <execution>
                            <id>prepare-agent</id>
                            <goals>
                                <goal>prepare-agent</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>report</id>
                            <phase>test</phase>
                            <goals>
                                <goal>report</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>

                <!-- Enforcer -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-enforcer-plugin</artifactId>
                    <version>${maven-enforcer-plugin.version}</version>
                </plugin>

                <!-- Versions -->
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>versions-maven-plugin</artifactId>
                    <version>${versions-maven-plugin.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>

        <!-- Plugins ejecutados en todos los módulos -->
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <executions>
                    <execution>
                        <id>enforce-versions</id>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <requireMavenVersion>
                                    <version>[3.9.0,)</version>
                                </requireMavenVersion>
                                <requireJavaVersion>
                                    <version>[17,)</version>
                                </requireJavaVersion>
                                <dependencyConvergence/>
                                <banDuplicatePomDependencyVersions/>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <!-- ==================== PERFILES ==================== -->
    <profiles>
        <!-- Perfil para CI/CD con cobertura -->
        <profile>
            <id>ci</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.jacoco</groupId>
                        <artifactId>jacoco-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>

        <!-- Perfil para construir imagen Docker -->
        <profile>
            <id>docker</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>build-image</id>
                                <goals>
                                    <goal>build-image-no-fork</goal>
                                </goals>
                            </execution>
                        </executions>
                        <configuration>
                            <image>
                                <name>acme/${project.artifactId}:${project.version}</name>
                                <env>
                                    <BPE_DELIM_JAVA_TOOL_OPTIONS xml:space="preserve"> </BPE_DELIM_JAVA_TOOL_OPTIONS>
                                    <BPE_APPEND_JAVA_TOOL_OPTIONS>-XX:MaxDirectMemorySize=64M</BPE_APPEND_JAVA_TOOL_OPTIONS>
                                </env>
                            </image>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>

        <!-- Perfil para native image -->
        <profile>
            <id>native</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.graalvm.buildtools</groupId>
                        <artifactId>native-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>build-native</id>
                                <goals>
                                    <goal>compile-no-fork</goal>
                                </goals>
                                <phase>package</phase>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

    <!-- ==================== REPOSITORIOS ==================== -->
    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

### POM de Módulo Common (Librería)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.acme</groupId>
        <artifactId>acme-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>acme-common</artifactId>
    <packaging>jar</packaging>
    <name>ACME Common</name>
    <description>Componentes compartidos entre microservicios</description>

    <dependencies>
        <!-- Solo dependencias necesarias para la librería -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
    </dependencies>

    <!-- NO incluir spring-boot-maven-plugin en módulos librería -->
</project>
```

### POM de Microservicio

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.acme</groupId>
        <artifactId>acme-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>acme-pedido-service</artifactId>
    <packaging>jar</packaging>
    <name>ACME Pedido Service</name>

    <dependencies>
        <!-- Módulo común -->
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>acme-common</artifactId>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Base de datos -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>

        <!-- OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <!-- Métricas -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>rabbitmq</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Este módulo SÍ es ejecutable -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 3. Plugins Esenciales

### Referencia Rápida

| Plugin | Propósito | Comando |
|--------|-----------|---------|
| `spring-boot-maven-plugin` | JAR ejecutable, Docker | `mvn package` |
| `maven-compiler-plugin` | Compilación Java | Automático |
| `maven-surefire-plugin` | Unit tests | `mvn test` |
| `maven-failsafe-plugin` | Integration tests | `mvn verify` |
| `maven-enforcer-plugin` | Validar reglas | `mvn validate` |
| `versions-maven-plugin` | Gestión versiones | `mvn versions:display-dependency-updates` |
| `jacoco-maven-plugin` | Cobertura | `mvn test jacoco:report` |

### Guía Detallada de Plugins

#### maven-enforcer-plugin (Validación de reglas)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.4.1</version>
    <executions>
        <execution>
            <id>enforce-rules</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <!-- Versión mínima de Maven -->
                    <requireMavenVersion>
                        <version>[3.9.0,)</version>
                        <message>Se requiere Maven 3.9.0 o superior</message>
                    </requireMavenVersion>

                    <!-- Versión mínima de Java -->
                    <requireJavaVersion>
                        <version>[17,)</version>
                        <message>Se requiere Java 17 o superior</message>
                    </requireJavaVersion>

                    <!-- Prohibir SNAPSHOTs en releases -->
                    <requireReleaseDeps>
                        <onlyWhenRelease>true</onlyWhenRelease>
                        <message>No se permiten dependencias SNAPSHOT en releases</message>
                    </requireReleaseDeps>

                    <!-- Convergencia de dependencias -->
                    <dependencyConvergence/>

                    <!-- Prohibir versiones duplicadas -->
                    <banDuplicatePomDependencyVersions/>

                    <!-- Prohibir dependencias específicas -->
                    <bannedDependencies>
                        <excludes>
                            <exclude>commons-logging:commons-logging</exclude>
                            <exclude>log4j:log4j</exclude>
                            <exclude>org.apache.logging.log4j:log4j-core</exclude>
                        </excludes>
                        <message>Usar SLF4J + Logback en lugar de otras librerías de logging</message>
                    </bannedDependencies>
                </rules>
                <fail>true</fail>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### versions-maven-plugin (Gestión de versiones)

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <version>2.16.2</version>
    <configuration>
        <!-- Ignorar versiones alpha, beta, RC -->
        <rulesUri>file://${project.basedir}/../version-rules.xml</rulesUri>
    </configuration>
</plugin>
```

**version-rules.xml** (en raíz del proyecto):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://mojo.codehaus.org/versions-maven-plugin/rule/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://mojo.codehaus.org/versions-maven-plugin/rule/2.0.0
         https://mojo.codehaus.org/versions-maven-plugin/xsd/rule-2.0.0.xsd">
    <ignoreVersions>
        <ignoreVersion type="regex">.*-alpha.*</ignoreVersion>
        <ignoreVersion type="regex">.*-beta.*</ignoreVersion>
        <ignoreVersion type="regex">.*-RC.*</ignoreVersion>
        <ignoreVersion type="regex">.*-M\d+</ignoreVersion>
        <ignoreVersion type="regex">.*-SNAPSHOT</ignoreVersion>
    </ignoreVersions>
</ruleset>
```

**Comandos útiles**:
```bash
# Ver actualizaciones disponibles de dependencias
mvn versions:display-dependency-updates

# Ver actualizaciones disponibles de plugins
mvn versions:display-plugin-updates

# Ver actualizaciones de propiedades
mvn versions:display-property-updates

# Actualizar versiones de dependencias a las últimas
mvn versions:use-latest-releases

# Actualizar versión del proyecto
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT

# Revertir cambios de versión
mvn versions:revert

# Confirmar cambios de versión
mvn versions:commit
```

#### jacoco-maven-plugin (Cobertura de código)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
    <configuration>
        <excludes>
            <exclude>**/*Application.class</exclude>
            <exclude>**/*Config.class</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/entity/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

---

## 4. Gestión de Versiones de Dependencias

### Estrategia Recomendada

```xml
<properties>
    <!-- 1. Versiones gestionadas por Spring Boot BOM - NO sobreescribir -->
    <!-- spring.version, jackson.version, hibernate.version, etc. -->

    <!-- 2. Sobreescribir solo si es necesario (con precaución) -->
    <postgresql.version>42.7.1</postgresql.version>

    <!-- 3. Dependencias NO gestionadas por Spring Boot -->
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <springdoc.version>2.3.0</springdoc.version>
    <archunit.version>1.2.1</archunit.version>

    <!-- 4. Versiones de módulos internos -->
    <acme.version>${project.version}</acme.version>
</properties>
```

### Verificar qué gestiona Spring Boot

```bash
# Ver todas las dependencias y sus versiones
mvn dependency:tree

# Ver dependencias con versiones gestionadas
mvn help:effective-pom | grep -A2 "<dependency>"

# Ver propiedades del parent de Spring Boot
mvn help:effective-pom | grep "<.*version>"
```

### Sobreescribir versiones del BOM (con precaución)

```xml
<properties>
    <!-- Sobreescribir versión de PostgreSQL driver -->
    <postgresql.version>42.7.1</postgresql.version>

    <!-- Sobreescribir versión de Flyway -->
    <flyway.version>10.4.1</flyway.version>
</properties>
```

⚠️ **Precaución**: Sobreescribir versiones puede causar incompatibilidades. Hacerlo solo cuando sea necesario y probar exhaustivamente.

---

## 5. Maven Profiles

### Perfiles Comunes

```xml
<profiles>
    <!-- ==================== DESARROLLO LOCAL ==================== -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>dev</spring.profiles.active>
        </properties>
    </profile>

    <!-- ==================== CI/CD ==================== -->
    <profile>
        <id>ci</id>
        <properties>
            <spring.profiles.active>test</spring.profiles.active>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- ==================== PRODUCCIÓN ==================== -->
    <profile>
        <id>prod</id>
        <properties>
            <spring.profiles.active>prod</spring.profiles.active>
        </properties>
    </profile>

    <!-- ==================== DOCKER BUILD ==================== -->
    <profile>
        <id>docker</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>build-image</id>
                            <phase>package</phase>
                            <goals>
                                <goal>build-image-no-fork</goal>
                            </goals>
                        </execution>
                    </executions>
                    <configuration>
                        <image>
                            <name>${docker.registry}/${project.artifactId}:${project.version}</name>
                            <publish>true</publish>
                        </image>
                        <docker>
                            <publishRegistry>
                                <username>${docker.username}</username>
                                <password>${docker.password}</password>
                            </publishRegistry>
                        </docker>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- ==================== ANÁLISIS SONAR ==================== -->
    <profile>
        <id>sonar</id>
        <properties>
            <sonar.host.url>${env.SONAR_HOST_URL}</sonar.host.url>
            <sonar.token>${env.SONAR_TOKEN}</sonar.token>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.sonarsource.scanner.maven</groupId>
                    <artifactId>sonar-maven-plugin</artifactId>
                    <version>3.10.0.2594</version>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- ==================== SKIP TESTS ==================== -->
    <profile>
        <id>skip-tests</id>
        <properties>
            <maven.test.skip>true</maven.test.skip>
        </properties>
    </profile>

    <!-- ==================== NATIVE IMAGE ==================== -->
    <profile>
        <id>native</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.graalvm.buildtools</groupId>
                    <artifactId>native-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>build-native</id>
                            <goals>
                                <goal>compile-no-fork</goal>
                            </goals>
                            <phase>package</phase>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### Uso de Perfiles

```bash
# Activar perfil específico
mvn clean package -Pci

# Múltiples perfiles
mvn clean package -Pci,docker

# Ver perfiles activos
mvn help:active-profiles

# Build para producción con Docker
mvn clean package -Pprod,docker -DskipTests
```

---

## 6. Comandos Maven Frecuentes

```bash
# ==================== BUILD ====================
mvn clean install                    # Build completo con tests
mvn clean package -DskipTests        # Build sin tests
mvn clean verify                     # Build con integration tests

# ==================== TESTING ====================
mvn test                             # Solo unit tests
mvn verify                           # Unit + integration tests
mvn test -Dtest=ClienteServiceTest   # Test específico
mvn failsafe:integration-test        # Solo integration tests

# ==================== DEPENDENCIAS ====================
mvn dependency:tree                  # Árbol de dependencias
mvn dependency:analyze               # Detectar dependencias no usadas
mvn versions:display-dependency-updates  # Ver actualizaciones

# ==================== SPRING BOOT ====================
mvn spring-boot:run                  # Ejecutar aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Con perfil
mvn spring-boot:build-image          # Construir imagen Docker

# ==================== MULTI-MÓDULO ====================
mvn clean install -pl acme-common    # Build solo un módulo
mvn clean install -pl acme-pedido-service -am  # Módulo + dependencias
mvn clean install -rf :acme-pedido-service     # Desde módulo específico

# ==================== CALIDAD ====================
mvn jacoco:report                    # Generar reporte cobertura
mvn sonar:sonar -Psonar              # Análisis SonarQube
mvn enforcer:enforce                 # Validar reglas
```

---

## Checklist de Maven

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Parent | `spring-boot-starter-parent` | Sin parent o BOM incorrecto |
| Versiones | Centralizadas en `<properties>` | Hardcodeadas en dependencias |
| Multi-módulo | `<dependencyManagement>` en parent | Versiones duplicadas |
| Módulo common | Sin `spring-boot-maven-plugin` | Con plugin de repackage |
| Módulo service | Con `spring-boot-maven-plugin` | Sin plugin |
| Tests | Surefire + Failsafe | Todo en Surefire |
| Enforcer | Validar Java/Maven mínimo | Sin validación |
| CI | Perfil con JaCoCo | Sin cobertura |

---

## Próximos Pasos

Este documento cubre el **Área 7: Dependencias Maven y Gestión**.

Cuando estés listo, continuamos con el **Área 8: APIs REST Best Practices** que incluirá:
- Versionado de APIs
- HTTP status codes
- Request/Response DTOs
- Validación
- Paginación y sorting

---

*Documento generado con Context7 - Spring Boot 3.4.x, Maven 3.9+*
