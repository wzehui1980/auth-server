# SSO 认证中心 - OAuth2 Authorization Server

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-green.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.2.2-blue.svg)
![OAuth2](https://img.shields.io/badge/OAuth2-Authorization%20Server-purple.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**基于 Spring Security 的 OAuth2 单点登录认证服务器**

[功能特性](#-功能特性) • [快速开始](#-快速开始) • [系统架构](#-系统架构) • [使用指南](#-使用指南) • [技术栈](#-技术栈)

</div>

---

## 📖 项目简介

这是一个功能完善的 **OAuth2 单点登录（SSO）认证服务器**，基于 Spring Security 6.x 和 Spring Authorization Server 构建。提供用户管理、角色管理、客户端管理、URL 权限控制、审计日志等完整功能，适用于企业级应用的统一认证授权场景。

### 核心能力

- 🔐 **OAuth2 认证** - 支持授权码模式、密码模式等多种授权类型
- 👥 **用户管理** - 用户 CRUD、角色分配、状态管理
- 🎭 **角色管理** - 角色定义、URL 权限规则配置
- 🖥️ **客户端管理** - OAuth2 客户端配置、密钥管理
- 🛡️ **权限控制** - 基于 URL 的动态权限验证
- 📊 **审计日志** - 完整的操作记录和安全事件追踪
- 🎨 **现代化 UI** - 响应式管理后台，支持 Bootstrap 5

---

## ✨ 功能特性

### 1. 认证授权
- ✅ OAuth2.0 标准协议实现
- ✅ 支持 Authorization Code、Password、Client Credentials 等授权模式
- ✅ PKCE 支持（Proof Key for Code Exchange）
- ✅ JWT Token 签发与验证
- ✅ Refresh Token 自动续期
- ✅ 单点登录（SSO）支持

### 2. 用户管理
- ✅ 用户注册、编辑、删除
- ✅ 用户启用/禁用
- ✅ 角色分配与管理
- ✅ 密码加密存储（BCrypt）
- ✅ 用户名唯一性校验

### 3. 角色管理
- ✅ 角色创建、编辑、删除
- ✅ 角色描述管理
- ✅ 内置角色保护（ROLE_ADMIN、ROLE_USER）
- ✅ URL 权限规则分配
- ✅ 角色使用情况统计

### 4. 客户端管理
- ✅ OAuth2 客户端注册
- ✅ 客户端 ID/密钥自动生成
- ✅ 多种认证方式配置（CLIENT_SECRET_BASIC、CLIENT_SECRET_POST、NONE）
- ✅ 授权类型灵活配置
- ✅ 重定向 URI 管理
- ✅ Scope 权限范围设置
- ✅ Token 有效期自定义

### 5. URL 权限控制
- ✅ 动态 URL 规则配置
- ✅ 基于角色的访问控制（RBAC）
- ✅ Ant 风格路径匹配
- ✅ 权限规则实时生效

### 6. 审计日志
- ✅ 全操作记录（CREATE、UPDATE、DELETE、ASSIGN、REVOKE）
- ✅ 用户行为追踪
- ✅ 安全事件监控
- ✅ 多维度筛选查询
- ✅ 统计仪表盘
- ✅ 失败操作标记

---

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Node.js**: 14+ (可选，仅前端开发)

### 1. 克隆项目

```bash
git clone https://github.com/yourusername/auth-server.git
cd auth-server
```

### 2. 配置数据库

创建 MySQL 数据库并修改配置文件：

```sql
CREATE DATABASE IF NOT EXISTS auth_server 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;
```

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_server?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
```

### 3. 编译项目

```bash
mvn clean install
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:9000` 启动

### 5. 访问管理后台

- **管理后台地址**: http://localhost:9000/admin/dashboard
- **默认账号**: admin / admin123

---

## 🏗️ 系统架构

### 技术架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Client Applications                   │
│  (Web App / Mobile App / Third-party Services)          │
└────────────────────┬────────────────────────────────────┘
                     │ OAuth2 Requests
                     ▼
┌─────────────────────────────────────────────────────────┐
│              OAuth2 Authorization Server                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │           Spring Security Filter Chain            │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   User Mgmt  │  │  Role Mgmt   │  │ Client Mgmt  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Permission   │  │  Audit Log   │  │    JWT       │  │
│  │   Control    │  │   Service    │  │   Issuer     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  MySQL Database                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────────────┐ │
│  │  users  │ │  roles  │ │ clients │ │ url_permissions│ │
│  └─────────┘ └─────────┘ └─────────┘ └───────────────┘ │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                   │
│  │user_role│ │audit_log│ │ ...     │                   │
│  └─────────┘ └─────────┘ └─────────┘                   │
└─────────────────────────────────────────────────────────┘
```

### 项目结构

```
auth-server/
├── src/main/java/com/example/authserver/
│   ├── config/                      # 配置类
│   │   ├── AuthorizationServerConfig.java      # OAuth2 服务器配置
│   │   ├── DefaultSecurityConfig.java          # 安全配置
│   │   ├── DataInitializerConfig.java          # 数据初始化
│   │   └── DynamicUrlPermissionManager.java    # 动态权限管理
│   ├── controller/                  # 控制器
│   │   ├── AdminController.java                # 用户管理
│   │   ├── RoleController.java                 # 角色管理
│   │   ├── ClientController.java               # 客户端管理
│   │   └── AuditLogController.java             # 审计日志
│   ├── entity/                      # 实体类
│   │   ├── User.java                           # 用户实体
│   │   ├── Role.java                           # 角色实体
│   │   ├── RegisteredClientEntity.java         # 客户端实体
│   │   └── UrlPermission.java                  # URL 权限实体
│   ├── repository/                # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   └── JpaRegisteredClientRepository.java
│   ├── service/                   # 业务逻辑层
│   │   ├── UserService.java
│   │   ├── RoleService.java
│   │   ├── UserDetailsServiceImpl.java
│   │   └── AuditLogService.java
│   ├── annotation/                # 自定义注解
│   │   └── AuditLog.java                     # 审计日志注解
│   └── aspect/                    # AOP 切面
│       └── AuditLogAspect.java               # 审计日志切面
├── src/main/resources/
│   ├── templates/admin/           # 管理后台页面
│   │   ├── dashboard.html
│   │   ├── users.html
│   │   ├── roles.html
│   │   ├── clients.html
│   │   └── audit-logs.html
│   ├── application.yml            # 应用配置
│   └── schema.sql                 # 数据库初始化脚本
└── pom.xml                        # Maven 配置
```

---

## 📦 技术栈

### 后端框架

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Boot** | 3.2.3 | 核心框架 |
| **Spring Security** | 6.2.2 | 安全框架 |
| **Spring Authorization Server** | 1.2.2 | OAuth2 认证服务器 |
| **Spring Data JPA** | 3.2.3 | ORM 框架 |
| **Thymeleaf** | 3.1.2.RELEASE | 模板引擎 |

### 数据库

| 技术 | 版本 | 说明 |
|------|------|------|
| **MySQL** | 8.0+ | 关系型数据库 |
| **HikariCP** | 5.x | 连接池 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Bootstrap** | 5.3.0 | UI 框架 |
| **Font Awesome** | 6.4.0 | 图标库 |
| **jQuery** | 3.6.0 | JavaScript 库 |

### 开发工具

| 技术 | 说明 |
|------|------|
| **Lombok** | 简化 Java 代码 |
| **Maven** | 依赖管理 |
| **Git** | 版本控制 |

---

## 📖 使用指南

### 1. 用户管理

#### 创建用户
1. 访问 `/admin/users` 页面
2. 点击"新增用户"按钮
3. 填写用户名、密码、选择角色
4. 点击"保存"完成创建

#### 分配角色
1. 在用户列表中找到目标用户
2. 点击"编辑"或"权限分配"
3. 勾选所需角色
4. 点击"保存"生效

### 2. 角色管理

#### 创建角色
1. 访问 `/admin/roles` 页面
2. 点击"新增角色"
3. 输入角色名称（自动添加 ROLE_前缀）和描述
4. 点击"保存"

#### 配置 URL 权限
1. 进入角色详情页
2. 在"URL 权限规则"区域
3. 添加需要保护的 URL 模式（如 `/api/admin/**`）
4. 保存后该角色即可访问对应 URL

### 3. 客户端管理

#### 注册 OAuth2 客户端
1. 访问 `/admin/clients` 页面
2. 点击"创建客户端"
3. 填写客户端信息：
   - **客户端 ID**: 唯一标识符
   - **客户端名称**: 显示名称
   - **认证方式**: CLIENT_SECRET_BASIC / NONE
   - **授权模式**: authorization_code / password / client_credentials
   - **重定向 URI**: 回调地址
   - **Scope**: openid, profile, email 等
4. 点击"创建"生成客户端凭证

#### OAuth2 授权流程示例

**授权码模式：**

```bash
# 1. 获取授权码
GET http://localhost:9000/oauth2/authorize?
    client_id=YOUR_CLIENT_ID&
    redirect_uri=http://localhost:8080/callback&
    response_type=code&
    scope=openid,profile

# 2. 使用授权码换取 Token
POST http://localhost:9000/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
code=AUTH_CODE&
redirect_uri=http://localhost:8080/callback&
client_id=YOUR_CLIENT_ID&
client_secret=YOUR_CLIENT_SECRET
```

### 4. 审计日志

#### 查看操作记录
1. 访问 `/admin/audit-logs` 页面
2. 使用筛选条件：
   - 操作人：按用户名筛选
   - 操作类型：CREATE/UPDATE/DELETE/ASSIGN 等
   - 模块：USER/ROLE/CLIENT/PERMISSION
   - 结果：成功/失败
   - 日期范围：起始和结束日期
3. 查看详细的操作记录，包括 IP 地址、执行时间等

---

## 🔧 配置说明

### 核心配置项

#### application.yml

```yaml
# 服务器端口
server:
  port: 9000

# 数据源配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_server
    username: root
    password: your_password
    
  # OAuth2 令牌配置
  security:
    oauth2:
      authorizationserver:
        issuer: http://localhost:9000
        
# 日志配置
logging:
  level:
    org.springframework.security: INFO
```

### 自定义 Token 有效期

在创建客户端时设置：
- **Access Token TTL**: 访问令牌有效期（小时），默认 2 小时
- **Refresh Token TTL**: 刷新令牌有效期（天），默认 7 天

---

## 🛡️ 安全建议

1. **生产环境必须修改默认密码**
   - 默认管理员账号：admin / admin123
   
2. **启用 HTTPS**
   - 配置 SSL 证书
   - 强制 HTTPS 重定向

3. **定期备份数据库**
   - 建议每日自动备份
   - 保留最近 30 天的备份

4. **监控异常登录**
   - 关注审计日志中的失败记录
   - 设置告警阈值

5. **限制客户端权限**
   - 最小化 Scope 授权
   - 定期审查客户端配置

---

## 📝 API 文档

### OAuth2 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/oauth2/authorize` | GET | 授权请求端点 |
| `/oauth2/token` | POST | Token 获取端点 |
| `/oauth2/revoke` | POST | Token 撤销端点 |
| `/oauth2/introspect` | POST | Token 验证端点 |
| `/.well-known/openid-configuration` | GET | OIDC 配置发现 |
| `/oauth2/jwks.json` | GET | JWKS 公钥端点 |

### 管理 API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/admin/users` | GET | 用户列表 |
| `/admin/users/add` | POST | 创建用户 |
| `/admin/users/update` | POST | 更新用户 |
| `/admin/users/delete` | POST | 删除用户 |
| `/admin/roles` | GET | 角色列表 |
| `/admin/clients` | GET | 客户端列表 |
| `/admin/audit-logs` | GET | 审计日志 |

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 保持代码整洁和可读性
- 编写必要的注释

---

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源

```
Copyright (c) 2024-present

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```


---

## 🙏 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Authorization Server](https://spring.io/projects/spring-authorization-server)
- [Bootstrap](https://getbootstrap.com/)
- [Font Awesome](https://fontawesome.com/)

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐️ Star 支持！**

[⬆️ 返回顶部](#sso-认证中心---oauth2-authorization-server)

</div>
