FROM node:22-slim

WORKDIR /app

# Копируем манифесты зависимостей и устанавливаем пакеты
COPY server/package*.json ./server/
WORKDIR /app/server
RUN npm install --omit=dev || true

WORKDIR /app

# Копируем сервер
COPY server/ ./server/

# Копируем статический сайт, который раздается сервером
COPY site/ ./site/

# Копируем файл version_info.json, который нужен серверу
COPY bot/updates/version_info.json ./bot/updates/version_info.json

WORKDIR /app/server

# Открываем порт 8080 для сервера и админки
ENV PORT=8080
EXPOSE 8080

CMD ["node", "server.js"]
