FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --prefer-offline
COPY . .
RUN npm run build -- --configuration=production

FROM nginx:alpine
# gettext supplies envsubst — used by entrypoint.sh to fill nginx.conf.template
RUN apk add --no-cache gettext

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80
EXPOSE 443
ENTRYPOINT ["/entrypoint.sh"]
