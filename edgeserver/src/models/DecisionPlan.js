class DecisionPlan {
    constructor(data) {
        this.id = data.id;
        this.timestamp = data.timestamp || new Date().toISOString();
        this.userId = data.userId;
        this.sessionId = data.sessionId;

        this.plan = data.plan || {
            primaryGoal: data.primaryGoal,
            actions: data.actions || [],
            warnings: data.warnings || [],
            information: data.information || [],
        };

        this.metadata = data.metadata || {
            confidence: data.confidence || 0.8,
            processingTime: data.processingTime,
            agentVersion: data.agentVersion,
            emergency: data.emergency || false,
        };
    }

    getPrimaryFeedback() {
        if (this.plan.warnings && this.plan.warnings.length > 0) {
            const highRiskWarnings = this.plan.warnings.filter(w => w.priority === 'high');
            if (highRiskWarnings.length > 0) {
                return {
                    type: 'warning',
                    content: highRiskWarnings[0].message,
                    priority: 'high'
                };
            }
        }

        if (this.plan.actions && this.plan.actions.length > 0) {
            const immediateActions = this.plan.actions.filter(a => a.immediate);
            if (immediateActions.length > 0) {
                return {
                    type: 'action',
                    content: immediateActions[0].description,
                    priority: 'medium'
                };
            }
        }

        if (this.plan.information && this.plan.information.length > 0) {
            return {
                type: 'info',
                content: this.plan.information[0].message,
                priority: 'low'
            };
        }

        return null;
    }

    toFeedback() {
        return {
            id: this.id,
            timestamp: this.timestamp,
            emergency: this.metadata.emergency,
            primary: this.getPrimaryFeedback(),
            actions: this.plan.actions.map(a => ({
                type: 'action',
                content: a.description,
                haptic: a.hapticPattern,
                priority: a.priority || 'medium'
            })),
            warnings: this.plan.warnings.map(w => ({
                type: 'warning',
                content: w.message,
                haptic: w.hapticPattern || 'vibrate_strong',
                priority: w.priority || 'high'
            })),
            info: this.plan.information.map(i => ({
                type: 'info',
                content: i.message,
                priority: i.priority || 'low'
            }))
        };
    }

    isValid() {
        return this.userId && this.sessionId &&
            (this.plan.actions.length > 0 ||
                this.plan.warnings.length > 0 ||
                this.plan.information.length > 0);
    }
}

module.exports = DecisionPlan;