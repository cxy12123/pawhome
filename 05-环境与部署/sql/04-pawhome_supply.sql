-- =============================================================
--  04 - 物资库存服务数据库 pawhome_supply
--  服务：pawhome-supply (8084)
--  职责：物资分类、物资台账、出入库流水
--  技术点：
--    1) 扣减库存使用 Redisson 分布式锁，防并发超卖
--    2) 出入库流水表记录 before/after 快照，便于对账
--    3) 库存低于预警值时触发通知服务
-- =============================================================
USE pawhome_supply;

-- -------------------------------------------------------------
-- 物资分类表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_supply_category;
CREATE TABLE t_supply_category (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(50) NOT NULL                COMMENT '分类名称',
    sort_no    INT         NOT NULL DEFAULT 0      COMMENT '排序号',
    create_time DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物资分类表';

-- -------------------------------------------------------------
-- 物资台账表
-- stock 为当前可用库存，所有变更必须走 t_supply_record 流水，禁止直接 UPDATE
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_supply_item;
CREATE TABLE t_supply_item (
    id            BIGINT       NOT NULL                COMMENT '主键',
    item_no       VARCHAR(32)  NOT NULL                COMMENT '物资编号',
    name          VARCHAR(100) NOT NULL                COMMENT '物资名称',
    category_id   BIGINT       NOT NULL                COMMENT '分类ID',
    category_name VARCHAR(50)           DEFAULT NULL   COMMENT '分类名称（冗余快照）',
    unit          VARCHAR(20)  NOT NULL DEFAULT '件'   COMMENT '计量单位',
    spec          VARCHAR(100)          DEFAULT NULL   COMMENT '规格型号',
    stock         INT          NOT NULL DEFAULT 0      COMMENT '当前库存',
    warn_stock    INT          NOT NULL DEFAULT 10     COMMENT '库存预警值',
    price         DECIMAL(10,2)         DEFAULT 0.00   COMMENT '参考单价',
    status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-启用 0-停用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_no (item_no),
    KEY idx_category (category_id),
    KEY idx_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物资台账表';

-- -------------------------------------------------------------
-- 出入库流水表
-- 只增不改，是库存的唯一可信来源（Event Sourcing 思路的轻量实现）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_supply_record;
CREATE TABLE t_supply_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    record_no     VARCHAR(32)  NOT NULL                COMMENT '流水号',
    item_id       BIGINT       NOT NULL                COMMENT '物资ID',
    item_name     VARCHAR(100)          DEFAULT NULL   COMMENT '物资名称（冗余快照）',
    type          VARCHAR(10)  NOT NULL                COMMENT '方向：IN-入库 OUT-出库',
    quantity      INT          NOT NULL                COMMENT '数量',
    before_stock  INT          NOT NULL DEFAULT 0      COMMENT '变更前库存',
    after_stock   INT          NOT NULL DEFAULT 0      COMMENT '变更后库存',
    source_type   VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '来源：DONATION-捐赠 PURCHASE-采购 ISSUE-领用 SCRAP-报损 MANUAL-手工调整',
    source_id     BIGINT                DEFAULT NULL   COMMENT '来源单据ID，如捐赠单ID',
    operator_id   BIGINT                DEFAULT NULL   COMMENT '操作人ID',
    operator_name VARCHAR(50)           DEFAULT NULL   COMMENT '操作人姓名',
    remark        VARCHAR(500)          DEFAULT NULL   COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_item_id (item_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物资出入库流水表';

-- -------------------------------------------------------------
-- 初始数据
-- -------------------------------------------------------------
INSERT INTO t_supply_category (id, name, sort_no) VALUES
(1, '主粮',   1),
(2, '零食',   2),
(3, '医疗用品', 3),
(4, '清洁用品', 4),
(5, '日用器具', 5);

INSERT INTO t_supply_item (id, item_no, name, category_id, category_name, unit, spec, stock, warn_stock, price) VALUES
(4001, 'SP202601001', '成犬狗粮',   1, '主粮',   '袋', '10kg/袋',  46,  20, 168.00),
(4002, 'SP202601002', '幼猫猫粮',   1, '主粮',   '袋', '2kg/袋',    8,  15,  98.00),
(4003, 'SP202601003', '猫砂',       4, '清洁用品','箱', '10L/箱',   35,  10,  45.00),
(4004, 'SP202601004', '狗罐头',     2, '零食',   '箱', '24罐/箱',  12,  10, 216.00),
(4005, 'SP202601005', '宠物消毒液', 4, '清洁用品','桶', '5L/桶',    19,   8,  68.00),
(4006, 'SP202601006', '体内驱虫药', 3, '医疗用品','盒', '10片/盒',   5,  12,  88.00),
(4007, 'SP202601007', '牵引绳',     5, '日用器具','条', '标准',     60,  20,  32.00),
(4008, 'SP202601008', '宠物航空箱', 5, '日用器具','个', '中号',     14,   5, 139.00),
(4009, 'SP202601009', '宠物尿垫',   4, '清洁用品','包', '50片/包',  72,  25,  39.00),
(4010, 'SP202601010', '猫爬架',     5, '日用器具','个', '三层',      3,   3, 259.00);

INSERT INTO t_supply_record
(record_no, item_id, item_name, type, quantity, before_stock, after_stock, source_type, source_id, operator_id, operator_name, remark) VALUES
('SR202601050001', 4001, '成犬狗粮', 'IN',  60, 0,  60, 'DONATION', 5001, 2, '李救助', '爱心企业捐赠'),
('SR202601050002', 4001, '成犬狗粮', 'OUT', 14, 60, 46, 'ISSUE',    NULL, 3, '王小愿', '一号犬舍日常投喂'),
('SR202601050003', 4002, '幼猫猫粮', 'IN',  20, 0,  20, 'PURCHASE', NULL, 2, '李救助', '季度采购'),
('SR202601050004', 4002, '幼猫猫粮', 'OUT', 12, 20, 8,  'ISSUE',    NULL, 3, '王小愿', '幼猫区投喂'),
('SR202601050005', 4003, '猫砂',     'IN',  40, 0,  40, 'DONATION', 5002, 2, '李救助', '市民捐赠'),
('SR202601050006', 4003, '猫砂',     'OUT', 5,  40, 35, 'ISSUE',    NULL, 3, '王小愿', '猫舍清理');
