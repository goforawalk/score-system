package com.scoresystem.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评分项实体类
 * 表示项目中的一个评分项，包含权重、分数范围和允许评分的专家角色
 */
@TableName("score_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreItem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private Double weight;
    
    @TableField("min_score")
    private Integer minScore;
    
    @TableField("max_score")
    private Integer maxScore;
    
    @TableField(exist = false)
    private String role;
    
    @TableField("project_id")
    private Long projectId;
    
    @TableField("display_order")
    private Integer displayOrder;
    
    @TableField("group_type")
    private String groupType;
    
    // 非数据库字段
    @TableField(exist = false)
    private Project project;
    
    // 角色列表，存储多个角色
    @TableField(exist = false)
    private List<String> roles;
}