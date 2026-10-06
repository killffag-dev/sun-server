const fs = require('fs');

function fix(filepath) {
    let code = fs.readFileSync(filepath, 'utf8');

    // Fix the `${safeKey}` to `\${safeKey}`
    code = code.replace(/id="sparksInput_\$\{safeKey\}"/g, 'id="sparksInput_\\${safeKey}"');
    code = code.replace(/modifySparks\('\$\{safeKey\}',/g, 'modifySparks(\'\\${safeKey}\',');

    // Remove stray }); if exists
    code = code.replace(/sendAction\(\{ action: 'add_coins', hwid, amount: val \* multiplier \}\);\r?\n\s*\}\);\r?\n\s*\}/, 
                        "sendAction({ action: 'add_coins', hwid, amount: val * multiplier });\n        }");

    fs.writeFileSync(filepath, code);
}

fix('C:\\Users\\edya\\Desktop\\sun-server-deploy\\server\\server.js');
fix('C:\\Users\\edya\\Desktop\\SUN\\server\\server.js');
console.log('Fixed successfully!');
