FROM node:22-bullseye

# 换国内源 + 安装系统 chromium + 中文字体
RUN set -eux; \
  sed -i 's|deb.debian.org|mirrors.aliyun.com|g; s|security.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list; \
  printf 'Acquire::ForceIPv4 "true";\nAcquire::Retries "5";\nAcquire::http::Timeout "30";\n' > /etc/apt/apt.conf.d/99tune; \
  apt-get update; \
  apt-get install -y --no-install-recommends chromium fonts-noto-cjk; \
  rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package.json package-lock.json* ./

# 跳过 Puppeteer 下载步骤（双保险），并换 npm 源
ENV PUPPETEER_SKIP_DOWNLOAD=1
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=1
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

RUN npm config set registry https://registry.npmmirror.com \
 && npm i --omit=dev

COPY server.js ./
EXPOSE 5999
CMD ["node", "server.js"]
