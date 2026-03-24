package com.example.authserver.service;

import com.example.authserver.entity.Role;
import com.example.authserver.entity.User;
import com.example.authserver.exception.ResourceConflictException;
import com.example.authserver.exception.ResourceNotFoundException;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 用户与角色管理服务（基于模型驱动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * 获取所有用户
   */
  public List<User> findAllUsers() {
    return userRepository.findAll();
  }

  /**
   * 根据用户名查找用户
   */
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  /**
   * 获取用户的角色名称列表
   */
  public List<String> getUserRoles(String username) {
    return userRepository.findByUsername(username)
        .map(user -> user.getRoles().stream()
            .map(Role::getName)
            .toList())
        .orElse(List.of());
  }

  /**
   * 新增用户（基于角色模型）
   */
  @Transactional
  public User createUser(String username, String password, List<String> roleNames, Boolean enabled) {
    // 参数验证
    if (!StringUtils.hasText(username)) {
      throw new IllegalArgumentException("用户名不能为空");
    }

    if (!StringUtils.hasText(password)) {
      throw new IllegalArgumentException("密码不能为空");
    }

    if (password.length() < 6) {
      throw new IllegalArgumentException("密码长度至少为 6 位");
    }

    // 检查用户名是否已存在
    if (userRepository.existsByUsername(username)) {
      log.warn("用户名已存在：{}", username);
      throw new ResourceConflictException("用户名", username);
    }

    User user = new User();
    user.setUsername(username.trim());
    user.setPassword(passwordEncoder.encode(password));
    user.setEnabled(enabled != null ? enabled : true);

    // 分配角色
    if (roleNames != null && !roleNames.isEmpty()) {
      List<Role> roles = roleNames.stream()
          .map(roleName -> roleRepository.findByName(roleName)
              .orElseThrow(() -> new ResourceNotFoundException("角色", roleName)))
          .toList();
      user.setRoles(roles);
      log.info("为用户 {} 分配角色：{}", username, roleNames);
    } else {
      // 默认分配 ROLE_USER
      Role roleUser = roleRepository.findByName("ROLE_USER")
          .orElseThrow(() -> new ResourceNotFoundException("默认角色", "ROLE_USER"));
      user.setRoles(List.of(roleUser));
    }

    // 保存用户
    User savedUser = userRepository.save(user);
    log.info("用户创建成功：{}", username);

    return savedUser;
  }

  /**
   * 更新用户信息
   */
  @Transactional
  public User updateUser(String username, String password, Boolean enabled) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("用户", username));

    // 如果提供了新密码则更新
    if (StringUtils.hasText(password)) {
      String trimmedPassword = password.trim();
      if (trimmedPassword.length() < 6) {
        throw new IllegalArgumentException("密码长度至少为 6 位");
      }
      user.setPassword(passwordEncoder.encode(trimmedPassword));
    }

    // 更新启用状态
    if (enabled != null) {
      user.setEnabled(enabled);
    }

    return userRepository.save(user);
  }

  /**
   * 删除用户
   */
  @Transactional
  public void deleteUser(String username) {
    if (!userRepository.existsByUsername(username)) {
      log.warn("用户不存在：{}", username);
      throw new ResourceNotFoundException("用户", username);
    }

    // JPA 会自动通过级联删除 user_roles 关联
    userRepository.deleteById(username);
    log.info("用户删除成功：{}", username);
  }

  /**
   * 更新用户角色
   */
  @Transactional
  public void updateUserAuthorities(String username, List<String> roleNames) {
    log.info("开始更新用户权限：username={}, roleNames={}", username, roleNames);

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("用户", username));

    if (roleNames == null || roleNames.isEmpty()) {
      throw new IllegalArgumentException("角色列表不能为空");
    }

    // ⭐ 获取新角色列表，并记录找不到的角色
    List<Role> roles = new ArrayList<>();
    for (String roleName : roleNames) {
      Role role = roleRepository.findByName(roleName)
          .orElseThrow(() -> {
            log.error("角色不存在：{}", roleName);
            return new ResourceNotFoundException("角色", roleName);
          });
      roles.add(role);
      log.info("找到角色：{}", roleName);
    }

    // 更新用户角色
    user.setRoles(roles);
    userRepository.save(user);
    log.info("用户 {} 角色更新成功，新角色：{}", username, roleNames);
  }

  /**
   * 获取所有角色
   */
  public Map<String, Object> findAllRoles() {
    List<Role> roles = roleRepository.findAllByOrderByName();
    Map<String, Integer> userCountMap = new HashMap<>();

    // 统计每个角色的用户数量
    for (Role role : roles) {
      int userCount = role.getUsers() != null ? role.getUsers().size() : 0;
      userCountMap.put(role.getName(), userCount);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("roles", roles);
    result.put("userCounts", userCountMap);

    return result;
  }

  /**
   * 新增角色
   */
  @Transactional
  public Role createRole(String roleName, String description) {
    if (!StringUtils.hasText(roleName)) {
      throw new IllegalArgumentException("角色名不能为空");
    }

    if (roleRepository.existsByName(roleName)) {
      log.warn("角色已存在：{}", roleName);
      throw new ResourceConflictException("角色", roleName);
    }

    Role role = new Role();
    role.setName(roleName.trim());
    role.setDescription(description != null ? description.trim() : null);

    Role savedRole = roleRepository.save(role);
    log.info("角色创建成功：{}", roleName);

    return savedRole;
  }

  /**
   * 删除角色
   */
  @Transactional
  public void deleteRole(String roleName) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("角色", roleName));

    // JPA 会自动通过级联删除 user_roles 关联
    roleRepository.delete(role);
    log.info("角色删除成功：{}", roleName);
  }

  /**
   * 根据名称查找角色
   */
  public Optional<Role> findRoleByName(String roleName) {
    return roleRepository.findByName(roleName);
  }

  /**
   * 更新角色描述
   */
  @Transactional
  public void updateRoleDescription(String roleName, String description) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("角色", roleName));

    role.setDescription(description != null ? description.trim() : null);
    roleRepository.save(role);
    log.info("角色描述更新成功：{}", roleName);
  }

  /**
   * 获取所有可用的角色列表
   */
  public List<String> getAvailableRoles() {
    return roleRepository.findAllByOrderByName()
        .stream()
        .map(Role::getName)
        .toList();
  }
}
