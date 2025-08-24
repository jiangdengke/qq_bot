package org.jdk.qq_bot.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupMessageEvent;
import love.forte.simbot.quantcat.common.annotations.ContentTrim;
import love.forte.simbot.quantcat.common.annotations.Filter;
import love.forte.simbot.quantcat.common.annotations.FilterValue;
import love.forte.simbot.quantcat.common.annotations.Listener;
import love.forte.simbot.quantcat.common.filter.MatchType;
import org.jdk.qq_bot.dto.CodesResponse;
import org.jdk.qq_bot.service.CityCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Handles {
  private final CityCodeService cityCodeService;

  @Value("${fy.youDao.url}")
  private String youDaoUrl;

  /** 处理翻译 */
  @Listener
  @ContentTrim
  @Filter(value = "fy{{word}}", matchType = MatchType.REGEX_MATCHES)
  public void fyGroupMessage(OneBotGroupMessageEvent event, @FilterValue("word") String word) {
    String base = StrUtil.emptyToDefault(youDaoUrl, "http://localhost:8000/");
    String url = (StrUtil.endWith(base, "/") ? base : base + "/") + "define";
    try (HttpResponse resp =
        HttpRequest.get(url)
            .header(Header.ACCEPT, "application/json")
            .form("word", word) // 自动编码为 ?word=...
            .timeout(10_000)
            .execute()) {

      if (!resp.isOk()) {
        event.replyAsync("查询失败：" + resp.getStatus());
        return;
      }

      JSONObject root = JSONUtil.parseObj(resp.body());
      String text = root.getStr("text", "");

      // 兜底：如果没有拼好的 text，就把 definitions 拼接成文本
      if (StrUtil.isBlank(text)) {
        JSONArray defs = root.getJSONArray("definitions");
        if (defs != null) {
          StringBuilder sb = new StringBuilder();
          for (Object o : defs) {
            if (!(o instanceof JSONObject def)) continue;
            String pos = def.getStr("pos", "");
            String tran = def.getStr("tran", "");
            if (StrUtil.isNotBlank(tran)) {
              if (StrUtil.isNotBlank(pos)) sb.append(pos).append('\n');
              sb.append(tran).append('\n');
            }
          }
          text = StrUtil.trim(sb);
        }
      }

      log.info("翻译结果: " + text);
      event.replyAsync(StrUtil.blankToDefault(text, "没查到释义~"));

    } catch (Exception e) {
      event.replyAsync("查询异常：" + e.getMessage());
    }
  }

  @Listener
  @ContentTrim
  @Filter(value = "query{{name}}", matchType = MatchType.REGEX_MATCHES)
  public void codes(OneBotGroupMessageEvent event, @FilterValue("name") String name) {
    log.info("cityCode and adCode 查询: " + name);
    CodesResponse codesByName = cityCodeService.getCodesByName(name);
    if (codesByName != null) {
      event.replyAsync(
          "查询结果: \n"
              + "adCode: "
              + codesByName.adCode()
              + "\n"
              + "cityCode: "
              + codesByName.cityCode());
    } else {
      event.replyAsync("未找到相关信息");
    }
  }
}
