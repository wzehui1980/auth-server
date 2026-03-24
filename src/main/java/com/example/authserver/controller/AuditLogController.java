package com.example.authserver.controller;

import com.example.authserver.entity.AuditLogEntity;
import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import com.example.authserver.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 审计日志管理控制器
 */
@Slf4j
@Controller
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditLogService auditLogService;

  @GetMapping
  public String auditLogsPage(
      Model model,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String operator,
      @RequestParam(required = false) String operationType,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String result,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {

    // 先添加枚举值列表（即使后续出错，这些基础数据也能正常显示）
    model.addAttribute("operationTypes", OperationType.values());
    model.addAttribute("moduleTypes", ModuleType.values());

    // 保留当前筛选条件
    model.addAttribute("operator", operator != null ? operator : "");
    model.addAttribute("operationType", operationType != null ? operationType : "");
    model.addAttribute("module", module != null ? module : "");
    model.addAttribute("result", result != null ? result : "");
    model.addAttribute("startDate", startDate != null ? startDate : "");
    model.addAttribute("endDate", endDate != null ? endDate : "");

    try {
      // 解析筛选参数
      OperationType opType = null;
      if (operationType != null && !operationType.isEmpty()) {
        try {
          opType = OperationType.valueOf(operationType);
        } catch (IllegalArgumentException e) {
          log.debug("无效的操作类型: {}", operationType);
        }
      }

      ModuleType modType = null;
      if (module != null && !module.isEmpty()) {
        try {
          modType = ModuleType.valueOf(module);
        } catch (IllegalArgumentException e) {
          log.debug("无效的模块类型: {}", module);
        }
      }

      String resultFilter = (result != null && !result.isEmpty()) ? result : null;
      String operatorFilter = (operator != null && !operator.trim().isEmpty()) ? operator.trim() : null;

      // 解析日期范围
      LocalDateTime startTime = null;
      LocalDateTime endTime = null;
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      if (startDate != null && !startDate.isEmpty()) {
        try {
          startTime = LocalDate.parse(startDate, dateFormatter).atStartOfDay();
        } catch (Exception e) {
          log.debug("无效的开始日期: {}", startDate);
        }
      }
      if (endDate != null && !endDate.isEmpty()) {
        try {
          endTime = LocalDate.parse(endDate, dateFormatter).atTime(LocalTime.MAX);
        } catch (Exception e) {
          log.debug("无效的结束日期: {}", endDate);
        }
      }

      // 查询数据
      Page<AuditLogEntity> logPage = auditLogService.findLogs(
          operatorFilter, opType, modType, resultFilter, startTime, endTime, page, size);

      // 获取统计信息
      Map<String, Object> statistics = auditLogService.getStatistics();

      // 传递数据到模板
      model.addAttribute("logs", logPage.getContent());
      model.addAttribute("currentPage", page);
      model.addAttribute("pageSize", size);
      model.addAttribute("totalPages", logPage.getTotalPages());
      model.addAttribute("totalElements", logPage.getTotalElements());
      model.addAttribute("statistics", statistics);

    } catch (Exception e) {
      log.error("查询审计日志失败", e);
      // 设置默认值
      model.addAttribute("logs", java.util.Collections.emptyList());
      model.addAttribute("currentPage", 1);
      model.addAttribute("pageSize", size);
      model.addAttribute("totalPages", 0);
      model.addAttribute("totalElements", 0L);
      model.addAttribute("statistics", java.util.Map.of(
          "totalCount", 0L,
          "todayCount", 0L,
          "failureCount", 0L,
          "loginCount", 0L));
      // ⭐ 确保枚举类型数据仍然存在
      model.addAttribute("operationTypes", OperationType.values());
      model.addAttribute("moduleTypes", ModuleType.values());
    }

    return "admin/audit-logs";
  }
}
