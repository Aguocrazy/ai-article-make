package com.aiarticle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 接口文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Article Make API")
                        .description("AI Article Make - 智能文章生成项目接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Article Make Team")
                                .email("dev@aiarticle.com")));
    }
}