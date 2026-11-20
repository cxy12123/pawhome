-- =============================================================
--  01 - 认证授权服务数据库 pawhome_auth
--  服务：pawhome-auth (8081)
--  职责：用户账号、角色、登录日志
--  注意：MySQL 5.7 兼容写法
-- =============================================================
USE pawhome_auth;

-- -------------------------------------------------------------
-- 用户表
-- 密码使用 BCrypt 加密存储，禁止明文
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id              BIGINT       NOT NULL                COMMENT '主键，雪花算法生成',
    username        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    real_name       VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
    phone           VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
    email           VARCHAR(100)          DEFAULT NULL   COMMENT '邮箱',
    avatar          VARCHAR(255)          DEFAULT NULL   COMMENT '头像地址',
    role            VARCHAR(20)  NOT NULL DEFAULT 'ADOPTER' COMMENT '角色：ADMIN-管理员 STAFF-救助站员工 VOLUNTEER-志愿者 ADOPTER-领养人',
    station_id      BIGINT                DEFAULT NULL   COMMENT '所属救助站ID',
    status          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-启用 0-停用',
    last_login_time DATETIME              DEFAULT NULL   COMMENT '最后登录时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone),
    KEY idx_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- -------------------------------------------------------------
-- 登录日志表
-- 用于支撑"异地登录告警"和"审计"场景
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_login_log;
CREATE TABLE t_login_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT                DEFAULT NULL   COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    login_ip    VARCHAR(64)           DEFAULT NULL   COMMENT '登录IP',
    user_agent  VARCHAR(500)          DEFAULT NULL   COMMENT '浏览器标识',
    success     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否成功：1-成功 0-失败',
    fail_reason VARCHAR(200)          DEFAULT NULL   COMMENT '失败原因',
    login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_login_time (login_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志表';

-- -------------------------------------------------------------
-- 初始账号数据
--
-- 密码字段采用 "密码编码器前缀" 约定（参考 Spring Security 的
-- DelegatingPasswordEncoder 设计）：
--   {noop}123456  -> 明文，服务首次启动时会被 DataInitializer
--                    自动替换为 BCrypt 密文，之后库里再无明文
--   {bcrypt}$2a$... -> 已是 BCrypt 密文，直接使用
--
-- 这样做的原因：SQL 脚本里写死的 BCrypt 密文无法在文档中验证，
-- 一旦写错就永远登录不上；用前缀标记 + 启动自动升级，既保证
-- 初始数据一定能用，又不违反"密码不得明文存储"的要求。
-- -------------------------------------------------------------
INSERT INTO t_user (id, username, password, real_name, phone, email, role, station_id, status) VALUES
(1, 'admin',   '{noop}123456', '系统管理员', '13800000001', 'admin@pawhome.org',   'ADMIN',     1, 1),
(2, 'staff01', '{noop}123456', '李救助',     '13800000002', 'staff01@pawhome.org', 'STAFF',     1, 1),
(3, 'vol01',   '{noop}123456', '王小愿',     '13800000003', 'vol01@pawhome.org',   'VOLUNTEER', 1, 1),
(4, 'user01',  '{noop}123456', '张领养',     '13800000004', 'user01@pawhome.org',  'ADOPTER',   1, 1);
