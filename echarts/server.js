// server.js
import express from "express";
import puppeteer from "puppeteer-core";
import { fileURLToPath } from "url";
import path from "path";
import { createRequire } from "module";   // 在 ESM 里创建 require
const require = createRequire(import.meta.url);

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(express.json({ limit: "2mb" }));

let browser;
async function ensureBrowser() {
    if (browser && (await browser.pages()).length >= 0) return browser;
    browser = await puppeteer.launch({
        executablePath: process.env.PUPPETEER_EXECUTABLE_PATH || "/usr/bin/chromium", // 或 /usr/bin/chromium-browser
        args: ["--no-sandbox", "--disable-setuid-sandbox"]
    });
    return browser;
}

app.post("/render", async (req, res) => {
    const t0 = Date.now();
    try {
        const { option, width = 1000, height = 380, backgroundColor = "#FFFFFF" } = req.body || {};
        if (!option) return res.status(400).json({ error: "missing option" });

        const br = await ensureBrowser();
        const page = await br.newPage();
        await page.setViewport({ width, height, deviceScaleFactor: 2 });

        await page.setContent(`
      <html>
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;background:${backgroundColor}">
          <div id="c" style="width:${width}px;height:${height}px;"></div>
        </body>
      </html>
    `);

        const echartsPath = require.resolve("echarts/dist/echarts.min.js");
        await page.addScriptTag({ path: echartsPath });

        // 初始化图表
        await page.evaluate((opt) => {
            const c = document.getElementById("c");
            const chart = echarts.init(c, null, { renderer: "canvas", locale: "ZH" });
            chart.setOption(opt, { notMerge: true, lazyUpdate: false });
            chart.resize();
            window.__chart__ = chart;
        }, option);

        // ✅ 等待 canvas 真正挂载并有尺寸，再截图（替代 waitForTimeout）
        await page.waitForFunction(
            () => {
                const el = document.querySelector("#c canvas");
                return el && el.width > 0 && el.height > 0;
            },
            { timeout: 8000 }
        );

        const el = await page.$("#c");
        if (!el) throw new Error("#c not found");
        const png = await el.screenshot({ type: "png" });
        await page.close();

        console.log(`[renderer] ok ${width}x${height} ${png.length}B in ${Date.now() - t0}ms`);
        res.type("png").send(png);
    } catch (e) {
        console.error(`[renderer] fail in ${Date.now() - t0}ms`, e);
        res.status(500).json({ error: String(e) });
    }
});

const PORT = process.env.PORT || 5999;
app.listen(PORT, () => console.log(`ECharts renderer listening on :${PORT}`));
