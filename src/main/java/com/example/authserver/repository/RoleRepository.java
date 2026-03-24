package com.example.authserver.repository;

import com.example.authserver.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问接口（独立）
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

  /**
   * 根据名称查找角色
   */
  Optional<Role> findByName(String name);

  /**
   * 检查角色名是否存在
   */
  boolean existsByName(String name);

  /**
   * 查询所有角色（按名称排序）
   */
  List<Role> findAllByOrderByName();

  /**
   * 搜索角色（按名称模糊匹配）
   */
  @Query("SELECT r FROM Role r WHERE r.name LIKE %:keyword%")
  List<Role> searchByName(@Param("keyword") String keyword);

  /**
   * 统计每个角色的用户数量
   */
  @Query("SELECT r.name, COUNT(u) FROM Role r LEFT JOIN r.users u GROUP BY r.name")
  List<Object[]> countUsersByRole();
}
