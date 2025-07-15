package com.scoresystem.service;

import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import java.util.List;
import java.util.Map;

/**
 * 评分服务接口
 */
public interface ScoreService {
    
    /**
     * 保存评分（创建或更新）
     * 
     * @param scoreRequest 评分请求
     * @return 保存后的评分DTO
     */
    ScoreDTO saveScore(ScoreRequest scoreRequest);
    
    /**
     * 获取评分历史
     * 
     * @param projectId 项目ID
     * @param username 用户名
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoreHistory(Long projectId, String username);
    
    /**
     * 获取评分历史（指定任务）
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @param username 用户名
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoreHistory(Long projectId, Long taskId, String username);
    
    /**
     * 获取项目所有评分
     * 
     * @param projectId 项目ID
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoresByProject(Long projectId);
    
    /**
     * 获取项目所有评分（指定任务）
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoresByProject(Long projectId, Long taskId);
    
    /**
     * 获取用户所有评分
     * 
     * @param username 用户名
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoresByUser(String username);
    
    /**
     * 获取用户所有评分（指定任务）
     * 
     * @param username 用户名
     * @param taskId 任务ID
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoresByUser(String username, Long taskId);
    
    /**
     * 计算项目总评分
     * 
     * @param projectId 项目ID
     * @return 项目总评分
     */
    Double calculateProjectTotalScore(Long projectId);
    
    /**
     * 计算项目总评分（指定任务）
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 项目总评分
     */
    Double calculateProjectTotalScore(Long projectId, Long taskId);
    
    /**
     * 计算评分项总评分
     * 
     * @param projectId 项目ID
     * @param scoreItemId 评分项ID
     * @return 评分项总评分
     */
    Double calculateScoreItemAverage(Long projectId, Long scoreItemId);
    
    /**
     * 计算评分项总评分（指定任务）
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @param scoreItemId 评分项ID
     * @return 评分项总评分
     */
    Double calculateScoreItemAverage(Long projectId, Long taskId, Long scoreItemId);
    
    /**
     * 获取项目评分统计
     * 
     * @param projectId 项目ID
     * @return 评分统计数据
     */
    Map<String, Object> getProjectScoreStatistics(Long projectId);
    
    /**
     * 获取项目评分统计（指定任务）
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 评分统计数据
     */
    Map<String, Object> getProjectScoreStatistics(Long projectId, Long taskId);
    
    /**
     * 获取所有评分记录
     * 
     * @return 评分DTO列表
     */
    List<ScoreDTO> getAllScores();
    
    /**
     * 获取所有评分记录（指定任务）
     * 
     * @param taskId 任务ID
     * @return 评分DTO列表
     */
    List<ScoreDTO> getAllScores(Long taskId);
    
    /**
     * 按任务获取评分记录
     * 
     * @param taskId 任务ID
     * @return 评分DTO列表
     */
    List<ScoreDTO> getScoresByTask(Long taskId);
} 