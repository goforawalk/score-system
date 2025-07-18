/**
 * API服务类，替代mock.js中的功能
 * 提供与后端API交互的方法
 */

const apiService = {
    // 认证相关方法
    /**
     * 用户登录
     * @param {string} username 用户名
     * @param {string} password 密码
     * @returns {Promise} 登录结果
     */
    login: function(username, password) {
        return http.post(apiConfig.auth.login, {
            username: username,
            password: password
        });
    },

    /**
     * 用户登出
     * @returns {Promise} 登出结果
     */
    logout: function() {
        return http.post(apiConfig.auth.logout);
    },

    // 用户管理相关方法
    /**
     * 获取用户列表
     * @returns {Promise} 用户列表
     */
    getUsers: function() {
        return http.get(apiConfig.users.base);
    },

    /**
     * 添加用户
     * @param {Object} userData 用户数据
     * @returns {Promise} 添加结果
     */
    addUser: function(userData) {
        return http.post(apiConfig.users.base, userData);
    },

    /**
     * 更新用户
     * @param {string} username 用户名
     * @param {Object} userData 用户数据
     * @returns {Promise} 更新结果
     */
    updateUser: function(username, userData) {
        return http.put(apiConfig.users.update(username), userData);
    },

    /**
     * 删除用户
     * @param {string} username 用户名
     * @returns {Promise} 删除结果
     */
    deleteUser: function(username) {
        return http.delete(apiConfig.users.delete(username));
    },

    // 项目管理相关方法
    /**
     * 获取项目列表
     * @returns {Promise} 项目列表
     */
    getProjects: function() {
        return http.get(apiConfig.projects.base);
    },

    /**
     * 获取项目详情
     * @param {number} id 项目ID
     * @returns {Promise} 项目详情
     */
    getProject: function(id) {
        return http.get(apiConfig.projects.getById(id));
    },

    /**
     * 创建项目
     * @param {Object} projectData 项目数据
     * @returns {Promise} 创建结果
     */
    createProject: function(projectData) {
        return http.post(apiConfig.projects.base, projectData);
    },

    /**
     * 更新项目
     * @param {number} id 项目ID
     * @param {Object} projectData 项目数据
     * @returns {Promise} 更新结果
     */
    updateProject: function(id, projectData) {
        return http.put(apiConfig.projects.update(id), projectData);
    },

    /**
     * 删除项目
     * @param {number} id 项目ID
     * @returns {Promise} 删除结果
     */
    deleteProject: function(id) {
        return http.delete(apiConfig.projects.delete(id));
    },

    /**
     * 获取项目评分进度
     * @param {number} projectId 项目ID
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分进度
     */
    getProjectScoringProgress: function(projectId, taskId) {
        let url = apiConfig.projects.getProgress(projectId);
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 获取项目评分详情
     * @param {number} projectId 项目ID
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分详情
     */
    getProjectScores: function(projectId, taskId) {
        let url = apiConfig.projects.getScores(projectId);
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 批量更新项目状态
     * @param {Array} projectIds 项目ID数组
     * @param {string} status 状态
     * @returns {Promise} 更新结果
     */
    batchUpdateProjects: function(projectIds, status) {
        return http.put(apiConfig.projects.base + '/batch-update', {
            projectIds: projectIds,
            status: status
        });
    },

    /**
     * 批量删除项目
     * @param {Array} projectIds 项目ID数组
     * @returns {Promise} 删除结果
     */
    batchDeleteProjects: function(projectIds) {
        return http.post(apiConfig.projects.base + '/batch-delete', {
            projectIds: projectIds
        });
    },

    /**
     * 更新项目顺序
     * @param {Array} projectIds 项目ID数组
     * @returns {Promise} 更新结果
     */
    updateProjectsOrder: function(projectIds) {
        return http.put(apiConfig.projects.base + '/order', {
            projectIds: projectIds
        });
    },

    // 评审任务相关方法
    /**
     * 获取任务列表
     * @returns {Promise} 任务列表
     */
    getTasks: function() {
        return http.get(apiConfig.tasks.base);
    },

    /**
     * 获取任务详情
     * @param {string} taskId 任务ID
     * @returns {Promise} 任务详情
     */
    getReviewTask: function(taskId) {
        return http.get(apiConfig.tasks.getById(taskId));
    },

    /**
     * 创建评审任务
     * @param {Object} taskData 任务数据
     * @returns {Promise} 创建结果
     */
    createReviewTask: function(taskData) {
        return http.post(apiConfig.tasks.base, taskData);
    },

    /**
     * 更新评审任务
     * @param {string} taskId 任务ID
     * @param {Object} taskData 任务数据
     * @returns {Promise} 更新结果
     */
    updateReviewTask: function(taskId, taskData) {
        return http.put(apiConfig.tasks.update(taskId), taskData);
    },

    /**
     * 启用评审任务
     * @param {string} taskId 任务ID
     * @returns {Promise} 启用结果
     */
    enableReviewTask: function(taskId) {
        return http.put(apiConfig.tasks.enable(taskId));
    },

    /**
     * 完成评审任务
     * @param {string} taskId 任务ID
     * @param {string} username 用户名
     * @returns {Promise} 完成结果
     */
    completeReviewTask: function(taskId, username) {
        return http.put(apiConfig.tasks.complete(taskId), {
            username: username
        });
    },

    /**
     * 检查任务完成状态
     * @param {string} taskId 任务ID
     * @returns {Promise} 任务完成状态
     */
    checkTaskCompletionStatus: function(taskId) {
        return http.get(apiConfig.tasks.getCompletionStatus(taskId));
    },

    /**
     * 获取当前活动任务及项目
     * @returns {Promise} 活动任务及项目
     */
    getActiveTaskWithProjects: function(username) {
        return http.get(apiConfig.tasks.active + '?username=' + encodeURIComponent(username));
    },

    // 评分相关方法
    /**
     * 获取所有评分记录
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分记录
     */
    getScores: function(taskId) {
        let url = apiConfig.scores.base;
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 提交评分
     * @param {Object} scoreData 评分数据
     * @returns {Promise} 提交结果
     */
    submitScore: function(scoreData) {
        // 确保projectId是数字类型
        const projectId = typeof scoreData.projectId === 'string' ? parseInt(scoreData.projectId) : scoreData.projectId;
        
        // 确保scores是对象格式（Map<Long, Integer>），不是数组
        let scores = {};
        if (Array.isArray(scoreData.scores)) {
            // 如果是数组格式，转换为对象格式
            scoreData.scores.forEach(item => {
                // 确保itemId是数字类型，score是整数
                const itemId = typeof item.itemId === 'string' ? parseInt(item.itemId) : item.itemId;
                const score = typeof item.score === 'string' ? parseInt(item.score) : item.score;
                scores[itemId] = score;
            });
        } else if (typeof scoreData.scores === 'object' && scoreData.scores !== null) {
            // 如果已经是对象格式，确保键值都是正确的类型
            Object.keys(scoreData.scores).forEach(key => {
                const itemId = typeof key === 'string' ? parseInt(key) : key;
                const score = typeof scoreData.scores[key] === 'string' ? parseInt(scoreData.scores[key]) : scoreData.scores[key];
                scores[itemId] = score;
            });
        }
        
        // 创建标准化的评分数据
        const normalizedScoreData = {
            projectId: projectId,
            taskId: scoreData.taskId,
            username: scoreData.username,
            scores: scores,
            totalScore: scoreData.totalScore || 0,
            comments: scoreData.comments || "",
            isDraft: scoreData.isDraft !== undefined ? scoreData.isDraft : false
        };
        
        console.log('提交评分数据（转换后）:', normalizedScoreData);
        return http.post(apiConfig.scores.base, normalizedScoreData);
    },

    /**
     * 获取评分历史
     * @param {number} projectId 项目ID
     * @param {string} username 用户名
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分历史
     */
    getScoringHistory: function(projectId, username, taskId) {
        let params = { projectId, username };
        if (taskId !== undefined && taskId !== null) {
            params.taskId = taskId;
        }
        return http.get(apiConfig.scores.base + '/history', params);
    },

    // 统计相关方法
    /**
     * 获取项目统计数据
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 统计数据
     */
    getProjectStats: function(taskId) {
        let url = apiConfig.statistics.dashboard;
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 获取统计数据
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 统计数据
     */
    getStatistics: function(taskId) {
        let url = apiConfig.statistics.base;
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 获取前端统计页面需要的完整统计数据
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 前端统计数据
     */
    getFrontendStatistics: function(taskId) {
        let url = apiConfig.statistics.frontend;
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 获取评分统计数据
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 统计数据
     */
    getScoreStatistics: function(taskId) {
        let url = apiConfig.statistics.base + '/scores';
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 按项目获取评分
     * @param {number} projectId 项目ID
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分记录
     */
    getScoresByProject: function(projectId, taskId) {
        let url = apiConfig.scores.getByProject(projectId);
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 按用户获取评分
     * @param {string} username 用户名
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分记录
     */
    getScoresByUser: function(username, taskId) {
        let url = apiConfig.scores.getByUser(username);
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 按专家获取评分（别名）
     * @param {string} username 用户名
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分记录
     */
    getScoresByExpert: function(username, taskId) {
        let url = apiConfig.scores.getByUser(username);
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 获取所有评分记录（别名）
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分记录
     */
    getAllScores: function(taskId) {
        let url = apiConfig.scores.base + '/all';
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    // 新增：获取项目详情（支持任务ID）
    /**
     * 获取项目详情
     * @param {number} projectId 项目ID
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 项目详情
     */
    getProjectDetails: function(projectId, taskId) {
        let url = apiConfig.projects.getById(projectId);
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    // 新增：根据任务获取评分数据
    /**
     * 根据任务获取评分数据
     * @param {number} taskId 任务ID
     * @returns {Promise} 评分数据
     */
    getScoresByTask: function(taskId) {
        if (!taskId) {
            throw new Error('任务ID不能为空');
        }
        return http.get(apiConfig.scores.base + '/task/' + encodeURIComponent(taskId));
    },

    // 新增：获取任务下的项目列表
    /**
     * 获取任务下的项目列表
     * @param {number} taskId 任务ID
     * @returns {Promise} 项目列表
     */
    getProjectsByTask: function(taskId) {
        if (!taskId) {
            throw new Error('任务ID不能为空');
        }
        return http.get(apiConfig.projects.base + '/task/' + encodeURIComponent(taskId));
    },

    // 新增：获取任务统计概览
    /**
     * 获取任务统计概览
     * @param {number} taskId 任务ID
     * @returns {Promise} 统计概览
     */
    getTaskOverview: function(taskId) {
        if (!taskId) {
            throw new Error('任务ID不能为空');
        }
        return http.get(apiConfig.statistics.base + '/task/' + encodeURIComponent(taskId) + '/overview');
    },

    // 新增：获取项目在任务下的统计详情
    /**
     * 获取项目在任务下的统计详情
     * @param {number} projectId 项目ID
     * @param {number} taskId 任务ID
     * @returns {Promise} 统计详情
     */
    getProjectTaskStatistics: function(projectId, taskId) {
        if (!projectId || !taskId) {
            throw new Error('项目ID和任务ID不能为空');
        }
        return http.get(apiConfig.statistics.base + '/project/' + projectId + '/task/' + encodeURIComponent(taskId));
    },

    // 新增：获取评分项统计
    /**
     * 获取评分项统计
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 评分项统计
     */
    getScoreItemStatistics: function(taskId) {
        let url = apiConfig.statistics.base + '/score-items';
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    // 新增：获取专家评分统计
    /**
     * 获取专家评分统计
     * @param {number} [taskId] 任务ID
     * @returns {Promise} 专家评分统计
     */
    getExpertStatistics: function(taskId) {
        let url = apiConfig.statistics.base + '/experts';
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    // 新增：导出统计数据
    /**
     * 导出统计数据
     * @param {number} taskId 任务ID
     * @param {string} format 导出格式 (excel/pdf)
     * @param {Object} options 导出选项
     * @returns {Promise} 导出结果
     */
    exportStatistics: function(taskId, format, options) {
        if (!taskId) {
            throw new Error('任务ID不能为空');
        }
        const params = {
            format: format,
            ...options
        };
        return http.post(apiConfig.statistics.base + '/export/' + encodeURIComponent(taskId), params);
    },

    // 获取简化任务列表
    /**
     * 获取简化任务列表
     * @param {boolean} [includeProjectCount] 是否包含项目数量
     * @returns {Promise}
     */
    getSimpleTasks: function(includeProjectCount) {
        let url = apiConfig.tasks.base + '/simple';
        if (includeProjectCount === true) {
            url += '?includeProjectCount=true';
        }
        return http.get(url);
    },

    // 获取简化项目列表，可选 taskId 过滤
    getSimpleProjects: function(taskId) {
        let url = apiConfig.projects.base + '/simple';
        if (taskId !== undefined && taskId !== null) {
            url += '?taskId=' + encodeURIComponent(taskId);
        }
        return http.get(url);
    },

    /**
     * 获取指定任务下所有项目的进度和总分
     * @param {number|string} taskId
     * @returns {Promise}
     */
    getTaskProjectProgressAndScores: function(taskId) {
        if (!taskId) {
            throw new Error('taskId不能为空');
        }
        return http.get(apiConfig.tasks.base + '/' + encodeURIComponent(taskId) + '/progress-scores');
    }
};
