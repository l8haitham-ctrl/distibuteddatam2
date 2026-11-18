FROM spark:4.3.0-java21-scala AS distribution

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY --from=distribution /opt/spark/jars /spark-jars
COPY dependencies.pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q org.apache.maven.plugins:maven-dependency-plugin:3.10.0:copy-dependencies \
      -DincludeScope=runtime -DexcludeGroupIds=org.slf4j,org.apache.logging.log4j -DoutputDirectory=/connectors
COPY src ./src
RUN mkdir -p /classes && javac -proc:none --release 21 -cp '/spark-jars/*' -d /classes \
    src/main/java/com/atlas/ingestion/IngestionJob.java && \
    jar --create --file /atlas-ingestion.jar -C /classes .

FROM distribution
USER root
COPY --from=build /connectors/ /opt/spark/jars/
COPY --from=build --chown=185:185 /atlas-ingestion.jar /opt/atlas/atlas-ingestion.jar
RUN mkdir -p /opt/spark/recovery && chown 185:185 /opt/spark/recovery
COPY spark-defaults.conf /opt/spark/conf/spark-defaults.conf
USER 185:185
