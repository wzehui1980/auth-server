package com.example.authserver.repository;

import com.example.authserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

  /**
   * 根据用户名查找用户
   */
  Optional<User> findByUsername(String username);

  /**
   * 检查用户名是否存在
   */
  boolean existsByUsername(String username);

  /**
   * 查询所有启用的用户
   */
  List<User> findByEnabledTrue();

  /**
   * 查询所有禁用的用户
   */
  List<User> findByEnabledFalse();

  /**
   * 搜索用户（按用户名模糊匹配）
   */
  @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword%")
  List<User> searchByUsername(@Param("keyword") String keyword);
}
