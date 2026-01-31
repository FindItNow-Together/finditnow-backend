FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY build/install/file-gateway /app/

EXPOSE 8090

CMD ["/app/bin/file-gateway"]
