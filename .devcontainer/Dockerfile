FROM node:24-bookworm-slim AS node-tools
FROM maven:3.9-eclipse-temurin-21
COPY --from=node-tools /usr/local/ /usr/local/
WORKDIR /workspace
CMD ["sleep", "infinity"]
