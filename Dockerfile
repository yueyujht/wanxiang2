# ==================== 构建阶段 ====================
# 使用 Alpine 版镜像（musl libc 走 clone 而非 clone3），
# 规避旧版 Docker（< 20.10.10）seccomp 对 clone3 的限制导致的 pthread_create EPERM
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# 先复制 pom，利用 Docker 层缓存预下载依赖
COPY pom.xml .
COPY wanxiang-common/pom.xml wanxiang-common/pom.xml
COPY wanxiang-user/pom.xml wanxiang-user/pom.xml
COPY wanxiang-app/pom.xml wanxiang-app/pom.xml
RUN mvn -q -B -pl wanxiang-app -am dependency:go-offline || true

# 复制源码并打包（只构建 wanxiang-app 及其依赖模块）
COPY wanxiang-common/src wanxiang-common/src
COPY wanxiang-user/src wanxiang-user/src
COPY wanxiang-app/src wanxiang-app/src
RUN mvn -q -B clean package -DskipTests -pl wanxiang-app -am

# ==================== 运行阶段 ====================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Alpine 需手动装时区数据，否则 TZ=Asia/Shanghai 不生效
RUN apk add --no-cache tzdata

COPY --from=build /build/wanxiang-app/target/wanxiang-app.jar app.jar

ENV TZ=Asia/Shanghai
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]