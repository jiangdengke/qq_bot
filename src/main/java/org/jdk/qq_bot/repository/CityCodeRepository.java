package org.jdk.qq_bot.repository;

import static org.jooq.generated.tables.CityCode.CITY_CODE;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

/** 城市编码数据访问 */
@Repository
@RequiredArgsConstructor
public class CityCodeRepository {
  private final DSLContext dsl;

  /** 模糊匹配中文名，取命中的第一条（可按需改排序策略） */
  public Record2<String, String> findTopByNameLike(String nameZh) {
    return dsl.select(CITY_CODE.ADCODE, CITY_CODE.CITYCODE)
        .from(CITY_CODE)
        .where(CITY_CODE.NAME_ZH.like("%" + nameZh + "%"))
        .limit(1)
        .fetchOne();
  }
}
