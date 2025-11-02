package com.platform.query.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI queryOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Query Service API")
                .version("1.0.0")
                .description("Log search and metrics query API")
                .contact(new Contact()
                    .name("Platform Team")
                    .email("support@platform.com")));
    }
}

