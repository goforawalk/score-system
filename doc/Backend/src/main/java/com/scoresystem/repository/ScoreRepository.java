package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 根据项目ID查找评分列表
     * 
     * @param projectId 项目ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.project_id = #{projectId}")
    List<Score> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据用户名查找评分列表
     * 
     * @param username 用户名
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s JOIN users u ON s.user_id = u.username WHERE u.username = #{username}")
    List<Score> findByUsername(@Param("username") String username);
    
    /**
     * 根据草稿状态查找评分列表
     * 
     * @param isDraft 是否为草稿
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.is_draft = #{isDraft}")
    List<Score> findByIsDraft(@Param("isDraft") boolean isDraft);
    
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
     * 根据项目ID查找最终评分
     * 
     * @param projectId 项目ID
     * @return 评分实体列表
     */
    @Select("SELECT s.* FROM scores s WHERE s.project_id = #{projectId} AND s.is_draft = 0")
    List<Score> findFinalScoresByProjectId(@Param("projectId") Long projectId);

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
     * 统计指定时间段内创建的评分数量
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 评分数量
     */
    @Select("SELECT COUNT(*) FROM scores WHERE create_time BETWEEN #{startDate} AND #{endDate}")
    int countByCreateTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
} 