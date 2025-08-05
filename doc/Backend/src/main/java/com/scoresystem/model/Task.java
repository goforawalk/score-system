package com.scoresystem.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 任务实体类
 * 表示一个评审任务，包含任务类型、状态、时间范围和相关专家
 */
@TableName("tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("task_id")
    private String taskId;
    
    private String category;
    
    @TableField("task_type")
    private Integer taskType;
    
    @TableField("score_group_type")
    private Integer scoreGroupType;
    
    private String status;
    
    @TableField("switch_mode")
    private Integer switchMode; // 1=自动切换，2=手动切换
    
    @TableField("start_time")
    private Date startTime;
    
    @TableField("end_time")
    private Date endTime;
    
    // 以下字段不直接映射到表中，需要单独处理
    @TableField(exist = false)
    private List<String> experts;
    
    @TableField(exist = false)
    private List<Project> projects;
    
    @TableField("update_time")
    private Date updateTime;
}