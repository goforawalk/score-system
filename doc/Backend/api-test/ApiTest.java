package com.scoresystem.test;

import com.scoresystem.dto.ScoreSystemModels.LoginRequest;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.dto.ScoreSystemModels.UserDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 评分系统API接口测试类
 * 
 * 可以独立运行此测试类，不需要依赖Spring环境
 * 使用标准的Java HTTP连接和JSON处理来发送请求和解析响应
 * 
 * 使用方法：
 * 1. 确保评分系统后端已启动，并在8080端口监听
 * 2. 运行这个测试类
 * 3. 查看测试结果和日志
 */
public class ApiTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String authToken = null;
    
    /**
     * 测试运行入口
     */
    public static void main(String[] args) {
        ApiTest test = new ApiTest();
        try {
            // 必须先登录获取令牌
            test.testLogin();
            
            // 测试各个API接口
            test.testGetUsers();
            test.testGetProjects();
            test.testGetActiveTasks();
            test.testSubmitScore();
            test.testGetScoreHistory();
            test.testGetStatistics();
            
            System.out.println("所有测试完成，测试成功!");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送HTTP请求的通用方法
     * 
     * @param endpoint API端点
     * @param method HTTP方法（GET, POST等）
     * @param requestBody 请求体数据（仅用于POST等方法）
     * @return 服务器响应JSON对象
     */
    private JsonNode sendRequest(String endpoint, String method, String requestBody) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        
        // 如果已登录，添加授权令牌
        if (authToken != null) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        // 对于POST等方法添加请求体
        if (requestBody != null) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        // 获取响应
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                        StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        // 解析和返回JSON响应
        String responseBody = response.toString();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IOException("空响应或无效响应");
        }
        
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        // 检查响应是否成功
        if (!jsonResponse.path("success").asBoolean()) {
            throw new IOException("API请求失败: " + jsonResponse.path("message").asText());
        }
        
        return jsonResponse;
    }
    
    /**
     * 测试登录API
     */
    public void testLogin() throws IOException {
        System.out.println("测试登录API...");
        
        // 准备登录请求
        ObjectNode loginRequest = objectMapper.createObjectNode();
        loginRequest.put("username", "admin");
        loginRequest.put("password", "admin123");
        
        // 发送登录请求
        JsonNode response = sendRequest("/auth/login", "POST", loginRequest.toString());
        
        // 验证响应
        JsonNode userData = response.path("data");
        String username = userData.path("username").asText();
        authToken = userData.path("token").asText();
        
        System.out.println("成功登录为: " + username);
        System.out.println("获取授权令牌: " + authToken);
        
        if (authToken == null || authToken.isEmpty()) {
            throw new IOException("登录成功但未获取到授权令牌");
        }
    }
    
    /**
     * 测试获取用户列表API
     */
    public void testGetUsers() throws IOException {
        System.out.println("测试获取用户列表API...");
        
        JsonNode response = sendRequest("/users", "GET", null);
        
        // 验证响应
        JsonNode users = response.path("data");
        System.out.println("成功获取用户列表，共 " + users.size() + " 个用户");
        
        // 输出前几个用户信息
        int maxDisplayUsers = Math.min(users.size(), 3);
        for (int i = 0; i < maxDisplayUsers; i++) {
            JsonNode user = users.get(i);
            System.out.println("用户 #" + (i+1) + ": " + user.path("username").asText() + 
                    " (" + user.path("name").asText() + ")");
        }
    }
    
    /**
     * 测试获取项目列表API
     */
    public void testGetProjects() throws IOException {
        System.out.println("测试获取项目列表API...");
        
        JsonNode response = sendRequest("/projects", "GET", null);
        
        // 验证响应
        JsonNode projects = response.path("data");
        System.out.println("成功获取项目列表，共 " + projects.size() + " 个项目");
        
        // 输出前几个项目信息
        int maxDisplayProjects = Math.min(projects.size(), 3);
        for (int i = 0; i < maxDisplayProjects; i++) {
            JsonNode project = projects.get(i);
            System.out.println("项目 #" + (i+1) + ": " + project.path("id").asText() + 
                    " - " + project.path("name").asText());
        }
    }
    
    /**
     * 测试获取活动任务API
     */
    public void testGetActiveTasks() throws IOException {
        System.out.println("测试获取活动任务API...");
        
        JsonNode response = sendRequest("/tasks/active", "GET", null);
        
        // 验证响应
        JsonNode data = response.path("data");
        JsonNode task = data.path("task");
        JsonNode projectsInOrder = data.path("projectsInOrder");
        
        System.out.println("成功获取活动任务: " + task.path("taskId").asText() + 
                " (" + task.path("category").asText() + ")");
        System.out.println("任务包含 " + projectsInOrder.size() + " 个项目");
    }
    
    /**
     * 测试提交评分API
     */
    public void testSubmitScore() throws IOException {
        System.out.println("测试提交评分API...");
        
        // 准备评分请求
        ObjectNode scoreRequest = objectMapper.createObjectNode();
        scoreRequest.put("projectId", 1);
        scoreRequest.put("username", "expert");
        scoreRequest.put("totalScore", 8.5);
        scoreRequest.put("comments", "这是API测试提交的评分");
        scoreRequest.put("isDraft", false);
        
        // 添加评分项目分数
        ObjectNode scores = objectMapper.createObjectNode();
        scores.put("1", 8);
        scores.put("2", 9);
        scoreRequest.set("scores", scores);
        
        // 发送请求
        JsonNode response = sendRequest("/scores", "POST", scoreRequest.toString());
        
        // 验证响应
        JsonNode scoreData = response.path("data");
        System.out.println("成功提交评分，评分ID: " + scoreData.path("id").asText());
    }
    
    /**
     * 测试获取评分历史API
     */
    public void testGetScoreHistory() throws IOException {
        System.out.println("测试获取评分历史API...");
        
        JsonNode response = sendRequest("/scores/history?projectId=1&username=expert", "GET", null);
        
        // 验证响应
        JsonNode scores = response.path("data");
        System.out.println("成功获取评分历史，共 " + scores.size() + " 条记录");
        
        // 输出前几条评分记录
        int maxDisplayScores = Math.min(scores.size(), 3);
        for (int i = 0; i < maxDisplayScores; i++) {
            JsonNode score = scores.get(i);
            System.out.println("评分 #" + (i+1) + ": " + score.path("id").asText() + 
                    " - 总分: " + score.path("totalScore").asText());
        }
    }
    
    /**
     * 测试获取统计数据API
     */
    public void testGetStatistics() throws IOException {
        System.out.println("测试获取统计数据API...");
        
        JsonNode response = sendRequest("/statistics", "GET", null);
        
        // 验证响应
        JsonNode stats = response.path("data");
        System.out.println("成功获取统计数据:");
        System.out.println("- 总项目数: " + stats.path("totalProjects").asInt());
        System.out.println("- 已完成项目数: " + stats.path("completedProjects").asInt());
        System.out.println("- 总专家数: " + stats.path("totalExperts").asInt());
    }
}