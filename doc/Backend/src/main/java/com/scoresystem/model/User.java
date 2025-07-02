package com.scoresystem.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户实体类
 */
@TableName("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @TableId
    private String username;
    
    private String password;
    
    private String role;
    
    private String name;
    
    private String email;
    
    private String department;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;
    
    @TableField("last_login_time")
    private Date lastLoginTime;
} 