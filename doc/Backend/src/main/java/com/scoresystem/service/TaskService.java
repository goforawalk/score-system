package com.scoresystem.service;

import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import java.util.List;
import java.util.Map;

/**
 * 任务服务接口
 */
public interface TaskService {
    
    /**
     * 获取当前活动任务
     * 
     * @return 任务DTO
     */
    TaskDTO getActiveTask();
    
    /**
     * 获取所有任务
     * 
     * @return 任务DTO列表
     */
    List<TaskDTO> getAllTasks();
    
    /**
     * 根据ID获取任务
     * 
     * @param taskId 任务ID
     * @return 任务DTO
     */
    TaskDTO getTaskById(Long taskId);
    
    /**
     * 保存任务（创建或更新）
     * 
     * @param taskDTO 任务DTO
     * @return 保存后的任务DTO
     */
    TaskDTO saveTask(TaskDTO taskDTO);
    
    /**
     * 删除任务
     * 
     * @param taskId 任务ID
     */
    void deleteTask(Long taskId);
    
    /**
     * 获取用户相关的任务
     * 
     * @param username 用户名
     * @return 任务DTO列表
     */
    List<TaskDTO> getTasksByUser(String username);
    
    /**
     * 启用评审任务
     * 
     * @param taskId 任务ID
     * @return 更新后的任务DTO
     */
    TaskDTO enableTask(Long taskId);
    
    /**
     * 完成评审任务
     */
    TaskDTO completeTask(Long taskId, String username);
    
    /**
     * 重置评审任务
     * 
     * @param taskId 任务ID
     * @return 重置后的任务DTO
     */
    TaskDTO resetTask(Long taskId);

    /**
     * 调整任务项目顺序（仅手动切换模式且项目未评审时可用）
     */
    TaskDTO reorderTaskProjects(Long taskId, List<Long> projectIds);

    /**
     * 获取任务项目顺序调整权限状态
     */
    Map<String, Object> getReorderPermission(Long taskId);
    
    /**
     * 检查任务完成状态
     */
    Map<String, Object> checkTaskCompletionStatus(Long taskId);

    /**
     * 更新任务切换模式
     * 
     * @param taskId 任务ID
     * @param switchMode 切换模式（1=自动切换，2=手动切换）
     * @return 更新后的任务DTO
     */
    TaskDTO updateTaskSwitchMode(Long taskId, Integer switchMode);

    /**
     * 获取所有评审任务记录
     * 
     * @param includeProjectCount 是否包含项目数量
     * @return 任务DTO列表
     */
    List<TaskDTO> getAllSimpleTasks(boolean includeProjectCount);

    /**
     * 获取任务的项目进度和分数
     * @param taskId 任务ID
     * @return 项目进度和分数
     */
    List<Map<String, Object>> getTaskProjectProgressAndScores(Long taskId);

    /**
     * 获取项目顺序
     * @param taskId 任务ID
     * @param projectId 项目ID
     * @return 项目顺序
     */
    Integer getProjectOrder(Long taskId, Long projectId);

    /**
     * 获取下一个项目ID
     * @param taskId 任务ID
     * @param currentOrder 当前项目顺序
     * @return 下一个项目ID
     */
    Long getNextProjectId(Long taskId, int currentOrder);

    /**
     * 标记项目为已评审
     * @param taskId 任务ID
     * @param projectId 项目ID
     */
    void markProjectReviewed(Long taskId, Long projectId);
} 