package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 项目数据访问接口
 */
@Mapper
public interface ProjectRepository extends BaseMapper<Project> {
    
    /**
     * 根据状态查找项目列表
     * 
     * @param status 状态
     * @return 项目实体列表
     */
    @Select("SELECT * FROM projects WHERE status = #{status}")
    List<Project> findByStatus(@Param("status") String status);
    
    /**
     * 根据显示顺序排序查找所有项目
     * 
     * @return 项目实体列表
     */
    @Select("SELECT * FROM projects ORDER BY display_order ASC")
    List<Project> findAllByOrderByDisplayOrderAsc();
    
    /**
     * 根据任务ID查找项目列表
     * 
     * @param taskId 任务ID
     * @return 项目实体列表
     */
    @Select("SELECT p.*, tp.is_reviewed FROM projects p " +
            "JOIN task_projects tp ON p.id = tp.project_id " +
            "WHERE tp.task_id = #{taskId} " +
            "ORDER BY tp.project_order ASC")
    List<Project> findByTaskIdOrderByDisplayOrderAsc(@Param("taskId") Long taskId);
    
    /**
     * 获取项目统计数据
     * 
     * @return 项目统计数据列表
     */
    @Select("SELECT p.id, p.name, p.status, " +
            "COUNT(DISTINCT s.id) AS score_count, " +
            "AVG(s.total_score) AS avg_score, " +
            "MIN(s.total_score) AS min_score, " +
            "MAX(s.total_score) AS max_score " +
            "FROM projects p " +
            "LEFT JOIN scores s ON p.id = s.project_id AND s.is_draft = 0 " +
            "GROUP BY p.id, p.name, p.status")
    List<Map<String, Object>> getProjectStatistics();
    
    /**
     * 统计指定时间段内创建的项目数量
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 项目数量
     */
    @Select("SELECT COUNT(*) FROM projects WHERE create_time BETWEEN #{startDate} AND #{endDate}")
    int countByCreateTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 清空所有项目数据
     */
    @Delete("DELETE FROM projects")
    void deleteAllProjects();
}
