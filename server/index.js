const express = require('express');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Env vars en Render (nunca en código)
const ADMIN_KEY = process.env.ADMIN_KEY || 'cambia_esto';
const SIGNING_KEY = process.env.SIGNING_KEY || 'cambia_esto';

const CONFIG_FILE = '/tmp/adrj-config.json';

function loadConfig() {
    try { return JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8')); }
    catch {
        return {
            latestVersionCode: 1,
            latestVersionName: "1.0.0",
            forceUpdate: false,
            minimumVersionCode: 1,
            message: "Actualización disponible",
            downloadUrl: "",
            publishedAt: new Date().toISOString().replace(/\.\d+Z$/, 'Z')
        };
    }
}

function saveConfig(cfg) {
    fs.writeFileSync(CONFIG_FILE, JSON.stringify(cfg, null, 2));
}

function signCanonical(payload) {
    // Orden: latestVersionCode, latestVersionName, forceUpdate, minimumVersionCode, message, downloadUrl, publishedAt
    const canonical = '{"latestVersionCode":' + payload.latestVersionCode +
        ',"latestVersionName":"' + payload.latestVersionName +
        '","forceUpdate":' + payload.forceUpdate +
        ',"minimumVersionCode":' + payload.minimumVersionCode +
        ',"message":"' + payload.message +
        '","downloadUrl":"' + payload.downloadUrl +
        '","publishedAt":"' + payload.publishedAt + '"}';
    return crypto.createHmac('sha256', SIGNING_KEY).update(canonical).digest('hex');
}

// PÚBLICO: la app consulta esto
app.get('/api/version', (req, res) => {
    const cfg = loadConfig();
    const payload = {
        latestVersionCode: cfg.latestVersionCode,
        latestVersionName: cfg.latestVersionName,
        forceUpdate: cfg.forceUpdate,
        minimumVersionCode: cfg.minimumVersionCode,
        message: cfg.message,
        downloadUrl: cfg.downloadUrl,
        publishedAt: cfg.publishedAt
    };
    const signature = signCanonical(payload);
    res.json({ ...payload, signature });
});

// ADMIN: actualizar configuración
app.post('/api/admin/update', express.json(), (req, res) => {
    if (req.headers['x-admin-key'] !== ADMIN_KEY) {
        return res.status(401).json({ error: 'unauthorized' });
    }
    const cfg = loadConfig();
    const b = req.body;
    if (typeof b.latestVersionCode === 'number') cfg.latestVersionCode = b.latestVersionCode;
    if (typeof b.latestVersionName === 'string') cfg.latestVersionName = b.latestVersionName;
    if (typeof b.forceUpdate === 'boolean') cfg.forceUpdate = b.forceUpdate;
    if (typeof b.minimumVersionCode === 'number') cfg.minimumVersionCode = b.minimumVersionCode;
    if (typeof b.message === 'string') cfg.message = b.message;
    if (typeof b.downloadUrl === 'string') cfg.downloadUrl = b.downloadUrl;
    cfg.publishedAt = new Date().toISOString().replace(/\.\d+Z$/, 'Z');
    saveConfig(cfg);
    res.json({ ok: true, config: cfg });
});

// ADMIN: forzar actualización
app.post('/api/admin/kill', express.json(), (req, res) => {
    if (req.headers['x-admin-key'] !== ADMIN_KEY) {
        return res.status(401).json({ error: 'unauthorized' });
    }
    const cfg = loadConfig();
    cfg.forceUpdate = true;
    if (req.body.message) cfg.message = req.body.message;
    if (req.body.latestVersionCode) cfg.latestVersionCode = req.body.latestVersionCode;
    if (req.body.latestVersionName) cfg.latestVersionName = req.body.latestVersionName;
    if (req.body.downloadUrl) cfg.downloadUrl = req.body.downloadUrl;
    cfg.publishedAt = new Date().toISOString().replace(/\.\d+Z$/, 'Z');
    saveConfig(cfg);
    res.json({ ok: true, config: cfg });
});

// HEALTH CHECK (para UptimeRobot)
app.get('/', (req, res) => res.send('OK'));
app.get('/health', (req, res) => res.json({ status: 'ok', time: Date.now() }));

// Iniciar servidor
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Servidor corriendo en puerto ${PORT}`);
});
