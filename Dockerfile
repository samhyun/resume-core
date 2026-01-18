FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache fontconfig ttf-dejavu ttf-freefont wkhtmltopdf \
    && addgroup -S app && adduser -S app -G app

USER app
WORKDIR /app

ARG ACTIVE_PROFILE=prod

COPY build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_DEFAULT=local \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul" \
    SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE}

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT:-8081}"]
