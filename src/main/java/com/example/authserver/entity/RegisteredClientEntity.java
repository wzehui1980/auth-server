package com.example.authserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth2 注册客户端实体（扁平化设计）
 */
@Entity
@Table(name = "oauth2_registered_client")
@Data
public class RegisteredClientEntity {

  @Id
  @Column(name = "id", nullable = false, unique = true, length = 100)
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * 客户端 ID
   */
  @Column(name = "client_id", nullable = false, length = 100)
  private String clientId;

  /**
   * 客户端 ID 签发时间
   */
  @Column(name = "client_id_issued_at")
  private LocalDateTime clientIdIssuedAt;

  /**
   * 客户端密钥（BCrypt 加密）
   */
  @Column(name = "client_secret", length = 500)
  private String clientSecret;

  /**
   * 客户端密钥过期时间
   */
  @Column(name = "client_secret_expires_at")
  private LocalDateTime clientSecretExpiresAt;

  /**
   * 客户端名称
   */
  @Column(name = "client_name", nullable = false, length = 200)
  private String clientName;

  /**
   * 客户端认证方式（逗号分隔）
   */
  @Column(name = "client_authentication_methods", nullable = false, length = 1000)
  private String clientAuthenticationMethods;

  /**
   * 授权类型（逗号分隔）
   */
  @Column(name = "authorization_grant_types", nullable = false, length = 1000)
  private String authorizationGrantTypes;

  /**
   * 重定向 URI（逗号分隔）
   */
  @Column(name = "redirect_uris", length = 1000)
  private String redirectUris;

  /**
   * 登出后重定向 URI（逗号分隔）
   */
  @Column(name = "post_logout_redirect_uris", length = 1000)
  private String postLogoutRedirectUris;

  /**
   * 权限范围（逗号分隔）
   */
  @Column(name = "scopes", nullable = false, length = 1000)
  private String scopes;

  /**
   * 是否需要授权同意
   */
  @Column(name = "require_authorization_consent", nullable = false)
  private boolean requireAuthorizationConsent;

  /**
   * 是否需要 PKCE
   */
  @Column(name = "require_proof_key", nullable = false)
  private boolean requireProofKey;

  /**
   * Access Token 有效期（秒）
   */
  @Column(name = "access_token_time_to_live", nullable = false)
  private int accessTokenTimeToLive;

  /**
   * Refresh Token 有效期（秒）
   */
  @Column(name = "refresh_token_time_to_live", nullable = false)
  private int refreshTokenTimeToLive;

  /**
   * 是否重复使用 Refresh Token
   */
  @Column(name = "reuse_refresh_tokens", nullable = false)
  private boolean reuseRefreshTokens;
}
