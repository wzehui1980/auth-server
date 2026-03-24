package com.example.authserver.repository;

import com.example.authserver.entity.UrlPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * URL 权限规则数据访问接口
 */
@Repository
public interface UrlPermissionRepository extends JpaRepository<UrlPermission, String> {

  /**
   * 查询所有启用的 URL 权限规则（按优先级降序排列）
   */
  @Query("SELECT u FROM UrlPermission u WHERE u.enabled = true ORDER BY u.priority DESC")
  List<UrlPermission> findAllEnabled();

  /**
   * 根据 URL 模式查询
   */
  List<UrlPermission> findByUrlPattern(String urlPattern);

  /**
   * 检查某个角色的 URL 权限是否存在
   */
  boolean existsByRequiredRole(String requiredRole);
}
