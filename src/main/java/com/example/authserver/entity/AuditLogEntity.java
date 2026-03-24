package com.example.authserver.entity;

import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Entity
@Table(name = "audit_logs")
@Data
public class AuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, unique = true, length = 100)
  private String id;

  @Column(name = "operator", length = 50)
  private String operator;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, length = 30)
  private OperationType operationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "module", nullable = false, length = 30)
  private ModuleType module;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "request_uri", length = 500)
  private String requestUri;

  @Column(name = "request_method", length = 10)
  private String requestMethod;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  @Column(name = "method_name", length = 200)
  private String methodName;

  @Column(name = "params", columnDefinition = "TEXT")
  private String params;

  @Column(name = "result", nullable = false, length = 20)
  private String result;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "execution_time")
  private Long executionTime;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
