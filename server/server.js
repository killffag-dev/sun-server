/**
 * SUN Client — Сервер авторизации и лицензий с красивой Админ-панелью
 * Запуск: node server/server.js
 * Админка доступна в браузере: http://localhost:8080/admin
 */

const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 8080;
const DB_FILE = process.env.DB_FILE || path.join(__dirname, 'database.json');
const DB_BACKUP_FILE = process.env.DB_BACKUP_FILE || path.join(__dirname, 'database.backup.json');
const SITE_DIR = process.env.SITE_DIR || path.resolve(path.join(__dirname, '..', 'site'));
const UPLOADS_DIR = path.join(SITE_DIR, 'uploads');
const AVATARS_DIR = path.join(UPLOADS_DIR, 'avatars');
const BANNERS_DIR = path.join(UPLOADS_DIR, 'banners');

try {
    if (!fs.existsSync(UPLOADS_DIR)) fs.mkdirSync(UPLOADS_DIR, { recursive: true });
    if (!fs.existsSync(AVATARS_DIR)) fs.mkdirSync(AVATARS_DIR, { recursive: true });
    if (!fs.existsSync(BANNERS_DIR)) fs.mkdirSync(BANNERS_DIR, { recursive: true });
} catch (e) {
    console.warn('[SUN-API] Ошибка создания папок загрузок:', e.message);
}

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'sun2026admin';
const SESSION_SECRET = process.env.SESSION_SECRET || 'SUN_SESSION_SECRET_2026_SECURE_KEY_9837418247918237';
// Map of active admin session tokens -> expiration timestamp (ms)
const activeAdminSessions = new Map();

// SUN Connect: Map of pending device pairing requests
// requestId -> { code, createdAt, expiresAt, status: 'pending'|'linked', token, userKey, user }
const activePairRequests = new Map();

function cleanupPairRequests() {
    const now = Date.now();
    for (const [id, req] of activePairRequests.entries()) {
        if (now > req.expiresAt) activePairRequests.delete(id);
    }
}

function timingSafeCompare(a, b) {
    if (typeof a !== 'string' || typeof b !== 'string') return false;
    const bufA = Buffer.from(a);
    const bufB = Buffer.from(b);
    if (bufA.length !== bufB.length) return false;
    return crypto.timingSafeEqual(bufA, bufB);
}

function cleanupAdminSessions() {
    const now = Date.now();
    for (const [tok, exp] of activeAdminSessions.entries()) {
        if (now > exp) activeAdminSessions.delete(tok);
    }
}

function isLocalRequest(req) {
    const rawForwarded = req.headers['x-forwarded-for'];
    const ip = (rawForwarded ? rawForwarded.split(',')[0].trim() : null) || 
               req.headers['cf-connecting-ip'] || 
               req.socket.remoteAddress || '';
    return ip === '127.0.0.1' || ip === '::1' || ip === '::ffff:127.0.0.1' || ip === 'localhost';
}

function isAdminAuthorized(req) {
    if (process.env.ALLOW_REMOTE_ADMIN !== 'true' && !isLocalRequest(req)) {
        return false;
    }
    // Если запрос идёт локально с вашего ПК — полный доступ без ввода паролей
    if (isLocalRequest(req)) {
        return true;
    }
    cleanupAdminSessions();
    const now = Date.now();
    const authHeader = req.headers['authorization'] || '';
    const adminToken = req.headers['x-admin-token'] || '';
    const cookies = req.headers['cookie'] || '';

    const checkToken = (tok) => {
        if (!tok) return false;
        if (timingSafeCompare(tok, ADMIN_PASSWORD)) return true;
        const exp = activeAdminSessions.get(tok);
        return typeof exp === 'number' && now < exp;
    };

    if (checkToken(adminToken)) return true;
    if (authHeader.startsWith('Bearer ')) {
        const token = authHeader.substring(7).trim();
        if (checkToken(token)) return true;
    }
    const match = cookies.match(/sun_admin_token=([^;]+)/);
    if (match && checkToken(match[1])) return true;
    return false;
}

// Password hashing utility with PBKDF2 + per-user salt and backward compatibility
function hashPassword(password, salt) {
    const userSalt = salt || crypto.randomBytes(16).toString('hex');
    const hash = crypto.pbkdf2Sync(password, userSalt, 25000, 32, 'sha256').toString('hex');
    return `pbkdf2$${userSalt}$${hash}`;
}

function verifyPassword(password, storedHash) {
    if (!password || !storedHash) return false;
    if (typeof storedHash === 'string' && storedHash.startsWith('pbkdf2$')) {
        const parts = storedHash.split('$');
        if (parts.length === 3) {
            const salt = parts[1];
            const expectedHash = parts[2];
            const computed = crypto.pbkdf2Sync(password, salt, 25000, 32, 'sha256').toString('hex');
            return timingSafeCompare(computed, expectedHash);
        }
    }
    // Backward compatibility with legacy SHA256 hashes
    const legacyHash = crypto.createHash('sha256').update(password + 'SUN_SECURE_SALT_2026').digest('hex');
    return timingSafeCompare(legacyHash, storedHash);
}

// Генератор криптографически стойкого лицензионного ключа (SUN-XXXX-XXXX)
function generateLicenseKey() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let p1 = '', p2 = '';
    for (let i = 0; i < 4; i++) {
        p1 += chars[crypto.randomInt(0, chars.length)];
        p2 += chars[crypto.randomInt(0, chars.length)];
    }
    return `SUN-${p1}-${p2}`;
}

// Генератор порядкового UID для пользователей (начиная от 10+, 0-9 зарезервированы для владельца)
function getNextUid() {
    let maxUid = 9;
    if (typeof database === 'object' && database !== null) {
        for (const k of Object.keys(database)) {
            const u = database[k];
            if (u && typeof u.uid === 'number' && u.uid > maxUid) {
                maxUid = u.uid;
            }
        }
    }
    return maxUid + 1;
}

// Генератор и валидатор долговечных криптографически подписанных сессий (HMAC-SHA256, 365 дней)
function createSessionToken(userKey, username) {
    const nowSec = Math.floor(Date.now() / 1000);
    const payload = {
        k: userKey,
        u: username,
        iat: nowSec,
        exp: nowSec + (365 * 24 * 60 * 60) // 365 дней сессии для исключения случайных разлогинов
    };
    const b64Payload = Buffer.from(JSON.stringify(payload)).toString('base64url');
    const header = 'sun_s1';
    const data = `${header}.${b64Payload}`;
    const sig = crypto.createHmac('sha256', SESSION_SECRET).update(data).digest('base64url');
    return `${data}.${sig}`;
}

function getUserBySessionToken(token, userKey) {
    if (!token && !userKey) return null;
    if (typeof token === 'string') token = token.trim();
    if (typeof userKey === 'string') userKey = userKey.trim();

    // 1. Проверяем криптографически подписанный токен (sun_s1.<payload>.<sig>)
    if (token && token.startsWith('sun_s1.')) {
        try {
            const parts = token.split('.');
            if (parts.length === 3) {
                const dataPart = `${parts[0]}.${parts[1]}`;
                const expectedSig = crypto.createHmac('sha256', SESSION_SECRET).update(dataPart).digest('base64url');
                if (timingSafeCompare(parts[2], expectedSig)) {
                    const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf-8'));
                    const nowSec = Math.floor(Date.now() / 1000);
                    if (payload && payload.k && payload.exp && payload.exp > nowSec) {
                        const user = database[payload.k];
                        if (user) {
                            return {
                                key: payload.k,
                                user,
                                shouldRenew: (payload.exp - nowSec) < (30 * 24 * 3600)
                            };
                        }
                    }
                }
            }
        } catch (e) {}
    }

    // 2. Обратная совместимость с hex-токенами или сессиями в объекте пользователя
    if (token) {
        const foundKey = Object.keys(database).find(k => 
            database[k].sessionToken && timingSafeCompare(database[k].sessionToken, token)
        );
        if (foundKey) {
            return { key: foundKey, user: database[foundKey], shouldRenew: true };
        }
    }

    // 3. Fallback по ключу пользователя (если токен истек или изменился SESSION_SECRET)
    const directKey = (token && database[token]) ? token : (userKey && database[userKey] ? userKey : null);
    if (directKey) {
        return { key: directKey, user: database[directKey], shouldRenew: true };
    }

    return null;
}

function getCookie(req, name) {
    const cookies = req.headers['cookie'] || '';
    const match = cookies.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]+)'));
    return match ? decodeURIComponent(match[1]) : null;
}

function getSessionCookieHeader(req, token) {
    const isHttps = req.headers['x-forwarded-proto'] === 'https' || (req.connection && req.connection.encrypted);
    const maxAge = token ? (365 * 24 * 60 * 60) : 0;
    const val = token ? encodeURIComponent(token) : '';
    return `sun_session=${val}; Path=/; SameSite=Lax; Max-Age=${maxAge}${isHttps ? '; Secure' : ''}`;
}

// Загрузка или создание базы данных с авто-восстановлением из бэкапа
function loadDatabase() {
    let db = {};
    try {
        if (fs.existsSync(DB_FILE)) {
            const data = fs.readFileSync(DB_FILE, 'utf-8');
            db = JSON.parse(data);
        }
    } catch (e) {
        console.error('[SUN-DB] Ошибка чтения базы данных:', e);
    }

    // Восстанавливаем из резервной копии, если в ней есть данные, которых нет в DB_FILE
    try {
        if (fs.existsSync(DB_BACKUP_FILE)) {
            const backupData = fs.readFileSync(DB_BACKUP_FILE, 'utf-8');
            const backupDb = JSON.parse(backupData);
            for (const k of Object.keys(backupDb)) {
                if (!db[k]) {
                    db[k] = backupDb[k];
                    console.log(`[SUN-DB] Восстановлен аккаунт из резервной копии: ${k}`);
                }
            }
        }
    } catch (e) {
        console.error('[SUN-DB] Ошибка чтения резервной копии базы данных:', e);
    }

    if (Object.keys(db).length === 0) {
        db = {
            "SUN-WALU-DPNK": {
                uid: 10,
                username: "12345678",
                email: "killffag@gmail.com",
                key: "SUN-WALU-DPNK",
                password: "pbkdf2$sun2026salt$180a17a523aa664f1ad2115249b47b0d6266102347a51c4e408745894b30d0a4",
                active: true,
                banned: false,
                created: "2026-10-05",
                expires: "2026-12-31",
                coins: 0,
                hwid: null,
                hwid_last_reset: null,
                cosmetics: ["wings_fire", "crown_gold", "cape_sun"],
                lastSeen: "2026-10-05 16:42"
            }
        };
    }

    // Присваиваем UID всем пользователям, у кого его нет (начиная от 10+, 0-9 зарезервированы)
    let nextUidCounter = 10;
    for (const k of Object.keys(db)) {
        if (db[k] && typeof db[k].uid === 'number' && db[k].uid >= nextUidCounter) {
            nextUidCounter = db[k].uid + 1;
        }
    }
    for (const k of Object.keys(db)) {
        if (db[k] && (typeof db[k].uid !== 'number' || db[k].uid < 10)) {
            db[k].uid = nextUidCounter++;
        }
    }

    saveDatabase(db);
    return db;
}

// Надежное сохранение базы данных в 2 независимых файла для защиты от потери данных
function saveDatabase(db) {
    try {
        const jsonStr = JSON.stringify(db, null, 2);
        fs.writeFileSync(DB_FILE, jsonStr, 'utf-8');
        fs.writeFileSync(DB_BACKUP_FILE, jsonStr, 'utf-8');
    } catch (e) {
        console.error('[SUN-DB] Ошибка сохранения базы данных:', e);
    }
}

const database = loadDatabase();

// HTML-код современной темной админ-панели
const ADMIN_HTML = `<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SUN Client — Панель Управления</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #0b0d13;
            --card-bg: #141722;
            --card-border: #232738;
            --accent: #ff9800;
            --accent-glow: rgba(255, 152, 0, 0.25);
            --text-main: #f0f3fa;
            --text-muted: #8b93a7;
            --green: #10b981;
            --red: #ef4444;
            --blue: #3b82f6;
            --purple: #8b5cf6;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg);
            color: var(--text-main);
            padding: 24px;
            min-height: 100vh;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        /* HEADER */
        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 28px;
            padding-bottom: 20px;
            border-bottom: 1px solid var(--card-border);
        }

        .logo-wrap {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .logo-badge {
            background: linear-gradient(135deg, #ff9800, #ff5722);
            color: #fff;
            font-weight: 800;
            font-size: 18px;
            padding: 8px 14px;
            border-radius: 10px;
            box-shadow: 0 4px 16px var(--accent-glow);
        }

        .logo-text h1 {
            font-size: 20px;
            font-weight: 700;
            letter-spacing: -0.5px;
        }

        .logo-text p {
            font-size: 13px;
            color: var(--text-muted);
        }

        /* STATS CARDS */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
            margin-bottom: 28px;
        }

        .stat-card {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 12px;
            padding: 18px 20px;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .stat-title {
            font-size: 13px;
            color: var(--text-muted);
            text-transform: uppercase;
            font-weight: 600;
            letter-spacing: 0.5px;
        }

        .stat-val {
            font-size: 28px;
            font-weight: 700;
            color: var(--text-main);
        }

        /* TOOLBAR */
        .toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 16px;
            margin-bottom: 16px;
            flex-wrap: wrap;
        }

        .search-box {
            position: relative;
            flex: 1;
            min-width: 260px;
        }

        .search-box input {
            width: 100%;
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            padding: 10px 16px;
            border-radius: 8px;
            color: #fff;
            font-size: 14px;
            outline: none;
            transition: border-color 0.2s;
        }

        .search-box input:focus {
            border-color: var(--accent);
        }

        .btn-add {
            background: var(--accent);
            color: #111;
            font-weight: 600;
            border: none;
            padding: 10px 18px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
            transition: opacity 0.2s;
        }
        .btn-add:hover { opacity: 0.9; }

        /* USERS TABLE */
        .table-wrap {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 12px;
            overflow-x: auto;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
            font-size: 14px;
        }

        th {
            background: rgba(255, 255, 255, 0.02);
            color: var(--text-muted);
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 14px 18px;
            border-bottom: 1px solid var(--card-border);
        }

        td {
            padding: 14px 18px;
            border-bottom: 1px solid var(--card-border);
            vertical-align: middle;
        }

        tr:last-child td { border-bottom: none; }
        tr:hover td { background: rgba(255, 255, 255, 0.015); }

        .hwid-badge {
            font-family: 'JetBrains Mono', monospace;
            background: #1c2130;
            padding: 4px 8px;
            border-radius: 6px;
            font-size: 12px;
            color: #c3cadc;
            border: 1px solid #282f45;
            display: inline-block;
        }

        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
        }

        .status-active { background: rgba(16, 185, 129, 0.15); color: var(--green); border: 1px solid rgba(16, 185, 129, 0.3); }
        .status-expired { background: rgba(239, 68, 68, 0.15); color: var(--red); border: 1px solid rgba(239, 68, 68, 0.3); }
        .status-banned { background: rgba(139, 92, 246, 0.15); color: #c084fc; border: 1px solid rgba(139, 92, 246, 0.3); }

        .tag {
            background: #252b3d;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 11px;
            margin-right: 4px;
            display: inline-block;
            margin-bottom: 2px;
        }

        /* ACTIONS */
        .actions-cell {
            display: flex;
            gap: 6px;
            align-items: center;
        }

        .action-btn {
            background: #202637;
            border: 1px solid #2e364e;
            color: #d1d7e5;
            padding: 6px 10px;
            border-radius: 6px;
            font-size: 12px;
            cursor: pointer;
            transition: all 0.15s;
        }

        .action-btn:hover {
            background: #2e364e;
            color: #fff;
        }

        .action-btn.ban { color: #f87171; border-color: rgba(239, 68, 68, 0.3); }
        .action-btn.ban:hover { background: rgba(239, 68, 68, 0.2); }

        /* MODAL */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.7);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 1000;
        }
        .modal {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            width: 440px;
            border-radius: 12px;
            padding: 24px;
        }
        .modal h2 { font-size: 18px; margin-bottom: 16px; }
        .modal input, .modal select {
            width: 100%;
            background: #0f121b;
            border: 1px solid var(--card-border);
            padding: 10px 12px;
            border-radius: 6px;
            color: #fff;
            margin-bottom: 12px;
            font-size: 14px;
            outline: none;
        }
        .modal-buttons {
            display: flex;
            justify-content: flex-end;
            gap: 8px;
            margin-top: 16px;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="logo-wrap">
                <div class="logo-badge">SUN</div>
                <div class="logo-text">
                    <h1>Панель Управления Клиентом</h1>
                    <p>Управление подписками, HWID и гардеробом</p>
                </div>
            </div>
            <div style="display: flex; align-items: center; gap: 14px;">
                <div>
                    <span style="font-size: 13px; color: var(--text-muted);">Статус:</span>
                    <span style="color: var(--green); font-weight: 600; font-size: 13px;"><span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:var(--green);margin-right:4px;"></span>Локальный доступ</span>
                </div>
            </div>
        </header>

        <div class="stats-grid">
            <div class="stat-card">
                <span class="stat-title">Всего пользователей</span>
                <span class="stat-val" id="stat-total">0</span>
            </div>
            <div class="stat-card">
                <span class="stat-title">Активных подписок</span>
                <span class="stat-val" id="stat-active" style="color: var(--green);">0</span>
            </div>
            <div class="stat-card">
                <span class="stat-title">Заблокировано</span>
                <span class="stat-val" id="stat-banned" style="color: var(--red);">0</span>
            </div>
        </div>

        <div class="toolbar">
            <div class="search-box">
                <input type="text" id="searchInput" placeholder="Поиск по нику или HWID..." oninput="filterUsers()">
            </div>
            <button class="btn-add" onclick="openAddModal()">+ Добавить пользователя</button>
        </div>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Пользователь</th>
                        <th>Ключ (SUN ID)</th>
                        <th>Привязка HWID</th>
                        <th>Искры (Sparks)</th>
                        <th>Статус</th>
                        <th>Истекает</th>
                        <th>Косметика</th>
                        <th>Действия</th>
                    </tr>
                </thead>
                <tbody id="usersTableBody">
                    <!-- Заполняется динамически -->
                </tbody>
            </table>
        </div>
    </div>

    <!-- Модальное окно добавления -->
    <div class="modal-overlay" id="addModal">
        <div class="modal">
            <h2>Добавить / Активировать пользователя</h2>
            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">Никнейм</label>
            <input type="text" id="newUsername" placeholder="Например: CoolPlayer">

            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">HWID (если есть, или сгенерируется сам)</label>
            <input type="text" id="newHwid" placeholder="SUN-XXXX-YYYY">

            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">Срок подписки</label>
            <select id="newDuration">
                <option value="7">7 дней</option>
                <option value="30" selected>30 дней (1 месяц)</option>
                <option value="90">90 дней (3 месяца)</option>
                <option value="365">1 год</option>
            </select>

            <div class="modal-buttons">
                <button class="action-btn" onclick="closeAddModal()">Отмена</button>
                <button class="btn-add" onclick="submitAddUser()">Сохранить</button>
            </div>
        </div>
    </div>



    <script>
        let allUsers = {};

        async function loadUsers() {
            try {
                const res = await fetch('/api/admin/users');
                if (res.ok) {
                    allUsers = await res.json();
                    renderTable();
                }
            } catch (e) {
                console.error('Ошибка загрузки пользователей', e);
            }
        }

        function esc(str) {
            if (!str) return '';
            return String(str).replace(/[&<>"']/g, function(m) {
                return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m];
            });
        }

        function renderTable() {
            const tbody = document.getElementById('usersTableBody');
            const search = document.getElementById('searchInput').value.toLowerCase();
            tbody.innerHTML = '';

            let total = 0;
            let activeCount = 0;
            let bannedCount = 0;

            const now = new Date();

            Object.entries(allUsers).forEach(([key, user]) => {
                total++;
                const isBanned = !!user.banned;
                const isExpired = new Date(user.expires) < now;
                const isActive = user.active && !isBanned && !isExpired;

                if (isActive) activeCount++;
                if (isBanned) bannedCount++;

                // Фильтр поиска
                if (search && !(user.username || '').toLowerCase().includes(search) && !key.toLowerCase().includes(search) && !(user.hwid && user.hwid.toLowerCase().includes(search))) {
                    return;
                }

                let statusBadge = '<span class="status-badge status-active"><span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:var(--green);margin-right:5px;"></span>Активна</span>';
                if (isBanned) {
                    statusBadge = '<span class="status-badge status-banned"><span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:var(--red);margin-right:5px;"></span>Забанен</span>';
                } else if (isExpired) {
                    statusBadge = '<span class="status-badge status-expired"><span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:var(--yellow);margin-right:5px;"></span>Истекла</span>';
                }

                const safeUsername = esc(user.username || 'User');
                const safeEmail = user.email ? '<div style="font-size:11px;color:var(--text-muted);">' + esc(user.email) + '</div>' : '';
                const safeKey = esc(key);
                const safeExpires = esc(user.expires || '');
                const safeCoins = Number(user.coins) || 0;

                let hwidBadge = user.hwid 
                    ? ('<span style="color:#10b981;font-size:12px;font-family:monospace;" title="' + esc(user.hwid) + '"><span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:#10b981;margin-right:5px;"></span>' + esc(user.hwid.substring(0, 14)) + '...</span>')
                    : '<span style="color:#8b93a7;font-size:12px;"><span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:#f59e0b;margin-right:5px;"></span>Не привязан</span>';

                const cosmeticsHtml = (Array.isArray(user.cosmetics) ? user.cosmetics : []).map(c => '<span class="tag">' + esc(c) + '</span>').join('') || '<span style="color:#555">нет</span>';
                const resetHwidBtn = user.hwid ? ('<button class="action-btn" title="Сбросить привязку HWID" onclick="resetHwid(\'' + safeKey + '\')">Сброс HWID</button>') : '';

                const tr = document.createElement('tr');
                tr.innerHTML = \`
                    <td>
                        <strong>\${safeUsername}</strong>
                        \${safeEmail}
                    </td>
                    <td><span class="hwid-badge">\${safeKey}</span></td>
                    <td>\${hwidBadge}</td>
                    <td><strong style="color:var(--accent);">\${safeCoins} Sparks</strong></td>
                    <td>\${statusBadge}</td>
                    <td>\${safeExpires}</td>
                    <td>\${cosmeticsHtml}</td>
                    <td>
                        <div class="actions-cell">
                            <button class="action-btn" title="Начислить 50 Sparks" onclick="addCoins('\${safeKey}', 50)">+50 Sparks</button>
                            \${resetHwidBtn}
                            <button class="action-btn" title="Продлить на 30 дней" onclick="extendDays('\${safeKey}', 30)">+30д</button>
                            <button class="action-btn" title="Выдать/забрать косметику" onclick="toggleCosmetic('\${safeKey}')">Косметика</button>
                            <button class="action-btn ban" onclick="toggleBan('\${safeKey}', \${!isBanned})">\${isBanned ? 'Разбан' : 'Бан'}</button>
                            <button class="action-btn" title="Удалить пользователя" onclick="deleteUser('\${safeKey}')">Удалить</button>
                        </div>
                    </td>
                \`;
                tbody.appendChild(tr);
            });

            document.getElementById('stat-total').innerText = total;
            document.getElementById('stat-active').innerText = activeCount;
            document.getElementById('stat-banned').innerText = bannedCount;
        }

        function filterUsers() {
            renderTable();
        }

        async function sendAction(data) {
            const token = getAuthToken();
            const res = await fetch('/api/admin/action', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    ...(token ? { 'X-Admin-Token': token } : {})
                },
                body: JSON.stringify(data)
            });
            if (res.status === 401) {
                document.getElementById('authModal').style.display = 'flex';
                return;
            }
            loadUsers();
        }

        function addCoins(hwid, amount) {
            sendAction({ action: 'add_coins', hwid, amount });
        }

        function resetHwid(hwid) {
            if (confirm('Сбросить привязку HWID для ' + hwid + '?')) {
                sendAction({ action: 'reset_hwid', hwid });
            }
        }

        function extendDays(hwid, days) {
            sendAction({ action: 'extend', hwid, days });
        }

        function toggleBan(hwid, banState) {
            sendAction({ action: 'ban', hwid, banned: banState });
        }

        function toggleCosmetic(hwid) {
            sendAction({ action: 'toggle_cosmetics', hwid });
        }

        function deleteUser(hwid) {
            if (confirm('Точно удалить запись ' + hwid + '?')) {
                sendAction({ action: 'delete', hwid });
            }
        }

        function openAddModal() {
            document.getElementById('addModal').style.display = 'flex';
        }

        function closeAddModal() {
            document.getElementById('addModal').style.display = 'none';
        }

        async function submitAddUser() {
            const username = document.getElementById('newUsername').value.trim() || 'User';
            let hwid = document.getElementById('newHwid').value.trim();
            const days = parseInt(document.getElementById('newDuration').value);

            if (!hwid) {
                hwid = 'SUN-' + Math.random().toString(36).substring(2, 6).toUpperCase() + '-' + Math.random().toString(36).substring(2, 6).toUpperCase();
            }

            await sendAction({ action: 'create', username, hwid, days });
            closeAddModal();
        }

        // Авто-обновление раз в 3 секунды
        loadUsers();
        setInterval(loadUsers, 3000);
    </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    // === SECURITY MODULE (Anti-DDoS & Headers) ===
    const rawForwarded = req.headers['x-forwarded-for'];
    const clientIp = (req.headers['cf-connecting-ip'] || 
                      (rawForwarded ? rawForwarded.split(',')[0].trim() : null) || 
                      req.socket.remoteAddress || 'unknown');
    
    // 1. Security Headers (Helmet equivalent)
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    res.setHeader('Content-Security-Policy', "default-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com https://challenges.cloudflare.com; script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com; frame-src https://challenges.cloudflare.com; img-src 'self' data: https:;");
    
    // 2. IP Rate Limiting (per real client IP)
    if (!global.rateLimits) global.rateLimits = new Map();
    const now = Date.now();
    const windowMs = 60000; // 1 min window
    const maxRequests = 120; // 120 reqs / min
    
    let rl = global.rateLimits.get(clientIp) || { count: 0, startTime: now };
    if (now - rl.startTime > windowMs) {
        rl = { count: 1, startTime: now };
    } else {
        rl.count++;
    }
    global.rateLimits.set(clientIp, rl);
    
    if (Math.random() < 0.05) { // Garbage collection for rate limits
        for (const [ip, data] of global.rateLimits.entries()) {
            if (now - data.startTime > windowMs) global.rateLimits.delete(ip);
        }
    }
    
    if (rl.count > maxRequests) {
        res.writeHead(429, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Too Many Requests. Rate limit exceeded.' }));
    }
    // === END SECURITY MODULE ===

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        return res.end();
    }

    const parsedUrl = url.parse(req.url, true);

    // 0.5 АВТОРИЗАЦИЯ АДМИНИСТРАТОРА (Разрешена только локально)
    if (parsedUrl.pathname === '/api/admin/login' && req.method === 'POST') {
        if (process.env.ALLOW_REMOTE_ADMIN !== 'true' && !isLocalRequest(req)) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
            return res.end('404 Not Found');
        }
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                if (data.password && timingSafeCompare(data.password, ADMIN_PASSWORD)) {
                    const token = crypto.randomBytes(32).toString('hex');
                    activeAdminSessions.set(token, Date.now() + 86400000); // 24 часа
                    const isHttps = req.headers['x-forwarded-proto'] === 'https' || (req.connection && req.connection.encrypted);
                    res.writeHead(200, {
                        'Content-Type': 'application/json; charset=utf-8',
                        'Set-Cookie': `sun_admin_token=${token}; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400${isHttps ? '; Secure' : ''}`
                    });
                    return res.end(JSON.stringify({ success: true, token }));
                } else {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ success: false, error: 'Неверный пароль администратора' }));
                }
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    if (parsedUrl.pathname === '/api/admin/logout' && req.method === 'POST') {
        const cookies = req.headers['cookie'] || '';
        const match = cookies.match(/sun_admin_token=([^;]+)/);
        if (match) activeAdminSessions.delete(match[1]);
        const adminToken = req.headers['x-admin-token'] || '';
        if (adminToken) activeAdminSessions.delete(adminToken);
        const isHttps = req.headers['x-forwarded-proto'] === 'https' || (req.connection && req.connection.encrypted);
        res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Set-Cookie': `sun_admin_token=; Path=/; HttpOnly; Max-Age=0${isHttps ? '; Secure' : ''}`
        });
        return res.end(JSON.stringify({ success: true }));
    }

    // 1. АДМИН-ПАНЕЛЬ (Разрешена только локально на вашем ПК для 100% безопасности)
    if (parsedUrl.pathname === '/admin' || parsedUrl.pathname === '/admin/') {
        if (process.env.ALLOW_REMOTE_ADMIN !== 'true' && !isLocalRequest(req)) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
            return res.end('404 Not Found');
        }
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end(ADMIN_HTML);
    }

    // 2. API ДЛЯ АДМИНКИ: Получение списка пользователей (строго защищено паролем)
    if (parsedUrl.pathname === '/api/admin/users' && req.method === 'GET') {
        if (!isAdminAuthorized(req)) {
            res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: 'Unauthorized: Требуется авторизация администратора' }));
        }
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify(database));
    }

    // 3. API ДЛЯ АДМИНКИ: Действия над пользователями (строго защищено паролем)
    if (parsedUrl.pathname === '/api/admin/action' && req.method === 'POST') {
        if (!isAdminAuthorized(req)) {
            res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: 'Unauthorized: Требуется авторизация администратора' }));
        }
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 512) { // 512KB payload limit
                req.destroy();
            }
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const { action, hwid } = data;

                if (action === 'extend' && database[hwid]) {
                    const currentExp = new Date(database[hwid].expires);
                    const baseDate = currentExp > new Date() ? currentExp : new Date();
                    baseDate.setDate(baseDate.getDate() + (data.days || 30));
                    database[hwid].expires = baseDate.toISOString().split('T')[0];
                    database[hwid].active = true;
                    saveDatabase(database);
                } else if (action === 'ban' && database[hwid]) {
                    database[hwid].banned = !!data.banned;
                    saveDatabase(database);
                } else if (action === 'toggle_cosmetics' && database[hwid]) {
                    const list = database[hwid].cosmetics || [];
                    if (list.includes("wings_fire")) {
                        database[hwid].cosmetics = [];
                    } else {
                        database[hwid].cosmetics = ["wings_fire", "crown_gold", "cape_sun"];
                    }
                    saveDatabase(database);
                } else if (action === 'delete') {
                    delete database[hwid];
                    saveDatabase(database);
                } else if (action === 'reset_hwid' && database[hwid]) {
                    database[hwid].hwid = null;
                    database[hwid].hwid_last_reset = null;
                    saveDatabase(database);
                } else if (action === 'add_coins' && database[hwid]) {
                    database[hwid].coins = (database[hwid].coins || 0) + (parseInt(data.amount) || 50);
                    saveDatabase(database);
                } else if (action === 'create') {
                    const key = (data.hwid && data.hwid.trim()) ? data.hwid.trim() : generateLicenseKey();
                    const exp = new Date();
                    exp.setDate(exp.getDate() + (data.days || 30));
                    database[key] = {
                        uid: getNextUid(),
                        username: data.username || 'User',
                        key: key,
                        active: true,
                        banned: false,
                        created: new Date().toISOString().split('T')[0],
                        expires: exp.toISOString().split('T')[0],
                        coins: 0,
                        hwid: null,
                        hwid_last_reset: null,
                        cosmetics: ["wings_fire", "crown_gold", "cape_sun"]
                    };
                    saveDatabase(database);
                }

                res.writeHead(200, { 'Content-Type': 'application/json' });
                return res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(400);
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    // 3.5 API САЙТА: Регистрация нового аккаунта SUN ID (Никнейм + Почта + Пароль)
    if (parsedUrl.pathname === '/api/register' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) { // 64KB payload limit
                req.destroy();
            }
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const username = (data.username || '').trim();
                const email = (data.email || '').trim().toLowerCase();
                const password = (data.password || '').trim();

                // Строгая валидация формата и длины
                if (!username || username.length < 3 || username.length > 24 || !/^[a-zA-Z0-9_\u0400-\u04FF]+$/.test(username)) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Никнейм должен содержать от 3 до 24 символов (буквы, цифры, _)" }));
                }
                if (email && (email.length > 64 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Введите корректный E-mail адрес" }));
                }
                if (!password || password.length < 8 || password.length > 128) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Пароль должен содержать от 8 до 128 символов" }));
                }

                // Проверяем, существует ли уже пользователь с таким ником или email
                let existingHwid = Object.keys(database).find(k => 
                    (database[k].username && database[k].username.toLowerCase() === username.toLowerCase()) ||
                    (email && database[k].email && database[k].email.toLowerCase() === email)
                );

                if (existingHwid) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Пользователь с таким ником или почтой уже зарегистрирован" }));
                }

                const newUid = getNextUid();
                const accountId = generateLicenseKey();
                const sessionToken = createSessionToken(accountId, username);
                const exp = new Date();
                exp.setDate(exp.getDate() + 60); // 60 дней бета-теста

                database[accountId] = {
                    uid: newUid,
                    username: username,
                    email: email,
                    key: accountId,
                    password: hashPassword(password),
                    sessionToken: sessionToken,
                    avatarUrl: null,
                    bannerUrl: null,
                    active: true,
                    banned: false,
                    created: new Date().toISOString().split('T')[0],
                    expires: exp.toISOString().split('T')[0],
                    coins: 0,
                    hwid: null,
                    hwid_last_reset: null,
                    cosmetics: ["wings_fire", "crown_gold", "cape_sun"]
                };
                saveDatabase(database);

                res.writeHead(200, {
                    'Content-Type': 'application/json; charset=utf-8',
                    'Set-Cookie': getSessionCookieHeader(req, sessionToken)
                });
                return res.end(JSON.stringify({
                    success: true,
                    uid: newUid,
                    username: username,
                    email: email,
                    key: accountId,
                    sessionToken: sessionToken,
                    avatarUrl: null,
                    bannerUrl: null,
                    coins: 0,
                    hwid: null,
                    hwid_last_reset: null,
                    expires: exp.toISOString().split('T')[0],
                    plan: "Бета-тест (60 дней)",
                    cosmetics: ["wings_fire", "crown_gold", "cape_sun"]
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: "Некорректные данные запроса" }));
            }
        });
        return;
    }

    // 3.55 API САЙТА: Вход через Google (GIS Token)
    if (parsedUrl.pathname === '/api/google-auth' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const credential = (data.credential || '').trim();

                if (!credential) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ 
                        error: "Авторизация через Google находится на модерации Google Cloud. Пожалуйста, используйте стандартный вход или регистрацию." 
                    }));
                }

                // Декодируем JWT токен Google Identity Services
                const parts = credential.split('.');
                if (parts.length !== 3) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Некорректный формат токена Google" }));
                }

                const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf8'));
                if (!payload || !payload.email) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Не удалось получить профиль из токена Google" }));
                }

                const googleEmail = payload.email.trim().toLowerCase();
                const googleSub = String(payload.sub || '');
                const googleName = (payload.name || payload.given_name || googleEmail.split('@')[0]).replace(/[^a-zA-Z0-9_\u0400-\u04FF]/g, '_').substring(0, 20);

                let foundKey = Object.keys(database).find(k => 
                    (googleSub && database[k].googleSub && database[k].googleSub === googleSub) ||
                    (database[k].email && database[k].email.toLowerCase() === googleEmail)
                );

                let user;
                let userKey;

                if (foundKey) {
                    userKey = foundKey;
                    user = database[foundKey];
                    if (googleSub) user.googleSub = googleSub;
                    user.sessionToken = createSessionToken(userKey, user.username);
                } else {
                    userKey = generateLicenseKey();
                    const exp = new Date();
                    exp.setDate(exp.getDate() + 60);

                    const username = googleName || ("User_" + userKey.substring(4, 8));
                    const sessionToken = createSessionToken(userKey, username);

                    user = {
                        uid: getNextUid(),
                        username: username,
                        email: googleEmail,
                        key: userKey,
                        googleSub: googleSub,
                        sessionToken: sessionToken,
                        provider: "google",
                        active: true,
                        banned: false,
                        created: new Date().toISOString().split('T')[0],
                        expires: exp.toISOString().split('T')[0],
                        coins: 0,
                        hwid: null,
                        hwid_last_reset: null,
                        cosmetics: ["wings_fire", "crown_gold", "cape_sun"]
                    };
                    database[userKey] = user;
                }
                saveDatabase(database);

                res.writeHead(200, {
                    'Content-Type': 'application/json; charset=utf-8',
                    'Set-Cookie': getSessionCookieHeader(req, user.sessionToken)
                });
                return res.end(JSON.stringify({
                    success: true,
                    uid: user.uid || 10,
                    username: user.username,
                    email: user.email,
                    key: userKey,
                    sessionToken: user.sessionToken,
                    avatarUrl: user.avatarUrl || null,
                    bannerUrl: user.bannerUrl || null,
                    coins: user.coins || 0,
                    hwid: user.hwid || null,
                    hwid_last_reset: user.hwid_last_reset || null,
                    expires: user.expires,
                    active: user.active && !user.banned,
                    cosmetics: user.cosmetics || ["wings_fire", "crown_gold", "cape_sun"]
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: "Ошибка обработки Google-авторизации" }));
            }
        });
        return;
    }

    // 3.6 API САЙТА: Вход в личный кабинет (по сессии, ключу или логину/паролю)
    if (parsedUrl.pathname === '/api/login' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const reqToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const reqKey = (data.userKey || data.key || getCookie(req, 'sun_key') || '').trim();

                // А. Вход по сохраненному токену сессии или ключу
                if (reqToken || reqKey) {
                    const auth = getUserBySessionToken(reqToken, reqKey);
                    if (auth) {
                        if (auth.user.banned) {
                            res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
                            return res.end(JSON.stringify({ error: "Ваш аккаунт заблокирован" }));
                        }
                        const u = auth.user;
                        const foundKey = auth.key;
                        let sessionToken = reqToken;
                        if (auth.shouldRenew || !reqToken || !reqToken.startsWith('sun_s1.')) {
                            sessionToken = createSessionToken(foundKey, u.username);
                            u.sessionToken = sessionToken;
                            saveDatabase(database);
                        }
                        res.writeHead(200, {
                            'Content-Type': 'application/json; charset=utf-8',
                            'Set-Cookie': getSessionCookieHeader(req, sessionToken)
                        });
                        return res.end(JSON.stringify({
                            success: true,
                            uid: u.uid || 10,
                            username: u.username,
                            email: u.email,
                            key: foundKey,
                            sessionToken: sessionToken,
                            avatarUrl: u.avatarUrl || null,
                            bannerUrl: u.bannerUrl || null,
                            coins: u.coins || 0,
                            hwid: u.hwid || null,
                            hwid_last_reset: u.hwid_last_reset || null,
                            expires: u.expires,
                            active: u.active && !u.banned,
                            cosmetics: u.cosmetics || ["wings_fire", "crown_gold", "cape_sun"]
                        }));
                    }
                    if (!data.query && !data.username && !data.email) {
                        res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({ error: "Сессия истекла, войдите заново" }));
                    }
                }

                // Б. Обычный вход по логину/почте/UID + обязательному паролю
                const query = (data.query || data.username || data.email || '').trim();
                const password = (data.password || '').trim();

                if (!query) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Введите ваш никнейм, почту или UID" }));
                }

                let foundKey = Object.keys(database).find(k => 
                    k.toLowerCase() === query.toLowerCase() || 
                    (database[k].username && database[k].username.toLowerCase() === query.toLowerCase()) ||
                    (database[k].email && database[k].email.toLowerCase() === query.toLowerCase()) ||
                    (database[k].uid !== undefined && String(database[k].uid) === query)
                );

                if (!foundKey) {
                    res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Аккаунт не найден. Проверьте данные или создайте новый." }));
                }

                const u = database[foundKey];

                if (!u.password) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Для этого аккаунта не установлен пароль. Войдите через Google." }));
                }

                if (!password || !verifyPassword(password, u.password)) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Неверный пароль" }));
                }

                // Выдаем долговечный подписанный сессионный токен при успешном логине
                const sessionToken = createSessionToken(foundKey, u.username);
                u.sessionToken = sessionToken;
                saveDatabase(database);

                res.writeHead(200, {
                    'Content-Type': 'application/json; charset=utf-8',
                    'Set-Cookie': getSessionCookieHeader(req, sessionToken)
                });
                return res.end(JSON.stringify({
                    success: true,
                    uid: u.uid || 10,
                    username: u.username,
                    email: u.email,
                    key: foundKey,
                    sessionToken: sessionToken,
                    avatarUrl: u.avatarUrl || null,
                    bannerUrl: u.bannerUrl || null,
                    coins: u.coins || 0,
                    hwid: u.hwid || null,
                    hwid_last_reset: u.hwid_last_reset || null,
                    expires: u.expires,
                    active: u.active && !u.banned,
                    cosmetics: u.cosmetics || ["wings_fire", "crown_gold", "cape_sun"]
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: "Некорректные данные запроса" }));
            }
        });
        return;
    }

    // 3.65 API САЙТА: Выход из профиля
    if (parsedUrl.pathname === '/api/logout' && req.method === 'POST') {
        res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Set-Cookie': getSessionCookieHeader(req, null)
        });
        return res.end(JSON.stringify({ success: true }));
    }

    // 3.66 SUN Connect API: Запрос кода сопряжения (вызывается клиентом Minecraft)
    if (parsedUrl.pathname === '/api/auth/pair/request' && (req.method === 'POST' || req.method === 'GET')) {
        cleanupPairRequests();
        const requestId = crypto.randomBytes(16).toString('hex');
        // Генерируем 6-значный пин-код (например, 749218)
        const codeNum = crypto.randomInt(100000, 999999).toString();
        const code = `${codeNum.substring(0, 3)}-${codeNum.substring(3)}`;
        const now = Date.now();
        const expiresAt = now + 5 * 60 * 1000; // 5 минут

        activePairRequests.set(requestId, {
            id: requestId,
            code: code,
            cleanCode: codeNum,
            createdAt: now,
            expiresAt: expiresAt,
            status: 'pending',
            token: null,
            userKey: null,
            user: null
        });

        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify({
            success: true,
            requestId: requestId,
            code: code,
            expiresInSeconds: 300
        }));
    }

    // 3.67 SUN Connect API: Подтверждение кода сопряжения на сайте (пользователь вводит код в ЛК)
    if (parsedUrl.pathname === '/api/auth/pair/link' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                cleanupPairRequests();
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const inputCode = (data.code || '').trim().replace(/[^0-9]/g, '');

                const auth = getUserBySessionToken(sessionToken);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Авторизуйтесь на сайте перед привязкой игры" }));
                }

                if (!inputCode || inputCode.length !== 6) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Введите 6-значный код сопряжения из игры" }));
                }

                let foundPair = null;
                for (const pair of activePairRequests.values()) {
                    if (pair.cleanCode === inputCode && pair.status === 'pending') {
                        foundPair = pair;
                        break;
                    }
                }

                if (!foundPair) {
                    res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Код не найден или срок его действия истек" }));
                }

                const u = auth.user;
                const foundKey = auth.key;
                const newClientToken = createSessionToken(foundKey, u.username);

                foundPair.status = 'linked';
                foundPair.token = newClientToken;
                foundPair.userKey = foundKey;
                foundPair.user = {
                    uid: u.uid || 10,
                    username: u.username,
                    coins: u.coins || 0,
                    cosmetics: u.cosmetics || ["wings_fire", "crown_gold", "cape_sun"]
                };

                console.log(`[SUN-CONNECT] Игрок ${u.username} успешно связал клиент через код ${foundPair.code}!`);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    message: "Клиент игры успешно привязан к вашему аккаунту SUN!",
                    username: u.username
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message || "Ошибка привязки" }));
            }
        });
        return;
    }

    // 3.68 SUN Connect API: Опрос статуса клиентом игры (poll раз в 2 сек)
    if (parsedUrl.pathname === '/api/auth/pair/poll' && (req.method === 'GET' || req.method === 'POST')) {
        cleanupPairRequests();
        const requestId = (parsedUrl.query.id || parsedUrl.query.requestId || '').trim();
        if (!requestId || !activePairRequests.has(requestId)) {
            res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: "Сессия сопряжения не найдена или истекла" }));
        }

        const pair = activePairRequests.get(requestId);
        if (pair.status === 'linked') {
            const resultData = {
                linked: true,
                token: pair.token,
                user: pair.user
            };
            activePairRequests.delete(requestId);
            res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify(resultData));
        }

        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify({ linked: false, status: 'pending' }));
    }

    // 3.7 API САЙТА: Сброс привязки HWID пользователем (с кулдауном 3 дня и защитой авторизацией)
    if (parsedUrl.pathname === '/api/reset-hwid' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const key = (data.key || '').trim();
                const password = (data.password || '').trim();

                let auth = sessionToken ? getUserBySessionToken(sessionToken) : null;
                let foundKey = auth ? auth.key : null;
                let user = auth ? auth.user : null;

                if (!user && key) {
                    foundKey = Object.keys(database).find(k => 
                        k.toUpperCase() === key.toUpperCase() || 
                        (database[k].key && database[k].key.toUpperCase() === key.toUpperCase())
                    );
                    if (foundKey && database[foundKey].password && verifyPassword(password, database[foundKey].password)) {
                        user = database[foundKey];
                    }
                }

                if (!user) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Требуется авторизация для сброса привязки" }));
                }

                if (user.hwid_last_reset) {
                    const lastReset = new Date(user.hwid_last_reset).getTime();
                    const cooldownMs = 3 * 24 * 60 * 60 * 1000; // 3 дня
                    const timePassed = Date.now() - lastReset;
                    if (timePassed < cooldownMs) {
                        const hoursLeft = Math.ceil((cooldownMs - timePassed) / (1000 * 60 * 60));
                        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({ 
                            error: `Сброс HWID доступен раз в 3 дня. До следующего сброса осталось: ${hoursLeft} ч.` 
                        }));
                    }
                }

                user.hwid = null;
                user.hwid_last_reset = new Date().toISOString();
                saveDatabase(database);
                console.log(`[SUN-API] [RESET-HWID] Сброс HWID для пользователя ${user.username} (${foundKey})`);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    message: "Привязка железа успешно сброшена! При следующем запуске ключ привяжется к вашему новому ПК."
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    // 3.8 API САЙТА: Смена пароля пользователем
    if (parsedUrl.pathname === '/api/change-password' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const currentPassword = (data.currentPassword || '').trim();
                const newPassword = (data.newPassword || '').trim();

                const auth = getUserBySessionToken(sessionToken);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Сессия не найдена или устарела" }));
                }

                const foundKey = auth.key;
                const user = auth.user;

                if (user.password && (!currentPassword || !verifyPassword(currentPassword, user.password))) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Неверный текущий пароль" }));
                }

                if (!newPassword || newPassword.length < 8 || newPassword.length > 128) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Новый пароль должен содержать от 8 до 128 символов" }));
                }

                user.password = hashPassword(newPassword);
                const newSessionToken = createSessionToken(foundKey, user.username);
                user.sessionToken = newSessionToken;
                saveDatabase(database);

                res.writeHead(200, {
                    'Content-Type': 'application/json; charset=utf-8',
                    'Set-Cookie': getSessionCookieHeader(req, newSessionToken)
                });
                return res.end(JSON.stringify({
                    success: true,
                    message: "Пароль успешно обновлен!",
                    sessionToken: newSessionToken
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    // 3.9 API САЙТА: Активация ключа / кода
    if (parsedUrl.pathname === '/api/activate-key' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const keyInput = (data.key || '').trim().toUpperCase();

                const auth = getUserBySessionToken(sessionToken);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Сессия недействительна" }));
                }

                const foundKey = auth.key;
                const user = auth.user;

                if (!keyInput) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Введите ключ для активации" }));
                }

                // Продление подписки на 30 дней и начисление бонуса
                const currentExp = new Date(user.expires && new Date(user.expires) > new Date() ? user.expires : new Date());
                currentExp.setDate(currentExp.getDate() + 30);
                user.expires = currentExp.toISOString().split('T')[0];
                user.coins = (user.coins || 0) + 50;
                user.active = true;
                saveDatabase(database);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    message: "Ключ активирован! Подписка продлена на 30 дней, начислено +50 Sparks.",
                    expires: user.expires,
                    coins: user.coins
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    // 3.92 API САЙТА: Загрузка аватарки пользователя (видна всем)
    if (parsedUrl.pathname === '/api/upload-avatar' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 1024 * 10) req.destroy(); // 10MB limit
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const auth = getUserBySessionToken(sessionToken);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Сессия недействительна или истекла" }));
                }

                const user = auth.user;
                const foundKey = auth.key;
                const rawImage = (data.imageBase64 || data.image || '').trim();

                if (!rawImage) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Изображение не передано" }));
                }

                let ext = 'png';
                let base64Data = rawImage;
                const match = rawImage.match(/^data:image\/(png|jpeg|jpg|webp|gif);base64,(.+)$/i);
                if (match) {
                    ext = match[1].toLowerCase() === 'jpeg' ? 'jpg' : match[1].toLowerCase();
                    base64Data = match[2];
                }

                const buffer = Buffer.from(base64Data, 'base64');
                if (buffer.length > 5 * 1024 * 1024) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Размер изображения не должен превышать 5 МБ" }));
                }

                const safeKey = foundKey.replace(/[^a-zA-Z0-9_-]/g, '_');
                const fileName = `${safeKey}_avatar_${Date.now()}.${ext}`;
                const savePath = path.join(AVATARS_DIR, fileName);
                fs.writeFileSync(savePath, buffer);

                if (user.avatarUrl && user.avatarUrl.startsWith('/uploads/avatars/')) {
                    const oldPath = path.join(SITE_DIR, user.avatarUrl);
                    try { if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath); } catch (e) {}
                }

                const avatarUrl = `/uploads/avatars/${fileName}`;
                user.avatarUrl = avatarUrl;
                saveDatabase(database);
                console.log(`[SUN-API] [AVATAR] Пользователь ${user.username} обновил аватарку -> ${avatarUrl}`);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ success: true, avatarUrl }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message || "Ошибка загрузки аватарки" }));
            }
        });
        return;
    }

    // 3.94 API САЙТА: Загрузка фона профиля (баннера, виден всем)
    if (parsedUrl.pathname === '/api/upload-banner' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 1024 * 10) req.destroy(); // 10MB limit
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const auth = getUserBySessionToken(sessionToken);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Сессия недействительна или истекла" }));
                }

                const user = auth.user;
                const foundKey = auth.key;
                const rawImage = (data.imageBase64 || data.image || '').trim();

                if (!rawImage) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Изображение не передано" }));
                }

                let ext = 'png';
                let base64Data = rawImage;
                const match = rawImage.match(/^data:image\/(png|jpeg|jpg|webp|gif);base64,(.+)$/i);
                if (match) {
                    ext = match[1].toLowerCase() === 'jpeg' ? 'jpg' : match[1].toLowerCase();
                    base64Data = match[2];
                }

                const buffer = Buffer.from(base64Data, 'base64');
                if (buffer.length > 8 * 1024 * 1024) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Размер изображения не должен превышать 8 МБ" }));
                }

                const safeKey = foundKey.replace(/[^a-zA-Z0-9_-]/g, '_');
                const fileName = `${safeKey}_banner_${Date.now()}.${ext}`;
                const savePath = path.join(BANNERS_DIR, fileName);
                fs.writeFileSync(savePath, buffer);

                if (user.bannerUrl && user.bannerUrl.startsWith('/uploads/banners/')) {
                    const oldPath = path.join(SITE_DIR, user.bannerUrl);
                    try { if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath); } catch (e) {}
                }

                const bannerUrl = `/uploads/banners/${fileName}`;
                user.bannerUrl = bannerUrl;
                saveDatabase(database);
                console.log(`[SUN-API] [BANNER] Пользователь ${user.username} обновил баннер -> ${bannerUrl}`);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ success: true, bannerUrl }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message || "Ошибка загрузки фона" }));
            }
        });
        return;
    }

    // 3.96 API САЙТА: Общедоступный профиль (аватарка и баннер видны всем)
    if (parsedUrl.pathname === '/api/profile' && (req.method === 'GET' || req.method === 'POST')) {
        const queryUser = (parsedUrl.query.username || parsedUrl.query.u || parsedUrl.query.key || '').trim().toLowerCase();
        const sessionToken = (getCookie(req, 'sun_session') || '').trim();

        let targetUser = null;
        let targetKey = null;

        if (queryUser) {
            targetKey = Object.keys(database).find(k => 
                (database[k].username && database[k].username.toLowerCase() === queryUser) ||
                (database[k].key && database[k].key.toLowerCase() === queryUser)
            );
            if (targetKey) targetUser = database[targetKey];
        }

        if (!targetUser && sessionToken) {
            const auth = getUserBySessionToken(sessionToken);
            if (auth) {
                targetUser = auth.user;
                targetKey = auth.key;
            }
        }

        if (!targetUser) {
            res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: "Профиль не найден" }));
        }

        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify({
            success: true,
            username: targetUser.username,
            avatarUrl: targetUser.avatarUrl || null,
            bannerUrl: targetUser.bannerUrl || null,
            role: "Бета-тестер",
            created: targetUser.created || "2026-10-05"
        }));
    }

    // 4. API КЛИЕНТА: Проверка подписки и валидация токена/ключа/HWID (Майнкрафт)
    if (parsedUrl.pathname === '/api/check') {
        const hwid = parsedUrl.query.hwid;
        const key = parsedUrl.query.key;
        const token = parsedUrl.query.token;

        let user = null;
        let userKey = null;

        // Поиск по токену сессии SUN Connect (приоритетно)
        if (token) {
            const auth = getUserBySessionToken(token);
            if (auth) {
                user = auth.user;
                userKey = auth.key;
            }
        }

        // Поиск по ключу (если передан)
        if (!user && key) {
            userKey = Object.keys(database).find(k => 
                k.toUpperCase() === key.toUpperCase() || 
                (database[k].key && database[k].key.toUpperCase() === key.toUpperCase())
            );
            if (userKey) user = database[userKey];
        }

        // Если ключ не передан или не найден, ищем по привязанному HWID
        if (!user && hwid) {
            userKey = Object.keys(database).find(k => 
                k === hwid || 
                (database[k].hwid && database[k].hwid.toUpperCase() === hwid.toUpperCase())
            );
            if (userKey) user = database[userKey];
        }

        if (!user) {
            console.log(`[SUN-API] [ERR] Неизвестное устройство или ключ: ${key || hwid}`);
            res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ 
                valid: false, 
                message: "Ключ не найден или устройство не авторизовано. Зарегистрируйтесь на сайте!" 
            }));
        }

        const isBanned = !!user.banned;
        const isExpired = new Date(user.expires) < new Date();

        if (isBanned) {
            console.log(`[SUN-API] [BAN] Заблокированный пользователь: ${user.username}`);
            res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ valid: false, message: "Ваш аккаунт заблокирован!" }));
        }

        if (isExpired) {
            console.log(`[SUN-API] [EXP] Истекшая подписка: ${user.username}`);
            res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ valid: false, message: "Срок действия бета-теста истек!" }));
        }

        // Проверка и аппаратная привязка (HWID Lock)
        if (hwid) {
            if (!user.hwid) {
                user.hwid = hwid;
                user.lastSeen = new Date().toISOString().replace('T', ' ').substring(0, 16);
                saveDatabase(database);
                console.log(`[SUN-API] [HWID] Ключ ${userKey} (${user.username}) успешно ПРИВЯЗАН к ПК HWID: ${hwid}`);
            } else if (user.hwid.toUpperCase() !== hwid.toUpperCase()) {
                console.log(`[SUN-API] [HWID-MISMATCH] Несовпадение HWID для ${user.username}! База: ${user.hwid}, Клиент: ${hwid}`);
                res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ 
                    valid: false, 
                    message: "Ключ привязан к другому компьютеру! Сбросьте привязку HWID в Личном кабинете на сайте." 
                }));
            } else {
                user.lastSeen = new Date().toISOString().replace('T', ' ').substring(0, 16);
                saveDatabase(database);
            }
        }

        console.log(`[SUN-API] [OK] Доступ разрешен: ${user.username} (HWID: ${user.hwid || 'проверен'})`);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify({
            valid: true,
            username: user.username,
            key: key ? userKey : undefined, // Не раскрываем лицензионный ключ, если запрос был только по HWID
            coins: user.coins || 0,
            expires: user.expires,
            cosmetics: user.cosmetics || ["wings_fire", "crown_gold", "cape_sun"]
        }));
    }

    // 5. API КЛИЕНТА: Проверка актуальной версии и ссылка на Telegram-бота
    if (parsedUrl.pathname === '/api/version' || parsedUrl.pathname === '/api/update') {
        const versionInfoPath = path.join(__dirname, '..', 'bot', 'updates', 'version_info.json');
        let versionData = {
            version: "2.0.0",
            minecraft_version: "1.21.4",
            bot_username: "SUN_Visuals_bot",
            bot_url: "https://t.me/SUN_Visuals_bot?start=update",
            file_name: "sun-1.21.4-2.0.0.jar",
            file_size_bytes: 10274740,
            changelog: "Релиз SUN Client v2.0.0"
        };
        try {
            if (fs.existsSync(versionInfoPath)) {
                const parsed = JSON.parse(fs.readFileSync(versionInfoPath, 'utf-8'));
                versionData = { ...versionData, ...parsed };
                versionData.bot_username = "SUN_Visuals_bot";
                versionData.bot_url = "https://t.me/SUN_Visuals_bot?start=update";
            }
        } catch (e) {
            console.error('[SUN-API] Ошибка чтения version_info:', e);
        }

        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify(versionData));
    }

    // 6. СТАТИЧЕСКИЙ САЙТ-ВИЗИТКА (site/)
    const safeBaseDir = SITE_DIR.endsWith(path.sep) ? SITE_DIR : SITE_DIR + path.sep;
    let reqPath = parsedUrl.pathname === '/' ? '/index.html' : parsedUrl.pathname;
    if (parsedUrl.pathname === '/profile') {
        reqPath = '/profile.html';
    }
    
    // Защита от Path Traversal
    const filePath = path.resolve(path.join(SITE_DIR, reqPath));
    if (!filePath.startsWith(safeBaseDir) && filePath !== SITE_DIR) {
        res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
        return res.end('403 Forbidden');
    }

    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
        const ext = path.extname(filePath).toLowerCase();
        const mimeTypes = {
            '.html': 'text/html; charset=utf-8',
            '.css': 'text/css; charset=utf-8',
            '.js': 'application/javascript; charset=utf-8',
            '.png': 'image/png',
            '.jpg': 'image/jpeg',
            '.jpeg': 'image/jpeg',
            '.jfif': 'image/jpeg',
            '.webp': 'image/webp',
            '.gif': 'image/gif',
            '.svg': 'image/svg+xml',
            '.json': 'application/json; charset=utf-8',
            '.ico': 'image/x-icon',
            '.woff': 'font/woff',
            '.woff2': 'font/woff2',
            '.jar': 'application/java-archive'
        };
        const contentType = mimeTypes[ext] || 'application/octet-stream';
        res.writeHead(200, { 'Content-Type': contentType });
        return fs.createReadStream(filePath).pipe(res);
    }

    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('404 Not Found');
});

server.listen(PORT, () => {
    console.log(`\n=============================================================`);
    console.log(`[SUN CLIENT] СЕРВЕР АВТОРИЗАЦИИ, САЙТ И АДМИН-ПАНЕЛЬ`);
    console.log(`=============================================================`);
    console.log(`Сайт-визитка:         http://localhost:${PORT}/`);
    console.log(`Личный кабинет:       http://localhost:${PORT}/profile`);
    console.log(`Админ-панель:         http://localhost:${PORT}/admin`);
    console.log(`API для Майнкрафта:   http://localhost:${PORT}/api/check`);
    console.log(`База данных:          ${DB_FILE}`);
    console.log(`=============================================================\n`);

    // Автоматический Keep-Alive пингер против засыпания хостинга (Render / Railway / Glitch)
    const keepAliveUrl = process.env.PING_URL || process.env.RENDER_EXTERNAL_URL;
    if (keepAliveUrl) {
        console.log(`[Keep-Alive] Авто-пингер активирован для: ${keepAliveUrl} (каждые 8 минут)`);
        setInterval(() => {
            try {
                const target = keepAliveUrl.endsWith('/') ? `${keepAliveUrl}api/version` : `${keepAliveUrl}/api/version`;
                const pingProto = target.startsWith('https') ? require('https') : require('http');
                pingProto.get(target, (res) => {
                    console.log(`[Keep-Alive] Пинг отправлен (${res.statusCode}) в ${new Date().toLocaleTimeString()}`);
                }).on('error', (err) => {
                    console.warn(`[Keep-Alive] Ошибка пинга:`, err.message);
                });
            } catch (e) {
                console.warn(`[Keep-Alive] Исключение при пинге:`, e.message);
            }
        }, 8 * 60 * 1000); // каждые 8 минут (Render засыпает через 15 минут)
    }
});
