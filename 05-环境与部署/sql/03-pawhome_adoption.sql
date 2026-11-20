-- =============================================================
--  03 - 领养服务数据库 pawhome_adoption
--  服务：pawhome-adoption (8083)
--  职责：领养申请、审核、回访
--  核心跨服务调用：
--    1) 提交申请 -> Feign 调用 animal 服务校验动物是否可领养
--    2) 审核通过 -> Feign 调用 animal 服务把动物置为 ADOPTED
--    3) 审核通过 -> Feign 调用 notify 服务发站内信
-- =============================================================
USE pawhome_adoption;

-- -------------------------------------------------------------
-- 领养申请表
-- 冗余 animal_name / animal_no 是微服务架构下的"数据快照"设计：
-- 避免每次列表查询都跨服务调用 animal，牺牲少量存储换性能
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_adoption_apply;
CREATE TABLE t_adoption_apply (
    id               BIGINT       NOT NULL                COMMENT '主键',
    apply_no         VARCHAR(32)  NOT NULL                COMMENT '申请单号 AD+日期+序号',
    animal_id        BIGINT       NOT NULL                COMMENT '动物ID（逻辑外键，指向 animal 服务）',
    animal_no        VARCHAR(32)           DEFAULT NULL   COMMENT '动物编号（冗余快照）',
    animal_name      VARCHAR(50)           DEFAULT NULL   COMMENT '动物昵称（冗余快照）',
    applicant_id     BIGINT       NOT NULL                COMMENT '申请人用户ID',
    applicant_name   VARCHAR(50)  NOT NULL                COMMENT '申请人姓名',
    phone            VARCHAR(20)  NOT NULL                COMMENT '联系电话',
    city             VARCHAR(50)           DEFAULT NULL   COMMENT '所在城市',
    address          VARCHAR(255)          DEFAULT NULL   COMMENT '居住地址',
    occupation       VARCHAR(50)           DEFAULT NULL   COMMENT '职业',
    family_member    INT          NOT NULL DEFAULT 1      COMMENT '家庭成员数',
    has_pet_experience TINYINT    NOT NULL DEFAULT 0      COMMENT '是否有养宠经验',
    has_stable_income  TINYINT    NOT NULL DEFAULT 0      COMMENT '是否有稳定收入',
    reason           VARCHAR(1000)         DEFAULT NULL   COMMENT '领养理由',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核 PASSED-已通过 REJECTED-已驳回 CANCELED-已取消 FINISHED-已完成',
    audit_user_id    BIGINT                DEFAULT NULL   COMMENT '审核人ID',
    audit_user_name  VARCHAR(50)           DEFAULT NULL   COMMENT '审核人姓名',
    audit_time       DATETIME              DEFAULT NULL   COMMENT '审核时间',
    audit_remark     VARCHAR(500)          DEFAULT NULL   COMMENT '审核意见',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_apply_no (apply_no),
    KEY idx_animal_id (animal_id),
    KEY idx_applicant (applicant_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '领养申请表';

-- -------------------------------------------------------------
-- 领养回访表
-- 领养成功不代表流程结束，需要在 1/3/6 个月回访
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_adoption_visit;
CREATE TABLE t_adoption_visit (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_id      BIGINT       NOT NULL                COMMENT '关联申请ID',
    visit_no      INT          NOT NULL DEFAULT 1      COMMENT '第几次回访',
    visit_date    DATE         NOT NULL                COMMENT '回访日期',
    visitor       VARCHAR(50)           DEFAULT NULL   COMMENT '回访人',
    animal_health VARCHAR(20)           DEFAULT NULL   COMMENT '动物健康状况：GOOD-良好 NORMAL-一般 BAD-较差',
    living_env    VARCHAR(20)           DEFAULT NULL   COMMENT '居住环境：GOOD-良好 NORMAL-一般 BAD-较差',
    result        VARCHAR(20)  NOT NULL DEFAULT 'PASS' COMMENT '回访结论：PASS-通过 WARN-需关注 FAIL-不合格',
    remark        VARCHAR(500)          DEFAULT NULL   COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_apply_id (apply_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '领养回访表';

-- -------------------------------------------------------------
-- 初始数据
-- -------------------------------------------------------------
INSERT INTO t_adoption_apply
(id, apply_no, animal_id, animal_no, animal_name, applicant_id, applicant_name, phone, city, address,
 occupation, family_member, has_pet_experience, has_stable_income, reason, status,
 audit_user_id, audit_user_name, audit_time, audit_remark, create_time) VALUES
(3001, 'AD20260105001', 1003, 'PH20260101003', '土豆', 4, '张领养', '13800000004', '杭州', '西湖区文三路 88 号',
 '软件工程师', 3, 1, 1, '家里有院子，之前养过一只柯基，希望给它一个稳定的家。', 'PENDING', NULL, NULL, NULL, NULL, '2026-01-05 10:12:00'),
(3002, 'AD20260106002', 1010, 'PH20260101010', '布丁', 4, '张领养', '13800000004', '杭州', '西湖区文三路 88 号',
 '软件工程师', 3, 1, 1, '孩子喜欢猫，家中已封窗，可长期饲养。', 'PENDING', NULL, NULL, NULL, NULL, '2026-01-06 15:30:00'),
(3003, 'AD20251220003', 1001, 'PH20260101001', '豆豆', 4, '张领养', '13800000004', '杭州', '西湖区文三路 88 号',
 '软件工程师', 3, 1, 1, '希望领养一只中华田园犬看家。', 'REJECTED', 2, '李救助', '2025-12-22 09:20:00', '该犬需要较大活动空间，建议先到站互动。', '2025-12-20 18:00:00');

INSERT INTO t_adoption_visit (apply_id, visit_no, visit_date, visitor, animal_health, living_env, result, remark) VALUES
(3003, 1, '2025-12-28', '王小愿', 'GOOD', 'NORMAL', 'WARN', '申请人居住为高层公寓，建议重新评估。');
