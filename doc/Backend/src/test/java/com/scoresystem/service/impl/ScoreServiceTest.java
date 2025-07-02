package com.scoresystem.service.impl;

import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ScoreService;
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
 * ScoreService测试类
 * 
 * 测试说明：
 * 1. 使用@SpringBootTest进行集成测试
 * 2. 使用@ActiveProfiles("sqlserver")指定使用SQL Server数据库
 * 3. 使用@Transactional确保测试数据回滚，不影响数据库
 * 4. 测试包括：保存评分、获取评分历史、获取项目评分、获取用户评分等功能
 */
@SpringBootTest
@ActiveProfiles("sqlserver")
@Transactional
public class ScoreServiceTest {

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
    private JdbcTemplate jdbcTemplate;
    
    private Project testProject;
    private User testUser;
    private ScoreItem testScoreItem1;
    private ScoreItem testScoreItem2;
    private ScoreRequest testScoreRequest;
    
    /**
     * 测试前准备
     * 创建测试数据
     */
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
        testProject.setDescription("测试项目描述");
        testProject.setStatus("active");
        
        projectRepository.insert(testProject);
        
        // 创建测试评分项
        testScoreItem1 = new ScoreItem();
        testScoreItem1.setProjectId(testProject.getId());
        testScoreItem1.setName("技术可行性");
        testScoreItem1.setDescription("项目技术方案是否可行");
        testScoreItem1.setWeight(0.6);
        testScoreItem1.setMaxScore(100);
        
        scoreItemRepository.insert(testScoreItem1);
        
        testScoreItem2 = new ScoreItem();
        testScoreItem2.setProjectId(testProject.getId());
        testScoreItem2.setName("商业价值");
        testScoreItem2.setDescription("项目商业价值评估");
        testScoreItem2.setWeight(0.4);
        testScoreItem2.setMaxScore(100);
        
        scoreItemRepository.insert(testScoreItem2);
        
        // 创建测试评分请求
        testScoreRequest = new ScoreRequest();
        testScoreRequest.setProjectId(testProject.getId());
        testScoreRequest.setUsername(testUser.getUsername());
        testScoreRequest.setComments("这是一个测试评分");
        testScoreRequest.setIsDraft(false);
        
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem1.getId(), 80);
        scores.put(testScoreItem2.getId(), 90);
        testScoreRequest.setScores(scores);
    }
    
    /**
     * 测试保存评分
     * 验证能够正确保存评分
     */
    @Test
    @DisplayName("测试保存评分")
    public void testSaveScore() {
        ScoreDTO savedScore = scoreService.saveScore(testScoreRequest);
        
        assertNotNull(savedScore, "保存的评分不应为空");
        assertEquals(testProject.getId(), savedScore.getProjectId(), "项目ID应该匹配");
        assertEquals(testUser.getUsername(), savedScore.getUsername(), "用户名应该匹配");
        assertEquals("这是一个测试评分", savedScore.getComments(), "评论应该匹配");
        assertFalse(savedScore.getIsDraft(), "评分不应该是草稿");
        
        // 验证总分计算正确
        // 总分 = (80 * 0.6 + 90 * 0.4) / (0.6 + 0.4) = (48 + 36) / 1 = 84
        assertEquals(84.0, savedScore.getTotalScore(), 0.01, "总分计算应该正确");
        
        // 验证评分已保存到数据库
        List<Score> dbScores = scoreRepository.findByProjectIdAndUsername(testProject.getId(), testUser.getUsername());
        assertFalse(dbScores.isEmpty(), "评分应该已保存到数据库");
        assertEquals(84.0, dbScores.get(0).getTotalScore(), 0.01, "数据库中的总分应该正确");
    }
    
    /**
     * 测试保存草稿评分
     * 验证能够正确保存草稿评分
     */
    @Test
    @DisplayName("测试保存草稿评分")
    public void testSaveDraftScore() {
        testScoreRequest.setIsDraft(true);
        ScoreDTO savedScore = scoreService.saveScore(testScoreRequest);
        
        assertNotNull(savedScore, "保存的评分不应为空");
        assertTrue(savedScore.getIsDraft(), "评分应该是草稿");
        
        // 再次保存，应该创建新的草稿
        ScoreDTO secondSavedScore = scoreService.saveScore(testScoreRequest);
        assertNotNull(secondSavedScore, "第二次保存的评分不应为空");
        assertTrue(secondSavedScore.getIsDraft(), "第二次保存的评分应该是草稿");
        
        // 验证数据库中有两条草稿记录
        List<Score> dbScores = scoreRepository.findByProjectIdAndUsername(testProject.getId(), testUser.getUsername());
        assertEquals(2, dbScores.size(), "数据库中应该有两条草稿记录");
    }
    
    /**
     * 测试更新评分
     * 验证能够正确更新现有评分
     */
    @Test
    @DisplayName("测试更新评分")
    public void testUpdateScore() {
        // 先保存一个评分
        ScoreDTO firstScore = scoreService.saveScore(testScoreRequest);
        assertNotNull(firstScore, "第一次保存的评分不应为空");
        
        // 修改评分并再次保存
        testScoreRequest.setComments("这是更新后的评论");
        Map<Long, Integer> updatedScores = new HashMap<>();
        updatedScores.put(testScoreItem1.getId(), 70);
        updatedScores.put(testScoreItem2.getId(), 85);
        testScoreRequest.setScores(updatedScores);
        
        ScoreDTO updatedScore = scoreService.saveScore(testScoreRequest);
        
        assertNotNull(updatedScore, "更新的评分不应为空");
        assertEquals("这是更新后的评论", updatedScore.getComments(), "评论应该已更新");
        
        // 验证总分已更新
        // 新总分 = (70 * 0.6 + 85 * 0.4) / (0.6 + 0.4) = (42 + 34) / 1 = 76
        assertEquals(76.0, updatedScore.getTotalScore(), 0.01, "总分应该已更新");
        
        // 验证数据库中只有一条记录
        List<Score> dbScores = scoreRepository.findByProjectIdAndUsername(testProject.getId(), testUser.getUsername());
        assertEquals(1, dbScores.size(), "数据库中应该只有一条记录");
        assertEquals("这是更新后的评论", dbScores.get(0).getComments(), "数据库中的评论应该已更新");
    }
    
    /**
     * 测试获取评分历史
     * 验证能够正确获取特定项目和用户的评分历史
     */
    @Test
    @DisplayName("测试获取评分历史")
    public void testGetScoreHistory() {
        // 保存多个评分
        scoreService.saveScore(testScoreRequest);
        
        // 修改为草稿并保存
        testScoreRequest.setIsDraft(true);
        testScoreRequest.setComments("这是草稿评分");
        scoreService.saveScore(testScoreRequest);
        
        // 获取评分历史
        List<ScoreDTO> scoreHistory = scoreService.getScoreHistory(testProject.getId(), testUser.getUsername());
        
        assertNotNull(scoreHistory, "评分历史不应为空");
        assertEquals(2, scoreHistory.size(), "应该有两条评分记录");
    }
    
    /**
     * 测试获取项目所有评分
     * 验证能够正确获取特定项目的所有评分
     */
    @Test
    @DisplayName("测试获取项目所有评分")
    public void testGetScoresByProject() {
        // 保存评分
        scoreService.saveScore(testScoreRequest);
        
        // 创建另一个用户并保存评分
        User anotherUser = new User();
        anotherUser.setUsername("anotherexpert");
        anotherUser.setPassword("password123");
        anotherUser.setName("另一位专家");
        anotherUser.setRole("expert");
        anotherUser.setCreateTime(new Date());
        anotherUser.setUpdateTime(new Date());
        
        userRepository.insert(anotherUser);
        
        ScoreRequest anotherScoreRequest = new ScoreRequest();
        anotherScoreRequest.setProjectId(testProject.getId());
        anotherScoreRequest.setUsername(anotherUser.getUsername());
        anotherScoreRequest.setComments("来自另一位专家的评分");
        anotherScoreRequest.setIsDraft(false);
        
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem1.getId(), 75);
        scores.put(testScoreItem2.getId(), 85);
        anotherScoreRequest.setScores(scores);
        
        scoreService.saveScore(anotherScoreRequest);
        
        // 获取项目所有评分
        List<ScoreDTO> projectScores = scoreService.getScoresByProject(testProject.getId());
        
        assertNotNull(projectScores, "项目评分不应为空");
        assertEquals(2, projectScores.size(), "应该有两条评分记录");
    }
    
    /**
     * 测试获取用户所有评分
     * 验证能够正确获取特定用户的所有评分
     */
    @Test
    @DisplayName("测试获取用户所有评分")
    public void testGetScoresByUser() {
        // 保存评分
        scoreService.saveScore(testScoreRequest);
        
        // 创建另一个项目并保存评分
        Project anotherProject = new Project();
        anotherProject.setName("另一个测试项目");
        anotherProject.setDescription("另一个测试项目描述");
        anotherProject.setStatus("active");
        
        projectRepository.insert(anotherProject);
        
        // 为新项目创建评分项
        ScoreItem anotherScoreItem = new ScoreItem();
        anotherScoreItem.setProjectId(anotherProject.getId());
        anotherScoreItem.setName("整体评价");
        anotherScoreItem.setDescription("项目整体评价");
        anotherScoreItem.setWeight(1.0);
        anotherScoreItem.setMaxScore(100);
        
        scoreItemRepository.insert(anotherScoreItem);
        
        // 为新项目创建评分
        ScoreRequest anotherProjectScoreRequest = new ScoreRequest();
        anotherProjectScoreRequest.setProjectId(anotherProject.getId());
        anotherProjectScoreRequest.setUsername(testUser.getUsername());
        anotherProjectScoreRequest.setComments("对另一个项目的评分");
        anotherProjectScoreRequest.setIsDraft(false);
        
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(anotherScoreItem.getId(), 95);
        anotherProjectScoreRequest.setScores(scores);
        
        scoreService.saveScore(anotherProjectScoreRequest);
        
        // 获取用户所有评分
        List<ScoreDTO> userScores = scoreService.getScoresByUser(testUser.getUsername());
        
        assertNotNull(userScores, "用户评分不应为空");
        assertEquals(2, userScores.size(), "应该有两条评分记录");
    }
    
    /**
     * 测试计算项目总评分
     * 验证能够正确计算项目的总评分
     */
    @Test
    @DisplayName("测试计算项目总评分")
    public void testCalculateProjectTotalScore() {
        // 保存评分
        scoreService.saveScore(testScoreRequest);
        
        // 创建另一个用户并保存评分
        User anotherUser = new User();
        anotherUser.setUsername("anotherexpert");
        anotherUser.setPassword("password123");
        anotherUser.setName("另一位专家");
        anotherUser.setRole("expert");
        anotherUser.setCreateTime(new Date());
        anotherUser.setUpdateTime(new Date());
        
        userRepository.insert(anotherUser);
        
        ScoreRequest anotherScoreRequest = new ScoreRequest();
        anotherScoreRequest.setProjectId(testProject.getId());
        anotherScoreRequest.setUsername(anotherUser.getUsername());
        anotherScoreRequest.setComments("来自另一位专家的评分");
        anotherScoreRequest.setIsDraft(false);
        
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(testScoreItem1.getId(), 75);
        scores.put(testScoreItem2.getId(), 85);
        anotherScoreRequest.setScores(scores);
        
        scoreService.saveScore(anotherScoreRequest);
        
        // 计算项目总评分
        Double totalScore = scoreService.calculateProjectTotalScore(testProject.getId());
        
        assertNotNull(totalScore, "总评分不应为空");
        // 第一个评分: 84.0, 第二个评分: 79.0, 平均值: 81.5
        assertEquals(81.5, totalScore, 0.01, "总评分计算应该正确");
    }
}
