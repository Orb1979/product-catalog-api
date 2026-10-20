FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY --chmod=755 gradlew ./
COPY settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY catalog-api/build.gradle catalog-api/build.gradle
COPY mock-pricing-system/build.gradle mock-pricing-system/build.gradle
COPY mock-pricing-system/src/ mock-pricing-system/src/
RUN ./gradlew :mock-pricing-system:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/mock-pricing-system/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
