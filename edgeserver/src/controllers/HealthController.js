const config = require('../config/config');
const os = require('os');


class HealthController {

    async check(req, res) {
        try {
            res.json({
                success: true,
                status: 'healthy',
                timestamp: new Date().toISOString(),
                service: 'LinkTo Edge Server',
                version: '1.0.0',
                uptime: process.uptime()
            });
        } catch (error) {
            res.status(500).json({
                success: false,
                status: 'unhealthy',
                error: error.message
            });
        }
    }

    async detailed(req, res) {
        try {
            const agentStatus = await this.checkAgentConnection();

            const memoryUsage = process.memoryUsage();
            const totalMemory = os.totalmem();
            const freeMemory = os.freemem();

            const systemInfo = {
                hostname: os.hostname(),
                platform: os.platform(),
                arch: os.arch(),
                cpus: os.cpus().length,
                loadAvg: os.loadavg(),
                uptime: os.uptime()
            };

            const processInfo = {
                pid: process.pid,
                uptime: process.uptime(),
                version: process.version,
                memoryUsage: {
                    rss: this.formatBytes(memoryUsage.rss),
                    heapTotal: this.formatBytes(memoryUsage.heapTotal),
                    heapUsed: this.formatBytes(memoryUsage.heapUsed),
                    external: this.formatBytes(memoryUsage.external)
                },
                cpuUsage: process.cpuUsage()
            };

            const environmentInfo = {
                nodeEnv: config.server.env,
                mockMode: config.agent.mockEnabled,
                agentApiUrl: config.agent.apiUrl,
                port: config.server.port
            };

            res.json({
                success: true,
                status: 'healthy',
                timestamp: new Date().toISOString(),
                service: {
                    name: 'LinkTo Edge Server',
                    version: '1.0.0',
                    agentConnection: agentStatus
                },
                system: systemInfo,
                process: processInfo,
                environment: environmentInfo,
                resources: {
                    totalMemory: this.formatBytes(totalMemory),
                    freeMemory: this.formatBytes(freeMemory),
                    memoryUsagePercent: ((totalMemory - freeMemory) / totalMemory * 100).toFixed(2) + '%'
                }
            });
        } catch (error) {
            res.status(500).json({
                success: false,
                status: 'unhealthy',
                timestamp: new Date().toISOString(),
                error: error.message,
                stack: config.server.env === 'development' ? error.stack : undefined
            });
        }
    }

    async readiness(req, res) {
        try {
            const checks = await Promise.allSettled([
                this.checkAgentConnection(),
                this.checkDiskSpace()
            ]);

            const allReady = checks.every(check =>
                check.status === 'fulfilled' && check.value === true
            );

            if (allReady) {
                res.json({
                    success: true,
                    status: 'ready',
                    timestamp: new Date().toISOString()
                });
            } else {
                res.status(503).json({
                    success: false,
                    status: 'not ready',
                    timestamp: new Date().toISOString(),
                    checks: checks.map((check, index) => ({
                        name: ['agent', 'disk'][index],
                        ready: check.status === 'fulfilled' ? check.value : false,
                        error: check.status === 'rejected' ? check.reason?.message : undefined
                    }))
                });
            }
        } catch (error) {
            res.status(500).json({
                success: false,
                status: 'error',
                error: error.message
            });
        }
    }

    async liveness(req, res) {
        res.json({
            success: true,
            status: 'alive',
            timestamp: new Date().toISOString(),
            uptime: process.uptime()
        });
    }

    async checkAgentConnection() {
        if (config.agent.mockEnabled) {
            return { connected: true, mode: 'mock' };
        }

        try {
            const axios = require('axios');
            const response = await axios.get(`${config.agent.apiUrl}/api/health`, {
                timeout: 5000,
                validateStatus: false
            });

            return {
                connected: response.status >= 200 && response.status < 500,
                statusCode: response.status,
                mode: 'real',
                url: config.agent.apiUrl
            };
        } catch (error) {
            return {
                connected: false,
                mode: 'real',
                url: config.agent.apiUrl,
                error: error.code || error.message
            };
        }
    }

    async checkDiskSpace() {
        return new Promise((resolve) => {
            // TODO: check disk
            try {
                const df = require('node-df') || (() => {});
                df((err, disks) => {
                    if (err) {
                        resolve(false);
                    } else {
                        const rootDisk = disks.find(d => d.filesystem === '/') || disks[0];
                        const availablePercent = (rootDisk.available / rootDisk.size) * 100;
                        resolve(availablePercent > 10);
                    }
                });
            } catch (e) {
                resolve(true);
            }
        });
    }

    async dependencies(req, res) {
        const dependencies = {
            agent: await this.checkAgentConnection(),
            knowledgeGraph: await this.checkKnowledgeGraphConnection(),
            database: await this.checkDatabaseConnection()
        };

        const allConnected = Object.values(dependencies)
            .every(dep => dep.connected !== false);

        res.json({
            success: true,
            timestamp: new Date().toISOString(),
            allConnected,
            dependencies
        });
    }

    async checkKnowledgeGraphConnection() {
        // TODO: check KG connection
        if (!config.knowledgeGraph.apiUrl) {
            return { connected: false, reason: 'not configured' };
        }

        try {
            const axios = require('axios');
            const response = await axios.get(`${config.knowledgeGraph.apiUrl}/api/health`, {
                timeout: 5000,
                validateStatus: false
            });

            return {
                connected: response.status >= 200 && response.status < 500,
                statusCode: response.status,
                url: config.knowledgeGraph.apiUrl
            };
        } catch (error) {
            return {
                connected: false,
                url: config.knowledgeGraph.apiUrl,
                error: error.code || error.message
            };
        }
    }

    async checkDatabaseConnection() {
        // TODO: check database connection
        return {
            connected: true,
            type: 'memory',
            note: '使用内存存储，无需数据库连接'
        };
    }

    formatBytes(bytes, decimals = 2) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];

        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }
}

module.exports = new HealthController();