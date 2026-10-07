const http = require('http');
const cp = require('child_process');

async function runTest() {
    const port = 9991;
    const serverProcess = cp.spawn('node', ['server.js'], {
        cwd: 'server',
        env: { ...process.env, PORT: String(port) }
    });

    serverProcess.stdout.on('data', d => console.log('[SERVER STDOUT]', d.toString().trim()));
    serverProcess.stderr.on('data', d => console.error('[SERVER STDERR]', d.toString().trim()));

    // Wait for server to boot
    await new Promise(r => setTimeout(r, 1500));

    function req(path, method = 'GET', body = null, headers = {}) {
        return new Promise((resolve, reject) => {
            const r = http.request({
                hostname: '127.0.0.1',
                port: port,
                path: path,
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    ...headers
                }
            }, (res) => {
                let data = '';
                res.on('data', c => data += c);
                res.on('end', () => {
                    try {
                        resolve({ status: res.statusCode, data: JSON.parse(data), headers: res.headers });
                    } catch {
                        resolve({ status: res.statusCode, data: data, headers: res.headers });
                    }
                });
            });
            r.on('error', reject);
            if (body) r.write(typeof body === 'string' ? body : JSON.stringify(body));
            r.end();
        });
    }

    try {
        console.log('\n--- 1. Testing GET /api/generate-link-code ---');
        const genRes = await req('/api/generate-link-code');
        console.log('Status:', genRes.status, 'Body:', genRes.data);
        if (!genRes.data.code || !genRes.data.token) throw new Error('Invalid generate-link-code response');
        const code = genRes.data.code;
        const token = genRes.data.token;

        console.log('\n--- 2. Testing GET /api/link-status (pending) ---');
        const pendingStatus = await req('/api/link-status?code=' + code + '&token=' + token);
        console.log('Status:', pendingStatus.status, 'Body:', pendingStatus.data);
        if (pendingStatus.data.linked !== false || !pendingStatus.data.pending) throw new Error('Status should be pending');

        console.log('\n--- 3. Testing POST /api/auth/pair/link with user session ---');
        // Log in to get session cookie or use direct session
        const loginRes = await req('/api/login', 'POST', {
            username: '12345678',
            password: '12345678'
        });
        console.log('Login res:', loginRes.status, loginRes.data);
        const cookie = loginRes.headers['set-cookie'] ? loginRes.headers['set-cookie'][0].split(';')[0] : '';
        console.log('Session Cookie:', cookie);

        const linkRes = await req('/api/auth/pair/link', 'POST', {
            code: code.substring(0, 3) + '-' + code.substring(3, 6)
        }, { Cookie: cookie });
        console.log('Pair link res:', linkRes.status, linkRes.data);
        if (!linkRes.data.success) throw new Error('Linking failed');

        console.log('\n--- 4. Testing GET /api/link-status (after link) ---');
        const linkedStatus = await req('/api/link-status?code=' + code + '&token=' + token);
        console.log('Status:', linkedStatus.status, 'Body:', linkedStatus.data);
        if (!linkedStatus.data.linked || linkedStatus.data.username !== '12345678') throw new Error('Should be linked to 12345678');

        console.log('\n--- 5. Testing GET /api/sparks-balance ---');
        const sparksRes = await req('/api/sparks-balance?key=SUN-WALU-DPNK');
        console.log('Status:', sparksRes.status, 'Body:', sparksRes.data);
        if (sparksRes.data.username !== '12345678' || !sparksRes.data.linked) throw new Error('Invalid sparks-balance');

        console.log('\n--- 6. Testing Static site & profile & admin ---');
        const siteIndex = await req('/');
        console.log('GET / Status:', siteIndex.status);
        const siteProfile = await req('/profile.html');
        console.log('GET /profile.html Status:', siteProfile.status);
        const siteAdmin = await req('/admin');
        console.log('GET /admin Status:', siteAdmin.status);

        console.log('\nALL TESTS PASSED SUCCESSFULLY! ✓✓✓');
    } finally {
        serverProcess.kill();
    }
}

runTest().catch(e => {
    console.error('TEST ERROR:', e);
    process.exit(1);
});
