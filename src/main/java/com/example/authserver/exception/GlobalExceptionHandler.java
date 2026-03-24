package com.example.authserver.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理资源不存在异常
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Map<String, Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
    log.warn("资源不存在：{}", ex.getMessage());
    return buildErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
  }

  /**
   * 处理资源冲突异常（如重复的用户名、客户端 ID 等）
   */
  @ExceptionHandler(ResourceConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  @ResponseBody
  public Map<String, Object> handleResourceConflictException(ResourceConflictException ex) {
    log.warn("资源冲突：{}", ex.getMessage());
    return buildErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
  }

  /**
   * 处理参数验证失败异常
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
    log.warn("参数验证失败：{}", ex.getMessage());
    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

    Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST.value(), "参数验证失败");
    response.put("fieldErrors", fieldErrors);
    return response;
  }

  /**
   * 处理非法参数异常
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("非法参数：{}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
  }

  /**
   * 处理用户名未找到异常
   */
  @ExceptionHandler(UsernameNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Map<String, Object> handleUsernameNotFoundException(UsernameNotFoundException ex) {
    log.warn("用户未找到：{}", ex.getMessage());
    return buildErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
  }

  /**
   * 处理凭证错误异常
   */
  @ExceptionHandler(BadCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ResponseBody
  public Map<String, Object> handleBadCredentialsException(BadCredentialsException ex) {
    log.warn("凭证错误：{}", ex.getMessage());
    return buildErrorResponse(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
  }

  /**
   * 处理访问拒绝异常
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public Map<String, Object> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("访问被拒绝：{}", ex.getMessage());
    return buildErrorResponse(HttpStatus.FORBIDDEN.value(), "没有权限执行此操作");
  }

  /**
   * 处理其他未捕获的异常
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Map<String, Object> handleGenericException(Exception ex) {
    log.error("系统内部异常", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统内部错误，请稍后重试");
  }

  /**
   * 构建统一的错误响应格式
   */
  private Map<String, Object> buildErrorResponse(int status, String message) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("timestamp", LocalDateTime.now());
    errorResponse.put("status", status);
    errorResponse.put("message", message);
    return errorResponse;
  }
}
