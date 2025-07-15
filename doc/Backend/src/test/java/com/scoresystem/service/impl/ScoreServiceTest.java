package com.scoresystem.service.impl;

import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评分服务集成测试类
 * 测试评分相关的业务逻辑，包括taskId支持
 * 
 * 测试说明：
 * 1. 使用@SpringBootTest进行集成测试，确保测试模式的一致性
 * 2. 使用@ActiveProfiles("sqlserver")指定使用SQL Server数据库
 * 3. 使用@Transactional确保测试数据回滚，不影响数据库
 */
@SpringBootTest
@ActiveProfiles("sqlserver")
@Transactional
class ScoreServiceTest {

    @Autowired
    private ScoreService scoreService;
    
    @Autowired
    private ScoreRepository scoreRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScoreItemRepository scoreItemRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    private User testUser;
    private Project testProject;
    private Task testTask;
    private ScoreItem testScoreItem;
    private Score testScore;
    private ScoreRequest testScoreRequest;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        cleanupTestData();
        
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser"); // 使用固定用户名，确保一致性
        testUser.setPassword("testpassword"); // 添加密码字段
        testUser.setName("测试用户");
        testUser.setRole("EXPERT");
        testUser.setCreateTime(new Date());
        testUser.setUpdateTime(new Date());
        
        // 调试输出：验证密码是否设置
        System.out.println("Debug: User password before insert: " + testUser.getPassword());
        System.out.println("Debug: User object: " + testUser);
        
        userRepository.insert(testUser);
        
        // 创建测试任务
        testTask = new Task();
        testTask.setTaskId("TEST-TASK-001");
        testTask.setCategory("测试任务");
        testTask.setStatus("ACTIVE");
        testTask.setStartTime(new Date());
        testTask.setEndTime(new Date(System.currentTimeMillis() + 86400000)); // 明天结束
        // 保存任务到数据库以获取真实ID
        taskRepository.insert(testTask);

        // 创建测试项目
        testProject = new Project();
        testProject.setName("测试项目");
        testProject.setDescription("测试项目描述");
        testProject.setStatus("IN_PROGRESS");
        testProject.setCreateTime(new Date());
        testProject.setUpdateTime(new Date());
        projectRepository.insert(testProject);
        
        // 创建测试评分项
        testScoreItem = new ScoreItem();
        testScoreItem.setName("测试评分项");
        testScoreItem.setMaxScore(100);
        testScoreItem.setWeight(1.0);
        testScoreItem.setProjectId(testProject.getId());
        testScoreItem.setDisplayOrder(1);
        scoreItemRepository.insert(testScoreItem);

        // 创建测试评分
        testScore = new Score();
        testScore.setProjectId(testProject.getId());
        testScore.setTaskId(testTask.getId()); // 使用真实taskId
        testScore.setUserId(testUser.getUsername()); // 使用真实用户名
        testScore.setTotalScore(85.0);
        testScore.setComments("测试评分");
        testScore.setCreateTime(new Date());
        testScore.setUpdateTime(new Date());
        testScore.setIsDraft(false);

        // 初始化测试评分请求
        testScoreRequest = new ScoreRequest();
        testScoreRequest.setProjectId(testProject.getId());
        testScoreRequest.setTaskId(testTask.getId()); // 使用真实taskId
        testScoreRequest.setUsername(testUser.getUsername()); // 使用真实用户名
        testScoreRequest.setTotalScore(85.0);
        testScoreRequest.setComments("测试评分");
        testScoreRequest.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        testScoreRequest.setScores(scores);
    }
    
    private void cleanupTestData() {
        // 清理评分详情
        try {
            // 这里可以添加清理逻辑，如果有直接的清理方法
        } catch (Exception e) {
            // 忽略异常
        }
    }

    @Test
    @DisplayName("测试保存评分 - 创建新评分")
    void testSaveScore_CreateNewScore() {
        // 执行测试
        ScoreDTO result = scoreService.saveScore(testScoreRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(testProject.getId(), result.getProjectId());
        assertEquals(testTask.getId(), result.getTaskId());
        assertEquals("testuser", result.getUsername());
        assertEquals(85.0, result.getTotalScore());
        assertEquals("测试评分", result.getComments());
        assertFalse(result.getIsDraft());
    }

    @Test
    @DisplayName("测试保存评分 - 更新现有评分")
    void testSaveScore_UpdateExistingScore() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);
        
        // 创建新的请求对象，而不是修改共享的对象
        ScoreRequest updateRequest = new ScoreRequest();
        updateRequest.setProjectId(testProject.getId());
        updateRequest.setTaskId(testTask.getId());
        updateRequest.setUsername("testuser");
        updateRequest.setTotalScore(90.0);
        updateRequest.setComments("更新的评分");
        updateRequest.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 90);
        updateRequest.setScores(scores);
        
        // 执行测试
        ScoreDTO result = scoreService.saveScore(updateRequest);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(testProject.getId(), result.getProjectId());
        assertEquals(testTask.getId(), result.getTaskId());
        assertEquals("testuser", result.getUsername());
        assertEquals(90.0, result.getTotalScore());
        assertEquals("更新的评分", result.getComments());
        assertFalse(result.getIsDraft());
    }

    @Test
    @DisplayName("测试保存评分 - 创建草稿")
    void testSaveScore_CreateDraft() {
        // 创建新的请求对象，而不是修改共享的对象
        ScoreRequest draftRequest = new ScoreRequest();
        draftRequest.setProjectId(testProject.getId());
        draftRequest.setTaskId(testTask.getId());
        draftRequest.setUsername("testuser");
        draftRequest.setTotalScore(85.0);
        draftRequest.setComments("测试评分");
        draftRequest.setIsDraft(true);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        draftRequest.setScores(scores);

        // 执行测试
        ScoreDTO result = scoreService.saveScore(draftRequest);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getIsDraft());
    }

    @Test
    @DisplayName("测试保存评分 - 项目不存在")
    void testSaveScore_ProjectNotFound() {
        // 创建新的请求对象，而不是修改共享的对象
        ScoreRequest invalidRequest = new ScoreRequest();
        invalidRequest.setProjectId(99999L); // 设置不存在的项目ID
        invalidRequest.setTaskId(testTask.getId());
        invalidRequest.setUsername("testuser");
        invalidRequest.setTotalScore(85.0);
        invalidRequest.setComments("测试评分");
        invalidRequest.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        invalidRequest.setScores(scores);

        // 执行测试
        ScoreDTO result = scoreService.saveScore(invalidRequest);

        // 验证结果
        assertNull(result);
    }

    @Test
    @DisplayName("测试保存评分 - 用户不存在")
    void testSaveScore_UserNotFound() {
        // 创建新的请求对象，而不是修改共享的对象
        ScoreRequest invalidRequest = new ScoreRequest();
        invalidRequest.setProjectId(testProject.getId());
        invalidRequest.setTaskId(testTask.getId());
        invalidRequest.setUsername("nonexistentuser"); // 设置不存在的用户名
        invalidRequest.setTotalScore(85.0);
        invalidRequest.setComments("测试评分");
        invalidRequest.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        invalidRequest.setScores(scores);

        // 执行测试
        ScoreDTO result = scoreService.saveScore(invalidRequest);

        // 验证结果
        assertNull(result);
    }

    @Test
    @DisplayName("测试获取评分历史 - 按项目和用户")
    void testGetScoreHistory_ByProjectAndUser() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoreHistory(testProject.getId(), "testuser");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testProject.getId(), result.get(0).getProjectId());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    @DisplayName("测试获取评分历史 - 按项目、用户和任务")
    void testGetScoreHistory_ByProjectAndUserAndTask() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoreHistory(testProject.getId(), testTask.getId(), "testuser");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testProject.getId(), result.get(0).getProjectId());
        assertEquals(testTask.getId(), result.get(0).getTaskId());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    @DisplayName("测试按项目获取评分")
    void testGetScoresByProject() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoresByProject(testProject.getId());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testProject.getId(), result.get(0).getProjectId());
    }

    @Test
    @DisplayName("测试按项目和任务获取评分")
    void testGetScoresByProject_WithTaskId() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoresByProject(testProject.getId(), testTask.getId());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testProject.getId(), result.get(0).getProjectId());
        assertEquals(testTask.getId(), result.get(0).getTaskId());
    }

    @Test
    @DisplayName("测试按用户获取评分")
    void testGetScoresByUser() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoresByUser("testuser");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    @DisplayName("测试按用户和任务获取评分")
    void testGetScoresByUser_WithTaskId() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoresByUser("testuser", testTask.getId());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals(testTask.getId(), result.get(0).getTaskId());
    }

    @Test
    @DisplayName("测试计算项目总评分")
    void testCalculateProjectTotalScore() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        Double result = scoreService.calculateProjectTotalScore(testProject.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(85.0, result);
    }

    @Test
    @DisplayName("测试计算项目总评分 - 指定任务")
    void testCalculateProjectTotalScore_WithTaskId() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        Double result = scoreService.calculateProjectTotalScore(testProject.getId(), testTask.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(85.0, result);
    }

    @Test
    @DisplayName("测试计算项目总评分 - 空评分")
    void testCalculateProjectTotalScore_EmptyScores() {
        // 执行测试
        Double result = scoreService.calculateProjectTotalScore(testProject.getId());

        // 验证结果
        assertEquals(0.0, result);
    }

    @Test
    @DisplayName("测试计算评分项平均分")
    void testCalculateScoreItemAverage() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        Double result = scoreService.calculateScoreItemAverage(testProject.getId(), testScoreItem.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(85.0, result);
    }

    @Test
    @DisplayName("测试计算评分项平均分 - 指定任务")
    void testCalculateScoreItemAverage_WithTaskId() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        Double result = scoreService.calculateScoreItemAverage(testProject.getId(), testTask.getId(), testScoreItem.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(85.0, result);
    }

    @Test
    @DisplayName("测试获取项目评分统计")
    void testGetProjectScoreStatistics() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        Map<String, Object> result = scoreService.getProjectScoreStatistics(testProject.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(85.0, result.get("totalScore"));
        assertNotNull(result.get("itemScores"));
    }

    @Test
    @DisplayName("测试获取项目评分统计 - 指定任务")
    void testGetProjectScoreStatistics_WithTaskId() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        Map<String, Object> result = scoreService.getProjectScoreStatistics(testProject.getId(), testTask.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(testProject.getId(), result.get("projectId"));
        assertEquals("测试项目", result.get("projectName"));
        assertEquals(testTask.getId(), result.get("taskId"));
        assertEquals(1, result.get("totalScores"));
        assertEquals(85.0, result.get("averageScore"));
    }

    @Test
    @DisplayName("测试获取项目评分统计 - 项目不存在")
    void testGetProjectScoreStatistics_ProjectNotFound() {
        // 执行测试
        Map<String, Object> result = scoreService.getProjectScoreStatistics(99999L);

        // 验证结果
        assertNotNull(result);
        assertEquals(0.0, result.get("totalScore"));
        assertEquals(0, result.get("scorerCount"));
    }

    @Test
    @DisplayName("测试获取所有评分")
    void testGetAllScores() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getAllScores();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("测试获取所有评分 - 指定任务")
    void testGetAllScores_WithTaskId() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getAllScores(testTask.getId());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testTask.getId(), result.get(0).getTaskId());
    }

    @Test
    @DisplayName("测试按任务获取评分")
    void testGetScoresByTask() {
        // 先保存一个评分
        scoreService.saveScore(testScoreRequest);

        // 执行测试
        List<ScoreDTO> result = scoreService.getScoresByTask(testTask.getId());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty()); // 应该不为空，因为我们刚刚保存了一个评分
        assertEquals(testTask.getId(), result.get(0).getTaskId());
    }

    @Test
    @DisplayName("测试保存评分 - 包含评分详情")
    void testSaveScore_WithScoreDetails() {
        // 创建包含详细评分项的请求
        ScoreRequest detailedRequest = new ScoreRequest();
        detailedRequest.setProjectId(testProject.getId());
        detailedRequest.setTaskId(testTask.getId());
        detailedRequest.setUsername("testuser");
        detailedRequest.setTotalScore(85.0);
        detailedRequest.setComments("详细评分测试");
        detailedRequest.setIsDraft(false);
        
        // 添加评分项详情
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        detailedRequest.setScores(scores);
        
        // 执行测试
        ScoreDTO result = scoreService.saveScore(detailedRequest);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(testProject.getId(), result.getProjectId());
        assertEquals(testTask.getId(), result.getTaskId());
        assertEquals("testuser", result.getUsername());
        assertEquals(85.0, result.getTotalScore());
        assertEquals("详细评分测试", result.getComments());
        assertFalse(result.getIsDraft());
    }
    
    /**
     * 测试评分项角色过滤 - 用户有权限
     * 验证用户有权限的评分项能够正常提交
     */
    @Test
    @DisplayName("测试评分项角色过滤 - 用户有权限")
    void testScoreItemRoleFilter_UserHasPermission() {
        // 确保测试用户有EXPERT角色
        testUser.setRole("EXPERT");
        userRepository.updateById(testUser);
        
        // 确保评分项与EXPERT角色关联
        jdbcTemplate.update(
            "INSERT INTO score_item_roles (score_item_id, role) VALUES (?, ?)",
            testScoreItem.getId(), "EXPERT"
        );
        
        // 创建评分请求
        ScoreRequest request = new ScoreRequest();
        request.setProjectId(testProject.getId());
        request.setTaskId(testTask.getId());
        request.setUsername("testuser");
        request.setTotalScore(85.0);
        request.setComments("角色权限测试");
        request.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        request.setScores(scores);
        
        // 执行测试
        ScoreDTO result = scoreService.saveScore(request);
        
        // 验证结果
        assertNotNull(result, "评分应该成功保存");
        assertEquals(85.0, result.getTotalScore(), "总分应该匹配");
    }
    
    /**
     * 测试评分项角色过滤 - 用户无权限
     * 验证用户无权限的评分项无法提交
     */
    @Test
    @DisplayName("测试评分项角色过滤 - 用户无权限")
    void testScoreItemRoleFilter_UserNoPermission() {
        // 设置用户为ADMIN角色，但评分项只关联EXPERT角色
        testUser.setRole("ADMIN");
        userRepository.updateById(testUser);
        
        // 删除ADMIN角色的评分项关联，只保留EXPERT角色
        jdbcTemplate.update(
            "DELETE FROM score_item_roles WHERE score_item_id = ? AND role = ?",
            testScoreItem.getId(), "ADMIN"
        );
        
        // 确保只有EXPERT角色关联
        jdbcTemplate.update(
            "INSERT INTO score_item_roles (score_item_id, role) VALUES (?, ?)",
            testScoreItem.getId(), "EXPERT"
        );
        
        // 创建评分请求
        ScoreRequest request = new ScoreRequest();
        request.setProjectId(testProject.getId());
        request.setTaskId(testTask.getId());
        request.setUsername("testuser");
        request.setTotalScore(85.0);
        request.setComments("无权限测试");
        request.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        request.setScores(scores);
        
        // 执行测试并验证异常
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            scoreService.saveScore(request);
        });
        
        assertTrue(exception.getMessage().contains("评分项") || 
                  exception.getMessage().contains("权限") || 
                  exception.getMessage().contains("角色"), 
                  "异常信息应该包含权限相关提示");
    }
    
    /**
     * 测试评分项角色过滤 - 混合权限
     * 验证用户对部分评分项有权限的情况
     */
    @Test
    @DisplayName("测试评分项角色过滤 - 混合权限")
    void testScoreItemRoleFilter_MixedPermissions() {
        // 创建第二个评分项
        ScoreItem testScoreItem2 = new ScoreItem();
        testScoreItem2.setName("测试评分项2");
        testScoreItem2.setMaxScore(100);
        testScoreItem2.setWeight(1.0);
        testScoreItem2.setProjectId(testProject.getId());
        testScoreItem2.setDisplayOrder(2);
        scoreItemRepository.insert(testScoreItem2);
        
        // 设置用户为EXPERT角色
        testUser.setRole("EXPERT");
        userRepository.updateById(testUser);
        
        // 第一个评分项关联EXPERT角色
        jdbcTemplate.update(
            "INSERT INTO score_item_roles (score_item_id, role) VALUES (?, ?)",
            testScoreItem.getId(), "EXPERT"
        );
        
        // 第二个评分项关联ADMIN角色
        jdbcTemplate.update(
            "INSERT INTO score_item_roles (score_item_id, role) VALUES (?, ?)",
            testScoreItem2.getId(), "ADMIN"
        );
        
        // 创建评分请求，包含两个评分项
        ScoreRequest request = new ScoreRequest();
        request.setProjectId(testProject.getId());
        request.setTaskId(testTask.getId());
        request.setUsername("testuser");
        request.setTotalScore(85.0);
        request.setComments("混合权限测试");
        request.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);  // 有权限
        scores.put(testScoreItem2.getId(), 90); // 无权限
        request.setScores(scores);

        // 执行测试并验证异常
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            scoreService.saveScore(request);
        });
        
        assertTrue(exception.getMessage().contains("评分项") || 
                  exception.getMessage().contains("权限") || 
                  exception.getMessage().contains("角色"), 
                  "异常信息应该包含权限相关提示");
    }
    
    /**
     * 测试评分项角色过滤 - 无评分项关联
     * 验证当评分项没有角色关联时的处理
     */
    @Test
    @DisplayName("测试评分项角色过滤 - 无评分项关联")
    void testScoreItemRoleFilter_NoRoleAssociation() {
        // 删除所有评分项角色关联
        jdbcTemplate.update("DELETE FROM score_item_roles WHERE score_item_id = ?", testScoreItem.getId());
        
        // 设置用户为EXPERT角色
        testUser.setRole("EXPERT");
        userRepository.updateById(testUser);
        
        // 创建评分请求
        ScoreRequest request = new ScoreRequest();
        request.setProjectId(testProject.getId());
        request.setTaskId(testTask.getId());
        request.setUsername("testuser");
        request.setTotalScore(85.0);
        request.setComments("无角色关联测试");
        request.setIsDraft(false);
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem.getId(), 85);
        request.setScores(scores);
        
        // 执行测试并验证异常
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            scoreService.saveScore(request);
        });
        
        assertTrue(exception.getMessage().contains("评分项") || 
                  exception.getMessage().contains("权限") || 
                  exception.getMessage().contains("角色"), 
                  "异常信息应该包含权限相关提示");
    }
} 