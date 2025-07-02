package com.scoresystem.service.impl;

import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskService测试类
 */
@SpringBootTest
@ActiveProfiles("sqlserver")
@Transactional
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private Task testTask;
    private TaskDTO testTaskDTO;
    private User testUser;
    private Project testProject;
    
    @BeforeEach
    public void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testexpert");
        testUser.setPassword("password123");
        testUser.setName("测试专家");
        testUser.setRole("expert");
        testUser.setCreateTime(new Date());
        testUser.setUpdateTime(new Date());
        
        userRepository.insert(testUser);
        
        // 创建测试项目
        testProject = new Project();
        testProject.setName("测试项目");
        testProject.setDescription("这是一个测试项目");
        testProject.setStatus("ACTIVE");
        testProject.setDisplayOrder(1);
        testProject.setCreateTime(new Date());
        testProject.setUpdateTime(new Date());
        
        projectRepository.insert(testProject);
        
        // 创建测试任务
        testTask = new Task();
        testTask.setTaskId("TEST-TASK-001");
        testTask.setCategory("测试类别");
        testTask.setTaskType(1);
        testTask.setScoreGroupType(1);
        testTask.setStatus("draft");
        testTask.setStartTime(new Date());
        Date endTime = new Date();
        endTime.setTime(endTime.getTime() + 7 * 24 * 60 * 60 * 1000); // 一周后结束
        testTask.setEndTime(endTime);
        
        taskRepository.insert(testTask);
        
        // 创建任务-专家关联关系
        jdbcTemplate.update(
            "INSERT INTO task_experts (task_id, expert_username) VALUES (?, ?)",
            testTask.getId(), testUser.getUsername()
        );
        
        // 创建任务-项目关联关系
        jdbcTemplate.update(
            "INSERT INTO task_projects (task_id, project_id) VALUES (?, ?)",
            testTask.getId(), testProject.getId()
        );
        
        // 创建测试TaskDTO
        testTaskDTO = new TaskDTO();
        testTaskDTO.setTaskId("TEST-TASK-002");
        testTaskDTO.setCategory("新测试类别");
        testTaskDTO.setTaskType(2);
        testTaskDTO.setScoreGroupType(2);
        testTaskDTO.setStatus("draft");
        testTaskDTO.setStartTime(new Date());
        Date newEndTime = new Date();
        newEndTime.setTime(newEndTime.getTime() + 14 * 24 * 60 * 60 * 1000); // 两周后结束
        testTaskDTO.setEndTime(newEndTime);
        testTaskDTO.setExperts(Arrays.asList(testUser.getUsername()));
        
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(testProject.getId());
        testTaskDTO.setProjects(Arrays.asList(projectDTO));
    }
    
    /**
     * 测试获取所有任务
     */
    @Test
    @DisplayName("测试获取所有任务")
    public void testGetAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        
        assertNotNull(tasks, "任务列表不应为空");
        assertFalse(tasks.isEmpty(), "任务列表不应为空");
        
        // 验证测试任务在列表中
        boolean found = tasks.stream()
                .anyMatch(t -> t.getId().equals(testTask.getId()));
        assertTrue(found, "测试任务应该在任务列表中");
    }
    
    /**
     * 测试根据ID获取任务
     */
    @Test
    @DisplayName("测试根据ID获取任务")
    public void testGetTaskById() {
        TaskDTO task = taskService.getTaskById(testTask.getId());
        
        assertNotNull(task, "应该能找到测试任务");
        assertEquals(testTask.getId(), task.getId(), "任务ID应该匹配");
        assertEquals("TEST-TASK-001", task.getTaskId(), "任务编号应该匹配");
        assertEquals("测试类别", task.getCategory(), "任务类别应该匹配");
        
        // 验证关联的专家
        assertNotNull(task.getExperts(), "专家列表不应为空");
        assertEquals(1, task.getExperts().size(), "专家数量应该为1");
        assertEquals(testUser.getUsername(), task.getExperts().get(0), "专家用户名应该匹配");
        
        // 验证关联的项目
        assertNotNull(task.getProjects(), "项目列表不应为空");
        assertEquals(1, task.getProjects().size(), "项目数量应该为1");
        assertEquals(testProject.getId(), task.getProjects().get(0).getId(), "项目ID应该匹配");
    }
    
    /**
     * 测试保存新任务
     */
    @Test
    @DisplayName("测试保存新任务")
    public void testSaveNewTask() {
        TaskDTO savedTask = taskService.saveTask(testTaskDTO);
        
        assertNotNull(savedTask, "保存的任务不应为空");
        assertNotNull(savedTask.getId(), "新任务应该有ID");
        assertEquals("TEST-TASK-002", savedTask.getTaskId(), "任务编号应该匹配");
        assertEquals("新测试类别", savedTask.getCategory(), "任务类别应该匹配");
        
        // 验证任务已保存到数据库
        Task dbTask = taskRepository.selectById(savedTask.getId());
        assertNotNull(dbTask, "任务应该已保存到数据库");
        assertEquals("TEST-TASK-002", dbTask.getTaskId(), "数据库中的任务编号应该匹配");
        
        // 验证关联的专家
        List<String> experts = jdbcTemplate.queryForList(
                "SELECT expert_username FROM task_experts WHERE task_id = ?",
                String.class,
                savedTask.getId());
        assertNotNull(experts, "专家关联不应为空");
        assertEquals(1, experts.size(), "专家数量应该为1");
        assertEquals(testUser.getUsername(), experts.get(0), "专家用户名应该匹配");
        
        // 验证关联的项目
        List<Long> projectIds = jdbcTemplate.queryForList(
                "SELECT project_id FROM task_projects WHERE task_id = ?",
                Long.class,
                savedTask.getId());
        assertNotNull(projectIds, "项目关联不应为空");
        assertEquals(1, projectIds.size(), "项目数量应该为1");
        assertEquals(testProject.getId(), projectIds.get(0), "项目ID应该匹配");
    }
    
    /**
     * 测试更新现有任务
     */
    @Test
    @DisplayName("测试更新现有任务")
    public void testUpdateExistingTask() {
        // 修改测试任务信息
        testTaskDTO.setId(testTask.getId());
        testTaskDTO.setTaskId("TEST-TASK-UPDATED");
        testTaskDTO.setCategory("更新的测试类别");
        
        TaskDTO updatedTask = taskService.saveTask(testTaskDTO);
        
        assertNotNull(updatedTask, "更新的任务不应为空");
        assertEquals(testTask.getId(), updatedTask.getId(), "任务ID应该匹配");
        assertEquals("TEST-TASK-UPDATED", updatedTask.getTaskId(), "更新后的任务编号应该匹配");
        assertEquals("更新的测试类别", updatedTask.getCategory(), "更新后的任务类别应该匹配");
        
        // 验证任务已更新到数据库
        Task dbTask = taskRepository.selectById(testTask.getId());
        assertNotNull(dbTask, "任务应该存在于数据库中");
        assertEquals("TEST-TASK-UPDATED", dbTask.getTaskId(), "数据库中的任务编号应该已更新");
    }
    
    /**
     * 测试删除任务
     */
    @Test
    @DisplayName("测试删除任务")
    public void testDeleteTask() {
        taskService.deleteTask(testTask.getId());
        
        // 验证任务已从数据库中删除
        Task dbTask = taskRepository.selectById(testTask.getId());
        assertNull(dbTask, "任务应该已从数据库中删除");
        
        // 验证关联关系已删除
        List<String> experts = jdbcTemplate.queryForList(
                "SELECT expert_username FROM task_experts WHERE task_id = ?",
                String.class,
                testTask.getId());
        assertTrue(experts.isEmpty(), "专家关联应该已删除");
        
        List<Long> projectIds = jdbcTemplate.queryForList(
                "SELECT project_id FROM task_projects WHERE task_id = ?",
                Long.class,
                testTask.getId());
        assertTrue(projectIds.isEmpty(), "项目关联应该已删除");
    }
    
    /**
     * 测试获取用户相关的任务
     */
    @Test
    @DisplayName("测试获取用户相关的任务")
    public void testGetTasksByUser() {
        List<TaskDTO> tasks = taskService.getTasksByUser(testUser.getUsername());
        
        assertNotNull(tasks, "任务列表不应为空");
        assertFalse(tasks.isEmpty(), "任务列表不应为空");
        
        // 验证测试任务在列表中
        boolean found = tasks.stream()
                .anyMatch(t -> t.getId().equals(testTask.getId()));
        assertTrue(found, "测试任务应该在任务列表中");
    }
    
    /**
     * 测试启用评审任务
     */
    @Test
    @DisplayName("测试启用评审任务")
    public void testEnableTask() {
        TaskDTO enabledTask = taskService.enableTask(testTask.getId());
        
        assertNotNull(enabledTask, "启用的任务不应为空");
        assertEquals("active", enabledTask.getStatus(), "任务状态应该为active");
        
        // 验证任务状态已更新到数据库
        Task dbTask = taskRepository.selectById(testTask.getId());
        assertNotNull(dbTask, "任务应该存在于数据库中");
        assertEquals("active", dbTask.getStatus(), "数据库中的任务状态应该已更新");
    }
    
    /**
     * 测试完成评审任务
     */
    @Test
    @DisplayName("测试完成评审任务")
    public void testCompleteTask() {
        // 先启用任务
        taskService.enableTask(testTask.getId());
        
        TaskDTO completedTask = taskService.completeTask(testTask.getId(), testUser.getUsername());
        
        assertNotNull(completedTask, "完成的任务不应为空");
        assertEquals("completed", completedTask.getStatus(), "任务状态应该为completed");
        
        // 验证任务状态已更新到数据库
        Task dbTask = taskRepository.selectById(testTask.getId());
        assertNotNull(dbTask, "任务应该存在于数据库中");
        assertEquals("completed", dbTask.getStatus(), "数据库中的任务状态应该已更新");
    }
    
    /**
     * 测试获取当前活动任务
     */
    @Test
    @DisplayName("测试获取当前活动任务")
    public void testGetActiveTask() {
        // 先启用任务
        taskService.enableTask(testTask.getId());
        
        TaskDTO activeTask = taskService.getActiveTask();
        
        assertNotNull(activeTask, "应该能找到活动任务");
        assertEquals(testTask.getId(), activeTask.getId(), "任务ID应该匹配");
        assertEquals("active", activeTask.getStatus(), "任务状态应该为active");
    }
}
 