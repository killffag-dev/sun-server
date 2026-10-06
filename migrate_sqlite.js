const fs = require('fs');

let code = fs.readFileSync('C:/Users/edya/Desktop/sun-server-deploy/server/server.js', 'utf8');
const lines = code.split('\n');

const replacements = {
    1040: 'targetKey',
    1043: 'targetKey',
    1051: 'targetKey',
    1054: 'targetKey',
    1059: 'targetKey',
    1062: 'targetKey',
    1080: 'key',
    1159: 'accountId',
    1266: 'userKey',
    1320: 'foundKey',
    1386: 'foundKey',
    1503: 'foundKey',
    1604: 'foundKey',
    1656: 'foundKey',
    1708: 'foundKey',
    1777: 'foundKey',
    1842: 'foundKey',
    1944: 'userKey'
};

for (const [lineIdx, keyVar] of Object.entries(replacements)) {
    const idx = parseInt(lineIdx); // 0-indexed in array is lineIdx
    // wait, my find_keys.js returned 1-indexed lines:
    // Line 1041: nearest key is targetKey -> index 1040
    // I should replace `saveDatabase(database)` with `saveDatabase(database, ${keyVar})`
    lines[idx] = lines[idx].replace('saveDatabase(database)', `saveDatabase(database, ${keyVar})`);
}

// Now replace the saveDatabase definition!
const oldFuncRegex = /function saveDatabase\(db\) \{[\s\S]*?catch \(e\) \{[\s\S]*?\}[\s\S]*?\}/;

const newFunc = `
const Database = require('better-sqlite3');
const sqlDb = new Database(path.join(__dirname, 'database.sqlite'));
sqlDb.pragma('journal_mode = WAL');

sqlDb.exec(\`
  CREATE TABLE IF NOT EXISTS users (
    key TEXT PRIMARY KEY,
    data TEXT
  )
\`);

const stmtInsert = sqlDb.prepare('INSERT OR REPLACE INTO users (key, data) VALUES (?, ?)');
const stmtDelete = sqlDb.prepare('DELETE FROM users WHERE key = ?');

function saveDatabase(db, modifiedKey = null) {
    try {
        if (modifiedKey) {
            if (!db[modifiedKey]) {
                stmtDelete.run(modifiedKey);
            } else {
                stmtInsert.run(modifiedKey, JSON.stringify(db[modifiedKey]));
            }
        } else {
            // Fallback: save all
            const insertMany = sqlDb.transaction((database) => {
                for (const key in database) {
                    stmtInsert.run(key, JSON.stringify(database[key]));
                }
            });
            insertMany(db);
        }
    } catch (e) {
        console.error('[SUN-DB] SQLite Save Error:', e);
    }
}
`;

code = lines.join('\n');
code = code.replace(oldFuncRegex, newFunc);

// Also replace loadDatabase()
const loadFuncRegex = /function loadDatabase\(\) \{[\s\S]*?return db;\r?\n\}/;

const newLoadFunc = `
function loadDatabase() {
    let db = {};
    try {
        const rows = sqlDb.prepare('SELECT key, data FROM users').all();
        for (const row of rows) {
            try {
                db[row.key] = JSON.parse(row.data);
            } catch(e) {}
        }
        console.log(\`[SUN-DB] Загружено \${Object.keys(db).length} аккаунтов из SQLite.\`);
    } catch (e) {
        console.error('[SUN-DB] Ошибка загрузки SQLite:', e);
    }
    return db;
}
`;

code = code.replace(loadFuncRegex, newLoadFunc);

fs.writeFileSync('C:/Users/edya/Desktop/sun-server-deploy/server/server.js', code);
console.log('Migrated codebase to SQLite!');
