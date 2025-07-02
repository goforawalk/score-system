# 评分系统后端API文档

本文档描述了评分系统后端API的使用方法和部署步骤，用于前后端联调测试。

## 项目结构

```
src/
  ├── main/
  │   ├── java/
  │   │   └── com/
  │   │       └── scoresystem/
  │   │           ├── controller/
  │   │           │   └── ScoreSystemController.java  // API控制器
  │   │           ├── dto/
  │   │           │   └── ScoreSystemModels.java      // 数据传输对象
  │   │           ├── service/
  │   │           │   ├── UserService.java            // 用户服务
  │   │           │   ├── ProjectService.java         // 项目服务
  │   │           │   ├── TaskService.java            // 任务服务
  │   │           │   ├── ScoreService.java           // 评分服务
  │   │           │   └── StatisticsService.java      // 统计服务
  │   │           ├── model/
  │   │           │   ├── User.java                   // 用户实体
  │   │           │   ├── Project.java                // 项目实体
  │   │           │   ├── ScoreItem.java              // 评分项实体
  │   │           │   ├── Task.java                   // 任务实体
  │   │           │   └── Score.java                  // 评分实体
  │   │           ├── repository/
  │   │           │   ├── UserRepository.java         // 用户数据访问
  │   │           │   ├── ProjectRepository.java      // 项目数据访问
  │   │           │   ├── ScoreItemRepository.java    // 评分项数据访问
  │   │           │   ├── TaskRepository.java         // 任务数据访问
  │   │           │   └── ScoreRepository.java        // 评分数据访问
  │   │           └── ScoreSystemApplication.java     // 应用程序入口
  │   └── resources/
  │       └── application.properties                  // 应用配置文件
  └── test/                                           // 测试代码目录
pom.xml                                               // Maven配置文件
```

## 环境要求

- JDK 11+
- Maven 3.6+
- SQL Server 2008+

## 快速开始

### 1. 克隆代码

```bash
git clone https://github.com/your-org/score-system-backend.git
cd score-system-backend
```

### 2. 配置数据库

创建SQL Server数据库：

使用SQL Server Management Studio (SSMS)或其他SQL Server客户端工具执行`_sql/init_sqlserver.sql`脚本创建数据库和表结构。

修改`application.properties`中的数据库配置：

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=score_system;encrypt=false
spring.datasource.username=sa
spring.datasource.password=YourStrongPassword
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.jpa.database-platform=org.hibernate.dialect.SQLServer2008Dialect
```

更多SQL Server配置详情，请参考`_sql/README.md`文件。

### 3. 构建和运行

```bash
mvn clean package
java -jar target/score-system-backend-1.0.0.jar
```

或者使用Maven插件运行：

```bash
mvn spring-boot:run
```

应用将在 http://localhost:8088 上运行，API根路径为 http://localhost:8088/api

## API接口说明

### 认证相关接口

#### 用户登录

- **URL**: `/api/auth/login`
- **方法**: POST
- **请求体**:
  ```json
  {
    "username": "expert1",
    "password": "password"
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "message": "登录成功",
    "data": {
      "username": "expert1",
      "role": "expert",
      "token": "eyJhbGciOiJIUzI1NiJ9..."
    }
  }
  ```

#### 用户登出

- **URL**: `/api/auth/logout`
- **方法**: POST
- **响应**:
  ```json
  {
    "success": true,
    "message": "登出成功",
    "data": null
  }
  ```

### 用户管理相关接口

#### 获取用户列表

- **URL**: `/api/users`
- **方法**: GET
- **响应**:
  ```json
  {
    "success": true,
    "message": "获取用户列表成功",
    "data": [
      {
        "username": "admin",
        "role": "admin",
        "name": "管理员",
        "email": "admin@example.com",
        "department": "技术部"
      },
      {
        "username": "expert1",
        "role": "expert",
        "name": "专家1",
        "email": "expert1@example.com",
        "department": "评审部"
      }
    ]
  }
  ```

### 项目管理相关接口

#### 获取项目列表

- **URL**: `/api/projects`
- **方法**: GET
- **响应**:
  ```json
  {
    "success": true,
    "message": "获取项目列表成功",
    "data": [
      {
        "id": 1,
        "name": "项目A",
        "description": "这是项目A的描述",
        "status": "active",
        "createTime": "2023-06-01 10:00:00",
        "updateTime": "2023-06-01 10:00:00",
        "displayOrder": 1,
        "scoreItems": [
          {
            "id": 1,
            "name": "技术可行性",
            "description": "评估项目技术可行性",
            "weight": 0.3,
            "minScore": 0,
            "maxScore": 100,
            "roles": ["expert1", "expert2"]
          },
          {
            "id": 2,
            "name": "市场前景",
            "description": "评估项目市场前景",
            "weight": 0.3,
            "minScore": 0,
            "maxScore": 100,
            "roles": ["expert1"]
          },
          {
            "id": 3,
            "name": "团队能力",
            "description": "评估项目团队能力",
            "weight": 0.4,
            "minScore": 0,
            "maxScore": 100,
            "roles": ["expert2"]
          }
        ]
      }
    ]
  }
  ```

### 评审任务相关接口

#### 获取当前活动任务及项目

- **URL**: `/api/tasks/active`
- **方法**: GET
- **响应**:
  ```json
  {
    "success": true,
    "message": "获取活动任务及项目成功",
    "data": {
      "task": {
        "id": "task-001",
        "category": "2023年第一季度项目评审",
        "taskType": 2,
        "scoreGroupType": 1,
        "status": "active",
        "startTime": "2023-06-01 10:00:00",
        "endTime": "2023-06-10 18:00:00",
        "experts": ["expert1", "expert2"]
      },
      "projectsInOrder": [
        {
          "id": 1,
          "name": "项目A",
          "description": "这是项目A的描述",
          "status": "active",
          "scoreItems": [
            {
              "id": 1,
              "name": "技术可行性",
              "description": "评估项目技术可行性",
              "weight": 0.3,
              "minScore": 0,
              "maxScore": 100,
              "roles": ["expert1", "expert2"]
            },
            {
              "id": 2,
              "name": "市场前景",
              "description": "评估项目市场前景",
              "weight": 0.3,
              "minScore": 0,
              "maxScore": 100,
              "roles": ["expert1"]
            }
          ]
        },
        {
          "id": 2,
          "name": "项目B",
          "description": "这是项目B的描述",
          "status": "active",
          "scoreItems": [
            {
              "id": 1,
              "name": "技术可行性",
              "description": "评估项目技术可行性",
              "weight": 0.3,
              "minScore": 0,
              "maxScore": 100,
              "roles": ["expert1", "expert2"]
            },
            {
              "id": 3,
              "name": "团队能力",
              "description": "评估项目团队能力",
              "weight": 0.4,
              "minScore": 0,
              "maxScore": 100,
              "roles": ["expert2"]
            }
          ]
        }
      ]
    }
  }
  ```

### 评分相关接口

#### 提交评分

- **URL**: `/api/scores`
- **方法**: POST
- **请求体**:
  ```json
  {
    "projectId": 1,
    "username": "expert1",
    "scores": {
      "1": 85,
      "2": 90
    },
    "totalScore": 87.5,
    "comments": "这是一个很好的项目",
    "isDraft": false
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "message": "提交评分成功",
    "data": {
      "id": 1,
      "projectId": 1,
      "username": "expert1",
      "scores": {
        "1": 85,
        "2": 90
      },
      "totalScore": 87.5,
      "comments": "这是一个很好的项目",
      "createTime": "2023-06-05 14:30:00",
      "updateTime": "2023-06-05 14:30:00",
      "isDraft": false
    }
  }
  ```

#### 获取评分历史

- **URL**: `/api/scores/history`
- **方法**: GET
- **参数**:
  - `projectId`: 项目ID
  - `username`: 用户名
- **响应**:
  ```json
  {
    "success": true,
    "message": "获取评分历史成功",
    "data": [
      {
        "id": 1,
        "projectId": 1,
        "username": "expert1",
        "scores": {
          "1": 80,
          "2": 85
        },
        "totalScore": 82.5,
        "comments": "初步评分",
        "createTime": "2023-06-04 10:30:00",
        "updateTime": "2023-06-04 10:30:00",
        "isDraft": true
      },
      {
        "id": 2,
        "projectId": 1,
        "username": "expert1",
        "scores": {
          "1": 85,
          "2": 90
        },
        "totalScore": 87.5,
        "comments": "这是一个很好的项目",
        "createTime": "2023-06-05 14:30:00",
        "updateTime": "2023-06-05 14:30:00",
        "isDraft": false
      }
    ]
  }
  ```

## 角色权限验证

系统根据用户角色过滤评分项，确保用户只能看到和评分他们有权限的评分项。

前端代码中的过滤逻辑：

```javascript
const filteredScoreItems = Array.isArray(project.scoreItems) ? project.scoreItems.filter(item =>
    item.roles && item.roles.includes(currentUser.username)
) : [];
```

后端实现类似的逻辑，确保数据安全：

```java
public List<ScoreItemDTO> getScoreItemsByUserRole(Long projectId, String username) {
    ProjectDTO project = getProjectById(projectId);
    if (project == null || project.getScoreItems() == null) {
        return new ArrayList<>();
    }
    
    return project.getScoreItems().stream()
            .filter(item -> item.getRoles() != null && item.getRoles().contains(username))
            .collect(Collectors.toList());
}
```

## 测试数据

系统初始化时会创建以下测试数据：

### 用户

1. 管理员：admin/admin123
2. 专家1：expert1/expert123
3. 专家2：expert2/expert123

### 项目

1. 项目A - 包含3个评分项，分配给expert1和expert2
2. 项目B - 包含2个评分项，分配给expert1
3. 项目C - 包含2个评分项，分配给expert2

### 任务

1. 任务1 - 2023年第一季度项目评审，分配给expert1和expert2

## 常见问题

### 跨域问题

如果前端访问API时遇到跨域问题，请确保后端CORS配置正确：

```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .maxAge(3600);
        }
    };
}
```

### 认证失败

如果遇到认证失败的问题，请检查：

1. 用户名和密码是否正确
2. 请求头中是否包含正确的Authorization头
3. Token是否过期

## 联系方式

如有问题，请联系：

- 邮箱：support@score-system.com
- 电话：123-456-7890