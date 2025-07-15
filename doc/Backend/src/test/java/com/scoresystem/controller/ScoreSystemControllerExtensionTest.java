package com.scoresystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.ScoreService;
import com.scoresystem.service.StatisticsService;
import com.scoresystem.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScoreSystemControllerExtension测试类
 * 
 * 测试说明：
 * 1. 使用@WebMvcTest进行控制器层测试
 * 2. 使用MockMvc模拟HTTP请求
 * 3. 使用@MockBean模拟服务层依赖
 * 4. 测试包括：批量更新项目、任务管理、评分扩展接口等功能
 */
@WebMvcTest(ScoreSystemControllerExtension.class)
public class ScoreSystemControllerExtensionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private ScoreService scoreService;

    @MockBean
    private StatisticsService statisticsService;

    private ProjectDTO testProject;
    private TaskDTO testTask;
    private ScoreDTO testScore;

    @BeforeEach
    public void setUp() {
        // 创建测试项目
        testProject = new ProjectDTO();
        testProject.setId(1L);
        testProject.setName("测试项目");
        testProject.setDescription("测试项目描述");
        testProject.setStatus("active");

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
     * 测试批量更新项目状态接口
     */
    @Test
    @DisplayName("测试批量更新项目状态接口")
    public void testBatchUpdateProjects() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("projectIds", Arrays.asList(1, 2, 3));
        request.put("status", "completed");

        doNothing().when(projectService).batchUpdateStatus(anyList(), anyString());

        mockMvc.perform(put("/projects/batch-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("批量更新项目状态成功"));
    }

    /**
     * 测试批量删除项目接口
     */
    @Test
    @DisplayName("测试批量删除项目接口")
    public void testBatchDeleteProjects() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("projectIds", Arrays.asList(1, 2, 3));

        doNothing().when(projectService).batchDelete(anyList());

        mockMvc.perform(post("/projects/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("批量删除项目成功"));
    }

    /**
     * 测试更新项目顺序接口
     */
    @Test
    @DisplayName("测试更新项目顺序接口")
    public void testUpdateProjectsOrder() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("projectIds", Arrays.asList(3, 1, 2));

        doNothing().when(projectService).updateOrder(anyList());

        mockMvc.perform(put("/projects/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新项目顺序成功"));
    }

    /**
     * 测试获取项目评分进度接口
     */
    @Test
    @DisplayName("测试获取项目评分进度接口")
    public void testGetProjectProgress() throws Exception {
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalExperts", 5);
        progress.put("completedExperts", 3);
        progress.put("completionPercentage", 60.0);

        when(projectService.getProjectProgress(1L, null)).thenReturn(progress);

        mockMvc.perform(get("/projects/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取项目评分进度成功"))
                .andExpect(jsonPath("$.data.completionPercentage").value(60.0));
    }

    /**
     * 测试获取项目评分详情接口
     */
    @Test
    @DisplayName("测试获取项目评分详情接口")
    public void testGetProjectScores() throws Exception {
        Map<String, Object> projectScores = new HashMap<>();
        projectScores.put("project", testProject);
        
        Map<String, Object> scoreStatistics = new HashMap<>();
        scoreStatistics.put("totalScores", 5);
        scoreStatistics.put("averageScore", 85.5);
        projectScores.put("scoreStatistics", scoreStatistics);
        
        Map<String, Object> scoreProgress = new HashMap<>();
        scoreProgress.put("totalExperts", 10);
        scoreProgress.put("completedExperts", 5);
        scoreProgress.put("completionPercentage", 50.0);
        projectScores.put("scoreProgress", scoreProgress);
        
        when(projectService.getProjectScores(1L, null)).thenReturn(projectScores);

        mockMvc.perform(get("/projects/1/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取项目评分详情成功"))
                .andExpect(jsonPath("$.data.project.id").value(1))
                .andExpect(jsonPath("$.data.scoreStatistics.totalScores").value(5))
                .andExpect(jsonPath("$.data.scoreProgress.completionPercentage").value(50.0));
    }

    /**
     * 测试获取项目评分详情接口（指定任务）
     */
    @Test
    @DisplayName("测试获取项目评分详情接口（指定任务）")
    public void testGetProjectScores_WithTaskId() throws Exception {
        Map<String, Object> projectScores = new HashMap<>();
        projectScores.put("project", testProject);
        Map<String, Object> scoreStatistics = new HashMap<>();
        scoreStatistics.put("totalScores", 3);
        scoreStatistics.put("averageScore", 88.0);
        projectScores.put("scoreStatistics", scoreStatistics);
        Map<String, Object> scoreProgress = new HashMap<>();
        scoreProgress.put("totalExperts", 8);
        scoreProgress.put("completedExperts", 3);
        scoreProgress.put("completionPercentage", 37.5);
        projectScores.put("scoreProgress", scoreProgress);
        
        when(projectService.getProjectScores(1L, 2L)).thenReturn(projectScores);

        mockMvc.perform(get("/projects/1/scores")
                .param("taskId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取项目评分详情成功"))
                .andExpect(jsonPath("$.data.scoreStatistics.totalScores").value(3))
                .andExpect(jsonPath("$.data.scoreStatistics.averageScore").value(88.0));
    }

    /**
     * 测试获取项目评分项接口
     */
    @Test
    @DisplayName("测试获取项目评分项接口")
    public void testGetProjectScoreItems() throws Exception {
        ScoreItemDTO item1 = new ScoreItemDTO();
        item1.setId(1L);
        item1.setName("评分项1");
        item1.setDescription("描述1");
        item1.setMaxScore(100);
        item1.setGroupType("0");
        item1.setWeight(1.0);
        item1.setDisplayOrder(1);

        ScoreItemDTO item2 = new ScoreItemDTO();
        item2.setId(2L);
        item2.setName("评分项2");
        item2.setDescription("描述2");
        item2.setMaxScore(100);
        item2.setGroupType("0");
        item2.setWeight(1.0);
        item2.setDisplayOrder(2);

        List<ScoreItemDTO> scoreItems = Arrays.asList(item1, item2);
        
        when(projectService.getScoreItemsByProjectId(1L)).thenReturn(scoreItems);

        mockMvc.perform(get("/projects/1/score-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取项目评分项成功"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("评分项1"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("评分项2"));
    }

    /**
     * 测试获取任务详情接口
     */
    @Test
    @DisplayName("测试获取任务详情接口")
    public void testGetTask() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(testTask);

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取任务详情成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * 测试创建评审任务接口
     */
    @Test
    @DisplayName("测试创建评审任务接口")
    public void testCreateTask() throws Exception {
        when(taskService.saveTask(any(TaskDTO.class))).thenReturn(testTask);

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("创建评审任务成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * 测试更新评审任务接口
     */
    @Test
    @DisplayName("测试更新评审任务接口")
    public void testUpdateTask() throws Exception {
        when(taskService.saveTask(any(TaskDTO.class))).thenReturn(testTask);

        mockMvc.perform(put("/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新评审任务成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * 测试启用评审任务接口
     */
    @Test
    @DisplayName("测试启用评审任务接口")
    public void testEnableTask() throws Exception {
        when(taskService.enableTask(1L)).thenReturn(testTask);

        mockMvc.perform(put("/tasks/1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("启用评审任务成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * 测试完成评审任务接口
     */
    @Test
    @DisplayName("测试完成评审任务接口")
    public void testCompleteTask() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");

        when(taskService.completeTask(eq(1L), eq("testuser"))).thenReturn(testTask);

        mockMvc.perform(put("/tasks/1/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("完成评审任务成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * 测试获取所有评分记录接口
     */
    @Test
    @DisplayName("测试获取所有评分记录接口")
    public void testGetScores() throws Exception {
        List<ScoreDTO> scores = Collections.singletonList(testScore);
        when(scoreService.getAllScores()).thenReturn(scores);

        mockMvc.perform(get("/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取所有评分记录成功"))
                .andExpect(jsonPath("$.data[0].totalScore").value(85.5));
    }

    /**
     * 测试按项目获取评分接口
     */
    @Test
    @DisplayName("测试按项目获取评分接口")
    public void testGetScoresByProject() throws Exception {
        List<ScoreDTO> scores = Collections.singletonList(testScore);
        when(scoreService.getScoresByProject(1L)).thenReturn(scores);

        mockMvc.perform(get("/scores/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("按项目获取评分成功"))
                .andExpect(jsonPath("$.data[0].totalScore").value(85.5));
    }

    /**
     * 测试按用户获取评分接口
     */
    @Test
    @DisplayName("测试按用户获取评分接口")
    public void testGetScoresByUser() throws Exception {
        List<ScoreDTO> scores = Collections.singletonList(testScore);
        when(scoreService.getScoresByUser("testuser")).thenReturn(scores);

        mockMvc.perform(get("/scores/user/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("按用户获取评分成功"))
                .andExpect(jsonPath("$.data[0].totalScore").value(85.5));
    }

    /**
     * 测试获取仪表盘统计数据接口
     */
    @Test
    @DisplayName("测试获取仪表盘统计数据接口")
    public void testGetDashboardStatistics() throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalProjects", 10);
        statistics.put("activeTasks", 2);
        
        when(statisticsService.getDashboardStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/statistics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取仪表盘统计数据成功"))
                .andExpect(jsonPath("$.data.totalProjects").value(10));
    }

    /**
     * 测试获取项目统计数据接口
     */
    @Test
    @DisplayName("测试获取项目统计数据接口")
    public void testGetProjectStatistics() throws Exception {
        List<Map<String, Object>> statistics = new ArrayList<>();
        Map<String, Object> stat = new HashMap<>();
        stat.put("projectName", "测试项目");
        stat.put("averageScore", 85.5);
        statistics.add(stat);
        
        when(statisticsService.getProjectStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/statistics/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取项目统计数据成功"))
                .andExpect(jsonPath("$.data[0].projectName").value("测试项目"));
    }
}
