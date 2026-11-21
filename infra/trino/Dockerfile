FROM trinodb/trino:477
USER root
COPY config/ /etc/trino/
COPY rules.json /etc/trino/rules.json
COPY Bootstrap.java start.sh /opt/atlas/
RUN mkdir -p /opt/atlas/runtime && javac -proc:none -d /opt/atlas /opt/atlas/Bootstrap.java && chown trino:trino /opt/atlas/runtime
USER trino
ENTRYPOINT ["bash","/opt/atlas/start.sh"]
