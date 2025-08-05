package com.scoresystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 评分系统数据传输对象集合
 */
public class ScoreSystemModels {

    /**
     * API响应通用结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
    }

    /**
     * 登录请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 用户DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private String username;
        private String password;
        private String role;
        private String name;
        private String email;
        private String department;
        private String token;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date createTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date updateTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date lastLoginTime;
    }

    /**
     * 项目DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectDTO {
        private Long id;
        private String name;
        private String description;
        private String status;
        private Integer displayOrder;
        private List<ScoreItemDTO> scoreItems;

        private Map<String, List<Map<String, Object>>> scoreGroups;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date createTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date updateTime;

        private String unit;
        private String leader;
        private String industry;

        // 新增：任务中间表is_reviewed
        private Integer isReviewed;
    }

    /**
     * 评分项DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreItemDTO {
        private Long id;
        private Long projectId;
        private String name;
        private String description;
        private Double weight;
        private String groupType;
        private Integer minScore;
        private Integer maxScore;
        private String role;
        private Integer displayOrder;
        private List<String> roles;
    }

    /**
     * 任务DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDTO {
        private Long id;
        private String taskId;
        private String category;
        private Integer taskType;
        private Integer scoreGroupType;
        private String status;
        private List<String> experts;
        private List<ProjectDTO> projects;
        private List<String> projectIds;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date endTime;
        
        private Integer projectCount;
        private Integer switchMode; // 1=自动切换，2=手动切换
    }

    /**
     * 评分请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreRequest {
        private Long projectId;
        private Long taskId;
        private String username;
        private Map<Long, Integer> scores;
        private Double totalScore;
        private String comments;
        private Boolean isDraft;
    }

    /**
     * 评分DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDTO {
        private Long id;
        private Long projectId;
        private Long taskId;
        private String username;
        private Map<Long, Integer> scores;
        private Double totalScore;
        private String comments;
        private Boolean isDraft;
        private String projectName;
        private String userFullName;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date createTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date updateTime;
    }
}