FROM trinodb/trino:477
USER root
COPY infra/trino/config/ /etc/trino/
COPY infra/trino/Bootstrap.java infra/trino/start.sh /opt/atlas/
COPY infra/analytics/rules.json /etc/trino/rules.json
COPY infra/analytics/redis.properties /etc/trino/catalog/redis.properties
COPY infra/analytics/tables/ /etc/trino/redis/
RUN mkdir -p /opt/atlas/runtime && javac -proc:none -d /opt/atlas /opt/atlas/Bootstrap.java && chown trino:trino /opt/atlas/runtime
USER trino
ENTRYPOINT ["bash","/opt/atlas/start.sh"]
