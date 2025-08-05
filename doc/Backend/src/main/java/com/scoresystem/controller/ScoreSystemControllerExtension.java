package com.scoresystem.controller;

import com.scoresystem.dto.ScoreSystemModels.ApiResponse;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.ScoreService;
import com.scoresystem.service.StatisticsService;
import com.scoresystem.service.TaskService;
import com.scoresystem.service.TestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

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
    
    @Autowired
    private TestDataService testDataService;
    
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectProgress(@PathVariable Long id, @RequestParam(required = false) Long taskId) {
        Map<String, Object> progress = projectService.getProjectProgress(id, taskId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目评分进度成功", progress));
    }
    
    /**
     * 获取项目评分详情
     */
    @GetMapping("/projects/{id}/scores")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectScores(@PathVariable Long id, @RequestParam(required = false) Long taskId) {
        Map<String, Object> scores = projectService.getProjectScores(id, taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目评分详情成功", scores));
    }
    
    /**
     * 获取项目评分项
     */
    @GetMapping("/projects/{id}/score-items")
    public ResponseEntity<ApiResponse<List<ScoreItemDTO>>> getProjectScoreItems(@PathVariable Long id) {
        List<ScoreItemDTO> scoreItems = projectService.getScoreItemsByProjectId(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目评分项成功", scoreItems));
    }

    /**
     * 获取项目评分明细
     */
    @GetMapping("/projects/{projectId}/score-details")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProjectScoreDetails(
            @PathVariable Long projectId,
            @RequestParam Long taskId) {
        List<Map<String, Object>> details = projectService.getProjectScoreDetails(projectId, taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取评分明细成功", details));
    }
    
    // 任务管理扩展接口
    
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
    
    /**
     * 重置评审任务
     */
    @PutMapping("/tasks/{id}/reset")
    public ResponseEntity<ApiResponse<TaskDTO>> resetTask(@PathVariable Long id) {
        TaskDTO task = taskService.resetTask(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "重置评审任务成功", task));
    }

    /**
     * 调整任务项目顺序（仅手动切换模式且项目未评审时可用）
     */
    @PutMapping("/tasks/{id}/reorder-projects")
    public ResponseEntity<ApiResponse<TaskDTO>> reorderTaskProjects(
            @PathVariable Long id,
            @RequestBody List<Long> projectIds) {
        TaskDTO task = taskService.reorderTaskProjects(id, projectIds);
        return ResponseEntity.ok(new ApiResponse<>(true, "调整项目顺序成功", task));
    }

    /**
     * 获取任务项目顺序调整权限状态
     */
    @GetMapping("/tasks/{id}/reorder-permission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReorderPermission(@PathVariable Long id) {
        Map<String, Object> permission = taskService.getReorderPermission(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取权限状态成功", permission));
    }
    
    /**
     * 更新任务切换模式
     */
    @PutMapping("/tasks/{id}/switch-mode")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTaskSwitchMode(
            @PathVariable Long id, 
            @RequestBody Map<String, Integer> request) {
        Integer switchMode = request.get("switchMode");
        
        TaskDTO task = taskService.updateTaskSwitchMode(id, switchMode);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "更新任务切换模式成功", task));
    }
    
    /**
     * 检查任务完成状态
     */
    @GetMapping("/tasks/{id}/completion-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkTaskCompletionStatus(@PathVariable Long id) {
        Map<String, Object> status = taskService.checkTaskCompletionStatus(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务完成状态成功", status));
    }

    /**
     * 导出任务Excel
     */
    @GetMapping("/tasks/{taskId}/export-excel")
    public void exportTaskExcel(@PathVariable Long taskId, HttpServletResponse response) throws IOException {
        // 1. 生成Excel
        byte[] excelBytes = statisticsService.generateTaskExcel(taskId);

        // 2. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=task_" + taskId + "_scores.xlsx");
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
    
    // 评分扩展接口
    
    /**
     * 获取所有评分记录
     */
    @GetMapping("/scores")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScores(@RequestParam(required = false) Long taskId) {
        List<ScoreDTO> scores;
        if (taskId != null) {
            scores = scoreService.getAllScores(taskId);
        } else {
            scores = scoreService.getAllScores();
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取所有评分记录成功", scores));
    }
    
    /**
     * 获取所有评分记录（别名）
     */
    @GetMapping("/scores/all")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getAllScores(@RequestParam(required = false) Long taskId) {
        List<ScoreDTO> scores;
        if (taskId != null) {
            scores = scoreService.getAllScores(taskId);
        } else {
            scores = scoreService.getAllScores();
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取所有评分记录成功", scores));
    }
    
    /**
     * 按项目获取评分
     */
    @GetMapping("/scores/project/{projectId}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByProject(@PathVariable Long projectId, @RequestParam(required = false) Long taskId) {
        List<ScoreDTO> scores;
        if (taskId != null) {
            scores = scoreService.getScoresByProject(projectId, taskId);
        } else {
            scores = scoreService.getScoresByProject(projectId);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "按项目获取评分成功", scores));
    }
    
    /**
     * 按用户获取评分
     */
    @GetMapping("/scores/user/{username}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByUser(@PathVariable String username, @RequestParam(required = false) Long taskId) {
        List<ScoreDTO> scores;
        if (taskId != null) {
            scores = scoreService.getScoresByUser(username, taskId);
        } else {
            scores = scoreService.getScoresByUser(username);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "按用户获取评分成功", scores));
    }
    
    /**
     * 按专家获取评分（别名）
     */
    @GetMapping("/scores/expert/{username}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByExpert(@PathVariable String username, @RequestParam(required = false) Long taskId) {
        List<ScoreDTO> scores;
        if (taskId != null) {
            scores = scoreService.getScoresByUser(username, taskId);
        } else {
            scores = scoreService.getScoresByUser(username);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "按专家获取评分成功", scores));
    }
    
    // 统计扩展接口
    
    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/statistics/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics(@RequestParam(required = false) Long taskId) {
        Map<String, Object> statistics;
        if (taskId != null) {
            statistics = statisticsService.getDashboardStatistics(taskId);
        } else {
            statistics = statisticsService.getDashboardStatistics();
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取仪表盘统计数据成功", statistics));
    }
    
    /**
     * 获取项目统计数据
     */
    @GetMapping("/statistics/projects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProjectStatistics(@RequestParam(required = false) Long taskId) {
        List<Map<String, Object>> statistics;
        if (taskId != null) {
            statistics = statisticsService.getProjectStatistics(taskId);
        } else {
            statistics = statisticsService.getProjectStatistics();
        }
        
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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getScoreStatistics(@RequestParam(required = false) Long taskId) {
        List<Map<String, Object>> statistics;
        if (taskId != null) {
            statistics = statisticsService.getScoreStatistics(taskId);
        } else {
            statistics = statisticsService.getScoreStatistics();
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取评分统计数据成功", statistics));
    }
    
    /**
     * 一键生成测试数据
     */
    @PostMapping("/test-data/generate")
    public ResponseEntity<ApiResponse<String>> generateTestData() {
        String result = testDataService.generateTestData();
        return ResponseEntity.ok(new ApiResponse<>(true, "测试数据生成成功", result));
    }
    
    @PostMapping("/tasks/{taskId}/manual-switch")
    public ResponseEntity<ApiResponse<Long>> manualSwitch(
            @PathVariable Long taskId,
            @RequestParam Long currentProjectId) {
        // 1. 标记当前项目已评审
        taskService.markProjectReviewed(taskId, currentProjectId);
        // 2. 获取当前顺序号
        Integer order = taskService.getProjectOrder(taskId, currentProjectId);
        // 3. 获取下一个项目
        Long nextProjectId = taskService.getNextProjectId(taskId, order);
        return ResponseEntity.ok(new ApiResponse<>(true, "切换成功", nextProjectId));
    }
    
} 