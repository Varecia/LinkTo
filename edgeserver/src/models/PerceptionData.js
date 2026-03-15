class PerceptionData {
    constructor(data) {
        this.timestamp = data.timestamp || new Date().toISOString();
        this.userId = data.userId;
        this.sessionId = data.sessionId;

        this.semanticMap = data.semanticMap || {
            location: data.location,
            objects: data.objects || [],
            sceneType: data.sceneType,
            hazards: data.hazards || [],
        };

        this.userContext = data.userContext || {
            action: data.userAction,
            heading: data.heading,
            speed: data.speed,
            command: data.userCommand,
        };

        this.environmentContext = data.environmentContext || {
            lighting: data.lighting,
            weather: data.weather,
            timeOfDay: data.timeOfDay,
        };

        this.emergency = data.emergency || false;
    }

    toPrompt() {
        const objects = this.semanticMap.objects
            .map(obj => `${obj.label}（距离${obj.distance}米，${obj.direction}）`)
            .join('，');

        const hazards = this.semanticMap.hazards
            .map(h => `${h.type}（危险等级:${h.level}）`)
            .join('，');

        let prompt = `当前场景：${this.semanticMap.sceneType || '未知'}。`;
        prompt += `用户正在${this.userContext.action || '未知动作'}。`;

        if (objects) {
            prompt += `周围有：${objects}。`;
        }

        if (hazards) {
            prompt += `⚠️ 检测到危险：${hazards}。`;
        }

        if (this.userContext.command) {
            prompt += `用户指令：${this.userContext.command}。`;
        }

        if (this.emergency) {
            prompt = '【紧急情况】' + prompt;
        }

        return prompt;
    }

    isValid() {
        return this.userId && this.sessionId && this.timestamp;
    }
}

module.exports = PerceptionData;