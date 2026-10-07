@echo off
title SUN Client Server
echo ========================================================
echo  SUN CLIENT - Сервер авторизации, сайт и админ-панель
echo  Сайт:           https://sun-server-production.up.railway.app/
echo  Личный кабинет: https://sun-server-production.up.railway.app/profile.html
echo  Админка:        https://sun-server-production.up.railway.app/admin
echo ========================================================
cd /d "%~dp0server"
node server.js
pause
