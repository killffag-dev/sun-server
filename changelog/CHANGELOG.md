[2026-10-06 13:27] ModsScreen (Sodium & Iris Dependencies)
- Added automatic synchronization to disable Iris when Sodium is disabled.
- Modified: src/main/java/naryn/sun/ui/mods/ModsScreen.java


[2026-10-06 13:56] AGENTS.md
- Обновлена документация: клиент переведен на бесплатную основу (удалены упоминания HWID, банов и подписок).
- Задокументировано наличие виртуальной валюты (Спарксы) и удаление модуля LicenseManager.java.
- Изменены файлы: AGENTS.md

[2026-10-06 14:04] Авторизация и Спарксы
- Добавлена кнопка 'Привязка' в главное меню, вызывающая генерацию 6-значного кода и предоставляющая кнопку копирования и перехода на сайт.
- Добавлена кнопка отображения баланса Спарксов в верхней панели главного меню.
- Изменены файлы: SunMainMenuScreen.java, MainMenuButton.java, MainMenuHeaderBar.java, добавлено LinkCodeScreen.java, SparksManager.java, а также обновлен server.js.
[2026-10-06 22:15] Исправление багов Главного Меню (Фон и эффекты)
- Устранен баг с накоплением времени и резким ускорением эффектов и параллакса мыши в фоне главного меню после Alt+Tab (добавлен сброс дельты и позиции при лагах > 500ms).
- Убран рендер фонов ClickGUI (Сетка, Соты, Космос и т.д.) из главного меню, чтобы они не накладывались на основной экран даже если включены в настройках модуля.
- Измененные файлы: src/main/java/naryn/sun/ui/mainmenu/background/MainMenuBackgroundRenderer.java, src/main/java/naryn/sun/ui/mainmenu/SunMainMenuScreen.java

- [2026-10-06 22:33] Main Menu UI Update
  - Split header buttons into Left/Right groups.
  - Replaced profile avatar with 'Войти' button if unlinked.
  - Moved LinkCodeScreen to a bottom sheet popup with animation.
  - Drawn vector Sparks icon and added balance display.
  - Modified: SunMainMenuScreen.java, MainMenuHeaderBar.java, HeaderActionBtn.java, LinkCodeScreen.java, SparksManager.java

- [2026-10-06 22:44] Network / API Configuration
  - Centralized API URL management into ServerConfig.java
  - Replaced hardcoded localhost strings to support easy switching to public domains
  - Modified: ServerConfig.java (new), SparksManager.java, UpdateManager.java, LinkCodeScreen.java, MainMenuHeaderBar.java

- [2026-10-06 22:46] Updated ServerConfig.java to use the real production domain (https://sunclient.ru) by default.

- [2026-10-06 23:03] Web Admin Panel
  - Redesigned visual layout for admin panel
  - Added viewing/editor mode switch and cosmetic dropdown
  - Modified: server/admin.html

- [2026-10-06 23:28] Server Logic Update
  - Global Announcement logic added to API and admin UI.
  - Editor/View mode fully functional.
  - Split online stats (Web & Game) and added Playtime telemetry.
  - DB migrated to SQLite with AES-256-CBC full payload encryption.

- [2026-10-06 23:47] Исправление падения сервера и восстановление доступа
  - Устранен фатальный краш Node.js сервера (`ReferenceError: clientVersion is not defined`) в эндпоинте `/api/check`.
  - Добавлена глобальная защита от падений процесса (`uncaughtException` и `unhandledRejection`).
  - Восстановлен рабочий локальный адрес `http://localhost:8080` в `ServerConfig.java`.
  - Добавлен удобный скрипт запуска `start_server.bat`.
  - Проверена работоспособность сайта, личного кабинета, админки и всех API.

- [2026-10-07 00:15] Полный перевод с LocalHost на боевой домен Railway
  - Заменен адрес по умолчанию в ServerConfig.java и EntitlementClient.java на боевой домен https://sun-server-production.up.railway.app с поддержкой JVM-переопределения (-Dsun.server.url).
  - В LinkCodeScreen.java и MainMenuHeaderBar.java обновлены переходы в браузер на https://sun-server-production.up.railway.app/profile.html.
  - В SparksManager.java и UpdateManager.java обеспечено динамическое разрешение ServerConfig.API_URL.
  - В server.js добавлен домен DOMAIN, привязка к 0.0.0.0, обработчик привязки игры /api/auth/pair/link, получение баланса Sparks из БД, и безопасный fallback при отсутствии нативного better-sqlite3.
  - Обновлены start_server.bat, Dockerfile и добавлен .dockerignore для корректной сборки контейнера.
  - Изменены файлы: ServerConfig.java, EntitlementClient.java, LinkCodeScreen.java, MainMenuHeaderBar.java, SparksManager.java, UpdateManager.java, server.js, start_server.bat, Dockerfile, .dockerignore.

- [2026-10-07 04:15] Восстановление работы боевого сервера Railway и полноценная привязка клиента Minecraft
  - Устранено падение Docker сборки и запуска на Railway: удалены проблемные нативные C++ зависимости better-sqlite3/sqlite3 из package.json, обеспечен мгновенный запуск чистого Node.js 20 с поддержкой базы database.json.
  - Реализован сквозной цикл привязки игрового клиента:
    - Сервер: эндпоинты /api/generate-link-code, /api/link-status, /api/auth/pair/link (с поддержкой очистки дефисов) и /api/sparks-balance (по ключу и токену).
    - Клиент: LinkCodeScreen.java теперь запрашивает уникальный код, выводит его пользователю, копирует в буфер обмена и запускает фоновый опрос сервера до момента подтверждения привязки в браузере.
    - Автосохранение привязанного аккаунта в SparksManager.java (.minecraft/Sun/account.json), корректный опрос баланса Спарксов и динамическое отображение ника/статуса/UID в MainMenuHeaderBar.java.
  - Изменены файлы: Dockerfile, package.json, server/Dockerfile, server/package.json, server/server.js, server/database.json, server/database.backup.json, SparksManager.java, LinkCodeScreen.java, MainMenuHeaderBar.java, ServerConfig.java, CHANGELOG.md.

