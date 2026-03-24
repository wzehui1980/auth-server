package com.example.authserver.service;

import com.example.authserver.entity.UrlPermission;
import com.example.authserver.repository.UrlPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * URL 权限规则管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlPermissionService {

  private final UrlPermissionRepository urlPermissionRepository;

  /**
   * 获取所有启用的 URL 权限规则（按优先级排序）
   */
  public List<UrlPermission> getAllEnabledPermissions() {
    return urlPermissionRepository.findAllEnabled();
  }

  /**
   * 获取所有 URL 权限规则（包含禁用的）
   */
  public List<UrlPermission> getAllPermissions() {
    return urlPermissionRepository.findAll();
  }

  /**
   * 根据 ID 查找 URL 权限规则
   */
  public UrlPermission findById(String id) {
    return urlPermissionRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("URL 权限规则不存在：" + id));
  }

  /**
   * 创建 URL 权限规则
   */
  @Transactional
  public UrlPermission createPermission(UrlPermission permission) {
    log.info("创建 URL 权限规则：{}, 角色：{}", permission.getUrlPattern(), permission.getRequiredRole());
    return urlPermissionRepository.save(permission);
  }

  /**
   * 更新 URL 权限规则
   */
  @Transactional
  public UrlPermission updatePermission(String id, UrlPermission permission) {
    UrlPermission existing = findById(id);

    existing.setUrlPattern(permission.getUrlPattern());
    existing.setHttpMethod(permission.getHttpMethod());
    existing.setRequiredRole(permission.getRequiredRole());
    existing.setDescription(permission.getDescription());
    existing.setEnabled(permission.isEnabled());
    existing.setPriority(permission.getPriority());

    log.info("更新 URL 权限规则：{}", id);
    return urlPermissionRepository.save(existing);
  }

  /**
   * 删除 URL 权限规则
   */
  @Transactional
  public void deletePermission(String id) {
    if (!urlPermissionRepository.existsById(id)) {
      throw new RuntimeException("URL 权限规则不存在：" + id);
    }
    urlPermissionRepository.deleteById(id);
    log.info("删除 URL 权限规则：{}", id);
  }

  /**
   * 启用/禁用 URL 权限规则
   */
  @Transactional
  public void togglePermission(String id, boolean enabled) {
    UrlPermission permission = findById(id);
    permission.setEnabled(enabled);
    urlPermissionRepository.save(permission);
    log.info("{}URL 权限规则：{}", enabled ? "启用" : "禁用", id);
  }
}
