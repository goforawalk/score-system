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
     * @returns {Promise} 评分进度
     */
    getProjectScoringProgress: function(projectId) {
        return http.get(apiConfig.projects.getProgress(projectId));
    },

    /**
     * 获取项目评分详情
     * @param {number} projectId 项目ID
     * @returns {Promise} 评分详情
     */
    getProjectScores: function(projectId) {
        return http.get(apiConfig.projects.getScores(projectId));
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
     * 获取当前活动任务及项目
     * @returns {Promise} 活动任务及项目
     */
    getActiveTaskWithProjects: function() {
        return http.get(apiConfig.tasks.active);
    },

    // 评分相关方法
    /**
     * 获取所有评分记录
     * @returns {Promise} 评分记录
     */
    getScores: function() {
        return http.get(apiConfig.scores.base);
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
                scores[item.itemId] = item.score;
            });
        } else if (typeof scoreData.scores === 'object') {
            // 如果已经是对象格式，直接使用
            scores = scoreData.scores;
        }
        
        // 创建标准化的评分数据
        const normalizedScoreData = {
            ...scoreData,
            projectId: projectId,
            scores: scores,
            // 确保其他必要字段存在
            totalScore: scoreData.totalScore || 0,
            comments: scoreData.comments || "",
            isDraft: scoreData.isDraft !== undefined ? scoreData.isDraft : false
        };
        
        return http.post(apiConfig.scores.base, normalizedScoreData);
    },

    /**
     * 获取评分历史
     * @param {number} projectId 项目ID
     * @param {string} username 用户名
     * @returns {Promise} 评分历史
     */
    getScoringHistory: function(projectId, username) {
        return http.get(apiConfig.scores.base + '/history', {
            projectId: projectId,
            username: username
        });
    },

    // 统计相关方法
    /**
     * 获取项目统计数据
     * @returns {Promise} 统计数据
     */
    getProjectStats: function() {
        return http.get(apiConfig.statistics.dashboard);
    },

    /**
     * 获取统计数据
     * @returns {Promise} 统计数据
     */
    getStatistics: function() {
        return http.get(apiConfig.statistics.base);
    }
};
