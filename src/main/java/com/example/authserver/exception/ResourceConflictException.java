package com.example.authserver.exception;

/**
 * 资源冲突异常（如重复的用户名、客户端 ID 等）
 */
public class ResourceConflictException extends RuntimeException {

  public ResourceConflictException(String message) {
    super(message);
  }

  public ResourceConflictException(String resourceType, String identifier) {
    super(resourceType + " 已存在：" + identifier);
  }
}
