package com.example.authserver.enums;

/**
 * 审计日志操作类型枚举
 */
public enum OperationType {
  CREATE("新增"),
  UPDATE("更新"),
  DELETE("删除"),
  LOGIN("登录"),
  LOGOUT("登出"),
  LOGIN_FAIL("登录失败"),
  ASSIGN("分配权限"),
  REVOKE("撤销权限");

  private final String description;

  OperationType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
