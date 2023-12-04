## Dockerfile
#latest version 3.3.0

FROM eclipse-temurin:17-jdk-jammy

COPY target/employeeTimeTracongBot-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]