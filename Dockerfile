FROM sbtscala/scala-sbt:eclipse-temurin-21_2.x

WORKDIR /app

COPY build.sbt ./
COPY project/ ./project/

RUN sbt update

COPY src/ ./src/

EXPOSE 8080

CMD ["sbt", "run"]