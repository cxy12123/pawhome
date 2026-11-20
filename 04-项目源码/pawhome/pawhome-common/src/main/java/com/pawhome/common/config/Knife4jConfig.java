package com.pawhome.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（OpenAPI 3）配置。
 * <p>
 * 每个服务都会暴露一份自己的文档，访问 http://localhost:端口/doc.html。
 * 调试时先在 auth 服务登录拿 token，点右上角"Authorize"填进去，
 * 后续所有接口就会自动带上 Authorization 头。
 */
@Configuration
public class Knife4jConfig {

    @Value("${spring.application.name:pawhome-service}")
    private String applicationName;

    @Bean
    public OpenAPI pawhomeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " 接口文档")
                        .description("PawHome 城市流浪动物救助站领养与物资管理平台 - 微服务接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("PawHome 项目组")))
                .components(new Components()
                        .addSecuritySchemes("Authorization", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }
}
