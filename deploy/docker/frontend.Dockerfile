FROM node:20-alpine AS web-build
WORKDIR /app/frontend/echoim-web
COPY frontend/echoim-web/package*.json ./
RUN npm ci
COPY frontend/echoim-web/ ./
RUN npm run build

FROM node:20-alpine AS admin-build
WORKDIR /app/frontend/echoim-admin
COPY frontend/echoim-admin/package*.json ./
RUN npm ci
COPY frontend/echoim-admin/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/docker/frontend.nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=web-build /app/frontend/echoim-web/dist /usr/share/nginx/html
COPY --from=admin-build /app/frontend/echoim-admin/dist /usr/share/nginx/html/admin
