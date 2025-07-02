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
     * 获取仪表盘统计数据
     */
    Map<String, Object> getDashboardStatistics();
    
    /**
     * 获取项目统计数据
     */
    List<Map<String, Object>> getProjectStatistics();
    
    /**
     * 获取用户统计数据
     */
    List<Map<String, Object>> getUserStatistics();
    
    /**
     * 获取任务统计数据
     */
    List<Map<String, Object>> getTaskStatistics();
    
    /**
     * 获取评分统计数据
     */
    List<Map<String, Object>> getScoreStatistics();
    
    /**
     * 获取评分分布统计
     */
    Map<String, Integer> getScoreDistribution();
    
    /**
     * 获取时间段统计数据
     */
    Map<String, Object> getTimeRangeStatistics(Date startDate, Date endDate);
    
    /**
     * 获取单个项目的统计数据
     * @param projectId 项目ID
     * @return 项目统计数据
     */
    Map<String, Object> getProjectStatisticsById(Long projectId);
    
    /**
     * 获取单个用户的统计数据
     * @param username 用户名
     * @return 用户统计数据
     */
    Map<String, Object> getUserStatisticsByUsername(String username);
    
    /**
     * 获取单个任务的统计数据
     * @param taskId 任务ID
     * @return 任务统计数据
     */
    Map<String, Object> getTaskStatisticsById(Long taskId);
} 