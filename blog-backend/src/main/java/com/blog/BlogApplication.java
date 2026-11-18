package com.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 博客系统启动类
 *
 * <p>两个注解的作用：
 * <ul>
 *   <li>{@code @SpringBootApplication}：开启自动配置，并扫描 com.blog 包下的组件</li>
 *   <li>{@code @MapperScan}：扫描 MyBatis-Plus 的 Mapper 接口，省去每个 Mapper 都加 @Mapper</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.blog.mapper")
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
