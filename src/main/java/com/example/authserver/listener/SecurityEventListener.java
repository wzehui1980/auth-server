package com.example.authserver.listener;

import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import com.example.authserver.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Spring Security 安全事件监听器
 * 监听登录成功、登录失败事件，并实现 LogoutHandler 处理登出审计
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEventListener implements LogoutHandler {

  private final AuditLogService auditLogService;

  /**
   * 登录成功事件
   */
  @EventListener
  public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    try {
      String username = event.getAuthentication().getName();
      String ipAddress = getIpFromRequest();
      String requestUri = getRequestUri();

      auditLogService.saveLog(username, OperationType.LOGIN, ModuleType.AUTH,
          "用户登录成功", requestUri, "POST", ipAddress,
          "SecurityEventListener.onAuthenticationSuccess", null,
          "SUCCESS", null, null);

      log.info("审计日志: 用户 {} 登录成功, IP: {}", username, ipAddress);
    } catch (Exception e) {
      log.error("记录登录成功审计日志失败: {}", e.getMessage());
    }
  }

  /**
   * 登录失败事件（凭证错误）
   */
  @EventListener
  public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
    try {
      String username = event.getAuthentication().getName();
      String ipAddress = getIpFromRequest();
      String requestUri = getRequestUri();
      String errorMsg = event.getException() != null ? event.getException().getMessage() : "用户名或密码错误";

      auditLogService.saveLog(username, OperationType.LOGIN_FAIL, ModuleType.AUTH,
          "登录失败: 用户名或密码错误", requestUri, "POST", ipAddress,
          "SecurityEventListener.onAuthenticationFailure", null,
          "FAILURE", errorMsg, null);

      log.info("审计日志: 用户 {} 登录失败, IP: {}", username, ipAddress);
    } catch (Exception e) {
      log.error("记录登录失败审计日志失败: {}", e.getMessage());
    }
  }

  /**
   * 登出处理（通过 LogoutHandler 接口）
   */
  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    try {
      if (authentication == null) {
        return;
      }
      String username = authentication.getName();
      String ipAddress = getIpAddress(request);

      auditLogService.saveLog(username, OperationType.LOGOUT, ModuleType.AUTH,
          "用户登出", request.getRequestURI(), "POST", ipAddress,
          "SecurityEventListener.logout", null,
          "SUCCESS", null, null);

      log.info("审计日志: 用户 {} 登出, IP: {}", username, ipAddress);
    } catch (Exception e) {
      log.error("记录登出审计日志失败: {}", e.getMessage());
    }
  }

  private String getIpFromRequest() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        return getIpAddress(attributes.getRequest());
      }
    } catch (Exception e) {
      log.debug("无法从 RequestContext 获取 IP: {}", e.getMessage());
    }
    return null;
  }

  private String getRequestUri() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        return attributes.getRequest().getRequestURI();
      }
    } catch (Exception e) {
      log.debug("无法获取请求 URI: {}", e.getMessage());
    }
    return null;
  }

  private String getIpAddress(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
      return ip.split(",")[0].trim();
    }
    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
      return ip;
    }
    return request.getRemoteAddr();
  }
}
