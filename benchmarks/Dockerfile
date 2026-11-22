FROM node:24-alpine
WORKDIR /app
COPY benchmarks/*.mjs /app/
COPY benchmarks/test/ /app/test/
RUN node --test test/*.test.mjs
ENTRYPOINT ["node","run.mjs"]
