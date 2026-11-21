FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /deps
COPY dependencies.xml pom.xml
RUN mvn -B -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=/jars
FROM apache/hive:4.0.1
USER root
COPY --from=deps /jars/ /opt/hive/lib/
COPY start.sh /opt/atlas/start.sh
RUN chmod 755 /opt/atlas/start.sh && chown -R hive:hive /opt/hive/conf
USER hive
ENTRYPOINT ["bash","/opt/atlas/start.sh"]
