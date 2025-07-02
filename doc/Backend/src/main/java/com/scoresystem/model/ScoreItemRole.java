package com.scoresystem.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评分项角色关联实体类
 * 表示评分项与角色之间的多对多关系
 */
@TableName("score_item_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreItemRole {
    
    @TableField("score_item_id")
    private Long scoreItemId;
    
    @TableField("role")
    private String role;
    
    // 非数据库字段
    @TableField(exist = false)
    private ScoreItem scoreItem;
}
