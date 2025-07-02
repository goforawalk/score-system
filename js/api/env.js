/**
 * 环境配置文件
 * 用于切换开发环境和生产环境
 */

const env = {
    // 当前环境：development 或 production
    current: 'development',
    
    // 是否使用模拟API - 强制设置为true，避免后端数据源连接问题
    useMockApi: false,  // 默认使用模拟API
    
    // API基础URL
    apiBaseUrl: {
        development: 'http://192.168.9.243:8080',
        production: 'https://api.score-system.com/api'
    },
    
    // 获取当前环境的API基础URL
    getApiBaseUrl: function() {
        return this.apiBaseUrl[this.current];
    },
    
    // 切换到开发环境
    setDevelopment: function() {
        this.current = 'development';
        console.log('已切换到开发环境');
    },
    
    // 切换到生产环境
    setProduction: function() {
        this.current = 'production';
        console.log('已切换到生产环境');
    },
    
    // 切换是否使用模拟API
    toggleMockApi: function() {
        // 检查后端连接状态
        if (this.useMockApi === true && !confirm('警告：后端数据源存在配置问题，切换到真实API可能导致所有请求失败。确定要继续吗？')) {
            console.log('已取消切换到真实API');
            return;
        }
        
        this.useMockApi = !this.useMockApi;
        console.log('模拟API已' + (this.useMockApi ? '启用' : '禁用'));
    }
}; 