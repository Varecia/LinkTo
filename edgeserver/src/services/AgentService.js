const axios = require('axios');
const config = require('../config/config');
const { v4: uuidv4 } = require('uuid');

class AgentService {
    constructor() {
        this.apiUrl = config.agent.apiUrl;
        this.timeout = config.agent.timeout;
        this.sessionCache = new Map();
    }

    async process(perceptionData) {
        try {
            console.log(`[AgentService] 处理感知数据，用户: ${perceptionData.userId}`);

            const requestBody = this.buildAgentRequest(perceptionData);

            const startTime = Date.now();
            const response = await axios.post(
                `${this.apiUrl}/api/agent/process`,
                requestBody,
                {
                    timeout: this.timeout,
                    headers: {
                        'Content-Type': 'application/json',
                    }
                }
            );
            const processingTime = Date.now() - startTime;

            const decisionPlan = this.parseAgentResponse(response.data, perceptionData, processingTime);

            this.updateSessionContext(perceptionData.sessionId, {
                lastInput: perceptionData,
                lastDecision: decisionPlan,
                timestamp: Date.now()
            });

            return decisionPlan;

        } catch (error) {
            console.error('[AgentService] 智能体调用失败:', error.message);

            return this.getFallbackDecision(perceptionData, error);
        }
    }

    buildAgentRequest(perceptionData) {
        const sessionContext = this.sessionCache.get(perceptionData.sessionId);

        return {
            requestId: uuidv4(),
            timestamp: perceptionData.timestamp,
            userId: perceptionData.userId,
            sessionId: perceptionData.sessionId,

            perception: {
                semanticMap: perceptionData.semanticMap,
                userContext: perceptionData.userContext,
                environmentContext: perceptionData.environmentContext,
            },

            prompt: perceptionData.toPrompt(),

            history: sessionContext ? {
                lastDecision: sessionContext.lastDecision,
                contextWindow: 3,
            } : null,

            emergency: perceptionData.emergency,

            config: {
                responseFormat: 'structured',
                maxTokens: 1000,
                temperature: 0.3,
            }
        };
    }

    parseAgentResponse(agentResponse, perceptionData, processingTime) {
        const plan = {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            userId: perceptionData.userId,
            sessionId: perceptionData.sessionId,

            plan: {
                primaryGoal: agentResponse.primaryGoal || '安全移动',
                actions: (agentResponse.actions || []).map(a => ({
                    description: a.description,
                    immediate: a.immediate || false,
                    priority: a.priority || 'medium',
                    hapticPattern: a.hapticPattern
                })),
                warnings: (agentResponse.warnings || []).map(w => ({
                    message: w.message,
                    priority: w.priority || 'high',
                    hapticPattern: w.hapticPattern || 'vibrate_strong'
                })),
                information: (agentResponse.information || []).map(i => ({
                    message: i.message,
                    priority: i.priority || 'low'
                }))
            },

            metadata: {
                confidence: agentResponse.confidence || 0.8,
                processingTime: processingTime,
                agentVersion: agentResponse.version || '1.0.0',
                emergency: perceptionData.emergency
            }
        };

        return new (require('../models/DecisionPlan'))(plan);
    }

    getFallbackDecision(perceptionData, error) {
        console.warn('[AgentService] 使用备用决策');

        const fallbackPlan = {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            userId: perceptionData.userId,
            sessionId: perceptionData.sessionId,

            plan: {
                primaryGoal: '安全第一',
                actions: [
                    {
                        description: '请放慢脚步，注意周围环境',
                        immediate: true,
                        priority: 'medium'
                    }
                ],
                warnings: perceptionData.semanticMap.hazards.length > 0 ? [
                    {
                        message: '检测到潜在危险，请小心',
                        priority: 'high',
                        hapticPattern: 'vibrate_strong'
                    }
                ] : [],
                information: [
                    {
                        message: '系统正在尝试重新连接智能服务',
                        priority: 'low'
                    }
                ]
            },

            metadata: {
                confidence: 0.3,
                processingTime: 0,
                agentVersion: 'fallback',
                emergency: perceptionData.emergency,
                error: error.message
            }
        };

        return new (require('../models/DecisionPlan'))(fallbackPlan);
    }

    updateSessionContext(sessionId, context) {
        this.sessionCache.set(sessionId, context);

        const oneHourAgo = Date.now() - 3600000;
        for (const [id, ctx] of this.sessionCache.entries()) {
            if (ctx.timestamp < oneHourAgo) {
                this.sessionCache.delete(id);
            }
        }
    }
}

module.exports = new AgentService();