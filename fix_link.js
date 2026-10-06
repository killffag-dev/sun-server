const fs = require('fs');

function fixLinking(filepath) {
    let code = fs.readFileSync(filepath, 'utf8');

    // 1. Remove "Этот аккаунт уже привязан" check in /api/link
    // It probably looks like: if (u.clientLinked || u.hwid) { ... error: "уже привязано" ... }
    code = code.replace(/if\s*\(\s*u\.clientLinked\s*\|\|\s*u\.hwid\s*\)\s*\{[\s\S]*?return res\.end.*?\}\s*\}/g, '');

    // 2. Remove 3-day cooldown in /api/reset-hwid
    // It looks like: if (user.hwid_last_reset) { const lastReset = ... if (timePassed < cooldownMs) { ... error ... } }
    code = code.replace(/if\s*\(\s*user\.hwid_last_reset\s*\)\s*\{[\s\S]*?const timePassed[\s\S]*?if\s*\(timePassed\s*<\s*cooldownMs\)\s*\{[\s\S]*?return res\.end.*?\}\s*\}\s*\}/g, '');

    fs.writeFileSync(filepath, code);
}

fixLinking('C:\\Users\\edya\\Desktop\\sun-server-deploy\\server\\server.js');
fixLinking('C:\\Users\\edya\\Desktop\\SUN\\server\\server.js');
console.log('Linking restrictions removed!');
