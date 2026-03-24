package com.example.authserver.controller;

import com.example.authserver.annotation.AuditLog;
import com.example.authserver.entity.Role;
import com.example.authserver.entity.UrlPermission;
import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import com.example.authserver.exception.ResourceConflictException;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 角色管理控制器（独立）
 */
@Slf4j
@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;
  private final RoleRepository roleRepository;

  /**
   * 角色管理页面
   */
  @GetMapping
  public String rolesPage(Model model,
      @RequestParam(required = false) String success) {
    log.debug("访问角色管理页面");

    try {
      // 获取所有角色及统计信息
      Map<String, Object> roleData = roleService.findAllRoles();

      // 构建角色列表（带用户数量和创建时间）
      List<Map<String, Object>> roleList = buildRoleList(roleData);

      model.addAttribute("roles", roleList);
      model.addAttribute("totalRoles", roleList.size());

      // ⭐ 处理成功消息
      if ("created".equals(success)) {
        model.addAttribute("successMessage", "角色创建成功！");
      }

      log.info("角色列表查询成功，共 {} 个角色", roleList.size());

    } catch (Exception e) {
      log.error("查询角色列表失败", e);
      model.addAttribute("errorMessage", "查询角色列表失败：" + e.getMessage());
    }

    return "admin/roles";
  }

  /**
   * 检查角色是否存在（AJAX 接口）
   */
  @GetMapping("/check-exists")
  @ResponseBody
  public Map<String, Boolean> checkRoleExists(@RequestParam String roleName) {
    log.debug("检查角色是否存在：{}", roleName);

    boolean exists = roleRepository.existsByName(roleName);
    Map<String, Boolean> response = new HashMap<>();
    response.put("exists", exists);

    return response;
  }

  /**
   * 新增角色
   */
  @AuditLog(module = ModuleType.ROLE, operationType = OperationType.CREATE, description = "新增角色: #{#roleName}")
  @PostMapping("/add")
  public String addRole(
      @RequestParam String roleName,
      @RequestParam(required = false) String description,
      RedirectAttributes redirectAttributes) {

    log.info("尝试新增角色：{}", roleName);

    try {
      // 确保角色名以 ROLE_ 开头
      if (!roleName.startsWith("ROLE_")) {
        roleName = "ROLE_" + roleName.toUpperCase();
      } else {
        roleName = roleName.toUpperCase();
      }

      roleService.createRole(roleName, description);

      // ⭐ 使用 RedirectAttributes 传递成功消息和角色名
      redirectAttributes.addFlashAttribute("successMessage",
          "角色 '" + roleName + "' 添加成功！");

      log.info("角色创建成功：{}", roleName);

    } catch (ResourceConflictException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "添加失败：角色 '" + roleName + "' 已存在，请选择其他角色名称");
      log.warn("角色创建失败 - 角色已存在：{}", roleName);
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "添加失败：" + e.getMessage());
      log.warn("角色创建失败：{}", e.getMessage());
    }

    // ⭐ 重定向到列表页
    return "redirect:/admin/roles";
  }

  /**
   * 删除角色
   */
  @AuditLog(module = ModuleType.ROLE, operationType = OperationType.DELETE, description = "删除角色：#{#roleName}")
  @PostMapping("/delete")
  public String deleteRole(
      @RequestParam String roleName,
      RedirectAttributes redirectAttributes) {

    log.info("尝试删除角色：{}", roleName);

    try {
      // 保护内置角色
      if ("ROLE_ADMIN".equals(roleName) || "ROLE_USER".equals(roleName)) {
        throw new IllegalArgumentException("系统内置角色不可删除");
      }

      roleService.deleteRole(roleName);

      redirectAttributes.addFlashAttribute("successMessage",
          "角色 '" + roleName + "' 已删除");

      log.info("角色删除成功：{}", roleName);

    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "删除失败：" + e.getMessage());
      log.warn("角色删除失败：{}", e.getMessage());
    }

    return "redirect:/admin/roles";
  }

  /**
   * 更新角色描述
   */
  @AuditLog(module = ModuleType.ROLE, operationType = OperationType.UPDATE, description = "更新角色: #{#roleName}")
  @PostMapping("/update")
  public String updateRole(
      @RequestParam String roleName,
      @RequestParam(required = false) String description,
      RedirectAttributes redirectAttributes) {

    log.info("尝试更新角色描述：{}", roleName);

    try {
      roleService.updateRoleDescription(roleName, description);

      redirectAttributes.addFlashAttribute("successMessage",
          "角色 '" + roleName + "' 描述更新成功！");

      log.info("角色描述更新成功：{}", roleName);

    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "更新失败：" + e.getMessage());
      log.warn("角色描述更新失败：{}", e.getMessage());
    }

    return "redirect:/admin/roles";
  }

  /**
   * 查看角色详情（包含 URL 权限规则）
   */
  @GetMapping("/{roleName}")
  public String viewRoleDetails(
      @PathVariable String roleName,
      Model model) {

    log.debug("查看角色详情：{}", roleName);

    try {
      // 获取角色基本信息
      Role role = roleService.findRoleByName(roleName)
          .orElseThrow(() -> new NoSuchElementException("角色不存在：" + roleName));

      // 构建角色详情
      Map<String, Object> roleDetails = new HashMap<>();
      roleDetails.put("roleName", role.getName());
      roleDetails.put("description", role.getDescription());
      roleDetails.put("id", role.getId());
      roleDetails.put("userCount", role.getUsers() != null ? role.getUsers().size() : 0);

      // 获取拥有该角色的用户列表
      List<String> users = role.getUsers().stream()
          .map(user -> user.getUsername())
          .toList();
      roleDetails.put("users", users);

      // ⭐ 获取角色的 URL 权限规则（从 url_permissions 表）
      List<UrlPermission> urlPermissions = roleService.getUrlPermissionsByRole(roleName);
      roleDetails.put("urlPermissions", urlPermissions);
      roleDetails.put("permissionCount", urlPermissions.size());

      model.addAttribute("roleDetails", roleDetails);
      model.addAttribute("role", roleDetails); // 兼容现有模板
      model.addAttribute("urlPermissions", urlPermissions); // 新增：URL 权限规则列表

      log.info("角色详情查询成功：{}, URL 权限规则 {} 个", roleName, urlPermissions.size());

    } catch (Exception e) {
      log.error("查询角色详情失败：{}", roleName, e);
      model.addAttribute("errorMessage", "查询角色详情失败：" + e.getMessage());
    }

    return "admin/role-detail";
  }

  /**
   * 为角色分配 URL 权限规则
   */
  @AuditLog(module = ModuleType.PERMISSION, operationType = OperationType.ASSIGN, description = "分配URL权限: #{#roleName}")
  @PostMapping("/{roleName}/permissions")
  public String assignUrlPermissions(
      @PathVariable String roleName,
      @RequestParam List<String> urlPatterns,
      RedirectAttributes redirectAttributes) {

    log.info("尝试为角色 {} 分配 URL 权限规则：{}", roleName, urlPatterns);

    try {
      // ⭐ 调用新的方法：分配 URL 权限规则到角色
      roleService.assignUrlPermissionsToRole(roleName, urlPatterns);

      redirectAttributes.addFlashAttribute("successMessage",
          "角色 '" + roleName + "' URL 权限规则分配成功！");

      log.info("角色 URL 权限规则分配成功：{}", roleName);

    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "分配失败：" + e.getMessage());
      log.error("角色 URL 权限规则分配失败", e);
    }

    return "redirect:/admin/roles/" + roleName;
  }

  /**
   * 删除角色的指定 URL 权限规则
   */
  @AuditLog(module = ModuleType.PERMISSION, operationType = OperationType.REVOKE, description = "撤销URL权限: #{#roleName}")
  @PostMapping("/{roleName}/permissions/remove")
  public String removeUrlPermissions(
      @PathVariable String roleName,
      @RequestParam List<String> urlPatterns,
      RedirectAttributes redirectAttributes) {

    log.info("尝试删除角色 {} 的 URL 权限规则：{}", roleName, urlPatterns);

    try {
      // ⭐ 调用新的方法：删除 URL 权限规则
      roleService.removeUrlPermissionsFromRole(roleName, urlPatterns);

      redirectAttributes.addFlashAttribute("successMessage",
          "角色 '" + roleName + "' URL 权限规则删除成功！");

      log.info("角色 URL 权限规则删除成功：{}", roleName);

    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "删除失败：" + e.getMessage());
      log.error("角色 URL 权限规则删除失败", e);
    }

    return "redirect:/admin/roles/" + roleName;
  }

  /**
   * 构建角色列表
   */
  private List<Map<String, Object>> buildRoleList(Map<String, Object> roleData) {
    List<Map<String, Object>> roleList = new ArrayList<>();

    @SuppressWarnings("unchecked")
    Map<String, Integer> userCounts = (Map<String, Integer>) roleData.get("userCounts");

    @SuppressWarnings("unchecked")
    List<Role> roles = (List<Role>) roleData.get("roles");

    for (Role role : roles) {
      Map<String, Object> roleInfo = new HashMap<>();
      roleInfo.put("roleName", role.getName());
      roleInfo.put("userCount", userCounts.getOrDefault(role.getName(), 0));
      roleInfo.put("description", role.getDescription() != null
          ? role.getDescription()
          : getDefaultDescription(role.getName()));
      roleInfo.put("id", role.getId());
      // ⭐ 添加创建时间（格式：YYYY-MM-DD HH:mm:ss）
      if (role.getCreatedAt() != null) {
        roleInfo.put("createdAt", formatDateTime(role.getCreatedAt()));
      } else {
        roleInfo.put("createdAt", "-");
      }
      roleList.add(roleInfo);
    }

    return roleList;
  }

  /**
   * 获取默认角色描述
   */
  private String getDefaultDescription(String roleName) {
    return switch (roleName) {
      case "ROLE_ADMIN" -> "系统管理员";
      case "ROLE_USER" -> "普通用户";
      default -> "系统自定义角色";
    };
  }

  /**
   * ⭐ 格式化日期时间（YYYY-MM-DD HH:mm）
   */
  private String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return "-";
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    return dateTime.format(formatter);
  }
}
