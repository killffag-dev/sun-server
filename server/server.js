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

// Password hashing utility
function hashPassword(password) {
    return crypto.createHash('sha256').update(password + 'SUN_SECURE_SALT_2026').digest('hex');
}

// Генератор уникального лицензионного ключа (SUN-XXXX-XXXX)
function generateLicenseKey() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let p1 = '', p2 = '';
    for (let i = 0; i < 4; i++) {
        p1 += chars.charAt(Math.floor(Math.random() * chars.length));
        p2 += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `SUN-${p1}-${p2}`;
}

// Загрузка или создание базы данных
function loadDatabase() {
    try {
        if (fs.existsSync(DB_FILE)) {
            const data = fs.readFileSync(DB_FILE, 'utf-8');
            return JSON.parse(data);
        }
    } catch (e) {
        console.error('[SUN-DB] Ошибка чтения базы данных:', e);
    }
    return {
        "TEST-PC-DEMO": {
            username: "Tester",
            active: true,
            banned: false,
            created: new Date().toISOString().split('T')[0],
            expires: "2026-12-31",
            lastSeen: "2026-09-27 20:00",
            cosmetics: ["wings_fire", "crown_gold"]
        }
    };
}

function saveDatabase(db) {
    try {
        fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2), 'utf-8');
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
            <div>
                <span style="font-size: 13px; color: var(--text-muted);">Статус сервера:</span>
                <span style="color: var(--green); font-weight: 600; font-size: 13px;">● Онлайн</span>
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
                        <th>Искры (⚡)</th>
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
                allUsers = await res.json();
                renderTable();
            } catch (e) {
                console.error('Ошибка загрузки пользователей', e);
            }
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
                const isBanned = user.banned;
                const isExpired = new Date(user.expires) < now;
                const isActive = user.active && !isBanned && !isExpired;

                if (isActive) activeCount++;
                if (isBanned) bannedCount++;

                // Фильтр поиска
                if (search && !user.username.toLowerCase().includes(search) && !key.toLowerCase().includes(search) && !(user.hwid && user.hwid.toLowerCase().includes(search))) {
                    return;
                }

                let statusBadge = '<span class="status-badge status-active">● Активна</span>';
                if (isBanned) {
                    statusBadge = '<span class="status-badge status-banned">⛔ Забанен</span>';
                } else if (isExpired) {
                    statusBadge = '<span class="status-badge status-expired">● Истекла</span>';
                }

                let hwidBadge = user.hwid 
                    ? ('<span style="color:#10b981;font-size:12px;font-family:monospace;" title="' + user.hwid + '">🟢 ' + user.hwid.substring(0, 14) + '...</span>')
                    : '<span style="color:#8b93a7;font-size:12px;">🟡 Не привязан</span>';

                const cosmeticsHtml = (user.cosmetics || []).map(c => '<span class="tag">' + c + '</span>').join('') || '<span style="color:#555">нет</span>';
                const resetHwidBtn = user.hwid ? ('<button class="action-btn" title="Сбросить привязку HWID" onclick="resetHwid(\'' + key + '\')">🔄 HWID</button>') : '';

                const tr = document.createElement('tr');
                tr.innerHTML = \`
                    <td>
                        <strong>\${user.username}</strong>
                        \${user.email ? '<div style="font-size:11px;color:var(--text-muted);">' + user.email + '</div>' : ''}
                    </td>
                    <td><span class="hwid-badge">\${key}</span></td>
                    <td>\${hwidBadge}</td>
                    <td><strong style="color:var(--accent);">⚡ \${user.coins || 0}</strong></td>
                    <td>\${statusBadge}</td>
                    <td>\${user.expires}</td>
                    <td>\${cosmeticsHtml}</td>
                    <td>
                        <div class="actions-cell">
                            <button class="action-btn" title="Начислить 50 Искр" onclick="addCoins('\${key}', 50)">+⚡50</button>
                            \${resetHwidBtn}
                            <button class="action-btn" title="Продлить на 30 дней" onclick="extendDays('\${key}', 30)">+30д</button>
                            <button class="action-btn" title="Выдать/забрать косметику" onclick="toggleCosmetic('\${key}')">👑 Косм.</button>
                            <button class="action-btn ban" onclick="toggleBan('\${key}', \${!isBanned})">\${isBanned ? 'Разбан' : 'Бан'}</button>
                            <button class="action-btn" title="Удалить пользователя" onclick="deleteUser('\${key}')">🗑️</button>
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
            await fetch('/api/admin/action', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
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
    const clientIp = req.socket.remoteAddress || 'unknown';
    
    // 1. Security Headers (Helmet equivalent)
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    res.setHeader('Content-Security-Policy', "default-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com; img-src 'self' data: https:;");
    
    // 2. IP Rate Limiting
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

    // 1. АДМИН-ПАНЕЛЬ (Красивая HTML-страница)
    if (parsedUrl.pathname === '/admin' || parsedUrl.pathname === '/admin/') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end(ADMIN_HTML);
    }

    // 2. API ДЛЯ АДМИНКИ: Получение списка пользователей
    if (parsedUrl.pathname === '/api/admin/users' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify(database));
    }

    // 3. API ДЛЯ АДМИНКИ: Действия над пользователями
    if (parsedUrl.pathname === '/api/admin/action' && req.method === 'POST') {
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
            if (body.length > 1024 * 512) { // 512KB payload limit
                req.destroy();
            }
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const username = (data.username || '').trim();
                const email = (data.email || '').trim().toLowerCase();
                const password = (data.password || '').trim();

                if (!username) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Пожалуйста, введите никнейм" }));
                }
                if (!password || password.length < 6) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Пароль должен содержать минимум 6 символов" }));
                }

                // Проверяем, существует ли уже пользователь с таким ником или email
                let existingHwid = Object.keys(database).find(k => 
                    database[k].username && database[k].username.toLowerCase() === username.toLowerCase() ||
                    email && database[k].email && database[k].email.toLowerCase() === email
                );

                if (existingHwid) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Пользователь с таким ником или почтой уже зарегистрирован" }));
                }

                const accountId = generateLicenseKey();
                const exp = new Date();
                exp.setDate(exp.getDate() + 60); // 60 дней бета-теста

                database[accountId] = {
                    username: username,
                    email: email,
                    key: accountId,
                    password: hashPassword(password),
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

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    username: username,
                    email: email,
                    key: accountId,
                    coins: 0,
                    hwid: null,
                    hwid_last_reset: null,
                    expires: exp.toISOString().split('T')[0],
                    plan: "Бета-тест (60 дней)",
                    cosmetics: ["wings_fire", "crown_gold", "cape_sun"]
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    // 3.55 API САЙТА: Быстрый вход через Google
    if (parsedUrl.pathname === '/api/google-auth' && req.method === 'POST') {
        try {
            const randomSuffix = Math.floor(1000 + Math.random() * 9000);
            const googleUsername = "Player_" + randomSuffix;
            const accountId = generateLicenseKey();
            const exp = new Date();
            exp.setDate(exp.getDate() + 60);

            database[accountId] = {
                username: googleUsername,
                email: "google." + randomSuffix + "@gmail.com",
                key: accountId,
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
            saveDatabase(database);

            res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({
                success: true,
                username: googleUsername,
                email: "google." + randomSuffix + "@gmail.com",
                key: accountId,
                coins: 0,
                hwid: null,
                hwid_last_reset: null,
                expires: exp.toISOString().split('T')[0],
                active: true,
                cosmetics: ["wings_fire", "crown_gold", "cape_sun"]
            }));
        } catch (e) {
            res.writeHead(500, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: "Ошибка создания Google-сессии" }));
        }
    }

    // 3.6 API САЙТА: Вход в личный кабинет (Никнейм/Email + Пароль или Ключ)
    if (parsedUrl.pathname === '/api/login' && req.method === 'POST') {
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
                const query = (data.query || data.username || data.key || '').trim();
                const password = (data.password || '').trim();

                if (!query) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Введите ваш никнейм или почту" }));
                }

                let foundKey = Object.keys(database).find(k => 
                    k.toLowerCase() === query.toLowerCase() || 
                    (database[k].username && database[k].username.toLowerCase() === query.toLowerCase()) ||
                    (database[k].email && database[k].email.toLowerCase() === query.toLowerCase())
                );

                if (!foundKey) {
                    res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Аккаунт не найден. Проверьте данные или создайте новый." }));
                }

                const u = database[foundKey];
                
                // Если у аккаунта есть пароль, проверяем его (если вход не по точному ключу сессии)
                if (u.password && query.toLowerCase() !== foundKey.toLowerCase()) {
                    if (!password || u.password !== hashPassword(password)) {
                        res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({ error: "Неверный пароль" }));
                    }
                }

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    username: u.username,
                    email: u.email,
                    key: foundKey,
                    coins: u.coins || 0,
                    hwid: u.hwid || null,
                    hwid_last_reset: u.hwid_last_reset || null,
                    expires: u.expires,
                    active: u.active && !u.banned,
                    cosmetics: u.cosmetics || ["wings_fire", "crown_gold", "cape_sun"]
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: e.message }));
            }
        });
        return;
    }

    // 3.7 API САЙТА: Сброс привязки HWID пользователем (с кулдауном 3 дня)
    if (parsedUrl.pathname === '/api/reset-hwid' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const key = (data.key || '').trim();
                let foundKey = Object.keys(database).find(k => 
                    k.toUpperCase() === key.toUpperCase() || 
                    (database[k].key && database[k].key.toUpperCase() === key.toUpperCase())
                );

                if (!foundKey) {
                    res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: "Аккаунт не найден" }));
                }

                const user = database[foundKey];
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
                console.log(`[SUN-API] 🔄 Сброс HWID для пользователя ${user.username} (${foundKey})`);

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

    // 4. API КЛИЕНТА: Проверка подписки и валидация HWID (Майнкрафт)
    if (parsedUrl.pathname === '/api/check') {
        const hwid = parsedUrl.query.hwid;
        const key = parsedUrl.query.key;

        if (!hwid && !key) {
            res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ valid: false, message: "HWID или Ключ не передан" }));
        }

        let user = null;
        let userKey = null;

        // Поиск по ключу (приоритетно)
        if (key) {
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
            console.log(`[SUN-API] ❌ Неизвестное устройство или ключ: ${key || hwid}`);
            res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ 
                valid: false, 
                message: "Ключ не найден или устройство не авторизовано. Зарегистрируйтесь на сайте!" 
            }));
        }

        const isBanned = !!user.banned;
        const isExpired = new Date(user.expires) < new Date();

        if (isBanned) {
            console.log(`[SUN-API] ⛔ Заблокированный пользователь: ${user.username}`);
            res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ valid: false, message: "Ваш аккаунт заблокирован!" }));
        }

        if (isExpired) {
            console.log(`[SUN-API] ⏳ Истекшая подписка: ${user.username}`);
            res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ valid: false, message: "Срок действия бета-теста истек!" }));
        }

        // Проверка и аппаратная привязка (HWID Lock)
        if (hwid) {
            if (!user.hwid) {
                // Первая активация на ПК -> жесткая привязка к этому железу!
                user.hwid = hwid;
                user.lastSeen = new Date().toISOString().replace('T', ' ').substring(0, 16);
                saveDatabase(database);
                console.log(`[SUN-API] 🔒 Ключ ${userKey} (${user.username}) успешно ПРИВЯЗАН к ПК HWID: ${hwid}`);
            } else if (user.hwid.toUpperCase() !== hwid.toUpperCase()) {
                console.log(`[SUN-API] ⛔ Несовпадение HWID для ${user.username}! База: ${user.hwid}, Клиент: ${hwid}`);
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

        console.log(`[SUN-API] ✅ Доступ разрешен: ${user.username} (HWID: ${user.hwid || 'проверен'})`);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify({
            valid: true,
            username: user.username,
            key: userKey,
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
    const SITE_DIR = path.resolve(path.join(__dirname, '..', 'site'));
    let reqPath = parsedUrl.pathname === '/' ? '/index.html' : parsedUrl.pathname;
    
    // Prevent Path Traversal
    const filePath = path.resolve(path.join(SITE_DIR, reqPath));
    if (!filePath.startsWith(SITE_DIR)) {
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
            '.svg': 'image/svg+xml',
            '.json': 'application/json; charset=utf-8',
            '.ico': 'image/x-icon',
            '.woff2': 'font/woff2'
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
    console.log(`☀️  SUN CLIENT — СЕРВЕР АВТОРИЗАЦИИ, САЙТ И АДМИН-ПАНЕЛЬ`);
    console.log(`=============================================================`);
    console.log(`🚀 Сайт-визитка (открой в браузере): http://localhost:${PORT}/`);
    console.log(`👑 Админ-панель:                     http://localhost:${PORT}/admin`);
    console.log(`⚡ API для Майнкрафта:              http://localhost:${PORT}/api/check`);
    console.log(`💾 Данные сохраняются в:             server/database.json`);
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
