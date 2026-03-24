package com.example.authserver.repository;

import com.example.authserver.entity.AuditLogEntity;
import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

  @Query("SELECT a FROM AuditLogEntity a WHERE " +
      "(:operator IS NULL OR a.operator LIKE %:operator%) AND " +
      "(:operationType IS NULL OR a.operationType = :operationType) AND " +
      "(:module IS NULL OR a.module = :module) AND " +
      "(:result IS NULL OR a.result = :result) AND " +
      "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
      "(:endTime IS NULL OR a.createdAt <= :endTime) " +
      "ORDER BY a.createdAt DESC")
  Page<AuditLogEntity> findByConditions(
      @Param("operator") String operator,
      @Param("operationType") OperationType operationType,
      @Param("module") ModuleType module,
      @Param("result") String result,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime,
      Pageable pageable);

  long countByCreatedAtAfter(LocalDateTime time);

  long countByResult(String result);

  long countByOperationType(OperationType operationType);
}
