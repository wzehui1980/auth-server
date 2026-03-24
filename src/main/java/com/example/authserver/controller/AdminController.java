package com.example.authserver.controller;

import com.example.authserver.annotation.AuditLog;
import com.example.authserver.entity.User;
import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import com.example.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员控制器 - 用户管理模块
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    /**
     * 管理员仪表盘首页
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 添加一些基本的统计信息
        List<User> users = userService.findAllUsers();
        model.addAttribute("userCount", users.size());
        return "admin/dashboard";
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    public String users(Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword) {

        // ⭐ 不主动清除消息，让 RedirectAttributes 的 Flash 属性正常传递
        // model.asMap().remove("errorMessage");
        // model.asMap().remove("successMessage");

        // ⭐ 根据关键词搜索用户（如果有关键词）
        List<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 模糊查询用户名
            users = userService.findAllUsers().stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
            log.info("搜索关键词：{}, 找到 {} 个用户", keyword, users.size());
        } else {
            // 查询所有用户
            users = userService.findAllUsers();
        }

        // 为每个用户添加角色信息
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("enabled", user.getEnabled());
            userData.put("roles", String.join(",", userService.getUserRoles(user.getUsername())));
            // ⭐ 添加创建时间（格式化）
            if (user.getCreatedAt() != null) {
                userData.put("createdAt", formatDateTime(user.getCreatedAt()));
            } else {
                userData.put("createdAt", "-");
            }
            userList.add(userData);
        }

        // 获取所有可用角色
        List<String> allRoles = userService.getAvailableRoles();

        // ⭐ 计算分页信息
        int totalUsers = users.size();
        int currentPage = page > 0 ? page : 1;
        int pageSize = size > 0 ? size : 10;
        int totalPages = (int) Math.ceil((double) totalUsers / pageSize);

        // 确保当前页码不超过总页数
        if (currentPage > totalPages) {
            currentPage = totalPages > 0 ? totalPages : 1;
        }

        // ⭐ 对 userList 进行分页（只取当前页的数据）
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalUsers);
        List<Map<String, Object>> pagedUserList;

        if (fromIndex < totalUsers) {
            pagedUserList = userList.subList(fromIndex, toIndex);
        } else {
            pagedUserList = new ArrayList<>(); // 空列表
        }

        model.addAttribute("users", pagedUserList);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("keyword", keyword != null ? keyword : "");

        return "admin/users";
    }

    /**
     * ⭐ 检查用户名是否存在（AJAX 接口）
     */
    @GetMapping("/users/check-username")
    @ResponseBody
    public Map<String, Boolean> checkUsernameExists(@RequestParam String username) {
        boolean exists = userService.findByUsername(username).isPresent();
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return response;
    }

    /**
     * 新增用户
     */
    @PostMapping("/users/add")
    public String addUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(defaultValue = "ROLE_USER") List<String> roles,
            @RequestParam(defaultValue = "true") boolean enabled,
            RedirectAttributes redirectAttributes) {

        log.info("尝试新增用户：{}", username);

        try {
            userService.createUser(username, password, roles, enabled);

            // ⭐ 成功：重定向到列表页，显示成功消息
            redirectAttributes.addFlashAttribute("successMessage",
                    "用户 '" + username + "' 添加成功！");
            log.info("用户创建成功：{}", username);
            return "redirect:/admin/users";

        } catch (RuntimeException e) {
            // ⭐ 失败：带错误信息重定向回列表页（模态框会自动打开）
            redirectAttributes.addFlashAttribute("errorMessage",
                    "添加失败：" + e.getMessage());
            redirectAttributes.addFlashAttribute("keepModalOpen", true); // 标记保持模态框打开
            log.warn("用户创建失败：{}", e.getMessage());
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "添加失败：系统异常");
            redirectAttributes.addFlashAttribute("keepModalOpen", true);
            log.error("用户创建异常", e);
            return "redirect:/admin/users";
        }
    }

    /**
     * 更新用户
     */
    @AuditLog(module = ModuleType.USER, operationType = OperationType.UPDATE, description = "更新用户: #{#username}")
    @PostMapping("/users/update")
    public String updateUser(
            @RequestParam String username,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "true") boolean enabled,
            RedirectAttributes redirectAttributes) {

        log.info("尝试更新用户：{}", username);

        try {
            userService.updateUser(username, password, enabled);
            redirectAttributes.addFlashAttribute("successMessage",
                    "用户 '" + username + "' 更新成功！");
            log.info("用户更新成功：{}", username);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "更新失败：" + e.getMessage());
            log.warn("用户更新失败：{}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "更新失败：系统异常");
            log.error("用户更新异常", e);
        }

        return "redirect:/admin/users";
    }

    /**
     * 删除用户
     */
    @AuditLog(module = ModuleType.USER, operationType = OperationType.DELETE, description = "删除用户：#{#username}")
    @PostMapping("/users/delete")
    public String deleteUser(
            @RequestParam String username,
            RedirectAttributes redirectAttributes) {

        log.info("尝试删除用户：{}", username);

        try {
            userService.deleteUser(username);
            redirectAttributes.addFlashAttribute("successMessage",
                    "用户 '" + username + "' 已删除");
            log.info("用户删除成功：{}", username);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "删除失败：" + e.getMessage());
            log.warn("用户删除失败：{}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "删除失败：系统异常");
            log.error("用户删除异常", e);
        }

        return "redirect:/admin/users";
    }

    /**
     * 修改用户权限
     */
    @AuditLog(module = ModuleType.USER, operationType = OperationType.ASSIGN, description = "修改用户权限: #{#username}")
    @PostMapping("/users/authorities")
    public String updateUserAuthorities(
            @RequestParam String username,
            @RequestParam(required = false) List<String> authorities,
            RedirectAttributes redirectAttributes) {

        log.info("尝试更新用户权限：{}, 权限列表：{}", username, authorities);

        // ⭐ 检查 authorities 是否为 null 或空
        if (authorities == null || authorities.isEmpty()) {
            log.error("未选择任何角色，username={}, authorities={}", username, authorities);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "更新失败：请至少选择一个权限角色");
            return "redirect:/admin/users";
        }

        try {
            log.info("准备更新用户权限：{}, authorities={}", username, authorities);
            userService.updateUserAuthorities(username, authorities);
            redirectAttributes.addFlashAttribute("successMessage",
                    "用户 '" + username + "' 权限更新成功！");
            log.info("用户权限更新成功：{}", username);
        } catch (IllegalArgumentException e) {
            log.error("参数错误：{}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "更新失败：" + e.getMessage());
        } catch (RuntimeException e) {
            log.error("运行时错误：{}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "更新失败：" + e.getMessage());
            log.warn("用户权限更新失败：{}", e.getMessage());
        } catch (Exception e) {
            log.error("系统异常：{}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "更新失败：系统异常");
            log.error("用户权限更新异常", e);
        }

        return "redirect:/admin/users";
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
