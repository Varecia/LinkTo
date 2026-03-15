const dotenv = require('dotenv');
dotenv.config();

module.exports = {
    server: {
        port: process.env.PORT || 8081,
        env: process.env.NODE_ENV || 'development',
    },
    agent: {
        apiUrl: process.env.AGENT_API_URL || 'http://localhost:8082',
        // mockEnabled: process.env.MOCK_AGENT === 'true',
        mockEnabled: true,
        timeout: parseInt(process.env.AGENT_TIMEOUT) || 30000,
    },
    knowledgeGraph: {
        apiUrl: process.env.KNOWLEDGE_GRAPH_API_URL || 'http://localhost:8083',
    },
    security: {
        jwtSecret: process.env.JWT_SECRET || 'default-secret-change-me',
        apiKey: process.env.API_KEY || 'default-api-key-change-me',
    }
};