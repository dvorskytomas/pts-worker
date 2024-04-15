FROM eclipse-temurin:latest

WORKDIR /opt/jmeter
COPY jmeter .

WORKDIR /opt/k6
COPY k6 .

WORKDIR /
COPY /target/pts-worker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

#ENTRYPOINT ["java","-jar","/app.jar"]
ENTRYPOINT exec java -jar /app.jar $ARGS
