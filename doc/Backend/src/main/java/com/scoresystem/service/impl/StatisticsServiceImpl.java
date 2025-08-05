package com.scoresystem.service.impl;

import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashSet;

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
	private ScoreItemRepository scoreItemRepository;

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
		int draftCount = (int) scores.stream().filter(Score::getIsDraft).count();
		int finalCount = scores.size() - draftCount;
		statistics.put("draftCount", draftCount);
		statistics.put("finalCount", finalCount);

		// 计算平均分
		if (!scores.isEmpty()) {
			double avgScore = scores.stream().filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
					.mapToDouble(Score::getTotalScore).average().orElse(0.0);
			statistics.put("averageScore", avgScore);
		} else {
			statistics.put("averageScore", 0.0);
		}

		// 获取相关项目
		List<Long> projectIds = scores.stream().map(Score::getProjectId).distinct().collect(Collectors.toList());
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
		List<String> experts = jdbcTemplate.queryForList("SELECT expert_username FROM task_experts WHERE task_id = ?",
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
				? (double) completedScores / (totalExperts * totalProjects)
				: 0.0;
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
			averageScore = scores.stream().filter(score -> score.getTotalScore() != null)
					.mapToDouble(Score::getTotalScore).average().orElse(0.0);
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
		List<String> usernames = jdbcTemplate.queryForList("SELECT DISTINCT user_id FROM scores", String.class);

		for (String username : usernames) {
			User user = userRepository.findByUsername(username);
			if (user == null)
				continue;

			Map<String, Object> userStat = new HashMap<>();
			userStat.put("username", user.getUsername());
			userStat.put("name", user.getName());

			// 获取用户评分
			List<Score> scores = scoreRepository.findByUsername(user.getUsername());
			userStat.put("scoreCount", scores.size());

			// 计算平均分
			if (!scores.isEmpty()) {
				double avgScore = scores.stream().filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
						.mapToDouble(Score::getTotalScore).average().orElse(0.0);
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
			if (totalScore == null)
				continue;

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
				Integer.class, startDate, endDate, startDate, endDate);
		statistics.put("projectCount", projectCount != null ? projectCount : 0);

		// 在指定日期范围内提交的评分数量
		Integer scoreCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM scores WHERE create_time BETWEEN ? AND ?", Integer.class, startDate, endDate);
		statistics.put("scoreCount", scoreCount != null ? scoreCount : 0);

		// 在指定日期范围内有评分活动的用户数量
		Integer userCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(DISTINCT user_id) FROM scores WHERE create_time BETWEEN ? AND ?", Integer.class,
				startDate, endDate);
		statistics.put("userCount", userCount != null ? userCount : 0);

		// 在指定时间段内的平均分
		Double averageScore = jdbcTemplate.queryForObject(
				"SELECT AVG(total_score) FROM scores WHERE create_time BETWEEN ? AND ? AND is_draft = 0", Double.class,
				startDate, endDate);
		statistics.put("averageScore", averageScore != null ? averageScore : 0.0);

		// 在指定时间段内的评分分布
		Map<String, Integer> scoreDistribution = new HashMap<>();
		scoreDistribution.put("0-60", 0);
		scoreDistribution.put("60-70", 0);
		scoreDistribution.put("70-80", 0);
		scoreDistribution.put("80-90", 0);
		scoreDistribution.put("90-100", 0);

		List<Double> scores = jdbcTemplate.queryForList(
				"SELECT total_score FROM scores WHERE create_time BETWEEN ? AND ? AND is_draft = 0", Double.class,
				startDate, endDate);

		for (Double score : scores) {
			if (score == null)
				continue;

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

	@Override
	public List<Map<String, Object>> getUserStatistics(Long taskId) {
		List<Map<String, Object>> statistics = new ArrayList<>();
		// 获取有评分记录的用户（指定任务）
		List<String> usernames = jdbcTemplate.queryForList("SELECT DISTINCT user_id FROM scores WHERE task_id = ?",
				String.class, taskId);
		for (String username : usernames) {
			User user = userRepository.findByUsername(username);
			if (user == null)
				continue;
			Map<String, Object> userStat = new HashMap<>();
			userStat.put("username", user.getUsername());
			userStat.put("name", user.getName());
			// 获取用户在该任务下的评分
			List<Score> scores = scoreRepository.findByUsernameAndTaskId(user.getUsername(), taskId);
			userStat.put("scoreCount", scores.size());
			// 计算平均分
			if (!scores.isEmpty()) {
				double avgScore = scores.stream().filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
						.mapToDouble(Score::getTotalScore).average().orElse(0.0);
				userStat.put("averageScore", avgScore);
			} else {
				userStat.put("averageScore", 0.0);
			}
			statistics.add(userStat);
		}
		return statistics;
	}

	@Override
	public Map<String, Object> getUserStatisticsByUsername(String username, Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		User user = userRepository.findByUsername(username);
		if (user == null) {
			return statistics;
		}
		List<Score> scores = scoreRepository.findByUsernameAndTaskId(username, taskId);
		statistics.put("scoreCount", scores.size());
		int draftCount = (int) scores.stream().filter(Score::getIsDraft).count();
		int finalCount = scores.size() - draftCount;
		statistics.put("draftCount", draftCount);
		statistics.put("finalCount", finalCount);
		if (!scores.isEmpty()) {
			double avgScore = scores.stream().filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
					.mapToDouble(Score::getTotalScore).average().orElse(0.0);
			statistics.put("averageScore", avgScore);
		} else {
			statistics.put("averageScore", 0.0);
		}
		List<Long> projectIds = scores.stream().map(Score::getProjectId).distinct().collect(Collectors.toList());
		statistics.put("projectCount", projectIds.size());
		return statistics;
	}

	@Override
	public Map<String, Object> getTimeRangeStatistics(Date startDate, Date endDate, Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		Integer projectCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(DISTINCT project_id) FROM scores WHERE create_time BETWEEN ? AND ? AND task_id = ?",
				Integer.class, startDate, endDate, taskId);
		statistics.put("projectCount", projectCount != null ? projectCount : 0);
		Integer scoreCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM scores WHERE create_time BETWEEN ? AND ? AND task_id = ?", Integer.class,
				startDate, endDate, taskId);
		statistics.put("scoreCount", scoreCount != null ? scoreCount : 0);
		Integer userCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(DISTINCT user_id) FROM scores WHERE create_time BETWEEN ? AND ? AND task_id = ?",
				Integer.class, startDate, endDate, taskId);
		statistics.put("userCount", userCount != null ? userCount : 0);
		Double averageScore = jdbcTemplate.queryForObject(
				"SELECT AVG(total_score) FROM scores WHERE create_time BETWEEN ? AND ? AND is_draft = 0 AND task_id = ?",
				Double.class, startDate, endDate, taskId);
		statistics.put("averageScore", averageScore != null ? averageScore : 0.0);
		Map<String, Integer> scoreDistribution = new HashMap<>();
		scoreDistribution.put("0-60", 0);
		scoreDistribution.put("60-70", 0);
		scoreDistribution.put("70-80", 0);
		scoreDistribution.put("80-90", 0);
		scoreDistribution.put("90-100", 0);
		List<Double> scores = jdbcTemplate.queryForList(
				"SELECT total_score FROM scores WHERE create_time BETWEEN ? AND ? AND is_draft = 0 AND task_id = ?",
				Double.class, startDate, endDate, taskId);
		for (Double score : scores) {
			if (score == null)
				continue;
			if (score < 60)
				scoreDistribution.put("0-60", scoreDistribution.get("0-60") + 1);
			else if (score < 70)
				scoreDistribution.put("60-70", scoreDistribution.get("60-70") + 1);
			else if (score < 80)
				scoreDistribution.put("70-80", scoreDistribution.get("70-80") + 1);
			else if (score < 90)
				scoreDistribution.put("80-90", scoreDistribution.get("80-90") + 1);
			else
				scoreDistribution.put("90-100", scoreDistribution.get("90-100") + 1);
		}
		statistics.put("scoreDistribution", scoreDistribution);
		return statistics;
	}

	@Override
	public Map<String, Object> getProjectStatisticsById(Long projectId, Long taskId) {
		return scoreService.getProjectScoreStatistics(projectId, taskId);
	}

	@Override
	public List<Map<String, Object>> getProjectStatistics(Long taskId) {
		List<Map<String, Object>> statistics = new ArrayList<>();
		// 获取该任务下所有项目
		List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
		for (Project project : projects) {
			Map<String, Object> stat = scoreService.getProjectScoreStatistics(project.getId(), taskId);
			statistics.add(stat);
		}
		return statistics;
	}

	@Override
	public Map<String, Object> getStatistics(Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		// 任务下项目数
		int projectCount = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId).size();
		statistics.put("totalProjects", projectCount);
		// 任务下评分数
		Integer scoreCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM scores WHERE task_id = ?", Integer.class,
				taskId);
		statistics.put("totalScores", scoreCount != null ? scoreCount : 0);
		// 任务下用户数
		Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT user_id) FROM scores WHERE task_id = ?",
				Integer.class, taskId);
		statistics.put("totalUsers", userCount != null ? userCount : 0);
		// 任务下平均分
		Double averageScore = jdbcTemplate.queryForObject(
				"SELECT AVG(total_score) FROM scores WHERE task_id = ? AND is_draft = 0", Double.class, taskId);
		statistics.put("averageScore", averageScore != null ? averageScore : 0.0);
		return statistics;
	}

	@Override
	public List<Map<String, Object>> getScoreStatistics(Long taskId) {
		List<Map<String, Object>> statistics = new ArrayList<>();
		List<Map<String, Object>> scoreStats = scoreRepository.getScoreStatisticsByTaskId(taskId);
		if (scoreStats != null) {
			statistics.addAll(scoreStats);
		}
		return statistics;
	}

	@Override
	public Map<String, Object> getDashboardStatistics(Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		// 任务下项目数
		int projectCount = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId).size();
		statistics.put("totalProjects", projectCount);
		// 任务下评分数
		Integer scoreCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM scores WHERE task_id = ?", Integer.class,
				taskId);
		statistics.put("totalScores", scoreCount != null ? scoreCount : 0);
		// 任务下平均分
		Double averageScore = jdbcTemplate.queryForObject(
				"SELECT AVG(total_score) FROM scores WHERE task_id = ? AND is_draft = 0", Double.class, taskId);
		statistics.put("averageScore", averageScore != null ? averageScore : 0.0);
		// 任务下最近活动（可根据需要补充）
		statistics.put("recentActivity", new ArrayList<>());
		return statistics;
	}

	@Override
	public Map<String, Integer> getScoreDistribution(Long taskId) {
		Map<String, Integer> distribution = new HashMap<>();
		
		// 获取指定任务的评分
		List<Score> scores = scoreRepository.findByTaskId(taskId);
		List<Score> finalScores = scores.stream().filter(score -> !score.getIsDraft()).collect(Collectors.toList());
		
		// 初始化分布区间
		distribution.put("90-100", 0);
		distribution.put("80-89", 0);
		distribution.put("70-79", 0);
		distribution.put("60-69", 0);
		distribution.put("50-59", 0);
		distribution.put("0-49", 0);
		
		// 统计分布
		for (Score score : finalScores) {
			if (score.getTotalScore() != null) {
				double totalScore = score.getTotalScore();
				if (totalScore >= 90) {
				distribution.put("90-100", distribution.get("90-100") + 1);
				} else if (totalScore >= 80) {
					distribution.put("80-89", distribution.get("80-89") + 1);
				} else if (totalScore >= 70) {
					distribution.put("70-79", distribution.get("70-79") + 1);
				} else if (totalScore >= 60) {
					distribution.put("60-69", distribution.get("60-69") + 1);
				} else if (totalScore >= 50) {
					distribution.put("50-59", distribution.get("50-59") + 1);
				} else {
					distribution.put("0-49", distribution.get("0-49") + 1);
				}
			}
		}
		
		return distribution;
	}

	/**
	 * 获取任务统计概览
	 */
	@Override
	public Map<String, Object> getTaskOverview(Long taskId) {
		Map<String, Object> overview = new HashMap<>();
		
		// 获取任务信息
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			return overview;
		}
		
		overview.put("taskId", taskId);
		overview.put("taskName", task.getCategory());
		overview.put("status", task.getStatus());
		
		// 获取任务下的项目
		List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
		overview.put("totalProjects", projects.size());
		
		// 获取任务的专家
		List<String> experts = jdbcTemplate.queryForList(
			"SELECT expert_username FROM task_experts WHERE task_id = ?", 
			String.class, taskId
		);
		overview.put("totalExperts", experts.size());
		
		// 计算评分完成情况
		int totalExpectedScores = projects.size() * experts.size();
		int completedScores = 0;
		int draftScores = 0;
		
		for (Project project : projects) {
			for (String expert : experts) {
				List<Score> scores = scoreRepository.findByProjectIdAndUsernameAndTaskId(
					project.getId(), expert, taskId
				);
				if (!scores.isEmpty()) {
					Score latestScore = scores.get(scores.size() - 1);
					if (latestScore.getIsDraft()) {
						draftScores++;
					} else {
						completedScores++;
					}
				}
			}
		}
		
		overview.put("completedScores", completedScores);
		overview.put("draftScores", draftScores);
		overview.put("totalExpectedScores", totalExpectedScores);
		overview.put("completionRate", totalExpectedScores > 0 ? 
			(double) completedScores / totalExpectedScores : 0.0);
		
		// 计算平均分
		List<Score> finalScores = scoreRepository.findByTaskIdAndIsDraft(taskId, false);
		double averageScore = finalScores.stream()
			.filter(score -> score.getTotalScore() != null)
			.mapToDouble(Score::getTotalScore)
			.average()
			.orElse(0.0);
		overview.put("averageScore", averageScore);
		
		return overview;
	}

	/**
	 * 获取项目在任务下的统计详情
	 */
	@Override
	public Map<String, Object> getProjectTaskStatistics(Long projectId, Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		
		// 获取项目信息
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			return statistics;
		}
		
		statistics.put("projectId", projectId);
		statistics.put("projectName", project.getName());
		statistics.put("taskId", taskId);
		
		// 获取该项目的所有评分
		List<Score> scores = scoreRepository.findByProjectIdAndTaskId(projectId, taskId);
		statistics.put("totalScores", scores.size());
		
		// 分离草稿和最终评分
		List<Score> draftScores = scores.stream().filter(Score::getIsDraft).collect(Collectors.toList());
		List<Score> finalScores = scores.stream().filter(score -> !score.getIsDraft()).collect(Collectors.toList());
		
		statistics.put("draftScores", draftScores.size());
		statistics.put("finalScores", finalScores.size());
		
		// 计算统计指标
		if (!finalScores.isEmpty()) {
			double avgScore = finalScores.stream()
				.filter(score -> score.getTotalScore() != null)
				.mapToDouble(Score::getTotalScore)
				.average()
				.orElse(0.0);
			statistics.put("averageScore", avgScore);
			
			double maxScore = finalScores.stream()
				.filter(score -> score.getTotalScore() != null)
				.mapToDouble(Score::getTotalScore)
				.max()
				.orElse(0.0);
			statistics.put("maxScore", maxScore);
			
			double minScore = finalScores.stream()
				.filter(score -> score.getTotalScore() != null)
				.mapToDouble(Score::getTotalScore)
				.min()
				.orElse(0.0);
			statistics.put("minScore", minScore);
			
			// 计算标准差
			double variance = finalScores.stream()
				.filter(score -> score.getTotalScore() != null)
				.mapToDouble(score -> Math.pow(score.getTotalScore() - avgScore, 2))
				.average()
				.orElse(0.0);
			double stdDev = Math.sqrt(variance);
			statistics.put("standardDeviation", stdDev);
		} else {
			statistics.put("averageScore", 0.0);
			statistics.put("maxScore", 0.0);
			statistics.put("minScore", 0.0);
			statistics.put("standardDeviation", 0.0);
		}
		
		// 获取专家评分详情
		List<Map<String, Object>> expertScores = new ArrayList<>();
		List<String> experts = jdbcTemplate.queryForList(
			"SELECT expert_username FROM task_experts WHERE task_id = ?", 
			String.class, taskId
		);
		
		for (String expert : experts) {
			List<Score> expertProjectScores = scoreRepository.findByProjectIdAndUsernameAndTaskId(
				projectId, expert, taskId
			);
			
			Map<String, Object> expertScore = new HashMap<>();
			expertScore.put("expert", expert);
			expertScore.put("hasScore", !expertProjectScores.isEmpty());
			
			if (!expertProjectScores.isEmpty()) {
				Score latestScore = expertProjectScores.get(expertProjectScores.size() - 1);
				expertScore.put("isDraft", latestScore.getIsDraft());
				expertScore.put("totalScore", latestScore.getTotalScore());
				expertScore.put("submitTime", latestScore.getUpdateTime());
			}
			
			expertScores.add(expertScore);
		}
		
		statistics.put("expertScores", expertScores);
		
		return statistics;
	}

	/**
	 * 获取评分项统计
	 */
	@Override
	public Map<String, Object> getScoreItemStatistics() {
		return getScoreItemStatistics(null);
	}

	/**
	 * 获取评分项统计（指定任务）
	 */
	@Override
	public Map<String, Object> getScoreItemStatistics(Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		
		// 获取评分项列表
		List<Map<String, Object>> scoreItems = jdbcTemplate.queryForList(
			"SELECT * FROM score_items ORDER BY display_order"
		);
		
		List<Map<String, Object>> itemStatistics = new ArrayList<>();
		
		for (Map<String, Object> item : scoreItems) {
			Long itemId = (Long) item.get("id");
			String itemName = (String) item.get("name");
			Integer maxScore = (Integer) item.get("max_score");
			
			Map<String, Object> itemStat = new HashMap<>();
			itemStat.put("itemId", itemId);
			itemStat.put("itemName", itemName);
			itemStat.put("maxScore", maxScore);
			
			// 计算该评分项的统计
			String sql = "SELECT AVG(score) as avg_score, COUNT(*) as count, " +
						"MAX(score) as max_score, MIN(score) as min_score " +
						"FROM score_item_scores sis " +
						"JOIN scores s ON sis.score_id = s.id " +
						"WHERE sis.item_id = ? AND s.is_draft = false";
			
			List<Object> params = new ArrayList<>();
			params.add(itemId);
			
			if (taskId != null) {
				sql += " AND s.task_id = ?";
				params.add(taskId);
			}
			
			List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params.toArray());
			
			if (!results.isEmpty()) {
				Map<String, Object> result = results.get(0);
				itemStat.put("averageScore", result.get("avg_score"));
				itemStat.put("scoreCount", result.get("count"));
				itemStat.put("maxScore", result.get("max_score"));
				itemStat.put("minScore", result.get("min_score"));
			} else {
				itemStat.put("averageScore", 0.0);
				itemStat.put("scoreCount", 0);
				itemStat.put("maxScore", 0);
				itemStat.put("minScore", 0);
			}
			
			itemStatistics.add(itemStat);
		}
		
		statistics.put("scoreItems", itemStatistics);
		return statistics;
	}

	/**
	 * 获取专家评分统计
	 */
	@Override
	public Map<String, Object> getExpertStatistics() {
		return getExpertStatistics(null);
	}

	/**
	 * 获取专家评分统计（指定任务）
	 */
	@Override
	public Map<String, Object> getExpertStatistics(Long taskId) {
		Map<String, Object> statistics = new HashMap<>();
		
		// 获取专家列表
		String expertSql = "SELECT DISTINCT expert_username FROM task_experts";
		List<Object> expertParams = new ArrayList<>();
		
		if (taskId != null) {
			expertSql += " WHERE task_id = ?";
			expertParams.add(taskId);
		}
		
		List<String> experts = jdbcTemplate.queryForList(expertSql, String.class, expertParams.toArray());
		
		List<Map<String, Object>> expertStatistics = new ArrayList<>();
		
		for (String expert : experts) {
			Map<String, Object> expertStat = new HashMap<>();
			expertStat.put("expert", expert);
			
			// 获取专家评分统计
			String scoreSql = "SELECT COUNT(*) as total_scores, " +
							"COUNT(CASE WHEN is_draft = false THEN 1 END) as final_scores, " +
							"COUNT(CASE WHEN is_draft = true THEN 1 END) as draft_scores, " +
							"AVG(CASE WHEN is_draft = false AND total_score IS NOT NULL THEN total_score END) as avg_score " +
							"FROM scores WHERE username = ?";
			
			List<Object> scoreParams = new ArrayList<>();
			scoreParams.add(expert);
			
			if (taskId != null) {
				scoreSql += " AND task_id = ?";
				scoreParams.add(taskId);
			}
			
			List<Map<String, Object>> results = jdbcTemplate.queryForList(scoreSql, scoreParams.toArray());
			
			if (!results.isEmpty()) {
				Map<String, Object> result = results.get(0);
				expertStat.put("totalScores", result.get("total_scores"));
				expertStat.put("finalScores", result.get("final_scores"));
				expertStat.put("draftScores", result.get("draft_scores"));
				expertStat.put("averageScore", result.get("avg_score"));
			} else {
				expertStat.put("totalScores", 0);
				expertStat.put("finalScores", 0);
				expertStat.put("draftScores", 0);
				expertStat.put("averageScore", 0.0);
			}
			
			expertStatistics.add(expertStat);
		}
		
		statistics.put("experts", expertStatistics);
		return statistics;
	}

	/**
	 * 导出统计数据
	 */
	@Override
	public String exportStatistics(Long taskId, Map<String, Object> exportOptions) {
		// 这里实现导出逻辑
		// 可以根据exportOptions中的format参数决定导出格式（excel/pdf）
		// 返回导出文件的URL
		
		String format = (String) exportOptions.getOrDefault("format", "excel");
		String fileName = "statistics_" + taskId + "_" + System.currentTimeMillis() + "." + format;
		
		// 实际实现中，这里应该生成文件并返回文件URL
		// 暂时返回一个模拟的URL
		return "/exports/" + fileName;
	}

	/**
	 * 获取前端统计页面需要的完整统计数据
	 */
	@Override
	public List<Map<String, Object>> getFrontendStatistics() {
		return getFrontendStatistics(null);
	}

	/**
	 * 获取前端统计页面需要的完整统计数据（指定任务）
	 */
	@Override
	public List<Map<String, Object>> getFrontendStatistics(Long taskId) {
		List<Map<String, Object>> statistics = new ArrayList<>();
		
		// 获取项目列表
		List<Project> projects;
		if (taskId != null) {
			projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
		} else {
			projects = projectRepository.selectList(null);
		}
		
		// 获取专家数量
		String expertSql = "SELECT COUNT(DISTINCT expert_username) FROM task_experts";
		List<Object> expertParams = new ArrayList<>();
		if (taskId != null) {
			expertSql += " WHERE task_id = ?";
			expertParams.add(taskId);
		}
		Integer totalExperts = jdbcTemplate.queryForObject(expertSql, Integer.class, expertParams.toArray());
		if (totalExperts == null) totalExperts = 0;
		
		for (Project project : projects) {
			Map<String, Object> projectStat = new HashMap<>();
			projectStat.put("id", project.getId());
			projectStat.put("name", project.getName());
			
			// 获取项目评分
			List<Score> scores;
			if (taskId != null) {
				scores = scoreRepository.findByProjectIdAndTaskId(project.getId(), taskId);
			} else {
				scores = scoreRepository.findByProjectId(project.getId());
			}
			
			// 计算完成率
			Set<String> scoredExperts = scores.stream()
				.map(Score::getUserId)
				.collect(Collectors.toSet());
			double completionRate = totalExperts > 0 ? (double) scoredExperts.size() / totalExperts * 100 : 0.0;
			projectStat.put("completionRate", completionRate);
			
			// 计算项目总分（平均分）
			double totalScore = 0.0;
			List<Score> finalScores = scores.stream()
				.filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
				.collect(Collectors.toList());
			
			if (!finalScores.isEmpty()) {
				totalScore = finalScores.stream()
					.mapToDouble(Score::getTotalScore)
					.average()
					.orElse(0.0);
			}
			projectStat.put("totalScore", totalScore);
			
			// 获取评分项统计
			List<Map<String, Object>> itemStats = new ArrayList<>();
			
			// 从项目配置中获取评分项信息
			if (project.getScoreItems() != null && !project.getScoreItems().isEmpty()) {
				for (int i = 0; i < project.getScoreItems().size(); i++) {
					String itemName = project.getScoreItems().get(i).getName();
					//Double weight = project.getScoreItems().get(i).getWeight();
					Integer maxScore = project.getScoreItems().get(i).getMaxScore();
					
					// 获取该评分项的所有分数
					List<Double> itemScores = new ArrayList<>();
					for (Score score : finalScores) {
						if (score.getScores() != null && score.getScores().containsKey((long) i)) {
							Integer itemScore = score.getScores().get((long) i);
							if (itemScore != null) {
								itemScores.add(itemScore.doubleValue());
							}
						}
					}
					
					// 计算统计值
					double avgScore = itemScores.isEmpty() ? 0.0 : 
						itemScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
					double maxItemScore = itemScores.isEmpty() ? 0.0 : 
						itemScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
					double minItemScore = itemScores.isEmpty() ? 0.0 : 
						itemScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
					
					Map<String, Object> itemStat = new HashMap<>();
					itemStat.put("name", itemName);
					itemStat.put("weight", 1);
					itemStat.put("maxScore", maxScore);
					itemStat.put("avgScore", avgScore);
					itemStat.put("maxScore", maxItemScore);
					itemStat.put("minScore", minItemScore);
					
					itemStats.add(itemStat);
				}
			}
			
			projectStat.put("itemStats", itemStats);
			statistics.add(projectStat);
		}
		
		return statistics;
	}

	@Override
public byte[] generateTaskExcel(Long taskId) {
    // 1. 查询任务、项目、评分项、专家、评分数据
    Task task = taskRepository.selectById(taskId);
    if (task == null) return null;
    String groupCategory = task.getCategory();

    List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
    // 预加载所有项目评分项
    for (Project p : projects) {
        p.setScoreItems(scoreItemRepository.findByProjectId(p.getId()));
    }

    // 查询所有专家账号和姓名
    List<String> expertUsernames = new ArrayList<>();
    Map<String, String> usernameToName = new HashMap<>();
    // 查询专家账号和姓名
    List<Map<String, Object>> expertRows = jdbcTemplate.queryForList(
        "SELECT DISTINCT username, name FROM task_experts_details WHERE task_id = ?",
        taskId
    );
    for (Map<String, Object> row : expertRows) {
        String username = (String) row.get("username");
        String name = (String) row.get("name");
        expertUsernames.add(username);
        usernameToName.put(username, name != null ? name : username);
    }

    // 查询所有评分
    List<Score> allScores = scoreRepository.findByTaskId(taskId);
    // 只保留非草稿
    allScores = allScores.stream().filter(s -> !Boolean.TRUE.equals(s.getIsDraft())).collect(Collectors.toList());
    
    // 为每个评分记录加载详细的评分项分数
    for (Score score : allScores) {
        List<Map<String, Object>> scoreDetails = jdbcTemplate.queryForList(
            "SELECT score_item_id, score_value FROM score_details WHERE score_id = ?",
            score.getId()
        );
        Map<Long, Integer> scoresMap = new HashMap<>();
        for (Map<String, Object> detail : scoreDetails) {
            Long scoreItemId = ((Number) detail.get("score_item_id")).longValue();
            Integer scoreValue = ((Number) detail.get("score_value")).intValue();
            scoresMap.put(scoreItemId, scoreValue);
        }
        score.setScores(scoresMap);
    }

    // 查询专家-项目-评分项分配关系
    String detailSql = "SELECT project_id, username, score_item_id FROM task_experts_details WHERE task_id = ?";
    List<Map<String, Object>> expertDetails = jdbcTemplate.queryForList(detailSql, taskId);
    // Map<projectId, Map<expert, Set<scoreItemId>>>
    Map<Long, Map<String, Set<Long>>> projectExpertScoreItems = new HashMap<>();
    for (Map<String, Object> row : expertDetails) {
        Long projectId = ((Number)row.get("project_id")).longValue();
        String expert = (String)row.get("username");
        Long scoreItemId = ((Number)row.get("score_item_id")).longValue();
        projectExpertScoreItems
            .computeIfAbsent(projectId, k -> new HashMap<>())
            .computeIfAbsent(expert, k -> new HashSet<>())
            .add(scoreItemId);
    }

    // 计算总分并排名
    Map<Long, Double> projectIdToTotalScore = new HashMap<>();
    for (Project p : projects) {
        double total = allScores.stream()
            .filter(s -> s.getProjectId().equals(p.getId()) && s.getTotalScore() != null)
            .mapToDouble(Score::getTotalScore).sum();
        projectIdToTotalScore.put(p.getId(), total);
    }
    // 排序
    List<Project> sortedProjects = new ArrayList<>(projects);
    sortedProjects.sort((a, b) -> Double.compare(
        projectIdToTotalScore.getOrDefault(b.getId(), 0.0),
        projectIdToTotalScore.getOrDefault(a.getId(), 0.0)
    ));

    // 计算排名
    Map<Long, Integer> projectIdToRank = new HashMap<>();
    int rank = 1;
    double lastScore = Double.NaN;
    int lastRank = 1;
    for (int i = 0; i < sortedProjects.size(); i++) {
        Project p = sortedProjects.get(i);
        double score = projectIdToTotalScore.getOrDefault(p.getId(), 0.0);
        if (!Double.isNaN(lastScore) && Double.compare(score, lastScore) != 0) {
            rank = i + 1;
        }
        projectIdToRank.put(p.getId(), rank);
        lastScore = score;
        lastRank = rank;
    }

    // 2. 组装Excel
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("评分明细");
        // 居中样式
        org.apache.poi.ss.usermodel.CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

        // 表头
        String[] headers = {"排名", "项目名称", "牵头单位", "组别", "所属产业", "评审专家", "打分模块", "专家打分", "最终得分"};
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(centerStyle);
        }

        int rowIdx = 1;
        for (Project project : sortedProjects) {
            List<ScoreItem> scoreItems = project.getScoreItems();
            if (scoreItems == null) continue;
            Map<String, Set<Long>> expertScoreItems = projectExpertScoreItems.getOrDefault(project.getId(), Collections.emptyMap());
            List<Object[]> detailRows = new ArrayList<>();
            for (String expert : expertUsernames) {
                String expertName = usernameToName.getOrDefault(expert, expert);
                Set<Long> responsibleItems = expertScoreItems.getOrDefault(expert, Collections.emptySet());
                if (responsibleItems.isEmpty()) continue;
                // 找到该专家对该项目的评分
                Score score = allScores.stream()
                    .filter(s -> s.getProjectId().equals(project.getId()) && expert.equals(s.getUserId()))
                    .findFirst().orElse(null);
                Map<Long, Integer> scoreMap = score != null ? score.getScores() : null;
                for (ScoreItem item : scoreItems) {
                    if (!responsibleItems.contains(item.getId())) continue; // 只导出负责的评分项
                    Integer value = (scoreMap != null && scoreMap.containsKey(item.getId())) ? scoreMap.get(item.getId()) : null;
                    detailRows.add(new Object[]{
                        projectIdToRank.get(project.getId()), // 排名
                        project.getName(), // 项目名称
                        project.getUnit(), // 牵头单位
                        groupCategory, // 组别
                        project.getIndustry(), // 所属产业
                        expertName, // 评审专家
                        item.getName(), // 打分模块
                        value, // 专家打分
                        projectIdToTotalScore.get(project.getId()) // 最终得分
                    });
                }
            }
            // 跨行合并：排名、项目名称、牵头单位、组别、所属产业、最终得分
            int mergeRows = detailRows.size();
            int startRow = rowIdx;
            for (Object[] rowData : detailRows) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowData.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                    if (rowData[i] != null)
                        cell.setCellValue(rowData[i].toString());
                    else
                        cell.setCellValue("");
                    cell.setCellStyle(centerStyle); // 应用居中样式
                }
            }
            // 合并单元格
            if (mergeRows > 1) {
                for (int col : new int[]{0, 1, 2, 3, 4, 8}) {
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        startRow, startRow + mergeRows - 1, col, col
                    ));
                }
            }
        }
        // 自动列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        workbook.write(bos);
        return bos.toByteArray();
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

}
