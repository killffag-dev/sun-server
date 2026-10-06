const fs = require('fs');

function fixProfile(filepath) {
    let code = fs.readFileSync(filepath, 'utf8');

    // Remove `if (pairCard) pairCard.style.display = 'none';`
    code = code.replace(/if\s*\(pairCard\)\s*pairCard\.style\.display\s*=\s*'none';/g, "if (pairCard) pairCard.style.display = ''; // Freemium: allow re-linking anytime");

    fs.writeFileSync(filepath, code);
}

fixProfile('C:\\Users\\edya\\Desktop\\sun-server-deploy\\site\\profile.html');
fixProfile('C:\\Users\\edya\\Desktop\\SUN\\site\\profile.html');
console.log('Profile linking fixed!');
