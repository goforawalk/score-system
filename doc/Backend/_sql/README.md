# SQL Server 2008 数据库配置指南

## 前置条件

1. 安装 SQL Server 2008
2. 安装 SQL Server Management Studio (SSMS) 或其他 SQL Server 客户端工具

## 配置步骤

### 1. 创建数据库和表结构

1. 打开 SQL Server Management Studio 并连接到您的 SQL Server 2008 实例
2. 打开 `init_sqlserver.sql` 脚本文件
3. 执行脚本创建数据库和表结构

### 2. 配置应用程序连接

在 `application.properties` 文件中已经配置了 SQL Server 2008 的连接参数：

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=score_system;encrypt=false
spring.datasource.username=sa
spring.datasource.password=YourStrongPassword
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.jpa.database-platform=org.hibernate.dialect.SQLServer2008Dialect
```

请根据您的实际环境修改以下参数：

- `localhost:1433` - SQL Server 的主机名和端口
- `sa` - SQL Server 的用户名
- `YourStrongPassword` - SQL Server 的密码

### 3. SQL Server 2008 身份验证配置

确保 SQL Server 2008 已启用混合身份验证模式（SQL Server 和 Windows 身份验证），并且 sa 账户已启用。

步骤：
1. 右键点击SQL Server实例 -> 属性 -> 安全性
2. 选择"SQL Server 和 Windows 身份验证模式"
3. 重启SQL Server服务

### 4. 防火墙配置

如果 SQL Server 与应用程序不在同一台机器上，请确保已在防火墙中开放 SQL Server 的端口（默认为 1433）。

### 5. SQL Server 2008 特定注意事项

1. **加密连接**：SQL Server 2008 对加密连接的支持有限，我们已在连接字符串中添加 `encrypt=false` 参数
2. **JDBC驱动**：我们使用的是 `mssql-jdbc 6.4.0.jre8` 驱动，该版本支持 SQL Server 2008
3. **Hibernate方言**：配置使用 `SQLServer2008Dialect` 以确保兼容性

## 故障排除

### 常见连接问题

1. **无法连接到 SQL Server 2008**
   - 检查 SQL Server 服务是否正在运行
   - 验证连接字符串中的主机名和端口是否正确
   - 确认 SQL Server 已启用 TCP/IP 协议（使用SQL Server配置管理器）

2. **登录失败**
   - 验证用户名和密码是否正确
   - 确认 SQL Server 已启用混合身份验证模式
   - 检查用户是否有访问数据库的权限

3. **加密连接问题**
   - 如果遇到 SSL 加密相关的错误，可以在连接字符串中添加 `encrypt=false` 参数（已在配置中添加）

4. **数据库不存在**
   - 确保已执行 `init_sqlserver.sql` 脚本创建数据库和表结构

5. **SQL Server 2008 特有问题**
   - 如果遇到 "不支持此版本的 SQL Server" 错误，请确保使用兼容的JDBC驱动版本
   - 如果遇到 JPA 相关错误，请确保使用正确的方言 `SQLServer2008Dialect` 