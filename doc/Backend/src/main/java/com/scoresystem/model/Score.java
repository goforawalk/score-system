package com.scoresystem.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * 评分实体类
 * 表示专家对项目的评分，包含各评分项的分数、总分和评论
 */
@TableName("scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("project_id")
    private Long projectId;
    
    @TableField("task_id")
    private Long taskId;
    
    @TableField("user_id")
    private String userId;
    
    @TableField(exist = false)
    private Map<Long, Integer> scores;
    
    @TableField("total_score")
    private Double totalScore;
    
    private String comments;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;
    
    @TableField("is_draft")
    private Boolean isDraft;
    
    // 非数据库字段，关联的项目和用户
    @TableField(exist = false)
    private Project project;
    
    @TableField(exist = false)
    private User user;
}