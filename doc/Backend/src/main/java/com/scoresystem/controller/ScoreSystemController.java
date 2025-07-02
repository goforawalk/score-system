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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveTasks() {
        TaskDTO activeTask = taskService.getActiveTask();
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
            @RequestParam String username) {
        List<ScoreDTO> scores = scoreService.getScoreHistory(projectId, username);
        return ResponseEntity.ok(new ApiResponse<>(true, "获取评分历史成功", scores));
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(new ApiResponse<>(true, "获取统计数据成功", statistics));
    }
} 