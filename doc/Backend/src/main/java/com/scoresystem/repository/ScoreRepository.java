package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 评分数据访问接口
 */
@Mapper
public interface ScoreRepository extends BaseMapper<Score> {
    
    /**
     * 根据项目ID和用户名查找评分列表
     * 
     * @param projectId 项目ID
     * @param username 用户名
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s JOIN projects p ON s.project_id = p.id JOIN users u ON s.user_id = u.username WHERE p.id = #{projectId} AND u.username = #{username} ORDER BY s.create_time DESC")
    List<Score> findByProjectIdAndUsername(@Param("projectId") Long projectId, @Param("username") String username);
    
    /**
     * 根据项目ID、任务ID和用户名查找评分列表
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @param username 用户名
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s JOIN projects p ON s.project_id = p.id JOIN users u ON s.user_id = u.username WHERE p.id = #{projectId} AND s.task_id = #{taskId} AND u.username = #{username} ORDER BY s.create_time DESC")
    List<Score> findByProjectIdAndTaskIdAndUsername(@Param("projectId") Long projectId, @Param("taskId") Long taskId, @Param("username") String username);
    
    /**
     * 根据项目ID、用户名和任务ID查找评分列表
     * 
     * @param projectId 项目ID
     * @param username 用户名
     * @param taskId 任务ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s JOIN projects p ON s.project_id = p.id JOIN users u ON s.user_id = u.username WHERE p.id = #{projectId} AND u.username = #{username} AND s.task_id = #{taskId} ORDER BY s.create_time DESC")
    List<Score> findByProjectIdAndUsernameAndTaskId(@Param("projectId") Long projectId, @Param("username") String username, @Param("taskId") Long taskId);
    
    /**
     * 根据项目ID查找评分列表
     * 
     * @param projectId 项目ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.project_id = #{projectId}")
    List<Score> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据项目ID和任务ID查找评分列表
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.project_id = #{projectId} AND s.task_id = #{taskId}")
    List<Score> findByProjectIdAndTaskId(@Param("projectId") Long projectId, @Param("taskId") Long taskId);
    
    /**
     * 根据用户名查找评分列表
     * 
     * @param username 用户名
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s JOIN users u ON s.user_id = u.username WHERE u.username = #{username}")
    List<Score> findByUsername(@Param("username") String username);
    
    /**
     * 根据用户名和任务ID查找评分列表
     * 
     * @param username 用户名
     * @param taskId 任务ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s JOIN users u ON s.user_id = u.username WHERE u.username = #{username} AND s.task_id = #{taskId}")
    List<Score> findByUsernameAndTaskId(@Param("username") String username, @Param("taskId") Long taskId);
    
    /**
     * 根据任务ID查找评分列表
     * 
     * @param taskId 任务ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.task_id = #{taskId}")
    List<Score> findByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据草稿状态查找评分列表
     * 
     * @param isDraft 是否为草稿
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.is_draft = #{isDraft}")
    List<Score> findByIsDraft(@Param("isDraft") boolean isDraft);
    
    /**
     * 根据草稿状态和任务ID查找评分列表
     * 
     * @param isDraft 是否为草稿
     * @param taskId 任务ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.is_draft = #{isDraft} AND s.task_id = #{taskId}")
    List<Score> findByIsDraftAndTaskId(@Param("isDraft") boolean isDraft, @Param("taskId") Long taskId);
    
    /**
     * 根据任务ID和草稿状态查找评分列表
     * 
     * @param taskId 任务ID
     * @param isDraft 是否为草稿
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.task_id = #{taskId} AND s.is_draft = #{isDraft}")
    List<Score> findByTaskIdAndIsDraft(@Param("taskId") Long taskId, @Param("isDraft") boolean isDraft);
    
    /**
     * 根据项目ID和评分项ID计算平均分
     * 
     * @param projectId 项目ID
     * @param scoreItemId 评分项ID
     * @return 平均分
     */
    @Select("SELECT AVG(ss.score_value) FROM scores s JOIN score_details ss ON s.id = ss.score_id " +
           "WHERE s.project_id = #{projectId} AND s.is_draft = 0 AND ss.score_item_id = #{scoreItemId}")
    Double calculateAverageScoreByProjectIdAndScoreItemId(@Param("projectId") Long projectId, @Param("scoreItemId") Long scoreItemId);
    
    /**
     * 根据项目ID、任务ID和评分项ID计算平均分
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @param scoreItemId 评分项ID
     * @return 平均分
     */
    @Select("SELECT AVG(ss.score_value) FROM scores s JOIN score_details ss ON s.id = ss.score_id " +
           "WHERE s.project_id = #{projectId} AND s.task_id = #{taskId} AND s.is_draft = 0 AND ss.score_item_id = #{scoreItemId}")
    Double calculateAverageScoreByProjectIdAndTaskIdAndScoreItemId(@Param("projectId") Long projectId, @Param("taskId") Long taskId, @Param("scoreItemId") Long scoreItemId);
    
    /**
     * 根据项目ID查找最终评分
     * 
     * @param projectId 项目ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.project_id = #{projectId} AND s.is_draft = 0")
    List<Score> findFinalScoresByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据项目ID和任务ID查找最终评分
     * 
     * @param projectId 项目ID
     * @param taskId 任务ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.project_id = #{projectId} AND s.task_id = #{taskId} AND s.is_draft = 0")
    List<Score> findFinalScoresByProjectIdAndTaskId(@Param("projectId") Long projectId, @Param("taskId") Long taskId);

    /**
     * 获取评分统计数据
     */
    @Select("SELECT p.name AS project_name, u.name AS expert_name, " +
            "s.total_score, s.is_draft, s.create_time, s.update_time " +
            "FROM scores s " +
            "JOIN projects p ON s.project_id = p.id " +
            "JOIN users u ON s.user_id = u.username " +
            "ORDER BY s.update_time DESC")
    List<Map<String, Object>> getScoreStatistics();
    
    /**
     * 获取评分统计数据（指定任务）
     * @param taskId 任务ID
     */
    @Select("SELECT p.name AS project_name, u.name AS expert_name, " +
            "s.total_score, s.is_draft, s.create_time, s.update_time " +
            "FROM scores s " +
            "JOIN projects p ON s.project_id = p.id " +
            "JOIN users u ON s.user_id = u.username " +
            "WHERE s.task_id = #{taskId} " +
            "ORDER BY s.update_time DESC")
    List<Map<String, Object>> getScoreStatisticsByTaskId(@Param("taskId") Long taskId);
    
    /**
     * 统计指定时间段内创建的评分数量
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 评分数量
     */
    @Select("SELECT COUNT(*) FROM scores WHERE create_time BETWEEN #{startDate} AND #{endDate}")
    int countByCreateTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 统计指定时间段内创建的评分数量（指定任务）
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param taskId 任务ID
     * @return 评分数量
     */
    @Select("SELECT COUNT(*) FROM scores WHERE create_time BETWEEN #{startDate} AND #{endDate} AND task_id = #{taskId}")
    int countByCreateTimeBetweenAndTaskId(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("taskId") Long taskId);

    /**
     * 清空所有评分数据
     */
    @Delete("DELETE FROM scores")
    void deleteAllScores();
} 