FROM spark:4.3.0-java21-scala AS distribution

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY --from=distribution /opt/spark/jars /spark-jars
COPY spark/dependencies.pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q org.apache.maven.plugins:maven-dependency-plugin:3.10.0:copy-dependencies \
      -DincludeScope=runtime -DexcludeGroupIds=org.slf4j,org.apache.logging.log4j -DoutputDirectory=/connectors
COPY spark/jdbc-drivers.pom.xml jdbc-drivers.pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -f jdbc-drivers.pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.10.0:copy-dependencies \
      -DincludeScope=runtime -DoutputDirectory=/jdbc-drivers
COPY spark/native-dependencies.pom.xml native-dependencies.pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -f native-dependencies.pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.10.0:copy-dependencies \
      -DincludeScope=runtime -DoutputDirectory=/native-jars
COPY spark/src ./src
COPY backend/src/main/java/com/atlas/explorer/features/connectors/connector /plugin-src/connector
COPY backend/src/main/java/com/atlas/explorer/features/connectors/dto /plugin-src/dto
COPY backend/src/main/java/com/atlas/explorer/features/nativeingestion/connector /native-src/connector
COPY backend/src/main/java/com/atlas/explorer/features/nativeingestion/dto /native-src/dto
COPY backend/src/main/resources/META-INF/services /classes/META-INF/services
RUN mkdir -p /classes && javac -proc:none --release 21 -cp '/spark-jars/*:/connectors/*:/native-jars/*' -d /classes \
    $(find src/main/java src/test/java /plugin-src /native-src -name '*.java') && \
    java -cp '/classes:/spark-jars/*:/connectors/*:/native-jars/*' com.atlas.ingestion.NativeSparkReaderCheck && \
    jar --create --file /atlas-ingestion.jar -C /classes .

FROM distribution
USER root
COPY --from=build /connectors/ /opt/spark/jars/
COPY --from=build /jdbc-drivers/ /opt/spark/jars/
COPY --from=build /native-jars/ /opt/spark/jars/
COPY --from=build --chown=185:185 /atlas-ingestion.jar /opt/atlas/atlas-ingestion.jar
RUN mkdir -p /opt/spark/recovery && chown 185:185 /opt/spark/recovery
COPY spark/spark-defaults.conf /opt/spark/conf/spark-defaults.conf
USER 185:185
