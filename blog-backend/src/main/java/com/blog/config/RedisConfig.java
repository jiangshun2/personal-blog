package com.blog.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 *
 * <p>为什么要自定义 RedisTemplate？
 * Spring Boot 默认的 RedisTemplate 使用 JDK 序列化，存进 Redis 的内容是一堆二进制乱码，
 * 既不可读也容易因类版本变化导致反序列化失败。这里改成：
 * <ul>
 *   <li>key / hashKey：StringRedisSerializer，Redis 里直接看到 blog:article:1 这样的可读键</li>
 *   <li>value / hashValue：GenericJackson2JsonRedisSerializer，JSON 存储并带 {@code @class} 类型信息，
 *       反序列化时能还原成原来的 VO / PageResult 对象</li>
 * </ul>
 * 另外注册 JavaTimeModule，让 LocalDateTime 类型的 VO 字段也能正常序列化。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key 用字符串序列化，保证 Redis 中键名可读
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // value 用 JSON 序列化（带类型信息）
        GenericJackson2JsonRedisSerializer valueSerializer = buildValueSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 构建 JSON 序列化器
     *
     * <p>要点：
     * <ul>
     *   <li>注册 JavaTimeModule：支持 LocalDateTime（VO 里的 createTime / updateTime）</li>
     *   <li>关闭时间戳输出，让时间以字符串保存，便于观察</li>
     *   <li>开启 default typing：序列化时写入 @class，反序列化才能还原具体类型
     *       （缓存里有 PageResult、VO 等多种对象，必须带类型信息）</li>
     * </ul>
     */
    private GenericJackson2JsonRedisSerializer buildValueSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
