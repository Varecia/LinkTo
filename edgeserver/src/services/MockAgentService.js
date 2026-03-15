const { v4: uuidv4 } = require('uuid');
const DecisionPlan = require('../models/DecisionPlan');

class MockAgentService {

    async process(perceptionData) {
        console.log(`[MockAgentService] 模拟处理，场景: ${perceptionData.semanticMap.sceneType}`);

        await new Promise(resolve => setTimeout(resolve, 500));

        let plan;
        switch (perceptionData.semanticMap.sceneType) {
            case 'crosswalk':
                plan = this.handleCrosswalk(perceptionData);
                break;
            case 'indoor':
                plan = this.handleIndoor(perceptionData);
                break;
            case 'obstacle':
                plan = this.handleObstacle(perceptionData);
                break;
            default:
                plan = this.handleDefault(perceptionData);
        }

        return new DecisionPlan(plan);
    }

    handleCrosswalk(perceptionData) {
        const hasVehicles = perceptionData.semanticMap.objects.some(
            obj => obj.label === 'car' || obj.label === 'truck' || obj.label === 'bus'
        );

        if (hasVehicles) {
            return {
                id: uuidv4(),
                timestamp: new Date().toISOString(),
                userId: perceptionData.userId,
                sessionId: perceptionData.sessionId,

                plan: {
                    primaryGoal: '安全通过路口',
                    actions: [
                        {
                            description: '请停在人行道边缘，等待车辆通过',
                            immediate: true,
                            priority: 'high',
                            hapticPattern: 'vibrate_strong'
                        }
                    ],
                    warnings: [
                        {
                            message: '注意！有车辆正在接近',
                            priority: 'high',
                            hapticPattern: 'vibrate_strong'
                        }
                    ],
                    information: [
                        {
                            message: '车辆通过后会提醒您',
                            priority: 'low'
                        }
                    ]
                },

                metadata: {
                    confidence: 0.9,
                    processingTime: 500,
                    agentVersion: 'mock-1.0',
                    emergency: perceptionData.emergency
                }
            };
        } else {
            return {
                id: uuidv4(),
                timestamp: new Date().toISOString(),
                userId: perceptionData.userId,
                sessionId: perceptionData.sessionId,

                plan: {
                    primaryGoal: '通过路口',
                    actions: [
                        {
                            description: '可以安全通过路口',
                            immediate: true,
                            priority: 'medium'
                        },
                        {
                            description: '请直行，保持当前方向',
                            immediate: false,
                            priority: 'low'
                        }
                    ],
                    warnings: [],
                    information: [
                        {
                            message: '前方是路口，请留意路面变化',
                            priority: 'low'
                        }
                    ]
                },

                metadata: {
                    confidence: 0.95,
                    processingTime: 500,
                    agentVersion: 'mock-1.0',
                    emergency: perceptionData.emergency
                }
            };
        }
    }

    handleIndoor(perceptionData) {
        const obstacles = perceptionData.semanticMap.objects.filter(
            obj => obj.distance < 2 &&
                (obj.label === 'chair' || obj.label === 'table' || obj.label === 'person')
        );

        if (obstacles.length > 0) {
            const nearest = obstacles[0];
            return {
                id: uuidv4(),
                timestamp: new Date().toISOString(),
                userId: perceptionData.userId,
                sessionId: perceptionData.sessionId,

                plan: {
                    primaryGoal: '避让障碍物',
                    actions: [
                        {
                            description: `请向${nearest.direction === 'left' ? '右' : '左'}侧避让`,
                            immediate: true,
                            priority: 'high',
                            hapticPattern: nearest.direction === 'left' ? 'vibrate_left' : 'vibrate_right'
                        }
                    ],
                    warnings: [
                        {
                            message: `前方${nearest.distance}米有${nearest.label}`,
                            priority: 'high',
                            hapticPattern: 'vibrate_strong'
                        }
                    ],
                    information: [
                        {
                            message: `已检测到${obstacles.length}个障碍物`,
                            priority: 'low'
                        }
                    ]
                },

                metadata: {
                    confidence: 0.85,
                    processingTime: 500,
                    agentVersion: 'mock-1.0',
                    emergency: perceptionData.emergency
                }
            };
        }

        return this.handleDefault(perceptionData);
    }

    handleObstacle(perceptionData) {
        return {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            userId: perceptionData.userId,
            sessionId: perceptionData.sessionId,

            plan: {
                primaryGoal: '避障',
                actions: [
                    {
                        description: '请停止前进',
                        immediate: true,
                        priority: 'high',
                        hapticPattern: 'vibrate_strong'
                    },
                    {
                        description: '检测到前方有障碍物，请向左侧绕行',
                        immediate: false,
                        priority: 'medium',
                        hapticPattern: 'vibrate_left'
                    }
                ],
                warnings: [
                    {
                        message: '前方有障碍物，距离2米',
                        priority: 'high',
                        hapticPattern: 'vibrate_strong'
                    }
                ],
                information: [
                    {
                        message: '已为您规划绕行路线',
                        priority: 'low'
                    }
                ]
            },

            metadata: {
                confidence: 0.8,
                processingTime: 500,
                agentVersion: 'mock-1.0',
                emergency: perceptionData.emergency
            }
        };
    }

    handleDefault(perceptionData) {
        return {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            userId: perceptionData.userId,
            sessionId: perceptionData.sessionId,

            plan: {
                primaryGoal: '继续前进',
                actions: [
                    {
                        description: '前方道路通畅，可以继续前进',
                        immediate: true,
                        priority: 'medium'
                    }
                ],
                warnings: [],
                information: [
                    {
                        message: `当前场景: ${perceptionData.semanticMap.sceneType || '未知'}`,
                        priority: 'low'
                    },
                    {
                        message: `检测到${perceptionData.semanticMap.objects.length}个物体`,
                        priority: 'low'
                    }
                ]
            },

            metadata: {
                confidence: 0.7,
                processingTime: 500,
                agentVersion: 'mock-1.0',
                emergency: perceptionData.emergency
            }
        };
    }
}

module.exports = new MockAgentService();