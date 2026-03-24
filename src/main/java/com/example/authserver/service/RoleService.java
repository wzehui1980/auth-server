package com.example.authserver.service;

import com.example.authserver.entity.Role;
import com.example.authserver.entity.UrlPermission;
import com.example.authserver.entity.User;
import com.example.authserver.exception.ResourceConflictException;
import com.example.authserver.exception.ResourceNotFoundException;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.repository.UrlPermissionRepository;
import com.example.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 角色管理服务（独立）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

  private final RoleRepository roleRepository;
  private final UrlPermissionRepository urlPermissionRepository;
  private final UserRepository userRepository;

  /**
   * 获取所有角色及统计信息
   */
  public Map<String, Object> findAllRoles() {
    List<Role> roles = roleRepository.findAllByOrderByName();
    Map<String, Integer> userCountMap = getUserCountMap();

    Map<String, Object> result = new HashMap<>();
    result.put("roles", roles);
    result.put("userCounts", userCountMap);

    log.debug("查询所有角色，共 {} 个", roles.size());

    return result;
  }

  /**
   * 根据名称查找角色
   */
  public Optional<Role> findRoleByName(String roleName) {
    return roleRepository.findByName(roleName);
  }

  /**
   * 新增角色
   */
  @Transactional
  public Role createRole(String roleName, String description) {
    if (!StringUtils.hasText(roleName)) {
      throw new IllegalArgumentException("角色名不能为空");
    }

    if (roleRepository.existsByName(roleName)) {
      log.warn("角色已存在：{}", roleName);
      throw new ResourceConflictException("角色", roleName);
    }

    Role role = new Role();
    role.setName(roleName.trim());
    role.setDescription(description != null ? description.trim() : null);

    Role savedRole = roleRepository.save(role);
    log.info("角色创建成功：{}", roleName);

    return savedRole;
  }

  /**
   * 更新角色描述
   */
  @Transactional
  public void updateRoleDescription(String roleName, String description) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("角色", roleName));

    role.setDescription(description != null ? description.trim() : null);
    roleRepository.save(role);
    log.info("角色描述更新成功：{}", roleName);
  }

  /**
   * 删除角色
   */
  @Transactional
  public void deleteRole(String roleName) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("角色", roleName));

    // 检查是否有用户使用该角色
    if (!role.getUsers().isEmpty()) {
      throw new IllegalArgumentException("有用户正在使用该角色，无法删除");
    }

    // JPA 会自动通过级联删除 role_authorities 关联
    roleRepository.delete(role);
    log.info("角色删除成功：{}", roleName);
  }

  /**
   * 为角色分配 URL 权限规则
   * 注意：这里不是直接关联，而是创建/更新 URL 权限规则
   */
  @Transactional
  public void assignUrlPermissionsToRole(String roleName, List<String> urlPatterns) {
    if (urlPatterns == null || urlPatterns.isEmpty()) {
      throw new IllegalArgumentException("URL 权限规则列表不能为空");
    }

    // 检查角色是否存在
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("角色", roleName));

    log.info("开始为角色 {} 分配 URL 权限规则，共 {} 个", roleName, urlPatterns.size());

    // 为每个 URL 模式创建或更新权限规则
    for (String urlPattern : urlPatterns) {
      // 检查是否已存在该角色的该 URL 规则
      List<UrlPermission> existingPermissions = urlPermissionRepository.findByUrlPattern(urlPattern);

      boolean exists = existingPermissions.stream()
          .anyMatch(p -> p.getRequiredRole().equals(roleName));

      if (!exists) {
        // 创建新的 URL 权限规则
        UrlPermission permission = new UrlPermission();
        permission.setUrlPattern(urlPattern);
        permission.setHttpMethod("*"); // 默认所有 HTTP 方法
        permission.setRequiredRole(roleName);
        permission.setDescription("角色 " + roleName + " 的 URL 权限");
        permission.setEnabled(true);
        permission.setPriority(0);

        urlPermissionRepository.save(permission);
        log.info("创建 URL 权限规则：{} -> {}", urlPattern, roleName);
      }
    }

    log.info("角色 {} URL 权限规则分配完成", roleName);
  }

  /**
   * 获取所有可用角色名称列表
   */
  public List<String> getAvailableRoles() {
    return roleRepository.findAllByOrderByName()
        .stream()
        .map(Role::getName)
        .toList();
  }

  /**
   * 获取角色的用户数量统计
   */
  public Map<String, Integer> getUserCountMap() {
    List<Object[]> results = roleRepository.countUsersByRole();
    Map<String, Integer> userCountMap = new HashMap<>();

    for (Object[] result : results) {
      String roleName = (String) result[0];
      Long count = (Long) result[1];
      userCountMap.put(roleName, count.intValue());
    }

    return userCountMap;
  }

  /**
   * 获取拥有指定角色的用户列表
   */
  public List<User> getUsersByRole(String roleName) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("角色", roleName));

    return role.getUsers() != null ? role.getUsers() : new ArrayList<>();
  }

  /**
   * 获取拥有指定角色的用户数量
   */
  public int countUsersByRole(String roleName) {
    return getUsersByRole(roleName).size();
  }

  /**
   * 删除角色的指定 URL 权限规则
   */
  @Transactional
  public void removeUrlPermissionsFromRole(String roleName, List<String> urlPatterns) {
    if (urlPatterns == null || urlPatterns.isEmpty()) {
      throw new IllegalArgumentException("URL 权限规则列表不能为空");
    }

    log.info("开始删除角色 {} 的 URL 权限规则，共 {} 个", roleName, urlPatterns.size());

    for (String urlPattern : urlPatterns) {
      // 查找匹配的权限规则
      List<UrlPermission> permissions = urlPermissionRepository.findByUrlPattern(urlPattern);

      for (UrlPermission permission : permissions) {
        if (permission.getRequiredRole().equals(roleName)) {
          urlPermissionRepository.delete(permission);
          log.info("删除 URL 权限规则：{} -> {}", urlPattern, roleName);
        }
      }
    }

    log.info("角色 {} URL 权限规则删除完成", roleName);
  }

  /**
   * 获取角色的所有 URL 权限规则
   */
  public List<UrlPermission> getUrlPermissionsByRole(String roleName) {
    // 先检查角色是否存在
    if (!roleRepository.existsByName(roleName)) {
      throw new ResourceNotFoundException("角色", roleName);
    }

    // 获取所有启用的 URL 权限规则，然后过滤出该角色的
    return urlPermissionRepository.findAllEnabled().stream()
        .filter(p -> p.getRequiredRole().equals(roleName))
        .toList();
  }
}
