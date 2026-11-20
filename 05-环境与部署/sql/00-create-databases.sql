-- =============================================================
--  PawHome 城市流浪动物救助站领养与物资管理平台
--  00 - 数据库创建脚本（Database per Service）
--  适用版本：MySQL 5.7.x
--  执行方式：mysql -uroot -p < 00-create-databases.sql
-- =============================================================

-- -------------------------------------------------------------
-- 微服务架构下遵循 "每个服务独享数据库" 原则：
--   pawhome_auth     -> 认证授权服务，只管用户与角色
--   pawhome_animal   -> 动物档案服务，只管动物与诊疗记录
--   pawhome_adoption -> 领养服务，只管申请与回访
--   pawhome_supply   -> 物资库存服务，只管物资与出入库流水
--   pawhome_donation -> 捐赠服务，只管捐赠单
--   pawhome_volunteer-> 志愿服务，只管志愿者与排班打卡
--   pawhome_notify   -> 通知服务，只管消息
-- 服务之间不允许跨库 JOIN，需要对方数据一律走 Feign 调用，
-- 这是本实验最核心的架构约束。
-- -------------------------------------------------------------

DROP DATABASE IF EXISTS pawhome_auth;
CREATE DATABASE pawhome_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP DATABASE IF EXISTS pawhome_animal;
CREATE DATABASE pawhome_animal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP DATABASE IF EXISTS pawhome_adoption;
CREATE DATABASE pawhome_adoption DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP DATABASE IF EXISTS pawhome_supply;
CREATE DATABASE pawhome_supply DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP DATABASE IF EXISTS pawhome_donation;
CREATE DATABASE pawhome_donation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP DATABASE IF EXISTS pawhome_volunteer;
CREATE DATABASE pawhome_volunteer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP DATABASE IF EXISTS pawhome_notify;
CREATE DATABASE pawhome_notify DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

SELECT
    SCHEMA_NAME        AS '已创建数据库',
    DEFAULT_CHARACTER_SET_NAME AS '字符集',
    DEFAULT_COLLATION_NAME     AS '排序规则'
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME LIKE 'pawhome%'
ORDER BY SCHEMA_NAME;
