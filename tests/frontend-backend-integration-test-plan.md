# 前端后端页面联调测试计划

## 测试概述

基于 `api-adapter-test.html` 页面的标准API调用方式，对前端页面进行完整的前后端联调测试，确保所有页面都能正确调用后端API服务。

## 标准API调用方式

### 1. API适配器模式
- **模拟API模式**：使用 `mockApi` 进行测试，避免后端连接问题
- **真实API模式**：使用 `apiService` 调用真实后端服务
- **切换机制**：通过 `api.switchToMock()` 和 `api.switchToReal()` 切换

### 2. 标准调用流程
```javascript
// 1. 引入必要的API文件
<script src="../js/api/mock.js"></script>
<script src="../js/api/env.js"></script>
<script src="../js/api/config.js"></script>
<script src="../js/api/http.js"></script>
<script src="../js/api/api-service.js"></script>
<script src="../js/api/api-adapter.js"></script>

// 2. 强制使用模拟API（推荐）
window.addEventListener('DOMContentLoaded', function() {
    if (typeof env !== 'undefined') {
        env.useMockApi = true;
    }
    api.switchToMock();
});

// 3. 调用API方法
api.login(username, password)
    .then(function(response) {
        // 处理成功响应
        console.log('成功:', response);
    })
    .catch(function(error) {
        // 处理错误
        console.error('失败:', error);
    });
```

## 需要测试的页面清单

### 管理员页面 (admin/)
1. **dashboard.html** - 仪表盘页面
2. **user-management.html** - 用户管理页面
3. **project-management.html** - 项目管理页面
4. **scoring-management.html** - 评分管理页面
5. **statistics.html** - 统计页面

### 专家页面 (expert/)
1. **scoring.html** - 评分页面
2. **review-complete.html** - 评审完成页面

### 公共页面
1. **index.html** - 登录页面

## 详细测试计划

### 1. 登录页面 (index.html)

#### 测试目标
- 验证登录功能是否正常调用后端API
- 验证登录成功后的页面跳转
- 验证错误处理和用户提示

#### 需要验证的API调用
```javascript
// 登录API
api.login(username, password)
```

#### 测试步骤
1. 打开登录页面
2. 输入正确的用户名密码
3. 点击登录按钮
4. 验证是否调用 `api.login()` 方法
5. 验证登录成功后的跳转
6. 测试错误情况（错误密码、网络错误等）

### 2. 管理员仪表盘 (admin/dashboard.html)

#### 测试目标
- 验证页面加载时是否正确获取统计数据
- 验证各个统计模块的数据显示

#### 需要验证的API调用
```javascript
// 获取统计数据
api.getStatistics()
api.getDashboardStatistics()
```

#### 测试步骤
1. 登录管理员账户
2. 访问仪表盘页面
3. 验证页面加载时是否调用统计API
4. 验证统计数据是否正确显示
5. 测试数据刷新功能

### 3. 用户管理页面 (admin/user-management.html)

#### 测试目标
- 验证用户列表的获取和显示
- 验证用户增删改功能
- 验证批量操作功能

#### 需要验证的API调用
```javascript
// 获取用户列表
api.getUsers()

// 添加用户
api.addUser(userData)

// 更新用户
api.updateUser(username, userData)

// 删除用户
api.deleteUser(username)
```

#### 测试步骤
1. 访问用户管理页面
2. 验证用户列表是否正确加载
3. 测试添加新用户功能
4. 测试编辑用户信息功能
5. 测试删除用户功能
6. 测试批量操作功能

### 4. 项目管理页面 (admin/project-management.html)

#### 测试目标
- 验证项目列表的获取和显示
- 验证项目的增删改功能
- 验证项目状态管理
- 验证项目顺序调整

#### 需要验证的API调用
```javascript
// 获取项目列表
api.getProjects()

// 创建项目
api.createProject(projectData)

// 更新项目
api.updateProject(id, projectData)

// 删除项目
api.deleteProject(id)

// 批量更新项目状态
api.batchUpdateProjects(projectIds, status)

// 批量删除项目
api.batchDeleteProjects(projectIds)

// 更新项目顺序
api.updateProjectsOrder(projectIds)

// 获取项目评分进度
api.getProjectScoringProgress(projectId)

// 获取项目评分详情
api.getProjectScores(projectId)
```

#### 测试步骤
1. 访问项目管理页面
2. 验证项目列表是否正确加载
3. 测试创建新项目功能
4. 测试编辑项目信息功能
5. 测试删除项目功能
6. 测试批量操作功能
7. 测试项目状态管理
8. 测试项目顺序调整

### 5. 评分管理页面 (admin/scoring-management.html)

#### 测试目标
- 验证评分数据的获取和显示
- 验证评分统计功能
- 验证评分导出功能

#### 需要验证的API调用
```javascript
// 获取评分历史
api.getScoringHistory(projectId, username)

// 获取项目评分进度
api.getProjectScoringProgress(projectId)

// 获取项目评分详情
api.getProjectScores(projectId)
```

#### 测试步骤
1. 访问评分管理页面
2. 验证评分数据是否正确加载
3. 测试评分统计功能
4. 测试评分导出功能
5. 测试评分详情查看

### 6. 统计页面 (admin/statistics.html)

#### 测试目标
- 验证统计数据的获取和显示
- 验证图表渲染功能
- 验证数据筛选功能

#### 需要验证的API调用
```javascript
// 获取统计数据
api.getStatistics()

// 获取前端统计数据
api.getFrontendStatistics()

// 获取项目统计数据
api.getProjectStatistics()
```

#### 测试步骤
1. 访问统计页面
2. 验证统计数据是否正确加载
3. 测试图表渲染功能
4. 测试数据筛选功能
5. 测试数据导出功能

### 7. 专家评分页面 (expert/scoring.html)

#### 测试目标
- 验证评分任务的获取和显示
- 验证评分提交功能
- 验证评分保存功能

#### 需要验证的API调用
```javascript
// 获取活动任务及项目
api.getActiveTaskWithProjects()

// 提交评分
api.submitScore(scoreData)

// 获取评分历史
api.getScoringHistory(projectId, username)
```

#### 测试步骤
1. 登录专家账户
2. 访问评分页面
3. 验证评分任务是否正确加载
4. 测试评分提交功能
5. 测试评分保存功能
6. 测试评分历史查看

### 8. 评审完成页面 (expert/review-complete.html)

#### 测试目标
- 验证评审完成状态的显示
- 验证评审结果查看功能

#### 需要验证的API调用
```javascript
// 获取活动任务及项目
api.getActiveTaskWithProjects()

// 获取项目评分进度
api.getProjectScoringProgress(projectId)
```

#### 测试步骤
1. 访问评审完成页面
2. 验证评审完成状态是否正确显示
3. 测试评审结果查看功能

## 测试环境配置

### 1. 模拟API模式（推荐）
```javascript
// 强制使用模拟API
env.useMockApi = true;
api.switchToMock();
```

### 2. 真实API模式（需要后端服务）
```javascript
// 切换到真实API
api.switchToReal();
```

## 测试检查清单

### 每个页面都需要检查的项目：

#### 1. API调用检查
- [ ] 页面是否正确引入了API相关文件
- [ ] 页面是否使用了标准的API调用方式
- [ ] API调用是否包含了错误处理
- [ ] API调用是否使用了Promise处理

#### 2. 用户体验检查
- [ ] 页面加载时是否显示加载状态
- [ ] API调用失败时是否显示错误信息
- [ ] 操作成功时是否显示成功提示
- [ ] 页面跳转是否正常

#### 3. 数据验证检查
- [ ] 接收到的数据格式是否正确
- [ ] 数据是否正确显示在页面上
- [ ] 数据更新时页面是否正确刷新

#### 4. 功能完整性检查
- [ ] 所有按钮和链接是否正常工作
- [ ] 表单提交是否正常
- [ ] 数据筛选和搜索是否正常
- [ ] 分页功能是否正常

## 测试执行步骤

### 第一阶段：模拟API测试
1. 确保所有页面都配置为使用模拟API
2. 逐个测试每个页面的功能
3. 记录发现的问题
4. 修复前端代码问题

### 第二阶段：真实API测试
1. 确保后端服务正常运行
2. 切换到真实API模式
3. 重复第一阶段的所有测试
4. 验证前后端数据一致性
5. 修复发现的问题

### 第三阶段：集成测试
1. 测试完整的用户流程
2. 测试跨页面的数据一致性
3. 测试并发操作
4. 性能测试

## 问题记录模板

### 问题记录格式
```
页面：admin/dashboard.html
问题类型：API调用/用户体验/数据验证/功能完整性
问题描述：[详细描述问题]
重现步骤：[如何重现问题]
期望结果：[期望的正确行为]
实际结果：[实际观察到的行为]
严重程度：高/中/低
修复状态：待修复/修复中/已修复
```

## 测试工具和资源

### 1. 浏览器开发者工具
- Network面板：监控API请求
- Console面板：查看错误信息
- Application面板：检查localStorage

### 2. 测试数据
- 使用测试数据生成功能创建测试数据
- 准备各种测试场景的数据

### 3. 测试环境
- 本地开发环境
- 测试服务器环境
- 生产环境（最终验证）

## 完成标准

### 测试完成标准
1. 所有页面都能正常加载
2. 所有API调用都能正常工作
3. 所有用户交互功能都能正常响应
4. 错误处理机制完善
5. 用户体验良好
6. 前后端数据一致

### 验收标准
1. 通过所有功能测试
2. 通过所有API集成测试
3. 通过用户体验测试
4. 通过性能测试
5. 通过安全测试
6. 文档完整且准确 