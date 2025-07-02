const mockApi = {
    // 初始化测试用户数据
    users: [
        { username: 'admin', name: '管理员', password: 'admin123', role: 'admin' },
        { username: 'expert1', name: '评审专家1', password: 'expert123', role: 'expert1' },
        { username: 'expert2', name: '评审专家2', password: 'expert123', role: 'expert2' },
        { username: 'expert3', name: '评审专家3', password: 'expert123', role: 'expert3' },
        { username: 'expert4', name: '评审专家4', password: 'expert123', role: 'expert4' },
        { username: 'expert5', name: '评审专家5', password: 'expert123', role: 'expert5' },
        { username: 'expert6', name: '评审专家6', password: 'expert123', role: 'expert6' },
        { username: 'expert7', name: '评审专家7', password: 'expert123', role: 'expert7' }
    ],

    // 数据持久化相关方法
    saveUsersToStorage: function() {
        if (typeof window !== 'undefined' && window.localStorage) {
            try {
                window.localStorage.setItem('mockUsers', JSON.stringify(this.users));
            } catch (e) {
                console.warn('保存用户数据到localStorage失败:', e);
            }
        }
    },

    loadUsersFromStorage: function() {
        if (typeof window !== 'undefined' && window.localStorage) {
            try {
                const storedUsers = window.localStorage.getItem('mockUsers');
                if (storedUsers) {
                    const parsedUsers = JSON.parse(storedUsers);
                    // 确保至少包含初始用户数据
                    const initialUsernames = ['admin', 'expert1', 'expert2', 'expert3', 'expert4', 'expert5', 'expert6', 'expert7'];
                    const hasAllInitialUsers = initialUsernames.every(username => 
                        parsedUsers.some(user => user.username === username)
                    );
                    
                    if (hasAllInitialUsers) {
                        this.users = parsedUsers;
                    } else {
                        // 如果缺少初始用户，重新初始化
                        this.saveUsersToStorage();
                    }
                } else {
                    // 首次使用，保存初始数据
                    this.saveUsersToStorage();
                }
            } catch (e) {
                console.warn('从localStorage加载用户数据失败:', e);
                this.saveUsersToStorage();
            }
        }
    },

    // 初始化时加载数据
    init: function() {
        this.loadUsersFromStorage();
    },

    // 扩充测试项目数据
    projects: [
        {
            id: 1,
            name: "智能制造创新平台",
            description: "基于大数据和人工智能技术的智能制造综合创新平台，支持工业4.0和数字化转型",
            unit: "北京智能技术研究院",
            leader: "张三",
            status: "active",
            createTime: "2024-01-15T08:00:00.000Z",
            scoreGroups: {
                preliminary: [
                    {
                        name: "技术可行性",
                        minScore: 1,
                        maxScore: 10,
                        roles: ["expert1", "expert2"]
                    },
                    {
                        name: "创新性",
                        minScore: 10,
                        maxScore: 50,
                        roles: ["expert2", "expert3"]
                    },
                    {
                        name: "应用价值",
                        minScore: 20,
                        maxScore: 80,
                        roles: ["expert1", "expert3"]
                    },
                    {
                        name: "市场前景",
                        minScore: 30,
                        maxScore: 90,
                        roles: ["expert1", "expert4"]
                    },
                    {
                        name: "技术成熟度",
                        minScore: 40,
                        maxScore: 100,
                        roles: ["expert3", "expert5"]
                    },
                    {
                        name: "投资价值",
                        minScore: 50,
                        maxScore: 150,
                        roles: ["expert2", "expert6"]
                    },
                    {
                        name: "风险评估",
                        minScore: 0,
                        maxScore: 50,
                        roles: ["expert4", "expert7"]
                    }
                ],
                semifinal: [
                    {
                        name: "实施效果",
                        minScore: 60,
                        maxScore: 90,
                        roles: ["expert1", "expert2"]
                    }
                ],
                final: []
            }
        },
        {
            id: 2,
            name: "绿色能源技术应用",
            description: "新一代太阳能和风能高效利用技术研究项目，旨在提高可再生能源的利用效率和经济性",
            unit: "上海新能源研究所",
            leader: "李四",
            status: "active",
            createTime: "2024-01-16T09:00:00.000Z",
            scoreGroups: {
                preliminary: [
                    {
                        name: "技术先进性",
                        minScore: 5,
                        maxScore: 25,
                        roles: ["expert1", "expert2", "expert3"]
                    },
                    {
                        name: "环保效益",
                        minScore: 10,
                        maxScore: 30,
                        roles: ["expert3"]
                    },
                    {
                        name: "经济可行性",
                        minScore: 15,
                        maxScore: 45,
                        roles: ["expert1", "expert2", "expert3"]
                    }
                ],
                semifinal: [
                    {
                        name: "实施效果",
                        minScore: 15,
                        maxScore: 75,
                        roles: ["expert1", "expert2", "expert3"]
                    }
                ],
                final: [
                    {
                        name: "推广价值",
                        minScore: 20,
                        maxScore: 200,
                        roles: ["expert1", "expert3"]
                    }
                ]
            }
        },
        {
            id: 3,
            name: "综合评审项目",
            description: "多领域交叉创新项目，包含技术创新、市场应用和商业模式三大评审维度的综合评估项目",
            unit: "综合评审委员会",
            leader: "王五",
            status: "active",
            createTime: "2024-01-17T10:00:00.000Z",
            scoreGroups: {
                preliminary: [
                    {
                        name: "技术评估",
                        minScore: 10,
                        maxScore: 40,
                        roles: ["expert1", "expert2", "expert3"]
                    },
                    {
                        name: "市场分析",
                        minScore: 15,
                        maxScore: 60,
                        roles: ["expert2", "expert4", "expert6"]
                    },
                    {
                        name: "财务评估",
                        minScore: 25,
                        maxScore: 75,
                        roles: ["expert1", "expert5", "expert7"]
                    },
                    {
                        name: "风险评估",
                        minScore: -10,
                        maxScore: 10,
                        roles: ["expert3", "expert4", "expert5"]
                    },
                    {
                        name: "创新性评估",
                        minScore: 0,
                        maxScore: 50,
                        roles: ["expert1", "expert6", "expert7"]
                    },
                    {
                        name: "可行性分析",
                        minScore: 30,
                        maxScore: 120,
                        roles: ["expert2", "expert3", "expert6"]
                    },
                    {
                        name: "投资建议",
                        minScore: 1,
                        maxScore: 5,
                        roles: ["expert4", "expert5", "expert7"]
                    }
                ],
                semifinal: [],
                final: []
            }
        }
    ],
    
    // 扩充评分记录数据
    scores: [
        // 清空评分记录，以便测试TC303
    ],

    // 扩充评审任务数据
    tasks: [
        // 更新任务配置，使用项目1和项目3，以及preliminary评分组
        {
            id: "task-002",
            category: "2024年技术创新专项",
            projectIdsInOrder: [1, 3],
            scoreGroupType: "preliminary", // 使用初赛评分项，符合TC303测试用例
            taskType: 2, // 类型2：个人完成后可进入下一项目
            status: "inactive", // 设为非活动状态，使task-001成为当前活动任务
            expertIds: ["expert1", "expert2", "expert3", "expert4", "expert5", "expert6", "expert7"],
            createdAt: "2024-01-16T09:00:00.000Z"
        },
        // 添加类型1任务，用于测试TC306：同步评审模式验证
        {
            id: "task-001",
            category: "2024年同步评审测试",
            projectIdsInOrder: [2, 1],
            scoreGroupType: "preliminary", // 使用初赛评分项
            taskType: 1, // 类型1：全部专家完成后进入下一项目（同步评审模式）
            status: "active", // 设为活动状态
            expertIds: ["expert1", "expert2", "expert3"],
            createdAt: "2024-01-15T08:00:00.000Z"
        }
    ],
    
    login: function(username, password) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const user = this.users.find(u => 
                    u.username === username && u.password === password
                );
                
                if (user) {
                    console.log('模拟API登录成功:', user.username);
                    resolve({
                        success: true,
                        message: '登录成功',
                        data: {
                            username: user.username,
                            name: user.name || user.username,
                            role: user.role,
                            token: 'mock-jwt-token-' + Date.now(),
                            createTime: new Date(),
                            updateTime: new Date(),
                            lastLoginTime: new Date()
                        }
                    });
                } else {
                    console.log('模拟API登录失败: 用户名或密码错误');
                    reject({
                        success: false,
                        message: '用户名或密码错误'
                    });
                }
            }, 500);
        });
    },

    // 获取项目统计数据
    getProjectStats: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    success: true,
                    data: {
                        totalProjects: this.projects.length,
                        activeProjects: this.projects.filter(p => p.status === 'active').length,
                        totalExperts: this.users.filter(u => u.role.startsWith('expert')).length
                    }
                });
            }, 300);
        });
    },
    
    // 获取项目列表
    getProjects: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    success: true,
                    data: this.projects
                });
            }, 300);
        });
    },
    
    // 添加项目
    addProject: function(project) {
        return new Promise((resolve) => {
            setTimeout(() => {
                // 处理新的scoreGroups数据结构
                if (project.scoreGroups) {
                    // 确保每个评分组中的评分项都有角色配置
                    Object.keys(project.scoreGroups).forEach(groupKey => {
                        if (Array.isArray(project.scoreGroups[groupKey])) {
                            project.scoreGroups[groupKey] = project.scoreGroups[groupKey].map(item => ({
                                ...item,
                                roles: item.roles || ['expert1', 'expert2', 'expert3', 'expert4', 'expert5', 'expert6', 'expert7'] // 默认所有专家都可评分
                            }));
                        }
                    });
                } else if (project.scoreItems) {
                    // 兼容旧的scoreItems数据结构
                project.scoreItems = project.scoreItems.map(item => ({
                    ...item,
                        roles: item.roles || ['expert1', 'expert2', 'expert3', 'expert4', 'expert5', 'expert6', 'expert7'] // 默认所有专家都可评分
                }));
                }
                
                project.id = Date.now();
                project.createTime = new Date().toISOString();
                project.status = 'draft';
                this.projects.push(project);
                resolve({
                    success: true,
                    data: project
                });
            }, 300);
        });
    },
    
    // 更新项目
    updateProject: function(projectId, projectData) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const index = this.projects.findIndex(p => p.id === projectId);
                if (index === -1) {
                    reject({
                        success: false,
                        message: '项目不存在'
                    });
                    return;
                }
                
                this.projects[index] = {
                    ...this.projects[index],
                    ...projectData,
                    updateTime: new Date().toISOString()
                };
                
                resolve({
                    success: true,
                    data: this.projects[index]
                });
            }, 300);
        });
    },
    
    // 删除项目
    deleteProject: function(projectId) {
        return new Promise((resolve) => {
            setTimeout(() => {
                this.projects = this.projects.filter(p => p.id !== projectId);
                resolve({
                    success: true
                });
            }, 300);
        });
    },
    
    // 启用/禁用项目
    toggleProjectStatus: function(projectId, status) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const project = this.projects.find(p => p.id === projectId);
                if (!project) {
                    reject({
                        success: false,
                        message: '项目不存在'
                    });
                    return;
                }
                
                project.status = status;
                project.updateTime = new Date().toISOString();
                
                resolve({
                    success: true,
                    data: project
                });
            }, 300);
        });
    },
    
    // 获取用户列表
    getUsers: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    success: true,
                    data: this.users
                });
            }, 300);
        });
    },
    
    // 添加用户
    addUser: function(userData) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (this.users.some(u => u.username === userData.username)) {
                    reject({
                        success: false,
                        message: '用户名已存在'
                    });
                    return;
                }
                
                const newUser = {
                    ...userData,
                    createTime: new Date().toISOString()
                };
                this.users.push(newUser);
                
                // 保存到localStorage
                this.saveUsersToStorage();
                
                resolve({
                    success: true,
                    data: {
                        ...newUser,
                        password: undefined
                    }
                });
            }, 300);
        });
    },
    
    // 更新用户
    updateUser: function(username, userData) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const index = this.users.findIndex(u => u.username === username);
                if (index === -1) {
                    reject({
                        success: false,
                        message: '用户不存在'
                    });
                    return;
                }
                
                this.users[index] = {
                    ...this.users[index],
                    ...userData
                };
                
                // 保存到localStorage
                this.saveUsersToStorage();
                
                logUserAction('admin', 'update_user', {
                    target: username,
                    changes: userData
                });
                
                resolve({
                    success: true,
                    data: {
                        ...this.users[index],
                        password: undefined
                    }
                });
            }, 300);
        });
    },
    
    // 删除用户
    deleteUser: function(username) {
        return new Promise((resolve) => {
            setTimeout(() => {
                this.users = this.users.filter(u => u.username !== username);
                
                // 保存到localStorage
                this.saveUsersToStorage();
                
                resolve({
                    success: true
                });
            }, 300);
        });
    },

    // 获取专家角色列表
    getExpertRoles: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                // 返回固定的专家角色列表
                const experts = ['expert1', 'expert2', 'expert3', 'expert4', 'expert5', 'expert6', 'expert7'];
                resolve({
                    success: true,
                    data: experts
                });
            }, 300);
        });
    },
    
    // 获取当前需要评分的项目
    getCurrentProject: function(username) {
        return new Promise((resolve) => {
            setTimeout(() => {
                const activeProjects = this.projects.filter(p => p.status === 'active');
                if (activeProjects.length === 0) {
                    resolve({
                        success: true,
                        data: null
                    });
                    return;
                }

                // 获取用户未评分的第一个项目
                const project = activeProjects.find(p => 
                    !this.scores.some(s => 
                        s.projectId === p.id && s.username === username
                    )
                );

                resolve({
                    success: true,
                    data: project
                });
            }, 300);
        });
    },
    
    // 提交评分
    submitScore: function(scoreData) {
        return new Promise((resolve) => {
            setTimeout(() => {
                // 确保projectId是数字类型
                const projectId = typeof scoreData.projectId === 'string' ? parseInt(scoreData.projectId) : scoreData.projectId;
                
                // 创建标准化的评分数据
                const normalizedScoreData = {
                    ...scoreData,
                    projectId: projectId
                };
                
                // 检查是否已存在相同的评分记录
                const existingIndex = this.scores.findIndex(s => 
                    s.projectId === projectId && s.username === normalizedScoreData.username
                );
                
                // 添加提交时间
                const scoreWithTime = {
                    ...normalizedScoreData,
                    submitTime: new Date().toISOString()
                };
                
                console.log('接收到评分数据:', scoreWithTime);
                
                if (existingIndex >= 0) {
                    // 更新已有记录
                    this.scores[existingIndex] = scoreWithTime;
                    console.log(`更新评分记录: 项目ID=${projectId}, 用户=${normalizedScoreData.username}`);
                } else {
                    // 添加新记录
                    this.scores.push(scoreWithTime);
                    console.log(`添加评分记录: 项目ID=${projectId}, 用户=${normalizedScoreData.username}`);
                }
                
                console.log('API中所有评分记录:', this.scores);
                
                resolve({
                    success: true,
                    data: scoreWithTime
                });
            }, 300);
        });
    },
    
    // 获取项目评分进度
    getProjectScoringProgress: function(projectId) {
        return new Promise((resolve) => {
            setTimeout(() => {
                // 获取项目信息
                const project = this.projects.find(p => p.id === projectId);
                if (!project) {
                    console.error(`项目${projectId}不存在`);
                    resolve({
                        success: false,
                        message: '项目不存在'
                    });
                    return;
                }

                // 获取当前活动任务，确定评分阶段和参与专家
                const activeTask = this.tasks.find(t => t.status === 'active');
                if (!activeTask) {
                    console.error('没有活动中的任务');
                    resolve({
                        success: false,
                        message: '没有活动中的任务'
                    });
                    return;
                }
                
                // 打印当前所有评分记录，便于调试
                console.log('当前API中所有评分记录:', JSON.stringify(this.scores));
                
                const scoreGroupType = activeTask.scoreGroupType || 'preliminary';
                const taskExperts = activeTask.expertIds || [];
                
                // 获取该项目的所有评分记录
                const projectScores = this.scores.filter(s => s.projectId === projectId);
                console.log(`项目${projectId}的评分记录:`, JSON.stringify(projectScores));
                
                // 计算已完成评分的专家
                const scoredExperts = [];
                projectScores.forEach(score => {
                    if (!scoredExperts.includes(score.username)) {
                        scoredExperts.push(score.username);
                }
                });
                
                // 判断评分是否完成的标志：所有参与任务的专家都已完成评分
                const completed = taskExperts.length > 0 && 
                                  taskExperts.every(expert => scoredExperts.includes(expert));
                
                // 详细日志记录评分进度
                console.log(`项目${projectId}评分进度详情:`, {
                    项目名称: project.name,
                    任务ID: activeTask.id,
                    任务类型: activeTask.taskType,
                    评分阶段: scoreGroupType,
                    参与专家: taskExperts,
                    已评分专家: scoredExperts,
                    已评分数量: scoredExperts.length,
                    总专家数量: taskExperts.length,
                    是否已完成: completed ? '是' : '否'
                });

                resolve({
                    success: true,
                    data: {
                        projectId: projectId,
                        projectName: project.name,
                        scored: scoredExperts.length,
                        total: taskExperts.length,
                        completed: completed,
                        scoredExperts: scoredExperts
                    }
                });
            }, 300);
        });
    },
    
    // 获取项目评分详情
    getProjectScores: function(projectId) {
        return new Promise((resolve) => {
            setTimeout(() => {
                // 转换 projectId 为数字类型，因为 select 值可能是字符串
                const id = parseInt(projectId);
                const project = this.projects.find(p => p.id === id);
                
                if (!project) {
                    resolve({
                        success: false,
                        message: `项目ID ${projectId} 不存在`
                    });
                    return;
                }

                // 获取项目的评分记录
                const scores = this.scores.filter(s => s.projectId === id);
                
                // 计算总评分项数 - 支持新的scoreGroups结构
                let totalItems = 0;
                let scoreItems = [];
                
                if (project.scoreGroups) {
                    Object.values(project.scoreGroups).forEach(group => {
                        if (Array.isArray(group)) {
                            group.forEach(item => {
                                totalItems += (item.roles && item.roles.length) || 0;
                                scoreItems.push(item);
                            });
                        }
                    });
                } else if (project.scoreItems) {
                    // 兼容旧的scoreItems结构
                    totalItems = project.scoreItems.reduce((acc, item) => acc + (item.roles && item.roles.length || 0), 0);
                    scoreItems = project.scoreItems;
                }
                
                resolve({
                    success: true,
                    data: {
                        projectId: id,
                        projectName: project.name,
                        statistics: {
                            averageScore: this.calculateAverageScore(id)
                        },
                        progress: {
                            total: totalItems,
                            scored: scores.length
                        },
                        details: scores,
                        scoreItems: scoreItems
                    }
                });
            }, 300);
        });
    },
    
    // 修改计算项目平均分方法
    calculateAverageScore: function(projectId) {
        const project = this.projects.find(p => p.id === projectId);
    const scores = this.scores.filter(s => s.projectId === projectId);
    
    if (!project || !scores.length) {
        return 0;
    }

    // 计算所有评分项的得分总和
    let totalScore = 0;
    scores.forEach(score => {
        totalScore += score.scores.reduce((sum, item) => sum + item.score, 0);
    });
    
    return totalScore;
    },
    
    // 获取统计数据
    getStatistics: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const statistics = this.projects.map(project => {
                    const scores = this.scores.filter(s => s.projectId === project.id);
                    const expertsCount = this.users.filter(u => u.role.startsWith('expert')).length;
                    const scoredCount = new Set(scores.map(s => s.username)).size;
                    
                    // 计算各评分项的统计数据 - 支持新的scoreGroups结构
                    let itemStats = [];
                    if (project.scoreGroups) {
                        Object.values(project.scoreGroups).forEach(group => {
                            if (Array.isArray(group)) {
                                group.forEach((item, index) => {
                                    const itemScores = scores.map(s => s.scores.find(i => i.itemId === index)?.score || 0);
                                    itemStats.push({
                                        name: item.name,
                                        weight: item.weight,
                                        avgScore: itemScores.length ? 
                                            itemScores.reduce((a, b) => a + b, 0) / itemScores.length : 0,
                                        maxScore: itemScores.length ? Math.max(...itemScores) : 0,
                                        minScore: itemScores.length ? Math.min(...itemScores) : 0
                                    });
                                });
                            }
                        });
                    } else if (project.scoreItems) {
                        // 兼容旧的scoreItems结构
                        itemStats = project.scoreItems.map((item, index) => {
                        const itemScores = scores.map(s => s.scores.find(i => i.itemId === index)?.score || 0);
                        return {
                            name: item.name,
                            weight: item.weight,
                            avgScore: itemScores.length ? 
                                itemScores.reduce((a, b) => a + b, 0) / itemScores.length : 0,
                            maxScore: itemScores.length ? Math.max(...itemScores) : 0,
                            minScore: itemScores.length ? Math.min(...itemScores) : 0
                        };
                    });
                    }

                    return {
                        id: project.id,
                        name: project.name,
                        totalScore: this.calculateAverageScore(project.id),
                        completionRate: (scoredCount / expertsCount) * 100,
                        itemStats: itemStats
                    };
                });

                resolve({
                    success: true,
                    data: statistics
                });
            }, 300);
        });
    },
    
    // 导出Excel数据
    exportToExcel: function(projectId) {
        return new Promise((resolve) => {
            setTimeout(() => {
                const project = this.projects.find(p => p.id === projectId);
                const scores = this.scores.filter(s => s.projectId === projectId);
                
                const excelData = {
                    projectName: project.name,
                    exportTime: new Date().toISOString(),
                    scores: scores,
                    statistics: {
                        averageScore: this.calculateAverageScore(projectId),
                        // ...其他统计数据
                    }
                };
                
                resolve({
                    success: true,
                    data: excelData
                });
            }, 300);
        });
    },
    
    // 生成PDF报告数据
    generatePDFReport: function(projectId) {
        return new Promise((resolve) => {
            setTimeout(() => {
                const project = this.projects.find(p => p.id === projectId);
                const scores = this.scores.filter(s => s.projectId === projectId);
                
                const reportData = {
                    projectName: project.name,
                    generateTime: new Date().toISOString(),
                    summary: {
                        totalExperts: this.users.filter(u => u.role.startsWith('expert')).length,
                        completedScoring: new Set(scores.map(s => s.username)).size,
                        averageScore: this.calculateAverageScore(projectId)
                    },
                    scoreDetails: scores,
                    // ...其他报告数据
                };
                
                resolve({
                    success: true,
                    data: reportData
                });
            }, 300);
        });
    },
    
    // 批量更新项目状态
    batchUpdateProjects: function(projectIds, status) {
        return new Promise((resolve) => {
            setTimeout(() => {
                projectIds.forEach(id => {
                    const project = this.projects.find(p => p.id === id);
                    if (project) {
                        project.status = status;
                        project.updateTime = new Date().toISOString();
                    }
                });
                
                resolve({
                    success: true,
                    message: '批量更新成功'
                });
            }, 500);
        });
    },
    
    // 批量删除项目
    batchDeleteProjects: function(projectIds) {
        return new Promise((resolve) => {
            setTimeout(() => {
                this.projects = this.projects.filter(p => 
                    !projectIds.includes(p.id)
                );
                
                resolve({
                    success: true,
                    message: '批量删除成功'
                });
            }, 500);
        });
    },

    // 更新项目顺序
    updateProjectsOrder: function(projectIds) {
        return new Promise((resolve) => {
            setTimeout(() => {
                // 更新项目顺序
                this.projects = projectIds.map(id => 
                    this.projects.find(p => p.id === id)
                ).filter(Boolean);
                resolve({
                    success: true,
                    message: '更新项目顺序成功'
                });
            }, 300);
        });
    },

    // 创建评审任务
    createReviewTask: function(taskData) {
        return new Promise((resolve) => {
        setTimeout(() => {
            const newTaskId = 'task-' + Date.now();
            const newTask = {
                id: newTaskId,
                category: taskData.category,
                projectIdsInOrder: taskData.projectIdsInOrder.map(id => parseInt(id)),
                taskType: taskData.taskType,
                scoreGroupType: taskData.scoreGroupType,
                expertIds: taskData.expertIds,
                status: 'pending',
                createdAt: new Date().toISOString()
            };
            
            this.tasks.push(newTask);
            
            resolve({
                success: true,
                message: '评审任务创建成功',
                data: { taskId: newTaskId }
            });
        }, 300);
    });
    },
    
    // 新增：启用评审任务
    enableReviewTask: function(taskId) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const task = this.tasks.find(t => t.id === taskId);
                if (task) {
                    // 可选：如果只允许一个任务是 active，则先将其他 active 任务设为 inactive
                    // this.tasks.forEach(t => {
                    //     if (t.status === 'active') {
                    //         t.status = 'inactive'; // 或者其他非激活状态
                    //     }
                    // });
                    task.status = 'active';
                    console.log('Task enabled:', task);
                    resolve({ success: true, message: '任务已启用' });
                } else {
                    reject({ success: false, message: '未找到任务' });
                }
            }, 200);
        });
    },

    // 更新评审任务
updateReviewTask: function(taskId, taskData) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            const taskIndex = this.tasks.findIndex(t => t.id === taskId);
            if (taskIndex === -1) {
                reject({ success: false, message: '未找到该任务' });
                return;
            }

            const task = this.tasks[taskIndex];
            if (task.status !== 'pending') {
                reject({ success: false, message: '只能编辑待启用状态的任务' });
                return;
            }

            // 更新任务数据
            this.tasks[taskIndex] = {
                ...task,
                category: taskData.category,
                projectIdsInOrder: taskData.projectIdsInOrder.map(id => parseInt(id)),
                taskType: taskData.taskType,
                scoreGroupType: taskData.scoreGroupType,
                expertIds: taskData.expertIds,
                updatedAt: new Date().toISOString()
            };

            resolve({
                success: true,
                message: '评审任务更新成功',
                data: this.tasks[taskIndex]
            });
        }, 300);
    });
},
    
    // 新增：获取所有任务 (用于在管理界面显示任务列表)
    getTasks: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true, data: this.tasks });
            }, 200);
        });
    },

    // 新增：获取当前启用的任务及其详细项目信息
    getActiveTaskWithProjects: function() {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                // 获取当前登录专家用户名
                let currentExpert = null;
                try {
                    if (window && window.localStorage) {
                        const userInfo = JSON.parse(window.localStorage.getItem('userInfo') || '{}');
                        currentExpert = userInfo.username;
                    }
                } catch (e) {}

                const activeTask = this.tasks.find(t => t.status === 'active');
                if (activeTask && currentExpert && Array.isArray(activeTask.expertIds) && activeTask.expertIds.includes(currentExpert)) {
                    const projectsInOrder = (Array.isArray(activeTask.projectIdsInOrder) ? activeTask.projectIdsInOrder : []).map(projectId => {
                        const project = this.projects.find(p => p.id === projectId);
                        if (!project) return null;
                        const group = activeTask.scoreGroupType;
                        let scoreItems = (project.scoreGroups && project.scoreGroups[group]) ? project.scoreGroups[group] : [];
                        if (!Array.isArray(scoreItems)) scoreItems = [];
                        return {
                            ...project,
                            scoreItems: scoreItems
                        };
                    }).filter(p => p && typeof p === 'object' && Array.isArray(p.scoreItems));
                    resolve({ 
                        success: true, 
                        data: {
                            task: activeTask,
                            projectsInOrder: projectsInOrder
                        }
                    });
                } else {
                    resolve({ success: true, data: { task: null, projectsInOrder: [] } });
                }
            }, 200);
        });
    },
    
    // 获取已排序的项目列表
    getSortedProjects: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const sortedProjects = [...this.projects].sort((a, b) => 
                    (a.orderIndex || 0) - (b.orderIndex || 0)
                );
                
                // 为每个项目添加scoreItems字段
                const projectsWithScoreItems = sortedProjects.map(project => {
                    // 默认使用preliminary阶段的评分项
                    const scoreItems = Array.isArray(project.scoreGroups?.preliminary) 
                        ? project.scoreGroups.preliminary 
                        : [];
                    
                    return {
                        ...project,
                        scoreItems: scoreItems
                    };
                });
                
                resolve({
                    success: true,
                    data: projectsWithScoreItems
                });
            }, 300);
        });
    },

    // 新增：获取所有评分记录
    getScores: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    success: true,
                    data: this.scores  // 修改这里，使用this.scores替代scores
                });
            }, 300);
        });
    },

    // 添加获取评审专家用户列表的函数
    getExpertUsers: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const expertUsers = [
                        {
                            id: 'expert1',
                        name: '评审专家1',
                            username: '评审专家1',
                        role: 'expert1'
                        },
                        {
                            id: 'expert2',
                        name: '评审专家2',
                            username: '评审专家2',
                        role: 'expert2'
                        },
                        {
                            id: 'expert3',
                        name: '评审专家3',
                            username: '评审专家3',
                        role: 'expert3'
                        },
                        {
                            id: 'expert4',
                        name: '评审专家4',
                            username: '评审专家4',
                        role: 'expert4'
                        },
                        {
                            id: 'expert5',
                        name: '评审专家5',
                            username: '评审专家5',
                        role: 'expert5'
                    },
                    {
                        id: 'expert6',
                        name: '评审专家6',
                        username: '评审专家6',
                        role: 'expert6'
                    },
                    {
                        id: 'expert7',
                        name: '评审专家7',
                        username: '评审专家7',
                        role: 'expert7'
                    }
                ];
                
                resolve({
                    success: true,
                    data: expertUsers
                });
            }, 300);
        });
    },

    // 获取评审任务详情
    getReviewTask: function(taskId) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const task = this.tasks.find(t => t.id === taskId);
                if (task) {
                    resolve({
                        success: true,
                        data: task
                    });
                } else {
                    reject({
                        success: false,
                        message: '未找到该评审任务'
                    });
                }
            }, 300);
        });
    },

    // 新增：完成评审任务
    completeReviewTask: function(taskId, username) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const task = this.tasks.find(t => t.id === taskId);
                if (task) {
                    // 标记任务为已完成
                    task.status = 'completed';
                    task.completedBy = username;
                    task.completedAt = new Date().toISOString();
                    
                    resolve({
                        success: true,
                        message: '评审任务已完成',
                        data: {
                            taskId: taskId,
                            completedBy: username,
                            completedAt: task.completedAt
                        }
                    });
                } else {
                    reject({
                        success: false,
                        message: '未找到该评审任务'
                    });
                }
            }, 300);
        });
    },

    // 新增：获取用户操作日志
    getUserLogs: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    success: true,
                    data: userLogs
                });
            }, 300);
        });
    },

    // 新增：获取项目模板数据
    getProjectTemplates: function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const templates = [
                    {
                        id: 'template-preliminary',
                        name: '初赛评审模板',
                        description: '包含产业、投融资、知识产权三个评分项',
                        scoreItems: [
                            { name: '产业', minScore: 0, maxScore: 100, roles: ['expert1', 'expert2'] },
                            { name: '投融资', minScore: 0, maxScore: 100, roles: ['expert2', 'expert3'] },
                            { name: '知识产权', minScore: 0, maxScore: 100, roles: ['expert1', 'expert3'] }
                        ]
                    },
                    {
                        id: 'template-semifinal',
                        name: '复赛评审模板',
                        description: '包含产业技术、投融资、知识产权、企业高管四个评分项',
                        scoreItems: [
                            { name: '产业技术', minScore: 0, maxScore: 100, roles: ['expert1', 'expert2'] },
                            { name: '投融资', minScore: 0, maxScore: 100, roles: ['expert2', 'expert3'] },
                            { name: '知识产权', minScore: 0, maxScore: 100, roles: ['expert1', 'expert3'] },
                            { name: '企业高管', minScore: 0, maxScore: 100, roles: ['expert1', 'expert2', 'expert4'] }
                        ]
                    },
                    {
                        id: 'template-final',
                        name: '决赛评审模板',
                        description: '包含投资、知识产权、产业技术、技术经理人、企业高管五个评分项',
                        scoreItems: [
                            { name: '投资', minScore: 0, maxScore: 100, roles: ['expert1'] },
                            { name: '知识产权', minScore: 0, maxScore: 100, roles: ['expert2'] },
                            { name: '产业技术', minScore: 0, maxScore: 100, roles: ['expert3'] },
                            { name: '技术经理人', minScore: 0, maxScore: 100, roles: ['expert1', 'expert2'] },
                            { name: '企业高管', minScore: 0, maxScore: 100, roles: ['expert2', 'expert3', 'expert4'] }
                        ]
                    }
                ];
                
                resolve({
                    success: true,
                    data: templates
                });
            }, 300);
        });
    },

    // 新增：获取评分历史
    getScoringHistory: function(projectId, username) {
        return new Promise((resolve) => {
            setTimeout(() => {
                const userScores = this.scores.filter(s => 
                    s.projectId === projectId && s.username === username
                );
                
                const history = [];
                userScores.forEach(scoreRecord => {
                    scoreRecord.scores.forEach(score => {
                        history.push({
                            itemName: `评分项${score.itemId + 1}`,
                            score: score.score,
                            submitTime: scoreRecord.submitTime
                        });
                    });
                });
                
                resolve({
                    success: true,
                    data: history
                });
            }, 300);
        });
    },

    // 还原项目
    restoreProject: function(projectId) {
        return new Promise((resolve) => {
            // 假设项目数据在 mockProjects
            const project = this.projects.find(p => p.id === projectId);
            if (project && project.status === 'archived') {
                project.status = 'active';
                resolve({ success: true });
            } else {
                resolve({ success: false, message: '项目不存在或状态错误' });
            }
        });
    },

    // 计算总评分项数 - 支持新的scoreGroups结构
    calculateTotalScoreItems: function(project) {
        let total = 0;
        if (project.scoreGroups) {
            Object.values(project.scoreGroups).forEach(group => {
                if (Array.isArray(group)) {
                    group.forEach(item => {
                        total += (Array.isArray(item.roles) ? item.roles.length : 0);
                    });
                }
            });
        } else if (project.scoreItems) {
            total = project.scoreItems.reduce((sum, item) => 
                sum + (Array.isArray(item.roles) ? item.roles.length : 0), 0);
        }
        return total;
    }
};

const userLogs = [];

function logUserAction(username, action, details) {
    userLogs.push({
        username,
        action,
        details,
        timestamp: new Date().toISOString()
    });
}

// 初始化mockApi
mockApi.init();