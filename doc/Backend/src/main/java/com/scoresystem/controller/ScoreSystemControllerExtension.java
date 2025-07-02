package com.scoresystem.controller;

import com.scoresystem.dto.ScoreSystemModels.ApiResponse;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.ScoreService;
import com.scoresystem.service.StatisticsService;
import com.scoresystem.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评分系统API控制器扩展
 * 实现前端需要但后端尚未实现的接口
 */
@RestController
@RequestMapping("/")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ScoreSystemControllerExtension {

    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ScoreService scoreService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    // 项目管理扩展接口
    
    /**
     * 批量更新项目状态
     */
    @PutMapping("/projects/batch-update")
    public ResponseEntity<ApiResponse<Void>> batchUpdateProjects(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Integer> intProjectIds = (List<Integer>) request.get("projectIds");
        // 将Integer类型的projectIds转换为Long类型
        List<Long> projectIds = intProjectIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        String status = (String) request.get("status");
        
        projectService.batchUpdateStatus(projectIds, status);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "批量更新项目状态成功", null));
    }
    
    /**
     * 批量删除项目
     */
    @PostMapping("/projects/batch-delete")
    public ResponseEntity<ApiResponse<Void>> batchDeleteProjects(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Integer> intProjectIds = (List<Integer>) request.get("projectIds");
        // 将Integer类型的projectIds转换为Long类型
        List<Long> projectIds = intProjectIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        
        projectService.batchDelete(projectIds);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "批量删除项目成功", null));
    }
    
    /**
     * 更新项目顺序
     */
    @PutMapping("/projects/order")
    public ResponseEntity<ApiResponse<Void>> updateProjectsOrder(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Integer> intProjectIds = (List<Integer>) request.get("projectIds");
        // 将Integer类型的projectIds转换为Long类型
        List<Long> projectIds = intProjectIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        
        projectService.updateOrder(projectIds);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "更新项目顺序成功", null));
    }
    
    /**
     * 获取项目评分进度
     */
    @GetMapping("/projects/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectProgress(@PathVariable Long id) {
        Map<String, Object> progress = projectService.getProjectProgress(id);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目评分进度成功", progress));
    }
    
    /**
     * 获取项目评分详情
     */
    @GetMapping("/projects/{id}/scores")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getProjectScores(@PathVariable Long id) {
        List<ScoreDTO> scores = scoreService.getScoresByProject(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目评分详情成功", scores));
    }
    
    // 任务管理扩展接口
    
    /**
     * 获取任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务列表成功", tasks));
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务详情成功", task));
    }
    
    /**
     * 创建评审任务
     */
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(@RequestBody TaskDTO taskDTO) {
        TaskDTO savedTask = taskService.saveTask(taskDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "创建评审任务成功", savedTask));
    }
    
    /**
     * 更新评审任务
     */
    @PutMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
        taskDTO.setId(id);
        TaskDTO updatedTask = taskService.saveTask(taskDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "更新评审任务成功", updatedTask));
    }
    
    /**
     * 启用评审任务
     */
    @PutMapping("/tasks/{id}/enable")
    public ResponseEntity<ApiResponse<TaskDTO>> enableTask(@PathVariable Long id) {
        TaskDTO task = taskService.enableTask(id);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "启用评审任务成功", task));
    }
    
    /**
     * 完成评审任务
     */
    @PutMapping("/tasks/{id}/complete")
    public ResponseEntity<ApiResponse<TaskDTO>> completeTask(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        TaskDTO task = taskService.completeTask(id, username);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "完成评审任务成功", task));
    }
    
    // 评分扩展接口
    
    /**
     * 获取所有评分记录
     */
    @GetMapping("/scores")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScores() {
        List<ScoreDTO> scores = scoreService.getAllScores();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取所有评分记录成功", scores));
    }
    
    /**
     * 获取所有评分记录（别名）
     */
    @GetMapping("/scores/all")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getAllScores() {
        List<ScoreDTO> scores = scoreService.getAllScores();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取所有评分记录成功", scores));
    }
    
    /**
     * 按项目获取评分
     */
    @GetMapping("/scores/project/{projectId}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByProject(@PathVariable Long projectId) {
        List<ScoreDTO> scores = scoreService.getScoresByProject(projectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "按项目获取评分成功", scores));
    }
    
    /**
     * 按用户获取评分
     */
    @GetMapping("/scores/user/{username}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByUser(@PathVariable String username) {
        List<ScoreDTO> scores = scoreService.getScoresByUser(username);
        return ResponseEntity.ok(new ApiResponse<>(true, "按用户获取评分成功", scores));
    }
    
    /**
     * 按专家获取评分（别名）
     */
    @GetMapping("/scores/expert/{username}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByExpert(@PathVariable String username) {
        List<ScoreDTO> scores = scoreService.getScoresByUser(username);
        return ResponseEntity.ok(new ApiResponse<>(true, "按专家获取评分成功", scores));
    }
    
    // 统计扩展接口
    
    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/statistics/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
        Map<String, Object> statistics = statisticsService.getDashboardStatistics();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取仪表盘统计数据成功", statistics));
    }
    
    /**
     * 获取项目统计数据
     */
    @GetMapping("/statistics/projects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProjectStatistics() {
        List<Map<String, Object>> statistics = statisticsService.getProjectStatistics();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目统计数据成功", statistics));
    }
    
    /**
     * 获取任务统计数据
     */
    @GetMapping("/statistics/tasks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTaskStatistics() {
        List<Map<String, Object>> statistics = statisticsService.getTaskStatistics();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务统计数据成功", statistics));
    }
    
    /**
     * 获取评分统计数据
     */
    @GetMapping("/statistics/scores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getScoreStatistics() {
        List<Map<String, Object>> statistics = statisticsService.getScoreStatistics();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取评分统计数据成功", statistics));
    }
} 