CREATE TABLE IF NOT EXISTS city_code (
    adcode   CHAR(6)     NOT NULL PRIMARY KEY,
    name_zh  VARCHAR(64) NOT NULL,
    citycode VARCHAR(16) NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 加班记录表
-- 加班明细表（带类型，默认 G1）
CREATE TABLE IF NOT EXISTS overtime_log (
                                            id          BIGINT       NOT NULL AUTO_INCREMENT,
                                            user_id     BIGINT       NOT NULL COMMENT 'QQ号',
                                            work_date   DATE         NOT NULL COMMENT '加班归属日期',
                                            ot_type     VARCHAR(2)   NOT NULL DEFAULT 'G1' COMMENT '加班类型: G1/G2/G3',
    hours       DECIMAL(5,2) NOT NULL COMMENT '加班小时，如 2.50',
    note        VARCHAR(255) NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 索引用独立语句（DDLDatabase 解析更稳妥）
CREATE INDEX idx_user_date ON overtime_log (user_id, work_date);
CREATE INDEX idx_user_type ON overtime_log (user_id, ot_type);