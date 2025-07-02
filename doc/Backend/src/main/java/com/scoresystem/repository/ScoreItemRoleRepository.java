package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.ScoreItemRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 评分项角色关联数据访问接口
 */
@Mapper
public interface ScoreItemRoleRepository extends BaseMapper<ScoreItemRole> {
    
    /**
     * 根据评分项ID查找角色列表
     * 
     * @param scoreItemId 评分项ID
     * @return 角色列表
     */
    @Select("SELECT role FROM score_item_roles WHERE score_item_id = #{scoreItemId}")
    List<String> findRolesByScoreItemId(@Param("scoreItemId") Long scoreItemId);
    
    /**
     * 根据评分项ID和角色查找记录
     * 
     * @param scoreItemId 评分项ID
     * @param role 角色
     * @return 评分项角色关联实体
     */
    @Select("SELECT * FROM score_item_roles WHERE score_item_id = #{scoreItemId} AND role = #{role}")
    ScoreItemRole findByScoreItemIdAndRole(@Param("scoreItemId") Long scoreItemId, @Param("role") String role);
    
    /**
     * 保存评分项角色关联
     * 
     * @param scoreItemId 评分项ID
     * @param role 角色
     */
    @Insert("INSERT INTO score_item_roles (score_item_id, role) VALUES (#{scoreItemId}, #{role})")
    void saveScoreItemRole(@Param("scoreItemId") Long scoreItemId, @Param("role") String role);
    
    /**
     * 删除评分项的所有角色关联
     * 
     * @param scoreItemId 评分项ID
     */
    @Delete("DELETE FROM score_item_roles WHERE score_item_id = #{scoreItemId}")
    void deleteByScoreItemId(@Param("scoreItemId") Long scoreItemId);
    
    /**
     * 删除评分项的特定角色关联
     * 
     * @param scoreItemId 评分项ID
     * @param role 角色
     */
    @Delete("DELETE FROM score_item_roles WHERE score_item_id = #{scoreItemId} AND role = #{role}")
    void deleteByScoreItemIdAndRole(@Param("scoreItemId") Long scoreItemId, @Param("role") String role);
    
    /**
     * 根据项目ID和角色查找评分项ID列表
     * 
     * @param projectId 项目ID
     * @param role 角色
     * @return 评分项ID列表
     */
    @Select("SELECT sir.score_item_id FROM score_item_roles sir " +
            "JOIN score_items si ON sir.score_item_id = si.id " +
            "WHERE si.project_id = #{projectId} AND sir.role = #{role}")
    List<Long> findScoreItemIdsByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") String role);
} 