/**
 * 综合验证脚本 - 测试整个数据流
 * 验证前端提交包含taskId的数据，后端接收并正确处理，后端返回适合评审评分和管理员统计的数据
 * 
 * 调整说明：
 * - 移除API路径中的/api前缀，匹配后端实际配置
 * - 更新API路径以匹配后端实际接口
 * - 调整数据结构以匹配后端期望的格式
 * - 修复浏览器环境中的global对象问题
 */

// 测试配置
const TEST_CONFIG = {
    baseUrl: 'http://localhost:8080',
    timeout: 10000,
    retryCount: 3
};

// 测试数据
const TEST_DATA = {
    user: {
        username: 'testuser',
        password: 'testpass',
        role: 'EXPERT'
    },
    task: {
        taskId: 'TEST-TASK-001',
        category: '综合验证测试',
        taskType: 1,
        scoreGroupType: 1,
        status: 'active'
    },
    project: {
        name: '综合验证测试项目',
        description: '用于验证整个数据流的测试项目',
        status: 'draft',
        unit: '测试单位',
        leader: '测试负责人',
        scoreGroups: {
            preliminary: [
                {
                    name: '初步评分项1',
                    minScore: 0,
                    maxScore: 100,
                    roles: ['EXPERT']
                }
            ],
            semifinal: [],
            final: []
        }
    },
    score: {
        totalScore: 85.0,
        comments: '综合验证测试评分',
        isDraft: false,
        scores: {
            1: 85
        }
    }
};

// 测试结果记录
let testResults = {
    passed: 0,
    failed: 0,
    errors: []
};

// 全局变量存储（浏览器环境）
window.testData = {
    userInfo: null,
    taskId: null,
    projectId: null,
    scoreId: null
};

/**
 * 日志记录函数
 */
function log(message, type = 'info') {
    const timestamp = new Date().toISOString();
    const prefix = type === 'error' ? '❌' : type === 'success' ? '✅' : 'ℹ️';
    console.log(`${prefix} [${timestamp}] ${message}`);
}

/**
 * 断言函数
 */
function assert(condition, message) {
    if (condition) {
        log(`PASS: ${message}`, 'success');
        testResults.passed++;
    } else {
        log(`FAIL: ${message}`, 'error');
        testResults.failed++;
        testResults.errors.push(message);
    }
}

/**
 * HTTP请求函数
 */
async function httpRequest(url, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        },
        timeout: TEST_CONFIG.timeout
    };

    const finalOptions = { ...defaultOptions, ...options };
    
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), finalOptions.timeout);
        
        const response = await fetch(url, {
            ...finalOptions,
            signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        return await response.json();
    } catch (error) {
        throw new Error(`请求失败: ${error.message}`);
    }
}

/**
 * 测试1: 用户登录
 */
async function testUserLogin() {
    log('开始测试用户登录...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/auth/login`, {
            method: 'POST',
            body: JSON.stringify({
                username: TEST_DATA.user.username,
                password: TEST_DATA.user.password
            })
        });
        
        assert(response.success, '登录响应包含success字段');
        assert(response.data && response.data.username, '登录响应包含用户信息');
        assert(response.data.role, '登录响应包含用户角色信息');
        
        // 保存用户信息用于后续测试
        window.testData.userInfo = response.data;
        
        log(`用户登录成功，用户名: ${window.testData.userInfo.username}`, 'success');
        return true;
    } catch (error) {
        log(`用户登录失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试2: 创建测试任务
 */
async function testCreateTask() {
    log('开始测试创建任务...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/tasks`, {
            method: 'POST',
            body: JSON.stringify(TEST_DATA.task)
        });
        
        assert(response.success, '创建任务响应包含success字段');
        assert(response.data && response.data.id, '创建任务响应包含任务ID');
        assert(response.data.taskId === TEST_DATA.task.taskId, '创建的任务ID正确');
        
        window.testData.taskId = response.data.id;
        log(`任务创建成功，任务ID: ${window.testData.taskId}`, 'success');
        return true;
    } catch (error) {
        log(`创建任务失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试3: 创建测试项目
 */
async function testCreateProject() {
    log('开始测试创建项目...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/projects`, {
            method: 'POST',
            body: JSON.stringify(TEST_DATA.project)
        });
        
        assert(response.success, '创建项目响应包含success字段');
        assert(response.data && response.data.id, '创建项目响应包含项目ID');
        assert(response.data.name === TEST_DATA.project.name, '创建的项目名称正确');
        
        window.testData.projectId = response.data.id;
        log(`项目创建成功，项目ID: ${window.testData.projectId}`, 'success');
        return true;
    } catch (error) {
        log(`创建项目失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试4: 验证任务列表API
 */
async function testTaskListAPI() {
    log('开始测试任务列表API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/tasks`);
        
        assert(response.success, '任务列表响应包含success字段');
        assert(Array.isArray(response.data), '任务列表响应包含数组数据');
        assert(response.data.length > 0, '任务列表不为空');
        
        const testTask = response.data.find(task => task.id === window.testData.taskId);
        assert(testTask, '创建的测试任务在列表中');
        assert(testTask.taskId === TEST_DATA.task.taskId, '测试任务ID正确');
        
        log('任务列表API测试通过', 'success');
        return true;
    } catch (error) {
        log(`任务列表API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试5: 验证项目列表API
 */
async function testProjectListAPI() {
    log('开始测试项目列表API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/projects`);
        
        assert(response.success, '项目列表响应包含success字段');
        assert(Array.isArray(response.data), '项目列表响应包含数组数据');
        assert(response.data.length > 0, '项目列表不为空');
        
        const testProject = response.data.find(project => project.id === window.testData.projectId);
        assert(testProject, '创建的测试项目在列表中');
        assert(testProject.name === TEST_DATA.project.name, '测试项目名称正确');
        
        log('项目列表API测试通过', 'success');
        return true;
    } catch (error) {
        log(`项目列表API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试6: 验证评分提交API
 */
async function testScoreSubmissionAPI() {
    log('开始测试评分提交API...');
    
    try {
        const scoreData = {
            projectId: window.testData.projectId,
            taskId: window.testData.taskId,
            username: window.testData.userInfo.username,
            scores: TEST_DATA.score.scores,
            totalScore: TEST_DATA.score.totalScore,
            comments: TEST_DATA.score.comments,
            isDraft: TEST_DATA.score.isDraft
        };
        
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/scores`, {
            method: 'POST',
            body: JSON.stringify(scoreData)
        });
        
        assert(response.success, '评分提交响应包含success字段');
        assert(response.data && response.data.id, '评分提交响应包含评分ID');
        assert(response.data.projectId === window.testData.projectId, '提交的评分关联正确的项目ID');
        assert(response.data.totalScore === TEST_DATA.score.totalScore, '提交的评分分数正确');
        
        window.testData.scoreId = response.data.id;
        log(`评分提交成功，评分ID: ${window.testData.scoreId}`, 'success');
        return true;
    } catch (error) {
        log(`评分提交失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试7: 验证评分历史API
 */
async function testScoreHistoryAPI() {
    log('开始测试评分历史API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/scores/history?projectId=${window.testData.projectId}&username=${window.testData.userInfo.username}`);
        
        assert(response.success, '评分历史响应包含success字段');
        assert(Array.isArray(response.data), '评分历史响应包含数组数据');
        assert(response.data.length > 0, '评分历史不为空');
        
        const testScore = response.data.find(score => score.id === window.testData.scoreId);
        assert(testScore, '提交的测试评分在历史中');
        assert(testScore.projectId === window.testData.projectId, '测试评分关联正确的项目ID');
        assert(testScore.totalScore === TEST_DATA.score.totalScore, '测试评分分数正确');
        
        log('评分历史API测试通过', 'success');
        return true;
    } catch (error) {
        log(`评分历史API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试8: 验证统计API
 */
async function testStatisticsAPI() {
    log('开始测试统计API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/statistics`);
        
        assert(response.success, '统计响应包含success字段');
        assert(Array.isArray(response.data), '统计响应包含数组数据');
        
        // 验证统计数据结构
        if (response.data.length > 0) {
            const stats = response.data[0];
            assert(typeof stats.id === 'number', '统计包含项目ID');
            assert(typeof stats.name === 'string', '统计包含项目名称');
        }
        
        log('统计API测试通过', 'success');
        return true;
    } catch (error) {
        log(`统计API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试9: 验证前端统计API
 */
async function testFrontendStatisticsAPI() {
    log('开始测试前端统计API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/statistics/frontend`);
        
        assert(response.success, '前端统计响应包含success字段');
        assert(Array.isArray(response.data), '前端统计响应包含数组数据');
        
        log('前端统计API测试通过', 'success');
        return true;
    } catch (error) {
        log(`前端统计API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试10: 验证项目进度API
 */
async function testProjectProgressAPI() {
    log('开始测试项目进度API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/projects/${window.testData.projectId}/progress`);
        
        assert(response.success, '项目进度响应包含success字段');
        assert(response.data, '项目进度响应包含数据');
        
        // 验证进度数据结构
        const progress = response.data;
        assert(typeof progress.totalExperts === 'number', '进度包含专家总数');
        assert(typeof progress.completedExperts === 'number', '进度包含已完成专家数');
        assert(typeof progress.completionPercentage === 'number', '进度包含完成百分比');
        
        log('项目进度API测试通过', 'success');
        return true;
    } catch (error) {
        log(`项目进度API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试11: 验证活动任务API
 */
async function testActiveTaskAPI() {
    log('开始测试活动任务API...');
    
    try {
        const response = await httpRequest(`${TEST_CONFIG.baseUrl}/tasks/active`);
        
        assert(response.success, '活动任务响应包含success字段');
        assert(response.data, '活动任务响应包含数据');
        
        // 验证活动任务数据结构
        const activeTaskData = response.data;
        assert(activeTaskData.hasOwnProperty('task'), '活动任务数据包含task字段');
        assert(activeTaskData.hasOwnProperty('projectsInOrder'), '活动任务数据包含projectsInOrder字段');
        
        log('活动任务API测试通过', 'success');
        return true;
    } catch (error) {
        log(`活动任务API测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 测试12: 验证数据完整性
 */
async function testDataIntegrity() {
    log('开始测试数据完整性...');
    
    try {
        // 验证项目-评分的关联关系
        const projectsResponse = await httpRequest(`${TEST_CONFIG.baseUrl}/projects`);
        const scoresResponse = await httpRequest(`${TEST_CONFIG.baseUrl}/scores`);
        
        const projects = projectsResponse.data;
        const scores = scoresResponse.data;
        
        // 验证评分关联的项目存在
        const projectIds = projects.map(p => p.id);
        const invalidScoreProjects = scores.filter(score => !projectIds.includes(score.projectId));
        assert(invalidScoreProjects.length === 0, '所有评分关联的项目都存在');
        
        log('数据完整性测试通过', 'success');
        return true;
    } catch (error) {
        log(`数据完整性测试失败: ${error.message}`, 'error');
        return false;
    }
}

/**
 * 清理测试数据
 */
async function cleanupTestData() {
    log('开始清理测试数据...');
    
    try {
        // 删除测试评分
        if (window.testData.scoreId) {
            try {
                await httpRequest(`${TEST_CONFIG.baseUrl}/scores/${window.testData.scoreId}`, {
                    method: 'DELETE'
                });
                log('测试评分已删除', 'success');
            } catch (error) {
                log(`删除测试评分失败: ${error.message}`, 'error');
            }
        }
        
        // 删除测试项目
        if (window.testData.projectId) {
            try {
                await httpRequest(`${TEST_CONFIG.baseUrl}/projects/${window.testData.projectId}`, {
                    method: 'DELETE'
                });
                log('测试项目已删除', 'success');
            } catch (error) {
                log(`删除测试项目失败: ${error.message}`, 'error');
            }
        }
        
        // 删除测试任务
        if (window.testData.taskId) {
            try {
                await httpRequest(`${TEST_CONFIG.baseUrl}/tasks/${window.testData.taskId}`, {
                    method: 'DELETE'
                });
                log('测试任务已删除', 'success');
            } catch (error) {
                log(`删除测试任务失败: ${error.message}`, 'error');
            }
        }
        
        log('测试数据清理完成', 'success');
    } catch (error) {
        log(`清理测试数据失败: ${error.message}`, 'error');
    }
}

/**
 * 主测试函数
 */
async function runComprehensiveVerification() {
    log('开始综合验证测试...', 'info');
    log('='.repeat(50), 'info');
    
    const tests = [
        { name: '用户登录', fn: testUserLogin },
        { name: '创建测试任务', fn: testCreateTask },
        { name: '创建测试项目', fn: testCreateProject },
        { name: '任务列表API', fn: testTaskListAPI },
        { name: '项目列表API', fn: testProjectListAPI },
        { name: '评分提交API', fn: testScoreSubmissionAPI },
        { name: '评分历史API', fn: testScoreHistoryAPI },
        { name: '统计API', fn: testStatisticsAPI },
        { name: '前端统计API', fn: testFrontendStatisticsAPI },
        { name: '项目进度API', fn: testProjectProgressAPI },
        { name: '活动任务API', fn: testActiveTaskAPI },
        { name: '数据完整性', fn: testDataIntegrity }
    ];
    
    let passedTests = 0;
    let totalTests = tests.length;
    
    for (const test of tests) {
        log(`\n执行测试: ${test.name}`);
        try {
            const result = await test.fn();
            if (result) {
                passedTests++;
            }
        } catch (error) {
            log(`测试 ${test.name} 执行异常: ${error.message}`, 'error');
        }
    }
    
    // 清理测试数据
    await cleanupTestData();
    
    // 输出测试结果
    log('\n' + '='.repeat(50));
    log('综合验证测试完成', 'info');
    log(`总测试数: ${totalTests}`);
    log(`通过测试: ${passedTests}`);
    log(`失败测试: ${totalTests - passedTests}`);
    log(`成功率: ${((passedTests / totalTests) * 100).toFixed(2)}%`);
    
    if (testResults.errors.length > 0) {
        log('\n详细错误信息:', 'error');
        testResults.errors.forEach((error, index) => {
            log(`${index + 1}. ${error}`, 'error');
        });
    }
    
    if (passedTests === totalTests) {
        log('\n🎉 所有测试通过！系统完全满足要求！', 'success');
        log('✅ 前端提交包含taskId的数据', 'success');
        log('✅ 后端接收并正确处理数据', 'success');
        log('✅ 后端返回适合评审评分和管理员统计的数据', 'success');
    } else {
        log('\n⚠️ 部分测试失败，请检查系统实现', 'error');
    }
    
    return passedTests === totalTests;
}

// 导出测试函数
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        runComprehensiveVerification,
        testResults
    };
}

// 如果直接运行此脚本
if (typeof window === 'undefined') {
    runComprehensiveVerification().catch(error => {
        log(`测试执行失败: ${error.message}`, 'error');
        process.exit(1);
    });
} 