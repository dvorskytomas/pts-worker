FROM eclipse-temurin:latest

WORKDIR /opt/jmeter
COPY jmeter .

WORKDIR /
COPY /target/pts-worker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","/app.jar"]
