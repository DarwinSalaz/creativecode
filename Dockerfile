FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
RUN mkdir /app
WORKDIR /app
EXPOSE 8080
ADD ./build/libs/creativecode-1.0.jar /app/creativecode.jar
ENTRYPOINT ["java", "-jar", "/app/creativecode.jar"]
