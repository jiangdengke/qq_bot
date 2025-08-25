# ---------- 构建阶段 ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷 pom，用缓存加速依赖下载
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

# 再拷源码并打包
COPY src/ src/
RUN mvn -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
WORKDIR /app

# 名字不确定就用通配符
COPY --from=build /app/target/*.jar /app/app.jar

VOLUME ["/app/logs", "/app/config"]
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar --spring.config.additional-location=file:/app/config/"]
