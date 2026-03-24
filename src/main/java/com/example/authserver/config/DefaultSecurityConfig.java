/*
 * @Author: error: error: git config user.name & please set dead value or install git && error: git config user.email & please set dead value or install git & please set dead value or install git
 * @Date: 2026-03-20 12:00:56
 * @LastEditors: error: error: git config user.name & please set dead value or install git && error: git config user.email & please set dead value or install git & please set dead value or install git
 * @LastEditTime: 2026-03-20 14:19:11
 * @FilePath: d:\DEVTOOLS2025\workspace\testAuthServer\src\main\java\com\example\authserver\config\DefaultSecurityConfig.java
 * @Description: 这是默认设置，请设置 `customMade`, 打开 koroFileHeader 查看配置 进行设置：https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.authserver.config;

import com.example.authserver.listener.SecurityEventListener;
import com.example.authserver.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 默认安全配置
 * 使用动态 URL 权限管理器处理所有请求的权限控制
 */
@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

    /**
     * 配置认证提供者，使用数据库中的用户信息
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 配置密码编码器，用于加密和验证密码
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 配置默认的安全过滤链（用于处理登录等常规请求）
     * 所有 URL 权限控制由动态 URL 权限管理器负责
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
            SecurityEventListener securityEventListener) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        // 允许访问静态资源和公开端点
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/login", "/oauth2/**", "/error").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated())
                // 表单登录处理从授权服务器过滤链重定向过来的登录请求
                .formLogin(form -> form
                        .defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .addLogoutHandler(securityEventListener)
                        .permitAll());

        return http.build();
    }
}
