package com.blog.config;

import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化数据：应用启动时若用户表为空，则创建默认管理员
 *
 * <p>为什么用代码初始化而不是写在 SQL 里？
 * 因为密码需要 BCrypt 加密，而 BCrypt 每次加盐结果都不同，
 * 用代码调用 PasswordEncoder 生成最可靠，也方便以后修改默认账号。
 *
 * <p>默认账号：admin / 123456（首次登录后请尽快修改）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        // 把明文密码加密后再存库
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setNickname("站长");
        admin.setEmail("admin@blog.com");
        userMapper.insert(admin);
        log.info("已创建默认管理员账号：admin / 123456，请登录后尽快修改密码");
    }
}
