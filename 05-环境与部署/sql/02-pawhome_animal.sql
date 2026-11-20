-- =============================================================
--  02 - 动物档案服务数据库 pawhome_animal
--  服务：pawhome-animal (8082)
--  职责：动物建档、健康状态、诊疗记录、图片
-- =============================================================
USE pawhome_animal;

-- -------------------------------------------------------------
-- 动物档案表
-- adopt_status 是领养服务跨服务关注的核心字段，
-- 领养通过后由领养服务通过 Feign 回调本服务更新为 ADOPTED
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_animal;
CREATE TABLE t_animal (
    id            BIGINT       NOT NULL                COMMENT '主键，雪花算法生成',
    animal_no     VARCHAR(32)  NOT NULL                COMMENT '动物编号，如 PH20260101001',
    name          VARCHAR(50)  NOT NULL                COMMENT '昵称',
    species       VARCHAR(20)  NOT NULL                COMMENT '物种：DOG-犬 CAT-猫 OTHER-其他',
    breed         VARCHAR(50)           DEFAULT NULL   COMMENT '品种',
    gender        VARCHAR(10)  NOT NULL DEFAULT 'M'    COMMENT '性别：M-公 F-母',
    age_month     INT          NOT NULL DEFAULT 0      COMMENT '月龄',
    weight        DECIMAL(6,2)          DEFAULT NULL   COMMENT '体重(kg)',
    health_status VARCHAR(20)  NOT NULL DEFAULT 'HEALTHY' COMMENT '健康状态：HEALTHY-健康 TREATING-治疗中 RECOVERING-康复中 DISABLED-残疾',
    adopt_status  VARCHAR(20)  NOT NULL DEFAULT 'WAITING' COMMENT '领养状态：WAITING-待领养 APPLYING-申请中 ADOPTED-已领养 OBSERVING-观察期',
    rescue_date   DATE                  DEFAULT NULL   COMMENT '救助日期',
    rescue_city   VARCHAR(50)           DEFAULT NULL   COMMENT '救助城市',
    rescue_source VARCHAR(100)          DEFAULT NULL   COMMENT '救助来源，如"路边救助""市民送养"',
    station_id    BIGINT       NOT NULL DEFAULT 1      COMMENT '所属救助站ID',
    sterilized    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否绝育：1-是 0-否',
    vaccinated    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已接种疫苗：1-是 0-否',
    dewormed      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已驱虫：1-是 0-否',
    description   VARCHAR(1000)         DEFAULT NULL   COMMENT '性格与情况描述',
    cover_image   VARCHAR(255)          DEFAULT NULL   COMMENT '封面图',
    view_count    INT          NOT NULL DEFAULT 0      COMMENT '被查看次数',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_animal_no (animal_no),
    KEY idx_species (species),
    KEY idx_adopt_status (adopt_status),
    KEY idx_station (station_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '动物档案表';

-- -------------------------------------------------------------
-- 诊疗记录表
-- 一次救助可能产生多次疫苗/治疗记录，形成 1:N
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_animal_medical;
CREATE TABLE t_animal_medical (
    id          BIGINT       NOT NULL                COMMENT '主键',
    animal_id   BIGINT       NOT NULL                COMMENT '动物ID',
    record_type VARCHAR(20)  NOT NULL                COMMENT '类型：VACCINE-疫苗 TREATMENT-治疗 STERILIZE-绝育 CHECK-体检',
    title       VARCHAR(100) NOT NULL                COMMENT '记录标题',
    vet_name    VARCHAR(50)           DEFAULT NULL   COMMENT '执行兽医',
    cost        DECIMAL(10,2)         DEFAULT 0.00   COMMENT '花费金额',
    record_date DATE         NOT NULL                COMMENT '记录日期',
    remark      VARCHAR(500)          DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_animal_id (animal_id),
    KEY idx_record_date (record_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '动物诊疗记录表';

-- -------------------------------------------------------------
-- 动物图片表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_animal_image;
CREATE TABLE t_animal_image (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    animal_id BIGINT       NOT NULL                COMMENT '动物ID',
    url       VARCHAR(255) NOT NULL                COMMENT '图片地址',
    sort_no   INT          NOT NULL DEFAULT 0      COMMENT '排序号',
    PRIMARY KEY (id),
    KEY idx_animal_id (animal_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '动物图片表';

-- -------------------------------------------------------------
-- 初始数据：12 只待领养动物
-- -------------------------------------------------------------
INSERT INTO t_animal
(id, animal_no, name, species, breed, gender, age_month, weight, health_status, adopt_status,
 rescue_date, rescue_city, rescue_source, station_id, sterilized, vaccinated, dewormed, description, view_count) VALUES
(1001, 'PH20260101001', '豆豆',  'DOG', '中华田园犬', 'M', 14,  8.60, 'HEALTHY',  'WAITING',  '2025-11-03', '杭州', '路边救助',   1, 1, 1, 1, '性格亲人，会握手，适合有院子的家庭。', 126),
(1002, 'PH20260101002', '花卷',  'CAT', '狸花猫',     'F', 9,   3.20, 'HEALTHY',  'WAITING',  '2025-11-15', '杭州', '市民送养',   1, 1, 1, 1, '安静黏人，会用猫砂盆。',             203),
(1003, 'PH20260101003', '土豆',  'DOG', '柯基串',     'M', 26, 11.40, 'RECOVERING','APPLYING', '2025-10-21', '宁波', '城管移交',   1, 0, 1, 1, '后腿曾有轻微伤，已基本康复。',       98),
(1004, 'PH20260101004', '奶糖',  'CAT', '布偶混血',   'F', 6,   2.10, 'HEALTHY',  'WAITING',  '2025-12-02', '杭州', '遗弃救助',   1, 0, 1, 1, '活泼好动，喜欢逗猫棒。',             311),
(1005, 'PH20260101005', '黑豹',  'DOG', '拉布拉多',   'M', 38, 26.50, 'HEALTHY',  'WAITING',  '2025-09-18', '绍兴', '繁殖场解救', 1, 1, 1, 1, '体型较大，需要有一定养犬经验。',     152),
(1006, 'PH20260101006', '小橘',  'CAT', '中华田园猫', 'M', 12,  4.00, 'TREATING', 'WAITING',  '2025-12-11', '杭州', '路边救助',   1, 0, 0, 1, '目前在治疗猫鼻支，暂不适合领养。',    76),
(1007, 'PH20260101007', '雪球',  'DOG', '萨摩耶',     'F', 30, 18.20, 'HEALTHY',  'ADOPTED',  '2025-08-05', '嘉兴', '主人遗弃',   1, 1, 1, 1, '已于 2026-01 被领养。',             420),
(1008, 'PH20260101008', '芝麻',  'CAT', '英短蓝猫',   'M', 20,  4.80, 'HEALTHY',  'WAITING',  '2025-11-27', '杭州', '市民送养',   1, 1, 1, 1, '性格独立，不吵不闹。',               189),
(1009, 'PH20260101009', '阿黄',  'DOG', '中华田园犬', 'M', 48, 15.00, 'DISABLED', 'WAITING',  '2025-07-14', '湖州', '车祸救助',   1, 1, 1, 1, '左前肢截肢，行动无碍，需要耐心陪伴。', 267),
(1010, 'PH20260101010', '布丁',  'CAT', '三花猫',     'F', 15,  3.60, 'HEALTHY',  'APPLYING', '2025-12-19', '杭州', '楼道救助',   1, 1, 1, 1, '亲人不怕生，喜欢被抱。',             134),
(1011, 'PH20260101011', '大壮',  'DOG', '金毛',       'M', 42, 30.10, 'HEALTHY',  'WAITING',  '2025-06-30', '杭州', '主人去世',   1, 1, 1, 1, '温和稳重，与其他狗相处融洽。',       355),
(1012, 'PH20260101012', '点点',  'DOG', '泰迪',       'F', 18,  5.40, 'HEALTHY',  'WAITING',  '2025-12-25', '杭州', '繁殖场解救', 1, 1, 1, 1, '小型犬，适合公寓饲养。',             212);

-- 封面图统一指向项目内置的本地图片（static/images/animals/{id}.jpg），
-- 由前端 <el-image> 直接加载，避免依赖外网图床
UPDATE t_animal SET cover_image = CONCAT('/images/animals/', id, '.jpg');

INSERT INTO t_animal_medical (id, animal_id, record_type, title, vet_name, cost, record_date, remark) VALUES
(2001, 1001, 'VACCINE',   '狂犬疫苗第一针', '陈医生', 120.00, '2025-11-05', '无不良反应'),
(2002, 1001, 'STERILIZE', '绝育手术',       '陈医生', 680.00, '2025-11-12', '术后恢复良好'),
(2003, 1003, 'TREATMENT', '后腿软组织修复', '刘医生', 950.00, '2025-10-22', '住院观察 5 天'),
(2004, 1006, 'TREATMENT', '猫鼻支治疗',     '刘医生', 430.00, '2025-12-12', '需持续用药两周'),
(2005, 1009, 'TREATMENT', '截肢手术',       '陈医生', 2200.00,'2025-07-16', '术后恢复良好，可正常行走'),
(2006, 1011, 'CHECK',     '全面体检',       '王医生', 260.00, '2025-07-02', '各项指标正常');

INSERT INTO t_animal_image (animal_id, url, sort_no) VALUES
(1001, '/images/animals/1001.jpg', 1),
(1002, '/images/animals/1002.jpg', 1),
(1004, '/images/animals/1004.jpg', 1),
(1005, '/images/animals/1005.jpg', 1);
