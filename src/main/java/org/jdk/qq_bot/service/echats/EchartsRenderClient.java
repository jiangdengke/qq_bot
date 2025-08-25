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

/**
 * 负责把 ECharts option 发到你 Node 渲染器（/render），拿回 PNG 并落盘。
 * 输出目录默认 run/charts，可通过配置覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EchartsRenderClient {

    /** 渲染服务基地址，例如 <a href="http://localhost:5999">...</a> */
    @Value("${echarts.render.base-url}")
    private String baseUrl;

    /** 输出目录（默认 run/charts） */
    @Value("${echarts.render.out-dir:run/charts}")
    private String outDir;

    private static final ObjectMapper M = new ObjectMapper();

    /** 渲染“本月每日小时”柱状图，返回生成文件路径 */
    public Path renderMonthDailyBar(long userId, OvertimeSummary s) throws Exception {
        log.info("[ECharts] user={} render daily-bar start", userId);
        var option = EchartsOptionBuilder.dailyBarOption(s); // 使用二次元风格的 option
        Path p = renderToPng(option, 1000, 380, "#FCFCFF", "month-daily-" + userId + ".png");
        log.info("[ECharts] user={} render daily-bar done -> {}", userId, p);
        return p;
    }

    /** 渲染“本月类型占比”饼图，返回生成文件路径 */
    public Path renderMonthTypePie(long userId, OvertimeSummary s) throws Exception {
        log.info("[ECharts] user={} render type-pie start", userId);
        var option = EchartsOptionBuilder.typePieOption(s); // 使用二次元风格的 option
        Path p = renderToPng(option, 560, 380, "#FCFCFF", "month-type-" + userId + ".png");
        log.info("[ECharts] user={} render type-pie done -> {}", userId, p);
        return p;
    }

    /**
     * 发送 POST /render，拿回 PNG 并写入 outDir/fileName。
     *
     * @param option ECharts 配置
     * @param w      画布宽度
     * @param h      画布高度
     * @param bg     渲染页面 body 背景色（会透过 transparent 的 option 背景）
     * @param fileName 输出文件名
     */
    private Path renderToPng(Map<String, Object> option, int w, int h, String bg, String fileName) throws Exception {
        String url = baseUrl + "/render";

        // 构造请求体（与 Node 渲染器约定相同键名）
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("option", option);
        payload.put("width", w);
        payload.put("height", h);
        payload.put("backgroundColor", bg);
        String body = M.writeValueAsString(payload);

        StopWatch sw = new StopWatch("echarts-" + fileName);
        log.info("[ECharts] POST {} ({}x{}, bg={}), payload={}B", url, w, h, bg, body.length());

        // HTTP 请求渲染（10s 超时）
        sw.start("http");
        var resp = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(10_000)
                .execute();
        sw.stop();

        int status = resp.getStatus();
        byte[] png = resp.bodyBytes();
        int bytes = (png == null ? -1 : png.length);
        log.info("[ECharts] response status={} bytes={}", status, bytes);

        if (status != 200 || png == null || png.length == 0) {
            throw new IllegalStateException("Renderer HTTP " + status + " -> " + resp.body());
        }

        // 写文件到 outDir
        sw.start("writeFile");
        Path out = Paths.get(outDir, fileName);
        Files.createDirectories(out.getParent());
        Files.write(out, png);
        sw.stop();

        log.info("[ECharts] saved -> {} ({} bytes)\n{}", out.toAbsolutePath(), bytes, sw.prettyPrint());
        return out.toAbsolutePath();
    }
}
