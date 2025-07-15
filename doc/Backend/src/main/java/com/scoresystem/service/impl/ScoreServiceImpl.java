package com.scoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评分服务实现类
 */
@Service
@Transactional
@Profile("!test")
public class ScoreServiceImpl extends ServiceImpl<ScoreRepository, Score> implements ScoreService {

    @Autowired
    private ScoreRepository scoreRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScoreItemRepository scoreItemRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 保存评分
     */
    @Override
    public ScoreDTO saveScore(ScoreRequest scoreRequest) {
        // 查询项目和用户
        Project project = projectRepository.selectById(scoreRequest.getProjectId());
        User user = userRepository.findByUsername(scoreRequest.getUsername());
        
        if (project == null || user == null) {
            return null;
        }
        
        // 查找现有评分或创建新评分（需要同时匹配项目、任务和用户）
        List<Score> existingScores = scoreRepository.findByProjectIdAndTaskIdAndUsername(
                scoreRequest.getProjectId(), scoreRequest.getTaskId(), scoreRequest.getUsername());
        
        Score score;
        if (existingScores.isEmpty() || scoreRequest.getIsDraft()) {
            // 创建新评分
            score = new Score();
            score.setProjectId(scoreRequest.getProjectId());
            score.setTaskId(scoreRequest.getTaskId());
            score.setUserId(scoreRequest.getUsername());
            score.setCreateTime(new Date());
        } else {
            // 更新现有评分
            score = existingScores.get(0);
        }
        
        // 更新评分字段
        score.setComments(scoreRequest.getComments());
        score.setIsDraft(scoreRequest.getIsDraft());
        score.setUpdateTime(new Date());
        
        // 计算总分
        double totalScore = 0.0;
        if (scoreRequest.getScores() != null && !scoreRequest.getScores().isEmpty()) {
            // 先获取所有评分项
            List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(scoreRequest.getProjectId());
            Map<Long, ScoreItem> scoreItemMap = scoreItems.stream()
                    .collect(Collectors.toMap(ScoreItem::getId, item -> item));
            
            // 计算加权总分
            double weightSum = 0.0;
            for (Map.Entry<Long, Integer> entry : scoreRequest.getScores().entrySet()) {
                ScoreItem item = scoreItemMap.get(entry.getKey());
                if (item != null) {
                	double weight = (item.getWeight() != null) ? item.getWeight() : 1.0;
                    totalScore += entry.getValue() * weight;
                    weightSum += weight;
                }
            }
            
            if (weightSum > 0) {
                totalScore = totalScore / weightSum;
            }
        }
        
        score.setTotalScore(totalScore);
        
        // 保存评分
        if (score.getId() == null) {
            scoreRepository.insert(score);
        } else {
            scoreRepository.updateById(score);
        }
        
        // 保存评分详情
        saveScoreDetails(score.getId(), scoreRequest.getScores());
        
        // 查询关联信息并转换
        score.setProject(project);
        score.setUser(user);
        score.setScores(scoreRequest.getScores());
        
        return convertToDTO(score);
    }
    
    /**
     * 获取评分历史
     */
    @Override
    public List<ScoreDTO> getScoreHistory(Long projectId, String username) {
        List<Score> scores = scoreRepository.findByProjectIdAndUsername(projectId, username);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取评分历史（指定任务）
     */
    @Override
    public List<ScoreDTO> getScoreHistory(Long projectId, Long taskId, String username) {
        List<Score> scores = scoreRepository.findByProjectIdAndTaskIdAndUsername(projectId, taskId, username);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取项目所有评分
     */
    @Override
    public List<ScoreDTO> getScoresByProject(Long projectId) {
        List<Score> scores = scoreRepository.findByProjectId(projectId);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取项目所有评分（指定任务）
     */
    @Override
    public List<ScoreDTO> getScoresByProject(Long projectId, Long taskId) {
        List<Score> scores = scoreRepository.findByProjectIdAndTaskId(projectId, taskId);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户所有评分
     */
    @Override
    public List<ScoreDTO> getScoresByUser(String username) {
        List<Score> scores = scoreRepository.findByUsername(username);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户所有评分（指定任务）
     */
    @Override
    public List<ScoreDTO> getScoresByUser(String username, Long taskId) {
        List<Score> scores = scoreRepository.findByUsernameAndTaskId(username, taskId);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算项目总评分
     */
    @Override
    public Double calculateProjectTotalScore(Long projectId) {
        List<Score> finalScores = scoreRepository.findFinalScoresByProjectId(projectId);
        
        if (finalScores.isEmpty()) {
            return 0.0;
        }
        
        // 计算平均分
        double sum = finalScores.stream()
                .mapToDouble(Score::getTotalScore)
                .sum();
        
        return sum / finalScores.size();
    }
    
    /**
     * 计算项目总评分（指定任务）
     */
    @Override
    public Double calculateProjectTotalScore(Long projectId, Long taskId) {
        List<Score> finalScores = scoreRepository.findFinalScoresByProjectIdAndTaskId(projectId, taskId);
        
        if (finalScores.isEmpty()) {
            return 0.0;
        }
        
        // 计算平均分
        double sum = finalScores.stream()
                .mapToDouble(Score::getTotalScore)
                .sum();
        
        return sum / finalScores.size();
    }
    
    /**
     * 计算评分项总评分
     */
    @Override
    public Double calculateScoreItemAverage(Long projectId, Long scoreItemId) {
        return scoreRepository.calculateAverageScoreByProjectIdAndScoreItemId(projectId, scoreItemId);
    }
    
    /**
     * 计算评分项总评分（指定任务）
     */
    @Override
    public Double calculateScoreItemAverage(Long projectId, Long taskId, Long scoreItemId) {
        return scoreRepository.calculateAverageScoreByProjectIdAndTaskIdAndScoreItemId(projectId, taskId, scoreItemId);
    }
    
    /**
     * 获取项目评分统计
     */
    @Override
    public Map<String, Object> getProjectScoreStatistics(Long projectId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取项目
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            statistics.put("totalScore", 0.0);
            statistics.put("scorerCount", 0);
            statistics.put("itemScores", new HashMap<>());
            return statistics;
        }
        
        // 获取项目评分项
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
        if (scoreItems == null) {
            scoreItems = new ArrayList<>();
        }
        
        // 计算总体评分
        Double totalScore = calculateProjectTotalScore(projectId);
        statistics.put("totalScore", totalScore);
        
        // 计算各评分项平均分
        Map<Long, Double> itemScores = new HashMap<>();
        for (ScoreItem item : scoreItems) {
            Double avgScore = calculateScoreItemAverage(projectId, item.getId());
            itemScores.put(item.getId(), avgScore != null ? avgScore : 0.0);
        }
        statistics.put("itemScores", itemScores);
        
        // 获取评分人数
        List<Score> finalScores = scoreRepository.findFinalScoresByProjectId(projectId);
        statistics.put("scorerCount", finalScores != null ? finalScores.size() : 0);
        
        return statistics;
    }
    
    /**
     * 获取项目评分统计（指定任务）
     */
    @Override
    public Map<String, Object> getProjectScoreStatistics(Long projectId, Long taskId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取项目信息
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            return statistics;
        }
        
        statistics.put("projectId", projectId);
        statistics.put("projectName", project.getName());
        statistics.put("taskId", taskId);
        
        // 获取该任务下的所有评分
        List<Score> scores = scoreRepository.findByProjectIdAndTaskId(projectId, taskId);
        statistics.put("totalScores", scores.size());
        
        // 计算平均分
        if (!scores.isEmpty()) {
            double averageScore = scores.stream()
                    .filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
                    .mapToDouble(Score::getTotalScore)
                    .average()
                    .orElse(0.0);
            statistics.put("averageScore", averageScore);
        } else {
            statistics.put("averageScore", 0.0);
        }
        
        // 统计草稿和最终评分数量
        long draftCount = scores.stream().filter(Score::getIsDraft).count();
        long finalCount = scores.size() - draftCount;
        statistics.put("draftCount", draftCount);
        statistics.put("finalCount", finalCount);
        
        // 获取评分项统计
        List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
        List<Map<String, Object>> itemStats = new ArrayList<>();
        
        for (ScoreItem item : scoreItems) {
            Map<String, Object> itemStat = new HashMap<>();
            itemStat.put("scoreItemId", item.getId());
            itemStat.put("scoreItemName", item.getName());
            itemStat.put("weight", item.getWeight());
            
            // 计算该评分项的平均分
            Double avgScore = scoreRepository.calculateAverageScoreByProjectIdAndTaskIdAndScoreItemId(
                    projectId, taskId, item.getId());
            itemStat.put("averageScore", avgScore != null ? avgScore : 0.0);
            
            itemStats.add(itemStat);
        }
        
        statistics.put("scoreItems", itemStats);
        
        return statistics;
    }
    
    /**
     * 加载评分关联信息
     */
    private void loadScoreRelations(Score score) {
        // 加载项目
        Project project = projectRepository.selectById(score.getProjectId());
        score.setProject(project);
        
        // 加载用户
        User user = userRepository.findByUsername(score.getUserId());
        score.setUser(user);
        
        // 加载评分详情
        Map<Long, Integer> scoreDetails = getScoreDetails(score.getId());
        score.setScores(scoreDetails);
    }
    
    /**
     * 获取评分详情
     */
    private Map<Long, Integer> getScoreDetails(Long scoreId) {
        return jdbcTemplate.query(
                "SELECT score_item_id, score_value FROM score_details WHERE score_id = ?",
                new Object[]{scoreId},
                rs -> {
                    Map<Long, Integer> details = new HashMap<>();
                    while (rs.next()) {
                        details.put(rs.getLong("score_item_id"), rs.getInt("score_value"));
                    }
                    return details;
                });
    }
    
    /**
     * 保存评分详情
     */
    private void saveScoreDetails(Long scoreId, Map<Long, Integer> scores) {
        // 删除旧评分详情
        jdbcTemplate.update("DELETE FROM score_details WHERE score_id = ?", scoreId);
        
        // 添加新评分详情
        if (scores != null && !scores.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : scores.entrySet()) {
                jdbcTemplate.update(
                        "INSERT INTO score_details (score_id, score_item_id, score_value) VALUES (?, ?, ?)",
                        new Object[]{scoreId, entry.getKey(), entry.getValue()});
            }
        }
    }
    
    /**
     * 转换Score实体到ScoreDTO
     */
    private ScoreDTO convertToDTO(Score score) {
        ScoreDTO dto = new ScoreDTO();
        dto.setId(score.getId());
        dto.setProjectId(score.getProjectId());
        dto.setUsername(score.getUserId());
        dto.setTotalScore(score.getTotalScore());
        dto.setComments(score.getComments());
        dto.setCreateTime(score.getCreateTime());
        dto.setUpdateTime(score.getUpdateTime());
        dto.setIsDraft(score.getIsDraft());
        dto.setScores(score.getScores());
        dto.setTaskId(score.getTaskId());
        
        // 设置项目名称
        if (score.getProject() != null) {
            dto.setProjectName(score.getProject().getName());
        }
        
        // 设置用户名称
        if (score.getUser() != null) {
            dto.setUserFullName(score.getUser().getName());
        }
        
        return dto;
    }
    
    /**
     * 获取所有评分记录
     */
    @Override
    public List<ScoreDTO> getAllScores() {
        List<Score> scores = scoreRepository.selectList(null);
        return scores.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有评分记录（指定任务）
     */
    @Override
    public List<ScoreDTO> getAllScores(Long taskId) {
        List<Score> scores = scoreRepository.findByTaskId(taskId);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 按任务获取评分记录
     */
    @Override
    public List<ScoreDTO> getScoresByTask(Long taskId) {
        List<Score> scores = scoreRepository.findByTaskId(taskId);
        
        // 加载关联信息
        return scores.stream()
                .map(score -> {
                    loadScoreRelations(score);
                    return convertToDTO(score);
                })
                .collect(Collectors.toList());
    }
}
