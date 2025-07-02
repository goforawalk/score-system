const { JSDOM } = require('jsdom');
const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');

global.window = dom.window;
global.document = dom.window.document;
global.jQuery = global.$ = require('jquery');

// 创建全局的 mockApi 对象
global.mockApi = {
    projects: [],
    scores: [],
    tasks: [],
    users: [
        { username: 'admin', password: 'admin123', role: 'admin' },
        { username: 'expert1', password: 'expert123', role: 'expert1' },
        { username: 'expert2', password: 'expert123', role: 'expert2' },
        { username: 'expert3', password: 'expert123', role: 'expert3' },
        { username: 'expert4', password: 'expert123', role: 'expert4' },
        { username: 'expert5', password: 'expert123', role: 'expert5' },
        { username: 'expert6', password: 'expert123', role: 'expert6' },
        { username: 'expert7', password: 'expert123', role: 'expert7' }
    ],
    // 必要的 API 方法
    calculateProjectScores: function(projectId) {
        const project = this.projects.find(p => p.id === projectId);
        if (!project) return [];

        return project.scoreItems.map((item, index) => {
            const scores = this.scores
                .filter(s => s.projectId === projectId)
                .map(s => s.scores.find(score => score.itemId === index)?.score)
                .filter(score => score !== undefined);

            const avgScore = scores.length > 0 
                ? scores.reduce((sum, score) => sum + score, 0) / scores.length 
                : 0;

            return {
                itemName: item.name,
                weight: item.weight,
                avgScore: avgScore
            };
        });
    },

    calculateAverageScore: function(projectId) {
        const details = this.calculateProjectScores(projectId);
        if (!details.length) return 0;

        const totalWeightedScore = details.reduce((sum, item) => 
            sum + (item.avgScore * item.weight), 0);
        const totalWeight = details.reduce((sum, item) => sum + item.weight, 0);

        return totalWeight > 0 ? totalWeightedScore / totalWeight : 0;
    },

    getProjectScores: function(projectId) {
        return Promise.resolve({
            success: true,
            data: {
                progress: {
                    total: this.users.filter(u => u.role.startsWith('expert')).length,
                    scored: this.scores.filter(s => s.projectId === projectId).length,
                    percentage: (this.users.filter(u => u.role.startsWith('expert')).length / 
                               this.users.filter(u => u.role.startsWith('expert')).length) * 100
                },
                details: this.calculateProjectScores(projectId),
                averageScore: this.calculateAverageScore(projectId)
            }
        });
    }
};