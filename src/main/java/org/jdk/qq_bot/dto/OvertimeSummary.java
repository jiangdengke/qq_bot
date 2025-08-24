package org.jdk.qq_bot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加班统计： - monthTotal：当月总计 - todayTotal：今日总计 - monthByType：当月分类型小计（G1/G2/G3） -
 * todayByType：今日分类型小计（G1/G2/G3） - dailyTotals：当月每日累计（总和，按日期升序）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeSummary {
  private BigDecimal monthTotal;
  private BigDecimal todayTotal;
  private Map<String, BigDecimal> monthByType = new LinkedHashMap<>();
  private Map<String, BigDecimal> todayByType = new LinkedHashMap<>();
  private Map<LocalDate, BigDecimal> dailyTotals = new LinkedHashMap<>();
}
