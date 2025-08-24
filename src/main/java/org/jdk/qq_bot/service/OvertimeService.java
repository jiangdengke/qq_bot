// src/main/java/org/jdk/qq_bot/service/OvertimeService.java
package org.jdk.qq_bot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jdk.qq_bot.dto.OvertimeSummary;
import org.jdk.qq_bot.repository.OvertimeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OvertimeService {

  private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
  private static final Set<String> VALID_TYPES = Set.of("G1", "G2", "G3");

  private final OvertimeLogRepository repo;

  /** 记录今天的加班（默认 G1；会做基本参数校验与备注截断） */
  @Transactional
  public void addOvertime(long userId, BigDecimal hours, String type, String note) {
    if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("hours must be > 0");
    }
    String t = (type == null || type.isBlank()) ? "G1" : type.toUpperCase(Locale.ROOT);
    if (!VALID_TYPES.contains(t)) {
      throw new IllegalArgumentException("type must be one of G1/G2/G3");
    }
    if (note != null && note.length() > 255) {
      note = note.substring(0, 255);
    }
    repo.insert(userId, LocalDate.now(CN), t, hours, note);
  }

  /** 兼容：不传 type 则默认 G1 */
  @Transactional
  public void addOvertime(long userId, BigDecimal hours, String note) {
    addOvertime(userId, hours, "G1", note);
  }

  /** 查询本月统计：整体合计、分类型小计、每日总和（按日期升序） */
  @Transactional(readOnly = true)
  public OvertimeSummary queryThisMonth(long userId) {
    LocalDate today = LocalDate.now(CN);
    YearMonth ym = YearMonth.from(today);
    LocalDate start = ym.atDay(1), end = ym.atEndOfMonth();

    BigDecimal monthTotal = repo.monthTotal(userId, start, end);
    BigDecimal todayTotal = repo.dayTotal(userId, today);

    Map<String, BigDecimal> monthByType =
        new LinkedHashMap<>(repo.monthTotalsByType(userId, start, end));
    Map<String, BigDecimal> todayByType = new LinkedHashMap<>(repo.dayTotalsByType(userId, today));

    // 补齐缺失类型为 0，便于上层直接取值
    VALID_TYPES.forEach(t -> monthByType.putIfAbsent(t, BigDecimal.ZERO));
    VALID_TYPES.forEach(t -> todayByType.putIfAbsent(t, BigDecimal.ZERO));

    var dailyTotals = repo.monthDailyTotals(userId, start, end);

    if (monthTotal == null) monthTotal = BigDecimal.ZERO;
    if (todayTotal == null) todayTotal = BigDecimal.ZERO;

    return new OvertimeSummary(monthTotal, todayTotal, monthByType, todayByType, dailyTotals);
  }
}
