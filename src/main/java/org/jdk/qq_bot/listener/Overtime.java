package org.jdk.qq_bot.listener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotFriendMessageEvent;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupMessageEvent;
import love.forte.simbot.quantcat.common.annotations.ContentTrim;
import love.forte.simbot.quantcat.common.annotations.Filter;
import love.forte.simbot.quantcat.common.annotations.FilterValue;
import love.forte.simbot.quantcat.common.annotations.Listener;
import love.forte.simbot.quantcat.common.filter.MatchType;
import org.jdk.qq_bot.dto.OvertimeSummary;
import org.jdk.qq_bot.service.OvertimeService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Overtime {
  private final OvertimeService overtimeService;

  /** 录入（显式类型）：overtime G2 1.0 */
  @Listener
  @ContentTrim
  @Filter(value = "^overtime\\s+{{type}}\\s+{{hours}}$", matchType = MatchType.REGEX_MATCHES)
  public void addOvertimeTypedGroup(
      OneBotGroupMessageEvent event,
      @FilterValue("type") String type,
      @FilterValue("hours") String hoursStr) {
    handleAdd(event.getUserId().getValue(), hoursStr, type, event);
  }

  /** 查询：overtime query */
  @Listener
  @ContentTrim
  @Filter(value = "^overtime\\s+query$", matchType = MatchType.REGEX_MATCHES)
  public void queryGroup(OneBotGroupMessageEvent event) {
    handleQuery(event.getUserId().getValue(), event);
  }

  // ================= 工具方法 =================

  private void handleAdd(long userId, String hoursStr, String type, Object evt) {
    System.out.println("handleAdd: " + userId + ", " + hoursStr + ", " + type);
    try {
      BigDecimal hours = new BigDecimal(hoursStr.trim());
      overtimeService.addOvertime(userId, hours, type, null);
      reply(
          evt,
          "✅ 已记录今天 "
              + type.toUpperCase()
              + " 加班 "
              + hours.stripTrailingZeros().toPlainString()
              + " 小时。");
    } catch (Exception e) {
      reply(evt, "❌ 用法：overtime 2.5（默认 G1），或 overtime G2 1.0（类型仅 G1/G2/G3）");
    }
  }

  private void handleQuery(long userId, Object evt) {
    OvertimeSummary s = overtimeService.queryThisMonth(userId);

    String byType =
        "G1="
            + fmt(s.getMonthByType().get("G1"))
            + "h, "
            + "G2="
            + fmt(s.getMonthByType().get("G2"))
            + "h, "
            + "G3="
            + fmt(s.getMonthByType().get("G3"))
            + "h";

    StringBuilder daily = new StringBuilder();
    if (s.getDailyTotals().isEmpty()) {
      daily.append("（本月暂无记录）");
    } else {
      for (Map.Entry<LocalDate, java.math.BigDecimal> e : s.getDailyTotals().entrySet()) {
        String d = e.getKey().toString().substring(5); // MM-dd
        daily
            .append(d)
            .append(" ")
            .append(e.getValue().stripTrailingZeros().toPlainString())
            .append("h\n");
      }
    }

    String msg =
        "📊 本月合计："
            + s.getMonthTotal().stripTrailingZeros().toPlainString()
            + "h"
            + "（"
            + byType
            + "）\n"
            + "🗓️ 今天："
            + s.getTodayTotal().stripTrailingZeros().toPlainString()
            + "h\n"
            + "—— 每日 ——\n"
            + daily;

    reply(evt, msg);
  }

  private static String fmt(BigDecimal x) {
    if (x == null) return "0";
    return x.stripTrailingZeros().toPlainString();
  }

  private void reply(Object evt, String text) {
    if (evt instanceof OneBotGroupMessageEvent e) e.replyAsync(text);
    else if (evt instanceof OneBotFriendMessageEvent e) e.replyAsync(text);
  }
}
