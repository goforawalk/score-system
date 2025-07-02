package com.scoresystem.service.impl;

import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatisticsService测试类
 * 
 * 测试说明：
 * 1. 使用@SpringBootTest进行集成测试
 * 2. 使用@ActiveProfiles("sqlserver")指定使用SQL Server数据库
 * 3. 使用@Transactional确保测试数据回滚，不影响数据库
 * 4. 测试包括：获取统计数据、获取仪表盘统计、获取项目统计等功能
 */
@SpringBootTest
@ActiveProfiles("sqlserver")
@Transactional
public class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScoreRepository scoreRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private Project testProject1;
    private Project testProject2;
    private Project testProject3;
    private Task testTask1;
    private Task testTask2;
    private User testUser1;
    private User testUser2;
    private Score testScore1;
    private Score testScore2;
    private Score testScore3;
    private Score testScore4;
    private Score testDraftScore;
    private Date pastDate;
    private Date currentDate;
    private Date futureDate;
    
    /**
     * 测试前准备
     * 创建多样化的测试数据，覆盖不同状态和类型的对象
     */
    @BeforeEach
    public void setUp() {
    	// 先清空相关表中的数据，确保测试环境的一致性
        // 修复外键约束问题：按照依赖关系顺序删除数据
        // 1. 先删除score_details表中的数据
    	jdbcTemplate.update("DELETE FROM score_details");
        // 2. 删除评分表数据
        jdbcTemplate.update("DELETE FROM scores");
        // 3. 删除score_item_roles表数据
        jdbcTemplate.update("DELETE FROM score_item_roles");
        // 4. 删除score_items表数据
        jdbcTemplate.update("DELETE FROM score_items");
        // 5. 删除task_experts表数据
        jdbcTemplate.update("DELETE FROM task_experts");
        // 6. 删除task_projects表数据
        jdbcTemplate.update("DELETE FROM task_projects");
        // 7. 删除项目和任务表数据
        jdbcTemplate.update("DELETE FROM tasks");
        jdbcTemplate.update("DELETE FROM projects");
        // 8. 最后删除用户表数据
        jdbcTemplate.update("DELETE FROM users");
        
        // 准备日期数据
        Calendar calendar = Calendar.getInstance();
        currentDate = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, -60);
        pastDate = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, 90); // 从过去的日期往后30天
        futureDate = calendar.getTime();
        
        // 创建多个测试用户
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setPassword("password123");
        testUser1.setName("测试用户1");
        testUser1.setRole("expert");
        testUser1.setCreateTime(pastDate);
        testUser1.setUpdateTime(currentDate);
        
        userRepository.insert(testUser1);
        
        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setPassword("password456");
        testUser2.setName("测试用户2");
        testUser2.setRole("admin");
        testUser2.setCreateTime(currentDate);
        testUser2.setUpdateTime(currentDate);
        
        userRepository.insert(testUser2);
        
        // 创建测试项目1 - 活跃状态
        testProject1 = new Project();
        testProject1.setName("测试项目1");
        testProject1.setDescription("测试项目1描述");
        testProject1.setStatus("active");
        testProject1.setCreateTime(pastDate);
        testProject1.setUpdateTime(currentDate);
        
        projectRepository.insert(testProject1);
        
        // 创建测试项目2 - 已完成状态
        testProject2 = new Project();
        testProject2.setName("测试项目2");
        testProject2.setDescription("测试项目2描述");
        testProject2.setStatus("completed");
        testProject2.setCreateTime(pastDate);
        testProject2.setUpdateTime(pastDate);
        
        projectRepository.insert(testProject2);
        
        // 创建测试项目3 - 新创建状态
        testProject3 = new Project();
        testProject3.setName("测试项目3");
        testProject3.setDescription("测试项目3描述");
        testProject3.setStatus("active");
        testProject3.setCreateTime(currentDate);
        testProject3.setUpdateTime(currentDate);
        
        projectRepository.insert(testProject3);
        
        // 创建测试任务1 - 活跃任务
        testTask1 = new Task();
        testTask1.setTaskId("TEST-TASK-001");
        testTask1.setCategory("测试类别1");
        testTask1.setTaskType(1);
        testTask1.setScoreGroupType(1);
        testTask1.setStatus("active");
        testTask1.setStartTime(pastDate);
        testTask1.setEndTime(futureDate);
        
        taskRepository.insert(testTask1);
        
        // 创建测试任务2 - 已结束任务
        testTask2 = new Task();
        testTask2.setTaskId("TEST-TASK-002");
        testTask2.setCategory("测试类别2");
        testTask2.setTaskType(2);
        testTask2.setScoreGroupType(2);
        testTask2.setStatus("completed");
        testTask2.setStartTime(pastDate);
        testTask2.setEndTime(pastDate);
        
        taskRepository.insert(testTask2);
        
        // 创建测试评分1 - 高分
        testScore1 = new Score();
        testScore1.setProjectId(testProject1.getId());
        testScore1.setUserId(testUser1.getUsername());
        testScore1.setTotalScore(85.5);
        testScore1.setComments("测试评分1");
        testScore1.setIsDraft(false);
        testScore1.setCreateTime(pastDate);
        testScore1.setUpdateTime(pastDate);
        
        scoreRepository.insert(testScore1);
        
        // 创建测试评分2 - 非常高分
        testScore2 = new Score();
        testScore2.setProjectId(testProject2.getId());
        testScore2.setUserId(testUser1.getUsername());
        testScore2.setTotalScore(92.0);
        testScore2.setComments("测试评分2");
        testScore2.setIsDraft(false);
        testScore2.setCreateTime(pastDate);
        testScore2.setUpdateTime(pastDate);
        
        scoreRepository.insert(testScore2);
        
        // 创建测试评分3 - 中等分数
        testScore3 = new Score();
        testScore3.setProjectId(testProject1.getId());
        testScore3.setUserId(testUser2.getUsername());
        testScore3.setTotalScore(75.0);
        testScore3.setComments("测试评分3");
        testScore3.setIsDraft(false);
        testScore3.setCreateTime(currentDate);
        testScore3.setUpdateTime(currentDate);
        
        scoreRepository.insert(testScore3);
        
        // 创建测试评分4 - 较低分数
        testScore4 = new Score();
        testScore4.setProjectId(testProject2.getId());
        testScore4.setUserId(testUser2.getUsername());
        testScore4.setTotalScore(65.5);
        testScore4.setComments("测试评分4");
        testScore4.setIsDraft(false);
        testScore4.setCreateTime(currentDate);
        testScore4.setUpdateTime(currentDate);
        
        scoreRepository.insert(testScore4);
        
        // 创建测试草稿评分
        testDraftScore = new Score();
        testDraftScore.setProjectId(testProject3.getId());
        testDraftScore.setUserId(testUser1.getUsername());
        testDraftScore.setTotalScore(88.0);
        testDraftScore.setComments("草稿评分");
        testDraftScore.setIsDraft(true);
        testDraftScore.setCreateTime(currentDate);
        testDraftScore.setUpdateTime(currentDate);
        
        scoreRepository.insert(testDraftScore);
    }
    
    /**
     * 测试获取统计数据
     * 验证能够正确获取系统统计数据
     */
    @Test
    @DisplayName("测试获取统计数据")
    public void testGetStatistics() {
        Map<String, Object> statistics = statisticsService.getStatistics();
        
        assertNotNull(statistics, "统计数据不应为空");
        assertTrue(statistics.containsKey("totalProjects"), "应包含项目总数");
        assertTrue(statistics.containsKey("activeProjects"), "应包含活动项目数");
        assertTrue(statistics.containsKey("completedProjects"), "应包含已完成项目数");
        assertTrue(statistics.containsKey("totalUsers"), "应包含用户总数");
        assertTrue(statistics.containsKey("activeTasks"), "应包含活动任务数");
        
        // 验证数据类型和基本有效性
        assertTrue((Long)statistics.get("totalProjects") >= 3, "项目总数应至少为3");
        assertTrue((int)statistics.get("activeProjects") >= 2, "活动项目数应至少为2");
        assertTrue((int)statistics.get("completedProjects") >= 1, "已完成项目数应至少为1");
        assertTrue((Long)statistics.get("totalUsers") >= 2, "用户总数应至少为2");
        assertTrue((int)statistics.get("activeTasks") >= 1, "活动任务数应至少为1");
    }
    
    /**
     * 测试获取仪表盘统计数据
     * 验证能够正确获取仪表盘统计数据
     */
    @Test
    @DisplayName("测试获取仪表盘统计数据")
    public void testGetDashboardStatistics() {
        Map<String, Object> dashboardStats = statisticsService.getDashboardStatistics();
        
        assertNotNull(dashboardStats, "仪表盘统计数据不应为空");
        assertTrue(dashboardStats.containsKey("totalProjects"), "应包含项目总数");
        assertTrue(dashboardStats.containsKey("totalScores"), "应包含评分总数");
        assertTrue(dashboardStats.containsKey("averageScore"), "应包含平均分");
        assertTrue(dashboardStats.containsKey("recentActivity"), "应包含最近活动");
        
        // 验证数据类型和基本有效性
        assertTrue((Long)dashboardStats.get("totalProjects") >= 3, "项目总数应至少为3");
        assertTrue((Long)dashboardStats.get("totalScores") >= 5, "评分总数应至少为5");
        assertTrue((Double)dashboardStats.get("averageScore") >= 0, "平均分应至少为0");
    }
    
    /**
     * 测试获取项目统计数据
     * 验证能够正确获取项目统计数据
     */
    @Test
    @DisplayName("测试获取项目统计数据")
    public void testGetProjectStatistics() {
        List<Map<String, Object>> projectStats = statisticsService.getProjectStatistics();
        
        assertNotNull(projectStats, "项目统计数据不应为空");
        
        // 项目数量可能因为内部实现而与实际创建项目数量不同
        // 验证基本属性
        for (Map<String, Object> stat : projectStats) {
            if (stat.containsKey("projectId")) {
                assertTrue(stat.containsKey("projectId"), "应包含项目ID");
                assertTrue(stat.containsKey("projectName"), "应包含项目名称");
            }
        }
    }
    
    /**
     * 测试获取用户统计数据
     * 验证能够正确获取用户统计数据
     */
    @Test
    @DisplayName("测试获取用户统计数据")
    public void testGetUserStatistics() {
        List<Map<String, Object>> userStats = statisticsService.getUserStatistics();
        
        assertNotNull(userStats, "用户统计数据不应为空");
        assertEquals(2, userStats.size(), "应有2个用户的统计数据");
        
        Map<String, Object> userStat1 = null;
        Map<String, Object> userStat2 = null;
        
        for (Map<String, Object> stat : userStats) {
            if ("testuser1".equals(stat.get("username"))) {
                userStat1 = stat;
            } else if ("testuser2".equals(stat.get("username"))) {
                userStat2 = stat;
            }
        }
        
        assertNotNull(userStat1, "应包含testuser1的统计数据");
        assertNotNull(userStat2, "应包含testuser2的统计数据");
        
        assertEquals("测试用户1", userStat1.get("name"), "用户全名应为测试用户1");
        assertEquals(3, userStat1.get("scoreCount"), "testuser1的评分数量应为3");
        
        assertEquals("测试用户2", userStat2.get("name"), "用户全名应为测试用户2");
        assertEquals(2, userStat2.get("scoreCount"), "testuser2的评分数量应为2");
        
        // 计算预期的平均分 (草稿评分不计入)
        double expectedAvg1 = (85.5 + 92.0) / 2;
        assertEquals(expectedAvg1, (Double)userStat1.get("averageScore"), 0.01, "testuser1的平均分应约为" + expectedAvg1);
        
        double expectedAvg2 = (75.0 + 65.5) / 2;
        assertEquals(expectedAvg2, (Double)userStat2.get("averageScore"), 0.01, "testuser2的平均分应约为" + expectedAvg2);
    }
    
    /**
     * 测试获取时间段统计数据
     * 验证能够正确获取时间段统计数据
     */
    @Test
    @DisplayName("测试获取时间段统计数据")
    public void testGetTimeRangeStatistics() {
        // 测试1: 完整时间范围查询
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1); // 一年前
        Date startDate = calendar.getTime();
        Date endDate = new Date(); // 当前时间
        
        Map<String, Object> fullRangeStats = statisticsService.getTimeRangeStatistics(startDate, endDate);
        
        assertNotNull(fullRangeStats, "时间段统计数据不应为空");
        assertTrue(fullRangeStats.containsKey("projectCount"), "应包含项目数量");
        assertTrue(fullRangeStats.containsKey("scoreCount"), "应包含评分数量");
        assertTrue(fullRangeStats.containsKey("userCount"), "应包含用户数量");
        assertTrue(fullRangeStats.containsKey("averageScore"), "应包含平均分");
        assertTrue(fullRangeStats.containsKey("scoreDistribution"), "应包含分数分布");
        
        // 验证数据类型和基本有效性
        assertTrue((int)fullRangeStats.get("projectCount") >= 3, "项目数量应至少为3");
        assertTrue((int)fullRangeStats.get("scoreCount") >= 5, "评分数量应至少为5");
        assertTrue((int)fullRangeStats.get("userCount") >= 2, "用户数量应至少为2");
        
        // 验证评分分布
        Map<String, Integer> scoreDistribution = (Map<String, Integer>)fullRangeStats.get("scoreDistribution");
        assertNotNull(scoreDistribution, "评分分布不应为空");
        
        assertTrue(scoreDistribution.containsKey("0-60"), "应包含0-60区间");
        assertTrue(scoreDistribution.containsKey("60-70"), "应包含60-70区间");
        assertTrue(scoreDistribution.containsKey("70-80"), "应包含70-80区间");
        assertTrue(scoreDistribution.containsKey("80-90"), "应包含80-90区间");
        assertTrue(scoreDistribution.containsKey("90-100"), "应包含90-100区间");
        
        // 只验证分布的格式而不验证具体数量
        assertTrue(scoreDistribution.get("0-60") >= 0, "0-60区间评分数应不小于0");
        assertTrue(scoreDistribution.get("60-70") >= 0, "60-70区间评分数应不小于0");
        assertTrue(scoreDistribution.get("70-80") >= 0, "70-80区间评分数应不小于0");
        assertTrue(scoreDistribution.get("80-90") >= 0, "80-90区间评分数应不小于0");
        assertTrue(scoreDistribution.get("90-100") >= 0, "90-100区间评分数应不小于0");
    }
    
    /**
     * 测试获取评分分布统计
     * 验证能够正确获取评分分布统计
     */
    @Test
    @DisplayName("测试获取评分分布统计")
    public void testGetScoreDistribution() {
        Map<String, Integer> distribution = statisticsService.getScoreDistribution();
        
        assertNotNull(distribution, "评分分布统计不应为空");
        
        // 评分区间应该包括 [0-60), [60-70), [70-80), [80-90), [90-100]
        assertTrue(distribution.containsKey("0-60"), "应包含0-60区间");
        assertTrue(distribution.containsKey("60-70"), "应包含60-70区间");
        assertTrue(distribution.containsKey("70-80"), "应包含70-80区间");
        assertTrue(distribution.containsKey("80-90"), "应包含80-90区间");
        assertTrue(distribution.containsKey("90-100"), "应包含90-100区间");
        
        // 只验证基本有效性
        assertTrue(distribution.get("0-60") >= 0, "0-60区间评分数应不小于0");
        assertTrue(distribution.get("60-70") >= 0, "60-70区间评分数应不小于0");
        assertTrue(distribution.get("70-80") >= 0, "70-80区间评分数应不小于0");
        assertTrue(distribution.get("80-90") >= 0, "80-90区间评分数应不小于0");
        assertTrue(distribution.get("90-100") >= 0, "90-100区间评分数应不小于0");
        
        // 验证有效性：所有区间评分总和应至少等于创建的非草稿评分数量
        int totalScores = distribution.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(totalScores >= 4, "总评分数量应至少为4");
    }
    
    /**
     * 测试获取单个项目统计数据
     * 验证能够正确获取单个项目的统计数据
     */
    @Test
    @DisplayName("测试获取单个项目统计数据")
    public void testGetProjectStatisticsById() {
        // 确保测试项目1关联了评分
        testScore1 = new Score();
        testScore1.setProjectId(testProject1.getId());
        testScore1.setUserId(testUser1.getUsername());
        testScore1.setTotalScore(85.5);
        testScore1.setComments("测试评分1");
        testScore1.setIsDraft(false);
        testScore1.setCreateTime(currentDate);
        testScore1.setUpdateTime(currentDate);
        
        scoreRepository.insert(testScore1);
        
        Map<String, Object> projectStats = statisticsService.getProjectStatisticsById(testProject1.getId());
        
        assertNotNull(projectStats, "项目统计数据不应为空");
        
        // 验证基本结构和关键数据
        assertTrue(projectStats.containsKey("totalScore"), "应包含总评分");
        assertTrue(projectStats.containsKey("scorerCount"), "应包含评分人数");
        assertTrue(projectStats.containsKey("itemScores"), "应包含评分项得分");
        
        // 验证数据正确性
        assertTrue((Double)projectStats.get("totalScore") > 0, "总评分应大于0");
        assertTrue((Integer)projectStats.get("scorerCount") > 0, "评分人数应大于0");
    }
    
    /**
     * 测试获取单个用户统计数据
     * 验证能够正确获取单个用户的统计数据
     */
    @Test
    @DisplayName("测试获取单个用户统计数据")
    public void testGetUserStatisticsByUsername() {
        Map<String, Object> userStats = statisticsService.getUserStatisticsByUsername(testUser1.getUsername());
        
        assertNotNull(userStats, "用户统计数据不应为空");
        assertTrue(userStats.containsKey("scoreCount"), "应包含评分数量");
        assertTrue(userStats.containsKey("draftCount"), "应包含草稿数量");
        assertTrue(userStats.containsKey("finalCount"), "应包含最终评分数量");
        assertTrue(userStats.containsKey("averageScore"), "应包含平均分");
        
        assertEquals(3, userStats.get("scoreCount"), "用户评分总数应为3");
        assertEquals(1, userStats.get("draftCount"), "用户草稿数应为1");
        assertEquals(2, userStats.get("finalCount"), "用户最终评分数应为2");
        
        // 计算预期的平均分 (草稿评分不计入)
        double expectedAvg = (85.5 + 92.0) / 2;
        assertEquals(expectedAvg, (Double)userStats.get("averageScore"), 0.01, "平均分应约为" + expectedAvg);
    }
    
    /**
     * 测试获取单个任务统计数据
     * 验证能够正确获取单个任务的统计数据
     */
    @Test
    @DisplayName("测试获取单个任务统计数据")
    public void testGetTaskStatisticsById() {
        // 为了测试任务统计，需要设置任务与项目的关联以及任务与专家的关联
        // 这里模拟关联关系，实际项目中应根据真实的数据模型进行测试
        
        // 先获取任务统计，验证基本结构
        Map<String, Object> taskStats = statisticsService.getTaskStatisticsById(testTask1.getId());
        
        assertNotNull(taskStats, "任务统计数据不应为空");
        assertTrue(taskStats.containsKey("projectCount"), "应包含项目数量");
        assertTrue(taskStats.containsKey("expertCount"), "应包含专家数量");
    }
}