package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.ScoreItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 评分项数据访问接口
 */
@Mapper
public interface ScoreItemRepository extends BaseMapper<ScoreItem> {
    
    /**
     * 根据项目ID查找评分项列表
     * 
     * @param projectId 项目ID
     * @return 评分项实体列表
     */
    @Select("SELECT * FROM score_items WHERE project_id = #{projectId} ORDER BY display_order ASC")
    List<ScoreItem> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * 根据用户角色获取评分项
     * 使用多表联查从score_item_roles表中获取角色信息
     * 
     * @param projectId 项目ID
     * @param role 用户角色
     * @return 评分项实体列表
     */
    @Select("SELECT si.* FROM score_items si " +
            "JOIN score_item_roles sir ON si.id = sir.score_item_id " +
            "WHERE si.project_id = #{projectId} AND sir.role = #{role} " +
            "ORDER BY si.display_order ASC")
    List<ScoreItem> findByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") String role);

    /**
     * 清空所有评分项数据
     */
    @Delete("DELETE FROM score_items")
    void deleteAllScoreItems();
}