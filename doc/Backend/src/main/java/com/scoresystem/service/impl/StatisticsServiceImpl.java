package com.scoresystem.service.impl;

import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ScoreService;
import com.scoresystem.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ScoreRepository scoreRepository;
    
    @Autowired
    private ScoreService scoreService;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    /**
     * 获取统计数据
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 项目总数
        Long projectCount = projectRepository.selectCount(null);
        statistics.put("totalProjects", projectCount);
        
        // 活动项目数
        int activeProjects = projectRepository.findByStatus("active").size();
        statistics.put("activeProjects", activeProjects);
        
        // 已完成项目数
        int completedProjects = projectRepository.findByStatus("completed").size();
        statistics.put("completedProjects", completedProjects);
        
        // 用户总数
        Long userCount = userRepository.selectCount(null);
        statistics.put("totalUsers", userCount);
        
        // 活动任务数
        int activeTasks = taskRepository.findByStatus("active").size();
        statistics.put("activeTasks", activeTasks);
        
        return statistics;
    }
    
    /**
     * 获取单个项目评分统计
     */
    @Override
    public Map<String, Object> getProjectStatisticsById(Long projectId) {
        return scoreService.getProjectScoreStatistics(projectId);
    }
    
    /**
     * 获取单个用户评分统计
     */
    @Override
    public Map<String, Object> getUserStatisticsByUsername(String username) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取用户
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return statistics;
        }
        
        // 获取用户评分
        List<Score> scores = scoreRepository.findByUsername(username);
        statistics.put("scoreCount", scores.size());
        
        // 分析评分状态
        int draftCount = (int) scores.stream()
                .filter(Score::getIsDraft)
                .count();
        int finalCount = scores.size() - draftCount;
        statistics.put("draftCount", draftCount);
        statistics.put("finalCount", finalCount);
        
        // 计算平均分
        if (!scores.isEmpty()) {
            double avgScore = scores.stream()
                    .filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
                    .mapToDouble(Score::getTotalScore)
                    .average()
                    .orElse(0.0);
            statistics.put("averageScore", avgScore);
        } else {
            statistics.put("averageScore", 0.0);
        }
        
        // 获取相关项目
        List<Long> projectIds = scores.stream()
                .map(Score::getProjectId)
                .distinct()
                .collect(Collectors.toList());
        statistics.put("projectCount", projectIds.size());
        
        return statistics;
    }
    
    /**
     * 获取单个任务评分统计
     */
    @Override
    public Map<String, Object> getTaskStatisticsById(Long taskId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取任务
        Task task = taskRepository.selectById(taskId);
        if (task == null) {
            return statistics;
        }
        
        // 获取任务的项目
        List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
        statistics.put("projectCount", projects.size());
        
        // 获取任务的专家
        List<String> experts = jdbcTemplate.queryForList(
                "SELECT expert_username FROM task_experts WHERE task_id = ?",
                String.class, taskId);
        statistics.put("expertCount", experts.size());
        
        // 统计每个项目的评分情况
        Map<Long, Object> projectStatistics = new HashMap<>();
        for (Project project : projects) {
            Map<String, Object> projectStat = scoreService.getProjectScoreStatistics(project.getId());
            projectStatistics.put(project.getId(), projectStat);
        }
        statistics.put("projectStatistics", projectStatistics);
        
        // 计算任务总体完成情况
        int totalExperts = experts.size();
        int totalProjects = projects.size();
        int completedScores = 0;
        
        for (Project project : projects) {
            for (String expert : experts) {
                List<Score> scores = scoreRepository.findByProjectIdAndUsername(project.getId(), expert);
                if (scores.stream().anyMatch(score -> !score.getIsDraft())) {
                    completedScores++;
                }
            }
        }
        
        double completionRate = totalExperts * totalProjects > 0 
                ? (double) completedScores / (totalExperts * totalProjects) : 0.0;
        statistics.put("completionRate", completionRate);
        
        return statistics;
    }
    
    /**
     * 获取仪表盘统计数据
     */
    @Override
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 项目总数
        Long projectCount = projectRepository.selectCount(null);
        statistics.put("totalProjects", projectCount);
        
        // 评分总数
        Long scoreCount = scoreRepository.selectCount(null);
        statistics.put("totalScores", scoreCount);
        
        // 计算平均分
        double averageScore = 0.0;
        List<Score> scores = scoreRepository.findByIsDraft(false);
        if (!scores.isEmpty()) {
            averageScore = scores.stream()
                .filter(score -> score.getTotalScore() != null)
                .mapToDouble(Score::getTotalScore)
                .average()
                .orElse(0.0);
        }
        statistics.put("averageScore", averageScore);
        
        // 最近活动
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        // 这里可以添加最近的评分、项目创建等活动
        statistics.put("recentActivity", recentActivity);
        
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getProjectStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        
        // 获取所有项目的统计数据
        List<Map<String, Object>> projectStats = projectRepository.getProjectStatistics();
        if (projectStats != null) {
            statistics.addAll(projectStats);
        }
        
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getTaskStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        
        // 获取所有任务的统计数据
        List<Map<String, Object>> taskStats = taskRepository.getTaskStatistics();
        if (taskStats != null) {
            statistics.addAll(taskStats);
        }
        
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getScoreStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        
        // 获取所有评分的统计数据
        List<Map<String, Object>> scoreStats = scoreRepository.getScoreStatistics();
        if (scoreStats != null) {
            statistics.addAll(scoreStats);
        }
        
        return statistics;
    }
    
    /**
     * 获取用户统计数据
     */
    @Override
    public List<Map<String, Object>> getUserStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        
        // 获取有评分记录的用户
        List<String> usernames = jdbcTemplate.queryForList(
            "SELECT DISTINCT user_id FROM scores", String.class);
        
        for (String username : usernames) {
            User user = userRepository.findByUsername(username);
            if (user == null) continue;
            
            Map<String, Object> userStat = new HashMap<>();
            userStat.put("username", user.getUsername());
            userStat.put("name", user.getName());
            
            // 获取用户评分
            List<Score> scores = scoreRepository.findByUsername(user.getUsername());
            userStat.put("scoreCount", scores.size());
            
            // 计算平均分
            if (!scores.isEmpty()) {
                double avgScore = scores.stream()
                    .filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
                    .mapToDouble(Score::getTotalScore)
                    .average()
                    .orElse(0.0);
                userStat.put("averageScore", avgScore);
            } else {
                userStat.put("averageScore", 0.0);
            }
            
            statistics.add(userStat);
        }
        
        return statistics;
    }
    
    /**
     * 获取评分分布统计
     */
    @Override
    public Map<String, Integer> getScoreDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        
        // 初始化分数区间
        distribution.put("0-60", 0);
        distribution.put("60-70", 0);
        distribution.put("70-80", 0);
        distribution.put("80-90", 0);
        distribution.put("90-100", 0);
        
        // 获取所有非草稿评分
        List<Score> scores = scoreRepository.findByIsDraft(false);
        
        // 统计各区间评分数量
        for (Score score : scores) {
            Double totalScore = score.getTotalScore();
            if (totalScore == null) continue;
            
            if (totalScore < 60) {
                distribution.put("0-60", distribution.get("0-60") + 1);
            } else if (totalScore < 70) {
                distribution.put("60-70", distribution.get("60-70") + 1);
            } else if (totalScore < 80) {
                distribution.put("70-80", distribution.get("70-80") + 1);
            } else if (totalScore < 90) {
                distribution.put("80-90", distribution.get("80-90") + 1);
            } else {
                distribution.put("90-100", distribution.get("90-100") + 1);
            }
        }
        
        return distribution;
    }
    
    /**
     * 获取时间段统计数据
     */
    @Override
    public Map<String, Object> getTimeRangeStatistics(Date startDate, Date endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 在指定日期范围内创建或更新的项目数量
        Integer projectCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM projects WHERE create_time BETWEEN ? AND ? OR update_time BETWEEN ? AND ?", 
            Integer.class, 
            startDate, endDate, startDate, endDate
        );
        statistics.put("projectCount", projectCount != null ? projectCount : 0);
        
        // 在指定日期范围内提交的评分数量
        Integer scoreCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scores WHERE create_time BETWEEN ? AND ?", 
            Integer.class, 
            startDate, endDate
        );
        statistics.put("scoreCount", scoreCount != null ? scoreCount : 0);
        
        // 在指定日期范围内有评分活动的用户数量
        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT user_id) FROM scores WHERE create_time BETWEEN ? AND ?", 
            Integer.class, 
            startDate, endDate
        );
        statistics.put("userCount", userCount != null ? userCount : 0);
        
        // 在指定时间段内的平均分
        Double averageScore = jdbcTemplate.queryForObject(
            "SELECT AVG(total_score) FROM scores WHERE create_time BETWEEN ? AND ? AND is_draft = 0", 
            Double.class, 
            startDate, endDate
        );
        statistics.put("averageScore", averageScore != null ? averageScore : 0.0);
        
        // 在指定时间段内的评分分布
        Map<String, Integer> scoreDistribution = new HashMap<>();
        scoreDistribution.put("0-60", 0);
        scoreDistribution.put("60-70", 0);
        scoreDistribution.put("70-80", 0);
        scoreDistribution.put("80-90", 0);
        scoreDistribution.put("90-100", 0);
        
        List<Double> scores = jdbcTemplate.queryForList(
            "SELECT total_score FROM scores WHERE create_time BETWEEN ? AND ? AND is_draft = 0", 
            Double.class, 
            startDate, endDate
        );
        
        for (Double score : scores) {
            if (score == null) continue;
            
            if (score < 60) {
                scoreDistribution.put("0-60", scoreDistribution.get("0-60") + 1);
            } else if (score < 70) {
                scoreDistribution.put("60-70", scoreDistribution.get("60-70") + 1);
            } else if (score < 80) {
                scoreDistribution.put("70-80", scoreDistribution.get("70-80") + 1);
            } else if (score < 90) {
                scoreDistribution.put("80-90", scoreDistribution.get("80-90") + 1);
            } else {
                scoreDistribution.put("90-100", scoreDistribution.get("90-100") + 1);
            }
        }
        statistics.put("scoreDistribution", scoreDistribution);
        
        return statistics;
    }
}
