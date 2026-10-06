const fs = require('fs');

function patch(filepath) {
    let lines = fs.readFileSync(filepath, 'utf8').split('\n');
    let newLines = [];
    let skip = false;

    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];

        // 1. Remove Ban button
        if (line.includes('<button class="action-btn ban"')) {
            continue; // Skip this line
        }

        // 2. Set isBanned to false in renderTable
        if (line.includes('const isBanned = !!user.banned;')) {
            if (i < 1000) { // UI part
                line = line.replace('const isBanned = !!user.banned;', 'const isBanned = false;');
            }
        }

        // 3. Remove api/check checks
        if (line.includes('const isBanned = !!user.banned;') && i > 1500) {
            skip = true; // Start skipping
        }

        if (skip && line.includes('Проверка и аппаратная привязка')) {
            skip = false; // Stop skipping when reaching HWID check
        }
        
        if (line.includes('Проверка и аппаратная привязка') || line.includes('HWID Lock')) {
            // Replace the HWID lock block
            newLines.push('        // Freemium: HWID Lock removed');
            newLines.push('        if (hwid) {');
            newLines.push('            user.hwid = hwid;');
            newLines.push('            user.lastSeen = new Date().toISOString().replace(\'T\', \' \').substring(0, 16);');
            newLines.push('            saveDatabase(database);');
            newLines.push('        }');
            
            // Skip the next 15 lines that make up the original HWID check
            let openBraces = 0;
            let j = i + 1;
            while(j < lines.length) {
                if (lines[j].includes('console.log(`[SUN-API] [OK]')) {
                    i = j - 1;
                    break;
                }
                j++;
            }
            continue;
        }

        // 4. Remove auth.user.banned check
        if (line.includes('if (auth.user.banned) {')) {
            // skip until the closing brace
            let j = i + 1;
            while (j < lines.length) {
                if (lines[j].includes('const u = auth.user;')) {
                    i = j - 1;
                    break;
                }
                j++;
            }
            continue; // Skip the 'if (auth.user.banned) {' line
        }

        if (!skip) {
            newLines.push(line);
        }
    }

    fs.writeFileSync(filepath, newLines.join('\n'));
}

patch('C:\\Users\\edya\\Desktop\\sun-server-deploy\\server\\server.js');
patch('C:\\Users\\edya\\Desktop\\SUN\\server\\server.js');
console.log('Fixed safely by line parsing!');
