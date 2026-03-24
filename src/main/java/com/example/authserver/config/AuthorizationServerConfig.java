package com.example.authserver.config;

import com.example.authserver.repository.JpaRegisteredClientRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

        private final PasswordEncoder passwordEncoder;

        public AuthorizationServerConfig(PasswordEncoder passwordEncoder) {
                this.passwordEncoder = passwordEncoder;
        }

        /**
         * 配置授权服务器的安全过滤链
         */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
                // 应用授权服务器的默认配置
                OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

                // 启用 OpenID Connect 1.0
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                                .oidc(Customizer.withDefaults());

                http
                                // 当未通过身份验证尝试访问授权端点时，重定向到登录页面
                                .exceptionHandling((exceptions) -> exceptions
                                                .defaultAuthenticationEntryPointFor(
                                                                new LoginUrlAuthenticationEntryPoint("/login"),
                                                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                                // 接受访问令牌用于用户信息和/或客户端注册
                                .oauth2ResourceServer((resourceServer) -> resourceServer
                                                .jwt(Customizer.withDefaults()));

                return http.build();
        }

        /**
         * 配置 RegisteredClientRepository，使用自定义 JPA 实现存储到数据库
         */
        // @Bean
        // public RegisteredClientRepository registeredClientRepository() {
        // // 使用我们自己的 JPA 实现（基于 database schema.sql 中的 oauth2_registered_client 表）
        // return new JpaRegisteredClientRepository();
        // }

        /**
         * 初始化默认客户端配置
         */
        @Bean
        public InitializingBean initializeDefaultClients(RegisteredClientRepository repository) {
                return () -> {
                        // 1. 配置 Web 应用客户端（授权码模式）
                        RegisteredClient webAppClient = RegisteredClient.withId(UUID.randomUUID().toString())
                                        .clientId("web-app-client")
                                        .clientSecret("{bcrypt}" + passwordEncoder.encode("web-app-secret"))
                                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                                        .redirectUri("http://127.0.0.1:9000/authorized")
                                        .scope(OidcScopes.OPENID)
                                        .scope(OidcScopes.PROFILE)
                                        .scope(OidcScopes.EMAIL)
                                        .scope("api.read")
                                        .scope("api.write")
                                        .clientSettings(ClientSettings.builder()
                                                        .requireAuthorizationConsent(true)
                                                        .build())
                                        .tokenSettings(TokenSettings.builder()
                                                        .accessTokenTimeToLive(java.time.Duration.ofHours(2))
                                                        .refreshTokenTimeToLive(java.time.Duration.ofDays(7))
                                                        .reuseRefreshTokens(false)
                                                        .build())
                                        .build();

                        // 2. 配置移动端客户端（PKCE 模式，更安全）
                        RegisteredClient mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
                                        .clientId("mobile-app-client")
                                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // 公开客户端，不需要密钥
                                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                                        .redirectUri("myapp://callback") // 移动端自定义 URI Scheme
                                        .scope(OidcScopes.OPENID)
                                        .scope(OidcScopes.PROFILE)
                                        .scope("api.read")
                                        .clientSettings(ClientSettings.builder()
                                                        .requireAuthorizationConsent(true)
                                                        .requireProofKey(true) // 强制使用 PKCE
                                                        .build())
                                        .tokenSettings(TokenSettings.builder()
                                                        .accessTokenTimeToLive(java.time.Duration.ofHours(1))
                                                        .refreshTokenTimeToLive(java.time.Duration.ofDays(30))
                                                        .reuseRefreshTokens(false)
                                                        .build())
                                        .build();

                        // 3. 配置后端服务客户端（客户端凭证模式，用于服务间调用）
                        RegisteredClient backendClient = RegisteredClient.withId(UUID.randomUUID().toString())
                                        .clientId("backend-service")
                                        .clientSecret("{bcrypt}" + passwordEncoder.encode("backend-secret"))
                                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                        .scope("api.read")
                                        .scope("api.write")
                                        .clientSettings(ClientSettings.builder()
                                                        .requireAuthorizationConsent(false) // 服务间调用不需要用户授权
                                                        .build())
                                        .tokenSettings(TokenSettings.builder()
                                                        .accessTokenTimeToLive(java.time.Duration.ofMinutes(30)) // 服务间调用
                                                                                                                 // token
                                                                                                                 // 有效期较短
                                                        .build())
                                        .build();

                        // 保存客户端配置到数据库（如果不存在）
                        saveOrUpdateClient(repository, webAppClient);
                        saveOrUpdateClient(repository, mobileClient);
                        saveOrUpdateClient(repository, backendClient);
                };
        }

        /**
         * 保存或更新客户端配置
         */
        private void saveOrUpdateClient(RegisteredClientRepository repository, RegisteredClient client) {
                RegisteredClient existingClient = repository.findByClientId(client.getClientId());
                if (existingClient != null) {
                        // 如果客户端已存在，使用原有 ID 进行更新
                        RegisteredClient updated = RegisteredClient.withId(existingClient.getId())
                                        .clientId(client.getClientId())
                                        .clientSecret(client.getClientSecret())
                                        .clientAuthenticationMethods(methods -> methods
                                                        .addAll(client.getClientAuthenticationMethods()))
                                        .authorizationGrantTypes(grantTypes -> grantTypes
                                                        .addAll(client.getAuthorizationGrantTypes()))
                                        .redirectUris(uris -> uris.addAll(client.getRedirectUris()))
                                        .scopes(scopes -> scopes.addAll(client.getScopes()))
                                        .clientSettings(client.getClientSettings())
                                        .tokenSettings(client.getTokenSettings())
                                        .build();
                        repository.save(updated);
                        System.out.println("已更新 OAuth2 客户端：" + client.getClientId());
                } else {
                        repository.save(client);
                        System.out.println("已初始化 OAuth2 客户端：" + client.getClientId());
                }
        }

        /**
         * 配置授权服务，使用 JDBC 存储授权状态
         */
        @Bean
        public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                        RegisteredClientRepository registeredClientRepository) {
                return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        }

        /**
         * 配置授权确认服务，使用 JDBC 存储用户的授权确认结果
         */
        @Bean
        public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                        RegisteredClientRepository registeredClientRepository) {
                return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
        }

        /**
         * 配置 JWK (JSON Web Key) 源，用于签名 JWT 令牌
         */
        @Bean
        public JWKSource<SecurityContext> jwkSource() {
                KeyPair keyPair = generateRsaKey();
                RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
                RSAKey rsaKey = new RSAKey.Builder(publicKey)
                                .privateKey(privateKey)
                                .keyID(UUID.randomUUID().toString())
                                .build();
                JWKSet jwkSet = new JWKSet(rsaKey);
                return new ImmutableJWKSet<>(jwkSet);
        }

        /**
         * 生成用于 JWK 的 RSA 密钥对
         */
        private static KeyPair generateRsaKey() {
                KeyPair keyPair;
                try {
                        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                        keyPairGenerator.initialize(2048);
                        keyPair = keyPairGenerator.generateKeyPair();
                } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                }
                return keyPair;
        }

        /**
         * 配置 JWT 解码器
         */
        @Bean
        public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
                return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        }

        /**
         * 配置授权服务器设置
         */
        @Bean
        public AuthorizationServerSettings authorizationServerSettings() {
                return AuthorizationServerSettings.builder().build();
        }

}
