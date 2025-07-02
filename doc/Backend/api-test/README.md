# 后端API测试工具

本目录包含用于测试后端API接口的独立测试工具。这些测试工具可以在不依赖前端的情况下，验证后端接口的正确性和完整性。

## 1. API测试工具

### 1.1 Postman测试集合

`score-system-api.postman_collection.json` 是一个完整的Postman测试集合，包含了所有API接口的测试用例和自动验证脚本。

**使用方法：**

1. 在Postman中导入该集合文件
2. 创建环境变量，设置`base_url`为你的服务器地址（默认为`http://localhost:8080`）
3. 按照顺序运行测试用例：
   - 先运行登录接口获取认证令牌
   - 然后测试其他接口

### 1.2 测试内容

Postman集合测试以下API接口：

- `/api/auth/login` - 用户登录
- `/api/auth/logout` - 用户登出
- `/api/users` - 用户管理（获取列表、新增/更新、删除）
- `/api/projects` - 项目管理（获取列表、新增/更新、删除）
- `/api/tasks/active` - 获取活动任务
- `/api/scores` - 评分管理（提交评分、获取评分历史）
- `/api/statistics` - 获取统计数据

## 2. 单元测试

项目中包含以下单元测试文件：

- `/src/test/java/com/scoresystem/controller/ScoreSystemControllerTest.java` - 控制器单元测试
- `/src/test/java/com/scoresystem/service/UserServiceTest.java` - 用户服务单元测试
- `/src/test/java/com/scoresystem/service/ProjectServiceTest.java` - 项目服务单元测试
- `/src/test/java/com/scoresystem/service/TaskServiceTest.java` - 任务服务单元测试
- `/src/test/java/com/scoresystem/service/ScoreServiceTest.java` - 评分服务单元测试

这些单元测试使用JUnit 5和Mockito框架，可以通过Maven执行：

```bash
mvn test
```

## 3. 集成测试

`/src/test/java/com/scoresystem/ApiIntegrationTest.java` 是一个集成测试类，使用Spring Boot Test进行测试，它将启动完整的应用程序上下文，并通过TestRestTemplate发送HTTP请求。

要运行集成测试：

```bash
mvn test -Dtest=ApiIntegrationTest
```

## 4. 测试策略

在测试后端API时，建议按照以下策略进行：

1. **单元测试**：验证每个组件的独立功能
   - 测试各个Service的业务逻辑
   - 测试Controller的请求处理和响应生成

2. **集成测试**：验证组件之间的交互和集成
   - 测试从Controller到Service再到Repository的完整流程
   - 验证数据流转的正确性

3. **API接口测试**：从外部客户端视角验证API
   - 使用Postman测试各个接口的可用性
   - 验证各种边界条件和错误处理

## 5. 测试数据准备

运行测试前，需要准备以下测试数据：

1. 用户数据：至少需要admin、expert两个角色的用户
2. 项目数据：至少需要一个包含评分项的测试项目
3. 任务数据：至少需要一个活动状态的评审任务

可以使用SQL脚本 `_sql/init_sqlserver.sql` 初始化测试数据。

## 6. 常见问题解决

- **认证失败**：确认使用了正确的用户名密码，检查JWT令牌是否正确设置在请求头
- **404错误**：确认API路径正确，后端服务正常运行
- **500错误**：检查服务器日志，可能是代码异常或数据库连接问题
- **数据测试不通过**：检查测试数据的准备是否正确，数据格式是否符合要求 