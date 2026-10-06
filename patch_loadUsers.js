const fs = require('fs');
const file = './server/server.js';
let content = fs.readFileSync(file, 'utf8');

content = content.replace(
    /const res = await fetch\('\/api\/admin\/users'\);\s*if \(res\.ok\) \{\s*allUsers = await res\.json\(\);\s*renderTable\(\);\s*\}/,
    `const res = await fetch('/api/admin/users', { headers: { 'X-Admin-Token': getAuthToken() } });
                if (res.ok) {
                    allUsers = await res.json();
                    renderTable();
                } else if (res.status === 401) {
                    document.cookie = 'sun_admin_token=; Max-Age=0; path=/';
                    location.reload();
                }`
);

fs.writeFileSync(file, content, 'utf8');
