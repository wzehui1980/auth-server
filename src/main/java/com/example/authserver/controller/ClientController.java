package com.example.authserver.controller;

import com.example.authserver.annotation.AuditLog;
import com.example.authserver.entity.RegisteredClientEntity;
import com.example.authserver.enums.ModuleType;
import com.example.authserver.enums.OperationType;
import com.example.authserver.service.RegisteredClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 客户端管理控制器（独立）
 */
@Slf4j
@Controller
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
public class ClientController {

    private final RegisteredClientService clientService;

    /**
     * 客户端管理页面
     */
    @GetMapping
    public String clientsPage(Model model) {
        log.debug("访问客户端管理页面");

        try {
            List<RegisteredClientEntity> clients = clientService.findAllClients();

            // ⭐ 为每个客户端预处理分割后的字段
            List<Map<String, Object>> clientList = new ArrayList<>();
            for (RegisteredClientEntity client : clients) {
                Map<String, Object> clientData = new HashMap<>();
                clientData.put("id", client.getId());
                clientData.put("clientId", client.getClientId());
                clientData.put("clientName", client.getClientName()); // ✅ 对应数据库 client_name 字段
                clientData.put("clientIdIssuedAt", client.getClientIdIssuedAt());
                // 分割逗号分隔的字符串
                clientData.put("clientAuthenticationMethods", splitString(client.getClientAuthenticationMethods()));
                clientData.put("authorizationGrantTypes", splitString(client.getAuthorizationGrantTypes()));
                clientData.put("scopes", splitString(client.getScopes()));
                clientData.put("redirectUris", splitString(client.getRedirectUris()));
                clientList.add(clientData);
            }

            model.addAttribute("clients", clientList);
            model.addAttribute("totalClients", clientList.size());

            log.info("客户端列表查询成功，共 {} 个客户端", clientList.size());

        } catch (Exception e) {
            log.error("查询客户端列表失败", e);
            model.addAttribute("errorMessage", "查询客户端列表失败：" + e.getMessage());
        }

        return "admin/clients";
    }

    /**
     * ⭐ 将逗号分隔的字符串转换为 List
     */
    private java.util.List<String> splitString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return java.util.Arrays.asList(str.split(","));
    }

    /**
     * ⭐ 格式化日期时间（YYYY-MM-DD HH:mm）
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }

    /**
     * 创建客户端
     */
    @AuditLog(module = ModuleType.CLIENT, operationType = OperationType.CREATE, description = "新增客户端: #{#clientId}")
    @PostMapping({ "/create", "/add" })
    public String createClient(
            @RequestParam(name = "clientId", defaultValue = "") String clientId,
            @RequestParam(name = "clientName", defaultValue = "") String clientName,
            @RequestParam(name = "clientSecret", defaultValue = "") String clientSecret,
            @RequestParam(defaultValue = "CLIENT_SECRET_BASIC") String clientAuthenticationMethod,
            @RequestParam(name = "authorizationGrantTypes", required = false) List<String> authorizationGrantTypes,
            @RequestParam(required = false) String redirectUri,
            @RequestParam(name = "scopes", required = false) List<String> scopes,
            @RequestParam(name = "accessTokenTTL", defaultValue = "2") int accessTokenTTL,
            @RequestParam(name = "refreshTokenTTL", defaultValue = "7") int refreshTokenTTL,
            @RequestParam(name = "requireConsent", defaultValue = "false") boolean requireConsent,
            @RequestParam(name = "requireProofKey", defaultValue = "false") boolean requireProofKey,
            RedirectAttributes redirectAttributes) {

        log.info("尝试创建客户端：{}", clientId);

        try {
            // 生成客户端 ID（如果为空）
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = clientService.generateClientId();
            }

            // 检查客户端 ID 是否已存在
            if (clientService.existsByClientId(clientId)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "客户端 ID '" + clientId + "' 已存在，请使用其他 ID");
                return "redirect:/admin/clients";
            }

            // 生成客户端密钥（如果为空或认证方式为 NONE）
            if ("NONE".equals(clientAuthenticationMethod)) {
                // 公开客户端不需要密钥
                clientSecret = null;
            } else if (clientSecret == null || clientSecret.trim().isEmpty()) {
                // ⭐ 生成 12 位随机密钥
                clientSecret = clientService.generateClientSecret(12);
            }
            // ⭐ 使用明文密钥，不进行加密
            String plainSecret = clientSecret;

            // 构建授权类型字符串
            String grantTypesStr = clientService.joinWithComma(authorizationGrantTypes);

            // 构建重定向 URI 字符串（单个，处理 null 值）
            String redirectUrisStr = clientService.processRedirectUri(redirectUri);

            // 构建权限范围字符串
            String scopesStr = clientService.joinWithComma(scopes);
            if (scopesStr.isEmpty()) {
                scopesStr = "openid,profile"; // 默认 scope
            }

            // 创建客户端实体
            RegisteredClientEntity client = new RegisteredClientEntity();
            client.setId(UUID.randomUUID().toString());
            client.setClientId(clientId);
            client.setClientSecret(plainSecret); // ⭐ 使用明文密钥
            client.setClientIdIssuedAt(LocalDateTime.now());
            // 仅当有密钥时才设置过期时间
            if (plainSecret != null) {
                client.setClientSecretExpiresAt(LocalDateTime.now().plusYears(1));
            } else {
                client.setClientSecretExpiresAt(null);
            }
            // ⭐ 使用表单提交的 clientName，如果为空则使用 clientId
            client.setClientName(clientName != null && !clientName.trim().isEmpty() ? clientName : clientId);
            client.setClientAuthenticationMethods(clientAuthenticationMethod);
            client.setAuthorizationGrantTypes(grantTypesStr);
            client.setRedirectUris(redirectUrisStr);
            client.setScopes(scopesStr);
            client.setRequireAuthorizationConsent(requireConsent);
            client.setRequireProofKey(requireProofKey);
            client.setAccessTokenTimeToLive(accessTokenTTL * 3600); // 小时转秒
            client.setRefreshTokenTimeToLive(refreshTokenTTL * 86400); // 天转秒
            client.setReuseRefreshTokens(false);

            // 保存到数据库
            clientService.saveClient(client);

            // ⭐ 使用客户端名称显示成功消息，不显示密钥
            String displayName = clientName != null && !clientName.trim().isEmpty() ? clientName : clientId;
            redirectAttributes.addFlashAttribute("successMessage",
                    "客户端 '" + displayName + "' 创建成功！");

            log.info("客户端创建成功：{}, 名称：{}", clientId, displayName);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "创建失败：" + e.getMessage());
            log.error("客户端创建失败", e);
        }

        return "redirect:/admin/clients";
    }

    /**
     * 删除客户端
     */
    @AuditLog(module = ModuleType.CLIENT, operationType = OperationType.DELETE, description = "删除客户端：#{#id}")
    @PostMapping("/delete")
    public String deleteClient(
            @RequestParam String id,
            RedirectAttributes redirectAttributes) {

        log.info("尝试删除客户端：{}", id);

        try {
            clientService.deleteClient(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "客户端已删除");

            log.info("客户端删除成功：{}", id);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "删除失败：" + e.getMessage());
            log.error("客户端删除失败", e);
        }

        return "redirect:/admin/clients";
    }

    /**
     * ⭐ 获取客户端详情（JSON）
     */
    @GetMapping("/detail/{clientId}")
    @ResponseBody
    public Map<String, Object> getClientDetail(@PathVariable String clientId) {
        log.info("获取客户端详情：{}", clientId);

        try {
            // 查询所有客户端，找到匹配的 clientId
            List<RegisteredClientEntity> clients = clientService.findAllClients();
            RegisteredClientEntity client = clients.stream()
                    .filter(c -> c.getClientId().equals(clientId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("客户端不存在：" + clientId));

            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("clientId", client.getClientId());
            result.put("clientName", client.getClientName());
            result.put("clientSecret", client.getClientSecret());
            result.put("clientAuthenticationMethods", splitString(client.getClientAuthenticationMethods()));
            result.put("authorizationGrantTypes", splitString(client.getAuthorizationGrantTypes()));
            result.put("scopes", splitString(client.getScopes()));
            result.put("redirectUris", splitString(client.getRedirectUris()));
            result.put("accessTokenTTL", client.getAccessTokenTimeToLive() / 3600); // 秒转小时
            result.put("refreshTokenTTL", client.getRefreshTokenTimeToLive() / 86400); // 秒转天
            result.put("requireConsent", client.isRequireAuthorizationConsent());
            result.put("requireProofKey", client.isRequireProofKey());

            return result;

        } catch (Exception e) {
            log.error("获取客户端详情失败", e);
            throw new RuntimeException("获取客户端详情失败：" + e.getMessage());
        }
    }

    /**
     * ⭐ 更新客户端
     */
    @AuditLog(module = ModuleType.CLIENT, operationType = OperationType.UPDATE, description = "更新客户端: #{#clientId}")
    @PostMapping("/update")
    public String updateClient(
            @RequestParam(name = "clientId") String clientId,
            @RequestParam(name = "clientName", defaultValue = "") String clientName,
            @RequestParam(name = "clientSecret", defaultValue = "") String clientSecret,
            @RequestParam(defaultValue = "CLIENT_SECRET_BASIC") String clientAuthenticationMethod,
            @RequestParam(name = "authorizationGrantTypes", required = false) List<String> authorizationGrantTypes,
            @RequestParam(required = false) String redirectUri,
            @RequestParam(name = "scopes", required = false) List<String> scopes,
            @RequestParam(name = "accessTokenTTL", defaultValue = "2") int accessTokenTTL,
            @RequestParam(name = "refreshTokenTTL", defaultValue = "7") int refreshTokenTTL,
            @RequestParam(name = "requireConsent", defaultValue = "false") boolean requireConsent,
            @RequestParam(name = "requireProofKey", defaultValue = "false") boolean requireProofKey,
            RedirectAttributes redirectAttributes) {

        log.info("尝试更新客户端：{}", clientId);

        try {
            // ⭐ 后端验证：客户端名称（必填）
            if (clientName == null || clientName.trim().isEmpty()) {
                throw new RuntimeException("客户端名称不能为空");
            }

            // ⭐ 后端验证：授权模式（至少一个）
            if (authorizationGrantTypes == null || authorizationGrantTypes.isEmpty()) {
                throw new RuntimeException("请至少选择一个授权模式");
            }

            // ⭐ 后端验证：重定向 URI（必填且格式正确）
            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                throw new RuntimeException("重定向 URI 不能为空");
            }
            // URI 格式验证
            String uriPattern = "^(https?://)([\\da-z.-]+)(:[\\d]+)?([/\\w .-]*)*/?$";
            if (!redirectUri.matches(uriPattern)) {
                throw new RuntimeException("重定向 URI 格式不正确，请输入有效的 URL");
            }

            // ⭐ 后端验证：权限范围（至少一个）
            if (scopes == null || scopes.isEmpty()) {
                throw new RuntimeException("请至少选择一个权限范围");
            }

            // 查询所有客户端，找到匹配的 clientId
            List<RegisteredClientEntity> clients = clientService.findAllClients();
            RegisteredClientEntity client = clients.stream()
                    .filter(c -> c.getClientId().equals(clientId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("客户端不存在：" + clientId));

            // 构建授权类型字符串
            String grantTypesStr = clientService.joinWithComma(authorizationGrantTypes);

            // 构建重定向 URI 字符串（单个，处理 null 值）
            String redirectUrisStr = clientService.processRedirectUri(redirectUri);

            // 构建权限范围字符串
            String scopesStr = clientService.joinWithComma(scopes);
            if (scopesStr.isEmpty()) {
                scopesStr = "openid,profile"; // 默认 scope
            }

            // 更新客户端信息
            // ⭐ 使用表单提交的 clientName，如果为空则使用 clientId
            client.setClientName(clientName != null && !clientName.trim().isEmpty() ? clientName : clientId);
            client.setClientAuthenticationMethods(clientAuthenticationMethod);
            client.setAuthorizationGrantTypes(grantTypesStr);
            client.setRedirectUris(redirectUrisStr);
            client.setScopes(scopesStr);
            client.setRequireAuthorizationConsent(requireConsent);
            client.setRequireProofKey(requireProofKey);
            client.setAccessTokenTimeToLive(accessTokenTTL * 3600); // 小时转秒
            client.setRefreshTokenTimeToLive(refreshTokenTTL * 86400); // 天转秒

            // 如果有新密钥，则更新（否则保持原样）
            if (clientSecret != null && !clientSecret.trim().isEmpty()
                    && !"••••••••••••".equals(clientSecret) && !"••••••••••••".equals(clientSecret)) {
                String encodedSecret = clientService.encodeClientSecret(clientSecret);
                client.setClientSecret(encodedSecret);
                if (encodedSecret != null) {
                    client.setClientSecretExpiresAt(LocalDateTime.now().plusYears(1));
                } else {
                    client.setClientSecretExpiresAt(null);
                }
            }

            // 保存到数据库
            clientService.saveClient(client);

            redirectAttributes.addFlashAttribute("successMessage",
                    "客户端 '" + client.getClientName() + "' 更新成功！");

            log.info("客户端更新成功：{}", clientId);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失败：" + e.getMessage());
            log.error("客户端更新失败", e);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失败：系统异常");
            log.error("客户端更新异常", e);
        }

        return "redirect:/admin/clients";
    }
}
