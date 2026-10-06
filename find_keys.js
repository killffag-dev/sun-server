const fs = require('fs');

let code = fs.readFileSync('C:/Users/edya/Desktop/sun-server-deploy/server/server.js', 'utf8');
const lines = code.split('\n');

for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('saveDatabase(database)')) {
        // Look up to find the closest database[XXXX]
        let foundKey = null;
        for (let j = i; j >= Math.max(0, i - 30); j--) {
            const match = lines[j].match(/database\[([a-zA-Z0-9_]+)\]/);
            if (match && match[1] !== 'k') { // ignore database[k] from loops
                foundKey = match[1];
                break;
            }
            if (lines[j].includes('delete database[')) {
                const delMatch = lines[j].match(/delete database\[([a-zA-Z0-9_]+)\]/);
                if (delMatch) {
                    foundKey = delMatch[1];
                    break;
                }
            }
            if (lines[j].match(/const ([a-zA-Z0-9_]+) = /)) {
                // sometimes it's just a local var assignment before save
            }
        }
        console.log(`Line ${i+1}: nearest key is ${foundKey}`);
    }
}
