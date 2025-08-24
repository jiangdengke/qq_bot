package org.jdk.qq_bot.service.echats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.jdk.qq_bot.dto.OvertimeSummary;

public final class EchartsOptionBuilder {
  private EchartsOptionBuilder() {}

  /** 每日总小时 柱状图（圆角淡彩） */
  public static Map<String, Object> dailyBarOption(OvertimeSummary s) {
    List<String> labels = new ArrayList<>();
    List<Double> data = new ArrayList<>();
    s.getDailyTotals()
        .forEach(
            (LocalDate d, BigDecimal h) -> {
              labels.add(d.toString().substring(5)); // MM-dd
              data.add(h == null ? 0d : h.doubleValue());
            });

    Map<String, Object> series = new LinkedHashMap<>();
    series.put("type", "bar");
    series.put("data", data);
    series.put("barWidth", 16);
    series.put(
        "itemStyle",
        Map.of(
            "borderRadius",
            List.of(10, 10, 0, 0),
            "shadowBlur",
            8,
            "shadowColor",
            "rgba(100,140,255,0.30)",
            "color",
            "rgba(142,178,255,0.75)"));
    series.put("emphasis", Map.of("itemStyle", Map.of("color", "rgba(142,178,255,0.95)")));

    Map<String, Object> option = new LinkedHashMap<>();
    option.put("backgroundColor", "transparent");
    option.put(
        "title",
        Map.of(
            "text", "本月每日加班（小时）",
            "left", "center",
            "textStyle", Map.of("color", "#556", "fontSize", 18)));
    option.put("grid", Map.of("left", 50, "right", 20, "top", 50, "bottom", 40));
    option.put(
        "xAxis",
        Map.of(
            "type", "category",
            "data", labels,
            "axisTick", Map.of("show", false),
            "axisLine", Map.of("lineStyle", Map.of("color", "#ccd")),
            "axisLabel", Map.of("color", "#556")));
    option.put(
        "yAxis",
        Map.of(
            "type",
            "value",
            "min",
            0,
            "splitLine",
            Map.of("lineStyle", Map.of("color", "rgba(0,0,0,0.06)")),
            "axisLabel",
            Map.of("color", "#556")));
    option.put("series", List.of(series));
    return option;
  }

  /** 本月类型占比 饼图（淡彩阴影） */
  public static Map<String, Object> typePieOption(OvertimeSummary s) {
    double g1 = s.getMonthByType().getOrDefault("G1", BigDecimal.ZERO).doubleValue();
    double g2 = s.getMonthByType().getOrDefault("G2", BigDecimal.ZERO).doubleValue();
    double g3 = s.getMonthByType().getOrDefault("G3", BigDecimal.ZERO).doubleValue();
    List<Map<String, Object>> data = new ArrayList<>();
    if (g1 + g2 + g3 == 0) {
      data.add(Map.of("name", "无记录", "value", 1));
    } else {
      data.add(Map.of("name", "G1", "value", g1));
      data.add(Map.of("name", "G2", "value", g2));
      data.add(Map.of("name", "G3", "value", g3));
    }

    Map<String, Object> series = new LinkedHashMap<>();
    series.put("type", "pie");
    series.put("radius", List.of("35%", "65%"));
    series.put("center", List.of("50%", "55%"));
    series.put("avoidLabelOverlap", true);
    series.put(
        "itemStyle",
        Map.of(
            "borderRadius", 8,
            "shadowBlur", 12,
            "shadowColor", "rgba(0,0,0,0.15)"));
    series.put(
        "label",
        Map.of(
            "formatter", "{b}: {d}%",
            "color", "#445"));
    series.put("data", data);

    Map<String, Object> option = new LinkedHashMap<>();
    option.put(
        "title",
        Map.of(
            "text", "本月加班类型占比",
            "left", "center",
            "textStyle", Map.of("color", "#556", "fontSize", 18)));
    option.put("color", List.of("#91CC75", "#FAC858", "#5470C6")); // 柔和配色
    option.put("series", List.of(series));
    option.put("tooltip", Map.of("trigger", "item"));
    option.put("legend", Map.of("bottom", 5, "textStyle", Map.of("color", "#556")));
    return option;
  }
}
