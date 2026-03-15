const PerceptionData = require('../models/PerceptionData');
const config = require('../config/config');
const agentService = require('../services/AgentService');
const mockAgentService = require('../services/MockAgentService');
const { v4: uuidv4 } = require('uuid');

class AgentController {

    async processPerception(req, res) {
        try {
            const startTime = Date.now();

            const apiKey = req.headers['x-api-key'];
            if (apiKey !== config.security.apiKey) {
                return res.status(401).json({
                    success: false,
                    error: 'Invalid API key'
                });
            }

            if (!req.body || Object.keys(req.body).length === 0) {
                return res.status(400).json({
                    success: false,
                    error: 'Request body is empty'
                });
            }

            const perceptionData = new PerceptionData({
                ...req.body,
                timestamp: new Date().toISOString(),
                sessionId: req.body.sessionId || uuidv4()
            });

            if (!perceptionData.isValid()) {
                return res.status(400).json({
                    success: false,
                    error: 'Invalid perception data'
                });
            }

            console.log(`[AgentController] 处理感知请求 - 用户: ${perceptionData.userId}, 场景: ${perceptionData.semanticMap.sceneType}`);

            const service = config.agent.mockEnabled ? mockAgentService : agentService;
            const decisionPlan = await service.process(perceptionData);

            const processingTime = Date.now() - startTime;

            res.json({
                success: true,
                requestId: req.body.requestId || uuidv4(),
                processingTime,
                decision: decisionPlan.toFeedback(),
                metadata: {
                    mockMode: config.agent.mockEnabled,
                    agentVersion: decisionPlan.metadata.agentVersion,
                    confidence: decisionPlan.metadata.confidence
                }
            });

        } catch (error) {
            console.error('[AgentController] 处理失败:', error);

            res.status(500).json({
                success: false,
                error: 'Internal server error',
                message: error.message
            });
        }
    }

    async processPerceptionStream(req, res) {
        res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive',
        });

        const sessionId = req.body.sessionId || uuidv4();

        const heartbeat = setInterval(() => {
            res.write(`event: heartbeat\ndata: ${JSON.stringify({ timestamp: Date.now() })}\n\n`);
        }, 30000);

        req.on('close', () => {
            clearInterval(heartbeat);
            res.end();
        });

        try {
            // TODO: establish WebSocket connection
            res.write(`event: error\ndata: ${JSON.stringify({ message: 'Stream mode not fully implemented' })}\n\n`);

        } catch (error) {
            console.error('[AgentController] 流式处理失败:', error);
            res.write(`event: error\ndata: ${JSON.stringify({ message: error.message })}\n\n`);
        }
    }

    async getSessionHistory(req, res) {
        try {
            const { sessionId } = req.params;

            // TODO: get log from database
            res.json({
                success: true,
                sessionId,
                history: [
                    {
                        timestamp: new Date().toISOString(),
                        type: 'perception',
                        data: '示例历史记录'
                    }
                ]
            });

        } catch (error) {
            console.error('[AgentController] 获取会话历史失败:', error);
            res.status(500).json({
                success: false,
                error: 'Failed to get session history'
            });
        }
    }
}

module.exports = new AgentController();