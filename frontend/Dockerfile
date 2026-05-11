FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --prefer-offline
COPY . .
RUN npm run build -- --configuration=production

FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# SSL certs are NOT baked into the image — they are bind-mounted from the host
# at runtime via docker-compose.prod.yml → /opt/cms/ssl/
EXPOSE 80