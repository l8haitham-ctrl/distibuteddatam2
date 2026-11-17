FROM node:24-alpine AS development
WORKDIR /app
CMD ["sh", "-c", "npm ci && npm start -- --proxy-config proxy.docker.conf.json"]

FROM development AS build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm test
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.28-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/browser /usr/share/nginx/html
EXPOSE 8080
