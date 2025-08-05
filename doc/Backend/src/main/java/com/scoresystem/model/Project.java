package com.scoresystem.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 项目实体类
 */
@TableName("projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String status;
    
    @TableField("display_order")
    private Integer displayOrder;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;

    private String unit;
    
    private String leader;
    
    @TableField("industry")
    private String industry;
    
    // 非数据库字段，关联的评分项列表
    @TableField(exist = false)
    private transient java.util.List<ScoreItem> scoreItems;

    @TableField(exist = false)
    private transient Integer isReviewed;
}