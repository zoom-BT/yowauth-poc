package yowyob.comops.api.kernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.adapter.in.web.ClientApplicationServiceEntitlementWebFilter;
import yowyob.comops.api.kernel.adapter.in.web.OrganizationServiceEntitlementWebFilter;
import yowyob.comops.api.kernel.adapter.in.web.PlatformServiceRouteResolver;
import yowyob.comops.api.kernel.application.port.in.AuthenticateClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlementDirectory;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionResolver;
import java.nio.charset.StandardCharsets;
import java.util.List;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class KernelSecurityConfiguration {

    @Bean
    @Order(0)
    SecurityWebFilterChain managementSecurityWebFilterChain(ServerHttpSecurity http,
            ManagementSecurityProperties managementSecurityProperties,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper) {
        ReactiveAuthenticationManager managementApiKeyAuthenticationManager = managementApiKeyAuthenticationManager(
                managementSecurityProperties);
        AuthenticationWebFilter managementApiKeyFilter = new AuthenticationWebFilter(
                managementApiKeyAuthenticationManager);
        managementApiKeyFilter.setServerAuthenticationConverter(new ManagementApiKeyAuthenticationConverter());
        managementApiKeyFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"));
        managementApiKeyFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> writeFailure(exchange, objectMapper,
                                HttpStatus.UNAUTHORIZED, ex.getMessage(), "UNAUTHORIZED"))
                        .accessDeniedHandler((exchange, ex) -> writeFailure(exchange, objectMapper,
                                HttpStatus.FORBIDDEN, ex.getMessage(), "FORBIDDEN")))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/actuator/**").hasAuthority("management:read")
                        .anyExchange().denyAll())
                .addFilterAt(managementApiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    @Order(1)
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            SecurityRuntimeProperties securityRuntimeProperties,
            AuthenticateClientApplicationUseCase authenticateClientApplicationUseCase,
            OrganizationServiceRuntimeEntitlementDirectory organizationServiceRuntimeEntitlementDirectory,
            ReactivePermissionResolver permissionResolver,
            UserSessionTokenService userSessionTokenService,
            ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
            OrganizationServiceRequestQuotaProperties organizationServiceRequestQuotaProperties,
            CorsConfigurationSource corsConfigurationSource,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        ReactiveAuthenticationManager apiKeyAuthenticationManager = apiKeyAuthenticationManager(
                securityRuntimeProperties, authenticateClientApplicationUseCase, permissionResolver);
        AuthenticationWebFilter apiKeyFilter = new AuthenticationWebFilter(apiKeyAuthenticationManager);
        apiKeyFilter.setServerAuthenticationConverter(new ApiKeyServerAuthenticationConverter(userSessionTokenService));
        apiKeyFilter.setRequiresAuthenticationMatcher(apiAuthenticationMatcher());
        apiKeyFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        PlatformServiceRouteResolver routeResolver = new PlatformServiceRouteResolver();
        ClientApplicationServiceEntitlementWebFilter clientApplicationServiceEntitlementWebFilter =
                new ClientApplicationServiceEntitlementWebFilter(routeResolver, objectMapper);
        OrganizationServiceEntitlementWebFilter organizationServiceEntitlementWebFilter =
                new OrganizationServiceEntitlementWebFilter(organizationServiceRuntimeEntitlementDirectory, routeResolver,
                        redisTemplateProvider.getIfAvailable(), organizationServiceRequestQuotaProperties,
                        objectMapper, meterRegistry);

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> writeFailure(exchange, objectMapper,
                                HttpStatus.UNAUTHORIZED, ex.getMessage(), "UNAUTHORIZED"))
                        .accessDeniedHandler((exchange, ex) -> writeFailure(exchange, objectMapper,
                                HttpStatus.FORBIDDEN, ex.getMessage(), "FORBIDDEN")))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**")
                        .permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(apiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(clientApplicationServiceEntitlementWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(organizationServiceEntitlementWebFilter, SecurityWebFiltersOrder.AUTHORIZATION)
                .build();
    }

    private ReactiveAuthenticationManager apiKeyAuthenticationManager(
            SecurityRuntimeProperties securityRuntimeProperties,
            AuthenticateClientApplicationUseCase authenticateClientApplicationUseCase,
            ReactivePermissionResolver permissionResolver) {
        requireValidTokenMechanism(securityRuntimeProperties);
        return new ApiKeyReactiveAuthenticationManager(authenticateClientApplicationUseCase, permissionResolver);
    }

    private ReactiveAuthenticationManager managementApiKeyAuthenticationManager(
            ManagementSecurityProperties managementSecurityProperties) {
        String apiKey = requireValidManagementApiKey(managementSecurityProperties);
        return new ManagementApiKeyReactiveAuthenticationManager(apiKey);
    }

    private void requireValidTokenMechanism(SecurityRuntimeProperties props) {
        if (!props.isJwtConfigured()) {
            throw new IllegalStateException(
                    "JWT signing is required. Configure 'iwm.security.jwt.private-key-path' or "
                            + "'iwm.security.jwt.auto-generate-key-pair=true'.");
        }
    }

    private ServerWebExchangeMatcher apiAuthenticationMatcher() {
        return exchange -> {
            // CORS preflight requests carry no credentials by design: never try to authenticate them.
            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }
            String path = exchange.getRequest().getPath().pathWithinApplication().value();
            return path.startsWith("/api/")
                    ? ServerWebExchangeMatcher.MatchResult.match()
                    : ServerWebExchangeMatcher.MatchResult.notMatch();
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${iwm.security.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
            List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static final java.util.Set<String> FORBIDDEN_MANAGEMENT_API_KEYS = java.util.Set.of(
            "change-me",
            "changeme",
            "dev-management-api-key",
            "dev-management-key",
            "default",
            "admin",
            "password",
            "secret");

    private String requireValidManagementApiKey(ManagementSecurityProperties managementSecurityProperties) {
        String apiKey = managementSecurityProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "iwm.management.security.api-key must be configured via IWM_MANAGEMENT_API_KEY.");
        }
        String normalized = apiKey.trim().toLowerCase(java.util.Locale.ROOT);
        if (FORBIDDEN_MANAGEMENT_API_KEYS.contains(normalized)) {
            throw new IllegalStateException(
                    "iwm.management.security.api-key uses a forbidden default value. "
                            + "Provide a strong secret via IWM_MANAGEMENT_API_KEY.");
        }
        if (apiKey.length() < 16) {
            throw new IllegalStateException(
                    "iwm.management.security.api-key must be at least 16 characters long.");
        }
        return apiKey;
    }

    private reactor.core.publisher.Mono<Void> writeFailure(
            org.springframework.web.server.ServerWebExchange exchange,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message,
            String errorCode) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(ApiResponse.failure(message, errorCode));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            payload = ("{\"success\":false,\"message\":\"" + sanitize(message)
                    + "\",\"errorCode\":\"" + errorCode + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }

    private String sanitize(String value) {
        if (value == null) {
            return "Request denied.";
        }
        return value.replace("\"", "'");
    }
}
