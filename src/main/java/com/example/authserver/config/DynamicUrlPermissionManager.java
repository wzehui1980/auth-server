package com.example.authserver.config;

import com.example.authserver.entity.UrlPermission;
import com.example.authserver.service.UrlPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 URL 权限管理器
 * 从数据库加载 URL 权限规则，并提供匹配逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicUrlPermissionManager {

  private final UrlPermissionService urlPermissionService;

  // 缓存已加载的权限规则
  private final Map<String, UrlPermission> permissionCache = new ConcurrentHashMap<>();

  // 路径匹配器
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  /**
   * 初始化时加载所有启用的 URL 权限规则
   */
  @PostConstruct
  public void init() {
    reloadPermissions();
    log.info("动态 URL 权限管理器初始化完成，已加载 {} 条规则", permissionCache.size());
  }

  /**
   * 重新加载所有权限规则
   */
  public void reloadPermissions() {
    List<UrlPermission> permissions = urlPermissionService.getAllEnabledPermissions();

    permissionCache.clear();
    for (UrlPermission permission : permissions) {
      permissionCache.put(permission.getId(), permission);
    }

    log.info("重新加载 URL 权限规则，共 {} 条", permissionCache.size());
  }

  /**
   * 检查请求是否需要特定角色
   * 
   * @param requestUri 请求 URI
   * @param httpMethod HTTP 方法
   * @param userRoles  用户角色列表
   * @return 是否有权限访问
   */
  public boolean hasPermission(String requestUri, String httpMethod, List<String> userRoles) {
    // 按优先级排序（优先级高的先匹配）
    List<UrlPermission> sortedPermissions = new ArrayList<>(permissionCache.values());
    sortedPermissions.sort((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()));

    for (UrlPermission permission : sortedPermissions) {
      if (matchesUrl(permission, requestUri, httpMethod)) {
        boolean hasRole = userRoles.contains(permission.getRequiredRole());
        log.debug("URL 权限匹配：{} {}, 所需角色：{}, 用户角色：{}, 结果：{}",
            httpMethod, requestUri, permission.getRequiredRole(), userRoles, hasRole);
        return hasRole;
      }
    }

    // 如果没有匹配的规则，默认允许访问
    log.debug("未找到匹配的 URL 权限规则，允许访问：{} {}", httpMethod, requestUri);
    return true;
  }

  /**
   * 检查 URL 和 HTTP 方法是否匹配
   */
  private boolean matchesUrl(UrlPermission permission, String requestUri, String httpMethod) {
    // 匹配 URL 模式
    boolean urlMatch = pathMatcher.match(permission.getUrlPattern(), requestUri);

    // 匹配 HTTP 方法（* 表示所有方法）
    boolean methodMatch = "*".equals(permission.getHttpMethod()) ||
        permission.getHttpMethod().equalsIgnoreCase(httpMethod);

    return urlMatch && methodMatch;
  }

  /**
   * 获取所有已加载的权限规则
   */
  public List<UrlPermission> getAllPermissions() {
    return new ArrayList<>(permissionCache.values());
  }

  /**
   * 添加单个权限规则到缓存
   */
  public void addPermission(UrlPermission permission) {
    permissionCache.put(permission.getId(), permission);
    log.info("添加 URL 权限规则到缓存：{} {}", permission.getUrlPattern(), permission.getRequiredRole());
  }

  /**
   * 从缓存中移除权限规则
   */
  public void removePermission(String id) {
    permissionCache.remove(id);
    log.info("从缓存中移除 URL 权限规则：{}", id);
  }
}
