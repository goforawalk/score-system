package com.scoresystem.service;

import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import java.util.List;
import java.util.Map;

/**
 * 项目服务接口
 */
public interface ProjectService {
    
    /**
     * 获取所有项目
     * 
     * @return 项目DTO列表
     */
    List<ProjectDTO> getAllProjects();
    
    /**
     * 根据ID获取项目
     * 
     * @param projectId 项目ID
     * @return 项目DTO
     */
    ProjectDTO getProjectById(Long projectId);
    
    /**
     * 保存项目（创建或更新）
     * 
     * @param projectDTO 项目DTO
     * @return 保存后的项目DTO
     */
    ProjectDTO saveProject(ProjectDTO projectDTO);
    
    /**
     * 删除项目
     * 
     * @param projectId 项目ID
     */
    void deleteProject(Long projectId);
    
    /**
     * 根据任务ID获取项目列表
     * 
     * @param taskId 任务ID
     * @return 项目DTO列表
     */
    List<ProjectDTO> getProjectsByTask(Long taskId);
    
    /**
     * 根据用户角色获取项目评分项
     * 
     * @param projectId 项目ID
     * @param username 用户名
     * @return 评分项DTO列表
     */
    List<ScoreItemDTO> getScoreItemsByUserRole(Long projectId, String username);
    
    /**
     * 根据项目ID获取评分项
     */
    List<ScoreItemDTO> getScoreItemsByProjectId(Long projectId);
    
    /**
     * 批量更新项目状态
     * 
     * @param projectIds 项目ID列表
     * @param status 状态
     */
    void batchUpdateStatus(List<Long> projectIds, String status);
    
    /**
     * 批量删除项目
     * 
     * @param projectIds 项目ID列表
     */
    void batchDelete(List<Long> projectIds);
    
    /**
     * 更新项目顺序
     * 
     * @param projectIds 项目ID列表（按顺序排列）
     */
    void updateOrder(List<Long> projectIds);
    
    /**
     * 获取项目评分进度
     * 
     * @param projectId 项目ID
     * @return 评分进度数据
     */
    Map<String, Object> getProjectProgress(Long projectId);
    
    /**
     * 获取项目评分进度（指定任务）
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 评分进度数据
     */
    Map<String, Object> getProjectProgress(Long projectId, Long taskId);
    
    /**
     * 获取项目评分详情
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID（可选）
     * @return 项目评分详情
     */
    Map<String, Object> getProjectScores(Long projectId, Long taskId);

    /**
     * 获取所有简化项目
     */
    List<ProjectDTO> getAllSimpleProjects();

    /**
     * 根据任务ID获取简化项目列表
     * 
     * @param taskId 任务ID
     * @return 项目DTO列表
     */
    List<ProjectDTO> getSimpleProjectsByTask(Long taskId);
} 