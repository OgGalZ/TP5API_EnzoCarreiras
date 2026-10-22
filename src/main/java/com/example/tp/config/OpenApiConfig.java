package com.example.tp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

// Bonus Swagger : https://localhost:8443/swagger-ui.html
// Dans "Authorize" : bearerAuth = le token seul, apiKey = "ApiKey 123456abcdef"
@Configuration
@OpenAPIDefinition(info = @Info(title = "TP5 API securisee", version = "1.0"))
@SecuritySchemes({
        @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT"),
        @SecurityScheme(name = "apiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
                paramName = "Authorization")
})
public class OpenApiConfig {
}
