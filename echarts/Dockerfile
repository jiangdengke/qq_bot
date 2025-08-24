FROM node:20-bullseye

# 中文字体（避免口口口）
RUN apt-get update && apt-get install -y --no-install-recommends \
    fonts-noto-cjk \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package.json ./
RUN npm i --omit=dev
COPY server.js ./

EXPOSE 5999
CMD ["node", "server.js"]
