const express = require('express');
const router = express.Router();
const agentController = require('../controllers/AgentController');
const healthController = require('../controllers/HealthController');
const multer = require('multer');
const upload = multer({ limits: { fileSize: 10 * 1024 * 1024 } }); // 10MB限制

router.post('/agent/process', agentController.processPerception.bind(agentController));
router.post('/agent/process/stream', agentController.processPerceptionStream.bind(agentController));
router.get('/agent/session/:sessionId', agentController.getSessionHistory.bind(agentController));

router.get('/health', healthController.check.bind(healthController));
router.get('/health/detailed', healthController.detailed.bind(healthController));

router.get('/version', (req, res) => {
    res.json({
        success: true,
        version: '1.0.0',
        name: 'LinkTo Edge Server',
        description: '边缘服务器，负责处理感知数据并调用认知决策智能体'
    });
});

module.exports = router;