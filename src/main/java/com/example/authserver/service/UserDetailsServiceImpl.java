package com.example.authserver.service;

import com.example.authserver.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 用户详情服务实现
 * 用于 Spring Security 认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserService userService;

  /**
   * 根据用户名加载用户详情
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("加载用户详情：{}", username);

    try {
      // 通过用户名查找用户
      User user = userService.findByUsername(username)
          .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));

      log.info("用户 {} 加载成功", username);

      // 转换为 Spring Security 的 UserDetails
      return new org.springframework.security.core.userdetails.User(
          user.getUsername(),
          user.getPassword(),
          user.getEnabled(),
          true, // accountNonExpired
          true, // credentialsNonExpired
          true, // accountNonLocked
          user.getRoles().stream()
              .map(role -> new SimpleGrantedAuthority(role.getName()))
              .collect(Collectors.toList()));

    } catch (Exception e) {
      log.error("加载用户详情失败：{}", username, e);
      throw new UsernameNotFoundException("加载用户详情失败：" + e.getMessage(), e);
    }
  }
}
