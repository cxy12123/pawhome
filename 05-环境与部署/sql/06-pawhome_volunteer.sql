-- =============================================================
--  06 - 志愿服务数据库 pawhome_volunteer
--  服务：pawhome-volunteer (8086)
--  职责：志愿者档案、排班、打卡、服务时长统计
-- =============================================================
USE pawhome_volunteer;

-- -------------------------------------------------------------
-- 志愿者档案表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_volunteer;
CREATE TABLE t_volunteer (
    id          BIGINT       NOT NULL                COMMENT '主键',
    user_id     BIGINT                DEFAULT NULL   COMMENT '关联用户ID（auth 服务）',
    name        VARCHAR(50)  NOT NULL                COMMENT '姓名',
    phone       VARCHAR(20)  NOT NULL                COMMENT '手机号',
    gender      VARCHAR(10)           DEFAULT 'M'    COMMENT '性别',
    skills      VARCHAR(255)          DEFAULT NULL   COMMENT '技能标签，逗号分隔，如"摄影,医疗"',
    join_date   DATE                  DEFAULT NULL   COMMENT '加入日期',
    total_hours DECIMAL(8,1) NOT NULL DEFAULT 0.0    COMMENT '累计服务时长(小时)',
    level       VARCHAR(20)  NOT NULL DEFAULT 'ROOKIE' COMMENT '等级：ROOKIE-新手 ACTIVE-活跃 SENIOR-资深',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-在册 0-退出',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '志愿者档案表';

-- -------------------------------------------------------------
-- 排班表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_volunteer_shift;
CREATE TABLE t_volunteer_shift (
    id             BIGINT      NOT NULL                COMMENT '主键',
    volunteer_id   BIGINT      NOT NULL                COMMENT '志愿者ID',
    volunteer_name VARCHAR(50)          DEFAULT NULL   COMMENT '志愿者姓名（冗余快照）',
    shift_date     DATE        NOT NULL                COMMENT '排班日期',
    start_time     VARCHAR(10) NOT NULL                COMMENT '开始时间 HH:mm',
    end_time       VARCHAR(10) NOT NULL                COMMENT '结束时间 HH:mm',
    work_type      VARCHAR(20) NOT NULL DEFAULT 'FEED' COMMENT '工作类型：FEED-投喂 CLEAN-清洁 ESCORT-陪护 MEDICAL-医疗协助 EVENT-活动',
    status         VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED-已排班 FINISHED-已完成 CANCELED-已取消',
    remark         VARCHAR(255)         DEFAULT NULL   COMMENT '备注',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_volunteer (volunteer_id),
    KEY idx_shift_date (shift_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '志愿者排班表';

-- -------------------------------------------------------------
-- 打卡记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_volunteer_checkin;
CREATE TABLE t_volunteer_checkin (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    shift_id      BIGINT       NOT NULL                COMMENT '排班ID',
    volunteer_id  BIGINT       NOT NULL                COMMENT '志愿者ID',
    checkin_time  DATETIME              DEFAULT NULL   COMMENT '签到时间',
    checkout_time DATETIME              DEFAULT NULL   COMMENT '签退时间',
    hours         DECIMAL(5,1) NOT NULL DEFAULT 0.0    COMMENT '本次服务时长',
    remark        VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_shift (shift_id),
    KEY idx_volunteer (volunteer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '志愿者打卡记录表';

-- -------------------------------------------------------------
-- 初始数据
-- -------------------------------------------------------------
INSERT INTO t_volunteer (id, user_id, name, phone, gender, skills, join_date, total_hours, level) VALUES
(6001, 3, '王小愿', '13800000003', 'F', '摄影,文案',   '2025-03-01', 128.5, 'SENIOR'),
(6002, 5, '赵帮忙', '13800000005', 'M', '驾驶,搬运',   '2025-06-12',  64.0, 'ACTIVE'),
(6003, 6, '孙照护', '13800000006', 'F', '宠物医疗',    '2025-09-20',  32.0, 'ACTIVE'),
(6004, NULL, '周新来', '13800000007', 'M', '水电维修',  '2026-01-02',   4.0, 'ROOKIE');

INSERT INTO t_volunteer_shift (id, volunteer_id, volunteer_name, shift_date, start_time, end_time, work_type, status) VALUES
(7001, 6001, '王小愿', '2026-01-12', '09:00', '12:00', 'FEED',   'FINISHED'),
(7002, 6002, '赵帮忙', '2026-01-12', '14:00', '18:00', 'CLEAN',  'PLANNED'),
(7003, 6003, '孙照护', '2026-01-13', '09:00', '17:00', 'MEDICAL','PLANNED'),
(7004, 6001, '王小愿', '2026-01-13', '13:00', '17:00', 'ESCORT', 'PLANNED');

INSERT INTO t_volunteer_checkin (shift_id, volunteer_id, checkin_time, checkout_time, hours, remark) VALUES
(7001, 6001, '2026-01-12 08:56:00', '2026-01-12 12:05:00', 3.0, '完成犬舍投喂与记录');
