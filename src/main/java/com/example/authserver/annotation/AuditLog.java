package com.example.authserver.annotation;

import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * 标注在 Controller 方法上，通过 AOP 自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

  /**
   * 操作模块
   */
  ModuleType module();

  /**
   * 操作类型
   */
  OperationType operationType();

  /**
   * 操作描述，支持 SpEL 表达式（如 "删除用户: #{#username}"）
   */
  String description() default "";
}
