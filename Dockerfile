# syntax=docker/dockerfile:1
# Imagem da API Spring, em duas etapas: o Maven compila (gerando as interfaces do
# openapi.yaml) e a imagem final leva só o JRE e o jar. Os testes rodam no CI, não aqui.
# Toda a configuração entra por variável de ambiente (ver application.properties).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# O cache do ~/.m2 sobrevive entre builds sem entrar na imagem.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --no-create-home centavo
COPY --from=build /app/target/*.jar app.jar
USER centavo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
