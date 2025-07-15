-- 插入测试用户
INSERT INTO users (username, password, name, role, email, department, create_time, update_time)
VALUES 
('admin', '$2a$10$8CRs.6.FKPNlvB1G9hZtTuOXvpRQCj0aOxbq5hTkf5KJGBzHiWEUa', '管理员', 'ADMIN', 'admin@example.com', '管理部', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user1', '$2a$10$8CRs.6.FKPNlvB1G9hZtTuOXvpRQCj0aOxbq5hTkf5KJGBzHiWEUa', '测试用户1', 'USER', 'user1@example.com', '技术部', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('expert1', '$2a$10$8CRs.6.FKPNlvB1G9hZtTuOXvpRQCj0aOxbq5hTkf5KJGBzHiWEUa', '专家用户1', 'EXPERT', 'expert1@example.com', '评审部', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试项目
INSERT INTO projects (id, name, description, status, creator, start_date, end_date, display_order, create_time, update_time)
VALUES 
(1, '测试项目1', '这是一个测试项目', 'ACTIVE', 'admin', '2025-01-01', '2025-12-31', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '测试项目2', '这是另一个测试项目', 'ACTIVE', 'admin', '2025-02-01', '2025-12-31', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试任务
INSERT INTO tasks (id, project_id, name, description, status, assigned_to, deadline, priority, create_time, update_time)
VALUES 
(1, 1, '测试任务1', '这是项目1的测试任务', 'TODO', 'user1', '2025-06-30', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, '测试任务2', '这是项目1的另一个测试任务', 'IN_PROGRESS', 'user1', '2025-07-15', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, '测试任务3', '这是项目2的测试任务', 'TODO', 'user1', '2025-06-30', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试评分项
INSERT INTO score_items (id, name, description, min_score, max_score, weight, display_order, create_time, update_time)
VALUES 
(1, '技术实现', '评估技术实现的质量', 0, 100, 0.3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '用户体验', '评估用户体验的质量', 0, 100, 0.3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '代码质量', '评估代码质量', 0, 100, 0.2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '文档质量', '评估文档质量', 0, 100, 0.2, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试评分
INSERT INTO scores (project_id, task_id, score_item_id, score, evaluator, comment, create_time, update_time)
VALUES 
(1, 1, 1, 85, 'expert1', '技术实现较好', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 1, 2, 90, 'expert1', '用户体验很好', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 1, 3, 75, 'expert1', '代码质量一般', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 1, 4, 80, 'expert1', '文档质量良好', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); 