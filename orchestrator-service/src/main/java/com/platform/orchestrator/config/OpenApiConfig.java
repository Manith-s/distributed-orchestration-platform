package com.platform.orchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI orchestratorOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Job Orchestration Platform API")
                .version("1.0.0")
                .description("Distributed job orchestration and execution platform")
                .contact(new Contact()
                    .name("Platform Team")
                    .email("support@platform.com")));
    }
}
