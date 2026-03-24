package com.example.authserver.service;

import com.example.authserver.entity.AuditLogEntity;
import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import com.example.authserver.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  /**
   * 保存审计日志（不抛出异常，审计日志写入失败不影响业务）
   */
  @Transactional
  public void saveLog(AuditLogEntity entity) {
    try {
      auditLogRepository.save(entity);
    } catch (Exception e) {
      log.error("审计日志保存失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 便捷方法：构建并保存审计日志
   */
  public void saveLog(String operator, OperationType operationType, ModuleType module,
      String description, String requestUri, String requestMethod,
      String ipAddress, String methodName, String params,
      String result, String errorMessage, Long executionTime) {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setOperator(operator);
    entity.setOperationType(operationType);
    entity.setModule(module);
    entity.setDescription(description);
    entity.setRequestUri(requestUri);
    entity.setRequestMethod(requestMethod);
    entity.setIpAddress(ipAddress);
    entity.setMethodName(methodName);
    entity.setParams(params);
    entity.setResult(result);
    entity.setErrorMessage(errorMessage);
    entity.setExecutionTime(executionTime);
    saveLog(entity);
  }

  /**
   * 多条件分页查询审计日志
   */
  public Page<AuditLogEntity> findLogs(String operator, OperationType operationType,
      ModuleType module, String result,
      LocalDateTime startTime, LocalDateTime endTime,
      int page, int size) {
    Pageable pageable = PageRequest.of(page - 1, size);
    return auditLogRepository.findByConditions(
        operator, operationType, module, result, startTime, endTime, pageable);
  }

  /**
   * 获取统计信息
   */
  public Map<String, Object> getStatistics() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalCount", auditLogRepository.count());

    LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    stats.put("todayCount", auditLogRepository.countByCreatedAtAfter(todayStart));
    stats.put("failureCount", auditLogRepository.countByResult("FAILURE"));
    stats.put("loginCount", auditLogRepository.countByOperationType(OperationType.LOGIN));

    return stats;
  }
}
