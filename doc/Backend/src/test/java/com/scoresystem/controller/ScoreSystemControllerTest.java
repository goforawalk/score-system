package com.scoresystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoresystem.dto.ScoreSystemModels;
import com.scoresystem.dto.ScoreSystemModels.ApiResponse;
import com.scoresystem.dto.ScoreSystemModels.LoginRequest;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.dto.ScoreSystemModels.UserDTO;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.ScoreService;
import com.scoresystem.service.StatisticsService;
import com.scoresystem.service.TaskService;
import com.scoresystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScoreSystemController测试类
 * 
 * 测试说明：
 * 1. 使用@WebMvcTest进行控制器层测试
 * 2. 使用MockMvc模拟HTTP请求
 * 3. 使用@MockBean模拟服务层依赖
 * 4. 测试包括：用户登录、获取用户列表、保存用户、删除用户等API接口
 */
@WebMvcTest(ScoreSystemController.class)
public class ScoreSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private ScoreService scoreService;

    @MockBean
    private StatisticsService statisticsService;

    private UserDTO testUser;
    private ProjectDTO testProject;
    private TaskDTO testTask;
    private ScoreDTO testScore;

    @BeforeEach
    public void setUp() {
        // 创建测试用户
        testUser = new UserDTO();
        testUser.setUsername("testuser");
        testUser.setName("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setRole("user");
        testUser.setDepartment("测试部门");
        testUser.setToken("test-token");

        // 创建测试项目
        testProject = new ProjectDTO();
        testProject.setId(1L);
        testProject.setName("测试项目");
        testProject.setDescription("测试项目描述");

        // 创建测试任务
        testTask = new TaskDTO();
        testTask.setId(1L);
        testTask.setTaskId("TEST-TASK-001");
        testTask.setCategory("测试类别");
        testTask.setTaskType(1);
        testTask.setScoreGroupType(1);
        testTask.setStatus("active");

        // 创建测试评分
        testScore = new ScoreDTO();
        testScore.setId(1L);
        testScore.setProjectId(1L);
        testScore.setUsername("testuser");
        testScore.setTotalScore(85.5);
    }

    /**
     * 测试用户登录接口
     */
    @Test
    @DisplayName("测试用户登录接口")
    public void testLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        when(userService.login("testuser", "password123")).thenReturn(testUser);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    /**
     * 测试获取用户列表接口
     */
    @Test
    @DisplayName("测试获取用户列表接口")
    public void testGetUsers() throws Exception {
        List<UserDTO> users = Collections.singletonList(testUser);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取用户列表成功"))
                .andExpect(jsonPath("$.data[0].username").value("testuser"));
    }

    /**
     * 测试保存用户接口
     */
    @Test
    @DisplayName("测试保存用户接口")
    public void testSaveUser() throws Exception {
        when(userService.saveUser(any(UserDTO.class))).thenReturn(testUser);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("保存用户成功"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    /**
     * 测试删除用户接口
     */
    @Test
    @DisplayName("测试删除用户接口")
    public void testDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser("testuser");

        mockMvc.perform(delete("/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除用户成功"));
    }

    /**
     * 测试获取项目列表接口
     */
    @Test
    @DisplayName("测试获取项目列表接口")
    public void testGetProjects() throws Exception {
        List<ProjectDTO> projects = Collections.singletonList(testProject);
        when(projectService.getAllProjects()).thenReturn(projects);

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取项目列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    /**
     * 测试保存项目接口
     */
    @Test
    @DisplayName("测试保存项目接口")
    public void testSaveProject() throws Exception {
        when(projectService.saveProject(any(ProjectDTO.class))).thenReturn(testProject);

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("保存项目成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * 测试删除项目接口
     */
    @Test
    @DisplayName("测试删除项目接口")
    public void testDeleteProject() throws Exception {
        doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除项目成功"));
    }

    /**
     * 测试获取活动任务接口 - 有活动任务
     */
    @Test
    @DisplayName("测试获取活动任务接口 - 有活动任务")
    public void testGetActiveTasksWithActiveTask() throws Exception {
        when(taskService.getActiveTask()).thenReturn(testTask);
        
        List<ProjectDTO> projects = Collections.singletonList(testProject);
        when(projectService.getProjectsByTask(1L)).thenReturn(projects);

        mockMvc.perform(get("/tasks/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取活动任务及项目成功"))
                .andExpect(jsonPath("$.data.task.id").value(1))
                .andExpect(jsonPath("$.data.projectsInOrder[0].id").value(1));
    }

    /**
     * 测试获取活动任务接口 - 无活动任务
     */
    @Test
    @DisplayName("测试获取活动任务接口 - 无活动任务")
    public void testGetActiveTasksWithNoActiveTask() throws Exception {
        when(taskService.getActiveTask()).thenReturn(null);

        mockMvc.perform(get("/tasks/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("当前无活动任务"))
                .andExpect(jsonPath("$.data.task").isEmpty())
                .andExpect(jsonPath("$.data.projectsInOrder").isArray())
                .andExpect(jsonPath("$.data.projectsInOrder").isEmpty());
    }

    /**
     * 测试提交评分接口
     */
    @Test
    @DisplayName("测试提交评分接口")
    public void testSubmitScore() throws Exception {
        ScoreRequest scoreRequest = new ScoreRequest();
        scoreRequest.setProjectId(1L);
        scoreRequest.setUsername("testuser");
        scoreRequest.setTotalScore(85.5);

        when(scoreService.saveScore(any(ScoreRequest.class))).thenReturn(testScore);

        mockMvc.perform(post("/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scoreRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("提交评分成功"))
                .andExpect(jsonPath("$.data.totalScore").value(85.5));
    }

    /**
     * 测试获取评分历史接口
     */
    @Test
    @DisplayName("测试获取评分历史接口")
    public void testGetScoreHistory() throws Exception {
        List<ScoreDTO> scores = Collections.singletonList(testScore);
        when(scoreService.getScoreHistory(1L, "testuser")).thenReturn(scores);

        mockMvc.perform(get("/scores/history")
                .param("projectId", "1")
                .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取评分历史成功"))
                .andExpect(jsonPath("$.data[0].totalScore").value(85.5));
    }

    /**
     * 测试获取统计数据接口
     */
    @Test
    @DisplayName("测试获取统计数据接口")
    public void testGetStatistics() throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalProjects", 10);
        statistics.put("completedTasks", 5);
        
        when(statisticsService.getStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取统计数据成功"))
                .andExpect(jsonPath("$.data.totalProjects").value(10))
                .andExpect(jsonPath("$.data.completedTasks").value(5));
    }
}
