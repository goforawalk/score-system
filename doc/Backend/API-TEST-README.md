# 后端API测试文档

## 测试概述

本文档描述了评分系统后端API的测试方案和测试覆盖情况。测试分为两个主要部分：

1. **单元测试**：测试各个控制器方法的独立功能
2. **集成测试**：测试前端API需求与后端接口的匹配情况

## 测试环境配置

### 依赖项

测试使用以下主要依赖：

- JUnit 5：测试框架
- Mockito：模拟依赖
- Spring Test：Spring应用程序测试支持

### 配置文件

测试使用`application-test.properties`配置文件，该文件位于`src/test/resources`目录下。

## 运行测试

### 使用Maven运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ScoreSystemControllerTest

# 运行集成测试
mvn test -Dtest=ApiIntegrationTest
```

### 使用IDE运行测试

1. 在IDE中打开项目
2. 右键点击测试类或测试方法
3. 选择"运行测试"

## 测试覆盖情况

### 控制器测试 (ScoreSystemControllerTest)

| API端点 | 测试方法 | 描述 |
|---------|---------|------|
| POST /api/auth/login | testLogin | 测试用户登录功能 |
| POST /api/auth/logout | testLogout | 测试用户登出功能 |
| GET /api/users | testGetUsers | 测试获取用户列表 |
| POST /api/users | testSaveUser | 测试创建/更新用户 |
| DELETE /api/users/{username} | testDeleteUser | 测试删除用户 |
| GET /api/projects | testGetProjects | 测试获取项目列表 |
| POST /api/projects | testSaveProject | 测试创建/更新项目 |
| DELETE /api/projects/{id} | testDeleteProject | 测试删除项目 |
| GET /api/tasks/active | testGetActiveTasks | 测试获取当前活动任务及项目 |
| POST /api/scores | testSubmitScore | 测试提交评分 |
| GET /api/scores/history | testGetScoreHistory | 测试获取评分历史 |
| GET /api/statistics | testGetStatistics | 测试获取统计数据 |

### 集成测试 (ApiIntegrationTest)

| 测试组 | 测试方法 | 描述 |
|-------|---------|------|
| 用户认证 | testAuthApi | 测试登录和登出接口 |
| 用户管理 | testUserApi | 测试用户CRUD操作 |
| 项目管理 | testProjectApi | 测试项目CRUD操作及批量操作 |
| 评审任务 | testTaskApi | 测试任务相关接口 |
| 评分 | testScoreApi | 测试评分提交和查询 |
| 统计 | testStatisticsApi | 测试统计数据接口 |

## 前端API需求与后端接口匹配分析

根据前端`js/api/api-service.js`和`js/api/config.js`文件中的API需求，我们对后端接口进行了匹配分析：

### 已实现的接口

✅ 用户认证相关接口 (login, logout)  
✅ 用户管理基本接口 (getUsers, addUser, deleteUser)  
✅ 项目管理基本接口 (getProjects, saveProject, deleteProject)  
✅ 评审任务相关接口 (getActiveTaskWithProjects)  
✅ 评分相关接口 (submitScore, getScoreHistory)  
✅ 统计相关接口 (getStatistics)  

### 需要补充实现的接口

以下是前端需要但后端尚未完全实现的接口：

1. **项目管理扩展接口**
   - PUT /api/projects/batch-update (批量更新项目状态)
   - POST /api/projects/batch-delete (批量删除项目)
   - PUT /api/projects/order (更新项目顺序)
   - GET /api/projects/{id}/progress (获取项目评分进度)
   - GET /api/projects/{id}/scores (获取项目评分详情)

2. **任务管理扩展接口**
   - GET /api/tasks (获取任务列表)
   - GET /api/tasks/{id} (获取任务详情)
   - POST /api/tasks (创建评审任务)
   - PUT /api/tasks/{id} (更新评审任务)
   - PUT /api/tasks/{id}/enable (启用评审任务)
   - PUT /api/tasks/{id}/complete (完成评审任务)

3. **评分扩展接口**
   - GET /api/scores (获取所有评分记录)
   - GET /api/scores/project/{projectId} (按项目获取评分)
   - GET /api/scores/user/{username} (按用户获取评分)

4. **统计扩展接口**
   - GET /api/statistics/dashboard (获取仪表盘统计数据)

## 接口补充实现建议

为了满足前端API需求，建议在`ScoreSystemController`中添加以下方法：

```java
// 项目管理扩展接口
@PutMapping("/projects/batch-update")
public ResponseEntity<ApiResponse<Void>> batchUpdateProjects(@RequestBody Map<String, Object> request) {
    // 实现批量更新项目状态
}

@PostMapping("/projects/batch-delete")
public ResponseEntity<ApiResponse<Void>> batchDeleteProjects(@RequestBody Map<String, Object> request) {
    // 实现批量删除项目
}

@PutMapping("/projects/order")
public ResponseEntity<ApiResponse<Void>> updateProjectsOrder(@RequestBody Map<String, Object> request) {
    // 实现更新项目顺序
}

@GetMapping("/projects/{id}/progress")
public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectProgress(@PathVariable Long id) {
    // 实现获取项目评分进度
}

@GetMapping("/projects/{id}/scores")
public ResponseEntity<ApiResponse<List<ScoreDTO>>> getProjectScores(@PathVariable Long id) {
    // 实现获取项目评分详情
}

// 任务管理扩展接口
@GetMapping("/tasks")
public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks() {
    // 实现获取任务列表
}

@GetMapping("/tasks/{id}")
public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long id) {
    // 实现获取任务详情
}

@PostMapping("/tasks")
public ResponseEntity<ApiResponse<TaskDTO>> createTask(@RequestBody TaskDTO taskDTO) {
    // 实现创建评审任务
}

@PutMapping("/tasks/{id}")
public ResponseEntity<ApiResponse<TaskDTO>> updateTask(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
    // 实现更新评审任务
}

@PutMapping("/tasks/{id}/enable")
public ResponseEntity<ApiResponse<TaskDTO>> enableTask(@PathVariable Long id) {
    // 实现启用评审任务
}

@PutMapping("/tasks/{id}/complete")
public ResponseEntity<ApiResponse<TaskDTO>> completeTask(@PathVariable Long id, @RequestBody Map<String, String> request) {
    // 实现完成评审任务
}

// 评分扩展接口
@GetMapping("/scores")
public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScores() {
    // 实现获取所有评分记录
}

@GetMapping("/scores/project/{projectId}")
public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByProject(@PathVariable Long projectId) {
    // 实现按项目获取评分
}

@GetMapping("/scores/user/{username}")
public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByUser(@PathVariable String username) {
    // 实现按用户获取评分
}

// 统计扩展接口
@GetMapping("/statistics/dashboard")
public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
    // 实现获取仪表盘统计数据
}
```

## 测试数据准备

为了更好地进行测试，建议准备以下测试数据：

1. 测试用户数据（管理员、专家、普通用户）
2. 测试项目数据
3. 测试任务数据
4. 测试评分数据

可以在测试类的`@BeforeEach`方法中准备这些数据，或者使用数据库初始化脚本。

## 常见问题解决

1. **测试数据库连接问题**：确保测试配置文件中的数据库连接信息正确
2. **认证问题**：在测试中可能需要模拟用户认证
3. **事务问题**：使用`@Transactional`注解确保测试不影响实际数据 