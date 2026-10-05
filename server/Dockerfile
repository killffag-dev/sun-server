FROM node:20-slim

WORKDIR /app

# Копируем сервер
COPY server/ ./server/

# Копируем статический сайт, который раздается сервером
COPY site/ ./site/

# Копируем файл version_info.json, который нужен серверу
COPY bot/updates/version_info.json ./bot/updates/version_info.json

WORKDIR /app/server

# Открываем порт 8080 для сервера и админки
EXPOSE 8080

CMD ["node", "server.js"]
