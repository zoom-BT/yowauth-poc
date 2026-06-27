package yowyob.comops.api.kernel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IWM Backend API")
                        .version("0.1.0-SNAPSHOT")
                        .description("Documentation for the modular monolith hexagonal backend APIs."))
                .components(new Components()
                        .addSecuritySchemes("ClientId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Client-Id")
                                .description("Client application identifier for server-to-server access"))
                        .addSecuritySchemes("ApiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key")
                                .description("Client application secret for server-to-server access"))
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token returned by /api/auth/login")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("ClientId")
                        .addList("ApiKey")
                        .addList("BearerAuth"));
    }
}
