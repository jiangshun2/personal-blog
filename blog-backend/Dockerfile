# ============================================================
# 个人博客系统 - 后端镜像
# 说明：镜像内直接运行打包好的 jar（Spring Boot 内嵌 Tomcat）
# 构建前置：在 blog-backend 目录执行 mvn -DskipTests package
# 注意：不安装 tzdata（国内拉 Alpine 源慢），时区通过 JVM 参数与环境变量指定
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制可执行 jar（内嵌 Tomcat，无需额外安装 Tomcat）
COPY target/blog-backend-1.0.0.jar app.jar

EXPOSE 8080

# 时区：东八区（Java 自带时区数据，无需 tzdata）
ENV TZ=Asia/Shanghai

# JVM 参数可按服务器内存调整（2C2G 服务器建议 512m 上限）
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
