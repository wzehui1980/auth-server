package com.example.authserver.repository;

import com.example.authserver.entity.RegisteredClientEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * JPA 实现的 RegisteredClientRepository
 * 将 OAuth2 客户端配置存储到数据库的 oauth2_registered_client 表
 * 注意：不使用@Repository 注解，避免与 AuthorizationServerConfig 中的 Bean 定义冲突
 */
@Slf4j
@Repository
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * 保存或更新客户端配置
   */
  @Override
  @Transactional
  public void save(RegisteredClient registeredClient) {
    Assert.notNull(registeredClient, "RegisteredClient cannot be null");

    log.debug("保存客户端配置：{}", registeredClient.getClientId());

    // 检查是否已存在
    RegisteredClient existing = findByClientId(registeredClient.getClientId());

    if (existing == null) {
      // 新增
      RegisteredClientEntity entity = convertToEntity(registeredClient);
      entityManager.merge(entity); // ⭐ 使用 merge 而不是 persist
      log.info("新增 OAuth2 客户端：{}", registeredClient.getClientId());
    } else {
      // 更新
      RegisteredClientEntity entity = entityManager.find(RegisteredClientEntity.class, existing.getId());
      updateEntity(entity, registeredClient);
      entityManager.merge(entity);
      log.info("更新 OAuth2 客户端：{}", registeredClient.getClientId());
    }
  }

  /**
   * 根据客户端 ID 查找
   */
  @Override
  public RegisteredClient findByClientId(String clientId) {
    Assert.hasText(clientId, "clientId cannot be empty");

    try {
      String jpql = "SELECT e FROM RegisteredClientEntity e WHERE e.clientId = :clientId";
      RegisteredClientEntity entity = entityManager.createQuery(jpql, RegisteredClientEntity.class)
          .setParameter("clientId", clientId)
          .getSingleResult();

      log.debug("找到客户端：{} -> {}", clientId, entity.getId());
      return convertToRegisteredClient(entity);

    } catch (NoResultException e) {
      log.debug("未找到客户端：{}", clientId);
      return null;
    }
  }

  /**
   * 根据 ID 查找（Spring Security OAuth2 需要）
   */
  public RegisteredClient findById(String id) {
    Assert.hasText(id, "id cannot be empty");

    RegisteredClientEntity entity = entityManager.find(RegisteredClientEntity.class, id);
    if (entity != null) {
      log.debug("找到客户端 by ID: {}", id);
      return convertToRegisteredClient(entity);
    }
    log.debug("未找到客户端 by ID: {}", id);
    return null;
  }

  /**
   * 根据 ID 查找实体（内部管理使用）
   */
  public java.util.Optional<RegisteredClientEntity> findByIdInternal(String id) {
    Assert.hasText(id, "id cannot be empty");

    RegisteredClientEntity entity = entityManager.find(RegisteredClientEntity.class, id);
    return java.util.Optional.ofNullable(entity);
  }

  /**
   * 查询所有客户端实体
   */
  public java.util.List<RegisteredClientEntity> findAll() {
    String jpql = "SELECT e FROM RegisteredClientEntity e ORDER BY e.clientId";
    return entityManager.createQuery(jpql, RegisteredClientEntity.class)
        .getResultList();
  }

  /**
   * 保存实体（返回实体对象）
   */
  @Transactional
  public RegisteredClientEntity save(RegisteredClientEntity entity) {
    Assert.notNull(entity, "Entity cannot be null");

    // ⭐ 统一使用 merge，不管新增还是更新
    // 因为我们的 ID 是 UUID，在保存前就已经设置好了
    RegisteredClientEntity result = entityManager.merge(entity);
    log.info("保存客户端实体：{} -> {}", entity.getId(), entity.getClientId());

    return result;
  }

  /**
   * 删除实体
   */
  @Transactional
  public void delete(RegisteredClientEntity entity) {
    Assert.notNull(entity, "Entity cannot be null");

    if (!entityManager.contains(entity)) {
      entity = entityManager.merge(entity);
    }
    entityManager.remove(entity);
    log.info("删除客户端实体：{} -> {}", entity.getId(), entity.getClientId());
  }

  /**
   * 转换为实体类
   */
  private RegisteredClientEntity convertToEntity(RegisteredClient client) {
    RegisteredClientEntity entity = new RegisteredClientEntity();
    entity.setId(client.getId());
    entity.setClientId(client.getClientId());

    // 时间类型转换：Instant -> LocalDateTime
    if (client.getClientIdIssuedAt() != null) {
      entity.setClientIdIssuedAt(java.time.LocalDateTime.ofInstant(
          client.getClientIdIssuedAt(), java.time.ZoneId.systemDefault()));
    } else {
      entity.setClientIdIssuedAt(java.time.LocalDateTime.now());
    }

    entity.setClientSecret(client.getClientSecret());

    if (client.getClientSecretExpiresAt() != null) {
      entity.setClientSecretExpiresAt(java.time.LocalDateTime.ofInstant(
          client.getClientSecretExpiresAt(), java.time.ZoneId.systemDefault()));
    }

    entity.setClientName(client.getClientName());
    entity.setClientAuthenticationMethods(String.join(",",
        client.getClientAuthenticationMethods().stream()
            .map(org.springframework.security.oauth2.core.ClientAuthenticationMethod::getValue)
            .toArray(String[]::new)));
    entity.setAuthorizationGrantTypes(String.join(",",
        client.getAuthorizationGrantTypes().stream()
            .map(org.springframework.security.oauth2.core.AuthorizationGrantType::getValue)
            .toArray(String[]::new)));
    entity.setRedirectUris(String.join(",", client.getRedirectUris().toArray(String[]::new)));
    entity.setPostLogoutRedirectUris(String.join(",", client.getPostLogoutRedirectUris().toArray(String[]::new)));
    entity.setScopes(String.join(",", client.getScopes().toArray(String[]::new)));
    entity.setRequireAuthorizationConsent(client.getClientSettings().isRequireAuthorizationConsent());
    entity.setRequireProofKey(client.getClientSettings().isRequireProofKey());
    entity.setAccessTokenTimeToLive((int) client.getTokenSettings().getAccessTokenTimeToLive().getSeconds());
    entity.setRefreshTokenTimeToLive((int) client.getTokenSettings().getRefreshTokenTimeToLive().getSeconds());
    entity.setReuseRefreshTokens(client.getTokenSettings().isReuseRefreshTokens());

    return entity;
  }

  /**
   * 转换为 RegisteredClient
   */
  private RegisteredClient convertToRegisteredClient(RegisteredClientEntity entity) {
    org.springframework.security.oauth2.server.authorization.settings.ClientSettings.Builder clientSettingsBuilder = org.springframework.security.oauth2.server.authorization.settings.ClientSettings
        .builder()
        .requireAuthorizationConsent(entity.isRequireAuthorizationConsent())
        .requireProofKey(entity.isRequireProofKey());

    org.springframework.security.oauth2.server.authorization.settings.TokenSettings.Builder tokenSettingsBuilder = org.springframework.security.oauth2.server.authorization.settings.TokenSettings
        .builder()
        .accessTokenTimeToLive(java.time.Duration.ofSeconds(entity.getAccessTokenTimeToLive()))
        .refreshTokenTimeToLive(java.time.Duration.ofSeconds(entity.getRefreshTokenTimeToLive()))
        .reuseRefreshTokens(entity.isReuseRefreshTokens());

    return RegisteredClient.withId(entity.getId())
        .clientId(entity.getClientId())
        // 时间类型转换：LocalDateTime -> Instant
        .clientIdIssuedAt(entity.getClientIdIssuedAt() != null
            ? entity.getClientIdIssuedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : null)
        .clientSecret(entity.getClientSecret())
        .clientSecretExpiresAt(entity.getClientSecretExpiresAt() != null
            ? entity.getClientSecretExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : null)
        .clientName(entity.getClientName())
        .clientAuthenticationMethods(methods -> {
          if (entity.getClientAuthenticationMethods() != null) {
            for (String method : entity.getClientAuthenticationMethods().split(",")) {
              if (!method.isEmpty()) {
                methods.add(new org.springframework.security.oauth2.core.ClientAuthenticationMethod(method));
              }
            }
          }
        })
        .authorizationGrantTypes(grantTypes -> {
          if (entity.getAuthorizationGrantTypes() != null) {
            for (String grantType : entity.getAuthorizationGrantTypes().split(",")) {
              if (!grantType.isEmpty()) {
                grantTypes.add(new org.springframework.security.oauth2.core.AuthorizationGrantType(grantType));
              }
            }
          }
        })
        .redirectUris(uris -> {
          if (entity.getRedirectUris() != null) {
            for (String uri : entity.getRedirectUris().split(",")) {
              if (!uri.isEmpty()) {
                uris.add(uri);
              }
            }
          }
        })
        .postLogoutRedirectUris(uris -> {
          if (entity.getPostLogoutRedirectUris() != null) {
            for (String uri : entity.getPostLogoutRedirectUris().split(",")) {
              if (!uri.isEmpty()) {
                uris.add(uri);
              }
            }
          }
        })
        .scopes(scopes -> {
          if (entity.getScopes() != null) {
            for (String scope : entity.getScopes().split(",")) {
              if (!scope.isEmpty()) {
                scopes.add(scope);
              }
            }
          }
        })
        .clientSettings(clientSettingsBuilder.build())
        .tokenSettings(tokenSettingsBuilder.build())
        .build();
  }

  /**
   * 更新实体
   */
  private void updateEntity(RegisteredClientEntity entity, RegisteredClient client) {
    entity.setClientSecret(client.getClientSecret());

    // 时间类型转换：Instant -> LocalDateTime
    if (client.getClientSecretExpiresAt() != null) {
      entity.setClientSecretExpiresAt(java.time.LocalDateTime.ofInstant(
          client.getClientSecretExpiresAt(), java.time.ZoneId.systemDefault()));
    }

    entity.setClientName(client.getClientName());
    entity.setClientAuthenticationMethods(String.join(",",
        client.getClientAuthenticationMethods().stream()
            .map(org.springframework.security.oauth2.core.ClientAuthenticationMethod::getValue)
            .toArray(String[]::new)));
    entity.setAuthorizationGrantTypes(String.join(",",
        client.getAuthorizationGrantTypes().stream()
            .map(org.springframework.security.oauth2.core.AuthorizationGrantType::getValue)
            .toArray(String[]::new)));
    entity.setRedirectUris(String.join(",", client.getRedirectUris().toArray(String[]::new)));
    entity.setPostLogoutRedirectUris(String.join(",", client.getPostLogoutRedirectUris().toArray(String[]::new)));
    entity.setScopes(String.join(",", client.getScopes().toArray(String[]::new)));
    entity.setRequireAuthorizationConsent(client.getClientSettings().isRequireAuthorizationConsent());
    entity.setRequireProofKey(client.getClientSettings().isRequireProofKey());
    entity.setAccessTokenTimeToLive((int) client.getTokenSettings().getAccessTokenTimeToLive().getSeconds());
    entity.setRefreshTokenTimeToLive((int) client.getTokenSettings().getRefreshTokenTimeToLive().getSeconds());
    entity.setReuseRefreshTokens(client.getTokenSettings().isReuseRefreshTokens());
  }
}
