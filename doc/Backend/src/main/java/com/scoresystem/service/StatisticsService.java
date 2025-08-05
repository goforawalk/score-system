package com.scoresystem.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取统计数据
     */
    Map<String, Object> getStatistics();
    
    /**
     * 获取统计数据（指定任务）
     * @param taskId 任务ID
     */
    Map<String, Object> getStatistics(Long taskId);
    
    /**
     * 获取仪表盘统计数据
     */
    Map<String, Object> getDashboardStatistics();
    
    /**
     * 获取仪表盘统计数据（指定任务）
     * @param taskId 任务ID
     */
    Map<String, Object> getDashboardStatistics(Long taskId);
    
    /**
     * 获取项目统计数据
     */
    List<Map<String, Object>> getProjectStatistics();
    
    /**
     * 获取项目统计数据（指定任务）
     * @param taskId 任务ID
     */
    List<Map<String, Object>> getProjectStatistics(Long taskId);
    
    /**
     * 获取用户统计数据
     */
    List<Map<String, Object>> getUserStatistics();
    
    /**
     * 获取用户统计数据（指定任务）
     * @param taskId 任务ID
     */
    List<Map<String, Object>> getUserStatistics(Long taskId);
    
    /**
     * 获取任务统计数据
     */
    List<Map<String, Object>> getTaskStatistics();
    
    /**
     * 获取评分统计数据
     */
    List<Map<String, Object>> getScoreStatistics();
    
    /**
     * 获取评分统计数据（指定任务）
     * @param taskId 任务ID
     */
    List<Map<String, Object>> getScoreStatistics(Long taskId);
    
    /**
     * 获取评分分布统计
     */
    Map<String, Integer> getScoreDistribution();
    
    /**
     * 获取评分分布统计（指定任务）
     * @param taskId 任务ID
     */
    Map<String, Integer> getScoreDistribution(Long taskId);
    
    /**
     * 获取时间段统计数据
     */
    Map<String, Object> getTimeRangeStatistics(Date startDate, Date endDate);
    
    /**
     * 获取时间段统计数据（指定任务）
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param taskId 任务ID
     */
    Map<String, Object> getTimeRangeStatistics(Date startDate, Date endDate, Long taskId);
    
    /**
     * 获取单个项目的统计数据
     * @param projectId 项目ID
     * @return 项目统计数据
     */
    Map<String, Object> getProjectStatisticsById(Long projectId);
    
    /**
     * 获取单个项目的统计数据（指定任务）
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 项目统计数据
     */
    Map<String, Object> getProjectStatisticsById(Long projectId, Long taskId);
    
    /**
     * 获取单个用户的统计数据
     * @param username 用户名
     * @return 用户统计数据
     */
    Map<String, Object> getUserStatisticsByUsername(String username);
    
    /**
     * 获取单个用户的统计数据（指定任务）
     * @param username 用户名
     * @param taskId 任务ID
     * @return 用户统计数据
     */
    Map<String, Object> getUserStatisticsByUsername(String username, Long taskId);
    
    /**
     * 获取单个任务的统计数据
     * @param taskId 任务ID
     * @return 任务统计数据
     */
    Map<String, Object> getTaskStatisticsById(Long taskId);

    /**
     * 获取任务统计概览
     * @param taskId 任务ID
     * @return 任务统计概览
     */
    Map<String, Object> getTaskOverview(Long taskId);

    /**
     * 获取项目在任务下的统计详情
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 项目任务统计详情
     */
    Map<String, Object> getProjectTaskStatistics(Long projectId, Long taskId);

    /**
     * 获取评分项统计
     * @return 评分项统计
     */
    Map<String, Object> getScoreItemStatistics();

    /**
     * 获取评分项统计（指定任务）
     * @param taskId 任务ID
     * @return 评分项统计
     */
    Map<String, Object> getScoreItemStatistics(Long taskId);

    /**
     * 获取专家评分统计
     * @return 专家评分统计
     */
    Map<String, Object> getExpertStatistics();

    /**
     * 获取专家评分统计（指定任务）
     * @param taskId 任务ID
     * @return 专家评分统计
     */
    Map<String, Object> getExpertStatistics(Long taskId);

    /**
     * 导出统计数据
     * @param taskId 任务ID
     * @param exportOptions 导出选项
     * @return 导出文件URL
     */
    String exportStatistics(Long taskId, Map<String, Object> exportOptions);

    /**
     * 获取前端统计页面需要的完整统计数据
     * @return 包含评分项详细统计、项目总分和完成率的统计数据
     */
    List<Map<String, Object>> getFrontendStatistics();

    /**
     * 获取前端统计页面需要的完整统计数据（指定任务）
     * @param taskId 任务ID
     * @return 包含评分项详细统计、项目总分和完成率的统计数据
     */
    List<Map<String, Object>> getFrontendStatistics(Long taskId);

    /**
     * 生成任务评分Excel
     * @param taskId 任务ID
     * @return 评分Excel文件字节数组
     */
    byte[] generateTaskExcel(Long taskId);
} 