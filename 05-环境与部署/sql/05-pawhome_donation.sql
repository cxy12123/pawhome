-- =============================================================
--  05 - 捐赠服务数据库 pawhome_donation
--  服务：pawhome-donation (8085)
--  职责：捐赠登记、模拟支付、物资类捐赠转库存
--  核心跨服务链路（本实验重点讲解的分布式一致性问题）：
--    物资捐赠支付成功 -> Feign 调用 supply 服务入库
--    若入库失败 -> 记录补偿任务，由定时任务重试，保证最终一致
-- =============================================================
USE pawhome_donation;

-- -------------------------------------------------------------
-- 捐赠单表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_donation;
CREATE TABLE t_donation (
    id           BIGINT       NOT NULL                COMMENT '主键',
    donation_no  VARCHAR(32)  NOT NULL                COMMENT '捐赠单号 DN+日期+序号',
    donor_id     BIGINT                DEFAULT NULL   COMMENT '捐赠人用户ID，匿名捐赠可为空',
    donor_name   VARCHAR(50)  NOT NULL                COMMENT '捐赠人名称',
    donor_phone  VARCHAR(20)           DEFAULT NULL   COMMENT '联系电话',
    donor_type   VARCHAR(20)  NOT NULL DEFAULT 'PERSON' COMMENT '捐赠主体：PERSON-个人 COMPANY-企业',
    type         VARCHAR(20)  NOT NULL                COMMENT '捐赠类型：MONEY-资金 GOODS-物资',
    amount       DECIMAL(12,2)         DEFAULT 0.00   COMMENT '金额（MONEY 类型必填）',
    item_desc    VARCHAR(255)          DEFAULT NULL   COMMENT '物资描述（GOODS 类型必填）',
    supply_item_id BIGINT              DEFAULT NULL   COMMENT '对应物资ID，用于入库',
    quantity     INT          NOT NULL DEFAULT 0      COMMENT '物资数量',
    remark       VARCHAR(500)          DEFAULT NULL   COMMENT '备注',
    status       VARCHAR(20)  NOT NULL DEFAULT 'INIT' COMMENT '状态：INIT-待支付 PAID-已支付 STORED-已入库 FAILED-入库失败 CANCELED-已取消',
    pay_time     DATETIME              DEFAULT NULL   COMMENT '支付时间',
    store_time   DATETIME              DEFAULT NULL   COMMENT '入库完成时间',
    retry_count  INT          NOT NULL DEFAULT 0      COMMENT '入库补偿重试次数',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_donation_no (donation_no),
    KEY idx_status (status),
    KEY idx_donor (donor_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '捐赠单表';

-- -------------------------------------------------------------
-- 捐赠补偿任务表
-- 用于处理"本地事务已提交、远程调用失败"的场景，
-- 定时任务扫描 status=FAILED 且 retry_count<5 的记录重试
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_donation_compensation;
CREATE TABLE t_donation_compensation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    donation_id BIGINT       NOT NULL                COMMENT '捐赠单ID',
    donation_no VARCHAR(32)  NOT NULL                COMMENT '捐赠单号',
    action      VARCHAR(30)  NOT NULL                COMMENT '补偿动作：SUPPLY_IN-物资入库',
    status      VARCHAR(20)  NOT NULL DEFAULT 'WAITING' COMMENT '状态：WAITING-待处理 SUCCESS-成功 FAILED-已放弃',
    retry_count INT          NOT NULL DEFAULT 0      COMMENT '重试次数',
    last_error  VARCHAR(500)          DEFAULT NULL   COMMENT '最后一次失败原因',
    next_time   DATETIME              DEFAULT NULL   COMMENT '下次重试时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_next (status, next_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '捐赠补偿任务表';

-- -------------------------------------------------------------
-- 初始数据
-- -------------------------------------------------------------
INSERT INTO t_donation
(id, donation_no, donor_id, donor_name, donor_phone, donor_type, type, amount, item_desc, supply_item_id, quantity,
 remark, status, pay_time, store_time, create_time) VALUES
(5001, 'DN20260105001', NULL, '杭州喵汪宠物用品有限公司', '0571-88886666', 'COMPANY', 'GOODS', 0.00, '成犬狗粮 10kg/袋', 4001, 60,
 '年度公益合作第一批', 'STORED', '2026-01-05 09:30:00', '2026-01-05 09:31:12', '2026-01-05 09:28:00'),
(5002, 'DN20260106002', 4, '张领养', '13800000004', 'PERSON', 'GOODS', 0.00, '猫砂 10L/箱', 4003, 40,
 '家里多买的，捐给站里', 'STORED', '2026-01-06 14:10:00', '2026-01-06 14:11:05', '2026-01-06 14:08:00'),
(5003, 'DN20260108003', 4, '张领养', '13800000004', 'PERSON', 'MONEY', 500.00, NULL, NULL, 0,
 '用于猫舍冬季保暖改造', 'PAID', '2026-01-08 20:15:00', NULL, '2026-01-08 20:14:00'),
(5004, 'DN20260110004', NULL, '匿名爱心人士', NULL, 'PERSON', 'MONEY', 1000.00, NULL, NULL, 0,
 '匿名捐赠，愿毛孩子们安好', 'PAID', '2026-01-10 11:02:00', NULL, '2026-01-10 11:01:00');
