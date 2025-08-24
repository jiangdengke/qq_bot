// src/main/java/org/jdk/qq_bot/repository/OvertimeLogRepository.java
package org.jdk.qq_bot.repository;

import static org.jooq.generated.tables.OvertimeLog.OVERTIME_LOG;
import static org.jooq.impl.DSL.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** 加班记录数据访问（写入 + 各类聚合查询） */
@Repository
@RequiredArgsConstructor
public class OvertimeLogRepository {
  private final DSLContext dsl;

  public void insert(long userId, LocalDate workDate, String type, BigDecimal hours, String note) {
    dsl.insertInto(OVERTIME_LOG)
        .set(OVERTIME_LOG.USER_ID, userId)
        .set(OVERTIME_LOG.WORK_DATE, workDate) // jOOQ 列为 LocalDate 映射
        .set(OVERTIME_LOG.OT_TYPE, type)
        .set(OVERTIME_LOG.HOURS, hours)
        .set(OVERTIME_LOG.NOTE, note)
        .execute();
  }

  public BigDecimal monthTotal(long userId, LocalDate start, LocalDate end) {
    return dsl.select(coalesce(sum(OVERTIME_LOG.HOURS), inline(BigDecimal.ZERO)))
        .from(OVERTIME_LOG)
        .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.between(start, end)))
        .fetchOne(0, BigDecimal.class);
  }

  public BigDecimal dayTotal(long userId, LocalDate day) {
    return dsl.select(coalesce(sum(OVERTIME_LOG.HOURS), inline(BigDecimal.ZERO)))
        .from(OVERTIME_LOG)
        .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.eq(day)))
        .fetchOne(0, BigDecimal.class);
  }

  public Map<String, BigDecimal> monthTotalsByType(long userId, LocalDate start, LocalDate end) {
    var out = new LinkedHashMap<String, BigDecimal>();
    dsl.select(OVERTIME_LOG.OT_TYPE, sum(OVERTIME_LOG.HOURS))
        .from(OVERTIME_LOG)
        .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.between(start, end)))
        .groupBy(OVERTIME_LOG.OT_TYPE)
        .fetchStream()
        .forEach(r -> out.put(r.get(OVERTIME_LOG.OT_TYPE), r.get(1, BigDecimal.class)));
    return out;
  }

  public Map<String, BigDecimal> dayTotalsByType(long userId, LocalDate day) {
    var out = new LinkedHashMap<String, BigDecimal>();
    dsl.select(OVERTIME_LOG.OT_TYPE, sum(OVERTIME_LOG.HOURS))
        .from(OVERTIME_LOG)
        .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.eq(day)))
        .groupBy(OVERTIME_LOG.OT_TYPE)
        .fetchStream()
        .forEach(r -> out.put(r.get(OVERTIME_LOG.OT_TYPE), r.get(1, BigDecimal.class)));
    return out;
  }

  public LinkedHashMap<LocalDate, BigDecimal> monthDailyTotals(
      long userId, LocalDate start, LocalDate end) {
    var out = new LinkedHashMap<LocalDate, BigDecimal>();
    dsl.select(OVERTIME_LOG.WORK_DATE, sum(OVERTIME_LOG.HOURS))
        .from(OVERTIME_LOG)
        .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.between(start, end)))
        .groupBy(OVERTIME_LOG.WORK_DATE)
        .orderBy(OVERTIME_LOG.WORK_DATE.asc())
        .fetchStream()
        .forEach(r -> out.put(r.get(OVERTIME_LOG.WORK_DATE), r.get(1, BigDecimal.class)));
    return out;
  }
}
