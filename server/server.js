/**
 * SUN Client — Сервер авторизации и лицензий с красивой Админ-панелью
 * Запуск: node server/server.js
 * Админка: /admin
 */

const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
// Database initialized in SQLite/JSON adapter section below

const PORT = process.env.PORT || 8080;
const DOMAIN = (process.env.DOMAIN || (process.env.RAILWAY_PUBLIC_DOMAIN ? `https://${process.env.RAILWAY_PUBLIC_DOMAIN}` : null) || process.env.RENDER_EXTERNAL_URL || 'https://sun-server-production.up.railway.app').replace(/\/+$/, '');
function resolveStorageDir() {
    if (process.env.DATA_DIR && fs.existsSync(process.env.DATA_DIR)) return process.env.DATA_DIR;
    if (process.env.RAILWAY_VOLUME_MOUNT_PATH && fs.existsSync(process.env.RAILWAY_VOLUME_MOUNT_PATH)) return process.env.RAILWAY_VOLUME_MOUNT_PATH;
    for (const d of ['/data', '/app/data']) {
        try {
            if (fs.existsSync(d)) {
                fs.accessSync(d, fs.constants.W_OK);
                return d;
            }
        } catch (e) {}
    }
    return __dirname;
}

const STORAGE_DIR = resolveStorageDir();
const DB_FILE = process.env.DB_FILE || path.join(STORAGE_DIR, 'database.json');
const DB_BACKUP_FILE = process.env.DB_BACKUP_FILE || path.join(STORAGE_DIR, 'database.backup.json');
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

process.on('uncaughtException', (err) => {
    console.error('[SUN-SERVER] [CRITICAL] Uncaught exception:', err);
});
process.on('unhandledRejection', (reason, promise) => {
    console.error('[SUN-SERVER] [CRITICAL] Unhandled rejection:', reason);
});
const SESSION_SECRET = process.env.SESSION_SECRET || 'SUN_SESSION_SECRET_2026_SECURE_KEY_9837418247918237';
// Map of active admin session tokens -> expiration timestamp (ms)
const activeAdminSessions = new Map();

// Map of active client link codes -> { code, token, createdAt, linked, user, key, uid, sparks }
const activeLinkCodes = new Map();
// Map of active client pair tokens -> { code, token, createdAt, linked, user, key, uid, sparks }
const activePairTokens = new Map();
setInterval(() => {
    const now = Date.now();
    for (const [code, entry] of activeLinkCodes.entries()) {
        if (now - entry.createdAt > 10 * 60 * 1000) activeLinkCodes.delete(code);
    }
    for (const [token, entry] of activePairTokens.entries()) {
        if (now - entry.createdAt > 10 * 60 * 1000) activePairTokens.delete(token);
    }
}, 60 * 1000);

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

function isAdminAuthorized(req) {
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


const DB_ENCRYPTION_KEY = crypto.scryptSync(process.env.DB_ENCRYPTION_KEY || 'SUN_SECURE_KEY', 'salt', 32);
const SQLITE_FILE = process.env.SQLITE_FILE || path.join(STORAGE_DIR, 'database.sqlite');

let sqlDb = null;
let sqliteEngineName = 'JSON Fallback';

function initSqliteAdapter() {
    let nativeDb = null;
    // 1. Try better-sqlite3 (native fast driver)
    try {
        let Better = null;
        try {
            Better = require('better-sqlite3');
        } catch (e1) {
            try {
                Better = require(path.join(__dirname, 'node_modules', 'better-sqlite3'));
            } catch (e2) {}
        }
        if (Better) {
            nativeDb = new Better(SQLITE_FILE);
            try { nativeDb.pragma('journal_mode = WAL'); } catch (pe) {}
            sqlDb = {
                exec: (sql) => nativeDb.exec(sql),
                prepare: (sql) => nativeDb.prepare(sql),
                runTx: (fn) => nativeDb.transaction(fn)(),
                all: (sql, params = []) => nativeDb.prepare(sql).all(...params),
                get: (sql, params = []) => nativeDb.prepare(sql).get(...params),
                engine: 'better-sqlite3'
            };
            sqliteEngineName = 'better-sqlite3';
            console.log('[SUN-DB] SQLite adapter initialized using better-sqlite3 (WAL mode)');
            return;
        }
    } catch (err) {
        console.warn('[SUN-DB] better-sqlite3 load attempt error:', err.message);
    }

    // 2. Try Node 22+ native built-in node:sqlite (zero external compilation dependencies)
    try {
        const { DatabaseSync } = require('node:sqlite');
        nativeDb = new DatabaseSync(SQLITE_FILE);
        try { nativeDb.exec('PRAGMA journal_mode = WAL;'); } catch (pe) {}
        sqlDb = {
            exec: (sql) => nativeDb.exec(sql),
            prepare: (sql) => nativeDb.prepare(sql),
            runTx: (fn) => {
                nativeDb.exec('BEGIN TRANSACTION;');
                try {
                    fn();
                    nativeDb.exec('COMMIT;');
                } catch (txErr) {
                    try { nativeDb.exec('ROLLBACK;'); } catch (rbErr) {}
                    throw txErr;
                }
            },
            all: (sql, params = []) => {
                const s = nativeDb.prepare(sql);
                return params.length ? s.all(...params) : s.all();
            },
            get: (sql, params = []) => {
                const s = nativeDb.prepare(sql);
                return params.length ? s.get(...params) : s.get();
            },
            engine: 'node:sqlite'
        };
        sqliteEngineName = 'node:sqlite (Node 22 Native)';
        console.log('[SUN-DB] SQLite adapter initialized using native node:sqlite (WAL mode)');
        return;
    } catch (err) {
        console.warn('[SUN-DB] node:sqlite not available:', err.message);
    }

    console.warn('[SUN-DB] Running in pure JSON fallback mode (database.json active)');
    sqlDb = null;
    sqliteEngineName = 'JSON Fallback';
}

initSqliteAdapter();
if (sqlDb) {
    try {
        sqlDb.exec('CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, encrypted_data TEXT)');
    } catch (err) {
        console.error('[SUN-DB] Failed to create users table:', err.message);
    }
}

function encryptObject(obj) {
    const text = JSON.stringify(obj);
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv('aes-256-cbc', DB_ENCRYPTION_KEY, iv);
    let encrypted = cipher.update(text, 'utf8', 'hex');
    encrypted += cipher.final('hex');
    return iv.toString('hex') + ':' + encrypted;
}

function decryptObject(text) {
    if (!text) return null;
    try {
        const textParts = text.split(':');
        const iv = Buffer.from(textParts.shift(), 'hex');
        const encryptedText = Buffer.from(textParts.join(':'), 'hex');
        const decipher = crypto.createDecipheriv('aes-256-cbc', DB_ENCRYPTION_KEY, iv);
        let decrypted = decipher.update(encryptedText, 'hex', 'utf8');
        decrypted += decipher.final('utf8');
        return JSON.parse(decrypted);
    } catch (e) {
        return null;
    }
}

// Загрузка или создание базы данных с авто-восстановлением и бесшовной миграцией
function loadDatabase() {
    let db = {};
    if (sqlDb) {
        // Проверяем, есть ли уже записи в SQLite
        let sqliteCount = 0;
        try {
            const countRow = sqlDb.get('SELECT count(*) as cnt FROM users');
            if (countRow && typeof countRow.cnt === 'number') {
                sqliteCount = countRow.cnt;
            }
        } catch (e) {}

        // Если в SQLite пусто, но существует legacy JSON — мигрируем
        if (sqliteCount === 0 && fs.existsSync(DB_FILE)) {
            console.log('[SUN-DB] Migrating database.json to SQLite...');
            try {
                const raw = fs.readFileSync(DB_FILE, 'utf-8');
                const dbJson = JSON.parse(raw);
                const insert = sqlDb.prepare('INSERT OR REPLACE INTO users (id, encrypted_data) VALUES (?, ?)');
                sqlDb.runTx(() => {
                    for (const key of Object.keys(dbJson)) {
                        insert.run(key, encryptObject(dbJson[key]));
                    }
                });
                if (fs.existsSync(DB_BACKUP_FILE)) {
                    try { fs.copyFileSync(DB_BACKUP_FILE, DB_BACKUP_FILE + '.migrated'); } catch (e) {}
                }
            } catch (e) {
                console.error('[SUN-DB] Error during migration:', e);
            }
        }

        try {
            const rows = sqlDb.all('SELECT id, encrypted_data FROM users');
            for (const row of rows) {
                const decrypted = decryptObject(row.encrypted_data);
                if (decrypted) {
                    if (decrypted.coins === undefined || decrypted.coins === null) decrypted.coins = 0;
                    if (!Array.isArray(decrypted.cosmetics)) decrypted.cosmetics = [];
                    db[row.id] = decrypted;
                }
            }
            console.log(`[SUN-DB] Loaded ${Object.keys(db).length} users from SQLite [${sqliteEngineName}]`);
        } catch (e) {
            console.error('[SUN-DB] SQLite load error:', e);
        }
    }

    // Если в SQLite ничего не найдено или SQLite недоступен, читаем JSON
    if (Object.keys(db).length === 0) {
        try {
            if (fs.existsSync(DB_FILE)) {
                db = JSON.parse(fs.readFileSync(DB_FILE, 'utf-8'));
                console.log(`[SUN-DB] Loaded ${Object.keys(db).length} users from JSON fallback file`);
            } else if (fs.existsSync(DB_FILE + '.migrated')) {
                db = JSON.parse(fs.readFileSync(DB_FILE + '.migrated', 'utf-8'));
            }
        } catch (e) {
            console.error('[SUN-DB] Fallback JSON load error:', e);
        }
    }

    // Начальный дефолтный пользователь, если база совсем пуста
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

// Надежное транзакционное сохранение базы данных в SQLite + страховочный JSON-снапшот
function saveDatabase(db) {
    if (!db || typeof db !== 'object' || Object.keys(db).length === 0) {
        console.warn('[SUN-DB] Prevented saving empty database to protect user data!');
        return;
    }
    if (sqlDb) {
        try {
            const insert = sqlDb.prepare('INSERT OR REPLACE INTO users (id, encrypted_data) VALUES (?, ?)');
            sqlDb.runTx(() => {
                for (const key of Object.keys(db)) {
                    insert.run(key, encryptObject(db[key]));
                }
            });

            const existingKeys = new Set(Object.keys(db));
            const allRows = sqlDb.all('SELECT id FROM users');
            const deleteStmt = sqlDb.prepare('DELETE FROM users WHERE id = ?');
            sqlDb.runTx(() => {
                for (const row of allRows) {
                    if (!existingKeys.has(row.id)) deleteStmt.run(row.id);
                }
            });
        } catch (e) {
            console.error('[SUN-DB] SQLite save error:', e);
        }
    }
    // Также всегда сохраняем резервную копию в JSON (снапшот безопасности)
    try {
        const tmpFile = DB_FILE + '.tmp';
        fs.writeFileSync(tmpFile, JSON.stringify(db, null, 2), 'utf-8');
        fs.renameSync(tmpFile, DB_FILE);
    } catch (e) {
        // Игнорируем ошибки временных файлов
    }
}

const database = loadDatabase();

const ANNOUNCEMENT_FILE = path.join(__dirname, 'announcement.json');
let announcement = { active: false, type: 'info', title: '', message: '', url: '' };
try {
    if (fs.existsSync(ANNOUNCEMENT_FILE)) {
        announcement = JSON.parse(fs.readFileSync(ANNOUNCEMENT_FILE, 'utf-8'));
    }
} catch (e) {
    console.error('[SUN-API] Error loading announcement:', e.message);
}

function saveAnnouncement() {
    try {
        fs.writeFileSync(ANNOUNCEMENT_FILE, JSON.stringify(announcement, null, 2), 'utf-8');
    } catch(e) {}
}


// Админ-панель загружается из server/admin.html

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
    res.setHeader('Content-Security-Policy', "default-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com https://challenges.cloudflare.com https://cdnjs.cloudflare.com; script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com https://cdn.tailwindcss.com https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://fonts.googleapis.com; frame-src https://challenges.cloudflare.com; font-src 'self' data: https://cdnjs.cloudflare.com https://fonts.gstatic.com; img-src 'self' data: https:;");
    
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

    // 0.5 АВТОРИЗАЦИЯ АДМИНИСТРАТОРА
    if (parsedUrl.pathname === '/api/admin/login' && req.method === 'POST') {
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

    // 1. АДМИН-ПАНЕЛЬ (Красивая HTML-страница)
    if (parsedUrl.pathname === '/admin' || parsedUrl.pathname === '/admin/') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end(fs.readFileSync(require('path').join(__dirname, 'admin.html'), 'utf-8'));
    }

    // 1.5 API ДЛЯ АДМИНКИ: Экспорт и импорт базы данных (резервные копии)
    if (parsedUrl.pathname === '/api/admin/export-db' && req.method === 'GET') {
        if (!isAdminAuthorized(req)) {
            res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: 'Unauthorized: Требуется авторизация администратора' }));
        }
        const nowStr = new Date().toISOString().split('T')[0];
        res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Content-Disposition': `attachment; filename="sun_database_backup_${nowStr}.json"`
        });
        return res.end(JSON.stringify(database, null, 2));
    }

    if (parsedUrl.pathname === '/api/admin/import-db' && req.method === 'POST') {
        if (!isAdminAuthorized(req)) {
            res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: 'Unauthorized: Требуется авторизация администратора' }));
        }
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 10 * 1024 * 1024) req.destroy();
        });
        req.on('end', () => {
            try {
                const parsed = JSON.parse(body);
                const imported = parsed.database || parsed;
                if (!imported || typeof imported !== 'object') {
                    throw new Error('Некорректный формат файла базы данных');
                }
                let count = 0;
                for (const k of Object.keys(imported)) {
                    if (imported[k] && typeof imported[k] === 'object') {
                        database[k] = imported[k];
                        count++;
                    }
                }
                saveDatabase(database);
                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ success: true, count, message: `Успешно импортировано/обновлено ${count} пользователей` }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: 'Ошибка импорта: ' + e.message }));
            }
        });
        return;
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

                if (action === 'send_global_announcement') {
                    const text = data.text || '';
                    const isUpdate = !!data.isUpdate;
                    const version = data.version || '';
                    
                    // Обновляем для всех в базе (игра)
                    for (const key of Object.keys(database)) {
                        database[key].pendingAnnouncement = { text, isUpdate, version };
                    }
                    saveDatabase(database);
                    
                    // Обновляем для сайта
                    announcement = {
                        active: true,
                        type: isUpdate ? 'warning' : 'info',
                        title: isUpdate ? 'Новая версия ' + version : 'Объявление',
                        message: text,
                        url: ''
                    };
                    fs.writeFileSync(ANNOUNCEMENT_FILE, JSON.stringify(announcement, null, 2));
                    console.log('[SUN-API] Глобальное объявление отправлено!');
                } else if (action === 'send_announcement') {
                    if (data.hwids) {
                        for (let h of data.hwids) {
                            if (database[h]) {
                                database[h].pendingAnnouncement = {
                                    text: data.text,
                                    isUpdate: !!data.isUpdate,
                                    version: data.version || null
                                };
                            }
                        }
                    }
                    saveDatabase(database);
                } else if (action === 'soft_delete') {
                    if (database[hwid]) database[hwid].deletedAt = new Date().toISOString();
                    saveDatabase(database);
                } else if (action === 'soft_delete_bulk') {
                    if (data.hwids) {
                        for (let h of data.hwids) {
                            if (database[h]) database[h].deletedAt = new Date().toISOString();
                        }
                    }
                    saveDatabase(database);
                } else if (action === 'hard_delete') {
                    delete database[hwid];
                    saveDatabase(database);
                } else if (action === 'restore') {
                    if (database[hwid]) delete database[hwid].deletedAt;
                    saveDatabase(database);
                } else if (action === 'add_coins_bulk') {
                    if (data.hwids) {
                        for (let h of data.hwids) {
                            if (database[h]) database[h].coins = (database[h].coins || 0) + (parseInt(data.amount) || 0);
                        }
                    }
                    saveDatabase(database);
                } else if (action === 'extend' && database[hwid]) {
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
                    const list = Array.isArray(database[hwid].cosmetics) ? database[hwid].cosmetics : [];
                    const defaults = ["wings_fire", "crown_gold", "cape_sun"];
                    const hasAll = defaults.every(d => list.includes(d));
                    if (hasAll) {
                        database[hwid].cosmetics = list.filter(c => !defaults.includes(c));
                    } else {
                        const set = new Set([...list, ...defaults]);
                        database[hwid].cosmetics = Array.from(set);
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
                    database[hwid].coins = Math.max(0, (database[hwid].coins || 0) + (parseInt(data.amount) || 50));
                    saveDatabase(database);
                } else if (action === 'set_coins' && database[hwid]) {
                    database[hwid].coins = Math.max(0, parseInt(data.amount) || 0);
                    saveDatabase(database);
                } else if (action === 'set_coins_bulk' && data.hwids) {
                    const amt = Math.max(0, parseInt(data.amount) || 0);
                    for (let h of data.hwids) {
                        if (database[h]) database[h].coins = amt;
                    }
                    saveDatabase(database);
                } else if (action === 'set_cosmetics' && database[hwid]) {
                    if (Array.isArray(data.cosmetics)) {
                        database[hwid].cosmetics = data.cosmetics;
                        saveDatabase(database);
                    }
                } else if (action === 'toggle_cosmetic_item' && database[hwid]) {
                    const item = String(data.item || '').trim();
                    if (item) {
                        if (!Array.isArray(database[hwid].cosmetics)) database[hwid].cosmetics = [];
                        const idx = database[hwid].cosmetics.indexOf(item);
                        if (idx >= 0) {
                            database[hwid].cosmetics.splice(idx, 1);
                        } else {
                            database[hwid].cosmetics.push(item);
                        }
                        saveDatabase(database);
                    }
                } else if (action === 'edit_user' && database[hwid]) {
                    const u = database[hwid];
                    if (typeof data.username === 'string' && data.username.trim()) {
                        u.username = data.username.trim();
                    }
                    if (data.coins !== undefined && !isNaN(parseInt(data.coins))) {
                        u.coins = Math.max(0, parseInt(data.coins));
                    }
                    if (Array.isArray(data.cosmetics)) {
                        u.cosmetics = data.cosmetics;
                    }
                    if (typeof data.expires === 'string' && data.expires) {
                        u.expires = data.expires;
                    }
                    if (typeof data.banned === 'boolean') {
                        u.banned = data.banned;
                    }
                    if (typeof data.active === 'boolean') {
                        u.active = data.active;
                    }
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

    // 3.96 API САЙТА & КЛИЕНТА: Мгновенная привязка аккаунта (1-Click Pair) и коды
    if (parsedUrl.pathname === '/api/auth/pair/start' && req.method === 'POST') {
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        let code = '';
        do {
            code = Math.floor(100000 + Math.random() * 900000).toString();
        } while (activeLinkCodes.has(code));

        const token = crypto.randomBytes(16).toString('hex');
        const entry = {
            code,
            token,
            createdAt: Date.now(),
            linked: false,
            user: null,
            key: null,
            uid: 10,
            sparks: 0
        };
        activeLinkCodes.set(code, entry);
        activePairTokens.set(token, entry);

        const host = req.headers['host'];
        const protocol = (req.headers['x-forwarded-proto'] === 'https' || (req.connection && req.connection.encrypted)) ? 'https' : 'http';
        const baseUrl = host ? `${protocol}://${host}` : DOMAIN;
        const linkUrl = `${baseUrl}/link?token=${token}`;

        return res.end(JSON.stringify({
            success: true,
            token,
            code,
            url: linkUrl
        }));
    }

    if (parsedUrl.pathname === '/api/generate-link-code') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        let code = '';
        do {
            code = Math.floor(100000 + Math.random() * 900000).toString();
        } while (activeLinkCodes.has(code));

        const token = crypto.randomBytes(16).toString('hex');
        const entry = {
            code,
            token,
            createdAt: Date.now(),
            linked: false,
            user: null,
            key: null,
            uid: 10,
            sparks: 0
        };
        activeLinkCodes.set(code, entry);
        activePairTokens.set(token, entry);
        return res.end(JSON.stringify({ code, token }));
    }

    if (parsedUrl.pathname === '/api/auth/pair/info') {
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        const token = (parsedUrl.query.token || '').trim();
        let entry = null;
        if (token) entry = activePairTokens.get(token);
        if (!entry && token) {
            for (const item of activeLinkCodes.values()) {
                if (item.token === token) { entry = item; break; }
            }
        }
        if (!entry) {
            return res.end(JSON.stringify({ valid: false, error: 'not_found' }));
        }
        return res.end(JSON.stringify({
            valid: true,
            linked: entry.linked,
            user: entry.user,
            code: entry.code
        }));
    }

    if (parsedUrl.pathname === '/api/link-status') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        const code = (parsedUrl.query.code || '').replace(/[^0-9]/g, '').trim();
        const token = (parsedUrl.query.token || '').trim();

        let entry = null;
        if (token) entry = activePairTokens.get(token);
        if (!entry && code) entry = activeLinkCodes.get(code);
        if (!entry && token) {
            for (const item of activeLinkCodes.values()) {
                if (item.token === token) { entry = item; break; }
            }
        }

        if (!entry) {
            return res.end(JSON.stringify({ linked: false, error: 'not_found' }));
        }

        if (entry.linked) {
            return res.end(JSON.stringify({
                linked: true,
                username: entry.user,
                key: entry.key,
                uid: entry.uid,
                sparks: entry.sparks,
                token: entry.token
            }));
        }

        return res.end(JSON.stringify({ linked: false, pending: true }));
    }

    if (parsedUrl.pathname === '/api/auth/pair/confirm' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const userKey = (data.userKey || data.key || getCookie(req, 'sun_key') || '').trim();
                const auth = getUserBySessionToken(sessionToken, userKey);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ success: false, error: 'Необходимо войти в аккаунт на сайте' }));
                }

                const token = (data.token || '').trim();
                const code = String(data.code || '').replace(/[^0-9]/g, '').trim();
                if (!token && !code) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ success: false, error: 'Токен или код сопряжения не указан' }));
                }

                let entry = null;
                if (token) entry = activePairTokens.get(token);
                if (!entry && code) entry = activeLinkCodes.get(code);
                if (!entry && token) {
                    for (const item of activeLinkCodes.values()) {
                        if (item.token === token) { entry = item; break; }
                    }
                }

                if (!entry) {
                    res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ success: false, error: 'Сессия привязки не найдена или истекла. Запустите вход в игре снова.' }));
                }

                if (entry.linked) {
                    if (entry.user === auth.user.username) {
                        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({
                            success: true,
                            message: `Клиент уже привязан к аккаунту ${auth.user.username}!`,
                            username: auth.user.username,
                            uid: entry.uid,
                            sparks: entry.sparks
                        }));
                    } else {
                        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({ success: false, error: 'Этот токен привязки уже был использован другим аккаунтом.' }));
                    }
                }

                entry.linked = true;
                entry.user = auth.user.username;
                entry.key = auth.user.key || auth.key;
                entry.uid = auth.user.uid || 10;
                entry.sparks = typeof auth.user.coins === 'number' ? auth.user.coins : 0;

                auth.user.clientLinked = true;
                auth.user.clientLinkToken = entry.token;
                saveDatabase(database);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    message: `Клиент успешно привязан к аккаунту ${auth.user.username}!`,
                    username: auth.user.username,
                    uid: entry.uid,
                    sparks: entry.sparks
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ success: false, error: 'Ошибка обработки запроса: ' + e.message }));
            }
        });
        return;
    }

    if (parsedUrl.pathname === '/api/auth/pair/link' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const sessionToken = (data.sessionToken || getCookie(req, 'sun_session') || '').trim();
                const userKey = (data.userKey || data.key || getCookie(req, 'sun_key') || '').trim();
                const auth = getUserBySessionToken(sessionToken, userKey);
                if (!auth) {
                    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: 'Необходимо войти в аккаунт на сайте' }));
                }
                const inputCode = String(data.code || '').replace(/[^0-9]/g, '').trim();
                if (!inputCode || inputCode.length !== 6) {
                    res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: 'Введите корректный 6-значный код из игры' }));
                }

                let entry = activeLinkCodes.get(inputCode);
                if (!entry) {
                    res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
                    return res.end(JSON.stringify({ error: 'Код привязки не найден или истек. Откройте окно привязки в игре.' }));
                }

                if (entry.linked) {
                    if (entry.user === auth.user.username) {
                        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({
                            success: true,
                            message: `Игра уже привязана к аккаунту ${auth.user.username}!`
                        }));
                    } else {
                        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                        return res.end(JSON.stringify({ error: 'Этот код уже был использован другим аккаунтом.' }));
                    }
                }

                entry.linked = true;
                entry.user = auth.user.username;
                entry.key = auth.user.key || auth.key;
                entry.uid = auth.user.uid || 10;
                entry.sparks = typeof auth.user.coins === 'number' ? auth.user.coins : 0;

                auth.user.clientLinked = true;
                auth.user.clientLinkToken = entry.token;
                saveDatabase(database);

                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({
                    success: true,
                    message: `Игра успешно привязана к аккаунту ${auth.user.username}!`
                }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                return res.end(JSON.stringify({ error: 'Ошибка обработки запроса: ' + e.message }));
            }
        });
        return;
    }

    if (parsedUrl.pathname === '/api/sparks-balance') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        const key = (parsedUrl.query.key || '').trim();
        const token = (parsedUrl.query.token || '').trim();
        const sessionToken = (getCookie(req, 'sun_session') || '').trim();

        let targetUser = null;
        if (key && database[key]) {
            targetUser = database[key];
        } else if (token) {
            for (const u of Object.values(database)) {
                if (u.clientLinkToken === token) { targetUser = u; break; }
            }
        }

        if (!targetUser && sessionToken) {
            const auth = getUserBySessionToken(sessionToken);
            if (auth) targetUser = auth.user;
        }

        if (targetUser) {
            const sparks = typeof targetUser.coins === 'number' ? targetUser.coins : 0;
            return res.end(JSON.stringify({
                sparks,
                username: targetUser.username,
                uid: targetUser.uid,
                linked: true
            }));
        }

        return res.end(JSON.stringify({ sparks: 0, linked: false }));
    }

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

    // 4. API КЛИЕНТА: Проверка подписки и валидация HWID (Майнкрафт)
    if (parsedUrl.pathname === '/api/check') {
        const hwid = parsedUrl.query.hwid;
        const key = parsedUrl.query.key;
        const clientVersion = (parsedUrl.query.version || parsedUrl.query.v || '').trim();

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

        if (clientVersion) user.clientVersion = clientVersion;
        // Проверка и аппаратная привязка (HWID Lock)
        if (hwid) {
            if (!user.hwid) {
                user.hwid = hwid;
                // Telemetry logic
                const now = Date.now();
                if (user.lastGamePing) {
                    const diff = Math.floor((now - user.lastGamePing) / 1000);
                    if (diff < 300) {
                        user.playtime = (user.playtime || 0) + diff;
                    }
                }
                user.lastGamePing = now;
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
                // Telemetry logic
                const now = Date.now();
                if (user.lastGamePing) {
                    const diff = Math.floor((now - user.lastGamePing) / 1000);
                    if (diff < 300) {
                        user.playtime = (user.playtime || 0) + diff;
                    }
                }
                user.lastGamePing = now;
                user.lastSeen = new Date().toISOString().replace('T', ' ').substring(0, 16);
                saveDatabase(database);
            }
        }

        let sendAnnounce = user.pendingAnnouncement || null;
        if (sendAnnounce) {
            if (sendAnnounce.isUpdate && user.clientVersion === sendAnnounce.version) {
                delete user.pendingAnnouncement;
                sendAnnounce = null;
                saveDatabase(database);
            } else if (!sendAnnounce.isUpdate) {
                delete user.pendingAnnouncement;
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

    
    // 6. API WEB PING
    if (parsedUrl.pathname === '/api/web-ping' && req.method === 'POST') {
        const sessionToken = getCookie(req, 'sun_session');
        let targetUser = null;
        if (sessionToken) {
            const auth = getUserBySessionToken(sessionToken);
            if (auth) targetUser = auth.user;
        }
        if (targetUser) {
            targetUser.lastWebPing = Date.now();
            targetUser.lastPing = Date.now();
            saveDatabase(database);
        }
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify({ success: true }));
    }

    // 6. API ANNOUNCEMENT
    if (parsedUrl.pathname === '/api/announcement') {
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify(announcement));
    }

    if (parsedUrl.pathname === '/api/admin/announcement' && req.method === 'POST') {
        if (!isAdminAuthorized(req)) {
            res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end(JSON.stringify({ error: 'Unauthorized' }));
        }
        let body = '';
        req.on('data', chunk => body += chunk);
        return req.on('end', () => {
            try {
                const data = JSON.parse(body);
                announcement.active = !!data.active;
                announcement.type = data.type || 'info';
                announcement.title = data.title || '';
                announcement.message = data.message || '';
                announcement.url = data.url || '';
                saveAnnouncement();
                res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
                res.end(JSON.stringify({ success: true }));
            } catch (e) {
                res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
                res.end(JSON.stringify({ error: 'Invalid JSON' }));
            }
        });
    }

    // 7. СТАТИЧЕСКИЙ САЙТ-ВИЗИТКА (site/)
    const safeBaseDir = SITE_DIR.endsWith(path.sep) ? SITE_DIR : SITE_DIR + path.sep;
    let reqPath = parsedUrl.pathname === '/' ? '/index.html' : parsedUrl.pathname;
    if (parsedUrl.pathname === '/profile') {
        reqPath = '/profile.html';
    }
    if (parsedUrl.pathname === '/link') {
        reqPath = '/link.html';
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


// Background job to permanently delete users from Recycle Bin after 30 days
setInterval(() => {
    let changed = false;
    const now = Date.now();
    for (const k of Object.keys(database)) {
        if (database[k].deletedAt) {
            const delDate = new Date(database[k].deletedAt).getTime();
            if (now - delDate > 30 * 24 * 60 * 60 * 1000) {
                delete database[k];
                changed = true;
            }
        }
    }
    if (changed) saveDatabase(database);
}, 60 * 60 * 1000); // Check every hour

server.listen(PORT, '0.0.0.0', () => {
    console.log(`\n=============================================================`);
    console.log(`[SUN CLIENT] СЕРВЕР АВТОРИЗАЦИИ, САЙТ И АДМИН-ПАНЕЛЬ`);
    console.log(`=============================================================`);
    console.log(`Сайт-визитка:         ${DOMAIN}/`);
    console.log(`Личный кабинет:       ${DOMAIN}/profile`);
    console.log(`Админ-панель:         ${DOMAIN}/admin`);
    console.log(`API для Майнкрафта:   ${DOMAIN}/api/check`);
    console.log(`База данных:          ${sqlDb ? `SQLite [${sqliteEngineName}] (${SQLITE_FILE})` : `JSON Fallback (${DB_FILE})`}`);
    console.log(`=============================================================\n`);

    // Автоматический Keep-Alive пингер против засыпания хостинга (Render / Railway / Glitch)
    const keepAliveUrl = process.env.PING_URL || process.env.RENDER_EXTERNAL_URL || (process.env.RAILWAY_PUBLIC_DOMAIN ? ('https://' + process.env.RAILWAY_PUBLIC_DOMAIN) : null) || DOMAIN;
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


