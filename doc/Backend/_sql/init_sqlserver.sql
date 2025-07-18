-- 创建数据库
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'score_system')
BEGIN
    CREATE DATABASE score_system;
END
GO

USE score_system;
GO

-- 用户表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[users]') AND type in (N'U'))
BEGIN
    CREATE TABLE users (
        username VARCHAR(50) PRIMARY KEY,
        password VARCHAR(255) NOT NULL,
        role VARCHAR(20) NOT NULL,
        name VARCHAR(50) NOT NULL,
        email VARCHAR(100),
        department VARCHAR(50),
        create_time DATETIME,
        update_time DATETIME,
        last_login_time DATETIME
    );
END
GO

-- 项目表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[projects]') AND type in (N'U'))
BEGIN
    CREATE TABLE projects (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(500),
        status VARCHAR(20) NOT NULL,
        display_order INT,
        create_time DATETIME,
        update_time DATETIME
    );
END
GO

-- 评分项表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[score_items]') AND type in (N'U'))
BEGIN
    CREATE TABLE score_items (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(500),
        weight FLOAT,
        min_score INT,
        max_score INT,
        project_id BIGINT,
        FOREIGN KEY (project_id) REFERENCES projects(id)
    );
END
GO

-- 评分项角色表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[score_item_roles]') AND type in (N'U'))
BEGIN
    CREATE TABLE score_item_roles (
        score_item_id BIGINT,
        role VARCHAR(20),
        PRIMARY KEY (score_item_id, role),
        FOREIGN KEY (score_item_id) REFERENCES score_items(id)
    );
END
GO

-- 任务表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[tasks]') AND type in (N'U'))
BEGIN
    CREATE TABLE tasks (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        task_id VARCHAR(50),
        category VARCHAR(50),
        task_type INT,
        score_group_type INT,
        status VARCHAR(20),
        start_time DATETIME,
        end_time DATETIME
    );
END
GO

-- 任务专家表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[task_experts]') AND type in (N'U'))
BEGIN
    CREATE TABLE task_experts (
        task_id BIGINT,
        expert_username VARCHAR(50),
        completed BIT DEFAULT 0,
        completion_time DATETIME,
        PRIMARY KEY (task_id, expert_username),
        FOREIGN KEY (task_id) REFERENCES tasks(id),
        FOREIGN KEY (expert_username) REFERENCES users(username)
    );
END
GO

-- 任务项目表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[task_projects]') AND type in (N'U'))
BEGIN
    CREATE TABLE task_projects (
        task_id BIGINT,
        project_id BIGINT,
        PRIMARY KEY (task_id, project_id),
        FOREIGN KEY (task_id) REFERENCES tasks(id),
        FOREIGN KEY (project_id) REFERENCES projects(id)
    );
END
GO

-- 评分表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[scores]') AND type in (N'U'))
BEGIN
    CREATE TABLE scores (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        project_id BIGINT,
        task_id BIGINT,
        user_id VARCHAR(50),
        total_score FLOAT,
        comments VARCHAR(1000),
        create_time DATETIME,
        update_time DATETIME,
        is_draft BIT,
        FOREIGN KEY (project_id) REFERENCES projects(id),
        FOREIGN KEY (task_id) REFERENCES tasks(id),
        FOREIGN KEY (user_id) REFERENCES users(username)
    );
END
GO

-- 评分详情表
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[score_details]') AND type in (N'U'))
BEGIN
    CREATE TABLE score_details (
        score_id BIGINT,
        score_item_id BIGINT,
        score_value INT,
        PRIMARY KEY (score_id, score_item_id),
        FOREIGN KEY (score_id) REFERENCES scores(id),
        FOREIGN KEY (score_item_id) REFERENCES score_items(id)
    );
END
GO

-- 初始化管理员账户
IF NOT EXISTS (SELECT * FROM users WHERE username = 'admin')
BEGIN
    INSERT INTO users (username, password, role, name, create_time)
    VALUES ('admin', '$2a$10$qA9GQFgE7BHlVoOH.Zb9SeZGMIq5pQ0xc0lBEDGfgRUFOaQNz7lZm', 'ADMIN', '系统管理员', GETDATE());
END
GO

-- 数据库迁移：为 scores 表添加 task_id 字段（如果不存在）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[scores]') AND name = 'task_id')
BEGIN
    ALTER TABLE scores ADD task_id BIGINT;
    ALTER TABLE scores ADD CONSTRAINT FK_scores_task_id FOREIGN KEY (task_id) REFERENCES tasks(id);
END
GO

-- 数据库迁移：为 task_experts 表添加 completed 和 completion_time 字段（如果不存在）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[task_experts]') AND name = 'completed')
BEGIN
    ALTER TABLE task_experts ADD completed BIT DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[task_experts]') AND name = 'completion_time')
BEGIN
    ALTER TABLE task_experts ADD completion_time DATETIME;
END
GO
