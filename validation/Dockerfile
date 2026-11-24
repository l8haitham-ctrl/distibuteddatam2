FROM node:24-alpine
WORKDIR /app/validation
COPY validation/smoke.mjs ./smoke.mjs
COPY scripts/platform-session.mjs /app/scripts/platform-session.mjs
COPY scripts/platform-session.test.mjs /app/scripts/platform-session.test.mjs
RUN node --test ../scripts/platform-session.test.mjs
ENTRYPOINT ["node","smoke.mjs"]
