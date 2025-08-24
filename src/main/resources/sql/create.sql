CREATE TABLE IF NOT EXISTS city_code (
    adcode   CHAR(6)     NOT NULL PRIMARY KEY,
    name_zh  VARCHAR(64) NOT NULL,
    citycode VARCHAR(16) NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
