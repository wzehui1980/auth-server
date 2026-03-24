package com.example.authserver.config;

import com.example.authserver.entity.Role;
import com.example.authserver.entity.User;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * 数据初始化配置
 * 仅初始化用户表（角色和 URL 权限规则由 schema.sql初始化）
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataInitializerConfig {

    private final PasswordEncoder passwordEncoder;

    /**
     * 使用 ApplicationRunner 在项目启动后初始化默认用户数据
     */
    @Bean
    public ApplicationRunner userInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository) {
        return args -> {
            log.info("开始初始化用户数据...");
            fixRoleDescriptions(roleRepository);
            initializeUsers(userRepository, roleRepository);
            log.info("用户数据初始化完成");
        };
    }

    /**
     * 修复角色描述的中文乱码问题
     */
    private void fixRoleDescriptions(RoleRepository roleRepository) {
        try {
            // 修复 ROLE_USER 描述
            roleRepository.findByName("ROLE_USER").ifPresent(role -> {
                if (role.getDescription() == null || !role.getDescription().equals("普通用户")) {
                    role.setDescription("普通用户");
                    roleRepository.save(role);
                    log.info("修复 ROLE_USER 描述为：普通用户");
                }
            });

            // 修复 ROLE_ADMIN 描述
            roleRepository.findByName("ROLE_ADMIN").ifPresent(role -> {
                if (role.getDescription() == null || !role.getDescription().equals("系统管理员")) {
                    role.setDescription("系统管理员");
                    roleRepository.save(role);
                    log.info("修复 ROLE_ADMIN 描述为：系统管理员");
                }
            });
        } catch (Exception e) {
            log.error("修复角色描述失败", e);
        }
    }

    /**
     * 初始化默认用户
     * 注意：角色数据由 schema.sql初始化，这里只创建用户并关联已有角色
     */
    private void initializeUsers(UserRepository userRepository, RoleRepository roleRepository) {
        if (userRepository.count() == 0) {
            // 从数据库获取已存在的角色（由 schema.sql初始化）
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER 角色不存在，请确保 schema.sql 已执行"));
            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN 角色不存在，请确保 schema.sql 已执行"));

            // 普通用户
            User user = createUser("user", "password", List.of(roleUser));
            userRepository.save(user);
            log.info("创建默认用户：user / password");

            // 管理员
            User admin = createUser("admin", "admin123", List.of(roleAdmin, roleUser));
            userRepository.save(admin);
            log.info("创建默认管理员：admin / admin123");

            log.info("默认用户初始化完成，共 {} 个用户", 2);
        } else {
            log.debug("用户数据已存在，跳过初始化");
        }
    }

    /**
     * 创建用户对象
     */
    private User createUser(String username, String rawPassword, List<Role> roles) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setRoles(roles);
        return user;
    }
}
