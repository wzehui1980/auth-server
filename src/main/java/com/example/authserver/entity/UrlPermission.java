package com.example.authserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * URL 权限规则实体
 * 用于动态配置 URL 访问权限
 */
@Entity
@Table(name = "url_permissions")
@Data
public class UrlPermission {

  @Id
  @Column(name = "id", nullable = false, unique = true, length = 100)
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * URL 路径模式（支持通配符）
   * 例如：/admin/**, /api/users/*
   */
  @Column(name = "url_pattern", nullable = false, length = 500)
  private String urlPattern;

  /**
   * HTTP 方法
   * GET, POST, PUT, DELETE, * (所有方法)
   */
  @Column(name = "http_method", nullable = false, length = 20)
  private String httpMethod = "*";

  /**
   * 所需角色
   * 例如：ROLE_ADMIN, ROLE_USER
   */
  @Column(name = "required_role", nullable = false, length = 100)
  private String requiredRole;

  /**
   * 规则描述
   */
  @Column(name = "description", length = 255)
  private String description;

  /**
   * 是否启用
   */
  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  /**
   * 优先级
   * 数字越大优先级越高，用于匹配冲突时
   */
  @Column(name = "priority", nullable = false)
  private int priority = 0;

  /**
   * 创建时间
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * 更新时间
   */
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
