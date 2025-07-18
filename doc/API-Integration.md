# 评分系统前后端API对接文档

## 1. 概述

本文档描述了评分系统前端与后端API的对接方案，包括API接口规范、前端调用方式和开发/生产环境切换机制。

## 2. API架构

我们采用了一种灵活的API架构，可以在开发环境中使用模拟API（mock.js），在生产环境中使用真实API（api-service.js）。主要组件包括：

- **env.js**: 环境配置文件，控制当前环境（开发/生产）和是否使用模拟API
- **config.js**: API配置文件，定义API基础URL和各个端点路径
- **http.js**: HTTP工具类，封装了基本的HTTP请求方法（GET、POST、PUT、DELETE）
- **api-service.js**: 真实API服务类，与后端API交互
- **api-adapter.js**: API适配器，根据环境配置选择使用模拟API或真实API

## 3. API接口规范

### 3.1 通用规范

- 所有API请求都使用JSON格式
- 所有API响应都遵循统一的格式：
  ```json
  {
    "success": true/false,
    "data": {...},
    "message": "错误信息（如果success为false）"
  }
  ```
- 所有API请求都需要在Header中携带认证令牌（除了登录接口）：
  ```
  Authorization: Bearer <token>
  ```

### 3.2 主要接口列表

#### 3.2.1 认证相关接口

| 接口名称 | 请求方式 | 路径 | 参数 | 返回数据 |
|---------|--------|------|------|---------|
| 用户登录 | POST | /api/auth/login | {username, password} | {success, data: {username, role, token}} |
| 用户登出 | POST | /api/auth/logout | 无 | {success} |

#### 3.2.2 用户管理相关接口

| 接口名称 | 请求方式 | 路径 | 参数 | 返回数据 |
|---------|--------|------|------|---------|
| 获取用户列表 | GET | /api/users | 无 | {success, data: [用户列表]} |
| 添加用户 | POST | /api/users | {username, password, role, name, ...} | {success, data: 用户信息} |
| 更新用户 | PUT | /api/users/:username | {用户数据} | {success, data: 用户信息} |
| 删除用户 | DELETE | /api/users/:username | 无 | {success} |

#### 3.2.3 项目管理相关接口

| 接口名称 | 请求方式 | 路径 | 参数 | 返回数据 |
|---------|--------|------|------|---------|
| 获取项目列表 | GET | /api/projects | 无 | {success, data: [项目列表]} |
| 获取项目详情 | GET | /api/projects/:id | 无 | {success, data: 项目详情} |
| 创建项目 | POST | /api/projects | {项目数据} | {success, data: 项目信息} |
| 更新项目 | PUT | /api/projects/:id | {项目数据} | {success, data: 项目信息} |
| 删除项目 | DELETE | /api/projects/:id | 无 | {success} |
| 获取项目评分进度 | GET | /api/projects/:id/progress | 无 | {success, data: {total, completed}} |

#### 3.2.4 评审任务相关接口

| 接口名称 | 请求方式 | 路径 | 参数 | 返回数据 |
|---------|--------|------|------|---------|
| 获取任务列表 | GET | /api/tasks | 无 | {success, data: [任务列表]} |
| 获取任务详情 | GET | /api/tasks/:id | 无 | {success, data: 任务详情} |
| 创建评审任务 | POST | /api/tasks | {任务数据} | {success, data: 任务信息} |
| 更新评审任务 | PUT | /api/tasks/:id | {任务数据} | {success, data: 任务信息} |
| 启用评审任务 | PUT | /api/tasks/:id/enable | 无 | {success} |
| 完成评审任务 | PUT | /api/tasks/:id/complete | {username} | {success} |
| 获取当前活动任务及项目 | GET | /api/tasks/active | 无 | {success, data: {task, projectsInOrder}} |

#### 3.2.5 评分相关接口

| 接口名称 | 请求方式 | 路径 | 参数 | 返回数据 |
|---------|--------|------|------|---------|
| 获取所有评分记录 | GET | /api/scores | 无 | {success, data: [评分记录列表]} |
| 提交评分 | POST | /api/scores | {projectId, username, scores: [{itemId, score}]} | {success, data: 评分信息} |
| 获取评分历史 | GET | /api/scores/history | {projectId, username} | {success, data: [评分历史记录]} |

## 4. 前端调用示例

### 4.1 用户登录

```javascript
api.login(username, password)
    .then(function(response) {
        if (response.success) {
            // 保存用户信息到localStorage
            localStorage.setItem('userInfo', JSON.stringify(response.data));
            // 跳转到对应页面
            redirectByRole(response.data.role);
        }
    })
    .catch(function(error) {
        showError(error.message || '登录失败');
    });
```

### 4.2 获取项目列表

```javascript
api.getProjects()
    .then(function(response) {
        if (response.success) {
            renderProjects(response.data);
        }
    })
    .catch(function(error) {
        showError(error.message || '获取项目列表失败');
    });
```

### 4.3 提交评分

```javascript
const scoreData = {
    projectId: projectId,
    username: userInfo.username,
    scores: [
        { itemId: 0, score: 85 },
        { itemId: 1, score: 90 }
    ]
};

api.submitScore(scoreData)
    .then(function(response) {
        if (response.success) {
            showSuccess('评分提交成功');
        }
    })
    .catch(function(error) {
        showError(error.message || '评分提交失败');
    });
```

## 5. 开发/生产环境切换

### 5.1 环境配置

在`env.js`中可以配置当前环境和是否使用模拟API：

```javascript
const env = {
    // 当前环境：development 或 production
    current: 'development',
    
    // 是否使用模拟API
    useMockApi: true,
    
    // API基础URL
    apiBaseUrl: {
        development: 'http://localhost:8080/api',
        production: 'https://api.score-system.com/api'
    }
};
```

### 5.2 切换API实现

可以通过API适配器提供的方法切换API实现：

```javascript
// 切换到模拟API
api.switchToMock();

// 切换到真实API
api.switchToReal();

// 获取当前API模式
const mode = api.getCurrentMode(); // 'mock' 或 'real'
```

## 6. 测试

我们提供了一个测试页面`tests/api-adapter-test.html`，用于测试API适配器和各个API接口。可以通过该页面验证API接口的正确性和切换API实现的功能。

## 7. 注意事项

1. 在开发环境中，默认使用模拟API（mock.js）
2. 在生产环境中，需要切换到真实API（api-service.js）
3. 所有API调用都应该通过API适配器（api）进行，而不是直接调用模拟API（mockApi）或真实API（apiService）
4. 所有API调用都应该处理成功和失败的情况
5. 所有API调用都应该处理网络错误和服务器错误

## 8. 后续工作

1. 完善后端API实现
2. 添加更多的API接口测试用例
3. 添加API文档自动生成工具
4. 添加API版本控制机制
5. 添加API性能监控和日志记录
