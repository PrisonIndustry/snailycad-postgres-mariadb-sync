FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS=""

EXPOSE 5555

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
