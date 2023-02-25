FROM openjdk:8-jdk-alpine
EXPOSE 8080
ADD /build/libs/creativecode-1.0.jar creativecode.jar
ENTRYPOINT ["java", "-jar", "dockerdemo.jar"]

