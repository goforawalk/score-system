package com.scoresystem.controller;

import com.scoresystem.dto.ScoreSystemModels.ApiResponse;
import com.scoresystem.dto.ScoreSystemModels.LoginRequest;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.dto.ScoreSystemModels.UserDTO;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.ScoreService;
import com.scoresystem.service.StatisticsService;
import com.scoresystem.service.TaskService;
import com.scoresystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评分系统API控制器
 * 提供评分系统的所有REST API接口
 */
@RestController
@RequestMapping("/")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ScoreSystemController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ScoreService scoreService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<UserDTO>> login(@RequestBody LoginRequest request) {
        UserDTO user = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "登录成功", user));
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(new ApiResponse<>(true, "登出成功", null));
    }
    
    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, "获取用户列表成功", users));
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody UserDTO userDTO) {
        UserDTO savedUser = userService.saveUser(userDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "创建用户成功", savedUser));
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/users/{username}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable String username, @RequestBody UserDTO userDTO) {
        userDTO.setUsername(username);
        UserDTO savedUser = userService.saveUser(userDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "更新用户成功", savedUser));
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.ok(new ApiResponse<>(true, "删除用户成功", null));
    }
    
    /**
     * 获取项目列表
     */
    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getProjects() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目列表成功", projects));
    }
    
    /**
     * 创建项目
     */
    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectDTO>> createProject(@RequestBody ProjectDTO projectDTO) {
        ProjectDTO savedProject = projectService.saveProject(projectDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "创建项目成功", savedProject));
    }
    
    /**
     * 更新项目
     */
    @PutMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectDTO>> updateProject(@PathVariable Long id, @RequestBody ProjectDTO projectDTO) {
        projectDTO.setId(id);
        ProjectDTO savedProject = projectService.saveProject(projectDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "更新项目成功", savedProject));
    }
    
    /**
     * 删除项目
     */
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "删除项目成功", null));
    }
    
    /**
     * 获取当前活动任务及项目
     */
    @GetMapping("/tasks/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveTasks(@RequestParam String username) {
        List<TaskDTO> userTasks = taskService.getTasksByUser(username);
        // 只取激活状态的任务
        TaskDTO activeTask = userTasks.stream()
            .filter(t -> "active".equals(t.getStatus()))
            .findFirst().orElse(null);
        Map<String, Object> response = new HashMap<>();
        
        if (activeTask == null) {
            response.put("task", null);
            response.put("projectsInOrder", new ArrayList<>());
            return ResponseEntity.ok(new ApiResponse<>(true, "当前无活动任务", response));
        }
        
        List<ProjectDTO> projectsInOrder = projectService.getProjectsByTask(activeTask.getId());
        
        response.put("task", activeTask);
        response.put("projectsInOrder", projectsInOrder);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取活动任务及项目成功", response));
    }
    
    /**
     * 提交评分
     */
    @PostMapping("/scores")
    public ResponseEntity<ApiResponse<ScoreDTO>> submitScore(@RequestBody ScoreRequest request) {
        ScoreDTO score = scoreService.saveScore(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "提交评分成功", score));
    }
    
    /**
     * 获取评分历史
     */
    @GetMapping("/scores/history")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoreHistory(
            @RequestParam Long projectId, 
            @RequestParam String username,
            @RequestParam(required = false) Long taskId) {
        List<ScoreDTO> scores;
        if (taskId != null) {
            scores = scoreService.getScoreHistory(projectId, taskId, username);
        } else {
            scores = scoreService.getScoreHistory(projectId, username);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "获取评分历史成功", scores));
    }
    
    /**
     * 获取统计数据
     */
    /**@GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(new ApiResponse<>(true, "获取统计数据成功", statistics));
    }**/
    /**
     * 获取统计数据（支持taskId过滤）
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStatistics(@RequestParam(required = false) Long taskId) {
        List<Map<String, Object>> statistics;
        if (taskId != null) {
            statistics = statisticsService.getProjectStatistics(taskId);
        } else {
            statistics = statisticsService.getProjectStatistics();
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取统计数据成功", statistics));
    }

    /**
     * 获取前端统计页面需要的完整统计数据
     */
    @GetMapping("/statistics/frontend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFrontendStatistics(@RequestParam(required = false) Long taskId) {
        List<Map<String, Object>> statistics;
        if (taskId != null) {
            statistics = statisticsService.getFrontendStatistics(taskId);
        } else {
            statistics = statisticsService.getFrontendStatistics();
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取前端统计数据成功", statistics));
    }

    /**
     * 获取任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务列表成功", tasks));
    }

    /**
     * 根据任务获取项目列表
     */
    @GetMapping("/projects/task/{taskId}")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getProjectsByTask(@PathVariable Long taskId) {
        List<ProjectDTO> projects = projectService.getProjectsByTask(taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务项目列表成功", projects));
    }

    /**
     * 根据任务获取评分数据
     */
    @GetMapping("/scores/task/{taskId}")
    public ResponseEntity<ApiResponse<List<ScoreDTO>>> getScoresByTask(@PathVariable Long taskId) {
        List<ScoreDTO> scores = scoreService.getScoresByTask(taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务评分数据成功", scores));
    }

    /**
     * 获取任务统计概览
     */
    @GetMapping("/statistics/task/{taskId}/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaskOverview(@PathVariable Long taskId) {
        Map<String, Object> overview = statisticsService.getTaskOverview(taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务统计概览成功", overview));
    }

    /**
     * 获取项目在任务下的统计详情
     */
    @GetMapping("/statistics/project/{projectId}/task/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectTaskStatistics(
            @PathVariable Long projectId, 
            @PathVariable Long taskId) {
        Map<String, Object> statistics = statisticsService.getProjectTaskStatistics(projectId, taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目任务统计详情成功", statistics));
    }

    /**
     * 获取评分项统计
     */
    @GetMapping("/statistics/score-items")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScoreItemStatistics(
            @RequestParam(required = false) Long taskId) {
        Map<String, Object> statistics;
        if (taskId != null) {
            statistics = statisticsService.getScoreItemStatistics(taskId);
        } else {
            statistics = statisticsService.getScoreItemStatistics();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "获取评分项统计成功", statistics));
    }

    /**
     * 获取专家评分统计
     */
    @GetMapping("/statistics/experts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExpertStatistics(
            @RequestParam(required = false) Long taskId) {
        Map<String, Object> statistics;
        if (taskId != null) {
            statistics = statisticsService.getExpertStatistics(taskId);
        } else {
            statistics = statisticsService.getExpertStatistics();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "获取专家评分统计成功", statistics));
    }

    /**
     * 导出统计数据
     */
    @PostMapping("/statistics/export/{taskId}")
    public ResponseEntity<ApiResponse<String>> exportStatistics(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> exportOptions) {
        String exportUrl = statisticsService.exportStatistics(taskId, exportOptions);
        return ResponseEntity.ok(new ApiResponse<>(true, "导出统计数据成功", exportUrl));
    }

    /**
     * 根据项目ID获取项目详情（含评分项）
     */
    @GetMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectDTO>> getProjectById(@PathVariable Long id) {
        ProjectDTO project = projectService.getProjectById(id);
        if (project == null) {
            return ResponseEntity.ok(new ApiResponse<>(false, "未找到指定项目", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "获取项目详情成功", project));
    }

    // 简化任务列表
    @GetMapping("/tasks/simple")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getSimpleTasks(@RequestParam(value = "withDetails", required = false, defaultValue = "false") boolean includeProjectCount) {
        List<TaskDTO> tasks = taskService.getAllSimpleTasks(includeProjectCount);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取简化任务列表成功", tasks));
    }

    // 简化项目列表
    @GetMapping("/projects/simple")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getSimpleProjects(
        @RequestParam(required = false) Long taskId) {
        List<ProjectDTO> projects;
        if (taskId != null) {
            projects = projectService.getSimpleProjectsByTask(taskId);
        } else {
            projects = projectService.getAllSimpleProjects();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "获取简化项目列表成功", projects));
    }

    /**
     * 获取指定评审任务下所有项目的进度和总分
     */
    @GetMapping("/tasks/{taskId}/progress-scores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTaskProjectProgressAndScores(@PathVariable Long taskId) {
        List<Map<String, Object>> result = taskService.getTaskProjectProgressAndScores(taskId);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取任务下项目进度和评分成功", result));
    }
} 