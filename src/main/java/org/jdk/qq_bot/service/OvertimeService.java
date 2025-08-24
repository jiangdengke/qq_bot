package org.jdk.qq_bot.service;

import static org.jooq.generated.tables.OvertimeLog.OVERTIME_LOG;
import static org.jooq.impl.DSL.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jdk.qq_bot.dto.OvertimeSummary;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * 加班记录服务（支持类型 G1/G2/G3；不指定时默认 G1） 设计要点： - 使用 Asia/Shanghai 取“今天”，避免服务器时区差异造成日期错位 - 插入用事务；查询只读 - 聚合用
 * SUM，空集用 COALESCE 转 0
 */
@Service
@RequiredArgsConstructor
public class OvertimeService {

  private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
  private static final Set<String> VALID_TYPES = Set.of("G1", "G2", "G3");

  private final DSLContext dsl;

  /**
   * 记录今天的加班（带类型）
   *
   * @param userId QQ号
   * @param hours 小时数（>0）
   * @param type G1/G2/G3（大小写不敏感）
   * @param note 备注，可为 null
   */
  public void addOvertime(long userId, BigDecimal hours, String type, String note) {
    if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("hours must be > 0");
    }
    // 默认类型：G1
    String t = (type == null || type.isBlank()) ? "G1" : type.toUpperCase(Locale.ROOT);
    if (!VALID_TYPES.contains(t)) {
      throw new IllegalArgumentException("type must be one of G1/G2/G3");
    }
    if (note != null && note.length() > 255) {
      note = note.substring(0, 255);
    }

    LocalDate today = LocalDate.now(CN);
    dsl.insertInto(OVERTIME_LOG)
        .set(OVERTIME_LOG.USER_ID, userId)
        .set(OVERTIME_LOG.WORK_DATE, today)
        .set(OVERTIME_LOG.HOURS, hours)
        .set(OVERTIME_LOG.OT_TYPE, t)
        .set(OVERTIME_LOG.NOTE, note)
        .execute();
  }

  /** 查询本月统计：整体合计、分类型小计、每日总和 */
  public OvertimeSummary queryThisMonth(long userId) {
    LocalDate today = LocalDate.now(CN);
    YearMonth ym = YearMonth.from(today);
    LocalDate start = ym.atDay(1);
    LocalDate end = ym.atEndOfMonth();

    // 整体合计（本月 / 今日）
    BigDecimal monthTotal =
        dsl.select(coalesce(sum(OVERTIME_LOG.HOURS), inline(BigDecimal.ZERO)))
            .from(OVERTIME_LOG)
            .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.between(start, end)))
            .fetchOne(0, BigDecimal.class);
    BigDecimal todayTotal =
        dsl.select(coalesce(sum(OVERTIME_LOG.HOURS), inline(BigDecimal.ZERO)))
            .from(OVERTIME_LOG)
            .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.eq(today)))
            .fetchOne(0, BigDecimal.class);

    // 分类型合计（本月）
    Map<String, BigDecimal> monthByType =
        dsl.select(OVERTIME_LOG.OT_TYPE, sum(OVERTIME_LOG.HOURS))
            .from(OVERTIME_LOG)
            .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.between(start, end)))
            .groupBy(OVERTIME_LOG.OT_TYPE)
            .fetchStream()
            .collect(
                Collectors.toMap(
                    r -> r.get(OVERTIME_LOG.OT_TYPE), r -> r.get(1, BigDecimal.class)));
    VALID_TYPES.forEach(t -> monthByType.putIfAbsent(t, BigDecimal.ZERO));

    // 分类型合计（今日）
    Map<String, BigDecimal> todayByType =
        dsl.select(OVERTIME_LOG.OT_TYPE, sum(OVERTIME_LOG.HOURS))
            .from(OVERTIME_LOG)
            .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.eq(today)))
            .groupBy(OVERTIME_LOG.OT_TYPE)
            .fetchStream()
            .collect(
                Collectors.toMap(
                    r -> r.get(OVERTIME_LOG.OT_TYPE), r -> r.get(1, BigDecimal.class)));
    VALID_TYPES.forEach(t -> todayByType.putIfAbsent(t, BigDecimal.ZERO));

    // 每日总和（当月）
    Map<LocalDate, BigDecimal> dailyTotals =
        dsl.select(OVERTIME_LOG.WORK_DATE, sum(OVERTIME_LOG.HOURS))
            .from(OVERTIME_LOG)
            .where(OVERTIME_LOG.USER_ID.eq(userId).and(OVERTIME_LOG.WORK_DATE.between(start, end)))
            .groupBy(OVERTIME_LOG.WORK_DATE)
            .orderBy(OVERTIME_LOG.WORK_DATE.asc())
            .fetchStream()
            .collect(
                Collectors.toMap(
                    r -> r.get(OVERTIME_LOG.WORK_DATE),
                    r -> r.get(1, BigDecimal.class),
                    (a, b) -> a,
                    LinkedHashMap::new // 保持日期升序
                    ));

    if (monthTotal == null) monthTotal = BigDecimal.ZERO;
    if (todayTotal == null) todayTotal = BigDecimal.ZERO;

    return new OvertimeSummary(monthTotal, todayTotal, monthByType, todayByType, dailyTotals);
  }
}
