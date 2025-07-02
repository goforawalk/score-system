package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务数据访问接口
 */
@Mapper
public interface TaskRepository extends BaseMapper<Task> {
    
    /**
     * 根据状态查找任务列表
     * 
     * @param status 状态
     * @return 任务实体列表
     */
    @Select("SELECT * FROM tasks WHERE status = #{status}")
    List<Task> findByStatus(@Param("status") String status);
    
    /**
     * 查找活动任务
     * 
     * @param status 状态
     * @param now 当前时间
     * @return 活动任务
     */
    @Select("SELECT TOP 1 * FROM tasks WHERE status = #{status} AND start_time <= #{now} AND end_time >= #{now}")
    Task findActiveTask(@Param("status") String status, @Param("now") Date now);
    
    /**
     * 根据专家用户名查找任务列表
     * 
     * @param username 专家用户名
     * @return 任务实体列表
     */
    @Select("SELECT t.* FROM tasks t " +
            "JOIN task_experts te ON t.id = te.task_id " +
            "WHERE te.expert_username = #{username}")
    List<Task> findByExpert(@Param("username") String username);
    
    /**
     * 根据任务编号查找任务
     * 
     * @param taskId 任务编号
     * @return 任务实体
     */
    @Select("SELECT * FROM tasks WHERE task_id = #{taskId}")
    Task findByTaskCode(@Param("taskId") String taskId);
    
    /**
     * 统计已完成任务数量
     * 
     * @return 已完成任务数量
     */
    @Select("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    int countCompletedTasks();
    
    /**
     * 获取任务统计数据
     * 
     * @return 任务统计数据列表
     */
    @Select("SELECT t.id, t.task_id AS task_code, t.category, t.status, " +
            "COUNT(DISTINCT te.expert_username) AS expert_count, " +
            "COUNT(DISTINCT tp.project_id) AS project_count, " +
            "COUNT(DISTINCT s.id) AS score_count " +
            "FROM tasks t " +
            "LEFT JOIN task_experts te ON t.id = te.task_id " +
            "LEFT JOIN task_projects tp ON t.id = tp.task_id " +
            "LEFT JOIN scores s ON tp.project_id = s.project_id AND te.expert_username = s.user_id " +
            "GROUP BY t.id, t.task_id, t.category, t.status")
    List<Map<String, Object>> getTaskStatistics();
    
    /**
     * 统计指定时间段内创建的任务数量
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 任务数量
     */
    @Select("SELECT COUNT(*) FROM tasks WHERE create_time BETWEEN #{startDate} AND #{endDate}")
    int countByCreateTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
} 