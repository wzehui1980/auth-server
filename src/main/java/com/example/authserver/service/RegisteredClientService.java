package com.example.authserver.service;

import com.example.authserver.entity.RegisteredClientEntity;
import com.example.authserver.exception.ResourceNotFoundException;
import com.example.authserver.repository.JpaRegisteredClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * OAuth2 客户端管理服务（独立）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisteredClientService {

    private final JpaRegisteredClientRepository repository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取所有客户端
     */
    public List<RegisteredClientEntity> findAllClients() {
        return repository.findAll();
    }

    /**
     * 根据 Client ID 查找客户端
     */
    public RegisteredClientEntity findByClientId(String clientId) {
        // 先通过接口方法查找，再转换为实体
        org.springframework.security.oauth2.server.authorization.client.RegisteredClient client = repository
                .findByClientId(clientId);

        if (client == null) {
            return null;
        }

        // 从数据库查询实体
        return repository.findByIdInternal(client.getId()).orElse(null);
    }

    /**
     * 检查 Client ID 是否存在
     */
    public boolean existsByClientId(String clientId) {
        return findByClientId(clientId) != null;
    }

    /**
     * 保存客户端
     */
    @Transactional
    public RegisteredClientEntity saveClient(RegisteredClientEntity client) {
        return repository.save(client);
    }

    /**
     * 根据 ID 查找客户端实体
     */
    public RegisteredClientEntity getClientById(String id) {
        return repository.findByIdInternal(id)
                .orElseThrow(() -> new ResourceNotFoundException("客户端", id));
    }

    /**
     * 删除客户端
     */
    @Transactional
    public void deleteClient(String id) {
        RegisteredClientEntity client = getClientById(id);
        repository.delete(client);
        log.info("客户端删除成功：{}", id);
    }

    /**
     * 生成随机客户端 ID
     */
    public String generateClientId() {
        return "client-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成随机客户端密钥
     */
    public String generateClientSecret(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 加密客户端密钥
     */
    public String encodeClientSecret(String clientSecret) {
        return clientSecret != null ? passwordEncoder.encode(clientSecret) : null;
    }

    /**
     * 构建逗号分隔的字符串
     */
    public String joinWithComma(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }

    /**
     * 处理重定向 URI（单个值，支持 null）
     */
    public String processRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return null;
        }
        return redirectUri.trim();
    }
}
