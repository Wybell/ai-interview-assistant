FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

ENV MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"

COPY pom.xml ./
COPY docker/maven-settings.xml ./maven-settings.xml
COPY src/ src/
RUN mvn -s maven-settings.xml -B -DskipTests -Dmaven.wagon.http.retryHandler.count=5 package

FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/ai-interview-assistant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
