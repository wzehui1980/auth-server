/*
 * @Author: error: error: git config user.name & please set dead value or install git && error: git config user.email & please set dead value or install git & please set dead value or install git
 * @Date: 2026-03-24 10:15:41
 * @LastEditors: error: error: git config user.name & please set dead value or install git && error: git config user.email & please set dead value or install git & please set dead value or install git
 * @LastEditTime: 2026-03-24 14:29:54
 * @FilePath: \testAuthServer\src\main\java\com\example\authserver\enums\ModuleType.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.authserver.enums;

/**
 * 审计日志模块类型枚举
 */
public enum ModuleType {
  USER("用户管理"),
  ROLE("角色管理"),
  CLIENT("客户端管理"),
  PERMISSION("权限管理"),
  AUTH("认证授权");

  private final String description;

  ModuleType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
