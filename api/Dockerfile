# Container image for the Runway REST API.

FROM gradle:8-jdk21 AS build
WORKDIR /src
COPY . .
# buildFatJar is supplied by the Ktor Gradle plugin and packages the application together with every dependency it needs (Ktor, 2026).
RUN sed -i 's/\r$//' gradlew && sh ./gradlew buildFatJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/*-all.jar runway-api.jar

# Documentation only: the platform tells the app which port to use through the PORT environment variable, which application.yaml reads.
EXPOSE 8080

CMD ["java", "-jar", "runway-api.jar"]

# Reference List
# IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
# Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
