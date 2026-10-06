const fs = require('fs');
const Database = require('better-sqlite3');
const path = require('path');

const DB_FILE = path.join(__dirname, 'database.json');
const SQLITE_FILE = path.join(__dirname, 'database.sqlite');

if (fs.existsSync(DB_FILE)) {
    const data = JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
    
    const sqlDb = new Database(SQLITE_FILE);
    sqlDb.pragma('journal_mode = WAL');

    sqlDb.exec(`
      CREATE TABLE IF NOT EXISTS users (
        key TEXT PRIMARY KEY,
        data TEXT
      )
    `);

    const stmtInsert = sqlDb.prepare('INSERT OR REPLACE INTO users (key, data) VALUES (?, ?)');
    
    const insertMany = sqlDb.transaction((database) => {
        for (const key in database) {
            stmtInsert.run(key, JSON.stringify(database[key]));
        }
    });

    insertMany(data);
    console.log(`Migrated ${Object.keys(data).length} users to SQLite!`);
    sqlDb.close();
} else {
    console.log('No database.json found to migrate.');
}
