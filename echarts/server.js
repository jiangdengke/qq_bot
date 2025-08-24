import express from "express";
import puppeteer from "puppeteer";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(express.json({ limit: "2mb" }));

let browser;
async function ensureBrowser() {
    if (browser && (await browser.pages()).length >= 0) return browser;
    // puppeteer 自带 Chromium，Docker 镜像会下，首次较慢
    browser = await puppeteer.launch({
        args: ["--no-sandbox", "--disable-setuid-sandbox"]
    });
    return browser;
}

app.post("/render", async (req, res) => {
    try {
        const { option, width = 1000, height = 380, backgroundColor = "#FFFFFF" } = req.body || {};
        if (!option) return res.status(400).json({ error: "missing option" });

        const br = await ensureBrowser();
        const page = await br.newPage();
        await page.setViewport({ width, height, deviceScaleFactor: 2 });

        // 装载空白页
        await page.setContent(`
      <html>
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;background:${backgroundColor}">
          <div id="c" style="width:${width}px;height:${height}px;"></div>
          <script>window.__BG__='${backgroundColor}'</script>
        </body>
      </html>
    `);

        // 注入 echarts
        await page.addScriptTag({ path: require.resolve("echarts/dist/echarts.min.js") });

        // 设置并渲染
        await page.evaluate((opt) => {
            const c = document.getElementById("c");
            const chart = echarts.init(c, null, { renderer: "canvas", locale: "ZH" });
            chart.setOption(opt, { notMerge: true, lazyUpdate: false });
            chart.resize();
            window.__chart__ = chart;
        }, option);

        // 等一帧
        await page.waitForTimeout(50);

        // 截图容器
        const el = await page.$("#c");
        const png = await el.screenshot({ type: "png" });

        await page.close();
        res.setHeader("Content-Type", "image/png");
        res.send(png);
    } catch (e) {
        console.error(e);
        res.status(500).json({ error: String(e) });
    }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => console.log(`ECharts renderer listening on :${PORT}`));
