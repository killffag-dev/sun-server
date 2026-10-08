[2026-10-08 13:53] SUN Connect Instruction Fix & Bento Boxes Visual Separation
- В блоке SUN Connect на главной странице (site/index.html) исправлена инструкция: заменено неверное упоминание меню GUI и несуществующего модуля на реальный вход через кнопку «Войти» в Главном меню игры.
- В site/style.css добавлен увеличенный отступ (.install-sub-grid margin-top: 2.6rem; padding-top: 1.6rem) и тонкая разделительная линия, четко визуально отделяющая 3 основных шага установки от нижних информационных боксов (SUN Connect и Важное Правило).
- Версия стилей в site/index.html обновлена до style.css?v=15 для сброса кэша браузера.
- Измененные файлы: site/index.html, site/style.css

[2026-10-08 12:45] Beta Release Prep & Client JAR Update
- Обновлен скачиваемый файл мода site/downloads/sun-client-1.21.4.jar до актуальной версии (13.6 МБ), включающей One-Click Connect, систему объявлений, всплывающее меню обновлений и семантическую проверку версий.
- В site/index.html исправлены инструкции установки (Шаг 1 и 2) и плашка правил: убран ошибочный запрет на Fabric API, указано требование Fabric 1.21.4 + Fabric API, а также отмечено, что Sodium, Iris, Lithium и шейдеры уже встроены в клиент и распаковываются автоматически.
- Ссылки на обновление клиента и Telegram-бота переведены на официальный канал https://t.me/SUN_Visual (в UpdateManager.java, server/server.js и bot/updates/version_info.json).
- Измененные файлы: site/downloads/sun-client-1.21.4.jar, site/index.html, server/server.js, bot/updates/version_info.json, src/main/java/naryn/sun/systems/update/UpdateManager.java

[2026-10-08 12:08] Semantic Version Check & Announcement Reset
- В AnnouncementManager.java и UpdateManager.java внедрено строгое семантическое сравнение версий (SemVer): уведомление о новой версии показывается только если версия на сервере строго новее текущей версии клиента (isRemoteVersionNewer). Некорректные и тестовые строки (напр. "2/2/") больше не вызывают ложных срабатываний и спама.
- В admin.html добавлена кнопка «Снять объявление», позволяющая в один клик отозвать активное объявление и остановить показ уведомлений об обновлении у всех игроков.
- В server.js добавлен обработчик действия 'clear_announcement', сбрасывающий активное объявление на сервере и у всех пользователей в базе.
- Тестовое объявление на сервере (server/announcement.json) деактивировано и очищено.
- Измененные файлы: src/main/java/naryn/sun/systems/announcement/AnnouncementManager.java, src/main/java/naryn/sun/systems/update/UpdateManager.java, server/server.js, server/admin.html, server/announcement.json

[2026-10-08 11:47] Profile vs Site Background Separation
- В site/style.css переопределение `.kimiko-body .bg` исправлено на темный темно-серый лес `assets/bg.webp` (в точности как на главной странице сайта).
- Внутренний баннер пользователя (`#banner-cover` / `.kimiko-banner-cover`) возвращен к картинке коричневого леса (`assets/Gemini_Generated_Image_...jfif`).
- Версия стилей обновлена до `style.css?v=12` для мгновенного сброса кэша.
- Изменения запушены на боевой сервер (git push origin main, коммит c449537).
- Измененные файлы: site/profile.html, site/style.css

[2026-10-08 11:43] Profile Background & Cache Invalidation (bg.webp)
- Сброшен жесткий браузерный кэш CSS в site/profile.html путем инкремента версии: `style.css?v=10`.
- В site/style.css и site/profile.html дефолтная обложка профиля переведена с коричневого леса на оригинальный темно-серый лес `assets/bg.webp`.
- Изменения запушены на боевой сервер (git push origin main, коммит 1cde729).
- Измененные файлы: site/profile.html, site/style.css

[2026-10-08 11:37] Profile Page Background Alignment (bg.webp)
- Фон страницы личного кабинета (site/style.css, .kimiko-body) приведен в полное соответствие с главной страницей: включен темный темно-серый фон леса (assets/bg.webp с градиентом) через общий класс `.bg`.
- Изменения запушены на боевой сервер (git push origin main, коммит 613fd46).
- Измененные файлы: site/style.css

[2026-10-08 11:33] Profile Security (Удаление кнопки «Отвязать»)
- Из личного кабинета (site/profile.html) полностью удалена кнопка «Отвязать», разметка и обработчики события отвязки клиента игры.
- Изменения запушены на боевой сервер (git push origin main).
- Измененные файлы: site/profile.html

[2026-10-08 11:31] Client-Web Pairing State Synchronization
- Устранена рассинхронизация статуса привязки в веб-профиле: в site/link.html при успешном подтверждении сопряжения (1-Click Pair, вход или регистрация) статус clientLinked, Sparks и UID мгновенно сохраняются в локальную сессию браузера.
- В site/profile.html исправлен запрос актуализации привязки через фоновый опрос /api/sparks-balance, гарантирующий подтягивание боевого статуса и баланса даже при наличии старого кэша в браузере.
- Изменения зафиксированы и отправлены в боевой репозиторий (git push origin main).
- Измененные файлы: site/link.html, site/profile.html

[2026-10-08 11:23] Profile UI Cleanup (Удаление блока скачивания и шагов из ЛК)
- Из личного кабинета (site/profile.html) удален дублирующий блок скачивания мода и инструкции по сопряжению в 3 шага.
- Измененные файлы: site/profile.html

[2026-10-08 11:19] Documentation & Site Cleanup (Удаление остатков 6-значного кода)
- В инструкции на главной странице сайта (site/index.html) полностью удалено упоминание 6-значного пин-кода и ввода его в Личном Кабинете.
- Шаги обновлены на актуальный One-Click Connect (нажатие «Войти» в игре и подтверждение привязки в браузере в 1 клик).
- Измененные файлы: site/index.html

[2026-10-07 23:45] Client & Web Announcements System (Одноразовые объявления и обновления версии)
- Реализована полноценная система объявлений: разделение на обычные объявления (одноразовые, навсегда запоминают закрытие) и обязательные обновления версии клиентом.
- На сайте (site/profile.html, site/app.js) закрытие крестиком или кнопкой сохраняется в localStorage по уникальному ID объявления, предотвращая повторное всплывание при перезагрузке страницы.
- На сервере (server/server.js, server/admin.html) внедрена генерация уникальных ID объявлений, передача флагов isUpdate и версии, а также автоматическая ссылка на секцию скачивания /#download.
- В клиенте игры создан AnnouncementManager: асинхронный опрос сервера, сохранение прочитанных объявлений в dismissed_announcements.json, и постоянная проверка актуальной версии мода (Sun.VERSION).
- Создан визуальный компонент MainMenuAnnouncementPopup: компактная карточка в левой верхней части главного меню игры (под кнопками шапки), открывающая прямую страницу скачивания на сайте по кнопке «Скачать с обновлением».
- Измененные файлы: src/main/java/naryn/sun/systems/announcement/AnnouncementManager.java, src/main/java/naryn/sun/ui/mainmenu/MainMenuAnnouncementPopup.java, src/main/java/naryn/sun/ui/mainmenu/SunMainMenuScreen.java, src/main/java/naryn/sun/ui/mainmenu/HeaderActionBtn.java, src/main/java/naryn/sun/systems/update/UpdateManager.java, server/server.js, site/profile.html, site/app.js

[2026-10-07 21:54] Link UI Monochrome Theme & Admin Security Hardening
- Полный перевод страницы сопряжения site/link.html в монохромный дизайн сайта: убраны золотые и жёлтые акценты, аватар и кнопка подтверждения приведены к чистому бело-серому стилю.
- Из формы входа админ-панели server/admin.html полностью удалена подсказка с паролем по умолчанию.
- Измененные файлы: site/link.html, server/admin.html

[2026-10-07 21:26] One-Click Connect Hardening & Security/UI Bugfixes
- Исправлен расчет ширины плашки профиля в MainMenuHeaderBar: теперь при привязанном аккаунте учитывается реальный никнейм из SparksManager, предотвращая обрезку текста и перекрытие кнопки баланса Спарксов.
- Устранена утечка фоновых потоков и нежелательное открытие браузера в LinkCodeScreen при быстром закрытии диалога игроком (добавлены проверки статуса running).
- Добавлена обработка истечения времени токена и кнопка «Повторить» при сбоях сети в LinkCodeScreen; вызовы буфера обмена GLFW вынесены в клиентский поток.
- Внедрено динамическое обновление баланса Спарксов в кнопке шапки HeaderActionBtn без необходимости пересоздания экрана.
- В server.js внедрена защита от захвата токенов привязки (re-link protection), идемпотентность подтверждений и поддержка аутентификации по ключу пользователя.
- В site/link.html согласована длина пароля при регистрации (от 8 символов) и улучшена обработка ошибок 404 (истечение срока ссылки).
- Измененные файлы: src/main/java/naryn/sun/ui/mainmenu/MainMenuHeaderBar.java, src/main/java/naryn/sun/ui/mainmenu/HeaderActionBtn.java, src/main/java/naryn/sun/ui/mainmenu/LinkCodeScreen.java, server/server.js, site/link.html, test_one_click_flow.js

[2026-10-07 21:10] One-Click Client Pairing & Main Menu Bugfix (Бесшовная привязка в 1 клик)
- Устранен конфликт координат клика в MainMenuHeaderBar: кнопка «Войти» больше не перехватывается блоком профиля при незалогиненном состоянии.
- Внедрен механизм One-Click Connect: клик по кнопке «Войти» запрашивает криптостойкий токен в /api/auth/pair/start и автоматически открывает браузер.
- Создана выделенная страница сопряжения site/link.html: мгновенное подтверждение в 1 клик для авторизованных пользователей и форма быстрой регистрации/входа с автоматической привязкой.
- В LinkCodeScreen реализован опрос сервера в фоне, авто-сохранение сессии в SparksManager, звуковое подтверждение и возврат в главное меню с обновленным профилем.
- В server.js добавлены эндпоинты /api/auth/pair/start, /api/auth/pair/info, /api/auth/pair/confirm и обновлен /api/link-status.
- Измененные файлы: src/main/java/naryn/sun/ui/mainmenu/MainMenuHeaderBar.java, src/main/java/naryn/sun/ui/mainmenu/LinkCodeScreen.java, server/server.js, site/link.html

[2026-10-07 20:10] Database Persistence & Admin Panel Fixes
- Устранена проблема со сбросом базы данных при изменениях и git push: файлы `database.json`, `database.backup.json` и `database.sqlite*` исключены из Git-индекса (`git rm --cached`) и добавлены в `.gitignore` и `.dockerignore`.
- Внедрено авто-определение постоянного хранилища (Persistent Volume `/data` или `DATA_DIR`) в `server.js` для предотвращения стирания базы при пересборках контейнеров на Railway / хостинге.
- Добавлена защита от сохранения пустой базы данных в `saveDatabase()`.
- Внедрено полноценное окно редактирования игрока в `admin.html`: ввод точного количества Sparks (с быстрыми кнопками `+10`, `+50`, `+100`, `-50`, `0`), безопасный выбор косметических предметов (крылья, корона, плащ, дракон, НЛО, самолет, корова) без случайного стирания активных предметов.
- Реализованы эндпоинты `/api/admin/export-db` и `/api/admin/import-db` с кнопками скачивания и восстановления бэкапа в 1 клик прямо из панели управления.
- Измененные файлы: server/admin.html, server/server.js, server/.gitignore, server/.dockerignore, .gitignore, .dockerignore

[2026-10-07 11:55] Admin Panel Realtime & Search by ID
- Мгновенное переключение режимов Редактора и Просмотра без задержек и сброса выбранных чекбоксов при авто-обновлении.
- Внедрен эндпоинт /api/web-ping в server.js и регулярный heartbeat на страницах сайта (index.html, profile.html) для точного отображения онлайна в реальном времени.
- Добавлен поиск пользователей по ID (user.uid / key) в поисковую строку админ-панели наряду с логином.
- Такт авто-обновления таблицы админ-панели сокращен до 3 секунд с сохранением выделения.
- Измененные файлы: server/admin.html, server/server.js, site/index.html, site/profile.html

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
- [2026-10-07 04:50] Миграция базы данных на SQLite (Multi-Engine) и подготовка к деплою
  - Разработан гибридный многоуровневый адаптер SQLite в server.js:
    - 1-й приоритет: нативный встроенный модуль Node 22 (node:sqlite / DatabaseSync) — нулевые зависимости, 100% совместимость в контейнерах без C++ сборщиков.
    - 2-й приоритет: высокопроизводительный better-sqlite3 с поддержкой WAL-журналирования.
    - 3-й приоритет: автономный аварийный fallback на database.json при любых непредвиденных ошибках окружения.
  - Настроено безопасное шифрование записей AES-256-CBC, транзакционная запись (BEGIN/COMMIT) и двойное сохранение (SQLite + аварийный бэкап-снапшот в JSON).
  - Обновлены Dockerfile и server/Dockerfile до node:22-slim с безопасным npm install (не блокирующим сборку на Railway).
  - Добавлены optionalDependencies в package.json и server/package.json.
  - Проверена сквозная работоспособность API авторизации (/api/check) и загрузки пользователей из database.sqlite.
- [2026-10-07 11:04] Актуализация AGENTS.md и защита баз данных в Git
  - Добавлено правило исключения живых баз данных (`database.json`, `database.sqlite*`) в `.gitignore` и зафиксировано в протоколе безопасности.
  - Актуализирован Roadmap (задачи 15.8, 15.9, 15.11 переведены в статус "Готово").
  - Задокументирована работа гибридной СУБД SQLite (Node 22 native / better-sqlite3 с AES-256) и привязки по коду без HWID/банов.
  - Изменены файлы: .gitignore, AGENTS.md, CHANGELOG.md.

- [2026-10-07 11:18] Исправление входа в Админ-панель (admin.html)
  - Устранена критическая синтаксическая ошибка JavaScript (`SyntaxError: Unexpected token ';'`) в строке формирования действий пользователя, из-за которой парсинг скрипта полностью блокировался и клик по кнопке входа не вызывал `loginAdmin()`.
  - Устранена ошибка `ReferenceError: gameOnlineCount is not defined` в `renderTable()`.
  - Форма авторизации переведена на стандартную отправку `<form onsubmit="...">` с поддержкой русской подсказки и корректной обработки клавиши Enter.
  - Добавлена интеллектуальная валидация введенных данных: при попытке ввести 6-значный код сопряжения игры или ключ SUN ID выводится понятная подсказка с указанием, что игровой код вводится в `/profile.html`, а для админки нужен мастер-пароль (`sun2026admin`).
- [2026-10-08 10:25] Исправление статуса привязки клиента игры и скрытие блока с 6-значным кодом в профиле
  - Устранена рассинхронизация статуса привязки между игрой и сайтом: эндпоинты авторизации (/api/login, /api/google-auth) в server.js теперь гарантированно возвращают поле clientLinked, а при подтверждении сопряжения выставляются флаги clientLinked и hwid.
  - Реализован эндпоинт /api/auth/pair/unlink для безопасной отвязки клиента игры пользователем из личного кабинета.
  - В site/profile.html добавлена автоматическая проверка статуса через /api/sparks-balance для мгновенного восстановления корректного состояния даже при кешированных ответах сервера.
  - Карточка ввода 6-значного кода и блок с 3 шагами инструкции («Скачайте мод...») теперь автоматически скрываются, когда клиент игры уже привязан, освобождая личный кабинет от визуального шума.
  - Рядом со статусом «Клиент игры: Привязан» добавлена компактная кнопка «Отвязать».
- [2026-10-08 10:31] Полное удаление метода с 6-значным кодом (переход на чистый 1-Click Connect)
  - Из site/profile.html полностью удалена карточка ручного ввода 6-значного кода (#card-pair-game), форма с инпутом кода и устаревшие обработчики fPairClient.
  - Обновлены шаги инструкции в личном кабинете: 3-й шаг теперь описывает мгновенное подтверждение в 1 клик через браузер без ручного ввода цифр.
  - В клиенте (LinkCodeScreen.java) из экрана убрана строчка «Или код вручную», оставлен лаконичный статус One-Click сопряжения.
  - Изменены файлы: site/profile.html, src/main/java/naryn/sun/ui/mainmenu/LinkCodeScreen.java, changelog/CHANGELOG.md.

