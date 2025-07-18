/**
 * 后端API服务配置文件
 * 包含API基础URL和各个API端点的路径
 */

const apiConfig = {
    // API基础URL，根据环境配置不同的地址
    get baseUrl() {
        return env.getApiBaseUrl();
    },

    // 认证相关接口
    auth: {
        login: '/api/auth/login',
        logout: '/api/auth/logout'
    },

    // 用户管理相关接口
    users: {
        base: '/api/users',
        getById: function(username) { 
            return '/api/users/' + username;
        },
        update: function(username) {
            return '/api/users/' + username;
        },
        delete: function(username) {
            return '/api/users/' + username;
        }
    },

    // 项目管理相关接口
    projects: {
        base: '/api/projects',
        getById: function(id) {
            return '/api/projects/' + id;
        },
        update: function(id) {
            return '/api/projects/' + id;
        },
        delete: function(id) {
            return '/api/projects/' + id;
        },
        getProgress: function(id) {
            return '/api/projects/' + id + '/progress';
        },
        getScores: function(id) {
            return '/api/projects/' + id + '/scores';
        },
        getScoreItems: function(id) {
            return '/api/projects/' + id + '/score-items';
        }
    },

    // 评审任务相关接口
    tasks: {
        base: '/api/tasks',
        getById: function(id) {
            return '/api/tasks/' + id;
        },
        update: function(id) {
            return '/api/tasks/' + id;
        },
        delete: function(id) {
            return '/api/tasks/' + id;
        },
        active: '/api/tasks/active',
        enable: function(id) {
            return '/api/tasks/' + id + '/enable';
        },
        complete: function(id) {
            return '/api/tasks/' + id + '/complete';
        },
        getCompletionStatus: function(id) {
            return '/api/tasks/' + id + '/completion-status';
        }
    },

    // 评分相关接口
    scores: {
        base: '/api/scores',
        getByProject: function(projectId) {
            return '/api/scores/project/' + projectId;
        },
        getByUser: function(username) {
            return '/api/scores/user/' + username;
        }
    },

    // 统计相关接口
    statistics: {
        base: '/api/statistics',
        dashboard: '/api/statistics/dashboard',
        frontend: '/api/statistics/frontend'
    }
};
