package com.scoresystem.service;

import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import java.util.List;

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
     * 
     * @param taskId 任务ID
     * @param username 用户名
     * @return 更新后的任务DTO
     */
    TaskDTO completeTask(Long taskId, String username);
} 