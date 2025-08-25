package org.jdk.qq_bot.service.echats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.jdk.qq_bot.dto.OvertimeSummary;

/**
 * 二次元风格增强版：
 * - 更丰富的叠加层（外环发光、顶端高光、糖纸条纹）
 * - graphic 贴纸/丝带/小星星/爱心
 * - 胶囊标签、中心数字
 * - 仍与服务端渲染协议完全兼容
 */
public final class EchartsOptionBuilder {
    private EchartsOptionBuilder() {}

    /* ------------------------- 公共小工具 ------------------------- */

    /** 粉彩色系（全局复用） */
    private static List<String> moePalette() {
        return List.of("#FFD1DC","#BDE0FE","#CDEAC0","#FFE8A3","#E0BBE4","#FFDFD3");
    }

    /** 上下方向线性渐变（适配 ECharts 渐变对象） */
    private static Map<String, Object> vGrad(String c1, String c2) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("type","linear"); g.put("x",0); g.put("y",0); g.put("x2",0); g.put("y2",1);
        g.put("colorStops", List.of(Map.of("offset",0,"color",c1), Map.of("offset",1,"color",c2)));
        return g;
    }

    /** 求和：本月总小时（用于中心数字） */
    private static double sumHours(OvertimeSummary s) {
        return s.getDailyTotals().values().stream().filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).sum();
    }

    /* ------------------------- 每日柱状图 ------------------------- */

    /** 每日总小时 柱状图（更花哨的二次元风格） */
    public static Map<String, Object> dailyBarOption(OvertimeSummary s) {
        // x 轴与数据
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        s.getDailyTotals().forEach((LocalDate d, BigDecimal h) -> {
            labels.add(d.toString().substring(5));           // MM-dd
            data.add(h == null ? 0d : h.doubleValue());
        });

        // —— 主柱子：粉→紫 渐变 + 阴影 + 圆角 —— //
        Map<String, Object> mainBar = new LinkedHashMap<>();
        mainBar.put("type","bar");
        mainBar.put("data", data);
        mainBar.put("barWidth", 18);
        mainBar.put("barCategoryGap","32%");
        mainBar.put("showBackground", true); // 背景条，做“糖纸”质感
        mainBar.put("backgroundStyle", Map.of(
                "color","rgba(255,255,255,0.55)",
                "shadowBlur",18,
                "shadowColor","rgba(160,160,210,0.25)"
        ));
        mainBar.put("itemStyle", Map.of(
                "borderRadius", List.of(12,12,8,8),
                "shadowBlur", 16,
                "shadowColor", "rgba(165,155,255,0.28)",
                "color", vGrad("rgba(255,209,220,0.96)","rgba(196,164,255,0.88)")
        ));
        mainBar.put("emphasis", Map.of("itemStyle", Map.of(
                "shadowBlur", 22,
                "shadowColor","rgba(165,155,255,0.45)",
                "color", vGrad("rgba(255,209,220,1.0)","rgba(196,164,255,0.96)")
        )));

        // —— 糖纸条纹：pictorialBar 作为重复窄条，营造“包裹”感 —— //
        Map<String, Object> stripes = new LinkedHashMap<>();
        stripes.put("type","pictorialBar");
        stripes.put("data", data);
        stripes.put("symbol","rect");
        stripes.put("symbolSize", List.of(18,2)); // 与 barWidth 对应
        stripes.put("symbolMargin", 2);
        stripes.put("symbolRepeat", true);
        stripes.put("z",-1); // 在主柱子下方
        stripes.put("itemStyle", Map.of("color","rgba(196,164,255,0.08)"));

        // —— 顶端高光：pictorialBar 圆点 + 发光 —— //
        Map<String, Object> glowCap = new LinkedHashMap<>();
        glowCap.put("type","pictorialBar");
        glowCap.put("data", data);
        glowCap.put("symbol","circle");
        glowCap.put("symbolSize", 16);
        glowCap.put("symbolOffset", List.of(0,-3));
        glowCap.put("symbolPosition","end");
        glowCap.put("z", 3);
        glowCap.put("itemStyle", Map.of(
                "color","rgba(255,210,230,0.95)",
                "shadowBlur", 20,
                "shadowColor","rgba(196,164,255,0.55)"
        ));

        // —— 主配置 —— //
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("backgroundColor","transparent");
        option.put("color", moePalette());
        option.put("animationEasing","elasticOut");
        option.put("animationDuration",1200);

        option.put("title", Map.of(
                "text","本月每日加班（小时） ᕦ(ò_óˇ)ᕤ",
                "left","center",
                "textStyle", Map.of("color","#556","fontSize",20,"fontWeight",700)
        ));
        option.put("grid", Map.of("left",56,"right",30,"top",70,"bottom",52));

        option.put("xAxis", Map.of(
                "type","category",
                "data", labels,
                "axisTick", Map.of("show",false),
                "axisLine", Map.of("lineStyle", Map.of("color","#d8dbe8")),
                "axisLabel", Map.of("color","#667","fontWeight",500)
        ));
        option.put("yAxis", Map.of(
                "type","value","min",0,
                "splitLine", Map.of("lineStyle", Map.of("color","rgba(0,0,0,0.06)")),
                "splitArea", Map.of("show",true, "areaStyle", Map.of(
                        "color", List.of("rgba(253,246,255,0.35)","rgba(255,255,255,0.0)")
                )),
                "axisLabel", Map.of("color","#667")
        ));
        option.put("tooltip", Map.of(
                "trigger","axis",
                "axisPointer", Map.of("type","shadow"),
                "backgroundColor","rgba(255,255,255,0.95)",
                "borderColor","#eadcff","borderWidth",1,
                "textStyle", Map.of("color","#334"),
                "formatter","🌸 {b}<br/>⏱️ {c} 小时"
        ));

        // —— 贴纸与丝带（graphic）—— //
        List<Map<String,Object>> graphics = new ArrayList<>();
        // 左上星星
        graphics.add(Map.of("type","text","silent",true,"z",-10,"left","6%","top","8%",
                "style", Map.of("text","✦","fontSize",28,"fill","rgba(189,224,254,0.38)")));
        // 右下爱心
        graphics.add(Map.of("type","text","silent",true,"z",-10,"right","6%","bottom","6%",
                "style", Map.of("text","❤","fontSize",22,"fill","rgba(255,209,220,0.38)")));
        // 背景丝带（扇形）
        graphics.add(Map.of("type","sector","silent",true,"z",-12,"left","center","top","55%",
                "shape", Map.of("cx",0,"cy",0,"r",220,"r0",180,"startAngle",Math.toRadians(-15),"endAngle",Math.toRadians(25)),
                "style", Map.of("fill", vGrad("rgba(196,164,255,0.12)","rgba(255,209,220,0.08)"))
        ));
        option.put("graphic", graphics);

        // —— 三层系列叠加：主柱 + 条纹 + 顶端高光 —— //
        option.put("series", List.of(mainBar, stripes, glowCap));
        return option;
    }

    /* ------------------------- 类型占比饼图 ------------------------- */

    /** 本月类型占比 饼图（外发光环 + 内高光 + 胶囊标签 + 贴纸） */
    public static Map<String, Object> typePieOption(OvertimeSummary s) {
        double g1 = s.getMonthByType().getOrDefault("G1", BigDecimal.ZERO).doubleValue();
        double g2 = s.getMonthByType().getOrDefault("G2", BigDecimal.ZERO).doubleValue();
        double g3 = s.getMonthByType().getOrDefault("G3", BigDecimal.ZERO).doubleValue();

        // 数据：为 0 的项依然保留，这样图例能显示；但扇区会非常薄
        List<Map<String, Object>> data = new ArrayList<>();
        if (g1 + g2 + g3 == 0) {
            data.add(Map.of("name","无记录","value",1)); // 占位，防止空心
        } else {
            data.add(Map.of("name","G1","value",g1));
            data.add(Map.of("name","G2","value",g2));
            data.add(Map.of("name","G3","value",g3));
        }

        // —— 主环（糖果质感 + 白描边） —— //
        Map<String, Object> main = new LinkedHashMap<>();
        main.put("type","pie");
        main.put("radius", List.of("38%","66%"));
        main.put("center", List.of("50%","54%"));
        main.put("avoidLabelOverlap", true);
        main.put("itemStyle", Map.of(
                "borderRadius",10,
                "shadowBlur", 18,
                "shadowColor","rgba(0,0,0,0.12)",
                "borderColor","#fff",
                "borderWidth",2
        ));
        main.put("label", Map.of(
                "formatter","{name|{b}}  {percent|{d}%}",
                "rich", Map.of(
                        "name", Map.of("fontSize",12,"color","#445","backgroundColor","rgba(255,255,255,0.9)",
                                "padding", List.of(3,8),"borderRadius", 9),
                        "percent", Map.of("fontSize",12,"fontWeight",600,"color","#445",
                                "backgroundColor","rgba(255,255,255,0.9)","padding", List.of(3,8),"borderRadius",9)
                )
        ));
        main.put("labelLine", Map.of("length",12,"length2",10,"smooth",true));
        main.put("data", data);

        // —— 外发光细环（静态可见的“氛围光”） —— //
        Map<String, Object> outerGlow = new LinkedHashMap<>();
        outerGlow.put("type","pie"); outerGlow.put("silent", true); outerGlow.put("z", -1);
        outerGlow.put("radius", List.of("70%","74%"));
        outerGlow.put("center", List.of("50%","54%"));
        outerGlow.put("label", Map.of("show", false));
        outerGlow.put("data", List.of(Map.of("value", 1, "itemStyle", Map.of(
                "color","rgba(189,224,254,0.35)",
                "shadowBlur", 25,
                "shadowColor","rgba(173,216,230,0.55)"
        ))));

        // —— 内高光细环（让中空部分更“发光”） —— //
        Map<String, Object> innerGloss = new LinkedHashMap<>();
        innerGloss.put("type","pie"); innerGloss.put("silent", true); innerGloss.put("z",-1);
        innerGloss.put("radius", List.of("28%","32%"));
        innerGloss.put("center", List.of("50%","54%"));
        innerGloss.put("label", Map.of("show", false));
        innerGloss.put("data", List.of(Map.of("value", 1, "itemStyle", Map.of(
                "color", vGrad("rgba(255,255,255,0.95)","rgba(255,255,255,0.35)")
        ))));

        // —— 主配置 —— //
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("backgroundColor","transparent");
        option.put("color", moePalette());
        option.put("title", Map.of(
                "text","本月加班类型占比 ❀",
                "left","center",
                "textStyle", Map.of("color","#556","fontSize",20,"fontWeight",700)
        ));
        option.put("tooltip", Map.of(
                "trigger","item",
                "backgroundColor","rgba(255,255,255,0.95)",
                "borderColor","#eadcff","borderWidth",1,
                "textStyle", Map.of("color","#334"),
                "formatter","🍬 {b}<br/>⏱️ {c} 小时（{d}%）"
        ));
        option.put("legend", Map.of(
                "bottom", 6, "icon","roundRect", "itemWidth",12, "itemHeight",8,
                "textStyle", Map.of("color","#556")
        ));

        // —— 中心数字 + 表情（graphic 文本） —— //
        double total = sumHours(s);
        List<Map<String,Object>> graphics = new ArrayList<>();
        graphics.add(Map.of("type","text","silent",true,"z",10,"left","center","top","46%",
                "style", Map.of(
                        "text", String.format("%.1f h", total),
                        "fontSize", 22, "fontWeight", 700, "fill","#556", "align","center"
                )));
        graphics.add(Map.of("type","text","silent",true,"z",10,"left","center","top","58%",
                "style", Map.of("text","(ง •̀_•́)ง", "fontSize", 14, "fill","#889", "align","center")));

        // 贴纸：左上星星、右下爱心、右上小花
        graphics.add(Map.of("type","text","silent",true,"z",-10,"left","8%","top","12%",
                "style", Map.of("text","✦","fontSize",26,"fill","rgba(189,224,254,0.38)")));
        graphics.add(Map.of("type","text","silent",true,"z",-10,"right","10%","bottom","9%",
                "style", Map.of("text","❤","fontSize",22,"fill","rgba(255,209,220,0.38)")));
        graphics.add(Map.of("type","text","silent",true,"z",-10,"right","16%","top","14%",
                "style", Map.of("text","❁","fontSize",20,"fill","rgba(224,187,228,0.40)")));

        // 背景丝带（扇形），让画面不要太空
        graphics.add(Map.of("type","sector","silent",true,"z",-12,"left","center","top","58%",
                "shape", Map.of("cx",0,"cy",0,"r",235,"r0",200,
                        "startAngle",Math.toRadians(210),"endAngle",Math.toRadians(260)),
                "style", Map.of("fill", vGrad("rgba(196,164,255,0.12)","rgba(255,209,220,0.08)"))
        ));

        option.put("graphic", graphics);

        // 组合三层环：外发光 + 主环 + 内高光
        option.put("series", List.of(outerGlow, main, innerGloss));
        return option;
    }
}
