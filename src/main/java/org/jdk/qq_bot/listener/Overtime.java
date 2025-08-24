package org.jdk.qq_bot.listener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupMessageEvent;
import love.forte.simbot.component.onebot.v11.message.segment.OneBotImage;
import love.forte.simbot.quantcat.common.annotations.ContentTrim;
import love.forte.simbot.quantcat.common.annotations.Filter;
import love.forte.simbot.quantcat.common.annotations.Listener;
import love.forte.simbot.quantcat.common.filter.MatchType;
import love.forte.simbot.resource.Resources;
import org.jdk.qq_bot.dto.OvertimeSummary;
import org.jdk.qq_bot.service.OvertimeService;
import org.jdk.qq_bot.service.echats.EchartsRenderClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Overtime {

  private final OvertimeService overtimeService;
  private final EchartsRenderClient echarts;
  /** 帮助文本（Java 17 文本块） */
  private static final String HELP_TEXT =
      """
🆘 Overtime 使用帮助

📥 录入今天加班
• overtime 2.5
  - 默认类型 G1，表示今天加班 2.5 小时
• overtime G2 1.0
  - 显式类型（仅 G1/G2/G3），大小写不敏感

🗓️ 设定某天（覆盖）
• overtime set YYMMDD 2.5
  - 例：overtime set 250824 2.5 → 将 2025-08-24 设为 G1 2.5 小时
• overtime set G2 YYMMDD 1.5
  - 例：overtime set G2 250824 1.5 → 将 2025-08-24 设为 G2 1.5 小时
  - 说明：set 为覆盖语义（先删该日记录，再插入指定类型与小时）

🧹 删除某天
• overtime del YYMMDD
  - 删除该日所有类型加班记录（若无记录则提示无操作）

📊 查询本月
• overtime query
  - 返回：本月合计、按类型小计（G1/G2/G3）、以及每日汇总

ℹ️ 规则说明
• 日期格式：YYMMDD（按 20YY 解析），如 250824 → 2025-08-24
• 小时数：支持最多两位小数（如 1.5、2.25），必须 > 0
• 类型仅限：G1 / G2 / G3（大小写均可）
• 同一天多次“录入（overtime …）”会累加；“set …”是覆盖
• 查询范围：按自然月统计（当地时区 Asia/Shanghai）

✅ 示例
overtime 2.5
overtime g2 1.0
overtime set 250824 2.5
overtime set G3 250901 3
overtime del 250824
overtime query
""";

  // —— 群聊：overtime help ——
  @Listener
  @ContentTrim
  @Filter(value = "(?i)^overtime\\s+help$", matchType = MatchType.REGEX_MATCHES)
  public void helpGroup(OneBotGroupMessageEvent event) {
    event.replyAsync(HELP_TEXT);
  }

  // ---------- 1) add（默认 G1）：overtime 2.5 ----------
  private static final Pattern ADD_DEFAULT_P =
      Pattern.compile("^overtime\\s+(\\d+(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE);

  @Listener
  @ContentTrim
  @Filter(value = "(?i)^overtime\\s+\\d+(?:\\.\\d{1,2})?$", matchType = MatchType.REGEX_MATCHES)
  public void addDefault(OneBotGroupMessageEvent event) {
    String text = event.getMessageContent().getPlainText().trim();
    Matcher m = ADD_DEFAULT_P.matcher(text);
    if (!m.matches()) return; // double check
    long uid = Long.parseLong(event.getUserId().toString());
    BigDecimal hours = new BigDecimal(m.group(1));
    try {
      overtimeService.addOvertime(uid, hours, "G1", null);
      event.replyAsync("✅ 已记录今天 G1 加班 " + fmt(hours) + " 小时");
    } catch (Exception e) {
      event.replyAsync("❌ 失败：" + e.getMessage());
    }
  }

  // ---------- 2) add（显式类型）：overtime G2 1.0 ----------
  private static final Pattern ADD_TYPED_P =
      Pattern.compile("^overtime\\s+(G[1-3])\\s+(\\d+(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE);

  @Listener
  @ContentTrim
  @Filter(
      value = "(?i)^overtime\\s+G[1-3]\\s+\\d+(?:\\.\\d{1,2})?$",
      matchType = MatchType.REGEX_MATCHES)
  public void addTyped(OneBotGroupMessageEvent event) {
    String text = event.getMessageContent().getPlainText().trim();
    Matcher m = ADD_TYPED_P.matcher(text);
    if (!m.matches()) return;
    long uid = Long.parseLong(event.getUserId().toString());
    String type = m.group(1).toUpperCase(Locale.ROOT);
    BigDecimal hours = new BigDecimal(m.group(2));
    try {
      overtimeService.addOvertime(uid, hours, type, null);
      event.replyAsync("✅ 已记录今天 " + type + " 加班 " + fmt(hours) + " 小时");
    } catch (Exception e) {
      event.replyAsync("❌ 失败：" + e.getMessage());
    }
  }

  // ---------- 3) set（支持两种：overtime set 250824 2.5 / overtime set G2 250824 1.5） ----------
  // 用宽过滤进入；方法内区分两种写法
  private static final Pattern SET_WITH_TYPE_P =
      Pattern.compile(
          "^overtime\\s+set\\s+(G[1-3])\\s+(\\d{6})\\s+(\\d+(?:\\.\\d{1,2})?)$",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern SET_NO_TYPE_P =
      Pattern.compile(
          "^overtime\\s+set\\s+(\\d{6})\\s+(\\d+(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE);

  @Listener
  @ContentTrim
  @Filter(value = "(?i)^overtime\\s+set\\b.*", matchType = MatchType.REGEX_MATCHES)
  public void setAtDate(OneBotGroupMessageEvent event) {
    String text = event.getMessageContent().getPlainText().trim();
    long uid = Long.parseLong(event.getUserId().toString());

    try {
      Matcher mt = SET_WITH_TYPE_P.matcher(text);
      if (mt.matches()) {
        String type = mt.group(1).toUpperCase(Locale.ROOT);
        LocalDate date = parseYyMmDd(mt.group(2));
        BigDecimal hours = new BigDecimal(mt.group(3));
        overtimeService.setOvertimeByDate(uid, date, hours, type, null);
        event.replyAsync("✅ 已将 " + date + " 的加班设为 " + type + " " + fmt(hours) + " 小时");
        return;
      }

      Matcher mn = SET_NO_TYPE_P.matcher(text);
      if (mn.matches()) {
        LocalDate date = parseYyMmDd(mn.group(1));
        BigDecimal hours = new BigDecimal(mn.group(2));
        overtimeService.setOvertimeByDate(uid, date, hours, "G1", null);
        event.replyAsync("✅ 已将 " + date + " 的加班设为 G1 " + fmt(hours) + " 小时");
        return;
      }

      event.replyAsync("❌ 用法：overtime set YYMMDD 2.5 或 overtime set G2 YYMMDD 1.5");
    } catch (Exception e) {
      event.replyAsync("❌ 失败：" + e.getMessage());
    }
  }

  // ---------- 4) del：overtime del 250824 ----------
  private static final Pattern DEL_P =
      Pattern.compile("^overtime\\s+del(?:ete)?\\s+(\\d{6})$", Pattern.CASE_INSENSITIVE);

  @Listener
  @ContentTrim
  @Filter(value = "(?i)^overtime\\s+del(?:ete)?\\s+\\d{6}$", matchType = MatchType.REGEX_MATCHES)
  public void deleteAtDate(OneBotGroupMessageEvent event) {
    String text = event.getMessageContent().getPlainText().trim();
    Matcher m = DEL_P.matcher(text);
    if (!m.matches()) return;
    long uid = Long.parseLong(event.getUserId().toString());
    LocalDate date = parseYyMmDd(m.group(1));
    try {
      int rows = overtimeService.deleteOvertimeByDate(uid, date);
      event.replyAsync(
          rows > 0
              ? ("🗑️ 已删除 " + date + " 的加班记录（" + rows + " 条）")
              : ("ℹ️ " + date + " 无加班记录，无需删除"));
    } catch (Exception e) {
      event.replyAsync("❌ 失败：" + e.getMessage());
    }
  }

  // ---------- 5) query：overtime query ----------
    /** 群聊：overtime query -> 文本 + 两张图 */
    @Listener
    @ContentTrim
    @Filter(value = "(?i)^overtime\\s+query$", matchType = MatchType.REGEX_MATCHES)
    public void queryWithCharts(OneBotGroupMessageEvent event) {
        long uid = Long.parseLong(event.getUserId().toString());

        try {
            // 1) 查询统计
            OvertimeSummary s = overtimeService.queryThisMonth(uid);

            String byType = "G1=" + fmt(s.getMonthByType().get("G1")) + "h, "
                    + "G2=" + fmt(s.getMonthByType().get("G2")) + "h, "
                    + "G3=" + fmt(s.getMonthByType().get("G3")) + "h";

            StringBuilder daily = new StringBuilder();
            if (s.getDailyTotals().isEmpty()) {
                daily.append("（本月暂无记录）");
            } else {
                for (Map.Entry<LocalDate, BigDecimal> e : s.getDailyTotals().entrySet()) {
                    daily.append(e.getKey().toString().substring(5)) // MM-dd
                            .append(" ").append(fmt(e.getValue())).append("h\n");
                }
            }

            String summary = "📊 本月合计：" + fmt(s.getMonthTotal()) + "h（" + byType + "）\n"
                    + "🗓️ 今天：" + fmt(s.getTodayTotal()) + "h\n"
                    + "—— 每日 ——\n" + daily;

            // 2) 生成图表（ECharts 渲染服务）
            Path barPng = echarts.renderMonthDailyBar(uid, s);  // 调用 EchartsOptionBuilder.dailyBarOption
            Path piePng = echarts.renderMonthTypePie(uid, s);   // 调用 EchartsOptionBuilder.typePieOption

            // 3) 先发文本，再发图（本地文件 -> base64）
            event.replyAsync(summary);

            OneBotImage.AdditionalParams params = new OneBotImage.AdditionalParams();
            params.setLocalFileToBase64(true); // 关键：把本地图片转 base64 发送

            var barImg = OneBotImage.create(Resources.valueOf(barPng.toFile()), params).toElement();
            var pieImg = OneBotImage.create(Resources.valueOf(piePng.toFile()), params).toElement();

            event.replyAsync(barImg);
            event.replyAsync(pieImg);

        } catch (Exception e) {
            event.replyAsync("⚠️ 生成图表失败：" + e.getMessage());
        }
    }

  // ---------- 工具 ----------
  private static LocalDate parseYyMmDd(String yymmdd) {
    if (yymmdd == null || !yymmdd.matches("\\d{6}")) {
      throw new IllegalArgumentException("日期格式应为 YYMMDD，例如 250824");
    }
    int yy = Integer.parseInt(yymmdd.substring(0, 2));
    int mm = Integer.parseInt(yymmdd.substring(2, 4));
    int dd = Integer.parseInt(yymmdd.substring(4, 6));
    int year = 2000 + yy;
    try {
      return LocalDate.of(year, mm, dd);
    } catch (DateTimeException ex) {
      throw new IllegalArgumentException("无效日期：" + yymmdd);
    }
  }

  private static String fmt(BigDecimal x) {
    if (x == null) return "0";
    return x.stripTrailingZeros().toPlainString();
  }
}
