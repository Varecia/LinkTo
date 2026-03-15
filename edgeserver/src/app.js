const express = require('express');
const cors = require('cors');
const config = require('./config/config');
const apiRoutes = require('./routes/api');

const app = express();

app.use(cors({
    origin: ['http://10.0.2.2:8080', 'http://localhost:8080'],
    credentials: true
}));

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

app.use('/api', (req, res, next) => {
    if (req.path === '/health' || req.path === '/version') {
        return next();
    }

    const apiKey = req.headers['x-api-key'];
    if (!apiKey || apiKey !== config.security.apiKey) {
        return res.status(401).json({
            success: false,
            error: 'Missing or invalid API key'
        });
    }
    next();
});

app.use('/api', apiRoutes);

app.get('/', (req, res) => {
    res.json({
        name: 'LinkTo Edge Server',
        version: '1.0.0',
        status: 'running',
        endpoints: {
            process: '/api/agent/process',
            health: '/api/health',
            version: '/api/version'
        },
        mockMode: config.agent.mockEnabled
    });
});

app.use((req, res) => {
    res.status(404).json({
        success: false,
        error: 'Route not found'
    });
});

app.use((err, req, res, next) => {
    console.error('服务器错误:', err.stack);
    res.status(500).json({
        success: false,
        error: 'Internal server error',
        message: config.server.env === 'development' ? err.message : undefined
    });
});

const PORT = config.server.port;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`=================================`);
    console.log(`LinkTo Edge Server 已启动`);
    console.log(`=================================`);
    console.log(`端口: ${PORT}`);
    console.log(`环境: ${config.server.env}`);
    console.log(`智能体模式: ${config.agent.mockEnabled ? '模拟' : '真实'}`);
    console.log(`智能体API: ${config.agent.apiUrl}`);
    console.log(`=================================`);
    console.log(`本地访问: http://localhost:${PORT}`);
    console.log(`模拟器访问: http://10.0.2.2:${PORT}`);
    console.log(`=================================`);
});