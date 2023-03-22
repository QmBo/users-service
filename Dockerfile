FROM openjdk:17-jdk-oracle
WORKDIR users-service
ADD target/users-service.jar users-service.jar
ENTRYPOINT java -jar users-service.jar