// src/main/java/org/jdk/qq_bot/service/CityCodeService.java
package org.jdk.qq_bot.service;

import static org.jooq.generated.Tables.CITY_CODE;

import lombok.RequiredArgsConstructor;
import org.jdk.qq_bot.dto.CodesResponse;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityCodeService {
  private final DSLContext dsl;

  /** 根据中文名精确查找，返回 adCode 与 cityCode；找不到返回 null */
  public CodesResponse getCodesByName(String nameZh) {
    Record2<String, String> record =
        dsl.select(CITY_CODE.ADCODE, CITY_CODE.CITYCODE)
            .from(CITY_CODE)
            .where(CITY_CODE.NAME_ZH.like("%" + nameZh + "%"))
            .fetchOne();
    if (record == null) {
      return null;
    }
    return new CodesResponse(record.value1(), record.value2());
  }
}
