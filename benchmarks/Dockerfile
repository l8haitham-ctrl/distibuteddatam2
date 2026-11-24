FROM node:24-alpine
WORKDIR /app/benchmarks
COPY benchmarks/*.mjs ./
COPY benchmarks/test/ ./test/
COPY scripts/platform-session.mjs /app/scripts/platform-session.mjs
RUN node --test test/*.test.mjs
ENTRYPOINT ["node","run.mjs"]
