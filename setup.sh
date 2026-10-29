#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 1. Creando carpetas ==="
mkdir -p app/src/main/jniLibs/arm64-v8a
mkdir -p app/src/main/assets/nodejs-project

echo "=== 2. Descargando yt-dlp standalone ARM64 ==="
cd app/src/main/jniLibs/arm64-v8a
wget -q https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64 -O libytdlp.so
chmod +x libytdlp.so
echo "yt-dlp: $(ls -lh libytdlp.so | awk '{print $5}')"

echo "=== 3. Descargando FFmpeg estático ARM64 ==="
wget -q https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz -O /tmp/ffmpeg.tar.xz
cd /tmp
tar xf ffmpeg.tar.xz
FFDIR=$(ls -d ffmpeg-*-arm64-static | head -1)
cp "$FFDIR/ffmpeg" ~/ADRJmusic/app/src/main/jniLibs/arm64-v8a/libffmpeg.so
cp "$FFDIR/ffprobe" ~/ADRJmusic/app/src/main/jniLibs/arm64-v8a/libffprobe.so
chmod +x ~/ADRJmusic/app/src/main/jniLibs/arm64-v8a/libffmpeg.so
chmod +x ~/ADRJmusic/app/src/main/jniLibs/arm64-v8a/libffprobe.so
rm -rf /tmp/ffmpeg.tar.xz /tmp/$FFDIR

echo "=== 4. Verificando binarios ==="
cd ~/ADRJmusic/app/src/main/jniLibs/arm64-v8a
file libytdlp.so | head -1
file libffmpeg.so | head -1
file libffprobe.so | head -1
ls -lh

echo "=== 5. Escribiendo main.js ==="
cat > ~/ADRJmusic/app/src/main/assets/nodejs-project/main.js << 'JS_EOF'
const express = require('express');
const { spawn } = require('child_process');

const app = express();
const PORT = 3000;

// Ruta nativa (viene como --native-dir=... al arrancar)
const arg = process.argv.find(a => a.startsWith('--native-dir='));
const NATIVE_DIR = arg ? arg.split('=')[1] : '/data/local/tmp';
const YTDLP = NATIVE_DIR + '/libytdlp.so';
const FFMPEG = NATIVE_DIR + '/libffmpeg.so';

console.log('[server] NATIVE_DIR =', NATIVE_DIR);
console.log('[server] YTDLP =', YTDLP);
console.log('[server] FFMPEG =', FFMPEG);

const searchCache = new Map();
const CACHE_TTL = 5 * 60 * 1000;

app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type, Range');
    res.header('Access-Control-Expose-Headers', 'Content-Length, Content-Range, Accept-Ranges');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    next();
});

app.get('/health', (req, res) => res.send('OK'));

function run(args, onData) {
    return new Promise((resolve, reject) => {
        const proc = spawn(YTDLP, args);
        let out = '', err = '';
        proc.stdout.on('data', d => { out += d.toString(); if (onData) onData(d.toString()); });
        proc.stderr.on('data', d => err += d.toString());
        proc.on('close', code => code === 0 ? resolve(out) : reject(new Error(err || 'exit ' + code)));
        proc.on('error', reject);
    });
}

app.get('/api/search', async (req, res) => {
    const q = req.query.q;
    if (!q) return res.json([]);
    const key = q.toLowerCase(), now = Date.now();
    if (searchCache.has(key) && now - searchCache.get(key).t < CACHE_TTL)
        return res.json(searchCache.get(key).v);
    try {
        const out = await run(['ytsearch20:' + q, '--dump-single-json', '--flat-playlist',
                               '--no-warnings', '--no-check-certificates', '--quiet',
                               '--ffmpeg-location', FFMPEG]);
        const data = JSON.parse(out);
        const tracks = (data.entries || []).map(v => ({
            id: v.id,
            title: v.title || 'Sin título',
            artist: v.uploader || v.channel || 'Desconocido',
            art: 'https://i.ytimg.com/vi/' + v.id + '/maxresdefault.jpg'
        }));
        searchCache.set(key, { v: tracks, t: now });
        res.json(tracks);
    } catch (e) {
        console.error('[search]', e.message);
        res.json([]);
    }
});

app.get('/api/stream', async (req, res) => {
    const id = req.query.id;
    if (!id) return res.status(400).send('falta id');
    try {
        const url = 'https://www.youtube.com/watch?v=' + id;
        const out = await run([url, '-f', 'bestaudio[ext=m4a]/bestaudio/best',
                               '-g', '--no-warnings', '--no-check-certificates', '--quiet',
                               '--ffmpeg-location', FFMPEG]);
        const audioUrl = out.trim().split('\n')[0];
        if (!audioUrl) return res.status(500).send('sin url');
        res.redirect(audioUrl);
    } catch (e) {
        res.status(500).send(e.message);
    }
});

app.get('/api/download', async (req, res) => {
    const id = req.query.id;
    if (!id) return res.status(400).send('falta id');
    try {
        const url = 'https://www.youtube.com/watch?v=' + id;
        res.setHeader('Content-Type', 'audio/mpeg');
        res.setHeader('Content-Disposition', 'attachment; filename="' + id + '.m4a"');
        const proc = spawn(YTDLP, [url, '-f', 'bestaudio[ext=m4a]/bestaudio/best',
                                    '-o', '-', '--no-warnings', '--quiet',
                                    '--ffmpeg-location', FFMPEG]);
        proc.stdout.pipe(res);
        proc.on('error', e => { if (!res.headersSent) res.status(500).send(e.message); });
        req.on('close', () => proc.kill('SIGKILL'));
    } catch (e) {
        if (!res.headersSent) res.status(500).send(e.message);
    }
});

app.listen(PORT, '127.0.0.1', () => {
    console.log('[server] ADRJmusic en http://127.0.0.1:' + PORT);
});
JS_EOF

echo "=== 6. npm install ==="
cd ~/ADRJmusic/app/src/main/assets/nodejs-project
cat > package.json << 'PKG_EOF'
{
  "name": "adrj-server",
  "version": "1.0.0",
  "main": "main.js",
  "dependencies": { "express": "^4.19.2" }
}
PKG_EOF
npm install --production --silent

echo ""
echo "=== TODO LISTO ==="
du -sh ~/ADRJmusic/app/src/main/jniLibs/arm64-v8a/
du -sh ~/ADRJmusic/app/src/main/assets/nodejs-project/
