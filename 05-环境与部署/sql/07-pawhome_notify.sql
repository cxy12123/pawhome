-- =============================================================
--  07 - 通知服务数据库 pawhome_notify
--  服务：pawhome-notify (8087)
--  职责：站内信、消息模板、已读状态
--  说明：本实验用站内信模拟短信/邮件通道，
--        真实场景可替换为短信网关或邮件服务器，接口保持不变
-- =============================================================
USE pawhome_notify;

-- -------------------------------------------------------------
-- 消息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_notify_message;
CREATE TABLE t_notify_message (
    id            BIGINT       NOT NULL                COMMENT '主键',
    receiver_id   BIGINT       NOT NULL                COMMENT '接收人用户ID',
    receiver_name VARCHAR(50)           DEFAULT NULL   COMMENT '接收人姓名（冗余快照）',
    title         VARCHAR(100) NOT NULL                COMMENT '消息标题',
    content       VARCHAR(1000) NOT NULL               COMMENT '消息内容',
    type          VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：SYSTEM-系统 ADOPTION-领养 SUPPLY-物资 DONATION-捐赠 VOLUNTEER-志愿',
    biz_type      VARCHAR(30)           DEFAULT NULL   COMMENT '业务类型，用于前端跳转',
    biz_id        BIGINT                DEFAULT NULL   COMMENT '业务单据ID',
    channel       VARCHAR(20)  NOT NULL DEFAULT 'INNER' COMMENT '通道：INNER-站内信 SMS-短信(模拟) EMAIL-邮件(模拟)',
    read_flag     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：1-已读 0-未读',
    read_time     DATETIME              DEFAULT NULL   COMMENT '阅读时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_receiver (receiver_id, read_flag),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知消息表';

-- -------------------------------------------------------------
-- 消息模板表
-- 支持占位符渲染，如 "您的领养申请 {applyNo} 已通过"
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_notify_template;
CREATE TABLE t_notify_template (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL                COMMENT '模板编码',
    name        VARCHAR(100) NOT NULL                COMMENT '模板名称',
    type        VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型',
    title_tpl   VARCHAR(200) NOT NULL                COMMENT '标题模板',
    content_tpl VARCHAR(1000) NOT NULL               COMMENT '内容模板',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知模板表';

-- -------------------------------------------------------------
-- 初始数据
-- -------------------------------------------------------------
INSERT INTO t_notify_template (code, name, type, title_tpl, content_tpl) VALUES
('ADOPTION_PASSED', '领养审核通过', 'ADOPTION', '领养申请已通过', '您好 {name}，您对「{animalName}」的领养申请（{applyNo}）已通过审核，请在 7 日内到救助站办理交接手续。'),
('ADOPTION_REJECTED','领养审核驳回', 'ADOPTION', '领养申请未通过', '您好 {name}，很抱歉，您对「{animalName}」的领养申请未通过，原因：{remark}。欢迎关注其他待领养动物。'),
('ADOPTION_SUBMITTED','领养申请已提交','ADOPTION','领养申请已提交', '您好 {name}，您对「{animalName}」的领养申请（{applyNo}）已提交，我们将在 3 个工作日内完成审核。'),
('SUPPLY_WARN',     '物资库存预警',   'SUPPLY',   '物资库存不足提醒', '物资「{itemName}」当前库存 {stock} {unit}，已低于预警值 {warnStock}，请及时补充。'),
('DONATION_STORED', '捐赠物资已入库', 'DONATION', '感谢您的捐赠', '您好 {name}，您捐赠的「{itemDesc}」×{quantity} 已入库，感谢您对流浪动物的关爱。'),
('VOLUNTEER_SHIFT', '志愿排班提醒',   'VOLUNTEER','新的排班安排', '您好 {name}，您有一条新的排班：{shiftDate} {startTime}-{endTime}，工作内容：{workType}。');

INSERT INTO t_notify_message (id, receiver_id, receiver_name, title, content, type, biz_type, biz_id, read_flag, create_time) VALUES
(8001, 4, '张领养', '领养申请已提交', '您好 张领养，您对「土豆」的领养申请（AD20260105001）已提交，我们将在 3 个工作日内完成审核。', 'ADOPTION', 'ADOPTION_APPLY', 3001, 0, '2026-01-05 10:12:05'),
(8002, 4, '张领养', '领养申请未通过', '您好 张领养，很抱歉，您对「豆豆」的领养申请未通过，原因：该犬需要较大活动空间，建议先到站互动。欢迎关注其他待领养动物。', 'ADOPTION', 'ADOPTION_APPLY', 3003, 1, '2025-12-22 09:20:10'),
(8003, 2, '李救助', '物资库存不足提醒', '物资「体内驱虫药」当前库存 5 盒，已低于预警值 12，请及时补充。', 'SUPPLY', 'SUPPLY_ITEM', 4006, 0, '2026-01-09 08:00:00');
