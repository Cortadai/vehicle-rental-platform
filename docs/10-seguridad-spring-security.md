# Área 10: Seguridad Básica - Spring Security 6.x

## Referencia Rápida (Senior Developers)

### Dependencias Esenciales

```xml
<!-- Resource Server con JWT -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Spring Security (incluido en el anterior, pero explícito si solo necesitas security) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Configuración Mínima OAuth2 Resource Server

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/acme
          # O directamente el JWK Set URI:
          # jwk-set-uri: https://keycloak.acme.com/realms/acme/protocol/openid-connect/certs
```

### SecurityFilterChain Básico

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // APIs REST stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
            );
        return http.build();
    }
}
```

### CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.acme.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
    config.setExposedHeaders(List.of("X-Request-ID"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

### Method Security

```java
@Service
public class OrderService {
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwner(#orderId)")
    public Order getOrder(Long orderId) { }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void deleteOrder(Long orderId) { }

    @PreAuthorize("authentication.name == #username")
    public List<Order> getMyOrders(String username) { }
}
```

---

## Guía Detallada (Junior/Mid Developers)

### 1. Conceptos Fundamentales de Spring Security 6.x

#### Cambios Importantes en Spring Security 6.x

| Aspecto | Spring Security 5.x | Spring Security 6.x |
|---------|---------------------|---------------------|
| Configuración | `WebSecurityConfigurerAdapter` | `SecurityFilterChain` Bean |
| Autorización | `authorizeRequests()` | `authorizeHttpRequests()` |
| Method Security | `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity` |
| CSRF | Habilitado por defecto | Habilitado por defecto |
| Namespace Jakarta | `javax.servlet` | `jakarta.servlet` |

#### Arquitectura de Seguridad

```
Request → Security Filter Chain → Controller
              ↓
    ┌─────────────────────────────────────────┐
    │  CorsFilter                              │
    │  CsrfFilter                              │
    │  BearerTokenAuthenticationFilter (JWT)   │
    │  ExceptionTranslationFilter              │
    │  AuthorizationFilter                     │
    └─────────────────────────────────────────┘
```

### 2. Configuración Completa de Seguridad

#### Estructura de Paquetes

```
com.acme.sales.infrastructure.security/
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── JwtConfig.java
├── jwt/
│   ├── JwtAuthenticationConverter.java
│   └── KeycloakRoleConverter.java
├── authorization/
│   ├── ResourceOwnerAuthorizationService.java
│   └── CustomPermissionEvaluator.java
└── audit/
    └── SecurityAuditListener.java
```

#### SecurityConfig Completo

```java
package com.acme.sales.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    /**
     * Security Filter Chain para la API principal.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Aplicar solo a rutas /api/**
            .securityMatcher("/api/**")

            // CORS - debe ir primero
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // CSRF deshabilitado para APIs REST stateless
            .csrf(csrf -> csrf.disable())

            // Sin sesiones - completamente stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Deshabilitar form login y http basic
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Reglas de autorización
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                // Endpoints de administración
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAnyRole("ADMIN", "MANAGER")

                // Endpoints específicos por rol
                .requestMatchers("/api/v1/reports/**").hasAuthority("SCOPE_reports:read")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasAuthority("SCOPE_orders:write")

                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )

            // OAuth2 Resource Server con JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                // Manejo de errores de autenticación
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Security Filter Chain para Actuator (separado).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Health y readiness públicos (para Kubernetes)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                // Prometheus métricas (acceso interno)
                .requestMatchers("/actuator/prometheus").hasIpAddress("10.0.0.0/8")
                // Otros endpoints de actuator requieren ADMIN
                .anyRequest().hasRole("ADMIN")
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Registrar como beans para inyectar ObjectMapper
    @Bean
    public CustomAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public CustomAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new CustomAccessDeniedHandler(objectMapper);
    }
}
```

### 3. OAuth2 Resource Server con Keycloak

#### Configuración de Propiedades

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Opción 1: Issuer URI (recomendado - auto-descubrimiento)
          issuer-uri: ${KEYCLOAK_ISSUER_URI:https://keycloak.acme.com/realms/acme}

          # Opción 2: JWK Set URI directo
          # jwk-set-uri: ${KEYCLOAK_JWKS_URI:https://keycloak.acme.com/realms/acme/protocol/openid-connect/certs}

          # Validaciones adicionales
          audiences: acme-api  # Validar audience claim

# Configuración custom para mapeo de roles
app:
  security:
    jwt:
      roles-claim: realm_access.roles      # Claim de Keycloak para roles
      resource-claim: resource_access      # Claim para roles de recursos
      client-id: acme-sales-api            # Client ID para roles específicos
```

#### Converter de JWT para Keycloak

```java
package com.acme.sales.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class JwtConfig {

    @Value("${app.security.jwt.client-id:}")
    private String clientId;

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Usar nombre de usuario del token
        converter.setPrincipalClaimName("preferred_username");

        // Converter personalizado para roles de Keycloak
        converter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter());

        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> keycloakRoleConverter() {
        return jwt -> {
            // Extraer scopes estándar (SCOPE_xxx)
            JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);

            // Extraer roles del realm (realm_access.roles)
            Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

            // Extraer roles del cliente específico (resource_access.{client-id}.roles)
            Collection<GrantedAuthority> clientRoles = extractClientRoles(jwt);

            // Combinar todas las authorities
            return Stream.of(scopeAuthorities, realmRoles, clientRoles)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        };
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        if (clientId == null || clientId.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Collections.emptyList();
        }

        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        if (clientAccess == null) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) clientAccess.get("roles");
        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
```

#### Ejemplo de JWT de Keycloak

```json
{
  "exp": 1705320000,
  "iat": 1705316400,
  "jti": "abc-123-def",
  "iss": "https://keycloak.acme.com/realms/acme",
  "aud": "acme-api",
  "sub": "user-uuid-123",
  "typ": "Bearer",
  "azp": "acme-sales-api",
  "preferred_username": "john.doe",
  "email": "john.doe@acme.com",
  "name": "John Doe",
  "realm_access": {
    "roles": ["user", "manager"]
  },
  "resource_access": {
    "acme-sales-api": {
      "roles": ["orders_write", "reports_read"]
    }
  },
  "scope": "openid profile email orders:read orders:write"
}
```

### 4. CORS Configuration Detallada

```java
package com.acme.sales.infrastructure.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.cors.max-age:3600}")
    private Long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration apiConfig = new CorsConfiguration();

        // Orígenes permitidos (NO usar "*" si allowCredentials=true)
        apiConfig.setAllowedOrigins(allowedOrigins);

        // Métodos HTTP permitidos
        apiConfig.setAllowedMethods(allowedMethods);

        // Headers permitidos en la request
        apiConfig.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Request-ID",
            "X-Correlation-ID"
        ));

        // Headers expuestos en la response (accesibles desde JavaScript)
        apiConfig.setExposedHeaders(List.of(
            "X-Request-ID",
            "X-Correlation-ID",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        ));

        // Permitir cookies/auth headers
        apiConfig.setAllowCredentials(true);

        // Tiempo de cache para preflight requests
        apiConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", apiConfig);

        return source;
    }
}
```

```yaml
# application.yml
app:
  cors:
    allowed-origins:
      - https://app.acme.com
      - https://admin.acme.com
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - PATCH
      - OPTIONS
    max-age: 3600

# Por ambiente
---
spring:
  config:
    activate:
      on-profile: dev
app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:5173
```

### 5. Method Security

#### Habilitar Method Security

```java
@Configuration
@EnableMethodSecurity(
    prePostEnabled = true,   // @PreAuthorize, @PostAuthorize
    securedEnabled = true,   // @Secured
    jsr250Enabled = true     // @RolesAllowed (JSR-250)
)
public class MethodSecurityConfig {
}
```

#### Uso de @PreAuthorize

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AuthorizationService authz;

    /**
     * Solo usuarios autenticados.
     */
    @PreAuthorize("isAuthenticated()")
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /**
     * Solo rol ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAll() {
        orderRepository.deleteAll();
    }

    /**
     * ADMIN o MANAGER.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderStats getStatistics() {
        return orderRepository.calculateStats();
    }

    /**
     * Verificar authority específica (scope del JWT).
     */
    @PreAuthorize("hasAuthority('SCOPE_orders:write')")
    public Order create(CreateOrderRequest request) {
        return orderRepository.save(orderMapper.toEntity(request));
    }

    /**
     * Acceso por ownership: solo el propietario o admin.
     */
    @PreAuthorize("hasRole('ADMIN') or @authz.isOrderOwner(#orderId, authentication)")
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Verificar que el usuario accede a sus propios datos.
     */
    @PreAuthorize("#username == authentication.name")
    public List<Order> findByUsername(String username) {
        return orderRepository.findByCustomerUsername(username);
    }

    /**
     * Expresión compleja con acceso a claims del JWT.
     */
    @PreAuthorize("authentication.principal.claims['department'] == 'SALES' or hasRole('ADMIN')")
    public List<Order> findSalesOrders() {
        return orderRepository.findBySalesDepartment();
    }

    /**
     * Post-authorize: verificar después de obtener el resultado.
     */
    @PostAuthorize("returnObject.customerId == authentication.name or hasRole('ADMIN')")
    public Order findOrderWithPostCheck(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }
}
```

#### Authorization Service Custom

```java
package com.acme.sales.infrastructure.security.authorization;

import com.acme.sales.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("authz")
@RequiredArgsConstructor
public class AuthorizationService {

    private final OrderRepository orderRepository;

    /**
     * Verifica si el usuario autenticado es propietario de la orden.
     */
    public boolean isOrderOwner(Long orderId, Authentication authentication) {
        String username = authentication.getName();
        return orderRepository.findById(orderId)
                .map(order -> order.getCustomerUsername().equals(username))
                .orElse(false);
    }

    /**
     * Verifica si el usuario pertenece a un tenant específico.
     */
    public boolean belongsToTenant(String tenantId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userTenant = jwt.getClaimAsString("tenant_id");
            return tenantId.equals(userTenant);
        }
        return false;
    }

    /**
     * Verifica nivel de acceso jerárquico.
     */
    public boolean hasMinimumLevel(int requiredLevel, Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Integer userLevel = jwt.getClaim("access_level");
            return userLevel != null && userLevel >= requiredLevel;
        }
        return false;
    }
}
```

### 6. Manejo de Errores de Seguridad

```java
package com.acme.sales.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("Authentication failed for request {}: {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> problem = Map.of(
            "type", "https://api.acme.com/errors/unauthorized",
            "title", "Unauthorized",
            "status", 401,
            "detail", "Authentication is required to access this resource",
            "instance", request.getRequestURI(),
            "timestamp", Instant.now().toString()
        );

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}

@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("Access denied for request {}: {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> problem = Map.of(
            "type", "https://api.acme.com/errors/forbidden",
            "title", "Forbidden",
            "status", 403,
            "detail", "You don't have permission to access this resource",
            "instance", request.getRequestURI(),
            "timestamp", Instant.now().toString()
        );

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
```

### 7. Auditoría de Seguridad

```java
package com.acme.sales.infrastructure.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityAuditListener {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("Authentication successful for user: {}", username);

        // Registrar en sistema de auditoría
        // auditService.recordLogin(username, SUCCESS);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();

        log.warn("Authentication failed for user: {} - Reason: {}", username, reason);

        // Registrar intento fallido
        // auditService.recordLogin(username, FAILURE, reason);
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent event) {
        log.warn("Authorization denied: {} for {}",
            event.getAuthorizationDecision(),
            event.getAuthentication().get().getName()
        );
    }
}
```

### 8. Obtener Usuario Actual

```java
package com.acme.sales.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    /**
     * Obtiene el username del usuario autenticado.
     */
    public Optional<String> getCurrentUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName);
    }

    /**
     * Obtiene el JWT del usuario autenticado.
     */
    public Optional<Jwt> getCurrentJwt() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

    /**
     * Obtiene un claim específico del JWT.
     */
    public <T> Optional<T> getJwtClaim(String claim, Class<T> type) {
        return getCurrentJwt()
                .map(jwt -> jwt.getClaim(claim))
                .filter(type::isInstance)
                .map(type::cast);
    }

    /**
     * Verifica si el usuario tiene un rol específico.
     */
    public boolean hasRole(String role) {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)))
                .orElse(false);
    }

    /**
     * Obtiene el email del usuario actual.
     */
    public Optional<String> getCurrentUserEmail() {
        return getJwtClaim("email", String.class);
    }

    /**
     * Obtiene el tenant ID del usuario actual (multi-tenant).
     */
    public Optional<String> getCurrentTenantId() {
        return getJwtClaim("tenant_id", String.class);
    }
}
```

#### Uso en Controllers

```java
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final SecurityUtils securityUtils;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUser() {
        String username = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        return ResponseEntity.ok(userService.getProfile(username));
    }

    @GetMapping("/me/jwt-info")
    public ResponseEntity<Map<String, Object>> getJwtInfo() {
        Jwt jwt = securityUtils.getCurrentJwt()
                .orElseThrow(() -> new UnauthorizedException("No JWT found"));

        return ResponseEntity.ok(Map.of(
            "subject", jwt.getSubject(),
            "issuer", jwt.getIssuer().toString(),
            "expiresAt", jwt.getExpiresAt().toString(),
            "claims", jwt.getClaims()
        ));
    }
}
```

### 9. Testing de Seguridad

#### Dependencias de Test

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Tests con @WithMockUser

```java
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean  // @MockitoBean reemplaza a @MockBean (deprecated desde Spring Boot 3.4)
    // import: org.springframework.test.context.bean.override.mockito.MockitoBean
    private OrderService orderService;

    @Test
    void whenUnauthenticated_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void whenAuthenticatedUser_thenOk() throws Exception {
        when(orderService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void whenUserTriesAdminEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAdmin_thenCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders/1"))
                .andExpect(status().isNoContent());
    }
}
```

#### Tests con JWT Mock

```java
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerJwtTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean  // @MockitoBean reemplaza a @MockBean (deprecated desde Spring Boot 3.4)
    // import: org.springframework.test.context.bean.override.mockito.MockitoBean
    private OrderService orderService;

    @Test
    void whenValidJwt_thenOk() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .with(jwt()
                    .jwt(builder -> builder
                        .subject("user-123")
                        .claim("preferred_username", "john.doe")
                        .claim("realm_access", Map.of("roles", List.of("user")))
                    )
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                ))
                .andExpect(status().isOk());
    }

    @Test
    void whenJwtWithScopes_thenCanAccessScopedEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\": 1}")
                .with(jwt()
                    .jwt(builder -> builder.claim("scope", "orders:write"))
                    .authorities(
                        new SimpleGrantedAuthority("SCOPE_orders:write"),
                        new SimpleGrantedAuthority("ROLE_USER")
                    )
                ))
                .andExpect(status().isCreated());
    }
}
```

#### Test de Method Security

```java
@SpringBootTest
class OrderServiceSecurityTest {

    @Autowired
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteOrders() {
        assertDoesNotThrow(() -> orderService.deleteOrder(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotDeleteOrders() {
        assertThrows(AccessDeniedException.class,
            () -> orderService.deleteOrder(1L));
    }

    @Test
    @WithMockUser(username = "john.doe")
    void userCanAccessOwnOrders() {
        // El usuario "john.doe" puede acceder a sus propias órdenes
        assertDoesNotThrow(() -> orderService.findByUsername("john.doe"));
    }

    @Test
    @WithMockUser(username = "john.doe")
    void userCannotAccessOthersOrders() {
        // El usuario "john.doe" NO puede acceder a órdenes de "jane.doe"
        assertThrows(AccessDeniedException.class,
            () -> orderService.findByUsername("jane.doe"));
    }
}
```

---

## ✅ Hacer

1. **Usar OAuth2 Resource Server** - Para APIs, no form login
2. **JWT stateless** - Sin sesiones, `SessionCreationPolicy.STATELESS`
3. **Separar actuator** - Security chain diferente para endpoints de gestión
4. **CORS específico** - No usar `*` con `allowCredentials=true`
5. **Method Security** - `@PreAuthorize` para lógica de autorización compleja
6. **Custom authentication converter** - Mapear claims de Keycloak a roles Spring
7. **Auditoría de seguridad** - Registrar logins exitosos y fallidos
8. **Tests de seguridad** - `@WithMockUser`, `jwt()` para test de endpoints
9. **Error handlers custom** - Respuestas RFC 7807 para 401/403
10. **SecurityUtils** - Clase helper para obtener usuario actual

## ❌ Evitar

1. **`permitAll()` excesivo** - Solo para endpoints realmente públicos
2. **Hardcodear secrets** - Usar environment variables o Secret Manager
3. **CSRF en APIs stateless** - Deshabilitar si no hay sesiones
4. **Exponer stack traces** - En errores de seguridad
5. **Roles en código** - Preferir authorities/scopes configurables
6. **Ignorar preflight** - CORS debe procesarse antes de Spring Security
7. **`@Secured` sobre `@PreAuthorize`** - PreAuthorize es más flexible
8. **Validar JWT manualmente** - Dejar que Spring Security lo maneje
9. **Confiar solo en frontend** - Siempre validar en backend
10. **Actuator público** - Proteger endpoints sensibles

---

## Checklist de Implementación

### Configuración Base
- [ ] `spring-boot-starter-oauth2-resource-server` como dependencia
- [ ] `SecurityFilterChain` configurado (no `WebSecurityConfigurerAdapter`)
- [ ] `SessionCreationPolicy.STATELESS` para APIs
- [ ] CSRF deshabilitado para APIs stateless
- [ ] `@EnableMethodSecurity` habilitado

### OAuth2/JWT
- [ ] `issuer-uri` o `jwk-set-uri` configurado
- [ ] JWT claims mapeados a authorities
- [ ] Roles de Keycloak extraídos correctamente
- [ ] Validación de audience si es necesario

### CORS
- [ ] `CorsConfigurationSource` bean definido
- [ ] Origins específicos (no `*` con credentials)
- [ ] Headers permitidos y expuestos configurados
- [ ] `maxAge` configurado para cache de preflight

### Autorización
- [ ] Rutas públicas con `permitAll()`
- [ ] Rutas de admin protegidas
- [ ] Method security con `@PreAuthorize`
- [ ] Authorization service para lógica custom

### Manejo de Errores
- [ ] `AuthenticationEntryPoint` custom para 401
- [ ] `AccessDeniedHandler` custom para 403
- [ ] Respuestas en formato Problem Details

### Testing
- [ ] Tests con `@WithMockUser`
- [ ] Tests con JWT mock (`jwt()`)
- [ ] Tests de method security
- [ ] Tests de endpoints públicos y protegidos
