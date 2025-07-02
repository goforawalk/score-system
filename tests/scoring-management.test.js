// 测试数据
const testProject = {
    id: 1,
    name: '测试项目',
    scoreItems: [
        { name: '技术可行性', weight: 40, roles: ['expert1', 'expert2'] },
        { name: '创新性', weight: 30, roles: ['expert2', 'expert3'] },
        { name: '市场价值', weight: 30, roles: ['expert1', 'expert3'] }
    ]
};

const testScores = [
    {
        projectId: 1,
        username: 'expert1',
        scores: [
            { itemId: 0, score: 85 },
            { itemId: 2, score: 90 }
        ]
    },
    {
        projectId: 1,
        username: 'expert2',
        scores: [
            { itemId: 0, score: 82 },
            { itemId: 1, score: 88 }
        ]
    }
    // expert3 尚未提交评分
];

// 执行测试步骤
const testProgressTracking = async () => {
    // 1. 设置测试数据
    mockApi.projects = [testProject];
    mockApi.scores = testScores;
    
    // 2. 加载项目评分数据
    const response = await mockApi.getProjectScores('test-project-1');
    
    // 3. 验证进度显示
    if (response.success) {
        const progress = response.data.progress;
        expect(progress).toMatchObject({
            total: 3,        // 总专家数
            scored: 2,       // 已评分专家数
        });
        // 使用 toBeCloseTo 检查百分比
        expect(progress.percentage).toBeCloseTo(66.7, 1);
    }
};


// 测试数据
const testScoresForAverage = [
    {
        projectId: 'test-project-1',
        username: 'expert1',
        submitTime: '2024-01-15T10:00:00.000Z',
        scores: [
            { itemId: 0, score: 85 }, // 权重40%的评分项
            { itemId: 2, score: 88 }  // 权重30%的评分项
        ]
    },
    {
        projectId: 'test-project-1',
        username: 'expert2',
        submitTime: '2024-01-15T11:00:00.000Z',
        scores: [
            { itemId: 0, score: 82 }, // 权重40%的评分项
            { itemId: 1, score: 90 }  // 权重30%的评分项
        ]
    },
    {
        projectId: 'test-project-1',
        username: 'expert3',
        submitTime: '2024-01-15T12:00:00.000Z',
        scores: [
            { itemId: 1, score: 86 }, // 权重30%的评分项
            { itemId: 2, score: 84 }  // 权重30%的评分项
        ]
    }
];

// 执行测试步骤
const testAverageCalculation = async () => {
    // 设置测试数据
    mockApi.projects = [testProject];
    mockApi.scores = testScoresForAverage;
    
    // 加载项目评分数据
    const response = await mockApi.getProjectScores('test-project-1');
    
    if (response.success) {
        const stats = response.data;
        
        // 验证平均分计算
        expect(stats.averageScore).toBeDefined();
        expect(typeof stats.averageScore).toBe('number');
        
        console.log('项目评分详情：', stats.details);
        console.log('项目平均分：', stats.averageScore);
        
        // 验证进度信息
        expect(stats.progress).toMatchObject({
            total: 3,
            scored: 3,
            percentage: 100
        });
    }
};

const { describe, test, expect, beforeEach } = require('@jest/globals');

// 确保测试前已经加载了 setup.js
require('./setup.js');

describe('评分管理模块测试', () => {
    beforeEach(() => {
        // 使用已定义的全局 mockApi
        global.mockApi.projects = [];
        global.mockApi.scores = [];
    });

    test('评分进度追踪', async () => {
        await testProgressTracking();
    });

    test('平均分计算', async () => {
        await testAverageCalculation();
        console.log('计算结果：', await mockApi.getProjectScores('test-project-1'));
    });
});

console.log('测试项目数据：', testProject);
console.log('测试评分数据：', testScoresForAverage);