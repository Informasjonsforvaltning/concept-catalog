FROM openjdk:17-slim

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY /target/concept-catalog.jar app.jar

CMD java -jar $JAVA_OPTS app.jar
