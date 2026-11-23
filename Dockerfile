# Compila com JDK 21; a imagem final só tem o JRE e as classes.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY Servidor/*.java ./
RUN javac -encoding UTF-8 -d out *.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./
EXPOSE 3000
# Configuração por ambiente: BACKEND1_HOST, BACKEND1_PORTA, CHAVE_INTERNA,
# TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN
ENTRYPOINT ["java", "-cp", ".", "Servidor"]
