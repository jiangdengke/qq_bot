package org.jdk.qq_bot.service.echats;

import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdk.qq_bot.dto.OvertimeSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class EchartsRenderClient {

  @Value("${echarts.render.base-url}")
  private String baseUrl;

  @Value("${echarts.render.out-dir:run/charts}")
  private String outDir;

  private static final ObjectMapper M = new ObjectMapper();

    public Path renderMonthDailyBar(long userId, OvertimeSummary s) throws Exception {
        log.info("[ECharts] user={} render daily-bar start", userId);
        var option = EchartsOptionBuilder.dailyBarOption(s);
        Path p = renderToPng(option, 1000, 380, "#FCFCFF", "month-daily-" + userId + ".png");
        log.info("[ECharts] user={} render daily-bar done -> {}", userId, p);
        return p;
    }

    public Path renderMonthTypePie(long userId, OvertimeSummary s) throws Exception {
        log.info("[ECharts] user={} render type-pie start", userId);
        var option = EchartsOptionBuilder.typePieOption(s);
        Path p = renderToPng(option, 560, 380, "#FCFCFF", "month-type-" + userId + ".png");
        log.info("[ECharts] user={} render type-pie done -> {}", userId, p);
        return p;
    }

    private Path renderToPng(Map<String, Object> option, int w, int h, String bg, String fileName) throws Exception {
        String url = baseUrl + "/render";

        // 构造请求体
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("option", option);
        payload.put("width", w);
        payload.put("height", h);
        payload.put("backgroundColor", bg);
        String body = M.writeValueAsString(payload);

        StopWatch sw = new StopWatch("echarts-" + fileName);
        log.info("[ECharts] POST {} ({}x{}, bg={}), payload={}B", url, w, h, bg, body.length());

        // 请求渲染
        sw.start("http");
        var resp = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(10_000)
                .execute();
        sw.stop();

        int status = resp.getStatus();
        byte[] png = resp.bodyBytes();
        int bytes = png == null ? -1 : png.length;
        log.info("[ECharts] response status={} bytes={}", status, bytes);
        if (status != 200 || png == null || png.length == 0) {
            throw new IllegalStateException("Renderer HTTP " + status + " -> " + resp.body());
        }

        // 落盘
        sw.start("writeFile");
        Path out = Paths.get(outDir, fileName);
        Files.createDirectories(out.getParent());
        Files.write(out, png);
        sw.stop();

        log.info("[ECharts] saved -> {} ({} bytes)\n{}", out.toAbsolutePath(), bytes, sw.prettyPrint());
        return out.toAbsolutePath();
    }
}
