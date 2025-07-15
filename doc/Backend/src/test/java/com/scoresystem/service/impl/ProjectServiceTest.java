package com.scoresystem.service.impl;

import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ProjectService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectService测试类
 * 
 * 测试说明：
 * 1. 使用@SpringBootTest进行集成测试
 * 2. 使用@ActiveProfiles("sqlserver")指定使用SQL Server数据库
 * 3. 使用@Transactional确保测试数据回滚，不影响数据库
 * 4. 测试包括：获取项目列表、根据ID获取项目、保存项目、删除项目、
 *    根据任务获取项目、获取评分项、批量操作、更新顺序等功能
 */
@SpringBootTest
@ActiveProfiles("sqlserver")
@Transactional
public class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private ScoreItemRepository scoreItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private Project testProject;
    private ProjectDTO testProjectDTO;
    private User testUser;
    private ScoreItem testScoreItem;
    
    /**
     * 测试前准备
     * 创建测试项目数据和用户数据
     */
    @BeforeEach
    public void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setName("测试用户");
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
        
        // 创建任务-项目关联关系（通过中间表）
        jdbcTemplate.update(
            "INSERT INTO task_projects (task_id, project_id) VALUES (?, ?)",
            1L, testProject.getId()
        );
        
        // 创建测试评分项
        testScoreItem = new ScoreItem();
        testScoreItem.setProjectId(testProject.getId());
        testScoreItem.setName("测试评分项");
        testScoreItem.setDescription("这是一个测试评分项");
        testScoreItem.setMaxScore(100);
        testScoreItem.setMinScore(0);
        testScoreItem.setWeight(1.0);
        testScoreItem.setDisplayOrder(1);
        
        scoreItemRepository.insert(testScoreItem);
        
        // 创建评分项角色关联
        jdbcTemplate.update(
            "INSERT INTO score_item_roles (score_item_id, role) VALUES (?, ?)",
            testScoreItem.getId(), "EXPERT"
        );
        
        // 创建测试ProjectDTO
        testProjectDTO = new ProjectDTO();
        testProjectDTO.setName("新测试项目");
        testProjectDTO.setDescription("这是一个新的测试项目");
        testProjectDTO.setStatus("DRAFT");
        testProjectDTO.setDisplayOrder(2);
    }
    
    /**
     * 测试获取所有项目
     * 验证能够正确获取所有项目列表
     */
    @Test
    @DisplayName("测试获取所有项目")
    public void testGetAllProjects() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        
        assertNotNull(projects, "项目列表不应为空");
        assertFalse(projects.isEmpty(), "项目列表不应为空");
        
        // 验证测试项目在列表中
        boolean found = projects.stream()
                .anyMatch(p -> p.getId().equals(testProject.getId()));
        assertTrue(found, "测试项目应该在项目列表中");
    }
    
    /**
     * 测试根据ID获取项目
     * 验证能够正确获取指定ID的项目
     */
    @Test
    @DisplayName("测试根据ID获取项目")
    public void testGetProjectById() {
        ProjectDTO project = projectService.getProjectById(testProject.getId());
        
        assertNotNull(project, "应该能找到测试项目");
        assertEquals(testProject.getId(), project.getId(), "项目ID应该匹配");
        assertEquals("测试项目", project.getName(), "项目名称应该匹配");
        
        // 验证评分项列表
        assertNotNull(project.getScoreItems(), "评分项列表不应为空");
        assertFalse(project.getScoreItems().isEmpty(), "评分项列表不应为空");
    }
    
    /**
     * 测试保存新项目
     * 验证能够正确创建新项目
     */
    @Test
    @DisplayName("测试保存新项目")
    public void testSaveNewProject() {
        ProjectDTO savedProject = projectService.saveProject(testProjectDTO);
        
        assertNotNull(savedProject, "保存的项目不应为空");
        assertNotNull(savedProject.getId(), "新项目应该有ID");
        assertEquals("新测试项目", savedProject.getName(), "项目名称应该匹配");
        assertEquals("这是一个新的测试项目", savedProject.getDescription(), "项目描述应该匹配");
        
        // 验证项目已保存到数据库
        Project dbProject = projectRepository.selectById(savedProject.getId());
        assertNotNull(dbProject, "项目应该已保存到数据库");
        assertEquals("新测试项目", dbProject.getName(), "数据库中的项目名称应该匹配");
    }
    
    /**
     * 测试更新现有项目
     * 验证能够正确更新现有项目
     */
    @Test
    @DisplayName("测试更新现有项目")
    public void testUpdateExistingProject() {
        // 修改测试项目信息
        testProjectDTO.setId(testProject.getId());
        testProjectDTO.setName("更新的测试项目");
        testProjectDTO.setDescription("这是一个更新的测试项目");
        
        ProjectDTO updatedProject = projectService.saveProject(testProjectDTO);
        
        assertNotNull(updatedProject, "更新的项目不应为空");
        assertEquals(testProject.getId(), updatedProject.getId(), "项目ID应该匹配");
        assertEquals("更新的测试项目", updatedProject.getName(), "更新后的项目名称应该匹配");
        assertEquals("这是一个更新的测试项目", updatedProject.getDescription(), "更新后的项目描述应该匹配");
        
        // 验证项目已更新到数据库
        Project dbProject = projectRepository.selectById(testProject.getId());
        assertNotNull(dbProject, "项目应该存在于数据库中");
        assertEquals("更新的测试项目", dbProject.getName(), "数据库中的项目名称应该已更新");
    }
    
    /**
     * 测试删除项目
     * 验证能够正确删除项目
     */
    @Test
    @DisplayName("测试删除项目")
    public void testDeleteProject() {
        projectService.deleteProject(testProject.getId());
        
        // 验证项目已从数据库中删除
        Project dbProject = projectRepository.selectById(testProject.getId());
        assertNull(dbProject, "项目应该已从数据库中删除");
    }
    
    /**
     * 测试根据任务ID获取项目列表
     * 验证能够正确获取指定任务ID的项目列表
     */
    @Test
    @DisplayName("测试根据任务ID获取项目列表")
    public void testGetProjectsByTask() {
        List<ProjectDTO> projects = projectService.getProjectsByTask(1L);
        
        assertNotNull(projects, "项目列表不应为空");
        assertFalse(projects.isEmpty(), "项目列表不应为空");
        
        // 验证测试项目在列表中
        boolean found = projects.stream()
                .anyMatch(p -> p.getId().equals(testProject.getId()));
        assertTrue(found, "测试项目应该在项目列表中");
    }
    
    /**
     * 测试根据用户角色获取项目评分项
     * 验证能够正确获取指定用户角色的评分项
     */
    @Test
    @DisplayName("测试根据用户角色获取项目评分项")
    public void testGetScoreItemsByUserRole() {
        List<ScoreItemDTO> scoreItems = projectService.getScoreItemsByUserRole(testProject.getId(), "testuser");
        
        assertNotNull(scoreItems, "评分项列表不应为空");
        assertFalse(scoreItems.isEmpty(), "评分项列表不应为空");
        
        // 验证测试评分项在列表中
        boolean found = scoreItems.stream()
                .anyMatch(s -> s.getId().equals(testScoreItem.getId()));
        assertTrue(found, "测试评分项应该在评分项列表中");
    }
    
    /**
     * 测试批量更新项目状态
     * 验证能够正确批量更新项目状态
     */
    @Test
    @DisplayName("测试批量更新项目状态")
    public void testBatchUpdateStatus() {
        List<Long> projectIds = new ArrayList<>();
        projectIds.add(testProject.getId());
        
        projectService.batchUpdateStatus(projectIds, "COMPLETED");
        
        // 验证项目状态已更新
        Project dbProject = projectRepository.selectById(testProject.getId());
        assertNotNull(dbProject, "项目应该存在于数据库中");
        assertEquals("COMPLETED", dbProject.getStatus(), "项目状态应该已更新");
    }
    
    /**
     * 测试批量删除项目
     * 验证能够正确批量删除项目
     */
    @Test
    @DisplayName("测试批量删除项目")
    public void testBatchDelete() {
        List<Long> projectIds = new ArrayList<>();
        projectIds.add(testProject.getId());
        
        projectService.batchDelete(projectIds);
        
        // 验证项目已从数据库中删除
        Project dbProject = projectRepository.selectById(testProject.getId());
        assertNull(dbProject, "项目应该已从数据库中删除");
        
        // 验证关联的评分项也已删除
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(testProject.getId());
        assertTrue(scoreItems.isEmpty(), "关联的评分项应该已删除");
    }
    
    /**
     * 测试更新项目顺序
     * 验证能够正确更新项目顺序
     */
    @Test
    @DisplayName("测试更新项目顺序")
    public void testUpdateOrder() {
        // 创建第二个测试项目
        Project testProject2 = new Project();
        testProject2.setName("测试项目2");
        testProject2.setDescription("这是第二个测试项目");
        testProject2.setStatus("ACTIVE");
        testProject2.setDisplayOrder(2);
        testProject2.setCreateTime(new Date());
        testProject2.setUpdateTime(new Date());
        
        projectRepository.insert(testProject2);
        
        // 更新顺序
        List<Long> projectIds = Arrays.asList(testProject2.getId(), testProject.getId());
        projectService.updateOrder(projectIds);
        
        // 验证顺序已更新
        Project dbProject1 = projectRepository.selectById(testProject.getId());
        Project dbProject2 = projectRepository.selectById(testProject2.getId());
        
        assertEquals(1, dbProject2.getDisplayOrder(), "第二个项目的顺序应该是1");
        assertEquals(2, dbProject1.getDisplayOrder(), "第一个项目的顺序应该是2");
    }
    
    /**
     * 测试获取项目评分进度
     * 验证能够正确获取项目评分进度
     */
    @Test
    @DisplayName("测试获取项目评分进度")
    public void testGetProjectProgress() {
        Map<String, Object> progress = projectService.getProjectProgress(testProject.getId());
        
        assertNotNull(progress, "评分进度不应为空");
        assertTrue(progress.containsKey("totalExperts"), "评分进度应包含totalExperts键");
        assertTrue(progress.containsKey("completedExperts"), "评分进度应包含completedExperts键");
        assertTrue(progress.containsKey("completionPercentage"), "评分进度应包含completionPercentage键");
    }
    
    /**
     * 测试获取项目评分详情
     * 验证能够正确获取项目的评分详情信息
     */
    @Test
    @DisplayName("测试获取项目评分详情")
    public void testGetProjectScores() {
        // 创建测试评分数据
        jdbcTemplate.update(
            "INSERT INTO scores (project_id, task_id, user_id, total_score, comments, create_time, update_time, is_draft) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            testProject.getId(), 1L, testUser.getUsername(), 85.0, "测试评分", new Date(), new Date(), false
        );
        
        // 测试获取项目评分详情（不指定任务）
        Map<String, Object> scores = projectService.getProjectScores(testProject.getId(), null);
        
        assertNotNull(scores, "项目评分详情不应为空");
        assertTrue(scores.containsKey("project"), "应包含项目信息");
        assertTrue(scores.containsKey("scoreStatistics"), "应包含评分统计");
        assertTrue(scores.containsKey("scoreProgress"), "应包含评分进度");
        
        // 验证项目信息
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) scores.get("project");
        assertNotNull(project, "项目信息不应为空");
        assertEquals(testProject.getId(), project.get("id"), "项目ID应该匹配");
        
        // 验证评分统计
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) scores.get("scoreStatistics");
        assertNotNull(statistics, "评分统计不应为空");
        assertTrue(statistics.containsKey("totalScores"), "应包含总评分数量");
        assertTrue(statistics.containsKey("averageScore"), "应包含平均分");
        
        // 验证评分进度
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) scores.get("scoreProgress");
        assertNotNull(progress, "评分进度不应为空");
        assertTrue(progress.containsKey("totalExperts"), "应包含专家总数");
        assertTrue(progress.containsKey("completedExperts"), "应包含已完成专家数");
    }
    
    /**
     * 测试获取项目评分详情（指定任务）
     * 验证能够正确获取指定任务下的项目评分详情
     */
    @Test
    @DisplayName("测试获取项目评分详情（指定任务）")
    public void testGetProjectScores_WithTaskId() {
        // 创建测试评分数据
        jdbcTemplate.update(
            "INSERT INTO scores (project_id, task_id, user_id, total_score, comments, create_time, update_time, is_draft) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            testProject.getId(), 2L, testUser.getUsername(), 90.0, "测试评分2", new Date(), new Date(), false
        );
        
        // 测试获取项目评分详情（指定任务）
        Map<String, Object> scores = projectService.getProjectScores(testProject.getId(), 2L);
        
        assertNotNull(scores, "项目评分详情不应为空");
        assertTrue(scores.containsKey("project"), "应包含项目信息");
        assertTrue(scores.containsKey("scoreStatistics"), "应包含评分统计");
        assertTrue(scores.containsKey("scoreProgress"), "应包含评分进度");
        
        // 验证评分统计
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) scores.get("scoreStatistics");
        assertNotNull(statistics, "评分统计不应为空");
        assertEquals(1, statistics.get("totalScores"), "总评分数量应该为1");
        assertEquals(90.0, statistics.get("averageScore"), "平均分应该为90.0");
    }
    
    /**
     * 测试获取项目评分详情 - 项目不存在
     * 验证当项目不存在时返回空数据
     */
    @Test
    @DisplayName("测试获取项目评分详情 - 项目不存在")
    public void testGetProjectScores_ProjectNotFound() {
        Map<String, Object> scores = projectService.getProjectScores(99999L, null);
        
        assertNotNull(scores, "返回结果不应为空");
        assertTrue(scores.containsKey("project"), "应包含项目信息");
        assertTrue(scores.containsKey("scoreStatistics"), "应包含评分统计");
        assertTrue(scores.containsKey("scoreProgress"), "应包含评分进度");
        
        // 验证项目信息为空
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) scores.get("project");
        assertNull(project, "项目信息应该为空");
        
        // 验证评分统计为空
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) scores.get("scoreStatistics");
        assertNotNull(statistics, "评分统计不应为空");
        assertEquals(0, statistics.get("totalScores"), "总评分数量应该为0");
        assertEquals(0.0, statistics.get("averageScore"), "平均分应该为0.0");
    }
}