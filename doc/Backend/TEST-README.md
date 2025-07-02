# 评分系统后端测试指南

本文档提供了评分系统后端测试的详细说明，包括配置、运行和排查常见问题。

## 测试环境准备

### 1. 配置测试环境

测试使用独立的配置文件 `application-test.properties`，主要配置如下：

```properties
# 数据库连接
spring.datasource.url=jdbc:sqlserver://192.168.9.243:1433;databaseName=score_system;encrypt=false;trustServerCertificate=true;sendTimeAsDateTime=false
spring.datasource.username=sa
spring.datasource.password=admin@123

# 日志级别
logging.level.com.scoresystem=DEBUG
logging.level.org.springframework.jdbc=DEBUG
```

### 2. 安装开发工具

确保已安装：
- JDK 8+
- Maven 3.6+
- IDE（如Eclipse、IntelliJ IDEA）

## 测试类说明

项目包含以下关键测试类：

### 基础连接测试

**DbConnectionTest**: 验证数据库连接基本功能
- `testConnection()`: 测试数据库连接并输出数据库元数据
- `testQuery()`: 测试简单查询功能
- `testTableStructure()`: 验证数据库表结构

### 数据库操作测试

**DatabaseConnectionTest**: 验证用户表的CRUD操作
- `testDatabaseConnection()`: 测试查询所有用户
- `testCrudOperations()`: 测试用户增删改查操作（带事务回滚）

### 事务测试

**TransactionTest**: 验证事务操作功能
- `testInsert()`: 测试插入操作
- `testUpdate()`: 测试更新操作
- `testDelete()`: 测试删除操作
- `testRawSql()`: 测试原生SQL操作

## 运行测试

### 使用Maven运行

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DbConnectionTest

# 运行特定测试方法
mvn test -Dtest=DatabaseConnectionTest#testDatabaseConnection
```

### 在IDE中运行

1. 导入项目到IDE
2. 右键点击测试类或测试方法
3. 选择"Run as > JUnit Test"

## 事务和数据隔离说明

所有测试方法使用 `@Transactional` 注解确保测试数据不会污染数据库：

```java
@Test
@Transactional
public void testCrudOperations() {
    // 测试代码...
}
```

测试完成后会自动回滚所有数据库操作，不会影响实际数据。

## 常见问题解决

### 1. LocalDateTime转换错误

**问题**: 
```
The conversion to class java.time.LocalDateTime is unsupported
```

**解决方案**:
- 在JDBC URL中添加 `sendTimeAsDateTime=false` 参数
- 确保已正确配置 `MyBatisPlusConfig.java` 中的类型处理器

### 2. 连接重置错误

**问题**:
```
Connection reset ClientConnectionId:xxx
```

**解决方案**:
- 检查网络连接
- 确认SQL Server配置允许远程连接
- 添加 `encrypt=false;trustServerCertificate=true` 到连接URL

### 3. 身份验证失败

**问题**:
```
Login failed for user 'sa'
```

**解决方案**:
- 确认用户名密码正确
- 确认SQL Server已启用混合验证模式
- 检查用户权限设置

## 日志和调试

测试过程中可以查看以下日志获取更多信息：

1. 应用日志: `/logs/score-system.log`
2. 控制台输出: 包含MyBatis SQL语句和参数
3. 数据库连接池日志: 通过设置 `logging.level.com.zaxxer.hikari=DEBUG` 