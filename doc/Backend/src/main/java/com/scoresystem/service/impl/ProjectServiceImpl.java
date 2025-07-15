package com.scoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.ScoreItemRole;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreItemRoleRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scoresystem.model.Score;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * 项目服务实现类
 */
@Service
@Transactional
@Profile("!test")
public class ProjectServiceImpl extends ServiceImpl<ProjectRepository, Project> implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private ScoreItemRepository scoreItemRepository;
    
    @Autowired
    private ScoreItemRoleRepository scoreItemRoleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScoreRepository scoreRepository;
    
    @Autowired
    private ScoreService scoreService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取所有项目
     */
    @Override
    public List<ProjectDTO> getAllProjects() {
        List<Project> projects = projectRepository.findAllByOrderByDisplayOrderAsc();
    List<ProjectDTO> projectDTOs = new ArrayList<>();
    for (Project project : projects) {
        // 查询并设置评分项
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(project.getId());
        for (ScoreItem scoreItem : scoreItems) {
            List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
            scoreItem.setRoles(roles);
            if (roles != null && !roles.isEmpty()) {
                scoreItem.setRole(roles.get(0));
            }
        }
        project.setScoreItems(scoreItems);
        ProjectDTO dto = convertToDTO(project);

        // 组装scoreGroups
        if (scoreItems != null) {
            List<ScoreItemDTO> scoreItemDTOs = scoreItems.stream()
                .map(this::convertToScoreItemDTO)
                .collect(Collectors.toList());
            Map<String, List<Map<String, Object>>> scoreGroups = new HashMap<>();
            for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
                List<Map<String, Object>> items = scoreItemDTOs.stream()
                    .filter(item -> groupType.equals(item.getGroupType()))
                    .map(item -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", item.getName());
                        map.put("minScore", item.getMinScore());
                        map.put("maxScore", item.getMaxScore());
                        map.put("roles", item.getRoles());
                        return map;
                    })
                    .collect(Collectors.toList());
                scoreGroups.put(groupType, items);
            }
            dto.setScoreGroups(scoreGroups);
        }
        projectDTOs.add(dto);
    }
    return projectDTOs;
    }
    
    /**
     * 根据ID获取项目
     */
    @Override
    public ProjectDTO getProjectById(Long projectId) {
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            return null;
        }
        
        // 查询关联的评分项
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
        
        // 查询每个评分项的角色列表
        for (ScoreItem scoreItem : scoreItems) {
            List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
            scoreItem.setRoles(roles);
            if (roles != null && !roles.isEmpty()) {
                // 为了兼容旧代码，设置第一个角色作为主角色
                scoreItem.setRole(roles.get(0));
            }
        }
        
        project.setScoreItems(scoreItems);
        
        ProjectDTO dto = convertToDTO(project);
        
        if (project.getScoreItems() != null) {
            List<ScoreItemDTO> scoreItemDTOs = scoreItems.stream()
            .map(this::convertToScoreItemDTO)
            .collect(Collectors.toList());
        Map<String, List<Map<String, Object>>> scoreGroups = new HashMap<>();
        for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
            List<Map<String, Object>> items = scoreItemDTOs.stream()
                .filter(item -> groupType.equals(item.getGroupType()))
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", item.getName());
                    map.put("minScore", item.getMinScore());
                    map.put("maxScore", item.getMaxScore());
                    map.put("roles", item.getRoles());
                    return map;
                })
                .collect(Collectors.toList());
            scoreGroups.put(groupType, items);
        }
        dto.setScoreGroups(scoreGroups);
        }
        
        return dto;
    }
    
    /**
     * 保存项目
     */
    @Override
    public ProjectDTO saveProject(ProjectDTO projectDTO) {
        Project project;
        boolean isNew = projectDTO.getId() == null;
        
        if (isNew) {
            project = new Project();
            project.setCreateTime(new Date());
        } else {
            project = projectRepository.selectById(projectDTO.getId());
            if (project == null) {
                return null;
            }
        }
        
        // 更新字段
        project.setName(projectDTO.getName());
        project.setDescription(projectDTO.getDescription());
        project.setStatus(projectDTO.getStatus() != null ? projectDTO.getStatus() : "draft");
        project.setDisplayOrder(projectDTO.getDisplayOrder() != null ? projectDTO.getDisplayOrder() : 0);
        project.setUpdateTime(new Date());
        project.setUnit(projectDTO.getUnit());
        project.setLeader(projectDTO.getLeader());
        
        // 保存项目
        if (isNew) {
            projectRepository.insert(project);
        } else {
            projectRepository.updateById(project);
        }
        
        // 处理评分项数据
        if (projectDTO.getScoreGroups() != null) {
            // 删除旧的评分项
            if (!isNew) {
                QueryWrapper<ScoreItem> scoreItemWrapper = new QueryWrapper<>();
                scoreItemWrapper.eq("project_id", project.getId());
                List<ScoreItem> oldScoreItems = scoreItemRepository.selectList(scoreItemWrapper);
                
                // 删除每个评分项的角色关联
                for (ScoreItem scoreItem : oldScoreItems) {
                    scoreItemRoleRepository.deleteByScoreItemId(scoreItem.getId());
                }
                
                // 删除评分详情记录（如果存在score_details表）
                for (ScoreItem scoreItem : oldScoreItems) {
                    try {
                        jdbcTemplate.update("DELETE FROM score_details WHERE score_item_id = ?", scoreItem.getId());
                    } catch (Exception e) {
                        // 如果表不存在或没有数据，忽略错误
                        System.out.println("score_details表可能不存在或为空: " + e.getMessage());
                    }
                }
                
                // 删除旧的评分项
                scoreItemRepository.delete(scoreItemWrapper);
            }
            
            // 保存新的评分项
            int displayOrder = 1;
            for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
                List<Map<String, Object>> groupItems = projectDTO.getScoreGroups().get(groupType);
                if (groupItems != null) {
                    for (Map<String, Object> itemData : groupItems) {
                        ScoreItem scoreItem = new ScoreItem();
                        scoreItem.setProjectId(project.getId());
                        scoreItem.setName((String) itemData.get("name"));
                        scoreItem.setGroupType(groupType);
                        scoreItem.setDisplayOrder(displayOrder++);
                        
                        // 设置最小值和最大值
                        Object minScoreObj = itemData.get("minScore");
                        if (minScoreObj != null) {
                            if (minScoreObj instanceof Integer) {
                                scoreItem.setMinScore((Integer) minScoreObj);
                            } else if (minScoreObj instanceof String) {
                                scoreItem.setMinScore(Integer.parseInt((String) minScoreObj));
                            }
                        } else {
                            scoreItem.setMinScore(0); // 默认最小值
                        }
                        
                        Object maxScoreObj = itemData.get("maxScore");
                        if (maxScoreObj != null) {
                            if (maxScoreObj instanceof Integer) {
                                scoreItem.setMaxScore((Integer) maxScoreObj);
                            } else if (maxScoreObj instanceof String) {
                                scoreItem.setMaxScore(Integer.parseInt((String) maxScoreObj));
                            }
                        } else {
                            scoreItem.setMaxScore(100); // 默认最大值
                        }
                        
                        // 保存评分项
                        scoreItemRepository.insert(scoreItem);
                        
                        // 保存角色关联
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) itemData.get("roles");
                        if (roles != null && !roles.isEmpty()) {
                            for (String role : roles) {
                                ScoreItemRole scoreItemRole = new ScoreItemRole();
                                scoreItemRole.setScoreItemId(scoreItem.getId());
                                scoreItemRole.setRole(role);
                                scoreItemRoleRepository.insert(scoreItemRole);
                            }
                        }
                    }
                }
            }
        }
        
        return convertToDTO(project);
    }
    
    /**
     * 删除项目
     */
    @Override
    public void deleteProject(Long projectId) {
        // 获取项目关联的所有评分项
        QueryWrapper<ScoreItem> scoreItemWrapper = new QueryWrapper<>();
        scoreItemWrapper.eq("project_id", projectId);
        List<ScoreItem> scoreItems = scoreItemRepository.selectList(scoreItemWrapper);
        
        // 删除每个评分项的角色关联
        for (ScoreItem scoreItem : scoreItems) {
            scoreItemRoleRepository.deleteByScoreItemId(scoreItem.getId());
        }
        
        // 删除评分详情记录（如果存在score_details表）
        for (ScoreItem scoreItem : scoreItems) {
            try {
                jdbcTemplate.update("DELETE FROM score_details WHERE score_item_id = ?", scoreItem.getId());
            } catch (Exception e) {
                // 如果表不存在或没有数据，忽略错误
                System.out.println("score_details表可能不存在或为空: " + e.getMessage());
            }
        }
        
        // 删除项目关联的评分项
        scoreItemRepository.delete(scoreItemWrapper);
        
        // 删除项目关联的评分
        QueryWrapper<Score> scoreWrapper = new QueryWrapper<>();
        scoreWrapper.eq("project_id", projectId);
        scoreRepository.delete(scoreWrapper);
        
        // 删除项目关联的任务关系
        jdbcTemplate.update("DELETE FROM task_projects WHERE project_id = ?", projectId);
        
        // 删除项目
        projectRepository.deleteById(projectId);
    }
    
    /**
     * 根据任务ID获取项目列表
     */
    @Override
    public List<ProjectDTO> getProjectsByTask(Long taskId) {
        List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
        List<ProjectDTO> projectDTOs = new ArrayList<>();
        for (Project project : projects) {
            // 新增：查找并设置评分项
            List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(project.getId());
            for (ScoreItem scoreItem : scoreItems) {
                List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
                scoreItem.setRoles(roles);
                if (roles != null && !roles.isEmpty()) {
                    scoreItem.setRole(roles.get(0));
                }
            }
            project.setScoreItems(scoreItems);
            projectDTOs.add(convertToDTO(project));
        }
        return projectDTOs;
    }
    
    /**
     * 根据用户角色获取评分项
     * 只返回用户角色有权限查看的评分项，确保安全性
     */
    @Override
    public List<ScoreItemDTO> getScoreItemsByUserRole(Long projectId, String username) {
        // 查询用户
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ArrayList<>();
        }
        // 防止空指针异常
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
        // 查询与用户角色相关的评分项
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectIdAndRole(projectId, role);
        // 严格按角色过滤，如果用户角色没有对应的评分项，返回空列表
        if (scoreItems == null || scoreItems.isEmpty()) {
            return new ArrayList<>();
        }
        // 为每个评分项设置角色信息
        for (ScoreItem scoreItem : scoreItems) {
            List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
            scoreItem.setRoles(roles);
            scoreItem.setRole(role); // 设置当前用户的角色
        }
        return scoreItems.stream()
                .map(this::convertToScoreItemDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据项目ID获取评分项
     */
    @Override
    public List<ScoreItemDTO> getScoreItemsByProjectId(Long projectId) {
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
        
        if (scoreItems == null) {
            return new ArrayList<>();
        }
        
        // 为每个评分项设置角色信息
        for (ScoreItem scoreItem : scoreItems) {
            List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
            scoreItem.setRoles(roles);
            if (roles != null && !roles.isEmpty()) {
                scoreItem.setRole(roles.get(0));
            }
        }
        
        return scoreItems.stream()
                .map(this::convertToScoreItemDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 批量更新项目状态
     */
    @Override
    public void batchUpdateStatus(List<Long> projectIds, String status) {
        if (projectIds == null || projectIds.isEmpty() || status == null) {
            return;
        }
        
        for (Long projectId : projectIds) {
            Project project = projectRepository.selectById(projectId);
            if (project != null) {
                project.setStatus(status);
                project.setUpdateTime(new Date());
                projectRepository.updateById(project);
            }
        }
    }
    
    /**
     * 批量删除项目
     */
    @Override
    public void batchDelete(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return;
        }
        
        for (Long projectId : projectIds) {
            // 获取项目关联的所有评分项
            QueryWrapper<ScoreItem> scoreItemWrapper = new QueryWrapper<>();
            scoreItemWrapper.eq("project_id", projectId);
            List<ScoreItem> scoreItems = scoreItemRepository.selectList(scoreItemWrapper);
            
            // 删除每个评分项的角色关联
            for (ScoreItem scoreItem : scoreItems) {
                scoreItemRoleRepository.deleteByScoreItemId(scoreItem.getId());
            }
            
            // 删除评分详情记录（如果存在score_details表）
            for (ScoreItem scoreItem : scoreItems) {
                try {
                    jdbcTemplate.update("DELETE FROM score_details WHERE score_item_id = ?", scoreItem.getId());
                } catch (Exception e) {
                    // 如果表不存在或没有数据，忽略错误
                    System.out.println("score_details表可能不存在或为空: " + e.getMessage());
                }
            }
            
            // 删除项目关联的评分项
            scoreItemRepository.delete(scoreItemWrapper);
            
            // 删除项目关联的评分
            QueryWrapper<Score> scoreWrapper = new QueryWrapper<>();
            scoreWrapper.eq("project_id", projectId);
            scoreRepository.delete(scoreWrapper);
            
            // 删除项目关联的任务关系
            jdbcTemplate.update("DELETE FROM task_projects WHERE project_id = ?", projectId);
            
            // 删除项目
            projectRepository.deleteById(projectId);
        }
    }
    
    /**
     * 更新项目顺序
     */
    @Override
    public void updateOrder(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < projectIds.size(); i++) {
            Long projectId = projectIds.get(i);
            Project project = projectRepository.selectById(projectId);
            if (project != null) {
                project.setDisplayOrder(i + 1);
                project.setUpdateTime(new Date());
                projectRepository.updateById(project);
            }
        }
    }
    
    /**
     * 获取项目评分进度
     */
    @Override
    public Map<String, Object> getProjectProgress(Long projectId) {
        Map<String, Object> progress = new HashMap<>();
        
        // 获取项目
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            progress.put("totalExperts", 0);
            progress.put("completedExperts", 0);
            progress.put("completionPercentage", 0.0);
            return progress;
        }
        
        // 获取项目关联的任务
        List<Map<String, Object>> taskProjects = jdbcTemplate.queryForList(
                "SELECT task_id FROM task_projects WHERE project_id = ?", projectId);
        
        if (taskProjects.isEmpty()) {
            progress.put("totalExperts", 0);
            progress.put("completedExperts", 0);
            progress.put("completionPercentage", 0.0);
            return progress;
        }
        
        Long taskId = (Long) taskProjects.get(0).get("task_id");
        
        // 获取任务关联的专家
        List<Map<String, Object>> taskExperts = jdbcTemplate.queryForList(
                "SELECT expert_username FROM task_experts WHERE task_id = ?", taskId);
        
        int totalExperts = taskExperts.size();
        int completedScores = 0;
        
        // 计算已完成评分的专家数量和收集专家列表
        List<String> scoredExperts = new ArrayList<>();
        for (Map<String, Object> expert : taskExperts) {
            String username = (String) expert.get("expert_username");
            List<Map<String, Object>> scores = jdbcTemplate.queryForList(
                    "SELECT * FROM scores WHERE project_id = ? AND user_id = ? AND is_draft = 0",
                    projectId, username);
            
            if (!scores.isEmpty()) {
                completedScores++;
                scoredExperts.add(username);
            }
        }
        
        int total = totalExperts;
        int completed = completedScores;
        double percentage = total > 0 ? (double) completed / total * 100 : 0.0;
        
        progress.put("totalExperts", total);
        progress.put("completedExperts", completed);
        progress.put("completionPercentage", percentage);
        progress.put("scoredExperts", scoredExperts);
        progress.put("completed", completed >= total && total > 0);
        
        return progress;
    }
    
    /**
     * 获取项目评分进度（指定任务）
     */
    @Override
    public Map<String, Object> getProjectProgress(Long projectId, Long taskId) {
        Map<String, Object> progress = new HashMap<>();
        
        // 获取项目
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            progress.put("totalExperts", 0);
            progress.put("completedExperts", 0);
            progress.put("completionPercentage", 0.0);
            return progress;
        }
        
        // 验证项目是否属于指定任务
        List<Map<String, Object>> taskProjects = jdbcTemplate.queryForList(
                "SELECT task_id FROM task_projects WHERE project_id = ? AND task_id = ?", 
                projectId, taskId);
        
        if (taskProjects.isEmpty()) {
            progress.put("totalExperts", 0);
            progress.put("completedExperts", 0);
            progress.put("completionPercentage", 0.0);
            return progress;
        }
        
        // 获取任务关联的专家
        List<Map<String, Object>> taskExperts = jdbcTemplate.queryForList(
                "SELECT expert_username FROM task_experts WHERE task_id = ?", taskId);
        
        int totalExperts = taskExperts.size();
        int completedScores = 0;
        
        // 计算已完成评分的专家数量和收集专家列表（指定任务）
        List<String> scoredExperts = new ArrayList<>();
        for (Map<String, Object> expert : taskExperts) {
            String username = (String) expert.get("expert_username");
            List<Map<String, Object>> scores = jdbcTemplate.queryForList(
                    "SELECT * FROM scores WHERE project_id = ? AND task_id = ? AND user_id = ? AND is_draft = 0",
                    projectId, taskId, username);
            
            if (!scores.isEmpty()) {
                completedScores++;
                scoredExperts.add(username);
            }
        }
        
        int total = totalExperts;
        int completed = completedScores;
        double percentage = total > 0 ? (double) completed / total * 100 : 0.0;
        
        progress.put("totalExperts", total);
        progress.put("completedExperts", completed);
        progress.put("completionPercentage", percentage);
        progress.put("scoredExperts", scoredExperts);
        progress.put("completed", completed >= total && total > 0);
        
        return progress;
    }
    
    /**
     * 获取项目评分详情
     */
    @Override
    public Map<String, Object> getProjectScores(Long projectId, Long taskId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取项目基本信息
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            result.put("error", "项目不存在");
            return result;
        }
        
        result.put("project", convertToDTO(project));
        
        // 获取评分统计信息
        Map<String, Object> scoreStatistics;
        if (taskId != null) {
            scoreStatistics = scoreService.getProjectScoreStatistics(projectId, taskId);
        } else {
            scoreStatistics = scoreService.getProjectScoreStatistics(projectId);
        }
        result.put("statistics", scoreStatistics);
        
        // 获取项目评分进度
        Map<String, Object> progress;
        if (taskId != null) {
            progress = getProjectProgress(projectId, taskId);
        } else {
            progress = getProjectProgress(projectId);
        }
        result.put("progress", progress);
        
        return result;
    }
    
    /**
     * 转换Project实体到ProjectDTO
     */
    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStatus(project.getStatus());
        dto.setDisplayOrder(project.getDisplayOrder());
        dto.setCreateTime(project.getCreateTime());
        dto.setUpdateTime(project.getUpdateTime());
        dto.setUnit(project.getUnit());
        dto.setLeader(project.getLeader());
        
        // 评分项
        if (project.getScoreItems() != null) {
            List<ScoreItemDTO> scoreItemDTOs = project.getScoreItems().stream()
                    .map(this::convertToScoreItemDTO)
                .collect(Collectors.toList());
            dto.setScoreItems(scoreItemDTOs);

            // 组装scoreGroups为Map
            Map<String, List<Map<String, Object>>> scoreGroups = new HashMap<>();
            for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
                List<Map<String, Object>> items = scoreItemDTOs.stream()
                    .filter(item -> groupType.equals(item.getGroupType()))
                    .map(item -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", item.getName());
                        map.put("minScore", item.getMinScore());
                        map.put("maxScore", item.getMaxScore());
                        map.put("roles", item.getRoles());
                        return map;
                    })
                    .collect(Collectors.toList());
                scoreGroups.put(groupType, items);
            }
            dto.setScoreGroups(scoreGroups);
        }
        return dto;
    }
    
    /**
     * 转换ScoreItem实体到ScoreItemDTO
     */
    private ScoreItemDTO convertToScoreItemDTO(ScoreItem scoreItem) {
        ScoreItemDTO dto = new ScoreItemDTO();
        dto.setId(scoreItem.getId());
        dto.setProjectId(scoreItem.getProjectId());
        dto.setName(scoreItem.getName());
        dto.setDescription(scoreItem.getDescription());
        dto.setRole(scoreItem.getRole());
        dto.setWeight(scoreItem.getWeight());
        dto.setMinScore(scoreItem.getMinScore());
        dto.setMaxScore(scoreItem.getMaxScore());
        dto.setDisplayOrder(scoreItem.getDisplayOrder());
        dto.setGroupType(scoreItem.getGroupType());
        
        // 传递角色列表
        dto.setRoles(scoreItem.getRoles());
        
        return dto;
    }

    /**
     * 只查主表，不查关联的简化项目列表
     */
    @Override
    public List<ProjectDTO> getAllSimpleProjects() {
        List<Project> projects = projectRepository.findAllByOrderByDisplayOrderAsc();
        return projects.stream().map(project -> {
            ProjectDTO dto = new ProjectDTO();
            dto.setId(project.getId());
            dto.setName(project.getName());
            dto.setStatus(project.getStatus());
            dto.setUnit(project.getUnit());
            dto.setLeader(project.getLeader());
            dto.setCreateTime(project.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> getSimpleProjectsByTask(Long taskId) {
        List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
        return projects.stream().map(project -> {
            ProjectDTO dto = new ProjectDTO();
            dto.setId(project.getId());
            dto.setName(project.getName());
            dto.setStatus(project.getStatus());
            dto.setUnit(project.getUnit());
            dto.setLeader(project.getLeader());
            dto.setCreateTime(project.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
    }
} 