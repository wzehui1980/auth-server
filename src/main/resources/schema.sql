-- ========================================
-- OAuth2 授权服务器数据库初始化脚本
-- ========================================
-- 说明：仅用于首次启动时的数据库初始化
-- 注意：请勿在生产环境中使用 DROP TABLE 语句
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id varchar(100) NOT NULL,                                    -- 用户唯一标识
    username varchar(50) NOT NULL,                               -- 用户名（登录名）
    password varchar(500) NOT NULL,                              -- 密码（BCrypt 加密）
    enabled boolean NOT NULL,                                    -- 是否启用
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,              -- 创建时间
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间
    PRIMARY KEY (id),
    UNIQUE INDEX ix_users_username (username)
);



-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id varchar(100) NOT NULL,                                    -- 角色唯一标识
    name varchar(50) NOT NULL,                                   -- 角色名称（如：ROLE_USER）
    description varchar(255) DEFAULT NULL,                       -- 角色描述
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,              -- 创建时间
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间
    PRIMARY KEY (id),
    UNIQUE INDEX ix_roles_name (name)
);

-- 用户 - 角色关联表（多对多）
CREATE TABLE IF NOT EXISTS user_roles (
    user_id varchar(100) NOT NULL,                               -- 用户 ID
    role_id varchar(100) NOT NULL,                               -- 角色 ID
    PRIMARY KEY (user_id, role_id),                              -- 联合主键
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, -- 外键约束：用户
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE  -- 外键约束：角色
);

-- URL 权限规则表（动态配置 URL 访问权限）
CREATE TABLE IF NOT EXISTS url_permissions (
    id varchar(100) NOT NULL,                                    -- 权限规则唯一标识
    url_pattern varchar(500) NOT NULL,                           -- URL 路径模式（支持通配符，如：/admin/**）
    http_method varchar(20) NOT NULL DEFAULT '*',                -- HTTP 方法（GET/POST/PUT/DELETE/*）
    required_role varchar(100) NOT NULL,                         -- 所需角色（如：ROLE_ADMIN）
    description varchar(255) DEFAULT NULL,                       -- 规则描述
    enabled boolean NOT NULL DEFAULT true,                       -- 是否启用
    priority int NOT NULL DEFAULT 0,                             -- 优先级（数字越大优先级越高，用于匹配冲突时）
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,              -- 创建时间
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间
    PRIMARY KEY (id),
    INDEX ix_url_pattern (url_pattern),                          -- URL 模式索引
    INDEX ix_enabled (enabled)                                   -- 启用状态索引
);



-- OAuth2 注册客户端表
-- 字段设计遵循 Spring Authorization Server 标准规范
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id varchar(100) NOT NULL,                                    -- 客户端唯一标识
    client_id varchar(100) NOT NULL,                             -- 客户端 ID（用于 OAuth2 协议）
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP,     -- 客户端 ID 创建时间
    client_secret varchar(500) DEFAULT NULL,                     -- 客户端密钥（BCrypt 加密，公开客户端可为 NULL）
    client_secret_expires_at timestamp DEFAULT NULL,             -- 密钥过期时间（NULL 表示永不过期）
    client_name varchar(200) NOT NULL,                           -- 客户端名称
    client_authentication_methods varchar(1000) NOT NULL,        -- 客户端认证方式（逗号分隔，如：client_secret_basic,client_secret_post）
    authorization_grant_types varchar(1000) NOT NULL,            -- 授权类型（逗号分隔，如：authorization_code,refresh_token）
    redirect_uris varchar(1000) DEFAULT NULL,                    -- 重定向 URI 列表（逗号分隔，可为 NULL）
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,        -- 登出后重定向 URI 列表（逗号分隔，可为 NULL）
    scopes varchar(1000) NOT NULL,                               -- 权限范围（逗号分隔，如：openid,profile,email）
    require_authorization_consent boolean NOT NULL DEFAULT false, -- 是否需要授权同意页面
    require_proof_key boolean NOT NULL DEFAULT false,            -- 是否需要 PKCE（Proof Key for Code Exchange）
    access_token_time_to_live int NOT NULL DEFAULT 7200,         -- Access Token 有效期（秒），默认 2 小时
    refresh_token_time_to_live int NOT NULL DEFAULT 604800,      -- Refresh Token 有效期（秒），默认 7 天
    reuse_refresh_tokens boolean NOT NULL DEFAULT false,         -- 是否重复使用 Refresh Token
    PRIMARY KEY (id),
    UNIQUE INDEX ix_oauth2_client_id (client_id)                 -- 客户端 ID 唯一索引
);

-- OAuth2 授权表（存储授权码、访问令牌、刷新令牌等）
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id varchar(100) NOT NULL,                                    -- 授权记录唯一标识
    registered_client_id varchar(100) NOT NULL,                  -- 注册的客户端 ID
    principal_name varchar(200) NOT NULL,                        -- 主体名称（用户名）
    authorization_aspect_type varchar(100) NOT NULL,             -- 授权方面类型
    authorization_grant_type varchar(100) NOT NULL,              -- 授权类型（如：authorization_code）
    authorized_scopes varchar(1000) DEFAULT NULL,                -- 授权的权限范围（逗号分隔）
    attributes blob DEFAULT NULL,                                -- 授权属性（JSON 格式）
    state varchar(500) DEFAULT NULL,                             -- 授权状态（用于防止 CSRF）
    
    -- 授权码相关字段
    authorization_code_value blob DEFAULT NULL,                  -- 授权码值
    authorization_code_issued_at timestamp DEFAULT NULL,         -- 授权码签发时间
    authorization_code_expires_at timestamp DEFAULT NULL,        -- 授权码过期时间
    authorization_code_metadata blob DEFAULT NULL,               -- 授权码元数据
    
    -- 访问令牌相关字段
    access_token_value blob DEFAULT NULL,                        -- 访问令牌值
    access_token_issued_at timestamp DEFAULT NULL,               -- 访问令牌签发时间
    access_token_expires_at timestamp DEFAULT NULL,              -- 访问令牌过期时间
    access_token_metadata blob DEFAULT NULL,                     -- 访问令牌元数据
    access_token_type varchar(100) DEFAULT NULL,                 -- 访问令牌类型（如：Bearer）
    access_token_scopes varchar(1000) DEFAULT NULL,              -- 访问令牌的权限范围
    
    -- OIDC ID Token 相关字段
    oidc_id_token_value blob DEFAULT NULL,                       -- OIDC ID Token 值
    oidc_id_token_issued_at timestamp DEFAULT NULL,              -- OIDC ID Token 签发时间
    oidc_id_token_expires_at timestamp DEFAULT NULL,             -- OIDC ID Token 过期时间
    oidc_id_token_metadata blob DEFAULT NULL,                    -- OIDC ID Token 元数据
    
    -- 刷新令牌相关字段
    refresh_token_value blob DEFAULT NULL,                       -- 刷新令牌值
    refresh_token_issued_at timestamp DEFAULT NULL,              -- 刷新令牌签发时间
    refresh_token_expires_at timestamp DEFAULT NULL,             -- 刷新令牌过期时间
    refresh_token_metadata blob DEFAULT NULL,                    -- 刷新令牌元数据
    
    -- 用户码相关字段
    user_code_value blob DEFAULT NULL,                           -- 用户码值
    user_code_issued_at timestamp DEFAULT NULL,                  -- 用户码签发时间
    user_code_expires_at timestamp DEFAULT NULL,                 -- 用户码过期时间
    user_code_metadata blob DEFAULT NULL,                        -- 用户码元数据
    
    -- 设备码相关字段
    device_code_value blob DEFAULT NULL,                         -- 设备码值
    device_code_issued_at timestamp DEFAULT NULL,                -- 设备码签发时间
    device_code_expires_at timestamp DEFAULT NULL,               -- 设备码过期时间
    device_code_metadata blob DEFAULT NULL,                      -- 设备码元数据
    
    PRIMARY KEY (id)                                             -- 主键
);

-- OAuth2 授权同意表（记录用户的授权同意）
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,                  -- 注册的客户端 ID
    principal_name varchar(200) NOT NULL,                        -- 主体名称（用户名）
    authorities varchar(1000) NOT NULL,                          -- 授权的权限列表（逗号分隔）
    PRIMARY KEY (registered_client_id, principal_name)           -- 联合主键
);

-- ========================================
-- 初始化数据（仅首次执行）
-- 注意：用户表初始化由 DataInitializerConfig.java 负责
-- ========================================

-- 插入默认角色（如果不存在）
INSERT IGNORE INTO roles (id, name, description)
VALUES 
    ('role-001', 'ROLE_USER', '普通用户'),
    ('role-002', 'ROLE_ADMIN', '系统管理员');

-- 插入默认 URL 权限规则（如果不存在）
INSERT IGNORE INTO url_permissions (id, url_pattern, http_method, required_role, description, enabled, priority)
VALUES 
    -- 公开访问路径
    ('perm-001', '/', '*', 'ROLE_USER', '首页访问权限', true, 0),
    ('perm-002', '/css/**', '*', 'ROLE_USER', 'CSS 资源', true, 0),
    ('perm-003', '/js/**', '*', 'ROLE_USER', 'JS 资源', true, 0),
    ('perm-004', '/images/**', '*', 'ROLE_USER', '图片资源', true, 0),
    ('perm-005', '/login', 'GET', 'ROLE_USER', '登录页面', true, 0),
    ('perm-006', '/oauth2/**', '*', 'ROLE_USER', 'OAuth2 端点', true, 0),
    
    -- 管理员专用路径
    ('perm-007', '/admin/**', '*', 'ROLE_ADMIN', '管理员后台所有路径', true, 10),
    ('perm-008', '/api/admin/**', '*', 'ROLE_ADMIN', '管理员 API', true, 10);

-- ========================================
-- 审计日志表
-- ========================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id varchar(100) NOT NULL,                                    -- 日志唯一标识
    operator varchar(50),                                        -- 操作人用户名
    operation_type varchar(30) NOT NULL,                         -- 操作类型（CREATE/UPDATE/DELETE/LOGIN/LOGOUT 等）
    module varchar(30) NOT NULL,                                 -- 操作模块（USER/ROLE/CLIENT/PERMISSION/AUTH）
    description varchar(500),                                    -- 操作描述
    request_uri varchar(500),                                    -- 请求路径
    request_method varchar(10),                                  -- HTTP 方法（GET/POST/PUT/DELETE）
    ip_address varchar(50),                                      -- 客户端 IP 地址
    method_name varchar(200),                                    -- 被调用的方法全限定名
    params text,                                                 -- 方法参数（JSON 格式）
    result varchar(20) NOT NULL,                                 -- 操作结果（SUCCESS/FAILURE）
    error_message varchar(1000),                                 -- 异常信息（失败时）
    execution_time bigint,                                       -- 方法执行耗时（毫秒）
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,              -- 创建时间
    PRIMARY KEY (id),
    INDEX ix_audit_operator (operator),
    INDEX ix_audit_operation_type (operation_type),
    INDEX ix_audit_module (module),
    INDEX ix_audit_created_at (created_at),
    INDEX ix_audit_result (result)
);
