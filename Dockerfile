# ==========================================
# Spring Boot Linux Dockerfile (JDK 8)
# ==========================================

# 构建阶段
FROM maven:3.9-eclipse-temurin-8 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 先下载依赖（利用 Docker 缓存层）
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建 JAR 包（跳过测试）
RUN mvn clean package -DskipTests -B

# ==========================================
# 生产阶段
# ==========================================
FROM eclipse-temurin:8-jre

# 安装 wget 用于健康检查
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# 创建非 root 用户
RUN groupadd -r spring && useradd -r -g spring spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
