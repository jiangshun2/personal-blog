package com.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（Knife4j / OpenAPI3）
 *
 * <p>启动后访问 http://localhost:8080/doc.html 即可查看并在线调试接口。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人博客系统 API")
                        .description("Spring Boot 3 + MyBatis-Plus + Redis + Vue3 博客系统接口文档")
                        .version("v1.0"));
    }
}
