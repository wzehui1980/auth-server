package com.example.authserver.aspect;

import com.example.authserver.annotation.AuditLog;
import com.example.authserver.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ExpressionParser spelParser = new SpelExpressionParser();

  private static final Pattern SPEL_PATTERN = Pattern.compile("#\\{(.+?)}");
  private static final Set<String> SENSITIVE_PARAMS = Set.of(
      "password", "secret", "credential", "token", "clientSecret");
  private static final int MAX_PARAMS_LENGTH = 2000;

  @Around("@annotation(auditLog)")
  public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
    long startTime = System.currentTimeMillis();
    String operator = getCurrentUser();
    String ipAddress = null;
    String requestUri = null;
    String requestMethod = null;

    // 获取 HTTP 请求上下文
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        ipAddress = getIpAddress(request);
        requestUri = request.getRequestURI();
        requestMethod = request.getMethod();
      }
    } catch (Exception e) {
      log.debug("无法获取 HTTP 请求上下文: {}", e.getMessage());
    }

    // 获取方法信息
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
    String params = serializeParams(signature.getParameterNames(), joinPoint.getArgs());

    // 解析 SpEL 描述
    String description = resolveDescription(auditLog.description(),
        signature.getParameterNames(), joinPoint.getArgs());

    String result = "FAILURE";
    String errorMessage = null;

    try {
      Object proceed = joinPoint.proceed();
      result = "SUCCESS";
      return proceed;
    } catch (Throwable e) {
      result = "FAILURE";
      errorMessage = e.getMessage();
      if (errorMessage != null && errorMessage.length() > 1000) {
        errorMessage = errorMessage.substring(0, 1000);
      }
      throw e;
    } finally {
      long executionTime = System.currentTimeMillis() - startTime;
      try {
        auditLogService.saveLog(operator, auditLog.operationType(), auditLog.module(),
            description, requestUri, requestMethod, ipAddress, methodName,
            params, result, errorMessage, executionTime);
      } catch (Exception e) {
        log.error("审计日志记录失败: {}", e.getMessage());
      }
    }
  }

  private String getCurrentUser() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      log.debug("无法获取当前用户: {}", e.getMessage());
    }
    return "anonymous";
  }

  private String getIpAddress(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
      // 多级代理时取第一个非 unknown 的 IP
      return ip.split(",")[0].trim();
    }
    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
      return ip;
    }
    return request.getRemoteAddr();
  }

  private String serializeParams(String[] paramNames, Object[] args) {
    if (paramNames == null || args == null || paramNames.length == 0) {
      return null;
    }
    try {
      Map<String, Object> paramMap = new HashMap<>();
      for (int i = 0; i < paramNames.length; i++) {
        Object arg = args[i];
        // 跳过不可序列化的类型
        if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
            || arg instanceof Model || arg instanceof RedirectAttributes
            || arg instanceof BindingResult || arg instanceof MultipartFile) {
          continue;
        }
        // 过滤敏感参数
        if (SENSITIVE_PARAMS.contains(paramNames[i].toLowerCase())) {
          paramMap.put(paramNames[i], "******");
        } else {
          paramMap.put(paramNames[i], arg);
        }
      }
      String json = objectMapper.writeValueAsString(paramMap);
      if (json.length() > MAX_PARAMS_LENGTH) {
        json = json.substring(0, MAX_PARAMS_LENGTH) + "...(truncated)";
      }
      return json;
    } catch (Exception e) {
      log.debug("参数序列化失败: {}", e.getMessage());
      return null;
    }
  }

  private String resolveDescription(String description, String[] paramNames, Object[] args) {
    if (description == null || description.isEmpty() || !description.contains("#{")) {
      return description;
    }
    try {
      EvaluationContext context = new StandardEvaluationContext();
      if (paramNames != null && args != null) {
        for (int i = 0; i < paramNames.length; i++) {
          context.setVariable(paramNames[i], args[i]);
        }
      }
      Matcher matcher = SPEL_PATTERN.matcher(description);
      StringBuilder sb = new StringBuilder();
      while (matcher.find()) {
        String expression = matcher.group(1);
        Object value = spelParser.parseExpression(expression).getValue(context);
        matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value.toString()) : "null");
      }
      matcher.appendTail(sb);
      return sb.toString();
    } catch (Exception e) {
      log.debug("SpEL 表达式解析失败: {}", e.getMessage());
      return description;
    }
  }
}
